# FastGuyDB Update Plan

## 1. Quyết định thiết kế

Các quyết định đã chốt:

1. `Product.price` sẽ đổi vai trò thành `Product.base_price` để hiển thị giá từ.
2. `ProductOption` sẽ được thay bằng `ProductVariant`, migrate dữ liệu xong thì drop.
3. `DeliveryZone` vẫn giữ làm fallback khi GHN lỗi hoặc môi trường dev chưa có token.
4. `Ingredient` và `ProductIngredient` bỏ hẳn khỏi database vì không triển khai quản lý kho thật.

---

## 2. Mục tiêu cập nhật database

Thiết kế lại database theo hướng:

```text
Product = món cha / món chung
ProductVariant = phiên bản khách thật sự mua
GHN = nguồn chính tính phí vận chuyển
DeliveryZone = fallback tạm thời
Ingredient/ProductIngredient = bỏ hẳn
```

Ví dụ:

```text
Product: Pizza Hải Sản
- ProductVariant: Size M - 89000
- ProductVariant: Size L - 119000

Product: Gà Rán
- ProductVariant: 1 miếng - 49000
- ProductVariant: Combo 3 miếng - 69000
- ProductVariant: Combo 6 miếng - 94000
```

---

## 3. Phân tích schema hiện tại

### 3.1 User / Role / Address

Hiện tại:

```text
Role 1 - n Users
Users 1 - n Address
Users 1 - n Orders
```

Đánh giá:

- `Role`, `Users` ổn.
- `Orders.user_id` nullable là hợp lý vì có thể có guest order.
- `Address.zone_id` hiện bắt buộc, không phù hợp khi chuyển sang GHN.

Cần cập nhật:

- Thêm mã địa chỉ GHN vào `Address`.
- Chuyển `zone_id` thành nullable.
- Lưu snapshot tên tỉnh/huyện/xã.

---

### 3.2 Product hiện tại

Hiện tại:

```text
Category
Product
ProductOption
Ingredient
ProductIngredient
```

Quan hệ hiện tại:

```text
Category 1 - n Product
Product 1 - n ProductOption
Product n - n Ingredient qua ProductIngredient
```

Vấn đề:

- `Product.price` đang là giá bán chính.
- `ProductOption` chỉ là lựa chọn cộng tiền, chưa phải biến thể bán hàng thật.
- `CartItem` và `OrderItem` không lưu `option_id`.
- `option_data` chỉ là JSON/text nên không có ràng buộc DB.
- `Ingredient/ProductIngredient` không được dùng để trừ kho thật.

Thiết kế mới:

```text
Category 1 - n Product
Product 1 - n ProductVariant
```

---

### 3.3 Cart hiện tại

Hiện tại:

```text
CartItem.product_id
CartItem.option_data
CartItem.unit_price
```

Vấn đề:

- Không biết chính xác khách chọn option nào bằng FK.
- Không kiểm tra được option có thuộc product không.
- Không trừ được tồn biến thể.
- Không báo cáo được biến thể nào bán chạy.

Thiết kế mới:

```text
CartItem.product_id
CartItem.variant_id
CartItem.unit_price
```

---

### 3.4 Order hiện tại

Hiện tại:

```text
OrderItem.product_id
OrderItem.product_name
OrderItem.option_data
OrderItem.unit_price
OrderItem.total_price
```

Điểm tốt:

- Có snapshot `product_name`, `unit_price`, `total_price`.

Thiếu:

- `variant_id`
- `variant_name`
- GHN shipping fields
- shipping provider/service info

Thiết kế mới:

```text
OrderItem.product_id
OrderItem.variant_id
OrderItem.product_name
OrderItem.variant_name
OrderItem.unit_price
OrderItem.total_price
```

---

### 3.5 Delivery hiện tại

Hiện tại:

```text
DeliveryZone(zone_id, district_name, shipping_fee)
Address.zone_id
Orders.zone_id
```

Vấn đề:

- Chỉ tính phí theo quận.
- Không đúng mô hình GHN.
- GHN cần mã tỉnh/huyện/xã, không dùng text quận đơn giản.

Thiết kế mới:

- GHN là nguồn chính tính phí.
- `DeliveryZone` giữ fallback.
- `Address.zone_id` và `Orders.zone_id` chuyển nullable.

---

## 4. ERD đề xuất mới

```text
Role
 └── Users
      ├── Address
      ├── Cart
      │    └── CartItem
      │         ├── Product
      │         └── ProductVariant
      ├── Orders
      │    ├── OrderItem
      │    │    ├── Product
      │    │    └── ProductVariant
      │    ├── Payment
      │    └── Review
      └── Schedule

Category
 └── Product
      └── ProductVariant

DeliveryZone
 ├── Address nullable fallback
 └── Orders nullable fallback

WorkShift
 └── Schedule
```

---

## 5. Schema đề xuất mới

### 5.1 Product

```sql
create table Product
(
    product_id     int identity primary key,
    category_id    int not null references Category,
    name           nvarchar(255) not null,
    description    nvarchar(1000),
    base_price     decimal(10, 2),
    image_url      varchar(500),
    gallery_images nvarchar(max),
    status         varchar(20) default 'AVAILABLE',
    created_at     datetime2 default getdate(),
    updated_at     datetime2
);
```

Ý nghĩa:

- `Product` là món cha.
- `base_price` chỉ dùng để hiển thị giá từ.
- Checkout không lấy giá từ `Product.base_price`.
- Checkout lấy giá từ `ProductVariant.price`.

---

### 5.2 ProductVariant

```sql
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
```

Quy tắc:

- Mỗi `Product` nên có ít nhất một `ProductVariant` mặc định.
- Khách hàng mua `ProductVariant`, không mua trực tiếp `Product`.
- `quantity_available` có thể nullable nếu không kiểm soát tồn.

---

### 5.3 CartItem

```sql
create table CartItem
(
    cart_item_id int identity primary key,
    cart_id      int not null references Cart,
    product_id   int not null references Product,
    variant_id   int not null references ProductVariant,
    quantity     int not null,
    unit_price   decimal(10, 2) not null,
    created_at   datetime2 default getdate()
);
```

Quy tắc:

- Add cart nhận `productId`, `variantId`, `quantity`.
- Backend validate variant thuộc product.
- `unit_price` là snapshot tại thời điểm thêm vào cart.

---

### 5.4 OrderItem

```sql
create table OrderItem
(
    order_item_id int identity primary key,
    order_id      int not null references Orders,
    product_id    int references Product,
    variant_id    int references ProductVariant,
    product_name  nvarchar(255) not null,
    variant_name  nvarchar(255),
    quantity      int not null,
    unit_price    decimal(10, 2) not null,
    total_price   decimal(10, 2) not null
);
```

Lý do lưu snapshot:

- Nếu admin đổi tên sản phẩm, đơn cũ vẫn đúng.
- Nếu admin đổi giá variant, đơn cũ vẫn giữ giá lúc mua.
- Nếu variant bị xóa/ẩn, đơn cũ vẫn xem được.

---

### 5.5 Address cho GHN

```sql
create table Address
(
    address_id      int identity primary key,
    user_id         int not null references Users,
    recipient_name  nvarchar(255) not null,
    phone           varchar(20) not null,
    street          nvarchar(255) not null,
    ward_name       nvarchar(100),
    district_name   nvarchar(100),
    province_name   nvarchar(100),
    ghn_province_id int,
    ghn_district_id int,
    ghn_ward_code   varchar(50),
    zone_id         int null references DeliveryZone,
    city            nvarchar(100) default N'TP. Hồ Chí Minh',
    is_default      bit default 0,
    created_at      datetime2 default getdate()
);
```

---

### 5.6 Orders cho GHN

```sql
create table Orders
(
    order_id                 int identity primary key,
    order_code               varchar(50) not null unique,
    user_id                  int references Users,
    customer_name            nvarchar(255) not null,
    customer_phone           varchar(20) not null,
    customer_address         nvarchar(500) not null,
    zone_id                  int null references DeliveryZone,
    to_province_name         nvarchar(100),
    to_district_name         nvarchar(100),
    to_ward_name             nvarchar(100),
    ghn_province_id          int,
    ghn_district_id          int,
    ghn_ward_code            varchar(50),
    total_amount             decimal(10, 2) not null,
    shipping_fee             decimal(10, 2) default 0,
    final_amount             decimal(10, 2) not null,
    shipping_provider        varchar(30) default 'GHN',
    shipping_service_id      int,
    shipping_service_type_id int,
    expected_delivery_time   datetime2,
    payment_method           varchar(50) not null,
    payment_status           varchar(20) default 'UNPAID',
    order_status             varchar(30) default 'PENDING',
    staff_id                 int references Users,
    shipper_id               int references Users,
    assigned_at              datetime2,
    confirmed_at             datetime2,
    ready_at                 datetime2,
    picked_up_at             datetime2,
    delivered_at             datetime2,
    cancelled_at             datetime2,
    failure_reason           nvarchar(500),
    internal_note            nvarchar(1000),
    delivery_note            nvarchar(500),
    created_at               datetime2 default getdate()
);
```

---

### 5.7 DeliveryZone fallback

```sql
create table DeliveryZone
(
    zone_id       int identity primary key,
    district_name nvarchar(100) not null,
    shipping_fee  decimal(10, 2) default 0,
    is_active     bit default 1
);
```

Vai trò mới:

- Không còn là nguồn chính tính phí.
- Chỉ dùng khi GHN lỗi hoặc dev/test offline.

---

### 5.8 Bỏ Ingredient/ProductIngredient

Bỏ khỏi schema:

```sql
drop table ProductIngredient;
drop table Ingredient;
```

Lý do:

- Không triển khai kho thật.
- Nguyên liệu thực tế quá nhiều và khó chuẩn hóa.
- Checkout hiện không trừ nguyên liệu.
- Staff status hiện không trừ nguyên liệu.

---

## 6. Migration SQL đề xuất

### 6.1 Thêm ProductVariant

```sql
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
```

---

### 6.2 Đồng bộ Product

```sql
if col_length('Product', 'gallery_images') is null
    alter table Product add gallery_images nvarchar(max);

go

if col_length('Product', 'base_price') is null
    alter table Product add base_price decimal(10, 2);

go

update Product
set base_price = price
where base_price is null;
```

Sau khi backend đổi sang `base_price`, có thể drop `price` ở phase sau:

```sql
-- alter table Product drop column price;
```

Khuyến nghị chưa drop ngay để tránh vỡ code cũ.

---

### 6.3 Tạo variant mặc định từ Product

```sql
insert into ProductVariant
(
    product_id,
    variant_name,
    price,
    is_default,
    status,
    created_at
)
select
    p.product_id,
    N'Mặc định',
    p.price,
    1,
    p.status,
    getdate()
from Product p
where not exists
(
    select 1
    from ProductVariant pv
    where pv.product_id = p.product_id
      and pv.is_default = 1
);
```

---

### 6.4 Migrate ProductOption sang ProductVariant

```sql
insert into ProductVariant
(
    product_id,
    variant_name,
    price,
    is_default,
    quantity_available,
    status,
    created_at
)
select
    po.product_id,
    po.option_name,
    p.price + po.extra_price,
    0,
    po.quantity_available,
    case when p.status = 'AVAILABLE' then 'AVAILABLE' else 'UNAVAILABLE' end,
    getdate()
from ProductOption po
join Product p on po.product_id = p.product_id
where not exists
(
    select 1
    from ProductVariant pv
    where pv.product_id = po.product_id
      and pv.variant_name = po.option_name
);
```

---

### 6.5 Cập nhật CartItem

```sql
if col_length('CartItem', 'variant_id') is null
    alter table CartItem add variant_id int null references ProductVariant;

go

if col_length('CartItem', 'created_at') is null
    alter table CartItem add created_at datetime2 default getdate();

go

update ci
set ci.variant_id = pv.variant_id
from CartItem ci
join ProductVariant pv on ci.product_id = pv.product_id
where pv.is_default = 1
  and ci.variant_id is null;
```

Sau khi migrate xong:

```sql
alter table CartItem alter column variant_id int not null;
```

---

### 6.6 Cập nhật OrderItem

```sql
if col_length('OrderItem', 'variant_id') is null
    alter table OrderItem add variant_id int null references ProductVariant;

go

if col_length('OrderItem', 'variant_name') is null
    alter table OrderItem add variant_name nvarchar(255);

go

update oi
set
    oi.variant_id = pv.variant_id,
    oi.variant_name = pv.variant_name
from OrderItem oi
join ProductVariant pv on oi.product_id = pv.product_id
where pv.is_default = 1
  and oi.variant_id is null;
```

Không nên ép `OrderItem.variant_id not null` vì đơn cũ vẫn cần xem được nếu dữ liệu variant bị thiếu.

---

### 6.7 Cập nhật Address cho GHN

```sql
if col_length('Address', 'province_name') is null
    alter table Address add province_name nvarchar(100);

go

if col_length('Address', 'district_name') is null
    alter table Address add district_name nvarchar(100);

go

if col_length('Address', 'ward_name') is null
    alter table Address add ward_name nvarchar(100);

go

if col_length('Address', 'ghn_province_id') is null
    alter table Address add ghn_province_id int;

go

if col_length('Address', 'ghn_district_id') is null
    alter table Address add ghn_district_id int;

go

if col_length('Address', 'ghn_ward_code') is null
    alter table Address add ghn_ward_code varchar(50);
```

Cần đổi `Address.zone_id` thành nullable bằng migration riêng nếu constraint hiện tại đang `NOT NULL`.

---

### 6.8 Cập nhật Orders cho GHN

```sql
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
```

Cần đổi `Orders.zone_id` thành nullable bằng migration riêng nếu constraint hiện tại đang `NOT NULL`.

---

### 6.9 Drop bảng không dùng

Chỉ chạy sau khi backend/frontend đã bỏ reference:

```sql
if object_id('ProductIngredient', 'U') is not null
    drop table ProductIngredient;

go

if object_id('Ingredient', 'U') is not null
    drop table Ingredient;

go

if object_id('ProductOption', 'U') is not null
    drop table ProductOption;
```

Thứ tự drop đúng:

```text
ProductIngredient -> Ingredient
ProductOption sau khi migrate sang ProductVariant
```

---

## 7. Backend cần cập nhật

### 7.1 Entity

Cần sửa/tạo:

```text
Product.java
ProductVariant.java
CartItem.java
OrderItem.java
Orders.java
Address.java
```

Bỏ/ngưng dùng:

```text
ProductOption.java
Ingredient.java
ProductIngredient.java
```

---

### 7.2 ProductDAO

Cần có method:

```text
findVariantsByProductId(int productId)
findVariantById(int variantId)
saveVariant(ProductVariant variant)
deleteVariant(int variantId)
```

---

### 7.3 Cart flow

Add cart mới nhận:

```json
{
  "productId": 1,
  "variantId": 2,
  "quantity": 1
}
```

Backend validate:

1. Product tồn tại.
2. Variant tồn tại.
3. Variant thuộc product.
4. Product/variant đang available.
5. Nếu `quantity_available` không null thì phải đủ số lượng.
6. Giá lấy từ `ProductVariant.price`.

---

### 7.4 Checkout flow

Khi checkout:

```text
CartItem -> OrderItem
```

Copy snapshot:

```text
product_id
variant_id
product_name
variant_name
unit_price
quantity
total_price
```

---

### 7.5 Shipping GHN

Thêm:

```text
ShippingServlet
ShippingService
GhnClient
```

Endpoint:

```http
GET /api/shipping/provinces
GET /api/shipping/districts?provinceId=202
GET /api/shipping/wards?districtId=1442
POST /api/shipping/fee
```

Request tính phí:

```json
{
  "toDistrictId": 1442,
  "toWardCode": "20107",
  "weight": 1000,
  "length": 20,
  "width": 20,
  "height": 10
}
```

Response:

```json
{
  "shippingProvider": "GHN",
  "shippingFee": 25000,
  "serviceId": 53320,
  "serviceTypeId": 2,
  "expectedDeliveryTime": "2026-06-26T10:00:00"
}
```

---

## 8. Frontend cần cập nhật

### 8.1 ProductDetailPage

- Load danh sách variants.
- Bắt khách chọn variant.
- Giá hiển thị theo variant.
- Add cart gửi `variantId`.

---

### 8.2 CartPage

Hiển thị:

```text
Product name
Variant name
Unit price
Quantity
Total
```

Không dùng `optionData` nữa.

---

### 8.3 CheckoutPage

- Load tỉnh từ GHN.
- Chọn tỉnh thì load huyện.
- Chọn huyện thì load xã.
- Chọn xã thì gọi API tính phí.
- Lưu GHN fields vào order.

---

### 8.4 OrderDetailPage

Hiển thị:

```text
Product name
Variant name
Unit price
Quantity
Total
Shipping fee GHN
Expected delivery time
```

---

## 9. Checklist test

### Product/Variant

- Product list hiển thị giá từ `base_price` hoặc default variant.
- Product detail hiển thị variants.
- Chọn variant đổi giá đúng.
- Variant unavailable không cho mua.

### Cart

- Add cart bằng variant.
- Cart hiển thị đúng variant.
- Tăng/giảm quantity giữ đúng giá.
- Không còn phụ thuộc `optionData`.

### Checkout

- Chọn tỉnh/huyện/xã GHN.
- Tính phí ship thành công.
- Nếu GHN lỗi, fallback DeliveryZone/default fee.
- Order lưu đúng shipping fee.

### Order

- OrderItem lưu `product_name`, `variant_name`, `unit_price` snapshot.
- User order detail xem đúng variant.
- Staff order detail xem đúng variant.
- Admin order detail xem đúng variant.

### Cleanup

- Không còn menu Ingredient/LowStock nếu đã bỏ.
- Không còn API gọi Ingredient.
- Không còn entity/DAO Ingredient được dùng.

---

## 10. Phân công 6 thành viên

### Thành viên 1 — Database migration

- Viết migration SQL.
- Tạo `ProductVariant`.
- Migrate `Product.price` sang `base_price`.
- Migrate `ProductOption` sang `ProductVariant`.
- Drop `Ingredient`, `ProductIngredient`, `ProductOption` sau khi code sạch.

### Thành viên 2 — Backend Product/Variant

- Tạo `ProductVariant` entity.
- Sửa `Product` entity dùng `basePrice`.
- Sửa `ProductDAO`.
- Sửa `ProductServlet`.
- Sửa `AdminProductServlet` CRUD variant.

### Thành viên 3 — Backend Cart/Order

- Sửa `CartItem` entity.
- Sửa `CartServlet` nhận `variantId`.
- Sửa `CartDAO`.
- Sửa `OrderService.checkout` copy variant snapshot.
- Sửa `OrderServlet`/`StaffOrderServlet` trả `variantName`.

### Thành viên 4 — GHN Shipping

- Tạo `ShippingServlet`.
- Tạo `ShippingService`.
- Tạo GHN client gọi provinces/districts/wards/fee.
- Thêm fallback DeliveryZone.
- Không hardcode token trong code.

### Thành viên 5 — Frontend Product/Cart/Checkout

- Product detail chọn variant.
- Cart hiển thị variant.
- Checkout chọn tỉnh/huyện/xã GHN.
- Gọi API tính phí ship.
- Gửi GHN fields khi tạo order.

### Thành viên 6 — Cleanup/Test/Docs

- Bỏ UI Ingredient/LowStock.
- Test full flow Product -> Variant -> Cart -> Checkout -> Staff -> User tracking.
- Cập nhật README.
- Build backend/frontend trước demo.

---

## 11. Thứ tự triển khai khuyến nghị

1. Tạo migration database.
2. Thêm backend entity/DAO `ProductVariant`.
3. Sửa Product API.
4. Sửa Cart API.
5. Sửa Checkout/Order API.
6. Thêm GHN Shipping API.
7. Sửa frontend Product/Cart/Checkout.
8. Bỏ Ingredient UI/API/code.
9. Drop bảng cũ.
10. Test full flow.
