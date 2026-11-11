package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porta interna de métricas (ADR-0008): o Prometheus coleta /actuator/prometheus sem token
 * numa porta que o Compose não publica; na porta pública tudo continua como antes.
 */
class MetricsInternalPortIntegrationTest extends AbstractIntegrationTest {

    private static final int PORTA_INTERNA = portaLivre();

    @DynamicPropertySource
    static void portaDeMetricas(DynamicPropertyRegistry registry) {
        registry.add("servicehub.metrics.internal-port", () -> PORTA_INTERNA);
    }

    private String interna(String caminho) {
        return "http://localhost:" + PORTA_INTERNA + caminho;
    }

    @Test
    @DisplayName("porta interna entrega /actuator/prometheus sem token")
    void deveColetarPrometheusSemTokenNaPortaInterna() {
        ResponseEntity<String> resp = rest.getForEntity(interna("/actuator/prometheus"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    @DisplayName("porta pública continua exigindo ADMIN em /actuator/prometheus")
    void deveRetornar401SemTokenNaPortaPublica() {
        ResponseEntity<String> resp = rest.getForEntity("/actuator/prometheus", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("porta interna não abre outros endpoints do Actuator nem da API")
    void deveRetornar401EmOutrasRotasNaPortaInterna() {
        assertThat(rest.getForEntity(interna("/actuator/metrics"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity(interna("/api/v1/users"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static int portaLivre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
