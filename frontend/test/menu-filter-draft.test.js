import test from 'node:test';
import assert from 'node:assert/strict';
import {
  applyMenuFilterDraft,
  createMenuFilterDraft,
  menuFilterCount,
  paginationRange,
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
