-- Khởi tạo cơ sở dữ liệu
CREATE DATABASE FastGuyDB;
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
GO

-- Tạo bảng Category
CREATE TABLE dbo.Category (
    category_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Category PRIMARY KEY,
    name nvarchar(255) NOT NULL,
    description nvarchar(500) NULL,
    image_url nvarchar(1000) NULL,
    sort_order int NOT NULL CONSTRAINT DF_Category_SortOrder DEFAULT 0,
    status varchar(20) NOT NULL CONSTRAINT DF_Category_Status DEFAULT 'ACTIVE',
    CONSTRAINT CK_Category_Status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Tạo bảng Product
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

-- Tạo bảng ProductVariant
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

-- Tạo bảng ProductModifierGroup
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

-- Tạo bảng ProductModifierOption
CREATE TABLE dbo.ProductModifierOption (
    modifier_option_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ProductModifierOption PRIMARY KEY,
    modifier_group_id int NOT NULL CONSTRAINT FK_ProductModifierOption_Group REFERENCES dbo.ProductModifierGroup(modifier_group_id),
    name nvarchar(255) NOT NULL,
    price decimal(18,2) NOT NULL CONSTRAINT DF_ProductModifierOption_Price DEFAULT 0,
    is_active bit NOT NULL CONSTRAINT DF_ProductModifierOption_Active DEFAULT 1,
    sort_order int NOT NULL CONSTRAINT DF_ProductModifierOption_Sort DEFAULT 0,
    CONSTRAINT CK_ProductModifierOption_Price CHECK (price >= 0)
);

-- Tạo bảng ShippingConfig
CREATE TABLE dbo.ShippingConfig (
    config_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_ShippingConfig PRIMARY KEY,
    config_key varchar(100) NOT NULL CONSTRAINT UQ_ShippingConfig_Key UNIQUE,
    config_value varchar(500) NOT NULL
);

-- Tạo bảng Users
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

-- Tạo bảng PasswordResetToken
CREATE TABLE dbo.PasswordResetToken (
    reset_token_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_PasswordResetToken PRIMARY KEY,
    user_id int NOT NULL CONSTRAINT FK_PasswordResetToken_User REFERENCES dbo.Users(user_id),
    token_hash varchar(64) NOT NULL CONSTRAINT UQ_PasswordResetToken_Hash UNIQUE,
    expires_at datetime2(0) NOT NULL,
    used_at datetime2(0) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_PasswordResetToken_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_PasswordResetToken_Updated DEFAULT GETDATE()
);

-- Tạo bảng Banner
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

-- Tạo bảng Address
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

-- Tạo bảng Cart
CREATE TABLE dbo.Cart (
    cart_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Cart PRIMARY KEY,
    user_id int NULL CONSTRAINT FK_Cart_User REFERENCES dbo.Users(user_id),
    session_id varchar(128) NULL,
    created_at datetime2(0) NOT NULL CONSTRAINT DF_Cart_Created DEFAULT GETDATE(),
    updated_at datetime2(0) NOT NULL CONSTRAINT DF_Cart_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_Cart_Owner CHECK (user_id IS NOT NULL OR session_id IS NOT NULL)
);

-- Tạo bảng CartItem
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

-- Tạo bảng Coupon
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

-- Tạo bảng Orders
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
    CONSTRAINT CK_Orders_RefundStatus CHECK (refund_status IS NULL OR refund_status IN ('PENDING', 'REFUNDED', 'REJECTED')),
    CONSTRAINT CK_Orders_FinalAmount CHECK (final_amount = total_amount + shipping_fee + service_fee - discount_amount),
    CONSTRAINT CK_Orders_AssignmentTimes CHECK (shipper_id IS NULL OR assigned_at IS NOT NULL)
);

-- Tạo bảng PaymentAttempt
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
    inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryItem PRIMARY KEY, name nvarchar(255) NOT NULL, item_type varchar(20) NOT NULL, base_unit varchar(10) NOT NULL, inventory_code varchar(30) NOT NULL, count_frequency varchar(10) NOT NULL CONSTRAINT DF_InventoryItem_CountFrequency DEFAULT 'WEEKLY', average_unit_cost decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_AverageUnitCost DEFAULT 0, last_counted_at datetime2(0) NULL,
    on_hand_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_OnHand DEFAULT 0, reserved_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Reserved DEFAULT 0, minimum_quantity decimal(19,4) NOT NULL CONSTRAINT DF_InventoryItem_Minimum DEFAULT 0,
    active bit NOT NULL CONSTRAINT DF_InventoryItem_Active DEFAULT 1, created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Created DEFAULT GETDATE(), updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryItem_Updated DEFAULT GETDATE(),
    CONSTRAINT CK_InventoryItem_Type CHECK (item_type IN ('INGREDIENT','FINISHED_GOOD')), CONSTRAINT CK_InventoryItem_BaseUnit CHECK (base_unit IN ('G','ML','PIECE')), CONSTRAINT UQ_InventoryItem_Code UNIQUE(inventory_code), CONSTRAINT CK_InventoryItem_CountFrequency CHECK(count_frequency IN('DAILY','WEEKLY')), CONSTRAINT CK_InventoryItem_AverageUnitCost CHECK(average_unit_cost>=0), CONSTRAINT CK_InventoryItem_OnHand CHECK (on_hand_quantity >= 0), CONSTRAINT CK_InventoryItem_Reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity), CONSTRAINT CK_InventoryItem_Minimum CHECK (minimum_quantity >= 0)
);
CREATE TABLE dbo.VariantInventoryItem (variant_inventory_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_VariantInventoryItem PRIMARY KEY, variant_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Variant REFERENCES dbo.ProductVariant(variant_id), inventory_item_id int NOT NULL CONSTRAINT FK_VariantInventoryItem_Item REFERENCES dbo.InventoryItem(inventory_item_id), CONSTRAINT UQ_VariantInventoryItem_Variant UNIQUE (variant_id), CONSTRAINT UQ_VariantInventoryItem_Item UNIQUE (inventory_item_id));
CREATE TABLE dbo.Recipe (recipe_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Recipe PRIMARY KEY, variant_id int NOT NULL CONSTRAINT FK_Recipe_Variant REFERENCES dbo.ProductVariant(variant_id), yield_quantity decimal(19,4) NOT NULL CONSTRAINT DF_Recipe_Yield DEFAULT 1, active bit NOT NULL CONSTRAINT DF_Recipe_Active DEFAULT 1, created_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Created DEFAULT GETDATE(), updated_at datetime2(0) NOT NULL CONSTRAINT DF_Recipe_Updated DEFAULT GETDATE(), CONSTRAINT UQ_Recipe_Variant UNIQUE (variant_id), CONSTRAINT CK_Recipe_Yield CHECK (yield_quantity > 0));
CREATE TABLE dbo.RecipeItem (recipe_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_RecipeItem PRIMARY KEY, recipe_id int NOT NULL CONSTRAINT FK_RecipeItem_Recipe REFERENCES dbo.Recipe(recipe_id), inventory_item_id int NOT NULL CONSTRAINT FK_RecipeItem_Item REFERENCES dbo.InventoryItem(inventory_item_id), quantity decimal(19,4) NOT NULL, CONSTRAINT UQ_RecipeItem_RecipeInventoryItem UNIQUE (recipe_id, inventory_item_id), CONSTRAINT CK_RecipeItem_Quantity CHECK (quantity > 0));
CREATE TABLE dbo.InventoryReservation (reservation_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservation PRIMARY KEY, order_id int NOT NULL CONSTRAINT FK_InventoryReservation_Order REFERENCES dbo.Orders(order_id), status varchar(20) NOT NULL, created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Created DEFAULT GETDATE(), updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Updated DEFAULT GETDATE(), CONSTRAINT UQ_InventoryReservation_Order UNIQUE (order_id), CONSTRAINT CK_InventoryReservation_Status CHECK (status IN ('RESERVED','CONSUMED','RELEASED','WASTED')));
CREATE TABLE dbo.InventoryReservationLegacyHistory (legacy_reservation_id int NOT NULL CONSTRAINT PK_InventoryReservationLegacyHistory PRIMARY KEY,canonical_reservation_id int NOT NULL,order_id int NOT NULL,variant_id int NOT NULL,inventory_item_id int NOT NULL CONSTRAINT FK_InventoryReservationLegacyHistory_Item REFERENCES dbo.InventoryItem(inventory_item_id),quantity decimal(19,4) NOT NULL,status varchar(20) NOT NULL,created_at datetime2(0) NOT NULL,updated_at datetime2(0) NOT NULL);
CREATE TABLE dbo.InventoryReservationItem (reservation_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservationItem PRIMARY KEY, reservation_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Reservation REFERENCES dbo.InventoryReservation(reservation_id), inventory_item_id int NOT NULL CONSTRAINT FK_InventoryReservationItem_Item REFERENCES dbo.InventoryItem(inventory_item_id), quantity decimal(19,4) NOT NULL, CONSTRAINT UQ_InventoryReservationItem_ReservationInventoryItem UNIQUE (reservation_id, inventory_item_id), CONSTRAINT CK_InventoryReservationItem_Quantity CHECK (quantity > 0));
CREATE TABLE dbo.GoodsReceipt (goods_receipt_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceipt PRIMARY KEY,supplier_name nvarchar(150) NOT NULL,invoice_number nvarchar(100) NULL,received_at datetime2(0) NOT NULL,status varchar(10) NOT NULL CONSTRAINT DF_GoodsReceipt_Status DEFAULT 'DRAFT',created_by int NOT NULL CONSTRAINT FK_GoodsReceipt_CreatedBy REFERENCES dbo.Users(user_id),approved_by int NULL CONSTRAINT FK_GoodsReceipt_ApprovedBy REFERENCES dbo.Users(user_id),created_at datetime2(0) NOT NULL CONSTRAINT DF_GoodsReceipt_CreatedAt DEFAULT GETDATE(),approved_at datetime2(0) NULL,CONSTRAINT CK_GoodsReceipt_Status CHECK(status IN('DRAFT','APPROVED')),CONSTRAINT CK_GoodsReceipt_Approval CHECK((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL)));
CREATE TABLE dbo.GoodsReceiptItem (goods_receipt_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoodsReceiptItem PRIMARY KEY,goods_receipt_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Receipt REFERENCES dbo.GoodsReceipt(goods_receipt_id),inventory_item_id int NOT NULL CONSTRAINT FK_GoodsReceiptItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),purchase_quantity decimal(19,4) NOT NULL,purchase_unit nvarchar(30) NOT NULL,conversion_factor decimal(19,4) NOT NULL,base_quantity decimal(19,4) NOT NULL,purchase_unit_price decimal(19,4) NOT NULL,line_total decimal(19,4) NOT NULL,average_cost_before decimal(19,4) NULL,average_cost_after decimal(19,4) NULL,CONSTRAINT UQ_GoodsReceiptItem_ReceiptItem UNIQUE(goods_receipt_id,inventory_item_id),CONSTRAINT CK_GoodsReceiptItem_Positive CHECK(purchase_quantity>0 AND conversion_factor>0 AND base_quantity>0 AND purchase_unit_price>0 AND line_total>0),CONSTRAINT CK_GoodsReceiptItem_Cost CHECK((average_cost_before IS NULL AND average_cost_after IS NULL) OR (average_cost_before>=0 AND average_cost_after>=0)));
CREATE TABLE dbo.StockCount (stock_count_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCount PRIMARY KEY,count_date date NOT NULL,frequency varchar(10) NOT NULL,status varchar(10) NOT NULL CONSTRAINT DF_StockCount_Status DEFAULT 'DRAFT',created_by int NOT NULL CONSTRAINT FK_StockCount_CreatedBy REFERENCES dbo.Users(user_id),approved_by int NULL CONSTRAINT FK_StockCount_ApprovedBy REFERENCES dbo.Users(user_id),created_at datetime2(0) NOT NULL CONSTRAINT DF_StockCount_CreatedAt DEFAULT GETDATE(),approved_at datetime2(0) NULL,CONSTRAINT CK_StockCount_Frequency CHECK(frequency IN('DAILY','WEEKLY')),CONSTRAINT CK_StockCount_Status CHECK(status IN('DRAFT','APPROVED')),CONSTRAINT CK_StockCount_Approval CHECK((status='DRAFT' AND approved_by IS NULL AND approved_at IS NULL) OR (status='APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL)));
CREATE TABLE dbo.StockCountItem (stock_count_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StockCountItem PRIMARY KEY,stock_count_id int NOT NULL CONSTRAINT FK_StockCountItem_Count REFERENCES dbo.StockCount(stock_count_id),inventory_item_id int NOT NULL CONSTRAINT FK_StockCountItem_Item REFERENCES dbo.InventoryItem(inventory_item_id),theoretical_quantity decimal(19,4) NOT NULL,actual_quantity decimal(19,4) NULL,variance_quantity decimal(19,4) NULL,unit_cost_snapshot decimal(19,4) NULL,variance_cost decimal(19,4) NULL,reason_code varchar(50) NULL,note nvarchar(500) NULL,CONSTRAINT UQ_StockCountItem_CountItem UNIQUE(stock_count_id,inventory_item_id),CONSTRAINT CK_StockCountItem_Quantity CHECK(theoretical_quantity>=0 AND (actual_quantity IS NULL OR actual_quantity>=0)),CONSTRAINT CK_StockCountItem_Cost CHECK(unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0));
CREATE TABLE dbo.InventoryTransaction (inventory_transaction_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryTransaction PRIMARY KEY, inventory_item_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Item REFERENCES dbo.InventoryItem(inventory_item_id), order_id int NULL CONSTRAINT FK_InventoryTransaction_Order REFERENCES dbo.Orders(order_id), transaction_type varchar(20) NOT NULL, quantity decimal(19,4) NOT NULL, quantity_before decimal(19,4) NULL, quantity_after decimal(19,4) NULL, reference_type varchar(30) NULL, reference_id varchar(100) NULL, reason_code varchar(50) NULL, note nvarchar(500) NULL, unit_cost_snapshot decimal(19,4) NULL,total_cost decimal(19,4) NULL,goods_receipt_id int NULL CONSTRAINT FK_InventoryTransaction_GoodsReceipt REFERENCES dbo.GoodsReceipt(goods_receipt_id),stock_count_id int NULL CONSTRAINT FK_InventoryTransaction_StockCount REFERENCES dbo.StockCount(stock_count_id),created_by int NULL CONSTRAINT FK_InventoryTransaction_CreatedBy REFERENCES dbo.Users(user_id), created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryTransaction_Created DEFAULT GETDATE(), CONSTRAINT CK_InventoryTransaction_Quantity CHECK (quantity <> 0), CONSTRAINT CK_InventoryTransaction_Type CHECK (transaction_type IN ('RECEIPT','RESERVE','RELEASE','CONSUME','ADJUSTMENT','WASTE','RETURN')),CONSTRAINT CK_InventoryTransaction_Cost CHECK((unit_cost_snapshot IS NULL OR unit_cost_snapshot>=0) AND (total_cost IS NULL OR total_cost>=0)));

-- Tạo bảng LoyaltyTransaction
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

-- Tạo bảng WorkShift
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

-- Tạo bảng CouponRedemption
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

-- Tạo bảng OrderItem
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
    CONSTRAINT CK_OrderItem_Total CHECK (total_price = unit_price * quantity),
    CONSTRAINT CK_OrderItem_CostSnapshot CHECK ((unit_cost_snapshot IS NULL AND total_cost_snapshot IS NULL) OR (unit_cost_snapshot >= 0 AND total_cost_snapshot >= 0))
);

-- Tạo bảng Review
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

-- Tạo bảng OrderStatusHistory
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

CREATE INDEX IX_InventoryItem_ActiveType ON dbo.InventoryItem(active,item_type);
CREATE INDEX IX_GoodsReceipt_StatusReceived ON dbo.GoodsReceipt(status,received_at DESC);
CREATE INDEX IX_StockCount_StatusDate ON dbo.StockCount(status,count_date DESC);
CREATE INDEX IX_InventoryTransaction_GoodsReceipt ON dbo.InventoryTransaction(goods_receipt_id) WHERE goods_receipt_id IS NOT NULL;
CREATE INDEX IX_InventoryTransaction_StockCount ON dbo.InventoryTransaction(stock_count_id) WHERE stock_count_id IS NOT NULL;
CREATE INDEX IX_RecipeItem_InventoryItem ON dbo.RecipeItem(inventory_item_id);
CREATE INDEX IX_InventoryReservationItem_InventoryItem ON dbo.InventoryReservationItem(inventory_item_id);
CREATE INDEX IX_InventoryTransaction_Order ON dbo.InventoryTransaction(order_id);
CREATE INDEX IX_InventoryTransaction_ItemCreated ON dbo.InventoryTransaction(inventory_item_id,created_at DESC);
CREATE INDEX IX_Review_Order ON dbo.Review(order_id);
CREATE INDEX IX_Review_ProductCreatedAt ON dbo.Review(product_id, created_at DESC, review_id DESC);
CREATE INDEX IX_Review_FeaturedCreatedAt ON dbo.Review(is_featured, created_at DESC) WHERE is_featured = 1;
GO

-- Dữ liệu mẫu nhỏ gọn
SET IDENTITY_INSERT dbo.ShippingConfig ON;
INSERT INTO dbo.ShippingConfig(config_id,config_key,config_value) VALUES (1,'ghn_from_district_id','1442'),(2,'ghn_from_ward_code','20107'),(3,'service_fee','0'),(4,'low_stock_threshold','5');
SET IDENTITY_INSERT dbo.ShippingConfig OFF;

SET IDENTITY_INSERT dbo.Category ON;
INSERT INTO dbo.Category(category_id,name,description,sort_order,status) VALUES (1,N'Burger',N'Burger thủ công',1,'ACTIVE'),(2,N'Combo',N'Combo tiết kiệm',2,'ACTIVE');
SET IDENTITY_INSERT dbo.Category OFF;

SET IDENTITY_INSERT dbo.Product ON;
INSERT INTO dbo.Product(product_id,category_id,name,description,base_price,image_url,gallery_images,status,available_from,available_to,created_at,updated_at) VALUES (1,1,N'Burger bò',N'Burger bò phô mai',50000,'/images/products/burger-bo.jpg',N'[]','AVAILABLE','08:00','22:00','2026-01-01','2026-01-01'),(2,2,N'Combo Burger',N'Burger và nước',70000,'/images/products/combo-burger.jpg',N'[]','AVAILABLE','08:00','22:00','2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.Product OFF;

SET IDENTITY_INSERT dbo.ProductVariant ON;
INSERT INTO dbo.ProductVariant(variant_id,product_id,variant_name,price,original_price,sku,quantity_available,weight,length,width,height,is_default,status,created_at,updated_at) VALUES (1,1,N'Tiêu chuẩn',50000,NULL,'BURGER-STD',100,500,20,20,10,1,'AVAILABLE','2026-01-01','2026-01-01'),(2,2,N'Tiêu chuẩn',70000,NULL,'COMBO-STD',50,800,25,25,15,1,'AVAILABLE','2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.ProductVariant OFF;

SET IDENTITY_INSERT dbo.InventoryItem ON;
INSERT INTO dbo.InventoryItem(inventory_item_id,name,item_type,base_unit,inventory_code,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES (1,N'Burger bò / Tiêu chuẩn','FINISHED_GOOD','PIECE','INV-000001',100,0,5,1),(2,N'Combo Burger / Tiêu chuẩn','FINISHED_GOOD','PIECE','INV-000002',50,0,5,1);
SET IDENTITY_INSERT dbo.InventoryItem OFF;
INSERT INTO dbo.VariantInventoryItem(variant_id,inventory_item_id) VALUES (1,1),(2,2);

SET IDENTITY_INSERT dbo.ProductModifierGroup ON;
INSERT INTO dbo.ProductModifierGroup(modifier_group_id,product_id,name,min_selections,max_selections,is_active,sort_order) VALUES (1,1,N'Tùy chọn thêm',0,1,1,1);
SET IDENTITY_INSERT dbo.ProductModifierGroup OFF;
SET IDENTITY_INSERT dbo.ProductModifierOption ON;
INSERT INTO dbo.ProductModifierOption(modifier_option_id,modifier_group_id,name,price,is_active,sort_order) VALUES (1,1,N'Thêm phô mai',10000,1,1);
SET IDENTITY_INSERT dbo.ProductModifierOption OFF;

SET IDENTITY_INSERT dbo.Users ON;
INSERT INTO dbo.Users(user_id,role_name,email,phone,password_hash,full_name,status,loyalty_points,favorite_ids_json,created_at,updated_at,failed_login_attempts) VALUES (1,'ADMIN','admin@fastguy.local','0901000001','pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=',N'Quản trị viên','ACTIVE',0,N'[]','2026-01-01','2026-01-01',0),(2,'STAFF','staff@fastguy.local','0901000002','pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=',N'Nhân viên','ACTIVE',0,N'[]','2026-01-01','2026-01-01',0),(3,'SHIPPER','shipper@fastguy.local','0901000003','pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=',N'Tài xế','ACTIVE',0,N'[]','2026-01-01','2026-01-01',0),(4,'USER','user@fastguy.local','0901000004','pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=',N'Khách hàng','ACTIVE',100,N'[]','2026-01-01','2026-01-01',0);
SET IDENTITY_INSERT dbo.Users OFF;

SET IDENTITY_INSERT dbo.Banner ON;
INSERT INTO dbo.Banner(banner_id,title,subtitle,image_url,link,sort_order,is_active,created_at,updated_at) VALUES (1,N'Ưu đãi FastGuy',N'Đặt món nhanh chóng','/images/banners/demo.jpg','/menu',1,1,'2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.Banner OFF;
SET IDENTITY_INSERT dbo.Address ON;
INSERT INTO dbo.Address(address_id,user_id,recipient_name,phone,street,ward_name,district_name,province_name,ghn_province_id,ghn_district_id,ghn_ward_code,city,is_default,created_at,updated_at) VALUES (1,4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',N'Bến Nghé',N'Quận 1',N'TP. Hồ Chí Minh',202,1442,'20107',N'TP. Hồ Chí Minh',1,'2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.Address OFF;
SET IDENTITY_INSERT dbo.Cart ON;
INSERT INTO dbo.Cart(cart_id,user_id,session_id,created_at,updated_at) VALUES (1,4,NULL,'2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.Cart OFF;
SET IDENTITY_INSERT dbo.CartItem ON;
INSERT INTO dbo.CartItem(cart_item_id,cart_id,product_id,variant_id,quantity,unit_price,modifiers_json,created_at,updated_at) VALUES (1,1,1,1,1,50000,N'[]','2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.CartItem OFF;
SET IDENTITY_INSERT dbo.Coupon ON;
INSERT INTO dbo.Coupon(coupon_id,code,type,value,min_order,max_discount,max_uses,used_count,expires_at,is_active,is_public,created_at,updated_at) VALUES (1,'GIAM10','FIXED',10000,50000,10000,100,1,'2027-01-01',1,1,'2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.Coupon OFF;
SET IDENTITY_INSERT dbo.WorkShift ON;
INSERT INTO dbo.WorkShift(shift_id,user_id,shift_date,start_time,end_time,check_in_at,check_out_at,status,created_at,updated_at) VALUES (1,2,'2026-01-02','08:00','16:00','2026-01-02 08:00','2026-01-02 16:00','CHECKED_OUT','2026-01-01','2026-01-02'),(2,3,'2026-01-02','08:00','16:00','2026-01-02 08:00','2026-01-02 16:00','CHECKED_OUT','2026-01-01','2026-01-02');
SET IDENTITY_INSERT dbo.WorkShift OFF;

SET IDENTITY_INSERT dbo.Orders ON;
INSERT INTO dbo.Orders(order_id,order_code,user_id,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,final_amount,shipping_provider,payment_method,payment_status,order_status,staff_id,shipper_id,assigned_at,confirmed_at,ready_at,picked_up_at,paid_at,delivered_at,cancelled_at,failure_reason,cancelled_by,coupon_code,discount_amount,created_at,updated_at) VALUES
(1,'FG001',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','PENDING',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:00'),
(2,'FG002',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','CONFIRMED',2,NULL,NULL,'2026-01-01 08:10',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:10'),
(3,'FG003',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','BANK_TRANSFER','PAID','PREPARING',2,NULL,NULL,'2026-01-01 08:10',NULL,NULL,'2026-01-01 08:15',NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:20'),
(4,'FG004',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','READY',2,NULL,NULL,'2026-01-01 08:10','2026-01-01 08:30',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:30'),
(5,'FG005',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','ASSIGNED',2,3,'2026-01-01 08:40','2026-01-01 08:10','2026-01-01 08:30',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:40'),
(6,'FG006',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','PICKED_UP',2,3,'2026-01-01 08:40','2026-01-01 08:10','2026-01-01 08:30','2026-01-01 08:50',NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-01-01 08:00','2026-01-01 08:50'),
(7,'FG007',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,55000,'GHN','BANK_TRANSFER','PAID','DELIVERED',2,3,'2026-01-01 08:40','2026-01-01 08:10','2026-01-01 08:30','2026-01-01 08:50','2026-01-01 08:15','2026-01-01 09:30',NULL,NULL,NULL,'GIAM10',10000,'2026-01-01 08:00','2026-01-01 09:30'),
(8,'FG008',4,N'Khách hàng','0901000004',N'1 Nguyễn Huệ',50000,15000,0,65000,'GHN','COD','UNPAID','CANCELLED',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-01-01 08:15',N'Khách đổi kế hoạch','CUSTOMER',NULL,0,'2026-01-01 08:00','2026-01-01 08:15');
SET IDENTITY_INSERT dbo.Orders OFF;

SET IDENTITY_INSERT dbo.PaymentAttempt ON;
INSERT INTO dbo.PaymentAttempt(payment_attempt_id,order_id,provider,provider_reference,checkout_url,amount,status,lease_token,created_at,updated_at) VALUES (1,3,'PAYOS','PAY-FG003','https://pay.payos.vn/web/FG003',65000,'PAID',NULL,'2026-01-01 08:00','2026-01-01 08:15'),(2,7,'PAYOS','PAY-FG007','https://pay.payos.vn/web/FG007',55000,'PAID',NULL,'2026-01-01 08:00','2026-01-01 08:15');
SET IDENTITY_INSERT dbo.PaymentAttempt OFF;
SET IDENTITY_INSERT dbo.CouponRedemption ON;
INSERT INTO dbo.CouponRedemption(redemption_id,coupon_id,user_id,order_id,claimed_at,used_at,discount_amount,created_at,updated_at) VALUES (1,1,4,7,'2025-12-31','2026-01-01',10000,'2025-12-31','2026-01-01');
SET IDENTITY_INSERT dbo.CouponRedemption OFF;
SET IDENTITY_INSERT dbo.OrderItem ON;
INSERT INTO dbo.OrderItem(order_item_id,order_id,product_id,variant_id,product_name,variant_name,quantity,unit_price,total_price,modifiers_json) VALUES (1,1,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(2,2,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(3,3,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(4,4,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(5,5,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(6,6,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(7,7,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]'),(8,8,1,1,N'Burger bò',N'Tiêu chuẩn',1,50000,50000,N'[]');
SET IDENTITY_INSERT dbo.OrderItem OFF;
SET IDENTITY_INSERT dbo.InventoryReservation ON;
INSERT INTO dbo.InventoryReservation(reservation_id,order_id,status,created_at,updated_at) VALUES (1,1,'RESERVED','2026-01-01','2026-01-01'),(2,7,'CONSUMED','2026-01-01','2026-01-01'),(3,8,'RELEASED','2026-01-01','2026-01-01');
SET IDENTITY_INSERT dbo.InventoryReservation OFF;
INSERT INTO dbo.InventoryReservationItem(reservation_id,inventory_item_id,quantity)
SELECT r.reservation_id,m.inventory_item_id,v.quantity
FROM (VALUES (1,1,CONVERT(decimal(19,4),1)),(2,1,CONVERT(decimal(19,4),1)),(3,1,CONVERT(decimal(19,4),1))) v(reservation_id,variant_id,quantity)
JOIN dbo.InventoryReservation r ON r.reservation_id=v.reservation_id
JOIN dbo.VariantInventoryItem m ON m.variant_id=v.variant_id;
SET IDENTITY_INSERT dbo.InventoryTransaction ON;
INSERT INTO dbo.InventoryTransaction(inventory_transaction_id,order_id,inventory_item_id,transaction_type,quantity,created_at,created_by,reason_code,note,quantity_before,quantity_after)
SELECT v.inventory_transaction_id,v.order_id,m.inventory_item_id,v.transaction_type,v.quantity,v.created_at,v.created_by,v.reason_code,v.note,v.quantity_before,v.quantity_after
FROM (VALUES (1,1,1,'RESERVE',CONVERT(decimal(19,4),1),CONVERT(datetime2(0),'2026-01-01'),2,CAST(NULL AS varchar(50)),N'Giữ tồn kho',CONVERT(decimal(19,4),100),CONVERT(decimal(19,4),99)),(2,7,1,'CONSUME',CONVERT(decimal(19,4),1),CONVERT(datetime2(0),'2026-01-01'),2,CAST(NULL AS varchar(50)),N'Hoàn tất đơn',CONVERT(decimal(19,4),99),CONVERT(decimal(19,4),98)),(3,8,1,'RELEASE',CONVERT(decimal(19,4),1),CONVERT(datetime2(0),'2026-01-01'),2,CAST(NULL AS varchar(50)),N'Hủy đơn',CONVERT(decimal(19,4),98),CONVERT(decimal(19,4),99))) v(inventory_transaction_id,order_id,variant_id,transaction_type,quantity,created_at,created_by,reason_code,note,quantity_before,quantity_after)
JOIN dbo.VariantInventoryItem m ON m.variant_id=v.variant_id;
SET IDENTITY_INSERT dbo.InventoryTransaction OFF;
SET IDENTITY_INSERT dbo.LoyaltyTransaction ON;
INSERT INTO dbo.LoyaltyTransaction(loyalty_transaction_id,user_id,order_id,transaction_type,points,created_at) VALUES (1,4,7,'EARN',55,'2026-01-01 09:30');
SET IDENTITY_INSERT dbo.LoyaltyTransaction OFF;
SET IDENTITY_INSERT dbo.Review ON;
INSERT INTO dbo.Review(review_id,user_id,order_id,product_id,rating,comment,created_at,updated_at) VALUES (1,4,7,1,5,N'Món ngon, giao nhanh.','2026-01-01 10:00','2026-01-01 10:00');
SET IDENTITY_INSERT dbo.Review OFF;
SET IDENTITY_INSERT dbo.OrderStatusHistory ON;
INSERT INTO dbo.OrderStatusHistory(history_id,order_id,actor_user_id,actor_role,from_status,to_status,note,created_at) VALUES (1,1,4,'USER',NULL,'PENDING',N'Đã tạo đơn.','2026-01-01 08:00'),(2,2,2,'STAFF','PENDING','CONFIRMED',N'Đã xác nhận.','2026-01-01 08:10'),(3,3,2,'STAFF','CONFIRMED','PREPARING',N'Đang chuẩn bị.','2026-01-01 08:20'),(4,4,2,'STAFF','PREPARING','READY',N'Sẵn sàng giao.','2026-01-01 08:30'),(5,5,2,'STAFF','READY','ASSIGNED',N'Đã phân công.','2026-01-01 08:40'),(6,6,3,'SHIPPER','ASSIGNED','PICKED_UP',N'Đã nhận món.','2026-01-01 08:50'),(7,7,3,'SHIPPER','PICKED_UP','DELIVERED',N'Đã giao.','2026-01-01 09:30'),(8,8,4,'USER','PENDING','CANCELLED',N'Khách hủy.','2026-01-01 08:15');
SET IDENTITY_INSERT dbo.OrderStatusHistory OFF;
GO
