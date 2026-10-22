package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("deve retornar 404 ProblemDetail quando o ADMIN busca usuário inexistente")
    void deveRetornar404QuandoBuscarUsuarioInexistente() {
        ResponseEntity<Map> resp = get("/api/v1/users/999", adminToken());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("title", "Recurso não encontrado");
    }

    @Test
    @DisplayName("RN01: deve retornar 409 quando o e-mail já existe, ignorando maiúsculas")
    void deveRetornar409QuandoEmailJaExisteIgnorandoCaixa() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");

        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/register",
                Map.of("fullName", "Maria Duplicada", "email", "Maria@X.com", "password", PASSWORD, "role", "CLIENT"),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("deve aceitar e normalizar e-mail com espaços e maiúsculas no cadastro")
    void deveAceitarEmailComEspacosQuandoCriar() {
        Map<String, Object> body = registrar("Maria Silva", "  Maria@X.com ", "CLIENT");

        assertThat(body).containsEntry("email", "maria@x.com");
    }

    @Test
    @DisplayName("deve atualizar o próprio perfil sem nunca expor a senha")
    void deveAtualizarTelefoneEBioQuandoPut() {
        long id = registrarId("Maria Silva", "maria@x.com", "PROVIDER");

        ResponseEntity<String> resp = call(HttpMethod.PUT, "/api/v1/users/" + id, login("maria@x.com"),
                Map.of("fullName", "  Maria Souza  ", "phone", "62999990000", "bio", "Eletricista"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .contains("\"fullName\":\"Maria Souza\"")
                .contains("\"phone\":\"62999990000\"")
                .contains("\"updatedAt\":\"")
                .doesNotContainIgnoringCase("password");
    }

    @Test
    @DisplayName("deve escapar HTML da bio (sanitização, Aula 8)")
    void deveEscaparHtmlDaBioQuandoAtualizar() {
        long id = registrarId("Maria Silva", "maria@x.com", "CLIENT");

        ResponseEntity<Map> resp = put("/api/v1/users/" + id, login("maria@x.com"),
                Map.of("fullName", "Maria", "bio", "<script>alert(1)</script>"));

        assertThat(resp.getBody()).containsEntry("bio", "&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    @DisplayName("deve listar usuários paginados para o ADMIN")
    void deveListarUsuariosPaginadosQuandoGet() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");
        registrar("João Pereira", "joao@x.com", "PROVIDER");

        ResponseEntity<Map> resp = get("/api/v1/users?size=1", adminToken());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) resp.getBody().get("content")).hasSize(1);
        assertThat(((Map<?, ?>) resp.getBody().get("page")).get("totalElements")).isEqualTo(3); // + admin
    }

    @Test
    @DisplayName("deve recusar ordenação por passwordHash")
    void deveRetornar400QuandoSortPorPasswordHash() {
        ResponseEntity<Map> resp = get("/api/v1/users?sort=passwordHash,desc", adminToken());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
