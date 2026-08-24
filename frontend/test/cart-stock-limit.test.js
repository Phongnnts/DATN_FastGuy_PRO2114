import assert from 'node:assert/strict';
import test from 'node:test';
import { cartStockLimit } from '../src/utils/cartStock.js';

test('cart stock uses recipe remaining servings instead of stale quantityAvailable', () => {
  assert.equal(cartStockLimit({ inventoryMode: 'INGREDIENT', remainingServings: 500, quantityAvailable: 3 }), 500);
  assert.equal(cartStockLimit({ remainingServings: 583, quantityAvailable: 3 }), 583);
  assert.equal(cartStockLimit({ inventoryMode: 'FINISHED_GOOD', remainingServings: 8, quantityAvailable: 3 }), 8);
  assert.equal(cartStockLimit({ inventoryMode: 'UNTRACKED', quantityAvailable: 3 }), null);
});
