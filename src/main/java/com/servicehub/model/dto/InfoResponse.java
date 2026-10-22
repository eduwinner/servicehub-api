package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status e versão da API")
public record InfoResponse(
        @Schema(description = "Situação da API", example = "OK") String status,
        @Schema(description = "Versão do build", example = "0.0.1-SNAPSHOT") String version,
        @Schema(description = "Mensagem informativa", example = "ServiceHub API operacional") String message) {
}
