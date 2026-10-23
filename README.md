# ServiceHub API

[![CI](https://github.com/eduwinner/servicehub-api/actions/workflows/ci.yml/badge.svg)](https://github.com/eduwinner/servicehub-api/actions/workflows/ci.yml)

API REST de gestão de prestadores de serviço, serviços, ordens de serviço e avaliações. É a **implementação de referência** do projeto PBL da disciplina *Desenvolvimento de API Back-End* (Engenharia de Software, 6º período, UniEVANGÉLICA).

O projeto evolui em fases, uma por bloco de aulas, e cada fase tem uma tag git. Assim dá para comparar, em sala, o que cada etapa acrescentou: `git diff fase-0-baseline fase-1-contrato-qualidade`.

## Pré-requisitos

- JDK 21
- Docker Desktop (rodando)
- Git

## Rodar com Docker (recomendado)

```bash
docker compose up -d --build
```

- API em `http://localhost:8090` (Swagger em `/docs`), PostgreSQL 16 em `localhost:5433`.
- A API só sobe depois que o Postgres passa no healthcheck; a própria API tem healthcheck em `/actuator/health`.
- Dados persistem no volume `pg_data` (`docker compose down` mantém; `down -v` apaga).
- Variáveis: crie um `.env` (opcional) para trocar `JWT_SECRET`, senha do admin, portas (`API_PORT`, `DB_PORT`) etc. Os padrões do Compose são só para desenvolvimento.
- pgAdmin (porta 8091, só em localhost, sem login): `docker compose --profile tools up -d`. Já abre com dois servidores no grupo **ServiceHub**: o banco deste projeto e o "Banco do aluno" em `localhost:5433`. Para inspecionar só o banco de uma equipe (com o compose dela rodando), suba apenas o pgAdmin: `docker compose --profile tools up -d pgadmin`. Usuário/senha do banco do aluno diferentes do padrão: defina `STUDENT_DB_USER`/`STUDENT_DB_PASS` ou ajuste em Properties.
- Depuração remota (perfil dev, porta 5005): `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build`.
- Admin inicial padrão do Compose: `admin@servicehub.com` / `Admin123`.

A imagem é multi-stage (Maven + JDK no build, só JRE Alpine no runtime), roda com usuário não-root e no fuso `America/Sao_Paulo`. No perfil `prod` (padrão do Compose) os logs saem em JSON (formato ECS).

## Como rodar localmente

```bash
docker compose up -d postgres        # Postgres em localhost:5433
# a API não sobe sem JWT_SECRET (mínimo 32 caracteres)
export JWT_SECRET=troque-este-segredo-de-desenvolvimento-com-32-bytes
export SERVICEHUB_ADMIN_EMAIL=admin@servicehub.com SERVICEHUB_ADMIN_PASSWORD=Admin123
./mvnw.cmd spring-boot:run           # Windows  (./mvnw no Linux/macOS)
```

A API sobe em `http://localhost:8080`.

### Variáveis de ambiente

Todas têm valor padrão de desenvolvimento; defina-as (ou crie um `.env`, nunca versionado) para mudar.

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/servicehub` | URL JDBC do Postgres |
| `DB_USER` | `servicehub` | Usuário do banco |
| `DB_PASS` | `secret` | Senha do banco |
| `DB_NAME` | `servicehub` | Nome do banco (usado pelo Compose) |
| `SERVER_PORT` | `8080` | Porta HTTP da API |
| `JWT_SECRET` | *(obrigatório)* | Chave HS256 dos tokens, mínimo 32 bytes; sem ela a API não sobe |
| `JWT_EXPIRATION_SECONDS` | `86400` | Validade do token (24 h) |
| `SERVICEHUB_ADMIN_EMAIL` / `SERVICEHUB_ADMIN_PASSWORD` | *(vazio)* | Cria o primeiro ADMIN no startup ([ADR-0007](docs/adr/0007-admin-inicial-por-variavel-de-ambiente.md)) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origens do navegador autorizadas (a bancada de testes usa 5173) |

## Como testar

```bash
./mvnw.cmd verify
```

Os testes sobem um PostgreSQL real em container (Testcontainers), então **o Docker precisa estar rodando**. Não é preciso subir o Compose antes. O relatório de cobertura (JaCoCo) fica em `target/site/jacoco/index.html`.

## Documentação

- Swagger UI: `http://localhost:8080/docs`
- Especificação OpenAPI (grupo v1): `http://localhost:8080/api-docs/v1`
- Todas as rotas da API começam com `/api/v1`. Listagens são paginadas (`?page=0&size=20&sort=id`, máximo 100 por página).

## Autenticação

1. `POST /api/v1/auth/register` com `fullName`, `email`, `password` e `role` (`CLIENT` ou `PROVIDER`).
2. `POST /api/v1/auth/login` devolve `accessToken` (JWT, 24 h).
3. Envie `Authorization: Bearer <accessToken>` nas rotas protegidas. No Swagger, use o botão **Authorize**.

Públicas: cadastro, login, leitura do catálogo de serviços e `/api/v1/info`. Detalhes em [ADR-0003](docs/adr/0003-jwt-nativo-spring-security.md).

## Fluxo principal

| Passo | Quem | Chamada |
|---|---|---|
| Publicar serviço | PROVIDER | `POST /api/v1/services` |
| Contratar (ordem PENDING) | CLIENT ou outro PROVIDER | `POST /api/v1/service-requests` |
| Aceitar, iniciar, concluir | prestador do serviço | `PATCH /api/v1/service-requests/{id}/status` |
| Cancelar (antes de iniciar) | cliente ou prestador | `PATCH /api/v1/service-requests/{id}/status` |
| Avaliar (nota 1–5) | quem contratou, após COMPLETED | `POST /api/v1/service-requests/{id}/reviews` |

Regras da máquina de estados em [ADR-0006](docs/adr/0006-maquina-de-estados-da-ordem.md).

## Testar manualmente

- **Swagger UI** em `/docs`: faça login em `POST /api/v1/auth/login` e use o botão **Authorize**.
- **Postman**: importe [docs/postman/servicehub.postman_collection.json](docs/postman/servicehub.postman_collection.json) e rode a coleção inteira (Collection Runner). Ela executa o ciclo completo e confere status codes; ajuste `baseUrl` se não estiver no Compose (8090).

## Observabilidade

- `GET /actuator/health` (público) e `GET /actuator/info`.
- `GET /actuator/metrics` e `GET /actuator/prometheus` (só ADMIN): métricas da JVM, HTTP e a métrica de negócio `servicehub_service_requests_transitions_total{to="..."}`, que conta quantas ordens chegaram a cada status.
- Logs com `X-Request-Id` em cada linha; no perfil `prod`, em JSON (ECS).

## Integração contínua

O workflow `.github/workflows/ci.yml` roda `./mvnw verify` (testes com PostgreSQL real e cobertura) a cada push e pull request no `main`, publica o relatório JaCoCo como artefato, constrói a imagem e sobe o Compose para conferir o healthcheck.

## Contrato de erro

Todo erro responde `application/problem+json` no formato ProblemDetail (RFC 9457). Exemplo de validação:

```json
{
  "type": "urn:servicehub:problem:validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/v1/services",
  "timestamp": "2026-11-05T17:30:12Z",
  "errors": [{ "field": "price", "message": "O preço deve ser maior que zero" }]
}
```

Toda resposta traz o header `X-Request-Id`, também gravado nos logs, para rastrear uma requisição. Detalhes em [ADR-0004](docs/adr/0004-problem-detail-rfc-9457.md).

## Bancada de testes (professor)

[frontend/](frontend/README.md) traz uma página que executa o ciclo completo do contrato contra a API de qualquer equipe e mostra cada requisição e resposta, além de um roteiro Playwright para rodar o ciclo ao vivo em sala (`npm run aovivo`). A API precisa liberar CORS para `http://localhost:5173`.

## Material dos alunos

- [Mapa de Construção (PDF)](docs/alunos/Mapa.pdf): schema, endpoints e regras de negócio que a API deve atingir.
- [Schema-alvo PostgreSQL](docs/alunos/servicehub_schema.sql).

## Fases e tags

| Fase | Tag | Aulas | Conteúdo |
|---|---|---|---|
| 0 | `fase-0-baseline` | 3–5 | Projeto base, Java 21, testes com Testcontainers |
| 1 | `fase-1-contrato-qualidade` | 2, 5, 6, 8 | `/api/v1`, erros `ProblemDetail`, validação, paginação, OpenAPI |
| 2 | `fase-2-seguranca` | 8, 9 | Spring Security, BCrypt, JWT, papéis, BOLA |
| 3 | `fase-3-dominio-ordens` | 4, 9 | Ordens de serviço, máquina de estados, avaliações, V9 alinhada ao schema dos alunos |
| 4 | `fase-4-testes` | 10 | Pirâmide de testes completa, cobertura, Postman |
| 5 | `fase-5-docker` | 11, 12 | Dockerfile multi-stage, Compose com healthcheck e perfis |
| 6 | `fase-6-ci-observabilidade` | 14, 16 | GitHub Actions, Actuator, Prometheus, logs estruturados |

Design completo: [docs/superpowers/specs/2026-10-22-servicehub-api-design.md](docs/superpowers/specs/2026-10-22-servicehub-api-design.md). Decisões de arquitetura: [docs/adr/](docs/adr/).

## Limitações conhecidas (nesta fase)

- Sem refresh token: o JWT vale 24 h e é revogado só pela desativação da conta.
- Ordens de serviço e avaliações existem só no banco, sem endpoints (Fase 3).
