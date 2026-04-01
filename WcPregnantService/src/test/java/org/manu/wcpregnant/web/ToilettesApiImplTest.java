package org.manu.wcpregnant.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ToilettesApiImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToiletteService service;

    /**
     * Test de lecture d'une toilette par ID.
     * Note: Vérifiez que le chemin /toilet/{id} correspond bien à votre interface générée.
     */
    @Test
    void lireToilette() throws Exception {
        // Mocking des comportements du service
        when(service.lireToilet(1L)).thenReturn(Optional.ofNullable(TestsUtils.buildToilet()));
        when(service.lireToilet(2L)).thenReturn(Optional.empty());

        // Cas 200 OK
        this.mockMvc.perform(get("/toilet/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("default test value")));

        // Cas 404 Not Found (ou 204 selon votre implémentation Controller)
        this.mockMvc.perform(get("/toilet/2"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * Test de l'ajout d'une toilette.
     * L'utilisation de .with(csrf()) règle l'erreur 403 Forbidden.
     */
    @Test
    void addToilette() throws Exception {
        String jsonPayload = TestsUtils.buildToiletteJson();

        this.mockMvc.perform(post("/toilet")
                        .with(csrf()) // Indispensable pour éviter le 403 avec Spring Security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isCreated()); // 201 Created attendu pour un POST réussi
    }

    /**
     * Test de la recherche à proximité.
     */
    @Test
    void getNearsetToilettes() throws Exception {
        List<Toilet> list = new ArrayList<>();
        list.add(TestsUtils.buildToilet());

        // Correction du mock pour matcher n'importe quelle position
        when(service.getNearsetToilettes(any(GeoPosition.class))).thenReturn(list);

        this.mockMvc.perform(get("/toilet")
                        .param("latitude", "48.1")
                        .param("longitude", "-2.2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("default test value")));
    }
}