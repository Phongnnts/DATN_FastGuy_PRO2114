SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory053_Test') THROW 51000, '053 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='053_inventory_costing_stock_count')
BEGIN
    IF COL_LENGTH(N'dbo.InventoryItem',N'average_unit_cost') IS NULL OR OBJECT_ID(N'dbo.GoodsReceipt',N'U') IS NULL OR OBJECT_ID(N'dbo.StockCount',N'U') IS NULL THROW 51000, '053 history exists but schema is incomplete', 1;
    PRINT '053_inventory_costing_stock_count already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.InventoryItem',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryTransaction',N'U') IS NULL THROW 51000, 'Run migration 052 first', 1;
    IF COL_LENGTH(N'dbo.InventoryItem',N'average_unit_cost') IS NOT NULL OR OBJECT_ID(N'dbo.GoodsReceipt',N'U') IS NOT NULL OR OBJECT_ID(N'dbo.StockCount',N'U') IS NOT NULL THROW 51000, '053 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:053_inventory_costing_stock_count',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'053 migration lock unavailable',1;

        ALTER TABLE dbo.InventoryItem ADD
            inventory_code varchar(30) NULL,
            count_frequency varchar(10) NOT NULL CONSTRAINT DF_InventoryItem_CountFrequency DEFAULT 'WEEKLY',
            average_unit_cost decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_AverageUnitCost DEFAULT 0,
            last_counted_at datetime2(0) NULL;
        EXEC sys.sp_executesql N'UPDATE dbo.InventoryItem SET inventory_code=CONCAT(''INV-'',RIGHT(CONCAT(''000000'',inventory_item_id),6));';
        ALTER TABLE dbo.InventoryItem ALTER COLUMN inventory_code varchar(30) NOT NULL;
        ALTER TABLE dbo.InventoryItem ADD
            CONSTRAINT UQ_InventoryItem_Code UNIQUE(inventory_code),
            CONSTRAINT CK_InventoryItem_CountFrequency CHECK(count_frequency IN('DAILY','WEEKLY')),
            CONSTRAINT CK_InventoryItem_AverageUnitCost CHECK(average_unit_cost>=0);

        CREATE TABLE dbo.GoodsReceipt(
            goods_receipt_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceipt PRIMARY KEY,
            supplier_name nvarchar(150) NOT NULL,
            invoice_number nvarchar(100) NULL,
            received_at datetime2(0) NOT NULL,
            status varchar(10) NOT NULL CONSTRAINT DF_GoodsReceipt_Status DEFAULT 'DRAFT',
            created_by int NOT NULL CONSTRAINT FK_GoodsReceipt_CreatedBy REFERENCES dbo.Users(user_id),
            approved_by int NULL CONSTRAINT FK_GoodsReceipt_ApprovedBy REFERENCES dbo.Users(user_id),
            created_at datetime2(0) NOT NULL CONSTRAINT DF_GoodsReceipt_CreatedAt DEFAULT GETDATE(),
            approved_at datetime2(0) NULL,
            CONSTRAINT CK_GoodsReceipt_Status CHECK(status IN('DRAFT','APPROVED')),
            CONSTRAINT CK_GoodsReceipt_Approval CHECK((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
        );
        CREATE TABLE dbo.GoodsReceiptItem(
            goods_receipt_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceiptItem PRIMARY KEY,
            goods_receipt_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Receipt REFERENCES dbo.GoodsReceipt(goods_receipt_id),
            inventory_item_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
            purchase_quantity decimal(19,4) NOT NULL,
            purchase_unit nvarchar(30) NOT NULL,
            conversion_factor decimal(19,4) NOT NULL,
            base_quantity decimal(19,4) NOT NULL,
            purchase_unit_price decimal(19,4) NOT NULL,
            line_total decimal(19,4) NOT NULL,
            average_cost_before decimal(19,4) NULL,
            average_cost_after decimal(19,4) NULL,
            CONSTRAINT UQ_GoodsReceiptItem_ReceiptItem UNIQUE(goods_receipt_id,inventory_item_id),
            CONSTRAINT CK_GoodsReceiptItem_Positive CHECK(purchase_quantity>0 AND conversion_factor>0 AND base_quantity>0 AND purchase_unit_price>0 AND line_total>0),
            CONSTRAINT CK_GoodsReceiptItem_Cost CHECK((average_cost_before IS NULL AND average_cost_after IS NULL) OR (average_cost_before>=0 AND average_cost_after>=0))
        );

        CREATE TABLE dbo.StockCount(
            stock_count_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCount PRIMARY KEY,
            count_date date NOT NULL,
            frequency varchar(10) NOT NULL,
            status varchar(10) NOT NULL CONSTRAINT DF_StockCount_Status DEFAULT 'DRAFT',
            created_by int NOT NULL CONSTRAINT FK_StockCount_CreatedBy REFERENCES dbo.Users(user_id),
            approved_by int NULL CONSTRAINT FK_StockCount_ApprovedBy REFERENCES dbo.Users(user_id),
            created_at datetime2(0) NOT NULL CONSTRAINT DF_StockCount_CreatedAt DEFAULT GETDATE(),
            approved_at datetime2(0) NULL,
            CONSTRAINT CK_StockCount_Frequency CHECK(frequency IN('DAILY','WEEKLY')),
            CONSTRAINT CK_StockCount_Status CHECK(status IN('DRAFT','APPROVED')),
            CONSTRAINT CK_StockCount_Approval CHECK((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
        );
        CREATE TABLE dbo.StockCountItem(
            stock_count_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCountItem PRIMARY KEY,
            stock_count_id int NOT NULL CONSTRAINT FK_StockCountItem_Count REFERENCES dbo.StockCount(stock_count_id),
            inventory_item_id int NOT NULL CONSTRAINT FK_StockCountItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
            theoretical_quantity decimal(19,4) NOT NULL,
            actual_quantity decimal(19,4) NULL,
            variance_quantity decimal(19,4) NULL,
            unit_cost_snapshot decimal(19,4) NULL,
            variance_cost decimal(19,4) NULL,
            reason_code varchar(50) NULL,
            note nvarchar(500) NULL,
            CONSTRAINT UQ_StockCountItem_CountItem UNIQUE(stock_count_id,inventory_item_id),
            CONSTRAINT CK_StockCountItem_Quantity CHECK(theoretical_quantity>=0 AND (actual_quantity IS NULL OR actual_quantity>=0)),
            CONSTRAINT CK_StockCountItem_Cost CHECK(unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0)
        );

        ALTER TABLE dbo.InventoryTransaction ADD
            unit_cost_snapshot decimal(19,4) NULL,
            total_cost decimal(19,4) NULL,
            goods_receipt_id int NULL,
            stock_count_id int NULL;
        ALTER TABLE dbo.InventoryTransaction ADD
            CONSTRAINT FK_InventoryTransaction_GoodsReceipt FOREIGN KEY(goods_receipt_id) REFERENCES dbo.GoodsReceipt(goods_receipt_id),
            CONSTRAINT FK_InventoryTransaction_StockCount FOREIGN KEY(stock_count_id) REFERENCES dbo.StockCount(stock_count_id),
            CONSTRAINT CK_InventoryTransaction_Cost CHECK((unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0) AND (total_cost IS NULL OR total_cost>=0));

        EXEC sys.sp_executesql N'INSERT dbo.InventoryTransaction(inventory_item_id,transaction_type,quantity,reason_code,note,quantity_before,quantity_after,reference_type,reference_id,unit_cost_snapshot,total_cost,created_at)
        SELECT inventory_item_id,''ADJUSTMENT'',on_hand_quantity,''OPENING_BALANCE'',N''Số dư đầu kỳ khi bật tính giá vốn'',CONVERT(decimal(19,4),0),on_hand_quantity,''MIGRATION'',''053'',CONVERT(decimal(19,4),0),CONVERT(decimal(19,4),0),GETDATE()
        FROM dbo.InventoryItem WHERE on_hand_quantity>0;';

        CREATE INDEX IX_GoodsReceipt_StatusReceived ON dbo.GoodsReceipt(status,received_at DESC);
        CREATE INDEX IX_StockCount_StatusDate ON dbo.StockCount(status,count_date DESC);
        EXEC sys.sp_executesql N'CREATE INDEX IX_InventoryTransaction_GoodsReceipt ON dbo.InventoryTransaction(goods_receipt_id) WHERE goods_receipt_id IS NOT NULL;
        CREATE INDEX IX_InventoryTransaction_StockCount ON dbo.InventoryTransaction(stock_count_id) WHERE stock_count_id IS NOT NULL;';

        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('053_inventory_costing_stock_count',N'Moving-average costing, goods receipts, physical stock counts, and cost snapshots');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
