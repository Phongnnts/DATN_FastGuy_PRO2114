# Delivery Failure Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm workflow giao thất bại, giao lại tối đa một lần, đổi Shipper, trả cửa hàng và xử lý đúng COD, refund, waste, coupon, loyalty.

**Architecture:** Mở rộng state machine trung tâm bằng `DELIVERY_FAILED` và `RETURNED_TO_STORE`; giữ dữ liệu query hiện tại trên `Orders`, giữ audit trail đầy đủ trong `OrderStatusHistory`. Mọi mutation đi qua service có DB lock và `expectedStatus`; frontend chỉ gọi action nghiệp vụ riêng và render theo role.

**Tech Stack:** Java 17, Jakarta Servlet 6.1, JPA/Hibernate 6.6, SQL Server, JUnit 5, Vue 3, Pinia, Axios, Node test runner, Vite.

## Global Constraints

- Mỗi đơn mặc định có tối đa hai lượt giao: lượt đầu và một lượt giao lại.
- Admin override thêm lượt phải có lý do và history; không tự chuyển trạng thái.
- Sáu reason code cố định: `CUSTOMER_UNREACHABLE`, `INVALID_ADDRESS`, `CUSTOMER_RESCHEDULED`, `CUSTOMER_REJECTED`, `SHIPPER_INCIDENT`, `PRODUCT_INCIDENT`.
- `CUSTOMER_RESCHEDULED` bắt buộc lịch tương lai, trong giờ mở cửa, tối đa 24 giờ.
- Failure/retry không đổi inventory, coupon, payment, refund hoặc loyalty.
- `RETURNED_TO_STORE` mới waste hàng, release coupon và mở refund cho online `PAID`; COD giữ `UNPAID`.
- Revenue chỉ tính `DELIVERED + PAID`.
- Không thêm dependency, bảng delivery attempt, GPS, call history, image upload hoặc COD reconciliation.
- Mọi mutation bắt buộc ownership/role/shift validation, `expectedStatus`, DB lock, atomic history và rollback.

---

## File Structure

- `database/migrations/041_delivery_failure_recovery.sql`: migration production idempotent cho cột và constraints mới.
- `database/migrations/041_validate.sql`: validation sau migration.
- `database/init.sql`, `database/DB_FastGuy.sql`: schema cài mới cùng contract.
- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`: mapping trạng thái giao thất bại hiện tại.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/DeliveryFailurePolicy.java`: validation thuần cho reason, attempt, retry mode và lịch.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java`: transaction và state mutation duy nhất.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`: facade action Shipper.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`: facade action Staff.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`: `/fail` trust boundary và DTO.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`: retry/return trust boundary và DTO.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminOrderServlet.java`: override trust boundary.
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`: failure queue/history queries.
- `Frontend/src/utils/constants.js`: status/reason labels.
- `Frontend/src/api/shipper.js`, `Frontend/src/api/staff.js`, `Frontend/src/api/admin.js`: API actions.
- `Frontend/src/components/shipper/OrderActionSheet.vue`: Shipper failure dialog.
- `Frontend/src/views/shipper/OrderDetailPage.vue`: failure action/read-only state.
- `Frontend/src/views/staff/OrdersPage.vue`, `Frontend/src/views/staff/OrderDetailPage.vue`: queue và recovery actions.
- `Frontend/src/components/common/OrderTimeline.vue`: timeline phân nhánh.
- Customer/admin status consumers: safe labels, filters và terminal handling.

---

### Task 1: Database Contract and Entity Mapping

**Files:**
- Create: `database/migrations/041_delivery_failure_recovery.sql`
- Create: `database/migrations/041_validate.sql`
- Modify: `database/init.sql:225-278,431-442`
- Modify: `database/DB_FastGuy.sql:225-282,454-464`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java:128-200,281-327`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/entity/OrdersMappingTest.java`

**Interfaces:**
- Produces: `Orders#getDeliveryAttemptCount(): int`, `getDeliveryAttemptLimit(): int`, `getDeliveryFailureCode(): String`, `getDeliveryFailedAt(): LocalDateTime`, `getRetryScheduledAt(): LocalDateTime`, `getReturnedToStoreAt(): LocalDateTime` and matching setters.
- Produces DB statuses `DELIVERY_FAILED`, `RETURNED_TO_STORE` in Orders/history constraints.

- [ ] **Step 1: Write failing mapping tests**

Add reflection assertions matching existing `OrdersMappingTest` style:

```java
assertColumn("deliveryAttemptCount", "delivery_attempt_count");
assertColumn("deliveryAttemptLimit", "delivery_attempt_limit");
assertColumn("deliveryFailureCode", "delivery_failure_code");
assertColumn("deliveryFailedAt", "delivery_failed_at");
assertColumn("retryScheduledAt", "retry_scheduled_at");
assertColumn("returnedToStoreAt", "returned_to_store_at");
```

- [ ] **Step 2: Run focused test and confirm RED**

Run:

```powershell
mvn -Dtest=OrdersMappingTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: FAIL because fields do not exist.

- [ ] **Step 3: Add entity fields with defaults**

Use mappings:

```java
@Column(name = "delivery_attempt_count", nullable = false)
private int deliveryAttemptCount;

@Column(name = "delivery_attempt_limit", nullable = false)
private int deliveryAttemptLimit = 2;

@Column(name = "delivery_failure_code")
private String deliveryFailureCode;

@Column(name = "delivery_failed_at")
private LocalDateTime deliveryFailedAt;

@Column(name = "retry_scheduled_at")
private LocalDateTime retryScheduledAt;

@Column(name = "returned_to_store_at")
private LocalDateTime returnedToStoreAt;
```

Add direct getters/setters using exact interface names above.

- [ ] **Step 4: Write migration and fresh-schema constraints**

`041_delivery_failure_recovery.sql` must:

```sql
ALTER TABLE dbo.Orders ADD
    delivery_attempt_count INT NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptCount DEFAULT 0,
    delivery_attempt_limit INT NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptLimit DEFAULT 2,
    delivery_failure_code VARCHAR(40) NULL,
    delivery_failed_at DATETIME2 NULL,
    retry_scheduled_at DATETIME2 NULL,
    returned_to_store_at DATETIME2 NULL;
```

Then drop/recreate `CK_Orders_Status`, `CK_OrderStatusHistory_From`, `CK_OrderStatusHistory_To`; add:

```sql
CONSTRAINT CK_Orders_DeliveryAttempts CHECK (
  delivery_attempt_count >= 0 AND delivery_attempt_limit >= 2
  AND delivery_attempt_count <= delivery_attempt_limit
),
CONSTRAINT CK_Orders_DeliveryFailureCode CHECK (
  delivery_failure_code IS NULL OR delivery_failure_code IN (
    'CUSTOMER_UNREACHABLE','INVALID_ADDRESS','CUSTOMER_RESCHEDULED',
    'CUSTOMER_REJECTED','SHIPPER_INCIDENT','PRODUCT_INCIDENT'
  )
)
```

Make migration rerunnable using `COL_LENGTH`, `OBJECT_ID` and named-constraint checks following migration 040 conventions. Apply same final schema to `init.sql` and `DB_FastGuy.sql`.

- [ ] **Step 5: Add migration validation**

`041_validate.sql` must `THROW` when columns/constraints are absent, attempt values violate limits, or latest history differs from `Orders.order_status`. Include both new statuses in accepted sets.

- [ ] **Step 6: Run mapping test and schema static checks**

Run:

```powershell
mvn -Dtest=OrdersMappingTest test
git diff --check
```

Expected: PASS; no whitespace errors.

- [ ] **Step 7: Commit if explicitly requested**

```powershell
git add database/migrations/041_delivery_failure_recovery.sql database/migrations/041_validate.sql database/init.sql database/DB_FastGuy.sql Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java Backend/FastGuy-FastFoodSite/src/test/java/entity/OrdersMappingTest.java
git commit -m "feat(delivery): add failure recovery schema"
```

---

### Task 2: Pure Delivery Failure Policy and State Machine

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/DeliveryFailurePolicy.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/DeliveryFailurePolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java:19-28,86-109,212-218`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperActionPolicyTest.java`

**Interfaces:**
- Produces: `DeliveryFailurePolicy.REASON_CODES: Set<String>`.
- Produces: `isValidFailure(String reasonCode, String note): boolean`.
- Produces: `canRetry(int attemptCount, int attemptLimit): boolean`.
- Produces: `isValidSchedule(String retryMode, LocalDateTime scheduledAt, LocalDateTime now, LocalTime openTime, LocalTime closeTime): boolean`.
- Produces state transitions `PICKED_UP -> DELIVERY_FAILED`, `DELIVERY_FAILED -> PICKED_UP|RETURNED_TO_STORE`.

- [ ] **Step 1: Write failing policy tests**

```java
@Test
void acceptsOnlyCanonicalReasonsAndNonBlankNote() {
    assertTrue(DeliveryFailurePolicy.isValidFailure("CUSTOMER_UNREACHABLE", "Đã gọi hai lần"));
    assertFalse(DeliveryFailurePolicy.isValidFailure("OTHER", "Ghi chú"));
    assertFalse(DeliveryFailurePolicy.isValidFailure("CUSTOMER_UNREACHABLE", "   "));
}

@Test
void allowsOnlyOneDefaultRetry() {
    assertTrue(DeliveryFailurePolicy.canRetry(1, 2));
    assertFalse(DeliveryFailurePolicy.canRetry(2, 2));
}

@Test
void scheduledRetryMustBeFutureOpenAndWithinTwentyFourHours() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 12, 10, 0);
    assertTrue(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", now.plusHours(2), now,
            LocalTime.of(8, 0), LocalTime.of(22, 0)));
    assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", now.plusHours(25), now,
            LocalTime.of(8, 0), LocalTime.of(22, 0)));
}
```

Also assert state transitions and role actions in existing tests.

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
mvn -Dtest=DeliveryFailurePolicyTest,OrderTransitionServiceTest,ShipperActionPolicyTest test
```

Expected: compilation/test failure for missing policy and statuses.

- [ ] **Step 3: Implement minimal pure policy**

Use immutable `Set.of(...)`; trim note; cap note at 500; `IMMEDIATE` requires `scheduledAt == null`; `SCHEDULED` requires `now < scheduledAt <= now + 24h` and local time inside store interval. Reject unsupported retry modes.

- [ ] **Step 4: Extend central transition map and action filters**

Change transition entries to:

```java
"PICKED_UP", Set.of("DELIVERED", "DELIVERY_FAILED"),
"DELIVERY_FAILED", Set.of("PICKED_UP", "RETURNED_TO_STORE"),
"RETURNED_TO_STORE", Set.of()
```

Shipper actions retain `PICKED_UP`, `DELIVERED`, `DELIVERY_FAILED`; Staff receives `PICKED_UP` and `RETURNED_TO_STORE` only from failure state through dedicated methods, not generic mutation. Add new terminal status to cancellation guards.

- [ ] **Step 5: Run focused tests and confirm GREEN**

```powershell
mvn -Dtest=DeliveryFailurePolicyTest,OrderTransitionServiceTest,ShipperActionPolicyTest test
```

Expected: PASS.

- [ ] **Step 6: Commit if explicitly requested**

```powershell
git add Backend/FastGuy-FastFoodSite/src/main/java/service/DeliveryFailurePolicy.java Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java Backend/FastGuy-FastFoodSite/src/test/java/service/DeliveryFailurePolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperActionPolicyTest.java
git commit -m "feat(delivery): define failure recovery policy"
```

---

### Task 3: Atomic Backend Recovery Mutations

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java:118-198`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryReservationService.java:15-77`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/RefundService.java:90-103`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/DeliveryFailureMutationContractTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderHistoryAtomicitySourceTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/RefundPolicyTest.java`

**Interfaces:**
- Produces: `reportDeliveryFailure(int orderId, int shipperId, String expectedStatus, String reasonCode, String note): MutationResult`.
- Produces: `retryDelivery(int orderId, int staffId, String expectedStatus, int shipperId, String retryMode, LocalDateTime scheduledAt, String note): MutationResult`.
- Produces: `startScheduledRetry(int orderId, int staffId, String expectedStatus): MutationResult`.
- Produces: `returnToStore(int orderId, int staffId, String expectedStatus, String note): MutationResult`.
- Produces: `overrideDeliveryAttemptLimit(int orderId, int adminId, String expectedStatus, String note): MutationResult`.
- Extends `MutationResult` with `UNPROCESSABLE` for valid JSON violating business rules.

- [ ] **Step 1: Write failing source/behavior contract tests**

Assert exact method signatures and invariants:

```java
assertTrue(source.contains("reportDeliveryFailure("));
assertTrue(source.contains("order.setDeliveryAttemptCount(order.getDeliveryAttemptCount() + 1)"));
assertTrue(source.contains("order.setOrderStatus(\"DELIVERY_FAILED\")"));
assertTrue(source.contains("returnToStore("));
assertTrue(source.contains("order.setRefundStatus(\"PENDING\")"));
assertFalse(failureMethodSource.contains("inventoryReservationService.cancel"));
assertFalse(failureMethodSource.contains("releaseCoupon"));
```

Add policy test allowing refund validation for `RETURNED_TO_STORE + PAID + PENDING` while retaining `CANCELLED`.

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
mvn -Dtest=DeliveryFailureMutationContractTest,OrderHistoryAtomicitySourceTest,RefundPolicyTest test
```

- [ ] **Step 3: Implement failure mutation**

Inside one transaction: lock order; check `expectedStatus=PICKED_UP`; verify owner/shift; validate reason/note; reject attempt count at limit; increment count; set failure fields/status; clear retry schedule; persist history. Do not touch payment, reservation, coupon or loyalty.

- [ ] **Step 4: Implement immediate/scheduled retry and reassignment**

Lock order; require `DELIVERY_FAILED`; require Staff active/check-in using existing shift policy; validate attempt limit and schedule. Replace assigned Shipper only after validating role/active/current shift. `IMMEDIATE` sets `PICKED_UP`; `SCHEDULED` stores schedule and leaves status unchanged. `startScheduledRetry` requires due schedule and then sets `PICKED_UP`.

- [ ] **Step 5: Implement return and override**

`returnToStore` sets `RETURNED_TO_STORE`, `returnedToStoreAt`, clears schedule, calls inventory cancellation path so consumed reservation becomes `WASTED`, releases coupon, and sets refund pending only when payment is `PAID`. `overrideDeliveryAttemptLimit` increments limit by one, requires Admin and nonblank note, writes same-status history without state side effects.

Update `RefundService` terminal validation from only `CANCELLED` to `Set.of("CANCELLED", "RETURNED_TO_STORE")`.

- [ ] **Step 6: Run focused backend tests**

```powershell
mvn -Dtest=DeliveryFailureMutationContractTest,OrderHistoryAtomicitySourceTest,RefundPolicyTest,OrderTransitionServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit if explicitly requested**

```powershell
git add Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryReservationService.java Backend/FastGuy-FastFoodSite/src/main/java/service/RefundService.java Backend/FastGuy-FastFoodSite/src/test/java/service/DeliveryFailureMutationContractTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/OrderHistoryAtomicitySourceTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/RefundPolicyTest.java
git commit -m "feat(delivery): add atomic recovery mutations"
```

---

### Task 4: Shipper, Staff, and Admin HTTP APIs

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java:21-85`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java:24-75`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java:76-90,209-324`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java:201-335,384-469`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminOrderServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java:351-446`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/DeliveryFailureApiContractTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperOptimisticConflictContractTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperIdentityPolicyTest.java`

**Interfaces:**
- Consumes Task 3 mutation signatures.
- Produces `POST /api/shipper/orders/{id}/fail`.
- Produces `POST /api/staff/orders/{id}/retry-delivery`, `/start-scheduled-retry`, `/return-to-store`.
- Produces `POST /api/admin/orders/{id}/delivery-attempt-override`.
- DTO fields use camelCase matching entity getters.

- [ ] **Step 1: Write failing API contract tests**

Assert routes, methods, required `expectedStatus`, JSON fields, 409 mapping for `CONFLICT`, 422 mapping for `UNPROCESSABLE`, and no generic cancel reuse. Assert Shipper active list includes only assigned/picked-up actions; failed order detail remains readable by owner while Staff decides.

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
mvn -Dtest=DeliveryFailureApiContractTest,ShipperOptimisticConflictContractTest,ShipperIdentityPolicyTest test
```

- [ ] **Step 3: Add service facades and DAO queue query**

`ShipperService#fail(...)` delegates only to `reportDeliveryFailure`. `StaffOrderService` exposes retry/start/return. Add DAO query for Staff failure queue ordered by `COALESCE(retryScheduledAt, deliveryFailedAt) ASC`; do not include failed orders in Shipper active work until status returns to `PICKED_UP`.

- [ ] **Step 4: Add servlet trust-boundary validation**

Parse ID and JSON once. Require exact actor role and existing auth/shift guards. Trim strings; reject missing/invalid types as 400. Delegate business validation to policy/service and map:

```java
case SUCCESS -> 200;
case CONFLICT -> 409;
case UNPROCESSABLE -> 422;
case INVALID -> 400;
```

Preserve 403 for ownership/role/shift and 404 for absent resources using existing servlet response format.

- [ ] **Step 5: Extend DTOs without leaking internal notes**

Staff/Admin/Shipper owner DTOs include attempt count/limit, failure code, failure note, failed/retry/returned times. Public/user DTO includes status and retry schedule only; never expose `failureReason` or Shipper incident detail.

- [ ] **Step 6: Run focused API tests**

```powershell
mvn -Dtest=DeliveryFailureApiContractTest,ShipperOptimisticConflictContractTest,ShipperIdentityPolicyTest test
```

Expected: PASS.

- [ ] **Step 7: Commit if explicitly requested**

```powershell
git add Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminOrderServlet.java Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/DeliveryFailureApiContractTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperOptimisticConflictContractTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperIdentityPolicyTest.java
git commit -m "feat(delivery): expose recovery APIs"
```

---

### Task 5: Shipper Failure UI

**Files:**
- Modify: `Frontend/src/utils/constants.js:9-71`
- Modify: `Frontend/src/api/shipper.js:1-24`
- Modify: `Frontend/src/components/shipper/OrderActionSheet.vue:1-150`
- Modify: `Frontend/src/views/shipper/OrderDetailPage.vue:1-220`
- Modify: `Frontend/src/views/shipper/MyOrdersPage.vue:1-110`
- Create: `Frontend/tests/shipper-delivery-failure.test.mjs`
- Modify: `Frontend/tests/shipper-operations-contract.test.mjs`
- Modify: `Frontend/tests/shipper-app-policy.test.mjs`

**Interfaces:**
- Consumes `shipperApi.failOrder(orderId, payload)` sending `{expectedStatus, reasonCode, note}`.
- Produces status/reason labels and accessible dialog behavior.

- [ ] **Step 1: Write failing frontend contracts**

Use Node source-contract style already present. Assert:

```js
assert.match(apiSource, /failOrder.*\/fail/)
assert.match(sheetSource, /CUSTOMER_UNREACHABLE/)
assert.match(sheetSource, /reasonCode/)
assert.match(sheetSource, /role="dialog"/)
assert.match(sheetSource, /aria-live="polite"/)
assert.match(detailSource, /DELIVERY_FAILED/)
```

Also assert six labels, nonblank note validation, disabled submit and `expectedStatus` payload.

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
node --test tests/shipper-delivery-failure.test.mjs tests/shipper-operations-contract.test.mjs tests/shipper-app-policy.test.mjs
```

Workdir: `Frontend`

- [ ] **Step 3: Add constants and API method**

Add two statuses and six reason labels. Implement:

```js
failOrder(orderId, payload) {
  return api.post(`/shipper/orders/${orderId}/fail`, payload)
}
```

Match current Axios wrapper/export style.

- [ ] **Step 4: Add accessible failure dialog**

From `PICKED_UP`, show `Giao thành công` and `Báo giao thất bại`. Dialog requires reason select and note textarea; trim note; cap 500; show attempt warning. Keep form on 409, refresh canonical order, announce error, never auto-retry.

- [ ] **Step 5: Update Shipper pages**

Failed order becomes read-only; detail remains available to owner. Do not show it as active action work until Staff retries it to `PICKED_UP`. Update terminal detection to include `RETURNED_TO_STORE` and labels for both statuses.

- [ ] **Step 6: Run focused frontend tests**

```powershell
node --test tests/shipper-delivery-failure.test.mjs tests/shipper-operations-contract.test.mjs tests/shipper-app-policy.test.mjs
```

Expected: PASS.

- [ ] **Step 7: Commit if explicitly requested**

```powershell
git add Frontend/src/utils/constants.js Frontend/src/api/shipper.js Frontend/src/components/shipper/OrderActionSheet.vue Frontend/src/views/shipper/OrderDetailPage.vue Frontend/src/views/shipper/MyOrdersPage.vue Frontend/tests/shipper-delivery-failure.test.mjs Frontend/tests/shipper-operations-contract.test.mjs Frontend/tests/shipper-app-policy.test.mjs
git commit -m "feat(delivery): let shippers report failures"
```

---

### Task 6: Staff Recovery UI and Customer-Safe Status

**Files:**
- Modify: `Frontend/src/api/staff.js`
- Modify: `Frontend/src/api/admin.js`
- Modify: `Frontend/src/views/staff/OrdersPage.vue`
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue`
- Modify: `Frontend/src/components/common/OrderTimeline.vue`
- Modify: `Frontend/src/views/user/OrdersPage.vue`
- Modify: `Frontend/src/views/user/OrderDetailPage.vue`
- Modify: `Frontend/src/views/guest/TrackOrderPage.vue`
- Modify: `Frontend/src/views/admin/OrdersPage.vue`
- Modify: `Frontend/src/views/admin/OrderDetailPage.vue`
- Create: `Frontend/tests/delivery-failure-recovery-ui.test.mjs`
- Modify: `Frontend/tests/staff-kitchen-contract.test.mjs`
- Modify: `Frontend/tests/frontend-stability-a11y.test.mjs`

**Interfaces:**
- Consumes Staff endpoints from Task 4.
- Produces failure queue, immediate/scheduled retry, Shipper replacement, return confirmation and safe customer status.

- [ ] **Step 1: Write failing recovery UI tests**

Assert Staff API paths and payload fields; Staff queue/status/actions; `CUSTOMER_RESCHEDULED` scheduling requirement; customer copy exactly `Giao chưa thành công, cửa hàng đang xử lý`; no `failureReason` rendering in user/guest files; branched timeline and accessibility attributes.

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
node --test tests/delivery-failure-recovery-ui.test.mjs tests/staff-kitchen-contract.test.mjs tests/frontend-stability-a11y.test.mjs
```

- [ ] **Step 3: Add Staff/Admin API methods**

Expose:

```js
retryDelivery(orderId, payload)
startScheduledRetry(orderId, payload)
returnToStore(orderId, payload)
overrideDeliveryAttempt(orderId, payload)
```

Use exact endpoint contracts from Task 4.

- [ ] **Step 4: Add Staff failure queue and actions**

Queue card shows code label, note, failed time, attempt count and schedule. First failure allows immediate retry, scheduled retry, Shipper replacement and return. At limit, hide retry controls. Reschedule reason preselects scheduled mode. Return confirmation lists waste, coupon release and possible online refund.

- [ ] **Step 5: Add customer-safe and admin rendering**

User/guest show only safe status and schedule. Admin sees operational details and override action requiring note. Update filters, badges and terminal lists so `RETURNED_TO_STORE` is terminal and `DELIVERY_FAILED` is active exception, not completed revenue.

- [ ] **Step 6: Replace linear timeline assumption**

Render normal path plus failure event/retry loop/return terminal branch from history. Preserve cancel special case. Announce new history entries through polite live region.

- [ ] **Step 7: Run focused UI tests**

```powershell
node --test tests/delivery-failure-recovery-ui.test.mjs tests/staff-kitchen-contract.test.mjs tests/frontend-stability-a11y.test.mjs
```

Expected: PASS.

- [ ] **Step 8: Commit if explicitly requested**

```powershell
git add Frontend/src/api/staff.js Frontend/src/api/admin.js Frontend/src/views/staff/OrdersPage.vue Frontend/src/views/staff/OrderDetailPage.vue Frontend/src/components/common/OrderTimeline.vue Frontend/src/views/user/OrdersPage.vue Frontend/src/views/user/OrderDetailPage.vue Frontend/src/views/guest/TrackOrderPage.vue Frontend/src/views/admin/OrdersPage.vue Frontend/src/views/admin/OrderDetailPage.vue Frontend/tests/delivery-failure-recovery-ui.test.mjs Frontend/tests/staff-kitchen-contract.test.mjs Frontend/tests/frontend-stability-a11y.test.mjs
git commit -m "feat(delivery): add staff recovery workflow"
```

---

### Task 7: Cross-System Verification and Backlog Alignment

**Files:**
- Modify: `README.md:151-153`
- Modify: `docs/product-backlog.md:83-97`
- Modify: `docs/release-backlog.md:110-118`
- Modify only if status assumptions fail tests: dashboard/report source files identified by failures.

**Interfaces:**
- Consumes all prior tasks.
- Produces verified end-to-end workflow and current backlog status.

- [ ] **Step 1: Run full backend suite**

```powershell
mvn test
```

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: all tests PASS. Fix only failures caused by new canonical statuses/actions.

- [ ] **Step 2: Run backend package**

```powershell
mvn package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run full frontend suite**

```powershell
npm test
```

Workdir: `Frontend`

Expected: all tests PASS.

- [ ] **Step 4: Run frontend build**

```powershell
npm run build
```

Expected: Vite build succeeds.

- [ ] **Step 5: Validate migration in disposable SQL Server database**

Follow `database/migrations/RUNBOOK.md`: backup/non-production DB, apply `041_delivery_failure_recovery.sql`, then `041_validate.sql`. Verify existing rows backfill `delivery_attempt_count=0`, `delivery_attempt_limit=2`; rerun migration to prove idempotency. Never run reset script against retained data.

- [ ] **Step 6: Run manual smoke sequence**

1. Create COD order and advance to `PICKED_UP`.
2. Shipper reports `CUSTOMER_UNREACHABLE`; verify no payment/inventory/coupon mutation.
3. Staff retries with different checked-in Shipper; verify `PICKED_UP` and history.
4. Report second failure; verify Staff retry hidden/rejected.
5. Return to store; verify `RETURNED_TO_STORE`, COD `UNPAID`, one `WASTE`, coupon released, no loyalty.
6. Repeat prepaid path; verify payment remains `PAID` while failed and refund becomes `PENDING` only at return.
7. Verify guest/user sees safe text and no internal note.
8. Verify reports exclude both new statuses from revenue.

- [ ] **Step 7: Align current docs**

Update README canonical flow with failure branch. Mark delivery failure workflow complete in product/release backlog only after all checks pass. Do not rewrite older historical design/report documents.

- [ ] **Step 8: Inspect final diff**

```powershell
git diff --check
git status --short
git diff --stat
git diff -- database Backend/FastGuy-FastFoodSite/src Frontend/src Frontend/tests README.md docs/product-backlog.md docs/release-backlog.md
```

Expected: no unrelated files, no secrets, no `.superpowers/brainstorm` artifacts.

- [ ] **Step 9: Commit if explicitly requested**

```powershell
git add README.md docs/product-backlog.md docs/release-backlog.md
git commit -m "docs(delivery): close failure recovery backlog"
```

Do not commit or push unless user explicitly requests it.
