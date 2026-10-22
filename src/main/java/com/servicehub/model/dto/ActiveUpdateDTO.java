package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ativação ou desativação lógica de uma conta")
public record ActiveUpdateDTO(
        @Schema(description = "true ativa, false desativa", example = "false")
        @NotNull(message = "O campo active é obrigatório")
        Boolean active) {
}
