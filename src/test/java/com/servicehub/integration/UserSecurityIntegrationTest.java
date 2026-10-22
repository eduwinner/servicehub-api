package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserSecurityIntegrationTest extends AbstractIntegrationTest {

    long mariaId;
    long joaoId;
    String maria;
    String joao;

    @BeforeEach
    void usuarios() {
        mariaId = registrarId("Maria Silva", "maria@x.com", "CLIENT");
        joaoId = registrarId("João Prestador", "joao@x.com", "PROVIDER");
        maria = login("maria@x.com");
        joao = login("joao@x.com");
    }

    @Test
    @DisplayName("deve recusar a listagem de usuários para quem não é ADMIN")
    void deveRetornar403QuandoClienteListaUsuarios() {
        ResponseEntity<Map> resp = get("/api/v1/users", maria);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:forbidden");
    }

    @Test
    @DisplayName("RN13: deve impedir que um usuário veja o perfil de outro")
    void deveRetornar403QuandoVerPerfilDeOutro() {
        assertThat(get("/api/v1/users/" + joaoId, maria).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/v1/users/" + mariaId, maria).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/users/" + mariaId, adminToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("RN13: deve impedir que um usuário edite o perfil de outro")
    void deveRetornar403QuandoEditarPerfilDeOutro() {
        ResponseEntity<Map> resp = put("/api/v1/users/" + joaoId, maria, Map.of("fullName", "Invasora"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("RN03: só o ADMIN altera papel, e o novo papel vale na hora para o token existente")
    void devePromoverClienteAPrestadorQuandoAdmin() {
        assertThat(patch("/api/v1/users/" + mariaId + "/role", maria, Map.of("role", "ADMIN")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> resp = patch("/api/v1/users/" + mariaId + "/role", adminToken(), Map.of("role", "PROVIDER"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("role", "PROVIDER");
        criarServico(maria, "Faxina", "80.00", "LIMPEZA");
    }

    @Test
    @DisplayName("RN11: exclusão pelo ADMIN é lógica, bloqueia login e desativa os serviços do usuário")
    void deveDesativarUsuarioEServicosQuandoAdminExclui() {
        long servicoId = criarServico(joao, "Instalação de tomadas", "150.00", "ELETRICA");

        assertThat(delete("/api/v1/users/" + joaoId, maria).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/api/v1/users/" + joaoId, adminToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Long.class, joaoId)).isEqualTo(1);
        assertThat(get("/api/v1/auth/me", joao).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/v1/services/" + servicoId, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("deve reativar um usuário pelo ADMIN sem reativar os serviços dele")
    void deveReativarUsuarioQuandoAdmin() {
        long servicoId = criarServico(joao, "Instalação de tomadas", "150.00", "ELETRICA");
        String admin = adminToken();
        delete("/api/v1/users/" + joaoId, admin);

        ResponseEntity<Map> resp = patch("/api/v1/users/" + joaoId + "/active", admin, Map.of("active", true));

        assertThat(resp.getBody()).containsEntry("active", true);
        login("joao@x.com");
        assertThat(get("/api/v1/services/" + servicoId, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
