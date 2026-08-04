# FastGuy — SiteMap

Sơ đồ trang (sitemap) chi tiết theo vai trò của hệ thống **FastGuy**, dựa trên cấu hình route hiện tại của frontend (`Frontend/src/router/index.js`). Ứng dụng là SPA (Vue Router, history mode), backend phục vụ qua `/api` với context `/FastGuy`.

**Quy ước cột:**

- **Tên route**: name đăng ký trong router.
- **Component**: file view hiển thị.
- **Điều kiện**: yêu cầu truy cập (auth / role / shift).
- **SEO**: gợi ý robots cho trang tương ứng.

---

## 1. SiteMap tổng quan

```text
/
├── Public / Guest (không cần đăng nhập)
├── /account/**  (User — requiresAuth + role USER)
├── /staff/**    (Staff — requiresAuth + role STAFF, nghiệp vụ cần CHECKED_IN)
├── /shipper/**  (Shipper — requiresAuth + role SHIPPER, nghiệp vụ cần CHECKED_IN)
├── /admin/**    (Admin — requiresAuth + role ADMIN)
├── /reports, /loyalty, /history  (redirect)
└── /*           (404)
```

---

## 2. Guest / Public

Layout: `GuestLayout.vue`. Tất cả route dưới `meta.guest`.

| Route | Tên route | Component | Mô tả | Điều kiện | SEO |
| ----- | --------- | --------- | ----- | --------- | --- |
| `/` | Login | `LoginPage.vue` | Đăng nhập mọi vai trò; redirect theo role hoặc `redirect` query | Không cần đăng nhập; đã đăng nhập → về trang theo role | noindex |
| `/login` | — | redirect `Login` | Alias đăng nhập | | noindex |
| `/home` | Home | `HomePage.vue` | Trang chủ storefront: banner, danh mục, sản phẩm nổi bật/bán chạy, combo | | index |
| `/menu` | Menu | `MenuPage.vue` | Thực đơn: tìm kiếm, lọc, sắp xếp, phân trang | | index |
| `/product/:id` | ProductDetail | `ProductDetailPage.vue` | Chi tiết món: gallery, variant, modifier, combo, thêm giỏ | | index |
| `/promotions` | Promotions | `PromotionsPage.vue` | Khuyến mãi, coupon công khai, claim/copy mã | | index |
| `/cart` | Cart | `CartPage.vue` | Giỏ hàng: thêm/sửa/xóa, ghi chú, tổng tiền | | noindex |
| `/checkout` | Checkout | `CheckoutPage.vue` | Thanh toán 4 bước: người nhận, địa chỉ, coupon/thanh toán, xác nhận; Guest/User | Guest hoặc User | noindex |
| `/order-success` | OrderSuccess | `OrderSuccessPage.vue` | Xác nhận đặt hàng; User xem biên nhận chi tiết, Guest xem mã đơn | | noindex |
| `/track-order` | TrackOrder | `TrackOrderPage.vue` | Tra cứu đơn bằng mã + 4 số cuối điện thoại, theo dõi trạng thái | | noindex |
| `/payment-return` | PaymentReturn | `PaymentReturnPage.vue` | Trang return PayOS: xác minh trạng thái thanh toán | | noindex |
| `/register` | Register | `RegisterPage.vue` | Đăng ký tài khoản User | | noindex |
| `/forgot-password` | ForgotPassword | `ForgotPasswordPage.vue` | Yêu cầu đặt lại mật khẩu | | noindex |
| `/reset-password` | ResetPassword | `ResetPasswordPage.vue` | Hoàn tất đặt lại mật khẩu bằng token | | noindex |
| `/help` | Help | `HelpPage.vue` | Trung tâm trợ giúp: FAQ, giao hàng, thanh toán, hủy/hoàn | | index |
| `/terms` | Terms | `TermsPage.vue` | Điều khoản sử dụng | | index |
| `/privacy` | Privacy | `PrivacyPage.vue` | Chính sách bảo mật | | index |

---

## 3. User (`/account`)

Layout: `UserLayout.vue` + `AccountTabs.vue`. `requiresAuth: true`, `role: USER`.

| Route | Tên route | Component | Mô tả | Điều kiện | SEO |
| ----- | --------- | --------- | ----- | --------- | --- |
| `/account` | — | redirect `AccountOverview` | Trang mặc định của tài khoản | USER | noindex |
| `/account/overview` | AccountOverview | `AccountOverviewPage.vue` | Tổng quan tài khoản: đơn hoạt động, điểm thưởng, shortcut | USER | noindex |
| `/account/profile` | Profile | `ProfilePage.vue` | Hồ sơ cá nhân: tên, email, điện thoại | USER | noindex |
| `/account/orders` | UserOrders | `OrdersPage.vue` | Danh sách đơn hàng, tab trạng thái, tìm kiếm | USER | noindex |
| `/account/orders/:id` | UserOrderDetail | `OrderDetailPage.vue` | Chi tiết đơn, timeline, hủy (PENDING), reorder, đánh giá | USER + ownership | noindex |
| `/account/favorites` | UserFavorites | `FavoritesPage.vue` | Món yêu thích | USER | noindex |
| `/account/addresses` | UserAddresses | `AddressesPage.vue` | Sổ địa chỉ: CRUD, đặt mặc định, GHN | USER | noindex |
| `/account/coupons` | UserCoupons | `CouponWalletPage.vue` | Ví mã ưu đãi đã claim | USER | noindex |
| `/account/rewards` | UserRewards | `RewardsPage.vue` | Điểm thưởng, hạng thành viên, lịch sử giao dịch | USER | noindex |
| `/account/notifications` | UserNotifications | `NotificationsPage.vue` | Hộp thông báo, đánh dấu đã đọc | USER | noindex |
| `/account/support` | UserSupport | `SupportPage.vue` | Yêu cầu hỗ trợ, danh sách ticket | USER | noindex |
| `/account/change-password` | ChangePassword | `ChangePasswordPage.vue` | Đổi mật khẩu | USER | noindex |
| `/account/history` | — | redirect `/account/orders?status=DELIVERED` | Lịch sử đã giao | USER | noindex |

---

## 4. Staff (`/staff`)

Layout: `StaffLayout.vue`. `requiresAuth: true`, `role: STAFF`. Các route nghiệp vụ đánh dấu **[shift]** yêu cầu ca hiện tại `CHECKED_IN`; nếu chưa → về `/staff/shifts`.

| Route | Tên route | Component | Mô tả | Điều kiện | SEO |
| ----- | --------- | --------- | ----- | --------- | --- |
| `/staff` | StaffDashboard | `DashboardPage.vue` | Tổng quan bếp: hàng đợi, thống kê ca | STAFF + [shift] | noindex |
| `/staff/orders` | StaffOrders | `OrdersPage.vue` | Kitchen queue: tabs PENDING/CONFIRMED/PREPARING/READY, tìm kiếm, đơn quá hạn | STAFF + [shift] | noindex |
| `/staff/orders/:id` | StaffOrderDetail | `OrderDetailPage.vue` | Chi tiết đơn: confirm/preparing/ready/cancel, gán Shipper, ghi chú | STAFF + [shift] | noindex |
| `/staff/dispatch` | StaffDispatch | `DispatchPage.vue` | Điều phối giao hàng: đơn READY chưa gán + Shipper + workload | STAFF + [shift] | noindex |
| `/staff/orders/history` | StaffOrderHistory | `OrderHistoryPage.vue` | Lịch sử đơn đã xử lý (không cần check-in) | STAFF | noindex |
| `/staff/support` | StaffSupport | `SupportPage.vue` | Xử lý support ticket | STAFF | noindex |
| `/staff/shifts` | StaffShifts | `StaffShiftsPage.vue` | Ca làm: lịch, check-in/check-out | STAFF | noindex |
| `/staff/kitchen` | — | redirect `StaffOrders` | Alias kitchen | STAFF | noindex |

---

## 5. Shipper (`/shipper`)

Layout: `ShipperLayout.vue`. `requiresAuth: true`, `role: SHIPPER`. Các route **[shift]** yêu cầu `CHECKED_IN`; chưa check-in → `/shipper/shifts`.

| Route | Tên route | Component | Mô tả | Điều kiện | SEO |
| ----- | --------- | --------- | ----- | --------- | --- |
| `/shipper` | ShipperDashboard | `DashboardPage.vue` | Tổng quan giao hàng: đơn active, đã nhận/giao hôm nay, đơn tiếp theo | SHIPPER + [shift] | noindex |
| `/shipper/orders` | ShipperOrders | `MyOrdersPage.vue` | Đơn được gán: chờ lấy hàng / đang giao | SHIPPER + [shift] | noindex |
| `/shipper/orders/:id` | ShipperOrderDetail | `OrderDetailPage.vue` | Chi tiết đơn giao: gọi khách, Maps, pickup/deliver, COD | SHIPPER + [shift]; đơn `DELIVERED`/`CANCELLED` xem được ngoài ca | noindex |
| `/shipper/history` | ShipperOrderHistory | `MyOrdersPage.vue` | Lịch sử giao (DELIVERED + CANCELLED), lọc ngày | SHIPPER | noindex |
| `/shipper/shifts` | ShipperShifts | `ShipperShiftsPage.vue` | Ca làm: lịch, check-in/check-out | SHIPPER | noindex |
| `/shipper/orders/history` | — | redirect `ShipperOrderHistory` | Alias lịch sử | SHIPPER | noindex |

---

## 6. Admin (`/admin`)

Layout: `AdminLayout.vue`. `requiresAuth: true`, `role: ADMIN`.

| Route | Tên route | Component | Mô tả | Điều kiện | SEO |
| ----- | --------- | --------- | ----- | --------- | --- |
| `/admin` | AdminDashboard | `DashboardPage.vue` | Tổng quan quản trị: doanh thu, đơn, sản phẩm bán chạy | ADMIN | noindex |
| `/admin/users` | AdminUsers | `UsersPage.vue` | Quản lý người dùng: CRUD, role/status, xem đơn | ADMIN | noindex |
| `/admin/products` | AdminProducts | `ProductsPage.vue` | Catalog sản phẩm: KPI, lọc, sort, phân trang, ẩn sản phẩm | ADMIN | noindex |
| `/admin/products/new` | AdminProductCreate | `ProductEditorPage.vue` | Tạo sản phẩm: General, Media, Variants (+ Modifiers/Combo sau khi có ID) | ADMIN | noindex |
| `/admin/products/:id/edit` | AdminProductEdit | `ProductEditorPage.vue` | Chỉnh sửa sản phẩm: General, Media, Variants, Modifiers, Combo | ADMIN | noindex |
| `/admin/inventory` | AdminInventory | `InventoryPage.vue` | Tồn kho theo biến thể; link chỉnh sửa sang ProductEditor | ADMIN | noindex |
| `/admin/categories` | AdminCategories | `CategoriesPage.vue` | Quản lý danh mục | ADMIN | noindex |
| `/admin/orders` | AdminOrders | `OrdersPage.vue` | Quản lý đơn: lọc, hoàn tiền | ADMIN | noindex |
| `/admin/orders/:id` | AdminOrderDetail | `OrderDetailPage.vue` | Chi tiết đơn, hủy, ghi chú, timeline | ADMIN | noindex |
| `/admin/reports` | AdminReports | `ReportsPage.vue` | Báo cáo thống kê, biểu đồ | ADMIN | noindex |
| `/admin/coupons` | AdminCoupons | `CouponsPage.vue` | Quản lý mã giảm giá | ADMIN | noindex |
| `/admin/banners` | AdminBanners | `BannersPage.vue` | Quản lý banner | ADMIN | noindex |
| `/admin/shifts` | AdminShifts | `ShiftsPage.vue` | Phân ca Staff/Shipper | ADMIN | noindex |
| `/admin/settings` | AdminSettings | `SettingsPage.vue` | Cấu hình cửa hàng | ADMIN | noindex |

---

## 7. Route chuyển hướng (Redirect)

| Route cũ / alias | Chuyển đến | Ghi chú |
| ---------------- | ---------- | ------- |
| `/login` | `/` | alias đăng nhập |
| `/account` | `/account/overview` | trang mặc định tài khoản |
| `/account/history` | `/account/orders?status=DELIVERED` | lịch sử đã giao |
| `/loyalty` | `/account/rewards` | ví điểm |
| `/history` | `/account/orders?status=DELIVERED` | alias toàn cục |
| `/reports` | `/admin/reports` | báo cáo admin |
| `/staff/kitchen` | `/staff/orders` | alias kitchen |
| `/shipper/orders/history` | `/shipper/history` | alias lịch sử |
| `/admin/products?edit=:id` | `/admin/products/:id/edit` | migration legacy query (guard `beforeEach`; query không hợp lệ → về catalog) |

---

## 8. Trang 404

| Route | Tên route | Component | Mô tả | SEO |
| ----- | --------- | --------- | ----- | --- |
| `/:pathMatch(.*)*` | NotFound | `NotFoundPage.vue` | Trang không tìm thấy, CTA về trang chủ | noindex |

---

## 9. Guard toàn cục (điều kiện truy cập)

| Guard | Hành vi |
| ----- | ------- |
| Đã đăng nhập truy cập `/` (Login) | Redirect theo role: USER → `/home`, STAFF → `/staff`, ADMIN → `/admin`, SHIPPER → `/shipper` |
| Guest route + đã đăng nhập + không phải USER | Redirect về trang theo role |
| Route cần auth nhưng chưa đăng nhập | Redirect `/` kèm `redirect=<fullPath>` |
| Route cần role nhưng sai role | Redirect về trang theo role hiện tại |
| Route `requiresCheckedInShift` | Gọi `/shifts/current`; không `CHECKED_IN` → STAFF về `/staff/shifts`, SHIPPER về `/shipper/shifts`; lỗi API → cùng fallback |
| Sau mỗi navigation | Cập nhật `document.title` từ `meta.title` |

---

## 10. Ghi chú SEO

| Nhóm trang | Robots | Lý do |
| ---------- | ------ | ----- |
| Home, Menu, Product, Promotions, Help, Terms, Privacy | `index` | Trang công khai, có giá trị tìm kiếm |
| Login, Register, auth, Cart, Checkout, Order Success, Tracking, Payment Return | `noindex` | Trang giao dịch/bảo mật |
| Account, Staff, Shipper, Admin | `noindex` | Trang cá nhân/vận hành, yêu cầu đăng nhập |
