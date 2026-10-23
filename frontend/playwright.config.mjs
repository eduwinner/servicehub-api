// Playwright da bancada ServiceHub.
//   npm run teste    → headless, ritmo rápido (conferência)
//   npm run aovivo   → navegador visível, ritmo de apresentação (sala de aula)
// A URL da API vem de API_URL (padrão http://localhost:8090) e a equipe de EQUIPE.
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 180_000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'node serve.mjs',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 15_000,
  },
  projects: [
    {
      name: 'rapido',
      metadata: { ritmo: 'rapido' },
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
    {
      name: 'aovivo',
      metadata: { ritmo: 'apresentacao' },
      use: {
        ...devices['Desktop Chrome'],
        headless: false,
        viewport: { width: 1440, height: 900 },
        launchOptions: { slowMo: 120 },
      },
    },
  ],
});
