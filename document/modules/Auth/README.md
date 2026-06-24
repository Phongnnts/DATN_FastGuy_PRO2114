# Module Auth

**Người phụ trách**: Người 1

## Mục tiêu

Đảm bảo đăng nhập, đăng ký, profile, JWT và route guard ổn định cho toàn bộ hệ thống.

---

## Files

### Backend

- `servlet/AuthServlet.java`
- `service/AuthService.java`
- `dao/UserDAO.java`
- `utils/JwtUtil.java`

### Frontend

- `views/guest/LoginPage.vue`
- `views/guest/RegisterPage.vue`
- `views/user/ProfilePage.vue`
- `views/user/ChangePasswordPage.vue`
- `stores/auth.js`
- `api/auth.js`
- `router/index.js`

---

## API Endpoints

| Method | Path | Việc cần kiểm tra |
|---|---|---|
| POST | `/api/auth/login` | Login đúng role, trả JWT đúng |
| POST | `/api/auth/register` | Tạo USER mới |
| GET | `/api/auth/profile` | Lấy profile từ token |
| PUT | `/api/auth/profile` | Cập nhật tên/sđt/avatar |
| POST | `/api/auth/logout` | Optional, frontend có thể clear token |

---

## Việc cần làm

### Backend

- [ ] Kiểm tra JWT payload có `userId`, `role`, `email`, `fullName`.
- [ ] Kiểm tra login trả đúng role: USER / STAFF / ADMIN.
- [ ] Hoàn thiện `GET /api/auth/profile`.
- [ ] Hoàn thiện `PUT /api/auth/profile`.
- [ ] Chuẩn hóa lỗi: missing token, invalid token, forbidden.

### Frontend

- [ ] Kiểm tra login/register form validation.
- [ ] Kiểm tra lưu token vào auth store.
- [ ] Kiểm tra refresh trang vẫn giữ session.
- [ ] Kiểm tra route guard:
  - Guest không vào User/Staff/Admin.
  - User không vào Staff/Admin.
  - Staff không vào Admin.
  - Admin vào dashboard đúng.

---

## Checklist test

- [ ] Login admin thành công.
- [ ] Login staff thành công.
- [ ] Login user thành công.
- [ ] Register user mới.
- [ ] Profile load đúng.
- [ ] Profile update đúng.
- [ ] Token sai/hết hạn bị logout.
- [ ] Route guard đúng role.

---

## Phụ thuộc

- Module này nên ổn định đầu tiên.
- Guest/User/Staff/Admin đều phụ thuộc Auth.
