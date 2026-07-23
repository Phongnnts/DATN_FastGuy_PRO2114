import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const checkout = readFileSync(new URL('../src/views/user/CheckoutPage.vue', import.meta.url), 'utf8');

test('only shows the place-order button on the payment step', () => {
  assert.match(checkout, /v-if="currentStep === 3"[\s\S]*?class="btn btn-lg btn-primary checkout-btn"/);
});
