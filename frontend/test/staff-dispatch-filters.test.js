import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/staff.js');
const store = read('../src/stores/staff.js');
const page = read('../src/views/staff/DispatchPage.vue');
const kitchen = read('../src/utils/staffKitchen.js');

test('dispatch API forwards the exact server filter', () => {
  assert.match(api, /getDispatchOrders\(filter\)\s*\{\s*return client\.get\('\/staff\/orders\/dispatch', \{ params: \{ filter \} \}\);\s*\}/);
});

test('dispatch store maps canonical response and rejects stale completions', () => {
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
  assert.match(store, /acceptsDispatchRequest\(\{ requestGeneration, latestGeneration: dispatchGeneration, requestFilter: filter, activeFilter \}\)/);
  assert.match(store, /function invalidateDispatch\(\)/);
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
  assert.match(page, /@keydown="handleTabKeydown\(\$event, index\)"/);
  assert.match(page, /\['ArrowLeft', 'ArrowRight', 'Home', 'End'\]/);
  assert.match(page, /tab\.label/);
  assert.match(page, /dispatchCounts\[tab\.countKey\]/);
  assert.match(page, /switchFilter\(tab\.filter\)/);
  assert.match(page, /fetchDispatchOrders\(activeFilter\.value\)/);
  assert.match(page, /setInterval\([^]*30000\)/);
  assert.match(page, /min-height:\s*44px/);
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

test('dispatch request acceptance includes the selected filter', () => {
  assert.equal(kitchen.includes('requestFilter === activeFilter'), true);
  assert.equal(kitchen.includes('requestGeneration === latestGeneration'), true);
});
