# Thiết kế cửa hàng đơn vận hành thật và nâng cấp UI/UX

## 1. Mục tiêu

FastGuy vận hành an toàn như cửa hàng đồ ăn nhanh một chi nhánh, ưu tiên luồng đơn hàng, tiền và tồn kho. Mỗi lát triển khai đồng thời hoàn thiện nghiệp vụ, API, UI và kiểm thử.

Mục tiêu trải nghiệm:

- Khách đặt món nhanh, hiểu rõ phí, ưu đãi và trạng thái đơn.
- Shipper xử lý giao hàng và bàn giao COD trên thiết bị di động.
- Staff nhìn thấy việc cần xử lý ngay và phục hồi đơn giao thất bại.
- Admin kiểm soát tiền, hoàn tiền, tồn kho và audit.

Không tạo vai trò Manager riêng trong giai đoạn này. Admin giữ chức năng quản lý cửa hàng.

## 2. Phạm vi

### 2.1 Giao hàng thất bại

- Shipper báo giao thất bại bằng một reason code hợp lệ và ghi chú bắt buộc.
- Staff có thể giao lại ngay, hẹn giao lại, đổi Shipper hoặc trả đơn về cửa hàng.
- Mặc định mỗi đơn có lần giao đầu và một lần giao lại.
- Admin có thể tăng giới hạn lượt giao; lý do bắt buộc và phải ghi audit.
- Giao thất bại không thay đổi payment, coupon, loyalty hoặc inventory.
- Chỉ `RETURNED_TO_STORE` mới waste inventory, release coupon và tạo refund cần thiết.
- Khách chỉ thấy thông tin an toàn: trạng thái, hướng xử lý và lịch giao lại; không thấy ghi chú nội bộ hoặc chi tiết sự cố nhạy cảm.

Thiết kế chi tiết của capability này tiếp tục tuân theo `docs/superpowers/specs/2026-08-12-delivery-failure-recovery-design.md`.

### 2.2 Đối soát COD

Đối soát theo ca làm:

1. Hệ thống tổng hợp các đơn COD `DELIVERED` thuộc Shipper và ca hiện tại.
2. Shipper xem số tiền dự kiến, xác nhận số tiền thực nộp và gửi bàn giao.
3. Bản bàn giao chuyển sang `SUBMITTED` và không được chỉnh âm thầm.
4. Admin kiểm đếm và xác nhận `SETTLED`, `SHORT` hoặc `OVER`.
5. `SHORT` và `OVER` bắt buộc lý do.
6. Lưu Shipper, Admin nhận, ca, expected amount, submitted amount, verified amount và timestamps.
7. Request lặp không tạo nhiều settlement cho cùng Shipper và ca.
8. Revenue không phụ thuộc trạng thái settlement; báo cáo phải tách doanh thu và COD chưa nộp.

### 2.3 Hoàn tiền

- Đơn online đã thanh toán và đủ điều kiện hoàn tạo refund `PENDING`.
- `PENDING` nghĩa là tiền chưa được xác nhận đã hoàn.
- Admin chỉ đánh dấu `REFUNDED` sau khi hoàn ngoài hệ thống và nhập reference.
- Từ chối bắt buộc lý do.
- UI hiển thị rõ phương thức, số tiền, thời gian chờ, người xử lý, reference và thời gian hoàn.
- Không thêm partial refund hoặc PayOS refund API trong đợt này.
- Mutation giữ pessimistic lock, terminal-state protection và idempotency hiện có.

### 2.4 Cảnh báo tồn kho

- Dùng một `lowStockThreshold` cấu hình chung thay cho ngưỡng hardcode khác nhau.
- Admin Dashboard hiển thị số SKU hết hàng và sắp hết.
- Staff nhận cảnh báo khi sản phẩm trong hàng đợi có nguy cơ thiếu.
- Cảnh báo chỉ hỗ trợ quyết định; không tự nhập hàng, tự điều chỉnh tồn hoặc tự khóa bán.
- Không thêm nguyên liệu, BOM, nhà cung cấp, purchase order hoặc nhiều kho.

### 2.5 Quick wins cho khách hàng

- Checkout có ô nhập coupon thủ công cho User và Guest.
- Trang hồ sơ tải dữ liệu thật từ profile API.
- Lỗi đồng bộ guest cart sau đăng nhập phải được thông báo, không bị nuốt.
- Tracking hiển thị ETA khi backend có dữ liệu.
- Đặt lại đơn giữ variant và modifier còn hợp lệ; item không còn hợp lệ được báo riêng.
- Claim freeship và thời gian giao chỉ hiển thị khi khớp cấu hình hoặc dữ liệu thật.

## 3. Trải nghiệm theo vai trò

### 3.1 Khách hàng

- Storefront dùng màu cam ấm, ảnh món nổi bật, CTA chính rõ ràng.
- Checkout giữ tổng tiền và CTA ở vị trí dễ thấy; phí, coupon và trạng thái thanh toán minh bạch.
- Order tracking ưu tiên trạng thái hiện tại, ETA và bước tiếp theo.
- Giao thất bại hiển thị lịch giao lại hoặc thông báo đơn đang được cửa hàng xử lý.
- Refund hiển thị tiến trình, không dùng từ ngữ khiến khách hiểu tiền đã về khi mới `PENDING`.

### 3.2 Shipper

- Giao diện mobile-first, hit area tối thiểu 44x44 px.
- Báo giao thất bại là luồng xác nhận hai bước để tránh thao tác nhầm.
- Trang COD tách số dự kiến, số đã gửi bàn giao và kết quả Admin xác nhận.
- Không dùng tổng COD hôm nay như đồng nghĩa với tiền đã đối soát.

### 3.3 Staff

- Dashboard có vùng “Cần xử lý ngay” cho đơn quá SLA, giao thất bại và rủi ro tồn.
- Recovery queue cho phép retry, reschedule, reassign hoặc return-to-store theo policy.
- Conflict hiển thị trạng thái mới nhất và yêu cầu reload trước khi thao tác tiếp.

### 3.4 Admin

- Dashboard mật độ cao nhưng có thứ bậc rõ: đơn cần xử lý, tiền cần xác nhận, tồn cần chú ý.
- COD settlement có hàng đợi chờ xác nhận và lịch sử thiếu/thừa.
- Refund queue phân biệt chờ xử lý và đã xử lý.
- Các hành động nhạy cảm dùng dialog chuẩn, yêu cầu reason phù hợp và không dùng browser `confirm()`.

## 4. Design system

- Storefront: cam ấm, bề mặt sáng, ảnh sản phẩm lớn, khoảng thở vừa, CTA tương phản cao.
- Vận hành: nền trung tính, mật độ thông tin cao, badge trạng thái nhất quán, màu cảnh báo không là tín hiệu duy nhất.
- Dùng cùng typography, spacing, radius, form control, dialog và status vocabulary cho toàn hệ thống.
- Desktop dùng bảng khi cần so sánh nhiều trường; mobile chuyển card thay vì ép cuộn ngang dài.
- Mọi màn có loading, empty, error, retry và conflict state.
- Modal có focus trap, trả focus, Escape và accessible name.
- Input có label; lỗi dùng mô tả liên kết; tương phản đạt WCAG 2.2 AA.
- Hỗ trợ `prefers-reduced-motion`; animation chỉ làm rõ trạng thái, không cản thao tác.

## 5. Kiến trúc và dữ liệu

- Giữ Vue 3, Pinia, servlet/service/DAO/entity và SQL migration hiện có.
- Không thêm dependency khi native platform hoặc utility hiện có đáp ứng.
- Mỗi capability là một lát dọc độc lập nhưng dùng chung policy trạng thái và audit conventions.
- Mutation nghiệp vụ chạy trong transaction.
- Dữ liệu cạnh tranh dùng `expectedStatus` hoặc expected value và lock phù hợp.
- Audit lưu actor, role, action, before/after hoặc from/to, reason/note và timestamp.
- Ownership, role, shift và active-account validation thực hiện ở trust boundary backend.

## 6. API và lỗi

- `400`: payload, reason, amount hoặc schedule không hợp lệ.
- `401`: chưa xác thực.
- `403`: sai role, ownership, active status hoặc shift.
- `404`: resource không tồn tại trong phạm vi được phép xem.
- `409`: expected state/value cũ, settlement trùng hoặc terminal state đã được xử lý.
- `500`: lỗi bất ngờ; transaction rollback và không lộ thông tin nội bộ.

Frontend map lỗi sang thông điệp hành động được: sửa dữ liệu, đăng nhập lại, reload trạng thái hoặc liên hệ cửa hàng. Không nuốt lỗi mutation.

## 7. Thứ tự triển khai

1. Hoàn tất delivery failure recovery đang triển khai.
2. Xây COD settlement theo ca cùng UI Shipper/Admin.
3. Làm rõ refund UI và metadata audit; giữ manual provider workflow.
4. Thống nhất low-stock threshold và cảnh báo Dashboard/Staff.
5. Sửa quick wins Customer.
6. Chuẩn hóa visual tokens, dialogs, responsive states và accessibility trên các màn đã chạm.

Mỗi bước phải hoàn tất backend, migration, frontend và kiểm thử trước khi sang bước sau.

## 8. Kiểm thử và tiêu chí chấp nhận

### Backend

- Unit tests cho policy reason, attempt, schedule, settlement amount và terminal state.
- Service tests cho ownership, role, shift, locking, idempotency, audit và rollback.
- API tests cho `400`, `403`, `404`, `409` và happy path.
- Migration validator kiểm tra constraint, default, index và retained-data parity.

### Frontend

- Component/source tests cho loading, empty, error, conflict và success.
- Kiểm thử keyboard, focus trap, accessible names và status announcements.
- E2E tối thiểu cho checkout coupon, delivery failure recovery, COD handover và refund visibility.
- Responsive verification ở mobile, tablet và desktop cho các màn thay đổi.

### Hoàn tất

- Không còn màn COD gọi tổng thu hôm nay là tiền đã đối soát.
- Giao thất bại không làm sai inventory, coupon, loyalty, payment hoặc revenue.
- Refund `PENDING` không được trình bày như đã hoàn.
- Một cấu hình low-stock được dùng nhất quán.
- Quick wins Customer hoạt động cho cả Guest và User đúng phạm vi.
- Test, lint, typecheck/build của project đều pass.

## 9. Ngoài phạm vi

- Vai trò Manager hoặc RBAC chi tiết.
- Nhiều chi nhánh, nhiều kho.
- Inventory nguyên liệu, BOM, supplier và purchase order.
- GPS realtime.
- Recommendation engine.
- Partial refund và PayOS automatic refund.
- Loyalty redemption.
- Delivery slots tổng quát ngoài lịch giao lại của đơn thất bại.
- Thiết kế lại toàn bộ website trong một lần.
