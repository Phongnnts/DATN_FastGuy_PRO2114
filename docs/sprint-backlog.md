# Sprint Backlog — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx`.
Chia theo 6 Sprint, mỗi Sprint 20 task (tổng 120 task).

| Task ID | Task | Description | Story ID | Backlog ID | Sprint# | State | Estimate Time (Hours) | Assign to | Note |
| ------- | ---- | ----------- | -------- | ---------- | ------- | ----- | --------------------- | --------- | ---- |
| T001 | Thiết kế Database User/Auth | Bảng Users, PasswordResetToken, unique phone/email | US001 | PB01 | 1 | Xong | 4 | Nam Phong | |
| T002 | API đăng ký | POST /api/auth/register, hash mật khẩu PBKDF2 | US001 | PB01 | 1 | Xong | 6 | Nam Phong | |
| T003 | Frontend trang đăng ký | Form đăng ký + validation client | US001 | PB01 | 1 | Xong | 6 | Phúc Khang | |
| T004 | API đăng nhập | POST /api/auth/login trả JWT, chuẩn hóa email | US002 | PB01 | 1 | Xong | 6 | Nam Phong | |
| T005 | Frontend đăng nhập + redirect theo role | Trang login, xử lý redirect query | US002 | PB01 | 1 | Xong | 6 | Phúc Khang | |
| T006 | Đăng xuất + xóa phiên | Logout xóa session/token phía client | US003 | PB01 | 1 | Xong | 2 | Phúc Khang | |
| T007 | API đặt lại mật khẩu | forgot-password + reset-password với token | US005 | PB01 | 1 | Xong | 6 | Nam Phong | |
| T008 | Frontend forgot/reset password | Hai trang quên/đặt lại mật khẩu | US005 | PB01 | 1 | Xong | 5 | Phúc Khang | |
| T009 | Khóa tài khoản khi sai mật khẩu | Đếm lần sai, khóa tạm thời | US008 | PB01 | 1 | Mới | 6 | Nam Phong | Backlog bảo mật |
| T010 | API đổi mật khẩu | change-password kiểm tra mật khẩu cũ | US004 | PB02 | 1 | Xong | 4 | Nam Phong | |
| T011 | Frontend đổi mật khẩu | Trang đổi mật khẩu + strength | US004 | PB02 | 1 | Xong | 4 | Phúc Khang | |
| T012 | API hồ sơ | GET/PUT /api/auth/me, cập nhật hồ sơ | US006,US007 | PB02 | 1 | Xong | 5 | Nam Phong | |
| T013 | Frontend hồ sơ | Trang profile + chỉnh sửa | US006,US007 | PB02 | 1 | Xong | 5 | Phúc Khang | |
| T014 | Chuẩn hóa email/trim đăng nhập | Lowercase email + trim input | US009,US010 | PB02 | 1 | Xong | 2 | Nam Phong | |
| T015 | Thiết kế bảng Address | Địa chỉ + GHN ids + default | US011 | PB02 | 1 | Xong | 3 | Nam Phong | |
| T016 | API địa chỉ | CRUD /api/user/addresses, default duy nhất | US011,US012 | PB02 | 1 | Xong | 6 | Nam Phong | |
| T017 | Frontend sổ địa chỉ | Trang /account/addresses + modal GHN | US011,US012 | PB02 | 1 | Xong | 8 | Phúc Khang | |
| T018 | API danh mục + sản phẩm công khai | GET /api/categories, /api/products + filters | US013-017 | PB03 | 1 | Xong | 8 | Nam Phong | |
| T019 | Frontend thực đơn | MenuPage search/filter/sort/pagination + URL state | US013-017 | PB03 | 1 | Xong | 10 | Phúc Khang | |
| T020 | Frontend chi tiết sản phẩm | ProductDetail variant/modifier/combo | US016 | PB03 | 1 | Xong | 8 | Phúc Khang | |
| T021 | Thiết kế bảng Cart/CartItem | Giỏ theo user/session, modifiers_json | US019-023 | PB04 | 2 | Xong | 4 | Nam Phong | |
| T022 | API giỏ hàng | POST/PUT/DELETE /api/cart, kiểm tra stock/modifier | US019-022 | PB04 | 2 | Xong | 8 | Nam Phong | |
| T023 | Giỏ theo session + hợp nhất | Cart theo sessionId, migrate sau login | US023 | PB04 | 2 | Xong | 5 | Nam Phong | |
| T024 | Frontend giỏ hàng | CartPage thêm/sửa/xóa, cảnh báo hết hàng | US019-022 | PB04 | 2 | Xong | 8 | Phúc Khang | |
| T025 | API checkout COD | POST /api/orders + guest-checkout, tạo đơn PENDING | US024,US026 | PB05 | 2 | Xong | 10 | Nam Phong | |
| T026 | Tích hợp PayOS | createPaymentLink, webhook có chữ ký | US025 | PB05 | 2 | Xong | 10 | Nam Phong | |
| T027 | Idempotency checkout | Idempotency-Key + request hash + replay | US027 | PB05 | 2 | Xong | 6 | Nam Phong | |
| T028 | Frontend checkout | CheckoutPage 4 bước, saved address, fee GHN | US026,US114,US115 | PB05 | 2 | Xong | 12 | Phúc Khang | |
| T029 | Trang payment return | PaymentReturnPage xác minh trạng thái | US043 | PB05 | 2 | Xong | 6 | Phúc Khang | |
| T030 | API coupon verify + áp dụng | Verify PERCENT/FIXED/FREE_SHIPPING | US028 | PB06 | 2 | Xong | 8 | Nam Phong | |
| T031 | Cart signature | Signature giỏ + phát hiện thay đổi | US029 | PB06 | 2 | Xong | 4 | Nam Phong | |
| T032 | Claim + ví coupon | Claim, giới hạn một lần/user | US040 | PB06 | 2 | Xong | 6 | Nam Phong | |
| T033 | Frontend ví mã | Trang /account/coupons | US040,US093 | PB06 | 2 | Xong | 5 | Phúc Khang | |
| T034 | Tạo đơn trả orderCode | Success page + mã đơn | US030 | PB07 | 2 | Xong | 4 | Phúc Khang | |
| T035 | API tra cứu đơn | GET /api/orders/track code + phoneSuffix | US031 | PB07 | 2 | Xong | 6 | Nam Phong | |
| T036 | Frontend track order | TrackOrderPage + timeline + poll 30s | US031,US111 | PB07 | 2 | Xong | 8 | Phúc Khang | |
| T037 | API lịch sử + chi tiết đơn | GET /api/orders, /api/orders/{id} ownership | US032,US033 | PB07 | 2 | Xong | 8 | Nam Phong | |
| T038 | Frontend danh sách đơn user | OrdersPage tab trạng thái + tìm kiếm | US032 | PB07 | 2 | Xong | 7 | Phúc Khang | |
| T039 | API hủy đơn | PUT cancel, chính sách trạng thái + WASTE | US034 | PB07 | 2 | Xong | 6 | Nam Phong | |
| T040 | API đánh giá | POST /api/reviews chỉ đơn DELIVERED | US036 | PB08 | 2 | Xong | 5 | Nam Phong | |
| T041 | API yêu thích | Toggle + danh sách favorites | US037 | PB08 | 2 | Xong | 4 | Nam Phong | |
| T042 | API loyalty | GET /api/loyalty/me + award khi DELIVERED | US038,US039 | PB08 | 2 | Xong | 6 | Nam Phong | |
| T043 | Frontend điểm thưởng | RewardsPage + LoyaltyWallet | US038 | PB08 | 2 | Xong | 5 | Phúc Khang | |
| T044 | API thông báo | GET/PUT /api/notifications, unread count | US041 | PB08 | 2 | Xong | 5 | Nam Phong | |
| T045 | Frontend thông báo | NotificationBell + inbox /account/notifications | US041 | PB08 | 2 | Xong | 6 | Phúc Khang | |
| T046 | API hỗ trợ | POST/GET /api/support, category, trạng thái | US042 | PB09 | 3 | Xong | 6 | Nam Phong | |
| T047 | Frontend hỗ trợ | SupportPage + StaffSupportPage | US042,US054 | PB09 | 3 | Xong | 8 | Phúc Khang | |
| T048 | API ca làm | /api/shifts current/mine/check-in/check-out | US044,US055 | PB10 | 3 | Xong | 8 | Nam Phong | |
| T049 | Frontend ca làm Staff/Shipper | StaffShiftsPage + ShipperShiftsPage | US044,US055 | PB10 | 3 | Xong | 8 | Phúc Khang | |
| T050 | API kitchen queue | /api/staff/orders theo trạng thái FIFO | US045-048 | PB10 | 3 | Xong | 10 | Nam Phong | |
| T051 | Frontend kitchen board | OrdersPage tab + search + quá hạn + modifiers | US045,US046,US099,US100 | PB10 | 3 | Xong | 12 | Phúc Khang | |
| T052 | Chuyển trạng thái Staff | Transition CONFIRMED/PREPARING/READY | US047,US048 | PB10 | 3 | Xong | 8 | Nam Phong | |
| T053 | Hủy đơn Staff | Cancel + failureReason + WASTE | US049 | PB11 | 3 | Xong | 5 | Nam Phong | |
| T054 | Ghi chú nội bộ đơn | POST notes + hiển thị | US052 | PB11 | 3 | Xong | 3 | Nam Phong | |
| T055 | Staff history + support | /api/staff/orders/history + support queue | US053,US054 | PB11 | 3 | Xong | 6 | Nam Phong | |
| T056 | API phân công Shipper | assign READY→ASSIGNED, active+checked-in | US050 | PB12 | 3 | Xong | 6 | Nam Phong | |
| T057 | Board điều phối | DispatchPage ready + shipper + workload | US051 | PB12 | 3 | Xong | 8 | Phúc Khang | |
| T058 | API shipper orders | /api/shipper/orders/mine/active/history + detail | US056,US057,US102 | PB12 | 3 | Xong | 8 | Nam Phong | |
| T059 | Frontend shipper list | MyOrdersPage pickup/delivering/history | US056 | PB12 | 3 | Xong | 8 | Phúc Khang | |
| T060 | API pickup/deliver + COD | PUT pickup/deliver, COD exact amount | US059,US060 | PB13 | 3 | Xong | 8 | Nam Phong | |
| T061 | Frontend shipper detail | OrderDetailPage gọi khách + Maps + COD | US057,US058 | PB13 | 3 | Xong | 10 | Phúc Khang | |
| T062 | Shipper dashboard | DashboardPage stats + active + next order | US061 | PB13 | 3 | Xong | 6 | Phúc Khang | |
| T063 | Shipper history filter | Lọc ngày trong lịch sử giao | US062,US113 | PB13 | 3 | Xong | 5 | Phúc Khang | |
| T064 | API admin users | CRUD user, role/status | US063 | PB14 | 4 | Xong | 8 | Nam Phong | |
| T065 | Frontend admin users | UsersPage + modal đơn của user | US063 | PB14 | 4 | Xong | 8 | Phúc Khang | |
| T066 | API admin categories | CRUD category + chặn xóa có sản phẩm | US064 | PB14 | 4 | Xong | 5 | Nam Phong | |
| T067 | API admin products | CRUD product + gallery mặc định | US065 | PB14 | 4 | Xong | 8 | Nam Phong | |
| T068 | API variant/modifier/combo | CRUD variant, modifier, combo | US066 | PB14 | 4 | Xong | 8 | Nam Phong | |
| T069 | Product catalog frontend | ProductsPage KPI/filter/sort + ẩn sản phẩm | US065 | PB14 | 4 | Xong | 8 | Phúc Khang | |
| T070 | Product editor frontend | ProductEditorPage 5 section, dirty guard | US065,US103,US104 | PB14 | 4 | Xong | 14 | Phúc Khang | |
| T071 | API inventory stock | Cập nhật quantity_available | US067 | PB15 | 4 | Xong | 5 | Nam Phong | |
| T072 | API inventory ledger | GET /api/admin/inventory/transactions phân trang | US067,US105 | PB15 | 4 | Xong | 7 | Nam Phong | |
| T073 | Frontend inventory + ledger | InventoryPage + InventoryLedgerPage | US067,US105 | PB15 | 4 | Xong | 10 | Phúc Khang | |
| T074 | API admin orders | List/detail/status/cancel/notes | US068 | PB16 | 4 | Xong | 8 | Nam Phong | |
| T075 | Frontend admin orders | OrdersPage filter + OrderDetailPage | US068 | PB16 | 4 | Xong | 10 | Phúc Khang | |
| T076 | API refund | RefundService REFUNDED/REJECTED + paymentStatus | US069 | PB16 | 4 | Xong | 8 | Nam Phong | |
| T077 | Webhook không hồi sinh refund | Guard terminal refund trong processWebhook | US069 | PB16 | 4 | Xong | 5 | Nam Phong | |
| T078 | Frontend refunds | RefundsPage + dialog + tách khỏi OrdersPage | US069,US106 | PB16 | 4 | Xong | 10 | Phúc Khang | |
| T079 | Payment block admin | Expose provider/reference/attempt trong detail | US107 | PB16 | 4 | Xong | 4 | Nam Phong | |
| T080 | API admin coupon | CRUD coupon | US070 | PB17 | 4 | Xong | 6 | Nam Phong | |
| T081 | API admin banner | CRUD banner + active | US071 | PB17 | 5 | Xong | 4 | Nam Phong | |
| T082 | API admin shifts | CRUD ca + chống trùng giờ | US072 | PB17 | 5 | Xong | 6 | Nam Phong | |
| T083 | Frontend admin shifts | ShiftsPage lọc/tạo/sửa ca | US072 | PB17 | 5 | Xong | 8 | Phúc Khang | |
| T084 | API settings | /api/admin/settings + revalidate ADMIN | US075 | PB17 | 5 | Xong | 5 | Nam Phong | |
| T085 | Frontend settings | SettingsPage 6 tab, lưu theo nhóm | US075 | PB17 | 5 | Xong | 10 | Phúc Khang | |
| T086 | API báo cáo | getFullReport + net revenue/refund | US073 | PB18 | 5 | Xong | 8 | Nam Phong | |
| T087 | Frontend báo cáo | ReportsPage KPI/chart + export CSV | US073,US074 | PB18 | 5 | Xong | 12 | Phúc Khang | |
| T088 | Admin dashboard | DashboardPage + metrics | US120 | PB18 | 5 | Xong | 6 | Phúc Khang | |
| T089 | SEO head manager | meta robots/description/canonical/OG | US098 | PB18 | 5 | Xong | 6 | Phúc Khang | |
| T090 | Breadcrumbs chung | AppBreadcrumbs + meta.breadcrumb | US108 | PB18 | 5 | Xong | 4 | Phúc Khang | |
| T091 | Fix ADM007 gallery | Default gallery_images khi payload thiếu | US065 | PB18 | 5 | Mới | 3 | Nam Phong | Defect ADM007 |
| T092 | Account overview | OverviewPage đơn + điểm + shortcut | US092,US112 | PB19 | 5 | Xong | 6 | Phúc Khang | |
| T093 | Trợ giúp + điều khoản | HelpPage/TermsPage/PrivacyPage | US096,US097 | PB19 | 5 | Xong | 8 | Phúc Khang | |
| T094 | Order success nâng cấp | SuccessPage biên nhận + guest marker | US109,US110 | PB20 | 5 | Xong | 8 | Phúc Khang | |
| T095 | Reorder | Reorder từ order detail kiểm tra stock | US035 | PB20 | 5 | Xong | 5 | Phúc Khang | |
| T096 | Poll tracking | Poll 30s đơn đang hoạt động + guard | US111 | PB20 | 5 | Xong | 5 | Phúc Khang | |
| T097 | Delivery note + fees breakdown | Ghi chú giao + hiển thị serviceFee | US116,US118 | PB20 | 5 | Xong | 3 | Nam Phong | |
| T098 | Giỏ xóa món không khả dụng | Cảnh báo + xóa item lỗi | US119 | PB15 | 5 | Xong | 4 | Phúc Khang | |
| T099 | Giữ giỏ khi checkout lỗi | Không xóa giỏ khi thất bại | US117 | PB20 | 5 | Xong | 3 | Phúc Khang | |
| T100 | Backup + migration DB | Backup, migration 040-041 + validate | US120 | PB18 | 5 | Xong | 8 | Nam Phong | |
| T101 | GPS shipper + bản đồ | Vị trí shipper + theo dõi realtime | US079 | PB21 | 6 | Mới | 16 | Nam Phong | Cần GPS |
| T102 | Delivery slot | Chọn khung giờ giao khi checkout | US080 | PB21 | 6 | Mới | 8 | Nam Phong | Cần schema |
| T103 | Loyalty redemption | Đổi điểm + giữ số dư | US081 | PB21 | 6 | Mới | 8 | Nam Phong | Cần API |
| T104 | Public rating sản phẩm | Rating/review summary trên product | US082 | PB21 | 6 | Mới | 6 | Nam Phong | Cần API |
| T105 | Gợi ý món | Recommendation theo lịch sử | US091 | PB21 | 6 | Mới | 10 | Nam Phong | |
| T106 | SLA cảnh báo bếp | Đơn quá hạn theo SLA | US083 | PB22 | 6 | Mới | 6 | Nam Phong | Cần SLA |
| T107 | Delivery exception | Báo giao thất bại + trả đơn về cửa hàng | US084 | PB22 | 6 | Đang làm | 10 | Nam Phong | Backend 212 test + frontend 262 test và build/package pass; migration runtime + smoke thủ công chờ môi trường |
| T108 | COD settlement | Đối soát tiền thu/nộp | US085 | PB22 | 6 | Mới | 8 | Nam Phong | Cần API |
| T109 | Ticket conversation | Trả lời theo chuỗi trong ticket | US086 | PB22 | 6 | Mới | 8 | Nam Phong | Cần schema |
| T110 | Per-recipient notification | Read state riêng từng user | US087 | PB23 | 6 | Mới | 8 | Nam Phong | Cần schema |
| T111 | Biên nhận PDF | Xuất hóa đơn PDF | US088 | PB23 | 6 | Mới | 6 | Phúc Khang | |
| T112 | Combo theo nhóm | Combo 1/2 người/gia đình | US089 | PB23 | 6 | Mới | 5 | Phúc Khang | |
| T113 | Cảnh báo rời editor chưa lưu | Dirty guard + beforeunload | US104 | PB14 | 6 | Xong | 5 | Phúc Khang | |
| T114 | Ledger phân trang | Page/size + clamp + KPI | US105 | PB24 | 6 | Xong | 6 | Phúc Khang | |
| T115 | Dispatch workload sort | Sort theo activeOrderCount | US051 | PB24 | 6 | Xong | 4 | Phúc Khang | |
| T116 | Shipper history date filter | Lọc ngày client-side | US113 | PB24 | 6 | Xong | 4 | Phúc Khang | |
| T117 | Checkout saved address | Chọn địa chỉ đã lưu | US114 | PB24 | 6 | Xong | 5 | Phúc Khang | |
| T118 | Phí GHN theo khu vực | Tính phí khi chọn district/ward | US115 | PB24 | 6 | Xong | 6 | Nam Phong | |
| T119 | Shipper layout mobile | Bottom nav + shift gating | US101 | PB23 | 6 | Xong | 6 | Phúc Khang | |
| T120 | Docs sơ đồ dự án | usecase/erd/sitemap/backlogs | US120 | PB18 | 6 | Xong | 8 | Nam Phong | |
