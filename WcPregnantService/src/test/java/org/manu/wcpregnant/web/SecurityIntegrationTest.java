package org.manu.wcpregnant.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev") // Utilise le UserDetailsService en mémoire (manu/admin)
@DisplayName("Tests d'Intégration Sécurité")
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Spring Boot 4.x n'expose plus @AutoConfigureMockMvc dans spring-boot-test-autoconfigure :
     * MockMvc est construit manuellement à partir du WebApplicationContext de l'application.
     */
    private MockMvc mockMvc;

    /** Lecture des réponses JSON des endpoints de réinitialisation. */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Compte de démonstration garanti présent pour les tests de réinitialisation.
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase("manu")
                .orElse(new UserAccount("manu", passwordEncoder.encode("manu"), "manu@example.com", true));
        account.clearResetCode();
        userAccountRepository.save(account);
    }

    // L'URL doit correspondre à ce qui est généré par OpenAPI (vu dans tes logs)
    private final String BASE_URL = "/toilets";

    // JSON complet pour satisfaire la validation interne (Address mandatory)
    private final String VALID_TOILET_JSON = """
        {
            "libelle": "WC Test",
            "adress": {
                "ville": "Paris",
                "codePostal": "75010"
            },
            "position": {
                "latitude": 48.87,
                "longitude": 2.35
            }
        }
        """;

    @Test
    @DisplayName("GET /toilets/1 - Anonyme : doit être 401 (Unauthorized)")
    void accesAnonyme_Refuse() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /toilets - Auth Basic manu : Succès technique (200)")
    void authBasic_Succes() throws Exception {
        // On appelle la liste complète plutôt qu'un ID précis pour éviter le 404
        mockMvc.perform(get(BASE_URL)
                        .with(httpBasic("manu", "manu")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Rôle ADMIN : Succès (201) avec données complètes")
    void roleAdmin_CanPost() throws Exception {
        // Données complètes pour satisfaire les contraintes du modèle (grade obligatoire)
        String jsonWithGrade = """
        {
            "libelle": "WC Test",
            "adress": { "ville": "Paris", "codePostal": "75010" },
            "position": { "latitude": 48.87, "longitude": 2.35 },
            "grade": { "proprete": 1, "accueil": 2, "espace": 3 }
        }
        """;

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithGrade))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /actuator/health - Anonyme : 200 (sonde de santé des conteneurs)")
    void healthAnonyme_Autorise() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/health/readiness - Anonyme : 200 (probe Kubernetes)")
    void healthReadinessAnonyme_Autorise() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La chaîne de santé n'ouvre pas /agent/tools : anonyme = 401")
    void healthChainDoesNotOpenAgentTools() throws Exception {
        mockMvc.perform(get("/agent/tools"))
                .andExpect(status().isUnauthorized());
    }

    // --- RÉINITIALISATION DU MOT DE PASSE : /auth/** doit rester public ---

    @Test
    @DisplayName("POST /auth/password-reset/request - Anonyme : 200 (la demande est publique)")
    void passwordResetRequestAnonyme_Autorise() throws Exception {
        String body = mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"manu\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        // En mode démonstration (SMTP désactivé) le code est renvoyé pour permettre le test.
        assertThat(json.has("demoCode")).isTrue();
        assertThat(json.get("demoCode").asText()).matches("\\d{6}");
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Compte inconnu : 404 et non 401")
    void passwordResetRequestCompteInconnu_Renvoie404() throws Exception {
        mockMvc.perform(post("/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"inconnu\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Login vide : 400")
    void passwordResetRequestLoginVide_Renvoie400() throws Exception {
        mockMvc.perform(post("/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cycle complet demande -> confirmation : le mot de passe est réellement remplacé")
    void passwordResetCycleComplet_RemplaceLeMotDePasse() throws Exception {
        String requestBody = mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"MANU\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String demoCode = objectMapper.readTree(requestBody).get("demoCode").asText();

        mockMvc.perform(post("/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"manu\", \"code\": \"" + demoCode + "\", \"newPassword\": \"MotDePasse123\"}"))
                .andExpect(status().isOk());

        UserAccount updated = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        assertThat(updated.getResetCode()).isNull();
        assertThat(passwordEncoder.matches("MotDePasse123", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /auth/password-reset/confirm - Code erroné : 404, mot de passe inchangé")
    void passwordResetConfirmCodeErrone_Renvoie404() throws Exception {
        UserAccount before = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        String passwordBefore = before.getPassword();

        mockMvc.perform(post("/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"manu\", \"code\": \"000000\", \"newPassword\": \"MotDePasse123\"}"))
                .andExpect(status().isNotFound());

        UserAccount after = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        assertThat(after.getPassword()).isEqualTo(passwordBefore);
    }
}
