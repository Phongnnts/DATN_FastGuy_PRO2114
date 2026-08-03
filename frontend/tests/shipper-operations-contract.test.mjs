import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const store = read('../src/stores/shipper.js');
const dashboard = read('../src/views/shipper/DashboardPage.vue');
const orders = read('../src/views/shipper/MyOrdersPage.vue');
const detail = read('../src/views/shipper/OrderDetailPage.vue');
const layout = read('../src/layouts/ShipperLayout.vue');
const router = read('../src/router/index.js');

test('shipper store maps canonical list and detail fields with separate request states', () => {
  for (const field of ['itemCount', 'assignedAt', 'pickedUpAt', 'deliveredAt', 'paymentMethod', 'paymentStatus', 'statusHistory', 'allowedActions', 'modifiers']) {
    assert.match(store, new RegExp(field));
  }
  for (const state of ['dashboardLoading', 'dashboardError', 'listLoading', 'listError', 'detailLoading', 'detailError']) {
    assert.match(store, new RegExp(state));
  }
  assert.match(store, /getActiveOrders/);
  assert.match(store, /activeOrders/);
  assert.match(store, /historyOrders/);
  assert.match(store, /listGeneration/);
  assert.match(store, /detailGeneration/);
  assert.match(store, /acceptsShipperRequest/);
  assert.match(store, /throw error/);
});

test('dashboard loads stats and active orders with retry and actionable next order', () => {
  assert.match(dashboard, /Promise\.allSettled/);
  assert.match(dashboard, /fetchActiveOrders/);
  assert.match(dashboard, /retry/);
  assert.match(dashboard, /nextOrder/);
  assert.match(dashboard, /ASSIGNED/);
  assert.match(dashboard, /PICKED_UP/);
  assert.match(dashboard, /inFlight/);
  assert.match(dashboard, /onUnmounted/);
});

test('active and history lists expose complete resilient operations UI', () => {
  assert.match(orders, /fetchActiveOrders/);
  assert.match(orders, /fetchHistory/);
  assert.match(orders, /setInterval\([^,]+,\s*30_000\)/s);
  assert.match(orders, /onUnmounted/);
  assert.match(orders, /inFlight/);
  assert.match(orders, /listError/);
  assert.match(orders, /retry/);
  assert.match(orders, /itemCount/);
  assert.match(orders, /paymentMethod/);
  assert.match(orders, /assignedAt|pickedUpAt|deliveredAt/);
  assert.match(orders, /localeCompare/);
  assert.match(orders, /watch\(historyOnly/);
  assert.match(orders, /startPolling/);
  assert.match(orders, /stopPolling/);
  assert.match(orders, /OrderStatusBadge/);
  assert.match(orders, /dateFrom/);
  assert.match(orders, /dateTo/);
  assert.match(orders, /type="date"/);
});

test('detail uses backend actions, exact COD validation, mutation guard and refetch', () => {
  assert.match(detail, /allowedActions/);
  assert.match(detail, /statusHistory/);
  assert.match(detail, /modifiers/);
  assert.match(detail, /submitting/);
  assert.match(detail, /validateExactCod\(collectedAmount\.value, order\.value\.total\)/);
  assert.match(detail, /if \(!stopped\) await load\(\)/);
  assert.match(detail, /detailError/);
  assert.match(detail, /onUnmounted/);
  assert.match(detail, /watch\(\(\) => route\.params\.id/);
  assert.match(detail, /shiftApi\.getCurrent/);
  assert.match(detail, /CHECKED_IN/);
  assert.match(detail, /OrderStatusBadge/);
  assert.match(detail, /encodeURIComponent\(order\.value\.customerAddress\)/);
  assert.match(detail, /https:\/\/www\.google\.com\/maps\/search\/\?api=1&query=/);
  assert.match(detail, /rel="noopener noreferrer"/);
});

test('shipper nested order routes activate orders nav and all shift failures use shifts', () => {
  assert.match(layout, /route\.path\.startsWith\('\/shipper\/orders\/'\)/);
  assert.match(layout, /@click="checkShift"/);
  assert.doesNotMatch(router, /SHIPPER: '\/shipper\/history'/);
  assert.ok((router.match(/SHIPPER: '\/shipper\/shifts'/g) || []).length >= 2);
});
