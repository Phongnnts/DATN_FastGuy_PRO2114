import assert from 'node:assert/strict';
import test from 'node:test';
import {
  normalizeLowStockThreshold,
  productStockSummary,
  stockState,
} from '../src/utils/stockPolicy.js';

test('normalizes backend threshold with default five', () => {
  assert.equal(normalizeLowStockThreshold(7), 7);
  assert.equal(normalizeLowStockThreshold('7'), 7);
  assert.equal(normalizeLowStockThreshold(0), 5);
  assert.equal(normalizeLowStockThreshold(1001), 5);
  assert.equal(normalizeLowStockThreshold('bad'), 5);
});

test('classifies managed and unlimited SKU boundaries', () => {
  assert.equal(stockState(null, 5), 'UNMANAGED');
  assert.equal(stockState(0, 5), 'OUT');
  assert.equal(stockState(-1, 5), 'OUT');
  assert.equal(stockState(1, 5), 'LOW');
  assert.equal(stockState(5, 5), 'LOW');
  assert.equal(stockState(6, 5), 'IN');
  assert.equal(stockState('bad', 5), 'UNKNOWN');
  assert.equal(stockState(Number.NaN, 5), 'UNKNOWN');
  assert.equal(stockState(Number.POSITIVE_INFINITY, 5), 'UNKNOWN');
});

test('summarizes product stock without treating unlimited as low', () => {
  const product = { variants: [
    { quantityAvailable: null },
    { quantityAvailable: 0 },
    { quantityAvailable: 5 },
    { quantityAvailable: 8 },
  ] };
  assert.deepEqual(productStockSummary(product, 5), {
    total: null,
    outOfStockSkus: 1,
    lowStockSkus: 1,
    managedSkus: 3,
    unknownSkus: 0,
    status: 'AVAILABLE',
  });
});

test('summarizes malformed quantities without returning NaN', () => {
  assert.deepEqual(productStockSummary({ variants: [
    { quantityAvailable: 'bad' },
    { quantityAvailable: 8 },
  ] }, 5), {
    total: null,
    outOfStockSkus: 0,
    lowStockSkus: 0,
    managedSkus: 1,
    unknownSkus: 1,
    status: 'UNKNOWN',
  });
});

test('product status marks available product out only when all managed SKUs are out', () => {
  assert.equal(productStockSummary({ status: 'AVAILABLE', variants: [{ quantityAvailable: 0 }, { quantityAvailable: 0 }] }, 5).status, 'OUT');
  assert.equal(productStockSummary({ status: 'AVAILABLE', variants: [{ quantityAvailable: 0 }, { quantityAvailable: null }] }, 5).status, 'AVAILABLE');
  assert.equal(productStockSummary({ status: 'UNAVAILABLE', variants: [{ quantityAvailable: 8 }] }, 5).status, 'UNAVAILABLE');
});
