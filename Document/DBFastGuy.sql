CREATE DATABASE FastGuy;
GO
USE FastGuy;
GO

CREATE TABLE Role (
    role_id INT PRIMARY KEY IDENTITY(1,1),
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Users (
    user_id INT PRIMARY KEY IDENTITY(1,1),
    role_id INT NOT NULL FOREIGN KEY REFERENCES Role(role_id),
    email VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    UNIQUE (phone),
    UNIQUE (email)
);

CREATE TABLE DeliveryZone (
    zone_id INT PRIMARY KEY IDENTITY(1,1),
    district_name NVARCHAR(100) NOT NULL,
    shipping_fee DECIMAL(10,2) DEFAULT 0,
    is_active BIT DEFAULT 1
);

CREATE TABLE Address (
    address_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    recipient_name NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    street NVARCHAR(255) NOT NULL,
    ward NVARCHAR(100),
    zone_id INT NOT NULL FOREIGN KEY REFERENCES DeliveryZone(zone_id),
    city NVARCHAR(100) DEFAULT N'TP. Hồ Chí Minh',
    is_default BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE Category (
    category_id INT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(500),
    sort_order INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

CREATE TABLE Product (
    product_id INT PRIMARY KEY IDENTITY(1,1),
    category_id INT NOT NULL FOREIGN KEY REFERENCES Category(category_id),
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000),
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2
);

CREATE TABLE ProductOption (
    option_id INT PRIMARY KEY IDENTITY(1,1),
    product_id INT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    option_name NVARCHAR(255) NOT NULL,
    extra_price DECIMAL(10,2) DEFAULT 0,
    stock_controlled BIT DEFAULT 0,
    quantity_available INT
);

CREATE TABLE Cart (
    cart_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT FOREIGN KEY REFERENCES Users(user_id),
    session_id VARCHAR(128),
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE CartItem (
    cart_item_id INT PRIMARY KEY IDENTITY(1,1),
    cart_id INT NOT NULL FOREIGN KEY REFERENCES Cart(cart_id),
    product_id INT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    quantity INT NOT NULL,
    option_data NVARCHAR(MAX),
    unit_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE Orders (
    order_id INT PRIMARY KEY IDENTITY(1,1),
    order_code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT FOREIGN KEY REFERENCES Users(user_id),
    customer_name NVARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_address NVARCHAR(500) NOT NULL,
    zone_id INT NOT NULL FOREIGN KEY REFERENCES DeliveryZone(zone_id),
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_fee DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    order_status VARCHAR(30) DEFAULT 'PENDING',
    staff_id INT FOREIGN KEY REFERENCES Users(user_id),
    shipper_id INT FOREIGN KEY REFERENCES Users(user_id),
    assigned_at DATETIME2,
    confirmed_at DATETIME2,
    ready_at DATETIME2,
    picked_up_at DATETIME2,
    delivered_at DATETIME2,
    cancelled_at DATETIME2,
    failure_reason NVARCHAR(500),
    internal_note NVARCHAR(1000),
    delivery_note NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE OrderItem (
    order_item_id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    product_id INT FOREIGN KEY REFERENCES Product(product_id),
    product_name NVARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    option_data NVARCHAR(MAX),
    total_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE Payment (
    payment_id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255),
    status VARCHAR(20),
    paid_at DATETIME2,
    shipper_id INT FOREIGN KEY REFERENCES Users(user_id),
    collected_at DATETIME2
);

CREATE TABLE Ingredient (
    ingredient_id INT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(255) NOT NULL,
    unit NVARCHAR(50) NOT NULL,
    stock_quantity DECIMAL(10,2) DEFAULT 0,
    min_stock_threshold DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

CREATE TABLE ProductIngredient (
    product_ingredient_id INT PRIMARY KEY IDENTITY(1,1),
    product_id INT NOT NULL FOREIGN KEY REFERENCES Product(product_id),
    ingredient_id INT NOT NULL FOREIGN KEY REFERENCES Ingredient(ingredient_id),
    quantity_required DECIMAL(10,2) NOT NULL
);

CREATE TABLE WorkShift (
    shift_id INT PRIMARY KEY IDENTITY(1,1),
    shift_name NVARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    role_type VARCHAR(20) NOT NULL
);

CREATE TABLE Schedule (
    schedule_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    shift_id INT NOT NULL FOREIGN KEY REFERENCES WorkShift(shift_id),
    work_date DATE NOT NULL,
    assigned_by INT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    status VARCHAR(20) DEFAULT 'PENDING',
    checked_in_at DATETIME2,
    checked_out_at DATETIME2,
    note NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE Review (
    review_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL FOREIGN KEY REFERENCES Users(user_id),
    order_id INT NOT NULL FOREIGN KEY REFERENCES Orders(order_id),
    product_id INT FOREIGN KEY REFERENCES Product(product_id),
    rating INT NOT NULL,
    comment NVARCHAR(1000),
    created_at DATETIME2 DEFAULT GETDATE()
);

INSERT INTO Role (role_name) VALUES ('USER'), ('STAFF'), ('SHIPPER'), ('ADMIN');
