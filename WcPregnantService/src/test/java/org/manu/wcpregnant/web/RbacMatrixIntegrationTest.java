package org.manu.wcpregnant.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.seed.TestUserProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

/**
 * Tests d'intégration de la matrice RBAC : chaque profil de compte déclaré dans
 * {@link TestUserProfiles} est confronté aux règles {@code @PreAuthorize} réelles des
 * endpoints (lecture {@code USER}, écriture {@code ADMIN}), y compris les cas non
 * hiérarchiques (ADMIN seul, aucun rôle, compte désactivé).
 */
@SpringBootTest
@ActiveProfiles("dev") // UserDetailsService en mémoire alimenté par TestUserProfiles
@DisplayName("Matrice RBAC : chaque profil face aux règles @PreAuthorize")
class RbacMatrixIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    /** Spring Boot 4.x : MockMvc est construit manuellement depuis le WebApplicationContext. */
    private MockMvc mockMvc;

    private static final String TOILETS = "/toilets";

    private static final String VALID_TOILET_JSON = """
        {
            "libelle": "WC RBAC",
            "adress": {
                "ville": "Paris",
                "codePostal": "75011"
            },
            "position": {
                "latitude": 48.86,
                "longitude": 2.35
            },
            "grade": {
                "proprete": 5,
                "accueil": 5,
                "espace": 5
            }
        }
        """;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("VISITOR (manu, USER) : lit la liste (200), ne crée pas (403)")
    void visitorProfile_canReadButNotCreate() throws Exception {
        // Arrange & Act
        mockMvc.perform(get(TOILETS).with(httpBasic("manu", "manu")))
                .andExpect(status().isOk());

        // Assert
        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("manu", "manu"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (admin, USER + ADMIN) : lit (200) et crée (201)")
    void adminProfile_canReadAndCreate() throws Exception {
        mockMvc.perform(get(TOILETS).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ADMIN_ONLY (adminonly) : crée (201) mais ne lit pas (403), rôles non hiérarchiques")
    void adminOnlyProfile_canCreateButNotRead() throws Exception {
        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("adminonly", "adminonly"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get(TOILETS).with(httpBasic("adminonly", "adminonly")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NO_ROLE (norole) : authentifié mais aucune autorité, donc 403")
    void noRoleProfile_isAuthenticatedButForbidden() throws Exception {
        mockMvc.perform(get(TOILETS).with(httpBasic("norole", "norole")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("norole", "norole"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DISABLED (disabled) : bon mot de passe mais compte désactivé, donc 401")
    void disabledProfile_cannotAuthenticate() throws Exception {
        mockMvc.perform(get(TOILETS).with(httpBasic("disabled", "disabled")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ANONYME (aucune credential) : accès refusé (401)")
    void anonymousRequest_isUnauthorized() throws Exception {
        mockMvc.perform(get(TOILETS))
                .andExpect(status().isUnauthorized());
    }
}
