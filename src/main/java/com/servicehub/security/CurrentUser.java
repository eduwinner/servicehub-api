package com.servicehub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Acesso ao usuário autenticado a partir da camada de serviço (regras de dono, BOLA). */
@Component
public class CurrentUser {

    public Optional<AuthUser> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthUser user ? Optional.of(user) : Optional.empty();
    }

    /** Usuário autenticado; chamado só em operações que já exigem login pela SecurityFilterChain. */
    public AuthUser get() {
        return find().orElseThrow(() ->
                new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "Autenticação necessária."));
    }
}
