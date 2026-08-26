# Ingredient Inventory Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển FastGuy sang tồn kho nguyên liệu theo công thức, có reserve tại checkout, consume khi bắt đầu chế biến, hỗ trợ tồn thành phẩm trong giai đoạn chuyển đổi và suy ra khả năng phục vụ cho admin/khách.

**Architecture:** Thêm aggregate `InventoryItem + Recipe` làm nguồn sự thật mới, duy trì dual-mode tại `ProductVariant` để migration không khóa menu. Mọi thay đổi kho dùng `BigDecimal`, khóa pessimistic theo item ID, persist balance và ledger trong cùng transaction. Triển khai bắt buộc theo thứ tự DATABASE → API → FRONTEND.

**Tech Stack:** SQL Server/T-SQL, Jakarta Servlet, JPA/Hibernate, Java 17, OpenAPI 3.1, Vue 3 Composition API, Pinia, Vitest/Node tests, Playwright.

## Global Constraints

- Một cửa hàng, một kho logic; chưa thêm `location`.
- Đơn vị chuẩn chỉ gồm `G`, `ML`, `PIECE`; lưu `DECIMAL(19,4)` và Java `BigDecimal`, không dùng `double`.
- Không thêm dependency mới.
- `ProductVariant.quantity_available` chưa xóa; chỉ là compatibility source cho `FINISHED_GOOD` trong rollout.
- Checkout reserve; `PREPARING` consume; hủy trước chế biến release; hủy sau consume không hoàn kho.
- Không làm lot/HSD, FEFO, supplier, kiểm kê, variance, giá vốn, báo cáo food cost hoặc prepared batch.
- OpenAPI là nguồn chuẩn; cập nhật contract trước servlet/client.
- Mọi migration chỉ chạy sau preflight read-only, trên DB disposable hoặc có phê duyệt retained-data và recovery plan riêng.
- Không commit nếu người dùng chưa yêu cầu rõ.

## File Map

### Database

- Create `database/migrations/052_ingredient_inventory_phase_1.sql`: schema, constraints, indexes, backfill dual-mode.
- Create `database/migrations/052_validate.sql`: catalog và data validator.
- Modify `database/init.sql`: canonical fresh schema.
- Modify `database/DB_FastGuy.sql`: canonical alternate schema.
- Modify `database/seed_demo.sql`: seed inventory items, mappings và recipes mẫu.

### API contract

- Modify `openapi/fastguy.yaml`: inventory item, receipt/adjustment, recipe, availability schemas/operations.
- Add/modify contract policy tests dưới `Backend/FastGuy-FastFoodSite/src/test/java/service/` và `frontend/test/` theo pattern hiện có.

### Backend persistence/domain

- Create `entity/InventoryItem.java`: balance và đơn vị chuẩn.
- Create `entity/VariantInventoryItem.java`: mapping thành phẩm.
- Create `entity/Recipe.java`, `entity/RecipeItem.java`: BOM theo variant.
- Modify `entity/ProductVariant.java`: `inventoryMode`.
- Modify `entity/InventoryReservation.java`: reservation cấp order.
- Create `entity/InventoryReservationItem.java`: reservation lines.
- Modify `entity/InventoryTransaction.java`: ledger theo item và decimal.
- Modify `src/main/resources/META-INF/persistence.xml`: đăng ký entity nếu persistence unit dùng explicit class list.

### Backend services/API

- Create `dao/InventoryItemDAO.java`, `dao/RecipeDAO.java`; modify `dao/InventoryTransactionDAO.java`.
- Create `service/InventoryAvailabilityService.java`: demand aggregation và servings.
- Rewrite `service/InventoryReservationService.java`: reserve/consume/release theo item.
- Create `service/InventoryItemService.java`, `service/RecipeService.java`; adapt `service/InventoryAdjustmentService.java`.
- Modify `service/OrderService.java`, `service/OrderTransitionService.java`, `service/StaffOrderService.java`: lifecycle integration.
- Create `servlet/AdminInventoryItemServlet.java`, `servlet/AdminRecipeServlet.java`.
- Modify `servlet/AdminInventoryAdjustmentServlet.java`, `servlet/AdminInventoryServlet.java`.
- Modify product/menu serialization path identified by CodeGraph during each task to expose derived availability only.

### Frontend

- Modify `frontend/src/api/index.js` and/or existing admin API module used by `adminApi`.
- Modify `frontend/src/stores/admin.js` if inventory state is currently centralized there.
- Rewrite `frontend/src/views/admin/InventoryPage.vue` as inventory overview/items.
- Modify `frontend/src/views/admin/InventoryLedgerPage.vue` to item filters.
- Create `frontend/src/views/admin/RecipesPage.vue`.
- Modify `frontend/src/router/index.js` and admin sidebar/layout component located by CodeGraph.
- Modify `frontend/src/views/admin/ProductEditorPage.vue` and `components/admin/product-editor/ProductVariantsSection.vue` for mode/link only.
- Modify `frontend/src/utils/stockPolicy.js`, `productMapper.js`, `components/common/ProductCard.vue`, menu/detail/cart checkout consumers.

---

### Task 1: Freeze OpenAPI Contract

**Files:**
- Modify: `openapi/fastguy.yaml`
- Test: existing OpenAPI/removed-domain policy tests; add `frontend/test/ingredient-inventory-contract.test.js` if no schema-focused test exists

**Interfaces:**
- Produces schemas `InventoryItem`, `Recipe`, `RecipeItem`, `InventoryAvailability`, `InventoryTransactionPage`, `InventoryConflict`.
- Produces operations for `/api/admin/inventory/items`, `/api/admin/inventory/items/{itemId}`, receipts, adjustments, ledger, recipe and availability.

- [ ] **Step 1: Add a failing contract policy test**

Assert exact operation paths, enums and decimal fields:

```js
assert.match(spec, /inventoryMode:[\s\S]*INGREDIENT[\s\S]*FINISHED_GOOD[\s\S]*UNTRACKED[\s\S]*SUSPENDED/);
assert.match(spec, /baseUnit:[\s\S]*G[\s\S]*ML[\s\S]*PIECE/);
assert.match(spec, /\/api\/admin\/inventory\/items:/);
assert.match(spec, /\/api\/admin\/product-variants\/\{variantId\}\/recipe:/);
```

- [ ] **Step 2: Run the focused test and confirm failure**

Run the repository’s existing Node contract-policy command or:

```powershell
node --test frontend/test/ingredient-inventory-contract.test.js
```

Expected: FAIL because paths/schemas do not exist.

- [ ] **Step 3: Add minimal OpenAPI operations and schemas**

Define request/response fields exactly as approved:

```yaml
InventoryItemQuantity:
  type: number
  format: decimal
  minimum: 0
  multipleOf: 0.0001
InventoryMode:
  type: string
  enum: [INGREDIENT, FINISHED_GOOD, UNTRACKED, SUSPENDED]
BaseUnit:
  type: string
  enum: [G, ML, PIECE]
```

Adjustment/receipt requests use `inventoryItemId`, decimal `quantity`, `expectedOnHandQuantity`, reason and note. Customer availability exposes only `availabilityStatus` and optional `remainingServings`.

- [ ] **Step 4: Run contract checks**

```powershell
npm run contract:lint
node --test frontend/test/ingredient-inventory-contract.test.js
```

Expected: PASS.

### Task 2: Add SQL Server Schema and Safe Backfill

**Files:**
- Create: `database/migrations/052_ingredient_inventory_phase_1.sql`
- Create: `database/migrations/052_validate.sql`
- Modify: `database/init.sql`
- Modify: `database/DB_FastGuy.sql`
- Modify: `database/seed_demo.sql`

**Interfaces:**
- Produces tables `InventoryItem`, `VariantInventoryItem`, `Recipe`, `RecipeItem`, `InventoryReservationItem`.
- Alters `ProductVariant.inventory_mode`, `InventoryReservation`, `InventoryTransaction` per approved schema.

- [ ] **Step 1: Run read-only preflight**

Confirm `@@SERVERNAME`, `DB_NAME()`, state, compatibility, current columns/FKs/indexes for `ProductVariant`, `InventoryReservation`, `InventoryTransaction`, `Orders`, `OrderItem`. Stop on target mismatch.

- [ ] **Step 2: Write validator first**

`052_validate.sql` must `THROW` when:

```sql
IF COL_LENGTH('dbo.ProductVariant', 'inventory_mode') IS NULL
    THROW 51000, '052 validation failed: inventory_mode missing', 1;
IF EXISTS (SELECT 1 FROM dbo.InventoryItem WHERE on_hand_quantity < 0 OR reserved_quantity < 0 OR reserved_quantity > on_hand_quantity)
    THROW 51000, '052 validation failed: invalid inventory balance', 1;
IF EXISTS (SELECT 1 FROM dbo.ProductVariant WHERE inventory_mode NOT IN ('INGREDIENT','FINISHED_GOOD','UNTRACKED','SUSPENDED'))
    THROW 51000, '052 validation failed: invalid inventory mode', 1;
```

Also assert every variant with pre-migration non-null quantity has exactly one `FINISHED_GOOD` mapping and matching on-hand quantity.

- [ ] **Step 3: Write idempotent migration**

Use named constraints and indexes. Required checks:

```sql
CHECK (base_unit IN ('G','ML','PIECE'))
CHECK (on_hand_quantity >= 0)
CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity)
CHECK (quantity > 0) -- RecipeItem and reservation item
UNIQUE (variant_id)
UNIQUE (recipe_id, inventory_item_id)
UNIQUE (reservation_id, inventory_item_id)
```

Backfill active stopped variants to `SUSPENDED`, non-null stock to `FINISHED_GOOD`, others to `UNTRACKED`. Create one item/mapping per managed variant without deleting old stock.

- [ ] **Step 4: Update canonical schema and demo seed**

Mirror exact names, types, defaults, FKs and indexes in both canonical SQL files. Seed at least one ingredient recipe and one finished-good variant, using base-unit quantities.

- [ ] **Step 5: Validate on disposable database**

Follow `database/migrations/RUNBOOK.md`; run migration then validator. Query parity against canonical SQL. Expected: validator prints `052 validation passed`; existing order and user rows unchanged.

### Task 3: Map JPA Entities and Decimal Invariants

**Files:**
- Create/modify entities listed in File Map
- Modify: `Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/entity/IngredientInventoryMappingTest.java`

**Interfaces:**
- Produces `InventoryItem.availableQuantity(): BigDecimal`.
- Produces `ProductVariant.getInventoryMode()`.
- Produces reservation aggregate with `List<InventoryReservationItem>`.

- [ ] **Step 1: Write failing mapping tests**

Assert table/column annotations, decimal Java types, unique constraints and allowed state helpers. Include:

```java
assertEquals(new BigDecimal("3.8000"), item.availableQuantity());
assertThrows(IllegalStateException.class, () -> item.reserve(new BigDecimal("6.0000")));
```

- [ ] **Step 2: Run focused test**

```powershell
mvn -Dtest=IngredientInventoryMappingTest test
```

Expected: compilation/test failure because entities do not exist.

- [ ] **Step 3: Implement minimal entities**

Use `BigDecimal.compareTo`, `add`, `subtract`; normalize to scale 4 where values enter services. Keep entity methods limited to balance invariants:

```java
public BigDecimal availableQuantity() {
    return onHandQuantity.subtract(reservedQuantity);
}
```

Do not add generic repository abstractions.

- [ ] **Step 4: Run mapping tests and schema bootstrap test**

```powershell
mvn -Dtest=IngredientInventoryMappingTest test
mvn -DskipTests package
```

Expected: PASS.

### Task 4: Implement Recipe and Availability Domain

**Files:**
- Create: `dao/InventoryItemDAO.java`, `dao/RecipeDAO.java`
- Create: `service/InventoryAvailabilityService.java`
- Test: `service/InventoryAvailabilityServiceTest.java`

**Interfaces:**
- Consumes `ProductVariant.inventoryMode`, recipe and item balances.
- Produces:

```java
Map<Integer, BigDecimal> aggregateDemand(EntityManager em, Map<Integer, Integer> variantQuantities)
AvailabilityResult availability(EntityManager em, int variantId)
```

`AvailabilityResult` contains mode, status, nullable servings and nullable limiting item ID for admin-only use.

- [ ] **Step 1: Write failing tests**

Cover limiting ingredient, shared ingredient aggregation, decimal quantities, `FINISHED_GOOD`, `UNTRACKED`, `SUSPENDED`, missing/inactive recipe.

```java
assertEquals(30, result.servings());
assertEquals("LOW_STOCK", low.status());
assertEquals(new BigDecimal("0.2400"), demand.get(beefId));
```

- [ ] **Step 2: Run focused test and confirm failure**

```powershell
mvn -Dtest=InventoryAvailabilityServiceTest test
```

- [ ] **Step 3: Implement minimal DAO queries and service**

Fetch recipes/items in bounded queries, aggregate with `Map.merge`, compute floor using `divide(..., 0, RoundingMode.FLOOR)`. Reject ingredient variants without active non-empty recipe.

- [ ] **Step 4: Run focused tests**

Expected: all availability cases PASS.

### Task 5: Replace Variant Reservation with Item Reservation

**Files:**
- Rewrite: `service/InventoryReservationService.java`
- Modify: `service/OrderService.java`, `service/OrderTransitionService.java`, `service/StaffOrderService.java`
- Test: `service/InventoryReservationPolicyTest.java`
- Test: `service/OrderMutationConcurrencyTest.java`
- Add: `service/IngredientOrderInventoryTest.java`

**Interfaces:**
- Consumes `aggregateDemand` from Task 4.
- Produces:

```java
void reserve(EntityManager em, Orders order, Map<Integer, Integer> variantQuantities)
boolean consume(EntityManager em, Orders order)
boolean release(EntityManager em, Orders order)
```

- [ ] **Step 1: Rewrite tests to express approved lifecycle**

Assertions:

```java
checkout -> reserved increases, onHand unchanged
PREPARING -> reserved decreases, onHand decreases
cancel before PREPARING -> reserved decreases, onHand unchanged
cancel after PREPARING -> no inventory reversal
```

Add mixed-cart and two-checkout concurrency tests; require no negative available quantity and one reservation per order.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
mvn -Dtest=InventoryReservationPolicyTest,IngredientOrderInventoryTest,OrderMutationConcurrencyTest test
```

- [ ] **Step 3: Implement reservation aggregate**

Sort item IDs, lock each with `PESSIMISTIC_WRITE`, validate all before mutation, then update balances, persist lines and signed ledger. Keep order and inventory mutation in one transaction.

- [ ] **Step 4: Wire exact order transitions**

Use CodeGraph immediately before edit to identify checkout creation and transition-to-`PREPARING` call sites. Remove direct variant decrement/increment paths. Preserve unrelated payment, status-history and scheduler behavior.

- [ ] **Step 5: Run focused and full backend tests**

```powershell
mvn -Dtest=InventoryReservationPolicyTest,IngredientOrderInventoryTest,OrderMutationConcurrencyTest,OrderTransitionServiceTest,OrderCancellationPolicyTest test
mvn test
```

Expected: PASS; no old variant reservation behavior remains for new orders.

### Task 6: Add Admin Inventory and Recipe APIs

**Files:**
- Create/modify services, DAOs and servlets listed in File Map
- Modify: `dao/InventoryTransactionDAO.java`
- Test: `servlet/AdminInventoryItemServletTest.java`
- Test: `servlet/AdminRecipeServletTest.java`
- Modify: existing inventory servlet/service tests

**Interfaces:**
- Implements OpenAPI operations from Task 1 exactly.
- Produces API DTO maps with decimal JSON numbers, `onHandQuantity`, `reservedQuantity`, `availableQuantity`.

- [ ] **Step 1: Write failing servlet and service tests**

Cover admin auth, decimal parser rejection, unit enum, immutable used base unit, stale expected quantity 409, atomic recipe replacement, mode transition guard and item ledger filters.

- [ ] **Step 2: Run focused tests**

Expected: FAIL because endpoints/services are absent.

- [ ] **Step 3: Implement item CRUD and receipt/adjustment**

Validate strings and `BigDecimal` at servlet boundary. `RECEIPT` must be positive. Adjustment uses `INCREASE`, `DECREASE`, `SET`, expected on-hand and no-op suppression. Persist balance plus ledger atomically.

- [ ] **Step 4: Implement recipe replacement and availability**

`PUT` validates all item IDs, active status, positive quantities and duplicates before deleting/replacing lines. Mode `INGREDIENT` requires valid recipe; switching away does not delete recipe.

- [ ] **Step 5: Adapt ledger query**

Replace variant/product filters with `inventoryItemId`, `orderId`, type and dates. Allowed types: `RECEIPT`, `RESERVE`, `RELEASE`, `CONSUME`, `ADJUSTMENT`.

- [ ] **Step 6: Run contract and backend tests**

```powershell
npm run contract:lint
mvn test
mvn -DskipTests package
```

Expected: PASS.

### Task 7: Expose Derived Customer Availability

**Files:**
- Modify exact product/menu serialization servlet/service/DAO found by CodeGraph
- Modify product availability contract tests
- Test: `service/ProductAvailabilitySerializationTest.java`

**Interfaces:**
- Adds `availabilityStatus` and optional `remainingServings` to product variants.
- Never exposes limiting item, on-hand or reserved quantities publicly.

- [ ] **Step 1: Write failing serialization tests**

Test `0`, `1..3`, `>=4`, untracked and suspended mappings. Assert serialized public response excludes `inventoryItemId`, `onHandQuantity`, `reservedQuantity`, `limitingItemId`.

- [ ] **Step 2: Run focused test and confirm failure**

- [ ] **Step 3: Add bounded availability hydration**

Batch-load recipes/items for listed variant IDs to avoid per-card N+1 queries. Add public fields only; retain compatibility fields only where existing admin contracts still require them.

- [ ] **Step 4: Run focused and backend tests**

Expected: PASS.

### Task 8: Build Admin Inventory UI

**Files:**
- Modify/create frontend API, pages, router/sidebar and product editor files from File Map
- Create: `frontend/src/utils/inventoryItem.js`
- Test: `frontend/test/inventory-item.test.js`
- Test: `frontend/test/admin-ingredient-inventory-policy.test.js`

**Interfaces:**
- Consumes Task 6 API only.
- Produces routes for inventory overview/items, recipes and ledger.

- [ ] **Step 1: Write failing utility/policy tests**

Test decimal formatting without floating-point arithmetic, available calculation display, low-stock state, payload field names, route/sidebar entries, and absence of embedded BOM editor in product form.

- [ ] **Step 2: Run focused frontend tests and confirm failure**

```powershell
node --test frontend/test/inventory-item.test.js frontend/test/admin-ingredient-inventory-policy.test.js
```

- [ ] **Step 3: Add API methods matching OpenAPI**

Methods: list/create/update item, receipt, adjustment, transaction list, get/replace recipe, get availability. Do not infer response fields beyond contract.

- [ ] **Step 4: Rewrite InventoryPage**

Show supported phase-1 KPIs only: item count, below-minimum count, unavailable variants, recent transactions. Table columns: item, type, on-hand, reserved, available, unit, minimum, status. Add create, receipt, adjustment and ledger actions with accessible dialogs and stale-conflict handling.

- [ ] **Step 5: Add RecipesPage**

Select variant, edit whole recipe, prevent duplicate item and non-positive quantity, show servings and limiting ingredient for admin. Link from variant editor; editor owns only mode selection.

- [ ] **Step 6: Update ledger, router and sidebar**

Ledger filters item/order/type/date. Sidebar entries: Tổng quan kho, Nguyên liệu/Công thức định lượng, Sổ kho; avoid adding phase-2 pages.

- [ ] **Step 7: Run frontend tests and build**

```powershell
npm test
npm run build
```

Expected: PASS.

### Task 9: Update Customer Stock UX and Checkout Conflict

**Files:**
- Modify: `frontend/src/utils/stockPolicy.js`, `productMapper.js`
- Modify: `frontend/src/components/common/ProductCard.vue`
- Modify menu/detail/cart checkout consumers located by CodeGraph
- Test: existing stock/product card/menu tests
- Modify: `frontend/tests/e2e/menu-filter-polish.spec.js`
- Add: `frontend/tests/e2e/ingredient-inventory.spec.js`

**Interfaces:**
- Consumes public availability fields from Task 7.
- Maps status to Vietnamese copy and CTA state.

- [ ] **Step 1: Write failing mapping tests**

Exact rules:

```text
IN_STOCK/UNTRACKED = Còn hàng
LOW_STOCK + N = Chỉ còn N phần
OUT_OF_STOCK/SUSPENDED = Tạm hết; CTA disabled
```

Checkout HTTP 409 must refresh cart/menu availability and show actionable out-of-stock message without automatic retry.

- [ ] **Step 2: Run focused tests and confirm failure**

- [ ] **Step 3: Implement minimal mapper and UI changes**

Use server status as source of truth. Do not derive ingredient stock client-side or show exact inventory quantities.

- [ ] **Step 4: Run frontend tests/build**

```powershell
npm test
npm run build
```

- [ ] **Step 5: Run desktop/mobile Playwright**

Cover admin create item, receipt, recipe save, customer low/out-of-stock rendering, concurrent checkout conflict, order transition consume and pre-preparing cancellation release. Verify no console errors and principal API requests return expected 2xx/409.

### Task 10: Migration Rollout and Final Verification

**Files:**
- Review all changed files; no new production code unless a failing check identifies a scoped defect.

**Interfaces:**
- Validates complete DATABASE → API → FRONTEND capability.

- [ ] **Step 1: Re-run SQL preflight and disposable migration**

Apply `052`, run `052_validate.sql`, verify row counts and backfill. Never execute against retained data without separate approval.

- [ ] **Step 2: Run all required checks**

```powershell
mvn test
mvn -DskipTests package
npm run contract:lint
npm test
npm run build
git diff --check
```

Run from each project’s correct working directory. Expected: all exit code 0.

- [ ] **Step 3: Run full relevant Playwright suite desktop/mobile**

Expected: all inventory, menu and checkout scenarios pass; no console errors; main requests successful.

- [ ] **Step 4: Review blast radius with CodeGraph**

Confirm no caller still mutates `ProductVariant.quantityAvailable` directly for `INGREDIENT`, no old reservation code writes variant stock, all DTO/API consumers match OpenAPI, and phase-2 functionality was not added.

- [ ] **Step 5: Report evidence**

Report migration target/validator output, backend/frontend/contract/E2E counts, changed schema/API surfaces and any remaining untracked artifacts. Do not commit or push without explicit user request.
