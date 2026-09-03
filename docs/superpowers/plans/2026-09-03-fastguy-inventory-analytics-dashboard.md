# FastGuy Inventory Analytics Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add truthful daily inventory analytics and redesign `/admin/inventory` into a KPI, trend, action, detail, and evidence dashboard.

**Architecture:** Extend OpenAPI first with one closed analytics response. Add pure aggregation methods to `InventoryReportService` and a dedicated Admin servlet path following existing report authorization/error patterns. Add a frontend API method, deterministic presentation helper, and dashboard sections using the existing chart library while retaining independent item/analytics loading.

**Tech Stack:** OpenAPI 3.1, Java 17/Jakarta Servlet/JPA, JUnit 5, Vue 3, existing Axios client, existing Chart.js integration, Node test runner, Vite, Playwright.

## Global Constraints

- Delivery order: OpenAPI → backend → frontend.
- No database migration or seed change.
- Do not sum incompatible physical quantities.
- All trend/movement charts use monetary values.
- Date range is inclusive, maximum 366 days, granularity `DAY` only.
- Preserve current inventory list, tabs, 10-item pagination, dialogs, mutations, and query behavior.
- Analytics and item loading fail independently; stale analytics responses cannot win.
- No fabricated chart points or previous-period values.
- Run full backend/frontend/browser verification; no commit/merge/push without explicit request.

---

### Task 1: OpenAPI contract

**Files:**
- Modify: `openapi/fastguy.yaml`
- Modify: `frontend/test/openapi-contract.test.js`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/InventoryCostingEndpointTest.java`

**Interfaces:**
- Produces: `GET /admin/inventory/analytics` with exact query and closed schemas from the approved design.

- [ ] Add failing exact contract tests for path, required date queries, DAY enum, and response fields/enums.
- [ ] Run focused frontend/backend tests and confirm failure.
- [ ] Add the smallest OpenAPI path and closed component schemas.
- [ ] Re-run focused contract tests and require pass.

### Task 2: Backend analytics aggregation

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryReportService.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryCostingWorkflowTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/integration/InventoryCostingIT.java`

**Interfaces:**
- Produces: `Map<String,Object> analytics(LocalDate from, LocalDate to)`.
- Produces pure helpers for comparison range, daily series, health classification, and attention rows.

- [ ] Add failing tests for inclusive daily buckets, same-length previous period, ending valuation, receipt/consume/waste/count adjustments, health thresholds, zero minimum, and missing costs.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement analytics using existing transaction query and item data; keep deterministic ordering.
- [ ] Run unit and disposable integration tests.

### Task 3: Analytics servlet

**Files:**
- Modify or create following existing mapping pattern: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/InventoryReportServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/InventoryCostingEndpointTest.java`

**Interfaces:**
- Consumes: required `fromDate`, `toDate`, `granularity=DAY`.
- Produces: contracted analytics envelope and 400/401/403/500 behavior.

- [ ] Add failing auth/query/path/serialization tests.
- [ ] Implement exact routing and validation without broadening other report endpoints.
- [ ] Run focused servlet tests.

### Task 4: Frontend analytics state and helpers

**Files:**
- Modify: `frontend/src/api/admin.js`
- Create: `frontend/src/utils/inventoryAnalytics.js`
- Create: `frontend/test/inventory-analytics.test.js`

**Interfaces:**
- Produces: `adminApi.getInventoryAnalytics(params)`.
- Produces period/query, delta, series normalization, and health presentation helpers.

- [ ] Add failing tests for 7/30/90/custom ranges, URL values, signed deltas, previous-zero fallback, empty series, and health labels.
- [ ] Implement the API call and pure helpers.
- [ ] Run focused helper/API tests.

### Task 5: Inventory analytics dashboard UI

**Files:**
- Modify: `frontend/src/views/admin/InventoryPage.vue`
- Modify: `frontend/tests/admin-inventory-operations.test.mjs`

**Interfaces:**
- Consumes: analytics API/helper output and existing item list.
- Produces: period controls, unified KPI strip, primary chart row, health/action row, table, attention ratios, recent evidence.

- [ ] Add failing source-policy tests for hierarchy and independent analytics state.
- [ ] Add URL-backed period state and generation-guarded analytics loader.
- [ ] Replace duplicate metric/priority structures with one KPI strip.
- [ ] Build inventory value line chart and movement-value bar chart with accessible summaries using existing chart components/library.
- [ ] Build segmented health bar and compact action card.
- [ ] Keep the 10-row table and add bottom attention/recent evidence row.
- [ ] Implement loading, initial error, retained-data refresh warning, empty, and partial-cost states.
- [ ] Run focused frontend tests and build.

### Task 6: Full verification

- [ ] Run `mvn test`.
- [ ] Run disposable/local `InventoryCostingIT` where configured.
- [ ] Run `npm test` and `npm run build`.
- [ ] Update intercepted browser fixtures to return the exact analytics schema.
- [ ] Run warehouse Playwright Chromium desktop/mobile; assert analytics request success, zero console/page errors, and no 390px overflow.
- [ ] Run Firefox smoke if available.
- [ ] Run `git diff --check`, inspect status/diff, and leave uncommitted.

## Self-review

Every approved API field and dashboard section maps to a task. The plan explicitly handles missing costs, previous-zero comparison, incompatible units, stale responses, and independent failure. No schema migration or unsupported metric remains.
