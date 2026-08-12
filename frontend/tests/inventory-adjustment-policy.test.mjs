import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { normalizeApiError } from '../src/api/error.js';
import {
  adjustmentState,
  nextFocusIndex,
  nextOperationIndex,
  submitAdjustment,
} from '../src/utils/inventoryAdjustment.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/admin.js');
const inventory = read('../src/views/admin/InventoryPage.vue');
const ledger = read('../src/views/admin/InventoryLedgerPage.vue');
const variantSection = read('../src/components/admin/product-editor/ProductVariantsSection.vue');

test('admin API exposes inventory adjustment and waste mutations', () => {
  assert.match(api, /adjustInventory\(variantId, data\)/);
  assert.match(api, /wasteInventory\(variantId, data\)/);
  assert.match(api, /client\.post\('\/admin\/inventory\/transactions\/adjustments'/);
  assert.match(api, /client\.post\('\/admin\/inventory\/transactions\/waste'/);
  assert.match(api, /\{ variantId, \.\.\.data \}/);
});

test('inventory page offers adjustment and waste actions for managed stock only', () => {
  assert.match(inventory, /openAdjust\(row, \$event\)/);
  assert.match(inventory, /openWaste\(row\)/);
  assert.match(inventory, /v-if="row\.stock !== null"/);
  assert.match(inventory, /submitAdjust/);
  assert.match(inventory, /submitWaste/);
  assert.match(inventory, /adminApi\.adjustInventory\(adjustmentRow\.value\.variantId/);
  assert.match(inventory, /adminApi\.wasteInventory\(wasteRow\.value\.variantId/);
});

test('inventory page validates adjustment and waste inputs', () => {
  assert.match(inventory, /quantity <= 0/);
  assert.match(inventory, /projectedQuantity\.value < 0/);
  assert.match(inventory, /quantity > wasteRow\.value\.stock/);
  assert.match(inventory, /Vui lòng chọn lý do điều chỉnh/);
  assert.match(inventory, /Vui lòng chọn lý do lãng phí/);
});

test('adjustment modal supports operation tabs and expected stock', () => {
  assert.match(inventory, /INCREASE/);
  assert.match(inventory, /DECREASE/);
  assert.match(inventory, /SET/);
  assert.match(inventory, /expectedQuantity/);
  assert.match(inventory, /role="tablist"/);
  assert.match(inventory, /role="tabpanel"/);
});

test('normalized API errors preserve conflict response data', () => {
  const error = normalizeApiError({
    message: 'Request failed',
    response: { status: 409, data: { message: 'Stale', data: { variantId: 12, currentQuantity: 27 } } },
  });
  assert.equal(error.status, 409);
  assert.deepEqual(error.data, { variantId: 12, currentQuantity: 27 });
});

test('adjustment state projects quantities and disables invalid or no-op input', () => {
  assert.deepEqual(adjustmentState('INCREASE', '3', 10), { projectedQuantity: 13, canSubmit: true });
  assert.deepEqual(adjustmentState('DECREASE', '11', 10), { projectedQuantity: -1, canSubmit: false });
  assert.deepEqual(adjustmentState('SET', '10', 10), { projectedQuantity: 10, canSubmit: false });
  assert.deepEqual(adjustmentState('SET', '0', 10), { projectedQuantity: 0, canSubmit: true });
  assert.deepEqual(adjustmentState('INCREASE', '', 10), { projectedQuantity: null, canSubmit: false });
});

test('submit coordinator calls mutation once and keeps conflict modal state', async () => {
  let calls = 0;
  const state = await submitAdjustment(async () => {
    calls += 1;
    throw Object.assign(new Error('Stale'), { status: 409, data: { currentQuantity: 27 } });
  }, { variantId: 12 });
  assert.equal(calls, 1);
  assert.deepEqual(state, {
    close: false,
    currentQuantity: 27,
    error: 'Tồn kho đã thay đổi. Đã cập nhật số lượng hiện tại, vui lòng kiểm tra và gửi lại.',
  });
});

test('submit coordinator keeps server no-op open without success', async () => {
  const state = await submitAdjustment(async () => ({ changed: false, currentQuantity: 10 }), {});
  assert.deepEqual(state, { close: false, currentQuantity: 10, error: 'Tồn kho không thay đổi' });
});

test('tab navigation reducer handles arrows home end and ignores other keys', () => {
  assert.equal(nextOperationIndex('ArrowRight', 2, 3), 0);
  assert.equal(nextOperationIndex('ArrowLeft', 0, 3), 2);
  assert.equal(nextOperationIndex('Home', 2, 3), 0);
  assert.equal(nextOperationIndex('End', 0, 3), 2);
  assert.equal(nextOperationIndex('Enter', 1, 3), null);
});

test('focus trap reducer wraps forward and backward only at boundaries', () => {
  assert.equal(nextFocusIndex(2, 3, false), 0);
  assert.equal(nextFocusIndex(0, 3, true), 2);
  assert.equal(nextFocusIndex(1, 3, false), null);
  assert.equal(nextFocusIndex(1, 3, true), null);
});

test('component wires tested helpers to DOM focus and live feedback', () => {
  assert.match(inventory, /:disabled="submitting \|\| !canSubmitAdjustment"/);
  assert.match(inventory, /nextOperationIndex\(/);
  assert.match(inventory, /nextFocusIndex\(/);
  assert.match(inventory, /submitAdjustment\(/);
  assert.match(inventory, /event\.key === 'Escape'/);
  assert.match(inventory, /adjustmentTrigger = event\.currentTarget/);
  assert.match(inventory, /restoreTarget\?\.focus\(\)/);
  assert.match(inventory, /aria-live="polite"/);
  assert.match(inventory, /role="alert"/);
});

test('OTHER requires note', () => {
  assert.match(inventory, /reasonCode === 'OTHER'/);
  assert.match(inventory, /Ghi chú là bắt buộc/);
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

test('variant editor exposes managed stock audit fields only after existing stock changes', () => {
  assert.match(variantSection, /quantityAvailable/);
  assert.match(variantSection, /reasonCode/);
  assert.match(variantSection, /note/);
  assert.match(variantSection, /expectedQuantity/);
  assert.match(variantSection, /Quản lý tồn kho/);
  assert.match(variantSection, /v-if="row\.variantId && stockChanged\(row\)"/);
  assert.match(variantSection, /currentQuantity/);
  assert.match(variantSection, /status === 409/);
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
