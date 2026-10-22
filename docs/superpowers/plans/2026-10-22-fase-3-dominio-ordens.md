# ServiceHub API — Fase 3 (Ordens de serviço e avaliações) — Plano de Implementação

> Execução inline (superpowers:executing-plans), aprovada pelo professor para todas as fases.

**Goal:** Implementar a regra de negócio central do ServiceHub: ordens de serviço com máquina de estados, avaliações, bloqueio de exclusão com ordens ativas e alinhamento do banco ao schema entregue aos alunos.

**Spec:** design §5.2 (máquina de estados, invariantes, V9), §5.3 (rotas de ordens e avaliações), §5.12 (métrica); Mapa dos alunos seções 4, 6.4, 6.5, 7 e 9 (RN06–RN10).

## Global Constraints

- Transições: PENDING→ACCEPTED (prestador), PENDING→CANCELLED (cliente ou prestador), ACCEPTED→IN_PROGRESS (prestador), ACCEPTED→CANCELLED (cliente ou prestador), IN_PROGRESS→COMPLETED (prestador). ADMIN executa qualquer transição válida. Outras: 409 `urn:servicehub:problem:invalid-transition`, título "Transição de status inválida", detail "Não é possível mudar de X para Y.". Transição válida por quem não pode: 403. Pessoa sem relação com a ordem: 403.
- `totalPrice` = preço do serviço na criação (RN07). CLIENT e PROVIDER abrem ordens; ADMIN não (403). Serviço próprio ou inativo: 409. Serviço inexistente: 404.
- Avaliação: só o cliente da ordem, só em COMPLETED, uma por ordem (409), nota 1–5 (400).
- RN10: excluir/desativar serviço ou usuário com ordens PENDING/ACCEPTED/IN_PROGRESS → 409.
- Listagem de ordens: só as do usuário (como cliente ou prestador), `?role=client|provider`, `?status=`; ADMIN vê todas; sem N+1.
- Métrica `servicehub.service_requests.transitions` (counter, tag `to`).
- V9 alinha o banco a `docs/alunos/servicehub_schema.sql` (NOT NULLs, CHECKs, `services.updated_at`, FKs `RESTRICT`, índices).

## Tarefas

| # | Entrega | Testes |
|---|---|---|
| 3.1 | V9; `RequestStatus` com regras de transição; entidades `ServiceRequest`, `Review`; `ServiceOffering.updatedAt` | `RequestStatusTest` (matriz completa); contexto sobe com `ddl-auto=validate` |
| 3.2 | `ServiceRequestService` + controller (criar, listar, buscar, mudar status), métrica | `ServiceRequestIntegrationTest` (RN06, RN07, RN08, RN13, N+1, métrica) |
| 3.3 | `ReviewService` + controller (criar, por serviço, por id); RN10 nos services de usuário e serviço | `ReviewIntegrationTest` (RN09), `FluxoCompletoIntegrationTest`, RN10 |
| 3.4 | OpenAPI, README, CLAUDE.md, ADR-0006, tag `fase-3-dominio-ordens` | suíte completa |
