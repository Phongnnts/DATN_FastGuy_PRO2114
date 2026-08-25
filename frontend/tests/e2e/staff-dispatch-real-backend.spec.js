import { expect, test } from '@playwright/test';
import { spawnSync } from 'node:child_process';

const runId = process.env.FASTGUY_E2E_RUN_ID;
const staffEmail = process.env.FASTGUY_E2E_STAFF_EMAIL;
const staffPassword = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const backendDir = process.env.FASTGUY_E2E_BACKEND_DIR;

test.skip(!runId || !staffEmail || !staffPassword || !backendDir, 'Requires isolated real-backend harness');

function collectEvidence(page) {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/')) requests.push({ method: response.request().method(), path: `${url.pathname}${url.search}`, status: response.status() });
  });
  return { errors, requests };
}

function runScheduler() {
  const result = spawnSync('mvn.cmd -Dtest=integration.StaffDispatchBrowserFixtureIT -De2e.action=scheduler test', {
    cwd: backendDir,
    env: process.env,
    encoding: 'utf8',
    timeout: 180000,
    shell: true,
  });
  expect(result.status, `${result.error || ''}\n${result.stdout || ''}\n${result.stderr || ''}`).toBe(0);
}

test('real backend Staff dispatch flow', async ({ page }, testInfo) => {
  const evidence = collectEvidence(page);
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(staffEmail);
  await page.getByPlaceholder('••••••').fill(staffPassword);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/staff(?:\/)?$/);
  await page.goto('/staff/dispatch');

  const tabs = page.getByRole('tablist', { name: 'Bộ lọc điều phối' });
  await expect(tabs.getByRole('tab', { name: 'Priority 1' })).toBeVisible();
  await expect(tabs.getByRole('tab', { name: 'New 1' })).toBeVisible();
  await expect(tabs.getByRole('tab', { name: 'Review 1' })).toBeVisible();
  const priorityCode = `E2E-${runId}-PRIORITY`;
  const newCode = `E2E-${runId}-NEW`;
  const reviewCode = `E2E-${runId}-REVIEW`;
  const cancelCode = `E2E-${runId}-CANCEL`;
  await expect(page.getByRole('link', { name: priorityCode })).toBeVisible();

  const shipperSelect = page.getByLabel(`Chọn shipper cho ${priorityCode}`);
  const shipperValue = await shipperSelect.locator('option', { hasText: 'E2E Shipper' }).getAttribute('value');
  await shipperSelect.selectOption(shipperValue);
  await page.getByRole('button', { name: 'Gán shipper' }).click();
  await expect(page.getByRole('link', { name: priorityCode })).toHaveCount(0);
  await expect(tabs.getByRole('tab', { name: 'Priority 0' })).toBeVisible();

  await tabs.getByRole('tab', { name: 'New 1' }).click();
  await expect(page.getByRole('link', { name: newCode })).toBeVisible();
  await tabs.getByRole('tab', { name: 'Review 1' }).click();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  await page.getByRole('link', { name: reviewCode }).click();
  await expect(page).toHaveURL(/\/staff\/orders\/\d+$/);
  await expect(page.getByRole('heading', { name: `Đơn hàng ${reviewCode}` })).toBeVisible();

  runScheduler();
  await page.goto('/staff/orders/history');
  await page.getByPlaceholder('Mã đơn, tên hoặc số điện thoại').fill(cancelCode);
  await page.getByRole('button', { name: 'Áp dụng' }).click();
  const cancelledRow = page.getByRole('row', { name: new RegExp(cancelCode) });
  await expect(cancelledRow).toBeVisible();
  await expect(cancelledRow.getByText('Đã hủy', { exact: true })).toBeVisible();

  const sidebar = page.locator('#staff-sidebar');
  const toggle = page.getByRole('button', { name: 'Mở menu nhân viên' });
  if (testInfo.project.name === 'mobile-chrome') {
    await expect(toggle).toBeVisible();
    await toggle.click();
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const overlay = page.getByRole('button', { name: 'Đóng menu nhân viên' });
    const box = await overlay.boundingBox();
    await overlay.click({ position: { x: box.width - 5, y: box.height / 2 } });
  } else {
    await expect(sidebar).toBeVisible();
    await expect(toggle).toBeHidden();
  }
  for (const required of [
    ['POST', '/api/auth/login', 200],
    ['GET', '/api/shifts/current', 200],
    ['GET', '/api/staff/orders/dispatch?filter=PRIORITY', 200],
    ['GET', '/api/staff/orders/dispatch?filter=NEW', 200],
    ['GET', '/api/staff/orders/dispatch?filter=REVIEW', 200],
  ]) expect(evidence.requests).toContainEqual({ method: required[0], path: required[1], status: required[2] });
  expect(evidence.requests.some(request => request.method === 'PUT' && /\/api\/staff\/orders\/\d+\/assign-shipper/.test(request.path) && request.status === 200)).toBe(true);
  expect(evidence.requests.some(request => request.method === 'GET' && /\/api\/staff\/orders\/\d+$/.test(request.path) && request.status === 200)).toBe(true);
  expect(evidence.requests.some(request => request.method === 'GET' && request.path.startsWith('/api/staff/orders/history?') && request.status === 200)).toBe(true);
  expect(evidence.errors).toEqual([]);
});
