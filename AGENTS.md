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
- Không chạy lệnh destructive, DDL/DML, migration, reset hoặc xóa dữ liệu nếu chưa được người dùng phê duyệt rõ.
- Không tạo tài liệu, TODO, roadmap hoặc comment nếu không được yêu cầu.

## Luồng bắt buộc

Mọi thay đổi đi theo thứ tự `DATABASE → API → FRONTEND`.

Trước khi sửa:

1. Lập plan ngắn gồm phạm vi, source of truth, dependency bị ảnh hưởng và kiểm tra phải chạy.
2. Dùng CodeGraph trước khi sửa code đã được index; ưu tiên cho data flow, caller/consumer và blast radius. Không dùng CodeGraph để đọc config hoặc tài liệu.
3. Khi liên quan Java/data flow, truy luồng `Servlet → Service → DAO → Entity/DTO` và frontend consumer liên quan.
4. Không đoán schema database.
5. Không đoán request hoặc response API.

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

## Thiết kế UI/UX

- Khi tạo mới hoặc thay đổi đáng kể giao diện, xác định người dùng chính, tác vụ chính, thứ tự ưu tiên thông tin và các trạng thái cần hỗ trợ trước khi sửa.
- Ưu tiên rõ ràng, dễ thao tác và nhất quán trước hiệu ứng trang trí.
- Kế thừa design language, component và design token hiện có. Nếu giao diện thiếu nhất quán, chỉ chuẩn hóa trong phạm vi màn hình đang sửa.
- Dùng spacing, typography, màu sắc, border, radius và shadow theo scale nhất quán; không hardcode khi đã có token tương ứng.
- Duy trì hierarchy rõ ràng; mỗi màn hình chỉ có một primary action nổi bật.
- Không lạm dụng gradient, glassmorphism, shadow đậm, bo góc lớn, card lồng card hoặc animation.
- Form phải có label rõ ràng, validation gần trường nhập và thông báo lỗi hướng dẫn cách sửa.
- Xử lý loading, error, empty, disabled, success và partial-data state khi liên quan.
- Responsive theo mobile-first; không để tràn ngang hoặc che khuất hành động chính.
- Dùng semantic HTML trước ARIA; hỗ trợ keyboard, focus visible và screen reader.
- Không truyền đạt trạng thái chỉ bằng màu; đảm bảo contrast WCAG 2.2 AA.
- Vùng bấm tối thiểu 40x40px, ưu tiên 44x44px; icon-only button phải có accessible label.
- Animation phải ngắn, có mục đích, tôn trọng `prefers-reduced-motion`; không dùng `transition: all`.
- Với thay đổi UI, dùng `vue-frontend` và `frontend-design`; thêm `tailwind-design-system` khi chuẩn hóa design system.
- Khi chỉ sửa CSS/text nhỏ, không tự redesign component hoặc màn hình lân cận.

## Kiểm tra

- Chạy kiểm tra nhỏ nhất chứng minh thay đổi đúng; chạy test/lint/build được project cung cấp trước khi hoàn thành.
- Nếu project đã định nghĩa lint/typecheck, chạy targeted command liên quan; trước khi hoàn tất chạy command bắt buộc được ghi trong project.
- Backend Java: chạy test liên quan và `mvn test`.
- Frontend Vue: chạy test liên quan, `npm test` và `npm run build`.
- Thay đổi DB/API: chạy integration test trên disposable/local test environment.
- Feature UI quan trọng: chạy Playwright Chromium desktop; xác nhận không có console error và request chính thành công.
- CSS/text nhỏ không chạm data flow không bắt chạy DB integration test.
- Không tuyên bố hoàn tất nếu kiểm tra thất bại. Nêu rõ lệnh lỗi và dừng.
- Sau khi đáp ứng yêu cầu, dừng; phản hồi ngắn gọn.

## Git

- Trước khi sửa, kiểm tra working tree; không revert, format hoặc ghi đè thay đổi chưa commit của người dùng.
- Không commit hoặc push nếu người dùng chưa yêu cầu rõ.
- Không stage thay đổi không liên quan và không ghi đè thay đổi hiện có của người dùng.
