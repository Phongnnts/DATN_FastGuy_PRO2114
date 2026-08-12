# Product Backlog — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx`.

| ID | STORY / FEATURE / REQUEST (As a) | I want to (Goal) | So that (Reason) | Priority | Business Value | Acceptance Criteria | State | Note |
| -- | -------------------------------- | ---------------- | ---------------- | -------- | -------------- | ------------------- | ----- | ---- |
| US001 | Người dùng | Tôi muốn đăng ký tài khoản | Để vào hệ thống mua sắm | 1.0 | Cao | Nhập email, mật khẩu (8–72 ký tự, có chữ và số), xác nhận mật khẩu; hệ thống kiểm tra email/điện thoại chưa tồn tại; đăng ký xong lưu tài khoản và thông báo thành công | Xong | |
| US002 | Người dùng | Tôi muốn đăng nhập bằng email hoặc số điện thoại | Để truy cập tài khoản | 1.0 | Cao | Nhập email/điện thoại + mật khẩu; kiểm tra tài khoản ACTIVE; đăng nhập xong chuyển về trang theo vai trò, trả về JWT | Xong | |
| US003 | Người dùng | Tôi muốn đăng xuất | Để thoát phiên an toàn | 1.0 | Vừa | Xóa phiên khỏi trình duyệt và quay về trang đăng nhập | Xong | |
| US004 | Người dùng | Tôi muốn đổi mật khẩu | Để bảo mật tài khoản | 2.0 | Vừa | Nhập mật khẩu hiện tại, mật khẩu mới, xác nhận; kiểm tra mật khẩu hiện tại đúng và mật khẩu mới đủ mạnh; cập nhật và báo thành công | Xong | |
| US005 | Người dùng | Tôi muốn đặt lại mật khẩu khi quên | Để lấy lại quyền truy cập | 2.0 | Cao | Nhập email; hệ thống gửi link đặt lại mật khẩu nếu email tồn tại (trả lời trùng lặp chống lộ email); token hết hạn và dùng một lần | Xong | |
| US006 | Người dùng | Tôi muốn xem thông tin cá nhân | Để kiểm tra hồ sơ | 2.0 | Thấp | GET /api/auth/me trả về họ tên, email, điện thoại, avatar, vai trò | Xong | |
| US007 | Người dùng | Tôi muốn cập nhật hồ sơ | Để thông tin luôn chính xác | 2.0 | Vừa | Sửa họ tên, email, điện thoại; kiểm tra trùng email/điện thoại và định dạng | Xong | |
| US008 | Người dùng | Tôi muốn tài khoản bị khóa khi nhập sai mật khẩu nhiều lần | Để chống tấn công brute-force | 4.0 | Cao | Sau 5 lần nhập sai tạm khóa tài khoản một khoảng thời gian; thông báo rõ ràng | Xong | Khóa 15 phút ngay lần sai thứ 5; regression ST043 pass |
| US009 | Người dùng | Tôi muốn đăng nhập không phân biệt chữ hoa/thường cho email | Để không nhập sai do viết hoa | 3.0 | Vừa | Email được chuẩn hóa chữ thường khi xác thực | Xong | |
| US010 | Người dùng | Tôi muốn tự động cắt khoảng trắng khi đăng nhập | Để tránh lỗi do copy-paste | 3.0 | Thấp | Trim đầu/cuối username/email và mật khẩu không bị trim | Xong | |
| US011 | Người dùng | Tôi muốn quản lý địa chỉ giao hàng | Để đặt hàng nhanh hơn | 2.0 | Cao | Thêm, sửa, xóa, đặt mặc định; duy nhất 1 địa chỉ mặc định; chọn tỉnh/quận/phường GHN | Xong | |
| US012 | Người dùng | Tôi muốn lưu nhiều địa chỉ | Để giao đến nhà/cơ quan | 2.0 | Vừa | Danh sách địa chỉ gắn user; đánh dấu mặc định | Xong | |
| US013 | Người dùng | Tôi muốn xem danh sách sản phẩm theo danh mục | Để dễ tìm món | 1.0 | Cao | GET /api/products lọc theo categoryId, hỗ trợ sắp xếp và phân trang | Xong | |
| US014 | Người dùng | Tôi muốn tìm kiếm món theo tên | Để tìm món nhanh | 1.0 | Cao | Tìm không phân biệt hoa thường; trả về danh sách rỗng khi không có kết quả | Xong | |
| US015 | Người dùng | Tôi muốn lọc sản phẩm theo giá, loại, khuyến mãi, còn hàng | Để chọn đúng nhu cầu | 2.0 | Vừa | Bộ lọc minPrice/maxPrice, productType, discounted, availability kết hợp | Xong | |
| US016 | Người dùng | Tôi muốn xem chi tiết sản phẩm | Để biết giá và tùy chọn | 1.0 | Cao | Gallery, base price, variant, modifier group/option, combo contents, liên quan | Xong | |
| US017 | Người dùng | Tôi muốn xem sản phẩm nổi bật và bán chạy | Để tham khảo gợi ý | 2.0 | Vừa | Endpoint featured, best-sellers trả danh sách có thứ tự | Xong | |
| US018 | Người dùng | Tôi muốn xem khung giờ khả dụng của món | Để không đặt ngoài giờ phục vụ | 3.0 | Thấp | Sản phẩm có available_from/to; hiển thị trạng thái phục vụ | Xong | |
| US019 | Khách hàng | Tôi muốn thêm món với biến thể và tùy chọn vào giỏ | Để chuẩn bị đặt hàng | 1.0 | Cao | Server kiểm tra product/variant/modifier hợp lệ, số lượng > 0, không vượt tồn kho | Xong | |
| US020 | Khách hàng | Tôi muốn sửa số lượng trong giỏ | Để điều chỉnh đơn | 1.0 | Cao | Cập nhật số lượng; đặt 0 tự xóa khỏi giỏ; không vượt tồn kho | Xong | |
| US021 | Khách hàng | Tôi muốn xóa sản phẩm khỏi giỏ | Để bỏ món không cần | 1.0 | Vừa | DELETE giỏ hàng theo cartItemId | Xong | |
| US022 | Khách hàng | Tôi muốn xem giỏ hàng với tổng tiền | Để biết cần thanh toán bao nhiêu | 1.0 | Cao | Trả danh sách item kèm modifiers, đơn giá, tổng tạm tính | Xong | |
| US023 | Khách hàng | Tôi muốn giỏ được lưu theo phiên khi chưa đăng nhập | Để không mất khi tải lại | 2.0 | Vừa | Giỏ theo sessionId; hợp nhất giỏ vào tài khoản sau đăng nhập | Xong | |
| US024 | Khách hàng | Tôi muốn đặt hàng thanh toán khi nhận hàng (COD) | Để mua không cần tài khoản | 1.0 | Cao | Guest checkout nhập họ tên, điện thoại, địa chỉ GHN; tạo đơn PENDING | Xong | |
| US025 | Khách hàng | Tôi muốn đặt hàng chuyển khoản PayOS | Để thanh toán online | 1.0 | Cao | Chọn BANK_TRANSFER khi PayOS được cấu hình; nhận checkout URL; webhook xác thực chữ ký | Xong | |
| US026 | Khách hàng | Tôi muốn đặt hàng bằng tài khoản | Để lưu lịch sử mua hàng | 1.0 | Cao | Checkout dùng địa chỉ đã lưu; đơn gắn user_id; lưu lịch sử | Xong | |
| US027 | Khách hàng | Tôi muốn chống đặt hàng trùng do bấm hai lần | Để không tạo đơn kép | 1.0 | Cao | Idempotency-Key + request hash; replay trả đơn cũ | Xong | |
| US028 | Khách hàng | Tôi muốn áp dụng mã giảm giá khi thanh toán | Để được giảm giá | 1.0 | Cao | Verify coupon PERCENT/FIXED/FREE_SHIPPING; giới hạn min_order, max_discount, expiry, số lần dùng | Xong | |
| US029 | Khách hàng | Tôi muốn kiểm tra giỏ hàng không thay đổi khi thanh toán | Để tránh sai giá | 1.0 | Cao | Cart signature; nếu giỏ thay đổi → báo lỗi yêu cầu thử lại | Xong | |
| US030 | Khách hàng | Tôi muốn nhận mã đơn sau khi đặt | Để tra cứu tiến trình | 1.0 | Cao | Tạo đơn trả orderCode; trang order-success hiển thị mã và biên nhận | Xong | |
| US031 | Khách hàng | Tôi muốn tra cứu đơn bằng mã và 4 số cuối điện thoại | Để theo dõi không cần đăng nhập | 1.0 | Cao | GET /api/orders/track với code + phoneSuffix (4 số); trả trạng thái, ETA, items | Xong | |
| US032 | Khách hàng | Tôi muốn xem lịch sử đơn hàng | Để biết đã mua gì | 1.0 | Cao | GET /api/orders trả danh sách đơn của user, sắp mới nhất | Xong | |
| US033 | Khách hàng | Tôi muốn xem chi tiết đơn hàng | Để xem trạng thái và thanh toán | 1.0 | Cao | GET /api/orders/{id} chỉ đơn của chủ sở hữu; trả items, timeline, refund | Xong | |
| US034 | Khách hàng | Tôi muốn hủy đơn khi còn ở trạng thái cho phép | Để thay đổi quyết định | 1.0 | Cao | User chỉ hủy PENDING; hủy sau PREPARING ghi tồn kho WASTE | Xong | |
| US035 | Khách hàng | Tôi muốn đặt lại món từ đơn cũ | Để đặt lại nhanh | 3.0 | Vừa | Reorder kiểm tra sản phẩm còn bán và đủ tồn kho; thêm lại vào giỏ | Xong | |
| US036 | Khách hàng | Tôi muốn đánh giá món sau khi nhận hàng | Để chia sẻ trải nghiệm | 2.0 | Vừa | Đánh giá 1–5 kèm bình luận; chỉ đơn DELIVERED; mỗi user mỗi đơn một đánh giá | Xong | |
| US037 | Khách hàng | Tôi muốn quản lý món yêu thích | Để lưu món ưa thích | 2.0 | Vừa | Thêm/xóa yêu thích theo productId; danh sách yêu thích cá nhân | Xong | |
| US038 | Khách hàng | Tôi muốn xem điểm thưởng và hạng thành viên | Để biết quyền lợi | 2.0 | Vừa | GET /api/loyalty/me trả điểm, hạng Bronze/Silver/Gold, lịch sử giao dịch | Xong | |
| US039 | Khách hàng | Tôi muốn nhận điểm khi đơn giao thành công | Để được ưu đãi | 2.0 | Cao | Tích điểm khi DELIVERED + PAID; đảo điểm khi hoàn tiền | Xong | |
| US040 | Khách hàng | Tôi muốn nhận và xem mã giảm giá | Để tiết kiệm chi phí | 2.0 | Vừa | Claim coupon công khai; ví coupon; giới hạn mỗi user một lần nhận | Xong | |
| US041 | Khách hàng | Tôi muốn xem thông báo và đánh dấu đã đọc | Để cập nhật đơn hàng | 2.0 | Vừa | GET /api/notifications; mark-read một/đọc tất cả; đếm chưa đọc | Xong | |
| US042 | Khách hàng | Tôi muốn gửi yêu cầu hỗ trợ | Để được giải quyết vấn đề | 2.0 | Vừa | Tạo ticket gắn đơn tùy chọn, chọn category; theo dõi trạng thái OPEN/PROCESSING/RESOLVED | Xong | |
| US043 | Khách hàng | Tôi muốn trả về đúng trạng thái khi thanh toán bị hủy | Để biết phải làm gì tiếp | 2.0 | Cao | Payment return: PAID→thành công, CANCELLED→hướng dẫn, PENDING→chờ xử lý | Xong | |
| US044 | Nhân viên | Tôi muốn xem ca làm việc và check-in/check-out | Để bắt đầu ca | 1.0 | Cao | GET /api/shifts/current; check-in trong cửa sổ start−15′ đến end+15′; check-out từ end | Xong | |
| US045 | Nhân viên | Tôi muốn xem danh sách đơn cần xử lý | Để ưu tiên công việc | 1.0 | Cao | Queue PENDING/CONFIRMED/PREPARING/READY sắp cũ nhất trước, hiển thị thời gian chờ | Xong | |
| US046 | Nhân viên | Tôi muốn xem chi tiết đơn với tùy chọn món | Để chế biến đúng | 1.0 | Cao | Detail hiển thị items, modifiers, khách, địa chỉ, thanh toán, ghi chú | Xong | |
| US047 | Nhân viên | Tôi muốn xác nhận đơn | Để bắt đầu xử lý | 1.0 | Cao | PENDING→CONFIRMED; đơn BANK_TRANSFER phải PAID trước | Xong | |
| US048 | Nhân viên | Tôi muốn chuyển đơn sang chế biến và sẵn sàng | Để báo bếp | 1.0 | Cao | CONFIRMED→PREPARING (tiêu thụ tồn kho)→READY | Xong | |
| US049 | Nhân viên | Tôi muốn hủy đơn khi cần | Để xử lý sai sót | 1.0 | Vừa | Hủy theo transition policy; ghi failureReason; hủy sau PREPARING ghi WASTE | Xong | |
| US050 | Nhân viên | Tôi muốn phân công Shipper cho đơn sẵn sàng | Để giao hàng | 1.0 | Cao | READY→ASSIGNED; Shipper phải ACTIVE + đang CHECKED_IN; gửi thông báo | Xong | |
| US051 | Nhân viên | Tôi muốn xem board điều phối với tải công việc Shipper | Để giao việc hợp lý | 2.0 | Cao | GET /api/staff/orders/shippers trả activeOrderCount; gán từ board | Xong | |
| US052 | Nhân viên | Tôi muốn thêm ghi chú nội bộ đơn | Để trao đổi với bếp | 2.0 | Vừa | POST notes nối vào internal_note; hiển thị trong detail | Xong | |
| US053 | Nhân viên | Tôi muốn xem lịch sử đơn đã xử lý | Để kiểm tra lại | 2.0 | Vừa | GET /api/staff/orders/history không yêu cầu check-in | Xong | |
| US054 | Nhân viên | Tôi muốn xử lý yêu cầu hỗ trợ | Để chăm sóc khách | 2.0 | Vừa | Danh sách ticket; cập nhật trạng thái và resolution | Xong | |
| US055 | Shipper | Tôi muốn xem ca làm việc và check-in | Để bắt đầu giao | 1.0 | Cao | Ca của Shipper; check-in/check-out; trang /shipper/shifts | Xong | |
| US056 | Shipper | Tôi muốn xem đơn được phân công | Để nhận việc | 1.0 | Cao | GET /api/shipper/orders/mine và /active chỉ đơn của mình | Xong | |
| US057 | Shipper | Tôi muốn xem chi tiết đơn giao | Để giao đúng | 1.0 | Cao | Địa chỉ, điện thoại, items, tổng tiền, tiền COD; xem được đơn đã giao/hủy ngoài ca | Xong | |
| US058 | Shipper | Tôi muốn gọi khách và mở Google Maps | Để giao hiệu quả | 2.0 | Vừa | Deep-link tel: và Google Maps theo địa chỉ | Xong | |
| US059 | Shipper | Tôi muốn xác nhận nhận hàng | Để bắt đầu giao | 1.0 | Cao | ASSIGNED→PICKED_UP | Xong | |
| US060 | Shipper | Tôi muốn xác nhận giao thành công | Để hoàn tất đơn | 1.0 | Cao | PICKED_UP→DELIVERED; COD thu đúng finalAmount; online phải PAID | Xong | |
| US061 | Shipper | Tôi muốn xem tổng quan ca giao | Để biết khối lượng | 2.0 | Vừa | Dashboard: active, đã nhận/giao hôm nay, đơn tiếp theo | Xong | |
| US062 | Shipper | Tôi muốn xem lịch sử giao và lọc theo ngày | Để đối chiếu | 2.0 | Vừa | History DELIVERED + CANCELLED, lọc ngày, search | Xong | |
| US063 | Quản trị viên | Tôi muốn quản lý người dùng | Để kiểm soát tài khoản | 1.0 | Cao | CRUD user; đổi vai trò/trạng thái; xem đơn của user | Xong | |
| US064 | Quản trị viên | Tôi muốn quản lý danh mục | Để tổ chức thực đơn | 1.0 | Cao | CRUD category; chặn xóa danh mục đang chứa sản phẩm | Xong | |
| US065 | Quản trị viên | Tôi muốn quản lý sản phẩm | Để cập nhật thực đơn | 1.0 | Cao | CRUD product; gallery mặc định khi thiếu; giá >= 0 | Xong | |
| US066 | Quản trị viên | Tôi muốn quản lý biến thể và tùy chọn | Để linh hoạt món | 1.0 | Cao | Variant (giá, tồn kho, default, SKU), modifier group/option, combo | Xong | |
| US067 | Quản trị viên | Tôi muốn quản lý tồn kho | Để không hết hàng | 1.0 | Cao | Cập nhật quantity_available; xem sổ giao dịch tồn kho (ledger) | Xong | |
| US068 | Quản trị viên | Tôi muốn xem và xử lý đơn hàng | Để giám sát | 1.0 | Cao | List/detail đơn; hủy; thêm ghi chú; xem timeline | Xong | |
| US069 | Quản trị viên | Tôi muốn xử lý hoàn tiền | Để bồi hoàn khách | 1.0 | Cao | Queue refund; REFUNDED (amount>0) đổi paymentStatus=REFUNDED; REJECTED kèm note; không để webhook hồi sinh | Xong | |
| US070 | Quản trị viên | Tôi muốn quản lý mã giảm giá | Để chạy khuyến mãi | 1.0 | Cao | CRUD coupon; điều kiện type/value/min_order/max_discount/expiry | Xong | |
| US071 | Quản trị viên | Tôi muốn quản lý banner | Để quảng bá | 2.0 | Vừa | CRUD banner; active/sort | Xong | |
| US072 | Quản trị viên | Tôi muốn phân ca làm việc | Để sắp lịch nhân sự | 1.0 | Cao | Tạo/sửa/xóa ca Staff/Shipper; chống trùng giờ | Xong | |
| US073 | Quản trị viên | Tôi muốn xem báo cáo doanh thu | Để đánh giá kinh doanh | 1.0 | Cao | Full report: periodRevenue, netRevenue (trừ refund), top products, category, payment stats | Xong | |
| US074 | Quản trị viên | Tôi muốn xuất báo cáo ra CSV | Để lưu trữ | 2.0 | Vừa | Export CSV UTF-8 BOM; chống Excel formula injection | Xong | |
| US075 | Quản trị viên | Tôi muốn cấu hình cửa hàng | Để vận hành linh hoạt | 1.0 | Vừa | Settings: store info, giờ mở cửa, phí/thuế, giao hàng; xem trạng thái PayOS/GHN | Xong | |
| US076 | Quản trị viên | Tôi muốn điều chỉnh tồn kho thủ công | Để sửa sai lệch kiểm kê | 3.0 | Cao | Endpoint adjustment ghi ledger kèm actor/reason; chặn stock không quản lý | Xong | Endpoint + ledger audit; xử lý conflict; backend 176 test, frontend 247 test |
| US077 | Quản trị viên | Tôi muốn ghi nhận lãng phí thủ công | Để quản lý hao hụt | 3.0 | Vừa | Waste entry ghi ledger, lý do, người tạo | Mới | Cần schema |
| US078 | Quản trị viên | Tôi muốn xem audit log thao tác | Để truy vết | 4.0 | Vừa | Ghi log ai làm gì, khi nào, resource nào | Mới | Cần schema |
| US079 | Khách hàng | Tôi muốn theo dõi vị trí Shipper thời gian thực | Để biết đơn đến đâu | 4.0 | Cao | GPS Shipper + bản đồ; ước tính ETA | Mới | Cần GPS |
| US080 | Khách hàng | Tôi muốn đặt hàng theo khung giờ giao | Để chủ động thời gian | 3.0 | Vừa | Chọn delivery slot khi checkout | Mới | Cần schema |
| US081 | Khách hàng | Tôi muốn đổi điểm thưởng lấy ưu đãi | Để dùng điểm | 3.0 | Vừa | Redemption giữ điểm, idempotent, trừ số dư | Mới | Cần API |
| US082 | Khách hàng | Tôi muốn xem đánh giá của người khác trên sản phẩm | Để quyết định mua | 3.0 | Vừa | Public rating/review summary trên product detail | Mới | Cần API |
| US083 | Nhân viên | Tôi muốn thấy cảnh báo đơn quá hạn theo SLA | Để ưu tiên bếp | 3.0 | Vừa | Đơn vượt ngưỡng thời gian hiển thị cảnh báo | Mới | Cần SLA |
| US084 | Shipper | Tôi muốn báo cáo giao thất bại | Để xử lý ngoại lệ | 3.0 | Vừa | Lý do thất bại, trả đơn về cửa hàng, giao lại | Mới | Cần workflow |
| US085 | Shipper | Tôi muốn xem đối soát tiền COD | Để khớp ca | 3.0 | Vừa | Số tiền thu, chưa nộp, lịch sử nộp | Mới | Cần API |
| US086 | Khách hàng | Tôi muốn chat/trả lời ticket hỗ trợ | Để giải quyết nhanh | 3.0 | Vừa | Thread phản hồi trong ticket | Mới | Cần schema |
| US087 | Quản trị viên | Tôi muốn thông báo riêng từng người trong cùng vai trò | Để không ai thấy trạng thái đọc của người khác | 3.0 | Cao | Per-recipient read state | Mới | Cần schema |
| US088 | Khách hàng | Tôi muốn tải hóa đơn/biên nhận PDF | Để lưu chứng từ | 3.0 | Thấp | Xuất biên nhận từ order | Mới | |
| US089 | Khách hàng | Tôi muốn đặt combo theo nhóm người | Để chọn nhanh | 2.0 | Vừa | Combo 1/2 người/gia đình nổi bật | Mới | |
| US090 | Khách hàng | Tôi muốn xem trạng thái cửa hàng đang mở/đóng | Để biết có thể đặt | 2.0 | Vừa | Hiển thị isOpen theo cấu hình giờ | Xong | |
| US091 | Khách hàng | Tôi muốn nhận gợi ý món theo lịch sử | Để đặt nhanh | 4.0 | Thấp | Personalized recommendation | Mới | |
| US092 | Khách hàng | Tôi muốn đặt lại nhanh từ tổng quan tài khoản | Để tiết kiệm thời gian | 3.0 | Vừa | Account overview hiển thị đơn hoạt động + quick reorder | Xong | |
| US093 | Khách hàng | Tôi muốn xem ví mã ưu đãi | Để dùng đúng hạn | 2.0 | Vừa | /account/coupons liệt kê coupon đã claim | Xong | |
| US094 | Khách hàng | Tôi muốn xem sổ địa chỉ riêng | Để quản lý gọn | 2.0 | Vừa | /account/addresses CRUD đầy đủ | Xong | |
| US095 | Khách hàng | Tôi muốn xem thông báo riêng | Để theo dõi | 2.0 | Vừa | /account/notifications inbox | Xong | |
| US096 | Khách hàng | Tôi muốn xem trung tâm trợ giúp | Để tự tra cứu | 2.0 | Vừa | /help FAQ, giao hàng, thanh toán, hủy/hoàn | Xong | |
| US097 | Khách hàng | Tôi muốn xem điều khoản và chính sách | Để hiểu ràng buộc | 2.0 | Thấp | /terms, /privacy | Xong | |
| US098 | Khách hàng | Tôi muốn trang tối ưu SEO cho thực đơn | Để tìm thấy qua Google | 2.0 | Vừa | Meta description/robots/canonical/OG cho trang indexable | Xong | |
| US099 | Nhân viên | Tôi muốn giao diện bếp dễ đọc từ xa | Để làm việc nhanh | 2.0 | Vừa | Kitchen board card lớn, cột trạng thái, đơn quá hạn | Xong | |
| US100 | Nhân viên | Tôi muốn xem tùy chọn món ngay trong ticket bếp | Để chế biến chính xác | 2.0 | Vừa | Modifiers hiển thị trong queue/detail | Xong | |
| US101 | Shipper | Tôi muốn ứng dụng gọn trên điện thoại | Để giao hàng tiện | 2.0 | Vừa | Shipper layout mobile-first, bottom nav | Xong | |
| US102 | Shipper | Tôi muốn xem chi tiết đơn lịch sử khi đã hết ca | Để đối chiếu | 3.0 | Vừa | Terminal order readable ngoài ca | Xong | |
| US103 | Quản trị viên | Tôi muốn editor sản phẩm tách theo route | Để chỉnh sửa rõ ràng | 2.0 | Vừa | /admin/products/new + /:id/edit với 5 section | Xong | |
| US104 | Quản trị viên | Tôi muốn cảnh báo khi rời editor chưa lưu | Để không mất dữ liệu | 2.0 | Vừa | Dirty navigation guard + beforeunload | Xong | |
| US105 | Quản trị viên | Tôi muốn xem sổ giao dịch tồn kho | Để truy vết biến động | 2.0 | Vừa | /admin/inventory/ledger lọc variant/type/date, phân trang | Xong | |
| US106 | Quản trị viên | Tôi muốn trang hoàn tiền riêng | Để xử lý tập trung | 2.0 | Cao | /admin/refunds KPI, filter, dialog REFUNDED/REJECTED | Xong | |
| US107 | Quản trị viên | Tôi muốn xem thông tin thanh toán chi tiết đơn | Để đối soát | 2.0 | Vừa | Payment block provider/reference/attempt trong order detail | Xong | |
| US108 | Quản trị viên | Tôi muốn breadcrumb trong trang quản trị | Để điều hướng | 3.0 | Thấp | AppBreadcrumbs từ meta.breadcrumb | Xong | |
| US109 | Khách hàng | Tôi muốn biên nhận đặt hàng chi tiết | Để xác nhận thông tin | 1.0 | Cao | Order success: items, tổng, địa chỉ, payment, in biên nhận; guest cần marker phiên | Xong | |
| US110 | Khách hàng | Tôi muốn xác nhận đặt hàng không bị giả mạo bằng URL | Để an toàn | 2.0 | Cao | Order success guest yêu cầu session marker một lần | Xong | |
| US111 | Khách hàng | Tôi muốn theo dõi đơn có tự cập nhật | Để biết trạng thái mới | 2.0 | Vừa | Poll 30s khi đơn đang hoạt động | Xong | |
| US112 | Khách hàng | Tôi muốn xem tổng quan tài khoản | Để nắm đơn và điểm | 2.0 | Vừa | /account/overview đơn hoạt động, điểm, shortcut | Xong | |
| US113 | Nhân viên | Tôi muốn lọc đơn theo ngày trong lịch sử | Để tra cứu | 3.0 | Thấp | Shipper history date filter client-side | Xong | |
| US114 | Khách hàng | Tôi muốn chọn địa chỉ đã lưu khi thanh toán | Để đặt nhanh | 1.0 | Cao | Checkout chọn từ sổ địa chỉ | Xong | |
| US115 | Khách hàng | Tôi muốn phí giao hàng hiển thị khi chọn khu vực | Để biết tổng tiền | 1.0 | Cao | Tính phí GHN theo district/ward | Xong | |
| US116 | Khách hàng | Tôi muốn xem phí dịch vụ và thuế rõ ràng | Để minh bạch giá | 2.0 | Vừa | Breakdown serviceFee/shipping/discount trong order | Xong | |
| US117 | Khách hàng | Tôi muốn thanh toán giữ nguyên giỏ khi thất bại | Để thử lại dễ | 2.0 | Vừa | Checkout lỗi không xóa giỏ, giữ input | Xong | |
| US118 | Khách hàng | Tôi muốn đặt hàng với ghi chú giao | Để dặn shipper | 2.0 | Thấp | Delivery note lưu theo đơn | Xong | |
| US119 | Khách hàng | Tôi muốn giỏ hàng xóa món không còn khả dụng | Để tránh lỗi | 2.0 | Vừa | Item hết hàng/gỡ bán hiển thị cảnh báo và có thể xóa | Xong | |
| US120 | Quản trị viên | Tôi muốn xem tổng quan admin | Để nắm tình hình | 1.0 | Cao | Dashboard: doanh thu, đơn theo trạng thái, sản phẩm bán chạy | Xong | |
