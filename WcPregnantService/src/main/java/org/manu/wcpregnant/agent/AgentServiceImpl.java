package org.manu.wcpregnant.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.manu.wcpregnant.agent.inference.ChatMessage;
import org.manu.wcpregnant.agent.inference.InferenceClient;
import org.manu.wcpregnant.agent.inference.InferenceRequest;
import org.manu.wcpregnant.agent.inference.InferenceResponse;
import org.manu.wcpregnant.agent.inference.ToolCallPayload;
import org.manu.wcpregnant.agent.tool.ToolCall;
import org.manu.wcpregnant.agent.tool.ToolExecutor;
import org.manu.wcpregnant.agent.tool.ToolRegistry;
import org.manu.wcpregnant.agent.tool.ToolResult;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.manu.wcpregnant.web.dto.AgentChatRequest;
import org.manu.wcpregnant.web.dto.AgentChatResponse;
import org.manu.wcpregnant.web.dto.AgentToolCallTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Orchestrateur agentique "Reason -&gt; Act -&gt; Observe" s'appuyant sur le modèle
 * servi localement par Strata AI (Qwen-3.8-Flash-Next, API compatible OpenAI).
 *
 * <p>Boucle de conversation :</p>
 * <ol>
 *   <li><strong>Reason</strong> : la conversation (prompt système, question de
 *       l'utilisateur, derniers résultats d'outils) est envoyée au modèle local
 *       accompagnée du registre d'outils au format OpenAI Function Calling.</li>
 *   <li><strong>Act</strong> : si le modèle demande des appels d'outils, chaque
 *       appel est intercepté et exécuté côté serveur par {@link ToolExecutor},
 *       lui-même adapté aux services métier existants (Haversine, CRUD, notation)
 *       sous le RBAC en vigueur.</li>
 *   <li><strong>Observe</strong> : le résultat normalisé de chaque outil est
 *       réinjecté dans la conversation comme message {@code tool}, et la boucle
 *       recommence jusqu'à une réponse textuelle ou l'atteinte du nombre maximal
 *       d'itérations.</li>
 * </ol>
 *
 * <p>La couche agent est un simple adaptateur : elle ne contient ni SQL, ni
 * mapping manuel, et ne contourne jamais les contraintes de sécurité.</p>
 *
 * @see AgentService
 * @see InferenceClient
 * @see ToolRegistry
 */
@Service
public class AgentServiceImpl implements AgentService {

    /** Logger de la couche agentique. */
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    /** Garde-fou absolu : le contrat OpenAPI borne {@code maxIterations} à 10. */
    private static final int HARD_MAX_ITERATIONS = 10;

    /** Prompt système définissant la mission de l'agent et ses règles d'usage des outils. */
    private static final String SYSTEM_PROMPT = """
            You are the WcPregnant assistant, a local agent helping pregnant people find
            public toilets accessible near them. You answer in French.
            Rules:
            - Use the tools to get real data from the WcPregnant database. Never invent
              an equipment, an address, an opening hour or a rating.
            - Prefer search_toilettes_proches when the user gives or implies a location.
            - Use get_details_toilette when the user asks about a specific equipment.
            - Use gerer_equipement_crud only when the user explicitly asks to create,
              update or delete an equipment.
            - When you have enough information, answer with a short, plain-text summary:
              name, address, distance in metres, accessibility and rating.
            - If a tool fails, explain the failure instead of guessing.
            """;

    /** Client d'inférence local (Strata AI). */
    private final InferenceClient inferenceClient;

    /** Registre des outils exposés au modèle. */
    private final ToolRegistry toolRegistry;

    /** Exécuteur des outils interceptés. */
    private final ToolExecutor toolExecutor;

    /** Mapper Jackson utilisé pour les échanges JSON avec le modèle. */
    private final ObjectMapper objectMapper;

    /** Nom du modèle servi localement. */
    private final String model;

    /** Nombre maximal d'allers-retours avec le modèle. */
    private final int maxIterations;

    /**
     * Construit l'orchestrateur agentique.
     *
     * @param inferenceClient client d'inférence local. Ne doit pas être {@code null}.
     * @param toolRegistry    registre des outils. Ne doit pas être {@code null}.
     * @param toolExecutor    exécuteur des outils. Ne doit pas être {@code null}.
     * @param objectMapper    mapper Jackson. Ne doit pas être {@code null}.
     * @param config          configuration agentique (modèle local, budget d'itérations).
     *                        Ne doit pas être {@code null}.
     */
    public AgentServiceImpl(InferenceClient inferenceClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            ObjectMapper objectMapper,
            AgentToolConfig config) {
        this.inferenceClient = Objects.requireNonNull(inferenceClient, "inferenceClient must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.toolExecutor = Objects.requireNonNull(toolExecutor, "toolExecutor must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.model = config.getInferenceModel();
        // La configuration reste le budget d'inférence de l'exploitation ; le
        // garde-fou absolu du contrat OpenAPI (10) évite une boucle infinie.
        this.maxIterations = Math.min(Math.max(1, config.getMaxIterations()), HARD_MAX_ITERATIONS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>La boucle s'arrête dès que le modèle renvoie une réponse textuelle ou
     * lorsque le nombre maximal d'itérations est atteint.</p>
     */
    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String question = request.getMessage();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        int iterationsLimit = resolveIterationsLimit(request.getMaxIterations());
        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(ChatMessage.system(SYSTEM_PROMPT + positionContext(request)));
        conversation.add(ChatMessage.user(question));

        List<AgentToolCallTrace> traces = new ArrayList<>();
        long startedAt = System.currentTimeMillis();
        int iterations = 0;
        String answer = null;

        while (iterations < iterationsLimit) {
            iterations++;
            InferenceRequest inferenceRequest =
                    new InferenceRequest(model, conversation, toolRegistry.openAiTools());
            InferenceResponse response = inferenceClient.chat(inferenceRequest);
            log.debug("Inference iteration {}: {} messages sent, {} tools declared",
                    iterations, conversation.size(), toolRegistry.openAiTools().size());

            if (response.hasToolCalls()) {
                conversation.add(ChatMessage.assistant(response.content(), response.toolCalls()));
                for (ToolCallPayload payload : response.toolCalls()) {
                    traces.add(executeAndObserve(payload, conversation));
                }
                continue;
            }

            answer = response.content();
            break;
        }

        if (answer == null || answer.isBlank()) {
            log.warn("Agent stopped after {} iterations without a final answer", iterationsLimit);
            answer = "I could not produce a final answer within " + iterationsLimit
                    + " inference iterations. Please narrow your question.";
        }

        int elapsedMillis = (int) (System.currentTimeMillis() - startedAt);
        log.info("Agent conversation finished after {} iterations, {} tool calls, {} ms",
                iterations, traces.size(), elapsedMillis);

        return AgentChatResponse.builder()
                .answer(answer)
                .model(model)
                .iterations(iterations)
                .toolCalls(traces)
                .elapsedMillis(elapsedMillis)
                .build();
    }

    /**
     * Détermine la borne d'itérations réellement appliquée.
     *
     * @param requested borne demandée par le client, {@code null} pour utiliser la configuration
     * @return le nombre maximal d'allers-retours avec le modèle
     * @throws IllegalArgumentException si la valeur demandée est inférieure à 1
     */
    private int resolveIterationsLimit(Integer requested) {
        if (requested == null) {
            return maxIterations;
        }
        if (requested < 1) {
            throw new IllegalArgumentException("maxIterations must be at least 1");
        }
        // La borne configurée prime : un client ne peut pas dépasser le budget
        // d'inférence défini par l'exploitation, ni le garde-fou absolu du contrat.
        return Math.min(Math.min(requested, maxIterations), HARD_MAX_ITERATIONS);
    }

    /**
     * Contexte de localisation injecté dans le prompt système lorsque la position
     * est fournie, afin d'aider le modèle à choisir un outil de proximité.
     *
     * @param request requête agentique
     * @return une ligne de contexte, ou une chaîne vide si aucune position n'est donnée
     */
    private String positionContext(AgentChatRequest request) {
        if (request.getPosition() == null) {
            return "";
        }
        return String.format(
                "\nUser position: latitude %s, longitude %s.",
                request.getPosition().getLatitude(),
                request.getPosition().getLongitude());
    }

    /**
     * Intercepte un appel d'outil demandé par le modèle, l'exécute côté serveur
     * et réinjecte son résultat dans la conversation (phase "Observe").
     *
     * @param payload      appel d'outil reçu du modèle. Ne doit pas être {@code null}.
     * @param conversation conversation à enrichir avec le résultat de l'outil
     * @return la trace de l'exécution destinée à la réponse API
     */
    private AgentToolCallTrace executeAndObserve(ToolCallPayload payload, List<ChatMessage> conversation) {
        Objects.requireNonNull(payload, "payload must not be null");
        String toolName = payload.toolName();

        if (toolName == null || toolName.isBlank()) {
            log.warn("Model requested a tool call without a function name, ignoring it");
            ToolResult invalid = new ToolResult("unknown", "ERROR", 0, null, "Tool call has no function name");
            conversation.add(ChatMessage.tool(payload.id(), toJson(invalid)));
            return toTrace(invalid, Map.of());
        }

        Map<String, Object> arguments = parseArguments(toolName, payload.rawArguments());
        if (arguments == null) {
            log.warn("Ignoring tool {} : arguments are not valid JSON", toolName);
            ToolResult invalidArguments = new ToolResult(toolName, "ERROR", 0, null,
                    "Tool arguments are not valid JSON: " + payload.rawArguments());
            conversation.add(ChatMessage.tool(payload.id(), toJson(invalidArguments)));
            return toTrace(invalidArguments, Map.of());
        }

        ToolResult result;
        try {
            result = toolExecutor.execute(new ToolCall(toolName, arguments));
        } catch (Exception ex) {
            log.warn("Tool {} failed: {}", toolName, ex.getMessage());
            result = new ToolResult(toolName, "ERROR", 0, null, ex.getMessage());
        }

        conversation.add(ChatMessage.tool(payload.id(), toJson(result)));
        return toTrace(result, arguments);
    }

    /**
     * Désérialise les arguments JSON d'un appel d'outil.
     *
     * @param toolName nom de l'outil, utilisé dans les logs
     * @param rawJson  arguments bruts fournis par le modèle
     * @return les arguments sous forme de map, ou {@code null} si le JSON est invalide
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String toolName, String rawJson) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(rawJson, Map.class);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            log.warn("Invalid arguments JSON for tool {}: {}", toolName, ex.getMessage());
            return null;
        }
    }

    /**
     * Convertit un objet en JSON destiné au modèle.
     *
     * @param value objet à sérialiser
     * @return le JSON, ou {@code "{}"} en cas d'échec de sérialisation
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Unable to serialize tool result: {}", ex.getMessage());
            return "{}";
        }
    }

    /**
     * Transforme un {@link ToolResult} en trace exposée par l'API.
     *
     * @param result    résultat normalisé de l'outil
     * @param arguments arguments effectivement transmis à l'outil
     * @return une {@link AgentToolCallTrace} au statut {@code OK} ou {@code ERROR}
     */
    private AgentToolCallTrace toTrace(ToolResult result, Map<String, Object> arguments) {
        boolean failed = "ERROR".equalsIgnoreCase(result.status());
        return AgentToolCallTrace.builder()
                .tool(result.tool())
                .arguments(arguments)
                .result(result.data())
                .status(failed ? AgentToolCallTrace.StatusEnum.ERROR : AgentToolCallTrace.StatusEnum.OK)
                .message(result.warning())
                .build();
    }
}

