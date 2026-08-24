import assert from 'node:assert/strict';
import test from 'node:test';
import { customerAvailability } from '../src/utils/stockPolicy.js';

test('customer availability shows exact servings for every tracked in-stock variant', () => {
  assert.deepEqual(customerAvailability({ availabilityStatus: 'IN_STOCK', remainingServings: 583 }), { status: 'IN_STOCK', remainingServings: 583, label: 'Còn 583 phần', available: true });
  assert.equal(customerAvailability({ availabilityStatus: 'LOW_STOCK', remainingServings: 2 }).label, 'Chỉ còn 2 phần');
  assert.equal(customerAvailability({ availabilityStatus: 'UNTRACKED' }).label, 'Còn hàng');
});
