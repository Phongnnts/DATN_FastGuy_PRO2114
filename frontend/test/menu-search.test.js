import test from 'node:test';
import assert from 'node:assert/strict';
import { menuSearchSuggestions } from '../src/utils/menuSearch.js';

const products = [
  { productId: 1, name: 'Bánh mì gà', description: 'Gà xé sốt cay', price: 30000 },
  { productId: 2, name: 'Burger bò', description: 'Phô mai', price: 59000 },
  { productId: 3, name: 'Cơm gà', description: 'Gà nướng', price: 45000 },
  { productId: 4, name: 'Gà rán', description: 'Giòn cay', price: 40000 },
  { productId: 5, name: 'Pizza gà', description: 'Phô mai gà', price: 79000 },
  { productId: 6, name: 'Wrap gà', description: 'Rau tươi', price: 49000 },
];

test('menu suggestions match Vietnamese text without accents and cap results', () => {
  const suggestions = menuSearchSuggestions(products, 'ga');
  assert.equal(suggestions.length, 5);
  assert.deepEqual(suggestions.map(item => item.productId), [4, 1, 3, 5, 6]);
});

test('menu suggestions prioritize product names before descriptions', () => {
  const suggestions = menuSearchSuggestions(products, 'pho mai');
  assert.deepEqual(suggestions.map(item => item.productId), [2, 5]);
});

test('menu suggestions return nothing for blank queries', () => {
  assert.deepEqual(menuSearchSuggestions(products, '  '), []);
});
