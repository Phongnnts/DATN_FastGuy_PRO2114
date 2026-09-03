# Menu Filters and Admin Visibility Design

## Objective

- Làm bộ lọc trang Thực đơn tự nhiên, dựa trên dữ liệu thật và hiển thị đầy đủ mọi danh mục hiện có từ API.
- Ẩn hoàn toàn Nhật ký hoạt động khỏi giao diện Admin và chặn route `/admin/activity-logs`.
- Ẩn hai nhóm Cài đặt hệ thống `Phí & thuế` và `Giao hàng` mà không đổi backend hoặc dữ liệu cấu hình.

## Menu categories

- Thanh danh mục lấy toàn bộ danh mục hiện có từ API, giữ đúng tên và số lượng trả về/được tính từ catalog.
- `Tất cả` đứng đầu; sau đó là mọi danh mục đang có theo thứ tự API.
- Không giới hạn tám mục, không đặt danh mục còn lại trong menu `Thêm`.
- Không gộp hoặc đổi tên taxonomy; mỗi category ID là một filter độc lập.
- Desktop cho phép xuống dòng có kiểm soát nếu cần; mobile dùng cuộn ngang, không gây tràn trang.
- Category được đồng bộ qua query `category`, reset page về 1 và có active state rõ ràng.

## Natural quick filters

Giữ các filter có dữ liệu/API thật:

- `Bán chạy` → `sold=1`.
- `Đang giảm giá` → `discounted=true`.
- `Dưới 40K` → `maxPrice=40000`.
- `Còn hàng` → `availability=AVAILABLE`.

Bỏ `Combo văn phòng` và `Combo sinh viên` vì đang dựa vào tìm từ khóa trong tên/mô tả, không phải thuộc tính catalog chuẩn. Quick filter, drawer filter, URL query, active chips và reset phải dùng cùng state, không tạo điều kiện mâu thuẫn hoặc chip trùng.

Giữ bộ lọc chi tiết hiện có: khoảng giá, trạng thái hàng, giảm giá, bán chạy, sort và grid/list. Không đổi API contract.

## Admin visibility

- Xóa entry Nhật ký hoạt động khỏi sidebar Admin.
- Xóa route `/admin/activity-logs`; truy cập URL cũ phải đi vào NotFound/route fallback hiện có thay vì tải page/API.
- Không xóa backend endpoint, entity, seed hoặc file page; chỉ làm tính năng không thể truy cập từ frontend.
- Trong Settings, loại `fees` và `delivery` khỏi danh sách tab hiển thị/điều hướng.
- Không gọi payment/delivery-specific loader khi tab không còn truy cập; giữ mapping/config backend để tránh scope creep.
- Các tab còn lại giữ loading, validation, save boundaries và keyboard tab behavior.

## Testing

- Test utility cho toàn bộ category giữ nguyên taxonomy/thứ tự và quick-filter mapping mới.
- Test MenuPage cho đủ category, không còn menu `Thêm` hoặc combo filters, URL/chip/reset nhất quán.
- Test router/sidebar xác nhận Activity Logs không còn truy cập.
- Test Settings xác nhận không hiển thị `Phí & thuế`, `Giao hàng`, nhưng các tab còn lại và save flow không đổi.
- Chạy focused tests, `npm test`, `npm run build`, Playwright Chromium desktop với không lỗi console/page và request catalog thành công.

## Non-goals

- Không đổi database, backend, OpenAPI hoặc seed.
- Không xóa dữ liệu ActivityLog hay config phí/giao hàng.
- Không redesign tổng thể trang Menu/Admin.
