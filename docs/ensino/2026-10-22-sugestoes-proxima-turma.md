# Sugestões para a próxima turma — Desenvolvimento de API Back-End

**Para:** Prof. Eduardo Dias Pereira
**Base:** leitura integral dos slides das Aulas 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 14 e 16, do Plano de Ensino 2026/2, do guia de apresentação final e das APS 1–16; e do estado do repositório `servicehub-api`.
**Data:** 2026-10-22

A disciplina já cobre, na ordem certa, a trilha HTTP → REST → Spring Boot → JPA/Flyway → OpenAPI → Segurança → JWT → Testes → Docker → Compose → CI/CD. O que segue são lacunas e atritos que vi ao tentar construir a API "seguindo o norte de cada aula", organizados por impacto na formação de um engenheiro de software que vai programar com IA.

---

## A. Lacunas de formação (o que ficou de fora)

### A1. Modelagem de domínio e regras de negócio antes do CRUD
A Aula 2 pede um diagrama de endpoints e a Aula 4 cria tabelas, mas em nenhum momento o aluno modela **o que o sistema faz**: quem pode abrir uma ordem, quais transições de status existem, o que é invariante (preço congelado na ordem, uma avaliação por ordem). A `service_requests` tem um `CHECK` com cinco estados e nenhum slide desenha a máquina de estados. Na programação assistida por IA, isso é exatamente o que o engenheiro precisa saber dizer ao agente; o CRUD a IA gera sozinha.

**Sugestão:** meia aula entre a 2 e a 4 (ou dentro da 4) com diagrama de estados da ordem de serviço, tabela "quem pode fazer o quê" e lista de invariantes. Entregável: `docs/dominio.md` do projeto. Isso também dá base para os testes unitários da Aula 10, que hoje testam exemplos de "Produto" sem relação com o ServiceHub.

### A2. Git e GitHub como prática, não como ferramenta citada
A Aula 1 declara "GitHub Flow com branches por feature e pull requests" obrigatório, e a Aula 14 usa GitHub Actions, mas não há aula de Git: branch, commit atômico, mensagem de commit, PR, revisão, tag/release (a Aula 16 pede "tag ou release"). O aluno chega na banca sem saber marcar uma versão.

**Sugestão:** 1h prática na Semana 3 (junto do setup) com fluxo mínimo: `main` protegida, branch por entrega, PR com template, Conventional Commits, tag por checkpoint. Avaliar o histórico do repositório como parte da nota do PI (ex.: "cada APS prática é um PR").

### A3. Tratamento de erros e contrato de erro
`@ControllerAdvice` aparece como item de checklist na Aula 6 e como exemplo de validação na Aula 8, mas nunca como tema. O projeto-base responde 500 para "usuário não encontrado". Um contrato de erro consistente é parte do design da API (Aula 2, slide 14) e da governança (Aula 5).

**Sugestão:** ensinar `ProblemDetail` (RFC 9457), nativo do Spring, logo após o CRUD (Aula 4 ou início da 5), mapeando exceções de domínio para 400/404/409. Fica pronto para o Swagger documentar os erros reais em vez de "500 sem handler".

### A4. DTOs, mapeamento e exposição de dados desde a Aula 4
"Nunca exponha entidades JPA" só aparece na Aula 8 (OWASP API3), mas o controller da Aula 4 retorna `User` com `passwordHash`. O aluno aprende o antipadrão primeiro e a correção quatro semanas depois.

**Sugestão:** separar DTO de request e de response já na Aula 4, com `record` Java. Ganha-se também o gancho para *mass assignment* (não aceitar `role` ou `providerId` do body).

### A5. Paginação, filtros e ordenação implementados
A Aula 2 explica query params e a Aula 8 diz "nunca permita listagens sem paginação", mas a Aula 4 implementa `findAll()` sem `Pageable`. Nenhum endpoint do projeto-base pagina.

**Sugestão:** `Pageable` e `Page<DTO>` como parte do CRUD inicial, com `size` máximo; filtros com `Specification` ou JPQL parametrizado.

### A6. Testes mais cedo e com o banco certo
Testes chegam na Semana 10, depois de segurança. Até lá, o único teste é `contextLoads`, que exige Postgres vivo. Além disso, a Aula 10 recomenda H2 para integração, mas as migrações da Aula 4 usam `BIGSERIAL`, `NUMERIC`, `CHECK` e `TEXT` do Postgres: com H2 o Flyway quebra ou o aluno desliga o Flyway nos testes, perdendo o que a Aula 4 ensinou.

**Sugestão:** um `@WebMvcTest` já na Semana 4 (status code e JSON do CRUD) e Testcontainers como caminho principal da Aula 10, com H2 citado apenas como alternativa com ressalvas. No mundo da IA, **o teste é o critério de aceite que o engenheiro escreve e a IA tem de satisfazer**; esse é o argumento central da sua missão.

### A7. Registro de decisões (ADR) e documentação além do Swagger
A disciplina documenta o contrato (OpenAPI) mas não as decisões. O guia da banca pede "registro de limitações e decisões importantes" e "decisão técnica em três movimentos (problema, decisão, efeito)": isso é um ADR.

**Sugestão:** adotar ADRs curtos (formato Nygard) como entregável desde a Semana 3. O `CLAUDE.md`/`AGENTS.md` do projeto é um ADR vivo para o agente de IA; mostrar isso em sala materializa a ideia de que o engenheiro gerencia contexto e processo.

### A8. Uma trilha explícita de programação assistida por IA
A sua missão declarada (IA escreve sintaxe, engenheiro domina arquitetura e processo) não aparece em nenhum slide. Os alunos vão usar IA de qualquer forma; sem método, usam mal.

**Sugestão:** um fio condutor de 15 minutos por aula, com quatro movimentos: (1) **especificar** (contrato OpenAPI, ADR, teste de aceite) antes de pedir código; (2) **gerar** com o agente dentro de um contexto de projeto (`CLAUDE.md`); (3) **revisar** com checklist da aula (status codes, camadas, OWASP); (4) **provar** com testes e demo. Avaliação: o aluno entrega o prompt/spec e o teste junto com o código. A Aula 6 (checkpoint) e a 16 (banca) já têm checklists prontos para isso.

### A9. Observabilidade como hábito, não como tópico da Semana 14
O `spring-boot-starter-actuator` está no `pom.xml` desde a Aula 4 e nunca é usado. A Semana 14 acumula deploy, versionamento, CI/CD, Prometheus e logs em uma aula de 15 slides.

**Sugestão:** usar `/actuator/health` na Aula 4, ligá-lo ao `HEALTHCHECK` do Dockerfile na 11 e ao `depends_on: service_healthy` na 12; deixar a 14 para CI/CD e versionamento, e a 15 (governança) para métricas e logs estruturados. Logs estruturados JSON são configuração nativa do Boot (`logging.structured.format.console=ecs`), cabe em um slide.

### A10. Transações, concorrência e exclusão de dados
`@Transactional` aparece em todos os services sem ser explicado. O `ON DELETE CASCADE` da V4 faz um `DELETE /users/{id}` apagar serviços, ordens e avaliações de terceiros; a coluna `is_active` existe e não é usada. Vale uma discussão de *soft delete* vs *hard delete* e de LGPD (direito ao esquecimento vs histórico contratual), que a Aula 3 menciona de passagem.

### A11. Front-end e CORS
A Aula 16 pede "Integração Front-End" na demo, mas nenhuma aula entrega um cliente. A Aula 8 configura CORS sem um consumidor para testar.

**Sugestão:** fornecer uma página HTML mínima (ou coleção Postman oficial) que consuma login + listagem, para que CORS e Bearer tenham onde ser vistos.

---

## B. Inconsistências entre slides que confundem quem segue o norte

| Onde | Problema | Ajuste |
| --- | --- | --- |
| Aula 1 × Aula 4 × Plano de Ensino | Domínio diferente em cada fonte: `providers/clients/contracts/reviews` (A1), `users/services/service_requests/reviews` (A4), "usuários, clientes, serviços e ordens de serviço" (Plano). A Aula 9 ainda usa um `Provider` e um `Booking` que não existem no schema | Unificar no modelo da Aula 4 (é o que está migrado) e corrigir A1, A9 e Plano |
| Aula 3 slide 7 × Aula 4 slide 7 | A3 manda gerar Spring Boot 3.2.x; o `pom.xml` da A4 usa starters que só existem no Boot 4 (`spring-boot-starter-webmvc`, `-data-jpa-test`, `-webmvc-test`) | Declarar Boot 4.x na A3 e explicar a divisão de starters do Boot 4 (já está no `CLAUDE.md` do projeto) |
| Aula 5 slide 6 | `springdoc 2.5.0` não funciona com Boot 4; precisa de `3.0.x` | Atualizar |
| Aula 6 slide 20 | Mostra H2 com `ddl-auto=create-drop`/`update`, contradizendo a A4 (`validate` + Flyway) | Remover H2 da revisão ou marcar como "não use no ServiceHub" |
| Aula 4 slide 9 | Pacote `br.com.servicehub.model`; o projeto é `com.servicehub` | Corrigir |
| Aula 4 slide 9 | `@Column(name = " password_hash")` com espaço no nome | Corrigir |
| Aula 4 slide 22 | Chama `hashPassword(...)` e `EmailAlreadyExistsException` que não são definidos; o projeto-base acabou gravando senha em texto puro | Mostrar BCrypt já na A4 ou deixar explícito "hash entra na A8" com `TODO` |
| Aula 2 slide 25 | "Próxima aula: JWT, OAuth 2.0, RBAC"; a próxima é Spring Boot setup | Corrigir |
| Aula 8 slide 7 | `user.getPassword()`; a entidade tem `passwordHash` | Corrigir |
| Aula 9 slide 12 | API do `jjwt` 0.11 (`setClaims`, `signWith(key, SignatureAlgorithm)`) está depreciada no 0.12; e o Spring Security já traz JWT nativo (`oauth2-resource-server`) que dispensa o filtro manual | Escolher um caminho e atualizar o código; a implementação de referência usa o nativo e documenta a equivalência |
| Aula 10 slide 14 | `@MockBean` foi removido no Boot 4; usar `@MockitoBean` | Atualizar |
| Aula 10 slide 16 | `@AutoConfigureTestDatabase` troca o datasource por embutido e quebra as migrações Postgres | Trocar por Testcontainers + `@ServiceConnection` |
| Aula 11 slides 10–24 | JDK 17 nas imagens; o projeto pede 21 | Atualizar para `eclipse-temurin:21-jre-alpine` |
| Aula 12 slide 5 | `version: '3.9'` é obsoleto no Compose v2 | Remover a chave |
| Aula 12 slide 13 | Usa `${DB_HOST}` no `application.properties` sem default; local, sem `.env`, a app não sobe | Mostrar `${DB_HOST:localhost}` |
| Aula 14 slide 6 | `actions/checkout@v3` e `setup-java@v3`; usar `@v5` (como no ci.yml) | Atualizar |
| Aula 4 slide 19 | `CREATE INDEX idx_users_email` redundante com `UNIQUE` (o projeto precisou de uma V6 só para remover) | Tirar do slide, ou manter como exemplo de "migração corretiva" |
| Aulas 7, 13 e 15 | Não há slides (7 e 13 são provas; a 15, Governança, não existe na pasta) | Criar a 15; ela é a casa natural de métricas, logs, ADR e versionamento |

---

## C. Avaliação

- **APS:** várias alternativas erradas são anedóticas ("calculadoras científicas", "baixar músicas pela porta 8080"). Elas não medem nada: o aluno elimina sem saber o conteúdo. Vale trocar por distratores plausíveis (ex.: "EXPOSE publica a porta no host" é um erro real e comum).
- **Checkpoint (Aula 6) e banca (Aula 16):** os checklists são bons. Sugiro acrescentar três itens que a IA não faz pelo aluno: (1) explicar uma decisão com ADR; (2) mostrar um teste que falha quando a regra de negócio é quebrada; (3) mostrar o diff de uma correção de revisão (PR).
- **Nota Livre:** reservar parte dela para "qualidade do processo" (histórico git, PRs, ADRs, `CLAUDE.md`).

---

## D. Proposta de cronograma ajustado (20 semanas, mesma carga)

| Sem. | Mudança em relação a 2026/2 |
| --- | --- |
| 1 | Igual + apresentar o método de trabalho com IA (A8) |
| 2 | HTTP/REST + **modelagem de domínio** (estados, invariantes, papéis) → entrega: diagrama de endpoints **e** de estados |
| 3 | Setup Spring Boot 4 + **Git/GitHub Flow** + `CLAUDE.md`/ADR-0001 |
| 4 | JPA/Flyway + DTOs + paginação + `ProblemDetail` + `/actuator/health` + primeiro `@WebMvcTest` |
| 5 | OpenAPI (igual), agora documentando erros reais |
| 6 | Checkpoint (igual) |
| 7 | 1ª V.A. |
| 8 | Spring Security + validação + BCrypt (igual) |
| 9 | JWT (nativo do Spring Security) + roles + BOLA. GraphQL/gRPC vira optativa da Nota Livre |
| 10 | Testes com Testcontainers; pirâmide completa; JaCoCo |
| 11 | Docker I (igual, JDK 21, healthcheck) |
| 12 | Docker II (igual, `.env`, perfis, `service_healthy`) |
| 13 | 2ª V.A. |
| 14 | CI/CD + versionamento + deploy (Railway/Render) |
| 15 | **Governança e observabilidade**: métricas, logs JSON, ADRs, deprecação, API Gateway (conceitual) |
| 16 | Checkpoint final (igual) |
| 17–20 | Igual |

---

## E. Como a implementação de referência apoia isso

O projeto em `c:\xampp\htdocs\servicehub-api` será evoluído em fases com uma tag git por fase (`fase-0-baseline` … `fase-6-ci-observabilidade`), ADRs em `docs/adr/` e `CLAUDE.md` atualizado. Em sala, `git diff fase-1-contrato-qualidade fase-2-seguranca` mostra exatamente o que "adicionar segurança" significa em código, e os ADRs mostram por que cada escolha foi feita. O documento de design está em `docs/superpowers/specs/2026-10-22-servicehub-api-design.md`.
