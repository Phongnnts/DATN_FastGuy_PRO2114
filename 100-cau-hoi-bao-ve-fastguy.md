# 100 câu hỏi ôn bảo vệ dự án FastGuy

## Cách sử dụng

- Ôn theo từng nhóm, tự trả lời trước khi đọc đáp án mẫu.
- Với câu hỏi phản biện, luôn tách **hiện trạng đã triển khai** khỏi **đề xuất cải tiến**.
- Khi trình bày, đi theo mô hình: **Bối cảnh → Quy tắc → Xử lý → Kết quả → Lý do**.
- Không học thuộc từng chữ. Hãy nhớ trạng thái, quyền, transaction, dữ liệu kiểm chứng và giới hạn thật của hệ thống.

## Nguyên tắc trả lời bảo vệ

1. **Bối cảnh:** Nêu đúng tác nhân và tình huống của FastGuy.
2. **Quy tắc:** Chỉ ra điều kiện nghiệp vụ, quyền hoặc invariant phải giữ.
3. **Xử lý:** Mô tả luồng Servlet → Service → DAO/JPA → SQL Server và frontend consumer khi liên quan.
4. **Kết quả:** Nêu trạng thái, dữ liệu lịch sử, phản hồi API hoặc tác động tồn kho/thanh toán.
5. **Lý do:** Giải thích lựa chọn về nhất quán dữ liệu, bảo mật, truy vết hoặc trải nghiệm người dùng.

## Nhóm 1. Tổng quan dự án và nghiệp vụ

### Câu 1. FastGuy giải quyết bài toán nghiệp vụ nào?

**Đáp án mẫu:** FastGuy là hệ thống đặt và vận hành giao đồ ăn nhanh. Hệ thống nối luồng khách chọn sản phẩm, tạo đơn, thanh toán COD hoặc chuyển khoản, nhân viên xử lý bếp, điều phối shipper, giao hàng, đối soát COD và quản trị sau bán. Điểm chính không chỉ là màn hình bán hàng mà là giữ nhất quán giữa đơn hàng, thanh toán, tồn kho, ca làm và lịch sử thao tác.

**Câu hỏi truy vấn tiếp:** Giá trị khác biệt của FastGuy so với một website chỉ nhận đơn là gì?

**Ý trả lời truy vấn:** FastGuy bao phủ cả vận hành nội bộ: trạng thái đơn theo vai trò, reservation tồn kho, ca làm, điều phối shipper, hoàn tiền, COD settlement và báo cáo ngoại lệ.

### Câu 2. Những tác nhân chính trong FastGuy là ai?

**Đáp án mẫu:** Hệ thống có bốn vai trò: `ADMIN`, `STAFF`, `SHIPPER`, `USER`; ngoài ra khách chưa đăng nhập vẫn có thể đặt và tra cứu đơn theo cơ chế guest. `USER` mua hàng; `STAFF` xử lý bếp, điều phối và một số nghiệp vụ vận hành; `SHIPPER` nhận và cập nhật giao hàng; `ADMIN` quản trị, giám sát và xử lý các nghiệp vụ đặc quyền.

**Câu hỏi truy vấn tiếp:** Guest có phải là một role lưu trong JWT không?

**Ý trả lời truy vấn:** Không. Guest không có JWT role; quyền truy cập đơn được chứng minh bằng track code, bốn số cuối điện thoại hoặc guest return proof tùy luồng.

### Câu 3. Công nghệ cốt lõi của FastGuy là gì?

**Đáp án mẫu:** Backend là Java 17 WAR chạy Jakarta Servlet 6.1, dùng JPA 3.1/Hibernate 6.6 để ánh xạ và thao tác SQL Server. Frontend dùng Vue 3 Composition API, Pinia, Vue Router và API client. Hợp đồng HTTP đã contract hóa lấy OpenAPI 3.1 tại `openapi/fastguy.yaml` làm nguồn chuẩn.

**Câu hỏi truy vấn tiếp:** Vì sao không mô tả dự án là Spring Boot?

**Ý trả lời truy vấn:** Source hiện tại dùng Servlet/JPA trực tiếp, không dùng Spring MVC hay Spring Data; gọi sai framework sẽ mô tả sai kiến trúc thực tế.

### Câu 4. Luồng nghiệp vụ đầu-cuối tiêu biểu là gì?

**Đáp án mẫu:** Khách cấu hình sản phẩm, variant và modifier, sau đó checkout với phương thức COD hoặc `BANK_TRANSFER`. Backend tự tính giá, khóa và reserve tồn, tạo đơn `PENDING`; thanh toán chuyển khoản chỉ được xác nhận từ PayOS đã kiểm chứng. Nhân viên đưa đơn qua chuẩn bị đến `READY`, gán shipper đang trong ca, shipper giao; kết quả cuối là `DELIVERED`, `RETURNED_TO_STORE` hoặc `CANCELLED`, kèm history và các bút toán liên quan.

**Câu hỏi truy vấn tiếp:** Tại sao không cho frontend tự gửi tổng tiền cuối cùng?

**Ý trả lời truy vấn:** Backend là trust boundary; giá, coupon, tồn kho và tổng tiền phải được tính lại từ dữ liệu tin cậy.

### Câu 5. FastGuy hỗ trợ khách đăng nhập và khách vãng lai khác nhau thế nào?

**Đáp án mẫu:** Người đăng nhập sở hữu đơn qua `userId`, xem lịch sử và dùng các tính năng gắn tài khoản. Guest vẫn đặt hàng được nhưng tra cứu bằng mã đơn kết hợp đúng bốn số cuối số điện thoại. Với PayOS return, guest còn dùng proof riêng được lưu dạng hash để không biến query return từ trình duyệt thành bằng chứng thanh toán tùy ý.

**Câu hỏi truy vấn tiếp:** Chỉ biết mã đơn guest có đủ xem đơn không?

**Ý trả lời truy vấn:** Không; luồng track yêu cầu thêm bốn số cuối điện thoại để giảm khả năng dò mã đơn.

### Câu 6. Tại sao trạng thái đơn hàng là trung tâm của thiết kế?

**Đáp án mẫu:** Trạng thái đơn biểu diễn tiến độ thật từ `PENDING` đến các kết thúc như `DELIVERED`, `CANCELLED`, `RETURNED_TO_STORE`. Mỗi chuyển trạng thái kéo theo quyền actor, điều kiện thanh toán, tồn kho, thời điểm nghiệp vụ và history. Vì vậy FastGuy dùng transition có kiểm soát thay vì cho client cập nhật chuỗi trạng thái trực tiếp.

**Câu hỏi truy vấn tiếp:** Một cột `orderStatus` có đủ audit không?

**Ý trả lời truy vấn:** Không; cột chỉ giữ hiện trạng, còn `OrderStatusHistory` giữ actor, role, trạng thái trước/sau, lý do và thời điểm.

### Câu 7. FastGuy xử lý sản phẩm có cấu hình như thế nào?

**Đáp án mẫu:** Sản phẩm có variant, modifier và combo để mô hình hóa kích cỡ, lựa chọn thêm và gói sản phẩm. Khi tạo đơn, `OrderItem` lưu snapshot tên sản phẩm, tên variant, đơn giá, tổng giá và modifier JSON. Snapshot giúp đơn cũ không bị đổi nghĩa khi catalog hoặc giá hiện tại được chỉnh sửa.

**Câu hỏi truy vấn tiếp:** Vì sao vẫn giữ liên kết product/variant nếu đã có snapshot?

**Ý trả lời truy vấn:** Liên kết hỗ trợ truy vấn và phân tích; snapshot bảo toàn bằng chứng giao dịch tại thời điểm mua.

### Câu 8. Hệ thống quản lý các ngoại lệ vận hành nào?

**Đáp án mẫu:** FastGuy có nhánh hủy đơn, thanh toán quá hạn, giao thất bại, trả hàng về cửa hàng, hoàn tiền chờ xử lý, thiếu/thừa COD và biến động tồn kho. Các ngoại lệ được biểu diễn bằng trạng thái riêng thay vì ghi đè hoặc xóa dữ liệu. Báo cáo cũng tách gross, refund, net và các exception để quản trị thấy chất lượng vận hành, không chỉ doanh thu.

**Câu hỏi truy vấn tiếp:** Vì sao không xóa đơn thất bại hoặc đã hủy?

**Ý trả lời truy vấn:** Giữ đơn và history phục vụ đối soát, audit, báo cáo, hoàn tồn và xử lý tranh chấp.

### Câu 9. Nguồn sự thật cho API và dữ liệu của FastGuy là gì?

**Đáp án mẫu:** Với endpoint đã contract hóa, OpenAPI 3.1 là nguồn chuẩn về request, response, enum và status code. Với dữ liệu, schema SQL Server thực tế, SQL canonical/migration và JPA mapping phải được đối chiếu; không suy đoán schema từ giao diện. Backend vẫn là nơi thực thi rule nghiệp vụ và là trust boundary cho mọi input từ browser.

**Câu hỏi truy vấn tiếp:** Nếu frontend đang dùng field không có trong OpenAPI thì theo bên nào?

**Ý trả lời truy vấn:** Phải xác minh và sửa contract/provider/consumer có kiểm soát; không hợp thức hóa field chỉ vì UI đang giả định.

### Câu 10. Phạm vi hiện tại của FastGuy có điểm nào cần nói thận trọng?

**Đáp án mẫu:** Có các hạn chế thật: logout chỉ phía client, JWT không có refresh token hay blacklist, chưa auto assignment shipper, loyalty chưa dùng điểm tại checkout, Cloudinary upload unsigned trực tiếp từ browser, scheduler chạy in-process và DTO còn chủ yếu là `Map<String,Object>`. Đây là hiện trạng cần trình bày trung thực. Các phương án khắc phục chỉ được gọi là **đề xuất**, không nói như tính năng đã tồn tại.

**Câu hỏi truy vấn tiếp:** Có thể khẳng định hệ thống đã triển khai production không?

**Ý trả lời truy vấn:** Không. Source và build/test chứng minh phần mềm, nhưng không có bằng chứng để tuyên bố deployment production.

## Nhóm 2. Kiến trúc và thiết kế hệ thống

### Câu 11. Kiến trúc backend FastGuy được phân lớp ra sao?

**Đáp án mẫu:** Servlet là HTTP boundary: đọc token, path, query/body, validate hình dạng input và trả status code. Service giữ rule nghiệp vụ, transaction, lock và phối hợp nhiều DAO/entity. DAO/JPA thực hiện truy vấn SQL Server; entity ánh xạ dữ liệu. Cách chia này tránh nhét nghiệp vụ vào servlet hoặc giao trực tiếp entity cho browser.

**Câu hỏi truy vấn tiếp:** Transaction nên bắt đầu ở DAO hay Service?

**Ý trả lời truy vấn:** Với use case gồm nhiều thay đổi liên quan, Service nên sở hữu transaction để tất cả cùng commit hoặc rollback.

### Câu 12. Vai trò của Servlet trong FastGuy là gì?

**Đáp án mẫu:** Servlet ánh xạ URL, phân biệt HTTP method, xác thực JWT/role, parse request và chuyển lỗi nghiệp vụ thành phản hồi API phù hợp như 400, 401, 403, 404 hoặc 409. Servlet không nên tự quyết định tồn kho, tiền hay transition phức tạp; các rule đó thuộc Service. Điều này giữ HTTP concern tách khỏi nghiệp vụ.

**Câu hỏi truy vấn tiếp:** Vì sao vẫn phải validate ở Servlet nếu Service có kiểm tra?

**Ý trả lời truy vấn:** Servlet chặn input sai định dạng tại trust boundary; Service tiếp tục bảo vệ invariant nghiệp vụ và caller khác.

### Câu 13. JPA/Hibernate được dùng để làm gì?

**Đáp án mẫu:** JPA định nghĩa mapping entity, quan hệ và lifecycle; Hibernate là provider thực thi trên SQL Server. Service có thể dùng `EntityManager`, JPQL, native query và `LockModeType.PESSIMISTIC_WRITE` trong transaction. Việc dùng ORM không loại bỏ nhu cầu hiểu constraint, index, isolation và schema thực tế.

**Câu hỏi truy vấn tiếp:** ORM có ngăn mọi race condition không?

**Ý trả lời truy vấn:** Không; race condition vẫn cần transaction, lock, unique constraint và kiểm tra trạng thái ngay trên bản ghi đã khóa.

### Câu 14. Frontend Vue được tổ chức theo vai trò nào?

**Đáp án mẫu:** Vue 3 dùng Composition API và `<script setup>` cho view/component; Pinia giữ state dùng chung như auth, order và notification; Vue Router tách khu vực guest, user, staff, shipper và admin. API client là lớp gọi backend, nhưng không được coi state frontend là bằng chứng quyền hoặc thanh toán.

**Câu hỏi truy vấn tiếp:** Route guard frontend có thay thế kiểm tra role backend không?

**Ý trả lời truy vấn:** Không. Route guard chỉ cải thiện UX; request vẫn có thể được gọi trực tiếp nên backend phải xác thực và phân quyền lại.

### Câu 15. OpenAPI 3.1 đóng vai trò gì trong dự án?

**Đáp án mẫu:** `openapi/fastguy.yaml` mô tả contract của endpoint đã chuẩn hóa: schema request/response, field required, enum, nullability, status code và lỗi. Nó giúp backend serialization, frontend consumer và contract test cùng đối chiếu một nguồn. Khi thay đổi API, contract phải đổi trước implementation và consumer.

**Câu hỏi truy vấn tiếp:** OpenAPI có thay thế test runtime không?

**Ý trả lời truy vấn:** Không; vẫn phải lint contract và kiểm tra output backend/fixture frontend thực sự khớp schema.

### Câu 16. Tại sao backend được gọi là trust boundary?

**Đáp án mẫu:** Browser, query string, JWT gửi lên, giá hiển thị và callback return đều có thể bị sửa. Backend phải xác minh token, role đang active, ownership, giá, tồn, trạng thái dự kiến, chữ ký webhook và dữ liệu provider trước khi ghi. Đây là ranh giới quyết định dữ liệu nào đủ tin để làm thay đổi nghiệp vụ.

**Câu hỏi truy vấn tiếp:** JWT hợp lệ có đủ cho mọi thao tác không?

**Ý trả lời truy vấn:** Không; còn phải kiểm tra role, trạng thái tài khoản, ownership, ca đang check-in và trạng thái resource tùy endpoint.

### Câu 17. FastGuy quản lý `EntityManager` và lỗi transaction thế nào?

**Đáp án mẫu:** Service tạo `EntityManager`, bắt đầu transaction cho mutation, commit khi toàn bộ invariant đạt. Khi có `RuntimeException`, nếu transaction còn active thì rollback; `EntityManager` được đóng trong `finally`. Mẫu này tránh partial write và rò rỉ tài nguyên kết nối.

**Câu hỏi truy vấn tiếp:** Vì sao không giữ một `EntityManager` singleton?

**Ý trả lời truy vấn:** `EntityManager` không phù hợp chia sẻ tự do giữa request/thread; phạm vi theo use case giúp transaction và lifecycle rõ ràng.

### Câu 18. Tại sao FastGuy dùng cả pessimistic locking và expected state?

**Đáp án mẫu:** Pessimistic lock tuần tự hóa các mutation tranh chấp trên cùng bản ghi. `expectedStatus` phát hiện client đang thao tác trên dữ liệu cũ: sau khi khóa, backend so trạng thái hiện tại với trạng thái client đã nhìn thấy; lệch thì trả conflict thay vì áp dụng mù. Hai cơ chế bảo vệ hai lớp: cạnh tranh trong DB và stale intent từ client.

**Câu hỏi truy vấn tiếp:** Chỉ dùng lock mà bỏ `expectedStatus` có vấn đề gì?

**Ý trả lời truy vấn:** Request cũ vẫn có thể chờ lock rồi ghi lên trạng thái mới, tạo hành động hợp lệ về kỹ thuật nhưng sai ý người dùng.

### Câu 19. Hạn chế của DTO hiện tại là gì?

**Đáp án mẫu:** Nhiều API hiện dùng `Map<String,Object>` cho request hoặc response. Cách này nhanh và linh hoạt nhưng làm contract compile-time yếu, dễ sai tên field, ép kiểu runtime và khó refactor. **Đề xuất:** chuyển dần endpoint quan trọng sang request/response DTO typed, đồng bộ OpenAPI và contract test, không rewrite toàn hệ thống cùng lúc.

**Câu hỏi truy vấn tiếp:** Vì sao không đổi toàn bộ Map ngay?

**Ý trả lời truy vấn:** Blast radius lớn; nên ưu tiên boundary có rủi ro cao như checkout, payment, refund và transition.

### Câu 20. Nếu mở rộng nhiều instance, kiến trúc hiện tại gặp điểm gì?

**Đáp án mẫu:** DB transaction và lock vẫn bảo vệ nhiều mutation, nhưng scheduler hiện chạy in-process từ `AppStartupListener`; mỗi instance có thể khởi tạo một daemon và xử lý cùng job mỗi phút. **Đề xuất:** dùng distributed lock/leader election hoặc scheduler ngoài tiến trình, đồng thời giữ job idempotent. Đây là cải tiến, không phải khả năng hiện có.

**Câu hỏi truy vấn tiếp:** Chỉ cấu hình một instance có đủ lâu dài không?

**Ý trả lời truy vấn:** Đủ cho phạm vi đơn instance, nhưng không phải giải pháp scale-out hoặc high availability.

## Nhóm 3. Người dùng, xác thực và phân quyền

### Câu 21. FastGuy xác thực đăng nhập như thế nào?

**Đáp án mẫu:** Người dùng đăng nhập bằng số điện thoại hoặc email và mật khẩu. `AuthService` tìm user, khóa pessimistic bản ghi trước khi cập nhật số lần sai, kiểm tra password hash và chỉ cho tài khoản `ACTIVE` đăng nhập. Khi thành công, backend phát JWT chứa `userId`, `role`, thời điểm phát hành và hết hạn.

**Câu hỏi truy vấn tiếp:** Vì sao khóa bản ghi user lúc đăng nhập?

**Ý trả lời truy vấn:** Để các request đăng nhập đồng thời không làm mất cập nhật bộ đếm sai hoặc vượt qua chính sách khóa tạm.

### Câu 22. JWT của FastGuy có thời hạn và nội dung gì?

**Đáp án mẫu:** JWT có `userId`, `role`, `issuedAt`, `expiration` và được ký bằng secret của backend. Thời hạn hiện tại là 86.400.000 ms, tức 24 giờ. Backend validate chữ ký/hết hạn rồi vẫn kiểm tra quyền và trạng thái actor theo nghiệp vụ.

**Câu hỏi truy vấn tiếp:** JWT có refresh token không?

**Ý trả lời truy vấn:** Chưa. Hệ thống hiện không có refresh token; hết 24 giờ người dùng phải đăng nhập lại.

### Câu 23. Mật khẩu được bảo vệ ra sao?

**Đáp án mẫu:** FastGuy dùng PBKDF2 để hash mật khẩu thay vì lưu plaintext hoặc hash nhanh. Khi đăng ký hay đổi mật khẩu, backend kiểm tra chính sách rồi lưu hash; khi đăng nhập chỉ so sánh qua hàm check. Source còn có cơ chế nâng cấp mật khẩu cũ chưa hash sau lần đăng nhập đúng.

**Câu hỏi truy vấn tiếp:** Tại sao PBKDF2 phù hợp hơn SHA-256 thuần?

**Ý trả lời truy vấn:** PBKDF2 có salt và nhiều vòng lặp, làm brute-force tốn chi phí hơn hash nhanh dùng một vòng.

### Câu 24. Cơ chế chống brute-force hoạt động thế nào?

**Đáp án mẫu:** `AuthService` đếm `failed_login_attempts`; sau 5 lần sai, đặt `locked_until` trong 15 phút. Bản ghi được khóa pessimistic khi cập nhật nên request đồng thời không dễ làm sai bộ đếm. Khi hết thời gian khóa hoặc đăng nhập đúng, bộ đếm và mốc khóa được reset.

**Câu hỏi truy vấn tiếp:** Chính sách này đã thay thế rate limit theo IP chưa?

**Ý trả lời truy vấn:** Chưa. Đây là khóa theo tài khoản; rate limiting theo IP/device là đề xuất bổ sung nếu triển khai internet quy mô lớn.

### Câu 25. Phân quyền bốn role được thực thi ở đâu?

**Đáp án mẫu:** Frontend ẩn/hiện route và thao tác theo role để cải thiện UX, nhưng backend mới là nơi quyết định. Servlet đọc Bearer token, validate claims, kiểm tra role yêu cầu; các endpoint đặc quyền còn kiểm tra user/role đang active hoặc điều kiện ca. Service tiếp tục kiểm tra actor cho transition và ownership.

**Câu hỏi truy vấn tiếp:** Nếu sửa Pinia để tự nhận role ADMIN thì sao?

**Ý trả lời truy vấn:** Chỉ giao diện bị thay đổi; backend không cấp quyền nếu JWT và active role không hợp lệ.

### Câu 26. Quyền của `USER` đối với đơn hàng được giới hạn thế nào?

**Đáp án mẫu:** User chỉ thao tác trên đơn thuộc tài khoản mình. Với transition thông thường, user chủ yếu được hủy khi đơn còn `PENDING`; không thể tự xác nhận, chuẩn bị, gán shipper hay báo giao thành công. Ownership và trạng thái được kiểm tra lại trong transaction, không dựa vào order object phía client.

**Câu hỏi truy vấn tiếp:** User có thể hủy khi đơn đã `READY` không?

**Ý trả lời truy vấn:** Không theo policy hiện tại; customer không được hủy ở `READY`, tránh phá vỡ đơn đã chuẩn bị xong.

### Câu 27. `STAFF` và `SHIPPER` khác nhau ở đâu?

**Đáp án mẫu:** Staff xử lý hàng đợi bếp, đưa đơn qua `CONFIRMED`, `PREPARING`, `READY`, điều phối và xử lý giao thất bại/trả cửa hàng theo rule. Shipper chỉ thao tác các đơn được gán cho mình, chủ yếu `PICKED_UP`, `DELIVERED`, `DELIVERY_FAILED` và nghiệp vụ COD. Nhiều thao tác yêu cầu danh tính active và ca đã check-in.

**Câu hỏi truy vấn tiếp:** Shipper có thể tự lấy bất kỳ đơn `READY` nào không?

**Ý trả lời truy vấn:** Không; đơn phải được staff gán, shipper hợp lệ và đang trong ca.

### Câu 28. `ADMIN` có toàn quyền bỏ qua mọi invariant không?

**Đáp án mẫu:** Không. Admin có endpoint quản trị và một số quyền override có audit, nhưng vẫn phải có JWT hợp lệ, role active, input đúng và trạng thái phù hợp. Ví dụ override giới hạn giao vẫn khóa đơn, kiểm tra `expectedStatus`, validate note và ghi `OrderStatusHistory`.

**Câu hỏi truy vấn tiếp:** Vì sao override cũng cần history?

**Ý trả lời truy vấn:** Vì quyền cao tạo rủi ro cao; audit phải cho biết ai thay đổi, lúc nào và lý do gì.

### Câu 29. Logout hiện tại có vô hiệu hóa JWT không?

**Đáp án mẫu:** Chưa. Logout hiện xóa state/token phía client và điều hướng về trang công khai; JWT đã phát vẫn hợp lệ đến khi hết hạn nếu bị sao chép. **Đề xuất:** refresh token rotation, revoke/blacklist theo session hoặc giảm access-token TTL tùy mô hình triển khai.

**Câu hỏi truy vấn tiếp:** Có nên nói hệ thống đã có server-side session revocation không?

**Ý trả lời truy vấn:** Không; source hiện không có refresh token hay blacklist.

### Câu 30. Guest PayOS return proof bảo vệ điều gì?

**Đáp án mẫu:** Guest không có JWT ownership, nên luồng quay về từ PayOS cần proof riêng. Backend lưu hash proof vào đơn guest và dùng nó để chứng minh browser quay lại đúng luồng, nhưng trạng thái paid vẫn phải dựa vào webhook HMAC hoặc truy vấn provider đã đối chiếu reference và amount. Proof không thay chữ ký thanh toán.

**Câu hỏi truy vấn tiếp:** Nếu client gửi `success=true` trên URL return thì đơn có được paid không?

**Ý trả lời truy vấn:** Không; client return không phải nguồn tin cậy để xác nhận tiền.

## Nhóm 4. Vòng đời đơn hàng

### Câu 31. Các trạng thái đơn hàng của FastGuy là gì?

**Đáp án mẫu:** Các trạng thái gồm `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `ASSIGNED`, `PICKED_UP`, `DELIVERY_FAILED`, `RETURNED_TO_STORE`, `DELIVERED`, `CANCELLED`. Đây là state machine nghiệp vụ; không phải mọi cặp trạng thái đều được chuyển trực tiếp. Ba trạng thái kết thúc chính là `DELIVERED`, `RETURNED_TO_STORE`, `CANCELLED`.

**Câu hỏi truy vấn tiếp:** `DELIVERY_FAILED` có phải trạng thái kết thúc không?

**Ý trả lời truy vấn:** Không; có thể thử lại về `PICKED_UP` hoặc được staff xác nhận `RETURNED_TO_STORE`.

### Câu 32. Transition hợp lệ được định nghĩa thế nào?

**Đáp án mẫu:** `OrderTransitionService` giữ map transition: `PENDING` sang `CONFIRMED/CANCELLED`; `CONFIRMED` sang `PREPARING/CANCELLED`; `PREPARING` sang `READY/CANCELLED`; `READY` sang `ASSIGNED/CANCELLED`; `ASSIGNED` sang `PICKED_UP/CANCELLED`; `PICKED_UP` sang `DELIVERED/DELIVERY_FAILED`; `DELIVERY_FAILED` sang `PICKED_UP/RETURNED_TO_STORE`. Sau đó role policy tiếp tục thu hẹp action được phép.

**Câu hỏi truy vấn tiếp:** Tại sao map transition chưa đủ?

**Ý trả lời truy vấn:** Cùng một cạnh còn phụ thuộc role, ownership, payment, shift và dữ liệu hiện tại của đơn.

### Câu 33. `OrderStatusHistory` lưu giá trị gì?

**Đáp án mẫu:** History lưu order, actor user ID nếu có, actor role, trạng thái trước, trạng thái sau, lý do/note và thời điểm. Nó được ghi cùng transaction với mutation trạng thái. Nhờ vậy có thể dựng timeline, giải thích ngoại lệ và kiểm tra thao tác đặc quyền.

**Câu hỏi truy vấn tiếp:** Nếu transaction chuyển trạng thái rollback thì history thế nào?

**Ý trả lời truy vấn:** History trong cùng transaction cũng rollback, tránh log nói đã đổi trong khi đơn chưa đổi.

### Câu 34. Checkout idempotency bảo vệ trường hợp nào?

**Đáp án mẫu:** Client có thể retry do timeout hoặc double-click. FastGuy dùng idempotency key, request hash và owner để nhận diện cùng ý định checkout: cùng key, cùng owner, cùng payload trả lại kết quả trước; key trùng nhưng hash hoặc owner khác phải bị từ chối. Cách này tránh tạo hai đơn và reserve tồn hai lần.

**Câu hỏi truy vấn tiếp:** Vì sao chỉ unique idempotency key chưa đủ?

**Ý trả lời truy vấn:** Cần hash và owner để ngăn một key bị tái sử dụng cho payload khác hoặc giữa hai chủ thể khác nhau.

### Câu 35. Request hash trong checkout có ý nghĩa gì?

**Đáp án mẫu:** Request hash là dấu vân tay ổn định của nội dung checkout có ý nghĩa nghiệp vụ. Khi retry, backend so hash đã lưu để biết request có thực sự giống lần đầu hay không. Nếu client đổi giỏ hàng, địa chỉ, coupon hoặc phương thức thanh toán mà giữ key cũ, hệ thống phải coi đó là conflict.

**Câu hỏi truy vấn tiếp:** Hash có thay thế validation payload không?

**Ý trả lời truy vấn:** Không; payload vẫn phải được validate và backend tính lại giá/tồn.

### Câu 36. Vì sao checkout dùng pessimistic locking?

**Đáp án mẫu:** Nhiều khách có thể mua cùng variant ở cùng thời điểm. Trong transaction, backend khóa dữ liệu cần thiết, kiểm tra tồn khả dụng rồi tạo reservation và đơn; request sau thấy số lượng đã thay đổi. Lock ngăn oversell do hai request cùng đọc một giá trị cũ trước khi trừ.

**Câu hỏi truy vấn tiếp:** Lock có nên giữ trong lúc gọi PayOS qua mạng không?

**Ý trả lời truy vấn:** Không nên; transaction DB cần ngắn, còn tạo payment link được tách bằng payment attempt/lease để tránh giữ lock qua network call.

### Câu 37. `expectedStatus` xử lý cập nhật đồng thời thế nào?

**Đáp án mẫu:** Client gửi trạng thái mà nó đã quan sát. Service khóa đơn, sau đó so `expectedStatus` với `orderStatus` hiện tại; nếu khác trả conflict, không thực hiện mutation. Ví dụ hai nhân viên cùng thao tác đơn `READY`, request đến sau không thể ghi đè kết quả của request trước bằng dữ liệu cũ.

**Câu hỏi truy vấn tiếp:** HTTP status phù hợp cho trường hợp này là gì?

**Ý trả lời truy vấn:** `409 Conflict`, vì request hợp lệ về cú pháp nhưng xung đột với state hiện tại.

### Câu 38. Luồng giao thất bại được thiết kế ra sao?

**Đáp án mẫu:** Từ `PICKED_UP`, shipper có thể chuyển sang `DELIVERY_FAILED` với lý do và dữ liệu retry theo policy. Hệ thống cho phép thử lại về `PICKED_UP`; khi không tiếp tục, staff xử lý `RETURNED_TO_STORE`. Các lần thử, giới hạn và override của admin phải được kiểm soát và ghi history.

**Câu hỏi truy vấn tiếp:** Tại sao không chuyển thẳng từ giao thất bại sang hủy?

**Ý trả lời truy vấn:** Hàng đã rời cửa hàng cần quy trình trả về, xử lý tồn và hoàn tiền rõ ràng; `RETURNED_TO_STORE` biểu diễn đúng sự kiện đó.

### Câu 39. Hủy đơn tác động những dữ liệu nào?

**Đáp án mẫu:** Service khóa đơn, kiểm tra actor/ownership/trạng thái, release reservation hoặc khôi phục tồn theo luồng tương thích, nhả coupon, đặt `CANCELLED`, thời điểm và actor hủy, rồi ghi history. Nếu đơn đã `PAID`, `refundStatus` chuyển `PENDING`; không tự coi tiền đã hoàn.

**Câu hỏi truy vấn tiếp:** Hủy đơn paid có đồng nghĩa refund thành công không?

**Ý trả lời truy vấn:** Không; hủy tạo yêu cầu hoàn `PENDING`, còn refund là quy trình riêng có kiểm chứng.

### Câu 40. Khi nào một đơn được giao thành công?

**Đáp án mẫu:** Đơn phải ở cạnh transition phù hợp, thuộc shipper thực hiện, actor đủ điều kiện ca và payment status đáp ứng policy. `OrderTransitionService.canDeliver` yêu cầu trạng thái thanh toán `PAID`; với COD, thu tiền khi giao phải cập nhật dữ liệu thu COD để đơn được coi paid/delivered nhất quán. Mutation được khóa và ghi history.

**Câu hỏi truy vấn tiếp:** Client tự gửi `paymentStatus=PAID` có đủ không?

**Ý trả lời truy vấn:** Không; backend quyết định từ luồng PayOS đã xác minh hoặc nghiệp vụ thu COD hợp lệ.

## Nhóm 5. Thanh toán, PayOS và hoàn tiền

### Câu 41. FastGuy hỗ trợ các phương thức thanh toán nào?

**Đáp án mẫu:** Hiện có `COD` và `BANK_TRANSFER`. `BANK_TRANSFER` tích hợp PayOS để tạo payment link và nhận/truy vấn trạng thái; COD được thu khi giao và đưa vào đối soát theo shipper, ca. Hai phương thức dùng cùng đơn hàng nhưng có nguồn xác nhận tiền và timeout khác nhau.

**Câu hỏi truy vấn tiếp:** Có thanh toán thẻ trực tiếp trong source không?

**Ý trả lời truy vấn:** Không nên khẳng định; contract hiện nêu COD và chuyển khoản PayOS.

### Câu 42. PayOS payment link được tạo an toàn thế nào?

**Đáp án mẫu:** Service khóa đơn, chỉ nhận đơn `BANK_TRANSFER`, `PENDING`, kiểm tra attempt hiện có. `PaymentAttempt` dùng trạng thái `CREATING/READY/FAILED/PAID` và lease token để một caller sở hữu lần gọi provider; network call nằm ngoài transaction dài. Khi lưu kết quả, backend khóa lại và đối chiếu order code, amount, paymentLinkId và checkout URL.

**Câu hỏi truy vấn tiếp:** Vì sao cần payment attempt nếu checkout đã idempotent?

**Ý trả lời truy vấn:** Checkout idempotent chống tạo trùng đơn; payment attempt chống tạo/lưu link provider trùng hoặc kết quả của caller cũ.

### Câu 43. Webhook PayOS được xác thực thế nào?

**Đáp án mẫu:** Endpoint webhook không tin payload chỉ vì có `success=true`. Service lấy `data` và `signature`, kiểm tra HMAC theo cơ chế PayOS; sau đó kiểm tra code thành công, orderCode, amount, paymentLinkId và payment attempt đã lưu. Chỉ khi mọi giá trị khớp, đơn bị khóa mới được đánh dấu `PAID`.

**Câu hỏi truy vấn tiếp:** Chữ ký đúng nhưng amount sai thì sao?

**Ý trả lời truy vấn:** Không cập nhật paid; amount phải khớp giá trị cuối của đơn và attempt.

### Câu 44. Vì sao không tin client confirm payment?

**Đáp án mẫu:** Client và URL return có thể bị sửa, replay hoặc mở trực tiếp. Nguồn đáng tin là webhook HMAC hợp lệ hoặc API PayOS trả dữ liệu được đối chiếu với reference và amount lưu trong DB. Client chỉ yêu cầu backend kiểm tra hoặc hiển thị kết quả, không có quyền quyết định `PAID`.

**Câu hỏi truy vấn tiếp:** Nếu webhook đến chậm thì người dùng làm gì?

**Ý trả lời truy vấn:** Backend có thể reconcile bằng truy vấn PayOS; vẫn không lấy query parameter của browser làm bằng chứng.

### Câu 45. Timeout chuyển khoản 15 phút được xử lý ra sao?

**Đáp án mẫu:** Đơn `BANK_TRANSFER` chưa thanh toán chỉ giữ cửa sổ chờ 15 phút. Scheduler định kỳ tìm đơn quá hạn, khóa và hủy theo điều kiện còn phù hợp, đồng thời release tài nguyên liên quan. Nếu thanh toán và timeout chạy gần nhau, lock cùng việc reconcile payment giúp tránh hủy mù một đơn đã được PayOS xác nhận.

**Câu hỏi truy vấn tiếp:** Timeout có phải do frontend đếm ngược quyết định không?

**Ý trả lời truy vấn:** Không; đồng hồ UI chỉ hiển thị, backend scheduler và state trong DB mới thực thi.

### Câu 46. Timeout COD 3 giờ khác timeout PayOS thế nào?

**Đáp án mẫu:** COD không chờ cổng thanh toán nhưng đơn `PENDING` không thể tồn tại vô hạn, nên policy hiện có timeout 3 giờ. Chuyển khoản có timeout 15 phút vì reservation gắn với phiên thanh toán ngắn. Cả hai phải kiểm tra trạng thái hiện tại trong transaction trước khi hủy để không tác động đơn đã tiến triển.

**Câu hỏi truy vấn tiếp:** Có nên dùng cùng một timeout cho mọi phương thức không?

**Ý trả lời truy vấn:** Không; đặc tính nghiệp vụ và thời gian chờ xác nhận của COD và PayOS khác nhau.

### Câu 47. Điều kiện tạo refund là gì?

**Đáp án mẫu:** Refund chỉ áp dụng cho đơn đã `PAID`, có order status `CANCELLED` hoặc `RETURNED_TO_STORE`, và `refundStatus` đang `PENDING`. Service phải khóa đơn để ngăn hai admin hoàn cùng lúc. Các trạng thái khác bị từ chối thay vì tạo refund tùy ý.

**Câu hỏi truy vấn tiếp:** Đơn `DELIVERED` có được hoàn qua luồng này không?

**Ý trả lời truy vấn:** Không theo điều kiện hiện tại; cần quy trình return/refund khác nếu sản phẩm đã giao.

### Câu 48. Refund và payment status được phân biệt thế nào?

**Đáp án mẫu:** `paymentStatus=PAID` ghi nhận tiền đã từng được thu; refund status ghi nhận vòng đời hoàn tiền. Không nên đổi paid thành unpaid để biểu diễn refund vì sẽ mất lịch sử dòng tiền. Báo cáo dùng gross, refund và net để phản ánh đúng cả thu và hoàn.

**Câu hỏi truy vấn tiếp:** Tại sao cách này tốt cho audit?

**Ý trả lời truy vấn:** Có thể chứng minh đơn đã thu bao nhiêu, hoàn bao nhiêu và thời điểm hai sự kiện độc lập.

### Câu 49. Điểm thưởng được xử lý thế nào khi hoàn tiền?

**Đáp án mẫu:** Loyalty hiện hỗ trợ earn, history, tier và reversal khi refund. Nếu đơn từng cộng điểm rồi được hoàn, `RefundService` phối hợp đảo điểm để số dư và lịch sử phản ánh giao dịch bị hoàn. Việc đảo phải idempotent hoặc được bảo vệ trạng thái để không trừ hai lần.

**Câu hỏi truy vấn tiếp:** Khách đã có thể dùng điểm giảm tiền ở checkout chưa?

**Ý trả lời truy vấn:** Chưa; redeem điểm tại checkout là đề xuất, không phải hiện trạng.

### Câu 50. Rủi ro quan trọng nhất của tích hợp thanh toán là gì?

**Đáp án mẫu:** Các rủi ro gồm giả mạo webhook, amount/reference mismatch, callback lặp, race giữa timeout và paid, tạo link trùng, refund lặp và lộ secret. FastGuy giảm rủi ro bằng HMAC, đối chiếu dữ liệu provider, pessimistic lock, payment attempt/lease và state guard. **Đề xuất** khi production: observability, retry queue bền vững, secret rotation và đối soát định kỳ.

**Câu hỏi truy vấn tiếp:** Có nên log toàn bộ secret hoặc signature để debug không?

**Ý trả lời truy vấn:** Không; chỉ log metadata an toàn, correlation ID và kết quả xác minh.

## Nhóm 6. Tồn kho, reservation và sản phẩm

### Câu 51. Inventory reservation giải quyết vấn đề gì?

**Đáp án mẫu:** Reservation tách “đã hứa cho đơn” khỏi “đã tiêu thụ thật”. Khi checkout, FastGuy reserve số lượng để khách khác không mua vượt tồn; khi đơn hoàn tất phù hợp thì consume, khi hủy thì release, khi hàng mất/hỏng có thể wasted. Cách này mô hình hóa vòng đời tồn thay vì chỉ trừ rồi cộng thủ công.

**Câu hỏi truy vấn tiếp:** Tại sao không trừ tồn vĩnh viễn ngay lúc thêm giỏ?

**Ý trả lời truy vấn:** Giỏ có thể bị bỏ; reservation chỉ nên tạo tại checkout có transaction và timeout rõ ràng.

### Câu 52. Bốn trạng thái inventory là gì?

**Đáp án mẫu:** `RESERVED` là đã giữ cho đơn; `CONSUMED` là đã dùng/giao theo nghiệp vụ; `RELEASED` là trả lượng giữ về khả dụng; `WASTED` là không thể trả lại tồn bán được. Transition tồn phải đi cùng trạng thái đơn và được audit, không sửa số lượng không dấu vết.

**Câu hỏi truy vấn tiếp:** Đơn `RETURNED_TO_STORE` luôn release toàn bộ sao?

**Ý trả lời truy vấn:** Không nên giả định; hàng có thể usable hoặc wasted, cần policy/đánh giá thực tế trước khi ghi biến động.

### Câu 53. `quantityAvailable = null` có nghĩa gì?

**Đáp án mẫu:** Với variant có `quantityAvailable` là `null`, FastGuy coi đó là không quản lý giới hạn số lượng. Backend không được hiểu `null` là 0, vì như vậy sẽ khóa bán nhầm sản phẩm không theo dõi tồn. Variant có số cụ thể mới chịu kiểm tra và cập nhật giới hạn.

**Câu hỏi truy vấn tiếp:** Tại sao không dùng một số rất lớn thay cho null?

**Ý trả lời truy vấn:** Null biểu diễn đúng semantics “không quản lý”; số giả tạo báo cáo sai và vẫn có trần tùy ý.

### Câu 54. Ledger tồn kho dùng để làm gì?

**Đáp án mẫu:** Inventory ledger lưu biến động có nguyên nhân, số lượng, đối tượng liên quan và thời điểm. Nó giúp truy ngược vì sao tồn thay đổi do reserve, consume, release, wasted hoặc adjustment, thay vì chỉ nhìn số hiện tại. Admin có thể lọc ledger để audit và điều tra chênh lệch.

**Câu hỏi truy vấn tiếp:** Ledger có thay thế cột số lượng hiện tại không?

**Ý trả lời truy vấn:** Không nhất thiết; số hiện tại phục vụ đọc nhanh, ledger phục vụ bằng chứng và đối soát.

### Câu 55. Làm sao chống oversell?

**Đáp án mẫu:** Checkout chạy trong transaction, khóa pessimistic bản ghi tồn/variant liên quan, kiểm tra số khả dụng rồi mới reserve. Mutation tiếp theo đọc state sau lock, vì vậy hai checkout đồng thời không cùng tiêu một lượng tồn. Constraint và rollback bảo đảm nếu một item thất bại thì toàn checkout không để lại reservation dở dang.

**Câu hỏi truy vấn tiếp:** Chỉ kiểm tra tồn ở frontend có đủ không?

**Ý trả lời truy vấn:** Không; dữ liệu có thể cũ hoặc bị sửa, và frontend không thể tuần tự hóa request đồng thời.

### Câu 56. Product variant được dùng thế nào?

**Đáp án mẫu:** `ProductVariant` gắn với product, có tên variant, giá, giá gốc, SKU, `quantityAvailable`, kích thước/khối lượng, default và status. Variant cho phép một sản phẩm có lựa chọn bán khác nhau mà vẫn quản lý giá/tồn riêng. Backend phải kiểm tra variant thuộc đúng product và đang hợp lệ khi checkout.

**Câu hỏi truy vấn tiếp:** Có thể gửi variant ID của sản phẩm khác không?

**Ý trả lời truy vấn:** Client có thể gửi nhưng backend phải từ chối vì quan hệ product–variant không khớp.

### Câu 57. Modifier được lưu trong đơn ra sao?

**Đáp án mẫu:** Modifier được cấu hình theo group/option ở catalog; khi tạo `OrderItem`, FastGuy lưu snapshot modifier JSON gồm option ID, group name, option name và price. Điều này giữ đúng lựa chọn và phụ thu tại thời điểm mua dù cấu hình modifier sau đó đổi.

**Câu hỏi truy vấn tiếp:** Vì sao không chỉ lưu danh sách modifier ID?

**Ý trả lời truy vấn:** ID không bảo toàn tên và giá lịch sử; snapshot cần cho hóa đơn, hỗ trợ và tranh chấp.

### Câu 58. Combo khác sản phẩm đơn như thế nào?

**Đáp án mẫu:** Combo gom nhiều thành phần thành một cấu hình bán. Khi xử lý, backend phải validate thành phần, lựa chọn và giá theo rule catalog hiện tại, đồng thời reserve đúng tài nguyên bị quản lý. Trong đơn vẫn cần snapshot đủ để đọc lại nội dung đã mua mà không phụ thuộc catalog đã đổi.

**Câu hỏi truy vấn tiếp:** Có nên tin cấu trúc combo do client gửi hoàn toàn không?

**Ý trả lời truy vấn:** Không; client chỉ gửi lựa chọn, backend phải hydrate và kiểm tra từ catalog tin cậy.

### Câu 59. Order item snapshot bảo vệ dữ liệu lịch sử thế nào?

**Đáp án mẫu:** `OrderItem` lưu `productName`, `variantName`, `unitPrice`, `totalPrice` và modifier JSON bên cạnh liên kết entity. Nếu admin đổi tên, giá hoặc vô hiệu hóa sản phẩm, đơn cũ vẫn hiển thị đúng giao dịch đã chốt. Đây là denormalization có chủ đích cho chứng từ lịch sử.

**Câu hỏi truy vấn tiếp:** Tổng tiền snapshot có cần backend tính lại lúc tạo không?

**Ý trả lời truy vấn:** Có; snapshot phải xuất phát từ giá backend xác minh, không sao chép tổng do browser cung cấp.

### Câu 60. Nếu reservation bị treo thì xử lý thế nào?

**Đáp án mẫu:** Timeout đơn sẽ đi qua scheduler để hủy đơn đủ điều kiện và release reservation. Job phải kiểm tra trạng thái reservation/đơn trong transaction để chạy lặp không giải phóng hai lần. **Đề xuất:** thêm metric reservation quá tuổi và job reconciliation độc lập nếu vận hành quy mô lớn.

**Câu hỏi truy vấn tiếp:** Vì sao job phải idempotent?

**Ý trả lời truy vấn:** Scheduler có thể retry hoặc chạy đồng thời; cùng một reservation không được release nhiều lần.

## Nhóm 7. Ca làm việc, điều phối shipper và COD

### Câu 61. FastGuy chuẩn hóa thời gian ca theo múi giờ nào?

**Đáp án mẫu:** Quy tắc ca dùng `Asia/Ho_Chi_Minh` làm business timezone. Việc chuẩn hóa tránh server chạy múi giờ khác làm sai ngày ca, cửa sổ check-in, giao hàng và đối soát. Dữ liệu thời gian phải được chuyển theo business time trước khi áp policy.

**Câu hỏi truy vấn tiếp:** Dùng `LocalDateTime.now()` tùy server có rủi ro gì?

**Ý trả lời truy vấn:** Kết quả có thể lệch nếu timezone JVM/server khác Việt Nam; business clock cần được chuẩn hóa.

### Câu 62. Grace check-in 15 phút có ý nghĩa gì?

**Đáp án mẫu:** Nhân viên hoặc shipper được check-in trong cửa sổ policy quanh giờ bắt đầu, với grace 15 phút theo rule hiện tại. Backend quyết định dựa trên business time và ca được phân công, không dựa vào việc nút UI còn hiện. Khi hợp lệ, trạng thái ca và `checkInAt` được cập nhật.

**Câu hỏi truy vấn tiếp:** Sửa giờ máy client có bypass được không?

**Ý trả lời truy vấn:** Không; backend dùng thời gian phía server/business clock để xác minh.

### Câu 63. Hệ thống chống xếp ca overlap thế nào?

**Đáp án mẫu:** Khi tạo hoặc sửa ca, backend kiểm tra khoảng thời gian của cùng người không giao nhau, kể cả ca qua ngày nếu policy hỗ trợ. Kiểm tra phải thực hiện trên dữ liệu DB và mutation phù hợp, không chỉ cảnh báo ở lịch frontend. Mục tiêu là một người không có hai ca đồng thời gây mâu thuẫn check-in và COD.

**Câu hỏi truy vấn tiếp:** Hai ca chạm đầu-cuối có phải overlap không?

**Ý trả lời truy vấn:** Theo mô hình khoảng nửa mở, ca trước kết thúc đúng lúc ca sau bắt đầu có thể hợp lệ; phải theo policy source.

### Câu 64. Vì sao cần checked-in gate?

**Đáp án mẫu:** Có role `STAFF` hoặc `SHIPPER` chưa đủ chứng minh người đó đang làm việc. Các service access kiểm tra danh tính active và ca hiện tại ở trạng thái `CHECKED_IN` trước thao tác vận hành. Gate này gắn quyền nghiệp vụ với lịch làm thực tế và giúp đối soát trách nhiệm.

**Câu hỏi truy vấn tiếp:** Shipper hết ca có xem lại đơn đã giao không?

**Ý trả lời truy vấn:** Policy cho phép đọc đơn sở hữu đã `DELIVERED` hoặc `CANCELLED`; thao tác vận hành mới vẫn bị gate theo ca.

### Câu 65. Điều kiện gán shipper là gì?

**Đáp án mẫu:** Đơn phải ở `READY`; shipper phải có role `SHIPPER`, trạng thái active và đang check-in trong ca phù hợp. Service khóa đơn, kiểm tra `expectedStatus`, gán shipper rồi chuyển `ASSIGNED` và ghi history. Không cho gán từ trạng thái tùy ý hoặc cho người ngoài ca.

**Câu hỏi truy vấn tiếp:** Vì sao chỉ gán đơn `READY`?

**Ý trả lời truy vấn:** Tránh shipper nhận đơn chưa hoàn tất chuẩn bị và giữ state machine rõ ràng.

### Câu 66. FastGuy có tự động phân shipper không?

**Đáp án mẫu:** Chưa. Hiện tại staff điều phối và chọn shipper đủ điều kiện; backend kiểm tra rule rồi mới gán. **Đề xuất:** auto assignment theo tải hiện tại, khoảng cách, ca và fairness, nhưng cần dữ liệu vị trí, SLA và cơ chế override trước khi triển khai.

**Câu hỏi truy vấn tiếp:** Tại sao không gọi danh sách shipper hợp lệ là auto assignment?

**Ý trả lời truy vấn:** Lọc ứng viên chỉ hỗ trợ người điều phối; hệ thống chưa tự chọn và tự gán.

### Câu 67. COD được ghi nhận khi giao thế nào?

**Đáp án mẫu:** Với COD, shipper thu tiền khi giao; backend phải xác nhận đơn thuộc shipper, đúng trạng thái, amount hợp lệ rồi cập nhật dữ liệu thu COD cùng kết quả `DELIVERED`. Số tiền thu được dùng để tính expected COD cho ca. Không để client tự sửa payment status tách rời giao hàng.

**Câu hỏi truy vấn tiếp:** Đơn chuyển khoản có vào expected COD không?

**Ý trả lời truy vấn:** Không; truy vấn settlement chỉ lấy đơn `COD`, `DELIVERED`, có `codCollectedAmount` trong cửa sổ ca.

### Câu 68. COD settlement được nhóm theo khóa nào?

**Đáp án mẫu:** Settlement được xác định theo cặp shipper và shift, có unique constraint để một ca không gửi nhiều bản bàn giao độc lập. Expected amount được tính từ các đơn COD delivered của shipper trong cửa sổ ca. Khi submit, service khóa ca và kiểm tra settlement hiện có để tránh trùng.

**Câu hỏi truy vấn tiếp:** Vì sao không chỉ nhóm theo ngày?

**Ý trả lời truy vấn:** Một shipper có thể nhiều ca trong ngày; shift là ranh giới trách nhiệm và thời gian chính xác hơn.

### Câu 69. Các trạng thái COD settlement có ý nghĩa gì?

**Đáp án mẫu:** `SUBMITTED` là shipper đã khai số tiền bàn giao; admin xác minh thành `SETTLED` nếu khớp, `SHORT` nếu thiếu hoặc `OVER` nếu thừa. Bản ghi giữ expected, submitted, verified amount, người nhận, lý do và thời điểm. `expectedStatus=SUBMITTED` bảo vệ bước verify khỏi cập nhật lặp/stale.

**Câu hỏi truy vấn tiếp:** Admin có thể verify settlement đã `SETTLED` lần nữa không?

**Ý trả lời truy vấn:** Không theo rule; verify yêu cầu trạng thái hiện tại và expected đều là `SUBMITTED`.

### Câu 70. Nếu shipper gửi COD hai lần cùng ca thì sao?

**Đáp án mẫu:** Service tìm settlement theo shipper+shift trong transaction và trả conflict nếu đã tồn tại; DB unique constraint là lớp bảo vệ cuối. Việc kết hợp application check, lock và constraint xử lý cả UX lẫn race condition. Không tạo hai record rồi cộng dồn tùy ý.

**Câu hỏi truy vấn tiếp:** Tại sao vẫn cần unique constraint nếu service đã kiểm tra?

**Ý trả lời truy vấn:** Hai transaction đồng thời có thể cùng chưa thấy record; constraint ngăn duplicate ở tầng dữ liệu.

## Nhóm 8. Coupon, điểm thưởng, review, hỗ trợ và thông báo

### Câu 71. Coupon trong FastGuy có các điều kiện chính nào?

**Đáp án mẫu:** Coupon có code, loại, giá trị, minimum order, maximum discount, giới hạn lượt dùng, số lượt đã dùng, hạn sử dụng, active và public. Backend phải kiểm tra mọi điều kiện và tự tính discount trong checkout. Client chỉ đề nghị code, không quyết định số tiền giảm.

**Câu hỏi truy vấn tiếp:** Vì sao cần `maxDiscount` với coupon phần trăm?

**Ý trả lời truy vấn:** Để giới hạn chi phí khuyến mại trên đơn giá trị lớn.

### Câu 72. `CouponRedemption` dùng để làm gì?

**Đáp án mẫu:** `CouponRedemption` ghi quan hệ coupon–user, order nếu đã dùng, thời điểm claim/use và discount amount. Nó hỗ trợ giới hạn theo người, audit coupon đã áp vào đơn nào và release khi đơn bị hủy theo policy. Bản ghi redemption chi tiết hơn chỉ tăng `used_count` tổng.

**Câu hỏi truy vấn tiếp:** Vì sao cần cả redemption và `used_count`?

**Ý trả lời truy vấn:** Redemption là lịch sử chi tiết; `used_count` hỗ trợ kiểm tra tổng nhanh nhưng phải giữ nhất quán trong transaction.

### Câu 73. Làm sao tránh coupon bị dùng vượt giới hạn khi checkout đồng thời?

**Đáp án mẫu:** Backend cần khóa coupon/redemption liên quan trong transaction, kiểm tra active, expiry, max uses và quyền người dùng rồi mới ghi redemption/tăng count. Checkout idempotency ngăn retry cùng đơn tiêu coupon hai lần. DB constraint phù hợp là lớp bảo vệ bổ sung cho uniqueness theo policy.

**Câu hỏi truy vấn tiếp:** Kiểm tra `used_count` trước transaction có đủ không?

**Ý trả lời truy vấn:** Không; nhiều request có thể cùng đọc count cũ và cùng vượt giới hạn.

### Câu 74. Loyalty hiện đã làm được gì?

**Đáp án mẫu:** Loyalty hiện hỗ trợ cộng điểm, xem lịch sử, xác định tier và đảo điểm khi refund. Điểm được gắn với user và giao dịch để audit thay vì chỉ sửa số dư không lý do. Hệ thống **chưa** hỗ trợ lấy điểm để giảm tiền tại checkout.

**Câu hỏi truy vấn tiếp:** Nếu hội đồng hỏi quy đổi điểm thành tiền thì trả lời sao?

**Ý trả lời truy vấn:** Đó là đề xuất tương lai; cần rule quy đổi, trần dùng, expiry, lock số dư và ledger trước khi triển khai.

### Câu 75. Điều kiện tạo review là gì?

**Đáp án mẫu:** Review yêu cầu user đã đăng nhập, đơn thuộc user và đã `DELIVERED`. Hệ thống áp uniqueness theo user/order để tránh đánh giá lặp cho cùng giao dịch. Rating/comment được validate ở backend; việc đã mua và đã giao không được suy ra từ frontend.

**Câu hỏi truy vấn tiếp:** Guest có review theo flow hiện tại không?

**Ý trả lời truy vấn:** Không nên khẳng định; policy review hiện gắn unique user/order và ownership user.

### Câu 76. `homepageConsent` bảo vệ điều gì?

**Đáp án mẫu:** Một review có thể tồn tại nhưng không đồng nghĩa được phép đưa lên trang chủ. `homepageConsent` lưu sự đồng ý riêng; chỉ review phù hợp, được chọn và có consent mới nên hiển thị công khai. Cách này tách quyền đánh giá dịch vụ khỏi quyền dùng nội dung làm testimonial.

**Câu hỏi truy vấn tiếp:** `isFeatured=true` có đủ hiển thị trang chủ không?

**Ý trả lời truy vấn:** Không; còn cần `homepageConsent=true` theo policy hiện tại.

### Câu 77. Support ticket có vòng đời thế nào?

**Đáp án mẫu:** User tạo ticket thuộc mình; staff/admin xử lý qua các transition trạng thái được service kiểm soát. Mỗi actor chỉ đọc/thao tác theo ownership hoặc role; input và trạng thái đích được validate. Không cho client ghi trực tiếp bất kỳ status nào vì sẽ bỏ qua quy trình hỗ trợ.

**Câu hỏi truy vấn tiếp:** Vì sao support ticket cũng cần state transition?

**Ý trả lời truy vấn:** Để phân biệt mới, đang xử lý, đã giải quyết/đóng và ngăn đóng hoặc mở lại trái policy.

### Câu 78. Notification theo role được phát và đọc ra sao?

**Đáp án mẫu:** Notification có thể nhắm người dùng hoặc role; `NotificationReceipt` ghi trạng thái nhận/đọc theo từng người thay vì sửa một cờ chung. Điều này đặc biệt quan trọng với thông báo role: một staff đọc không làm thông báo biến mất với staff khác. Frontend Pinia polling để cập nhật chuông và danh sách.

**Câu hỏi truy vấn tiếp:** Polling có phải push realtime không?

**Ý trả lời truy vấn:** Không; hiện là polling. WebSocket/SSE là đề xuất nếu cần độ trễ thấp hơn.

### Câu 79. Cloudinary được tích hợp thế nào?

**Đáp án mẫu:** Browser upload trực tiếp tới Cloudinary bằng unsigned upload preset; backend chỉ nhận và lưu URL kết quả. Lợi ích là giảm băng thông/tải cho server ứng dụng và đơn giản hóa upload media. Rủi ro là preset công khai có thể bị lạm dụng, khó kiểm soát loại/kích thước/nội dung nếu cấu hình Cloudinary yếu; backend cũng phải validate URL/domain trước khi lưu.

**Câu hỏi truy vấn tiếp:** Backend có giữ file nhị phân hiện tại không?

**Ý trả lời truy vấn:** Không theo luồng này; browser gửi thẳng Cloudinary, backend lưu URL.

### Câu 80. Nên cải tiến upload Cloudinary thế nào?

**Đáp án mẫu:** **Đề xuất:** chuyển nghiệp vụ nhạy cảm sang signed upload do backend cấp chữ ký ngắn hạn, giới hạn folder, MIME, kích thước và transformation; thêm moderation/quota nếu công khai. Có thể vẫn direct upload để giữ lợi ích không proxy file qua backend. Đây là hướng nâng cấp, không mô tả như đã triển khai.

**Câu hỏi truy vấn tiếp:** Signed upload có bắt backend truyền toàn bộ file không?

**Ý trả lời truy vấn:** Không; backend chỉ ký tham số, browser vẫn upload trực tiếp lên Cloudinary.

## Nhóm 9. API, dữ liệu, bảo mật và xử lý đồng thời

### Câu 81. Một request mutation đi qua những lớp nào?

**Đáp án mẫu:** Vue/Pinia gọi API; Servlet nhận request, parse và xác thực; Service kiểm tra rule, mở transaction và phối hợp DAO/entity; Hibernate phát truy vấn tới SQL Server; kết quả được serialize theo contract. Với mutation quan trọng, history/ledger được ghi trong cùng transaction trước khi trả response.

**Câu hỏi truy vấn tiếp:** Frontend có gọi DAO trực tiếp không?

**Ý trả lời truy vấn:** Không; DAO là lớp backend, browser chỉ giao tiếp qua HTTP API.

### Câu 82. API phân biệt 401, 403 và 409 thế nào?

**Đáp án mẫu:** `401` dùng khi thiếu hoặc token không hợp lệ; `403` khi danh tính đã biết nhưng không có role/quyền cần thiết; `409` khi request xung đột state hiện tại như `expectedStatus` lệch hoặc settlement đã tồn tại. Phân biệt đúng giúp frontend xử lý và audit chính xác hơn trả chung 400.

**Câu hỏi truy vấn tiếp:** Resource không tồn tại dùng mã nào?

**Ý trả lời truy vấn:** Thông thường `404`, trừ khi policy cố ý che giấu resource để tránh lộ thông tin.

### Câu 83. Input validation được đặt ở đâu?

**Đáp án mẫu:** Servlet validate cấu trúc như JSON, số dương, chuỗi không rỗng, enum/path; Service validate invariant như ownership, role, trạng thái, amount, tồn và quan hệ entity. SQL constraint tiếp tục bảo vệ uniqueness và integrity. Đây là defense in depth, không lặp vô ích.

**Câu hỏi truy vấn tiếp:** Vì sao DB constraint vẫn cần khi Service đã validate?

**Ý trả lời truy vấn:** DB là điểm hội tụ của request đồng thời và các caller khác; constraint chặn vi phạm cuối cùng.

### Câu 84. Pessimistic lock được dùng tại những điểm nào?

**Đáp án mẫu:** Source dùng `PESSIMISTIC_WRITE` ở đăng nhập để cập nhật brute-force counter, checkout/tồn, order transition/cancel, payment reconcile/webhook, refund, shift/settlement và các mutation tranh chấp khác. Lock được lấy trong transaction và giữ ngắn. Mục tiêu là đọc–kiểm tra–ghi trên cùng state đã khóa.

**Câu hỏi truy vấn tiếp:** Có nên khóa toàn bảng để đơn giản không?

**Ý trả lời truy vấn:** Không; khóa bản ghi cần thiết giảm contention và nguy cơ deadlock.

### Câu 85. Idempotency khác optimistic conflict thế nào?

**Đáp án mẫu:** Idempotency nói rằng cùng một ý định retry phải cho cùng kết quả, điển hình checkout với key/hash/owner. Optimistic conflict qua `expectedStatus` nói rằng ý định dựa trên state cũ không được áp lên state mới. Một use case có thể cần cả hai vì retry mạng và thao tác đồng thời là hai vấn đề khác nhau.

**Câu hỏi truy vấn tiếp:** Webhook có cần idempotent không?

**Ý trả lời truy vấn:** Có; provider có thể gửi lặp, nên state guard phải khiến webhook paid lặp không cộng tiền hoặc phát side effect hai lần.

### Câu 86. SQL Server đóng vai trò gì ngoài lưu dữ liệu?

**Đáp án mẫu:** SQL Server thực thi transaction, isolation, row lock, unique/foreign-key/check constraint và truy vấn báo cáo. Nó là lớp đảm bảo integrity cuối cùng cho các race mà application check đơn lẻ không chặn được. JPA mapping phải khớp schema runtime, migration và canonical SQL.

**Câu hỏi truy vấn tiếp:** Có thể đoán schema từ entity không?

**Ý trả lời truy vấn:** Không; phải đối chiếu catalog SQL Server, SQL/migration và mapping vì có thể tồn tại drift.

### Câu 87. Guest tracking cân bằng tiện dụng và bảo mật thế nào?

**Đáp án mẫu:** Guest nhập order track code và đúng bốn số cuối điện thoại; frontend kiểm tra định dạng bốn chữ số, backend phải đối chiếu cả hai. Cách này tiện hơn tài khoản nhưng yếu hơn xác thực đầy đủ, nên response cần giới hạn dữ liệu nhạy cảm và chống enumeration/rate abuse.

**Câu hỏi truy vấn tiếp:** Đề xuất tăng bảo mật cho guest tracking là gì?

**Ý trả lời truy vấn:** Rate limit, mã track entropy cao, response tối thiểu và proof/token một lần; đây là đề xuất nếu threat model yêu cầu.

### Câu 88. Báo cáo doanh thu được tính theo nguyên tắc nào?

**Đáp án mẫu:** Reporting tách gross, refund và net để không che mất tiền đã hoàn. Hệ thống hỗ trợ time range và các lát cắt như doanh thu theo giờ, category, payment method, cùng exception vận hành. Query phải dùng mốc nghiệp vụ phù hợp và tránh cộng lặp do join order item.

**Câu hỏi truy vấn tiếp:** Net revenue được hiểu thế nào?

**Ý trả lời truy vấn:** Về nguyên tắc là gross trừ refund đủ điều kiện trong phạm vi báo cáo, theo định nghĩa contract/query hiện tại.

### Câu 89. Scheduler hiện tại hoạt động thế nào?

**Đáp án mẫu:** `AppStartupListener` khởi tạo một daemon scheduler trong tiến trình ứng dụng, chạy mỗi phút để xử lý các tác vụ quá hạn như đơn chờ thanh toán/COD. Job kiểm tra lại state và dùng transaction trước mutation. Mô hình đơn giản, phù hợp single instance nhưng phụ thuộc lifecycle của web app.

**Câu hỏi truy vấn tiếp:** Server dừng thì scheduler có chạy không?

**Ý trả lời truy vấn:** Không; đây là scheduler in-process, không phải dịch vụ job độc lập bền vững.

### Câu 90. Rủi ro multi-instance của scheduler là gì?

**Đáp án mẫu:** Mỗi instance có thể tạo daemon riêng và cùng quét một tập đơn mỗi phút. Lock/idempotency giảm double mutation nhưng vẫn gây truy vấn trùng, contention và side effect ngoài DB có thể lặp. **Đề xuất:** distributed lock, leader election, DB job claiming hoặc external scheduler/queue, kèm metric và retry policy.

**Câu hỏi truy vấn tiếp:** Có thể tuyên bố FastGuy đã hỗ trợ scheduler cluster-safe không?

**Ý trả lời truy vấn:** Không; đây chính là hạn chế cần nêu trong phản biện.

## Nhóm 10. Kiểm thử, triển khai, hạn chế và tình huống phản biện

### Câu 91. FastGuy cần những lớp kiểm thử nào?

**Đáp án mẫu:** Backend cần unit/service policy test, servlet/API contract test, entity mapping test và integration test với SQL Server disposable cho transaction/lock. Frontend cần component/store/API test và build; OpenAPI cần lint/contract validation. Luồng UI quan trọng nên chạy Playwright desktop/mobile, kiểm tra console và request chính.

**Câu hỏi truy vấn tiếp:** Mock test có chứng minh pessimistic lock hoạt động trên SQL Server không?

**Ý trả lời truy vấn:** Không; cần integration test đồng thời trên DB test thực để chứng minh isolation/lock/constraint.

### Câu 92. Các lệnh kiểm tra tổng thể nên gồm gì?

**Đáp án mẫu:** Backend chạy test liên quan rồi `mvn test`. Frontend chạy test liên quan, `npm test` và `npm run build`. OpenAPI chạy lint/contract test theo script dự án; thay đổi DB/API cần integration test trên môi trường disposable/local, không dùng dữ liệu retained tùy tiện.

**Câu hỏi truy vấn tiếp:** Build thành công có thay thế test không?

**Ý trả lời truy vấn:** Không; build chứng minh biên dịch/đóng gói, không chứng minh rule nghiệp vụ và race condition.

### Câu 93. Có thể khẳng định FastGuy đã production-ready không?

**Đáp án mẫu:** Không nên khẳng định nếu chỉ có source, test và build. Hiện chưa có bằng chứng trong yêu cầu này về deployment production, monitoring, backup/restore drill, secret rotation, HA, capacity test hay runbook sự cố. Có thể nói hệ thống đã có các cơ chế kỹ thuật nền tảng và nêu rõ các gate cần hoàn thiện trước production.

**Câu hỏi truy vấn tiếp:** Những gate tối thiểu trước production là gì?

**Ý trả lời truy vấn:** Môi trường cấu hình an toàn, migration/backup, integration/E2E, observability, load/security test, scheduler strategy và rollback plan.

### Câu 94. Hạn chế xác thực lớn nhất hiện tại là gì?

**Đáp án mẫu:** JWT sống 24 giờ nhưng logout chỉ xóa token phía client; chưa có refresh token, rotation hay blacklist. Nếu token bị lấy, backend không revoke riêng token đó trước expiry. **Đề xuất:** access token ngắn hơn, refresh token HttpOnly có rotation và server-side session/revocation.

**Câu hỏi truy vấn tiếp:** Tại sao không chỉ tăng JWT lên 30 ngày để đỡ đăng nhập?

**Ý trả lời truy vấn:** Thời gian lộ token tăng mạnh; tiện dụng phải giải bằng refresh/session có kiểm soát, không kéo dài access token tùy ý.

### Câu 95. Hạn chế điều phối lớn nhất hiện tại là gì?

**Đáp án mẫu:** FastGuy chưa auto assignment; staff chọn shipper từ những người đủ điều kiện ca và backend xác minh trước khi gán. Cách này dễ giải thích và cho người vận hành kiểm soát nhưng phụ thuộc thao tác thủ công. **Đề xuất:** scoring theo khoảng cách, tải, SLA và fairness, luôn có manual override và audit.

**Câu hỏi truy vấn tiếp:** Auto assignment nên tối ưu một tiêu chí duy nhất không?

**Ý trả lời truy vấn:** Không; tối ưu chỉ khoảng cách có thể làm lệch tải hoặc vi phạm ca/SLA.

### Câu 96. Hạn chế loyalty hiện tại là gì?

**Đáp án mẫu:** Loyalty đã earn, history, tier và refund reversal nhưng chưa redeem điểm ở checkout. Vì vậy không được giới thiệu FastGuy như đã hỗ trợ “dùng điểm thanh toán”. **Đề xuất:** ledger debit/credit, tỷ lệ quy đổi, trần dùng, expiry, pessimistic lock số dư và idempotency checkout trước khi mở tính năng.

**Câu hỏi truy vấn tiếp:** Chỉ trừ cột `loyalty_points` có đủ không?

**Ý trả lời truy vấn:** Không; cần history/ledger và liên kết order để audit, refund reversal và chống trừ lặp.

### Câu 97. Hạn chế upload media hiện tại là gì?

**Đáp án mẫu:** Unsigned direct upload giảm tải backend nhưng preset ở browser có thể bị lạm dụng và giới hạn kiểm soát phụ thuộc cấu hình Cloudinary. Backend chỉ lưu URL nên phải chống URL ngoài domain hoặc tài nguyên không hợp lệ. **Đề xuất:** signed parameters ngắn hạn, folder/quota/MIME/size restriction và moderation.

**Câu hỏi truy vấn tiếp:** Có nên proxy mọi file qua backend để an toàn tuyệt đối không?

**Ý trả lời truy vấn:** Không bắt buộc; signed direct upload giữ hiệu năng mà vẫn tăng kiểm soát.

### Câu 98. Hạn chế DTO `Map<String,Object>` gây hậu quả gì?

**Đáp án mẫu:** Map làm mất kiểu tĩnh, validation phân tán, lỗi field chỉ lộ runtime và khó bảo đảm OpenAPI luôn khớp serialization. Nó cũng khiến IDE/refactor kém an toàn. **Đề xuất:** thay dần bằng record/DTO typed ở endpoint rủi ro cao, thêm schema validation và contract test; không cần big-bang rewrite.

**Câu hỏi truy vấn tiếp:** Endpoint nào nên ưu tiên?

**Ý trả lời truy vấn:** Checkout, payment/webhook, refund, order transition và COD settlement vì sai field có tác động tiền hoặc state.

### Câu 99. Nếu hội đồng hỏi “hai webhook PayOS đến cùng lúc thì sao?” trả lời thế nào?

**Đáp án mẫu:** Mỗi webhook phải qua HMAC và đối chiếu orderCode, amount, paymentLinkId, attempt. Service mở transaction và khóa pessimistic đơn trước khi chuyển state; webhook sau thấy đơn/attempt đã `PAID` và reconciliation phải không phát side effect lần hai. Test tốt nhất là integration test gửi hai request đồng thời và kiểm tra chỉ một kết quả nghiệp vụ.

**Câu hỏi truy vấn tiếp:** Nếu side effect gửi notification nằm ngoài transaction thì sao?

**Ý trả lời truy vấn:** Có nguy cơ gửi lặp; đề xuất outbox/idempotency key cho side effect ngoài DB.

### Câu 100. Nếu được thêm một vòng cải tiến trước demo, nên ưu tiên gì?

**Đáp án mẫu:** Không tự thêm tính năng rộng. Ưu tiên chứng minh các luồng rủi ro cao: checkout idempotency và lock, PayOS webhook/timeout, refund reversal, inventory release, assignment theo ca và COD settlement; chạy backend/frontend/OpenAPI/build cùng E2E chính. Sau đó mới chọn một hạn chế có tác động rõ, ví dụ harden Cloudinary hoặc typed DTO cho payment, và gọi đó là **đề xuất cải tiến**.

**Câu hỏi truy vấn tiếp:** Vì sao không ưu tiên giao diện đẹp hơn trước?

**Ý trả lời truy vấn:** Khi bảo vệ, tính đúng và bằng chứng cho tiền, tồn, quyền, concurrency quan trọng hơn polish không sửa được rủi ro nghiệp vụ.

## Checklist 15 phút trước khi vào bảo vệ

- [ ] Nhớ đúng bốn role: `ADMIN`, `STAFF`, `SHIPPER`, `USER`.
- [ ] Nhớ đủ 10 order status và các trạng thái kết thúc.
- [ ] Nói được mô hình: Bối cảnh → Quy tắc → Xử lý → Kết quả → Lý do.
- [ ] Phân biệt frontend UX guard với backend trust boundary.
- [ ] Nhớ JWT 24 giờ; không refresh token, không blacklist; logout client-side.
- [ ] Nhớ PBKDF2; khóa 5 lần sai trong 15 phút.
- [ ] Giải thích checkout idempotency key, request hash, owner và pessimistic lock.
- [ ] Giải thích `expectedStatus` và `409 Conflict`.
- [ ] Nhớ PayOS webhook HMAC; không tin client confirm; timeout 15 phút.
- [ ] Nhớ COD pending timeout 3 giờ và COD settlement theo shipper+shift.
- [ ] Nhớ refund chỉ cho paid order `CANCELLED`/`RETURNED_TO_STORE`, trạng thái refund `PENDING`.
- [ ] Nhớ đảo điểm khi refund; loyalty chưa redeem tại checkout.
- [ ] Nhớ inventory `RESERVED/CONSUMED/RELEASED/WASTED`; `quantityAvailable=null` là không giới hạn quản lý.
- [ ] Nhớ ca `Asia/Ho_Chi_Minh`, grace 15 phút, chống overlap, bắt buộc checked-in.
- [ ] Nhớ chưa auto assignment.
- [ ] Nhớ guest track code + bốn số cuối điện thoại; guest PayOS return proof.
- [ ] Nhớ review delivered, unique user/order, homepage consent.
- [ ] Nhớ Cloudinary unsigned direct upload: lợi ích giảm tải, rủi ro preset bị lạm dụng.
- [ ] Nhớ scheduler daemon mỗi phút, in-process, có rủi ro multi-instance.
- [ ] Nhớ reporting gross/refund/net và các lát cắt thời gian, giờ, category, payment, exception.
- [ ] Nhớ Servlet → Service → DAO/JPA → SQL Server; DTO Map là hạn chế.
- [ ] Không tuyên bố production deployment nếu không có bằng chứng.
- [ ] Khi nói phần chưa có, mở đầu bằng “Đề xuất”.
