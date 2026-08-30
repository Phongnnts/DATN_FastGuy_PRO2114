import { expect, test } from '@playwright/test';

const email = process.env.FASTGUY_E2E_ADMIN_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const runId = process.env.FASTGUY_E2E_RUN_ID;

test.skip(!email || !password || !runId, 'Requires R7 real backend harness');

test('R7 real backend lists, filters, paginates, handles empty and records a mutation', async ({ page }) => {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/')) requests.push({ method: response.request().method(), path: url.pathname, status: response.status() });
  });

  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/admin/);
  await page.goto('/admin/attendance');
  await expect(page.getByRole('heading', { name: 'Mức công nhân viên' })).toBeVisible();
  await page.locator('.rate-panel select').selectOption({ label: 'Staff Operations' });
  await page.getByLabel('Mức giờ thường').fill('31000');
  await page.getByLabel('Mức giờ tăng ca').fill('46000');
  const mutation = page.waitForResponse(response => /\/api\/admin\/staff\/\d+\/pay-rates$/.test(new URL(response.url()).pathname) && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Thêm mức công' }).click();
  expect((await mutation).status()).toBe(201);

  await page.goto('/admin/activity-logs');
  await expect(page.getByRole('heading', { name: 'Nhật ký hoạt động' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Danh sách nhật ký hoạt động' }).getByText('Tạo mức lương nhân viên', { exact: true })).toBeVisible();
  await page.getByLabel('Loại thao tác').selectOption('STAFF_PAY_RATE_CREATED');
  await page.getByRole('button', { name: 'Lọc' }).click();
  await expect(page.getByRole('region', { name: 'Danh sách nhật ký hoạt động' }).getByText('Tạo mức lương nhân viên', { exact: true })).toBeVisible();
  await page.getByLabel('ID người thực hiện').fill('2147483647');
  await page.getByRole('button', { name: 'Lọc' }).click();
  await expect(page.getByText('Không có nhật ký phù hợp.')).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Phân trang nhật ký' })).toHaveCount(0);

  expect(requests).toEqual(expect.arrayContaining([
    expect.objectContaining({ method: 'POST', status: 201 }),
    { method: 'GET', path: '/api/admin/activity-logs', status: 200 },
  ]));
  expect(errors).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
});
