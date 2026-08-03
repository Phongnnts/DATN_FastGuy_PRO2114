import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const checkout = read('../src/views/user/CheckoutPage.vue');
const router = read('../src/router/index.js');
const detail = read('../src/views/user/OrderDetailPage.vue');
const tracking = read('../src/views/guest/TrackOrderPage.vue');
const success = read('../src/views/user/OrderSuccessPage.vue');

test('routes successful COD checkout through safe success page', () => {
  assert.match(router, /path: 'order-success'[\s\S]*name: 'OrderSuccess'/);
  assert.match(checkout, /name: 'OrderSuccess'/);
  assert.doesNotMatch(success, /token|returnProof|phoneSuffix|checkoutUrl/);
});

test('polls active authenticated and guest orders every 30 seconds and cleans timers', () => {
  for (const source of [detail, tracking]) {
    assert.match(source, /setInterval\([\s\S]*30000/);
    assert.match(source, /\['DELIVERED', 'CANCELLED'\]\.includes/);
    assert.match(source, /onBeforeUnmount\(\(\) => \{[\s\S]*clearInterval\(pollTimer\)[\s\S]*\}\)/);
    assert.match(source, /stopped = true/);
  }
});
