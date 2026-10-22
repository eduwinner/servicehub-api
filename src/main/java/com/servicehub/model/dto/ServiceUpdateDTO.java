package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Atualização de serviço: os mesmos campos da criação mais o estado ativo (reativar/desativar). */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Dados para atualizar um serviço; 'active' é opcional e reativa ou desativa o serviço")
public class ServiceUpdateDTO extends ServiceOfferingDTO {

    @Schema(description = "true reativa, false desativa; ausente mantém como está", example = "true")
    private Boolean active;
}
