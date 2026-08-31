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

const metrics = [
  { section: 'financial', label: 'Doanh thu thuần hôm nay', value: '420.000₫' },
  { section: 'orders', label: 'Đơn đang hoạt động', value: '14' },
  { section: 'refunds', label: 'Hoàn tiền chờ xử lý', value: '3' },
  { section: 'cod', label: 'COD chờ xác nhận', value: '4' },
  { section: 'inventory', label: 'Mặt hàng sắp hết', value: '5' },
  { section: 'staffing', label: 'Ca thiếu nhân sự', value: '2' },
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
  return { method: request.method(), path: url.pathname, query: [...url.searchParams.entries()].sort(([left], [right]) => left.localeCompare(right)) };
}

async function mockDestinations(page) {
  const calls = { orders: [], refunds: [], monitoring: [], inventory: [], cod: [] };
  const exact = (name, expected, data) => route => {
    const evidence = requestEvidence(route.request());
    calls[name].push(evidence);
    return JSON.stringify(evidence) === JSON.stringify(expected)
      ? route.fulfill({ json: ok(data) })
      : route.fulfill({ status: 501, json: { status: 'error', message: `Unexpected ${name} request` } });
  };
  await page.route('**/api/admin/orders*', exact('orders', { method: 'GET', path: '/api/admin/orders', query: [['attentionOnly', 'true']] }, []));
  await page.route('**/api/admin/refunds*', exact('refunds', { method: 'GET', path: '/api/admin/refunds', query: [] }, []));
  await page.route('**/api/admin/users*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/shifts/week*', route => route.fulfill({ json: ok({ weekStart: '2026-08-31', shifts: [] }) }));
  await page.route('**/api/admin/shifts/monitoring*', exact('monitoring', { method: 'GET', path: '/api/admin/shifts/monitoring', query: [] }, []));
  await page.route('**/api/admin/inventory/items*', exact('inventory', { method: 'GET', path: '/api/admin/inventory/items', query: [] }, []));
  await page.route('**/api/admin/inventory/transactions*', route => route.fulfill({ json: ok({ items: [], totalItems: 0, totalPages: 0 }) }));
  await page.route('**/api/admin/products*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/cod-settlements/admin*', exact('cod', { method: 'GET', path: '/api/cod-settlements/admin', query: [['status', 'SUBMITTED']] }, []));
  return calls;
}

async function setup(page, data = dashboardData()) {
  await authenticate(page);
  const calls = await mockDestinations(page);
  await page.route('**/api/admin/dashboard*', route => route.fulfill({ json: ok(data) }));
  return calls;
}

async function clickAndCapture(page, linkName, apiPath) {
  const requestPromise = page.waitForRequest(request => new URL(request.url()).pathname === apiPath);
  await page.getByRole('link', { name: new RegExp(linkName) }).click();
  return requestEvidence(await requestPromise);
}

test('dashboard hierarchy, canonical metrics, attention routes, and chart data are decision-first', async ({ page }) => {
  await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin');

  await expect(page.getByRole('heading', { name: 'Hoạt động hôm nay' })).toBeVisible();
  const sections = page.locator('[data-dashboard-section]');
  await expect(sections).toHaveCount(5);
  expect(await sections.evaluateAll(nodes => nodes.map(node => node.dataset.dashboardSection))).toEqual(['header', 'attention', 'metrics', 'flow', 'flow-data']);
  await expect(page.getByRole('heading', { name: 'Cần xử lý ngay' })).toBeVisible();

  for (const metric of metrics) await expect(page.getByRole('group', { name: metric.label })).toContainText(metric.value);

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

test('attention destinations issue each exact critical request once', async ({ page }) => {
  const calls = await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin');

  expect(await clickAndCapture(page, 'Đơn chờ xác nhận quá lâu', '/api/admin/orders')).toEqual({ method: 'GET', path: '/api/admin/orders', query: [['attentionOnly', 'true']] });
  await expect(page.getByRole('tab', { name: /Cần xử lý/ })).toHaveAttribute('aria-selected', 'true');
  expect(calls.orders).toEqual([{ method: 'GET', path: '/api/admin/orders', query: [['attentionOnly', 'true']] }]);

  await page.goto('/admin');
  expect(await clickAndCapture(page, 'Yêu cầu hoàn tiền đang chờ', '/api/admin/refunds')).toEqual({ method: 'GET', path: '/api/admin/refunds', query: [] });
  await expect(page.getByRole('button', { name: /Chờ hoàn/ })).toHaveAttribute('aria-pressed', 'true');
  expect(calls.refunds).toEqual([{ method: 'GET', path: '/api/admin/refunds', query: [] }]);

  await page.goto('/admin');
  expect(await clickAndCapture(page, 'Ca làm cần bổ sung nhân viên', '/api/admin/shifts/monitoring')).toEqual({ method: 'GET', path: '/api/admin/shifts/monitoring', query: [] });
  await expect(page.getByRole('tab', { name: 'Giám sát' })).toHaveAttribute('aria-selected', 'true');
  expect(calls.monitoring).toEqual([{ method: 'GET', path: '/api/admin/shifts/monitoring', query: [] }]);

  await page.goto('/admin');
  expect(await clickAndCapture(page, 'Mặt hàng dưới mức an toàn', '/api/admin/inventory/items')).toEqual({ method: 'GET', path: '/api/admin/inventory/items', query: [] });
  await expect(page.getByLabel('Trạng thái')).toHaveValue('LOW');
  expect(calls.inventory).toEqual([{ method: 'GET', path: '/api/admin/inventory/items', query: [] }]);

  await page.goto('/admin');
  expect(await clickAndCapture(page, 'Bàn giao COD đang chờ', '/api/cod-settlements/admin')).toEqual({ method: 'GET', path: '/api/cod-settlements/admin', query: [['status', 'SUBMITTED']] });
  await expect(page.locator('#cod-status-filter')).toHaveValue('SUBMITTED');
  expect(calls.cod).toEqual([{ method: 'GET', path: '/api/cod-settlements/admin', query: [['status', 'SUBMITTED']] }]);

  expect(errors).toEqual([]);
});

test('destination query values reject invalid input safely', async ({ page }) => {
  await setup(page);
  const errors = observeBrowser(page);

  await page.goto('/admin/inventory?filter=INVALID&tab=history');
  await expect(page).toHaveURL(/tab=history/);
  await page.getByRole('tab', { name: 'Tồn hiện tại' }).click();
  await expect(page.getByLabel('Trạng thái')).toHaveValue('ALL');
  await expect(page).toHaveURL(/filter=INVALID/);

  await page.goto('/admin/cod-settlements?status=INVALID');
  await expect(page.locator('#cod-status-filter')).toHaveValue('SUBMITTED');
  await expect(page).toHaveURL(/status=INVALID/);
  expect(errors).toEqual([]);
});

test('Inventory Back and Forward restore filter history without dropping tab', async ({ page }) => {
  await setup(page);
  const errors = observeBrowser(page);
  await page.goto('/admin/inventory?tab=current&filter=LOW');
  const filter = page.getByLabel('Trạng thái');
  await expect(filter).toHaveValue('LOW');

  await filter.selectOption('OUT');
  await expect(page).toHaveURL(/tab=current/);
  await expect(page).toHaveURL(/filter=OUT/);
  await expect(filter).toHaveValue('OUT');

  await page.goBack();
  await expect(page).toHaveURL(/tab=current/);
  await expect(page).toHaveURL(/filter=LOW/);
  await expect(filter).toHaveValue('LOW');

  await page.goForward();
  await expect(page).toHaveURL(/tab=current/);
  await expect(page).toHaveURL(/filter=OUT/);
  await expect(filter).toHaveValue('OUT');
  expect(errors).toEqual([]);
});

test('initial loading uses a layout-matched skeleton and forbidden has a permission state', async ({ page }) => {
  await authenticate(page);
  const errors = observeBrowser(page, { expectedStatuses: [403] });
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
  const errors = observeBrowser(page, { expectedStatuses: [500] });
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

for (const unavailable of Object.keys(availableSections)) {
  test(`partial response isolates ${unavailable} unavailability`, async ({ page }) => {
    const sectionAvailability = { ...availableSections, [unavailable]: 'UNAVAILABLE' };
    expect(Object.values(sectionAvailability).filter(value => value === 'UNAVAILABLE')).toHaveLength(1);
    await setup(page, dashboardData({ sectionAvailability }));
    const errors = observeBrowser(page);
    await page.goto('/admin');

    await expect(page.getByRole('status')).toContainText('Một số dữ liệu tạm thời chưa khả dụng');
    for (const metric of metrics) {
      const group = page.getByRole('group', { name: metric.label });
      if (metric.section === unavailable) await expect(group).toContainText('Không khả dụng');
      else {
        await expect(group).toContainText(metric.value);
        await expect(group).not.toContainText('Không khả dụng');
      }
    }
    const flowRegion = page.getByRole('region', { name: 'Luồng đơn đang hoạt động' });
    if (unavailable === 'orders') {
      await expect(flowRegion).toContainText('Dữ liệu luồng đơn không khả dụng');
      await expect(page.getByRole('table', { name: 'Dữ liệu luồng đơn đang hoạt động' })).toHaveCount(0);
    } else {
      await expect(flowRegion).toContainText('14 đơn');
      const table = page.getByRole('table', { name: 'Dữ liệu luồng đơn đang hoạt động' });
      for (const [label, value] of flow) await expect(table.getByRole('row', { name: new RegExp(`${label} ${value}`) })).toBeVisible();
    }
    expect(errors).toEqual([]);
  });
}

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
