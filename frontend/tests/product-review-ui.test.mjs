import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { createReviewPageController, normalizeReviewPage } from '../src/utils/reviewPage.js';
import { mapProduct } from '../src/utils/productMapper.js';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');
const indexHtml = () => read('../index.html');
const emojiPattern = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u;
const page = (overrides = {}) => ({
  items: [{ reviewId: 1, productId: 7, rating: 5, comment: 'Ngon', userName: 'An', createdAt: '2026-08-22T10:00:00' }],
  total: 1,
  page: 1,
  size: 10,
  averageRating: 5,
  reviewCount: 1,
  ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 1 },
  ...overrides,
});
const deferred = () => {
  let resolve;
  let reject;
  const promise = new Promise((ok, fail) => { resolve = ok; reject = fail; });
  return { promise, resolve, reject };
};

test('review page normalization clamps aggregates and completes all five distribution buckets', () => {
  assert.deepEqual(normalizeReviewPage(page({
    total: -4,
    page: 0,
    size: 100,
    averageRating: 8,
    reviewCount: -2,
    ratingDistribution: { 1: -1, 3: 2.8, 5: 4 },
  })), {
    items: page().items,
    total: 0,
    page: 1,
    size: 50,
    averageRating: 5,
    reviewCount: 0,
    ratingDistribution: { 1: 0, 2: 0, 3: 2, 4: 0, 5: 4 },
  });
});

test('review controller ignores stale and stopped responses', async () => {
  const first = deferred();
  const second = deferred();
  const states = [];
  const requests = [first, second];
  const controller = createReviewPageController({ requestPage: () => requests.shift().promise, applyState: state => states.push(structuredClone(state)) });

  const oldLoad = controller.load(7);
  const newLoad = controller.load(8, { reset: true });
  second.resolve(page({ items: [], total: 0, reviewCount: 0, averageRating: 0, ratingDistribution: {} }));
  await newLoad;
  first.resolve(page());
  await oldLoad;
  assert.equal(states.at(-1).data.total, 0);

  const stopped = deferred();
  const stoppedStates = [];
  const stoppedController = createReviewPageController({ requestPage: () => stopped.promise, applyState: state => stoppedStates.push(state) });
  const pending = stoppedController.load(7);
  stoppedController.stop();
  stopped.resolve(page());
  await pending;
  assert.equal(stoppedStates.length, 1);
  assert.equal(stoppedStates[0].loading, true);
});

test('review controller distinguishes initial failure from nonblocking refresh failure and preserves valid page', async () => {
  const states = [];
  const responses = [Promise.reject(new Error('Mất kết nối')), Promise.resolve(page()), Promise.reject(new Error('Tải trang lỗi'))];
  const controller = createReviewPageController({ requestPage: () => responses.shift(), applyState: state => states.push(structuredClone(state)) });

  await controller.load(7);
  assert.equal(states.at(-1).initialError, 'Mất kết nối');
  assert.equal(states.at(-1).data, null);

  await controller.load(7);
  const valid = structuredClone(states.at(-1).data);
  await controller.load(7);
  assert.deepEqual(states.at(-1).data, valid);
  assert.equal(states.at(-1).page, 1);
  assert.equal(states.at(-1).refreshError, 'Tải trang lỗi');
  assert.equal(states.at(-1).initialError, '');
});

test('review controller enforces server-driven page bounds before requesting', async () => {
  let calls = 0;
  const controller = createReviewPageController({ requestPage: async (productId, params) => { calls += 1; return page({ total: 11, page: params.page }); }, applyState: () => {} });
  await controller.load(7);
  assert.equal(controller.goToPage(0, 7), false);
  assert.equal(controller.goToPage(3, 7), false);
  assert.equal(controller.goToPage(2, 7), true);
  await controller.pending();
  assert.equal(calls, 2);
  assert.equal(controller.snapshot().page, 2);
});

test('failed pagination preserves the visible page and retry repeats the failed target', async () => {
  const requestedPages = [];
  let failPageTwo = true;
  const controller = createReviewPageController({
    requestPage: async (productId, params) => {
      requestedPages.push(params.page);
      if (params.page === 2 && failPageTwo) throw new Error('Trang 2 lỗi');
      return page({ total: 11, page: params.page, items: [{ ...page().items[0], reviewId: params.page }] });
    },
    applyState: () => {},
  });
  await controller.load(7);
  controller.goToPage(2, 7);
  await controller.pending();
  assert.equal(controller.snapshot().page, 1);
  assert.equal(controller.snapshot().data.items[0].reviewId, 1);
  assert.equal(controller.snapshot().refreshError, 'Trang 2 lỗi');

  failPageTwo = false;
  controller.retry();
  await controller.pending();
  assert.deepEqual(requestedPages, [1, 2, 2]);
  assert.equal(controller.snapshot().page, 2);
  assert.equal(controller.snapshot().data.items[0].reviewId, 2);
});

test('review API keeps order methods and requests product reviews with pagination params', async () => {
  const source = await read('../src/api/review.js');
  assert.match(source, /getByProduct\(productId, params = \{ page: 1, size: 10 \}\)/);
  assert.match(source, /client\.get\(`\/reviews\/product\/\$\{productId\}`, \{ params \}\)/);
  assert.match(source, /getByOrder\(orderId\)/);
  assert.match(source, /create\(data\)/);
});

test('product mapper preserves finite rating summaries and clamps invalid values', () => {
  const empty = mapProduct({ averageRating: 0, reviewCount: 0 });
  const rated = mapProduct({ averageRating: 4.24, reviewCount: 16 });
  const invalid = mapProduct({ averageRating: 8, reviewCount: -2 });
  assert.deepEqual([empty.averageRating, empty.reviewCount], [0, 0]);
  assert.deepEqual([rated.averageRating, rated.reviewCount], [4.24, 16]);
  assert.deepEqual([invalid.averageRating, invalid.reviewCount], [5, 0]);
});

test('product mapper preserves real card merchandising fields and zero sold count', () => {
  const mapped = mapProduct({
    averageRating: 4.54,
    reviewCount: 18,
    soldCount: 0,
    bestSeller: true,
    isNew: true,
    discountPercent: 20,
    originalPrice: 75000,
    price: 60000,
  });
  assert.deepEqual({
    averageRating: mapped.averageRating,
    reviewCount: mapped.reviewCount,
    soldCount: mapped.soldCount,
    bestSeller: mapped.bestSeller,
    isNew: mapped.isNew,
    discountPercent: mapped.discountPercent,
    originalPrice: mapped.originalPrice,
  }, {
    averageRating: 4.54,
    reviewCount: 18,
    soldCount: 0,
    bestSeller: true,
    isNew: true,
    discountPercent: 20,
    originalPrice: 75000,
  });
});

test('premium product UI loads approved Font Awesome Free and keeps Bootstrap Icons available', async () => {
  const [html, card, detail] = await Promise.all([
    indexHtml(),
    read('../src/components/common/ProductCard.vue'),
    read('../src/views/guest/ProductDetailPage.vue'),
  ]);
  assert.match(html, /href="https:\/\/cdn\.jsdelivr\.net\/npm\/@fortawesome\/fontawesome-free@6\.7\.2\/css\/all\.min\.css"/);
  assert.match(html, /integrity="sha256-dABdfBfUoC8vJUBOwGVdm8L9qlMWaHTIfXt\+7GnZCIo=" crossorigin="anonymous"/);
  assert.match(html, /href="https:\/\/cdn\.jsdelivr\.net\/npm\/bootstrap-icons@1\.11\.3\/font\/bootstrap-icons\.min\.css"/);
  assert.doesNotMatch(card, emojiPattern);
  assert.doesNotMatch(detail, emojiPattern);
  assert.doesNotMatch(card, /class="bi bi-/);
  assert.doesNotMatch(detail, /class="bi bi-/);
  for (const token of ['fa-solid fa-star', 'fa-solid fa-fire', 'fa-regular fa-heart', 'fa-solid fa-heart', 'fa-solid fa-plus']) assert.match(card, new RegExp(token));
  for (const token of ['fa-solid fa-bolt', 'fa-regular fa-heart', 'fa-solid fa-heart', 'fa-solid fa-circle-check', 'fa-solid fa-circle-xmark', 'fa-solid fa-cart-shopping', 'fa-solid fa-truck-fast', 'fa-solid fa-star']) assert.match(detail, new RegExp(token));
});

test('product card renders premium content, exact Font Awesome icons, actions, and motion policy', async () => {
  const source = await read('../src/components/common/ProductCard.vue');
  assert.match(source, /class="product-rating" :aria-label="ratingLabel"/);
  assert.match(source, /fa-solid fa-star/);
  assert.match(source, /\{\{ ratingText \}\}/);
  assert.match(source, /class="product-sold"/);
  assert.match(source, /fa-solid fa-fire/);
  assert.match(source, /\{\{ soldCount \}\} đã bán/);
  assert.match(source, /class="product-desc"/);
  assert.match(source, /-webkit-line-clamp:2/);
  assert.match(source, /class="best-badge"/);
  assert.match(source, /class="new-badge">Mới/);
  assert.match(source, /class="hot-badge">-\{\{/);
  assert.match(source, /\.add-btn\{[^}]*width:44px[^}]*height:44px[^}]*border-radius:50%/);
  assert.match(source, /v-if="canAdd\(\)" class="add-btn"/);
  assert.match(source, /v-else-if="product\.cardDataComplete === false \|\| \(product\.inStock && product\.isAvailableNow !== false\)" class="option-btn"/);
  assert.match(source, /@media\(prefers-reduced-motion:reduce\)/);
  assert.match(source, /const added = ref\(false\)/);
  assert.match(source, /addedTimer = setTimeout\(\(\) => \{ added\.value = false; \}, 900\)/);
  assert.match(source, /fa-solid fa-check/);
  assert.match(source, /FastGuy/);
  assert.match(source, /Ảnh món đang được cập nhật/);
  assert.match(source, /await cart\.addItem\(props\.product\.productId, variantId\)/);
  assert.doesNotMatch(source, /class="rating-badge"/);
  assert.doesNotMatch(source, emojiPattern);
});

test('product detail supplements controller behavior with initial and nonblocking error states', async () => {
  const source = await read('../src/views/guest/ProductDetailPage.vue');
  assert.match(source, /createReviewPageController/);
  assert.match(source, /reviewApi\.getByProduct\(productId, params\)/);
  assert.match(source, /loadReviews\(id, \{ reset: true \}\)/);
  assert.match(source, /reviewController\.stop\(\)/);
  assert.match(source, /reviewInitialError && !reviewData/);
  assert.match(source, /reviewRefreshError/);
  assert.match(source, /class="review-error-banner" role="status" aria-live="polite"/);
  assert.match(source, /reviewController\.goToPage\(page, route\.params\.id\)/);
  assert.match(source, /reviewController\.retry\(\)/);
  assert.match(source, /Đang tải đánh giá/);
  assert.match(source, /Chưa có đánh giá/);
  assert.match(source, /reviewPage === 1/);
  assert.match(source, /reviewPage \* reviewSize >= reviewData\.total/);
});

test('product detail presents premium product facts options actions and delivery without changing data flow', async () => {
  const source = await read('../src/views/guest/ProductDetailPage.vue');
  assert.match(source, /class="product-meta"/);
  assert.match(source, /class="detail-rating"/);
  assert.match(source, /@click\.prevent="scrollToReviews"/);
  assert.match(source, /reviewsTitle\.value\?\.focus\(\{ preventScroll: true \}\)/);
  assert.match(source, /reviewAverage\.toFixed\(1\)/);
  assert.match(source, /reviewCount/);
  assert.match(source, />Kích cỡ</);
  assert.match(source, /class="variant-option"/);
  assert.match(source, /:aria-pressed="selectedVariant\?\.variantId === variant\.variantId"/);
  assert.match(source, /class="purchase-actions"/);
  assert.match(source, /fa-solid fa-cart-shopping/);
  assert.match(source, /placeInCart\('\/cart'\)/);
  assert.match(source, /placeInCart\('\/checkout'\)/);
  assert.match(source, /class="delivery-grid"/);
  assert.match(source, /Dự kiến \{\{ estimatedDeliveryMinutes \}\} phút/);
  assert.match(source, /Phí giao hàng theo địa chỉ/);
  assert.match(source, /@media \(prefers-reduced-motion: reduce\)/);
});

test('product detail keeps review state semantics and derives related cards from hydrated catalog only', async () => {
  const source = await read('../src/views/guest/ProductDetailPage.vue');
  assert.match(source, /import ProductCard from '@\/components\/common\/ProductCard\.vue'/);
  assert.match(source, /const relatedProducts = computed/);
  assert.match(source, /candidate\.productId !== product\.value\?\.productId/);
  assert.match(source, /candidate\.categoryId === product\.value\?\.categoryId/);
  assert.match(source, /\.slice\(0, 4\)/);
  assert.match(source, /class="related-products"/);
  assert.match(source, /<ProductCard v-for="related in relatedProducts"/);
  assert.match(source, /reviewInitialError && !reviewData/);
  assert.match(source, /class="review-error-banner" role="status" aria-live="polite"/);
  assert.match(source, /v-for="rating in \[5, 4, 3, 2, 1\]"/);
  assert.match(source, /role="progressbar"/);
  assert.match(source, /class="review-pagination" aria-label="Phân trang đánh giá"/);
});

test('product detail renders only public review fields, five-to-one progress semantics, and accessible controls', async () => {
  const source = await read('../src/views/guest/ProductDetailPage.vue');
  assert.match(source, /v-for="rating in \[5, 4, 3, 2, 1\]"/);
  assert.match(source, /role="progressbar"/);
  assert.match(source, /:aria-valuenow="reviewData\.ratingDistribution\[rating\]"/);
  assert.match(source, /review\.userName/);
  assert.match(source, /review\.createdAt/);
  assert.match(source, /review\.comment/);
  assert.match(source, /review\.rating/);
  assert.match(source, /:aria-label="`\$\{review\.rating\} trên 5 sao`"/);
  assert.doesNotMatch(source, /review\.(avatar|orderId|homepageConsent|featured)/);
  assert.match(source, /<button[^>]*:disabled="reviewPage === 1"/);
  assert.match(source, /<button[^>]*:disabled="reviewPage \* reviewSize >= reviewData\.total"/);
  assert.match(source, /@media \(max-width: 480px\)[\s\S]*\.review-summary/);
});
