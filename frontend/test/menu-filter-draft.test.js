import test from 'node:test';
import assert from 'node:assert/strict';
import * as menuFilters from '../src/utils/menuFilters.js';
import {
  applyMenuFilterDraft,
  createMenuFilterDraft,
  buildMenuCategoryGroups,
  menuFilterCount,
  paginationRange,
  quickFilterParams,
} from '../src/utils/menuFilters.js';

const active = {
  price: '30_60',
  min: '',
  max: '',
  availability: 'AVAILABLE',
  discounted: true,
  bestSeller: false,
};

test('mobile filter draft stays independent until applied', () => {
  const draft = createMenuFilterDraft(active);
  draft.price = 'OVER_60';
  draft.bestSeller = true;

  assert.equal(active.price, '30_60');
  assert.equal(active.bestSeller, false);
  assert.deepEqual(applyMenuFilterDraft(draft), {
    price: 'OVER_60',
    min: '',
    max: '',
    availability: 'AVAILABLE',
    discounted: true,
    bestSeller: true,
  });
});

test('drawer opens with effective quick filter values and unchanged apply preserves quick keys', () => {
  const state = {
    price: 'ALL', min: '', max: '', availability: 'ALL', discounted: false, bestSeller: false,
    quickFilters: ['available', 'discounted', 'bestSeller', 'under40'],
  };
  const draft = menuFilters.createEffectiveMenuFilterDraft(state);
  assert.deepEqual(draft, {
    price: 'CUSTOM', min: '', max: '40000', availability: 'AVAILABLE', discounted: true, bestSeller: true,
  });
  assert.deepEqual(menuFilters.applyEffectiveMenuFilterDraft(state, draft), state);
});

test('explicit drawer changes replace corresponding quick keys with detailed state', () => {
  const state = {
    price: 'ALL', min: '', max: '', availability: 'ALL', discounted: false, bestSeller: false,
    quickFilters: ['available', 'discounted', 'bestSeller', 'under40'],
  };
  const applied = menuFilters.applyEffectiveMenuFilterDraft(state, {
    price: 'CUSTOM', min: '', max: '50000', availability: 'OUT_OF_STOCK', discounted: false, bestSeller: false,
  });
  assert.deepEqual(applied, {
    price: 'CUSTOM', min: '', max: '50000', availability: 'OUT_OF_STOCK', discounted: false, bestSeller: false,
    quickFilters: [],
  });
});

test('editing minimum price removes under40 and preserves the entered range', () => {
  const state = {
    price: 'ALL', min: '', max: '', availability: 'ALL', discounted: false, bestSeller: false,
    quickFilters: ['under40'],
  };
  assert.deepEqual(menuFilters.applyEffectiveMenuFilterDraft(state, {
    price: 'CUSTOM', min: '10000', max: '40000', availability: 'ALL', discounted: false, bestSeller: false,
  }), {
    price: 'CUSTOM', min: '10000', max: '40000', availability: 'ALL', discounted: false, bestSeller: false,
    quickFilters: [],
  });
});

test('hydrate deduplicates quick query keys while preserving first URL order', () => {
  assert.deepEqual(
    menuFilters.hydrateMenuFilterState({ quick: 'under40,available,under40,discounted,available,bestSeller' }).quickFilters,
    ['under40', 'available', 'discounted', 'bestSeller'],
  );
});

test('hydrate remove and reset normalize quick and detailed state without duplicates', () => {
  const hydrated = menuFilters.hydrateMenuFilterState({
    quick: 'available,discounted,bestSeller,under40', availability: 'OUT_OF_STOCK', discounted: 'true', bestSeller: 'true',
  });
  assert.deepEqual(hydrated, {
    price: 'ALL', min: '', max: '', availability: 'ALL', discounted: false, bestSeller: false,
    quickFilters: ['available', 'discounted', 'bestSeller', 'under40'],
  });
  assert.deepEqual(menuFilters.removeMenuFilter(hydrated, 'quick:available'), {
    ...hydrated, quickFilters: ['discounted', 'bestSeller', 'under40'],
  });
  assert.deepEqual(menuFilters.resetMenuFilters(), {
    price: 'ALL', min: '', max: '', availability: 'ALL', discounted: false, bestSeller: false, quickFilters: [],
  });
});

test('filter badge counts only refinement groups', () => {
  assert.equal(menuFilterCount(active), 3);
  assert.equal(menuFilterCount({ price: 'ALL', availability: 'ALL', discounted: false, bestSeller: false }), 0);
});

test('pagination range reports visible one-based item bounds', () => {
  assert.deepEqual(paginationRange(1, 12, 133), { from: 1, to: 12 });
  assert.deepEqual(paginationRange(12, 12, 133), { from: 133, to: 133 });
  assert.deepEqual(paginationRange(1, 12, 0), { from: 0, to: 0 });
});

test('menu category groups preserve every API category as a distinct ordered filter', () => {
  const categories = [
    { id: 1, name: 'Burger', productCount: 9 },
    { id: 2, name: 'Cơm', productCount: 5 },
    { id: 3, name: 'Cơm Tấm', productCount: 4 },
    { id: 4, name: 'Cơm Rang', productCount: 5 },
  ];

  assert.deepEqual(buildMenuCategoryGroups(categories, 23), [
    { key: 'all', name: 'Tất cả', count: 23, categoryIds: [], children: [] },
    { key: '1', name: 'Burger', count: 9, categoryIds: [1], children: [] },
    { key: '2', name: 'Cơm', count: 5, categoryIds: [2], children: [] },
    { key: '3', name: 'Cơm Tấm', count: 4, categoryIds: [3], children: [] },
    { key: '4', name: 'Cơm Rang', count: 5, categoryIds: [4], children: [] },
  ]);
});

test('quick filter presets map exactly to supported catalog query parameters', () => {
  assert.deepEqual(quickFilterParams(['bestSeller', 'discounted', 'under40', 'available']), {
    sold: 1,
    discounted: true,
    maxPrice: 40000,
    availability: 'AVAILABLE',
  });
  assert.deepEqual(quickFilterParams(['officeCombo', 'studentCombo']), {});
});
