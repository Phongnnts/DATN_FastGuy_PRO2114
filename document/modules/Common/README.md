# Module Common

**Người phụ trách**: Người 6

## Mục tiêu

Quản lý phần dùng chung: layouts, constants, styles, router, helper, docs, build/test. Đồng thời hỗ trợ Database và Shipping vì đây là phần nền tảng của giai đoạn mới.

---

## Files

### Layouts

- `layouts/GuestLayout.vue`
- `layouts/UserLayout.vue`
- `layouts/StaffLayout.vue`
- `layouts/AdminLayout.vue`

### Styles

- `assets/styles/global.css`
- `assets/styles/variables.css`

### Utils

- `utils/constants.js`
- `utils/format.js`
- `utils/helpers.js`
- `utils/validators.js`

### Components

- `components/common/ProductCard.vue`
- `components/common/OrderStatusBadge.vue`
- `components/common/OrderTimeline.vue`
- `components/common/HeroBanner.vue`

### Router

- `router/index.js`

### Backend common

- `utils/DatabaseUtil.java`
- `utils/JsonUtil.java`
- `utils/ApiResponse.java`

---

## Constants cần cập nhật

- Cloudinary config.
- Order status:
  - PENDING
  - CONFIRMED
  - PREPARING
  - READY
  - CANCELLED
- Payment status:
  - UNPAID
  - PAID
- Shipping provider:
  - GHN
  - FALLBACK_ZONE
- Product status:
  - AVAILABLE
  - UNAVAILABLE

---

## Việc cần làm

### Frontend common

- [ ] Cập nhật `constants.js`.
- [ ] Kiểm tra `formatPrice`.
- [ ] Kiểm tra `formatDate`.
- [ ] Kiểm tra `OrderStatusBadge` đủ trạng thái.
- [ ] Kiểm tra `OrderTimeline` không duplicate trạng thái.
- [ ] Kiểm tra layouts/sidebar.
- [ ] Bỏ link Ingredients/LowStock nếu không còn dùng.

### Backend common

- [ ] `ApiResponse` trả error nhất quán.
- [ ] `JsonUtil` parse request ổn định.
- [ ] `DatabaseUtil` không hardcode secret.

### Docs/Test

- [ ] Cập nhật `document/WORK_PLAN.md`.
- [ ] Cập nhật README module.
- [ ] Viết checklist demo.
- [ ] Chạy backend compile.
- [ ] Chạy frontend build.

---

## Cloudinary Config

```js
CLOUDINARY: {
  cloudName: 'ds4dnsj0o',
  uploadPreset: 'upload-fastguy',
  uploadUrl: 'https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload',
}
```

---

## Checklist test

- [ ] Layout không vỡ.
- [ ] Sidebar đúng role.
- [ ] Status badge đúng màu.
- [ ] Timeline đúng thứ tự.
- [ ] Format tiền/ngày đúng.
- [ ] Backend compile.
- [ ] Frontend build.

---

## Phụ thuộc

- Tất cả module khác phụ thuộc Common.
