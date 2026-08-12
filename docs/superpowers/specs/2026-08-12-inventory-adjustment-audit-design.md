# Inventory Adjustment and Audit Design

## Mục tiêu

Hoàn thiện điều chỉnh tồn kho thủ công theo biến thể với ba thao tác tăng, giảm và đặt số lượng mới; ngăn ghi đè dữ liệu cũ; bảo đảm mọi thay đổi tồn kho từ giao diện quản trị đều có ledger audit.

## Phạm vi

- Tồn kho tiếp tục quản lý tại `ProductVariant.quantityAvailable`.
- Trang tồn kho hỗ trợ `INCREASE`, `DECREASE`, `SET` trong một modal ba tab.
- API dùng `expectedQuantity` để phát hiện dữ liệu cũ.
- Mọi cập nhật `quantityAvailable` từ trang tồn kho hoặc editor sản phẩm/biến thể đi qua inventory service và tạo audit khi giá trị thay đổi.
- Chuyển giữa không quản lý tồn kho (`null`) và có quản lý chỉ thực hiện trong editor biến thể.
- Tái sử dụng `InventoryTransaction` và endpoint hiện có; không thêm dependency.

## Ngoài phạm vi

- Không thay đổi reservation, consume, release hoặc waste workflow.
- Không thêm cấu hình ngưỡng sắp hết hàng.
- Không triển khai import hàng loạt, barcode hoặc quản lý kho nhiều chi nhánh.
- Không thay đổi transaction type `RETURN`.

## API contract

### Điều chỉnh tồn kho

Giữ endpoint:

`POST /api/admin/inventory/transactions/adjustments`

Payload:

```json
{
  "variantId": 12,
  "operation": "INCREASE",
  "quantity": 5,
  "expectedQuantity": 20,
  "reasonCode": "STOCK_COUNT",
  "note": "Kiểm kê cuối ca"
}
```

Quy tắc:

- `operation` chỉ nhận `INCREASE`, `DECREASE`, `SET`.
- `quantity` là số nguyên dương với `INCREASE` và `DECREASE`; là số nguyên không âm với `SET`.
- `expectedQuantity` là số nguyên không âm và phải bằng tồn kho sau khi variant được khóa.
- `reasonCode` chỉ nhận `STOCK_COUNT`, `DAMAGE`, `EXPIRED`, `OTHER`.
- `note` tối đa 500 ký tự; bắt buộc và không chỉ chứa khoảng trắng khi `reasonCode` là `OTHER`.
- Variant không tồn tại, không quản lý tồn kho hoặc payload sai trả HTTP 400.
- `expectedQuantity` khác tồn hiện tại trả HTTP 409, kèm `variantId` và `currentQuantity`; không đổi stock hoặc ledger.
- Kết quả không đổi trả HTTP 200 với `changed: false`; không tạo ledger.
- Kết quả có đổi trả HTTP 200 với `changed: true`, `before`, `after`.

### Chuyển chế độ quản lý tồn kho

Editor biến thể tiếp tục là nơi duy nhất chuyển `quantityAvailable` giữa `null` và số nguyên không âm. Mutation phải gửi tồn kho kỳ vọng, lý do và ghi chú theo cùng quy tắc audit.

- `null` sang số: bật quản lý tồn kho, ghi before `null`, after là số mới.
- Số sang `null`: tắt quản lý tồn kho, ghi before là số cũ, after `null`.
- Số sang số: dùng cùng inventory service và conflict contract.
- Thay đổi thông tin variant không đổi tồn kho không tạo inventory ledger.

Ledger dùng loại `ADJUSTMENT`; before/after biểu diễn rõ chuyển trạng thái. `quantity` giữ số dương để thỏa constraint hiện tại: dùng độ lệch tuyệt đối khi hai đầu là số, và dùng giá trị số ở đầu được quản lý khi một đầu là `null`. Trường hợp `null` sang `0` hoặc `0` sang `null` dùng `quantity = 1` làm audit marker; before/after vẫn là nguồn sự thật của thay đổi chế độ.

## Backend design

`InventoryAdjustmentService` là đường ghi tồn kho duy nhất cho mutation quản trị:

1. Validate operation, quantity, reason và note tại trust boundary.
2. Mở transaction và khóa variant bằng `PESSIMISTIC_WRITE`.
3. So sánh giá trị hiện tại với `expectedQuantity`, gồm cả `null` khi editor đổi chế độ.
4. Tính tồn mới bằng phép toán chính xác, chặn số âm và integer overflow.
5. Nếu không đổi, commit/rollback sạch mà không persist ledger.
6. Nếu có đổi, cập nhật variant và persist `ADJUSTMENT` cùng transaction.
7. Trả conflict riêng để servlet map HTTP 409; lỗi validation map HTTP 400.

Các servlet cập nhật product/variant không được set `quantityAvailable` trực tiếp. Chúng lưu metadata và gọi inventory service cho thay đổi stock trong cùng ranh giới transaction phù hợp; nếu không thể bảo đảm atomicity giữa metadata và stock, mutation bị từ chối thay vì lưu một phần.

## UI/UX design

### Trang tồn kho

Nút `Điều chỉnh` mở một modal với ba tab:

- `Tăng`: nhập số lượng cộng thêm.
- `Giảm`: nhập số lượng trừ đi.
- `Đặt mới`: nhập tồn kho tuyệt đối.

Modal hiển thị sản phẩm, biến thể, SKU, tồn hiện tại và tồn dự kiến theo thời gian thực. Submit bị khóa khi input sai hoặc kết quả không đổi. Lý do bắt buộc; chọn `Khác` làm ghi chú bắt buộc.

Khi API trả HTTP 409:

- Giữ modal mở.
- Cập nhật tồn hiện tại từ response.
- Xóa xác nhận submit cũ và yêu cầu quản trị viên kiểm tra lại tồn dự kiến.
- Không tự gửi lại request.

Thành công cập nhật row bằng dữ liệu response và đồng bộ lại danh sách; toast nêu rõ thao tác. No-op chỉ hiển thị thông báo không có thay đổi.

### Editor biến thể

Editor hiển thị rõ toggle quản lý tồn kho. Bật quản lý yêu cầu số lượng ban đầu; tắt quản lý yêu cầu xác nhận. Mọi thay đổi stock yêu cầu lý do và ghi chú theo cùng quy tắc. Conflict giữ form và yêu cầu xác nhận lại.

### Accessibility

- Tab dùng semantics `tablist`, `tab`, `tabpanel` và hỗ trợ bàn phím.
- Modal có accessible name, focus trap, Escape để đóng khi không submit, và restore focus.
- Input có label liên kết; lỗi dùng `role="alert"` hoặc `aria-live`.
- Tồn dự kiến được công bố bằng vùng `aria-live="polite"`.

## Error handling

- HTTP 400: payload, reason, note, variant hoặc phép tính không hợp lệ.
- HTTP 409: tồn kho kỳ vọng đã cũ; response chứa tồn mới nhất.
- HTTP 500: lỗi hệ thống; transaction rollback, không lưu stock hoặc ledger một phần.
- Frontend không tự retry mutation.

## Kiểm thử

### Backend

- `INCREASE`, `DECREASE`, `SET` tính đúng before/after.
- Chặn giảm vượt tồn, số âm, số thực, overflow và operation không hợp lệ.
- `OTHER` thiếu note bị từ chối; reason ngoài whitelist bị từ chối.
- `expectedQuantity` lệch tạo HTTP 409 và không ghi dữ liệu.
- No-op trả `changed: false`, không tạo ledger quantity `0`.
- Variant unmanaged bị từ chối tại adjustment endpoint.
- Bật/tắt quản lý qua editor tạo ledger với before/after đúng.
- Product/variant editor không còn đường set stock không audit.
- Stock update và ledger persist rollback cùng nhau khi lỗi.

### Frontend

- Ba tab và tồn dự kiến hoạt động đúng.
- Validation theo từng operation và quy tắc `OTHER`.
- Submit gửi `expectedQuantity` từ snapshot đang hiển thị.
- HTTP 409 cập nhật snapshot, giữ modal, không retry.
- Editor dùng audited flow cho mọi thay đổi stock.
- Modal/tab đạt keyboard và accessible-name contract.

### Verification

- Backend: `mvn test`, `mvn package -DskipTests`.
- Frontend: test command, lint và build được khai báo trong `package.json`.
- `git diff --check` sạch.

## Tiêu chí hoàn thành

- Admin tăng, giảm hoặc đặt tồn mới mà không ghi đè thay đổi đồng thời.
- Mọi mutation quản trị của `quantityAvailable` có audit hoặc là no-op không tạo ledger.
- Không còn lỗi DB do adjustment quantity bằng `0`.
- Conflict được trình bày rõ và yêu cầu xác nhận lại.
- Backend và frontend verification pass.
