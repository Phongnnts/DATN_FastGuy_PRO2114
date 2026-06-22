# FastGuy — Kịch bản demo (dành cho buổi họp online)

## Trước khi demo

Đảm bảo các service đang chạy:

| Service | Trạng thái |
|---------|-----------|
| SQL Server + database FastGuyDB | Đang chạy |
| Backend Tomcat (port 8082) | Đang chạy |
| Frontend Vite (port 5173) | Đang chạy |

Tài khoản demo:

| Role | Email | Mật khẩu |
|------|-------|----------|
| Admin | admin@fastguy.com | 123456 |
| Staff | staff1@fastguy.com | 123456 |
| User | customer1@email.com | 123456 |

---

## Phần 1 — Giới thiệu dự án (2 phút)

"Em xin giới thiệu dự án **FastGuy** — ứng dụng web đặt đồ ăn nhanh trực tuyến.

Dự án gồm 2 phần:
- **Backend** viết bằng Java Jakarta EE, chạy trên Tomcat, kết nối SQL Server
- **Frontend** viết bằng Vue 3, chạy trên Vite

Có 4 role: Guest (khách), User (khách hàng), Staff (nhân viên), Admin (quản trị)."

---

## Phần 2 — Demo Guest flow (3 phút)

### Trang chủ

"Đây là trang chủ, khách hàng có thể thấy:
- Hero banner với title Nhanh Ngon — Chuẩn Việt Chuẩn Tây
- Danh mục sản phẩm
- Thực đơn hôm nay với các món ăn"

→ Click vào sản phẩm bất kỳ

### Chi tiết sản phẩm

"Chi tiết sản phẩm: tên, giá, mô tả, đánh giá. Có thể chọn số lượng và thêm vào giỏ."

→ Click "Thêm vào giỏ"

### Giỏ hàng

"Giỏ hàng hiển thị sản phẩm đã chọn, tổng tiền. Có thể tăng/giảm số lượng hoặc xóa."

→ Click "Tiến hành đặt hàng"

### Chuyển hướng đăng nhập

"Hệ thống yêu cầu đăng nhập vì khách chưa đăng nhập."

→ Đăng nhập với `customer1@email.com` / `123456`

---

## Phần 3 — Demo User flow (3 phút)

### Checkout

"Đây là trang thanh toán, chọn:
- Khu vực giao hàng (quận)
- Phường (xã)
- Địa chỉ cụ thể
- Phương thức thanh toán (COD, MoMo, VNPay)
- Ghi chú cho đơn hàng"

→ Click "Đặt hàng"

### Đơn hàng

"Đơn hàng đã được tạo với trạng thái 'Chờ xác nhận'.
User có thể xem chi tiết đơn hàng."

→ Vào `/account/orders` → xem đơn vừa tạo

---

## Phần 4 — Demo Staff flow (3 phút)

### Đăng nhập Staff

→ Logout → đăng nhập `staff1@fastguy.com` / `123456`

### Dashboard

"Staff Dashboard hiển thị thống kê: tổng đơn, đơn chờ, đơn hôm nay."

### Xử lý đơn hàng

→ Vào `/staff/orders`

"Danh sách đơn hàng chờ xác nhận."

→ Click vào đơn vừa tạo (đơn của customer1)

"Staff xem chi tiết đơn hàng, thông tin khách hàng, sản phẩm.

Staff có thể:
- **Xác nhận** đơn → trạng thái thành 'Đã xác nhận'
- **Hủy** đơn nếu có vấn đề"

→ Click "Xác nhận đơn hàng"

---

## Phần 5 — Demo Admin flow (3 phút)

### Đăng nhập Admin

→ Logout → đăng nhập `admin@fastguy.com` / `123456`

### Dashboard

"Admin Dashboard — tổng quan toàn hệ thống: doanh thu, đơn hàng, người dùng."

### Quản lý sản phẩm

→ Vào `/admin/products`

"Admin có thể thêm, sửa, xóa sản phẩm."

→ Click "Thêm sản phẩm" → điền thông tin → "Lưu"

### Quản lý danh mục

→ Vào `/admin/categories`

"Thêm, sửa, xóa danh mục."

### Báo cáo doanh thu

→ Vào `/admin/reports/revenue`

"Biểu đồ doanh thu theo tháng, sử dụng Chart.js."

### Báo cáo sản phẩm bán chạy

→ Vào `/admin/reports/top-products`

"Sản phẩm nào bán chạy nhất."

---

## Phần 6 — Hỏi đáp (2 phút)

### Câu hỏi thường gặp

**Hỏi: Tại sao chọn Vue mà không phải React?**

Trả lời: "Vue dễ học hơn, phù hợp với team chưa nhiều kinh nghiệm. Syntax đơn giản, template rõ ràng, tích hợp sẵn Composition API."

**Hỏi: Backend dùng framework gì?**

Trả lời: "Dùng Jakarta Servlet thuần, không Spring. Vì dự án quy mô vừa phải, không cần quá nhiều dependency. JPA Hibernate để quản lý database."

**Hỏi: Bảo mật đăng nhập thế nào?**

Trả lời: "Dùng JWT token. Khi đăng nhập thành công, backend trả token. Frontend lưu vào localStorage, gửi kèm trong header Authorization. AuthFilter kiểm tra token mỗi request."

**Hỏi: Có bao nhiêu bảng trong database?**

Trả lời: "17 bảng, bao gồm Users, Product, Category, Orders, Cart, Payment, Review, Ingredient, WorkShift, Schedule..."

**Hỏi: Luồng đặt hàng như thế nào?**

Trả lời: "User thêm vào giỏ → checkout → chọn địa chỉ + thanh toán → tạo đơn với trạng thái PENDING → Staff xác nhận → chuyển PREPARING → giao hàng."

---

## Kết thúc

"Cảm ơn thầy/cô đã xem demo. Dự án còn đang phát triển, tụi em sẽ tiếp tục hoàn thiện thêm."
