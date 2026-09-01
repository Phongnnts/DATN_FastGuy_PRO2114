import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });
const attentionOrders = [
  { orderId: 7, orderCode: 'FG-0007', status: 'DELIVERY_FAILED', customerName: 'An', paymentMethod: 'COD', paymentStatus: 'UNPAID', itemCount: 1, finalAmount: 90000, serviceFee: 0, cancelledBy: null, failureNote: 'Không liên lạc được', deliveryFailureCode: 'CUSTOMER_UNREACHABLE', deliveryAttemptCount: 1, deliveryAttemptLimit: 2, deliveryFailedAt: '2026-08-29T10:00:00', retryScheduledAt: null, returnedToStoreAt: null, refundStatus: 'PENDING', refundAmount: 90000, refundedAt: null, refundNote: null, createdAt: '2026-08-29T09:00:00', attentionReasons: ['DELIVERY_FAILED', 'PENDING_REFUND'], waitingMinutes: 80, allowedActions: ['PICKED_UP', 'RETURNED_TO_STORE'] },
  { orderId: 5, orderCode: 'FG-0005', status: 'CONFIRMED', customerName: 'Bình', paymentMethod: 'BANK_TRANSFER', paymentStatus: 'PAID', itemCount: 2, finalAmount: 120000, serviceFee: 0, cancelledBy: null, failureNote: null, deliveryFailureCode: null, deliveryAttemptCount: 0, deliveryAttemptLimit: 2, deliveryFailedAt: null, retryScheduledAt: null, returnedToStoreAt: null, refundStatus: null, refundAmount: null, refundedAt: null, refundNote: null, createdAt: '2026-08-29T08:00:00', attentionReasons: ['PROCESSING_OVERDUE'], waitingMinutes: 120, allowedActions: ['PREPARING', 'CANCELLED'] },
];
const pendingOrder = { orderId: 9, orderCode: 'FG-0009', status: 'PENDING', customerName: 'Nam Phong', paymentMethod: 'BANK_TRANSFER', paymentStatus: 'PAID', itemCount: 1, finalAmount: 80001, serviceFee: 0, cancelledBy: null, failureNote: null, deliveryFailureCode: null, deliveryAttemptCount: 0, deliveryAttemptLimit: 2, deliveryFailedAt: null, retryScheduledAt: null, returnedToStoreAt: null, refundStatus: null, refundAmount: null, refundedAt: null, refundNote: null, createdAt: '2026-09-01T15:14:00', attentionReasons: [], waitingMinutes: 18, allowedActions: ['CONFIRMED', 'CANCELLED'] };
const pendingDetail = { ...pendingOrder, customerPhone: '0912323417', customerAddress: '5802 Phường An Khánh, Thành phố Thủ Đức', totalAmount: 80001, shippingFee: 0, discountAmount: 0, deliveryNote: 'Gọi trước khi giao', failureReason: null, cancelledAt: null, confirmedAt: null, deliveredAt: null, staffName: null, shipperName: null, internalNote: null, review: null, payment: null, items: [{ productName: 'Classic Burger', variantName: 'Tiêu chuẩn', quantity: 1, unitPrice: 80001, totalPrice: 80001, imageUrl: '' }], statusHistory: [{ historyId: 1, orderId: 9, actorUserId: null, actorRole: null, fromStatus: null, toStatus: 'PENDING', status: 'PENDING', note: 'Đơn được tạo', time: '2026-09-01T15:14:00' }], statusEnteredAt: '2026-09-01T15:14:00', expiresAt: null, remainingSeconds: null, timeoutPolicy: null, ownerShiftCode: null };

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' })); }, { value: token });
}

function observeBrowser(page, { expectedStatuses = [] } = {}) {
  const errors = [];
  page.on('pageerror', error => errors.push(`page: ${error.message}`));
  page.on('console', message => {
    if (message.type() !== 'error') return;
    if (expectedStatuses.some(status => message.text().includes(`server responded with a status of ${status}`))) return;
    errors.push(`console: ${message.text()}`);
  });
  page.on('requestfailed', request => errors.push(`request: ${request.method()} ${request.url()} ${request.failure()?.errorText}`));
  page.on('response', response => {
    if (response.status() >= 400 && !expectedStatuses.includes(response.status())) errors.push(`response: ${response.status()} ${response.request().method()} ${response.url()}`);
  });
  return errors;
}

function requestEvidence(request) {
  const url = new URL(request.url());
  let body = null;
  if (request.postData()) body = request.postDataJSON();
  return { method: request.method(), path: url.pathname, query: [...url.searchParams.entries()].sort(([left], [right]) => left.localeCompare(right)), body };
}

async function setup(page, { cancelConflict = false, pendingDelay = 0, detailReloadFailure = false } = {}) {
  await authenticate(page);
  const calls = { list: [], detail: [], status: [], cancel: [] };
  let detail = structuredClone(pendingDetail);
  const handleOrderRoute = async route => {
    const request = route.request();
    const evidence = requestEvidence(request);
    const url = new URL(request.url());
    if (request.method() === 'GET' && url.pathname === '/api/admin/orders') {
      calls.list.push(evidence);
      const common = [['page', '1'], ['pageSize', '20'], ['sort', 'WAITING_DESC']];
      const attention = url.searchParams.get('attentionOnly') === 'true';
      const pending = url.searchParams.get('status') === 'PENDING';
      const refundPending = url.searchParams.get('refundStatus') === 'PENDING';
      const expectedQuery = attention ? [['attentionOnly', 'true'], ...common] : pending ? [...common, ['status', 'PENDING']].sort(([left], [right]) => left.localeCompare(right)) : refundPending ? [...common, ['refundStatus', 'PENDING']].sort(([left], [right]) => left.localeCompare(right)) : common;
      if (JSON.stringify(evidence.query) !== JSON.stringify(expectedQuery)) return route.fulfill({ status: 501, json: { status: 'error', message: 'Unexpected list request' } });
      const items = attention ? attentionOrders : pending && detail.status === 'PENDING' ? [pendingOrder] : [];
      if (pending && pendingDelay) await new Promise(resolve => setTimeout(resolve, pendingDelay));
      return route.fulfill({ json: ok({ items, pagination: { page: 1, pageSize: 20, totalItems: items.length, totalPages: items.length ? 1 : 0 } }) });
    }
    if (request.method() === 'GET' && url.pathname === '/api/admin/orders/9') {
      calls.detail.push(evidence);
      if (detailReloadFailure && calls.detail.length > 1) return route.fulfill({ status: 500, json: { status: 'error', message: 'Detail unavailable' } });
      return route.fulfill({ json: ok(detail) });
    }
    if (request.method() === 'PUT' && url.pathname === '/api/admin/orders/9/status') {
      calls.status.push(evidence);
      if (JSON.stringify(evidence.body) !== JSON.stringify({ expectedStatus: 'PENDING', status: 'CONFIRMED', note: null })) return route.fulfill({ status: 501, json: { status: 'error', message: 'Unexpected status request' } });
      detail = { ...detail, status: 'CONFIRMED', allowedActions: ['PREPARING', 'CANCELLED'] };
      return route.fulfill({ json: ok({ orderId: 9, status: 'CONFIRMED' }) });
    }
    if (request.method() === 'PUT' && url.pathname === '/api/admin/orders/9/cancel') {
      calls.cancel.push(evidence);
      if (JSON.stringify(evidence.body) !== JSON.stringify({ expectedStatus: 'PENDING', reason: 'Khách yêu cầu đổi đơn' })) return route.fulfill({ status: 501, json: { status: 'error', message: 'Unexpected cancel request' } });
      if (cancelConflict) {
        detail = { ...detail, status: 'CONFIRMED', allowedActions: ['PREPARING'] };
        return route.fulfill({ status: 409, json: { status: 'error', message: 'Conflict' } });
      }
      detail = { ...detail, status: 'CANCELLED', allowedActions: [] };
      return route.fulfill({ json: ok({ orderId: 9, status: 'CANCELLED' }) });
    }
    return route.fulfill({ status: 501, json: { status: 'error', message: 'Unexpected order request' } });
  };
  await page.route('**/api/admin/orders', handleOrderRoute);
  await page.route('**/api/admin/orders?*', handleOrderRoute);
  await page.route('**/api/admin/orders/**', handleOrderRoute);
  return { calls };
}

test('friendly queue keeps compact filters, tabs, and responsive order presentation', async ({ page }, testInfo) => {
  const { calls } = await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin/orders?status=ATTENTION');
  await expect(page.locator('.filter-toolbar')).toBeVisible();
  await expect(page.getByRole('tab', { name: /Khác/ })).toBeVisible();
  await page.getByRole('tab', { name: /Khác/ }).click();
  await expect(page.getByRole('menuitemradio', { name: 'Đã hủy' })).toBeVisible();
  await page.keyboard.press('Escape');
  const otherTab = page.getByRole('tab', { name: /Khác/ });
  await expect(otherTab).toBeFocused();
  await page.keyboard.press('ArrowRight');
  const allTab = page.getByRole('tab', { name: 'Tất cả' });
  await expect(allTab).toBeFocused();
  await page.keyboard.press('ArrowLeft');
  await expect(otherTab).toBeFocused();
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('Enter');
  await expect(allTab).toHaveAttribute('aria-selected', 'true');
  await page.goto('/admin/orders?status=ATTENTION');
  const visibleQueue = testInfo.project.name === 'mobile-chrome' ? page.locator('.mobile-order-list') : page.getByRole('table', { name: 'Danh sách đơn hàng' });
  await expect(visibleQueue).toBeVisible();
  await expect(visibleQueue.locator('.attention-reasons').getByText('Giao thất bại', { exact: true })).toBeVisible();
  expect(calls.list.length).toBeGreaterThan(0);
  expect(errors, testInfo.project.name).toEqual([]);
});

test('blocking filter changes hide stale queue rows', async ({ page }, testInfo) => {
  await setup(page, { pendingDelay: 500 });
  const errors = observeBrowser(page);
  await page.goto('/admin/orders?status=ATTENTION');
  const visibleQueue = testInfo.project.name === 'mobile-chrome' ? page.locator('.mobile-order-list') : page.getByRole('table', { name: 'Danh sách đơn hàng' });
  await expect(visibleQueue.getByText('FG-0007', { exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'Chờ xác nhận' }).click();
  await expect(page.getByText('FG-0007', { exact: true })).toHaveCount(0);
  await expect(page.getByLabel('Đang tải đơn hàng')).toBeVisible();
  await expect(visibleQueue.getByText('FG-0009', { exact: true })).toBeVisible();
  expect(errors, testInfo.project.name).toEqual([]);
});

test('refund attention canonicalizes contradictory URL filters', async ({ page }, testInfo) => {
  const { calls } = await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin/orders?status=REFUND_PENDING&refundStatus=REJECTED');
  await expect(page).toHaveURL('/admin/orders?status=REFUND_PENDING');
  await expect(page.getByLabel('Trạng thái hoàn tiền')).toHaveValue('PENDING');
  await expect.poll(() => calls.list.length).toBe(1);
  expect(calls.list[0].query).toEqual([['page', '1'], ['pageSize', '20'], ['refundStatus', 'PENDING'], ['sort', 'WAITING_DESC']]);
  expect(errors, testInfo.project.name).toEqual([]);
});

test('drawer confirms an allowed order with exact expected status and refreshes canonical data', async ({ page }, testInfo) => {
  const { calls } = await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin/orders?status=PENDING');
  const row = page.locator('tr.order-row-trigger', { hasText: 'FG-0009' });
  await row.focus();
  await expect(row).toBeFocused();
  await page.keyboard.press('Enter');
  const drawer = page.getByRole('dialog', { name: 'FG-0009' });
  await expect(drawer).toBeVisible();
  await expect(drawer.getByText('Nam Phong')).toBeVisible();
  await expect(drawer.getByRole('link', { name: '0912323417' })).toHaveAttribute('href', 'tel:0912323417');
  await expect(drawer.getByText('5802 Phường An Khánh, Thành phố Thủ Đức')).toBeVisible();
  await expect(drawer.getByText('Classic Burger')).toBeVisible();
  await expect(drawer.locator('.payment-breakdown .total')).toContainText('80.001₫');
  await drawer.getByRole('button', { name: 'Xác nhận đơn' }).click();
  await expect(drawer.getByText('Xác nhận: Xác nhận đơn?')).toBeVisible();
  await drawer.getByRole('button', { name: 'Xác nhận đơn' }).click();
  await expect.poll(() => calls.status.length).toBe(1);
  expect(calls.status[0]).toEqual({ method: 'PUT', path: '/api/admin/orders/9/status', query: [], body: { expectedStatus: 'PENDING', status: 'CONFIRMED', note: null } });
  await expect(drawer.getByText('Đã cập nhật đơn hàng.')).toBeVisible();
  expect(errors, testInfo.project.name).toEqual([]);
});

test('failed canonical detail reload removes stale drawer actions', async ({ page }, testInfo) => {
  await setup(page, { detailReloadFailure: true });
  const errors = observeBrowser(page, { expectedStatuses: [500] });
  await page.goto('/admin/orders?status=PENDING');
  await page.getByRole('button', { name: 'Xem chi tiết đơn hàng FG-0009' }).first().click();
  const drawer = page.getByRole('dialog', { name: 'FG-0009' });
  await drawer.getByRole('button', { name: 'Xác nhận đơn' }).click();
  await drawer.getByRole('button', { name: 'Xác nhận đơn' }).click();
  await expect(page.getByRole('dialog', { name: 'Đang tải' }).getByRole('alert')).toContainText('Detail unavailable');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Xác nhận đơn' })).toHaveCount(0);
  expect(errors, testInfo.project.name).toEqual([]);
});

test('drawer conflict reloads canonical order and preserves cancellation reason', async ({ page }, testInfo) => {
  const { calls } = await setup(page, { cancelConflict: true });
  const errors = observeBrowser(page, { expectedStatuses: [409] });
  await page.goto('/admin/orders?status=PENDING');
  await page.getByRole('button', { name: 'Xem chi tiết đơn hàng FG-0009' }).first().click();
  const drawer = page.getByRole('dialog', { name: 'FG-0009' });
  await drawer.getByRole('button', { name: 'Hủy đơn' }).click();
  await drawer.getByLabel('Lý do hủy đơn').fill('Khách yêu cầu đổi đơn');
  await drawer.getByRole('button', { name: 'Hủy đơn' }).click();
  await expect.poll(() => calls.cancel.length).toBe(1);
  expect(calls.cancel[0].body).toEqual({ expectedStatus: 'PENDING', reason: 'Khách yêu cầu đổi đơn' });
  await expect(drawer.getByRole('alert')).toContainText('Đơn hàng đã thay đổi');
  await expect(drawer.getByLabel('Lý do hủy đơn')).toHaveValue('Khách yêu cầu đổi đơn');
  await expect(drawer.getByRole('button', { name: 'Hủy đơn' })).toBeDisabled();
  expect(calls.detail.length).toBeGreaterThanOrEqual(2);
  expect(calls.list.length).toBeGreaterThanOrEqual(2);
  expect(errors, testInfo.project.name).toEqual([]);
});
