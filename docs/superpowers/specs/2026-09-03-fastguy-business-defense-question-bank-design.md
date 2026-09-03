# Thiết kế bộ 100 câu hỏi vấn đáp nghiệp vụ FastGuy

## Mục tiêu

Tạo một tài liệu ôn thi bảo vệ dự án FastGuy bằng tiếng Việt, ưu tiên kiến thức nghiệp vụ, luồng xử lý và logic hệ thống. Câu trả lời phải dễ hiểu, có thể trình bày bằng lời trước hội đồng và không sa vào thuật ngữ kỹ thuật.

## Đầu ra

- File chính: `docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md`.
- Đúng 100 câu hỏi có đánh số liên tục.
- Chia thành 10 nhóm, mỗi nhóm 10 câu.
- Mỗi câu trả lời dài khoảng 3–6 câu, đủ giải thích nhưng dễ học.

## Cấu trúc nội dung

1. Tổng quan bài toán và phạm vi FastGuy.
2. Vai trò và phân quyền Guest, User, Staff, Shipper, Admin.
3. Menu, giỏ hàng và đặt hàng.
4. Thanh toán, hủy đơn và hoàn tiền.
5. Tiếp nhận đơn và chế biến tại cửa hàng.
6. Phân công và giao hàng.
7. Kho nguyên liệu, công thức, nhập hàng và kiểm kê.
8. Coupon, điểm thưởng và chương trình tiếp thị.
9. Đánh giá, hỗ trợ và thông báo sau bán.
10. Quản trị, báo cáo, validation và xử lý ngoại lệ.

## Cách viết câu trả lời

- Giải thích theo ngôn ngữ đời thường và tình huống thực tế.
- Nêu rõ ai thực hiện, điều kiện được thực hiện, hệ thống xử lý thế nào và vì sao có quy tắc đó.
- Ưu tiên các câu hỏi dạng “vì sao”, “khi nào”, “nếu xảy ra thì sao” thay vì hỏi tên công nghệ.
- Khi cần nhắc đến trạng thái đơn, dùng tên trạng thái kèm nghĩa tiếng Việt.
- Không mô tả thiết kế hoặc kế hoạch chưa triển khai như tính năng chắc chắn đã có.
- Không đưa số liệu kiểm thử hoặc trạng thái triển khai dễ lỗi thời vào câu trả lời nếu không cần thiết.

## Nguồn nội dung

Ưu tiên đối chiếu:

- `docs/usecase.md`.
- `docs/erd.md`.
- `docs/unit-test-report.md` và `docs/system-test-report.md`.
- Các đặc tả nghiệp vụ trong `docs/superpowers/specs/`.
- Các kế hoạch liên quan trong `docs/superpowers/plans/` chỉ dùng để bổ sung bối cảnh và phải phân biệt với chức năng đã hoàn tất.
- `luong-nghiep-vu-fastguy.md`, `on-tap-van-dap-fastguy.md` và `100-cau-hoi-bao-ve-fastguy.md` dùng để tránh bỏ sót hoặc lặp lại cách diễn đạt cũ.

## Tiêu chí hoàn thành

- Có đúng 100 câu và 100 câu trả lời.
- Bao phủ luồng chuẩn và ngoại lệ quan trọng của đơn hàng, thanh toán, giao hàng, kho và hậu mãi.
- Không có câu trả lời mâu thuẫn về vai trò, trạng thái hoặc điều kiện nghiệp vụ.
- Người không chuyên kỹ thuật vẫn có thể đọc hiểu và dùng để trả lời vấn đáp.
- Không chỉnh sửa các tài liệu hiện có.
