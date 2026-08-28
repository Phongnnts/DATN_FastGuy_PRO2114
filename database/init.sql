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
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

CREATE TABLE dbo.SchemaMigrationHistory (
    migration_id varchar(100) NOT NULL CONSTRAINT PK_SchemaMigrationHistory PRIMARY KEY,
    applied_at datetime2(0) NOT NULL CONSTRAINT DF_SchemaMigrationHistory_AppliedAt DEFAULT SYSUTCDATETIME(),
    applied_by sysname NOT NULL CONSTRAINT DF_SchemaMigrationHistory_AppliedBy DEFAULT ORIGINAL_LOGIN(),
    details nvarchar(1000) NULL
);
INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES
    ('000_preflight_history', N'Canonical fresh schema baseline'),
    ('042_login_bruteforce_lock', N'Canonical fresh schema baseline'),
    ('059_shift_schedule_order_timeout', N'Canonical fresh schema baseline'),
    ('060_operating_finance', N'Canonical fresh schema baseline');

CREATE TABLE dbo.Category (
    category_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Category PRIMARY KEY,
    name nvarchar(255) NOT NULL,
    description nvarchar(500) NULL,
    image_url nvarchar(1000) NULL,
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
    is_new bit NOT NULL CONSTRAINT DF_Product_IsNew DEFAULT 0,
    spice_level tinyint NOT NULL CONSTRAINT DF_Product_SpiceLevel DEFAULT 0,
    status varchar(20) NOT NULL CONSTRAINT DF_Product_Status DEFAULT 'AVAILABLE',
    available_from time(0) NULL,
    available_to time(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Product_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Product_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Product_BasePrice CHECK (base_price >= 0),
    CONSTRAINT CK_Product_SpiceLevel CHECK (spice_level BETWEEN 0 AND 3),
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
    inventory_mode varchar(20) NOT NULL CONSTRAINT DF_ProductVariant_InventoryMode DEFAULT 'UNTRACKED',
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
    CONSTRAINT CK_ProductVariant_InventoryMode CHECK (inventory_mode IN ('INGREDIENT','FINISHED_GOOD','UNTRACKED','SUSPENDED')),
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
    failed_login_attempts int NOT NULL CONSTRAINT DF_Users_FailedLoginAttempts DEFAULT 0,
    locked_until datetime2(0) NULL,
    CONSTRAINT CK_Users_FailedLoginAttempts CHECK (failed_login_attempts >= 0),
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
    status_entered_at datetime2(0) NOT NULL CONSTRAINT DF_Orders_StatusEnteredAt DEFAULT SYSDATETIME(),
    staff_id int NULL CONSTRAINT FK_Orders_Staff REFERENCES dbo.Users(user_id),
    staff_shift_id int NULL,
    shipper_id int NULL CONSTRAINT FK_Orders_Shipper REFERENCES dbo.Users(user_id),
    assigned_at datetime2(0) NULL,
    confirmed_at datetime2(0) NULL,
    ready_at datetime2(0) NULL,
    picked_up_at datetime2(0) NULL,
    paid_at datetime2(0) NULL,
    delivered_at datetime2(0) NULL,
    cancelled_at datetime2(0) NULL,
    failure_reason nvarchar(500) NULL,
    delivery_attempt_count int NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptCount DEFAULT 0,
    delivery_attempt_limit int NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptLimit DEFAULT 2,
    delivery_failure_code varchar(30) NULL,
    delivery_failed_at datetime2(0) NULL,
    retry_scheduled_at datetime2(0) NULL,
    returned_to_store_at datetime2(0) NULL,
    cancelled_by varchar(20) NULL,
    refund_status varchar(20) NULL,
    refund_amount decimal(18,2) NULL,
    refunded_at datetime2(0) NULL,
    refund_note nvarchar(500) NULL,
    refund_processed_by int NULL CONSTRAINT FK_Orders_RefundProcessedBy REFERENCES dbo.Users(user_id),
    refund_reference nvarchar(200) NULL,
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
    CONSTRAINT CK_Orders_Status CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_Orders_DeliveryAttempts CHECK (delivery_attempt_count >= 0 AND delivery_attempt_limit > 0 AND delivery_attempt_count <= delivery_attempt_limit),
    CONSTRAINT CK_Orders_DeliveryFailureCode CHECK (delivery_failure_code IS NULL OR delivery_failure_code IN ('CUSTOMER_UNREACHABLE', 'INVALID_ADDRESS', 'CUSTOMER_RESCHEDULED', 'CUSTOMER_REJECTED', 'SHIPPER_INCIDENT', 'PRODUCT_INCIDENT')),
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

CREATE TABLE dbo.InventoryItem (
    inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryItem PRIMARY KEY,
    name nvarchar(255) NOT NULL,
    item_type varchar(20) NOT NULL,
    base_unit varchar(10) NOT NULL,
    inventory_code varchar(30) NOT NULL,
    count_frequency varchar(10) NOT NULL CONSTRAINT DF_InventoryItem_CountFrequency DEFAULT 'WEEKLY',
    average_unit_cost decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_AverageUnitCost DEFAULT 0,
    last_counted_at datetime2(0) NULL,
    on_hand_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_OnHand DEFAULT 0,
    reserved_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Reserved DEFAULT 0,
    minimum_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Minimum DEFAULT 0,
    active bit NOT NULL CONSTRAINT DF_InventoryItem_Active DEFAULT 1,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_InventoryItem_Type CHECK (item_type IN ('INGREDIENT','FINISHED_GOOD')),
    CONSTRAINT CK_InventoryItem_BaseUnit CHECK (base_unit IN ('G','ML','PIECE')),
    CONSTRAINT UQ_InventoryItem_Code UNIQUE (inventory_code),
    CONSTRAINT CK_InventoryItem_CountFrequency CHECK (count_frequency IN ('DAILY','WEEKLY')),
    CONSTRAINT CK_InventoryItem_AverageUnitCost CHECK (average_unit_cost >= 0),
    CONSTRAINT CK_InventoryItem_OnHand CHECK (on_hand_quantity >= 0),
    CONSTRAINT CK_InventoryItem_Reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity),
    CONSTRAINT CK_InventoryItem_Minimum CHECK (minimum_quantity >= 0)
);
CREATE TABLE dbo.VariantInventoryItem (
    variant_inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_VariantInventoryItem PRIMARY KEY,
    variant_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Variant REFERENCES dbo.ProductVariant(variant_id),
    inventory_item_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    CONSTRAINT UQ_VariantInventoryItem_Variant UNIQUE (variant_id),
    CONSTRAINT UQ_VariantInventoryItem_Item UNIQUE (inventory_item_id)
);
CREATE TABLE dbo.Recipe (
    recipe_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Recipe PRIMARY KEY,
    variant_id int NOT NULL CONSTRAINT FK_Recipe_Variant REFERENCES dbo.ProductVariant(variant_id),
    yield_quantity decimal(19,4) NOT NULL CONSTRAINT DF_Recipe_Yield DEFAULT 1,
    active bit NOT NULL CONSTRAINT DF_Recipe_Active DEFAULT 1,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_Recipe_Variant UNIQUE (variant_id),
    CONSTRAINT CK_Recipe_Yield CHECK (yield_quantity > 0)
);
CREATE TABLE dbo.RecipeItem (
    recipe_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_RecipeItem PRIMARY KEY,
    recipe_id int NOT NULL CONSTRAINT FK_RecipeItem_Recipe REFERENCES dbo.Recipe(recipe_id),
    inventory_item_id int NOT NULL CONSTRAINT FK_RecipeItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    quantity decimal(19,4) NOT NULL,
    CONSTRAINT UQ_RecipeItem_RecipeInventoryItem UNIQUE (recipe_id, inventory_item_id),
    CONSTRAINT CK_RecipeItem_Quantity CHECK (quantity > 0)
);
CREATE TABLE dbo.InventoryReservation (
    reservation_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservation PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT FK_InventoryReservation_Order REFERENCES dbo.Orders(order_id),
    status varchar(20) NOT NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_InventoryReservation_Order UNIQUE (order_id),
    CONSTRAINT CK_InventoryReservation_Status CHECK (status IN ('RESERVED','CONSUMED','RELEASED','WASTED'))
);
CREATE TABLE dbo.InventoryReservationLegacyHistory (
    legacy_reservation_id int NOT NULL CONSTRAINT PK_InventoryReservationLegacyHistory PRIMARY KEY,
    canonical_reservation_id int NOT NULL,
    order_id int NOT NULL,
    variant_id int NOT NULL,
    inventory_item_id int NOT NULL CONSTRAINT FK_InventoryReservationLegacyHistory_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    quantity decimal(19,4) NOT NULL,
    status varchar(20) NOT NULL,
    created_at datetime2(0) NOT NULL,
    updated_at datetime2(0) NOT NULL
);
CREATE TABLE dbo.InventoryReservationItem (
    reservation_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservationItem PRIMARY KEY,
    reservation_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Reservation REFERENCES dbo.InventoryReservation(reservation_id),
    inventory_item_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    quantity decimal(19,4) NOT NULL,
    CONSTRAINT UQ_InventoryReservationItem_ReservationInventoryItem UNIQUE (reservation_id, inventory_item_id),
    CONSTRAINT CK_InventoryReservationItem_Quantity CHECK (quantity > 0)
);
CREATE TABLE dbo.GoodsReceipt (
    goods_receipt_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceipt PRIMARY KEY,
    supplier_name nvarchar(150) NOT NULL,
    invoice_number nvarchar(100) NULL,
    received_at datetime2(0) NOT NULL,
    status varchar(10) NOT NULL CONSTRAINT DF_GoodsReceipt_Status DEFAULT 'DRAFT',
    created_by int NOT NULL CONSTRAINT FK_GoodsReceipt_CreatedBy REFERENCES dbo.Users(user_id),
    approved_by int NULL CONSTRAINT FK_GoodsReceipt_ApprovedBy REFERENCES dbo.Users(user_id),
    created_at datetime2(0) NOT NULL CONSTRAINT DF_GoodsReceipt_CreatedAt DEFAULT GETDATE(),
    approved_at datetime2(0) NULL,
    CONSTRAINT CK_GoodsReceipt_Status CHECK (status IN ('DRAFT','APPROVED')),
    CONSTRAINT CK_GoodsReceipt_Approval CHECK ((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
);
CREATE TABLE dbo.GoodsReceiptItem (
    goods_receipt_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceiptItem PRIMARY KEY,
    goods_receipt_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Receipt REFERENCES dbo.GoodsReceipt(goods_receipt_id),
    inventory_item_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    purchase_quantity decimal(19,4) NOT NULL,
    purchase_unit nvarchar(30) NOT NULL,
    conversion_factor decimal(19,4) NOT NULL,
    base_quantity decimal(19,4) NOT NULL,
    purchase_unit_price decimal(19,4) NOT NULL,
    line_total decimal(19,4) NOT NULL,
    average_cost_before decimal(19,4) NULL,
    average_cost_after decimal(19,4) NULL,
    CONSTRAINT UQ_GoodsReceiptItem_ReceiptItem UNIQUE (goods_receipt_id,inventory_item_id),
    CONSTRAINT CK_GoodsReceiptItem_Positive CHECK (purchase_quantity>0 AND conversion_factor>0 AND base_quantity>0 AND purchase_unit_price>0 AND line_total>0),
    CONSTRAINT CK_GoodsReceiptItem_Cost CHECK ((average_cost_before IS NULL AND average_cost_after IS NULL) OR (average_cost_before>=0 AND average_cost_after>=0))
);
CREATE TABLE dbo.StockCount (
    stock_count_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCount PRIMARY KEY,
    count_date date NOT NULL,
    frequency varchar(10) NOT NULL,
    status varchar(10) NOT NULL CONSTRAINT DF_StockCount_Status DEFAULT 'DRAFT',
    created_by int NOT NULL CONSTRAINT FK_StockCount_CreatedBy REFERENCES dbo.Users(user_id),
    approved_by int NULL CONSTRAINT FK_StockCount_ApprovedBy REFERENCES dbo.Users(user_id),
    created_at datetime2(0) NOT NULL CONSTRAINT DF_StockCount_CreatedAt DEFAULT GETDATE(),
    approved_at datetime2(0) NULL,
    CONSTRAINT CK_StockCount_Frequency CHECK (frequency IN ('DAILY','WEEKLY')),
    CONSTRAINT CK_StockCount_Status CHECK (status IN ('DRAFT','APPROVED')),
    CONSTRAINT CK_StockCount_Approval CHECK ((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
);
CREATE TABLE dbo.StockCountItem (
    stock_count_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCountItem PRIMARY KEY,
    stock_count_id int NOT NULL CONSTRAINT FK_StockCountItem_Count REFERENCES dbo.StockCount(stock_count_id),
    inventory_item_id int NOT NULL CONSTRAINT FK_StockCountItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    theoretical_quantity decimal(19,4) NOT NULL,
    actual_quantity decimal(19,4) NULL,
    variance_quantity decimal(19,4) NULL,
    unit_cost_snapshot decimal(19,4) NULL,
    variance_cost decimal(19,4) NULL,
    reason_code varchar(50) NULL,
    note nvarchar(500) NULL,
    CONSTRAINT UQ_StockCountItem_CountItem UNIQUE (stock_count_id,inventory_item_id),
    CONSTRAINT CK_StockCountItem_Quantity CHECK (theoretical_quantity>=0 AND (actual_quantity IS NULL OR actual_quantity>=0)),
    CONSTRAINT CK_StockCountItem_Cost CHECK (unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0)
);
CREATE TABLE dbo.InventoryTransaction (
    inventory_transaction_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryTransaction PRIMARY KEY,
    inventory_item_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Item REFERENCES dbo.InventoryItem(inventory_item_id),
    order_id int NULL CONSTRAINT FK_InventoryTransaction_Order REFERENCES dbo.Orders(order_id),
    transaction_type varchar(20) NOT NULL,
    quantity decimal(19,4) NOT NULL,
    quantity_before decimal(19,4) NULL,
    quantity_after decimal(19,4) NULL,
    reference_type varchar(30) NULL,
    reference_id varchar(100) NULL,
    reason_code varchar(50) NULL,
    note nvarchar(500) NULL,
    unit_cost_snapshot decimal(19,4) NULL,
    total_cost decimal(19,4) NULL,
    goods_receipt_id int NULL CONSTRAINT FK_InventoryTransaction_GoodsReceipt REFERENCES dbo.GoodsReceipt(goods_receipt_id),
    stock_count_id int NULL CONSTRAINT FK_InventoryTransaction_StockCount REFERENCES dbo.StockCount(stock_count_id),
    created_by int NULL CONSTRAINT FK_InventoryTransaction_CreatedBy REFERENCES dbo.Users(user_id),
    created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryTransaction_Created DEFAULT GETDATE(),
    CONSTRAINT CK_InventoryTransaction_Quantity CHECK (quantity <> 0),
    CONSTRAINT CK_InventoryTransaction_Type CHECK (transaction_type IN ('RECEIPT','RESERVE','RELEASE','CONSUME','ADJUSTMENT','WASTE','RETURN')),
    CONSTRAINT CK_InventoryTransaction_Cost CHECK ((unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0) AND (total_cost IS NULL OR total_cost>=0))
);

CREATE TABLE dbo.OperatingExpense (
    expense_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_OperatingExpense PRIMARY KEY,
    expense_date date NOT NULL,
    category varchar(20) NOT NULL,
    description nvarchar(500) NOT NULL,
    amount decimal(18,2) NOT NULL,
    created_by int NOT NULL CONSTRAINT FK_OperatingExpense_CreatedBy REFERENCES dbo.Users(user_id),
    created_at datetime2(0) NOT NULL CONSTRAINT DF_OperatingExpense_CreatedAt DEFAULT SYSUTCDATETIME(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_OperatingExpense_UpdatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_OperatingExpense_Category CHECK (category IN ('RENT','UTILITIES','SALARY','MARKETING','MAINTENANCE','OTHER')),
    CONSTRAINT CK_OperatingExpense_Amount CHECK (amount > 0)
);

CREATE TABLE dbo.FixedAsset (
    asset_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_FixedAsset PRIMARY KEY,
    asset_name nvarchar(255) NOT NULL,
    acquisition_cost decimal(18,2) NOT NULL,
    salvage_value decimal(18,2) NOT NULL,
    depreciation_start_date date NOT NULL,
    useful_life_months int NOT NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_FixedAsset_Status DEFAULT 'ACTIVE',
    retired_at datetime2(0) NULL,
    created_by int NOT NULL CONSTRAINT FK_FixedAsset_CreatedBy REFERENCES dbo.Users(user_id),
    created_at datetime2(0) NOT NULL CONSTRAINT DF_FixedAsset_CreatedAt DEFAULT SYSUTCDATETIME(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_FixedAsset_UpdatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_FixedAsset_Value CHECK (acquisition_cost > 0 AND salvage_value >= 0 AND salvage_value < acquisition_cost),
    CONSTRAINT CK_FixedAsset_UsefulLife CHECK (useful_life_months > 0),
    CONSTRAINT CK_FixedAsset_Status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT CK_FixedAsset_Retirement CHECK ((status = 'ACTIVE' AND retired_at IS NULL) OR (status = 'RETIRED' AND retired_at IS NOT NULL))
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
    shift_code varchar(10) NOT NULL,
    check_in_source varchar(10) NULL,
    check_out_source varchar(10) NULL,
    staff_role_snapshot varchar(10) NOT NULL CONSTRAINT DF_WorkShift_StaffRoleSnapshot DEFAULT 'NON_STAFF',
    check_in_at datetime2(0) NULL,
    check_out_at datetime2(0) NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_WorkShift_Status DEFAULT 'SCHEDULED',
    created_at datetime2(0) NOT NULL CONSTRAINT DF_WorkShift_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_WorkShift_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_WorkShift_Time CHECK (start_time < end_time),
    CONSTRAINT CK_WorkShift_ShiftCode CHECK (shift_code IN ('MORNING','AFTERNOON','EVENING')),
    CONSTRAINT CK_WorkShift_CheckInSource CHECK (check_in_source IS NULL OR check_in_source IN ('MANUAL','AUTO')),
    CONSTRAINT CK_WorkShift_CheckOutSource CHECK (check_out_source IS NULL OR check_out_source IN ('MANUAL','AUTO')),
    CONSTRAINT CK_WorkShift_StaffRoleSnapshot CHECK (staff_role_snapshot IN ('STAFF','NON_STAFF')),
    CONSTRAINT CK_WorkShift_StaffFixedTimes CHECK (staff_role_snapshot<>'STAFF' OR (shift_code='MORNING' AND start_time='08:00' AND end_time='12:00') OR (shift_code='AFTERNOON' AND start_time='12:00' AND end_time='16:00') OR (shift_code='EVENING' AND start_time='16:00' AND end_time='21:00')),
    CONSTRAINT CK_WorkShift_Status CHECK (status IN ('SCHEDULED', 'CHECKED_IN', 'CHECKED_OUT', 'ABSENT', 'CANCELLED')),
    CONSTRAINT CK_WorkShift_CheckTimes CHECK (check_out_at IS NULL OR (check_in_at IS NOT NULL AND check_out_at >= check_in_at))
);

CREATE TABLE dbo.CodSettlement (
    settlement_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CodSettlement PRIMARY KEY,
    shipper_id int NOT NULL,
    shift_id int NOT NULL,
    received_by int NULL,
    status varchar(20) NOT NULL CONSTRAINT DF_CodSettlement_Status DEFAULT 'SUBMITTED',
    expected_amount decimal(18,2) NOT NULL,
    submitted_amount decimal(18,2) NOT NULL,
    verified_amount decimal(18,2) NULL,
    reason nvarchar(500) NULL,
    submitted_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_SubmittedAt DEFAULT SYSUTCDATETIME(),
    verified_at datetime2(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_CreatedAt DEFAULT SYSUTCDATETIME(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_UpdatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_CodSettlement_Shipper FOREIGN KEY (shipper_id) REFERENCES dbo.Users(user_id),
    CONSTRAINT FK_CodSettlement_Shift FOREIGN KEY (shift_id) REFERENCES dbo.WorkShift(shift_id),
    CONSTRAINT FK_CodSettlement_ReceivedBy FOREIGN KEY (received_by) REFERENCES dbo.Users(user_id),
    CONSTRAINT UQ_CodSettlement_ShipperShift UNIQUE (shipper_id, shift_id),
    CONSTRAINT CK_CodSettlement_Status CHECK (status IN ('SUBMITTED','SETTLED','SHORT','OVER')),
    CONSTRAINT CK_CodSettlement_Amounts CHECK (expected_amount >= 0 AND submitted_amount >= 0 AND (verified_amount IS NULL OR verified_amount >= 0)),
    CONSTRAINT CK_CodSettlement_Verification CHECK (
        (status = 'SUBMITTED' AND received_by IS NULL AND verified_amount IS NULL AND verified_at IS NULL)
        OR (status = 'SETTLED' AND received_by IS NOT NULL AND verified_amount = submitted_amount AND verified_at IS NOT NULL)
        OR (status = 'SHORT' AND received_by IS NOT NULL AND verified_amount < submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
        OR (status = 'OVER' AND received_by IS NOT NULL AND verified_amount > submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
    )
);
CREATE INDEX IX_CodSettlement_StatusSubmittedAt ON dbo.CodSettlement(status, submitted_at DESC);
CREATE INDEX IX_CodSettlement_ShipperSubmittedAt ON dbo.CodSettlement(shipper_id, submitted_at DESC);
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT FK_Orders_StaffShift FOREIGN KEY(staff_shift_id) REFERENCES dbo.WorkShift(shift_id);

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
    unit_cost_snapshot decimal(18,2) NULL,
    total_cost_snapshot decimal(18,2) NULL,
    modifiers_json nvarchar(max) NOT NULL CONSTRAINT DF_OrderItem_Modifiers DEFAULT N'[]',
    CONSTRAINT CK_OrderItem_Quantity CHECK (quantity > 0),
    CONSTRAINT CK_OrderItem_Amounts CHECK (unit_price >= 0 AND total_price >= 0),
    CONSTRAINT CK_OrderItem_CostSnapshot CHECK ((unit_cost_snapshot IS NULL AND total_cost_snapshot IS NULL) OR (unit_cost_snapshot >= 0 AND total_cost_snapshot >= 0))
);

CREATE TABLE dbo.Review (
    review_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Review PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_Review_User REFERENCES dbo.Users(user_id),
    order_id int NOT NULL CONSTRAINT FK_Review_Order REFERENCES dbo.Orders(order_id),
    product_id int NOT NULL CONSTRAINT FK_Review_Product REFERENCES dbo.Product(product_id),
    rating int NOT NULL,
    comment nvarchar(1000) NULL,
    is_featured bit NOT NULL CONSTRAINT DF_Review_IsFeatured DEFAULT 0,
    homepage_consent bit NOT NULL CONSTRAINT DF_Review_HomepageConsent DEFAULT 0,

    created_at datetime2(0) NOT NULL CONSTRAINT DF_Review_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Review_Updated DEFAULT GETDATE(),
    CONSTRAINT UQ_Review_UserOrderProduct UNIQUE (user_id, order_id, product_id),
    CONSTRAINT CK_Review_Rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT CK_Review_FeaturedConsent CHECK (is_featured = 0 OR homepage_consent = 1)
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
    CONSTRAINT CK_OrderStatusHistory_From CHECK (from_status IS NULL OR from_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_OrderStatusHistory_To CHECK (to_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'DELIVERED', 'CANCELLED')),
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
CREATE INDEX IX_PasswordResetToken_User ON dbo.PasswordResetToken(user_id);
CREATE INDEX IX_Address_User ON dbo.Address(user_id);
CREATE INDEX IX_CartItem_Cart ON dbo.CartItem(cart_id);
CREATE INDEX IX_CartItem_Product ON dbo.CartItem(product_id);
CREATE INDEX IX_CartItem_Variant ON dbo.CartItem(variant_id);
CREATE INDEX IX_Orders_User ON dbo.Orders(user_id);
CREATE INDEX IX_Orders_Staff_Status ON dbo.Orders(staff_id, order_status);
CREATE UNIQUE INDEX UX_WorkShift_Staff_Date_Code ON dbo.WorkShift(shift_date,shift_code) WHERE staff_role_snapshot='STAFF';
CREATE INDEX IX_Orders_StaffShift_Status ON dbo.Orders(staff_shift_id, order_status);
CREATE INDEX IX_Orders_Status_StatusEnteredAt ON dbo.Orders(order_status,status_entered_at);
CREATE INDEX IX_Orders_PaymentStatus_OrderStatus_StatusEnteredAt ON dbo.Orders(payment_status,order_status,status_entered_at);
CREATE INDEX IX_Orders_Shipper_Status ON dbo.Orders(shipper_id, order_status);
CREATE INDEX IX_Orders_Status_Created ON dbo.Orders(order_status, created_at);
CREATE INDEX IX_InventoryItem_ActiveType ON dbo.InventoryItem(active, item_type);
CREATE INDEX IX_GoodsReceipt_StatusReceived ON dbo.GoodsReceipt(status, received_at DESC);
CREATE INDEX IX_StockCount_StatusDate ON dbo.StockCount(status, count_date DESC);
CREATE INDEX IX_InventoryTransaction_GoodsReceipt ON dbo.InventoryTransaction(goods_receipt_id) WHERE goods_receipt_id IS NOT NULL;
CREATE INDEX IX_InventoryTransaction_StockCount ON dbo.InventoryTransaction(stock_count_id) WHERE stock_count_id IS NOT NULL;
CREATE INDEX IX_RecipeItem_InventoryItem ON dbo.RecipeItem(inventory_item_id);
CREATE INDEX IX_InventoryReservationItem_InventoryItem ON dbo.InventoryReservationItem(inventory_item_id);
CREATE INDEX IX_InventoryTransaction_Order ON dbo.InventoryTransaction(order_id);
CREATE INDEX IX_InventoryTransaction_ItemCreated ON dbo.InventoryTransaction(inventory_item_id, created_at DESC);
CREATE INDEX IX_LoyaltyTransaction_User_Created ON dbo.LoyaltyTransaction(user_id, created_at);
CREATE INDEX IX_OperatingExpense_ExpenseDate ON dbo.OperatingExpense(expense_date, category);
CREATE INDEX IX_FixedAsset_Status_DepreciationStartDate ON dbo.FixedAsset(status, depreciation_start_date);
CREATE INDEX IX_WorkShift_User_Date ON dbo.WorkShift(user_id, shift_date);
CREATE INDEX IX_WorkShift_Date_Status ON dbo.WorkShift(shift_date, status);
CREATE INDEX IX_CouponRedemption_Coupon ON dbo.CouponRedemption(coupon_id);
CREATE INDEX IX_OrderItem_Order ON dbo.OrderItem(order_id);
CREATE INDEX IX_OrderItem_Product ON dbo.OrderItem(product_id);
CREATE INDEX IX_OrderItem_Variant ON dbo.OrderItem(variant_id);
CREATE INDEX IX_Review_Order ON dbo.Review(order_id);
CREATE INDEX IX_Review_ProductCreatedAt ON dbo.Review(product_id, created_at DESC, review_id DESC);
CREATE INDEX IX_Review_FeaturedCreatedAt ON dbo.Review(is_featured, created_at DESC) WHERE is_featured = 1;
CREATE INDEX IX_OrderStatusHistory_Order_Created ON dbo.OrderStatusHistory(order_id, created_at);
CREATE INDEX IX_OrderStatusHistory_Actor ON dbo.OrderStatusHistory(actor_user_id);
GO
CREATE OR ALTER TRIGGER dbo.TR_Orders_AssignmentRoleGuard ON dbo.Orders AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.staff_id WHERE i.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN ('STAFF','ADMIN'))) THROW 51413, 'Orders.staff_id must reference STAFF or ADMIN.', 1;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.shipper_id WHERE i.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>'SHIPPER')) THROW 51414, 'Orders.shipper_id must reference SHIPPER.', 1;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.WorkShift ws ON ws.shift_id=i.staff_shift_id LEFT JOIN dbo.Users u ON u.user_id=ws.user_id WHERE i.staff_shift_id IS NOT NULL AND (ws.shift_id IS NULL OR u.user_id IS NULL OR u.role_name<>'STAFF')) THROW 51420, 'Orders.staff_shift_id must reference a STAFF shift.', 1;
END;
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

SET IDENTITY_INSERT dbo.InventoryItem ON;
INSERT dbo.InventoryItem(inventory_item_id,name,item_type,base_unit,inventory_code,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES (1,N'Classic Burger standard','FINISHED_GOOD','PIECE','INV-000001',96,0,5,1),(2,N'Classic Burger large','FINISHED_GOOD','PIECE','INV-000002',79,0,5,1),(3,N'Fries standard','FINISHED_GOOD','PIECE','INV-000003',150,0,10,1),(4,N'Cola large','FINISHED_GOOD','PIECE','INV-000004',200,0,10,1),(5,N'Combo Burger','FINISHED_GOOD','PIECE','INV-000005',58,0,5,1);
SET IDENTITY_INSERT dbo.InventoryItem OFF;
INSERT dbo.VariantInventoryItem(variant_id,inventory_item_id) VALUES (1,1),(2,2),(3,3),(4,4),(5,5);

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

INSERT dbo.ShippingConfig (config_key, config_value) VALUES
    ('ghn_from_district_id', '1442'), ('ghn_from_ward_code', '20107'),
    ('default_weight', '500'), ('default_length', '20'), ('default_width', '20'), ('default_height', '10'),
    ('default_service_type_id', '2'), ('business_open_time', '00:00'), ('business_close_time', '00:00'), ('service_fee', '0'),
    ('low_stock_threshold', '5');

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

DECLARE @WeekStart date=DATEADD(day,1-DATEPART(weekday,CAST(GETDATE() AS date)),CAST(GETDATE() AS date));
;WITH days(n) AS (SELECT n FROM (VALUES(0),(1),(2),(3),(4),(5),(6)) d(n)), slots(shift_code,start_time,end_time) AS (SELECT * FROM (VALUES('MORNING',CAST('08:00' AS time),CAST('12:00' AS time)),('AFTERNOON',CAST('12:00' AS time),CAST('16:00' AS time)),('EVENING',CAST('16:00' AS time),CAST('21:00' AS time))) s(shift_code,start_time,end_time))
INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,shift_code,check_in_source,check_out_source,staff_role_snapshot,check_in_at,check_out_at,status)
SELECT 2,DATEADD(day,d.n,@WeekStart),s.start_time,s.end_time,s.shift_code,CASE WHEN DATEADD(day,d.n,@WeekStart)<=CAST(GETDATE() AS date) THEN 'AUTO' END,CASE WHEN DATEADD(day,d.n,@WeekStart)<CAST(GETDATE() AS date) OR (DATEADD(day,d.n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)>s.end_time) THEN 'AUTO' END,'STAFF',CASE WHEN DATEADD(day,d.n,@WeekStart)<CAST(GETDATE() AS date) OR (DATEADD(day,d.n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)>=s.start_time) THEN DATEADD(second,DATEDIFF(second,CAST('00:00' AS time),s.start_time),CAST(DATEADD(day,d.n,@WeekStart) AS datetime2)) END,CASE WHEN DATEADD(day,d.n,@WeekStart)<CAST(GETDATE() AS date) OR (DATEADD(day,d.n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)>s.end_time) THEN DATEADD(second,DATEDIFF(second,CAST('00:00' AS time),s.end_time),CAST(DATEADD(day,d.n,@WeekStart) AS datetime2)) END,CASE WHEN DATEADD(day,d.n,@WeekStart)>CAST(GETDATE() AS date) OR (DATEADD(day,d.n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)<s.start_time) THEN 'SCHEDULED' WHEN DATEADD(day,d.n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)<=s.end_time THEN 'CHECKED_IN' ELSE 'CHECKED_OUT' END FROM days d CROSS JOIN slots s;
INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,shift_code,check_in_source,check_out_source,staff_role_snapshot,check_in_at,check_out_at,status)
SELECT 3,DATEADD(day,n,@WeekStart),'09:00','18:00','MORNING',CASE WHEN DATEADD(day,n,@WeekStart)<=CAST(GETDATE() AS date) THEN 'MANUAL' END,CASE WHEN DATEADD(day,n,@WeekStart)<CAST(GETDATE() AS date) THEN 'MANUAL' END,'NON_STAFF',CASE WHEN DATEADD(day,n,@WeekStart)<=CAST(GETDATE() AS date) THEN DATEADD(hour,9,CAST(DATEADD(day,n,@WeekStart) AS datetime2)) END,CASE WHEN DATEADD(day,n,@WeekStart)<CAST(GETDATE() AS date) THEN DATEADD(hour,18,CAST(DATEADD(day,n,@WeekStart) AS datetime2)) END,CASE WHEN DATEADD(day,n,@WeekStart)>CAST(GETDATE() AS date) OR (DATEADD(day,n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)<'09:00') THEN 'SCHEDULED' WHEN DATEADD(day,n,@WeekStart)=CAST(GETDATE() AS date) AND CAST(GETDATE() AS time)<='18:00' THEN 'CHECKED_IN' ELSE 'CHECKED_OUT' END FROM (VALUES(0),(1),(2),(3),(4),(5),(6)) d(n);

SET IDENTITY_INSERT dbo.Orders ON;
INSERT dbo.Orders (order_id, order_code, idempotency_key, request_hash, idempotency_owner, user_id, customer_name, customer_phone, customer_address, to_province_name, to_district_name, to_ward_name, total_amount, shipping_fee, service_fee, final_amount, cod_collected_amount, cod_collected_at, shipping_provider, expected_delivery_time, payment_method, payment_status, payos_payment_link_id, payos_checkout_url, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, paid_at, delivered_at, cancelled_at, failure_reason, cancelled_by, coupon_code, discount_amount, delivery_note, created_at, updated_at) VALUES
    (1, 'FG-DEMO-001', 'demo-order-001', REPLICATE('a',64), 'USER:4', 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 59000, 15000, 0, 74000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'BANK_TRANSFER', 'UNPAID', 'DEMO-REFERENCE-001', 'https://pay.payos.vn/web/demo', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, N'Giao tai le tan', DATEADD(minute, -20, GETDATE()), GETDATE()),
    (2, 'FG-DEMO-002', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 69000, 15000, 0, 84000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'CONFIRMED', 2, NULL, NULL, DATEADD(minute, -25, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(minute, -30, GETDATE()), GETDATE()),
    (3, 'FG-DEMO-003', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 89000, 15000, 0, 104000, NULL, NULL, 'GHN', DATEADD(hour, 1, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'PREPARING', 2, NULL, NULL, DATEADD(minute, -40, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(minute, -45, GETDATE()), GETDATE()),
    (4, 'FG-DEMO-004', NULL, NULL, NULL, 4, N'FastGuy Customer', '0901000004', N'123 Nguyen Hue, Quan 1', N'TP. Ho Chi Minh', N'Quan 1', N'Ben Nghe', 59000, 15000, 0, 74000, NULL, NULL, 'GHN', DATEADD(minute, 45, GETDATE()), 'COD', 'UNPAID', NULL, NULL, 'READY', 2, NULL, NULL, DATEADD(hour, -1, GETDATE()), DATEADD(minute, -15, GETDATE()), NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, DATEADD(hour, -2, GETDATE()), GETDATE()),
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
INSERT dbo.InventoryReservation (reservation_id, order_id, status, created_at, updated_at) VALUES
    (1,1,'RESERVED',DATEADD(minute,-20,GETDATE()),GETDATE()),(2,2,'RESERVED',DATEADD(minute,-30,GETDATE()),GETDATE()),(3,3,'CONSUMED',DATEADD(minute,-45,GETDATE()),GETDATE()),(4,4,'CONSUMED',DATEADD(hour,-2,GETDATE()),GETDATE()),(5,5,'CONSUMED',DATEADD(hour,-3,GETDATE()),GETDATE()),(6,6,'CONSUMED',DATEADD(hour,-4,GETDATE()),GETDATE()),(7,7,'CONSUMED',DATEADD(day,-2,GETDATE()),DATEADD(day,-1,GETDATE())),(8,8,'RELEASED',DATEADD(day,-3,GETDATE()),DATEADD(day,-3,GETDATE()));
SET IDENTITY_INSERT dbo.InventoryReservation OFF;
INSERT dbo.InventoryReservationItem(reservation_id,inventory_item_id,quantity) VALUES (1,1,1),(2,1,1),(3,5,1),(4,1,1),(5,5,1),(6,1,1),(7,2,1),(8,3,1);

SET IDENTITY_INSERT dbo.InventoryTransaction ON;
INSERT dbo.InventoryTransaction(inventory_transaction_id,order_id,inventory_item_id,transaction_type,quantity,created_at) VALUES
    (1,1,1,'RESERVE',1,DATEADD(minute,-20,GETDATE())),(2,2,1,'RESERVE',1,DATEADD(minute,-30,GETDATE())),(3,3,5,'RESERVE',1,DATEADD(minute,-45,GETDATE())),(4,3,5,'CONSUME',1,DATEADD(minute,-35,GETDATE())),(5,4,1,'RESERVE',1,DATEADD(hour,-2,GETDATE())),(6,4,1,'CONSUME',1,DATEADD(hour,-1,GETDATE())),(7,5,5,'RESERVE',1,DATEADD(hour,-3,GETDATE())),(8,5,5,'CONSUME',1,DATEADD(hour,-2,GETDATE())),(9,6,1,'RESERVE',1,DATEADD(hour,-4,GETDATE())),(10,6,1,'CONSUME',1,DATEADD(hour,-3,GETDATE())),(11,7,2,'RESERVE',1,DATEADD(day,-2,GETDATE())),(12,7,2,'CONSUME',1,DATEADD(day,-2,GETDATE())),(13,8,3,'RESERVE',1,DATEADD(day,-3,GETDATE())),(14,8,3,'RELEASE',1,DATEADD(day,-3,GETDATE()));
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
INSERT dbo.Review (review_id, user_id, order_id, product_id, rating, comment, created_at, updated_at) VALUES
    (1, 4, 7, 1, 5, N'Giao nhanh, mon an con nong.', DATEADD(hour, -20, GETDATE()), DATEADD(hour, -20, GETDATE()));
SET IDENTITY_INSERT dbo.Review OFF;

UPDATE dbo.InventoryItem SET average_unit_cost=CASE inventory_item_id WHEN 1 THEN 28000 WHEN 2 THEN 34000 WHEN 3 THEN 9000 WHEN 4 THEN 5000 ELSE 43000 END;
UPDATE dbo.OrderItem SET unit_cost_snapshot=CASE variant_id WHEN 1 THEN 28000 WHEN 2 THEN 34000 WHEN 3 THEN 9000 WHEN 4 THEN 5000 ELSE 43000 END,total_cost_snapshot=quantity*CASE variant_id WHEN 1 THEN 28000 WHEN 2 THEN 34000 WHEN 3 THEN 9000 WHEN 4 THEN 5000 ELSE 43000 END;
UPDATE o SET status_entered_at=CASE WHEN o.order_status IN('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP') THEN DATEADD(minute,-5,SYSDATETIME()) ELSE COALESCE(o.delivered_at,o.cancelled_at,o.created_at) END,staff_shift_id=CASE WHEN o.staff_id=2 AND o.order_status IN('CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP') THEN (SELECT TOP(1) shift_id FROM dbo.WorkShift WHERE user_id=2 AND shift_date=CAST(GETDATE() AS date) ORDER BY CASE WHEN CAST(GETDATE() AS time) BETWEEN start_time AND end_time THEN 0 ELSE 1 END,start_time) END FROM dbo.Orders o;
UPDATE dbo.InventoryItem SET reserved_quantity=2 WHERE inventory_item_id=1;

SET IDENTITY_INSERT dbo.Orders ON;
;WITH n(n) AS (SELECT n FROM (VALUES(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),(31),(32)) v(n))
INSERT dbo.Orders(order_id,order_code,user_id,customer_name,customer_phone,customer_address,total_amount,shipping_fee,final_amount,cod_collected_amount,cod_collected_at,payment_method,payment_status,order_status,staff_id,shipper_id,assigned_at,confirmed_at,ready_at,picked_up_at,paid_at,delivered_at,cancelled_at,cancelled_by,discount_amount,status_entered_at,created_at,updated_at)
SELECT n,CONCAT('FG-DEMO-',RIGHT(CONCAT('000',n),3)),4,N'FastGuy Customer','0901000004',N'123 Nguyen Hue, Quan 1',59000,15000,74000,CASE WHEN n%4<>0 THEN 74000 END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,'COD',CASE WHEN n%4<>0 THEN 'PAID' ELSE 'UNPAID' END,CASE WHEN n%4<>0 THEN 'DELIVERED' ELSE 'CANCELLED' END,CASE WHEN n%4<>0 THEN 2 END,CASE WHEN n%4<>0 THEN 3 END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4<>0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4=0 THEN DATEADD(day,-(n*2),GETDATE()) END,CASE WHEN n%4=0 THEN 'CUSTOMER' END,0,DATEADD(day,-(n*2),GETDATE()),DATEADD(day,-(n*2),GETDATE()),DATEADD(day,-(n*2),GETDATE()) FROM n;
SET IDENTITY_INSERT dbo.Orders OFF;
INSERT dbo.OrderItem(order_id,product_id,variant_id,product_name,variant_name,quantity,unit_price,total_price,unit_cost_snapshot,total_cost_snapshot,modifiers_json) SELECT order_id,1,1,N'Classic Burger',N'Tieu chuan',1,59000,59000,28000,28000,N'[{"modifierOptionId":3,"groupName":"Sot","optionName":"Sot dac biet","price":0}]' FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.InventoryReservation(order_id,status,created_at,updated_at) SELECT order_id,CASE WHEN order_status='DELIVERED' THEN 'CONSUMED' ELSE 'RELEASED' END,created_at,status_entered_at FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.InventoryReservationItem(reservation_id,inventory_item_id,quantity) SELECT reservation_id,1,1 FROM dbo.InventoryReservation WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.InventoryTransaction(inventory_item_id,order_id,transaction_type,quantity,quantity_before,quantity_after,unit_cost_snapshot,total_cost,created_at) SELECT 1,order_id,'RESERVE',1,100,99,28000,28000,DATEADD(minute,-5,created_at) FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.InventoryTransaction(inventory_item_id,order_id,transaction_type,quantity,quantity_before,quantity_after,unit_cost_snapshot,total_cost,created_at) SELECT 1,order_id,CASE WHEN order_status='DELIVERED' THEN 'CONSUME' ELSE 'RELEASE' END,1,99,CASE WHEN order_status='DELIVERED' THEN 98 ELSE 100 END,28000,28000,status_entered_at FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.OrderStatusHistory(order_id,actor_user_id,actor_role,from_status,to_status,note,created_at) SELECT order_id,CASE WHEN order_status='DELIVERED' THEN 3 ELSE 4 END,CASE WHEN order_status='DELIVERED' THEN 'SHIPPER' ELSE 'USER' END,CASE WHEN order_status='DELIVERED' THEN 'PICKED_UP' ELSE 'PENDING' END,order_status,N'Demo history',status_entered_at FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32;
INSERT dbo.LoyaltyTransaction(user_id,order_id,transaction_type,points,created_at) SELECT 4,order_id,'EARN',74,delivered_at FROM dbo.Orders WHERE order_id BETWEEN 9 AND 32 AND order_status='DELIVERED';
UPDATE dbo.Users SET loyalty_points=(SELECT SUM(points) FROM dbo.LoyaltyTransaction WHERE user_id=4) WHERE user_id=4;
INSERT dbo.OperatingExpense(expense_date,category,description,amount,created_by) VALUES (DATEADD(month,-5,CAST(GETDATE() AS date)),'RENT',N'Tien thue cua hang',18000000,1),(DATEADD(month,-4,CAST(GETDATE() AS date)),'UTILITIES',N'Dien nuoc',4200000,1),(DATEADD(month,-3,CAST(GETDATE() AS date)),'SALARY',N'Luong nhan vien',24000000,1),(DATEADD(month,-2,CAST(GETDATE() AS date)),'MARKETING',N'Khuyen mai sinh vien',3500000,1),(DATEADD(month,-1,CAST(GETDATE() AS date)),'MAINTENANCE',N'Bao tri thiet bi',1800000,1),(CAST(GETDATE() AS date),'OTHER',N'Vat tu van hanh',900000,1);
INSERT dbo.FixedAsset(asset_name,acquisition_cost,salvage_value,depreciation_start_date,useful_life_months,status,created_by) VALUES (N'Bep chien cong nghiep',45000000,5000000,DATEADD(month,-18,CAST(GETDATE() AS date)),60,'ACTIVE',1),(N'Tu dong bao quan',62000000,7000000,DATEADD(month,-14,CAST(GETDATE() AS date)),72,'ACTIVE',1),(N'Bo may POS',18000000,1000000,DATEADD(month,-10,CAST(GETDATE() AS date)),36,'ACTIVE',1),(N'Xe may giao hang',32000000,4000000,DATEADD(month,-8,CAST(GETDATE() AS date)),48,'ACTIVE',1);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

DECLARE @RequiredTables TABLE (table_name sysname PRIMARY KEY);
INSERT @RequiredTables (table_name) VALUES
    ('SchemaMigrationHistory'), ('Users'), ('PasswordResetToken'), ('Address'), ('Category'), ('Product'), ('ProductVariant'),
    ('ProductModifierGroup'), ('ProductModifierOption'), ('Cart'), ('CartItem'), ('Orders'), ('OrderItem'),
    ('Coupon'), ('CouponRedemption'), ('Banner'), ('Review'), ('OrderStatusHistory'), ('LoyaltyTransaction'), ('WorkShift'),
    ('PaymentAttempt'), ('InventoryItem'), ('VariantInventoryItem'), ('Recipe'), ('RecipeItem'), ('InventoryReservation'), ('InventoryReservationLegacyHistory'), ('InventoryReservationItem'), ('GoodsReceipt'), ('GoodsReceiptItem'), ('StockCount'), ('StockCountItem'), ('InventoryTransaction'), ('OperatingExpense'), ('FixedAsset'), ('CodSettlement'), ('ShippingConfig');
DECLARE @ExpectedTableCount int = (SELECT COUNT(*) FROM @RequiredTables);
IF @ExpectedTableCount <> 37 THROW 51020, 'Validation failed: canonical required table list must contain 37 tables.',1;

IF EXISTS (
    SELECT 1 FROM @RequiredTables r
    WHERE OBJECT_ID(N'dbo.' + QUOTENAME(r.table_name), N'U') IS NULL
)
    THROW 51000, 'Validation failed: required table missing.', 1;

IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID('dbo')) <> @ExpectedTableCount
    THROW 51001, 'Validation failed: dbo table count differs from canonical CREATE TABLE set.', 1;

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
   OR (SELECT COUNT(*) FROM dbo.WorkShift WHERE shift_date = CAST(GETDATE() AS date)) < 2
   OR (SELECT COUNT(DISTINCT order_status) FROM dbo.Orders) <> 8
   OR (SELECT COUNT(*) FROM dbo.PaymentAttempt) = 0
   OR (SELECT COUNT(*) FROM dbo.Review) = 0
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

IF EXISTS (SELECT 1 FROM dbo.InventoryReservation r LEFT JOIN dbo.InventoryReservationItem ri ON ri.reservation_id=r.reservation_id GROUP BY r.reservation_id HAVING COUNT(ri.reservation_item_id)=0)
    THROW 51006, 'Validation failed: reservation has no inventory lines.', 1;

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
    OUTER APPLY (SELECT SUM(quantity) AS quantity FROM dbo.InventoryReservationItem WHERE reservation_id=reservation.reservation_id) reserved
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
    ) lifecycle
    WHERE COALESCE(lifecycle.reserve_count, 0) <> 1
       OR COALESCE(lifecycle.reserve_quantity, 0) <> reserved.quantity
       OR (reservation.status = 'RESERVED' AND (COALESCE(lifecycle.consume_count, 0) <> 0 OR COALESCE(lifecycle.release_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'CONSUMED' AND (COALESCE(lifecycle.consume_count, 0) <> 1 OR COALESCE(lifecycle.consume_quantity, 0) <> reserved.quantity OR COALESCE(lifecycle.release_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'RELEASED' AND (COALESCE(lifecycle.release_count, 0) <> 1 OR COALESCE(lifecycle.release_quantity, 0) <> reserved.quantity OR COALESCE(lifecycle.consume_count, 0) <> 0 OR COALESCE(lifecycle.waste_count, 0) <> 0))
       OR (reservation.status = 'WASTED' AND (COALESCE(lifecycle.consume_count, 0) <> 1 OR COALESCE(lifecycle.consume_quantity, 0) <> reserved.quantity OR COALESCE(lifecycle.waste_count, 0) <> 1 OR COALESCE(lifecycle.waste_quantity, 0) <> reserved.quantity OR COALESCE(lifecycle.release_count, 0) <> 0))
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

DECLARE @ValidationWeekStart date=DATEADD(day,1-DATEPART(weekday,CAST(GETDATE() AS date)),CAST(GETDATE() AS date));
IF (SELECT COUNT(*) FROM dbo.WorkShift WHERE staff_role_snapshot='STAFF' AND shift_date BETWEEN @ValidationWeekStart AND DATEADD(day,6,@ValidationWeekStart))<>21 OR EXISTS(SELECT 1 FROM dbo.WorkShift WHERE staff_role_snapshot='STAFF' GROUP BY shift_date,shift_code HAVING COUNT(*)<>1) THROW 51013, 'Validation failed: current week must contain exact 7x3 STAFF schedule, one STAFF per slot.',1;
IF NOT ((SELECT COUNT(*) FROM dbo.Orders) BETWEEN 20 AND 40) THROW 51014, 'Validation failed: demo order count must remain manageable.',1;
IF EXISTS(SELECT 1 FROM dbo.Orders WHERE order_status IN('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP') AND status_entered_at<DATEADD(minute,-30,SYSDATETIME())) THROW 51015, 'No active demo order may exceed its state timeout',1;
IF EXISTS(SELECT 1 FROM dbo.Orders WHERE order_status IN('CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP') AND (staff_shift_id IS NULL OR staff_id<>2)) THROW 51016, 'Validation failed: active owned store order lacks STAFF shift ownership.',1;
IF EXISTS(SELECT 1 FROM dbo.OrderItem WHERE unit_cost_snapshot IS NULL OR total_cost_snapshot IS NULL) THROW 51017, 'Validation failed: order cost snapshot incomplete.',1;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.OperatingExpense') AND name='IX_OperatingExpense_ExpenseDate') OR NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.FixedAsset') AND name='IX_FixedAsset_Status_DepreciationStartDate') THROW 51018, 'Validation failed: operating finance index missing.',1;
IF (SELECT COALESCE(SUM(final_amount),0) FROM dbo.Orders WHERE order_status='DELIVERED')<=0 OR (SELECT COALESCE(SUM(total_cost_snapshot),0) FROM dbo.OrderItem oi JOIN dbo.Orders o ON o.order_id=oi.order_id WHERE o.order_status='DELIVERED')<=0 OR (SELECT COALESCE(SUM(amount),0) FROM dbo.OperatingExpense)<=0 OR (SELECT COALESCE(SUM((acquisition_cost-salvage_value)/useful_life_months),0) FROM dbo.FixedAsset WHERE status='ACTIVE')<=0 THROW 51019, 'Financial demo must have nonzero revenue, COGS, expense, and depreciation',1;

SELECT 'Users' AS table_name, COUNT(*) AS row_count FROM dbo.Users
UNION ALL SELECT 'Products', COUNT(*) FROM dbo.Product
UNION ALL SELECT 'Variants', COUNT(*) FROM dbo.ProductVariant
UNION ALL SELECT 'Orders', COUNT(*) FROM dbo.Orders
UNION ALL SELECT 'OrderItems', COUNT(*) FROM dbo.OrderItem
UNION ALL SELECT 'OrderStatuses', COUNT(DISTINCT order_status) FROM dbo.Orders
UNION ALL SELECT 'TodayShifts', COUNT(*) FROM dbo.WorkShift WHERE shift_date = CAST(GETDATE() AS date)
UNION ALL SELECT 'InventoryTransactions', COUNT(*) FROM dbo.InventoryTransaction;

PRINT 'FastGuyDB canonical schema and demo data validated successfully.';
GO
