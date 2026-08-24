import assert from 'node:assert/strict';
import test from 'node:test';
import { MAX_PRODUCT_QUANTITY, productQuantityInCart, validateProductQuantity } from '../src/utils/cartQuantityPolicy.js';

test('cart quantity limit is twenty across variants and modifiers of one product', () => {
  const items=[{productId:7,quantity:8,key:'a'},{productId:7,quantity:12,key:'b'},{productId:8,quantity:20,key:'c'}];
  assert.equal(MAX_PRODUCT_QUANTITY,20);
  assert.equal(productQuantityInCart(items,7),20);
  assert.equal(validateProductQuantity(items,7,1).allowed,false);
  assert.equal(validateProductQuantity(items,7,0).allowed,true);
  assert.equal(validateProductQuantity(items,7,12,'b').allowed,true);
  assert.equal(validateProductQuantity(items,7,13,'b').allowed,false);
});
