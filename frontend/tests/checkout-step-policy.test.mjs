import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const checkout = readFileSync(new URL('../src/views/user/CheckoutPage.vue', import.meta.url), 'utf8');

test('only shows the place-order button on the payment step', () => {
  assert.match(checkout, /v-if="currentStep === 3"[\s\S]*?class="btn btn-lg btn-primary checkout-btn"/);
});

test('checkout treats HTTP 409 as a stock conflict: refresh availability, actionable message, no automatic retry', () => {
  assert.match(checkout, /if \(e\?\.status === 409\) \{/);
  assert.match(checkout, /CONFLICT_MESSAGE/);
  assert.match(checkout, /cart\.fetchCart\(\)/);
  assert.match(checkout, /productStore\.refreshAvailability\(\)/);
  assert.match(checkout, /clearIdempotencyKey\(\)/);
  assert.equal((checkout.match(/@click="placeOrder"/g) || []).length, 1);
});
