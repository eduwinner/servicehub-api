package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Avaliação")
public record ReviewResponseDTO(
        @Schema(example = "4") Long id,
        @Schema(example = "31") Long requestId,
        @Schema(example = "12") Long serviceId,
        @Schema(example = "9") Long reviewerId,
        @Schema(example = "João Pereira") String reviewerName,
        @Schema(example = "5") Integer rating,
        @Schema(example = "Pontual e caprichoso.") String comment,
        @Schema(example = "2026-11-10T12:10:00") LocalDateTime createdAt) {
}
