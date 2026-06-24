# WORK PLAN — FastGuy

## Mục tiêu giai đoạn tiếp theo

Hoàn thiện FastGuy theo thiết kế mới:

```text
Product = món cha / món chung
ProductVariant = phiên bản khách thật sự mua
GHN = nguồn chính tính phí vận chuyển
DeliveryZone = fallback khi GHN lỗi/dev offline
Ingredient/ProductIngredient = bỏ khỏi scope
```

---

## Phân công tổng quan

| Người       | Module chính       | Phạm vi                                                  |
| ----------- | ------------------ | -------------------------------------------------------- |
| **Người 1** | Auth               | Login/register/profile, JWT, route guard                 |
| **Người 2** | Guest              | Menu, ProductDetail, ProductVariant UI, Cart             |
| **Người 3** | User               | Address CRUD, Checkout, GHN address UI, order history    |
| **Người 4** | Staff              | Order processing PENDING → READY, tabs, timeline         |
| **Người 5** | Admin              | Product CRUD, ProductVariant CRUD, Cloudinary, dashboard |
| **Người 6** | Common/DB/Shipping | Database, GHN backend, constants, layout, docs, test     |

---

## Module 1 — Auth / Người 1

### Mục tiêu

Đảm bảo hệ thống đăng nhập, đăng ký, profile và phân quyền ổn định cho toàn bộ flow.

### Backend

- Kiểm tra `AuthServlet.java`.
- Kiểm tra `AuthService.java`.
- Kiểm tra `UserDAO.java`.
- Kiểm tra `JwtUtil.java`.
- Hoàn thiện/củng cố API:
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET /api/auth/profile`
  - `PUT /api/auth/profile`
  - `POST /api/auth/logout` nếu cần.
- JWT cần có:
  - `userId`
  - `role`
  - `email`
  - `fullName`

### Frontend

- Kiểm tra `LoginPage.vue`.
- Kiểm tra `RegisterPage.vue`.
- Kiểm tra `ProfilePage.vue`.
- Kiểm tra `ChangePasswordPage.vue`.
- Kiểm tra `stores/auth.js`.
- Kiểm tra `router/index.js`.
- Route guard:
  - Guest không vào User/Staff/Admin.
  - User không vào Staff/Admin.
  - Staff không vào Admin.
  - Admin không bị redirect sai.

### Checklist

- [ ] Login Admin/Staff/User thành công.
- [ ] Register user mới thành công.
- [ ] Refresh trang vẫn giữ session.
- [ ] Token sai/hết hạn thì logout.
- [ ] Profile load đúng dữ liệu.
- [ ] Route guard đúng role.

---

## Module 2 — Guest / Người 2

### Mục tiêu

Khách xem menu, xem chi tiết món, chọn `ProductVariant`, thêm giỏ hàng và xem cart đúng biến thể.

### Backend phối hợp

Phối hợp với Người 5 để thống nhất product response:

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "imageUrl": "",
  "galleryImages": [],
  "variants": [
    {
      "variantId": 1,
      "variantName": "Mặc định",
      "price": 45000,
      "isDefault": true,
      "status": "AVAILABLE"
    }
  ]
}
```

### Frontend

- Sửa `MenuPage.vue`:
  - Hiển thị `basePrice` hoặc giá variant mặc định.
  - Không dùng `Product.price` cũ.
- Sửa `ProductDetailPage.vue`:
  - Load `variants`.
  - Cho khách chọn variant.
  - Giá thay đổi theo variant.
  - Add cart gửi `productId`, `variantId`, `quantity`.
- Sửa `CartPage.vue`:
  - Hiển thị `variantName`.
  - Không dùng `optionData`.
  - Tổng tiền tính từ `unitPrice * quantity`.
- Kiểm tra `stores/cart.js`:
  - Lưu variant trong item.
  - Không còn phụ thuộc option string.

### Checklist

- [ ] Menu hiển thị giá đúng.
- [ ] Product detail hiển thị variants.
- [ ] Chọn variant đổi giá đúng.
- [ ] Add cart gửi đúng `variantId`.
- [ ] Cart hiển thị đúng tên variant.
- [ ] Không còn lỗi `optionData`.

---

## Module 3 — User / Người 3

### Mục tiêu

User quản lý địa chỉ, checkout với GHN, xem lịch sử đơn và trạng thái đơn.

### Backend

- Cập nhật `AddressServlet.java`.
- Cập nhật `AddressDAO.java`.
- Address cần hỗ trợ:
  - `provinceName`
  - `districtName`
  - `wardName`
  - `ghnProvinceId`
  - `ghnDistrictId`
  - `ghnWardCode`
  - `zoneId` nullable fallback.
- Cập nhật `OrderServlet.java` để nhận GHN shipping fields.
- Cập nhật `OrderService.checkout()`:
  - Đọc cart theo `variantId`.
  - Lưu shipping fee thật.
  - Lưu snapshot địa chỉ.

### Frontend

- Sửa `ProfilePage.vue`:
  - CRUD địa chỉ.
  - Lưu tỉnh/huyện/xã GHN.
- Sửa `CheckoutPage.vue`:
  - Load provinces.
  - Chọn province → load districts.
  - Chọn district → load wards.
  - Chọn ward → gọi `/api/shipping/fee`.
  - Hiển thị shipping fee.
- Sửa `OrdersPage.vue`.
- Sửa `OrderDetailPage.vue`:
  - Hiển thị product + variant.
  - Hiển thị shipping fee.
  - Hiển thị timeline.

### Checklist

- [ ] Thêm/sửa/xóa địa chỉ.
- [ ] Set địa chỉ mặc định.
- [ ] Checkout dùng địa chỉ đã lưu.
- [ ] Checkout tính phí GHN.
- [ ] Order lưu đúng shipping fee.
- [ ] User thấy trạng thái staff cập nhật.

---

## Module 4 — Staff / Người 4

### Mục tiêu

Staff xử lý đơn hàng ổn định theo flow:

```text
PENDING → CONFIRMED → PREPARING → READY
PENDING/CONFIRMED/PREPARING → CANCELLED
```

### Backend

- Cập nhật `StaffOrderServlet.java`.
- Cập nhật `StaffOrderService.java`.
- Đảm bảo API:
  - `GET /api/staff/orders`
  - `GET /api/staff/orders/confirmed`
  - `GET /api/staff/orders/preparing`
  - `GET /api/staff/orders/ready`
  - `GET /api/staff/orders/history`
  - `GET /api/staff/orders/{id}`
  - `PUT /api/staff/orders/{id}/status`
- Response order item cần có:
  - `productName`
  - `variantName`
  - `quantity`
  - `unitPrice`
  - `totalPrice`

### Frontend

- Sửa `OrdersPage.vue`:
  - Tabs hoạt động đúng.
  - Không race condition.
  - Không bị 500 silent.
  - Hiển thị trạng thái đúng.
- Sửa `OrderDetailPage.vue`:
  - Hiển thị product + variant.
  - Nút chuyển trạng thái đúng.
  - Timeline đúng.
  - Hủy đơn có lý do.
- Sửa `DashboardPage.vue`:
  - Chart status đúng màu.

### Checklist

- [ ] PENDING tab có đơn mới.
- [ ] Confirm xong qua CONFIRMED.
- [ ] Start preparing xong qua PREPARING.
- [ ] Complete xong qua READY.
- [ ] Cancel đơn lưu lý do.
- [ ] User timeline cập nhật.
- [ ] Không còn 500 ở các tab.

---

## Module 5 — Admin / Người 5

### Mục tiêu

Admin quản lý product và variant theo schema mới, upload ảnh Cloudinary và kiểm tra dashboard.

### Backend

- Sửa `Product.java`:
  - `basePrice`
  - `galleryImages`
  - bỏ dùng `price` cũ.
- Tạo `ProductVariant.java`.
- Sửa `ProductDAO.java`:
  - `findVariantsByProductId`
  - `findVariantById`
  - `saveVariant`
  - `deleteVariant`
- Sửa `ProductServlet.java`:
  - Product detail trả variants.
- Sửa `AdminProductServlet.java`:
  - CRUD product.
  - CRUD variant.
  - Không dùng `ProductOption`.

### Frontend

- Sửa `ProductsPage.vue`:
  - Form product dùng `basePrice`.
  - Quản lý danh sách variants.
  - Thêm/sửa/xóa variant.
  - Set default variant.
  - Upload ảnh Cloudinary.
  - Gallery images.
- Kiểm tra `DashboardPage.vue`.
- Kiểm tra `CategoriesPage.vue`.

### Checklist

- [ ] Admin tạo product.
- [ ] Admin tạo variant.
- [ ] Product detail thấy variant mới.
- [ ] Admin sửa giá variant.
- [ ] Đơn cũ không đổi giá vì OrderItem snapshot.
- [ ] Xóa/ẩn variant không làm vỡ đơn cũ.

---

## Module 6 — Common / Database / Shipping / Người 6

### Mục tiêu

Đảm bảo schema mới, GHN API, constants, layout, docs và test tổng thể.

### Database

- Kiểm tra `database/init.sql`.
- Đảm bảo có:
  - `ProductVariant`
  - `Product.base_price`
  - `Product.gallery_images`
  - `CartItem.variant_id`
  - `OrderItem.variant_id`
  - `OrderItem.variant_name`
  - GHN fields trong `Address`
  - GHN fields trong `Orders`
- Đảm bảo đã bỏ:
  - `Ingredient`
  - `ProductIngredient`
  - `ProductOption`

### Shipping GHN

Tạo backend shipping:

```text
ShippingServlet
ShippingService
GhnClient
```

Endpoint:

```http
GET /api/shipping/provinces
GET /api/shipping/districts?provinceId=
GET /api/shipping/wards?districtId=
POST /api/shipping/fee
```

Yêu cầu:

- Không hardcode token trong code.
- Token/shopId lấy từ config/env.
- Nếu GHN lỗi thì fallback `DeliveryZone`.

### Common

- Cập nhật `constants.js`:
  - Cloudinary.
  - order status.
  - payment status.
  - shipping provider.
- Kiểm tra layouts/sidebar.
- Bỏ menu Ingredients/LowStock nếu không dùng.
- Kiểm tra `format.js`.

### Docs/Test

- Cập nhật tài liệu trong `document/`.
- Viết checklist demo.
- Build backend/frontend.

### Checklist

- [ ] `init.sql` chạy được từ đầu.
- [ ] Backend compile.
- [ ] Frontend build.
- [ ] GHN fee API hoạt động hoặc fallback.
- [ ] Sidebar không còn link không dùng.
- [ ] Full flow demo chạy được.

---

# Timeline triển khai

## Tuần 1 — Database + Model nền tảng

| Người   | Việc                                     |
| ------- | ---------------------------------------- |
| Người 1 | Rà Auth/profile/route guard              |
| Người 2 | Chuẩn bị ProductDetail dùng variant      |
| Người 3 | Chuẩn bị Address mới có GHN fields       |
| Người 4 | Rà Staff order không phụ thuộc schema cũ |
| Người 5 | Tạo entity/DAO ProductVariant            |
| Người 6 | Chốt `init.sql`, kiểm tra schema mới     |

## Tuần 2 — ProductVariant + Cart

| Người   | Việc                                               |
| ------- | -------------------------------------------------- |
| Người 1 | Fix auth/profile phát sinh                         |
| Người 2 | ProductDetail chọn variant + Cart hiển thị variant |
| Người 3 | Checkout đọc cart mới                              |
| Người 4 | Staff detail hiển thị variant                      |
| Người 5 | Admin CRUD variant                                 |
| Người 6 | Common constants/status                            |

## Tuần 3 — GHN + Checkout + Order

| Người   | Việc                                        |
| ------- | ------------------------------------------- |
| Người 1 | Test role guard toàn flow                   |
| Người 2 | Đảm bảo cart gửi đúng variant               |
| Người 3 | Checkout chọn tỉnh/huyện/xã + tính phí ship |
| Người 4 | Staff tabs/status ổn định                   |
| Người 5 | Admin kiểm tra product data                 |
| Người 6 | GHN API backend + fallback                  |

## Tuần 4 — Integration + Demo

| Người   | Việc                               |
| ------- | ---------------------------------- |
| Người 1 | Auth/session/permission test       |
| Người 2 | Guest/menu/cart UX                 |
| Người 3 | User order history/detail/timeline |
| Người 4 | Staff order flow full test         |
| Người 5 | Admin dashboard/products/orders    |
| Người 6 | Docs + build + checklist demo      |

---

# Checklist end-to-end

## Flow 1 — Product Variant

```text
Admin tạo product
→ Admin tạo variants
→ Guest xem product detail
→ Guest chọn variant
→ Add cart
→ Cart hiển thị variant
```

## Flow 2 — Checkout GHN

```text
User chọn địa chỉ
→ Chọn tỉnh/huyện/xã
→ Gọi GHN fee
→ Checkout
→ Order lưu shipping fee + GHN fields
```

## Flow 3 — Staff Status

```text
Order PENDING
→ Staff CONFIRMED
→ Staff PREPARING
→ Staff READY
→ User thấy timeline cập nhật
```

## Flow 4 — Admin kiểm tra

```text
Admin xem dashboard
→ Xem product/variant
→ Xem order
→ Kiểm tra dữ liệu không lỗi
```

---

# Lệnh kiểm tra trước khi demo

## Backend

```bash
cd Backend/FastGuy-FastFoodSite
mvn clean compile
```

## Frontend

```bash
cd Frontend
npm run build
```
