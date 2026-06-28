# WORK PLAN — FastGuy Comprehensive

## Mục tiêu

Hoàn thiện dự án FastGuy theo đúng thiết kế database mới. Chia làm 4 module chính:

| Module | Nhánh | Actor | Mô tả |
|---|---|---|---|
| User | `module/user` | Khách hàng | Checkout GHN, chọn tỉnh/huyện/xã, tính phí ship |
| Admin | `module/admin` | Quản trị viên | Dashboard hoàn chỉnh, quản lý hệ thống |
| Staff | `module/staff` | Nhân viên | Xử lý đơn, dashboard chart |
| Shipper | `module/shipper` | Tài xế giao hàng | Nhận đơn, giao hàng, mobile UI |

---

## Module User — Checkout GHN

### Yêu cầu

Checkout phải gửi GHN fields thay vì `zoneId` cứng. Mặc định chọn TP.HCM.

### API thay đổi

**POST /api/orders** — request body mới:

```json
{
  "address": "123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
  "phone": "0901000006",
  "deliveryNote": "",
  "paymentMethod": "COD",
  "ghnProvinceId": 202,
  "ghnDistrictId": 1442,
  "ghnWardCode": "20107",
  "toProvinceName": "TP. Hồ Chí Minh",
  "toDistrictName": "Quận 1",
  "toWardName": "Phường Bến Nghé",
  "shippingFee": 15000
}
```

### File cần sửa

| STT | File | Việc |
|---|---|---|
| 1 | `OrderServlet.java` | Đọc 8 field GHN từ body |
| 2 | `OrderService.java` | Sửa method `checkout`, set GHN fields vào Orders |
| 3 | `frontend/views/user/CheckoutPage.vue` | Hàm `placeOrder()` gửi GHN field, mặc định TP.HCM |
| 4 | `frontend/stores/order.js` | Đảm bảo `createOrder` pass toàn bộ data |

---

## Module Admin — Dashboard hoàn chỉnh + Quản lý

### Hiện tại đã có

Backend `AdminService.getDashboard()` ✅ trả về:
- `totalUsers`, `totalOrders`, `totalProducts`, `totalRevenue`
- `ordersByStatus`, `ordersToday`, `revenueToday`
- `revenueByMonth`, `topProducts`

Frontend `DashboardPage.vue` ✅ có:
- 6 stat cards (users, orders, products, revenue, ordersToday, revenueToday)
- 2 charts (revenue by month bar, top products horizontal bar)

### Cần bổ sung

#### Dashboard

- [ ] Thêm doughnut chart `ordersByStatus` (PENDING, CONFIRMED, PREPARING, READY, ...)
- [ ] Thêm stat card "Chờ xác nhận" (pendingOrders)
- [ ] Thêm stat card "Đơn hôm nay"

#### Product management

- [ ] Kiểm tra `ProductsPage.vue` CRUD variant hoạt động
- [ ] Kiểm tra upload Cloudinary

#### User management

- [ ] `UsersPage.vue` — danh sách user, role, trạng thái

#### UI improvements

- [ ] Đổi `grid-2` thành `grid-3` để có 3 chart cạnh nhau

### API

```http
GET /api/admin/dashboard
Response: { totalUsers, totalOrders, totalProducts, totalRevenue,
            ordersByStatus: { PENDING: 2, CONFIRMED: 1, ... },
            ordersToday, revenueToday, revenueByMonth: [...], topProducts: [...] }
```

---

## Module Staff — Đã hoàn thiện

### Hiện tại đã có

- [x] 4 tab: PENDING / CONFIRMED / PREPARING / READY
- [x] OrderDetail: nút chuyển trạng thái + cancel modal
- [x] Dashboard: doughnut chart + 4 stat cards
- [x] Đã bỏ lowStockIngredients

### Chỉ cần kiểm tra

- [ ] Tabs fetch đúng API, không 500
- [ ] Status buttons hoạt động đúng flow
- [ ] Items hiển thị variantName

---

## Module Shipper — Tài xế giao hàng

### Mô tả

Shipper là actor mới (role_id = 3). Sau khi Staff chuyển đơn sang READY, shipper nhận đơn và giao cho khách.

### Luồng

```text
READY → Shipper nhận đơn → PICKED_UP (đã lấy hàng) → DELIVERED (đã giao)
READY → CANCELLED (nếu shipper hủy)
```

### Backend — File cần tạo

| File | Mô tả |
|---|---|
| `servlet/ShipperServlet.java` | REST API cho shipper |
| `service/ShipperService.java` | Business logic |
| `entity/Orders.java` | Đã có sẵn shipper_id, picked_up_at, delivered_at |

### API Endpoints

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/shipper/orders` | Danh sách đơn READY để nhận |
| GET | `/api/shipper/orders/mine` | Đơn shipper đang giao |
| PUT | `/api/shipper/orders/{id}/pickup` | Đã lấy hàng → PICKED_UP |
| PUT | `/api/shipper/orders/{id}/deliver` | Đã giao → DELIVERED |
| GET | `/api/shipper/orders/history` | Lịch sử giao hàng |

### Backend — ShipperService.java (mẫu)

```
package service;

public class ShipperService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    // Lấy đơn READY chưa có shipper
    public List<Orders> getAvailableOrders() {
        return ordersDAO.findByStatus("READY");
    }

    // Lấy đơn của shipper
    public List<Orders> getMyOrders(int shipperId) {
        // Query orders where shipper_id = shipperId AND status IN ('PICKED_UP', 'READY')
    }

    // Nhận đơn - gán shipper_id, chuyển READY → PICKED_UP
    public boolean pickUpOrder(int orderId, int shipperId) {
        // Gán shipper_id, set picked_up_at, chuyển status
    }

    // Giao thành công - chuyển PICKED_UP → DELIVERED
    public boolean deliverOrder(int orderId) {
        // Set delivered_at, chuyển status
    }
}
```

### Frontend — Shipper mobile UI

Thiết kế mobile-first (responsive, hoặc có thể làm PWA).

#### File cần tạo

| File | Mô tả |
|---|---|
| `layouts/ShipperLayout.vue` | Layout mobile cho shipper |
| `views/shipper/DashboardPage.vue` | Trang chủ, danh sách đơn chờ |
| `views/shipper/MyOrdersPage.vue` | Đơn đang giao + lịch sử |
| `views/shipper/OrderDetailPage.vue` | Chi tiết đơn + nút pickup/deliver |
| `stores/shipper.js` | Store Pinia cho shipper |
| `api/shipper.js` | API client |
| `router/index.js` | Thêm route cho `/shipper/*` |

#### Giao diện mobile

- Tối ưu cho màn hình nhỏ (dưới 768px)
- Card thay vì table
- Nút lớn dễ bấm
- Tích hợp bản đồ (có thể dùng Google Maps hoặc Leaflet)

#### Dashboard shipper

- Số đơn chờ nhận hôm nay
- Số đơn đang giao
- Danh sách đơn READY (có địa chỉ, khoảng cách)
- Nút "Nhận đơn"

#### Order detail

- Thông tin khách hàng (tên, sđt, địa chỉ)
- Sản phẩm đã đặt (productName + variantName)
- Phí ship, tổng tiền
- Nút "Đã lấy hàng" (PICKED_UP)
- Nút "Đã giao thành công" (DELIVERED)
- Nút "Liên hệ khách" (gọi điện)

### Database

Các trường đã có sẵn:
- `Users` với `role_id = 3` (SHIPPER)
- `Orders.shipper_id` (FK → Users)
- `Orders.picked_up_at` (datetime)
- `Orders.delivered_at` (datetime)

Thêm status:
- `PICKED_UP` vào order_status (hiện đã có DELIVERING, có thể dùng DELIVERING hoặc thêm mới)

Seed data shipper:
```sql
-- Nếu chưa có shipper
insert into Users (role_id, email, phone, password_hash, full_name, status)
values (3, 'shipper1@fastguy.com', '0901000004', '123456', N'Phạm Văn C', 'ACTIVE');
```

---

## Timeline

| Giai đoạn | Nội dung | Thời gian |
|---|---|---|
| Phase 1 | GHN Checkout (User) | Ngày 1 |
| Phase 2 | Admin Dashboard hoàn chỉnh | Ngày 2 |
| Phase 3 | Shipper Backend API | Ngày 3 |
| Phase 4 | Shipper Frontend mobile | Ngày 4-5 |
| Phase 5 | Kiểm tra toàn bộ flow + Fix bug | Ngày 6 |

## Lệnh kiểm tra

```bash
# Backend
cd Backend/FastGuy-FastFoodSite && mvn clean compile

# Frontend
cd Frontend && npm run build
```
