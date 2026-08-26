# Ingredient-Based Inventory Phase 1 Design

## Mục tiêu

Chuyển FastGuy từ tồn kho trực tiếp theo món sang tồn kho nguyên liệu và công thức định lượng, đồng thời duy trì tương thích với các biến thể đang quản lý tồn thành phẩm. Sau giai đoạn này, hệ thống tự tính khả năng phục vụ theo nguyên liệu khả dụng, giữ nguyên liệu khi checkout, tiêu hao khi bắt đầu chế biến và duy trì sổ kho có thể truy vết.

## Phạm vi

- Một cửa hàng, một kho logic.
- Quản lý nguyên liệu và thành phẩm bằng đơn vị chuẩn.
- Công thức riêng cho từng `ProductVariant`.
- Ba chế độ tồn kho hoạt động: `INGREDIENT`, `FINISHED_GOOD`, `UNTRACKED`; `SUSPENDED` khóa bán.
- Nhập kho, điều chỉnh kho và ledger theo `InventoryItem`.
- Reserve tại checkout; consume khi đơn chuyển sang `PREPARING`; release khi hủy trước chế biến.
- Tính số suất khả dụng cho admin và trạng thái còn hàng cho khách.
- Chuyển đổi dần từ tồn variant hiện tại sang công thức, không khóa toàn bộ menu.

## Ngoài phạm vi

- Lot, hạn sử dụng, FEFO, supplier và nhiều kho/chi nhánh.
- Kiểm kê định kỳ, variance, giá vốn bình quân và báo cáo food cost.
- Prepared batch, bán thành phẩm và quy trình chế biến nhiều cấp.
- Hoàn kho tự động sau khi nguyên liệu đã được consume.
- Hiển thị số tồn nguyên liệu chính xác cho khách.

## Mô hình dữ liệu

### ProductVariant

Thêm `inventory_mode` với các giá trị:

- `INGREDIENT`: tồn bán được suy ra từ recipe.
- `FINISHED_GOOD`: variant dùng một `InventoryItem` thành phẩm đại diện.
- `UNTRACKED`: không kiểm soát tồn.
- `SUSPENDED`: không được checkout.

`quantity_available` được giữ trong giai đoạn chuyển tiếp để tương thích, nhưng không còn là nguồn sự thật cho `INGREDIENT`. Mutation mới phải đi qua inventory service. Cột chỉ được loại bỏ ở migration sau khi toàn bộ consumer cũ đã chuyển đổi.

### InventoryItem

- `inventory_item_id`
- `name`
- `item_type`: `INGREDIENT` hoặc `FINISHED_GOOD`
- `base_unit`: enum đơn vị chuẩn
- `on_hand_quantity DECIMAL(19,4)`
- `reserved_quantity DECIMAL(19,4)`
- `minimum_quantity DECIMAL(19,4)`
- `active`
- timestamps

Quy tắc:

- `on_hand_quantity >= 0`.
- `reserved_quantity >= 0`.
- `reserved_quantity <= on_hand_quantity`.
- `minimum_quantity >= 0`.
- `available_quantity = on_hand_quantity - reserved_quantity`.

Đơn vị chuẩn giai đoạn 1: `G`, `ML`, `PIECE`. Dữ liệu nhập phải quy đổi về đơn vị chuẩn tại trust boundary; không lưu hỗn hợp kg/g hoặc l/ml trong ledger.

### VariantInventoryItem

Ánh xạ một-một variant `FINISHED_GOOD` với `InventoryItem` thành phẩm:

- `variant_id` unique
- `inventory_item_id` unique

### Recipe

- `recipe_id`
- `variant_id` unique
- `yield_quantity DECIMAL(19,4)`, mặc định `1`
- `active`
- timestamps

Giai đoạn 1 yêu cầu một recipe trực tiếp cho một variant; không hỗ trợ recipe lồng nhau.

### RecipeItem

- `recipe_item_id`
- `recipe_id`
- `inventory_item_id`
- `quantity DECIMAL(19,4)`

`quantity > 0`; unique theo `recipe_id + inventory_item_id`. `InventoryItem.base_unit` là đơn vị của quantity.

### InventoryReservation và dòng giữ kho

Reservation cấp đơn giữ trạng thái và các dòng nguyên liệu:

- `InventoryReservation`: `reservation_id`, `order_id` unique, `status`, timestamps.
- `InventoryReservationItem`: `reservation_id`, `inventory_item_id`, `quantity`; unique theo reservation và item.

Trạng thái: `RESERVED`, `CONSUMED`, `RELEASED`. Transition chỉ hợp lệ từ `RESERVED` sang `CONSUMED` hoặc `RELEASED`.

### InventoryTransaction

Ledger mới tham chiếu `InventoryItem`, không tham chiếu trực tiếp variant:

- `inventory_transaction_id`
- `inventory_item_id`
- `order_id` nullable
- `transaction_type`
- `quantity DECIMAL(19,4)` có dấu
- `quantity_before`
- `quantity_after`
- `reference_type`, `reference_id`
- `reason_code`, `note`
- `created_by`, `created_at`

Loại giai đoạn 1: `RECEIPT`, `RESERVE`, `RELEASE`, `CONSUME`, `ADJUSTMENT`. `RESERVE` và `RELEASE` ghi thay đổi reserved; `RECEIPT`, `CONSUME`, `ADJUSTMENT` ghi thay đổi on-hand. Mọi mutation và ledger phải commit trong cùng DB transaction.

## Khả năng phục vụ

Với variant `INGREDIENT`:

```text
servings = FLOOR(MIN((item.onHand - item.reserved) / recipeItem.quantity) * recipe.yieldQuantity)
```

Kết quả bị chặn tại `0`. Recipe thiếu, inactive, không có item hoặc tham chiếu item inactive làm variant không khả dụng; không suy luận là không giới hạn.

Với `FINISHED_GOOD`, số suất bằng `FLOOR(available_quantity)`. `UNTRACKED` không trả con số hữu hạn. `SUSPENDED` luôn bằng `0`.

Giỏ có nhiều món phải gom nhu cầu theo `InventoryItem` trước khi kiểm tra; không kiểm tra từng dòng độc lập vì nhiều món có thể dùng chung nguyên liệu.

## Vòng đời đơn hàng

### Checkout

1. Resolve variant và inventory mode.
2. Từ số lượng món và recipe, gom tổng nhu cầu từng `InventoryItem`.
3. Khóa item bằng `PESSIMISTIC_WRITE` theo ID tăng dần để giảm deadlock.
4. Kiểm tra toàn bộ available quantity.
5. Tăng `reserved_quantity`, tạo reservation/items và ledger `RESERVE`.
6. Persist order và reservation atomically.

Không đủ tồn trả HTTP 409; không tạo order, reservation hoặc ledger một phần. Unique order reservation và kiểm tra trạng thái bảo đảm retry không giữ hai lần.

### Bắt đầu chế biến

Khi đơn chuyển sang `PREPARING`:

1. Khóa reservation và item theo thứ tự ổn định.
2. Chỉ nhận reservation `RESERVED`.
3. Với mỗi dòng: giảm `reserved_quantity`, giảm `on_hand_quantity` cùng lượng.
4. Chuyển reservation thành `CONSUMED`.
5. Ghi ledger `CONSUME` và đổi trạng thái đơn atomically.

### Hủy

- Trước `PREPARING`: giảm reserved, chuyển `RELEASED`, ghi `RELEASE`; on-hand không đổi.
- Sau `CONSUMED`: không hoàn kho tự động. Hủy đơn chỉ xử lý nghiệp vụ đơn; hao hụt sau chế biến thuộc giai đoạn 2.
- Transition lặp trả kết quả idempotent hoặc HTTP 409 theo contract; không nhân đôi ledger.

## API contract

OpenAPI 3.1 phải được cập nhật trước implementation.

### Admin inventory items

- `GET /api/admin/inventory/items`
- `POST /api/admin/inventory/items`
- `GET /api/admin/inventory/items/{itemId}`
- `PUT /api/admin/inventory/items/{itemId}`

Response gồm on-hand, reserved, available, minimum, base unit và trạng thái. Không cho đổi base unit sau khi item có recipe hoặc ledger, tránh đổi nghĩa dữ liệu lịch sử.

### Receipt và adjustment

- `POST /api/admin/inventory/transactions/receipts`
- `POST /api/admin/inventory/transactions/adjustments`
- `GET /api/admin/inventory/transactions`

Mutation gửi `inventoryItemId`, quantity theo base unit, `expectedOnHandQuantity`, reason/note phù hợp. Conflict stale quantity trả HTTP 409 và current quantity; frontend không tự retry.

### Recipe

- `GET /api/admin/product-variants/{variantId}/recipe`
- `PUT /api/admin/product-variants/{variantId}/recipe`
- `GET /api/admin/product-variants/{variantId}/availability`

`PUT` thay toàn bộ recipe trong một transaction. Chỉ cho bật `INGREDIENT` khi recipe hợp lệ và có ít nhất một active item.

### Customer availability

Product/menu response bổ sung trạng thái derived:

- `availabilityStatus`: `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`, `UNTRACKED`, `SUSPENDED`
- `remainingServings` chỉ trả khi từ 1 đến 3.

Mapping: `0` là `OUT_OF_STOCK`; `1..3` là `LOW_STOCK`; từ `4` là `IN_STOCK`. Không trả nguyên liệu giới hạn hoặc số lượng nguyên liệu.

## Giao diện quản trị

### Tổng quan kho

Hiển thị số nguyên liệu, số item dưới minimum, số món tạm hết và giao dịch gần đây. Không hiển thị giá trị tồn kho hoặc chi phí trong giai đoạn 1.

### Nguyên liệu

Bảng gồm tên, loại, on-hand, reserved, available, đơn vị, minimum và trạng thái. Hành động: tạo item, nhập kho, điều chỉnh, xem ledger.

### Công thức định lượng

Chọn variant, quản lý recipe items, hiển thị lượng dùng theo base unit, số suất khả dụng và item giới hạn. Product editor chỉ chọn inventory mode và liên kết sang trang recipe; không nhúng editor BOM phức tạp.

### Sổ kho

Bộ lọc chuyển từ product/variant sang inventory item, order, transaction type và khoảng ngày. Ledger giải thích thay đổi on-hand hoặc reserved.

### Website khách

- `IN_STOCK`, `UNTRACKED`: “Còn hàng”.
- `LOW_STOCK`: “Chỉ còn N phần”.
- `OUT_OF_STOCK`, `SUSPENDED`: “Tạm hết”, khóa CTA.

Frontend vẫn phải xử lý HTTP 409 tại checkout vì trạng thái menu chỉ là snapshot.

## Migration và rollout

1. Thêm schema mới và constraints; không xóa bảng/cột cũ.
2. Tạo một `InventoryItem` loại `FINISHED_GOOD` cho mỗi variant có `quantity_available` khác null; copy số lượng sang on-hand và tạo mapping.
3. Backfill `inventory_mode`: variant ngừng bán thành `SUSPENDED`; variant có quantity thành `FINISHED_GOOD`; còn lại thành `UNTRACKED`.
4. Deploy backend dual-mode và API contract.
5. Deploy admin UI và customer availability.
6. Admin khai báo nguyên liệu/recipe; chỉ sau validation mới chuyển từng variant sang `INGREDIENT`.
7. Khi chuyển mode, tồn thành phẩm cũ phải bằng 0 hoặc admin xác nhận xử lý qua adjustment; không âm thầm bỏ tồn.

Migration cần preflight catalog, validator sau migration và integration test trên DB disposable theo runbook SQL Server. Không chạy trên retained data khi chưa có recovery plan và xác nhận riêng.

## Lỗi và bảo toàn dữ liệu

- HTTP 400: quantity, unit, recipe, mode hoặc transition không hợp lệ.
- HTTP 404: item/variant/order không tồn tại.
- HTTP 409: thiếu available quantity, stale expected quantity, recipe chưa sẵn sàng hoặc transition xung đột.
- HTTP 500: rollback toàn bộ order/inventory mutation.
- Decimal dùng `BigDecimal`; không dùng `double` cho số lượng.
- Khóa item theo ID ổn định; không cho tồn hoặc reserved âm.
- Không log payload có credential; audit lưu actor, reference và reason.

## Kiểm thử

### Database

- Migration backfill đúng mode, item và mapping.
- Constraints chặn quantity âm, recipe quantity không dương, duplicate recipe item/reservation item.
- Validator xác nhận schema và dữ liệu backfill.

### Backend

- Tính suất đúng theo item giới hạn và decimal quantity.
- Mixed cart gom chung nguyên liệu đúng.
- Hai checkout đồng thời không oversell.
- Checkout rollback sạch khi một item thiếu.
- Reserve, consume, release đúng quantity và ledger.
- Retry/transition lặp không nhân đôi reservation hoặc ledger.
- Hủy sau consume không hoàn kho.
- `FINISHED_GOOD`, `UNTRACKED`, `SUSPENDED` hoạt động đúng.
- Recipe thiếu hoặc inactive khóa bán.

### Contract và frontend

- OpenAPI lint và contract tests pass.
- CRUD item/recipe validate đúng trust boundary.
- Admin hiển thị on-hand/reserved/available và conflict đúng.
- Menu map availability đúng; không lộ tồn nguyên liệu.
- Checkout HTTP 409 cập nhật trải nghiệm hết hàng.

### Verification

- Backend: test liên quan, `mvn test`, integration test DB disposable.
- Frontend: test liên quan, `npm test`, `npm run build`.
- OpenAPI lint và contract tests.
- Playwright desktop/mobile cho recipe admin, menu availability và checkout cạnh tranh.
- Không có console error; request chính thành công.

## Tiêu chí hoàn thành

- Món `INGREDIENT` không còn phụ thuộc `ProductVariant.quantityAvailable`.
- Khả năng phục vụ được suy ra từ available nguyên liệu và recipe.
- Checkout đồng thời không oversell.
- Reservation và order lifecycle nhất quán, idempotent, audit được.
- Variant hiện tại tiếp tục bán qua `FINISHED_GOOD` trong thời gian chuyển đổi.
- Admin quản lý nguyên liệu, công thức, nhập/điều chỉnh và ledger trên các API đã contract hóa.
- Toàn bộ migration, backend, frontend, contract và E2E checks bắt buộc pass trước rollout.
