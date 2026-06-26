# Module Auth

**Nhánh git:** `module/auth`
**Người phụ trách:** Người 1

---

## Trạng thái

**Backend đã xong.** Chỉ cần kiểm tra backend + sửa frontend.

---

## Files cần sửa

| File | Việc |
|---|---|
| `frontend/src/stores/auth.js` | Kiểm tra JWT decode, lưu session, role getter |
| `frontend/src/router/index.js` | Route guard theo role |
| `frontend/src/views/guest/LoginPage.vue` | Form login, gọi API |
| `frontend/src/views/guest/RegisterPage.vue` | Form register |
| `frontend/src/views/user/ProfilePage.vue` | Hiển thị + sửa thông tin |
| `frontend/src/views/user/ChangePasswordPage.vue` | Đổi mật khẩu |

---

## API hiện tại

### POST /api/auth/login

```json
Request: { "email": "admin@fastguy.com", "password": "123456" }
Response: {
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "role": "ADMIN",
    "email": "admin@fastguy.com",
    "fullName": "Admin FastGuy"
  }
}
```

### POST /api/auth/register

```json
Request: { "email": "...", "password": "...", "phone": "...", "fullName": "..." }
Response: { "success": true, "data": { "userId": 9 } }
```

### GET /api/auth/profile

```json
Response: { "success": true, "data": { "userId": 1, "email": "...", "fullName": "...", "phone": "...", "avatarUrl": "", "roleName": "ADMIN" } }
```

### PUT /api/auth/profile

```json
Request: { "fullName": "...", "phone": "...", "avatarUrl": "..." }
Response: { "success": true, "message": "Profile updated" }
```

---

## AI Prompt — stores/auth.js

```
Kiểm tra và sửa file `frontend/src/stores/auth.js`.

Yêu cầu:
1. Khi login thành công, lưu token vào localStorage với key 'token'
2. decode JWT bằng tay (base64 parse payload) để lấy userId, role, email, fullName
3. Khi khởi tạo store (module load), kiểm tra localStorage có token không → decode → set user
4. Token hết hạn (exp) thì tự động logout
5. Các getter:
   - isLoggedIn: !!token.value
   - role: user.value?.role || 'GUEST'
   - isUser: role === 'USER'
   - isStaff: role === 'STAFF'
   - isAdmin: role === 'ADMIN'
6. Hàm logout(): xóa token, xóa user, redirect về /login
7. Hàm updateProfile(data): gọi PUT /api/auth/profile, cập nhật user

import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { ROLES } from '@/utils/constants'
```

---

## AI Prompt — router/index.js

```
Kiểm tra file `frontend/src/router/index.js`.

Route guard yêu cầu:
1. GuestLayout (path: /): chỉ cho GUEST (chưa login)
   - LoginPage, RegisterPage, ForgotPasswordPage, HomePage, MenuPage, CartPage, TrackOrderPage
   - Nếu đã login → redirect về /user/home hoặc /staff/dashboard hoặc /admin tùy role
2. UserLayout (path: /user): chỉ cho USER
   - Nếu chưa login → redirect /login
   - Nếu role khác USER → redirect về home phù hợp
3. StaffLayout (path: /staff): chỉ cho STAFF
   - Nếu chưa login → redirect /login
   - Nếu role khác STAFF → redirect
4. AdminLayout (path: /admin): chỉ cho ADMIN

Sử dụng `router.beforeEach` với `useAuthStore()`.
```

---

## AI Prompt — LoginPage.vue

```
Sửa `frontend/src/views/guest/LoginPage.vue`.

Yêu cầu:
1. Form nhập email, password
2. Gọi `authStore.login(email, password)`
3. Nếu thành công, redirect theo role:
   - ADMIN → /admin
   - STAFF → /staff/dashboard
   - USER → /
4. Nếu thất bại, hiển thị lỗi
5. Có link đến RegisterPage
```

---

## Checklist test

- [ ] Login admin → vào /admin
- [ ] Login staff → vào /staff/dashboard
- [ ] Login user → vào /
- [ ] Register user mới → login được
- [ ] Profile hiển thị đúng thông tin
- [ ] Profile update được
- [ ] Token sai/hết hạn → tự logout
- [ ] Route guard: guest không vào user/staff/admin
