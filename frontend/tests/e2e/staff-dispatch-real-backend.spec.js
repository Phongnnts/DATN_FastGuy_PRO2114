import { expect, test } from '@playwright/test';
import { spawnSync } from 'node:child_process';
import { hasPostConflictPriorityReload } from './staff-dispatch-request-evidence.js';

const runId = process.env.FASTGUY_E2E_RUN_ID;
const staffEmail = process.env.FASTGUY_E2E_STAFF_EMAIL;
const staffPassword = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const backendDir = process.env.FASTGUY_E2E_BACKEND_DIR;
const mavenHome = process.env.FASTGUY_E2E_MAVEN_HOME;

test.skip(!runId || !staffEmail || !staffPassword || !backendDir || !mavenHome, 'Requires isolated real-backend harness');

function collectEvidence(page) {
  const errors = [];
  const expectedConflictDiagnostics = [];
  const requests = [];
  const events = [];
  const requestIds = new WeakMap();
  let nextRequestId = 0;
  let sequence = 0;
  page.on('request', request => {
    const url = new URL(request.url());
    if (!url.pathname.startsWith('/api/')) return;
    const id = ++nextRequestId;
    requestIds.set(request, id);
    events.push({ id, sequence: ++sequence, timestamp: Date.now(), phase: 'request', method: request.method(), path: `${url.pathname}${url.search}` });
  });
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => {
    if (message.type() !== 'error') return;
    if (message.text() === 'Failed to load resource: the server responded with a status of 409 (Conflict)') expectedConflictDiagnostics.push(message.text());
    else errors.push(message.text());
  });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/')) {
      const request = response.request();
      requests.push({ method: request.method(), path: `${url.pathname}${url.search}`, body: request.method() === 'PUT' ? request.postDataJSON() : null, status: response.status() });
      events.push({ id: requestIds.get(request), sequence: ++sequence, timestamp: Date.now(), phase: 'response', method: request.method(), path: `${url.pathname}${url.search}`, status: response.status() });
    }
  });
  return { errors, expectedConflictDiagnostics, requests, events };
}

function runFixture(action) {
  const result = spawnSync(process.env.JAVA_HOME + '\\bin\\java.exe', [
    '-classpath', `${mavenHome}\\boot\\plexus-classworlds-2.9.0.jar`,
    `-Dclassworlds.conf=${mavenHome}\\bin\\m2.conf`, `-Dmaven.home=${mavenHome}`,
    `-Dmaven.multiModuleProjectDirectory=${backendDir}`,
    'org.codehaus.plexus.classworlds.launcher.Launcher',
    '-Dtest=integration.StaffDispatchBrowserFixtureIT', `-De2e.action=${action}`, 'test',
  ], {
    cwd: backendDir,
    env: process.env,
    encoding: 'utf8',
    timeout: 180000,
  });
  expect(result.status, `${result.error || ''}\n${result.stdout || ''}\n${result.stderr || ''}`).toBe(0);
}

test('real backend Staff dispatch flow', async ({ page }, testInfo) => {
  test.slow();
  const evidence = collectEvidence(page);
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(staffEmail);
  await page.getByPlaceholder('••••••').fill(staffPassword);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/staff(?:\/)?$/);
  await page.goto('/staff/dispatch');

  const tabs = page.getByRole('tablist', { name: 'Bộ lọc điều phối' });
  await expect(tabs.getByRole('tab', { name: 'Ưu tiên 2' })).toBeVisible();
  await expect(tabs.getByRole('tab', { name: 'Đơn mới 2' })).toBeVisible();
  await expect(tabs.getByRole('tab', { name: 'Xem lại 1' })).toBeVisible();
  const priorityCode = `E2E-${runId}-PRIORITY-OLD`;
  const conflictCode = `E2E-${runId}-PRIORITY-RACE`;
  const newestCode = `E2E-${runId}-NEW-RECENT`;
  const olderNewCode = `E2E-${runId}-NEW-OLDER`;
  const reviewCode = `E2E-${runId}-REVIEW`;
  const cancelCode = `E2E-${runId}-CANCEL`;
  await expect(page.getByRole('link', { name: priorityCode })).toBeVisible();
  await expect(page.getByRole('row', { name: new RegExp(priorityCode) }).getByText('Chờ lâu', { exact: true })).toBeVisible();
  const priorityLinks = await page.getByRole('tabpanel').locator('a.order-link').allTextContents();
  expect(priorityLinks.slice(0, 2)).toEqual([priorityCode, conflictCode]);

  const shipperSelect = page.getByLabel(`Chọn shipper cho ${priorityCode}`);
  const shipperValue = await shipperSelect.locator('option', { hasText: 'E2E Shipper' }).getAttribute('value');
  await shipperSelect.selectOption(shipperValue);
  const assignmentPath = new URL(await page.getByRole('link', { name: priorityCode }).getAttribute('href'), 'http://e2e').pathname.replace('/staff/orders/', '/api/staff/orders/') + '/assign-shipper';
  await page.getByRole('row', { name: new RegExp(priorityCode) }).getByRole('button', { name: 'Gán shipper' }).click();
  await expect(page.getByRole('link', { name: priorityCode })).toHaveCount(0);
  await expect(tabs.getByRole('tab', { name: 'Ưu tiên 1' })).toBeVisible();

  const conflictSelect = page.getByLabel(`Chọn shipper cho ${conflictCode}`);
  await conflictSelect.selectOption(shipperValue);
  const conflictPath = new URL(await page.getByRole('link', { name: conflictCode }).getAttribute('href'), 'http://e2e').pathname.replace('/staff/orders/', '/api/staff/orders/') + '/assign-shipper';
  await conflictSelect.evaluate(select => {
    window.__conflictSelectionReset = false;
    const observe = () => {
      if (select.value === 'Chọn shipper') window.__conflictSelectionReset = true;
      if (select.isConnected) requestAnimationFrame(observe);
    };
    observe();
  });
  runFixture('conflict');
  await page.getByRole('row', { name: new RegExp(conflictCode) }).getByRole('button', { name: 'Gán shipper' }).click();
  await expect(page.getByText('Đơn hàng đã được cập nhật. Vui lòng thử lại.', { exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: conflictCode })).toHaveCount(0);
  expect(await page.evaluate(() => window.__conflictSelectionReset)).toBe(true);
  await expect(tabs.getByRole('tab', { name: 'Ưu tiên 0' })).toBeVisible();
  expect(hasPostConflictPriorityReload(evidence.events, conflictPath)).toBe(true);

  await tabs.getByRole('tab', { name: 'Đơn mới 2' }).click();
  await expect(page.getByRole('link', { name: newestCode })).toBeVisible();
  await expect(page.getByRole('row', { name: new RegExp(newestCode) }).getByText('Mới', { exact: true })).toBeVisible();
  const newLinks = await page.getByRole('tabpanel').locator('a.order-link').allTextContents();
  expect(newLinks.slice(0, 2)).toEqual([newestCode, olderNewCode]);
  await tabs.getByRole('tab', { name: 'Xem lại 1' }).click();
  await expect(page.getByRole('row', { name: new RegExp(reviewCode) }).getByText('Cần xem lại', { exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  const reviewUrl = new URL(await page.getByRole('link', { name: reviewCode }).getAttribute('href'), 'http://e2e').pathname;
  const reviewApiPath = reviewUrl.replace('/staff/orders/', '/api/staff/orders/');
  await page.getByRole('link', { name: reviewCode }).click();
  await expect(page).toHaveURL(`http://127.0.0.1:15174${reviewUrl}`);
  await expect(page.getByRole('heading', { name: `Đơn hàng ${reviewCode}` })).toBeVisible();

  runFixture('scheduler');
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
  for (const [method, path, status] of [
    ['POST', '/api/auth/login', 200],
    ['GET', '/api/shifts/current', 200],
    ['GET', '/api/staff/orders/shippers', 200],
    ['GET', '/api/staff/orders/dispatch?filter=PRIORITY', 200],
    ['GET', '/api/staff/orders/dispatch?filter=NEW', 200],
    ['GET', '/api/staff/orders/dispatch?filter=REVIEW', 200],
    ['GET', reviewApiPath, 200],
    ['GET', `/api/staff/orders/history?page=1&size=20&search=${cancelCode}`, 200],
  ]) expect(evidence.requests).toContainEqual({ method, path, body: null, status });
  expect(evidence.requests).toContainEqual({ method: 'PUT', path: assignmentPath, body: { shipperId: Number(shipperValue), expectedStatus: 'READY' }, status: 200 });
  expect(evidence.requests).toContainEqual({ method: 'PUT', path: conflictPath, body: { shipperId: Number(shipperValue), expectedStatus: 'READY' }, status: 409 });
  expect(evidence.expectedConflictDiagnostics).toEqual(['Failed to load resource: the server responded with a status of 409 (Conflict)']);
  expect(evidence.errors).toEqual([]);
});
