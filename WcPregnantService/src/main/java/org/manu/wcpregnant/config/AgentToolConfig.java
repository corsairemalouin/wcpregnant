package org.manu.wcpregnant.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import org.manu.wcpregnant.agent.inference.InferenceClient;
import org.manu.wcpregnant.agent.inference.LocalInferenceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * Configuration de la couche agentique (Tool Calling).
 *
 * <p>Centralise les limites appliquées aux outils (rayon de recherche, nombre
 * de résultats) et les paramètres d'inférence du modèle local servi par
 * Strata AI. Toutes ces valeurs proviennent de {@code application.properties}
 * et peuvent être surchargées par des variables d'environnement, conformément
 * à la contrainte "aucune URL ni clé en dur".</p>
 */
@Configuration
@Getter
public class AgentToolConfig {

    /** Rayon appliqué quand le LLM n'indique pas de rayon (en mètres). */
    @Value("${agent.tools.default-radius:200.0}")
    private double defaultRadius;

    /** Rayon maximal autorisé pour une recherche de proximité (en mètres). */
    @Value("${agent.tools.max-radius:5000.0}")
    private double maxRadius;

    /** Nombre maximal d'équipements renvoyés par un appel d'outil. */
    @Value("${agent.tools.max-results:10}")
    private int maxResults;

    /** URL de l'API compatible OpenAI du serveur d'inférence local. */
    @Value("${agent.inference.base-url:http://localhost:8000/v1}")
    private String inferenceBaseUrl;

    /** Nom du modèle servi localement par Strata AI. */
    @Value("${agent.inference.model:Qwen-3.8-Flash-Next}")
    private String inferenceModel;

    /** Délai maximal (en secondes) d'une requête auprès du modèle local. */
    @Value("${agent.inference.timeout-seconds:30}")
    private int inferenceTimeoutSeconds;

    /** Nombre maximal d'allers-retours entre le modèle et les outils avant de rendre la réponse. */
    @Value("${agent.max-iterations:4}")
    private int maxIterations;

    /**
     * Fournit le client d'inférence utilisé par la couche agent.
     *
     * <p>Le client pointe exclusivement vers le serveur local configuré :
     * aucune donnée ne quitte l'environnement.</p>
     *
     * @param objectMapper mapper Jackson mis à disposition par Spring
     * @return un {@link InferenceClient} configuré pour l'inférence locale
     */
    @Bean
    public InferenceClient inferenceClient(ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, inferenceTimeoutSeconds)))
                .build();
        return new LocalInferenceClient(this, httpClient, Objects.requireNonNull(objectMapper, "objectMapper must not be null"));
    }
}
