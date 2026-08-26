# Staff Dispatch Filters And Automatic Closing Cancellation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add server-classified Priority, New and Review filters to Staff dispatch, then automatically cancel unassigned READY orders after their applicable store closing time.

**Architecture:** Preserve current Servlet, Service, DAO, JPA and Vue layers. Add a small dispatch policy for deterministic closing-time/classification logic, contract one filtered Staff endpoint, reuse `OrderTransitionService.cancel` for transactional side effects, then consume the response in `DispatchPage.vue` with stale-request protection.

**Tech Stack:** Java 17, Jakarta Servlet 6.1, JPA/Hibernate, SQL Server, OpenAPI 3.1, Vue 3, Pinia, Node test runner, Playwright.

## Global Constraints

- Mandatory order: `DATABASE → API → FRONTEND`.
- Stop implementation if SQL Server identity/schema/config cannot be verified.
- No schema migration and no new dependency.
- OpenAPI changes precede backend provider and Vue consumer changes.
- Retained `FastGuyDB` remains read-only; integration mutations use disposable/local test database only.
- `Priority`: unassigned READY, old or at most 30 minutes from closing.
- `New`: unassigned READY with `readyAt` in the last 15 minutes.
- `Review`: DELIVERY_FAILED awaiting Staff action.
- Auto-cancel only unassigned READY after closing; never ASSIGNED, PICKED_UP, DELIVERY_FAILED or terminal orders.
- Cancellation must reuse inventory, coupon, refund and history invariants in `OrderTransitionService.cancel`.
- Keep checked-in Staff access, polling, expected status, conflict and stale-request guards.

## File Map

- `openapi/fastguy.yaml`: authoritative dispatch operation and schemas.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/DispatchOrderPolicy.java`: pure closing datetime and classification policy.
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`: candidate queries and deterministic ordering.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`: dispatch envelope orchestration.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`: filter parsing and serialization.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java`: atomic system cancellation precondition.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java`: minute-tick closing cancellation.
- `frontend/src/api/staff.js`: dispatch request.
- `frontend/src/stores/staff.js`: canonical dispatch mapping/state.
- `frontend/src/views/staff/DispatchPage.vue`: tabs, counts, assignment/review surfaces.
- Focused Java/frontend tests plus disposable integration and Playwright specs.

---

### Task 1: Database And Runtime Gate

**Files:**
- Read: `database/init.sql`
- Read: `database/DB_FastGuy.sql`
- Read: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`
- Read: `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java`

**Interfaces:**
- Consumes: runtime SQL Server catalog and retained read-only data.
- Produces: verified table/column/config evidence required by every later task.

- [ ] **Step 1: Confirm server and database identity**

Run read-only SQL:

```sql
SELECT @@SERVERNAME AS [ServerName], DB_NAME() AS [DatabaseName],
       [state_desc], [compatibility_level]
FROM [sys].[databases]
WHERE [name] = DB_NAME();
```

Expected: explicit server name, expected database name, `ONLINE`, compatibility level `160`. If connection or identity fails, stop here and report the blocker.

- [ ] **Step 2: Verify runtime catalog**

Run read-only SQL:

```sql
SELECT [c].[name], [t].[name] AS [type_name], [c].[is_nullable]
FROM [sys].[columns] [c]
JOIN [sys].[types] [t] ON [t].[user_type_id] = [c].[user_type_id]
WHERE [c].[object_id] = OBJECT_ID(N'[dbo].[Orders]')
  AND [c].[name] IN (N'order_id',N'order_status',N'created_at',N'ready_at',N'assigned_at',N'shipper_id',N'payment_status',N'refund_status');

SELECT [config_key], [config_value]
FROM [dbo].[ShippingConfig]
WHERE [config_key] IN (N'business_open_time',N'business_close_time');
```

Expected: all eight order columns and exactly two parseable `HH:mm` config rows.

- [ ] **Step 3: Compare mappings and canonical SQL**

Confirm SQL types/nullability match `Orders.java`, `database/init.sql` and `database/DB_FastGuy.sql`. Record no migration needed. Do not edit code until all checks pass.

### Task 2: OpenAPI Dispatch Contract

**Files:**
- Modify: `openapi/fastguy.yaml`
- Modify: `frontend/test/openapi-contract.test.js`

**Interfaces:**
- Produces: `GET /staff/orders/dispatch?filter=PRIORITY|NEW|REVIEW`, `StaffDispatchResponse`, `StaffDispatchOrder`, `StaffDispatchCounts`.

- [ ] **Step 1: Write failing contract assertions**

Add a test that parses the OpenAPI document and asserts:

```js
assert.ok(paths['/staff/orders/dispatch']?.get);
assert.deepEqual(filter.schema.enum, ['PRIORITY', 'NEW', 'REVIEW']);
assert.deepEqual(dispatchOrder.properties.classification.enum, ['PRIORITY', 'NEW', 'REVIEW']);
assert.ok(dispatchOrder.properties.readyAt.nullable);
assert.equal(dispatchOrder.properties.minutesUntilClose.type, 'integer');
```

- [ ] **Step 2: Run contract test and verify failure**

Run: `node --test test/openapi-contract.test.js`
Expected: FAIL because `/staff/orders/dispatch` is absent.

- [ ] **Step 3: Add the minimal OpenAPI operation and schemas**

Define required filter query parameter, response envelope fields `items`, `counts`, `serverTime`, `openTime`, `closeTime`, exact enums, nullability and `400/401/403` responses. Reuse existing Staff list schemas through `allOf` where possible.

- [ ] **Step 4: Validate contract**

Run: `npm run contract:lint`
Expected: PASS with no unresolved or remote refs.

- [ ] **Step 5: Run contract regression**

Run: `node --test test/openapi-contract.test.js`
Expected: PASS.

### Task 3: Closing-Time And Classification Policy

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/DispatchOrderPolicy.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/DispatchOrderPolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/StaffOrderServletBehaviorTest.java`

**Interfaces:**
- Produces: `DispatchOrderPolicy.closingAt(LocalDateTime createdAt, LocalTime open, LocalTime close)`, `classify(Orders order, LocalDateTime now, LocalTime open, LocalTime close)`, `StaffOrderService.getDispatchOrders(String filter)`.

- [ ] **Step 1: Write failing pure policy tests**

Cover exact boundaries:

```java
assertEquals(LocalDateTime.of(2026, 8, 25, 22, 0), policy.closingAt(LocalDateTime.of(2026, 8, 25, 10, 0), LocalTime.of(8, 0), LocalTime.of(22, 0)));
assertEquals(LocalDateTime.of(2026, 8, 26, 2, 0), policy.closingAt(LocalDateTime.of(2026, 8, 25, 23, 0), LocalTime.of(18, 0), LocalTime.of(2, 0)));
assertNull(policy.closingAt(LocalDateTime.of(2026, 8, 25, 10, 0), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
```

Also assert READY at `now-15m` is New, READY with 30 minutes remaining is Priority, old READY is Priority, and DELIVERY_FAILED is Review.

- [ ] **Step 2: Run policy tests and verify failure**

Run: `mvn -Dtest=DispatchOrderPolicyTest test`
Expected: FAIL because the policy class does not exist.

- [ ] **Step 3: Implement pure policy**

Use `Duration.between(now, closingAt).toMinutes()`. Return no READY classification when closing time has passed. Treat equal open/close as 24-hour and return `null` closing/minutes. Keep enum values exact: `PRIORITY`, `NEW`, `REVIEW`.

- [ ] **Step 4: Add DAO/service orchestration**

Query unassigned READY and DELIVERY_FAILED candidates using existing columns, then classify with one `now` and one config read per request. Return selected items and all three counts from the same snapshot time. Preserve deterministic ordering from the spec.

- [ ] **Step 5: Add Servlet filter and serializer tests**

Assert missing/unknown filter returns `400`; valid filters serialize `readyAt`, `classification`, `minutesUntilClose`, counts and server/config times. Assert checked-in access remains required.

- [ ] **Step 6: Run focused backend tests**

Run: `mvn -Dtest=DispatchOrderPolicyTest,StaffOrderServletBehaviorTest test`
Expected: PASS.

### Task 4: Atomic Scheduler Cancellation

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderSchedulerClosingPolicyTest.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/integration/ReadyOrderClosingCancellationIT.java`

**Interfaces:**
- Consumes: `DispatchOrderPolicy.closingAt`, `OrderTransitionService.cancel` side effects.
- Produces: `OrderScheduler.cancelReadyOrdersAfterClosing()` and atomic `cancelReadyIfUnassignedAfterClosing(...)` behavior.

- [ ] **Step 1: Write failing scheduler policy tests**

Assert candidate selection includes only READY with null shipper and closing datetime `<= now`; excludes ASSIGNED, PICKED_UP, DELIVERY_FAILED, CANCELLED and 24-hour configuration. Assert iteration continues after one cancellation throws.

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -Dtest=OrderSchedulerClosingPolicyTest test`
Expected: FAIL because closing cancellation is absent.

- [ ] **Step 3: Add atomic cancellation method**

Inside one pessimistic-write transaction, recheck order is `READY`, shipper is null and computed closing datetime is not after `now`; then perform the same reservation, coupon, refund and history side effects as `cancel`, with actor `SYSTEM` and reason `Quá giờ đóng cửa chưa được điều phối`. Avoid duplicating side-effect code by adding a precondition-capable private cancellation path, not a second cancellation implementation.

- [ ] **Step 4: Extend scheduler tick**

Read config once, skip equal open/close, find candidates, process each independently. Keep existing 15-minute bank transfer and 3-hour COD pending cancellation unchanged.

- [ ] **Step 5: Run focused scheduler tests**

Run: `mvn -Dtest=OrderSchedulerClosingPolicyTest,OrderTransitionServiceTest test`
Expected: PASS.

- [ ] **Step 6: Run disposable integration test**

Against the approved disposable database, create one READY paid order and control orders in excluded states. Trigger the scheduler method directly. Assert only the candidate becomes CANCELLED; inventory reservation/coupon are released; history actor is SYSTEM; `refund_status = PENDING`; excluded orders remain unchanged. Clean all test rows.

### Task 5: Vue Dispatch Filters

**Files:**
- Modify: `frontend/src/api/staff.js`
- Modify: `frontend/src/stores/staff.js`
- Modify: `frontend/src/views/staff/DispatchPage.vue`
- Modify: `frontend/src/utils/staffKitchen.js`
- Create: `frontend/test/staff-dispatch-filters.test.js`

**Interfaces:**
- Consumes: `GET /staff/orders/dispatch?filter=...` contract.
- Produces: `staffApi.getDispatchOrders(filter)`, store dispatch state, accessible three-tab UI.

- [ ] **Step 1: Write failing frontend contract tests**

Assert the API forwards exact filter; store maps `readyAt`, `classification`, `minutesUntilClose`, counts; DispatchPage contains tablist labels, keyboard navigation, assignment only for READY tabs and `Xử lý lại` for Review.

- [ ] **Step 2: Run test and verify failure**

Run: `node --test test/staff-dispatch-filters.test.js`
Expected: FAIL because dispatch API/state/tabs are absent.

- [ ] **Step 3: Add API and store state**

Add exact request and canonical state: `dispatchItems`, `dispatchCounts`, `dispatchLoading`, `dispatchError`, request generation. Reject stale completion when filter changes or store is invalidated.

- [ ] **Step 4: Implement tabs and surfaces**

Default to Priority. Add roving tab keyboard behavior. Preserve 30-second polling and independent shipper loading. For Priority/New render current assign controls; for Review render failure reason/attempts and route link to Staff detail. Announce loading/error; add visible non-color badges and 44px targets.

- [ ] **Step 5: Run focused frontend tests**

Run: `node --test test/staff-dispatch-filters.test.js tests/staff-dispatch-policy.test.mjs tests/staff-order-concurrency.test.mjs tests/staff-kitchen-contract.test.mjs`
Expected: PASS.

### Task 6: Full Verification And Integration QA

**Files:**
- Create: `frontend/tests/e2e/staff-dispatch-filters.spec.js`
- Verify: all modified files.

**Interfaces:**
- Consumes: disposable backend/database and Staff test session.
- Produces: end-to-end evidence for filter navigation, assignment conflict and automatic cancellation visibility.

- [ ] **Step 1: Add Playwright desktop/mobile flow**

Using seeded test credentials and disposable data, verify three tabs and counts, Priority ordering, New recency, Review navigation, assignment removal from lists, responsive reflow, no console errors and successful critical API requests. Do not mutate retained `FastGuyDB`.

- [ ] **Step 2: Run OpenAPI checks**

Run: `npm run contract:lint` and frontend/backend contract tests.
Expected: PASS.

- [ ] **Step 3: Run backend tests**

Run: `mvn test`
Expected: all tests PASS.

- [ ] **Step 4: Run frontend tests and build**

Run: `npm test` then `npm run build`
Expected: all tests PASS; build exits 0.

- [ ] **Step 5: Run disposable integration and Playwright**

Run backend integration from `Backend/FastGuy-FastFoodSite` with disposable DB environment variables: `mvn -Pintegration -Dit.test=ReadyOrderClosingCancellationIT verify`. Then run frontend browser coverage with `npx playwright test tests/e2e/staff-dispatch-filters.spec.js`.
Expected: all scenarios PASS, no retained-data mutation, no console errors.

- [ ] **Step 6: Inspect final scope**

Run: `git diff --check` and inspect `git diff --stat`.
Expected: no migration, no dependency, no unrelated files, no whitespace errors.

#### Final Fix Evidence (2026-08-26)

- Runtime gate: `DuckJo/FastGuyDB_Inventory054_Test`, `ONLINE`, compatibility `160`; required Orders columns `8/8`; business hours parseable.
- TDD RED: production scheduler clock failed by seven hours under UTC JVM default; Vietnamese label/source assertions failed; executable harness safety/restoration tests failed before implementation.
- Focused GREEN: scheduler `7/7`; dispatch/harness `11/11`.
- Full backend: `441/441`; frontend: `578/578`; OpenAPI contract: `11/11`; Vite build passed; OpenAPI valid with three pre-existing availability-schema warnings.
- Disposable integration: `ReadyOrderClosingCancellationIT` `2/2`; cleanup `0 tracked rows`.
- Real backend Playwright: desktop `1/1`, mobile `1/1`; critical requests and console assertions passed. Fixture cleanup reported `0` for Orders, Users, WorkShift, Cart, CartItem, InventoryItem, InventoryTransaction, OrderStatusHistory, CouponRedemption, InventoryReservation and InventoryReservationItem; ShippingConfig restored.
- Post-run audit: listeners `0`, runtime processes `0`, Tomcat11 `Stopped`, E2E Orders `0`, E2E Users `0`; executable injected-failure environment restoration self-test passed.
- N+1 remains deferred per final review ledger; no N+1 change included.

#### Harness Reparse-Point Safety Fix Evidence (2026-08-26)

- TDD RED: executable Windows junction tests accepted both target and intermediate-ancestor junctions; the recursive removal path returned success.
- Focused GREEN: `node --test tests/staff-dispatch-real-harness.test.mjs` passed `7/7`; normal nonexistent/existing children accepted; both junction placements rejected; external victim markers survived; junctions removed with non-traversing `cmd /c rmdir` cleanup.
- Full regression: backend `441/441`; frontend `581/581`; Vite production build passed.
- Real disposable backend E2E: `BLOCKED` before startup because `DB_URL`, `DB_USER`, and `DB_PASSWORD` were absent from Process and User environments. No credential or database target was inferred; desktop/mobile were not rerun.
- Harness cleanup policy now revalidates immediately before every recursive temp deletion and before runtime creation. Reparse-point cleanup rejection is recorded as a secondary failure without replacing the primary failure.

#### Wall-Clock-Independent Fixture Fix Evidence (2026-08-26)

- Root cause reproduced at `13:59 Asia/Ho_Chi_Minh`: fixed `18:00`/`06:00` hours made active READY orders belong to an operating window already closed at `06:00`.
- TDD RED: pure fixture timeline cases at `00:05`, `05:59`, `13:59`, `23:55` failed at `05:59` (`Priority=4`) and `13:59` (`Priority=0`). GREEN passed `4/4`: each case classifies `2 Priority`, `2 New`; the independent cancellation candidate closes before `now`.
- Fixture now derives distinct hours from business time (`open=now-2h`, `close=now+2h`) and seeds the cancellation candidate two days earlier. Scheduler no longer changes the shared close hour, so active orders remain valid while only the candidate is overdue.
- Full regression: backend `445/445`; frontend `581/581`; Vite production build passed. Focused real-harness/policy tests passed within the frontend suite.
- Real backend Playwright target verified as `DuckJo/FastGuyDB_Inventory054_Test`, `ONLINE`, compatibility `160`; desktop `1/1`, mobile `1/1`; critical API, console and page-error assertions passed.
- One intermediate desktop verification failed after an unnecessary all-day shift change redirected login to `/staff/shifts`; fixture cleanup still reported all tracked rows `0` and ShippingConfig restored. That unrelated shift change was removed before the passing rerun.
- Final cleanup: Orders, Users, WorkShift, Cart, CartItem, InventoryItem, InventoryTransaction, OrderStatusHistory, CouponRedemption, InventoryReservation and InventoryReservationItem all `0`; ShippingConfig restored; ports `18080/18005/15174` listeners `0`; harness Java/cmd processes `0`; Tomcat11 `Stopped`; temp root absent; harness environment variables unset; restoration self-test passed.

#### Midnight-Safe Shift Fixture Evidence (2026-08-26)

- TDD RED: fixture Staff/shipper shifts checked through production `WorkShiftService.isValidCheckedInShift` passed at `00:05`, `05:59`, `13:59` but failed at `23:55`; wrapped `endTime=00:55` was treated as already expired.
- Production semantics also make `23:59 + 15m` wrap to `00:14`. The test-only fixture now clamps late shifts to `endTime=23:44`, whose production grace reaches `23:59`, and clamps early starts to `00:00`; normal times retain the one-hour window.
- Focused boundary suite passed `8/8`: four Staff/shipper validity cases plus four unchanged order classification/cancellation cases. Full backend passed `449/449`; frontend passed `581/581`; Vite production build passed.
- Real backend Playwright target verified as `DuckJo/FastGuyDB_Inventory054_Test`, `ONLINE`, compatibility `160`; desktop `1/1`, mobile `1/1`; critical API, console and page-error assertions passed.
- Final cleanup: all tracked fixture tables `0`; ShippingConfig restored; ports `18080/18005/15174` listeners `0`; harness Java/cmd processes `0`; Tomcat11 `Stopped`; temp root absent; harness environment variables unset; restoration self-test passed.

#### End-Of-Day Nanosecond Fixture Evidence (2026-08-26)

- TDD RED: production `WorkShiftService.isValidCheckedInShift` rejected the fixture Staff shift at `23:59:59.999999999`; `endTime=23:44` plus the 15-minute grace ended at `23:59:00`.
- A representable test-only value exists: `LocalTime.MAX.minusMinutes(15)` is `23:44:59.999999999`, and production `plusMinutes(15)` reaches exactly `LocalTime.MAX` without wrap. No production behavior changed.
- Focused boundary suite passed `9/9`: five Staff/shipper boundaries through the production helper plus four unchanged order classification/cancellation cases. Full backend passed `450/450`; frontend passed `581/581`; Vite production build passed.
- Disposable target verified on every attempt as `DuckJo/FastGuyDB_Inventory054_Test`, `ONLINE`, compatibility `160`. Real desktop E2E was blocked three times by the unrelated exact request-count assertion: expected `3` Priority loads, observed `4` because the 30-second poll completed alongside the conflict refresh; visible conflict state reached `Ưu tiên 0`. Mobile did not run because the harness stops after desktop failure.
- Every failed run cleanup reported all tracked fixture tables `0` and ShippingConfig restored. Final audit: ports `18080/18005/15174` listeners `0`; harness Java/cmd processes `0`; Tomcat11 `Stopped`; temp root absent; harness environment variables unset; restoration self-test passed.
