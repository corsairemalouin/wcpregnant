package org.manu.wcpregnant.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.agent.tool.ToiletToolRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test bout en bout de la conversation agentique {@code POST /agent/chat}.
 *
 * <p>Un <strong>faux serveur d'inférence local</strong> (JDK {@code HttpServer},
 * API compatible OpenAI) est démarré sur {@code 127.0.0.1:45678} : il imite le
 * comportement de Strata AI en renvoyant d'abord un appel d'outil, puis une
 * réponse textuelle dès qu'une observation d'outil lui est réinjectée.
 * L'URL de ce serveur est injectée dans le contexte Spring via
 * {@code @SpringBootTest(properties = ...)}, ce qui évite toute dépendance à une
 * inférence réelle. Aucun appel réseau externe n'est effectué, ce qui respecte la
 * contrainte "inférence 100 % locale" et rend le test déterministe.</p>
 */
@SpringBootTest(properties = {"agent.inference.base-url=http://127.0.0.1:45678/v1"})
@ActiveProfiles("dev") // UserDetailsService en mémoire : manu (USER) / admin (USER, ADMIN)
@DisplayName("Test bout en bout de l'agent (faux serveur d'inférence local)")
class AgentChatEndToEndTest {

    /** Marqueur envoyé dans la question pour déclencher un appel d'outil CRUD (test RBAC). */
    private static final String CRUD_MARKER = "supprimer la toilette 999";

    /**
     * Port du faux serveur d'inférence. Il doit être fixé pour pouvoir être injecté
     * dans le contexte Spring par {@code @SpringBootTest(properties = ...)}.
     *
     * <p>Choisi en dehors des plages réservées par Hyper-V (les ports 58301-58302
     * sont indisponibles sur les postes Windows équipés de Virtual Machine).</p>
     */
    private static final int FAKE_INFERENCE_PORT = 45678;

    /** Nombre de tentatives de bind : un JVM résiduel peut occuper brièvement le port. */
    private static final int BIND_RETRY_COUNT = 5;

    /** Corps de la dernière requête reçue par le faux serveur, pour inspecter la conversation. */
    private static final AtomicReference<String> lastInferenceRequest = new AtomicReference<>("");

    /** Nombre de requêtes reçues par le faux serveur. */
    private static final AtomicInteger inferenceRequestCount = new AtomicInteger(0);

    /** Faux serveur d'inférence OpenAI-compatible, démarré au chargement de la classe. */
    private static final HttpServer fakeInferenceServer = startFakeInferenceServer();

    private final WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    AgentChatEndToEndTest(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    /**
     * Démarre le faux serveur d'inférence sur un port éphémère de {@code 127.0.0.1}
     * et injecte son URL réelle comme propriété système, lue par
     * {@code AgentToolConfig} via {@code agent.inference.base-url}.
     *
     * <p>Le port n'est pas fixé : un port réservé par l'OS (plages Hyper-V sous
     * Windows) rendrait le bind impossible et rendrait la suite instable.</p>
     *
     * @return le serveur démarré
     */
    /**
     * Démarre le faux serveur d'inférence sur le port fixé par
     * {@code FAKE_INFERENCE_PORT}, URL injectée dans le contexte Spring par
     * {@code @SpringBootTest(properties = ...)} et lue par {@code AgentToolConfig}.
     *
     * <p>Le bind est tenté plusieurs fois : un JVM résiduel (exécution interrompue
     * de la suite) peut occuper brièvement le port et rendre l'échec transitoire.</p>
     *
     * @return le serveur démarré
     */
    private static HttpServer startFakeInferenceServer() {
        HttpServer server = null;
        IOException lastError = null;
        for (int attempt = 1; attempt <= BIND_RETRY_COUNT && server == null; attempt++) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", FAKE_INFERENCE_PORT), 0);
            } catch (IOException ex) {
                lastError = ex;
                sleepBeforeRetry(attempt);
            }
        }
        if (server == null) {
            throw new IllegalStateException(
                    "Unable to start the fake local inference server on port " + FAKE_INFERENCE_PORT, lastError);
        }
        server.createContext("/v1/chat/completions", AgentChatEndToEndTest::serveChatCompletions);
        server.start();
        return server;
    }

    /**
     * Patient entre deux tentatives de bind du faux serveur d'inférence.
     *
     * @param attempt numéro de la tentative déjà effectuée
     */
    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Répond comme le ferait un modèle local : un appel d'outil tant que la
     * conversation ne contient aucune observation, puis une réponse textuelle.
     *
     * @param exchange échange HTTP reçu par le faux serveur
     * @throws IOException en cas d'écriture impossible de la réponse
     */
    private static void serveChatCompletions(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        inferenceRequestCount.incrementAndGet();
        lastInferenceRequest.set(body);

        boolean hasObservation = body.contains("\"role\":\"tool\"");
        byte[] answer = (hasObservation ? finalAnswerJson() : toolCallJson(body))
                .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, answer.length);
        try (OutputStream bodyStream = exchange.getResponseBody()) {
            bodyStream.write(answer);
        }
    }

    /**
     * Réponse "tool_calls" du faux modèle, choisie selon la question posée.
     *
     * @param body corps de la requête d'inférence
     * @return la réponse JSON au format OpenAI
     */
    private static String toolCallJson(String body) {
        if (body.contains(CRUD_MARKER)) {
            return """
                    {"model":"Qwen-3.8-Flash-Next","choices":[{"message":{"role":"assistant","tool_calls":[
                    {"id":"call-crud","type":"function","function":{"name":"gerer_equipement_crud",
                    "arguments":"{\\"action\\":\\"DELETE\\",\\"id\\":999}"}}]},
                    "finish_reason":"tool_calls"}]}
                    """;
        }

        return """
                {"model":"Qwen-3.8-Flash-Next","choices":[{"message":{"role":"assistant","tool_calls":[
                {"id":"call-proximity","type":"function","function":{"name":"search_toilettes_proches",
                "arguments":"{\\"latitude\\":48.8566,\\"longitude\\":2.3522,\\"rayon_metres\\":500}"}}]},
                "finish_reason":"tool_calls"}]}
                """;
    }

    /**
     * Réponse textuelle du faux modèle une fois les outils exécutés.
     *
     * @return la réponse JSON au format OpenAI
     */
    private static String finalAnswerJson() {
        return """
                {"model":"Qwen-3.8-Flash-Next","choices":[{"message":{"role":"assistant",
                "content":"Plusieurs toilettes accessibles ont ete trouvees dans un rayon de 500 metres."},
                "finish_reason":"stop"}]}
                """;
    }

    /**
     * Arrête le faux serveur d'inférence local.
     */
    @AfterAll
    static void stopFakeInferenceServer() {
        fakeInferenceServer.stop(0);
    }

    /**
     * Réinitialise les compteurs du faux serveur et reconstruit MockMvc avec la
     * chaîne de sécurité, comme les autres tests d'intégration (Spring Boot 4.x
     * n'expose plus {@code @AutoConfigureMockMvc}).
     */
    @BeforeEach
    void setUp() {
        inferenceRequestCount.set(0);
        lastInferenceRequest.set("");
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ========================================================================
    // Boucle Reason -> Act -> Observe
    // ========================================================================

    @Test
    @DisplayName("POST /agent/chat - boucle complete : outil execute puis reponse textuelle")
    void chat_shouldExecuteToolAndAnswer_whenModelRequestsAToolCall() throws Exception {
        // Arrange
        String payload = """
                {"message":"Y a-t-il des toilettes accessibles pres de la mairie de Paris ?",
                 "position":{"latitude":48.8566,"longitude":2.3522}}
                """;

        // Act & Assert
        mockMvc.perform(post("/agent/chat")
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Qwen-3.8-Flash-Next"))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.toolCalls.length()").value(1))
                .andExpect(jsonPath("$.toolCalls[0].tool").value(ToiletToolRegistry.SEARCH_NEAR_TOILETS))
                .andExpect(jsonPath("$.toolCalls[0].status").value("OK"))
                .andExpect(jsonPath("$.toolCalls[0].arguments.latitude").value(48.8566))
                .andExpect(jsonPath("$.answer").value(
                        "Plusieurs toilettes accessibles ont ete trouvees dans un rayon de 500 metres."));

        // Assert : le faux serveur a bien été sollicité deux fois (Reason, puis Observe).
        assertThat(inferenceRequestCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /agent/chat - l'observation d'outil est reinjectee dans la conversation")
    void chat_shouldReinjectToolObservation_asToolMessage() throws Exception {
        // Arrange
        String payload = """
                {"message":"Donne-moi la liste des WC a proximite",
                 "position":{"latitude":48.8566,"longitude":2.3522}}
                """;

        // Act
        mockMvc.perform(post("/agent/chat")
                .with(httpBasic("manu", "manu"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isOk());

        // Assert : la seconde requête contient le message assistant avec tool_calls
        // et le message "tool" portant le résultat de l'exécuteur.
        assertThat(lastInferenceRequest.get())
                .contains("\"role\":\"tool\"")
                .contains(ToiletToolRegistry.SEARCH_NEAR_TOILETS);
    }

    @Test
    @DisplayName("POST /agent/chat - RBAC : un appel CRUD sans rôle ADMIN trace une erreur, sans crash")
    void chat_shouldTraceToolError_whenCrudToolIsCalledWithoutAdminRole() throws Exception {
        // Arrange
        String payload = """
                {"message":"Peux-tu supprimer la toilette 999 ?"}
                """;

        // Act & Assert
        mockMvc.perform(post("/agent/chat")
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolCalls[0].tool").value(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD))
                .andExpect(jsonPath("$.toolCalls[0].status").value("ERROR"))
                .andExpect(jsonPath("$.toolCalls[0].message").value(
                        "The tool gerer_equipement_crud requires the ADMIN role"));
    }

    @Test
    @DisplayName("POST /agent/chat - garde-fou : la boucle s'arrête à maxIterations")
    void chat_shouldStopAtMaxIterations_whenModelNeverAnswers() throws Exception {
        // Arrange
        String payload = """
                {"message":"Liste les toilettes", "maxIterations": 1}
                """;

        // Act & Assert
        mockMvc.perform(post("/agent/chat")
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iterations").value(1))
                .andExpect(jsonPath("$.answer").value(
                        "I could not produce a final answer within 1 inference iterations. Please narrow your question."));
    }
}