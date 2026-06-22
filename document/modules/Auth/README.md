# Module Auth

**Mục tiêu**: Đăng ký, đăng nhập, xem/sửa thông tin cá nhân.

**Người phụ trách**: Người 1

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
- `stores/auth.js`
- `api/auth.js`

---

## API Endpoints

| Method | Path | Trạng thái |
|--------|------|------------|
| POST | `/api/auth/login` | ✅ Có |
| POST | `/api/auth/register` | ✅ Có |
| POST | `/api/auth/logout` | ❌ Thêm |
| GET | `/api/auth/profile` | ❌ Thêm |
| PUT | `/api/auth/profile` | ❌ Thêm |

---

## Việc cần làm

- [ ] Backend: thêm `POST /api/auth/logout`
- [ ] Backend: thêm `GET /api/auth/profile`
- [ ] Backend: thêm `PUT /api/auth/profile`

---

## Phụ thuộc

- **Không phụ thuộc module nào**. Làm đầu tiên.
