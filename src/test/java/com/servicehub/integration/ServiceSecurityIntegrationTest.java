package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceSecurityIntegrationTest extends AbstractIntegrationTest {

    long joaoId;
    String joao;
    String ana;
    String maria;

    @BeforeEach
    void usuarios() {
        joaoId = registrarId("João Prestador", "joao@x.com", "PROVIDER");
        registrar("Ana Prestadora", "ana@x.com", "PROVIDER");
        registrar("Maria Cliente", "maria@x.com", "CLIENT");
        joao = login("joao@x.com");
        ana = login("ana@x.com");
        maria = login("maria@x.com");
    }

    @Test
    @DisplayName("deve exigir token para criar serviço")
    void deveRetornar401QuandoCriarServicoSemToken() {
        ResponseEntity<Map> resp = post("/api/v1/services", null,
                Map.of("title", "Faxina", "price", "80.00", "category", "LIMPEZA"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("deve impedir que um CLIENT publique serviço")
    void deveRetornar403QuandoClienteCriaServico() {
        ResponseEntity<Map> resp = post("/api/v1/services", maria,
                Map.of("title", "Faxina", "price", "80.00", "category", "LIMPEZA"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("RN04: o prestador do serviço é sempre quem está autenticado, nunca um id do corpo")
    void deveUsarUsuarioDoTokenComoPrestador() {
        ResponseEntity<Map> resp = post("/api/v1/services", joao,
                Map.of("title", "Faxina", "price", "80.00", "category", "LIMPEZA", "providerId", 999));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) resp.getBody().get("providerId")).longValue()).isEqualTo(joaoId);
    }

    @Test
    @DisplayName("RN05: só o dono ou o ADMIN altera um serviço")
    void deveRetornar403QuandoOutroPrestadorAltera() {
        long id = criarServico(joao, "Faxina", "80.00", "LIMPEZA");
        Map<String, Object> novo = Map.of("title", "Faxina completa", "price", "120.00", "category", "LIMPEZA");

        assertThat(put("/api/v1/services/" + id, ana, novo).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(put("/api/v1/services/" + id, joao, novo).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(put("/api/v1/services/" + id, adminToken(), novo).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("RN12: exclusão do serviço é lógica; some do público, continua visível ao dono")
    void deveOcultarServicoQuandoDonoExclui() {
        long id = criarServico(joao, "Faxina", "80.00", "LIMPEZA");

        assertThat(delete("/api/v1/services/" + id, ana).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/api/v1/services/" + id, joao).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(get("/api/v1/services/" + id, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/v1/services/" + id, ana).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<Map> dono = get("/api/v1/services/" + id, joao);
        assertThat(dono.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dono.getBody()).containsEntry("active", false);
        assertThat((List<?>) get("/api/v1/services", null).getBody().get("content")).isEmpty();
    }

    @Test
    @DisplayName("deve reativar o serviço pelo PUT com active=true")
    void deveReativarServicoQuandoPutActiveTrue() {
        long id = criarServico(joao, "Faxina", "80.00", "LIMPEZA");
        delete("/api/v1/services/" + id, joao);

        ResponseEntity<Map> resp = put("/api/v1/services/" + id, joao,
                Map.of("title", "Faxina", "price", "80.00", "category", "LIMPEZA", "active", true));

        assertThat(resp.getBody()).containsEntry("active", true);
        assertThat(get("/api/v1/services/" + id, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("deve listar inativos só para o ADMIN com includeInactive=true")
    void deveIncluirInativosQuandoAdminPede() {
        long id = criarServico(joao, "Faxina", "80.00", "LIMPEZA");
        delete("/api/v1/services/" + id, joao);

        Map<?, ?> publico = get("/api/v1/services?includeInactive=true", null).getBody();
        Map<?, ?> admin = get("/api/v1/services?includeInactive=true", adminToken()).getBody();

        assertThat((List<?>) publico.get("content")).isEmpty();
        assertThat((List<?>) admin.get("content")).hasSize(1);
    }

    @Test
    @DisplayName("deve escapar HTML da descrição (sanitização, Aula 8)")
    void deveEscaparHtmlDaDescricao() {
        ResponseEntity<Map> resp = post("/api/v1/services", joao, Map.of("title", "Faxina", "price", "80.00",
                "category", "LIMPEZA", "description", "<b>barato</b>"));

        assertThat(resp.getBody()).containsEntry("description", "&lt;b&gt;barato&lt;/b&gt;");
    }

    @Test
    @DisplayName("deve permitir CORS para a bancada de testes em localhost:5173")
    void devePermitirCorsDaBancada() {
        org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders();
        h.setOrigin("http://localhost:5173");
        h.setAccessControlRequestMethod(org.springframework.http.HttpMethod.POST);
        h.setAccessControlRequestHeaders(List.of("authorization", "content-type"));

        ResponseEntity<String> resp = rest.exchange("/api/v1/services", org.springframework.http.HttpMethod.OPTIONS,
                new org.springframework.http.HttpEntity<>(h), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }
}
