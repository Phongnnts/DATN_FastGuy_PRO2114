import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

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

function deprecatedDashboardProperties(contract) {
  const marker = '    AdminDashboardData:';
  const start = contract.indexOf(marker);
  assert.notEqual(start, -1, 'AdminDashboardData schema must exist');
  const tail = contract.slice(start + marker.length);
  const nextSchema = tail.search(/\r?\n    [A-Za-z][A-Za-z0-9]*:\r?\n/);
  const schema = tail.slice(0, nextSchema === -1 ? undefined : nextSchema);
  const properties = [...schema.matchAll(/^        ([A-Za-z][A-Za-z0-9]*):/gm)];
  return properties
    .filter((match, index) => {
      const end = properties[index + 1]?.index ?? schema.length;
      return /\bdeprecated:\s*true\b/.test(schema.slice(match.index, end));
    })
    .map(match => match[1]);
}

test('R3 dashboard uses the four approved cockpit KPIs and new canonical analytics', () => {
  for (const field of ['netCashRevenueToday', 'operationalOrderCountToday', 'aovToday', 'completionRateToday', 'revenueLast7Days', 'topProductsLast7Days', 'lowStockProducts']) assert.match(dashboard, new RegExp(`data(?:\\.value)?\\?\\.${field}|data\\.${field}`));
  for (const label of ['Doanh thu thuần hôm nay', 'Đơn hàng hôm nay', 'Giá trị đơn trung bình', 'Tỷ lệ hoàn thành']) assert.match(dashboard, new RegExp(label));
});

test('R3 dashboard source follows the approved cockpit section order', () => {
  ordered(dashboard, ['<header class="dashboard-heading"', 'class="operating-metrics', 'id="revenue-title">Doanh thu 7 ngày', 'id="attention-title">Cần xử lý', 'id="status-title">Trạng thái đơn hàng', 'id="products-title">Món bán chạy', 'id="stock-title">Món sắp tạm hết']);
  assert.doesNotMatch(dashboard, /TRUNG TÂM ĐIỀU HÀNH|Ưu tiên xử lý|<p class="eyebrow"/);
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
