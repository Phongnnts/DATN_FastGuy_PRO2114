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
    IF (SELECT COUNT(*) FROM dbo.SchemaMigrationHistory WHERE migration_id IN ('000_preflight_history','010_expand_schema','020_backfill_legacy','030_constraints_indexes','035_backend_hardening','039_inventory_waste_state','040_production_hardening')) <> 7 THROW 51400, 'Migration history incomplete; run 040_production_hardening.sql.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Users u CROSS APPLY OPENJSON(u.favorite_ids_json) j LEFT JOIN dbo.Product p ON p.product_id=TRY_CONVERT(int,JSON_VALUE(j.value,'$.productId')) WHERE j.[type]<>5 OR p.product_id IS NULL OR NULLIF(JSON_VALUE(j.value,'$.createdAt'),'') IS NULL OR TRY_CONVERT(datetime2(0),JSON_VALUE(j.value,'$.createdAt'),126) IS NULL) THROW 51401, 'Favorite JSON must contain valid {productId,createdAt} objects.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CartItem ci LEFT JOIN dbo.Cart c ON c.cart_id=ci.cart_id LEFT JOIN dbo.Product p ON p.product_id=ci.product_id LEFT JOIN dbo.ProductVariant v ON v.variant_id=ci.variant_id AND v.product_id=ci.product_id WHERE c.cart_id IS NULL OR p.product_id IS NULL OR v.variant_id IS NULL) THROW 51402, 'CartItem referential or product/variant validation failed.', 1;
    IF EXISTS (SELECT 1 FROM dbo.OrderItem oi LEFT JOIN dbo.Orders o ON o.order_id=oi.order_id LEFT JOIN dbo.ProductVariant v ON v.variant_id=oi.variant_id WHERE o.order_id IS NULL OR (oi.variant_id IS NOT NULL AND (v.variant_id IS NULL OR (oi.product_id IS NOT NULL AND v.product_id<>oi.product_id)))) THROW 51403, 'OrderItem referential or product/variant validation failed.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CouponRedemption r LEFT JOIN dbo.Coupon c ON c.coupon_id=r.coupon_id LEFT JOIN dbo.Users u ON u.user_id=r.user_id LEFT JOIN dbo.Orders o ON o.order_id=r.order_id WHERE c.coupon_id IS NULL OR u.user_id IS NULL OR (r.order_id IS NOT NULL AND o.order_id IS NULL)) THROW 51404, 'CouponRedemption has orphan rows.', 1;
    IF EXISTS (SELECT role_name FROM dbo.Users GROUP BY role_name HAVING role_name NOT IN ('ADMIN','STAFF','SHIPPER','USER')) THROW 51405, 'Role validation failed.', 1;
    IF COL_LENGTH('dbo.Orders', 'guest_return_proof_hash') IS NULL THROW 51406, 'Guest return proof hash column missing.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders WHERE guest_return_proof_hash IS NOT NULL AND (LEN(guest_return_proof_hash)<>64 OR guest_return_proof_hash LIKE '%[^0-9a-f]%')) THROW 51407, 'Guest return proof hash validation failed.', 1;
    IF OBJECT_ID(N'dbo.PaymentAttempt',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryReservation',N'U') IS NULL OR OBJECT_ID(N'dbo.InventoryTransaction',N'U') IS NULL THROW 51408, 'Payment or inventory table missing.', 1;
    IF EXISTS (SELECT user_id FROM dbo.Address WHERE is_default=1 GROUP BY user_id HAVING COUNT(*)>1) THROW 51409, 'Multiple default addresses remain.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o WHERE NOT EXISTS (SELECT 1 FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id)) THROW 51410, 'Order without status history.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o OUTER APPLY (SELECT TOP (1) h.to_status FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id ORDER BY h.created_at DESC,h.history_id DESC) latest WHERE latest.to_status<>o.order_status) THROW 51411, 'Latest order history status differs from Orders.order_status.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o WHERE NOT EXISTS (SELECT 1 FROM dbo.OrderItem oi WHERE oi.order_id=o.order_id) AND ISNULL(o.internal_note,N'') NOT LIKE N'%[[]SYSTEM:ITEMLESS-ORDER]%') THROW 51412, 'Itemless order lacks internal marker.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.staff_id WHERE o.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN ('STAFF','ADMIN'))) THROW 51413, 'Invalid staff assignment.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.shipper_id WHERE o.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>'SHIPPER')) THROW 51414, 'Invalid shipper assignment.', 1;
    IF EXISTS (SELECT 1 FROM dbo.WorkShift w LEFT JOIN dbo.Users u ON u.user_id=w.user_id WHERE u.user_id IS NULL OR u.role_name NOT IN ('STAFF','SHIPPER')) THROW 51415, 'Invalid workshift role.', 1;
    IF OBJECT_ID(N'dbo.CK_OrderItem_Total',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_Orders_FinalAmount',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_Orders_AssignmentTimes',N'C') IS NULL THROW 51416, 'Production-safe checks missing.', 1;
    IF OBJECT_ID(N'dbo.TR_Orders_AssignmentRoleGuard',N'TR') IS NULL OR OBJECT_ID(N'dbo.TR_WorkShift_RoleGuard',N'TR') IS NULL OR OBJECT_ID(N'dbo.TR_Users_OperationalRoleGuard',N'TR') IS NULL THROW 51417, 'Role guard triggers missing.', 1;
    IF EXISTS (SELECT 1 FROM sys.triggers WHERE object_id IN (OBJECT_ID(N'dbo.TR_Orders_AssignmentRoleGuard'),OBJECT_ID(N'dbo.TR_WorkShift_RoleGuard'),OBJECT_ID(N'dbo.TR_Users_OperationalRoleGuard')) AND is_disabled=1) THROW 51418, 'Role guard trigger disabled.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
SELECT migration_id, applied_at, applied_by, details FROM dbo.SchemaMigrationHistory ORDER BY applied_at, migration_id;
IF OBJECT_ID(N'dbo.Role',N'U') IS NOT NULL EXEC(N'SELECT N''Role'' legacy_table, COUNT_BIG(*) row_count FROM dbo.Role');
IF OBJECT_ID(N'dbo.FavoriteProduct',N'U') IS NOT NULL EXEC(N'SELECT N''FavoriteProduct'' legacy_table, COUNT_BIG(*) row_count FROM dbo.FavoriteProduct');
PRINT 'Validation passed. Compare business totals, sampled JSON, order amounts and application smoke tests before cutover.';
