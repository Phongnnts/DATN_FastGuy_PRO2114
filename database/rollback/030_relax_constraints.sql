USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51900, 'Migration history not found.', 1;
IF NOT EXISTS (SELECT 1 FROM msdb.dbo.backupset WHERE database_name=DB_NAME() AND type='D' AND backup_finish_date>=DATEADD(day,-7,GETDATE())) THROW 51901, 'Restore from the required backup instead; no recent full backup is recorded.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.CK_OrderItem_ModifiersJson',N'C') IS NOT NULL ALTER TABLE dbo.OrderItem DROP CONSTRAINT CK_OrderItem_ModifiersJson;
    IF OBJECT_ID(N'dbo.CK_CartItem_ModifiersJson',N'C') IS NOT NULL ALTER TABLE dbo.CartItem DROP CONSTRAINT CK_CartItem_ModifiersJson;
    IF OBJECT_ID(N'dbo.CK_Product_GalleryJson',N'C') IS NOT NULL ALTER TABLE dbo.Product DROP CONSTRAINT CK_Product_GalleryJson;
    IF OBJECT_ID(N'dbo.CK_Users_FavoritesJson',N'C') IS NOT NULL ALTER TABLE dbo.Users DROP CONSTRAINT CK_Users_FavoritesJson;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'UX_Orders_Idempotency') DROP INDEX UX_Orders_Idempotency ON dbo.Orders;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.CouponRedemption') AND name=N'UX_CouponRedemption_Order') DROP INDEX UX_CouponRedemption_Order ON dbo.CouponRedemption;
    DELETE dbo.SchemaMigrationHistory WHERE migration_id IN ('030_constraints_indexes','040_validate');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT 'Only enforcement added by 030 was relaxed. Backfilled data and legacy tables were preserved. Restore the verified full backup for complete rollback.';
