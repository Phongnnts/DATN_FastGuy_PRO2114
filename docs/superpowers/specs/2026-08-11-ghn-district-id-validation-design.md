# BUG-UT-001 GHN District ID Validation Design

## Mục tiêu

Đảm bảo API tạo hoặc cập nhật địa chỉ trả HTTP 400 thay vì HTTP 500 khi `ghnDistrictId` không phải số nguyên dương JSON.

## Phạm vi

- Áp dụng tại trust boundary xử lý payload địa chỉ.
- `ghnDistrictId` hợp lệ khi là JSON number, biểu diễn số nguyên dương.
- Thiếu, `null`, chuỗi số, chuỗi không phải số, số thực, số không và số âm đều không hợp lệ.
- Giữ nguyên luồng lưu địa chỉ khi payload hợp lệ.
- Không thêm DTO, dependency, schema hoặc migration.
- Không refactor validation các trường không liên quan.

## Thiết kế

### Validation

`AddressValidator` chịu trách nhiệm từ chối `ghnDistrictId` trước khi servlet ép kiểu hoặc gọi service.

Điều kiện hợp lệ:

1. Giá trị là `Number`.
2. Giá trị hữu hạn và không có phần thập phân.
3. Giá trị lớn hơn `0`.
4. Giá trị nằm trong miền `int` mà tầng persistence hiện dùng.

Giữ thông báo ổn định `Quan/huyen GHN khong hop le` cho mọi trường hợp không hợp lệ.

### HTTP contract

`AddressServlet` tiếp tục dùng kết quả từ `AddressValidator` để trả HTTP 400. Payload lỗi không được đi tiếp tới bước chuyển đổi dữ liệu hoặc service, nên không phát sinh `NumberFormatException`, lỗi thu hẹp số hoặc HTTP 500.

Không thay đổi response của payload hợp lệ.

## Kiểm thử

### Validator regression

Kiểm tra `ghnDistrictId` với các trường hợp:

- Số nguyên dương hợp lệ trả `null`.
- Thiếu hoặc `null` trả lỗi.
- `"123"` và `"abc"` trả lỗi.
- `1.5`, `0`, số âm và số vượt `Integer.MAX_VALUE` trả lỗi.

### Endpoint regression

Gửi payload địa chỉ hợp lệ ngoại trừ `ghnDistrictId` sai kiểu và xác nhận:

- HTTP status là 400.
- Response chứa thông báo `Quan/huyen GHN khong hop le`.
- Service hoặc persistence không được gọi.
- Không trả HTTP 500.

## Tiêu chí đóng defect

- Regression test cho `BUG-UT-001` pass.
- Toàn bộ backend test pass.
- Backend package pass.
- `docs/unit-test-defect.md` chỉ chuyển `BUG-UT-001` sang `Close` sau khi các kiểm tra trên pass.

## Ngoài phạm vi

- Không chấp nhận chuỗi số để tương thích ngược.
- Không thay đổi validation `ghnProvinceId` hoặc `ghnWardCode` trừ khi test chứng minh cùng nguyên nhân trực tiếp làm `BUG-UT-001` không thể đóng.
- Không thay đổi frontend vì backend phải tự bảo vệ trust boundary.
