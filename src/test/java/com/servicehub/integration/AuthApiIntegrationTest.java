package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("deve cadastrar prestador e retornar 201 com Location, papel e sem senha")
    void deveRegistrarPrestadorQuandoDadosValidos() {
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/register",
                Map.of("fullName", "Maria Silva", "email", "maria@x.com", "password", PASSWORD, "role", "PROVIDER"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).hasPath("/api/v1/users/2");
        assertThat(resp.getBody()).contains("\"role\":\"PROVIDER\"").doesNotContainIgnoringCase("password");
    }

    @Test
    @DisplayName("RN03: deve recusar cadastro com papel ADMIN")
    void deveRetornar400QuandoRegistrarComoAdmin() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/register",
                Map.of("fullName", "Esperto", "email", "e@x.com", "password", PASSWORD, "role", "ADMIN"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((List<Map<String, String>>) resp.getBody().get("errors"))
                .anySatisfy(e -> assertThat(e).containsEntry("field", "role"));
    }

    @Test
    @DisplayName("RN02: deve gravar a senha como hash BCrypt")
    void deveGravarSenhaComoHashBcrypt() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = 'maria@x.com'", String.class);

        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    @DisplayName("deve emitir token Bearer de 24h quando o login é válido")
    void deveRetornarTokenQuandoLoginValido() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");

        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", " Maria@X.com ", "password", PASSWORD), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("tokenType", "Bearer").containsEntry("expiresIn", 86400);
        assertThat((String) resp.getBody().get("accessToken")).matches("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$");
    }

    @Test
    @DisplayName("deve retornar 401 em ProblemDetail quando a senha está errada")
    void deveRetornar401QuandoSenhaErrada() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");

        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", "maria@x.com", "password", "Errada123"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:unauthorized");
    }

    @Test
    @DisplayName("deve retornar o usuário autenticado em /auth/me")
    void deveRetornarUsuarioQuandoMe() {
        registrar("Maria Silva", "maria@x.com", "PROVIDER");

        ResponseEntity<Map> resp = get("/api/v1/auth/me", login("maria@x.com"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("email", "maria@x.com").containsEntry("role", "PROVIDER");
    }

    @Test
    @DisplayName("deve retornar 401 em ProblemDetail quando não há token")
    void deveRetornar401QuandoSemToken() {
        ResponseEntity<Map> resp = get("/api/v1/auth/me", null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:unauthorized")
                .containsEntry("title", "Não autenticado");
    }

    @Test
    @DisplayName("deve retornar 401 quando o token foi adulterado")
    void deveRetornar401QuandoTokenAdulterado() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");
        String token = login("maria@x.com");
        String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        ResponseEntity<Map> resp = get("/api/v1/auth/me", adulterado);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:unauthorized");
    }

    @Test
    @DisplayName("RN11: token emitido antes da desativação deixa de valer")
    void deveRetornar401QuandoUsuarioDesativadoAposLogin() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");
        String token = login("maria@x.com");
        jdbc.update("UPDATE users SET is_active = FALSE WHERE email = 'maria@x.com'");

        assertThat(get("/api/v1/auth/me", token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RN11: usuário inativo não faz login")
    void deveRetornar401QuandoLoginDeUsuarioInativo() {
        registrar("Maria Silva", "maria@x.com", "CLIENT");
        jdbc.update("UPDATE users SET is_active = FALSE WHERE email = 'maria@x.com'");

        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", "maria@x.com", "password", PASSWORD), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RN01: cadastros simultâneos com o mesmo e-mail resultam em um 201 e um 409, nunca 500")
    void deveRetornar409QuandoCadastrosSimultaneos() {
        Map<String, String> corpo = Map.of("fullName", "Maria", "email", "corrida@x.com", "password", PASSWORD,
                "role", "CLIENT");
        List<CompletableFuture<HttpStatus>> chamadas = List.of(1, 2, 3, 4).stream()
                .map(i -> CompletableFuture.supplyAsync(() ->
                        (HttpStatus) rest.postForEntity("/api/v1/auth/register", corpo, Map.class).getStatusCode()))
                .toList();

        List<HttpStatus> status = chamadas.stream().map(CompletableFuture::join).toList();

        assertThat(status).containsOnly(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(status).filteredOn(s -> s == HttpStatus.CREATED).hasSize(1);
    }

    @Test
    @DisplayName("deve criar o ADMIN inicial a partir das variáveis de ambiente")
    void deveAutenticarAdminInicial() {
        ResponseEntity<Map> resp = get("/api/v1/auth/me", adminToken());

        assertThat(resp.getBody()).containsEntry("role", "ADMIN").containsEntry("email", ADMIN_EMAIL);
    }
}
