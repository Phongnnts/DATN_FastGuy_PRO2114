# FastGuy — Sơ Đồ Use Case

Tài liệu mô tả toàn bộ use case của hệ thống **FastGuy - Website bán đồ ăn nhanh online**. Nguồn: tài liệu báo cáo dự án (`VYTA_FASTGUY.docx`), yêu cầu đặc tả và hiện trạng codebase (router, backend servlet, luồng nghiệp vụ).

---

## 1. Tổng quan

### 1.1. Các tác nhân (Actor)

| STT | Actor | Mô tả | Phạm vi |
| --- | ----- | ----- | ------- |
| 1 | **Guest** | Khách vãng lai, chưa đăng nhập | Xem thực đơn, giỏ hàng, đặt hàng không cần tài khoản, tra cứu đơn |
| 2 | **User** | Khách hàng đã đăng ký và đăng nhập | Toàn bộ chức năng Guest + tài khoản cá nhân, lịch sử, coupon, đánh giá, hỗ trợ, điểm thưởng |
| 3 | **Staff** | Nhân viên xử lý đơn hàng | Ca làm, xác nhận/chế biến đơn, phân công Shipper, xử lý hỗ trợ |
| 4 | **Shipper** | Nhân viên giao hàng | Ca làm, nhận đơn được phân công, giao hàng, thu tiền COD |
| 5 | **Admin** | Quản trị viên hệ thống | Quản lý dữ liệu toàn hệ thống, báo cáo, cấu hình |

### 1.2. Hệ thống ngoài

| Hệ thống | Vai trò |
| -------- | ------- |
| GHN | Tra cứu tỉnh/quận/phường, tính phí giao hàng |
| PayOS | Tạo link thanh toán, xác thực webhook, trạng thái thanh toán |
| Cloudinary | Lưu trữ ảnh sản phẩm/banner |
| Jakarta Mail | Gửi email đặt lại mật khẩu |
| SQL Server | Lưu trữ dữ liệu |

### 1.3. Luồng trạng thái đơn hàng

`PENDING → CONFIRMED → PREPARING → READY → ASSIGNED → PICKED_UP → DELIVERED`

`CANCELLED` là trạng thái kết thúc qua lệnh hủy riêng.

| Trạng thái | Mô tả | Actor thao tác |
| ---------- | ----- | -------------- |
| PENDING | Đơn vừa tạo, chờ xử lý | User/Guest tạo; Staff xem |
| CONFIRMED | Đã xác nhận | Staff |
| PREPARING | Đang chế biến | Staff (tiêu thụ tồn kho đã giữ) |
| READY | Sẵn sàng giao | Staff |
| ASSIGNED | Đã phân công Shipper | Staff |
| PICKED_UP | Shipper đã nhận hàng | Shipper |
| DELIVERED | Giao thành công | Shipper (tích điểm nếu User có tài khoản) |
| CANCELLED | Đã hủy | User/Guest/Staff/Admin/System |

Ghi chú: hủy từ `PREPARING` trở đi ghi tồn kho `WASTE`, không hoàn hàng đã chế biến. Staff/Shipper chỉ thao tác nghiệp vụ khi tài khoản `ACTIVE` và ca `CHECKED_IN`. Shipper chỉ xem/thao tác đơn được gán cho mình.

---

## 2. Use Case theo tác nhân

### 2.1. Guest

| STT | Mã UC | Tên use case | Mô tả |
| --- | ----- | ------------ | ----- |
| 1 | UC-G01 | Xem thực đơn | Xem danh mục, danh sách món, hình ảnh, giá, chi tiết sản phẩm |
| 2 | UC-G02 | Tìm kiếm và lọc món | Tìm theo tên, lọc theo danh mục, khoảng giá, loại món, khuyến mãi, còn hàng, sắp xếp |
| 3 | UC-G03 | Quản lý giỏ hàng | Thêm món, thay đổi số lượng, xóa món khỏi giỏ tạm thời |
| 4 | UC-G04 | Đặt hàng không cần đăng nhập | Nhập họ tên, số điện thoại, địa chỉ để đặt hàng |
| 5 | UC-G05 | Chọn phương thức thanh toán | Thanh toán COD hoặc chuyển khoản qua PayOS |
| 6 | UC-G06 | Áp dụng mã giảm giá | Sử dụng coupon khi đáp ứng điều kiện |
| 7 | UC-G07 | Nhận mã đơn hàng | Nhận mã đơn sau khi đặt thành công |
| 8 | UC-G08 | Tra cứu đơn hàng | Tra cứu bằng mã đơn và bốn số cuối số điện thoại |
| 9 | UC-G09 | Theo dõi trạng thái đơn | Xem trạng thái xử lý hiện tại của đơn |
| 10 | UC-G10 | Xem khuyến mãi | Xem coupon công khai, sao chép/claim mã |
| 11 | UC-G11 | Trợ giúp | Xem FAQ, chính sách giao hàng, thanh toán, hủy/hoàn, liên hệ |

### 2.2. User

| STT | Mã UC | Tên use case | Mô tả |
| --- | ----- | ------------ | ----- |
| 1 | UC-U01 | Đăng ký và đăng nhập | Tạo tài khoản, đăng nhập, đăng xuất, khôi phục mật khẩu |
| 2 | UC-U02 | Xem và tìm kiếm món | Xem danh mục, sản phẩm, biến thể, tùy chọn và combo |
| 3 | UC-U03 | Quản lý giỏ hàng | Thêm, sửa số lượng, xóa sản phẩm; hợp nhất giỏ sau đăng nhập |
| 4 | UC-U04 | Quản lý địa chỉ | Thêm, sửa, xóa và đặt địa chỉ mặc định |
| 5 | UC-U05 | Đặt hàng | Đặt hàng bằng tài khoản, chọn COD hoặc PayOS |
| 6 | UC-U06 | Xem lịch sử đơn hàng | Xem danh sách, chi tiết và trạng thái các đơn đã đặt |
| 7 | UC-U07 | Hủy đơn hàng | Hủy đơn khi còn ở trạng thái cho phép |
| 8 | UC-U08 | Quản lý hồ sơ | Cập nhật thông tin cá nhân và mật khẩu |
| 9 | UC-U09 | Quản lý coupon | Nhận (claim), xem và sử dụng mã giảm giá |
| 10 | UC-U10 | Đánh giá sản phẩm | Đánh giá sau khi đơn đã giao thành công |
| 11 | UC-U11 | Gửi yêu cầu hỗ trợ | Tạo và theo dõi yêu cầu hỗ trợ |
| 12 | UC-U12 | Xem thông báo | Xem thông báo và cập nhật trạng thái đã đọc |
| 13 | UC-U13 | Theo dõi điểm thưởng | Xem điểm, cấp thành viên và lịch sử giao dịch điểm |
| 14 | UC-U14 | Quản lý yêu thích | Thêm/bỏ món yêu thích |
| 15 | UC-U15 | Đặt lại đơn | Đặt lại món từ đơn cũ (kiểm tra tồn kho) |

### 2.3. Staff

| STT | Mã UC | Tên use case | Mô tả |
| --- | ----- | ------------ | ----- |
| 1 | UC-S01 | Xem ca làm việc | Xem ca được phân công, check-in/check-out |
| 2 | UC-S02 | Xem danh sách đơn hàng | Xem, tìm kiếm và lọc đơn cần xử lý theo trạng thái |
| 3 | UC-S03 | Xem chi tiết đơn hàng | Xem khách hàng, sản phẩm, thanh toán, địa chỉ giao |
| 4 | UC-S04 | Xác nhận đơn hàng | Chuyển trạng thái PENDING → CONFIRMED |
| 5 | UC-S05 | Chuẩn bị đơn hàng | Chuyển CONFIRMED → PREPARING → READY |
| 6 | UC-S06 | Kiểm tra tồn kho | Kiểm tra tồn kho theo biến thể sản phẩm |
| 7 | UC-S07 | Hủy đơn hàng | Hủy khi hết hàng, sai thông tin hoặc khách yêu cầu |
| 8 | UC-S08 | Phân công Shipper | Chọn Shipper khả dụng, chuyển READY → ASSIGNED |
| 9 | UC-S09 | Xem lịch sử xử lý | Xem lịch sử trạng thái và các đơn đã xử lý |
| 10 | UC-S10 | Xử lý yêu cầu hỗ trợ | Xem và phản hồi support ticket của khách |
| 11 | UC-S11 | Điều phối giao hàng | Board đơn READY chưa gán + workload Shipper |
| 12 | UC-S12 | Ghi chú nội bộ đơn | Thêm ghi chú nội bộ cho đơn |

### 2.4. Shipper

| STT | Mã UC | Tên use case | Mô tả |
| --- | ----- | ------------ | ----- |
| 1 | UC-SH01 | Xem ca làm việc | Xem ca được phân công, check-in/check-out |
| 2 | UC-SH02 | Xem đơn được phân công | Chỉ xem các đơn do Staff phân công |
| 3 | UC-SH03 | Xem chi tiết đơn hàng | Xem địa chỉ, số điện thoại, sản phẩm, số tiền COD |
| 4 | UC-SH04 | Liên hệ khách hàng | Gọi khách bằng số điện thoại, mở địa chỉ trên Google Maps |
| 5 | UC-SH05 | Xác nhận nhận hàng | Chuyển đơn ASSIGNED → PICKED_UP |
| 6 | UC-SH06 | Xác nhận giao thành công | Chuyển đơn PICKED_UP → DELIVERED |
| 7 | UC-SH07 | Thu tiền COD | Nhập và xác nhận số tiền đã thu từ khách |
| 8 | UC-SH08 | Xem lịch sử giao hàng | Xem các đơn đã giao và đã hủy của Shipper |
| 9 | UC-SH09 | Xem tổng quan ca | Xem số đơn đang giao, đã nhận, đã giao hôm nay, đơn tiếp theo |

### 2.5. Admin

| STT | Mã UC | Tên use case | Mô tả |
| --- | ----- | ------------ | ----- |
| 1 | UC-A01 | Quản lý người dùng | Thêm, sửa, khóa, mở khóa, phân quyền tài khoản |
| 2 | UC-A02 | Quản lý danh mục | Thêm, sửa và cập nhật trạng thái danh mục |
| 3 | UC-A03 | Quản lý sản phẩm | Quản lý sản phẩm, hình ảnh, giá, trạng thái bán |
| 4 | UC-A04 | Quản lý biến thể và tùy chọn | Quản lý variant, modifier, combo sản phẩm |
| 5 | UC-A05 | Quản lý tồn kho | Theo dõi và điều chỉnh tồn kho theo biến thể |
| 6 | UC-A06 | Quản lý đơn hàng | Xem, tìm kiếm, lọc, kiểm tra toàn bộ đơn hàng |
| 7 | UC-A07 | Quản lý thanh toán/hoàn tiền | Theo dõi COD, PayOS, xử lý yêu cầu hoàn tiền |
| 8 | UC-A08 | Quản lý coupon | Thêm, sửa, thiết lập điều kiện sử dụng coupon |
| 9 | UC-A09 | Quản lý ca làm việc | Tạo và phân ca cho Staff, Shipper |
| 10 | UC-A10 | Quản lý hỗ trợ | Xem và xử lý support ticket của khách |
| 11 | UC-A11 | Quản lý banner và cấu hình | Quản lý banner, phí giao hàng, cấu hình hệ thống |
| 12 | UC-A12 | Xem báo cáo thống kê | Xem doanh thu, đơn hàng, sản phẩm, danh mục, phương thức thanh toán |

---

## 3. Đặc tả Use Case chính

### 3.1. ĐẶT HÀNG KHÔNG CẦN ĐĂNG NHẬP

| Thành phần | Nội dung |
| ---------- | -------- |
| **Mục đích** | Cho phép Guest lựa chọn sản phẩm và đặt hàng mà không cần tạo tài khoản |
| **Tác nhân** | Guest |
| **Tiền điều kiện** | Sản phẩm đang bán và giỏ hàng có ít nhất một sản phẩm |
| **Hậu điều kiện** | Đơn hàng được tạo và Guest nhận được mã đơn để tra cứu |

**Luồng sự kiện chính**

1. Guest truy cập trang thực đơn.
2. Hệ thống hiển thị danh mục và sản phẩm đang bán.
3. Guest chọn sản phẩm, biến thể, tùy chọn và số lượng.
4. Hệ thống thêm sản phẩm vào giỏ hàng và tính tổng tiền tạm tính.
5. Guest mở trang thanh toán và nhập thông tin nhận hàng.
6. Hệ thống kiểm tra họ tên, số điện thoại và địa chỉ giao hàng.
7. Guest chọn phương thức thanh toán và nhập mã giảm giá nếu có.
8. Hệ thống kiểm tra sản phẩm, tồn kho, mã giảm giá và tính tổng tiền cuối cùng.
9. Guest xác nhận đặt hàng.
10. Hệ thống tạo đơn và hiển thị mã đơn hàng.

**Luồng ngoại lệ**

- 6a. Thông tin nhận hàng không hợp lệ → hệ thống yêu cầu nhập lại.
- 8a. Sản phẩm không còn bán hoặc không đủ số lượng → thông báo lỗi.
- 8b. Mã giảm giá không hợp lệ → không áp dụng giảm giá.
- 10a. Không thể tạo đơn → giữ nguyên giỏ hàng và thông báo lỗi.

### 3.2. ĐẶT VÀ THEO DÕI ĐƠN HÀNG

| Thành phần | Nội dung |
| ---------- | -------- |
| **Mục đích** | Cho phép User đặt hàng bằng tài khoản và theo dõi quá trình xử lý đơn |
| **Tác nhân** | User |
| **Tiền điều kiện** | User đã đăng nhập và giỏ hàng có sản phẩm |
| **Hậu điều kiện** | Đơn hàng được tạo và lưu trong lịch sử mua hàng của User |

**Luồng sự kiện chính**

1. User đăng nhập vào hệ thống.
2. Hệ thống xác thực tài khoản và hiển thị trang chủ.
3. User chọn sản phẩm và thêm vào giỏ hàng.
4. Hệ thống cập nhật giỏ hàng và tổng tiền.
5. User mở trang thanh toán và chọn địa chỉ giao hàng.
6. Hệ thống kiểm tra địa chỉ và tính phí giao hàng.
7. User chọn mã giảm giá và phương thức thanh toán.
8. Hệ thống kiểm tra điều kiện áp dụng và hiển thị tổng tiền cuối cùng.
9. User xác nhận đặt hàng.
10. Hệ thống tạo đơn và lưu vào lịch sử đơn hàng.
11. User mở lịch sử và chọn một đơn.
12. Hệ thống hiển thị chi tiết, trạng thái hiện tại và lịch sử xử lý đơn.

**Luồng ngoại lệ**

- 2a. Tài khoản không hợp lệ hoặc bị khóa → hệ thống từ chối đăng nhập.
- 6a. Địa chỉ không hợp lệ → yêu cầu chọn lại.
- 8a. Mã giảm giá không đủ điều kiện → thông báo lỗi.
- 9a. Sản phẩm không đủ số lượng → không tạo đơn.
- 12a. Đơn không thuộc User → hệ thống từ chối truy cập.

### 3.3. XỬ LÝ VÀ PHÂN CÔNG ĐƠN HÀNG

| Thành phần | Nội dung |
| ---------- | -------- |
| **Mục đích** | Cho phép Staff tiếp nhận, chuẩn bị và phân công đơn hàng cho Shipper |
| **Tác nhân** | Staff |
| **Tiền điều kiện** | Staff đã đăng nhập, có ca làm hợp lệ và đã check-in |
| **Hậu điều kiện** | Đơn hàng được xử lý đúng trình tự và phân công cho Shipper |

**Luồng sự kiện chính**

1. Staff mở danh sách đơn hàng.
2. Hệ thống hiển thị các đơn cần xử lý theo trạng thái và thời gian tạo.
3. Staff chọn một đơn để xem chi tiết.
4. Hệ thống hiển thị thông tin khách hàng, sản phẩm, thanh toán và ghi chú.
5. Staff xác nhận đơn hàng.
6. Hệ thống chuyển đơn PENDING → CONFIRMED và lưu lịch sử xử lý.
7. Staff bắt đầu chuẩn bị món.
8. Hệ thống chuyển đơn CONFIRMED → PREPARING và cập nhật tồn kho.
9. Staff xác nhận đã chuẩn bị xong.
10. Hệ thống chuyển đơn PREPARING → READY.
11. Staff chọn Shipper giao hàng.
12. Hệ thống kiểm tra Shipper và chuyển đơn READY → ASSIGNED.

**Luồng ngoại lệ**

- 4a. Đơn không tồn tại hoặc đã hủy → thông báo lỗi.
- 5a. Đơn không ở trạng thái PENDING → không cho xác nhận.
- 8a. Sản phẩm không đủ số lượng → không cho tiếp tục xử lý.
- 11a. Không có Shipper phù hợp → giữ đơn ở READY.
- 12a. Shipper không hoạt động hoặc không có ca hợp lệ → từ chối phân công.

### 3.4. NHẬN VÀ GIAO ĐƠN HÀNG

| Thành phần | Nội dung |
| ---------- | -------- |
| **Mục đích** | Cho phép Shipper nhận và giao các đơn được Staff phân công |
| **Tác nhân** | Shipper |
| **Tiền điều kiện** | Shipper đã đăng nhập, đã check-in và có đơn được phân công |
| **Hậu điều kiện** | Đơn hàng giao thành công và trạng thái được cập nhật |

**Luồng sự kiện chính**

1. Shipper mở danh sách đơn được phân công.
2. Hệ thống chỉ hiển thị các đơn thuộc Shipper hiện tại.
3. Shipper chọn một đơn để xem chi tiết.
4. Hệ thống hiển thị địa chỉ, số điện thoại, sản phẩm và số tiền cần thu.
5. Shipper xác nhận đã nhận hàng từ cửa hàng.
6. Hệ thống kiểm tra quyền xử lý và chuyển đơn ASSIGNED → PICKED_UP.
7. Shipper giao hàng cho khách.
8. Với đơn COD, Shipper nhập số tiền đã thu.
9. Shipper xác nhận giao hàng thành công.
10. Hệ thống kiểm tra thông tin thanh toán và chuyển đơn PICKED_UP → DELIVERED.
11. Với đơn COD, hệ thống cập nhật trạng thái thanh toán thành đã thanh toán.

**Luồng ngoại lệ**

- 2a. Không có đơn được phân công → hiển thị danh sách rỗng.
- 3a. Đơn không thuộc Shipper → hệ thống từ chối truy cập.
- 5a. Đơn không ở ASSIGNED → không cho nhận hàng.
- 8a. Số tiền COD không đúng → yêu cầu kiểm tra lại.
- 9a. Không thể giao hàng → Shipper thông báo Staff/Admin xử lý.

### 3.5. QUẢN LÝ VÀ THEO DÕI HỆ THỐNG

| Thành phần | Nội dung |
| ---------- | -------- |
| **Mục đích** | Cho phép Admin quản lý dữ liệu và theo dõi toàn bộ hoạt động của hệ thống |
| **Tác nhân** | Admin |
| **Tiền điều kiện** | Admin đã đăng nhập bằng tài khoản quản trị |
| **Hậu điều kiện** | Dữ liệu được cập nhật và thông tin hoạt động được hiển thị |

**Luồng sự kiện chính**

1. Admin đăng nhập vào trang quản trị.
2. Hệ thống xác thực quyền Admin và hiển thị bảng điều khiển.
3. Admin chọn chức năng quản lý người dùng, sản phẩm, danh mục, tồn kho, đơn hàng, mã giảm giá hoặc ca làm việc.
4. Hệ thống hiển thị danh sách dữ liệu tương ứng.
5. Admin thêm mới, chỉnh sửa, khóa hoặc cập nhật trạng thái dữ liệu.
6. Hệ thống kiểm tra thông tin và lưu thay đổi.
7. Admin mở phần quản lý đơn hàng và thanh toán.
8. Hệ thống hiển thị chi tiết đơn, trạng thái và lịch sử xử lý.
9. Admin mở trang báo cáo thống kê.
10. Hệ thống tổng hợp và hiển thị doanh thu, số lượng đơn và sản phẩm bán chạy.

**Luồng ngoại lệ**

- 2a. Tài khoản không có quyền Admin → từ chối truy cập.
- 5a. Dữ liệu nhập không hợp lệ → hiển thị lỗi.
- 5b. Dữ liệu đang được sử dụng → không cho xóa trực tiếp.
- 6a. Dữ liệu bị trống → không lưu thay đổi.
- 8a. Đơn hàng không tồn tại → thông báo lỗi.
- 10a. Không có dữ liệu trong thời gian được chọn → hiển thị kết quả bằng 0.

---

## 4. Quyền truy cập và điều kiện nghiệp vụ

| Quy tắc | Mô tả |
| ------- | ----- |
| Đăng nhập | JWT bearer; role lưu trong `Users.role_name`; tài khoản phải `ACTIVE` |
| Vai trò | `ADMIN`, `STAFF`, `SHIPPER`, `USER` |
| Ca làm | Staff/Shipper thao tác nghiệp vụ chỉ khi ca hôm nay `CHECKED_IN` |
| Quyền Shipper | Chỉ đọc/thao tác đơn có `shipper_id = mình`; chi tiết đơn `DELIVERED`/`CANCELLED` xem được ngoài ca |
| Quyền User | Chỉ xem đơn thuộc `user_id` của mình |
| Hủy đơn | User chỉ hủy khi `PENDING`; Staff/Admin hủy theo policy transition |
| Thanh toán | Đơn `BANK_TRANSFER` phải `PAID` trước khi Staff confirm |
| COD | Số tiền thu phải khớp chính xác `final_amount` |
| Tích điểm | Đơn `DELIVERED` + `PAID` + có `user_id` → cộng điểm; hoàn tiền → đảo điểm |
| Tra cứu guest | Mã đơn + 4 số cuối điện thoại; guest payment status dùng `returnProof` opaque |
