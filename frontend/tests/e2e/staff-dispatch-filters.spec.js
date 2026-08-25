import { expect, test } from '@playwright/test';

const success = data => ({ status: 'success', data });
const shippers = [{ id: 8, fullName: 'Shipper A', phone: '0987654321', activeOrderCount: 1 }];

function dispatchOrder(overrides = {}) {
  return {
    orderId: 1, orderCode: 'FG-001', userId: 10, customerName: 'Khách thử nghiệm', customerPhone: '0912345678', customerAddress: '01 Nguyễn Huệ, Quận 1',
    status: 'READY', orderStatus: 'READY', itemCount: 1,
    items: [{ productId: 1, variantId: 1, productName: 'Gà rán', variantName: 'M', quantity: 1, unitPrice: 80000, totalPrice: 80000, imageUrl: '', modifiers: [] }],
    totalAmount: 80000, shippingFee: 15000, serviceFee: 0, discountAmount: 0, paymentMethod: 'COD', paymentStatus: 'UNPAID', finalAmount: 95000,
    refundAmount: 0, refundedAt: null, shipperId: null, shipperName: null, assignedAt: null, updatedAt: '2026-08-25T09:00:00', endedAt: null, createdAt: '2026-08-25T08:00:00',
    deliveryAttemptCount: 0, deliveryAttemptLimit: 3, deliveryFailureCode: null, failureNote: null, deliveryFailedAt: null, retryScheduledAt: null, returnedToStoreAt: null,
    readyAt: '2026-08-25T08:30:00', classification: 'PRIORITY', minutesUntilClose: 15,
    ...overrides,
  };
}

function response(items, counts) {
  return success({ items, counts, serverTime: '2026-08-25T09:30:00', openTime: '08:00', closeTime: '22:00' });
}

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 7, fullName: 'Staff Mock UI', role: 'STAFF' }));
  }, token);
}

function collectPageEvidence(page) {
  const errors = [];
  const responses = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error' && !/Failed to load resource.*409/.test(message.text())) errors.push(message.text());
  });
  page.on('response', value => {
    const url = new URL(value.url());
    if (url.pathname.startsWith('/api/')) responses.push({ method: value.request().method(), url: `${url.pathname}${url.search}`, status: value.status() });
  });
  return { errors, responses };
}

async function mockStaffShell(page) {
  const shift = { shiftId: 4, role: 'STAFF', shiftDate: '2026-08-25', startTime: '08:00', endTime: '17:00', checkInAt: '08:01', status: 'CHECKED_IN' };
  await page.route(/\/api\/shifts\/current$/, route => route.fulfill({ json: success({ state: 'CHECKED_IN', shift }) }));
  await page.route(/\/api\/staff\/orders$/, route => route.fulfill({ json: success([]) }));
  await page.route(/\/api\/staff\/orders\/shippers$/, route => route.fulfill({ json: success(shippers) }));
}

function expectResponse(responses, method, url, status = 200) {
  expect(responses).toContainEqual({ method, url, status });
}

function expectStaffShellResponses(responses) {
  expectResponse(responses, 'GET', '/api/shifts/current');
  expectResponse(responses, 'GET', '/api/staff/orders');
  expectResponse(responses, 'GET', '/api/staff/orders/shippers');
}

test('mocked UI E2E presents dispatch filters, ordering, navigation and responsive layout', async ({ page }, testInfo) => {
  const evidence = collectPageEvidence(page);
  const counts = { priority: 2, new: 2, review: 1 };
  const orders = {
    PRIORITY: [dispatchOrder({ orderId: 101, orderCode: 'FG-PRIORITY-FIRST', minutesUntilClose: 5 }), dispatchOrder({ orderId: 102, orderCode: 'FG-PRIORITY-SECOND', readyAt: '2026-08-25T07:30:00', minutesUntilClose: 20 })],
    NEW: [dispatchOrder({ orderId: 201, orderCode: 'FG-NEWEST', classification: 'NEW', createdAt: '2026-08-25T09:20:00' }), dispatchOrder({ orderId: 202, orderCode: 'FG-OLDER', classification: 'NEW', createdAt: '2026-08-25T09:00:00' })],
    REVIEW: [dispatchOrder({ orderId: 303, orderCode: 'FG-REVIEW', status: 'DELIVERY_FAILED', orderStatus: 'DELIVERY_FAILED', classification: 'REVIEW', shipperId: 8, deliveryFailureCode: 'CUSTOMER_UNAVAILABLE', failureNote: 'Không liên lạc được khách', deliveryFailedAt: '2026-08-25T09:10:00' })],
  };
  await authenticate(page);
  await mockStaffShell(page);
  await page.route(/\/api\/staff\/orders\/dispatch\?filter=(PRIORITY|NEW|REVIEW)$/, route => {
    const filter = new URL(route.request().url()).searchParams.get('filter');
    return route.fulfill({ json: response(orders[filter], counts) });
  });
  await page.route(/\/api\/staff\/orders\/303$/, route => route.fulfill({ json: success(orders.REVIEW[0]) }));

  await page.goto('/staff/dispatch');
  await expect(page.getByRole('heading', { name: 'Điều phối giao hàng' })).toBeVisible();
  const tabs = page.getByRole('tablist', { name: 'Bộ lọc điều phối' });
  await expect(tabs.getByRole('tab', { name: 'Priority 2' })).toHaveAttribute('aria-selected', 'true');
  await expect(tabs.getByRole('tab', { name: 'New 2' })).toBeVisible();
  await expect(tabs.getByRole('tab', { name: 'Review 1' })).toBeVisible();
  await expect(page.locator('tbody .order-link')).toHaveText(['FG-PRIORITY-FIRST', 'FG-PRIORITY-SECOND']);
  await tabs.getByRole('tab', { name: 'New 2' }).click();
  await expect(page.locator('tbody .order-link')).toHaveText(['FG-NEWEST', 'FG-OLDER']);
  await tabs.getByRole('tab', { name: 'Review 1' }).click();
  await expect(page.getByRole('link', { name: 'FG-REVIEW' })).toBeVisible();
  await expect(page.getByLabel('Chọn shipper cho FG-REVIEW')).toHaveCount(0);

  const sidebar = page.locator('#staff-sidebar');
  const menuToggle = page.getByRole('button', { name: 'Mở menu nhân viên' });
  if (testInfo.project.name === 'mobile-chrome') {
    await expect(menuToggle).toBeVisible();
    expect((await sidebar.boundingBox()).x).toBeLessThan(0);
    await menuToggle.click();
    await expect.poll(async () => (await sidebar.boundingBox()).x).toBe(0);
    await expect(page.locator('tbody tr')).toHaveCSS('display', 'grid');
    const overlay = page.getByRole('button', { name: 'Đóng menu nhân viên' });
    const box = await overlay.boundingBox();
    await overlay.click({ position: { x: box.width - 5, y: box.height / 2 } });
  } else {
    await expect(sidebar).toBeVisible();
    await expect(menuToggle).toBeHidden();
    await expect(page.locator('tbody tr')).toHaveCSS('display', 'table-row');
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  await page.getByRole('link', { name: 'FG-REVIEW' }).click();
  await expect(page.getByRole('heading', { name: 'Đơn hàng FG-REVIEW' })).toBeVisible();

  expectStaffShellResponses(evidence.responses);
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=PRIORITY');
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=NEW');
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=REVIEW');
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/303');
  expect(evidence.errors).toEqual([]);
});

test('mocked UI E2E assigns with optimistic status then removes the order after canonical reload', async ({ page }) => {
  const evidence = collectPageEvidence(page);
  const order = dispatchOrder({ orderId: 401, orderCode: 'FG-ASSIGN' });
  let assigned = false;
  let dispatchLoads = 0;
  let payload;
  await authenticate(page);
  await mockStaffShell(page);
  await page.route(/\/api\/staff\/orders\/dispatch\?filter=PRIORITY$/, route => {
    dispatchLoads += 1;
    return route.fulfill({ json: response(assigned ? [] : [order], { priority: assigned ? 0 : 1, new: 0, review: 0 }) });
  });
  await page.route(/\/api\/staff\/orders\/401\/assign-shipper$/, async route => {
    payload = route.request().postDataJSON();
    assigned = true;
    return route.fulfill({ json: { status: 'success', data: null, message: 'Shipper assigned' } });
  });

  await page.goto('/staff/dispatch');
  await page.getByLabel('Chọn shipper cho FG-ASSIGN').selectOption('8');
  await page.getByRole('button', { name: 'Gán shipper' }).click();
  await expect(page.getByRole('link', { name: 'FG-ASSIGN' })).toHaveCount(0);
  await expect(page.getByRole('tab', { name: 'Priority 0' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Không có đơn trong bộ lọc' })).toBeVisible();
  expect(payload).toEqual({ shipperId: 8, expectedStatus: 'READY' });
  expect(dispatchLoads).toBe(2);
  expectStaffShellResponses(evidence.responses);
  expectResponse(evidence.responses, 'PUT', '/api/staff/orders/401/assign-shipper');
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=PRIORITY');
  expect(evidence.errors).toEqual([]);
});

test('mocked UI E2E shows assignment conflict and reloads canonical dispatch state', async ({ page }) => {
  const evidence = collectPageEvidence(page);
  const order = dispatchOrder({ orderId: 409, orderCode: 'FG-CONFLICT' });
  let dispatchLoads = 0;
  await authenticate(page);
  await mockStaffShell(page);
  await page.route(/\/api\/staff\/orders\/dispatch\?filter=PRIORITY$/, route => {
    dispatchLoads += 1;
    return route.fulfill({ json: response([order], { priority: 1, new: 0, review: 0 }) });
  });
  await page.route(/\/api\/staff\/orders\/409\/assign-shipper$/, route => route.fulfill({ status: 409, json: { status: 'error', message: 'Đơn hàng đã thay đổi, vui lòng kiểm tra lại' } }));

  await page.goto('/staff/dispatch');
  await page.getByLabel('Chọn shipper cho FG-CONFLICT').selectOption('8');
  await page.getByRole('button', { name: 'Gán shipper' }).click();
  await expect(page.locator('.toast-container')).toContainText('Đơn hàng đã thay đổi, vui lòng kiểm tra lại');
  await expect(page.getByRole('link', { name: 'FG-CONFLICT' })).toBeVisible();
  await expect(page.getByLabel('Chọn shipper cho FG-CONFLICT').locator('option:checked')).toHaveText('Chọn shipper');
  await expect.poll(() => dispatchLoads).toBe(2);
  expectStaffShellResponses(evidence.responses);
  expectResponse(evidence.responses, 'PUT', '/api/staff/orders/409/assign-shipper', 409);
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=PRIORITY');
  expect(evidence.errors).toEqual([]);
});

test('mocked UI E2E reflects canonical automatic-cancellation removal without claiming scheduler integration', async ({ page }) => {
  const evidence = collectPageEvidence(page);
  const order = dispatchOrder({ orderId: 501, orderCode: 'FG-AUTO-CANCEL', minutesUntilClose: 0 });
  let cancelled = false;
  let dispatchLoads = 0;
  await authenticate(page);
  await mockStaffShell(page);
  await page.route(/\/api\/staff\/orders\/dispatch\?filter=PRIORITY$/, route => {
    dispatchLoads += 1;
    return route.fulfill({ json: response(cancelled ? [] : [order], { priority: cancelled ? 0 : 1, new: 0, review: 0 }) });
  });

  await page.goto('/staff/dispatch');
  await expect(page.getByRole('link', { name: 'FG-AUTO-CANCEL' })).toBeVisible();
  cancelled = true;
  await page.getByRole('button', { name: 'Làm mới' }).click();
  await expect(page.getByRole('link', { name: 'FG-AUTO-CANCEL' })).toHaveCount(0);
  await expect(page.getByRole('tab', { name: 'Priority 0' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Không có đơn trong bộ lọc' })).toBeVisible();
  expect(dispatchLoads).toBe(2);
  expectStaffShellResponses(evidence.responses);
  expectResponse(evidence.responses, 'GET', '/api/staff/orders/dispatch?filter=PRIORITY');
  expect(evidence.errors).toEqual([]);
});
