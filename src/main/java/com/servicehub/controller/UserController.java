package com.servicehub.controller;

import com.servicehub.model.dto.UserDTO;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.model.dto.UserUpdateDTO;
import com.servicehub.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Tag(
  name = "Usuários",
  description = "Operações de cadastro e gerenciamento de usuários da plataforma ServiceHub"
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário na plataforma")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "500", description = "E-mail já cadastrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserDTO dto) {
        UserResponseDTO body = userService.createUser(dto);
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.getId())).body(body);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "500", description = "Usuário não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @GetMapping("/{id}")
    public UserResponseDTO findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso")
    @GetMapping
    public Page<UserResponseDTO> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.findAll(pageable);
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Usuário não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @PutMapping("/{id}")
    public UserResponseDTO update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return userService.updateUser(id, dto);
    }

    @Operation(summary = "Remover usuário", description = "Exclui um usuário da plataforma")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
        @ApiResponse(responseCode = "500", description = "Usuário não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
