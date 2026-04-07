package org.manu.wcpregnant.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ToilettesApiImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToiletteService service;

    // Correspond au @RequestMapping du contrôleur
    private final String BASE_URL = "/api/wcpregnant/toilets";

    /**
     * Test de lecture d'une toilette par ID.
     * URL attendue : /api/wcpregnant/toilets/toilet/{id}
     */
    @Test
    @WithMockUser(roles = "USER")
    void lireToilette() throws Exception {
        // Mocking des comportements du service
        when(service.lireToilet(1L)).thenReturn(Optional.of(TestsUtils.buildToilet()));
        when(service.lireToilet(2L)).thenReturn(Optional.empty());

        // Cas 200 OK : L'ID est passé dans le chemin (Path Variable)
        this.mockMvc.perform(get(BASE_URL + "/toilet/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value(containsString("default test value")));

        // Cas 204 No Content : L'ID n'existe pas
        this.mockMvc.perform(get(BASE_URL + "/toilet/2")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    /**
     * Test de l'ajout d'une toilette (Admin uniquement).
     * URL attendue : /api/wcpregnant/toilets/toilet
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void addToilette() throws Exception {
        String jsonPayload = TestsUtils.buildToiletteJson();

        this.mockMvc.perform(post(BASE_URL + "/toilet")
                        .with(csrf()) // Indispensable pour les requêtes POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    /**
     * Test de la recherche à proximité.
     * URL attendue : /api/wcpregnant/toilets/toilet?latitude=...&longitude=...
     * Note : L'interface OpenAPI n'utilise pas "/nearby" mais "/toilet" avec params.
     */
    @Test
    @WithMockUser(roles = "USER")
    void getNearsetToilettes() throws Exception {
        List<Toilet> list = new ArrayList<>();
        list.add(TestsUtils.buildToilet());

        when(service.getNearsetToilettes(any(GeoPosition.class))).thenReturn(list);

        this.mockMvc.perform(get(BASE_URL + "/toilet")
                        .param("latitude", "48.1")
                        .param("longitude", "-2.2")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("default test value")));
    }
}