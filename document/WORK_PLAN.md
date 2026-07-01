# WORK PLAN — FastGuy (Hoàn thiện toàn diện)

## Mục tiêu

Hoàn thiện dự án FastGuy với 6 phase, 48 mục, ước tính 4.900 dòng code mới.

**Yêu cầu:** Mỗi mục = 1 commit riêng, đảm bảo đạt ~4.000+ dòng contribute.

---

## Tổng quan

| Phase | Nội dung | Số mục | Dòng ước tính |
|---|---|---|---|
| 1 | Fix Backend cốt lõi | 8 | 600 |
| 2 | SePay QR Payment | 5 | 800 |
| 3 | Frontend Core | 10 | 1,200 |
| 4 | UI/UX Design System | 10 | 900 |
| 5 | Staff nâng cao | 10 | 800 |
| 6 | Shipper + Cleanup | 5 | 600 |
| **Tổng** | | **48** | **~4.900** |

---

## Phase 1 — Fix Backend cốt lõi (8 mục ~ 600 dòng)

### Mục 1.1 — Fix AdminServlet chặn request orders

**File:** `Backend/.../servlet/AdminServlet.java`

**Vấn đề:** `@WebServlet("/api/admin/*")` bắt hết request `/api/admin/*`, gây 404 cho `/api/admin/orders`.

**Fix:** Thêm case `"/orders"` vào `doGet`:
```java
} else if (path.equals("/orders")) {
    var orders = new AdminOrderServlet().getOrders();
    ApiResponse.ok(resp, orders);
}
```

Cần tạo method `getOrders()` trong `AdminOrderServlet` để có thể gọi từ bên ngoài.

**Dòng ước tính:** 30 dòng

---

### Mục 1.2 — Fix AdminOrderServlet wildcard + mapping

**File:** `Backend/.../servlet/AdminOrderServlet.java`

**Sửa:** Thêm `@WebServlet("/api/admin/orders/*")` và method `getOrders()` public.

**Dòng ước tính:** 25 dòng

---

### Mục 1.3 — StaffShiftServlet implement thật

**File:** `Backend/.../servlet/StaffShiftServlet.java`

**Hiện tại:** Trả `"OK"` string cứng.

**Fix:** 
- `doGet`: Lấy lịch sử check-in/out từ `ScheduleDAO`
- `doPost`: Tạo schedule mới, ghi `checked_in_at`
- `doPut`: Cập nhật `checked_out_at`

**Dòng ước tính:** 60 dòng

---

### Mục 1.4 — AdminScheduleServlet CRUD hoàn chỉnh

**File:** `Backend/.../servlet/AdminScheduleServlet.java`

**Hiện tại:** Stub, trả list rỗng.

**Fix:** Implement:
- `doGet`: List schedules với user name, shift name (JOIN query)
- `doPost`: Tạo schedule mới
- `doPut`: Cập nhật schedule
- `doDelete`: Xóa schedule

**Dòng ước tính:** 80 dòng

---

### Mục 1.5 — AdminShiftServlet CRUD hoàn chỉnh

**File:** `Backend/.../servlet/AdminShiftServlet.java`

**Hiện tại:** Stub, không đọc body.

**Fix:** Implement CRUD với `WorkShiftDAO`:
- `doGet`: List work shifts
- `doPost`: Tạo shift mới (shift_name, start_time, end_time, role_type)
- `doPut`: Cập nhật shift
- `doDelete`: Xóa shift

**Dòng ước tính:** 80 dòng

---

### Mục 1.6 — VoucherServlet trả dữ liệu thật

**File:** `Backend/.../servlet/VoucherServlet.java`

**Hiện tại:** Trả list rỗng.

**Fix:** Tạo `VoucherDAO` + lưu voucher vào DB, trả danh sách voucher của user.

**Dòng ước tính:** 80 dòng

---

### Mục 1.7 — FavoritesServlet lưu DB

**File:** `Backend/.../servlet/FavoritesServlet.java`

**Hiện tại:** Trả list rỗng, không lưu.

**Fix:** Tạo `FavoriteDAO` + lưu favorite product của user vào DB.

**Dòng ước tính:** 70 dòng

---

### Mục 1.8 — StaffOrderServlet CSV export thật

**File:** `Backend/.../servlet/StaffOrderServlet.java`

**Hiện tại:** Chỉ ghi header, không có dữ liệu.

**Fix:** 
```java
writer.write("orderCode,status,customerName,finalAmount,createdAt\n");
for (Orders o : all) {
    writer.write(String.format("%s,%s,%s,%s,%s\n",
        o.getOrderCode(), o.getOrderStatus(), o.getCustomerName(),
        o.getFinalAmount(), o.getCreatedAt()));
}
```

**Dòng ước tính:** 30 dòng

---

## Phase 2 — SePay QR Payment (5 mục ~ 800 dòng)

### Mục 2.1 — Tạo SePayConfig.java

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

    public static String getBankAccount() { return BANK_ACCOUNT; }
    public static String getBankCode() { return BANK_CODE; }
}
```

**Dòng ước tính:** 40 dòng

---

### Mục 2.2 — Tạo SePayService.java

**File:** `Backend/.../service/SePayService.java` (tạo mới)

Chức năng:
- Tạo QR URL từ order amount
- Xử lý webhook từ SePay
- Verify webhook signature (nếu có)
- Cập nhật `payment_status = 'PAID'` khi nhận được webhook

```java
package service;

import dao.OrdersDAO;
import utils.SePayConfig;

public class SePayService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    public String generateQrUrl(int orderId, long amount, String orderCode) {
        return SePayConfig.buildQrUrl(amount, "TT ORD-" + orderCode);
    }

    public boolean processWebhook(Map<String, Object> payload) {
        // Parse transaction data
        // Find order by description
        // Update payment_status
        return true;
    }
}
```

**Dòng ước tính:** 80 dòng

---

### Mục 2.3 — Tạo SePayWebhookServlet.java

**File:** `Backend/.../servlet/SePayWebhookServlet.java` (tạo mới)

```java
@WebServlet("/api/payment/sepay-webhook")
public class SePayWebhookServlet extends HttpServlet {
    private SePayService sePayService = new SePayService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        // Parse JSON body
        // Call sePayService.processWebhook()
        // Return success/failure
    }
}
```

**Dòng ước tính:** 60 dòng

---

### Mục 2.4 — Sửa OrderService trả QR URL

**File:** `Backend/.../service/OrderService.java`

Sau khi checkout, nếu `paymentMethod = 'BANK_TRANSFER'`, trả thêm `sepayQrUrl`:
```java
Map<String, Object> result = new HashMap<>();
result.put("orderId", order.getOrderId());
result.put("orderCode", order.getOrderCode());
result.put("status", order.getOrderStatus());
result.put("finalAmount", order.getFinalAmount());
if ("BANK_TRANSFER".equals(paymentMethod)) {
    SePayService sePayService = new SePayService();
    result.put("sepayQrUrl", sePayService.generateQrUrl(
        order.getOrderId(), order.getFinalAmount().longValue(), order.getOrderCode()));
}
return result;
```

**Dòng ước tính:** 40 dòng

---

### Mục 2.5 — CheckoutPage.vue QR + Polling

**File:** `frontend/src/views/user/CheckoutPage.vue`

**Thêm:**
- Khi `paymentMethod === 'BANK_TRANSFER'`, hiển thị QR code sau khi đặt hàng
- Polling `orderApi.getPaymentStatus(orderId)` mỗi 5 giây
- Khi `paymentStatus === 'PAID'`: hiển thị thành công, tự động redirect
- Khi quá 30 phút: hiển thị thông báo hết hạn

**Template QR:**
```html
<div v-if="paymentMethod === 'BANK_TRANSFER' && sepayQrUrl" class="card mb-3">
  <h3>Quét mã QR để thanh toán</h3>
  <div class="qr-container">
    <img :src="sepayQrUrl" alt="QR" style="width:250px" />
    <p class="qr-amount">{{ formatPrice(total) }}</p>
    <p class="qr-description">Nội dung: {{ orderCode }}</p>
    <div v-if="paymentConfirmed" class="payment-success">✓ Đã thanh toán</div>
    <div v-else class="payment-waiting"><i class="bi bi-arrow-repeat spin"></i> Chờ thanh toán...</div>
  </div>
</div>
```

**Dòng ước tính:** 120 dòng

---

## Phase 3 — Frontend Core (10 mục ~ 1.200 dòng)

### Mục 3.1 — Admin OrdersPage hiển thị data

**File:** `frontend/src/views/admin/OrdersPage.vue`

Thêm:
- Load orders từ `adminStore.fetchOrders()`
- Search theo mã đơn
- Filter theo status (PENDING/CONFIRMED/PREPARING/READY/DELIVERED/CANCELLED)
- Phân trang
- Click vào order → xem chi tiết

**Dòng ước tính:** 120 dòng

---

### Mục 3.2 — Admin UsersPage CRUD hoàn chỉnh

**File:** `frontend/src/views/admin/UsersPage.vue`

Thêm:
- Form thêm user: fullName, email, phone, password, role (dropdown)
- Form sửa user: fullName, email, phone, role
- Nút xóa user + confirm modal
- Validation: email/phone duplicate check
- Search + filter theo role

**Dòng ước tính:** 200 dòng

---

### Mục 3.3 — Admin ProductsPage variant weight

**File:** `frontend/src/views/admin/ProductsPage.vue`

Thêm field cho variant: weight, length, width, height (decimal)
- Form thêm variant có thêm 4 field mới
- Cập nhật khi sửa variant

**Dòng ước tính:** 60 dòng

---

### Mục 3.4 — Staff OrderDetail shipper gán

**File:** `frontend/src/views/staff/OrderDetailPage.vue`

Thêm:
- Khi order status = READY, hiển thị dropdown chọn shipper
- Nút "Gán shipper"
- Gọi API assign-shipper
- Toast thông báo thành công/thất bại

**Dòng ước tính:** 100 dòng

---

### Mục 3.5 — Staff Dashboard biểu đồ doanh thu

**File:** `frontend/src/views/staff/DashboardPage.vue`

Thêm Chart.js bar chart:
- Doanh thu theo ngày trong tuần này
- Số đơn xử lý theo ngày

```js
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

**Dòng ước tính:** 120 dòng

---

### Mục 3.6 — Shipper Dashboard realtime auto-refresh

**File:** `frontend/src/views/shipper/DashboardPage.vue`

Thêm:
- Auto-refresh danh sách đơn mỗi 30 giây
- Toast thông báo khi có đơn mới
- Sound notification (optional)

```js
onMounted(() => {
  shipperStore.fetchAvailableOrders();
  interval = setInterval(() => {
    shipperStore.fetchAvailableOrders();
  }, 30000);
});
onUnmounted(() => clearInterval(interval));
```

**Dòng ước tính:** 80 dòng

---

### Mục 3.7 — Shipper OrderDetail pickup/deliver

**File:** `frontend/src/views/shipper/OrderDetailPage.vue`

Cải thiện:
- Nút "Nhận đơn" (Pickup) - lớn, màu xanh
- Nút "Đã giao thành công" (Deliver) - lớn, màu xanh lá
- Nút "Hủy" + modal lý do
- Nút "Gọi điện" - link tel:
- Nút "Chỉ đường" - link Google Maps

**Dòng ước tính:** 100 dòng

---

### Mục 3.8 — User OrderDetail variant + shipping

**File:** `frontend/src/views/user/OrderDetailPage.vue`

Đảm bảo:
- Hiển thị variantName (Size L, Combo 3 miếng, ...)
- Hiển thị shipping fee riêng
- Hiển thị expected delivery time
- Nút "Hủy đơn" (chỉ khi PENDING)
- Timeline status đẹp hơn

**Dòng ước tính:** 80 dòng

---

### Mục 3.9 — User OrdersPage search + filter

**File:** `frontend/src/views/user/OrdersPage.vue`

Thêm:
- Search theo mã đơn
- Filter tab: Tất cả / Chờ xác nhận / Đã giao / Đã hủy
- Sort theo ngày đặt (mới nhất/cũ nhất)
- Phân trang

**Dòng ước tính:** 80 dòng

---

### Mục 3.10 — TrackOrderPage guest

**File:** `frontend/src/views/guest/TrackOrderPage.vue`

Cải thiện:
- UI đẹp hơn với card + shadow
- Hiển thị đầy đủ: items, variantName, shipping fee, timeline
- Button "Mua lại" redirect to menu
- Share order button

**Dòng ước tính:** 80 dòng

---

## Phase 4 — UI/UX Design System (10 mục ~ 900 dòng)

### Mục 4.1 — CSS Design Tokens

**File:** `frontend/src/assets/styles/variables.css`

```css
--shadow-sm: 0 1px 3px rgba(0,0,0,0.08);
--shadow-md: 0 4px 12px rgba(0,0,0,0.1);
--shadow-lg: 0 8px 24px rgba(0,0,0,0.12);
--radius-lg: 12px;
--transition-fast: 0.15s ease;
--transition-normal: 0.25s ease;
```

**Dòng ước tính:** 30 dòng

---

### Mục 4.2 — Global Card + Shadow + Buttons

**File:** `frontend/src/assets/styles/global.css`

```css
.card { box-shadow: var(--shadow-sm); transition: box-shadow var(--transition-fast); }
.card:hover { box-shadow: var(--shadow-md); }

.btn-success { background: #10b981; color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; padding: 8px 16px; }
.btn-success:hover { background: #059669; }

.btn-danger { background: #ef4444; color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; padding: 8px 16px; }
.btn-danger:hover { background: #dc2626; }
```

**Dòng ước tính:** 80 dòng

---

### Mục 4.3 — Table Row Hover

**File:** `frontend/src/assets/styles/global.css`

```css
.table tbody tr { transition: background var(--transition-fast); }
.table tbody tr:hover { background: #f8f9fa; cursor: pointer; }

.table tbody tr:active { background: #f0f4f8; }
```

**Dòng ước tính:** 20 dòng

---

### Mục 4.4 — Loading Skeleton Component

**File:** `frontend/src/components/common/SkeletonCard.vue` (tạo mới)

```vue
<template>
  <div class="skeleton-card" :style="{ width, height }">
    <div class="skeleton-shimmer"></div>
  </div>
</template>

<style scoped>
.skeleton-card { background: #f0f0f0; border-radius: var(--radius); overflow: hidden; position: relative; }
.skeleton-shimmer {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent 25%, rgba(255,255,255,0.4) 50%, transparent 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
</style>
```

**Dòng ước tính:** 80 dòng

---

### Mục 4.5 — Toast Notification Component

**File:** `frontend/src/components/common/Toast.vue`

**Cập nhật:** Thêm animation + auto-destroy + queue nhiều toast:
```vue
<script setup>
import { ref, watch, onUnmounted } from 'vue';
const props = defineProps({ message: String, type: { default: 'success' } });
const visible = ref(false);
let timer = null;

watch(() => props.message, (val) => {
  if (val) {
    visible.value = true;
    clearTimeout(timer);
    timer = setTimeout(() => { visible.value = false; }, 3000);
  }
});
onUnmounted(() => clearTimeout(timer));
</script>

<template>
  <teleport to="body">
    <transition name="toast-fade">
      <div v-if="visible" :class="['toast', 'toast-' + type]" class="toast-container">
        <i :class="type === 'success' ? 'bi-check-circle-fill' : type === 'error' ? 'bi-x-circle-fill' : 'bi-info-circle-fill'"></i>
        {{ message }}
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.toast-container { position: fixed; top: 20px; right: 20px; z-index: 9999; padding: 12px 20px; border-radius: var(--radius); box-shadow: var(--shadow-lg); font-weight: 600; display: flex; align-items: center; gap: 8px; }
.toast-success { background: #10b981; color: #fff; }
.toast-error { background: #ef4444; color: #fff; }
.toast-info { background: #3b82f6; color: #fff; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateX(50px); }
</style>
```

**Dòng ước tính:** 120 dòng

---

### Mục 4.6 — HomePage Hero with Images

**File:** `frontend/src/views/guest/HomePage.vue`

Cập nhật hero slides với ảnh từ Cloudinary:
```js
const slides = [
  { image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg',
    title: 'Burger Ngon Mỗi Ngày', subtitle: 'Khám phá bộ sưu tập burger mới' },
  { image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/GaRan/ga-ran.jpg',
    title: 'Gà Rán Giòn Rụm', subtitle: 'Công thức đặc biệt từ bếp' },
  { image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Pizza/pizza.jpg',
    title: 'Pizza Nóng Hổi', subtitle: 'Phô mai kéo sợi thơm ngon' },
];
```

Thêm CSS:
```css
.hero-slide { background-size: cover; background-position: center; background-blend-mode: overlay; background-color: rgba(0,0,0,0.4); }
```

**Dòng ước tính:** 180 dòng

---

### Mục 4.7 — HomePage About Cards

**File:** `frontend/src/views/guest/HomePage.vue`

```css
.about-card { transition: all var(--transition-normal); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; text-align: center; }
.about-card:hover { box-shadow: var(--shadow-md); transform: translateY(-4px); border-color: var(--primary); }
.about-card i { transition: transform var(--transition-normal); }
.about-card:hover i { transform: scale(1.2); color: var(--primary); }
```

**Dòng ước tính:** 80 dòng

---

### Mục 4.8 — ProductCard Hover Effect

**File:** `frontend/src/components/common/ProductCard.vue`

```css
.product-card { transition: all var(--transition-normal); border-radius: var(--radius); overflow: hidden; cursor: pointer; }
.product-card:hover { box-shadow: var(--shadow-lg); transform: translateY(-4px); }
.product-card img { transition: transform var(--transition-normal); }
.product-card:hover img { transform: scale(1.05); }
.product-card .card-body { transition: background var(--transition-fast); }
.product-card:hover .card-body { background: #fafafa; }
```

**Dòng ước tính:** 60 dòng

---

### Mục 4.9 — ProductDetail Image Zoom

**File:** `frontend/src/views/guest/ProductDetailPage.vue`

```css
.detail-image-bg { border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); cursor: zoom-in; }
.detail-image-bg img { transition: transform var(--transition-normal); width: 100%; }
.detail-image-bg:hover img { transform: scale(1.08); }
.gallery-thumb { border-radius: var(--radius-sm); transition: all var(--transition-fast); border: 2px solid transparent; cursor: pointer; }
.gallery-thumb:hover { border-color: var(--primary); box-shadow: var(--shadow-sm); }
.gallery-thumb.active { border-color: var(--primary); }
```

**Dòng ước tính:** 40 dòng

---

### Mục 4.10 — Page Transitions

**File:** `frontend/src/router/index.js` + `assets/styles/global.css`

**Router:**
```js
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 }; }
});
```

**CSS:**
```css
.page-enter-active, .page-leave-active { transition: opacity 0.2s ease; }
.page-enter-from, .page-leave-to { opacity: 0; }

/* Thêm vào App.vue hoặc wrapper */
<router-view v-slot="{ Component }">
  <transition name="page" mode="out-in">
    <component :is="Component" />
  </transition>
</router-view>
```

**Dòng ước tính:** 40 dòng

---

## Phase 5 — Staff Nâng cao (10 mục ~ 800 dòng)

### Mục 5.1 — Staff Dashboard thống kê doanh thu ca

**File:** `Backend/.../service/StaffService.java` + `views/staff/DashboardPage.vue`

Backend:
```java
public double getShiftRevenue(int userId, LocalDate date) {
    // Tính tổng finalAmount của đơn do staff xử lý trong ngày
}
```

Frontend: Thêm stat card "Doanh thu ca"

**Dòng ước tính:** 80 dòng

---

### Mục 5.2 — Staff OrderDetail in hóa đơn

**File:** `frontend/src/views/staff/OrderDetailPage.vue`

Cải thiện print invoice:
- Template in rõ ràng hơn
- Thông tin cửa hàng
- Mã QR đơn hàng
- Chữ ký nhận hàng

**Dòng ước tính:** 80 dòng

---

### Mục 5.3 — Staff Badge đơn mới

**File:** `frontend/src/layouts/StaffLayout.vue`

```js
const pendingCount = ref(0);
onMounted(async () => {
  const updateCount = async () => {
    try {
      const orders = await staffApi.getOrders();
      pendingCount.value = Array.isArray(orders) ? orders.length : 0;
    } catch(e) {}
  };
  await updateCount();
  setInterval(updateCount, 30000); // Refresh mỗi 30s
});
```

**Dòng ước tính:** 60 dòng

---

### Mục 5.4 — Staff Shift History

**File:** `frontend/src/views/staff/ShiftsPage.vue`

Thêm:
- Lịch sử check-in/out theo tuần
- Thống kê: tổng giờ làm trong tuần, số ngày đi làm
- Bộ lọc theo tuần

**Dòng ước tính:** 80 dòng

---

### Mục 5.5 — Staff Dashboard top sản phẩm

**File:** `frontend/src/views/staff/DashboardPage.vue`

Thêm Chart.js horizontal bar: Top 5 sản phẩm bán chạy trong ca/ngày.

**Dòng ước tính:** 80 dòng

---

### Mục 5.6 — Staff Internal Notes UI

**File:** `frontend/src/views/staff/OrderDetailPage.vue`

Thêm:
- Form textarea + nút "Lưu ghi chú"
- Hiển thị lịch sử ghi chú (thời gian, người ghi, nội dung)
- Auto-load khi mở order detail

**Dòng ước tính:** 80 dòng

---

### Mục 5.7 — Staff CSV Export UI

**File:** `frontend/src/views/staff/OrdersPage.vue`

Thêm:
- Nút "Xuất CSV" trên toolbar
- Chọn khoảng thời gian (hôm nay, tuần này, tháng này, tùy chọn)
- Download file

**Dòng ước tính:** 60 dòng

---

### Mục 5.8 — Staff Dashboard biểu đồ cột đơn theo giờ

**File:** `frontend/src/views/staff/DashboardPage.vue`

Chart.js bar: Số đơn xử lý theo từng giờ trong ngày (8h, 9h, 10h, ...)

**Dòng ước tính:** 80 dòng

---

### Mục 5.9 — Staff Layout sidebar thu gọn

**File:** `frontend/src/layouts/StaffLayout.vue`

Mobile-responsive:
- Nút hamburger toggle
- Sidebar thu gọn trên mobile
- Overlay khi mở sidebar

**Dòng ước tính:** 80 dòng

---

### Mục 5.10 — Staff Dark Mode

**File:** `frontend/src/assets/styles/global.css` + `StaffLayout.vue`

```css
.dark-mode { --bg: #1a1a2e; --card-bg: #16213e; --text: #e0e0e0; --border: #2a2a4a; }
```

Toggle button trong StaffLayout:
```js
const isDark = ref(false);
function toggleDark() {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark-mode', isDark.value);
}
```

**Dòng ước tính:** 80 dòng

---

## Phase 6 — Shipper + Cleanup (5 mục ~ 600 dòng)

### Mục 6.1 — Shipper Dashboard Map

**File:** `frontend/src/views/shipper/DashboardPage.vue`

Tích hợp Leaflet/Google Maps:
- Hiển thị vị trí các địa chỉ giao hàng
- Marker cho mỗi đơn
- Click marker → xem thông tin đơn
- Nút "Chỉ đường" → mở Google Maps navigation

```bash
npm install leaflet
```

```js
import L from 'leaflet';
// Khởi tạo bản đồ
// Thêm marker cho từng đơn
// Fit bounds để hiển thị tất cả marker
```

**Dòng ước tính:** 200 dòng

---

### Mục 6.2 — Shipper Auto-refresh Orders

**File:** `frontend/src/views/shipper/DashboardPage.vue`

```js
onMounted(() => {
  fetchOrders();
  const interval = setInterval(() => {
    const prevCount = shipperStore.availableOrders.length;
    shipperStore.fetchAvailableOrders();
    if (shipperStore.availableOrders.length > prevCount) {
      // Toast thông báo có đơn mới
      showToast = true;
      toastMessage = 'Có đơn giao hàng mới!';
    }
  }, 20000);
  onUnmounted(() => clearInterval(interval));
});
```

**Dòng ước tính:** 60 dòng

---

### Mục 6.3 — Shipper Order Tracking Link

**File:** `frontend/src/views/shipper/OrderDetailPage.vue`

Thêm:
- Nút "Gọi điện" - `<a :href="'tel:' + order.customerPhone">`
- Nút "Chỉ đường" - `<a :href="'https://maps.google.com/?q=' + encodeURIComponent(order.customerAddress)" target="_blank">`
- Copy địa chỉ - clipboard API

```js
function callCustomer() { window.location.href = 'tel:' + order.value.customerPhone; }
function openMaps() { window.open('https://maps.google.com/?q=' + encodeURIComponent(order.value.customerAddress), '_blank'); }
function copyAddress() { navigator.clipboard.writeText(order.value.customerAddress); }
```

**Dòng ước tính:** 80 dòng

---

### Mục 6.4 — Cleanup Modal Style Trùng

Xóa private modal classes khỏi 4 file:

1. `frontend/src/views/user/ProfilePage.vue` — xóa `.modal-overlay`, `.modal`, `.modal-header`, `.modal-body`, `.modal-footer`
2. `frontend/src/views/staff/OrderDetailPage.vue` — xóa `.btn-success`, `.btn-danger`, `.modal-overlay`, `.modal`
3. `frontend/src/views/shipper/OrderDetailPage.vue` — xóa `.btn-success`, `.btn-danger`, `.modal-overlay`, `.modal`
4. `frontend/src/views/admin/ProductsPage.vue` — xóa `.modal-content`, `.modal-header`, `.modal-close`, `.modal-footer`

Tất cả đã có trong `global.css`.

**Dòng ước tính:** 120 dòng (xóa ~120 dòng, thêm 0)

---

### Mục 6.5 — Cleanup Constants + Dead Code

**File:** `frontend/src/utils/constants.js`

Xóa: `SHIFT_TYPES`, kiểm tra các export khác không dùng.

Kiểm tra toàn bộ imports trong project, xóa file/export không dùng.

**Dòng ước tính:** 30 dòng (xóa ~30 dòng)

---

## Tổng kết dòng code

| Phase | Mục | Dòng mới |
|---|---|---|
| Phase 1 — Backend | 8 | 600 |
| Phase 2 — SePay QR | 5 | 800 |
| Phase 3 — Frontend Core | 10 | 1,200 |
| Phase 4 — UI/UX Design | 10 | 900 |
| Phase 5 — Staff nâng cao | 10 | 800 |
| Phase 6 — Shipper + Cleanup | 5 | 600 |
| **Tổng** | **48** | **~4,900** |

## Cách đạt 4.000+ commit lines

1. Mỗi mục = **1 commit riêng biệt** với message rõ ràng
2. Format commit message:
   ```
   feat: them tinh nang X
   fix: sua loi Y
   style: hover effect cho card
   refactor: gom modal style vao global
   ```
3. 48 commits × trung bình 85 dòng/commit ≈ 4.080 dòng
4. GitHub sẽ hiển thị chính xác dòng contribute của từng tác giả

## Lệnh kiểm tra

```bash
cd Backend/FastGuy-FastFoodSite && mvn clean compile
cd Frontend && npm run build
```

---

## Ưu tiên hiện tại — Demo luồng mua hàng COD + BANK_TRANSFER

### Mục tiêu demo

Hoàn thiện luồng chính:

```text
USER mua hàng
→ STAFF xác nhận / chuẩn bị / sẵn sàng giao
→ SHIPPER nhận đơn / giao hàng
→ USER xem trạng thái cuối
```

Phạm vi bắt buộc:

- Thanh toán COD.
- Thanh toán BANK_TRANSFER bằng QR + webhook/check payment.
- Đủ trạng thái từ tạo đơn tới giao thành công.
- Staff không xử lý đơn chuyển khoản khi chưa thanh toán.
- Shipper nhận đơn và giao đơn.

Không làm trong đợt demo này:

- Guest anonymous checkout.
- GHN tạo vận đơn thật.
- Hoàn tiền.
- Giao thất bại chi tiết.
- Refactor lớn ngoài luồng mua hàng.

### Trạng thái chuẩn

#### Order status

```text
PENDING
→ CONFIRMED
→ PREPARING
→ READY
→ PICKED_UP
→ DELIVERED
```

Hủy đơn:

```text
PENDING | CONFIRMED | PREPARING → CANCELLED
```

#### Payment status

COD:

```text
UNPAID → PAID
```

BANK_TRANSFER:

```text
PENDING → PAID
```

#### Payment rule

| Payment method | Khi tạo đơn | Staff confirm khi | Khi set PAID |
|---|---|---|---|
| COD | `UNPAID` | Ngay | Shipper giao `DELIVERED` |
| BANK_TRANSFER | `PENDING` | Chỉ khi `PAID` | SePay webhook/check payment |

---

### Phần A — Chuẩn hóa trạng thái đơn hàng

**Mục tiêu:** Backend/frontend dùng cùng status, không còn lệch `DELIVERING` và `PICKED_UP`.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`
- `Frontend/src/utils/constants.js`
- `Frontend/src/components/common/OrderStatusBadge.vue`
- `Frontend/src/views/user/OrdersPage.vue`
- `Frontend/src/views/user/OrderDetailPage.vue`
- `Frontend/src/views/staff/OrdersPage.vue`
- `Frontend/src/views/staff/OrderDetailPage.vue`
- `Frontend/src/views/shipper/DashboardPage.vue`
- `Frontend/src/views/shipper/MyOrdersPage.vue`
- `Frontend/src/views/shipper/OrderDetailPage.vue`

**Việc làm:**

1. Chuẩn hóa status dùng chung:
   - `PENDING`
   - `CONFIRMED`
   - `PREPARING`
   - `READY`
   - `PICKED_UP`
   - `DELIVERED`
   - `CANCELLED`
2. Đổi mọi frontend `DELIVERING` thành `PICKED_UP`.
3. Sửa `OrderStatusBadge` để label/class reactive khi status đổi.
4. Tabs user/staff/shipper lọc đúng status mới.
5. Text hiển thị:
   - `PENDING`: Chờ xác nhận
   - `CONFIRMED`: Đã xác nhận
   - `PREPARING`: Đang chuẩn bị
   - `READY`: Sẵn sàng giao
   - `PICKED_UP`: Đang giao
   - `DELIVERED`: Đã giao
   - `CANCELLED`: Đã hủy

**Kiểm tra:**

- User order list lọc đúng `PICKED_UP`.
- Staff chỉ xử lý tới `READY`.
- Shipper thấy `READY`, `PICKED_UP`, `DELIVERED`.
- Badge đổi đúng sau mỗi action.

**Commit:** `chuan hoa trang thai don hang`

---

### Phần B — Fix checkout tạo đơn COD/BANK_TRANSFER

**Mục tiêu:** User tạo đơn được bằng COD và BANK_TRANSFER, bank QR hiển thị đúng.

**Root cause hiện tại:**

- `orderStore.createOrder()` bỏ mất `sepayQrUrl`.
- Payment status chưa thống nhất.
- `paidAt` có nguy cơ lấy sai từ `deliveredAt`.
- Bank config backend/frontend có thể lệch.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/SePayService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/SePayWebhookServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/utils/SePayConfig.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Payment.java`
- `Frontend/src/api/order.js`
- `Frontend/src/stores/order.js`
- `Frontend/src/views/user/CheckoutPage.vue`
- `Frontend/src/views/user/OrderDetailPage.vue`

**Việc làm backend:**

1. Khi tạo COD:
   - `paymentMethod = COD`
   - `paymentStatus = UNPAID`
   - `orderStatus = PENDING`
2. Khi tạo BANK_TRANSFER:
   - `paymentMethod = BANK_TRANSFER`
   - `paymentStatus = PENDING`
   - `orderStatus = PENDING`
3. Response create order trả đủ:

```json
{
  "orderId": 123,
  "orderCode": "FG123",
  "paymentMethod": "BANK_TRANSFER",
  "paymentStatus": "PENDING",
  "finalAmount": 150000,
  "sepayQrUrl": "...",
  "transferContent": "FG123"
}
```

4. `GET /api/orders/{id}/payment-status` trả đúng:

```json
{
  "orderId": 123,
  "paymentStatus": "PAID",
  "paidAt": "2026-07-01T..."
}
```

5. Không dùng `deliveredAt` làm `paidAt`.
6. Webhook SePay:
   - đọc transfer content/order code
   - tìm order
   - check amount == `finalAmount`
   - set `paymentStatus = PAID`
   - set `paidAt = now`

**Việc làm frontend:**

1. `orderStore.createOrder()` giữ lại field backend trả:
   - `sepayQrUrl`
   - `transferContent`
   - `finalAmount`
   - `paymentStatus`
   - `orderId`
2. Checkout bank hiển thị:
   - QR
   - số tiền
   - nội dung chuyển khoản
   - nút “Kiểm tra thanh toán”
3. Poll/check payment status.
4. COD tạo xong redirect order detail.
5. BANK_TRANSFER chỉ redirect khi `PAID`, hoặc cho user xem màn “Chờ thanh toán”.

**Kiểm tra:**

- Checkout COD tạo order `PENDING` + payment `UNPAID`.
- Checkout BANK_TRANSFER hiện QR + payment `PENDING`.
- Webhook/check payment đổi payment thành `PAID`.

**Commit:** `fix thanh toan cod bank`

---

### Phần C — Chặn staff xử lý bank chưa thanh toán

**Mục tiêu:** Staff không confirm đơn chuyển khoản khi payment chưa `PAID`.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- `Frontend/src/views/staff/OrderDetailPage.vue`
- `Frontend/src/stores/staff.js`

**Rule:**

```text
COD + UNPAID: staff confirm được
BANK_TRANSFER + PENDING: staff không confirm
BANK_TRANSFER + PAID: staff confirm được
```

**Việc làm:**

1. Backend validate trước khi `PENDING → CONFIRMED`.
2. Nếu bank chưa paid, trả lỗi: `Đơn chuyển khoản chưa thanh toán`.
3. Frontend disable nút “Xác nhận đơn” nếu:
   - `paymentMethod === BANK_TRANSFER`
   - `paymentStatus !== PAID`
4. Hiện cảnh báo: `Chờ khách thanh toán chuyển khoản`.

**Kiểm tra:**

- Bank chưa paid: staff confirm bị chặn.
- Bank paid: staff confirm được.
- COD unpaid: staff confirm được.

**Commit:** `chan xu ly don chua thanh toan`

---

### Phần D — Fix staff flow tới READY và assign shipper

**Mục tiêu:** Staff xử lý mượt từ `PENDING` tới `READY`, sau đó assign shipper.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/UserDAO.java`
- `Frontend/src/views/staff/OrdersPage.vue`
- `Frontend/src/views/staff/OrderDetailPage.vue`
- `Frontend/src/stores/staff.js`
- `Frontend/src/api/staff.js`

**Việc làm:**

1. Backend giữ transition:

```text
PENDING → CONFIRMED
CONFIRMED → PREPARING
PREPARING → READY
```

2. Cancel chỉ cho:

```text
PENDING | CONFIRMED | PREPARING
```

3. Assign shipper chỉ khi:
   - `orderStatus = READY`
   - `shipperId = null`
   - user được assign có role `SHIPPER`
4. Không cho assign lại nếu đã có shipper.
5. Staff detail trả đủ:
   - order status
   - payment method
   - payment status
   - shipper info
   - items + variant name
6. Frontend sau action status phải fetch lại detail.
7. Nếu status mới là `READY`, auto load shipper list.
8. Assign xong refresh detail.

**Kiểm tra:**

- Staff chuyển được `PENDING → CONFIRMED → PREPARING → READY`.
- READY hiện dropdown shipper.
- Assign shipper thành công.
- Assign lại bị chặn.

**Commit:** `fix luong staff ready`

---

### Phần E — Fix shipper nhận/giao đơn

**Mục tiêu:** Shipper thấy đơn READY, nhận đơn, giao đơn.

```text
READY → PICKED_UP → DELIVERED
```

COD delivered thì payment thành `PAID`.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrderDAO.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/UserDAO.java`
- `Frontend/src/router/index.js`
- `Frontend/src/layouts/ShipperLayout.vue`
- `Frontend/src/stores/shipper.js`
- `Frontend/src/api/shipper.js`
- `Frontend/src/views/shipper/DashboardPage.vue`
- `Frontend/src/views/shipper/MyOrdersPage.vue`
- `Frontend/src/views/shipper/OrderDetailPage.vue`

**Việc làm backend:**

1. `ShipperServlet` check JWT hợp lệ và role `SHIPPER`.
2. Dashboard/list trả:
   - đơn `READY` chưa có shipper hoặc đã assign cho shipper hiện tại
   - đơn `PICKED_UP` của shipper hiện tại
3. Detail cho phép xem:
   - `READY` unassigned
   - `READY` assigned to current shipper
   - `PICKED_UP` current shipper
   - `DELIVERED` current shipper
4. Nhận đơn:
   - order phải `READY`
   - nếu shipper null thì set current shipper
   - status `PICKED_UP`
5. Giao thành công:
   - order phải `PICKED_UP`
   - status `DELIVERED`
   - set `deliveredAt = now`
   - nếu COD: `paymentStatus = PAID`, `paidAt = now`

**Việc làm frontend:**

1. Xử lý `/shipper/history`:
   - Lười nhất: bỏ nav link.
   - Nếu cần demo lịch sử: thêm route history lọc `DELIVERED`.
2. Dashboard hiển thị đơn READY.
3. My Orders hiển thị đơn `PICKED_UP`.
4. Detail:
   - Nếu `READY`: nút “Nhận đơn”
   - Nếu `PICKED_UP`: nút “Đã giao hàng”
   - Nếu `DELIVERED`: chỉ xem
5. Sau action refresh detail/list.

**Kiểm tra:**

- User thường gọi shipper API bị 403.
- Shipper thấy đơn READY.
- Nhận đơn đổi status thành `PICKED_UP`.
- Giao COD đổi status `DELIVERED`, payment `PAID`.
- Giao bank paid giữ payment `PAID`.
- Không còn 404 `/shipper/history`.

**Commit:** `fix luong shipper giao hang`

---

### Phần F — Fix cart đủ cho demo

**Mục tiêu:** Cart ổn khi demo add/update/remove/checkout.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/CartServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/CartService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/CartDAO.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/CartItemDAO.java`
- `Frontend/src/stores/cart.js`
- `Frontend/src/api/cart.js`
- `Frontend/src/views/guest/CartPage.vue`
- `Frontend/src/views/user/CheckoutPage.vue`

**Việc làm:**

1. Backend add cart:
   - nếu same `cartId + productId + variantId`: tăng quantity
   - không tạo dòng trùng
2. Backend update:
   - check cart item thuộc user
   - check quantity > 0
   - check stock
3. Backend delete:
   - check cart item thuộc user
4. Frontend logged-in:
   - remove gọi API
   - update quantity gọi API
   - fail thì giữ UI cũ/hiện lỗi
5. Checkout lấy cart server mới nhất.

**Kiểm tra:**

- Add same variant 2 lần → 1 dòng, quantity tăng.
- Remove rồi refresh không quay lại.
- User không sửa được cart item user khác.
- Checkout cart đúng.

**Commit:** `fix cart cho demo`

---

### Phần G — Fix shipping fee tối thiểu cho checkout

**Mục tiêu:** Checkout không bị phí ship sai/0 do client gửi bậy.

**Scope:** Không làm GHN vận đơn thật. Chỉ tính phí hiển thị và lưu vào order.

**Files cần kiểm tra/sửa:**

- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShippingServlet.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ShippingService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/utils/GhnClient.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java`
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- `Frontend/src/api/shipping.js`
- `Frontend/src/views/user/CheckoutPage.vue`

**Việc làm:**

1. Backend GHN success và fallback đều trả cùng key:

```json
{
  "shippingFee": 30000
}
```

2. Checkout backend không tin hoàn toàn `shippingFee` client.
3. Nếu có đủ GHN IDs, backend tính lại fee.
4. Nếu thiếu GHN IDs, dùng fallback `DeliveryZone`.
5. Order lưu:
   - `shippingFee`
   - address snapshot
   - GHN IDs nếu có

**Kiểm tra:**

- Checkout có fee.
- Backend không nhận fee âm/0 bất hợp lý.
- GHN fail vẫn checkout được bằng fallback.

**Commit:** `fix phi ship checkout`

---

### Phần H — Checklist demo và verification

**Commands:**

```powershell
cd Backend/FastGuy-FastFoodSite
mvn clean compile
```

```powershell
cd Frontend
npm run build
```

#### Demo COD

1. Login user.
2. Vào menu.
3. Chọn product variant.
4. Add cart.
5. Checkout COD.
6. Verify order `PENDING`, payment `UNPAID`.
7. Login staff.
8. Chuyển `PENDING → CONFIRMED → PREPARING → READY`.
9. Assign shipper.
10. Login shipper.
11. Nhận đơn: `READY → PICKED_UP`.
12. Giao thành công: `PICKED_UP → DELIVERED`.
13. Login user.
14. Verify order `DELIVERED`, payment `PAID`.

#### Demo BANK_TRANSFER

1. Login user.
2. Add cart.
3. Checkout BANK_TRANSFER.
4. Verify QR hiện, nội dung chuyển khoản hiện, order `PENDING`, payment `PENDING`.
5. Trigger webhook/test payment.
6. Verify payment `PAID`.
7. Login staff.
8. Chuyển `PENDING → CONFIRMED → PREPARING → READY`.
9. Assign shipper.
10. Login shipper.
11. Nhận/giao.
12. Login user.
13. Verify order `DELIVERED`, payment `PAID`.

**Definition of Done:**

- `mvn clean compile` pass.
- `npm run build` pass.
- COD demo chạy đủ tới `DELIVERED + PAID`.
- BANK_TRANSFER demo chạy đủ tới `DELIVERED + PAID`.
- Staff không confirm bank order chưa paid.
- User thường không gọi được shipper API.
- Shipper nhận/giao đúng status.
- Không còn frontend status `DELIVERING`.
- Không còn route 404 `/shipper/history`.

**Commit:** `kiem tra luong demo`

---

### Thứ tự làm đề xuất

#### Ngày 1 — Core status + payment

1. Phần A — Chuẩn hóa trạng thái đơn hàng.
2. Phần B — Fix checkout COD/BANK_TRANSFER.
3. Phần C — Chặn staff xử lý bank chưa thanh toán.
4. Chạy backend compile + frontend build.

#### Ngày 2 — Staff + Shipper

1. Phần D — Fix staff flow tới READY và assign shipper.
2. Phần E — Fix shipper nhận/giao đơn.
3. Chạy backend compile + frontend build.
4. Demo COD end-to-end.

#### Ngày 3 — Cart + Shipping + hardening

1. Phần F — Fix cart đủ cho demo.
2. Phần G — Fix shipping fee tối thiểu cho checkout.
3. Phần H — Checklist demo và verification.
4. Demo COD + BANK_TRANSFER end-to-end.

#### Ưu tiên nếu thiếu thời gian

1. Bank QR hiện + payment status đúng.
2. Status `PICKED_UP` thống nhất.
3. Staff chặn bank chưa paid.
4. Shipper nhận/giao + COD set paid.
5. `/shipper/history` 404.
6. Cart remove/update sync server.
7. Shipping fee verify.
