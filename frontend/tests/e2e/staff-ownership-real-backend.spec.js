import { expect, test } from '@playwright/test';
import { spawnSync } from 'node:child_process';

const runId = process.env.FASTGUY_E2E_RUN_ID;
const email = process.env.FASTGUY_E2E_STAFF_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;

test.skip(!runId || !email || !password, 'Requires isolated real-backend harness');

function code(suffix) { return `E2E-OWN-${runId}-${suffix}`; }
function runFixture(action) {
  const result = spawnSync('mvn.cmd', [`-Dtest=integration.StaffOwnershipBrowserFixtureIT`, `-De2e.action=${action}`, 'test'], {
    cwd: process.env.FASTGUY_E2E_BACKEND_DIR, env: process.env, encoding: 'utf8', timeout: 180000,
  });
  expect(result.status, `${result.stdout || ''}\n${result.stderr || ''}`).toBe(0);
}
function evidence(page) {
  const errors = [];
  const requests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error' && !message.text().includes('409 (Conflict)')) errors.push(message.text()); });
  page.on('response', async response => {
    const url = new URL(response.url());
    if (!url.pathname.startsWith('/api/')) return;
    let body = null;
    try { body = await response.json(); } catch {}
    requests.push({ method: response.request().method(), path: `${url.pathname}${url.search}`, status: response.status(), body });
  });
  return { errors, requests };
}

async function login(page) {
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/staff(?:\/)?$/);
}

async function orderId(page, orderCode) {
  const href = await page.getByRole('link', { name: orderCode }).getAttribute('href');
  return Number(href.split('/').pop());
}

async function assertNoPageOverflow(page) {
  const overflow = await page.evaluate(() => ({
    fits: document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    documentWidth: document.documentElement.scrollWidth,
    viewportWidth: document.documentElement.clientWidth,
    culprits: [...document.querySelectorAll('body *')].filter(element => {
      const box = element.getBoundingClientRect();
      return box.right > document.documentElement.clientWidth + 1 || element.scrollWidth > element.clientWidth + 1;
    }).slice(0, 12).map(element => ({ tag: element.tagName, className: element.className, id: element.id, clientWidth: element.clientWidth, scrollWidth: element.scrollWidth, right: Math.round(element.getBoundingClientRect().right) })),
  }));
  expect(overflow.fits, JSON.stringify(overflow)).toBe(true);
}

test('real backend Staff ownership and handover', async ({ page, request }, testInfo) => {
  test.slow();
  const log = evidence(page);
  await login(page);
  await page.goto('/staff/orders?tab=CONFIRMED');
  await expect(page.getByRole('link', { name: code('OWN-CONFIRMED') })).toBeVisible();
  await expect(page.getByRole('link', { name: code('OTHER-READY') })).toHaveCount(0);

  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('tab', { selected: true }).press('End');
    await expect(page.getByRole('tab', { name: /Bàn giao/ })).toHaveAttribute('aria-selected', 'true');
  } else {
    await page.getByRole('tab', { name: /Bàn giao/ }).click();
  }
  for (const suffix of ['UNOWNED-PREPARING', 'OTHER-READY', 'RACE', 'RECOVERY', 'TERMINAL']) await expect(page.getByRole('link', { name: code(suffix) })).toBeVisible();
  for (const suffix of ['OWN-CONFIRMED', 'ASSIGNED', 'PENDING']) await expect(page.getByRole('link', { name: code(suffix) })).toHaveCount(0);
  await assertNoPageOverflow(page);

  const unownedRow = page.getByRole('row', { name: new RegExp(code('UNOWNED-PREPARING')) });
  const unownedId = await orderId(page, code('UNOWNED-PREPARING'));
  await unownedRow.getByRole('button', { name: 'Nhận bàn giao' }).click();
  await expect(page.getByText(`${code('UNOWNED-PREPARING')} đã chuyển vào hàng đợi PREPARING.`)).toBeAttached();
  await expect(page.getByRole('link', { name: code('UNOWNED-PREPARING') })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Đang chế biến' }).click();
  await expect(page.getByRole('link', { name: code('UNOWNED-PREPARING') })).toBeVisible();

  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('tab', { selected: true }).press('End');
    await expect(page.getByRole('tab', { name: /Bàn giao/ })).toHaveAttribute('aria-selected', 'true');
  } else {
    await page.getByRole('tab', { name: /Bàn giao/ }).click();
  }
  const raceId = await orderId(page, code('RACE'));
  const token = await page.evaluate(() => localStorage.getItem('token'));
  const payload = { expectedStatus: 'READY', expectedOwnerShiftId: Number((await page.getByRole('row', { name: new RegExp(code('RACE')) }).getByText(/^Shift \d+$/).textContent()).match(/\d+/)[0]) };
  const racePath = `/api/staff/orders/${raceId}/handover`;
  const raceResults = await Promise.all([
    request.put(racePath, { baseURL: 'http://127.0.0.1:15174', headers: { Authorization: `Bearer ${token}` }, data: payload }),
    request.put(racePath, { baseURL: 'http://127.0.0.1:15174', headers: { Authorization: `Bearer ${token}` }, data: payload }),
  ]);
  expect(raceResults.map(result => result.status()).sort()).toEqual([200, 409]);
  const raceBodies = await Promise.all(raceResults.map(result => result.json()));
  expect(raceBodies.every(body => body && typeof body === 'object')).toBe(true);

  const api = { baseURL: 'http://127.0.0.1:15174', headers: { Authorization: `Bearer ${token}` } };
  const ownership = await request.get('/api/staff/orders/ownership-count', api);
  expect(ownership.status()).toBe(200);
  expect((await ownership.json()).data.activeOwnershipCount).toBeGreaterThanOrEqual(3);

  const currentShiftResponse = await request.get('/api/shifts/current', api);
  expect(currentShiftResponse.status()).toBe(200);
  const currentShiftId = (await currentShiftResponse.json()).data.shift.shiftId;
  const blockedCheckout = await request.post(`/api/shifts/${currentShiftId}/check-out`, api);
  const blockedCheckoutBody = await blockedCheckout.json();
  expect(blockedCheckout.status(), JSON.stringify(blockedCheckoutBody)).toBe(409);
  expect(blockedCheckoutBody).toMatchObject({ status: 'error', data: { activeOwnershipCount: expect.any(Number) } });

  const recoveryId = await orderId(page, code('RECOVERY'));
  const recoveryRow = page.getByRole('row', { name: new RegExp(code('RECOVERY')) });
  const recoveryOwner = Number((await recoveryRow.getByText(/^Shift \d+$/).textContent()).match(/\d+/)[0]);
  const recoveryClaim = await request.put(`/api/staff/orders/${recoveryId}/handover`, { ...api, data: { expectedStatus: 'DELIVERY_FAILED', expectedOwnerShiftId: recoveryOwner } });
  expect(recoveryClaim.status()).toBe(200);
  const shippers = await request.get('/api/staff/orders/shippers', api);
  expect(shippers.status()).toBe(200);
  const shipperId = (await shippers.json()).data[0].id;
  const recovery = await request.post(`/api/staff/orders/${recoveryId}/retry-delivery`, { ...api, data: { expectedStatus: 'DELIVERY_FAILED', shipperId, retryMode: 'IMMEDIATE', scheduledAt: null, note: 'Ownership E2E recovery' } });
  expect(recovery.status()).toBe(200);
  const recoveryHandover = await request.get('/api/staff/orders/handover', api);
  expect((await recoveryHandover.json()).data.some(item => item.orderCode === code('RECOVERY'))).toBe(false);

  const terminalQueue = await request.get('/api/staff/orders/handover', api);
  const terminalItem = (await terminalQueue.json()).data.find(item => item.orderCode === code('TERMINAL'));
  const terminalClaim = await request.put(`/api/staff/orders/${terminalItem.orderId}/handover`, { ...api, data: { expectedStatus: 'DELIVERY_FAILED', expectedOwnerShiftId: terminalItem.staffShiftId } });
  expect(terminalClaim.status()).toBe(200);
  const terminal = await request.post(`/api/staff/orders/${terminalItem.orderId}/return-to-store`, { ...api, data: { expectedStatus: 'DELIVERY_FAILED', note: 'Ownership E2E terminal' } });
  const terminalBody = await terminal.json();
  expect(terminal.status(), JSON.stringify(terminalBody)).toBe(200);
  const terminalDetail = await request.get(`/api/staff/orders/${terminalItem.orderId}`, api);
  expect(terminalDetail.status()).toBe(200);
  expect((await terminalDetail.json()).data).toMatchObject({ status: 'RETURNED_TO_STORE', staffShiftId: null });

  const adminLogin = await request.post('/api/auth/login', { baseURL: api.baseURL, data: { login: `ownership-admin-${runId}@test.local`, password } });
  const adminLoginBody = await adminLogin.json();
  expect(adminLogin.status(), JSON.stringify(adminLoginBody)).toBe(200);
  const adminToken = adminLoginBody.data.token;
  const adminApi = { baseURL: api.baseURL, headers: { Authorization: `Bearer ${adminToken}` } };

  const handoverBeforeAdmin = await request.get('/api/staff/orders/handover', api);
  const adminOrder = (await handoverBeforeAdmin.json()).data.find(item => item.orderCode === code('ADMIN-PENDING'));
  expect(adminOrder).toBeUndefined();
  const adminList = await request.get('/api/admin/orders', adminApi);
  const adminOrderId = (await adminList.json()).data.find(item => item.orderCode === code('ADMIN-PENDING')).orderId;
  const adminTransition = await request.put(`/api/admin/orders/${adminOrderId}/status`, { ...adminApi, data: { status: 'CONFIRMED', note: 'Ownership E2E admin' } });
  expect(adminTransition.status()).toBe(200);
  const handoverAfterAdmin = await request.get('/api/staff/orders/handover', api);
  expect((await handoverAfterAdmin.json()).data.some(item => item.orderCode === code('ADMIN-PENDING'))).toBe(true);
  const adminDetail = await request.get(`/api/staff/orders/${adminOrderId}`, api);
  expect(adminDetail.status()).toBe(200);
  expect((await adminDetail.json()).data).toMatchObject({ status: 'CONFIRMED', staffShiftId: null });

  await page.goto('/staff/shifts');
  await expect(page.getByRole('alert')).toContainText('đơn cần bàn giao trước khi check-out');
  await expect(page.getByRole('link', { name: 'Mở danh sách bàn giao' })).toBeVisible();
  await assertNoPageOverflow(page);
  const otherLogin = await request.post('/api/auth/login', { baseURL: api.baseURL, data: { login: `ownership-other-${runId}@test.local`, password } });
  const otherLoginBody = await otherLogin.json();
  expect(otherLogin.status(), JSON.stringify(otherLoginBody)).toBe(200);
  const otherApi = { baseURL: api.baseURL, headers: { Authorization: `Bearer ${otherLoginBody.data.token}` } };
  const otherQueue = await request.get('/api/staff/orders/handover', otherApi);
  expect(otherQueue.status()).toBe(200);
  const currentOwned = (await otherQueue.json()).data.filter(item => item.staffShiftId === currentShiftId);
  expect(currentOwned.length).toBeGreaterThan(0);
  for (const item of currentOwned) {
    const transfer = await request.put(`/api/staff/orders/${item.orderId}/handover`, { ...otherApi, data: { expectedStatus: item.status, expectedOwnerShiftId: currentShiftId } });
    expect(transfer.status()).toBe(200);
  }
  const releasedOwnership = await request.get('/api/staff/orders/ownership-count', api);
  expect((await releasedOwnership.json()).data.activeOwnershipCount).toBe(0);
  const successfulCheckout = await request.post(`/api/shifts/${currentShiftId}/check-out`, api);
  expect(successfulCheckout.status()).toBe(200);
  expect((await successfulCheckout.json()).status).toBe('success');
  expect(log.requests.some(item => item.path === `/api/staff/orders/${unownedId}/handover` && item.status === 200)).toBe(true);
  expect(log.errors).toEqual([]);
  expect(['desktop-chrome', 'mobile-chrome']).toContain(testInfo.project.name);
});
