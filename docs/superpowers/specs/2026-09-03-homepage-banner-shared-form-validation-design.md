# FastGuy Homepage Banner and Shared Form Validation Design

## Objective

- Mở rộng banner trang chủ gần toàn bộ chiều rộng viewport theo hình tham chiếu, giữ nguyên dữ liệu banner, carousel và nội dung hiện tại.
- Bổ sung placeholder, dấu bắt buộc, validation khi blur và submit, lỗi cục bộ dưới field cho đúng bốn nhóm: đăng nhập, đăng ký, quên mật khẩu, thông tin nhận hàng/địa chỉ giao hàng.
- Dùng chung component và style validation để bốn nhóm nhất quán.

## Non-goals

- Không thêm hoặc khôi phục chức năng hỗ trợ khách hàng.
- Không thay đổi database, backend, API hoặc OpenAPI.
- Không áp dụng validation mới cho form Admin, Staff, Shipper hoặc các form khác.
- Không đổi bố cục, màu chủ đạo, kích thước tổng thể hay luồng submit hiện tại ngoài banner và trạng thái field được yêu cầu.
- Không thêm dependency.

## Homepage banner

`HomePage.vue` tiếp tục dùng dữ liệu banner hiện tại, fallback slides, carousel controls, reduced-motion behavior và CTA. Chỉ thay đổi khung hero để:

- rộng gần sát hai mép viewport như hình tham chiếu;
- không bị giới hạn bởi `.container` chung ở cấp ngoài;
- nội dung bên trong vẫn có khoảng an toàn và giới hạn đọc phù hợp;
- ảnh nền dùng `cover`, giữ vùng phủ tối để chữ dễ đọc;
- không gây tràn ngang;
- trên mobile giữ nội dung, CTA và điều khiển trong viewport.

## Shared validation architecture

### Shared field component

Tạo một component field dùng chung cho input và select. Component chịu trách nhiệm:

- render label và dấu `*` màu đỏ khi `required`;
- liên kết label với control bằng `for`/`id`;
- truyền placeholder và các thuộc tính input/select cần thiết;
- áp dụng class lỗi, `aria-invalid` và `aria-describedby`;
- render thông báo lỗi ngay dưới control với `role="alert"`;
- giữ một vùng field theo document flow để lỗi không chồng lên field bên cạnh.

Select hỗ trợ option mặc định do consumer cung cấp; component không tự tạo dữ liệu quận/phường.

### Shared validation utility

Tạo utility thuần, không dependency, gồm:

- kiểm tra chuỗi bắt buộc sau khi trim;
- kiểm tra email;
- kiểm tra số điện thoại theo quy tắc hiện tại của dự án/backend;
- kiểm tra mật khẩu theo quy tắc đăng ký hiện tại;
- kiểm tra xác nhận mật khẩu khớp mật khẩu.

Mỗi trang sở hữu state form, touched state và errors. Quy tắc tương tác:

1. Blur field: đánh dấu touched và validate field đó.
2. Submit: đánh dấu toàn bộ field bắt buộc là touched, validate toàn form; không gọi API khi có lỗi.
3. Input/change sau khi field đã touched hoặc có lỗi: validate lại; khi hợp lệ, xóa lỗi và border trở lại bình thường.
4. Lỗi API/form tổng quát hiện có vẫn hiển thị riêng, không bị biến thành lỗi field nếu backend không cung cấp mapping chính xác.

## Field requirements

### Login

- Email: placeholder `your@email.com`; bắt buộc; lỗi trống và sai định dạng dưới field.
- Mật khẩu: placeholder `Nhập mật khẩu`; bắt buộc; lỗi trống dưới field.

### Registration

- Họ và tên: `Nhập họ và tên`; bắt buộc.
- Email: `your@email.com`; bắt buộc, đúng định dạng.
- Số điện thoại: `Nhập số điện thoại`; bắt buộc, đúng định dạng.
- Mật khẩu: `Nhập mật khẩu`; bắt buộc, theo policy hiện có.
- Xác nhận mật khẩu: `Nhập lại mật khẩu`; bắt buộc, phải khớp.

Thông báo lỗi cụ thể kế thừa wording hiện có nếu đã rõ; nếu chưa có, dùng câu ngắn theo mẫu `Vui lòng nhập ...` và `... không hợp lệ`.

### Forgot password

- Email: placeholder `your@email.com`.
- Trống: `Vui lòng nhập email`.
- Sai định dạng: `Email không hợp lệ`.

### Shipping/address

Áp dụng cho phần nhập địa chỉ mới trong checkout và màn hình quản lý địa chỉ tương ứng nếu cùng biểu mẫu nhận hàng:

- Tên người nhận: `Họ tên người nhận`; lỗi trống `Vui lòng nhập tên người nhận`.
- Quận/Huyện: option mặc định `Chọn quận/huyện`; lỗi `Vui lòng chọn quận/huyện`.
- Phường/Xã: option mặc định `Chọn phường/xã`; lỗi `Vui lòng chọn phường/xã`.
- Số nhà, tên đường: `VD: 123 Nguyễn Huệ`; lỗi `Vui lòng nhập địa chỉ cụ thể`.
- Số điện thoại: `Số điện thoại nhận hàng`; lỗi trống `Vui lòng nhập số điện thoại`; lỗi định dạng `Số điện thoại không hợp lệ`.

Luồng phụ thuộc quận/phường, tải địa chỉ đã lưu, giới hạn giao hàng TP.HCM, phí giao hàng và checkout payload giữ nguyên.

## Visual and accessibility rules

- Dấu bắt buộc: đỏ, đứng ngay sau label, không chỉ dùng màu để báo lỗi vì lỗi còn có text.
- Border lỗi: `#ef4444` hoặc token tương đương chính xác nếu dự án đã có.
- Text lỗi: `#ef4444`, 12–13px, margin-top 4px.
- Placeholder: xám nhạt, nhất quán với token hiện tại.
- Focus visible hiện có được giữ; trạng thái focus lỗi vẫn nhận biết rõ.
- Error id duy nhất cho từng field; screen reader nhận lỗi qua `aria-describedby`.
- Không dùng absolute positioning cho lỗi.

## Error handling

- Client validation chặn request không hợp lệ.
- Server/API errors vẫn hiển thị bằng vùng lỗi tổng quát hiện tại.
- Loading guard, disabled submit và điều hướng thành công hiện tại không đổi.
- Việc chọn lại quận phải reset phường và lỗi phụ thuộc được tính lại.

## Testing

- Unit/source tests cho shared validators và component contract.
- Regression tests cho placeholder, required marker, blur validation, submit validation, live error clearing và API không được gọi khi invalid.
- Tests riêng cho exact forgot-password messages và shipping required/phone messages.
- Existing frontend suite: `npm test`.
- Production build: `npm run build`.
- Playwright Chromium desktop cho login/register/forgot-password, checkout/address validation và banner width/overflow; xác nhận không console/page errors và không request submit khi form invalid.

## Source of truth

- Các Vue form hiện tại và validation/backend policy hiện có.
- API payload và endpoint hiện tại; không đổi contract.
- Hình banner do người dùng cung cấp cho mục tiêu chiều rộng và tỷ lệ thị giác.
