package com.servicehub.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Dados públicos de um usuário (nunca inclui senha ou hash)")
public class UserResponseDTO {

    @Schema(description = "ID do usuário", example = "7")
    private Long id;

    @Schema(description = "Nome completo do usuário", example = "Maria Silva")
    private String fullName;

    @Schema(description = "E-mail do usuário", example = "maria.silva@email.com")
    private String email;

    @Schema(description = "Telefone com DDD", example = "62999990000")
    private String phone;

    @Schema(description = "Apresentação do usuário", example = "Eletricista há 12 anos")
    private String bio;

    @Schema(description = "URL da foto de perfil", example = "https://cdn.servicehub.com/avatars/7.png")
    private String avatarUrl;

    @Schema(description = "Data de criação", example = "2026-11-05T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última alteração", example = "2026-11-05T14:30:00")
    private LocalDateTime updatedAt;
}
