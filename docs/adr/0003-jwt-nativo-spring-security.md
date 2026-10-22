# ADR-0003 — JWT com o suporte nativo do Spring Security

**Status:** aceito — 2026-10-22 (decisão do professor)

## Contexto

A Aula 9 implementa JWT com a biblioteca `jjwt` (`JwtService` + `OncePerRequestFilter` escrito à mão). O Spring Security já traz um *resource server* JWT completo (`spring-boot-starter-oauth2-resource-server`, Nimbus JOSE), que faz o mesmo sem dependência extra e sem filtro manual.

## Decisão

| Conceito da Aula 9 | Implementação aqui |
|---|---|
| `JwtService.generateToken` (jjwt) | `security/TokenService` com `JwtEncoder` (`NimbusJwtEncoder`, HS256) |
| `JwtAuthFilter extends OncePerRequestFilter` | `BearerTokenAuthenticationFilter` do Spring, ativado por `.oauth2ResourceServer(jwt)` |
| `jwtService.isTokenValid` | `NimbusJwtDecoder` valida assinatura e `exp` |
| `userService.loadUserByUsername` dentro do filtro | `security/JwtUserConverter` carrega o usuário pelo claim `uid` e rejeita inativos |
| `@Value("${jwt.secret}")` | `servicehub.jwt.secret` vindo de `JWT_SECRET`, mínimo 32 bytes; sem ele a aplicação não sobe |

Claims: `sub` (e-mail), `uid`, `roles`, `iss`, `iat`, `exp`. Expiração padrão de 24 h. As authorities vêm do papel atual no banco, não do claim, então promover ou desativar alguém vale na hora.

## Consequências

- Menos código para manter e nenhuma biblioteca de terceiros na cadeia de segurança.
- Em sala, a tabela acima permite mostrar o código da Aula 9 e o equivalente de mercado lado a lado.
- Uma consulta ao banco por requisição autenticada (ver ADR-0002).
- Refresh tokens continuam fora do escopo (não-objetivo do design).
