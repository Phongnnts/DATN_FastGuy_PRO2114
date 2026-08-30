import { expect, test } from '@playwright/test';

const credentials = {
  Admin: process.env.FASTGUY_E2E_ADMIN_EMAIL,
  Staff: process.env.FASTGUY_E2E_STAFF_EMAIL,
};
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const attendanceMonth = previousBusinessMonth();

test.skip(!password || Object.values(credentials).some(value => !value), 'Requires disposable harness credentials');

function previousBusinessMonth() {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: 'numeric', day: 'numeric' }).formatToParts(new Date()).map(part => [part.type, part.value]));
  const date = new Date(Number(parts.year), Number(parts.month) - 1, Number(parts.day));
  date.setDate(1);
  date.setMonth(date.getMonth() - 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

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
  const attendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/shifts/attendance');
  await page.goto('/admin/attendance');
  expect((await attendanceResponse).status()).toBeLessThan(400);
  const attendancePanel = page.getByRole('region', { name: 'Chấm công' });
  const monthInput = attendancePanel.getByLabel('Tháng');
  const selectedAttendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/shifts/attendance');
  await monthInput.fill(attendanceMonth);
  await monthInput.press('Tab');
  expect((await selectedAttendanceResponse).status()).toBeLessThan(400);
  await expect(page.getByRole('heading', { name: 'Chấm công & tiền công' })).toBeVisible();
  await expect(page.getByRole('spinbutton', { name: 'Phút duyệt' }).first()).toBeVisible();
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
  const attendancePanel = page.getByRole('region', { name: 'Chấm công tháng' });
  const monthInput = attendancePanel.getByLabel('Tháng');
  const selectedAttendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/shifts/attendance');
  await monthInput.fill(attendanceMonth);
  await monthInput.press('Tab');
  expect((await selectedAttendanceResponse).status()).toBeLessThan(400);
  await expect(page.getByText('Chọn ngày màu xanh để xem ca. Check-in và check-out đều do bạn thực hiện.', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Chấm công tháng' })).toBeVisible();
  await expect(page.getByText('Chờ duyệt', { exact: true }).first()).toBeVisible();
  expectSuccess(observed.requests, '/api/shifts/attendance');
  expect(observed.errors).toEqual([]);
});
