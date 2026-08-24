SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test',N'FastGuyDB_Inventory057_Test') THROW 51000, '057 validator target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='057_order_item_cost_snapshot') THROW 51000, '057 migration history missing', 1;
IF EXISTS(SELECT 1 FROM (VALUES(N'unit_cost_snapshot'),(N'total_cost_snapshot')) e(column_name) LEFT JOIN sys.columns c ON c.object_id=OBJECT_ID(N'dbo.OrderItem') AND c.name=e.column_name LEFT JOIN sys.types t ON t.user_type_id=c.user_type_id WHERE c.column_id IS NULL OR t.name<>N'decimal' OR c.precision<>18 OR c.scale<>2 OR c.is_nullable<>1) THROW 51000, '057 column mismatch', 1;
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.OrderItem') AND name=N'CK_OrderItem_CostSnapshot' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '057 cost check missing', 1;
IF EXISTS(SELECT 1 FROM dbo.OrderItem WHERE (unit_cost_snapshot IS NULL AND total_cost_snapshot IS NOT NULL) OR (unit_cost_snapshot IS NOT NULL AND total_cost_snapshot IS NULL) OR unit_cost_snapshot<0 OR total_cost_snapshot<0) THROW 51000, '057 snapshot data invalid', 1;
PRINT '057 validation passed';
