# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ServiceHub API — Spring Boot 4.1.1 / Java 21 LTS REST backend, built with Maven. Reference implementation for the PBL course "Desenvolvimento de API Back-End" (UniEVANGÉLICA). Git remote: `https://github.com/eduwinner/servicehub-api.git`, branch `main`, one annotated tag per phase (`fase-0-baseline`, `fase-1-contrato-qualidade`, …).

Key documents — read before changing behavior:

- Design (binding): `docs/superpowers/specs/2026-10-22-servicehub-api-design.md`
- Current implementation plan: `docs/superpowers/plans/`
- Architecture decisions: `docs/adr/NNNN-*.md` (Nygard format). Add one for every non-obvious choice.
- Slide text extracts (git-ignored, local only): `docs/ensino/slides-extrato/`

Commit messages: Portuguese, Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).

## Commands

Windows shell — use `mvnw.cmd` (a plain `mvnw` script also exists for POSIX shells).

```
mvnw.cmd clean install        # build + tests
mvnw.cmd verify               # full test suite (needs Docker running)
mvnw.cmd spring-boot:run      # run locally (needs Postgres on localhost:5433, see below)
mvnw.cmd test -Dtest=ServicehubApiApplicationTests#contextLoads   # single test
mvnw.cmd clean package        # produce target/*.jar (required before docker build)
```

Docker (Postgres + pgAdmin + API):

```
docker compose up -d
```

- API container: host `8090` → container `8080`
- Postgres: host `5433` → container `5432` (user/pass/db: `servicehub`/`secret`/`servicehub`)
- pgAdmin: host `8091` (admin@admin.com / admin)

The Dockerfile just copies a pre-built `target/*.jar` — run `mvnw.cmd clean package` before `docker compose up --build` (multi-stage build comes in Phase 5).

## Tests

- Integration tests extend `com.servicehub.AbstractIntegrationTest` (`RANDOM_PORT`, `TestRestTemplate`, `TRUNCATE ... RESTART IDENTITY` before each test). Boot 4 moved `TestRestTemplate` to `spring-boot-resttestclient` (needs `@AutoConfigureTestRestTemplate`). Web slice tests: `@WebMvcTest` from `org.springframework.boot.webmvc.test.autoconfigure` + `@MockitoBean`.
- Tests that need a database use **Testcontainers** (`postgres:16-alpine`) via `com.servicehub.TestcontainersConfiguration` (`@ServiceConnection`), imported with `@Import`. **Docker must be running**; no Compose needed. H2 is deliberately not used (ADR-0005): migrations rely on Postgres-only SQL.
- The context test lives in `com.servicehub.servicehub_api` (see `HELP.md`) — an artifact of the original Maven artifactId being an invalid package name. Don't "fix" this without checking whether it's intentional.

## API conventions (Phase 1)

- All routes under `/api/v1`. `HelloController` was replaced by `InfoController` (`GET /api/v1/info`).
- Errors: `exception/GlobalExceptionHandler` (extends `ResponseEntityExceptionHandler`) returns `ProblemDetail`; `type` URNs live in `exception/ProblemTypes`. Throw `ResourceNotFoundException` (404), `EmailAlreadyExistsException` / `BusinessRuleException` (409); never bare `RuntimeException`.
- Validation: Bean Validation on request DTOs + `@Valid`; 400 carries `errors: [{field, message}]`. Enum parse errors list accepted values (Jackson 3: `tools.jackson.databind.exc.InvalidFormatException`).
- Pagination: controllers take `@ParameterObject @PageableDefault Pageable`; JSON is `{content, page{size,number,totalElements,totalPages}}` via `spring.data.web.pageable.serialization-mode=via-dto`. Do **not** add `@EnableSpringDataWebSupport` — it disables Boot's pageable autoconfig and silently drops `max-page-size=100`.
- `?sort=` is allowlisted per endpoint via `controller/SortGuard` (`SORTABLE` set in each controller); never accept arbitrary JPA paths (e.g. `passwordHash`, `provider.*`).
- Framework errors (type mismatch, 404 route, 405, 415) are normalized in `GlobalExceptionHandler.handleExceptionInternal`/`handleTypeMismatch` to the same ProblemDetail contract in Portuguese.
- `ServiceOfferingRepository.findAll(Specification, Pageable)` has `@EntityGraph("provider")` to avoid N+1 in the listing; keep response mapping inside the read-only transaction.
- `config/RequestIdFilter` sets/echoes `X-Request-Id` and MDC `requestId`.
- OpenAPI: Swagger UI at `/docs`, spec at `/api-docs/v1`; every endpoint documents each status code with `ProblemDetail` schema for errors.
- `POST` returns `ResponseEntity.created(Location)`.

## Security (Phase 2)

- Spring Security 7 + `oauth2-resource-server`, JWT HS256 (`security/SecurityConfig`, `TokenService`). `JWT_SECRET` (>= 32 bytes) is mandatory: `JwtProperties` fails fast. Tests set it in `AbstractIntegrationTest`.
- `JwtUserConverter` (registered as `@Bean`, NOT `@Component`: `@WebMvcTest` loads every `Converter`) loads the user by claim `uid` on every request; inactive means 401. Authorities come from the DB role, so role changes apply immediately.
- Services get the caller via `security/CurrentUser`; role rules use `@PreAuthorize` on service methods; owner rules (BOLA) are explicit `AccessDeniedException`s in the service. 401/403 are ProblemDetail both in filters (`ProblemResponseWriter`) and in `GlobalExceptionHandler`.
- Registration is `POST /api/v1/auth/register` (role CLIENT|PROVIDER only); there is no `POST /users`. Admin bootstrap: `AdminInitializer` from `SERVICEHUB_ADMIN_*`.
- Soft delete (ADR-0002): users and services set `active=false`; deactivating a user deactivates their services.
- Free text (`description`, `bio`) goes through `service/TextSanitizer` (HTML escape).
- OpenAPI: global `bearerAuth`; public operations declare `@SecurityRequirements` (empty); `OpenApiConfig.securityResponsesCustomizer` adds 401/403 to every protected operation. Groups `v1` and `public`.
- Web slice tests use `@AutoConfigureMockMvc(addFilters = false)`; security behaviour is covered by integration tests (`*SecurityIntegrationTest`, `AuthApiIntegrationTest`) using helpers in `AbstractIntegrationTest` (`registrar`, `login`, `adminToken`, `get/post/put/patch/delete`).

## Domain (Phase 3)

- `model/RequestStatus` holds the order state machine as data (`canTransitionTo`, `allowedFor(target, Actor)`, `ACTIVE`). Invalid transition: `InvalidStateTransitionException` (409 `invalid-transition`); valid but not allowed for the caller: `AccessDeniedException` (403). See ADR-0006.
- `ServiceRequestService`: create (RN06/RN07: price snapshot, no own/inactive service, ADMIN cannot order), list by participation (`role=client|provider`, `status`), changeStatus; increments counter `servicehub.service_requests.transitions{to}`.
- `ReviewService`: only the order client, only COMPLETED, one per order (exists check + UNIQUE fallback). `Review.rating` is `Short` because the column is SMALLINT (`ddl-auto=validate`).
- RN10: deactivating a service or user with PENDING/ACCEPTED/IN_PROGRESS orders → 409.
- Repositories use `@EntityGraph` on `findAll(Specification, Pageable)`/`findById` to avoid N+1; tests assert statement counts via Hibernate statistics.
- V9 aligned the DB with `docs/alunos/servicehub_schema.sql` (NOT NULLs, CHECKs, RESTRICT FKs, indexes).

## Tests & coverage (Phase 4)

- `mvnw verify` generates JaCoCo at `target/site/jacoco/index.html` (~96% lines). Surefire loads Mockito as `-javaagent` via `maven-dependency-plugin:properties`; keep `@{argLine}` so JaCoCo still works.
- `ProtectedRoutesIntegrationTest` sweeps every protected route for 401; add new protected routes to its CSV.
- `ServiceOfferingSpecifications.titleContains` escapes `%`, `_` with `!` as LIKE escape char.
- SQL is logged through `org.hibernate.SQL` (DEBUG), not `show-sql`, so lines carry the MDC `requestId`.
- Postman collection: `docs/postman/servicehub.postman_collection.json` (full cycle, default baseUrl 8090).

## Architecture

Standard layered Spring MVC structure under `src/main/java/com/servicehub/`:

- `controller/` — `@RestController`s, thin, delegate to services
- `service/` — `@Service` + `@Transactional` business logic; maps entities to response DTOs
- `repository/` — `JpaRepository` interfaces (Spring Data derived queries)
- `model/` — JPA `@Entity` classes; `model/dto/` — request/response DTOs (Lombok `@Data`; new DTOs use Java `record`)

### Database & migrations

- Postgres, managed exclusively through **Flyway** (`src/main/resources/db/migration/V{n}__description.sql`). `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never creates/alters schema, so any entity change must ship a matching Flyway migration. **Never edit or rename V1–V7**; add new versions only.
- Tables: `users` (V1, extended in V3), `services` (V2, FK `ON DELETE CASCADE` since V4), `service_requests` (V5, `CHECK` on status), `reviews` (V7). All four tables have entities and endpoints. `services` is backed by the `ServiceOffering` entity (named to avoid clashing with `org.springframework.stereotype.Service`). `ServiceOffering.category` is a Java enum (`ServiceCategory`, `EnumType.STRING`); the DB column is a plain `VARCHAR(50)` with no `CHECK` constraint, so only the app enforces valid values.
- Config comes from env vars with dev defaults: `DB_URL` (default `jdbc:postgresql://localhost:5433/servicehub`, the Compose-published port), `DB_USER`, `DB_PASS`, `SERVER_PORT`. The Compose `api` service sets `DB_URL` to `postgres:5432`. `spring.jpa.open-in-view=false`: build response DTOs inside `@Transactional` service methods.
- Target schema for the end state (handed to students): `docs/alunos/servicehub_schema.sql`; Phase 3 adds a V9 migration to match it (see design 5.2).

### Spring Boot 4 dependency gotcha

This project is on Spring Boot 4.1.1, which split autoconfiguration into per-feature modules (e.g. `spring-boot-jdbc`, `spring-boot-hibernate`, `spring-boot-flyway` instead of one monolithic `spring-boot-autoconfigure`). Adding a raw integration library (e.g. `flyway-core`) is **not** enough to get autoconfiguration — you need the matching `org.springframework.boot:spring-boot-starter-<feature>` (e.g. `spring-boot-starter-flyway`), which pulls in the Boot-managed autoconfig module plus the underlying library. If a feature silently doesn't activate (no relevant log lines, no errors), check for a missing `spring-boot-starter-*` first. Also: `@MockBean` is gone — use `@MockitoBean`; Testcontainers 2.x module artifacts are `testcontainers-postgresql` / `testcontainers-junit-jupiter`, class `org.testcontainers.postgresql.PostgreSQLContainer`.

### Current known gaps (not yet implemented)

- `.env.example` could not be created by the agent (permission deny rule on `.env*`); the variable list is in README.
