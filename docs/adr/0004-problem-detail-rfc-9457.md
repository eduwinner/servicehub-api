# ADR-0004 — Erros no formato ProblemDetail (RFC 9457)

**Status:** aceito — 2026-10-22

## Contexto

O projeto base lançava `RuntimeException` para "usuário não encontrado" e "e-mail já cadastrado", e o cliente recebia um HTTP 500 genérico. A Aula 2 pede "respostas de erro consistentes, com status HTTP correto e payload descritivo"; a Aula 8 mostra um `@RestControllerAdvice` que monta um `Map` artesanal (`timestamp`, `status`, `erros`). Um formato inventado por projeto obriga cada cliente a aprender um contrato novo.

## Decisão

Todas as respostas de erro usam **`ProblemDetail`** (RFC 9457, `application/problem+json`), suportado nativamente pelo Spring Framework 6+/7:

- `GlobalExceptionHandler extends ResponseEntityExceptionHandler` traduz exceções de domínio (`ResourceNotFoundException` → 404, `EmailAlreadyExistsException` e `BusinessRuleException` → 409) e erros de entrada (`MethodArgumentNotValidException`, `HttpMessageNotReadableException` → 400).
- `type` é um URN estável (`urn:servicehub:problem:<tipo>`, em `ProblemTypes`); clientes devem decidir pelo `type`, nunca pelo texto de `detail`.
- Extensões: `timestamp` em todos; `errors: [{field, message}]` nos erros de validação; enum inválido lista os valores aceitos em `detail`.
- Qualquer outra exceção vira 500 com `detail` fixo ("Informe o X-Request-Id ao suporte"); o erro real vai só para o log, correlacionado pelo `X-Request-Id`.
- `server.error.include-stacktrace=never` e `include-message=never`.

## Consequências

- Um único contrato de erro, documentado no OpenAPI com `schema = ProblemDetail`.
- Mensagens de erro em português ficam no `detail`/`errors`; o `type` é independente de idioma.
- A Aula 8 pode mostrar o `Map` artesanal como passo intermediário e o `ProblemDetail` como padrão de mercado.
- As Fases 2 e 3 acrescentam `unauthorized`, `forbidden` e `invalid-transition` ao mesmo esquema.
