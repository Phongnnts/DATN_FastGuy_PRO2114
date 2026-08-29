import { expect, test } from '@playwright/test';

const email = process.env.FASTGUY_E2E_ADMIN_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const runId = process.env.FASTGUY_E2E_RUN_ID;

test.skip(!runId || !email || !password, 'Requires operations real-backend harness');

function evidence(page) {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const path = new URL(response.url()).pathname;
    if (path.startsWith('/api/')) requests.push({ path, status: response.status() });
  });
  return { errors, requests };
}

async function login(page) {
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/admin/);
}

function expectSuccess(requests, path) {
  const response = requests.find(item => item.path === path);
  expect(response).toBeTruthy();
  expect(response.status).toBeLessThan(400);
}

test('R1 Admin navigation uses real inventory and report APIs', async ({ page }, testInfo) => {
  const observed = evidence(page);
  await login(page);
  if (testInfo.project.name.includes('mobile')) await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  const itemsResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/inventory/items');
  await page.getByRole('link', { name: 'Tồn kho' }).click();
  expect((await itemsResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toHaveAttribute('aria-selected', 'true');
  const ledgerResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/inventory/transactions');
  await page.getByRole('tab', { name: 'Lịch sử biến động' }).click();
  expect((await ledgerResponse).status()).toBeLessThan(400);
  await expect(page).toHaveURL(/\/admin\/inventory\?tab=history$/);
  await expect(page.getByRole('tabpanel', { name: 'Lịch sử biến động' })).toBeVisible();
  await page.goBack();
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toHaveAttribute('aria-selected', 'true');
  const menuResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/inventory/reports/menu-performance');
  await page.goto('/admin/inventory/reports');
  expect((await menuResponse).status()).toBeLessThan(400);
  await expect(page).toHaveURL(/\/admin\/reports\?tab=menu$/);
  await expect(page.getByRole('tab', { name: 'Hiệu quả món' })).toHaveAttribute('aria-selected', 'true');
  const expensesResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/operating-expenses');
  await page.getByRole('tab', { name: 'Hiệu quả món' }).press('ArrowRight');
  expect((await expensesResponse).status()).toBeLessThan(400);
  await expect(page).toHaveURL(/\/admin\/reports\?tab=expenses$/);
  await expect(page.getByRole('tab', { name: 'Chi phí' })).toBeFocused();
  expectSuccess(observed.requests, '/api/admin/inventory/items');
  expectSuccess(observed.requests, '/api/admin/inventory/transactions');
  expectSuccess(observed.requests, '/api/admin/inventory/reports/menu-performance');
  expectSuccess(observed.requests, '/api/admin/operating-expenses');
  expect(observed.errors).toEqual([]);
});
