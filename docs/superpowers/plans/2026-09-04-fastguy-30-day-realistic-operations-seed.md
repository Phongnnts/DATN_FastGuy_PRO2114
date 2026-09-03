# FastGuy 30-Day Realistic Operations Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and validate one idempotent 30-day operational seed covering every active FastGuy role and business domain on DemoDatabase.

**Architecture:** Extend the existing presentation seed in dependency order: ownership cleanup, identities/catalog, customer engagement, orders/payments, workforce/delivery, warehouse, then finance/audit. Keep one transaction and one strict validator so partial operational states cannot commit.

**Tech Stack:** SQL Server T-SQL, JUnit 5 source-policy tests, Maven, sqlcmd UTF-8.

## Global Constraints

- Exact target is `DuckJo/DemoDatabase`; never write `FastGuyDB`.
- 2 ADMIN, 6 STAFF, 4 SHIPPER, 20 customers, 40 products, 2–3 variants per product, approximately 180 orders across 30 days.
- All visible content is natural Vietnamese with full diacritics; no numbered placeholder names.
- All owned technical keys use `FG-OPS-*`; no secrets or real personal data.
- Preserve current uncommitted frontend and earlier seed work.
- No schema/API/frontend changes.

---

### Task 1: Catalog runtime schema and add failing source policies

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/PresentationDemoSeedPolicyTest.java`
- Modify: `database/seed_presentation_demo.sql`
- Modify: `database/seed_presentation_demo_validate.sql`

- [ ] Query DemoDatabase catalog for exact columns, nullability, defaults, FKs and constraints for all seeded tables.
- [ ] Add failing policy assertions for role counts, customer data, 2–3 variants, modifiers, 180 orders, reviews, shifts/pay/COD, receipts/counts, finance/assets/banners/activity logs.
- [ ] Run the focused policy test and confirm it fails for missing operational coverage.

### Task 2: Ownership cleanup, identities and catalog

- [ ] Extend exact ownership capture and FK-safe cleanup for all `FG-OPS-*` rows and legacy owned rows.
- [ ] Insert 2 ADMIN, 6 STAFF, 4 SHIPPER and 20 customers with natural names, safe emails, valid phones/status/roles and password hashes compatible with current auth.
- [ ] Insert customer addresses and optional profile data using valid TP.HCM hierarchy identifiers already accepted by current schema.
- [ ] Expand 40 products to 2–3 natural sizes each with exact prices, SKUs, shipping dimensions and statuses.
- [ ] Add appropriate modifier groups/options and variant-scaled recipes.
- [ ] Add validator checks for role/customer/catalog completeness and uniqueness.

### Task 3: Customer engagement and commerce

- [ ] Insert favorites, carts/cart items/modifiers, coupons, claimed/used coupon rows and loyalty balances/history.
- [ ] Generate approximately 180 orders over inclusive 30 local dates with varied natural customers, addresses, notes, item counts and modifiers.
- [ ] Insert exact order items, cost snapshots, payment attempts, reservations and complete chronological status histories.
- [ ] Cover valid order/payment/refund/failure states without impossible timestamp combinations.
- [ ] Insert natural product-scoped reviews only for delivered orders, with realistic rating distribution and homepage consent.
- [ ] Validate arithmetic, references, state timelines, reviews, coupons and loyalty.

### Task 4: Workforce, delivery and COD

- [ ] Generate 30-day STAFF/SHIPPER schedules with multiple workers per slot and no duplicate user/date/code.
- [ ] Populate checked-in/out, attendance approval, regular/overtime minutes, pay-rate histories and snapshots consistently.
- [ ] Assign active/delivered orders to compatible staff/shipper shifts and preserve one-active-delivery rules.
- [ ] Generate COD settlement states with reconcilable amounts and verifier/timestamps.
- [ ] Validate shift, attendance, pay, assignment and COD invariants.

### Task 5: Warehouse, finance, merchandising and audit

- [ ] Generate multiple approved receipts with complete ingredient lines and average-cost evidence.
- [ ] Generate consumption linked to delivered/operational orders, waste, approved stock counts and bounded adjustments with exact before/after math.
- [ ] Insert operating expenses, fixed assets and lifecycle values allowed by schema.
- [ ] Insert active/inactive banners and category merchandising with natural copy.
- [ ] Insert ActivityLog rows only for concrete seeded objects/actions with safe metadata.
- [ ] Add validator summaries and strict checks for every domain.

### Task 6: Disposable execution and full verification

- [ ] Confirm `@@SERVERNAME`, `DB_NAME()`, ONLINE, compatibility 160, migration history and catalog immediately before writes.
- [ ] Run focused policy tests, then `mvn test`.
- [ ] Execute seed and validator twice with `sqlcmd -b -f 65001` on DemoDatabase only.
- [ ] Query role/domain counts, 30-day coverage, financial totals and orphan checks.
- [ ] Run `git diff --check`; inspect only intended SQL/test changes.
- [ ] Do not commit, push or execute against FastGuyDB without a separate later request and recovery gate.
