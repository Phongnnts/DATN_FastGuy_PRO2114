# FastGuy - Hướng Dẫn Chạy Dự Án

## Yêu cầu cài đặt

| Công cụ    | Phiên bản |
| ---------- | --------- |
| JDK        | 17+       |
| Maven      | 3.8+      |
| Tomcat     | 11        |
| Node.js    | 18+       |
| npm        | 9+        |
| SQL Server | 2019+     |
| SSMS       | Mới nhất  |

---

# 1. Cài đặt Database

## Bước 1: Chọn cách cập nhật database

Database hiện có cần giữ dữ liệu: sao lưu đã kiểm chứng, dừng ghi và chạy lần lượt các script theo `database/migrations/RUNBOOK.md`. Migration mở rộng schema và giữ lại dữ liệu/bảng legacy để cutover an toàn.

Khởi tạo local/demo từ đầu: `database/init.sql` đóng kết nối, **xóa toàn bộ `FastGuyDB` và dữ liệu hiện có**, rồi tạo lại schema cùng dữ liệu demo. Không chạy script này trên database cần giữ dữ liệu.

Login chạy script phải được phép tạo database trên SQL Server và toàn quyền với `FastGuyDB` để chuyển `SINGLE_USER`, xóa, tạo lại, rồi tạo bảng/index. Dùng tài khoản `sysadmin` cho môi trường local là cách đơn giản nhất; không dùng tài khoản ứng dụng giới hạn quyền.

```powershell
sqlcmd -S localhost -E -C -b -i database/init.sql
```

SQL Server Authentication:

```powershell
sqlcmd -S localhost -U your_sql_server_login -P your_sql_server_password -C -b -i database/init.sql
```

Tài khoản demo sau khi chạy script; tất cả dùng mật khẩu `123456`:

| Vai trò | Email |
| --- | --- |
| Admin | `admin@fastguy.local` |
| Staff | `staff@fastguy.local` |
| Shipper | `shipper@fastguy.local` |
| User | `user@fastguy.local` |

---

# 2. Cấu hình backend

Tạo `Backend/FastGuy-FastFoodSite/.env` hoặc khai báo biến môi trường:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=FastGuyDB;encrypt=true;trustServerCertificate=true
DB_USER=your_sql_server_login
DB_PASSWORD=your_sql_server_password
JWT_SECRET=replace-with-at-least-32-characters
```

Các tích hợp ngoài cần biến tương ứng khi sử dụng:

```properties
GHN_TOKEN=
GHN_SHOP_ID=
PAYOS_CLIENT_ID=
PAYOS_API_KEY=
PAYOS_CHECKSUM_KEY=
```

`persistence.xml` giữ `hibernate.hbm2ddl.auto=none`. Hibernate không tạo hoặc cập nhật schema.

PayOS hỗ trợ tạo checkout link, kiểm tra trạng thái và webhook có xác thực chữ ký khi ba biến `PAYOS_*` được cấu hình. Browser return không tự chứng minh thanh toán: trạng thái được đọc lại từ backend; guest phải gửi `orderCode` cùng `returnProof` opaque do checkout cấp.

---

# 3. Chạy Backend

## Cài Smart Tomcat

Trong IntelliJ:

```
File → Settings → Plugins
```

Tìm:

```
Smart Tomcat
```

→ Install → Restart IDE

---

## Tạo cấu hình chạy

```
Run → Edit Configurations
```

Chọn:

```
+ → Smart Tomcat
```

Điền thông tin:

| Mục           | Giá trị                  |     |
| ------------- | ------------------------ | --- |
| Name          | FastGuy                  |     |
| Tomcat Server | Đường dẫn Tomcat 11      |     |
| Context Path  | /FastGuy                 |     |
| Port          | 8082 ( nhớ để port 8082) |     |
| Deployment    | FastGuy-FastFoodSite     |     |

---

## Chạy Backend

Nhấn nút **Run (▶)**.

---

# 4. Chạy Frontend

Mở terminal:

```bash
cd Frontend
```

Cài dependencies:

```bash
npm install
```

Khởi động frontend:

```bash
npm run dev
```

Frontend sẽ chạy tại:

```
http://localhost:5173
```

Route `/` là trang đăng nhập; trang chủ công khai ở `/home`.

Luồng trạng thái đơn chuẩn: `PENDING → CONFIRMED → PREPARING → READY → ASSIGNED → PICKED_UP → DELIVERED`; nhánh giao thất bại là `PICKED_UP → DELIVERY_FAILED → PICKED_UP | RETURNED_TO_STORE`, mặc định tối đa hai lần giao. `CANCELLED` là trạng thái kết thúc qua lệnh hủy riêng. Staff và Shipper chỉ thao tác nghiệp vụ khi tài khoản active và ca hiện tại `CHECKED_IN`; Staff gán Shipper ở `READY`, Shipper chỉ xem/thao tác đơn đã gán cho mình. Giao thất bại và giao lại không đổi tồn kho, thanh toán, coupon hoặc điểm; trả về cửa hàng ghi `WASTE`, trả coupon, không cộng điểm và chỉ tạo hoàn tiền với đơn trả trước. Hủy từ `PREPARING` trở đi ghi tồn kho `WASTE`, không hoàn hàng đã chế biến vào tồn bán.

---

# 5. Kiểm tra Proxy

Mở file:

```text
Frontend/vite.config.js
```

Kiểm tra cấu hình:

```js
proxy: {
  '/api': {
    target: 'http://localhost:8082/FastGuy',
    changeOrigin: true
  }
}
```

---

# 6. Test Hệ Thống

Mở trình duyệt:

```
http://localhost:5173
```

## Tài khoản demo

Sử dụng bốn tài khoản seed đã liệt kê tại phần cài đặt database. Mật khẩu chung: `123456`.

---

# 7. Thứ Tự Chạy Dự Án

### Bước 1

Khởi tạo database SQL Server

### Bước 2

Chạy Backend bằng Smart Tomcat

### Bước 3

Chạy Frontend

```bash
npm run dev
```

### Bước 4

Truy cập:

```
http://localhost:5173
```

---
