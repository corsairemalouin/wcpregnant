package org.manu.wcpregnant.agent;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.agent.inference.ChatMessage;
import org.manu.wcpregnant.agent.inference.InferenceClient;
import org.manu.wcpregnant.agent.inference.InferenceException;
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
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du cerveau agentique : boucle Reason -> Act -> Observe, exécution des
 * outils interceptés, limites d'iterations et propagation des erreurs.
 *
 * <p>L'inférence est mockée : les tests n'appellent jamais le serveur local,
 * ils valident uniquement l'orchestration et les traces produites.</p>
 */
@DisplayName("Tests du service agentique")
class AgentServiceImplTest {

    private static final int MAX_ITERATIONS = 5;

    /** Nom du modèle simulé dans les tests : jamais d'appel réseau réel. */
    private static final String MODEL = "local-model";

    @Mock
    private InferenceClient inferenceClient;

    @Mock
    private ToolRegistry registry;

    @Mock
    private ToolExecutor executor;

    @Mock
    private AgentToolConfig config;

    private AgentServiceImpl agentService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(config.getInferenceModel()).thenReturn(MODEL);
        when(config.getMaxIterations()).thenReturn(MAX_ITERATIONS);
        agentService = new AgentServiceImpl(inferenceClient, registry, executor,
                new ObjectMapper(), config);
        when(registry.openAiTools()).thenReturn(List.of(Map.of("type", "function")));
    }

    /**
     * Réponse d'inférence purement textuelle (fin de conversation).
     *
     * @param content message du modèle
     * @return une réponse sans appel d'outil
     */
    private static InferenceResponse textAnswer(String content) {
        return new InferenceResponse(MODEL, List.of(new InferenceResponse.InferenceChoice(
                new InferenceResponse.InferenceMessage("assistant", content, null), "stop")));
    }

    /**
     * Réponse d'inférence demandant un appel d'outil.
     *
     * @param tool nom de l'outil demandé
     * @param args arguments JSON de l'outil
     * @return une réponse portant un seul {@code tool_call}
     */
    private static InferenceResponse toolCallAnswer(String tool, String args) {
        return new InferenceResponse(MODEL, List.of(new InferenceResponse.InferenceChoice(
                new InferenceResponse.InferenceMessage("assistant", null,
                        List.of(ToolCallPayload.of("call-1", tool, args))),
                "tool_calls")));
    }

    // ========================================================================
    // Validation d'entrée
    // ========================================================================

    @Test
    @DisplayName("chat - requête null : NullPointerException")
    void chat_shouldThrow_whenRequestIsNull() {
        assertThrows(NullPointerException.class, () -> agentService.chat(null));
        verify(inferenceClient, never()).chat(any(InferenceRequest.class));
    }

    @Test
    @DisplayName("chat - message vide : IllegalArgumentException")
    void chat_shouldThrow_whenMessageIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> agentService.chat(AgentChatRequest.builder().message("   ").build()));
        verify(inferenceClient, never()).chat(any(InferenceRequest.class));
    }

    @Test
    @DisplayName("chat - itérations non positives : IllegalArgumentException")
    void chat_shouldThrow_whenMaxIterationsIsNotPositive() {
        AgentChatRequest request = AgentChatRequest.builder().message("hello").maxIterations(0).build();
        assertThrows(IllegalArgumentException.class, () -> agentService.chat(request));
    }

    // ========================================================================
    // Réponse directe du modèle
    // ========================================================================

    @Test
    @DisplayName("chat - réponse textuelle : une seule itération, aucune trace")
    void chat_shouldReturnModelAnswer_whenNoToolCallIsRequested() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(textAnswer("La toilette la plus proche est à 120 m."));

        // Act
        AgentChatResponse response = agentService.chat(
                AgentChatRequest.builder().message("Quelle est la toilette la plus proche ?").build());

        // Assert
        assertEquals("La toilette la plus proche est à 120 m.", response.getAnswer());
        assertEquals("local-model", response.getModel());
        assertEquals(1, response.getIterations());
        assertThat(response.getToolCalls()).isEmpty();
        assertThat(response.getElapsedMillis()).isNotNull();
        verify(executor, never()).execute(any(ToolCall.class));
    }

    @Test
    @DisplayName("chat - conversation : prompt système puis message utilisateur")
    void chat_shouldSeedConversation_withSystemPromptAndUserMessage() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class))).thenReturn(textAnswer("ok"));

        // Act
        agentService.chat(AgentChatRequest.builder().message("Ouvre la liste des WC").build());

        // Assert
        ArgumentCaptor<InferenceRequest> captor = ArgumentCaptor.forClass(InferenceRequest.class);
        verify(inferenceClient).chat(captor.capture());
        InferenceRequest sent = captor.getValue();
        assertEquals("local-model", sent.model());
        assertThat(sent.tools()).hasSize(1);
        assertThat(sent.messages()).hasSize(2);
        assertEquals("system", sent.messages().get(0).role());
        assertThat(sent.messages().get(0).content()).contains("WcPregnant assistant");
        assertEquals("user", sent.messages().get(1).role());
        assertEquals("Ouvre la liste des WC", sent.messages().get(1).content());
    }

    @Test
    @DisplayName("chat - position fournie : contexte géographique injecté")
    void chat_shouldIncludePositionContext_whenPositionIsProvided() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class))).thenReturn(textAnswer("ok"));

        // Act
        agentService.chat(AgentChatRequest.builder()
                .message("la plus proche")
                .position(GeoPosition.builder().latitude(48.87).longitude(2.35).build())
                .build());

        // Assert
        ArgumentCaptor<InferenceRequest> captor = ArgumentCaptor.forClass(InferenceRequest.class);
        verify(inferenceClient).chat(captor.capture());
        assertThat(captor.getValue().messages().get(0).content()).contains("latitude 48.87");
    }
    // ========================================================================
    // Boucle Reason -> Act -> Observe
    // ========================================================================

    @Test
    @DisplayName("chat - appel d'outil : exécution, trace et relance du modèle")
    void chat_shouldExecuteToolAndLoop_whenModelRequestsAToolCall() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("search_toilettes_proches", "{\"latitude\":48.87,\"longitude\":2.35}"))
                .thenReturn(textAnswer("La plus proche est à 120 m."));
        when(executor.execute(new ToolCall("search_toilettes_proches", Map.of("latitude", 48.87, "longitude", 2.35))))
                .thenReturn(new ToolResult("search_toilettes_proches", "OK", 1, List.of(Map.of("distance_metres", 120)), null));

        // Act
        AgentChatResponse response = agentService.chat(
                AgentChatRequest.builder().message("la toilette la plus proche").build());

        // Assert
        assertEquals("La plus proche est à 120 m.", response.getAnswer());
        assertEquals(2, response.getIterations());
        assertThat(response.getToolCalls()).hasSize(1);
        AgentToolCallTrace trace = response.getToolCalls().get(0);
        assertEquals("search_toilettes_proches", trace.getTool());
        assertEquals(AgentToolCallTrace.StatusEnum.OK, trace.getStatus());
        assertThat(trace.getResult()).isNotNull();
        verify(executor).execute(new ToolCall("search_toilettes_proches", Map.of("latitude", 48.87, "longitude", 2.35)));
    }

    @Test
    @DisplayName("chat - observation : le résultat de l'outil est réinjecté comme message tool")
    void chat_shouldReinjectToolResult_asToolMessage() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("get_details_toilette", "{\"id\":1}"))
                .thenReturn(textAnswer("Voici les détails."));
        when(executor.execute(any(ToolCall.class)))
                .thenReturn(new ToolResult("get_details_toilette", "OK", 1, Map.of("libelle", "WC Gare"), null));

        // Act
        agentService.chat(AgentChatRequest.builder().message("donne le détail de la 1").build());

        // Assert
        ArgumentCaptor<InferenceRequest> captor = ArgumentCaptor.forClass(InferenceRequest.class);
        verify(inferenceClient, times(2)).chat(captor.capture());
        InferenceRequest secondCall = captor.getAllValues().get(1);
        assertThat(secondCall.messages()).hasSize(4);
        ChatMessage observation = secondCall.messages().get(3);
        assertEquals("tool", observation.role());
        assertEquals("call-1", observation.toolCallId());
        assertThat(observation.content()).contains("WC Gare");
    }

    @Test
    @DisplayName("chat - modèle bloqué en boucle : arrêt à maxIterations")
    void chat_shouldStop_whenMaxIterationsIsReached() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("list_toilettes", "{}"));
        when(executor.execute(any(ToolCall.class)))
                .thenReturn(new ToolResult("list_toilettes", "OK", 0, List.of(), null));

        // Act
        AgentChatResponse response = agentService.chat(
                AgentChatRequest.builder().message("liste").maxIterations(3).build());

        // Assert
        assertEquals(3, response.getIterations());
        assertThat(response.getAnswer()).contains("could not produce a final answer");
        assertThat(response.getToolCalls()).hasSize(3);
        verify(inferenceClient, times(3)).chat(any(InferenceRequest.class));
    }

    @Test
    @DisplayName("chat - outil en erreur : trace ERROR et poursuite de la conversation")
    void chat_shouldRecordErrorTrace_whenExecutorThrows() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("gerer_equipement_crud", "{\"action\":\"DELETE\",\"id\":999999}"))
                .thenReturn(textAnswer("Je n'ai pas pu supprimer cette toilette."));
        when(executor.execute(any(ToolCall.class)))
                .thenThrow(new IllegalArgumentException("Toilet with id 999999 not found"));

        // Act
        AgentChatResponse response = agentService.chat(
                AgentChatRequest.builder().message("supprime la 999999").build());

        // Assert
        assertThat(response.getToolCalls()).hasSize(1);
        AgentToolCallTrace trace = response.getToolCalls().get(0);
        assertEquals(AgentToolCallTrace.StatusEnum.ERROR, trace.getStatus());
        assertEquals("Toilet with id 999999 not found", trace.getMessage());
        assertEquals("Je n'ai pas pu supprimer cette toilette.", response.getAnswer());
        assertEquals(2, response.getIterations());
    }

    @Test
    @DisplayName("chat - appel d'outil sans nom : trace ERROR, aucune exécution")
    void chat_shouldIgnoreToolCall_withoutFunctionName() {
        // Arrange
        InferenceResponse malformed = new InferenceResponse(MODEL, List.of(new InferenceResponse.InferenceChoice(
                new InferenceResponse.InferenceMessage("assistant", null,
                        List.of(ToolCallPayload.of("call-x", null, "{}"))), "tool_calls")));
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(malformed)
                .thenReturn(textAnswer("Je ne peux pas exécuter cet outil."));

        // Act
        AgentChatResponse response = agentService.chat(AgentChatRequest.builder().message("aide").build());

        // Assert
        assertThat(response.getToolCalls()).hasSize(1);
        assertEquals(AgentToolCallTrace.StatusEnum.ERROR, response.getToolCalls().get(0).getStatus());
        assertThat(response.getToolCalls().get(0).getTool()).isEqualTo("unknown");
        verify(executor, never()).execute(any(ToolCall.class));
    }

    @Test
    @DisplayName("chat - arguments non JSON : trace ERROR sans crash")
    void chat_shouldReportError_whenArgumentsAreNotJson() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("list_toilettes", "not-json"))
                .thenReturn(textAnswer("Arguments invalides."));

        // Act
        AgentChatResponse response = agentService.chat(AgentChatRequest.builder().message("liste").build());

        // Assert
        assertThat(response.getToolCalls()).hasSize(1);
        assertEquals(AgentToolCallTrace.StatusEnum.ERROR, response.getToolCalls().get(0).getStatus());
        verify(executor, never()).execute(any(ToolCall.class));
    }

    @Test
    @DisplayName("chat - serveur d'inférence indisponible : InferenceException propagée")
    void chat_shouldPropagateInferenceException_whenLocalServerIsDown() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenThrow(new InferenceException("Local inference server is unreachable"));

        // Act / Assert
        assertThrows(InferenceException.class,
                () -> agentService.chat(AgentChatRequest.builder().message("hello").build()));
    }

    @Test
    @DisplayName("chat - itérations demandées : bornées par la configuration")
    void chat_shouldCapIterations_whenRequestAsksMoreThanConfigured() {
        // Arrange
        when(inferenceClient.chat(any(InferenceRequest.class)))
                .thenReturn(toolCallAnswer("list_toilettes", "{}"));
        when(executor.execute(any(ToolCall.class)))
                .thenReturn(new ToolResult("list_toilettes", "OK", 0, List.of(), null));

        // Act
        AgentChatResponse response = agentService.chat(
                AgentChatRequest.builder().message("liste").maxIterations(100).build());

        // Assert
        assertEquals(MAX_ITERATIONS, response.getIterations());
    }
}
