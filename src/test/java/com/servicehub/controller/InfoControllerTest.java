package com.servicehub.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InfoController.class)
@AutoConfigureMockMvc(addFilters = false)
class InfoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/info deve retornar 200 com status OK")
    void deveRetornarInfoQuandoChamado() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.message").value("ServiceHub API operacional"));
    }
}
