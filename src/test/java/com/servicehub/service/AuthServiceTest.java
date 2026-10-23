package com.servicehub.service;

import com.servicehub.model.dto.LoginDTO;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.CurrentUser;
import com.servicehub.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock UserService userService;
    @Mock CurrentUser currentUser;

    @InjectMocks AuthService service;

    @Test
    @DisplayName("e-mail inexistente ainda executa o BCrypt (sem oráculo de tempo para enumerar contas)")
    void deveExecutarBcryptQuandoEmailInexistente() {
        when(userRepository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginDTO("ninguem@x.com", "Senha123")))
                .isInstanceOf(BadCredentialsException.class);
        verify(passwordEncoder).matches(eq("Senha123"), anyString());
    }
}
