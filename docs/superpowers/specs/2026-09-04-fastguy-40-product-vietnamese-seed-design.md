# FastGuy 40-Product Vietnamese Presentation Seed Design

## Objective

Viết lại bộ dữ liệu trình diễn do seed sở hữu để có 40 sản phẩm, nguyên liệu và dữ liệu kho tự nhiên, đầy đủ dấu tiếng Việt, không chứa chữ `DEMO` trong mã kỹ thuật hoặc nội dung hiển thị.

## Scope

- Chính xác 7 danh mục: `Bánh mì`, `Burger`, `Pizza`, `Cơm`, `Mì`, `Gà rán`, `Nước uống`.
- Phân bổ 40 món theo thứ tự trên: `6-6-6-6-6-5-5`.
- Mỗi sản phẩm có đúng một biến thể/kích cỡ `Tiêu chuẩn`.
- Mỗi biến thể dùng chế độ tồn kho `INGREDIENT`, có một công thức hoạt động và đúng 2 dòng nguyên liệu.
- Viết lại dữ liệu nguyên liệu, phiếu nhập hàng, giao dịch nhập hàng, kiểm kê và giao dịch điều chỉnh thuộc bộ seed.
- Cập nhật đơn hàng seed để tham chiếu hợp lệ đến 40 sản phẩm mới và giữ các trạng thái vận hành/báo cáo hiện có.

## Non-goals

- Không đổi schema, entity, DAO, service, API, OpenAPI hoặc frontend.
- Không thêm danh mục ngoài 7 danh mục đã duyệt.
- Không chạy seed trên `FastGuyDB` trong phạm vi triển khai này.
- Không sửa hoặc xóa dữ liệu không thuộc quyền sở hữu của seed.

## Ownership and identifiers

Dữ liệu seed mới dùng prefix kỹ thuật không chứa `DEMO`:

- Danh mục: mô tả ownership `FG-CATEGORY-01` đến `FG-CATEGORY-07`.
- SKU: `FG-SKU-001` đến `FG-SKU-040`.
- Nguyên liệu: `FG-ING-001` trở đi.
- Phiếu nhập: `FG-REC-001` trở đi.
- Đơn hàng/idempotency/thanh toán/tham chiếu: prefix `FG-*` tương ứng.

Seed phải nhận diện cả ownership cũ `DEMO-PRES-*` trong bước dọn dữ liệu để nâng cấp an toàn, nhưng chỉ tạo dữ liệu mới bằng `FG-*`. Việc xóa tuân theo thứ tự khóa ngoại hiện có và chỉ tác động các hàng được xác định bởi marker cũ hoặc mới.

## Product dataset

40 món dùng tên và mô tả tiếng Việt tự nhiên, đầy đủ dấu. Mỗi danh mục có món khác nhau, giá hợp lý và không dùng từ `demo`, `trình diễn`, `mẫu` trong trường hiển thị. Món nước uống vẫn có công thức 2 nguyên liệu, ví dụ thành phần đồ uống và đá/nước đường phù hợp.

Mỗi biến thể:

- `variant_name = N'Tiêu chuẩn'`;
- SKU `FG-SKU-nnn`;
- trạng thái phù hợp với sản phẩm;
- `inventory_mode = 'INGREDIENT'`;
- thông số vận chuyển hợp lệ như seed hiện tại.

## Ingredients and recipes

Danh sách nguyên liệu được mở rộng đủ để tạo công thức có ý nghĩa cho 40 món. Tên nguyên liệu dùng tiếng Việt đầy đủ dấu; mã dùng `FG-ING-nnn`; đơn vị chỉ dùng enum schema hiện có `G`, `ML`, `PIECE`.

Công thức được khai báo tường minh theo sản phẩm thay vì ghép tuần tự bằng modulo. Mỗi món có đúng 2 nguyên liệu khác nhau, số lượng dương và phù hợp đơn vị. Tổng số công thức là 40; tổng số dòng `RecipeItem` là 80.

## Goods receipts and inventory

- Tạo ít nhất một phiếu nhập đã duyệt mang mã `FG-REC-*`.
- Phiếu nhập có dòng cho toàn bộ nguyên liệu seed, giá và hệ số quy đổi hợp lệ.
- Tạo giao dịch `RECEIPT` khớp lượng trước/sau và giá trị.
- Giữ một số giao dịch hao hụt có lý do tiếng Việt để dashboard kho có dữ liệu.

## Stock count

- Tạo một phiếu kiểm kê đã duyệt cho toàn bộ nguyên liệu seed.
- Dòng kiểm kê có tồn lý thuyết, tồn thực tế, chênh lệch và giá trị nhất quán.
- Có ít nhất một điều chỉnh kiểm kê hợp lệ để sổ kho và báo cáo có bằng chứng vận hành.
- Ghi chú/lý do hiển thị bằng tiếng Việt đầy đủ dấu.

## Idempotency and safety

- Repository seed và validator tiếp tục hard-lock chính xác `DB_NAME() = 'DemoDatabase'`.
- Seed yêu cầu session context opt-in hiện có, chạy trong transaction với `XACT_ABORT ON` và rollback toàn bộ khi lỗi.
- Chạy lặp lại phải cho cùng số lượng và không tạo trùng.
- Không gọi stored procedure.
- Không chạy `database/init.sql`.
- Không ghi `FastGuyDB` nếu chưa có phê duyệt riêng, backup và bằng chứng restore theo policy.

## Validation

Validator phải chứng minh:

- 7 danh mục đúng tên và phân bổ `6-6-6-6-6-5-5`.
- 40 sản phẩm, 40 biến thể, 40 công thức hoạt động và 80 dòng công thức.
- Mỗi sản phẩm có đúng một biến thể `Tiêu chuẩn`.
- Mỗi công thức có đúng 2 nguyên liệu khác nhau, lượng dương.
- Không có chuỗi `DEMO` trong mã ownership mới, SKU, mã nguyên liệu, mã phiếu nhập, mã đơn hàng hoặc nội dung hiển thị thuộc seed mới.
- Không có từ `demo`, `trình diễn`, `mẫu` trong tên/mô tả/ghi chú hiển thị thuộc seed.
- Toàn bộ nguyên liệu hợp lệ; phiếu nhập, giao dịch kho và kiểm kê nhất quán.
- Đơn hàng, thanh toán, hoàn tiền, ca làm và COD hiện có vẫn đạt các kiểm tra trình diễn trước đó.

## Verification

1. Cập nhật test policy Java để khóa số lượng, prefix mới, phân bổ danh mục và công thức tường minh.
2. Chạy test Java liên quan rồi `mvn test`.
3. Trên `DemoDatabase` disposable: chạy seed và validator hai lần để chứng minh idempotency.
4. Đối chiếu catalog/runtime với schema canonical trước khi chạy.
5. Không chạy trên retained `FastGuyDB`.
6. Chạy `git diff --check` và bảo vệ toàn bộ thay đổi frontend chưa commit hiện tại.
