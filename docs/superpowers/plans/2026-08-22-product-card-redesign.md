# Product Card Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign every customer-facing product card as the approved vertical card while preserving real product data, correct add/navigation behavior, accessibility, and responsive grids.

**Architecture:** Keep `mapProduct()` as the UI data boundary, `canDirectAddProduct()` as the single action-policy function, and `ProductCard.vue` as presentation. Consumer pages only own grid layout. No API, database, backend, dependency, or unrelated UI changes.

**Tech Stack:** Vue 3 `<script setup>`, Vue Router, Pinia, scoped CSS, Node test runner, Playwright.

## Global Constraints

- Vertical rounded white card; image is the dominant region.
- Render only data-backed tags: `Best seller`/`Bán chạy`, `Mới`, and discount percentage; never invent promotional tags.
- Product name visible; description clamped to at most two lines.
- Reviewed copy: `★ 4.5/5 · 18 đánh giá`; zero-review copy: `Chưa có đánh giá`.
- Always render sold copy, including `Đã bán 0`.
- Render current price; render a higher old price struck through when supplied by a valid discount/original-price pair.
- Add control is circular and exactly `44px` by `44px`.
- Direct add is allowed only for a simple product with one immediately selectable available variant and no modifier choice; every other purchasable product navigates to its detail page.
- Desktop uses 4 columns, tablet uses 2–3 columns, mobile uses exactly 2 columns with no horizontal overflow.
- Preserve keyboard focus, accessible names/status announcements, 44px targets, and `prefers-reduced-motion` behavior.
- TDD order is RED, then GREEN. No API/DB/backend work.

## File Map

- Modify `frontend/src/utils/productMapper.js`: preserve and normalize card presentation fields already present in product payloads.
- Modify `frontend/src/utils/productCard.js`: enforce direct-add versus detail-navigation policy.
- Modify `frontend/src/components/common/ProductCard.vue`: approved card semantics, interaction, and scoped responsive styling.
- Modify `frontend/src/views/guest/MenuPage.vue`: four/three-or-two/two-column catalog grid without overflow; retain list mode.
- Modify `frontend/src/components/guest/FeaturedProducts.vue`: same responsive card-grid contract.
- Modify `frontend/src/views/user/FavoritesPage.vue`: replace generic auto-fill grid with explicit responsive card grid.
- Modify `frontend/test/product-card-eligibility.test.js`: action-policy unit coverage.
- Modify `frontend/tests/product-review-ui.test.mjs`: mapper and ProductCard presentation policy coverage.
- Modify `frontend/tests/e2e/product-reviews.spec.js`: real-browser card copy, controls, navigation/add behavior, breakpoints, overflow, accessibility, and reduced-motion verification.

---

### Task 1: Behavior and Data Policy Tests

**Files:**
- Modify: `frontend/test/product-card-eligibility.test.js:5-25`
- Modify: `frontend/tests/product-review-ui.test.mjs:134-156`
- Modify: `frontend/src/utils/productMapper.js:20-52`
- Modify: `frontend/src/utils/productCard.js:1-11`

**Interfaces:**
- Consumes: raw product objects already returned to frontend stores.
- Produces: `mapProduct(product)` with normalized `averageRating`, `reviewCount`, `soldCount`, `bestSeller`, `isNew`, `discountPercent`, `originalPrice`, `productType`, `variants`, `defaultVariant`, and `modifierGroups`; `canDirectAddProduct(product): boolean`.

- [ ] **Step 1: RED — replace the permissive eligibility expectation with explicit simple-product policy tests.**

```js
const product = (overrides = {}) => ({
  productId: 1,
  productType: 'SIMPLE',
  inStock: true,
  isAvailableNow: true,
  defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: null },
  variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: null }],
  modifierGroups: [],
  ...overrides,
});

test('direct add allows only a simple product with one selectable available variant and no modifiers', () => {
  assert.equal(canDirectAddProduct(product()), true);
  assert.equal(canDirectAddProduct(product({ productType: 'COMBO' })), false);
  assert.equal(canDirectAddProduct(product({ variants: [product().defaultVariant, { variantId: 11, status: 'AVAILABLE', quantityAvailable: 5 }] })), false);
  assert.equal(canDirectAddProduct(product({ modifierGroups: [{ groupId: 1, minSelections: 0, options: [] }] })), false);
});

test('direct add rejects unavailable, out-of-stock, or outside-hours products', () => {
  assert.equal(canDirectAddProduct(product({ inStock: false })), false);
  assert.equal(canDirectAddProduct(product({ isAvailableNow: false })), false);
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'UNAVAILABLE', quantityAvailable: 5 } })), false);
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: 0 } })), false);
});
```

- [ ] **Step 2: RED — extend mapper tests with the exact card fields and sanitization boundaries.**

```js
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
```

- [ ] **Step 3: Run focused tests and confirm RED is caused by current multiple-variant eligibility plus dropped mapper fields.**

```powershell
npm test -- test/product-card-eligibility.test.js tests/product-review-ui.test.mjs
```

Expected: FAIL on multiple variants/modifiers and missing `isNew`, `discountPercent`, or `originalPrice`; existing unrelated assertions remain green.

- [ ] **Step 4: GREEN — minimally normalize the fields in `mapProduct()`.**

```js
const discountPercent = Number(product.discountPercent);
const originalPrice = parsePrice(product.originalPrice);
```

Add these returned properties beside existing merchandising fields:

```js
soldCount: Math.max(0, Math.floor(Number(product.soldCount ?? product.totalSold) || 0)),
bestSeller: Boolean(product.bestSeller ?? product.isBestSeller),
isNew: Boolean(product.isNew),
discountPercent: Number.isFinite(discountPercent) && discountPercent > 0 ? Math.min(100, Math.round(discountPercent)) : null,
originalPrice: Number(originalPrice) > 0 ? Number(originalPrice) : null,
```

Do not derive `bestSeller` or `isNew` from sold count, dates, or array position. Do not change the request/response contract.

- [ ] **Step 5: GREEN — tighten `canDirectAddProduct()` without adding another helper.**

```js
export function canDirectAddProduct(product) {
  const variant = product?.defaultVariant;
  const stock = variant?.quantityAvailable;
  return Boolean(product?.productType === 'SIMPLE'
    && product?.inStock
    && product?.isAvailableNow !== false
    && Array.isArray(product?.variants)
    && product.variants.length === 1
    && Array.isArray(product?.modifierGroups)
    && product.modifierGroups.length === 0
    && variant?.variantId
    && variant.status === 'AVAILABLE'
    && (stock == null || Number(stock) > 0));
}
```

- [ ] **Step 6: Run focused tests; require all PASS.**

```powershell
npm test -- test/product-card-eligibility.test.js tests/product-review-ui.test.mjs
```

Expected: PASS.

### Task 2: ProductCard Template and CSS Redesign

**Files:**
- Modify: `frontend/tests/product-review-ui.test.mjs:143-156`
- Modify: `frontend/src/components/common/ProductCard.vue:18-80`

**Interfaces:**
- Consumes: Task 1 normalized product fields and `canDirectAddProduct(product)`.
- Produces: one semantic vertical `<article>` with `.product-image`, `.product-tags`, `.product-info`, `.product-rating`, `.product-sold`, `.product-price`, `.add-btn`, and `.option-btn` selectors consumed by tests/E2E.

- [ ] **Step 1: RED — replace the old image-corner rating source assertions with approved-card assertions.**

```js
test('product card renders approved content, actions, and motion policy', async () => {
  const source = await read('../src/components/common/ProductCard.vue');
  assert.match(source, /reviewCount\.value > 0 \? `★ \$\{averageRating\.value\.toFixed\(1\)\}\/5 · \$\{reviewCount\.value\} đánh giá` : 'Chưa có đánh giá'/);
  assert.match(source, /Đã bán \{\{ soldCount \}\}/);
  assert.match(source, /class="product-desc"/);
  assert.match(source, /-webkit-line-clamp:2/);
  assert.match(source, /class="best-badge">Bán chạy/);
  assert.match(source, /class="new-badge">Mới/);
  assert.match(source, /class="hot-badge">-\{\{/);
  assert.match(source, /\.add-btn\{[^}]*width:44px[^}]*height:44px[^}]*border-radius:50%/);
  assert.match(source, /v-if="canAdd\(\)" class="add-btn"/);
  assert.match(source, /v-else-if="product\.inStock && product\.isAvailableNow !== false" class="option-btn"/);
  assert.match(source, /@media\(prefers-reduced-motion:reduce\)/);
  assert.doesNotMatch(source, /class="rating-badge"/);
});
```

- [ ] **Step 2: Run the focused source policy test and confirm RED.**

```powershell
npm test -- tests/product-review-ui.test.mjs
```

Expected: FAIL because rating is still an image overlay, sold zero is hidden, and homepage add is pill-shaped.

- [ ] **Step 3: GREEN — simplify computed presentation values.**

Use one rating string and safe sold count:

```js
const ratingText = computed(() => reviewCount.value > 0
  ? `★ ${averageRating.value.toFixed(1)}/5 · ${reviewCount.value} đánh giá`
  : 'Chưa có đánh giá');
const soldCount = computed(() => Math.max(0, Math.floor(Number(props.product.soldCount) || 0)));
```

Keep the existing detailed `ratingLabel`, price validation, stock guard, pending guard, toast cleanup, favorite login redirect, and `formatPrice()`.

- [ ] **Step 4: GREEN — restructure only the card template.**

Required order inside the article:

```vue
<article class="product-card" :class="{ 'list-mode': listMode, 'homepage-card': homepage }">
  <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
    <div class="product-image">
      <img v-if="product.image && !imageFailed" :src="product.image" :alt="product.name" loading="lazy" decoding="async" @error="imageFailed = true">
      <div v-else class="image-fallback" role="img" :aria-label="`Chưa có ảnh ${product.name}`"><i class="bi bi-image" aria-hidden="true"></i></div>
      <div class="product-tags">
        <span v-if="product.bestSeller" class="best-badge">Bán chạy</span>
        <span v-if="product.isNew" class="new-badge">Mới</span>
        <span v-if="hasDiscount" class="hot-badge">-{{ Math.round(product.discountPercent ?? (1 - discountPrice / product.price) * 100) }}%</span>
      </div>
      <div v-if="!product.inStock || product.isAvailableNow === false" class="stock-badge">{{ product.isAvailableNow === false ? 'Ngoài giờ bán' : 'Hết hàng' }}</div>
    </div>
    <div class="product-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <p v-if="product.description" class="product-desc">{{ product.description }}</p>
      <p class="product-rating" :aria-label="ratingLabel">{{ ratingText }}</p>
      <p class="product-sold">Đã bán {{ soldCount }}</p>
    </div>
  </router-link>
  <button class="fav-btn" :class="{ active: favoriteStore.isFavorite(product.productId) }" :aria-label="favoriteStore.isFavorite(product.productId) ? `Bỏ yêu thích ${product.name}` : `Yêu thích ${product.name}`" @click="toggleFavorite"><i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'" aria-hidden="true"></i></button>
  <div class="product-footer">
    <div class="product-price"><span class="price-now">{{ formatPrice(currentPrice) }}</span><span v-if="crossedPrice" class="price-old">{{ formatPrice(crossedPrice) }}</span></div>
    <button v-if="canAdd()" class="add-btn" :disabled="pending" :aria-label="pending ? `Đang thêm ${product.name}` : `Thêm ${product.name} vào giỏ`" @click="addToCart"><span v-if="pending" class="mini-spinner"></span><i v-else class="bi bi-plus" aria-hidden="true"></i></button>
    <router-link v-else-if="product.inStock && product.isAvailableNow !== false" class="option-btn" :to="`/product/${product.productId}`" :aria-label="`Chọn món ${product.name}`"><span>Chọn món</span><i class="bi bi-chevron-right" aria-hidden="true"></i></router-link>
  </div>
  <div v-if="message" class="toast" role="status" aria-live="polite">{{ message }}</div>
</article>
```

Remove category, spice, generic option tags, image-corner rating, and homepage-specific add-label/pill behavior because they are outside the approved card.

- [ ] **Step 5: GREEN — consolidate the duplicate scoped style blocks into one minimal card stylesheet.**

Implement these measurable rules:

```css
.product-card{position:relative;display:flex;min-width:0;overflow:hidden;flex-direction:column;border:1px solid var(--border-light);border-radius:20px;background:#fff;transition:box-shadow var(--transition-normal),transform var(--transition-normal)}
.product-main{display:flex;min-width:0;flex:1;flex-direction:column;color:inherit}
.product-image{position:relative;overflow:hidden;aspect-ratio:4/3;background:var(--surface)}
.product-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s var(--ease-out)}
.product-tags{position:absolute;top:10px;left:10px;display:flex;max-width:calc(100% - 64px);flex-wrap:wrap;gap:5px}
.product-tags span{padding:5px 8px;border-radius:999px;color:#fff;font-size:10px;font-weight:800}
.product-info{display:flex;min-width:0;flex:1;flex-direction:column;padding:14px 14px 8px}
.product-name{display:-webkit-box;overflow:hidden;font-size:16px;line-height:1.35;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.product-desc{display:-webkit-box;overflow:hidden;margin-top:6px;color:var(--text-mid);font-size:12px;line-height:1.45;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.product-rating,.product-sold{margin-top:8px;color:var(--text-mid);font-size:11px}
.product-footer{display:flex;min-width:0;align-items:center;justify-content:space-between;gap:8px;padding:10px 14px 14px}
.product-price{display:flex;min-width:0;flex-wrap:wrap;align-items:baseline;gap:5px}
.price-now{color:var(--primary);font-size:17px;font-weight:850}
.price-old{color:var(--text-light);font-size:12px;text-decoration:line-through}
.add-btn{display:grid;width:44px;height:44px;min-width:44px;min-height:44px;place-items:center;border:0;border-radius:50%;color:#fff;background:var(--primary)}
```

Retain explicit `:focus-visible`, disabled/pending, stock overlay, favorite 44px circle, toast/live region, list-mode layout, and image fallback styling. At `max-width:560px`, reduce padding/type/gaps only; keep description at two lines, rating/sold visible, and add at 44px. Under `prefers-reduced-motion: reduce`, remove card/image transitions, hover transform, and spinner animation.

- [ ] **Step 6: Run focused tests; require PASS.**

```powershell
npm test -- test/product-card-eligibility.test.js tests/product-review-ui.test.mjs
```

Expected: PASS.

### Task 3: Responsive Consumers and E2E Verification

**Files:**
- Modify: `frontend/src/views/guest/MenuPage.vue:95,102-129`
- Modify: `frontend/src/components/guest/FeaturedProducts.vue:25-35`
- Modify: `frontend/src/views/user/FavoritesPage.vue:40-60`
- Modify: `frontend/tests/e2e/product-reviews.spec.js:3-50`

**Interfaces:**
- Consumes: Task 2 card selectors and accessible names.
- Produces: `.grid`/`.favorites-grid` layouts with 4 desktop, 3 or 2 tablet, and 2 mobile columns; Playwright proof on `desktop-chrome` and `mobile-chrome`.

- [ ] **Step 1: RED — extend product fixtures to cover all visible policies and both actions.**

Add to `ratedProduct`: `soldCount: 18`, `bestSeller: true`, `isNew: true`, `discountPercent: 20`, `originalPrice: 75000`, `discountPrice: 59000`, and `price: 75000`. Keep `emptyProduct` with `soldCount: 0`. Add an option product with two variants so `canDirectAddProduct()` rejects it.

- [ ] **Step 2: RED — update the card E2E test with exact copy, action, columns, overflow, and motion assertions.**

```js
test('product cards render approved responsive content and action policy', async ({ page }, testInfo) => {
  const observed = await installFixtures(page);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/menu');

  await expect(page.getByLabel('Đánh giá 4.5 trên 5 từ 18 lượt')).toHaveText('★ 4.5/5 · 18 đánh giá');
  await expect(page.getByLabel('Chưa có đánh giá, 0 lượt')).toHaveText('Chưa có đánh giá');
  await expect(page.getByText('Đã bán 0', { exact: true })).toBeVisible();
  await expect(page.getByText('Bán chạy', { exact: true })).toBeVisible();
  await expect(page.getByText('Mới', { exact: true })).toBeVisible();
  await expect(page.getByText('-20%', { exact: true })).toBeVisible();
  await expect(page.locator('.price-old').first()).toHaveCSS('text-decoration-line', 'line-through');
  await expect(page.getByRole('button', { name: /Thêm .* vào giỏ/ }).first()).toHaveCSS('width', '44px');
  await expect(page.getByRole('button', { name: /Thêm .* vào giỏ/ }).first()).toHaveCSS('height', '44px');
  await expect(page.getByRole('link', { name: /Chọn món/ })).toHaveAttribute('href', /\/product\/\d+/);

  const expectedColumns = testInfo.project.name === 'mobile-chrome' ? 2 : 4;
  await expect.poll(() => page.locator('.content > .grid').evaluate(grid => getComputedStyle(grid).gridTemplateColumns.split(' ').length)).toBe(expectedColumns);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Product card grid tràn ngang');
  });
  await expect(page.locator('.product-card').first()).toHaveCSS('transition-duration', '0s');
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});
```

Use fixture rating `4.5` and review count `18` so expected copy is exact. Route the cart POST in the fixture, click the simple product add button, and assert one successful `/api/cart/items` request. Click the option-product `Chọn món` link and assert navigation to its `/product/:id`; do not expect a cart request for that product.

- [ ] **Step 3: Run the E2E test and confirm RED before layout changes.**

```powershell
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome
```

Expected: FAIL on old copy/action policy, desktop three-column menu, or card motion/layout assertions. The mocked routes keep card data deterministic; `PLAYWRIGHT_API_TARGET` only satisfies Vite proxy configuration.

- [ ] **Step 4: GREEN — set explicit consumer grids without changing data flow.**

For `MenuPage.vue` and `FeaturedProducts.vue`:

```css
.grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:18px}
@media(max-width:1100px){.grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:800px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:560px){.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}}
```

Keep `MenuPage.vue` list mode as one column. Remove later duplicate `.grid` breakpoint rules that override this contract. Preserve skeleton, filter, loading, empty, and error behavior.

For `FavoritesPage.vue`, rename `grid-4` to local `favorites-grid` and add:

```css
.favorites-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--space-5)}
@media(max-width:1100px){.favorites-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:800px){.favorites-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:560px){.favorites-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}}
@media(prefers-reduced-motion:reduce){.spin{animation:none}}
```

Every grid child/card must retain `min-width:0`; no horizontal scroller or one-column mobile fallback.

- [ ] **Step 5: Run focused unit and dual-viewport E2E checks; require PASS and no console/page errors.**

```powershell
npm test -- test/product-card-eligibility.test.js tests/product-review-ui.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome
```

Expected: PASS.

- [ ] **Step 6: Run full frontend verification.**

```powershell
npm test
npm run build
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/home.spec.js tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome
```

Expected: all tests PASS, Vite build exits `0`, both browser projects report no horizontal overflow, console errors, page errors, or failed primary mocked requests.
