package org.manu.wcpregnant.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.agent.AgentService;

import org.manu.wcpregnant.agent.tool.ToolCall;
import org.manu.wcpregnant.agent.tool.ToolExecutor;
import org.manu.wcpregnant.agent.tool.ToolRegistry;
import org.manu.wcpregnant.agent.tool.ToolResult;
import org.manu.wcpregnant.web.api.AgentApi;
import org.manu.wcpregnant.web.dto.AgentChatRequest;
import org.manu.wcpregnant.web.dto.AgentChatResponse;

import org.manu.wcpregnant.web.dto.ApiError;
import org.manu.wcpregnant.web.dto.ToolCallRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation de l'API agentique (Tool Calling).
 *
 * <p>Conformément au principe <strong>DIP</strong>, ce contrôleur ne dépend que
 * des abstractions {@link ToolRegistry} et {@link ToolExecutor} de la couche
 * {@code agent} : il ne contient aucune logique métier et se contente de
 * transmettre les appels au registre et à l'exécuteur.</p>
 *
 * <p>Le RBAC est appliqué ici comme sur les autres endpoints : la découverte
 * des outils exige une authentification, et l'exécuteur refuse toute écriture
 * sans le rôle {@code ADMIN}.</p>
 *
 * @see org.manu.wcpregnant.agent.tool.ToiletToolRegistry
 * @see org.manu.wcpregnant.agent.tool.ToiletToolExecutor
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AgentApiImpl implements AgentApi {

    private final ToolRegistry registry;
    private final ToolExecutor executor;
    private final AgentService agentService;


    /**
     * {@inheritDoc}
     *
     * <p>Retourne le registre sérialisé au format OpenAI Function Calling,
     * directement injectable dans le champ {@code tools} d'une requête
     * {@code /chat/completions} envoyée au serveur d'inférence local.</p>
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Object>> getAgentTools() {
        List<Object> tools = new ArrayList<>(registry.openAiTools());
        log.debug("Exposing {} agent tools to the local inference engine", tools.size());
        return ResponseEntity.ok(tools);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Le nom de l'outil et ses arguments sont validés par l'exécuteur, qui
     * confronte l'appel au registre avant de déléguer aux services métier.</p>
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> callAgentTool(ToolCallRequest toolCallRequest) {
        try {
            ToolCall call = buildToolCall(toolCallRequest);

            log.debug("Agent tool call: {} with arguments {}", call.name(), call.arguments());

            ToolResult result = executor.execute(call);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            log.warn("Rejected agent tool call: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(apiError(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Le cerveau agentique décide quels outils appeler, les exécute via la
     * couche {@code agent}, puis laisse le modèle local formuler la réponse.
     * Une demande vide est rejetée en {@code 400} ; une indisponibilité du
     * serveur d'inférence local se traduit par une {@code 503}.</p>
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentChatResponse> chatWithAgent(AgentChatRequest agentChatRequest) {
        // Les erreurs sont portées par les exceptions : IllegalArgumentException -> 400,
        // InferenceException (@ResponseStatus SERVICE_UNAVAILABLE) -> 503.
        return ResponseEntity.ok(agentService.chat(agentChatRequest));
    }

    /**
     * Construit l'appel d'outil immuable à partir de la requête HTTP reçue.
     *
     * <p>Le nom de l'outil est obligatoire et les arguments sont normalisés
     * en map, quel que soit le type renvoyé par le désérialiseur JSON.</p>
     *
     * @param toolCallRequest requête d'appel d'outil, peut être {@code null}
     * @return un {@link ToolCall} prêt à être exécuté
     * @throws IllegalArgumentException si le nom est absent ou les arguments mal formés
     */
    private ToolCall buildToolCall(ToolCallRequest toolCallRequest) {
        Assert.notNull(toolCallRequest, "toolCallRequest cannot be null");
        Assert.hasText(toolCallRequest.getName(), "tool name is mandatory");

        return new ToolCall(toolCallRequest.getName(), extractArguments(toolCallRequest.getArguments()));
    }

    /**
     * Construit l'erreur normalisée renvoyée pour un appel d'outil invalide,
     * conformément au schéma {@code ApiError} du contrat OpenAPI.
     *
     * @param status  code HTTP de la réponse
     * @param message message explicatif destiné au client et au LLM
     * @return un {@link ApiError} renseigné
     */
    private ApiError apiError(HttpStatus status, String message) {
        ApiError error = new ApiError();
        error.setStatus(status.value());
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        return error;
    }

    /**
     * Normalise les arguments JSON reçus en map {@code String -> Object}.
     *
     * @param rawArguments objet JSON désérialisé par Spring, peut être {@code null}
     * @return les arguments sous forme de map, vide si aucun argument n'a été fourni
     * @throws IllegalArgumentException si les arguments ne sont pas un objet JSON
     */
    private Map<String, Object> extractArguments(Object rawArguments) {
        if (rawArguments == null) {
            return Map.of();
        }
        if (!(rawArguments instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("arguments must be a JSON object");
        }

        Map<String, Object> arguments = new LinkedHashMap<>();
        map.forEach((key, value) -> arguments.put(String.valueOf(key), value));
        return arguments;
    }
}
