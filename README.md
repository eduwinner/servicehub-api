# ServiceHub API

API REST de gestão de prestadores de serviço, serviços, ordens de serviço e avaliações. É a **implementação de referência** do projeto PBL da disciplina *Desenvolvimento de API Back-End* (Engenharia de Software, 6º período, UniEVANGÉLICA).

O projeto evolui em fases, uma por bloco de aulas, e cada fase tem uma tag git. Assim dá para comparar, em sala, o que cada etapa acrescentou: `git diff fase-0-baseline fase-1-contrato-qualidade`.

## Pré-requisitos

- JDK 21
- Docker Desktop (rodando)
- Git

## Como rodar localmente

```bash
docker compose up -d postgres        # Postgres em localhost:5433
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

## Como testar

```bash
./mvnw.cmd verify
```

Os testes sobem um PostgreSQL real em container (Testcontainers), então **o Docker precisa estar rodando**. Não é preciso subir o Compose antes.

## Documentação

- Swagger UI: `http://localhost:8080/docs`
- Especificação OpenAPI (grupo v1): `http://localhost:8080/api-docs/v1`
- Todas as rotas da API começam com `/api/v1`. Listagens são paginadas (`?page=0&size=20&sort=id`, máximo 100 por página).

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

## Material dos alunos

- [Mapa de Construção (PDF)](docs/alunos/Mapa.pdf): schema, endpoints e regras de negócio que a API deve atingir.
- [Schema-alvo PostgreSQL](docs/alunos/servicehub_schema.sql).

## Fases e tags

| Fase | Tag | Aulas | Conteúdo |
|---|---|---|---|
| 0 | `fase-0-baseline` | 3–5 | Projeto base, Java 21, testes com Testcontainers |
| 1 | `fase-1-contrato-qualidade` | 2, 5, 6, 8 | `/api/v1`, erros `ProblemDetail`, validação, paginação, OpenAPI |
| 2 | `fase-2-seguranca` | 8, 9 | Spring Security, BCrypt, JWT, papéis, BOLA |
| 3 | `fase-3-dominio-ordens` | 4, 9 | Ordens de serviço, máquina de estados, avaliações |
| 4 | `fase-4-testes` | 10 | Pirâmide de testes completa, cobertura, Postman |
| 5 | `fase-5-docker` | 11, 12 | Dockerfile multi-stage, Compose com healthcheck e perfis |
| 6 | `fase-6-ci-observabilidade` | 14, 16 | GitHub Actions, Actuator, Prometheus, logs estruturados |

Design completo: [docs/superpowers/specs/2026-10-22-servicehub-api-design.md](docs/superpowers/specs/2026-10-22-servicehub-api-design.md). Decisões de arquitetura: [docs/adr/](docs/adr/).

## Limitações conhecidas (nesta fase)

- Sem autenticação; senhas gravadas em texto puro (Fase 2).
- O prestador de um serviço ainda vem no corpo (`providerId`); passa a vir do token na Fase 2.
- Exclusão ainda é física (`DELETE`); a exclusão lógica entra na Fase 2.
- Ordens de serviço e avaliações existem só no banco, sem endpoints (Fase 3).
