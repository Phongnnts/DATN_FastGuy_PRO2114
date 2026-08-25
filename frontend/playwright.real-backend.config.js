import { defineConfig, devices } from '@playwright/test';

const apiTarget = process.env.PLAYWRIGHT_API_TARGET;
if (!apiTarget || /localhost:8082|127\.0\.0\.1:8082/.test(apiTarget)) throw new Error('Isolated PLAYWRIGHT_API_TARGET required');

export default defineConfig({
  testDir: './tests/e2e',
  reporter: 'list',
  timeout: 60000,
  use: { baseURL: 'http://127.0.0.1:15174', screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  projects: [
    { name: 'desktop-chrome', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 15174 --strictPort',
    url: 'http://127.0.0.1:15174',
    env: { VITE_API_PROXY_TARGET: apiTarget },
    reuseExistingServer: false,
    timeout: 120000,
  },
});
