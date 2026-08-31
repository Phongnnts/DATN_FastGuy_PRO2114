import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });
const availableSections = { financial: 'AVAILABLE', orders: 'AVAILABLE', refunds: 'AVAILABLE', cod: 'AVAILABLE', inventory: 'AVAILABLE', staffing: 'AVAILABLE' };
const dashboardData = (overrides = {}) => ({
  netCashRevenueToday: 420000,
  activeOrderCount: 14,
  pendingRefundCount: 3,
  pendingCodCount: 4,
  lowStockItemCount: 5,
  staffingGapCount: 2,
  activeOrdersByStatus: { PENDING: 2, CONFIRMED: 1, PREPARING: 3, READY: 4, ASSIGNED: 1, PICKED_UP: 2, DELIVERY_FAILED: 1, DELIVERED: 99, CANCELLED: 8, RETURNED_TO_STORE: 7 },
  operationalOrderCountToday: 12,
  operationalCompletedCountToday: 8,
  completionRateToday: 66.67,
  attentionItems: [
    { type: 'OVERDUE_PENDING_ORDERS', severity: 'CRITICAL', count: 2 },
    { type: 'DELIVERY_FAILED_ORDERS', severity: 'WARNING', count: 1 },
    { type: 'PENDING_REFUNDS', severity: 'WARNING', count: 3 },
    { type: 'STAFF_COVERAGE_GAPS', severity: 'CRITICAL', count: 2 },
    { type: 'LOW_STOCK_ITEMS', severity: 'WARNING', count: 5 },
    { type: 'PENDING_COD_SETTLEMENTS', severity: 'WARNING', count: 4 },
  ],
  sectionAvailability: availableSections,
  customerCount: 1,
  totalUsers: 3,
  totalOrders: 20,
  activeProductCount: 2,
  totalProducts: 2,
  ordersByStatus: { PENDING: 2, DELIVERED: 2 },
  operationalOrderCount: 4,
  operationalCompletedCount: 2,
  completionRate: 50,
  totalRevenue: 500000,
  pendingOrders: 2,
  revenueToday: 250000,
  ordersToday: 4,
  pendingCodAmount: 100000,
  revenueByMonth: [{ month: 8, year: 2026, revenue: 250000 }],
  topProducts: [{ name: 'Burger', sold: 3 }],
  lowStockThreshold: 5,
  outOfStockSkuCount: 0,
  lowStockSkuCount: 2,
  deliveredOrdersToday: 2,
  activeOrdersToday: 2,
  aovToday: 125000,
  grossProfitToday: 80000,
  costComplete: true,
  ...overrides,
});

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' })); }, { value: token });
}

function observeBrowser(page) {
  const errors = [];
  page.on('pageerror', error => errors.push(`page: ${error.message}`));
  page.on('console', message => { if (message.type() === 'error') errors.push(`console: ${message.text()}`); });
  page.on('requestfailed', request => errors.push(`request: ${request.method()} ${request.url()} ${request.failure()?.errorText}`));
  return errors;
}

async function mockDestinations(page) {
  await page.route('**/api/admin/orders*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/refunds*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/users*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/shifts/week*', route => route.fulfill({ json: ok({ weekStart: '2026-08-31', shifts: [] }) }));
  await page.route('**/api/admin/shifts/monitoring*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/inventory/items*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/inventory/transactions*', route => route.fulfill({ json: ok({ items: [], totalItems: 0, totalPages: 0 }) }));
  await page.route('**/api/admin/products*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/cod-settlements/admin*', route => route.fulfill({ json: ok([]) }));
}

async function setup(page, data = dashboardData()) {
  await authenticate(page);
  await mockDestinations(page);
  await page.route('**/api/admin/dashboard*', route => route.fulfill({ json: ok(data) }));
}

const metrics = [
  ['Doanh thu thuần hôm nay', '420.000₫'],
  ['Đơn đang hoạt động', '14'],
  ['Hoàn tiền chờ xử lý', '3'],
  ['COD chờ xác nhận', '4'],
  ['Mặt hàng sắp hết', '5'],
  ['Ca thiếu nhân sự', '2'],
];
const flow = [
  ['Chờ xác nhận', '2'],
  ['Đã xác nhận', '1'],
  ['Đang chế biến', '3'],
  ['Sẵn sàng giao', '4'],
  ['Đã gán shipper', '1'],
  ['Đang giao', '2'],
  ['Giao thất bại', '1'],
];

test('dashboard hierarchy, canonical metrics, attention routes, and chart data are decision-first', async ({ page }) => {
  await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin');

  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  const sections = page.locator('[data-dashboard-section]');
  await expect(sections).toHaveCount(5);
  expect(await sections.evaluateAll(nodes => nodes.map(node => node.dataset.dashboardSection))).toEqual(['header', 'attention', 'metrics', 'flow', 'flow-data']);
  await expect(page.getByRole('heading', { name: 'Cần xử lý ngay' })).toBeVisible();

  for (const [label, value] of metrics) {
    const metric = page.getByRole('group', { name: label });
    await expect(metric).toContainText(value);
  }

  const links = [
    ['Đơn chờ xác nhận quá lâu', '/admin/orders?status=ATTENTION'],
    ['Yêu cầu hoàn tiền đang chờ', '/admin/refunds?status=PENDING'],
    ['Ca làm cần bổ sung nhân viên', '/admin/shifts?tab=monitoring'],
    ['Mặt hàng dưới mức an toàn', '/admin/inventory?filter=LOW'],
    ['Bàn giao COD đang chờ', '/admin/cod-settlements?status=SUBMITTED'],
  ];
  for (const [name, href] of links) await expect(page.getByRole('link', { name: new RegExp(name) })).toHaveAttribute('href', href);

  const table = page.getByRole('table', { name: 'Dữ liệu luồng đơn đang hoạt động' });
  const chart = page.getByRole('img', { name: /Luồng đơn đang hoạt động/ });
  for (const [label, value] of flow) {
    await expect(table.getByRole('row', { name: new RegExp(`${label} ${value}`) })).toBeVisible();
    await expect(chart).toHaveAttribute('aria-label', new RegExp(`${label}: ${value}`));
  }
  await expect(table).not.toContainText('Đã giao');
  await expect(table).not.toContainText('Đã hủy');
  await expect(table).not.toContainText('Đã hoàn kho');
  expect(errors).toEqual([]);
});

test('all attention destinations hydrate exact query state and reject invalid values safely', async ({ page }) => {
  await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin');

  await page.getByRole('link', { name: /Đơn chờ xác nhận quá lâu/ }).click();
  await expect(page).toHaveURL(/\/admin\/orders\?status=ATTENTION$/);
  await expect(page.getByRole('tab', { name: /Cần xử lý/ })).toHaveAttribute('aria-selected', 'true');

  await page.goto('/admin');
  await page.getByRole('link', { name: /Yêu cầu hoàn tiền đang chờ/ }).click();
  await expect(page).toHaveURL(/\/admin\/refunds\?status=PENDING$/);
  await expect(page.locator('.status-filter button.active')).toContainText('Chờ hoàn');

  await page.goto('/admin');
  await page.getByRole('link', { name: /Ca làm cần bổ sung nhân viên/ }).click();
  await expect(page).toHaveURL(/\/admin\/shifts\?tab=monitoring$/);
  await expect(page.getByRole('tab', { name: 'Giám sát' })).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('tab', { name: 'Lịch tuần' }).click();
  await expect(page).toHaveURL(/\/admin\/shifts\?tab=schedule$/);

  await page.goto('/admin');
  await page.getByRole('link', { name: /Mặt hàng dưới mức an toàn/ }).click();
  await expect(page).toHaveURL(/\/admin\/inventory\?filter=LOW$/);
  await expect(page.getByLabel('Trạng thái')).toHaveValue('LOW');
  await page.goto('/admin/inventory?filter=INVALID&tab=history');
  await expect(page).toHaveURL(/tab=history/);
  await page.getByRole('tab', { name: 'Tồn hiện tại' }).click();
  await expect(page.getByLabel('Trạng thái')).toHaveValue('ALL');
  await expect(page).toHaveURL(/filter=INVALID/);

  await page.goto('/admin');
  await page.getByRole('link', { name: /Bàn giao COD đang chờ/ }).click();
  await expect(page).toHaveURL(/\/admin\/cod-settlements\?status=SUBMITTED$/);
  await expect(page.getByRole('button', { name: 'Chờ xác nhận' })).toHaveAttribute('aria-pressed', 'true');
  await page.goto('/admin/cod-settlements?status=INVALID');
  await expect(page.getByRole('button', { name: 'Chờ xác nhận' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page).toHaveURL(/status=INVALID/);

  expect(errors).toEqual([]);
});

test('initial loading uses a layout-matched skeleton and forbidden has a permission state', async ({ page }) => {
  await authenticate(page);
  const errors = observeBrowser(page);
  let release;
  const responseGate = new Promise(resolve => { release = resolve; });
  await page.route('**/api/admin/dashboard*', async route => {
    await responseGate;
    await route.fulfill({ status: 403, json: { status: 'error', message: 'Forbidden' } });
  });
  await page.goto('/admin');

  const skeleton = page.getByRole('status', { name: 'Đang tải tổng quan' });
  await expect(skeleton.locator('.skeleton-attention')).toBeVisible();
  await expect(skeleton.locator('.skeleton-metrics')).toBeVisible();
  await expect(skeleton.locator('.skeleton-flow')).toBeVisible();
  release();
  await expect(page.getByRole('alert')).toContainText('Bạn không có quyền xem tổng quan vận hành');
  await expect(page.getByRole('button', { name: 'Thử lại' })).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toHaveCount(0);
  expect(errors).toEqual([]);
});

test('initial error retries while refresh remains nonblocking and exposes store error', async ({ page }) => {
  await authenticate(page);
  const errors = observeBrowser(page);
  let calls = 0;
  let releaseRefresh;
  const refreshGate = new Promise(resolve => { releaseRefresh = resolve; });
  await page.route('**/api/admin/dashboard*', async route => {
    calls += 1;
    if (calls === 1) return route.fulfill({ status: 500, json: { status: 'error', message: 'Không tải được cockpit' } });
    if (calls === 2) return route.fulfill({ json: ok(dashboardData()) });
    await refreshGate;
    return route.fulfill({ status: 500, json: { status: 'error', message: 'Mất kết nối' } });
  });
  await page.goto('/admin');

  await expect(page.getByRole('alert')).toContainText('Không tải được cockpit');
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  await page.getByRole('button', { name: 'Làm mới' }).click();
  await expect(page.getByRole('status')).toContainText('Đang cập nhật tổng quan');
  await expect(page.getByRole('group', { name: 'Đơn đang hoạt động' })).toContainText('14');
  releaseRefresh();
  await expect(page.getByRole('alert')).toContainText('Mất kết nối');
  await expect(page.getByRole('alert')).toHaveAttribute('data-store-error', 'Mất kết nối');
  await expect(page.getByRole('group', { name: 'Đơn đang hoạt động' })).toContainText('14');
  expect(errors).toEqual([]);
});

test('partial response marks affected metrics and flow unavailable instead of rendering false zero', async ({ page }) => {
  await setup(page, dashboardData({
    netCashRevenueToday: 0,
    activeOrderCount: 0,
    pendingRefundCount: 0,
    pendingCodCount: 0,
    lowStockItemCount: 0,
    staffingGapCount: 0,
    activeOrdersByStatus: {},
    sectionAvailability: { financial: 'UNAVAILABLE', orders: 'UNAVAILABLE', refunds: 'UNAVAILABLE', cod: 'UNAVAILABLE', inventory: 'UNAVAILABLE', staffing: 'UNAVAILABLE' },
  }));
  const errors = observeBrowser(page);
  await page.goto('/admin');

  await expect(page.getByRole('status')).toContainText('Một số dữ liệu tạm thời chưa khả dụng');
  for (const [label] of metrics) await expect(page.getByRole('group', { name: label })).toContainText('Không khả dụng');
  await expect(page.getByRole('region', { name: 'Luồng đơn đang hoạt động' })).toContainText('Dữ liệu luồng đơn không khả dụng');
  await expect(page.getByRole('table', { name: 'Dữ liệu luồng đơn đang hoạt động' })).toHaveCount(0);
  expect(errors).toEqual([]);
});

test('zero state is compact and chart alternative states no active orders clearly', async ({ page }) => {
  await setup(page, dashboardData({ attentionItems: [], activeOrderCount: 0, activeOrdersByStatus: {} }));
  const errors = observeBrowser(page);
  await page.goto('/admin');

  const attention = page.getByRole('region', { name: 'Cần xử lý ngay' });
  await expect(attention).toContainText('Không có việc cần xử lý ngay');
  expect((await attention.boundingBox()).height).toBeLessThan(180);
  await expect(page.getByRole('region', { name: 'Luồng đơn đang hoạt động' })).toContainText('Không có đơn đang hoạt động');
  await expect(page.getByRole('table', { name: 'Dữ liệu luồng đơn đang hoạt động' })).toContainText('Không có đơn đang hoạt động');
  expect(errors).toEqual([]);
});
