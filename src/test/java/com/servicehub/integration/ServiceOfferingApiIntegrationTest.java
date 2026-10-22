package com.servicehub.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceOfferingApiIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PageMeta(int size, int number, long totalElements, int totalPages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ServicePage(List<Map<String, Object>> content, PageMeta page) {
    }

    @BeforeEach
    void semear() {
        Number providerId = (Number) rest.postForEntity("/api/v1/users",
                Map.of("fullName", "João Prestador", "email", "joao@x.com", "password", "Senha123"), Map.class)
                .getBody().get("id");
        criarServico("Limpeza residencial", "100.00", "LIMPEZA", providerId);
        criarServico("Limpeza pós-obra", "250.00", "LIMPEZA", providerId);
        criarServico("Instalação de tomadas", "150.00", "ELETRICA", providerId);
    }

    private void criarServico(String title, String price, String category, Number providerId) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/services",
                Map.of("title", title, "price", price, "category", category, "providerId", providerId), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ServicePage listar(String query) {
        ResponseEntity<ServicePage> resp = rest.getForEntity("/api/v1/services" + query, ServicePage.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    @DisplayName("deve filtrar por categoria quando listar")
    void deveFiltrarPorCategoriaQuandoListar() {
        assertThat(listar("?category=LIMPEZA").page().totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve filtrar por faixa de preço quando listar")
    void deveFiltrarPorFaixaDePrecoQuandoListar() {
        ServicePage page = listar("?minPrice=120&maxPrice=200");

        assertThat(page.content()).extracting(s -> s.get("title")).containsExactly("Instalação de tomadas");
    }

    @Test
    @DisplayName("deve buscar no título sem diferenciar maiúsculas quando listar")
    void deveBuscarPorTituloIgnorandoCaixaQuandoListar() {
        assertThat(listar("?q=LIMPEZA").page().totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve limitar a página a 100 itens quando size excede o máximo")
    void deveLimitarTamanhoDaPaginaQuandoSizeExcede() {
        assertThat(listar("?size=500").page().size()).isEqualTo(100);
    }

    @Test
    @DisplayName("deve paginar quando size é 1")
    void devePaginarQuandoSizeUm() {
        ServicePage page = listar("?size=1&page=1");

        assertThat(page.content()).hasSize(1);
        assertThat(page.page().totalPages()).isEqualTo(3);
    }
}
