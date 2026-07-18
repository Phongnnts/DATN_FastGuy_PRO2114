# 🚀 FastGuy - Hướng Dẫn Chạy Dự Án

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

## Bước 1: Tạo Database ở máy (Tìm .sql trong folder database)

Mở SSMS, chạy file `database/init.sql` (hoặc chạy từng lệnh dưới đây):

```sql
CREATE DATABASE FastGuyDB;
USE FastGuyDB;
-- Sau đó chạy toàn bộ lệnh trong database/init.sql
```

---

# 2. Cấu hình persistence

Mở file:

```text
Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml
```

Kiểm tra thông tin kết nối:

```xml
<property name="jakarta.persistence.jdbc.url"
          value="jdbc:sqlserver://localhost:1433;databaseName=FastGuyDB;encrypt=false"/>

<property name="jakarta.persistence.jdbc.user"
          value="JavaDuAn"/> -- Tên Login SqlServer

<property name="jakarta.persistence.jdbc.password"
          value="ZaZksnguyen1234"/> Pass SqlServer

<property name="hibernate.hbm2ddl.auto"
          value="update"/>
```

> Hibernate sẽ tự tạo bảng khi chạy lần đầu.

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

## Tài khoản Admin

```
Email: admin@fastguy.com
Password: 123456
```

## Tài khoản Staff

```
Email: staff1@fastguy.com
Password: 123456
Email: staff2@fastguy.com
Password: 123456
```

## Tài khoản Shipper

```
Email: shipper1@fastguy.com
Password: 123456
Email: shipper2@fastguy.com
Password: 123456
```

## Tài khoản User

```
Email: customer1@email.com
Password: 123456
Email: customer2@email.com
Password: 123456
```

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
