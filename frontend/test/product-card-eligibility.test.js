import test from 'node:test';
import assert from 'node:assert/strict';
import { canDirectAddProduct } from '../src/utils/productCard.js';

const product = (overrides = {}) => ({
  productId: 1,
  inStock: true,
  isAvailableNow: true,
  defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: null },
  variants: [
    { variantId: 10, status: 'AVAILABLE', quantityAvailable: null },
    { variantId: 11, status: 'AVAILABLE', quantityAvailable: 5 },
  ],
  modifierGroups: [],
  ...overrides,
});

test('direct add allows selectable available default with multiple variants and positive or unlimited stock', () => {
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: 5 } })), true);
  assert.equal(canDirectAddProduct(product()), true);
});

test('direct add blocks an unavailable default variant', () => {
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'UNAVAILABLE', quantityAvailable: 5 } })), false);
});
