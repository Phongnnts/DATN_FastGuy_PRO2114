import { expect, test } from '@playwright/test';

const dashboard = {
  netCashRevenueToday: 420000,
  operationalOrderCountToday: 12,
  aovToday: 125000,
  completionRateToday: 66.7,
  activeOrderCount: 14,
  activeOrdersByStatus: { PENDING: 2, CONFIRMED: 1, PREPARING: 3, READY: 4, ASSIGNED: 1, PICKED_UP: 2, DELIVERY_FAILED: 1 },
  pendingRefundCount: 3,
  pendingCodCount: 4,
  lowStockItemCount: 5,
  staffingGapCount: 2,
  revenueLast7Days: [
    { date: '2026-08-26', revenue: 210000 },
    { date: '2026-08-27', revenue: 270000 },
    { date: '2026-08-28', revenue: 240000 },
    { date: '2026-08-29', revenue: 330000 },
    { date: '2026-08-30', revenue: 310000 },
    { date: '2026-08-31', revenue: 380000 },
    { date: '2026-09-01', revenue: 420000 },
  ],
  topProductsLast7Days: [
    { productId: 1, name: 'Burger bò', sold: 32, revenue: 2560000 },
    { productId: 2, name: 'Gà rán', sold: 27, revenue: 1890000 },
    { productId: 3, name: 'Khoai tây', sold: 21, revenue: 630000 },
  ],
  lowStockProducts: [
    { productId: 1, name: 'Burger bò', remainingServings: 8 },
    { productId: 2, name: 'Gà rán', remainingServings: 11 },
  ],
  attentionItems: [{ type: 'LOW_STOCK_ITEMS', severity: 'WARNING', count: 5 }],
  sectionAvailability: { financial: 'AVAILABLE', orders: 'AVAILABLE', refunds: 'AVAILABLE', cod: 'AVAILABLE', inventory: 'AVAILABLE', staffing: 'AVAILABLE' },
};

test('admin balanced cockpit renders desktop analytics without browser errors', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' }));
  }, { value: token });
  let dashboardRequests = 0;
  let priorityRequests = 0;
  await page.route('**/api/admin/orders*', route => {
    const params = [...new URL(route.request().url()).searchParams.entries()].sort();
    if (JSON.stringify(params) !== JSON.stringify([['attentionOnly', 'true'], ['page', '1'], ['pageSize', '8'], ['sort', 'WAITING_DESC']])) return route.fulfill({ status: 200, json: { status: 'success', data: { items: [], pagination: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } } });
    priorityRequests += 1;
    return route.fulfill({ status: 200, json: { status: 'success', data: { items: [
      { orderId: 1, orderCode: 'FG-PRIORITY-01', status: 'PENDING', customerName: 'An', paymentMethod: 'COD', paymentStatus: 'UNPAID', itemCount: 1, finalAmount: 120000, serviceFee: 0, cancelledBy: null, failureNote: null, deliveryFailureCode: null, deliveryAttemptCount: 0, deliveryAttemptLimit: 3, deliveryFailedAt: null, retryScheduledAt: null, returnedToStoreAt: null, refundStatus: null, refundAmount: null, refundedAt: null, refundNote: null, createdAt: '2026-09-01T08:00:00Z', attentionReasons: ['OVERDUE_PENDING'], waitingMinutes: 45, allowedActions: [] },
      { orderId: 2, orderCode: 'FG-PRIORITY-02', status: 'DELIVERY_FAILED', customerName: 'Bình', paymentMethod: 'COD', paymentStatus: 'UNPAID', itemCount: 2, finalAmount: 180000, serviceFee: 0, cancelledBy: null, failureNote: null, deliveryFailureCode: 'CUSTOMER_UNAVAILABLE', deliveryAttemptCount: 1, deliveryAttemptLimit: 3, deliveryFailedAt: '2026-09-01T08:30:00Z', retryScheduledAt: null, returnedToStoreAt: null, refundStatus: null, refundAmount: null, refundedAt: null, refundNote: null, createdAt: '2026-09-01T07:30:00Z', attentionReasons: ['DELIVERY_FAILED'], waitingMinutes: 30, allowedActions: [] },
    ], pagination: { page: 1, pageSize: 8, totalItems: 2, totalPages: 1 } } } });
  });
  await page.route('**/api/admin/dashboard*', route => {
    dashboardRequests += 1;
    return route.fulfill({ status: 200, json: { status: 'success', data: dashboard } });
  });

  await page.goto('/admin');

  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  for (const name of ['Doanh thu 7 ngày', 'Cần xử lý', 'Trạng thái đơn hàng', 'Món bán chạy', 'Món sắp tạm hết']) {
    await expect(page.getByRole('heading', { name })).toBeVisible();
  }
  await expect(page.getByText('Còn đủ nguyên liệu cho khoảng 8 phần')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đơn cần ưu tiên' })).toBeVisible();
  await expect(page.getByText('FG-PRIORITY-01')).toBeVisible();
  await expect(page.getByRole('img', { name: 'Biểu đồ doanh thu 7 ngày' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Biểu đồ trạng thái đơn hàng' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Biểu đồ món bán chạy' })).toBeVisible();
  const primaryAction = page.getByRole('link', { name: 'Xem đơn cần xử lý' });
  const revenueDisclosure = page.getByText('Xem dữ liệu biểu đồ doanh thu', { exact: true });
  const priorityAction = page.getByRole('button', { name: /FG-PRIORITY-01/ });
  for (const control of [primaryAction, revenueDisclosure, priorityAction]) {
    await control.focus();
    await expect(control).toBeFocused();
  }
  const [revenueBox, attentionBox, statusBox, productsBox, stockBox] = await Promise.all(['.revenue-panel', '.attention-panel', '.status-panel', '.products-panel', '.stock-panel'].map(selector => page.locator(selector).boundingBox()));
  expect(Math.abs(revenueBox.y - attentionBox.y)).toBeLessThan(2);
  expect(revenueBox.width / attentionBox.width).toBeGreaterThan(1.8);
  expect(Math.max(statusBox.y, productsBox.y, stockBox.y) - Math.min(statusBox.y, productsBox.y, stockBox.y)).toBeLessThan(2);
  expect(statusBox.width < productsBox.width && stockBox.width < statusBox.width).toBeTruthy();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  await priorityAction.click();
  await expect(page).toHaveURL(/\/admin\/orders\?status=ATTENTION&orderId=1|\/admin\/orders\?orderId=1&status=ATTENTION/);
  expect(dashboardRequests).toBe(1);
  expect(priorityRequests).toBe(1);
  expect(errors).toEqual([]);
});
