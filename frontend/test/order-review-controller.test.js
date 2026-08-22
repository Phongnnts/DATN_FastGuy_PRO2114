import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { createOrderReviewController, uniqueReviewProducts } from '../src/utils/orderReviewController.js';

const item = (productId, overrides = {}) => ({ productId, productName: `Món ${productId}`, image: `${productId}.jpg`, variantId: productId * 10, ...overrides });

test('review products dedupe order lines by productId and keep first product display', () => {
  assert.deepEqual(uniqueReviewProducts([
    item(1),
    item(1, { productName: 'Biến thể khác', variantId: 11 }),
    item(2),
  ]), [item(1), item(2)]);
});

test('controller fetches grouped reviews once and hydrates independent product state', async () => {
  let calls = 0;
  const controller = createOrderReviewController({
    getByOrder: async orderId => {
      calls += 1;
      assert.equal(orderId, 9);
      return { orderId, reviews: [{ reviewId: 7, productId: 2, rating: 4, comment: 'Ổn', createdAt: '2026-08-22T10:00:00' }] };
    },
    create: async () => assert.fail('create must not run'),
  });

  await controller.load(9, [item(1), item(1, { variantId: 11 }), item(2)]);

  assert.equal(calls, 1);
  assert.deepEqual(controller.products.map(product => product.productId), [1, 2]);
  assert.equal(controller.stateFor(1).status, 'idle');
  assert.equal(controller.stateFor(1).form.rating, 5);
  assert.equal(controller.stateFor(2).status, 'reviewed');
  assert.equal(controller.stateFor(2).review.comment, 'Ổn');
});

test('submissions use product-scoped payload and keep locks errors success independent', async () => {
  let releaseFirst;
  const firstPending = new Promise(resolve => { releaseFirst = resolve; });
  const payloads = [];
  const controller = createOrderReviewController({
    getByOrder: async orderId => ({ orderId, reviews: [] }),
    create: async payload => {
      payloads.push(payload);
      if (payload.productId === 1) await firstPending;
      if (payload.productId === 2) throw new Error('Không gửi được món 2');
      return { reviewId: 10, productId: payload.productId, rating: payload.rating, comment: payload.comment, createdAt: '2026-08-22T10:00:00' };
    },
  });
  await controller.load(9, [item(1), item(2)]);
  controller.stateFor(1).form = { rating: 3, comment: '  Ngon  ' };
  controller.stateFor(2).form = { rating: 2, comment: '' };

  const first = controller.submit(1);
  assert.equal(controller.stateFor(1).submitting, true);
  assert.equal((await controller.submit(1)).ignored, true);
  await controller.submit(2);
  assert.equal(controller.stateFor(1).submitting, true);
  assert.equal(controller.stateFor(2).submitting, false);
  assert.equal(controller.stateFor(2).status, 'error');
  assert.equal(controller.stateFor(2).error, 'Không gửi được món 2');
  releaseFirst();
  await first;

  assert.deepEqual(payloads, [
    { orderId: 9, productId: 1, rating: 3, comment: 'Ngon' },
    { orderId: 9, productId: 2, rating: 2, comment: null },
  ]);
  assert.equal(controller.stateFor(1).status, 'success');
  assert.equal(controller.stateFor(1).submitting, false);
  assert.equal(controller.stateFor(2).status, 'error');
});

test('failed grouped load keeps initialized forms and retry hydrates reviews', async () => {
  let attempts = 0;
  const controller = createOrderReviewController({
    getByOrder: async orderId => {
      attempts += 1;
      if (attempts === 1) throw new Error('Không tải được trạng thái đánh giá');
      return { orderId, reviews: [{ reviewId: 8, productId: 2, rating: 5, comment: 'Ngon', createdAt: '2026-08-22T10:00:00' }] };
    },
    create: async () => assert.fail('create must not run'),
  });

  const first = controller.load(9, [item(1), item(2)]);
  assert.deepEqual(controller.products.map(product => product.productId), [1, 2]);
  assert.equal(controller.stateFor(1).form.rating, 5);
  await first;
  assert.equal(controller.loadError, 'Không tải được trạng thái đánh giá');

  await controller.load(9, [item(1), item(2)]);
  assert.equal(controller.loadError, '');
  assert.equal(controller.stateFor(2).status, 'reviewed');
});

test('409 submit reloads grouped status and keeps draft when canonical reload fails', async () => {
  let conflictReloadFails = false;
  let loads = 0;
  const conflict = Object.assign(new Error('ALREADY_REVIEWED'), { status: 409 });
  const controller = createOrderReviewController({
    getByOrder: async orderId => {
      loads += 1;
      if (loads === 1) return { orderId, reviews: [] };
      if (conflictReloadFails) throw new Error('Không thể đồng bộ đánh giá');
      return { orderId, reviews: [{ reviewId: 9, productId: 1, rating: 4, comment: 'Đã lưu', createdAt: '2026-08-22T10:00:00' }] };
    },
    create: async () => { throw conflict; },
  });
  await controller.load(9, [item(1), item(2)]);
  controller.stateFor(1).form = { rating: 4, comment: 'Bản nháp' };
  await controller.submit(1);
  assert.equal(controller.stateFor(1).status, 'reviewed');
  assert.equal(controller.stateFor(1).review.comment, 'Đã lưu');

  const failed = createOrderReviewController({
    getByOrder: async orderId => loads++ === 2 ? { orderId, reviews: [] } : Promise.reject(new Error('Không thể đồng bộ đánh giá')),
    create: async () => { throw conflict; },
  });
  await failed.load(9, [item(1)]);
  failed.stateFor(1).form = { rating: 2, comment: 'Giữ tôi' };
  await failed.submit(1);
  assert.deepEqual(failed.stateFor(1).form, { rating: 2, comment: 'Giữ tôi' });
  assert.equal(failed.stateFor(1).status, 'error');
  assert.match(failed.stateFor(1).error, /Không thể đồng bộ đánh giá/);
});

test('load generation and stop ignore stale fetch and submit completions', async () => {
  let releaseOld;
  let releaseSubmit;
  const old = new Promise(resolve => { releaseOld = resolve; });
  const pendingSubmit = new Promise(resolve => { releaseSubmit = resolve; });
  let loadCall = 0;
  const controller = createOrderReviewController({
    getByOrder: async orderId => ++loadCall === 1 ? old : { orderId, reviews: [{ reviewId: 2, productId: 2, rating: 5, comment: null, createdAt: '2026-08-22T10:00:00' }] },
    create: async payload => { await pendingSubmit; return { reviewId: 3, ...payload, createdAt: '2026-08-22T10:00:00' }; },
  });
  const stale = controller.load(9, [item(1)]);
  await controller.load(10, [item(2)]);
  releaseOld({ orderId: 9, reviews: [{ reviewId: 1, productId: 1, rating: 1, comment: null, createdAt: '2026-08-22T10:00:00' }] });
  assert.equal((await stale).ignored, true);
  assert.deepEqual(controller.products.map(product => product.productId), [2]);
  controller.stateFor(2).review = null;
  controller.stateFor(2).status = 'idle';

  const submit = controller.submit(2);
  controller.stop();
  releaseSubmit();
  assert.equal((await submit).ignored, true);
  assert.equal(controller.stateFor(2).review, null);
});

test('order item refresh preserves active drafts and reviewed states', async () => {
  const controller = createOrderReviewController({ getByOrder: async orderId => ({ orderId, reviews: [] }), create: async () => assert.fail() });
  await controller.load(9, [item(1), item(2)]);
  controller.stateFor(1).status = 'editing';
  controller.stateFor(1).form = { rating: 3, comment: 'Đang viết' };
  controller.stateFor(2).status = 'reviewed';
  controller.stateFor(2).review = { reviewId: 2, productId: 2, rating: 5, comment: 'Xong' };

  controller.initialize(9, [item(1, { variantId: 11 }), item(2)]);

  assert.deepEqual(controller.stateFor(1).form, { rating: 3, comment: 'Đang viết' });
  assert.equal(controller.stateFor(1).status, 'editing');
  assert.equal(controller.stateFor(2).review.comment, 'Xong');
});

test('StarRating exposes an explicit accessible group label and keyboard behavior', async () => {
  const source = await readFile(new URL('../src/components/common/StarRating.vue', import.meta.url), 'utf8');
  assert.match(source, /label: \{ type: String, default: '' \}/);
  assert.match(source, /:aria-label="label \|\|/);
  assert.match(source, /\['ArrowRight', 'ArrowUp', 'ArrowLeft', 'ArrowDown', 'Home', 'End'\]/);
  assert.match(source, /role="radio"/);
});

test('OrderDetail wires accessible delivered product review forms and recovery', async () => {
  const source = await readFile(new URL('../src/views/user/OrderDetailPage.vue', import.meta.url), 'utf8');
  assert.match(source, /createOrderReviewController/);
  assert.match(source, /v-if="isDelivered"/);
  assert.match(source, /reviewController\.initialize/);
  assert.match(source, /reviewController\.stop\(\)/);
  assert.match(source, /<form[^>]*@submit\.prevent="submitReview\(product\.productId\)"/);
  assert.match(source, /<StarRating[^>]*:label="`Số sao cho \$\{product\.productName\}`"/);
  assert.match(source, /maxlength="1000"/);
  assert.match(source, /1000 - reviewState\(product\.productId\)\.form\.comment\.length/);
  assert.match(source, /role="status" aria-live="polite"/);
  assert.match(source, /role="alert"/);
  assert.match(source, /retryReviews/);
  assert.match(source, /min-height: 44px/);
  assert.doesNotMatch(source, /homepageConsent|review-consent/);
  assert.match(source, /Kích cỡ/);
  assert.match(source, /reorderController\.run/);
  assert.match(source, /refundLabel/);
  assert.doesNotMatch(source, /Phí dịch vụ|Phí phục vụ|Support|Combo/);
});
