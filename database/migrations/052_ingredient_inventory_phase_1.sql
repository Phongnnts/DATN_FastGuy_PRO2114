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
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory052_Test') THROW 51000, '052 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '052_ingredient_inventory_phase_1')
BEGIN
    IF COL_LENGTH(N'dbo.ProductVariant',N'inventory_mode') IS NULL OR OBJECT_ID(N'dbo.InventoryItem',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryReservationItem',N'U') IS NULL THROW 51000, '052 history exists but schema is incomplete', 1;
    PRINT '052_ingredient_inventory_phase_1 already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.ProductVariant',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryReservation',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryTransaction',N'U') IS NULL THROW 51000, '052 pre-migration schema missing', 1;
    IF EXISTS (SELECT 1 FROM (VALUES(N'InventoryItem'),(N'VariantInventoryItem'),(N'Recipe'),(N'RecipeItem'),(N'InventoryReservationItem')) x(name) WHERE OBJECT_ID(N'dbo.'+x.name,N'U') IS NOT NULL) OR COL_LENGTH(N'dbo.ProductVariant',N'inventory_mode') IS NOT NULL THROW 51000, '052 schema partially exists', 1;
    IF COL_LENGTH(N'dbo.InventoryReservation',N'variant_id') IS NULL OR COL_LENGTH(N'dbo.InventoryReservation',N'quantity') IS NULL OR COL_LENGTH(N'dbo.InventoryTransaction',N'variant_id') IS NULL THROW 51000, '052 legacy columns missing', 1;
    IF EXISTS (SELECT 1 FROM dbo.InventoryReservation WHERE quantity<=0) OR EXISTS (SELECT 1 FROM dbo.InventoryTransaction WHERE quantity<=0) THROW 51000, '052 legacy quantity invalid', 1;
    IF EXISTS(SELECT 1 FROM dbo.InventoryReservation GROUP BY order_id HAVING COUNT(DISTINCT status)>1) THROW 51000, '052 mixed reservation statuses per order', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:052_ingredient_inventory_phase_1',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'052 migration lock unavailable',1;
        DECLARE @legacy_reservations_before bigint=(SELECT COUNT_BIG(*) FROM dbo.InventoryReservation);
        DECLARE @legacy_transactions_before bigint=(SELECT COUNT_BIG(*) FROM dbo.InventoryTransaction);
        ALTER TABLE dbo.ProductVariant ADD inventory_mode varchar(20) NULL;
        CREATE TABLE dbo.InventoryItem (inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryItem PRIMARY KEY,name nvarchar(255) NOT NULL,item_type varchar(20) NOT NULL,base_unit varchar(10) NOT NULL,on_hand_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_OnHand DEFAULT 0,reserved_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Reserved DEFAULT 0,minimum_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Minimum DEFAULT 0,active bit NOT NULL CONSTRAINT DF_InventoryItem_Active DEFAULT 1,created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Created DEFAULT GETDATE(),updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Updated DEFAULT GETDATE(),CONSTRAINT CK_InventoryItem_Type CHECK(item_type IN('INGREDIENT','FINISHED_GOOD')),CONSTRAINT CK_InventoryItem_BaseUnit CHECK(base_unit IN('G','ML','PIECE')),CONSTRAINT CK_InventoryItem_OnHand CHECK(on_hand_quantity >= 0),CONSTRAINT CK_InventoryItem_Reserved CHECK(reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity),CONSTRAINT CK_InventoryItem_Minimum CHECK(minimum_quantity >= 0));
        CREATE TABLE dbo.VariantInventoryItem (variant_inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_VariantInventoryItem PRIMARY KEY,variant_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Variant REFERENCES dbo.ProductVariant(variant_id),inventory_item_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),CONSTRAINT UQ_VariantInventoryItem_Variant UNIQUE(variant_id),CONSTRAINT UQ_VariantInventoryItem_Item UNIQUE(inventory_item_id));
        CREATE TABLE dbo.Recipe (recipe_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Recipe PRIMARY KEY,variant_id int NOT NULL CONSTRAINT FK_Recipe_Variant REFERENCES dbo.ProductVariant(variant_id),yield_quantity decimal(19,4) NOT NULL CONSTRAINT DF_Recipe_Yield DEFAULT 1,active bit NOT NULL CONSTRAINT DF_Recipe_Active DEFAULT 1,created_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Created DEFAULT GETDATE(),updated_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Updated DEFAULT GETDATE(),CONSTRAINT UQ_Recipe_Variant UNIQUE(variant_id),CONSTRAINT CK_Recipe_Yield CHECK(yield_quantity>0));
        CREATE TABLE dbo.RecipeItem (recipe_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_RecipeItem PRIMARY KEY,recipe_id int NOT NULL CONSTRAINT FK_RecipeItem_Recipe REFERENCES dbo.Recipe(recipe_id),inventory_item_id int NOT NULL CONSTRAINT FK_RecipeItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),quantity decimal(19,4) NOT NULL,CONSTRAINT UQ_RecipeItem_RecipeInventoryItem UNIQUE(recipe_id,inventory_item_id),CONSTRAINT CK_RecipeItem_Quantity CHECK(quantity > 0));
        CREATE TABLE #Backfill(variant_id int PRIMARY KEY,inventory_item_id int);
        EXEC sys.sp_executesql N'MERGE dbo.InventoryItem target USING(SELECT v.variant_id,CONCAT(N''Finished good: '',p.name,N'' / '',v.variant_name) name,CONVERT(decimal(19,4),COALESCE(v.quantity_available,0)) quantity FROM dbo.ProductVariant v JOIN dbo.Product p ON p.product_id=v.product_id WHERE v.quantity_available IS NOT NULL OR EXISTS(SELECT 1 FROM dbo.InventoryReservation r WHERE r.variant_id=v.variant_id) OR EXISTS(SELECT 1 FROM dbo.InventoryTransaction t WHERE t.variant_id=v.variant_id)) source ON 1=0 WHEN NOT MATCHED THEN INSERT(name,item_type,base_unit,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES(source.name,''FINISHED_GOOD'',''PIECE'',source.quantity,0,0,1) OUTPUT source.variant_id,inserted.inventory_item_id INTO #Backfill;
        INSERT dbo.VariantInventoryItem(variant_id,inventory_item_id) SELECT variant_id,inventory_item_id FROM #Backfill;
        UPDATE dbo.ProductVariant SET inventory_mode=CASE WHEN status <> ''AVAILABLE'' THEN ''SUSPENDED'' WHEN quantity_available IS NOT NULL THEN ''FINISHED_GOOD'' ELSE ''UNTRACKED'' END;';
        CREATE TABLE dbo.InventoryReservationLegacyHistory (legacy_reservation_id int NOT NULL CONSTRAINT PK_InventoryReservationLegacyHistory PRIMARY KEY,canonical_reservation_id int NOT NULL,order_id int NOT NULL,variant_id int NOT NULL,inventory_item_id int NOT NULL,quantity decimal(19,4) NOT NULL,status varchar(20) NOT NULL,created_at datetime2(0) NOT NULL,updated_at datetime2(0) NOT NULL,CONSTRAINT FK_InventoryReservationLegacyHistory_Item FOREIGN KEY(inventory_item_id) REFERENCES dbo.InventoryItem(inventory_item_id));
        DECLARE @converted_reservations bigint;
        EXEC sys.sp_executesql N'INSERT dbo.InventoryReservationLegacyHistory(legacy_reservation_id,canonical_reservation_id,order_id,variant_id,inventory_item_id,quantity,status,created_at,updated_at)
        SELECT r.reservation_id,MIN(r.reservation_id) OVER(PARTITION BY r.order_id),r.order_id,r.variant_id,m.inventory_item_id,CONVERT(decimal(19,4),r.quantity),r.status,CONVERT(datetime2(0),r.created_at),CONVERT(datetime2(0),r.updated_at) FROM dbo.InventoryReservation r JOIN dbo.VariantInventoryItem m ON m.variant_id=r.variant_id;
        SET @rows=@@ROWCOUNT;',N'@rows bigint OUTPUT',@rows=@converted_reservations OUTPUT;
        IF @converted_reservations<>@legacy_reservations_before THROW 51000, '052 reservation archive count mismatch', 1;
        CREATE TABLE dbo.InventoryReservationItem (reservation_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservationItem PRIMARY KEY,reservation_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Reservation REFERENCES dbo.InventoryReservation(reservation_id),inventory_item_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),quantity decimal(19,4) NOT NULL,CONSTRAINT UQ_InventoryReservationItem_ReservationInventoryItem UNIQUE(reservation_id,inventory_item_id),CONSTRAINT CK_InventoryReservationItem_Quantity CHECK(quantity > 0));
        EXEC sys.sp_executesql N'INSERT dbo.InventoryReservationItem(reservation_id,inventory_item_id,quantity) SELECT MIN(canonical_reservation_id),inventory_item_id,SUM(quantity) FROM dbo.InventoryReservationLegacyHistory GROUP BY order_id,inventory_item_id;
        DELETE r FROM dbo.InventoryReservation r JOIN dbo.InventoryReservationLegacyHistory h ON h.legacy_reservation_id=r.reservation_id WHERE h.legacy_reservation_id<>h.canonical_reservation_id;
        UPDATE r SET status=CASE WHEN EXISTS(SELECT 1 FROM dbo.InventoryReservationLegacyHistory h WHERE h.canonical_reservation_id=r.reservation_id AND h.status=''RESERVED'') THEN ''RESERVED'' WHEN EXISTS(SELECT 1 FROM dbo.InventoryReservationLegacyHistory h WHERE h.canonical_reservation_id=r.reservation_id AND h.status=''CONSUMED'') THEN ''CONSUMED'' WHEN EXISTS(SELECT 1 FROM dbo.InventoryReservationLegacyHistory h WHERE h.canonical_reservation_id=r.reservation_id AND h.status=''WASTED'') THEN ''WASTED'' ELSE ''RELEASED'' END FROM dbo.InventoryReservation r;
        IF EXISTS(SELECT 1 FROM (SELECT CONVERT(decimal(38,4),COALESCE(v.quantity_available,0)) legacy_available,COALESCE(SUM(CASE WHEN r.status=''RESERVED'' THEN CONVERT(decimal(38,4),ri.quantity) ELSE CONVERT(decimal(38,4),0) END),0) reserved_quantity FROM dbo.VariantInventoryItem m JOIN dbo.ProductVariant v ON v.variant_id=m.variant_id LEFT JOIN dbo.InventoryReservationItem ri ON ri.inventory_item_id=m.inventory_item_id LEFT JOIN dbo.InventoryReservation r ON r.reservation_id=ri.reservation_id GROUP BY m.inventory_item_id,v.quantity_available) balances WHERE TRY_CONVERT(decimal(19,4),reserved_quantity) IS NULL OR TRY_CONVERT(decimal(19,4),legacy_available+reserved_quantity) IS NULL) THROW 51000,''052 inventory balance overflow'',1;
        ;WITH balances AS(SELECT m.inventory_item_id,CONVERT(decimal(38,4),COALESCE(v.quantity_available,0)) legacy_available,COALESCE(SUM(CASE WHEN r.status=''RESERVED'' THEN CONVERT(decimal(38,4),ri.quantity) ELSE CONVERT(decimal(38,4),0) END),0) reserved_quantity FROM dbo.VariantInventoryItem m JOIN dbo.ProductVariant v ON v.variant_id=m.variant_id LEFT JOIN dbo.InventoryReservationItem ri ON ri.inventory_item_id=m.inventory_item_id LEFT JOIN dbo.InventoryReservation r ON r.reservation_id=ri.reservation_id GROUP BY m.inventory_item_id,v.quantity_available)
        UPDATE i SET reserved_quantity=balances.reserved_quantity,on_hand_quantity=balances.legacy_available+balances.reserved_quantity FROM dbo.InventoryItem i JOIN balances ON balances.inventory_item_id=i.inventory_item_id;';
        ALTER TABLE dbo.InventoryTransaction ADD inventory_item_id int NULL,reference_type varchar(30) NULL,reference_id varchar(100) NULL;
        DECLARE @converted_transactions bigint;
        EXEC sys.sp_executesql N'UPDATE t SET inventory_item_id=m.inventory_item_id,quantity=CONVERT(decimal(19,4),t.quantity),quantity_before=CONVERT(decimal(19,4),t.quantity_before),quantity_after=CONVERT(decimal(19,4),t.quantity_after) FROM dbo.InventoryTransaction t JOIN dbo.VariantInventoryItem m ON m.variant_id=t.variant_id;
        SET @rows=@@ROWCOUNT;',N'@rows bigint OUTPUT',@rows=@converted_transactions OUTPUT;
        IF @converted_transactions<>@legacy_transactions_before THROW 51000, '052 transaction conversion count mismatch', 1;
        DECLARE @drop nvarchar(max)=N'';
        SELECT @drop+=N'ALTER TABLE '+QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id))+N'.'+QUOTENAME(OBJECT_NAME(parent_object_id))+N' DROP CONSTRAINT '+QUOTENAME(name)+N';' FROM sys.foreign_keys fk WHERE parent_object_id IN(OBJECT_ID(N'dbo.InventoryReservation'),OBJECT_ID(N'dbo.InventoryTransaction')) AND EXISTS(SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.columns c ON c.object_id=fkc.parent_object_id AND c.column_id=fkc.parent_column_id WHERE fkc.constraint_object_id=fk.object_id AND c.name=N'variant_id');
        SELECT @drop+=N'ALTER TABLE '+QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id))+N'.'+QUOTENAME(OBJECT_NAME(parent_object_id))+N' DROP CONSTRAINT '+QUOTENAME(name)+N';' FROM sys.check_constraints cc WHERE parent_object_id IN(OBJECT_ID(N'dbo.InventoryReservation'),OBJECT_ID(N'dbo.InventoryTransaction')) AND (definition LIKE N'%quantity%' OR definition LIKE N'%status%' OR definition LIKE N'%transaction_type%');
        SELECT @drop+=N'ALTER TABLE '+QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id))+N'.'+QUOTENAME(OBJECT_NAME(parent_object_id))+N' DROP CONSTRAINT '+QUOTENAME(name)+N';' FROM sys.key_constraints kc WHERE parent_object_id=OBJECT_ID(N'dbo.InventoryReservation') AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=kc.parent_object_id AND ic.index_id=kc.unique_index_id AND c.name IN(N'variant_id',N'quantity'));
        SELECT @drop+=N'DROP INDEX '+QUOTENAME(i.name)+N' ON '+QUOTENAME(OBJECT_SCHEMA_NAME(i.object_id))+N'.'+QUOTENAME(OBJECT_NAME(i.object_id))+N';' FROM sys.indexes i WHERE i.object_id IN(OBJECT_ID(N'dbo.InventoryReservation'),OBJECT_ID(N'dbo.InventoryTransaction')) AND i.is_primary_key=0 AND i.is_unique_constraint=0 AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND c.name=N'variant_id');
        EXEC sys.sp_executesql @drop;
        ALTER TABLE dbo.InventoryReservation DROP COLUMN variant_id,quantity;
        ALTER TABLE dbo.InventoryReservation ALTER COLUMN created_at datetime2(0) NOT NULL;
        ALTER TABLE dbo.InventoryReservation ALTER COLUMN updated_at datetime2(0) NOT NULL;
        ALTER TABLE dbo.InventoryReservation ADD CONSTRAINT UQ_InventoryReservation_Order UNIQUE(order_id),CONSTRAINT CK_InventoryReservation_Status CHECK(status IN('RESERVED','CONSUMED','RELEASED','WASTED'));
        EXEC sys.sp_executesql N'ALTER TABLE dbo.InventoryTransaction DROP COLUMN variant_id;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN created_at datetime2(0) NOT NULL;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN order_id int NULL;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN inventory_item_id int NOT NULL;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN quantity decimal(19,4) NOT NULL;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN quantity_before decimal(19,4) NULL;
        ALTER TABLE dbo.InventoryTransaction ALTER COLUMN quantity_after decimal(19,4) NULL;
        ALTER TABLE dbo.InventoryTransaction ADD CONSTRAINT FK_InventoryTransaction_Item FOREIGN KEY(inventory_item_id) REFERENCES dbo.InventoryItem(inventory_item_id),CONSTRAINT CK_InventoryTransaction_Quantity CHECK(quantity <> 0),CONSTRAINT CK_InventoryTransaction_Type CHECK(transaction_type IN(''RECEIPT'',''RESERVE'',''RELEASE'',''CONSUME'',''ADJUSTMENT'',''WASTE'',''RETURN''));';
        EXEC sys.sp_executesql N'ALTER TABLE dbo.ProductVariant ALTER COLUMN inventory_mode varchar(20) NOT NULL;
        ALTER TABLE dbo.ProductVariant ADD CONSTRAINT DF_ProductVariant_InventoryMode DEFAULT ''UNTRACKED'' FOR inventory_mode,CONSTRAINT CK_ProductVariant_InventoryMode CHECK(inventory_mode IN(''INGREDIENT'',''FINISHED_GOOD'',''UNTRACKED'',''SUSPENDED''));';
        EXEC sys.sp_executesql N'IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.InventoryItem'') AND name=N''IX_InventoryItem_ActiveType'') CREATE INDEX IX_InventoryItem_ActiveType ON dbo.InventoryItem(active,item_type);
        IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.RecipeItem'') AND name=N''IX_RecipeItem_InventoryItem'') CREATE INDEX IX_RecipeItem_InventoryItem ON dbo.RecipeItem(inventory_item_id);
        IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.InventoryReservationItem'') AND name=N''IX_InventoryReservationItem_InventoryItem'') CREATE INDEX IX_InventoryReservationItem_InventoryItem ON dbo.InventoryReservationItem(inventory_item_id);
        IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.InventoryTransaction'') AND name=N''IX_InventoryTransaction_Order'') CREATE INDEX IX_InventoryTransaction_Order ON dbo.InventoryTransaction(order_id);
        IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.InventoryTransaction'') AND name=N''IX_InventoryTransaction_ItemCreated'') CREATE INDEX IX_InventoryTransaction_ItemCreated ON dbo.InventoryTransaction(inventory_item_id,created_at DESC);';
        DECLARE @legacy_history_after bigint;
        EXEC sys.sp_executesql N'SELECT @rows=COUNT_BIG(*) FROM dbo.InventoryReservationLegacyHistory;',N'@rows bigint OUTPUT',@rows=@legacy_history_after OUTPUT;
        IF @legacy_history_after<>@legacy_reservations_before OR (SELECT COUNT_BIG(*) FROM dbo.InventoryTransaction)<>@legacy_transactions_before THROW 51000, '052 legacy history preservation failed', 1;
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('052_ingredient_inventory_phase_1',N'Ingredient inventory schema and lossless legacy inventory conversion');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
