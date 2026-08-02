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
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51200, 'Run 000_preflight_history.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '020_backfill_legacy')
BEGIN
    PRINT '020_backfill_legacy already applied; skipped.';
    RETURN;
END;
BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH('dbo.Users', 'role_id') IS NOT NULL AND OBJECT_ID(N'dbo.Role', N'U') IS NOT NULL
        EXEC(N'UPDATE u SET role_name = UPPER(LTRIM(RTRIM(r.role_name))) FROM dbo.Users u JOIN dbo.Role r ON r.role_id = u.role_id WHERE u.role_name IS NULL');
    UPDATE dbo.Users SET role_name = 'USER' WHERE role_name IS NULL;
    UPDATE dbo.Users SET loyalty_points = 0 WHERE loyalty_points IS NULL;
    UPDATE dbo.Users SET updated_at = COALESCE(updated_at, created_at, GETDATE()) WHERE updated_at IS NULL;
    UPDATE dbo.Product SET gallery_images = N'[]' WHERE gallery_images IS NULL OR ISJSON(gallery_images) <> 1;
    UPDATE dbo.Product SET updated_at = COALESCE(updated_at, created_at, GETDATE()) WHERE updated_at IS NULL;
    IF OBJECT_ID(N'dbo.FavoriteProduct', N'U') IS NOT NULL
        EXEC(N'UPDATE u SET favorite_ids_json = COALESCE((SELECT fp.product_id AS productId, CONVERT(varchar(33), COALESCE(fp.created_at, u.created_at, GETDATE()), 126) AS createdAt FROM dbo.FavoriteProduct fp WHERE fp.user_id = u.user_id AND EXISTS (SELECT 1 FROM dbo.Product p WHERE p.product_id = fp.product_id) ORDER BY fp.product_id FOR JSON PATH), N''[]'') FROM dbo.Users u WHERE u.favorite_ids_json IS NULL OR LTRIM(RTRIM(u.favorite_ids_json)) = N''''');
    UPDATE dbo.Users SET favorite_ids_json = N'[]' WHERE favorite_ids_json IS NULL OR LTRIM(RTRIM(favorite_ids_json)) = N'';
    IF OBJECT_ID(N'dbo.CartItemModifier', N'U') IS NOT NULL
       AND COL_LENGTH('dbo.CartItemModifier', 'cart_item_id') IS NOT NULL
       AND COL_LENGTH('dbo.CartItemModifier', 'modifier_option_id') IS NOT NULL
         EXEC(N'UPDATE ci SET modifiers_json = COALESCE((SELECT cim.modifier_option_id AS modifierOptionId FROM dbo.CartItemModifier cim WHERE cim.cart_item_id = ci.cart_item_id ORDER BY cim.modifier_option_id FOR JSON PATH), N''[]'') FROM dbo.CartItem ci WHERE ci.modifiers_json IS NULL OR LTRIM(RTRIM(ci.modifiers_json)) = N''''');
    IF COL_LENGTH('dbo.CartItem', 'selected_modifier_option_ids') IS NOT NULL
        EXEC(N'UPDATE ci SET modifiers_json = COALESCE((SELECT TRY_CONVERT(int, LTRIM(RTRIM(j.value))) AS modifierOptionId FROM STRING_SPLIT(REPLACE(REPLACE(ci.selected_modifier_option_ids, ''['', ''''), '']'', ''''), '','') j WHERE TRY_CONVERT(int, LTRIM(RTRIM(j.value))) IS NOT NULL FOR JSON PATH), N''[]'') FROM dbo.CartItem ci WHERE ci.modifiers_json IS NULL');
    UPDATE dbo.CartItem SET modifiers_json = N'[]' WHERE modifiers_json IS NULL OR LTRIM(RTRIM(modifiers_json)) = N'';
    UPDATE dbo.CartItem SET updated_at = COALESCE(updated_at, created_at, GETDATE()) WHERE updated_at IS NULL;
    IF OBJECT_ID(N'dbo.OrderItemModifier', N'U') IS NOT NULL
        EXEC(N'UPDATE oi SET modifiers_json = COALESCE((SELECT oim.modifier_option_id AS modifierOptionId, oim.group_name AS groupName, oim.option_name AS optionName, oim.price FROM dbo.OrderItemModifier oim WHERE oim.order_item_id = oi.order_item_id ORDER BY oim.order_item_modifier_id FOR JSON PATH), N''[]'') FROM dbo.OrderItem oi WHERE oi.modifiers_json IS NULL OR LTRIM(RTRIM(oi.modifiers_json)) = N''''');
    UPDATE dbo.OrderItem SET modifiers_json = N'[]' WHERE modifiers_json IS NULL OR LTRIM(RTRIM(modifiers_json)) = N'';
    UPDATE dbo.Orders SET payment_method = CASE payment_method WHEN 'CASH' THEN 'COD' WHEN 'BANKING' THEN 'BANK_TRANSFER' ELSE payment_method END WHERE payment_method IN ('CASH','BANKING');
    UPDATE dbo.Orders SET service_fee = 0 WHERE service_fee IS NULL;
    UPDATE dbo.Orders SET updated_at = COALESCE(updated_at, created_at, GETDATE()) WHERE updated_at IS NULL;
    IF OBJECT_ID(N'dbo.ClaimedCoupon', N'U') IS NOT NULL
        EXEC(N'INSERT dbo.CouponRedemption(coupon_id,user_id,order_id,claimed_at,used_at,discount_amount,created_at,updated_at) SELECT c.coupon_id,c.user_id,NULL,COALESCE(c.claimed_at,GETDATE()),NULL,NULL,COALESCE(c.claimed_at,GETDATE()),COALESCE(c.claimed_at,GETDATE()) FROM dbo.ClaimedCoupon c WHERE NOT EXISTS (SELECT 1 FROM dbo.CouponRedemption r WHERE r.coupon_id=c.coupon_id AND r.user_id=c.user_id)');
    IF OBJECT_ID(N'dbo.CouponUsage', N'U') IS NOT NULL
        EXEC(N'INSERT dbo.CouponRedemption(coupon_id,user_id,order_id,claimed_at,used_at,discount_amount,created_at,updated_at) SELECT cu.coupon_id,cu.user_id,cu.order_id,COALESCE(cu.used_at,o.created_at,GETDATE()),COALESCE(cu.used_at,o.created_at,GETDATE()),o.discount_amount,COALESCE(cu.used_at,o.created_at,GETDATE()),COALESCE(cu.used_at,o.created_at,GETDATE()) FROM dbo.CouponUsage cu JOIN dbo.Orders o ON o.order_id=cu.order_id WHERE NOT EXISTS (SELECT 1 FROM dbo.CouponRedemption r WHERE r.order_id=cu.order_id)');
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '020_backfill_legacy')
        INSERT dbo.SchemaMigrationHistory(migration_id, details) VALUES ('020_backfill_legacy', N'Roles, favorites, modifiers, coupons and orders backfilled; legacy objects retained');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
