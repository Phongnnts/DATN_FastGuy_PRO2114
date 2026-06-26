# Module Staff

**Nhánh git:** `module/staff`
**Người phụ trách:** Người 4

---

## Trạng thái

**Backend đã xong hoàn toàn.** Cần sửa frontend.

---

## API Endpoints

| Method | Path | Trả về |
|---|---|---|
| GET | `/api/staff/orders` | Danh sách PENDING |
| GET | `/api/staff/orders/confirmed` | Danh sách CONFIRMED |
| GET | `/api/staff/orders/preparing` | Danh sách PREPARING |
| GET | `/api/staff/orders/ready` | Danh sách READY |
| GET | `/api/staff/orders/history` | READY + CANCELLED |
| GET | `/api/staff/orders/{id}` | Chi tiết đơn |
| PUT | `/api/staff/orders/{id}/status` | Body: `{ "status": "CONFIRMED" }` |

### Response order detail items

```json
{
  "productId": 1,
  "variantId": 2,
  "variantName": "Size L",
  "productName": "Classic Beef Burger",
  "quantity": 1,
  "unitPrice": 55000,
  "totalPrice": 55000,
  "imageUrl": ""
}
```

---

## Luồng trạng thái

```text
PENDING ──→ CONFIRMED ──→ PREPARING ──→ READY
PENDING/CONFIRMED/PREPARING ──→ CANCELLED
```

---

## File cần sửa

| File | Việc |
|---|---|
| `frontend/src/stores/staff.js` | `mapOrder()` thêm variantId/variantName vào items |
| `frontend/src/views/staff/OrdersPage.vue` | Fix tabs, fetch đúng API, xử lý lỗi |
| `frontend/src/views/staff/OrderDetailPage.vue` | Hiển thị variantName, nút status, cancel modal |
| `frontend/src/views/staff/DashboardPage.vue` | Đã cleanup, chart status |

---

## AI Prompt 1 — stores/staff.js

```
Sửa `frontend/src/stores/staff.js`.

Yêu cầu:
1. Trong `mapOrder()`: items mapping thêm `variantId`, `variantName`
```
items: (o.items || []).map((i) => ({
  productId: i.productId,
  variantId: i.variantId || null,
  productName: i.productName,
  variantName: i.variantName || '',
  quantity: i.quantity,
  price: typeof i.unitPrice === 'string' ? parseFloat(i.unitPrice) : i.unitPrice || 0,
  totalPrice: typeof i.totalPrice === 'string' ? parseFloat(i.totalPrice) : i.totalPrice || 0,
  image: i.imageUrl || '',
}))
```
2. `mapOrderListItem()`: giữ nguyên, không cần variant
3. `updateOrderStatus()`: giữ nguyên, backend đã xử lý flow
```

---

## AI Prompt 2 — OrdersPage.vue

```
Sửa `frontend/src/views/staff/OrdersPage.vue`.

Yêu cầu:
1. Mỗi tab gọi API riêng:
   - PENDING → `staffStore.fetchOrders()`
   - CONFIRMED → `staffStore.fetchConfirmedOrders()`
   - PREPARING → `staffStore.fetchPreparingOrders()`
   - READY → `staffStore.fetchReadyOrders()`
2. Khi đổi tab, `switchTab()` set activeTab trước, rồi gọi fetch
3. Nếu fetch lỗi (catch), hiển thị alert hoặc thông báo "Lỗi tải dữ liệu"
4. `filteredOrders` filter theo activeTab (đề phòng)
5. Bảng hiển thị: mã đơn, khách hàng, số món, tổng tiền, ngày đặt, trạng thái
6. Click vào dòng → `/staff/orders/{id}`
```

---

## AI Prompt 3 — OrderDetailPage.vue

```
Sửa `frontend/src/views/staff/OrderDetailPage.vue`.

Yêu cầu:
1. Load order từ `staffStore.fetchOrderById(route.params.id)`
2. Hiển thị thông tin: orderCode, khách hàng, địa chỉ, ghi chú
3. Items: hiển thị productName + variantName + đơn giá + số lượng + thành tiền
4. Nút chuyển trạng thái:
   - PENDING → "Xác nhận" → status CONFIRMED
   - CONFIRMED → "Bắt đầu chế biến" → status PREPARING
   - PREPARING → "Hoàn thành" → status READY
   - READY → không có nút, hiển thị "Đã sẵn sàng"
   - Luôn có nút "Hủy đơn" (trừ khi READY)
5. Cancel modal: nhập lý do → gọi updateOrderStatus với failureReason
6. Khi chuyển trạng thái thành công: reload order
7. Timeline (OrderTimeline component)
```

---

## Checklist test

- [ ] PENDING tab có đơn mới
- [ ] Confirm → đơn qua CONFIRMED tab
- [ ] Start preparing → đơn qua PREPARING tab
- [ ] Complete → đơn qua READY tab
- [ ] Cancel đơn → lưu lý do
- [ ] Order detail hiển thị variantName
- [ ] User timeline cập nhật
- [ ] Không còn 500 ở các tab
