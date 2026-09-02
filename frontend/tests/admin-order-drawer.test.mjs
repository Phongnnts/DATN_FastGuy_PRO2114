import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const drawer = readFileSync(new URL('../src/components/admin/AdminOrderDrawer.vue', import.meta.url), 'utf8');
const page = readFileSync(new URL('../src/views/admin/OrdersPage.vue', import.meta.url), 'utf8');

test('drawer renders only contracted detail sections and truthful payment totals', () => {
  for (const field of ['customerName', 'customerPhone', 'customerAddress', 'deliveryNote', 'staffName', 'shipperName', 'items', 'totalAmount', 'shippingFee', 'discountAmount', 'finalAmount', 'statusHistory']) assert.match(drawer, new RegExp(`order\\.${field}`));
  assert.match(drawer, /order-drawer-identity/);
  assert.match(drawer, /order-drawer-fulfillment/);
  assert.match(drawer, /order-drawer-items/);
  assert.match(drawer, /order-drawer-payment/);
  assert.match(drawer, /order-drawer-timeline/);
  assert.match(drawer, /order-drawer-actions/);
  assert.ok(drawer.indexOf('order-drawer-identity') < drawer.indexOf('order-drawer-fulfillment'));
  assert.ok(drawer.indexOf('order-drawer-fulfillment') < drawer.indexOf('order-drawer-items'));
  assert.ok(drawer.indexOf('order-drawer-items') < drawer.indexOf('order-drawer-payment'));
  assert.ok(drawer.indexOf('order-drawer-payment') < drawer.indexOf('order-drawer-timeline'));
  assert.ok(drawer.indexOf('order-drawer-timeline') < drawer.indexOf('order-drawer-actions'));
  assert.match(drawer, /\.order-modal :is\(button,a,textarea\)[^{]*\{[^}]*min-height:40px/);
  assert.match(drawer, /\.order-modal-close\{[^}]*width:44px[^}]*height:44px/);
  assert.match(drawer, /OrderTimeline/);
  assert.match(drawer, /imageUrl/);
  assert.doesNotMatch(drawer, /ETA|estimatedDelivery|serviceFee/);
});

test('drawer is modal, focus-contained, escapable, and responsive', () => {
  assert.match(drawer, /role="dialog"/);
  assert.match(drawer, /aria-modal="true"/);
  assert.match(drawer, /event\.key === 'Escape'/);
  assert.match(drawer, /event\.key !== 'Tab'/);
  assert.match(drawer, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(drawer, /order-modal-close/);
  assert.match(drawer, /class="order-modal-backdrop"/);
  assert.match(drawer, /class="order-modal"/);
  assert.match(drawer, /width:880px/);
  assert.match(drawer, /max-width:calc\(100vw - 48px\)/);
  assert.match(drawer, /max-height:85dvh/);
  assert.match(drawer, /@media\(max-width:640px\)/);
  assert.match(drawer, /height:100dvh/);
  assert.doesNotMatch(drawer, /justify-content:flex-end/);
});

test('drawer actions are controlled by canonical allowedActions and current detail before mutation', () => {
  const selectAction = page.match(/function selectDrawerAction\(action\) \{[\s\S]*?\n\}/)?.[0] || '';
  const confirmAction = page.match(/async function confirmDrawerAction\(\) \{[\s\S]*?\n\}/)?.[0] || '';
  assert.match(drawer, /const actions = computed\(\(\) => inlineOrderActions\(props\.order\?\.allowedActions\)\)/);
  assert.match(drawer, /actionStillAllowed = computed\(\(\) => actions\.value\.some\(action => action\.key === props\.pendingAction\)\)/);
  assert.match(selectAction, /selectedAllowedActionKeys\(\)\.has\(action\)/);
  assert.match(confirmAction, /!selectedOrder\.value/);
  assert.match(confirmAction, /!selectedAllowedActionKeys\(\)\.has\(pendingAction\.value\)/);
  assert.match(confirmAction, /expectedStatus: selectedOrder\.value\.status/);
  assert.match(drawer, /requiresNote/);
  assert.match(drawer, /confirm-action/);
  assert.match(drawer, /\/admin\/refunds/);
  assert.match(drawer, /Mở trang đầy đủ/);
});

test('OrdersPage delegates drawer presentation without moving request ownership', () => {
  assert.match(page, /AdminOrderDrawer/);
  assert.match(page, /adminApi\.getOrderById/);
  assert.match(page, /detailTrigger/);
  assert.match(page, /detailTriggerOrderId/);
  assert.match(page, /data-order-id/);
});

test('OrdersPage consumes positive orderId deep-links and removes only orderId on close', () => {
  assert.match(page, /function routeOrderId\(\)/);
  assert.match(page, /parseOrderIdQuery\(route\.query\.orderId\)/);
  assert.match(page, /router\.replace\(\{ query: \{ \.\.\.route\.query, orderId: undefined \} \}\)/);
});

test('conflict presentation is controlled so cancellation reason survives canonical reload', () => {
  assert.match(drawer, /:value="actionNote"/);
  assert.match(drawer, /update:action-note/);
  assert.match(drawer, /actionError/);
  assert.doesNotMatch(drawer, /actionNote\s*=\s*ref/);
});

test('canonical recovery failures remain contained in the drawer', () => {
  assert.match(page, /reloadCanonicalOrder/);
  assert.match(page, /selectedOrder\.value = null;[\s\S]*loadOrderDetail\(orderId\)/);
  assert.match(page, /Không thể tải lại dữ liệu mới nhất/);
  assert.doesNotMatch(page, /await Promise\.all\(\[loadOrderDetail\(orderId\), loadOrders/);
});
