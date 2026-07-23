# Staff Shift Gating Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chỉ cho Staff đã check-in xem dashboard và đơn đang xử lý; vẫn cho xem ca làm, lịch sử đơn, hỗ trợ; chỉ cho check-out từ giờ kết thúc ca.

**Architecture:** `WorkShiftService` cung cấp một policy trạng thái ca dùng chung và kiểm tra checkout ở backend. Các servlet nghiệp vụ gọi một guard backend sau xác thực JWT; frontend dùng cùng trạng thái từ `/api/shifts/current` để fail-closed, ẩn menu và chuyển route bị khóa về `/staff/shifts`.

**Tech Stack:** Java 17, Jakarta Servlet 6, JPA 3.1/Hibernate 6.6, JUnit 5, Vue 3.5, Vue Router 4.6, Node.js built-in test runner.

## Global Constraints

- Không thêm dependency hoặc framework.
- Backend là nguồn phân quyền; frontend chỉ phản chiếu policy.
- Check-in: ca hôm nay, từ `startTime - 15 phút` đến `endTime + 15 phút`.
- Check-out: chỉ ca đã check-in, từ `endTime` trở đi, không giới hạn muộn.
- Ngoài trạng thái `CHECKED_IN`, Staff vẫn được mở `/staff/shifts`, `/staff/orders/history`, `/staff/support`.
- UI fail-closed khi không tải được trạng thái ca.
- Mỗi task một commit riêng; message tiếng Việt không dấu.

---

### Task 1: Policy trạng thái ca và thời điểm check-out

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/WorkShiftService.java:100-130`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShiftServlet.java:23-39`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/WorkShiftPolicyTest.java`

**Interfaces:**
- Produces: `static boolean canCheckOut(LocalTime now, LocalTime endTime)`.
- Produces: `Map<String,Object> current(int userId)` với `state` thuộc `NONE`, `UPCOMING`, `CHECK_IN_ALLOWED`, `CHECKED_IN`, `CHECKED_OUT` và `shift` là map ca hoặc `null`.
- Produces: `GET /api/shifts/current`.

- [ ] **Step 1: Viết test policy checkout thất bại trước giờ kết thúc**

```java
package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class WorkShiftPolicyTest {
    @Test
    void checkoutStartsAtShiftEnd() {
        LocalTime end = LocalTime.of(17, 0);
        assertFalse(WorkShiftService.canCheckOut(LocalTime.of(16, 59), end));
        assertTrue(WorkShiftService.canCheckOut(LocalTime.of(17, 0), end));
        assertTrue(WorkShiftService.canCheckOut(LocalTime.of(20, 0), end));
    }
}
```

- [ ] **Step 2: Chạy test và xác nhận đỏ**

Run: `mvn -Dtest=service.WorkShiftPolicyTest test`

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: FAIL vì `canCheckOut` chưa tồn tại.

- [ ] **Step 3: Thêm policy checkout tối thiểu**

Trong `WorkShiftService`:

```java
static boolean canCheckOut(LocalTime now, LocalTime endTime) {
    return !now.isBefore(endTime);
}
```

Trong nhánh checkout của `check(...)`, sau kiểm tra `Cannot check out`:

```java
if (!shift.getShiftDate().equals(LocalDate.now()) || !canCheckOut(LocalTime.now(), shift.getEndTime())) {
    throw new IllegalArgumentException("Check-out is only allowed from shift end time");
}
```

- [ ] **Step 4: Thêm truy vấn trạng thái ca hiện tại**

Thêm `current(int userId)` vào `WorkShiftService`: tải các ca hôm nay theo `startTime`, ưu tiên ca `CHECKED_IN`, sau đó ca chưa checkout gần nhất; trả `CHECKED_OUT` nếu mọi ca đã checkout, `CHECK_IN_ALLOWED` nếu `now` nằm trong cửa sổ check-in, `UPCOMING` nếu chưa tới cửa sổ, ngược lại `NONE`. Dùng chính `toMap(shift)` cho trường `shift`.

```java
Map<String, Object> result = new HashMap<>();
result.put("state", state);
result.put("shift", selected == null ? null : toMap(selected));
return result;
```

Thêm vào `ShiftServlet.doGet` trước nhánh `has-today`:

```java
else if ("/current".equals(req.getPathInfo())) {
    ApiResponse.ok(resp, workShiftService.current(userId));
}
```

Giữ `/has-today` tương thích trong task này; frontend sẽ ngừng dùng ở Task 3.

- [ ] **Step 5: Chạy test backend**

Run: `mvn -Dtest=service.WorkShiftPolicyTest test`

Expected: PASS, 1 test.

- [ ] **Step 6: Commit riêng Task 1**

```bash
git add Backend/FastGuy-FastFoodSite/src/main/java/service/WorkShiftService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShiftServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/WorkShiftPolicyTest.java
git commit -m "bo sung trang thai va checkout ca lam"
```

---

### Task 2: Backend guard cho nghiệp vụ Staff

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffShiftAccessService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffDashboardServlet.java:17-36`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java:31-228`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/StaffShiftAccessPolicyTest.java`

**Interfaces:**
- Consumes: `WorkShiftService.current(int userId)` từ Task 1.
- Produces: `boolean StaffShiftAccessService.hasCheckedInShift(int userId)`.
- Produces: `static boolean StaffOrderServlet.requiresCheckedInShift(String method, String pathInfo)`.

- [ ] **Step 1: Viết test ma trận endpoint**

```java
package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import servlet.StaffOrderServlet;

class StaffShiftAccessPolicyTest {
    @Test
    void historyReadsRemainAvailableOutsideShift() {
        assertFalse(StaffOrderServlet.requiresCheckedInShift("GET", "/history"));
    }

    @Test
    void activeOrdersAndMutationsRequireCheckedInShift() {
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/12"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/export"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("POST", "/12/notes"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("PUT", "/12/status"));
    }
}
```

- [ ] **Step 2: Chạy test và xác nhận đỏ**

Run: `mvn -Dtest=service.StaffShiftAccessPolicyTest test`

Expected: FAIL vì `requiresCheckedInShift` chưa tồn tại.

- [ ] **Step 3: Tạo service guard tối thiểu**

```java
package service;

public class StaffShiftAccessService {
    private final WorkShiftService workShiftService = new WorkShiftService();

    public boolean hasCheckedInShift(int userId) {
        return "CHECKED_IN".equals(workShiftService.current(userId).get("state"));
    }
}
```

- [ ] **Step 4: Thêm policy và guard vào `StaffOrderServlet`**

```java
private StaffShiftAccessService staffShiftAccessService = new StaffShiftAccessService();

public static boolean requiresCheckedInShift(String method, String pathInfo) {
    return !("GET".equals(method) && "/history".equals(pathInfo));
}

private boolean requireCheckedInShift(HttpServletRequest req, HttpServletResponse resp, int staffId) throws IOException {
    if (!requiresCheckedInShift(req.getMethod(), req.getPathInfo())) return true;
    if (staffShiftAccessService.hasCheckedInShift(staffId)) return true;
    ApiResponse.error(resp, "Checked-in shift required", 403);
    return false;
}
```

Gọi guard ngay sau `getStaffId(...)` trong `doGet`, `doPost`, `doPut`. `/history` là ngoại lệ duy nhất của servlet này; hỗ trợ nằm ở servlet riêng nên không sửa.

- [ ] **Step 5: Guard dashboard**

Trong `StaffDashboardServlet`, lấy `staffId` từ JWT rồi kiểm tra:

```java
if (!staffShiftAccessService.hasCheckedInShift(staffId)) {
    ApiResponse.error(resp, "Checked-in shift required", 403);
    return;
}
```

Không thêm guard vào `StaffSupportTicketServlet` hoặc `ShiftServlet`.

- [ ] **Step 6: Chạy test backend focused và toàn bộ**

Run: `mvn -Dtest=service.StaffShiftAccessPolicyTest test`

Expected: PASS, 2 tests.

Run: `mvn test`

Expected: BUILD SUCCESS, không test thất bại.

- [ ] **Step 7: Commit riêng Task 2**

```bash
git add Backend/FastGuy-FastFoodSite/src/main/java/service/StaffShiftAccessService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffDashboardServlet.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/StaffShiftAccessPolicyTest.java
git commit -m "khoa nghiep vu staff ngoai ca"
```

---

### Task 3: Frontend fail-closed và route gating

**Files:**
- Modify: `Frontend/src/api/shift.js:3-15`
- Modify: `Frontend/src/layouts/StaffLayout.vue:1-57`
- Modify: `Frontend/src/router/index.js:135-172,299-336`
- Modify: `Frontend/src/views/staff/StaffShiftsPage.vue:1-64,87-95`
- Modify: `Frontend/src/components/common/ShiftStatus.vue:1-75`
- Create: `Frontend/tests/staff-shift-policy.test.mjs`

**Interfaces:**
- Consumes: `GET /shifts/current` trả `{ state, shift }`.
- Produces: `shiftApi.getCurrent()`.
- Produces: route meta `requiresCheckedInShift: true` cho dashboard và đơn đang xử lý.

- [ ] **Step 1: Viết test policy source frontend**

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8');
const layout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');
const shiftApi = readFileSync(new URL('../src/api/shift.js', import.meta.url), 'utf8');

test('marks dashboard and active orders as checked-in routes', () => {
  assert.match(router, /name: 'StaffDashboard'[\s\S]*?requiresCheckedInShift: true/);
  assert.match(router, /name: 'StaffOrders'[\s\S]*?requiresCheckedInShift: true/);
  assert.match(router, /name: 'StaffOrderDetail'[\s\S]*?requiresCheckedInShift: true/);
  assert.doesNotMatch(router, /name: 'StaffOrderHistory'[\s\S]*?requiresCheckedInShift: true/);
});

test('loads current shift and fails closed', () => {
  assert.match(shiftApi, /getCurrent\(\)/);
  assert.match(layout, /shiftState\.value = 'UNKNOWN'/);
});
```

- [ ] **Step 2: Chạy test và xác nhận đỏ**

Run: `node --test tests/staff-shift-policy.test.mjs`

Workdir: `Frontend`

Expected: FAIL vì route meta, `getCurrent`, `shiftState` chưa tồn tại.

- [ ] **Step 3: Thêm API trạng thái hiện tại**

Trong `Frontend/src/api/shift.js`:

```js
getCurrent() {
  return client.get('/shifts/current');
},
```

- [ ] **Step 4: Đổi `StaffLayout` sang state fail-closed**

Thay `hasTodayShift` bằng:

```js
const shiftState = ref('UNKNOWN');
const checkedIn = computed(() => shiftState.value === 'CHECKED_IN');
```

`checkShift()` gọi `shiftApi.getCurrent()`, gán `data?.state || 'UNKNOWN'`; catch gán `UNKNOWN`. Menu luôn có `Ca làm`, `Lịch sử đơn`, `Hỗ trợ`; chỉ thêm `Tổng quan`, `Đơn hàng` khi `checkedIn`. Chỉ poll pending orders khi `checkedIn`. Banner phân biệt `UNKNOWN`, `NONE/UPCOMING/CHECK_IN_ALLOWED`, `CHECKED_OUT`; không render dữ liệu nghiệp vụ khi chưa xác định trạng thái.

- [ ] **Step 5: Thêm route meta và guard async**

Gắn `meta: { requiresCheckedInShift: true }` vào `StaffDashboard`, `StaffOrders`, `StaffOrderDetail`. Không gắn vào `StaffOrderHistory`, `StaffSupport`, `StaffShifts`.

Đổi `router.beforeEach` thành `async`; sau auth/role checks:

```js
if (to.matched.some((record) => record.meta.requiresCheckedInShift)) {
  try {
    const current = await shiftApi.getCurrent();
    if (current?.state !== 'CHECKED_IN') return next('/staff/shifts');
  } catch {
    return next('/staff/shifts');
  }
}
```

Import `shiftApi` từ `@/api`. Guard không gọi API ca cho route công khai, lịch sử, hỗ trợ hoặc ca làm.

- [ ] **Step 6: Đồng bộ UI check-in/check-out**

Trong `StaffShiftsPage.vue`, tính `canCheckOut` từ giờ hiện tại và `todayShift.endTime`; disable nút trước giờ kết thúc, hiển thị `Có thể check-out từ HH:mm`. Sau check-in/check-out thành công, phát `window.dispatchEvent(new Event('staff-shift-changed'))`.

Trong `StaffLayout.vue`, lắng nghe `staff-shift-changed`, gọi lại `checkShift()`, dọn listener khi unmount.

Trong `ShiftStatus.vue`, disable action checkout trước `endTime`, dùng cùng nhãn; vẫn để backend quyết định cuối cùng.

- [ ] **Step 7: Chạy frontend tests và build**

Run: `node --test tests/*.test.mjs`

Expected: tất cả test PASS.

Run: `npm run build`

Expected: Vite build thành công, không lỗi import/compile.

- [ ] **Step 8: Commit riêng Task 3**

```bash
git add Frontend/src/api/shift.js Frontend/src/layouts/StaffLayout.vue Frontend/src/router/index.js Frontend/src/views/staff/StaffShiftsPage.vue Frontend/src/components/common/ShiftStatus.vue Frontend/tests/staff-shift-policy.test.mjs
git commit -m "khoa giao dien staff theo ca lam"
```

---

### Task 4: Verification tích hợp

**Files:**
- Không tạo file mới.

**Interfaces:**
- Consumes: toàn bộ interfaces Tasks 1-3.
- Produces: bằng chứng backend, frontend và policy hoạt động cùng nhau.

- [ ] **Step 1: Chạy backend verification**

Run: `mvn clean test`

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Chạy frontend verification**

Run: `node --test tests/*.test.mjs; if ($?) { npm run build }`

Workdir: `Frontend`

Expected: tất cả test PASS; Vite build thành công.

- [ ] **Step 3: Kiểm tra diff**

Run: `git diff --check`

Expected: không có whitespace error; cảnh báo LF/CRLF được phép.

- [ ] **Step 4: Kiểm tra runtime thủ công sau restart Tomcat**

Restart SmartTomcat để tạo lại classloader. Kiểm tra lần lượt:

1. Staff chưa có ca: `/staff` chuyển `/staff/shifts`; lịch sử và hỗ trợ mở được.
2. Staff có ca nhưng chưa check-in: dashboard/đơn bị chuyển; check-in chỉ thành công trong cửa sổ cho phép.
3. Staff `CHECKED_IN`: dashboard, danh sách, chi tiết, mutation và gán Shipper mở được.
4. Checkout trước `endTime`: UI disabled; gọi API trực tiếp trả HTTP 400.
5. Checkout tại/sau `endTime`: thành công; dashboard/đơn lại bị khóa; lịch sử/hỗ trợ vẫn mở.
6. API `/shifts/current` lỗi: route nghiệp vụ fail-closed về `/staff/shifts`.

- [ ] **Step 5: Không commit verification nếu không đổi file**

Nếu verification phát hiện lỗi, sửa trong task sở hữu file, chạy lại command tương ứng, tạo commit mới; không amend và không commit rỗng.
