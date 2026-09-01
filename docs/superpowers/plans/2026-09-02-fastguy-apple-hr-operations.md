# FastGuy Apple-Inspired HR Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved Admin shell and HR Dashboard, Users, Shifts, and Attendance/Pay Rates redesign without weakening existing contracts or safeguards.

**Architecture:** Implement in thin frontend slices after contract verification. Reuse current Vue pages, Admin API clients, Pinia stores, semantic CSS variables, and native calendar layout; keep scheduling, monitoring, attendance, and pay calculations authoritative in backend services. Introduce a dedicated HR Dashboard route only after its exact data composition is proven from OpenAPI and current providers.

**Tech Stack:** Vue 3 Composition API, Vue Router, Pinia, Vite, native CSS, Bootstrap Icons, Node test runner, Playwright Chromium, Java Servlets/JPA only if contract analysis proves a backend gap.

## Global Constraints

- Follow `DATABASE → API → FRONTEND` for every cross-stack change.
- `openapi/fastguy.yaml` is authoritative for contracted endpoints and fields.
- Do not infer schema, request fields, response fields, metrics, trends, or payroll meaning.
- Week is the default Shifts calendar mode; Monitoring remains separate from editable scheduling.
- Preserve URL state, stale-response generation guards, canonical-week freshness checks, self-account safeguards, attendance conflict reloads, focus behavior, and responsive behavior.
- Use the existing icon system and dependencies; do not add a calendar/UI dependency unless approved after analysis.
- Use Tailwind v4 only with the `tw:` prefix; incremental migration only.
- Target WCAG 2.2 AA and respect `prefers-reduced-motion`.
- Do not modify protected untracked `.agents/skills/*`, `.hermes/`, `diagrams/`, or `wireframes/`.
- Do not commit or push implementation unless explicitly requested.

---

### Task 1: Contract and route baseline

**Files:**
- Inspect: `openapi/fastguy.yaml`
- Inspect: `frontend/src/router/index.js`
- Inspect: `frontend/src/api/admin.js`
- Inspect: `frontend/src/stores/admin.js`
- Test: `frontend/test/openapi-contract.test.js`
- Test: `frontend/tests/admin-r2-navigation.test.mjs`

**Interfaces:**
- Consumes: current Users, shift week, monitoring, attendance, and pay-rate operations.
- Produces: an exact operation/schema/route matrix used by Tasks 2–6; no implementation field outside this matrix.

- [ ] **Step 1: Trace each HR operation with CodeGraph**

Record the exact operations used by `UsersPage`, `ShiftsPage`, and `AttendancePage`, including request parameters and response schemas.

- [ ] **Step 2: Run contract and navigation baselines**

Run: `npm test -- --runInBand` only if supported by the current package script; otherwise run the exact targeted Node commands discovered in `package.json` for `test/openapi-contract.test.js` and `tests/admin-r2-navigation.test.mjs`.

Expected: PASS before implementation.

- [ ] **Step 3: Decide HR Dashboard and Month data strategy**

Use only these outcomes:

```text
HR Dashboard: compose independently from existing contracted reads, or add one minimal OpenAPI operation before provider code.
Month: compose bounded existing week reads, or add one minimal month-range OpenAPI operation before provider code.
```

Do not implement until both choices are documented in the task notes with exact operation IDs.

- [ ] **Step 4: Commit only if implementation execution was explicitly authorized**

```bash
git add openapi/fastguy.yaml frontend/src/router/index.js frontend/test/openapi-contract.test.js frontend/tests/admin-r2-navigation.test.mjs
git commit -m "test(hr): define admin HR contracts"
```

### Task 2: Shared Admin shell visual system

**Files:**
- Modify: `frontend/src/assets/styles/variables.css`
- Modify: `frontend/src/layouts/AdminLayout.vue`
- Test: `frontend/tests/admin-shell-apple-operations.test.mjs`
- Test: relevant Admin layout tests under `frontend/test/`

**Interfaces:**
- Consumes: existing complete Admin navigation inventory and drawer lifecycle.
- Produces: semantic Admin tokens and shared sidebar/header surface used by every later task.

- [ ] **Step 1: Add failing shell assertions**

Assert that the complete current route inventory remains present, active navigation uses semantic classes, the profile remains in the account block, and drawer Escape/focus/inert behavior remains wired.

- [ ] **Step 2: Run targeted shell tests and verify failure**

Expected: FAIL on the new approved shell selectors/tokens, not on missing routes.

- [ ] **Step 3: Implement semantic tokens**

Add semantic values equivalent to:

```css
--admin-canvas: #fff;
--admin-ink: #20212b;
--admin-muted: #858794;
--admin-hairline: rgba(20, 20, 35, 0.075);
--admin-brand: #ff7448;
--admin-brand-soft: #fff1eb;
```

Add existing-system-compatible success, warning, danger, surface, shadow, and moderate-radius tokens. Do not scatter raw duplicates through components.

- [ ] **Step 4: Implement shell surfaces**

Preserve all navigation and drawer logic while applying the approved inset white sidebar/header, moderate radii, hairline boundaries, subtle layered shadows, grouped labels, active state, and bottom account block.

- [ ] **Step 5: Verify shell behavior**

Run targeted shell tests and desktop/mobile Playwright checks for keyboard navigation, drawer focus containment, Escape, inert background, and trigger restoration.

Expected: PASS with zero console errors.

### Task 3: Dedicated HR Dashboard

**Files:**
- Create: `frontend/src/views/admin/HrDashboardPage.vue`
- Modify: `frontend/src/router/index.js`
- Modify only if required by Task 1: `frontend/src/api/admin.js`
- Modify only if required by Task 1: `frontend/src/stores/admin.js`
- Test: create a focused HR Dashboard test beside existing Admin source/state tests
- Test: create/update a Playwright Admin HR spec under `frontend/tests/e2e/`

**Interfaces:**
- Consumes: exact Task 1 operations for Users, current shift week, Monitoring, and Attendance.
- Produces: independent section states and exact filtered links to Users, Shifts, Monitoring, and Attendance.

- [ ] **Step 1: Write failing section-state tests**

Cover independent loading/error/success for workforce, today’s three fixed shifts, monitoring exceptions, and pending attendance. Assert that one section failure does not remove valid sibling sections.

- [ ] **Step 2: Run tests and verify failure**

Expected: FAIL because `HrDashboardPage.vue` and its route do not exist.

- [ ] **Step 3: Add the exact route and navigation item**

Follow existing route naming, path, title, role metadata, and breadcrumb conventions discovered in Task 1.

- [ ] **Step 4: Implement independent reads**

Use separate generation IDs or existing loader utilities so stale requests cannot overwrite current state. Do not duplicate `WorkShiftService` monitoring calculations in Vue.

- [ ] **Step 5: Implement approved hierarchy**

Render only contract-backed values: active workforce/account count, staffed fixed shifts out of three, pending attendance count, current exceptions, today’s shift list, and exact destination links.

- [ ] **Step 6: Verify partial failure and responsive states**

Run focused tests and Playwright desktop/mobile. Check loading, error, empty, partial data, keyboard focus, and no console errors.

### Task 4: Users workspace redesign

**Files:**
- Modify: `frontend/src/views/admin/UsersPage.vue`
- Modify only if existing store gaps are proven: `frontend/src/stores/admin.js`
- Test: `frontend/tests/admin-user-protection.test.mjs`
- Test: relevant Users source/policy tests under `frontend/test/`
- Test: HR Playwright spec

**Interfaces:**
- Consumes: existing `fetchUsers`, create/update/delete/status mutations, avatar upload, and `adminApi.getUserOrders(userId)`.
- Produces: searchable/filterable account table, detail inspector, add/edit dialog, and role-aware order history.

- [ ] **Step 1: Write failing preservation and presentation tests**

Assert search fields, role filters, pagination reset, self-account safeguards, status/delete confirmations, avatar controls, add/edit validation, and order-history behavior remain present. Add assertions for the approved table/detail-inspector structure.

- [ ] **Step 2: Run targeted Users tests and verify failure**

Expected: existing safeguards PASS; new visual/structure assertions FAIL.

- [ ] **Step 3: Implement approved KPI summaries**

Derive total, active, STAFF+SHIPPER, inactive, and optional percentages from the same loaded account collection. Do not add trend claims.

- [ ] **Step 4: Implement table and detail inspector**

Keep semantic table headers, identity/contact/role/status/points/actions, visible row focus, 40px action targets, and mobile labeled cards/list. Inspector must not replace existing safe mutations.

- [ ] **Step 5: Refine dialogs without changing validation**

Preserve exact full-name, email, phone, password, self-role, avatar, busy, error, and order-history behavior. Add focus containment, Escape, and restoration if current coverage proves gaps.

- [ ] **Step 6: Verify Users flows**

Run focused tests and Playwright for search, each role filter, pagination, inspector, add/edit validation, status confirmation, protected self actions, order history, and mobile reflow.

### Task 5: Shifts Week, Month, and Monitoring

**Files:**
- Modify: `frontend/src/views/admin/ShiftsPage.vue`
- Modify only if Task 1 requires a contract extension: `openapi/fastguy.yaml`
- Modify only if contract extension is approved: shift servlet/service/DAO files identified by CodeGraph
- Modify only if required: `frontend/src/api/admin.js`
- Test: `frontend/tests/weekly-shifts-policy.test.mjs`
- Test: `frontend/test/operating-finance-ui.test.js`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShiftServletBehaviorTest.java`
- Test: relevant WorkShiftService policy/integration tests
- Test: HR Playwright spec

**Interfaces:**
- Consumes: fixed shift codes, week read/replace, Monitoring, active STAFF, canonical-week freshness check, and Task 1 Month strategy.
- Produces: URL-restorable `schedule|monitoring` mode and `week|month` view with selected period/date.

- [ ] **Step 1: Write failing URL and view-state tests**

Cover Week default, valid query restoration, invalid query fallback, Back/Forward behavior, current-day marker, selected Month day, and future navigation disabled under current policy.

- [ ] **Step 2: Run targeted shift tests and verify failure**

Expected: existing week freshness/stale guards PASS; Month/view-state assertions FAIL.

- [ ] **Step 3: Implement state normalization**

Add small pure functions for valid tab, calendar view, period, and selected date. Update query state with existing router conventions without loops.

- [ ] **Step 4: Implement Week layout**

Render Monday–Sunday columns and Morning/Afternoon/Evening rows using native controls. Preserve exactly one active STAFF assignment per fixed slot and the existing save payload.

- [ ] **Step 5: Implement Month using the approved Task 1 strategy**

Render a Notion-like hairline grid with compact fixed-shift entries and a selected-day inspector. Do not imply free-form events or future scheduling.

- [ ] **Step 6: Implement separate Monitoring presentation**

Preserve 30-second polling only while active, all generation guards, and backend-provided state/severity. Show labels/icons in addition to semantic tint.

- [ ] **Step 7: Verify save conflict and lifecycle**

Test stale canonical-week conflict, failed reads, monitoring tab enter/leave polling, unmount cleanup, URL restoration, keyboard controls, desktop inspector, and mobile sheet/panel.

- [ ] **Step 8: Run backend gates only if backend/API changed**

Run relevant servlet/service tests, disposable integration tests, and `mvn test`.

### Task 6: Attendance approval and pay rates

**Files:**
- Modify: `frontend/src/views/admin/AttendancePage.vue`
- Modify only if contract analysis requires it: `frontend/src/api/admin.js`
- Test: `frontend/tests/attendance-truth-source.test.mjs`
- Test: `frontend/tests/admin-attendance-r6.test.mjs`
- Test: HR Playwright spec

**Interfaces:**
- Consumes: attendance month/status/user filters, approval with `expectedUpdatedAt`, effective-dated pay-rate list/create.
- Produces: separate `attendance` and `rates` views with unambiguous calculated/estimated/missing/legacy states.

- [ ] **Step 1: Write failing mode and truth-label tests**

Assert Attendance is default; Rates is separate; labels distinguish `CALCULATED`, preview estimate, missing effective rate, and `LEGACY_UNAVAILABLE`; missing rate and approved rows cannot be approved.

- [ ] **Step 2: Run targeted Attendance tests and verify failure**

Expected: source-of-truth safeguards PASS; new mode/presentation assertions FAIL.

- [ ] **Step 3: Implement attendance summary and filters**

Derive totals only from currently loaded rows. Keep month/status/user request key and generation checks unchanged.

- [ ] **Step 4: Implement approval table/cards**

Preserve actual, overlap-eligible, late, early-leave, potential OT, editable approved values, note, pay state, and action. Keep `expectedUpdatedAt`; on 409 reload canonical data and retain visible conflict guidance.

- [ ] **Step 5: Implement effective-dated rate workspace**

Keep only staff, effective date, regular hourly rate, overtime hourly rate, chronological history, busy/error state, and duplicate-date conflict.

- [ ] **Step 6: Verify approval and rate flows**

Run focused tests and Playwright for filters, stale requests, valid approval, missing-rate disabled action, approved disabled action, conflict reload, rate creation, duplicate date, empty history, and mobile labeled layout.

### Task 7: Full HR verification and review

**Files:**
- Review: all files changed by Tasks 1–6
- Test: all focused tests above
- Test: `frontend/tests/e2e/` HR specs

**Interfaces:**
- Consumes: completed shell and HR surfaces.
- Produces: verified implementation ready for user review, not automatically committed or pushed.

- [ ] **Step 1: Run focused tests**

Run every targeted command identified in Tasks 1–6.

Expected: all PASS.

- [ ] **Step 2: Run full frontend gates**

```bash
npm test
npm run build
```

Expected: PASS with no new warnings attributable to the change.

- [ ] **Step 3: Run browser verification**

Run Chromium desktop and narrow/mobile flows for Admin shell, HR Dashboard, Users, Shifts Week/Month/Monitoring, Attendance approval, and Pay Rates. Confirm critical requests succeed and console has no uncaught errors.

- [ ] **Step 4: Run backend gates when applicable**

If any backend/API file changed:

```bash
mvn test
```

Also run the approved disposable/local integration test; never target retained `FastGuyDB` for writes.

- [ ] **Step 5: Check diff hygiene**

```bash
git diff --check
git status --short
git diff --stat
git diff
```

Confirm no protected artifacts, secrets, unrelated changes, generated reports, or mockup companion files are included.

- [ ] **Step 6: Request review**

Present verification evidence and wait for explicit instruction before committing, pushing, or creating a PR.
