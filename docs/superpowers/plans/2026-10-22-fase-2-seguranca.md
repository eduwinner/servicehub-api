# ServiceHub API — Fase 2 (Segurança) — Plano de Implementação

> Execução: superpowers:executing-plans (inline, aprovado pelo professor em 2026-10-22 para todas as fases).

**Goal:** Autenticação JWT stateless, papéis ADMIN/PROVIDER/CLIENT, regras de dono (BOLA), BCrypt, exclusão lógica, CORS e sanitização, com os pendentes da revisão das Fases 0–1.

**Architecture:** Spring Security 7 + `oauth2-resource-server` (JWT HS256 via `NimbusJwtEncoder/Decoder`). Um `Converter<Jwt, AbstractAuthenticationToken>` carrega o usuário pelo claim `uid` a cada requisição, rejeita inativos (401) e usa o papel do banco (mudança de papel vale na hora). O principal é `AuthUser(id, email, role)`; services leem o usuário via `CurrentUser`. `@PreAuthorize` nos services; regras de dono explícitas no service.

**Spec:** `docs/superpowers/specs/2026-10-22-servicehub-api-design.md` §5.2, §5.3, §5.4, §5.5 e §6 (Fase 2).

## Global Constraints

- `JWT_SECRET` obrigatório, mínimo 32 bytes; sem ele a aplicação não sobe. Expiração `JWT_EXPIRATION_SECONDS` (padrão 86400).
- Claims: `sub`=e-mail, `uid`, `roles` (`["ROLE_X"]`), `iat`, `exp`.
- Rotas públicas: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/services/**`, `GET /api/v1/reviews/**`, `GET /api/v1/info`, `/actuator/health`, `/docs/**`, `/swagger-ui/**`, `/api-docs/**`. Todo o resto exige token.
- 401 e 403 sempre em ProblemDetail (`urn:servicehub:problem:unauthorized` / `forbidden`), tanto no filtro quanto em `@PreAuthorize`.
- `POST /users` deixa de existir; cadastro é `POST /auth/register` (`role` só `CLIENT|PROVIDER`).
- `providerId` sai do corpo de serviços: o prestador é o usuário autenticado. DTO de criação e de atualização separados (`active` só no update).
- Exclusão lógica para usuários e serviços; desativar usuário desativa os serviços dele. (409 por ordens ativas entra na Fase 3, quando ordens existirem.)
- Admin inicial por `SERVICEHUB_ADMIN_EMAIL`/`SERVICEHUB_ADMIN_PASSWORD`; ausente → `WARN` e nenhum admin.
- CORS por `CORS_ALLOWED_ORIGINS` (padrão `http://localhost:3000,http://localhost:5173`).
- Texto livre (`description`, `bio`) escapado com `StringEscapeUtils.escapeHtml4` no service.

## Review Focus

1. JWT válido de usuário desativado depois da emissão → 401 (não 200).
2. `PUT /users/{outro}` por um CLIENT → 403; mesmo id inexistente por CLIENT → 403 antes de 404 não é exigido (404 aceitável para ADMIN).
3. `POST /auth/register` com `role: ADMIN` → 400.
4. Token assinado com outra chave / expirado / malformado → 401 ProblemDetail, nunca 500.
5. Dois cadastros simultâneos com o mesmo e-mail → 409, nunca 500.

## Tarefas

| # | Entrega | Testes que provam |
|---|---|---|
| 2.1 | V8 `role`; enum `Role`; `User.role`/`active`; `UserResponseDTO` com `role`, `active`; BCrypt no cadastro; `trim` em `fullName`; corrida de e-mail → 409 | `UserServiceTest` (hash ≠ senha; race), integração register |
| 2.2 | `JwtProperties` (fail-fast), `TokenService`, `SecurityConfig` (stateless, regras públicas, CORS, entry point/access denied em ProblemDetail), `AuthUser`, `CurrentUser`, conversor JWT com checagem de ativo; `AuthController` (`register`, `login`, `me`); `AdminInitializer` | `AuthApiIntegrationTest`: register/login/me, senha errada 401, token adulterado 401, sem token 401 em todas as rotas protegidas, usuário desativado 401 |
| 2.3 | Autorização: `@EnableMethodSecurity`; users (lista ADMIN, ver/editar o próprio ou ADMIN, `PATCH role/active` ADMIN, `DELETE` ADMIN lógico + cascata); services (`POST` PROVIDER com dono do token, `PUT/DELETE` dono ou ADMIN, inativo 404 ao público); handlers 401/403 | `UserSecurityIntegrationTest`, `ServiceSecurityIntegrationTest`, unitários de dono |
| 2.4 | Sanitização, OpenAPI (`bearerAuth`, grupo `public`), testes de fatia web ajustados, README/CLAUDE.md/ADR-0002/0003/0007, tag `fase-2-seguranca` | suíte completa |
