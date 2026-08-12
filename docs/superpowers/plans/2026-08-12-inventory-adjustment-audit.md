# Audited Inventory Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm thao tác tăng, giảm, đặt mới với conflict HTTP 409 và bảo đảm mọi thay đổi tồn kho quản trị đi qua ledger audit.

**Architecture:** Mở rộng `InventoryAdjustmentService` thành đường ghi tồn kho quản trị duy nhất, giữ pessimistic lock và endpoint hiện có. Trang tồn kho gửi operation cùng snapshot `expectedQuantity`; product/variant editor ủy quyền mọi thay đổi `quantityAvailable` cho cùng service. Frontend giữ component hiện có, nâng modal thành ba tab và xử lý conflict không tự retry.

**Tech Stack:** Java 17, Jakarta Servlet/JPA, JUnit 5, Vue 3 Composition API, Axios, Node test runner, Vite 8, SQL Server.

## Global Constraints

- Tồn kho tiếp tục quản lý tại `ProductVariant.quantityAvailable`.
- Chỉ nhận operation `INCREASE`, `DECREASE`, `SET`.
- `OTHER` bắt buộc note không rỗng; note tối đa 500 ký tự.
- Stale stock trả HTTP 409 và không ghi stock hoặc ledger.
- No-op trả `changed: false` và không tạo ledger.
- Mọi mutation quản trị của `quantityAvailable` phải qua inventory service.
- Chuyển `null` và managed quantity chỉ thực hiện trong editor biến thể.
- Không thay đổi waste, reservation, consume, release hoặc transaction type `RETURN`.
- Không thêm dependency, abstraction dùng một lần hoặc migration nếu schema hiện tại đủ.

---

### Task 1: Backend Adjustment Contract and Conflict Handling

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/exception/InventoryConflictException.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryAdjustmentService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminInventoryAdjustmentServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryAdjustmentPolicyTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminInventoryAdjustmentServletTest.java`

**Interfaces:**
- Produces: `Map<String, Object> adjust(int variantId, String operation, int quantity, Integer expectedQuantity, String reasonCode, String note, int adminId)`.
- Produces: `InventoryConflictException(int variantId, Integer currentQuantity)` with getters used by servlet.
- HTTP body consumes `variantId`, `operation`, `quantity`, `expectedQuantity`, `reasonCode`, `note`.
- HTTP 409 body exposes `variantId` and `currentQuantity` through `ApiResponse` data.

- [ ] **Step 1: Add failing service policy tests**

Replace source assertions tied to `newQuantity` with checks for exact operation contract, expected quantity comparison, whitelist, `OTHER` note rule, no-op branch, overflow-safe arithmetic, and conflict exception:

```java
@Test
void adjustmentSupportsThreeOperationsWithExpectedQuantity() throws Exception {
    String src = read(SERVICE);
    assertTrue(src.contains("\"INCREASE\""));
    assertTrue(src.contains("\"DECREASE\""));
    assertTrue(src.contains("\"SET\""));
    assertTrue(src.contains("expectedQuantity"));
    assertTrue(src.contains("InventoryConflictException"));
    assertTrue(src.contains("Math.addExact"));
    assertTrue(src.contains("changed\", false"));
}

@Test
void adjustmentValidatesReasonAndOtherNote() throws Exception {
    String src = read(SERVICE);
    assertTrue(src.contains("STOCK_COUNT"));
    assertTrue(src.contains("DAMAGE"));
    assertTrue(src.contains("EXPIRED"));
    assertTrue(src.contains("OTHER"));
    assertTrue(src.contains("Ghi chú là bắt buộc khi chọn lý do Khác"));
}
```

- [ ] **Step 2: Add failing runtime servlet tests**

Create a servlet test following existing Mockito-free servlet proxy conventions from `AddressValidationPolicyTest`. Exercise valid JSON payload mapping and conflict mapping:

```java
@Test
void staleExpectedQuantityReturns409WithCurrentQuantity() throws Exception {
    InventoryConflictException conflict = new InventoryConflictException(12, 27);
    assertEquals(12, conflict.getVariantId());
    assertEquals(27, conflict.getCurrentQuantity());
    String src = Files.readString(SERVLET);
    assertTrue(src.contains("catch (InventoryConflictException e)"));
    assertTrue(src.contains("409"));
    assertTrue(src.contains("currentQuantity"));
}

@Test
void adjustmentPayloadUsesOperationQuantityAndExpectedQuantity() throws Exception {
    String src = Files.readString(SERVLET);
    assertTrue(src.contains("body.get(\"operation\")"));
    assertTrue(src.contains("body.get(\"quantity\")"));
    assertTrue(src.contains("body.get(\"expectedQuantity\")"));
    assertFalse(src.contains("body.get(\"newQuantity\")"));
}
```

- [ ] **Step 3: Run focused backend tests and verify RED**

Run:

```powershell
mvn -Dtest=InventoryAdjustmentPolicyTest,AdminInventoryAdjustmentServletTest test
```

Expected: FAIL because operation contract, exception, 409 mapping, and no-op behavior do not exist.

- [ ] **Step 4: Implement conflict exception and minimal service contract**

Implement exception:

```java
package exception;

public class InventoryConflictException extends RuntimeException {
    private final int variantId;
    private final Integer currentQuantity;

    public InventoryConflictException(int variantId, Integer currentQuantity) {
        super("Tồn kho đã thay đổi, vui lòng kiểm tra lại");
        this.variantId = variantId;
        this.currentQuantity = currentQuantity;
    }

    public int getVariantId() { return variantId; }
    public Integer getCurrentQuantity() { return currentQuantity; }
}
```

In `adjust`, validate reason against a static set, enforce `OTHER` note, lock variant, compare `expectedQuantity`, calculate with `Math.addExact`, reject negative results, return no-op before creating `InventoryTransaction`, and otherwise persist positive absolute delta with before/after. Catch `ArithmeticException` and expose a stable `IllegalArgumentException` message.

- [ ] **Step 5: Update servlet parsing and HTTP 409 mapping**

Parse integers exactly rather than narrowing arbitrary `Number` values. Invoke new service signature. Map conflict:

```java
} catch (InventoryConflictException e) {
    ApiResponse.error(resp, e.getMessage(), 409, Map.of(
        "variantId", e.getVariantId(),
        "currentQuantity", e.getCurrentQuantity()
    ));
}
```

Use existing `ApiResponse` overload if present; if absent, add smallest overload accepting data without changing existing callers.

- [ ] **Step 6: Run focused and full backend tests**

Run:

```powershell
mvn -Dtest=InventoryAdjustmentPolicyTest,AdminInventoryAdjustmentServletTest test
mvn test
```

Expected: focused tests PASS; full backend suite PASS.

- [ ] **Step 7: Commit backend adjustment contract**

```powershell
git add -- Backend/FastGuy-FastFoodSite/src/main/java/exception/InventoryConflictException.java Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryAdjustmentService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminInventoryAdjustmentServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryAdjustmentPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminInventoryAdjustmentServletTest.java
git commit -m "feat(inventory): add conflict-safe stock adjustments"
```

---

### Task 2: Route Product and Variant Stock Changes Through Audit Service

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryAdjustmentService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminProductServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminVariantServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryAdjustmentPolicyTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminVariantServletPolicyTest.java`

**Interfaces:**
- Consumes: Task 1 conflict and reason validation.
- Produces: `Map<String, Object> setManagedQuantity(int variantId, Integer newQuantity, Integer expectedQuantity, String reasonCode, String note, int adminId)`.
- Editor payload consumes `expectedQuantity`, `reasonCode`, `note` whenever `quantityAvailable` changes.

- [ ] **Step 1: Add failing policy tests for audited editor mutations**

```java
@Test
void editorStockChangesDelegateToInventoryService() throws Exception {
    String product = Files.readString(Path.of("src/main/java/servlet/AdminProductServlet.java"));
    String variant = Files.readString(Path.of("src/main/java/servlet/AdminVariantServlet.java"));
    assertTrue(product.contains("setManagedQuantity("));
    assertTrue(variant.contains("setManagedQuantity("));
    assertFalse(product.contains("setQuantityAvailable(intOf("));
    assertFalse(variant.contains("setQuantityAvailable(intOf("));
}

@Test
void managedModeChangesKeepNullableBeforeAndAfterAudit() throws Exception {
    String src = read(SERVICE);
    assertTrue(src.contains("setManagedQuantity"));
    assertTrue(src.contains("setQuantityBefore(before)"));
    assertTrue(src.contains("setQuantityAfter(newQuantity)"));
}
```

- [ ] **Step 2: Run focused test and verify RED**

Run:

```powershell
mvn -Dtest=InventoryAdjustmentPolicyTest,AdminVariantServletPolicyTest test
```

Expected: FAIL because editor servlets still set quantity directly.

- [ ] **Step 3: Implement nullable managed-mode mutation**

Add `setManagedQuantity` using same lock, expected-value comparison, reason validation, no-op behavior, ledger persist, and transaction rollback. For ledger quantity use absolute delta when both values are numbers, non-null value when one side is null, and `1` for `null ↔ 0` marker. Keep before/after nullable.

- [ ] **Step 4: Remove direct stock writes from editor flows**

Capture original and requested `quantityAvailable`; persist metadata without mutating stock directly; invoke `setManagedQuantity` only when values differ. Require `expectedQuantity`, `reasonCode`, and `note` only for stock changes. Preserve existing create behavior for a brand-new variant because no prior stock exists to audit; creation remains one insert, not an adjustment.

- [ ] **Step 5: Run focused and full backend tests**

Run:

```powershell
mvn -Dtest=InventoryAdjustmentPolicyTest,AdminVariantServletPolicyTest test
mvn test
```

Expected: PASS; no direct update path remains for existing variant stock.

- [ ] **Step 6: Commit audited editor backend flow**

```powershell
git add -- Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryAdjustmentService.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminProductServlet.java Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminVariantServlet.java Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryAdjustmentPolicyTest.java Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminVariantServletPolicyTest.java
git commit -m "fix(inventory): audit editor stock changes"
```

---

### Task 3: Three-Tab Inventory Adjustment Modal

**Files:**
- Modify: `Frontend/src/views/admin/InventoryPage.vue`
- Modify: `Frontend/src/api/admin.js`
- Test: `Frontend/tests/inventory-adjustment-policy.test.mjs`

**Interfaces:**
- Consumes: Task 1 body `{ operation, quantity, expectedQuantity, reasonCode, note }`.
- Consumes: HTTP 409 response data `{ variantId, currentQuantity }`.
- Produces: one accessible modal with tab IDs `adjust-increase`, `adjust-decrease`, `adjust-set`.

- [ ] **Step 1: Add failing frontend contract tests**

Extend `inventory-adjustment-policy.test.mjs`:

```js
test('adjustment modal supports operation tabs and expected stock', () => {
  assert.match(inventoryPage, /INCREASE/);
  assert.match(inventoryPage, /DECREASE/);
  assert.match(inventoryPage, /SET/);
  assert.match(inventoryPage, /expectedQuantity/);
  assert.match(inventoryPage, /role="tablist"/);
  assert.match(inventoryPage, /role="tabpanel"/);
});

test('stale conflict refreshes snapshot without automatic retry', () => {
  assert.match(inventoryPage, /currentQuantity/);
  assert.match(inventoryPage, /409/);
  assert.doesNotMatch(inventoryPage, /submitAdjust\(\)/);
});

test('OTHER requires note', () => {
  assert.match(inventoryPage, /reasonCode === 'OTHER'/);
  assert.match(inventoryPage, /Ghi chú là bắt buộc/);
});
```

- [ ] **Step 2: Run focused frontend test and verify RED**

Run:

```powershell
npm test -- tests/inventory-adjustment-policy.test.mjs
```

Expected: FAIL because three tabs and expected quantity are absent.

- [ ] **Step 3: Implement operation state and preview calculation**

Replace `newQuantity` form with:

```js
const adjustmentForm = ref({
  operation: 'INCREASE',
  quantity: '',
  reasonCode: 'STOCK_COUNT',
  note: '',
});

const projectedQuantity = computed(() => {
  if (!adjustmentRow.value) return null;
  const quantity = Number(adjustmentForm.value.quantity);
  if (!Number.isInteger(quantity)) return null;
  if (adjustmentForm.value.operation === 'INCREASE') return adjustmentRow.value.stock + quantity;
  if (adjustmentForm.value.operation === 'DECREASE') return adjustmentRow.value.stock - quantity;
  return quantity;
});
```

Validate positive delta for increase/decrease, nonnegative target for set, no negative projection, and required note for `OTHER`.

- [ ] **Step 4: Implement accessible three-tab modal**

Use buttons with `role="tab"`, `aria-selected`, `aria-controls`, keyboard Left/Right/Home/End handling, one `role="tabpanel"`, linked labels, projected stock `aria-live="polite"`, error `role="alert"`, focus trap, Escape close, and restore focus to triggering button. Reuse existing CSS tokens; add no dependency.

- [ ] **Step 5: Send new payload and handle conflict**

Send:

```js
await adminApi.adjustInventory(adjustmentRow.value.variantId, {
  operation: adjustmentForm.value.operation,
  quantity,
  expectedQuantity: adjustmentRow.value.stock,
  reasonCode: adjustmentForm.value.reasonCode,
  note: adjustmentForm.value.note.trim(),
});
```

On HTTP 409, read Axios response data shape used by existing client, replace `adjustmentRow.value.stock` with `currentQuantity`, set an inline conflict message, and return without closing or retrying.

- [ ] **Step 6: Run focused and full frontend verification**

Run:

```powershell
npm test -- tests/inventory-adjustment-policy.test.mjs
npm test
npm run build
```

Expected: all frontend tests PASS and Vite build succeeds.

- [ ] **Step 7: Commit modal UI**

```powershell
git add -- Frontend/src/views/admin/InventoryPage.vue Frontend/src/api/admin.js Frontend/tests/inventory-adjustment-policy.test.mjs
git commit -m "feat(inventory): add three-mode adjustment modal"
```

---

### Task 4: Product Editor Audited Stock UX

**Files:**
- Modify: `Frontend/src/components/admin/product-editor/ProductVariantsSection.vue`
- Modify: relevant editor submit owner found from imports of `ProductVariantsSection.vue`
- Test: `Frontend/tests/inventory-adjustment-policy.test.mjs`

**Interfaces:**
- Consumes: Task 2 editor fields `expectedQuantity`, `reasonCode`, `note` for existing stock changes.
- Produces: explicit managed-stock toggle and audit fields only when stock changes.

- [ ] **Step 1: Locate submit owner and add exact file to task execution notes**

Run:

```powershell
rg -n "ProductVariantsSection|quantityAvailable" Frontend/src/views/admin Frontend/src/components/admin/product-editor
```

Expected: one product editor owner assembling variant payload. Modify that existing owner only; do not create a wrapper.

- [ ] **Step 2: Add failing frontend tests for editor contract**

```js
test('variant editor exposes managed stock audit fields', () => {
  assert.match(variantSection, /quantityAvailable/);
  assert.match(variantSection, /reasonCode/);
  assert.match(variantSection, /note/);
  assert.match(variantSection, /expectedQuantity/);
  assert.match(variantSection, /Quản lý tồn kho/);
});
```

Also assert audit fields render conditionally only after existing quantity changes.

- [ ] **Step 3: Run focused test and verify RED**

Run:

```powershell
npm test -- tests/inventory-adjustment-policy.test.mjs
```

Expected: FAIL because editor lacks audit reason and expected snapshot.

- [ ] **Step 4: Implement managed-stock editor state**

For existing variants, retain original nullable quantity as `expectedQuantity`. Show explicit toggle. On change, require reason; require note for `OTHER`; confirm disabling managed stock. Include audit fields in update payload only when quantity changes. New variants keep current creation contract.

- [ ] **Step 5: Handle editor conflict without retry**

On HTTP 409, keep editor open, update original snapshot from `currentQuantity`, show inline message, and require user resubmit. Do not auto-merge metadata after conflict.

- [ ] **Step 6: Run frontend tests and build**

Run:

```powershell
npm test -- tests/inventory-adjustment-policy.test.mjs
npm test
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit editor UX**

```powershell
git add -- Frontend/src/components/admin/product-editor/ProductVariantsSection.vue Frontend/src/views/admin Frontend/tests/inventory-adjustment-policy.test.mjs
git commit -m "feat(inventory): audit product editor stock changes"
```

Stage only submit owner identified in Step 1, not whole directory if unrelated files changed.

---

### Task 5: Cross-Stack Verification and Backlog Closure

**Files:**
- Modify: `docs/product-backlog.md`
- Modify: `docs/release-backlog.md`
- Modify: `docs/sprint-backlog.md` only where inventory adjustment status exists.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verified feature and synchronized backlog status.

- [ ] **Step 1: Run fresh backend verification**

```powershell
mvn test
mvn package -DskipTests
```

Working directory: `Backend/FastGuy-FastFoodSite`.

Expected: all tests PASS and package reports `BUILD SUCCESS`.

- [ ] **Step 2: Run fresh frontend verification**

```powershell
npm test
npm run build
```

Working directory: `Frontend`.

Expected: all tests PASS and Vite build succeeds. No lint command exists in current `package.json`; do not invent one.

- [ ] **Step 3: Verify diff and stock mutation coverage**

```powershell
rg -n "setQuantityAvailable\(" Backend/FastGuy-FastFoodSite/src/main/java
rg -n "quantityAvailable" Frontend/src/views/admin Frontend/src/components/admin/product-editor
git diff --check
git status --short
```

Expected: order/reservation services may mutate stock for order lifecycle; admin product/variant paths delegate existing-stock changes to inventory service. Diff check emits no output.

- [ ] **Step 4: Update backlog status only after verification passes**

Change manual inventory adjustment entries from `Mới` to `Xong` and add concise evidence referencing endpoint, ledger audit, conflict handling, backend tests, and frontend tests. Do not alter unrelated stale backlog entries.

- [ ] **Step 5: Commit verification-backed backlog closure**

```powershell
git add -- docs/product-backlog.md docs/release-backlog.md docs/sprint-backlog.md
git commit -m "docs(inventory): close audited adjustment backlog"
```

- [ ] **Step 6: Final clean verification**

```powershell
git diff --check HEAD~5..HEAD
git status --short --branch
```

Expected: no whitespace errors and no uncommitted feature files.
