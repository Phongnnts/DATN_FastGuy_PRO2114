# UI Scope Hide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ẩn Review, Support, Combo và mọi dòng phí dịch vụ khỏi UI FastGuy; giữ nguyên tổng tiền backend, ca làm việc, COD, hoàn tiền; chuẩn hóa nhãn Kích cỡ và Topping/Tùy chọn.

**Architecture:** Thay đổi trình bày tối thiểu trong Vue/router; không đổi dữ liệu, request, response, store, API client hay backend. Các route Support bị gỡ khỏi bảng route để tự rơi vào catch-all `NotFound`; tổng checkout vẫn gồm phí dịch vụ nhưng không hiện dòng riêng, còn tổng ở chi tiết đơn tiếp tục dùng `finalAmount`/`total` từ backend.

**Tech Stack:** Vue 3, Vue Router 4, Pinia, Node.js `node:test`, Playwright, Vite.

## Global Constraints

- Chỉ sửa `Frontend`; không sửa backend, API, OpenAPI, database.
- Không thêm dependency, abstraction hoặc API call.
- Không xóa dữ liệu/field khỏi mapper/store; chỉ ẩn consumer UI.
- Giữ nguyên shifts, COD, refund và mọi control liên quan.
- Giữ nguyên phép tính checkout có `serviceFee`; chỉ bỏ dòng hiển thị phí.
- Chi tiết/thành công đơn phải tiếp tục hiển thị tổng backend (`finalAmount` được map thành `total`); không tự tính lại tổng từ các dòng còn thấy.
- Route Support đã gỡ phải tới catch-all `NotFound`; guard đăng nhập/phân quyền hiện có vẫn hoạt động.
- Từ ngữ nhìn thấy: variant là `Kích cỡ`; modifier là `Topping` hoặc `Tùy chọn` theo ngữ cảnh.
- TDD bắt buộc: RED trước, GREEN sau.
- Không commit, push hoặc stage.

## File Map

- Create: `Frontend/tests/ui-scope-hide-policy.test.mjs` — regression policy tập trung cho phần UI bị ẩn, route fallback, tổng backend và wording.
- Modify: `Frontend/src/router/index.js` — bỏ route `UserSupport`, `StaffSupport` và title map tương ứng; giữ catch-all `NotFound`.
- Modify: `Frontend/src/layouts/GuestLayout.vue` — bỏ CTA gửi Support; giữ điện thoại/email tĩnh.
- Modify: `Frontend/src/layouts/StaffLayout.vue` — bỏ nav/route exception Support.
- Modify: `Frontend/src/components/common/AccountTabs.vue` — bỏ tab Support.
- Modify: `Frontend/src/views/user/AccountOverviewPage.vue` — bỏ shortcut Support.
- Modify: `Frontend/src/views/guest/HelpPage.vue` — bỏ CTA đăng nhập/gửi ticket; giữ FAQ và tra cứu đơn.
- Modify: `Frontend/src/views/user/OrderDetailPage.vue` — bỏ toàn bộ review consumer; giữ refund/COD/order total.
- Modify: `Frontend/src/views/admin/OrderDetailPage.vue` — bỏ review feature UI/handler; giữ refund/order controls.
- Modify: `Frontend/src/views/guest/HomePage.vue` — không render review proof và occasion combo.
- Modify: `Frontend/src/views/guest/ProductDetailPage.vue` — không render combo composition; đổi nhãn kích cỡ/topping.
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue` — bỏ tab/import/render Combo; đổi tab wording.
- Modify: `Frontend/src/components/admin/product-editor/ProductVariantsSection.vue` — đổi wording variant nhìn thấy.
- Modify: `Frontend/src/components/admin/product-editor/ProductModifiersSection.vue` — chuẩn hóa wording modifier.
- Modify: `Frontend/src/views/user/CheckoutPage.vue` — ẩn hai dòng phí phục vụ; giữ `serviceFee` trong `total` và payload hiện hữu.
- Modify: `Frontend/src/views/user/OrderSuccessPage.vue` — bảo vệ tổng backend, wording item.
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue` — ẩn phí dịch vụ, đổi wording item; giữ COD/refund/shift-related actions.
- Modify: `Frontend/src/views/shipper/OrderDetailPage.vue` — ẩn phí dịch vụ; giữ COD/refund/delivery actions.
- Modify: `Frontend/src/components/shipper/OrderActionSheet.vue` — ẩn phí dịch vụ; giữ tổng/COD actions.
- Modify: `Frontend/src/views/admin/SettingsPage.vue` — ẩn field phí dịch vụ; không đổi config API/helper.
- Modify: `Frontend/src/views/admin/ReportsPage.vue` — bỏ card/CSV row phí dịch vụ; giữ gross/refund/net cash backend fields.
- Modify: `Frontend/tests/admin-homepage-controls.test.mjs` — bỏ assertion yêu cầu combo/review UI.
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs` — đổi assertions để yêu cầu Combo không xuất hiện trong editor.
- Modify: `Frontend/tests/admin-reports-policy.test.mjs` — đổi assertions để cấm dòng/card phí dịch vụ nhưng giữ reconciliation/refund.
- Modify: `Frontend/tests/settings-policy.test.mjs`, `Frontend/tests/ui-consistency-policy.test.mjs`, `Frontend/tests/staff-support-ownership.test.mjs`, `Frontend/tests/staff-kitchen-contract.test.mjs`, `Frontend/tests/shipper-app-policy.test.mjs` — cập nhật policy cũ đang bắt UI bị loại bỏ; không đổi test contract dữ liệu backend.
- Modify: `Frontend/tests/e2e/home.spec.js` — xác nhận homepage không có review/combo ở desktop/mobile.

---

### Task 1: Tests + Hide Review and Support

**Files:**
- Create: `Frontend/tests/ui-scope-hide-policy.test.mjs`
- Modify: `Frontend/tests/admin-homepage-controls.test.mjs`
- Modify: `Frontend/tests/staff-support-ownership.test.mjs`
- Modify: `Frontend/tests/ui-consistency-policy.test.mjs`
- Modify: `Frontend/src/router/index.js`
- Modify: `Frontend/src/layouts/GuestLayout.vue`
- Modify: `Frontend/src/layouts/StaffLayout.vue`
- Modify: `Frontend/src/components/common/AccountTabs.vue`
- Modify: `Frontend/src/views/user/AccountOverviewPage.vue`
- Modify: `Frontend/src/views/guest/HelpPage.vue`
- Modify: `Frontend/src/views/user/OrderDetailPage.vue`
- Modify: `Frontend/src/views/admin/OrderDetailPage.vue`
- Modify: `Frontend/src/views/guest/HomePage.vue`

**Interfaces:**
- Consumes: Vue Router catch-all `{ path: '/:pathMatch(.*)*', name: 'NotFound' }`; existing auth guards; backend order mapping.
- Produces: no reachable `/account/support` or `/staff/support`; no Review customer/admin/homepage UI; unchanged order, COD and refund data flow.

- [ ] **Step 1: Write failing scope tests**

Create `Frontend/tests/ui-scope-hide-policy.test.mjs` using the existing source-policy style:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const userOrder = read('../src/views/user/OrderDetailPage.vue');
const adminOrder = read('../src/views/admin/OrderDetailPage.vue');
const home = read('../src/views/guest/HomePage.vue');
const guestLayout = read('../src/layouts/GuestLayout.vue');
const staffLayout = read('../src/layouts/StaffLayout.vue');
const accountTabs = read('../src/components/common/AccountTabs.vue');
const accountOverview = read('../src/views/user/AccountOverviewPage.vue');
const help = read('../src/views/guest/HelpPage.vue');

test('review UI is absent from customer admin and homepage', () => {
  assert.doesNotMatch(userOrder, /reviewApi|StarRating|Đánh giá đơn hàng|homepageConsent/);
  assert.doesNotMatch(adminOrder, /updateFeaturedReview|order\.review|review-card/);
  assert.doesNotMatch(home, /HomepageProof|featuredReviews/);
});

test('support routes navigation and ticket CTA are absent', () => {
  assert.doesNotMatch(router, /UserSupport|StaffSupport|views\/(user|staff)\/SupportPage/);
  assert.match(router, /path: '\/:pathMatch\(\.\*\)\*'[\s\S]*name: 'NotFound'/);
  for (const source of [guestLayout, staffLayout, accountTabs, accountOverview, help]) {
    assert.doesNotMatch(source, /\/account\/support|\/staff\/support/);
  }
});

test('COD refund and shifts remain visible', () => {
  assert.match(userOrder, /refundLabel/);
  assert.match(staffLayout, /StaffShifts|Ca làm việc/);
  assert.match(router, /StaffShifts/);
});
```

Update conflicting old tests: remove assertions that require admin review controls, Support page ownership/nav, or `StaffSupport`; replace them with `doesNotMatch` assertions against reachable UI. Keep unrelated assertions unchanged.

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```powershell
npm test -- --test-name-pattern="review UI|support routes|COD refund"
```

Expected: FAIL because Review, Support routes/nav/CTA and homepage review still exist.

- [ ] **Step 3: Apply minimal Review removals**

- In `OrderDetailPage.vue` user: remove `StarRating`, `reviewApi`, review refs/computeds/functions, review load call, review template and review-only CSS. Do not touch cancel, reorder, payment or refund branches.
- In admin order detail: remove `reviewSaving`, `reviewMessage`, `reviewEligibilityReasons`, `updateFeaturedReview`, review card and review-only CSS. Keep order loading and refund/payment attempts unchanged.
- In homepage: remove `HomepageProof` import/render only. Do not alter homepage store/API shape.

- [ ] **Step 4: Apply minimal Support removals and safe fallback**

- Delete child route objects named `UserSupport` and `StaffSupport`; delete title map entries only.
- Keep catch-all `NotFound` last. Do not add redirects that could bypass role guards.
- Remove Support nav entries from `AccountTabs.vue` and `StaffLayout.vue`; remove `$route.name === 'StaffSupport'` exception.
- Remove ticket CTAs from account overview, guest footer and Help page. Keep phone/email, FAQ and order tracking links.
- Leave unreachable Support page files and API methods intact to minimize blast radius.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run:

```powershell
npm test -- --test-name-pattern="review UI|support routes|COD refund|homepage|support"
```

Expected: PASS; no assertion requires removed Review/Support UI.

### Task 2: Hide Combo and Normalize Visible Wording

**Files:**
- Modify: `Frontend/tests/ui-scope-hide-policy.test.mjs`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`
- Modify: `Frontend/tests/admin-homepage-controls.test.mjs`
- Modify: `Frontend/src/views/guest/HomePage.vue`
- Modify: `Frontend/src/views/guest/ProductDetailPage.vue`
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/src/components/admin/product-editor/ProductVariantsSection.vue`
- Modify: `Frontend/src/components/admin/product-editor/ProductModifiersSection.vue`
- Modify: visible item headings in `Frontend/src/views/user/OrderDetailPage.vue`, `Frontend/src/views/user/OrderSuccessPage.vue`, `Frontend/src/views/staff/OrderDetailPage.vue`, `Frontend/src/views/admin/OrderDetailPage.vue`

**Interfaces:**
- Consumes: existing product/variant/modifier objects unchanged.
- Produces: no Combo homepage/detail/editor UI; visible variant labels use `Kích cỡ`; modifier labels use `Topping`/`Tùy chọn`.

- [ ] **Step 1: Add failing Combo/wording tests**

Append:

```js
const productDetail = read('../src/views/guest/ProductDetailPage.vue');
const productEditor = read('../src/views/admin/ProductEditorPage.vue');
const variantsSection = read('../src/components/admin/product-editor/ProductVariantsSection.vue');
const modifiersSection = read('../src/components/admin/product-editor/ProductModifiersSection.vue');

test('combo UI is absent from homepage product detail and admin editor', () => {
  assert.doesNotMatch(home, /HomepageOccasions|occasionCombos/);
  assert.doesNotMatch(productDetail, /product\.combo|Combo gồm/);
  assert.doesNotMatch(productEditor, /ProductComboSection|id: 'combo'|activeSection === 'combo'/);
});

test('visible product option wording uses Kích cỡ and Topping or Tùy chọn', () => {
  assert.match(productDetail, /Kích cỡ/);
  assert.doesNotMatch(productDetail, />Phân loại</);
  assert.match(variantsSection, /Kích cỡ/);
  assert.doesNotMatch(variantsSection, />Biến thể</);
  assert.match(modifiersSection, /Topping|Tùy chọn/);
});
```

Change old product-editor/homepage tests to assert Combo editor wiring is absent. Keep helper/API tests because data capabilities remain unchanged.

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```powershell
npm test -- --test-name-pattern="combo UI|visible product option wording|product editor"
```

Expected: FAIL on existing homepage occasion block, product combo block, editor tab/component and old wording.

- [ ] **Step 3: Remove only Combo presentation**

- Remove `HomepageOccasions` import/render from homepage.
- Remove `product.combo` block from product detail.
- Remove `ProductComboSection` import, Combo tab, Combo render branch and Combo-only dirty tracking/reload wiring from product editor.
- Leave `ProductComboSection.vue`, combo API methods, mapper/store fields and helper functions untouched.

- [ ] **Step 4: Relabel visible wording**

- Product detail and admin variant editor: `Phân loại`/`Biến thể` to `Kích cỡ`, including buttons, headings and user-facing fallbacks.
- Modifier editor/group presentation: use `Topping` for food add-ons; use `Tùy chọn` for generic action/input wording.
- Order tables/item summaries: change visible `Phân loại` headings to `Kích cỡ`; retain raw `variantName` and modifier values.
- Do not rename symbols, JSON fields, IDs, route params or API payload fields.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run:

```powershell
npm test -- --test-name-pattern="combo UI|visible product option wording|product editor|homepage"
```

Expected: PASS.

### Task 3: Hide Service Fee Rows, Preserve Backend Totals

**Files:**
- Modify: `Frontend/tests/ui-scope-hide-policy.test.mjs`
- Modify: `Frontend/tests/admin-reports-policy.test.mjs`
- Modify: `Frontend/tests/settings-policy.test.mjs`
- Modify: `Frontend/tests/ui-consistency-policy.test.mjs`
- Modify: `Frontend/tests/staff-kitchen-contract.test.mjs`
- Modify: `Frontend/tests/shipper-app-policy.test.mjs`
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Modify: `Frontend/src/views/user/OrderDetailPage.vue`
- Modify: `Frontend/src/views/user/OrderSuccessPage.vue`
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue`
- Modify: `Frontend/src/views/shipper/OrderDetailPage.vue`
- Modify: `Frontend/src/components/shipper/OrderActionSheet.vue`
- Modify: `Frontend/src/views/admin/SettingsPage.vue`
- Modify: `Frontend/src/views/admin/ReportsPage.vue`

**Interfaces:**
- Consumes: checkout `serviceFee`, order `finalAmount`/`total`, report `grossRevenue`/`refundTotal`/`netCashRevenue`.
- Produces: no visible service-fee row/card/input/export row; totals, COD and refunds remain backend-consistent.

- [ ] **Step 1: Add failing fee-hiding and total-preservation tests**

Append:

```js
const checkout = read('../src/views/user/CheckoutPage.vue');
const orderSuccess = read('../src/views/user/OrderSuccessPage.vue');
const staffOrder = read('../src/views/staff/OrderDetailPage.vue');
const shipperOrder = read('../src/views/shipper/OrderDetailPage.vue');
const shipperSheet = read('../src/components/shipper/OrderActionSheet.vue');
const settings = read('../src/views/admin/SettingsPage.vue');
const reports = read('../src/views/admin/ReportsPage.vue');

test('service fee rows are hidden without changing checkout arithmetic', () => {
  for (const source of [checkout, userOrder, orderSuccess, staffOrder, shipperOrder, shipperSheet, settings, reports]) {
    assert.doesNotMatch(source, /Phí dịch vụ|Phí phục vụ/);
  }
  assert.match(checkout, /cart\.subtotal \+ \(shippingFee\.value \|\| 0\) \+ serviceFee\.value - couponDiscount\.value/);
  assert.match(userOrder, /total: data\.finalAmount \|\| 0/);
  assert.match(userOrder, /formatPrice\(order\.total\)/);
  assert.match(orderSuccess, /formatPrice\(order\.total\)/);
});

test('refund COD shifts and backend report totals remain', () => {
  assert.match(staffOrder, /COD|paymentMethod/);
  assert.match(reports, /grossRevenue/);
  assert.match(reports, /refundTotal/);
  assert.match(reports, /netCashRevenue/);
});
```

Update existing report/settings/staff/shipper policy assertions: forbid service-fee presentation, but retain contract fields in stores/mappers and operational assertions.

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```powershell
npm test -- --test-name-pattern="service fee rows|refund COD shifts|report|settings|shipper|kitchen"
```

Expected: FAIL because fee labels/cards/input/export rows remain.

- [ ] **Step 3: Remove fee presentation only**

- Checkout: remove both `Phí phục vụ` rows. Keep `serviceFee` computed and `total` expression unchanged so visible checkout total still matches submitted pricing.
- User order detail/success: keep backend total displays unchanged; remove a fee row only if present after Task 2 edits.
- Staff/shipper detail and shipper action sheet: remove fee rows only; preserve total, discount, payment method, COD collection, refund and delivery actions.
- Settings: remove service-fee form group from template only; do not alter config loading, validation helper, payload mapping or other settings.
- Reports: remove service-fee KPI card and CSV summary row only. Preserve backend gross revenue, refund, net cash and operational cohort calculations/charts.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run:

```powershell
npm test -- --test-name-pattern="service fee rows|refund COD shifts|report|settings|shipper|kitchen"
```

Expected: PASS; tests still prove backend total rendering and checkout arithmetic.

### Task 4: Full Tests, Build, Desktop/Mobile E2E and Self-Review

**Files:**
- Modify: `Frontend/tests/e2e/home.spec.js`
- Modify: `Frontend/tests/ui-scope-hide-policy.test.mjs` only if verification exposes a missing explicit assertion.

**Interfaces:**
- Consumes: completed Tasks 1–3.
- Produces: verified desktop/mobile UI scope, safe route fallback, clean console and successful primary requests.

- [ ] **Step 1: Extend Playwright coverage and verify RED before final E2E adjustments**

Add tests using existing route mocks in `home.spec.js`:

```js
test('hidden homepage scope stays absent on desktop and mobile', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ status: 'success', data: {
      bestSellers: [],
      occasionCombos: [{ productId: 1, name: 'Combo ẩn' }],
      featuredReviews: [{ reviewId: 1, userName: 'Ẩn', rating: 5, comment: 'Ẩn' }],
    } }),
  }));
  for (const viewport of [{ width: 1440, height: 900 }, { width: 390, height: 844 }]) {
    await page.setViewportSize(viewport);
    await page.goto('/home');
    await expect(page.getByText('Combo ẩn')).toHaveCount(0);
    await expect(page.getByText('Trải nghiệm thật từ khách hàng FastGuy')).toHaveCount(0);
  }
});
```

Add route-fallback assertions for `/account/support` and `/staff/support` using the project’s existing auth setup; assert either `NotFound` content or the existing auth redirect when unauthenticated, never a Support page.

Run:

```powershell
npm run test:e2e -- tests/e2e/home.spec.js
```

Expected before completing mocks/assertions: RED on any stale visible scope or incorrect route expectation.

- [ ] **Step 2: Run complete frontend unit/policy suite**

Run:

```powershell
npm test
```

Expected: PASS, zero failures. Fix only stale assertions caused by this scope change; do not weaken unrelated tests.

- [ ] **Step 3: Run production build**

Run:

```powershell
npm run build
```

Expected: PASS with Vite exit code 0; no unresolved removed imports/components.

- [ ] **Step 4: Run desktop and mobile Playwright verification**

Run:

```powershell
npm run test:e2e -- tests/e2e/home.spec.js --project=chromium
```

If `playwright.config.js` defines separate desktop/mobile projects, run both project names instead. Verify at 1440×900 and 390×844:

- Homepage has no Review or Combo sections even when mocked payload includes both.
- Product detail has no Combo composition; visible labels read `Kích cỡ` and `Topping`/`Tùy chọn`.
- User/staff Support URLs do not render Support; NotFound/auth redirect is safe.
- Checkout/order views have no service-fee row; displayed total remains present.
- Staff/shipper flows still expose shifts/COD; order views still expose refund state where applicable.
- Browser console has no error.
- Homepage/product/order primary API requests complete successfully under test mocks.

Expected: PASS on desktop and mobile.

- [ ] **Step 5: Self-review diff and plan acceptance**

Run:

```powershell
git diff --check
git status --short
git diff -- Frontend
```

Confirm:

- Only intended `Frontend` files changed during implementation.
- No Backend, OpenAPI, database, API client or dependency file changed.
- No `TBD`, `TODO`, placeholder assertion, disabled test or `.skip` added.
- No Support route remains reachable; catch-all `NotFound` remains last.
- No Review/Combo/service-fee visible copy remains in scoped UI.
- `serviceFee` remains in checkout arithmetic and existing data contracts.
- Backend `finalAmount`/`total`, shifts, COD and refund behavior remain intact.
- No commit, push or stage occurred.
