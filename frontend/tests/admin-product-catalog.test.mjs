import assert from 'node:assert/strict';
import test from 'node:test';
import {
  catalogCounts,
  filterProducts,
  paginateProducts,
  productTypes,
} from '../src/utils/adminProductCatalog.js';

const products = [
  { id: 1, name: 'Burger', productType: 'SIMPLE', status: 'AVAILABLE', inStock: true, discountPrice: null, categoryId: 1, categoryName: 'Món chính', basePrice: 50000, variants: [] },
  { id: 2, name: 'Combo', productType: 'COMBO', status: 'AVAILABLE', inStock: false, discountPrice: 80000, categoryId: 2, categoryName: 'Combo', basePrice: 100000, variants: [] },
  { id: 3, name: 'Pizza', productType: 'VARIANT', status: 'UNAVAILABLE', inStock: false, discountPrice: 0, categoryId: 1, categoryName: 'Món chính', basePrice: 90000, variants: [{ variantName: 'L', sku: 'PIZZA-L' }] },
];

test('productTypes returns actual unique canonical values', () => {
  assert.deepEqual(productTypes(products), ['COMBO', 'SIMPLE', 'VARIANT']);
});

test('filterProducts applies canonical product type with search and category', () => {
  assert.deepEqual(filterProducts(products, { query: 'combo', categoryId: '2', productType: 'COMBO' }).map((product) => product.id), [2]);
  assert.deepEqual(filterProducts(products, { query: 'pizza-l', productType: 'VARIANT' }).map((product) => product.id), [3]);
});

test('catalogCounts uses canonical availability stock and discount fields', () => {
  assert.deepEqual(catalogCounts(products), { total: 3, available: 2, outOfStock: 2, discounted: 1 });
});

test('paginateProducts clamps pages and returns stable bounds', () => {
  assert.deepEqual(paginateProducts(products, 2, 2), { page: 2, pageCount: 2, start: 2, end: 3, items: [products[2]] });
  assert.deepEqual(paginateProducts(products, 9, 2), { page: 2, pageCount: 2, start: 2, end: 3, items: [products[2]] });
  assert.deepEqual(paginateProducts([], 1, 10), { page: 1, pageCount: 1, start: 0, end: 0, items: [] });
});
