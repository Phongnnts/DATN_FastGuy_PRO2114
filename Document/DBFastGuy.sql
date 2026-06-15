sửa﻿-- ============================================
-- DATABASE: FastGuy
-- SQL Server
-- ============================================

CREATE DATABASE FastGuy;
GO

USE FastGuy;
GO

-- ============================================
-- 1. ROLE
-- ============================================
CREATE TABLE Role (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- ============================================
-- 2. PERMISSION
-- ============================================
CREATE TABLE Permission (
    permission_id INT IDENTITY(1,1) PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL
);

-- ============================================
-- 3. ROLE_PERMISSION
-- ============================================
CREATE TABLE RolePermission (
    role_id INT NOT NULL FOREIGN KEY REFERENCES Role(role_id),
    permission_id INT NOT NULL FOREIGN KEY REFERENCES Permission(permission_id),
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================
-- 4. USERS
-- ============================================
CREATE TABLE Users (
    user_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id INT NOT NULL FOREIGN KEY REFERENCES Role(role_id),
    email VARCHAR(255) NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Users_status CHECK (status IN (N'ACTIVE', N'LOCKED'))
);

-- ============================================
-- 5. DELIVERY_ZONE
-- ============================================
CREATE TABLE DeliveryZone (
    zone_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    district_name NVARCHAR(100) NOT NULL UNIQUE,
    shipping_fee DECIMAL(18,2) NOT NULL CHECK (shipping_fee >= 0),
    is_active BIT NOT NULL DEFAULT 1
);

-- ============================================
-- 6. ADDRESS
-- ============================================
CREATE TABLE Address (
    address_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    recipient_name NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    street NVARCHAR(255) NOT NULL,
    ward NVARCHAR(100) NULL,
    zone_id BIGINT NOT NULL FOREIGN KEY REFERENCES DeliveryZone(zone_id),
    city NVARCHAR(100) NOT NULL,
    is_default BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- ============================================
-- 7. CATEGORY
-- ============================================
CREATE TABLE Category (
    category_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    description NVARCHAR(500) NULL,
    sort_order INT DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
    CONSTRAINT CHK_Category_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);

-- ============================================
-- 8. PRODUCT
-- ============================================
CREATE TABLE Product (
    product_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    category_id BIGINT NOT NULL FOREIGN KEY REFERENCES Category(category_id),
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000) NULL,
    price DECIMAL(18,2) NOT NULL CHECK (price > 0),
    image_url VARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT N'AVAILABLE',
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT CHK_Product_status CHECK (status IN (N'AVAILABLE', N'UNAVAILABLE'))
);

-- ============================================
-- 9. PRODUCT_OPTION
-- ============================================
CREATE TABLE ProductOption (
    option_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    option_name NVARCHAR(255) NOT NULL,
    extra_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    stock_controlled BIT NOT NULL DEFAULT 0,
    quantity_available INT NULL
);

-- ============================================
-- 10. CART
-- ============================================
CREATE TABLE Cart (
    cart_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    session_id VARCHAR(128) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT CK_Cart_UserOrSession CHECK (user_id IS NOT NULL OR session_id IS NOT NULL)
);

-- ============================================
-- 11. CART_ITEM
-- ============================================
CREATE TABLE CartItem (
    cart_item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL FOREIGN KEY REFERENCES Cart(cart_id),
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    option_data NVARCHAR(MAX) NULL,
    unit_price DECIMAL(18,2) NOT NULL
);

-- ============================================
-- 12. ORDERS
-- ============================================
CREATE TABLE Orders (
    order_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    customer_name NVARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_address NVARCHAR(500) NOT NULL,
    zone_id BIGINT NOT NULL FOREIGN KEY REFERENCES DeliveryZone(zone_id),
    total_amount DECIMAL(18,2) NOT NULL,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(18,2) NOT NULL,
    payment_method NVARCHAR(50) NOT NULL,
    payment_status NVARCHAR(20) NOT NULL DEFAULT N'UNPAID',
    order_status NVARCHAR(30) NOT NULL DEFAULT N'PENDING',
    staff_id BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    shipper_id BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    assigned_at DATETIME2 NULL,
    confirmed_at DATETIME2 NULL,
    ready_at DATETIME2 NULL,
    picked_up_at DATETIME2 NULL,
    delivered_at DATETIME2 NULL,
    cancelled_at DATETIME2 NULL,
    failure_reason NVARCHAR(500) NULL,
    internal_note NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CHK_Orders_payment_method CHECK (payment_method IN (N'COD', N'VNPAY', N'MOMO', N'VIETQR')),
    CONSTRAINT CHK_Orders_payment_status CHECK (payment_status IN (N'UNPAID', N'PAID')),
    CONSTRAINT CHK_Orders_order_status CHECK (order_status IN (N'PENDING', N'CONFIRMED', N'READY', N'DELIVERING', N'DELIVERED', N'CANCELLED', N'FAILED'))
);

-- ============================================
-- 13. ORDER_ITEM
-- ============================================
CREATE TABLE OrderItem (
    order_item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    product_id BIGINT NULL FOREIGN KEY REFERENCES Product(product_id),
    product_name NVARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    option_data NVARCHAR(MAX) NULL,
    total_price DECIMAL(18,2) NOT NULL
);

-- ============================================
-- 14. PAYMENT
-- ============================================
CREATE TABLE Payment (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    amount DECIMAL(18,2) NOT NULL,
    payment_method NVARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) NULL,
    status NVARCHAR(20) NOT NULL,
    paid_at DATETIME2 NULL,
    shipper_id BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    collected_at DATETIME2 NULL,
    CONSTRAINT CHK_Payment_method CHECK (payment_method IN (N'COD', N'VNPAY', N'MOMO', N'VIETQR')),
    CONSTRAINT CHK_Payment_status CHECK (status IN (N'PENDING', N'SUCCESS', N'FAILED'))
);

-- ============================================
-- 15. INGREDIENT
-- ============================================
CREATE TABLE Ingredient (
    ingredient_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    unit NVARCHAR(50) NOT NULL,
    stock_quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    min_stock_threshold DECIMAL(18,2) NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
    CONSTRAINT CHK_Ingredient_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);

-- ============================================
-- 16. PRODUCT_INGREDIENT
-- ============================================
CREATE TABLE ProductIngredient (
    product_ingredient_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    ingredient_id BIGINT NOT NULL FOREIGN KEY REFERENCES Ingredient(ingredient_id),
    quantity_required DECIMAL(18,2) NOT NULL CHECK (quantity_required > 0),
    CONSTRAINT UQ_ProductIngredient UNIQUE (product_id, ingredient_id)
);

-- ============================================
-- 17. INVENTORY_TRANSACTION
-- ============================================
CREATE TABLE InventoryTransaction (
    transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ingredient_id BIGINT NOT NULL FOREIGN KEY REFERENCES Ingredient(ingredient_id),
    order_id BIGINT NULL FOREIGN KEY REFERENCES Orders(order_id),
    quantity_change DECIMAL(18,2) NOT NULL,
    reason NVARCHAR(50) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    created_by BIGINT NULL FOREIGN KEY REFERENCES Users(user_id),
    CONSTRAINT CHK_InventoryTransaction_reason CHECK (reason IN (N'STOCK_IN', N'ORDER_CONFIRM', N'ORDER_CANCEL', N'ADJUSTMENT'))
);

-- ============================================
-- 18. WORK_SHIFT
-- ============================================
CREATE TABLE WorkShift (
    shift_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shift_name NVARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    role_type NVARCHAR(20) NOT NULL,
    CONSTRAINT CHK_WorkShift_role CHECK (role_type IN (N'STAFF', N'SHIPPER'))
);

-- ============================================
-- 19. SCHEDULE
-- ============================================
CREATE TABLE Schedule (
    schedule_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    shift_id BIGINT NOT NULL FOREIGN KEY REFERENCES WorkShift(shift_id),
    work_date DATE NOT NULL,
    assigned_by BIGINT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    note NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_Schedule UNIQUE (user_id, work_date, shift_id)
);

-- ============================================
-- 20. REVIEW
-- ============================================
CREATE TABLE Review (
    review_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    order_id BIGINT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    product_id BIGINT NULL FOREIGN KEY REFERENCES Product(product_id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_users_role ON Users(role_id);
CREATE INDEX idx_users_phone ON Users(phone);
CREATE INDEX idx_users_status ON Users(status);

CREATE INDEX idx_address_user ON Address(user_id);
CREATE INDEX idx_address_zone ON Address(zone_id);

CREATE INDEX idx_product_category ON Product(category_id);
CREATE INDEX idx_product_status ON Product(status);

CREATE INDEX idx_cart_user ON Cart(user_id);
CREATE INDEX idx_cart_session ON Cart(session_id);
CREATE INDEX idx_cartitem_cart ON CartItem(cart_id);
CREATE INDEX idx_cartitem_product ON CartItem(product_id);

CREATE INDEX idx_orders_user ON Orders(user_id);
CREATE INDEX idx_orders_staff ON Orders(staff_id);
CREATE INDEX idx_orders_shipper ON Orders(shipper_id);
CREATE INDEX idx_orders_status ON Orders(order_status, created_at);
CREATE INDEX idx_orders_created ON Orders(created_at);
CREATE INDEX idx_orders_code ON Orders(order_code);
CREATE INDEX idx_orders_zone ON Orders(zone_id);

CREATE INDEX idx_orderitem_order ON OrderItem(order_id);
CREATE INDEX idx_orderitem_product ON OrderItem(product_id);

CREATE INDEX idx_payment_order ON Payment(order_id);
CREATE INDEX idx_payment_status ON Payment(status);

CREATE INDEX idx_ingredient_status ON Ingredient(status);
CREATE INDEX idx_productingredient_product ON ProductIngredient(product_id);
CREATE INDEX idx_productingredient_ingredient ON ProductIngredient(ingredient_id);

CREATE INDEX idx_inventory_ingredient ON InventoryTransaction(ingredient_id, created_at);
CREATE INDEX idx_inventory_order ON InventoryTransaction(order_id);

CREATE INDEX idx_schedule_user_date ON Schedule(user_id, work_date);
CREATE INDEX idx_schedule_shift ON Schedule(shift_id);

CREATE INDEX idx_review_order ON Review(order_id);
CREATE INDEX idx_review_product ON Review(product_id);

-- ============================================
-- SAMPLE DATA
-- ============================================

INSERT INTO Role (role_name) VALUES ('USER'), ('STAFF'), ('SHIPPER'), ('ADMIN'), ('GUEST');

INSERT INTO Permission (permission_name, description) VALUES
('VIEW_MENU', N'Xem thực đơn'),
('VIEW_PRODUCT', N'Xem chi tiết món ăn'),
('SEARCH_PRODUCT', N'Tìm kiếm món ăn'),
('MANAGE_CART', N'Quản lý giỏ hàng'),
('PLACE_ORDER', N'Đặt hàng'),
('VIEW_ORDER', N'Xem đơn hàng'),
('CANCEL_ORDER', N'Hủy đơn hàng'),
('MANAGE_ORDER', N'Quản lý đơn hàng (Staff)'),
('CONFIRM_ORDER', N'Xác nhận đơn hàng'),
('ASSIGN_SHIPPER', N'Phân công shipper'),
('VIEW_ASSIGNED_ORDER', N'Xem đơn được giao'),
('UPDATE_DELIVERY', N'Cập nhật trạng thái giao hàng'),
('MANAGE_PRODUCT', N'Quản lý món ăn'),
('MANAGE_CATEGORY', N'Quản lý danh mục'),
('MANAGE_USER', N'Quản lý người dùng'),
('MANAGE_INVENTORY', N'Quản lý tồn kho'),
('VIEW_STATISTICS', N'Xem thống kê'),
('EXPORT_REPORT', N'Xuất báo cáo'),
('MANAGE_SCHEDULE', N'Quản lý ca làm việc'),
('VIEW_SYSTEM_LOG', N'Xem nhật ký hệ thống');

-- USER permissions
INSERT INTO RolePermission (role_id, permission_id)
SELECT 1, permission_id FROM Permission WHERE permission_name IN
('VIEW_MENU', 'VIEW_PRODUCT', 'SEARCH_PRODUCT', 'MANAGE_CART', 'PLACE_ORDER', 'VIEW_ORDER', 'CANCEL_ORDER');

-- STAFF permissions
INSERT INTO RolePermission (role_id, permission_id)
SELECT 2, permission_id FROM Permission WHERE permission_name IN
('VIEW_MENU', 'VIEW_PRODUCT', 'SEARCH_PRODUCT', 'VIEW_ORDER', 'MANAGE_ORDER', 'CONFIRM_ORDER', 'ASSIGN_SHIPPER');

-- SHIPPER permissions
INSERT INTO RolePermission (role_id, permission_id)
SELECT 3, permission_id FROM Permission WHERE permission_name IN
('VIEW_MENU', 'VIEW_PRODUCT', 'VIEW_ORDER', 'VIEW_ASSIGNED_ORDER', 'UPDATE_DELIVERY');

-- ADMIN permissions
INSERT INTO RolePermission (role_id, permission_id)
SELECT 4, permission_id FROM Permission;

INSERT INTO DeliveryZone (district_name, shipping_fee, is_active) VALUES
(N'Quận 1', 15000, 1),
(N'Quận 3', 20000, 1),
(N'Quận 4', 20000, 1),
(N'Quận 5', 25000, 1),
(N'Quận 6', 25000, 1),
(N'Quận 7', 30000, 1),
(N'Quận 8', 25000, 1),
(N'Quận 10', 20000, 1),
(N'Quận 11', 25000, 1),
(N'Quận Bình Thạnh', 20000, 1),
(N'Quận Phú Nhuận', 20000, 1),
(N'Quận Tân Bình', 25000, 1),
(N'Quận Gò Vấp', 30000, 1),
(N'Quận 12', 35000, 1),
(N'Huyện Hóc Môn', 40000, 1),
(N'Huyện Bình Chánh', 45000, 1);

INSERT INTO Category (name, description, sort_order, status) VALUES
(N'Bánh mì', N'Các loại bánh mì kẹp', 1, N'ACTIVE'),
(N'Cơm', N'Cơm văn phòng, cơm chiên, cơm tấm...', 2, N'ACTIVE'),
(N'Mì / Phở', N'Mì xào, mì quảng, phở bò...', 3, N'ACTIVE'),
(N'Gà rán', N'Gà rán, cánh gà...', 4, N'ACTIVE'),
(N'Pizza', N'Pizza các loại', 5, N'ACTIVE'),
(N'Đồ uống', N'Nước ngọt, trà sữa, cafe...', 6, N'ACTIVE');

INSERT INTO Product (category_id, name, description, price, image_url, status) VALUES
(1, N'Bánh mì thịt nướng', N'Bánh mì giòn, thịt heo nướng, rau thơm', 25000, NULL, N'AVAILABLE'),
(1, N'Bánh mì chảo', N'Bánh mì kèm pate, trứng ốp la, xúc xích', 35000, NULL, N'AVAILABLE'),
(1, N'Bánh mì gà xé', N'Thịt gà xé phay, mayonnaise, dưa leo', 30000, NULL, N'AVAILABLE'),
(1, N'Bánh mì đặc biệt', N'Thịt nướng, chả lụa, trứng, pate', 45000, NULL, N'AVAILABLE'),
(2, N'Cơm tấm sườn bì chả', N'Sườn nướng, bì, chả trứng', 45000, NULL, N'AVAILABLE'),
(2, N'Cơm chiên dương châu', N'Tôm, thịt gà, xá xíu, trứng', 55000, NULL, N'AVAILABLE'),
(2, N'Cơm gà Hội An', N'Cơm gà xé phay, rau thơm', 50000, NULL, N'AVAILABLE'),
(2, N'Cơm sườn non', N'Sườn non nướng mật ong', 55000, NULL, N'AVAILABLE'),
(3, N'Phở bò tái', N'Nước dùng xương bò, thịt bò tái', 45000, NULL, N'AVAILABLE'),
(3, N'Mì quảng', N'Tôm, thịt heo, bánh đa, đậu phộng', 45000, NULL, N'AVAILABLE'),
(3, N'Mì xào hải sản', N'Mì trứng xào tôm, mực, chả cá', 65000, NULL, N'AVAILABLE'),
(3, N'Bún bò Huế', N'Giò heo, chả lụa, nước lèo sả ớt', 50000, NULL, N'AVAILABLE'),
(4, N'Gà rán nguyên con', N'Gà rán giòn tan, tẩm bột gia vị', 120000, NULL, N'AVAILABLE'),
(4, N'Cánh gà rán sốt cay', N'6 miếng cánh gà, sốt Gochujang', 65000, NULL, N'AVAILABLE'),
(4, N'Đùi gà rán', N'2 đùi kèm khoai tây chiên', 70000, NULL, N'AVAILABLE'),
(4, N'Combo gà rán gia đình', N'1 gà, 2 cánh, 2 đùi, khoai tây', 250000, NULL, N'AVAILABLE'),
(5, N'Pizza hải sản (L)', N'Tôm, mực, thanh cua, phô mai', 150000, NULL, N'AVAILABLE'),
(5, N'Pizza thịt nguội (M)', N'Pepperoni, xúc xích, phô mai', 130000, NULL, N'AVAILABLE'),
(5, N'Pizza bò băm nấm (L)', N'Thịt bò băm, nấm, sốt BBQ', 140000, NULL, N'AVAILABLE'),
(5, N'Pizza gà cay (L)', N'Thịt gà ướp cay, phô mai mozzarella', 145000, NULL, N'AVAILABLE'),
(6, N'Coca cola (lon)', N'330ml', 15000, NULL, N'AVAILABLE'),
(6, N'Trà sữa trân châu', N'Trà sữa truyền thống, trân châu đen', 30000, NULL, N'AVAILABLE'),
(6, N'Cà phê sữa đá', N'Phin, sữa đặc, đá', 25000, NULL, N'AVAILABLE'),
(6, N'Nước cam ép', N'Cam tươi vắt 100%', 30000, NULL, N'AVAILABLE');

INSERT INTO WorkShift (shift_name, start_time, end_time, role_type) VALUES
(N'Ca sáng', '08:00', '12:00', N'STAFF'),
(N'Ca chiều', '13:00', '17:00', N'STAFF'),
(N'Ca tối', '18:00', '22:00', N'SHIPPER');

-- Password for all test users: "123456" (BCrypt hash — regenerate via PasswordUtil if needed)
-- Password for all test users: "123456" (BCrypt hash generated by PasswordUtil)
INSERT INTO Users (role_id, email, phone, password_hash, full_name, status) VALUES
(4, 'admin@fastguy.com',   '0900000001', '$2a$10$d4bbbmsSl3wmesbdrATrweBFzH9eD110D6.yexaD28Ho3ANDaUwjq', N'Quản trị viên', N'ACTIVE'),
(1, 'user@fastguy.com',    '0900000004', '$2a$10$d4bbbmsSl3wmesbdrATrweBFzH9eD110D6.yexaD28Ho3ANDaUwjq', N'Nguyễn Văn A',   N'ACTIVE'),
(2, 'staff@fastguy.com',   '0900000002', '$2a$10$d4bbbmsSl3wmesbdrATrweBFzH9eD110D6.yexaD28Ho3ANDaUwjq', N'Lê Thị B',       N'ACTIVE'),
(3, 'shipper@fastguy.com', '0900000003', '$2a$10$d4bbbmsSl3wmesbdrATrweBFzH9eD110D6.yexaD28Ho3ANDaUwjq', N'Trần Văn C',     N'ACTIVE');

GO
