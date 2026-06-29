# WORK PLAN — Sửa lỗi toàn diện (3 ngày)

## Mục tiêu

Fix 25 lỗi từ báo cáo kiểm tra, chia làm 3 ngày, mỗi ngày 8-9 lỗi.

---

## Phân công

| Ngày | Số lỗi | Trọng tâm |
|---|---|---|
| Ngày 1 | 9 | Critical (crash) + backend zoneId |
| Ngày 2 | 9 | High (sai dữ liệu, thiếu chức năng) |
| Ngày 3 | 7 | Medium + Dead code cleanup |

---

## Ngày 1 — Critical (9 lỗi)

### 1.1 Admin RevenueReportPage — crash vì null dashboard

**File:** `frontend/src/views/admin/RevenueReportPage.vue`

**Vấn đề:** `const data = adminStore.dashboard` → null → crash khi mount.

**Sửa:**
```js
// Dòng 10: const data = adminStore.dashboard;
// Đổi thành:
const data = computed(() => adminStore.dashboard || {
  totalRevenue: 0, totalOrders: 0, revenueToday: 0, ordersToday: 0,
  revenueByMonth: [], topProducts: [],
});
```

Và thêm `onMounted`:
```js
onMounted(async () => {
  await adminStore.fetchDashboard();
});
```

---

### 1.2 Admin TopProductsReportPage — crash vì null dashboard

**File:** `frontend/src/views/admin/TopProductsReportPage.vue`

**Sửa:** Tương tự 1.1 — thêm computed default + `onMounted` gọi `fetchDashboard()`.

---

### 1.3 Staff OrderHistoryPage — không fetch orders

**File:** `frontend/src/views/staff/OrderHistoryPage.vue`

**Vấn đề:** Không có `onMounted` → không gọi API → danh sách trống.

**Sửa:** Thêm:
```js
onMounted(async () => {
  await staffStore.fetchOrders();
});
```

---

### 1.4 Admin store — sai tên method zone (3 lỗi)

**File:** `frontend/src/stores/admin.js`

| Dòng | Sai | Đúng |
|---|---|---|
| 236 | `adminApi.createZone(data)` | `adminApi.createDeliveryZone(data)` |
| 244 | `adminApi.updateZone(id, data)` | `adminApi.updateDeliveryZone(id, data)` |
| 252 | `adminApi.deleteZone(id)` | `adminApi.deleteDeliveryZone(id)` |

---

### 1.5 Admin store — gọi method không tồn tại

**File:** `frontend/src/stores/admin.js` dòng 285

**Vấn đề:** Gọi `adminApi.createSchedule(data)` nhưng API không có method này.

**Sửa:** Thêm vào `frontend/src/api/admin.js`:
```js
createSchedule(data) { return client.post('/admin/schedules', data); },
updateSchedule(id, data) { return client.put(`/admin/schedules/${id}`, data); },
deleteSchedule(id) { return client.delete(`/admin/schedules/${id}`); },
```

---

### 1.6 Staff export — sai HTTP method

**File:** `frontend/src/api/staff.js` dòng 44

**Vấn đề:** `exportOrders()` dùng GET, backend StaffOrderServlet xử lý export trong `doPut`.

**Sửa:** Đổi GET thành PUT, và thêm handler trong backend `StaffOrderServlet.doGet()`:
```java
// Trong doGet, thêm case cho "/export"
} else if (path.equals("/export")) {
    // Gọi logic export CSV
}
```

Hoặc đổi frontend sang PUT:
```js
exportOrders(params) {
  return client.put('/staff/orders/export', null, { params, responseType: 'blob' });
},
```

---

### 1.7 Backend OrderService — zoneId không được lưu

**File:** `Backend/.../service/OrderService.java`

**Vấn đề:** Param `zoneId` không được set vào order.

**Sửa:**
```java
// Thêm import
import dao.DeliveryZoneDAO;
// Thêm field
private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();
// Trong checkout(), sau khi tính shippingFee:
DeliveryZone zone = zoneId > 0 ? deliveryZoneDAO.findById(zoneId) : null;
order.setZone(zone);
order.setShippingProvider(zone != null ? "FALLBACK_ZONE" : "NONE");
```

---

### 1.8 Kiểm tra đầu ngày 1

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

---

## Ngày 2 — High (9 lỗi)

### 2.1 Product API — sai param category

**File:** `frontend/src/api/product.js` dòng 11

**Vấn đề:** Gửi param `category` nhưng backend đọc `categoryId`.

**Sửa:**
```js
// Đổi
getByCategory(slug) { return client.get('/products', { params: { category: slug } }); }
// Thành
getByCategory(categoryId) { return client.get('/products', { params: { categoryId } }); }
```

---

### 2.2 Cart — updateQuantity không sync server

**File:** `frontend/src/stores/cart.js` hàm `updateQuantity()`

**Vấn đề:** Chỉ edit localStorage, không gọi API khi logged in.

**Sửa:**
```js
async function updateQuantity(productId, variantId, quantity) {
  const key = itemKey(productId, variantId);
  const item = items.value.find((i) => i.key === key);
  if (!item) return;
  if (quantity <= 0) {
    removeItem(productId, variantId);
    return;
  }
  item.quantity = quantity;
  save();
  // Nếu logged in, sync lên server
  const auth = useAuthStore();
  if (auth.isLoggedIn && item.cartItemId) {
    try { await cartApi.updateItem(item.cartItemId, { quantity }); }
    catch (e) { console.error('Sync cart failed:', e); }
  }
}
```

---

### 2.3 Auth — forgotPassword không gọi API

**File:** `frontend/src/stores/auth.js` dòng 84-86

**Sửa:**
```js
async function forgotPassword(email) {
  try {
    const res = await authApi.forgotPassword(email);
    return res;
  } catch (e) {
    throw new Error('Không thể gửi yêu cầu đặt lại mật khẩu');
  }
}
```

---

### 2.4 User OrdersPage — item.image luôn undefined

**File:** `frontend/src/stores/order.js` hàm `mapOrderListItem`

**Vấn đề:** Backend `toListItem` trả `items: new ArrayList<>()` rỗng.

**Sửa:** Cập nhật `OrderServlet.toListItem()` để trả items có productName + quantity:
```java
// Thêm query items
List<Map<String, Object>> itemList = orderItemDAO.findByOrderId(o.getOrderId())
    .stream().map(oi -> {
        Map<String, Object> im = new HashMap<>();
        im.put("productName", oi.getProductName());
        im.put("quantity", oi.getQuantity());
        im.put("image", oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
        return im;
    }).collect(Collectors.toList());
m.put("items", itemList);
```

---

### 2.5 Staff ShiftsPage — shiftStatus không được update

**File:** `frontend/src/stores/staff.js` (checkIn/checkOut) + `views/staff/ShiftsPage.vue`

**Sửa store:**
```js
async function checkIn() {
  try {
    const res = await staffApi.checkIn();
    if (res) shiftStatus.value = { current: res, history: [] };
    return res;
  } catch { return null; }
}
async function checkOut() {
  try {
    const res = await staffApi.checkOut();
    if (res) shiftStatus.value = { current: null, history: res.history || [] };
    return res;
  } catch { return null; }
}
```

**Sửa ShiftsPage.vue dòng 42:**
```html
<span class="badge badge-{{ s.status?.toLowerCase() }}">{{ SCHEDULE_STATUS_LABEL[s.status] || s.status }}</span>
```

---

### 2.6 Admin ShiftsPage — users dropdown rỗng

**File:** `frontend/src/views/admin/ShiftsPage.vue`

**Sửa:** Thêm vào `onMounted`:
```js
onMounted(async () => {
  await Promise.all([
    adminStore.fetchShifts(),
    adminStore.fetchSchedules(),
    adminStore.fetchUsers(),  // THÊM DÒNG NÀY
  ]);
});
```

---

### 2.7 Admin API — thiếu schedule methods

**File:** `frontend/src/api/admin.js`

**Sửa:** Thêm:
```js
createSchedule(data) { return client.post('/admin/schedules', data); },
updateSchedule(id, data) { return client.put(`/admin/schedules/${id}`, data); },
deleteSchedule(id) { return client.delete(`/admin/schedules/${id}`); },
```

---

### 2.8 Variant update/delete URL sai

**Vấn đề:** Frontend gọi `PUT/DELETE /api/admin/variants/{id}` nhưng `AdminProductServlet` mapping là `/api/admin/products/*`.

**Sửa:** Tạo servlet riêng `AdminVariantServlet.java` mapping `/api/admin/variants/*`:

```java
@WebServlet("/api/admin/variants/*")
public class AdminVariantServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // update variant
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // delete variant
    }
}
```

---

### 2.9 Kiểm tra đầu ngày 2

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

---

## Ngày 3 — Medium + Cleanup (7 lỗi)

### 3.1 StaffOrderServlet — history thiếu status

**File:** `Backend/.../servlet/StaffOrderServlet.java` dòng 82-88

**Sửa:** Thêm CONFIRMED, PREPARING, PICKED_UP, DELIVERED vào history:

```java
} else if (path.equals("/history")) {
    List<Orders> all = new java.util.ArrayList<>();
    for (String s : new String[]{"READY", "DELIVERED", "CANCELLED", "CONFIRMED", "PREPARING"}) {
        all.addAll(ordersDAO.findByStatus(s));
    }
    all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    ApiResponse.ok(resp, all.stream().map(o -> toListItem(o)).collect(Collectors.toList()));
}
```

---

### 3.2 CheckoutPage — key trùng lặp

**File:** `frontend/src/views/user/CheckoutPage.vue` dòng 331

**Sửa:**
```html
v-for="item in cart.items" :key="item.key || item.productId + '_' + item.variantId"
```

---

### 3.3 Backend AdminCategoryServlet — productCount

**File:** `Backend/.../servlet/AdminCategoryServlet.java` dòng 42

**Sửa:** Tính productCount thực tế:
```java
// Thêm ProductDAO
private ProductDAO productDAO = new ProductDAO();
// Trong toMap:
long count = productDAO.findByCategoryId(c.getCategoryId()).size();
m.put("productCount", count);
```

---

### 3.4 Backend StaffOrderServlet — export GET handler

**File:** `Backend/.../servlet/StaffOrderServlet.java`

**Sửa:** Thêm handler trong `doGet`:
```java
} else if (path.equals("/export")) {
    // Copy logic export từ doPut sang
    exportOrders(req, resp);
}
```

---

### 3.5 Database seed — Payment amount sai

**File:** `database/init.sql` dòng ~550

**Sửa:**
```sql
-- Đổi
insert into Payment ... values (1, 177000, ...)
-- Thành
insert into Payment ... values (1, 186000, ...)
```

---

### 3.6 Dead code — xóa helpers/validators

**File:** `frontend/src/utils/helpers.js`, `frontend/src/utils/validators.js`

**Hành động:** Xóa 2 file không dùng đến.

---

### 3.7 Dead code — constants dư

**File:** `frontend/src/utils/constants.js`

**Sửa:** Xóa `INGREDIENT_UNITS`, `DELIVERY_ZONE_TYPES`.

---

### Kiểm tra cuối cùng

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
git add -A && git commit -m "fix all bugs - day 3"
git push origin develop
```

---

## Tổng kết

| Ngày | Số lỗi | Loại |
|---|---|---|
| Ngày 1 | 9 | Critical + Backend zoneId |
| Ngày 2 | 9 | High |
| Ngày 3 | 7 | Medium + Cleanup |
| **Tổng** | **25** | |
