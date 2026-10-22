package com.servicehub.controller;

import com.servicehub.model.dto.LoginDTO;
import com.servicehub.model.dto.RegisterDTO;
import com.servicehub.model.dto.TokenResponse;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.service.AuthService;
import com.servicehub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Cadastro, login (JWT) e dados do usuário autenticado")
public class AuthController {

    private static final String PROBLEM = "application/problem+json";

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar usuário",
            description = "Cadastro público de cliente (CLIENT) ou prestador (PROVIDER). O e-mail é normalizado e precisa "
                    + "ser único; a senha é gravada como hash BCrypt e nunca aparece nas respostas.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos, senha fraca ou papel diferente de CLIENT/PROVIDER",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterDTO dto) {
        UserResponseDTO body = userService.register(dto);
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.getId())).body(body);
    }

    @Operation(summary = "Fazer login",
            description = "Troca e-mail e senha por um JWT válido por 24 horas. Envie-o nas demais chamadas no header "
                    + "Authorization: Bearer <token>.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "400", description = "E-mail ou senha ausentes",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou conta desativada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @Operation(summary = "Usuário autenticado", description = "Retorna os dados de quem está chamando, a partir do token.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado ou de conta desativada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me")
    public UserResponseDTO me() {
        return authService.me();
    }
}
