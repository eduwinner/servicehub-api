package com.servicehub.controller;

import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    static final String USUARIO_VALIDO =
            "{\"fullName\":\"Maria Silva\",\"email\":\"maria@x.com\",\"password\":\"Senha123\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService service;

    @Test
    @DisplayName("deve retornar 404 em ProblemDetail quando o usuário não existe")
    void deveRetornar404ProblemDetailQuandoUsuarioNaoExiste() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Usuário não encontrado: 99"));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value(containsString("99")))
                .andExpect(jsonPath("$.instance").value("/api/v1/users/99"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("deve retornar 409 em ProblemDetail quando o e-mail já existe")
    void deveRetornar409ProblemDetailQuandoEmailDuplicado() throws Exception {
        when(service.createUser(any())).thenThrow(new EmailAlreadyExistsException("maria@x.com"));

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(endsWith("email-already-exists")));
    }

    @Test
    @DisplayName("deve retornar 400 listando os campos inválidos quando nome vazio e senha fraca")
    void deveRetornar400ComErrosQuandoNomeVazioESenhaFraca() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"x@y.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(endsWith("validation-error")))
                .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("deve retornar 201 com header Location quando criar usuário")
    void deveRetornar201ComLocationQuandoCriarUsuario() throws Exception {
        when(service.createUser(any())).thenReturn(usuario(5L));

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/5"));
    }

    @Test
    @DisplayName("deve ignorar campo desconhecido (ex.: role) quando criar usuário")
    void deveIgnorarCampoDesconhecidoQuandoCriarUsuario() throws Exception {
        when(service.createUser(any())).thenReturn(usuario(6L));

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Maria Silva\",\"email\":\"maria@x.com\",\"password\":\"Senha123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
    }

    private static UserResponseDTO usuario(Long id) {
        return new UserResponseDTO(id, "Maria Silva", "maria@x.com", null, null, null, null, null);
    }
}
