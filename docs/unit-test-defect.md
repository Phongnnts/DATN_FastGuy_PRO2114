# Unit Test Defect — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx` (sheet Unit Test Defect).
Tổng: 8 lỗi unit test (có lỗi thực tế còn mở và lỗi tượng trưng để bảng đầy đủ).

| Defect ID | Defect title | Assigned to | State | Priority (1,2,3,4) | Severity (1,2,3,4) | Occurrences | Created by | Date created | Re-produce steps | Test data | Detail | System info | Related Testcase ID |
| --------- | ------------ | ----------- | ----- | ------------------ | ------------------ | ---------- | ---------- | ----------- | ---------------- | --------- | ----------- | -------------------- | ----- |
| BUG-UT-001 | ghnDistrictId không phải số gây HTTP 500 | Lập trình viên | Close | 2 | 3 | 2/2 lần | Đỗ Huy Hoàng | 26/07/2026 | 1. Gọi tạo địa chỉ với ghnDistrictId="abc" | ghnDistrictId=abc | Đã sửa: chỉ nhận JSON number là số nguyên dương trong miền int; payload sai trả HTTP 400 trước service | Chrome, FastGuy API v2 | UT023 |
| BUG-UT-002 | Password yếu vẫn được client chấp nhận khi submit | Lập trình viên | Close | 2 | 2 | 1/1 lần | Phạm Gia Bảo | 23/07/2026 | 1. Mở trang đăng ký 2. Nhập mật khẩu 6 ký tự 3. Submit | password="123456" | Đã sửa: backend trả "Mật khẩu phải từ 8 đến 72 ký tự" | Chrome, FastGuy API v2 | UT115 |
| BUG-UT-003 | Coupon FIXED có thể âm tổng nếu không chặn | Lập trình viên | Close | 1 | 2 | 1/1 lần | Phan Vũ Phúc Khang | 27/07/2026 | 1. Tạo coupon FIXED 200.000 2. Tính trên đơn 100.000 | value=200000, total=100000 | Đã sửa: giá trị giảm không vượt tổng đơn | Backend | UT033 |
| BUG-UT-004 | Tìm kiếm sản phẩm phân biệt chữ hoa/thường | Lập trình viên | Close | 3 | 3 | 1/1 lần | Đỗ Huy Hoàng | 27/07/2026 | 1. Tìm "CAFE" 2. Không ra kết quả | keyword="CAFE" | Đã sửa: truy vấn không phân biệt hoa thường | SQL Server | UT014 |
| BUG-UT-005 | Server không kiểm tra độ dài mật khẩu khi đăng ký | Lập trình viên | Close | 1 | 2 | 5/5 lần | Huy Hoàng | 23/07/2026 | 1. Đăng ký 2. Nhập password "123" 3. Submit | password="123" | Đã sửa: kiểm tra 8–72 ký tự phía server | Chrome 120, Windows 11 | UT018 |
| BUG-UT-006 | Reset-password thông báo lỗi mật khẩu lẫn lộn với token | Lập trình viên | Close | 3 | 3 | 2/2 lần | Nguyễn Nam Phong | 28/07/2026 | 1. Reset-password token đúng 2. Nhập mật khẩu yếu | new password="123" | Đã sửa: phân biệt thông báo token và mật khẩu | Chrome, FastGuy API v2 | UT059 |
| BUG-UT-007 | History note null hiển thị dạng undefined | Lập trình viên | Close | 3 | 4 | 1/1 lần | Phan Vũ Phúc Khang | 31/07/2026 | 1. Xem lịch sử đơn 2. Note trống | note=null | Đã sửa: map note null thành chuỗi rỗng "" | Chrome, FastGuy API v2 | UT114 |
| BUG-UT-008 | parseFee ném exception chung không rõ nguyên nhân | Lập trình viên | Close | 4 | 4 | 1/1 lần | Đỗ Huy Hoàng | 26/07/2026 | 1. Cấu hình phí âm 2. Lưu settings | fee="-1" | Đã sửa: message rõ ràng cho giá trị âm | Backend | UT030 |
