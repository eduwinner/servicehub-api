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

/** Avaliações (RN09), bloqueio de exclusão com ordens ativas (RN10) e o ciclo completo de ponta a ponta. */
class ReviewIntegrationTest extends AbstractIntegrationTest {

    long servico;
    long joaoId;
    String joao;
    String maria;
    String ana;

    @BeforeEach
    void cenario() {
        joaoId = registrarId("João Prestador", "joao@x.com", "PROVIDER");
        registrar("Maria Cliente", "maria@x.com", "CLIENT");
        registrar("Ana Cliente", "ana@x.com", "CLIENT");
        joao = login("joao@x.com");
        maria = login("maria@x.com");
        ana = login("ana@x.com");
        servico = criarServico(joao, "Instalação de tomadas", "150.00", "ELETRICA");
    }

    private long ordem() {
        ResponseEntity<Map> resp = post("/api/v1/service-requests", maria, Map.of("serviceId", servico));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private long ordemConcluida() {
        long id = ordem();
        for (String s : List.of("ACCEPTED", "IN_PROGRESS", "COMPLETED")) {
            assertThat(patch("/api/v1/service-requests/" + id + "/status", joao, Map.of("status", s))
                    .getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        return id;
    }

    private ResponseEntity<Map> avaliar(String token, long ordem, Object rating) {
        return post("/api/v1/service-requests/" + ordem + "/reviews", token,
                Map.of("rating", rating, "comment", "Pontual e caprichoso."));
    }

    @Test
    @DisplayName("ciclo completo: cadastro, serviço, ordem, aceite, execução, conclusão e avaliação")
    void deveExecutarCicloCompleto() {
        long ordem = ordemConcluida();

        ResponseEntity<Map> review = avaliar(maria, ordem, 5);

        assertThat(review.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(review.getHeaders().getLocation().getPath()).startsWith("/api/v1/reviews/");
        assertThat(review.getBody()).containsEntry("rating", 5).containsEntry("reviewerName", "Maria Cliente");
        long reviewId = ((Number) review.getBody().get("id")).longValue();

        assertThat(get("/api/v1/reviews/" + reviewId, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> page = get("/api/v1/services/" + servico + "/reviews", null).getBody();
        assertThat((List<?>) page.get("content")).hasSize(1);
    }

    @Test
    @DisplayName("RN09: só depois de COMPLETED")
    void deveRetornar409QuandoAvaliarOrdemNaoConcluida() {
        ResponseEntity<Map> resp = avaliar(maria, ordem(), 5);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("RN09: só quem abriu a ordem avalia")
    void deveRetornar403QuandoOutroAvalia() {
        long ordem = ordemConcluida();

        assertThat(avaliar(joao, ordem, 5).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(avaliar(ana, ordem, 5).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("RN09: uma avaliação por ordem")
    void deveRetornar409QuandoAvaliarDuasVezes() {
        long ordem = ordemConcluida();
        avaliar(maria, ordem, 5);

        assertThat(avaliar(maria, ordem, 4).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("deve validar nota de 1 a 5")
    void deveRetornar400QuandoNotaForaDaFaixa() {
        long ordem = ordemConcluida();

        assertThat(avaliar(maria, ordem, 6).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(avaliar(maria, ordem, 0).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("deve retornar 404 para avaliação e ordem inexistentes")
    void deveRetornar404QuandoInexistente() {
        assertThat(get("/api/v1/reviews/999", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(avaliar(maria, 999, 5).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("RN10: serviço com ordem ativa não pode ser excluído; depois de concluída, pode")
    void deveRetornar409QuandoExcluirServicoComOrdemAtiva() {
        long ordem = ordem();

        ResponseEntity<Map> resp = delete("/api/v1/services/" + servico, joao);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        patch("/api/v1/service-requests/" + ordem + "/status", maria, Map.of("status", "CANCELLED"));
        assertThat(delete("/api/v1/services/" + servico, joao).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("RN10: usuário com ordem ativa (como cliente ou prestador) não pode ser desativado")
    void deveRetornar409QuandoDesativarUsuarioComOrdemAtiva() {
        ordem();

        assertThat(delete("/api/v1/users/" + joaoId, adminToken()).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
