import { defineConfig, devices } from '@playwright/test';

const externalBaseUrl = process.env.PLAYWRIGHT_BASE_URL;
const apiTarget = process.env.PLAYWRIGHT_API_TARGET;
if (!externalBaseUrl && !apiTarget) {
  throw new Error('PLAYWRIGHT_API_TARGET is required when PLAYWRIGHT_BASE_URL is not set');
}

export default defineConfig({
  testDir: './tests/e2e',
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: externalBaseUrl || 'http://127.0.0.1:5174',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop-chrome', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },
  ],
  webServer: externalBaseUrl ? undefined : {
    command: 'npm run dev -- --host 127.0.0.1 --port 5174 --strictPort',
    url: 'http://127.0.0.1:5174',
    env: { VITE_API_PROXY_TARGET: apiTarget },
    reuseExistingServer: false,
    timeout: 120000,
  },
});
