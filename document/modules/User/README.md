# Module User

**Người phụ trách**: Người 3

## Mục tiêu

User quản lý địa chỉ, checkout bằng GHN, xem lịch sử đơn và theo dõi trạng thái từ Staff.

---

## Files

### Backend

- `servlet/UserServlet.java`
- `servlet/AddressServlet.java`
- `servlet/OrderServlet.java`
- `service/OrderService.java`
- `dao/AddressDAO.java`
- `dao/OrdersDAO.java`
- `dao/OrderItemDAO.java`
- `entity/Address.java`
- `entity/Orders.java`
- `entity/OrderItem.java`

### Frontend

- `views/user/ProfilePage.vue`
- `views/user/CheckoutPage.vue`
- `views/user/OrdersPage.vue`
- `views/user/OrderDetailPage.vue`
- `api/address.js`
- `api/order.js`

---

## Address mới cần hỗ trợ

```text
recipientName
phone
street
wardName
districtName
provinceName
ghnProvinceId
ghnDistrictId
ghnWardCode
zoneId nullable fallback
isDefault
```

---

## API Endpoints

| Method | Path | Mục tiêu |
|---|---|---|
| GET | `/api/user/addresses` | Lấy địa chỉ user |
| POST | `/api/user/addresses` | Thêm địa chỉ |
| PUT | `/api/user/addresses/{id}` | Sửa địa chỉ |
| DELETE | `/api/user/addresses/{id}` | Xóa địa chỉ |
| PUT | `/api/user/addresses/{id}/default` | Set mặc định |
| POST | `/api/orders` | Checkout |
| GET | `/api/orders` | Lịch sử đơn |
| GET | `/api/orders/{id}` | Chi tiết đơn |

---

## Việc cần làm

### Backend

- [ ] Cập nhật `AddressServlet` theo field GHN mới.
- [ ] Cập nhật `AddressDAO`.
- [ ] Cập nhật `OrderService.checkout()`:
  - Đọc cart item theo `variantId`.
  - Tạo `OrderItem` có `variantName`.
  - Lưu shipping fee thật.
  - Lưu GHN fields vào `Orders`.
- [ ] Cập nhật `OrderServlet.toDetail()` trả `variantName`.

### Frontend

- [ ] `ProfilePage.vue`: CRUD địa chỉ.
- [ ] `CheckoutPage.vue`: chọn province/district/ward.
- [ ] `CheckoutPage.vue`: gọi `/api/shipping/fee`.
- [ ] `CheckoutPage.vue`: hiển thị shipping fee.
- [ ] `OrdersPage.vue`: danh sách đơn.
- [ ] `OrderDetailPage.vue`: hiển thị product + variant + timeline.

---

## Checklist test

- [ ] Thêm địa chỉ mới.
- [ ] Sửa địa chỉ.
- [ ] Xóa địa chỉ.
- [ ] Set địa chỉ mặc định.
- [ ] Checkout tính phí GHN.
- [ ] Order lưu đúng địa chỉ snapshot.
- [ ] Order detail hiển thị variant.
- [ ] Timeline đổi theo Staff.

---

## Phụ thuộc

- **Auth**: cần user login.
- **Guest**: cần cart có variant.
- **Shipping**: cần API GHN fee.
- **Staff**: trạng thái đơn do Staff cập nhật.
