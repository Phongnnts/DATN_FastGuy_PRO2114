import test from 'node:test';
import assert from 'node:assert/strict';
import {
  applyMenuFilterDraft,
  createMenuFilterDraft,
  buildMenuCategoryGroups,
  matchesMenuDiscovery,
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

test('filter badge counts only refinement groups', () => {
  assert.equal(menuFilterCount(active), 3);
  assert.equal(menuFilterCount({ price: 'ALL', availability: 'ALL', discounted: false, bestSeller: false }), 0);
});

test('pagination range reports visible one-based item bounds', () => {
  assert.deepEqual(paginationRange(1, 12, 133), { from: 1, to: 12 });
  assert.deepEqual(paginationRange(12, 12, 133), { from: 133, to: 133 });
  assert.deepEqual(paginationRange(1, 12, 0), { from: 0, to: 0 });
});

test('menu category groups merge rice categories without changing source taxonomy', () => {
  const categories = [
    { id: 1, name: 'Burger', productCount: 9 },
    { id: 2, name: 'Cơm', productCount: 5 },
    { id: 3, name: 'Cơm Tấm', productCount: 4 },
    { id: 4, name: 'Cơm Rang', productCount: 5 },
  ];

  const groups = buildMenuCategoryGroups(categories, 23);
  assert.deepEqual(groups.find(group => group.key === 'rice'), {
    key: 'rice',
    name: 'Cơm',
    count: 14,
    categoryIds: [2, 3, 4],
    children: [
      { id: 2, name: 'Cơm', count: 5 },
      { id: 3, name: 'Cơm Tấm', count: 4 },
      { id: 4, name: 'Cơm Rang', count: 5 },
    ],
  });
  assert.equal(groups[0].count, 23);
});

test('quick filter presets map to supported catalog query parameters', () => {
  assert.deepEqual(quickFilterParams(['bestSeller', 'discounted', 'under40']), {
    sold: 1,
    discounted: true,
    maxPrice: 40000,
  });
});

test('combo discovery matches exact normalized phrase in product name or description', () => {
  assert.equal(matchesMenuDiscovery({ name: 'Combo văn phòng', description: '' }, 'officeCombo'), true);
  assert.equal(matchesMenuDiscovery({ name: 'Cơm trưa', description: 'Suất dành cho sinh viên' }, 'studentCombo'), true);
  assert.equal(matchesMenuDiscovery({ name: 'Combo tiết kiệm', description: 'Dành cho mọi người' }, 'studentCombo'), false);
});
