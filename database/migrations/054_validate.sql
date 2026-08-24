SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test') THROW 51000, '054 validator target database is not approved', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='054_repair_inventory_text_encoding') THROW 51000, '054 migration history missing', 1;

DECLARE @Names TABLE(inventory_code varchar(30) PRIMARY KEY,name nvarchar(255) NOT NULL);
INSERT @Names(inventory_code,name) VALUES
('INV-000167',N'Đá viên'),('INV-000168',N'Đường'),('INV-000169',N'Bánh burger'),('INV-000170',N'Bánh mì'),
('INV-000171',N'Bánh tortilla'),('INV-000172',N'Bánh tráng'),('INV-000173',N'Bột mì'),('INV-000174',N'Bột pizza'),
('INV-000175',N'Bacon'),('INV-000176',N'Bao bì món ăn'),('INV-000177',N'Cà chua'),('INV-000178',N'Dầu ăn'),
('INV-000179',N'Gạo'),('INV-000180',N'Gia vị'),('INV-000181',N'Hộp pizza'),('INV-000182',N'Hải sản'),
('INV-000183',N'Khoai tây'),('INV-000184',N'Ly và ống hút'),('INV-000185',N'Nước pha chế'),('INV-000186',N'Phô mai lát'),
('INV-000187',N'Phô mai mozzarella'),('INV-000188',N'Rau củ hỗn hợp'),('INV-000189',N'Rau xà lách'),('INV-000190',N'Sữa và kem'),
('INV-000191',N'Sốt BBQ'),('INV-000192',N'Sốt burger'),('INV-000193',N'Sốt món Á'),('INV-000194',N'Sốt pizza'),
('INV-000195',N'Thịt bò'),('INV-000196',N'Thịt gà'),('INV-000197',N'Thịt heo'),('INV-000198',N'Trứng');

IF EXISTS(SELECT 1 FROM dbo.InventoryItem i JOIN @Names n ON n.inventory_code=i.inventory_code WHERE i.name<>n.name) THROW 51000, '054 ingredient name repair mismatch', 1;
IF EXISTS(SELECT 1 FROM dbo.InventoryTransaction WHERE reason_code='OPENING_BALANCE' AND reference_type='MIGRATION' AND reference_id='053' AND note<>N'Số dư đầu kỳ khi bật tính giá vốn') THROW 51000, '054 opening balance note repair mismatch', 1;
PRINT '054 validation passed';
