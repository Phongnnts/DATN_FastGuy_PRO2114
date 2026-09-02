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
  assert.match(page, /class="[^"]*\bfilter-toolbar\b[^"]*"/);
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

test('Orders uses the approved full-width pale queue and centered detail modal', () => {
  assert.match(page, /class="panel queue-workspace orders-light-workspace"/);
  assert.doesNotMatch(page, /orders-desktop-split|class="order-inspector"|desktopInspector/);
  assert.match(page, /order-row-selected/);
  assert.match(page, /\.orders-light-workspace/);
  assert.match(page, /\.status-semantic-dot/);
  assert.match(page, /\.status-semantic-chip/);
  assert.match(page, /<AdminOrderDrawer :open=/);
});

test('Orders applies the complete Notion warm-paper visual contract', () => {
  assert.match(page, /--orders-paper:#f6f5f4/);
  assert.match(page, /--brand-primary:#FF6846/);
  assert.match(page, /font-family:Inter,-apple-system/);
  assert.match(page, /\.orders-light-workspace\{[^}]*border-radius:16px[^}]*box-shadow:none/);
  assert.match(page, /\.orders-light-workspace \.status-segments>button[^}]*border-radius:8px/);
  assert.match(page, /\.desktop-order-table thead\{[^}]*top:0/);
  assert.doesNotMatch(page, /\.orders-light-workspace[^\n]*gradient/);
  assert.match(page, /const pageSize = 10/);
  assert.match(page, /\.orders-light-workspace\{[^}]*border-radius:16px/);
  assert.match(page, /\.orders-light-workspace :is\(\.form-input,\.form-select,\.advanced-filter-trigger,\.btn\)\{[^}]*border-radius:10px/);
  assert.match(page, /\.desktop-order-table tbody tr\{[^}]*height:72px/);
  for (const token of ['--brand-primary:#FF6846', '--brand-primary-hover:#F85B38', '--brand-primary-active:#E94F30', '--brand-soft:#FFF1EC', '--brand-border:#FFD8CC', '--brand-text:#E95635']) assert.match(page, new RegExp(token));
  assert.match(page, /box-shadow:0 2px 5px rgba\(255,104,70,\.16\),0 0 14px rgba\(255,104,70,\.10\)/);
  assert.match(page, /class="waiting-primary"/);
  assert.match(page, /class="waiting-secondary"/);
  assert.match(page, /formatTime\(order\.createdAt\)/);
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
  const table = page.match(/<table[\s\S]*?<\/table>/)?.[0] || '';
  const cards = page.match(/<div class="mobile-order-list"[\s\S]*?<footer class="table-footer">/)?.[0] || '';
  assert.match(page, /class="orders-header orders-command-header"/);
  assert.match(page, /class="filter-toolbar orders-toolbar"/);
  assert.match(page, /class="orders-queue"/);
  assert.match(table, /<th>Thao tác tiếp theo<\/th>/);
  assert.equal((table.match(/class="btn btn-primary row-primary-action"/g) || []).length, 1);
  assert.equal((cards.match(/class="mobile-detail-action mobile-primary-action"/g) || []).length, 1);
  assert.match(table, /v-if="inlineOrderActions\(order\.allowedActions\)\[0\]"[\s\S]*v-else[\s\S]*class="btn btn-outline row-detail-action"/);
  assert.match(cards, /v-if="inlineOrderActions\(order\.allowedActions\)\[0\]"[\s\S]*v-else[\s\S]*class="mobile-detail-action"/);
  assert.match(table, /@click="openPrimaryAction\(order, \$event\)"/);
  assert.match(cards, /@click="openPrimaryAction\(order, \$event\)"/);
  assert.match(page, /paymentMethodLabel/);
  assert.match(page, /paymentStatusLabel/);
  assert.match(page, /order\.attentionReasons/);
  assert.doesNotMatch(table, /customerAddress/);
});

test('touched queue controls guarantee minimum pointer targets', () => {
  assert.match(page, /\.order-link,\.refund-action,\.row-primary-action,\.row-detail-action\{[^}]*min-height:40px/);
  assert.match(page, /\.order-link,\.refund-action\{[^}]*min-width:40px/);
  assert.match(page, /\.pagination button\s*\{[^}]*height:\s*40px[^}]*width:\s*40px/);
  assert.match(page, /\.mobile-detail-action\{[^}]*min-height:44px/);
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
