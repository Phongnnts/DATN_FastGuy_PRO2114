# FastGuy 30-Day Realistic Operations Seed Design

## Objective

Tạo một bộ seed hợp nhất mô phỏng cửa hàng FastGuy đang vận hành thật trong 30 ngày, phục vụ demo và bảo vệ đồ án. Dữ liệu phải tự nhiên, đầy đủ dấu tiếng Việt, nhất quán giữa khách hàng, STAFF, ADMIN, SHIPPER, bán hàng, thanh toán, kho, nhân sự và báo cáo.

## Safety and ownership

- Chỉ chạy trên đúng `DemoDatabase`; không tạo wrapper hoặc thực thi trên `FastGuyDB`.
- Yêu cầu session-context opt-in, `XACT_ABORT ON`, transaction và rollback toàn bộ khi lỗi.
- Dùng prefix kỹ thuật `FG-OPS-*` cho khóa ownership không hiển thị; trường hiển thị dùng nội dung tự nhiên, không chứa `demo`, `mẫu`, `trình diễn`.
- Cleanup nhận diện chính xác dữ liệu seed cũ và mới, xóa theo thứ tự FK, không chạm dữ liệu ngoài ownership.
- Chạy hai lần cho cùng kết quả, không tạo trùng.

## Scale

- 2 ADMIN, 6 STAFF, 4 SHIPPER, 20 khách hàng.
- 7 danh mục; 40 sản phẩm phân bổ `6-6-6-6-6-5-5`.
- Mỗi sản phẩm có 2–3 kích cỡ phù hợp món; mỗi biến thể có SKU, giá, trạng thái, kích thước vận chuyển và chế độ tồn kho đầy đủ.
- Modifier/topping chỉ gắn với món phù hợp, có min/max, giá và trạng thái hợp lệ.
- Công thức theo từng biến thể, định lượng tăng hợp lý theo kích cỡ.
- Khoảng 180 đơn hàng trong 30 ngày.

## Natural data

Tên người, số điện thoại, địa chỉ TP.HCM, tên món, ghi chú giao hàng, đánh giá, lý do hủy/hoàn/giao thất bại, ghi chú ca làm và kho phải đa dạng, tự nhiên, đầy đủ dấu. Không dùng chuỗi đánh số như `Khách hàng 1` trong trường hiển thị. Email dùng miền an toàn dành cho dữ liệu giả và không trùng.

## Customer data

Mỗi khách có hồ sơ đầy đủ; phần lớn có 1–2 địa chỉ hợp lệ với GHN province/district/ward fields. Phân bổ hợp lý giỏ hàng đang mở, yêu thích, coupon đã nhận/đã dùng, loyalty earn/redeem và lịch sử đơn. Không tạo dữ liệu nhạy cảm thật.

## Catalog and inventory

Giữ 40 món tự nhiên trong bảy danh mục. Mỗi món có 2–3 kích cỡ và công thức riêng. Nguyên liệu dùng đơn vị schema `G`, `ML`, `PIECE`, giá vốn và tồn kho hợp lý. Có nhiều phiếu nhập trong 30 ngày, giao dịch receipt/consumption/waste/adjustment, kiểm kê theo tần suất và bằng chứng chênh lệch hợp lệ.

## Orders and payments

Khoảng 180 đơn phân bố theo ngày/giờ mở cửa, khách hàng, món và giá trị khác nhau. Bao phủ các trạng thái hợp lệ từ PENDING đến DELIVERED, CANCELLED, DELIVERY_FAILED và RETURNED_TO_STORE; timeline phải tăng dần và phù hợp trạng thái. Có COD và BANK_TRANSFER/PayOS, payment attempts, giảm giá, hoàn tiền, reservation/cost snapshots và lịch sử trạng thái. Đơn đang giao gắn với shipper/ca phù hợp; đơn bếp gắn STAFF theo quy tắc hiện tại.

## Reviews and engagement

Chỉ đơn đã giao mới có đánh giá. Đánh giá phủ 1–5 sao nhưng tập trung 4–5 sao, nội dung tự nhiên theo món/giao hàng/đóng gói. Một số đánh giá có consent và featured để trang chủ có dữ liệu. Favorites, cart và modifiers tham chiếu đúng sản phẩm/biến thể còn hoạt động.

## Workforce and operations

Tạo lịch 30 ngày cho 6 STAFF và 4 SHIPPER với nhiều người mỗi ca, trạng thái scheduled/checked-in/checked-out phù hợp thời gian. Có check-in/out source, attendance status, approved minutes/overtime, pay-rate history và pay snapshots. COD settlement phủ submitted/settled/short/over với expected/submitted/verified amounts nhất quán.

## Finance, merchandising and audit

Có operating expenses theo nhóm, fixed assets với trạng thái/lifecycle hợp lệ, coupon, banner, category merchandising và store-facing content. ActivityLog chỉ ghi các hành động thực sự được mô phỏng bởi seed, metadata hợp lệ, không tạo log giả không có đối tượng liên quan.

## Validator

Validator kiểm tra target, ownership, counts/ranges, uniqueness, FK, trường bắt buộc, tiếng Việt tự nhiên, category distribution, variant/recipe coverage, recipe scaling, order arithmetic, payment/refund consistency, timeline, shift ownership, attendance/pay snapshots, COD reconciliation, inventory transaction math, receipt/count coverage, review eligibility, coupon/loyalty integrity, finance/assets and activity-log references. Validator xuất bảng tóm tắt đủ để dùng làm bằng chứng demo.

## Verification

- Source-policy tests phải fail trước khi seed mới được viết.
- Chạy focused tests và `mvn test`.
- Xác nhận `DuckJo/DemoDatabase`, ONLINE, compatibility 160 và migration history trước mọi write.
- Chạy seed + validator hai lần trên `DemoDatabase`.
- Không ghi `FastGuyDB`.
- Không sửa hoặc ghi đè thay đổi frontend chưa commit.
