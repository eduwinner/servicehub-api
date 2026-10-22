package com.servicehub.service;

import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.User;
import com.servicehub.model.dto.RegisterDTO;
import com.servicehub.model.Role;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    CurrentUser currentUser;

    @InjectMocks
    UserService service;

    private static RegisterDTO dto(String email) {
        RegisterDTO dto = new RegisterDTO();
        dto.setFullName("Maria Silva");
        dto.setEmail(email);
        dto.setPassword("Senha123");
        dto.setRole("CLIENT");
        return dto;
    }

    @Test
    @DisplayName("deve lançar EmailAlreadyExists quando o e-mail já está cadastrado")
    void deveLancarEmailAlreadyExistsQuandoEmailJaCadastrado() {
        when(repository.existsByEmail("maria@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(dto("maria@x.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deve normalizar o e-mail (trim + minúsculas) antes de verificar e salvar")
    void deveNormalizarEmailQuandoCriar() {
        when(repository.existsByEmail("maria@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("$2a$hash");
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(dto(" Maria@X.com "));

        ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
        verify(repository).existsByEmail("maria@x.com");
        verify(repository).saveAndFlush(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("maria@x.com");
        assertThat(salvo.getValue().getPasswordHash()).isEqualTo("$2a$hash");
    }

    @Test
    @DisplayName("deve lançar ResourceNotFound quando o id não existe")
    void deveLancarResourceNotFoundQuandoIdInexistente() {
        when(currentUser.get()).thenReturn(new AuthUser(1L, "admin@x.com", Role.ADMIN));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("RN01: deve traduzir violação de UNIQUE (corrida) em EmailAlreadyExists")
    void deveLancarEmailAlreadyExistsQuandoUniqueViolada() {
        when(repository.existsByEmail("maria@x.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(repository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.register(dto("maria@x.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("RN13: deve negar acesso ao perfil de outro usuário antes de consultar o banco")
    void deveNegarQuandoClienteBuscaOutroUsuario() {
        when(currentUser.get()).thenReturn(new AuthUser(5L, "maria@x.com", Role.CLIENT));

        assertThatThrownBy(() -> service.findById(6L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(repository, never()).findById(any());
    }
}
