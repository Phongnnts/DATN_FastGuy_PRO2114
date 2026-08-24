import assert from 'node:assert/strict';
import test from 'node:test';
import { normalizePurchaseQuantity } from '../src/utils/purchaseQuantity.js';

test('keyboard purchase quantity is an integer clamped from one through available stock', () => {
  assert.equal(normalizePurchaseQuantity('', null), 1);
  assert.equal(normalizePurchaseQuantity('2.8', null), 2);
  assert.equal(normalizePurchaseQuantity('-4', null), 1);
  assert.equal(normalizePurchaseQuantity('12', 5), 5);
  assert.equal(normalizePurchaseQuantity('25', null), 20);
  assert.equal(normalizePurchaseQuantity('3', 5), 3);
});
