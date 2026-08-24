SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test') THROW 51000, '056 validator target database is not approved', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='056_verify_inventory_text_repair_balances') THROW 51000, '056 migration history missing', 1;
IF EXISTS(SELECT 1 FROM dbo.InventoryItem WHERE inventory_code BETWEEN 'INV-000167' AND 'INV-000198' AND (name IS NULL OR on_hand_quantity<0 OR minimum_quantity<0)) THROW 51000,'056 inventory invariant mismatch',1;
IF EXISTS(SELECT i.inventory_item_id FROM dbo.InventoryItem i LEFT JOIN dbo.InventoryTransaction t ON t.inventory_item_id=i.inventory_item_id AND t.reason_code='OPENING_BALANCE' AND t.reference_type='MIGRATION' AND t.reference_id='053' WHERE i.inventory_code BETWEEN 'INV-000167' AND 'INV-000198' GROUP BY i.inventory_item_id HAVING COUNT_BIG(t.inventory_transaction_id)<>1) THROW 51000,'056 opening row cardinality mismatch',1;
PRINT '056 validation passed';
