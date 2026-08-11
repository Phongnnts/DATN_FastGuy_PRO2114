# FastGuy Defect Closure and Integration UX Design

## Mục tiêu

Đóng các defect hệ thống còn tồn đọng bằng kiểm thử có bằng chứng, chỉ sửa hành vi người dùng chưa rõ khi GHN hoặc PayOS không khả dụng, và đồng bộ trạng thái backlog với code thực tế.

## Phạm vi

### BUG-ST-001: Product thiếu `galleryImages`

- Chạy regression test tạo product không gửi `galleryImages`.
- Kết quả hợp lệ phải không trả HTTP 500 và phải lưu gallery rỗng dưới dạng `[]`.
- Nếu test pass, không sửa implementation; chuyển defect sang Closed.
- Nếu test fail, sửa nhỏ nhất tại trust boundary hoặc persistence lifecycle và giữ database default hiện có.

### BUG-ST-007 và US008: Khóa đăng nhập tạm thời

- Chạy regression test cho năm lần sai, thông báo khóa ngay lần thứ năm, khóa 15 phút và reset cửa sổ thử sau khi hết hạn.
- Giữ transaction cùng `PESSIMISTIC_WRITE` để tránh lost update.
- Nếu test pass, không sửa implementation; chuyển BUG-ST-007 sang Closed và US008 sang Xong.
- Migration `042_login_bruteforce_lock.sql` phải tồn tại trong schema test trước system retest.

### BUG-ST-002 và BUG-ST-003: GHN không khả dụng

- GHN tiếp tục là nguồn phí giao hàng bắt buộc.
- Khi thiếu cấu hình, token không hợp lệ hoặc GHN tạm lỗi, checkout bị chặn.
- Không dùng `delivery_fee` mặc định và không miễn phí giao hàng.
- Frontend phân biệt trạng thái chưa có phí hợp lệ, không gửi yêu cầu tạo đơn.
- Thông báo phải nói rõ dịch vụ giao hàng chưa được cấu hình hoặc tạm không khả dụng và yêu cầu thử lại sau.
- Giữ HTTP status backend hiện có nếu contract đã phân biệt được lỗi tích hợp; chỉ mở rộng contract khi frontend không thể nhận biết trạng thái.

### BUG-ST-004: PayOS chưa cấu hình

- Phương thức `BANK_TRANSFER` luôn hiện trong danh sách thanh toán.
- Khi PayOS thiếu cấu hình, lựa chọn ở trạng thái disabled và hiển thị lý do.
- Frontend không gửi `BANK_TRANSFER` khi capability không khả dụng.
- COD vẫn hoạt động nếu GHN trả phí hợp lệ.
- Không thay đổi payment lifecycle, webhook hoặc logic đối soát.

## Luồng dữ liệu

1. Checkout tải capability thanh toán và dữ liệu cần tính phí từ API hiện có.
2. Frontend yêu cầu phí GHN cho địa chỉ giao hàng.
3. Chỉ khi nhận phí hợp lệ, checkout mới cho phép tạo đơn.
4. PayOS capability quyết định `BANK_TRANSFER` enabled hoặc disabled; lựa chọn vẫn được render.
5. Submit kiểm tra lại phương thức đang enabled trước khi gửi request.
6. Backend tiếp tục validate payment method và shipping fee tại trust boundary.

## Xử lý lỗi

- Lỗi GHN không được chuyển thành phí giả hoặc silently fallback.
- Thông báo GHN không để người dùng hiểu nhầm COD bị hỏng.
- PayOS unavailable không làm biến mất phương thức thanh toán.
- Response lỗi giữ nguyên dữ liệu form và cho phép retry.
- Không log token GHN, PayOS credential hoặc payload chứa secret.

## Kiểm thử và tiêu chí đóng defect

### Backend

- Product create thiếu `galleryImages` thành công và lưu `[]`.
- Sai mật khẩu lần thứ năm trả thông báo khóa.
- Đăng nhập đúng trong thời gian khóa bị từ chối.
- Hết 15 phút reset bộ đếm và mở cửa sổ năm lần thử mới.
- User biến mất giữa lookup và transaction được xử lý an toàn.
- Backend từ chối payment method không khả dụng nếu request bị giả mạo.

### Frontend

- GHN lỗi hoặc thiếu cấu hình khóa submit và hiện thông báo cụ thể.
- GHN retry thành công mở lại submit với phí mới.
- PayOS thiếu cấu hình vẫn render chuyển khoản dưới dạng disabled kèm lý do.
- PayOS khả dụng cho phép chọn và submit `BANK_TRANSFER`.
- Không thể submit phương thức disabled bằng thao tác UI hoặc state cũ.

### Tài liệu

- Chỉ chuyển defect sang Closed sau khi test tương ứng pass.
- BUG-ST-007 và US008 được cập nhật cùng nhau để tránh lệch backlog/code.
- Defect GHN/PayOS ghi rõ chính sách fail-closed và trạng thái capability, không mô tả hành vi đã duyệt là lỗi COD.

## Ngoài phạm vi

- Không thêm schema hoặc migration mới.
- Không thêm dependency.
- Không triển khai fallback phí giao hàng.
- Không sửa payment webhook, refund hoặc reconciliation.
- Không triển khai backlog chức năng mới như US089.
- Không refactor checkout ngoài phần cần thiết cho trạng thái integration.

## Thứ tự thực hiện

1. Viết hoặc xác nhận regression test cho BUG-ST-001 và BUG-ST-007.
2. Chạy test; chỉ sửa implementation nếu test fail.
3. Bổ sung test GHN fail-closed và PayOS disabled.
4. Sửa UX/guard nhỏ nhất để test pass.
5. Chạy backend test, frontend test, lint và build được project cung cấp.
6. Cập nhật defect/backlog dựa trên bằng chứng test cuối cùng.
