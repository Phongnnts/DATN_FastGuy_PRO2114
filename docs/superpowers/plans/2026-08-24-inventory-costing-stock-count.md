# Inventory Costing And Stock Count Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add moving-average ingredient costing, approvable receipts, physical stock counts, variance costing, and admin reports.

**Architecture:** `InventoryItem` owns current balance and average cost. Immutable `InventoryTransaction` rows own historical quantity and cost snapshots. Draft receipt/count documents become inventory mutations only through atomic approval services.

**Tech Stack:** SQL Server 2016+, Java 17, Jakarta Servlet/JPA, Hibernate 6.6, OpenAPI 3.1, Vue 3, Axios, Node test runner, Playwright.

## Global Constraints

- Work in `DATABASE → API → FRONTEND` order.
- Use `DECIMAL(19,4)` and `BigDecimal`; never `double` for quantity or money.
- Do not write retained `FastGuyDB` without separate approval and verified recovery evidence.
- No lots, expiry dates, FEFO, supplier module, multiple warehouses, or new dependencies.

---

### Task 1: Database Foundation

- [ ] Add migration `053_inventory_costing_stock_count.sql` with guarded, transactional DDL.
- [ ] Add `053_validate.sql` covering columns, constraints, indexes, and backfill.
- [ ] Run source preflight and disposable migration/validator/rerun.

### Task 2: OpenAPI Contract

- [ ] Add inventory cost fields, receipt endpoints/schemas, stock-count endpoints/schemas, and report endpoints/schemas.
- [ ] Add backend contract assertions and run OpenAPI lint.

### Task 3: Cost Domain

- [ ] Write failing entity and cost-calculation tests.
- [ ] Add JPA mappings and moving-average `BigDecimal` calculation.
- [ ] Snapshot cost on consumption/waste and expose ledger cost fields.

### Task 4: Receipt Workflow

- [ ] Write failing service/servlet tests for draft and one-time approval.
- [ ] Implement receipt entities, service, and servlet.
- [ ] Verify atomic balance/cost/ledger behavior.

### Task 5: Stock Count Workflow

- [ ] Write failing tests for draft snapshots, stale approval, reserved floor, and variance cost.
- [ ] Implement count entities, service, and servlet.
- [ ] Verify approved count immutability and ledger references.

### Task 6: Reports

- [ ] Write failing aggregation tests.
- [ ] Implement summary, item-loss, and menu-cost queries/endpoints.
- [ ] Verify waste and count loss are not double-counted.

### Task 7: Admin Frontend

- [ ] Add tested payload/math utilities and API client methods from OpenAPI.
- [ ] Add receipt, stock-count, and report routes/views.
- [ ] Extend inventory, recipe, and ledger views with cost/count data.

### Task 8: Verification

- [ ] Run backend focused tests, `mvn test`, and `mvn package`.
- [ ] Run `npm test`, OpenAPI lint, and `npm run build`.
- [ ] Run disposable DB integration and Playwright desktop/mobile.
- [ ] Review diff; do not commit or write retained DB without explicit request.
