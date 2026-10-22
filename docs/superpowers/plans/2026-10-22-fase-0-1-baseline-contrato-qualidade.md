# ServiceHub API — Fases 0 e 1 (Baseline + Contrato e Qualidade) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformar o projeto base (CRUD de usuários e serviços sem testes, sem validação e com erros 500) em uma API v1 com contrato REST consistente, erros `ProblemDetail`, validação, paginação, documentação OpenAPI fiel e suíte de testes rodando contra Postgres real, commitada e tagueada em duas fases.

**Architecture:** Spring MVC em camadas (`controller → service → repository`), DTOs separados de entidades, exceções de domínio traduzidas por um `@RestControllerAdvice` para RFC 9457. Testes em três níveis: unitário (Mockito), fatia web (`@WebMvcTest`) e integração (`@SpringBootTest` + Testcontainers Postgres via `@ServiceConnection`). Sem segurança ainda (Fase 2).

**Tech Stack:** Java 21, Spring Boot 4.1.1 (Spring Framework 7, Spring Data JPA, Bean Validation), PostgreSQL 16 + Flyway, Springdoc 3.0.3, JUnit 5 + Mockito + AssertJ, Testcontainers 2.0.5, Maven Wrapper, Git.

**Spec:** `docs/superpowers/specs/2026-10-22-servicehub-api-design.md` (seções 3, 4, 5.1–5.3, 5.5–5.9, 5.13, 6 e Apêndice A).

## Global Constraints

- Java **21** (`<java.version>21</java.version>`); Spring Boot **4.1.1** mantido; Springdoc **3.0.3** mantido.
- Migrações **V1–V7 nunca são editadas ou renomeadas**; só migrações novas.
- Testes de integração rodam contra **PostgreSQL real** (Testcontainers, imagem `postgres:16-alpine`); H2 é proibido.
- Prefixo de todas as rotas da API: **`/api/v1`**.
- Nenhuma entidade JPA em resposta HTTP; `passwordHash` nunca sai em JSON.
- Mensagens de erro e de validação em **português**; nomes de classes, campos e rotas em inglês.
- Listagens sempre paginadas; `size` máximo **100**, padrão **20**.
- Erros sempre em `ProblemDetail` (`application/problem+json`); `server.error.include-stacktrace=never`, `server.error.include-message=never`.
- Configuração lê variáveis de ambiente com default de desenvolvimento: `DB_URL` (default `jdbc:postgresql://localhost:5433/servicehub`), `DB_USER` (`servicehub`), `DB_PASS` (`secret`), `SERVER_PORT` (`8080`).
- Nenhum `System.out`; logs via SLF4J.
- Commits em português no estilo Conventional Commits, sempre com trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` (passar como segundo `-m`).
- Windows: usar `mvnw.cmd`; comandos abaixo funcionam no Git Bash. Docker Desktop precisa estar rodando para os testes de integração.

## Review Focus

1. `GET /api/v1/services?size=500` ou `page=-1`: deve devolver no máximo 100 itens e nunca 500. Teste na Task 1.4.
2. `POST /api/v1/services` com `"price": "abc"` ou `"category": "INVALIDA"`: deve devolver 400 `ProblemDetail` listando as categorias válidas, nunca 500. Teste na Task 1.3.
3. `POST /api/v1/users` com campo desconhecido (ex.: `"role": "ADMIN"`): deve ser ignorado sem erro e sem efeito (base para a defesa contra mass assignment da Fase 2). Teste na Task 1.3.
4. `X-Request-Id` enviado pelo cliente com mais de 64 caracteres ou com caracteres fora de `[A-Za-z0-9._-]`: deve ser descartado e substituído por um UUID, nunca ecoado. Teste na Task 1.1.
5. E-mail com maiúsculas (`Maria@X.com`) e e-mail existente em minúsculas: deve ser tratado como duplicado (normalização para minúsculas + trim antes de persistir e de comparar). Teste na Task 1.2.

---

## Estrutura de arquivos ao final da Fase 1

```text
src/main/java/com/servicehub/
├── ServicehubApiApplication.java
├── config/
│   ├── OpenApiConfig.java            (reescrito: Info, servers, grupo v1)
│   ├── WebConfig.java                (novo: @EnableSpringDataWebSupport VIA_DTO)
│   └── RequestIdFilter.java          (novo)
├── controller/
│   ├── InfoController.java           (substitui HelloController)
│   ├── UserController.java           (/api/v1/users, ResponseEntity, @Valid, Pageable)
│   └── ServiceOfferingController.java(/api/v1/services, filtros, Pageable)
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── EmailAlreadyExistsException.java
│   ├── BusinessRuleException.java
│   ├── ProblemTypes.java
│   └── GlobalExceptionHandler.java
├── model/  (User ganha phone/bio/avatarUrl/updatedAt; StatusResponse removido)
│   └── dto/
│       ├── InfoResponse.java         (record, novo)
│       ├── UserDTO.java              (validação)
│       ├── UserUpdateDTO.java        (record, novo)
│       ├── UserResponseDTO.java      (campos novos)
│       ├── ServiceOfferingDTO.java   (validação)
│       ├── ServiceOfferingResponseDTO.java
│       └── ServiceFilter.java        (record, novo)
├── repository/ (ServiceOfferingRepository + JpaSpecificationExecutor;
│               ServiceOfferingSpecifications novo)
└── service/   (UserService, ServiceOfferingService: exceções de domínio, Page)

src/test/java/com/servicehub/
├── servicehub_api/ServicehubApiApplicationTests.java (usa Testcontainers)
├── TestcontainersConfiguration.java
├── AbstractIntegrationTest.java
├── config/RequestIdFilterTest.java
├── controller/InfoControllerTest.java
├── controller/UserControllerTest.java
├── controller/ServiceOfferingControllerTest.java
├── service/UserServiceTest.java
├── service/ServiceOfferingServiceTest.java
├── integration/UserApiIntegrationTest.java
├── integration/ServiceOfferingApiIntegrationTest.java
└── integration/OpenApiIntegrationTest.java

docs/adr/0001-stack-e-versoes.md, 0004-problem-detail.md, 0005-testcontainers.md
README.md, .env.example, CLAUDE.md (atualizado)
```

---

# FASE 0 — Baseline (tag `fase-0-baseline`)

> Desvio em relação ao design: a base de Testcontainers (prevista na Fase 1) entra já na Fase 0 (Task 0.3), porque o design exige `mvnw verify` verde ao fim de **cada** fase e o `contextLoads` atual exige Postgres externo. Fixtures de usuário em todos os testes usam a senha `"Senha123"` (válida para a regex da Task 1.3).

### Task 0.1: Commit da baseline e branch `main`

**Files:**
- Modify: `.gitignore` (adicionar `.github/modernize/`; `.env` e `docs/ensino/slides-extrato/` já estão)

**Interfaces:**
- Produces: branch `main` com dois commits (baseline do código; documentos de design), remoto `origin` já configurado para `https://github.com/eduwinner/servicehub-api.git`.

- [ ] **Step 1: Renomear o branch**

Run: `git branch -m master main && git branch --show-current`
Expected: `main`

- [ ] **Step 2: Ignorar resíduo de ferramenta**

Adicionar ao final de `.gitignore` a linha `.github/modernize/`.

- [ ] **Step 3: Conferir o que será commitado**

Run: `git add -A && git status --short`
Expected: aparecem `pom.xml`, `src/`, `Dockerfile`, `docker-compose.yml`, `CLAUDE.md`, `mvnw*`, `.mvn/`, `.gitattributes`, `.gitignore`, `.vscode/`, `docs/`; **não** aparecem `target/`, `.env`, `.github/modernize/`, `docs/ensino/slides-extrato/`, `HELP.md`.

- [ ] **Step 4: Commit da baseline sem os documentos**

```bash
git reset -q docs && git commit -m "chore: baseline do ServiceHub API (Aulas 3-5: CRUD, Flyway, Springdoc)" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

- [ ] **Step 5: Commit dos documentos de design**

```bash
git add docs && git commit -m "docs: design da API de referencia e sugestoes para a proxima turma" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

- [ ] **Step 6: Verificar**

Run: `git log --oneline`
Expected: dois commits, o de baseline primeiro.

### Task 0.2: Java 21 e limpeza de arquivos soltos

**Files:**
- Modify: `pom.xml` (`<java.version>25</java.version>` → `21`; preencher `<name>servicehub-api</name>` e `<description>API de gestão de serviços — projeto PBL</description>`)
- Delete: `src/main/resources/V6__Create_reviews_table.sql`
- Modify: `Dockerfile` (`FROM eclipse-temurin:25-jdk` → `FROM eclipse-temurin:21-jdk`; o multi-stage vem na Fase 5)

**Interfaces:**
- Produces: `mvnw.cmd -q -DskipTests compile` passa sem `-Djava.version`.

- [ ] **Step 1: Verificar que hoje falha**

Run: `./mvnw.cmd -q -B -DskipTests compile`
Expected: `error: release version 25 not supported` (JAVA_HOME é o JDK 21).

- [ ] **Step 2: Aplicar as três alterações de arquivo**

- [ ] **Step 3: Verificar que compila**

Run: `./mvnw.cmd -q -B -DskipTests compile; echo exit=$?`
Expected: `exit=0`, sem saída de erro.

- [ ] **Step 4: Commit**

```bash
git add pom.xml Dockerfile src/main/resources && git commit -m "chore: fixar Java 21 e remover migracao solta fora de db/migration" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 0.3: Testcontainers para que `contextLoads` não dependa de Postgres externo

**Files:**
- Modify: `pom.xml` (dependências de teste)
- Create: `src/test/java/com/servicehub/TestcontainersConfiguration.java`
- Modify: `src/test/java/com/servicehub/servicehub_api/ServicehubApiApplicationTests.java`
- Create: `docs/adr/0005-testcontainers-em-vez-de-h2.md`

**Interfaces:**
- Produces: `TestcontainersConfiguration` (`@TestConfiguration(proxyBeanMethods = false)`) com bean `@Bean @ServiceConnection PostgreSQLContainer postgresContainer()` usando a imagem `postgres:16-alpine`. Todo teste `@SpringBootTest` posterior faz `@Import(TestcontainersConfiguration.class)`.

- [ ] **Step 1: Adicionar dependências de teste ao `pom.xml`**

Versões gerenciadas pelo BOM 4.1.1 (confirmado: `spring-boot-testcontainers`, Testcontainers 2.0.5 com módulos `testcontainers-postgresql` e `testcontainers-junit-jupiter`):

```xml
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-testcontainers</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-postgresql</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><scope>test</scope></dependency>
```

- [ ] **Step 2: Verificar que o teste atual falha sem banco**

Run: `docker compose stop postgres 2>/dev/null; ./mvnw.cmd -q -B test 2>&1 | tail -5`
Expected: `contextLoads` falha com erro de conexão (`Connection to postgres:5432 refused` ou `UnknownHost postgres`).

- [ ] **Step 3: Criar `TestcontainersConfiguration`**

Classe de teste anotada `@TestConfiguration(proxyBeanMethods = false)`; método `@Bean @ServiceConnection PostgreSQLContainer postgresContainer()` devolvendo `new PostgreSQLContainer("postgres:16-alpine")`. No Testcontainers 2.x a classe está em `org.testcontainers.postgresql.PostgreSQLContainer`; se o import falhar, inspecionar o jar baixado em `~/.m2/repository/org/testcontainers/testcontainers-postgresql/2.0.5/` e usar o pacote encontrado.

- [ ] **Step 4: Importar no teste de contexto**

`ServicehubApiApplicationTests` recebe `@Import(TestcontainersConfiguration.class)`. Manter o pacote `com.servicehub.servicehub_api` (ver `CLAUDE.md`).

- [ ] **Step 5: Verificar que passa**

Run: `./mvnw.cmd -q -B test 2>&1 | tail -5; echo exit=${PIPESTATUS[0]}`
Expected: `exit=0`; no log, Flyway aplica `Successfully applied 7 migrations`.

- [ ] **Step 6: Escrever ADR-0005**

`docs/adr/0005-testcontainers-em-vez-de-h2.md`, formato Nygard (Título, Status: aceito, Contexto, Decisão, Consequências). Contexto: migrações usam `BIGSERIAL`, `NUMERIC`, `TEXT`, `CHECK` do Postgres; a Aula 10 sugere H2. Decisão: Postgres real via Testcontainers + `@ServiceConnection`. Consequências: Docker obrigatório para `mvnw test`; paridade com produção; ~5 s a mais por JVM de teste.

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/test docs/adr && git commit -m "test: Testcontainers Postgres para os testes de contexto (ADR-0005)" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 0.4: README, `.env.example`, ADR-0001, `CLAUDE.md`, tag e push

**Files:**
- Create: `README.md`, `.env.example`, `docs/adr/0001-stack-e-versoes.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: `.env.example`** com exatamente estas chaves e comentários em português:

```dotenv
# Banco de dados (Compose publica o Postgres em localhost:5433)
DB_NAME=servicehub
DB_USER=servicehub
DB_PASS=secret
DB_URL=jdbc:postgresql://localhost:5433/servicehub
# Porta HTTP da API quando executada fora do Docker
SERVER_PORT=8080
```

(As chaves de JWT, CORS e admin entram na Fase 2; as do Compose na Fase 5.)

- [ ] **Step 2: `README.md`** com seções: O que é; Pré-requisitos (JDK 21, Docker Desktop, Git); Como rodar localmente (`docker compose up -d postgres` + `mvnw.cmd spring-boot:run`); Como testar (`mvnw.cmd verify`, exige Docker); Documentação (Swagger em `http://localhost:8080/swagger-ui.html`, muda para `/docs` na Fase 1); Mapa de fases e tags (tabela da seção 6 do design, com link para o design); Limitações conhecidas (sem segurança, erros 500, sem validação, sem paginação: tudo resolvido nas fases seguintes).

- [ ] **Step 3: ADR-0001** (`docs/adr/0001-stack-e-versoes.md`): Java 21 LTS (slides: 17 ou 21; JAVA_HOME do professor é 21), Spring Boot 4.1.1 (starters por recurso), Springdoc 3.0.3, Postgres 16, Flyway, Lombok nas entidades existentes e `record` nos DTOs novos.

- [ ] **Step 4: Atualizar `CLAUDE.md`**: Java 21; seção "Testes" dizendo que `mvnw.cmd test` usa Testcontainers e exige Docker; citar `docs/adr/`, o design e o plano; remover a frase "Not currently a git repository".

- [ ] **Step 5: Verificar build completo**

Run: `./mvnw.cmd -q -B verify; echo exit=$?`
Expected: `exit=0`.

- [ ] **Step 6: Commit, tag e push**

```bash
git add README.md .env.example docs/adr CLAUDE.md && git commit -m "docs: README, .env.example, ADR-0001 e CLAUDE.md da fase 0" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
git tag -a fase-0-baseline -m "Fase 0: baseline (Aulas 3-5) com Java 21 e Testcontainers"
git push -u origin main && git push origin fase-0-baseline
```

Expected: `git ls-remote --tags origin` lista `fase-0-baseline`.

---

# FASE 1 — Contrato e Qualidade (tag `fase-1-contrato-qualidade`)

### Task 1.1: Prefixo `/api/v1`, `InfoController` e `RequestIdFilter`

**Files:**
- Delete: `src/main/java/com/servicehub/controller/HelloController.java`, `src/main/java/com/servicehub/model/StatusResponse.java`
- Create: `src/main/java/com/servicehub/model/dto/InfoResponse.java`, `src/main/java/com/servicehub/controller/InfoController.java`, `src/main/java/com/servicehub/config/RequestIdFilter.java`
- Modify: `UserController` (`@RequestMapping("/api/v1/users")`), `ServiceOfferingController` (`@RequestMapping("/api/v1/services")`), `pom.xml` (execução `build-info` do `spring-boot-maven-plugin`), `application.properties`
- Test: `src/test/java/com/servicehub/controller/InfoControllerTest.java`, `src/test/java/com/servicehub/config/RequestIdFilterTest.java`

**Interfaces:**
- Produces: `record InfoResponse(String status, String version, String message)`; `InfoController.info()` em `GET /api/v1/info`; `RequestIdFilter extends OncePerRequestFilter` (`@Component`), constantes `public static final String HEADER = "X-Request-Id"` e `MDC_KEY = "requestId"`.

- [ ] **Step 1: Teste de fatia web do `InfoController`**

`@WebMvcTest(InfoController.class)`; `@MockitoBean` não é necessário.

```java
@Test @DisplayName("GET /api/v1/info deve retornar 200 com status OK")
void deveRetornarInfoQuandoChamado() throws Exception {
    mockMvc.perform(get("/api/v1/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OK"))
        .andExpect(jsonPath("$.version").isNotEmpty())
        .andExpect(jsonPath("$.message").value("ServiceHub API operacional"));
}
```

- [ ] **Step 2: Teste do filtro** (`@WebMvcTest(InfoController.class)` também carrega filtros `@Component`):

```java
@Test void deveGerarRequestIdQuandoHeaderAusente() // header X-Request-Id na resposta casa com regex UUID
@Test void deveEcoarRequestIdQuandoHeaderValido()  // envia "abc-123.X_9" → resposta tem o mesmo valor
@Test void deveSubstituirRequestIdQuandoHeaderInvalido() // envia 65 'a' e também "<script>" → resposta tem UUID, diferente do enviado
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `./mvnw.cmd -q -B test -Dtest='InfoControllerTest,RequestIdFilterTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL por classe inexistente / 404.

- [ ] **Step 4: Implementar**

- `InfoResponse` record. `InfoController` (`@RestController`, `@RequestMapping("/api/v1/info")`, `@Tag(name = "Status")`) injeta `ObjectProvider<BuildProperties>`; `version` = `buildProperties.getIfAvailable()` → `getVersion()`, senão `"dev"`.
- `pom.xml`: no `spring-boot-maven-plugin`, adicionar `<executions><execution><goals><goal>build-info</goal></goals></execution></executions>`.
- `RequestIdFilter.doFilterInternal`: lê o header; aceita apenas se casar `^[A-Za-z0-9._-]{1,64}$`, senão `UUID.randomUUID().toString()`; `MDC.put(MDC_KEY, id)`; `response.setHeader(HEADER, id)`; `try { chain.doFilter } finally { MDC.remove(MDC_KEY) }`.
- `application.properties`: `logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%X{requestId:-}] %logger{36} - %msg%n`.
- Mover os dois controllers para `/api/v1/...`; apagar `HelloController` e `StatusResponse`.

- [ ] **Step 5: Rodar e ver passar**

Run: mesmo comando do Step 3. Expected: PASS (4 testes).

- [ ] **Step 6: Commit**

```bash
git add -A src pom.xml && git commit -m "feat: prefixo /api/v1, endpoint de info e filtro X-Request-Id" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 1.2: Exceções de domínio e `GlobalExceptionHandler` com `ProblemDetail`

**Files:**
- Create: `exception/ResourceNotFoundException.java`, `exception/EmailAlreadyExistsException.java`, `exception/BusinessRuleException.java`, `exception/ProblemTypes.java`, `exception/GlobalExceptionHandler.java`
- Modify: `service/UserService.java`, `service/ServiceOfferingService.java`, `application.properties`
- Create: `src/test/java/com/servicehub/AbstractIntegrationTest.java`
- Test: `service/UserServiceTest.java`, `service/ServiceOfferingServiceTest.java`, `controller/UserControllerTest.java` (parte 1), `integration/UserApiIntegrationTest.java` (parte 1)

**Interfaces:**
- Produces:
  - `ResourceNotFoundException(String message)`, `EmailAlreadyExistsException(String email)` (mensagem `"E-mail já cadastrado: " + email`), `BusinessRuleException(String message)` — todas `extends RuntimeException`.
  - `ProblemTypes` com `URI NOT_FOUND = URI.create("urn:servicehub:problem:not-found")`, `EMAIL_ALREADY_EXISTS` (`...:email-already-exists`), `BUSINESS_RULE` (`...:business-rule`), `VALIDATION` (`...:validation-error`), `INTERNAL` (`...:internal-error`).
  - `GlobalExceptionHandler extends ResponseEntityExceptionHandler` (`@RestControllerAdvice`) com `ProblemDetail handleNotFound(ResourceNotFoundException, HttpServletRequest)` → 404, `handleEmailExists` → 409, `handleBusinessRule` → 409, `handleUnexpected(Exception)` → 500 com `detail` fixo `"Erro interno. Informe o X-Request-Id ao suporte."`. Todo `ProblemDetail` recebe `setType`, `setTitle` (`"Recurso não encontrado"`, `"Conflito"`, `"Regra de negócio violada"`, `"Erro interno"`), `setInstance(URI.create(request.getRequestURI()))` e `setProperty("timestamp", Instant.now())`.
  - `AbstractIntegrationTest`: `@SpringBootTest(webEnvironment = RANDOM_PORT) @Import(TestcontainersConfiguration.class)`, campo `@Autowired protected TestRestTemplate rest`, `@Autowired JdbcTemplate jdbc`, e `@BeforeEach void limparBanco()` executando `TRUNCATE TABLE reviews, service_requests, services, users RESTART IDENTITY CASCADE`.
  - `UserService.findEntityById` lança `ResourceNotFoundException("Usuário não encontrado: " + id)`; `createUser` lança `EmailAlreadyExistsException`; `ServiceOfferingService` idem com `"Serviço não encontrado: " + id` e `"Prestador não encontrado: " + id`.
  - Normalização: `UserService.createUser` aplica `email.trim().toLowerCase(Locale.ROOT)` antes de `existsByEmail` e de salvar.

- [ ] **Step 1: Testes unitários dos services** (`@ExtendWith(MockitoExtension.class)`, `@Mock` repositórios, `@InjectMocks`):

```java
// UserServiceTest
@Test void deveLancarEmailAlreadyExistsQuandoEmailJaCadastrado()   // existsByEmail("maria@x.com") → true; assertThatThrownBy(...).isInstanceOf(EmailAlreadyExistsException.class)
@Test void deveNormalizarEmailQuandoCriar()                        // dto email " Maria@X.com " → verify(repo).existsByEmail("maria@x.com"); save recebe user com email "maria@x.com"
@Test void deveLancarResourceNotFoundQuandoIdInexistente()         // findById(99L) → Optional.empty(); findById → ResourceNotFoundException com mensagem contendo "99"
// ServiceOfferingServiceTest
@Test void deveLancarResourceNotFoundQuandoPrestadorInexistente()  // createService com providerId 7 e userRepository.findById(7L) vazio
@Test void deveLancarResourceNotFoundQuandoServicoInexistente()    // findById(42L)
```

- [ ] **Step 2: Teste de fatia web** (`@WebMvcTest(UserController.class)`, `@MockitoBean UserService`):

```java
@Test void deveRetornar404ProblemDetailQuandoUsuarioNaoExiste() // when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Usuário não encontrado: 99"));
// expect status 404, content type application/problem+json, $.type == "urn:servicehub:problem:not-found", $.title == "Recurso não encontrado", $.detail contém "99", $.instance == "/api/v1/users/99", $.timestamp não vazio
@Test void deveRetornar409ProblemDetailQuandoEmailDuplicado()   // POST válido; service lança EmailAlreadyExistsException → 409, $.type termina em "email-already-exists"
```

- [ ] **Step 3: Teste de integração** (`UserApiIntegrationTest extends AbstractIntegrationTest`):

```java
@Test void deveRetornar404QuandoBuscarUsuarioInexistente() // rest.getForEntity("/api/v1/users/999", ProblemDetail.class) → 404 e body.getTitle() == "Recurso não encontrado"
@Test void deveRetornar409QuandoEmailJaExisteIgnorandoCaixa() // POST "maria@x.com" → 201; POST "Maria@X.com" → 409
```

- [ ] **Step 4: Rodar e ver falhar**

Run: `./mvnw.cmd -q -B test -Dtest='UserServiceTest,ServiceOfferingServiceTest,UserControllerTest,UserApiIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (classes de exceção não existem; respostas 500).

- [ ] **Step 5: Implementar** as classes da seção Interfaces; `application.properties` recebe `spring.mvc.problemdetails.enabled=true`, `server.error.include-stacktrace=never`, `server.error.include-message=never`. O handler de 500 loga a exceção com `log.error("Erro inesperado", ex)`.

- [ ] **Step 6: Rodar e ver passar**

Run: comando do Step 4. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A src && git commit -m "feat: excecoes de dominio e ProblemDetail (RFC 9457) para 404/409/500" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 1.3: Bean Validation, `Location` em `POST`, DTO de atualização de usuário

**Files:**
- Modify: `pom.xml` (`spring-boot-starter-validation`), `model/dto/UserDTO.java`, `model/dto/ServiceOfferingDTO.java`, `model/dto/UserResponseDTO.java`, `model/User.java`, `controller/UserController.java`, `controller/ServiceOfferingController.java`, `service/UserService.java`, `exception/GlobalExceptionHandler.java`
- Create: `model/dto/UserUpdateDTO.java`
- Test: `controller/UserControllerTest.java` (parte 2), `controller/ServiceOfferingControllerTest.java`, `integration/UserApiIntegrationTest.java` (parte 2)

**Interfaces:**
- Produces:
  - `UserDTO` (criação): `fullName` `@NotBlank @Size(min=2,max=100)`; `email` `@NotBlank @Email @Size(max=255)`; `password` `@NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[A-Z])(?=.*\\d).+$", message="A senha deve conter ao menos uma letra maiúscula e um número")`. Mensagens das demais em português (`"O nome é obrigatório"`, etc.).
  - `record UserUpdateDTO(@NotBlank @Size(min=2,max=100) String fullName, @Size(max=20) String phone, @Size(max=2000) String bio, @Size(max=500) String avatarUrl)`.
  - `User` ganha `phone`, `bio`, `avatarUrl` (`@Column(name="avatar_url")`), `updatedAt` (`@Column(name="updated_at")`) e `@PreUpdate onUpdate()` definindo `updatedAt = LocalDateTime.now()`; `onCreate` também define `updatedAt`.
  - `UserResponseDTO` ganha `phone`, `bio`, `avatarUrl`, `updatedAt`.
  - `ServiceOfferingDTO`: `title` `@NotBlank @Size(min=3,max=150)`; `description` `@Size(max=2000)`; `price` `@NotNull @DecimalMin("0.01") @DecimalMax("99999999.99")`; `category` `@NotNull`; `providerId` `@NotNull @Positive` (removido na Fase 2).
  - Controllers: `ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserDTO dto)` devolve `ResponseEntity.created(URI.create("/api/v1/users/" + body.getId())).body(body)`; `update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto)`; idem para serviços com `Location: /api/v1/services/{id}`.
  - `UserService.updateUser(Long id, UserUpdateDTO dto)` atualiza os quatro campos.
  - `GlobalExceptionHandler.handleMethodArgumentNotValid` (override) → 400 com `type` `VALIDATION`, `title` `"Dados inválidos"`, `detail` `"Um ou mais campos estão inválidos."`, propriedade `errors` = lista de `{field, message}` ordenada por `field`.
  - `GlobalExceptionHandler.handleHttpMessageNotReadable` (override) → 400 `type` `VALIDATION`; se a causa for `InvalidFormatException` cujo `getTargetType().isEnum()`, `detail` = `"Valor inválido para '" + campo + "'. Valores aceitos: " + Arrays.toString(constants)`; caso contrário `"Corpo da requisição inválido ou malformado."`.
  - Jackson mantém o default `FAIL_ON_UNKNOWN_PROPERTIES=false` (campos desconhecidos ignorados).

- [ ] **Step 1: Testes de fatia web**

```java
// UserControllerTest
@Test void deveRetornar400ComErrosQuandoNomeVazioESenhaFraca()  // {"fullName":"","email":"x@y.com","password":"abc"} → 400, $.type termina "validation-error", $.errors[?(@.field=='fullName')] existe, $.errors[?(@.field=='password')] existe
@Test void deveRetornar201ComLocationQuandoCriarUsuario()       // service devolve DTO id 5 → status 201, header Location "/api/v1/users/5"
@Test void deveIgnorarCampoDesconhecidoQuandoCriarUsuario()     // body com "role":"ADMIN" → 201 (não 400)
// ServiceOfferingControllerTest
@Test void deveRetornar400ListandoCategoriasQuandoCategoriaInvalida() // "category":"INVALIDA" → 400, $.detail contém "LIMPEZA"
@Test void deveRetornar400QuandoPrecoNaoNumerico()               // "price":"abc" → 400, $.title == "Dados inválidos"
@Test void deveRetornar400QuandoPrecoZero()                      // price 0 → 400, $.errors[0].field == "price"
@Test void deveRetornar201ComLocationQuandoCriarServico()        // Location "/api/v1/services/{id}"
```

- [ ] **Step 2: Teste de integração**

```java
@Test void deveAtualizarTelefoneEBioQuandoPut() // POST usuário → PUT /api/v1/users/{id} com {"fullName":"Maria Souza","phone":"62999990000","bio":"Eletricista"} → 200, body.phone == "62999990000", body.updatedAt != null; passwordHash nunca aparece: asserção de que o JSON bruto não contém "password"
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `./mvnw.cmd -q -B test -Dtest='UserControllerTest,ServiceOfferingControllerTest,UserApiIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL.

- [ ] **Step 4: Implementar** conforme Interfaces. `spring-boot-starter-validation` no `pom.xml`.

- [ ] **Step 5: Rodar e ver passar**

Run: comando do Step 3. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A src pom.xml && git commit -m "feat: Bean Validation nos DTOs, Location em POST e atualizacao de perfil do usuario" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 1.4: Paginação e filtros

**Files:**
- Create: `config/WebConfig.java`, `model/dto/ServiceFilter.java`, `repository/ServiceOfferingSpecifications.java`
- Modify: `repository/ServiceOfferingRepository.java`, `service/UserService.java`, `service/ServiceOfferingService.java`, `controller/UserController.java`, `controller/ServiceOfferingController.java`, `application.properties`
- Test: `integration/ServiceOfferingApiIntegrationTest.java`, `integration/UserApiIntegrationTest.java` (parte 3), `controller/ServiceOfferingControllerTest.java` (parte 2)

**Interfaces:**
- Produces:
  - `WebConfig` (`@Configuration @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)`). JSON de página: `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`.
  - `application.properties`: `spring.data.web.pageable.default-page-size=20`, `spring.data.web.pageable.max-page-size=100`.
  - `record ServiceFilter(ServiceCategory category, Long providerId, BigDecimal minPrice, BigDecimal maxPrice, String q)`.
  - `ServiceOfferingSpecifications` (classe final, métodos estáticos `Specification<ServiceOffering>`): `isActive()`, `hasCategory(ServiceCategory)`, `hasProvider(Long)`, `priceGte(BigDecimal)`, `priceLte(BigDecimal)`, `titleContains(String)` (`lower(title) like %lower(q)%`), e `fromFilter(ServiceFilter)` que combina `isActive()` com os não nulos via `Specification.allOf(...)`.
  - `ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering>`.
  - `UserService.findAll(Pageable) : Page<UserResponseDTO>`; `ServiceOfferingService.search(ServiceFilter, Pageable) : Page<ServiceOfferingResponseDTO>` (substitui `findAll()`); `findAll` sem parâmetro é removido dos dois services.
  - Controllers: `GET /api/v1/users` recebe `@PageableDefault(size = 20, sort = "id") Pageable`; `GET /api/v1/services` recebe `@RequestParam(required=false)` para cada campo do filtro mais `Pageable`, monta `ServiceFilter` e chama `search`.

- [ ] **Step 1: Teste de integração de serviços** (semear via `POST /api/v1/users` + `POST /api/v1/services`; 3 serviços: `LIMPEZA` 100.00 "Limpeza residencial", `LIMPEZA` 250.00 "Limpeza pós-obra", `ELETRICA` 150.00 "Instalação de tomadas"):

```java
@Test void deveFiltrarPorCategoriaQuandoListar()          // ?category=LIMPEZA → page.totalElements == 2
@Test void deveFiltrarPorFaixaDePrecoQuandoListar()       // ?minPrice=120&maxPrice=200 → 1 item, title "Instalação de tomadas"
@Test void deveBuscarPorTituloIgnorandoCaixaQuandoListar()// ?q=LIMPEZA → 2 itens
@Test void deveLimitarTamanhoDaPaginaQuandoSizeExcede()   // ?size=500 → page.size == 100 e status 200
@Test void devePaginarQuandoSizeUm()                       // ?size=1&page=1 → content tem 1 item, page.totalPages == 3
```

- [ ] **Step 2: Teste de integração de usuários**

```java
@Test void deveListarUsuariosPaginadosQuandoGet() // 2 usuários → GET /api/v1/users?size=1 → content.size()==1, page.totalElements==2
```

- [ ] **Step 3: Teste de fatia web** (`ServiceOfferingControllerTest`): `deveRepassarFiltrosAoServiceQuandoListar` — `GET /api/v1/services?category=ELETRICA&minPrice=10&q=tom` → `verify(service).search(eq(new ServiceFilter(ELETRICA, null, new BigDecimal("10"), null, "tom")), any(Pageable.class))`.

- [ ] **Step 4: Rodar e ver falhar**

Run: `./mvnw.cmd -q -B test -Dtest='ServiceOfferingApiIntegrationTest,UserApiIntegrationTest,ServiceOfferingControllerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (listas sem `content`/`page`; `search` inexistente).

- [ ] **Step 5: Implementar** conforme Interfaces.

- [ ] **Step 6: Rodar e ver passar**

Run: comando do Step 4. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A src && git commit -m "feat: paginacao em usuarios e servicos e filtros por categoria, prestador, preco e titulo" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 1.5: OpenAPI fiel ao contrato

**Files:**
- Modify: `config/OpenApiConfig.java`, `application.properties`, os três controllers, todos os DTOs
- Test: `integration/OpenApiIntegrationTest.java`

**Interfaces:**
- Produces:
  - `OpenApiConfig`: bean `OpenAPI servicehubOpenAPI(ObjectProvider<BuildProperties>)` com `Info` título `"ServiceHub API"`, descrição `"API de gestão de prestadores, serviços, ordens de serviço e avaliações — projeto PBL da disciplina Desenvolvimento de API Back-End (UniEVANGÉLICA)"`, versão do `BuildProperties` ou `"dev"`, `Contact` nome `"Prof. Eduardo Dias Pereira"`, `License` `"MIT"`; `servers`: `http://localhost:8080` (`"Execução local"`) e `http://localhost:8090` (`"Docker Compose"`). Bean `GroupedOpenApi v1Api()` com `group("v1")`, `displayName("ServiceHub API v1")`, `pathsToMatch("/api/v1/**")`.
  - `application.properties`: `springdoc.api-docs.path=/api-docs`, `springdoc.swagger-ui.path=/docs`, `springdoc.swagger-ui.operations-sorter=method`, `springdoc.swagger-ui.tags-sorter=alpha`, `springdoc.swagger-ui.doc-expansion=none`.
  - Em cada endpoint: `@Operation(summary, description)` em português; `@ApiResponses` com **exatamente** os códigos da tabela 5.3 do design para aquele endpoint (nesta fase: 200/201/204, 400, 404, 409; sem 401/403), erros com `content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))`; `@Parameter(description, example)` em `id` e em cada filtro; `@Schema(description, example)` em todo campo de DTO (exemplos do domínio: `"Instalação elétrica residencial"`, `"150.00"`, `"maria.silva@email.com"`). Remover as descrições "exceção não tratada, sem handler dedicado ainda".

- [ ] **Step 1: Teste de integração**

```java
@Test void deveExporEspecificacaoDoGrupoV1()        // GET /api-docs/v1 → 200; body contém "\"/api/v1/users\"", "\"/api/v1/services\"", "\"/api/v1/info\"", "ProblemDetail"
@Test void naoDeveDocumentarRespostas500NosEndpointsDeCrud() // body de /api-docs/v1 não contém a string "\"500\":" dentro de paths de /api/v1/users e /api/v1/services (asserção simples: body não contém "sem handler")
@Test void deveServirSwaggerUiEmDocs()               // GET /docs → status 200 ou 302; GET /swagger-ui/index.html → 200
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw.cmd -q -B test -Dtest=OpenApiIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (404 em `/api-docs/v1`).

- [ ] **Step 3: Implementar** conforme Interfaces.

- [ ] **Step 4: Rodar e ver passar**

Run: comando do Step 2. Expected: PASS.

- [ ] **Step 5: Verificação manual (uma vez)**

Run: `docker compose up -d postgres && ./mvnw.cmd -q spring-boot:run` e abrir `http://localhost:8080/docs`.
Expected: grupo "ServiceHub API v1", tags Status/Usuários/Serviços, cada erro documentado com schema `ProblemDetail`; checklist da Aula 5 (Apêndice A do design) satisfeito exceto os itens de JWT e grupo público (Fase 2). Encerrar com Ctrl+C.

- [ ] **Step 6: Commit**

```bash
git add -A src && git commit -m "docs(openapi): metadados, grupo v1, respostas de erro ProblemDetail e exemplos de dominio" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

### Task 1.6: Configuração por variáveis de ambiente, ADR-0004, docs, tag e push

**Files:**
- Modify: `src/main/resources/application.properties`, `README.md`, `CLAUDE.md`, `docs/superpowers/specs/2026-10-22-servicehub-api-design.md` (apenas a linha de Status)
- Create: `docs/adr/0004-problem-detail-rfc-9457.md`

**Interfaces:**
- Produces: `application.properties` final da Fase 1:

```properties
spring.application.name=servicehub-api
server.port=${SERVER_PORT:8080}
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/servicehub}
spring.datasource.username=${DB_USER:servicehub}
spring.datasource.password=${DB_PASS:secret}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.mvc.problemdetails.enabled=true
server.error.include-stacktrace=never
server.error.include-message=never
spring.data.web.pageable.default-page-size=20
spring.data.web.pageable.max-page-size=100
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/docs
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.doc-expansion=none
logging.level.com.servicehub=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%X{requestId:-}] %logger{36} - %msg%n
```

Removidos: `server.servlet.context-path`, `spring.jackson.date-format`, `spring.datasource.driver-class-name`, `spring.jpa.properties.hibernate.dialect` (Hibernate 7 detecta). `docker-compose.yml` passa a usar `DB_URL`/`DB_USER`/`DB_PASS` no serviço `api` em vez de `SPRING_DATASOURCE_*` (mínimo para continuar funcionando; o Compose completo é da Fase 5).

- [ ] **Step 1: Aplicar o `application.properties` acima e ajustar o Compose**

- [ ] **Step 2: Verificar que o `open-in-view=false` não quebrou `providerName`**

Run: `./mvnw.cmd -q -B test -Dtest=ServiceOfferingApiIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (o `toResponseDTO` roda dentro de `@Transactional(readOnly = true)` do service; se falhar com `LazyInitializationException`, anotar `search`/`findById` com `@Transactional(readOnly = true)` e manter `open-in-view=false`).

- [ ] **Step 3: ADR-0004** (`docs/adr/0004-problem-detail-rfc-9457.md`): contexto (Aula 8 mostra `Map` artesanal; Aula 2 pede erros consistentes), decisão (`ProblemDetail` nativo, tipos `urn:servicehub:problem:*`, propriedade `errors` em validação), consequências (contrato único documentado no OpenAPI; clientes leem `type` e não `detail`).

- [ ] **Step 4: README**: seção "Contrato de erro" com um exemplo de `ProblemDetail` 400 com `errors`; seção "Documentação" apontando `/docs` e `/api-docs/v1`; tabela de variáveis de ambiente; tag `fase-1-contrato-qualidade` no mapa.

- [ ] **Step 5: `CLAUDE.md`**: atualizar "Current known gaps" (remover erros 500, validação, paginação, DTOs; manter segurança e senha em texto puro para a Fase 2); documentar `ProblemTypes`, `AbstractIntegrationTest` (TRUNCATE no `@BeforeEach`), `@MockitoBean`, `VIA_DTO`, e que `HelloController` foi substituído por `InfoController`.

- [ ] **Step 6: Build completo**

Run: `./mvnw.cmd -q -B verify; echo exit=$?`
Expected: `exit=0`; todos os testes das Tasks 0.3 a 1.5 passam.

- [ ] **Step 7: Commit, tag e push**

```bash
git add -A && git commit -m "chore: configuracao por variaveis de ambiente, ADR-0004 e documentacao da fase 1" -m "Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
git tag -a fase-1-contrato-qualidade -m "Fase 1: /api/v1, ProblemDetail, validacao, paginacao, OpenAPI e testes"
git push origin main && git push origin fase-1-contrato-qualidade
```

Expected: `git ls-remote --tags origin` lista as duas tags.
