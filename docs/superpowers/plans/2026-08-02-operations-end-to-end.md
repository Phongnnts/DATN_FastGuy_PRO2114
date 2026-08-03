# FastGuy Operations End-to-End Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện luồng Kitchen → Dispatch → Shipper bằng UI vận hành rõ ràng và API nhỏ, không thêm schema hoặc trạng thái nghiệp vụ mới.

**Architecture:** Giữ Servlet → Service → DAO hiện tại. Mở rộng DTO/read policy tối thiểu ở backend, chuẩn hóa mapper Pinia ở frontend, rồi nâng từng page theo dữ liệu canonical. Polling dùng in-flight guard và giữ dữ liệu cũ khi refresh nền lỗi.

**Tech Stack:** Java 17, Jakarta Servlet, JPA/Hibernate, SQL Server, Vue 3, Pinia, Vue Router, Axios, Node test, Maven/JUnit.

## Global Constraints

- Giữ lifecycle `PENDING → CONFIRMED → PREPARING → READY → ASSIGNED → PICKED_UP → DELIVERED` và `CANCELLED`.
- Không thêm GPS, proof ảnh/OTP, auto-dispatch, kitchen station, COD settlement hoặc schema mới.
- Mọi mutation phải revalidate active role, checked-in shift, ownership và transition ở backend.
- GET lịch sử/detail terminal của Shipper được phép ngoài ca; pickup/deliver vẫn yêu cầu checked-in.
- Không thêm dependency mới.
- Không dùng `window.confirm()` cho hành động mới.
- Không sửa `README.md` hoặc file trong `docs/` ngoài spec/plan này.

---

### Task 1: Backend Shipper Workload Contract

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java`

**Interfaces:**
- Produces: `long countActiveByShipper(int shipperId)` counting `ASSIGNED` and `PICKED_UP`.
- Produces: `/api/staff/orders/shippers` items with `id`, `fullName`, `phone`, `activeOrderCount`.

- [ ] **Step 1: Write failing policy/source test**

Assert `OrdersDAO` query contains both active statuses and `StaffOrderServlet` emits `activeOrderCount`.

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -Dtest=ShipperAssignmentPolicyTest test`
Expected: FAIL because workload field/query does not exist.

- [ ] **Step 3: Add minimal DAO count**

```java
public long countActiveByShipper(int shipperId) {
    EntityManager em = DatabaseUtil.getEntityManager();
    try {
        return em.createQuery(
            "SELECT COUNT(o) FROM Orders o WHERE o.shipper.userId = :shipperId AND o.orderStatus IN ('ASSIGNED','PICKED_UP')",
            Long.class)
            .setParameter("shipperId", shipperId)
            .getSingleResult();
    } finally {
        em.close();
    }
}
```

- [ ] **Step 4: Add service/DTO workload field**

Keep current active + checked-in filtering. Add `activeOrderCount` from DAO for each returned Shipper.

- [ ] **Step 5: Run focused and full backend tests**

Run: `mvn -Dtest=ShipperAssignmentPolicyTest test` then `mvn test`
Expected: PASS.

### Task 2: Backend Shipper List and History Contract

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperIdentityPolicyTest.java`

**Interfaces:**
- Produces list fields: `orderId`, `orderCode`, `status`, `customerName`, `customerPhone`, `customerAddress`, `finalAmount`, `shippingFee`, `paymentMethod`, `paymentStatus`, `itemCount`, `assignedAt`, `pickedUpAt`, `deliveredAt`, `createdAt`.
- Produces history containing owned `DELIVERED` and `CANCELLED` orders.

- [ ] **Step 1: Add failing source contract assertions**

Assert Shipper list DTO contains all required names and history query includes both terminal statuses with deterministic ordering.

- [ ] **Step 2: Run focused test**

Run: `mvn -Dtest=ShipperIdentityPolicyTest test`
Expected: FAIL on missing fields/history status.

- [ ] **Step 3: Add DAO/service query**

Use one query scoped by `shipperId` and terminal statuses:

```java
WHERE o.shipper.userId = :shipperId
  AND o.orderStatus IN ('DELIVERED','CANCELLED')
ORDER BY COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC
```

- [ ] **Step 4: Expand list DTO**

Calculate `itemCount` as sum of item quantities, not line count. Emit null timestamps as null.

- [ ] **Step 5: Run focused/full tests**

Run: `mvn -Dtest=ShipperIdentityPolicyTest test` then `mvn test`
Expected: PASS.

### Task 3: Backend Shipper Detail Read Policy

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperShiftAccessService.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ShipperIdentityPolicyTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/StaffShiftAccessPolicyTest.java`

**Interfaces:**
- GET `/shipper/orders/{id}`: active account + ownership; checked-in required only for non-terminal order.
- PUT pickup/deliver: active account + ownership + checked-in always.
- Detail adds `items[].modifiers`, `statusHistory`, lifecycle timestamps and `allowedActions`.

- [ ] **Step 1: Write failing policy tests**

Cover terminal read outside shift allowed, active read outside shift denied, mutation outside shift denied.

- [ ] **Step 2: Run focused tests**

Run: `mvn -Dtest=ShipperIdentityPolicyTest,StaffShiftAccessPolicyTest test`
Expected: FAIL under current uniform shift gate.

- [ ] **Step 3: Split GET detail authorization from mutation authorization**

Parse path before applying checked-in guard. Load owned order; permit no-shift read only for `DELIVERED` or `CANCELLED`.

- [ ] **Step 4: Expand detail DTO**

```java
item.put("modifiers", orderItem.getModifiers());
data.put("statusHistory", historyService.getByOrderId(orderId));
data.put("assignedAt", toString(order.getAssignedAt()));
data.put("pickedUpAt", toString(order.getPickedUpAt()));
data.put("deliveredAt", toString(order.getDeliveredAt()));
data.put("allowedActions", transitionService.getAllowedActions(order.getOrderStatus(), "SHIPPER", order.getPaymentStatus()));
```

- [ ] **Step 5: Run backend verification**

Run: `mvn clean verify`
Expected: all tests and WAR build pass.

### Task 4: Frontend Staff Store Contract

**Files:**
- Modify: `Frontend/src/stores/staff.js`
- Modify: `Frontend/tests/staff-dispatch-policy.test.mjs`
- Create: `Frontend/tests/staff-kitchen-contract.test.mjs`

**Interfaces:**
- `mapOrderListItem` exposes `customerPhone`, `itemCount`, `items[].modifiers`, canonical `status`.
- `mapOrder` exposes `serviceFee`, `discount`, `allowedActions`, `internalNotes`.

- [ ] **Step 1: Add failing source-policy tests**

Assert canonical mappings and no fallback that converts customer name into user ID.

- [ ] **Step 2: Run focused tests**

Run: `node --test tests/staff-kitchen-contract.test.mjs tests/staff-dispatch-policy.test.mjs`
Expected: FAIL for missing mappings.

- [ ] **Step 3: Implement minimal mappings**

Use nullish semantics:

```js
userId: o.userId ?? null,
itemCount: Number(o.itemCount ?? (o.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)),
modifiers: Array.isArray(item.modifiers) ? item.modifiers : [],
allowedActions: Array.isArray(o.allowedActions) ? o.allowedActions : [],
```

- [ ] **Step 4: Run focused tests**

Expected: PASS.

### Task 5: Staff Kitchen Queue UX

**Files:**
- Modify: `Frontend/src/views/staff/OrdersPage.vue`
- Modify: `Frontend/tests/staff-kitchen-contract.test.mjs`

**Interfaces:**
- Consumes mapped Staff list contract from Task 4.
- URL query `tab` is canonical UI state.

- [ ] **Step 1: Add failing tests**

Assert page updates query on tab change, searches `orderCode/customerName/customerPhone`, uses `itemCount`, renders modifier names and clears poll timer.

- [ ] **Step 2: Run test and verify failure**

Run: `node --test tests/staff-kitchen-contract.test.mjs`

- [ ] **Step 3: Implement tab/query synchronization**

Use `router.replace({ query: { ...route.query, tab: activeTab } })` and watch route query for browser navigation.

- [ ] **Step 4: Implement safe polling**

Add `refreshing`, `inFlight`, `lastUpdated`, `staleError`. Silent refresh must not clear existing rows.

- [ ] **Step 5: Improve responsive ticket content**

Render customer phone, `itemCount`, waiting duration and compact modifier summary. Keep current table/card responsive pattern.

- [ ] **Step 6: Run test/build**

Run: `node --test tests/staff-kitchen-contract.test.mjs` then `npm run build`
Expected: PASS.

### Task 6: Staff Detail UX

**Files:**
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue`
- Modify: `Frontend/tests/staff-kitchen-contract.test.mjs`

**Interfaces:**
- Consumes `allowedActions`, item modifiers, service fee, discount, internal notes.

- [ ] **Step 1: Add failing assertions**

Assert phone/modifiers/internal notes render and action visibility references `allowedActions`.

- [ ] **Step 2: Run focused test**

Expected: FAIL.

- [ ] **Step 3: Render missing canonical data**

Show phone as `tel:` link, modifier list per item, service/discount rows, refund state and notes.

- [ ] **Step 4: Use backend actions and safe refresh**

Expose buttons only when action exists; disable during mutation; refetch after successful mutation. Add distinct `shipperError` and retry.

- [ ] **Step 5: Run test/build**

Expected: PASS.

### Task 7: Dispatch Workload and Polling

**Files:**
- Modify: `Frontend/src/views/staff/DispatchPage.vue`
- Modify: `Frontend/tests/staff-dispatch-policy.test.mjs`

**Interfaces:**
- Consumes `activeOrderCount` from Task 1.

- [ ] **Step 1: Add failing policy assertions**

Assert workload rendering, 30-second polling, in-flight guard, cleanup and separate retry states.

- [ ] **Step 2: Run focused test**

Expected: FAIL.

- [ ] **Step 3: Implement board refresh model**

Load ready orders and shippers independently. Preserve successful lane when the other fails. Clear selected ID if no longer in available Shippers.

- [ ] **Step 4: Render workload**

Show `Đang giao: N đơn`; sort Shippers by active count then name. Keep manual assignment.

- [ ] **Step 5: Run test/build**

Expected: PASS.

### Task 8: Frontend Shipper Store Contract

**Files:**
- Modify: `Frontend/src/stores/shipper.js`
- Create: `Frontend/tests/shipper-operations-contract.test.mjs`

**Interfaces:**
- List mapper exposes all Task 2 fields.
- Detail mapper exposes modifiers, timeline, timestamps and allowed actions.
- Store exposes loading/error separately for dashboard/list/detail.

- [ ] **Step 1: Write failing source-policy tests**

Assert list/detail fields and error state are preserved.

- [ ] **Step 2: Run test and verify failure**

Run: `node --test tests/shipper-operations-contract.test.mjs`

- [ ] **Step 3: Implement minimal mapper/state changes**

Do not swallow API errors into null. Keep prior data during silent active refresh.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

### Task 9: Shipper Dashboard and Lists

**Files:**
- Modify: `Frontend/src/views/shipper/DashboardPage.vue`
- Modify: `Frontend/src/views/shipper/MyOrdersPage.vue`
- Modify: `Frontend/tests/shipper-operations-contract.test.mjs`

**Interfaces:**
- Dashboard consumes dashboard stats + active list.
- Active page uses `/shipper/orders/active`; history uses `/shipper/orders/history`.

- [ ] **Step 1: Add failing page assertions**

Assert active endpoint use, loading/error/retry, canonical status badge, item count and lifecycle timestamps.

- [ ] **Step 2: Implement dashboard next-order card**

Load stats and active orders in parallel; render assigned/picked-up counts and next order CTA.

- [ ] **Step 3: Implement active/history list states**

Add safe 30-second polling only on active route, cleanup, search and deterministic sorting.

- [ ] **Step 4: Run focused tests/build**

Expected: PASS.

### Task 10: Shipper Detail and Layout

**Files:**
- Modify: `Frontend/src/views/shipper/OrderDetailPage.vue`
- Modify: `Frontend/src/layouts/ShipperLayout.vue`
- Modify: `Frontend/src/router/index.js`
- Modify: `Frontend/tests/shipper-operations-contract.test.mjs`
- Modify: `Frontend/tests/staff-shift-policy.test.mjs`

**Interfaces:**
- Terminal history detail route remains same `/shipper/orders/:id`; backend decides read eligibility.
- Mutation buttons rely on `allowedActions` and checked-in state.

- [ ] **Step 1: Add failing tests**

Assert timeline/modifiers, mutation guard, refetch, nested route active nav and Shipper fallback always points to `/shipper/shifts`.

- [ ] **Step 2: Enhance detail**

Add loading/error/retry, modifiers, timeline, timestamps, COD validation and `submitting` guard. Refetch after pickup/deliver.

- [ ] **Step 3: Fix layout/navigation state**

Treat `/shipper/orders/:id` as active Orders nav. Add retry for unknown shift. Use `/shipper/shifts` for all shift-check failures.

- [ ] **Step 4: Run focused tests/build**

Expected: PASS.

### Task 11: End-to-End Verification

**Files:**
- Review all changed files from Tasks 1–10.

- [ ] **Step 1: Run frontend tests**

Run: `node --test tests/*.test.mjs`
Expected: all pass.

- [ ] **Step 2: Build frontend**

Run: `npm run build`
Expected: Vite build success.

- [ ] **Step 3: Verify backend**

Run: `mvn clean verify`
Expected: all tests and WAR build pass.

- [ ] **Step 4: Check diff hygiene**

Run: `git diff --check`
Expected: no errors.

- [ ] **Step 5: Manual smoke matrix**

Verify Staff checked-in kitchen tabs/actions, dispatch assignment/workload, Shipper active/detail/pickup/deliver/history outside shift at widths `375`, `768`, `1440`.
