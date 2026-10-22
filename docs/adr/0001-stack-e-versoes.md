# ADR-0001 — Stack e versões da API de referência

**Status:** aceito — 2026-10-22

## Contexto

Os slides da disciplina citam versões diferentes em aulas diferentes: Spring Boot 3.2.x e Java 17/21 na Aula 3, starters exclusivos do Spring Boot 4 na Aula 4 (`spring-boot-starter-webmvc`, `-data-jpa-test`, `-webmvc-test`), `springdoc 2.5.0` na Aula 5, imagens JDK 17 na Aula 11. O projeto base foi gerado com Spring Boot 4.1.1 e `java.version=25`, mas o `JAVA_HOME` do professor é o JDK 21 e o build falhava com `release version 25 not supported`.

## Decisão

| Item | Versão | Motivo |
|---|---|---|
| Java | 21 LTS | Slides dizem "17 ou 21"; é o LTS que os alunos têm instalado |
| Spring Boot | 4.1.1 | Já em uso; autoconfiguração dividida em starters por recurso (ver `CLAUDE.md`) |
| Springdoc OpenAPI | 3.0.3 | Única linha compatível com Boot 4 (a 2.x da Aula 5 não funciona) |
| PostgreSQL | 16 (alpine) | Mesma família da Aula 4 (15), imagem menor |
| Flyway | gerenciado pelo Boot (12.x) | Migrações versionadas, `ddl-auto=validate` |
| Testcontainers | gerenciado pelo Boot (2.0.x) | ADR-0005 |
| Lombok | gerenciado pelo Boot | Mantido nas entidades existentes (os slides usam) |
| DTOs novos | `record` Java | Imutáveis e sem Lombok; mostra a opção moderna |

## Consequências

- Os slides das Aulas 3, 5, 10 e 11 precisam de atualização de versão para a próxima turma (lista em `docs/ensino/`).
- Alunos com JDK 17 precisam instalar o 21.
- Toda nova capacidade do Spring exige o `spring-boot-starter-<recurso>` correspondente, não só a biblioteca.
