# BACKEND — FastGuy Java (Jakarta EE)

## 1. Cấu trúc package

```
Backend/FastGuy-FastFoodSite/src/main/java/
├── entity/           # JPA entities — mapping bảng DB (User, Product, Order...)
├── dao/              # Data Access Object — CRUD queries
├── service/          # Business logic layer
├── servlet/          # HTTP endpoints (Jakarta Servlet)
├── filter/           # Filters (CORS, Auth/JWT)
├── utils/
│   ├── DatabaseUtil  # EntityManagerFactory singleton
│   ├── JwtUtil       # JWT generate/verify
│   ├── JsonUtil      # JSON read/write (Jackson)
│   └── ApiResponse   # Chuẩn response { status, data, message }
└── listener/         # Context listeners
```

### Luồng xử lý request

```
Browser / Frontend
       ↓ HTTP request (JSON)
   Servlet (servlet/*.java)
       ↓ gọi
   Service (service/*.java)  — xử lý logic, kiểm tra điều kiện
       ↓ gọi
   DAO (dao/*.java)          — query DB qua JPA EntityManager
       ↓
   Database (SQL Server)
       ↓
   Entity → DAO → Service → Servlet
       ↓
   Response JSON (ApiResponse + JsonUtil)
```

## 2. Cách thêm 1 API mới

### Ví dụ: Thêm API lấy sản phẩm theo ID

#### Bước 1 — DAO (nếu chưa có method)

```java
// dao/ProductDAO.java
public Product findById(int id) {
    EntityManager em = DatabaseUtil.getEntityManager();
    try {
        return em.find(Product.class, id);
    } finally {
        em.close();
    }
}
```

#### Bước 2 — Service (nếu cần logic)

```java
// service/ProductService.java
public Product getById(int id) {
    return productDAO.findById(id);
}
```

#### Bước 3 — Servlet (endpoint)

```java
package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import utils.ApiResponse;
import utils.JsonUtil;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {

    private ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCors(resp);

        String pathInfo = req.getPathInfo(); // "/123"

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Product product = productService.getById(id);

            if (product == null) {
                JsonUtil.write(resp, ApiResponse.error("Không tìm thấy sản phẩm"));
                return;
            }

            // Map entity → Map nếu cần trả field cụ thể
            Map<String, Object> data = new HashMap<>();
            data.put("id", product.getProductId());
            data.put("name", product.getName());
            data.put("price", product.getPrice());

            JsonUtil.write(resp, ApiResponse.ok(data));
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.write(resp, ApiResponse.error("Lỗi máy chủ"));
        }
    }

    private void setCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
```

## 3. Chuẩn response API

Mọi response đều dùng `ApiResponse`:

```java
// Thành công
ApiResponse.ok(data, "Thông báo")
// → { "status": "success", "data": {...}, "message": "Thông báo" }

// Lỗi
ApiResponse.error("Lỗi gì đó")
// → { "status": "error", "data": null, "message": "Lỗi gì đó" }
```

Viết JSON bằng `JsonUtil`:

```java
JsonUtil.write(resp, ApiResponse.ok(data));
```

## 4. JPA EntityManager pattern

```java
// Luôn dùng try-finally để đóng EntityManager
EntityManager em = DatabaseUtil.getEntityManager();
try {
    // Query...
    return em.createQuery("SELECT e FROM Entity e", Entity.class)
             .getResultList();
} finally {
    em.close(); // Luôn đóng
}
```

Với transaction (ghi dữ liệu):

```java
EntityManager em = DatabaseUtil.getEntityManager();
try {
    em.getTransaction().begin();
    em.persist(entity);       // INSERT
    // em.merge(entity);      // UPDATE
    // em.remove(entity);     // DELETE
    em.getTransaction().commit();
} catch (Exception e) {
    em.getTransaction().rollback();
    e.printStackTrace();
} finally {
    em.close();
}
```

## 5. Authentication flow

### Login

```
POST /api/auth/login
Body: { "login": "email_or_phone", "password": "..." }

Response:
{
  "status": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "role": "ADMIN",
    "fullName": "Admin FastGuy",
    "avatarUrl": "..."
  }
}
```

### JWT

- Token được sinh bằng `JwtUtil.generate(userId, roleName)`
- Frontend gửi token trong header: `Authorization: Bearer <token>`
- `AuthFilter` kiểm tra token trước khi vào servlet

### Các endpoint cần auth

Không cần config riêng — `AuthFilter` tự động check path:

- Path bắt đầu bằng `/api/auth/` → public (không check token)
- Các path khác `/api/*` → cần token hợp lệ

## 6. Encoding & CORS

Mọi servlet đều cần 2 dòng đầu trong mỗi method:

```java
resp.setContentType("application/json;charset=UTF-8");
setCors(resp);
```

Hàm CORS mẫu:

```java
private void setCors(HttpServletResponse resp) {
    resp.setHeader("Access-Control-Allow-Origin", "*");
    resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
}
```

> **Với SQL Server:** Dùng `nvarchar` cho cột tiếng Việt.
> JDBC URL không cần `characterEncoding=UTF-8` — SQL Server driver xử lý Unicode qua `nvarchar`.

## 7. Cấu trúc database

```
Database: FastGuyDB (SQL Server)

17 tables:
  Role, Users, Address, DeliveryZone
  Category, Product, ProductOption
  Cart, CartItem
  Orders, OrderItem, Payment
  Ingredient, ProductIngredient
  WorkShift, Schedule
  Review
```

Seed data: `database/init.sql` — chạy 1 lần để có dữ liệu mẫu.
