package com.servicehub.model.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Dados públicos de um serviço oferecido")
public class ServiceOfferingResponseDTO {

    @Schema(description = "ID do serviço", example = "1")
    private Long id;

    @Schema(description = "Título do serviço", example = "Instalação elétrica residencial")
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação e reparo de tomadas, fiação e disjuntores")
    private String description;

    @Schema(description = "Preço do serviço", example = "150.00")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço", example = "ELETRICA")
    private ServiceCategory category;

    @Schema(description = "Indica se o serviço está ativo", example = "true")
    private boolean active;

    @Schema(description = "Data de criação do serviço", example = "2026-11-05T14:35:00")
    private LocalDateTime createdAt;

    @Schema(description = "ID do usuário que presta o serviço", example = "1")
    private Long providerId;

    @Schema(description = "Nome do usuário que presta o serviço", example = "João Silva")
    private String providerName;
}
