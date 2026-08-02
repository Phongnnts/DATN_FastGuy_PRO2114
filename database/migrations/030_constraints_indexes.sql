USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.Users WHERE role_name NOT IN ('ADMIN','STAFF','SHIPPER','USER')) THROW 51300, 'Unknown role_name values require manual mapping.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Users WHERE ISJSON(favorite_ids_json) <> 1 OR LEFT(LTRIM(favorite_ids_json),1) <> '[' OR EXISTS (SELECT 1 FROM OPENJSON(favorite_ids_json) j WHERE j.[type] <> 5 OR TRY_CONVERT(int, JSON_VALUE(j.value,'$.productId')) IS NULL OR NULLIF(JSON_VALUE(j.value,'$.createdAt'),'') IS NULL OR TRY_CONVERT(datetime2(0), JSON_VALUE(j.value,'$.createdAt'), 126) IS NULL)) THROW 51301, 'Users.favorite_ids_json must be an array of {productId,createdAt} objects.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Product WHERE ISJSON(gallery_images) <> 1) THROW 51302, 'Invalid Product.gallery_images.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CartItem WHERE ISJSON(modifiers_json) <> 1) THROW 51303, 'Invalid CartItem.modifiers_json.', 1;
    IF EXISTS (SELECT 1 FROM dbo.OrderItem WHERE ISJSON(modifiers_json) <> 1) THROW 51304, 'Invalid OrderItem.modifiers_json.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CartItem ci JOIN dbo.ProductVariant v ON v.variant_id=ci.variant_id WHERE ci.product_id<>v.product_id) THROW 51305, 'CartItem product/variant mismatch requires manual correction.', 1;
    IF EXISTS (SELECT 1 FROM dbo.OrderItem oi JOIN dbo.ProductVariant v ON v.variant_id=oi.variant_id WHERE oi.product_id IS NOT NULL AND oi.product_id<>v.product_id) THROW 51306, 'OrderItem product/variant mismatch requires manual correction.', 1;
    IF EXISTS (SELECT 1 FROM dbo.ProductComboItem ci JOIN dbo.ProductVariant v ON v.variant_id=ci.variant_id WHERE ci.product_id<>v.product_id) THROW 51307, 'ProductComboItem product/variant mismatch requires manual correction.', 1;
    ALTER TABLE dbo.Users ALTER COLUMN role_name varchar(20) NOT NULL;
    ALTER TABLE dbo.Users ALTER COLUMN loyalty_points int NOT NULL;
    ALTER TABLE dbo.Users ALTER COLUMN favorite_ids_json nvarchar(max) NOT NULL;
    ALTER TABLE dbo.Users ALTER COLUMN updated_at datetime2(0) NOT NULL;
    ALTER TABLE dbo.Product ALTER COLUMN gallery_images nvarchar(max) NOT NULL;
    ALTER TABLE dbo.Product ALTER COLUMN updated_at datetime2(0) NOT NULL;
    ALTER TABLE dbo.CartItem ALTER COLUMN modifiers_json nvarchar(max) NOT NULL;
    ALTER TABLE dbo.CartItem ALTER COLUMN updated_at datetime2(0) NOT NULL;
    ALTER TABLE dbo.OrderItem ALTER COLUMN modifiers_json nvarchar(max) NOT NULL;
    ALTER TABLE dbo.Orders ALTER COLUMN service_fee decimal(18,2) NOT NULL;
    ALTER TABLE dbo.Orders ALTER COLUMN updated_at datetime2(0) NOT NULL;
    IF OBJECT_ID(N'dbo.CK_Users_FavoritesJson', N'C') IS NULL ALTER TABLE dbo.Users WITH CHECK ADD CONSTRAINT CK_Users_FavoritesJson CHECK (ISJSON(favorite_ids_json)=1);
    IF OBJECT_ID(N'dbo.CK_Product_GalleryJson', N'C') IS NULL ALTER TABLE dbo.Product WITH CHECK ADD CONSTRAINT CK_Product_GalleryJson CHECK (ISJSON(gallery_images)=1);
    IF OBJECT_ID(N'dbo.CK_CartItem_ModifiersJson', N'C') IS NULL ALTER TABLE dbo.CartItem WITH CHECK ADD CONSTRAINT CK_CartItem_ModifiersJson CHECK (ISJSON(modifiers_json)=1);
    IF OBJECT_ID(N'dbo.CK_OrderItem_ModifiersJson', N'C') IS NULL ALTER TABLE dbo.OrderItem WITH CHECK ADD CONSTRAINT CK_OrderItem_ModifiersJson CHECK (ISJSON(modifiers_json)=1);
    IF OBJECT_ID(N'dbo.CK_Orders_GuestReturnProofHash', N'C') IS NULL ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_GuestReturnProofHash CHECK (guest_return_proof_hash IS NULL OR (LEN(guest_return_proof_hash)=64 AND guest_return_proof_hash NOT LIKE '%[^0-9a-f]%'));
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.ProductVariant') AND name=N'UX_ProductVariant_Default') CREATE UNIQUE INDEX UX_ProductVariant_Default ON dbo.ProductVariant(product_id) WHERE is_default=1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.ProductVariant') AND name=N'UX_ProductVariant_Sku') CREATE UNIQUE INDEX UX_ProductVariant_Sku ON dbo.ProductVariant(sku) WHERE sku IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Users') AND name=N'UX_Users_Email') CREATE UNIQUE INDEX UX_Users_Email ON dbo.Users(email) WHERE email IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'UX_Orders_Idempotency') CREATE UNIQUE INDEX UX_Orders_Idempotency ON dbo.Orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.CouponRedemption') AND name=N'UX_CouponRedemption_Order') CREATE UNIQUE INDEX UX_CouponRedemption_Order ON dbo.CouponRedemption(order_id) WHERE order_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '030_constraints_indexes') INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES ('030_constraints_indexes',N'Canonical nullability, JSON checks and filtered unique indexes applied');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT 'Legacy tables remain intentionally. Remove only after application cutover and archival approval.';
