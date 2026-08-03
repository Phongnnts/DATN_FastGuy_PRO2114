# Canonical Database Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cung cap mot `database/init.sql` duy nhat tao lai day du `FastGuyDB` va du lieu demo cho thanh vien nhom.

**Architecture:** `database/init.sql` drop/recreate cho local/demo; chuoi `database/migrations/000..040` expand/backfill/enforce/validate database hien co va giu legacy trong cutover. Entity JPA va `persistence.xml` la checklist doi chieu.

**Tech Stack:** SQL Server, T-SQL, sqlcmd, Java 17, JPA/Hibernate.

## Global Constraints

- `database/init.sql` chi danh cho local/demo; script drop va tao lai `FastGuyDB`, khong bao toan du lieu cu.
- Database hien co phai dung `database/migrations/RUNBOOK.md`, backup/stop writes va chay `000`, `010`, `020`, `030`, `040` theo thu tu; khong chay `init.sql`.
- Migration bao toan du lieu va giu bang/cot legacy trong cutover; chi archive/remove bang migration duoc phe duyet rieng.
- Fresh init tao du entity tables va `ShippingConfig`; khong tao bang/cot legacy.
- Seed demo day du, khong chua credentials PayOS/GHN.
- Backend `hibernate.hbm2ddl.auto=none`.

---

### Task 1: Canonical schema and seeds

**Files:**
- Modify: `database/init.sql`
- Preserve: `database/migrations/**` cho database hien co

**Interfaces:**
- Produces: T-SQL batch runnable by `sqlcmd -S localhost -E -C -i database/init.sql`.
- Produces: `FastGuyDB` matching every entity listed in `persistence.xml`.

- [ ] **Step 1: Replace init.sql with drop/create header, all tables, FKs, constraints and indexes**

Use deterministic identity IDs for demo rows and dependency-safe table creation order.

- [ ] **Step 2: Add full demo seed**

Seed role accounts, products, variants, modifiers, combo, coupons, banners, shifts, orders, inventory, payment attempts, reviews, support, notifications, loyalty and histories.

- [ ] **Step 3: Validate retained-data migration path**

Giữ chuỗi migration và runbook. Xác nhận migration không drop dữ liệu legacy; destructive cleanup nằm ngoài plan và cần phê duyệt riêng.

- [ ] **Step 4: Validate SQL structure**

Check every `persistence.xml` entity table exists and banned legacy names do not exist.

---

### Task 2: Runtime verification

**Files:**
- Test: `database/init.sql`

**Interfaces:**
- Consumes: canonical script from Task 1.
- Produces: recreated local `FastGuyDB` with valid demo data.

- [ ] **Step 1: Execute init.sql on local SQL Server**

Run available `sqlcmd` binary with Windows authentication. Expected: exit 0 and validation counts.

- [ ] **Step 2: Execute init.sql a second time**

Expected: exit 0 again, proving deterministic recreate behavior.

- [ ] **Step 3: Run backend tests**

Run `mvn clean test` in `Backend/FastGuy-FastFoodSite`. Expected: all tests pass.

- [ ] **Step 4: Run diff validation**

Run `git diff --check`. Expected: no whitespace errors.
