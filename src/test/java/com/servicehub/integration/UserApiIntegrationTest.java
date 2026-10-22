package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    private ResponseEntity<Map> criarUsuario(String nome, String email) {
        return rest.postForEntity("/api/v1/users",
                Map.of("fullName", nome, "email", email, "password", "Senha123"), Map.class);
    }

    @Test
    @DisplayName("deve retornar 404 ProblemDetail quando o usuário não existe")
    void deveRetornar404QuandoBuscarUsuarioInexistente() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/v1/users/999", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("title", "Recurso não encontrado");
    }

    @Test
    @DisplayName("deve retornar 409 quando o e-mail já existe, ignorando maiúsculas")
    void deveRetornar409QuandoEmailJaExisteIgnorandoCaixa() {
        assertThat(criarUsuario("Maria Silva", "maria@x.com").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> resp = criarUsuario("Maria Duplicada", "Maria@X.com");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("deve atualizar telefone e bio sem nunca expor a senha")
    void deveAtualizarTelefoneEBioQuandoPut() {
        Number id = (Number) criarUsuario("Maria Silva", "maria@x.com").getBody().get("id");

        ResponseEntity<String> resp = rest.exchange("/api/v1/users/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("fullName", "Maria Souza", "phone", "62999990000", "bio", "Eletricista")),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .contains("\"phone\":\"62999990000\"")
                .contains("\"bio\":\"Eletricista\"")
                .contains("\"updatedAt\":\"")
                .doesNotContainIgnoringCase("password");
    }
}
