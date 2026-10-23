package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** Achados da revisão final das fases 2–6: acentos, concorrência na máquina de estados e no RN10, travamento do admin. */
class ReviewFixesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationRunner adminInitializer;

    @Test
    @DisplayName("sanitização preserva acentos e escapa só marcação HTML")
    void devePreservarAcentosQuandoSanitiza() {
        registrar("João", "joao@x.com", "PROVIDER");
        String joao = login("joao@x.com");

        ResponseEntity<Map> resp = post("/api/v1/services", joao, Map.of("title", "Instalação elétrica",
                "price", "150.00", "category", "ELETRICA", "description", "Tomadas, disjuntores e fiação <b>já</b>"));

        assertThat(resp.getBody()).containsEntry("description", "Tomadas, disjuntores e fiação &lt;b&gt;já&lt;/b&gt;");
    }

    @Test
    @DisplayName("GET seguido de PUT com o mesmo texto não re-escapa (idempotente)")
    void deveSerIdempotenteQuandoPutReenviaTextoEscapado() {
        registrar("João", "joao@x.com", "PROVIDER");
        String joao = login("joao@x.com");
        Map<?, ?> criado = post("/api/v1/services", joao, Map.of("title", "Faxina", "price", "80.00",
                "category", "LIMPEZA", "description", "Ótimo <b>preço</b>")).getBody();
        Object id = criado.get("id");

        Map<?, ?> depois = put("/api/v1/services/" + id, joao, Map.of("title", "Faxina", "price", "80.00",
                "category", "LIMPEZA", "description", criado.get("description"))).getBody();

        assertThat(depois.get("description")).isEqualTo(criado.get("description"));
    }

    @RepeatedTest(5)
    @DisplayName("RN08: mudanças de status simultâneas não furam a máquina de estados (um 200, um 409)")
    void deveSerializarMudancasDeStatusConcorrentes() throws Exception {
        registrar("João", "joao@x.com", "PROVIDER");
        registrar("Maria", "maria@x.com", "CLIENT");
        String joao = login("joao@x.com");
        String maria = login("maria@x.com");
        long servico = criarServico(joao, "Faxina", "80.00", "LIMPEZA");
        Object ordem = post("/api/v1/service-requests", maria, Map.of("serviceId", servico)).getBody().get("id");
        patch("/api/v1/service-requests/" + ordem + "/status", joao, Map.of("status", "ACCEPTED"));

        List<HttpStatusCode> status = concurrently(
                () -> patch("/api/v1/service-requests/" + ordem + "/status", joao, Map.of("status", "IN_PROGRESS")).getStatusCode(),
                () -> patch("/api/v1/service-requests/" + ordem + "/status", maria, Map.of("status", "CANCELLED")).getStatusCode());

        assertThat(status).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
    }

    @RepeatedTest(5)
    @DisplayName("RN10: excluir serviço e contratá-lo ao mesmo tempo nunca deixa ordem ativa em serviço inativo")
    void deveSerializarExclusaoEContratacaoConcorrentes() throws Exception {
        registrar("João", "joao@x.com", "PROVIDER");
        registrar("Maria", "maria@x.com", "CLIENT");
        String joao = login("joao@x.com");
        String maria = login("maria@x.com");
        long servico = criarServico(joao, "Faxina", "80.00", "LIMPEZA");

        concurrently(
                () -> delete("/api/v1/services/" + servico, joao).getStatusCode(),
                () -> post("/api/v1/service-requests", maria, Map.of("serviceId", servico)).getStatusCode());

        Long orfas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM service_requests r JOIN services s ON s.id = r.service_id
                WHERE s.is_active = FALSE AND r.status IN ('PENDING','ACCEPTED','IN_PROGRESS')""", Long.class);
        assertThat(orfas).isZero();
    }

    @Test
    @DisplayName("o ADMIN não pode desativar nem rebaixar a própria conta")
    void deveRetornar409QuandoAdminDesativaOuRebaixaASiMesmo() {
        String admin = adminToken();
        Object adminId = get("/api/v1/auth/me", admin).getBody().get("id");

        assertThat(delete("/api/v1/users/" + adminId, admin).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(patch("/api/v1/users/" + adminId + "/role", admin, Map.of("role", "CLIENT")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(get("/api/v1/auth/me", admin).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("o startup recupera o ADMIN configurado se ele foi desativado ou rebaixado por SQL")
    void deveRecuperarAdminConfiguradoNoStartup() throws Exception {
        jdbc.update("UPDATE users SET is_active = FALSE, role = 'CLIENT' WHERE email = ?", ADMIN_EMAIL);

        adminInitializer.run(null);

        assertThat(get("/api/v1/auth/me", adminToken()).getBody()).containsEntry("role", "ADMIN");
    }

    private static List<HttpStatusCode> concurrently(Supplier<HttpStatusCode> a, Supplier<HttpStatusCode> b) {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Supplier<HttpStatusCode> wa = () -> { await(barrier); return a.get(); };
        Supplier<HttpStatusCode> wb = () -> { await(barrier); return b.get(); };
        CompletableFuture<HttpStatusCode> fa = CompletableFuture.supplyAsync(wa);
        CompletableFuture<HttpStatusCode> fb = CompletableFuture.supplyAsync(wb);
        return List.of(fa.join(), fb.join());
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
