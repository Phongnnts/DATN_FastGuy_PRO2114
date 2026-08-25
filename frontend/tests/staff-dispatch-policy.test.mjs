import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8');
const layout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');
const dispatch = readFileSync(new URL('../src/views/staff/DispatchPage.vue', import.meta.url), 'utf8');
const staffStore = readFileSync(new URL('../src/stores/staff.js', import.meta.url), 'utf8');
const orderApi = readFileSync(new URL('../src/api/order.js', import.meta.url), 'utf8');
const orderStore = readFileSync(new URL('../src/stores/order.js', import.meta.url), 'utf8');

test('staff dispatch and kitchen routes remain discoverable', () => {
  assert.match(router, /path: 'dispatch'[\s\S]*name: 'StaffDispatch'[\s\S]*DispatchPage\.vue/);
  assert.match(router, /path: 'kitchen'[\s\S]*redirect: \{ name: 'StaffOrders' \}/);
  assert.match(layout, /Bếp · Đơn hàng/);
  assert.match(layout, /Điều phối giao hàng/);
  assert.match(dispatch, /fetchDispatchOrders/);
  assert.match(dispatch, /getAvailableShippers/);
  assert.match(dispatch, /assignShipper/);
});

test('dispatch keeps independent lanes safe while polling workload', () => {
  assert.match(dispatch, /activeOrderCount/);
  assert.match(dispatch, /sortAvailableShippers/);
  assert.match(dispatch, /ordersError/);
  assert.match(dispatch, /shippersError/);
  assert.match(dispatch, /retryOrders/);
  assert.match(dispatch, /retryShippers/);
  assert.match(dispatch, /ordersInFlight/);
  assert.match(dispatch, /shippersInFlight/);
  assert.match(dispatch, /setInterval\([^]*30000\)/);
  assert.match(dispatch, /clearInterval\(pollTimer\)/);
  assert.match(dispatch, /validDispatchSelections/);
  assert.match(dispatch, /error\.status === 422/);
  assert.match(dispatch, /Shipper không còn trong ca hoạt động/);
  assert.match(dispatch, /acceptsDispatchRequest/);
  assert.match(dispatch, /stopped = true/);
  assert.match(dispatch, /<caption/);
  assert.match(dispatch, /data-label="Thao tác"/);
  assert.match(dispatch, /aria-live="polite"/);
  assert.match(dispatch, /aria-hidden="true"/);
  assert.match(dispatch, /await Promise\.allSettled\(\[loadOrders\(\), loadShippers\(\)\]\)/);
  assert.match(dispatch, /order\.customerPhone/);
  assert.match(dispatch, /order\.shippingAddress/);
  assert.match(dispatch, /formatPrice\(order\.total\)/);
  assert.match(dispatch, /waitingDuration\(order\.readyAt \|\| order\.createdAt\)/);
});

test('frontend order contracts use canonical fields and safe compatibility fallbacks', () => {
  assert.match(staffStore, /status: o\.status \?\? o\.orderStatus/);
  assert.match(staffStore, /userId: o\.userId \?\? null/);
  assert.match(staffStore, /customerPhone: o\.customerPhone \?\? ''/);
  assert.match(staffStore, /itemCount: kitchenItemCount\(o\)/);
  assert.match(staffStore, /modifiers: Array\.isArray\(item\.modifiers\) \? item\.modifiers : \[\]/);
  assert.match(dispatch, /order\.itemCount \?\? 0/);
  assert.match(dispatch, /Khách vãng lai/);
  assert.doesNotMatch(orderApi, /verifyPayment/);
  assert.match(orderStore, /estimatedDeliveryAt: data\.estimatedDeliveryAt \|\| null/);
  assert.doesNotMatch(orderStore, /checkoutUrl: data\.checkoutUrl[\s\S]{0,120}statusHistory/);
});
