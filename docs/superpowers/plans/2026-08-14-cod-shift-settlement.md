# COD Shift Settlement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây đối soát COD theo ca cho Shipper/Admin và thay metric COD chưa nộp tạm tính bằng số liệu settlement thật.

**Architecture:** Thêm bảng `CodSettlement` liên kết duy nhất `(shipper_id, shift_id)`, giữ snapshot tiền kỳ vọng và các lần nộp/xác nhận; service dùng transaction cùng pessimistic lock trên ca và settlement để chống request lặp, chỉnh âm thầm và xác nhận terminal hai lần. Một servlet phục vụ API Shipper/Admin theo role, còn Vue dùng API module chung cho trang bàn giao mobile-first, hàng đợi Admin và metric dashboard; doanh thu hiện có không đổi.

**Tech Stack:** Java 17, Jakarta Servlet 6.1, Jakarta Persistence 3.1/Hibernate 6.6, SQL Server, JUnit 5, Vue 3 Composition API, Vue Router 4, Axios, Node test runner, Vite 8.

## Global Constraints

- Phạm vi chỉ COD settlement theo ca, Shipper/Admin UI và metric pending thật; không triển khai delivery failure, refund, inventory hoặc Customer quick wins.
- Giữ Vue 3, Pinia, servlet/service/DAO/entity và SQL migration hiện có.
- Không thêm dependency khi native platform hoặc utility hiện có đáp ứng.
- Không tạo vai trò Manager; Admin giữ chức năng quản lý cửa hàng.
- Chỉ tổng hợp đơn `payment_method = 'COD'`, `order_status = 'DELIVERED'`, có `cod_collected_amount`, đúng Shipper và `delivered_at` nằm trong `[shift_date + start_time, shift_date + end_time)`.
- Shipper chỉ được tạo bàn giao cho ca thuộc chính mình, tài khoản `ACTIVE`, role `SHIPPER`, ca đã check-in; `submittedAmount >= 0`.
- Tạo bàn giao chụp `expectedAmount`; request lặp cho cùng Shipper/ca không tạo settlement thứ hai và trả `409` nếu settlement đã tồn tại.
- `SUBMITTED` không được Shipper sửa âm thầm; Admin chỉ chuyển `SUBMITTED` sang `SETTLED`, `SHORT` hoặc `OVER`.
- `verifiedAmount >= 0`; `SHORT` và `OVER` bắt buộc `reason` không rỗng; `SETTLED` yêu cầu `verifiedAmount == submittedAmount`, `SHORT` yêu cầu nhỏ hơn, `OVER` yêu cầu lớn hơn.
- Lưu Shipper, Admin nhận, ca, expected amount, submitted amount, verified amount, reason và timestamps.
- Mutation chạy trong transaction, dùng pessimistic lock và rollback khi lỗi.
- `400`: payload/amount/reason không hợp lệ; `401`: thiếu auth; `403`: role/ownership/active/shift sai; `404`: settlement/shift ngoài phạm vi; `409`: settlement trùng, expected status cũ hoặc terminal state.
- Revenue không phụ thuộc settlement; không sửa query doanh thu. Metric `pendingCodAmount` là tổng `submitted_amount` của `SUBMITTED`, không phải tổng COD hôm nay.
- UI có loading, empty, error, retry, conflict và success; mutation error không bị nuốt.
- Shipper mobile-first, hit area tối thiểu `44x44 px`; Admin desktop table và mobile card; màu không là tín hiệu trạng thái duy nhất.
- Input có label, lỗi liên kết bằng `aria-describedby`, mutation result dùng `role="status"`/`role="alert"`; dialog có accessible name, Escape, focus trap và trả focus.
- Hỗ trợ `prefers-reduced-motion`; không dùng browser `confirm()`.
- Không commit. Cuối mỗi task chỉ review `git diff`/`git status`.

---

## File Map

- Create: `database/migrations/046_cod_shift_settlement.sql` — schema, constraints, unique key, indexes và migration history.
- Create: `database/migrations/046_cod_shift_settlement_validate.sql` — validator độc lập cho bảng, constraints, indexes và dữ liệu retained.
- Modify: `database/init.sql` — đồng bộ DDL `CodSettlement` cho fresh install, không thêm seed settlement.
- Modify: `database/DB_FastGuy.sql` — đồng bộ DDL `CodSettlement` cho fresh install, không thêm seed settlement.
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/entity/CodSettlement.java` — JPA mapping settlement.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml` — đăng ký entity.
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/dao/CodSettlementDAO.java` — truy vấn settlement, COD theo cửa sổ ca và pending metric.
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java` — validation, auth boundary, locking, submit/verify và DTO maps.
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/CodSettlementServlet.java` — route `/api/cod-settlements/*`, JWT role mapping và HTTP errors.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java` — thêm `pendingCodAmount`/`pendingCodCount` thật vào dashboard.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/entity/CodSettlementMappingTest.java` — source mapping contract.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementPolicyTest.java` — amount/status/reason policy unit tests.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementServiceContractTest.java` — transaction/lock/idempotency/ownership source contract.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/CodSettlementApiContractTest.java` — API paths, role checks và status mapping.
- Create: `Frontend/src/api/codSettlement.js` — API calls cho Shipper/Admin.
- Modify: `Frontend/src/api/index.js` — export API module.
- Modify: `Frontend/src/views/shipper/CashPage.vue` — expected/submitted/result, submit form, history và states.
- Create: `Frontend/src/views/admin/CodSettlementsPage.vue` — pending queue, verify dialog, mismatch history và responsive states.
- Modify: `Frontend/src/router/index.js` — route/title Admin COD.
- Modify: `Frontend/src/layouts/AdminLayout.vue` — Admin navigation link.
- Modify: `Frontend/src/views/admin/DashboardPage.vue` — pending COD metric thật và link queue.
- Create: `Frontend/test/cod-settlement-api.test.js` — API/route contracts.
- Create: `Frontend/test/cod-settlement-ui.test.js` — source tests cho states, accessibility, copy và metric.

---

### Task 1: Schema và JPA mapping

**Files:**
- Create: `database/migrations/046_cod_shift_settlement.sql`
- Create: `database/migrations/046_cod_shift_settlement_validate.sql`
- Modify: `database/init.sql` immediately after `dbo.WorkShift` DDL and before dependent seed sections
- Modify: `database/DB_FastGuy.sql` immediately after `dbo.WorkShift` DDL
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/entity/CodSettlement.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml:30-34`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/entity/CodSettlementMappingTest.java`

**Interfaces:**
- Consumes: `entity.User`, `entity.WorkShift`, migration convention `SchemaMigrationHistory(migration_id, details)`.
- Produces: entity `CodSettlement`; fields `settlementId:int`, `shipper:User`, `shift:WorkShift`, `receivedBy:User`, `status:String`, `expectedAmount:BigDecimal`, `submittedAmount:BigDecimal`, `verifiedAmount:BigDecimal`, `reason:String`, `submittedAt:LocalDateTime`, `verifiedAt:LocalDateTime`, `createdAt:LocalDateTime`, `updatedAt:LocalDateTime`.

- [ ] **Step 1: Write failing mapping test**

```java
package entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CodSettlementMappingTest {
    @Test
    void mapsSettlementColumnsAndPersistenceUnit() throws IOException {
        String entity = Files.readString(Path.of("src/main/java/entity/CodSettlement.java"));
        String persistence = Files.readString(Path.of("src/main/resources/META-INF/persistence.xml"));

        assertTrue(entity.contains("@Table(name = \"CodSettlement\""));
        assertTrue(entity.contains("@JoinColumn(name = \"shipper_id\")"));
        assertTrue(entity.contains("@JoinColumn(name = \"shift_id\")"));
        assertTrue(entity.contains("@JoinColumn(name = \"received_by\")"));
        assertTrue(entity.contains("@Column(name = \"expected_amount\", precision = 18, scale = 2)"));
        assertTrue(entity.contains("@Column(name = \"submitted_amount\", precision = 18, scale = 2)"));
        assertTrue(entity.contains("@Column(name = \"verified_amount\", precision = 18, scale = 2)"));
        assertTrue(persistence.contains("<class>entity.CodSettlement</class>"));
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=entity.CodSettlementMappingTest test`

Expected: FAIL during test compilation because `CodSettlementMappingTest` reads missing `src/main/java/entity/CodSettlement.java`, or test FAIL with `NoSuchFileException`.

- [ ] **Step 3: Add migration with exact constraints and indexes**

Create `database/migrations/046_cod_shift_settlement.sql`:

```sql
USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51460, 'Run 000_preflight_history.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement')
    PRINT '046_cod_shift_settlement already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        CREATE TABLE dbo.CodSettlement (
            settlement_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CodSettlement PRIMARY KEY,
            shipper_id int NOT NULL,
            shift_id int NOT NULL,
            received_by int NULL,
            status varchar(20) NOT NULL CONSTRAINT DF_CodSettlement_Status DEFAULT 'SUBMITTED',
            expected_amount decimal(18,2) NOT NULL,
            submitted_amount decimal(18,2) NOT NULL,
            verified_amount decimal(18,2) NULL,
            reason nvarchar(500) NULL,
            submitted_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_SubmittedAt DEFAULT SYSUTCDATETIME(),
            verified_at datetime2(0) NULL,
            created_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_CreatedAt DEFAULT SYSUTCDATETIME(),
            updated_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_UpdatedAt DEFAULT SYSUTCDATETIME(),
            CONSTRAINT FK_CodSettlement_Shipper FOREIGN KEY (shipper_id) REFERENCES dbo.Users(user_id),
            CONSTRAINT FK_CodSettlement_Shift FOREIGN KEY (shift_id) REFERENCES dbo.WorkShift(shift_id),
            CONSTRAINT FK_CodSettlement_ReceivedBy FOREIGN KEY (received_by) REFERENCES dbo.Users(user_id),
            CONSTRAINT UQ_CodSettlement_ShipperShift UNIQUE (shipper_id, shift_id),
            CONSTRAINT CK_CodSettlement_Status CHECK (status IN ('SUBMITTED','SETTLED','SHORT','OVER')),
            CONSTRAINT CK_CodSettlement_Amounts CHECK (expected_amount >= 0 AND submitted_amount >= 0 AND (verified_amount IS NULL OR verified_amount >= 0)),
            CONSTRAINT CK_CodSettlement_Verification CHECK (
                (status = 'SUBMITTED' AND received_by IS NULL AND verified_amount IS NULL AND verified_at IS NULL)
                OR (status = 'SETTLED' AND received_by IS NOT NULL AND verified_amount = submitted_amount AND verified_at IS NOT NULL)
                OR (status = 'SHORT' AND received_by IS NOT NULL AND verified_amount < submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
                OR (status = 'OVER' AND received_by IS NOT NULL AND verified_amount > submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
            )
        );
        CREATE INDEX IX_CodSettlement_StatusSubmittedAt ON dbo.CodSettlement(status, submitted_at DESC);
        CREATE INDEX IX_CodSettlement_ShipperSubmittedAt ON dbo.CodSettlement(shipper_id, submitted_at DESC);
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('046_cod_shift_settlement', N'Added shift-scoped COD settlement with immutable submission and Admin verification constraints');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
```

- [ ] **Step 4: Add exact migration validator**

Create `database/migrations/046_cod_shift_settlement_validate.sql`:

```sql
USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51461, '046 migration history missing.', 1;
    IF OBJECT_ID(N'dbo.CodSettlement', N'U') IS NULL THROW 51462, 'CodSettlement table missing.', 1;
    IF OBJECT_ID(N'dbo.UQ_CodSettlement_ShipperShift', N'UQ') IS NULL THROW 51463, 'Shipper/shift unique constraint missing.', 1;
    IF OBJECT_ID(N'dbo.CK_CodSettlement_Status', N'C') IS NULL OR OBJECT_ID(N'dbo.CK_CodSettlement_Amounts', N'C') IS NULL OR OBJECT_ID(N'dbo.CK_CodSettlement_Verification', N'C') IS NULL THROW 51464, 'COD settlement checks missing.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CodSettlement') AND name = N'IX_CodSettlement_StatusSubmittedAt') THROW 51465, 'Pending queue index missing.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CodSettlement GROUP BY shipper_id, shift_id HAVING COUNT(*) > 1) THROW 51466, 'Duplicate shipper/shift settlement.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CodSettlement cs JOIN dbo.WorkShift ws ON ws.shift_id = cs.shift_id WHERE ws.user_id <> cs.shipper_id) THROW 51467, 'Settlement shipper differs from shift owner.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT '046 COD settlement validation passed.';
```

- [ ] **Step 5: Synchronize exact fresh-install schema in both canonical SQL files**

Insert the same `CREATE TABLE dbo.CodSettlement (...)`, `IX_CodSettlement_StatusSubmittedAt`, and `IX_CodSettlement_ShipperSubmittedAt` definitions from Step 3 into both `database/init.sql` and `database/DB_FastGuy.sql` immediately after `dbo.WorkShift` is created. Preserve every column type, nullability, default, FK, unique constraint, check constraint, and index name byte-for-byte; omit only migration-wrapper statements (`SchemaMigrationHistory`, transaction wrapper, migration insert, `GO`). Do not add sample `CodSettlement` rows: fresh installs start with zero settlements while existing `WorkShift`/`Orders` seed remains unchanged.

Exact synchronized block:

```sql
CREATE TABLE dbo.CodSettlement (
    settlement_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CodSettlement PRIMARY KEY,
    shipper_id int NOT NULL,
    shift_id int NOT NULL,
    received_by int NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_CodSettlement_Status DEFAULT 'SUBMITTED',
    expected_amount decimal(18,2) NOT NULL,
    submitted_amount decimal(18,2) NOT NULL,
    verified_amount decimal(18,2) NULL,
    reason nvarchar(500) NULL,
    submitted_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_SubmittedAt DEFAULT SYSUTCDATETIME(),
    verified_at datetime2(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_CreatedAt DEFAULT SYSUTCDATETIME(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_UpdatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_CodSettlement_Shipper FOREIGN KEY (shipper_id) REFERENCES dbo.Users(user_id),
    CONSTRAINT FK_CodSettlement_Shift FOREIGN KEY (shift_id) REFERENCES dbo.WorkShift(shift_id),
    CONSTRAINT FK_CodSettlement_ReceivedBy FOREIGN KEY (received_by) REFERENCES dbo.Users(user_id),
    CONSTRAINT UQ_CodSettlement_ShipperShift UNIQUE (shipper_id, shift_id),
    CONSTRAINT CK_CodSettlement_Status CHECK (status IN ('SUBMITTED','SETTLED','SHORT','OVER')),
    CONSTRAINT CK_CodSettlement_Amounts CHECK (expected_amount >= 0 AND submitted_amount >= 0 AND (verified_amount IS NULL OR verified_amount >= 0)),
    CONSTRAINT CK_CodSettlement_Verification CHECK (
        (status = 'SUBMITTED' AND received_by IS NULL AND verified_amount IS NULL AND verified_at IS NULL)
        OR (status = 'SETTLED' AND received_by IS NOT NULL AND verified_amount = submitted_amount AND verified_at IS NOT NULL)
        OR (status = 'SHORT' AND received_by IS NOT NULL AND verified_amount < submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
        OR (status = 'OVER' AND received_by IS NOT NULL AND verified_amount > submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
    )
);
CREATE INDEX IX_CodSettlement_StatusSubmittedAt ON dbo.CodSettlement(status, submitted_at DESC);
CREATE INDEX IX_CodSettlement_ShipperSubmittedAt ON dbo.CodSettlement(shipper_id, submitted_at DESC);
```

- [ ] **Step 6: Validate migration/fresh-install DDL parity before JPA work**

Run from repository root:

```powershell
$files = @('database/migrations/046_cod_shift_settlement.sql','database/init.sql','database/DB_FastGuy.sql'); $required = @('CREATE TABLE dbo.CodSettlement','UQ_CodSettlement_ShipperShift','CK_CodSettlement_Status','CK_CodSettlement_Amounts','CK_CodSettlement_Verification','IX_CodSettlement_StatusSubmittedAt','IX_CodSettlement_ShipperSubmittedAt'); foreach ($file in $files) { $sql = Get-Content -Raw $file; foreach ($token in $required) { if ($sql -notmatch [regex]::Escape($token)) { throw "$file missing $token" } }; if ($sql -notmatch "status IN \('SUBMITTED','SETTLED','SHORT','OVER'\)") { throw "$file status set differs" } }; $fresh = @('database/init.sql','database/DB_FastGuy.sql'); foreach ($file in $fresh) { $sql = Get-Content -Raw $file; if ([regex]::Matches($sql, 'CREATE TABLE dbo\.CodSettlement').Count -ne 1) { throw "$file must define CodSettlement exactly once" }; if ($sql -match 'INSERT\s+(dbo\.)?CodSettlement') { throw "$file must not seed CodSettlement" } }; 'COD schema parity checks passed.'
```

Expected: `COD schema parity checks passed.`

- [ ] **Step 7: Add minimal entity and register it**

Create `CodSettlement.java` with annotations matching test and conventional getters/setters. Use `@PrePersist` to set `createdAt`, `updatedAt`, `submittedAt`; use `@PreUpdate` only for `updatedAt`. Add `<class>entity.CodSettlement</class>` after `entity.WorkShift` in `persistence.xml`. Exact class signature:

```java
@Entity
@Table(name = "CodSettlement", uniqueConstraints = @UniqueConstraint(name = "UQ_CodSettlement_ShipperShift", columnNames = {"shipper_id", "shift_id"}))
public class CodSettlement {
    // fields listed in Interfaces, Jakarta annotations, public getters/setters
}
```

- [ ] **Step 8: Run mapping test and backend compile to verify GREEN**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=entity.CodSettlementMappingTest test && mvn -DskipTests package`

Expected: `Tests run: 1, Failures: 0, Errors: 0` and `BUILD SUCCESS` twice.

- [ ] **Step 9: Review task diff without commit**

Run: `git diff -- database/migrations/046_cod_shift_settlement.sql database/migrations/046_cod_shift_settlement_validate.sql database/init.sql database/DB_FastGuy.sql Backend/FastGuy-FastFoodSite/src/main/java/entity/CodSettlement.java Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml Backend/FastGuy-FastFoodSite/src/test/java/entity/CodSettlementMappingTest.java && git status --short`

Expected: only seven intended paths shown; `init.sql`, `DB_FastGuy.sql`, and migration share identical `CodSettlement` DDL; no seed settlement, dependency, or unrelated schema change.

---

### Task 2: Settlement policy, DAO và transactional service

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/dao/CodSettlementDAO.java`
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementPolicyTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementServiceContractTest.java`

**Interfaces:**
- Consumes: `CodSettlement`, `WorkShift`, `User`, `DatabaseUtil.getEntityManager()`, `WorkShiftService.BUSINESS_ZONE` semantics (`Asia/Ho_Chi_Minh`).
- Produces: `record SettlementConflictException(String message)` is not valid Java exception shape; implement `public static final class SettlementConflictException extends RuntimeException { public SettlementConflictException(String message) { super(message); } }`.
- Produces: `static void validateSubmission(BigDecimal submittedAmount)`; `static void validateVerification(String status, BigDecimal submittedAmount, BigDecimal verifiedAmount, String reason)`.
- Produces: `Map<String,Object> getShipperCurrent(int shipperId)`; `Map<String,Object> submit(int shipperId, int shiftId, BigDecimal submittedAmount)`; `List<Map<String,Object>> listForShipper(int shipperId)`; `List<Map<String,Object>> listForAdmin(String status)`; `Map<String,Object> verify(int adminId, int settlementId, String expectedStatus, String status, BigDecimal verifiedAmount, String reason)`.
- Produces DAO package methods taking caller-owned `EntityManager`: `WorkShift findOwnedShiftForUpdate(EntityManager em, int shiftId, int shipperId)`, `CodSettlement findByShipperAndShift(EntityManager em, int shipperId, int shiftId)`, `CodSettlement findForUpdate(EntityManager em, int settlementId)`, `BigDecimal sumExpectedForShift(EntityManager em, int shipperId, WorkShift shift)`, `List<CodSettlement> listByShipper(int shipperId)`, `List<CodSettlement> listByStatus(String status)`, `BigDecimal sumPendingAmount()`, `long countPending()`.

- [ ] **Step 1: Write failing policy unit tests**

```java
package service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CodSettlementPolicyTest {
    @Test void acceptsNonNegativeSubmission() {
        assertDoesNotThrow(() -> CodSettlementService.validateSubmission(new BigDecimal("0.00")));
    }

    @Test void rejectsMissingOrNegativeSubmission() {
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("-0.01")));
    }

    @Test void settledMustEqualSubmittedAmount() {
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("SETTLED", new BigDecimal("100000.00"), new BigDecimal("100000.00"), null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SETTLED", new BigDecimal("100000.00"), new BigDecimal("99999.00"), null));
    }

    @Test void shortAndOverRequireDirectionAndReason() {
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("SHORT", new BigDecimal("100000.00"), new BigDecimal("90000.00"), "Thiếu tiền mặt"));
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("OVER", new BigDecimal("100000.00"), new BigDecimal("110000.00"), "Nộp dư"));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SHORT", new BigDecimal("100000.00"), new BigDecimal("90000.00"), " "));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("OVER", new BigDecimal("100000.00"), new BigDecimal("90000.00"), "Sai chiều"));
    }

    @Test void rejectsUnknownOrNegativeVerification() {
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SUBMITTED", BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SETTLED", BigDecimal.ZERO, new BigDecimal("-0.01"), null));
    }
}
```

- [ ] **Step 2: Run policy test to verify RED**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.CodSettlementPolicyTest test`

Expected: FAIL compilation with `cannot find symbol: class CodSettlementService`.

- [ ] **Step 3: Add minimal validation methods**

```java
static void validateSubmission(BigDecimal submittedAmount) {
    if (submittedAmount == null || submittedAmount.signum() < 0) throw new IllegalArgumentException("Số tiền thực nộp không hợp lệ");
}

static void validateVerification(String status, BigDecimal submittedAmount, BigDecimal verifiedAmount, String reason) {
    if (!Set.of("SETTLED", "SHORT", "OVER").contains(status)) throw new IllegalArgumentException("Trạng thái xác nhận không hợp lệ");
    if (submittedAmount == null || verifiedAmount == null || verifiedAmount.signum() < 0) throw new IllegalArgumentException("Số tiền kiểm đếm không hợp lệ");
    int comparison = verifiedAmount.compareTo(submittedAmount);
    if ("SETTLED".equals(status) && comparison != 0) throw new IllegalArgumentException("Số tiền khớp phải bằng số đã nộp");
    if ("SHORT".equals(status) && (comparison >= 0 || reason == null || reason.isBlank())) throw new IllegalArgumentException("Thiếu tiền cần số kiểm đếm thấp hơn và lý do");
    if ("OVER".equals(status) && (comparison <= 0 || reason == null || reason.isBlank())) throw new IllegalArgumentException("Thừa tiền cần số kiểm đếm cao hơn và lý do");
}
```

- [ ] **Step 4: Run policy test to verify GREEN**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.CodSettlementPolicyTest test`

Expected: `Tests run: 5, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

- [ ] **Step 5: Write failing transaction/locking contract test**

```java
package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CodSettlementServiceContractTest {
    @Test void submitUsesShiftLockUniqueLookupAndRollback() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/CodSettlementService.java"));
        assertTrue(service.contains("findOwnedShiftForUpdate(em, shiftId, shipperId)"));
        assertTrue(service.contains("findByShipperAndShift(em, shipperId, shiftId)"));
        assertTrue(service.contains("throw new SettlementConflictException(\"Ca này đã gửi bàn giao COD\")"));
        assertTrue(service.contains("if (em.getTransaction().isActive()) em.getTransaction().rollback();"));
    }

    @Test void verifyLocksSettlementAndProtectsExpectedStatus() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/CodSettlementService.java"));
        assertTrue(service.contains("findForUpdate(em, settlementId)"));
        assertTrue(service.contains("!settlement.getStatus().equals(expectedStatus)"));
        assertTrue(service.contains("!\"SUBMITTED\".equals(settlement.getStatus())"));
        assertTrue(service.contains("settlement.setReceivedBy(admin);"));
    }

    @Test void daoScopesExpectedCodToDeliveredOrdersAndShiftWindow() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/CodSettlementDAO.java"));
        assertTrue(dao.contains("o.shipper.userId = :shipperId"));
        assertTrue(dao.contains("o.paymentMethod = 'COD'"));
        assertTrue(dao.contains("o.orderStatus = 'DELIVERED'"));
        assertTrue(dao.contains("o.codCollectedAmount IS NOT NULL"));
        assertTrue(dao.contains("o.deliveredAt >= :start AND o.deliveredAt < :end"));
        assertTrue(dao.contains("LockModeType.PESSIMISTIC_WRITE"));
    }
}
```

- [ ] **Step 6: Run contract test to verify RED**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.CodSettlementServiceContractTest test`

Expected: FAIL with `NoSuchFileException: src/main/java/dao/CodSettlementDAO.java`.

- [ ] **Step 7: Implement DAO queries**

Use `LocalDateTime start = LocalDateTime.of(shift.getShiftDate(), shift.getStartTime())` and matching end. Required expected sum JPQL:

```java
BigDecimal result = em.createQuery(
    "SELECT SUM(o.codCollectedAmount) FROM Orders o WHERE o.shipper.userId = :shipperId " +
    "AND o.paymentMethod = 'COD' AND o.orderStatus = 'DELIVERED' AND o.codCollectedAmount IS NOT NULL " +
    "AND o.deliveredAt >= :start AND o.deliveredAt < :end", BigDecimal.class)
    .setParameter("shipperId", shipperId)
    .setParameter("start", start)
    .setParameter("end", end)
    .getSingleResult();
return result == null ? BigDecimal.ZERO : result;
```

`findOwnedShiftForUpdate` must query `ws.shiftId`, `ws.user.userId`, `ws.user.role = 'SHIPPER'`, `ws.user.status = 'ACTIVE'`, `ws.checkInAt IS NOT NULL`, then call `.setLockMode(LockModeType.PESSIMISTIC_WRITE)` and return `null` when empty. `findForUpdate` uses `em.find(CodSettlement.class, settlementId, LockModeType.PESSIMISTIC_WRITE)`. Public read methods open/close their own `EntityManager`; mutation helpers never close caller-owned manager.

- [ ] **Step 8: Implement service transaction and DTO map**

`submit` sequence: validate amount; begin; lock owned shift; `404`-semantics exception `IllegalArgumentException("Không tìm thấy ca hợp lệ")` if null; reject duplicate with `SettlementConflictException`; compute expected; persist `SUBMITTED`; commit; return map. Catch `RuntimeException`, rollback, rethrow.

`verify` sequence: reject blank `expectedStatus`; begin; load active Admin using `em.find(User.class, adminId, LockModeType.PESSIMISTIC_READ)` and reject absent/non-Admin/inactive with `SecurityException`; lock settlement; reject absent; reject expected status mismatch or non-`SUBMITTED` with `SettlementConflictException`; validate; set status, verified amount, trimmed reason only for mismatch, Admin and verified timestamp; commit; return map.

DTO keys must be exact:

```java
Map<String, Object> toMap(CodSettlement settlement) {
    Map<String, Object> result = new HashMap<>();
    result.put("settlementId", settlement.getSettlementId());
    result.put("shipperId", settlement.getShipper().getUserId());
    result.put("shipperName", settlement.getShipper().getFullName());
    result.put("shiftId", settlement.getShift().getShiftId());
    result.put("shiftDate", settlement.getShift().getShiftDate());
    result.put("startTime", settlement.getShift().getStartTime());
    result.put("endTime", settlement.getShift().getEndTime());
    result.put("status", settlement.getStatus());
    result.put("expectedAmount", settlement.getExpectedAmount());
    result.put("submittedAmount", settlement.getSubmittedAmount());
    result.put("verifiedAmount", settlement.getVerifiedAmount());
    result.put("reason", settlement.getReason());
    result.put("receivedByName", settlement.getReceivedBy() == null ? null : settlement.getReceivedBy().getFullName());
    result.put("submittedAt", settlement.getSubmittedAt());
    result.put("verifiedAt", settlement.getVerifiedAt());
    return result;
}
```

`getShipperCurrent` returns `{ state, shift, settlement }`; `shift` contains `shiftId`, `shiftDate`, `startTime`, `endTime`, `expectedAmount`; no valid current checked-in Shipper shift returns `state: "NO_ACTIVE_SHIFT"`, null shift/settlement. Reuse current-shift query semantics but include role `SHIPPER` rather than calling `currentCheckedInShift`, currently hardcoded `STAFF`.

- [ ] **Step 9: Run service tests and backend suite to verify GREEN**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.CodSettlementPolicyTest,service.CodSettlementServiceContractTest test && mvn test`

Expected: focused `Tests run: 8, Failures: 0, Errors: 0`; full suite `BUILD SUCCESS`.

- [ ] **Step 10: Review task diff without commit**

Run: `git diff -- Backend/FastGuy-FastFoodSite/src/main/java/dao/CodSettlementDAO.java Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementServiceContractTest.java && git status --short`

Expected: DAO/service/tests only; no revenue query changed.

---

### Task 3: Authenticated Shipper/Admin API

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/CodSettlementServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/CodSettlementApiContractTest.java`

**Interfaces:**
- Consumes service signatures from Task 2.
- Produces endpoints: `GET /api/cod-settlements/current`, `GET /api/cod-settlements/mine`, `POST /api/cod-settlements`, `GET /api/cod-settlements/admin?status=SUBMITTED|SHORT|OVER|SETTLED`, `PUT /api/cod-settlements/{id}/verify`.
- Produces submit JSON `{ shiftId:number, submittedAmount:number }`; verify JSON `{ expectedStatus:"SUBMITTED", status:"SETTLED"|"SHORT"|"OVER", verifiedAmount:number, reason?:string }`.

- [ ] **Step 1: Write failing API contract test**

```java
package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CodSettlementApiContractTest {
    @Test void exposesExactRoutesAndRoleBoundaries() throws IOException {
        String source = Files.readString(Path.of("src/main/java/servlet/CodSettlementServlet.java"));
        assertTrue(source.contains("@WebServlet(\"/api/cod-settlements/*\")"));
        assertTrue(source.contains("\"/current\".equals(path)"));
        assertTrue(source.contains("\"/mine\".equals(path)"));
        assertTrue(source.contains("\"/admin\".equals(path)"));
        assertTrue(source.contains("requireRole(req, resp, \"SHIPPER\")"));
        assertTrue(source.contains("requireRole(req, resp, \"ADMIN\")"));
        assertTrue(source.contains("PrivilegedAuth.isActiveRole"));
    }

    @Test void mapsValidationAuthorizationConflictAndUnexpectedErrors() throws IOException {
        String source = Files.readString(Path.of("src/main/java/servlet/CodSettlementServlet.java"));
        assertTrue(source.contains("catch (IllegalArgumentException e)"));
        assertTrue(source.contains("catch (SecurityException e)"));
        assertTrue(source.contains("catch (SettlementConflictException e)"));
        assertTrue(source.contains("ApiResponse.error(resp, e.getMessage(), 400)"));
        assertTrue(source.contains("ApiResponse.error(resp, \"Forbidden\", 403)"));
        assertTrue(source.contains("ApiResponse.error(resp, e.getMessage(), 409)"));
        assertTrue(source.contains("ApiResponse.error(resp, \"Internal server error\", 500)"));
    }
}
```

- [ ] **Step 2: Run API test to verify RED**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=servlet.CodSettlementApiContractTest test`

Expected: FAIL with `NoSuchFileException: src/main/java/servlet/CodSettlementServlet.java`.

- [ ] **Step 3: Implement servlet dispatch and payload parsing**

Use Jackson already installed:

```java
private final ObjectMapper mapper = new ObjectMapper();
private final CodSettlementService service = new CodSettlementService();

private int requireRole(HttpServletRequest req, HttpServletResponse resp, String requiredRole) throws IOException {
    String header = req.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return -1; }
    String token = header.substring(7);
    int userId = JwtUtil.getUserId(token);
    String role = JwtUtil.getRole(token);
    if (!requiredRole.equals(role) || !PrivilegedAuth.isActiveRole(userId, role)) { ApiResponse.error(resp, "Forbidden", 403); return -1; }
    return userId;
}
```

`doGet`: Shipper `/current` and `/mine`; Admin `/admin`; reject invalid admin status with `400`; unknown path `404`. `doPost`: only exact `/`, Shipper role, require numeric `shiftId` and `submittedAmount`, call `submit`, return HTTP success envelope. `doPut`: parse `/{positive-int}/verify`, Admin role, require strings and numeric `verifiedAmount`, call `verify`. Set `application/json;charset=UTF-8` before auth.

Wrap dispatch using:

```java
try {
    // dispatch
} catch (SettlementConflictException e) {
    ApiResponse.error(resp, e.getMessage(), 409);
} catch (SecurityException e) {
    ApiResponse.error(resp, "Forbidden", 403);
} catch (IllegalArgumentException e) {
    ApiResponse.error(resp, e.getMessage(), 400);
} catch (RuntimeException e) {
    ApiResponse.error(resp, "Internal server error", 500);
}
```

Do not return exception class, SQL text, stack trace or raw request body.

- [ ] **Step 4: Run API test and backend suite to verify GREEN**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=servlet.CodSettlementApiContractTest test && mvn test`

Expected: focused `Tests run: 2, Failures: 0, Errors: 0`; full suite `BUILD SUCCESS`.

- [ ] **Step 5: Review task diff without commit**

Run: `git diff -- Backend/FastGuy-FastFoodSite/src/main/java/servlet/CodSettlementServlet.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/CodSettlementApiContractTest.java && git status --short`

Expected: servlet and API contract test only; auth checks occur before service calls.

---

### Task 4: Pending COD metric thật trên Admin Dashboard

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java:13-48`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/AdminCodMetricPolicyTest.java`
- Modify: `Frontend/src/views/admin/DashboardPage.vue:217-221`

**Interfaces:**
- Consumes: `CodSettlementDAO.sumPendingAmount():BigDecimal`, `CodSettlementDAO.countPending():long`.
- Produces Admin dashboard keys `pendingCodAmount:BigDecimal`, `pendingCodCount:long`; preserves existing revenue keys and formulas unchanged.

- [ ] **Step 1: Write failing backend metric test**

```java
package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminCodMetricPolicyTest {
    @Test void dashboardUsesSubmittedSettlementsForPendingCodWithoutChangingRevenue() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        String dao = Files.readString(Path.of("src/main/java/dao/CodSettlementDAO.java"));
        assertTrue(service.contains("data.put(\"pendingCodAmount\", codSettlementDAO.sumPendingAmount());"));
        assertTrue(service.contains("data.put(\"pendingCodCount\", codSettlementDAO.countPending());"));
        assertTrue(dao.contains("WHERE cs.status = 'SUBMITTED'"));
        assertTrue(service.contains("double totalRevenue = ordersDAO.sumRevenue();"));
        assertTrue(service.contains("data.put(\"revenueToday\", ordersDAO.sumRevenueToday());"));
    }
}
```

- [ ] **Step 2: Run metric test to verify RED**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.AdminCodMetricPolicyTest test`

Expected: FAIL assertion for missing `pendingCodAmount`.

- [ ] **Step 3: Add DAO field and dashboard keys**

In `AdminService`:

```java
private CodSettlementDAO codSettlementDAO = new CodSettlementDAO();
```

After `pendingOrders`:

```java
data.put("pendingCodAmount", codSettlementDAO.sumPendingAmount());
data.put("pendingCodCount", codSettlementDAO.countPending());
```

DAO queries aggregate only `SUBMITTED`; return `BigDecimal.ZERO` for null sum. Do not modify `OrdersDAO`, `totalRevenue`, `revenueToday`, report calculations or payment status.

- [ ] **Step 4: Update dashboard UI metric**

Replace one operation strip item with exact copy and destination:

```vue
<div>
  <span class="signal warning"><i class="bi bi-cash-stack"></i></span>
  <p>COD chờ xác nhận<strong>{{ formatPrice(data.pendingCodAmount || 0) }}</strong></p>
  <router-link to="/admin/cod-settlements">{{ data.pendingCodCount || 0 }} bàn giao</router-link>
</div>
```

Keep revenue cards unchanged so UI explicitly separates revenue and money awaiting COD verification.

- [ ] **Step 5: Run backend metric test and frontend build to verify GREEN**

Run: `cd Backend/FastGuy-FastFoodSite && mvn -Dtest=service.AdminCodMetricPolicyTest test`

Expected: `Tests run: 1, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

Run: `cd Frontend && npm run build`

Expected: Vite exits `0` and prints `built in` with no unresolved route failure (route arrives in Task 6; Vue template compilation itself passes).

- [ ] **Step 6: Review task diff without commit**

Run: `git diff -- Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java Backend/FastGuy-FastFoodSite/src/main/java/dao/CodSettlementDAO.java Backend/FastGuy-FastFoodSite/src/test/java/service/AdminCodMetricPolicyTest.java Frontend/src/views/admin/DashboardPage.vue && git status --short`

Expected: pending settlement aggregation and UI metric only; all revenue lines unchanged.

---

### Task 5: Frontend API và Shipper handover UI

**Files:**
- Create: `Frontend/src/api/codSettlement.js`
- Modify: `Frontend/src/api/index.js:9-13`
- Modify: `Frontend/src/views/shipper/CashPage.vue:1-97`
- Test: `Frontend/test/cod-settlement-api.test.js`
- Test: `Frontend/test/cod-settlement-ui.test.js`

**Interfaces:**
- Consumes endpoints from Task 3 and normalized errors `{ status:number, message:string }` from `src/api/error.js`.
- Produces `codSettlementApi.getCurrent()`, `.getMine()`, `.submit({ shiftId, submittedAmount })`, `.getAdmin(status)`, `.verify(id, payload)`.
- Produces Shipper state fields matching backend DTO exactly: `state`, `shift.shiftId`, `shift.expectedAmount`, `settlement.status`, `settlement.submittedAmount`, `settlement.verifiedAmount`, `settlement.reason`.

- [ ] **Step 1: Write failing frontend API test**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('COD settlement API exposes shipper and admin operations', () => {
  const api = read('../src/api/codSettlement.js');
  const index = read('../src/api/index.js');
  assert.match(api, /client\.get\('\/cod-settlements\/current'\)/);
  assert.match(api, /client\.get\('\/cod-settlements\/mine'\)/);
  assert.match(api, /client\.post\('\/cod-settlements', data\)/);
  assert.match(api, /client\.get\('\/cod-settlements\/admin', \{ params: \{ status \} \}\)/);
  assert.match(api, /client\.put\(`\/cod-settlements\/\$\{id\}\/verify`, data\)/);
  assert.match(index, /codSettlementApi/);
});
```

- [ ] **Step 2: Run API test to verify RED**

Run: `cd Frontend && npm test -- --test-name-pattern="COD settlement API"`

Expected: FAIL with `ENOENT` for `src/api/codSettlement.js`.

- [ ] **Step 3: Add minimal API module**

```js
import client from './client';

export default {
  getCurrent() { return client.get('/cod-settlements/current'); },
  getMine() { return client.get('/cod-settlements/mine'); },
  submit(data) { return client.post('/cod-settlements', data); },
  getAdmin(status) { return client.get('/cod-settlements/admin', { params: { status } }); },
  verify(id, data) { return client.put(`/cod-settlements/${id}/verify`, data); },
};
```

Add `export { default as codSettlementApi } from './codSettlement';` to `src/api/index.js`.

- [ ] **Step 4: Run API test to verify GREEN**

Run: `cd Frontend && npm test -- --test-name-pattern="COD settlement API"`

Expected: `pass 1`, `fail 0`.

- [ ] **Step 5: Write failing Shipper UI source test**

Append to `Frontend/test/cod-settlement-ui.test.js`:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('shipper COD page separates expected submitted and verified money', () => {
  const page = read('../src/views/shipper/CashPage.vue');
  assert.match(page, /Tiền dự kiến theo ca/);
  assert.match(page, /Số tiền thực nộp/);
  assert.match(page, /Kết quả Admin xác nhận/);
  assert.doesNotMatch(page, /tạm tính bằng tổng thu hôm nay/);
  assert.match(page, /codSettlementApi\.submit/);
  assert.match(page, /error\?\.status === 409/);
});

test('shipper COD form exposes labels errors announcements and 44px controls', () => {
  const page = read('../src/views/shipper/CashPage.vue');
  assert.match(page, /for="submitted-amount"/);
  assert.match(page, /aria-describedby="submitted-error"/);
  assert.match(page, /id="submitted-error"[^>]*role="alert"/);
  assert.match(page, /role="status"/);
  assert.match(page, /min-height:\s*44px/);
  assert.match(page, /@media\(prefers-reduced-motion:reduce\)/);
});
```

- [ ] **Step 6: Run Shipper UI test to verify RED**

Run: `cd Frontend && npm test -- --test-name-pattern="shipper COD"`

Expected: FAIL because old page contains `tạm tính bằng tổng thu hôm nay` and lacks submit API/form.

- [ ] **Step 7: Replace Shipper page with current-shift settlement flow**

Use refs `loading`, `submitting`, `error`, `formError`, `successMessage`, `current`, `history`, `submittedAmount`; load `getCurrent` and `getMine` in `Promise.all`; generation guard same as existing page. Compute:

```js
const shift = computed(() => current.value?.shift || null);
const settlement = computed(() => current.value?.settlement || null);
const canSubmit = computed(() => current.value?.state === 'READY_TO_SUBMIT' && !settlement.value && !submitting.value);
const resultLabel = computed(() => ({ SUBMITTED: 'Đang chờ Admin kiểm đếm', SETTLED: 'Đã khớp', SHORT: 'Thiếu tiền', OVER: 'Thừa tiền' }[settlement.value?.status] || 'Chưa gửi bàn giao'));
```

Submit validation and conflict:

```js
async function submitSettlement() {
  formError.value = '';
  successMessage.value = '';
  const amount = Number(submittedAmount.value);
  if (submittedAmount.value === '' || !Number.isFinite(amount) || amount < 0) {
    formError.value = 'Nhập số tiền thực nộp từ 0 trở lên.';
    return;
  }
  submitting.value = true;
  try {
    await codSettlementApi.submit({ shiftId: shift.value.shiftId, submittedAmount: amount });
    successMessage.value = 'Đã gửi bàn giao COD. Số tiền không thể sửa sau khi gửi.';
    await load();
  } catch (error) {
    formError.value = error?.status === 409 ? 'Ca này đã được gửi bàn giao. Đang tải trạng thái mới nhất.' : error.message || 'Không thể gửi bàn giao COD.';
    if (error?.status === 409) await load();
  } finally {
    submitting.value = false;
  }
}
```

Template requirements: loading `role="status"`; top-level API error `role="alert"` plus retry; `NO_ACTIVE_SHIFT` empty card; three labeled cards “Tiền dự kiến theo ca”, “Đã gửi bàn giao”, “Kết quả Admin xác nhận”; form only when `canSubmit`; input `id="submitted-amount"`, `type="number"`, `min="0"`, `step="1000"`, label, linked error; submit button min-height 44px; immutable warning before submit; history cards include shift date/time, all three amounts, status text, reason and receiver/timestamps. Do not fetch order history or derive settlement client-side.

- [ ] **Step 8: Run frontend tests and build to verify GREEN**

Run: `cd Frontend && npm test && npm run build`

Expected: Node summary `fail 0`; Vite exits `0` and prints `built in`.

- [ ] **Step 9: Review task diff without commit**

Run: `git diff -- Frontend/src/api/codSettlement.js Frontend/src/api/index.js Frontend/src/views/shipper/CashPage.vue Frontend/test/cod-settlement-api.test.js Frontend/test/cod-settlement-ui.test.js && git status --short`

Expected: old today/history derivation removed; API is settlement source of truth; no dependency changes.

---

### Task 6: Admin queue, verification dialog và navigation

**Files:**
- Create: `Frontend/src/views/admin/CodSettlementsPage.vue`
- Modify: `Frontend/src/router/index.js:398-503,517-530`
- Modify: `Frontend/src/layouts/AdminLayout.vue:24-38`
- Modify: `Frontend/test/cod-settlement-api.test.js`
- Modify: `Frontend/test/cod-settlement-ui.test.js`

**Interfaces:**
- Consumes `codSettlementApi.getAdmin(status)` and `.verify(id, { expectedStatus, status, verifiedAmount, reason })`.
- Produces route `{ path:'cod-settlements', name:'AdminCodSettlements' }`, URL `/admin/cod-settlements`, title `Đối soát COD`.
- Produces filter values `SUBMITTED`, `SHORT`, `OVER`, `SETTLED`; default `SUBMITTED`.

- [ ] **Step 1: Extend failing API/route contract test**

Append inside frontend test files:

```js
test('admin COD route and navigation use dedicated settlement page', () => {
  const router = read('../src/router/index.js');
  const layout = read('../src/layouts/AdminLayout.vue');
  assert.match(router, /path: 'cod-settlements'/);
  assert.match(router, /name: 'AdminCodSettlements'/);
  assert.match(router, /CodSettlementsPage\.vue/);
  assert.match(router, /AdminCodSettlements: 'Đối soát COD'/);
  assert.match(layout, /\/admin\/cod-settlements/);
});

test('admin COD page has queue states accessible verify dialog and conflict reload', () => {
  const page = read('../src/views/admin/CodSettlementsPage.vue');
  assert.match(page, /codSettlementApi\.getAdmin/);
  assert.match(page, /codSettlementApi\.verify/);
  assert.match(page, /expectedStatus:\s*selected\.value\.status/);
  assert.match(page, /error\?\.status === 409/);
  assert.match(page, /role="dialog"/);
  assert.match(page, /aria-modal="true"/);
  assert.match(page, /aria-labelledby="verify-title"/);
  assert.match(page, /@keydown\.esc/);
  assert.match(page, /focusable/);
  assert.match(page, /window\.matchMedia\('\(max-width: 760px\)'\)/);
});
```

- [ ] **Step 2: Run Admin UI tests to verify RED**

Run: `cd Frontend && npm test -- --test-name-pattern="admin COD"`

Expected: FAIL with missing route assertions and `ENOENT` for `CodSettlementsPage.vue`.

- [ ] **Step 3: Add route, title and navigation**

Insert Admin child:

```js
{
  path: 'cod-settlements',
  name: 'AdminCodSettlements',
  component: () => import('@/views/admin/CodSettlementsPage.vue'),
  meta: { breadcrumb: [{ label: 'Tổng quan', to: '/admin' }, { label: 'Đối soát COD' }] },
},
```

Add `AdminCodSettlements: 'Đối soát COD'` to `pageTitles`. Add sidebar entry after Đơn hàng:

```js
{ label: 'Đối soát COD', path: '/admin/cod-settlements', icon: 'bi-cash-stack' },
```

- [ ] **Step 4: Implement Admin queue and responsive presentation**

State:

```js
const filter = ref('SUBMITTED');
const rows = ref([]);
const loading = ref(true);
const error = ref('');
const selected = ref(null);
const verifiedAmount = ref('');
const outcome = ref('SETTLED');
const reason = ref('');
const formError = ref('');
const saving = ref(false);
const successMessage = ref('');
const dialogRef = ref(null);
const triggerRef = ref(null);
const mobile = ref(window.matchMedia('(max-width: 760px)').matches);
```

`load` calls `codSettlementApi.getAdmin(filter.value)`, catches actionable error, and supplies retry. `openVerify(row, event)` stores trigger, initializes verified amount to submitted amount, opens dialog, waits `nextTick`, focuses first form control. `closeDialog()` refuses while saving, closes and returns focus.

Focus trap exact algorithm:

```js
function focusable() {
  return [...dialogRef.value.querySelectorAll('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
}
function trapFocus(event) {
  if (event.key !== 'Tab') return;
  const controls = focusable();
  const first = controls[0];
  const last = controls[controls.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}
```

Verify payload and conflict:

```js
await codSettlementApi.verify(selected.value.settlementId, {
  expectedStatus: selected.value.status,
  status: outcome.value,
  verifiedAmount: Number(verifiedAmount.value),
  reason: outcome.value === 'SETTLED' ? null : reason.value.trim(),
});
```

Client validation mirrors backend amount direction/reason rules. On `409`, set `formError` to “Bàn giao đã được xử lý ở nơi khác. Danh sách mới nhất đã được tải.”, close dialog only after preserving message at page level, then `await load()`. Other errors remain in dialog.

Template: heading, filter tabs/select, loading, error+retry, empty, success announcement; desktop table columns Shipper/Ca/Dự kiến/Đã nộp/Trạng thái/Gửi lúc/Thao tác; mobile cards selected by `mobile`; `SHORT`/`OVER` history displays verified amount, reason, receiver, verified time. Verify dialog exists only for `SUBMITTED`, has `role="dialog" aria-modal="true" aria-labelledby="verify-title"`, `@keydown.esc="closeDialog"`, `@keydown="trapFocus"`; labeled outcome/amount/reason fields, linked errors and 44px controls. Modal backdrop does not close during save. CSS includes reduced-motion rule.

- [ ] **Step 5: Run Admin UI tests to verify GREEN**

Run: `cd Frontend && npm test -- --test-name-pattern="admin COD"`

Expected: both Admin COD tests pass, `fail 0`.

- [ ] **Step 6: Run full frontend verification**

Run: `cd Frontend && npm test && npm run build`

Expected: all Node tests `fail 0`; Vite exits `0`, no unresolved import or template compile error.

- [ ] **Step 7: Review task diff without commit**

Run: `git diff -- Frontend/src/views/admin/CodSettlementsPage.vue Frontend/src/router/index.js Frontend/src/layouts/AdminLayout.vue Frontend/test/cod-settlement-api.test.js Frontend/test/cod-settlement-ui.test.js && git status --short`

Expected: dedicated Admin COD route/page/navigation only; no browser `confirm()` and no unrelated layout refactor.

---

### Task 7: End-to-end contract verification và final review

**Files:**
- Modify only if a check exposes a defect: files listed in Tasks 1-6.
- Test: all backend and frontend tests created above.

**Interfaces:**
- Consumes all Task 1-6 contracts.
- Produces verified vertical slice: migration, backend, API, Shipper UI, Admin UI, real pending metric; no commit.

- [ ] **Step 1: Run migration source invariant checks**

Run from repository root:

```powershell
$files = @('database/migrations/046_cod_shift_settlement.sql','database/init.sql','database/DB_FastGuy.sql'); $required = @('CREATE TABLE dbo.CodSettlement','UQ_CodSettlement_ShipperShift','CK_CodSettlement_Status','CK_CodSettlement_Amounts','CK_CodSettlement_Verification','IX_CodSettlement_StatusSubmittedAt','IX_CodSettlement_ShipperSubmittedAt'); foreach ($file in $files) { $sql = Get-Content -Raw $file; foreach ($token in $required) { if ($sql -notmatch [regex]::Escape($token)) { throw "$file missing $token" } }; if ($sql -notmatch "status IN \('SUBMITTED','SETTLED','SHORT','OVER'\)") { throw "$file status set differs" } }; foreach ($file in @('database/init.sql','database/DB_FastGuy.sql')) { $sql = Get-Content -Raw $file; if ([regex]::Matches($sql, 'CREATE TABLE dbo\.CodSettlement').Count -ne 1 -or $sql -match 'INSERT\s+(dbo\.)?CodSettlement') { throw "$file fresh-install COD schema invalid" } }; $validator = Get-Content -Raw 'database/migrations/046_cod_shift_settlement_validate.sql'; if ($validator -notmatch 'Duplicate shipper/shift settlement') { throw 'COD validator invariant missing' }; 'COD migration and fresh-install schema checks passed.'
```

Expected: `COD migration and fresh-install schema checks passed.`

- [ ] **Step 2: Run complete backend test/build gate**

Run: `cd Backend/FastGuy-FastFoodSite && mvn clean test && mvn -DskipTests package`

Expected: both commands end `BUILD SUCCESS`; zero test failures/errors; WAR produced at `Backend/FastGuy-FastFoodSite/target/FastGuy.war`.

- [ ] **Step 3: Run complete frontend test/build gate**

Run: `cd Frontend && npm test && npm run build`

Expected: Node summary `fail 0`; Vite build exits `0`; output generated under `Frontend/dist`.

- [ ] **Step 4: Scan forbidden placeholders and fake COD copy**

Run from repository root:

```powershell
$paths = @("database/migrations/046_cod_shift_settlement.sql","database/migrations/046_cod_shift_settlement_validate.sql","Backend/FastGuy-FastFoodSite/src/main/java/entity/CodSettlement.java","Backend/FastGuy-FastFoodSite/src/main/java/dao/CodSettlementDAO.java","Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java","Backend/FastGuy-FastFoodSite/src/main/java/servlet/CodSettlementServlet.java","Frontend/src/api/codSettlement.js","Frontend/src/views/shipper/CashPage.vue","Frontend/src/views/admin/CodSettlementsPage.vue"); $hits = Select-String -Path $paths -Pattern 'TBD|TODO|implement later|tạm tính bằng tổng thu hôm nay|window\.confirm|confirm\('; if ($hits) { $hits; throw 'Forbidden placeholder or fake COD copy found' }; 'Placeholder and COD copy scan passed.'
```

Expected: `Placeholder and COD copy scan passed.`

- [ ] **Step 5: Check type/property consistency across boundaries**

Run from repository root:

```powershell
$service = Get-Content -Raw "Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java"; $shipper = Get-Content -Raw "Frontend/src/views/shipper/CashPage.vue"; $admin = Get-Content -Raw "Frontend/src/views/admin/CodSettlementsPage.vue"; @('settlementId','shiftId','status','expectedAmount','submittedAmount','verifiedAmount','reason','submittedAt','verifiedAt') | ForEach-Object { if ($service -notmatch $_ -or ($shipper -notmatch $_ -and $admin -notmatch $_)) { throw "Boundary property mismatch: $_" } }; 'Boundary property checks passed.'
```

Expected: `Boundary property checks passed.`

- [ ] **Step 6: Review complete diff and status without commit**

Run: `git diff --check && git diff --stat && git diff -- database/migrations/046_cod_shift_settlement.sql database/migrations/046_cod_shift_settlement_validate.sql database/init.sql database/DB_FastGuy.sql Backend/FastGuy-FastFoodSite/src/main Frontend/src Frontend/test Backend/FastGuy-FastFoodSite/src/test && git status --short`

Expected: `git diff --check` prints nothing and exits `0`; diff contains only scoped COD settlement files plus Admin pending metric; no `package.json`, `pom.xml`, lockfile, delivery/refund/inventory/customer change; status remains uncommitted.

- [ ] **Step 7: Manual responsive/accessibility smoke check**

Run: `cd Frontend && npm run dev -- --host 127.0.0.1`

Expected: Vite prints local URL and serves app. At widths `360`, `768`, `1280`: `/shipper/cash` has 44px controls and no horizontal overflow; `/admin/cod-settlements` switches cards/table; keyboard opens dialog, Tab stays trapped, Escape closes, focus returns to trigger; loading/error/empty/conflict/success text remains readable; status text accompanies color. Stop server with `Ctrl+C` after verification.

---

## Acceptance Trace

- Settlement per Shipper/ca, immutable submit, duplicate prevention: Tasks 1-3.
- Expected/submitted/verified amounts, actors, reason, timestamps: Tasks 1-3.
- `SETTLED`/`SHORT`/`OVER` validation and terminal protection: Tasks 1-3.
- Revenue independent and pending COD metric thật: Task 4.
- Shipper mobile-first handover, separated money concepts: Task 5.
- Admin pending queue, mismatch history, accessible dialog: Task 6.
- `400`, `401`, `403`, `404`, `409`, `500`, rollback and no internal leakage: Tasks 2-3.
- Loading, empty, error, retry, conflict, success, responsive and accessibility: Tasks 5-7.
- No dependency and no commit: Global Constraints and Task 7 review.
