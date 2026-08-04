# FastGuy — ERD (Entity Relationship Diagram)

Tài liệu mô tả cấu trúc dữ liệu của hệ thống **FastGuy** theo hai mức: **ERD Level 1** (quan niệm, danh sách thực thể và quan hệ) và **ERD Level 2** (logical/physical, đầy đủ cột của từng bảng). Nguồn: `database/init.sql`.

Database: `FastGuyDB` — SQL Server. Tổng số bảng: **27** (26 bảng thực thể + `ShippingConfig` cấu hình).

---

## 1. ERD Level 1 — Quan hệ thực thể

### 1.1. Danh sách thực thể theo nhóm

| Nhóm | Thực thể | Mô tả |
| ---- | -------- | ----- |
| Người dùng | `Users` | Tài khoản, vai trò, trạng thái, điểm thành viên |
| Người dùng | `PasswordResetToken` | Mã đặt lại mật khẩu |
| Người dùng | `Address` | Địa chỉ giao hàng, địa chỉ mặc định |
| Sản phẩm | `Category` | Danh mục sản phẩm |
| Sản phẩm | `Product` | Thông tin món ăn |
| Sản phẩm | `ProductVariant` | Biến thể, giá và tồn kho |
| Sản phẩm | `ProductModifierGroup` | Nhóm tùy chọn của sản phẩm |
| Sản phẩm | `ProductModifierOption` | Các lựa chọn và giá bổ sung |
| Sản phẩm | `ProductCombo` | Thông tin combo |
| Sản phẩm | `ProductComboItem` | Các sản phẩm thuộc combo |
| Giỏ hàng & đơn hàng | `Cart` | Giỏ hàng của User hoặc Guest |
| Giỏ hàng & đơn hàng | `CartItem` | Các sản phẩm trong giỏ hàng |
| Giỏ hàng & đơn hàng | `Orders` | Thông tin đơn, thanh toán, giao hàng, trạng thái |
| Giỏ hàng & đơn hàng | `OrderItem` | Chi tiết sản phẩm tại thời điểm đặt hàng |
| Giỏ hàng & đơn hàng | `OrderStatusHistory` | Lịch sử thay đổi trạng thái đơn |
| Thanh toán & tồn kho | `PaymentAttempt` | Các lần tạo và xử lý thanh toán PayOS |
| Thanh toán & tồn kho | `InventoryReservation` | Số lượng sản phẩm được giữ khi tạo đơn |
| Thanh toán & tồn kho | `InventoryTransaction` | Lịch sử trữ, hoàn và điều chỉnh tồn kho |
| Khuyến mãi | `Coupon` | Mã giảm giá và điều kiện sử dụng |
| Khuyến mãi | `CouponRedemption` | Thông tin nhận và sử dụng coupon |
| Khuyến mãi | `Banner` | Banner và chương trình khuyến mãi |
| Chăm sóc khách hàng | `Review` | Đánh giá sau khi giao hàng |
| Chăm sóc khách hàng | `SupportTicket` | Yêu cầu hỗ trợ của khách hàng |
| Chăm sóc khách hàng | `Notification` | Thông báo và trạng thái đã đọc |
| Chăm sóc khách hàng | `LoyaltyTransaction` | Lịch sử cộng và trừ điểm thành viên |
| Ca làm & cấu hình | `WorkShift` | Ca làm, check-in, check-out của Staff/Shipper |
| Ca làm & cấu hình | `ShippingConfig` | Cấu hình và thông tin tính phí giao hàng |

### 1.2. Bảng quan hệ (Relationship)

Bảng được viết theo cách trực quan kiểu **"A has B"** (A sở hữu B):

- `has` = 1 — 1: một A có đúng một B.
- `has many` = 1 — N: một A có nhiều B.
- `belongs to` = N — 1: nhiều A thuộc về một B.

| Quan hệ (trực quan) | Cardinality | Giải thích |
| ------------------- | ----------- | ---------- |
| `Users` **has many** `PasswordResetToken` | 1 — N | Một user có nhiều token đặt lại mật khẩu |
| `Users` **has many** `Address` | 1 — N | Một user có nhiều địa chỉ giao hàng |
| `Users` **has many** `Cart` | 1 — N | Một user có nhiều giỏ hàng (theo lịch sử) |
| `Users` **has many** `Orders` (user_id) | 1 — N | Một khách đặt nhiều đơn hàng |
| `Users` **has many** `Orders` (staff_id) | 1 — N | Một Staff xử lý nhiều đơn hàng |
| `Users` **has many** `Orders` (shipper_id) | 1 — N | Một Shipper giao nhiều đơn hàng |
| `Users` **has many** `LoyaltyTransaction` | 1 — N | Một user có nhiều giao dịch điểm |
| `Users` **has many** `WorkShift` | 1 — N | Một Staff/Shipper có nhiều ca làm |
| `Users` **has many** `CouponRedemption` | 1 — N | Một user nhận nhiều coupon |
| `Users` **has many** `Review` | 1 — N | Một user gửi nhiều đánh giá |
| `Users` **has many** `SupportTicket` (user_id) | 1 — N | Một user tạo nhiều ticket hỗ trợ |
| `Users` **has many** `SupportTicket` (staff_id) | 1 — N | Một Staff phụ trách nhiều ticket |
| `Users` **has many** `Notification` (user_id) | 1 — N | Một user nhận nhiều thông báo cá nhân |
| `Users` **has many** `OrderStatusHistory` (actor_user_id) | 1 — N | Một user thao tác nhiều sự kiện trạng thái |
| `Category` **has many** `Product` | 1 — N | Một danh mục chứa nhiều sản phẩm |
| `Product` **has many** `ProductVariant` | 1 — N | Một sản phẩm có nhiều biến thể |
| `Product` **has many** `ProductModifierGroup` | 1 — N | Một sản phẩm có nhiều nhóm tùy chọn |
| `Product` **has one** `ProductCombo` | 1 — 1 | Một sản phẩm có tối đa một combo (unique product_id) |
| `ProductCombo` **has many** `ProductComboItem` | 1 — N | Một combo gồm nhiều món thành phần |
| `Product` **has many** `ProductComboItem` | 1 — N | Một sản phẩm xuất hiện trong nhiều combo |
| `ProductVariant` **has many** `ProductComboItem` | 1 — N | Một biến thể xuất hiện trong nhiều combo |
| `ProductModifierGroup` **has many** `ProductModifierOption` | 1 — N | Một nhóm tùy chọn có nhiều lựa chọn |
| `Cart` **has many** `CartItem` | 1 — N | Một giỏ hàng gồm nhiều món |
| `Product` **has many** `CartItem` | 1 — N | Một sản phẩm xuất hiện trong nhiều giỏ |
| `ProductVariant` **has many** `CartItem` | 1 — N | Một biến thể xuất hiện trong nhiều giỏ |
| `Orders` **has many** `OrderItem` | 1 — N | Một đơn gồm nhiều chi tiết món |
| `Product` **has many** `OrderItem` | 1 — N | Một sản phẩm xuất hiện trong nhiều đơn (snapshot, nullable) |
| `ProductVariant` **has many** `OrderItem` | 1 — N | Một biến thể xuất hiện trong nhiều đơn (snapshot, nullable) |
| `Orders` **has many** `OrderStatusHistory` | 1 — N | Một đơn có nhiều sự kiện lịch sử trạng thái |
| `Orders` **has one** `PaymentAttempt` | 1 — 1 | Một đơn có một lần thanh toán (unique order_id) |
| `Orders` **has many** `InventoryReservation` | 1 — N | Một đơn giữ hàng ở nhiều biến thể |
| `Orders` **has many** `InventoryTransaction` | 1 — N | Một đơn tạo nhiều giao dịch tồn kho |
| `Orders` **has many** `LoyaltyTransaction` | 1 — N | Một đơn phát sinh nhiều giao dịch điểm |
| `Orders` **has many** `CouponRedemption` | 1 — N | Một đơn có thể dùng một coupon (nullable) |
| `Orders` **has many** `Review` | 1 — N | Một đơn có nhiều đánh giá (mỗi user một đánh giá) |
| `Orders` **has many** `SupportTicket` | 1 — N | Một đơn có thể có nhiều ticket (nullable) |
| `ProductVariant` **has many** `InventoryReservation` | 1 — N | Một biến thể được giữ trong nhiều đơn |
| `ProductVariant` **has many** `InventoryTransaction` | 1 — N | Một biến thể có nhiều giao dịch tồn kho |
| `Coupon` **has many** `CouponRedemption` | 1 — N | Một coupon được nhiều user nhận/dùng |
| `Notification` **belongs to** `Users` | N — 1 | Thông báo nhắm một user hoặc một role (target tuỳ chọn) |

---

## 2. ERD Level 2 — Chi tiết từng bảng

### 2.1. `dbo.Category`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| category_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| name | nvarchar(255) | NOT NULL | |
| description | nvarchar(500) | NULL | |
| sort_order | int | NOT NULL | DEFAULT 0 |
| status | varchar(20) | NOT NULL | DEFAULT 'ACTIVE'; CHECK IN ('ACTIVE','INACTIVE') |

### 2.2. `dbo.Product`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| product_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| category_id | int | NOT NULL | **FK** → Category(category_id) |
| name | nvarchar(255) | NOT NULL | |
| description | nvarchar(1000) | NULL | |
| base_price | decimal(18,2) | NOT NULL | CHECK base_price >= 0 |
| image_url | varchar(500) | NULL | |
| gallery_images | nvarchar(max) | NOT NULL | DEFAULT N'[]' |
| status | varchar(20) | NOT NULL | DEFAULT 'AVAILABLE'; CHECK IN ('AVAILABLE','UNAVAILABLE','INACTIVE') |
| available_from | time(0) | NULL | |
| available_to | time(0) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_Product_Category(category_id).

### 2.3. `dbo.ProductVariant`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| variant_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| product_id | int | NOT NULL | **FK** → Product(product_id) |
| variant_name | nvarchar(255) | NOT NULL | |
| price | decimal(18,2) | NOT NULL | CHECK price >= 0 |
| original_price | decimal(18,2) | NULL | CHECK original_price >= 0 |
| sku | varchar(100) | NULL | UNIQUE filtered (sku) |
| quantity_available | int | NULL | CHECK NULL hoặc >= 0 (NULL = không giới hạn) |
| weight | decimal(10,2) | NOT NULL | DEFAULT 500; CHECK > 0 |
| length | decimal(10,2) | NOT NULL | DEFAULT 20; CHECK > 0 |
| width | decimal(10,2) | NOT NULL | DEFAULT 20; CHECK > 0 |
| height | decimal(10,2) | NOT NULL | DEFAULT 10; CHECK > 0 |
| is_default | bit | NOT NULL | DEFAULT 0; UNIQUE filtered (product_id) khi =1 |
| status | varchar(20) | NOT NULL | DEFAULT 'AVAILABLE'; CHECK IN ('AVAILABLE','UNAVAILABLE','INACTIVE') |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_ProductVariant_Product(product_id); UX_ProductVariant_Sku (filtered); UX_ProductVariant_Default (filtered).

### 2.4. `dbo.ProductModifierGroup`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| modifier_group_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| product_id | int | NOT NULL | **FK** → Product(product_id) |
| name | nvarchar(255) | NOT NULL | |
| min_selections | int | NOT NULL | DEFAULT 0; CHECK >= 0 |
| max_selections | int | NOT NULL | DEFAULT 1; CHECK max >= min |
| is_active | bit | NOT NULL | DEFAULT 1 |
| sort_order | int | NOT NULL | DEFAULT 0 |

**Index:** IX_ProductModifierGroup_Product(product_id).

### 2.5. `dbo.ProductModifierOption`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| modifier_option_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| modifier_group_id | int | NOT NULL | **FK** → ProductModifierGroup(modifier_group_id) |
| name | nvarchar(255) | NOT NULL | |
| price | decimal(18,2) | NOT NULL | DEFAULT 0; CHECK >= 0 |
| is_active | bit | NOT NULL | DEFAULT 1 |
| sort_order | int | NOT NULL | DEFAULT 0 |

**Index:** IX_ProductModifierOption_Group(modifier_group_id).

### 2.6. `dbo.ProductCombo`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| combo_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| product_id | int | NOT NULL | **FK** → Product(product_id); UNIQUE |
| is_active | bit | NOT NULL | DEFAULT 1 |

### 2.7. `dbo.ProductComboItem`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| combo_item_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| combo_id | int | NOT NULL | **FK** → ProductCombo(combo_id) |
| product_id | int | NOT NULL | **FK** → Product(product_id) |
| variant_id | int | NOT NULL | **FK** → ProductVariant(variant_id) |
| quantity | int | NOT NULL | DEFAULT 1; CHECK > 0 |
| sort_order | int | NOT NULL | DEFAULT 0 |

**Index:** IX_ProductComboItem_Combo, IX_ProductComboItem_Product, IX_ProductComboItem_Variant.

### 2.8. `dbo.ShippingConfig`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| config_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| config_key | varchar(100) | NOT NULL | UNIQUE |
| config_value | varchar(500) | NOT NULL | |

### 2.9. `dbo.Users`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| user_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| role_name | varchar(20) | NOT NULL | DEFAULT 'USER'; CHECK IN ('ADMIN','STAFF','SHIPPER','USER') |
| email | varchar(255) | NULL | UNIQUE filtered (email) |
| phone | varchar(20) | NOT NULL | UNIQUE |
| password_hash | varchar(255) | NOT NULL | |
| full_name | nvarchar(255) | NOT NULL | |
| avatar_url | varchar(500) | NULL | |
| status | varchar(20) | NOT NULL | DEFAULT 'ACTIVE'; CHECK IN ('ACTIVE','INACTIVE','BLOCKED') |
| loyalty_points | int | NOT NULL | DEFAULT 0; CHECK >= 0 |
| favorite_ids_json | nvarchar(max) | NOT NULL | DEFAULT N'[]'; ISJSON |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** UX_Users_Email (filtered), UX_Users_Phone.

### 2.10. `dbo.PasswordResetToken`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| reset_token_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| token_hash | varchar(64) | NOT NULL | UNIQUE |
| expires_at | datetime2(0) | NOT NULL | |
| used_at | datetime2(0) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_PasswordResetToken_User(user_id).

### 2.11. `dbo.Banner`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| banner_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| title | nvarchar(255) | NULL | |
| subtitle | nvarchar(500) | NULL | |
| image_url | varchar(500) | NOT NULL | |
| link | varchar(500) | NULL | |
| sort_order | int | NOT NULL | DEFAULT 0 |
| is_active | bit | NOT NULL | DEFAULT 1 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

### 2.12. `dbo.Address`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| address_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| recipient_name | nvarchar(255) | NOT NULL | |
| phone | varchar(20) | NOT NULL | |
| street | nvarchar(255) | NOT NULL | |
| ward_name | nvarchar(100) | NULL | |
| district_name | nvarchar(100) | NULL | |
| province_name | nvarchar(100) | NULL | |
| ghn_province_id | int | NULL | |
| ghn_district_id | int | NULL | |
| ghn_ward_code | varchar(50) | NULL | |
| city | nvarchar(100) | NOT NULL | DEFAULT N'TP. Ho Chi Minh' |
| is_default | bit | NOT NULL | DEFAULT 0; UNIQUE filtered (user_id) khi =1 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_Address_User(user_id); UX_Address_Default (filtered).

### 2.13. `dbo.Cart`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| cart_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NULL | **FK** → Users(user_id); UNIQUE filtered |
| session_id | varchar(128) | NULL | UNIQUE filtered |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** CK — `user_id IS NOT NULL OR session_id IS NOT NULL`.

**Index:** UX_Cart_User (filtered), UX_Cart_Session (filtered).

### 2.14. `dbo.CartItem`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| cart_item_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| cart_id | int | NOT NULL | **FK** → Cart(cart_id) |
| product_id | int | NOT NULL | **FK** → Product(product_id) |
| variant_id | int | NOT NULL | **FK** → ProductVariant(variant_id) |
| quantity | int | NOT NULL | CHECK > 0 |
| unit_price | decimal(18,2) | NOT NULL | CHECK >= 0 |
| modifiers_json | nvarchar(max) | NOT NULL | DEFAULT N'[]'; ISJSON |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_CartItem_Cart, IX_CartItem_Product, IX_CartItem_Variant.

### 2.15. `dbo.Coupon`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| coupon_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| code | varchar(50) | NOT NULL | UNIQUE |
| type | varchar(20) | NOT NULL | CHECK IN ('PERCENT','FIXED','FREE_SHIPPING') |
| value | decimal(18,2) | NOT NULL | CHECK >= 0 |
| min_order | decimal(18,2) | NOT NULL | DEFAULT 0; CHECK >= 0 |
| max_discount | decimal(18,2) | NULL | CHECK NULL hoặc >= 0 |
| max_uses | int | NOT NULL | DEFAULT 0; CHECK >= 0 |
| used_count | int | NOT NULL | DEFAULT 0; CHECK >= 0 |
| expires_at | datetime2(0) | NULL | |
| is_active | bit | NOT NULL | DEFAULT 1 |
| is_public | bit | NOT NULL | DEFAULT 1 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** CK — `max_uses = 0 OR used_count <= max_uses`.

### 2.16. `dbo.Orders`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| order_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_code | varchar(50) | NOT NULL | UNIQUE |
| idempotency_key | varchar(100) | NULL | UNIQUE filtered |
| request_hash | varchar(64) | NULL | |
| idempotency_owner | varchar(100) | NULL | |
| user_id | int | NULL | **FK** → Users(user_id) |
| customer_name | nvarchar(255) | NOT NULL | |
| customer_phone | varchar(20) | NOT NULL | |
| customer_address | nvarchar(500) | NOT NULL | |
| to_province_name | nvarchar(100) | NULL | |
| to_district_name | nvarchar(100) | NULL | |
| to_ward_name | nvarchar(100) | NULL | |
| ghn_province_id | int | NULL | |
| ghn_district_id | int | NULL | |
| ghn_ward_code | varchar(50) | NULL | |
| total_amount | decimal(18,2) | NOT NULL | CHECK >= 0 |
| shipping_fee | decimal(18,2) | NOT NULL | DEFAULT 0; CHECK >= 0 |
| service_fee | decimal(18,2) | NOT NULL | DEFAULT 0; CHECK >= 0 |
| final_amount | decimal(18,2) | NOT NULL | CHECK >= 0 |
| cod_collected_amount | decimal(18,2) | NULL | CHECK NULL hoặc >= 0 |
| cod_collected_at | datetime2(0) | NULL | |
| shipping_provider | varchar(30) | NOT NULL | DEFAULT 'GHN' |
| shipping_service_id | int | NULL | |
| shipping_service_type_id | int | NULL | |
| expected_delivery_time | datetime2(0) | NULL | |
| ghn_order_code | varchar(50) | NULL | |
| ghn_tracking_url | varchar(500) | NULL | |
| ghn_status | varchar(30) | NULL | |
| from_district_id | int | NULL | |
| from_ward_code | varchar(50) | NULL | |
| payment_method | varchar(50) | NOT NULL | CHECK IN ('COD','BANK_TRANSFER') |
| payment_status | varchar(20) | NOT NULL | DEFAULT 'UNPAID'; CHECK IN ('UNPAID','PAID','FAILED','REFUNDED') |
| payos_payment_link_id | varchar(100) | NULL | |
| payos_checkout_url | varchar(500) | NULL | |
| guest_return_proof_hash | varchar(64) | NULL | CHECK NULL hoặc 64 ký tự hex |
| order_status | varchar(30) | NOT NULL | DEFAULT 'PENDING'; CHECK IN ('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERED','CANCELLED') |
| staff_id | int | NULL | **FK** → Users(user_id) |
| shipper_id | int | NULL | **FK** → Users(user_id) |
| assigned_at | datetime2(0) | NULL | |
| confirmed_at | datetime2(0) | NULL | |
| ready_at | datetime2(0) | NULL | |
| picked_up_at | datetime2(0) | NULL | |
| paid_at | datetime2(0) | NULL | |
| delivered_at | datetime2(0) | NULL | |
| cancelled_at | datetime2(0) | NULL | |
| failure_reason | nvarchar(500) | NULL | |
| cancelled_by | varchar(20) | NULL | CHECK IN ('CUSTOMER','USER','STAFF','ADMIN','SYSTEM') |
| refund_status | varchar(20) | NULL | CHECK IN ('PENDING','REFUNDED','REJECTED') |
| refund_amount | decimal(18,2) | NULL | CHECK NULL hoặc >= 0 |
| refunded_at | datetime2(0) | NULL | |
| refund_note | nvarchar(500) | NULL | |
| internal_note | nvarchar(1000) | NULL | |
| coupon_code | varchar(50) | NULL | |
| discount_amount | decimal(18,2) | NOT NULL | DEFAULT 0; CHECK >= 0 |
| delivery_note | nvarchar(500) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_Orders_User(user_id); IX_Orders_Staff_Status(staff_id, order_status); IX_Orders_Shipper_Status(shipper_id, order_status); IX_Orders_Status_Created(order_status, created_at); UX_Orders_Idempotency (filtered).

### 2.17. `dbo.PaymentAttempt`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| payment_attempt_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_id | int | NOT NULL | **FK** → Orders(order_id); UNIQUE |
| provider | varchar(20) | NOT NULL | |
| provider_reference | varchar(100) | NULL | |
| checkout_url | varchar(500) | NULL | |
| amount | decimal(18,2) | NOT NULL | CHECK >= 0 |
| status | varchar(20) | NOT NULL | CHECK IN ('CREATING','READY','PENDING','PAID','FAILED','EXPIRED','CANCELLED') |
| lease_token | varchar(36) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

### 2.18. `dbo.InventoryReservation`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| reservation_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| variant_id | int | NOT NULL | **FK** → ProductVariant(variant_id) |
| quantity | int | NOT NULL | CHECK > 0 |
| status | varchar(20) | NOT NULL | CHECK IN ('RESERVED','CONSUMED','RELEASED','WASTED') |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** UNIQUE (order_id, variant_id).

**Index:** IX_InventoryReservation_Variant(variant_id).

### 2.19. `dbo.InventoryTransaction`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| inventory_transaction_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| variant_id | int | NOT NULL | **FK** → ProductVariant(variant_id) |
| transaction_type | varchar(20) | NOT NULL | CHECK IN ('RESERVE','RELEASE','CONSUME','WASTE','RETURN','ADJUSTMENT') |
| quantity | int | NOT NULL | CHECK > 0 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_InventoryTransaction_Order(order_id); IX_InventoryTransaction_Variant(variant_id).

### 2.20. `dbo.LoyaltyTransaction`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| loyalty_transaction_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| transaction_type | varchar(20) | NOT NULL | CHECK IN ('EARN','REDEEM','REVERSE','REFUND','ADJUSTMENT') |
| points | int | NOT NULL | CHECK <> 0 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** UNIQUE (order_id, transaction_type).

**Index:** IX_LoyaltyTransaction_User_Created(user_id, created_at).

### 2.21. `dbo.WorkShift`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| shift_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| shift_date | date | NOT NULL | |
| start_time | time(0) | NOT NULL | CHECK start < end |
| end_time | time(0) | NOT NULL | CHECK start < end |
| check_in_at | datetime2(0) | NULL | |
| check_out_at | datetime2(0) | NULL | CHECK: NULL hoặc (check_in_at IS NOT NULL AND check_out_at >= check_in_at) |
| status | varchar(20) | NOT NULL | DEFAULT 'SCHEDULED'; CHECK IN ('SCHEDULED','CHECKED_IN','CHECKED_OUT','ABSENT','CANCELLED') |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_WorkShift_User_Date(user_id, shift_date); IX_WorkShift_Date_Status(shift_date, status).

### 2.22. `dbo.CouponRedemption`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| redemption_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| coupon_id | int | NOT NULL | **FK** → Coupon(coupon_id) |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| order_id | int | NULL | **FK** → Orders(order_id) |
| claimed_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| used_at | datetime2(0) | NULL | |
| discount_amount | decimal(18,2) | NULL | CHECK NULL hoặc >= 0 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** UNIQUE (user_id, coupon_id).

**Index:** IX_CouponRedemption_Coupon(coupon_id); UX_CouponRedemption_Order (filtered order_id).

### 2.23. `dbo.OrderItem`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| order_item_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| product_id | int | NULL | **FK** → Product(product_id) |
| variant_id | int | NULL | **FK** → ProductVariant(variant_id) |
| product_name | nvarchar(255) | NOT NULL | snapshot |
| variant_name | nvarchar(255) | NULL | snapshot |
| quantity | int | NOT NULL | CHECK > 0 |
| unit_price | decimal(18,2) | NOT NULL | CHECK >= 0 |
| total_price | decimal(18,2) | NOT NULL | CHECK >= 0 |
| modifiers_json | nvarchar(max) | NOT NULL | DEFAULT N'[]'; ISJSON |

**Index:** IX_OrderItem_Order(order_id); IX_OrderItem_Product(product_id); IX_OrderItem_Variant(variant_id).

### 2.24. `dbo.Review`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| review_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NOT NULL | **FK** → Users(user_id) |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| rating | int | NOT NULL | CHECK BETWEEN 1 AND 5 |
| comment | nvarchar(1000) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** UNIQUE (user_id, order_id).

**Index:** IX_Review_Order(order_id).

### 2.25. `dbo.SupportTicket`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| ticket_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NULL | **FK** → Users(user_id) |
| order_id | int | NULL | **FK** → Orders(order_id) |
| subject | nvarchar(255) | NOT NULL | |
| category | varchar(30) | NOT NULL | CHECK IN ('MISSING_ITEM','COLD_FOOD','WRONG_ITEM','LATE_DELIVERY','OTHER') |
| description | nvarchar(2000) | NOT NULL | |
| status | varchar(20) | NOT NULL | DEFAULT 'OPEN'; CHECK IN ('OPEN','PROCESSING','RESOLVED') |
| staff_id | int | NULL | **FK** → Users(user_id) |
| resolution | nvarchar(2000) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| resolved_at | datetime2(0) | NULL | |

**Index:** IX_SupportTicket_User_Created(user_id, created_at); IX_SupportTicket_Order(order_id); IX_SupportTicket_Staff_Status(staff_id, status).

### 2.26. `dbo.Notification`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| notification_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| user_id | int | NULL | **FK** → Users(user_id) |
| role_name | varchar(50) | NULL | CHECK IN ('ADMIN','STAFF','SHIPPER','USER') |
| title | nvarchar(255) | NOT NULL | |
| message | nvarchar(1000) | NULL | |
| type | varchar(50) | NULL | |
| target_url | varchar(500) | NULL | |
| is_read | bit | NOT NULL | DEFAULT 0 |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |
| updated_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Ràng buộc:** CK — `user_id IS NOT NULL OR role_name IS NOT NULL`.

**Index:** IX_Notification_User_Read(user_id, is_read, created_at); IX_Notification_Role_Read(role_name, is_read, created_at).

### 2.27. `dbo.OrderStatusHistory`

| Cột | Kiểu | Null | Ràng buộc |
| --- | --- | --- | --- |
| history_id | int IDENTITY(1,1) | NOT NULL | **PK** |
| order_id | int | NOT NULL | **FK** → Orders(order_id) |
| actor_user_id | int | NULL | **FK** → Users(user_id) |
| actor_role | varchar(50) | NULL | CHECK IN ('ADMIN','STAFF','SHIPPER','USER','GUEST','SYSTEM','PAYOS') |
| from_status | varchar(30) | NULL | CHECK NULL hoặc một trong 8 trạng thái đơn |
| to_status | varchar(30) | NOT NULL | CHECK một trong 8 trạng thái đơn |
| note | nvarchar(500) | NULL | |
| created_at | datetime2(0) | NOT NULL | DEFAULT GETDATE() |

**Index:** IX_OrderStatusHistory_Order_Created(order_id, created_at); IX_OrderStatusHistory_Actor(actor_user_id).

---

## 3. Tổng hợp ràng buộc dữ liệu

### 3.1. Danh sách enum

| Bảng / cột | Giá trị cho phép |
| ---------- | ---------------- |
| Category.status | ACTIVE, INACTIVE |
| Product.status | AVAILABLE, UNAVAILABLE, INACTIVE |
| ProductVariant.status | AVAILABLE, UNAVAILABLE, INACTIVE |
| Users.role_name | ADMIN, STAFF, SHIPPER, USER |
| Users.status | ACTIVE, INACTIVE, BLOCKED |
| Coupon.type | PERCENT, FIXED, FREE_SHIPPING |
| Orders.payment_method | COD, BANK_TRANSFER |
| Orders.payment_status | UNPAID, PAID, FAILED, REFUNDED |
| Orders.order_status | PENDING, CONFIRMED, PREPARING, READY, ASSIGNED, PICKED_UP, DELIVERED, CANCELLED |
| Orders.cancelled_by | CUSTOMER, USER, STAFF, ADMIN, SYSTEM |
| Orders.refund_status | PENDING, REFUNDED, REJECTED |
| PaymentAttempt.status | CREATING, READY, PENDING, PAID, FAILED, EXPIRED, CANCELLED |
| InventoryReservation.status | RESERVED, CONSUMED, RELEASED, WASTED |
| InventoryTransaction.transaction_type | RESERVE, RELEASE, CONSUME, WASTE, RETURN, ADJUSTMENT |
| LoyaltyTransaction.transaction_type | EARN, REDEEM, REVERSE, REFUND, ADJUSTMENT |
| WorkShift.status | SCHEDULED, CHECKED_IN, CHECKED_OUT, ABSENT, CANCELLED |
| SupportTicket.category | MISSING_ITEM, COLD_FOOD, WRONG_ITEM, LATE_DELIVERY, OTHER |
| SupportTicket.status | OPEN, PROCESSING, RESOLVED |
| Notification.role_name | ADMIN, STAFF, SHIPPER, USER |
| OrderStatusHistory.actor_role | ADMIN, STAFF, SHIPPER, USER, GUEST, SYSTEM, PAYOS |

### 3.2. Quy tắc quan trọng khác

| Quy tắc | Mô tả |
| ------- | ----- |
| Giỏ hàng thuộc chủ | `Cart.user_id` hoặc `Cart.session_id` bắt buộc có một |
| Thông báo có đối tượng | `Notification.user_id` hoặc `role_name` bắt buộc có một |
| Địa chỉ mặc định duy nhất | UNIQUE filtered `Address(user_id)` khi `is_default = 1` |
| Variant default duy nhất | UNIQUE filtered `ProductVariant(product_id)` khi `is_default = 1` |
| SKU duy nhất | UNIQUE filtered `ProductVariant(sku)` |
| Một đơn một payment attempt | UNIQUE `PaymentAttempt(order_id)` |
| Giữ hàng theo đơn+biến thể | UNIQUE `InventoryReservation(order_id, variant_id)` |
| Điểm một đơn một loại | UNIQUE `LoyaltyTransaction(order_id, transaction_type)` |
| Coupon một user một lần nhận | UNIQUE `CouponRedemption(user_id, coupon_id)` |
| Đánh giá một user một đơn | UNIQUE `Review(user_id, order_id)` |
| Idempotency đơn | UNIQUE filtered `Orders(idempotency_key)` |
| Combo mỗi sản phẩm | UNIQUE `ProductCombo(product_id)` |
| Lưu trữ JSON | `gallery_images`, `favorite_ids_json`, `modifiers_json` phải hợp lệ JSON |

### 3.3. Quan hệ FK trỏ đến Users (vai trò nghiệp vụ)

| Cột FK | Bảng | Vai trò kỳ vọng |
| ------ | ---- | --------------- |
| Orders.staff_id | Users | STAFF hoặc ADMIN (guard trigger) |
| Orders.shipper_id | Users | SHIPPER (guard trigger) |
| WorkShift.user_id | Users | STAFF hoặc SHIPPER (guard trigger) |
| SupportTicket.staff_id | Users | STAFF hoặc ADMIN (guard trigger) |
