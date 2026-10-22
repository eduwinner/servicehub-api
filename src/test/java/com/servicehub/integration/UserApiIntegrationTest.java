package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
