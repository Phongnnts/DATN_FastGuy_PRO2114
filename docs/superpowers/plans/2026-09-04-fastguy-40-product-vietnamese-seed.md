# FastGuy 40-Product Vietnamese Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the owned presentation dataset with 40 naturally named Vietnamese products, meaningful two-ingredient recipes, and consistent receipt/count inventory evidence.

**Architecture:** Keep the existing transactional seed and validator, but migrate ownership recognition from old `DEMO-PRES-*` markers to newly created `FG-*` markers. Declare product, ingredient, and recipe mappings explicitly in table variables so category distribution and recipe semantics are deterministic and validator-enforced.

**Tech Stack:** SQL Server T-SQL, JUnit 5 source-policy tests, Maven.

## Global Constraints

- Repository seed and validator remain hard-locked to exact `DemoDatabase`.
- Do not execute against `FastGuyDB`.
- Do not change schema, backend runtime code, API, OpenAPI, or frontend.
- Create exactly 40 products distributed `6-6-6-6-6-5-5` across the seven approved categories.
- Create exactly 40 variants named `Tiêu chuẩn`, 40 active recipes, and 80 recipe lines.
- New owned identifiers use `FG-*`; no newly created identifier or visible field may contain `DEMO`.
- Preserve unrelated uncommitted frontend work and protected untracked files.

---

### Task 1: Lock the new dataset policy in tests

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`

**Interfaces:**
- Consumes: `database/seed_presentation_demo.sql`, `database/seed_presentation_demo_validate.sql` as source text.
- Produces: regression policy for counts, prefixes, distribution, explicit recipes, receipt/count evidence, and target locks.

- [ ] Add failing assertions requiring `@ExpectedProducts int=40`, `@ExpectedRecipeLines int=80`, `FG-SKU-`, `FG-ING-`, `FG-REC-`, category counts `6-6-6-6-6-5-5`, and `N'Tiêu chuẩn'`.
- [ ] Add failing assertions rejecting new inserts with `DEMO-PRES-` and requiring validator checks for no `DEMO` in new owned identifiers/visible content.
- [ ] Add a failing assertion that recipe mappings are explicit and enforce exactly two distinct ingredients per recipe rather than modulo assignment.
- [ ] Run `mvn "-Dtest=PresentationDemoSeedPolicyTest" test`; expect policy failures against the current 20-product seed.

### Task 2: Rewrite products, ingredients, and recipes

**Files:**
- Modify: `database/seed_presentation_demo.sql`

**Interfaces:**
- Produces: 7 owned categories, 40 products, 40 variants, ingredient catalog, 40 recipes, 80 recipe items.
- Ownership cleanup recognizes both `DEMO-PRES-*` and `FG-*`; inserts only `FG-*`.

- [ ] Update cleanup table variables and FK-safe deletes to identify old and new owned products, variants, ingredients, receipts, counts, and orders.
- [ ] Replace `@Products` with 40 fully accented natural Vietnamese dishes distributed `6-6-6-6-6-5-5`.
- [ ] Generate variants `FG-SKU-001` through `FG-SKU-040`, each named `Tiêu chuẩn` and using `INGREDIENT` mode.
- [ ] Replace generic 20-ingredient generation with an explicit Vietnamese ingredient catalog using `FG-ING-nnn`, valid units, positive stock/cost/minimum values.
- [ ] Add an explicit recipe mapping table `(product_n, line_no, ingredient_n, quantity)` with two different ingredients per product and positive quantities meaningful for their units.
- [ ] Insert 40 active recipes and 80 mapped recipe items.
- [ ] Run `mvn "-Dtest=PresentationDemoSeedPolicyTest" test`; expect remaining failures only for validator/warehouse sections not yet updated.

### Task 3: Rewrite receipt, stock count, orders, and validator

**Files:**
- Modify: `database/seed_presentation_demo.sql`
- Modify: `database/seed_presentation_demo_validate.sql`

**Interfaces:**
- Consumes: all `FG-*` ownership and dataset tables from Task 2.
- Produces: idempotent receipt/count/order evidence and strict validation for the complete dataset.

- [ ] Change owned order, payment, history, receipt and related technical references to non-DEMO `FG-*` prefixes while retaining cleanup compatibility with old rows.
- [ ] Update order generation to cycle through all 40 products without changing valid payment/refund/status diversity.
- [ ] Create approved `FG-REC-001` receipt lines and matching `RECEIPT` transactions for every owned ingredient.
- [ ] Create approved stock-count rows for every owned ingredient plus at least one mathematically consistent adjustment transaction.
- [ ] Update counts to 40 products, 40 variants, 40 recipes and 80 recipe lines.
- [ ] Add validator assertions for category distribution, one `Tiêu chuẩn` variant per product, exactly two distinct recipe ingredients, non-DEMO identifiers/content, and receipt/count consistency.
- [ ] Run `mvn "-Dtest=PresentationDemoSeedPolicyTest" test`; require pass.

### Task 4: Validate on disposable DemoDatabase

**Files:**
- Verify: `database/seed_presentation_demo.sql`
- Verify: `database/seed_presentation_demo_validate.sql`

**Interfaces:**
- Requires confirmed SQL Server identity and exact `DB_NAME() = 'DemoDatabase'` before each write.

- [ ] Query `@@SERVERNAME`, `DB_NAME()`, database state, compatibility level, migration 065 presence, and relevant table/index/column catalog using read-only commands.
- [ ] Stop if target is not exact `DemoDatabase`, schema differs, or migration 065 is absent.
- [ ] With user-approved disposable write scope, set session context and run seed plus validator once.
- [ ] Run seed plus validator a second time to prove idempotency.
- [ ] Query exact category distribution, product/variant/recipe counts, ingredient count, receipt lines, stock count lines, and scan all owned identifiers/content for `DEMO`.
- [ ] Do not execute any command against `FastGuyDB`.

### Task 5: Full verification and review

**Files:**
- Verify only the SQL scripts and policy test changed; preserve existing frontend diff.

- [ ] Run `mvn test`; require zero failures.
- [ ] Run `git diff --check`.
- [ ] Inspect `git status --short`, `git diff -- database/seed_presentation_demo.sql database/seed_presentation_demo_validate.sql Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`, and confirm no unrelated file was modified by this task.
- [ ] Do not commit, push, migrate, seed retained data, or stage unrelated files unless explicitly requested.
