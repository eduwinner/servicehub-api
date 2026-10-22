# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ServiceHub API — Spring Boot 4.1.1 / Java 25 LTS REST backend, built with Maven. Not currently a git repository. No README exists.

## Commands

Windows shell — use `mvnw.cmd` (a plain `mvnw` script also exists for POSIX shells).

```
mvnw.cmd clean install        # build
mvnw.cmd spring-boot:run      # run locally (needs Postgres reachable, see below)
mvnw.cmd test                 # run all tests
mvnw.cmd test -Dtest=ServicehubApiApplicationTests#contextLoads   # single test
mvnw.cmd clean package        # produce target/*.jar (required before docker build)
```

Docker (Postgres + pgAdmin + API):

```
docker-compose up -d
```

- API container: host `8090` → container `8080`
- Postgres: host `5433` → container `5432` (user/pass/db: `servicehub`/`secret`/`servicehub`)
- pgAdmin: host `8091` (admin@admin.com / admin)

The Dockerfile just copies a pre-built `target/*.jar` — run `mvnw.cmd clean package` before `docker-compose up --build`.

## Architecture

Standard layered Spring MVC structure under `src/main/java/com/servicehub/`:

- `controller/` — `@RestController`s, thin, delegate to services
- `service/` — `@Service` + `@Transactional` business logic
- `repository/` — `JpaRepository` interfaces (Spring Data derived queries)
- `model/` — JPA `@Entity` classes; `model/dto/` — request/response DTOs (Lombok `@Data`)

Note the base package is `com.servicehub`, but the test class lives in `com.servicehub.servicehub_api` (see `HELP.md`) — an artifact of the original Maven artifactId being an invalid package name. Don't "fix" this without checking whether it's intentional.

### Database & migrations

- Postgres, managed exclusively through **Flyway** (`src/main/resources/db/migration/V{n}__description.sql`). `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never creates/alters schema, so any entity change must ship a matching Flyway migration.
- Only `users` (V1) and `services` (V2) tables exist so far. `services` is backed by the `ServiceOffering` entity (named to avoid clashing with `org.springframework.stereotype.Service`) — full CRUD under `/api/services`, mirroring the `User`/`UserController` stack. `ServiceOffering.category` is a Java enum (`ServiceCategory`, `EnumType.STRING`); the DB column is a plain `VARCHAR(50)` with no `CHECK` constraint, so nothing at the DB layer enforces valid values — only the app does.
- Connection settings in `application.properties` are the local/docker defaults (`jdbc:postgresql://postgres:5432/servicehub`); `docker-compose.yml` overrides them via env vars for the `api` service (still pointing at the `postgres` service).

### Spring Boot 4 dependency gotcha

This project is on Spring Boot 4.1.1, which split autoconfiguration into per-feature modules (e.g. `spring-boot-jdbc`, `spring-boot-hibernate`, `spring-boot-flyway` instead of one monolithic `spring-boot-autoconfigure`). Adding a raw integration library (e.g. `flyway-core`) is **not** enough to get autoconfiguration — you need the matching `org.springframework.boot:spring-boot-starter-<feature>` (e.g. `spring-boot-starter-flyway`), which pulls in the Boot-managed autoconfig module plus the underlying library. If a feature silently doesn't activate (no relevant log lines, no errors), check for a missing `spring-boot-starter-*` first.

### Current known gaps (not yet implemented)

- No authentication/security layer at all.
- Passwords are stored as-is into `password_hash` (`UserDTO.password` → `User.passwordHash`) with a `// depois trocar por hash` comment — no hashing is implemented.
- Controllers return entities directly (no response DTOs), so `User.passwordHash` leaks in every JSON response that includes a user — directly from `/api/users`, and indirectly via `ServiceOffering.provider` on `/api/services`. Lazily-loaded `provider` also serializes Hibernate's `hibernateLazyInitializer`/`handler` proxy internals into the JSON body.
- Errors are surfaced via bare `RuntimeException` (e.g. duplicate email, user not found) — no `@ControllerAdvice`/global exception handling, so these currently return generic 500s instead of meaningful HTTP status codes.
- Only one test exists (`ServicehubApiApplicationTests.contextLoads`), which requires a live Postgres connection to pass (`@SpringBootTest` + Flyway on startup).
