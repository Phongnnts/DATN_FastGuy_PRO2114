USE master;
GO

IF DB_ID(N'FastGuyDB') IS NOT NULL
BEGIN
    ALTER DATABASE FastGuyDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE FastGuyDB;
END;
GO

CREATE DATABASE FastGuyDB;
GO
ALTER DATABASE FastGuyDB SET READ_COMMITTED_SNAPSHOT ON;
GO
USE FastGuyDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

CREATE TABLE dbo.Category (
    category_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Category PRIMARY KEY,
    name nvarchar(255) NOT NULL,
    description nvarchar(500) NULL,
    sort_order int NOT NULL CONSTRAINT DF_Category_SortOrder DEFAULT 0,
    status varchar(20) NOT NULL CONSTRAINT DF_Category_Status DEFAULT 'ACTIVE',
    CONSTRAINT CK_Category_Status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE dbo.Product (
    product_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Product PRIMARY KEY,
    category_id int NOT NULL CONSTRAINT FK_Product_Category REFERENCES dbo.Category(category_id),
    name nvarchar(255) NOT NULL,
    description nvarchar(1000) NULL,
    base_price decimal(18,2) NOT NULL,
    image_url varchar(500) NULL,
    gallery_images nvarchar(max) NOT NULL CONSTRAINT DF_Product_Gallery DEFAULT N'[]',
    status varchar(20) NOT NULL CONSTRAINT DF_Product_Status DEFAULT 'AVAILABLE',
    available_from time(0) NULL,
    available_to time(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Product_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Product_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Product_BasePrice CHECK (base_price >= 0),
    CONSTRAINT CK_Product_Status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'INACTIVE'))
);

CREATE TABLE dbo.ProductVariant (
    variant_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductVariant PRIMARY KEY,
    product_id int NOT NULL CONSTRAINT FK_ProductVariant_Product REFERENCES dbo.Product(product_id),
    variant_name nvarchar(255) NOT NULL,
    price decimal(18,2) NOT NULL,
    original_price decimal(18,2) NULL,
    sku varchar(100) NULL,
    quantity_available int NULL,
    weight decimal(10,2) NOT NULL CONSTRAINT DF_ProductVariant_Weight DEFAULT 500,
    length decimal(10,2) NOT NULL CONSTRAINT DF_ProductVariant_Length DEFAULT 20,
    width decimal(10,2) NOT NULL CONSTRAINT DF_ProductVariant_Width DEFAULT 20,
    height decimal(10,2) NOT NULL CONSTRAINT DF_ProductVariant_Height DEFAULT 10,
    is_default bit NOT NULL CONSTRAINT DF_ProductVariant_Default DEFAULT 0,
    status varchar(20) NOT NULL CONSTRAINT DF_ProductVariant_Status DEFAULT 'AVAILABLE',
    created_at datetime2(0) NOT NULL CONSTRAINT DF_ProductVariant_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_ProductVariant_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_ProductVariant_Price CHECK (price >= 0 AND (original_price IS NULL OR original_price >= 0)),
    CONSTRAINT CK_ProductVariant_Quantity CHECK (quantity_available IS NULL OR quantity_available >= 0),
    CONSTRAINT CK_ProductVariant_Dimensions CHECK (weight > 0 AND length > 0 AND width > 0 AND height > 0),
    CONSTRAINT CK_ProductVariant_Status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'INACTIVE'))
);

CREATE TABLE dbo.ProductModifierGroup (
    modifier_group_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductModifierGroup PRIMARY KEY,
    product_id int NOT NULL CONSTRAINT FK_ProductModifierGroup_Product REFERENCES dbo.Product(product_id),
    name nvarchar(255) NOT NULL,
    min_selections int NOT NULL CONSTRAINT DF_ProductModifierGroup_Min DEFAULT 0,
    max_selections int NOT NULL CONSTRAINT DF_ProductModifierGroup_Max DEFAULT 1,
    is_active bit NOT NULL CONSTRAINT DF_ProductModifierGroup_Active DEFAULT 1,
    sort_order int NOT NULL CONSTRAINT DF_ProductModifierGroup_Sort DEFAULT 0,
    CONSTRAINT CK_ProductModifierGroup_Selections CHECK (min_selections >= 0 AND max_selections >= min_selections)
);

CREATE TABLE dbo.ProductModifierOption (
    modifier_option_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductModifierOption PRIMARY KEY,
    modifier_group_id int NOT NULL CONSTRAINT FK_ProductModifierOption_Group REFERENCES dbo.ProductModifierGroup(modifier_group_id),
    name nvarchar(255) NOT NULL,
    price decimal(18,2) NOT NULL CONSTRAINT DF_ProductModifierOption_Price DEFAULT 0,
    is_active bit NOT NULL CONSTRAINT DF_ProductModifierOption_Active DEFAULT 1,
    sort_order int NOT NULL CONSTRAINT DF_ProductModifierOption_Sort DEFAULT 0,
    CONSTRAINT CK_ProductModifierOption_Price CHECK (price >= 0)
);

CREATE TABLE dbo.ProductCombo (
    combo_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductCombo PRIMARY KEY,
    product_id int NOT NULL CONSTRAINT FK_ProductCombo_Product REFERENCES dbo.Product(product_id),
    is_active bit NOT NULL CONSTRAINT DF_ProductCombo_Active DEFAULT 1,
    CONSTRAINT UQ_ProductCombo_Product UNIQUE (product_id)
);

CREATE TABLE dbo.ProductComboItem (
    combo_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductComboItem PRIMARY KEY,
    combo_id int NOT NULL CONSTRAINT FK_ProductComboItem_Combo REFERENCES dbo.ProductCombo(combo_id),
    product_id int NOT NULL CONSTRAINT FK_ProductComboItem_Product REFERENCES dbo.Product(product_id),
    variant_id int NOT NULL CONSTRAINT FK_ProductComboItem_Variant REFERENCES dbo.ProductVariant(variant_id),
    quantity int NOT NULL CONSTRAINT DF_ProductComboItem_Quantity DEFAULT 1,
    sort_order int NOT NULL CONSTRAINT DF_ProductComboItem_Sort DEFAULT 0,
    CONSTRAINT CK_ProductComboItem_Quantity CHECK (quantity > 0)
);

CREATE TABLE dbo.ShippingConfig (
    config_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ShippingConfig PRIMARY KEY,
    config_key varchar(100) NOT NULL CONSTRAINT UQ_ShippingConfig_Key UNIQUE,
    config_value varchar(500) NOT NULL
);

CREATE TABLE dbo.Users (
    user_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Users PRIMARY KEY,
    role_name varchar(20) NOT NULL CONSTRAINT DF_Users_Role DEFAULT 'USER',
    email varchar(255) NULL,
    phone varchar(20) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name nvarchar(255) NOT NULL,
    avatar_url varchar(500) NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_Users_Status DEFAULT 'ACTIVE',
    loyalty_points int NOT NULL CONSTRAINT DF_Users_Loyalty DEFAULT 0,
    favorite_ids_json nvarchar(max) NOT NULL CONSTRAINT DF_Users_Favorites DEFAULT N'[]',
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Users_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Users_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Users_Role CHECK (role_name IN ('ADMIN', 'STAFF', 'SHIPPER', 'USER')),
    CONSTRAINT CK_Users_Status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    CONSTRAINT CK_Users_Loyalty CHECK (loyalty_points >= 0)
);

CREATE TABLE dbo.PasswordResetToken (
    reset_token_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_PasswordResetToken PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_PasswordResetToken_User REFERENCES dbo.Users(user_id),
    token_hash varchar(64) NOT NULL CONSTRAINT UQ_PasswordResetToken_Hash UNIQUE,
    expires_at datetime2(0) NOT NULL,
    used_at datetime2(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_PasswordResetToken_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_PasswordResetToken_Updated DEFAULT GETDATE()
);

CREATE TABLE dbo.Banner (
    banner_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Banner PRIMARY KEY,
    title nvarchar(255) NULL,
    subtitle nvarchar(500) NULL,
    image_url varchar(500) NOT NULL,
    link varchar(500) NULL,
    sort_order int NOT NULL CONSTRAINT DF_Banner_Sort DEFAULT 0,
    is_active bit NOT NULL CONSTRAINT DF_Banner_Active DEFAULT 1,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Banner_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Banner_Updated DEFAULT GETDATE()
);

CREATE TABLE dbo.Address (
    address_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Address PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_Address_User REFERENCES dbo.Users(user_id),
    recipient_name nvarchar(255) NOT NULL,
    phone varchar(20) NOT NULL,
    street nvarchar(255) NOT NULL,
    ward_name nvarchar(100) NULL,
    district_name nvarchar(100) NULL,
    province_name nvarchar(100) NULL,
    ghn_province_id int NULL,
    ghn_district_id int NULL,
    ghn_ward_code varchar(50) NULL,
    city nvarchar(100) NOT NULL CONSTRAINT DF_Address_City DEFAULT N'TP. Ho Chi Minh',
    is_default bit NOT NULL CONSTRAINT DF_Address_Default DEFAULT 0,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Address_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Address_Updated DEFAULT GETDATE()
);

CREATE TABLE dbo.Cart (
    cart_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Cart PRIMARY KEY,
    user_id int NULL CONSTRAINT FK_Cart_User REFERENCES dbo.Users(user_id),
    session_id varchar(128) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Cart_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Cart_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Cart_Owner CHECK (user_id IS NOT NULL OR session_id IS NOT NULL)
);

CREATE TABLE dbo.CartItem (
    cart_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CartItem PRIMARY KEY,
    cart_id int NOT NULL CONSTRAINT FK_CartItem_Cart REFERENCES dbo.Cart(cart_id),
    product_id int NOT NULL CONSTRAINT FK_CartItem_Product REFERENCES dbo.Product(product_id),
    variant_id int NOT NULL CONSTRAINT FK_CartItem_Variant REFERENCES dbo.ProductVariant(variant_id),
    quantity int NOT NULL,
    unit_price decimal(18,2) NOT NULL,
    modifiers_json nvarchar(max) NOT NULL CONSTRAINT DF_CartItem_Modifiers DEFAULT N'[]',
    created_at datetime2(0) NOT NULL CONSTRAINT DF_CartItem_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_CartItem_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_CartItem_Quantity CHECK (quantity > 0),
    CONSTRAINT CK_CartItem_Price CHECK (unit_price >= 0)
);

CREATE TABLE dbo.Coupon (
    coupon_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Coupon PRIMARY KEY,
    code varchar(50) NOT NULL CONSTRAINT UQ_Coupon_Code UNIQUE,
    type varchar(20) NOT NULL,
    value decimal(18,2) NOT NULL,
    min_order decimal(18,2) NOT NULL CONSTRAINT DF_Coupon_MinOrder DEFAULT 0,
    max_discount decimal(18,2) NULL,
    max_uses int NOT NULL CONSTRAINT DF_Coupon_MaxUses DEFAULT 0,
    used_count int NOT NULL CONSTRAINT DF_Coupon_UsedCount DEFAULT 0,
    expires_at datetime2(0) NULL,
    is_active bit NOT NULL CONSTRAINT DF_Coupon_Active DEFAULT 1,
    is_public bit NOT NULL CONSTRAINT DF_Coupon_Public DEFAULT 1,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Coupon_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Coupon_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Coupon_Type CHECK (type IN ('PERCENT', 'FIXED', 'FREE_SHIPPING')),
    CONSTRAINT CK_Coupon_Amounts CHECK (value >= 0 AND min_order >= 0 AND (max_discount IS NULL OR max_discount >= 0)),
    CONSTRAINT CK_Coupon_Usage CHECK (max_uses >= 0 AND used_count >= 0 AND (max_uses = 0 OR used_count <= max_uses))
);

CREATE TABLE dbo.Orders (
    order_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Orders PRIMARY KEY,
    order_code varchar(50) NOT NULL CONSTRAINT UQ_Orders_Code UNIQUE,
    idempotency_key varchar(100) NULL,
    request_hash varchar(64) NULL,
    idempotency_owner varchar(100) NULL,
    user_id int NULL CONSTRAINT FK_Orders_User REFERENCES dbo.Users(user_id),
    customer_name nvarchar(255) NOT NULL,
    customer_phone varchar(20) NOT NULL,
    customer_address nvarchar(500) NOT NULL,
    to_province_name nvarchar(100) NULL,
    to_district_name nvarchar(100) NULL,
    to_ward_name nvarchar(100) NULL,
    ghn_province_id int NULL,
    ghn_district_id int NULL,
    ghn_ward_code varchar(50) NULL,
    total_amount decimal(18,2) NOT NULL,
    shipping_fee decimal(18,2) NOT NULL CONSTRAINT DF_Orders_ShippingFee DEFAULT 0,
    service_fee decimal(18,2) NOT NULL CONSTRAINT DF_Orders_ServiceFee DEFAULT 0,
    final_amount decimal(18,2) NOT NULL,
    cod_collected_amount decimal(18,2) NULL,
    cod_collected_at datetime2(0) NULL,
    shipping_provider varchar(30) NOT NULL CONSTRAINT DF_Orders_ShippingProvider DEFAULT 'GHN',
    shipping_service_id int NULL,
    shipping_service_type_id int NULL,
    expected_delivery_time datetime2(0) NULL,
    ghn_order_code varchar(50) NULL,
    ghn_tracking_url varchar(500) NULL,
    ghn_status varchar(30) NULL,
    from_district_id int NULL,
    from_ward_code varchar(50) NULL,
    payment_method varchar(50) NOT NULL,
    payment_status varchar(20) NOT NULL CONSTRAINT DF_Orders_PaymentStatus DEFAULT 'UNPAID',
    payos_payment_link_id varchar(100) NULL,
    payos_checkout_url varchar(500) NULL,
    guest_return_proof_hash varchar(64) NULL,
    order_status varchar(30) NOT NULL CONSTRAINT DF_Orders_Status DEFAULT 'PENDING',
    staff_id int NULL CONSTRAINT FK_Orders_Staff REFERENCES dbo.Users(user_id),
    shipper_id int NULL CONSTRAINT FK_Orders_Shipper REFERENCES dbo.Users(user_id),
    assigned_at datetime2(0) NULL,
    confirmed_at datetime2(0) NULL,
    ready_at datetime2(0) NULL,
    picked_up_at datetime2(0) NULL,
    paid_at datetime2(0) NULL,
    delivered_at datetime2(0) NULL,
    cancelled_at datetime2(0) NULL,
    failure_reason nvarchar(500) NULL,
    cancelled_by varchar(20) NULL,
    refund_status varchar(20) NULL,
    refund_amount decimal(18,2) NULL,
    refunded_at datetime2(0) NULL,
    refund_note nvarchar(500) NULL,
    internal_note nvarchar(1000) NULL,
    coupon_code varchar(50) NULL,
    discount_amount decimal(18,2) NOT NULL CONSTRAINT DF_Orders_Discount DEFAULT 0,
    delivery_note nvarchar(500) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Orders_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Orders_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Orders_Amounts CHECK (total_amount >= 0 AND shipping_fee >= 0 AND service_fee >= 0 AND final_amount >= 0 AND discount_amount >= 0 AND (cod_collected_amount IS NULL OR cod_collected_amount >= 0) AND (refund_amount IS NULL OR refund_amount >= 0)),
    CONSTRAINT CK_Orders_PaymentMethod CHECK (payment_method IN ('COD', 'BANK_TRANSFER')),
    CONSTRAINT CK_Orders_PaymentStatus CHECK (payment_status IN ('UNPAID', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT CK_Orders_GuestReturnProofHash CHECK (guest_return_proof_hash IS NULL OR (LEN(guest_return_proof_hash)=64 AND guest_return_proof_hash NOT LIKE '%[^0-9a-f]%')),
    CONSTRAINT CK_Orders_Status CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_Orders_CancelledBy CHECK (cancelled_by IS NULL OR cancelled_by IN ('CUSTOMER', 'USER', 'STAFF', 'ADMIN', 'SYSTEM')),
    CONSTRAINT CK_Orders_RefundStatus CHECK (refund_status IS NULL OR refund_status IN ('PENDING', 'REFUNDED', 'REJECTED'))
);

CREATE TABLE dbo.PaymentAttempt (
    payment_attempt_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_PaymentAttempt PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_PaymentAttempt_Order REFERENCES dbo.Orders(order_id),
    provider varchar(20) NOT NULL,
    provider_reference varchar(100) NULL,
    checkout_url varchar(500) NULL,
    amount decimal(18,2) NOT NULL,
    status varchar(20) NOT NULL,
    lease_token varchar(36) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_PaymentAttempt_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_PaymentAttempt_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_PaymentAttempt_Order UNIQUE (order_id),
    CONSTRAINT CK_PaymentAttempt_Amount CHECK (amount >= 0),
    CONSTRAINT CK_PaymentAttempt_Status CHECK (status IN ('CREATING', 'READY', 'PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE dbo.InventoryReservation (
    reservation_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservation PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_InventoryReservation_Order REFERENCES dbo.Orders(order_id),
    variant_id int NOT NULL CONSTRAINT FK_InventoryReservation_Variant REFERENCES dbo.ProductVariant(variant_id),
    quantity int NOT NULL,
    status varchar(20) NOT NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_InventoryReservation_OrderVariant UNIQUE (order_id, variant_id),
    CONSTRAINT CK_InventoryReservation_Quantity CHECK (quantity > 0),
    CONSTRAINT CK_InventoryReservation_Status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'WASTED'))
);

CREATE TABLE dbo.InventoryTransaction (
    inventory_transaction_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryTransaction PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Order REFERENCES dbo.Orders(order_id),
    variant_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Variant REFERENCES dbo.ProductVariant(variant_id),
    transaction_type varchar(20) NOT NULL,
    quantity int NOT NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryTransaction_Created DEFAULT GETDATE(),
    CONSTRAINT CK_InventoryTransaction_Quantity CHECK (quantity > 0),
    CONSTRAINT CK_InventoryTransaction_Type CHECK (transaction_type IN ('RESERVE', 'RELEASE', 'CONSUME', 'WASTE', 'RETURN', 'ADJUSTMENT'))
);

CREATE TABLE dbo.LoyaltyTransaction (
    loyalty_transaction_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoyaltyTransaction PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_LoyaltyTransaction_User REFERENCES dbo.Users(user_id),
    order_id int NOT NULL CONSTRAINT FK_LoyaltyTransaction_Order REFERENCES dbo.Orders(order_id),
    transaction_type varchar(20) NOT NULL,
    points int NOT NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_LoyaltyTransaction_Created DEFAULT GETDATE(),
    CONSTRAINT UQ_LoyaltyTransaction_OrderType UNIQUE (order_id, transaction_type),
    CONSTRAINT CK_LoyaltyTransaction_Type CHECK (transaction_type IN ('EARN', 'REDEEM', 'REVERSE', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT CK_LoyaltyTransaction_Points CHECK (points <> 0)
);

CREATE TABLE dbo.WorkShift (
    shift_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_WorkShift PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_WorkShift_User REFERENCES dbo.Users(user_id),
    shift_date date NOT NULL,
    start_time time(0) NOT NULL,
    end_time time(0) NOT NULL,
    check_in_at datetime2(0) NULL,
    check_out_at datetime2(0) NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_WorkShift_Status DEFAULT 'SCHEDULED',
    created_at datetime2(0) NOT NULL CONSTRAINT DF_WorkShift_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_WorkShift_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_WorkShift_Time CHECK (start_time < end_time),
    CONSTRAINT CK_WorkShift_Status CHECK (status IN ('SCHEDULED', 'CHECKED_IN', 'CHECKED_OUT', 'ABSENT', 'CANCELLED')),
    CONSTRAINT CK_WorkShift_CheckTimes CHECK (check_out_at IS NULL OR (check_in_at IS NOT NULL AND check_out_at >= check_in_at))
);

CREATE TABLE dbo.CouponRedemption (
    redemption_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CouponRedemption PRIMARY KEY,
    coupon_id int NOT NULL CONSTRAINT FK_CouponRedemption_Coupon REFERENCES dbo.Coupon(coupon_id),
    user_id int NOT NULL CONSTRAINT FK_CouponRedemption_User REFERENCES dbo.Users(user_id),
    order_id int NULL CONSTRAINT FK_CouponRedemption_Order REFERENCES dbo.Orders(order_id),
    claimed_at datetime2(0) NOT NULL CONSTRAINT DF_CouponRedemption_Claimed DEFAULT GETDATE(),
    used_at datetime2(0) NULL,
    discount_amount decimal(18,2) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_CouponRedemption_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_CouponRedemption_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_CouponRedemption_UserCoupon UNIQUE (user_id, coupon_id),
    CONSTRAINT CK_CouponRedemption_Discount CHECK (discount_amount IS NULL OR discount_amount >= 0)
);

CREATE TABLE dbo.OrderItem (
    order_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_OrderItem PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_OrderItem_Order REFERENCES dbo.Orders(order_id),
    product_id int NULL CONSTRAINT FK_OrderItem_Product REFERENCES dbo.Product(product_id),
    variant_id int NULL CONSTRAINT FK_OrderItem_Variant REFERENCES dbo.ProductVariant(variant_id),
    product_name nvarchar(255) NOT NULL,
    variant_name nvarchar(255) NULL,
    quantity int NOT NULL,
    unit_price decimal(18,2) NOT NULL,
    total_price decimal(18,2) NOT NULL,
    modifiers_json nvarchar(max) NOT NULL CONSTRAINT DF_OrderItem_Modifiers DEFAULT N'[]',
    CONSTRAINT CK_OrderItem_Quantity CHECK (quantity > 0),
    CONSTRAINT CK_OrderItem_Amounts CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE TABLE dbo.Review (
    review_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Review PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_Review_User REFERENCES dbo.Users(user_id),
    order_id int NOT NULL CONSTRAINT FK_Review_Order REFERENCES dbo.Orders(order_id),
    rating int NOT NULL,
    comment nvarchar(1000) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Review_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Review_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_Review_UserOrder UNIQUE (user_id, order_id),
    CONSTRAINT CK_Review_Rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE dbo.SupportTicket (
    ticket_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_SupportTicket PRIMARY KEY,
    user_id int NULL CONSTRAINT FK_SupportTicket_User REFERENCES dbo.Users(user_id),
    order_id int NULL CONSTRAINT FK_SupportTicket_Order REFERENCES dbo.Orders(order_id),
    subject nvarchar(255) NOT NULL,
    category varchar(30) NOT NULL,
    description nvarchar(2000) NOT NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_SupportTicket_Status DEFAULT 'OPEN',
    staff_id int NULL CONSTRAINT FK_SupportTicket_Staff REFERENCES dbo.Users(user_id),
    resolution nvarchar(2000) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_SupportTicket_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_SupportTicket_Updated DEFAULT GETDATE(),
    resolved_at datetime2(0) NULL,
    CONSTRAINT CK_SupportTicket_Category CHECK (category IN ('MISSING_ITEM', 'COLD_FOOD', 'WRONG_ITEM', 'LATE_DELIVERY', 'OTHER')),
    CONSTRAINT CK_SupportTicket_Status CHECK (status IN ('OPEN', 'PROCESSING', 'RESOLVED'))
);

CREATE TABLE dbo.Notification (
    notification_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notification PRIMARY KEY,
    user_id int NULL CONSTRAINT FK_Notification_User REFERENCES dbo.Users(user_id),
    role_name varchar(50) NULL,
    title nvarchar(255) NOT NULL,
    message nvarchar(1000) NULL,
    type varchar(50) NULL,
    target_url varchar(500) NULL,
    is_read bit NOT NULL CONSTRAINT DF_Notification_Read DEFAULT 0,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Notification_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Notification_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Notification_Target CHECK (user_id IS NOT NULL OR role_name IS NOT NULL),
    CONSTRAINT CK_Notification_Role CHECK (role_name IS NULL OR role_name IN ('ADMIN', 'STAFF', 'SHIPPER', 'USER'))
);

CREATE TABLE dbo.OrderStatusHistory (
    history_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_OrderStatusHistory PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_OrderStatusHistory_Order REFERENCES dbo.Orders(order_id),
    actor_user_id int NULL CONSTRAINT FK_OrderStatusHistory_Actor REFERENCES dbo.Users(user_id),
    actor_role varchar(50) NULL,
    from_status varchar(30) NULL,
    to_status varchar(30) NOT NULL,
    note nvarchar(500) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_OrderStatusHistory_Created DEFAULT GETDATE(),
    CONSTRAINT CK_OrderStatusHistory_From CHECK (from_status IS NULL OR from_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_OrderStatusHistory_To CHECK (to_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_OrderStatusHistory_Role CHECK (actor_role IS NULL OR actor_role IN ('ADMIN', 'STAFF', 'SHIPPER', 'USER', 'GUEST', 'SYSTEM', 'PAYOS'))
);
GO

CREATE UNIQUE INDEX UX_ProductVariant_Sku ON dbo.ProductVariant(sku) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX UX_ProductVariant_Default ON dbo.ProductVariant(product_id) WHERE is_default = 1;
CREATE UNIQUE INDEX UX_Users_Email ON dbo.Users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_Users_Phone ON dbo.Users(phone);
CREATE UNIQUE INDEX UX_Address_Default ON dbo.Address(user_id) WHERE is_default = 1;
CREATE UNIQUE INDEX UX_Cart_User ON dbo.Cart(user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX UX_Cart_Session ON dbo.Cart(session_id) WHERE session_id IS NOT NULL;
CREATE UNIQUE INDEX UX_Orders_Idempotency ON dbo.Orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX UX_CouponRedemption_Order ON dbo.CouponRedemption(order_id) WHERE order_id IS NOT NULL;

CREATE INDEX IX_Product_Category ON dbo.Product(category_id);
CREATE INDEX IX_ProductVariant_Product ON dbo.ProductVariant(product_id);
CREATE INDEX IX_ProductModifierGroup_Product ON dbo.ProductModifierGroup(product_id);
CREATE INDEX IX_ProductModifierOption_Group ON dbo.ProductModifierOption(modifier_group_id);
CREATE INDEX IX_ProductComboItem_Combo ON dbo.ProductComboItem(combo_id);
CREATE INDEX IX_ProductComboItem_Product ON dbo.ProductComboItem(product_id);
CREATE INDEX IX_ProductComboItem_Variant ON dbo.ProductComboItem(variant_id);
CREATE INDEX IX_PasswordResetToken_User ON dbo.PasswordResetToken(user_id);
CREATE INDEX IX_Address_User ON dbo.Address(user_id);
CREATE INDEX IX_CartItem_Cart ON dbo.CartItem(cart_id);
CREATE INDEX IX_CartItem_Product ON dbo.CartItem(product_id);
CREATE INDEX IX_CartItem_Variant ON dbo.CartItem(variant_id);
CREATE INDEX IX_Orders_User ON dbo.Orders(user_id);
CREATE INDEX IX_Orders_Staff_Status ON dbo.Orders(staff_id, order_status);
CREATE INDEX IX_Orders_Shipper_Status ON dbo.Orders(shipper_id, order_status);
CREATE INDEX IX_Orders_Status_Created ON dbo.Orders(order_status, created_at);
CREATE INDEX IX_InventoryReservation_Variant ON dbo.InventoryReservation(variant_id);
CREATE INDEX IX_InventoryTransaction_Order ON dbo.InventoryTransaction(order_id);
CREATE INDEX IX_InventoryTransaction_Variant ON dbo.InventoryTransaction(variant_id);
CREATE INDEX IX_LoyaltyTransaction_User_Created ON dbo.LoyaltyTransaction(user_id, created_at);
CREATE INDEX IX_WorkShift_User_Date ON dbo.WorkShift(user_id, shift_date);
CREATE INDEX IX_WorkShift_Date_Status ON dbo.WorkShift(shift_date, status);
CREATE INDEX IX_CouponRedemption_Coupon ON dbo.CouponRedemption(coupon_id);
CREATE INDEX IX_OrderItem_Order ON dbo.OrderItem(order_id);
CREATE INDEX IX_OrderItem_Product ON dbo.OrderItem(product_id);
CREATE INDEX IX_OrderItem_Variant ON dbo.OrderItem(variant_id);
CREATE INDEX IX_Review_Order ON dbo.Review(order_id);
CREATE INDEX IX_SupportTicket_User_Created ON dbo.SupportTicket(user_id, created_at);
CREATE INDEX IX_SupportTicket_Order ON dbo.SupportTicket(order_id);
CREATE INDEX IX_SupportTicket_Staff_Status ON dbo.SupportTicket(staff_id, status);
CREATE INDEX IX_Notification_User_Read ON dbo.Notification(user_id, is_read, created_at);
CREATE INDEX IX_Notification_Role_Read ON dbo.Notification(role_name, is_read, created_at);
CREATE INDEX IX_OrderStatusHistory_Order_Created ON dbo.OrderStatusHistory(order_id, created_at);
CREATE INDEX IX_OrderStatusHistory_Actor ON dbo.OrderStatusHistory(actor_user_id);
GO

SET NOCOUNT ON;
BEGIN TRY
BEGIN TRANSACTION;

SET IDENTITY_INSERT dbo.Category ON;
INSERT dbo.Category (category_id, name, description, sort_order, status) VALUES
    (1, N'Burger', N'Burger tuoi lam tai bep', 1, 'ACTIVE'),
    (2, N'Mon an kem', N'Mon an kem va nuoc uong', 2, 'ACTIVE'),
    (3, N'Combo', N'Combo tiet kiem', 3, 'ACTIVE');
SET IDENTITY_INSERT dbo.Category OFF;

SET IDENTITY_INSERT dbo.Product ON;
INSERT dbo.Product (product_id, category_id, name, description, base_price, image_url, gallery_images, status, available_from, available_to, created_at, updated_at) VALUES
    (1, 1, N'Classic Burger', N'Bo nuong, rau va sot dac biet', 59000, '/images/products/classic-burger.jpg', N'[]', 'AVAILABLE', '08:00', '22:00', DATEADD(day, -30, GETDATE()), GETDATE()),
    (2, 2, N'Khoai tay chien', N'Khoai tay chien gion', 25000, '/images/products/fries.jpg', N'[]', 'AVAILABLE', NULL, NULL, DATEADD(day, -30, GETDATE()), GETDATE()),
    (3, 2, N'Cola', N'Nuoc ngot co gas', 15000, '/images/products/cola.jpg', N'[]', 'AVAILABLE', NULL, NULL, DATEADD(day, -30, GETDATE()), GETDATE()),
    (4, 3, N'Combo Burger', N'Burger, khoai tay va cola', 89000, '/images/products/combo-burger.jpg', N'[]', 'AVAILABLE', NULL, NULL, DATEADD(day, -20, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Product OFF;

SET IDENTITY_INSERT dbo.ProductVariant ON;
INSERT dbo.ProductVariant (variant_id, product_id, variant_name, price, original_price, sku, quantity_available, weight, length, width, height, is_default, status, created_at, updated_at) VALUES
    (1, 1, N'Tieu chuan', 59000, NULL, 'BURGER-STD', 96, 500, 20, 20, 10, 1, 'AVAILABLE', DATEADD(day, -30, GETDATE()), GETDATE()),
    (2, 1, N'Lon', 69000, 79000, 'BURGER-L', 79, 650, 22, 22, 12, 0, 'AVAILABLE', DATEADD(day, -30, GETDATE()), GETDATE()),
    (3, 2, N'Tieu chuan', 25000, NULL, 'FRIES-STD', 150, 250, 15, 10, 8, 1, 'AVAILABLE', DATEADD(day, -30, GETDATE()), GETDATE()),
    (4, 3, N'Lon', 15000, NULL, 'COLA-L', 200, 500, 8, 8, 18, 1, 'AVAILABLE', DATEADD(day, -30, GETDATE()), GETDATE()),
    (5, 4, N'Tieu chuan', 89000, 99000, 'COMBO-BURGER', 58, 1250, 30, 25, 15, 1, 'AVAILABLE', DATEADD(day, -20, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.ProductVariant OFF;

SET IDENTITY_INSERT dbo.ProductModifierGroup ON;
INSERT dbo.ProductModifierGroup (modifier_group_id, product_id, name, min_selections, max_selections, is_active, sort_order) VALUES
    (1, 1, N'Pho mai', 0, 2, 1, 1),
    (2, 1, N'Sot', 1, 1, 1, 2);
SET IDENTITY_INSERT dbo.ProductModifierGroup OFF;

SET IDENTITY_INSERT dbo.ProductModifierOption ON;
INSERT dbo.ProductModifierOption (modifier_option_id, modifier_group_id, name, price, is_active, sort_order) VALUES
    (1, 1, N'Pho mai cheddar', 10000, 1, 1),
    (2, 1, N'Pho mai mozzarella', 12000, 1, 2),
    (3, 2, N'Sot dac biet', 0, 1, 1),
    (4, 2, N'Sot cay', 0, 1, 2);
SET IDENTITY_INSERT dbo.ProductModifierOption OFF;

SET IDENTITY_INSERT dbo.ProductCombo ON;
INSERT dbo.ProductCombo (combo_id, product_id, is_active) VALUES (1, 4, 1);
SET IDENTITY_INSERT dbo.ProductCombo OFF;

SET IDENTITY_INSERT dbo.ProductComboItem ON;
INSERT dbo.ProductComboItem (combo_item_id, combo_id, product_id, variant_id, quantity, sort_order) VALUES
    (1, 1, 1, 1, 1, 1), (2, 1, 2, 3, 1, 2), (3, 1, 3, 4, 1, 3);
SET IDENTITY_INSERT dbo.ProductComboItem OFF;

INSERT dbo.ShippingConfig (config_key, config_value) VALUES
    ('ghn_from_district_id', '1442'), ('ghn_from_ward_code', '20107'),
    ('default_weight', '500'), ('default_length', '20'), ('default_width', '20'), ('default_height', '10'),
    ('default_service_type_id', '2'), ('business_open_time', '00:00'), ('business_close_time', '00:00'), ('service_fee', '0');

-- Demo password for every account: 123456. Hash format is the PBKDF2 format used by utils.PasswordUtil.
DECLARE @DemoPasswordHash varchar(255) = 'pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=';
SET IDENTITY_INSERT dbo.Users ON;
INSERT dbo.Users (user_id, role_name, email, phone, password_hash, full_name, avatar_url, status, loyalty_points, favorite_ids_json, created_at, updated_at) VALUES
    (1, 'ADMIN', 'admin@fastguy.local', '0901000001', @DemoPasswordHash, N'FastGuy Admin', NULL, 'ACTIVE', 0, N'[]', DATEADD(day, -90, GETDATE()), GETDATE()),
    (2, 'STAFF', 'staff@fastguy.local', '0901000002', @DemoPasswordHash, N'FastGuy Staff', NULL, 'ACTIVE', 0, N'[]', DATEADD(day, -60, GETDATE()), GETDATE()),
    (3, 'SHIPPER', 'shipper@fastguy.local', '0901000003', @DemoPasswordHash, N'FastGuy Shipper', NULL, 'ACTIVE', 0, N'[]', DATEADD(day, -60, GETDATE()), GETDATE()),
    (4, 'USER', 'user@fastguy.local', '0901000004', @DemoPasswordHash, N'FastGuy Customer', NULL, 'ACTIVE', 250, N'[{"productId":1,"createdAt":"2026-08-02T00:00:00"},{"productId":4,"createdAt":"2026-08-02T00:00:00"}]', DATEADD(day, -45, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Users OFF;

SET IDENTITY_INSERT dbo.Banner ON;
INSERT dbo.Banner (banner_id, title, subtitle, image_url, link, sort_order, is_active, created_at, updated_at) VALUES
    (1, N'Combo moi moi ngay', N'Thu combo burger gia uu dai', '/images/banners/combo.jpg', '/menu?category=3', 1, 1, DATEADD(day, -7, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Banner OFF;

SET IDENTITY_INSERT dbo.Address ON;
INSERT dbo.Address (address_id, user_id, recipient_name, phone, street, ward_name, district_name, province_name, ghn_province_id, ghn_district_id, ghn_ward_code, city, is_default, created_at, updated_at) VALUES
    (1, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue', N'Ben Nghe', N'Quan 1', N'TP. Ho Chi Minh', 202, 1442, '20107', N'TP. Ho Chi Minh', 1, DATEADD(day, -30, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Address OFF;

SET IDENTITY_INSERT dbo.Cart ON;
INSERT dbo.Cart (cart_id, user_id, session_id, created_at, updated_at) VALUES (1, 4, NULL, DATEADD(day, -2, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Cart OFF;

SET IDENTITY_INSERT dbo.CartItem ON;
INSERT dbo.CartItem (cart_item_id, cart_id, product_id, variant_id, quantity, unit_price, modifiers_json, created_at, updated_at) VALUES
    (1, 1, 1, 1, 1, 69000, N'[{"modifierOptionId":1,"groupId":1,"groupName":"Pho mai","name":"Pho mai cheddar","price":10000},{"modifierOptionId":3,"groupId":2,"groupName":"Sot","name":"Sot dac biet","price":0}]', DATEADD(hour, -2, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.CartItem OFF;

SET IDENTITY_INSERT dbo.Coupon ON;
INSERT dbo.Coupon (coupon_id, code, type, value, min_order, max_discount, max_uses, used_count, expires_at, is_active, is_public, created_at, updated_at) VALUES
    (1, 'WELCOME10', 'PERCENT', 10, 50000, 20000, 100, 1, DATEADD(day, 90, GETDATE()), 1, 1, DATEADD(day, -10, GETDATE()), GETDATE()),
    (2, 'FREESHIP', 'FREE_SHIPPING', 0, 30000, 30000, 100, 0, DATEADD(day, 60, GETDATE()), 1, 1, DATEADD(day, -10, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Coupon OFF;

SET IDENTITY_INSERT dbo.WorkShift ON;
INSERT dbo.WorkShift (shift_id, user_id, shift_date, start_time, end_time, check_in_at, check_out_at, status, created_at, updated_at) VALUES
    (1, 2, CAST(GETDATE() AS date), '08:00', '16:00', CASE WHEN CAST(GETDATE() AS time) >= '08:00' THEN DATEADD(hour, 8, CAST(CAST(GETDATE() AS date) AS datetime2)) END, CASE WHEN CAST(GETDATE() AS time) > '16:00' THEN DATEADD(hour, 16, CAST(CAST(GETDATE() AS date) AS datetime2)) END, CASE WHEN CAST(GETDATE() AS time) < '08:00' THEN 'SCHEDULED' WHEN CAST(GETDATE() AS time) <= '16:00' THEN 'CHECKED_IN' ELSE 'CHECKED_OUT' END, DATEADD(day, -1, GETDATE()), GETDATE()),
    (2, 3, CAST(GETDATE() AS date), '14:00', '22:00', CASE WHEN CAST(GETDATE() AS time) >= '14:00' THEN DATEADD(hour, 14, CAST(CAST(GETDATE() AS date) AS datetime2)) END, CASE WHEN CAST(GETDATE() AS time) > '22:00' THEN DATEADD(hour, 22, CAST(CAST(GETDATE() AS date) AS datetime2)) END, CASE WHEN CAST(GETDATE() AS time) < '14:00' THEN 'SCHEDULED' WHEN CAST(GETDATE() AS time) <= '22:00' THEN 'CHECKED_IN' ELSE 'CHECKED_OUT' END, DATEADD(day, -1, GETDATE()), GETDATE()),
    (3, 2, DATEADD(day, 1, CAST(GETDATE() AS date)), '16:00', '22:00', NULL, NULL, 'SCHEDULED', GETDATE(), GETDATE());
SET IDENTITY_INSERT dbo.WorkShift OFF;

SET IDENTITY_INSERT dbo.Orders ON;
INSERT dbo.Orders (order_id, order_code, idempotency_key, request_hash, idempotency_owner, user_id, customer_name, customer_phone, customer_address, to_province_name, to_district_name, to_ward_name, total_amount, shipping_fee, service_fee, final_amount, cod_collected_amount, cod_collected_at, shipping_provider, expected_delivery_time, payment_method, payment_status, payos_payment_link_id, payos_checkout_url, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, paid_at, delivered_at, cancelled_at, failure_reason, cancelled_by, coupon_code, discount_amount, delivery_note, created_at, updated_at) VALUES
    (1, 'FG-DEMO-001', 'demo-order-001', REPLICATE('a',64), 'USER:4', 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 59000, 15000, 0, 74000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'BANK_TRANSFER', 'UNPAID', 'DEMO-REFERENCE-001', 'https://pay.payos.vn/web/demo', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, N'Giao tai le tan', DATEADD(minute, -20, GETDATE()), GETDATE()),
    (2, 'FG-DEMO-002', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 69000, 15000, 0, 84000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'CONFIRMED', 2, NULL, NULL, DATEADD(minute, -25, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(minute, -30, GETDATE()), GETDATE()),
    (3, 'FG-DEMO-003', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 89000, 15000, 0, 104000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'PREPARING', 2, NULL, NULL, DATEADD(minute, -40, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(minute, -45, GETDATE()), GETDATE()),
    (4, 'FG-DEMO-004', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 59000, 15000, 0, 74000, NULL, NULL, 'GHN', DATEADD(minute, 45, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'READY', 2, NULL, NULL, DATEADD(hour, -1, GETDATE()), DATEADD(minute, -15, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(hour, -2, GETDATE()), GETDATE()),
    (5, 'FG-DEMO-005', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 89000, 15000, 0, 104000, NULL, NULL, 'GHN', DATEADD(minute, 30, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'ASSIGNED', 2, 3, DATEADD(minute, -10, GETDATE()), DATEADD(hour, -2, GETDATE()), DATEADD(minute, -20, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(hour, -3, GETDATE()), GETDATE()),
    (6, 'FG-DEMO-006', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 59000, 15000, 0, 74000, NULL, NULL, 'GHN', DATEADD(minute, 15, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'PICKED_UP', 2, 3, DATEADD(minute, -30, GETDATE()), DATEADD(hour, -3, GETDATE()), DATEADD(hour, -1, GETDATE()), DATEADD(minute, -20, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(hour, -4, GETDATE()), GETDATE()),
    (7, 'FG-DEMO-007', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 79000, 15000, 0, 74000, 74000, DATEADD(day, -1, GETDATE()), 'GHN', DATEADD(day, -1, GETDATE()), 'COD', 'PAID', NULL, NULL, 'DELIVERED', 2, 3, DATEADD(day, -1, GETDATE()), DATEADD(day, -1, GETDATE()), DATEADD(day, -1, GETDATE()), DATEADD(day, -1, GETDATE()), DATEADD(day, -1, GETDATE()), DATEADD(day, -1, GETDATE()), NULL, NULL, NULL, 'WELCOME10', 20000, NULL, DATEADD(day, -2, GETDATE()), DATEADD(day, -1, GETDATE())),
    (8, 'FG-DEMO-008', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 25000, 15000, 0, 40000, NULL, NULL, 'GHN', NULL, 'COD', 'UNPAID', NULL, NULL, 'CANCELLED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(day, -3, GETDATE()), N'Khach hang doi y', 'CUSTOMER', NULL, 0, NULL, DATEADD(day, -3, GETDATE()), DATEADD(day, -3, GETDATE()));
SET IDENTITY_INSERT dbo.Orders OFF;

SET IDENTITY_INSERT dbo.PaymentAttempt ON;
INSERT dbo.PaymentAttempt (payment_attempt_id, order_id, provider, provider_reference, checkout_url, amount, status, lease_token, created_at, updated_at) VALUES
    (1, 1, 'PAYOS', 'DEMO-REFERENCE-001', 'https://pay.payos.vn/web/demo', 74000, 'READY', NULL, DATEADD(minute, -19, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.PaymentAttempt OFF;

SET IDENTITY_INSERT dbo.CouponRedemption ON;
INSERT dbo.CouponRedemption (redemption_id, coupon_id, user_id, order_id, claimed_at, used_at, discount_amount, created_at, updated_at) VALUES
    (1, 1, 4, 7, DATEADD(day, -5, GETDATE()), DATEADD(day, -2, GETDATE()), 20000, DATEADD(day, -5, GETDATE()), DATEADD(day, -2, GETDATE())),
    (2, 2, 4, NULL, DATEADD(day, -1, GETDATE()), NULL, NULL, DATEADD(day, -1, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.CouponRedemption OFF;

SET IDENTITY_INSERT dbo.OrderItem ON;
INSERT dbo.OrderItem (order_item_id, order_id, product_id, variant_id, product_name, variant_name, quantity, unit_price, total_price, modifiers_json) VALUES
    (1, 1, 1, 1, N'Classic Burger', N'Tieu chuan', 1, 59000, 59000, N'[{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]'),
    (2, 2, 1, 1, N'Classic Burger', N'Tieu chuan', 1, 69000, 69000, N'[{"modifierOptionId":1,"groupName":"Pho mai","optionName":"Pho mai cheddar","price":10000},{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]'),
    (3, 3, 4, 5, N'Combo Burger', N'Tieu chuan', 1, 89000, 89000, N'[]'),
    (4, 4, 1, 1, N'Classic Burger', N'Tieu chuan', 1, 59000, 59000, N'[{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]'),
    (5, 5, 4, 5, N'Combo Burger', N'Tieu chuan', 1, 89000, 89000, N'[]'),
    (6, 6, 1, 1, N'Classic Burger', N'Tieu chuan', 1, 59000, 59000, N'[{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]'),
    (7, 7, 1, 2, N'Classic Burger', N'Lon', 1, 79000, 79000, N'[{"modifierOptionId":1,"groupName":"Pho mai","optionName":"Pho mai cheddar","price":10000},{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]'),
    (8, 8, 2, 3, N'Khoai tay chien', N'Tieu chuan', 1, 25000, 25000, N'[]');
SET IDENTITY_INSERT dbo.OrderItem OFF;

SET IDENTITY_INSERT dbo.InventoryReservation ON;
INSERT dbo.InventoryReservation (reservation_id, order_id, variant_id, quantity, status, created_at, updated_at) VALUES
    (1, 1, 1, 1, 'RESERVED', DATEADD(minute, -20, GETDATE()), GETDATE()),
    (2, 2, 1, 1, 'RESERVED', DATEADD(minute, -30, GETDATE()), GETDATE()),
    (3, 3, 5, 1, 'CONSUMED', DATEADD(minute, -45, GETDATE()), GETDATE()),
    (4, 4, 1, 1, 'CONSUMED', DATEADD(hour, -2, GETDATE()), GETDATE()),
    (5, 5, 5, 1, 'CONSUMED', DATEADD(hour, -3, GETDATE()), GETDATE()),
    (6, 6, 1, 1, 'CONSUMED', DATEADD(hour, -4, GETDATE()), GETDATE()),
    (7, 7, 2, 1, 'CONSUMED', DATEADD(day, -2, GETDATE()), DATEADD(day, -1, GETDATE())),
    (8, 8, 3, 1, 'RELEASED', DATEADD(day, -3, GETDATE()), DATEADD(day, -3, GETDATE()));
SET IDENTITY_INSERT dbo.InventoryReservation OFF;

SET IDENTITY_INSERT dbo.InventoryTransaction ON;
INSERT dbo.InventoryTransaction (inventory_transaction_id, order_id, variant_id, transaction_type, quantity, created_at) VALUES
    (1, 1, 1, 'RESERVE', 1, DATEADD(minute, -20, GETDATE())),
    (2, 2, 1, 'RESERVE', 1, DATEADD(minute, -30, GETDATE())),
    (3, 3, 5, 'RESERVE', 1, DATEADD(minute, -45, GETDATE())), (4, 3, 5, 'CONSUME', 1, DATEADD(minute, -35, GETDATE())),
    (5, 4, 1, 'RESERVE', 1, DATEADD(hour, -2, GETDATE())), (6, 4, 1, 'CONSUME', 1, DATEADD(hour, -1, GETDATE())),
    (7, 5, 5, 'RESERVE', 1, DATEADD(hour, -3, GETDATE())), (8, 5, 5, 'CONSUME', 1, DATEADD(hour, -2, GETDATE())),
    (9, 6, 1, 'RESERVE', 1, DATEADD(hour, -4, GETDATE())), (10, 6, 1, 'CONSUME', 1, DATEADD(hour, -3, GETDATE())),
    (11, 7, 2, 'RESERVE', 1, DATEADD(day, -2, GETDATE())), (12, 7, 2, 'CONSUME', 1, DATEADD(day, -2, GETDATE())),
    (13, 8, 3, 'RESERVE', 1, DATEADD(day, -3, GETDATE())), (14, 8, 3, 'RELEASE', 1, DATEADD(day, -3, GETDATE()));
SET IDENTITY_INSERT dbo.InventoryTransaction OFF;

SET IDENTITY_INSERT dbo.OrderStatusHistory ON;
INSERT dbo.OrderStatusHistory (history_id, order_id, actor_user_id, actor_role, from_status, to_status, note, created_at) VALUES
    (1, 1, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(minute, -20, GETDATE())),
    (2, 2, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(minute, -30, GETDATE())),
    (3, 2, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(minute, -25, GETDATE())),
    (4, 3, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(minute, -45, GETDATE())),
    (5, 3, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(minute, -40, GETDATE())),
    (6, 3, 2, 'STAFF', 'CONFIRMED', 'PREPARING', N'Bep dang chuan bi', DATEADD(minute, -35, GETDATE())),
    (7, 4, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(hour, -2, GETDATE())),
    (8, 4, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(hour, -1, GETDATE())),
    (9, 4, 2, 'STAFF', 'CONFIRMED', 'PREPARING', N'Bep dang chuan bi', DATEADD(minute, -30, GETDATE())),
    (10, 4, 2, 'STAFF', 'PREPARING', 'READY', N'Don san sang', DATEADD(minute, -15, GETDATE())),
    (11, 5, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(hour, -3, GETDATE())),
    (12, 5, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(hour, -2, GETDATE())),
    (13, 5, 2, 'STAFF', 'CONFIRMED', 'PREPARING', N'Bep dang chuan bi', DATEADD(hour, -1, GETDATE())),
    (14, 5, 2, 'STAFF', 'PREPARING', 'READY', N'Don san sang', DATEADD(minute, -20, GETDATE())),
    (15, 5, 2, 'STAFF', 'READY', 'ASSIGNED', N'Da gan shipper', DATEADD(minute, -10, GETDATE())),
    (16, 6, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(hour, -4, GETDATE())),
    (17, 6, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(hour, -3, GETDATE())),
    (18, 6, 2, 'STAFF', 'CONFIRMED', 'PREPARING', N'Bep dang chuan bi', DATEADD(hour, -2, GETDATE())),
    (19, 6, 2, 'STAFF', 'PREPARING', 'READY', N'Don san sang', DATEADD(hour, -1, GETDATE())),
    (20, 6, 2, 'STAFF', 'READY', 'ASSIGNED', N'Da gan shipper', DATEADD(minute, -30, GETDATE())),
    (21, 6, 3, 'SHIPPER', 'ASSIGNED', 'PICKED_UP', N'Da nhan mon', DATEADD(minute, -20, GETDATE())),
    (22, 7, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(day, -2, GETDATE())),
    (23, 7, 2, 'STAFF', 'PENDING', 'CONFIRMED', N'Da xac nhan', DATEADD(day, -2, GETDATE())),
    (24, 7, 2, 'STAFF', 'CONFIRMED', 'PREPARING', N'Bep dang chuan bi', DATEADD(day, -2, GETDATE())),
    (25, 7, 2, 'STAFF', 'PREPARING', 'READY', N'Don san sang', DATEADD(day, -2, GETDATE())),
    (26, 7, 2, 'STAFF', 'READY', 'ASSIGNED', N'Da gan shipper', DATEADD(day, -2, GETDATE())),
    (27, 7, 3, 'SHIPPER', 'ASSIGNED', 'PICKED_UP', N'Da nhan mon', DATEADD(day, -1, GETDATE())),
    (28, 7, 3, 'SHIPPER', 'PICKED_UP', 'DELIVERED', N'Giao thanh cong', DATEADD(day, -1, GETDATE())),
    (29, 8, 4, 'USER', NULL, 'PENDING', N'Don hang duoc tao', DATEADD(day, -3, GETDATE())),
    (30, 8, 4, 'USER', 'PENDING', 'CANCELLED', N'Khach hang doi y', DATEADD(day, -3, GETDATE()));
SET IDENTITY_INSERT dbo.OrderStatusHistory OFF;

SET IDENTITY_INSERT dbo.LoyaltyTransaction ON;
INSERT dbo.LoyaltyTransaction (loyalty_transaction_id, user_id, order_id, transaction_type, points, created_at) VALUES
    (1, 4, 7, 'EARN', 250, DATEADD(day, -1, GETDATE()));
SET IDENTITY_INSERT dbo.LoyaltyTransaction OFF;

SET IDENTITY_INSERT dbo.Review ON;
INSERT dbo.Review (review_id, user_id, order_id, rating, comment, created_at, updated_at) VALUES
    (1, 4, 7, 5, N'Giao nhanh, mon an con nong.', DATEADD(hour, -20, GETDATE()), DATEADD(hour, -20, GETDATE()));
SET IDENTITY_INSERT dbo.Review OFF;

SET IDENTITY_INSERT dbo.SupportTicket ON;
INSERT dbo.SupportTicket (ticket_id, user_id, order_id, subject, category, description, status, staff_id, resolution, created_at, updated_at, resolved_at) VALUES
    (1, 4, 7, N'Can ho tro hoa don', 'OTHER', N'Xin gui lai thong tin hoa don.', 'PROCESSING', 2, NULL, DATEADD(hour, -4, GETDATE()), GETDATE(), NULL);
SET IDENTITY_INSERT dbo.SupportTicket OFF;

SET IDENTITY_INSERT dbo.Notification ON;
INSERT dbo.Notification (notification_id, user_id, role_name, title, message, type, target_url, is_read, created_at, updated_at) VALUES
    (1, 4, NULL, N'Don hang da giao', N'Don FG-DEMO-007 da giao thanh cong.', 'ORDER', '/orders/7', 0, DATEADD(day, -1, GETDATE()), GETDATE()),
    (2, NULL, 'STAFF', N'Don moi', N'Don FG-DEMO-001 dang cho xu ly.', 'ORDER', '/staff/orders/1', 0, DATEADD(minute, -20, GETDATE()), GETDATE()),
    (3, 3, NULL, N'Don da gan', N'Ban duoc gan don FG-DEMO-005.', 'ORDER', '/shipper/orders/5', 0, DATEADD(minute, -10, GETDATE()), GETDATE());
SET IDENTITY_INSERT dbo.Notification OFF;

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

DECLARE @RequiredTables TABLE (table_name sysname PRIMARY KEY);
INSERT @RequiredTables (table_name) VALUES
    ('Users'), ('PasswordResetToken'), ('Address'), ('Category'), ('Product'), ('ProductVariant'),
    ('ProductModifierGroup'), ('ProductModifierOption'), ('ProductCombo'), ('ProductComboItem'), ('Cart'),
    ('CartItem'), ('Orders'), ('OrderItem'), ('Coupon'), ('CouponRedemption'), ('Banner'), ('Review'),
    ('Notification'), ('SupportTicket'), ('OrderStatusHistory'), ('LoyaltyTransaction'), ('WorkShift'),
    ('PaymentAttempt'), ('InventoryReservation'), ('InventoryTransaction'), ('ShippingConfig');

IF EXISTS (
    SELECT 1 FROM @RequiredTables r
    WHERE OBJECT_ID(N'dbo.' + QUOTENAME(r.table_name), N'U') IS NULL
)
    THROW 51000, 'Validation failed: required table missing.', 1;

IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID('dbo')) <> 27
    THROW 51001, 'Validation failed: dbo must contain exactly 26 entity tables plus ShippingConfig.', 1;

IF OBJECT_ID(N'dbo.Role', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.DeliveryZone', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.FavoriteProduct', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.CartItemModifier', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.OrderItemModifier', N'U') IS NOT NULL
    THROW 51002, 'Validation failed: forbidden legacy table exists.', 1;

IF COL_LENGTH('dbo.CartItem', 'selected_modifier_option_ids') IS NOT NULL
    THROW 51003, 'Validation failed: legacy CartItem column exists.', 1;

IF (SELECT COUNT(DISTINCT role_name) FROM dbo.Users WHERE status = 'ACTIVE') <> 4
   OR (SELECT COUNT(*) FROM dbo.ProductModifierOption) = 0
   OR (SELECT COUNT(*) FROM dbo.ProductComboItem) = 0
   OR (SELECT COUNT(*) FROM dbo.WorkShift WHERE shift_date = CAST(GETDATE() AS date)) < 2
   OR (SELECT COUNT(DISTINCT order_status) FROM dbo.Orders) <> 8
   OR (SELECT COUNT(*) FROM dbo.PaymentAttempt) = 0
   OR (SELECT COUNT(*) FROM dbo.Review) = 0
   OR (SELECT COUNT(*) FROM dbo.SupportTicket) = 0
   OR (SELECT COUNT(*) FROM dbo.Notification) = 0
   OR (SELECT COUNT(*) FROM dbo.LoyaltyTransaction) = 0
    THROW 51004, 'Validation failed: required demo data missing.', 1;

IF EXISTS (
    SELECT o.order_id
    FROM dbo.Orders o
    LEFT JOIN dbo.InventoryReservation r ON r.order_id = o.order_id
    GROUP BY o.order_id
    HAVING COUNT(r.reservation_id) = 0
)
    THROW 51005, 'Validation failed: demo order is missing inventory reservation.', 1;

IF EXISTS (
    SELECT oi.order_id, oi.variant_id
    FROM dbo.OrderItem oi
    GROUP BY oi.order_id, oi.variant_id
    HAVING SUM(oi.quantity) <> (
        SELECT COALESCE(SUM(r.quantity), 0)
        FROM dbo.InventoryReservation r
        WHERE r.order_id = oi.order_id AND r.variant_id = oi.variant_id
    )
)
    THROW 51006, 'Validation failed: reservation quantities do not match order items.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.OrderItem item
    OUTER APPLY (
        SELECT COUNT(*) AS sauce_count
        FROM OPENJSON(CASE WHEN ISJSON(item.modifiers_json) = 1 THEN item.modifiers_json ELSE N'[]' END)
             WITH (modifier_option_id int '$.modifierOptionId') selected
        JOIN dbo.ProductModifierOption option_row ON option_row.modifier_option_id = selected.modifier_option_id
        WHERE option_row.modifier_group_id = 2
    ) sauce
    WHERE item.product_id = 1
      AND (ISJSON(item.modifiers_json) <> 1 OR sauce.sauce_count <> 1)
)
    THROW 51010, 'Validation failed: seeded Product 1 order item must contain exactly one required sauce option.', 1;

IF EXISTS (
    SELECT 1 FROM (VALUES (1, 96), (2, 79), (3, 150), (4, 200), (5, 58)) expected(variant_id, quantity_available)
    JOIN dbo.ProductVariant variant ON variant.variant_id = expected.variant_id
    WHERE variant.quantity_available <> expected.quantity_available
)
    THROW 51007, 'Validation failed: variant stock does not reflect reservation deductions and releases.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.PaymentAttempt attempt
    JOIN dbo.Orders orders ON orders.order_id = attempt.order_id
    WHERE attempt.status = 'READY'
      AND (attempt.provider <> 'PAYOS' OR orders.payment_method <> 'BANK_TRANSFER'
           OR attempt.amount <> orders.final_amount
           OR orders.payos_payment_link_id IS NULL OR attempt.provider_reference IS NULL
           OR orders.payos_payment_link_id <> attempt.provider_reference
           OR orders.payos_checkout_url IS NULL OR attempt.checkout_url IS NULL
           OR orders.payos_checkout_url <> attempt.checkout_url)
)
    THROW 51008, 'Validation failed: PayOS order fields do not match ready payment attempt.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.InventoryReservation reservation
    OUTER APPLY (
        SELECT
            SUM(CASE WHEN transaction_row.transaction_type = 'RESERVE' THEN 1 ELSE 0 END) AS reserve_count,
            SUM(CASE WHEN transaction_row.transaction_type = 'CONSUME' THEN 1 ELSE 0 END) AS consume_count,
            SUM(CASE WHEN transaction_row.transaction_type = 'RELEASE' THEN 1 ELSE 0 END) AS release_count,
            SUM(CASE WHEN transaction_row.transaction_type = 'WASTE' THEN 1 ELSE 0 END) AS waste_count,
            SUM(CASE WHEN transaction_row.transaction_type = 'RESERVE' THEN transaction_row.quantity ELSE 0 END) AS reserve_quantity,
            SUM(CASE WHEN transaction_row.transaction_type = 'CONSUME' THEN transaction_row.quantity ELSE 0 END) AS consume_quantity,
            SUM(CASE WHEN transaction_row.transaction_type = 'RELEASE' THEN transaction_row.quantity ELSE 0 END) AS release_quantity,
            SUM(CASE WHEN transaction_row.transaction_type = 'WASTE' THEN transaction_row.quantity ELSE 0 END) AS waste_quantity,
            MIN(CASE WHEN transaction_row.transaction_type = 'RESERVE' THEN transaction_row.created_at END) AS reserved_at,
            MIN(CASE WHEN transaction_row.transaction_type IN ('CONSUME', 'RELEASE', 'WASTE') THEN transaction_row.created_at END) AS terminal_at
        FROM dbo.InventoryTransaction transaction_row
        WHERE transaction_row.order_id = reservation.order_id
          AND transaction_row.variant_id = reservation.variant_id
    ) lifecycle
    WHERE COALESCE(lifecycle.reserve_count, 0) <> 1
       OR COALESCE(lifecycle.reserve_quantity, 0) <> reservation.quantity
       OR (reservation.status = 'RESERVED' AND (COALESCE(lifecycle.consume_count, 0) <> 0 OR COALESCE(lifecycle.release_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'CONSUMED' AND (COALESCE(lifecycle.consume_count, 0) <> 1 OR COALESCE(lifecycle.consume_quantity, 0) <> reservation.quantity OR COALESCE(lifecycle.release_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'RELEASED' AND (COALESCE(lifecycle.release_count, 0) <> 1 OR COALESCE(lifecycle.release_quantity, 0) <> reservation.quantity OR COALESCE(lifecycle.consume_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'WASTED' AND (COALESCE(lifecycle.consume_count, 0) <> 1 OR COALESCE(lifecycle.consume_quantity, 0) <> reservation.quantity OR COALESCE(lifecycle.waste_count, 0) <> 1 OR COALESCE(lifecycle.waste_quantity, 0) <> reservation.quantity OR COALESCE(lifecycle.release_count, 0) <> 0))
       OR (reservation.status IN ('CONSUMED', 'RELEASED', 'WASTED') AND (lifecycle.terminal_at IS NULL OR lifecycle.terminal_at < lifecycle.reserved_at))
)
    THROW 51011, 'Validation failed: inventory transaction sequence does not match reservation lifecycle.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.InventoryTransaction transaction_row
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.InventoryReservation reservation
        WHERE reservation.order_id = transaction_row.order_id
          AND reservation.variant_id = transaction_row.variant_id
    )
)
    THROW 51012, 'Validation failed: inventory transaction has no matching reservation.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.Orders orders
    OUTER APPLY (
        SELECT TOP (1) history.to_status
        FROM dbo.OrderStatusHistory history
        WHERE history.order_id = orders.order_id
        ORDER BY history.created_at DESC, history.history_id DESC
    ) latest
    WHERE latest.to_status IS NULL OR latest.to_status <> orders.order_status
)
    THROW 51009, 'Validation failed: latest order history does not match order status.', 1;

SELECT 'Users' AS table_name, COUNT(*) AS row_count FROM dbo.Users
UNION ALL SELECT 'Products', COUNT(*) FROM dbo.Product
UNION ALL SELECT 'Variants', COUNT(*) FROM dbo.ProductVariant
UNION ALL SELECT 'Orders', COUNT(*) FROM dbo.Orders
UNION ALL SELECT 'OrderItems', COUNT(*) FROM dbo.OrderItem
UNION ALL SELECT 'OrderStatuses', COUNT(DISTINCT order_status) FROM dbo.Orders
UNION ALL SELECT 'TodayShifts', COUNT(*) FROM dbo.WorkShift WHERE shift_date = CAST(GETDATE() AS date)
UNION ALL SELECT 'InventoryTransactions', COUNT(*) FROM dbo.InventoryTransaction
UNION ALL SELECT 'Notifications', COUNT(*) FROM dbo.Notification;

PRINT 'FastGuyDB canonical schema and demo data validated successfully.';
GO
