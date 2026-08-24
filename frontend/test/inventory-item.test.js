import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildAdjustmentPayload,
  buildItemPayload,
  buildRecipePayload,
  formatQuantity,
  inventoryKpis,
  itemStockState,
  parseQuantity,
  recipeServings,
  validateRecipeForm,
} from '../src/utils/inventoryItem.js';

test('formatQuantity renders decimal quantities with vi-VN grouping and four-digit precision', () => {
  assert.equal(formatQuantity(1500), '1.500');
  assert.equal(formatQuantity('10.5000'), '10,5');
  assert.equal(formatQuantity('0.3001'), '0,3001');
  assert.equal(formatQuantity('0.30000004'), '0,3');
  assert.equal(formatQuantity(-2.5), '-2,5');
  assert.equal(formatQuantity('123456.789'), '123.456,789');
});

test('formatQuantity rejects non-numeric values', () => {
  assert.equal(formatQuantity(null), '—');
  assert.equal(formatQuantity(undefined), '—');
  assert.equal(formatQuantity(''), '—');
  assert.equal(formatQuantity('abc'), '—');
});

test('parseQuantity accepts only positive decimals up to four fractional digits', () => {
  assert.deepEqual(parseQuantity('12,5'), { ok: true, value: 12.5 });
  assert.deepEqual(parseQuantity('0.0001'), { ok: true, value: 0.0001 });
  for (const bad of ['', '   ', '0', '-1', '1.00001', 'abc', '1e3']) {
    assert.deepEqual(parseQuantity(bad), { ok: false, value: null });
  }
});

test('itemStockState derives low-stock, out and inactive display state', () => {
  const base = { active: true, availableQuantity: 5, minimumQuantity: 5 };
  assert.equal(itemStockState({ ...base }), 'LOW');
  assert.equal(itemStockState({ ...base, availableQuantity: 5.001 }), 'OK');
  assert.equal(itemStockState({ ...base, availableQuantity: 0 }), 'OUT');
  assert.equal(itemStockState({ ...base, active: false }), 'INACTIVE');
});

test('inventoryKpis counts items and active below-minimum stock', () => {
  assert.deepEqual(inventoryKpis([
    { active: true, availableQuantity: 9, minimumQuantity: 5 },
    { active: true, availableQuantity: 4, minimumQuantity: 5 },
    { active: true, availableQuantity: 0, minimumQuantity: 5 },
    { active: false, availableQuantity: 100, minimumQuantity: 5 },
  ]), { itemCount: 4, belowMinimumCount: 2 });
  assert.deepEqual(inventoryKpis([]), { itemCount: 0, belowMinimumCount: 0 });
});

test('payload builders keep contracted field names exactly', () => {
  assert.deepEqual(
    buildItemPayload({ inventoryCode: ' bot-chien ', name: ' Bột chiên ', itemType: 'INGREDIENT', baseUnit: 'G', minimumQuantity: '5', countFrequency: 'DAILY', active: true }),
    { inventoryCode: 'BOT-CHIEN', name: 'Bột chiên', itemType: 'INGREDIENT', baseUnit: 'G', minimumQuantity: 5, countFrequency: 'DAILY', active: true },
  );
  const item = { inventoryItemId: 7, onHandQuantity: 20 };
  assert.deepEqual(
    buildAdjustmentPayload(item, { operation: 'DECREASE', quantity: '0.5', reason: 'Kiểm kê', note: 'lẻ' }),
    { inventoryItemId: 7, quantity: -0.5, expectedOnHandQuantity: 20, reason: 'Kiểm kê', note: 'lẻ' },
  );
  assert.deepEqual(
    buildRecipePayload({ inventoryMode: 'INGREDIENT', yieldQuantity: '4', active: true, items: [{ inventoryItemId: 3, quantity: '1.5' }] }),
    { inventoryMode: 'INGREDIENT', yieldQuantity: 4, active: true, items: [{ inventoryItemId: 3, quantity: 1.5 }] },
  );
});

test('validateRecipeForm blocks missing mode, yield and duplicate or non-positive items', () => {
  assert.deepEqual(validateRecipeForm({ inventoryMode: '', yieldQuantity: '', items: [] }), {
    inventoryMode: 'Chọn chế độ kho',
    yieldQuantity: 'Số phần đầu ra phải lớn hơn 0',
    items: 'Thêm ít nhất một dòng nguyên liệu',
  });
  const errors = validateRecipeForm({
    inventoryMode: 'INGREDIENT',
    yieldQuantity: '4',
    items: [
      { inventoryItemId: 3, quantity: '1' },
      { inventoryItemId: 3, quantity: '2' },
      { inventoryItemId: '', quantity: '2' },
      { inventoryItemId: 4, quantity: '0' },
    ],
  });
  assert.deepEqual(errors.lines, { 1: 'Mặt hàng đã được chọn', 2: 'Chọn mặt hàng', 3: 'Số lượng phải lớn hơn 0' });
  assert.equal(validateRecipeForm({ inventoryMode: 'UNTRACKED', yieldQuantity: '1', items: [{ inventoryItemId: 3, quantity: '1' }] }).lines, undefined);
});

test('recipeServings finds limiting ingredient deterministically', () => {
  const recipe = { yieldQuantity: 4, items: [{ inventoryItemId: 1, quantity: 2 }, { inventoryItemId: 2, quantity: 8 }] };
  const byId = { 1: { availableQuantity: 9 }, 2: { availableQuantity: 30 } };
  assert.deepEqual(recipeServings(recipe, byId), { servings: 15, limitingItemId: 2 });
  assert.deepEqual(recipeServings(recipe, {}), { servings: 0, limitingItemId: 1 });
  assert.deepEqual(recipeServings(null, byId), { servings: 0, limitingItemId: null });
});
