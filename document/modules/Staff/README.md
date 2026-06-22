# Module Staff

**Mục tiêu**: Xử lý đơn hàng đầy đủ luồng PENDING → CONFIRMED → PREPARING → READY (bàn giao giao hàng). Dashboard Chart.js 4 màu.

**Người phụ trách**: Người 4 — **Module quan trọng nhất**

---

## Files

### Backend
- `servlet/StaffDashboardServlet.java`
- `servlet/StaffOrderServlet.java`
- `service/StaffService.java`
- `service/StaffOrderService.java`

### Frontend
- `views/staff/DashboardPage.vue` (doughnut chart 4 màu)
- `views/staff/OrdersPage.vue` (tab PENDING / CONFIRMED / PREPARING / READY)
- `views/staff/OrderDetailPage.vue` (3 nút: Xác nhận / Chế biến / Hoàn thành)
- `views/staff/OrderHistoryPage.vue`

---

## Luồng trạng thái

```
PENDING ──→ CONFIRMED ──→ PREPARING ──→ READY
PENDING ──→ CANCELLED
```

| Trạng thái | Staff làm gì? | Ghi timestamp |
|------------|---------------|---------------|
| PENDING | Xem thông tin đơn | — |
| CONFIRMED | Bấm "Xác nhận" | `confirmed_at` |
| PREPARING | Bấm "Bắt đầu chế biến" | — |
| READY | Bấm "Hoàn thành" | `ready_at` |
| CANCELLED | Bấm "Hủy đơn" + lý do | `cancelled_at` + `failure_reason` |

---

## API Endpoints

| Method | Path | Trạng thái |
|--------|------|------------|
| GET | `/api/staff/dashboard` (kèm ordersByStatus) | ✅ Có |
| GET | `/api/staff/orders/` | ✅ Có |
| GET | `/api/staff/orders/confirmed` | ✅ Có |
| GET | `/api/staff/orders/history` | ✅ Có |
| GET | `/api/staff/orders/{id}` | ✅ Có |
| PUT | `/api/staff/orders/{id}/status` (cho phép CONFIRMED→PREPARING→READY) | ❌ Sửa |

---

## Dashboard Chart.js — 4 màu

| Trạng thái | Màu |
|------------|-----|
| PENDING | Vàng `#F59E0B` |
| CONFIRMED | Xanh dương `#3B82F6` |
| PREPARING | Tím `#8B5CF6` |
| READY | Xanh lá `#10B981` |

---

## Giao diện OrderDetailPage

| Button | Hiện khi | Chuyển thành |
|--------|----------|--------------|
| "Xác nhận" | PENDING | CONFIRMED |
| "Bắt đầu chế biến" | CONFIRMED | PREPARING |
| "Hoàn thành" | PREPARING | READY |
| "Hủy đơn" | Luôn (trừ READY) | CANCELLED + modal lý do |

---

## Việc cần làm

- [ ] Backend: sửa `StaffOrderService.updateStatus()` cho phép CONFIRMED→PREPARING→READY
- [ ] Backend: thêm ghi `confirmed_at`, `ready_at`, `cancelled_at`
- [ ] Frontend: `OrderDetailPage` — thêm 3 button (Xác nhận / Chế biến / Hoàn thành)
- [ ] Frontend: `OrderDetailPage` — button "Hủy đơn" + modal nhập failure_reason
- [ ] Frontend: `OrdersPage` — thêm tab PREPARING + READY
- [ ] Frontend: kiểm tra doughnut chart hiển thị đúng 4 màu

---

## Phụ thuộc

- **Auth**: Cần đăng nhập
- **Guest**: Cần có đơn PENDING từ khách
- **User**: User cần thấy trạng thái Staff cập nhật
