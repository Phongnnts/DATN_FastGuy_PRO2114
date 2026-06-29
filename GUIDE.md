# Hướng dẫn làm việc với dự án FastGuy

## 1. Cài đặt Git

- Tải Git tại: https://git-scm.com/downloads/win
- Chạy file cài đặt, chọn các option mặc định, nhấn **Next** hết
- Sau khi cài, mở **PowerShell** hoặc **CMD** kiểm tra:

```powershell
git --version
```

Nếu hiển thị version là thành công.

## 2. Cấu hình Git (tên + email)

Mỗi người cần cấu hình tên và email để commit hiển thị đúng tác giả:

```powershell
git config --global user.name "Tên của bạn"
git config --global user.email "email@example.com"
```

## 3. Clone dự án về máy

```powershell
git clone https://github.com/Phongnnts/DATN_FastGuy_PRO2114.git
cd DATN_FastGuy_PRO2114
```

## 4. Xác thực với GitHub (dùng Personal Access Token)

- Vào **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
- Tạo token mới với scope **`repo`** (cấp full quyền cho repository)
- Copy token đó và giữ cẩn thận

Khi push lần đầu, Git sẽ yêu cầu đăng nhập:
- **Username**: tên GitHub của bạn
- **Password**: dán token vào (sẽ được lưu lại cho các lần sau)

> ⚠️ **Không chia sẻ token cho người khác.**

## 5. Được chủ repo mời vào dự án

- Chủ repo (Phongnnts) vào **Settings → Collaborators** và thêm GitHub username của bạn
- Kiểm tra email hoặc vào **GitHub → Notifications** để chấp nhận lời mời
- Chỉ sau khi được mời, bạn mới có quyền push code

## 6. Chọn nhánh được phân công

Xem danh sách nhánh trên remote:

```powershell
git branch -a
```

Chuyển sang nhánh của bạn (ví dụ `module/auth`):

```powershell
git checkout module/auth
```

> Nếu chưa có branch ở local:
> ```powershell
> git checkout -b module/auth origin/module/auth
> ```

Phân công nhánh:

| Người | Module | Nhánh |
|-------|--------|-------|
| 1 | Auth | `module/auth` |
| 2 | Guest | `module/guest` |
| 3 | User | `module/user` |
| 4 | Staff | `module/staff` |
| 5 | Admin | `module/admin` |
| 6 | Common | `module/common` |

## 7. Quy tắc branch (QUAN TRỌNG)

- ❌ **KHÔNG được push thẳng lên nhánh `main` hoặc `develop`**
- ✅ **Chỉ push code lên nhánh được phân công** (vd `module/auth`, `module/guest`, ...)
- Khi hoàn thành tính năng, tạo **Pull Request** trên GitHub để merge vào `main`
- Không tự ý merge, cần ít nhất 1 người review trước khi merge

## 8. Quy trình làm việc hàng ngày

```powershell
# Bước 1: Kéo code mới nhất từ nhánh của mình
git pull origin module/auth

# Bước 2: Làm việc (sửa code, thêm file...)

# Bước 3: Thêm file đã thay đổi vào stage
git add .

# Bước 4: Commit với message mô tả ngắn gọn
git commit -m "Mô tả ngắn gọn nội dung đã làm"

# Bước 5: Đẩy lên remote
git push origin module/auth

# Bước 6: Lên GitHub tạo Pull Request (nếu đã xong tính năng)
```

## 9. Lưu ý

- Luôn **pull** code mới nhất trước khi bắt đầu làm việc để tránh conflict
- Commit message nên viết bằng tiếng Việt không dấu hoặc tiếng Anh, rõ ràng
- Nếu có conflict, báo leader để được hỗ trợ
- Không commit file `.env`, token, mật khẩu lên GitHub
