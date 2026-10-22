package com.servicehub.security;

import com.servicehub.model.Role;

/** Usuário autenticado da requisição atual, montado a partir do JWT e conferido no banco. */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
