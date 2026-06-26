# WORK PLAN — FastGuy

## Mục tiêu giai đoạn

Hoàn thiện FastGuy theo thiết kế mới:

```text
Product = món cha / món chung
ProductVariant = phiên bản khách thật sự mua
GHN = nguồn chính tính phí vận chuyển
DeliveryZone = fallback khi GHN lỗi/dev offline
Ingredient/ProductIngredient = bỏ hẳn
```

---

## Trạng thái hiện tại (Phase 0 — Đã hoàn thành)

### Database

- `init.sql` mới bao gồm:
  - `Product` dùng `base_price`
  - `ProductVariant`
  - `CartItem` có `variant_id` (bỏ `option_data`)
  - `OrderItem` có `variant_id`, `variant_name` (bỏ `option_data`)
  - `Address` thêm GHN fields
  - `Orders` thêm GHN fields
  - Đã bỏ: `Ingredient`, `ProductIngredient`, `ProductOption`

### Backend

| File | Thay đổi |
|---|---|
| `Product.java` | `price` → `basePrice` |
| `ProductVariant.java` | Entity mới |
| `CartItem.java` | Bỏ `optionData`, thêm `variant` |
| `OrderItem.java` | Bỏ `optionData`, thêm `variant`, `variantName` |
| `Address.java` | Thêm GHN fields |
| `Orders.java` | Thêm GHN + shipping fields |
| `ProductDAO.java` | Bỏ option methods, thêm variant methods |
| `CartService.java` | `addItem` nhận `variantId`, giá lấy từ variant |
| `OrderService.java` | Checkout copy snapshot variant |
| `StaffService.java` | Bỏ `lowStockIngredients` |
| `ProductServlet.java` | Trả `variants` |
| `AdminProductServlet.java` | CRUD variant thay option |
| `CartServlet.java` | Nhận `variantId` (không nhận `unitPrice`/`optionData`) |
| `persistence.xml` | Đăng ký `ProductVariant`, bỏ entity cũ |

### Frontend

- Đã bỏ routes Ingredients/LowStock (staff + admin)
- Đã bỏ API Ingredient (admin/staff)
- Đã bỏ store Ingredient (admin/staff)
- Đã bỏ `lowStockIngredients` dashboard card
- Đã bỏ `optionData` khỏi CartItem, thay bằng `variantName`

### Branches

Tất cả 6 nhánh `module/*` đã merge `develop` mới nhất và push.

---

## Phân công tổng quan

| Người | Module chính | Phạm vi |
|---|---|---|
| **Người 1** | Auth | Login/register/profile, JWT, route guard |
| **Người 2** | Guest | Menu, ProductDetail, ProductVariant UI, Cart |
| **Người 3** | User | Address CRUD, Checkout, GHN address UI, order history |
| **Người 4** | Staff | Order processing PENDING → READY, tabs, timeline |
| **Người 5** | Admin | Product CRUD, ProductVariant CRUD, Cloudinary, dashboard |
| **Người 6** | Common/DB/Shipping | Database, GHN backend, constants, layout, docs, test |

---

## Phase 1 — ProductVariant API + Admin UI

**Yêu cầu:** Phải hoàn thành trước Phase 2 (Guest cần Product API hoàn chỉnh).

### Người 5 — Admin

| STT | Việc | File |
|---|---|---|
| 1.1 | `ProductsPage.vue`: form dùng `basePrice`, thêm gallery images | `admin/ProductsPage.vue` |
| 1.2 | `ProductsPage.vue`: quản lý variants (thêm/sửa/xóa, set default) | `admin/ProductsPage.vue` |
| 1.3 | Upload ảnh Cloudinary widget | `admin/ProductsPage.vue` |
| 1.4 | Kiểm tra `DashboardPage.vue` | `admin/DashboardPage.vue` |

**Backend đã xong** — AdminProductServlet có đầy đủ CRUD variant.

### Checklist

- [ ] Admin tạo product
- [ ] Admin tạo variant (size M, size L, combo...)
- [ ] Admin set default variant
- [ ] Product detail trả variants
- [ ] Upload ảnh Cloudinary

---

## Phase 2 — Guest ProductDetail + Cart

**Phụ thuộc:** Phase 1 xong (Product API có variants).

### Người 2 — Guest

| STT | Việc | File |
|---|---|---|
| 2.1 | `MenuPage.vue`: hiển thị `basePrice` hoặc default variant price | `guest/MenuPage.vue` |
| 2.2 | `ProductDetailPage.vue`: load `variants`, cho chọn variant, đổi giá | `guest/ProductDetailPage.vue` |
| 2.3 | Add cart gửi `productId`, `variantId`, `quantity` | `guest/ProductDetailPage.vue` |
| 2.4 | `CartPage.vue`: hiển thị `variantName`, bỏ `optionData` | `guest/CartPage.vue` |
| 2.5 | `stores/cart.js`: sửa `addItem` thành `(productId, variantId, quantity)` | `stores/cart.js` |

### Response API cần dùng

**Product list:**

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "price": 45000,
  "defaultVariant": { "variantId": 1, "variantName": "Mặc định", "price": 45000 },
  "imageUrl": "",
  "categoryName": "Burger"
}
```

**Product detail:**

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "variants": [
    { "variantId": 1, "variantName": "Mặc định", "price": 45000, "isDefault": true, "status": "AVAILABLE" },
    { "variantId": 2, "variantName": "Size L", "price": 55000, "isDefault": false, "status": "AVAILABLE" }
  ],
  "galleryImages": []
}
```

### Checklist

- [ ] Menu hiển thị giá đúng
- [ ] Product detail hiển thị variants
- [ ] Chọn variant đổi giá đúng
- [ ] Add cart gửi `productId`, `variantId`, `quantity`
- [ ] Cart hiển thị `variantName`

---

## Phase 3 — Checkout basic + Order + Staff

**Phụ thuộc:** Phase 2 xong (Cart có variant). Backend đã xong.

### Người 3 — User

| STT | Việc | File |
|---|---|---|
| 3.1 | `ProfilePage.vue`: CRUD address mới (wardName/districtName/provinceName/GHN ids) | `user/ProfilePage.vue` |
| 3.2 | `CheckoutPage.vue`: checkout với cart variant, fallback fee | `user/CheckoutPage.vue` |
| 3.3 | `OrderDetailPage.vue`: hiển thị `variantName`, product | `user/OrderDetailPage.vue` |
| 3.4 | `OrdersPage.vue`: lịch sử đơn | `user/OrdersPage.vue` |

### Người 4 — Staff

| STT | Việc | File |
|---|---|---|
| 3.5 | `OrdersPage.vue`: tabs PENDING/CONFIRMED/PREPARING/READY (fix race + 500) | `staff/OrdersPage.vue` |
| 3.6 | `OrderDetailPage.vue`: hiển thị `variantName`, product | `staff/OrderDetailPage.vue` |
| 3.7 | Nút chuyển trạng thái đúng, cancel modal | `staff/OrderDetailPage.vue` |
| 3.8 | `DashboardPage.vue`: chart status đúng màu | `staff/DashboardPage.vue` |

### Checklist

- [ ] User checkout tạo đơn thành công
- [ ] OrderItem có `variantName`
- [ ] User xem đơn thấy variant
- [ ] Staff tabs hoạt động đúng
- [ ] Staff detail thấy variant
- [ ] Staff chuyển trạng thái đúng flow
- [ ] Không còn 500 ở tabs

---

## Phase 4 — GHN Shipping

**Phụ thuộc:** Phase 3 xong (checkout basic ổn định).

### Người 6 — Shipping

| STT | Việc | File |
|---|---|---|
| 4.1 | Tạo `GhnClient.java` gọi API GHN | `utils/GhnClient.java` |
| 4.2 | Tạo `ShippingService.java` xử lý business | `service/ShippingService.java` |
| 4.3 | Tạo `ShippingServlet.java` expose API | `servlet/ShippingServlet.java` |
| 4.4 | Implement provinces API | `ShippingServlet.java` |
| 4.5 | Implement districts API | `ShippingServlet.java` |
| 4.6 | Implement wards API | `ShippingServlet.java` |
| 4.7 | Implement fee calculation | `ShippingServlet.java` |
| 4.8 | Implement fallback DeliveryZone | `ShippingService.java` |
| 4.9 | Config GHN (token/shopId không hardcode) | `utils/AppConfig.java` |

### Người 3 (phối hợp)

| STT | Việc | File |
|---|---|---|
| 4.10 | `CheckoutPage.vue`: chọn province/district/ward | `user/CheckoutPage.vue` |
| 4.11 | Gọi `/api/shipping/fee` | `user/CheckoutPage.vue` |
| 4.12 | Hiển thị shipping fee thật | `user/CheckoutPage.vue` |
| 4.13 | Lưu GHN fields vào order | `user/CheckoutPage.vue` + `OrderService.java` (đã sẵn entity) |

### API GHN cần implement

```http
GET  /api/shipping/provinces
GET  /api/shipping/districts?provinceId=202
GET  /api/shipping/wards?districtId=1442
POST /api/shipping/fee
```

Body tính phí:

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

### Checklist

- [ ] Lấy province thành công
- [ ] Lấy district theo province thành công
- [ ] Lấy ward theo district thành công
- [ ] Tính phí GHN thành công
- [ ] GHN lỗi thì fallback zone
- [ ] Checkout lưu đúng shipping fee
- [ ] Orders lưu đúng GHN fields

---

## Phase 5 — Integration + Test

### Người 1 — Auth

- Kiểm tra `LoginPage.vue`, `RegisterPage.vue`, `ProfilePage.vue`, `ChangePasswordPage.vue`
- Route guard: Guest → User → Staff → Admin
- Refresh giữ session, token hết hạn logout

### Người 2 — Guest UX

- Menu/Cart mượt
- Không còn lỗi `optionData`

### Người 3 — User integration

- User order history/detail
- Timeline trạng thái
- Shipping fee hiển thị

### Người 4 — Staff full test

- Staff order flow PENDING → CONFIRMED → PREPARING → READY
- Cancel + lý do

### Người 5 — Admin integration

- Dashboard/products/orders
- CRUD variant

### Người 6 — Common cleanup

- Cập nhật `constants.js`: Cloudinary, order status, shipping provider
- Kiểm tra sidebar/layout
- Frontend build

---

## Phase 6 — Cleanup cuối

### Tất cả

- [ ] Xóa file frontend Ingredient/LowStock còn sót (nếu có)
- [ ] `npm run build` không lỗi
- [ ] Cập nhật README/document

---

## Timeline

| Tuần | Phase | Người tham gia chính |
|---|---|---|
| Tuần 1 | Phase 1 — Admin variant UI | Người 5 |
| Tuần 1 | Phase 2 — Guest ProductDetail + Cart | Người 2 |
| Tuần 2 | Phase 3 — Checkout + Order + Staff | Người 3, Người 4 |
| Tuần 2 | Phase 1 bổ sung | Người 5 (nếu cần) |
| Tuần 3 | Phase 4 — GHN Shipping | Người 6 + Người 3 |
| Tuần 3 | Phase 5 — Integration test | Tất cả |
| Tuần 4 | Phase 6 — Cleanup + Demo | Tất cả |

---

## Luồng end-to-end

```text
Admin tạo product + variant
→ Guest xem menu, chọn variant
→ Add cart → Cart hiển thị variant
→ User checkout → chọn tỉnh/huyện/xã GHN → tính phí ship
→ Order PENDING
→ Staff CONFIRMED → PREPARING → READY
→ User theo dõi timeline
```

---

## Command kiểm tra

### Backend

```bash
cd Backend/FastGuy-FastFoodSite
mvn clean compile
```

### Frontend

```bash
cd Frontend
npm run build
```
