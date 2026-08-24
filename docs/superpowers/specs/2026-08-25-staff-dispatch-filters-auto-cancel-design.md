# Staff Dispatch Filters And Automatic Closing Cancellation Design

## Mục tiêu

Bổ sung ba bộ lọc vận hành cho màn hình Staff điều phối giao hàng và tự động hủy đơn `READY` chưa được gán shipper khi đã qua giờ đóng cửa áp dụng cho ngày tạo đơn.

Các quy tắc nghiệp vụ đã duyệt:

- `Ưu tiên`: đơn `READY`, chưa gán shipper, chờ lâu nhất hoặc còn tối đa 30 phút đến giờ đóng cửa.
- `Đơn mới`: đơn `READY`, chưa gán shipper, chuyển sang `READY` trong 15 phút gần nhất.
- `Xem lại`: đơn `DELIVERY_FAILED`, chờ Staff xử lý.
- `Quá ca`: đơn vẫn `READY` và chưa gán shipper sau giờ đóng cửa của ngày tạo đơn sẽ được scheduler tự động hủy.

## Phạm vi

- OpenAPI cho danh sách điều phối có filter và metadata phân loại.
- Backend Servlet, Service và DAO cung cấp một nguồn phân loại server-side.
- Frontend Staff Dispatch hiển thị ba tab, số lượng và trạng thái phù hợp từng tab.
- OrderScheduler hủy đơn quá giờ đóng cửa qua cancellation transaction hiện có.
- Unit, contract, disposable integration và Playwright desktop/mobile.

Không thuộc phạm vi:

- Thay đổi schema database hoặc migration.
- Tự hủy đơn `ASSIGNED`, `PICKED_UP`, `DELIVERY_FAILED` hoặc trạng thái terminal.
- Tối ưu tuyến giao hoặc tự động gán shipper.
- Thay đổi cấu hình giờ mở/đóng cửa hiện có.

## Nguồn chuẩn

- OpenAPI 3.1 là nguồn chuẩn request/response cho endpoint điều phối.
- `ShippingConfig.business_open_time` và `ShippingConfig.business_close_time` là nguồn giờ hoạt động.
- `Orders.order_status`, `created_at`, `ready_at`, `assigned_at` và quan hệ shipper là nguồn trạng thái/mốc thời gian.
- `OrderTransitionService.cancel` là đường hủy chuẩn để hoàn reservation, trả coupon, tạo history và đánh dấu refund.
- `OrderScheduler` là cơ chế chạy nền hiện có, chu kỳ một phút.

## API Contract

Mở rộng endpoint hiện tại:

```http
GET /staff/orders/dispatch?filter=PRIORITY|NEW|REVIEW
```

- `filter` bắt buộc, enum `PRIORITY`, `NEW`, `REVIEW`.
- HTTP `200` trả envelope:

```json
{
  "items": [],
  "counts": {
    "priority": 0,
    "new": 0,
    "review": 0
  },
  "serverTime": "2026-08-25T20:30:00",
  "openTime": "08:00",
  "closeTime": "22:00"
}
```

Mỗi item dùng list DTO Staff hiện tại và bổ sung:

- `readyAt`: nullable ISO local datetime; bắt buộc có giá trị cho item `READY` được phân loại.
- `classification`: enum `PRIORITY`, `NEW`, `REVIEW`.
- `minutesUntilClose`: nullable integer; có thể âm khi request chạy sát scheduler, chỉ áp dụng item `READY`.
- Các field phục hồi giao hàng hiện có tiếp tục xuất hiện cho `REVIEW`.

Lỗi:

- HTTP `400` khi filter thiếu hoặc ngoài enum.
- HTTP `401`, `403` giữ chính sách Staff và checked-in shift hiện tại.

Endpoint `/ready` và `/delivery-failures` hiện có không đổi để tránh ảnh hưởng consumer khác.

## Phân loại Server-Side

Backend lấy `now` một lần cho toàn request bằng business clock hiện có. Phân loại không phụ thuộc clock trình duyệt.

### Priority

Điều kiện chung:

- `orderStatus = READY`.
- Chưa có shipper.
- Chưa qua closing datetime tại thời điểm query.

Một đơn là priority khi:

- Còn tối đa 30 phút đến closing datetime; hoặc
- Không thuộc nhóm `NEW`.

Sắp xếp:

1. `minutesUntilClose` tăng dần.
2. `readyAt` tăng dần.
3. `orderId` tăng dần.

Quy tắc này khiến các đơn READY cũ luôn có mặt trong Priority; đơn mới chỉ vào Priority ngay nếu sắp đóng cửa.

### New

- `orderStatus = READY`.
- Chưa có shipper.
- `readyAt >= now - 15 phút`.
- Chưa qua closing datetime.
- Sắp xếp `readyAt` giảm dần, `orderId` giảm dần.

### Review

- `orderStatus = DELIVERY_FAILED`.
- Sắp xếp `deliveryFailedAt` tăng dần rồi `orderId` tăng dần.
- Không bị automatic closing cancellation.

Counts được tính bằng cùng policy và cùng snapshot time với items. Một đơn mới còn tối đa 30 phút đến giờ đóng cửa có thể được tính trong cả `priority` và `new`; mỗi tab phản ánh đúng tập riêng.

## Closing Datetime

Giờ hoạt động lấy từ `StoreConfigService`.

- `openTime < closeTime`: closing datetime là ngày tạo đơn tại `closeTime`.
- `openTime > closeTime`: ca qua đêm; đơn tạo từ `openTime` trở đi đóng vào ngày kế tiếp tại `closeTime`, đơn tạo trước `closeTime` đóng trong cùng ngày.
- `openTime = closeTime`: cửa hàng mở 24 giờ; không có closing datetime, không tự hủy, `minutesUntilClose = null`.
- Giá trị cấu hình không hợp lệ: fail closed cho mutation, scheduler không hủy và ghi lỗi; API trả lỗi thay vì phân loại bằng giá trị đoán.

## Automatic Cancellation

OrderScheduler tiếp tục chạy mỗi phút. Mỗi tick:

1. Đọc cấu hình giờ hoạt động một lần.
2. Nếu mở 24 giờ, bỏ qua closing cancellation.
3. Query candidate `READY`, chưa có shipper, có `createdAt`, closing datetime nhỏ hơn hoặc bằng `now`.
4. Với từng candidate, gọi cancellation transaction bằng actor `SYSTEM`, reason `Quá giờ đóng cửa chưa được điều phối`.
5. Transaction pessimistic-lock order và kiểm tra lại `READY`, chưa có shipper trước mutation.

Side effects chuẩn:

- Release inventory reservation theo policy hiện có.
- Release coupon redemption.
- Chuyển `orderStatus = CANCELLED`.
- Ghi `cancelledAt`, `cancelledBy = SYSTEM`, reason và status history.
- Nếu payment status là `PAID`, đặt `refundStatus = PENDING`.
- Không tự chuyển payment thành `REFUNDED`.

Scheduler xử lý từng order độc lập; lỗi một order không chặn order khác. Multi-node execution an toàn nhờ pessimistic lock và recheck trạng thái.

## Frontend

Màn hình `DispatchPage.vue` có tablist:

- `Ưu tiên (n)` mặc định.
- `Đơn mới (n)`.
- `Xem lại (n)`.

Hành vi:

- Chuyển tab gọi endpoint với filter tương ứng; stale response không ghi đè tab hiện tại.
- Polling 30 giây giữ nguyên, tải tab hiện tại và counts.
- `Ưu tiên` và `Đơn mới` giữ dropdown shipper và nút `Gán shipper`.
- `Xem lại` không hiển thị gán shipper trực tiếp; hiển thị lý do, số lần giao và CTA `Xử lý lại` tới chi tiết đơn.
- Badge nêu rõ `Sắp đóng cửa`, `Chờ lâu`, `Mới`, `Cần xem lại`; không truyền thông tin chỉ bằng màu.
- Empty, loading và error state riêng theo tab.
- Tabs hỗ trợ ArrowLeft, ArrowRight, Home, End; touch target tối thiểu 44px.

## Data Flow

```text
DispatchPage
  -> staff store/api getDispatchOrders(filter)
  -> StaffOrderServlet GET /dispatch
  -> StaffOrderService
  -> OrdersDAO + StoreConfigService
  -> Dispatch response DTO
```

Automatic cancellation:

```text
AppStartupListener
  -> OrderScheduler mỗi phút
  -> OrdersDAO stale READY candidates
  -> OrderTransitionService.cancel as SYSTEM
  -> inventory/coupon/refund/history side effects
```

## Concurrency Và Lỗi

- Assignment tiếp tục gửi `expectedStatus = READY`.
- Scheduler recheck status và shipper trong transaction.
- Assignment thắng trước: scheduler bỏ qua vì order đã `ASSIGNED` hoặc có shipper.
- Scheduler thắng trước: assignment nhận conflict/canonical reload, order biến khỏi danh sách.
- API config/query failure không được biến thành danh sách rỗng giả.
- Frontend giữ tab và hiển thị retry khi request lỗi.

## Database Gate

Không có migration dự kiến. Tuy nhiên backend phụ thuộc runtime schema và dữ liệu cấu hình.

Trước implementation DB-dependent phải xác nhận:

- `@@SERVERNAME`, `DB_NAME()`, state, compatibility level.
- `Orders` có `ready_at`, `created_at`, `assigned_at`, `order_status`, `shipper_id` theo JPA mapping.
- `ShippingConfig` có hai key giờ hoạt động hợp lệ.
- Runtime schema khớp `database/init.sql`, `database/DB_FastGuy.sql` và migrations liên quan.

SQL Server MCP hiện không kết nối được `:1433`. Không triển khai DAO/scheduler hoặc tuyên bố hoàn tất integration cho đến khi gate này đạt.

## Kiểm thử

- Unit policy: same-day, overnight, 24-hour, invalid config, boundary đúng closing time.
- DAO/service: classification, ordering, overlapping Priority/New counts.
- Scheduler: chỉ hủy `READY` chưa gán; bỏ qua mọi trạng thái khác; lỗi một order không chặn order sau.
- Cancellation: inventory/coupon/history/refund side effects và concurrency assignment-vs-scheduler.
- Servlet serialization và OpenAPI contract tests.
- Frontend source/unit tests cho tab, stale guard, CTA theo classification.
- `mvn test`, `npm test`, `npm run build`, OpenAPI lint.
- Disposable/local integration DB cho automatic cancellation; không mutation retained `FastGuyDB`.
- Playwright desktop/mobile cho ba tab, loading/error/empty, assignment và review navigation; không console error, request chính thành công.

## Tiêu chí hoàn thành

- Staff lọc chính xác theo ba nhóm đã duyệt.
- Counts và item classification do server quyết định.
- Đơn READY chưa gán bị hủy không muộn hơn một scheduler interval sau closing datetime.
- Đơn đã gán, đang giao, giao thất bại và terminal không bị auto-cancel.
- Cancellation giữ đầy đủ inventory, coupon, refund và audit invariants.
- API contract, backend, frontend, disposable integration và Playwright đều đạt.
