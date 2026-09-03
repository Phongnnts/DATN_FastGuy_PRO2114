SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'DemoDatabase') AND DB_NAME() NOT LIKE N'FastGuyDB[_]%[_]Test' THROW 51000, '065 target is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryItem',N'U') IS NULL OR OBJECT_ID(N'dbo.StockCountItem',N'U') IS NULL THROW 51000, '065 prerequisites missing', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='065_warehouse_operations_redesign')
BEGIN
 IF COL_LENGTH(N'dbo.StockCountItem',N'reserved_quantity_snapshot') IS NULL THROW 51000, '065 history exists but schema is incomplete', 1;
 PRINT '065_warehouse_operations_redesign already applied';
END
ELSE
BEGIN
 BEGIN TRY
  BEGIN TRANSACTION;
  IF COL_LENGTH(N'dbo.StockCountItem',N'reserved_quantity_snapshot') IS NOT NULL THROW 51000, '065 schema partially exists', 1;
  ALTER TABLE dbo.InventoryItem DROP CONSTRAINT CK_InventoryItem_Reserved;
  ALTER TABLE dbo.InventoryItem ADD CONSTRAINT CK_InventoryItem_Reserved CHECK(reserved_quantity>=0);
  ALTER TABLE dbo.StockCountItem ADD reserved_quantity_snapshot decimal(19,4) NOT NULL CONSTRAINT DF_StockCountItem_ReservedSnapshot DEFAULT 0;
  INSERT dbo.ShippingConfig(config_key,config_value)
  SELECT v.config_key,v.config_value FROM (VALUES
   ('morning_count_notice_enabled','1'),
   ('morning_count_notice_title',N'Cửa hàng đang chuẩn bị nguyên liệu'),
   ('morning_count_notice_message',N'Chúng tôi đang kiểm kê đầu ngày. Bạn vẫn có thể xem thực đơn và đặt món theo giờ hoạt động.'),
   ('morning_count_notice_image_url',''),
   ('morning_count_notice_link',''),
   ('morning_count_notice_cta_label',N'Xem thông báo')
  )v(config_key,config_value) WHERE NOT EXISTS(SELECT 1 FROM dbo.ShippingConfig c WHERE c.config_key=v.config_key);
  INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('065_warehouse_operations_redesign',N'Warehouse shortage risk, count reservation snapshots, morning notice');
  COMMIT;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
END;
GO
