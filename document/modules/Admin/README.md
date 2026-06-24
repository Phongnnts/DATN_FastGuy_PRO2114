# Module Admin

**Người phụ trách**: Người 5

## Mục tiêu

Admin quản lý product theo schema mới, gồm `Product` và `ProductVariant`, upload ảnh Cloudinary và kiểm tra dashboard.

---

## Files

### Backend

- `servlet/AdminServlet.java`
- `servlet/AdminUserServlet.java`
- `servlet/AdminProductServlet.java`
- `servlet/AdminCategoryServlet.java`
- `servlet/AdminDeliveryZoneServlet.java`
- `servlet/ProductServlet.java`
- `dao/ProductDAO.java`
- `entity/Product.java`
- `entity/ProductVariant.java`

### Frontend

- `views/admin/DashboardPage.vue`
- `views/admin/UsersPage.vue`
- `views/admin/ProductsPage.vue`
- `views/admin/CategoriesPage.vue`
- `views/admin/DeliveryZonesPage.vue`
- `api/admin.js`

---

## Product model mới

```text
Product = món cha
ProductVariant = phiên bản bán được
```

Ví dụ:

```text
Product: Pizza Hải Sản
- Variant: Size M - 89000
- Variant: Size L - 119000
```

---

## API cần hỗ trợ

| Method | Path | Mục tiêu |
|---|---|---|
| GET | `/api/admin/products` | List products |
| POST | `/api/admin/products` | Tạo product |
| PUT | `/api/admin/products/{id}` | Sửa product |
| DELETE | `/api/admin/products/{id}` | Xóa/ẩn product |
| GET | `/api/admin/products/{id}/variants` | List variants |
| POST | `/api/admin/products/{id}/variants` | Tạo variant |
| PUT | `/api/admin/variants/{id}` | Sửa variant |
| DELETE | `/api/admin/variants/{id}` | Xóa/ẩn variant |

---

## Việc cần làm

### Backend

- [ ] Sửa `Product.java`: `basePrice`, `galleryImages`.
- [ ] Tạo `ProductVariant.java`.
- [ ] Sửa `ProductDAO`:
  - `findVariantsByProductId`
  - `findVariantById`
  - `saveVariant`
  - `deleteVariant`
- [ ] Sửa `ProductServlet` trả variants.
- [ ] Sửa `AdminProductServlet` CRUD variants.
- [ ] Không dùng `ProductOption` nữa.

### Frontend

- [ ] `ProductsPage.vue`: product form dùng `basePrice`.
- [ ] `ProductsPage.vue`: thêm/sửa/xóa variants.
- [ ] `ProductsPage.vue`: set default variant.
- [ ] `ProductsPage.vue`: upload ảnh Cloudinary.
- [ ] `ProductsPage.vue`: gallery images.
- [ ] `DashboardPage.vue`: kiểm tra chart.

---

## Cloudinary

```text
Cloud name: ds4dnsj0o
Upload preset: upload-fastguy
Upload URL: https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload
```

---

## Checklist test

- [ ] Admin tạo product.
- [ ] Admin tạo variant.
- [ ] Product detail thấy variant mới.
- [ ] Admin sửa giá variant.
- [ ] Đơn cũ không đổi giá.
- [ ] Ẩn variant không làm vỡ đơn cũ.
- [ ] Upload ảnh/galleries hoạt động.

---

## Phụ thuộc

- **Database**: cần `ProductVariant`.
- **Common**: cần Cloudinary constants.
- **Guest**: dùng product response mới.
