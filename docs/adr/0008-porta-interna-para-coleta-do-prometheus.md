# ADR-0008 — Porta interna para a coleta do Prometheus

**Status:** aceito — 2026-11-11

## Contexto

`/actuator/prometheus` exige token de ADMIN, e o contrato dos alunos (Mapa) mantém isso na porta pública. Um servidor Prometheus coleta a cada poucos segundos e não sabe pedir nem renovar um JWT. As alternativas eram:

- liberar `/actuator/prometheus` na porta pública, o que expõe métricas internas a qualquer um;
- dar ao Prometheus um token fixo de longa duração, o que contradiz a expiração do JWT;
- mover todo o Actuator para `management.server.port`, o que tiraria `/actuator/health` da porta da API e quebraria o contrato, o healthcheck e a bancada.

## Decisão

A API abre um conector Tomcat adicional em `servicehub.metrics.internal-port` (variável `METRICS_INTERNAL_PORT`, desligado com 0, o padrão). Ele é criado por `config/MetricsPortConfig`. No `SecurityConfig`, uma regra libera só `GET /actuator/prometheus` e só quando a requisição chega por essa porta. O restante do Actuator e da API segue as regras de sempre, inclusive nessa porta.

O Compose liga a porta em 8081 e **não a publica** (`expose`, sem `ports`). Assim, só os containers da rede `servicehub-net`, como o Prometheus do perfil `observability`, a alcançam.

## Consequências

- A porta pública não muda: health continua público e metrics e prometheus continuam só para ADMIN.
- O Prometheus coleta `api:8081` sem credencial e o Grafana lê do Prometheus.
- A segurança depende de a 8081 nunca ser publicada. Publicá-la com `ports:` equivale a abrir as métricas.
- `MetricsInternalPortIntegrationTest` prova três coisas: a porta interna entrega as métricas sem token; a pública continua 401; a interna não abre outros endpoints.
