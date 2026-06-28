# WORK PLAN — FastGuy (Hoàn thiện)

## Mục tiêu

Hoàn thiện 2 module:

1. **Checkout GHN** — Chọn tỉnh → huyện → xã, mặc định TP.HCM, tính phí ship, lưu GHN fields
2. **Admin Dashboard** — Thêm doughnut chart, hoàn chỉnh thống kê
3. **Staff Dashboard** — Giữ nguyên, không cần sửa

---

## Phân công theo nhánh

| Nhánh | Người | Việc |
|---|---|---|
| `module/user` | Thành viên 1 | Checkout GHN |
| `module/admin` | Thành viên 2 | Admin Dashboard |
| `module/staff` | — | Giữ nguyên |

---

## Nhánh `module/user` — Checkout GHN

### 4 file cần sửa

| File | Việc |
|---|---|
| `Backend/.../servlet/OrderServlet.java` | +20 dòng đọc GHN fields từ request |
| `Backend/.../service/OrderService.java` | +15 dòng tham số + set GHN vào Orders |
| `frontend/src/views/user/CheckoutPage.vue` | +10 dòng sửa placeOrder, set mặc định TP.HCM |
| `frontend/src/stores/order.js` | Kiểm tra, đảm bảo createOrder pass toàn bộ data |

### Chi tiết

#### 1. OrderServlet.java — doPost

Đọc thêm 6 field GHN từ body request:

```
ghnProvinceId (int, optional)
ghnDistrictId (int, optional)
ghnWardCode (string, optional)
toProvinceName (string, optional)
toDistrictName (string, optional)
toWardName (string, optional)
shippingFee (decimal, optional, mặc định 0)
```

Sau đó truyền vào `orderService.checkout(...)` cùng 8 tham số mới.

#### 2. OrderService.java — checkout()

Sửa method signature — thêm 8 tham số:

```
Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
String toProvinceName, String toDistrictName, String toWardName,
BigDecimal shippingFee
```

Bỏ dòng lấy `DeliveryZone` và `deliveryZoneDAO`. Set 8 field GHN vào Orders entity.

#### 3. stores/order.js

Kiểm tra hàm `createOrder(data)` — đảm bảo gửi toàn bộ data xuống backend API.

#### 4. CheckoutPage.vue — placeOrder()

Sửa hàm `placeOrder()`:

- Bỏ gửi `zoneId: 1` cứng
- Gửi các field GHN:
  - `ghnProvinceId`, `ghnDistrictId`, `ghnWardCode`
  - `toProvinceName`, `toDistrictName`, `toWardName`
  - `shippingFee`

Sau khi load provinces, tự động chọn TP.HCM làm mặc định:

```
Tìm province có tên chứa "Hồ Chí Minh"
Nếu tìm thấy → set selectedProvince = id của nó
```

### Kiểm tra

```bash
# Backend
cd Backend/FastGuy-FastFoodSite && mvn clean compile

# Frontend
cd Frontend && npm run build
```

---

## Nhánh `module/admin` — Admin Dashboard

### 1 file cần sửa

| File | Việc |
|---|---|
| `frontend/src/views/admin/DashboardPage.vue` | +40 dòng thêm doughnut chart |

### Chi tiết

Thêm doughnut chart hiển thị phân bố đơn hàng theo trạng thái (ordersByStatus).

Backend `AdminService.getDashboard()` đã trả về `ordersByStatus` — không cần sửa backend.

#### Labels + Màu

```
PENDING → 'Chờ xác nhận' → #F59E0B
CONFIRMED → 'Đã xác nhận' → #3B82F6
PREPARING → 'Đang chế biến' → #8B5CF6
READY → 'Đã sẵn sàng' → #10B981
DELIVERING → 'Đang giao' → #06B6D4
DELIVERED → 'Đã giao' → #22C55E
CANCELLED → 'Đã hủy' → #EF4444
```

#### Chart config

- Type: `doughnut`
- Data: labels + values từ `ordersByStatus`, filter chỉ lấy status có count > 0
- Options: responsive, legend ở bottom

#### Template

Đổi `grid-2` thành `grid-3`. Thêm canvas ref cho status chart.

### Kiểm tra

```bash
cd Frontend && npm run build
```

---

## Kiểm tra tổng thể

```bash
# Backend
cd Backend/FastGuy-FastFoodSite && mvn clean compile

# Frontend
cd Frontend && npm run build
```
