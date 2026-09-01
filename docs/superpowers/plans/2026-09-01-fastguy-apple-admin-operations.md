# FastGuy Apple-Inspired Admin Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver one Apple-inspired FastGuy visual system across the admin shell, Dashboard summary cockpit, and full Orders workspace while preserving current contracts and operational behavior.

**Architecture:** Implement this as three frontend-only vertical deliverables. First refine shared admin tokens and shell. Then extend Dashboard presentation with a priority-order preview loaded from the existing contracted `GET /admin/orders` endpoint without writing into the shared Orders list. Finally redesign Orders and convert `AdminOrderDrawer` into a centered desktop modal/full-screen mobile sheet while retaining URL state, stale-request handling, allowed actions, and focus behavior.

**Tech Stack:** Vue 3 `<script setup>`, Pinia 3, Vue Router 4, Axios, Chart.js 4, Bootstrap Icons, Tailwind CSS v4 with `tw:` prefix, native CSS, Node test runner, Playwright Chromium desktop.

## Global Constraints

- Keep Be Vietnam Pro as the primary interface font.
- Use `#EEF1F5` canvas, `#FFFFFF` surface, `#182230` foreground, `#667085` muted, `#E4E7EC` border, `#F45B2A` brand, and semantic status colors through shared tokens.
- Expanded desktop sidebar is 248px; sticky topbar is 64px.
- Do not implement a collapsed desktop sidebar in this release.
- Dashboard remains the analytics summary; Orders remains the complete operational workspace.
- Do not invent trends, counts, notifications, global search behavior, fields, statuses, or state transitions.
- Use the existing `GET /admin/dashboard`, `GET /admin/orders`, and `GET /admin/orders/{id}` contracts; no DB, OpenAPI, or backend change is planned.
- Preserve loading, refresh, empty, filtered-empty, error, authorization, partial, stale-request, mutation, conflict, and success behavior.
- Modal width is 880px with `max-width: calc(100vw - 48px)` and `max-height: 85dvh`; mobile detail uses `100dvh`.
- Meet WCAG 2.2 AA: semantic controls, visible focus, focus containment/restoration, Escape handling, status not color-only, chart alternatives, reduced motion.
- Add no dependency. Tailwind utilities, if used, require the `tw:` prefix; no Preflight.
- Do not stage, delete, or alter protected untracked `.agents/skills/*`, `.hermes/`, `diagrams/`, or `wireframes/`.

## File Structure

- Modify `frontend/src/assets/styles/variables.css`: own shared admin semantic tokens only.
- Modify `frontend/src/layouts/AdminLayout.vue`: own sidebar/topbar structure, responsive drawer, account presentation, shell material.
- Create `frontend/tests/admin-shell-apple-operations.test.mjs`: source-level shell/token policy checks.
- Modify `frontend/src/views/admin/DashboardPage.vue`: own Dashboard layout, charts, attention list, priority-order preview and section states.
- Modify `frontend/src/utils/adminDashboardViewState.js`: only if the preview needs an independently testable view-state helper.
- Modify `frontend/tests/admin-dashboard-r3.test.mjs`: Dashboard structure and data-source policy.
- Modify `frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js`: rendered Dashboard and priority preview proof.
- Modify `frontend/src/views/admin/OrdersPage.vue`: own tabs, filters, table/cards, row opening and route state.
- Modify `frontend/src/utils/adminOrderWorkspace.js`: pure labels/filter presentation only.
- Modify `frontend/tests/admin-order-workspace.test.mjs`: pure workspace/filter policy.
- Modify `frontend/src/components/admin/AdminOrderDrawer.vue`: retain public props/events; change presentation to modal/sheet.
- Modify `frontend/tests/admin-order-drawer.test.mjs`: modal dimensions, focus, Escape and responsive policy.
- Modify `frontend/tests/e2e/admin-orders-r4.spec.js`: whole-row opening, centered modal, keyboard/focus and unchanged mutation requests.

---

### Task 1: Shared Apple-Inspired Admin Shell

**Files:**
- Modify: `frontend/src/layouts/AdminLayout.vue:152-287`
- Modify: `frontend/src/assets/styles/variables.css:14-26`
- Create: `frontend/tests/admin-shell-apple-operations.test.mjs`

**Interfaces:**
- Consumes: `auth.user`, `auth.logout()`, `route.meta.title`, existing `navigationGroups`, existing responsive drawer methods.
- Produces: shared tokens `--admin-canvas`, `--admin-surface`, `--admin-foreground`, `--admin-muted`, `--admin-subtle`, `--admin-border`, `--admin-brand`, `--admin-brand-dark`, `--admin-brand-soft`; unchanged `<router-view />` shell contract.

- [ ] **Step 1: Locate the authoritative token stylesheet**

Run:

```powershell
rg --line-number --glob "*.css" --glob "*.vue" -- "--admin-canvas|--admin-brand" frontend/src
```

Expected: one authoritative global token definition plus component consumers. Select that existing stylesheet; do not create a second token source.

- [ ] **Step 2: Write the failing shell policy test**

Create `frontend/tests/admin-shell-apple-operations.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const layout = readFileSync(new URL('../src/layouts/AdminLayout.vue', import.meta.url), 'utf8');
const variables = readFileSync(new URL('../src/assets/styles/variables.css', import.meta.url), 'utf8');

test('admin shell uses the approved fixed geometry and FastGuy material', () => {
  assert.match(layout, /\.sidebar\{width:248px\}/);
  assert.match(layout, /\.main-content\{min-width:0;margin-left:248px\}/);
  assert.match(layout, /\.topbar\{height:64px/);
  assert.match(layout, /backdrop-filter:blur\(/);
  assert.match(layout, /FastGuy/);
});

test('admin tokens expose the approved Apple-inspired FastGuy palette', () => {
  for (const value of ['#EEF1F5', '#FFFFFF', '#182230', '#667085', '#98A2B3', '#E4E7EC', '#F45B2A', '#D9481C', '#FFF0EA']) {
    assert.match(variables.toUpperCase(), new RegExp(value));
  }
});

test('responsive navigation retains dialog semantics and focus recovery', () => {
  for (const source of ['role="dialog"', 'aria-modal', 'event.key === \'Escape\'', 'triggerToRestore', 'backgroundInert']) assert.match(layout, new RegExp(source));
});
```

- [ ] **Step 3: Run the test and confirm red**

Run:

```powershell
node --test tests/admin-shell-apple-operations.test.mjs
```

Working directory: `frontend`.

Expected: FAIL because the topbar remains 60px and approved palette/material assertions are not all present.

- [ ] **Step 4: Implement minimal shared tokens and shell presentation**

Update the existing token source to these semantic values:

```css
--admin-canvas:#EEF1F5;
--admin-surface:#FFFFFF;
--admin-foreground:#182230;
--admin-muted:#667085;
--admin-subtle:#98A2B3;
--admin-border:#E4E7EC;
--admin-brand:#F45B2A;
--admin-brand-dark:#D9481C;
--admin-brand-soft:#FFF0EA;
```

In `AdminLayout.vue`, preserve all drawer logic and navigation groups. Change only shell presentation:

```css
.sidebar{width:248px;background:rgba(255,255,255,.92)}
.main-content{min-width:0;margin-left:248px;background:var(--admin-canvas)}
.topbar{height:64px;border-bottom:1px solid var(--admin-border);background:rgba(255,255,255,.78);backdrop-filter:blur(20px)}
.sidebar-nav a.router-link-active{position:relative;color:var(--admin-brand-dark);background:var(--admin-brand-soft)}
.sidebar-nav a.router-link-active::before{position:absolute;inset:8px auto 8px 0;width:3px;border-radius:99px;background:var(--admin-brand);content:""}
```

Keep website and logout controls truthful. Do not add a search box, notification bell, or collapsed state.

- [ ] **Step 5: Run focused and full frontend checks**

Run:

```powershell
node --test tests/admin-shell-apple-operations.test.mjs
npm test
npm run build
```

Expected: all pass.

- [ ] **Step 6: Commit shell deliverable**

```powershell
git add frontend/src/layouts/AdminLayout.vue frontend/src/assets/styles/variables.css frontend/tests/admin-shell-apple-operations.test.mjs
git diff --cached --check
git commit -m "feat(admin): refine operations shell"
```

---

### Task 2: Dashboard Priority Preview Data Flow

**Files:**
- Modify: `frontend/src/views/admin/DashboardPage.vue:1-113`
- Modify: `frontend/tests/admin-dashboard-r3.test.mjs`
- Modify: `frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js`

**Interfaces:**
- Consumes: `adminStore.fetchDashboard()`, `adminApi.getOrders({ attentionOnly: true, sort: 'WAITING_DESC', page: 1, pageSize: 8 })`, existing paginated `{ items, pagination }` envelope.
- Produces: local `priorityOrders`, `priorityState`, `priorityError`; does not mutate `adminStore.allOrders`.

- [ ] **Step 1: Add failing Dashboard source-policy assertions**

Append to `frontend/tests/admin-dashboard-r3.test.mjs`:

```js
test('dashboard priority preview uses the contracted order queue without polluting Orders store state', () => {
  assert.match(dashboard, /adminApi\.getOrders/);
  assert.match(dashboard, /attentionOnly:\s*true/);
  assert.match(dashboard, /sort:\s*'WAITING_DESC'/);
  assert.match(dashboard, /pageSize:\s*8/);
  assert.match(dashboard, /priorityOrders/);
  assert.doesNotMatch(dashboard, /adminStore\.fetchOrders/);
  assert.match(dashboard, /Đơn cần ưu tiên/);
  assert.match(dashboard, /Xem tất cả/);
});
```

- [ ] **Step 2: Run focused test and confirm red**

Run:

```powershell
node --test tests/admin-dashboard-r3.test.mjs
```

Expected: FAIL on missing `adminApi.getOrders`, `priorityOrders`, and preview copy.

- [ ] **Step 3: Implement independent preview loading**

In `DashboardPage.vue`, import existing interfaces and add local state:

```js
import { useRouter } from 'vue-router';
import { adminApi } from '@/api';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { formatPrice, formatDate } from '@/utils/format';

const router = useRouter();
const priorityOrders = ref([]);
const priorityState = ref('loading');
const priorityError = ref('');
let priorityRequestGeneration = 0;

async function loadPriorityOrders() {
  const generation = ++priorityRequestGeneration;
  priorityState.value = 'loading';
  priorityError.value = '';
  try {
    const result = await adminApi.getOrders({ attentionOnly: true, sort: 'WAITING_DESC', page: 1, pageSize: 8 });
    if (stopped || generation !== priorityRequestGeneration) return;
    priorityOrders.value = Array.isArray(result?.items) ? result.items : Array.isArray(result) ? result.slice(0, 8) : [];
    priorityState.value = 'ready';
  } catch (error) {
    if (stopped || generation !== priorityRequestGeneration) return;
    priorityError.value = error.message || 'Không thể tải đơn cần ưu tiên.';
    priorityState.value = 'error';
  }
}
```

Load Dashboard and preview independently with `Promise.allSettled([loadDashboard(), loadPriorityOrders()])`; retain valid Dashboard sections if preview fails. Increment `priorityRequestGeneration` on unmount.

- [ ] **Step 4: Render preview states and exact navigation**

Add after the analytics grid:

```vue
<section class="panel priority-orders-panel" aria-labelledby="priority-orders-title">
  <header>
    <div><h2 id="priority-orders-title">Đơn cần ưu tiên</h2><p>Xếp theo thời gian chờ và ngoại lệ vận hành</p></div>
    <router-link :to="{ path: '/admin/orders', query: { status: 'ATTENTION' } }">Xem tất cả</router-link>
  </header>
  <div v-if="priorityState === 'loading'" class="priority-order-skeleton" role="status">Đang tải đơn cần ưu tiên...</div>
  <div v-else-if="priorityState === 'error'" class="priority-order-error" role="alert"><span>{{ priorityError }}</span><button type="button" @click="loadPriorityOrders">Thử lại</button></div>
  <div v-else-if="priorityOrders.length" class="priority-order-list">
    <button v-for="order in priorityOrders" :key="order.orderId" type="button" @click="router.push({ path: '/admin/orders', query: { status: 'ATTENTION', orderId: order.orderId } })">
      <span><strong>{{ order.orderCode }}</strong><small>{{ order.customerName || 'Khách hàng' }}</small></span>
      <span>{{ order.waitingMinutes }} phút</span>
      <strong>{{ formatPrice(order.finalAmount || 0) }}</strong>
      <OrderStatusBadge :status="order.status" />
    </button>
  </div>
  <p v-else class="empty">Không có đơn cần ưu tiên.</p>
</section>
```

Do not open a second Dashboard-owned detail modal. Route to Orders and let Orders own detail loading.

- [ ] **Step 5: Extend mocked Dashboard E2E setup**

In `admin-dashboard-balanced-cockpit.spec.js`, add an Orders route that accepts exactly:

```js
[['attentionOnly', 'true'], ['page', '1'], ['pageSize', '8'], ['sort', 'WAITING_DESC']]
```

Return two contracted list rows. Assert:

```js
await expect(page.getByRole('heading', { name: 'Đơn cần ưu tiên' })).toBeVisible();
await expect(page.getByText('FG-PRIORITY-01')).toBeVisible();
await page.getByRole('button', { name: /FG-PRIORITY-01/ }).click();
await expect(page).toHaveURL(/\/admin\/orders\?status=ATTENTION&orderId=1|\/admin\/orders\?orderId=1&status=ATTENTION/);
```

- [ ] **Step 6: Run focused unit and desktop E2E checks**

Run:

```powershell
node --test tests/admin-dashboard-r3.test.mjs tests/admin-dashboard-stock-policy.test.mjs tests/admin-dashboard-operational-policy.test.mjs
npx playwright test tests/e2e/admin-dashboard-balanced-cockpit.spec.js --project=desktop-chrome --config=playwright.config.js
```

Expected: all pass; zero page/console errors; one Dashboard request and one priority Orders request.

- [ ] **Step 7: Commit Dashboard data-flow deliverable**

```powershell
git add frontend/src/views/admin/DashboardPage.vue frontend/tests/admin-dashboard-r3.test.mjs frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js
git diff --cached --check
git commit -m "feat(admin): add priority order preview"
```

---

### Task 3: Apple-Inspired Dashboard Presentation

**Files:**
- Modify: `frontend/src/views/admin/DashboardPage.vue:115-141`
- Modify: `frontend/tests/admin-dashboard-r3.test.mjs`
- Modify: `frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js`

**Interfaces:**
- Consumes: unchanged Dashboard response fields and Task 2 `priorityOrders` state.
- Produces: approved 12-column responsive layout and accessible chart alternatives.

- [ ] **Step 1: Strengthen failing layout policy test**

Add assertions:

```js
test('dashboard uses the approved Apple-inspired hierarchy without decorative fake data', () => {
  for (const className of ['dashboard-hero', 'metric-card', 'revenue-panel', 'attention-panel', 'status-panel', 'products-panel', 'stock-panel', 'priority-orders-panel']) {
    assert.match(dashboard, new RegExp(className));
  }
  assert.match(dashboard, /grid-template-columns:repeat\(12,minmax\(0,1fr\)\)/);
  assert.doesNotMatch(dashboard, /sparkline|fakeTrend|notification-bell|global-search/);
});
```

- [ ] **Step 2: Run and confirm red**

Run:

```powershell
node --test tests/admin-dashboard-r3.test.mjs
```

Expected: FAIL until the approved class hierarchy replaces current compact names.

- [ ] **Step 3: Refine Dashboard template and CSS**

Implement:

- Hero: one operational eyebrow, heading, date/refresh state, “Xem đơn cần xử lý” route.
- Four cards: label/value only; comparison area omitted because current contract has no comparison period.
- Grid spans: revenue 8, attention 4, status 4, products 5, stock 3.
- Priority preview full width below analytics.
- `border-radius:17px`, one-pixel borders, subtle shadows, no nested generic cards.
- Topbar/modal blur only; Dashboard cards remain opaque white.
- Chart animation remains disabled for reduced motion.

Add an adjacent visually available or disclosed data table/list for each chart, e.g.:

```vue
<details class="chart-data">
  <summary>Xem dữ liệu biểu đồ</summary>
  <table><thead><tr><th>Ngày</th><th>Doanh thu</th></tr></thead><tbody><tr v-for="row in revenueSeries" :key="row.date"><td>{{ row.date }}</td><td>{{ formatPrice(row.revenue) }}</td></tr></tbody></table>
</details>
```

Create equivalent lists/tables for status and top products.

- [ ] **Step 4: Add visual and accessibility E2E assertions**

Assert the primary action, chart data disclosure, and priority section are keyboard reachable. At desktop viewport, assert panel bounding boxes establish 8/4 and 4/5/3 rows without horizontal overflow.

- [ ] **Step 5: Run Dashboard checks**

```powershell
node --test tests/admin-dashboard-r3.test.mjs tests/admin-dashboard-stock-policy.test.mjs tests/admin-dashboard-operational-policy.test.mjs
npx playwright test tests/e2e/admin-dashboard-balanced-cockpit.spec.js --project=desktop-chrome --config=playwright.config.js
npm run build
```

Expected: all pass.

- [ ] **Step 6: Commit Dashboard visual deliverable**

```powershell
git add frontend/src/views/admin/DashboardPage.vue frontend/tests/admin-dashboard-r3.test.mjs frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js
git diff --cached --check
git commit -m "feat(admin): polish dashboard operations cockpit"
```

---

### Task 4: Orders Tabs, Compact Filters, and Whole-Row Interaction

**Files:**
- Modify: `frontend/src/views/admin/OrdersPage.vue:24-396`
- Modify: `frontend/src/utils/adminOrderWorkspace.js`
- Modify: `frontend/tests/admin-order-workspace.test.mjs`
- Modify: `frontend/tests/admin-orders-r4.test.mjs`
- Modify: `frontend/tests/e2e/admin-orders-r4.spec.js`

**Interfaces:**
- Consumes: existing route query keys `status`, `search`, `paymentStatus`, `refundStatus`, `sort`, `fromDate`, `toDate`, `page`; existing `adminStore.fetchOrders()`.
- Produces: unchanged query/API params; row action `openOrder(order, event)`; optional `route.query.orderId` detail deep-link consumed by Task 5.

- [ ] **Step 1: Add failing policy tests for the compact workspace**

Add source assertions to `admin-orders-r4.test.mjs`:

```js
assert.match(ordersPage, /class="filter-toolbar"/);
assert.match(ordersPage, /class="advanced-filter-panel"/);
assert.match(ordersPage, /class="active-filters"/);
assert.match(ordersPage, /class="order-row-trigger"/);
assert.match(ordersPage, /@keydown\.enter="openOrder/);
assert.match(ordersPage, /position:sticky/);
assert.doesNotMatch(ordersPage, /order-shortcuts/);
```

The last assertion removes the four low-value shortcut cards, matching the approved data-first workspace.

- [ ] **Step 2: Run focused tests and confirm red**

```powershell
node --test tests/admin-order-workspace.test.mjs tests/admin-orders-r4.test.mjs
```

Expected: FAIL on old shortcuts/filter structure and missing row trigger.

- [ ] **Step 3: Remove shortcut cards; preserve status tabs**

Delete only the `ORDER_SHORTCUTS` import and `<nav class="order-shortcuts">`. Keep `PRIMARY_ORDER_STATUSES`, `OTHER_ORDER_STATUSES`, keyboard navigation, and exact URL behavior.

Do not add counts because the current list response does not provide per-status batch counts for all tabs in a stable frontend interface.

- [ ] **Step 4: Convert filters into visible toolbar plus advanced date panel**

Keep all existing fields and `applyFilters()` logic. Change presentation to:

```vue
<section ref="filterPanelRef" class="filter-toolbar" tabindex="-1" aria-labelledby="order-filter-title">
  <h2 id="order-filter-title" class="sr-only">Bộ lọc đơn hàng</h2>
  <!-- search, payment, refund, sort -->
  <button type="button" class="advanced-filter-trigger" :aria-expanded="advancedFiltersOpen" @click="advancedFiltersOpen = !advancedFiltersOpen">Bộ lọc</button>
  <button type="button" class="btn btn-primary" @click="applyFilters">Áp dụng</button>
</section>
<section v-if="advancedFiltersOpen" class="advanced-filter-panel" aria-label="Bộ lọc ngày đặt">
  <!-- existing presets, from/to labels, validation and apply button -->
</section>
```

Keep `activeOrderFilterChips()` and individual removal. Rename visible reset copy to “Xóa tất cả” without changing `resetFilters()` behavior.

- [ ] **Step 5: Make the desktop row keyboard-openable without breaking nested controls**

Use a focusable row only as the row-opening surface:

```vue
<tr
  v-for="order in paged"
  :key="order.orderId"
  class="order-row-trigger"
  :class="{ attention: order.attentionReasons?.length }"
  tabindex="0"
  :aria-label="`Xem chi tiết đơn hàng ${order.orderCode}`"
  @click="openOrderFromRow(order, $event)"
  @keydown.enter="openOrder(order, $event)"
  @keydown.space.prevent="openOrder(order, $event)"
>
```

Add:

```js
function openOrderFromRow(order, event) {
  if (event.target.closest('button,a,input,select,textarea')) return;
  openOrder(order, event);
}
```

Keep the order-code button as an explicit accessible action. `openOrder()` must store the row or button as `detailTrigger` for restoration.

- [ ] **Step 6: Implement sticky and visual hierarchy CSS**

- Toolbar sticky beneath 64px topbar.
- Table header sticky beneath topbar plus toolbar.
- Full-row hover/focus background `#FFF8F5`.
- Left rail uses brand for selected/focus context; warning rail remains semantic for attention.
- Numeric values use tabular figures.
- Mobile cards preserve order code, customer, wait, item count, total, payment, status, attention reasons, and detail action.

- [ ] **Step 7: Update E2E expectations**

Retain all exact query assertions in `admin-orders-r4.spec.js`. Add:

```js
const row = page.locator('tr.order-row-trigger', { hasText: 'FG-0009' });
await row.focus();
await page.keyboard.press('Enter');
await expect(page.getByRole('dialog', { name: 'FG-0009' })).toBeVisible();
```

Run desktop only per current project policy; remove no mobile production CSS.

- [ ] **Step 8: Run focused checks**

```powershell
node --test tests/admin-order-workspace.test.mjs tests/admin-orders-r4.test.mjs tests/admin-order-drawer.test.mjs
npx playwright test tests/e2e/admin-orders-r4.spec.js --project=desktop-chrome --config=playwright.config.js
```

Expected: all pass; exact API request evidence unchanged.

- [ ] **Step 9: Commit Orders workspace deliverable**

```powershell
git add frontend/src/views/admin/OrdersPage.vue frontend/src/utils/adminOrderWorkspace.js frontend/tests/admin-order-workspace.test.mjs frontend/tests/admin-orders-r4.test.mjs frontend/tests/e2e/admin-orders-r4.spec.js
git diff --cached --check
git commit -m "feat(admin): streamline order operations workspace"
```

---

### Task 5: Centered Order Modal and Dashboard Deep-Link

**Files:**
- Modify: `frontend/src/components/admin/AdminOrderDrawer.vue:1-136`
- Modify: `frontend/src/views/admin/OrdersPage.vue:207-305,358`
- Modify: `frontend/tests/admin-order-drawer.test.mjs`
- Modify: `frontend/tests/e2e/admin-orders-r4.spec.js`

**Interfaces:**
- Consumes: unchanged component props `open`, `loading`, `order`, `error`, `busy`, `actionError`, `actionMessage`, `pendingAction`, `actionNote`; unchanged emitted events.
- Produces: centered desktop modal/full-screen mobile sheet; consumes optional numeric `route.query.orderId`; removes `orderId` query on close while preserving all other filters.

- [ ] **Step 1: Change the failing drawer policy test to the approved modal contract**

Replace drawer geometry assertions with:

```js
assert.match(drawer, /class="order-modal-backdrop"/);
assert.match(drawer, /class="order-modal"/);
assert.match(drawer, /width:880px/);
assert.match(drawer, /max-width:calc\(100vw - 48px\)/);
assert.match(drawer, /max-height:85dvh/);
assert.match(drawer, /@media\(max-width:640px\)/);
assert.match(drawer, /height:100dvh/);
assert.doesNotMatch(drawer, /justify-content:flex-end/);
```

Keep existing focus, Escape, body overflow, allowed-action, conflict, and canonical-reload assertions.

- [ ] **Step 2: Run and confirm red**

```powershell
node --test tests/admin-order-drawer.test.mjs
```

Expected: FAIL because presentation is still right-side drawer.

- [ ] **Step 3: Convert presentation while preserving behavior and public API**

Rename CSS/template classes from drawer to modal, but do not rename component file or props/events in this task. Use:

```css
.order-modal-backdrop{position:fixed;z-index:120;inset:0;display:grid;place-items:center;padding:24px;background:rgba(24,34,48,.34);backdrop-filter:blur(10px)}
.order-modal{display:grid;grid-template-rows:auto minmax(0,1fr) auto;width:880px;max-width:calc(100vw - 48px);max-height:85dvh;border:1px solid rgba(255,255,255,.9);border-radius:22px;background:rgba(255,255,255,.96);box-shadow:0 28px 80px rgba(24,34,48,.22);overflow:hidden}
@media(max-width:640px){.order-modal-backdrop{padding:0}.order-modal{width:100%;max-width:none;height:100dvh;max-height:none;border-radius:0}}
```

Use a two-column `.modal-content-grid` for facts/items/payment and timeline at desktop; collapse to one column below 768px. Keep header/footer sticky by the existing grid rows.

- [ ] **Step 4: Support Dashboard-to-Orders detail deep-link**

In `OrdersPage.vue`, after list data loads, parse only a positive integer:

```js
function routeOrderId() {
  const value = Number.parseInt(route.query.orderId, 10);
  return Number.isInteger(value) && value > 0 ? value : null;
}
```

After `loadOrders()` completes, if `routeOrderId()` exists and no matching detail request is active, call `loadOrderDetail(id)`. Do not require the order to exist on the current page.

On close:

```js
router.replace({ query: { ...route.query, orderId: undefined } });
```

Preserve all status/filter/page keys and focus restoration when a local trigger exists.

- [ ] **Step 5: Add E2E deep-link and focus checks**

Add a test that opens `/admin/orders?status=ATTENTION&orderId=9`, confirms one detail request and visible dialog, presses Escape, confirms the dialog closes and URL remains `/admin/orders?status=ATTENTION`.

In the row-open test, after closing, assert the originating row/button is focused.

- [ ] **Step 6: Run modal and Orders checks**

```powershell
node --test tests/admin-order-drawer.test.mjs tests/admin-order-workspace.test.mjs tests/admin-orders-r4.test.mjs
npx playwright test tests/e2e/admin-orders-r4.spec.js --project=desktop-chrome --config=playwright.config.js
```

Expected: all pass; exact mutation payload tests remain unchanged.

- [ ] **Step 7: Commit modal deliverable**

```powershell
git add frontend/src/components/admin/AdminOrderDrawer.vue frontend/src/views/admin/OrdersPage.vue frontend/tests/admin-order-drawer.test.mjs frontend/tests/e2e/admin-orders-r4.spec.js
git diff --cached --check
git commit -m "feat(admin): center order detail workflow"
```

---

### Task 6: Integrated Visual and Regression Gate

**Files:**
- Modify only if a failing check identifies a scoped defect in files from Tasks 1–5.
- Test: all frontend tests and focused desktop Playwright specs.

**Interfaces:**
- Consumes: completed shell, Dashboard, Orders, modal deliverables.
- Produces: verified frontend build with no API/DB change.

- [ ] **Step 1: Run full frontend unit suite**

```powershell
npm test
```

Expected: all tests pass. Record exact pass count.

- [ ] **Step 2: Run production build**

```powershell
npm run build
```

Expected: exit 0; no unresolved imports or CSS build errors.

- [ ] **Step 3: Run focused desktop Chromium flows**

```powershell
npx playwright test tests/e2e/admin-dashboard-balanced-cockpit.spec.js tests/e2e/admin-orders-r4.spec.js --project=desktop-chrome --config=playwright.config.js
```

Expected: all pass; no uncaught page errors, console errors, failed requests, or unexpected HTTP responses.

- [ ] **Step 4: Inspect responsive production behavior manually with Playwright**

At desktop 1440×1000 and mobile 390×844, verify:

- No unintended horizontal overflow.
- Sidebar/topbar remain usable.
- Dashboard panels stack in decision order.
- Orders desktop table becomes mobile cards below 768px.
- Modal becomes full-screen sheet on mobile.
- Focus remains visible.
- Reduced-motion emulation disables chart and entry animation.

No new mobile automated project gate is required.

- [ ] **Step 5: Run repository integrity checks**

```powershell
git diff --check
git status --short
git diff --stat
git log --oneline -10
```

Expected: no whitespace errors; only intended tracked changes/commits plus protected untracked artifacts.

- [ ] **Step 6: Commit only scoped verification fixes, if any**

If no fixes were needed, do not create an empty commit. If scoped fixes were required:

Stage only the exact scoped files changed while fixing this gate, inspect the staged list, then commit:

```powershell
git status --short
git add frontend/src/layouts/AdminLayout.vue frontend/src/assets/styles/variables.css frontend/src/views/admin/DashboardPage.vue frontend/src/views/admin/OrdersPage.vue frontend/src/components/admin/AdminOrderDrawer.vue frontend/src/utils/adminOrderWorkspace.js frontend/tests/admin-shell-apple-operations.test.mjs frontend/tests/admin-dashboard-r3.test.mjs frontend/tests/admin-order-workspace.test.mjs frontend/tests/admin-orders-r4.test.mjs frontend/tests/admin-order-drawer.test.mjs frontend/tests/e2e/admin-dashboard-balanced-cockpit.spec.js frontend/tests/e2e/admin-orders-r4.spec.js
git diff --cached --name-only
git diff --cached --check
git commit -m "fix(admin): close operations UI regressions"
```

Before committing, unstage every listed file that was not actually changed by the scoped fix.

- [ ] **Step 7: Stop without push**

Report exact test/build/E2E evidence. Do not push until explicitly requested.
