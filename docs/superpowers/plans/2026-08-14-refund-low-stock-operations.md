# Refund Audit and Low-Stock Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Làm rõ audit hoàn tiền thủ công và dùng một ngưỡng tồn kho thấp thống nhất trong backend contracts, Admin/Staff dashboards, Inventory và Products mà không đổi schema hoặc tạo notification lưu trữ.

**Architecture:** Giữ dữ liệu audit refund hiện có trên `Orders`; API resolve `refundProcessedBy` thành tên processor khi đọc, không đổi schema refund. Migration idempotent `047_low_stock_threshold.sql` chạy sau COD migration `046`, seed config row `low_stock_threshold=5` trong `ShippingConfig`, rồi đồng bộ canonical bootstrap SQL. `StoreConfigService` và Admin Settings quản lý giá trị persist; `ProductDAO` tính count theo SKU/variant và các dashboard phát cùng contract. Frontend dùng utility thuần chung cho phân loại/filter tồn kho.

**Tech Stack:** Java 17, Jakarta Servlet 6.1, JPA/Hibernate 6.6, SQL Server, JUnit 5, Vue 3 Composition API, Pinia, native Node.js test runner.

## Global Constraints

- Giữ Vue 3, Pinia, servlet/service/DAO/entity và SQL migration hiện có.
- Không thêm dependency khi native platform hoặc utility hiện có đáp ứng.
- Không đổi schema cho processor name; giữ `Orders.refund_processed_by` hiện có.
- Không thêm partial refund hoặc PayOS refund API; workflow vẫn hoàn thủ công ngoài hệ thống.
- Mutation refund giữ pessimistic lock, terminal-state protection và idempotency hiện có.
- Dùng đúng một config key `low_stock_threshold`, default persist `5`, kiểu số nguyên trong khoảng `1..1000`.
- Migration này phải chạy sau COD migration `046`; dùng số kế tiếp `047_low_stock_threshold` và fail precondition nếu history chưa có `046_cod_shift_settlement`.
- Migration chỉ upsert một row `ShippingConfig`; không tạo bảng hoặc cột mới.
- `database/init.sql` và `database/DB_FastGuy.sql` phải chứa cùng canonical default `low_stock_threshold=5` cho database tạo mới.
- Count tồn kho là count SKU/`ProductVariant`, không phải tổng quantity và không phải count product.
- `outOfStockSkuCount`: variant có `quantityAvailable <= 0`; `lowStockSkuCount`: variant có `quantityAvailable > 0 && quantityAvailable <= lowStockThreshold`.
- Variant có `quantityAvailable == null` là không giới hạn, không thuộc hai count.
- Dashboard alerts là dữ liệu response hiện thời; không tạo `Notification`, read receipt, migration hoặc persistent notification.
- Cảnh báo chỉ hỗ trợ quyết định; không tự nhập hàng, tự điều chỉnh tồn hoặc tự khóa bán.
- API role/active-account/checked-in-shift validation hiện có không được nới lỏng.
- Mọi màn đã chạm phải giữ loading, empty, error/retry; trạng thái cảnh báo không chỉ dựa vào màu.
- Không commit. Task kết thúc bằng kiểm tra hẹp; bước cuối chạy full test/build và review `git diff`/`git status`.

---

## File Map

### Create

- `database/migrations/047_low_stock_threshold.sql` — migration idempotent sau `046_cod_shift_settlement`, upsert default config row và ghi history.
- `database/migrations/047_validate.sql` — validator độc lập cho dependency history, đúng một row, value integer/range và retained value.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/LowStockThresholdMigrationPolicyTest.java` — source validator cho migration/bootstrap compatibility.
- `Frontend/src/utils/stockPolicy.js` — nguồn frontend duy nhất cho normalize threshold, phân loại variant và tổng hợp stock product.
- `Frontend/tests/stock-policy.test.mjs` — unit tests runnable cho threshold boundary, unlimited stock và product aggregation.
- `Frontend/tests/admin-dashboard-stock-policy.test.mjs` — source contract cho Admin stock cards/alerts/navigation.

### Modify

- `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java` — khai báo, parse và validate `low_stock_threshold`.
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/ProductDAO.java` — count SKU hết/sắp hết bằng exact boundary contract.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java` — thêm stock count contract vào Admin dashboard.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffService.java` — thêm stock count contract vào Staff dashboard.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminRefundServlet.java` — resolve processor IDs thành names theo batch và serialize `refundProcessedByName`.
- `Frontend/src/views/admin/RefundsPage.vue` — hiển thị audit metadata, pending copy chính xác và accessible refund dialog.
- `Frontend/src/views/admin/DashboardPage.vue` — hiển thị SKU hết/sắp hết và link xử lý Inventory.
- `Frontend/src/views/staff/DashboardPage.vue` — thêm tồn kho vào “Cần xử lý ngay”, không tạo notification.
- `Frontend/src/views/admin/InventoryPage.vue` — thay mọi ngưỡng `5` bằng shared stock policy/config contract.
- `Frontend/src/views/admin/ProductsPage.vue` — thay ngưỡng `10` và product-sum classification bằng shared SKU policy.
- `Frontend/src/views/admin/SettingsPage.vue` — thêm editable Inventory tab cho `low_stock_threshold`.
- `Frontend/src/utils/settingsValidation.js` — validate/payload scope `inventory` với exact integer range `1..1000`.
- `Frontend/tests/settings-validation.test.mjs` — unit tests cho inventory validation và payload.
- `Frontend/tests/settings-policy.test.mjs` — source tests cho accessible Inventory settings tab/save flow.
- `database/init.sql` — seed canonical default cho database mới.
- `database/DB_FastGuy.sql` — seed canonical default cho bootstrap legacy snapshot.
- `Frontend/tests/refund-policy.test.mjs` — source assertions cho audit display/copy/dialog.
- `Frontend/tests/staff-dashboard-policy.test.mjs` — source assertions cho stock alert contract.
- `Frontend/tests/admin-product-catalog.test.mjs` — source assertions đảm bảo Products dùng shared threshold.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java` — config default/range contract.
- `Backend/FastGuy-FastFoodSite/src/test/java/dao/ProductDAOPolicyTest.java` — exact JPQL count boundary contract.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/AdminDashboardStockPolicyTest.java` — Admin service response contract.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/StaffDashboardPolicyTest.java` — Staff service response contract.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/RefundPolicyTest.java` — refund processor-name read contract.

### Explicitly unchanged

- `entity/Orders.java` và refund SQL columns — không đổi schema refund.
- Mọi table/column definition — migration `047` chỉ seed config row, không tạo table/column.
- `NotificationService`, `NotificationDAO`, notification UI — không persistent alerts.
- `RefundService.update(...)` signature và mutation sequence — không đổi locking/idempotency semantics.
- `Frontend/package.json`, `Backend/FastGuy-FastFoodSite/pom.xml` — không dependency/script mới.

---

### Task 1: Persisted Low-Stock Default After COD

**Files:**
- Create: `database/migrations/047_low_stock_threshold.sql`
- Create: `database/migrations/047_validate.sql`
- Modify: `database/init.sql:543-560`
- Modify: `database/DB_FastGuy.sql:470-472`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/LowStockThresholdMigrationPolicyTest.java`

**Interfaces:**
- Consumes: COD plan migration history ID `046_cod_shift_settlement`; existing `dbo.ShippingConfig(config_key, config_value)` và `dbo.SchemaMigrationHistory`.
- Produces: exactly one persisted row `('low_stock_threshold','5')`; migration history `047_low_stock_threshold`; no table/column creation; existing operator value retained when row already exists.

- [ ] **Step 1: Viết RED migration/bootstrap source validator**

Tạo `LowStockThresholdMigrationPolicyTest.java`:

```java
package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LowStockThresholdMigrationPolicyTest {
    @Test
    void migrationRunsAfterCodAndSeedsWithoutOverwriting() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/047_low_stock_threshold.sql"));
        assertTrue(migration.contains("migration_id = '046_cod_shift_settlement'"));
        assertTrue(migration.contains("migration_id = '047_low_stock_threshold'"));
        assertTrue(migration.contains("IF NOT EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold')"));
        assertTrue(migration.contains("VALUES ('low_stock_threshold', '5')"));
        assertFalse(migration.contains("CREATE TABLE"));
        assertFalse(migration.contains("ALTER TABLE"));
    }

    @Test
    void validatorAndFreshBootstrapContainCanonicalDefault() throws Exception {
        String validator = Files.readString(Path.of("../../database/migrations/047_validate.sql"));
        String init = Files.readString(Path.of("../../database/init.sql"));
        String snapshot = Files.readString(Path.of("../../database/DB_FastGuy.sql"));
        assertTrue(validator.contains("047_low_stock_threshold"));
        assertTrue(validator.contains("COUNT(*) <> 1"));
        assertTrue(validator.contains("TRY_CONVERT(int, config_value)"));
        assertTrue(validator.contains("BETWEEN 1 AND 1000"));
        assertTrue(init.contains("('low_stock_threshold', '5')"));
        assertTrue(snapshot.contains("'low_stock_threshold','5'"));
    }
}
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
mvn -Dtest=service.LowStockThresholdMigrationPolicyTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: ERROR/FAIL vì `047_low_stock_threshold.sql` và `047_validate.sql` chưa tồn tại.

- [ ] **Step 3: Tạo migration idempotent, dependency sau COD và không overwrite**

Tạo `database/migrations/047_low_stock_threshold.sql`:

```sql
USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51470, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51471, 'Run 046_cod_shift_settlement.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '047_low_stock_threshold')
    PRINT '047_low_stock_threshold already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF NOT EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold')
            INSERT dbo.ShippingConfig(config_key, config_value) VALUES ('low_stock_threshold', '5');
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('047_low_stock_threshold', N'Added persisted shared low-stock threshold default without overwriting existing configuration');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
```

- [ ] **Step 4: Tạo exact post-migration validator**

Tạo `database/migrations/047_validate.sql`:

```sql
USE FastGuyDB;
GO
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51472, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51473, '046_cod_shift_settlement is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '047_low_stock_threshold') THROW 51474, '047_low_stock_threshold is missing.', 1;
IF (SELECT COUNT(*) FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold') <> 1 THROW 51475, 'low_stock_threshold must exist exactly once.', 1;
IF EXISTS (
    SELECT 1 FROM dbo.ShippingConfig
    WHERE config_key = 'low_stock_threshold'
      AND (TRY_CONVERT(int, config_value) IS NULL OR TRY_CONVERT(int, config_value) NOT BETWEEN 1 AND 1000)
) THROW 51476, 'low_stock_threshold must be an integer between 1 and 1000.', 1;
PRINT '047 low-stock threshold validation passed.';
GO
```

- [ ] **Step 5: Đồng bộ fresh-database seed files**

Trong `database/init.sql`, thêm vào existing multi-row `INSERT dbo.ShippingConfig (config_key, config_value) VALUES`:

```sql
    ('low_stock_threshold', '5'),
```

Trong `database/DB_FastGuy.sql`, mở rộng existing `INSERT INTO dbo.ShippingConfig(config_id,config_key,config_value)` bằng row ID kế tiếp chưa dùng trong chính statement, giữ `IDENTITY_INSERT` convention:

```sql
,(4,'low_stock_threshold','5')
```

Nếu live COD execution đã thêm ShippingConfig seed khác và dùng ID `4`, chọn `MAX(config_id)+1` theo file sau COD; yêu cầu invariant là key/value, không cố định ID.

- [ ] **Step 6: Chạy GREEN migration source validator**

Run:

```powershell
mvn -Dtest=service.LowStockThresholdMigrationPolicyTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Chạy SQL migration/validator trên disposable DB đã áp dụng 046**

Run:

```powershell
sqlcmd -b -S "$env:FASTGUY_DB_SERVER" -d FastGuyDB -E -i database/migrations/047_low_stock_threshold.sql; if ($?) { sqlcmd -b -S "$env:FASTGUY_DB_SERVER" -d FastGuyDB -E -i database/migrations/047_validate.sql }
```

Workdir: workspace root. Nếu project dùng SQL authentication, thay `-E` bằng credentials chuẩn của môi trường; không ghi credential vào repo.

Expected: `047 low-stock threshold validation passed.` Chạy migration lần hai phải in `047_low_stock_threshold already applied.` và validator vẫn pass.

---

### Task 2: Shared Low-Stock Configuration Contract

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java:13-126`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/StoreConfigPolicyTest.java:11-42`

**Interfaces:**
- Consumes: rows từ `ShippingConfig` qua `Map<String, String> getAll()`.
- Produces: `public static final String LOW_STOCK_THRESHOLD = "low_stock_threshold"`; `public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5`; `public int getLowStockThreshold()` trả `1..1000`; `update(Map<String,Object>)` chấp nhận key và reject ngoài range.

- [ ] **Step 1: Viết RED tests cho default, parse và validation source contract**

Thêm vào `StoreConfigPolicyTest`:

```java
@Test
void lowStockThresholdHasOneBoundedSharedContract() throws IOException {
    String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
    assertTrue(src.contains("public static final String LOW_STOCK_THRESHOLD = \"low_stock_threshold\""));
    assertTrue(src.contains("public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5"));
    assertTrue(src.contains("public int getLowStockThreshold()"));
    assertTrue(src.contains("parseIntSafe(getAll().get(LOW_STOCK_THRESHOLD), DEFAULT_LOW_STOCK_THRESHOLD)"));
    assertTrue(src.contains("threshold < 1 || threshold > 1000"));
    assertTrue(src.contains("low_stock_threshold must be between 1 and 1000"));
}
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
mvn -Dtest=service.StoreConfigPolicyTest#lowStockThresholdHasOneBoundedSharedContract test
```

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: FAIL tại assertion thiếu `LOW_STOCK_THRESHOLD`.

- [ ] **Step 3: Thêm config constant, getter và bounded update validation**

Trong `StoreConfigService`, thêm:

```java
public static final String LOW_STOCK_THRESHOLD = "low_stock_threshold";
public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
```

Mở rộng integer keys:

```java
private static final java.util.Set<String> INT_KEYS = Set.of("estimated_delivery_minutes", LOW_STOCK_THRESHOLD);
```

Thêm getter:

```java
public int getLowStockThreshold() {
    int threshold = parseIntSafe(getAll().get(LOW_STOCK_THRESHOLD), DEFAULT_LOW_STOCK_THRESHOLD);
    return threshold >= 1 && threshold <= 1000 ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
}
```

Mở rộng nhánh validation integer, giữ validation ETA hiện có:

```java
int integer = Integer.parseInt(value);
if ("estimated_delivery_minutes".equals(key) && (integer < 10 || integer > 180)) {
    throw new IllegalArgumentException("estimated_delivery_minutes must be between 10 and 180");
}
if (LOW_STOCK_THRESHOLD.equals(key)) {
    int threshold = integer;
    if (threshold < 1 || threshold > 1000) {
        throw new IllegalArgumentException("low_stock_threshold must be between 1 and 1000");
    }
}
```

Không đưa key vào `getPublicConfig()`; threshold chỉ cần cho authenticated operations contracts.

- [ ] **Step 4: Chạy GREEN và regression class**

Run:

```powershell
mvn -Dtest=service.StoreConfigPolicyTest test
```

Expected: BUILD SUCCESS, mọi test trong class PASS.

---

### Task 3: Admin Settings Control for Low-Stock Threshold

**Files:**
- Modify: `Frontend/src/utils/settingsValidation.js:43-85`
- Modify: `Frontend/tests/settings-validation.test.mjs:1-115`
- Modify: `Frontend/src/views/admin/SettingsPage.vue:1-301`
- Modify: `Frontend/tests/settings-policy.test.mjs:1-75`

**Interfaces:**
- Consumes: `GET /api/admin/settings` map containing persisted `low_stock_threshold`; `PUT /api/admin/settings` validation from Task 2.
- Produces: `validateInventory(threshold): Record<string,string>`; `SCOPE_KEYS.inventory = ['low_stock_threshold']`; `buildSettingsPayload('inventory', form)` returns `{ low_stock_threshold: number }`; accessible editable Settings tab.

- [ ] **Step 1: Viết RED utility tests cho integer range và isolated payload**

Mở rộng imports trong `settings-validation.test.mjs`:

```js
  validateInventory,
```

Thêm tests:

```js
test('validateInventory accepts integer low-stock threshold from 1 through 1000', () => {
  assert.deepEqual(validateInventory(1), {});
  assert.deepEqual(validateInventory(5), {});
  assert.deepEqual(validateInventory('1000'), {});
});

test('validateInventory rejects empty, fractional and out-of-range thresholds', () => {
  const message = 'Ngưỡng sắp hết phải là số nguyên từ 1 đến 1000';
  for (const value of ['', null, 0, 1001, 1.5, 'abc']) {
    assert.deepEqual(validateInventory(value), { low_stock_threshold: message });
  }
});

test('buildSettingsPayload sends only persisted inventory threshold', () => {
  assert.deepEqual(buildSettingsPayload('inventory', { low_stock_threshold: '7', delivery_fee: 15000 }), {
    payload: { low_stock_threshold: 7 },
    errors: {},
  });
  assert.deepEqual(SCOPE_KEYS.inventory, ['low_stock_threshold']);
});
```

Cập nhật exact editable scope assertion:

```js
assert.deepEqual(Object.keys(SCOPE_KEYS).sort(), ['delivery', 'fees', 'hours', 'inventory', 'store']);
```

- [ ] **Step 2: Chạy utility RED chính xác**

Run:

```powershell
node --test tests/settings-validation.test.mjs
```

Workdir: `Frontend`

Expected: FAIL vì export `validateInventory` và scope `inventory` chưa có.

- [ ] **Step 3: Implement validation và payload tối thiểu**

Trong `settingsValidation.js`, thêm:

```js
export function validateInventory(threshold) {
  const errors = {};
  const value = Number(threshold);
  if (threshold === '' || threshold === null || threshold === undefined || !Number.isInteger(value) || value < 1 || value > 1000) {
    errors.low_stock_threshold = 'Ngưỡng sắp hết phải là số nguyên từ 1 đến 1000';
  }
  return errors;
}
```

Mở rộng `SCOPE_KEYS`:

```js
inventory: ['low_stock_threshold'],
```

Thêm switch case trước `default`:

```js
case 'inventory':
  return {
    payload: { low_stock_threshold: Number(form.low_stock_threshold) },
    errors: validateInventory(form.low_stock_threshold),
  };
```

- [ ] **Step 4: Chạy utility GREEN**

Run:

```powershell
node --test tests/settings-validation.test.mjs
```

Expected: toàn bộ settings validation tests PASS.

- [ ] **Step 5: Viết RED Settings page source test**

Trong `settings-policy.test.mjs`, đổi labels và editable save assertions:

```js
const TAB_LABELS = ['Cửa hàng', 'Giờ hoạt động', 'Phí & thuế', 'Giao hàng', 'Tồn kho', 'Thanh toán', 'Vận chuyển GHN'];
```

```js
for (const scope of ['store', 'hours', 'fees', 'delivery', 'inventory']) {
  assert.match(page, new RegExp(`saveTab\\('${scope}'\\)`));
}
```

Thêm:

```js
test('inventory tab edits persisted low-stock threshold with accessible error', () => {
  assert.match(page, /id: 'inventory'/);
  assert.match(page, /low_stock_threshold: 5/);
  assert.match(page, /form\.value\.low_stock_threshold = Number/);
  assert.match(page, /saveTab\('inventory'\)/);
  assert.match(page, /for="settings-low-stock-threshold"/);
  assert.match(page, /id="settings-low-stock-threshold"/);
  assert.match(page, /fieldError\('inventory', 'low_stock_threshold'\)/);
  assert.match(page, /role="alert"/);
});
```

- [ ] **Step 6: Chạy Settings page RED chính xác**

Run:

```powershell
node --test tests/settings-policy.test.mjs
```

Workdir: `Frontend`

Expected: FAIL vì Inventory tab/form chưa có.

- [ ] **Step 7: Wire Settings form, baseline và persisted load**

Trong `SettingsPage.vue`, thêm tab giữa delivery và payment:

```js
{ id: 'inventory', label: 'Tồn kho', icon: 'bi-boxes' },
```

Mở rộng:

```js
const EDITABLE_SCOPES = ['store', 'hours', 'fees', 'delivery', 'inventory'];
```

Trong `createForm()`:

```js
low_stock_threshold: 5,
```

Khởi tạo error map ở cả declaration và `load()`:

```js
const tabErrors = ref({ store: {}, hours: {}, fees: {}, delivery: {}, inventory: {} });
```

Trong `applySettings()` sau numeric conversions:

```js
form.value.low_stock_threshold = Number(form.value.low_stock_threshold || 5);
```

- [ ] **Step 8: Render accessible Inventory settings tab**

Thêm sau delivery form:

```vue
<form v-else-if="activeTab === 'inventory'" class="card card-flat settings-card" @submit.prevent="saveTab('inventory')" novalidate>
  <h3 class="panel-title"><i class="bi bi-boxes"></i> Tồn kho</h3>
  <div class="form-group" style="max-width:280px">
    <label class="form-label" for="settings-low-stock-threshold">Ngưỡng cảnh báo sắp hết (SKU)</label>
    <input id="settings-low-stock-threshold" v-model.number="form.low_stock_threshold" class="form-input" type="number" min="1" max="1000" step="1" aria-describedby="settings-low-stock-help">
    <small id="settings-low-stock-help" class="readonly-note">SKU có tồn từ 1 đến ngưỡng này được tính là sắp hết.</small>
    <p v-if="fieldError('inventory', 'low_stock_threshold')" class="field-error" role="alert">{{ fieldError('inventory', 'low_stock_threshold') }}</p>
  </div>
  <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
</form>
```

- [ ] **Step 9: Chạy GREEN Settings tests và build**

Run:

```powershell
node --test tests/settings-validation.test.mjs tests/settings-policy.test.mjs; if ($?) { npm run build }
```

Workdir: `Frontend`

Expected: tests PASS; Vite build success.

---

### Task 4: Backend SKU Count Contracts for Admin and Staff

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/ProductDAO.java:15-321`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java:13-73`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffService.java:14-71`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/dao/ProductDAOPolicyTest.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/AdminDashboardStockPolicyTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/StaffDashboardPolicyTest.java:11-27`

**Interfaces:**
- Consumes: `StoreConfigService.getLowStockThreshold()` và `ProductVariant.quantityAvailable`.
- Produces: `ProductDAO.countOutOfStockSkus(): long`; `ProductDAO.countLowStockSkus(int threshold): long`; Admin/Staff dashboard fields `lowStockThreshold: int`, `outOfStockSkuCount: long`, `lowStockSkuCount: long`.

- [ ] **Step 1: Viết RED DAO boundary tests**

Thêm vào `ProductDAOPolicyTest`:

```java
@Test
void dashboardStockCountsUseSkuBoundaryAndExcludeUnlimitedStock() throws IOException {
    String src = Files.readString(Path.of("src/main/java/dao/ProductDAO.java"));
    assertTrue(src.contains("public long countOutOfStockSkus()"));
    assertTrue(src.contains("v.quantityAvailable <= 0"));
    assertTrue(src.contains("public long countLowStockSkus(int threshold)"));
    assertTrue(src.contains("v.quantityAvailable > 0 AND v.quantityAvailable <= :threshold"));
}
```

- [ ] **Step 2: Viết RED service contract tests**

Tạo `AdminDashboardStockPolicyTest.java`:

```java
package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminDashboardStockPolicyTest {
    @Test
    void dashboardPublishesSharedSkuStockCounts() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        assertTrue(src.contains("storeConfigService.getLowStockThreshold()"));
        assertTrue(src.contains("productDAO.countOutOfStockSkus()"));
        assertTrue(src.contains("productDAO.countLowStockSkus(lowStockThreshold)"));
        assertTrue(src.contains("data.put(\"lowStockThreshold\", lowStockThreshold)"));
        assertTrue(src.contains("data.put(\"outOfStockSkuCount\""));
        assertTrue(src.contains("data.put(\"lowStockSkuCount\""));
    }
}
```

Thêm vào `StaffDashboardPolicyTest`:

```java
@Test
void dashboardPublishesSharedSkuStockAlerts() throws IOException {
    String src = Files.readString(Path.of("src/main/java/service/StaffService.java"));
    assertTrue(src.contains("storeConfigService.getLowStockThreshold()"));
    assertTrue(src.contains("productDAO.countOutOfStockSkus()"));
    assertTrue(src.contains("productDAO.countLowStockSkus(lowStockThreshold)"));
    assertTrue(src.contains("data.put(\"lowStockThreshold\", lowStockThreshold)"));
    assertTrue(src.contains("data.put(\"outOfStockSkuCount\""));
    assertTrue(src.contains("data.put(\"lowStockSkuCount\""));
}
```

- [ ] **Step 3: Chạy RED chính xác**

Run:

```powershell
mvn -Dtest=dao.ProductDAOPolicyTest#dashboardStockCountsUseSkuBoundaryAndExcludeUnlimitedStock,service.AdminDashboardStockPolicyTest,service.StaffDashboardPolicyTest#dashboardPublishesSharedSkuStockAlerts test
```

Expected: FAIL vì methods/fields dashboard chưa tồn tại.

- [ ] **Step 4: Implement hai DAO aggregate queries**

Thêm vào `ProductDAO`:

```java
public long countOutOfStockSkus() {
    EntityManager em = DatabaseUtil.getEntityManager();
    try {
        return em.createQuery(
                "SELECT COUNT(v) FROM ProductVariant v WHERE v.quantityAvailable <= 0", Long.class)
                .getSingleResult();
    } finally {
        em.close();
    }
}

public long countLowStockSkus(int threshold) {
    EntityManager em = DatabaseUtil.getEntityManager();
    try {
        return em.createQuery(
                "SELECT COUNT(v) FROM ProductVariant v WHERE v.quantityAvailable > 0 AND v.quantityAvailable <= :threshold", Long.class)
                .setParameter("threshold", threshold)
                .getSingleResult();
    } finally {
        em.close();
    }
}
```

Không filter product/variant sale status: contract đo tồn kho SKU được quản lý; `null` tự bị SQL/JPQL loại khỏi comparisons.

- [ ] **Step 5: Publish exact Admin dashboard fields**

Trong `AdminService`, thêm field:

```java
private StoreConfigService storeConfigService = new StoreConfigService();
```

Trong `getDashboardWithPeriod`, trước `return data`:

```java
int lowStockThreshold = storeConfigService.getLowStockThreshold();
data.put("lowStockThreshold", lowStockThreshold);
data.put("outOfStockSkuCount", productDAO.countOutOfStockSkus());
data.put("lowStockSkuCount", productDAO.countLowStockSkus(lowStockThreshold));
```

- [ ] **Step 6: Publish cùng fields trong Staff dashboard**

Trong `StaffService`, thêm imports/fields:

```java
import dao.ProductDAO;

private ProductDAO productDAO = new ProductDAO();
private StoreConfigService storeConfigService = new StoreConfigService();
```

Trước `return data`:

```java
int lowStockThreshold = storeConfigService.getLowStockThreshold();
data.put("lowStockThreshold", lowStockThreshold);
data.put("outOfStockSkuCount", productDAO.countOutOfStockSkus());
data.put("lowStockSkuCount", productDAO.countLowStockSkus(lowStockThreshold));
```

- [ ] **Step 7: Chạy GREEN backend contracts**

Run:

```powershell
mvn -Dtest=dao.ProductDAOPolicyTest,service.AdminDashboardStockPolicyTest,service.StaffDashboardPolicyTest test
```

Expected: BUILD SUCCESS.

---

### Task 5: Refund Processor Name Read Contract Without Schema Change

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminRefundServlet.java:3-107`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/RefundPolicyTest.java:15-72`

**Interfaces:**
- Consumes: existing `Orders.getRefundProcessedBy(): Integer`, `UserDAO.findById(int): User`, `User.getFullName(): String`.
- Produces: each `GET /api/admin/refunds` row includes existing `refundProcessedBy: Integer|null` plus `refundProcessedByName: String|null`; mutation and schema unchanged.

- [ ] **Step 1: Viết RED source contract test**

Thêm vào `RefundPolicyTest`:

```java
@Test
void refundListResolvesProcessorNamesWithoutChangingOrderSchema() throws Exception {
    String servlet = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/servlet/AdminRefundServlet.java"));
    String order = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/entity/Orders.java"));
    assertTrue(servlet.contains("Map<Integer, String> processorNames"));
    assertTrue(servlet.contains("userDAO.findById(processorId)"));
    assertTrue(servlet.contains("m.put(\"refundProcessedByName\""));
    assertTrue(order.contains("private Integer refundProcessedBy"));
    assertFalse(order.contains("refundProcessedByName"));
}
```

Bổ sung static import nếu thiếu:

```java
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
mvn -Dtest=service.RefundPolicyTest#refundListResolvesProcessorNamesWithoutChangingOrderSchema test
```

Expected: FAIL tại assertion `Map<Integer, String> processorNames`.

- [ ] **Step 3: Resolve unique processor IDs trước mapping DTO**

Thêm imports/field:

```java
import dao.UserDAO;
import entity.User;

private UserDAO userDAO = new UserDAO();
```

Sau `findRefunds(...)`, build cache một lần cho mỗi unique ID:

```java
Map<Integer, String> processorNames = pending.stream()
        .map(Orders::getRefundProcessedBy)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toMap(processorId -> processorId, processorId -> {
            User processor = userDAO.findById(processorId);
            return processor == null ? "" : processor.getFullName();
        }));
```

Trong DTO mapping, giữ ID và thêm name nullable:

```java
Integer processorId = o.getRefundProcessedBy();
m.put("refundProcessedBy", processorId);
String processorName = processorId == null ? null : processorNames.get(processorId);
m.put("refundProcessedByName", processorName == null || processorName.isBlank() ? null : processorName);
```

- [ ] **Step 4: Chạy GREEN refund backend tests**

Run:

```powershell
mvn -Dtest=service.RefundPolicyTest,entity.OrdersMappingTest test
```

Expected: BUILD SUCCESS; entity vẫn chỉ persist processor ID/reference.

---

### Task 6: Refund Audit UI and Exact Pending Semantics

**Files:**
- Modify: `Frontend/src/views/admin/RefundsPage.vue:1-276`
- Modify: `Frontend/tests/refund-policy.test.mjs`

**Interfaces:**
- Consumes: refund row `{ paymentMethod, refundAmount, refundStatus, refundProcessedBy, refundProcessedByName, refundReference, refundedAt, refundNote, cancelledAt, paidAt }`.
- Produces: queue copy phân biệt `PENDING` và `REFUNDED`; terminal rows show processor/reference/time; dialog retains reference/rejection validation and accessible focus loop.

- [ ] **Step 1: Viết RED source tests cho audit metadata và pending copy**

Trong `Frontend/tests/refund-policy.test.mjs`, giữ tests utility hiện có và thêm:

```js
import { readFileSync } from 'node:fs';

const page = readFileSync(new URL('../src/views/admin/RefundsPage.vue', import.meta.url), 'utf8');

test('refund queue shows manual workflow and terminal audit metadata', () => {
  assert.match(page, /Tiền chưa được xác nhận đã hoàn/);
  assert.match(page, /refundProcessedByName/);
  assert.match(page, /refundReference/);
  assert.match(page, /refundedAt/);
  assert.match(page, /Người xử lý/);
  assert.match(page, /Mã tham chiếu/);
  assert.match(page, /Thời gian hoàn/);
});

test('refund dialog traps focus and restores trigger focus', () => {
  assert.match(page, /function handleRefundKeydown\(event\)/);
  assert.match(page, /event\.key !== 'Tab'/);
  assert.match(page, /previousFocus\.value\?\.focus\(\)/);
  assert.match(page, /@keydown="handleRefundKeydown"/);
});
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
node --test tests/refund-policy.test.mjs
```

Workdir: `Frontend`

Expected: FAIL do copy/audit fields/focus handler chưa có.

- [ ] **Step 3: Thêm helper audit display và focus trap**

Trong `<script setup>`:

```js
function processorLabel(row) {
  return row.refundProcessedByName || (row.refundProcessedBy ? `Admin #${row.refundProcessedBy}` : '—');
}

function handleRefundKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeRefund();
    return;
  }
  if (event.key !== 'Tab') return;
  const controls = [...refundDialog.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)')];
  if (!controls.length) return;
  const first = controls[0];
  const last = controls[controls.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}
```

Đổi form event thành:

```vue
<form ref="refundDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="refund-title" tabindex="-1" @keydown="handleRefundKeydown" @submit.prevent="saveRefund">
```

- [ ] **Step 4: Render audit fields trong cột hoàn tiền và làm rõ pending**

Thay nội dung cột `Hoàn tiền` bằng:

```vue
<td data-label="Hoàn tiền">
  <span v-if="row.refundStatus === 'REFUNDED'" class="refund-badge refund-done">Đã xác nhận hoàn {{ formatPrice(row.refundAmount) }}</span>
  <span v-else-if="row.refundStatus === 'REJECTED'" class="refund-badge refund-rejected">Đã từ chối</span>
  <span v-else class="refund-badge refund-pending">Chờ hoàn thủ công</span>
  <small v-if="row.refundStatus === 'PENDING'" class="sub">Tiền chưa được xác nhận đã hoàn</small>
  <dl v-else class="refund-audit">
    <div><dt>Người xử lý</dt><dd>{{ processorLabel(row) }}</dd></div>
    <div v-if="row.refundStatus === 'REFUNDED'"><dt>Mã tham chiếu</dt><dd>{{ row.refundReference || '—' }}</dd></div>
    <div v-if="row.refundStatus === 'REFUNDED'"><dt>Thời gian hoàn</dt><dd>{{ row.refundedAt ? formatDate(row.refundedAt) : '—' }}</dd></div>
    <div v-if="row.refundStatus === 'REJECTED'"><dt>Lý do</dt><dd>{{ row.refundNote || '—' }}</dd></div>
  </dl>
</td>
```

Thêm CSS compact, không chỉ dùng màu:

```css
.refund-audit { display: grid; gap: 3px; margin: 6px 0 0; font-size: 11px; }
.refund-audit div { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 6px; }
.refund-audit dt { color: var(--text-mid); }
.refund-audit dd { margin: 0; overflow-wrap: anywhere; }
```

- [ ] **Step 5: Làm rõ manual workflow trong header/dialog**

Đổi page subtitle thành:

```vue
<p>Xác nhận kết quả hoàn tiền đã thực hiện ngoài hệ thống và lưu audit.</p>
```

Trong dialog order info, thêm:

```vue
<div><span>Trạng thái</span><strong>Chờ hoàn thủ công · Tiền chưa được xác nhận đã hoàn</strong></div>
```

Giữ amount readonly bằng `finalAmount`, reference bắt buộc cho `REFUNDED`, note bắt buộc cho `REJECTED`.

- [ ] **Step 6: Chạy GREEN refund frontend tests và build**

Run:

```powershell
node --test tests/refund-policy.test.mjs; if ($?) { npm run build }
```

Workdir: `Frontend`

Expected: tests PASS, Vite build success.

---

### Task 7: Shared Frontend Stock Policy and Inventory/Products Consistency

**Files:**
- Create: `Frontend/src/utils/stockPolicy.js`
- Create: `Frontend/tests/stock-policy.test.mjs`
- Modify: `Frontend/src/views/admin/InventoryPage.vue:1-435`
- Modify: `Frontend/src/views/admin/ProductsPage.vue:1-229`
- Modify: `Frontend/tests/admin-product-catalog.test.mjs:1-33`

**Interfaces:**
- Consumes: backend dashboard `lowStockThreshold`; product variants with `quantityAvailable`.
- Produces: `normalizeLowStockThreshold(value): number`; `stockState(quantity, threshold): 'UNMANAGED'|'OUT'|'LOW'|'IN'`; `productStockSummary(product, threshold): { total:number|null, outOfStockSkus:number, lowStockSkus:number, managedSkus:number }`.

- [ ] **Step 1: Viết RED unit tests cho exact threshold boundaries**

Tạo `Frontend/tests/stock-policy.test.mjs`:

```js
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  normalizeLowStockThreshold,
  productStockSummary,
  stockState,
} from '../src/utils/stockPolicy.js';

test('normalizes backend threshold with default five', () => {
  assert.equal(normalizeLowStockThreshold(7), 7);
  assert.equal(normalizeLowStockThreshold('7'), 7);
  assert.equal(normalizeLowStockThreshold(0), 5);
  assert.equal(normalizeLowStockThreshold(1001), 5);
  assert.equal(normalizeLowStockThreshold('bad'), 5);
});

test('classifies managed and unlimited SKU boundaries', () => {
  assert.equal(stockState(null, 5), 'UNMANAGED');
  assert.equal(stockState(0, 5), 'OUT');
  assert.equal(stockState(-1, 5), 'OUT');
  assert.equal(stockState(1, 5), 'LOW');
  assert.equal(stockState(5, 5), 'LOW');
  assert.equal(stockState(6, 5), 'IN');
});

test('summarizes product stock without treating unlimited as low', () => {
  const product = { variants: [
    { quantityAvailable: null },
    { quantityAvailable: 0 },
    { quantityAvailable: 5 },
    { quantityAvailable: 8 },
  ] };
  assert.deepEqual(productStockSummary(product, 5), {
    total: null,
    outOfStockSkus: 1,
    lowStockSkus: 1,
    managedSkus: 3,
  });
});
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
node --test tests/stock-policy.test.mjs
```

Workdir: `Frontend`

Expected: FAIL `ERR_MODULE_NOT_FOUND` cho `stockPolicy.js`.

- [ ] **Step 3: Implement utility thuần tối thiểu**

Tạo `Frontend/src/utils/stockPolicy.js`:

```js
export const DEFAULT_LOW_STOCK_THRESHOLD = 5;

export function normalizeLowStockThreshold(value) {
  const threshold = Number(value);
  return Number.isInteger(threshold) && threshold >= 1 && threshold <= 1000
    ? threshold
    : DEFAULT_LOW_STOCK_THRESHOLD;
}

export function stockState(quantity, thresholdValue) {
  if (quantity === null || quantity === undefined) return 'UNMANAGED';
  const stock = Number(quantity);
  const threshold = normalizeLowStockThreshold(thresholdValue);
  if (stock <= 0) return 'OUT';
  if (stock <= threshold) return 'LOW';
  return 'IN';
}

export function productStockSummary(product, thresholdValue) {
  const variants = Array.isArray(product?.variants) ? product.variants : [];
  const states = variants.map((variant) => ({
    quantity: variant.quantityAvailable,
    state: stockState(variant.quantityAvailable, thresholdValue),
  }));
  const managed = states.filter(({ state }) => state !== 'UNMANAGED');
  return {
    total: states.some(({ state }) => state === 'UNMANAGED')
      ? null
      : managed.reduce((sum, { quantity }) => sum + Number(quantity || 0), 0),
    outOfStockSkus: managed.filter(({ state }) => state === 'OUT').length,
    lowStockSkus: managed.filter(({ state }) => state === 'LOW').length,
    managedSkus: managed.length,
  };
}
```

- [ ] **Step 4: Chạy utility GREEN**

Run:

```powershell
node --test tests/stock-policy.test.mjs
```

Expected: 3 tests PASS.

- [ ] **Step 5: Inventory dùng dashboard threshold và `stockState` ở mọi boundary**

Trong `InventoryPage.vue`, import:

```js
import { normalizeLowStockThreshold, stockState } from '@/utils/stockPolicy';
```

Trong `loadProducts`, fetch dashboard cùng products để nhận config contract:

```js
await Promise.all([adminStore.fetchProducts(), adminStore.fetchDashboard()]);
```

Thêm:

```js
const lowStockThreshold = computed(() => normalizeLowStockThreshold(adminStore.dashboard?.lowStockThreshold));
```

Thay exact computed/filter/labels/classes:

```js
const outOfStockRows = computed(() => managedRows.value.filter((row) => stockState(row.stock, lowStockThreshold.value) === 'OUT'));
const lowStockRows = computed(() => managedRows.value.filter((row) => stockState(row.stock, lowStockThreshold.value) === 'LOW'));
```

```js
if (activeFilter.value === 'OUT') return stockState(row.stock, lowStockThreshold.value) === 'OUT';
if (activeFilter.value === 'LOW') return stockState(row.stock, lowStockThreshold.value) === 'LOW';
```

```js
const state = stockState(row.stock, lowStockThreshold.value);
if (state === 'UNMANAGED') return 'Không giới hạn';
if (state === 'OUT') return 'Hết hàng';
if (state === 'LOW') return 'Sắp hết';
```

Dùng cùng `state` trong `statusClass`; bỏ mọi comparison `<= 5`.

Hiển thị boundary trong label:

```vue
<span class="stat-label">Sắp hết (1–{{ lowStockThreshold }})</span>
```

- [ ] **Step 6: Products dùng cùng utility, không aggregate sai threshold**

Trong `ProductsPage.vue`, import:

```js
import { normalizeLowStockThreshold, productStockSummary } from '@/utils/stockPolicy';
```

Load dashboard cùng catalog:

```js
await Promise.all([adminStore.fetchProducts(), adminStore.fetchCategories(), adminStore.fetchDashboard()]);
```

Thay `stockOf`:

```js
const lowStockThreshold = computed(() => normalizeLowStockThreshold(adminStore.dashboard?.lowStockThreshold));

function stockSummary(product) {
  return productStockSummary(product, lowStockThreshold.value);
}

function stockOf(product) {
  return stockSummary(product).total;
}
```

Thay filter stock:

```js
const summary = stockSummary(product);
return !stockFilter.value
  || (stockFilter.value === 'unlimited' ? summary.total === null
    : stockFilter.value === 'out' ? summary.outOfStockSkus > 0
      : stockFilter.value === 'low' ? summary.lowStockSkus > 0
        : summary.outOfStockSkus === 0 && summary.lowStockSkus === 0);
```

Thay copy options:

```vue
<option value="in">Còn hàng trên {{ lowStockThreshold }}</option>
<option value="low">Có SKU sắp hết (1–{{ lowStockThreshold }})</option>
<option value="out">Có SKU hết hàng</option>
```

Không dùng `product.inStock` để quyết định low/out filter; field đó chỉ còn phục vụ canonical sale availability nơi khác.

- [ ] **Step 7: Thêm source regression chống hardcode drift**

Thêm vào `admin-product-catalog.test.mjs`:

```js
import { readFileSync } from 'node:fs';

const productsPage = readFileSync(new URL('../src/views/admin/ProductsPage.vue', import.meta.url), 'utf8');
const inventoryPage = readFileSync(new URL('../src/views/admin/InventoryPage.vue', import.meta.url), 'utf8');

test('products and inventory consume one shared low-stock policy', () => {
  assert.match(productsPage, /productStockSummary/);
  assert.match(productsPage, /lowStockThreshold/);
  assert.doesNotMatch(productsPage, /stock <= 10|stock > 10/);
  assert.match(inventoryPage, /stockState/);
  assert.match(inventoryPage, /lowStockThreshold/);
  assert.doesNotMatch(inventoryPage, /stock <= 5|stock > 0 && row\.stock <= 5/);
});
```

- [ ] **Step 8: Chạy GREEN frontend stock suite và build**

Run:

```powershell
node --test tests/stock-policy.test.mjs tests/admin-product-catalog.test.mjs; if ($?) { npm run build }
```

Workdir: `Frontend`

Expected: tests PASS; Vite build success.

---

### Task 8: Admin Dashboard Stock Attention Cards

**Files:**
- Modify: `Frontend/src/views/admin/DashboardPage.vue:1-247`
- Create: `Frontend/tests/admin-dashboard-stock-policy.test.mjs`

**Interfaces:**
- Consumes: Admin dashboard `lowStockThreshold`, `outOfStockSkuCount`, `lowStockSkuCount`.
- Produces: visible cards/links to `/admin/inventory?filter=OUT|LOW`; zero values remain visible; no persistent notifications.

- [ ] **Step 1: Viết RED source test**

Tạo `Frontend/tests/admin-dashboard-stock-policy.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('admin dashboard exposes out and low stock SKU counts with inventory actions', () => {
  assert.match(page, /outOfStockSkuCount/);
  assert.match(page, /lowStockSkuCount/);
  assert.match(page, /lowStockThreshold/);
  assert.match(page, /Hết hàng/);
  assert.match(page, /Sắp hết/);
  assert.match(page, /\/admin\/inventory/);
});

test('admin stock alerts remain current response UI only', () => {
  assert.doesNotMatch(page, /Notification|markRead|notify/);
});
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
node --test tests/admin-dashboard-stock-policy.test.mjs
```

Workdir: `Frontend`

Expected: FAIL do dashboard chưa consume stock fields.

- [ ] **Step 3: Thêm hai stock cards vào operation strip**

Mở rộng `operation-strip` với:

```vue
<div>
  <span class="signal danger"><i class="bi bi-x-octagon"></i></span>
  <p>SKU hết hàng<strong>{{ Number(data.outOfStockSkuCount || 0) }}</strong></p>
  <router-link :to="{ path: '/admin/inventory', query: { filter: 'OUT' } }">Kiểm tra</router-link>
</div>
<div>
  <span class="signal warning"><i class="bi bi-exclamation-triangle"></i></span>
  <p>SKU sắp hết<strong>{{ Number(data.lowStockSkuCount || 0) }}</strong></p>
  <router-link :to="{ path: '/admin/inventory', query: { filter: 'LOW' } }">Ngưỡng ≤ {{ data.lowStockThreshold }}</router-link>
</div>
```

Đổi grid CSS của strip để 5 cards wrap responsive theo pattern hiện có; mỗi icon kèm text label nên màu không là tín hiệu duy nhất.

- [ ] **Step 4: Inventory nhận filter query để action có đích đúng**

Trong `InventoryPage.vue`, đổi router import/use:

```js
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
```

Trước `onMounted(loadProducts)`, thêm:

```js
const requestedFilter = String(route.query.filter || '').toUpperCase();
if (['ALL', 'LOW', 'OUT', 'UNMANAGED', 'UNAVAILABLE'].includes(requestedFilter)) {
  activeFilter.value = requestedFilter;
}
```

- [ ] **Step 5: Chạy GREEN dashboard test và build**

Run:

```powershell
node --test tests/admin-dashboard-stock-policy.test.mjs tests/stock-policy.test.mjs; if ($?) { npm run build }
```

Workdir: `Frontend`

Expected: tests PASS; build success.

---

### Task 9: Staff Current-Response Stock Alert

**Files:**
- Modify: `Frontend/src/views/staff/DashboardPage.vue:1-147`
- Modify: `Frontend/tests/staff-dashboard-policy.test.mjs:1-28`

**Interfaces:**
- Consumes: Staff dashboard `lowStockThreshold`, `outOfStockSkuCount`, `lowStockSkuCount`.
- Produces: “Cần xử lý ngay” tổng count gồm stock alerts; explanatory counts rendered in current dashboard only; action routes to staff orders because Staff has no inventory mutation screen.

- [ ] **Step 1: Viết RED source tests**

Thêm vào `staff-dashboard-policy.test.mjs`:

```js
test('staff dashboard includes current low-stock signals without persistent notifications', () => {
  assert.match(dashboard, /outOfStockSkuCount/);
  assert.match(dashboard, /lowStockSkuCount/);
  assert.match(dashboard, /lowStockThreshold/);
  assert.match(dashboard, /SKU hết hàng/);
  assert.match(dashboard, /SKU sắp hết/);
  assert.doesNotMatch(dashboard, /Notification|markRead|notify/);
});
```

- [ ] **Step 2: Chạy RED chính xác**

Run:

```powershell
node --test tests/staff-dashboard-policy.test.mjs
```

Workdir: `Frontend`

Expected: FAIL vì stock fields/copy chưa có.

- [ ] **Step 3: Mở rộng alert count và copy**

Thay computed:

```js
const alertCount = computed(() => Number(data.value.overdueOrders || 0)
  + Number(data.value.awaitingShipperOrders || 0)
  + Number(data.value.outOfStockSkuCount || 0)
  + Number(data.value.lowStockSkuCount || 0));
```

Thay alert content:

```vue
<section v-if="alertCount" class="operations-alert" role="status">
  <i class="bi bi-exclamation-circle"></i>
  <div>
    <strong>{{ alertCount }} tín hiệu cần chú ý</strong>
    <span>
      {{ data.overdueOrders || 0 }} đơn quá thời gian ·
      {{ data.awaitingShipperOrders || 0 }} đơn chờ shipper ·
      {{ data.outOfStockSkuCount || 0 }} SKU hết hàng ·
      {{ data.lowStockSkuCount || 0 }} SKU sắp hết (ngưỡng ≤ {{ data.lowStockThreshold }})
    </span>
  </div>
  <button class="btn btn-sm btn-outline" @click="goOrders('PENDING')">Kiểm tra hàng đợi</button>
</section>
```

Không gọi notification API, không thêm store/state read receipt, không cho Staff chỉnh tồn kho.

- [ ] **Step 4: Chạy GREEN staff test và build**

Run:

```powershell
node --test tests/staff-dashboard-policy.test.mjs; if ($?) { npm run build }
```

Workdir: `Frontend`

Expected: tests PASS; build success.

---

### Task 10: Cross-Layer Verification and Final Review

**Files:**
- Review only: mọi file trong File Map.

**Interfaces:**
- Consumes: tất cả contracts từ Tasks 1–9.
- Produces: bằng chứng test/build pass, không dependency/schema/notification drift, diff chỉ trong scope.

- [ ] **Step 1: Chạy full backend tests**

Run:

```powershell
mvn test
```

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: BUILD SUCCESS, 0 failures, 0 errors.

- [ ] **Step 2: Chạy full frontend tests**

Run:

```powershell
npm test
```

Workdir: `Frontend`

Expected: toàn bộ Node tests PASS.

- [ ] **Step 3: Chạy frontend production build**

Run:

```powershell
npm run build
```

Workdir: `Frontend`

Expected: Vite build success; không unresolved import/compiler error. Project không cung cấp lint hoặc typecheck script riêng trong `Frontend/package.json`, nên không bịa lệnh.

- [ ] **Step 4: Quét invariant cấm schema/dependency/persistent notification**

Run từ workspace root:

```powershell
rg -n "low_stock_threshold|refundProcessedByName|outOfStockSkuCount|lowStockSkuCount" Backend Frontend database; rg -n "Notification|notifyUser|markRead" Frontend/src/views/admin/DashboardPage.vue Frontend/src/views/staff/DashboardPage.vue
```

Expected:

- `047_low_stock_threshold.sql` phụ thuộc `046_cod_shift_settlement`, chỉ upsert config row và không tạo table/column.
- `047_validate.sql`, `database/init.sql`, `database/DB_FastGuy.sql`, backend service và Admin Settings cùng dùng key/default/range `low_stock_threshold` / `5` / `1..1000`.
- `refundProcessedByName` chỉ là read DTO/frontend display; không nằm trong `entity/Orders.java` hoặc refund SQL columns.
- `outOfStockSkuCount` và `lowStockSkuCount` có ở cả backend services và dashboard UIs.
- Lệnh quét notification không trả match trong hai dashboard files.

- [ ] **Step 5: Tự rà soát coverage spec và placeholder**

Run:

```powershell
rg -n "TBD|TODO|implement later|fill in details|Similar to Task|Add appropriate error handling|add validation|handle edge cases" docs/superpowers/plans/2026-08-14-refund-low-stock-operations.md
```

Expected: không match. Sau đó đối chiếu thủ công:

- Refund UI có method, amount, pending semantics, processor name, reference, refunded time.
- Processor name không đổi schema.
- Một threshold dùng cho backend counts, Inventory và Products.
- Admin có out/low SKU counts.
- Staff alert có queue risk counts trong response hiện tại.
- Không persistent notification, dependency, partial refund hoặc automatic provider refund.

- [ ] **Step 6: Review diff và status, không stage/commit**

Run:

```powershell
git diff --check; git diff -- Backend/FastGuy-FastFoodSite/src Backend/FastGuy-FastFoodSite/src/test Frontend/src Frontend/tests; git status --short
```

Expected:

- `git diff --check` không lỗi whitespace.
- Diff chỉ chạm files trong File Map.
- Không có migration, dependency manifest hoặc notification persistence change.
- `git status --short` giữ thay đổi unstaged/uncommitted để người dùng review.
