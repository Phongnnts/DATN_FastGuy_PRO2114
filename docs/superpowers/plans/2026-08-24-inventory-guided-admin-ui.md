# Guided Inventory Admin UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign four inventory admin workflows as a guided operations center for owner-managers.

**Architecture:** Keep recipe and inventory-settings writes as independent OpenAPI resources and user actions. Extend the existing servlet/service with readiness validation and a deterministic pessimistic-read capacity snapshot; Vue consumes only contracted fields and reuses the shared confirmation dialog.

**Tech Stack:** OpenAPI 3.1, Java 17, Jakarta Servlet/JPA, SQL Server, Vue 3 `<script setup>`, Pinia, Node test runner, Playwright.

## Global Constraints

- OpenAPI first, backend second, frontend third. No database, dependency, route-path, or git operation.
- Recipe `PUT` and inventory-settings `PUT` are never combined in one save flow.
- Settings `409` is readiness failure only; no stale/concurrency wording or auto-reload claim.
- Capacity cost `0` is missing while general `Money` still allows zero.
- Plain Vietnamese before technical terminology.
- Desktop and mobile must expose the same actions.
- WCAG 2.2 focus, labels, target size, reflow, and live-region requirements apply.
- No new dependency.

---

### Task 1: Guided Navigation And Shared Presentation
- [ ] Add failing policy/helper tests for grouped navigation, workflow labels, totals, count progress, and variance reasons.
- [ ] Implement minimal helpers and grouped inventory navigation treatment.
- [ ] Run focused tests.

### Task 2: Inventory Operations Center
- [ ] Add failing source-policy assertions for today's work, workflow orientation, simplified rows, and expandable details.
- [ ] Redesign `InventoryPage.vue` without changing API calls or mutations.
- [ ] Run focused tests.

### Task 3: Guided Goods Receipts
- [ ] Add failing tests for staged labels, sentence conversion, document total, and accessible approval dialog.
- [ ] Redesign `GoodsReceiptsPage.vue`; replace native confirms.
- [ ] Run focused tests.

### Task 4: Understandable Recipes
- [ ] Add failing contract tests for exact settings/capacity paths, strict schemas, all `400/401/403/404/409/500` responses, and zero-cost capacity semantics.
- [ ] Add failing service/servlet tests for tracked-mode readiness, truthful `409`, unknown persisted mode `500`, rollback/close behavior, and deterministic `PESSIMISTIC_READ` snapshot locks.
- [ ] Update OpenAPI, then `AdminRecipeService` and `AdminRecipeServlet`; keep transactions short and resource writes independent.
- [ ] Add failing frontend tests for independent dirty states, zero-cost drafts, separate confirmations/actions, exact readiness message, picker outside-focus containment, body scroll lock, Escape, and trigger restoration.
- [ ] Redesign `RecipesPage.vue` using shared `ConfirmDialog`; sticky save sends recipe only and settings save sends settings only.
- [ ] Run focused tests.

### Task 5: Guided Stock Counts
- [ ] Add failing tests for step labels, progress/filter helpers, conditional reason validation, summary, and approval dialog.
- [ ] Redesign `StockCountsPage.vue`; replace native confirm.
- [ ] Run focused tests.

### Task 6: Verification
- [ ] Run focused RED then GREEN backend/frontend tests.
- [ ] Run OpenAPI contract lint and backend serialization/contract tests.
- [ ] Run `mvn test`; run disposable/local JPA integration when available.
- [ ] Run `npm test` and `npm run build`.
- [ ] Run Playwright desktop/mobile; assert recipe/settings request separation, settings `409` copy, dialog focus containment/restoration, no console/page errors, and critical requests.
- [ ] Assert no console/page errors and critical requests succeed.
- [ ] Run `git diff --check` and review only intended OpenAPI/backend/frontend/tests/docs changes.
