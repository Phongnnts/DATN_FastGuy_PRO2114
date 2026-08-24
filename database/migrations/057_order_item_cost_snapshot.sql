SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test',N'FastGuyDB_Inventory057_Test') THROW 51000, '057 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='057_order_item_cost_snapshot')
BEGIN
    IF COL_LENGTH(N'dbo.OrderItem',N'unit_cost_snapshot') IS NULL OR COL_LENGTH(N'dbo.OrderItem',N'total_cost_snapshot') IS NULL THROW 51000, '057 history exists but schema is incomplete', 1;
    PRINT '057_order_item_cost_snapshot already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.OrderItem',N'U') IS NULL THROW 51000, 'OrderItem table missing', 1;
    IF COL_LENGTH(N'dbo.OrderItem',N'unit_cost_snapshot') IS NOT NULL OR COL_LENGTH(N'dbo.OrderItem',N'total_cost_snapshot') IS NOT NULL THROW 51000, '057 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:057_order_item_cost_snapshot',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'057 migration lock unavailable',1;
        DECLARE @order_item_count_before bigint=(SELECT COUNT_BIG(*) FROM dbo.OrderItem);
        ALTER TABLE dbo.OrderItem ADD unit_cost_snapshot decimal(18,2) NULL,total_cost_snapshot decimal(18,2) NULL;
        EXEC sys.sp_executesql N'ALTER TABLE dbo.OrderItem ADD CONSTRAINT CK_OrderItem_CostSnapshot CHECK((unit_cost_snapshot IS NULL AND total_cost_snapshot IS NULL) OR (unit_cost_snapshot>=0 AND total_cost_snapshot>=0));';
        IF (SELECT COUNT_BIG(*) FROM dbo.OrderItem)<>@order_item_count_before THROW 51000,'057 changed OrderItem row count',1;
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('057_order_item_cost_snapshot',N'Immutable nullable cost snapshots for new order items');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
