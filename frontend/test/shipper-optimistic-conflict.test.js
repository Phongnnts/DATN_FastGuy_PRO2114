import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('shipper API sends expectedStatus for pickup and delivery', () => {
  const api = read('../src/api/shipper.js');
  assert.match(api, /pickUpOrder\(id, expectedStatus\)/);
  assert.match(api, /expectedStatus/);
  assert.match(api, /deliverOrder\(id, collectedAmount, expectedStatus\)/);
});

test('shipper store preserves status and reloads canonical data on conflict', () => {
  const store = read('../src/stores/shipper.js');
  assert.match(store, /error\.status === 409/);
  assert.match(store, /fetchActiveOrders\(true\)/);
  assert.match(store, /fetchOrderById\(id\)/);
  assert.match(store, /Đơn hàng đã thay đổi trạng thái\. Dữ liệu mới nhất đã được tải lại\./);
});

test('quick action and detail send displayed canonical status', () => {
  const sheet = read('../src/components/shipper/OrderActionSheet.vue');
  const detail = read('../src/views/shipper/OrderDetailPage.vue');
  assert.match(sheet, /props\.order\.status/);
  assert.match(detail, /order\.value\.status/);
  assert.match(sheet, /error\?\.status === 409/);
  assert.match(detail, /error\?\.status === 409/);
});
