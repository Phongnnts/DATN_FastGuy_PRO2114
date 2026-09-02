import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { dashboardViewState } from '../src/utils/adminDashboardViewState.js';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const dashboard = read('../src/views/admin/DashboardPage.vue');
const shifts = read('../src/views/admin/ShiftsPage.vue');
const inventory = read('../src/views/admin/InventoryPage.vue');
const cod = read('../src/views/admin/CodSettlementsPage.vue');
const openapi = read('../../openapi/fastguy.yaml');

function ordered(source, values) {
  let cursor = -1;
  for (const value of values) {
    const next = source.indexOf(value);
    assert.ok(next > cursor, `${value} must appear after the previous dashboard section`);
    cursor = next;
  }
}

function schemaSource(contract, name) {
  const marker = `    ${name}:`;
  const start = contract.indexOf(marker);
  assert.notEqual(start, -1, `${name} schema must exist`);
  const tail = contract.slice(start + marker.length);
  const nextSchema = tail.search(/\r?\n    [A-Za-z][A-Za-z0-9]*:\r?\n/);
  return tail.slice(0, nextSchema === -1 ? undefined : nextSchema);
}

function deprecatedDashboardProperties(contract) {
  const schema = schemaSource(contract, 'AdminDashboardData');
  const properties = [...schema.matchAll(/^        ([A-Za-z][A-Za-z0-9]*):/gm)];
  return properties
    .filter((match, index) => {
      const end = properties[index + 1]?.index ?? schema.length;
      return /\bdeprecated:\s*true\b/.test(schema.slice(match.index, end));
    })
    .map(match => match[1]);
}

test('R3 dashboard visible business values use only contracted dashboard fields', () => {
  const dashboardSchema = schemaSource(openapi, 'AdminDashboardData');
  const sourcePolicy = {
    netCashRevenueToday: 'Doanh thu thuần hôm nay',
    operationalOrderCountToday: 'Đơn hàng hôm nay',
    aovToday: 'Giá trị đơn trung bình',
    completionRateToday: 'Tỷ lệ hoàn thành',
    revenueLast7Days: 'Doanh thu 7 ngày',
    activeOrderCount: 'Trạng thái đơn hàng',
    activeOrdersByStatus: 'Trạng thái đơn hàng',
    topProductsLast7Days: 'Món bán chạy',
    lowStockProducts: 'Món sắp tạm hết',
    attentionItems: 'Cần xử lý',
  };

  for (const [field, label] of Object.entries(sourcePolicy)) {
    assert.match(dashboardSchema, new RegExp(`^        ${field}:`, 'm'), `${label} must have contracted AdminDashboardData field ${field}`);
    assert.match(dashboard, new RegExp(`data(?:\\.value)?\\?\\.${field}|data\\.${field}`), `${label} must read contracted source field ${field}`);
  }
});

test('R3 dashboard derives partial state from every contracted availability section', () => {
  const availabilitySchema = schemaSource(openapi, 'AdminDashboardSectionAvailability');
  for (const section of ['financial', 'orders', 'refunds', 'cod', 'inventory', 'staffing']) {
    assert.match(availabilitySchema, new RegExp(`^        ${section}: \\{ type: string, enum: \\[AVAILABLE, UNAVAILABLE\\] \\}`, 'm'));
    assert.equal(dashboardViewState({}, 'ready', null, { [section]: 'UNAVAILABLE' }), 'partial');
  }
});

test('R3 dashboard has independent presentation branches for currently rendered sections', () => {
  assert.match(dashboard, /available\('financial'\)[^]*data\.netCashRevenueToday/);
  assert.match(dashboard, /available\('orders'\)[^]*data\.operationalOrderCountToday/);
  assert.match(dashboard, /available\('inventory'\)[^]*lowStockProducts/);
});

test('R3 dashboard excludes uncontracted profitability and period-comparison claims', () => {
  assert.doesNotMatch(dashboard, /lợi nhuận|biên lợi nhuận|so với (hôm qua|kỳ trước)|tăng so với|giảm so với/i);
});

test('Operations Studio dashboard establishes the approved business hierarchy', () => {
  ordered(dashboard, ['business-health-header', 'business-performance-workspace', 'cash-risk-rail']);
});

test('Operations Studio cash-risk rail branches on refund availability', () => {
  assert.match(dashboard, /data-cash-risk-section="refunds"[^]*available\('refunds'\)/, 'cash-risk refunds branch must expose unavailable state');
});

test('Operations Studio cash-risk rail branches on COD availability', () => {
  assert.match(dashboard, /data-cash-risk-section="cod"[^]*available\('cod'\)/, 'cash-risk COD branch must expose unavailable state');
});

test('Operations Studio cash-risk rail branches on staffing availability', () => {
  assert.match(dashboard, /data-cash-risk-section="staffing"[^]*available\('staffing'\)/, 'cash-risk staffing branch must expose unavailable state');
});

test('R3 dashboard source follows the approved cockpit section order', () => {
  ordered(dashboard, ['<header class="operations-hero"', 'class="dashboard-kpi-grid', 'class="primary-operations-grid"', 'id="revenue-title">Doanh thu 7 ngày', 'id="attention-title">Cần xử lý', 'class="secondary-insights-grid"', 'id="status-title">Trạng thái đơn hàng', 'id="products-title">Món bán chạy', 'id="stock-title">Món sắp tạm hết', 'class="panel priority-workspace']);
  assert.doesNotMatch(dashboard, /TRUNG TÂM ĐIỀU HÀNH|Ưu tiên xử lý/);
});

test('dashboard uses the approved Apple-inspired hierarchy without decorative fake data', () => {
  for (const className of ['operations-hero', 'dashboard-kpi-grid', 'primary-operations-grid', 'secondary-insights-grid', 'revenue-panel', 'attention-panel', 'status-panel', 'products-panel', 'stock-panel', 'priority-workspace']) {
    assert.match(dashboard, new RegExp(className));
  }
  assert.match(dashboard, /FASTGUY LIVE OPERATIONS/);
  assert.match(dashboard, /aria-label="Chỉ số điều hành hôm nay"/);
  assert.match(dashboard, /grid-template-columns:repeat\(12,minmax\(0,1fr\)\)/);
  assert.doesNotMatch(dashboard, /sparkline|fakeTrend|notification-bell|global-search|so với hôm qua/);
});

test('R3 attention maps all six types to exact destination queries', () => {
  for (const type of ['OVERDUE_PENDING_ORDERS', 'DELIVERY_FAILED_ORDERS', 'PENDING_REFUNDS', 'STAFF_COVERAGE_GAPS', 'LOW_STOCK_ITEMS', 'PENDING_COD_SETTLEMENTS']) assert.match(dashboard, new RegExp(type));
  for (const destination of [
    "/admin/orders', query: { status: 'ATTENTION' }",
    "/admin/refunds', query: { status: 'PENDING' }",
    "/admin/shifts', query: { tab: 'monitoring' }",
    "/admin/inventory', query: { filter: 'LOW' }",
    "/admin/cod-settlements', query: { status: 'SUBMITTED' }",
  ]) assert.match(dashboard, new RegExp(destination.replace(/[/?{}']/g, '\\$&')));
  assert.match(dashboard, /Không có việc cần xử lý ngay/);
});

test('R3 dashboard renders three statistical charts with semantic alternatives', () => {
  assert.match(dashboard, /activeOrdersByStatus/);
  assert.match(dashboard, /revenueChartRef/);
  assert.match(dashboard, /statusChartRef/);
  assert.match(dashboard, /topProductsChartRef/);
  assert.match(dashboard, /type: 'line'/);
  assert.match(dashboard, /type: 'doughnut'/);
  assert.match(dashboard, /indexAxis: 'y'/);
  for (const label of ['Doanh thu 7 ngày', 'Trạng thái đơn hàng', 'Món bán chạy', 'Món sắp tạm hết']) assert.match(dashboard, new RegExp(label));
  assert.match(dashboard, /prefers-reduced-motion/);
  assert.match(dashboard, /const animation = reducedMotion \? false/);
  for (const [status, label] of Object.entries({ PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', ASSIGNED: 'Đã gán shipper', PICKED_UP: 'Đang giao', DELIVERY_FAILED: 'Giao thất bại' })) {
    assert.match(dashboard, new RegExp(`${status}[^\\n]+${label}`));
  }
  for (const terminal of ['DELIVERED', 'CANCELLED', 'RETURNED_TO_STORE']) assert.doesNotMatch(dashboard, new RegExp(`${terminal}:`));
});

test('R3 dashboard exposes Task 5 states, store error, unavailable sections, and matched skeletons', () => {
  for (const state of ['loading', 'ready', 'refreshing', 'partial', 'forbidden', 'error']) assert.match(dashboard, new RegExp(`['"]${state}['"]`));
  assert.match(dashboard, /adminStore\.error/);
  assert.match(dashboard, /sectionAvailability/);
  assert.match(dashboard, /Không khả dụng/);
  assert.match(dashboard, /dashboard-skeleton/);
  assert.match(dashboard, /skeleton-attention/);
  assert.match(dashboard, /skeleton-metrics/);
  assert.match(dashboard, /skeleton-flow/);
});

test('dashboard priority preview uses the contracted order queue without polluting Orders store state', () => {
  assert.match(dashboard, /adminApi\.getOrders/);
  assert.match(dashboard, /attentionOnly:\s*true/);
  assert.match(dashboard, /sort:\s*'WAITING_DESC'/);
  assert.match(dashboard, /pageSize:\s*8/);
  assert.match(dashboard, /priorityOrders/);
  assert.match(dashboard, /Array\.isArray\(result\?\.items\)\s*\?\s*result\.items\.slice\(0, 8\)\s*:\s*Array\.isArray\(result\)\s*\?\s*result\.slice\(0, 8\)\s*:\s*\[\]/);
  assert.doesNotMatch(dashboard, /adminStore\.fetchOrders/);
  assert.match(dashboard, /Đơn cần ưu tiên/);
  assert.match(dashboard, /Xem tất cả/);
});

test('R3 destination pages validate and synchronize exact query enums', () => {
  assert.match(shifts, /useRoute/);
  assert.match(shifts, /useRouter/);
  assert.match(shifts, /\['schedule', 'monitoring'\]/);
  assert.match(shifts, /watch\(\(\) => route\.query\.tab/);
  assert.match(shifts, /router\.replace/);

  assert.match(inventory, /STATUS_FILTERS\.some/);
  assert.match(inventory, /route\.query\.filter/);
  assert.match(inventory, /watch\(\(\) => route\.query\.filter/);
  assert.match(inventory, /watch\(statusFilter/);
  assert.match(inventory, /router\.push\(\{ query: \{ \.\.\.route\.query, filter:/);
  assert.match(inventory, /'ALL'/);

  assert.match(cod, /useRoute/);
  assert.match(cod, /useRouter/);
  assert.match(cod, /\['SUBMITTED', 'SHORT', 'OVER', 'SETTLED'\]/);
  assert.match(cod, /route\.query\.status/);
  assert.match(cod, /watch\(\(\) => route\.query\.status/);
  assert.match(cod, /router\.replace/);
});
