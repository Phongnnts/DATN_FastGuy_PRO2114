# Premium Product UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nâng cấp `ProductCard` và trang chi tiết sản phẩm thành giao diện premium, nhất quán, responsive, truy cập được, đồng thời giữ nguyên toàn bộ contract và hành vi dữ liệu hiện hữu.

**Architecture:** Chỉ thay đổi lớp trình bày Vue và test trực tiếp liên quan. `ProductCard.vue` tiếp tục nhận product đã hydrate qua prop; `ProductDetailPage.vue` tiếp tục dùng store/controller/API hiện hữu, chỉ thêm các computed trình bày từ dữ liệu đã có. Font Awesome Free được nạp một lần qua CDN trong `index.html`; Bootstrap Icons vẫn được nạp và mọi nơi ngoài hai component mục tiêu giữ nguyên.

**Tech Stack:** Vue 3 `<script setup>`, Vue Router, Pinia, scoped CSS, Font Awesome Free 6.7.2 CDN, Bootstrap Icons 1.11.3, Node test runner, Playwright, Vite.

## Global Constraints

- Frontend-only. Không thay đổi backend, API, DB, OpenAPI, store, mapper, utility nghiệp vụ hoặc business logic.
- Chỉ sửa `frontend/index.html`, `frontend/src/components/common/ProductCard.vue`, `frontend/src/views/guest/ProductDetailPage.vue`, `frontend/tests/product-review-ui.test.mjs`, `frontend/tests/ui-scope-hide-policy.test.mjs`, `frontend/tests/e2e/product-reviews.spec.js`, và `frontend/tests/e2e/home.spec.js` khi assertion trực tiếp liên quan cần cập nhật.
- Bảo toàn toàn bộ thay đổi chưa commit, đặc biệt data/hydration trong `ProductCard.vue`, favorite hydration, mapper, store, menu, homepage và favorites.
- Không hoàn nguyên, ghi đè hoặc format lại file ngoài phần dòng cần thay đổi.
- Không thêm dependency npm. Font Awesome Free dùng đúng CDN đã duyệt: `https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.7.2/css/all.min.css`.
- Giữ nguyên CDN Bootstrap Icons `https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css`; không đổi icon Bootstrap tại component khác.
- Trong `ProductCard.vue` và `ProductDetailPage.vue`: không có emoji Unicode; icon mới dùng đúng class Font Awesome nêu trong từng task; icon trang trí có `aria-hidden="true"`.
- Không đổi signature, payload, route, store call, cart/favorite behavior, review controller, delivery controller, modifier validation, stock validation hoặc hydration policy.
- Không tạo dữ liệu quảng cáo giả. Badge, rating, sold count, giá cũ, delivery estimate, variant, modifier, review và related product chỉ hiển thị từ dữ liệu hiện có.
- Tất cả target tương tác tối thiểu `44px`; có `:focus-visible`; không phụ thuộc màu duy nhất để truyền đạt trạng thái; hỗ trợ `prefers-reduced-motion: reduce`.
- Mobile không tràn ngang; nội dung giữ được ở 320px CSS viewport; desktop kiểm tra ở Chromium desktop; mobile kiểm tra bằng Pixel 7 project.
- TDD bắt buộc theo thứ tự RED rồi GREEN. Mỗi RED phải thất bại đúng assertion mới; mỗi GREEN phải chạy lại focused test.
- Không commit. Kế hoạch không yêu cầu stage, commit hoặc push.

## File Map

- Modify `frontend/index.html`: nạp Font Awesome Free qua CDN; giữ Bootstrap Icons.
- Modify `frontend/src/components/common/ProductCard.vue`: icon Font Awesome, semantic card, premium visual treatment, responsive/focus/reduced-motion rules; không đổi data/action flow.
- Modify `frontend/src/views/guest/ProductDetailPage.vue`: premium information hierarchy, option controls, CTA, delivery cards, reviews, related products; không đổi API/store/controller behavior.
- Modify `frontend/tests/product-review-ui.test.mjs`: source-contract guards cho CDN, icon classes, không emoji, card/detail structure, review và related section.
- Modify `frontend/tests/ui-scope-hide-policy.test.mjs`: giữ các scope guard hiện tại; bổ sung guard không đưa combo hoặc field ngoài contract trở lại related UI.
- Modify `frontend/tests/e2e/product-reviews.spec.js`: fixture-driven assertions cho card, detail, options, CTA, delivery, reviews, related cards, responsive, focus, request và console errors.
- Modify `frontend/tests/e2e/home.spec.js`: chỉ cập nhật exact card copy/icon assertion bị ảnh hưởng và giữ kiểm tra homepage/detail thực tế.

---

### Task 1: Font Awesome Free and Regression Guards

**Files:**
- Modify: `frontend/tests/product-review-ui.test.mjs:7,173-220`
- Modify: `frontend/index.html:15-19`
- Modify: `frontend/src/components/common/ProductCard.vue:50-76`
- Modify: `frontend/src/views/guest/ProductDetailPage.vue:152-316`

**Interfaces:**
- Consumes: HTML stylesheet loading; existing inline `<i>` presentation points.
- Produces: one global Font Awesome Free stylesheet; exact `fa-solid`/`fa-regular` classes available to both target components; source guards preventing emoji and accidental Bootstrap-icon migration outside scope.

- [ ] **Step 1: RED — add exact CDN and icon-policy source tests.**

Add these source constants after the existing `read` helper in `frontend/tests/product-review-ui.test.mjs`:

```js
const indexHtml = () => read('../index.html');
const emojiPattern = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u;
```

Add this test before the ProductCard presentation test:

```js
test('premium product UI loads approved Font Awesome Free and keeps Bootstrap Icons available', async () => {
  const [html, card, detail] = await Promise.all([
    indexHtml(),
    read('../src/components/common/ProductCard.vue'),
    read('../src/views/guest/ProductDetailPage.vue'),
  ]);
  assert.match(html, /href="https:\/\/cdn\.jsdelivr\.net\/npm\/@fortawesome\/fontawesome-free@6\.7\.2\/css\/all\.min\.css"/);
  assert.match(html, /href="https:\/\/cdn\.jsdelivr\.net\/npm\/bootstrap-icons@1\.11\.3\/font\/bootstrap-icons\.min\.css"/);
  assert.doesNotMatch(card, emojiPattern);
  assert.doesNotMatch(detail, emojiPattern);
  assert.doesNotMatch(card, /class="bi bi-/);
  assert.doesNotMatch(detail, /class="bi bi-/);
  for (const token of [
    'fa-solid fa-star',
    'fa-solid fa-fire',
    'fa-regular fa-heart',
    'fa-solid fa-heart',
    'fa-solid fa-plus',
  ]) assert.match(card, new RegExp(token));
  for (const token of [
    'fa-solid fa-bolt',
    'fa-regular fa-heart',
    'fa-solid fa-heart',
    'fa-solid fa-circle-check',
    'fa-solid fa-circle-xmark',
    'fa-solid fa-cart-shopping',
    'fa-solid fa-truck-fast',
    'fa-solid fa-star',
  ]) assert.match(detail, new RegExp(token));
});
```

- [ ] **Step 2: Run the focused test and verify RED.**

Run from `frontend`:

```powershell
npm test -- tests/product-review-ui.test.mjs
```

Expected: FAIL because Font Awesome CDN and exact `fa-*` classes are absent; current emoji in `ProductCard.vue` also violates the new guard.

- [ ] **Step 3: GREEN — add the approved Font Awesome stylesheet without changing existing links.**

Insert directly after the Google Fonts stylesheet and before Bootstrap Icons in `frontend/index.html`:

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.7.2/css/all.min.css" />
```

Do not remove or reorder the existing Bootstrap Icons link relative to app startup.

- [ ] **Step 4: GREEN — replace icons only inside the two target components using the exact map.**

Use these exact replacements in `ProductCard.vue`:

```text
image fallback       fa-solid fa-image
best seller          fa-solid fa-fire
rating               fa-solid fa-star
sold count           fa-solid fa-fire
favorite inactive    fa-regular fa-heart
favorite active      fa-solid fa-heart
add                   fa-solid fa-plus
choose/detail         fa-solid fa-chevron-right
```

The favorite binding must be exactly:

```vue
<i :class="favoriteStore.isFavorite(product.productId) ? 'fa-solid fa-heart' : 'fa-regular fa-heart'" aria-hidden="true"></i>
```

Use these exact replacements in `ProductDetailPage.vue`:

```text
loading/review loading   fa-solid fa-rotate
load error               fa-solid fa-circle-exclamation
breadcrumb chevron       fa-solid fa-chevron-right
hot label                fa-solid fa-bolt
favorite inactive        fa-regular fa-heart
favorite active          fa-solid fa-heart
availability yes         fa-solid fa-circle-check
availability no          fa-solid fa-circle-xmark
quantity minus           fa-solid fa-minus
quantity plus            fa-solid fa-plus
add cart                 fa-solid fa-cart-shopping
buy now                  fa-solid fa-arrow-right
estimated delivery       fa-solid fa-clock
shipping                 fa-solid fa-truck-fast
review star              fa-solid fa-star
empty reviews            fa-regular fa-comment-dots
pagination previous      fa-solid fa-chevron-left
pagination next          fa-solid fa-chevron-right
not found                fa-solid fa-box-open
```

Every decorative icon receives `aria-hidden="true"`. Keep all existing button/link accessible names and visible Vietnamese labels.

- [ ] **Step 5: Run the focused test; require PASS.**

```powershell
npm test -- tests/product-review-ui.test.mjs
```

Expected: PASS, including existing controller/mapper/review assertions.

### Task 2: ProductCard Premium Polish

**Files:**
- Modify: `frontend/tests/product-review-ui.test.mjs:173-187`
- Modify: `frontend/src/components/common/ProductCard.vue:18-116`
- Modify: `frontend/tests/e2e/product-reviews.spec.js:67-101,170-196`
- Modify only if exact text changes: `frontend/tests/e2e/home.spec.js:34-44`

**Interfaces:**
- Consumes: hydrated `product` prop; existing `canDirectAddProduct(product)`, cart store, auth store, favorite store and router.
- Produces: `.product-card`, `.product-main`, `.product-image`, `.product-tags`, `.product-info`, `.product-rating`, `.product-sold`, `.product-footer`, `.add-btn`, `.option-btn`; accessible actions consumed by existing pages and E2E.

- [ ] **Step 1: RED — replace stale card source assertions with the exact premium contract.**

Replace the current `product card renders approved content, actions, and motion policy` test body with:

```js
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
  assert.doesNotMatch(source, /class="rating-badge"/);
  assert.doesNotMatch(source, emojiPattern);
});
```

Preserve the `cardDataComplete === false` fallback assertion because it protects the current uncommitted hydration behavior.

- [ ] **Step 2: RED — update deterministic browser assertions to premium copy and circular add control.**

In the first ProductCard E2E test, assert:

```js
await expect(page.getByLabel('Đánh giá 4.5 trên 5 từ 18 lượt')).toHaveText('4.5 · 18 đánh giá');
await expect(page.getByLabel('Chưa có đánh giá, 0 lượt').first()).toHaveText('Chưa có đánh giá');
await expect(page.getByText('0 đã bán', { exact: true }).first()).toBeVisible();
const ratedCard = page.getByRole('article').filter({ hasText: 'Burger đánh giá' });
await expect(ratedCard.locator('.best-badge .fa-fire')).toBeVisible();
await expect(ratedCard.locator('.product-rating .fa-star')).toBeVisible();
const add = page.getByRole('button', { name: /Thêm .* vào giỏ/ }).first();
await expect(add).toHaveCSS('width', '44px');
await expect(add).toHaveCSS('height', '44px');
await expect(add.locator('.fa-plus')).toBeVisible();
await expect(page.getByRole('link', { name: /Chọn món/ }).locator('.fa-chevron-right')).toBeVisible();
```

Update the favorites hydration test from emoji-bearing text locators to `.product-rating`, `.product-sold`, `.best-badge`, `.new-badge`, and `.hot-badge`. Keep exact cart request count, catalog request count, card order and hydrated action policy assertions unchanged.

- [ ] **Step 3: Run focused source and E2E tests; verify RED only on new presentation assertions.**

```powershell
npm test -- tests/product-review-ui.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product cards|favorites"
```

Expected: source test or browser assertions FAIL on current pill add control, current text/icon placement, or old card styling. Existing cart/navigation/hydration request assertions remain valid.

- [ ] **Step 4: GREEN — refine only ProductCard presentation.**

Keep every computed, `addToCart()`, `toggleFavorite()`, `notify()`, timer cleanup and prop unchanged. Use this semantic order:

```vue
<article class="product-card" :class="{ 'list-mode': listMode, 'homepage-card': homepage }">
  <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
    <div class="product-image">
      <!-- existing image/fallback and truthful stock overlay -->
      <div class="product-tags">
        <!-- truthful bestSeller/isNew/discount badges only -->
      </div>
    </div>
    <div class="product-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <p class="product-desc">{{ product.description || '\u00a0' }}</p>
      <p class="product-rating" :aria-label="ratingLabel"><i class="fa-solid fa-star" aria-hidden="true"></i>{{ ratingText }}</p>
      <p class="product-sold"><i class="fa-solid fa-fire" aria-hidden="true"></i>{{ soldCount }} đã bán</p>
    </div>
  </router-link>
  <!-- existing favorite action -->
  <div class="product-footer">
    <!-- existing validated current/old prices -->
    <!-- existing canAdd/cardDataComplete action branches -->
  </div>
  <!-- existing live-region toast -->
</article>
```

Comments above explain plan placement only; do not add comments to the Vue file.

The direct-add button remains icon-only with its current dynamic `aria-label`:

```vue
<button v-if="canAdd()" class="add-btn" :disabled="pending" :aria-label="pending ? `Đang thêm ${product.name}` : `Thêm ${product.name} vào giỏ`" @click="addToCart"><span v-if="pending" class="mini-spinner"></span><i v-else class="fa-solid fa-plus" aria-hidden="true"></i></button>
```

The option link keeps its hydration-safe condition and visible label:

```vue
<router-link v-else-if="product.cardDataComplete === false || (product.inStock && product.isAvailableNow !== false)" class="option-btn" :to="`/product/${product.productId}`" :aria-label="`Chọn món ${product.name}`"><span>Chọn món</span><i class="fa-solid fa-chevron-right" aria-hidden="true"></i></router-link>
```

- [ ] **Step 5: GREEN — apply measurable premium CSS without changing consumer grids.**

Implement these exact base measurements, then retain existing list-mode behavior:

```css
.product-card{position:relative;display:flex;min-width:0;overflow:hidden;flex-direction:column;border:1px solid rgba(89,64,48,.12);border-radius:22px;background:#fff;box-shadow:0 6px 20px rgba(55,35,23,.06);transition:box-shadow var(--transition-normal),transform var(--transition-normal)}
.product-card:hover{box-shadow:0 18px 40px rgba(55,35,23,.13);transform:translateY(-4px)}
.product-image{position:relative;overflow:hidden;height:200px;flex:0 0 200px;background:var(--surface)}
.product-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s var(--ease-out)}
.product-tags>span{display:inline-flex;align-items:center;gap:5px;padding:6px 9px;border-radius:999px;color:#fff;font-size:10px;font-weight:800;box-shadow:0 4px 12px rgba(55,35,23,.14)}
.product-info{display:flex;min-width:0;flex:1;flex-direction:column;padding:16px 16px 8px}
.product-desc{display:-webkit-box;overflow:hidden;min-height:34.8px;margin-top:6px;color:var(--text-mid);font-size:12px;line-height:1.45;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.product-rating,.product-sold{display:flex;align-items:center;gap:5px;overflow:hidden;color:var(--text-mid);font-size:11px;text-overflow:ellipsis;white-space:nowrap}
.product-rating{margin-top:10px;color:#76513f}.product-rating i{color:#f59e0b}.product-sold{margin-top:5px}
.product-footer{display:flex;min-width:0;align-items:center;justify-content:space-between;gap:8px;margin-top:auto;padding:10px 16px 16px}
.add-btn{display:grid;width:44px;height:44px;min-width:44px;min-height:44px;place-items:center;border:0;border-radius:50%;color:#fff;background:linear-gradient(135deg,var(--primary),var(--primary-dark));box-shadow:0 8px 18px rgba(212,97,58,.24)}
```

Retain explicit focus styles for `.product-main`, `.fav-btn`, `.add-btn`, `.option-btn`; pending/disabled state; stock overlay; fallback; toast; spinner; list mode. At `max-width:560px`, keep two-line description, rating, sold count and 44px controls visible. Under reduced motion, disable card/image transitions, hover transform and spinner animation.

- [ ] **Step 6: Run focused checks; require PASS.**

```powershell
npm test -- tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product cards|favorites"
```

Expected: PASS; one simple product cart POST; option product navigation without extra cart POST; no horizontal overflow; no console/page errors.

### Task 3: ProductDetail Information, Options, CTA, and Delivery

**Files:**
- Modify: `frontend/tests/product-review-ui.test.mjs:189-220`
- Modify: `frontend/src/views/guest/ProductDetailPage.vue:40-51,161-259,319-367`
- Modify: `frontend/tests/e2e/product-reviews.spec.js:3-64,214-228`

**Interfaces:**
- Consumes: existing `product`, `selectedVariant`, `selectedModifiers`, `selectedAvailable`, `selectedStock`, `effectivePrice`, `estimatedDeliveryMinutes`, `selectVariant()`, `toggleModifier()`, `placeInCart()` and favorite behavior.
- Produces: `.product-meta`, `.detail-rating`, `.variant-option`, `.availability`, `.purchase-actions`, `.delivery-grid`; same `/cart` and `/checkout` destinations and same cart payload.

- [ ] **Step 1: RED — add source guards for premium detail hierarchy while preserving behavior.**

Add this test to `frontend/tests/product-review-ui.test.mjs`:

```js
test('product detail presents premium product facts options actions and delivery without changing data flow', async () => {
  const source = await read('../src/views/guest/ProductDetailPage.vue');
  assert.match(source, /class="product-meta"/);
  assert.match(source, /class="detail-rating"/);
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
```

Keep existing source assertions for `createReviewPageController`, initial/nonblocking review errors, public review fields, pagination and mobile review layout.

- [ ] **Step 2: RED — extend the detail E2E fixture and assertions.**

Use existing `ratedProduct`; do not add fields absent from its frontend shape. In the detail E2E test, add:

```js
await expect(page.locator('.product-meta')).toContainText('Burger');
await expect(page.locator('.detail-rating')).toContainText('4.2');
await expect(page.getByRole('heading', { name: 'Burger đánh giá', level: 1 })).toBeVisible();
const size = page.getByRole('button', { name: /Tiêu chuẩn/ });
await expect(size).toHaveAttribute('aria-pressed', 'true');
await expect(page.getByText('Còn 20 phần', { exact: true })).toBeVisible();
await expect(page.getByRole('button', { name: /Thêm vào giỏ/ })).toBeEnabled();
await expect(page.getByRole('button', { name: /Mua ngay/ })).toBeEnabled();
await expect(page.getByText('Dự kiến 30 phút', { exact: true })).toBeVisible();
await expect(page.getByText('Phí giao hàng theo địa chỉ', { exact: true })).toBeVisible();
```

Add an unavailable variant to a copied fixture and route it as `/api/products/47`; assert the unavailable button is disabled and add/buy actions remain disabled when no selectable variant exists. Do not alter API payload names.

- [ ] **Step 3: Run focused tests and verify RED.**

```powershell
npm test -- tests/product-review-ui.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product detail"
```

Expected: FAIL on absent `.product-meta`, `.detail-rating`, `.purchase-actions`, `.delivery-grid` or variant `aria-pressed`; existing review requests still return `200`.

- [ ] **Step 4: GREEN — create the information hierarchy using existing values only.**

Immediately above `<h1>`, render:

```vue
<div class="product-meta">
  <span>{{ product.categoryName || 'Món ăn' }}</span>
  <span v-if="product.bestSeller"><i class="fa-solid fa-bolt" aria-hidden="true"></i>Bán chạy</span>
</div>
<h1>{{ product.name }}</h1>
<p class="detail-rating" :aria-label="reviewCount ? `Đánh giá ${reviewAverage.toFixed(1)} trên 5 từ ${reviewCount} lượt` : 'Chưa có đánh giá'">
  <i class="fa-solid fa-star" aria-hidden="true"></i>
  <strong>{{ reviewAverage.toFixed(1) }}</strong>
  <span>{{ reviewCount ? `${reviewCount} đánh giá` : 'Chưa có đánh giá' }}</span>
</p>
```

Remove the unconditional `HOT` text. `Bán chạy` appears only when `product.bestSeller` is true. Keep favorite action and description fallback unchanged.

- [ ] **Step 5: GREEN — improve option semantics without changing selection logic.**

For variant buttons, add:

```vue
:aria-pressed="selectedVariant?.variantId === variant.variantId"
```

Keep current disabled expression, `selectVariant(variant)`, stock copy and price. Keep modifier buttons as `aria-pressed`; preserve `role="group"`, `aria-describedby`, min/max text, validation errors and `toggleModifier(group, option)`.

Do not convert controls to radio/checkbox inputs because that would require changing selection behavior beyond this visual scope.

- [ ] **Step 6: GREEN — group CTA and delivery presentation while retaining destinations.**

Use:

```vue
<div class="purchase-actions">
  <div class="purchase-row">
    <!-- existing quantity control -->
    <button class="add-cart-btn" :disabled="!selectedAvailable" @click="placeInCart('/cart')"><i class="fa-solid fa-cart-shopping" aria-hidden="true"></i>Thêm vào giỏ - {{ formatPrice(effectivePrice * quantity) }}</button>
  </div>
  <button class="buy-now-btn" :disabled="!selectedAvailable" @click="placeInCart('/checkout')">Mua ngay<i class="fa-solid fa-arrow-right" aria-hidden="true"></i></button>
</div>
<div class="delivery-grid">
  <div v-if="estimatedDeliveryMinutes">
    <i class="fa-solid fa-clock" aria-hidden="true"></i>
    <span><strong>Dự kiến {{ estimatedDeliveryMinutes }} phút</strong><small>Thời gian thực tế xác nhận khi tính giao hàng</small></span>
  </div>
  <div>
    <i class="fa-solid fa-truck-fast" aria-hidden="true"></i>
    <span><strong>Phí giao hàng theo địa chỉ</strong><small>Hiển thị chính xác tại bước thanh toán</small></span>
  </div>
</div>
```

Keep quantity min/max/stock guards exactly as-is. Do not add free-shipping, guarantee, preparation-time or inventory claims.

- [ ] **Step 7: GREEN — apply responsive, focus and reduced-motion detail styles.**

Use a two-column layout above 900px and one column at/below 900px. Keep sticky gallery desktop-only. Ensure:

```css
.product-meta,.detail-rating{display:flex;align-items:center;flex-wrap:wrap}
.variant-option,.quantity-control button,.add-cart-btn,.buy-now-btn,.favorite-detail-btn{min-height:44px}
.purchase-actions{display:grid;gap:12px}
.delivery-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;margin-top:24px}
.product-purchase-panel button:focus-visible,.gallery-thumb:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
@media (max-width:480px){.purchase-row,.delivery-grid{grid-template-columns:1fr}.product-purchase-panel{padding:20px 16px}}
@media (prefers-reduced-motion: reduce){.main-image,.variant-option,.add-cart-btn,.buy-now-btn{transition:none}.main-image-wrap:hover .main-image,.add-cart-btn:hover{transform:none}.spin{animation:none}}
```

Do not remove existing review focus styles.

- [ ] **Step 8: Run focused checks; require PASS.**

```powershell
npm test -- tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product detail"
```

Expected: PASS; no changed request URL/payload; no console/page error; no overflow.

### Task 4: Premium Reviews and Related Product Cards

**Files:**
- Modify: `frontend/tests/product-review-ui.test.mjs:189-220`
- Modify: `frontend/tests/ui-scope-hide-policy.test.mjs:74-87`
- Modify: `frontend/src/views/guest/ProductDetailPage.vue:1-12,40-52,261-310,319-367`
- Modify: `frontend/tests/e2e/product-reviews.spec.js:214-228`

**Interfaces:**
- Consumes: existing `reviewData`, review loading/error/pagination controller state, `productStore.allProducts`, current `product.productId`, `product.categoryId`, and `ProductCard` prop contract.
- Produces: existing `.product-reviews` behavior with premium layout; presentation-only `relatedProducts`; `.related-products` grid rendering existing `ProductCard` instances.

- [ ] **Step 1: RED — add exact source coverage for review preservation and related-card projection.**

Add to `frontend/tests/product-review-ui.test.mjs`:

```js
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
```

Extend the existing combo scope test in `frontend/tests/ui-scope-hide-policy.test.mjs`:

```js
assert.doesNotMatch(productDetail, /product\.combo|candidate\.isCombo|Combo gồm/);
assert.doesNotMatch(productDetail, /review\.(avatar|orderId|homepageConsent|featured)/);
```

- [ ] **Step 2: RED — expand detail E2E for review visuals and related cards.**

After existing review pagination assertions, add:

```js
await expect(page.locator('.product-reviews')).toBeVisible();
await expect(page.locator('.rating-distribution [role="progressbar"]')).toHaveCount(5);
await expect(page.locator('.review-item').first()).toContainText('An');
await expect(page.locator('.review-item').first().locator('.fa-star')).toHaveCount(5);
await expect(page.getByRole('region', { name: 'Món cùng danh mục' })).toBeVisible();
const relatedCards = page.locator('.related-products .product-card');
await expect(relatedCards).toHaveCount(2);
await expect(relatedCards.filter({ hasText: 'Burger đánh giá' })).toHaveCount(0);
```

The fixture catalog already contains three category-1 products. Current product `45` is excluded, leaving products `46` and `47`; no new API route is needed.

- [ ] **Step 3: Run focused tests and verify RED.**

```powershell
npm test -- tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product detail renders review"
```

Expected: FAIL on absent related projection/section and old star rendering; all current review pagination/controller assertions remain green.

- [ ] **Step 4: GREEN — add presentation-only related product projection.**

Add the existing component import:

```js
import ProductCard from '@/components/common/ProductCard.vue';
```

Add beside other computed values:

```js
const relatedProducts = computed(() => (productStore.allProducts || [])
  .filter((candidate) => candidate.productId !== product.value?.productId && candidate.categoryId === product.value?.categoryId)
  .slice(0, 4));
```

This is a local presentation projection only. Do not fetch, remap, sort, mutate the store, infer merchandising flags or add fallback products from another category.

- [ ] **Step 5: GREEN — polish reviews while preserving every state branch.**

Keep the exact branch order: initial loading, initial error, valid data with nonblocking refresh error/loading, empty state, content, pagination. Replace Unicode stars in visible review markup with Font Awesome icons:

```vue
<p class="review-rating" :aria-label="reviewCount ? `Đánh giá trung bình ${reviewAverage.toFixed(1)} trên 5 từ ${reviewCount} lượt` : 'Chưa có đánh giá'">
  <i class="fa-solid fa-star" aria-hidden="true"></i><strong>{{ reviewAverage.toFixed(1) }}</strong><small>/5 · {{ reviewCount }} lượt</small>
</p>
```

For each review item:

```vue
<p class="review-stars" :aria-label="`${review.rating} trên 5 sao`"><i v-for="star in 5" :key="star" :class="star <= review.rating ? 'fa-solid fa-star' : 'fa-regular fa-star'" aria-hidden="true"></i></p>
```

Do not expose `avatar`, `orderId`, `homepageConsent` or `featured`. Keep `userName`, `createdAt`, `rating`, `comment` only.

- [ ] **Step 6: GREEN — render related products after reviews.**

Insert inside the same `.container`, after `.product-reviews`:

```vue
<section v-if="relatedProducts.length" class="related-section" aria-labelledby="related-title" aria-label="Món cùng danh mục">
  <div class="related-heading">
    <div><p class="review-eyebrow">Có thể bạn sẽ thích</p><h2 id="related-title">Món cùng danh mục</h2></div>
    <router-link to="/menu">Xem thực đơn<i class="fa-solid fa-arrow-right" aria-hidden="true"></i></router-link>
  </div>
  <div class="related-products">
    <ProductCard v-for="related in relatedProducts" :key="related.productId" :product="related" />
  </div>
</section>
```

Use one accessible naming mechanism in final markup: retain `aria-labelledby="related-title"`; remove redundant `aria-label` if the browser exposes duplicate naming during E2E inspection.

- [ ] **Step 7: GREEN — add responsive related/review CSS.**

```css
.review-stars{display:flex;gap:3px;color:#f59e0b}
.related-section{margin-top:48px}
.related-heading{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:20px}
.related-heading h2{margin:0;font-size:clamp(24px,3vw,34px)}
.related-heading a{display:inline-flex;min-height:44px;align-items:center;gap:8px;color:var(--primary-dark);font-weight:800}
.related-products{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:18px}
@media (max-width:1100px){.related-products{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media (max-width:800px){.related-products{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media (max-width:480px){.related-heading{align-items:flex-start;flex-direction:column}.related-products{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}}
```

Keep review distribution/list one column below 900px and pagination wrapping below 480px. Every card remains `min-width:0`; no horizontal scroller.

- [ ] **Step 8: Run focused checks; require PASS.**

```powershell
npm test -- tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome --grep="product detail renders review"
```

Expected: PASS; exactly one page-1 and one page-2 review request; two related cards in deterministic fixture; current product absent; no extra products API request introduced by related UI.

### Task 5: Full Tests, Build, Playwright Visual and Accessibility Verification

**Files:**
- Modify only when a directly related assertion is stale: `frontend/tests/e2e/product-reviews.spec.js`
- Modify only when a directly related assertion is stale: `frontend/tests/e2e/home.spec.js`
- No production files.

**Interfaces:**
- Consumes: completed Tasks 1–4.
- Produces: full frontend regression evidence, deterministic desktop/mobile visual artifacts, keyboard/accessibility evidence, clean console/request evidence and successful production build.

- [ ] **Step 1: Add a browser-level accessibility and visual verification test before final verification.**

Add to `frontend/tests/e2e/product-reviews.spec.js`:

```js
test('premium product UI is keyboard accessible and visually stable', async ({ page }, testInfo) => {
  const observed = await installFixtures(page);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/product/45');
  await expect(page.getByRole('heading', { name: 'Burger đánh giá', level: 1 })).toBeVisible();

  const interactive = page.locator('.product-purchase-panel button:not([disabled]), .product-purchase-panel a[href], .product-reviews button:not([disabled]), .related-section a[href], .related-section button:not([disabled])');
  const count = await interactive.count();
  expect(count).toBeGreaterThan(0);
  for (let index = 0; index < count; index += 1) {
    await interactive.nth(index).focus();
    await expect(interactive.nth(index)).toBeFocused();
    const box = await interactive.nth(index).boundingBox();
    expect(box?.height || 0).toBeGreaterThanOrEqual(44);
  }

  await expect(page.locator('img:not([alt])')).toHaveCount(0);
  await expect(page.locator('button:not([aria-label])').filter({ hasText: /^\s*$/ })).toHaveCount(0);
  await expect(page.locator('[role="progressbar"]')).toHaveCount(5);
  await expect(page.locator('.main-image')).toHaveCSS('transition-duration', '0s');
  await page.locator('html').evaluate((element) => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Premium product UI tràn ngang');
  });

  await testInfo.attach(`premium-product-${testInfo.project.name}`, {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});
```

This uses Playwright only; do not add axe or another dependency. If an existing text button lacks `aria-label`, it remains valid because its visible text supplies the accessible name.

- [ ] **Step 2: Run all Node frontend tests.**

```powershell
npm test
```

Expected: exit `0`; all tests PASS. A failure outside permitted files must be reported, not fixed by widening scope.

- [ ] **Step 3: Run production build.**

```powershell
npm run build
```

Expected: Vite exits `0`; no unresolved Font Awesome import because CSS is CDN-loaded; no Vue compile warning caused by changed templates.

- [ ] **Step 4: Run directly related Playwright suites on desktop and mobile.**

```powershell
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:8080'; npm run test:e2e -- tests/e2e/home.spec.js tests/e2e/product-reviews.spec.js --project=desktop-chrome --project=mobile-chrome
```

Expected: all tests PASS; both projects generate `premium-product-desktop-chrome` and `premium-product-mobile-chrome` screenshot attachments; homepage/product/cart primary requests succeed; mocked review/cart/favorite/catalog requests preserve asserted counts; no unexpected console/page error.

- [ ] **Step 5: Inspect both visual artifacts against exact acceptance.**

Desktop acceptance:

```text
- Product detail is two-column; gallery left, purchase panel right.
- Card image remains dominant; four related cards fit when four are available.
- Product name, rating, price, options, availability, CTA and delivery hierarchy is visually distinct.
- Review summary, distribution and list align without overlap.
- Font Awesome icons render as glyphs, not missing-character boxes.
```

Mobile acceptance:

```text
- Product detail becomes one column; no sticky overlap.
- CTA labels and prices do not clip; controls remain at least 44px high.
- Delivery blocks stack; review pagination wraps; related products use two columns.
- No horizontal overflow at Pixel 7 viewport or 320px CSS viewport.
- Focus rings are visible and not clipped by card/panel overflow.
```

If either artifact fails one item, return to the owning task, add a failing assertion where measurable, apply the smallest CSS/template correction, then rerun Steps 2–4.

- [ ] **Step 6: Verify scope and working-tree preservation.**

Run from repository root:

```powershell
git diff --name-only
git diff -- frontend/index.html frontend/src/components/common/ProductCard.vue frontend/src/views/guest/ProductDetailPage.vue frontend/tests/product-review-ui.test.mjs frontend/tests/ui-scope-hide-policy.test.mjs frontend/tests/e2e/product-reviews.spec.js frontend/tests/e2e/home.spec.js
```

Expected: implementation changes are limited to permitted files. Existing uncommitted files remain present; no backend/API/DB/OpenAPI/store/mapper/business-logic file was modified by this implementation; no commit exists.

## Definition of Done

- [ ] Font Awesome Free 6.7.2 loads from the approved CDN; Bootstrap Icons 1.11.3 remains loaded and untouched elsewhere.
- [ ] `ProductCard.vue` and `ProductDetailPage.vue` contain no emoji and use the exact approved Font Awesome classes with decorative icons hidden from assistive technology.
- [ ] ProductCard has premium responsive presentation, truthful badges/data, valid pricing, two-line description, visible rating/sold count, 44px circular direct-add action, hydration-safe option navigation, preserved favorite/cart behavior and reduced-motion support.
- [ ] ProductDetail has premium information hierarchy, accessible selected/disabled option states, unchanged modifier/stock/quantity validation, unchanged `/cart` and `/checkout` actions, truthful delivery claims, preserved loading/error/not-found states and reduced-motion support.
- [ ] Review section preserves initial and nonblocking errors, loading, empty, distribution, public fields and pagination behavior; related cards derive only from already hydrated same-category catalog products, exclude the current product and never trigger a new fetch.
- [ ] No backend, API, DB, OpenAPI, store, mapper, hydration utility or business logic changed; existing uncommitted data/hydration work remains intact.
- [ ] `npm test` passes.
- [ ] `npm run build` passes.
- [ ] Related Playwright suites pass on `desktop-chrome` and `mobile-chrome`; primary requests succeed; no unexpected console/page errors; no horizontal overflow.
- [ ] Desktop/mobile screenshots satisfy the visual checklist; keyboard focus, accessible names, image alternatives, progress semantics, 44px targets and reduced motion are verified.
- [ ] No placeholder, new dependency, production comment, commit, stage or push was introduced.
