import { expect, test } from '@playwright/test';

const email = process.env.FASTGUY_E2E_ADMIN_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const runId = process.env.FASTGUY_E2E_RUN_ID;
test.skip(!runId || !email || !password, 'Requires operations real-backend harness');

test('R3 real dashboard returns decision KPIs and attention contract', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  const dashboardResponse = page.waitForResponse(value => new URL(value.url()).pathname === '/api/admin/dashboard');
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/admin/);
  const response = await dashboardResponse;
  expect(response.status()).toBeLessThan(400);
  const payload = await response.json();
  for (const field of ['deliveredOrdersToday','activeOrdersToday','aovToday','grossProfitToday','costComplete','attentionItems']) expect(payload.data).toHaveProperty(field);
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Cần chú ý' })).toBeVisible();
  expect(errors).toEqual([]);
});
