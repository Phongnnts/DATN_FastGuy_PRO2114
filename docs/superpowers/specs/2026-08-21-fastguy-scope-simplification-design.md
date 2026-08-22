# Thiết kế đơn giản hóa phạm vi FastGuy

## CAPABILITY

FastGuy thu hẹp sản phẩm về vận hành bán món đơn lẻ có kích cỡ và topping: xóa vĩnh viễn Review, SupportTicket, ProductCombo, ProductComboItem và phí dịch vụ; giữ WorkShift, ProductVariant cùng ProductModifierGroup/ProductModifierOption. Khách hàng, nhân viên và quản trị viên không còn thấy hoặc gọi được các khả năng đã xóa. Quản trị viên có đủ dữ liệu để đối soát COD và xử lý hoàn tiền bằng chứng thủ công. Dữ liệu lịch sử cục bộ được viết lại nhất quán, có kiểm chứng và dừng an toàn khi không chứng minh được bất biến.

### Tác nhân và kết quả

- **Khách hàng:** không còn review, hỗ trợ hoặc combo theo dịp; checkout và mọi tổng tiền không còn phí dịch vụ; nhìn thấy “Kích cỡ” thay cho “Biến thể”.
- **Nhân viên:** không còn màn hình hoặc hành động SupportTicket; dữ liệu đơn không còn phí dịch vụ.
- **Shipper:** giữ nguyên ca làm việc; số COD phải bàn giao phản ánh tổng tiền mới sau khi loại phí dịch vụ.
- **Quản trị viên:** không còn quản trị review nổi bật, hỗ trợ hoặc combo; xem và xác minh COD theo ca; xem đầy đủ chi tiết hoàn tiền và nhập bằng chứng hoàn tiền khi hoàn tất.
- **Hệ thống:** OpenAPI là nguồn chuẩn trước backend/provider serialization và frontend consumer; migration phá hủy chỉ chạy sau khi catalog, backup, restore và validator đạt yêu cầu.

## CONSTRAINTS

### Chính sách cố định

- Xóa vĩnh viễn dữ liệu, bảng, entity/DTO, DAO, service, servlet/API, OpenAPI, test/fixture chuyên biệt và UI của `Review`, `SupportTicket`, `ProductCombo`, `ProductComboItem`.
- Xóa section review và combo theo dịp trên homepage, review nổi bật trong admin, hỗ trợ phía khách hàng và nhân viên.
- Xóa phí dịch vụ toàn hệ thống: config key, cột `Orders.service_fee`, công thức checkout, serializer, OpenAPI, settings, báo cáo, fixture/test và UI.
- Không giữ endpoint tương thích, adapter, bảng lưu trữ, cờ ẩn hoặc dead code cho bốn domain bị xóa. Sau triển khai, các đường HTTP đã bị xóa không còn operation trong OpenAPI và không còn provider route; yêu cầu tới đường không còn ánh xạ nhận `404`, không trả payload legacy.
- Giữ nguyên `WorkShift`, gồm schema, lifecycle và hành vi hiện tại.
- Giữ nguyên entity/schema `ProductVariant`; không đổi tên bảng, cột, class, JSON field hoặc quan hệ chỉ để đổi thuật ngữ. Mọi wording khách hàng/quản trị viên nhìn thấy đổi từ “Biến thể” sang “Kích cỡ”. SKU, giá, tồn kho và kích thước GHN tiếp tục thuộc ProductVariant.
- Giữ `ProductModifierGroup` và `ProductModifierOption` làm topping/tùy chọn món; không nhập chúng vào ProductVariant và không biến chúng thành combo.
- Không được đoán runtime schema, request hoặc response. Catalog SQL Server read-only và `openapi/fastguy.yaml` quyết định implementation thực tế.
- Migration `050` về inventory parity là luồng riêng. Tính năng này không được giả định `050` đã chạy; chỉ catalog runtime mới xác nhận trạng thái của nó. Migration mới phải khai báo prerequisite thực sự theo object/constraint cần dùng, không phụ thuộc mù vào số thứ tự `050`.
- Đây là chuyển đổi dữ liệu local phục vụ demo. Không có tuyên bố production-ready hoặc production parity.

### Bất biến tiền tệ

Với mỗi đơn có phí dịch vụ lịch sử `f`, trong đó `f` phải không null hoặc được chuẩn hóa theo quy tắc schema đã được catalog chứng minh, migration áp dụng `new_final_amount = old_final_amount - f` rồi xóa nguồn phí. Không giá trị tiền tệ hoặc điểm nào được âm sau chuyển đổi.

Các snapshot phụ thuộc tổng đơn phải được đối chiếu và điều chỉnh nhất quán theo ngữ nghĩa runtime đã chứng minh, tối thiểu gồm:

- `Orders.cod_collected_amount`;
- `Orders.refund_amount`;
- `PaymentAttempt.amount`;
- các giá trị tiền trong `CodSettlement`;
- giao dịch loyalty và `Users.loyalty_points`.

Validator phải chứng minh quan hệ trước/sau cho từng bản ghi, tổng hợp theo đơn và theo ca. Nếu một snapshot không thể gán chắc chắn cho đơn hoặc không chứng minh được nó có chứa phí dịch vụ, migration dừng thay vì trừ suy đoán. Dữ liệu PayOS lịch sử sau viết lại chỉ là biểu diễn demo cục bộ; không được tuyên bố số tiền local còn khớp link, giao dịch, webhook hoặc sổ cái của nhà cung cấp bên ngoài.

### Bất biến COD

- Mỗi đơn COD đã giao được gán đúng một shipper và một `WorkShift` bằng quan hệ/thời gian đã được runtime catalog và code hiện hành chứng minh.
- Với mỗi ca, `shift_fee_delta` là tổng phí dịch vụ đã xóa của các đơn COD `DELIVERED` thuộc ca đó.
- `CodSettlement.expectedAmount` mới bằng giá trị cũ trừ `shift_fee_delta`.
- Với snapshot lịch sử đã nộp hoặc đã xác minh, `submittedAmount` và `verifiedAmount` không null lần lượt trừ cùng `shift_fee_delta`. Trường null được giữ null.
- Bất kỳ kết quả âm nào làm migration thất bại.
- Sau đồng bộ, settlement đang `SUBMITTED` vẫn là `SUBMITTED`.
- Settlement đã xác minh được tính lại từ `verifiedAmount` so với `submittedAmount`: bằng nhau là `SETTLED`, nhỏ hơn là `SHORT`, lớn hơn là `OVER`.
- Giữ nguyên `reason`, `receivedBy`, `submittedAt`, `verifiedAt` và các timestamp khác. Migration không tự xóa reason khi trạng thái tính lại là `SETTLED`.
- Nếu không chứng minh được gán ca/đơn hoặc tổng `shift_fee_delta`, migration dừng toàn bộ.

### Bất biến loyalty

- Với từng đơn đủ điều kiện EARN, tính lại điểm EARN từ `new_finalAmount` bằng `floor(new_finalAmount / 1000)` theo đơn vị tiền hiện hành.
- Giao dịch reverse/refund liên quan đơn phải được đối chiếu lại từ EARN mới, giữ đúng dấu và không cho đảo nhiều hơn số điểm đã earn sau chuyển đổi.
- Tính lại `Users.loyalty_points` từ ledger sau khi mọi giao dịch theo đơn đã được reconcile; không chỉnh balance bằng delta tổng quát nếu ledger không giải thích được kết quả.
- Dừng migration khi gặp loại giao dịch không hỗ trợ, thiếu liên kết đơn, duplicate/ambiguous reversal, số dư âm hoặc ledger không khớp balance.

### Trust boundary và quyền

- Backend tiếp tục xác thực actor, role, trạng thái tài khoản, ownership và state transition; frontend không phải nguồn quyền.
- Submission COD bất biến sau khi tạo. Shipper không sửa hoặc gửi lại settlement của cùng ca.
- Chỉ Admin active được xác minh settlement `SUBMITTED` thành `SETTLED`, `SHORT` hoặc `OVER`; trạng thái khác bị từ chối bằng conflict, không ghi một phần.
- `SHORT`/`OVER` bắt buộc reason không rỗng. `SETTLED` yêu cầu verified bằng submitted.
- Hoàn tất refund bắt buộc `refundReference` do Admin nhập. Giá trị này là bằng chứng từ ngân hàng, ví điện tử hoặc chuyển khoản thủ công; không phải mã đơn nội bộ và không tự sinh.

## IMPLEMENTATION CONTRACT

### 1. Thứ tự triển khai

Thực hiện đúng `DATABASE → API → BACKEND → FRONTEND`:

1. Chụp catalog runtime read-only, đối chiếu SQL canonical, migration và JPA mapping.
2. Tạo migration cùng validator; kiểm chứng trên database disposable được restore từ backup đại diện.
3. Cập nhật OpenAPI trước: xóa contract domain/phí dịch vụ, bổ sung contract COD/refund cần thiết.
4. Cập nhật backend/provider serialization đúng contract.
5. Cập nhật frontend API consumer, store và view theo contract đã lint.
6. Chạy contract, integration, build và E2E; không triển khai vào retained database trong lane mặc định.

### 2. Database và migration

#### Preflight read-only

- Ghi nhận `@@SERVERNAME`, `DB_NAME()`, database state, compatibility level.
- Kiểm tra sự tồn tại, kiểu, nullability, default, constraint, index và foreign key của các bảng/cột liên quan.
- Đối chiếu runtime với `database/init.sql`, `database/DB_FastGuy.sql`, migrations liên quan và JPA mapping.
- Xác định bằng catalog liệu migration `050` đã được phản ánh; không chạy hoặc gộp `050` vào migration này.
- Nếu sai server/database, thiếu quyền `SELECT`/`VIEW DEFINITION`, schema lệch không giải thích được hoặc attribution dữ liệu không chứng minh được, dừng ở source analysis.

#### Migration

Migration chạy trong transaction phù hợp với SQL Server và theo thứ tự bảo toàn dữ liệu:

1. Validator preflight tạo tập đối chiếu trước thay đổi: phí theo đơn, tiền theo đơn, COD theo ca, loyalty theo đơn/user và số lượng bản ghi domain sẽ xóa.
2. Viết lại `final_amount` và mọi snapshot tiền được validator xác nhận có chứa phí dịch vụ.
3. Đồng bộ `CodSettlement` theo chính sách deterministic ở phần CONSTRAINTS.
4. Tính lại EARN, reverse/refund và balance loyalty từ ledger.
5. Chạy invariant checks trong cùng quy trình; lỗi làm rollback toàn bộ.
6. Xóa dữ liệu và bảng `Review`, `SupportTicket`, `ProductComboItem`, `ProductCombo` theo thứ tự foreign key thực tế.
7. Xóa config key phí dịch vụ và cột `Orders.service_fee` cùng constraint/index phụ thuộc đã catalog xác nhận.
8. Không đổi schema `WorkShift` hoặc `ProductVariant`.

Validator sau migration phải kiểm tra object đã xóa/giữ, không orphan, không âm, công thức tổng tiền, snapshot theo đơn, tổng COD theo ca, trạng thái COD, loyalty ledger/balance và số bản ghi kỳ vọng. Validator có chế độ rerun read-only; migration rerun không được trừ phí lần hai và phải kết thúc an toàn theo migration ledger/object guards của dự án.

#### Chính sách migration và rollback

- **Disposable:** restore backup đại diện vào local/disposable; apply migration; chạy validator; chạy migration lần hai để chứng minh idempotency guard; chạy validator lần hai; lưu log và checksum. Không dùng `database/init.sql` để mô phỏng retained data.
- **Retained:** cần một phê duyệt phá hủy riêng, rõ tên server/database và migration; backup đã kiểm chứng; restore thử thành công; thời lượng restore đo được; recovery owner; maintenance window; tiêu chí go/no-go; lệnh rollback/recovery đã diễn tập. Không suy diễn phê duyệt từ việc user duyệt đặc tả.
- Vì migration xóa bảng/cột và viết lại lịch sử, rollback bằng down-script không được coi là an toàn. Trước commit transaction có thể rollback tự động khi validator lỗi; sau commit, recovery duy nhất là dừng ứng dụng, restore backup đã xác minh, xác nhận catalog/checksum, chạy validator phiên bản trước và đưa application version trước trở lại.
- Không bao giờ chạy `database/init.sql` trên retained database.

### 3. API và HTTP effects

`openapi/fastguy.yaml` là nguồn chuẩn:

- Xóa mọi operation/schema/field dành riêng cho Review, SupportTicket, ProductCombo, ProductComboItem và service fee.
- Xóa service-fee input/output khỏi checkout, order serializers, settings và reports.
- Homepage response không còn review hoặc occasion-combo section; admin product/homepage response không còn featured review/combo data.
- API sản phẩm vẫn giữ field contract ProductVariant hiện hành. Đổi nhãn UI, không đổi JSON field nếu không có quyết định contract riêng.
- COD settlement response phải biểu diễn rõ: shipper, shift, `expectedAmount`, `submittedAmount`, `verifiedAmount`, difference, status, reason, `submittedAt`, `verifiedAt`, verified-by. Nullability phản ánh state lifecycle; difference được định nghĩa duy nhất trong OpenAPI/provider là `verifiedAmount - submittedAmount` khi cả hai có giá trị, ngược lại null.
- Refund detail serialize: `customerName`, `customerPhone` snapshot, `orderCode`, `paymentMethod`, `finalAmount`, `refundAmount`, `refundStatus`, `refundReference`, `refundNote`, `processedBy`, `refundedAt`.
- Contract refund-completion đánh dấu `refundReference` bắt buộc tại transition hoàn tất; định nghĩa validation/error/status code theo pattern endpoint hiện có, không tạo endpoint mới trong đặc tả này.
- Không thêm hoặc đoán URL. Implementation dùng operation hiện có sau khi inventory OpenAPI/provider xác nhận.
- Lint OpenAPI; backend contract test và frontend fixture/consumer test cùng kiểm chứng schema, required, nullable, enum, status code và error response.

### 4. Backend/provider

- Xóa toàn bộ luồng `Servlet → Service → DAO → Entity/DTO` của bốn domain bị loại và mọi caller không còn hợp lệ.
- Xóa tính service fee ở checkout, mapping Orders, serializer, settings, report và test/fixture.
- Công thức tạo đơn mới không có service fee; `finalAmount` phải được tính từ các thành phần contract còn giữ. Không duy trì field service fee bằng `0`.
- Homepage service/serializer không query hoặc trả review/combo section.
- Product CRUD giữ ProductVariant, SKU, giá, tồn kho, GHN dimensions và modifier/topping.
- COD serialization trả đủ contract. Submission tạo một snapshot rồi bất biến. Verification khóa/revalidate settlement, chỉ Admin active, chỉ từ `SUBMITTED`; mismatch cần reason; mutation lỗi rollback toàn bộ và trả error theo contract hiện có.
- Refund detail lấy customer name/phone snapshot thuộc đơn, không lấy profile hiện tại nếu snapshot đơn tồn tại. `processedBy` serialize theo representation được OpenAPI chốt, không để frontend suy diễn từ ID không contract hóa.
- Refund completion trim và validate `refundReference`; từ chối null/rỗng; lưu đúng chuỗi Admin nhập, không sinh mã và không thay bằng `orderCode`.

### 5. Frontend

#### Xóa bề mặt

- Xóa homepage review và occasion combo sections.
- Xóa admin featured review và toàn bộ combo editor/consumer.
- Xóa support customer/staff, route/navigation/API client/store/view liên quan.
- Xóa mọi nhãn, dòng tiền, filter, KPI, setting hoặc report service fee.
- Xóa link/nút dẫn tới HTTP operation đã loại; không để UI gọi endpoint 404.

#### Kích cỡ và topping

- Ở mọi bề mặt khách hàng và admin, nhãn “Biến thể” đổi thành “Kích cỡ”, gồm form, danh sách, loading/empty/error, validation và aria-label.
- Không đổi key, route, API payload hoặc schema ProductVariant chỉ vì relabel.
- Topping/tùy chọn tiếp tục dùng ProductModifierGroup/ProductModifierOption, hiển thị tách biệt với kích cỡ.

#### Admin COD settlement

- Danh sách/chi tiết hiển thị rõ shipper, ca, expected, submitted, verified, difference, status, reason, submittedAt, verifiedAt, verifiedBy.
- Submission chỉ đọc đối với Admin; không có edit/resubmit.
- Chỉ settlement `SUBMITTED` hiển thị hành động verify. Admin nhập verified amount; UI suy ra/giới hạn lựa chọn kết quả theo so sánh nhưng backend vẫn quyết định hợp lệ.
- Khi verified khác submitted, reason bắt buộc với label và inline error. Khi bằng nhau, kết quả là `SETTLED`.
- Có loading, empty, error, retry, stale/conflict sau concurrent update, mutation lock và refetch sau thành công.
- Desktop hiển thị bảng/chi tiết dễ so sánh; mobile dùng card/stack, không cuộn ngang bắt buộc, số tiền và trạng thái không bị cắt, control có label và vùng chạm phù hợp.

#### Admin refund detail

- Hiển thị customerName, customerPhone snapshot, orderCode, paymentMethod, finalAmount, refundAmount, refundStatus, refundReference, refundNote, processedBy, refundedAt.
- Điện thoại là link `tel:` với visible text là snapshot; không dùng profile hiện tại để thay thế âm thầm.
- Nhãn `refundReference` là **“Mã giao dịch/biên nhận hoàn tiền”**.
- Help text giải thích: đây là bằng chứng Admin nhập từ ngân hàng, ví điện tử hoặc chuyển khoản thủ công; không phải mã đơn nội bộ; hệ thống không tự tạo.
- Khi hoàn tất refund, trường này bắt buộc trước submit, có validation/error rõ ràng, chống double-submit và refetch detail sau thành công.
- Có loading, empty/not-found, error/retry; desktop/mobile không che khuất số tiền, reference hoặc trạng thái.

## ACCEPTANCE CRITERIA

### Database

- Catalog read-only ghi nhận đúng target và chứng minh schema trước khi migration; trạng thái migration `050` được xác định bằng catalog, không giả định.
- Sau migration, bốn bảng domain và `Orders.service_fee` không tồn tại; `WorkShift`, `ProductVariant`, ProductModifierGroup/ProductModifierOption và dữ liệu của chúng vẫn tồn tại.
- Mỗi `final_amount` lịch sử bằng giá trị cũ trừ đúng service fee cũ; mọi snapshot được hỗ trợ khớp quy tắc đối chiếu; không giá trị âm.
- Mỗi COD shift có delta bằng tổng phí của đúng các đơn COD delivered thuộc ca; expected/submitted/verified được trừ đúng; status tính lại đúng; metadata được giữ.
- EARN từng đơn bằng `floor(new_finalAmount/1000)`; reverse/refund hợp lệ; từng `Users.loyalty_points` bằng ledger đã reconcile.
- Attribution không chứng minh được, unsupported loyalty ambiguity, negative hoặc invariant mismatch làm migration thất bại và không để thay đổi một phần.
- Disposable restore/apply/validate/rerun/validate đạt; retained execution chưa được phép nếu thiếu approval và recovery evidence riêng.

### API

- OpenAPI không còn operation/schema/field của Review, SupportTicket, ProductCombo, ProductComboItem hoặc service fee.
- Homepage, checkout, orders, settings và reports contract không còn dữ liệu đã xóa.
- COD/refund schema chứa đúng field, required/nullability/enums và semantics đã nêu; không có remote `$ref`; lint đạt.
- Provider output và frontend fixtures validate theo cùng OpenAPI. HTTP route đã loại không còn provider mapping và trả `404` khi không bị một security boundary hiện hữu chặn trước.

### Backend

- Không còn source/caller runtime của bốn domain hoặc service fee; build không còn import/reference mồ côi.
- Đơn mới và mọi serializer/report dùng tổng tiền không phí dịch vụ.
- COD submission không sửa được; chỉ Admin active verify `SUBMITTED`; transition/status/reason/concurrency đúng; lỗi rollback.
- Refund detail lấy snapshot đúng; hoàn tất refund thiếu reference bị từ chối; reference không tự sinh.
- Test liên quan và toàn bộ `mvn test` đạt.

### Frontend

- Không còn homepage review/occasion combo, admin featured review/combo, support customer/staff hoặc service-fee UI/navigation/client calls.
- Mọi wording customer/admin liên quan ProductVariant là “Kích cỡ”; SKU, giá, stock, GHN dimensions và topping vẫn hoạt động.
- API client/store/view chỉ dùng field có trong OpenAPI; test frontend liên quan, `npm test` và `npm run build` đạt.
- Không có console error; request chính thành công; không có request tới API đã xóa.

### COD và refund

- Admin COD thấy đủ shipper, shift, expected/submitted/verified/difference/status/reason/timestamps/verifier trên desktop và mobile.
- Chỉ `SUBMITTED` có verify; mismatch không reason không submit được và backend cũng từ chối; stale update hiển thị conflict rồi reload.
- Refund detail thấy đủ mười field yêu cầu; phone mở `tel:` đúng snapshot; label/reference help text đúng; completion bắt buộc reference.

### Integration và E2E

- Integration test trên disposable database chứng minh rewrite tiền, COD, loyalty, xóa schema và migration rerun guard.
- Contract tests chứng minh OpenAPI/provider/frontend không drift.
- Playwright desktop và mobile kiểm tra tối thiểu: homepage sau xóa section, checkout không service fee, chọn Kích cỡ/topping, admin product editor không combo, admin COD verify match/mismatch, admin refund detail/completion.
- E2E xác nhận không console error và request chính có HTTP success; đường đã xóa chỉ được kiểm tra `404` ở integration/contract scope phù hợp.
- `mvn test`, test frontend liên quan, `npm test`, `npm run build` và `git diff --check` đều đạt trước khi tuyên bố hoàn tất.

## MIGRATION / ROLLBACK POLICY

- Migration phá hủy là artifact riêng kèm validator; không chạy qua công cụ catalog read-only.
- Chỉ disposable/local được chạy trong implementation và verification mặc định.
- Retained database cần user phê duyệt riêng sau khi xem chính xác target, backup verification, restore rehearsal, recovery plan và migration checksum.
- Validator lỗi trước commit phải rollback transaction. Lỗi sau commit hoặc phát hiện hậu kiểm phải dừng application writes và restore backup; không cố “bù” bằng DML thủ công.
- Không dùng `init.sql` trên retained data. Không gộp hoặc mặc nhiên chạy migration `050`.
- Không tuyên bố parity với PayOS lịch sử sau rewrite và không tuyên bố sẵn sàng production.

## NON-GOALS

- Không archive, soft-delete, export hoặc xây read-only legacy viewer cho domain bị xóa.
- Không giữ backward compatibility cho API/JSON field đã xóa.
- Không đổi tên schema/class/API field ProductVariant thành Size.
- Không thay đổi WorkShift, lifecycle giao hàng hoặc mô hình attribution ngoài điều cần thiết để validator chứng minh dữ liệu hiện có.
- Không thiết kế lại loyalty, payment, PayOS, COD settlement hoặc refund thành hệ thống sổ cái mới.
- Không tự động xác minh giao dịch ngân hàng/ví, không sinh refund reference, không tích hợp provider mới.
- Không thêm endpoint do đặc tả suy đoán; chỉ sửa operation hiện có sau inventory contract/provider.
- Không thực thi migration trên retained/production database trong phạm vi implementation mặc định.
- Không bao gồm migration `050` inventory parity.

## OPEN QUESTIONS

Không còn câu hỏi sản phẩm chặn implementation. Các chi tiết tên endpoint, kiểu cột, constraint, quan hệ attribution và representation `processedBy` không phải quyết định mở: chúng phải được lấy từ OpenAPI, runtime catalog và source hiện tại. Nếu ba nguồn không đủ hoặc mâu thuẫn, implementation dừng để yêu cầu quyết định mới; không tự suy luận.

## HANDOFF

Đặc tả sẵn sàng chuyển sang kế hoạch triển khai theo lát cắt `DATABASE → API → BACKEND → FRONTEND`.

Kế hoạch phải:

1. Inventory runtime catalog, OpenAPI operations, backend providers/callers và frontend consumers bằng CodeGraph cùng công cụ read-only.
2. Tách migration/validator thành gate đầu tiên; giữ migration `050` độc lập.
3. Chia test theo database invariants, contract/provider, backend policy, frontend consumer và Playwright desktop/mobile.
4. Đặt retained migration thành gate phê duyệt riêng ngoài implementation mặc định.
5. Dừng ở bất kỳ catalog mismatch, attribution ambiguity, invariant failure, contract drift hoặc recovery-evidence gap nào.
