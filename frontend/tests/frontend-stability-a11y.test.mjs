import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const constants = read('../src/utils/constants.js');
const tracking = read('../src/views/guest/TrackOrderPage.vue');
const shipperOrders = read('../src/views/shipper/MyOrdersPage.vue');

test('keeps entry routes and redirects legacy routes', () => {
  assert.match(router, /path: '\/'[\s\S]*name: 'Login'/);
  assert.match(router, /path: 'home'[\s\S]*name: 'Home'/);
  assert.match(router, /path: '\/reports'.*AdminReports/);
  assert.match(router, /path: '\/loyalty'.*UserRewards/);
  assert.match(router, /path: '\/history'.*UserOrders/);
  assert.doesNotMatch(router, /findLast/);
});

test('uses backend payment and shift status values', () => {
  assert.match(constants, /PAYMENT_STATUS = \{\s*UNPAID: 'UNPAID',\s*PAID: 'PAID'/);
  assert.match(constants, /SHIFT_STATUS = \{\s*SCHEDULED: 'SCHEDULED',\s*CHECKED_IN: 'CHECKED_IN',\s*CHECKED_OUT: 'CHECKED_OUT'/);
  assert.doesNotMatch(constants, /REFUNDED: 'REFUNDED'/);
  assert.doesNotMatch(constants, /ABSENT: 'ABSENT'/);
});

test('guest tracking omits unsupported checkout continuation', () => {
  assert.doesNotMatch(tracking, /trackingResult\.checkoutUrl/);
  assert.doesNotMatch(tracking, /Tiếp tục thanh toán/);
});

test('shipper tabs and cards expose basic accessibility', () => {
  assert.match(shipperOrders, /role="tablist"/);
  assert.match(shipperOrders, /role="tab"/);
  assert.match(shipperOrders, /role="tabpanel"/);
  assert.match(shipperOrders, /@keydown="handleCardKeydown/);
});
