import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/admin.js');
const inventory = read('../src/views/admin/InventoryPage.vue');
const ledger = read('../src/views/admin/InventoryLedgerPage.vue');

test('admin API exposes inventory adjustment and waste mutations', () => {
  assert.match(api, /adjustInventory\(variantId, data\)/);
  assert.match(api, /wasteInventory\(variantId, data\)/);
  assert.match(api, /client\.post\('\/admin\/inventory\/transactions\/adjustments'/);
  assert.match(api, /client\.post\('\/admin\/inventory\/transactions\/waste'/);
  assert.match(api, /\{ variantId, \.\.\.data \}/);
});

test('inventory page offers adjustment and waste actions for managed stock only', () => {
  assert.match(inventory, /openAdjust\(row\)/);
  assert.match(inventory, /openWaste\(row\)/);
  assert.match(inventory, /v-if="row\.stock !== null"/);
  assert.match(inventory, /submitAdjust/);
  assert.match(inventory, /submitWaste/);
  assert.match(inventory, /adminApi\.adjustInventory\(adjustmentRow\.value\.variantId/);
  assert.match(inventory, /adminApi\.wasteInventory\(wasteRow\.value\.variantId/);
});

test('inventory page validates adjustment and waste inputs', () => {
  assert.match(inventory, /newQuantity < 0/);
  assert.match(inventory, /quantity <= 0/);
  assert.match(inventory, /quantity > wasteRow\.value\.stock/);
  assert.match(inventory, /Vui lòng chọn lý do điều chỉnh/);
  assert.match(inventory, /Vui lòng chọn lý do lãng phí/);
});

test('inventory page ships reason codes and accessible modals', () => {
  assert.match(inventory, /STOCK_COUNT/);
  assert.match(inventory, /DAMAGE/);
  assert.match(inventory, /EXPIRED/);
  assert.match(inventory, /OTHER/);
  assert.match(inventory, /role="dialog"/);
  assert.match(inventory, /aria-modal="true"/);
  assert.doesNotMatch(inventory, /window\.confirm\(/);
});

test('ledger exposes adjustment type and audit columns', () => {
  assert.match(ledger, /ADJUSTMENT/);
  assert.match(ledger, /Điều chỉnh/);
  assert.match(ledger, /Biến động/);
  assert.match(ledger, /Chi tiết/);
  assert.match(ledger, /row\.reasonCode/);
  assert.match(ledger, /row\.createdByName/);
  assert.match(ledger, /quantityBefore !== null && row\.quantityAfter !== null/);
});
