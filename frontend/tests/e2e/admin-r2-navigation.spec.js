import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });

async function setup(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN' }));
  }, { value: token });
  await page.route('**/api/admin/users*', route => route.fulfill({ json: ok([{ userId: 2, fullName: 'Nhân viên A', roleName: 'STAFF', status: 'ACTIVE' }]) }));
  await page.route('**/api/admin/shifts/week*', route => route.fulfill({ json: ok({ weekStart: '2026-08-24', shifts: [] }) }));
  await page.route('**/api/admin/shifts/attendance*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/inventory/stock-counts', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/inventory/items', route => route.fulfill({ json: ok([]) }));
}

function errors(page) {
  const values = [];
  page.on('pageerror', error => values.push(error.message));
  page.on('console', message => { if (message.type() === 'error') values.push(message.text()); });
  return values;
}

test('R2 exposes attendance and stock count workflows', async ({ page }, testInfo) => {
  await setup(page);
  const observed = errors(page);
  await page.goto('/admin/shifts');
  await expect(page.getByRole('tab', { name: 'Lịch tuần' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Giám sát' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Duyệt công' })).toHaveCount(0);
  if (testInfo.project.name.includes('mobile')) await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  const attendanceResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/shifts/attendance');
  await page.getByRole('link', { name: 'Chấm công & tiền công' }).click();
  expect((await attendanceResponse).status()).toBeLessThan(400);
  await expect(page).toHaveURL(/\/admin\/attendance$/);
  await expect(page.locator('main.attendance-page').getByRole('heading', { level: 1, name: 'Chấm công & tiền công' })).toBeVisible();
  await expect(page.getByText('Không có chấm công phù hợp.')).toBeVisible();
  if (testInfo.project.name.includes('mobile')) await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  const countsResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/inventory/stock-counts');
  await page.getByRole('link', { name: 'Kiểm kê kho' }).click();
  expect((await countsResponse).status()).toBeLessThan(400);
  await expect(page).toHaveURL(/\/admin\/inventory\/stock-counts$/);
  await expect(page.locator('main.count-page').getByRole('heading', { level: 1, name: 'Đếm và đối chiếu tồn kho' })).toBeVisible();
  expect(observed).toEqual([]);
});
