import test from 'node:test';
import assert from 'node:assert/strict';
import { createCouponController } from '../src/utils/checkoutCoupon.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}

function setup() {
  const requests = [];
  const state = { code: '', applied: null, discount: 0, verifying: false, error: '' };
  const controller = createCouponController(state, (code, subtotal, shippingFee) => {
    const request = deferred();
    requests.push({ code, subtotal, shippingFee, ...request });
    return request.promise;
  });
  return { state, controller, requests };
}

test('latest explicit verification wins when responses resolve out of order', async () => {
  const { state, controller, requests } = setup();
  state.code = ' first ';
  const first = controller.verify(100000, 15000);
  state.code = 'second';
  const second = controller.verify(100000, 15000);

  requests[1].resolve({ valid: true, code: 'SECOND', discount: 20000 });
  await second;
  requests[0].resolve({ valid: true, code: 'FIRST', discount: 10000 });
  await first;

  assert.deepEqual(requests.map(({ code, subtotal, shippingFee }) => ({ code, subtotal, shippingFee })), [
    { code: 'first', subtotal: 100000, shippingFee: 15000 },
    { code: 'second', subtotal: 100000, shippingFee: 15000 },
  ]);
  assert.equal(state.applied.code, 'SECOND');
  assert.equal(state.discount, 20000);
});

test('totals invalidation rejects stale response and permits a new explicit verification', async () => {
  const { state, controller, requests } = setup();
  state.code = 'SAVE10';
  const stale = controller.verify(100000, 15000);
  controller.invalidate();
  assert.equal(state.applied, null);
  assert.equal(state.verifying, false);

  const current = controller.verify(120000, 20000);
  requests[0].resolve({ valid: true, code: 'SAVE10', discount: 10000 });
  await stale;
  assert.equal(state.applied, null);
  requests[1].resolve({ valid: true, code: 'SAVE10', discount: 12000 });
  await current;

  assert.equal(state.applied.code, 'SAVE10');
  assert.equal(state.discount, 12000);
});

test('current success error and remove update canonical coupon state', async () => {
  const { state, controller, requests } = setup();
  state.code = 'BAD';
  const invalid = controller.verify(50000, 0);
  requests[0].resolve({ valid: false, message: 'Không đủ điều kiện' });
  await invalid;
  assert.equal(state.error, 'Không đủ điều kiện');

  state.code = 'BROKEN';
  const failed = controller.verify(50000, 0);
  requests[1].reject(new Error('offline'));
  await failed;
  assert.equal(state.error, 'Lỗi kiểm tra mã');

  state.code = 'OK';
  const success = controller.verify(50000, 0);
  requests[2].resolve({ valid: true, code: 'OK', discount: 5000 });
  await success;
  assert.equal(state.applied.code, 'OK');
  controller.remove();
  assert.deepEqual(state, { code: '', applied: null, discount: 0, verifying: false, error: '' });
});
