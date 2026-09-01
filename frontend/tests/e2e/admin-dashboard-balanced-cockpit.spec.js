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
  await expect(page.getByRole('img', { name: 'Biểu đồ doanh thu 7 ngày' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Biểu đồ trạng thái đơn hàng' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Biểu đồ món bán chạy' })).toBeVisible();
  expect(dashboardRequests).toBe(1);
  expect(errors).toEqual([]);
});
