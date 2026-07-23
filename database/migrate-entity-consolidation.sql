-- Migration: Consolidate entities 29 -> 23
-- Applied to local FastGuyDB via sqlcmd Windows Auth

-- Step 1: Remove DeliveryZone
ALTER TABLE Address DROP CONSTRAINT FK__Address__zone_id__5AEE82B9;
GO
ALTER TABLE Address DROP COLUMN zone_id;
GO
ALTER TABLE Orders DROP CONSTRAINT FK__Orders__zone_id__6C190EBB;
GO
ALTER TABLE Orders DROP COLUMN zone_id;
GO
DROP TABLE DeliveryZone;
GO

-- Step 2: Role -> role_name column on Users
ALTER TABLE Users ADD role_name VARCHAR(20) DEFAULT 'USER';
GO
UPDATE Users SET role_name = r.role_name
FROM Users u
INNER JOIN Role r ON u.role_id = r.role_id;
GO
ALTER TABLE Users DROP CONSTRAINT FK__Users__role_id__5535A963;
GO
ALTER TABLE Users DROP COLUMN role_id;
GO
DROP TABLE Role;
GO

-- Step 3: CartItemModifier -> JSON on CartItem
-- Note: ProductModifierGroup uses 'name' (not 'group_name'), ProductModifierOption uses 'name' (not 'option_name')
ALTER TABLE CartItem ADD modifiers_json NVARCHAR(MAX);
GO
UPDATE ci
SET ci.modifiers_json = (
    SELECT
        cm.modifier_option_id AS modifierOptionId,
        g.modifier_group_id AS groupId,
        g.[name] AS groupName,
        o.[name] AS [name],
        o.price
    FROM CartItemModifier cm
    INNER JOIN ProductModifierOption o ON cm.modifier_option_id = o.modifier_option_id
    INNER JOIN ProductModifierGroup g ON o.modifier_group_id = g.modifier_group_id
    WHERE cm.cart_item_id = ci.cart_item_id
    FOR JSON PATH
)
FROM CartItem ci
WHERE EXISTS (SELECT 1 FROM CartItemModifier cm WHERE cm.cart_item_id = ci.cart_item_id);
GO
UPDATE CartItem SET modifiers_json = '[]' WHERE modifiers_json IS NULL;
GO
DROP TABLE CartItemModifier;
GO

-- Step 4: OrderItemModifier -> JSON on OrderItem
-- Note: OrderItemModifier has 'group_name', 'option_name' columns directly (denormalized)
ALTER TABLE OrderItem ADD modifiers_json NVARCHAR(MAX);
GO
UPDATE oi
SET oi.modifiers_json = (
    SELECT
        om.modifier_option_id AS modifierOptionId,
        om.group_name AS groupName,
        om.option_name AS optionName,
        om.price
    FROM OrderItemModifier om
    WHERE om.order_item_id = oi.order_item_id
    FOR JSON PATH
)
FROM OrderItem oi
WHERE EXISTS (SELECT 1 FROM OrderItemModifier om WHERE om.order_item_id = oi.order_item_id);
GO
UPDATE OrderItem SET modifiers_json = '[]' WHERE modifiers_json IS NULL;
GO
DROP TABLE OrderItemModifier;
GO

-- Step 5: FavoriteProduct -> JSON on Users
ALTER TABLE Users ADD favorite_ids_json NVARCHAR(MAX);
GO
UPDATE u
SET u.favorite_ids_json = (
    SELECT
        f.product_id AS productId,
        f.created_at AS createdAt
    FROM FavoriteProduct f
    WHERE f.user_id = u.user_id
    ORDER BY f.created_at DESC
    FOR JSON PATH
)
FROM Users u
WHERE EXISTS (SELECT 1 FROM FavoriteProduct f WHERE f.user_id = u.user_id);
GO
UPDATE Users SET favorite_ids_json = '[]' WHERE favorite_ids_json IS NULL;
GO
DROP TABLE FavoriteProduct;
GO

-- Step 6: ClaimedCoupon + CouponUsage -> CouponRedemption
IF OBJECT_ID('CouponRedemption', 'U') IS NULL
BEGIN
    CREATE TABLE CouponRedemption (
        redemption_id INT IDENTITY(1,1) PRIMARY KEY,
        coupon_id INT NOT NULL,
        user_id INT NOT NULL,
        order_id INT NULL,
        claimed_at DATETIME2 NULL,
        used_at DATETIME2 NULL,
        discount_amount DECIMAL(18,2) NULL,
        created_at DATETIME2 NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL DEFAULT GETDATE()
    );
END;
GO
IF OBJECT_ID('ClaimedCoupon', 'U') IS NOT NULL
BEGIN
    INSERT INTO CouponRedemption (coupon_id, user_id, claimed_at, created_at, updated_at)
    SELECT cc.coupon_id, cc.user_id, MIN(COALESCE(cc.claimed_at, GETDATE())), MIN(COALESCE(cc.claimed_at, GETDATE())), GETDATE()
    FROM ClaimedCoupon cc
    INNER JOIN Coupon c ON c.coupon_id = cc.coupon_id
    INNER JOIN Users u ON u.user_id = cc.user_id
    WHERE NOT EXISTS (SELECT 1 FROM CouponRedemption cr WHERE cr.coupon_id = cc.coupon_id AND cr.user_id = cc.user_id)
    GROUP BY cc.coupon_id, cc.user_id;
END;
GO
IF OBJECT_ID('CouponUsage', 'U') IS NOT NULL
BEGIN
    UPDATE cr
    SET order_id = x.order_id, used_at = x.used_at, discount_amount = x.discount_amount, updated_at = GETDATE()
    FROM CouponRedemption cr
    INNER JOIN (
        SELECT coupon_id, user_id, order_id, used_at, discount_amount,
               ROW_NUMBER() OVER (PARTITION BY coupon_id, user_id ORDER BY used_at DESC, coupon_usage_id DESC) rn
        FROM CouponUsage
        WHERE user_id IS NOT NULL
    ) x ON x.coupon_id = cr.coupon_id AND x.user_id = cr.user_id AND x.rn = 1
    INNER JOIN Orders o ON o.order_id = x.order_id
    WHERE cr.used_at IS NULL;

    INSERT INTO CouponRedemption (coupon_id, user_id, order_id, claimed_at, used_at, discount_amount, created_at, updated_at)
    SELECT x.coupon_id, x.user_id, x.order_id, x.used_at, x.used_at, x.discount_amount, x.used_at, GETDATE()
    FROM (
        SELECT coupon_id, user_id, order_id, used_at, discount_amount,
               ROW_NUMBER() OVER (PARTITION BY coupon_id, user_id ORDER BY used_at DESC, coupon_usage_id DESC) rn
        FROM CouponUsage
        WHERE user_id IS NOT NULL
    ) x
    INNER JOIN Coupon c ON c.coupon_id = x.coupon_id
    INNER JOIN Users u ON u.user_id = x.user_id
    INNER JOIN Orders o ON o.order_id = x.order_id
    WHERE x.rn = 1 AND NOT EXISTS (SELECT 1 FROM CouponRedemption cr WHERE cr.coupon_id = x.coupon_id AND cr.user_id = x.user_id);
END;
GO
DELETE cr FROM CouponRedemption cr
WHERE NOT EXISTS (SELECT 1 FROM Coupon c WHERE c.coupon_id = cr.coupon_id)
   OR NOT EXISTS (SELECT 1 FROM Users u WHERE u.user_id = cr.user_id)
   OR (cr.order_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Orders o WHERE o.order_id = cr.order_id));
GO
;WITH ranked AS (
    SELECT redemption_id, ROW_NUMBER() OVER (PARTITION BY user_id, coupon_id ORDER BY CASE WHEN used_at IS NULL THEN 0 ELSE 1 END, used_at DESC, redemption_id) rn
    FROM CouponRedemption
)
DELETE FROM ranked WHERE rn > 1;
GO
;WITH ranked AS (
    SELECT redemption_id, ROW_NUMBER() OVER (PARTITION BY order_id ORDER BY used_at DESC, redemption_id) rn
    FROM CouponRedemption WHERE order_id IS NOT NULL
)
UPDATE cr SET order_id = NULL, used_at = NULL, discount_amount = NULL, updated_at = GETDATE()
FROM CouponRedemption cr INNER JOIN ranked r ON r.redemption_id = cr.redemption_id WHERE r.rn > 1;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CouponRedemption_Coupon') ALTER TABLE CouponRedemption ADD CONSTRAINT FK_CouponRedemption_Coupon FOREIGN KEY (coupon_id) REFERENCES Coupon(coupon_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CouponRedemption_User') ALTER TABLE CouponRedemption ADD CONSTRAINT FK_CouponRedemption_User FOREIGN KEY (user_id) REFERENCES Users(user_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CouponRedemption_Order') ALTER TABLE CouponRedemption ADD CONSTRAINT FK_CouponRedemption_Order FOREIGN KEY (order_id) REFERENCES Orders(order_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_CouponRedemption_User_Coupon' AND object_id = OBJECT_ID('CouponRedemption')) CREATE UNIQUE INDEX UQ_CouponRedemption_User_Coupon ON CouponRedemption(user_id, coupon_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_CouponRedemption_Order' AND object_id = OBJECT_ID('CouponRedemption')) CREATE UNIQUE INDEX UQ_CouponRedemption_Order ON CouponRedemption(order_id) WHERE order_id IS NOT NULL;
GO
-- Legacy tables retain redemption history that the one-row-per-user model cannot represent.
-- Drop them only after an audited archival migration is available.
GO
