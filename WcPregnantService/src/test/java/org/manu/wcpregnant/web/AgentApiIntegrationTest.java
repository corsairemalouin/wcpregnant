package org.manu.wcpregnant.web;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de la couche agentique (Tool Calling).
 *
 * <p>Contrairement à {@link AgentApiImplTest} (unitaire, dépendances mockées),
 * ce test démarre réellement le contexte Spring : le registre, l'exécuteur,
 * la configuration {@code agent.*} et la chaîne de sécurité sont exercés de
 * bout en bout, ce qui valide le câblage complet de l'API {@code /agent/**}.</p>
 */
@SpringBootTest(properties = {"agent.inference.base-url=http://127.0.0.1:59999/v1"})
@ActiveProfiles("dev") // UserDetailsService en mémoire : manu (USER) / admin (USER, ADMIN)
@DisplayName("Tests d'intégration de l'API agentique")
class AgentApiIntegrationTest {

    private static final String TOOLS_URL = "/agent/tools";
    private static final String CALL_URL = "/agent/tools/call";
    private static final String CHAT_URL = "/agent/chat";

    private final WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    AgentApiIntegrationTest(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    /**
     * Spring Boot 4.x n'expose plus {@code @AutoConfigureMockMvc} :
     * MockMvc est construit manuellement avec la chaîne de sécurité appliquée.
     */
    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ========================================================================
    // Découverte des outils
    // ========================================================================

    @Test
    @DisplayName("GET /agent/tools - anonyme : 401")
    void getAgentTools_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(get(TOOLS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /agent/tools - authentifié : registre OpenAI complet")
    void getAgentTools_shouldReturnOpenAiRegistry_whenAuthenticated() throws Exception {
        mockMvc.perform(get(TOOLS_URL)
                        .with(httpBasic("manu", "manu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("function"))
                .andExpect(jsonPath("$[0].function.name").value(ToiletToolRegistry.SEARCH_NEAR_TOILETS))
                .andExpect(jsonPath("$[0].function.parameters.type").value("object"))
                .andExpect(jsonPath("$[0].function.parameters.required[0]").value("latitude"))
                .andExpect(jsonPath("$[1].function.name").value(ToiletToolRegistry.GET_TOILET_DETAILS))
                .andExpect(jsonPath("$[2].function.name").value(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD));
    }

    // ========================================================================
    // Exécution d'outils
    // ========================================================================

    @Test
    @DisplayName("POST /agent/tools/call - outil inconnu : 400 ApiError")
    void callAgentTool_shouldReturnBadRequest_whenToolIsUnknown() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"invent_un_outil\", \"arguments\": {}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Unknown tool 'invent_un_outil'. Available tools: [search_toilettes_proches, get_details_toilette, gerer_equipement_crud]"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /agent/tools/call - nom d'outil vide : 400 ApiError")
    void callAgentTool_shouldReturnApiError_whenToolNameIsBlank() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"arguments\": {\"id\": 1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("tool name is mandatory"));
    }

    @Test
    @DisplayName("POST /agent/tools/call - arguments non objet : 400 ApiError")
    void callAgentTool_shouldReturnApiError_whenArgumentsAreNotAnObject() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"get_details_toilette\", \"arguments\": \"42\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("arguments must be a JSON object"));
    }

    @Test
    @DisplayName("POST /agent/tools/call - action inconnue : 400 ApiError")
    void callAgentTool_shouldReturnApiError_whenCrudActionIsUnknown() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"gerer_equipement_crud\", \"arguments\": {\"action\": \"TELEPORT\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Unknown action 'TELEPORT'. Allowed values: [CREATE, UPDATE, DELETE]"));
    }

    @Test
    @DisplayName("POST /agent/tools/call - coordonnées invalides : 400")
    void callAgentTool_shouldReturnBadRequest_whenLatitudeIsOutOfRange() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"search_toilettes_proches\", \"arguments\": {\"latitude\": 200, \"longitude\": 2.35}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /agent/tools/call - recherche de proximité : 200 statut OK")
    void callAgentTool_shouldDelegateToProximityService_whenUserIsAuthenticated() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"search_toilettes_proches\", \"arguments\": {\"latitude\": 48.87, \"longitude\": 2.35, \"rayon_metres\": 100}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value(ToiletToolRegistry.SEARCH_NEAR_TOILETS))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    @DisplayName("POST /agent/tools/call - CRUD sans rôle ADMIN : 403")
    void callAgentTool_shouldReturnForbidden_whenCrudIsCalledWithoutAdminRole() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"gerer_equipement_crud\", \"arguments\": {\"action\": \"DELETE\", \"id\": 1}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /agent/tools/call - création par ADMIN : 200 statut CREATED")
    void callAgentTool_shouldCreateToilet_whenAdminRoleIsPresent() throws Exception {
        String payload = """
                {
                  "name": "gerer_equipement_crud",
                  "arguments": {
                    "action": "CREATE",
                    "payload": {
                      "libelle": "WC Agent",
                      "adress": { "ville": "Paris", "codePostal": "75010" },
                      "position": { "latitude": 48.87, "longitude": 2.35 },
                      "grade": { "proprete": 5, "accueil": 6, "espace": 7 }
                    }
                  }
                }
                """;

        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("POST /agent/tools/call - identifiant inexistant : 404")
    void callAgentTool_shouldReturnNotFound_whenToiletIdDoesNotExist() throws Exception {
        mockMvc.perform(post(CALL_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"get_details_toilette\", \"arguments\": {\"id\": 999999}}"))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // Conversation agentique (POST /agent/chat)
    // ========================================================================

    @Test
    @DisplayName("POST /agent/chat - anonyme : 401")
    void chatWithAgent_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"la toilette la plus proche\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /agent/chat - message vide : 400")
    void chatWithAgent_shouldReturnBadRequest_whenMessageIsBlank() throws Exception {
        mockMvc.perform(post(CHAT_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /agent/chat - maxIterations hors contrat : 400")
    void chatWithAgent_shouldReturnBadRequest_whenMaxIterationsExceedsContract() throws Exception {
        mockMvc.perform(post(CHAT_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"liste les toilettes\", \"maxIterations\": 50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /agent/chat - serveur d'inférence injoignable : 503")
    void chatWithAgent_shouldReturnServiceUnavailable_whenLocalInferenceIsDown() throws Exception {
        mockMvc.perform(post(CHAT_URL)
                        .with(httpBasic("manu", "manu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Y a-t-il des toilettes accessibles près de la mairie ?\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
