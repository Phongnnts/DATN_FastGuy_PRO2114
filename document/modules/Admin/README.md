# Module Admin

**Mục tiêu**: Dashboard Chart.js đơn giản, CRUD đầy đủ (Users, Products, Categories, DeliveryZones), upload ảnh Cloudinary.

**Bỏ qua**: Shifts, Schedules, Ingredients, Reports.

**Người phụ trách**: Người 5

---

## Files

### Backend
- `servlet/AdminServlet.java`
- `servlet/AdminUserServlet.java`
- `servlet/AdminProductServlet.java`
- `servlet/AdminCategoryServlet.java`
- `servlet/AdminDeliveryZoneServlet.java`
- `service/AdminService.java`

### Frontend
- `views/admin/DashboardPage.vue` (Chart.js bar)
- `views/admin/UsersPage.vue`
- `views/admin/ProductsPage.vue` (upload Cloudinary)
- `views/admin/CategoriesPage.vue`
- `views/admin/DeliveryZonesPage.vue`
- `api/admin.js`

---

## API Endpoints

| Method | Path | Trạng thái |
|--------|------|------------|
| GET | `/api/admin/dashboard` | ✅ Có |
| GET/POST/PUT/DELETE | `/api/admin/users/*` | ✅ Có |
| GET/POST/PUT/DELETE | `/api/admin/products/*` | ✅ Có |
| GET/POST/PUT/DELETE | `/api/admin/categories/*` | ✅ Có |
| GET/POST/PUT/DELETE | `/api/admin/delivery-zones/*` | ✅ Có |

---

## Cloudinary Upload

**Cấu hình**:
```
Cloud name:     ds4dnsj0o
Upload preset:  upload-fastguy
API URL:        https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload
```

**Thư mục**: `Image_Cloudinery/Burger/`, `BanhMi/`, `Com/`, `GaRan/`, `GoiTom/`, `KhoaiTay/`, `Nuoc/`, `Pizza/`, `Tacos/`

**Luồng**: Admin chọn danh mục → tự động chọn thư mục → upload ảnh → lưu URL → preview.

---

## Việc cần làm

- [ ] Backend: sửa `Product.java` thêm field `galleryImages`
- [ ] Backend: sửa `ProductDAO` lưu/load galleryImages
- [ ] Frontend: `ProductsPage` — thêm upload Cloudinary widget
- [ ] Frontend: `ProductsPage` — dropdown chọn thư mục ảnh
- [ ] Frontend: `ProductsPage` — preview ảnh + gallery
- [ ] Frontend: kiểm tra `DashboardPage` chart hiển thị

---

## Phụ thuộc

- **Auth**: Cần đăng nhập
- **Common**: Layout, constants, Cloudinary config
