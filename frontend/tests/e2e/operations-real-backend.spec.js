import { expect, test } from '@playwright/test';

const runId = process.env.FASTGUY_E2E_RUN_ID;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const credentials = {
  Admin: process.env.FASTGUY_E2E_ADMIN_EMAIL,
  Staff: process.env.FASTGUY_E2E_STAFF_EMAIL,
  User: process.env.FASTGUY_E2E_USER_EMAIL,
};

test.skip(!runId || !password || Object.values(credentials).some(value => !value), 'Requires operations real-backend harness');

function evidence(page) {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/')) requests.push({ method: response.request().method(), path: url.pathname, status: response.status() });
  });
  return { errors, requests };
}

async function login(page, role) {
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(credentials[role]);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  const destinations = { Admin: /\/admin/, Staff: /\/staff/, User: /\/home/ };
  await expect(page).toHaveURL(destinations[role]);
}

function expectSuccess(events, method, path) {
  expect(events).toEqual(expect.arrayContaining([expect.objectContaining({ method, path, status: expect.any(Number) })]));
  expect(events.find(item => item.method === method && item.path === path).status).toBeLessThan(400);
}

test('Admin desktop weekly schedule monitoring expense asset and report KPI', async ({ page }) => {
  const observed = evidence(page);
  await login(page, 'Admin');
  await expect(page).toHaveURL(/\/admin/);
  await page.goto('/admin/shifts');
  const calendar = page.getByLabel('Lịch bảy ngày ba ca');
  await expect(calendar.locator('article')).toHaveCount(7);
  await expect(calendar.locator('select')).toHaveCount(21);
  const monitoringResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/shifts/monitoring');
  await page.getByRole('tab', { name: 'Giám sát' }).click();
  expect((await monitoringResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('tabpanel', { name: 'Giám sát' })).toBeVisible();

  const expense = `E2E-${runId}-expense`;
  await page.goto('/admin/operating-expenses');
  await page.getByRole('button', { name: 'Thêm chi phí' }).click();
  await page.getByLabel('Mô tả').fill(expense);
  await page.getByLabel('Số tiền').fill('120');
  await page.getByRole('button', { name: 'Lưu' }).click();
  await expect(page.getByRole('row', { name: new RegExp(expense) })).toBeVisible();

  const asset = `E2E-${runId}-asset`;
  await page.goto('/admin/fixed-assets');
  await page.getByRole('button', { name: 'Thêm tài sản' }).click();
  await page.getByLabel('Tên tài sản').fill(asset);
  await page.getByLabel('Nguyên giá').fill('1200');
  await page.getByLabel('Giá trị thu hồi').fill('0');
  await page.getByLabel('Thời gian').fill('12');
  await page.getByRole('button', { name: 'Lưu' }).click();
  await expect(page.getByRole('row', { name: new RegExp(asset) })).toBeVisible();

  await page.goto('/admin/reports');
  await expect(page.getByLabel('Báo cáo lợi nhuận hoạt động')).toContainText('Chi phí vận hành');
  for (const [method, path] of [['GET', '/api/admin/shifts/monitoring'], ['POST', '/api/admin/operating-expenses'], ['POST', '/api/admin/fixed-assets'], ['GET', '/api/admin/reports/operating-profit']]) expectSuccess(observed.requests, method, path);
  expect(observed.errors).toEqual([]);
});

test('Staff desktop weekly calendar source timeout metadata cutoff banner and no manual handover', async ({ page }) => {
  const observed = evidence(page);
  await login(page, 'Staff');
  const weekResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/shifts/week');
  await page.goto('/staff/shifts');
  expect((await weekResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('heading', { name: 'Lịch làm tuần' })).toBeVisible();
  await expect(page.getByRole('columnheader', { name: 'Nguồn' })).toBeVisible();
  await expect(page.getByText('MANUAL AUTO').first()).toBeAttached();
  await page.goto('/staff/orders');
  await expect(page.getByText(/Nhận đơn đến/)).toBeVisible();
  await expect(page.getByText('Bàn giao thủ công', { exact: true })).toHaveCount(0);
  await page.getByPlaceholder('Tìm mã đơn, tên, số điện thoại').fill(`E2E-${runId}-TIMEOUT`);
  await expect(page.getByText(/Hạn xử lý|expiresAt/).first()).toBeVisible();
  expect(observed.errors).toEqual([]);
});

test('User desktop login and checkout render backend cutoff config', async ({ page }) => {
  const observed = evidence(page);
  await login(page, 'User');
  const config = await page.request.get('/api/store/config');
  expect(config.ok()).toBe(true);
  const payload = await config.json();
  expect(payload.data.orderCutoffTime).toMatch(/^\d{2}:\d{2}$/);
  await page.goto('/checkout');
  await expect(page.locator('body')).toContainText(payload.data.orderCutoffTime);
  expect(observed.errors).toEqual([]);
});

test('Guest desktop checkout renders backend cutoff config deterministically', async ({ page }) => {
  const observed = evidence(page);
  const config = await page.request.get('/api/store/config');
  expect(config.ok()).toBe(true);
  const payload = await config.json();
  expect(payload.data.orderCutoffTime).toMatch(/^\d{2}:\d{2}$/);
  await page.goto('/checkout');
  await expect(page.locator('body')).toContainText(payload.data.orderCutoffTime);
  expect(observed.errors).toEqual([]);
});
