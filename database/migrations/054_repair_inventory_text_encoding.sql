SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test') THROW 51000, '054 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run migration history preflight first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='054_repair_inventory_text_encoding')
BEGIN
    PRINT '054_repair_inventory_text_encoding already applied';
END
ELSE
BEGIN
    IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='053_inventory_costing_stock_count') THROW 51000, 'Run migration 053 first', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:054_repair_inventory_text_encoding',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'054 migration lock unavailable',1;

        DECLARE @Names TABLE(inventory_code varchar(30) PRIMARY KEY,name nvarchar(255) NOT NULL,base_unit varchar(10) NOT NULL);
        INSERT @Names(inventory_code,name,base_unit) VALUES
            ('INV-000167',N'Đá viên','G'),
            ('INV-000168',N'Đường','G'),
            ('INV-000169',N'Bánh burger','PIECE'),
            ('INV-000170',N'Bánh mì','PIECE'),
            ('INV-000171',N'Bánh tortilla','PIECE'),
            ('INV-000172',N'Bánh tráng','PIECE'),
            ('INV-000173',N'Bột mì','G'),
            ('INV-000174',N'Bột pizza','G'),
            ('INV-000175',N'Bacon','G'),
            ('INV-000176',N'Bao bì món ăn','PIECE'),
            ('INV-000177',N'Cà chua','G'),
            ('INV-000178',N'Dầu ăn','ML'),
            ('INV-000179',N'Gạo','G'),
            ('INV-000180',N'Gia vị','G'),
            ('INV-000181',N'Hộp pizza','PIECE'),
            ('INV-000182',N'Hải sản','G'),
            ('INV-000183',N'Khoai tây','G'),
            ('INV-000184',N'Ly và ống hút','PIECE'),
            ('INV-000185',N'Nước pha chế','ML'),
            ('INV-000186',N'Phô mai lát','PIECE'),
            ('INV-000187',N'Phô mai mozzarella','G'),
            ('INV-000188',N'Rau củ hỗn hợp','G'),
            ('INV-000189',N'Rau xà lách','G'),
            ('INV-000190',N'Sữa và kem','ML'),
            ('INV-000191',N'Sốt BBQ','ML'),
            ('INV-000192',N'Sốt burger','ML'),
            ('INV-000193',N'Sốt món Á','ML'),
            ('INV-000194',N'Sốt pizza','ML'),
            ('INV-000195',N'Thịt bò','G'),
            ('INV-000196',N'Thịt gà','G'),
            ('INV-000197',N'Thịt heo','G'),
            ('INV-000198',N'Trứng','PIECE');

        IF EXISTS(SELECT 1 FROM dbo.InventoryItem i JOIN @Names n ON n.inventory_code=i.inventory_code WHERE i.item_type<>'INGREDIENT' OR i.base_unit<>n.base_unit) THROW 51000,'054 authoritative ingredient mapping conflicts with runtime data',1;
        UPDATE i SET name=n.name FROM dbo.InventoryItem i JOIN @Names n ON n.inventory_code=i.inventory_code;
        UPDATE dbo.InventoryTransaction
        SET note=N'Số dư đầu kỳ khi bật tính giá vốn'
        WHERE reason_code='OPENING_BALANCE' AND reference_type='MIGRATION' AND reference_id='053';

        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('054_repair_inventory_text_encoding',N'Repair authoritative Vietnamese inventory names and opening balance notes after sqlcmd code-page corruption');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
