import { expect, test } from '@playwright/test';

const credentials = {
  Admin: process.env.FASTGUY_E2E_ADMIN_EMAIL,
  Staff: process.env.FASTGUY_E2E_STAFF_EMAIL,
};
const password = process.env.FASTGUY_E2E_PASSWORD;

test.skip(!password || Object.values(credentials).some(value => !value), 'Requires retained demo credentials');

function evidence(page) {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/')) requests.push({ path: url.pathname, status: response.status() });
  });
  return { errors, requests };
}

async function login(page, role) {
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(credentials[role]);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(role === 'Admin' ? /\/admin/ : /\/staff/);
}

function expectSuccess(requests, path) {
  const response = requests.find(item => item.path === path);
  expect(response).toBeTruthy();
  expect(response.status).toBeLessThan(400);
}

test('Admin reviews attendance and normalized report', async ({ page }) => {
  const observed = evidence(page);
  await login(page, 'Admin');
  await page.goto('/admin/shifts');
  const attendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/shifts/attendance');
  await page.getByRole('tab', { name: 'Duyệt công' }).click();
  expect((await attendanceResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('tabpanel', { name: 'Duyệt công' })).toContainText(/Không có chấm công phù hợp|Phút duyệt/);
  await page.goto('/admin/reports');
  await expect(page.getByText('Phí giao hàng thu khách', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Doanh thu thuần', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Giá vốn', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Tỷ lệ giá vốn', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Biên lợi nhuận gộp', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Giá trị đơn trung bình', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Dòng tiền ròng', { exact: true })).toHaveCount(0);
  expectSuccess(observed.requests, '/api/admin/shifts/attendance');
  expectSuccess(observed.requests, '/api/admin/reports/full');
  expect(observed.errors).toEqual([]);
});

test('Staff reads monthly attendance', async ({ page }) => {
  const observed = evidence(page);
  await login(page, 'Staff');
  const attendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/shifts/attendance');
  await page.goto('/staff/shifts');
  expect((await attendanceResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('heading', { name: 'Chấm công tháng' })).toBeVisible();
  await expect(page.getByText(/Chưa có dữ liệu chấm công tháng này|Đi muộn/).first()).toBeVisible();
  expectSuccess(observed.requests, '/api/shifts/attendance');
  expect(observed.errors).toEqual([]);
});
