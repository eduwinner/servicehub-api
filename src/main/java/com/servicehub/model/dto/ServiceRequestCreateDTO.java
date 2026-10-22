package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Abertura de ordem de serviço. Cliente, status e preço são definidos pela API.")
public record ServiceRequestCreateDTO(
        @Schema(description = "ID do serviço contratado", example = "12")
        @NotNull(message = "O serviço é obrigatório")
        @Positive(message = "O id do serviço deve ser positivo")
        Long serviceId,

        @Schema(description = "Data e hora desejadas (horário de Brasília)", example = "2026-11-10T09:00:00")
        @Future(message = "A data agendada deve estar no futuro")
        LocalDateTime scheduledAt,

        @Schema(description = "Observações para o prestador", example = "Apartamento 302, interfone 3021")
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
        String notes) {
}
