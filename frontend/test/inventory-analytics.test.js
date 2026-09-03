import assert from 'node:assert/strict';
import test from 'node:test';
import { inventoryAnalyticsDelta, inventoryAnalyticsPeriod, normalizeInventoryAnalyticsSeries } from '../src/utils/inventoryAnalytics.js';

test('inventory analytics period builds inclusive presets', () => {
  assert.deepEqual(inventoryAnalyticsPeriod(7, new Date('2026-09-03T12:00:00')), { fromDate: '2026-08-28', toDate: '2026-09-03', granularity: 'DAY' });
});

test('inventory analytics delta handles previous zero truthfully', () => {
  assert.deepEqual(inventoryAnalyticsDelta(110, 100), { value: 10, percent: 10, comparable: true });
  assert.deepEqual(inventoryAnalyticsDelta(10, 0), { value: 10, percent: null, comparable: false });
});

test('inventory analytics series normalizes monetary values', () => {
  assert.deepEqual(normalizeInventoryAnalyticsSeries([{ date: '2026-09-01', inventoryValue: '12.5', receiptValue: null }]), [{ date: '2026-09-01', inventoryValue: 12.5, receiptValue: 0, consumptionValue: 0, wasteValue: 0, adjustmentLossValue: 0, adjustmentGainValue: 0 }]);
});
