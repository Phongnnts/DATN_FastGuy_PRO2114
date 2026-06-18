# FastGuy - Fast Food Ordering System

## Yêu cầu hệ thống

| Công cụ | Phiên bản | Ghi chú |
|---------|-----------|---------|
| **JDK** | 17+ | OpenJDK hoặc Oracle JDK |
| **Apache Maven** | 3.8+ | Build backend |
| **Apache Tomcat** | 11 | Jakarta Servlet 6.1 |
| **Node.js** | 18+ | Chạy frontend |
| **npm** | 9+ | Kèm theo Node.js |
| **SQL Server** | 2019+ | Hoặc Express |
| **SQL Server Management Studio (SSMS)** | - | Tạo database |

---

## Cấu trúc thư mục

```
DATN_FastGuy/
├── Backend/
│   └── FastGuy-FastFoodSite/     # Backend Java (Maven + WAR)
│       ├── pom.xml
│       ├── src/main/java/
│       │   ├── dao/               # Data Access Objects
│       │   ├── entity/            # JPA entities
│       │   ├── service/           # Business logic
│       │   ├── servlet/           # API endpoints
│       │   └── utils/             # JWT, JSON, DB connection
│       └── src/main/resources/META-INF/
│           └── persistence.xml    # JPA config (DB connection)
├── Frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── .env
│   └── src/
│       ├── api/                   # API client (axios)
│       ├── stores/                # Pinia stores
│       ├── views/                 # Vue pages
│       └── components/            # Reusable components
└── .smarttomcat/                  # Intellij Smart Tomcat config (gitignored)
```

---

## 1. Cài đặt Database

### Bước 1: Tạo database

Mở **SSMS** và chạy:

```sql
CREATE DATABASE FastGuy1;
```

### Bước 2: Tạo login user

```sql
CREATE LOGIN JavaDuAn WITH PASSWORD = 'ZaZksnguyen1234';
USE FastGuy1;
CREATE USER JavaDuAn FOR LOGIN JavaDuAn;
ALTER ROLE db_owner ADD MEMBER JavaDuAn;
```

### Bước 3: Bật TCP/IP cho SQL Server

1. Mở **SQL Server Configuration Manager**
2. **SQL Server Network Configuration** → **Protocols for MSSQLSERVER**
3. Bật **TCP/IP** → chuột phải **Enable**
4. Restart SQL Server

### Bước 4: Kiểm tra kết nối

```sql
SELECT 1;  -- phải trả về 1
```

> Cấu hình kết nối DB ở `Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml`

---

## 2. Chạy Backend

### Cách 1: Smart Tomcat (IntelliJ IDEA) — Khuyến nghị

**Smart Tomcat** là plugin IntelliJ giúp deploy WAR trực tiếp từ IDE mà không cần copy file thủ công.

#### Cài plugin

1. **File** → **Settings** → **Plugins**
2. Tìm `Smart Tomcat` → **Install**
3. Restart IntelliJ

#### Cấu hình Run Configuration

1. **Run** → **Edit Configurations**
2. Nhấn **+** → **Smart Tomcat**
3. Điền các thông tin:

| Field | Giá trị | Giải thích |
|-------|---------|-----------|
| **Name** | `FastGuy` | Tên tùy chọn |
| **Tomcat Server** | `D:\path\to\apache-tomcat-11` | Đường dẫn Tomcat đã giải nén |
| **Context Path** | `/FastGuy` | **Context path** — đường dẫn gốc của web app (xem giải thích bên dưới) |
| **Port** | `8082` | Cổng Tomcat chạy |
| **VM Options** | để trống | |
| **Deployment** | chọn **FastGuy-FastFoodSite** | Module Maven |

#### Chạy

- Nhấn nút **Run** (▶) bên cạnh config `FastGuy`
- Mở trình duyệt: `http://localhost:8082/FastGuy/api/delivery-zones`
- Phải thấy JSON danh sách quận/huyện

### Cách 2: Maven + WAR thủ công

```bash
# Build WAR
cd Backend/FastGuy-FastFoodSite
mvn clean package -DskipTests
```

File WAR tạo ra ở `target/FastGuy.war` (tên `FastGuy` lấy từ `<finalName>` trong `pom.xml`).

Copy file này vào `webapps/` của Tomcat:

```bash
cp target/FastGuy.war D:\path\to\apache-tomcat-11\webapps\
```

Khởi động Tomcat:

```bash
D:\path\to\apache-tomcat-11\bin\startup.bat
```

> Ứng dụng tự động deploy với context path `/FastGuy`

### Giải thích: Port, Context Path là gì?

#### Port
- Là cổng Tomcat lắng nghe HTTP request
- Mặc định Tomcat là **8080**
- Dự án này dùng **8082** (cấu hình trong Smart Tomcat) để tránh xung đột với các app khác
- URL đầy đủ: `http://localhost:8082/FastGuy/...`

#### Context Path (`/FastGuy`)
- Là "đường dẫn gốc" của web app
- Ví dụ: nếu context path là `/FastGuy`:
  - API order ở `/api/orders` → URL đầy đủ: `http://localhost:8082/FastGuy/api/orders`
  - Nếu context path là `/` (root): URL là `http://localhost:8082/api/orders`
- Tên context path lấy từ `<finalName>FastGuy</finalName>` trong `pom.xml`
- **Muốn đổi**: sửa `<finalName>` trong `pom.xml` và cập nhật lại Smart Tomcat context path + Vite proxy trong `frontend/vite.config.js`

### Cấu hình Backend (persistence.xml)

```xml
<!-- File: src/main/resources/META-INF/persistence.xml -->
<property name="jakarta.persistence.jdbc.url"
          value="jdbc:sqlserver://localhost:1433;databaseName=FastGuy1;encrypt=false"/>
<property name="jakarta.persistence.jdbc.user" value="JavaDuAn"/>
<property name="jakarta.persistence.jdbc.password" value="ZaZksnguyen1234"/>
<property name="hibernate.hbm2ddl.auto" value="update"/>
```

- `hbm2ddl.auto = update`: Hibernate tự động tạo/cập nhật bảng khi chạy
- Lần đầu chạy sẽ tạo toàn bộ tables từ entity classes

---

## 3. Chạy Frontend

### Cài dependencies

```bash
cd Frontend
npm install
```

### Chạy dev server

```bash
npm run dev
```

Server chạy tại `http://localhost:5173`

### Cấu hình proxy (vite.config.js)

```js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8082/FastGuy',  // Backend URL
      changeOrigin: true
    }
  }
}
```

Giải thích:
- Trình duyệt gọi `fetch('/api/orders')` → Vite proxy chuyển tiếp đến `http://localhost:8082/FastGuy/api/orders`
- Giúp tránh CORS khi phát triển

> Nếu backend chạy ở port/context khác thì phải sửa `target` cho khớp.

### Build production (tùy chọn)

```bash
npm run build
```

Kết quả trong `dist/` — deploy lên web server tĩnh (nginx, ...).

---

## 4. Kiểm tra luồng

Sau khi **backend** + **frontend** đều chạy:

1. Mở `http://localhost:5173`
2. Đăng nhập:
   - User: `user1@gmail.com` / `123456`
   - Staff: `staff1@fastguy.com` / `123456`
3. User đặt hàng → chọn quận/phường → nhập số nhà → đặt
4. Staff vào tab "Quản lý đơn hàng" → xem đơn PENDING → Xác nhận / Hủy

---

## 5. Lưu ý

- **Port 8082 conflict**: Nếu Tomcat báo port đã dùng, vào **Smart Tomcat config** đổi `Port` sang số khác (ví dụ 8083), và cập nhật lại `target` trong `vite.config.js`
- **Ward data**: Danh sách phường được hardcode trong `CheckoutPage.vue` (không lưu DB)
- **Hibernate tự tạo bảng**: Nếu gặp lỗi "table không tồn tại", kiểm tra `hbm2ddl.auto` = `update` và user SQL có quyền CREATE TABLE
- **Encoding**: Nếu dữ liệu tiếng Việt bị lỗi hiển thị, thêm `?useUnicode=true&characterEncoding=UTF-8` vào connection URL

---

## 6. Tài khoản mẫu

| Email | Password | Vai trò |
|-------|----------|---------|
| `admin@fastguy.com` | `123456` | Admin |
| `staff1@fastguy.com` | `123456` | Staff |
| `staff2@fastguy.com` | `123456` | Staff |
| `user1@gmail.com` | `123456` | User |
| `user2@gmail.com` | `123456` | User |
| `shipper1@fastguy.com` | `123456` | Shipper |
