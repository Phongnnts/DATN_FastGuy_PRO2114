create database FastGuyDB;
go

use FastGuyDB;
go

-- ============================================================
-- CREATE TABLES
-- ============================================================

create table Category (
    category_id int identity primary key,
    name nvarchar(255) not null,
    description nvarchar(500),
    sort_order int default 0,
    status varchar(20) default 'ACTIVE'
);

create table DeliveryZone (
    zone_id int identity primary key,
    district_name nvarchar(100) not null,
    shipping_fee decimal(10,2) default 0,
    is_active bit default 1
);

create table Product (
    product_id int identity primary key,
    category_id int not null references Category(category_id),
    name nvarchar(255) not null,
    description nvarchar(1000),
    base_price decimal(10,2),
    image_url varchar(500),
    gallery_images nvarchar(max),
    status varchar(20) default 'AVAILABLE',
    created_at datetime2 default getdate(),
    updated_at datetime2
);

create table ProductVariant (
    variant_id int identity primary key,
    product_id int not null references Product(product_id),
    variant_name nvarchar(255) not null,
    price decimal(10,2) not null,
    original_price decimal(10,2),
    sku varchar(100),
    quantity_available int,
    weight decimal(10,2) default 500,
    length decimal(10,2) default 20,
    width decimal(10,2) default 20,
    height decimal(10,2) default 10,
    is_default bit default 0,
    status varchar(20) default 'AVAILABLE',
    created_at datetime2 default getdate(),
    updated_at datetime2
);
create unique index idx_variant_sku on ProductVariant(sku) where sku is not null;

create table ShippingConfig (
    config_id int identity primary key,
    config_key varchar(100) not null,
    config_value varchar(500) not null
);
create unique index UQ__Shipping__BDF6033DB3EF9639 on ShippingConfig(config_key);

create table Role (
    role_id int identity primary key,
    role_name varchar(50) not null
);
create unique index UQ__Role__783254B111E61016 on Role(role_name);

create table Users (
    user_id int identity primary key,
    role_id int not null references Role(role_id),
    email varchar(255),
    phone varchar(20) not null,
    password_hash varchar(255) not null,
    full_name nvarchar(255) not null,
    avatar_url varchar(500),
    status varchar(20) default 'ACTIVE',
    created_at datetime2 default getdate()
);
create unique index UQ__Users__AB6E6164B24E18DD on Users(email) where email is not null;
create unique index UQ__Users__B43B145F28B1640F on Users(phone);

create table Banner (
    banner_id int identity primary key,
    title nvarchar(255),
    subtitle nvarchar(500),
    image_url varchar(500) not null,
    link varchar(500),
    sort_order int default 0,
    is_active bit default 1,
    created_at datetime2 default getdate()
);

create table Address (
    address_id int identity primary key,
    user_id int not null references Users(user_id),
    recipient_name nvarchar(255) not null,
    phone varchar(20) not null,
    street nvarchar(255) not null,
    ward_name nvarchar(100),
    district_name nvarchar(100),
    province_name nvarchar(100),
    ghn_province_id int,
    ghn_district_id int,
    ghn_ward_code varchar(50),
    zone_id int references DeliveryZone(zone_id),
    city nvarchar(100) default N'TP. Hồ Chí Minh',
    is_default bit default 0,
    created_at datetime2 default getdate()
);

create table Cart (
    cart_id int identity primary key,
    user_id int references Users(user_id),
    session_id varchar(128),
    created_at datetime2 default getdate()
);

create table CartItem (
    cart_item_id int identity primary key,
    cart_id int not null references Cart(cart_id),
    product_id int not null references Product(product_id),
    variant_id int not null references ProductVariant(variant_id),
    quantity int not null,
    unit_price decimal(10,2) not null,
    created_at datetime2 default getdate()
);
create unique index idx_cartitem_variant on CartItem(cart_id, variant_id);

create table Coupon (
    coupon_id int identity primary key,
    code varchar(50) not null,
    type varchar(20) not null,
    value decimal(10,2) not null,
    min_order decimal(10,2) default 0,
    max_discount decimal(10,2),
    max_uses int default 0,
    used_count int default 0,
    expires_at datetime2,
    is_active bit default 1,
    is_public bit default 1,
    created_at datetime2 default getdate()
);
create unique index UQ__Coupon__357D4CF93B725CDD on Coupon(code);

create table ClaimedCoupon (
    claimed_id int identity primary key,
    coupon_id int not null references Coupon(coupon_id),
    user_id int not null references Users(user_id),
    claimed_at datetime2 default getdate(),
    used_at datetime2
);
create unique index idx_claimedcoupon_user_coupon on ClaimedCoupon(user_id, coupon_id);

create table CouponUsage (
    coupon_usage_id int identity primary key,
    coupon_id int not null references Coupon(coupon_id),
    user_id int references Users(user_id),
    order_id int not null references Orders(order_id),
    discount_amount decimal(10,2) not null,
    used_at datetime2 default getdate()
);
create unique index idx_couponusage_order on CouponUsage(order_id);

create table Orders (
    order_id int identity primary key,
    order_code varchar(50) not null,
    user_id int references Users(user_id),
    customer_name nvarchar(255) not null,
    customer_phone varchar(20) not null,
    customer_address nvarchar(500) not null,
    zone_id int references DeliveryZone(zone_id),
    to_province_name nvarchar(100),
    to_district_name nvarchar(100),
    to_ward_name nvarchar(100),
    ghn_province_id int,
    ghn_district_id int,
    ghn_ward_code varchar(50),
    total_amount decimal(10,2) not null,
    shipping_fee decimal(10,2) default 0,
    final_amount decimal(10,2) not null,
    shipping_provider varchar(30) default 'GHN',
    shipping_service_id int,
    shipping_service_type_id int,
    expected_delivery_time datetime2,
    ghn_order_code varchar(50),
    ghn_tracking_url varchar(500),
    ghn_status varchar(30),
    from_district_id int,
    from_ward_code varchar(50),
    payment_method varchar(50) not null,
    payment_status varchar(20) default 'UNPAID',
    order_status varchar(30) default 'PENDING',
    staff_id int references Users(user_id),
    shipper_id int references Users(user_id),
    assigned_at datetime2,
    confirmed_at datetime2,
    ready_at datetime2,
    picked_up_at datetime2,
    paid_at datetime2,
    delivered_at datetime2,
    cancelled_at datetime2,
    coupon_code varchar(50),
    discount_amount decimal(10,2) default 0,
    failure_reason nvarchar(500),
    internal_note nvarchar(1000),
    delivery_note nvarchar(500),
    created_at datetime2 default getdate()
);
create unique index UQ__Orders__99D12D3FCA6940DB on Orders(order_code);

create table OrderItem (
    order_item_id int identity primary key,
    order_id int not null references Orders(order_id),
    product_id int references Product(product_id),
    variant_id int references ProductVariant(variant_id),
    product_name nvarchar(255) not null,
    variant_name nvarchar(255),
    quantity int not null,
    unit_price decimal(10,2) not null,
    total_price decimal(10,2) not null
);

create table Review (
    review_id int identity primary key,
    user_id int not null references Users(user_id),
    order_id int not null references Orders(order_id),
    rating int not null,
    comment nvarchar(1000),
    created_at datetime2 default getdate()
);
create unique index UQ_Review_User_Order on Review(user_id, order_id);

create table FavoriteProduct (
    favorite_id int identity primary key,
    user_id int not null references Users(user_id),
    product_id int not null references Product(product_id),
    created_at datetime2 default getdate()
);
create unique index UQ_FavoriteProduct_User_Product on FavoriteProduct(user_id, product_id);

go

-- ============================================================
-- SEED DATA
-- ============================================================

insert into Role(role_name) values ('USER'), ('STAFF'), ('SHIPPER'), ('ADMIN'), ('GUEST');

-- Default admin (password: admin123)
insert into Users(role_id, email, phone, password_hash, full_name, status)
values (4, 'admin@fastguy.vn', '0900000000', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Admin FastGuy', 'ACTIVE');
go
