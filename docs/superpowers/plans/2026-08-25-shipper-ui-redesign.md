# Shipper UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign toàn bộ giao diện Shipper theo hướng Field Command, mobile-first và desktop sidebar, không đổi nghiệp vụ hoặc API.

**Architecture:** Giữ nguyên router, Pinia store và API clients. Thay đổi markup/CSS tại layout, views và action sheet; thêm một file test source-level khóa các invariant UI/accessibility, sau đó thêm Playwright cho navigation và responsive shell.

**Tech Stack:** Vue 3 Composition API, Vue Router, Pinia, CSS scoped, Node test runner, Playwright.

## Global Constraints

- Không thay đổi database, backend, API contract, request, response hoặc endpoint.
- Không thêm dependency.
- Mobile 320-480px; touch target tối thiểu 44x44px; WCAG 2.2 AA.
- Mobile dùng năm tab: Trang chủ, Đơn, Lịch sử, COD, Hồ sơ; Ca làm mở từ Dashboard.
- Desktop dùng sidebar cố định đủ sáu mục.
- Giữ polling, generation guards, cleanup, COD validation, expected status/version và xử lý HTTP 409.
- Ưu tiên CSS/markup; JavaScript mới chỉ dùng dữ liệu hiện có.

## File Map

- `frontend/src/layouts/ShipperLayout.vue`: Field Command shell, mobile bottom navigation, desktop sidebar.
- `frontend/src/views/shipper/DashboardPage.vue`: shift summary, task priority, operational metrics.
- `frontend/src/views/shipper/MyOrdersPage.vue`: active/history toolbar, cards, quick actions, pagination.
- `frontend/src/views/shipper/OrderDetailPage.vue`: information hierarchy, sticky actions, failure/issue dialogs.
- `frontend/src/components/shipper/OrderActionSheet.vue`: mobile quick-action presentation.
- `frontend/src/components/common/ShiftStatus.vue`: shared shift command card.
- `frontend/src/views/shipper/ShipperShiftsPage.vue`: today/upcoming/history hierarchy.
- `frontend/src/views/shipper/CashPage.vue`: COD summary, submission, settlement history.
- `frontend/src/views/user/ProfilePage.vue`: reused Shipper profile presentation; changes must remain role-safe.
- `frontend/test/shipper-field-command-ui.test.js`: UI contract regression checks.
- `frontend/tests/e2e/shipper-field-command.spec.js`: desktop/mobile critical navigation checks.

---

### Task 1: Field Command Shell

**Files:**
- Modify: `frontend/src/layouts/ShipperLayout.vue`
- Test: `frontend/test/shipper-field-command-ui.test.js`

**Interfaces:**
- Consumes: existing `shiftApi.getCurrent()`, `staff-shift-changed`, Vue Router routes.
- Produces: `.shipper-sidebar`, `.shipper-bottom-nav`, `desktopNavItems`, `mobileNavItems` presentation contract.

- [ ] **Step 1: Write the failing shell test**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('shipper shell exposes five mobile destinations and full desktop sidebar', () => {
  const layout = read('../src/layouts/ShipperLayout.vue');
  assert.match(layout, /mobileNavItems/);
  assert.match(layout, /shipper-bottom-nav/);
  assert.match(layout, /shipper-sidebar/);
  assert.match(layout, /aria-current/);
  assert.match(layout, /min-height:\s*44px/);
  assert.match(layout, /@media\s*\(min-width:\s*900px\)/);
});
```

- [ ] **Step 2: Run test and verify failure**

Run: `node --test test/shipper-field-command-ui.test.js`
Expected: FAIL because the new shell selectors do not exist.

- [ ] **Step 3: Implement the shell**

Define `mobileNavItems` by filtering Ca làm from the existing six destinations. Render desktop sidebar and mobile bottom navigation from separate arrays. Preserve `checkShift`, route visibility, logout and shift banners. Add `:aria-current="activeClass(item.path) ? 'page' : undefined"`; desktop breakpoint `900px`; all navigation/logout controls at least 44px.

- [ ] **Step 4: Run shell test**

Run: `node --test test/shipper-field-command-ui.test.js`
Expected: PASS.

### Task 2: Dashboard And Order Lists

**Files:**
- Modify: `frontend/src/views/shipper/DashboardPage.vue`
- Modify: `frontend/src/views/shipper/MyOrdersPage.vue`
- Modify: `frontend/src/components/shipper/OrderActionSheet.vue`
- Test: `frontend/test/shipper-field-command-ui.test.js`

**Interfaces:**
- Consumes: `store.dashboard`, `store.activeOrders`, `store.historyOrders`, existing mutation methods.
- Produces: `.operations-grid`, `.priority-order`, `.order-toolbar`, `.order-card-actions` UI contracts.

- [ ] **Step 1: Add failing dashboard/list assertions**

```js
test('shipper dashboard and order list expose field actions without changing data flow', () => {
  const dashboard = read('../src/views/shipper/DashboardPage.vue');
  const orders = read('../src/views/shipper/MyOrdersPage.vue');
  const sheet = read('../src/components/shipper/OrderActionSheet.vue');
  assert.match(dashboard, /operations-grid/);
  assert.match(dashboard, /priority-order/);
  assert.match(dashboard, /to="\/shipper\/shifts"/);
  assert.match(orders, /order-toolbar/);
  assert.match(orders, /order-card-actions/);
  assert.match(sheet, /safe-area-inset-bottom/);
  assert.match(sheet, /props\.order\.status/);
});
```

- [ ] **Step 2: Run test and verify failure**

Run: `node --test test/shipper-field-command-ui.test.js`
Expected: FAIL on new class contracts.

- [ ] **Step 3: Restyle dashboard and lists**

Use existing computed fields only. Dashboard order: heading, shift command, operational metrics, priority order, Ca làm shortcut. Lists retain tabs/search/sort/date filters/pagination/polling; reorganize controls into `.order-toolbar`; cards retain semantic router link plus separate Gọi/Bản đồ/primary actions. Action sheet retains focus lifecycle, validation, confirmation and conflict handling.

- [ ] **Step 4: Run focused tests**

Run: `node --test test/shipper-field-command-ui.test.js test/shipper-optimistic-conflict.test.js`
Expected: PASS.

### Task 3: Order Detail Command Surface

**Files:**
- Modify: `frontend/src/views/shipper/OrderDetailPage.vue`
- Test: `frontend/test/shipper-field-command-ui.test.js`

**Interfaces:**
- Consumes: existing order DTO, `pickUpOrder`, `deliverOrder`, failure and issue commands.
- Produces: `.order-command-header`, `.order-detail-grid`, `.sticky-command-bar` presentation contracts.

- [ ] **Step 1: Add failing detail assertions**

```js
test('shipper order detail keeps mutation guards inside a sticky command surface', () => {
  const detail = read('../src/views/shipper/OrderDetailPage.vue');
  assert.match(detail, /order-command-header/);
  assert.match(detail, /order-detail-grid/);
  assert.match(detail, /sticky-command-bar/);
  assert.match(detail, /safe-area-inset-bottom/);
  assert.match(detail, /order\.value\.status/);
  assert.match(detail, /error\?\.status === 409/);
  assert.match(detail, /role="dialog"/);
});
```

- [ ] **Step 2: Run test and verify failure**

Run: `node --test test/shipper-field-command-ui.test.js`
Expected: FAIL on detail selectors.

- [ ] **Step 3: Reorganize detail markup and CSS**

Group header/status, customer/address actions, items, payment/COD, notes and timeline. Move successful pickup/delivery CTA to `.sticky-command-bar`; keep failure action visually separate. Preserve dialog refs, focus trap, Escape, restore focus, exact COD check, checked-in gating and all mutation methods unchanged.

- [ ] **Step 4: Run focused tests**

Run: `node --test test/shipper-field-command-ui.test.js test/shipper-optimistic-conflict.test.js test/shipper-delivery-ux.test.js`
Expected: PASS; if `test/shipper-delivery-ux.test.js` is absent, run the two existing files only.

### Task 4: Shifts, COD And Profile

**Files:**
- Modify: `frontend/src/components/common/ShiftStatus.vue`
- Modify: `frontend/src/views/shipper/ShipperShiftsPage.vue`
- Modify: `frontend/src/views/shipper/CashPage.vue`
- Modify: `frontend/src/views/user/ProfilePage.vue` only if role-scoped classes can avoid customer regressions.
- Test: `frontend/test/shipper-field-command-ui.test.js`

**Interfaces:**
- Consumes: current shift/COD/profile APIs and events.
- Produces: `.shift-command`, `.shift-sections`, `.cod-command-summary`, role-safe profile shell.

- [ ] **Step 1: Add failing operational-page assertions**

```js
test('shipper shift and COD pages use command hierarchy and preserve irreversible warnings', () => {
  const shift = read('../src/components/common/ShiftStatus.vue');
  const shifts = read('../src/views/shipper/ShipperShiftsPage.vue');
  const cash = read('../src/views/shipper/CashPage.vue');
  assert.match(shift, /shift-command/);
  assert.match(shifts, /shift-sections/);
  assert.match(cash, /cod-command-summary/);
  assert.match(cash, /Số tiền không thể sửa sau khi gửi/);
  assert.match(cash, /role="alert"/);
});
```

- [ ] **Step 2: Run test and verify failure**

Run: `node --test test/shipper-field-command-ui.test.js`
Expected: FAIL on new selectors.

- [ ] **Step 3: Restyle shifts, COD and profile**

Make current shift the dominant command card; retain check-in/out events and guards. Reorder shifts as today, upcoming, history. Keep COD three-value summary, irreversible warning, form eligibility, conflict and history. For shared `ProfilePage.vue`, add only role-scoped wrapper/classes using current auth role; do not remove customer features or alter profile requests.

- [ ] **Step 4: Run focused tests**

Run: `node --test test/shipper-field-command-ui.test.js test/cod-settlement-ui.test.js test/cod-settlement-conflict.test.js`
Expected: PASS.

### Task 5: Full Verification And Browser QA

**Files:**
- Create: `frontend/tests/e2e/shipper-field-command.spec.js`
- Verify: all modified frontend files.

**Interfaces:**
- Consumes: deployed local frontend/backend and a valid Shipper test session supplied through the existing E2E auth convention.
- Produces: desktop/mobile navigation and responsive regression coverage.

- [ ] **Step 1: Write Playwright checks**

Create tests using the repository's existing authentication helper/config. Verify at desktop `1440x900`: sidebar visible, six destinations available, bottom nav hidden. Verify at mobile `390x844` and `320x700`: bottom nav visible with five destinations, sidebar hidden, Ca làm accessible from Dashboard, priority card and controls remain visible, no horizontal overflow. Capture no console errors; verify primary shipper requests return successful responses.

- [ ] **Step 2: Run full unit suite**

Run: `npm test`
Expected: all tests PASS.

- [ ] **Step 3: Run production build**

Run: `npm run build`
Expected: exit code 0.

- [ ] **Step 4: Run Playwright desktop/mobile**

Run: `npx playwright test tests/e2e/shipper-field-command.spec.js`
Expected: all scenarios PASS, no console errors, no failed primary requests.

- [ ] **Step 5: Inspect final diff**

Run: `git diff --check`
Expected: no whitespace errors. Confirm no backend, database, OpenAPI, store or API client changes were introduced by this redesign.
