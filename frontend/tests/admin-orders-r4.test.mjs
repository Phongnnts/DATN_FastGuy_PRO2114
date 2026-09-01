import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const page = readFileSync(new URL('../src/views/admin/OrdersPage.vue', import.meta.url), 'utf8');
const dashboard = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/admin.js', import.meta.url), 'utf8');
const policy = readFileSync(new URL('../src/utils/adminOrderWorkspace.js', import.meta.url), 'utf8');

test('R4 exposes backend-defined attention tab and reason badges', () => {
  assert.match(page, /route\.query\.status/);
  assert.match(policy, /key:\s*'ATTENTION'/);
  assert.match(page, /attentionOnly/);
  for (const reason of ['PROCESSING_OVERDUE', 'DELIVERY_FAILED', 'PENDING_REFUND']) assert.match(page, new RegExp(reason));
  assert.match(page, /Quá hạn xử lý/);
  assert.match(page, /Giao thất bại/);
  assert.match(page, /Chờ hoàn tiền/);
});

test('R4 attention requests ignore date controls and reject stale responses', () => {
  assert.match(store, /ordersRequestGeneration/);
  assert.match(store, /requestGeneration !== ordersRequestGeneration/);
  assert.match(page, /attentionOnly:\s*attentionActive\.value/);
  assert.match(page, /activeStatus\.value !== 'REFUND_PENDING'/);
  assert.match(page, /:disabled=".*attention/i);
});

test('dashboard order attention links open the unified attention tab', () => {
  assert.match(dashboard, /OVERDUE_PENDING_ORDERS[^\n]*status:\s*'ATTENTION'/);
  assert.match(dashboard, /DELIVERY_FAILED_ORDERS[^\n]*status:\s*'ATTENTION'/);
  assert.match(dashboard, /PENDING_REFUNDS[^\n]*\/admin\/refunds/);
});

test('friendly workspace exposes compact filters, lifecycle navigation, and whole-row interaction', () => {
  assert.match(page, /Theo dõi, xác nhận, giao nhận và xử lý ngoại lệ/);
  assert.match(page, /PRIMARY_ORDER_STATUSES/);
  assert.match(page, /OTHER_ORDER_STATUSES/);
  assert.match(page, />Khác/);
  assert.match(page, /role="menu"/);
  assert.match(page, /aria-haspopup="menu"/);
  assert.match(page, /class="filter-toolbar"/);
  assert.match(page, /class="advanced-filter-panel"/);
  assert.match(page, /class="active-filters"/);
  assert.match(page, /class="order-row-trigger"/);
  assert.match(page, /@keydown\.enter="openOrderFromRow/);
  assert.match(page, /@keydown\.space="openOrderFromRow/);
  assert.match(page, /event\.type === 'keydown'\) event\.preventDefault/);
  assert.doesNotMatch(page, /activeStatus === 'ATTENTION' && index === 0/);
  assert.match(page, /position:sticky/);
  assert.doesNotMatch(page, /order-shortcuts/);
});

test('friendly filters remain explicit URL-backed and removable', () => {
  assert.match(page, /Tìm mã đơn, khách hàng, SĐT/);
  assert.match(page, /advancedFiltersOpen/);
  assert.match(page, /activeOrderFilterChips/);
  assert.match(page, /removeFilter/);
  assert.match(page, /page:\s*undefined/);
  assert.match(page, /Tùy chỉnh bộ lọc/);
});

test('Khác menu supports keyboard close and focus restoration', () => {
  assert.match(page, /handleOtherMenuKeydown/);
  assert.match(page, /event\.key === 'Escape'/);
  assert.match(page, /otherTrigger\.value\?\.focus/);
  assert.match(page, /document\.addEventListener\('pointerdown'/);
});

test('queue provides equivalent semantic desktop and mobile presentations', () => {
  assert.match(page, /desktop-order-table/);
  assert.match(page, /mobile-order-list/);
  assert.match(page, /Xem chi tiết/);
  assert.match(page, /paymentMethodLabel/);
  assert.match(page, /paymentStatusLabel/);
  assert.match(page, /order\.attentionReasons/);
  assert.doesNotMatch(page.match(/<table[\s\S]*?<\/table>/)?.[0] || '', /customerAddress/);
});

test('silent refresh retains canonical rows and announces progress or warning', () => {
  assert.match(page, /const refreshing = ref\(false\)/);
  assert.match(page, /refreshing\.value = silent/);
  assert.match(page, /Đang cập nhật danh sách/);
  assert.match(page, /Dữ liệu gần nhất vẫn được giữ lại/);
  assert.match(page, /table-skeleton/);
});

test('drawer detail and mutation work accept only current request ownership', () => {
  assert.match(page, /detailRequestGeneration/);
  assert.match(page, /requestGeneration !== detailRequestGeneration/);
  assert.match(page, /stopped/);
  assert.match(page, /\+\+detailRequestGeneration/);
});

test('drawer mutations use latest allowed action and expected status', () => {
  assert.match(page, /adminApi\.updateOrderStatus\(selectedOrder\.value\.orderId,\s*\{[\s\S]*expectedStatus:\s*selectedOrder\.value\.status[\s\S]*status:\s*pendingAction\.value/);
  assert.match(page, /adminApi\.cancelOrder\(selectedOrder\.value\.orderId,\s*\{[\s\S]*expectedStatus:\s*selectedOrder\.value\.status[\s\S]*reason:\s*actionNote\.value\.trim\(\)/);
  assert.match(page, /inlineOrderActions\(selectedOrder\.value\?\.allowedActions\)/);
  assert.match(page, /error\.status === 409/);
  assert.match(page, /loadOrders\(\{ silent: true \}\)/);
});

test('list view state accepts only the newest request and clears stale rows for blocking loads', () => {
  assert.match(page, /listRequestGeneration/);
  assert.match(page, /requestGeneration !== listRequestGeneration/);
  assert.match(page, /if \(requestGeneration === listRequestGeneration\)/);
  assert.match(page, /if \(!silent\) adminStore\.allOrders = \[\]/);
});

test('refund attention removes contradictory refund filters', () => {
  assert.match(page, /activeStatus\.value === 'REFUND_PENDING' && route\.query\.refundStatus !== undefined/);
  assert.match(page, /refundStatus:\s*undefined/);
  assert.match(page, /refundStatus\.value = activeStatus\.value === 'REFUND_PENDING' \? 'PENDING'/);
});

test('status tabs use a complete manual keyboard model and controlled panel', () => {
  assert.match(page, /:tabindex="isOtherOrderStatus\(activeStatus\) \? 0 : -1"/);
  assert.match(page, /selectOtherStatus/);
  assert.match(page, /event\.key === 'ArrowRight'/);
  assert.match(page, /event\.key === 'ArrowLeft' && index === 0/);
  assert.match(page, /role="tabpanel"/);
  assert.match(page, /aria-controls="order-queue-panel"/);
  assert.match(page, /closeOtherMenu\(\{ restoreFocus: true \}\)/);
});

test('mobile detail actions include their order code in the accessible name', () => {
  assert.match(page, /:aria-label="`Xem chi tiết đơn hàng \$\{order\.orderCode\}`" class="mobile-detail-action"/);
});
