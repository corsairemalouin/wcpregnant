package org.manu.wcpregnant.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev") // Utilise le UserDetailsService en mémoire (manu/admin)
@DisplayName("Tests d'Intégration Sécurité")
class SecurityIntegrationTest {

    private final WebApplicationContext webApplicationContext;

    /**
     * Spring Boot 4.x n'expose plus @AutoConfigureMockMvc dans spring-boot-test-autoconfigure :
     * MockMvc est construit manuellement à partir du WebApplicationContext de l'application.
     */
    private MockMvc mockMvc;

    SecurityIntegrationTest(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
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
}