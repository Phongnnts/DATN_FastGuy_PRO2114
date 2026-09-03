import { readFileSync } from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeApiError } from '../src/api/error.js';
import {
  buildAdjustmentPayload,
  buildItemPayload,
  formatQuantity,
  parseQuantity,
} from '../src/utils/inventoryItem.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/admin.js');
const inventory = read('../src/views/admin/InventoryPage.vue');

test('admin API exposes item-level inventory mutations without legacy variant stock routes', () => {
  assert.match(api, /client\.post\('\/admin\/inventory\/items', data\)/);
  assert.doesNotMatch(api, /receiptInventory|\/admin\/inventory\/transactions\/receipts/);
  assert.match(api, /adjustInventoryItem\(data\)/);
  assert.match(api, /recordInventoryWaste\(data\)[\s\S]*?transactions\/waste/);
  assert.doesNotMatch(api, /\{ variantId, \.\.\.data \}/);
});

test('inventory page offers create, goods receipt navigation and adjustment actions per item', () => {
  assert.match(inventory, /openDialog\('create'\)/);
  assert.match(inventory, /name: 'AdminGoodsReceipts'/);
  assert.doesNotMatch(inventory, /openDialog\('receipt', item, \$event\)/);
  assert.match(inventory, /openDialog\('adjust', item, \$event\)/);
  assert.match(inventory, /submitItemForm/);
  assert.match(inventory, /submitMutation\(dialog\.kind\)/);
  assert.match(inventory, /openDialog\('waste', item, \$event\)/);
  assert.match(inventory, /Ghi nhận hao hụt/);
});

test('inventory dialogs validate decimal quantity and required reason', () => {
  assert.match(inventory, /Số lượng phải là số dương, tối đa 4 chữ số thập phân/);
  assert.match(inventory, /Vui lòng nhập lý do/);
  assert.match(inventory, /Khả dụng sau điều chỉnh không thể âm/);
});

test('adjustment dialog supports increase and decrease with projected preview', () => {
  assert.match(inventory, /'INCREASE'/);
  assert.match(inventory, /'DECREASE'/);
  assert.match(inventory, /projectedQuantity\(form\)/);
  assert.match(inventory, /aria-live="polite"/);
});

test('normalized API errors preserve conflict response data', () => {
  const error = normalizeApiError({
    message: 'Request failed',
    response: { status: 409, data: { message: 'Stale', data: { variantId: 12, currentQuantity: 27 } } },
  });
  assert.equal(error.status, 409);
  assert.deepEqual(error.data, { variantId: 12, currentQuantity: 27 });
  const conflict = normalizeApiError({
    message: 'Request failed',
    response: { status: 409, data: { status: 'error', message: 'Stale expected quantity', currentOnHandQuantity: 30.5 } },
  });
  assert.equal(conflict.currentOnHandQuantity, 30.5);
});

test('quantity parsing accepts only positive decimals up to four fractional digits', () => {
  assert.equal(parseQuantity('2,5').value, 2.5);
  assert.equal(parseQuantity('0').ok, false);
  assert.equal(parseQuantity('-1').ok, false);
  assert.equal(parseQuantity('1.00001').ok, false);
});

test('formatQuantity renders decimals deterministically without float arithmetic', () => {
  assert.equal(formatQuantity('10.5000'), '10,5');
  assert.equal(formatQuantity('0.30000004'), '0,3');
  assert.equal(formatQuantity(null), '—');
});

test('adjustment payload builder sends contracted fields', () => {
  const item = { inventoryItemId: 7, onHandQuantity: 20 };
  assert.deepEqual(
    buildAdjustmentPayload(item, { operation: 'INCREASE', quantity: '1.5', reason: 'Kiểm kê', note: 'bù' }),
    { inventoryItemId: 7, quantity: 1.5, expectedOnHandQuantity: 20, reason: 'Kiểm kê', note: 'bù' },
  );
  assert.deepEqual(buildItemPayload({ name: ' Bột ', itemType: 'INGREDIENT', baseUnit: 'G', minimumQuantity: '', active: true }).minimumQuantity, 0);
});

test('modal keeps accessible dialog semantics and stale-conflict recovery', () => {
  assert.match(inventory, /role="dialog"/);
  assert.match(inventory, /aria-modal="true"/);
  assert.match(inventory, /event\.key === 'Escape'/);
  assert.match(inventory, /currentOnHandQuantity/);
  assert.match(inventory, /dialogTrigger/);
  assert.ok(inventory.includes('trigger?.focus?.();'));
  assert.match(inventory, /if \(saving\.value\) return;/);
  assert.doesNotMatch(inventory, /window\.confirm\(/);
});
