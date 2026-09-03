# 100 câu hỏi vấn đáp nghiệp vụ dự án FastGuy

Tài liệu này tập trung vào nghiệp vụ, luồng xử lý và các quy tắc quan trọng của FastGuy. Khi thi, nên trả lời theo bốn ý: **ai thực hiện, điều kiện gì, hệ thống xử lý ra sao, vì sao cần quy tắc đó**. Có thể dùng ví dụ thực tế để câu trả lời tự nhiên hơn, không cần trình bày sâu về mã nguồn.

## Mục lục

1. Tổng quan và phạm vi dự án — câu 1–10
2. Vai trò và phân quyền — câu 11–20
3. Menu, giỏ hàng và đặt hàng — câu 21–30
4. Thanh toán, hủy đơn và hoàn tiền — câu 31–40
5. Tiếp nhận và chế biến — câu 41–50
6. Phân công và giao hàng — câu 51–60
7. Kho nguyên liệu — câu 61–70
8. Khuyến mãi và khách hàng thân thiết — câu 71–80
9. Hậu mãi và chăm sóc khách hàng — câu 81–90
10. Quản trị, báo cáo và xử lý ngoại lệ — câu 91–100

---

## 1. Tổng quan và phạm vi dự án

### Câu 1: FastGuy giải quyết bài toán gì?

**Trả lời:** FastGuy hỗ trợ một cửa hàng đồ ăn nhanh quản lý trọn vẹn quá trình bán hàng trực tuyến. Hệ thống kết nối việc xem món, đặt hàng, thanh toán, chế biến, giao hàng và chăm sóc sau bán. Mục tiêu là giảm thao tác rời rạc và giúp mọi bộ phận cùng theo dõi một đơn hàng thống nhất.

### Câu 2: Giá trị chính của FastGuy đối với khách hàng là gì?

**Trả lời:** Khách hàng có thể xem món, chọn cấu hình phù hợp, đặt hàng và theo dõi tiến độ trên cùng một hệ thống. Giá, phí giao và ưu đãi được kiểm tra lại trước khi tạo đơn. Nhờ vậy, khách dễ biết đơn đang ở đâu và cần làm gì tiếp theo.

### Câu 3: Giá trị chính của FastGuy đối với cửa hàng là gì?

**Trả lời:** Cửa hàng có một quy trình chung để tiếp nhận đơn, chế biến, giao hàng và xử lý ngoại lệ. Người quản lý theo dõi được doanh thu, tồn kho, nhân sự, hoàn tiền và các vấn đề vận hành. Dữ liệu được lưu theo lịch sử nên dễ kiểm tra khi có tranh chấp.

### Câu 4: FastGuy khác một website chỉ đăng menu ở điểm nào?

**Trả lời:** Website đăng menu chủ yếu giúp khách xem thông tin, còn FastGuy quản lý cả vòng đời của đơn hàng. Mỗi bước đều có người chịu trách nhiệm, điều kiện thực hiện và trạng thái rõ ràng. Hệ thống còn liên kết đơn với thanh toán, tồn kho, coupon, giao hàng và hậu mãi.

### Câu 5: Vì sao đơn hàng được xem là trung tâm của hệ thống?

**Trả lời:** Hầu hết nghiệp vụ đều phát sinh từ đơn hàng: thanh toán, chế biến, giao hàng, tích điểm và báo cáo. Khi trạng thái đơn thay đổi, trách nhiệm cũng chuyển từ khách sang cửa hàng rồi đến shipper. Lấy đơn hàng làm trung tâm giúp các bộ phận không xử lý mâu thuẫn với nhau.

### Câu 6: Phạm vi tổ chức hiện tại của FastGuy là gì?

**Trả lời:** FastGuy hướng đến mô hình một cửa hàng và một kho vận hành chính. Hệ thống chưa đặt mục tiêu trở thành sàn nhiều nhà bán hoặc hệ thống quản trị nhiều chi nhánh. Giới hạn này giúp dự án tập trung xử lý tốt quy trình cốt lõi của một cửa hàng đồ ăn nhanh.

### Câu 7: Những phần nào không thuộc phạm vi chính của FastGuy?

**Trả lời:** FastGuy không thay thế hệ thống kế toán chuyên nghiệp, ERP mua hàng hoặc phần mềm tối ưu tuyến giao hàng. Dự án cũng chưa tập trung vào nhiều kho, nhiều chi nhánh hay theo dõi nguyên liệu theo từng lô phức tạp. Các nội dung đó có thể là hướng mở rộng sau này.

### Câu 8: Một luồng đơn hàng thành công diễn ra như thế nào?

**Trả lời:** Khách chọn món, xác nhận giỏ, địa chỉ, ưu đãi và phương thức thanh toán để tạo đơn. Cửa hàng xác nhận, chuẩn bị món và đánh dấu sẵn sàng; sau đó đơn được giao cho shipper. Khi giao thành công, đơn được hoàn tất và các nghiệp vụ như tích điểm, đối soát, báo cáo được cập nhật.

### Câu 9: Vì sao hệ thống cần nhiều trạng thái đơn hàng?

**Trả lời:** Mỗi trạng thái thể hiện một giai đoạn và một trách nhiệm khác nhau. Ví dụ, đơn đang chế biến không thể được xử lý giống đơn đang chờ xác nhận hoặc đã giao. Trạng thái rõ ràng giúp ngăn thao tác sai và cho khách biết tiến độ thực tế.

### Câu 10: Nguyên tắc chung khi FastGuy xử lý một thao tác quan trọng là gì?

**Trả lời:** Hệ thống kiểm tra đúng người, đúng quyền, đúng đối tượng và đúng thời điểm. Sau đó mới kiểm tra các điều kiện nghiệp vụ như trạng thái đơn, số lượng tồn hoặc số tiền. Nếu điều kiện không còn đúng, thao tác bị từ chối thay vì cố xử lý và tạo dữ liệu sai.

---

## 2. Vai trò và phân quyền

### Câu 11: FastGuy có những nhóm người dùng chính nào?

**Trả lời:** Hệ thống có khách chưa đăng nhập, khách có tài khoản, nhân viên cửa hàng, shipper và quản trị viên. Mỗi nhóm nhìn thấy dữ liệu và thực hiện công việc khác nhau. Việc tách vai trò giúp xác định rõ trách nhiệm và bảo vệ thông tin.

### Câu 12: Guest và User khác nhau như thế nào?

**Trả lời:** Guest là khách chưa đăng nhập nhưng vẫn có thể xem menu và đặt hàng theo luồng được hỗ trợ. User có tài khoản nên có thể quản lý địa chỉ, theo dõi lịch sử đơn, coupon, điểm thưởng và các tiện ích cá nhân. Guest phải cung cấp thông tin xác minh phù hợp khi tra cứu đơn.

### Câu 13: Staff chịu trách nhiệm gì?

**Trả lời:** Staff tiếp nhận đơn, xác nhận, chế biến và chuẩn bị đơn để giao. Khi đơn sẵn sàng, Staff có thể phân công shipper và hỗ trợ xử lý sự cố vận hành. Staff không được tự thực hiện các bước giao hàng thay cho shipper.

### Câu 14: Shipper chịu trách nhiệm gì?

**Trả lời:** Shipper nhận những đơn đã được cửa hàng phân công, lấy hàng và giao cho khách. Shipper cập nhật kết quả giao thành công hoặc lý do giao thất bại. Shipper không tự chọn đơn, tự sửa giá hay tự hủy đơn.

### Câu 15: Admin chịu trách nhiệm gì?

**Trả lời:** Admin quản lý dữ liệu nền và giám sát hoạt động chung như sản phẩm, người dùng, ca làm, kho, coupon, đơn hàng và báo cáo. Admin có thể xử lý một số ngoại lệ mà vai trò khác không được phép. Những thao tác nhạy cảm vẫn phải có điều kiện và lý do để tránh lạm quyền.

### Câu 16: Vì sao chỉ ẩn nút trên giao diện là chưa đủ để phân quyền?

**Trả lời:** Người dùng có thể không nhìn thấy nút nhưng vẫn có thể cố gửi yêu cầu bằng cách khác. Vì vậy, hệ thống phía sau phải kiểm tra lại danh tính và quyền ở mọi thao tác quan trọng. Giao diện chỉ giúp sử dụng thuận tiện, không phải lớp bảo vệ cuối cùng.

### Câu 17: Quyền sở hữu dữ liệu được hiểu như thế nào?

**Trả lời:** Người dùng chỉ được xem hoặc sửa dữ liệu thuộc về mình, chẳng hạn địa chỉ và đơn hàng cá nhân. Shipper chỉ được thao tác trên đơn được gán cho chính mình. Quy tắc này ngăn một người truy cập dữ liệu của người khác dù họ biết mã định danh.

### Câu 18: Vì sao trạng thái tài khoản ảnh hưởng đến quyền thao tác?

**Trả lời:** Có đúng vai trò vẫn chưa đủ nếu tài khoản đã bị khóa hoặc ngừng hoạt động. Hệ thống cần kiểm tra trạng thái hiện tại trước khi cho phép thao tác. Điều này giúp quyền bị thu hồi có hiệu lực ngay, thay vì tiếp tục dùng thông tin đăng nhập cũ.

### Câu 19: Vì sao Staff và Shipper cần ở trong ca làm việc hợp lệ?

**Trả lời:** Các thao tác vận hành phải gắn với người đang thực sự làm việc tại thời điểm đó. Ca làm giúp xác định trách nhiệm, thời gian và dữ liệu đối soát. Nhân viên chưa check-in hoặc đã kết thúc ca sẽ không được tiếp tục cập nhật nghiệp vụ quan trọng.

### Câu 20: Vì sao hệ thống không cho vô hiệu hóa quản trị viên đang hoạt động cuối cùng?

**Trả lời:** Nếu không còn quản trị viên hoạt động, cửa hàng có thể mất khả năng quản lý hệ thống. Quy tắc này bảo đảm luôn có ít nhất một người đủ quyền xử lý sự cố và quản trị tài khoản. Đây là một biện pháp bảo vệ hoạt động liên tục.

---

## 3. Menu, giỏ hàng và đặt hàng

### Câu 21: Menu của FastGuy gồm những thành phần nào?

**Trả lời:** Menu được tổ chức theo danh mục, sản phẩm và các lựa chọn cụ thể như kích cỡ hoặc phiên bản món. Một món có thể có lựa chọn thêm và cũng có thể nằm trong combo. Cấu trúc này giúp khách tùy chỉnh món nhưng cửa hàng vẫn quản lý được giá và công thức.

### Câu 22: Vì sao cần phân biệt sản phẩm và biến thể sản phẩm?

**Trả lời:** Sản phẩm thể hiện món chung, còn biến thể thể hiện lựa chọn thực tế được bán, ví dụ kích cỡ nhỏ hoặc lớn. Mỗi biến thể có thể có giá, mã hàng và khả năng bán khác nhau. Khi đặt hàng, khách phải chọn đúng biến thể để hệ thống tính chính xác.

### Câu 23: Hệ thống kiểm tra món thêm như thế nào?

**Trả lời:** Món thêm phải thuộc đúng nhóm lựa chọn của sản phẩm đang mua. Hệ thống kiểm tra số lựa chọn tối thiểu, tối đa và trạng thái đang bán. Nhờ đó khách không thể ghép một món thêm không phù hợp hoặc vượt giới hạn cửa hàng quy định.

### Câu 24: Giỏ hàng của Guest được xử lý ra sao?

**Trả lời:** Guest có thể tạo giỏ tạm trong quá trình mua hàng. Khi đăng nhập, hệ thống có thể kết hợp giỏ tạm với giỏ tài khoản theo quy tắc tránh trùng cấu hình món. Mục tiêu là không làm mất lựa chọn của khách nhưng cũng không tạo các dòng trùng khó hiểu.

### Câu 25: Khi nào hai món trong giỏ được xem là giống nhau?

**Trả lời:** Hai dòng chỉ giống nhau khi cùng sản phẩm, cùng biến thể và cùng toàn bộ lựa chọn thêm. Nếu khác kích cỡ hoặc khác topping thì phải là hai dòng riêng. Quy tắc này giúp số lượng và giá của từng cấu hình được tính đúng.

### Câu 26: Vì sao hệ thống phải kiểm tra lại giỏ lúc thanh toán?

**Trả lời:** Từ lúc khách thêm món đến lúc thanh toán, giá, trạng thái bán hoặc tồn kho có thể đã thay đổi. Hệ thống không thể tin hoàn toàn dữ liệu cũ trên trình duyệt. Vì vậy, mọi món và số tiền được kiểm tra lại trước khi đơn được tạo.

### Câu 27: Vì sao khách không được tự gửi tổng tiền cuối cùng?

**Trả lời:** Dữ liệu từ trình duyệt có thể cũ hoặc bị thay đổi. Hệ thống phải tự tính giá món, món thêm, giảm giá, phí giao và tổng thanh toán. Điều này bảo vệ cả khách hàng lẫn cửa hàng khỏi sai lệch số tiền.

### Câu 28: Địa chỉ giao hàng được kiểm tra như thế nào?

**Trả lời:** Địa chỉ phải có đủ thông tin cần thiết và thuộc đúng người dùng nếu lấy từ sổ địa chỉ. Các mã khu vực phải phù hợp để tính phí và hỗ trợ giao hàng. Nếu địa chỉ không hợp lệ, hệ thống yêu cầu khách sửa trước khi tạo đơn.

### Câu 29: Phí giao hàng được xác định như thế nào?

**Trả lời:** Hệ thống dựa trên địa chỉ và dịch vụ giao hàng được cấu hình để lấy mức phí phù hợp. Nếu không thể xác minh phí, hệ thống không nên tự đoán một con số bất kỳ. Tùy quy tắc vận hành, hệ thống sẽ báo lỗi rõ ràng hoặc dùng mức dự phòng đã được cửa hàng quy định trước.

### Câu 30: FastGuy tránh tạo hai đơn khi khách bấm thanh toán nhiều lần ra sao?

**Trả lời:** Mỗi lần xác nhận checkout có một dấu hiệu nhận biết riêng. Nếu cùng khách gửi lại đúng nội dung đó, hệ thống trả lại kết quả đã tạo thay vì tạo đơn mới. Nếu nội dung đã thay đổi nhưng vẫn dùng dấu hiệu cũ, hệ thống từ chối để khách xác nhận lại.

---

## 4. Thanh toán, hủy đơn và hoàn tiền

### Câu 31: FastGuy hỗ trợ những phương thức thanh toán chính nào?

**Trả lời:** Hệ thống hỗ trợ thanh toán khi nhận hàng và chuyển khoản trực tuyến qua cổng thanh toán. Mỗi phương thức có cách xác nhận và thời hạn chờ khác nhau. Khách chọn phương thức trước khi hoàn tất đặt hàng.

### Câu 32: Đơn COD được xem là đã thanh toán khi nào?

**Trả lời:** Đơn COD chỉ được xem là đã thanh toán khi shipper giao hàng và thu đủ số tiền cần thu. Số tiền thu phải khớp với tổng tiền cuối cùng của đơn. Việc chỉ đánh dấu “đã giao” mà chưa xác nhận tiền là chưa đủ cho đối soát.

### Câu 33: Chuyển khoản trực tuyến được xác nhận như thế nào?

**Trả lời:** Hệ thống chỉ ghi nhận thanh toán khi nhận được bằng chứng đáng tin cậy từ cổng thanh toán. Thông tin đơn, số tiền và mã giao dịch phải khớp. Trang khách quay về sau thanh toán chỉ để hiển thị kết quả, không tự nó chứng minh tiền đã vào.

### Câu 34: Vì sao không thể tin tham số “thanh toán thành công” trên trình duyệt?

**Trả lời:** Thông tin trên trình duyệt có thể bị sửa hoặc xuất hiện khi giao dịch chưa thực sự hoàn tất. Nếu tin ngay, hệ thống có thể giao món khi chưa nhận được tiền. Vì vậy, FastGuy phải đối chiếu với thông báo hoặc trạng thái chính thức từ nhà cung cấp thanh toán.

### Câu 35: Điều gì xảy ra khi đơn chuyển khoản chờ quá lâu?

**Trả lời:** Đơn chuyển khoản chưa được thanh toán trong thời hạn quy định sẽ bị hủy để tránh giữ món và tài nguyên vô thời hạn. Các phần đã giữ như tồn kho hoặc coupon sẽ được xử lý theo quy tắc hoàn trả. Nếu tiền về sát thời điểm hủy, hệ thống phải kiểm tra trạng thái thực tế để tránh xử lý mâu thuẫn.

### Câu 36: Người dùng được tự hủy đơn khi nào?

**Trả lời:** Người dùng chỉ được tự hủy đơn của mình khi đơn còn ở giai đoạn chờ xử lý. Khi cửa hàng đã xác nhận hoặc bắt đầu chế biến, việc hủy cần do nhân viên có thẩm quyền xử lý. Quy tắc này tránh lãng phí món đã chuẩn bị và tranh chấp trách nhiệm.

### Câu 37: Khi hủy đơn trước chế biến, hệ thống xử lý những gì?

**Trả lời:** Hệ thống đổi trạng thái đơn sang đã hủy và giải phóng phần tồn kho đã giữ. Coupon đã gắn với đơn cũng được hoàn lại nếu đủ điều kiện. Toàn bộ thay đổi phải diễn ra thống nhất để không có tình trạng đơn hủy nhưng kho vẫn bị giữ.

### Câu 38: Vì sao đơn đã thanh toán bị hủy chưa thể coi là đã hoàn tiền?

**Trả lời:** Hủy đơn là dừng việc thực hiện đơn, còn hoàn tiền là một nghiệp vụ tài chính riêng. Hệ thống tạo yêu cầu hoàn tiền ở trạng thái chờ xử lý và theo dõi đến khi hoàn tất. Tách hai bước giúp cửa hàng kiểm soát số tiền, lý do và tránh hoàn lặp.

### Câu 39: Điều kiện cơ bản để một đơn được hoàn tiền là gì?

**Trả lời:** Đơn phải thực sự đã thanh toán và ở trạng thái cho phép hoàn, chẳng hạn đã hủy hoặc đã trả về cửa hàng. Yêu cầu hoàn tiền phải còn hiệu lực và chưa được hoàn trước đó. Nhân viên cần kiểm tra số tiền, lý do và lịch sử xử lý.

### Câu 40: Nếu đơn đã tích điểm nhưng sau đó được hoàn tiền thì sao?

**Trả lời:** Điểm đã cộng từ đơn phải được thu hồi tương ứng khi hoàn tiền hoàn tất. Nếu không, khách vừa nhận lại tiền vừa giữ quyền lợi phát sinh từ giao dịch không còn doanh thu. Lịch sử điểm vẫn được giữ để có thể giải thích thay đổi cho khách.

---

## 5. Tiếp nhận và chế biến

### Câu 41: Các trạng thái chính trước khi giao hàng là gì?

**Trả lời:** Đơn thường đi từ chờ xác nhận, đã xác nhận, đang chuẩn bị đến sẵn sàng giao. Mỗi lần chuyển trạng thái cho biết công việc đã tiến thêm một bước. Hệ thống không cho nhảy tùy ý vì có thể bỏ qua trách nhiệm hoặc kiểm tra cần thiết.

### Câu 42: Trạng thái PENDING có ý nghĩa gì?

**Trả lời:** PENDING nghĩa là đơn đã được tạo nhưng đang chờ cửa hàng tiếp nhận. Ở giai đoạn này, thông tin thanh toán và điều kiện đơn vẫn có thể cần được xác minh. Đây cũng là giai đoạn người dùng có thể tự hủy theo quy tắc của hệ thống.

### Câu 43: Khi nào Staff được xác nhận đơn?

**Trả lời:** Staff phải có tài khoản hoạt động, đang trong ca hợp lệ và đơn đang ở trạng thái chờ xác nhận. Hệ thống cũng cần bảo đảm đơn không bị người khác xử lý trước đó. Sau khi xác nhận, cửa hàng chính thức nhận trách nhiệm chuẩn bị món.

### Câu 44: Trạng thái PREPARING có ý nghĩa gì?

**Trả lời:** PREPARING nghĩa là cửa hàng đã bắt đầu chế biến hoặc đóng gói đơn. Từ thời điểm này, việc hủy trở nên nhạy cảm hơn vì nguyên liệu có thể đã được sử dụng. Hệ thống cần phân biệt rõ với đơn mới chỉ được xác nhận nhưng chưa chế biến.

### Câu 45: Khi nào đơn được chuyển sang READY?

**Trả lời:** Đơn được chuyển sang READY khi món đã hoàn tất, đóng gói và sẵn sàng bàn giao. Staff chịu trách nhiệm xác nhận bước này. Chỉ sau đó đơn mới phù hợp để gán cho shipper nhận giao.

### Câu 46: Vì sao không cho chuyển thẳng từ PENDING sang READY?

**Trả lời:** Chuyển thẳng sẽ bỏ qua bước cửa hàng xác nhận và ghi nhận quá trình chuẩn bị. Điều đó làm lịch sử khó hiểu và có thể gây sai trách nhiệm. Luồng tuần tự giúp hệ thống phản ánh đúng hoạt động thực tế.

### Câu 47: Nếu hai nhân viên cùng xử lý một đơn thì sao?

**Trả lời:** Hệ thống chỉ chấp nhận người hoàn thành thao tác hợp lệ trước. Người còn lại sẽ nhận thông báo rằng trạng thái đơn đã thay đổi và cần tải lại dữ liệu. Cách này tránh một đơn bị xác nhận hoặc chuyển bước hai lần.

### Câu 48: Vì sao cần lưu lịch sử trạng thái đơn?

**Trả lời:** Lịch sử cho biết đơn đã đi qua bước nào, lúc nào và do ai thực hiện. Khi khách phản ánh chậm hoặc sai quy trình, cửa hàng có dữ liệu để kiểm tra. Báo cáo thời gian xử lý cũng dựa trên lịch sử này.

### Câu 49: Hệ thống xử lý đơn bị chậm như thế nào?

**Trả lời:** Đơn có thể được đánh dấu theo thời gian chờ hoặc đưa vào nhóm cần chú ý để nhân viên ưu tiên. Quản lý có thể xem giai đoạn nào gây chậm và ai đang phụ trách. Hệ thống hỗ trợ phát hiện vấn đề, còn quyết định điều phối vẫn thuộc người vận hành.

### Câu 50: Vì sao Staff không được tự đánh dấu đã giao?

**Trả lời:** Công việc của Staff kết thúc ở bước chuẩn bị và bàn giao đơn. Kết quả giao hàng phải do shipper được phân công xác nhận vì họ trực tiếp thực hiện. Tách trách nhiệm này giúp dữ liệu đáng tin cậy và tránh một người tự hoàn tất toàn bộ quy trình.

---

## 6. Phân công và giao hàng

### Câu 51: Khi nào một đơn được gán cho shipper?

**Trả lời:** Đơn chỉ được gán khi đã sẵn sàng giao. Shipper phải có tài khoản hoạt động và đáp ứng điều kiện ca làm. Việc gán quá sớm có thể khiến shipper chờ món hoặc lấy nhầm đơn chưa hoàn tất.

### Câu 52: Vì sao shipper không tự chọn đơn để giao?

**Trả lời:** Cửa hàng cần kiểm soát thứ tự ưu tiên, khu vực và khối lượng công việc. Nếu shipper tự chọn, các đơn khó hoặc xa có thể bị bỏ lại. Phân công tập trung giúp trách nhiệm của từng đơn rõ ràng hơn.

### Câu 53: Khi nào shipper được xác nhận đã lấy hàng?

**Trả lời:** Shipper chỉ được xác nhận khi đơn đã gán cho mình và đang ở trạng thái phù hợp. Thao tác này cho biết trách nhiệm đã chuyển từ cửa hàng sang người giao. Sau đó đơn được theo dõi trong giai đoạn đang giao.

### Câu 54: Điều kiện để xác nhận giao hàng thành công là gì?

**Trả lời:** Shipper phải đúng là người được giao đơn và đơn đã được lấy khỏi cửa hàng. Với COD, số tiền thu phải khớp số tiền khách cần thanh toán. Khi đủ điều kiện, đơn mới chuyển sang hoàn tất.

### Câu 55: Giao hàng thất bại được ghi nhận như thế nào?

**Trả lời:** Shipper chọn lý do phù hợp và có thể bổ sung ghi chú để cửa hàng hiểu tình huống. Hệ thống lưu số lần giao và lịch sử thất bại. Dữ liệu này là cơ sở để quyết định giao lại, đổi shipper hoặc trả đơn về cửa hàng.

### Câu 56: Vì sao DELIVERY_FAILED không đồng nghĩa với CANCELLED?

**Trả lời:** Giao thất bại chỉ cho biết một lần giao chưa thành công, chưa có nghĩa khách không còn nhận đơn. Cửa hàng có thể liên hệ khách và tổ chức giao lại. Chỉ khi quyết định dừng đơn hoặc trả về cửa hàng mới xử lý các hậu quả tiếp theo.

### Câu 57: Khi nào đơn có thể được giao lại?

**Trả lời:** Đơn được giao lại khi vẫn còn phù hợp để giao, khách xác nhận nhận hàng và chưa vượt giới hạn số lần thử. Cửa hàng có thể giữ shipper cũ hoặc phân công lại tùy tình huống. Mỗi lần thử phải được ghi nhận để tránh lặp vô hạn.

### Câu 58: Khi nào cần đổi shipper?

**Trả lời:** Cửa hàng có thể đổi shipper khi người cũ không thể tiếp tục, hết ca hoặc gặp sự cố. Việc đổi phải do người có quyền thực hiện và được lưu lịch sử. Shipper mới chỉ được thao tác sau khi đơn chính thức được gán lại.

### Câu 59: RETURNED_TO_STORE có ý nghĩa gì?

**Trả lời:** Trạng thái này cho biết đơn không giao được và đã được đưa trở lại cửa hàng. Đây là điểm cửa hàng quyết định xử lý món, coupon và hoàn tiền nếu khách đã trả trước. Không nên coi món đã trả về là tồn kho bán được như nguyên liệu chưa chế biến.

### Câu 60: Đối soát COD theo ca nhằm mục đích gì?

**Trả lời:** Đối soát so sánh số tiền hệ thống ghi nhận với số tiền shipper bàn giao trong ca. Nếu chênh lệch, quản lý có thể kiểm tra từng đơn và lý do. Gắn đối soát với ca làm giúp xác định rõ người, thời gian và trách nhiệm.

---

## 7. Kho nguyên liệu

### Câu 61: Vì sao FastGuy quản lý kho theo nguyên liệu?

**Trả lời:** Một món ăn thường được tạo từ nhiều nguyên liệu như bánh, thịt, rau và sốt. Quản lý theo nguyên liệu phản ánh đúng lượng thực tế được nhập và sử dụng. Cửa hàng cũng biết nguyên liệu nào sắp thiếu dù sản phẩm vẫn còn hiển thị trên menu.

### Câu 62: Công thức món có vai trò gì trong quản lý kho?

**Trả lời:** Công thức cho biết một phần món cần bao nhiêu của từng nguyên liệu. Khi đơn được chế biến, hệ thống dựa vào công thức để tính lượng tiêu hao. Nếu công thức sai, tồn kho và giá vốn của món cũng sẽ sai.

### Câu 63: Tạo nguyên liệu mới có làm tăng tồn kho không?

**Trả lời:** Không. Tạo nguyên liệu chỉ khai báo tên, đơn vị và các mức quản lý cần thiết. Số lượng chỉ tăng khi có nghiệp vụ nhập hàng hoặc điều chỉnh hợp lệ, nhờ đó nguồn gốc tồn kho luôn rõ ràng.

### Câu 64: Nhập hàng được ghi nhận như thế nào?

**Trả lời:** Nhân viên chọn nguyên liệu, số lượng, đơn giá và thông tin chứng từ cần thiết. Khi phiếu nhập được chấp nhận, tồn kho tăng và hệ thống lưu bằng chứng giao dịch. Dữ liệu này phục vụ kiểm tra số lượng và tính giá vốn.

### Câu 65: Tồn thực tế, tồn đã giữ và tồn khả dụng khác nhau thế nào?

**Trả lời:** Tồn thực tế là lượng đang được hệ thống ghi nhận trong kho. Tồn đã giữ là phần đã dành cho các đơn nhưng chưa tiêu hao. Tồn khả dụng là phần còn có thể tiếp tục bán, thường được tính từ tồn thực tế trừ phần đã giữ.

### Câu 66: Vì sao phải giữ tồn kho khi khách đặt hàng?

**Trả lời:** Giữ tồn giúp bảo đảm nguyên liệu cho đơn đã được chấp nhận và tránh bán cùng một lượng cho nhiều khách. Khi đơn tiếp tục chế biến, phần giữ được chuyển thành tiêu hao. Nếu đơn hủy đúng điều kiện, phần giữ được giải phóng.

### Câu 67: Giữ tồn, tiêu hao, giải phóng và hao hụt khác nhau thế nào?

**Trả lời:** Giữ tồn chỉ tạm dành nguyên liệu, chưa có nghĩa nguyên liệu đã dùng. Tiêu hao xảy ra khi nguyên liệu được dùng để chế biến, còn giải phóng trả phần đã giữ về khả dụng. Hao hụt ghi nhận nguyên liệu mất do hỏng, hết hạn hoặc sự cố và cần có lý do.

### Câu 68: Kiểm kê kho nhằm mục đích gì?

**Trả lời:** Kiểm kê so sánh số lượng hệ thống với số lượng nhân viên đếm được ngoài thực tế. Chênh lệch được ghi nhận để số liệu phản ánh đúng kho thật. Lịch sử trước và sau kiểm kê giúp quản lý tìm nguyên nhân thất thoát.

### Câu 69: Nếu kiểm kê cho thấy tồn thực tế thấp hơn lượng đã giữ thì sao?

**Trả lời:** Hệ thống vẫn phải ghi nhận sự thật là kho đang thiếu, thay vì cấm phê duyệt kết quả đếm. Tồn khả dụng cho đơn mới được giới hạn về không để không bán thêm. Cửa hàng cần xử lý các đơn đã cam kết và điều tra nguyên nhân chênh lệch.

### Câu 70: Giá vốn bình quân được hiểu đơn giản như thế nào?

**Trả lời:** Khi nhập cùng nguyên liệu với nhiều mức giá, hệ thống tính một mức giá trung bình dựa trên lượng cũ và lượng mới. Ví dụ, nhập thêm nguyên liệu giá cao sẽ làm giá bình quân tăng theo tỷ trọng. Cách này đủ rõ ràng cho vận hành mà không cần theo dõi từng lô xuất riêng.

---

## 8. Khuyến mãi và khách hàng thân thiết

### Câu 71: Coupon trong FastGuy có tác dụng gì?

**Trả lời:** Coupon cung cấp ưu đãi như giảm theo phần trăm, giảm số tiền cố định hoặc miễn phí giao hàng. Mỗi coupon có thời gian, điều kiện đơn tối thiểu và giới hạn sử dụng. Hệ thống kiểm tra lại tại checkout trước khi áp dụng.

### Câu 72: Vì sao coupon đang hiển thị chưa chắc áp dụng được?

**Trả lời:** Coupon có thể đã hết hạn, hết lượt, chưa đạt giá trị đơn tối thiểu hoặc không thuộc ví của khách. Trạng thái có thể thay đổi từ lúc khách xem đến lúc thanh toán. Vì vậy, hệ thống phải xác minh điều kiện ở thời điểm tạo đơn.

### Câu 73: Ví coupon của người dùng là gì?

**Trả lời:** Ví coupon lưu những ưu đãi người dùng đã nhận hoặc được cấp. Nó giúp hệ thống xác định quyền sử dụng của từng khách. Một coupon yêu cầu sở hữu sẽ không thể dùng chỉ bằng cách đoán hoặc nhập mã của người khác.

### Câu 74: Vì sao không cho một người nhận trùng cùng coupon?

**Trả lời:** Nếu chương trình quy định mỗi khách chỉ nhận một lần, nhận trùng sẽ làm sai ngân sách khuyến mãi. Hệ thống kiểm tra cả người dùng và coupon trước khi thêm vào ví. Việc này cũng giúp báo cáo số người tham gia chính xác hơn.

### Câu 75: Lượt dùng coupon được gắn với đơn như thế nào?

**Trả lời:** Khi checkout thành công, lượt sử dụng được liên kết với đúng đơn. Một lượt không thể gắn cho nhiều đơn khác nhau. Nếu tạo đơn thất bại hoặc đơn được hủy hợp lệ, hệ thống xử lý hoàn quyền theo chính sách.

### Câu 76: Nếu hai khách cùng dùng lượt coupon cuối cùng thì sao?

**Trả lời:** Hệ thống phải kiểm tra giới hạn tại thời điểm xác nhận, không chỉ khi hiển thị coupon. Yêu cầu được chấp nhận trước sẽ sử dụng lượt còn lại; yêu cầu sau được báo rằng ưu đãi không còn. Cách này tránh tổng số lượt vượt mức chương trình.

### Câu 77: Điểm thưởng được cộng khi nào?

**Trả lời:** Điểm chỉ nên được cộng khi đơn đã giao thành công và đủ điều kiện của chương trình. Không cộng ngay lúc tạo đơn vì đơn còn có thể bị hủy hoặc giao thất bại. Lịch sử điểm ghi rõ điểm đến từ giao dịch nào.

### Câu 78: Vì sao cần lưu lịch sử điểm thay vì chỉ lưu tổng điểm?

**Trả lời:** Tổng điểm chỉ cho biết số hiện tại, còn lịch sử giải thích vì sao số đó tăng hoặc giảm. Khi có hoàn tiền hay khiếu nại, cửa hàng có thể truy lại giao dịch liên quan. Điều này làm chương trình khách hàng thân thiết minh bạch hơn.

### Câu 79: Banner và coupon khác nhau thế nào?

**Trả lời:** Banner chủ yếu truyền thông tin hoặc quảng bá chương trình trên giao diện. Coupon là quyền lợi có điều kiện và ảnh hưởng trực tiếp đến số tiền đơn hàng. Một banner có thể dẫn đến coupon, nhưng việc nhìn thấy banner không đồng nghĩa chắc chắn được giảm giá.

### Câu 80: Làm sao tránh khuyến mãi làm sai tổng tiền?

**Trả lời:** Hệ thống áp dụng ưu đãi theo loại và giới hạn đã cấu hình, sau đó tự tính lại tổng tiền. Mức giảm không được làm phát sinh kết quả vô lý ngoài quy tắc. Kết quả cuối cùng được lưu cùng đơn để lịch sử không thay đổi khi chương trình được sửa sau này.

---

## 9. Hậu mãi và chăm sóc khách hàng

### Câu 81: Khi nào khách được đánh giá đơn hàng?

**Trả lời:** Khách chỉ được đánh giá đơn của mình sau khi giao thành công. Điều kiện này bảo đảm người đánh giá đã thực sự trải nghiệm dịch vụ. Mỗi khách chỉ tạo một đánh giá cho một đơn để tránh lặp nội dung.

### Câu 82: Đánh giá cần được kiểm tra những gì?

**Trả lời:** Điểm đánh giá phải nằm trong khoảng cho phép và nội dung không vượt giới hạn. Đơn phải thuộc đúng khách và đã hoàn tất. Hệ thống cũng kiểm tra đánh giá chưa tồn tại cho cặp khách và đơn đó.

### Câu 83: Vì sao đánh giá nổi bật trên trang chủ cần sự đồng ý?

**Trả lời:** Một đánh giá dùng cho quảng bá khác với đánh giá chỉ phục vụ phản hồi nội bộ. Sự đồng ý giúp tôn trọng quyền của khách khi nội dung của họ được hiển thị rộng rãi. Admin chỉ nên chọn nổi bật những đánh giá đáp ứng điều kiện này.

### Câu 84: Quy trình xử lý yêu cầu hỗ trợ diễn ra như thế nào?

**Trả lời:** Yêu cầu bắt đầu ở trạng thái mới mở, sau đó được nhân viên tiếp nhận và xử lý. Khi vấn đề đã giải quyết, nhân viên ghi kết quả rồi đóng yêu cầu. Các trạng thái giúp khách và cửa hàng biết vụ việc đang ở bước nào.

### Câu 85: Vì sao đóng yêu cầu hỗ trợ phải có kết quả xử lý?

**Trả lời:** Nếu chỉ đánh dấu đã giải quyết mà không ghi nội dung, khách và quản lý không biết vấn đề được xử lý ra sao. Kết quả là bằng chứng cho việc hoàn tất. Nó cũng giúp nhân viên khác tham khảo khi gặp trường hợp tương tự.

### Câu 86: Thông báo trong FastGuy dùng để làm gì?

**Trả lời:** Thông báo giúp người dùng biết các thay đổi quan trọng như trạng thái đơn hoặc phản hồi hỗ trợ. Người dùng có thể phân biệt thông báo đã đọc và chưa đọc. Thông báo hỗ trợ giao tiếp nhưng không thay thế trạng thái chính thức của đơn.

### Câu 87: Guest tra cứu đơn bằng cách nào?

**Trả lời:** Guest cung cấp mã đơn và thông tin xác minh phù hợp, chẳng hạn một phần số điện thoại đã đặt. Hệ thống chỉ trả dữ liệu khi các thông tin khớp. Cách này cân bằng sự tiện lợi với việc bảo vệ thông tin đơn hàng.

### Câu 88: Vì sao không chỉ dùng mã đơn để tra cứu?

**Trả lời:** Mã đơn có thể bị nhìn thấy, chia sẻ nhầm hoặc đoán được. Nếu chỉ cần mã đơn, người lạ có thể xem tên, địa chỉ hoặc tiến độ giao hàng. Thêm một yếu tố xác minh làm giảm nguy cơ lộ thông tin.

### Câu 89: Khi khách phản ánh giao thiếu món, cửa hàng xử lý thế nào?

**Trả lời:** Nhân viên xác minh đơn, danh sách món, lịch sử chuẩn bị và giao hàng. Sau đó cửa hàng chọn phương án phù hợp như giao bổ sung, hỗ trợ hoặc hoàn một phần theo chính sách. Kết quả cần được ghi lại để tránh xử lý hai lần và phục vụ đánh giá chất lượng.

### Câu 90: Vì sao cần giữ lịch sử hỗ trợ và thông báo?

**Trả lời:** Lịch sử giúp chứng minh cửa hàng đã trao đổi và xử lý vấn đề ở thời điểm nào. Khi vụ việc được chuyển cho nhân viên khác, họ vẫn hiểu bối cảnh. Dữ liệu tổng hợp còn cho biết những lỗi nào xảy ra thường xuyên để cửa hàng cải thiện.

---

## 10. Quản trị, báo cáo và xử lý ngoại lệ

### Câu 91: Dashboard quản trị giúp trả lời những câu hỏi nào?

**Trả lời:** Dashboard cho biết tình hình đơn hàng, doanh thu, kho, nhân sự và các vấn đề cần chú ý. Nó giúp quản lý nhận ra chỗ bất thường trước khi xem chi tiết. Dashboard hỗ trợ quyết định vận hành chứ không thay thế báo cáo kế toán chính thức.

### Câu 92: Vì sao doanh thu cần phân biệt tổng thu, hoàn tiền và doanh thu ròng?

**Trả lời:** Tổng thu cho biết giá trị giao dịch ban đầu, nhưng chưa phản ánh tiền đã hoàn lại. Doanh thu ròng thể hiện kết quả sau khi trừ hoàn tiền theo quy tắc báo cáo. Tách các số này giúp quản lý không hiểu nhầm hiệu quả kinh doanh.

### Câu 93: Báo cáo kho nên dùng giá trị tiền hay cộng tất cả số lượng?

**Trả lời:** Các nguyên liệu có đơn vị khác nhau như gam, mililit và cái nên không thể cộng số lượng thành một con số có ý nghĩa. Báo cáo tổng quan nên quy đổi theo giá trị tiền. Khi cần xem số lượng, quản lý xem riêng từng nguyên liệu và đơn vị của nó.

### Câu 94: Vì sao phải lưu ảnh chụp tên và giá món trong đơn?

**Trả lời:** Menu và giá có thể thay đổi sau khi khách mua. Nếu đơn chỉ tham chiếu dữ liệu hiện tại, hóa đơn cũ có thể hiển thị sai tên hoặc sai giá. Lưu thông tin tại thời điểm mua giúp lịch sử giao dịch luôn đúng.

### Câu 95: Validation nghiệp vụ khác kiểm tra dữ liệu cơ bản như thế nào?

**Trả lời:** Kiểm tra cơ bản xem dữ liệu có đúng dạng hay không, ví dụ số lượng phải là số dương. Validation nghiệp vụ xem thao tác có phù hợp tình huống hay không, ví dụ chỉ đơn READY mới được gán shipper. Cả hai đều cần thiết để hệ thống vừa nhận dữ liệu đúng vừa xử lý đúng quy trình.

### Câu 96: Khi dịch vụ bên ngoài như giao hàng hoặc thanh toán bị lỗi thì sao?

**Trả lời:** Hệ thống không nên tự tạo kết quả giả để tiếp tục. FastGuy báo trạng thái rõ ràng, giữ dữ liệu đang có và cho phép thử lại theo quy tắc an toàn. Với tiền hoặc phí giao, chỉ dùng phương án dự phòng nếu cửa hàng đã cấu hình từ trước.

### Câu 97: FastGuy xử lý thế nào khi dữ liệu người dùng đang xem đã cũ?

**Trả lời:** Trước khi cập nhật, hệ thống kiểm tra trạng thái hoặc giá trị hiện tại. Nếu một người khác đã xử lý trước, yêu cầu cũ bị từ chối và người dùng được yêu cầu tải lại. Điều này tránh ghi đè quyết định mới bằng thông tin đã lỗi thời.

### Câu 98: Vì sao các thay đổi liên quan trong một nghiệp vụ phải thành công cùng nhau?

**Trả lời:** Ví dụ khi hủy đơn, trạng thái đơn, tồn kho và coupon phải cùng được cập nhật. Nếu chỉ một phần thành công, dữ liệu sẽ mâu thuẫn và khó sửa. Vì vậy, hệ thống coi chúng là một công việc thống nhất: hoặc hoàn tất toàn bộ, hoặc giữ nguyên như trước.

### Câu 99: Khi bảo vệ, cần phân biệt tính năng đã làm và hướng phát triển như thế nào?

**Trả lời:** Chỉ nên khẳng định những gì có bằng chứng trong hệ thống, tài liệu kiểm thử hoặc mã nguồn hiện tại. Nội dung trong kế hoạch chưa hoàn tất phải được giới thiệu là định hướng, không phải chức năng đang chạy. Cách trình bày trung thực giúp hội đồng đánh giá đúng phạm vi và chất lượng dự án.

### Câu 100: Nếu được phát triển tiếp, FastGuy có thể mở rộng theo hướng nào?

**Trả lời:** Hệ thống có thể mở rộng quản lý nhiều chi nhánh, nhiều kho, lô và hạn sử dụng nguyên liệu hoặc tự động điều phối giao hàng. Các báo cáo có thể phát triển thành dự báo nhu cầu và hỗ trợ mua hàng. Tuy nhiên, mỗi mở rộng cần giữ nguyên nguyên tắc phân quyền, lịch sử rõ ràng và dữ liệu nhất quán.
