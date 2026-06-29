# WORK PLAN — FastGuy (Hoàn thiện toàn diện)

## Mục tiêu

Hoàn thiện dự án FastGuy với 5 phase, bao gồm fix lỗi, tích hợp SePay QR, nâng cấp Staff, UI/UX, và cleanup.

---

## Phase 1 — Fix lỗi hiện tại

### 1.1 GHN API không hoạt động

**File:** `Backend/.../utils/AppConfig.java` dòng 11

**Vấn đề:** Host đang dùng `dev-online-gateway.ghn.vn` (môi trường test) nhưng token là của production → GHN trả 401 → GhnClient bắt exception trả `[]` → dropdown tỉnh/huyện/xã rỗng.

**Fix:**
```java
// Dòng 11: đổi
GHN_HOST = "https://dev-online-gateway.ghn.vn";
// Thành
GHN_HOST = "https://online-gateway.ghn.vn";
```

---

### 1.2 HomePage — thiếu ảnh hero

**File:** `frontend/src/views/guest/HomePage.vue`

**Vấn đề:** Hero section chỉ có text trên gradient, không có ảnh sản phẩm. About section icon-only.

**Fix:** Thêm ảnh vào hero slides + about cards:
```html
<!-- Hero slide: thêm ảnh nền -->
<div class="hero-slide" :style="{ backgroundImage: 'url(https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg)' }">
```

**Checklist:**
- [ ] Hero slide 1: ảnh burger
- [ ] Hero slide 2: ảnh gà rán
- [ ] Hero slide 3: ảnh pizza
- [ ] About cards: thêm icon hover effect

---

### 1.3 CheckoutPage — loading state

**File:** `frontend/src/views/user/CheckoutPage.vue` dòng 32

**Vấn đề:** `loadingProvinces` được khai báo nhưng không dùng. User không thấy trạng thái đang tải.

**Fix:**
```js
// Dòng 39-56: onMounted set loadingProvinces = true
onMounted(async () => {
  loadingProvinces.value = true;
  try {
    const [provData, addrData] = await Promise.all([...]);
    ...
  } finally {
    loadingProvinces.value = false;
  }
});
```

**Template:**
```html
<select v-if="loadingProvinces" disabled><option>Đang tải dữ liệu...</option></select>
<select v-else v-model="selectedProvince">...</select>
```

---

## Phase 2 — SePay QR Code (6 mục)

### 2.1 Tạo SePayConfig.java

**File:** `Backend/.../utils/SePayConfig.java` (tạo mới)

```java
package utils;

public class SePayConfig {
    private static final String BANK_ACCOUNT = "6513527";
    private static final String BANK_CODE = "MB";
    private static final String QR_TEMPLATE = "compact";

    public static String buildQrUrl(long amount, String description) {
        return "https://qr.sepay.vn/img"
            + "?acc=" + BANK_ACCOUNT
            + "&bank=" + BANK_CODE
            + "&amount=" + amount
            + "&des=" + java.net.URLEncoder.encode(description)
            + "&template=" + QR_TEMPLATE;
    }
}
```

---

### 2.2 Tạo SePayService.java

**File:** `Backend/.../service/SePayService.java` (tạo mới)

```java
package service;

import dao.OrdersDAO;
import entity.Orders;
import utils.SePayConfig;

import java.time.LocalDateTime;
import java.util.Map;

public class SePayService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    public String generateQrUrl(int orderId, long amount, String orderCode) {
        String description = "TT " + orderCode;
        return SePayConfig.buildQrUrl(amount, description);
    }

    public boolean processWebhook(Map<String, Object> payload) {
        // Parse webhook từ SePay
        // Tìm order theo description
        // Update payment_status = 'PAID'
        // Ghi paid_at
        return true;
    }
}
```

---

### 2.3 Tạo SePayWebhookServlet.java

**File:** `Backend/.../servlet/SePayWebhookServlet.java` (tạo mới)

```java
package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.SePayService;
import utils.ApiResponse;
import utils.JsonUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payment/sepay-webhook")
public class SePayWebhookServlet extends HttpServlet {
    private SePayService sePayService = new SePayService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        boolean ok = sePayService.processWebhook(body);
        if (ok) {
            ApiResponse.ok(resp, null, "Payment confirmed");
        } else {
            ApiResponse.error(resp, "Processing failed", 500);
        }
    }
}
```

---

### 2.4 Sửa OrderService.java — trả QR URL

**File:** `Backend/.../service/OrderService.java`

Sau khi checkout thành công, nếu `paymentMethod = 'BANK_TRANSFER'`, trả thêm `sepayQrUrl`:

```java
// Thêm field
private SePayService sePayService = new SePayService();

// Trong checkout(), sau khi ordersDAO.save(order):
Map<String, Object> result = new HashMap<>();
result.put("orderId", order.getOrderId());
result.put("orderCode", order.getOrderCode());
result.put("status", order.getOrderStatus());
result.put("finalAmount", order.getFinalAmount());
if ("BANK_TRANSFER".equals(paymentMethod)) {
    result.put("sepayQrUrl", sePayService.generateQrUrl(
        order.getOrderId(),
        order.getFinalAmount().longValue(),
        order.getOrderCode()
    ));
}
return result; // Đổi kiểu trả về từ Orders → Map<String, Object>
```

---

### 2.5 Sửa OrderServlet.java — payment-status endpoint

**File:** `Backend/.../servlet/OrderServlet.java`

Thêm endpoint GET `/api/orders/{id}/payment-status`:

```java
// Trong doGet(), thêm case:
if (path != null && path.endsWith("/payment-status")) {
    String idStr = path.substring(1, path.length() - "/payment-status".length());
    int orderId = Integer.parseInt(idStr);
    Orders order = ordersDAO.findById(orderId);
    if (order == null) { ApiResponse.error(resp, "Not found", 404); return; }
    Map<String, Object> data = new HashMap<>();
    data.put("paymentStatus", order.getPaymentStatus());
    data.put("paidAt", order.getDeliveredAt());
    ApiResponse.ok(resp, data);
    return;
}
```

---

### 2.6 Sửa CheckoutPage.vue — QR + polling

**File:** `frontend/src/views/user/CheckoutPage.vue`

**Sửa hàm placeOrder()** — khi chọn BANK_TRANSFER, lưu sepayQrUrl và bắt đầu polling:

```js
const sepayQrUrl = ref('');
const paymentConfirmed = ref(false);
let paymentPolling = null;

async function placeOrder() {
  // ... existing code ...
  const result = await orderStore.createOrder({ ... });
  
  if (paymentMethod.value === 'BANK_TRANSFER' && result.sepayQrUrl) {
    sepayQrUrl.value = result.sepayQrUrl;
    // Bắt đầu polling
    paymentPolling = setInterval(async () => {
      try {
        const status = await orderApi.getPaymentStatus(result.id);
        if (status.paymentStatus === 'PAID') {
          paymentConfirmed.value = true;
          clearInterval(paymentPolling);
        }
      } catch (e) {}
    }, 5000);
  } else {
    cart.clear();
    router.push(`/account/orders/${result.id}`);
  }
}

onUnmounted(() => {
  if (paymentPolling) clearInterval(paymentPolling);
});
```

**Thêm template** cho QR code:
```html
<div v-if="paymentMethod === 'BANK_TRANSFER' && sepayQrUrl" class="card mb-3">
  <h3>Quét mã QR để thanh toán</h3>
  <div class="qr-container" style="text-align:center;padding:20px">
    <img :src="sepayQrUrl" alt="QR thanh toán" style="width:250px;height:250px" />
    <p style="font-size:24px;font-weight:800;margin:12px 0">{{ formatPrice(shippingFee + cart.subtotal) }}</p>
    <p style="color:var(--text-mid)">Nội dung: TT-{{ orderCode }}</p>
    <div v-if="paymentConfirmed" class="payment-success">
      <i class="bi bi-check-circle-fill" style="color:#10b981;font-size:48px"></i>
      <p style="color:#10b981;font-weight:600">Đã thanh toán thành công!</p>
    </div>
    <div v-else class="payment-waiting">
      <i class="bi bi-arrow-repeat spin"></i> Đang chờ thanh toán...
    </div>
  </div>
</div>
```

---

## Phase 3 — Staff nâng cao (5 mục)

### 3.1 CSV Export thật

**File:** `Backend/.../servlet/StaffOrderServlet.java`

Hiện tại chỉ ghi header. Cần ghi dữ liệu thật:
```java
} else if (path.equals("/export")) {
    resp.setContentType("text/csv;charset=UTF-8");
    resp.setHeader("Content-Disposition", "attachment; filename=orders.csv");
    List<Orders> all = new java.util.ArrayList<>();
    for (String s : new String[]{"PENDING","CONFIRMED","PREPARING","READY","DELIVERED","CANCELLED"}) {
        all.addAll(ordersDAO.findByStatus(s));
    }
    var writer = resp.getWriter();
    writer.write("orderCode,status,customerName,finalAmount,createdAt\n");
    for (Orders o : all) {
        writer.write(String.format("%s,%s,%s,%s,%s\n",
            o.getOrderCode(), o.getOrderStatus(), o.getCustomerName(),
            o.getFinalAmount(), o.getCreatedAt()));
    }
    writer.flush();
}
```

---

### 3.2 Notes — lưu thật

**File:** `Backend/.../servlet/StaffOrderServlet.java` doPost

Hiện tại stub. Cần lưu vào `Orders.internal_note`:
```java
// Trong doPost, xử lý path /notes
String path = req.getPathInfo();
if (path != null && path.contains("/notes")) {
    String orderIdStr = path.substring(1, path.indexOf("/notes") - 1);
    int orderId = Integer.parseInt(orderIdStr);
    Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
    String note = (String) body.get("note");
    Orders order = ordersDAO.findById(orderId);
    if (order != null) {
        String existing = order.getInternalNote();
        order.setInternalNote(existing != null ? existing + "\n" + note : note);
        ordersDAO.save(order);
    }
    ApiResponse.ok(resp, null, "Note saved");
    return;
}
```

---

### 3.3 Dashboard — biểu đồ doanh thu

**File:** `frontend/src/views/staff/DashboardPage.vue`

Thêm biểu đồ doanh thu ngày (bar chart) bên cạnh doughnut chart hiện tại:
```js
// Thêm ref cho revenue chart
const revenueChartRef = ref(null);
let revenueChart = null;

// Trong buildCharts():
if (revenueChartRef.value && data.value?.revenueByMonth?.length) {
    revenueChart = new Chart(revenueChartRef.value, {
        type: 'bar',
        data: {
            labels: data.value.revenueByMonth.map(m => 'Tháng ' + m.month),
            datasets: [{ data: data.value.revenueByMonth.map(m => m.revenue) }]
        }
    });
}
```

---

### 3.4 Shipper assign — hoàn thiện

**File:** `Backend/.../servlet/StaffOrderServlet.java` doPut

Hiện tại có endpoint `/api/staff/orders/{orderId}/assign-shipper`. Cần kiểm tra:
- Shipper tồn tại
- Shipper có role = SHIPPER
- Order đang ở status READY

---

### 3.5 StaffLayout — badge thông báo

**File:** `frontend/src/layouts/StaffLayout.vue`

Thêm badge hiển thị số đơn PENDING đang chờ:
```html
<router-link to="/staff/orders">
  Đơn hàng
  <span v-if="pendingCount > 0" class="badge badge-warning">{{ pendingCount }}</span>
</router-link>
```

```js
// Store gọi staffApi.getOrders() đếm số lượng
const pendingCount = ref(0);
onMounted(async () => {
  const orders = await staffApi.getOrders();
  pendingCount.value = Array.isArray(orders) ? orders.length : 0;
});
```

---

## Phase 4 — UI/UX (5 mục)

### 4.1 Loading skeleton

**File:** `frontend/src/assets/styles/global.css`

Thêm CSS skeleton:
```css
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
  min-height: 20px;
}
.skeleton-card {
  height: 200px; border-radius: var(--radius); @extend .skeleton;
}
```

Áp dụng cho: HomePage (product grid), MenuPage (product grid), CartPage.

---

### 4.2 Toast notifications

**File:** `frontend/src/components/common/Toast.vue` (tạo mới)

```vue
<script setup>
import { ref, watch } from 'vue';
const props = defineProps({ message: String, type: { type: String, default: 'success' } });
const visible = ref(false);
watch(() => props.message, (val) => {
  if (val) { visible.value = true; setTimeout(() => { visible.value = false; }, 3000); }
});
</script>
<template>
  <teleport to="body">
    <div v-if="visible" :class="['toast', 'toast-' + type]">
      <i :class="type === 'success' ? 'bi-check-circle' : 'bi-exclamation-circle'"></i>
      {{ message }}
    </div>
  </teleport>
</template>
```

---

### 4.3 Page transitions

**File:** `frontend/src/router/index.js`

```js
const router = createRouter({
  history: createWebHistory(),
  routes,
});
router.beforeEach((to, from, next) => {
  // Thêm transition class
  document.body.classList.add('page-transition');
  setTimeout(() => document.body.classList.remove('page-transition'), 300);
  next();
});
```

**CSS global:**
```css
.page-transition { opacity: 0.5; transition: opacity 0.2s; }
```

---

### 4.4 NotFoundPage

**File:** `frontend/src/views/NotFoundPage.vue`

Cải thiện giao diện 404 với ảnh minh họa + nút về trang chủ:
```html
<div class="not-found">
  <img src="https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/404.png" alt="404" />
  <h1>Trang không tìm thấy</h1>
  <p>Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển</p>
  <router-link to="/" class="btn btn-primary">Về trang chủ</router-link>
</div>
```

---

### 4.5 Responsive tablet

Kiểm tra các trang chính ở kích thước tablet (768px-1024px):

| Trang | Vấn đề cần fix |
|---|---|
| MenuPage | Grid 5 cột → 3 cột |
| Admin Dashboard | Grid 3 charts → 2 charts |
| Staff OrdersPage | Table → card view |
| CheckoutPage | Sidebar xuống dưới |

---

## Phase 5 — Code Cleanup (4 mục)

### 5.1 Xóa file chết

| File | Lý do |
|---|---|
| `frontend/src/utils/helpers.js` | 5 functions — zero imports |
| `frontend/src/utils/validators.js` | 9 functions — zero imports |

### 5.2 Gom style modal trùng

Xóa private modal classes khỏi:
- `ProfilePage.vue`
- `Staff/OrderDetailPage.vue`
- `Shipper/OrderDetailPage.vue`
- `Admin/ProductsPage.vue`

Các class này đã có trong `global.css`.

### 5.3 Gom button styles

Xóa private `.btn-success`, `.btn-danger` khỏi:
- `Staff/OrderDetailPage.vue`
- `Shipper/OrderDetailPage.vue`

Các class này đã được thêm vào `global.css`.

### 5.4 Dọn constants.js

Xóa export không dùng:
- `SHIFT_TYPES` (dòng 93)

---

## Tổng kết

| Phase | Số mục | Loại |
|---|---|---|
| 1 | 3 | Fix lỗi hiện tại |
| 2 | 6 | SePay QR thanh toán |
| 3 | 5 | Staff nâng cao |
| 4 | 5 | UI/UX |
| 5 | 4 | Code cleanup |
| **Tổng** | **23** | |
