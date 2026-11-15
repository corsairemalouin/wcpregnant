package org.manu.wcpregnant.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.agent.AgentService;
import org.manu.wcpregnant.agent.inference.InferenceException;
import org.manu.wcpregnant.agent.tool.ToolCall;
import org.manu.wcpregnant.agent.tool.ToolExecutor;
import org.manu.wcpregnant.agent.tool.ToolRegistry;
import org.manu.wcpregnant.agent.tool.ToolResult;
import org.manu.wcpregnant.web.dto.ApiError;
import org.manu.wcpregnant.web.dto.AgentChatRequest;
import org.manu.wcpregnant.web.dto.AgentChatResponse;
import org.manu.wcpregnant.web.dto.ToolCallRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du contrôleur agentique : découverte du registre et délégation
 * de l'appel d'outil à l'exécuteur.
 */
class AgentApiImplTest {

    private AgentApiImpl api;

    @Mock
    private ToolRegistry registry;

    @Mock
    private ToolExecutor executor;

    @Mock
    private AgentService agentService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        api = new AgentApiImpl(registry, executor, agentService);
    }

    // ========================================================================
    // getAgentTools
    // ========================================================================

    @Test
    void getAgentTools_shouldReturnTheRegistryEnvelopes() {
        // Arrange
        when(registry.openAiTools()).thenReturn(List.of(Map.of("type", "function")));

        // Act
        ResponseEntity<List<Object>> response = api.getAgentTools();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getAgentTools_shouldReturnEmptyList_whenRegistryIsEmpty() {
        // Arrange
        when(registry.openAiTools()).thenReturn(List.of());

        // Act
        ResponseEntity<List<Object>> response = api.getAgentTools();

        // Assert
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAgentTools_shouldBeCalledThroughTheRegistryAbstraction() {
        // Arrange / Act
        api.getAgentTools();

        // Assert
        verify(registry).openAiTools();
    }

    // ========================================================================
    // callAgentTool
    // ========================================================================

    @Test
    void callAgentTool_shouldDelegateToExecutor_withNormalizedArguments() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder()
                .name("search_toilettes_proches")
                .arguments(Map.of("latitude", 48.6, "longitude", -1.9))
                .build();
        when(executor.execute(new ToolCall("search_toilettes_proches", Map.of("latitude", 48.6, "longitude", -1.9))))
                .thenReturn(new ToolResult("search_toilettes_proches", "OK", 0, List.of(), null));

        // Act
        ResponseEntity<Object> response = api.callAgentTool(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(executor).execute(new ToolCall("search_toilettes_proches", Map.of("latitude", 48.6, "longitude", -1.9)));
    }

    @Test
    void callAgentTool_shouldReturnEmptyArguments_whenNoneProvided() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder().name("get_details_toilette").build();
        when(executor.execute(new ToolCall("get_details_toilette", Map.of())))
                .thenReturn(new ToolResult("get_details_toilette", "OK", 1, "toilet", null));

        // Act
        ResponseEntity<Object> response = api.callAgentTool(request);

        // Assert
        assertThat(response.getBody()).isNotNull();
        verify(executor).execute(new ToolCall("get_details_toilette", Map.of()));
    }

    @Test
    void callAgentTool_shouldReturnBadRequest_whenRequestOrNameIsNull() {
        // Arrange / Act
        ResponseEntity<Object> nullRequest = api.callAgentTool(null);
        ResponseEntity<Object> blankName = api.callAgentTool(ToolCallRequest.builder().name("  ").build());

        // Assert
        assertThat(nullRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blankName.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) blankName.getBody()).getMessage()).isEqualTo("tool name is mandatory");
        assertThat(((ApiError) blankName.getBody()).getStatus()).isEqualTo(400);
        assertThat(((ApiError) blankName.getBody()).getTimestamp()).isNotNull();
    }

    @Test
    void callAgentTool_shouldReturnBadRequest_whenArgumentsAreNotAnObject() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder()
                .name("get_details_toilette")
                .arguments("not-an-object")
                .build();

        // Act
        ResponseEntity<Object> response = api.callAgentTool(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("arguments must be a JSON object");
        verify(executor, never()).execute(any(ToolCall.class));
    }

    @Test
    void callAgentTool_shouldReturnApiError_whenExecutorRejectsUnknownTool() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder().name("fly_to_the_moon").build();
        when(executor.execute(any(ToolCall.class)))
                .thenThrow(new IllegalArgumentException("Unknown tool 'fly_to_the_moon'"));

        // Act
        ResponseEntity<Object> response = api.callAgentTool(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) response.getBody()).getStatus()).isEqualTo(400);
        assertThat(((ApiError) response.getBody()).getMessage())
                .isEqualTo("Unknown tool 'fly_to_the_moon'");
    }

    @Test
    void callAgentTool_shouldReturnApiError_whenToolArgumentsAreInvalid() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder()
                .name("search_toilettes_proches")
                .arguments(Map.of("latitude", 200))
                .build();
        when(executor.execute(any(ToolCall.class)))
                .thenThrow(new IllegalArgumentException("latitude is required"));

        // Act
        ResponseEntity<Object> response = api.callAgentTool(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("latitude is required");
    }

    @Test
    void callAgentTool_shouldNotSwallowAccessDenied_whenAdminRoleIsMissing() {
        // Arrange
        ToolCallRequest request = ToolCallRequest.builder().name("gerer_equipement_crud").build();
        when(executor.execute(any(ToolCall.class)))
                .thenThrow(new AccessDeniedException("The tool gerer_equipement_crud requires the ADMIN role"));

        // Act / Assert
        assertThrows(AccessDeniedException.class, () -> api.callAgentTool(request));
    }
    // ========================================================================
    // Conversation agentique
    // ========================================================================

    @Test
    void chatWithAgent_shouldDelegateToAgentService() {
        // Arrange
        AgentChatRequest request = AgentChatRequest.builder().message("la plus proche").build();
        AgentChatResponse response = AgentChatResponse.builder()
                .answer("La plus proche est à 120 m.")
                .model("Qwen-3.8-Flash-Next")
                .iterations(1)
                .build();
        when(agentService.chat(request)).thenReturn(response);

        // Act
        ResponseEntity<AgentChatResponse> result = api.chatWithAgent(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getAnswer()).isEqualTo("La plus proche est à 120 m.");
        verify(agentService).chat(request);
    }

    @Test
    void chatWithAgent_shouldPropagateIllegalArgumentException_forBadRequestMapping() {
        // Arrange
        AgentChatRequest request = AgentChatRequest.builder().message("").build();
        when(agentService.chat(any(AgentChatRequest.class)))
                .thenThrow(new IllegalArgumentException("message must not be blank"));

        // Act / Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> api.chatWithAgent(request));
        assertThat(exception.getMessage()).isEqualTo("message must not be blank");
    }

    @Test
    void chatWithAgent_shouldPropagateInferenceException_forServiceUnavailableMapping() {
        // Arrange
        AgentChatRequest request = AgentChatRequest.builder().message("hello").build();
        when(agentService.chat(any(AgentChatRequest.class)))
                .thenThrow(new InferenceException("Local inference server is unreachable"));

        // Act / Assert
        InferenceException exception = assertThrows(InferenceException.class,
                () -> api.chatWithAgent(request));
        assertThat(exception.getMessage()).isEqualTo("Local inference server is unreachable");
    }
}
