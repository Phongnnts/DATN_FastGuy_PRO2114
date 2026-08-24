# Shipper UI Redesign Design

## Mục tiêu

Thiết kế lại toàn bộ giao diện Shipper theo hướng Field Command: thao tác nhanh khi đang di chuyển, thông tin cô đọng, thẩm mỹ hiện đại. Thay đổi chỉ thuộc presentation và cách tổ chức nội dung; giữ nguyên API, Pinia store, route nghiệp vụ và quy tắc chuyển trạng thái hiện có.

Thứ tự ưu tiên:

1. Tốc độ thao tác và khả năng dùng một tay.
2. Mật độ thông tin đủ để quyết định nhanh.
3. Hình ảnh nhất quán, hiện đại và cao cấp.

## Phạm vi

Redesign toàn bộ khu vực Shipper:

- Layout và điều hướng.
- Dashboard.
- Đơn đang giao và lịch sử đơn.
- Chi tiết đơn và action sheet.
- Ca làm.
- Bàn giao COD.
- Hồ sơ.
- Loading, empty, error, conflict và submitting states.

Không thuộc phạm vi:

- Thay đổi database, backend, API contract hoặc response DTO.
- Thay đổi transition `ASSIGNED -> PICKED_UP -> DELIVERED` và delivery failure hiện có.
- Tối ưu tuyến đường, bản đồ nhúng hoặc theo dõi vị trí nền.
- Thêm nghiệp vụ, số liệu hoặc field chưa có từ API hiện tại.

## Ngôn ngữ hình ảnh

- Nền xám trung tính nhẹ; card trắng; chữ xanh than có tương phản cao.
- Đỏ FastGuy dành cho CTA chính, điểm nhấn điều hướng và việc cần xử lý ngay.
- Xanh lá biểu thị ca đang hoạt động hoặc hoàn thành thành công.
- Cam biểu thị trạng thái cần chú ý; xám biểu thị trạng thái đã kết thúc hoặc thứ yếu.
- Border mảnh, radius vừa, shadow tiết chế; tránh card nổi quá mức.
- Typography phân cấp rõ: mã đơn và CTA nổi bật; metadata nhỏ nhưng vẫn đọc được ngoài trời.
- Icon luôn đi cùng accessible name khi không có nhãn chữ.

## Responsive Layout

### Mobile

- Tối ưu từ 320px đến 480px.
- Header gọn, content một cột, safe-area padding.
- Bottom navigation cố định gồm năm mục: Trang chủ, Đơn, Lịch sử, COD, Hồ sơ.
- Ca làm truy cập từ trạng thái ca hoặc shortcut trên Dashboard.
- CTA chính tối thiểu 44px và đặt trong vùng dễ chạm bằng một tay.
- CTA chuyển trạng thái trên chi tiết đơn sticky ở cuối, không che nội dung.

### Desktop

- Sidebar cố định hiển thị đủ sáu mục: Trang chủ, Đơn giao, Lịch sử, Ca làm, COD, Hồ sơ.
- Vùng nội dung rộng, giới hạn chiều dài dòng và mở rộng card thành grid khi hữu ích.
- Các action quan trọng vẫn hiện trực tiếp; không phụ thuộc hover.

## Cấu trúc màn hình

### Layout

- Header/sidebar hiển thị nhận diện Shipper, trạng thái ca và điều hướng.
- Trạng thái ca có nhãn rõ: đang trong ca, đã kết thúc hoặc chưa check-in.
- Mobile bottom navigation chỉ chứa năm đích chính; route Ca làm vẫn tồn tại và được mở từ Dashboard.
- Logout giữ nguyên hành vi hiện tại.

### Dashboard

- Khối chào ngắn cùng trạng thái ca và giờ ca hiện tại.
- Nhóm số liệu cô đọng từ dữ liệu đang có: chờ lấy, đang giao, COD hoặc kết quả ca nếu API hiện tại cung cấp.
- Danh sách "Việc cần làm ngay" ưu tiên đơn đang giao trước, sau đó đơn chờ lấy; không tạo thuật toán tối ưu tuyến.
- Mỗi card hiển thị mã đơn, trạng thái, khách hàng/điểm nhận, địa chỉ, thanh toán và CTA kế tiếp.
- Shortcut Ca làm đưa đến route Ca làm; không nhúng toàn bộ quản lý ca vào Dashboard.

### Đơn giao

- Hai tab nghiệp vụ giữ nguyên: chờ lấy và đang giao.
- Search, sort và bộ lọc giữ nguyên dữ liệu/hành vi, được gom thành toolbar gọn.
- Card là semantic link tới chi tiết; quick actions Gọi, Bản đồ và CTA nghiệp vụ vẫn truy cập bằng bàn phím.
- Action sheet mobile giữ focus trap, Escape, restore focus, COD validation và confirm dialog.
- Polling 30 giây, stale-request guard và refresh sau mutation giữ nguyên.

### Lịch sử

- Dùng chung cấu trúc card với Đơn giao nhưng giảm độ nổi của CTA.
- Bộ lọc ngày, search, sort và phân trang hiện có được trình bày rõ trên desktop, thu gọn hợp lý trên mobile.
- Trạng thái và timestamp là thông tin chính; không thêm field mới.

### Chi tiết đơn

- Header gồm mã đơn, badge trạng thái và thời gian liên quan.
- Các khu vực: khách hàng, địa chỉ, danh sách món, thanh toán/COD, ghi chú và timeline.
- Gọi và mở bản đồ là action phụ luôn có nhãn rõ.
- CTA chính phản ánh trạng thái hiện tại: lấy hàng hoặc giao thành công.
- Báo giao thất bại tách khỏi CTA thành công, giữ nguyên reason validation và dialog hiện có.
- COD yêu cầu số tiền chính xác như nghiệp vụ hiện tại.

### Ca làm

- ShiftStatus là khối chính cho check-in/check-out và trạng thái hiện tại.
- Ca hôm nay hiển thị trước; ca sắp tới và lịch sử theo sau.
- Check-in, check-out, timestamp và trạng thái giữ nguyên hành vi hiện tại.
- Event `staff-shift-changed` tiếp tục đồng bộ layout, dashboard và danh sách ca.

### COD

- Ba số liệu chính: tiền dự kiến, tiền đã gửi và kết quả Admin xác nhận.
- Form bàn giao chỉ hiện khi state hiện tại cho phép.
- Cảnh báo "không thể sửa sau khi gửi" đặt sát input và CTA.
- Conflict, validation, success và lịch sử đối soát giữ nguyên dữ liệu/hành vi hiện tại.

### Hồ sơ

- Trình bày thông tin cá nhân và trạng thái tài khoản hiện có theo card rõ ràng.
- Không thêm chỉnh sửa hồ sơ nếu API hiện tại không hỗ trợ.
- Logout là action nguy hiểm thứ yếu, có khoảng cách với thao tác thông thường.

## Dữ liệu và hành vi

- Vue views tiếp tục gọi Pinia store và API clients hiện có.
- Không đổi request, response, endpoint hoặc OpenAPI.
- Không suy diễn dữ liệu bản đồ, khoảng cách, tuyến tối ưu hoặc số liệu không có trong response.
- Mutation giữ expected status/version và xử lý HTTP 409 hiện có.
- CTA bị khóa khi request đang chạy để tránh submit trùng.
- Navigation, polling, generation guards và cleanup khi unmount được bảo toàn.

## Trạng thái hệ thống

- Loading dùng skeleton theo hình dạng nội dung; text `role="status"` vẫn khả dụng cho assistive technology.
- Empty state nêu rõ không có dữ liệu và không biến thành lỗi.
- Error state hiển thị thông báo ngắn và nút Thử lại.
- Conflict 409 thông báo dữ liệu đã đổi và hiển thị dữ liệu mới nhất sau refresh.
- Thành công được announce bằng live region; dialog đóng và focus được khôi phục đúng chỗ.
- Reduced motion loại bỏ animation không cần thiết.

## Accessibility

- Touch target tối thiểu 44x44px.
- Tương phản đáp ứng WCAG 2.2 AA.
- Bottom navigation và sidebar có current-page semantics.
- Tabs hỗ trợ ArrowLeft, ArrowRight, Home và End như hiện tại.
- Card điều hướng, button mutation và external links không dùng lẫn semantics.
- Dialog/action sheet có accessible name, focus trap, Escape và focus restoration.
- Focus ring luôn nhìn thấy; không truyền thông tin chỉ bằng màu.

## Tổ chức triển khai

- Tái sử dụng component hiện có; chỉ trích xuất token hoặc component dùng chung khi thực sự được nhiều màn hình sử dụng.
- Chuẩn hóa presentation trong ShipperLayout và các shipper views, không refactor store/API ngoài nhu cầu trực tiếp.
- Giữ diff nhỏ theo từng màn hình; ưu tiên CSS và markup trước JavaScript mới.
- Không thêm dependency.

## Kiểm thử và nghiệm thu

- Cập nhật hoặc thêm test nhỏ cho navigation, CTA visibility, COD validation và optimistic conflict nếu markup/hành vi thay đổi.
- Chạy test frontend liên quan, `npm test` và `npm run build`.
- Chạy Playwright desktop và mobile cho các luồng quan trọng:
  - Điều hướng toàn bộ khu vực Shipper.
  - Check-in/check-out và đồng bộ trạng thái ca.
  - Xem đơn, lấy hàng, giao thành công và báo giao thất bại.
  - COD validation và bàn giao COD.
  - Loading/error/empty/conflict states khả dụng.
- Xác nhận không có console error và request chính thành công.
- Nghiệm thu ở 320px, mobile phổ biến và desktop; content không bị che bởi sticky navigation/CTA.

## Tiêu chí hoàn thành

- Toàn bộ màn hình Shipper dùng nhất quán hướng Field Command.
- Mobile hoàn thành tác vụ chính bằng ít thao tác, CTA kế tiếp dễ nhận biết.
- Desktop hiển thị mật độ cao hơn mà không thay đổi nghiệp vụ.
- Mọi route và chức năng Shipper hiện tại vẫn hoạt động.
- Không thay đổi API/DB; không dùng dữ liệu không tồn tại.
- Full frontend tests, build và Playwright desktop/mobile đều đạt.
