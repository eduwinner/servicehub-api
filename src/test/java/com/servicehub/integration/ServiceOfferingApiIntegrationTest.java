package com.servicehub.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManagerFactory;
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
        registrar("João Prestador", "joao@x.com", "PROVIDER");
        String joao = login("joao@x.com");
        criarServico(joao, "Limpeza residencial", "100.00", "LIMPEZA");
        criarServico(joao, "Limpeza pós-obra", "250.00", "LIMPEZA");
        criarServico(joao, "Instalação de tomadas", "150.00", "ELETRICA");
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

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("deve retornar 400 quando sort usa propriedade inexistente")
    void deveRetornar400QuandoSortInexistente() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/v1/services?sort=foo", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:validation-error");
    }

    @Test
    @DisplayName("deve recusar ordenação por campo interno do prestador")
    void deveRetornar400QuandoSortPorCampoDoPrestador() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/v1/services?sort=provider.passwordHash", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("deve listar serviços de vários prestadores sem N+1 consultas")
    void deveListarSemNMaisUmQuandoVariosPrestadores() {
        for (int i = 2; i <= 4; i++) {
            registrar("Prestador " + i, "p" + i + "@x.com", "PROVIDER");
            criarServico(login("p" + i + "@x.com"), "Serviço " + i, "90.00", "PINTURA");
        }
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        listar("?size=50");

        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("deve tratar % e _ da busca como texto literal, não como curinga")
    void deveTratarCuringasComoTextoQuandoBuscar() {
        assertThat(listar("?q=%25").page().totalElements()).isZero();
        assertThat(listar("?q=Limpeza_residencial").page().totalElements()).isZero();
        assertThat(listar("?q=Limpeza res").page().totalElements()).isEqualTo(1);
    }
}
