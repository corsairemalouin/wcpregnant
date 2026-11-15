package org.manu.wcpregnant.agent.inference;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.config.AgentToolConfig;
import tools.jackson.databind.ObjectMapper;

/**
 * Client d'inférence pointant vers le serveur local Strata AI (Qwen-3.8-Flash-Next),
 * qui expose une API compatible OpenAI {@code /chat/completions}.
 *
 * <p><strong>Zéro fuite de données</strong> : l'URL est configurée par variable
 * d'environnement et ne pointe que vers localhost ou le réseau privé du cluster.
 * Aucun appel n'est effectué vers un service cloud.</p>
 *
 * @see InferenceClient
 * @see AgentToolConfig
 */
@Slf4j
public class LocalInferenceClient implements InferenceClient {

    /** Client HTTP JDK, injecté pour permettre la testabilité. */
    private final HttpClient httpClient;

    /** Mapper Jackson utilisé pour sérialiser/désérialiser les échanges JSON. */
    private final ObjectMapper objectMapper;

    /** URL de base du serveur d'inférence local, par exemple {@code http://localhost:8000/v1}. */
    private final String baseUrl;

    /** Nom du modèle servi localement. */
    private final String model;

    /** Délai maximal d'une requête d'inférence. */
    private final Duration requestTimeout;

    /**
     * Construit le client à partir de la configuration agentique.
     *
     * @param config       configuration des outils et de l'inférence. Ne doit pas être {@code null}.
     * @param httpClient   client HTTP à utiliser. Ne doit pas être {@code null}.
     * @param objectMapper mapper Jackson. Ne doit pas être {@code null}.
     */
    public LocalInferenceClient(AgentToolConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        Objects.requireNonNull(config, "config must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        String configuredBaseUrl = config.getInferenceBaseUrl();
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            throw new IllegalArgumentException("inferenceBaseUrl must not be blank");
        }
        if (config.getInferenceModel() == null || config.getInferenceModel().isBlank()) {
            throw new IllegalArgumentException("inferenceModel must not be blank");
        }

        this.baseUrl = configuredBaseUrl;
        this.model = config.getInferenceModel();
        this.requestTimeout = Duration.ofSeconds(Math.max(1, config.getInferenceTimeoutSeconds()));
    }

    /**
     * Envoie la conversation au modèle local et retourne sa réponse.
     *
     * @param request la requête d'inférence. Ne doit pas être {@code null}.
     * @return la réponse du modèle
     * @throws InferenceException si le serveur local est injoignable, en timeout ou renvoie une erreur HTTP
     */
    @Override
    public InferenceResponse chat(InferenceRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String payload = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        log.debug("Calling local inference model {} at {}", request.model(), endpoint());

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new InferenceException("Local inference server returned HTTP " + status + ": " + truncate(response.body()));
            }
            InferenceResponse inferenceResponse = objectMapper.readValue(response.body(), InferenceResponse.class);
            log.debug("Local inference model answered after HTTP {}", status);
            return inferenceResponse;
        } catch (InferenceException e) {
            throw e;
        } catch (IOException e) {
            throw new InferenceException("Unable to reach the local inference server at " + endpoint() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InferenceException("Inference request was interrupted", e);
        }
    }

    /**
     * URL complète de l'endpoint de conversation.
     *
     * @return {@code baseUrl} suivi de {@code /chat/completions}
     */
    private URI endpoint() {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + "/chat/completions");
    }

    /**
     * Tronque le corps de réponse pour éviter d'inonder les logs.
     *
     * @param body corps de réponse brut
     * @return les 200 premiers caractères
     */
    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }
}
