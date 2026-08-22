# Operating-Day Close Cutoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Áp dụng ngày vận hành và các mốc close−60/close−30/close thống nhất từ backend, tự động đóng đơn an toàn cuối ngày, tách đơn giao tồn qua đêm và khiến frontend chỉ hiển thị trạng thái authoritative từ API.

**Architecture:** `StoreConfigService` là nguồn thời gian duy nhất, dùng `Clock` và `Asia/Ho_Chi_Minh` để tạo immutable `StoreOperatingState`; servlet serialize cùng contract cho public config, checkout errors, dashboard và carry-over. DAO lọc queue theo nửa khoảng `[startOfDay, nextStartOfDay)`, còn close scheduler chọn candidate rồi gọi `OrderTransitionService` để khóa pessimistic từng order, kiểm tra lại trạng thái và ghi inventory/refund/history trong cùng transaction. Frontend chỉ render timestamps, phase và booleans từ API; kitchen/dispatch ngày mới và carry-over là hai nguồn dữ liệu riêng.

**Tech Stack:** Java 17 WAR, Jakarta Servlet 6.1, JPA/Hibernate 6.6, SQL Server, JUnit 5; OpenAPI 3.1; Vue 3 Composition API, Pinia, Axios, native Node.js tests, Vite, Playwright.

## Global Constraints

- Thứ tự triển khai bắt buộc: Database → OpenAPI/API → Backend → Frontend → integration/E2E.
- Business timezone cố định `Asia/Ho_Chi_Minh`; mọi timestamp contract dùng ISO 8601 offset datetime, ví dụ `2026-08-20T21:30:00+07:00`.
- `operatingDate` là ngày lịch local của cửa sổ vận hành; cấu hình được hỗ trợ phải thỏa `openTime < closeTime` trong cùng ngày.
- Nếu `openTime >= closeTime`, trả lỗi cấu hình ổn định, khóa checkout/assignment; không dùng logic cross-midnight hiện có để đoán operating date.
- Deliberate ceiling: chưa hỗ trợ cửa hàng mở qua nửa đêm; chỉ thêm khi product requirement định nghĩa rõ operating date qua ngày.
- Một operating date dùng chung queue, không gắn order cứng vào shift.
- Mốc warning là `closeAt.minusMinutes(60)`; mốc assignment cutoff là `closeAt.minusMinutes(30)`.
- Tại đúng boundary dùng nửa khoảng: `OPEN` khi `openAt <= now < warningAt`; `WARNING` khi `warningAt <= now < assignmentCutoffAt`; `CUTOFF` khi `assignmentCutoffAt <= now < closeAt`; `CLOSED` ngoài cửa sổ.
- `checkoutAllowed=true` chỉ trong `OPEN` hoặc `WARNING`; `assignmentAllowed=true` chỉ trong `OPEN` hoặc `WARNING`.
- Kitchen/dispatch queue chỉ lấy order có `createdAt >= startOfDay && createdAt < nextStartOfDay` của `operatingDate` authoritative.
- Carry-over chỉ gồm `ASSIGNED`/`PICKED_UP`, `assignedAt < assignmentCutoffAt`, `createdAt < todayStart`; không tính workload ngày mới.
- Sau cutoff không tạo assignment mới. Request stale sau cutoff trả HTTP `409`, code `ASSIGNMENT_CLOSED`; expected-state conflict hiện có vẫn là conflict riêng.
- Checkout user và guest sau cutoff trả HTTP `409`, code `CHECKOUT_CLOSED`, kèm `operatingState` authoritative.
- Tại close: `PENDING`/`CONFIRMED` hủy và release reservation; `PREPARING`/`READY` hủy và chuyển reservation thành `WASTED`; paid order đặt `refundStatus=PENDING`.
- Close sweep idempotent, khóa pessimistic từng order, actor `SYSTEM`, reason/history cố định; không bypass invariant transaction của `OrderTransitionService`.
- Chỉ shipper đã được assign trước cutoff trên order `ASSIGNED`/`PICKED_UP` được tiếp tục mutation đến terminal.
- Staff carry-over là read-only: endpoint GET riêng, UI không hiển thị assign/status mutation controls.
- Không thêm dependency, không đổi schema nếu Task 1 không chứng minh thiếu field/index.
- Không migration theo mặc định; không tự đặt tên migration. Nếu catalog parity fail, dừng và xin phê duyệt migration riêng.
- Không commit/push. Mỗi task kết thúc bằng `git diff --check`, `git status --short` và review diff hẹp.
- Không tuyên bố production-ready; integration/E2E chỉ là bằng chứng trên disposable/local known environment.

---

## File Map

### Create

- `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreOperatingState.java` — immutable authoritative operating state và enum phase.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OperatingDayException.java` — exception có stable API code và state authoritative.
- `Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java` — kiểm tra OpenAPI/provider serialization/error mapping.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayQueuePolicyTest.java` — kiểm tra nửa khoảng ngày và carry-over DAO/service contract.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayCloseIntegrationTest.java` — integration test close sweep trên disposable DB.
- `Frontend/src/utils/operatingDay.js` — pure display policy đọc phase/timestamp authoritative, không tự tính close.
- `Frontend/tests/operating-day-policy.test.mjs` — unit test banner/lock/read-only policy.
- `Frontend/tests/e2e/operating-day-close.spec.js` — Playwright desktop/mobile cho checkout và staff queue/carry-over.

### Modify

- `openapi/fastguy.yaml` — contract public operating state, checkout 409, staff dashboard/queues, carry-over và assignment 409.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java` — business clock, validation và `getOperatingState(Clock)`.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java` — cả user/guest checkout gọi cùng cutoff guard.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java` — map `CHECKOUT_CLOSED` thành 409 có details.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StoreConfigServlet.java` — serialize operating state trong `/api/store/config`.
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java` — date-range queue, daily counts/workload, close candidates và carry-over query.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffService.java` — dashboard ngày vận hành và state authoritative.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java` — queue ngày hiện tại, assignment cutoff và carry-over read-only.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java` — route `/carry-over`, state metadata và assignment error mapping.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java` — close-specific cancellation dưới pessimistic lock.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java` — mỗi phút chạy timeout cũ và close sweep idempotent.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java` — cho phép owner cũ tiếp tục, chặn order không hợp lệ/stale.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java` — clock/boundary/config tests.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderSchedulerTest.java` — close eligibility/idempotency tests.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java` — close inventory/refund/history tests.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryReservationPolicyTest.java` — release/waste boundary.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java` — cutoff/stale/overnight owner tests.
- `Frontend/src/api/store.js` — giữ GET `/store/config`, tiêu thụ schema mới.
- `Frontend/src/api/staff.js` — thêm GET `/staff/orders/carry-over`.
- `Frontend/src/stores/staff.js` — tách daily orders và carry-over state.
- `Frontend/src/views/user/CheckoutPage.vue` — banner warning/cutoff, disable checkout authoritative, xử lý 409.
- `Frontend/src/components/common/PublicHeader.vue` — banner close−60 authoritative cho public layout.
- `Frontend/src/views/staff/DashboardPage.vue` — daily metrics/state.
- `Frontend/src/views/staff/OrdersPage.vue` — kitchen queue ngày hiện tại.
- `Frontend/src/views/staff/DispatchPage.vue` — daily dispatch, cutoff assignment lock, carry-over link/section.
- `Frontend/src/views/staff/OrderDetailPage.vue` — read-only carry-over và cutoff assignment lock.
- `Frontend/tests/checkout-step-policy.test.mjs` — checkout authoritative policy source checks.
- `Frontend/tests/staff-dashboard-policy.test.mjs` — daily metrics/state checks.
- `Frontend/tests/staff-dispatch-policy.test.mjs` — cutoff/carry-over checks.
- `Frontend/tests/staff-kitchen-contract.test.mjs` — daily queue API contract checks.
- `Frontend/tests/staff-kitchen-helpers.test.mjs` — mapping/date-boundary fixtures.

### Explicitly unchanged unless Task 1 proves otherwise

- `database/init.sql`, `database/DB_FastGuy.sql`, `database/migrations/*` — không có schema change mặc định.
- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java` và `entity/InventoryReservation.java` — dùng các field hiện có `createdAt`, `assignedAt`, statuses, refund/history.
- `Frontend/package.json`, backend `pom.xml` — không dependency/script mới.
- Shift model — shift chỉ kiểm soát quyền staff/shipper; không trở thành owner của operating-day queue.

---

## Interfaces

### Backend value objects

```java
package service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StoreOperatingState(
        LocalDate operatingDate,
        OffsetDateTime serverNow,
        OffsetDateTime openAt,
        OffsetDateTime warningAt,
        OffsetDateTime assignmentCutoffAt,
        OffsetDateTime closeAt,
        Phase phase,
        boolean checkoutAllowed,
        boolean assignmentAllowed,
        String timezone) {
    public enum Phase { OPEN, WARNING, CUTOFF, CLOSED }
}
```

```java
public final class OperatingDayException extends IllegalStateException {
    public OperatingDayException(String code, String message, StoreOperatingState operatingState);
    public String code();
    public StoreOperatingState operatingState();
}
```

### Service/DAO signatures

```java
// StoreConfigService
public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
public StoreOperatingState getOperatingState(Clock clock);
public StoreOperatingState getOperatingState(); // delegate Clock.system(BUSINESS_ZONE)
public Map<String, Object> getPublicConfig();

// OrdersDAO: all ranges are [startInclusive, endExclusive)
public List<Orders> findByStatusAndCreatedAtRange(String status, LocalDateTime startInclusive, LocalDateTime endExclusive);
public long countByStatusAndCreatedAtRange(String status, LocalDateTime startInclusive, LocalDateTime endExclusive);
public List<Orders> findCloseCandidates(LocalDateTime startInclusive, LocalDateTime endExclusive);
public List<Orders> findCarryOverDelivery(LocalDateTime todayStart, LocalDateTime assignmentCutoff);
public long countActiveDailyByShipper(int shipperId, LocalDateTime startInclusive, LocalDateTime endExclusive);

// StaffOrderService
public List<Orders> getOrdersForOperatingDate(String status, StoreOperatingState state);
public List<Orders> getCarryOverOrders(StoreOperatingState state);
public OrderTransitionService.MutationResult assignShipper(int orderId, int shipperId, int staffId, String expectedStatus, Clock clock);

// OrderTransitionService
public CloseCancellationResult cancelAtClose(int orderId, LocalDateTime closeAt, String reason);
public record CloseCancellationResult(int orderId, boolean changed, String previousStatus, String refundStatus) {}
```

### JSON contract

`StoreOperatingState` dùng lower camel case và ISO offset datetime:

```json
{
  "operatingDate": "2026-08-20",
  "serverNow": "2026-08-20T21:30:00+07:00",
  "openAt": "2026-08-20T08:00:00+07:00",
  "warningAt": "2026-08-20T21:00:00+07:00",
  "assignmentCutoffAt": "2026-08-20T21:30:00+07:00",
  "closeAt": "2026-08-20T22:00:00+07:00",
  "phase": "CUTOFF",
  "checkoutAllowed": false,
  "assignmentAllowed": false,
  "timezone": "Asia/Ho_Chi_Minh"
}
```

Checkout cutoff response:

```json
{
  "status": "error",
  "message": "Cửa hàng đã ngừng nhận đơn hôm nay",
  "data": {
    "code": "CHECKOUT_CLOSED",
    "operatingState": {}
  }
}
```

Assignment cutoff response giống envelope trên với code `ASSIGNMENT_CLOSED`. Expected-status race giữ HTTP 409 và code `ORDER_STATE_CONFLICT`; không gộp hai trường hợp.

---

### Task 1: Read-only DB Catalog and Query Parity Gate

**Files:**
- Inspect only: `database/init.sql`
- Inspect only: `database/DB_FastGuy.sql`
- Inspect only: `database/migrations/*`
- Inspect only: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`
- Inspect only: `Backend/FastGuy-FastFoodSite/src/main/java/entity/InventoryReservation.java`
- Inspect only: `Backend/FastGuy-FastFoodSite/src/main/java/entity/OrderStatusHistory.java`
- Inspect only: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`

**Interfaces:**
- Consumes: live SQL Server catalog plus canonical SQL/JPA mappings.
- Produces: evidence that existing columns/indexes support `created_at`, `assigned_at`, `order_status`, `payment_status`, `refund_status`, reservation status và order history; hoặc stop report, không code change.

- [ ] **Step 1: Xác nhận đúng read-only target**

Run bằng account chỉ có `SELECT`/`VIEW DEFINITION`:

```powershell
sqlcmd -b -S "$env:FASTGUY_DB_SERVER" -d "$env:FASTGUY_DB_NAME" -E -Q "SET NOCOUNT ON; SELECT @@SERVERNAME AS server_name, DB_NAME() AS database_name, state_desc, compatibility_level FROM sys.databases WHERE name = DB_NAME(); SELECT HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'VIEW DEFINITION') AS can_view_definition;"
```

Expected: một row đúng server/database dự kiến, `state_desc=ONLINE`, compatibility level được ghi lại, `can_view_definition=1`. Nếu target không xác định, sai DB, không kết nối hoặc account có quy trình write-only không kiểm soát: **STOP toàn kế hoạch ở source analysis; không đoán schema, không tạo migration**.

- [ ] **Step 2: Đọc catalog columns/constraints/indexes**

```powershell
sqlcmd -b -S "$env:FASTGUY_DB_SERVER" -d "$env:FASTGUY_DB_NAME" -E -Q "SET NOCOUNT ON; SELECT t.name AS table_name,c.name AS column_name,ty.name AS type_name,c.max_length,c.is_nullable FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id JOIN sys.types ty ON ty.user_type_id=c.user_type_id WHERE t.name IN ('Orders','InventoryReservation','OrderStatusHistory') ORDER BY t.name,c.column_id; SELECT t.name AS table_name,i.name AS index_name,i.is_unique,STRING_AGG(c.name, ',') WITHIN GROUP (ORDER BY ic.key_ordinal) AS key_columns FROM sys.tables t JOIN sys.indexes i ON i.object_id=t.object_id JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE t.name IN ('Orders','InventoryReservation','OrderStatusHistory') AND i.is_hypothetical=0 GROUP BY t.name,i.name,i.is_unique ORDER BY t.name,i.name;"
```

Expected: field parity với JPA/source; index evidence được ghi nguyên trạng, không yêu cầu một tên index cụ thể.

- [ ] **Step 3: Đối chiếu canonical SQL và mapping**

Run:

```powershell
rg -n "created_at|assigned_at|order_status|payment_status|refund_status|InventoryReservation|OrderStatusHistory" database/init.sql database/DB_FastGuy.sql database/migrations Backend/FastGuy-FastFoodSite/src/main/java/entity Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java
```

Expected: không có mismatch về tên/type/nullability ảnh hưởng query. Nếu thiếu field hoặc index làm query không khả thi: **STOP**, ghi exact catalog/source mismatch và yêu cầu plan migration riêng; không invent migration filename trong feature này.

- [ ] **Step 4: Chốt no-migration gate**

Ghi vào execution notes: `DB parity PASS; no schema migration required` kèm output catalog. Không sửa DB file. Nếu chỉ thiếu index tối ưu nhưng query vẫn đúng với dữ liệu test, giữ no-migration và đo bằng integration test; không tối ưu đoán.

- [ ] **Step 5: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- database Backend/FastGuy-FastFoodSite/src/main/java/entity Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java
```

Expected: không có diff từ Task 1.

---

### Task 2: Operating-Day Policy with Injectable Clock

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreOperatingState.java`
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/OperatingDayException.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java`

**Interfaces:**
- Consumes: `business_open_time`, existing `business_close_time`, `Clock`.
- Produces: exact `StoreOperatingState` record/signatures trong Interfaces; same-day invariant; no-arg business clock delegate.

- [ ] **Step 1: Viết RED boundary tests**

Thêm tests dùng `Clock.fixed(Instant.parse(...), ZoneId.of("Asia/Ho_Chi_Minh"))` và fake config `08:00/22:00`:

```java
@Test
void operatingStateUsesInclusiveWarningAndCutoffBoundaries() {
    StoreConfigService service = configured("08:00", "22:00");
    assertState(service, "2026-08-20T13:59:59Z", StoreOperatingState.Phase.OPEN, true, true);
    assertState(service, "2026-08-20T14:00:00Z", StoreOperatingState.Phase.WARNING, true, true);
    assertState(service, "2026-08-20T14:30:00Z", StoreOperatingState.Phase.CUTOFF, false, false);
    assertState(service, "2026-08-20T15:00:00Z", StoreOperatingState.Phase.CLOSED, false, false);
}

@Test
void midnightStartsNewCalendarOperatingDateAndRemainsClosed() {
    StoreOperatingState state = configured("08:00", "22:00").getOperatingState(clock("2026-08-20T17:00:00Z"));
    assertEquals(LocalDate.of(2026, 8, 21), state.operatingDate());
    assertEquals(StoreOperatingState.Phase.CLOSED, state.phase());
}

@Test
void rejectsCrossMidnightConfigurationInsteadOfMisclassifyingOrders() {
    assertThrows(IllegalStateException.class,
        () -> configured("22:00", "02:00").getOperatingState(clock("2026-08-20T16:00:00Z")));
}
```

Test thêm exact `warningAt`, `assignmentCutoffAt`, `closeAt`, offset `+07:00`, timezone string và boundary `openAt`.

- [ ] **Step 2: Chạy RED**

```powershell
mvn -Dtest=service.StoreConfigPolicyTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: compilation FAIL vì `StoreOperatingState`, `getOperatingState(Clock)` chưa tồn tại.

- [ ] **Step 3: Implement immutable state và policy tối thiểu**

Tạo record đúng Interfaces. Trong `StoreConfigService`:

```java
public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

public StoreOperatingState getOperatingState() {
    return getOperatingState(Clock.system(BUSINESS_ZONE));
}

public StoreOperatingState getOperatingState(Clock clock) {
    Map<String, String> config = getAll();
    LocalTime open = LocalTime.parse(config.getOrDefault(OPEN_TIME, "00:00"));
    LocalTime close = LocalTime.parse(config.getOrDefault(CLOSE_TIME, "00:00"));
    if (!open.isBefore(close)) throw new IllegalStateException("INVALID_BUSINESS_HOURS");
    ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(BUSINESS_ZONE);
    LocalDate operatingDate = now.toLocalDate();
    ZonedDateTime openAt = operatingDate.atTime(open).atZone(BUSINESS_ZONE);
    ZonedDateTime closeAt = operatingDate.atTime(close).atZone(BUSINESS_ZONE);
    ZonedDateTime warningAt = closeAt.minusMinutes(60);
    ZonedDateTime cutoffAt = closeAt.minusMinutes(30);
    // Chọn phase theo exact half-open boundaries trong Global Constraints.
}
```

`getPublicConfig()` lấy một state duy nhất, không gọi `LocalTime.now()`, thêm `operatingState`; giữ `isOpen` tương thích bằng `checkoutAllowed` trong lần rollout này.

- [ ] **Step 4: Chạy GREEN và test timezone JVM khác**

```powershell
mvn -Dtest=service.StoreConfigPolicyTest -Duser.timezone=UTC test; if ($?) { mvn -Dtest=service.StoreConfigPolicyTest -Duser.timezone=America/New_York test }
```

Expected: cả hai `BUILD SUCCESS`; timestamps/state không đổi theo JVM timezone.

- [ ] **Step 5: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Backend/FastGuy-FastFoodSite/src/main/java/service/StoreOperatingState.java Backend/FastGuy-FastFoodSite/src/main/java/service/OperatingDayException.java Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java
```

Expected: chỉ bốn file Task 2; không DB/API/frontend diff.

---

### Task 3: OpenAPI Authoritative Operating State and Carry-Over Contract

**Files:**
- Modify: `openapi/fastguy.yaml`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java`

**Interfaces:**
- Consumes: `StoreOperatingState` JSON trong Interfaces.
- Produces: operations `getPublicStoreConfig`, `getStaffCarryOverOrders`; schemas `StoreOperatingState`, `OperatingConflictResponse`, `StaffOrderListResponse`; 409 codes.

- [ ] **Step 1: Viết RED contract source/parse test**

```java
@Test
void openApiDefinesAuthoritativeOperatingStateAndConflicts() throws Exception {
    String yaml = Files.readString(Path.of("../../openapi/fastguy.yaml"));
    for (String token : List.of("/store/config:", "operationId: getPublicStoreConfig",
            "/staff/orders/carry-over:", "operationId: getStaffCarryOverOrders",
            "StoreOperatingState:", "operatingDate:", "serverNow:", "warningAt:",
            "assignmentCutoffAt:", "checkoutAllowed:", "assignmentAllowed:",
            "CHECKOUT_CLOSED", "ASSIGNMENT_CLOSED", "ORDER_STATE_CONFLICT")) {
        assertTrue(yaml.contains(token), token);
    }
    assertFalse(yaml.contains("http://"));
    assertFalse(yaml.contains("https://"));
}
```

- [ ] **Step 2: Chạy RED contract lint/tests**

```powershell
mvn -Dtest=servlet.OperatingDayApiContractTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: FAIL vì paths/schemas chưa có.

- [ ] **Step 3: Thêm contract trước provider/consumer**

Trong `openapi/fastguy.yaml` khai báo:

```yaml
/store/config:
  get:
    operationId: getPublicStoreConfig
    security: []
    responses:
      '200':
        description: Public store configuration with authoritative operating state
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PublicStoreConfigResponse'
/staff/orders/carry-over:
  get:
    operationId: getStaffCarryOverOrders
    security:
      - bearerAuth: []
    responses:
      '200':
        description: Read-only delivery queue carried from prior calendar dates
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StaffOrderListResponse'
      '401': { $ref: '#/components/responses/Unauthorized' }
      '403': { $ref: '#/components/responses/Forbidden' }
```

`StoreOperatingState.required` chứa đủ chín fields; datetime fields `type: string`, `format: date-time`; `phase.enum=[OPEN, WARNING, CUTOFF, CLOSED]`; `timezone.const=Asia/Ho_Chi_Minh`. Thêm 409 cho user/guest checkout operations và assignment operation; `OperatingConflictResponse.data.required=[code, operatingState]`, code enum chứa `CHECKOUT_CLOSED`, `ASSIGNMENT_CLOSED`, `ORDER_STATE_CONFLICT`.

- [ ] **Step 4: Chạy GREEN contract lint**

```powershell
mvn -Dtest=servlet.OperatingDayApiContractTest test
```

Expected: `BUILD SUCCESS`.

```powershell
npm run contract:lint
```

Workdir: `Frontend`.

Expected: Redocly exit code 0, không unresolved/local/remote `$ref` error.

- [ ] **Step 5: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- openapi/fastguy.yaml Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java
```

Expected: chỉ contract/test Task 3.

---

### Task 4: Checkout Cutoff Enforcement for User and Guest

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StoreConfigServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java`

**Interfaces:**
- Consumes: `StoreConfigService.getOperatingState(Clock)`, code `CHECKOUT_CLOSED`.
- Produces: cùng guard cho `OrderService.checkout(...)` và `guestCheckout(...)`; `/api/store/config` state authoritative; HTTP 409 exact envelope.

- [ ] **Step 1: Viết RED user/guest and servlet mapping tests**

Tests phải assert cả hai checkout paths gọi cùng helper, boundary `warningAt−1s` allowed, đúng `assignmentCutoffAt` rejected, và serializer chứa all fields:

```java
@Test
void checkoutConflictIs409WithStableCodeAndAuthoritativeState() throws Exception {
    OperatingDayException error = new OperatingDayException("CHECKOUT_CLOSED", "Cửa hàng đã ngừng nhận đơn hôm nay", cutoffState());
    ResponseCapture response = invokeCheckoutError(error);
    assertEquals(409, response.status());
    assertEquals("CHECKOUT_CLOSED", response.json().path("data").path("code").asText());
    assertEquals("2026-08-20T21:30:00+07:00",
        response.json().path("data").path("operatingState").path("serverNow").asText());
}
```

- [ ] **Step 2: Chạy RED**

```powershell
mvn -Dtest=service.StoreConfigPolicyTest,servlet.OperatingDayApiContractTest test
```

Expected: FAIL vì checkout vẫn dùng `LocalTime.now()` và maps `IllegalArgumentException` thành 400.

- [ ] **Step 3: Dùng một backend guard trước mọi write**

Thay `validateBusinessHoursAndGetServiceFee` bằng helper nhận state/clock:

```java
private BigDecimal validateCheckoutAndGetServiceFee(Map<String, String> config, Clock clock) {
    StoreOperatingState state = storeConfigService.getOperatingState(clock);
    if (!state.checkoutAllowed()) {
        throw new OperatingDayException("CHECKOUT_CLOSED", "Cửa hàng đã ngừng nhận đơn hôm nay", state);
    }
    return StoreConfigService.parseFee(config.get(StoreConfigService.SERVICE_FEE));
}
```

Cả authenticated `checkout(...)` và `guestCheckout(...)` phải gọi helper trước reserve/order persist. No-arg public methods delegate business clock; package-private overload nhận `Clock` phục vụ deterministic tests.

- [ ] **Step 4: Map exception riêng, không bắt vào generic 400**

Trong cả user và guest branches của `OrderServlet`, catch `OperatingDayException` trước `IllegalArgumentException`, trả:

```java
ApiResponse.error(resp, e.getMessage(), 409,
        Map.of("code", e.code(), "operatingState", e.operatingState()));
```

`StoreConfigServlet` trả config với nested `operatingState` đúng OpenAPI. Không để frontend tự parse `closeTime`.

- [ ] **Step 5: Chạy GREEN focused tests**

```powershell
mvn -Dtest=service.StoreConfigPolicyTest,servlet.OperatingDayApiContractTest test
```

Expected: `BUILD SUCCESS`; user/guest tại cutoff đều 409, warning vẫn allowed.

- [ ] **Step 6: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StoreConfigServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java
```

Expected: không frontend/DB diff.

---

### Task 5: Daily Queues, Dashboard, Workload and Assignment Cutoff

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayQueuePolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java`

**Interfaces:**
- Consumes: operating state/date bounds; existing checked-in shift authorization và expected status.
- Produces: exact DAO/service signatures; daily dashboard counts; `ASSIGNMENT_CLOSED` distinct from `ORDER_STATE_CONFLICT`.

- [ ] **Step 1: Viết RED DAO half-open range tests**

Test source/fake DAO phải assert JPQL có cả bounds:

```java
@Test
void dailyQueuesUseCreatedAtHalfOpenRange() throws Exception {
    String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
    assertTrue(dao.contains("o.createdAt >= :start"));
    assertTrue(dao.contains("o.createdAt < :end"));
    assertTrue(dao.contains("findByStatusAndCreatedAtRange"));
}
```

Test fixtures: `2026-08-20T00:00` included; `2026-08-21T00:00` excluded. Dashboard status counts và priority/overdue/awaiting shipper dùng same range. `countActiveDailyByShipper` không dùng `shiftStart`.

- [ ] **Step 2: Viết RED assignment boundary/race tests**

Trong `ShipperAssignmentPolicyTest`:

```java
@Test
void assignmentAllowedOneSecondBeforeCutoffButRejectedAtCutoff() {
    assertEquals(SUCCESS, assignAt("2026-08-20T14:29:59Z"));
    assertEquals(ASSIGNMENT_CLOSED, assignAt("2026-08-20T14:30:00Z"));
}

@Test
void expectedStatusRaceRemainsDistinctFromClosedAssignment() {
    assertEquals(CONFLICT, assignReadyOrderWithExpectedStatus("PREPARING"));
}
```

Nếu `MutationResult` hiện chưa có `ASSIGNMENT_CLOSED`, thêm enum value đó; servlet map `ASSIGNMENT_CLOSED` sang code cùng tên, map `CONFLICT` sang `ORDER_STATE_CONFLICT`.

- [ ] **Step 3: Chạy RED**

```powershell
mvn -Dtest=service.OperatingDayQueuePolicyTest,service.ShipperAssignmentPolicyTest test
```

Expected: compilation/assertion FAIL vì methods/range/result chưa tồn tại.

- [ ] **Step 4: Implement DAO ranges và replace global queue/count calls**

Tạo bounds từ state bằng local datetime của `operatingDate.atStartOfDay()` và `.plusDays(1).atStartOfDay()`. JPQL:

```java
SELECT o FROM Orders o
WHERE o.orderStatus = :status
  AND o.createdAt >= :start
  AND o.createdAt < :end
ORDER BY o.createdAt ASC, o.orderId ASC
```

`StaffOrderService.getPendingOrders/getConfirmedOrders/getPreparingOrders/getReadyOrders` delegate `getOrdersForOperatingDate(status, state)`. `StaffService.getDashboard` thay global `countByStatus`, `findPriorityOrders`, `countOverdueActive` và `READY` count bằng range variants. Shift summary vẫn hiển thị riêng nhưng không định nghĩa queue.

- [ ] **Step 5: Enforce assignment cutoff ở service trust boundary**

`assignShipper(..., Clock clock)` lấy state ngay trước transition; nếu `!assignmentAllowed` trả `ASSIGNMENT_CLOSED`. No-arg servlet path truyền business clock. Sau check, canonical transition vẫn pessimistic-lock/expectedStatus; không bỏ checked-in shipper validation.

Servlet 409 payload:

```java
Map.of("code", "ASSIGNMENT_CLOSED", "operatingState", storeConfigService.getOperatingState())
```

Expected-state conflict:

```java
Map.of("code", "ORDER_STATE_CONFLICT")
```

- [ ] **Step 6: Chạy GREEN focused tests**

```powershell
mvn -Dtest=service.OperatingDayQueuePolicyTest,service.ShipperAssignmentPolicyTest test
```

Expected: `BUILD SUCCESS`; exact midnight/cutoff/race assertions pass.

- [ ] **Step 7: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java Backend/FastGuy-FastFoodSite/src/main/java/service/StaffService.java Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayQueuePolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java
```

Expected: daily query không còn gọi `findByStatus()`/global count cho kitchen/dispatch/dashboard.

---

### Task 6: Idempotent Close Sweep, Inventory, Refund and History

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderSchedulerTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryReservationPolicyTest.java`

**Interfaces:**
- Consumes: `findCloseCandidates(start,end)`, `cancelAtClose(orderId, closeAt, reason)`.
- Produces: one locked transaction/order; exact reason `Đóng ngày vận hành`; paid refund pending; PREPARING/READY wasted; PENDING/CONFIRMED released.

- [ ] **Step 1: Viết RED close eligibility/boundary tests**

```java
@Test
void closeSweepSelectsOnlyNonDeliveryStatusesFromOperatingDate() {
    for (String status : List.of("PENDING", "CONFIRMED", "PREPARING", "READY"))
        assertTrue(OrderScheduler.shouldClose(status));
    for (String status : List.of("ASSIGNED", "PICKED_UP", "DELIVERED", "CANCELLED"))
        assertFalse(OrderScheduler.shouldClose(status));
}

@Test
void sweepRunsAtCloseAndNotBefore() {
    assertFalse(OrderScheduler.shouldRunCloseSweep(stateAt("21:59:59")));
    assertTrue(OrderScheduler.shouldRunCloseSweep(stateAt("22:00:00")));
}
```

Candidate range excludes prior/future date; exact midnight next day is excluded.

- [ ] **Step 2: Viết RED transition side-effect tests**

Test matrix:

| from | reservation trước | reservation sau | transaction | paid refund |
|---|---|---|---|---|
| PENDING | RESERVED | RELEASED | RELEASE | PENDING nếu PAID |
| CONFIRMED | RESERVED | RELEASED | RELEASE | PENDING nếu PAID |
| PREPARING | CONSUMED | WASTED | WASTE | PENDING nếu PAID |
| READY | CONSUMED | WASTED | WASTE | PENDING nếu PAID |

Assert history: actorUserId null, actorRole `SYSTEM`, from status, to `CANCELLED`, reason `Đóng ngày vận hành`, timestamp close execution. Assert rerun returns `changed=false`, không thêm history/inventory transaction/refund duplicate.

- [ ] **Step 3: Chạy RED**

```powershell
mvn -Dtest=service.OrderSchedulerTest,service.OrderTransitionServiceTest,service.InventoryReservationPolicyTest test
```

Expected: FAIL vì `cancelAtClose`, `shouldRunCloseSweep`, close-specific policy chưa có.

- [ ] **Step 4: Implement close-specific canonical transition**

Trong `cancelAtClose`:

```java
em.getTransaction().begin();
Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
if (order == null || !Set.of("PENDING", "CONFIRMED", "PREPARING", "READY").contains(order.getOrderStatus())) {
    em.getTransaction().rollback();
    return new CloseCancellationResult(orderId, false, order == null ? null : order.getOrderStatus(),
            order == null ? null : order.getRefundStatus());
}
```

Sau lock, re-check `createdAt < closeAt` và thuộc operating-day range truyền vào hoặc xác nhận candidate metadata; không hủy order mới/ngày khác. Gọi inventory service trong cùng transaction với explicit mode theo from status: release cho PENDING/CONFIRMED, waste cho PREPARING/READY. Set `CANCELLED`, `cancelledAt`, `cancelledBy=SYSTEM`, reason; nếu `paymentStatus=PAID` và refund chưa completed thì `refundStatus=PENDING`; persist một history; commit. Rollback active transaction và close owned `EntityManager` khi lỗi.

- [ ] **Step 5: Implement scheduler mà không phá timeout cũ**

Mỗi tick lấy một `StoreOperatingState`. Giữ bank PENDING UNPAID >15 phút và COD PENDING UNPAID >3h. Nếu phase `CLOSED` và `serverNow >= closeAt`, query candidate rồi gọi `cancelAtClose` từng ID. Idempotency dựa trên locked terminal re-check, không cần scheduler memory/schema marker; nhiều app instances vẫn an toàn.

Không dùng `printStackTrace`; dùng logging convention hiện có nếu có. Một order lỗi không chặn order sau.

- [ ] **Step 6: Chạy GREEN focused tests**

```powershell
mvn -Dtest=service.OrderSchedulerTest,service.OrderTransitionServiceTest,service.InventoryReservationPolicyTest,service.OrderCancellationPolicyTest,service.OrderHistoryAtomicitySourceTest test
```

Expected: `BUILD SUCCESS`; timeout guard UNPAID cũ vẫn pass; close matrix/idempotency pass.

- [ ] **Step 7: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java Backend/FastGuy-FastFoodSite/src/test/java/service/OrderSchedulerTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryReservationPolicyTest.java
```

Expected: không bypass `OrderTransitionService`; không scheduler-global transaction.

---

### Task 7: Overnight Shipper Continuation and Staff Read-Only Carry-Over

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayQueuePolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java`

**Interfaces:**
- Consumes: `findCarryOverDelivery(todayStart, assignmentCutoff)`, existing shipper ownership/transition invariants.
- Produces: GET `/api/staff/orders/carry-over`; read-only items; owner shipper may continue `ASSIGNED → PICKED_UP → terminal` overnight.

- [ ] **Step 1: Viết RED carry-over query tests**

Assert exact predicate:

```sql
o.orderStatus IN ('ASSIGNED','PICKED_UP')
AND o.assignedAt < :assignmentCutoff
AND o.createdAt < :todayStart
```

Fixtures: prior-day ASSIGNED at cutoff−1s included; exactly cutoff excluded; PICKED_UP included; READY excluded; today-created excluded; unassigned excluded.

- [ ] **Step 2: Viết RED overnight owner mutation tests**

```java
@Test
void assignedBeforeCutoffOwnerMayPickUpAfterMidnight() {
    assertEquals(SUCCESS, shipperPickUp(priorDayAssignedOrder(), ownerId, nextDayClock()));
}

@Test
void nonOwnerOrStaleAssignmentCannotContinue() {
    assertNotEquals(SUCCESS, shipperPickUp(priorDayAssignedOrder(), otherShipperId, nextDayClock()));
    assertNotEquals(SUCCESS, shipperPickUp(assignedAtCutoffOrder(), ownerId, nextDayClock()));
}
```

Delivery/failure terminal actions từ `PICKED_UP` giữ allowed action/payment checks hiện có.

- [ ] **Step 3: Chạy RED**

```powershell
mvn -Dtest=service.OperatingDayQueuePolicyTest,service.ShipperAssignmentPolicyTest,servlet.OperatingDayApiContractTest test
```

Expected: FAIL vì carry-over route/query và overnight eligibility chưa có.

- [ ] **Step 4: Implement read-only carry-over endpoint**

`StaffOrderServlet.doGet` match `path.equals("/carry-over")` trước dynamic order ID parsing; giữ staff auth/checked-in shift gate. Response list item dùng serializer hiện có nhưng thêm `queueType: CARRY_OVER`, `readOnly: true`, `operatingState`. Không tạo POST/PUT route cho carry-over.

- [ ] **Step 5: Preserve shipper continuation under canonical transition**

Trong shipper mutation validation, owner/status là nguồn quyền: order phải có shipper ID trùng actor; ASSIGNED/PICKED_UP; `assignedAt` không null và trước cutoff của original created date. Không yêu cầu order thuộc ngày hiện tại; không tính carry-over vào `countActiveDailyByShipper`. Vẫn yêu cầu checked-in shift theo invariant hiện có nếu canonical transition đang yêu cầu; không nới role/account checks.

- [ ] **Step 6: Chạy GREEN focused tests**

```powershell
mvn -Dtest=service.OperatingDayQueuePolicyTest,service.ShipperAssignmentPolicyTest,servlet.OperatingDayApiContractTest,service.OrderTransitionServiceTest test
```

Expected: `BUILD SUCCESS`; overnight owner success, non-owner/stale blocked, route GET-only.

- [ ] **Step 7: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayQueuePolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperAssignmentPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/OperatingDayApiContractTest.java
```

Expected: không shift/order FK mới; carry-over không xuất hiện trong daily workload.

---

### Task 8: Frontend Authoritative Banner and Checkout Lock

**Files:**
- Create: `Frontend/src/utils/operatingDay.js`
- Create: `Frontend/tests/operating-day-policy.test.mjs`
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Modify: `Frontend/src/components/common/PublicHeader.vue`
- Modify: `Frontend/tests/checkout-step-policy.test.mjs`
- Inspect only: `Frontend/src/api/store.js`

**Interfaces:**
- Consumes: OpenAPI `PublicStoreConfigResponse.operatingState`; 409 `CHECKOUT_CLOSED`.
- Produces: pure `operatingBanner(state)` và `checkoutDisabled(state)`; accessible warning/cutoff UI; no local close calculation.

- [ ] **Step 1: Viết RED pure policy tests**

```js
import { operatingBanner, checkoutDisabled } from '../src/utils/operatingDay.js';

test('WARNING displays authoritative close timestamp without deriving cutoff', () => {
  const state = fixture({ phase: 'WARNING', checkoutAllowed: true });
  assert.deepEqual(operatingBanner(state), {
    tone: 'warning',
    text: 'Cửa hàng ngừng nhận đơn lúc 21:30',
  });
  assert.equal(checkoutDisabled(state), false);
});

test('CUTOFF and CLOSED disable checkout', () => {
  assert.equal(checkoutDisabled(fixture({ phase: 'CUTOFF', checkoutAllowed: false })), true);
  assert.equal(checkoutDisabled(fixture({ phase: 'CLOSED', checkoutAllowed: false })), true);
});
```

Fixture có serverNow/assignmentCutoffAt/closeAt ISO offset. Test source assert không có `minus`, `setMinutes`, `closeTime` parsing trong utility/components.

- [ ] **Step 2: Chạy RED**

```powershell
node --test tests/operating-day-policy.test.mjs tests/checkout-step-policy.test.mjs
```

Workdir: `Frontend`.

Expected: FAIL `ERR_MODULE_NOT_FOUND` cho `operatingDay.js` hoặc missing authoritative assertions.

- [ ] **Step 3: Implement utility native-only**

```js
export function checkoutDisabled(state) {
  return !state || state.checkoutAllowed !== true;
}

export function operatingBanner(state) {
  if (!state) return null;
  const cutoff = new Intl.DateTimeFormat('vi-VN', {
    timeZone: state.timezone,
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(state.assignmentCutoffAt));
  if (state.phase === 'WARNING') return { tone: 'warning', text: `Cửa hàng ngừng nhận đơn lúc ${cutoff}` };
  if (state.phase === 'CUTOFF' || state.phase === 'CLOSED') return { tone: 'closed', text: 'Cửa hàng đã ngừng nhận đơn hôm nay' };
  return null;
}
```

Không thêm dependency. Utility chỉ format timestamp được server cấp.

- [ ] **Step 4: Wire public header và checkout**

`CheckoutPage.vue` thay `isOpen/closeTime` decision bằng nested `operatingState.checkoutAllowed`; disable submit/button ngay khi false; banner dùng `role="status"`, cutoff error dùng `role="alert"`; giữ keyboard/focus/loading. Khi POST trả 409 code `CHECKOUT_CLOSED`, replace local state bằng `error.response.data.data.operatingState`, hiển thị message và không tự retry.

`PublicHeader.vue` tải/reuse public config theo pattern hiện có; render warning từ close−60 và closed/cutoff state. Nếu fetch fail, không đoán giờ; checkout vẫn fail-safe disabled khi state absent, các trang browse không bị khóa.

- [ ] **Step 5: Chạy GREEN focused tests**

```powershell
node --test tests/operating-day-policy.test.mjs tests/checkout-step-policy.test.mjs
```

Expected: all tests pass; source assertions xác nhận không parse `closeTime` để quyết định.

- [ ] **Step 6: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Frontend/src/utils/operatingDay.js Frontend/tests/operating-day-policy.test.mjs Frontend/src/views/user/CheckoutPage.vue Frontend/src/components/common/PublicHeader.vue Frontend/tests/checkout-step-policy.test.mjs
```

Expected: không API path/dependency mới; warning accessible, checkout fail-safe.

---

### Task 9: Staff Daily Queues, Carry-Over UI and Assignment Lock

**Files:**
- Modify: `Frontend/src/api/staff.js`
- Modify: `Frontend/src/stores/staff.js`
- Modify: `Frontend/src/views/staff/DashboardPage.vue`
- Modify: `Frontend/src/views/staff/OrdersPage.vue`
- Modify: `Frontend/src/views/staff/DispatchPage.vue`
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue`
- Modify: `Frontend/tests/staff-dashboard-policy.test.mjs`
- Modify: `Frontend/tests/staff-dispatch-policy.test.mjs`
- Modify: `Frontend/tests/staff-kitchen-contract.test.mjs`
- Modify: `Frontend/tests/staff-kitchen-helpers.test.mjs`

**Interfaces:**
- Consumes: GET `/staff/orders/carry-over`, daily queue responses, dashboard `operatingState`, 409 codes.
- Produces: `carryOverOrders` separate from `allOrders`; read-only section “Đơn giao tồn cuối ngày”; assignment disabled authoritative.

- [ ] **Step 1: Viết RED API/store separation tests**

Assertions cụ thể:

```js
assert.match(apiSource, /getCarryOverOrders\(\).*client\.get\('\/staff\/orders\/carry-over'\)/s);
assert.match(storeSource, /const carryOverOrders = ref\(\[\]\)/);
assert.doesNotMatch(storeSource, /allOrders\.value\s*=.*carryOver/s);
```

Test mapping giữ `assignedAt`, `queueType`, `readOnly`, `operatingState`. Kitchen contract chỉ gọi daily PENDING/CONFIRMED/PREPARING/READY endpoints.

- [ ] **Step 2: Viết RED UI policy tests**

`staff-dispatch-policy.test.mjs` assert exact label `Đơn giao tồn cuối ngày`, carry-over empty/loading/error states, không assign button trong carry-over, assignment controls dùng `operatingState.assignmentAllowed`. Dashboard policy assert daily counts không cộng carry-over; hiển thị separate carry-over count/link.

- [ ] **Step 3: Chạy RED**

```powershell
node --test tests/staff-dashboard-policy.test.mjs tests/staff-dispatch-policy.test.mjs tests/staff-kitchen-contract.test.mjs tests/staff-kitchen-helpers.test.mjs
```

Workdir: `Frontend`.

Expected: FAIL vì API/store/UI carry-over chưa có.

- [ ] **Step 4: Add API/store separate state**

Trong `staff.js` API:

```js
getCarryOverOrders() {
  return client.get('/staff/orders/carry-over');
}
```

Trong Pinia thêm `carryOverOrders`, `fetchCarryOverOrders()`, separate loading/error nếu convention cho phép; không merge vào `allOrders`, không dùng carry-over cho dashboard workload. Preserve stale-request version guard.

- [ ] **Step 5: Render daily and carry-over surfaces**

- `DashboardPage.vue`: daily cards từ server; separate carry-over count/link; render `updatedAt/serverNow` authoritative.
- `OrdersPage.vue`: chỉ kitchen orders ngày hiện tại; không client-filter ngày bằng `new Date()`.
- `DispatchPage.vue`: READY daily queue; assignment button disabled khi `assignmentAllowed=false`; carry-over section read-only với link detail.
- `OrderDetailPage.vue`: nếu `queueType=CARRY_OVER`/`readOnly=true`, ẩn assign/status mutation controls, giữ contact/order details. Nếu assignment 409 `ASSIGNMENT_CLOSED`, update operating state từ response và announce error.

- [ ] **Step 6: Chạy GREEN focused tests**

```powershell
node --test tests/staff-dashboard-policy.test.mjs tests/staff-dispatch-policy.test.mjs tests/staff-kitchen-contract.test.mjs tests/staff-kitchen-helpers.test.mjs tests/operating-day-policy.test.mjs
```

Expected: all tests pass; daily/carry-over state tách biệt.

- [ ] **Step 7: Review hẹp**

```powershell
git diff --check; git status --short; git diff -- Frontend/src/api/staff.js Frontend/src/stores/staff.js Frontend/src/views/staff/DashboardPage.vue Frontend/src/views/staff/OrdersPage.vue Frontend/src/views/staff/DispatchPage.vue Frontend/src/views/staff/OrderDetailPage.vue Frontend/tests/staff-dashboard-policy.test.mjs Frontend/tests/staff-dispatch-policy.test.mjs Frontend/tests/staff-kitchen-contract.test.mjs Frontend/tests/staff-kitchen-helpers.test.mjs
```

Expected: không client-derived operating date/cutoff; carry-over mutation controls absent.

---

### Task 10: Disposable Integration, Desktop/Mobile E2E and Full Verification

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/OperatingDayCloseIntegrationTest.java`
- Create: `Frontend/tests/e2e/operating-day-close.spec.js`
- Verify: all files in File Map

**Interfaces:**
- Consumes: complete DB/API/backend/frontend contracts.
- Produces: evidence cho close−60/close−30/close/midnight, refund, release/waste, stale assignment, overnight delivery, desktop/mobile; no production claim.

- [ ] **Step 1: Viết RED disposable DB integration matrix**

Test setup tạo data riêng trong transaction/disposable DB, không dùng retained/prod. Cases:

```java
@ParameterizedTest
@CsvSource({
  "PENDING,RESERVED,RELEASED,RELEASE",
  "CONFIRMED,RESERVED,RELEASED,RELEASE",
  "PREPARING,CONSUMED,WASTED,WASTE",
  "READY,CONSUMED,WASTED,WASTE"
})
void closeSweepCancelsWithExactInventoryEffect(String status, String before, String after, String transactionType) {}

@Test void paidCloseCancellationCreatesPendingRefundOnce() {}
@Test void rerunningSweepCreatesNoSecondHistoryInventoryTransactionOrRefund() {}
@Test void midnightDailyQueueExcludesPriorDayAndCarryOverIncludesEligibleDelivery() {}
@Test void assignmentRequestStartedBeforeButLockedAfterCutoffReturnsAssignmentClosed() {}
@Test void priorOwnerCompletesPickedUpOrderAfterMidnight() {}
```

- [ ] **Step 2: Chạy RED integration test trên known disposable target**

```powershell
mvn -Dtest=service.OperatingDayCloseIntegrationTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`; env phải trỏ disposable/local test DB đã được xác nhận bằng Task 1 query.

Expected: test FAIL trước khi fixtures/provider wiring hoàn tất; nếu không có disposable DB hoặc target không chắc chắn: STOP, báo `OperatingDayCloseIntegrationTest not run: disposable DB unavailable`, không chạy trên retained DB.

- [ ] **Step 3: Hoàn thiện minimal integration fixture và chạy GREEN**

Dùng existing persistence/test fixture convention; inject fixed clocks, không sleep. Cleanup theo transaction rollback hoặc unique test IDs. Không chạy DDL. Chạy lại:

```powershell
mvn -Dtest=service.OperatingDayCloseIntegrationTest test
```

Expected: `BUILD SUCCESS`; mỗi case assert DB rows/history/count cụ thể.

- [ ] **Step 4: Viết Playwright RED desktop/mobile flow**

`operating-day-close.spec.js` dùng API fixture/known test controls của environment, không sửa system clock bằng arbitrary sleep. Assertions:

```js
test('warning, checkout cutoff and carry-over are authoritative', async ({ page }) => {
  // close−60: role=status banner visible, checkout enabled
  // close−30: checkout/assign disabled; forced stale API mutation returns 409 stable code
  // close: eligible kitchen orders cancelled; carry-over delivery remains read-only
  // midnight: kitchen daily queue excludes yesterday; carry-over remains visible
});
```

Capture page errors/console errors; assert critical `/api/store/config`, checkout, staff daily và carry-over requests success/expected 409. Locators dùng role/label/test-id; condition waits only.

- [ ] **Step 5: Chạy RED E2E trên từng project**

```powershell
npm run test:e2e -- --project=desktop-chrome tests/e2e/operating-day-close.spec.js
```

```powershell
npm run test:e2e -- --project=mobile-chrome tests/e2e/operating-day-close.spec.js
```

Workdir: `Frontend`, known local environment (`playwright.config.js` base URL hoặc explicit approved URL).

Expected: FAIL trước khi selectors/test-state controls hoàn tất. Không chạy nếu environment không có deterministic operating-state fixture; báo exact blocker thay vì mock UI rồi gọi integration.

- [ ] **Step 6: Hoàn thiện E2E fixture và chạy GREEN desktop/mobile**

Dùng existing auth/storage/test data convention; seed qua approved disposable test setup, không SQL write qua catalog connection. Re-run hai lệnh Step 5.

Expected: cả `desktop-chrome` và `mobile-chrome` pass; zero uncaught page error/console error; critical requests trả expected 2xx/409.

- [ ] **Step 7: Chạy toàn bộ backend verification**

```powershell
mvn test
```

```powershell
mvn package
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: `BUILD SUCCESS` cho cả hai. Phải gồm focused tests `StoreConfigPolicyTest`, `OrderSchedulerTest`, `OrderTransitionServiceTest`, `InventoryReservationPolicyTest`, `ShipperAssignmentPolicyTest`, OpenAPI/provider contract và integration test.

- [ ] **Step 8: Chạy toàn bộ frontend/contract verification**

```powershell
npm test
```

```powershell
npm run contract:lint
```

```powershell
npm run build
```

```powershell
npm run test:e2e
```

Workdir: `Frontend`, E2E trên known environment.

Expected: Node tests all pass, Redocly exit 0, Vite build success, Playwright desktop/mobile pass. `npm test` phải gồm `checkout-step-policy.test.mjs`, `staff-dashboard-policy.test.mjs`, `staff-dispatch-policy.test.mjs`, `staff-kitchen-contract.test.mjs`, `staff-kitchen-helpers.test.mjs`, `operating-day-policy.test.mjs`.

- [ ] **Step 9: Final diff/status review, không commit**

```powershell
git diff --check; git status --short; git diff --stat; git diff -- database openapi Backend/FastGuy-FastFoodSite/src/main/java Backend/FastGuy-FastFoodSite/src/test/java Frontend/src Frontend/tests
```

Expected: `git diff --check` exit 0; không DB/migration diff; chỉ files trong File Map; không secret, generated `dist`, Playwright report/profile, commit hoặc push. Nếu bất kỳ verification nào fail, ghi exact command/error và không tuyên bố hoàn tất hay production-ready.
