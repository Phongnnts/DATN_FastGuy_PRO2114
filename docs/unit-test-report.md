# Unit Test Report — FastGuy (Website bán đồ ăn nhanh online)

Cấu trúc theo template: `Bản sao của Nhóm 4_Danh sách công việc trong Sprint, Release backlog, product backlog.xlsx` (sheet Unit Test report).
Tổng: 120 unit test (UT001–UT120).

| Unit ID | Module Title | Testcase title | Expected result | Actual result | Status | Run type | Tested by | Date started | Test step details | Test data | Notes |
| ------- | ------------ | -------------- | --------------- | ------------- | ------ | -------- | --------- | ----------- | ----------------- | -------- | ----- |
| UT001 | PasswordUtil | Hash null | Ném IllegalArgumentException("Password is required") | Ném đúng IllegalArgumentException | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 22/07/2026 | hash(null) | src/test/PasswordUtilTest.java | |
| UT002 | PasswordUtil | Hash rồi check đúng | true | Trả về true | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 22/07/2026 | hash("abc12345"); check("abc12345", h) | password=abc12345 | |
| UT003 | PasswordUtil | Check sai mật khẩu | false | Trả về false | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 22/07/2026 | check("sai", hash("dung")) | wrong password | |
| UT004 | PasswordUtil | Legacy plaintext | true (so sánh plaintext) | So sánh plaintext khớp, trả về true | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 22/07/2026 | check("pw123", "pw123") | chuỗi không có prefix pbkdf2$ | |
| UT005 | PasswordUtil | Stored hash hỏng | false, không ném | Trả về false, không phát sinh ngoại lệ | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 23/07/2026 | check("x", "pbkdf2$abc$...") | salt/iterations không hợp lệ | |
| UT006 | PasswordUtil | Check null | false | Trả về false cho cả 2 trường hợp | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 23/07/2026 | check(null,x) và check(x,null) | null | |
| UT007 | JwtUtil | Vòng đời token | claims không null; getUserId=4, getRole="USER" | Claims không null, userId=4, role=USER | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 23/07/2026 | t=generate(4,"USER"); c=validate(t) | userId=4, role=USER | |
| UT008 | JwtUtil | Token bị sửa chữ ký | null | Trả về null khi token sai chữ ký | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 23/07/2026 | Sửa 1 ký tự cuối token rồi validate | token tampered | |
| UT009 | JwtUtil | Token sai role claim | validate trả claims, servlet chặn 403 | Validate trả claims, servlet chặn 403 | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 23/07/2026 | Craft token role "HACKER" | role=HACKER | |
| UT010 | JwtUtil | getUserId token hỏng | -1 | Trả về -1 | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 23/07/2026 | getUserId("garbage") | garbage token | |
| UT011 | JwtUtil | getRole token hỏng | null | Trả về null | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 23/07/2026 | getRole("garbage") | garbage token | |
| UT012 | JwtUtil | validate null | null (không ném) | Trả về null, không throw | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 23/07/2026 | validate(null) | null | |
| UT013 | AuthService | Login đúng phone | Trả user USER | Đăng nhập thành công | Pass | Manual (Demo Account) | Nguyễn Nam Phong | 23/07/2026 | login("0901000004","123456") | DB FastGuyDB | |
| UT014 | AuthService | Login đúng email | Trả user USER | Đăng nhập thành công | Pass | Manual (Demo Account) | Nguyễn Thành Phát | 25/07/2026 | login("user@fastguy.local","123456") | demo account | |
| UT015 | AuthService | Login sai mật khẩu | null | Trả về null | Pass | Manual (Demo Account) | Nguyễn Thành Phát | 25/07/2026 | login("user@fastguy.local","sai") | sai mật khẩu | |
| UT016 | AuthService | Login user inactive | null | Trả về null, không cho đăng nhập | Pass | Manual (Demo Account) | Nguyễn Thành Phát | 25/07/2026 | Set user INACTIVE rồi login | status=INACTIVE | |
| UT017 | AuthService | isStrongPassword hợp lệ | true | Trả về true | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 25/07/2026 | isStrongPassword("Abcdef12") | password=Abcdef12 | |
| UT018 | AuthService | Mật khẩu yếu | false | Trả về false cho cả 3 trường hợp | Pass | Automated (JUnit 5) | Bùi Đức Bình | 25/07/2026 | isStrongPassword("abcdefgh")/("12345678")/("short1") | weak passwords | |
| UT019 | AddressValidator | Địa chỉ hợp lệ | Trả null (không lỗi) | Trả về null | Pass | Automated (JUnit 5) | Bùi Đức Bình | 25/07/2026 | validate name/phone/street/GHN ids | hợp lệ | |
| UT020 | AddressValidator | Phone sai format | Trả chuỗi lỗi phone | Báo lỗi định dạng điện thoại | Pass | Automated (JUnit 5) | Bùi Đức Bình | 25/07/2026 | phone="012345" hoặc "12345678901" | phone sai | |
| UT021 | AddressValidator | Street quá ngắn | Trả lỗi street | Báo lỗi độ dài địa chỉ đường | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 25/07/2026 | street="ab" | street ngắn | |
| UT022 | AddressValidator | Name quá ngắn | Trả lỗi name | Báo lỗi tên quá ngắn | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | name="A" | name ngắn | |
| UT023 | AddressValidator | ghnDistrictId không phải số | Trả lỗi | Integer.parseInt không bọc lỗi gây Exception 500 | Fail | Automated (JUnit 5) | Đỗ Huy Hoàng | 26/07/2026 | Truyền "abc" | ghnDistrictId=abc | Defect: nên bắt lỗi trả 400 |
| UT024 | AddressValidator | WardCode quá dài | Trả lỗi | Trả về chuỗi báo lỗi wardCode | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 26/07/2026 | "A" x 25 ký tự | wardCode dài | |
| UT025 | StoreConfigService | Giờ mở cửa | true | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 26/07/2026 | isOpen("08:00","22:00",10:00) | 08:00-22:00 @10:00 | |
| UT026 | StoreConfigService | Sát giờ đóng | false (close-exclusive) | Trả về false | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 26/07/2026 | isOpen("08:00","22:00",22:00) | @22:00 | |
| UT027 | StoreConfigService | Đúng giờ mở | true | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 26/07/2026 | isOpen("08:00","22:00",08:00) | @08:00 | |
| UT028 | StoreConfigService | Qua đêm | true | Trả về true | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 26/07/2026 | isOpen("22:00","08:00",23:00) | 22:00-08:00 @23:00 | |
| UT029 | StoreConfigService | Mở 24h | true | Trả về true | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 26/07/2026 | isOpen("10:00","10:00",03:00) | mở 24h | |
| UT030 | StoreConfigService | parseFee | null→0; âm→ném | null ra 0, số âm ném IllegalArgumentException | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 26/07/2026 | parseFee(null); parseFee("-1") | null, -1 | |
| UT031 | CouponService | PERCENT tính đúng | 10000 | Trả về 10000 | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 27/07/2026 | calculateDiscount(PERCENT,100000,null,10) | 10% của 100000 | |
| UT032 | CouponService | PERCENT bị cap | 15000 | Giới hạn đúng max discount | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 27/07/2026 | value=20, maxDiscount=15000, total=100000 | 20% cap 15000 | |
| UT033 | CouponService | FIXED vượt tổng | 100000 (không âm) | Trả về 100000, không bị âm | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 27/07/2026 | value=200000, total=100000 | FIXED vượt tổng | |
| UT034 | CouponService | FREE_SHIPPING | 30000 | Trả về 30000 (miễn phí ship) | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 27/07/2026 | calculateDiscount(FREE_SHIPPING,100000,30000) | shipping=30000 | |
| UT035 | CouponService | Type lạ | BigDecimal.ZERO | Trả về 0 | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 27/07/2026 | calculateDiscount("XYZ",...) | type=XYZ | |
| UT036 | CouponService | Verify code rỗng/không tồn tại | valid:false + message | Trả về valid:false kèm message | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 27/07/2026 | verify("",...)/verify("XYZ1",...) | '', XYZ1 | |
| UT037 | OrderTransitionService | Chuyển hợp lệ | true | Trả về true | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 27/07/2026 | canTransition("PENDING","CONFIRMED") | PENDING→CONFIRMED | |
| UT038 | OrderTransitionService | Chuyển ngược | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 27/07/2026 | canTransition("DELIVERED","READY") | DELIVERED→READY | |
| UT039 | OrderTransitionService | Từ CANCELLED | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | canTransition("CANCELLED","PENDING") | CANCELLED→PENDING | |
| UT040 | OrderTransitionService | isCanonicalStatus | true / false | PICKED_UP→true, FOO→false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | isCanonicalStatus("PICKED_UP")/("FOO") | PICKED_UP, FOO | |
| UT041 | OrderTransitionService | canDeliver | false / true | UNPAID→false, PAID→true | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | canDeliver(COD,"UNPAID")/("PAID") | COD | |
| UT042 | OrderTransitionService | canCancel khách hủy READY | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | canCancel(READY, uid, null, false, "USER") | READY, USER | |
| UT043 | OrderService.checkout | Thiếu Idempotency-Key | Ném "Thiếu Idempotency-Key" | Ném đúng exception | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | checkout(..., null, ...) | key=null | |
| UT044 | OrderService.checkout | Key quá dài | Ném "Idempotency-Key quá dài" | Ném đúng exception | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | key > 100 ký tự | key len=105 | |
| UT045 | OrderService.checkout | Phone sai | Ném "Thông tin giao hàng không hợp lệ" | Ném đúng exception | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 27/07/2026 | phone="099999" | phone sai | |
| UT046 | OrderService.checkout | Payment method lạ | Ném "Phương thức thanh toán không hợp lệ" | Ném đúng exception | Pass | Automated (JUnit 5) | Bùi Đức Bình | 27/07/2026 | payment="CRYPTO" | CRYPTO | |
| UT047 | OrderService.checkout | Giỏ hàng rỗng | Ném "Giỏ hàng trống" | Ném đúng exception | Pass | Automated (JUnit 5) | Bùi Đức Bình | 27/07/2026 | items rỗng/null | items=[] | |
| UT048 | OrderService.checkout | Guest dùng coupon | Ném "Vui lòng đăng nhập và nhận mã trước khi sử dụng" | Ném đúng exception | Pass | Automated (JUnit 5) | Bùi Đức Bình | 27/07/2026 | guestCheckout kèm couponCode | Guest + Coupon | |
| UT049 | OrderService (idempotency + cancel) | Replay cùng key+hash | Lần 2 trả đơn cũ, không tạo mới | Trả order cũ, DB không tăng | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | checkout 2 lần cùng key+hash | key-123 | |
| UT050 | OrderService (idempotency + cancel) | Replay khác hash | Ném "Idempotency key đã được dùng cho yêu cầu khác" | Ném đúng exception | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | cùng key, khác hash | key-123 diff hash | |
| UT051 | OrderService (idempotency + cancel) | Key của user khác | Ném "Idempotency key không hợp lệ" | Ném đúng exception | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | dùng key prefix USER:{idKhac} | USER:999 | |
| UT052 | OrderService (idempotency + cancel) | matchesRequestHash | false / false / true | Khớp đúng | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 28/07/2026 | (null,null)/(a,b)/(a,a) | 3 cặp hash | |
| UT053 | OrderService (idempotency + cancel) | Cancel đúng owner | true | Hủy đơn thành công | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | cancelOrder(id, owner) | Order#101 | |
| UT054 | OrderService (idempotency + cancel) | Cancel người khác | false | Trả false, không cho hủy | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | cancelOrder(id, non-owner) | Order#101 | |
| UT055 | CartService | Quantity ≤ 0 | false | Trả về false | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 28/07/2026 | addItem(user,1,1,0,[]) | qty=0 | |
| UT056 | CartService | Variant không AVAILABLE | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 28/07/2026 | variant status UNAVAILABLE | UNAVAILABLE | |
| UT057 | CartService | Modifier sai product | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 28/07/2026 | modifier thuộc product khác | modifier 99 | |
| UT058 | CartService | Vượt min/max group | false | Trả về false | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 28/07/2026 | group min=1 chọn 0 | min=1, selected=0 | |
| UT059 | CartService | Update qty 0 = xóa | Xóa item, trả true | Item bị xóa khỏi giỏ | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 28/07/2026 | updateItemQuantity(itemId,user,0) | qty=0 | |
| UT060 | CartService | Stock không đủ khi merge | false | Trả về false do vượt stock | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 28/07/2026 | line 80, stock 100, thêm 30 | 80+30>100 | |
| UT061 | WorkShiftService | Đang check-in | "CHECKED_IN" | Trả về CHECKED_IN | Pass | Manual (Demo Account) | Nguyễn Nam Phong | 28/07/2026 | shift CHECKED_IN chưa checkout | S-01 | |
| UT062 | WorkShiftService | Đã check-out hết | "CHECKED_OUT" | Trả về CHECKED_OUT | Pass | Manual (Demo Account) | Nguyễn Nam Phong | 28/07/2026 | tất cả shift CHECKED_OUT | S-01 | |
| UT063 | WorkShiftService | Cửa sổ check-in | "CHECK_IN_ALLOWED" | Trả về CHECK_IN_ALLOWED | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | now trong [start-15,end+15] | 07:50 @08:00 | |
| UT064 | WorkShiftService | Sớm quá | "UPCOMING" | Trả về UPCOMING | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | now < start-15 | 07:30 @08:00 | |
| UT065 | WorkShiftService | Check-out sớm | false | Trả về false | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | canCheckOut(now<end, end) | 16:00 @17:00 | |
| UT066 | WorkShiftService | Grace sau giờ kết thúc | true | Trả về true | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 28/07/2026 | isCheckedInWithinGrace(end+15) | 17:15 @17:00 | |
| UT067 | AddressService | Địa chỉ đầu tiên | Địa chỉ mới = default | Tự động set isDefault=true | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | create cho user chưa có địa chỉ | user mới | |
| UT068 | AddressService | Bỏ default cuối | Ném "Set another default address first" | Ném đúng exception | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | update isDefault=false địa chỉ default duy nhất | address 10 | |
| UT069 | AddressService | Xóa default | Địa chỉ mới nhất promote default | Địa chỉ còn lại được promote | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | xóa default khi còn 2 địa chỉ | 2 địa chỉ | |
| UT070 | AddressService | Xóa không sở hữu | Ném "Address not found" | Ném đúng exception | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 28/07/2026 | delete(userA, addressCuaB) | khác user | |
| UT071 | AddressService | Update xuyên user | Ném "Address not found" | Ném đúng exception | Pass | Manual (Demo Account) | Phạm Gia Bảo | 28/07/2026 | update(userA, addressCuaB, ...) | khác user | |
| UT072 | AddressService | Set default mới | Default cũ bị clear | Default cũ chuyển false, địa chỉ mới thành default | Pass | Manual (Demo Account) | Phạm Gia Bảo | 28/07/2026 | create isDefault=true khi đã có default | user 4 | |
| UT073 | LoyaltyService | Điểm theo ngàn | 1 | Trả về 1 | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 28/07/2026 | pointsForAmount(1999) | amount=1999 | |
| UT074 | LoyaltyService | Null amount | 0 | Trả về 0 | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 28/07/2026 | pointsForAmount(null) | null | |
| UT075 | LoyaltyService | Gold | "Gold" | Trả về Gold | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 28/07/2026 | tierForPoints(2000) | 2000 | |
| UT076 | LoyaltyService | Silver | "Silver" | Trả về Silver | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 28/07/2026 | tierForPoints(500) | 500 | |
| UT077 | LoyaltyService | Bronze | "Bronze" | Cả 499 và -5 trả Bronze | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 29/07/2026 | tierForPoints(499)/(-5) | 499, -5 | |
| UT078 | LoyaltyService | Không đủ điều kiện | 0, không ghi EARN | Trả 0 điểm, không tạo EARN | Pass | Manual (Demo Account) | Phạm Gia Bảo | 29/07/2026 | awardForDelivery order PENDING | PENDING | |
| UT079 | InventoryReservationService | Reserved→Consumed | true | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 29/07/2026 | canTransition("RESERVED","CONSUMED") | RESERVED | |
| UT080 | InventoryReservationService | Consumed→Reserved | false | Trả về false | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 29/07/2026 | canTransition("CONSUMED","RESERVED") | CONSUMED | |
| UT081 | InventoryReservationService | Consumed→Wasted | true | Trả về true | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | canTransition("CONSUMED","WASTED") | CONSUMED | |
| UT082 | InventoryReservationService | Hủy khi Reserved | "RELEASE" | Trả về RELEASE | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | cancellationTransactionType("RESERVED") | RESERVED | |
| UT083 | InventoryReservationService | Hủy khi Consumed | "WASTE" | Trả về WASTE | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | cancellationTransactionType("CONSUMED") | CONSUMED | |
| UT084 | InventoryReservationService | Không ném 2 nhóm trên | null | Trả về null | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | cancellationTransactionType("RELEASED") | RELEASED | |
| UT085 | PayOSService + PayOSPaymentService | Cấu hình đủ | isConfigured() → true | isConfigured trả về true | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | ClientId+ApiKey+ChecksumKey non-blank | keys set | |
| UT086 | PayOSService + PayOSPaymentService | Amount không hợp lệ | {error:"Số tiền thanh toán không hợp lệ"} | Trả object lỗi đúng message | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | createPaymentLink amount ≤ 0 | amount=0 | |
| UT087 | PayOSService + PayOSPaymentService | matchesProviderResponse | true; đổi amount → false | Đúng amount → true, đổi amount → false | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | orderCode khớp + amount khớp + linkId non-blank | 1001 | |
| UT088 | PayOSService + PayOSPaymentService | Tạo link khi mới | true | Trả về true | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 29/07/2026 | shouldCreateProviderLink(true,"CREATING") | new=true | |
| UT089 | PayOSService + PayOSPaymentService | Không phải attempt mới | false | Trả về false | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 29/07/2026 | shouldCreateProviderLink(false,"CREATING") | new=false | |
| UT090 | PayOSService + PayOSPaymentService | markPaid trên order CANCELLED | paymentStatus=PAID, refundStatus=PENDING | Cập nhật đúng | Pass | Manual (Demo Account) | Phan Vũ Phúc Khang | 29/07/2026 | markPaid(cancelledOrder, now) | Order#202 | |
| UT091 | SupportTicketService | Subject quá dài | "Invalid ticket data" | Trả về error | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 29/07/2026 | subject 256 ký tự | len=256 | |
| UT092 | SupportTicketService | Category sai | "Invalid ticket data" | Trả về error | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 29/07/2026 | category "GIFT" | GIFT | |
| UT093 | SupportTicketService | Order không thuộc user | "Order not found" | Trả về error | Pass | Manual (Demo Account) | Nguyễn Nam Phong | 29/07/2026 | orderId của user khác | Order#300 | |
| UT094 | SupportTicketService | RESOLVED thiếu resolution | "Resolution is required" | Trả về error | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 29/07/2026 | update(...,"RESOLVED",null) | resolution=null | |
| UT095 | SupportTicketService | Chuyển lui | "Invalid status transition" | Trả về error | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 29/07/2026 | PROCESSING→OPEN | PROCESSING | |
| UT096 | SupportTicketService | Update cùng trạng thái | Cho phép (không lỗi) | Thao tác thành công không báo lỗi | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 29/07/2026 | PROCESSING→PROCESSING | PROCESSING | |
| UT097 | ReviewService | Rating 0 | "Số sao phải là số nguyên từ 1 đến 5" | Trả về lỗi đúng | Pass | Automated (JUnit 5) | Nguyễn Nam Phong | 29/07/2026 | create(user,order,0,...) | rating=0 | |
| UT098 | ReviewService | Rating 6 | Lỗi rating | Trả về lỗi vượt giới hạn | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | create(user,order,6,...) | rating=6 | |
| UT099 | ReviewService | Rating hợp lệ | Tạo thành công | Tạo review 1 sao thành công | Pass | Manual (Demo Account) | Đỗ Huy Hoàng | 30/07/2026 | rating 1 (biên dưới) | rating=1 | |
| UT100 | ReviewService | Comment 1001 ký tự | "Bình luận không được vượt quá 1000 ký tự" | Trả về lỗi quá 1000 ký tự | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | comment len=1001 | len=1001 | |
| UT101 | ReviewService | Comment đúng 1000 | Thành công | Tạo review thành công | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | comment len=1000 | len=1000 | |
| UT102 | ReviewService | Comment chỉ khoảng trắng | Comment lưu = null | Review tạo với comment=null | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | comment="   " | spaces | |
| UT103 | RefundService | Status sai | "Invalid refund status" | Trả về lỗi | Pass | Automated (JUnit 5) | Đỗ Huy Hoàng | 30/07/2026 | update(order,"DONE",...) | DONE | |
| UT104 | RefundService | Order không tồn tại | "Order not found" | Trả về lỗi | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 30/07/2026 | order null | null | |
| UT105 | RefundService | Không đủ điều kiện | "Order is not eligible for refund" | Trả về lỗi | Pass | Manual (Demo Account) | Nguyễn Thành Phát | 30/07/2026 | order PENDING/UNPAID | PENDING | |
| UT106 | RefundService | Amount âm | "Invalid refund amount" | Trả về lỗi | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 30/07/2026 | amount=-1000 | -1000 | |
| UT107 | RefundService | Amount vượt total | "Invalid refund amount" | Trả về lỗi | Pass | Automated (JUnit 5) | Nguyễn Thành Phát | 30/07/2026 | amount>finalAmount | 500000/200000 | |
| UT108 | RefundService | Refund hợp lệ | refundStatus=REFUNDED, reverseForRefund chạy | Refund thành công, reverse kích hoạt | Pass | Manual (Demo Account) | Nguyễn Thành Phát | 30/07/2026 | REFUNDED + amount ≤ finalAmount | 50000/100000 | |
| UT109 | Shipper/Staff/Guest | Nhân viên hợp lệ | true | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 31/07/2026 | isValidStaffIdentity("STAFF","ACTIVE") | STAFF/ACTIVE | |
| UT110 | Shipper/Staff/Guest | Shipper đọc đơn đã giao | true (không cần check-in) | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 31/07/2026 | canReadOwnedOrder("DELIVERED", false) | DELIVERED | |
| UT111 | Shipper/Staff/Guest | Pickup đúng shipper | true | Trả về true | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 31/07/2026 | canPickUp("ASSIGNED", 3, 3) | ASSIGNED/3 | |
| UT112 | Shipper/Staff/Guest | Action của shipper | {PICKED_UP} | Trả về danh sách chứa PICKED_UP | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 31/07/2026 | getAllowedActions("ASSIGNED",...) | ASSIGNED | |
| UT113 | Shipper/Staff/Guest | GuestReturnProof roundtrip | true / false | Khớp hash → true, sai token → false | Pass | Automated (JUnit 5) | Phạm Gia Bảo | 31/07/2026 | verify(token, hash(token)) | token test | |
| UT114 | Shipper/Staff/Guest | History map note null | note = "" | Map note null thành chuỗi rỗng | Pass | Automated (JUnit 5) | Phan Vũ Phúc Khang | 31/07/2026 | map item note=null | note=null | |
| UT115 | Frontend (smoke/validation) | Register mật khẩu yếu | Chặn client, không gọi API | Form hiện validation error | Pass | Manual (Frontend/Smoke) | Phan Vũ Phúc Khang | 31/07/2026 | nhập password 6 ký tự không số | abcdef | |
| UT116 | Frontend (smoke/validation) | Register xác nhận sai | Chặn client | Chặn submit và báo Mật khẩu không khớp | Pass | Manual (Frontend/Smoke) | Phan Vũ Phúc Khang | 31/07/2026 | pass/confirm khác | 123456aA/aB | |
| UT117 | Frontend (smoke/validation) | Checkout thiếu địa chỉ GHN | Nút đặt hàng bị disable | Nút Đặt hàng bị vô hiệu hóa | Pass | Manual (Frontend/Smoke) | Phan Vũ Phúc Khang | 31/07/2026 | chưa chọn district/ward | District unselected | |
| UT118 | Frontend (smoke/validation) | Track-order sai suffix | Báo lỗi, không tra được | Báo lỗi định dạng mã tra cứu | Pass | Manual (Frontend/Smoke) | Nguyễn Nam Phong | 31/07/2026 | suffix "12" (không đủ 4 số) | code+suffix 12 | |
| UT119 | Frontend (smoke/validation) | Cancel thiếu lý do | Chặn submit | Chặn submit và yêu cầu nhập lý do hủy | Pass | Manual (Frontend/Smoke) | Nguyễn Nam Phong | 31/07/2026 | reason rỗng/>500 | '' | |
| UT120 | Frontend (smoke/validation) | Shipper giao COD sai tiền | Chặn, không gọi /deliver | Báo lỗi số tiền thu không khớp | Pass | Manual (Frontend/Smoke) | Nguyễn Nam Phong | 31/07/2026 | collected ≠ finalAmount | 50000/60000 | |
