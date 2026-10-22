package com.servicehub.service;

import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.User;
import com.servicehub.model.dto.UserDTO;
import com.servicehub.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    UserService service;

    private static UserDTO dto(String email) {
        UserDTO dto = new UserDTO();
        dto.setFullName("Maria Silva");
        dto.setEmail(email);
        dto.setPassword("Senha123");
        return dto;
    }

    @Test
    @DisplayName("deve lançar EmailAlreadyExists quando o e-mail já está cadastrado")
    void deveLancarEmailAlreadyExistsQuandoEmailJaCadastrado() {
        when(repository.existsByEmail("maria@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(dto("maria@x.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve normalizar o e-mail (trim + minúsculas) antes de verificar e salvar")
    void deveNormalizarEmailQuandoCriar() {
        when(repository.existsByEmail("maria@x.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createUser(dto(" Maria@X.com "));

        ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
        verify(repository).existsByEmail("maria@x.com");
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("maria@x.com");
    }

    @Test
    @DisplayName("deve lançar ResourceNotFound quando o id não existe")
    void deveLancarResourceNotFoundQuandoIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
