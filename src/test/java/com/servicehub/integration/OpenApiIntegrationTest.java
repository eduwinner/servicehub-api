package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("deve expor a especificação OpenAPI do grupo v1")
    void deveExporEspecificacaoDoGrupoV1() {
        ResponseEntity<String> resp = rest.getForEntity("/api-docs/v1", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .contains("\"/api/v1/users\"")
                .contains("\"/api/v1/services\"")
                .contains("\"/api/v1/info\"")
                .contains("ProblemDetail");
    }

    @Test
    @DisplayName("não deve documentar erros como '500 sem handler'")
    void naoDeveDocumentarRespostas500NosEndpointsDeCrud() {
        ResponseEntity<String> resp = rest.getForEntity("/api-docs/v1", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).doesNotContain("sem handler");
    }

    @Test
    @DisplayName("deve servir o Swagger UI em /docs")
    void deveServirSwaggerUiEmDocs() {
        ResponseEntity<String> docs = rest.getForEntity("/docs", String.class);
        ResponseEntity<String> ui = rest.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(docs.getStatusCode().is2xxSuccessful() || docs.getStatusCode().is3xxRedirection()).isTrue();
        assertThat(ui.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("deve documentar o esquema bearerAuth (botão Authorize no Swagger)")
    void deveDocumentarEsquemaBearer() {
        String body = rest.getForEntity("/api-docs/v1", String.class).getBody();

        assertThat(body).contains("\"bearerAuth\"").contains("\"scheme\":\"bearer\"").contains("\"bearerFormat\":\"JWT\"");
    }

    @Test
    @DisplayName("deve expor o grupo público só com autenticação e leitura do catálogo")
    void deveExporGrupoPublico() {
        ResponseEntity<String> resp = rest.getForEntity("/api-docs/public", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("/api/v1/auth/login").contains("/api/v1/services")
                .doesNotContain("/api/v1/users");
    }
}
