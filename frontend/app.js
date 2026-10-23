/* Bancada ServiceHub — executa o ciclo completo do Mapa de Construção contra a API de um aluno.
 * Sem dependências. Cada passo valida o status code e campos-chave do contrato. */
'use strict';

const PASSWORD = 'Senha123';
const TIMEOUT_MS = 10000;

/** Estado compartilhado entre os passos (ids, tokens, e-mails da execução atual). */
let ctx = {};

const ACTORS = { public: 'Público', provider: 'Prestador', client: 'Cliente' };

/**
 * Cada passo: método, rota, corpo, token, status esperado e verificações do contrato.
 * check(body, res) devolve null quando passa, ou um texto explicando o que faltou.
 */
const STEPS = [
  {
    id: 'info', group: 'Conexão', title: 'API no ar', actor: 'public',
    method: 'GET', path: () => '/api/v1/info', expect: 200,
    check: (b) => b && b.status === 'OK' ? null : 'o campo "status" deveria ser "OK"',
  },
  {
    id: 'register-provider', group: 'Cadastro e login', title: 'Cadastrar prestador', actor: 'public',
    method: 'POST', path: () => '/api/v1/auth/register', expect: 201,
    body: () => ({ fullName: 'João Prestador', email: ctx.providerEmail, password: PASSWORD, role: 'PROVIDER' }),
    check: (b, res) => noPassword(res) || requireField(b, 'id') || (b.role === 'PROVIDER' ? null : 'o papel retornado deveria ser PROVIDER'),
    save: (b) => { ctx.providerId = b.id; },
  },
  {
    id: 'register-client', group: 'Cadastro e login', title: 'Cadastrar cliente', actor: 'public',
    method: 'POST', path: () => '/api/v1/auth/register', expect: 201,
    body: () => ({ fullName: 'Maria Cliente', email: ctx.clientEmail, password: PASSWORD, role: 'CLIENT' }),
    check: (b, res) => noPassword(res) || requireField(b, 'id'),
    save: (b) => { ctx.clientId = b.id; },
  },
  {
    id: 'login-provider', group: 'Cadastro e login', title: 'Login do prestador', actor: 'public',
    method: 'POST', path: () => '/api/v1/auth/login', expect: 200,
    body: () => ({ email: ctx.providerEmail, password: PASSWORD }),
    check: (b) => requireField(b, 'accessToken'),
    save: (b) => { ctx.providerToken = b.accessToken; },
  },
  {
    id: 'login-client', group: 'Cadastro e login', title: 'Login do cliente', actor: 'public',
    method: 'POST', path: () => '/api/v1/auth/login', expect: 200,
    body: () => ({ email: ctx.clientEmail, password: PASSWORD }),
    check: (b) => requireField(b, 'accessToken'),
    save: (b) => { ctx.clientToken = b.accessToken; },
  },
  {
    id: 'create-service', group: 'Ciclo do serviço', title: 'Prestador publica serviço', actor: 'provider',
    method: 'POST', path: () => '/api/v1/services', expect: 201, needs: ['providerToken'],
    body: () => ({ title: 'Instalação elétrica residencial', description: 'Tomadas, disjuntores e fiação',
      price: 150.0, category: 'ELETRICA' }),
    check: (b) => requireField(b, 'id') ||
      (ctx.providerId == null || Number(b.providerId) === Number(ctx.providerId)
        ? null : 'providerId deveria ser o do prestador autenticado (vem do token)'),
    save: (b) => { ctx.serviceId = b.id; },
  },
  {
    id: 'create-request', group: 'Ciclo do serviço', title: 'Cliente contrata o serviço', actor: 'client',
    method: 'POST', path: () => '/api/v1/service-requests', expect: 201, needs: ['clientToken', 'serviceId'],
    body: () => ({ serviceId: ctx.serviceId, scheduledAt: futureDate(), notes: 'Apartamento 302' }),
    check: (b) => requireField(b, 'id') || expectStatus(b, 'PENDING') ||
      (Number(b.totalPrice) === 150 ? null : 'totalPrice deveria copiar o preço do serviço (150.00)'),
    save: (b) => { ctx.requestId = b.id; },
  },
  statusStep('accept', 'Prestador aceita', 'ACCEPTED'),
  statusStep('start', 'Prestador inicia a execução', 'IN_PROGRESS'),
  statusStep('complete', 'Prestador conclui', 'COMPLETED'),
  {
    id: 'review', group: 'Ciclo do serviço', title: 'Cliente avalia', actor: 'client',
    method: 'POST', path: () => `/api/v1/service-requests/${ctx.requestId}/reviews`, expect: 201,
    needs: ['clientToken', 'requestId'],
    body: () => ({ rating: 5, comment: 'Pontual e caprichoso.' }),
    check: (b) => requireField(b, 'id') || (Number(b.rating) === 5 ? null : 'a nota retornada deveria ser 5'),
  },
  {
    id: 'reviews-public', group: 'Ciclo do serviço', title: 'Avaliações do serviço (público)', actor: 'public',
    method: 'GET', path: () => `/api/v1/services/${ctx.serviceId}/reviews`, expect: 200, needs: ['serviceId'],
    check: (b) => b && Array.isArray(b.content) && b.content.length >= 1 ? null
      : 'deveria retornar uma página com "content" contendo a avaliação',
  },
  {
    id: 'no-token', group: 'Contrato de segurança e erros', title: 'Sem token não cria serviço', actor: 'public',
    method: 'POST', path: () => '/api/v1/services', expect: 401,
    body: () => ({ title: 'Teste', price: 10, category: 'OUTROS' }),
    check: problemType('unauthorized'),
  },
  {
    id: 'client-forbidden', group: 'Contrato de segurança e erros', title: 'Cliente não publica serviço', actor: 'client',
    method: 'POST', path: () => '/api/v1/services', expect: 403, needs: ['clientToken'],
    body: () => ({ title: 'Teste', price: 10, category: 'OUTROS' }),
    check: problemType('forbidden'),
  },
  {
    id: 'invalid-transition', group: 'Contrato de segurança e erros', title: 'Ordem concluída não volta a PENDING',
    actor: 'provider', method: 'PATCH', path: () => `/api/v1/service-requests/${ctx.requestId}/status`, expect: 409,
    needs: ['providerToken', 'requestId'], body: () => ({ status: 'PENDING' }),
    check: problemType('invalid-transition'),
  },
];

function statusStep(id, title, status) {
  return {
    id, group: 'Ciclo do serviço', title, actor: 'provider',
    method: 'PATCH', path: () => `/api/v1/service-requests/${ctx.requestId}/status`, expect: 200,
    needs: ['providerToken', 'requestId'], body: () => ({ status }),
    check: (b) => expectStatus(b, status),
  };
}

// ---------- verificações reutilizáveis ----------
function requireField(b, field) {
  return b && b[field] != null ? null : `a resposta deveria conter o campo "${field}"`;
}
function expectStatus(b, status) {
  return b && b.status === status ? null : `o status da ordem deveria ser ${status}`;
}
function noPassword(res) {
  return /password|senha/i.test(res.text) ? 'a resposta expõe a senha ou o hash' : null;
}
function problemType(suffix) {
  return (b, res) => {
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/problem+json')) return 'o erro deveria vir como application/problem+json (ProblemDetail)';
    if (!b || typeof b.type !== 'string' || !b.type.endsWith(suffix)) {
      return `o campo "type" deveria terminar em "${suffix}"`;
    }
    return null;
  };
}
function futureDate() {
  const d = new Date(Date.now() + 7 * 86400000);
  d.setHours(9, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T09:00:00`;
}

// ---------- estado da tela ----------
const $ = (sel) => document.querySelector(sel);
const results = new Map();
let selectedId = null;
let running = false;

function newContext() {
  const suffix = Date.now().toString(36);
  return {
    providerEmail: `prestador.${suffix}@bancada.servicehub`,
    clientEmail: `cliente.${suffix}@bancada.servicehub`,
  };
}

function baseUrl() {
  return $('#baseUrl').value.trim().replace(/\/+$/, '');
}

// ---------- execução ----------
async function runStep(step) {
  const el = stepEl(step.id);
  setState(el, 'running', 'executando…');
  select(step.id);
  // a turma acompanha o passo atual sem o professor precisar rolar a tela
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  el.scrollIntoView({ block: 'nearest', behavior: reduce ? 'auto' : 'smooth' });

  const missing = (step.needs || []).filter((k) => ctx[k] == null);
  if (missing.length) {
    const result = { ok: false, message: `Depende de passos anteriores (falta ${missing.join(', ')}). Execute o ciclo em ordem.` };
    finish(step, el, result);
    return result;
  }

  const url = baseUrl() + step.path();
  const headers = { Accept: 'application/json' };
  const body = step.body ? step.body() : undefined;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = step.actor === 'provider' ? ctx.providerToken : step.actor === 'client' ? ctx.clientToken : null;
  if (token && step.id !== 'no-token') headers.Authorization = `Bearer ${token}`;

  const request = { method: step.method, url, headers, body };
  const started = performance.now();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  let result;
  try {
    const res = await fetch(url, {
      method: step.method, headers, signal: controller.signal,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch { /* corpo não JSON */ }
    const ms = Math.round(performance.now() - started);
    const response = { status: res.status, text, json, ms, headers: res.headers };

    if (res.status !== step.expect) {
      result = { ok: false, request, response, message: `Esperado ${step.expect}, recebido ${res.status}.` };
    } else {
      const problem = step.check ? step.check(json, { text, headers: res.headers }) : null;
      result = problem
        ? { ok: false, request, response, message: `Status ${res.status} correto, mas ${problem}.` }
        : { ok: true, request, response, message: `Status ${res.status} conforme o contrato.` };
      if (!problem && step.save) step.save(json);
    }
  } catch (err) {
    const ms = Math.round(performance.now() - started);
    const message = err.name === 'AbortError'
      ? `Sem resposta em ${TIMEOUT_MS / 1000} s.`
      : 'Sem resposta. Confira se a API está no ar nesta URL e se o CORS libera http://localhost:5173.';
    result = { ok: false, request, response: { status: null, text: String(err.message || err), json: null, ms }, message };
  } finally {
    clearTimeout(timer);
  }

  finish(step, el, result);
  return result;
}

function finish(step, el, result) {
  results.set(step.id, result);
  const label = result.response && result.response.status != null
    ? (result.ok ? `${result.response.status} ✓` : `${result.response.status} ✗ esperado ${step.expect}`)
    : (result.ok ? 'ok' : 'falhou');
  setState(el, result.ok ? 'ok' : 'fail', label);
  if (selectedId === step.id) renderInspector(step);
  renderSummary();
}

async function runAll(event) {
  event.preventDefault();
  if (running) return;
  if (!$('#config').reportValidity()) return;
  reset();
  running = true;
  const btn = $('#runAll');
  btn.disabled = true;
  btn.setAttribute('aria-busy', 'true');
  btn.textContent = 'Executando…';
  document.body.dataset.run = 'running';

  const pace = Number($('#pace').value);
  let failed = false;
  for (const step of STEPS) {
    if (failed) {
      setState(stepEl(step.id), 'skipped', 'não executado');
      continue;
    }
    const result = await runStep(step);
    if (!result.ok) failed = true;
    if (pace) await new Promise((r) => setTimeout(r, pace));
  }

  running = false;
  btn.disabled = false;
  btn.removeAttribute('aria-busy');
  btn.textContent = 'Executar ciclo completo';
  document.body.dataset.run = failed ? 'fail' : 'ok';
}

function reset() {
  ctx = newContext();
  results.clear();
  for (const step of STEPS) setState(stepEl(step.id), 'idle', 'não executado');
  selectedId = null;
  document.body.dataset.run = 'idle';
  renderInspector(null);
  renderSummary();
}

// ---------- renderização ----------
function stepEl(id) {
  return document.querySelector(`[data-step="${id}"]`);
}

function setState(el, state, label) {
  el.dataset.state = state;
  el.querySelector('.step-status').textContent = label;
}

function select(id) {
  selectedId = id;
  document.querySelectorAll('.step').forEach((li) => li.removeAttribute('aria-current'));
  const el = stepEl(id);
  if (el) el.setAttribute('aria-current', 'true');
  renderInspector(STEPS.find((s) => s.id === id));
}

function renderInspector(step) {
  const result = step ? results.get(step.id) : null;
  $('#inspectorStep').textContent = step ? `${STEPS.indexOf(step) + 1}. ${step.title}` : 'Selecione ou execute um passo.';
  const verdict = $('#verdict');

  if (!step || !result) {
    $('#reqLine').textContent = step ? `${step.method} ${safePath(step)}` : '—';
    $('#reqBody').textContent = '';
    $('#resLine').textContent = step ? `Esperado: ${step.expect}` : '—';
    $('#resBody').textContent = '';
    verdict.hidden = true;
    return;
  }

  const req = result.request;
  if (req) {
    const auth = req.headers.Authorization ? `\nAuthorization: Bearer ${req.headers.Authorization.slice(7, 27)}…` : '';
    $('#reqLine').textContent = `${req.method} ${req.url}`;
    $('#reqBody').textContent = (req.body !== undefined ? JSON.stringify(req.body, null, 2) : '') + (auth ? `\n${auth.trim()}` : '');
  } else {
    $('#reqLine').textContent = `${step.method} ${safePath(step)}`;
    $('#reqBody').textContent = '';
  }

  const res = result.response;
  const line = $('#resLine');
  line.textContent = '';
  if (res && res.status != null) {
    const span = document.createElement('span');
    span.className = result.ok ? 'status-ok' : 'status-fail';
    span.textContent = `HTTP ${res.status}`;
    line.append(span, ` · esperado ${step.expect} · ${res.ms} ms`);
    $('#resBody').textContent = res.json != null ? JSON.stringify(res.json, null, 2) : res.text;
  } else {
    line.textContent = res ? `Sem resposta · ${res.ms} ms` : '—';
    $('#resBody').textContent = res ? res.text : '';
  }

  verdict.hidden = false;
  verdict.dataset.result = result.ok ? 'ok' : 'fail';
  verdict.textContent = result.message;
}

function safePath(step) {
  try { return step.path(); } catch { return '…'; }
}

function renderSummary() {
  const total = STEPS.length;
  const done = [...results.values()];
  const ok = done.filter((r) => r.ok).length;
  const fail = done.length - ok;
  const el = $('#summary');
  if (!done.length) {
    el.textContent = 'Nenhum passo executado.';
    delete el.dataset.result;
  } else if (fail) {
    const first = STEPS.find((s) => results.get(s.id) && !results.get(s.id).ok);
    el.textContent = `${ok} de ${total} aprovados · falhou em “${first.title}”`;
    el.dataset.result = 'fail';
  } else {
    el.textContent = ok === total ? `${ok} de ${total} aprovados · contrato atendido` : `${ok} de ${total} aprovados`;
    el.dataset.result = ok === total ? 'ok' : '';
  }
}

function buildSteps() {
  const list = $('#steps');
  const tpl = $('#stepTemplate');
  let group = null;
  STEPS.forEach((step, i) => {
    if (step.group !== group) {
      group = step.group;
      const label = document.createElement('li');
      label.className = 'group-label';
      label.setAttribute('role', 'presentation');
      label.textContent = group;
      list.append(label);
    }
    const li = tpl.content.firstElementChild.cloneNode(true);
    li.dataset.step = step.id;
    li.dataset.testid = `step-${step.id}`;
    li.setAttribute('data-testid', `step-${step.id}`);
    li.querySelector('.step-num').textContent = i + 1;
    li.querySelector('.step-title').textContent = step.title;
    li.querySelector('.step-route').textContent = `${step.method} ${routeTemplate(step)}`;
    li.querySelector('.step-actor').textContent = ACTORS[step.actor];
    li.querySelector('.step-main').addEventListener('click', () => select(step.id));
    const runBtn = li.querySelector('.step-run');
    runBtn.setAttribute('aria-label', `Executar: ${step.title}`);
    runBtn.addEventListener('click', async () => {
      if (running) return;
      if (!ctx.providerEmail) ctx = newContext();
      await runStep(step);
    });
    list.append(li);
  });
}

function routeTemplate(step) {
  const saved = ctx;
  ctx = { requestId: '{id}', serviceId: '{id}' };
  const path = step.path();
  ctx = saved;
  return path;
}

async function copyReport() {
  const team = $('#team').value.trim() || '(sem identificação)';
  const lines = [
    `Bancada ServiceHub — ${team}`,
    `API: ${baseUrl()}`,
    `Data: ${new Date().toLocaleString('pt-BR')}`,
    $('#summary').textContent,
    '',
    ...STEPS.map((s, i) => {
      const r = results.get(s.id);
      const mark = !r ? '—' : r.ok ? 'OK ' : 'ERR';
      return `${String(i + 1).padStart(2)}. [${mark}] ${s.title}${r && !r.ok ? ` — ${r.message}` : ''}`;
    }),
  ];
  const text = lines.join('\n');
  try {
    await navigator.clipboard.writeText(text);
    toast('Relatório copiado.');
  } catch {
    toast('Não foi possível copiar; o relatório foi impresso no console.');
    console.log(text);
  }
}

function toast(message) {
  let el = document.querySelector('.toast');
  if (!el) {
    el = document.createElement('div');
    el.className = 'toast';
    el.setAttribute('role', 'status');
    document.body.append(el);
  }
  el.textContent = message;
  el.classList.add('show');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove('show'), 2200);
}

// ---------- inicialização ----------
function init() {
  const params = new URLSearchParams(location.search);
  const saved = (() => { try { return localStorage.getItem('bancada.baseUrl'); } catch { return null; } })();
  $('#baseUrl').value = params.get('api') || saved || $('#baseUrl').value;
  if (params.get('equipe')) $('#team').value = params.get('equipe');
  if (params.get('ritmo') === 'rapido') $('#pace').value = '0';
  $('#baseUrl').addEventListener('change', () => {
    try { localStorage.setItem('bancada.baseUrl', baseUrl()); } catch { /* armazenamento indisponível */ }
  });

  buildSteps();
  reset();
  $('#config').addEventListener('submit', runAll);
  $('#reset').addEventListener('click', () => { if (!running) reset(); });
  $('#copyReport').addEventListener('click', copyReport);
}

init();
