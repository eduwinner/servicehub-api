# ADR-0005 — Testcontainers (PostgreSQL real) em vez de H2 nos testes

**Status:** aceito — 2026-10-22

## Contexto

A Aula 10 apresenta duas opções de banco para testes: H2 em memória ("zero configuração") e Testcontainers ("mesmo banco da produção"), e o slide 16 usa `@AutoConfigureTestDatabase`, que troca o datasource por um banco embutido.

As migrações Flyway do ServiceHub (Aula 4) usam recursos específicos do PostgreSQL: `BIGSERIAL`, `TEXT`, `NUMERIC(10,2)`, `CHECK (status IN (...))` e `ALTER TABLE ... DROP CONSTRAINT`. Com H2, ou o Flyway falha, ou é preciso desligá-lo e usar `ddl-auto=create-drop`, o que testa um schema diferente do que roda em produção e anula o que a Aula 4 ensinou (`ddl-auto=validate` + migrações versionadas).

Além disso, o único teste do projeto (`contextLoads`) exigia um Postgres já rodando em `postgres:5432`, então falhava em qualquer máquina sem o Compose de pé.

## Decisão

Todos os testes que precisam de banco usam **PostgreSQL 16 real** via Testcontainers 2.x, ligado ao Spring com `@ServiceConnection` (classe `TestcontainersConfiguration`, importada por `@Import`). O Flyway aplica as mesmas migrações de produção a cada execução. H2 não é dependência do projeto.

## Consequências

- **Docker Desktop precisa estar rodando** para `mvnw test` / `mvnw verify`. Já é pré-requisito da disciplina (Aulas 11–12).
- Paridade com produção: um erro de SQL ou de migração aparece no teste, não no deploy.
- Cada JVM de teste paga alguns segundos para subir o container (o Spring reaproveita o contexto entre classes com a mesma configuração).
- Nos runners do GitHub Actions (`ubuntu-latest`) o Docker já está disponível; nenhuma configuração extra.
- Para a próxima turma: a Aula 10 pode manter H2 como contraexemplo, mostrando o que quebra com as migrações do ServiceHub.
