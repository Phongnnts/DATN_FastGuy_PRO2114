# FastGuy Warehouse macOS Floating Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current warehouse layouts with a stronger macOS floating-card system and add correct client-side 10-item pagination to Ingredients, Inventory, and Stock Count ingredient selection.

**Architecture:** Add one small generic pagination helper in `inventoryOperations.js`, covered by direct unit tests. Each page owns its current page/search/filter state and derives a visible slice after filtering. Preserve API arrays, selection across pages, route/query behavior, and every existing mutation flow.

**Tech Stack:** Vue 3 Composition API, existing Admin tokens/scoped CSS, Node test runner, Vite, Playwright Chromium and Firefox.

## Global Constraints

- Frontend-only; no contract/API/backend/database changes.
- Exactly 10 filtered ingredients/items per page on Ingredients, Inventory, and Stock Count Step 1.
- Stock Count recent panel shows 5 counts by default and no pagination.
- No unsupported all-count route; `Xem tất cả phiếu` expands in place.
- Search/filter changes reset page 1; shrinking results clamp page.
- Selection persists across Stock Count ingredient pages.
- Shared macOS palette, 16–18px primary radius, subtle border/shadow, restrained orange.
- Preserve all existing business handlers, query state, conflicts, dialogs, and approved locks.
- No new dependency and no unrelated refactor.
- Run focused tests, full `npm test`, `npm run build`, browser gates, and `git diff --check`.

---

### Task 1: Pagination helper and red tests

**Files:**
- Modify: `frontend/src/utils/inventoryOperations.js`
- Modify: `frontend/test/inventory-operations.test.js`

**Interfaces:**
- Produces: `paginateWarehouseItems(items, page, pageSize = 10)` returning `{ items, page, totalPages, totalItems, from, to }`.

- [ ] Add failing tests proving 10-item slicing, one-based ranges, empty results, and page clamping.
- [ ] Run `node --test test/inventory-operations.test.js` and confirm failure.
- [ ] Implement deterministic array pagination without mutating input.
- [ ] Re-run the focused test and require pass.

### Task 2: Ingredients floating workspace

**Files:**
- Modify: `frontend/src/views/admin/IngredientsPage.vue`
- Modify: `frontend/test/admin-ingredient-inventory-policy.test.js`

**Interfaces:**
- Consumes: filtered `ingredients` and `paginateWarehouseItems`.
- Produces: `ingredientPage`, `paginatedIngredients`, 10-row table/cards, range footer.

- [ ] Add failing source tests for `PAGE_SIZE = 10`, paginated rendering, reset watches, pagination footer, neutral repeated row actions, and 900px cards.
- [ ] Implement `ingredientPage`, paginated computed state, search/status reset, and clamp.
- [ ] Render only `paginatedIngredients.items` in desktop/mobile.
- [ ] Add range and accessible previous/page/next controls.
- [ ] Restyle cards/workspace to macOS floating surfaces and reduce orange row actions.
- [ ] Run focused tests.

### Task 3: Minimal Inventory workspace

**Files:**
- Modify: `frontend/src/views/admin/InventoryPage.vue`
- Modify: `frontend/tests/admin-inventory-operations.test.mjs`

**Interfaces:**
- Consumes: `filteredItems`, route-backed `statusFilter`, `paginateWarehouseItems`.
- Produces: `inventoryPage`, `paginatedItems`, compact metrics, priority card, quick chips, 10-row table/cards.

- [ ] Add failing tests for 10-item pagination and the absence of the large dark priority treatment.
- [ ] Implement pagination after existing search/status filtering with resets/clamp.
- [ ] Replace dark priority block and large workflow cards with one white priority card and compact action chips.
- [ ] Keep six essential columns; show available/reserved as secondary text.
- [ ] Render pagination footer and mobile cards.
- [ ] Run inventory/ledger/adjustment focused tests.

### Task 4: Stock Count selection grid and recent-five panel

**Files:**
- Modify: `frontend/src/views/admin/StockCountsPage.vue`
- Modify: `frontend/test/inventory-operations.test.js`
- Modify: `frontend/tests/admin-inventory-workflows-operations-studio.test.mjs`

**Interfaces:**
- Consumes: `selectableIngredients`, selected ID array, `paginateWarehouseItems`.
- Produces: search/filter state, 2x5 paged grid, select-all/clear, recent five/full-list toggle.

- [ ] Add failing tests for `PAGE_SIZE = 10`, paged ingredient cards, whole-card checkbox labels, select-all/clear, recent `slice(0, 5)`, and in-place all-count expansion.
- [ ] Add ingredient search and active/all filter; derive paginated selection after filtering.
- [ ] Preserve selection IDs across pages; make select-all/clear operate on all filtered items.
- [ ] Move date and create CTA into a fixed footer row within the selection card.
- [ ] Replace Step 1 vertical checkbox list with 2-column card grid and subtle selected state.
- [ ] Show five recent count buttons by default and implement `Xem tất cả phiếu` in place.
- [ ] Keep the editor and approval flow unchanged; compact its empty state.
- [ ] Run focused count tests.

### Task 5: Receipts and Recipes visual alignment

**Files:**
- Modify: `frontend/src/views/admin/GoodsReceiptsPage.vue`
- Modify: `frontend/src/views/admin/RecipesPage.vue`

**Interfaces:**
- Produces: floating macOS surfaces only; no new state/data behavior.

- [ ] Align radius, border, shadow, spacing, and restrained accent with the three primary screens.
- [ ] Remove redundant visual borders while preserving semantic fieldsets and focus states.
- [ ] Keep receipt stages and recipe summaries visually prominent.
- [ ] Run focused receipt/recipe tests.

### Task 6: Full verification

**Files:**
- Modify E2E heading/selectors only if approved visible text changed.

- [ ] Run all warehouse-focused unit/source tests.
- [ ] Run `npm test` and require zero failures.
- [ ] Run `npm run build` and require exit 0.
- [ ] Run `admin-inventory-operations.spec.js` on desktop/mobile Chromium.
- [ ] Run bounded Firefox desktop/mobile smoke for Ingredients, Inventory, Receipts, Recipes, Counts.
- [ ] Confirm no console/page errors and no 390px horizontal overflow.
- [ ] Run `git diff --check`, status, and diff review; do not commit or merge without explicit instruction.

## Self-review

The plan covers every approved pagination and layout rule, preserves current contracts, and avoids creating the unapproved all-count route. Helper names and page size remain consistent across tasks. No placeholder or unspecified behavior remains.
