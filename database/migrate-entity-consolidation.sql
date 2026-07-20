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
        om.option_name AS [name],
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
GO
-- ClaimedCoupon had: coupon_id, user_id, claimed_at (no created_at)
INSERT INTO CouponRedemption (coupon_id, user_id, order_id, claimed_at, used_at, created_at)
SELECT cc.coupon_id, cc.user_id, NULL, cc.claimed_at, NULL, cc.claimed_at
FROM ClaimedCoupon cc;
GO
-- CouponUsage had: coupon_id, user_id, order_id, discount_amount, used_at (no created_at)
INSERT INTO CouponRedemption (coupon_id, user_id, order_id, claimed_at, used_at, discount_amount, created_at)
SELECT cu.coupon_id, cu.user_id, cu.order_id, cu.used_at, cu.used_at, cu.discount_amount, cu.used_at
FROM CouponUsage cu;
GO
DROP TABLE CouponUsage;
GO
DROP TABLE ClaimedCoupon;
GO
