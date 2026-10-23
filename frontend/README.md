# Bancada ServiceHub

Ferramenta do professor para validar, ao vivo e em sala, a API ServiceHub de cada equipe. Ela executa o ciclo completo do **Mapa de Construção** e confere status codes e campos do contrato em 15 passos:

| Grupo | Passos |
|---|---|
| Conexão | API no ar (`GET /api/v1/info`) |
| Cadastro e login | cadastrar prestador, cadastrar cliente, login de cada um |
| Ciclo do serviço | prestador publica serviço → cliente contrata (PENDING) → prestador aceita → inicia → conclui → cliente avalia → avaliações públicas |
| Contrato de segurança e erros | sem token = 401, cliente publicando serviço = 403, ordem concluída voltando a PENDING = 409 (todos em ProblemDetail) |

## Pré-requisitos

- Node.js 18 ou superior.
- A API da equipe rodando (`docker compose up -d --build` no projeto do aluno).
- **CORS liberado para `http://localhost:5173`** na API do aluno (exigência do Mapa, seções 5 e 12). Sem isso, o navegador bloqueia as chamadas e a bancada mostra "Sem resposta".

## Uso manual (tela no projetor)

```bash
cd frontend
npm run bancada          # abre em http://localhost:5173
```

Informe a equipe e a URL da API (padrão `http://localhost:8090`), escolha o ritmo **Apresentação** e clique em **Executar ciclo completo**. Clique em qualquer passo para ver a requisição e a resposta. **Copiar relatório** gera um resumo em texto para colar no AVA ou na planilha de notas.

Atalhos pela URL: `http://localhost:5173/?api=http://localhost:8090&equipe=Grupo%203&ritmo=rapido`.

## Uso com Playwright (automático, ao vivo)

Instale uma vez:

```bash
cd frontend
npm install
npx playwright install chromium   # só se o navegador ainda não estiver instalado
```

Rodar contra a API de uma equipe (PowerShell):

```powershell
$env:API_URL = "http://localhost:8090"; $env:EQUIPE = "Grupo 3"
npm run aovivo      # navegador visível, ritmo de apresentação
npm run teste       # sem janela, rápido (conferência)
npm run relatorio   # abre o relatório HTML com cada passo e os anexos das falhas
```

No Git Bash ou Linux: `API_URL=http://localhost:8090 EQUIPE="Grupo 3" npm run aovivo`.

Cada passo vira uma etapa no relatório do Playwright; quando algo falha, a etapa traz anexada a requisição, a resposta e o motivo (por exemplo, "Esperado 201, recebido 400" ou "o erro deveria vir como application/problem+json").

## Como os dados de teste são gerados

Cada execução cria um prestador e um cliente novos com e-mails únicos (`prestador.<marca-de-tempo>@bancada.servicehub`), senha `Senha123`. Por isso o ciclo pode ser repetido quantas vezes for preciso contra o mesmo banco.
