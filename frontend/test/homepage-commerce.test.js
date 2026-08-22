import assert from 'node:assert/strict';
import test from 'node:test';
import { homepageRecommendationChips, homepageRecommendations, homepageRanking, newHomepageProducts, signatureHomepageProduct } from '../src/utils/homepageCommerce.js';

const products = [
  { productId: 1, name: 'Gà cay', price: 48000, soldCount: 80, averageRating: 4.8, reviewCount: 12, spiceLevel: 2, productType: 'SIMPLE', isNew: true },
  { productId: 2, name: 'Combo đôi', price: 129000, soldCount: 120, averageRating: 4.7, reviewCount: 20, productType: 'COMBO', isNew: false },
  { productId: 3, name: 'Burger', price: 65000, soldCount: 80, averageRating: 4.9, reviewCount: 14, productType: 'SIMPLE', isNew: true },
];

test('signature homepage product uses the first available bestseller and returns null when empty', () => {
  assert.equal(signatureHomepageProduct(products), products[0]);
  assert.equal(signatureHomepageProduct([]), null);
  assert.equal(signatureHomepageProduct(null), null);
});

test('homepage ranking sorts real sold count then rating without mutating input', () => {
  const source = [...products];
  assert.deepEqual(homepageRanking(products).map(item => item.productId), [2, 3, 1]);
  assert.deepEqual(products, source);
});

test('homepage recommendations only expose chips with matching real products', () => {
  const chips = homepageRecommendationChips(products);
  assert.deepEqual(chips.map(chip => chip.key), ['QUICK', 'FULL', 'SPICY', 'PAIR', 'GROUP', 'UNDER_100']);
  assert.deepEqual(homepageRecommendations(products, 'SPICY').map(item => item.productId), [1]);
  assert.deepEqual(homepageRecommendations(products, 'UNDER_100').map(item => item.productId), [1, 3]);
});

test('new homepage products returns at most four explicitly new products', () => {
  assert.deepEqual(newHomepageProducts([...products, ...products, ...products]).map(item => item.productId), [1, 3, 1, 3]);
  assert.deepEqual(newHomepageProducts(products.map(item => ({ ...item, isNew: false }))), []);
});
