# FastGuy — Restructure & Development Plan

## Phase 0: Dọn dẹp & khởi tạo cấu trúc

### 0.1 Xóa package/class cũ
- [ ] `servlet/*` (10 files — thay bằng controller/)
- [ ] `dao/*` + `dao/impl/*` + `dao/generic/*` (18+1 files — thay bằng repository/)
- [ ] `service/generic/*` (1 file)
- [ ] `service/impl/VoucherService.java`
- [ ] `service/impl/LogService.java`
- [ ] `service/impl/ReportService.java`
- [ ] `entity/Voucher.java`
- [ ] `entity/OrderVoucher.java`
- [ ] `entity/PaymentTransaction.java`
- [ ] `entity/SystemLog.java`
- [ ] `entity/Report.java`
- [ ] `entity/DeliveryProof.java`
- [ ] `entity/DeliveryFailure.java`
- [ ] `filter/LoggingFilter.java`
- [ ] `utils/VoucherValidator.java`
- [ ] `utils/QRCodeUtil.java`
- [ ] `utils/ExcelExportUtil.java`
- [ ] `src/main/webapp/index.jsp`

### 0.2 Tạo package mới
- [ ] `controller/`
- [ ] `repository/`
- [ ] `dto/`
- [ ] `config/`
- [ ] `exception/`

### 0.3 Cập nhật pom.xml
Thêm dependencies:
- Jakarta Servlet 6 API (`jakarta.servlet:jakarta.servlet-api:6.1.0`)
- Hibernate 6 (`org.hibernate.orm:hibernate-core:6.6.0.Final`)
- SQL Server JDBC (`com.microsoft.sqlserver:mssql-jdbc:12.8.1.jre11`)
- Jackson 2.18 (`com.fasterxml.jackson.core:jackson-databind:2.18.0`)
- jjwt 0.12 (`io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- BCrypt (`org.mindrot:jbcrypt:0.4`)
- `maven-compiler-plugin` (source/target = 24)
- `maven-war-plugin` (failOnMissingWebXml = false)

### 0.4 Cập nhật persistence.xml
- persistence-unit name = FastGuyPU
- provider = Hibernate
- SQL Server dialect
- JDBC connection
- hbm2ddl.auto = validate

### 0.5 Cập nhật web.xml
- Jakarta Servlet 6 namespace (`xmlns="https://jakarta.ee/xml/ns/jakartaee"`)
- version="6.0"
- Filter mappings: CorsFilter, JwtAuthenticationFilter

### 0.6 Sửa .gitignore
- Dòng 6: xóa `git commit -m "Add gitignore"`
- Thêm `Frontend/node_modules/`

### 0.7 Cập nhật DBFastGuy.sql
Ghi đè bằng schema mới (xem tài liệu user cung cấp), bổ sung:
- [ ] Permission table
- [ ] RolePermission table
- [ ] Sample permissions

---

## Phase 1: Base Infrastructure

### 1.1 config/HibernateConfig.java
- Singleton EntityManagerFactory
- Method getEntityManager()

### 1.2 config/JwtConfig.java
- SECRET_KEY, EXPIRATION_MS, ISSUER constants
- Đọc từ context params hoặc hardcode (cho đồ án)

### 1.3 exception/AppException.java
- Base: statusCode, message, field (optional)

### 1.4 exception/ResourceNotFoundException.java (404)
### 1.5 exception/BadRequestException.java (400)
### 1.6 exception/UnauthorizedException.java (401)
### 1.7 exception/DuplicateResourceException.java (409)

### 1.8 exception/GlobalExceptionHandler.java
- Filter hoặc servlet-level catch
- Trả về `{"status":"error","message":"...","data":null}`

### 1.9 dto/ApiResponse.java
```java
public class ApiResponse<T> {
    private String status; // "success" | "error"
    private String message;
    private T data;
    // static factories: ok(data), ok(data, message), error(message)
}
```

### 1.10 dto/PagedResponse.java
```java
public class PagedResponse<T> {
    private List<T> items;
    private int total;
    private int page;
    private int limit;
}
```

### 1.11 filter/CorsFilter.java
```java
@WebFilter("/*")
public class CorsFilter implements Filter {
    // Allow origins, methods, headers
    // X-Session-Id exposed header
}
```

### 1.12 filter/JwtAuthenticationFilter.java
- Bỏ qua: /api/auth/**, /api/products/**, /api/categories/**, /api/delivery-zones/**
- Check Authorization header
- Parse JWT → set request attribute "userId" + "role"

### 1.13 util/JwtUtil.java
- generateToken(userId, role)
- validateToken(token) → Claims
- getUserId(token), getRole(token)

### 1.14 util/PasswordUtil.java
- hash(password) → String
- verify(password, hash) → boolean

### 1.15 util/JsonUtil.java
- toJson(object) → String
- fromJson(json, class) → T
- Jackson ObjectMapper singleton

---

## Phase 2: Auth & Users

### 2.1 entity/Role.java
- roleId, roleName
- @OneToMany → Set<Permission> hoặc @ManyToMany

### 2.2 entity/Permission.java
- permissionId, permissionName, description

### 2.3 entity/User.java
- userId, role (ManyToOne → Role), email, phone, passwordHash, fullName, avatarUrl, status, createdAt

### 2.4 repository/RoleRepository.java
- findByRoleName(String name)

### 2.5 repository/UserRepository.java
- findByEmail(String), findByPhone(String), existsByPhone(String), findAll(int page, int limit)

### 2.6 repository/PermissionRepository.java
- findPermissionsByRoleId(int roleId)

### 2.7 dto/RegisterRequest.java
- fullName, phone, email (opt), password

### 2.8 dto/LoginRequest.java
- login (email or phone), password

### 2.9 dto/LoginResponse.java
- token, userId, role, fullName, avatarUrl

### 2.10 dto/UserDTO.java
- userId, fullName, phone, email, avatarUrl, role, status

### 2.11 dto/ChangePasswordRequest.java
- oldPassword, newPassword

### 2.12 service/AuthService.java
- register(RegisterRequest) → User
- login(LoginRequest) → LoginResponse
- changePassword(Long userId, ChangePasswordRequest)

### 2.13 service/UserService.java
- getProfile(Long userId) → UserDTO
- updateProfile(Long userId, UserDTO)

### 2.14 controller/AuthController.java
- POST /api/auth/register
- POST /api/auth/login
- PUT /api/auth/change-password
- POST /api/auth/forgot-password (optional)
- POST /api/auth/reset-password (optional)

### 2.15 controller/UserController.java
- GET /api/users/profile
- PUT /api/users/profile

---

## Phase 3: Address & Delivery Zone

### 3.1 entity/DeliveryZone.java
- zoneId, districtName, shippingFee, isActive

### 3.2 entity/Address.java
- addressId, user (ManyToOne), recipientName, phone, street, ward, zone (ManyToOne), city, isDefault

### 3.3 repository/DeliveryZoneRepository.java
- findAllActive()

### 3.4 repository/AddressRepository.java
- findByUserUserId(Long userId)
- findByIdAndUserUserId(Long addressId, Long userId)

### 3.5 dto/AddressDTO.java / CreateAddressRequest.java

### 3.6 service/AddressService.java
- CRUD, kiểm tra ownership, setDefault()

### 3.7 controller/AddressController.java
- GET /api/addresses
- POST /api/addresses
- PUT /api/addresses/{id}
- DELETE /api/addresses/{id}

### 3.8 controller/DeliveryZoneController.java
- GET /api/delivery-zones

---

## Phase 4: Category & Product

### 4.1 entity/Category.java
- categoryId, name, description, sortOrder, status (ACTIVE/INACTIVE)

### 4.2 entity/Product.java
- productId, category (ManyToOne), name, description, price, imageUrl, status (AVAILABLE/UNAVAILABLE), createdAt, updatedAt

### 4.3 entity/ProductOption.java
- optionId, product (ManyToOne), optionName, extraPrice, stockControlled, quantityAvailable

### 4.4 repository/CategoryRepository.java
- findAllActiveOrderBySortOrder()

### 4.5 repository/ProductRepository.java
- search/filter: byCategory, byKeyword, byStatus
- Paged queries

### 4.6 repository/ProductOptionRepository.java
- findByProductProductId(Long productId)

### 4.7 dto/ProductDTO.java / CategoryDTO.java / ProductDetailDTO.java / CreateProductRequest.java

### 4.8 service/ProductService.java
- getCategories(), getProducts(page, filter), getProductDetail(id)
- Admin: create, update, delete product

### 4.9 controller/ProductController.java
- GET /api/categories
- GET /api/products
- GET /api/products/{id}

---

## Phase 5: Cart

### 5.1 entity/Cart.java
- cartId, user (ManyToOne, nullable), sessionId (nullable), createdAt, updatedAt
- CHECK (user IS NOT NULL OR sessionId IS NOT NULL)

### 5.2 entity/CartItem.java
- cartItemId, cart (ManyToOne), product (ManyToOne), quantity, optionData (JSON string), unitPrice

### 5.3 repository/CartRepository.java
- findByUserUserId(Long userId)
- findBySessionId(String sessionId)

### 5.4 repository/CartItemRepository.java

### 5.5 dto/CartDTO.java / AddToCartRequest.java / UpdateCartItemRequest.java

### 5.6 service/CartService.java
- getOrCreateCart(Long userId, String sessionId)
- addItem(cartId, productId, quantity, options)
- updateQuantity(cartItemId, quantity)
- removeItem(cartItemId)
- clearCart(cartId)
- mergeGuestCartToUser(sessionId, userId) — khi guest login

### 5.7 controller/CartController.java
- GET /api/cart
- POST /api/cart/items
- PUT /api/cart/items/{cartItemId}
- DELETE /api/cart/items/{cartItemId}
- DELETE /api/cart

---

## Phase 6: Order & Payment

### 6.1 entity/Order.java
- orderId, orderCode, user (ManyToOne, nullable), customerName, customerPhone, customerAddress
- zone (ManyToOne → DeliveryZone)
- totalAmount, shippingFee, finalAmount
- paymentMethod (COD/VNPAY/MOMO/VIETQR), paymentStatus (UNPAID/PAID)
- orderStatus (PENDING/CONFIRMED/READY/DELIVERING/DELIVERED/CANCELLED/FAILED)
- staff (ManyToOne), shipper (ManyToOne)
- timestamps: assignedAt, confirmedAt, readyAt, pickedUpAt, deliveredAt, cancelledAt
- failureReason, internalNote
- createdAt

### 6.2 entity/OrderItem.java
- orderItemId, order (ManyToOne), product (ManyToOne, nullable), productName, quantity, unitPrice, optionData, totalPrice

### 6.3 entity/Payment.java
- paymentId, order (ManyToOne), amount, paymentMethod, transactionId, status (PENDING/SUCCESS/FAILED), paidAt
- COD: shipper (ManyToOne), collectedAt

### 6.4 repository/OrderRepository.java
- Find by status, user, staff, shipper
- findPendingOrders() — for staff
- findAssignedOrders(shipperId)
- Date-range queries for statistics

### 6.5 repository/OrderItemRepository.java

### 6.6 repository/PaymentRepository.java

### 6.7 dto/CreateOrderRequest.java / OrderDTO.java / OrderDetailDTO.java / PaymentDTO.java

### 6.8 service/OrderService.java
- createOrder (từ cart hoặc trực tiếp cho guest)
- getOrdersByUser
- getOrderDetail
- cancelOrder (user — only PENDING/CONFIRMED)
- generateOrderCode()

### 6.9 service/PaymentService.java
- processPayment(orderId, method) → paymentUrl (nếu online)
- handleWebhook(provider, data)

### 6.10 controller/OrderController.java
- POST /api/orders
- GET /api/orders/my-orders
- GET /api/orders/{orderId}
- PUT /api/orders/{orderId}/cancel

---

## Phase 7: Staff

### 7.1 entity/Ingredient.java / ProductIngredient.java / InventoryTransaction.java
- ingredient management for stock checking

### 7.2 repository/IngredientRepository.java / ProductIngredientRepository.java / InventoryTransactionRepository.java
- findByProductId, deductStock

### 7.3 service/StaffService.java
- getPendingOrders()
- confirmOrder(orderId, staffId) → check stock, deduct inventory
- markReady(orderId)
- assignShipper(orderId, shipperId)
- cancelOrder(orderId, staffId, reason) → restore stock if confirmed
- addInternalNote(orderId, note)
- getProcessedHistory(staffId, from, to)

### 7.4 service/InventoryService.java
- checkStock(productId, quantity) → boolean
- deductStock(productId, quantity, orderId)
- restoreStock(productId, quantity, orderId)

### 7.5 controller/StaffController.java
- GET /api/staff/orders/pending
- GET /api/staff/orders/{orderId}
- PUT /api/staff/orders/{orderId}/confirm
- PUT /api/staff/orders/{orderId}/ready
- PUT /api/staff/orders/{orderId}/assign-shipper
- PUT /api/staff/orders/{orderId}/cancel
- PUT /api/staff/orders/{orderId}/internal-note
- GET /api/staff/orders/processed

---

## Phase 8: Shipper

### 8.1 service/ShipperService.java
- getAssignedOrders(shipperId)
- acceptOrder(orderId, shipperId)
- deliverSuccess(orderId, shipperId, codCollected)
- deliverFail(orderId, shipperId, reason)
- getDeliveryHistory(shipperId, status)

### 8.2 controller/ShipperController.java
- GET /api/shipper/orders/assigned
- PUT /api/shipper/orders/{orderId}/accept
- PUT /api/shipper/orders/{orderId}/deliver-success
- PUT /api/shipper/orders/{orderId}/deliver-fail
- GET /api/shipper/orders/history

---

## Phase 9: Admin

### 9.1 entity/WorkShift.java / Schedule.java / Review.java

### 9.2 repository/WorkShiftRepository.java / ScheduleRepository.java / ReviewRepository.java

### 9.3 service/AdminService.java
- User management: list, create, update, lock
- Product management: create, update, delete
- Category management: CRUD
- Schedule management: create shift, assign staff
- Ingredient management: import, adjust, transactions
- Statistics: dashboard, revenue, top products, performance
- Report export

### 9.4 controller/AdminController.java
- GET/POST/PUT /api/admin/users/**
- POST/PUT/DELETE /api/admin/products/**
- POST/PUT/DELETE /api/admin/categories/**
- GET/POST/PUT/DELETE /api/admin/ingredients/**
- GET/POST/DELETE /api/admin/work-shifts/**
- GET/POST/DELETE /api/admin/schedules/**
- GET /api/admin/orders/**
- GET /api/admin/statistics/**
- GET /api/admin/reports/export

---

## Phase 10: Frontend (sau khi backend ổn định)

### 10.1 Initialize project
- `npm create vue@latest` (hoặc thủ công: package.json, vite.config.js, main.js, App.vue)
- `npm install pinia axios vue-router bootstrap@5`

### 10.2 src/types/*.ts
- Interface cho từng entity (IUser, IProduct, IOrder, ...)

### 10.3 src/services/*.ts
- http.ts (Axios instance, interceptor cho JWT + X-Session-Id)
- authService.ts, productService.ts, cartService.ts, orderService.ts, ...

### 10.4 src/stores/*.ts
- authStore.ts (token, user info, login/logout)
- cartStore.ts (items, total, add/remove/update)
- orderStore.ts

### 10.5 src/router/index.ts
- Routes: /, /menu, /product/:id, /cart, /checkout, /orders, /login, /register
- Guards: auth(), role()

### 10.6 src/views/*.vue
- Pages cho từng role

---

## Sequence: từng Phase triển khai

```
Phase 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → [10]
```
Mỗi Phase được code và test bằng Bruno trước khi chuyển phase tiếp theo.
