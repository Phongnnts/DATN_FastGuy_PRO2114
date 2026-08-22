import assert from 'node:assert/strict';
import test from 'node:test';
import { resolveProductDetailPricing } from '../src/utils/productDetailPricing.js';

const cases = [
  {
    name: 'current contract uses price as current and originalPrice as crossed old price',
    product: { price: 59000, originalPrice: 75000, discountPercent: 20 },
    expected: { currentPrice: 59000, crossedPrice: 75000, discountPercent: 20 },
  },
  {
    name: 'legacy contract uses a valid lower discountPrice as current price',
    product: { price: 75000, discountPrice: 59000 },
    expected: { currentPrice: 59000, crossedPrice: 75000, discountPercent: 21 },
  },
  {
    name: 'product without a real discount has no crossed price or percent',
    product: { price: 59000, originalPrice: 59000, discountPrice: 75000 },
    expected: { currentPrice: 59000, crossedPrice: null, discountPercent: null },
  },
  {
    name: 'selected variant current and old prices override product prices',
    product: { price: 59000, originalPrice: 75000 },
    variant: { price: 69000, originalPrice: 90000 },
    expected: { currentPrice: 69000, crossedPrice: 90000, discountPercent: 23 },
  },
];

for (const { name, product, variant, expected } of cases) {
  test(name, () => {
    assert.deepEqual(resolveProductDetailPricing(product, variant), expected);
  });
}
