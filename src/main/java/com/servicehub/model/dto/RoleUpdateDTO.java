package com.servicehub.model.dto;

import com.servicehub.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo papel do usuário")
public record RoleUpdateDTO(
        @Schema(description = "Papel", example = "PROVIDER")
        @NotNull(message = "O papel é obrigatório")
        Role role) {
}
