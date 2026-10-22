# ServiceHub API — Documento de Design

**Data:** 2026-10-22
**Autor:** Claude (a pedido do Prof. Eduardo Dias Pereira)
**Status:** decisões fechadas em 2026-10-22; aguardando aprovação final para iniciar o plano de implementação
**Público:** quem for construir a API de referência (humano ou agente de IA) sem acesso ao autor do pedido

---

## 1. Intenção e porquê

O ServiceHub API é o projeto PBL da disciplina *Desenvolvimento de API Back-End* (Engenharia de Software, 6º período, UniEVANGÉLICA, 2026/2). Os alunos constroem a própria API ao longo do semestre seguindo o norte de cada aula; ao final, entregam uma API funcionando em Docker Compose (API + PostgreSQL), com autenticação JWT, documentação OpenAPI, testes automatizados e pipeline de CI.

Este documento descreve a **implementação de referência** que o professor usará em sala para demonstrar que programação assistida por IA exige domínio de **fundamentos, arquitetura e processo**: a IA escreve a sintaxe; o engenheiro decide o contrato, as regras de negócio, a estrutura, a estratégia de testes e o ambiente de execução, e verifica o resultado.

Por isso o projeto tem dois produtos:

1. **A API em si**, construída estritamente com as técnicas ensinadas nos slides (Aulas 1 a 16), em versões modernas e corretas.
2. **O rastro do processo**: um commit e uma tag por fase, registros de decisão (ADRs), README e `CLAUDE.md` mantidos, para que o professor possa abrir qualquer fase e mostrar "o que mudou e por quê".

## 2. Objetivos, não-objetivos e anti-objetivos

### Objetivos

- Cobrir **todas as entregas práticas** previstas nos slides: CRUD com Flyway (A4), OpenAPI (A5), validação + Spring Security + `@PreAuthorize` (A8), login JWT + roles (A9), testes unitários/web/integração (A10), Dockerfile multi-stage (A11), Compose com healthcheck, `.env` e perfis (A12), CI com GitHub Actions + Actuator/Prometheus + logs estruturados (A14).
- Implementar o domínio completo da Aula 4: `users`, `services`, `service_requests` (ordens de serviço) e `reviews`, incluindo a **máquina de estados** da ordem de serviço, que é a regra de negócio central e hoje não existe no código.
- Ser um exemplo de **API nível 2 de Richardson bem feita**: substantivos no plural, verbos HTTP corretos, status codes semânticos, erros padronizados, paginação, versionamento por path.
- Passar nos próprios critérios de aceite da banca (Aula 16): `docker compose up` sobe tudo, dados persistem, Swagger completo, pipeline verde.
- Ser **reproduzível na máquina de um aluno** com JDK 21, Maven Wrapper e Docker Desktop.

### Não-objetivos (fora de escopo desta versão)

- Refresh tokens e revogação de tokens (citados na Aula 8/9 como "próximos passos"; ficam como ADR + issue).
- Deploy em nuvem (Railway/Render). A imagem e o Compose ficam **prontos** para isso; executar o deploy depende de conta do professor.
- Front-end. A integração é demonstrada com Swagger UI e uma coleção Postman exportada em `docs/postman/`.
- gRPC, API Gateway, microsserviços, Kubernetes, Swarm.
- Upload de arquivos (`avatar_url` continua sendo uma string).
- Notificações por e-mail.

### Anti-objetivos (o que faria o projeto falhar mesmo "funcionando")

- Código que o professor não consiga explicar em sala com os slides na mão. Nada de biblioteca "mágica" extra sem valor didático.
- Entidades JPA expostas em respostas HTTP; qualquer vazamento de `password_hash`.
- Quebrar migrações já aplicadas (editar ou renomear V1–V7). Flyway valida checksum: apenas migrações novas.
- Dependência de banco em memória (H2) para testes: as migrações usam `BIGSERIAL`, `TEXT`, `NUMERIC` e `CHECK` do Postgres. Testes rodam contra Postgres real.
- Segredos versionados (`.env`, chave JWT real, senha de admin em produção).

## 3. Restrições

- **Stack dos slides:** Java + Spring Boot + Spring Data JPA + PostgreSQL + Flyway + Springdoc + Spring Security + JUnit 5/Mockito/MockMvc + Docker/Compose + GitHub Actions.
- **Ponto de partida obrigatório:** o repositório atual em `c:\xampp\htdocs\servicehub-api` (Spring Boot 4.1.1, migrações V1–V7, CRUD de `users` e `services`, Springdoc 3.0.3). Evoluir, não recomeçar.
- **Spring Boot 4.x** mudou nomes de starters e APIs de teste. Detalhes no `CLAUDE.md` (seção "Spring Boot 4 dependency gotcha") e na seção 5.1 abaixo. Onde o slide mostra API antiga (ex.: `@MockBean`, `springdoc 2.5.0`, `jjwt 0.11`), a referência usa a API atual e o ADR registra a diferença.
- **Windows** no computador do professor (`mvnw.cmd`); Linux nos runners de CI e nos containers. Scripts devem funcionar nos dois.
- Portas do Compose já em uso pelos alunos: API `8090→8080`, Postgres `5433→5432`, pgAdmin `8091→80`. Manter.
- O repositório git existe mas **não tem nenhum commit**. O primeiro passo da construção é commitar a baseline.

## 4. Ponto de partida (estado atual do repositório)

| Item | Estado |
| --- | --- |
| Build | Spring Boot 4.1.1, `java.version=25`, Maven Wrapper |
| Persistência | Postgres via Flyway V1–V7; `ddl-auto=validate` |
| Tabelas | `users`, `services`, `service_requests`, `reviews` (as duas últimas **sem entidade, repositório, service ou controller**) |
| Endpoints | `/api/hello`, `/api/status`, `/api/info`, CRUD `/api/users`, CRUD `/api/services` |
| DTOs | Request e Response separados para `User` e `ServiceOffering` (bom) |
| Segurança | Nenhuma. Senha gravada em texto puro em `password_hash` |
| Erros | `RuntimeException` → HTTP 500 genérico |
| Validação | Nenhuma (`@Valid` ausente, sem `starter-validation`) |
| Paginação | Nenhuma (`findAll()` retorna lista inteira) |
| Documentação | Springdoc configurado; `@Tag`/`@Operation` presentes; respostas de erro documentadas como "500 (sem handler ainda)" |
| Testes | Só `contextLoads`, que exige Postgres vivo |
| Docker | Dockerfile de estágio único copiando `target/*.jar` (exige build prévio); Compose sem healthcheck, credenciais inline, `version: '3.9'` obsoleto |
| Lixo | `src/main/resources/V6__Create_reviews_table.sql` fora de `db/migration` (duplicado de V7) |
| Docs | `CLAUDE.md` atualizado; sem README; sem ADRs |

## 5. Decisões de arquitetura

Tudo nesta seção é **decisão minha (Claude)**, tomada para ficar fiel aos slides e às práticas atuais. Cada uma pode ser revertida pelo professor; as que mais merecem atenção estão marcadas com ⚠️.

### 5.1 Stack e versões

- **Java 21 LTS** (⚠️ hoje o `pom.xml` diz 25). Os slides dizem "Java 17 ou 21"; 21 é o LTS que os alunos têm mais chance de ter instalado, e o JDK 25 local compila para `--release 21` sem problema. Imagem de runtime: `eclipse-temurin:21-jre-alpine`.
- **Spring Boot 4.1.1** (manter). Starters: `spring-boot-starter-webmvc`, `-data-jpa`, `-flyway`, `-validation`, `-security`, `-oauth2-resource-server`, `-actuator`; testes: `spring-boot-starter-test`, `-webmvc-test`, `-data-jpa-test`, `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `spring-security-test`. O construtor deve conferir os nomes exatos contra o BOM 4.1.1 antes de assumir.
- **Springdoc 3.0.x** (manter).
- **Micrometer Prometheus registry** (`micrometer-registry-prometheus`).
- **JaCoCo** plugin Maven para relatório de cobertura (Aula 10 cita 80% como meta; relatório gerado, meta **não** bloqueia o build).
- Lombok continua (os slides usam). Preferir `record` Java para DTOs novos e `@Data` apenas nas entidades existentes, para mostrar a opção moderna sem reescrever o que já existe.

### 5.2 Domínio e modelo de dados

Segue o schema da Aula 4 (já migrado). Nomes em inglês no código e no banco, como nos slides; descrições em português na documentação.

```text
users ──1:N── services ──1:N── service_requests ──1:1── reviews
  │                                   │                     │
  └──── client_id ────────────────────┘                     │
  └──── reviewer_id ────────────────────────────────────────┘
```

Alterações de schema (novas migrações, nunca editar as antigas):

- **V8** `Add_role_to_users`: `role VARCHAR(20) NOT NULL DEFAULT 'CLIENT'` + `CHECK (role IN ('ADMIN','PROVIDER','CLIENT'))`. Perfis exatamente como na Aula 9.
- **V9** `Align_with_student_schema` (Fase 3): leva o banco ao schema-alvo entregue aos alunos em `docs/alunos/servicehub_schema.sql` — `services.updated_at`; `NOT NULL` em `is_active`/`created_at`/`updated_at`; `CHECK (email = LOWER(email))`; `CHECK` de `category`, `price > 0`, `total_price > 0`; todas as FKs `ON DELETE RESTRICT` (substitui o `CASCADE` da V4, coerente com o soft delete). O schema-alvo e o "Mapa de Construção" (PDF dos alunos) são a referência de contrato; a implementação de referência deve terminar idêntica a eles.
- `service_requests.status` já tem `CHECK` com os cinco estados da Aula 4.

Entidades a criar: `ServiceRequest`, `Review`; enums `Role`, `RequestStatus`. `User` ganha `role`, `phone`, `bio`, `avatarUrl`, `active`, `updatedAt` (as colunas já existem desde V3) e `@PreUpdate`.

**Máquina de estados da ordem de serviço** (regra de negócio central):

| De | Para | Quem pode |
| --- | --- | --- |
| `PENDING` | `ACCEPTED` | PROVIDER dono do serviço |
| `PENDING` | `CANCELLED` | CLIENT que abriu, ou PROVIDER dono |
| `ACCEPTED` | `IN_PROGRESS` | PROVIDER dono |
| `ACCEPTED` | `CANCELLED` | CLIENT ou PROVIDER |
| `IN_PROGRESS` | `COMPLETED` | PROVIDER dono |
| qualquer outra | — | **409 Conflict** com `ProblemDetail` explicando a transição inválida |

ADMIN pode fazer qualquer transição válida. `COMPLETED` e `CANCELLED` são terminais.

Invariantes:

- `total_price` é **snapshot** de `services.price` no momento da criação da ordem.
- Um CLIENT não abre ordem para o próprio serviço.
- `Review` só pode ser criada pelo CLIENT da ordem, com a ordem em `COMPLETED`, uma única vez por ordem (a `UNIQUE` do banco é a segunda linha de defesa; a primeira é o service, que responde 409).
- Só serviços `active = true` aceitam novas ordens.
- **Quem contrata:** qualquer usuário autenticado com papel `CLIENT` ou `PROVIDER` pode abrir ordens (um prestador também contrata outros prestadores). É isso que dá sentido à invariante "não abre ordem para o próprio serviço". ADMIN não abre ordens. (⚠️ decisão de domínio; ver seção 8.)
- **Papel é único e definido no registro.** Só ADMIN altera papel, via `PATCH /users/{id}/role`. Não há "upgrade" automático.
- Exclusão de usuário e de serviço é **soft delete** (⚠️: `is_active = false`). Motivo: `services.provider_id` tem `ON DELETE CASCADE` desde V4, e apagar um prestador apagaria o histórico de ordens e avaliações de clientes. Semântica completa:
  - `DELETE /services/{id}` responde **409** enquanto o serviço tiver ordens em `PENDING`, `ACCEPTED` ou `IN_PROGRESS`; o dono precisa resolvê-las antes. Depois, `active = false`.
  - `DELETE /users/{id}` responde **409** enquanto o usuário tiver ordens ativas (como cliente ou como prestador). Depois, `active = false` **e** todos os serviços dele passam a `active = false` (cascata lógica, na mesma transação).
  - Usuário inativo **não faz login** (401) e um JWT emitido antes da desativação **deixa de valer**: a cada requisição autenticada o filtro carrega o usuário pelo claim `uid` e rejeita se `active = false` (uma consulta por requisição, indexada por PK; é o custo aceito para ter revogação sem refresh tokens). ADR-0002 registra.
  - Serviço inativo: `GET /services/{id}` responde **404** para o público; o PROVIDER dono e o ADMIN continuam vendo (endpoint `permitAll` com principal opcional). Listagem pública não inclui inativos.
  - Reativação: serviço, via `PUT /services/{id}` com campo `active` (dono ou ADMIN; é o único campo de "estado" aceito no body). Usuário, via `PATCH /users/{id}/active` (ADMIN). Reativar usuário **não** reativa serviços automaticamente.

### 5.3 Contrato REST (v1)

Prefixo `/api/v1` (Aula 5, slide 20: versionamento por path). Os endpoints atuais `/api/users` e `/api/services` são movidos; `/api/hello` e `/api/status` (texto puro) são removidos; `/api/info` vira `/api/v1/info`.

Convenções globais:

- JSON em UTF-8; datas em ISO-8601 **sem offset** (`2026-10-22T14:30:00`), tipo `LocalDateTime`, coerente com as colunas `TIMESTAMP` sem fuso das migrações. Para que `@Future` e os horários da demo batam com o relógio da sala, **o fuso da JVM é fixado em `America/Sao_Paulo`** no Dockerfile (`-Duser.timezone`) e no Compose (`TZ`), e documentado no README. Remover `spring.jackson.date-format` legado.
- Listagens **sempre paginadas** com `page`, `size` (máximo 100) e `sort` do Spring Data; resposta `Page` serializada de forma estável (`spring.data.web.pageable.*` e `PagedModel` via `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).
- `201 Created` devolve `Location: /api/v1/<recurso>/{id}` e o corpo do recurso.
- `DELETE` devolve `204`.
- Erros: ver 5.5.

| Método | Rota | Quem | Sucesso | Erros relevantes |
| --- | --- | --- | --- | --- |
| `POST` | `/auth/register` | público | 201 `UserResponse` | 400 validação; 409 e-mail duplicado; `role` aceita só `CLIENT`/`PROVIDER` |
| `POST` | `/auth/login` | público | 200 `{accessToken, tokenType:"Bearer", expiresIn}` | 401 credenciais inválidas |
| `GET` | `/auth/me` | autenticado | 200 `UserResponse` | 401 |
| `GET` | `/users` | ADMIN | 200 `Page<UserResponse>` | 401/403 |
| `GET` | `/users/{id}` | ADMIN ou o próprio | 200 | 403, 404 |
| `PUT` | `/users/{id}` | ADMIN ou o próprio | 200 | 400, 403, 404 (não altera `role`, `email` nem senha) |
| `DELETE` | `/users/{id}` | ADMIN | 204 (soft, cascata lógica nos serviços) | 403, 404, 409 ordens ativas |
| `PATCH` | `/users/{id}/role` | ADMIN | 200 `UserResponse` | 400, 403, 404 (`{role: CLIENT\|PROVIDER\|ADMIN}`) |
| `PATCH` | `/users/{id}/active` | ADMIN | 200 `UserResponse` | 400, 403, 404 (`{active: true\|false}`) |
| `GET` | `/services` | público | 200 `Page<ServiceResponse>` (só ativos; ADMIN vê todos com `?includeInactive=true`) | — |
| `GET` | `/services/{id}` | público | 200 | 404 |
| `POST` | `/services` | PROVIDER | 201 | 400; 403 se CLIENT. `provider` = usuário autenticado, **nunca** vem do body |
| `PUT` | `/services/{id}` | PROVIDER dono ou ADMIN | 200 | 400, 403, 404 (aceita `active` para reativar/desativar) |
| `DELETE` | `/services/{id}` | PROVIDER dono ou ADMIN | 204 (soft) | 403, 404, 409 ordens ativas |
| `GET` | `/services/{id}/reviews` | público | 200 `Page<ReviewResponse>` | 404 |
| `POST` | `/service-requests` | CLIENT ou PROVIDER | 201 `ServiceRequestResponse` (`PENDING`) | 400; 403 (ADMIN); 404 serviço; 409 serviço inativo ou próprio |
| `GET` | `/service-requests` | autenticado | 200 `Page` com as ordens em que o usuário é **cliente ou prestador** (`?role=client\|provider` para filtrar o lado); ADMIN vê todas; filtro `?status=` | 401 |
| `GET` | `/service-requests/{id}` | CLIENT dono, PROVIDER dono ou ADMIN | 200 | 403, 404 |
| `PATCH` | `/service-requests/{id}/status` | conforme tabela de transições | 200 | 400, 403, 404, 409 transição inválida |
| `POST` | `/service-requests/{id}/reviews` | CLIENT dono | 201 `ReviewResponse` | 400; 403; 404; 409 não concluída ou já avaliada |
| `GET` | `/reviews/{id}` | público | 200 | 404 |
| `GET` | `/info` | público | 200 `{status, version, message}` | — |
| `GET` | `/actuator/health`, `/actuator/info` | público | 200 | — |
| `GET` | `/actuator/**` (demais) | ADMIN | 200 | 401/403 |

Filtros de `GET /services`: `category`, `providerId`, `minPrice`, `maxPrice`, `q` (busca em `title`, case-insensitive). Implementar com `Specification` ou query JPQL com parâmetros nomeados; nunca concatenação (Aula 8).

Respostas de serviço incluem `providerId` e `providerName` (já é assim). Respostas de ordem incluem `serviceId`, `serviceTitle`, `clientId`, `clientName`, `providerId`, `status`, `totalPrice`, `scheduledAt`, `notes`, `createdAt`, `updatedAt`.

### 5.4 Segurança

- **Spring Security 7 (Boot 4)** com `SecurityFilterChain`, `SessionCreationPolicy.STATELESS`, CSRF desabilitado (API stateless, como nos slides).
- **Senhas com BCrypt** (`BCryptPasswordEncoder`, custo padrão). Ao registrar, hash; ao migrar da baseline, usuários existentes com senha em texto puro ficam inválidos e isso é documentado (banco de referência é descartável: `docker compose down -v`).
- **JWT HS256 com o suporte nativo do Spring Security** (⚠️): `spring-boot-starter-oauth2-resource-server` + `NimbusJwtEncoder`/`NimbusJwtDecoder` com chave simétrica vinda de `JWT_SECRET` (mínimo 32 bytes; a aplicação **recusa subir** com segredo ausente ou curto). Os slides da Aula 9 usam a biblioteca `jjwt` com um `OncePerRequestFilter` manual. A escolha nativa elimina uma dependência de terceiros e um filtro escrito à mão, usando o mesmo conceito (token Bearer assinado, claims `sub`, `iat`, `exp`, `roles`). O ADR-0003 registra o paralelo slide↔implementação para o professor explicar em sala; se preferir fidelidade literal ao slide, o construtor troca por `jjwt 0.12.x` sem alterar o contrato.
- Claims: `sub` = e-mail, `uid` = id do usuário, `roles` = `["ROLE_PROVIDER"]`, `iat`, `exp`. Expiração padrão **24h** (`JWT_EXPIRATION_SECONDS`, como na Aula 9).
- `@EnableMethodSecurity` + `@PreAuthorize` **na camada de serviço** (Aula 8, slide 20). Verificações de **propriedade (BOLA)** feitas explicitamente no service com o usuário autenticado, nunca confiando em ids do body.
- Mass assignment: DTOs de request não têm `role` (exceto `register`, restrito), `id`, `providerId`, `clientId` nem `status` livre.
- CORS: `CorsConfigurationSource` com origens lidas de `CORS_ALLOWED_ORIGINS` (padrão `http://localhost:3000,http://localhost:5173`), sem wildcard com credenciais.
- Headers de segurança padrão do Spring Security mantidos; `server.error.include-stacktrace=never`; `include-message=never`.
- Sanitização: `trim` e normalização nos DTOs; campos de texto livre (`description`, `notes`, `comment`, `bio`) passam por escape HTML (`StringEscapeUtils.escapeHtml4`, Apache Commons Text) no service, como na Aula 8. Tamanho máximo de payload configurado.
- Swagger UI com botão **Authorize** (`SecurityScheme` bearer JWT), rotas `/swagger-ui/**`, `/v3/api-docs/**` liberadas (⚠️ em produção o ADR recomenda desligar ou proteger).
- **Usuário ADMIN inicial** (⚠️): criado por um `ApplicationRunner` no startup **se não existir**, com e-mail e senha vindos de `SERVICEHUB_ADMIN_EMAIL` / `SERVICEHUB_ADMIN_PASSWORD`. Sem essas variáveis, nenhum admin é criado e um `WARN` é logado. O `.env.example` traz valores de desenvolvimento. Motivo: evita hash fixo em migração SQL versionada.
- Rate limiting no login (Bucket4j) fica como **opcional** na Fase 7.

### 5.5 Tratamento de erros

`@RestControllerAdvice` que devolve **`ProblemDetail` (RFC 9457)**, formato nativo do Spring 6+/7 (`application/problem+json`), em vez do `Map` artesanal do slide. Campos: `type`, `title`, `status`, `detail`, `instance`, mais `timestamp` e, em erros de validação, `errors: [{field, message}]`.

Exceções de domínio em `com.servicehub.exception`: `ResourceNotFoundException` (404), `EmailAlreadyExistsException` (409), `InvalidStateTransitionException` (409), `BusinessRuleException` (409), `AccessDeniedException` do Spring (403), `MethodArgumentNotValidException` (400), `HttpMessageNotReadableException` (400), `AuthenticationException` (401), fallback (500 sem detalhes internos). A classe `ErrorResponse` citada na Aula 5 é o próprio `ProblemDetail`, documentado via `@Schema`.

### 5.6 Validação

`spring-boot-starter-validation` + `@Valid` em todo `@RequestBody`. Regras mínimas:

- `fullName` 2–100; `email` `@Email` ≤ 255; senha 8–72 com ao menos uma maiúscula e um dígito (regex da Aula 8).
- `title` 3–150; `price` `@DecimalMin("0.01")` ≤ 99.999.999,99; `category` enum obrigatório; `description` ≤ 2000.
- `scheduledAt` `@Future` (opcional); `notes` ≤ 1000; `rating` 1–5; `comment` ≤ 1000.
- `status` em `PATCH .../status` é enum; valor fora do enum → 400 com mensagem listando os válidos.

### 5.7 Documentação OpenAPI

- `OpenApiConfig` com `Info` (título, descrição, versão lida de `@project.version@`), contato, licença, `SecurityScheme` bearer, `servers` (local `http://localhost:8080`, docker `http://localhost:8090`).
- Dois grupos `GroupedOpenApi`: `v1` (`/api/v1/**`) e `public` (`/api/v1/auth/**`, `/api/v1/services/**` só leitura) para ilustrar a Aula 5, slide 16.
- Todo controller com `@Tag`; todo endpoint com `@Operation` (summary + description em português), `@ApiResponses` cobrindo sucesso e **cada** erro da tabela 5.3 com `ProblemDetail` como schema; `@Parameter` em path/query; `@Schema` com `example` realista em todos os DTOs.
- Paths: `springdoc.api-docs.path=/api-docs`, `springdoc.swagger-ui.path=/docs` (Aula 5, slide 14), mantendo redirecionamento de `/swagger-ui.html`.
- Checklist da Aula 5, slide 23, é o critério de pronto desta parte.

### 5.8 Testes

Pirâmide da Aula 10, com Postgres real:

- **Unitários (Service)**: `@ExtendWith(MockitoExtension.class)`, `@Mock` repositórios, AssertJ. Cobrem regras de negócio: e-mail duplicado, máquina de estados completa (todas as transições válidas e inválidas), snapshot de preço, propriedade/BOLA, review única.
- **Fatia web (Controller)**: `@WebMvcTest(Controller.class)` + `@MockitoBean` (Boot 4 removeu `@MockBean`) + `spring-security-test` (`@WithMockUser`, `jwt()` post-processor). Validam status, `Location`, JSON, erros de validação em `ProblemDetail`, 401/403.
- **Integração**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ServiceConnection` com `PostgreSQLContainer` (Testcontainers), Flyway aplicando V1–V8, `TestRestTemplate`. Um teste de fluxo ponta a ponta: registra provider e client → login → cria serviço → abre ordem → aceita → inicia → conclui → avalia → consulta avaliações. Mais um teste garantindo que **todos os endpoints protegidos respondem 401 sem token**.
- O `contextLoads` atual passa a usar o mesmo container (deixa de exigir Postgres externo).
- `mvnw verify` roda tudo e gera `target/site/jacoco/index.html`. Pré-requisito: Docker em execução (já é pré-requisito da disciplina).
- Nomenclatura `deveXQuandoY` + `@DisplayName`, padrão AAA (Aula 10).

### 5.9 Configuração, perfis e segredos

- `application.properties` lê **tudo de variáveis de ambiente com default de desenvolvimento**: `DB_URL` (default `jdbc:postgresql://localhost:5433/servicehub`, que é a porta publicada pelo Compose), `DB_USER`, `DB_PASS`, `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `CORS_ALLOWED_ORIGINS`, `SERVICEHUB_ADMIN_*`, `SERVER_PORT`.
- Perfis: `dev` (default: `show-sql`, log `DEBUG` em `com.servicehub`, Swagger ligado) e `prod` (`show-sql=false`, logs JSON estruturados via `logging.structured.format.console=ecs`, `INFO`). O Compose sobe com `SPRING_PROFILES_ACTIVE=prod` para que a demo em sala mostre logs de produção; `dev` é para `mvnw spring-boot:run`.
- `.env.example` versionado com todas as variáveis comentadas; `.env` no `.gitignore`.
- `management.endpoints.web.exposure.include=health,info,metrics,prometheus`; `health` com `show-details=when_authorized`; `info` com `build` (via `build-info` do plugin Spring Boot).

### 5.10 Docker e Compose

- **Dockerfile multi-stage** (Aula 11, slide 24): estágio `builder` `maven:3.9-eclipse-temurin-21` com cache de dependências (`COPY pom.xml` + `dependency:go-offline`, depois `COPY src`), `mvn package -DskipTests`; estágio final `eclipse-temurin:21-jre-alpine`, usuário não-root `spring`, `COPY --from=builder`, `EXPOSE 8080`, `ENTRYPOINT` com `-XX:MaxRAMPercentage=75.0`, `HEALTHCHECK` chamando `/actuator/health` com `wget`.
- Testes **não** rodam dentro do `docker build` (precisariam de Docker-in-Docker); rodam no `mvnw verify` e no CI. ADR registra o porquê.
- `.dockerignore` com `target/`, `.git/`, `docs/`, `.env`.
- **`docker-compose.yml`** (sem `version:`): `postgres` (`postgres:16-alpine`, `healthcheck` com `pg_isready`, volume `postgres_data`, `env_file: .env`), `api` (`build: .`, `depends_on: postgres: condition: service_healthy`, `env_file`, `restart: unless-stopped`, porta `8090:8080`), `pgadmin` sob `profiles: ["tools"]` (não sobe por padrão). Rede `servicehub-net`.
- `docker-compose.dev.yml` de override opcional: porta de debug `5005`, `SPRING_PROFILES_ACTIVE=dev` (Aula 12, slide 16).
- Critério: `docker compose up -d --build` numa máquina limpa, com `.env` copiado de `.env.example`, sobe API saudável; `docker compose down` e `up` de novo mantém os dados; `down -v` zera.

### 5.11 CI (GitHub Actions)

O branch local `master` é renomeado para `main` na Fase 0. `.github/workflows/ci.yml`: gatilhos `push` e `pull_request` em `main`; `ubuntu-latest`; `actions/checkout@v4`, `actions/setup-java@v4` (Temurin 21, cache Maven); `./mvnw -B verify` (Testcontainers funciona nos runners); upload do relatório JaCoCo como artefato; job `docker` que faz `docker build` da imagem para provar que o Dockerfile está íntegro. Push da imagem para GHCR fica como passo comentado/opcional (exige o repositório no GitHub).

### 5.12 Observabilidade

Actuator `health`/`info`/`metrics`/`prometheus`; métrica customizada `servicehub.service_requests.transitions` (counter com tag `to`) para o professor mostrar uma métrica de negócio além das de JVM. Logs via SLF4J com `MDC` de `requestId` (filtro simples que lê/gera `X-Request-Id` e devolve no response, Aula 6 slide 11). Nenhum `System.out`.

### 5.13 Processo, git e documentação

- **Um commit por fase, com tag** `fase-0-baseline` … `fase-6-ci-observabilidade`. Mensagens em português, estilo Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`). Permite `git diff fase-2-seguranca fase-3-dominio-ordens` em sala.
- **ADRs** em `docs/adr/NNNN-titulo.md` (formato Nygard: contexto, decisão, consequências). Mínimo: 0001 stack e versões; 0002 soft delete; 0003 JWT nativo vs jjwt; 0004 ProblemDetail; 0005 Testcontainers em vez de H2; 0006 máquina de estados da ordem; 0007 admin via variável de ambiente.
- `README.md`: o que é, arquitetura (diagrama C4 nível 2 em Mermaid), como rodar (local e Compose), variáveis, como testar, como a documentação é acessada, mapa de tags por aula, limitações conhecidas.
- `CLAUDE.md` atualizado ao fim de cada fase (é a "memória" do agente; serve de exemplo de contexto para programação assistida por IA).
- `docs/postman/servicehub.postman_collection.json` com o fluxo ponta a ponta, usando variável de ambiente para o token.

## 6. Fases de entrega

Cada fase termina com `mvnw verify` verde, commit, tag, ADRs e `CLAUDE.md` atualizados. Ordem pensada para **nunca escrever código descartável** (por isso segurança vem antes das ordens de serviço, que dependem do usuário autenticado).

| Fase | Conteúdo | Aulas | Tag |
| --- | --- | --- | --- |
| 0 | Commit da baseline atual; remover `V6` solto; Java 21; `.gitignore` (+`.env`); README e `.env.example` iniciais; ADR-0001 | 3–5 | `fase-0-baseline` |
| 1 | Prefixo `/api/v1`; `ProblemDetail` + exceções de domínio; Bean Validation; paginação e filtros em `/services`; `X-Request-Id`; OpenAPI com erros reais; base de testes (fatia web + Testcontainers) e `contextLoads` sem Postgres externo | 2, 5, 6, 8 | `fase-1-contrato-qualidade` |
| 2 | V8 `role`; Spring Security + BCrypt; registro/login/me com JWT; `@PreAuthorize` nos services; BOLA em `users` e `services`; CORS; sanitização; Swagger Authorize; admin via env; soft delete; testes de segurança | 8, 9 | `fase-2-seguranca` |
| 3 | Entidades `ServiceRequest` e `Review`; máquina de estados; endpoints de ordens e avaliações; métrica de transições; testes unitários e de integração do fluxo completo | 4 (lição de casa), 9 | `fase-3-dominio-ordens` |
| 4 | Completar a pirâmide: cobertura das regras de negócio, testes de 401 em massa, JaCoCo; coleção Postman | 10 | `fase-4-testes` |
| 5 | Dockerfile multi-stage, `.dockerignore`, Compose com healthcheck/`.env`/perfis, override dev, perfil `prod` com logs JSON | 11, 12 | `fase-5-docker` |
| 6 | GitHub Actions; Actuator/Prometheus revisados; README final com C4 e mapa de tags; `CLAUDE.md` final | 14, 16 | `fase-6-ci-observabilidade` |
| 7 (opcional) | GraphQL somente leitura (`providers`, `services`) com Spring for GraphQL + GraphiQL; rate limiting no login com Bucket4j | 9 (entrega 2), 8 | `fase-7-extras` |

Estimativa de tamanho: Fases 1–3 são as maiores; cada uma rende um plano de implementação próprio.

## 7. O que fica a critério de quem constrói

- Estrutura interna de pacotes além de `controller/service/repository/model/dto/exception/config/security` (ex.: `mapper/`).
- Usar MapStruct ou mapeamento manual (recomendo manual: é o que os slides mostram e evita mais um processador de anotações ao lado do Lombok).
- `Specification` vs JPQL para os filtros de `/services`.
- Nomes exatos de classes de teste, fixtures e builders.
- Formato do `Page` serializado, desde que estável e documentado no OpenAPI.
- Versão exata das imagens base (Postgres 16 vs 15 do slide; recomendo 16-alpine), desde que `pg_isready` exista.
- Detalhes do filtro de `X-Request-Id`.

## 8. Decisões que dependem do professor

Levantadas por mim e pela revisão independente de um construtor. Respondidas pelo professor em 2026-10-22; o restante do documento já reflete as respostas.

1. **JWT:** nativo do Spring Security (`oauth2-resource-server`). O ADR-0003 documenta a equivalência com o `jjwt` + filtro da Aula 9. ✅ decidido
2. **Java:** 21 LTS. ✅ decidido
3. **Soft delete** para usuários e serviços, com 409 enquanto houver ordens ativas. ✅ assumido (recomendação não contestada)
4. **PROVIDER também contrata** serviços de outros prestadores; ADMIN altera papel via `PATCH /users/{id}/role`. ✅ decidido
5. **Repositório GitHub:** o professor cria e informa a URL antes da Fase 6; a Fase 6 fecha com o pipeline executando de verdade. Até lá, o remoto pode ficar vazio. ✅ decidido
6. **Fase 7 (GraphQL + rate limiting):** só depois da Fase 6 concluída e aprovada; não entra no primeiro ciclo. ✅ assumido

## Apêndice A — Trechos dos slides usados como critério de pronto

Os slides originais estão em `E:\Dados\OneDrive\UniEvangélica\2026-2\01-DESENVOLVIMENTO DE API BACKEND\Slides` (.pptx). O texto extraído de cada aula está em `docs/ensino/slides-extrato/` (ignorado pelo git por padrão). Os trechos citados diretamente neste design:

**Senha (Aula 8, slide 9):** mínimo 8 caracteres e `^(?=.*[A-Z])(?=.*\d).+$` (ao menos uma maiúscula e um dígito). Limite superior 72 é meu (limite do BCrypt).

**Checklist de documentação (Aula 5, slide 23):** Bean OpenAPI com título, versão e contato; paths do Swagger UI e api-docs personalizados; autenticação JWT configurada no Swagger UI; grupos de API definidos; todos os controllers com `@Tag`; todos os endpoints com `@Operation` (summary + description); todos os `@ApiResponse` documentados (sucesso e erros); todos os parâmetros com `@Parameter`; DTOs de request e response com `@Schema`; classe de erro padronizada e documentada; exemplos reais em todos os campos; descrições em português claro; endpoints depreciados marcados; documentação validada no Swagger UI local.

**Auditoria de qualidade (Aula 6, slide 23):** separação Controller/Service/Repository; nenhuma lógica de negócio no Controller; `@ControllerAdvice`; URIs com substantivos no plural; métodos HTTP corretos; status codes corretos; JSON consistente; todos os endpoints no Swagger com summaries, descriptions, `@Schema` e exemplos.

**Header de rastreio (Aula 6, slide 11):** `X-Request-Id: identificador único da requisição para rastreamento`.

**Sanitização (Aula 8, slides 12–13):** `StringEscapeUtils.escapeHtml4` (Apache Commons Text) na camada de serviço; allowlists; limite de tamanho de payload via `application.properties`; consultas sempre parametrizadas.

**Dockerfile de referência (Aula 11, slide 24):** estágio `builder` `maven:3.9-eclipse-temurin-17` com `COPY pom.xml` + `mvn dependency:go-offline -q`, depois `COPY src` + `mvn package -DskipTests -q`; estágio final `eclipse-temurin:17-jre-alpine`, `addgroup -S spring && adduser -S spring -G spring`, `USER spring`, `COPY --from=builder /app/target/*.jar app.jar`, `EXPOSE 8080`, `ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]`. Neste projeto, 17 → 21.

**Compose de referência (Aula 12, slide 21):** `api` com `build: .`, `env_file: .env`, `depends_on: postgres: condition: service_healthy`, `restart: unless-stopped`; `postgres` com `healthcheck: pg_isready -U ${DB_USER} -d ${DB_NAME}` (`interval: 10s`, `timeout: 5s`, `retries: 5`), volume `pg_data`; rede `bridge`.

**Testes (Aula 10, slides 20–21):** nomes `deve[Comportamento]Quando[Condição]`, `@DisplayName`, padrão Arrange/Act/Assert, testes independentes.

**Roles (Aula 9, slide 11):** `ROLE_ADMIN`, `ROLE_PROVIDER`, `ROLE_CLIENT`.
