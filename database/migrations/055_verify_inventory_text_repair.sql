SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test') THROW 51000, '055 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run migration history preflight first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='055_verify_inventory_text_repair')
BEGIN
    PRINT '055_verify_inventory_text_repair already applied';
END
ELSE
BEGIN
    IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='054_repair_inventory_text_encoding') THROW 51000, 'Run migration 054 first', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @Expected TABLE(inventory_code varchar(30) PRIMARY KEY,name nvarchar(255) NOT NULL,base_unit varchar(10) NOT NULL,on_hand decimal(19,4) NOT NULL,minimum_quantity decimal(19,4) NOT NULL);
        INSERT @Expected VALUES
        ('INV-000167',N'Đá viên','G',100000,12000),('INV-000168',N'Đường','G',50000,6000),('INV-000169',N'Bánh burger','PIECE',600,80),('INV-000170',N'Bánh mì','PIECE',400,50),
        ('INV-000171',N'Bánh tortilla','PIECE',400,50),('INV-000172',N'Bánh tráng','PIECE',500,60),('INV-000173',N'Bột mì','G',80000,10000),('INV-000174',N'Bột pizza','G',70000,8000),
        ('INV-000175',N'Bacon','G',30000,3500),('INV-000176',N'Bao bì món ăn','PIECE',1200,150),('INV-000177',N'Cà chua','G',35000,4000),('INV-000178',N'Dầu ăn','ML',70000,8000),
        ('INV-000179',N'Gạo','G',100000,12000),('INV-000180',N'Gia vị','G',25000,3000),('INV-000181',N'Hộp pizza','PIECE',500,60),('INV-000182',N'Hải sản','G',45000,5000),
        ('INV-000183',N'Khoai tây','G',80000,10000),('INV-000184',N'Ly và ống hút','PIECE',1000,120),('INV-000185',N'Nước pha chế','ML',120000,15000),('INV-000186',N'Phô mai lát','PIECE',900,100),
        ('INV-000187',N'Phô mai mozzarella','G',50000,6000),('INV-000188',N'Rau củ hỗn hợp','G',60000,7000),('INV-000189',N'Rau xà lách','G',40000,5000),('INV-000190',N'Sữa và kem','ML',35000,4000),
        ('INV-000191',N'Sốt BBQ','ML',30000,3500),('INV-000192',N'Sốt burger','ML',30000,3500),('INV-000193',N'Sốt món Á','ML',40000,5000),('INV-000194',N'Sốt pizza','ML',30000,3500),
        ('INV-000195',N'Thịt bò','G',70000,8000),('INV-000196',N'Thịt gà','G',90000,10000),('INV-000197',N'Thịt heo','G',60000,7000),('INV-000198',N'Trứng','PIECE',700,80);

        IF (SELECT COUNT_BIG(*) FROM @Expected)<>32 THROW 51000,'055 expected inventory map is incomplete',1;
        IF EXISTS(SELECT 1 FROM @Expected e LEFT JOIN dbo.InventoryItem i ON i.inventory_code=e.inventory_code WHERE i.inventory_item_id IS NULL OR i.name IS NULL OR i.name<>e.name OR i.item_type<>'INGREDIENT' OR i.base_unit<>e.base_unit) THROW 51000,'055 authoritative inventory text mapping mismatch',1;
        IF EXISTS(SELECT 1 FROM dbo.InventoryTransaction WHERE reason_code='OPENING_BALANCE' AND reference_type='MIGRATION' AND reference_id='053' AND (note IS NULL OR note<>N'Số dư đầu kỳ khi bật tính giá vốn')) THROW 51000,'055 opening balance note mismatch',1;
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('055_verify_inventory_text_repair',N'Verify authoritative inventory text repair against retained pre-054 quantities, units, and recipe-linked identities');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
