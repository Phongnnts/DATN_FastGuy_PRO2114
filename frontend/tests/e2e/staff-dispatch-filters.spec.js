import { expect, test } from '@playwright/test';

const success = data => ({ status: 'success', data });
const counts = { priority: 2, new: 2, review: 1 };

function dispatchOrder(overrides) {
  return {
    orderId: 1,
    orderCode: 'FG-001',
    userId: 10,
    customerName: 'Khách thử nghiệm',
    customerPhone: '0912345678',
    customerAddress: '01 Nguyễn Huệ, Quận 1',
    status: 'READY',
    orderStatus: 'READY',
    itemCount: 1,
    items: [{ productId: 1, variantId: 1, productName: 'Gà rán', variantName: 'M', quantity: 1, unitPrice: 80000, totalPrice: 80000, imageUrl: '', modifiers: [] }],
    totalAmount: 80000,
    shippingFee: 15000,
    serviceFee: 0,
    discountAmount: 0,
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    finalAmount: 95000,
    refundAmount: 0,
    refundedAt: null,
    shipperId: null,
    shipperName: null,
    assignedAt: null,
    updatedAt: '2026-08-25T09:00:00',
    endedAt: null,
    createdAt: '2026-08-25T08:00:00',
    deliveryAttemptCount: 0,
    deliveryAttemptLimit: 3,
    deliveryFailureCode: null,
    failureNote: null,
    deliveryFailedAt: null,
    retryScheduledAt: null,
    returnedToStoreAt: null,
    readyAt: '2026-08-25T08:30:00',
    classification: 'PRIORITY',
    minutesUntilClose: 15,
    ...overrides,
  };
}

const dispatchByFilter = {
  PRIORITY: [
    dispatchOrder({ orderId: 101, orderCode: 'FG-PRIORITY-FIRST', minutesUntilClose: 5 }),
    dispatchOrder({ orderId: 102, orderCode: 'FG-PRIORITY-SECOND', readyAt: '2026-08-25T07:30:00', minutesUntilClose: 20 }),
  ],
  NEW: [
    dispatchOrder({ orderId: 201, orderCode: 'FG-NEWEST', classification: 'NEW', createdAt: '2026-08-25T09:20:00', readyAt: '2026-08-25T09:25:00', minutesUntilClose: null }),
    dispatchOrder({ orderId: 202, orderCode: 'FG-OLDER', classification: 'NEW', createdAt: '2026-08-25T09:00:00', readyAt: '2026-08-25T09:05:00', minutesUntilClose: null }),
  ],
  REVIEW: [
    dispatchOrder({ orderId: 303, orderCode: 'FG-REVIEW', status: 'DELIVERY_FAILED', orderStatus: 'DELIVERY_FAILED', classification: 'REVIEW', shipperId: 8, shipperName: 'Shipper A', deliveryFailureCode: 'CUSTOMER_UNAVAILABLE', failureNote: 'Không liên lạc được khách', deliveryFailedAt: '2026-08-25T09:10:00', minutesUntilClose: null }),
  ],
};

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 7, fullName: 'Staff Mock UI', role: 'STAFF' }));
  }, token);
}

async function mockStaffDispatch(page, criticalResponses) {
  const shift = { shiftId: 4, role: 'STAFF', shiftDate: '2026-08-25', startTime: '08:00', endTime: '17:00', checkInAt: '08:01', status: 'CHECKED_IN' };
  page.on('response', response => {
    if (/\/api\/(shifts\/current|staff\/orders(?:\/dispatch|\/shippers|\/303)?)(?:\?|$)/.test(response.url())) criticalResponses.push({ url: response.url(), status: response.status() });
  });
  await page.route(/\/api\/shifts\/current$/, route => route.fulfill({ json: success({ state: 'CHECKED_IN', shift }) }));
  await page.route(/\/api\/staff\/orders$/, route => route.fulfill({ json: success([]) }));
  await page.route(/\/api\/staff\/orders\/shippers$/, route => route.fulfill({ json: success([{ id: 8, fullName: 'Shipper A', phone: '0987654321', activeOrderCount: 1 }]) }));
  await page.route(/\/api\/staff\/orders\/dispatch\?filter=(PRIORITY|NEW|REVIEW)$/, route => {
    const filter = new URL(route.request().url()).searchParams.get('filter');
    return route.fulfill({ json: success({ items: dispatchByFilter[filter], counts, serverTime: '2026-08-25T09:30:00', openTime: '08:00', closeTime: '22:00' }) });
  });
  await page.route(/\/api\/staff\/orders\/303$/, route => route.fulfill({ json: success(dispatchByFilter.REVIEW[0]) }));
}

test('mocked UI E2E presents dispatch filters, ordering, navigation and responsive layout', async ({ page }, testInfo) => {
  const errors = [];
  const criticalResponses = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await authenticate(page);
  await mockStaffDispatch(page, criticalResponses);

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
    await expect(menuToggle).toHaveAttribute('aria-expanded', 'true');
    await expect.poll(async () => (await sidebar.boundingBox()).x).toBe(0);
    await expect(page.locator('tbody tr')).toHaveCSS('display', 'grid');
    const overlay = page.getByRole('button', { name: 'Đóng menu nhân viên' });
    const overlayBox = await overlay.boundingBox();
    await overlay.click({ position: { x: overlayBox.width - 5, y: overlayBox.height / 2 } });
  } else {
    await expect(sidebar).toBeVisible();
    await expect(menuToggle).toBeHidden();
    await expect(page.locator('tbody tr')).toHaveCSS('display', 'table-row');
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);

  await page.getByRole('link', { name: 'FG-REVIEW' }).click();
  await expect(page).toHaveURL(/\/staff\/orders\/303$/);
  await expect(page.getByRole('heading', { name: 'Đơn hàng FG-REVIEW' })).toBeVisible();
  expect(criticalResponses.length).toBeGreaterThanOrEqual(6);
  expect(criticalResponses.every(response => response.status === 200)).toBe(true);
  expect(errors).toEqual([]);
});
