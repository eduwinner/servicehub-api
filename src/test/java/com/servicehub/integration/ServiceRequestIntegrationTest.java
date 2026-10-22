package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRequestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    EntityManagerFactory emf;

    long servicoJoao;
    long servicoAna;
    String joao;
    String ana;
    String maria;
    long mariaId;

    @BeforeEach
    void cenario() {
        registrar("João Prestador", "joao@x.com", "PROVIDER");
        registrar("Ana Prestadora", "ana@x.com", "PROVIDER");
        mariaId = registrarId("Maria Cliente", "maria@x.com", "CLIENT");
        joao = login("joao@x.com");
        ana = login("ana@x.com");
        maria = login("maria@x.com");
        servicoJoao = criarServico(joao, "Instalação de tomadas", "150.00", "ELETRICA");
        servicoAna = criarServico(ana, "Faxina", "80.00", "LIMPEZA");
    }

    private ResponseEntity<Map> abrir(String token, long serviceId) {
        return post("/api/v1/service-requests", token,
                Map.of("serviceId", serviceId, "scheduledAt", "2099-01-10T09:00:00", "notes", "Apto 302"));
    }

    private long abrirId(String token, long serviceId) {
        ResponseEntity<Map> resp = abrir(token, serviceId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> mudar(String token, long id, String status) {
        return patch("/api/v1/service-requests/" + id + "/status", token, Map.of("status", status));
    }

    @Test
    @DisplayName("deve abrir ordem PENDING com Location e dados do serviço, cliente e prestador")
    void deveAbrirOrdemQuandoClienteContrata() {
        ResponseEntity<Map> resp = abrir(maria, servicoJoao);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation().getPath()).startsWith("/api/v1/service-requests/");
        assertThat(resp.getBody())
                .containsEntry("status", "PENDING")
                .containsEntry("serviceTitle", "Instalação de tomadas")
                .containsEntry("clientName", "Maria Cliente")
                .containsEntry("notes", "Apto 302");
        assertThat(new BigDecimal(resp.getBody().get("totalPrice").toString())).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("RN07: o preço da ordem não muda quando o prestador reajusta o serviço")
    void deveManterPrecoDaOrdemQuandoServicoReajustado() {
        long ordem = abrirId(maria, servicoJoao);
        put("/api/v1/services/" + servicoJoao, joao,
                Map.of("title", "Instalação de tomadas", "price", "300.00", "category", "ELETRICA"));

        Map<?, ?> body = get("/api/v1/service-requests/" + ordem, maria).getBody();

        assertThat(new BigDecimal(body.get("totalPrice").toString())).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("RN06: um prestador também contrata serviços de outros prestadores")
    void devePermitirPrestadorContratarOutroPrestador() {
        assertThat(abrir(ana, servicoJoao).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("RN06: ninguém contrata o próprio serviço")
    void deveRetornar409QuandoContrataProprioServico() {
        ResponseEntity<Map> resp = abrir(joao, servicoJoao);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:business-rule");
    }

    @Test
    @DisplayName("RN06: serviço inativo não aceita novas ordens")
    void deveRetornar409QuandoServicoInativo() {
        delete("/api/v1/services/" + servicoJoao, joao);

        assertThat(abrir(maria, servicoJoao).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("deve retornar 404 para serviço inexistente e 403 para ADMIN abrindo ordem")
    void deveRetornar404Ou403QuandoServicoInexistenteOuAdmin() {
        assertThat(abrir(maria, 9999).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(abrir(adminToken(), servicoJoao).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("deve recusar data agendada no passado")
    void deveRetornar400QuandoAgendamentoNoPassado() {
        ResponseEntity<Map> resp = post("/api/v1/service-requests", maria,
                Map.of("serviceId", servicoJoao, "scheduledAt", "2001-01-01T09:00:00"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("RN08: o prestador conduz a ordem até COMPLETED")
    void deveConcluirOrdemQuandoPrestadorConduz() {
        long ordem = abrirId(maria, servicoJoao);

        assertThat(mudar(joao, ordem, "ACCEPTED").getBody()).containsEntry("status", "ACCEPTED");
        assertThat(mudar(joao, ordem, "IN_PROGRESS").getBody()).containsEntry("status", "IN_PROGRESS");
        assertThat(mudar(joao, ordem, "COMPLETED").getBody()).containsEntry("status", "COMPLETED");
    }

    @Test
    @DisplayName("RN08: o cliente não aceita a própria ordem (403)")
    void deveRetornar403QuandoClienteTentaAceitar() {
        long ordem = abrirId(maria, servicoJoao);

        ResponseEntity<Map> resp = mudar(maria, ordem, "ACCEPTED");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("RN08: transição fora da máquina de estados responde 409 explicando")
    void deveRetornar409QuandoTransicaoInvalida() {
        long ordem = abrirId(maria, servicoJoao);

        ResponseEntity<Map> resp = mudar(joao, ordem, "COMPLETED");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody())
                .containsEntry("type", "urn:servicehub:problem:invalid-transition")
                .containsEntry("title", "Transição de status inválida")
                .containsEntry("detail", "Não é possível mudar de PENDING para COMPLETED.");
    }

    @Test
    @DisplayName("RN08: estados terminais não voltam atrás")
    void deveRetornar409QuandoReabrirOrdemConcluida() {
        long ordem = abrirId(maria, servicoJoao);
        mudar(joao, ordem, "ACCEPTED");
        mudar(joao, ordem, "IN_PROGRESS");
        mudar(joao, ordem, "COMPLETED");

        assertThat(mudar(adminToken(), ordem, "PENDING").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("RN08: cliente cancela enquanto a ordem não começou; ADMIN executa transição válida")
    void devePermitirCancelamentoPeloClienteEAdmin() {
        long o1 = abrirId(maria, servicoJoao);
        long o2 = abrirId(maria, servicoJoao);

        assertThat(mudar(maria, o1, "CANCELLED").getBody()).containsEntry("status", "CANCELLED");
        assertThat(mudar(adminToken(), o2, "ACCEPTED").getBody()).containsEntry("status", "ACCEPTED");
    }

    @Test
    @DisplayName("RN13: quem não participa da ordem não a vê nem a altera")
    void deveRetornar403QuandoTerceiroAcessaOrdem() {
        long ordem = abrirId(maria, servicoJoao);

        assertThat(get("/api/v1/service-requests/" + ordem, ana).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mudar(ana, ordem, "CANCELLED").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/v1/service-requests/" + ordem, joao).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/service-requests/" + ordem, adminToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("RN13: a listagem traz só as ordens do usuário, como cliente ou como prestador")
    void deveListarApenasOrdensDoUsuario() {
        abrirId(maria, servicoJoao);
        abrirId(maria, servicoAna);
        abrirId(ana, servicoJoao);

        assertThat(totalDe(get("/api/v1/service-requests", maria))).isEqualTo(2);
        assertThat(totalDe(get("/api/v1/service-requests", joao))).isEqualTo(2);
        assertThat(totalDe(get("/api/v1/service-requests", ana))).isEqualTo(2);
        assertThat(totalDe(get("/api/v1/service-requests?role=client", ana))).isEqualTo(1);
        assertThat(totalDe(get("/api/v1/service-requests?role=provider", ana))).isEqualTo(1);
        assertThat(totalDe(get("/api/v1/service-requests", adminToken()))).isEqualTo(3);
    }

    @Test
    @DisplayName("deve filtrar ordens por status")
    void deveFiltrarPorStatus() {
        long o1 = abrirId(maria, servicoJoao);
        abrirId(maria, servicoJoao);
        mudar(joao, o1, "ACCEPTED");

        assertThat(totalDe(get("/api/v1/service-requests?status=ACCEPTED", maria))).isEqualTo(1);
        assertThat(get("/api/v1/service-requests?status=FOO", maria).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("deve listar ordens sem N+1 consultas")
    void deveListarOrdensSemNMaisUm() {
        for (int i = 0; i < 4; i++) {
            abrirId(maria, i % 2 == 0 ? servicoJoao : servicoAna);
        }
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        get("/api/v1/service-requests", maria);

        // 1 consulta do usuário (conversor JWT) + 1 da página (o total cabe na página, sem count)
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("deve contar transições na métrica servicehub.service_requests.transitions")
    void deveRegistrarMetricaDeTransicoes() {
        Counter accepted = meterRegistry.counter("servicehub.service_requests.transitions", "to", "ACCEPTED");
        double antes = accepted.count();
        long ordem = abrirId(maria, servicoJoao);

        mudar(joao, ordem, "ACCEPTED");

        assertThat(accepted.count()).isEqualTo(antes + 1);
    }

    private static long totalDe(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) ((Map<?, ?>) resp.getBody().get("page")).get("totalElements")).longValue();
    }

    @SuppressWarnings("unused")
    private static List<?> conteudo(ResponseEntity<Map> resp) {
        return (List<?>) resp.getBody().get("content");
    }
}
