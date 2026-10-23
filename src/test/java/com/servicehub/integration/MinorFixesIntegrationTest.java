package com.servicehub.integration;

import com.servicehub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Pendentes menores da revisão final das fases 2–6. */
class MinorFixesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JwtEncoder jwtEncoder;

    private String tokenComUid(Object uid) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("x@x.com").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .claim("uid", uid).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("claim uid não inteiro (1.7) é recusado com 401, mesmo com assinatura válida")
    void deveRetornar401QuandoUidNaoInteiro() {
        ResponseEntity<Map> resp = get("/api/v1/auth/me", tokenComUid(1.7));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/v1/auth/me", tokenComUid(1)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("RN12: dono e ADMIN continuam vendo as avaliações de um serviço inativo")
    void deveMostrarAvaliacoesDeServicoInativoAoDonoEAdmin() {
        registrar("João", "joao@x.com", "PROVIDER");
        String joao = login("joao@x.com");
        long servico = criarServico(joao, "Faxina", "80.00", "LIMPEZA");
        delete("/api/v1/services/" + servico, joao);
        String url = "/api/v1/services/" + servico + "/reviews";

        assertThat(get(url, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(url, joao).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(url, adminToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("rota inexistente responde 404 em ProblemDetail mesmo sem token")
    void deveRetornar404QuandoRotaInexistenteSemToken() {
        ResponseEntity<Map> resp = get("/api/v1/nada", null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:not-found");
    }

    @Test
    @DisplayName("método não suportado responde 405 em ProblemDetail mesmo sem token")
    void deveRetornar405QuandoMetodoNaoSuportadoSemToken() {
        ResponseEntity<Map> resp = call(HttpMethod.DELETE, "/api/v1/info", null, null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resp.getBody()).containsEntry("type", "urn:servicehub:problem:method-not-allowed");
    }

    @Test
    @DisplayName("401 dos filtros segue o mesmo contrato do GlobalExceptionHandler (timestamp e instance)")
    void deveManterContratoNo401DosFiltros() {
        ResponseEntity<Map> resp = get("/api/v1/auth/me", null);

        assertThat(resp.getBody()).containsKeys("type", "title", "status", "detail", "instance", "timestamp")
                .containsEntry("status", 401).containsEntry("instance", "/api/v1/auth/me");
    }

    @Test
    @DisplayName("rota protegida existente continua 401 sem token (não vira 404)")
    void deveContinuar401QuandoRotaExiste() {
        assertThat(get("/api/v1/service-requests", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(List.of(get("/api/v1/users/1", null).getStatusCode())).containsOnly(HttpStatus.UNAUTHORIZED);
    }
}
