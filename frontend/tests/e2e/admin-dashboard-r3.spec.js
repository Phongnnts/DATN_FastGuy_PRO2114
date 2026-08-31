import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });
const availableSections = { financial: 'AVAILABLE', orders: 'AVAILABLE', refunds: 'AVAILABLE', cod: 'AVAILABLE', inventory: 'AVAILABLE', staffing: 'AVAILABLE' };
const dashboardData = (overrides = {}) => ({ customerCount: 1, activeProductCount: 2, totalOrders: 10, totalRevenue: 500000, operationalOrderCount: 4, operationalCompletedCount: 2, completionRate: 50, ordersByStatus: { PENDING: 2, DELIVERED: 2 }, revenueToday: 250000, ordersToday: 4, deliveredOrdersToday: 2, activeOrdersToday: 2, aovToday: 125000, grossProfitToday: 80000, costComplete: true, pendingCodAmount: 100000, pendingCodCount: 1, lowStockThreshold: 5, outOfStockSkuCount: 0, lowStockSkuCount: 2, revenueByMonth: [{ month: '08/2026', revenue: 250000 }], topProducts: [{ name: 'Burger', sold: 3 }], attentionItems: [{ type: 'OVERDUE_PENDING_ORDERS', severity: 'WARNING', count: 2 }, { type: 'LOW_STOCK_ITEMS', severity: 'WARNING', count: 2 }, { type: 'PENDING_COD_SETTLEMENTS', severity: 'WARNING', count: 1 }], sectionAvailability: availableSections, ...overrides });

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' })); }, { value: token });
}

async function setup(page, data = dashboardData()) {
  await authenticate(page);
  await page.route('**/api/admin/dashboard*', route => route.fulfill({ json: ok(data) }));
}

test('R3 dashboard is decision-first on desktop and mobile', async ({ page }) => {
  await setup(page);
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  for (const label of ['Doanh thu hôm nay', 'Đơn đã giao', 'Đơn đang xử lý', 'Giá trị đơn trung bình', 'Lợi nhuận gộp']) await expect(page.getByText(label, { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Cần chú ý' })).toBeVisible();
  await expect(page.getByRole('link', { name: /Đơn chờ xác nhận quá lâu/ })).toHaveAttribute('href', /admin\/orders/);
  await expect(page.getByRole('link', { name: /Mặt hàng dưới mức an toàn/ })).toHaveAttribute('href', /admin\/inventory/);
  for (const title of ['Doanh thu gần đây', 'Trạng thái đơn', 'Món bán chạy']) await expect(page.getByRole('heading', { name: title })).toBeVisible();
  expect(errors).toEqual([]);
});

test('ready enum renders dashboard content instead of the initial error branch', async ({ page }) => {
  await setup(page);
  await page.goto('/admin');

  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  await expect(page.getByText('250.000 ₫')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thử lại' })).toHaveCount(0);
});

test('loading and forbidden enums render the initial state branches', async ({ page }) => {
  await authenticate(page);
  let release;
  const responseGate = new Promise(resolve => { release = resolve; });
  await page.route('**/api/admin/dashboard*', async route => {
    await responseGate;
    await route.fulfill({ status: 403, json: { status: 'error', message: 'Forbidden' } });
  });
  await page.goto('/admin');

  await expect(page.getByRole('status')).toContainText('Đang tải tổng quan');
  release();
  await expect(page.getByRole('alert')).toContainText('Forbidden');
  await expect(page.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toHaveCount(0);
});

test('enum states keep existing refresh error and partial UI behavior', async ({ page }) => {
  await authenticate(page);
  let releaseRefresh;
  const refreshGate = new Promise(resolve => { releaseRefresh = resolve; });
  let calls = 0;
  await page.route('**/api/admin/dashboard*', async route => {
    calls += 1;
    if (calls === 1) return route.fulfill({ json: ok(dashboardData()) });
    if (calls === 2) {
      await refreshGate;
      return route.fulfill({ status: 500, json: { status: 'error', message: 'Mất kết nối' } });
    }
    return route.fulfill({ json: ok(dashboardData({ sectionAvailability: { ...availableSections, inventory: 'UNAVAILABLE' } })) });
  });
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();

  await page.getByRole('button', { name: 'Làm mới' }).click();
  await expect(page.getByRole('status')).toContainText('Đang cập nhật tổng quan');
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  releaseRefresh();
  await expect(page.getByRole('alert')).toContainText('Mất kết nối');
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();

  await page.getByRole('button', { name: 'Làm mới' }).click();
  await expect(page.getByRole('status')).toContainText('Một số dữ liệu tạm thời chưa khả dụng');
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
});
