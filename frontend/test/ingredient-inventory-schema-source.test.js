import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('052 migration and validator encode the frozen ingredient inventory schema', () => {
  const migration = read('database/migrations/052_ingredient_inventory_phase_1.sql');
  const validator = read('database/migrations/052_validate.sql');
  for (const table of ['InventoryItem', 'VariantInventoryItem', 'Recipe', 'RecipeItem', 'InventoryReservationItem']) {
    assert.match(migration, new RegExp(`CREATE TABLE dbo\\.${table}`));
    assert.ok(validator.includes(`N'${table}'`), table);
  }
  for (const token of ["base_unit IN('G','ML','PIECE')", 'on_hand_quantity >= 0', 'reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity', 'quantity > 0', 'UQ_VariantInventoryItem_Variant', 'UQ_Recipe_Variant', 'UQ_RecipeItem_RecipeInventoryItem', 'UQ_InventoryReservationItem_ReservationInventoryItem']) assert.ok(migration.includes(token), token);
  assert.ok(validator.includes("THROW 51000, '052 validation failed: inventory_mode missing', 1;"));
  assert.ok(validator.includes("PRINT '052 validation passed'"));
  assert.ok(migration.includes("quantity_available IS NOT NULL THEN ''FINISHED_GOOD''"));
  assert.ok(migration.includes("status <> ''AVAILABLE'' THEN ''SUSPENDED''"));
  for (const token of ['SchemaMigrationHistory', "migration_id = '052_ingredient_inventory_phase_1'", 'sys.foreign_key_columns', 'QUOTENAME', 'legacy_reservations_before', 'legacy_transactions_before', "'WASTE'", "'WASTED'", "'RETURN'", 'CONVERT(decimal(19,4),']) assert.ok(migration.includes(token), token);
  for (const [before, after] of [['INSERT dbo.InventoryReservationItem', 'DROP COLUMN variant_id,quantity'], ['UPDATE t SET inventory_item_id', 'DROP COLUMN variant_id']]) {
    const beforeIndex = migration.indexOf(before);
    const afterIndex = migration.indexOf(after);
    assert.notEqual(beforeIndex, -1, before);
    assert.notEqual(afterIndex, -1, after);
    assert.ok(beforeIndex < afterIndex, `${before} before ${after}`);
  }
  for (const token of ['InventoryReservationLegacyHistory', 'MIN(r.reservation_id)', 'SUM(quantity)', 'ALTER COLUMN order_id int NULL', 'IX_InventoryTransaction_Order', 'IF NOT EXISTS(SELECT 1 FROM sys.indexes']) assert.ok(migration.includes(token), token);
  assert.doesNotMatch(migration, /cannot preserve multiple legacy reservations/);
  for (const token of ['reserved_quantity=balances.reserved_quantity', 'on_hand_quantity=balances.legacy_available+balances.reserved_quantity', 'TRY_CONVERT(decimal(19,4)', '052 inventory balance overflow', 'COUNT(DISTINCT status)>1', '052 mixed reservation statuses per order']) assert.ok(migration.includes(token), token);
  assert.ok(migration.indexOf('052 mixed reservation statuses per order') < migration.indexOf('BEGIN TRANSACTION'));
  for (const token of ['COUNT(DISTINCT h.status)>1', 'r.status<>MIN(h.status)', '052 validation failed: reservation status reconciliation mismatch']) assert.ok(validator.includes(token), token);
  for (const token of ['052 validation failed: reserved balance mismatch', '052 validation failed: legacy reservation history mismatch', 'SUM(ri.quantity)', 'SUM(h.quantity)', 'MIN(h.legacy_reservation_id)', 'i.on_hand_quantity-i.reserved_quantity=CONVERT(decimal(19,4),v.quantity_available)']) assert.ok(validator.includes(token), token);
  for (const alter of ['ALTER TABLE dbo.InventoryReservation ALTER COLUMN created_at datetime2(0) NOT NULL', 'ALTER TABLE dbo.InventoryReservation ALTER COLUMN updated_at datetime2(0) NOT NULL', 'ALTER TABLE dbo.InventoryTransaction ALTER COLUMN created_at datetime2(0) NOT NULL']) {
    assert.ok(migration.includes(alter), alter);
    assert.ok(migration.indexOf(alter) < migration.indexOf('INSERT dbo.SchemaMigrationHistory'), alter);
  }
  assert.match(migration, /EXEC sys\.sp_executesql N'[\s\S]*UPDATE dbo\.ProductVariant SET inventory_mode=/);
  const firstBatch = migration.slice(0, migration.indexOf('GO'));
  for (const option of ['ANSI_NULLS ON', 'QUOTED_IDENTIFIER ON', 'ANSI_PADDING ON', 'ANSI_WARNINGS ON', 'ARITHABORT ON', 'CONCAT_NULL_YIELDS_NULL ON', 'NUMERIC_ROUNDABORT OFF']) assert.ok(firstBatch.includes(`SET ${option};`), option);
  const withoutDynamicSql = migration.replace(/EXEC sys\.sp_executesql N'[^']*(?:''[^']*)*'(?:,[^;]*)?;/g, '');
  assert.doesNotMatch(withoutDynamicSql, /(?:UPDATE dbo\.ProductVariant SET inventory_mode|ALTER TABLE dbo\.ProductVariant ALTER COLUMN inventory_mode|CK_ProductVariant_InventoryMode CHECK\(inventory_mode\)|(?:INSERT|MERGE|UPDATE|DELETE)[^;]*dbo\.(?:InventoryItem|VariantInventoryItem|InventoryReservationLegacyHistory|InventoryReservationItem)|UPDATE t SET inventory_item_id|CREATE INDEX [^;]* ON dbo\.(?:InventoryItem|RecipeItem|InventoryReservationItem)|COUNT_BIG\(\*\) FROM dbo\.InventoryReservationLegacyHistory)/);
  for (const token of ['sys.columns', 'sys.default_constraints', 'sys.check_constraints', 'sys.foreign_keys', 'is_not_trusted', 'sys.indexes', 'sys.index_columns', 'sys.foreign_key_columns', 'parent_object_id', 'referenced_object_id', 'decimal(19,4)', "migration_id = '052_ingredient_inventory_phase_1'", "COL_LENGTH(N'dbo.InventoryReservation',N'variant_id') IS NOT NULL", "COL_LENGTH(N'dbo.InventoryTransaction',N'variant_id') IS NOT NULL", 'IX_InventoryTransaction_Order', "N'InventoryReservationLegacyHistory',N'legacy_reservation_id'", "N'InventoryTransaction',N'note',N'nvarchar',1000", "@no_defaults", "N'InventoryTransaction',N'transaction_type'", "N'InventoryTransaction',N'order_id',N'Orders',N'order_id'", "N'InventoryTransaction',N'created_by',N'Users',N'user_id'", "N'InventoryReservation',N'order_id',N'Orders',N'order_id'"]) assert.ok(validator.includes(token), token);
  assert.ok(validator.includes("WHERE e.table_name<>N'ProductVariant'"));
  assert.ok(validator.includes("N'(',N''),N')',N''"));
  assert.match(validator, /DECLARE @fks TABLE\(name sysname NULL,/);
  const tableVariablesWithNullValues = [...validator.matchAll(/DECLARE @(\w+) TABLE\(([^;]+)\);\s*INSERT @\1 VALUES ([^;]*NULL[^;]*);/g)];
  assert.deepEqual(tableVariablesWithNullValues.map(match => match[1]), ['fks']);
});

test('canonical schemas and demo seed mirror phase 1 inventory', () => {
  for (const path of ['database/init.sql', 'database/DB_FastGuy.sql']) {
    const sql = read(path);
    for (const token of ['inventory_mode varchar(20)', 'CREATE TABLE dbo.InventoryItem', 'CREATE TABLE dbo.VariantInventoryItem', 'CREATE TABLE dbo.Recipe', 'CREATE TABLE dbo.RecipeItem', 'CREATE TABLE dbo.InventoryReservationItem', 'inventory_item_id int', 'IX_InventoryTransaction_ItemCreated']) assert.ok(sql.includes(token), `${path}: ${token}`);
    const reservation = sql.slice(sql.indexOf('CREATE TABLE dbo.InventoryReservation ('), sql.indexOf('CREATE TABLE dbo.InventoryReservationLegacyHistory'));
    const transaction = sql.slice(sql.indexOf('CREATE TABLE dbo.InventoryTransaction ('), sql.indexOf('CREATE TABLE dbo.LoyaltyTransaction'));
    assert.doesNotMatch(reservation, /variant_id/);
    assert.doesNotMatch(transaction, /variant_id/);
  }
  for (const path of ['database/init.sql', 'database/DB_FastGuy.sql', 'database/seed_demo.sql']) {
    const sql = read(path);
    assert.doesNotMatch(sql, /INSERT(?: INTO)? dbo\.InventoryReservation\([^)]*variant_id/i, path);
    assert.doesNotMatch(sql, /INSERT(?: INTO)? dbo\.InventoryTransaction\([^)]*variant_id/i, path);
  }
  const seed = read('database/seed_demo.sql');
  for (const token of ['FG-DEMO Bột mì', 'INGREDIENT', 'FINISHED_GOOD', 'dbo.RecipeItem', 'dbo.VariantInventoryItem']) assert.ok(seed.includes(token), token);
  assert.match(seed, /INSERT dbo\.VariantInventoryItem[\s\S]*FROM dbo\.ProductVariant/);
  assert.match(seed, /INSERT dbo\.InventoryReservationItem[\s\S]*JOIN dbo\.RecipeItem[\s\S]*UNION ALL[\s\S]*FROM dbo\.VariantInventoryItem/);
  assert.match(read('database/migrations/RUNBOOK.md'), /052_ingredient_inventory_phase_1\.sql[\s\S]*052_validate\.sql[\s\S]*rerun/i);
});

test('054 repairs authoritative inventory text and enforces UTF-8 sqlcmd input', () => {
  const migration = read('database/migrations/054_repair_inventory_text_encoding.sql');
  const validator = read('database/migrations/054_validate.sql');
  const wrapper = read('.opencode/skills/sqlserver-migrations/scripts/Invoke-SqlServerMigrationCheck.ps1');
  for (const name of ['Thịt bò', 'Thịt gà', 'Thịt heo', 'Sốt món Á', 'Sữa và kem', 'Rau xà lách']) assert.ok(migration.includes(`N'${name}'`), name);
  assert.ok(migration.includes("N'Số dư đầu kỳ khi bật tính giá vốn'"));
  assert.ok(migration.includes("reason_code='OPENING_BALANCE'"));
  assert.ok(migration.includes("migration_id='054_repair_inventory_text_encoding'"));
  assert.ok(validator.includes("PRINT '054 validation passed'"));
  assert.ok(validator.includes("N'Số dư đầu kỳ khi bật tính giá vốn'"));
  assert.match(wrapper, /'-f', '65001'/);
  assert.match(read('database/migrations/RUNBOOK.md'), /054_repair_inventory_text_encoding\.sql[\s\S]*054_validate\.sql/);
  const audit = read('database/migrations/055_verify_inventory_text_repair.sql');
  const auditValidator = read('database/migrations/055_validate.sql');
  for (const token of ["COUNT_BIG(*) FROM @Expected)<>32", 'i.inventory_item_id IS NULL', 'i.name IS NULL', "migration_id='055_verify_inventory_text_repair'"]) assert.ok(audit.includes(token), token);
  for (const token of ["COUNT_BIG(*) FROM @Expected)<>32", 'i.inventory_item_id IS NULL', 'i.name IS NULL', "PRINT '055 validation passed'"]) assert.ok(auditValidator.includes(token), token);
  assert.match(read('.opencode/skills/sqlserver-migrations/SKILL.md'), /sqlcmd -b -V 16 -f 65001 -i/);
  const balanceAudit = read('database/migrations/056_verify_inventory_text_repair_balances.sql');
  for (const token of ['i.on_hand_quantity<>e.on_hand', 'i.minimum_quantity<>e.minimum_quantity', 'opening.row_count<>1', 'opening.quantity<>e.on_hand', 'opening.quantity_before<>0', 'opening.quantity_after<>e.on_hand']) assert.ok(balanceAudit.includes(token), token);
});
