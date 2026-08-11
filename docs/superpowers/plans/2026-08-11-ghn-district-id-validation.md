# GHN District ID Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Đóng `BUG-UT-001` bằng validation chính xác cho `ghnDistrictId`, bảo đảm payload sai bị chặn với HTTP 400 trước service.

**Architecture:** Giữ `AddressValidator` làm trust boundary dùng chung cho tạo và cập nhật địa chỉ. Bổ sung predicate nhỏ trong cùng class để chỉ nhận `Number` hữu hạn, nguyên, dương và nằm trong miền `int`; giữ `AddressServlet` trả HTTP 400 từ validation hiện có.

**Tech Stack:** Java 17, Jakarta Servlet 6.1, Jackson 2.18.8, JUnit 5.11.4, Maven.

## Global Constraints

- `ghnDistrictId` chỉ hợp lệ khi là JSON number biểu diễn số nguyên dương trong miền `int`.
- Thiếu, `null`, chuỗi số, chuỗi không phải số, số thực, số không, số âm và số vượt `Integer.MAX_VALUE` đều không hợp lệ.
- Giữ thông báo `Quan/huyen GHN khong hop le`.
- Không thêm DTO, dependency, schema hoặc migration.
- Không thay đổi frontend hoặc validation trường không liên quan.
- Chỉ đóng `BUG-UT-001` sau khi focused test, toàn bộ backend test và package pass.

---

### Task 1: Siết validation `ghnDistrictId`

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/utils/AddressValidatorTest.java:147-162`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/utils/AddressValidator.java:35-40`

**Interfaces:**
- Consumes: `AddressValidator.validate(Map<String, Object>)`.
- Produces: `AddressValidator.validate(Map<String, Object>)` trả `null` cho district ID hợp lệ hoặc `Quan/huyen GHN khong hop le` cho mọi district ID ngoài contract.

- [ ] **Step 1: Viết regression test thất bại cho kiểu và biên số**

Thêm import:

```java
import static org.junit.jupiter.api.Assertions.assertAll;
```

Thêm test vào `AddressValidatorTest`:

```java
@Test
@DisplayName("GHN district ID must be a positive integer JSON number")
void invalidGhnDistrictId_returnsError() {
    String expected = "Quan/huyen GHN khong hop le";

    assertAll(
            () -> assertEquals(expected, validateGhnDistrictId(null)),
            () -> assertEquals(expected, validateGhnDistrictId("123")),
            () -> assertEquals(expected, validateGhnDistrictId("abc")),
            () -> assertEquals(expected, validateGhnDistrictId(1.5)),
            () -> assertEquals(expected, validateGhnDistrictId(0)),
            () -> assertEquals(expected, validateGhnDistrictId(-1)),
            () -> assertEquals(expected, validateGhnDistrictId((long) Integer.MAX_VALUE + 1)));
}

private String validateGhnDistrictId(Object value) {
    Map<String, Object> body = validBody();
    body.put("ghnDistrictId", value);
    return AddressValidator.validate(body);
}
```

Giữ test `validAddress_noError()` làm bằng chứng số nguyên dương `1442` vẫn hợp lệ.

- [ ] **Step 2: Chạy focused test và xác nhận RED**

Run từ `Backend/FastGuy-FastFoodSite`:

```bash
mvn -Dtest=AddressValidatorTest test
```

Expected: FAIL ít nhất với `1.5` hoặc `(long) Integer.MAX_VALUE + 1`, vì implementation cũ dùng `intValue()` và có thể chấp nhận thu hẹp số sai.

- [ ] **Step 3: Viết implementation nhỏ nhất**

Trong `AddressValidator`, thay check `ghnDistrictId` bằng helper cùng class:

```java
if (!isPositiveInt(body.get("ghnDistrictId"))) {
    return "Quan/huyen GHN khong hop le";
}
```

Thêm helper trước dấu `}` cuối class:

```java
private static boolean isPositiveInt(Object value) {
    if (!(value instanceof Number number)) {
        return false;
    }
    double doubleValue = number.doubleValue();
    return Double.isFinite(doubleValue)
            && doubleValue == Math.rint(doubleValue)
            && doubleValue > 0
            && doubleValue <= Integer.MAX_VALUE;
}
```

- [ ] **Step 4: Chạy focused test và xác nhận GREEN**

Run:

```bash
mvn -Dtest=AddressValidatorTest test
```

Expected: `BUILD SUCCESS`, mọi test trong `AddressValidatorTest` pass.

- [ ] **Step 5: Commit validator và regression test**

```bash
git add Backend/FastGuy-FastFoodSite/src/main/java/utils/AddressValidator.java Backend/FastGuy-FastFoodSite/src/test/java/utils/AddressValidatorTest.java
git commit -m "fix(address): validate GHN district integer range"
```

### Task 2: Khóa HTTP 400 contract trước service

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AddressValidationPolicyTest.java`
- Verify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AddressServlet.java:62-80`
- Verify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AddressServlet.java:84-123`

**Interfaces:**
- Consumes: source contract của `AddressServlet.doPost`, `AddressServlet.doPut`, `AddressValidator.validate` và `ApiResponse.error`.
- Produces: regression proof rằng validation error trả 400 và `return` trước `toAddress(...)`/`addressService.create(...)`/`addressService.update(...)`.

- [ ] **Step 1: Viết policy regression test cho POST và PUT**

Tạo `AddressValidationPolicyTest.java`:

```java
package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AddressValidationPolicyTest {
    private static final Path SOURCE = Path.of("src/main/java/servlet/AddressServlet.java");

    @Test
    void invalidAddressReturns400BeforePostServiceCall() throws IOException {
        String source = Files.readString(SOURCE);
        String post = source.substring(source.indexOf("protected void doPost"), source.indexOf("protected void doPut"));

        assertValidationReturnsBefore(post, "addressService.create");
    }

    @Test
    void invalidAddressReturns400BeforePutServiceCall() throws IOException {
        String source = Files.readString(SOURCE);
        String put = source.substring(source.indexOf("protected void doPut"), source.indexOf("protected void doDelete"));

        assertValidationReturnsBefore(put, "addressService.update");
    }

    private void assertValidationReturnsBefore(String method, String serviceCall) {
        int validation = method.indexOf("AddressValidator.validate(body)");
        int badRequest = method.indexOf("ApiResponse.error(resp, validationError, 400)", validation);
        int earlyReturn = method.indexOf("return;", badRequest);
        int conversion = method.indexOf("toAddress(body)");
        int service = method.indexOf(serviceCall);

        assertTrue(validation >= 0);
        assertTrue(badRequest > validation);
        assertTrue(earlyReturn > badRequest);
        assertTrue(conversion > earlyReturn);
        assertTrue(service > earlyReturn);
    }
}
```

- [ ] **Step 2: Chạy HTTP contract test**

Run từ `Backend/FastGuy-FastFoodSite`:

```bash
mvn -Dtest=AddressValidationPolicyTest test
```

Expected: `BUILD SUCCESS`. Existing servlet đã có HTTP 400 và early return; không sửa production servlet nếu test pass.

- [ ] **Step 3: Chạy validator và HTTP contract cùng nhau**

Run:

```bash
mvn -Dtest=AddressValidatorTest,AddressValidationPolicyTest test
```

Expected: `BUILD SUCCESS`; chuỗi `"abc"` bị validator từ chối và servlet contract bảo đảm lỗi dừng ở HTTP 400 trước service.

- [ ] **Step 4: Commit contract test**

```bash
git add Backend/FastGuy-FastFoodSite/src/test/java/servlet/AddressValidationPolicyTest.java
git commit -m "test(address): guard validation HTTP contract"
```

### Task 3: Verify và đóng defect

**Files:**
- Modify: `docs/unit-test-defect.md:8`

**Interfaces:**
- Consumes: passing tests từ Task 1-2.
- Produces: trạng thái `BUG-UT-001` khớp code đã kiểm chứng.

- [ ] **Step 1: Chạy toàn bộ backend test**

Run từ `Backend/FastGuy-FastFoodSite`:

```bash
mvn test
```

Expected: `BUILD SUCCESS`, không test fail/error.

- [ ] **Step 2: Package WAR**

Run:

```bash
mvn package -DskipTests
```

Expected: `BUILD SUCCESS`, tạo `target/FastGuy.war`.

- [ ] **Step 3: Đóng defect bằng bằng chứng test**

Trong `docs/unit-test-defect.md`, đổi dòng `BUG-UT-001`:

```markdown
| BUG-UT-001 | ghnDistrictId không phải số gây HTTP 500 | Lập trình viên | Close | 2 | 3 | 2/2 lần | Đỗ Huy Hoàng | 26/07/2026 | 1. Gọi tạo địa chỉ với ghnDistrictId="abc" | ghnDistrictId=abc | Đã sửa: chỉ nhận JSON number là số nguyên dương trong miền int; payload sai trả HTTP 400 trước service | Chrome, FastGuy API v2 | UT023 |
```

- [ ] **Step 4: Kiểm tra diff cuối**

Run từ repo root:

```bash
git diff --check
git status --short
git diff --stat HEAD~2
```

Expected: không whitespace error; chỉ file validator, hai test và defect document thuộc phạm vi thay đổi.

- [ ] **Step 5: Commit tài liệu đóng defect**

```bash
git add docs/unit-test-defect.md
git commit -m "docs(test): close GHN district validation defect"
```
