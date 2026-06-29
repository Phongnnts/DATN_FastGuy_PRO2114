# WORK PLAN — FastGuy (5 ngày)

## Mục tiêu

Fix 41 lỗi/chức năng, chia 5 ngày:

| Ngày | Số mục | Trọng tâm |
|---|---|---|
| Ngày 1 | 8 | Backend + Database critical |
| Ngày 2 | 10 | Frontend core |
| Ngày 3 | 7 | Feature completion |
| Ngày 4 | 6 | Cleanup + Test |
| Ngày 5 | 10 | UI Polish (hiệu ứng, hover, shadow) |

---

## Ngày 1 — Backend + Database (8 mục)

### 1.1 Dashboard 0 — OrdersDAO.java

**File:** `Backend/.../dao/OrdersDAO.java` dòng 85, 116

**Lỗi:** `SELECT SUM(o.finalAmount)` trả về `BigDecimal` nhưng code dùng `Double.class` → ClassCastException → cả request dashboard fail → tất cả số = 0.

**Fix:**
```java
// Dòng 85: đổi
Double result = em.createQuery(... Double.class)...
// Thành
BigDecimal result = em.createQuery(... BigDecimal.class)...

// Dòng 116: tương tự
```

Sửa `AdminService.java`:
```java
// Dòng 19: double totalRevenue = ordersDAO.sumRevenue();
// Thành:
double totalRevenue = ordersDAO.sumRevenue() != null ? ordersDAO.sumRevenue().doubleValue() : 0;
// Dòng 38: tương tự
```

---

### 1.2 Gallery ảnh hỏng — ProductServlet.java

**File:** `Backend/.../servlet/ProductServlet.java` dòng 100-112

**Lỗi:** Gallery lưu JSON `["url1","url2"]`, code thêm cả chuỗi JSON làm 1 element → ảnh hỏng.

**Fix:** Parse JSON bằng ObjectMapper:
```java
private static final ObjectMapper mapper = new ObjectMapper();

List<String> galleryList = new ArrayList<>();
String gallery = p.getGalleryImages();
if (gallery != null && !gallery.isEmpty()) {
    try {
        galleryList = mapper.readValue(gallery, new TypeReference<List<String>>() {});
    } catch (Exception e) {
        for (String url : gallery.split(",")) {
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) galleryList.add(trimmed);
        }
    }
}
m.put("galleryImages", galleryList);
```

---

### 1.3 Track Order — OrderServlet.java

**File:** `Backend/.../servlet/OrderServlet.java` dòng 44-64

**Lỗi:** Endpoint `/track` yêu cầu auth, guest không tra cứu được.

**Fix:** Đưa case `/track` LÊN TRƯỚC `getUserId`:
```java
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/json;charset=UTF-8");
    String path = req.getPathInfo();

    // KHÔNG cần auth
    if ("/track".equals(path)) {
        String code = req.getParameter("code");
        if (code == null) { ApiResponse.error(resp, "Missing order code", 400); return; }
        List<Orders> allOrders = ordersDAO.findAll();
        Orders order = allOrders.stream()
            .filter(o -> code.equals(o.getOrderCode()))
            .findFirst().orElse(null);
        if (order == null) { ApiResponse.error(resp, "Order not found", 404); return; }
        ApiResponse.ok(resp, toDetail(order));
        return;
    }
    // Các endpoint còn lại cần auth
    int userId = getUserId(req, resp);
    if (userId < 0) return;
    // ... giữ nguyên
}
```

---

### 1.4 productCount — AdminCategoryServlet.java

**File:** `Backend/.../servlet/AdminCategoryServlet.java` dòng 42

**Fix:**
```java
private ProductDAO productDAO = new ProductDAO();
long count = productDAO.findByCategoryId(c.getCategoryId()).size();
m.put("productCount", count);
```

---

### 1.5 History thiếu status — StaffOrderServlet.java

**File:** `Backend/.../servlet/StaffOrderServlet.java` dòng 82-88

**Fix:**
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

### 1.6 AdminUserServlet — check duplicate phone

**File:** `Backend/.../servlet/AdminUserServlet.java`

**Fix:** Thêm sau check email:
```java
if (userDAO.findByPhone(phone) != null) {
    ApiResponse.error(resp, "Số điện thoại đã tồn tại", 400);
    return;
}
```

---

### 1.7 AdminVariantServlet — thiếu fields

**File:** `Backend/.../servlet/AdminVariantServlet.java`

**Fix:** Thêm vào doPut:
```java
if (body.containsKey("originalPrice"))
    v.setOriginalPrice(BigDecimal.valueOf(((Number) body.get("originalPrice")).doubleValue()));
if (body.containsKey("sku")) v.setSku((String) body.get("sku"));
if (body.containsKey("quantityAvailable"))
    v.setQuantityAvailable(((Number) body.get("quantityAvailable")).intValue());
if (body.containsKey("weight"))
    v.setWeight(BigDecimal.valueOf(((Number) body.get("weight")).doubleValue()));
if (body.containsKey("length"))
    v.setLength(BigDecimal.valueOf(((Number) body.get("length")).doubleValue()));
if (body.containsKey("width"))
    v.setWidth(BigDecimal.valueOf(((Number) body.get("width")).doubleValue()));
if (body.containsKey("height"))
    v.setHeight(BigDecimal.valueOf(((Number) body.get("height")).doubleValue()));
```

---

### 1.8 Payment seed data sai

**File:** `database/init.sql` dòng 550

**Fix:**
```sql
-- Đổi 177000 → 186000
insert into Payment (order_id, amount, payment_method, transaction_id, status, paid_at, shipper_id, collected_at)
values (1, 186000, 'CASH', null, 'COMPLETED', '2025-06-01 11:10:00', 4, '2025-06-01 11:10:00');
```

### Kiểm tra ngày 1

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
```

---

## Ngày 2 — Frontend Core (10 mục)

### 2.1 Store sai tên method zone

**File:** `frontend/src/stores/admin.js` dòng 234-256

**Fix 3 dòng:**
```js
// Dòng 236: adminApi.createZone(data) → adminApi.createDeliveryZone(data)
// Dòng 244: adminApi.updateZone(id, data) → adminApi.updateDeliveryZone(id, data)
// Dòng 252: adminApi.deleteZone(id) → adminApi.deleteDeliveryZone(id)
```

---

### 2.2 Store thiếu schedule methods

**File:** `frontend/src/api/admin.js`

**Thêm:**
```js
createSchedule(data) { return client.post('/admin/schedules', data); },
updateSchedule(id, data) { return client.put(`/admin/schedules/${id}`, data); },
deleteSchedule(id) { return client.delete(`/admin/schedules/${id}`); },
```

---

### 2.3 Auth store — forgotPassword

**File:** `frontend/src/stores/auth.js` dòng 84-91

**Fix:**
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

### 2.4 Cart store — updateQuantity sync server

**File:** `frontend/src/stores/cart.js` hàm `updateQuantity()`

**Fix:**
```js
async function updateQuantity(productId, variantId, quantity) {
  const key = itemKey(productId, variantId);
  const item = items.value.find((i) => i.key === key);
  if (!item) return;
  if (quantity <= 0) { removeItem(productId, variantId); return; }
  item.quantity = quantity;
  save();
  const auth = useAuthStore();
  if (auth.isLoggedIn && item.cartItemId) {
    try { await cartApi.updateItem(item.cartItemId, { quantity }); }
    catch (e) { console.error('Sync cart failed:', e); }
  }
}
```

---

### 2.5 Staff store — fetchHistory

**File:** `frontend/src/stores/staff.js`

**Thêm:**
```js
async function fetchHistory() {
  loading.value = true;
  try {
    const data = await staffApi.getOrderHistory();
    allOrders.value = Array.isArray(data) ? data.map(mapOrderListItem) : [];
    return allOrders.value;
  } catch { return []; }
  finally { loading.value = false; }
}
```
Thêm vào `return`: `fetchHistory`

---

### 2.6 OrderHistoryPage — gọi đúng API

**File:** `frontend/src/views/staff/OrderHistoryPage.vue` dòng 20

**Fix:** `await staffStore.fetchOrders()` → `await staffStore.fetchHistory()`

---

### 2.7 CheckoutPage — GHN provinces + default HCMC

**File:** `frontend/src/views/user/CheckoutPage.vue` dòng 39-56

**Fix:**
```js
onMounted(async () => {
  try {
    const [provData, addrData] = await Promise.all([
      shippingApi.getProvinces(),
      userApi.getAddresses(),
    ]);
    provinces.value = (provData || []).map(p => ({
      id: p.ProvinceID || p.province_id || p.provinceId,
      name: p.ProvinceName || p.province_name || p.provinceName,
    }));
    // Auto-select TP.HCM
    const hcm = provinces.value.find(p => p.name?.includes('Hồ Chí Minh'));
    if (hcm) selectedProvince.value = hcm.id;
    
    savedAddresses.value = addrData || [];
    const defaultAddr = savedAddresses.value.find(a => a.isDefault);
    if (defaultAddr) selectAddress(defaultAddr);
  } catch {
    provinces.value = []; savedAddresses.value = [];
  }
});
```

---

### 2.8 CheckoutPage — key trùng lặp

**File:** `frontend/src/views/user/CheckoutPage.vue` dòng 331

**Fix:**
```html
v-for="item in cart.items" :key="item.key || item.productId + '_' + item.variantId"
```

---

### 2.9 Staff ShiftsPage — shiftStatus

**File:** `frontend/src/stores/staff.js` + `views/staff/ShiftsPage.vue`

**Fix store:**
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

**Fix ShiftsPage.vue dòng 42:**
```html
<span class="badge badge-{{ s.status?.toLowerCase() }}">{{ SCHEDULE_STATUS_LABEL[s.status] || s.status }}</span>
```

---

### 2.10 Admin ShiftsPage — users dropdown

**File:** `frontend/src/views/admin/ShiftsPage.vue`

**Fix onMounted:**
```js
onMounted(async () => {
  await Promise.all([
    adminStore.fetchShifts(),
    adminStore.fetchSchedules(),
    adminStore.fetchUsers(),
  ]);
});
```

### Kiểm tra ngày 2

```bash
cd Frontend && npm run build
```

---

## Ngày 3 — Feature Completion (7 mục)

### 3.1 MenuPage — search + sort

**File:** `frontend/src/views/guest/MenuPage.vue`

**Thêm search mô tả:**
```js
if (searchInput.value) {
  const q = searchInput.value.toLowerCase();
  result = result.filter((p) =>
    p.name.toLowerCase().includes(q) ||
    (p.description && p.description.toLowerCase().includes(q))
  );
}
```

**Thêm sort:**
```js
const sortBy = ref('name');
if (sortBy.value === 'price-asc') result.sort((a,b) => a.price - b.price);
if (sortBy.value === 'price-desc') result.sort((a,b) => b.price - a.price);
if (sortBy.value === 'name') result.sort((a,b) => a.name.localeCompare(b.name));
```

**Fix route.query.category (dòng 58-65):**
```js
onMounted(async () => {
  if (!productStore.fetched) await productStore.init();
  if (route.query.category) {
    const cat = productStore.allCategories.find(
      (c) => c.id === Number(route.query.category)
    );
    if (cat) activeCategory.value = cat.id;
  }
});
```

---

### 3.2 CheckoutPage — chuyển khoản

**File:** `frontend/src/views/user/CheckoutPage.vue`

**Thêm template sau payment selector:**
```html
<div v-if="paymentMethod === 'BANK_TRANSFER'" class="card mb-3">
  <h3>Thông tin chuyển khoản</h3>
  <div style="padding:16px; background:#f8f9fa; border-radius:8px">
    <p><strong>Ngân hàng:</strong> MB Bank</p>
    <p><strong>Số tài khoản:</strong> 6513527</p>
    <p><strong>Chủ tài khoản:</strong> FastGuy</p>
    <p><strong>Nội dung:</strong> Mã đơn hàng + SĐT</p>
    <p style="color:var(--text-mid);font-size:13px">Sau khi chuyển khoản, vui lòng chờ xác nhận</p>
  </div>
</div>
```

---

### 3.3 Admin Dashboard — reactive

**File:** `frontend/src/views/admin/DashboardPage.vue`

**Fix dòng 14-27 dùng computed:**
```js
const data = computed(() => adminStore.dashboard || {
  totalUsers: 0, totalOrders: 0, totalProducts: 0, totalRevenue: 0,
  pendingOrders: 0, ordersToday: 0, revenueToday: 0,
  revenueByMonth: [], topProducts: [], ordersByStatus: {},
});
```

---

### 3.4 RevenueReportPage — reactive

**File:** `frontend/src/views/admin/RevenueReportPage.vue`

**Fix:**
```js
const data = computed(() => adminStore.dashboard || {
  totalRevenue: 0, totalOrders: 0, revenueToday: 0, ordersToday: 0,
  revenueByMonth: [], topProducts: [],
});
const filteredData = ref([]);
watch(() => data.value?.revenueByMonth, (val) => {
  filteredData.value = val || [];
}, { immediate: true });
```

---

### 3.5 TopProductsReportPage — reactive

**File:** `frontend/src/views/admin/TopProductsReportPage.vue`

**Fix tương tự:** dùng `computed` + `watch`.

---

### 3.6 Backend — Schedule/Shift CRUD

**File:** `AdminScheduleServlet.java`, `AdminShiftServlet.java`, `StaffShiftServlet.java`

Implement CRUD thực tế sử dụng ScheduleDAO, WorkShiftDAO (hiện tại trả list rỗng).

---

### 3.7 Backend — Favorites/Voucher

**File:** `FavoritesServlet.java`, `VoucherServlet.java`

Implement lưu favorites vào DB (hiện tại trả list rỗng).

### Kiểm tra ngày 3

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

---

## Ngày 4 — Cleanup + Test (6 mục)

### 4.1 Xóa helpers.js

**File:** `frontend/src/utils/helpers.js` — zero imports → xóa

### 4.2 Xóa validators.js

**File:** `frontend/src/utils/validators.js` — zero imports → xóa

### 4.3 Xóa constants chết

**File:** `frontend/src/utils/constants.js` — xóa `SHIFT_TYPES` (dòng 93)

### 4.4 Ẩn Admin DeliveryZones

**File:** `frontend/src/router/index.js` — comment route `/admin/delivery-zones`

### 4.5 Kiểm tra 3 backend servlet

Xác nhận `AdminScheduleServlet`, `AdminShiftServlet`, `StaffShiftServlet` đã implement từ ngày 3.

### 4.6 Kiểm tra cuối

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

Test full flow:
1. Menu → chọn variant → Add cart
2. Checkout → HCMC → quận → phường → tính phí → đặt hàng
3. Staff → confirm → preparing → ready
4. Shipper → pickup → deliver
5. Admin dashboard → số liệu
6. Guest Track Order → nhập mã → timeline

---

## Ngày 5 — UI Polish (10 mục)

### 5.1 CSS variables

**File:** `frontend/src/assets/styles/variables.css`

**Thêm:**
```css
--shadow-sm: 0 1px 3px rgba(0,0,0,0.08);
--shadow-md: 0 4px 12px rgba(0,0,0,0.1);
--shadow-lg: 0 8px 24px rgba(0,0,0,0.12);
--radius-lg: 12px;
--transition-fast: 0.15s ease;
--transition-normal: 0.25s ease;
```

---

### 5.2 Global CSS — card shadow + table hover + buttons

**File:** `frontend/src/assets/styles/global.css`

**Sửa `.card`:**
```css
.card {
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast);
}
.card:hover { box-shadow: var(--shadow-md); }
```

**Thêm table row hover:**
```css
.table tbody tr:hover {
  background: #f8f9fa; cursor: pointer;
}
```

**Thêm button variants (xóa private):**
```css
.btn-success { background: #10b981; color: #fff; border: none; padding: 8px 16px; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; }
.btn-success:hover { background: #059669; }
.btn-danger { background: #ef4444; color: #fff; border: none; padding: 8px 16px; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; }
.btn-danger:hover { background: #dc2626; }
```

---

### 5.3 HomePage — about cards

**File:** `frontend/src/views/guest/HomePage.vue`

```css
.about-card {
  transition: all var(--transition-normal); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 24px;
}
.about-card:hover {
  box-shadow: var(--shadow-md); transform: translateY(-4px);
  border-color: var(--primary);
}
```

---

### 5.4 ProductCard — hover

**File:** `frontend/src/components/common/ProductCard.vue`

```css
.product-card { transition: all var(--transition-normal); border-radius: var(--radius); overflow: hidden; }
.product-card:hover { box-shadow: var(--shadow-lg); transform: translateY(-4px); }
.product-card img { transition: transform var(--transition-normal); }
.product-card:hover img { transform: scale(1.05); }
```

---

### 5.5 ProductDetail — image zoom

**File:** `frontend/src/views/guest/ProductDetailPage.vue`

```css
.detail-image-bg { border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); }
.detail-image-bg img { transition: transform var(--transition-normal); }
.detail-image-bg:hover img { transform: scale(1.08); }
.gallery-thumb { border-radius: var(--radius-sm); transition: all var(--transition-fast); border: 2px solid transparent; }
.gallery-thumb:hover { border-color: var(--primary); box-shadow: var(--shadow-sm); }
```

---

### 5.6 Xóa style trùng lặp

Xóa private modal/button classes khỏi:
- `ProfilePage.vue`
- `Staff/OrderDetailPage.vue`
- `Shipper/OrderDetailPage.vue`
- `Admin/ProductsPage.vue`

Các class này đã có trong `global.css`.

---

### 5.7 Staff OrdersPage — row hover

**File:** `frontend/src/views/staff/OrdersPage.vue`

```css
.table tbody tr { cursor: pointer; transition: background var(--transition-fast); }
.table tbody tr:hover { background: #f0f4f8; }
```

---

### 5.8 StarRating — hover

**File:** `frontend/src/components/common/StarRating.vue`

```css
.star { cursor: pointer; transition: all var(--transition-fast); }
.star:hover { transform: scale(1.2); }
```

---

### 5.9 Loading skeleton

**File:** `frontend/src/assets/styles/global.css`

```css
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}
```

---

### 5.10 Checkout — sticky sidebar + responsive

**File:** `frontend/src/views/user/CheckoutPage.vue`

```css
.checkout-sidebar .card { position: sticky; top: 24px; box-shadow: var(--shadow-md); }
@media (max-width: 768px) { .checkout-sidebar .card { position: static; } }
```

### Kiểm tra ngày 5

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

---

## Tổng kết

| Ngày | Số mục | Loại |
|---|---|---|
| 1 | 8 | Backend + Database |
| 2 | 10 | Frontend core |
| 3 | 7 | Feature completion |
| 4 | 6 | Cleanup + Test |
| 5 | 10 | UI Polish |
| **Tổng** | **41** | |
