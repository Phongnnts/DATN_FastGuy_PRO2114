# Module User

**Mục tiêu**: Quản lý địa chỉ giao hàng (CRUD hoàn chỉnh), xem lịch sử đơn hàng + chi tiết đơn (theo dõi trạng thái từ Staff), checkout.

**Người phụ trách**: Người 3

---

## Files

### Backend
- `servlet/UserServlet.java`
- `servlet/AddressServlet.java` (❌ tạo mới)
- `dao/AddressDAO.java` (❌ tạo mới)

### Frontend
- `views/user/ProfilePage.vue`
- `views/user/OrdersPage.vue`
- `views/user/OrderDetailPage.vue`
- `views/user/CheckoutPage.vue`

---

## API Endpoints

| Method | Path | Trạng thái |
|--------|------|------------|
| GET | `/api/user/home` | ✅ Có |
| GET | `/api/user/addresses` | ❌ Thêm |
| POST | `/api/user/addresses` | ❌ Thêm |
| PUT | `/api/user/addresses/{id}` | ❌ Thêm |
| DELETE | `/api/user/addresses/{id}` | ❌ Thêm |
| PUT | `/api/user/addresses/{id}/default` | ❌ Thêm |

---

## Việc cần làm

- [ ] Backend: tạo `AddressDAO` (CRUD)
- [ ] Backend: tạo `AddressServlet` (5 endpoint)
- [ ] Frontend: `ProfilePage` — thêm form quản lý địa chỉ
- [ ] Frontend: `CheckoutPage` — load addresses từ API thay vì hardcode
- [ ] Frontend: `OrderDetailPage` — hiển thị trạng thái đơn theo luồng Staff

---

## Phụ thuộc

- **Auth**: Cần user login
- **Guest**: Cần có đơn (Orders) để xem lịch sử
- **Staff**: Cần Staff cập nhật trạng thái thì User mới thấy được
