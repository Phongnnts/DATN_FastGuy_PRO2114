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
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51100, 'Run 000_preflight_history.sql first.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH('dbo.Users', 'role_name') IS NULL ALTER TABLE dbo.Users ADD role_name varchar(20) NULL;
    IF COL_LENGTH('dbo.Users', 'loyalty_points') IS NULL ALTER TABLE dbo.Users ADD loyalty_points int NULL;
    IF COL_LENGTH('dbo.Users', 'favorite_ids_json') IS NULL ALTER TABLE dbo.Users ADD favorite_ids_json nvarchar(max) NULL;
    IF COL_LENGTH('dbo.Users', 'updated_at') IS NULL ALTER TABLE dbo.Users ADD updated_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.Product', 'gallery_images') IS NULL ALTER TABLE dbo.Product ADD gallery_images nvarchar(max) NULL;
    IF COL_LENGTH('dbo.Product', 'available_from') IS NULL ALTER TABLE dbo.Product ADD available_from time(0) NULL;
    IF COL_LENGTH('dbo.Product', 'available_to') IS NULL ALTER TABLE dbo.Product ADD available_to time(0) NULL;
    IF COL_LENGTH('dbo.Product', 'updated_at') IS NULL ALTER TABLE dbo.Product ADD updated_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.CartItem', 'modifiers_json') IS NULL ALTER TABLE dbo.CartItem ADD modifiers_json nvarchar(max) NULL;
    IF COL_LENGTH('dbo.CartItem', 'updated_at') IS NULL ALTER TABLE dbo.CartItem ADD updated_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.OrderItem', 'modifiers_json') IS NULL ALTER TABLE dbo.OrderItem ADD modifiers_json nvarchar(max) NULL;
    IF COL_LENGTH('dbo.Orders', 'service_fee') IS NULL ALTER TABLE dbo.Orders ADD service_fee decimal(18,2) NULL;
    IF COL_LENGTH('dbo.Orders', 'idempotency_key') IS NULL ALTER TABLE dbo.Orders ADD idempotency_key varchar(100) NULL;
    IF COL_LENGTH('dbo.Orders', 'request_hash') IS NULL ALTER TABLE dbo.Orders ADD request_hash varchar(64) NULL;
    IF COL_LENGTH('dbo.Orders', 'idempotency_owner') IS NULL ALTER TABLE dbo.Orders ADD idempotency_owner varchar(100) NULL;
    IF COL_LENGTH('dbo.Orders', 'guest_return_proof_hash') IS NULL ALTER TABLE dbo.Orders ADD guest_return_proof_hash varchar(64) NULL;
    IF COL_LENGTH('dbo.Orders', 'cod_collected_amount') IS NULL ALTER TABLE dbo.Orders ADD cod_collected_amount decimal(18,2) NULL;
    IF COL_LENGTH('dbo.Orders', 'cod_collected_at') IS NULL ALTER TABLE dbo.Orders ADD cod_collected_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.Orders', 'refund_status') IS NULL ALTER TABLE dbo.Orders ADD refund_status varchar(20) NULL;
    IF COL_LENGTH('dbo.Orders', 'refund_amount') IS NULL ALTER TABLE dbo.Orders ADD refund_amount decimal(18,2) NULL;
    IF COL_LENGTH('dbo.Orders', 'refunded_at') IS NULL ALTER TABLE dbo.Orders ADD refunded_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.Orders', 'refund_note') IS NULL ALTER TABLE dbo.Orders ADD refund_note nvarchar(500) NULL;
    IF COL_LENGTH('dbo.Orders', 'internal_note') IS NULL ALTER TABLE dbo.Orders ADD internal_note nvarchar(1000) NULL;
    IF COL_LENGTH('dbo.Orders', 'updated_at') IS NULL ALTER TABLE dbo.Orders ADD updated_at datetime2(0) NULL;
    IF OBJECT_ID(N'dbo.CouponRedemption', N'U') IS NULL CREATE TABLE dbo.CouponRedemption (
        redemption_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CouponRedemption PRIMARY KEY,
        coupon_id int NOT NULL, user_id int NOT NULL, order_id int NULL, claimed_at datetime2(0) NOT NULL,
        used_at datetime2(0) NULL, discount_amount decimal(18,2) NULL, created_at datetime2(0) NOT NULL,
        updated_at datetime2(0) NOT NULL
    );
    IF OBJECT_ID(N'dbo.PaymentAttempt', N'U') IS NULL CREATE TABLE dbo.PaymentAttempt (
        payment_attempt_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_PaymentAttempt PRIMARY KEY,
        order_id int NOT NULL CONSTRAINT FK_PaymentAttempt_Order REFERENCES dbo.Orders(order_id),
        provider varchar(20) NOT NULL, provider_reference varchar(100) NULL, checkout_url varchar(500) NULL,
        amount decimal(18,2) NOT NULL, status varchar(20) NOT NULL, lease_token varchar(36) NULL,
        created_at datetime2(0) NOT NULL CONSTRAINT DF_PaymentAttempt_Created DEFAULT GETDATE(),
        updated_at datetime2(0) NOT NULL CONSTRAINT DF_PaymentAttempt_Updated DEFAULT GETDATE(),
        CONSTRAINT UQ_PaymentAttempt_Order UNIQUE (order_id),
        CONSTRAINT CK_PaymentAttempt_Amount CHECK (amount >= 0),
        CONSTRAINT CK_PaymentAttempt_Status CHECK (status IN ('CREATING','READY','PENDING','PAID','FAILED','EXPIRED','CANCELLED'))
    );
    IF OBJECT_ID(N'dbo.InventoryReservation', N'U') IS NULL CREATE TABLE dbo.InventoryReservation (
        reservation_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryReservation PRIMARY KEY,
        order_id int NOT NULL CONSTRAINT FK_InventoryReservation_Order REFERENCES dbo.Orders(order_id),
        variant_id int NOT NULL CONSTRAINT FK_InventoryReservation_Variant REFERENCES dbo.ProductVariant(variant_id),
        quantity int NOT NULL, status varchar(20) NOT NULL,
        created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Created DEFAULT GETDATE(),
        updated_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryReservation_Updated DEFAULT GETDATE(),
        CONSTRAINT UQ_InventoryReservation_OrderVariant UNIQUE (order_id, variant_id),
        CONSTRAINT CK_InventoryReservation_Quantity CHECK (quantity > 0),
        CONSTRAINT CK_InventoryReservation_Status CHECK (status IN ('RESERVED','CONSUMED','RELEASED'))
    );
    IF OBJECT_ID(N'dbo.InventoryTransaction', N'U') IS NULL CREATE TABLE dbo.InventoryTransaction (
        inventory_transaction_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_InventoryTransaction PRIMARY KEY,
        order_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Order REFERENCES dbo.Orders(order_id),
        variant_id int NOT NULL CONSTRAINT FK_InventoryTransaction_Variant REFERENCES dbo.ProductVariant(variant_id),
        transaction_type varchar(20) NOT NULL, quantity int NOT NULL,
        created_at datetime2(0) NOT NULL CONSTRAINT DF_InventoryTransaction_Created DEFAULT GETDATE(),
        CONSTRAINT CK_InventoryTransaction_Quantity CHECK (quantity > 0),
        CONSTRAINT CK_InventoryTransaction_Type CHECK (transaction_type IN ('RESERVE','RELEASE','CONSUME','RETURN','ADJUSTMENT'))
    );
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '010_expand_schema')
        INSERT dbo.SchemaMigrationHistory(migration_id, details) VALUES ('010_expand_schema', N'Nullable canonical columns and CouponRedemption added');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
