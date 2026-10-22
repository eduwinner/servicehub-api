package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginDTO(
        @Schema(description = "E-mail cadastrado", example = "maria.silva@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,
        @Schema(description = "Senha", example = "Senha123")
        @NotBlank(message = "A senha é obrigatória")
        String password) {
}
