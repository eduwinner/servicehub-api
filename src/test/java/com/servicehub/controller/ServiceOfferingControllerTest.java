package com.servicehub.controller;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.dto.ServiceFilter;
import com.servicehub.model.dto.ServiceOfferingResponseDTO;
import com.servicehub.service.ServiceOfferingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceOfferingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceOfferingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ServiceOfferingService service;

    private static String servico(String price, String category) {
        return "{\"title\":\"Instalação de tomadas\",\"description\":\"Troca e instalação\","
                + "\"price\":" + price + ",\"category\":\"" + category + "\"}";
    }

    @Test
    @DisplayName("deve retornar 400 listando as categorias válidas quando a categoria não existe")
    void deveRetornar400ListandoCategoriasQuandoCategoriaInvalida() throws Exception {
        mockMvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                        .content(servico("150.00", "INVALIDA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("LIMPEZA")));
    }

    @Test
    @DisplayName("deve retornar 400 'Dados inválidos' quando o preço não é numérico")
    void deveRetornar400QuandoPrecoNaoNumerico() throws Exception {
        mockMvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                        .content(servico("\"abc\"", "ELETRICA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    @DisplayName("deve retornar 400 apontando o campo price quando o preço é zero")
    void deveRetornar400QuandoPrecoZero() throws Exception {
        mockMvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                        .content(servico("0", "ELETRICA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));
    }

    @Test
    @DisplayName("deve retornar 201 com header Location quando criar serviço")
    void deveRetornar201ComLocationQuandoCriarServico() throws Exception {
        when(service.createService(any())).thenReturn(new ServiceOfferingResponseDTO(
                12L, "Instalação de tomadas", "Troca e instalação", new BigDecimal("150.00"),
                ServiceCategory.ELETRICA, true, null, null, 1L, "João Silva"));

        mockMvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                        .content(servico("150.00", "ELETRICA")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/services/12"));
    }

    @Test
    @DisplayName("deve repassar os filtros ao service quando listar")
    void deveRepassarFiltrosAoServiceQuandoListar() throws Exception {
        when(service.search(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/services?category=ELETRICA&minPrice=10&q=tom"))
                .andExpect(status().isOk());

        verify(service).search(eq(new ServiceFilter(ServiceCategory.ELETRICA, null, new BigDecimal("10"), null, "tom", false)),
                any(Pageable.class));
    }

    @Test
    @DisplayName("deve retornar 400 em ProblemDetail listando categorias quando filtro de categoria é inválido")
    void deveRetornar400QuandoFiltroCategoriaInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/services?category=FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value(containsString("LIMPEZA")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("deve retornar 400 em ProblemDetail quando minPrice não é número")
    void deveRetornar400QuandoMinPriceNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/v1/services?minPrice=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"))
                .andExpect(jsonPath("$.detail").value(containsString("minPrice")));
    }

    @Test
    @DisplayName("deve retornar 405 em ProblemDetail em português quando o método não é suportado")
    void deveRetornar405QuandoMetodoNaoSuportado() throws Exception {
        mockMvc.perform(patch("/api/v1/services"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:method-not-allowed"))
                .andExpect(jsonPath("$.title").value("Método não permitido"));
    }

    @Test
    @DisplayName("deve retornar 415 em ProblemDetail quando o corpo não é JSON")
    void deveRetornar415QuandoConteudoNaoJson() throws Exception {
        mockMvc.perform(post("/api/v1/services").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:unsupported-media-type"));
    }

    @Test
    @DisplayName("deve retornar 404 em ProblemDetail quando a rota não existe")
    void deveRetornar404QuandoRotaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("deve retornar 400 quando o preço tem mais de duas casas decimais")
    void deveRetornar400QuandoPrecoComTresCasas() throws Exception {
        mockMvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                        .content(servico("150.005", "ELETRICA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));
    }
}
