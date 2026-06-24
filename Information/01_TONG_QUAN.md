# FastGuy — Tổng quan dự án

## 1. Giới thiệu

FastGuy là ứng dụng web đặt đồ ăn nhanh trực tuyến.
Khách hàng có thể xem thực đơn, đặt món, theo dõi đơn hàng.
Staff xác nhận và quản lý đơn hàng, nguyên liệu, ca làm việc.
Admin quản lý toàn bộ hệ thống (sản phẩm, danh mục, người dùng, doanh thu).

---

## 2. Công nghệ sử dụng (Tech Stack)

### Backend

| Công nghệ | Mục đích |
|-----------|----------|
| Java 17 | Ngôn ngữ lập trình |
| Jakarta EE (Servlet 6) | Xử lý HTTP request/response |
| Hibernate (JPA) | Kết nối và truy vấn database |
| SQL Server | Lưu trữ dữ liệu |
| JWT (jjwt) | Xác thực người dùng |
| Jackson | Đọc/ghi JSON |
| Tomcat 10 | Chạy ứng dụng backend |

### Frontend

| Công nghệ | Mục đích |
|-----------|----------|
| Vue 3 (Composition API) | Framework giao diện |
| Vite 8 | Build tool, dev server |
| Pinia 3 | Quản lý state |
| Vue Router 4 | Điều hướng trang |
| Axios | Gọi API backend |
| Chart.js | Vẽ biểu đồ báo cáo |
| CSS thuần (variables) | Tạo giao diện |
| Bootstrap Icons | Icon |

---

## 3. Các role và tính năng

### Guest (Khách chưa đăng nhập)

- Xem trang chủ
- Xem thực đơn, tìm kiếm sản phẩm
- Xem chi tiết sản phẩm
- Thêm sản phẩm vào giỏ hàng
- Đăng nhập / Đăng ký
- Tra cứu đơn hàng

### User (Khách đã đăng nhập)

- Tất cả quyền Guest
- Đặt hàng, thanh toán
- Xem lịch sử đơn hàng
- Quản lý thông tin cá nhân
- Đánh giá sản phẩm
- Yêu thích sản phẩm
- Đổi mật khẩu

### Staff (Nhân viên)

- Dashboard tổng quan
- Xem và xác nhận đơn hàng
- Hủy đơn hàng
- Quản lý nguyên liệu, tồn kho
- Quản lý ca làm việc
- Xem lịch sử đơn hàng

### Admin (Quản trị viên)

- Dashboard tổng quan
- Quản lý người dùng (xem danh sách)
- Quản lý sản phẩm (CRUD)
- Quản lý danh mục (CRUD)
- Quản lý khu vực giao hàng (xem danh sách)
- Quản lý nguyên liệu
- Quản lý đơn hàng
- Quản lý ca làm việc, phân ca
- Xem báo cáo doanh thu
- Xem báo cáo sản phẩm bán chạy

---

## 4. Danh sách màn hình (UI)

### Guest Layout

| URL | Màn hình |
|-----|----------|
| `/` | Trang chủ — Hero banner, danh mục, sản phẩm nổi bật |
| `/menu` | Thực đơn — danh sách sản phẩm + filter danh mục |
| `/product/:id` | Chi tiết sản phẩm |
| `/cart` | Giỏ hàng |
| `/login` | Đăng nhập |
| `/register` | Đăng ký |
| `/track-order` | Tra cứu đơn hàng |
| `/forgot-password` | Quên mật khẩu |

### User Layout

| URL | Màn hình |
|-----|----------|
| `/account/profile` | Thông tin cá nhân |
| `/account/orders` | Lịch sử đơn hàng |
| `/account/orders/:id` | Chi tiết đơn hàng |
| `/account/orders/:id/review` | Đánh giá sản phẩm |
| `/account/change-password` | Đổi mật khẩu |
| `/account/favorites` | Sản phẩm yêu thích |
| `/checkout` | Thanh toán |

### Staff Layout

| URL | Màn hình |
|-----|----------|
| `/staff` | Dashboard — thống kê đơn hàng |
| `/staff/orders` | Danh sách đơn hàng cần xử lý |
| `/staff/orders/:id` | Chi tiết đơn hàng |
| `/staff/orders/history` | Lịch sử đơn hàng |
| `/staff/ingredients` | Quản lý nguyên liệu |
| `/staff/ingredients/low-stock` | Nguyên liệu sắp hết |
| `/staff/shifts` | Ca làm việc |

### Admin Layout

| URL | Màn hình |
|-----|----------|
| `/admin` | Dashboard — thống kê tổng quan |
| `/admin/users` | Danh sách người dùng |
| `/admin/products` | Quản lý sản phẩm |
| `/admin/categories` | Quản lý danh mục |
| `/admin/delivery-zones` | Khu vực giao hàng |
| `/admin/ingredients` | Quản lý nguyên liệu |
| `/admin/orders` | Quản lý đơn hàng |
| `/admin/shifts` | Quản lý ca làm việc |
| `/admin/reports/revenue` | Báo cáo doanh thu |
| `/admin/reports/top-products` | Sản phẩm bán chạy |

---

## 5. Kiến trúc tổng quan

```
┌─────────────────────────────────────┐
│         Frontend (Vue 3)            │
│  Port 5173 (dev)                    │
│         ↓ API proxy                 │
├─────────────────────────────────────┤
│         Backend (Java + Tomcat)     │
│  Port 8082 /FastGuy                 │
│         ↓ JPA / Hibernate           │
├─────────────────────────────────────┤
│         Database (SQL Server)       │
│  FastGuyDB — 17 tables              │
└─────────────────────────────────────┘
```

### Frontend → Backend

- Frontend proxy: `/api` → `http://localhost:8082/FastGuy`
- Giao tiếp qua JSON
- Auth bằng JWT token (Bearer)

---

## 6. Database: 17 tables

| Bảng | Chức năng |
|------|-----------|
| Role | Vai trò (USER, STAFF, ADMIN) |
| Users | Người dùng |
| Address | Địa chỉ giao hàng |
| DeliveryZone | Khu vực giao hàng + phí ship |
| Category | Danh mục sản phẩm |
| Product | Sản phẩm |
| ProductOption | Tùy chọn sản phẩm (size, topping) |
| Cart | Giỏ hàng |
| CartItem | Sản phẩm trong giỏ |
| Orders | Đơn hàng |
| OrderItem | Sản phẩm trong đơn |
| Payment | Thanh toán |
| Ingredient | Nguyên liệu |
| ProductIngredient | Nguyên liệu của sản phẩm |
| WorkShift | Ca làm việc |
| Schedule | Lịch phân ca |
| Review | Đánh giá sản phẩm |
