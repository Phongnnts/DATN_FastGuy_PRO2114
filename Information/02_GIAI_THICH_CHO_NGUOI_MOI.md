# FastGuy — Giải thích dễ hiểu cho người mới

Dự án FastGuy gồm 2 phần chính:

- **Backend Java Jakarta EE**: xử lý dữ liệu, đăng nhập, giỏ hàng, đơn hàng, sản phẩm.
- **Frontend Vue**: giao diện người dùng nhìn thấy và thao tác.

Nói đơn giản:

```
Frontend = màn hình người dùng bấm
Backend = nơi xử lý yêu cầu
Database = nơi lưu dữ liệu
```

Ví dụ người dùng bấm xem sản phẩm:

```
Người dùng bấm trên web
        ↓
Frontend gọi API
        ↓
Backend nhận request
        ↓
Backend hỏi database
        ↓
Database trả dữ liệu
        ↓
Backend trả JSON
        ↓
Frontend hiển thị sản phẩm
```

---

# PHẦN 1 — BACKEND FastGuy Java

## 1. Backend dùng để làm gì?

Backend là nơi xử lý các việc quan trọng như:

- Lấy danh sách sản phẩm
- Đăng nhập, đăng ký
- Kiểm tra token JWT
- Thêm sản phẩm vào giỏ hàng
- Tạo đơn hàng
- Quản lý user, staff, admin
- Đọc và ghi dữ liệu vào SQL Server

Frontend không trực tiếp nói chuyện với database. Frontend chỉ gọi API của backend.

---

## 2. Cấu trúc thư mục backend

```
entity/     → Đại diện cho bảng trong database
dao/        → Chuyên query database
service/    → Xử lý logic nghiệp vụ
servlet/    → Nơi nhận request từ frontend
filter/     → Kiểm tra request trước khi vào servlet
utils/      → Các hàm dùng chung
listener/   → Chạy khi app bắt đầu hoặc kết thúc
```

### entity

Entity giống như bản Java của bảng database.

Ví dụ database có bảng `Product`, thì Java có class `Product`.

```
Bảng Product trong SQL Server
        ↔
Product.java trong entity
```

Entity thường chứa các field như:

```java
private int productId;
private String name;
private BigDecimal price;
```

### dao

DAO là nơi viết code để lấy dữ liệu từ database.

Ví dụ:

```java
Product product = em.find(Product.class, id);
```

Nghĩa là: tìm sản phẩm trong database theo id.

DAO chỉ nên tập trung vào: SELECT, INSERT, UPDATE, DELETE.

### service

Service là nơi xử lý logic chính.

Ví dụ: người dùng muốn đặt hàng → Service sẽ kiểm tra:
- Giỏ hàng có sản phẩm không?
- Sản phẩm còn hàng không?
- Địa chỉ giao hàng hợp lệ không?
- Tổng tiền là bao nhiêu?

Sau đó service mới gọi DAO để lưu đơn hàng vào database.

Nói ngắn gọn: `DAO = làm việc với database`, `Service = xử lý nghiệp vụ`.

### servlet

Servlet là nơi nhận request từ frontend.

Ví dụ frontend gọi `GET /api/products/123` thì `ProductServlet` sẽ nhận request này.

Servlet sẽ:
1. Đọc dữ liệu từ request
2. Gọi service
3. Nhận kết quả
4. Trả JSON về frontend

Servlet giống như "cửa tiếp nhận yêu cầu".

### filter

Filter chạy trước servlet.

Ví dụ `AuthFilter` dùng để kiểm tra đăng nhập.

```
Frontend gọi /api/orders
        ↓
AuthFilter kiểm tra token
        ↓
Token đúng → cho vào OrderServlet
Token sai → trả 401 Unauthorized
```

### utils

Chứa các class tiện ích dùng chung:

- `DatabaseUtil` → tạo EntityManager để kết nối database
- `JwtUtil` → tạo và kiểm tra JWT token
- `JsonUtil` → đọc/ghi JSON
- `ApiResponse` → chuẩn hóa response trả về frontend

---

## 3. Luồng xử lý request backend

Ví dụ lấy sản phẩm id = 123:

```
Frontend → ProductServlet → ProductService → ProductDAO → Database
                                                                    ↓
Frontend ← ProductServlet ← ProductService ← ProductDAO ← Database
```

Giải thích:
1. Servlet nhận yêu cầu
2. Service xử lý logic
3. DAO hỏi database
4. Database trả dữ liệu
5. Servlet trả kết quả cho frontend

---

## 4. Cách thêm một API mới

Ví dụ thêm API `GET /api/products/123` — lấy sản phẩm theo id.

### Bước 1: Thêm method trong DAO

```java
public Product findById(int id) {
    EntityManager em = DatabaseUtil.getEntityManager();
    try {
        return em.find(Product.class, id);
    } finally {
        em.close();
    }
}
```

Mở kết nối → tìm Product theo id → đóng kết nối.

### Bước 2: Thêm method trong Service

```java
public Product getById(int id) {
    return productDAO.findById(id);
}
```

Service có thể thêm logic kiểm tra nếu cần.

### Bước 3: Thêm Servlet endpoint

```java
@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {
```

- Đọc id từ URL: `String pathInfo = req.getPathInfo()` → `/123`
- Parse id: `int id = Integer.parseInt(pathInfo.substring(1))`
- Gọi service: `Product product = productService.getById(id)`
- Trả JSON: `JsonUtil.write(resp, ApiResponse.ok(data))`

Frontend nhận JSON:

```json
{
  "status": "success",
  "data": { "id": 123, "name": "Burger bò", "price": 59000 },
  "message": null
}
```

---

## 5. Chuẩn response API

Thành công:

```json
{ "status": "success", "data": {}, "message": "Thông báo" }
```

Lỗi:

```json
{ "status": "error", "data": null, "message": "Lỗi gì đó" }
```

Code:

```java
JsonUtil.write(resp, ApiResponse.ok(data));
JsonUtil.write(resp, ApiResponse.error("Không tìm thấy"));
```

---

## 6. EntityManager là gì?

EntityManager là công cụ của JPA dùng để làm việc với database.

Luôn nhớ đóng EntityManager sau khi dùng:

```java
EntityManager em = DatabaseUtil.getEntityManager();
try {
    // ...
} finally {
    em.close(); // quan trọng
}
```

---

## 7. Khi nào cần transaction?

- **Đọc dữ liệu**: không cần transaction
- **Ghi dữ liệu** (INSERT/UPDATE/DELETE): cần transaction

Mẫu code:

```java
em.getTransaction().begin();
em.persist(entity); // hoặc merge, remove
em.getTransaction().commit();
```

Nếu lỗi thì rollback để tránh dữ liệu sai.

---

## 8. Đăng nhập và JWT

Khi user đăng nhập:

```
POST /api/auth/login  Body: { "login": "...", "password": "..." }
```

Backend trả về token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": 1,
  "role": "ADMIN",
  "fullName": "Admin FastGuy",
  "avatarUrl": "..."
}
```

Frontend lưu token, gửi kèm khi gọi API cần auth:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

AuthFilter kiểm tra token, nếu sai/hết hạn → 401 Unauthorized.

---

## 9. API nào cần đăng nhập?

- `/api/auth/*` → public (không cần token)
- `/api/*` → cần token hợp lệ

---

## 10. Encoding và CORS

Servlet cần 2 dòng:

```java
resp.setContentType("application/json;charset=UTF-8");
setCors(resp);
```

CORS cho phép frontend (port 5173) gọi backend (port 8082).

---

## 11. Database

- Tên: `FastGuyDB` (SQL Server)
- File dữ liệu mẫu: `database/init.sql`
- Dùng `nvarchar` cho dữ liệu tiếng Việt
- 17 bảng: Users, Product, Category, Orders, Cart, Payment, Review...

---

# PHẦN 2 — FRONTEND FastGuy Vue

## 1. Cấu trúc thư mục

```
api/          → hàm gọi backend
assets/       → CSS, biến màu
components/   → component dùng lại
composables/  → logic dùng lại
layouts/      → layout theo role
router/       → định nghĩa đường dẫn
stores/       → Pinia store lưu state
utils/        → hàm tiện ích
views/        → các trang chính
```

### api

Chứa các hàm gọi backend.

Ví dụ `productApi.getAll()` gọi `GET /products`.

File `client.js` (Axios) tự động gắn token vào request.

### stores

Lưu dữ liệu dùng chung:

- `product store` → danh sách sản phẩm
- `auth store` → thông tin user
- `cart store` → giỏ hàng
- `order store` → đơn hàng

### views

Các trang `.vue` gồm 3 phần:

```vue
<script setup>  // code logic
<template>      // HTML
<style scoped>  // CSS riêng
```

### router

Quyết định URL nào mở trang nào.

---

## 2. Luồng dữ liệu frontend

```
Component gọi Store → Store gọi API → API gọi Backend
                                                    ↓
Component hiển thị ← Store lưu dữ liệu ← Backend trả JSON
```

Ví dụ load sản phẩm:

```vue
<script setup>
import { onMounted } from 'vue'
import { useProductStore } from '@/stores/product'
const productStore = useProductStore()
onMounted(() => productStore.init())
</script>
```

Trong store:

```js
const allProducts = ref([])
async function init() {
  allProducts.value = await productApi.getAll()
}
```

---

## 3. Route và phân quyền

Mỗi route có `meta` để kiểm tra quyền:

```js
meta: { guest: true }                     // ai cũng vào được
meta: { requiresAuth: true, role: ROLES.USER }  // chỉ user
meta: { requiresAuth: true, role: ROLES.ADMIN } // chỉ admin
```

Router guard tự động kiểm tra và chuyển hướng nếu không đủ quyền.

---

## 4. Quy tắc đặt tên field

Backend trả field tên gì, frontend dùng tên đó.

Ví dụ backend trả `fullName` và `avatarUrl`:

```js
user.fullName    // đúng
user.avatarUrl   // đúng
```

Không tự đổi thành `name` hay `avatar`.

---

## 5. Vue DevTools

Cài extension Vue DevTools → F12 → tab Vue → Pinia.

Xem state store real-time: dữ liệu, loading, error.

---

## 6. Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| 401 Unauthorized | Token sai/hết hạn | Đăng nhập lại |
| Cannot read properties of null | User null | Dùng `v-if` hoặc `?.` |
| 404 route | Quên thêm route | Kiểm tra router/index.js |
| Dữ liệu không cập nhật | Dùng biến thường | Dùng `ref()` |
| CORS | Khác port | Set header trong servlet |

---

# Tóm tắt cực ngắn

## Backend

```
Servlet nhận request → Service xử lý logic → DAO query database
Entity map với bảng → JsonUtil trả JSON → AuthFilter kiểm tra token
```

## Frontend

```
View hiển thị giao diện → Store lưu state → API gọi backend
Router quản lý đường dẫn → Layout chia giao diện theo role
```

## Luồng đầy đủ

```
User click → Vue Component → Pinia Store → API Axios
    → Java Servlet → Service → DAO → Database
    → JSON trả về → Vue cập nhật giao diện
```

Cứ nhớ một câu: **Frontend hỏi, Backend xử lý, Database lưu dữ liệu.**
