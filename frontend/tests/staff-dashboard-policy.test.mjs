import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { staffDashboardAttention } from '../src/utils/staffDashboardAttention.js';

const dashboard = readFileSync(new URL('../src/views/staff/DashboardPage.vue', import.meta.url), 'utf8');

test('staff dashboard renders realtime shift operations and shift report', () => {
  assert.match(dashboard, /shiftCompletedOrders/);
  assert.match(dashboard, /shiftFailedOrders/);
  assert.match(dashboard, /shiftNetRevenue/);
  assert.match(dashboard, /priorityOrders/);
  assert.match(dashboard, /Đơn hoàn thành trong ca/);
  assert.match(dashboard, /Đơn thất bại trong ca/);
  assert.match(dashboard, /Doanh thu thuần trong ca/);
  assert.match(dashboard, /Đơn cần ưu tiên/);
  assert.match(dashboard, /waitingDuration/);
  assert.match(dashboard, /formatPrice/);
  assert.match(dashboard, /setInterval\([^]*30000\)/);
});

test('staff dashboard refreshes silently and exposes accessible states', () => {
  assert.match(dashboard, /async function load\(\{ silent = false \} = \{\}\)/);
  assert.match(dashboard, /aria-live="polite"/);
  assert.match(dashboard, /role="alert"/);
  assert.match(dashboard, /lastUpdated/);
  assert.match(dashboard, /@media\(max-width:768px\)/);
  assert.match(dashboard, /prefers-reduced-motion:reduce/);
});

test('staff attention sums exact order and stock signals', () => {
  assert.deepEqual(staffDashboardAttention({
    overdueOrders: 2,
    awaitingShipperOrders: 3,
    outOfStockSkuCount: 4,
    lowStockSkuCount: 5,
    lowStockThreshold: 7,
  }), {
    alertCount: 14,
    overdueOrders: 2,
    awaitingShipperOrders: 3,
    outOfStockSkuCount: 4,
    lowStockSkuCount: 5,
    lowStockThreshold: 7,
    routeTab: 'PENDING',
  });
});

test('staff attention defaults missing response counts to zero', () => {
  assert.deepEqual(staffDashboardAttention({ lowStockThreshold: 6 }), {
    alertCount: 0,
    overdueOrders: 0,
    awaitingShipperOrders: 0,
    outOfStockSkuCount: 0,
    lowStockSkuCount: 0,
    lowStockThreshold: 6,
    routeTab: 'PENDING',
  });
});

test('staff dashboard wires current-response attention without persistent notifications', () => {
  assert.match(dashboard, /staffDashboardAttention/);
  assert.match(dashboard, /attention\.outOfStockSkuCount/);
  assert.match(dashboard, /attention\.lowStockSkuCount/);
  assert.match(dashboard, /attention\.lowStockThreshold/);
  assert.match(dashboard, /SKU hết hàng/);
  assert.match(dashboard, /SKU sắp hết/);
  assert.match(dashboard, /goOrders\(attention\.routeTab\)/);
  assert.doesNotMatch(dashboard, /Notification|markRead|notify/);
});
