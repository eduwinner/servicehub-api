# ADR-0002 — Exclusão lógica de usuários e serviços

**Status:** aceito — 2026-10-22

## Contexto

A Aula 4 implementa `DELETE` físico, e a migração V4 criou `services.provider_id` com `ON DELETE CASCADE`. Apagar um prestador apagaria os serviços dele e, a partir da Fase 3, as ordens e avaliações de outras pessoas ligadas a esses serviços. A tabela `users` já tinha `is_active` desde a V3, sem uso.

## Decisão

- `DELETE /users/{id}` (ADMIN) e `DELETE /services/{id}` (dono ou ADMIN) marcam `is_active = false`; nenhuma linha é apagada.
- Desativar um usuário desativa todos os serviços dele na mesma transação (`deactivateAllByProvider`). Reativar o usuário (`PATCH /users/{id}/active`) **não** reativa os serviços: o prestador escolhe quais voltam (`PUT` com `active: true`).
- Usuário inativo não faz login, e um JWT emitido antes da desativação deixa de valer: o conversor JWT consulta o usuário a cada requisição (uma busca por chave primária). É o preço de ter revogação sem refresh tokens nem lista de bloqueio.
- Serviço inativo some das listagens e responde 404 ao público; o dono e o ADMIN continuam vendo (o ADMIN lista com `includeInactive=true`).
- Fase 3: excluir usuário ou serviço com ordens `PENDING`, `ACCEPTED` ou `IN_PROGRESS` responde 409 (RN10), e a V9 troca o `CASCADE` por `RESTRICT`.

## Consequências

- Histórico preservado e auditável. LGPD (direito ao esquecimento) exigiria anonimização, que fica fora do escopo da disciplina.
- Toda consulta de catálogo precisa filtrar `is_active`; isso está centralizado em `ServiceOfferingSpecifications`.
