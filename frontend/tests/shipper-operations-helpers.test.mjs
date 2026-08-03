import assert from 'node:assert/strict';
import test from 'node:test';
import { acceptsShipperRequest, isActiveShipperMode, validateExactCod } from '../src/utils/shipperOperations.js';

test('shipper request acceptance rejects stale generation mode and stopped owner', () => {
  assert.equal(acceptsShipperRequest({ requestGeneration: 3, latestGeneration: 3, requestMode: 'active', currentMode: 'active', stopped: false }), true);
  assert.equal(acceptsShipperRequest({ requestGeneration: 2, latestGeneration: 3, requestMode: 'active', currentMode: 'active', stopped: false }), false);
  assert.equal(acceptsShipperRequest({ requestGeneration: 3, latestGeneration: 3, requestMode: 'active', currentMode: 'history', stopped: false }), false);
  assert.equal(acceptsShipperRequest({ requestGeneration: 3, latestGeneration: 3, requestMode: 'active', currentMode: 'active', stopped: true }), false);
});

test('shipper mode recognizes active route only', () => {
  assert.equal(isActiveShipperMode('ShipperOrders'), true);
  assert.equal(isActiveShipperMode('ShipperOrderHistory'), false);
});

test('exact COD validation requires finite exact total', () => {
  assert.deepEqual(validateExactCod('120000', 120000), { valid: true, amount: 120000 });
  assert.equal(validateExactCod('119999', 120000).valid, false);
  assert.equal(validateExactCod('', 0).valid, false);
  assert.equal(validateExactCod('not-a-number', 120000).valid, false);
});
