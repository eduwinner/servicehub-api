package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Varredura de segurança: toda rota protegida responde 401 em ProblemDetail sem token,
 * e toda rota pública responde sem exigir token (Mapa, seção 10).
 */
class ProtectedRoutesIntegrationTest extends AbstractIntegrationTest {

    @ParameterizedTest(name = "{0} {1} sem token → 401")
    @DisplayName("rotas protegidas exigem token")
    @CsvSource({
            "GET,    /api/v1/auth/me",
            "GET,    /api/v1/users",
            "GET,    /api/v1/users/1",
            "PUT,    /api/v1/users/1",
            "DELETE, /api/v1/users/1",
            "PATCH,  /api/v1/users/1/role",
            "PATCH,  /api/v1/users/1/active",
            "POST,   /api/v1/services",
            "PUT,    /api/v1/services/1",
            "DELETE, /api/v1/services/1",
            "POST,   /api/v1/service-requests",
            "GET,    /api/v1/service-requests",
            "GET,    /api/v1/service-requests/1",
            "PATCH,  /api/v1/service-requests/1/status",
            "POST,   /api/v1/service-requests/1/reviews",
            "GET,    /actuator/metrics"
    })
    void deveRetornar401QuandoSemToken(HttpMethod method, String url) {
        ResponseEntity<Map> resp = call(method, url, null, Map.of(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:unauthorized");
    }

    @ParameterizedTest(name = "{0} {1} é público")
    @DisplayName("rotas públicas não exigem token")
    @CsvSource({
            "GET, /api/v1/info",
            "GET, /api/v1/services",
            "GET, /actuator/health",
            "GET, /api-docs/v1"
    })
    void deveResponderSemTokenQuandoRotaPublica(HttpMethod method, String url) {
        ResponseEntity<String> resp = call(method, url, null, null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
