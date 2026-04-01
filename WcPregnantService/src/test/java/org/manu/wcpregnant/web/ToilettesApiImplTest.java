package org.manu.wcpregnant.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    @Test
    void lireToilette() throws Exception {
        when(service.lireToilet(1L)).thenReturn(Optional.ofNullable(TestsUtils.buildToilet()));
        when(service.lireToilet(2L)).thenReturn(TestsUtils.buildEmptyToilette());
        this.mockMvc.perform(get("/wcpregnant/toilets/toilet/1")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("default test value")));
        this.mockMvc.perform(get("/wcpregnant/toilets/toilet/2")).andDo(print()).andExpect(status().isNoContent());
    }

    @Test
    void addToilette() throws Exception {
        String data = TestsUtils.buildToiletteJson();
        this.mockMvc
                .perform(post("/wcpregnant/toilets/toilet").contentType(MediaType.APPLICATION_JSON)
                        .content(TestsUtils.buildToiletteJson().getBytes()))
                .andDo(print()).andExpect(status().is2xxSuccessful());
    }

    @Test
    void getNearsetToilettes() throws Exception {
        List<Toilet> list = new ArrayList<>(1);
        list.add(TestsUtils.buildToilet());
        when(service.getNearsetToilettes(any(GeoPosition.class))).thenReturn(list);
        this.mockMvc.perform(get("/wcpregnant/toilets/toilets?longitude=-2.2&latitude=48.1")).andDo(print())
                .andExpect(status().isOk()).andExpect(content().string(containsString("default test value")));
    }

}