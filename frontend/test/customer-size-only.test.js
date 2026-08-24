import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('../src/views/guest/ProductDetailPage.vue', import.meta.url), 'utf8');

test('customer product detail keeps sizes and removes modifier options', () => {
  assert.match(source, /product\.variants/);
  assert.match(source, /variant\.isDefault/);
  assert.doesNotMatch(source, /product\.modifierGroups|selectedModifiers|toggleModifier|modifierErrors/);
  assert.match(source, /cart\.addItem\(product\.value\.productId, selectedVariant\.value\.variantId, quantity\.value, \[\]\)/);
});
