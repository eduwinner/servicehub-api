package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Actuator (Aula 14): health público, métricas e Prometheus só para ADMIN, métrica de negócio exposta. */
class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("health é público e responde UP")
    void deveResponderHealthPublico() {
        ResponseEntity<Map> resp = get("/actuator/health", null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("métricas são restritas ao ADMIN")
    void deveRetornar403QuandoClienteConsultaMetricas() {
        registrar("Maria", "maria@x.com", "CLIENT");

        assertThat(get("/actuator/metrics", login("maria@x.com")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/actuator/metrics", adminToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Prometheus expõe a métrica de transições de ordens")
    void deveExporMetricaDeNegocioNoPrometheus() {
        registrar("João", "joao@x.com", "PROVIDER");
        registrar("Maria", "maria@x.com", "CLIENT");
        long servico = criarServico(login("joao@x.com"), "Faxina", "80.00", "LIMPEZA");
        post("/api/v1/service-requests", login("maria@x.com"), Map.of("serviceId", servico));

        ResponseEntity<String> resp = call(HttpMethod.GET, "/actuator/prometheus", adminToken(), null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("servicehub_service_requests_transitions_total{to=\"PENDING\"");
    }
}
