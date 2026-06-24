# Module Shipping / GHN

**Người phụ trách**: Người 6

## Mục tiêu

Tích hợp GHN để tính phí vận chuyển trong checkout. Chỉ cần tính phí, chưa cần tạo đơn GHN thật.

---

## Quyết định thiết kế

- GHN là nguồn chính tính phí ship.
- `DeliveryZone` giữ làm fallback khi GHN lỗi hoặc chưa cấu hình token.
- Không hardcode GHN token/shopId trong source code.
- Checkout lưu snapshot phí ship và địa chỉ vào `Orders`.

---

## GHN API cần dùng

### Lấy tỉnh/thành

```http
GET /api/shipping/provinces
```

### Lấy quận/huyện

```http
GET /api/shipping/districts?provinceId=202
```

### Lấy phường/xã

```http
GET /api/shipping/wards?districtId=1442
```

### Tính phí ship

```http
POST /api/shipping/fee
```

Request:

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

## Backend files cần tạo

```text
servlet/ShippingServlet.java
service/ShippingService.java
utils/GhnClient.java
```

Có thể thêm config:

```text
utils/AppConfig.java
```

---

## Config cần có

```text
GHN_TOKEN
GHN_SHOP_ID
GHN_HOST=https://dev-online-gateway.ghn.vn
GHN_FROM_DISTRICT_ID
GHN_FROM_WARD_CODE
```

Nguồn config đề xuất:

1. Environment variables.
2. `persistence.xml` property hoặc config file local không commit.
3. Fallback dev values nếu chưa có.

Không commit token thật.

---

## Flow checkout

```text
User mở CheckoutPage
→ Frontend gọi /api/shipping/provinces
→ User chọn province
→ Frontend gọi /api/shipping/districts
→ User chọn district
→ Frontend gọi /api/shipping/wards
→ User chọn ward
→ Frontend gọi /api/shipping/fee
→ Backend gọi GHN
→ Backend trả shippingFee
→ User đặt hàng
→ Orders lưu shippingFee + GHN fields
```

---

## Fallback DeliveryZone

Nếu GHN lỗi:

```text
GHN timeout
GHN token thiếu
GHN service unavailable
Không tìm được service
```

Backend dùng fallback:

```text
DeliveryZone.shipping_fee
```

Response fallback:

```json
{
  "shippingProvider": "FALLBACK_ZONE",
  "shippingFee": 15000,
  "serviceId": null,
  "serviceTypeId": null,
  "expectedDeliveryTime": null
}
```

---

## Việc cần làm

### Backend

- [ ] Tạo `GhnClient` gọi API GHN.
- [ ] Tạo `ShippingService` xử lý business logic.
- [ ] Tạo `ShippingServlet` expose API.
- [ ] Implement provinces.
- [ ] Implement districts.
- [ ] Implement wards.
- [ ] Implement calculate fee.
- [ ] Implement fallback DeliveryZone.
- [ ] Chuẩn hóa error response.

### Frontend phối hợp User module

- [ ] Checkout gọi province API.
- [ ] Checkout gọi district API.
- [ ] Checkout gọi ward API.
- [ ] Checkout gọi fee API.
- [ ] Hiển thị loading/error khi tính phí.
- [ ] Disable đặt hàng nếu chưa có phí ship hợp lệ.

---

## Checklist test

- [ ] Lấy province thành công.
- [ ] Lấy district theo province thành công.
- [ ] Lấy ward theo district thành công.
- [ ] Tính phí GHN thành công.
- [ ] GHN lỗi thì fallback zone.
- [ ] Checkout lưu đúng shipping fee.
- [ ] Orders lưu đúng GHN fields.

---

## Không làm trong giai đoạn này

- Không tạo đơn GHN thật.
- Không tracking GHN.
- Không hủy đơn GHN.
- Không in vận đơn GHN.

Các phần này để phase sau nếu còn thời gian.
