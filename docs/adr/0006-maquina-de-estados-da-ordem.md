# ADR-0006 — Máquina de estados da ordem de serviço

**Status:** aceito — 2026-10-22

## Contexto

A migração V5 (Aula 4) criou `service_requests.status` com um `CHECK` de cinco valores, mas nenhum slide define **quem** pode levar a ordem de qual estado para qual. Sem essa regra, qualquer participante poderia "concluir" uma ordem recém-aberta, ou um cliente poderia aceitar a própria contratação. É a regra de negócio central do ServiceHub e o melhor exemplo, para a turma, de conhecimento que a IA não tem e o engenheiro precisa especificar.

## Decisão

A tabela de transições vive no enum `model/RequestStatus`, como dado, e não espalhada em `if`s:

| De | Para | Quem |
|---|---|---|
| PENDING | ACCEPTED | prestador dono do serviço |
| PENDING | CANCELLED | cliente da ordem ou prestador |
| ACCEPTED | IN_PROGRESS | prestador |
| ACCEPTED | CANCELLED | cliente ou prestador |
| IN_PROGRESS | COMPLETED | prestador |

- ADMIN executa qualquer transição **válida**; nunca uma inválida.
- Transição fora da tabela: **409** `urn:servicehub:problem:invalid-transition` ("Não é possível mudar de X para Y.").
- Transição válida, mas não permitida ao papel de quem chama: **403**. Quem não participa da ordem recebe 403 antes de qualquer análise.
- `COMPLETED` e `CANCELLED` são terminais. Ordens `PENDING`, `ACCEPTED` e `IN_PROGRESS` são "ativas" e bloqueiam a exclusão do serviço e dos usuários envolvidos (RN10).
- Cada transição incrementa a métrica `servicehub.service_requests.transitions{to=...}`.

## Consequências

- `RequestStatusTest` cobre as 25 combinações (5 × 5) para os três papéis: a regra inteira é verificável em milissegundos, sem banco.
- Mudar a regra (por exemplo, permitir que o cliente cancele em `IN_PROGRESS`) é uma linha na tabela e uma linha no teste.
- 409 versus 403 separa "o pedido não faz sentido no estado atual" de "você não pode pedir isso": o cliente da API sabe se deve corrigir o fluxo ou o usuário.
