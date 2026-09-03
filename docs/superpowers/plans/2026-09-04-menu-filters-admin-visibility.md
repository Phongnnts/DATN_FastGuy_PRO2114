# Menu Filters and Admin Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show every real menu category, provide truthful quick filters, and remove Activity Logs plus fees/delivery settings from Admin navigation.

**Architecture:** Simplify menu category grouping to a one-to-one API mapping and unify quick-filter state with existing URL/detail-filter state. Hide Admin surfaces at router/navigation/tab configuration boundaries without deleting backend code or persisted data.

**Tech Stack:** Vue 3 Composition API, Vue Router, existing Pinia/API clients, Node test runner, Vite, Playwright Chromium.

## Global Constraints

- Do not change database, backend, OpenAPI, seed or API payloads.
- Display every category returned by the API in API order.
- Remove keyword-only office/student combo filters.
- Keep only truthful quick filters: best seller, discounted, under 40K, available.
- Remove Activity Logs sidebar/route and fees/delivery settings tabs only.
- Preserve current layout/design language and unrelated behavior.

---

### Task 1: Menu category and quick-filter policy

**Files:**
- Modify: `frontend/src/utils/menuFilters.js`
- Modify: `frontend/test/menu-filter-draft.test.js`
- Modify: relevant menu source-policy test if present.

- [ ] Write failing tests proving all categories remain distinct/in API order and `available` maps to `availability=AVAILABLE`.
- [ ] Write failing tests proving office/student keyword filters are absent.
- [ ] Run focused tests and observe expected failures.
- [ ] Simplify `buildMenuCategoryGroups` to `Tất cả` plus one group per category without taxonomy merging.
- [ ] Extend quick-filter parameter/state helpers for `available`.
- [ ] Run focused tests to green.

### Task 2: Menu UI and URL state

**Files:**
- Modify: `frontend/src/views/guest/MenuPage.vue`
- Modify: menu tests.

- [ ] Write failing tests for all-category rendering, no `Thêm`, no combo quick filters, and `Còn hàng` URL/chip/reset behavior.
- [ ] Remove primary/overflow category splitting and render all category groups in one responsive category rail.
- [ ] Replace combo filters with `Còn hàng`; remove keyword-discovery query mutation.
- [ ] Ensure quick filter and drawer availability state do not create duplicate chips or contradictory query fields.
- [ ] Preserve category/search/sort/page/grid-list flow and run focused tests.

### Task 3: Hide Admin Activity Logs and settings sections

**Files:**
- Modify: `frontend/src/layouts/AdminLayout.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/admin/SettingsPage.vue`
- Modify: related Admin router/sidebar/settings tests.

- [ ] Write failing tests that Activity Logs has no sidebar entry/route and settings excludes fees/delivery tabs.
- [ ] Remove the Activity Logs navigation item and route import/record.
- [ ] Remove fees/delivery from visible settings tab configuration and any loader reachable only from those tabs.
- [ ] Preserve remaining tab keyboard navigation, validation and save behavior.
- [ ] Run focused tests to green.

### Task 4: Verification

- [ ] Run all focused menu/Admin tests.
- [ ] Run `npm test` and require zero failures.
- [ ] Run `npm run build` and require success.
- [ ] Run Chromium desktop against a known environment; verify full category rail, four quick filters, successful catalog request, Activity Logs inaccessible, hidden settings tabs, no overflow and zero console/page errors.
- [ ] Run `git diff --check` and inspect exact diff.
- [ ] Do not commit/push the new UI work unless explicitly requested after verification.
