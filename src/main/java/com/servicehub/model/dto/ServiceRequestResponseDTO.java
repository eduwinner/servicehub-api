package com.servicehub.model.dto;

import com.servicehub.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ordem de serviço")
public record ServiceRequestResponseDTO(
        @Schema(example = "31") Long id,
        @Schema(example = "12") Long serviceId,
        @Schema(example = "Instalação elétrica residencial") String serviceTitle,
        @Schema(example = "9") Long clientId,
        @Schema(example = "João Pereira") String clientName,
        @Schema(example = "7") Long providerId,
        @Schema(example = "PENDING") RequestStatus status,
        @Schema(description = "Preço congelado na contratação", example = "150.00") BigDecimal totalPrice,
        @Schema(example = "2026-11-10T09:00:00") LocalDateTime scheduledAt,
        @Schema(example = "Apartamento 302, interfone 3021") String notes,
        @Schema(example = "2026-11-05T15:00:00") LocalDateTime createdAt,
        @Schema(example = "2026-11-05T15:00:00") LocalDateTime updatedAt) {
}
