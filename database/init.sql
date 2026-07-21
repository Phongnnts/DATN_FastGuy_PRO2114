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
    ward_name nvarchar(100),
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
    available_from time,
    available_to time,
    created_at datetime2 default getdate(),
    updated_at datetime2
);

create table ProductModifierGroup (
    modifier_group_id int identity primary key,
    product_id int not null references Product(product_id),
    name nvarchar(255) not null,
    min_selections int not null default 0,
    max_selections int not null default 1,
    is_active bit not null default 1,
    sort_order int not null default 0,
    constraint CK_ProductModifierGroup_Selections check (min_selections >= 0 and max_selections >= min_selections)
);

create table ProductModifierOption (
    modifier_option_id int identity primary key,
    modifier_group_id int not null references ProductModifierGroup(modifier_group_id),
    name nvarchar(255) not null,
    price decimal(10,2) not null default 0,
    is_active bit not null default 1,
    sort_order int not null default 0,
    constraint CK_ProductModifierOption_Price check (price >= 0)
);

create table ProductCombo (
    combo_id int identity primary key,
    product_id int not null unique references Product(product_id),
    is_active bit not null default 1
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
alter table ProductVariant add constraint CK_ProductVariant_QuantityAvailable check (quantity_available is null or quantity_available >= 0);

create table ProductComboItem (
    combo_item_id int identity primary key,
    combo_id int not null references ProductCombo(combo_id),
    product_id int not null references Product(product_id),
    variant_id int not null references ProductVariant(variant_id),
    quantity int not null default 1,
    sort_order int not null default 0,
    constraint CK_ProductComboItem_Quantity check (quantity > 0)
);


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
    loyalty_points int not null default 0,
    created_at datetime2 default getdate()
);
create unique index UQ__Users__AB6E6164B24E18DD on Users(email) where email is not null;
create unique index UQ__Users__B43B145F28B1640F on Users(phone);

create table PasswordResetToken (
    reset_token_id int identity primary key,
    user_id int not null references Users(user_id),
    token_hash varchar(64) not null unique,
    expires_at datetime2 not null,
    used_at datetime2,
    created_at datetime2 not null default getdate()
);
create index IX_PasswordResetToken_User on PasswordResetToken(user_id);

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
    selected_modifier_option_ids varchar(500),
    created_at datetime2 default getdate()
);
create unique index idx_cartitem_variant on CartItem(cart_id, variant_id, selected_modifier_option_ids);
alter table CartItem add constraint CK_CartItem_Quantity check (quantity > 0);


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
    service_fee decimal(10,2) default 0,
    final_amount decimal(10,2) not null,
    cod_collected_amount decimal(10,2),
    cod_collected_at datetime2,
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
    payos_payment_link_id varchar(100),
    payos_checkout_url varchar(500),
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
    cancelled_by varchar(20),
    refund_status varchar(20),
    refund_amount decimal(10,2),
    refunded_at datetime2,
    refund_note nvarchar(500),
    internal_note nvarchar(1000),
    delivery_note nvarchar(500),
    created_at datetime2 default getdate()
);
create unique index UQ__Orders__99D12D3FCA6940DB on Orders(order_code);

create table LoyaltyTransaction (
    loyalty_transaction_id int identity primary key,
    user_id int not null references Users(user_id),
    order_id int not null references Orders(order_id),
    transaction_type varchar(20) not null,
    points int not null,
    created_at datetime2 default getdate()
);
create unique index UQ_LoyaltyTransaction_Order_Type on LoyaltyTransaction(order_id, transaction_type);
create index IX_LoyaltyTransaction_User_Created on LoyaltyTransaction(user_id, created_at);

create table WorkShift (
    shift_id int identity primary key,
    user_id int not null references Users(user_id),
    shift_date date not null,
    start_time time not null,
    end_time time not null,
    check_in_at datetime2,
    check_out_at datetime2,
    status varchar(20) default 'SCHEDULED',
    created_at datetime2 default getdate()
);
create index IX_WorkShift_User_Date on WorkShift(user_id, shift_date);
create index IX_WorkShift_Date on WorkShift(shift_date);

create table CouponRedemption (
    redemption_id int identity primary key,
    coupon_id int not null references Coupon(coupon_id),
    user_id int not null references Users(user_id),
    order_id int references Orders(order_id),
    claimed_at datetime2 not null default getdate(),
    used_at datetime2,
    discount_amount decimal(18,2),
    created_at datetime2 not null default getdate(),
    updated_at datetime2 not null default getdate()
);
create unique index UQ_CouponRedemption_User_Coupon on CouponRedemption(user_id, coupon_id);
create unique index UQ_CouponRedemption_Order on CouponRedemption(order_id) where order_id is not null;

create table OrderItem (
    order_item_id int identity primary key,
    order_id int not null references Orders(order_id),
    product_id int references Product(product_id),
    variant_id int references ProductVariant(variant_id),
    product_name nvarchar(255) not null,
    variant_name nvarchar(255),
    quantity int not null,
    unit_price decimal(10,2) not null,
    total_price decimal(10,2) not null,
    constraint CK_OrderItem_Quantity check (quantity > 0),
    constraint CK_OrderItem_Amount check (unit_price >= 0 and total_price >= 0)
);
create index IX_OrderItem_Order on OrderItem(order_id);
create index IX_OrderItem_Variant on OrderItem(variant_id);

create table OrderItemModifier (
    order_item_modifier_id int identity primary key,
    order_item_id int not null references OrderItem(order_item_id),
    modifier_option_id int references ProductModifierOption(modifier_option_id),
    group_name nvarchar(255) not null,
    option_name nvarchar(255) not null,
    price decimal(10,2) not null,
    constraint CK_OrderItemModifier_Price check (price >= 0)
);
create index IX_OrderItemModifier_OrderItem on OrderItemModifier(order_item_id);

create table Review (
    review_id int identity primary key,
    user_id int not null references Users(user_id),
    order_id int not null references Orders(order_id),
    rating int not null,
    comment nvarchar(1000),
    created_at datetime2 not null default getdate(),
    updated_at datetime2 not null default getdate(),
    constraint CK_Review_Rating check (rating between 1 and 5)
);
create unique index UQ_Review_User_Order on Review(user_id, order_id);

create table SupportTicket (
    ticket_id int identity primary key,
    user_id int references Users(user_id),
    order_id int references Orders(order_id),
    subject nvarchar(255) not null,
    category varchar(30) not null,
    description nvarchar(2000) not null,
    status varchar(20) not null default 'OPEN',
    staff_id int references Users(user_id),
    resolution nvarchar(2000),
    created_at datetime2 default getdate(),
    updated_at datetime2 default getdate(),
    resolved_at datetime2,
    constraint CK_SupportTicket_Category check (category in ('MISSING_ITEM', 'COLD_FOOD', 'WRONG_ITEM', 'LATE_DELIVERY', 'OTHER')),
    constraint CK_SupportTicket_Status check (status in ('OPEN', 'PROCESSING', 'RESOLVED'))
);
create index IX_SupportTicket_User on SupportTicket(user_id, created_at);
create index IX_SupportTicket_Status on SupportTicket(status, created_at);

create table Notification (
    notification_id int identity primary key,
    user_id int references Users(user_id),
    role_name varchar(50),
    title nvarchar(255) not null,
    message nvarchar(1000),
    type varchar(50),
    target_url varchar(500),
    is_read bit default 0,
    created_at datetime2 default getdate()
);
create index IX_Notification_User_Read on Notification(user_id, is_read, created_at);
create index IX_Notification_Role_Read on Notification(role_name, is_read, created_at);

create table OrderStatusHistory (
    history_id int identity primary key,
    order_id int not null references Orders(order_id),
    actor_user_id int references Users(user_id),
    actor_role varchar(50),
    from_status varchar(30),
    to_status varchar(30) not null,
    note nvarchar(500),
    created_at datetime2 default getdate()
);
create index IX_OrderStatusHistory_Order on OrderStatusHistory(order_id, created_at);

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




insert into Category (name, description, sort_order, status) values
(N'Burger', N'Burger thơm ngon', 1, 'ACTIVE'),
(N'Gà Rán', N'Gà rán giòn rụm', 2, 'ACTIVE'),
(N'Tacos & Wraps', N'Tacos và Wrap đa dạng', 3, 'ACTIVE'),
(N'Pizza', N'Pizza nóng hổi', 4, 'ACTIVE'),
(N'Món Ăn Kèm', N'Đồ ăn kèm hấp dẫn', 5, 'ACTIVE'),
(N'Cơm', N'Cơm phần dinh dưỡng', 6, 'ACTIVE'),
(N'Cơm Tấm', N'Cơm tấm Sài Gòn', 7, 'ACTIVE'),
(N'Cơm Rang', N'Cơm rang thập cẩm', 8, 'ACTIVE'),
(N'Bánh Mì', N'Bánh mì Việt Nam', 9, 'ACTIVE'),
(N'Món Cuốn', N'Đồ cuốn tươi ngon', 10, 'ACTIVE'),
(N'Đồ Uống', N'Thức uống giải khát', 11, 'ACTIVE'),
(N'Tráng Miệng', N'Món tráng miệng ngọt ngào', 12, 'ACTIVE')
;
go

insert into DeliveryZone (district_name, shipping_fee, is_active) values
(N'Quận 1', 15000.00, 1),
(N'Quận 2', 15000.00, 1),
(N'Quận 3', 12000.00, 1),
(N'Quận 4', 12000.00, 1),
(N'Quận 5', 12000.00, 1),
(N'Quận 6', 12000.00, 1),
(N'Quận 7', 10000.00, 1),
(N'Quận 8', 12000.00, 1),
(N'Quận 9', 15000.00, 1),
(N'Quận 10', 10000.00, 1),
(N'Quận 11', 12000.00, 1),
(N'Quận 12', 15000.00, 1),
(N'Bình Thạnh', 10000.00, 1),
(N'Tân Bình', 12000.00, 1),
(N'Tân Phú', 12000.00, 1),
(N'Gò Vấp', 12000.00, 1),
(N'Phú Nhuận', 10000.00, 1),
(N'Thủ Đức', 15000.00, 1),
(N'Bình Tân', 15000.00, 1),
(N'Hóc Môn', 20000.00, 1),
(N'Củ Chi', 25000.00, 1),
(N'Nhà Bè', 20000.00, 1),
(N'Cần Giờ', 30000.00, 1)
;
go

insert into Product (category_id, name, description, base_price, image_url, gallery_images, status, created_at, updated_at) values
(1, N'Classic Beef Burger', N'Burger bò cổ điển với thịt bò nướng, rau xà lách, cà chua và sốt đặc biệt', 45000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104193/126c206b96e4f0028379a860c2c072a4_hog84a.jpg', N'["https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104198/126c206b96e4f0028379a860c2c072a4_vcmikp.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104202/243b84a0b420290d9832a60eff0842ff_zpsi6q.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104204/7595d43128b37efa858d8589220cad91_lgmlrs.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104205/db4076314df0396db67a5da459546f34_asecmc.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104206/ebcbc6aaa9deca9d6efc1efc93b66945_oy7ftc.jpg"]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:43:27 AM'),
(1, N'Double Cheese Burger', N'Burger bò với hai lớp phô mai béo ngậy', 55000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104217/243b84a0b420290d9832a60eff0842ff_ibzhzq.jpg', N'["https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104220/126c206b96e4f0028379a860c2c072a4_s2s5aq.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104221/243b84a0b420290d9832a60eff0842ff_hidvjw.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104222/7595d43128b37efa858d8589220cad91_fefzjh.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104223/db4076314df0396db67a5da459546f34_otxxpu.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104224/ebcbc6aaa9deca9d6efc1efc93b66945_ygwadj.jpg"]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:43:45 AM'),
(1, N'BBQ Bacon Burger', N'Burger bò kèm thịt xông khói và sốt BBQ', 59000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104234/7595d43128b37efa858d8589220cad91_sdn8yg.jpg', N'["https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104237/126c206b96e4f0028379a860c2c072a4_fwy5mg.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104237/243b84a0b420290d9832a60eff0842ff_dnghem.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104238/7595d43128b37efa858d8589220cad91_oh8asb.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104239/db4076314df0396db67a5da459546f34_lrnvff.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104239/ebcbc6aaa9deca9d6efc1efc93b66945_h38xoe.jpg"]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:43:59 AM'),
(1, N'Crispy Chicken Burger', N'Burger gà chiên giòn với rau sống tươi mát', 49000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104247/7595d43128b37efa858d8589220cad91_lxrkdh.jpg', N'["https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104249/126c206b96e4f0028379a860c2c072a4_wiy7le.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104257/126c206b96e4f0028379a860c2c072a4_oni756.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104258/243b84a0b420290d9832a60eff0842ff_aoox6o.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104259/7595d43128b37efa858d8589220cad91_aijnmo.jpg"]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:44:18 AM'),
(1, N'Spicy Chicken Burger', N'Burger gà cay với sốt cay Hàn Quốc', 52000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104267/db4076314df0396db67a5da459546f34_z65amc.jpg', N'["https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104271/243b84a0b420290d9832a60eff0842ff_jicsi0.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104271/7595d43128b37efa858d8589220cad91_adcnlt.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104272/db4076314df0396db67a5da459546f34_wyi0uo.jpg","https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104273/ebcbc6aaa9deca9d6efc1efc93b66945_tvlcp5.jpg"]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:44:32 AM'),
(1, N'Vietnamese Bánh Mì Burger', N'Burger phong cách Việt Nam với đồ chua, rau thơm và pate', 55000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104326/ebcbc6aaa9deca9d6efc1efc93b66945_olgng4.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:45:27 AM'),
(1, N'Teriyaki Burger', N'Burger thịt bò sốt Teriyaki đậm đà', 56000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104352/0c3aed9234d8e51462d885d5f247bb58_mk0qrp.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:45:51 AM'),
(2, N'Gà Rán Giòn Truyền Thống', N'Gà rán giòn rụm, công thức truyền thống', 49000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104344/1993ab5aec3e467f0e2c4ed132b06965_k9d8bu.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:45:44 AM'),
(2, N'Gà Rán Cay Hàn Quốc', N'Gà rán sốt cay Hàn Quốc đậm đà', 55000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104360/043728e36f7546b393e2d10d5b15e7d1_g43hb0.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:45:59 AM'),
(2, N'Gà Sốt Mật Ong', N'Gà rán sốt mật ong ngọt ngào', 52000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104373/99620ddd880507b3ff749b7f5eb5dced_ntoelc.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:46:13 AM'),
(2, N'Gà Sốt Phô Mai', N'Gà rán phủ sốt phô mai béo ngậy', 58000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104380/c606c301ad07be18041289c411f58724_cgnrhr.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:46:20 AM'),
(2, N'Cánh Gà BBQ', N'Cánh gà nướng sốt BBQ thơm lừng', 45000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104386/f8711d3824eab7f6df2970cbb76a91e0_bqpo9r.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:46:26 AM'),
(2, N'Gà Popcorn', N'Gà viên chiên giòn ăn liền', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(3, N'Beef Tacos', N'Tacos bò với rau củ tươi và sốt salsa', 45000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104404/8a427b668f2374794d5c965ce8f8ab84_hsqnz3.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:46:44 AM'),
(3, N'Chicken Tacos', N'Tacos gà với sốt kem chua', 42000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104421/156d17947f685e1c95a9585a07f9e19d_ojkc55.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:03 AM'),
(3, N'Shrimp Tacos', N'Tacos tôm tươi với sốt bơ tỏi', 55000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104411/156d17947f685e1c95a9585a07f9e19d_lep4ge.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:46:51 AM'),
(3, N'Spicy Vietnamese Pork Tacos', N'Tacos thịt heo cay phong cách Việt', 48000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104429/b01d97ea799b6a20929c0168d018bdab_kmjlcf.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:10 AM'),
(3, N'Chicken Wrap', N'Wrap gà tươi mát với rau củ', 42000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104488/f8711d3824eab7f6df2970cbb76a91e0_e2da8q.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:48:08 AM'),
(3, N'Beef Wrap', N'Wrap bò với phô mai và sốt đặc biệt', 45000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104479/1993ab5aec3e467f0e2c4ed132b06965_llsmio.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:59 AM'),
(4, N'Pepperoni Pizza', N'Pizza pepperoni truyền thống với phô mai mozzarella', 89000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104441/0c3aed9234d8e51462d885d5f247bb58_g4xgdm.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:21 AM'),
(4, N'Hawaiian Pizza', N'Pizza Hawaii với dứa và jambon', 85000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104469/99620ddd880507b3ff749b7f5eb5dced_bjez8x.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:50 AM'),
(4, N'BBQ Chicken Pizza', N'Pizza gà BBQ với hành tây và phô mai', 92000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104462/043728e36f7546b393e2d10d5b15e7d1_gpexai.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:42 AM'),
(4, N'Seafood Pizza', N'Pizza hải sản với tôm, mực và sốt đặc biệt', 99000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104448/1993ab5aec3e467f0e2c4ed132b06965_dcgxoj.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:47:28 AM'),
(4, N'Pizza Bò Nướng Việt ', N'Pizza bò nướng phong cách Việt Nam', 95000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104506/67af359f944c3d53a3c31ba1fe7ec85e_vdjp7y.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:48:26 AM'),
(4, N'Pizza Gà Xé Phô Mai', N'Pizza gà xé phô mai béo ngậy', 89000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104513/125c299199be679a76700b46f1ace0a6_mm431n.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:48:33 AM'),
(5, N'Khoai Tây Chiên', N'Khoai tây chiên giòn vàng', 20000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104524/d3352b478577a405415310a24c226601_zhh6k0.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:48:44 AM'),
(5, N'Khoai Lắc Phô Mai', N'Khoai tây lắc phô mai thơm ngon', 25000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104531/6631567b09bd9f177f08537f5a0e9cce_ida9ot.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:48:51 AM'),
(5, N'Khoai Tây Múi Cau', N'Khoai tây cắt múi cau chiên giòn', 22000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104540/63cac4a1c2ba62a7db43c4cfc85ecbbb_mkc1hc.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:00 AM'),
(5, N'Hành Tây Chiên', N'Hành tây chiên giòn rụm', 25000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104559/6631567b09bd9f177f08537f5a0e9cce_lbvzgy.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:19 AM'),
(5, N'Mozzarella Sticks', N'Phô mai Mozzarella chiên giòn', 35000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104550/63cac4a1c2ba62a7db43c4cfc85ecbbb_yukfbq.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:10 AM'),
(5, N'Salad Rau Củ', N'Salad rau củ tươi ngon với sốt dầu giấm', 28000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(5, N'Nugget Gà', N'Nugget gà chiên giòn', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(6, N'Cơm Gà Chiên Nước Mắm', N'Cơm gà chiên giòn sốt nước mắm đậm đà', 45000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104575/1b88452c4b325f4ae3f2f541c1ae5c70_c022zf.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:34 AM'),
(6, N'Cơm Gà Xối Mỡ', N'Cơm gà xối mỡ thơm béo', 42000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104581/6826f5120b2989195621cc75d6d35ad1_mudbpr.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:40 AM'),
(6, N'Cơm Sườn Nướng', N'Cơm sườn nướng than hoa', 48000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104605/f0dab7b828862eb2eb393eea634f99a9_rvszkt.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:50:05 AM'),
(6, N'Cơm Bò Lúc Lắc', N'Cơm bò lúc lắc với rau củ xào', 55000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104596/ccc4f3cdde4d55c5ab8e0905d7a18bb3_bm82kc.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:49:57 AM'),
(6, N'Cơm Gà Sốt Tiêu Đen', N'Cơm gà sốt tiêu đen cay nồng', 48000.00, 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1783104620/f0dab7b828862eb2eb393eea634f99a9_w4i4hl.jpg', N'[]', 'AVAILABLE', '7/4/2026 1:40:02 AM', '7/4/2026 1:50:19 AM'),
(7, N'Cơm Tấm Sườn', N'Cơm tấm sườn nướng với mỡ hành', 45000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(7, N'Cơm Tấm Sườn Bì Chả', N'Cơm tấm đầy đủ sườn, bì, chả trứng', 55000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(7, N'Cơm Tấm Gà Nướng', N'Cơm tấm gà nướng thơm lừng', 48000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(7, N'Cơm Tấm Sườn Trứng', N'Cơm tấm sườn nướng kèm trứng ốp la', 50000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Cơm Rang Dương Châu', N'Cơm rang Dương Châu thập cẩm', 40000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Cơm Rang Gà Xé', N'Cơm rang gà xé với rau củ', 42000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Cơm Rang Bò', N'Cơm rang bò đậm đà', 45000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Cơm Rang Hải Sản', N'Cơm rang hải sản tôm mực', 50000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Cơm Rang Kim Chi', N'Cơm rang kim chi Hàn Quốc cay nhẹ', 42000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Bánh Mì Thịt Nướng', N'Bánh mì thịt nướng với đồ chua, rau thơm', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Bánh Mì Gà Xé', N'Bánh mì gà xé với sốt đặc biệt', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Bánh Mì Xíu Mại', N'Bánh mì xíu mại sốt cà đậm đà', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Bánh Mì Chả Cá', N'Bánh mì chả cá chiên giòn', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Bánh Mì Bò Phô Mai', N'Bánh mì bò nướng phô mai', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(10, N'Gỏi Cuốn Tôm Thịt', N'Gỏi cuốn tôm thịt tươi mát', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(10, N'Bò Lá Lốt Cuốn', N'Bò cuốn lá lốt thơm nức', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(10, N'Nem Nướng Cuốn', N'Nem nướng cuốn với rau sống', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(10, N'Chả Giò Chiên', N'Chả giò chiên giòn rụm', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Coca-Cola', N'Nước giải khát Coca-Cola lon', 12000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Pepsi', N'Nước giải khát Pepsi lon', 12000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Sprite', N'Nước giải khát Sprite lon', 12000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Fanta', N'Nước giải khát Fanta lon', 12000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Trà Chanh', N'Trà chanh tươi mát lạnh', 20000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Trà Đào', N'Trà đào thơm ngon ngọt dịu', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Trà Tắc', N'Trà tắc (quất) chua ngọt giải nhiệt', 20000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Trà Sữa', N'Trà sữa trân châu đường đen', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Cà Phê Sữa Đá', N'Cà phê sữa đá đậm đà', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Cà Phê Muối', N'Cà phê muối đặc biệt', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Nước Cam', N'Nước cam tươi nguyên chất', 30000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Chanh Dây', N'Nước chanh dây chua ngọt', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Kem Vanilla', N'Kem vanilla mát lạnh', 15000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Kem Chocolate', N'Kem chocolate béo ngậy', 15000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Sundae Caramel', N'Sundae caramel với sốt caramel ngọt ngào', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Bánh Flan', N'Bánh flan mềm mịn với caramel', 20000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Tiramisu Mini', N'Tiramisu mini vị cà phê Ý', 35000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Bánh Su Kem', N'Bánh su kem tươi mát', 25000.00, NULL, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(1, N'Gà Việt', NULL, 0.00, NULL, N'[]', 'AVAILABLE', '7/4/2026 1:45:02 AM', NULL)
;
go

insert into ProductVariant (product_id, variant_name, price, original_price, sku, quantity_available, weight, length, width, height, is_default, status, created_at, updated_at) values
(1, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(2, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(3, N'Mặc định', 59000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(4, N'Mặc định', 49000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(5, N'Mặc định', 52000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(6, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(7, N'Mặc định', 56000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Mặc định', 49000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(9, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(10, N'Mặc định', 52000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(11, N'Mặc định', 58000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(12, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(13, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(14, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(15, N'Mặc định', 42000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(16, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(17, N'Mặc định', 48000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(18, N'Mặc định', 42000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(19, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(20, N'Mặc định', 89000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(21, N'Mặc định', 85000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(22, N'Mặc định', 92000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(23, N'Mặc định', 99000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(24, N'Mặc định', 95000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(25, N'Mặc định', 89000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(26, N'Mặc định', 20000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(27, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(28, N'Mặc định', 22000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(29, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(30, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(31, N'Mặc định', 28000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(32, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(33, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(34, N'Mặc định', 42000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(35, N'Mặc định', 48000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(36, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(37, N'Mặc định', 48000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(38, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(39, N'Mặc định', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(40, N'Mặc định', 48000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(41, N'Mặc định', 50000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(42, N'Mặc định', 40000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(43, N'Mặc định', 42000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(44, N'Mặc định', 45000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(45, N'Mặc định', 50000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(46, N'Mặc định', 42000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(47, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(48, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(49, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(50, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(51, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(52, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(53, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(54, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(55, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(56, N'Mặc định', 12000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(57, N'Mặc định', 12000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(58, N'Mặc định', 12000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(59, N'Mặc định', 12000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(60, N'Mặc định', 20000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(61, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(62, N'Mặc định', 20000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(63, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(64, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(65, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(66, N'Mặc định', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(67, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(68, N'Mặc định', 15000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(69, N'Mặc định', 15000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(70, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(71, N'Mặc định', 20000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(72, N'Mặc định', 35000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(73, N'Mặc định', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(1, N'Size L (lớn)', 55000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(2, N'Size L (lớn)', 65000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(4, N'Size L (lớn)', 59000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Combo 3 miếng', 69000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(8, N'Combo 6 miếng', 94000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(13, N'Combo 6 cánh', 70000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(20, N'Size L (30cm)', 119000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(21, N'Size L (30cm)', 115000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, NULL, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(22, N'Size L (30cm)', 122000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(60, N'Size L', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(61, N'Size L', 30000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(62, N'Size L', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL),
(63, N'Size L', 25000.00, NULL, NULL, 100, 500.00, 20.00, 20.00, 10.00, 0, 'AVAILABLE', '7/4/2026 1:40:02 AM', NULL)
;
update ProductVariant set is_default = 1 where variant_name = N'Mặc định';
go

insert into ShippingConfig (config_key, config_value) values
('ghn_from_district_id', '1442'),
('ghn_from_ward_code', '20107'),
('default_weight', '500'),
('default_length', '20'),
('default_width', '20'),
('default_height', '10'),
('default_service_type_id', '2'),
('business_open_time', '08:00'),
('business_close_time', '22:00'),
('service_fee', '0')
;
go

insert into Role (role_name) values
('ADMIN'),
('GUEST'),
('SHIPPER'),
('STAFF'),
('USER')
;
go

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status, created_at) values
(1, 'admin@fastguy.com', '0901000001', 'pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=', N'Nam Phong', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(4, 'staff1@fastguy.com', '0901000002', '123456', N'Nguyễn Văn A', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(4, 'staff2@fastguy.com', '0901000003', '123456', N'Trần Thị B', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(3, 'shipper1@fastguy.com', '0901000004', '123456', N'Phạm Văn C', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(3, 'shipper2@fastguy.com', '0901000005', '123456', N'Lê Thị D', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(5, 'user1@gmail.com', '0901000006', 'pbkdf2$120000$JpbGvZddz37orCzWxpmabw==$Ld1dCzqT9N0fD5cRfj0S6HF16f59UQZAulLRTvFc/sE=', N'Phúc Khang', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(5, 'user2@gmail.com', '0901000007', '123456', N'Mai Thị F', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM'),
(2, NULL, '0901000008', '123456', N'Khách Vãng Lai 1', NULL, 'ACTIVE', '7/4/2026 1:40:02 AM')
;
go

insert into Banner (title, subtitle, image_url, link, sort_order, is_active, created_at) values
(N'Khuyến mãi mùa hè', N'Giảm đến 10% cho tất cả Burger', '/images/banner-summer.jpg', '/menu?category=1', 1, 1, '7/4/2026 1:40:02 AM'),
(N'Combo Gà Rán', N'Combo gà rán giòn rụm chỉ từ 35.000đ', '/images/banner-chicken.jpg', '/menu?category=2', 2, 1, '7/4/2026 1:40:02 AM'),
(N'Pizza Size L', N'Pizza size L chỉ từ 85.000đ', '/images/banner-pizza.jpg', '/menu?category=4', 3, 1, '7/4/2026 1:40:02 AM')
;
go

insert into Address (user_id, recipient_name, phone, street, ward_name, district_name, province_name, ghn_province_id, ghn_district_id, ghn_ward_code, zone_id, city, is_default, created_at) values
(6, N'Hoàng Văn E', '0901000006', N'123 Lê Lợi', N'Phường Bến Nghé', N'Quận 1', N'TP. Hồ Chí Minh', NULL, NULL, NULL, 1, N'TP. Hồ Chí Minh', NULL, '7/4/2026 1:40:02 AM'),
(7, N'Mai Thị F', '0901000007', N'456 Nguyễn Huệ', N'Phường Bến Thành', N'Quận 1', N'TP. Hồ Chí Minh', NULL, NULL, NULL, 1, N'TP. Hồ Chí Minh', NULL, '7/4/2026 1:40:02 AM'),
(6, N'Hoàng Văn E', '0901000006', N'789 Võ Văn Kiệt', N'Phường Cô Giang', N'Quận 4', N'TP. Hồ Chí Minh', NULL, NULL, NULL, 4, N'TP. Hồ Chí Minh', 0, '7/4/2026 1:40:02 AM')
;
go

insert into Cart (user_id, session_id, created_at) values
(6, NULL, '7/4/2026 1:40:02 AM'),
(7, NULL, '7/4/2026 1:40:02 AM'),
(2, NULL, '7/4/2026 1:41:50 AM'),
(1, NULL, '7/4/2026 1:42:56 AM')
;
go

insert into CartItem (cart_id, product_id, variant_id, quantity, unit_price, created_at) values
(2, 21, 81, 1, 115000.00, '7/4/2026 1:40:02 AM'),
(2, 26, 26, 1, 20000.00, '7/4/2026 1:40:02 AM'),
(1, 3, 3, 1, 59000.00, '7/8/2026 12:41:35 PM'),
(1, 10, 10, 1, 52000.00, '7/8/2026 2:14:36 PM'),
(1, 25, 25, 2, 89000.00, '7/8/2026 2:38:21 PM'),
(1, 5, 5, 1, 52000.00, '7/8/2026 3:43:29 PM')
;
go

insert into Coupon (code, type, value, min_order, max_discount, max_uses, used_count, expires_at, is_active, is_public, created_at) values
('WELCOME10', 'PERCENT', 10.00, 50000.00, 20000.00, 100, 1, '12/31/2026 11:59:59 PM', 1, 1, '7/4/2026 1:40:02 AM'),
('FREESHIP', 'FREE_SHIPPING', 0.00, 30000.00, NULL, 50, 0, '12/31/2026 11:59:59 PM', 1, 1, '7/4/2026 1:40:02 AM'),
('GIAM20K', 'FIXED', 20000.00, 100000.00, NULL, 30, 0, '12/31/2026 11:59:59 PM', 1, 1, '7/4/2026 1:40:02 AM')
;
go

insert into ClaimedCoupon (coupon_id, user_id, claimed_at, used_at) values
(1, 6, '6/1/2025 9:00:00 AM', NULL),
(2, 6, '6/1/2025 9:00:00 AM', NULL),
(3, 7, '6/2/2025 10:00:00 AM', NULL),
(3, 6, '7/8/2026 12:41:47 PM', NULL)
;
go

insert into Orders (order_code, user_id, customer_name, customer_phone, customer_address, zone_id, to_province_name, to_district_name, to_ward_name, ghn_province_id, ghn_district_id, ghn_ward_code, total_amount, shipping_fee, final_amount, shipping_provider, shipping_service_id, shipping_service_type_id, expected_delivery_time, ghn_order_code, ghn_tracking_url, ghn_status, from_district_id, from_ward_code, payment_method, payment_status, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, paid_at, delivered_at, cancelled_at, coupon_code, discount_amount, failure_reason, cancelled_by, refund_status, refund_note, internal_note, delivery_note, created_at) values
('FG-20250601-001', 6, N'Hoàng Văn E', '0901000006', N'123 Lê Lợi, Phường Bến Nghé, Quận 1', 1, NULL, NULL, NULL, NULL, NULL, NULL, 171000.00, 15000.00, 186000.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CASH', 'PAID', 'DELIVERED', 2, 4, '6/1/2025 10:15:00 AM', '6/1/2025 10:20:00 AM', '6/1/2025 10:40:00 AM', '6/1/2025 10:50:00 AM', '6/1/2025 11:10:00 AM', '6/1/2025 11:10:00 AM', NULL, NULL, 0.00, NULL, NULL, NULL, NULL, NULL, NULL, '6/1/2025 10:00:00 AM'),
('FG-20250601-002', 7, N'Mai Thị F', '0901000007', N'456 Nguyễn Huệ, Phường Bến Thành, Quận 1', 1, NULL, NULL, NULL, NULL, NULL, NULL, 135000.00, 15000.00, 150000.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'BANKING', 'PAID', 'DELIVERED', 3, 5, '6/1/2025 11:30:00 AM', '6/1/2025 11:35:00 AM', '6/1/2025 12:00:00 PM', '6/1/2025 12:10:00 PM', '6/1/2025 12:30:00 PM', '6/1/2025 12:30:00 PM', NULL, NULL, 0.00, NULL, NULL, NULL, NULL, NULL, NULL, '6/1/2025 11:15:00 AM'),
('FG-20250602-001', 6, N'Hoàng Văn E', '0901000006', N'789 Võ Văn Kiệt, Phường Cô Giang, Quận 4', 4, NULL, NULL, NULL, NULL, NULL, NULL, 117000.00, 12000.00, 129000.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CASH', 'UNPAID', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL, NULL, NULL, NULL, NULL, NULL, '6/2/2025 2:00:00 PM'),
('FG-20250602-002', NULL, N'Nguyễn Vãng Lai', '0901888999', N'12 Nguyễn Trãi, Phường Phạm Ngũ Lão, Quận 1', 1, NULL, NULL, NULL, NULL, NULL, NULL, 49000.00, 15000.00, 64000.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CASH', 'UNPAID', 'CONFIRMED', 2, 4, '6/2/2025 2:30:00 PM', '6/2/2025 2:35:00 PM', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL, NULL, NULL, NULL, NULL, NULL, '6/2/2025 2:20:00 PM'),
('GST-FA1F4F8A', NULL, N'Nam Phong', '0974211242', N'Nam Phong, Quang Trung, Phường Trung Mỹ Tây, Quận 12, Hồ Chí Minh', NULL, N'Hồ Chí Minh', N'Quận 12', N'Phường Trung Mỹ Tây', 202, 1454, '21211', 55000.00, 21001.00, 76001.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COD', 'UNPAID', 'READY', 2, 5, '7/4/2026 8:41:35 AM', '7/4/2026 1:41:58 AM', '7/4/2026 8:41:25 AM', NULL, NULL, NULL, NULL, NULL, 0.00, NULL, NULL, NULL, NULL, NULL, NULL, '7/4/2026 1:41:32 AM'),
('ORD-D9D9FD16', 6, N'Phúc Khang', '098765512331', N'Nam PHong, 123, Phường An Khánh, Thành Phố Thủ Đức, Hồ Chí Minh', NULL, N'Hồ Chí Minh', N'Thành Phố Thủ Đức', N'Phường An Khánh', 202, 3695, '90768', 387000.00, 21001.00, 388001.00, 'GHN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COD', 'UNPAID', 'READY', 2, 5, '7/8/2026 3:53:16 PM', '7/4/2026 8:40:08 AM', '7/8/2026 3:53:10 PM', NULL, NULL, NULL, NULL, 'WELCOME10', 20000.00, NULL, NULL, NULL, NULL, NULL, NULL, '7/4/2026 8:39:50 AM')
;
go

insert into CouponUsage (coupon_id, user_id, order_id, discount_amount, used_at) values
(1, 6, 6, 20000.00, '7/4/2026 8:39:50 AM')
;
go

insert into OrderItem (order_id, product_id, variant_id, product_name, variant_name, quantity, unit_price, total_price) values
(1, 1, 1, N'Classic Beef Burger', N'Mặc định', 2, 45000.00, 90000.00),
(1, 8, 77, N'Gà Rán Giòn Truyền Thống', N'Combo 3 miếng', 1, 69000.00, 69000.00),
(1, 56, 56, N'Coca-Cola', N'Mặc định', 1, 12000.00, 12000.00),
(2, 21, 81, N'Hawaiian Pizza', N'Size L (30cm)', 1, 115000.00, 115000.00),
(2, 26, 26, N'Khoai Tây Chiên', N'Mặc định', 1, 20000.00, 20000.00),
(3, 22, 22, N'BBQ Chicken Pizza', N'Mặc định', 1, 92000.00, 92000.00),
(3, 61, 61, N'Trà Đào', N'Mặc định', 1, 25000.00, 25000.00),
(4, 4, 4, N'Crispy Chicken Burger', N'Mặc định', 1, 49000.00, 49000.00),
(5, 2, 2, N'Double Cheese Burger', N'Mặc định', 1, 55000.00, 55000.00),
(6, 1, 1, N'Classic Beef Burger', N'Mặc định', 4, 45000.00, 180000.00),
(6, 8, 77, N'Gà Rán Giòn Truyền Thống', N'Combo 3 miếng', 3, 69000.00, 207000.00)
;
go

insert into FavoriteProduct (user_id, product_id, created_at) values
(6, 25, '7/8/2026 2:38:23 PM')
;
go

