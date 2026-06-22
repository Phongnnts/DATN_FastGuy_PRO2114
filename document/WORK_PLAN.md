# WORK PLAN — FastGuy

## Phân công

| Người | Module | Nhiệm vụ chính |
|-------|--------|----------------|
| **1** | Auth | Login/register, xem/sửa profile API |
| **2** | Guest | Menu, ProductDetail (options + gallery), Cart, Checkout |
| **3** | User | CRUD address, xem lịch sử đơn + trạng thái |
| **4** | Staff | Xử lý đơn PENDING→CONFIRMED→PREPARING→READY |
| **5** | Admin | CRUD đầy đủ + upload Cloudinary + dashboard chart |
| **6** | Common | Layouts, constants, Chart.js, Cloudinary config |

---

## Thứ tự theo tuần

### Tuần 1: Nền tảng

| # | Người | Việc |
|---|-------|------|
| 1 | **Người 1** | Thêm `POST /api/auth/logout` |
| 2 | **Người 1** | Thêm `GET /api/auth/profile` |
| 3 | **Người 1** | Thêm `PUT /api/auth/profile` |
| 4 | **Người 6** | Thêm Cloudinary config vào `constants.js` |
| 5 | **Người 6** | Kiểm tra Chart.js admin + staff dashboard |
| 6 | **Người 6** | Rà soát layouts + sidebar links |

### Tuần 2: Guest + Admin

| # | Người | Việc |
|---|-------|------|
| 7 | **Người 2** | Sửa `ProductServlet` → wildcard `/api/products/*` |
| 8 | **Người 2** | Thêm handler `GET /{id}` kèm options + image |
| 9 | **Người 2** | Frontend: `ProductDetailPage` — radio Size/Combo |
| 10 | **Người 2** | Frontend: `ProductDetailPage` — gallery Cloudinary |
| 11 | **Người 2** | Frontend: `CartPage` — hiển thị option đã chọn |
| 12 | **Người 5** | Sửa `Product.java` thêm field `galleryImages` |
| 13 | **Người 5** | Frontend: `ProductsPage` — upload Cloudinary |
| 14 | **Người 5** | Kiểm tra DashboardPage chart |

### Tuần 3: Staff + User (luồng chính)

| # | Người | Việc |
|---|-------|------|
| 15 | **Người 3** | Tạo `AddressDAO` + `AddressServlet` |
| 16 | **Người 3** | Frontend: `ProfilePage` — form quản lý address |
| 17 | **Người 3** | Frontend: `CheckoutPage` — load address từ API |
| 18 | **Người 4** | Sửa `StaffOrderService` — thêm PREPARING, READY |
| 19 | **Người 4** | Frontend: `OrderDetailPage` — 3 button + modal hủy |
| 20 | **Người 4** | Frontend: `OrdersPage` — thêm tab PREPARING + READY |
| 21 | **Người 4** | Kiểm tra doughnut chart 4 màu |

### Tuần 4: Tích hợp

| # | Người | Việc |
|---|-------|------|
| 22 | **Tất cả** | Chạy thử toàn bộ luồng: Guest → Order → Staff → Done |
| 23 | **Tất cả** | Fix bug (404, 500, hiển thị sai) |
| 24 | **Người 6** | Fix layout + responsive |
| 25 | **Tất cả** | Demo nội bộ |

---

## Luồng end-to-end

```
Guest:  Menu → Chọn SP + Size/Combo → Giỏ hàng → Checkout → PENDING
Staff:  PENDING → Xác nhận (CONFIRMED) → Chế biến (PREPARING) → Hoàn thành (READY)
User:   Xem lịch sử đơn → Theo dõi trạng thái (PENDING/CONFIRMED/PREPARING/READY)
Admin:  Dashboard → CRUD Users/Products/Categories/Zones → Upload ảnh
```

---

## Ghi chú

- Backend: Tomcat 8082, context path `/FastGuy`
- Frontend: Vite 5173, proxy `/api` → backend
- Upload ảnh: trực tiếp từ frontend lên Cloudinary (không cần backend)
- Bỏ qua: ChangePassword, ForgotPassword, Review, Favorites, Shifts, Ingredients, Reports
