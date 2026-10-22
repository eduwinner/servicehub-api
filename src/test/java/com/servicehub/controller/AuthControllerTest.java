package com.servicehub.controller;

import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.model.Role;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.service.AuthService;
import com.servicehub.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    static final String CADASTRO_VALIDO =
            "{\"fullName\":\"Maria Silva\",\"email\":\"maria@x.com\",\"password\":\"Senha123\",\"role\":\"PROVIDER\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthService authService;

    private static UserResponseDTO usuario(Long id) {
        return new UserResponseDTO(id, "Maria Silva", "maria@x.com", Role.PROVIDER, true, null, null, null, null, null);
    }

    @Test
    @DisplayName("deve retornar 400 listando os campos inválidos quando nome vazio e senha fraca")
    void deveRetornar400ComErrosQuandoNomeVazioESenhaFraca() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"   \",\"email\":\"x@y.com\",\"password\":\"abc\",\"role\":\"CLIENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(endsWith("validation-error")))
                .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("deve retornar 201 com header Location quando cadastrar")
    void deveRetornar201ComLocationQuandoCadastrar() throws Exception {
        when(userService.register(any())).thenReturn(usuario(5L));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/5"));
    }

    @Test
    @DisplayName("deve ignorar campo desconhecido quando cadastrar")
    void deveIgnorarCampoDesconhecidoQuandoCadastrar() throws Exception {
        when(userService.register(any())).thenReturn(usuario(6L));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_VALIDO.replace("}", ",\"isAdmin\":true}")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("deve retornar 409 em ProblemDetail quando o e-mail já existe")
    void deveRetornar409ProblemDetailQuandoEmailDuplicado() throws Exception {
        when(userService.register(any())).thenThrow(new EmailAlreadyExistsException("maria@x.com"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(endsWith("email-already-exists")));
    }
}
