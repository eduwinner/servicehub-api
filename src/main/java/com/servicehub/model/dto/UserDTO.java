package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro ou atualização de um usuário")
public class UserDTO {

    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String fullName;

    @Schema(description = "E-mail do usuário, deve ser único na plataforma", example = "joao.silva@email.com")
    private String email;

    @Schema(description = "Senha do usuário", example = "senha123")
    private String password;
}
