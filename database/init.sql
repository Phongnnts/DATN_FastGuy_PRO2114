create database FastGuyDB;
go

use FastGuyDB;
go

-- ============================================================
-- CREATE TABLES
-- ============================================================

create table Category
(
    category_id int identity
        primary key,
    name        nvarchar(255) not null,
    description nvarchar(500),
    sort_order  int         default 0,
    status      varchar(20) default 'ACTIVE'
)
go

create table DeliveryZone
(
    zone_id       int identity
        primary key,
    district_name nvarchar(100) not null,
    shipping_fee  decimal(10, 2) default 0,
    is_active     bit            default 1
)
go

create table Ingredient
(
    ingredient_id       int identity
        primary key,
    name                nvarchar(255) not null,
    unit                nvarchar(50)  not null,
    stock_quantity      decimal(10, 2) default 0,
    min_stock_threshold decimal(10, 2) default 0,
    status              varchar(20)    default 'ACTIVE'
)
go

create table Product
(
    product_id  int identity
        primary key,
    category_id int            not null
        references Category,
    name        nvarchar(255)  not null,
    description nvarchar(1000),
    price       decimal(10, 2) not null,
    image_url   varchar(500),
    status      varchar(20) default 'AVAILABLE',
    created_at  datetime2   default getdate(),
    updated_at  datetime2
)
go

create table ProductIngredient
(
    product_ingredient_id int identity
        primary key,
    product_id            int            not null
        references Product,
    ingredient_id         int            not null
        references Ingredient,
    quantity_required     decimal(10, 2) not null
)
go

create table ProductOption
(
    option_id          int identity
        primary key,
    product_id         int           not null
        references Product,
    option_name        nvarchar(255) not null,
    extra_price        decimal(10, 2) default 0,
    stock_controlled   bit            default 0,
    quantity_available int
)
go

create table Role
(
    role_id   int identity
        primary key,
    role_name varchar(50) not null
        unique
)
go

create table Users
(
    user_id       int identity
        primary key,
    role_id       int           not null
        references Role,
    email         varchar(255)
        unique,
    phone         varchar(20)   not null
        unique,
    password_hash varchar(255)  not null,
    full_name     nvarchar(255) not null,
    avatar_url    varchar(500),
    status        varchar(20) default 'ACTIVE',
    created_at    datetime2   default getdate()
)
go

create table Address
(
    address_id     int identity
        primary key,
    user_id        int           not null
        references Users,
    recipient_name nvarchar(255) not null,
    phone          varchar(20)   not null,
    street         nvarchar(255) not null,
    ward           nvarchar(100),
    zone_id        int           not null
        references DeliveryZone,
    city           nvarchar(100) default N'TP. Hồ Chí Minh',
    is_default     bit           default 0,
    created_at     datetime2     default getdate()
)
go
create table Cart
(
    cart_id    int identity
        primary key,
    user_id    int
        references Users,
    session_id varchar(128),
    created_at datetime2 default getdate()
)
go

create table CartItem
(
    cart_item_id int identity
        primary key,
    cart_id      int            not null
        references Cart,
    product_id   int            not null
        references Product,
    quantity     int            not null,
    option_data  nvarchar(max),
    unit_price   decimal(10, 2) not null
)
go

create table Orders
(
    order_id         int identity
        primary key,
    order_code       varchar(50)    not null
        unique,
    user_id          int
        references Users,
    customer_name    nvarchar(255)  not null,
    customer_phone   varchar(20)    not null,
    customer_address nvarchar(500)  not null,
    zone_id          int            not null
        references DeliveryZone,
    total_amount     decimal(10, 2) not null,
    shipping_fee     decimal(10, 2) default 0,
    final_amount     decimal(10, 2) not null,
    payment_method   varchar(50)    not null,
    payment_status   varchar(20)    default 'UNPAID',
    order_status     varchar(30)    default 'PENDING',
    staff_id         int
        references Users,
    shipper_id       int
        references Users,
    assigned_at      datetime2,
    confirmed_at     datetime2,
    ready_at         datetime2,
    picked_up_at     datetime2,
    delivered_at     datetime2,
    cancelled_at     datetime2,
    failure_reason   nvarchar(500),
    internal_note    nvarchar(1000),
    delivery_note    nvarchar(500),
    created_at       datetime2      default getdate()
)
go

create table OrderItem
(
    order_item_id int identity
        primary key,
    order_id      int            not null
        references Orders,
    product_id    int
        references Product,
    product_name  nvarchar(255)  not null,
    quantity      int            not null,
    unit_price    decimal(10, 2) not null,
    option_data   nvarchar(max),
    total_price   decimal(10, 2) not null
)
go

create table Payment
(
    payment_id     int identity
        primary key,
    order_id       int            not null
        references Orders,
    amount         decimal(10, 2) not null,
    payment_method varchar(50)    not null,
    transaction_id varchar(255),
    status         varchar(20),
    paid_at        datetime2,
    shipper_id     int
        references Users,
    collected_at   datetime2
)
go

create table Review
(
    review_id  int identity
        primary key,
    user_id    int not null
        references Users,
    order_id   int not null
        references Orders,
    product_id int
        references Product,
    rating     int not null,
    comment    nvarchar(1000),
    created_at datetime2 default getdate()
)
go

create table WorkShift
(
    shift_id   int identity
        primary key,
    shift_name nvarchar(100) not null,
    start_time time          not null,
    end_time   time          not null,
    role_type  varchar(20)   not null
)
go

create table Schedule
(
    schedule_id    int identity
        primary key,
    user_id        int  not null
        references Users,
    shift_id       int  not null
        references WorkShift,
    work_date      date not null,
    assigned_by    int  not null
        references Users,
    status         varchar(20) default 'PENDING',
    checked_in_at  datetime2,
    checked_out_at datetime2,
    note           nvarchar(500),
    created_at     datetime2   default getdate()
)
go


-- ============================================================
-- 1. ROLE
-- ============================================================
insert into Role (role_name) values ('USER');     -- 1
insert into Role (role_name) values ('STAFF');   -- 2
insert into Role (role_name) values ('SHIPPER'); -- 3
insert into Role (role_name) values ('ADMIN');   -- 4
insert into Role (role_name) values ('GUEST');   -- 5

-- ============================================================
-- 2. CATEGORY
-- ============================================================
insert into Category (name, description, sort_order, status) values (N'Burger', N'Burger thơm ngon', 1, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Gà Rán', N'Gà rán giòn rụm', 2, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Tacos & Wraps', N'Tacos và Wrap đa dạng', 3, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Pizza', N'Pizza nóng hổi', 4, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Món Ăn Kèm', N'Đồ ăn kèm hấp dẫn', 5, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Cơm', N'Cơm phần dinh dưỡng', 6, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Cơm Tấm', N'Cơm tấm Sài Gòn', 7, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Cơm Rang', N'Cơm rang thập cẩm', 8, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Bánh Mì', N'Bánh mì Việt Nam', 9, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Món Cuốn', N'Đồ cuốn tươi ngon', 10, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Đồ Uống', N'Thức uống giải khát', 11, 'ACTIVE');
insert into Category (name, description, sort_order, status) values (N'Tráng Miệng', N'Món tráng miệng ngọt ngào', 12, 'ACTIVE');

-- ============================================================
-- 3. PRODUCT
-- ============================================================

-- Burger (category_id = 1)
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Classic Beef Burger', N'Burger bò cổ điển với thịt bò nướng, rau xà lách, cà chua và sốt đặc biệt', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Double Cheese Burger', N'Burger bò với hai lớp phô mai béo ngậy', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'BBQ Bacon Burger', N'Burger bò kèm thịt xông khói và sốt BBQ', 59000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Crispy Chicken Burger', N'Burger gà chiên giòn với rau sống tươi mát', 49000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Spicy Chicken Burger', N'Burger gà cay với sốt cay Hàn Quốc', 52000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Vietnamese Bánh Mì Burger', N'Burger phong cách Việt Nam với đồ chua, rau thơm và pate', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (1, N'Teriyaki Burger', N'Burger thịt bò sốt Teriyaki đậm đà', 56000, '', 'AVAILABLE');

-- Gà Rán (category_id = 2)
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Gà Rán Giòn Truyền Thống', N'Gà rán giòn rụm, công thức truyền thống', 49000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Gà Rán Cay Hàn Quốc', N'Gà rán sốt cay Hàn Quốc đậm đà', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Gà Sốt Mật Ong', N'Gà rán sốt mật ong ngọt ngào', 52000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Gà Sốt Phô Mai', N'Gà rán phủ sốt phô mai béo ngậy', 58000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Cánh Gà BBQ', N'Cánh gà nướng sốt BBQ thơm lừng', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (2, N'Gà Popcorn', N'Gà viên chiên giòn ăn liền', 35000, '', 'AVAILABLE');

-- Tacos & Wraps (category_id = 3)
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Beef Tacos', N'Tacos bò với rau củ tươi và sốt salsa', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Chicken Tacos', N'Tacos gà với sốt kem chua', 42000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Shrimp Tacos', N'Tacos tôm tươi với sốt bơ tỏi', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Spicy Vietnamese Pork Tacos', N'Tacos thịt heo cay phong cách Việt', 48000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Chicken Wrap', N'Wrap gà tươi mát với rau củ', 42000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (3, N'Beef Wrap', N'Wrap bò với phô mai và sốt đặc biệt', 45000, '', 'AVAILABLE');

-- Pizza (category_id = 4)
insert into Product (category_id, name, description, price, image_url, status) values (4, N'Pepperoni Pizza', N'Pizza pepperoni truyền thống với phô mai mozzarella', 89000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (4, N'Hawaiian Pizza', N'Pizza Hawaii với dứa và jambon', 85000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (4, N'BBQ Chicken Pizza', N'Pizza gà BBQ với hành tây và phô mai', 92000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (4, N'Seafood Pizza', N'Pizza hải sản với tôm, mực và sốt đặc biệt', 99000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (4, N'Pizza Bò Nướng Việt Nam', N'Pizza bò nướng phong cách Việt Nam', 95000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (4, N'Pizza Gà Xé Phô Mai', N'Pizza gà xé phô mai béo ngậy', 89000, '', 'AVAILABLE');

-- Món Ăn Kèm (category_id = 5)
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Khoai Tây Chiên', N'Khoai tây chiên giòn vàng', 20000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Khoai Lắc Phô Mai', N'Khoai tây lắc phô mai thơm ngon', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Khoai Tây Múi Cau', N'Khoai tây cắt múi cau chiên giòn', 22000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Hành Tây Chiên', N'Hành tây chiên giòn rụm', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Mozzarella Sticks', N'Phô mai Mozzarella chiên giòn', 35000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Salad Rau Củ', N'Salad rau củ tươi ngon với sốt dầu giấm', 28000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (5, N'Nugget Gà', N'Nugget gà chiên giòn', 30000, '', 'AVAILABLE');

-- Cơm (category_id = 6)
insert into Product (category_id, name, description, price, image_url, status) values (6, N'Cơm Gà Chiên Nước Mắm', N'Cơm gà chiên giòn sốt nước mắm đậm đà', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (6, N'Cơm Gà Xối Mỡ', N'Cơm gà xối mỡ thơm béo', 42000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (6, N'Cơm Sườn Nướng', N'Cơm sườn nướng than hoa', 48000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (6, N'Cơm Bò Lúc Lắc', N'Cơm bò lúc lắc với rau củ xào', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (6, N'Cơm Gà Sốt Tiêu Đen', N'Cơm gà sốt tiêu đen cay nồng', 48000, '', 'AVAILABLE');

-- Cơm Tấm (category_id = 7)
insert into Product (category_id, name, description, price, image_url, status) values (7, N'Cơm Tấm Sườn', N'Cơm tấm sườn nướng với mỡ hành', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (7, N'Cơm Tấm Sườn Bì Chả', N'Cơm tấm đầy đủ sườn, bì, chả trứng', 55000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (7, N'Cơm Tấm Gà Nướng', N'Cơm tấm gà nướng thơm lừng', 48000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (7, N'Cơm Tấm Sườn Trứng', N'Cơm tấm sườn nướng kèm trứng ốp la', 50000, '', 'AVAILABLE');

-- Cơm Rang (category_id = 8)
insert into Product (category_id, name, description, price, image_url, status) values (8, N'Cơm Rang Dương Châu', N'Cơm rang Dương Châu thập cẩm', 40000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (8, N'Cơm Rang Gà Xé', N'Cơm rang gà xé với rau củ', 42000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (8, N'Cơm Rang Bò', N'Cơm rang bò đậm đà', 45000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (8, N'Cơm Rang Hải Sản', N'Cơm rang hải sản tôm mực', 50000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (8, N'Cơm Rang Kim Chi', N'Cơm rang kim chi Hàn Quốc cay nhẹ', 42000, '', 'AVAILABLE');

-- Bánh Mì (category_id = 9)
insert into Product (category_id, name, description, price, image_url, status) values (9, N'Bánh Mì Thịt Nướng', N'Bánh mì thịt nướng với đồ chua, rau thơm', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (9, N'Bánh Mì Gà Xé', N'Bánh mì gà xé với sốt đặc biệt', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (9, N'Bánh Mì Xíu Mại', N'Bánh mì xíu mại sốt cà đậm đà', 35000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (9, N'Bánh Mì Chả Cá', N'Bánh mì chả cá chiên giòn', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (9, N'Bánh Mì Bò Phô Mai', N'Bánh mì bò nướng phô mai', 35000, '', 'AVAILABLE');

-- Món Cuốn (category_id = 10)
insert into Product (category_id, name, description, price, image_url, status) values (10, N'Gỏi Cuốn Tôm Thịt', N'Gỏi cuốn tôm thịt tươi mát', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (10, N'Bò Lá Lốt Cuốn', N'Bò cuốn lá lốt thơm nức', 35000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (10, N'Nem Nướng Cuốn', N'Nem nướng cuốn với rau sống', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (10, N'Chả Giò Chiên', N'Chả giò chiên giòn rụm', 25000, '', 'AVAILABLE');

-- Đồ Uống (category_id = 11)
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Coca-Cola', N'Nước giải khát Coca-Cola lon', 12000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Pepsi', N'Nước giải khát Pepsi lon', 12000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Sprite', N'Nước giải khát Sprite lon', 12000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Fanta', N'Nước giải khát Fanta lon', 12000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Trà Chanh', N'Trà chanh tươi mát lạnh', 20000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Trà Đào', N'Trà đào thơm ngon ngọt dịu', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Trà Tắc', N'Trà tắc (quất) chua ngọt giải nhiệt', 20000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Trà Sữa', N'Trà sữa trân châu đường đen', 35000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Cà Phê Sữa Đá', N'Cà phê sữa đá đậm đà', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Cà Phê Muối', N'Cà phê muối đặc biệt', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Nước Cam', N'Nước cam tươi nguyên chất', 30000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (11, N'Chanh Dây', N'Nước chanh dây chua ngọt', 25000, '', 'AVAILABLE');

-- Tráng Miệng (category_id = 12)
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Kem Vanilla', N'Kem vanilla mát lạnh', 15000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Kem Chocolate', N'Kem chocolate béo ngậy', 15000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Sundae Caramel', N'Sundae caramel với sốt caramel ngọt ngào', 25000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Bánh Flan', N'Bánh flan mềm mịn với caramel', 20000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Tiramisu Mini', N'Tiramisu mini vị cà phê Ý', 35000, '', 'AVAILABLE');
insert into Product (category_id, name, description, price, image_url, status) values (12, N'Bánh Su Kem', N'Bánh su kem tươi mát', 25000, '', 'AVAILABLE');

-- ============================================================
-- 4. INGREDIENT
-- ============================================================
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Thịt bò xay', N'kg', 50, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Thịt gà', N'kg', 60, 15, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Thịt heo', N'kg', 40, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Tôm tươi', N'kg', 20, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Mực', N'kg', 15, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh mì burger', N'cái', 200, 50, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh pizza', N'cái', 100, 20, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh tortilla', N'cái', 150, 30, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh mì Việt Nam', N'cái', 100, 20, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Cơm trắng', N'kg', 100, 20, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Cơm tấm', N'kg', 80, 15, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Phô mai Mozzarella', N'kg', 25, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Phô mai Cheddar', N'kg', 20, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Xà lách', N'kg', 15, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Cà chua', N'kg', 20, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Hành tây', N'kg', 25, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Khoai tây', N'kg', 80, 20, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Trứng', N'quả', 200, 50, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Sữa tươi', N'lít', 40, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Dầu ăn', N'lít', 30, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Sốt BBQ', N'lít', 15, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Sốt Teriyaki', N'lít', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Sốt mayonnaise', N'lít', 15, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Tương ớt', N'lít', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Nước mắm', N'lít', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bột chiên giòn', N'kg', 30, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Kem vanilla', N'lít', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Kem chocolate', N'lít', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Trà túi lọc', N'hộp', 20, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Cà phê bột', N'kg', 15, 5, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Đường', N'kg', 50, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bột bánh flan', N'hộp', 10, 3, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh su kem', N'cái', 50, 10, 'ACTIVE');
insert into Ingredient (name, unit, stock_quantity, min_stock_threshold, status) values (N'Bánh tiramisu', N'cái', 20, 5, 'ACTIVE');

-- ============================================================
-- 5. PRODUCT_INGREDIENT (mỗi product ~ 3-4 nguyên liệu)
-- ============================================================
-- Classic Beef Burger (product_id = 1)
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (1, 1, 0.15);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (1, 6, 1);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (1, 14, 0.03);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (1, 15, 0.02);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (1, 12, 0.02);

-- Gà Rán Giòn Truyền Thống (product_id = 8)
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (8, 2, 0.2);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (8, 26, 0.05);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (8, 20, 0.1);

-- Khoai Tây Chiên (product_id = 33)
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (33, 17, 0.2);
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (33, 20, 0.05);

-- Coca-Cola (product_id = 70)
insert into ProductIngredient (product_id, ingredient_id, quantity_required) values (56, 31, 0.02);

-- ============================================================
-- 6. PRODUCT_OPTION
-- ============================================================
-- Burger: thêm size
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (1, N'Size L (lớn)', 10000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (2, N'Size L (lớn)', 10000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (4, N'Size L (lớn)', 10000, 0, null);

-- Gà Rán: combo / miếng
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (8, N'Combo 3 miếng', 20000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (8, N'Combo 6 miếng', 45000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (13, N'Combo 6 cánh', 25000, 0, null);

-- Pizza: size
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (21, N'Size L (30cm)', 30000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (22, N'Size L (30cm)', 30000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (23, N'Size L (30cm)', 30000, 0, null);

-- Đồ Uống: size
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (60, N'Size L', 5000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (61, N'Size L', 5000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (62, N'Size L', 5000, 0, null);
insert into ProductOption (product_id, option_name, extra_price, stock_controlled, quantity_available) values (63, N'Size L', 5000, 0, null);

-- ============================================================
-- 7. DELIVERY_ZONE
-- ============================================================
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 1', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 2', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 3', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 4', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 5', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 6', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 7', 10000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 8', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 9', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 10', 10000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 11', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Quận 12', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Bình Thạnh', 10000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Tân Bình', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Tân Phú', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Gò Vấp', 12000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Phú Nhuận', 10000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Thủ Đức', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Bình Tân', 15000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Hóc Môn', 20000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Củ Chi', 25000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Nhà Bè', 20000, 1);
insert into DeliveryZone (district_name, shipping_fee, is_active) values (N'Cần Giờ', 30000, 1);

-- ============================================================
-- 8. USERS (admin, staff, shipper, customer mẫu)
-- password_hash placeholder: '$2a$10$...'
-- ============================================================
insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (4, 'admin@fastguy.com', '0901000001', '123456', N'Admin FastGuy', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (2, 'staff1@fastguy.com', '0901000002', '123456', N'Nguyễn Văn A', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (2, 'staff2@fastguy.com', '0901000003', '123456', N'Trần Thị B', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (3, 'shipper1@fastguy.com', '0901000004', '123456', N'Phạm Văn C', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (3, 'shipper2@fastguy.com', '0901000005', '123456', N'Lê Thị D', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (1, 'customer1@email.com', '0901000006', '123456', N'Hoàng Văn E', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (1, 'customer2@email.com', '0901000007', '123456', N'Mai Thị F', '', 'ACTIVE');

insert into Users (role_id, email, phone, password_hash, full_name, avatar_url, status)
values (1, null, '0901000008', '123456', N'Khách Vãng Lai 1', '', 'ACTIVE');

-- ============================================================
-- 9. ADDRESS
-- ============================================================
insert into Address (user_id, recipient_name, phone, street, ward, zone_id, city, is_default)
values (6, N'Hoàng Văn E', '0901000006', N'123 Lê Lợi', N'Phường Bến Nghé', 1, N'TP. Hồ Chí Minh', 1);

insert into Address (user_id, recipient_name, phone, street, ward, zone_id, city, is_default)
values (7, N'Mai Thị F', '0901000007', N'456 Nguyễn Huệ', N'Phường Bến Thành', 1, N'TP. Hồ Chí Minh', 1);

insert into Address (user_id, recipient_name, phone, street, ward, zone_id, city, is_default)
values (6, N'Hoàng Văn E', '0901000006', N'789 Võ Văn Kiệt', N'Phường Cô Giang', 4, N'TP. Hồ Chí Minh', 0);

-- ============================================================
-- 10. CART & CART_ITEM (giỏ hàng mẫu)
-- ============================================================
insert into Cart (user_id, session_id) values (6, null);
insert into Cart (user_id, session_id) values (7, null);

insert into CartItem (cart_id, product_id, quantity, option_data, unit_price) values (1, 1, 2, null, 45000);
insert into CartItem (cart_id, product_id, quantity, option_data, unit_price) values (1, 8, 1, '{"size":"Combo 3 miếng"}', 69000);
insert into CartItem (cart_id, product_id, quantity, option_data, unit_price) values (1, 56, 2, null, 12000);
insert into CartItem (cart_id, product_id, quantity, option_data, unit_price) values (2, 22, 1, '{"size":"Size L (30cm)"}', 115000);
insert into CartItem (cart_id, product_id, quantity, option_data, unit_price) values (2, 33, 1, null, 20000);

-- ============================================================
-- 11. ORDERS & ORDER_ITEM
-- ============================================================
insert into Orders (order_code, user_id, customer_name, customer_phone, customer_address, zone_id, total_amount, shipping_fee, final_amount, payment_method, payment_status, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, delivered_at, created_at)
values ('FG-20250601-001', 6, N'Hoàng Văn E', '0901000006', N'123 Lê Lợi, Phường Bến Nghé, Quận 1', 1, 162000, 15000, 177000, 'CASH', 'PAID', 'DELIVERED', 2, 4, '2025-06-01 10:15:00', '2025-06-01 10:20:00', '2025-06-01 10:40:00', '2025-06-01 10:50:00', '2025-06-01 11:10:00', '2025-06-01 10:00:00');

insert into Orders (order_code, user_id, customer_name, customer_phone, customer_address, zone_id, total_amount, shipping_fee, final_amount, payment_method, payment_status, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, delivered_at, created_at)
values ('FG-20250601-002', 7, N'Mai Thị F', '0901000007', N'456 Nguyễn Huệ, Phường Bến Thành, Quận 1', 1, 135000, 15000, 150000, 'BANKING', 'PAID', 'DELIVERED', 3, 5, '2025-06-01 11:30:00', '2025-06-01 11:35:00', '2025-06-01 12:00:00', '2025-06-01 12:10:00', '2025-06-01 12:30:00', '2025-06-01 11:15:00');

insert into Orders (order_code, user_id, customer_name, customer_phone, customer_address, zone_id, total_amount, shipping_fee, final_amount, payment_method, payment_status, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, delivered_at, created_at)
values ('FG-20250602-001', 6, N'Hoàng Văn E', '0901000006', N'789 Võ Văn Kiệt, Phường Cô Giang, Quận 4', 4, 95000, 12000, 107000, 'CASH', 'UNPAID', 'PENDING', null, null, null, null, null, null, null, '2025-06-02 14:00:00');

insert into Orders (order_code, user_id, customer_name, customer_phone, customer_address, zone_id, total_amount, shipping_fee, final_amount, payment_method, payment_status, order_status, staff_id, shipper_id, assigned_at, confirmed_at, ready_at, picked_up_at, delivered_at, created_at)
values ('FG-20250602-002', null, N'Nguyễn Vãng Lai', '0901888999', N'12 Nguyễn Trãi, Phường Phạm Ngũ Lão, Quận 1', 1, 45000, 15000, 60000, 'CASH', 'UNPAID', 'CONFIRMED', 2, 4, '2025-06-02 14:30:00', '2025-06-02 14:35:00', null, null, null, '2025-06-02 14:20:00');

-- Order Items cho Order 1
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (1, 1, N'Classic Beef Burger', 2, 45000, null, 90000);
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (1, 8, N'Gà Rán Giòn Truyền Thống', 1, 49000, '{"size":"Combo 3 miếng"}', 69000);
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (1, 56, N'Coca-Cola', 1, 12000, null, 12000);

-- Order Items cho Order 2
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (2, 22, N'Hawaiian Pizza', 1, 85000, '{"size":"Size L (30cm)"}', 115000);
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (2, 33, N'Khoai Tây Chiên', 1, 20000, null, 20000);

-- Order Items cho Order 3
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (3, 23, N'BBQ Chicken Pizza', 1, 92000, null, 92000);
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (3, 61, N'Trà Đào', 1, 25000, null, 25000);

-- Order Items cho Order 4
insert into OrderItem (order_id, product_id, product_name, quantity, unit_price, option_data, total_price) values (4, 4, N'Crispy Chicken Burger', 1, 49000, null, 49000);

-- ============================================================
-- 12. PAYMENT
-- ============================================================
insert into Payment (order_id, amount, payment_method, transaction_id, status, paid_at, shipper_id, collected_at) values (1, 177000, 'CASH', null, 'COMPLETED', '2025-06-01 11:10:00', 4, '2025-06-01 11:10:00');
insert into Payment (order_id, amount, payment_method, transaction_id, status, paid_at, shipper_id, collected_at) values (2, 150000, 'BANKING', 'BANK-TXN-001', 'COMPLETED', '2025-06-01 12:30:00', null, null);

-- ============================================================
-- 13. REVIEW
-- ============================================================
insert into Review (user_id, order_id, product_id, rating, comment) values (6, 1, 1, 5, N'Burger ngon, thịt bò mọng nước');
insert into Review (user_id, order_id, product_id, rating, comment) values (6, 1, 8, 4, N'Gà rán giòn, nhưng hơi mặn');
insert into Review (user_id, order_id, product_id, rating, comment) values (7, 2, 22, 5, N'Pizza Hawaiian rất ngon, phô mai nhiều');

-- ============================================================
-- 14. WORK_SHIFT
-- ============================================================
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Ca sáng', '07:00', '11:30', 'STAFF');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Ca trưa', '11:00', '14:30', 'STAFF');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Ca chiều', '14:00', '18:00', 'STAFF');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Ca tối', '17:00', '22:00', 'STAFF');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Shipper sáng', '07:00', '12:00', 'SHIPPER');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Shipper chiều', '12:00', '17:00', 'SHIPPER');
insert into WorkShift (shift_name, start_time, end_time, role_type) values (N'Shipper tối', '17:00', '22:00', 'SHIPPER');

-- ============================================================
-- 15. SCHEDULE
-- ============================================================
insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (2, 1, '2025-06-01', 1, 'COMPLETED', '2025-06-01 06:55:00', '2025-06-01 11:35:00', null);

insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (3, 3, '2025-06-01', 1, 'COMPLETED', '2025-06-01 13:55:00', '2025-06-01 18:05:00', null);

insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (4, 5, '2025-06-01', 1, 'COMPLETED', '2025-06-01 06:50:00', '2025-06-01 12:10:00', null);

insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (5, 6, '2025-06-01', 1, 'COMPLETED', '2025-06-01 11:50:00', '2025-06-01 17:10:00', null);

insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (2, 2, '2025-06-02', 1, 'CHECKED_IN', '2025-06-02 10:55:00', null, null);

insert into Schedule (user_id, shift_id, work_date, assigned_by, status, checked_in_at, checked_out_at, note)
values (4, 5, '2025-06-02', 1, 'CHECKED_IN', '2025-06-02 06:50:00', null, null);

