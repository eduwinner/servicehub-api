package com.servicehub.model.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para cadastro ou atualização de um serviço oferecido")
public class ServiceOfferingDTO {

    @Schema(description = "Título do serviço", example = "Instalação elétrica residencial", minLength = 3, maxLength = 150)
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 150, message = "O título deve ter entre 3 e 150 caracteres")
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação e reparo de tomadas, fiação e disjuntores", maxLength = 2000)
    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String description;

    @Schema(description = "Preço do serviço em reais", example = "150.00")
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
    @DecimalMax(value = "99999999.99", message = "O preço deve ser no máximo 99.999.999,99")
    @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo duas casas decimais")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço", example = "ELETRICA")
    @NotNull(message = "A categoria é obrigatória")
    private ServiceCategory category;

    @Schema(description = "ID do usuário que presta o serviço (sai do corpo na Fase 2: passa a vir do token)", example = "1")
    @NotNull(message = "O prestador é obrigatório")
    @Positive(message = "O id do prestador deve ser positivo")
    private Long providerId;
}
