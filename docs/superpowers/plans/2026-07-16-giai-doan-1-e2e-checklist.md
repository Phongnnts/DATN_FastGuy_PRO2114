# E2E Checklist — Giai đoạn 1

## Pre-requisites
- [ ] Fresh/reset database bằng `database/migrate-local.sql`
- [ ] Backend deploy, frontend build OK (`mvn verify` + `npm run build`)
- [ ] Cloudinary upload preset configured
- [ ] GHN token configured (or mock fee = 0)

---

## 1. Auth & Account

### 1.1 Register
- [ ] Register với SĐT mới → redirect login
- [ ] Register trùng SĐT → lỗi "đã tồn tại"
- [ ] Register trùng email → lỗi "đã tồn tại"
- [ ] Register SĐT sai format → lỗi validation

### 1.2 Login
- [ ] Login SĐT + mật khẩu đúng → JWT + redirect role home
- [ ] Login sai mật khẩu → lỗi
- [ ] Login SĐT không tồn tại → lỗi

### 1.3 Change Password
- [ ] Đổi mật khẩu đúng current → thành công, login lại bằng mật khẩu mới
- [ ] Đổi mật khẩu sai current → lỗi
- [ ] Mật khẩu mới yếu (< 8 ký tự) → lỗi

### 1.4 Forgot/Reset Password
- [ ] Nhập email hợp lệ → message trung lập
- [ ] Token invalid/expired → lỗi

---

## 2. Guest → User Flow

### 2.1 Guest Cart
- [ ] Guest thêm sản phẩm với modifier → giỏ hàng hiển thị đúng
- [ ] Guest thêm sản phẩm hết hàng → toast lỗi
- [ ] Guest thêm modifier không đúng min/max → toast lỗi

### 2.2 Cart Migration
- [ ] Login từ guest cart → modifier vẫn còn nguyên
- [ ] Cart có item invalid → rejected list trả về

### 2.3 Product Detail
- [ ] Chọn modifier không đủ min → nútdisabled
- [ ] Chọn modifier vượt max → toast lỗi
- [ ] Click "Mua ngay" → redirect checkout

---

## 3. Checkout & Payment

### 3.1 Address
- [ ] Tạo địa chỉ mới với GHN province/district/ward cascade
- [ ] Set default address → chỉ 1 default
- [ ] Checkout chọn saved address → auto-fill GHN fields + fee

### 3.2 Coupon
- [ ] Claim coupon từ trang khuyến mãi
- [ ] Áp dụng coupon tại checkout → giảm giá đúng
- [ ] Coupon hết hạn/expired → toast lỗi
- [ ] Coupon đã dùng → toast lỗi

### 3.3 COD Checkout
- [ ] Checkout COD → redirect order detail, status = PENDING
- [ ] Stock trừ đúng sau checkout
- [ ] Cart trống sau checkout

### 3.4 PayOS Checkout
- [ ] Checkout PayOS → redirect PayOS page
- [ ] PayOS success → webhook confirm PAID, status = CONFIRMED
- [ ] PayOS fail → order CANCELLED, stock hoàn

### 3.5 Order Timeout
- [ ] BANK_TRANSFER order hết 15 phút → auto CANCELLED

---

## 4. Staff Order Processing

### 4.1 Staff Dashboard
- [ ] Hiển thị pending orders, confirmed orders, orders-by-status

### 4.2 Order Lifecycle
- [ ] PENDING → CONFIRMED (COD tự confirm)
- [ ] PENDING → PayOS link (BANK_TRANSFER)
- [ ] CONFIRMED → PREPARING
- [ ] PREPARING → READY
- [ ] READY → Assign shipper (chỉ 1 shipper)
- [ ] Cancel đơn → status = CANCELLED

### 4.3 Allowed Actions
- [ ] API detail trả về `allowedActions` đúng theo status + role
- [ ] UI chỉ render button mà backend cho phép

---

## 5. Shipper Delivery

### 5.1 Shipper Dashboard
- [ ] Hiển thị đơn available, đơn active, stats

### 5.2 Delivery Flow
- [ ] Pickup đơn READY → status = PICKED_UP
- [ ] Shipper khác pickup đơn đã gán → bị chặn
- [ ] Deliver COD → collected amount phải = finalAmount
- [ ] Deliver thành công → DELIVERED + loyalty EARN
- [ ] Cancel delivery → reason required

### 5.3 Shift
- [ ] Check-in / check-out ca làm việc

---

## 6. Admin

### 6.1 Product CRUD
- [ ] Tạo sản phẩm + variant + modifier group/option
- [ ] Sửa modifier option (name, price, isActive)
- [ ] Xóa modifier group có order reference → soft-delete
- [ ] Tạo combo + combo item

### 6.2 Report
- [ ] Revenue report chỉ tính DELIVERED + PAID
- [ ] Revenue filter theo `deliveredAt`
- [ ] Top product chỉ tính DELIVERED
- [ ] Chart render đúng, period filter hoạt động

### 6.3 Other
- [ ] User CRUD + role management
- [ ] Coupon CRUD + claim/display
- [ ] Banner CRUD
- [ ] Store settings (giờ mở cửa, phí service)

---

## 7. Notification & Support

- [ ] Notification bell hiển thị unread count
- [ ] Mark read / mark all read
- [ ] Tạo support ticket với category + description
- [ ] Staff resolve ticket → notification gửi user

---

## 8. Verification Commands

```bash
# Backend
mvn verify

# Frontend
npm run build

# Git
git diff --check
```

---

## Definition of Done

- [ ] Tất cả checklist items pass
- [ ] Không còn `alert()`/`confirm()` trong luồng order/customer
- [ ] Toast notification hoạt động trên tất cả role
- [ ] `allowedActions` trả về từ backend, UI chỉ render theo
- [ ] Race conditions đã fix (PESSIMISTIC_WRITE)
- [ ] Database migration đã chạy (`CartItemModifier` table)
