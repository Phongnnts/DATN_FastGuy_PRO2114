# Module Staff

**Người phụ trách**: Người 4

## Mục tiêu

Staff xử lý đơn hàng đầy đủ flow:

```text
PENDING → CONFIRMED → PREPARING → READY
PENDING/CONFIRMED/PREPARING → CANCELLED
```

---

## Files

### Backend

- `servlet/StaffDashboardServlet.java`
- `servlet/StaffOrderServlet.java`
- `service/StaffOrderService.java`
- `dao/OrdersDAO.java`
- `dao/OrderItemDAO.java`

### Frontend

- `views/staff/DashboardPage.vue`
- `views/staff/OrdersPage.vue`
- `views/staff/OrderDetailPage.vue`
- `views/staff/OrderHistoryPage.vue`
- `stores/staff.js`
- `api/staff.js`

---

## API Endpoints

| Method | Path | Mục tiêu |
|---|---|---|
| GET | `/api/staff/dashboard` | Dashboard + ordersByStatus |
| GET | `/api/staff/orders` | PENDING |
| GET | `/api/staff/orders/confirmed` | CONFIRMED |
| GET | `/api/staff/orders/preparing` | PREPARING |
| GET | `/api/staff/orders/ready` | READY |
| GET | `/api/staff/orders/history` | READY/CANCELLED history |
| GET | `/api/staff/orders/{id}` | Chi tiết đơn |
| PUT | `/api/staff/orders/{id}/status` | Chuyển trạng thái |

---

## Response order item cần có

```json
{
  "productId": 1,
  "variantId": 2,
  "productName": "Classic Beef Burger",
  "variantName": "Size L",
  "quantity": 1,
  "unitPrice": 55000,
  "totalPrice": 55000
}
```

---

## Việc cần làm

### Backend

- [ ] `StaffOrderService.updateStatus()` validate đúng flow.
- [ ] Ghi `confirmedAt` khi CONFIRMED.
- [ ] Ghi `readyAt` khi READY.
- [ ] Ghi `cancelledAt` + `failureReason` khi CANCELLED.
- [ ] `StaffOrderServlet.toDetail()` trả `variantName`.
- [ ] Null-safe với guest order `user_id = null`.
- [ ] Không để `OrdersDAO.save()` nuốt lỗi âm thầm.

### Frontend

- [ ] `OrdersPage.vue`: tabs không race condition.
- [ ] `OrdersPage.vue`: PENDING/CONFIRMED/PREPARING/READY đều fetch đúng.
- [ ] `OrderDetailPage.vue`: hiển thị product + variant.
- [ ] `OrderDetailPage.vue`: nút status đúng trạng thái hiện tại.
- [ ] `OrderDetailPage.vue`: cancel modal có lý do.
- [ ] `DashboardPage.vue`: chart status đúng màu.

---

## Checklist test

- [ ] PENDING tab có đơn mới.
- [ ] Xác nhận đơn chuyển sang CONFIRMED.
- [ ] Bắt đầu chế biến chuyển sang PREPARING.
- [ ] Hoàn thành chuyển sang READY.
- [ ] Hủy đơn lưu lý do.
- [ ] User timeline cập nhật.
- [ ] Không còn 500 ở staff tabs.

---

## Phụ thuộc

- **User/Guest**: cần đơn hàng được checkout.
- **Database**: OrderItem có `variant_id`, `variant_name`.
