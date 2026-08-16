# Delivery Failure Recovery Design

## Mục tiêu

Hoàn thiện xử lý giao hàng thất bại để Shipper ghi nhận đúng sự cố, Staff quyết định giao lại hoặc nhận hàng trả về, và hệ thống giữ nhất quán trạng thái đơn, thanh toán, tồn kho, coupon, loyalty, refund, lịch sử và thông báo.

## Phạm vi

- Shipper báo giao thất bại từ đơn `PICKED_UP` thuộc mình khi đang `CHECKED_IN`.
- Staff điều phối chọn giao lại ngay, hẹn giờ giao lại, đổi Shipper hoặc trả hàng về cửa hàng.
- Mỗi đơn mặc định có tối đa hai lượt giao: lượt đầu và một lượt giao lại.
- Admin có thể mở thêm lượt, bắt buộc nhập lý do và ghi lịch sử audit.
- COD và đơn trả trước được xử lý theo chính sách thanh toán riêng.
- Khách hàng, Staff, Shipper và Admin thấy trạng thái cùng thông tin phù hợp quyền.
- Tái sử dụng state transition, refund queue, inventory reservation, order history và notification hiện có; không thêm dependency.

## Ngoài phạm vi

- Không triển khai GPS hoặc theo dõi Shipper thời gian thực.
- Không tích hợp lịch sử cuộc gọi.
- Không tải ảnh bằng chứng vì chưa có storage flow cho nghiệp vụ giao hàng.
- Không triển khai đối soát COD hoặc nộp tiền COD.
- Không xử lý khiếu nại sau khi đơn đã `DELIVERED`.
- Không thêm bảng delivery attempt riêng; giới hạn hai lượt được quản lý trực tiếp trên đơn và lịch sử trạng thái.

## State machine

```text
PICKED_UP
  ├─ DELIVERED
  └─ DELIVERY_FAILED

DELIVERY_FAILED
  ├─ PICKED_UP            giao lại ngay hoặc đến lịch hẹn
  └─ RETURNED_TO_STORE    kết thúc không giao
```

Quy tắc:

- Shipper là actor duy nhất chuyển `PICKED_UP` sang `DELIVERY_FAILED`.
- `DELIVERY_FAILED` không tự động đổi tồn kho, coupon, payment, refund hoặc loyalty.
- Staff là actor vận hành chọn retry, đổi Shipper hoặc `RETURNED_TO_STORE`.
- Sau lần thất bại đầu tiên, Staff được tạo đúng một lượt giao lại.
- Sau lần thất bại thứ hai, Staff chỉ được chọn `RETURNED_TO_STORE`.
- Admin được mở thêm một lượt qua action override riêng; mỗi override bắt buộc lý do và tạo history.
- Retry không reserve hoặc consume tồn kho lần hai.
- Không cho chuyển trực tiếp `DELIVERY_FAILED` sang `DELIVERED`; retry phải quay lại `PICKED_UP` để giữ audit trail.
- `RETURNED_TO_STORE` là terminal status, tách khỏi `CANCELLED` để không trộn hủy đơn với hàng giao không thành công đã quay về cửa hàng.

## Dữ liệu

Bổ sung vào `Orders`:

- `delivery_attempt_count INT NOT NULL DEFAULT 0`: tăng khi chuyển `PICKED_UP` sang `DELIVERY_FAILED`; giá trị tối đa mặc định là `2`.
- `delivery_failure_code VARCHAR(40) NULL`: mã lỗi gần nhất.
- `delivery_failed_at DATETIME2 NULL`: thời điểm thất bại gần nhất.
- `retry_scheduled_at DATETIME2 NULL`: `NULL` khi giao lại ngay; có giá trị khi hẹn giao lại.
- `returned_to_store_at DATETIME2 NULL`: thời điểm Staff xác nhận hàng đã về cửa hàng.
- `failure_reason` tiếp tục lưu ghi chú nghiệp vụ gần nhất.

Các reason code cố định:

- `CUSTOMER_UNREACHABLE`
- `INVALID_ADDRESS`
- `CUSTOMER_RESCHEDULED`
- `CUSTOMER_REJECTED`
- `SHIPPER_INCIDENT`
- `PRODUCT_INCIDENT`

Migration mở rộng constraints của `Orders` và `OrderStatusHistory` với `DELIVERY_FAILED`, `RETURNED_TO_STORE`; thêm constraints cho attempt count và các trường thời gian. `database/init.sql` và schema cài mới được cập nhật cùng contract.

`OrderStatusHistory` giữ toàn bộ chuỗi thất bại, retry, đổi Shipper, override và trả cửa hàng. Các cột trên `Orders` phục vụ query queue và trạng thái hiện tại, không thay thế history.

## API contract

### Shipper báo thất bại

`POST /api/shipper/orders/{id}/fail`

```json
{
  "expectedStatus": "PICKED_UP",
  "reasonCode": "CUSTOMER_UNREACHABLE",
  "note": "Đã gọi hai lần, khách không nghe máy"
}
```

Quy tắc:

- `reasonCode` phải thuộc sáu mã cho phép.
- `note` bắt buộc, sau trim không rỗng, tối đa 500 ký tự.
- Đơn phải thuộc Shipper hiện tại và Shipper phải active, role `SHIPPER`, đang `CHECKED_IN`.
- Đơn phải ở `PICKED_UP`; stale status trả HTTP 409.
- Thành công tăng `deliveryAttemptCount`, ghi mã/lý do/thời gian, chuyển `DELIVERY_FAILED`, tạo history và thông báo Staff/Admin trong cùng transaction.

### Staff giao lại

`POST /api/staff/orders/{id}/retry-delivery`

```json
{
  "expectedStatus": "DELIVERY_FAILED",
  "shipperId": 12,
  "retryMode": "SCHEDULED",
  "scheduledAt": "2026-08-12T18:30:00+07:00",
  "note": "Khách hẹn nhận lúc 18:30"
}
```

Quy tắc:

- `retryMode` chỉ nhận `IMMEDIATE`, `SCHEDULED`.
- `scheduledAt` bắt buộc với `SCHEDULED`, phải ở tương lai, trong giờ mở cửa và không quá 24 giờ.
- `CUSTOMER_RESCHEDULED` bắt buộc dùng `SCHEDULED`.
- Shipper mới phải active, role `SHIPPER`, đang `CHECKED_IN` tại thời điểm giao lại bắt đầu.
- `IMMEDIATE` chuyển đơn về `PICKED_UP` ngay.
- `SCHEDULED` giữ `DELIVERY_FAILED` đến giờ hẹn; đơn xuất hiện trong queue hẹn. Staff xác nhận bắt đầu giao để chuyển về `PICKED_UP`; hệ thống không tự gán Shipper cũ.
- Không cho retry khi `deliveryAttemptCount >= 2`, trừ khi Admin đã mở thêm lượt.

### Staff xác nhận trả cửa hàng

`POST /api/staff/orders/{id}/return-to-store`

```json
{
  "expectedStatus": "DELIVERY_FAILED",
  "note": "Đã nhận lại hàng tại cửa hàng"
}
```

Quy tắc:

- Chỉ thực hiện sau khi hàng vật lý đã về cửa hàng.
- Chuyển `RETURNED_TO_STORE`, ghi timestamp/history và hoàn tất xử lý tồn kho, coupon, refund trong cùng transaction.
- COD chưa thu giữ `UNPAID`, không tạo refund.
- Đơn online `PAID` tạo `refundStatus=PENDING` để vào queue hoàn tiền hiện có.
- Hàng đã chế biến chuyển reservation `CONSUMED` sang `WASTED`; không tăng tồn bán.
- Coupon được release theo policy đơn không hoàn tất hiện có.
- Không cộng loyalty; không có loyalty để reverse vì đơn chưa từng `DELIVERED`.

### Admin mở thêm lượt

`POST /api/admin/orders/{id}/delivery-attempt-override`

Payload gồm `expectedStatus`, `note`. `note` bắt buộc và history ghi rõ Admin, số lượt cũ, giới hạn mới. Action không tự chuyển trạng thái hoặc gán Shipper.

## Backend design

`OrderTransitionService` tiếp tục là đường mutation trạng thái duy nhất:

1. Validate actor, canonical status, expected status và ownership tại trust boundary.
2. Khóa đơn bằng `PESSIMISTIC_WRITE`.
3. Validate attempt limit, lịch hẹn, Shipper và payment invariants.
4. Áp dụng state mutation cùng side effects.
5. Persist `OrderStatusHistory` và notifications trong cùng transaction.
6. Rollback toàn bộ nếu bất kỳ side effect nào lỗi.

Các invariant:

- `PICKED_UP -> DELIVERY_FAILED` không thay đổi reservation `CONSUMED`.
- Retry không tạo inventory transaction, coupon redemption, payment attempt hoặc loyalty transaction mới.
- `RETURNED_TO_STORE` mới ghi `WASTE`, release coupon và mở refund nếu cần.
- COD failure không ghi `codCollectedAmount`, `codCollectedAt` hoặc `paidAt`.
- Online payment giữ `PAID` trong lúc chờ Staff quyết định.
- Revenue tiếp tục chỉ tính `DELIVERED + PAID`; `DELIVERY_FAILED` và `RETURNED_TO_STORE` không tính doanh thu.
- Mutation retry do mất response không tạo history hoặc side effect trùng.

Queue Staff bổ sung nhóm `DELIVERY_FAILED`, gồm attempt count, reason, note, failed time, retry schedule và Shipper hiện tại. Scheduled retry đến hạn được đánh dấu sẵn sàng xử lý; không dùng background mutation tự động.

## UI/UX design

### Shipper

- Đơn `PICKED_UP` có hai action: `Giao thành công` và `Báo giao thất bại`.
- Form thất bại dùng dialog có select sáu lý do và textarea ghi chú bắt buộc.
- Hiển thị attempt hiện tại và cảnh báo khi đây là lượt cuối.
- Submit khóa khi request đang chạy; conflict giữ nội dung form và tải lại trạng thái.
- Sau khi báo thất bại, đơn chuyển sang read-only chờ Staff xử lý.

### Staff

- Board có queue `Giao thất bại` nổi bật khỏi lịch sử terminal.
- Chi tiết hiển thị reason, note, thời gian, attempt count và Shipper.
- Lần đầu thất bại cho phép: giao ngay, hẹn giờ, đổi Shipper, trả cửa hàng.
- Lần hai thất bại chỉ cho phép trả cửa hàng; UI không hiển thị retry thường.
- `CUSTOMER_RESCHEDULED` mở sẵn chế độ hẹn giờ.
- Xác nhận trả cửa hàng nêu rõ tác động: waste hàng, release coupon, refund online nếu có.

### Khách hàng

- Hiển thị `DELIVERY_FAILED` thành “Giao chưa thành công, cửa hàng đang xử lý”.
- Không lộ ghi chú nội bộ hoặc thông tin sự cố Shipper.
- Khi đã hẹn lại, hiển thị thời gian dự kiến.
- `RETURNED_TO_STORE` hiển thị đơn không giao thành công; online có trạng thái hoàn tiền tương ứng.

### Accessibility

- Dialog có accessible name, focus trap, Escape và restore focus.
- Select/textarea/date-time có label và error liên kết.
- Lỗi submit dùng `role="alert"`; thay đổi trạng thái dùng `aria-live="polite"`.
- Touch target tối thiểu `44x44` px; thao tác chính dùng được bằng bàn phím.

## Error handling

- HTTP 400: JSON sai, field thiếu hoặc kiểu dữ liệu sai.
- HTTP 403: sai role, ngoài ca hoặc đơn không thuộc Shipper.
- HTTP 404: không tìm thấy đơn hoặc Shipper được chọn.
- HTTP 409: `expectedStatus` cũ, vượt giới hạn lượt hoặc trạng thái không cho action.
- HTTP 422: reason/note/lịch hẹn không thỏa quy tắc nghiệp vụ.
- HTTP 500: rollback trạng thái, history và mọi side effect; frontend không tự retry mutation.

## Kiểm thử

### Backend

- Shipper đúng owner và trong ca báo thất bại thành công; actor sai bị chặn.
- Sáu reason code hợp lệ; reason lạ, note rỗng/quá dài bị chặn.
- Failure tăng attempt đúng một lần và không đổi inventory/payment/coupon/loyalty.
- Stale `expectedStatus` trả 409, không tạo history trùng.
- Retry ngay và retry hẹn giờ giữ side effects không đổi.
- Lịch hẹn phải tương lai, trong giờ mở cửa, tối đa 24 giờ.
- Lần thất bại thứ hai chặn Staff retry.
- Admin override bắt buộc note và tạo history.
- Đổi Shipper kiểm tra active role, ca và ownership mới.
- COD trả cửa hàng giữ `UNPAID`, không tạo refund.
- Online `PAID` trả cửa hàng tạo refund pending.
- Return tạo đúng một `WASTE`, release coupon một lần, không award loyalty.
- Transaction rollback đồng thời order, history, inventory và notification khi lỗi.
- Revenue/report không tính trạng thái mới.

### Frontend

- Shipper dialog validation, loading, conflict và focus behavior đúng.
- Staff queue hiển thị failure data và action theo attempt count.
- `CUSTOMER_RESCHEDULED` bắt buộc lịch hẹn.
- Khách nhận label an toàn, không thấy note nội bộ.
- Timeline hiển thị nhánh failure/retry/return, không giả vờ tuyến tính.
- Các status filter, badge, dashboard và history xử lý trạng thái mới đúng.

### Verification

- Backend: `mvn test`, `mvn package -DskipTests`.
- Frontend: test command, lint và build được khai báo trong `package.json`.
- Migration validation và smoke test order/payment/inventory/staff/shipper.
- `git diff --check` sạch.

## Tiêu chí hoàn thành

- Không còn phải dùng `CANCELLED` để biểu diễn giao thất bại hoặc trả cửa hàng.
- Shipper ghi nhận thất bại có reason code và note; không thay đổi tiền hoặc kho sớm.
- Staff xử lý retry/đổi Shipper/hẹn giờ/trả cửa hàng theo giới hạn hai lượt.
- COD, online refund, waste, coupon và loyalty giữ đúng invariant.
- Mọi mutation có expected-status conflict protection và history atomic.
- UI theo role hiển thị trạng thái rõ, an toàn và accessible.
- Backend, frontend, migration và build verification pass.
