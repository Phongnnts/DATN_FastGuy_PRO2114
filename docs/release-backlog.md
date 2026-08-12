# Release Backlog — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx`.
Chia theo 6 Sprint; mỗi backlog gộp 5 user story.

| Backlog ID | Backlog | STORY / FEATURE (As a) | I want to (Goal) | So that (Reason) | Priority | Business Value | Sprint# | State | Note |
| ---------- | ------- | ---------------------- | ---------------- | ---------------- | -------- | -------------- | ------- | ----- | ---- |
| PB01 | Đăng ký / Đăng nhập | Người dùng | Đăng ký tài khoản | Vào hệ thống mua sắm | 1.0 | Cao | 1 | Xong | |
| PB01 | Đăng ký / Đăng nhập | Người dùng | Đăng nhập bằng email/SĐT | Truy cập tài khoản | 1.0 | Cao | 1 | Xong | |
| PB01 | Đăng ký / Đăng nhập | Người dùng | Đăng xuất | Thoát phiên an toàn | 1.0 | Vừa | 1 | Xong | |
| PB01 | Đăng ký / Đăng nhập | Người dùng | Đặt lại mật khẩu khi quên | Lấy lại quyền truy cập | 2.0 | Cao | 1 | Xong | |
| PB01 | Đăng ký / Đăng nhập | Người dùng | Tài khoản bị khóa khi sai mật khẩu nhiều lần | Chống brute-force | 4.0 | Cao | 1 | Mới | Backlog bảo mật |
| PB02 | Tài khoản & Bảo mật | Người dùng | Đổi mật khẩu | Bảo mật tài khoản | 2.0 | Vừa | 1 | Xong | |
| PB02 | Tài khoản & Bảo mật | Người dùng | Xem và cập nhật hồ sơ | Thông tin luôn chính xác | 2.0 | Vừa | 1 | Xong | |
| PB02 | Tài khoản & Bảo mật | Người dùng | Đăng nhập email không phân biệt hoa thường | Không nhập sai do viết hoa | 3.0 | Vừa | 1 | Xong | |
| PB02 | Tài khoản & Bảo mật | Người dùng | Tự động trim khoảng trắng khi đăng nhập | Tránh lỗi copy-paste | 3.0 | Thấp | 1 | Xong | |
| PB02 | Tài khoản & Bảo mật | Người dùng | Quản lý địa chỉ giao hàng | Đặt hàng nhanh hơn | 2.0 | Cao | 1 | Xong | |
| PB03 | Danh mục & Tìm kiếm | Khách hàng | Xem danh sách sản phẩm theo danh mục | Dễ tìm món | 1.0 | Cao | 1 | Xong | |
| PB03 | Danh mục & Tìm kiếm | Khách hàng | Tìm kiếm món theo tên | Tìm món nhanh | 1.0 | Cao | 1 | Xong | |
| PB03 | Danh mục & Tìm kiếm | Khách hàng | Lọc theo giá, loại, khuyến mãi, còn hàng | Chọn đúng nhu cầu | 2.0 | Vừa | 1 | Xong | |
| PB03 | Danh mục & Tìm kiếm | Khách hàng | Xem chi tiết sản phẩm | Biết giá và tùy chọn | 1.0 | Cao | 1 | Xong | |
| PB03 | Danh mục & Tìm kiếm | Khách hàng | Xem sản phẩm nổi bật và bán chạy | Tham khảo gợi ý | 2.0 | Vừa | 1 | Xong | |
| PB04 | Giỏ hàng | Khách hàng | Thêm món kèm biến thể/tùy chọn vào giỏ | Chuẩn bị đặt hàng | 1.0 | Cao | 1 | Xong | |
| PB04 | Giỏ hàng | Khách hàng | Sửa số lượng trong giỏ | Điều chỉnh đơn | 1.0 | Cao | 1 | Xong | |
| PB04 | Giỏ hàng | Khách hàng | Xóa sản phẩm khỏi giỏ | Bỏ món không cần | 1.0 | Vừa | 1 | Xong | |
| PB04 | Giỏ hàng | Khách hàng | Xem giỏ với tổng tiền | Biết cần thanh toán | 1.0 | Cao | 1 | Xong | |
| PB04 | Giỏ hàng | Khách hàng | Giỏ lưu theo phiên khi chưa đăng nhập | Không mất khi tải lại | 2.0 | Vừa | 1 | Xong | |
| PB05 | Checkout & Thanh toán | Khách hàng | Đặt hàng COD không cần tài khoản | Mua nhanh | 1.0 | Cao | 2 | Xong | |
| PB05 | Checkout & Thanh toán | Khách hàng | Đặt hàng chuyển khoản PayOS | Thanh toán online | 1.0 | Cao | 2 | Xong | |
| PB05 | Checkout & Thanh toán | Khách hàng | Đặt hàng bằng tài khoản | Lưu lịch sử mua | 1.0 | Cao | 2 | Xong | |
| PB05 | Checkout & Thanh toán | Khách hàng | Chống đặt hàng trùng (idempotency) | Không tạo đơn kép | 1.0 | Cao | 2 | Xong | |
| PB05 | Checkout & Thanh toán | Khách hàng | Chọn địa chỉ đã lưu khi thanh toán | Đặt nhanh | 1.0 | Cao | 2 | Xong | |
| PB06 | Coupon | Khách hàng | Áp dụng mã giảm giá khi thanh toán | Được giảm giá | 1.0 | Cao | 2 | Xong | |
| PB06 | Coupon | Khách hàng | Kiểm tra giỏ không thay đổi khi thanh toán | Tránh sai giá | 1.0 | Cao | 2 | Xong | |
| PB06 | Coupon | Khách hàng | Nhận và xem mã giảm giá | Tiết kiệm chi phí | 2.0 | Vừa | 2 | Xong | |
| PB06 | Coupon | Khách hàng | Xem ví mã ưu đãi | Dùng đúng hạn | 2.0 | Vừa | 2 | Xong | |
| PB06 | Coupon | Khách hàng | Đặt hàng với ghi chú giao | Dặn shipper | 2.0 | Thấp | 2 | Xong | |
| PB07 | Đơn hàng & Tracking | Khách hàng | Nhận mã đơn sau khi đặt | Tra cứu tiến trình | 1.0 | Cao | 2 | Xong | |
| PB07 | Đơn hàng & Tracking | Khách hàng | Tra cứu đơn bằng mã + 4 số cuối SĐT | Theo dõi không cần đăng nhập | 1.0 | Cao | 2 | Xong | |
| PB07 | Đơn hàng & Tracking | Khách hàng | Xem lịch sử đơn hàng | Biết đã mua gì | 1.0 | Cao | 2 | Xong | |
| PB07 | Đơn hàng & Tracking | Khách hàng | Xem chi tiết đơn | Xem trạng thái và thanh toán | 1.0 | Cao | 2 | Xong | |
| PB07 | Đơn hàng & Tracking | Khách hàng | Hủy đơn khi còn ở trạng thái cho phép | Thay đổi quyết định | 1.0 | Cao | 2 | Xong | |
| PB08 | Đánh giá, Yêu thích, Điểm, Thông báo | Khách hàng | Đánh giá món sau khi nhận | Chia sẻ trải nghiệm | 2.0 | Vừa | 2 | Xong | |
| PB08 | Đánh giá, Yêu thích, Điểm, Thông báo | Khách hàng | Quản lý món yêu thích | Lưu món ưa thích | 2.0 | Vừa | 2 | Xong | |
| PB08 | Đánh giá, Yêu thích, Điểm, Thông báo | Khách hàng | Xem điểm thưởng và hạng thành viên | Biết quyền lợi | 2.0 | Vừa | 2 | Xong | |
| PB08 | Đánh giá, Yêu thích, Điểm, Thông báo | Khách hàng | Nhận điểm khi đơn giao thành công | Được ưu đãi | 2.0 | Cao | 2 | Xong | |
| PB08 | Đánh giá, Yêu thích, Điểm, Thông báo | Khách hàng | Xem thông báo và đánh dấu đã đọc | Cập nhật đơn hàng | 2.0 | Vừa | 2 | Xong | |
| PB09 | Hỗ trợ & Thanh toán | Khách hàng | Gửi yêu cầu hỗ trợ | Được giải quyết vấn đề | 2.0 | Vừa | 3 | Xong | |
| PB09 | Hỗ trợ & Thanh toán | Khách hàng | Trả về đúng trạng thái khi thanh toán bị hủy | Biết làm gì tiếp | 2.0 | Cao | 3 | Xong | |
| PB09 | Hỗ trợ & Thanh toán | Khách hàng | Xem trạng thái cửa hàng mở/đóng | Biết có thể đặt | 2.0 | Vừa | 3 | Xong | |
| PB09 | Hỗ trợ & Thanh toán | Khách hàng | Xem phí dịch vụ và thuế rõ ràng | Minh bạch giá | 2.0 | Vừa | 3 | Xong | |
| PB09 | Hỗ trợ & Thanh toán | Khách hàng | Thanh toán thất bại giữ nguyên giỏ | Thử lại dễ | 2.0 | Vừa | 3 | Xong | |
| PB10 | Staff — Ca làm & Bếp | Nhân viên | Xem ca làm việc và check-in/out | Bắt đầu ca | 1.0 | Cao | 3 | Xong | |
| PB10 | Staff — Ca làm & Bếp | Nhân viên | Xem danh sách đơn cần xử lý | Ưu tiên công việc | 1.0 | Cao | 3 | Xong | |
| PB10 | Staff — Ca làm & Bếp | Nhân viên | Xem chi tiết đơn kèm tùy chọn món | Chế biến đúng | 1.0 | Cao | 3 | Xong | |
| PB10 | Staff — Ca làm & Bếp | Nhân viên | Xác nhận đơn | Bắt đầu xử lý | 1.0 | Cao | 3 | Xong | |
| PB10 | Staff — Ca làm & Bếp | Nhân viên | Chuyển đơn sang chế biến và sẵn sàng | Báo bếp | 1.0 | Cao | 3 | Xong | |
| PB11 | Staff — Xử lý & Hỗ trợ | Nhân viên | Hủy đơn khi cần | Xử lý sai sót | 1.0 | Vừa | 3 | Xong | |
| PB11 | Staff — Xử lý & Hỗ trợ | Nhân viên | Thêm ghi chú nội bộ đơn | Trao đổi với bếp | 2.0 | Vừa | 3 | Xong | |
| PB11 | Staff — Xử lý & Hỗ trợ | Nhân viên | Xem lịch sử đơn đã xử lý | Kiểm tra lại | 2.0 | Vừa | 3 | Xong | |
| PB11 | Staff — Xử lý & Hỗ trợ | Nhân viên | Xử lý yêu cầu hỗ trợ | Chăm sóc khách | 2.0 | Vừa | 3 | Xong | |
| PB11 | Staff — Xử lý & Hỗ trợ | Nhân viên | Xem giao diện bếp dễ đọc từ xa | Làm việc nhanh | 2.0 | Vừa | 3 | Xong | |
| PB12 | Dispatch & Shipper | Nhân viên | Phân công Shipper cho đơn sẵn sàng | Giao hàng | 1.0 | Cao | 3 | Xong | |
| PB12 | Dispatch & Shipper | Nhân viên | Xem board điều phối với tải Shipper | Giao việc hợp lý | 2.0 | Cao | 3 | Xong | |
| PB12 | Dispatch & Shipper | Shipper | Xem ca làm việc và check-in | Bắt đầu giao | 1.0 | Cao | 3 | Xong | |
| PB12 | Dispatch & Shipper | Shipper | Xem đơn được phân công | Nhận việc | 1.0 | Cao | 3 | Xong | |
| PB12 | Dispatch & Shipper | Shipper | Xem chi tiết đơn giao | Giao đúng | 1.0 | Cao | 3 | Xong | |
| PB13 | Shipper — Giao hàng | Shipper | Gọi khách và mở Google Maps | Giao hiệu quả | 2.0 | Vừa | 3 | Xong | |
| PB13 | Shipper — Giao hàng | Shipper | Xác nhận nhận hàng | Bắt đầu giao | 1.0 | Cao | 3 | Xong | |
| PB13 | Shipper — Giao hàng | Shipper | Xác nhận giao thành công | Hoàn tất đơn | 1.0 | Cao | 3 | Xong | |
| PB13 | Shipper — Giao hàng | Shipper | Xem tổng quan ca giao | Biết khối lượng | 2.0 | Vừa | 3 | Xong | |
| PB13 | Shipper — Giao hàng | Shipper | Xem lịch sử giao và lọc ngày | Đối chiếu | 2.0 | Vừa | 3 | Xong | |
| PB14 | Admin — Người dùng & Danh mục | Quản trị viên | Quản lý người dùng | Kiểm soát tài khoản | 1.0 | Cao | 4 | Xong | |
| PB14 | Admin — Người dùng & Danh mục | Quản trị viên | Quản lý danh mục | Tổ chức thực đơn | 1.0 | Cao | 4 | Xong | |
| PB14 | Admin — Người dùng & Danh mục | Quản trị viên | Quản lý sản phẩm | Cập nhật thực đơn | 1.0 | Cao | 4 | Xong | |
| PB14 | Admin — Người dùng & Danh mục | Quản trị viên | Quản lý biến thể và tùy chọn | Linh hoạt món | 1.0 | Cao | 4 | Xong | |
| PB14 | Admin — Người dùng & Danh mục | Quản trị viên | Editor sản phẩm tách theo route | Chỉnh sửa rõ ràng | 2.0 | Vừa | 4 | Xong | |
| PB15 | Admin — Tồn kho | Quản trị viên | Quản lý tồn kho | Không hết hàng | 1.0 | Cao | 4 | Xong | |
| PB15 | Admin — Tồn kho | Quản trị viên | Xem sổ giao dịch tồn kho | Truy vết biến động | 2.0 | Vừa | 4 | Xong | |
| PB15 | Admin — Tồn kho | Quản trị viên | Điều chỉnh tồn kho thủ công | Sửa sai lệch kiểm kê | 3.0 | Cao | 4 | Xong | Endpoint + ledger audit; xử lý conflict; backend 176 test, frontend 247 test |
| PB15 | Admin — Tồn kho | Quản trị viên | Ghi nhận lãng phí thủ công | Quản lý hao hụt | 3.0 | Vừa | 4 | Mới | Cần schema |
| PB15 | Admin — Tồn kho | Quản trị viên | Giỏ hàng xóa món không khả dụng | Tránh lỗi | 2.0 | Vừa | 4 | Xong | |
| PB16 | Admin — Đơn hàng & Hoàn tiền | Quản trị viên | Xem và xử lý đơn hàng | Giám sát | 1.0 | Cao | 4 | Xong | |
| PB16 | Admin — Đơn hàng & Hoàn tiền | Quản trị viên | Xử lý hoàn tiền | Bồi hoàn khách | 1.0 | Cao | 4 | Xong | |
| PB16 | Admin — Đơn hàng & Hoàn tiền | Quản trị viên | Trang hoàn tiền riêng | Xử lý tập trung | 2.0 | Cao | 4 | Xong | |
| PB16 | Admin — Đơn hàng & Hoàn tiền | Quản trị viên | Xem thông tin thanh toán chi tiết đơn | Đối soát | 2.0 | Vừa | 4 | Xong | |
| PB16 | Admin — Đơn hàng & Hoàn tiền | Quản trị viên | Xem audit log thao tác | Truy vết | 4.0 | Vừa | 4 | Mới | Cần schema |
| PB17 | Admin — Coupon, Banner, Ca | Quản trị viên | Quản lý mã giảm giá | Chạy khuyến mãi | 1.0 | Cao | 5 | Xong | |
| PB17 | Admin — Coupon, Banner, Ca | Quản trị viên | Quản lý banner | Quảng bá | 2.0 | Vừa | 5 | Xong | |
| PB17 | Admin — Coupon, Banner, Ca | Quản trị viên | Phân ca làm việc | Sắp lịch nhân sự | 1.0 | Cao | 5 | Xong | |
| PB17 | Admin — Coupon, Banner, Ca | Quản trị viên | Cấu hình cửa hàng | Vận hành linh hoạt | 1.0 | Vừa | 5 | Xong | |
| PB17 | Admin — Coupon, Banner, Ca | Quản trị viên | Breadcrumb trong trang quản trị | Điều hướng | 3.0 | Thấp | 5 | Xong | |
| PB18 | Báo cáo | Quản trị viên | Xem báo cáo doanh thu | Đánh giá kinh doanh | 1.0 | Cao | 5 | Xong | |
| PB18 | Báo cáo | Quản trị viên | Xuất báo cáo CSV | Lưu trữ | 2.0 | Vừa | 5 | Xong | |
| PB18 | Báo cáo | Quản trị viên | Xem tổng quan admin | Nắm tình hình | 1.0 | Cao | 5 | Xong | |
| PB18 | Báo cáo | Khách hàng | Trang tối ưu SEO | Tìm thấy qua Google | 2.0 | Vừa | 5 | Xong | |
| PB18 | Báo cáo | Quản trị viên | Xem lỗi gallery sản phẩm khi thiếu trường | Tạo sản phẩm ổn định | 1.0 | Cao | 5 | Mới | ADM007 |
| PB19 | Trải nghiệm tài khoản | Khách hàng | Xem tổng quan tài khoản | Nắm đơn và điểm | 2.0 | Vừa | 5 | Xong | |
| PB19 | Trải nghiệm tài khoản | Khách hàng | Xem sổ địa chỉ riêng | Quản lý gọn | 2.0 | Vừa | 5 | Xong | |
| PB19 | Trải nghiệm tài khoản | Khách hàng | Xem thông báo riêng | Theo dõi | 2.0 | Vừa | 5 | Xong | |
| PB19 | Trải nghiệm tài khoản | Khách hàng | Xem trung tâm trợ giúp | Tự tra cứu | 2.0 | Vừa | 5 | Xong | |
| PB19 | Trải nghiệm tài khoản | Khách hàng | Xem điều khoản và chính sách | Hiểu ràng buộc | 2.0 | Thấp | 5 | Xong | |
| PB20 | Biên nhận & Đặt lại nhanh | Khách hàng | Biên nhận đặt hàng chi tiết | Xác nhận thông tin | 1.0 | Cao | 5 | Xong | |
| PB20 | Biên nhận & Đặt lại nhanh | Khách hàng | Xác nhận đặt hàng không bị giả mạo URL | An toàn | 2.0 | Cao | 5 | Xong | |
| PB20 | Biên nhận & Đặt lại nhanh | Khách hàng | Theo dõi đơn tự cập nhật | Biết trạng thái mới | 2.0 | Vừa | 5 | Xong | |
| PB20 | Biên nhận & Đặt lại nhanh | Khách hàng | Đặt lại món từ đơn cũ | Đặt lại nhanh | 3.0 | Vừa | 5 | Xong | |
| PB20 | Biên nhận & Đặt lại nhanh | Khách hàng | Đặt lại nhanh từ tổng quan | Tiết kiệm thời gian | 3.0 | Vừa | 5 | Xong | |
| PB21 | Nâng cao — Realtime & Điểm | Khách hàng | Theo dõi vị trí Shipper thời gian thực | Biết đơn đến đâu | 4.0 | Cao | 6 | Mới | Cần GPS |
| PB21 | Nâng cao — Realtime & Điểm | Khách hàng | Đặt hàng theo khung giờ giao | Chủ động thời gian | 3.0 | Vừa | 6 | Mới | Cần schema |
| PB21 | Nâng cao — Realtime & Điểm | Khách hàng | Đổi điểm thưởng lấy ưu đãi | Dùng điểm | 3.0 | Vừa | 6 | Mới | Cần API |
| PB21 | Nâng cao — Realtime & Điểm | Khách hàng | Xem đánh giá người khác trên sản phẩm | Quyết định mua | 3.0 | Vừa | 6 | Mới | Cần API |
| PB21 | Nâng cao — Realtime & Điểm | Khách hàng | Nhận gợi ý món theo lịch sử | Đặt nhanh | 4.0 | Thấp | 6 | Mới | |
| PB22 | Nâng cao — Bếp & SLA | Nhân viên | Thấy cảnh báo đơn quá hạn theo SLA | Ưu tiên bếp | 3.0 | Vừa | 6 | Mới | Cần SLA |
| PB22 | Nâng cao — Bếp & SLA | Nhân viên | Xem tùy chọn món trong ticket bếp | Chế biến chính xác | 2.0 | Vừa | 6 | Xong | |
| PB22 | Nâng cao — Bếp & SLA | Shipper | Báo cáo giao thất bại | Xử lý ngoại lệ | 3.0 | Vừa | 6 | Mới | Cần workflow |
| PB22 | Nâng cao — Bếp & SLA | Shipper | Xem đối soát tiền COD | Khớp ca | 3.0 | Vừa | 6 | Mới | Cần API |
| PB22 | Nâng cao — Bếp & SLA | Khách hàng | Chat/trả lời ticket hỗ trợ | Giải quyết nhanh | 3.0 | Vừa | 6 | Mới | Cần schema |
| PB23 | Nâng cao — Thông báo & Biên nhận | Quản trị viên | Thông báo riêng từng người cùng vai trò | Không lộ trạng thái đọc | 3.0 | Cao | 6 | Mới | Cần schema |
| PB23 | Nâng cao — Thông báo & Biên nhận | Khách hàng | Tải hóa đơn/biên nhận PDF | Lưu chứng từ | 3.0 | Thấp | 6 | Mới | |
| PB23 | Nâng cao — Thông báo & Biên nhận | Khách hàng | Đặt combo theo nhóm người | Chọn nhanh | 2.0 | Vừa | 6 | Mới | |
| PB23 | Nâng cao — Thông báo & Biên nhận | Shipper | Ứng dụng gọn trên điện thoại | Giao tiện | 2.0 | Vừa | 6 | Xong | |
| PB23 | Nâng cao — Thông báo & Biên nhận | Shipper | Xem chi tiết đơn lịch sử ngoài ca | Đối chiếu | 3.0 | Vừa | 6 | Xong | |
| PB24 | Nâng cao — Vận hành & Điều phối | Quản trị viên | Xem sổ giao dịch tồn kho phân trang | Truy vết quy mô | 2.0 | Vừa | 6 | Xong | |
| PB24 | Nâng cao — Vận hành & Điều phối | Nhân viên | Điều phối theo tải công việc Shipper | Phân việc hợp lý | 2.0 | Cao | 6 | Xong | |
| PB24 | Nâng cao — Vận hành & Điều phối | Shipper | Lọc đơn theo ngày trong lịch sử | Tra cứu | 3.0 | Thấp | 6 | Xong | |
| PB24 | Nâng cao — Vận hành & Điều phối | Khách hàng | Chọn địa chỉ đã lưu khi thanh toán | Đặt nhanh | 1.0 | Cao | 6 | Xong | |
| PB24 | Nâng cao — Vận hành & Điều phối | Khách hàng | Phí giao hàng hiển thị khi chọn khu vực | Biết tổng tiền | 1.0 | Cao | 6 | Xong | |
