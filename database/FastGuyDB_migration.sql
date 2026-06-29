-- ============================================================
-- FastGuyDB Migration Script
-- Chuyển từ schema cũ lên schema mới
-- ProductVariant, GHN fields, bỏ Ingredient/ProductOption
-- ============================================================

use FastGuyDB;
go

-- ============================================================
-- 1. PRODUCT: thêm base_price, gallery_images
-- ============================================================
if col_length('Product', 'base_price') is null
begin
    alter table Product add base_price decimal(10, 2);
    update Product set base_price = price where base_price is null;
end
go

if col_length('Product', 'gallery_images') is null
    alter table Product add gallery_images nvarchar(max);
go

-- ============================================================
-- 2. PRODUCT_VARIANT: tạo bảng mới
-- ============================================================
if object_id('ProductVariant', 'U') is null
begin
    create table ProductVariant
    (
        variant_id         int identity primary key,
        product_id         int not null references Product,
        variant_name       nvarchar(255) not null,
        price              decimal(10, 2) not null,
        original_price     decimal(10, 2),
        sku                varchar(100),
        quantity_available int,
        is_default         bit default 0,
        status             varchar(20) default 'AVAILABLE',
        created_at         datetime2 default getdate(),
        updated_at         datetime2
    );

    -- Tạo variant mặc định từ Product.price
    insert into ProductVariant (product_id, variant_name, price, is_default, status, created_at)
    select product_id, N'Mặc định', base_price, 1, status, getdate()
    from Product;

    -- Migrate ProductOption sang ProductVariant
    if object_id('ProductOption', 'U') is not null
    begin
        insert into ProductVariant (product_id, variant_name, price, is_default, quantity_available, status, created_at)
        select 
            po.product_id,
            po.option_name,
            isnull(p.base_price, 0) + isnull(po.extra_price, 0),
            0,
            po.quantity_available,
            case when p.status = 'AVAILABLE' then 'AVAILABLE' else 'UNAVAILABLE' end,
            getdate()
        from ProductOption po
        join Product p on po.product_id = p.product_id
        where not exists (
            select 1 from ProductVariant pv 
            where pv.product_id = po.product_id and pv.variant_name = po.option_name
        );
    end
end
go

-- ============================================================
-- 3. CART_ITEM: thêm variant_id, created_at
-- ============================================================
if col_length('CartItem', 'variant_id') is null
    alter table CartItem add variant_id int null references ProductVariant;
go

if col_length('CartItem', 'created_at') is null
    alter table CartItem add created_at datetime2 default getdate();
go

-- Gán variant mặc định cho cart item cũ
update ci
set ci.variant_id = pv.variant_id
from CartItem ci
join ProductVariant pv on ci.product_id = pv.product_id
where pv.is_default = 1 and ci.variant_id is null;
go

-- ============================================================
-- 4. ORDER_ITEM: thêm variant_id, variant_name
-- ============================================================
if col_length('OrderItem', 'variant_id') is null
    alter table OrderItem add variant_id int null references ProductVariant;
go

if col_length('OrderItem', 'variant_name') is null
    alter table OrderItem add variant_name nvarchar(255);
go

-- Gán variant mặc định cho order item cũ
update oi
set
    oi.variant_id = pv.variant_id,
    oi.variant_name = pv.variant_name
from OrderItem oi
join ProductVariant pv on oi.product_id = pv.product_id
where pv.is_default = 1 and oi.variant_id is null;
go

-- ============================================================
-- 5. ADDRESS: thêm GHN fields, đổi ward -> ward_name
-- ============================================================
if col_length('Address', 'ward_name') is null
begin
    exec sp_rename 'Address.ward', 'ward_name', 'COLUMN';
end
go

if col_length('Address', 'district_name') is null
    alter table Address add district_name nvarchar(100);
go

if col_length('Address', 'province_name') is null
    alter table Address add province_name nvarchar(100);
go

if col_length('Address', 'ghn_province_id') is null
    alter table Address add ghn_province_id int;
go

if col_length('Address', 'ghn_district_id') is null
    alter table Address add ghn_district_id int;
go

if col_length('Address', 'ghn_ward_code') is null
    alter table Address add ghn_ward_code varchar(50);
go

-- ============================================================
-- 6. ORDERS: thêm GHN fields, zone_id nullable
-- ============================================================
if col_length('Orders', 'to_province_name') is null
    alter table Orders add to_province_name nvarchar(100);
go

if col_length('Orders', 'to_district_name') is null
    alter table Orders add to_district_name nvarchar(100);
go

if col_length('Orders', 'to_ward_name') is null
    alter table Orders add to_ward_name nvarchar(100);
go

if col_length('Orders', 'ghn_province_id') is null
    alter table Orders add ghn_province_id int;
go

if col_length('Orders', 'ghn_district_id') is null
    alter table Orders add ghn_district_id int;
go

if col_length('Orders', 'ghn_ward_code') is null
    alter table Orders add ghn_ward_code varchar(50);
go

if col_length('Orders', 'shipping_provider') is null
    alter table Orders add shipping_provider varchar(30) default 'GHN';
go

if col_length('Orders', 'shipping_service_id') is null
    alter table Orders add shipping_service_id int;
go

if col_length('Orders', 'shipping_service_type_id') is null
    alter table Orders add shipping_service_type_id int;
go

if col_length('Orders', 'expected_delivery_time') is null
    alter table Orders add expected_delivery_time datetime2;
go

-- ============================================================
-- 7. XÓA BẢNG CŨ (chỉ xóa nếu còn)
-- ============================================================
if object_id('ProductIngredient', 'U') is not null
    drop table ProductIngredient;
go

if object_id('Ingredient', 'U') is not null
    drop table Ingredient;
go

if object_id('ProductOption', 'U') is not null
    drop table ProductOption;
go

-- ============================================================
-- 8. VERIFY
-- ============================================================
select 'Product' as tbl, count(*) as cnt from Product
union all
select 'ProductVariant', count(*) from ProductVariant
union all
select 'CartItem', count(*) from CartItem
union all
select 'OrderItem', count(*) from OrderItem
union all
select 'Address', count(*) from Address
union all
select 'Orders', count(*) from Orders;

select 'Migration complete!' as status;
go
