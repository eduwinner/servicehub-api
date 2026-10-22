package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de um usuário")
public class UserDTO {

    @Schema(description = "Nome completo do usuário", example = "Maria Silva", minLength = 2, maxLength = 100)
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    private String fullName;

    @Schema(description = "E-mail do usuário, único na plataforma (comparado sem diferenciar maiúsculas)",
            example = "maria.silva@email.com", maxLength = 255)
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    @Schema(description = "Senha: 8 a 72 caracteres, ao menos uma letra maiúscula e um número",
            example = "Senha123", minLength = 8, maxLength = 72)
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "A senha deve conter ao menos uma letra maiúscula e um número")
    private String password;
}
