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

test('R3 dashboard uses only the six canonical operating metrics and consumes no deprecated contract property', () => {
  for (const field of ['netCashRevenueToday', 'activeOrderCount', 'pendingRefundCount', 'pendingCodCount', 'lowStockItemCount', 'staffingGapCount']) assert.match(dashboard, new RegExp(`data\\.${field}`));
  const deprecated = deprecatedDashboardProperties(openapi);
  assert.ok(deprecated.length > 0, 'AdminDashboardData must expose deprecated compatibility properties');
  const dashboardConsumer = dashboard.slice(0, dashboard.indexOf('<style scoped>'));
  for (const field of deprecated) assert.doesNotMatch(dashboardConsumer, new RegExp(`\\b${field}\\b`), `DashboardPage consumes deprecated AdminDashboardData.${field}`);
  for (const label of ['Doanh thu thuần hôm nay', 'Đơn đang hoạt động', 'Hoàn tiền chờ xử lý', 'COD chờ xác nhận', 'Mặt hàng sắp hết', 'Ca thiếu nhân sự']) assert.match(dashboard, new RegExp(label));
});

test('R3 dashboard source follows the decision-first section order', () => {
  ordered(dashboard, ['<header class="dashboard-heading"', 'id="attention-title">Cần xử lý ngay', 'class="operating-metrics"', 'id="active-flow-title"', 'class="flow-data"']);
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

test('R3 dashboard keeps one active-order chart with an equivalent semantic table', () => {
  assert.match(dashboard, /activeOrdersByStatus/);
  assert.match(dashboard, /activeOrderChartRef/);
  assert.match(dashboard, /<table[^>]*aria-label="Dữ liệu luồng đơn đang hoạt động"/);
  assert.match(dashboard, /v-for="item in activeOrderSeries"/);
  assert.match(dashboard, /aria-describedby="active-flow-data"/);
  assert.match(dashboard, /prefers-reduced-motion/);
  assert.match(dashboard, /animation: reducedMotion/);
  assert.doesNotMatch(dashboard, /revenueChartRef|topChartRef|statusChartRef/);
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
