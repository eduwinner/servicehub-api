package com.servicehub.model.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para cadastro ou atualização de um serviço oferecido")
public class ServiceOfferingDTO {

    @Schema(description = "Título do serviço", example = "Instalação elétrica residencial")
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação e reparo de tomadas, fiação e disjuntores")
    private String description;

    @Schema(description = "Preço do serviço", example = "150.00")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço")
    private ServiceCategory category;

    @Schema(description = "ID do usuário que presta o serviço", example = "1")
    private Long providerId;
}
