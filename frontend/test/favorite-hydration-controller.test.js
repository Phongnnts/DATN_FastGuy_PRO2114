import assert from 'node:assert/strict';
import test from 'node:test';
import { createFavoriteLoadController } from '../src/utils/favoriteHydration.js';

const reduced = [
  { productId: 2, name: 'Hai', price: '20000', imageUrl: '' },
  { productId: 1, name: 'Một', price: '10000', imageUrl: '' },
];
const full = [
  { productId: 1, name: 'Một đủ', price: 9000, originalPrice: 10000, discountPercent: 10, soldCount: 12, averageRating: 4.6, reviewCount: 7, bestSeller: true, isNew: true, variants: [{ variantId: 10, price: 9000, quantityAvailable: 3, status: 'AVAILABLE' }], defaultVariant: { variantId: 10, price: 9000, quantityAvailable: 3, status: 'AVAILABLE' }, modifierGroups: [{ modifierGroupId: 4, options: [{ modifierOptionId: 5, price: '2000' }] }] },
  { productId: 2, name: 'Hai đủ', price: 20000, soldCount: 5, variants: [{ variantId: 20, price: 20000, quantityAvailable: 2, status: 'AVAILABLE' }], defaultVariant: { variantId: 20, price: 20000, quantityAvailable: 2, status: 'AVAILABLE' }, modifierGroups: [] },
];

function deferred() {
  let resolve;
  const promise = new Promise(resolvePromise => { resolve = resolvePromise; });
  return { promise, resolve };
}

function harness({ catalog = full, catalogError } = {}) {
  const applied = [];
  const failures = [];
  const warnings = [];
  let catalogCalls = 0;
  const controller = createFavoriteLoadController({
    getFavorites: async () => reduced,
    getCatalog: async () => { catalogCalls += 1; if (catalogError) throw catalogError; return { content: catalog }; },
    apply: items => applied.push(items),
    fail: error => failures.push(error.message),
    warn: error => warnings.push(error.message),
    setLoading: () => {},
  });
  return { controller, applied, failures, warnings, get catalogCalls() { return catalogCalls; } };
}

test('hydrates complete product card metadata in favorite API order with one catalog request', async () => {
  const state = harness();
  await state.controller.load();
  assert.equal(state.catalogCalls, 1);
  assert.deepEqual(state.applied[0].map(item => item.productId), [2, 1]);
  assert.equal(state.applied[0][1].soldCount, 12);
  assert.equal(state.applied[0][1].averageRating, 4.6);
  assert.equal(state.applied[0][1].bestSeller, true);
  assert.equal(state.applied[0][1].variants[0].variantId, 10);
  assert.equal(state.applied[0][1].modifierGroups[0].options[0].price, 2000);
  assert.equal(state.applied[0][1].defaultVariant.variantId, 10);
  assert.equal(state.applied[0][0].cardDataComplete, true);
  assert.equal(state.applied[0][1].cardDataComplete, true);
});

test('falls back to normalized reduced favorite when catalog omits its product', async () => {
  const state = harness({ catalog: [full[0]] });
  await state.controller.load();
  assert.deepEqual(state.applied[0].map(item => item.productId), [2, 1]);
  assert.equal(state.applied[0][0].name, 'Hai');
  assert.equal(state.applied[0][0].price, 20000);
  assert.equal(state.applied[0][0].cardDataComplete, false);
  assert.equal(state.applied[0][1].cardDataComplete, true);
});

test('catalog failure preserves normalized reduced favorites and surfaces warning only', async () => {
  const state = harness({ catalogError: new Error('Catalog unavailable') });
  await state.controller.load();
  assert.deepEqual(state.applied[0].map(item => item.productId), [2, 1]);
  assert.equal(state.applied[0][0].price, 20000);
  assert.equal(state.applied[0][0].cardDataComplete, false);
  assert.deepEqual(state.warnings, ['Catalog unavailable']);
  assert.deepEqual(state.failures, []);
});

test('invalidation prevents a stale favorite request from applying after logout', async () => {
  const favorites = deferred();
  const applied = [];
  let catalogCalls = 0;
  const controller = createFavoriteLoadController({
    getFavorites: () => favorites.promise,
    getCatalog: async () => { catalogCalls += 1; return { content: full }; },
    apply: items => applied.push(items),
    fail: () => {},
    setLoading: () => {},
  });
  const load = controller.load();
  controller.invalidate();
  favorites.resolve(reduced);
  await load;
  assert.equal(catalogCalls, 0);
  assert.deepEqual(applied, []);
});

test('older load cannot overwrite a newer load', async () => {
  const first = deferred();
  const second = deferred();
  const requests = [first, second];
  const applied = [];
  const controller = createFavoriteLoadController({
    getFavorites: () => requests.shift().promise,
    getCatalog: async () => ({ content: full }),
    apply: items => applied.push(items),
    fail: () => {},
    setLoading: () => {},
  });
  const older = controller.load();
  const newer = controller.load();
  second.resolve([reduced[1]]);
  await newer;
  first.resolve(reduced);
  await older;
  assert.deepEqual(applied.map(items => items.map(item => item.productId)), [[1]]);
});
