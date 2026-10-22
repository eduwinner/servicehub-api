package com.servicehub.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Autenticação cujo principal é o {@link AuthUser}; as authorities vêm do papel atual no banco. */
public class AuthUserAuthentication extends AbstractAuthenticationToken {

    private final AuthUser user;
    private final Jwt jwt;

    public AuthUserAuthentication(AuthUser user, Jwt jwt) {
        super(List.of(new SimpleGrantedAuthority(user.role().authority())));
        this.user = user;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public AuthUser getPrincipal() {
        return user;
    }

    @Override
    public Jwt getCredentials() {
        return jwt;
    }

    @Override
    public String getName() {
        return user.email();
    }
}
