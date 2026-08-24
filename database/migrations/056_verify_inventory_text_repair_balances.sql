SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test') THROW 51000, '056 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run migration history preflight first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='056_verify_inventory_text_repair_balances')
BEGIN
    PRINT '056_verify_inventory_text_repair_balances already applied';
END
ELSE
BEGIN
    IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='055_verify_inventory_text_repair') THROW 51000, 'Run migration 055 first', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @Expected TABLE(inventory_code varchar(30) PRIMARY KEY,on_hand decimal(19,4) NOT NULL,minimum_quantity decimal(19,4) NOT NULL);
        INSERT @Expected VALUES
        ('INV-000167',100000,12000),('INV-000168',50000,6000),('INV-000169',600,80),('INV-000170',400,50),('INV-000171',400,50),('INV-000172',500,60),('INV-000173',80000,10000),('INV-000174',70000,8000),
        ('INV-000175',30000,3500),('INV-000176',1200,150),('INV-000177',35000,4000),('INV-000178',70000,8000),('INV-000179',100000,12000),('INV-000180',25000,3000),('INV-000181',500,60),('INV-000182',45000,5000),
        ('INV-000183',80000,10000),('INV-000184',1000,120),('INV-000185',120000,15000),('INV-000186',900,100),('INV-000187',50000,6000),('INV-000188',60000,7000),('INV-000189',40000,5000),('INV-000190',35000,4000),
        ('INV-000191',30000,3500),('INV-000192',30000,3500),('INV-000193',40000,5000),('INV-000194',30000,3500),('INV-000195',70000,8000),('INV-000196',90000,10000),('INV-000197',60000,7000),('INV-000198',700,80);

        IF EXISTS(SELECT 1 FROM @Expected e LEFT JOIN dbo.InventoryItem i ON i.inventory_code=e.inventory_code WHERE i.inventory_item_id IS NULL OR i.on_hand_quantity<>e.on_hand OR i.minimum_quantity<>e.minimum_quantity) THROW 51000,'056 retained quantity evidence mismatch',1;
        IF EXISTS(SELECT 1 FROM @Expected e LEFT JOIN dbo.InventoryItem i ON i.inventory_code=e.inventory_code OUTER APPLY(SELECT COUNT_BIG(*) row_count,MIN(t.quantity) quantity,MIN(t.quantity_before) quantity_before,MIN(t.quantity_after) quantity_after FROM dbo.InventoryTransaction t WHERE t.inventory_item_id=i.inventory_item_id AND t.reason_code='OPENING_BALANCE' AND t.reference_type='MIGRATION' AND t.reference_id='053') opening WHERE opening.row_count<>1 OR opening.quantity<>e.on_hand OR opening.quantity_before<>0 OR opening.quantity_after<>e.on_hand) THROW 51000,'056 opening balance evidence mismatch',1;
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('056_verify_inventory_text_repair_balances',N'Verify retained ingredient quantities, thresholds, and opening ledger rows against pre-054 backup evidence');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
