import assert from 'node:assert/strict';
import test from 'node:test';
import {
  acceptsKitchenRequest,
  kitchenItemCount,
  matchesKitchenSearch,
  waitingDuration,
  staffOrderDiscount,
  staffOrderItemTotal,
  sortAvailableShippers,
  validDispatchSelections,
  acceptsDispatchRequest,
} from '../src/utils/staffKitchen.js';

test('kitchen search normalizes code, customer name, and phone', () => {
  const order = { orderCode: 'FG-123', customerName: 'Nguyễn An', customerPhone: '090 123' };
  assert.equal(matchesKitchenSearch(order, 'fg-123'), true);
  assert.equal(matchesKitchenSearch(order, 'NGUYỄN'), true);
  assert.equal(matchesKitchenSearch(order, '090 123'), true);
  assert.equal(matchesKitchenSearch(order, 'khác'), false);
});

test('kitchen item count prefers finite canonical value then sums quantities', () => {
  assert.equal(kitchenItemCount({ itemCount: '4', items: [{ quantity: 9 }] }), 4);
  assert.equal(kitchenItemCount({ itemCount: null, items: [{ quantity: '2' }, { quantity: 3 }] }), 5);
  assert.equal(kitchenItemCount({ itemCount: 'bad', items: [{ quantity: 2 }] }), 2);
});

test('waiting duration handles valid, future, and invalid timestamps', () => {
  const now = Date.parse('2026-08-02T12:00:00Z');
  assert.equal(waitingDuration('2026-08-02T11:25:00Z', now), '35 phút');
  assert.equal(waitingDuration('2026-08-02T13:00:00Z', now), '0 phút');
  assert.equal(waitingDuration('invalid', now), 'Chưa rõ');
  assert.equal(waitingDuration(null, now), 'Chưa rõ');
});

test('staff detail uses canonical discount and line total', () => {
  assert.equal(staffOrderDiscount({ discountAmount: '15000', discount: 99 }), 15000);
  assert.equal(staffOrderDiscount({}), 0);
  assert.equal(staffOrderItemTotal({ totalPrice: '42000', unitPrice: 10000, quantity: 3 }), 42000);
  assert.equal(staffOrderItemTotal({ unitPrice: 10000, quantity: 3 }), 30000);
});

test('dispatch helpers sort workload then name and remove invalid selections', () => {
  const sorted = sortAvailableShippers([
    { id: 3, fullName: 'Bình', activeOrderCount: 1 },
    { id: 2, fullName: 'An', activeOrderCount: 1 },
    { id: 1, fullName: 'Cường', activeOrderCount: 0 },
  ]);
  assert.deepEqual(sorted.map(({ id }) => id), [1, 2, 3]);
  assert.deepEqual(validDispatchSelections({ 10: 2, 11: 9 }, sorted), { 10: 2 });
});

test('dispatch request acceptance rejects stale and stopped work', () => {
  assert.equal(acceptsDispatchRequest({ requestGeneration: 2, latestGeneration: 2, stopped: false }), true);
  assert.equal(acceptsDispatchRequest({ requestGeneration: 1, latestGeneration: 2, stopped: false }), false);
  assert.equal(acceptsDispatchRequest({ requestGeneration: 2, latestGeneration: 2, stopped: true }), false);
});

test('request acceptance requires latest generation and active tab', () => {
  assert.equal(acceptsKitchenRequest({ requestGeneration: 2, latestGeneration: 2, requestTab: 'READY', activeTab: 'READY' }), true);
  assert.equal(acceptsKitchenRequest({ requestGeneration: 1, latestGeneration: 2, requestTab: 'PENDING', activeTab: 'READY' }), false);
  assert.equal(acceptsKitchenRequest({ requestGeneration: 2, latestGeneration: 2, requestTab: 'PENDING', activeTab: 'READY' }), false);
});
