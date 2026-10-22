package com.servicehub.config;

import com.servicehub.controller.InfoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(InfoController.class)
class RequestIdFilterTest {

    private static final String UUID_REGEX =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("deve gerar um X-Request-Id (UUID) quando o cliente não envia")
    void deveGerarRequestIdQuandoHeaderAusente() throws Exception {
        String id = mockMvc.perform(get("/api/v1/info"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).matches(UUID_REGEX);
    }

    @Test
    @DisplayName("deve ecoar o X-Request-Id quando o valor enviado é válido")
    void deveEcoarRequestIdQuandoHeaderValido() throws Exception {
        String id = mockMvc.perform(get("/api/v1/info").header("X-Request-Id", "abc-123.X_9"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).isEqualTo("abc-123.X_9");
    }

    @Test
    @DisplayName("deve substituir por UUID quando o X-Request-Id é longo demais")
    void deveSubstituirRequestIdQuandoHeaderLongoDemais() throws Exception {
        String enviado = "a".repeat(65);
        String id = mockMvc.perform(get("/api/v1/info").header("X-Request-Id", enviado))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).isNotEqualTo(enviado).matches(UUID_REGEX);
    }

    @Test
    @DisplayName("deve substituir por UUID quando o X-Request-Id tem caracteres inválidos")
    void deveSubstituirRequestIdQuandoHeaderComCaracteresInvalidos() throws Exception {
        String id = mockMvc.perform(get("/api/v1/info").header("X-Request-Id", "<script>"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).matches(UUID_REGEX);
    }
}
