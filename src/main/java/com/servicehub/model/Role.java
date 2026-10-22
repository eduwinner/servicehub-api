package com.servicehub.model;

/** Papéis da plataforma (Aula 9). Cada usuário tem exatamente um. */
public enum Role {
    ADMIN,
    PROVIDER,
    CLIENT;

    /** Nome da authority no Spring Security (ex.: ROLE_PROVIDER), usado por hasRole('PROVIDER'). */
    public String authority() {
        return "ROLE_" + name();
    }
}
