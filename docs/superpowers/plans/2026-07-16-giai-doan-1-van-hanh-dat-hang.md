# Giai đoạn 1 — Hoàn thiện vận hành đặt hàng Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện luồng khách chọn món → checkout → thanh toán → bếp/staff → shipper nội bộ → giao/hoàn tất, đồng thời hoàn thiện chức năng khách hàng và admin/báo cáo để demo vận hành được từ đầu đến cuối.

**Architecture:** Giữ Jakarta Servlet 6/JPA/Vue hiện có. Mỗi chức năng đi theo thứ tự `Entity → DAO → Service → Servlet → API frontend → Store/View`; không thêm Spring Boot, framework mới, interface đơn lẻ, hoặc dependency mới. GHN chỉ dùng dữ liệu khu vực và tính phí; giao hàng do shipper nội bộ xử lý.

**Tech Stack:** Java 17, Tomcat 11, Jakarta Servlet 6, JPA/Hibernate, SQL Server, Vue 3, Pinia, Vue Router, Axios, Chart.js, Maven, Vite.

## Global Constraints

- Branch làm việc phải tách từ `develop`; không commit/push thẳng `main`.
- Mỗi mục được phân công = một commit, commit message tiếng Việt không dấu.
- Không commit `.env`, token GHN/PayOS, mật khẩu DB, JWT secret, credentials demo thật.
- Mọi API ghi dữ liệu phải validate input server-side; frontend chỉ hỗ trợ UX, không phải lớp bảo vệ.
- Mọi thay đổi trạng thái đơn phải atomic, lưu `OrderStatusHistory` trong cùng transaction, không read-modify-save detached entity.
- GHN chỉ quote địa chỉ/phí; `shippingProvider` phải nhất quán là `INTERNAL` khi shipper nội bộ giao.
- Giá, tồn kho, phí giao hàng, coupon, tổng tiền luôn tính lại ở backend lúc tạo đơn.
- Không hiển thị địa chỉ, số điện thoại, ghi chú, giá trị đơn trong tracking public.
- Không dùng `alert`, `confirm`, `printStackTrace` cho luồng mới; dùng response lỗi chuẩn và toast chung.
- TDD: thêm test trước khi sửa production code; chạy test đỏ rồi xanh.
- Mọi task phải chạy `mvn verify`, `npm run build`, `git diff --check` trước khi bàn giao nếu không bị module khác chặn.

---

## Phân công và dependency

| Thành viên | Nhánh gợi ý | Ownership chính | Phụ thuộc |
|---|---|---|---|
| 1 — Auth | `feature/auth-customer-security` | Auth, reset/change password, quyền, tracking public | Không phụ thuộc task khác |
| 2 — Guest | `feature/guest-cart-checkout` | Guest cart, coupon UX, checkout | API address của TV3; contract order của TV4 |
| 3 — User | `feature/user-address-account` | Address CRUD GHN, profile, history/review/support | Shipping API hiện có; order detail TV4 |
| 4 — Staff | `feature/staff-order-lifecycle` | State machine đơn, payment, staff/shipper handoff | Contract checkout TV2; API admin report TV5 chỉ đọc |
| 5 — Admin | `feature/admin-reporting` | Product options, inventory, report/dashboard, UX toast | Order state/history TV4 |

### Thứ tự tích hợp

1. TV4 chốt state matrix/API contract đơn hàng trước.
2. TV1 và TV3 có thể triển khai song song.
3. TV2 triển khai checkout sau khi nhận contract TV3/TV4.
4. TV5 triển khai report sau khi nhận trường trạng thái/thời gian từ TV4.
5. Merge về `develop` theo thứ tự: TV4 → TV1/TV3 → TV2 → TV5 → kiểm thử E2E liên module.

## Contract chung phải chốt trước khi code

### State matrix GĐ1

| Từ trạng thái | Actor | Hành động | Sang trạng thái | Điều kiện |
|---|---|---|---|---|
| `PENDING` | STAFF | Confirm | `CONFIRMED` | COD hoặc `paymentStatus=PAID` |
| `PENDING` | USER | Cancel | `CANCELLED` | Chưa staff confirm |
| `PENDING` | SYSTEM | Payment link failed | `CANCELLED` | BANK_TRANSFER, UNPAID, tạo PayOS link thất bại |
| `PENDING` | SYSTEM | Payment expired | `CANCELLED` | BANK_TRANSFER, UNPAID, `paymentExpiresAt < now` |
| `CONFIRMED` | STAFF | Start preparing | `PREPARING` | — |
| `PREPARING` | STAFF | Mark ready | `READY` | — |
| `READY` | STAFF | Assign shipper | `READY` | Shipper ACTIVE, không ghi đè shipper đã gán |
| `READY` | SHIPPER | Pick up | `PICKED_UP` | Đơn gán đúng shipper |
| `PICKED_UP` | SHIPPER | Deliver | `DELIVERED` | COD: số tiền thu = `finalAmount`; chuyển khoản: đã PAID |
| `PICKED_UP` | SHIPPER | Delivery failed | `DELIVERY_FAILED` hoặc ticket hỗ trợ | Không hoàn tồn/coupon tự động |

> Nếu database/schema hiện tại chưa có `DELIVERY_FAILED`, GĐ1 không tự thêm state này. Shipper tạo support ticket và staff/admin xử lý hủy/refund có audit. Không mở rộng scope hoàn tất giao thất bại nếu chưa có UX/logic refund tương ứng.

### Response public tracking

`GET /api/orders/track?code={orderCode}&phoneSuffix={4-so-cuoi}` chỉ được trả:

```json
{
  "orderCode": "GST-AB12CD34",
  "orderStatus": "PREPARING",
  "paymentMethod": "COD",
  "paymentStatus": "UNPAID",
  "createdAt": "2026-07-16T10:30:00",
  "estimatedDeliveryAt": null,
  "items": [{ "name": "Burger", "quantity": 2 }],
  "statusHistory": [{ "status": "PREPARING", "timestamp": "2026-07-16T10:45:00" }]
}
```

Không trả `customerAddress`, `customerPhone`, `deliveryNote`, `internalNote`, `checkoutUrl`, refund fields, user ID, amount, coupon.

---

# Thành viên 4 — Luồng đơn hàng, thanh toán, staff/shipper

### Task 4.1: Gom transition đơn hàng vào một service atomic

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderTransitionService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Orders.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderTransitionServiceTest.java`

**Produces:** `transition(orderId, actorId, actorRole, targetStatus, details)`, `expireUnpaidOrder(orderId, details)`, `cancelFailedPaymentLink(orderId, details)`, `allowedActions(order, role, actorId)`.

- [ ] Viết test matrix toàn bộ transition hợp lệ/không hợp lệ; test actor sai, online unpaid confirm/deliver bị chặn, transaction rollback không ghi history.
- [ ] Chạy `mvn "-Dtest=service.OrderTransitionServiceTest" test`; kỳ vọng FAIL vì service chưa tồn tại.
- [ ] Tạo `OrderTransitionService`; transaction dùng `EntityManager.find(..., LockModeType.PESSIMISTIC_WRITE)`.
- [ ] Trong cùng transaction: validate role/ownership/state, mutate fields, restore stock/coupon khi hủy hợp lệ, persist `OrderStatusHistory`, commit.
- [ ] Chuyển các đường staff confirm/prepare/ready/cancel, shipper pickup/deliver, user cancel sang service này; không giữ logic transition trùng lặp.
- [ ] Chạy test đơn lẻ; kỳ vọng PASS.
- [ ] Commit: `hoan thien luong chuyen trang thai don`.

**Acceptance criteria:** Hai request đồng thời không thể cùng pickup/deliver hoặc đổi trạng thái ngược; history đúng actor/from/to/timestamp; payment chưa PAID không thể confirm/deliver.

### Task 4.2: Sửa flow PayOS và timeout thanh toán

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/PayOSService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/PayOSPaymentService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderScheduler.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/PayOSPaymentServiceTest.java`

**Consumes:** `OrderTransitionService.cancelFailedPaymentLink` và `expireUnpaidOrder` từ Task 4.1.

- [ ] Viết test: PayOS API error trả lỗi 502; order chuyển `CANCELLED` qua `cancelFailedPaymentLink`, không gọi expiry nếu chưa có `paymentExpiresAt`.
- [ ] Viết test webhook: signature sai reject; callback đúng chỉ mark PAID một lần; callback duplicate là no-op.
- [ ] Chạy test; kỳ vọng FAIL ở nhánh cleanup hiện tại.
- [ ] Bảo đảm `createPaymentLink` chỉ persist URL/link ID/expiry khi response PayOS hợp lệ và order còn `PENDING + UNPAID` dưới lock.
- [ ] Nếu link thất bại, gọi `cancelFailedPaymentLink`; trả `502 Không thể tạo link PayOS`; không ném 500.
- [ ] Scheduler chỉ chọn đơn BANK_TRANSFER `PENDING + UNPAID` có `paymentExpiresAt` quá hạn; mỗi đơn lỗi không được làm dừng scheduler.
- [ ] Chạy `mvn "-Dtest=service.PayOSPaymentServiceTest" test`; kỳ vọng PASS.
- [ ] Commit: `sua luong thanh toan payos`.

**Acceptance criteria:** COD tạo đơn 201; PayOS thành công trả checkout URL; PayOS lỗi trả 502 và đơn cancel/audit/hoàn tồn đúng; timeout chỉ hủy đơn hết hạn.

### Task 4.3: Hoàn thiện staff–shipper handoff

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/StaffOrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ShipperService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/StaffOrderServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ShipperServlet.java`
- Modify: `Frontend/src/views/staff/OrderDetailPage.vue`
- Modify: `Frontend/src/views/shipper/OrderDetailPage.vue`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/StaffOrderServiceTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/ShipperServiceTest.java`

- [ ] Viết test assign chỉ cho `READY`, shipper ACTIVE, không gán đè; pickup chỉ shipper được gán; COD amount phải bằng `finalAmount`.
- [ ] Chạy test; kỳ vọng FAIL cho race/authorization đang thiếu.
- [ ] Dùng transition/lock Task 4.1 cho assign/pickup/deliver/cancel.
- [ ] API response đơn staff/shipper phải có `allowedActions`, `assignedShipper`, status history; không lộ payment link.
- [ ] UI chỉ render action backend cho phép; sau action reload detail/list; hiển thị lỗi API bằng toast.
- [ ] Chạy test service và `npm run build`; kỳ vọng PASS.
- [ ] Commit: `hoan thien ban giao don staff shipper`.

**Acceptance criteria:** Staff thấy READY và gán được shipper một lần; shipper khác không pickup; shipper nhận đúng COD; user thấy timeline cập nhật.

---

# Thành viên 1 — Auth, bảo mật khách hàng, tracking public

### Task 1.1: Thay secrets hardcode và chuẩn hóa runtime config

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/utils/AppConfig.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/utils/JwtUtil.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/resources/META-INF/persistence.xml`
- Modify: `.gitignore`
- Modify: `README.md`
- Create: `.env.example`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/utils/AppConfigTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/utils/JwtUtilTest.java`

- [ ] Viết test dev thiếu `JWT_SECRET` tạo key runtime; production thiếu secret fail fast; env priority cao hơn `.env` local.
- [ ] Chạy test; kỳ vọng FAIL với JWT hardcode.
- [ ] `AppConfig.get()` đọc env → system property → `.env` local ignored; không trả token/secret trong API/log.
- [ ] `JwtUtil` không chứa key cố định; production yêu cầu `JWT_SECRET` đủ mạnh; local dev có key ephemeral và token hết hiệu lực sau restart.
- [ ] `persistence.xml` dùng placeholder/config runtime, không commit DB username/password; README chỉ hướng dẫn tạo `.env.example` không chứa secret thật.
- [ ] Chạy test config/JWT; kỳ vọng PASS.
- [ ] Commit: `bao mat cau hinh ung dung`.

### Task 1.2: Password change/reset có API thật

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AuthService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AuthServlet.java`
- Modify: `Frontend/src/api/auth.js`
- Modify: `Frontend/src/views/user/ChangePasswordPage.vue`
- Modify: `Frontend/src/views/guest/ForgotPasswordPage.vue`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/AuthServiceTest.java`

**Scope GĐ1:** Change password cho user đăng nhập. Forgot password chỉ nhận email/phone và trả message trung lập; không giả vờ gửi email khi hệ thống chưa có mail provider.

- [ ] Viết test change password: sai current password 400; new password yếu 400; đúng thì hash mới, password cũ login fail.
- [ ] Chạy test; kỳ vọng FAIL.
- [ ] Thêm `PUT /api/auth/password`; JWT hợp lệ role USER/STAFF/SHIPPER/ADMIN; body `{currentPassword,newPassword}`.
- [ ] Mật khẩu tối thiểu 8 ký tự, có chữ và số; lưu BCrypt/PBKDF đã dùng; không chấp nhận plaintext mới.
- [ ] `POST /api/auth/forgot-password` luôn trả 200 cùng một message, không tiết lộ tài khoản tồn tại; UI ghi rõ tính năng gửi link chưa kích hoạt nếu không cấu hình mail.
- [ ] Dùng toast UI, xoá form sau success; không `setTimeout` success giả.
- [ ] Chạy test và `npm run build`; kỳ vọng PASS.
- [ ] Commit: `hoan thien doi mat khau`.

### Task 1.3: Bảo vệ tracking public và ownership review

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ReviewServlet.java`
- Modify: `Frontend/src/views/guest/TrackOrderPage.vue`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/OrderServletTest.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ReviewServletTest.java`

- [ ] Viết test tracking thiếu/sai `phoneSuffix` trả cùng 400/404; DTO public chỉ có allowlist contract chung.
- [ ] Viết test user A không đọc/ghi review order user B.
- [ ] Chạy test; kỳ vọng FAIL.
- [ ] Track endpoint yêu cầu `phoneSuffix` đúng bốn số; lookup query theo order code, không `findAll().stream()`.
- [ ] Tạo `toPublicTracking()` explicit allowlist; authenticated order detail giữ fields owner cần.
- [ ] Review servlet/service kiểm ownership order trước detail/review action.
- [ ] UI track thêm input 4 số cuối điện thoại, không cache PII; thông báo trung lập khi không tìm thấy.
- [ ] Chạy test và frontend build; kỳ vọng PASS.
- [ ] Commit: `bao ve tra cuu don hang`.

---

# Thành viên 3 — Account khách hàng, địa chỉ GHN, lịch sử/support

### Task 3.1: Address CRUD lưu đủ GHN location và default atomic

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/AddressDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AddressService.java` nếu chưa có thì tạo mới
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AddressServlet.java`
- Modify: `Frontend/src/views/user/ProfilePage.vue`
- Modify: `Frontend/src/api/shipping.js`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/AddressServiceTest.java`

**Produces:** Address object gồm `recipientName`, `phone`, `street`, `provinceName`, `districtName`, `wardName`, `ghnProvinceId`, `ghnDistrictId`, `ghnWardCode`, `isDefault`.

- [ ] Viết test create/update default chạy trong một transaction: sau success chỉ một default; khi validation lỗi default cũ không bị mất.
- [ ] Viết test phone/address/GHN district/ward invalid trả 400, không 500.
- [ ] Chạy test; kỳ vọng FAIL nếu DAO tách transaction.
- [ ] Service transaction: lock/reset default và save address cùng EntityManager transaction.
- [ ] Profile form dùng province/district/ward GHN theo cascade; save IDs + names; `isDefault` checkbox.
- [ ] Sau save reload `savedAddresses`; checkout có thể chọn địa chỉ và quote ngay không nhập lại khu vực.
- [ ] Chạy test và `npm run build`; kỳ vọng PASS.
- [ ] Commit: `hoan thien dia chi giao hang`.

### Task 3.2: Hoàn thiện history, review, support status UX

**Files:**
- Modify: `Frontend/src/views/user/OrdersPage.vue`
- Modify: `Frontend/src/views/user/OrderDetailPage.vue`
- Modify: `Frontend/src/views/user/PurchaseHistoryPage.vue`
- Modify: `Frontend/src/views/user/SupportPage.vue`
- Modify: `Frontend/src/api/order.js`
- Modify: `Frontend/src/api/support.js`
- Test: `Frontend` manual acceptance checklist trong plan task, không thêm dependency test mới.

- [ ] Kiểm tra order detail chỉ hiện cancel khi `allowedActions` chứa `CANCEL`; không tự đoán từ status ở frontend.
- [ ] Hiển thị payment status, PayOS checkout URL chỉ owner/BANK_TRANSFER/PENDING; polling dừng khi PAID/CANCELLED hoặc unmount.
- [ ] Review form chỉ hiện với `canReview`; reload review sau submit; lỗi API rõ ràng.
- [ ] Support ticket hiển thị status/resolution; form category/description required, counter max length theo API.
- [ ] Dùng shared toast từ Task 5.3; không `alert` mới.
- [ ] Chạy `npm run build`; manual verify: logged-in user tạo ticket, xem order, hủy PENDING, review DELIVERED.
- [ ] Commit: `cai thien trang tai khoan khach hang`.

---

# Thành viên 2 — Guest cart, checkout, coupon

### Task 2.1: Guest cart migration không mất modifier

**Files:**
- Modify: `Frontend/src/stores/cart.js`
- Modify: `Frontend/src/views/guest/LoginPage.vue`
- Modify: `Frontend/src/api/cart.js`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AuthServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/CartService.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/CartServiceTest.java`

**Contract:** migrate payload `{items:[{productId,variantId,quantity,modifierOptionIds}]}`. Backend trả `{migrated:[key], rejected:[{key,message}]}`.

- [ ] Viết test migration giữ modifier IDs hợp lệ; item sai trả rejected và không làm hỏng item còn lại.
- [ ] Chạy test; kỳ vọng FAIL với endpoint stub/không có modifier.
- [ ] Backend validate product/variant/modifier/tồn như add-cart; merge only valid items.
- [ ] Frontend `await migrateGuestCart()` trước redirect; chỉ xoá session item `migrated`; giữ rejected item + hiển thị toast.
- [ ] Chạy test và `npm run build`; kỳ vọng PASS.
- [ ] Commit: `sua dong bo gio hang khach`.

### Task 2.2: Coupon UX và checkout authoritative

**Files:**
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Modify: `Frontend/src/api/order.js`
- Modify: `Frontend/src/stores/order.js`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/OrderService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/OrderServiceTest.java`

**Consumes:** Address contract Task 3.1; transition/payment contract Task 4.1/4.2.

- [ ] Viết test quote: cart empty, invalid GHN address, expired/claimed coupon, stock changed; total = authoritative subtotal + GHN fee + service fee − discount.
- [ ] Viết test checkout quote token stale trả `409`; stock/coupon không bị giữ sai sau rollback.
- [ ] Chạy test; kỳ vọng FAIL nếu quote contract thiếu.
- [ ] User checkout gọi `/orders/quote` sau khi cart/location/coupon đổi; create gửi `quoteToken`; backend tính lại/compare fingerprint dưới transaction.
- [ ] Guest checkout không hiển thị coupon input nếu coupon yêu cầu claim/account; trả lỗi rõ nếu gửi coupon không hợp lệ.
- [ ] UI thêm coupon input + apply/remove; disable đặt hàng khi quote loading hoặc location chưa đủ; render reason validation cạnh field.
- [ ] COD thành công: clear cart, redirect owner order/guest track. BANK_TRANSFER: redirect URL PayOS chỉ khi backend trả URL.
- [ ] Chạy test, `mvn verify`, `npm run build`; kỳ vọng PASS.
- [ ] Commit: `hoan thien checkout va ma giam gia`.

### Task 2.3: Product detail validation và checkout feedback

**Files:**
- Modify: `Frontend/src/views/guest/ProductDetailPage.vue`
- Modify: `Frontend/src/views/guest/CartPage.vue`
- Modify: `Frontend/src/components/common/CartItem.vue`
- Modify: `Frontend/src/stores/cart.js`

- [ ] Tạo `missingModifierGroups` computed từ `minSelections/maxSelections`; nút add/buy disabled nếu invalid; hiển thị tên group thiếu và `aria-live` message.
- [ ] Khi API add/update cart reject stock/option, giữ UI nhất quán với server bằng reload cart; show toast error.
- [ ] Cart item hiển thị modifier snapshot/name/extra price; quantity control không vượt `quantityAvailable`.
- [ ] Chạy `npm run build`; manual verify modifier bắt buộc, modifier max, item out-of-stock, buy-now redirect checkout.
- [ ] Commit: `cai thien gio hang va tuy chon mon`.

---

# Thành viên 5 — Admin, báo cáo, phản hồi UI chung

### Task 5.1: Product option/combo CRUD đủ vòng đời

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminProductServlet.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java`
- Modify: `Frontend/src/views/admin/ProductsPage.vue`
- Modify: `Frontend/src/api/admin.js`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/service/AdminServiceTest.java`

- [ ] Viết test create/update/delete modifier group/option: product ownership, active flag, `0 <= minSelections <= maxSelections`, price không âm.
- [ ] Viết test combo item chỉ tham chiếu variant thuộc đúng product.
- [ ] Chạy test; kỳ vọng FAIL cho edit/delete chưa hỗ trợ.
- [ ] Bổ sung endpoint/service CRUD tối thiểu cho group, option, combo item; không hard delete nếu đã tham chiếu order, dùng `isActive=false`.
- [ ] Admin UI chỉnh sửa/xóa/disable hiện có; confirm qua modal chung; upload Cloudinary validate MIME image và size trước upload, hiển thị lỗi upload.
- [ ] Chạy test và frontend build; kỳ vọng PASS.
- [ ] Commit: `hoan thien quan ly tuy chon san pham`.

### Task 5.2: Report thống nhất dữ liệu delivered/revenue

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/OrdersDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/AdminServlet.java`
- Modify: `Frontend/src/views/admin/DashboardPage.vue`
- Modify: `Frontend/src/views/admin/RevenueReportPage.vue`
- Modify: `Frontend/src/views/admin/TopProductsReportPage.vue`
- Test: `Backend/FastGuy-FastFoodSite/src/test/java/dao/OrdersDAOTest.java`

**Report contract:** revenue chỉ `DELIVERED` và `paymentStatus=PAID`; khoảng ngày dựa `deliveredAt`; canceled/refunded tách metric riêng; top product chỉ đơn delivered.

- [ ] Viết test dataset chứa PENDING, CANCELLED, DELIVERED/PAID, DELIVERED/UNPAID, REFUNDED; assert revenue/top-product chỉ tính đúng record.
- [ ] Chạy test; kỳ vọng FAIL với query cũ.
- [ ] Sửa DAO query filter/date field; API trả empty arrays/zero totals hợp lệ, không `null`.
- [ ] Dashboard/report render loading/empty/error; chart destroy/recreate không memory leak; currency/date format dùng utility hiện có.
- [ ] Chạy test + `npm run build`; kỳ vọng PASS.
- [ ] Commit: `sua bao cao doanh thu`.

### Task 5.3: Shared toast, error handling, operational refresh

**Files:**
- Create: `Frontend/src/stores/toast.js`
- Create: `Frontend/src/components/common/AppToast.vue`
- Modify: `Frontend/src/App.vue`
- Modify: `Frontend/src/api/client.js`
- Modify: `Frontend/src/views/admin/OrdersPage.vue`
- Modify: `Frontend/src/views/staff/OrdersPage.vue`
- Modify: `Frontend/src/views/shipper/DashboardPage.vue`
- Modify: `Frontend/src/views/shipper/MyOrdersPage.vue`

**Interface:** `const toast = useToastStore(); toast.success(message); toast.error(message);`.

- [ ] Implement Pinia queue `{id,type,message}` with auto-dismiss 4 seconds and manual dismiss.
- [ ] `AppToast` renders `role="status"` for success, `role="alert"` for error, accessible dismiss button.
- [ ] Axios interceptor preserves backend `message`; 401 clears session and routes login once, no page hard reload.
- [ ] Thay các `alert/confirm` chạm vào luồng order/customer/admin đang sửa ở task này bằng toast/modal.
- [ ] Staff/shipper refresh on mount + window focus; polling 20–30 seconds only while view visible, clear interval on unmount.
- [ ] Chạy `npm run build`; manual verify toast, 401, staff READY list refresh, shipper assignment refresh.
- [ ] Commit: `cai thien phan hoi giao dien`.

---

## Kiểm thử tích hợp cuối GĐ1

### Task I.1: E2E manual checklist và database verification

**Owner:** TV4 chủ trì; TV1–TV5 mỗi người xác nhận module mình.

**Files:**
- Create: `docs/superpowers/plans/2026-07-16-giai-doan-1-e2e-checklist.md`
- Modify nếu phát hiện bug nhỏ trong phạm vi task owner.

- [ ] Fresh/reset database bằng script versioned đã được team thống nhất; không chạy `init.sql` lên DB có dữ liệu.
- [ ] Đăng ký/login, đổi mật khẩu, đăng xuất/login bằng mật khẩu mới.
- [ ] Guest thêm món có modifier, login, xác nhận modifier còn nguyên.
- [ ] User tạo/sửa default address GHN, checkout COD, verify quote/tổng tiền/stock/order history/public tracking không lộ PII.
- [ ] User checkout PayOS test credentials: success webhook PAID; failure/timeout cancel, tồn/coupon rollback đúng.
- [ ] Staff confirm → prepare → ready → assign shipper; shipper pickup → collect COD → deliver; verify history/loyalty/report.
- [ ] Admin tạo/sửa/disable modifier/combo; product detail phản ánh đúng.
- [ ] Admin report kiểm canceled/refunded không bị tính revenue/top product.
- [ ] Run: `mvn verify`.
- [ ] Run: `npm run build`.
- [ ] Run: `git diff --check`.
- [ ] Chỉ merge từng task sau review chéo của ít nhất một thành viên khác; không commit/push secrets.

## Definition of Done GĐ1

- [ ] Toàn bộ state matrix được test service-level và E2E manual.
- [ ] Không còn public endpoint trả PII tracking; review enforced ownership.
- [ ] User đặt COD/PayOS không rơi 500 khi payment link lỗi; mỗi đơn có audit history đúng.
- [ ] Address saved dùng lại được trong checkout; guest cart không mất modifier sau login.
- [ ] Admin sửa được product options/combo và report revenue đúng delivered/paid.
- [ ] Backend `mvn verify`, frontend `npm run build`, `git diff --check` pass tại `develop`.
- [ ] README cập nhật environment variables, local run, migration, test commands sau khi các task hoàn tất.
