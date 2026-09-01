# AGENTS.md

## Phạm vi

- Chỉ làm đúng yêu cầu; không tự thêm tính năng, refactor hoặc tối ưu.
- Chọn thay đổi nhỏ nhất đáp ứng đầy đủ yêu cầu.
- Không sửa file không liên quan. Vấn đề ngoài phạm vi chỉ báo lại.
- Hỏi khi yêu cầu mơ hồ hoặc thay đổi có rủi ro mất dữ liệu.

## Cách làm

- Đọc file liên quan và làm theo convention hiện có; không quét toàn repo nếu không cần.
- Tái sử dụng code và dependency sẵn có. Không thêm abstraction dùng một lần hoặc dependency mới khi vài dòng code đủ.
- Tailwind CSS v4 được phép cho frontend mới và khi nâng cấp UI. Dùng prefix `tw:` và migrate tăng dần; không bắt buộc viết lại CSS hiện có.
- Không hardcode secret, không log credential, luôn validate input tại trust boundary.
- Không tạo tài liệu, TODO, roadmap hoặc comment nếu không được yêu cầu.

## Luồng bắt buộc

Mọi thay đổi đi theo thứ tự `DATABASE → API → FRONTEND`.

Trước khi sửa:

1. Lập plan ngắn gồm phạm vi, source of truth, dependency bị ảnh hưởng và kiểm tra phải chạy.
2. Dùng CodeGraph truy luồng Java `Servlet → Service → DAO → Entity/DTO` và frontend consumer liên quan.
3. Không đoán schema database.
4. Không đoán request hoặc response API.

## Database

- Khi backend phụ thuộc dữ liệu, kiểm tra SQL Server catalog và xác nhận chính xác server/database trước khi sửa.
- Đối chiếu runtime schema với `database/init.sql`, `database/DB_FastGuy.sql`, migration liên quan và JPA mapping.
- Nếu chưa xác định đúng server/database hoặc không kết nối được, dừng ở source analysis và báo rõ; không tự suy luận schema.
- SQL Server MCP chỉ được ghi dữ liệu hoặc chạy DDL khi `DB_NAME() = 'DemoDatabase'` và người dùng đã phê duyệt thao tác; mọi database khác, gồm `FastGuyDB`, chỉ read-only. Không chạy stored procedure qua MCP.
- Migration phải tuân thủ `.opencode/skills/database-safety/SKILL.md` và `database/migrations/RUNBOOK.md`; cần xác nhận riêng trước khi thực thi.

## API Contract

- OpenAPI 3.1 là nguồn chuẩn cho endpoint đã được contract hóa.
- Khi đổi endpoint legacy chưa có OpenAPI, thêm contract nhỏ nhất trước khi sửa implementation.
- Không sửa Vue API client dựa trên response đoán, tài liệu cũ hoặc ảnh DevTools.
- Thay đổi DTO/API phải kiểm tra servlet serialization, contract test và frontend consumer.
- Không dùng field không có trong OpenAPI cho endpoint đã contract hóa.

## Blast Radius

- Thay đổi DB: kiểm tra migration, Entity, DAO, Service, Servlet/API và frontend consumer.
- Thay đổi Entity/DAO/Service/API: kiểm tra caller và consumer bằng CodeGraph.
- Thay đổi DTO/API: kiểm tra OpenAPI, backend contract test, frontend API client/store/view.
- Thay đổi Vue API client: kiểm tra OpenAPI operation/schema trước.

## Kiểm tra

- Chạy kiểm tra nhỏ nhất chứng minh thay đổi đúng; chạy test/lint/build được project cung cấp trước khi hoàn thành.
- Backend Java: chạy test liên quan và `mvn test`.
- Frontend Vue: chạy test liên quan, `npm test` và `npm run build`.
- Thay đổi DB/API: chạy integration test trên disposable/local test environment.
- Feature UI quan trọng: chạy Playwright desktop và mobile; xác nhận không có console error và request chính thành công.
- CSS/text nhỏ không chạm data flow không bắt chạy DB integration test.
- Không tuyên bố hoàn tất nếu kiểm tra thất bại. Nêu rõ lệnh lỗi và dừng.
- Sau khi đáp ứng yêu cầu, dừng; phản hồi ngắn gọn.

## Git

- Không commit hoặc push nếu người dùng chưa yêu cầu rõ.
- Không stage thay đổi không liên quan và không ghi đè thay đổi hiện có của người dùng.
