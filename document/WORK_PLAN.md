# WORK PLAN — Sửa lỗi toàn diện

## Mục tiêu

Sửa các lỗi API mismatch, field name sai, thiếu endpoint, và dọn dẹp code chết.

## Phân công theo nhóm

| Nhóm | Mức độ | Số lỗi | Mô tả |
|---|---|---|---|
| **A** | CRITICAL | 8 | Gây crash hoặc mất dữ liệu |
| **B** | MODERATE | 8 | Sai field name, thiếu field |
| **C** | CLEANUP | 4 | Xóa file entity/DAO cũ |
| **D** | FEATURE | 15 | Thiếu backend endpoint |

---

## File danh sách đầy đủ

```
document/WORK_PLAN.md
```

---

## Nhóm A — Critical (sửa ngay)

### A1. `stores/staff.js` — thiếu checkIn/checkOut

**Vấn đề:** `views/staff/ShiftsPage.vue` gọi `staffStore.checkIn()` / `checkOut()` nhưng store không có method này → crash.

**Sửa:** Thêm vào `stores/staff.js`:

```js
async function checkIn() {
  try { return await staffApi.checkIn(); } catch { return null; }
}
async function checkOut() {
  try { return await staffApi.checkOut(); } catch { return null; }
}
```

Thêm vào `return`: `checkIn, checkOut`

---

### A2. `guest/TrackOrderPage.vue` — thiếu await

**Vấn đề:** Dòng 19: `const result = orderStore.trackOrder(orderCode.value)` — thiếu `await` → Promise luôn truthy → tracking luôn "thành công".

**Sửa:**

```js
const result = await orderStore.trackOrder(orderCode.value);
```

---

### A3. `user/OrderReviewPage.vue` — async trong computed

**Vấn đề:** `computed(() => orderStore.fetchById(route.params.id))` — async không reactive trong computed.

**Sửa:**

```js
const order = ref(null);
onMounted(async () => {
  order.value = await orderStore.fetchById(route.params.id);
});
```

Bỏ dòng `const order = computed(...)` cũ.

---

### A4. `admin/RevenueReportPage.vue` — thiếu fetchDashboard

**Vấn đề:** Đọc `adminStore.dashboard` trực tiếp nhưng không gọi `fetchDashboard()` → crash nếu vào trang trực tiếp.

**Sửa:** Thêm vào `onMounted`:

```js
onMounted(async () => {
  await adminStore.fetchDashboard();
});
```

---

### A5. `admin/TopProductsReportPage.vue` — thiếu fetchDashboard

**Sửa:** Giống A4 — thêm `onMounted` gọi `adminStore.fetchDashboard()`.

---

### A6. `stores/auth.js` — updateProfile không gọi API

**Vấn đề:** Chỉ `Object.assign(user.value, data)` local, không gọi backend → mất dữ liệu.

**Sửa:**

```js
async function updateProfile(data) {
  try {
    await authApi.updateProfile(data);
    Object.assign(user.value, data);
  } catch {
    throw new Error('Cập nhật thất bại');
  }
}
```

---

### A7. `stores/order.js` — reviewOrder không gọi API

**Vấn đề:** Chỉ update local, không gọi `orderApi.review()` → review không lưu.

**Sửa:**

```js
async function reviewOrder(id, data) {
  try {
    await orderApi.review(id, data);
    const order = currentOrder.value;
    if (order) { order.review = data; }
  } catch {
    throw new Error('Gửi đánh giá thất bại');
  }
}
```

---

### A8. `stores/product.js` + `ProductServlet.java` — phantom fields

**Vấn đề:** Store đọc `discountPrice`, `rating`, `reviewCount`, `inStock`, `featured` nhưng backend không trả → luôn null/0/false.

**Sửa frontend** (`stores/product.js` — mapProduct):

```js
discountPrice: p.discountPrice || null,
rating: p.rating || 0,
reviewCount: p.reviewCount || 0,
inStock: p.inStock !== undefined ? p.inStock : (p.status === 'AVAILABLE'),
featured: p.featured || false,
```

**Sửa backend** (`ProductServlet.java` — toMap):

```java
m.put("discountPrice", null);
m.put("rating", 0);
m.put("reviewCount", 0);
m.put("inStock", "AVAILABLE".equals(p.getStatus()));
m.put("featured", false);
```

Tương tự với `AdminProductServlet.java` — toMap.

---

## Nhóm B — Moderate (sai field name)

### B1. `user/CheckoutPage.vue` — addr.ward → addr.wardName

| Dòng | Cũ | Mới |
|---|---|---|
| 230 | `addr.ward` | `addr.wardName` |

Dòng 230 đang dùng `addr.street, addr.ward, addr.city`. Đổi `addr.ward` → `addr.wardName`.

### B2. `user/ProfilePage.vue` — addr.ward → addr.wardName

| Dòng | Cũ | Mới |
|---|---|---|
| 102 | `addr.ward` | `addr.wardName` |
| 106 | `addr.ward` | `addr.wardName` |

### B3. `stores/staff.js` — thiếu updatedAt

Thêm vào `mapOrderListItem()`:

```js
updatedAt: o.updatedAt || o.confirmedAt || o.createdAt,
```

### B4. `ShipperServlet.java` — items rỗng

**Vấn đề:** `toDetail()` trả `data.items = new ArrayList<>()`, không load OrderItem từ DB.

**Sửa:**

```java
List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
    .stream()
    .map(oi -> {
        Map<String, Object> im = new HashMap<>();
        im.put("productId", oi.getProduct().getProductId());
        im.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
        im.put("productName", oi.getProductName());
        im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
        im.put("quantity", oi.getQuantity());
        im.put("unitPrice", oi.getUnitPrice());
        im.put("totalPrice", oi.getTotalPrice());
        return im;
    })
    .collect(Collectors.toList());
data.put("items", items);
```

### B5. `stores/order.js` — fetchById logic sai

**Sửa:**

```js
async function fetchById(id) {
  if (currentOrder.value?.id === Number(id)) return currentOrder.value;
  await fetchDetail(id);
  return currentOrder.value;
}
```

### B6. `admin/ShiftsPage.vue` — u.name → u.fullName

| Dòng | Cũ | Mới |
|---|---|---|
| 248 | `u.name` | `u.fullName` |
| 248 | `u.role` | `u.roleName` |

### B7. `user/CheckoutPage.vue` — thiếu variantName

Thêm vào template trong vòng lặp item:

```html
<div v-if="item.variantName" class="item-variant">
  {{ item.variantName }}
</div>
```

### B8. `AdminService.java` — thiếu pendingOrders

Thêm vào `getDashboard()`:

```java
data.put("pendingOrders", ordersDAO.countByStatus("PENDING"));
```

---

## Nhóm C — Cleanup (xóa file cũ)

Xóa các file không còn dùng:

| File | Lý do |
|---|---|
| `entity/Ingredient.java` | Bảng `Ingredient` không tồn tại |
| `entity/ProductIngredient.java` | Bảng `ProductIngredient` không tồn tại |
| `entity/ProductOption.java` | Bảng `ProductOption` không tồn tại |
| `dao/IngredientDAO.java` | DAO duy nhất references Ingredient — không còn dùng |

---

## Nhóm D — Thiếu backend endpoint

| STT | API | Servlet | Hành động |
|---|---|---|---|
| D1 | `PUT /auth/change-password` | `AuthServlet.java` | Thêm handler trong doPut |
| D2 | `POST /auth/forgot-password` | `AuthServlet.java` | Thêm handler trong doPost |
| D3 | `POST /auth/reset-password` | `AuthServlet.java` | Thêm handler trong doPost |
| D4 | `POST /auth/cart/migrate` | `AuthServlet.java` | Thêm handler trong doPost |
| D5 | `GET/POST/DELETE /user/favorites` | `FavoritesServlet.java` | Tạo mới |
| D6 | `GET /user/vouchers` | `VoucherServlet.java` | Tạo mới |
| D7 | `POST /orders/{id}/review` | `OrderServlet.java` | Thêm handler trong doPost |
| D8 | `GET /admin/orders` | `AdminOrderServlet.java` | Tạo mới |
| D9 | `GET/POST/PUT/DELETE /admin/shifts` | `AdminShiftServlet.java` | Tạo mới |
| D10 | `GET/POST /admin/schedules` | `AdminScheduleServlet.java` | Tạo mới |
| D11 | `GET /admin/reports/revenue` | `AdminServlet.java` | Thêm handler |
| D12 | `GET /admin/reports/top-products` | `AdminServlet.java` | Thêm handler |
| D13 | `PUT /staff/orders/{id}/assign` | `StaffOrderServlet.java` | Thêm handler |
| D14 | `POST /staff/orders/{id}/notes` | `StaffOrderServlet.java` | Thêm handler |
| D15 | `GET /staff/orders/export` | `StaffOrderServlet.java` | Thêm handler |
| D16 | `POST/GET /staff/shifts/*` | `StaffShiftServlet.java` | Tạo mới |
| D17 | `GET /products/featured` | `ProductServlet.java` | Thêm handler |

---

## Thứ tự triển khai

```text
Bước 1: Nhóm A (8 lỗi critical)
Bước 2: Nhóm B (8 lỗi moderate)
Bước 3: Nhóm C (xóa 4 file)
Bước 4: Nhóm D (tạo/handler endpoint)
Bước 5: Backend compile + Frontend build
```

## Lệnh kiểm tra

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```
