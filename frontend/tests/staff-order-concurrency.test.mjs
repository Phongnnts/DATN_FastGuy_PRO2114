import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('staff mutations send expected status', async () => {
  const api = await read('../src/api/staff.js');
  const store = await read('../src/stores/staff.js');
  const detail = await read('../src/views/staff/OrderDetailPage.vue');
  const dispatch = await read('../src/views/staff/DispatchPage.vue');

  assert.match(api, /updateOrderStatus\(id, status, expectedStatus, failureReason\)/);
  assert.match(api, /\{ status, expectedStatus, failureReason \}/);
  assert.match(api, /assignShipper\(id, shipperId, expectedStatus\)/);
  assert.match(api, /\{ shipperId, expectedStatus \}/);
  assert.match(store, /staffApi\.updateOrderStatus\(id, status, expectedStatus, failureReason\)/);
  assert.match(detail, /order\.value\.status/);
  assert.match(dispatch, /staffApi\.assignShipper\(order\.id, shipperId, order\.status\)/);
});

test('conflicts retain HTTP status and refetch canonical staff data', async () => {
  const client = await read('../src/api/client.js');
  const detail = await read('../src/views/staff/OrderDetailPage.vue');
  const dispatch = await read('../src/views/staff/DispatchPage.vue');

  assert.match(client, /normalizeApiError\(err\)/);
  assert.match(detail, /error\.status === 409/);
  assert.match(detail, /error\.status === 422/);
  assert.match(detail, /Shipper không còn trong ca hoạt động/);
  assert.match(dispatch, /error\.status === 409/);
  assert.match(detail, /await load\(\{ silent: true \}\)/);
  assert.match(dispatch, /await Promise\.allSettled\(\[loadOrders\(\), loadShippers\(\)\]\)/);
});
