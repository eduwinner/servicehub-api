# ADR-0007 — ADMIN inicial por variável de ambiente

**Status:** aceito — 2026-10-22

## Contexto

Só um ADMIN altera papéis, então alguém precisa ser o primeiro. Um `INSERT` numa migração Flyway versionaria e-mail e hash de senha no Git, e o mesmo admin existiria em todos os ambientes.

## Decisão

`security/AdminInitializer` (um `ApplicationRunner`) cria o ADMIN no startup se `SERVICEHUB_ADMIN_EMAIL` e `SERVICEHUB_ADMIN_PASSWORD` estiverem definidos e o e-mail ainda não existir. Sem as variáveis, registra um `WARN` e não cria nada. A senha é gravada em BCrypt.

## Consequências

- Cada ambiente define o próprio admin; nada sensível no repositório.
- Trocar a variável depois não altera um admin já existente (é só criação).
- Nos testes de integração, o mesmo runner recria o admin depois de cada `TRUNCATE`.
