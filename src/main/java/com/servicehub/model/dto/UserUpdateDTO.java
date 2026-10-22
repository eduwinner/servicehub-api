package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados editáveis do perfil. E-mail, papel e senha não são alterados por aqui.")
public record UserUpdateDTO(
        @Schema(description = "Nome completo", example = "Maria Souza Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String fullName,

        @Schema(description = "Telefone com DDD, só dígitos", example = "62999990000")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        String phone,

        @Schema(description = "Apresentação do usuário", example = "Eletricista há 12 anos")
        @Size(max = 2000, message = "A bio deve ter no máximo 2000 caracteres")
        String bio,

        @Schema(description = "URL da foto de perfil", example = "https://cdn.servicehub.com/avatars/7.png")
        @Size(max = 500, message = "A URL do avatar deve ter no máximo 500 caracteres")
        String avatarUrl) {
}
