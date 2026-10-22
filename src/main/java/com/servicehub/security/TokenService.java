package com.servicehub.security;

import com.servicehub.model.User;
import com.servicehub.model.dto.TokenResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Emite o JWT (HS256) com sub = e-mail, uid, roles, iat e exp (Aula 9). */
@Service
public class TokenService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLES = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("servicehub-api")
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.expirationSeconds()))
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLES, List.of(user.getRole().authority()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.expirationSeconds());
    }
}
