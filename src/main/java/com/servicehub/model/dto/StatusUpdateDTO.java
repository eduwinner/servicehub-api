package com.servicehub.model.dto;

import com.servicehub.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo status da ordem (ver máquina de estados)")
public record StatusUpdateDTO(
        @Schema(description = "Status de destino", example = "ACCEPTED")
        @NotNull(message = "O status é obrigatório")
        RequestStatus status) {
}
