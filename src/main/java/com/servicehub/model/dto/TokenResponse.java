package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso emitido no login")
public record TokenResponse(
        @Schema(description = "JWT a ser enviado em Authorization: Bearer <token>", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,
        @Schema(description = "Validade em segundos", example = "86400")
        long expiresIn) {
}
