package com.servicehub.security;

import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Transforma um JWT já validado (assinatura e expiração) no usuário autenticado.
 * Consulta o banco a cada requisição: usuário excluído ou desativado depois da emissão do token
 * recebe 401 (RN11), e mudanças de papel valem imediatamente.
 * Registrado como @Bean no SecurityConfig (não @Component): fatias @WebMvcTest carregam todo Converter.
 */
public class JwtUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public JwtUserConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Object uid = jwt.getClaim(TokenService.CLAIM_USER_ID);
        if (!(uid instanceof Long || uid instanceof Integer)) {
            throw new DisabledException("Token sem identificação de usuário válida.");
        }
        User user = userRepository.findById(((Number) uid).longValue())
                .filter(User::isActive)
                .orElseThrow(() -> new DisabledException("Usuário inexistente ou desativado."));
        return new AuthUserAuthentication(new AuthUser(user.getId(), user.getEmail(), user.getRole()), jwt);
    }
}
