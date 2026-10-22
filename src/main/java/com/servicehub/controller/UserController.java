package com.servicehub.controller;

import com.servicehub.model.dto.UserDTO;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.model.dto.UserUpdateDTO;
import com.servicehub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Cadastro e perfil das pessoas que usam a plataforma")
public class UserController {

    private static final String PROBLEM = "application/problem+json";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuário",
            description = "Cria um usuário. O e-mail é normalizado (minúsculas, sem espaços) e precisa ser único. "
                    + "A senha nunca aparece nas respostas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos (nome, e-mail ou senha fraca)",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserDTO dto) {
        UserResponseDTO body = userService.createUser(dto);
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.getId())).body(body);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados públicos de um usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public UserResponseDTO findById(@Parameter(description = "ID do usuário", example = "7") @PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Listar usuários",
            description = "Lista paginada de usuários. Paginação: page (a partir de 0), size (padrão 20, máximo 100) e sort (ex.: fullName,asc).")
    @ApiResponse(responseCode = "200", description = "Página de usuários")
    @GetMapping
    public Page<UserResponseDTO> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.findAll(pageable);
    }

    @Operation(summary = "Atualizar perfil",
            description = "Altera nome, telefone, bio e avatar. E-mail, papel e senha não são alterados por este endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public UserResponseDTO update(@Parameter(description = "ID do usuário", example = "7") @PathVariable Long id,
                                  @Valid @RequestBody UserUpdateDTO dto) {
        return userService.updateUser(id, dto);
    }

    @Operation(summary = "Remover usuário", description = "Remove um usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário removido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID do usuário", example = "7") @PathVariable Long id) {
        userService.deleteUser(id);
    }
}
