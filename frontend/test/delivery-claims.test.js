import test from 'node:test';
import assert from 'node:assert/strict';
import { createStoreConfigController, normalizeEstimatedDeliveryMinutes } from '../src/utils/deliveryClaims.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

test('accepts only finite integer delivery estimates from 10 through 180', () => {
  for (const value of [10, 30, 180]) assert.equal(normalizeEstimatedDeliveryMinutes(value), value);
  for (const value of [null, undefined, '', '30', 9, 181, 10.5, NaN, Infinity, -Infinity]) {
    assert.equal(normalizeEstimatedDeliveryMinutes(value), null);
  }
});

test('store config loading starts without blocking independent product work', async () => {
  const config = deferred();
  const applied = [];
  const controller = createStoreConfigController({
    requestConfig: () => config.promise,
    applyEstimate: (value) => applied.push(value),
  });

  const configLoad = controller.load();
  const product = await Promise.resolve('product-ready');

  assert.equal(product, 'product-ready');
  assert.deepEqual(applied, []);
  config.resolve({ estimatedDeliveryMinutes: 30 });
  await configLoad;
  assert.deepEqual(applied, [30]);
});

test('store config controller ignores stale and unmounted responses', async () => {
  const first = deferred();
  const second = deferred();
  const afterUnmount = deferred();
  const requests = [first, second, afterUnmount];
  const applied = [];
  const controller = createStoreConfigController({
    requestConfig: () => requests.shift().promise,
    applyEstimate: (value) => applied.push(value),
  });

  const older = controller.load();
  const newer = controller.load();
  second.resolve({ estimatedDeliveryMinutes: 45 });
  first.resolve({ estimatedDeliveryMinutes: 20 });
  await Promise.all([older, newer]);

  const unmounted = controller.load();
  controller.stop();
  afterUnmount.resolve({ estimatedDeliveryMinutes: 60 });
  await unmounted;

  assert.deepEqual(applied, [45]);
});

test('invalid and failed config responses hide delivery estimate', async () => {
  const responses = [
    Promise.resolve({ estimatedDeliveryMinutes: 9 }),
    Promise.reject(new Error('config unavailable')),
  ];
  const applied = [];
  const controller = createStoreConfigController({
    requestConfig: () => responses.shift(),
    applyEstimate: (value) => applied.push(value),
  });

  await controller.load();
  await controller.load();

  assert.deepEqual(applied, [null, null]);
});
