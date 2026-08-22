# Đơn giản hóa phạm vi FastGuy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xóa vĩnh viễn Review, SupportTicket, ProductCombo, ProductComboItem và phí dịch vụ; giữ nguyên WorkShift, ProductVariant, topping; hoàn thiện chi tiết refund và đối soát COD có contract, kiểm chứng dữ liệu, UI desktop/mobile.

**Architecture:** Triển khai theo hard gate `DATABASE → OpenAPI/API → Backend → Frontend → verification`. Migration `051` dùng catalog để chứng minh semantic inventory parity cần thiết; nếu branch có migration `050` thì phải xếp trước `051`, nhưng runtime không bị khóa cứng theo migration ID khi constraint canonical đã được chứng minh tương đương. Mọi sai lệch catalog, attribution tiền/COD/loyalty hoặc recovery evidence đều dừng thay vì suy đoán.

**Tech Stack:** SQL Server 2016+, T-SQL/sqlcmd, Java 17 WAR, Jakarta Servlet 6.1, JPA 3.1, Hibernate 6.6, Maven/JUnit 5, OpenAPI 3.1/Redocly, Vue 3 Composition API, Pinia, Axios, Vite, Node test runner, Playwright.

## Global Constraints

- Chỉ thay đổi đúng phạm vi đặc tả `docs/superpowers/specs/2026-08-21-fastguy-scope-simplification-design.md`; không thêm dependency, compatibility adapter, feature flag, archive table hoặc legacy viewer.
- Thực hiện đúng thứ tự `DATABASE → OpenAPI/API → Backend → Frontend → verification`; phase sau không bắt đầu khi gate trước chưa GREEN.
- Runtime SQL Server catalog/data là nguồn chuẩn cho schema và attribution; `openapi/fastguy.yaml` là nguồn chuẩn cho endpoint đã contract hóa; source hiện hành quyết định URL legacy chưa có OpenAPI.
- Read-only inventory dùng principal chỉ có `SELECT` và `VIEW DEFINITION`; tài khoản `sysadmin` không được chấp nhận làm bằng chứng read-only.
- Không sửa migration đã triển khai. Main hiện kết thúc ở `049`; worktree khác có `050` inventory parity chưa merge. Nếu `050` được merge trước thì simplification dùng `051_remove_review_support_combo_service_fee.sql` và `051_validate.sql`; nếu số migration thay đổi do thứ tự merge, phải chọn lại số chưa dùng trước khi tạo file.
- Migration simplification không khóa cứng theo history ID `050`. Gate bắt buộc là catalog chứng minh `InventoryReservation.status` chấp nhận đúng `RESERVED/CONSUMED/RELEASED/WASTED`, constraint trusted/enabled và không còn blocker legacy. Nếu semantic parity chưa đạt, dừng và hoàn tất migration parity riêng trước.
- Migration phá hủy chỉ chạy trên database disposable đã restore từ backup đại diện sau xác nhận riêng; implementation mặc định dừng trước retained deployment.
- Retained deployment cần phê duyệt riêng ghi rõ server/database/migration, backup verified, restore rehearsal, writes stopped, recovery owner/window/go-no-go. Không suy diễn phê duyệt từ việc duyệt plan/spec.
- Không dùng `database/init.sql` trên retained data. Không dùng down-script để rollback phá hủy; sau commit chỉ recovery bằng verified backup và application version trước.
- Xóa dữ liệu vĩnh viễn; dùng `#temp`/staging trong transaction và audit result set/log checksum, không tạo bảng archive/backup dài hạn trong database.
- Migration dùng `SET XACT_ABORT ON`, `TRY/CATCH` + `THROW`, deterministic lock/order, migration-history guard, transaction ngắn nhất có thể; lỗi invariant rollback toàn bộ.
- PayOS lịch sử sau rewrite chỉ là parity demo cục bộ; không tuyên bố link, webhook, giao dịch hoặc sổ cái provider ngoài hệ thống còn khớp.
- Công thức cuối duy nhất: `finalAmount = totalAmount + shippingFee - discountAmount`; checkout chỉ hiển thị/tính subtotal, shipping, discount.
- Giữ nguyên schema, lifecycle và hành vi WorkShift. Giữ nguyên ProductVariant schema/JSON/quan hệ; chỉ đổi nhãn UI “Biến thể” thành “Kích cỡ”.
- Giữ SKU, giá, tồn kho, kích thước GHN trên ProductVariant. Giữ ProductModifierGroup/ProductModifierOption dưới nhãn topping/tùy chọn.
- Xóa test/fixture chỉ khi chúng chuyên biệt cho capability bị xóa; thêm test absence/contract thay thế. Không xóa blanket test hoặc test bảo vệ capability còn giữ.
- Backend tiếp tục kiểm tra actor, role active, ownership, state transition và transaction rollback; frontend không quyết định quyền hoặc trạng thái authoritative.
- UI giữ loading, empty/not-found, error/retry, stale/conflict, mutation lock, focus/keyboard, target tối thiểu 24×24 CSS px và responsive mobile không bắt buộc cuộn ngang.
- Không tuyên bố production, retained deployment hoặc external payment parity.
- Không commit, push, stage hoặc ghi đè thay đổi không liên quan. Mỗi task kết thúc bằng `git diff --check`, diff giới hạn task và `git status --short` để reviewer duyệt độc lập.

---

## File Map

### Database và policy

- Create: `database/migrations/051_remove_review_support_combo_service_fee.sql` — preflight strict, snapshot `#temp`, rewrite tiền/COD/loyalty, purge notification legacy, drop domain/service fee, invariant checks, migration history.
- Create: `database/migrations/051_validate.sql` — validator read-only rerunnable cho catalog, dữ liệu, tiền, COD, loyalty, retained schemas.
- Modify: `database/init.sql` — canonical fresh schema không còn bốn bảng, service fee config/cột/constraint.
- Modify: `database/DB_FastGuy.sql` — canonical schema/seed tương đương `init.sql`.
- Modify: `database/seed_demo.sql` — demo data không insert domain/config/cột đã xóa.
- Modify: `database/migrations/RUNBOOK.md` — đặt migration parity trước simplification khi cả hai cùng tồn tại; ghi semantic parity gate cho runtime đã tương đương, disposable/retained gates, backup/restore recovery và cảnh báo PayOS local parity; không sửa `041_local_demo_seed.sql` hoặc migration deployed khác.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/integration/ScopeSimplificationMigrationIT.java` — runtime acceptance trên disposable target.
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/ScopeSimplificationMigrationPolicyTest.java` — source-policy test cho migration/canonical SQL/runbook.
- Delete after replacement coverage: `Backend/FastGuy-FastFoodSite/src/test/java/service/HomepageMerchandisingMigrationPolicyTest.java` — chỉ phần policy review/combo cũ; nếu file còn kiểm tra variant/modifier thì sửa giữ phần đó thay vì xóa.

### OpenAPI và contract tests

- Modify: `openapi/fastguy.yaml` — bỏ review/combo/service fee/homepage fields; định nghĩa refund detail/completion và COD settlement.
- Modify: `Frontend/test/openapi-contract.test.js` — schema absence, required/nullability/enum/semantics mới.
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/HomepageAdminContractTest.java` — homepage/admin product output không còn field đã xóa.
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminOrderServletBehaviorTest.java` — order/refund serialization và field absence.
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/CodSettlementApiContractTest.java` — COD request/response/status/error contract.
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/CodSettlementServiceContractTest.java` — provider map khớp OpenAPI.

### Backend removal và money model

- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Review.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/dao/ReviewDAO.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/service/ReviewService.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ReviewServlet.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/entity/SupportTicket.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/dao/SupportTicketDAO.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/service/SupportTicketService.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/SupportTicketServlet.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffSupportTicketServlet.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/entity/ProductCombo.java`.
- Delete: `Backend/FastGuy-FastFoodSite/src/main/java/entity/ProductComboItem.java`.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/HomepageService.java` — chỉ best sellers, ProductVariant và modifiers.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/ProductModifierDAO.java` — bỏ combo query, giữ modifier groups/options.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ProductServlet.java` — bỏ combo map, giữ variant/modifier JSON.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminProductServlet.java` — bỏ combo routes/maps, giữ product/variant/modifier CRUD.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java` — bỏ JPA field/getter/setter `serviceFee`.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java` — bỏ config/tính service fee; áp công thức cuối.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StoreConfigService.java` — bỏ key service fee.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StoreConfigServlet.java` — bỏ service fee serialization/update.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminOrderServlet.java` — bỏ review/service fee; bổ sung refund detail contract.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/RefundService.java` — bắt buộc reference khi hoàn tất, giữ snapshot/contact semantics.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminRefundServlet.java` — parse/validate/serialize refund contract.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/CodSettlementService.java` — server tính difference/status; map verifier/timestamps.
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/CodSettlementServlet.java` — COD contract/error mapping.
- Modify: report serializers/services được CodeGraph inventory xác nhận đang đọc service fee; không chạm report ngoài reference thực tế.
- Delete/update focused tests: `ReviewServletConsentTest.java`, `ReviewFeaturedMutationTest.java`, `SupportTicketOwnershipPolicyTest.java`, `HomepageServiceTest.java`, `HomepageServiceIT.java`, `HomepageMerchandisingMappingTest.java`; giữ và sửa assertions variant/modifier còn giá trị.

### Frontend removal và enhancement

- Delete: `Frontend/src/api/review.js`, `Frontend/src/api/support.js`.
- Modify: `Frontend/src/api/index.js`, `Frontend/src/api/admin.js`, `Frontend/src/router/index.js` — bỏ exports/calls/routes/nav review/support/combo.
- Delete: `Frontend/src/views/user/SupportPage.vue`, `Frontend/src/views/staff/SupportPage.vue`.
- Delete: `Frontend/src/components/guest/HomepageProof.vue`, `Frontend/src/components/guest/HomepageOccasions.vue`.
- Delete: `Frontend/src/components/admin/product-editor/ProductComboSection.vue`.
- Modify: `Frontend/src/views/guest/HomePage.vue`, `Frontend/src/stores/homepage.js`, `Frontend/src/utils/homepage.js` — bỏ review/occasion combo consumers.
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue` và product-editor components — bỏ combo editor, relabel kích cỡ, giữ variant/modifier.
- Modify: `Frontend/src/views/user/CheckoutPage.vue`, order detail/success/admin settings/reports/refunds pages — bỏ service fee và dùng contract mới.
- Modify: `Frontend/src/api/codSettlement.js`, `Frontend/src/views/admin/CodSettlementsPage.vue`, `Frontend/src/views/shipper/cod-settlement-state.js`, `Frontend/src/views/admin/refund-state.js` — contract COD/refund.
- Modify/add focused Node tests dưới `Frontend/test/`, policy tests dưới `Frontend/tests/`, E2E tại `Frontend/tests/e2e/home.spec.js` và các spec flow admin/checkout cần thiết.

## Interfaces xuyên task

```text
Money invariant:
  finalAmount = totalAmount + shippingFee - discountAmount
  mọi money/points >= 0

Migration handoff:
  #OrderFeeRewrite(order_id, old_service_fee, old_final_amount, new_final_amount, ...)
  #ShiftFeeDelta(shift_id, shipper_id, shift_fee_delta)
  #LoyaltyReconcile(order_id, old_earn, new_earn, reversal_total, ...)
  Các bảng chỉ tồn tại trong session/transaction; validator nhận trạng thái persisted, không phụ thuộc #temp.

COD response:
  settlementId: integer
  shipperId: integer
  shipperName: string
  shiftId: integer
  shiftDate: string(date)
  startTime/endTime: string(time)
  expectedAmount: number
  submittedAmount: number|null
  verifiedAmount: number|null
  difference: number|null = verifiedAmount - submittedAmount
  status: SUBMITTED|SETTLED|SHORT|OVER
  reason: string|null
  submittedAt/verifiedAt: string(date-time)|null
  verifiedBy: object|null theo representation chốt trong OpenAPI

Refund detail:
  customerName, customerPhone, orderCode, paymentMethod,
  finalAmount, refundAmount, refundStatus, refundReference,
  refundNote, processedBy, refundedAt

Refund completion request:
  refundReference: string, required, trim, non-empty
  refundNote/status theo operation hiện có được inventory xác nhận
```

# PHASE A — Database phá hủy (hard gate)

### Task 1: Runtime read-only catalog, data và invariant inventory trên exact target

**Files:**
- Read: `database/init.sql`
- Read: `database/DB_FastGuy.sql`
- Read: `database/migrations/046_cod_shift_settlement.sql`
- Read: `database/migrations/049_category_images.sql`
- Read: `database/migrations/RUNBOOK.md`
- Read: JPA entities/DAOs/services được nêu trong File Map
- No repository write: evidence lưu trong secure execution log, không chứa credential

**Interfaces:**
- Consumes: `FASTGUY_DB_SERVER`, exact database name, principal chỉ có `SELECT` + `VIEW DEFINITION`, branch target đã xác định.
- Produces: signed inventory gồm target identity, semantic inventory parity, object/column/constraint/index/FK/default, row counts, supported attribution model cho order/payment/COD/loyalty/notification; trạng thái `PASS` hoặc `STOP`.

- [ ] **Step 1: Chạy source baseline và migration sequence check**

```powershell
Get-ChildItem -LiteralPath .\database\migrations -File | Sort-Object Name | Select-Object -ExpandProperty Name
git log --oneline --all -- database/migrations
git status --short
```

Expected RED/STOP khi branch chưa có migration parity cần thiết và catalog runtime vẫn còn constraint legacy chặn `WASTED`; không được tiếp tục sang migration simplification. Nếu catalog đã chứng minh semantic parity tương đương, ghi bằng chứng thay vì yêu cầu history ID cụ thể.

- [ ] **Step 2: Xác minh principal và exact target bằng read-only wrapper**

```powershell
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 `
  -Mode Preflight `
  -Server $env:FASTGUY_DB_SERVER `
  -Database $env:FASTGUY_DB_NAME
```

Expected GREEN: log hiện đúng `@@SERVERNAME`, `DB_NAME()`, `ONLINE`, compatibility level; principal có `SELECT`/`VIEW DEFINITION`, `IS_SRVROLEMEMBER('sysadmin') = 0`. Sai một giá trị: STOP.

- [ ] **Step 3: Inventory catalog và migration history read-only**

```sql
SELECT @@SERVERNAME AS server_name, DB_NAME() AS database_name,
       DATABASEPROPERTYEX(DB_NAME(), 'Status') AS database_state,
       compatibility_level
FROM sys.databases WHERE name = DB_NAME();
SELECT IS_SRVROLEMEMBER('sysadmin') AS is_sysadmin,
       HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'VIEW DEFINITION') AS can_view_definition;
SELECT s.name AS schema_name, o.name, o.type_desc
FROM sys.objects o JOIN sys.schemas s ON s.schema_id = o.schema_id
WHERE o.name IN ('Review','SupportTicket','ProductCombo','ProductComboItem','Orders','ShippingConfig','PaymentAttempt','CodSettlement','WorkShift','ProductVariant','ProductModifierGroup','ProductModifierOption')
ORDER BY s.name, o.name;
SELECT t.name AS table_name, c.name AS column_name, ty.name AS type_name,
       c.max_length, c.precision, c.scale, c.is_nullable, dc.definition AS default_definition
FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id
JOIN sys.types ty ON ty.user_type_id=c.user_type_id
LEFT JOIN sys.default_constraints dc ON dc.object_id=c.default_object_id
WHERE t.name IN ('Orders','ShippingConfig','PaymentAttempt','CodSettlement','WorkShift','ProductVariant')
ORDER BY t.name, c.column_id;
SELECT OBJECT_SCHEMA_NAME(parent_object_id) AS schema_name,
       OBJECT_NAME(parent_object_id) AS table_name, name, type_desc
FROM sys.objects
WHERE parent_object_id IN (OBJECT_ID('Orders'), OBJECT_ID('ShippingConfig'), OBJECT_ID('PaymentAttempt'), OBJECT_ID('CodSettlement'))
ORDER BY table_name, name;
```

Expected GREEN: output đủ type/null/default/check/index/FK để đối chiếu canonical/JPA; catalog chứng minh constraint inventory canonical trusted/enabled chấp nhận đúng bốn trạng thái và không còn blocker legacy. Object lạ, semantic parity thiếu, principal sysadmin hoặc permission mismatch: STOP.

- [ ] **Step 4: Inventory data, totals và attribution không ghi dữ liệu**

Chạy các `SELECT` được dựng từ chính tên cột/FK vừa catalog để ghi: row count bốn domain; service fee null/min/max/sum; tổng `final_amount`, `cod_collected_amount`, `refund_amount`, `PaymentAttempt.amount`; COD delivered theo shipper/shift window; settlement expected/submitted/verified/status/reason; loyalty transaction type/order link/sign/sum và `Users.loyalty_points`; notification discriminator/deep link dùng `SUPPORT_TICKET`.

```sql
SELECT COUNT_BIG(*) AS order_count,
       SUM(COALESCE(service_fee,0)) AS service_fee_sum,
       SUM(final_amount) AS final_amount_sum
FROM Orders;
SELECT status, COUNT_BIG(*) AS row_count,
       SUM(expected_amount) AS expected_sum,
       SUM(submitted_amount) AS submitted_sum,
       SUM(verified_amount) AS verified_sum
FROM CodSettlement GROUP BY status;
SELECT provider, status, COUNT_BIG(*) AS row_count,
       SUM(amount) AS amount_sum
FROM PaymentAttempt GROUP BY provider, status;
```

Expected RED nếu có: fee null không được schema giải thích; PaymentAttempt không khớp mô hình snapshot chứng minh được; COD delivered không gán duy nhất shipper/shift; unsupported loyalty type/missing order/duplicate reversal; notification source/deep link không catalog được. Mọi RED là STOP, không viết phép trừ suy đoán.

- [ ] **Step 5: Chốt local rewrite model và audit baseline**

Ghi rõ trong execution evidence: tập PaymentAttempt được rewrite chỉ khi runtime/source chứng minh amount chứa service fee; refund/COD snapshot được rewrite theo exact state; loyalty EARN dùng `floor(new_final_amount/1000)` và reversals không vượt EARN; PayOS external parity không được bảo đảm. Ghi row-level key counts, aggregate before values, catalog checksum và query checksum.

Expected GREEN: mọi row thuộc đúng một supported case; aggregate có thể tái tính deterministic. Bất kỳ row không phân loại duy nhất: STOP.

- [ ] **Step 6: Review task diff/status**

```powershell
git diff --check
git diff -- database Backend/FastGuy-FastFoodSite/src/main Frontend openapi
git status --short
```

Expected: không có repository diff từ inventory; chỉ thay đổi có sẵn của user được liệt kê, không bị chạm.

### Task 2: Migration 051, validator, canonical fresh schema/seeds và source policy tests

**Files:**
- Create: `database/migrations/051_remove_review_support_combo_service_fee.sql`
- Create: `database/migrations/051_validate.sql`
- Modify: `database/init.sql`
- Modify: `database/DB_FastGuy.sql`
- Modify: `database/seed_demo.sql`
- Modify: `database/migrations/RUNBOOK.md`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/ScopeSimplificationMigrationPolicyTest.java`
- Delete/Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/HomepageMerchandisingMigrationPolicyTest.java`

**Interfaces:**
- Consumes: Task 1 `PASS`, exact catalog names/constraints/attribution và semantic inventory parity đã chứng minh.
- Produces: idempotency-guarded migration với số chưa dùng tại thời điểm tạo (`051` nếu `050` đã merge trước), rerunnable read-only validator, fresh schemas/seeds không chứa removed objects; không tạo archive table.

- [ ] **Step 1: Viết failing source-policy test trước migration**

```java
@Test
void migration051IsDestructiveGuardedAndCanonicalSchemasMatchScope() throws Exception {
    String migration = Files.readString(repo("database/migrations/051_remove_review_support_combo_service_fee.sql"));
    String validator = Files.readString(repo("database/migrations/051_validate.sql"));
    assertAll(
        () -> assertTrue(migration.contains("SET XACT_ABORT ON")),
        () -> assertTrue(migration.contains("BEGIN TRY")),
        () -> assertTrue(migration.contains("THROW")),
        () -> assertTrue(migration.contains("051_remove_review_support_combo_service_fee")),
        () -> assertFalse(migration.toUpperCase().contains("CREATE TABLE [DBO].[ARCHIVE")),
        () -> assertTrue(validator.contains("ProductVariant")),
        () -> assertTrue(validator.contains("WorkShift")),
        () -> assertCanonicalRemoved("database/init.sql"),
        () -> assertCanonicalRemoved("database/DB_FastGuy.sql"),
        () -> assertCanonicalRemoved("database/seed_demo.sql")
    );
}
```

`assertCanonicalRemoved` phải parse statement/identifier thay vì cấm từ trong migration comments; cho phép tên trong validator assertion và runbook, không cho phép `CREATE/INSERT/ALTER` đối với removed tables/column/config.

- [ ] **Step 2: Chạy test để xác nhận RED**

```powershell
mvn -Dtest=service.ScopeSimplificationMigrationPolicyTest test
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: FAIL vì `051_remove_review_support_combo_service_fee.sql`/`051_validate.sql` chưa tồn tại và canonical SQL vẫn chứa removed objects.

- [ ] **Step 3: Viết migration tối thiểu theo exact inventory**

Migration phải: acquire migration applock; assert semantic inventory parity trực tiếp từ `sys.check_constraints`; assert exact preflight schema/checksum; materialize `#OrderFeeRewrite`, `#ShiftFeeDelta`, `#LoyaltyReconcile`; validate unique attribution/nonnegative trước update; update order/payment/refund/COD/loyalty theo key order; emit audit summary; purge legacy notifications/deep links dùng `SUPPORT_TICKET`; delete/drop `ProductComboItem → ProductCombo`, `Review`, `SupportTicket`; remove ShippingConfig key `service_fee`, Orders default/check phụ thuộc và cột `service_fee`; recreate final amount check theo công thức mới; insert migration history; commit. `CATCH` rollback + `THROW`.

Source-policy test phải yêu cầu statement thật cho từng snapshot, từng update và từng invariant; không chấp nhận comment thay implementation. Tên bảng history và columns lấy đúng từ catalog/canonical source (`SchemaMigrationHistory` hiện hành), không dùng tên minh họa. Transaction chỉ bao quanh snapshot/validate/write/drop/history; catalog reporting nặng chạy trước transaction.

- [ ] **Step 4: Viết validator read-only rerunnable**

`051_validate.sql` phải `THROW` khi: removed table/column/config còn tồn tại; WorkShift/ProductVariant/modifier thiếu hoặc đổi schema; money/points âm; final formula sai; PaymentAttempt không khớp local model đã khóa; settlement amount/status sai; loyalty balance khác ledger; orphan; migration history/checksum thiếu. Validator không DML/DDL/procedure execution.

```sql
SET NOCOUNT ON;
IF OBJECT_ID(N'dbo.Review', N'U') IS NOT NULL OR
   OBJECT_ID(N'dbo.SupportTicket', N'U') IS NOT NULL OR
   OBJECT_ID(N'dbo.ProductComboItem', N'U') IS NOT NULL OR
   OBJECT_ID(N'dbo.ProductCombo', N'U') IS NOT NULL
    THROW 51051, 'Removed table still exists', 1;
IF COL_LENGTH(N'dbo.Orders', N'service_fee') IS NOT NULL
    THROW 51051, 'Orders.service_fee still exists', 1;
IF EXISTS (
    SELECT 1 FROM dbo.Orders
    WHERE final_amount <> total_amount + shipping_fee - discount_amount
       OR final_amount < 0 OR total_amount < 0 OR shipping_fee < 0 OR discount_amount < 0
) THROW 51051, 'Order money invariant failed', 1;
IF OBJECT_ID(N'dbo.WorkShift', N'U') IS NULL OR OBJECT_ID(N'dbo.ProductVariant', N'U') IS NULL
    THROW 51051, 'Retained schema missing', 1;
```

- [ ] **Step 5: Đồng bộ canonical schema, seeds và runbook**

Xóa CREATE/FK/index/seed/config/column của bốn domain và service fee khỏi `init.sql`, `DB_FastGuy.sql`, `seed_demo.sql`; cập nhật final amount constraint. Không sửa `041_local_demo_seed.sql`: runbook đánh dấu deployed migration immutable và không được chạy seed legacy sau simplification. Runbook đặt migration parity trước simplification khi cả hai file cùng có trong branch; nếu runtime đã semantic-equivalent thì yêu cầu catalog evidence thay vì history ID, sau đó disposable restore/apply/rerun/validate, retained manual gate, backup restore recovery và PayOS disclaimer.

- [ ] **Step 6: Chạy policy test GREEN và stale-token scan**

```powershell
mvn -Dtest=service.ScopeSimplificationMigrationPolicyTest test
rg -n "CREATE TABLE.*(Review|SupportTicket|ProductCombo)|INSERT INTO.*(Review|SupportTicket|ProductCombo)|service_fee" database/init.sql database/DB_FastGuy.sql database/seed_demo.sql
```

Expected: test PASS; `rg` exit 1/no matches. `WorkShift`, `ProductVariant`, `ProductModifierGroup`, `ProductModifierOption` vẫn có canonical definitions/seeds.

- [ ] **Step 7: Review task diff/status**

```powershell
git diff --check
git diff -- database Backend/FastGuy-FastFoodSite/src/test/java/service/ScopeSimplificationMigrationPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/service/HomepageMerchandisingMigrationPolicyTest.java
git status --short
```

Expected: chỉ file task; không sửa migration `000–050`.

### Task 3: Disposable restored database preflight, apply, rerun, validate và integration evidence

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/integration/ScopeSimplificationMigrationIT.java`
- Read: Task 2 migration/validator/runbook
- No retained database write

**Interfaces:**
- Consumes: verified representative backup, exact disposable server/database, Task 2 artifacts, Task 1 baseline model.
- Produces: disposable evidence gồm restore checksum, preflight, apply, validator, rerun guard, validator lần hai, integration PASS; database disposable bị drop/cleanup theo operator policy, không retained write.

- [ ] **Step 1: Viết failing disposable integration assertions**

```java
@Test
void migratedDisposableDatabaseHasOnlyNewMoneyAndRetainedProductModel() throws Exception {
    assertFalse(tableExists("Review"));
    assertFalse(tableExists("SupportTicket"));
    assertFalse(tableExists("ProductComboItem"));
    assertFalse(tableExists("ProductCombo"));
    assertFalse(columnExists("Orders", "service_fee"));
    assertEquals(0, count("SELECT COUNT(*) FROM Orders WHERE final_amount <> total_amount + shipping_fee - discount_amount"));
    assertEquals(0, count("SELECT COUNT(*) FROM Orders WHERE final_amount < 0 OR cod_collected_amount < 0 OR refund_amount < 0"));
    assertEquals(0, count("SELECT COUNT(*) FROM PaymentAttempt WHERE amount < 0"));
    assertTrue(tableExists("WorkShift"));
    assertTrue(tableExists("ProductVariant"));
    assertTrue(tableExists("ProductModifierGroup"));
    assertTrue(tableExists("ProductModifierOption"));
    assertLoyaltyBalanceMatchesLedger();
    assertSettlementStatusesMatchAmounts();
}
```

Test fixture chỉ kết nối khi `FASTGUY_DB_DISPOSABLE=true` và database name có suffix `_Disposable`; nếu không, fail closed, không auto-create/drop retained target.

- [ ] **Step 2: Chạy test trước migration để xác nhận RED đúng lý do**

```powershell
$env:FASTGUY_DB_DISPOSABLE='true'
mvn -Pintegration -Dit.test=integration.ScopeSimplificationMigrationIT verify
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: FAIL vì restored pre-051 schema còn removed tables/service fee; không phải connection/credential error.

- [ ] **Step 3: Preflight disposable và xác nhận restore evidence**

```powershell
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 `
  -Mode Preflight -Server $env:FASTGUY_DB_SERVER `
  -Database $env:FASTGUY_DB_DISPOSABLE_NAME -Disposable
```

Expected: exact disposable identity, suffix/flag hợp lệ, restore checksum và semantic inventory parity present. Thiếu một điều kiện: STOP.

- [ ] **Step 4: Apply migration đúng một lần bằng sqlcmd có fail-fast**

```powershell
sqlcmd -b -V 16 -S "$env:FASTGUY_DB_SERVER" -d "$env:FASTGUY_DB_DISPOSABLE_NAME" -i ".\database\migrations\051_remove_review_support_combo_service_fee.sql"
if ($LASTEXITCODE -ne 0) { throw "051 apply failed" }
```

Expected GREEN: exit 0; audit summary row counts/totals khớp Task 1 baseline transformation; no retained target.

- [ ] **Step 5: Validate, rerun migration, validate lần hai**

```powershell
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 -Mode Validate -Server $env:FASTGUY_DB_SERVER -Database $env:FASTGUY_DB_DISPOSABLE_NAME -ScriptPath .\database\migrations\051_validate.sql -Disposable
sqlcmd -b -V 16 -S "$env:FASTGUY_DB_SERVER" -d "$env:FASTGUY_DB_DISPOSABLE_NAME" -i ".\database\migrations\051_remove_review_support_combo_service_fee.sql"
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 -Mode Validate -Server $env:FASTGUY_DB_SERVER -Database $env:FASTGUY_DB_DISPOSABLE_NAME -ScriptPath .\database\migrations\051_validate.sql -Disposable
```

Expected: cả ba exit 0; rerun báo history/object guard và không trừ tiền/points lần hai; audit totals trước/sau rerun giống nhau.

- [ ] **Step 6: Chạy integration test GREEN**

```powershell
mvn -Pintegration -Dit.test=integration.ScopeSimplificationMigrationIT verify
```

Expected: PASS; fresh/restored acceptance gồm no removed objects/data, money nonnegative, final invariant, PaymentAttempt local model, settlement recalculation, loyalty ledger/balance, WorkShift/variant/modifier retained.

- [ ] **Step 7: Review evidence và task diff/status**

```powershell
git diff --check
git diff -- Backend/FastGuy-FastFoodSite/src/test/java/integration/ScopeSimplificationMigrationIT.java
git status --short
```

Expected: evidence chứa target/checksum/exit codes nhưng không credential; không giữ database write ngoài disposable; chỉ test file thuộc task trong diff.

### Task 4: Retained deployment manual gate — dừng trước execution

**Files:**
- Read: `database/migrations/RUNBOOK.md`
- No repository write
- No retained SQL execution

**Interfaces:**
- Consumes: Task 3 evidence.
- Produces: trạng thái `BLOCKED_PENDING_EXPLICIT_RETAINED_APPROVAL`; chỉ chuyển thành execution riêng khi user cung cấp đầy đủ approval/recovery payload.

- [ ] **Step 1: Chạy gate check không phá hủy**

```powershell
$required = @(
  'FASTGUY_RETAINED_APPROVAL_ID','FASTGUY_RETAINED_SERVER','FASTGUY_RETAINED_DATABASE',
  'FASTGUY_BACKUP_VERIFIED','FASTGUY_RESTORE_REHEARSAL','FASTGUY_RESTORE_DURATION',
  'FASTGUY_RECOVERY_OWNER','FASTGUY_MAINTENANCE_WINDOW','FASTGUY_WRITES_STOPPED'
)
$missing = $required | Where-Object { -not (Test-Path "Env:$_") -or [string]::IsNullOrWhiteSpace((Get-Item "Env:$_").Value) }
if ($missing.Count -gt 0) { "BLOCKED_PENDING_EXPLICIT_RETAINED_APPROVAL: $($missing -join ', ')"; exit 3 }
```

Expected RED/BLOCKED trong plan implementation: exit 3 và danh sách evidence thiếu. Đây là hard stop mong đợi, không phải lỗi cần bypass.

- [ ] **Step 2: Xác nhận recovery semantics trên giấy tờ vận hành**

Reviewer phải thấy: exact server/database/migration checksum; full backup verified/off-host; restore rehearsal thành công và thời lượng đo; writes stopped; recovery owner/window/go-no-go; rollback sau commit bằng stop app + restore backup + previous validator + previous app version. Không có down-script/DML bù.

Expected: nếu thiếu bất kỳ mục nào, giữ `BLOCKED`. Không chạy preflight/apply retained trong task này.

- [ ] **Step 3: Review task diff/status và kết thúc PHASE A**

```powershell
git diff --check
git status --short
```

Expected: không có diff từ manual gate; implementation plan dừng retained lane tại đây. Chỉ source/API work tiếp tục trên disposable/local test environment sau Tasks 1–3 GREEN.

# PHASE B — Contract, backend và frontend removal

### Task 5: OpenAPI contract-first removal và refund/COD schemas

**Files:**
- Modify: `openapi/fastguy.yaml`
- Modify: `Frontend/test/openapi-contract.test.js`
- Modify: backend contract tests liệt kê trong File Map

**Interfaces:**
- Consumes: database contract sau Task 3; exact operations hiện có trong OpenAPI/provider inventory.
- Produces: OpenAPI không review/combo/serviceFee/homepage legacy; refund/COD schemas authoritative; removed legacy support routes vẫn không được thêm vào OpenAPI.

- [ ] **Step 1: Viết failing contract tests cho absence và schemas mới**

```js
test('scope simplification removes legacy fields and defines refund/COD semantics', () => {
  assert.equal(findOperationById(spec, 'getReviewByOrder'), undefined);
  assert.equal(findOperationById(spec, 'createReview'), undefined);
  assert.equal(findOperationById(spec, 'getAdminProductCombo'), undefined);
  assert.equal(spec.components.schemas.HomepageData.properties.occasionCombos, undefined);
  assert.equal(spec.components.schemas.HomepageData.properties.featuredReviews, undefined);
  assert.equal(hasSchemaProperty(spec, 'serviceFee'), false);
  assert.deepEqual(requiredOf('RefundDetail'), [
    'customerName','customerPhone','orderCode','paymentMethod','finalAmount','refundAmount',
    'refundStatus','refundReference','refundNote','processedBy','refundedAt'
  ]);
  assert.equal(schema('RefundCompletionRequest').required.includes('refundReference'), true);
  assert.equal(schema('CodSettlement').properties.difference.description.includes('verifiedAmount - submittedAmount'), true);
});
```

- [ ] **Step 2: Chạy contract test RED**

```powershell
npm test -- test/openapi-contract.test.js
```

Workdir: `Frontend`.

Expected: FAIL vì review/combo/serviceFee vẫn tồn tại; refund/COD schema chưa đủ.

- [ ] **Step 3: Sửa OpenAPI tối thiểu**

Xóa operations/schemas Review và ProductCombo; xóa featured review mutation; xóa `serviceFee`/`serviceFeeRevenue`; homepage required chỉ còn fields thực tế; order/settings/reports/checkout không field fee. Không thêm support operations legacy. Định nghĩa `RefundDetail`, completion request bắt buộc trimmed non-empty reference và error statuses theo endpoint hiện có. Định nghĩa COD required/nullability/enums, `difference = verifiedAmount - submittedAmount` khi đủ hai số, ngược lại null; `verifiedBy` representation duy nhất.

- [ ] **Step 4: Cập nhật backend contract tests trước provider**

Assertions phải yêu cầu exact serialized keys, absence của removed keys, 400/401/403/404/409 theo pattern hiện có, không kiểm tra implementation internals. Removed provider-route 404 test được viết trước khi xóa servlet/routes và phải RED vì mapping còn tồn tại.

- [ ] **Step 5: Chạy lint + contract tests GREEN cho contract source**

```powershell
npm run contract:lint
npm test -- test/openapi-contract.test.js
mvn -Dtest=servlet.HomepageAdminContractTest,servlet.AdminOrderServletBehaviorTest,servlet.CodSettlementApiContractTest,service.CodSettlementServiceContractTest test
```

Expected: Redocly PASS, không remote `$ref`; frontend OpenAPI test PASS. Backend tests mới vẫn RED ở assertions provider chưa đổi — đây là handoff cho Tasks 6–8/12–13, không nới contract để làm chúng pass.

- [ ] **Step 6: Review task diff/status**

```powershell
git diff --check
git diff -- openapi/fastguy.yaml Frontend/test/openapi-contract.test.js Backend/FastGuy-FastFoodSite/src/test/java
git status --short
```

Expected: chỉ contract và contract tests; không provider/consumer implementation.

### Task 6: Xóa Review/Support backend và homepage/admin wiring

**Files:**
- Delete: Review/Support entity, DAO, service, servlet files trong File Map
- Modify: `HomepageService.java`, `AdminOrderServlet.java`, notification callers được CodeGraph xác nhận
- Delete/Modify: focused Review/Support/Homepage tests trong File Map

**Interfaces:**
- Consumes: Task 5 homepage/order schemas không review.
- Produces: không runtime route/source Review/Support; homepage provider chỉ trả contract còn lại; removed route nhận 404 nếu security boundary không chặn trước.

- [ ] **Step 1: Viết/hoàn thiện failing absence tests**

```java
@Test
void homepageContainsNoReviewPayload() {
    Map<String,Object> data = service.getHomepage();
    assertFalse(data.containsKey("featuredReviews"));
    assertFalse(data.containsKey("occasionCombos"));
}

@Test
void removedReviewAndSupportRoutesAreUnmapped() {
    assertAll(
        () -> assertRouteAbsent("/api/reviews"),
        () -> assertRouteAbsent("/api/reviews/order/1"),
        () -> assertRouteAbsent("/api/support"),
        () -> assertRouteAbsent("/api/staff/support")
    );
}
```

- [ ] **Step 2: Chạy focused tests RED**

```powershell
mvn -Dtest=service.HomepageServiceTest,servlet.HomepageAdminContractTest test
```

Expected: FAIL vì homepage còn featured reviews/occasion combos hoặc routes/classes còn mapped.

- [ ] **Step 3: Xóa minimal runtime chain và callers**

Xóa exact files. Bỏ `ReviewDAO` dependency/query/map khỏi HomepageService; bỏ admin featured review serialization/mutation; bỏ support/review notification creation/deep links. Không sửa generic notification framework ngoài discriminator/caller đã mất. Giữ homepage best sellers, variants, modifier groups/options.

- [ ] **Step 4: Xử lý tests chuyên biệt có chủ đích**

Xóa `ReviewServletConsentTest`, `ReviewFeaturedMutationTest`, `SupportTicketOwnershipPolicyTest` vì capability bị xóa. Sửa `HomepageServiceTest`/`HomepageServiceIT` để bảo vệ best sellers, ProductVariant và modifiers; xóa test file chỉ khi toàn bộ assertions chuyên biệt review/combo và replacement absence test đã tồn tại.

- [ ] **Step 5: Chạy GREEN và route/source scan**

```powershell
mvn -Dtest=service.HomepageServiceTest,servlet.HomepageAdminContractTest test
rg -n "ReviewDAO|ReviewService|ReviewServlet|SupportTicketDAO|SupportTicketService|SupportTicketServlet|StaffSupportTicketServlet" Backend/FastGuy-FastFoodSite/src/main Backend/FastGuy-FastFoodSite/src/test
```

Expected: tests PASS; `rg` no runtime matches. Chỉ migration/validator/explicit absence test được phép chứa removed names.

- [ ] **Step 6: Review task diff/status**

```powershell
git diff --check
git diff -- Backend/FastGuy-FastFoodSite/src/main Backend/FastGuy-FastFoodSite/src/test
git status --short
```

Expected: chỉ Review/Support/homepage/admin wiring và focused tests.

### Task 7: Xóa combo backend, giữ ProductVariant và ProductModifier

**Files:**
- Delete: `entity/ProductCombo.java`, `entity/ProductComboItem.java`
- Modify: `ProductModifierDAO.java`, `HomepageService.java`, `ProductServlet.java`, `AdminProductServlet.java`
- Modify: product/homepage mapping and contract tests

**Interfaces:**
- Consumes: Task 5 Product/Homepage schema không combo; ProductVariant JSON field giữ nguyên.
- Produces: product/admin/homepage provider không combo routes/fields; variants và modifiers hoạt động nguyên contract.

- [ ] **Step 1: Viết failing preservation + absence tests**

```java
@Test
void productPayloadKeepsVariantsAndModifiersButHasNoCombo() {
    Map<String,Object> payload = productPayload();
    assertFalse(payload.containsKey("combo"));
    assertFalse(payload.containsKey("isCombo"));
    assertFalse(payload.containsKey("productType") && "COMBO".equals(payload.get("productType")));
    assertTrue(payload.containsKey("variants"));
    assertTrue(payload.containsKey("modifierGroups"));
    assertVariantKeepsPriceStockSkuAndGhnDimensions(payload);
}
```

Thêm route absence assertions cho `/api/admin/products/{id}/combo` và `/combo/items` dựa trên exact servlet mapping hiện có.

- [ ] **Step 2: Chạy focused tests RED**

```powershell
mvn -Dtest=servlet.HomepageAdminContractTest,entity.HomepageMerchandisingMappingTest test
```

Expected: FAIL vì combo fields/routes/entities còn tồn tại.

- [ ] **Step 3: Xóa combo minimal**

Xóa entities; bỏ combo JPQL/import/map/routes từ DAO/servlets/service. Không đổi ProductVariant entity/schema/key; không nhập ProductModifier vào variant; giữ SKU, price, quantityAvailable/stock, GHN length/width/height/weight theo provider hiện hành.

- [ ] **Step 4: Chạy GREEN và preservation scans**

```powershell
mvn -Dtest=servlet.HomepageAdminContractTest,entity.HomepageMerchandisingMappingTest,service.HomepageServiceTest test
rg -n "ProductCombo|ProductComboItem|homepageCombos|activeComboProductIds" Backend/FastGuy-FastFoodSite/src/main
rg -n "class ProductVariant|ProductModifierGroup|ProductModifierOption|sku|quantityAvailable|weight|length|width|height" Backend/FastGuy-FastFoodSite/src/main/java
```

Expected: first scan no matches; second chứng minh retained model/callers còn hiện diện; tests PASS.

- [ ] **Step 5: Review task diff/status**

```powershell
git diff --check
git diff -- Backend/FastGuy-FastFoodSite/src/main Backend/FastGuy-FastFoodSite/src/test
git status --short
```

Expected: combo-only removal và preservation tests; WorkShift/ProductVariant entities không bị sửa schema.

### Task 8: Xóa service fee backend checkout/serialization/report/settings

**Files:**
- Modify: `entity/Orders.java`, `service/OrderService.java`, `service/StoreConfigService.java`, `servlet/StoreConfigServlet.java`, `servlet/AdminOrderServlet.java`
- Modify: actual report service/servlet được CodeGraph xác nhận
- Modify: checkout/order/store/report tests

**Interfaces:**
- Consumes: Task 5 no-serviceFee contract, Task 3 DB without column/config.
- Produces: order creation/serialization/report/settings dùng `final = total + shipping - discount`; không field zero compatibility.

- [ ] **Step 1: Viết failing money tests**

```java
@Test
void checkoutTotalHasNoServiceFee() {
    CheckoutResult result = checkout(subtotal("100000.00"), shipping("15000.00"), discount("10000.00"));
    assertEquals(new BigDecimal("105000.00"), result.finalAmount());
    assertFalse(result.serialized().containsKey("serviceFee"));
}

@Test
void reportsAndSettingsExposeNoServiceFee() {
    assertFalse(adminReport().containsKey("serviceFeeRevenue"));
    assertFalse(storeConfig().containsKey("serviceFee"));
}
```

Dùng actual return/map types hiện có; không tạo DTO chỉ để phục vụ test.

- [ ] **Step 2: Chạy focused tests RED**

```powershell
mvn -Dtest=service.CheckoutShippingPolicyTest,service.StoreConfigPolicyTest,service.AdminOperationalReportingPolicyTest,servlet.AdminOrderServletBehaviorTest test
```

Expected: FAIL vì OrderService còn đọc service fee, serializer/settings/report còn field.

- [ ] **Step 3: Sửa minimal provider**

Bỏ Orders mapping/getter/setter; đổi `validateBusinessHoursAndGetServiceFee` thành validation giờ mở cửa không trả fee; bỏ StoreConfig key; xóa serializer/report aggregation. Tạo đơn và guest checkout dùng duy nhất total + shipping - discount. Không trả `serviceFee: 0`.

- [ ] **Step 4: Chạy GREEN và stale scan**

```powershell
mvn -Dtest=service.CheckoutShippingPolicyTest,service.StoreConfigPolicyTest,service.AdminOperationalReportingPolicyTest,servlet.AdminOrderServletBehaviorTest test
rg -n "serviceFee|getServiceFee|setServiceFee|service_fee|serviceFeeRevenue" Backend/FastGuy-FastFoodSite/src/main Backend/FastGuy-FastFoodSite/src/test
```

Expected: tests PASS; no runtime matches; chỉ explicit absence tests được phép match.

- [ ] **Step 5: Review task diff/status**

```powershell
git diff --check
git diff -- Backend/FastGuy-FastFoodSite/src/main Backend/FastGuy-FastFoodSite/src/test
git status --short
```

Expected: service-fee-specific diff; payment/COD/refund policy ngoài formula không refactor.

### Task 9: Xóa frontend Review/Support/homepage sections/routes/API/components

**Files:**
- Delete/Modify: exact frontend files nêu trong File Map
- Modify: related homepage/router/navigation/order tests

**Interfaces:**
- Consumes: Task 5/6 homepage and removed-route contract.
- Produces: không UI/client/navigation gọi Review/Support; homepage không proof/occasion sections.

- [ ] **Step 1: Viết failing frontend absence tests**

```js
test('router and API barrel expose no review/support capability', async () => {
  const routerSource = await read('src/router/index.js');
  const apiSource = await read('src/api/index.js');
  assert.doesNotMatch(routerSource, /UserSupport|StaffSupport|\/support/);
  assert.doesNotMatch(apiSource, /reviewApi|supportApi/);
});

test('homepage renders neither occasion combos nor featured reviews', () => {
  assert.equal(homepageSections({ bestSellers: [] }).includes('occasionCombos'), false);
  assert.equal(homepageSections({ bestSellers: [] }).includes('featuredReviews'), false);
});
```

- [ ] **Step 2: Chạy RED**

```powershell
npm test -- tests/staff-support-ownership.test.mjs test/homepage-schema.test.js
```

Expected: FAIL vì support routes/API và homepage consumers/components còn tồn tại. Nếu test support cũ chỉ bảo vệ capability bị xóa, thay nội dung bằng absence assertion trước khi xóa file test.

- [ ] **Step 3: Xóa minimal consumers**

Xóa API files/views/components; bỏ barrel exports, lazy routes, nav/title entries, review actions trong order detail/admin, homepage store/utils/template branches. Không để link/nút gọi removed endpoints.

- [ ] **Step 4: Chạy GREEN và source scan**

```powershell
npm test -- tests/staff-support-ownership.test.mjs test/homepage-schema.test.js
rg -n "reviewApi|supportApi|UserSupport|StaffSupport|HomepageProof|HomepageOccasions|featuredReviews|occasionCombos" Frontend/src Frontend/test Frontend/tests
```

Expected: tests PASS; no runtime matches; explicit absence test matches được review thủ công.

- [ ] **Step 5: Review task diff/status**

```powershell
git diff --check
git diff -- Frontend/src Frontend/test Frontend/tests
git status --short
```

Expected: chỉ Review/Support/homepage removal; không combo/service fee/COD/refund implementation trong task.

### Task 10: Xóa combo frontend; relabel ProductVariant thành “Kích cỡ”; giữ topping/tùy chọn

**Files:**
- Delete: `Frontend/src/components/admin/product-editor/ProductComboSection.vue`
- Modify: `Frontend/src/api/admin.js`, `Frontend/src/views/admin/ProductEditorPage.vue`, product detail/cart/editor components và tests được CodeGraph xác nhận
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`, product tests

**Interfaces:**
- Consumes: Task 5/7 product contract giữ `variants`/`modifierGroups`, không combo.
- Produces: không combo editor/consumer/API; visible wording “Kích cỡ”; underlying variant payload keys nguyên vẹn; topping separate.

- [ ] **Step 1: Viết failing UI contract tests**

```js
test('product UI says Kích cỡ and retains variant/modifier payload keys', async () => {
  const source = await productSurfaceSource();
  assert.doesNotMatch(source, /Combo cố định|createCombo|comboItem/);
  assert.match(source, /Kích cỡ/);
  assert.match(source, /variantId|variantName|sku|quantityAvailable/);
  assert.match(source, /modifierGroups|modifierOptions|Topping|tùy chọn/i);
});
```

- [ ] **Step 2: Chạy RED**

```powershell
npm test -- tests/admin-product-editor-contract.test.mjs test/product-card-eligibility.test.js test/product-store.test.js
```

Expected: FAIL vì combo editor/API hoặc visible “Biến thể” còn tồn tại.

- [ ] **Step 3: Xóa/relabel minimal**

Xóa component và admin API combo methods; bỏ editor section/state/dirty tracking combo; bỏ homepage/product combo badges/branches. Đổi mọi customer/admin visible label, loading/empty/error/validation/aria-label từ “Biến thể” sang “Kích cỡ”. Không đổi `variantId`, `variantName`, route, payload, entity/schema. Giữ topping/modifier selection riêng.

- [ ] **Step 4: Chạy GREEN và preservation scan**

```powershell
npm test -- tests/admin-product-editor-contract.test.mjs test/product-card-eligibility.test.js test/product-store.test.js
rg -n "createCombo|updateCombo|deleteCombo|ProductComboSection|Combo cố định|occasionCombo" Frontend/src
rg -n "variantId|variantName|sku|quantityAvailable|modifierGroups|modifierOptions" Frontend/src
```

Expected: first scan no matches; second có retained consumers; tests chứng minh size price/stock/SKU/GHN và modifiers còn hoạt động.

- [ ] **Step 5: Review task diff/status**

```powershell
git diff --check
git diff -- Frontend/src Frontend/test Frontend/tests
git status --short
```

Expected: combo removal + wording only; no API key rename.

### Task 11: Xóa service fee frontend checkout/settings/reports/details

**Files:**
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Modify: `Frontend/src/views/user/OrderDetailPage.vue`, `OrderSuccessPage.vue`, admin/staff/shipper order detail views nếu actual consumer có fee
- Modify: `Frontend/src/views/admin/SettingsPage.vue`, `ReportsPage.vue`, `RefundsPage.vue`
- Modify: related format/state/tests/fixtures

**Interfaces:**
- Consumes: Task 5/8 no-serviceFee contract/provider.
- Produces: UI total chỉ subtotal + shipping − discount; không settings/filter/KPI/report/detail fee.

- [ ] **Step 1: Viết failing consumer tests**

```js
test('checkout breakdown uses only subtotal shipping and discount', () => {
  const rows = checkoutRows({ subtotal: 100000, shippingFee: 15000, discountAmount: 10000, finalAmount: 105000 });
  assert.deepEqual(rows.map(row => row.key), ['subtotal', 'shippingFee', 'discountAmount', 'finalAmount']);
  assert.equal(rows.some(row => /service/i.test(row.key)), false);
});
```

Thêm source/fixture assertions không `serviceFee`/`serviceFeeRevenue` ở settings, reports, order/refund detail.

- [ ] **Step 2: Chạy RED**

```powershell
npm test -- test/checkout-price-breakdown.test.js test/refund-state.test.js tests/admin-reporting-policy.test.mjs
```

Expected: FAIL tại service fee row/fixture/KPI hiện có. Nếu exact filename inventory khác, dùng file test hiện có chứa consumer; không tạo alias test trùng.

- [ ] **Step 3: Sửa minimal UI consumers**

Xóa computed/data/labels/formatting/filters/settings fields và report chart/KPI service fee. Final total đọc provider finalAmount và test formula; frontend không giữ compatibility zero.

- [ ] **Step 4: Chạy GREEN và stale scan**

```powershell
npm test -- test/checkout-price-breakdown.test.js test/refund-state.test.js tests/admin-reporting-policy.test.mjs
rg -n "serviceFee|service_fee|serviceFeeRevenue|Phí dịch vụ" Frontend/src Frontend/test Frontend/tests
```

Expected: tests PASS; no runtime/fixture matches; explicit absence tests duy nhất được phép match.

- [ ] **Step 5: Review task diff/status**

```powershell
git diff --check
git diff -- Frontend/src Frontend/test Frontend/tests
git status --short
```

Expected: service fee removal only.

# PHASE C — Operator enhancement

### Task 12: Refund detail provider/API/UI và bắt buộc refundReference khi hoàn tất

**Files:**
- Modify: `openapi/fastguy.yaml` only if Task 5 test exposes inconsistency; otherwise no contract change
- Modify: `RefundService.java`, `AdminRefundServlet.java`, `AdminOrderServlet.java`
- Modify: `Frontend/src/api/admin.js`, `Frontend/src/views/admin/RefundsPage.vue`, `Frontend/src/views/admin/refund-state.js`
- Modify/add focused backend/frontend tests

**Interfaces:**
- Consumes: Task 5 `RefundDetail`/completion contract.
- Produces: snapshot contact/detail; required exact reference; label/help text/tel link; no generated order-code substitute.

- [ ] **Step 1: Viết failing backend policy/contract tests**

```java
@Test
void completingRefundRequiresExternalReferenceAndSerializesSnapshotContact() {
    assertThrows(IllegalArgumentException.class, () -> completeRefund(orderId, "   "));
    Map<String,Object> detail = refundDetail(orderId);
    assertEquals(order.getCustomerName(), detail.get("customerName"));
    assertEquals(order.getCustomerPhone(), detail.get("customerPhone"));
    assertEquals(Set.of("customerName","customerPhone","orderCode","paymentMethod","finalAmount",
        "refundAmount","refundStatus","refundReference","refundNote","processedBy","refundedAt"), detail.keySet());
}
```

Thêm success assertion reference được trim/lưu đúng Admin input, không bằng orderCode trừ khi Admin thật sự nhập cùng chuỗi; double terminal request theo policy hiện có không ghi lần hai.

- [ ] **Step 2: Chạy backend RED**

```powershell
mvn -Dtest=service.RefundServiceTest,servlet.AdminOrderServletBehaviorTest test
```

Expected: FAIL vì reference chưa required hoặc detail thiếu contact/processed representation.

- [ ] **Step 3: Implement backend GREEN tối thiểu**

Dùng Orders customer snapshot; trim/non-empty/length theo OpenAPI; persist exact normalized input; serialize đủ 11 fields; auth Admin active và state transition/rollback giữ nguyên. Không tự sinh reference, không lấy profile phone thay snapshot.

- [ ] **Step 4: Viết frontend RED**

```js
test('refund completion explains and requires evidence reference', () => {
  const state = createRefundCompletionState();
  assert.equal(state.validate({ refundReference: '   ' }).refundReference.length > 0, true);
  assert.equal(REFUND_REFERENCE_LABEL, 'Mã giao dịch/biên nhận hoàn tiền');
  assert.match(REFUND_REFERENCE_HELP, /ngân hàng|ví điện tử|chuyển khoản thủ công/);
});
```

Component test/policy assertion phải thấy `href="tel:<customerPhone snapshot>"`, visible phone text, loading/not-found/error/retry, mutation lock và refetch sau success.

- [ ] **Step 5: Chạy frontend RED rồi implement UI**

```powershell
npm test -- test/refund-state.test.js test/refund-modal-state.test.js test/refund-production-state.test.js
```

Expected RED trước implementation: missing required validation/label/help/contact. Sau minimal API/state/view changes, chạy lại expected PASS.

- [ ] **Step 6: Chạy full focused GREEN**

```powershell
mvn -Dtest=service.RefundServiceTest,servlet.AdminOrderServletBehaviorTest test
npm test -- test/refund-state.test.js test/refund-modal-state.test.js test/refund-production-state.test.js
```

Expected: PASS; thiếu/rỗng reference bị backend/UI từ chối; detail/contact/reference semantics đúng contract.

- [ ] **Step 7: Review task diff/status**

```powershell
git diff --check
git diff -- openapi Backend/FastGuy-FastFoodSite/src Frontend/src Frontend/test
git status --short
```

Expected: refund-only diff; không thay payment provider hoặc WorkShift.

### Task 13: COD settlement API/UI authoritative, immutable và accessible

**Files:**
- Modify: `CodSettlementService.java`, `CodSettlementServlet.java`
- Modify: `Frontend/src/api/codSettlement.js`, `Frontend/src/views/admin/CodSettlementsPage.vue`, shipper/admin COD state helpers
- Modify: COD backend/frontend tests trong File Map

**Interfaces:**
- Consumes: Task 5 COD contract.
- Produces: server authoritative `difference/status`; immutable submit; Admin active transitions; complete responsive/accessibility UI with stale conflict recovery.

- [ ] **Step 1: Viết failing backend behavior tests**

```java
@Test
void serverComputesDifferenceAndStatusAndRejectsStaleTransition() {
    Map<String,Object> shortResult = verify(submitted("100000.00"), verified("99000.00"), reason("Thiếu tiền"));
    assertEquals(new BigDecimal("-1000.00"), shortResult.get("difference"));
    assertEquals("SHORT", shortResult.get("status"));
    assertThrows(SettlementConflictException.class, () -> verifyAgain(shortResult));
}

@Test
void submitIsImmutablePerShipperShift() {
    submit(shipperId, shiftId, new BigDecimal("100000.00"));
    assertThrows(SettlementConflictException.class,
        () -> submit(shipperId, shiftId, new BigDecimal("90000.00")));
}
```

Thêm match = SETTLED, over = OVER, mismatch without reason = 400/no write, non-active/non-admin = 403, stale expectedStatus = 409, transaction rollback assertions. Không truyền client-chosen final status nếu contract Task 5 đã chốt server derives status; request chỉ mang expectedStatus/verifiedAmount/reason.

- [ ] **Step 2: Chạy backend RED**

```powershell
mvn -Dtest=service.CodSettlementTransactionFlowTest,service.CodSettlementServiceContractTest,servlet.CodSettlementApiContractTest test
```

Expected: FAIL vì `difference`/`verifiedBy` thiếu hoặc status còn phụ thuộc client.

- [ ] **Step 3: Implement provider minimal**

Trong transaction: lock settlement; revalidate `SUBMITTED`; auth active Admin; normalize amount; derive comparison/status server-side; require reason cho SHORT/OVER; preserve immutable submitted snapshot; set verified amount/reason/verifier/time; flush/commit; rollback mọi RuntimeException. `toMap` trả shipper/shift/expected/submitted/verified/difference/status/reason/submittedAt/verifiedAt/verifiedBy.

- [ ] **Step 4: Chạy backend GREEN**

```powershell
mvn -Dtest=service.CodSettlementTransactionFlowTest,service.CodSettlementShiftPolicyTest,service.CodSettlementServiceContractTest,servlet.CodSettlementApiContractTest test
```

Expected: PASS; WorkShift selection/window tests unchanged.

- [ ] **Step 5: Viết frontend RED cho clarity/conflict/accessibility**

```js
test('COD verify state derives preview but sends no authoritative status', () => {
  const payload = buildVerifyPayload({ expectedStatus: 'SUBMITTED', submittedAmount: 100000, verifiedAmount: 99000, reason: 'Thiếu tiền' });
  assert.deepEqual(payload, { expectedStatus: 'SUBMITTED', verifiedAmount: 99000, reason: 'Thiếu tiền' });
});
```

Policy/component assertions: list/detail hiển thị đủ fields; verify chỉ với SUBMITTED; difference visible; 409 hiển thị conflict và refetch; modal trap focus, Escape/close, restore trigger focus; labels/error via `aria-describedby`; controls ≥24×24; mobile card/stack không overflow horizontal.

- [ ] **Step 6: Implement frontend rồi chạy GREEN**

```powershell
npm test -- test/admin-cod-settlement-state.test.js tests/shipper-operations-contract.test.mjs
```

Expected: PASS; UI không gửi status guessed; loading/empty/error/retry/conflict/mutation lock/refetch đạt; shipper submit không edit/resubmit.

- [ ] **Step 7: Review task diff/status**

```powershell
git diff --check
git diff -- Backend/FastGuy-FastFoodSite/src Frontend/src Frontend/test Frontend/tests
git status --short
```

Expected: COD-only diff; WorkShift schema/lifecycle không đổi.

### Task 14: Full contract/backend/frontend/integration/Playwright verification và stale-reference scan

**Files:**
- Modify/add only missing E2E specs under `Frontend/tests/e2e/`
- No production implementation unless a failing acceptance test identifies an in-scope defect; defect phải quay lại RED/GREEN task sở hữu

**Interfaces:**
- Consumes: Tasks 1–13 GREEN, disposable test environment, backend/frontend URLs and role fixtures.
- Produces: complete verification evidence; không production/retained claim.

- [ ] **Step 1: Viết E2E acceptance cases trước khi sửa fixture/implementation**

Desktop + mobile phải cover: homepage không review/occasion combo; removed customer/staff routes render 404/not-found; checkout chỉ subtotal+shipping−discount; chọn Kích cỡ và topping; admin product editor không combo; refund detail/contact/tel/reference completion; COD verify match/mismatch/conflict; shift workflows unchanged.

```js
test('checkout has no service fee and variants/modifiers remain usable', async ({ page }) => {
  await page.getByRole('button', { name: /Kích cỡ/i }).click();
  await page.getByRole('checkbox', { name: /topping/i }).check();
  await expect(page.getByText('Phí dịch vụ')).toHaveCount(0);
  await expect(page.getByText(/Tổng cộng/)).toBeVisible();
});
```

Mỗi spec thu page errors, console errors và critical API responses; dùng role/label/test-id, condition waits, không sleep.

- [ ] **Step 2: Chạy focused E2E để xác nhận RED đúng lý do nếu acceptance còn thiếu**

```powershell
npm run test:e2e -- --project=chromium tests/e2e/home.spec.js
```

Expected trước khi bổ sung fixture/selector còn thiếu: FAIL ở behavior cụ thể, không fail do unknown environment/auth. Nếu implementation đã đạt và test PASS ngay, chứng minh test bằng cách tạm đảo assertion locally, quan sát fail, hoàn nguyên assertion trước khi tiếp tục.

- [ ] **Step 3: Chạy OpenAPI lint và contract suites**

```powershell
npm run contract:lint
npm test -- test/openapi-contract.test.js
mvn -Dtest=servlet.HomepageAdminContractTest,servlet.AdminOrderServletBehaviorTest,servlet.CodSettlementApiContractTest,service.CodSettlementServiceContractTest test
```

Expected: tất cả PASS; removed fields absent schemas/JSON; removed provider routes 404; refund/COD exact interfaces.

- [ ] **Step 4: Chạy database disposable integration lại từ clean restore**

Lặp đúng Task 3: restore representative backup vào disposable mới, preflight, apply `051`, validate, rerun, validate, integration test.

```powershell
mvn -Pintegration -Dit.test=integration.ScopeSimplificationMigrationIT verify
```

Expected: PASS; không removed tables/data/config/column; money/points nonnegative; final invariant; PaymentAttempt local model; settlement recalculation/status; loyalty ledger/balance; WorkShift/variants/modifiers retained. Không retained write.

- [ ] **Step 5: Chạy toàn backend**

```powershell
mvn test
```

Workdir: `Backend/FastGuy-FastFoodSite`.

Expected: BUILD SUCCESS, zero failed/error tests.

- [ ] **Step 6: Chạy toàn frontend test/build**

```powershell
npm test
npm run build
```

Workdir: `Frontend`.

Expected: all Node tests PASS; Vite build exit 0; no unresolved imports từ deleted files.

- [ ] **Step 7: Chạy Playwright desktop và mobile**

```powershell
npm run test:e2e -- --project=chromium
npm run test:e2e -- --project=mobile
```

Nếu `playwright.config.js` dùng tên project khác, dùng exact desktop/mobile project names đã khai báo trong config, không thêm browser/dependency mới.

Expected: homepage, checkout, Kích cỡ+topping, admin editor, COD, refund, shift smoke PASS; zero uncaught page error/console error; critical requests HTTP success; no request tới removed APIs; screenshots/traces chỉ failure artifacts, không commit.

- [ ] **Step 8: Chạy stale-reference và schema-preservation scans**

```powershell
rg -n "ReviewDAO|ReviewService|ReviewServlet|SupportTicketDAO|SupportTicketService|SupportTicketServlet|ProductCombo|ProductComboItem|serviceFee|service_fee|serviceFeeRevenue|featuredReviews|occasionCombos|reviewApi|supportApi" Backend/FastGuy-FastFoodSite/src/main Frontend/src openapi/fastguy.yaml database/init.sql database/DB_FastGuy.sql database/seed_demo.sql
rg -n "WorkShift|ProductVariant|ProductModifierGroup|ProductModifierOption|variantId|variantName|sku|quantityAvailable" Backend/FastGuy-FastFoodSite/src/main Frontend/src openapi/fastguy.yaml database/init.sql database/DB_FastGuy.sql
```

Expected: first scan no runtime/canonical/contract matches; second scan có retained model/provider/consumer references. Migration/validator/tests được scan riêng và chỉ chứa removed names để drop/assert absence.

- [ ] **Step 9: Self-review spec coverage, placeholders, interfaces và destructive safety**

```powershell
$plan = Get-Content -Raw docs/superpowers/plans/2026-08-21-fastguy-scope-simplification.md
$redFlags = @(('T'+'BD'), ('T'+'ODO'), ('implement'+' later'), ('fill in'+' details'), ('add'+' appropriate'), ('similar to'+' Task'))
$redFlags | Where-Object { $plan.Contains($_) }
rg -n "051_remove_review_support_combo_service_fee.sql|051_validate.sql|050|sysadmin|XACT_ABORT|PayOS|refundReference|difference|WorkShift|ProductVariant|ProductModifier" docs/superpowers/plans/2026-08-21-fastguy-scope-simplification.md
```

Expected: first scan no matches; second bao phủ mọi safety/interface anchor. Reviewer đối chiếu đủ acceptance: fresh schema absence; disposable rewrite invariants; endpoint 404/JSON absence; checkout formula; refund contact/reference semantics; server-authoritative COD; unchanged shifts; size price/stock/SKU/GHN; modifiers.

- [ ] **Step 10: Final diff/status review, không commit/push**

```powershell
git diff --check
git diff --stat
git diff -- database openapi Backend/FastGuy-FastFoodSite Frontend
git status --short
```

Expected: `git diff --check` exit 0; chỉ files trong File Map và E2E acceptance files cần thiết; không credential, browser profile/report, archive table, migration deployed edit, retained execution artifact hoặc thay đổi user không liên quan. Báo rõ mọi lệnh fail; không tuyên bố hoàn tất/production/retained deployment khi còn failure.

## Acceptance Matrix

| Acceptance | Chứng minh |
|---|---|
| Fresh schema không removed tables/service fee config/column | Task 2 policy test + canonical SQL fresh build + Task 14 scan |
| Disposable migration xóa data/schema, không âm, final invariant | Tasks 3/14 validator + `ScopeSimplificationMigrationIT` |
| PaymentAttempt nhất quán local model | Task 1 classification + Task 2 rewrite + Tasks 3/14 validator |
| Settlement amounts/status recalculated | Task 2 `#ShiftFeeDelta` + Tasks 3/13 tests |
| Loyalty balance khớp ledger | Task 2 reconcile + Tasks 3/14 integration |
| Removed endpoints 404, fields absent schemas/JSON | Tasks 5–8 contract/provider absence tests |
| Checkout chỉ subtotal + shipping − discount | Tasks 8/11 unit + Task 14 E2E |
| Refund admin thấy contact/reference semantics | Task 12 backend/frontend + Task 14 desktop/mobile |
| COD difference/status authoritative server-side | Task 13 transaction/contract/UI tests |
| Shift workflows unchanged | Tasks 2/3 schema checks + Task 13 policy + Task 14 smoke |
| ProductVariant giữ size price/stock/SKU/GHN | Tasks 7/10 preservation tests + Task 14 E2E |
| ProductModifier topping/tùy chọn hoạt động | Tasks 7/10 preservation tests + Task 14 E2E |
| Retained write chưa diễn ra | Task 4 BLOCKED gate + execution evidence review |
