# AGENTS.md

## Phạm vi

- Chỉ làm đúng yêu cầu; không tự thêm tính năng, refactor hoặc tối ưu.
- Chọn thay đổi nhỏ nhất đáp ứng đầy đủ yêu cầu.
- Không sửa file không liên quan. Vấn đề ngoài phạm vi chỉ báo lại.
- Hỏi khi yêu cầu mơ hồ hoặc thay đổi có rủi ro mất dữ liệu.

## Cách làm

- Đọc file liên quan và làm theo convention hiện có; không quét toàn repo nếu không cần.
- Tái sử dụng code và dependency sẵn có. Không thêm abstraction dùng một lần hoặc dependency mới khi vài dòng code đủ.
- Không hardcode secret, không log credential, luôn validate input tại trust boundary.
- Không tạo tài liệu, TODO, roadmap hoặc comment nếu không được yêu cầu.

## Kiểm tra

- Chạy kiểm tra nhỏ nhất chứng minh thay đổi đúng; chạy test/lint/build được project cung cấp trước khi hoàn thành.
- Không tuyên bố hoàn tất nếu kiểm tra thất bại. Nêu rõ lệnh lỗi và dừng.
- Sau khi đáp ứng yêu cầu, dừng; phản hồi ngắn gọn.

## Git

- Không commit hoặc push nếu người dùng chưa yêu cầu rõ.
- Không stage thay đổi không liên quan và không ghi đè thay đổi hiện có của người dùng.
