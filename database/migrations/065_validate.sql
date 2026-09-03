SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'DemoDatabase') AND DB_NAME() NOT LIKE N'FastGuyDB[_]%[_]Test' THROW 51000, '065 validator target is not approved', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='065_warehouse_operations_redesign') THROW 51000, '065 history missing', 1;
IF COL_LENGTH(N'dbo.StockCountItem',N'reserved_quantity_snapshot') IS NULL THROW 51000, '065 reserved snapshot missing', 1;
IF EXISTS(SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id WHERE c.object_id=OBJECT_ID(N'dbo.StockCountItem') AND c.name='reserved_quantity_snapshot' AND (t.name<>'decimal' OR c.precision<>19 OR c.scale<>4 OR c.is_nullable<>0)) THROW 51000, '065 reserved snapshot definition mismatch', 1;
IF OBJECT_DEFINITION(OBJECT_ID(N'dbo.CK_InventoryItem_Reserved')) NOT LIKE '%reserved_quantity%>=(0)%' OR OBJECT_DEFINITION(OBJECT_ID(N'dbo.CK_InventoryItem_Reserved')) LIKE '%on_hand_quantity%' THROW 51000, '065 reserved constraint mismatch', 1;
IF EXISTS(SELECT required.config_key FROM (VALUES('morning_count_notice_enabled'),('morning_count_notice_title'),('morning_count_notice_message'),('morning_count_notice_image_url'),('morning_count_notice_link'),('morning_count_notice_cta_label'))required(config_key) LEFT JOIN dbo.ShippingConfig c ON c.config_key=required.config_key GROUP BY required.config_key HAVING COUNT(c.config_key)<>1) THROW 51000, '065 notice config mismatch', 1;
PRINT '065 validation passed';
