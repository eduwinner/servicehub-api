package com.servicehub.security;

import com.servicehub.model.Role;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Cria o primeiro ADMIN no startup a partir de SERVICEHUB_ADMIN_EMAIL / SERVICEHUB_ADMIN_PASSWORD
 * (ADR-0007). Nunca grava senha em migração versionada. Sem as variáveis, não cria nada.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${servicehub.admin.email:}") String email,
                            @Value("${servicehub.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password == null || password.isBlank()) {
            log.warn("SERVICEHUB_ADMIN_EMAIL/SERVICEHUB_ADMIN_PASSWORD não definidos: nenhum ADMIN inicial criado.");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setFullName("Administrador");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("ADMIN inicial criado: {}", email);
    }
}
