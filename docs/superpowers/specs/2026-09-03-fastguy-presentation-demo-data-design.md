# Thiết kế dữ liệu demo vận hành FastGuy

## Mục tiêu

Tạo bộ dữ liệu demo toàn nghiệp vụ đủ phong phú để trình bày menu, đơn hàng, doanh thu, hoàn tiền, COD, nhân sự, kho nguyên liệu, công thức và báo cáo trong khoảng 30 ngày.

## Phạm vi dữ liệu

- 20 sản phẩm/biến thể có tên món tự nhiên, trạng thái và mức giá đa dạng.
- Sản phẩm được phân vào đúng 7 danh mục hiển thị: `Bánh mì`, `Burger`, `Pizza`, `Cơm`, `Mì`, `Gà rán`, `Nước uống`.
- Tên danh mục, sản phẩm, mô tả, khách hàng, địa chỉ, nguyên liệu, ghi chú, đánh giá và dữ liệu vận hành hiển thị không chứa `demo`, `DEMO`, `trình diễn` hoặc `mẫu`.
- 20 nguyên liệu theo các đơn vị G, ML và PIECE.
- 40 dòng công thức liên kết biến thể với nguyên liệu.
- 45 đơn hàng trải trong 30 ngày, gồm COD và BANK_TRANSFER.
- Có đơn giao thành công trong từng ngày của 7 ngày gần nhất, gồm ít nhất một đơn hôm nay, để dashboard luôn có biểu đồ doanh thu, doanh thu thuần, tỷ lệ hoàn thành và top sản phẩm.
- Có đơn đang xử lý, hủy, giao thất bại và trả về cửa hàng.
- Có yêu cầu hoàn tiền ở các trạng thái cần thiết cho hàng đợi và báo cáo nhưng không gọi nhà cung cấp thanh toán.
- Có dữ liệu bàn giao COD ở trạng thái chờ, đã xác minh và có chênh lệch để trình bày đối soát.
- Có lịch làm việc, chấm công và mức lương mẫu cho các vai trò vận hành hiện có.
- Có dữ liệu nhập kho, giữ chỗ, tiêu hao, hao hụt, điều chỉnh và kiểm kê để dashboard và báo cáo có số liệu thực tế.

## Nguyên tắc dữ liệu

- Dùng tiền tố/business key `DEMO-PRES-*` chỉ trong trường kỹ thuật không hiển thị như SKU, mã đơn, idempotency key, reference và marker cleanup.
- Không đưa marker kỹ thuật vào `Category.name`, `Product.name`, `Product.description`, `InventoryItem.name`, tên khách, địa chỉ hoặc nội dung người dùng nhìn thấy.
- Số tiền đơn phải khớp chi tiết đơn, giảm giá, phí giao và tổng cuối.
- Trạng thái thanh toán, mốc thời gian và trạng thái đơn phải hợp lý với nhau.
- Dữ liệu kho phải giữ số lượng không âm, chi phí không âm và ledger có số dư trước/sau nhất quán.
- Công thức chỉ tham chiếu biến thể và nguyên liệu đang tồn tại.
- Seed phải tạo dữ liệu có tính trình diễn, không chứa thông tin cá nhân thật.

## An toàn và idempotency

- Chỉ chạy khi `DB_NAME() = 'DemoDatabase'`.
- Yêu cầu session context `FASTGUY_ALLOW_PRESENTATION_DEMO_SEED = 1`.
- Yêu cầu migration 065 đã tồn tại.
- Bao toàn bộ thay đổi trong transaction với `XACT_ABORT ON` và `TRY/CATCH`.
- Chạy lại không làm tăng số bản ghi hoặc doanh thu; seed cập nhật/thay thế đúng tập dữ liệu `DEMO-PRES-*` mà nó sở hữu.
- Không xóa hoặc sửa dữ liệu ngoài namespace demo.
- Không xóa coupon demo nếu bản ghi ngoài namespace đang tham chiếu; upsert cấu hình coupon và giữ nguyên quan hệ ngoài phạm vi.
- Chỉ áp dụng lên `FastGuyDB` sau khi bản chính xác đã qua hai vòng seed + validator trên `DemoDatabase`, identity được xác nhận, backup/restore đã được xác nhận và người dùng phê duyệt thao tác ghi.

## Đầu ra

- `database/seed_presentation_demo.sql`: script seed.
- `database/seed_presentation_demo_validate.sql`: validator độc lập.
- Test source-policy để khóa target, opt-in, namespace và các số lượng kỳ vọng.

## Kiểm tra

- Kiểm tra tĩnh script và source-policy test.
- Preflight xác minh server, database, trạng thái, compatibility và migration 065.
- Chạy seed hai lần trên `DemoDatabase` để chứng minh idempotency.
- Chạy validator sau mỗi lần.
- Đối chiếu số lượng, tổng doanh thu, FK, trạng thái, số dư kho, hoàn tiền, COD, ca làm và chấm công.
- Validator xác nhận đủ 7 danh mục, mỗi sản phẩm thuộc đúng một danh mục được phép và không có từ cấm trong mọi trường hiển thị do seed sở hữu.
- Kiểm tra đúng 7 điểm doanh thu gần nhất, doanh thu hôm nay lớn hơn 0, tỷ lệ hoàn thành hôm nay lớn hơn 0 và top sản phẩm có ít nhất 5 dòng.
- Chạy backend `mvn test`; frontend không thay đổi nên không yêu cầu build lại.

## Cổng đưa sang FastGuyDB

Kết quả trên `DemoDatabase` không tự động cho phép ghi vào `FastGuyDB`. Trước khi đưa dữ liệu sang database giữ lại cần backup đầy đủ, thử restore, xác nhận identity và một phê duyệt riêng từ người dùng. Phiên hiện tại đã có xác nhận backup/restore và phê duyệt mở rộng dữ liệu mẫu; nếu validator hoặc identity lệch phải dừng và yêu cầu phê duyệt lại.

## Luồng triển khai

1. Mở rộng source-policy test và quan sát RED.
2. Mở rộng seed/validator theo từng nhóm phụ thuộc: dữ liệu nền → đơn hàng/thanh toán → hoàn tiền/COD → nhân sự → kho/báo cáo.
3. Chạy seed + validator hai lần trên `DemoDatabase`.
4. Xác nhận lại `DuckJo/FastGuyDB`, áp dụng cùng bản SQL đã kiểm chứng trong transaction và chạy validator.
5. Kiểm tra read-only các số liệu mà dashboard và trang nghiệp vụ thực sự truy vấn.
6. Chạy `mvn test`, frontend test/build nếu file frontend trong working tree được đưa vào cùng commit, và `git diff --check`.
7. Chỉ stage các file thuộc hai nhóm đã hoàn thành; không stage hàng loạt 129 artifact không liên quan.

## Phạm vi Git

- Repository đang ở `main`, đồng bộ với `origin/main`; không có feature branch cần merge.
- Tạo các commit nhỏ theo nhóm: dữ liệu trình diễn và các defect fix đã được kiểm chứng.
- Không đưa `.agents`, `.hermes`, tài liệu người dùng, diagram hoặc artifact không liên quan vào commit.
- Không push nếu người dùng không yêu cầu rõ; yêu cầu hiện tại chỉ bao gồm commit và tích hợp vào local `main`.
