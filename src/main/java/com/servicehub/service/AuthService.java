package com.servicehub.service;

import com.servicehub.model.User;
import com.servicehub.model.dto.LoginDTO;
import com.servicehub.model.dto.TokenResponse;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.CurrentUser;
import com.servicehub.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID = "E-mail ou senha inválidos.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserService userService;
    private final CurrentUser currentUser;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       UserService userService, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /** Mesma resposta para e-mail inexistente, senha errada ou conta inativa: não revela qual falhou. */
    public TokenResponse login(LoginDTO dto) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(dto.email()))
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(dto.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException(INVALID));
        return tokenService.issue(user);
    }

    public UserResponseDTO me() {
        return userService.findById(currentUser.get().id());
    }
}
