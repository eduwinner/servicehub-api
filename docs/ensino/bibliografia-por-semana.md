# Bibliografia por semana — Desenvolvimento de API Back-End

Levantamento feito na Minha Biblioteca (acervo UniEVANGÉLICA) em 23/10/2026, a partir dos sumários dos livros.
Base para as leituras indicadas em cada capítulo da apostila. Capítulos conferidos no sumário de cada obra.

## Obras

| Sigla | Obra | ISBN | Situação |
|---|---|---|---|
| SCH | SCHILDT, H. *Java para iniciantes*. 6. ed. Bookman. | 9788582603376 | Básica (mantida) |
| HOR | HORSTMANN, C. *Conceitos de computação com Java*. Bookman. | 9788577804078 | Básica (mantida) |
| INT | RODRIGUES, T. N. et al. *Integração de aplicações*. Grupo A, 2020. | 9786556900216 | **Nova (proposta para a básica)** |
| ALV | ALVES, W. P. *Java para Web: desenvolvimento de aplicações*. Saraiva. | 9788536519357 | Básica atual; proposta: complementar (JSP/Servlet, MySQL) |
| FER | FERREIRA, A. G. *Interface de programação de aplicação (API) e Web Services*. Saraiva, 2021. | 9786553560338 | **Não encontrado no acervo** (ISBN, título e autor) |
| ARQ | MARTIN, R. C. *Arquitetura limpa*. Alta Books, 2019. | 9788550808161 | Complementar (mantida) |
| CL | MARTIN, R. C. *Código limpo*. Alta Books, 2009. | 9788550816043 | Complementar (mantida) |
| AGL | MARTIN, R. C. *Desenvolvimento ágil limpo*. Alta Books, 2020. | 9788550816890 | Complementar (mantida) |
| FOW | FOWLER, M. *Padrões de arquitetura de aplicações corporativas*. Bookman, 2006. | 9788577800643 | Nova |
| DDD | KHONONOV, V. *Aprenda Domain-Driven Design*. Alta Books, 2024. | 9788550819686 | Nova |
| FAS | RICHARDS, M.; FORD, N. *Fundamentos da arquitetura de software*. Alta Books, 2024. | 9788550819754 | Nova |
| PRE | PRESSMAN, R. S.; MAXIM, B. R. *Engenharia de software*. 9. ed. Grupo A, 2021. | 9786558040118 | Nova |
| TST | GONÇALVES, P. F. et al. *Testes de software e gerência de configuração*. Grupo A, 2019. | 9788595029361 | Nova |
| DEV | MONTEIRO, E. R. et al. *DevOps*. Grupo A, 2021. | 9786556901725 | Nova |
| DS3 | MACHADO, R. P.; FRANCO, M. H. I.; BERTAGNOLLI, S. C. *Desenvolvimento de software III: programação de sistemas web orientada a objetos em Java*. Bookman (Tekne), 2016. | 9788582603710 | Nova |

Temas sem livro no acervo (usar documentação oficial e RFCs): Spring Boot 4, Flyway, OpenAPI/Springdoc, JWT (RFC 7519), ProblemDetail (RFC 9457), Testcontainers, Docker Compose, GitHub Actions, OWASP API Security Top 10.

Integração contínua: tratamento introdutório (o tema tem disciplina própria no 7º período).

## Leituras por semana

| Sem. | Tema | Leituras |
|---|---|---|
| 1 | APIs, ecossistema Java, método com IA | INT: "Tecnologias de integração entre sistemas e aplicações" (API, REST e RESTful, JSON) e "SOAP" · SCH cap. 1 · HOR cap. 1 · FAS cap. 1 |
| 2 | HTTP, REST, modelagem do domínio | INT: "Geração de serviços com REST", "Serviços web" · DDD caps. 1–3 · PRE caps. 7–8 |
| 3 | Spring Boot, setup, Git | SCH caps. 8 e 12 (pacotes, interfaces, anotações) · FOW cap. 1 · ARQ caps. 22 e 32 · DEV: "Tipos de controles de versão", "Criação de branches e commits com o Git" · CL caps. 2–3 |
| 4 | JPA, Flyway, erros padronizados | INT: "Servidores de aplicação JPA" · DS3 cap. 4 · FOW caps. 3 e 10 · SCH cap. 9 · CL cap. 7 |
| 5 | Contrato com OpenAPI | INT: "Software de apoio" (Postman), "Conceito e uso de web service e APIs" · FOW cap. 15 (Data Transfer Object) · docs Springdoc |
| 6 | Checkpoint e revisão (1ª VA) | Revisão das leituras 1–5 |
| 8 | Segurança: validação, senhas, OWASP | PRE cap. 18 · INT: "Segurança em webservices" · ARQ cap. 20 · OWASP API Top 10 |
| 9 | JWT e autorização | INT: "Segurança em webservices" · PRE cap. 18 · RFC 7519 · docs Spring Security |
| 10 | Testes | TST: "Níveis de teste", "Testes unitários", "Teste de software com JUnit" · CL cap. 9 · PRE caps. 19–20 · ARQ cap. 28 · AGL cap. 5 |
| 11 | Containers e Dockerfile | DEV: "Configuração e manutenção de contêineres usando o Docker" |
| 12 | Docker Compose e ambientes | DEV: "Uso do Docker para microsserviços", "Gerenciando o ciclo de vida de contêineres" |
| 14 | CI (introdução), deploy, versionamento | DEV: "Integração e entrega contínuas" (introdutório) · PRE cap. 22 |
| 15 | Governança e observabilidade | FAS caps. 4–6 e 19 (decisões de arquitetura) · ARQ cap. 15 |
| 16–18 | Checkpoint final e banca | FAS cap. 21 (diagramando e apresentando a arquitetura) · AGL cap. 7 |
