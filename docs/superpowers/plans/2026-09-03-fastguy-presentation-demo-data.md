# FastGuy Full Presentation Demo Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mở rộng bộ `DEMO-PRES-*` thành dữ liệu trình diễn toàn nghiệp vụ, áp dụng an toàn và idempotent lên `FastGuyDB`, rồi commit các thay đổi đã kiểm chứng vào local `main`.

**Architecture:** Giữ một seed transactional và một validator độc lập vì các nhóm đơn hàng, hoàn tiền, COD, nhân sự và kho có quan hệ khóa ngoại chặt chẽ. Seed chỉ thay thế dữ liệu có business key `DEMO-PRES-*`, upsert tài nguyên có thể được dữ liệu ngoài namespace tham chiếu, được kiểm chứng hai lần trên `DemoDatabase` trước khi tạo bản target-locked tạm cho `FastGuyDB`.

**Tech Stack:** SQL Server T-SQL, Java 17, JUnit 5, Maven, Vue 3/Vite, Node test runner, Playwright Chromium, `sqlcmd`.

## Global Constraints

- Không đổi schema hoặc chạy `database/init.sql` trên `FastGuyDB`.
- Giữ nguyên exact target guard `DemoDatabase` trong source repository; bản `FastGuyDB` chỉ được tạo tạm ngoài workspace.
- Chỉ sở hữu và thay thế dữ liệu có namespace `DEMO-PRES-*` hoặc quan hệ trực tiếp tới các bản ghi đó.
- Không xóa coupon demo nếu dữ liệu ngoài namespace đang tham chiếu.
- Mọi seed chạy trong transaction với `XACT_ABORT ON`, `TRY/CATCH` và session opt-in `FASTGUY_ALLOW_PRESENTATION_DEMO_SEED=1`.
- Giữ 20 sản phẩm, 20 biến thể, 20 nguyên liệu, 40 dòng công thức và 45 đơn hàng.
- Có đúng 7 đơn `DELIVERED`/`PAID` trong 7 ngày gần nhất, gồm đúng một đơn hôm nay.
- Không gọi PayOS, GHN hoặc dịch vụ tài chính thật.
- Không stage `.agents`, `.hermes`, diagrams, tài liệu người dùng hoặc artifact không liên quan.
- Repository đang ở `main`; tích hợp là commit trực tiếp vào local `main`, không tạo merge commit giả.

---

### Task 1: Khóa contract dữ liệu toàn nghiệp vụ bằng source-policy test

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`

**Interfaces:**
- Consumes: `database/seed_presentation_demo.sql`, `database/seed_presentation_demo_validate.sql` dưới dạng source text.
- Produces: policy tokens khóa namespace, số lượng, 7-day dashboard, refund, COD, shift, attendance, pay rate, coupon upsert và validator output.

- [ ] **Step 1: Thêm assertions RED cho các nhóm dữ liệu còn thiếu**

Thêm các token rõ nghĩa vào test:

```java
for (String token : new String[] {
        "@ExpectedRefundOrders int=3",
        "@ExpectedCodSettlements int=4",
        "@ExpectedDemoShifts int=9",
        "@ExpectedPayRates int=2",
        "DEMO-PRES-SHIFT",
        "DEMO-PRES-COD"
}) assertTrue(seed.contains(token), token);

for (String token : new String[] {
        "@ExpectedRefundOrders int=3",
        "@ExpectedCodSettlements int=4",
        "@ExpectedDemoShifts int=9",
        "@ExpectedPayRates int=2"
}) assertTrue(validator.contains(token), token);
```

- [ ] **Step 2: Chạy targeted test và xác nhận RED**

Run: `mvn -Dtest=PresentationDemoSeedPolicyTest test`

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: FAIL tại token đầu tiên chưa có trong seed/validator.

- [ ] **Step 3: Không commit riêng test đỏ**

Giữ test đỏ trong working tree để Task 2–4 lần lượt đưa về GREEN.

### Task 2: Mở rộng đơn hàng, doanh thu và hoàn tiền

**Files:**
- Modify: `database/seed_presentation_demo.sql`
- Modify: `database/seed_presentation_demo_validate.sql`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`

**Interfaces:**
- Consumes: 45 `DEMO-PRES-ORD-*`, `Orders.refund_status/refund_amount/refunded_at/refund_processed_by/refund_reference`, `OrderItem`.
- Produces: 7 điểm doanh thu, doanh thu hôm nay, completion rate hôm nay, top 5 sản phẩm và 3 trạng thái hoàn tiền hợp lệ.

- [ ] **Step 1: Khai báo exact expectations**

Trong seed và validator thêm:

```sql
DECLARE @ExpectedDeliveredLast7Days int=7;
DECLARE @ExpectedDeliveredToday int=1;
DECLARE @ExpectedRefundOrders int=3;
```

- [ ] **Step 2: Gán ba order demo vào các trạng thái refund hợp lệ**

Sau khi insert 45 orders, update ba order đã giao ngoài nhóm 7 ngày gần nhất:

```sql
UPDATE dbo.Orders
SET refund_status=CASE RIGHT(order_code,3) WHEN '019' THEN 'PENDING' WHEN '029' THEN 'REFUNDED' ELSE 'REJECTED' END,
    refund_amount=CASE WHEN RIGHT(order_code,3)='029' THEN final_amount ELSE NULL END,
    refunded_at=CASE WHEN RIGHT(order_code,3)='029' THEN DATEADD(hour,4,delivered_at) END,
    refund_note=CASE RIGHT(order_code,3) WHEN '019' THEN N'Yêu cầu hoàn tiền trình diễn' WHEN '029' THEN N'Đã hoàn tiền trình diễn' ELSE N'Từ chối do không đủ bằng chứng' END,
    refund_processed_by=CASE WHEN RIGHT(order_code,3) IN('029','039') THEN @ActorId END,
    refund_reference=CASE WHEN RIGHT(order_code,3)='029' THEN N'DEMO-PRES-REFUND-029' END
WHERE order_code IN('DEMO-PRES-ORD-019','DEMO-PRES-ORD-029','DEMO-PRES-ORD-039');
```

Chọn exact order IDs thực sự có timeline phù hợp; nếu trạng thái hiện tại không phải `DELIVERED`, điều chỉnh ba số nhưng giữ business key deterministic.

- [ ] **Step 3: Validator khóa dashboard và refund invariants**

Thêm kiểm tra exact 7-day/today/top-product hiện có và:

```sql
IF (SELECT COUNT(*) FROM dbo.Orders WHERE order_code LIKE 'DEMO-PRES-ORD-%' AND refund_status IS NOT NULL)<>@ExpectedRefundOrders THROW 51924, 'Expected presentation refund diversity', 1;
IF EXISTS(SELECT 1 FROM dbo.Orders WHERE order_code LIKE 'DEMO-PRES-ORD-%' AND ((refund_status='REFUNDED' AND (refund_amount IS NULL OR refunded_at IS NULL OR refund_processed_by IS NULL OR refund_reference IS NULL)) OR (refund_status='PENDING' AND refunded_at IS NOT NULL))) THROW 51925, 'Presentation refund timeline invalid', 1;
```

- [ ] **Step 4: Chạy targeted test**

Run: `mvn -Dtest=PresentationDemoSeedPolicyTest test`

Expected: vẫn FAIL chỉ vì COD/shift/pay-rate token chưa được triển khai.

### Task 3: Bổ sung lịch làm, chấm công và mức lương

**Files:**
- Modify: `database/seed_presentation_demo.sql`
- Modify: `database/seed_presentation_demo_validate.sql`

**Interfaces:**
- Consumes: hai user hiện có thuộc `STAFF`/`SHIPPER`, một admin `@ActorId`, schema `StaffPayRate` và `WorkShift`.
- Produces: 2 pay rates và 9 shifts gồm scheduled, pending attendance và approved/calculated attendance.

- [ ] **Step 1: Chọn user hiện có, không tạo tài khoản giả**

```sql
DECLARE @StaffId int=(SELECT TOP(1) user_id FROM dbo.Users WHERE role_name='STAFF' AND status='ACTIVE' ORDER BY user_id);
DECLARE @ShipperId int=(SELECT TOP(1) user_id FROM dbo.Users WHERE role_name='SHIPPER' AND status='ACTIVE' ORDER BY user_id);
IF @StaffId IS NULL OR @ShipperId IS NULL THROW 51808, 'Presentation seed requires active STAFF and SHIPPER users', 1;
DECLARE @ExpectedDemoShifts int=9;
DECLARE @ExpectedPayRates int=2;
```

- [ ] **Step 2: Xóa đúng shifts/pay rates do seed sở hữu**

Nhận diện shifts bằng `attendance_note LIKE N'DEMO-PRES-SHIFT%'` và pay rates bằng exact `(user_id,effective_from)` trong ngày seed sở hữu. Xóa `CodSettlement` liên quan trước shifts; không xóa shift lịch thật.

- [ ] **Step 3: Upsert hai pay rates deterministic**

Dùng `effective_from=DATEADD(day,-30,CAST(@Now AS date))`, update nếu đã tồn tại, insert nếu chưa tồn tại; rate STAFF và SHIPPER khác nhau, đều dương.

- [ ] **Step 4: Insert 9 shifts hợp lệ**

Tạo ba ca STAFF hôm nay theo exact fixed times, ba ca lịch tương lai, và ba ca lịch sử cho STAFF/SHIPPER. Chỉ shift lịch sử approved dùng:

```sql
attendance_status='APPROVED',
approved_minutes=240,
approved_overtime_minutes=30,
pay_snapshot_status='CALCULATED',
regular_pay_amount=regular_hourly_rate_snapshot*4,
overtime_pay_amount=overtime_hourly_rate_snapshot*.5,
total_pay_amount=regular_pay_amount+overtime_pay_amount
```

Shift pending giữ toàn bộ approval/pay snapshot fields `NULL`.

- [ ] **Step 5: Validator khóa exact counts và constraints**

Kiểm tra 9 shifts, 2 effective pay rates, ít nhất một `PENDING`, một `APPROVED/CALCULATED`, fixed STAFF times và tổng lương khớp.

### Task 4: Bổ sung đối soát COD gắn với shifts

**Files:**
- Modify: `database/seed_presentation_demo.sql`
- Modify: `database/seed_presentation_demo_validate.sql`

**Interfaces:**
- Consumes: `@ShipperId`, bốn demo shifts lịch sử, COD delivered orders và `CodSettlement` constraints.
- Produces: 4 settlements gồm `SUBMITTED`, `SETTLED`, `SHORT`, `OVER`.

- [ ] **Step 1: Khai báo expectation và ownership key**

```sql
DECLARE @ExpectedCodSettlements int=4;
```

Dùng shifts có `attendance_note LIKE N'DEMO-PRES-SHIFT-COD-%'`; chuỗi `DEMO-PRES-COD` xuất hiện trong `reason` của trạng thái SHORT/OVER để validator và cleanup nhận diện.

- [ ] **Step 2: Insert 4 settlements đúng constraint**

- `SUBMITTED`: các trường verification `NULL`.
- `SETTLED`: `verified_amount=submitted_amount`, có `received_by`, `verified_at`.
- `SHORT`: `verified_amount<submitted_amount`, reason không rỗng.
- `OVER`: `verified_amount>submitted_amount`, reason không rỗng.

`expected_amount` phải bằng tổng `cod_collected_amount` của order COD được gắn cho shipper/shift hoặc một aggregate deterministic đã được validator đối chiếu.

- [ ] **Step 3: Validator khóa exact status set**

```sql
IF (SELECT COUNT(*) FROM dbo.CodSettlement c JOIN dbo.WorkShift w ON w.shift_id=c.shift_id WHERE w.attendance_note LIKE N'DEMO-PRES-SHIFT-COD-%')<>@ExpectedCodSettlements THROW 51926, 'Expected presentation COD settlements', 1;
IF (SELECT COUNT(DISTINCT c.status) FROM dbo.CodSettlement c JOIN dbo.WorkShift w ON w.shift_id=c.shift_id WHERE w.attendance_note LIKE N'DEMO-PRES-SHIFT-COD-%')<>4 THROW 51927, 'Expected all COD settlement states', 1;
```

- [ ] **Step 4: Chạy targeted policy test và xác nhận GREEN**

Run: `mvn -Dtest=PresentationDemoSeedPolicyTest test`

Expected: 1 test, 0 failures.

### Task 5: Chứng minh idempotency trên DemoDatabase

**Files:**
- Verify: `database/seed_presentation_demo.sql`
- Verify: `database/seed_presentation_demo_validate.sql`

**Interfaces:**
- Consumes: exact repository seed/validator.
- Produces: hai vòng kết quả giống nhau trên `DuckJo/DemoDatabase`.

- [ ] **Step 1: Preflight read-only**

Run:

```powershell
sqlcmd -S DuckJo -E -C -b -Q "SELECT @@SERVERNAME,DB_NAME(),state_desc,compatibility_level FROM sys.databases WHERE name IN('DemoDatabase','FastGuyDB')"
```

Expected: server `DuckJo`; both targets `ONLINE`, compatibility `160`.

- [ ] **Step 2: Tạo wrapper session-context trong approved temp directory**

Tạo `C:\Users\NamPhong\AppData\Local\Temp\opencode\seed-demo-wrapper.sql` bằng cách prepend:

```sql
EXEC sys.sp_set_session_context @key=N'FASTGUY_ALLOW_PRESENTATION_DEMO_SEED',@value=1;
```

Không sửa target guard trong repository file.

- [ ] **Step 3: Chạy seed + validator hai lần**

Run cho mỗi vòng:

```powershell
sqlcmd -S DuckJo -E -C -b -V 16 -f 65001 -d DemoDatabase -i <seed-wrapper>
sqlcmd -S DuckJo -E -C -b -V 16 -f 65001 -d DemoDatabase -i database/seed_presentation_demo_validate.sql
```

Expected mỗi vòng: validator in `Presentation demo seed validation passed`; exact counts không tăng.

- [ ] **Step 4: Dừng nếu bất kỳ validator nào lỗi**

Không tạo hoặc chạy bản retained-target khi một vòng disposable không thành công.

### Task 6: Áp dụng bản đã kiểm chứng lên FastGuyDB

**Files:**
- Temporary only: `C:\Users\NamPhong\AppData\Local\Temp\opencode\seed-retained-wrapper.sql`
- Temporary only: `C:\Users\NamPhong\AppData\Local\Temp\opencode\validate-retained.sql`

**Interfaces:**
- Consumes: byte-current repository seed/validator đã qua Task 5 và phê duyệt retained write hiện tại.
- Produces: dữ liệu mẫu toàn nghiệp vụ trên `DuckJo/FastGuyDB` cùng bằng chứng validator/read-only.

- [ ] **Step 1: Xác nhận identity ngay trước write**

Run:

```powershell
sqlcmd -S DuckJo -E -C -d FastGuyDB -b -Q "SELECT @@SERVERNAME,DB_NAME(),state_desc,compatibility_level FROM sys.databases WHERE name=DB_NAME()"
```

Expected: `DuckJo`, `FastGuyDB`, `ONLINE`, `160`; nếu lệch phải dừng.

- [ ] **Step 2: Tạo bản tạm target-locked**

Thay exact guard/message `DemoDatabase` → `FastGuyDB` trong memory và prepend session context. Không ghi bản rewritten vào repository.

- [ ] **Step 3: Chạy retained seed và validator**

Run:

```powershell
sqlcmd -S DuckJo -E -C -b -V 16 -f 65001 -d FastGuyDB -i <retained-seed-wrapper>
sqlcmd -S DuckJo -E -C -b -V 16 -f 65001 -d FastGuyDB -i <retained-validator>
```

Expected: transaction commits và validator passes.

- [ ] **Step 4: Chạy exact consumer queries read-only**

Xác nhận:

- 7 revenue rows từ `SUM(final_amount)` với `DELIVERED/PAID`.
- `revenue_today > 0`.
- `completion_rate_today > 0`.
- ít nhất 5 top-product rows.
- 3 refund rows.
- 4 COD settlement statuses.
- 9 demo shifts và 2 pay rates.
- warehouse receipt/count/ledger validator vẫn pass.

- [ ] **Step 5: Xóa toàn bộ temp wrappers**

Dùng exact paths; không chạy wildcard cleanup hoặc `git clean`.

### Task 7: Kiểm tra ứng dụng và toàn bộ test suite

**Files:**
- Verify all modified backend/frontend/OpenAPI files already in working tree.

**Interfaces:**
- Consumes: current working tree và updated `FastGuyDB`.
- Produces: fresh quality evidence trước commit.

- [ ] **Step 1: Backend full test**

Run: `mvn test`

Workdir: `Backend/FastGuy-FastFoodSite`

Expected: 571 tests, 0 failures/errors.

- [ ] **Step 2: Frontend unit/build/contract checks**

Run:

```powershell
npm test
npm run build
npm run contract:lint
```

Workdir: `frontend`

Expected: 769 tests pass; build passes; OpenAPI valid, only documented pre-existing warnings.

- [ ] **Step 3: Focused Chromium desktop**

Run:

```powershell
$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:65535'
npx playwright test tests/e2e/checkout-payment-shipping.spec.js --project=desktop-chrome
```

Expected: all focused mocked UI tests pass. If authenticated live runtime credentials are unavailable, report real-browser dashboard inspection as a remaining runtime gap rather than fabricating evidence.

- [ ] **Step 4: Git whitespace and scope review**

Run:

```powershell
git diff --check
git status --short --branch
git diff --stat
git diff --name-only
```

Expected: no whitespace errors; only exact owned files selected later.

### Task 8: Commit exact completed groups into local main

**Files:**
- Commit A: `database/seed_presentation_demo.sql`, `database/seed_presentation_demo_validate.sql`, `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`, `docs/superpowers/plans/2026-09-03-fastguy-presentation-demo-data.md`
- Commit B: exact 15 API/backend/frontend defect-fix files listed by `git diff --name-only`, including focused E2E test and OpenAPI.

**Interfaces:**
- Consumes: green verification evidence from Task 7.
- Produces: two reviewable commits on local `main` after existing spec commit `620ba48`.

- [ ] **Step 1: Update this plan to reflect final executed counts**

Change only concrete expected evidence if actual suite counts legitimately changed; do not add narrative artifacts.

- [ ] **Step 2: Stage and inspect Commit A exact paths**

```powershell
git add -- database/seed_presentation_demo.sql database/seed_presentation_demo_validate.sql Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java docs/superpowers/plans/2026-09-03-fastguy-presentation-demo-data.md
git diff --cached --check
git diff --cached --stat
git diff --cached
```

- [ ] **Step 3: Commit presentation data**

```powershell
git commit -m "feat(database): expand presentation demo data"
```

- [ ] **Step 4: Stage only defect-fix paths**

Stage exact OpenAPI, five backend implementation files, three backend tests, three frontend views, three frontend tests. Confirm no `.agents`, `.hermes`, broad `docs/`, diagrams or user documents are staged.

- [ ] **Step 5: Commit operational fixes**

```powershell
git commit -m "fix(operations): close checkout payment and shift defects"
```

- [ ] **Step 6: Final local-main proof**

Run:

```powershell
git status --short --branch
git log --oneline -5
git diff HEAD~2..HEAD --stat
```

Expected: commits are on `main`; unrelated untracked artifacts remain untouched; no push unless separately requested.
