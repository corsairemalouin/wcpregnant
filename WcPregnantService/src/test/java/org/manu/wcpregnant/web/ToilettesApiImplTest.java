package org.manu.wcpregnant.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class ToilettesApiImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Helper pour créer un objet Toilet valide avec toutes les contraintes SQL (Grade, etc.)
     */
    private Toilet getMockToilet() {
        return Toilet.builder()
                .libelle("WC Public")
                .grade(Graduation.builder().proprete(1).accueil(2).espace(3).build())
                .adress(Adress.builder().ville("Paris").codePostal("75001").build())
                .position(GeoPosition.builder().latitude(48.8).longitude(2.3).build())
                .build();
    }

    /**
     * Helper pour créer un toilette en base et récupérer son ID généré
     */
    private Long createAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/toilets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(getMockToilet())))
                .andExpect(status().isCreated())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(content);
        return node.get("id").asLong();
    }

    @Nested
    @DisplayName("Tests de Création (POST)")
    class CreateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN peut créer un toilette")
        void create_Admin_Success() throws Exception {
            mockMvc.perform(post("/toilets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(getMockToilet())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER ne peut PAS créer un toilette (403)")
        void create_User_Forbidden() throws Exception {
            mockMvc.perform(post("/toilets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(getMockToilet())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests de Mise à jour (PUT)")
    class UpdateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN peut modifier un toilette existant")
        void update_Admin_Success() throws Exception {
            // Extraction de l'ID réel généré par la BDD
            Long id = createAndGetId();

            mockMvc.perform(put("/toilets/" + id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(getMockToilet())))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER ne peut PAS modifier un toilette (403)")
        void update_User_Forbidden() throws Exception {
            mockMvc.perform(put("/toilets/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(getMockToilet())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests de Suppression (DELETE)")
    class DeleteTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN peut supprimer un toilette existant")
        void delete_Admin_Success() throws Exception {
            // Extraction de l'ID réel généré par la BDD
            Long id = createAndGetId();

            mockMvc.perform(delete("/toilets/" + id)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER ne peut PAS supprimer un toilette (403)")
        void delete_User_Forbidden() throws Exception {
            mockMvc.perform(delete("/toilets/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests de Lecture (GET)")
    class GetTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Tout utilisateur authentifié peut lister les toilettes")
        void getAll_Authenticated_Success() throws Exception {
            mockMvc.perform(get("/toilets"))
                    .andExpect(status().isOk());
        }
    }
}