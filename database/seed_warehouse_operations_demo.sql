SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME()<>N'DemoDatabase' THROW 51710, 'Warehouse demo seed target must be DemoDatabase', 1;
IF TRY_CONVERT(int,SESSION_CONTEXT(N'FASTGUY_ALLOW_WAREHOUSE_DEMO_SEED'))<>1 THROW 51711, 'Set FASTGUY_ALLOW_WAREHOUSE_DEMO_SEED=1 for this session', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='065_warehouse_operations_redesign') THROW 51712, 'Migration 065 is required', 1;
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @Items TABLE(code varchar(30) PRIMARY KEY,name nvarchar(150),unit varchar(10),on_hand decimal(19,4),reserved decimal(19,4),minimum decimal(19,4),cost decimal(19,4));
 INSERT @Items VALUES
 ('DEMO-ING-BEEF',N'Thịt bò','G',1800,600,2000,160),
 ('DEMO-ING-BUN',N'Bánh burger','PIECE',140,20,40,4000),
 ('DEMO-ING-CHEESE',N'Phô mai lát','PIECE',90,12,30,3000),
 ('DEMO-ING-LETTUCE',N'Rau xà lách','G',5200,300,1000,45),
 ('DEMO-ING-SAUCE',N'Sốt burger','ML',4800,250,900,50),
 ('DEMO-ING-POTATO',N'Khoai tây','G',15000,1800,3500,32),
 ('DEMO-ING-OIL',N'Dầu ăn','ML',9000,700,2000,38),
 ('DEMO-ING-SEASON',N'Gia vị','G',2200,100,500,70),
 ('DEMO-ING-COLA',N'Nước ngọt nền','ML',25000,3200,6000,18),
 ('DEMO-ING-CUP',N'Ly và ống hút','PIECE',180,35,60,1200),
 ('DEMO-ING-PACK',N'Bao bì món ăn','PIECE',240,30,70,900),
 ('DEMO-ING-ICE',N'Đá viên','G',12000,1500,3000,5);
 UPDATE i SET name=s.name,base_unit=s.unit,on_hand_quantity=s.on_hand,reserved_quantity=s.reserved,minimum_quantity=s.minimum,average_unit_cost=s.cost,count_frequency='DAILY',active=1,item_type='INGREDIENT'
 FROM dbo.InventoryItem i JOIN @Items s ON s.code=i.inventory_code;
 INSERT dbo.InventoryItem(inventory_code,name,item_type,base_unit,on_hand_quantity,reserved_quantity,minimum_quantity,count_frequency,average_unit_cost,active)
 SELECT s.code,s.name,'INGREDIENT',s.unit,s.on_hand,s.reserved,s.minimum,'DAILY',s.cost,1 FROM @Items s WHERE NOT EXISTS(SELECT 1 FROM dbo.InventoryItem i WHERE i.inventory_code=s.code);
 DECLARE @Recipe TABLE(sku varchar(50),code varchar(30),quantity decimal(19,4),PRIMARY KEY(sku,code));
 INSERT @Recipe VALUES
 ('BURGER-STD','DEMO-ING-BUN',1),('BURGER-STD','DEMO-ING-BEEF',120),('BURGER-STD','DEMO-ING-CHEESE',1),('BURGER-STD','DEMO-ING-LETTUCE',20),('BURGER-STD','DEMO-ING-SAUCE',20),('BURGER-STD','DEMO-ING-PACK',1),
 ('BURGER-L','DEMO-ING-BUN',1),('BURGER-L','DEMO-ING-BEEF',160),('BURGER-L','DEMO-ING-CHEESE',2),('BURGER-L','DEMO-ING-LETTUCE',30),('BURGER-L','DEMO-ING-SAUCE',30),('BURGER-L','DEMO-ING-PACK',1),
 ('FRIES-STD','DEMO-ING-POTATO',180),('FRIES-STD','DEMO-ING-OIL',25),('FRIES-STD','DEMO-ING-SEASON',5),('FRIES-STD','DEMO-ING-PACK',1),
 ('COLA-L','DEMO-ING-COLA',350),('COLA-L','DEMO-ING-ICE',180),('COLA-L','DEMO-ING-CUP',1),
 ('COMBO-BURGER','DEMO-ING-BUN',1),('COMBO-BURGER','DEMO-ING-BEEF',120),('COMBO-BURGER','DEMO-ING-CHEESE',1),('COMBO-BURGER','DEMO-ING-POTATO',150),('COMBO-BURGER','DEMO-ING-OIL',20),('COMBO-BURGER','DEMO-ING-COLA',300),('COMBO-BURGER','DEMO-ING-CUP',1),('COMBO-BURGER','DEMO-ING-PACK',1);
 INSERT dbo.Recipe(variant_id,yield_quantity,active)
 SELECT v.variant_id,1,1 FROM dbo.ProductVariant v WHERE EXISTS(SELECT 1 FROM @Recipe r WHERE r.sku=v.sku) AND NOT EXISTS(SELECT 1 FROM dbo.Recipe x WHERE x.variant_id=v.variant_id);
 UPDATE r SET yield_quantity=1,active=1 FROM dbo.Recipe r JOIN dbo.ProductVariant v ON v.variant_id=r.variant_id WHERE EXISTS(SELECT 1 FROM @Recipe x WHERE x.sku=v.sku);
 DELETE ri FROM dbo.RecipeItem ri JOIN dbo.Recipe r ON r.recipe_id=ri.recipe_id JOIN dbo.ProductVariant v ON v.variant_id=r.variant_id WHERE EXISTS(SELECT 1 FROM @Recipe x WHERE x.sku=v.sku);
 INSERT dbo.RecipeItem(recipe_id,inventory_item_id,quantity)
 SELECT r.recipe_id,i.inventory_item_id,x.quantity FROM @Recipe x JOIN dbo.ProductVariant v ON v.sku=x.sku JOIN dbo.Recipe r ON r.variant_id=v.variant_id JOIN dbo.InventoryItem i ON i.inventory_code=x.code;
 UPDATE v SET inventory_mode='INGREDIENT' FROM dbo.ProductVariant v WHERE EXISTS(SELECT 1 FROM @Recipe x WHERE x.sku=v.sku);
 IF NOT EXISTS(SELECT 1 FROM dbo.StockCount WHERE count_date=CAST(GETDATE() AS date) AND frequency='DAILY')
 BEGIN
  DECLARE @CountId int;
  INSERT dbo.StockCount(count_date,frequency,status,created_by) VALUES(CAST(GETDATE() AS date),'DAILY','DRAFT',1);
  SET @CountId=SCOPE_IDENTITY();
  INSERT dbo.StockCountItem(stock_count_id,inventory_item_id,theoretical_quantity,unit_cost_snapshot,reserved_quantity_snapshot)
  SELECT @CountId,i.inventory_item_id,i.on_hand_quantity,i.average_unit_cost,i.reserved_quantity FROM dbo.InventoryItem i WHERE i.inventory_code LIKE 'DEMO-ING-%';
 END;
 COMMIT;
END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
SELECT COUNT_BIG(*) ingredient_count FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-ING-%';
