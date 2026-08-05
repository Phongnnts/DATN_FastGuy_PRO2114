# System Test Report — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx` (sheet System Test Report).
Tổng: 120 system test (ST001–ST120), endpoint đã cập nhật theo dự án hiện tại. Ký hiệu trạng thái: PASS / FAIL / BLOCKED.

| Unit ID | Testcase title | Expected result | Actual result | Status | Run type | Tested by | Date started | Test step details | Test data | Notes |
| ------- | -------------- | --------------- | ------------- | ------ | -------- | --------- | ----------- | ----------------- | -------- | ----- |
| ST001 | Đăng ký tài khoản mới hợp lệ — POST /api/auth/register | Thành công, trả JWT + role USER | Trả {data:{role:"USER",token:...}} | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 22/07/2026 | POST /api/auth/register payload hợp lệ | email/phone/password mới | |
| ST002 | Đăng ký trùng số điện thoại | Báo lỗi đã tồn tại | Trả message "Số điện thoại hoặc email đã tồn tại" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 22/07/2026 | POST register phone trùng | phone đã dùng | |
| ST003 | Đăng ký trùng email | Báo lỗi đã tồn tại | Trả message đã tồn tại | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 22/07/2026 | POST register email trùng | email đã dùng | |
| ST004 | Đăng ký mật khẩu quá ngắn (<8) | Báo lỗi độ dài mật khẩu | Trả "Mật khẩu phải từ 8 đến 72 ký tự" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 23/07/2026 | POST register password "123" | password=123 | |
| ST005 | Đăng ký mật khẩu quá dài (>72) | Báo lỗi độ dài mật khẩu | Trả "Mật khẩu phải từ 8 đến 72 ký tự" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 23/07/2026 | POST register password len=75 | password 75 ký tự | |
| ST006 | Đăng ký email sai định dạng | Báo lỗi email | Trả "Email không hợp lệ" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 23/07/2026 | POST register email "invalid_email" | email sai | |
| ST007 | Đăng ký số điện thoại sai định dạng | Báo lỗi SĐT | Trả "Số điện thoại không hợp lệ" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 23/07/2026 | POST register phone "12345" | phone=12345 | |
| ST008 | Đăng ký thiếu họ tên | Báo lỗi họ tên | Trả "Họ tên phải từ 2 đến 100 ký tự" | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 23/07/2026 | POST register fullName="" | fullName rỗng | |
| ST009 | Đăng ký mật khẩu yếu (123456) | Chặn mật khẩu yếu | Trả "Mật khẩu phải từ 8 đến 72 ký tự" | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 23/07/2026 | POST register password "123456" | password=123456 | |
| ST010 | Đăng ký xong đăng nhập bằng SĐT | Đăng nhập thành công | Trả token | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 23/07/2026 | POST login phone mới tạo | phone + password | |
| ST011 | Admin tạo danh mục mới — POST /api/admin/categories | 201, tạo thành công | 201 catId mới | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 23/07/2026 | POST admin categories | name mới | |
| ST012 | Tạo danh mục trùng tên | Cho phép trùng tên (không duy nhất) | 201, ghi nhận cho phép trùng | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 23/07/2026 | POST admin categories trùng name | name trùng | |
| ST013 | Tạo danh mục tên rỗng | Báo lỗi | Trả "Tên danh mục phải từ 1 đến 100 ký tự" | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 23/07/2026 | POST admin categories name="" | name rỗng | |
| ST014 | Sửa danh mục — PUT /api/admin/categories/12 | 200 | Trả {data:{name:"...",categoryId:12}} | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 25/07/2026 | PUT admin categories/12 | name mới | |
| ST015 | Xóa danh mục đang có sản phẩm — DELETE /api/admin/categories/1 | Báo lỗi | 409 "Không thể xóa danh mục đang có sản phẩm" | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 25/07/2026 | DELETE admin categories/1 | cat có sản phẩm | |
| ST016 | Admin tạo sản phẩm mới — POST /api/admin/products | 201 | 201 prodId mới | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 25/07/2026 | POST admin products payload đầy đủ | name/price/category | |
| ST017 | Tạo sản phẩm thiếu galleryImages — POST /api/admin/products | Tạo được (mặc định) hoặc báo lỗi rõ | HTTP 500 SQL Error 515 gallery_images NULL | FAIL | System API (Postman/HTTP) | Bùi Đức Bình | 25/07/2026 | POST admin products thiếu galleryImages | thiếu field | Defect ADM007 |
| ST018 | Tạo sản phẩm giá âm | Báo lỗi | Trả "basePrice must be >= 0" | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 25/07/2026 | POST admin products basePrice=-1000 | -1000 | |
| ST019 | Sửa sản phẩm — PUT /api/admin/products/19 | 200 | Trả updated=True | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 25/07/2026 | PUT admin products/19 | name mới | |
| ST020 | Xóa (ẩn) sản phẩm — DELETE /api/admin/products/19 | 200 | Trả {message:"Product hidden"} | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 25/07/2026 | DELETE admin products/19 | soft delete | |
| ST021 | Admin tạo người dùng mới — POST /api/admin/users | 201 | 201 uid mới | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 25/07/2026 | POST admin users | thông tin mới | |
| ST022 | Tạo người dùng trùng email | Báo lỗi | 409 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | POST admin users email trùng | email dùng rồi | |
| ST023 | Sửa vai trò người dùng | 200 | 200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 26/07/2026 | PUT admin users role=STAFF | role=STAFF | |
| ST024 | Vô hiệu hóa người dùng | 200 | 200 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 26/07/2026 | PUT admin users status=INACTIVE | INACTIVE | |
| ST025 | Admin tạo mã giảm giá — POST /api/admin/coupons | 201 | 201 couponId mới | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 26/07/2026 | POST admin coupons | code mới | |
| ST026 | Tạo coupon trùng mã | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 26/07/2026 | POST admin coupons code trùng | code trùng | |
| ST027 | Admin tạo banner — POST /api/admin/banners | 201 | 201 bannerId mới | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 26/07/2026 | POST admin banners | title/image | |
| ST028 | Báo cáo doanh thu — GET /api/admin/reports/full | 200, có dữ liệu | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 26/07/2026 | GET admin reports/full | period mặc định | |
| ST029 | Bảng điều khiển Admin — GET /api/admin/dashboard | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 26/07/2026 | GET admin dashboard | | |
| ST030 | Danh sách hoàn tiền — GET /api/admin/refunds | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 26/07/2026 | GET admin refunds | | |
| ST031 | Danh sách yêu thích — GET /api/favorites | 200 | status=200 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 27/07/2026 | GET favorites | token user | |
| ST032 | Thêm/Xóa yêu thích — POST /api/favorites/toggle/{productId} | 200 | status=200 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 27/07/2026 | POST favorites/toggle | productId | |
| ST033 | Xem điểm thưởng — GET /api/loyalty/me | 200 | status=200, có points/history | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 27/07/2026 | GET loyalty/me | token user | |
| ST034 | Danh sách thông báo — GET /api/notifications | 200, có thông báo | status=200 count>0 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 27/07/2026 | GET notifications | token user | |
| ST035 | Đánh dấu đã đọc tất cả — PUT /api/notifications/read-all | 200 | Trả {message:"Read"} | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 27/07/2026 | PUT notifications/read-all | | |
| ST036 | Đánh giá đơn đã giao — POST /api/reviews | 200 | Trả {data:{rating:5,comment:"Ngon"}} | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 27/07/2026 | POST reviews rating=5 | order DELIVERED | |
| ST037 | Đánh giá lại đơn đã đánh giá | 409 | status=409 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 27/07/2026 | POST reviews đơn đã review | order reviewed | |
| ST038 | Đánh giá đơn chưa giao | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 27/07/2026 | POST reviews order PENDING | PENDING | |
| ST039 | Xác thực mã FREESHIP — POST /api/coupons/verify | Hợp lệ | Trả {data:{valid:true,code:"FREESHIP"}} | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST coupons/verify | FREESHIP | |
| ST040 | Lưu mã giảm giá vào ví — POST /api/coupons/claim | 200 | 400 "Bạn đã nhận mã này rồi" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST coupons/claim cùng coupon | coupon đã claim | |
| ST041 | Đăng nhập bằng email đúng — POST /api/auth/login | Thành công role USER | Trả token | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST login email/password | user@fastguy.local | |
| ST042 | Đăng nhập bằng số điện thoại | Thành công | Trả token | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST login phone/password | 0901000004 | |
| ST043 | Đăng nhập sai mật khẩu | Báo lỗi | Không trả token | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST login sai mật khẩu | wrong password | |
| ST044 | Đăng nhập email không tồn tại | Báo lỗi | Không trả token | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST login email lạ | notfound@... | |
| ST045 | Đăng nhập để trống tài khoản | Báo lỗi | Trả "Sai tài khoản hoặc mật khẩu" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 27/07/2026 | POST login account="" | empty | |
| ST046 | Đăng nhập để trống mật khẩu | Báo lỗi | Trả "Sai tài khoản hoặc mật khẩu" | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 27/07/2026 | POST login password="" | empty | |
| ST047 | Đăng nhập email không phân biệt hoa/thường | Thành công hoặc ghi nhận thực tế | login thành công | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 27/07/2026 | POST login USER@FASTGUY.LOCAL | uppercase email | |
| ST048 | Đăng nhập tài khoản INACTIVE | Admin khóa → thất bại | Khóa → không đăng nhập được | PASS | System API (Postman/HTTP) | Bùi Đức Bình | 27/07/2026 | POST login tài khoản INACTIVE | INACTIVE | |
| ST049 | Lấy thông tin cá nhân — GET /api/auth/me | Trả thông tin người dùng | Trả {data:{role,createdAt,...}} | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | GET auth/me | token hợp lệ | |
| ST050 | Đăng xuất — POST /api/auth/logout | Thành công hoặc không tồn tại endpoint | status=200 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | POST auth/logout | | |
| ST051 | Đổi mật khẩu đúng mật khẩu hiện tại — PUT /api/auth/change-password | Thành công | Trả {message:"Đổi mật khẩu thành công"} | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | PUT change-password | current+new | |
| ST052 | Đổi mật khẩu sai mật khẩu hiện tại | Báo lỗi | Trả "Mật khẩu hiện tại không đúng" | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | PUT change-password sai current | wrong current | |
| ST053 | Đổi mật khẩu mới yếu (<8) | Báo lỗi | Trả "Mật khẩu mới phải từ 8 ký tự..." | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | PUT change-password new="123" | weak new | |
| ST054 | Gửi link khôi phục (email tồn tại) — POST /api/auth/forgot-password | Trả thông báo trùng lặp | 200 message chuẩn bảo mật | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | POST forgot-password | email có | |
| ST055 | Gửi link khôi phục (email không tồn tại) | Trả thông báo trùng lặp | 200 message trùng lặp | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | POST forgot-password | email không có | |
| ST056 | Gửi link khôi phục (email sai định dạng) | Trả thông báo trùng lặp | 200 message trùng lặp | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | POST forgot-password email sai | invalid email | |
| ST057 | Đặt lại mật khẩu token sai — POST /api/auth/reset-password | Báo lỗi token | Trả "Liên kết hoặc mật khẩu không hợp lệ" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | POST reset-password token sai | invalid token | |
| ST058 | Đặt lại mật khẩu token rỗng | Báo lỗi token | Trả message token rỗng | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | POST reset-password token="" | empty token | |
| ST059 | Đặt lại mật khẩu mới yếu | Báo lỗi mật khẩu | Trả message không hợp lệ | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | POST reset-password new="123" | weak | |
| ST060 | Đổi mật khẩu: cũ thất bại, mới thành công | Flow đúng | cũ từ chối, mới thành công | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | thử sai rồi đổi đúng | | |
| ST061 | Xem danh sách sản phẩm công khai — GET /api/products | 200, có dữ liệu | status=200 | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | GET products | | |
| ST062 | Xem chi tiết sản phẩm — GET /api/products/1 | 200, đầy đủ thông tin | status=200 | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 28/07/2026 | GET products/1 | | |
| ST063 | Xem sản phẩm không tồn tại — GET /api/products/9999 | 404 | status=404 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | GET products/9999 | | |
| ST064 | Xem danh sách danh mục — GET /api/categories | 200 | status=200 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | GET categories | | |
| ST065 | Tìm kiếm từ khóa "burger" — GET /api/products?q=burger | Có kết quả | status=200, danh sách khớp | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | GET products?q=burger | q=burger | |
| ST066 | Tìm kiếm không có kết quả | Trả danh sách rỗng | status=200, danh sách rỗng | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 28/07/2026 | GET products?q=xyz_not_found | q không tồn tại | |
| ST067 | Lọc theo danh mục Burger | Chỉ sản phẩm Burger | status=200, đúng danh mục | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | GET products?categoryId=1 | category=Burger | |
| ST068 | Sản phẩm nổi bật — GET /api/products/featured | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | GET products/featured | | |
| ST069 | Sản phẩm bán chạy — GET /api/products/best-sellers | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | GET products/best-sellers | | |
| ST070 | Chi tiết sản phẩm có biến thể + tùy chọn — GET /api/products/2 | Có dữ liệu variant/modifier | variants=True modifiers=True | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 28/07/2026 | GET products/2 | | |
| ST071 | Xem giỏ hàng — GET /api/cart | 200, có sản phẩm | status=200 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 28/07/2026 | GET cart | token user | |
| ST072 | Thêm sản phẩm vào giỏ — POST /api/cart | 200 | Trả {message:"Added to cart"} | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 28/07/2026 | POST cart {productId,variantId,quantity} | item hợp lệ | |
| ST073 | Thêm số lượng vượt tồn kho | Báo lỗi | Trả message không đủ stock | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 28/07/2026 | POST cart quantity=9999 | qty lớn | |
| ST074 | Thêm biến thể không tồn tại | Báo lỗi | Trả message invalid variant | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 28/07/2026 | POST cart variantId=9999 | | |
| ST075 | Cập nhật số lượng — PUT /api/cart | 200 | Trả {message:"Updated"} | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 28/07/2026 | PUT cart {cartItemId,quantity} | | |
| ST076 | Cập nhật số lượng = 0 | Sản phẩm bị xóa | status=200 removed | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 29/07/2026 | PUT cart quantity=0 | qty=0 | |
| ST077 | Xóa sản phẩm khỏi giỏ — DELETE /api/cart/{cartItemId} | 200 | Trả {message:"Removed"} | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 29/07/2026 | DELETE cart/{id} | | |
| ST078 | Xem giỏ khi chưa đăng nhập | 401 | status=401 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 29/07/2026 | GET cart không token | no token | |
| ST079 | Thêm sản phẩm kèm tùy chọn hợp lệ | 200 | Trả "Added to cart" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 29/07/2026 | POST cart kèm modifier | modifiers hợp lệ | |
| ST080 | Thêm tùy chọn không tồn tại | Báo lỗi | Trả message invalid modifier | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 29/07/2026 | POST cart modifierId=9999 | | |
| ST081 | Xem danh sách địa chỉ — GET /api/user/addresses | 200 | status=200 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | GET user/addresses | token user | |
| ST082 | Tạo địa chỉ mới hợp lệ — POST /api/user/addresses | 200 | Trả {data:{ghnDistrictId,...}} | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | POST user/addresses | address hợp lệ | |
| ST083 | Tạo địa chỉ thiếu tên đường | Báo lỗi | Trả "Số nhà/tên đường phải từ 5 đến 255 ký tự" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | POST user/addresses street="" | street rỗng | |
| ST084 | Tạo địa chỉ số điện thoại sai | Báo lỗi | Trả "Số điện thoại không hợp lệ" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | POST user/addresses phone="123" | phone sai | |
| ST085 | Tạo địa chỉ thiếu mã phường/xã GHN | Báo lỗi | Trả "Phường/xã GHN không hợp lệ" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | POST user/addresses wardCode=null | | |
| ST086 | Đặt địa chỉ mặc định (chỉ 1 mặc định) | Chỉ 1 mặc định | defaults=1 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | PUT user/addresses/{id}/default | | |
| ST087 | Sửa địa chỉ — PUT /api/user/addresses/1 | 200 | updated=True | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | PUT user/addresses/1 | | |
| ST088 | Xóa địa chỉ — DELETE /api/user/addresses/1 | 200 | Trả {message:"Address deleted"} | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 29/07/2026 | DELETE user/addresses/1 | | |
| ST089 | Lấy danh sách tỉnh/thành GHN — GET /api/shipping/provinces | 200 hoặc rỗng nếu chưa cấu hình | status=200, có danh sách | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 29/07/2026 | GET shipping/provinces | | |
| ST090 | Tính phí vận chuyển — POST /api/shipping/fee | Cần GHN token; chưa cấu hình nên BỊ CHẶN | status=502 | BLOCKED | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 29/07/2026 | POST shipping/fee | thiếu GHN token | |
| ST091 | Thanh toán thiếu địa chỉ | Báo lỗi | Trả "Missing address" | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 29/07/2026 | POST orders addressId=null | | |
| ST092 | Thanh toán thiếu Idempotency-Key | Báo lỗi | Trả "Thiếu Idempotency-Key" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders thiếu header | | |
| ST093 | Thanh toán chữ ký giỏ rỗng | Báo lỗi | Trả "Thông tin giao hàng không hợp lệ" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders cartSignature="" | | |
| ST094 | Thanh toán khi giỏ hàng rỗng | Báo lỗi Giỏ hàng rỗng | Trả "Giỏ hàng trống" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders items=[] | | |
| ST095 | Thanh toán chữ ký giỏ sai | Báo lỗi Giỏ đã thay đổi | Trả "Giỏ hàng đã thay đổi, vui lòng thử lại" | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders signature mismatch | | |
| ST096 | Thanh toán chuyển khoản khi PayOS chưa cấu hình | Báo lỗi phương thức không khả dụng | 400 | BLOCKED | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders BANK_TRANSFER | PayOS chưa config | |
| ST097 | Thanh toán COD (cần GHN) | Tạo đơn PENDING | 400 "Vui lòng chọn đầy đủ địa chỉ GHN" | BLOCKED | System API (Postman/HTTP) | Nguyễn Nam Phong | 29/07/2026 | POST orders COD | thiếu GHN config | |
| ST098 | Khách thanh toán thiếu sản phẩm — POST /api/orders/guest-checkout | Báo lỗi | Trả "Missing items" | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | POST guest-checkout items=[] | | |
| ST099 | Khách thanh toán SĐT sai | Báo lỗi | Trả "Thông tin giao hàng không hợp lệ" | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | POST guest-checkout phone="123" | | |
| ST100 | Kiểm tra khả năng thanh toán — GET /api/orders/payment-capabilities | 200, trả cấu hình PayOS/COD | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | GET orders/payment-capabilities | | |
| ST101 | Nhân viên xem bảng điều khiển — GET /api/staff/dashboard | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | GET staff/dashboard | token staff+check-in | |
| ST102 | Nhân viên xem danh sách đơn — GET /api/staff/orders | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | GET staff/orders | | |
| ST103 | Nhân viên xem chi tiết đơn — GET /api/staff/orders/1 | 200 | status=200 | PASS | System API (Postman/HTTP) | Đỗ Huy Hoàng | 30/07/2026 | GET staff/orders/1 | | |
| ST104 | Xác nhận đơn chuyển khoản chưa thanh toán | Báo lỗi | 400 "Đơn chuyển khoản chưa thanh toán" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 30/07/2026 | PUT staff/orders/1/status CONFIRMED | BANK_TRANSFER UNPAID | |
| ST105 | Chuyển Xác nhận → Chế biến | 200 | Trả "Status updated" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 30/07/2026 | PUT staff/orders/2/status PREPARING | | |
| ST106 | Chuyển Chế biến → Sẵn sàng | 200 | Trả "Status updated" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 30/07/2026 | PUT staff/orders/3/status READY | | |
| ST107 | Chuyển ngược Sẵn sàng → Chế biến | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 30/07/2026 | PUT staff/orders/3/status PREPARING | invalid transition | |
| ST108 | Gán Shipper cho đơn Sẵn sàng — PUT /api/staff/orders/{id}/assign-shipper | 200 | Trả "Shipper assigned" | PASS | System API (Postman/HTTP) | Nguyễn Thành Phát | 30/07/2026 | PUT assign-shipper shipperId=3 | | |
| ST109 | Gán Shipper không tồn tại | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 31/07/2026 | PUT assign-shipper shipperId=9999 | | |
| ST110 | Thêm ghi chú đơn — POST /api/staff/orders/{id}/notes | 200 | Trả "Note saved" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 31/07/2026 | POST notes "Giao gấp" | | |
| ST111 | Shipper xem bảng điều khiển — GET /api/shipper/dashboard | 200 | status=200 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 31/07/2026 | GET shipper/dashboard | token shipper | |
| ST112 | Shipper xem đơn được gán — GET /api/shipper/orders/mine | 200 | status=200 | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 31/07/2026 | GET shipper/orders/mine | | |
| ST113 | Shipper nhận đơn — PUT /api/shipper/orders/1/pickup | 200 | Trả "Picked up successfully" | PASS | System API (Postman/HTTP) | Phạm Gia Bảo | 31/07/2026 | PUT shipper/orders/1/pickup | | |
| ST114 | Nhận đơn chưa được gán cho mình | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 31/07/2026 | PUT pickup đơn của người khác | | |
| ST115 | Giao hàng COD đúng số tiền — PUT /api/shipper/orders/{id}/deliver | 200, đơn DELIVERED | Trả "Delivered successfully" | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 31/07/2026 | PUT deliver collected=finalAmount | | |
| ST116 | Giao hàng COD sai số tiền | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 31/07/2026 | PUT deliver collected sai | | |
| ST117 | Giao hàng lần 2 (đã giao) | Báo lỗi | 400 | PASS | System API (Postman/HTTP) | Phan Vũ Phúc Khang | 31/07/2026 | PUT deliver đơn DELIVERED | | |
| ST118 | Xem chi tiết đơn của mình — GET /api/shipper/orders/1 | 200 | status=200 | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 31/07/2026 | GET shipper/orders/1 | | |
| ST119 | Xem chi tiết đơn không thuộc về mình | 403/404 | status=404 | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 31/07/2026 | GET shipper/orders/999 | không sở hữu | |
| ST120 | Shipper điểm danh ca làm việc — POST /api/shifts/{id}/check-in | 200 | 400 (ngoài cửa sổ ca) | PASS | System API (Postman/HTTP) | Nguyễn Nam Phong | 31/07/2026 | POST shifts/{id}/check-in | | |
