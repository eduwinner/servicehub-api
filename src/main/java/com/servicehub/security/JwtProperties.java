package com.servicehub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Configuração do JWT (servicehub.jwt.*), vinda de JWT_SECRET e JWT_EXPIRATION_SECONDS.
 * A aplicação se recusa a subir com segredo ausente ou curto: HS256 exige ao menos 256 bits.
 */
@ConfigurationProperties("servicehub.jwt")
public record JwtProperties(String secret, long expirationSeconds) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("Defina JWT_SECRET com pelo menos " + MIN_SECRET_BYTES
                    + " bytes. A API não sobe sem um segredo forte para assinar os tokens.");
        }
        if (expirationSeconds <= 0) {
            expirationSeconds = 86_400;
        }
    }
}
