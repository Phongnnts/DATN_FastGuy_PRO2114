# Customer Conversion UX Quick Wins Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện các quick wins chuyển đổi khách hàng: coupon thủ công cho Guest/User, profile hydration, phản hồi migrate giỏ hàng, ETA thật, reorder giữ modifier hợp lệ, và loại bỏ claim giao hàng không có dữ liệu.

**Architecture:** Giữ nguyên contract backend hiện có và sửa lát frontend nhỏ nhất. Pinia chịu trách nhiệm hydrate session/cart; các page chỉ render loading/error/success từ state cục bộ; order detail dùng `items[].modifiers` đã có và tracking render `trackingResult.estimatedDeliveryAt` đã được order store normalize từ backend `estimatedDeliveryAt`. Tests dùng `node:test` theo convention source-contract hiện tại, không thêm dependency.

**Tech Stack:** Vue 3 Composition API, Pinia 3, Vue Router 4, Axios client hiện có, CSS variables hiện có, Node.js built-in test runner.

## Global Constraints

- Chỉ sửa phạm vi quick wins Customer trong `docs/superpowers/specs/2026-08-14-single-store-operations-ui-ux-design.md:61-68` và accessibility/tokens trên màn được chạm.
- Checkout phải cho cả Guest và User nhập coupon thủ công; server checkout vẫn tính lại tổng authoritative qua `couponCode` hiện có.
- Profile phải hydrate từ `GET /api/auth/profile`; không lấy local-storage snapshot làm nguồn dữ liệu cuối cùng.
- Lỗi migrate guest cart sau login phải hiện cho người dùng; không được nuốt mutation error.
- ETA chỉ hiện khi backend `estimatedDeliveryAt` được normalize thành `trackingResult.estimatedDeliveryAt`; không suy diễn từ client clock.
- Reorder giữ `variantId` và toàn bộ modifier option còn hợp lệ; item lỗi phải được liệt kê riêng và không chặn item hợp lệ.
- Claim freeship/thời gian giao chỉ hiện khi khớp `GET /api/store/config` hoặc dữ liệu đơn/GHN thật.
- Không thêm backend endpoint, migration, dependency, abstraction dùng một lần, hoặc refactor ngoài phạm vi.
- Giữ Vue 3, Pinia, servlet/service/DAO/entity và SQL migration hiện có.
- Input có label; lỗi liên kết bằng `aria-describedby`; status động dùng `role="status"`/`role="alert"`; control chạm tối thiểu `44px`.
- Dùng token sẵn có trong `Frontend/src/assets/styles/variables.css`; không tạo token toàn cục mới khi token hiện tại đủ.
- Không commit. Bỏ qua mọi bước commit mặc định của workflow.

---

## File Map

- Modify: `Frontend/src/views/user/CheckoutPage.vue` — mở form coupon thủ công cho Guest/User, giữ wallet riêng cho User, thêm trạng thái accessible và hit area.
- Modify: `Frontend/src/stores/auth.js` — expose `hydrateProfile(): Promise<object>` để đọc profile thật, cập nhật/persist session.
- Modify: `Frontend/src/views/user/ProfilePage.vue` — gọi hydration khi mount; render loading/error/retry trước form.
- Modify: `Frontend/src/views/guest/LoginPage.vue` — await migrate trước redirect và hiện cảnh báo có thể hành động.
- Unchanged: `Frontend/src/stores/order.js:101-122` — normalization hiện có đã map backend `data.estimatedDeliveryAt` thành `trackingResult.estimatedDeliveryAt`; không cần code change.
- Modify: `Frontend/src/views/guest/TrackOrderPage.vue` — render `trackingResult.estimatedDeliveryAt` khi normalized result có dữ liệu.
- Modify: `Frontend/src/views/user/OrderDetailPage.vue` — map modifier snapshots, validate option IDs hiện hành, reorder với modifiers và báo lỗi theo item.
- Modify: `Frontend/src/router/index.js` — bỏ meta description “30 phút” không dựa dữ liệu runtime.
- Modify: `Frontend/src/layouts/GuestLayout.vue` — bỏ footer claim “30 phút”.
- Modify: `Frontend/src/views/guest/ProductDetailPage.vue` — tải store config và thay benefit claim bằng dữ liệu cấu hình; không claim freeship thiếu contract.
- Modify: `Frontend/src/views/guest/CartPage.vue` — thay claim “30 phút” bằng copy trung thực vì phí/ETA chỉ có sau địa chỉ.
- Modify: `Frontend/test/checkout-integration-ux.test.js` — source tests cho manual coupon Guest/User và accessibility.
- Create: `Frontend/test/customer-conversion-quick-wins.test.js` — source-contract tests cho profile hydration, migrate feedback, ETA, modifier reorder, truthful claims.

## Exact Existing Contracts

```js
// Frontend/src/api/auth.js
getProfile(): Promise<{
  userId: number,
  fullName: string,
  email: string,
  phone: string,
  avatarUrl: string,
  role: 'USER' | 'STAFF' | 'SHIPPER' | 'ADMIN',
  status: string,
  loyaltyPoints: number,
  createdAt: string | null
}>

// Frontend/src/api/coupon.js
verify(code: string, totalAmount: number, shippingFee: number): Promise<{
  valid: boolean,
  code?: string,
  description?: string,
  discount?: number,
  message?: string
}>

// Backend tracking payload consumed by Frontend/src/stores/order.js
{
  orderCode: string,
  orderStatus: string,
  estimatedDeliveryAt?: string | null,
  items: object[],
  statusHistory: Array<{ status: string, timestamp: string }>
}

// Existing Frontend/src/stores/order.js normalization; unchanged
trackOrder(orderCode: string, phoneSuffix: string): Promise<{
  orderCode: string,
  status: string,
  estimatedDeliveryAt: string | null,
  items: object[],
  statusHistory: Array<{ status: string, time: string }>
}>

// GET /api/orders/:id, already emitted by OrderServlet.toDetail
items[].modifiers: Array<{
  modifierOptionId: number,
  groupId: number,
  groupName: string,
  name: string,
  price: number | string
}>

// Frontend/src/stores/cart.js
addItem(
  productId: number,
  variantId: number,
  quantity?: number,
  modifiers?: Array<{ modifierOptionId: number, groupId?: number, groupName?: string, name: string, price: number | string }>
): Promise<void>

migrateToUser(): Promise<void>

// Frontend/src/api/store.js
getConfig(): Promise<{
  estimatedDeliveryMinutes?: number,
  serviceFee?: number,
  isOpen?: boolean,
  openTime?: string,
  closeTime?: string
}>
```

### Task 1: Manual Coupon for Guest and User Checkout

**Files:**
- Modify: `Frontend/src/views/user/CheckoutPage.vue:283-327,594-654`
- Modify: `Frontend/test/checkout-integration-ux.test.js`

**Interfaces:**
- Consumes: `couponApi.verify(code: string, totalAmount: number, shippingFee: number)` and existing checkout payload field `couponCode: string`.
- Produces: one manual coupon form visible to Guest/User; claimed wallet remains User-only; `appliedCoupon.value?.code` continues into both checkout contracts.

- [ ] **Step 1: Append failing source tests**

Add to `Frontend/test/checkout-integration-ux.test.js`:

```js
test('manual coupon form is available to guest and user while wallet remains user-only', () => {
  assert.match(checkout, /<form class="coupon-manual" @submit\.prevent="verifyCoupon"/);
  assert.match(checkout, /id="checkout-coupon-code"/);
  assert.match(checkout, /aria-describedby="checkout-coupon-status"/);
  assert.match(checkout, /<div v-if="!isGuest && !appliedCoupon" class="my-coupons">/);
  assert.doesNotMatch(checkout, /<div v-if="!isGuest" class="checkout-coupon">/);
});

test('coupon verification announces loading, errors, and applied state', () => {
  assert.match(checkout, /id="checkout-coupon-status"/);
  assert.match(checkout, /role="status"/);
  assert.match(checkout, /role="alert"/);
  assert.match(checkout, /:disabled="verifyingCoupon \|\| !couponCode\.trim\(\)"/);
  assert.match(checkout, /couponCode: appliedCoupon\.value\?\.code \|\| ''/);
});
```

- [ ] **Step 2: Run RED test**

Run: `npm test -- --test-name-pattern="manual coupon|coupon verification"`

Working directory: `Frontend`

Expected: FAIL because `coupon-manual`, `checkout-coupon-code`, and `checkout-coupon-status` do not exist and coupon container is still guarded by `v-if="!isGuest"`.

- [ ] **Step 3: Replace coupon markup with minimal shared manual form**

In `Frontend/src/views/user/CheckoutPage.vue`, đổi chính xác opening tag:

```vue
<div v-if="!isGuest" class="checkout-coupon">
```

thành:

```vue
<div class="checkout-coupon">
```

Ngay sau closing tag của `.coupon-header`, chèn form đầy đủ:

```vue
<form v-if="!appliedCoupon" class="coupon-manual" @submit.prevent="verifyCoupon">
  <label for="checkout-coupon-code">Nhập mã giảm giá</label>
  <div class="coupon-manual-row">
    <input
      id="checkout-coupon-code"
      v-model.trim="couponCode"
      class="form-input"
      type="text"
      autocomplete="off"
      maxlength="50"
      aria-describedby="checkout-coupon-status"
      placeholder="Ví dụ: FASTGUY10"
    />
    <button type="submit" class="btn btn-outline" :disabled="verifyingCoupon || !couponCode.trim()">
      {{ verifyingCoupon ? 'Đang kiểm tra...' : 'Áp dụng' }}
    </button>
  </div>
  <p v-if="verifyingCoupon" id="checkout-coupon-status" class="coupon-msg" role="status">Đang kiểm tra mã giảm giá.</p>
  <p v-else-if="couponError" id="checkout-coupon-status" class="coupon-msg error" role="alert">{{ couponError }}</p>
  <p v-else id="checkout-coupon-status" class="sr-only">Nhập mã rồi chọn Áp dụng.</p>
</form>
```

Xóa block lỗi cũ sau wallet để tránh hai message cùng render:

```vue
<div v-if="!appliedCoupon && couponError" class="coupon-body">
  <div class="coupon-msg error">
    <i class="bi bi-exclamation-circle"></i> {{ couponError }}
  </div>
</div>
```

Thêm `role="status"` vào opening tag hiện có của applied card:

```vue
<div v-if="appliedCoupon" class="coupon-applied" role="status">
```

Không đổi nội dung `.my-coupons` và children của `.coupon-applied`.

Append scoped styles in `CheckoutPage.vue`:

```css
.coupon-manual { display: grid; gap: var(--space-2); margin-top: var(--space-3); }
.coupon-manual > label { font-size: 13px; font-weight: 700; }
.coupon-manual-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); }
.coupon-manual-row .btn { min-height: var(--control-height); }
.coupon-msg { margin: 0; color: var(--text-mid); font-size: 13px; }
.coupon-msg.error { color: var(--red-active); }
@media (max-width: 480px) { .coupon-manual-row { grid-template-columns: 1fr; } .coupon-manual-row .btn { width: 100%; } }
```

- [ ] **Step 4: Run GREEN test**

Run: `npm test -- --test-name-pattern="manual coupon|coupon verification"`

Working directory: `Frontend`

Expected: PASS; Guest has manual input, User has manual input plus wallet, both payload branches retain `couponCode`.

### Task 2: Profile API Hydration with Loading/Error/Retry

**Files:**
- Modify: `Frontend/src/stores/auth.js:90-121`
- Modify: `Frontend/src/views/user/ProfilePage.vue:1-84`
- Create: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: `authApi.getProfile()` existing contract.
- Produces: `auth.hydrateProfile(): Promise<object>`; normalized store user shape `{ id, fullName, email, phone, avatarUrl, role, status, loyaltyPoints, createdAt }`.

- [ ] **Step 1: Create failing profile hydration tests**

Create `Frontend/test/customer-conversion-quick-wins.test.js`:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');
const authStore = read('../src/stores/auth.js');
const profile = read('../src/views/user/ProfilePage.vue');
const login = read('../src/views/guest/LoginPage.vue');
const tracking = read('../src/views/guest/TrackOrderPage.vue');
const orderDetail = read('../src/views/user/OrderDetailPage.vue');
const router = read('../src/router/index.js');
const guestLayout = read('../src/layouts/GuestLayout.vue');
const productDetail = read('../src/views/guest/ProductDetailPage.vue');
const cartPage = read('../src/views/guest/CartPage.vue');

test('profile hydrates authoritative API data and persists normalized user', () => {
  assert.match(authStore, /async function hydrateProfile\(\)/);
  assert.match(authStore, /const data = await authApi\.getProfile\(\)/);
  assert.match(authStore, /id: data\.userId/);
  assert.match(authStore, /persist\(\);/);
  assert.match(authStore, /hydrateProfile,/);
});

test('profile page exposes loading error and retry states', () => {
  assert.match(profile, /const profileLoading = ref\(true\)/);
  assert.match(profile, /const profileError = ref\(''\)/);
  assert.match(profile, /await auth\.hydrateProfile\(\)/);
  assert.match(profile, /role="status"/);
  assert.match(profile, /role="alert"/);
  assert.match(profile, /@click="loadProfile"/);
});
```

- [ ] **Step 2: Run RED profile tests**

Run: `npm test -- --test-name-pattern="profile"`

Working directory: `Frontend`

Expected: FAIL because `hydrateProfile`, `profileLoading`, `profileError`, and `loadProfile` do not exist.

- [ ] **Step 3: Add exact store hydration interface**

Add before `updateProfile` in `Frontend/src/stores/auth.js`:

```js
async function hydrateProfile() {
  if (!user.value) throw new Error('Chưa đăng nhập');
  const data = await authApi.getProfile();
  user.value = {
    id: data.userId,
    fullName: data.fullName || '',
    email: data.email || '',
    phone: data.phone || '',
    avatarUrl: data.avatarUrl || '',
    role: data.role || user.value.role,
    status: data.status || '',
    loyaltyPoints: Number(data.loyaltyPoints || 0),
    createdAt: data.createdAt || null,
  };
  persist();
  return user.value;
}
```

Add `hydrateProfile,` to returned store object immediately before `updateProfile,`.

- [ ] **Step 4: Hydrate page and expose actionable states**

Replace `onMounted(syncProfile);` and synchronous mount setup in `ProfilePage.vue` with:

```js
const profileLoading = ref(true);
const profileError = ref('');

onMounted(loadProfile);

async function loadProfile() {
  profileLoading.value = true;
  profileError.value = '';
  try {
    await auth.hydrateProfile();
    syncProfile();
  } catch (error) {
    profileError.value = error.message || 'Không thể tải hồ sơ. Vui lòng thử lại.';
  } finally {
    profileLoading.value = false;
  }
}
```

Insert before `.profile-grid`:

```vue
<div v-if="profileLoading" class="profile-state" role="status">
  <span class="spinner" aria-hidden="true"></span> Đang tải hồ sơ...
</div>
<div v-else-if="profileError" class="profile-state profile-error" role="alert">
  <span>{{ profileError }}</span>
  <button type="button" class="btn btn-primary" @click="loadProfile">Thử lại</button>
</div>
```

Change profile section condition to:

```vue
<section v-else class="profile-grid" aria-label="Thông tin tài khoản">
```

Append scoped styles:

```css
.profile-state { display: flex; align-items: center; justify-content: center; gap: var(--space-3); min-height: 180px; padding: var(--space-6); border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: var(--bg-card); }
.profile-error { flex-direction: column; color: var(--red-active); text-align: center; }
.profile-state .btn { min-height: var(--control-height); }
```

- [ ] **Step 5: Run GREEN profile tests**

Run: `npm test -- --test-name-pattern="profile"`

Working directory: `Frontend`

Expected: PASS.

### Task 3: Await Cart Migration and Surface Partial Failure

**Files:**
- Modify: `Frontend/src/views/guest/LoginPage.vue:17-31,43-59`
- Modify: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: `cart.migrateToUser(): Promise<void>`, which rejects with `Không thể đồng bộ N món trong giỏ hàng` and preserves failed items.
- Produces: login waits for migration; auth failure remains `error`; migration failure becomes non-blocking `migrationWarning` and then redirects.

- [ ] **Step 1: Append failing migration test**

```js
test('login awaits cart migration and announces recoverable failure', () => {
  assert.match(login, /const migrationWarning = ref\(''\)/);
  assert.match(login, /await cart\.migrateToUser\(\)/);
  assert.doesNotMatch(login, /migrateToUser\(\)\.catch\(\(\) => \{\}\)/);
  assert.match(login, /role="alert"/);
  assert.match(login, /Một số món chưa được đồng bộ/);
});
```

- [ ] **Step 2: Run RED migration test**

Run: `npm test -- --test-name-pattern="cart migration"`

Working directory: `Frontend`

Expected: FAIL because login currently fire-and-forgets migration and swallows rejection.

- [ ] **Step 3: Await migration without treating successful login as failed**

Add state:

```js
const migrationWarning = ref('');
```

Replace `handleLogin()` with:

```js
async function handleLogin() {
  error.value = '';
  migrationWarning.value = '';
  loading.value = true;
  try {
    const user = await auth.login(email.value, password.value);
    try {
      await cart.migrateToUser();
    } catch (migrationError) {
      migrationWarning.value = `${migrationError.message || 'Một số món chưa được đồng bộ.'} Các món này vẫn được giữ trong giỏ để bạn kiểm tra lại.`;
    }
    const role = user?.role || '';
    const redirect = route.query.redirect || (role === 'USER' ? '/home' : `/${role.toLowerCase()}`);
    if (migrationWarning.value) {
      await router.push({ path: redirect, query: { cartMigration: 'failed' } });
    } else {
      await router.push(redirect);
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
```

Because redirect would remove page-local warning, change warning delivery to existing toast store instead of query-only state: import `useToast`, create `const toast = useToast();`, and immediately before redirect call:

```js
if (migrationWarning.value) toast.error(migrationWarning.value);
```

Use plain `await router.push(redirect);`; do not retain `cartMigration` query branch. Keep `migrationWarning` state so behavior remains directly testable and render before button:

```vue
<p v-if="migrationWarning" class="form-error" role="alert">{{ migrationWarning }}</p>
```

- [ ] **Step 4: Run GREEN migration test**

Run: `npm test -- --test-name-pattern="cart migration"`

Working directory: `Frontend`

Expected: PASS; login redirect occurs after migration settles and toast/error text identifies preserved items.

### Task 4: Render Real ETA in Guest Tracking

**Files:**
- Modify: `Frontend/src/views/guest/TrackOrderPage.vue:1-145,150-195`
- Modify: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: `trackingResult.estimatedDeliveryAt: string | null`, already normalized unchanged by `Frontend/src/stores/order.js:117` from backend `data.estimatedDeliveryAt`.
- Produces: `formatEta(value: string): string`; ETA card only when normalized value is non-null. No `order.js` code change.

- [ ] **Step 1: Append failing ETA test**

```js
test('tracking displays only normalized backend ETA with accessible status', () => {
  assert.match(tracking, /function formatEta\(value\)/);
  assert.match(tracking, /v-if="trackingResult\.estimatedDeliveryAt"/);
  assert.match(tracking, /formatEta\(trackingResult\.estimatedDeliveryAt\)/);
  assert.match(tracking, /class="eta-card" role="status"/);
  assert.doesNotMatch(tracking, /Date\.now\(\).*estimatedDeliveryAt/);
});
```

- [ ] **Step 2: Run RED ETA test**

Run: `npm test -- --test-name-pattern="backend ETA"`

Working directory: `Frontend`

Expected: FAIL because tracking does not render normalized `trackingResult.estimatedDeliveryAt`; `Frontend/src/stores/order.js:117` already provides it and remains unchanged.

- [ ] **Step 3: Add minimal date formatter and ETA card**

Add in script:

```js
function formatEta(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
  }).format(date);
}
```

Insert after `.result-header`:

```vue
<div v-if="trackingResult.estimatedDeliveryAt" class="eta-card" role="status">
  <i class="bi bi-clock-history" aria-hidden="true"></i>
  <div><span>Thời gian giao dự kiến</span><strong>{{ formatEta(trackingResult.estimatedDeliveryAt) }}</strong></div>
</div>
```

Append scoped styles:

```css
.eta-card { display: flex; align-items: center; gap: var(--space-3); margin-top: var(--space-5); padding: var(--space-4); border: 1px solid var(--primary-100); border-radius: var(--radius-sm); background: var(--primary-light); }
.eta-card i { color: var(--primary-dark); font-size: 22px; }
.eta-card div { display: grid; gap: var(--space-1); }
.eta-card span { color: var(--text-mid); font-size: 12px; }
.eta-card strong { color: var(--text-dark); }
```

- [ ] **Step 4: Run GREEN ETA test**

Run: `npm test -- --test-name-pattern="backend ETA"`

Working directory: `Frontend`

Expected: PASS.

### Task 5: Preserve Valid Reorder Modifiers and Report Invalid Items

**Files:**
- Modify: `Frontend/src/views/user/OrderDetailPage.vue:60-73,131-155,203-213`
- Modify: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: order snapshot `item.modifiers[]`; live product `modifierGroups[].options[]` with `modifierOptionId`, `status`/`isActive`, `name`, `price`.
- Produces: `resolveReorderModifiers(product: object, snapshots: object[]): object[] | null`; `null` means at least one old option is unavailable, array means all snapshots resolved to live options.

- [ ] **Step 1: Append failing modifier reorder test**

```js
test('reorder maps snapshots to live modifier options and passes them to cart', () => {
  assert.match(orderDetail, /modifiers: Array\.isArray\(i\.modifiers\) \? i\.modifiers : \[\]/);
  assert.match(orderDetail, /function resolveReorderModifiers\(product, snapshots\)/);
  assert.match(orderDetail, /option\.modifierOptionId === Number\(snapshot\.modifierOptionId\)/);
  assert.match(orderDetail, /await cart\.addItem\(item\.productId, item\.variantId, item\.quantity, modifiers\)/);
  assert.match(orderDetail, /Không thể thêm .* tùy chọn không còn khả dụng/);
});
```

- [ ] **Step 2: Run RED modifier test**

Run: `npm test -- --test-name-pattern="reorder maps"`

Working directory: `Frontend`

Expected: FAIL because order mapping drops modifiers and reorder calls `cart.addItem` without them.

- [ ] **Step 3: Preserve snapshots during detail mapping**

Add to each mapped order item after `image`:

```js
modifiers: Array.isArray(i.modifiers) ? i.modifiers : [],
```

Add helper before `reorder()`:

```js
function resolveReorderModifiers(product, snapshots) {
  const options = (product?.modifierGroups || []).flatMap(group =>
    (group.options || []).map(option => ({
      ...option,
      groupId: option.groupId ?? group.modifierGroupId,
      groupName: option.groupName || group.name || '',
    })),
  );
  const resolved = snapshots.map(snapshot => options.find(option =>
    option.modifierOptionId === Number(snapshot.modifierOptionId)
      && option.status !== 'INACTIVE'
      && option.isActive !== false,
  ));
  if (resolved.some(option => !option)) return null;
  return resolved.map(option => ({
    modifierOptionId: Number(option.modifierOptionId),
    groupId: Number(option.groupId),
    groupName: option.groupName || '',
    name: option.name || '',
    price: Number(option.price || 0),
  }));
}
```

- [ ] **Step 4: Replace reorder loop with per-item reason reporting**

Replace `unavailable` and loop body with:

```js
const unavailable = [];
try {
  for (const item of order.value.items) {
    try {
      const product = await productStore.fetchById(item.productId);
      const variant = (product?.variants || []).find(v => v.variantId === item.variantId);
      const stock = variant?.quantityAvailable;
      if (!variant || variant.status !== 'AVAILABLE' || (stock !== null && stock !== undefined && Number(stock) < item.quantity)) {
        unavailable.push(`${item.productName}: phiên bản hoặc số lượng không còn khả dụng`);
        continue;
      }
      const modifiers = resolveReorderModifiers(product, item.modifiers);
      if (modifiers === null) {
        unavailable.push(`${item.productName}: tùy chọn không còn khả dụng`);
        continue;
      }
      await cart.addItem(item.productId, item.variantId, item.quantity, modifiers);
    } catch (error) {
      unavailable.push(`${item.productName}: ${error.message || 'không thể thêm vào giỏ'}`);
    }
  }
  if (unavailable.length) toast.error(`Không thể thêm ${unavailable.join('; ')}.`);
  if (unavailable.length < order.value.items.length) router.push('/cart');
} finally {
  reordering.value = false;
}
```

Render modifier snapshots beneath variant:

```vue
<ul v-if="item.modifiers.length" class="item-modifiers" aria-label="Tùy chọn món">
  <li v-for="modifier in item.modifiers" :key="modifier.modifierOptionId">
    {{ modifier.groupName ? `${modifier.groupName}: ` : '' }}{{ modifier.name }}
  </li>
</ul>
```

Add styles:

```css
.item-modifiers { margin: var(--space-1) 0 0; padding-left: var(--space-4); color: var(--text-mid); font-size: 12px; }
```

- [ ] **Step 5: Run GREEN modifier test**

Run: `npm test -- --test-name-pattern="reorder maps"`

Working directory: `Frontend`

Expected: PASS; all valid modifiers reach both guest-local and authenticated cart add contracts.

### Task 6: Replace Unsupported Delivery and Freeship Claims

**Files:**
- Modify: `Frontend/src/router/index.js:27-35`
- Modify: `Frontend/src/layouts/GuestLayout.vue:96-102`
- Modify: `Frontend/src/views/guest/ProductDetailPage.vue:1-212`
- Modify: `Frontend/src/views/guest/CartPage.vue:74-77`
- Modify: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: `storeApi.getConfig()` and optional `estimatedDeliveryMinutes: number` constrained by backend to 10–180.
- Produces: product benefit uses config value only after successful load; no freeship threshold claim because current public store config exposes no free-shipping threshold.

- [ ] **Step 1: Append failing truthful-claims test**

```js
test('customer surfaces do not hardcode 30-minute or freeship claims', () => {
  for (const source of [router, guestLayout, productDetail, cartPage]) {
    assert.doesNotMatch(source, /giao[^\n<]{0,40}30 phút/i);
  }
  assert.doesNotMatch(productDetail, /Miễn phí ship/);
  assert.match(productDetail, /storeApi\.getConfig\(\)/);
  assert.match(productDetail, /storeConfig\?\.estimatedDeliveryMinutes/);
  assert.match(cartPage, /Thời gian và phí giao hàng được tính sau khi bạn chọn địa chỉ nhận hàng\./);
});
```

- [ ] **Step 2: Run RED truthful-claims test**

Run: `npm test -- --test-name-pattern="hardcode 30-minute"`

Working directory: `Frontend`

Expected: FAIL on router description, footer, product benefit, and cart route note.

- [ ] **Step 3: Remove static claims from metadata, footer, and cart**

Change router Home description to:

```js
description: 'FastGuy — đặt đồ ăn nhanh trực tuyến với thực đơn đa dạng, combo tiết kiệm và nhiều ưu đãi hấp dẫn mỗi ngày.',
```

Change footer paragraph to:

```vue
<p>Đặt đồ ăn nhanh trực tuyến. Thực đơn đa dạng, thanh toán tiện lợi.</p>
```

Change cart route note to:

```vue
<section class="cart-block route-note">
  <i class="bi bi-geo-alt" aria-hidden="true"></i>
  <div><strong>Thông tin giao hàng</strong><p>Thời gian và phí giao hàng được tính sau khi bạn chọn địa chỉ nhận hàng.</p></div>
</section>
```

- [ ] **Step 4: Hydrate product claim from existing store config**

In `ProductDetailPage.vue`, add `storeApi` to existing `@/api` import, add:

```js
const storeConfig = ref(null);
```

In existing mount/load flow, make one independent config request:

```js
try {
  storeConfig.value = await storeApi.getConfig();
} catch {
  storeConfig.value = null;
}
```

Replace benefit grid with:

```vue
<div class="benefit-grid">
  <div v-if="storeConfig?.estimatedDeliveryMinutes">
    <i class="bi bi-clock-history" aria-hidden="true"></i>
    <span><strong>Dự kiến {{ storeConfig.estimatedDeliveryMinutes }} phút</strong><small>Thời gian thực tế xác nhận khi tính giao hàng</small></span>
  </div>
  <div>
    <i class="bi bi-truck" aria-hidden="true"></i>
    <span><strong>Phí giao hàng theo địa chỉ</strong><small>Hiển thị chính xác tại bước thanh toán</small></span>
  </div>
</div>
```

- [ ] **Step 5: Run GREEN truthful-claims test**

Run: `npm test -- --test-name-pattern="hardcode 30-minute"`

Working directory: `Frontend`

Expected: PASS; unavailable config hides time claim rather than inventing fallback.

### Task 7: Final Accessibility, Token, Coverage, and Build Verification

**Files:**
- Modify if needed: only files listed in File Map.
- Test: `Frontend/test/checkout-integration-ux.test.js`
- Test: `Frontend/test/customer-conversion-quick-wins.test.js`

**Interfaces:**
- Consumes: completed Tasks 1–6.
- Produces: all touched controls labeled, status announced, 44px controls, existing tokens only; full suite/build green.

- [ ] **Step 1: Add final source assertions for touched-screen accessibility/tokens**

Append:

```js
test('touched customer controls use labels status semantics and existing control token', () => {
  assert.match(profile, /role="status"/);
  assert.match(profile, /role="alert"/);
  assert.match(tracking, /role="status"/);
  assert.match(orderDetail, /aria-label="Tùy chọn món"/);
  assert.match(productDetail, /aria-hidden="true"/);
});
```

Append to checkout test:

```js
test('manual coupon controls retain 44px token hit area', () => {
  assert.match(checkout, /min-height: var\(--control-height\)/);
  assert.match(checkout, /<label for="checkout-coupon-code">/);
});
```

- [ ] **Step 2: Run focused RED/GREEN accessibility tests**

Run: `npm test -- --test-name-pattern="touched customer controls|44px token"`

Working directory: `Frontend`

Expected before final fixes: FAIL for any omitted semantic/token assertion. Apply only missing attributes/styles exactly named by assertion, then rerun same command; expected PASS.

- [ ] **Step 3: Run complete frontend tests**

Run: `npm test`

Working directory: `Frontend`

Expected: all Node tests PASS, zero failures.

- [ ] **Step 4: Run production build**

Run: `npm run build`

Working directory: `Frontend`

Expected: Vite exits code 0 and writes production bundle; no unresolved imports or Vue template compile errors.

- [ ] **Step 5: Confirm no unsupported backend work or dependencies entered diff**

Run: `git diff --name-only -- Backend Frontend/package.json Frontend/package-lock.json`

Working directory: repository root.

Expected: no output. If `Frontend/package*.json` or any `Backend/` file appears, revert only those plan-unapproved changes before continuing.

- [ ] **Step 6: Scan implementation for swallowed migration and unsupported claims**

Run: `rg -n "migrateToUser\(\)\.catch\(\(\) => \{\}\)|giao.{0,40}30 phút|Miễn phí ship|Cho đơn từ 50k" Frontend/src`

Working directory: repository root.

Expected: no matches.

- [ ] **Step 7: Review exact scope diff without committing**

Run: `git diff --check; git diff -- Frontend/src/stores/auth.js Frontend/src/views/user/CheckoutPage.vue Frontend/src/views/user/ProfilePage.vue Frontend/src/views/guest/LoginPage.vue Frontend/src/views/guest/TrackOrderPage.vue Frontend/src/views/user/OrderDetailPage.vue Frontend/src/router/index.js Frontend/src/layouts/GuestLayout.vue Frontend/src/views/guest/ProductDetailPage.vue Frontend/src/views/guest/CartPage.vue Frontend/test/checkout-integration-ux.test.js Frontend/test/customer-conversion-quick-wins.test.js`

Working directory: repository root.

Expected: `git diff --check` exits 0; diff contains only planned customer quick wins; no commit is created.

## Spec Coverage Matrix

- Manual coupon Guest/User: Task 1.
- Profile hydration from real API: Task 2.
- Guest cart migrate feedback: Task 3.
- Tracking ETA from backend data: Task 4.
- Reorder valid variant/modifiers and per-item invalid feedback: Task 5.
- Truthful freeship/delivery claims: Task 6.
- Accessibility and existing tokens only on touched screens: Tasks 1–7.
- No new backend where contract exists: File Map, Exact Existing Contracts, Task 7 Step 5.
- Tests/build pass: Task 7 Steps 2–4.
- No commit: Global Constraints and Task 7 Step 7.

## Self-Review Result

- Placeholder scan: không có marker chưa hoàn thành, hướng dẫn mơ hồ, undefined function, hoặc omitted error behavior.
- Type consistency: `hydrateProfile`, `formatEta`, `resolveReorderModifiers`, `modifierOptionId`, backend/normalized `estimatedDeliveryAt`, and `estimatedDeliveryMinutes` names match existing frontend/backend contracts; `Frontend/src/stores/order.js:117` normalization remains unchanged.
- Scope check: all seven requested customer quick-win concerns covered; delivery recovery, COD, refund, inventory, global redesign, and backend expansion excluded.
- Command check: frontend exposes `npm test` and `npm run build`; project exposes no lint/typecheck script, so no invented command included.
