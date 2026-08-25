import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { createDispatchRequestGate, dispatchTabTarget } from '../src/utils/staffKitchen.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/staff.js');
const store = read('../src/stores/staff.js');
const page = read('../src/views/staff/DispatchPage.vue');

test('dispatch API forwards the exact server filter', () => {
  assert.match(api, /getDispatchOrders\(filter\)\s*\{\s*return client\.get\('\/staff\/orders\/dispatch', \{ params: \{ filter \} \}\);\s*\}/);
});

test('dispatch store maps canonical response', () => {
  for (const state of ['dispatchItems', 'dispatchCounts', 'dispatchLoading', 'dispatchError']) {
    assert.match(store, new RegExp(`const ${state} = ref\\(`));
  }
  assert.match(store, /readyAt: o\.readyAt \|\| null/);
  assert.match(store, /classification: o\.classification/);
  assert.match(store, /minutesUntilClose: o\.minutesUntilClose \?\? null/);
  assert.match(store, /priority: Number\(data\?\.counts\?\.priority \?\? 0\)/);
  assert.match(store, /new: Number\(data\?\.counts\?\.new \?\? 0\)/);
  assert.match(store, /review: Number\(data\?\.counts\?\.review \?\? 0\)/);
  assert.match(store, /staffApi\.getDispatchOrders\(filter\)/);
  assert.match(store, /dispatchRequestGate\.begin\(filter\)/);
  assert.match(store, /dispatchRequestGate\.accepts\(request\)/);
  assert.match(store, /function invalidateDispatch\(\)/);
});

test('Priority New Priority requests accept only the latest completion', () => {
  const gate = createDispatchRequestGate('PRIORITY');
  const firstPriority = gate.begin('PRIORITY');
  const newRequest = gate.begin('NEW');
  const latestPriority = gate.begin('PRIORITY');

  assert.deepEqual([firstPriority.filter, newRequest.filter, latestPriority.filter], ['PRIORITY', 'NEW', 'PRIORITY']);
  const applied = [];
  for (const [request, value] of [[newRequest, 'new'], [firstPriority, 'old-priority'], [latestPriority, 'latest-priority']]) {
    if (gate.accepts(request)) applied.push(value);
  }
  assert.deepEqual(applied, ['latest-priority']);
});

test('dispatch keyboard target is calculated synchronously', () => {
  assert.equal(dispatchTabTarget(0, 'ArrowLeft', 3), 2);
  assert.equal(dispatchTabTarget(2, 'ArrowRight', 3), 0);
  assert.equal(dispatchTabTarget(1, 'Home', 3), 0);
  assert.equal(dispatchTabTarget(1, 'End', 3), 2);
  assert.equal(dispatchTabTarget(1, 'Enter', 3), 1);
});

test('dispatch tabs expose exact labels, counts, roving focus, and current-filter polling', () => {
  assert.match(page, /const activeFilter = ref\('PRIORITY'\)/);
  assert.match(page, /label: 'Priority'/);
  assert.match(page, /label: 'New'/);
  assert.match(page, /label: 'Review'/);
  assert.match(page, /role="tablist"/);
  assert.match(page, /role="tab"/);
  assert.match(page, /:aria-selected="activeFilter === tab\.filter"/);
  assert.match(page, /:tabindex="activeFilter === tab\.filter \? 0 : -1"/);
  assert.match(page, /aria-controls="dispatch-panel"/);
  assert.match(page, /@keydown="handleTabKeydown\(\$event, index\)"/);
  assert.match(page, /dispatchTabTarget\(index, event\.key, tabs\.length\)/);
  assert.match(page, /activeFilter\.value = tabs\[nextIndex\]\.filter;[\s\S]*tabElements\.value\[nextIndex\]\?\.focus\(\);[\s\S]*void loadOrders\(tabs\[nextIndex\]\.filter\);/);
  assert.match(page, /tab\.label/);
  assert.match(page, /dispatchCounts\[tab\.countKey\]/);
  assert.match(page, /switchFilter\(tab\.filter\)/);
  assert.match(page, /fetchDispatchOrders\(activeFilter\.value\)/);
  assert.match(page, /setInterval\([^]*30000\)/);
  assert.match(page, /id="dispatch-panel"/);
  assert.doesNotMatch(page, /ordersInFlight/);
  assert.match(page, /min-height:\s*44px/);
  assert.match(page, /\.refresh-control\{[^}]*min-height:44px/);
});

test('dispatch keeps assignment READY-only and renders Review incident action', () => {
  assert.match(page, /const canAssign = computed\(\(\) => activeFilter\.value !== 'REVIEW'\)/);
  assert.match(page, /!canAssign\.value \|\| !order\.shipperId/);
  assert.match(page, /v-if="canAssign"/);
  assert.match(page, /order\.failureNote \|\| order\.deliveryFailureCode/);
  assert.match(page, /order\.deliveryAttemptCount/);
  assert.match(page, /order\.deliveryAttemptLimit/);
  assert.match(page, />Xử lý lại</);
  assert.match(page, /staffApi\.assignShipper\(order\.id, shipperId, order\.status\)/);
  assert.match(page, /error\.status === 409/);
  assert.match(page, /getAvailableShippers/);
  assert.match(page, /shippersLoading/);
});
