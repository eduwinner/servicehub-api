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

Swagger UI em `http://localhost:8080/swagger-ui.html` (passa para `/docs` na Fase 1).

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
- Erros de negócio respondem 500 genérico; não há validação de entrada nem paginação (Fase 1).
- Ordens de serviço e avaliações existem só no banco, sem endpoints (Fase 3).
