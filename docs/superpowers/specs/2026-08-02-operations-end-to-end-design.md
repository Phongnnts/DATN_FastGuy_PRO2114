# FastGuy Operations End-to-End Design

## Mục tiêu

Hoàn thiện luồng vận hành từ bếp đến giao hàng bằng UI chuyên nghiệp và API nhỏ, giữ nguyên lifecycle `PENDING → CONFIRMED → PREPARING → READY → ASSIGNED → PICKED_UP → DELIVERED`. Không thêm GPS, proof ảnh/OTP, auto-dispatch, kitchen station hoặc schema mới.

## Phạm vi

### Staff Kitchen Queue

- Giữ `/staff/orders` và alias `/staff/kitchen`.
- Hiển thị tab `PENDING`, `CONFIRMED`, `PREPARING`, `READY` theo query URL.
- Danh sách ưu tiên đơn cũ trước, có thời gian chờ, khách, điện thoại, tổng số lượng món và thanh toán.
- Search theo mã đơn, tên khách và số điện thoại.
- Hiển thị modifier trong ticket/card khi dữ liệu có sẵn.
- Poll nền 30 giây, không chồng request; giữ dữ liệu cũ khi refresh lỗi và báo trạng thái dữ liệu cũ.
- Loading, empty, error và retry rõ ràng; responsive table trên desktop, card trên mobile.

### Staff Order Detail

- Hiển thị điện thoại, modifier, service fee, discount, refund và internal notes.
- Dùng `allowedActions` từ backend làm nguồn hành động chính; fallback trạng thái chỉ để tương thích.
- Phân biệt “không có Shipper khả dụng” và “không tải được danh sách Shipper”.
- Có refresh/poll an toàn; khóa nút trong mutation; tải lại dữ liệu chuẩn sau mutation.
- Giữ print hiện tại; không xây invoice domain mới.

### Dispatch

- `/staff/dispatch` hiển thị đơn `READY` chưa gán và Shipper đang `ACTIVE`, `CHECKED_IN`.
- Backend trả thêm `activeOrderCount`; workload tính trên `ASSIGNED`, `PICKED_UP`.
- UI hiển thị tên, phone, workload; chọn Shipper và gán từng đơn.
- Poll 30 giây, retry riêng orders/shippers, xóa selection không còn hợp lệ.
- Backend revalidate role, active state và checked-in shift trong transaction khi gán.
- Không giới hạn capacity cứng và không auto-assign trong phase này.

### Shipper Dashboard và Active Deliveries

- Dashboard hiển thị active count, waiting pickup, delivering, delivered today và next active order.
- Dùng dashboard endpoint và active-orders endpoint hiện có.
- Có refresh/error/retry; card active dẫn tới detail.
- `/shipper/orders` dùng active endpoint thay vì tải toàn lịch sử.
- List hiển thị payment, item count, assigned/picked-up timestamps và sắp xếp theo thời điểm nghiệp vụ.

### Shipper Detail và History

- Detail hiển thị modifiers, timeline, pickup/delivery timestamps, payment/COD.
- Chống double submit, validate COD client-side, refetch sau pickup/deliver.
- Google Maps deep-link giữ theo địa chỉ text; không lưu vị trí.
- History gồm `DELIVERED` và `CANCELLED`, có date/search cơ bản.
- Shipper được xem detail đơn lịch sử của chính mình ngoài ca; mutation vẫn bắt buộc checked-in.
- Backend ownership và active-account check vẫn áp dụng.

## API Contract

### Staff available shippers

`GET /api/staff/orders/shippers`

```json
[
  {
    "id": 3,
    "fullName": "Shipper",
    "phone": "0900000000",
    "activeOrderCount": 2
  }
]
```

### Shipper list item

Các endpoint `/shipper/orders/active`, `/shipper/orders/mine`, `/shipper/orders/history` trả tối thiểu:

- `orderId`, `orderCode`, `status`
- `customerName`, `customerPhone`, `customerAddress`
- `finalAmount`, `shippingFee`
- `paymentMethod`, `paymentStatus`
- `itemCount`
- `assignedAt`, `pickedUpAt`, `deliveredAt`, `createdAt`

History gồm `DELIVERED` và `CANCELLED` thuộc Shipper.

### Shipper detail

Thêm:

- `items[].modifiers`
- `statusHistory`
- `assignedAt`, `pickedUpAt`, `deliveredAt`
- `allowedActions`

GET detail của đơn lịch sử thuộc Shipper không yêu cầu checked-in. `pickup` và `deliver` vẫn yêu cầu checked-in.

## Backend thay đổi

- `OrdersDAO`: query active count theo Shipper; history gồm delivered/cancelled; ordering deterministic.
- `StaffOrderService`/`StaffOrderServlet`: DTO workload.
- `ShipperService`/`ShipperServlet`: list DTO đầy đủ, detail modifier/history, read policy ngoài ca cho terminal orders.
- Không thêm bảng hoặc migration.
- Không thay đổi canonical lifecycle.

## Frontend thay đổi

- `staff` store map `itemCount`, `customerPhone`, modifiers, allowed actions và request states.
- `shipper` store map list/detail contract đầy đủ và giữ error state.
- Kitchen page đồng bộ tab vào URL và poll nền an toàn.
- Staff detail dùng backend actions và render dữ liệu đã có.
- Dispatch hiển thị workload, polling và lỗi tách biệt.
- Shipper pages dùng `OrderStatusBadge`, `OrderTimeline`, loading/empty/error states.
- Layout active navigation nhận cả detail route và có retry shift state.

## Error và concurrency

- Mutation khóa nút cho đến khi hoàn thành.
- Sau mutation luôn refetch resource.
- Poll không chạy khi request trước chưa xong.
- Refresh nền lỗi không xóa dữ liệu đã hiển thị.
- Assignment lỗi do trạng thái/ca thay đổi hiển thị conflict và reload board.
- Không tin state frontend cho role, shift, ownership hoặc lifecycle.

## Kiểm thử

### Backend

- Available Shipper chỉ active + checked-in và trả workload đúng.
- Assignment revalidation từ chối inactive/out-of-shift.
- History trả delivered/cancelled của đúng Shipper.
- Historical detail ngoài ca chỉ đọc được khi thuộc Shipper.
- Pickup/deliver ngoài ca vẫn bị từ chối.
- DTO list/detail có field mới và modifier/history.

### Frontend

- Route/tab query ổn định.
- Kitchen dùng `itemCount`, search 3 trường, render modifiers.
- Dispatch workload và retry states.
- Shipper active/history/detail map đúng field.
- Poll cleanup và no-overlap policy.
- Build, Node tests, Maven verify và `git diff --check` phải đạt.

## Không thuộc phạm vi

- GPS/live map, route optimization.
- Offer/accept/reject và auto-dispatch.
- Proof ảnh, OTP, chữ ký.
- Delivery failure/return-to-store state mới.
- COD settlement/earnings.
- Kitchen station, item-level preparation và printer integration.
- Schema/migration mới.
