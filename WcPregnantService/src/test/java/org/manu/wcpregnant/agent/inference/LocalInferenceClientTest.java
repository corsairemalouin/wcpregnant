package org.manu.wcpregnant.agent.inference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du client d'inférence local : sérialisation de la requête OpenAI-compatible,
 * analyse de la réponse et comportement en cas d'indisponibilité du serveur local.
 *
 * <p>Le {@code HttpClient} JDK est mocké : aucun appel réseau réel n'est émis,
 * ce qui garantit l'absence de fuite de données vers le cloud pendant les tests.</p>
 */
@DisplayName("Tests du client d'inférence local")
class LocalInferenceClientTest {

    private static final String BASE_URL = "http://localhost:8000/v1";
    private static final String MODEL = "Qwen-3.8-Flash-Next";
    private static final int TIMEOUT_SECONDS = 7;

    @Mock
    private HttpClient httpClient;

    private LocalInferenceClient client;

    /**
     * Fabrique une configuration agentique simulée pour le client d'inférence.
     *
     * @param baseUrl        URL du serveur local d'inférence
     * @param model          nom du modèle servi localement
     * @param timeoutSeconds délai maximal d'une requête en secondes
     * @return un {@link AgentToolConfig} mocké
     */
    private static AgentToolConfig config(String baseUrl, String model, int timeoutSeconds) {
        AgentToolConfig config = mock(AgentToolConfig.class);
        when(config.getInferenceBaseUrl()).thenReturn(baseUrl);
        when(config.getInferenceModel()).thenReturn(model);
        when(config.getInferenceTimeoutSeconds()).thenReturn(timeoutSeconds);
        return config;
    }

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        client = new LocalInferenceClient(config(BASE_URL, MODEL, TIMEOUT_SECONDS),
                httpClient, new ObjectMapper());
    }

    /**
     * Fabrique une réponse HTTP factice pour le client JDK mocké.
     *
     * @param status code de retour simulé
     * @param body   corps JSON simulé
     * @return une {@code HttpResponse} non suspendante
     */
    private static HttpResponse<String> httpResponse(int status, String body) {
        return new HttpResponse<String>() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (a, b) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create(BASE_URL);
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    // ========================================================================
    // Analyse d'une réponse d'inférence
    // ========================================================================

    @Test
    @DisplayName("chat - réponse textuelle : choix et finish_reason analysés")
    void chat_shouldParseChoice_whenModelAnswersWithoutToolCall() throws Exception {
        // Arrange
        String json = """
                {
                  "model": "Qwen-3.8-Flash-Next",
                  "choices": [
                    {
                      "index": 0,
                      "message": { "role": "assistant", "content": "La toilette la plus proche est à 120 m." },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse(200, json));

        // Act
        InferenceResponse response = client.chat(new InferenceRequest("Qwen-3.8-Flash-Next",
                List.of(ChatMessage.user("Quelle est la toilette la plus proche ?")), List.of()));

        // Assert
        assertEquals("Qwen-3.8-Flash-Next", response.model());
        assertThat(response.content()).isEqualTo("La toilette la plus proche est à 120 m.");
        assertThat(response.toolCalls()).isEmpty();
        assertEquals("stop", response.finishReason());
    }

    @Test
    @DisplayName("chat - appels d'outils : tool_calls extraits du choix")
    void chat_shouldParseToolCalls_whenModelRequestsTools() throws Exception {
        // Arrange
        String json = """
                {
                  "model": "local-model",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "call-1",
                            "type": "function",
                            "function": {
                              "name": "search_toilettes_proches",
                              "arguments": "{\\"latitude\\":48.87,\\"longitude\\":2.35}"
                            }
                          }
                        ]
                      },
                      "finish_reason": "tool_calls"
                    }
                  ]
                }
                """;
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse(200, json));

        // Act
        InferenceResponse response = client.chat(new InferenceRequest("local-model",
                List.of(ChatMessage.system("system prompt")), List.of(Map.of("type", "function"))));

        // Assert
        assertThat(response.toolCalls()).hasSize(1);
        ToolCallPayload payload = response.toolCalls().get(0);
        assertEquals("call-1", payload.id());
        assertEquals("search_toilettes_proches", payload.toolName());
        assertEquals("{\"latitude\":48.87,\"longitude\":2.35}", payload.rawArguments());
        assertNull(response.content());
        assertEquals("tool_calls", response.finishReason());
    }

    @Test
    @DisplayName("chat - réponse sans choix : contenu null sans exception")
    void chat_shouldReturnEmptyContent_whenChoicesAreMissing() throws Exception {
        // Arrange
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse(200, "{\"model\":\"local-model\"}"));

        // Act
        InferenceResponse response = client.chat(new InferenceRequest("local-model",
                List.of(ChatMessage.user("hello")), List.of()));

        // Assert
        assertNull(response.content());
        assertThat(response.toolCalls()).isEmpty();
        assertNull(response.finishReason());
    }

    // ========================================================================
    // Construction et envoi de la requête
    // ========================================================================

    @Test
    @DisplayName("chat - requête : endpoint, méthode POST et timeout appliqués")
    void chat_shouldPostToChatCompletions_withConfiguredTimeout() throws Exception {
        // Arrange
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse(200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}"));

        // Act
        client.chat(new InferenceRequest("Qwen-3.8-Flash-Next",
                List.of(ChatMessage.user("Quelle est la toilette la plus proche ?")),
                List.of(Map.of("type", "function"))));

        // Assert
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = captor.getValue();
        assertEquals("POST", sent.method());
        assertEquals(URI.create(BASE_URL + "/chat/completions"), sent.uri());
        assertEquals(Optional.of(Duration.ofSeconds(7)), sent.timeout());
    }

    // ========================================================================
    // Cas d'erreur
    // ========================================================================

    @Test
    @DisplayName("chat - erreur HTTP du serveur local : InferenceException")
    void chat_shouldThrowInferenceException_whenServerReturnsError() throws Exception {
        // Arrange
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse(500, "{\"error\":\"model crashed\"}"));

        // Act / Assert
        InferenceException exception = assertThrows(InferenceException.class,
                () -> client.chat(new InferenceRequest("local-model", List.of(ChatMessage.user("hi")), List.of())));
        assertThat(exception.getMessage()).contains("Local inference server returned HTTP 500");
        assertThat(exception.getMessage()).contains("model crashed");
    }

    @Test
    @DisplayName("chat - serveur local injoignable : InferenceException")
    void chat_shouldThrowInferenceException_whenLocalServerIsUnreachable() throws Exception {
        // Arrange
        when(httpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("Connection refused"));

        // Act / Assert
        InferenceException exception = assertThrows(InferenceException.class,
                () -> client.chat(new InferenceRequest("local-model", List.of(ChatMessage.user("hi")), List.of())));
        assertThat(exception.getMessage()).contains("Unable to reach the local inference server");
        assertThat(exception.getMessage()).contains("Connection refused");
    }

    @Test
    @DisplayName("constructeur - URL d'inférence vide : IllegalArgumentException")
    void constructor_shouldRejectBlankBaseUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocalInferenceClient(config("  ", MODEL, TIMEOUT_SECONDS), httpClient, new ObjectMapper()));
        assertThrows(IllegalArgumentException.class,
                () -> new LocalInferenceClient(config(null, MODEL, TIMEOUT_SECONDS), httpClient, new ObjectMapper()));
    }

    @Test
    @DisplayName("constructeur - modèle vide : IllegalArgumentException")
    void constructor_shouldRejectBlankModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocalInferenceClient(config(BASE_URL, "", TIMEOUT_SECONDS), httpClient, new ObjectMapper()));
    }

    @Test
    @DisplayName("constructeur - dépendances null : NullPointerException")
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
                () -> new LocalInferenceClient(null, httpClient, new ObjectMapper()));
        assertThrows(NullPointerException.class,
                () -> new LocalInferenceClient(config(BASE_URL, MODEL, TIMEOUT_SECONDS), null, new ObjectMapper()));
        assertThrows(NullPointerException.class,
                () -> new LocalInferenceClient(config(BASE_URL, MODEL, TIMEOUT_SECONDS), httpClient, null));
    }
}
