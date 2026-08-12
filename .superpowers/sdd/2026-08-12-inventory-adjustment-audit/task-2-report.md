# Task 2 report

## Status

Implemented audited stock mutations for existing product and variant editor flows. New variant creation retains direct initial quantity assignment.

## TDD evidence

### RED

Command:

`mvn "-Dtest=InventoryAdjustmentPolicyTest,AdminVariantServletPolicyTest" test`

Result: build failed at test compilation because `InventoryAdjustmentService.setManagedQuantity(...)` did not exist. This proved nullable managed-stock behavior was absent.

### GREEN

Focused command:

`mvn "-Dtest=InventoryAdjustmentPolicyTest,AdminVariantServletPolicyTest" test`

Result: 5 tests run, 0 failures, 0 errors, BUILD SUCCESS.

Full backend command:

`mvn test`

Result: 174 tests run, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS.

## Changed files

- `Backend/FastGuy-FastFoodSite/src/main/java/service/InventoryAdjustmentService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminProductServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminVariantServlet.java`
- `Backend/FastGuy-FastFoodSite/src/test/java/service/InventoryAdjustmentPolicyTest.java`
- `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AdminVariantServletPolicyTest.java`

## Behavior

- Added `setManagedQuantity(...)` with pessimistic locking, expected nullable quantity conflict detection, audit validation, no-op handling, rollback, and nullable before/after ledger values.
- Existing product and variant updates delegate changed `quantityAvailable` values to inventory adjustment service.
- Audit payload fields are required only when quantity changes.
- New variant creation still writes initial quantity during its single insert.
- Nullable conflict data uses a mutable map because `Map.of` rejects null values.

## Self-review

- `git diff --check` passed.
- Search found one direct editor `setQuantityAvailable(...)` call, limited to new variant creation.
- No frontend file was changed by Task 2 work.
- Existing unrelated `frontend/package-lock.json` change remains unstaged.

## Concerns

- Product and variant metadata persistence remains separate from inventory service transaction, matching existing DAO structure. A later conflict can leave in-memory metadata mutations on the request entity, but no metadata save occurs after conflict response.
- Existing compiler warnings in `JwtUtil` and `OrdersDAO` remain unchanged.

## Commit

Initial commit: `9eb0646 fix(inventory): audit editor stock changes`.

## Reviewer fix: atomic metadata and stock persistence

### Root cause

Existing editor flow called `setManagedQuantity(...)`, committed stock and ledger, then called `ProductDAO.saveVariant(v)`. `saveVariant` opened another transaction and merged a detached entity carrying stale `quantityAvailable`, allowing audited stock overwrite and partial persistence when metadata save failed.

### RED

Command:

`mvn "-Dtest=InventoryAdjustmentPolicyTest" test`

Result: test compilation failed because atomic `setManagedQuantity(..., ProductVariant metadata)` did not exist. Added runtime proxy cases require metadata to update locked managed entity without `merge`, and require ledger failure to roll back the shared transaction.

### GREEN

Focused command:

`mvn "-Dtest=InventoryAdjustmentPolicyTest,AdminVariantServletPolicyTest" test`

Result: 7 tests run, 0 failures, 0 errors, BUILD SUCCESS.

Full backend command:

`mvn test`

Result: 176 tests run, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS.

### Fix

- Stock-changing editor requests now copy metadata onto pessimistically locked managed variant inside inventory service transaction.
- Stock, metadata, and ledger commit or roll back together.
- Stock-changing paths no longer call detached `ProductDAO.saveVariant(v)` after inventory commit.
- Metadata-only updates retain existing DAO save path.
- New variant creation remains unchanged.

### Verification

- `git diff --check` passed.
- Runtime proxy tests verify no `EntityManager.merge` call and rollback on ledger persistence failure.
- Existing unrelated `frontend/package-lock.json` change remains unstaged.

Reviewer fix commit: pending.
