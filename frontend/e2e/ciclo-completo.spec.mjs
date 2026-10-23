// Ciclo completo do ServiceHub dirigido pela bancada: cada passo vira um test.step no relatório,
// então o professor vê exatamente onde a API da equipe deixou de cumprir o contrato.
import { test, expect } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8090';
const EQUIPE = process.env.EQUIPE || '';

test('ciclo completo do contrato ServiceHub', async ({ page }, testInfo) => {
  const ritmo = testInfo.project.metadata.ritmo;
  const query = new URLSearchParams({ api: API_URL, equipe: EQUIPE, ritmo });

  await test.step(`abrir a bancada apontando para ${API_URL}`, async () => {
    await page.goto(`/?${query}`);
    await expect(page.getByTestId('base-url')).toHaveValue(API_URL);
  });

  await test.step('executar o ciclo completo', async () => {
    await page.getByTestId('run-all').click();
    await expect(page.locator('body')).toHaveAttribute('data-run', /^(ok|fail)$/, { timeout: 150_000 });
  });

  const steps = page.locator('[data-testid^="step-"]');
  const count = await steps.count();
  for (let i = 0; i < count; i++) {
    const step = steps.nth(i);
    const title = (await step.locator('.step-title').textContent()).trim();
    await test.step(`${i + 1}. ${title}`, async () => {
      const state = await step.getAttribute('data-state');
      if (state !== 'ok') {
        await step.locator('.step-main').click();
        const verdict = await page.getByTestId('verdict').textContent().catch(() => '');
        await testInfo.attach(`passo ${i + 1} — ${title}`, {
          body: `${await page.getByTestId('request-line').textContent()}\n\n${verdict}\n\n` +
            `${await page.getByTestId('response-line').textContent()}\n${await page.getByTestId('response-body').textContent()}`,
          contentType: 'text/plain',
        });
      }
      expect.soft(state, `${title}: ${state === 'skipped' ? 'não executado (passo anterior falhou)' : 'veja o anexo'}`)
        .toBe('ok');
    });
  }

  await test.step('resultado', async () => {
    await expect(page.getByTestId('summary')).toContainText('contrato atendido');
  });
});
