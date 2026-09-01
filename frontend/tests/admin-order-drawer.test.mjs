import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const drawer = readFileSync(new URL('../src/components/admin/AdminOrderDrawer.vue', import.meta.url), 'utf8');
const page = readFileSync(new URL('../src/views/admin/OrdersPage.vue', import.meta.url), 'utf8');

test('drawer renders only contracted detail sections and truthful payment totals', () => {
  for (const field of ['customerName', 'customerPhone', 'customerAddress', 'deliveryNote', 'staffName', 'shipperName', 'items', 'totalAmount', 'shippingFee', 'discountAmount', 'finalAmount', 'statusHistory']) assert.match(drawer, new RegExp(`order\\.${field}`));
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
  assert.match(drawer, /order-drawer-close/);
  assert.match(drawer, /width:min\(460px,100%\)/);
  assert.match(drawer, /@media\(max-width:640px\)/);
});

test('drawer actions are controlled by allowedActions and keep refund processing external', () => {
  assert.match(drawer, /inlineOrderActions\(props\.order\?\.allowedActions\)/);
  assert.match(drawer, /pendingAction/);
  assert.match(drawer, /requiresNote/);
  assert.match(drawer, /confirm-action/);
  assert.match(drawer, /\/admin\/refunds/);
  assert.match(drawer, /Mở trang đầy đủ/);
});

test('OrdersPage delegates drawer presentation without moving request ownership', () => {
  assert.match(page, /AdminOrderDrawer/);
  assert.match(page, /adminApi\.getOrderById/);
  assert.match(page, /detailTrigger/);
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
