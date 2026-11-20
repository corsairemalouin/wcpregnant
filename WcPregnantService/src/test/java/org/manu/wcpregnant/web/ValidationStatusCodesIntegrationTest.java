package org.manu.wcpregnant.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration des codes HTTP de validation.
 *
 * <p>Vérifie que les contraintes déclarées dans le contrat OpenAPI
 * ({@code @Min}, {@code @Max}, {@code @DecimalMax} portées par les interfaces
 * générées {@code @Validated}) sont bien traduites en {@code 400 Bad Request}
 * avec un corps {@code ApiError}, et non en {@code 500 Internal Server Error}
 * comme c'était le cas avant l'activation du
 * {@link GlobalExceptionHandler} de production.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Codes HTTP de validation : 400/404 au lieu de 500")
class ValidationStatusCodesIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    /** MockMvc construit manuellement : Spring Boot 4.x n'expose plus @AutoConfigureMockMvc. */
    private MockMvc mockMvc;

    /** Lecture des corps d'erreur JSON des réponses validées. */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOILETS = "/toilets";

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * Lit le corps JSON d'une réponse MockMvc.
     *
     * @param result résultat HTTP obtenu par MockMvc
     * @return le corps JSON parsé
     * @throws Exception si la lecture ou le parsing échoue
     */
    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("GET /toilets/0 (id < 1) : 400 ApiError et non 500")
    void getToiletById_shouldReturn400_whenIdViolatesMinConstraint() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get(TOILETS + "/0").with(httpBasic("manu", "manu")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        JsonNode body = readBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).contains("id: must be greater than or equal to 1");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    @DisplayName("GET /toilets?size=1000 (size > 100) : 400 avec le champ fautif")
    void listToilets_shouldReturn400_whenPageSizeExceedsMaximum() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get(TOILETS).with(httpBasic("manu", "manu"))
                        .param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        JsonNode body = readBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).contains("size: must be less than or equal to 100");
    }

    @Test
    @DisplayName("GET /toilets?latitude=200 : 400 pour la contrainte de latitude")
    void listToilets_shouldReturn400_whenLatitudeIsOutOfRange() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get(TOILETS).with(httpBasic("manu", "manu"))
                        .param("latitude", "200"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        JsonNode body = readBody(result);
        assertThat(body.get("message").asText()).contains("latitude: must be less than or equal to 90");
    }

    @Test
    @DisplayName("GET /toilets/999999 : 404 conservé (comportement du contrôleur, pas 500)")
    void getToiletById_shouldReturn404_whenResourceIsMissing() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get(TOILETS + "/999999").with(httpBasic("manu", "manu")))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert : le contrôleur construit lui-même le 404, le corps reste vide
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("PUT /toilets/999999/grade : 404 ApiError porté par le handler global")
    void gradeToilet_shouldReturn404ApiError_whenResourceIsMissing() throws Exception {
        // Arrange
        String payload = """
            {
                "proprete": 8,
                "accueil": 7,
                "espace": 6
            }
            """;

        // Act
        MvcResult result = mockMvc.perform(put(TOILETS + "/999999/grade")
                        .with(httpBasic("manu", "manu"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        JsonNode body = readBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("message").asText()).contains("Toilette not found with id: 999999");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    @DisplayName("POST /toilets avec un corps invalide : 400 et non 500")
    void createToilet_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        // Arrange
        String invalidPayload = """
            {
                "libelle": "WC invalide",
                "adress": {
                    "ville": "Paris",
                    "codePostal": "75011"
                },
                "position": {
                    "latitude": 200,
                    "longitude": 2.35
                }
            }
            """;

        // Act
        MvcResult result = mockMvc.perform(post(TOILETS)
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        JsonNode body = readBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).contains("position.latitude: must be less than or equal to 90");
    }
}
