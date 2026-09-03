import { expect, test } from '@playwright/test';

const email = process.env.FASTGUY_E2E_ADMIN_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const runId = process.env.FASTGUY_E2E_RUN_ID;
test.skip(!runId || !email || !password, 'Requires operations real-backend harness');

test('R5 real backend shares report period and exposes estimated result', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/admin/);

  const profitResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/reports/operating-profit');
  await page.goto('/admin/reports');
  const profit = await profitResponse;
  expect(profit.status()).toBeLessThan(400);
  const payload = await profit.json();
  for (const field of ['storeExpenses', 'estimatedOperatingResult', 'includesManualSalary', 'costComplete']) expect(payload.data).toHaveProperty(field);
  expect(payload.data.includesManualSalary).toBe(true);
  await expect(page.getByRole('heading', { name: 'Báo cáo kinh doanh' })).toBeVisible();
  await expect(page.locator('.executive-kpis')).toBeVisible();
  await expect(page.locator('.charts-grid .chart-card')).toHaveCount(10);
  await expect(page.locator('.mac-table')).toBeVisible();
  await expect(page.locator('.mac-table tbody tr').first()).toBeVisible();
  await expect(page.getByLabel('Kết quả vận hành ước tính')).toContainText('Chi phí cửa hàng');
  await expect(page.getByLabel('Kết quả vận hành ước tính')).toContainText('Bao gồm khoản lương nhập tay');
  await expect(page.getByText('Khấu hao', { exact: true })).toHaveCount(0);

  const expenseResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/operating-expenses');
  await page.getByRole('tab', { name: 'Chi phí' }).click();
  const expenses = await expenseResponse;
  expect(expenses.status()).toBeLessThan(400);
  const expenseUrl = new URL(expenses.url());
  expect(expenseUrl.searchParams.get('fromDate')).toBeTruthy();
  expect(expenseUrl.searchParams.get('toDate')).toBeTruthy();
  await expect(page.getByText(`E2E-${runId}-SALARY`, { exact: true })).toBeVisible();
  await expect(page.getByText(`E2E-${runId}-OLD`, { exact: true })).toHaveCount(0);
  await expect(page.getByRole('row', { name: new RegExp(`E2E-${runId}-SALARY`) })).toContainText('Lương · Nhập tay');

  const menuResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/inventory/reports/menu-performance');
  await page.getByRole('tab', { name: 'Hiệu quả món' }).click();
  const menu = await menuResponse;
  expect(menu.status()).toBeLessThan(400);
  const menuUrl = new URL(menu.url());
  expect(menuUrl.searchParams.get('fromDate')).toBe(expenseUrl.searchParams.get('fromDate'));
  expect(menuUrl.searchParams.get('toDate')).toBe(expenseUrl.searchParams.get('toDate'));
  expect(errors).toEqual([]);
});
