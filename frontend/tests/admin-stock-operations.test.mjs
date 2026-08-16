import assert from 'node:assert/strict';
import test from 'node:test';
import {
  createLatestCatalogFetcher,
  createStockPageLoader,
  inventoryMatchesStockFilter,
  inventoryRowCanMutate,
  inventoryRowsSummary,
  productMatchesStockFilter,
} from '../src/utils/adminStockOperations.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

test('first dashboard failure keeps products usable with default threshold five', async () => {
  const state = { loading: false, error: '', dashboardError: '', threshold: null };
  const loader = createStockPageLoader(state);
  await loader.load({
    required: [() => Promise.resolve()],
    dashboard: () => Promise.reject(new Error('dashboard failed')),
  });
  assert.deepEqual(state, { loading: false, error: '', dashboardError: 'dashboard failed', threshold: 5 });
});

test('refresh dashboard failure retains prior threshold and surfaces stale warning', async () => {
  const state = { loading: false, error: '', dashboardError: '', threshold: 7 };
  const loader = createStockPageLoader(state);
  await loader.load({
    required: [() => Promise.resolve()],
    dashboard: () => Promise.reject(new Error('dashboard failed')),
  });
  assert.deepEqual(state, { loading: false, error: '', dashboardError: 'dashboard failed', threshold: 7 });
});

test('required product failure reports error while dashboard failure stays non-blocking', async () => {
  const state = { loading: false, error: '', threshold: null };
  const loader = createStockPageLoader(state);
  await loader.load({
    required: [() => Promise.reject(new Error('products failed'))],
    dashboard: () => Promise.reject(new Error('dashboard failed')),
    errorMessage: 'fallback',
  });
  assert.deepEqual(state, { loading: false, error: 'products failed', threshold: 5 });
});

test('stale request cannot overwrite latest success error or loading state', async () => {
  const oldRequired = deferred();
  const state = { loading: false, error: '', threshold: null };
  const loader = createStockPageLoader(state);
  const oldLoad = loader.load({ required: [() => oldRequired.promise], dashboard: () => Promise.resolve({ lowStockThreshold: 9 }) });
  await loader.load({ required: [() => Promise.resolve()], dashboard: () => Promise.resolve({ lowStockThreshold: 7 }) });
  oldRequired.reject(new Error('stale failure'));
  await oldLoad;
  assert.deepEqual(state, { loading: false, error: '', threshold: 7 });
});

test('inventory filters explicit SKU states safely', () => {
  assert.equal(inventoryMatchesStockFilter({ stock: 0 }, 'OUT', 5), true);
  assert.equal(inventoryMatchesStockFilter({ stock: 5 }, 'LOW', 5), true);
  assert.equal(inventoryMatchesStockFilter({ stock: null }, 'UNMANAGED', 5), true);
  assert.equal(inventoryMatchesStockFilter({ stock: 'bad' }, 'LOW', 5), false);
  assert.equal(inventoryMatchesStockFilter({ stock: 'bad' }, 'UNKNOWN', 5), true);
});

test('products filters by SKU semantics and excludes unknown from in stock', () => {
  const mixed = { variants: [{ quantityAvailable: 0 }, { quantityAvailable: 8 }] };
  const unknown = { variants: [{ quantityAvailable: 'bad' }] };
  assert.equal(productMatchesStockFilter(mixed, 'out', 5), true);
  assert.equal(productMatchesStockFilter({ status: 'AVAILABLE', variants: [{ quantityAvailable: 0 }, { quantityAvailable: 0 }] }, 'out', 5), true);
  assert.equal(productMatchesStockFilter(mixed, 'in', 5), false);
  assert.equal(productMatchesStockFilter(unknown, 'in', 5), false);
  assert.equal(productMatchesStockFilter(unknown, 'unknown', 5), true);
});

test('inventory summary excludes unknown rows from managed totals and actions', () => {
  const rows = [{ stock: 8 }, { stock: 5 }, { stock: null }, { stock: 'bad' }];
  assert.deepEqual(inventoryRowsSummary(rows, 5), {
    managedRows: [rows[0], rows[1]],
    outOfStockRows: [],
    lowStockRows: [rows[1]],
    unmanagedRows: [rows[2]],
    unknownRows: [rows[3]],
    totalStock: 13,
  });
  assert.equal(inventoryRowCanMutate(rows[0], 5), true);
  assert.equal(inventoryRowCanMutate(rows[2], 5), false);
  assert.equal(inventoryRowCanMutate(rows[3], 5), false);
});

test('latest catalog fetcher keeps newest products and categories when older responses resolve last', async () => {
  const oldRequest = deferred();
  const state = { products: [] };
  const fetchLatest = createLatestCatalogFetcher({
    load: () => oldRequest.promise,
    map: (items) => items,
    commit: (items) => { state.products = items; },
  });
  const oldLoad = fetchLatest();
  const newest = [{ id: 2 }];
  const newLoad = createLatestCatalogFetcher;
  assert.equal(typeof newLoad, 'function');
  fetchLatest.setLoad(() => Promise.resolve(newest));
  await fetchLatest();
  oldRequest.resolve([{ id: 1 }]);
  await oldLoad;
  assert.deepEqual(state.products, newest);

  const oldCategories = deferred();
  const categoryState = { categories: [] };
  const fetchCategories = createLatestCatalogFetcher({
    load: () => oldCategories.promise,
    map: (items) => items,
    commit: (items) => { categoryState.categories = items; },
  });
  const oldCategoryLoad = fetchCategories();
  const newestCategories = [{ id: 20 }];
  fetchCategories.setLoad(() => Promise.resolve(newestCategories));
  await fetchCategories();
  oldCategories.resolve([{ id: 10 }]);
  await oldCategoryLoad;
  assert.deepEqual(categoryState.categories, newestCategories);
});
