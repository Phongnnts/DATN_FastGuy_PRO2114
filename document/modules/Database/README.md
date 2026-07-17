# Module Database

**Người phụ trách**: Người 6

## Mục tiêu

Hoàn thiện schema mới cho FastGuy, đồng bộ với thiết kế ProductVariant, GHN shipping và bỏ nguyên liệu.

---

## Quyết định thiết kế

```text
Product = món cha / món chung
ProductVariant = phiên bản khách thật sự mua
DeliveryZone = fallback khi GHN lỗi/dev offline
Ingredient/ProductIngredient = bỏ hẳn
ProductOption = thay bằng ProductVariant
```

---

## File chính

```text
database/init.sql
database/FastGuyDBUpdate.md
```

---

## Schema cần có

### Product

- `product_id`
- `category_id`
- `name`
- `description`
- `base_price`
- `image_url`
- `gallery_images`
- `status`
- `created_at`
- `updated_at`

### ProductVariant

- `variant_id`
- `product_id`
- `variant_name`
- `price`
- `original_price`
- `sku`
- `quantity_available`
- `is_default`
- `status`
- `created_at`
- `updated_at`

### CartItem

- `cart_item_id`
- `cart_id`
- `product_id`
- `variant_id`
- `quantity`
- `unit_price`
- `created_at`

### OrderItem

- `order_item_id`
- `order_id`
- `product_id`
- `variant_id`
- `product_name`
- `variant_name`
- `quantity`
- `unit_price`
- `total_price`

### Address GHN fields

- `province_name`
- `district_name`
- `ward_name`
- `ghn_province_id`
- `ghn_district_id`
- `ghn_ward_code`
- `zone_id` nullable fallback

### Orders GHN fields

- `to_province_name`
- `to_district_name`
- `to_ward_name`
- `ghn_province_id`
- `ghn_district_id`
- `ghn_ward_code`
- `shipping_provider`
- `shipping_service_id`
- `shipping_service_type_id`
- `expected_delivery_time`

---

## Bảng bỏ khỏi scope

Phải không còn trong schema mới:

```text
Ingredient
ProductIngredient
ProductOption
```

---

## ERD mới

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

## Việc cần làm

- [ ] Kiểm tra `init.sql` chạy được từ database trống.
- [ ] Kiểm tra `ProductVariant` được seed cho tất cả products.
- [ ] Kiểm tra cart seed có `variant_id` hợp lệ.
- [ ] Kiểm tra order item seed có `variant_id`, `variant_name`.
- [ ] Kiểm tra không còn `option_data`.
- [ ] Kiểm tra không còn `Ingredient`, `ProductIngredient`, `ProductOption`.
- [ ] Kiểm tra `Address.zone_id` nullable.
- [ ] Kiểm tra `Orders.zone_id` nullable.
- [ ] Cập nhật entity backend theo schema mới.

---

## Checklist test SQL

- [ ] Tạo database mới thành công.
- [ ] Chạy toàn bộ `init.sql` không lỗi.
- [ ] Select products có variants.
- [ ] Select cart items join được variant.
- [ ] Select order items join được variant.
- [ ] Select address có GHN fields.
- [ ] Select orders có shipping fields.

---

## Query kiểm tra nhanh

```sql
select p.product_id, p.name, pv.variant_id, pv.variant_name, pv.price
from Product p
join ProductVariant pv on p.product_id = pv.product_id
order by p.product_id, pv.variant_id;
```

```sql
select ci.cart_item_id, p.name, pv.variant_name, ci.quantity, ci.unit_price
from CartItem ci
join Product p on ci.product_id = p.product_id
join ProductVariant pv on ci.variant_id = pv.variant_id;
```

```sql
select oi.order_item_id, oi.product_name, oi.variant_name, oi.quantity, oi.unit_price, oi.total_price
from OrderItem oi;
```

---

## Phụ thuộc

- **Admin** cần schema ProductVariant để CRUD.
- **Guest** cần ProductVariant để chọn món.
- **User** cần Cart/Order theo variant.
- **Shipping** cần GHN fields trong Address/Orders.
