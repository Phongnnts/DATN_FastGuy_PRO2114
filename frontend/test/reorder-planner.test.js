import test from 'node:test';
import assert from 'node:assert/strict';
import { mapProduct } from '../src/utils/productMapper.js';
import { createReorderController, executeReorderPlan, planReorder, reorderItemKey } from '../src/utils/reorderPlanner.js';

const option = (groupId, modifierOptionId, overrides = {}) => ({ groupId, modifierOptionId, name: `Option ${modifierOptionId}`, price: modifierOptionId, status: 'ACTIVE', isActive: true, ...overrides });
const group = (modifierGroupId, options, overrides = {}) => ({ modifierGroupId, name: `Group ${modifierGroupId}`, status: 'ACTIVE', isActive: true, minSelections: 0, maxSelections: options.length, options, ...overrides });
const product = (overrides = {}) => ({
  productId: 1,
  name: 'Burger',
  status: 'AVAILABLE',
  isAvailable: true,
  inStock: true,
  isAvailableNow: true,
  variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: 5 }],
  modifierGroups: [group(2, [option(2, 20)])],
  ...overrides,
});
const item = (overrides = {}) => ({ productId: 1, variantId: 10, productName: 'Burger', quantity: 1, modifiers: [{ groupId: 2, modifierOptionId: 20 }], ...overrides });
const fetchProducts = (products) => async (id) => products.find(value => value.productId === Number(id)) || null;

test('planner preserves live option payload and enforces composite group identity', async () => {
  const live = product({ modifierGroups: [group(2, [option(2, 20, { name: 'Live cheese', price: 9000 })]), group(3, [option(3, 20)])] });
  const result = await planReorder([item()], fetchProducts([live]));
  assert.deepEqual(result.invalid, []);
  assert.deepEqual(result.entries, [{ productId: 1, variantId: 10, quantity: 1, productName: 'Burger', modifiers: [{ modifierOptionId: 20, groupId: 2, groupName: 'Group 2', name: 'Live cheese', price: 9000 }] }]);
});

test('planner rejects duplicate option identities and duplicate current group IDs', async () => {
  const duplicateOptions = product({ modifierGroups: [group(2, [option(2, 20), option(2, 20)])] });
  const duplicateGroups = product({ modifierGroups: [group(2, [option(2, 20)]), group(2, [option(2, 21)])] });
  assert.equal((await planReorder([item()], fetchProducts([duplicateOptions]))).entries.length, 0);
  assert.equal((await planReorder([item()], fetchProducts([duplicateGroups]))).entries.length, 0);
});

test('planner enforces active group minimum maximum and newly required groups', async () => {
  const required = product({ modifierGroups: [group(2, [option(2, 20)], { minSelections: 1, maxSelections: 1 }), group(3, [option(3, 30)], { minSelections: 1, maxSelections: 1 })] });
  const tooMany = item({ modifiers: [{ groupId: 2, modifierOptionId: 20 }, { groupId: 2, modifierOptionId: 21 }] });
  const maxProduct = product({ modifierGroups: [group(2, [option(2, 20), option(2, 21)], { maxSelections: 1 })] });
  assert.equal((await planReorder([item()], fetchProducts([required]))).entries.length, 0);
  assert.equal((await planReorder([tooMany], fetchProducts([maxProduct]))).entries.length, 0);
});

test('planner rejects unavailable products variants invalid stock and unsafe quantities', async () => {
  for (const quantity of [0, -1, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) {
    assert.equal((await planReorder([item({ quantity })], fetchProducts([product()]))).entries.length, 0);
  }
  for (const live of [
    product({ isAvailable: false }), product({ inStock: false }), product({ isAvailableNow: false }),
    product({ variants: [] }), product({ variants: [{ variantId: 10, status: 'INACTIVE', quantityAvailable: 5 }] }),
    product({ variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: NaN }] }),
    product({ variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: 0 }] }),
  ]) assert.equal((await planReorder([item()], fetchProducts([live]))).entries.length, 0);
});

test('planner aggregates duplicate lines then validates aggregate stock', async () => {
  const valid = await planReorder([item({ quantity: 2 }), item({ quantity: 3 })], fetchProducts([product()]));
  assert.equal(valid.entries.length, 1);
  assert.equal(valid.entries[0].quantity, 5);
  const excess = await planReorder([item({ quantity: 3 }), item({ quantity: 3 })], fetchProducts([product()]));
  assert.equal(excess.entries.length, 0);
  assert.equal(excess.invalid.length, 1);
});

test('planner keeps valid entries when another item is invalid', async () => {
  const result = await planReorder([item(), item({ productId: 2, productName: 'Missing' })], fetchProducts([product()]));
  assert.equal(result.entries.length, 1);
  assert.equal(result.invalid.length, 1);
  assert.match(result.invalid[0], /Missing/);
});

test('executor sends exact payload and continues after API failure', async () => {
  const entries = [
    { productId: 1, variantId: 10, quantity: 2, productName: 'Burger', modifiers: [{ groupId: 2, modifierOptionId: 20 }] },
    { productId: 2, variantId: 11, quantity: 1, productName: 'Fries', modifiers: [] },
  ];
  const calls = [];
  const result = await executeReorderPlan(entries, async (...args) => { calls.push(args); if (args[0] === 1) throw new Error('API down'); });
  assert.deepEqual(calls, [[1, 10, 2, entries[0].modifiers], [2, 11, 1, []]]);
  assert.equal(result.added, 1);
  assert.deepEqual(result.failed, ['Burger: API down']);
});

test('actual product mapper feeds mapped availability contract into planner', async () => {
  const mapped = mapProduct({
    productId: 1,
    name: 'Burger',
    isAvailable: true,
    inStock: true,
    isAvailableNow: true,
    variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: 3 }],
    modifierGroups: [group(2, [option(2, 20)])],
  });
  const result = await planReorder([item()], fetchProducts([mapped]));
  assert.equal(result.entries.length, 1);
});

test('null stock remains unlimited while finite zero stays unavailable', async () => {
  const unlimited = product({ status: undefined, isAvailable: true, variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: null }] });
  const valid = await planReorder([item({ quantity: 999 })], fetchProducts([unlimited]));
  assert.equal(valid.entries[0].quantity, 999);
  const zero = product({ status: undefined, isAvailable: true, variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: 0 }] });
  assert.equal((await planReorder([item()], fetchProducts([zero]))).entries.length, 0);
});

test('render keys stay unique for duplicate identical snapshots', () => {
  const duplicate = item();
  const first = reorderItemKey({ ...duplicate, orderItemId: 50 }, 0);
  const second = reorderItemKey({ ...duplicate, orderItemId: 51 }, 1);
  const fallbackFirst = reorderItemKey(duplicate, 0);
  const fallbackSecond = reorderItemKey(duplicate, 1);
  assert.equal(new Set([first, second, fallbackFirst, fallbackSecond]).size, 4);
});

test('controller blocks double click and returns success partial and all-fail messages', async () => {
  let release;
  const pending = new Promise(resolve => { release = resolve; });
  const controller = createReorderController({ fetchProduct: fetchProducts([product()]), addItem: async () => pending });
  const first = controller.run([item()]);
  assert.equal((await controller.run([item()])).ignored, true);
  release();
  assert.match((await first).message, /Đã thêm/);

  const partial = await createReorderController({ fetchProduct: fetchProducts([product()]), addItem: async () => {} }).run([item(), item({ productId: 2, productName: 'Missing' })]);
  assert.equal(partial.kind, 'partial');
  assert.match(partial.message, /Không thể thêm/);

  const failed = await createReorderController({ fetchProduct: fetchProducts([]), addItem: async () => {} }).run([item()]);
  assert.equal(failed.kind, 'error');
  assert.equal(failed.added, 0);
});
