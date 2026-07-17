# Module User

**Nhánh git:** `module/user`
**Người phụ trách:** Người 3

---

## Trạng thái

**Backend Address + Order đã xong entity/DAO/service/servlet.** Cần sửa frontend.

---

## API Address

### GET /api/user/addresses

```json
{
  "success": true,
  "data": [
    {
      "addressId": 1,
      "recipientName": "Hoàng Văn E",
      "phone": "0901000006",
      "street": "123 Lê Lợi",
      "wardName": "Phường Bến Nghé",
      "districtName": "Quận 1",
      "provinceName": "TP. Hồ Chí Minh",
      "ghnProvinceId": null,
      "ghnDistrictId": null,
      "ghnWardCode": null,
      "city": "TP. Hồ Chí Minh",
      "isDefault": true,
      "createdAt": "...",
      "zone": { "zoneId": 1, "districtName": "Quận 1", "shippingFee": 15000 }
    }
  ]
}
```

### POST /api/user/addresses

```json
Request: { "recipientName": "...", "phone": "...", "street": "...", "wardName": "...", "districtName": "...", "provinceName": "...", "ghnProvinceId": null, "ghnDistrictId": null, "ghnWardCode": null, "isDefault": true }
Response: { "success": true, "data": { "addressId": 2, ... } }
```

### PUT /api/user/addresses/{id}
Body tương tự POST. Không gửi `ward` nữa — gửi `wardName`.

### PUT /api/user/addresses/{id}/default
Không cần body.

### DELETE /api/user/addresses/{id}

---

## API Order

### POST /api/orders (checkout)

```json
Request: { "zoneId": 1, "address": "123 Lê Lợi, Phường Bến Nghé, Quận 1", "phone": "0901000006", "deliveryNote": "", "paymentMethod": "COD" }
Response: { "success": true, "data": { "orderId": 5, "orderCode": "ORD-ABC12345", "status": "PENDING", "finalAmount": 60000 } }
```

(Lưu ý: sau Phase 4 sẽ thêm GHN fields. Hiện tại zoneId fallback.)

### GET /api/orders

```json
Response: { "success": true, "data": [ { "orderId": 5, "orderCode": "ORD-ABC12345", "status": "PENDING", "finalAmount": 60000, "createdAt": "..." } ] }
```

### GET /api/orders/{id}

```json
Response: { "success": true, "data": { "orderId": 5, "orderCode": "...", "status": "PENDING", "items": [ { "productId": 1, "variantId": 2, "productName": "Classic Beef Burger", "variantName": "Size L", "quantity": 1, "unitPrice": 55000, "totalPrice": 55000, "image": "" } ], "statusHistory": [ { "status": "PENDING", "time": "...", "note": "" } ] } }
```

---

## File cần sửa

| File | Việc |
|---|---|
| `frontend/src/views/user/ProfilePage.vue` | Form address: ward → wardName, thêm districtName/provinceName/GHN fields |
| `frontend/src/views/user/CheckoutPage.vue` | Load address + cart variant + fallback shipping + checkout |
| `frontend/src/views/user/OrdersPage.vue` | List order, fetch từ API |
| `frontend/src/views/user/OrderDetailPage.vue` | Hiển thị variantName, shipping fee, timeline |
| `frontend/src/api/address.js` | Thêm field mới |

---

## AI Prompt 1 — ProfilePage.vue (phần address)

```
Sửa phần quản lý địa chỉ trong `frontend/src/views/user/ProfilePage.vue`.

Backend Address mới dùng các field:
- recipientName, phone, street
- wardName (thay vì ward)
- districtName (mới)
- provinceName (mới)
- ghnProvinceId, ghnDistrictId, ghnWardCode (mới, nullable)
- isDefault

Yêu cầu:
1. Form thêm/sửa address gồm: recipientName, phone, street, wardName, districtName, provinceName
2. KHÔNG gửi `ward` — gửi `wardName`
3. isDefault là checkbox
4. (Tạm thời) ghnProvinceId/ghnDistrictId/ghnWardCode có thể bỏ qua hoặc để null
5. Load danh sách address từ GET /api/user/addresses
6. Mỗi address hiển thị: recipientName, phone, street, wardName, districtName, provinceName
```

---

## AI Prompt 2 — CheckoutPage.vue

```
Sửa `frontend/src/views/user/CheckoutPage.vue`.

Backend:
- Cart API trả items có variantName (không optionData)
- POST /api/orders nhận: zoneId, address, phone, deliveryNote, paymentMethod
- GET /api/user/addresses trả address mới (wardName, districtName, provinceName)

Yêu cầu:
1. Load addresses từ API → user chọn hoặc thêm mới
2. Hiển thị cart items với variantName (ví dụ: "Classic Beef Burger - Size L")
3. Tạm thời dùng DeliveryZone tính shipping (zoneId)
4. Hiển thị tổng: tạm tính + phí ship = tổng cộng
5. Nút Đặt hàng → POST /api/orders
6. Sau khi thành công → redirect /user/orders/{orderId}
```

---

## AI Prompt 3 — OrderDetailPage.vue

```
Sửa `frontend/src/views/user/OrderDetailPage.vue`.

API trả:
```
{
  "orderId": 5,
  "orderCode": "ORD-ABC12345",
  "status": "PENDING",
  "items": [
    {
      "productId": 1,
      "variantId": 2,
      "productName": "Classic Beef Burger",
      "variantName": "Size L",
      "quantity": 1,
      "unitPrice": 55000,
      "totalPrice": 55000,
      "image": ""
    }
  ],
  "statusHistory": [
    { "status": "PENDING", "time": "...", "note": "" },
    { "status": "CONFIRMED", "time": "...", "note": "" }
  ],
  "shippingFee": 15000
}
```

Yêu cầu:
1. Hiển thị thông tin đơn: orderCode, trạng thái, ngày đặt
2. Items: hiển thị productName + variantName + đơn giá + số lượng + thành tiền
3. Shipping fee
4. Timeline (OrderTimeline component từ common)
5. (Nếu status DELIVERED) hiển thị nút "Đánh giá"
```

---

## Checklist test

- [ ] Thêm/sửa/xóa address thành công
- [ ] Address hiển thị wardName/districtName/provinceName
- [ ] Checkout load cart items có variantName
- [ ] Checkout tạo đơn thành công
- [ ] Order item có variantName
- [ ] Order detail hiển thị variant
- [ ] Timeline trạng thái
