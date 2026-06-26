# Module Admin

**Nhánh git:** `module/admin`
**Người phụ trách:** Người 5

---

## Trạng thái

**Backend đã xong CRUD Product + ProductVariant.** Cần sửa frontend.

---

## API

### Product

| Method | Path | Body/Params |
|---|---|---|
| GET | `/api/admin/products` | List all |
| POST | `/api/admin/products` | `{ categoryId, name, description, basePrice, imageUrl, galleryImages, status }` |
| PUT | `/api/admin/products/{id}` | `{ name, basePrice, ... }` |
| DELETE | `/api/admin/products/{id}` | |

### Variant

| Method | Path | Body/Params |
|---|---|---|
| GET | `/api/admin/products/{id}/variants` | List variants của product |
| POST | `/api/admin/products/{id}/variants` | `{ variantName, price, isDefault }` |
| PUT | `/api/admin/variants/{id}` | `{ variantName, price, isDefault, status }` |
| DELETE | `/api/admin/variants/{id}` | |

### Response product

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "categoryId": 1,
  "categoryName": "Burger",
  "basePrice": 45000,
  "imageUrl": "...",
  "description": "...",
  "status": "AVAILABLE",
  "galleryImages": ["https://..."],
  "variants": [
    { "variantId": 1, "variantName": "Mặc định", "price": 45000, "isDefault": true, "status": "AVAILABLE" },
    { "variantId": 2, "variantName": "Size L", "price": 55000, "isDefault": false, "status": "AVAILABLE" }
  ]
}
```

### Response variant

```json
{ "variantId": 2, "variantName": "Size L", "price": 55000, "originalPrice": null, "sku": null, "quantityAvailable": null, "isDefault": false, "status": "AVAILABLE" }
```

---

## File cần sửa

| File | Việc |
|---|---|
| `frontend/src/stores/admin.js` | Thêm variant CRUD methods, bỏ nốt option cũ |
| `frontend/src/api/admin.js` | Thêm variant endpoints |
| `frontend/src/views/admin/ProductsPage.vue` | Form dùng basePrice, quản lý variants |
| `frontend/src/views/admin/DashboardPage.vue` | Kiểm tra chart |

---

## AI Prompt 1 — stores/admin.js

```
Sửa `frontend/src/stores/admin.js`.

Thêm các method cho ProductVariant:
```
async function fetchVariants(productId) {
  loading.value = true;
  try {
    const data = await adminApi.getVariants(productId);
    return Array.isArray(data) ? data : [];
  } catch { return []; }
  finally { loading.value = false; }
}

async function createVariant(productId, data) {
  try {
    const res = await adminApi.createVariant(productId, data);
    return res;
  } catch { return null; }
}

async function updateVariant(id, data) {
  try {
    await adminApi.updateVariant(id, data);
  } catch {}
}

async function deleteVariant(id) {
  try {
    await adminApi.deleteVariant(id);
  } catch {}
}
```
Thêm vào return: `fetchVariants, createVariant, updateVariant, deleteVariant`
```

---

## AI Prompt 2 — api/admin.js

```
Sửa `frontend/src/api/admin.js`.

Thêm:
```
getVariants(productId) { return client.get(`/admin/products/${productId}/variants`); },
createVariant(productId, data) { return client.post(`/admin/products/${productId}/variants`, data); },
updateVariant(id, data) { return client.put(`/admin/variants/${id}`, data); },
deleteVariant(id) { return client.delete(`/admin/variants/${id}`); },
```

Đã bỏ ingredient methods từ trước.
```

---

## AI Prompt 3 — ProductsPage.vue

```
Sửa `frontend/src/views/admin/ProductsPage.vue`.

Mô hình mới:
- Product = món cha, có `basePrice` (giá hiển thị)
- ProductVariant = phiên bản khách mua, có `price` (giá thật)
- Mỗi product có 1 variant mặc định + nhiều variant phụ

Yêu cầu:

### Form thêm/sửa Product
- Các field: categoryId, name, description, basePrice, imageUrl, status
- basePrice là giá hiển thị "từ xx.xxxđ"
- Upload ảnh Cloudinary (giữ nguyên)
- Gallery images

### Quản lý Variants
- Khi xem/sửa product, hiển thị danh sách variants
- Mỗi variant hiển thị: variantName, price, isDefault, status
- Nút "Thêm variant": mở form nhập variantName, price, checkbox isDefault
- Nút "Sửa": mở form sửa variantName, price, isDefault
- Nút "Xóa": confirm rồi gọi deleteVariant
- Khi set isDefault = true cho variant nào, các variant khác tự thành false

### API gọi
- Load product: `adminStore.fetchProducts()` hoặc `adminApi.getProduct(id)`
- Load variants: `adminStore.fetchVariants(productId)`
- Tạo variant: `adminStore.createVariant(productId, { variantName, price, isDefault })`
- Sửa variant: `adminStore.updateVariant(variantId, { variantName, price, isDefault })`
- Xóa variant: `adminStore.deleteVariant(variantId)`
```

---

## Cloudinary Config

```js
CLOUDINARY: {
  cloudName: 'ds4dnsj0o',
  uploadPreset: 'upload-fastguy',
  uploadUrl: 'https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload',
}
```

---

## Checklist test

- [ ] Tạo product với basePrice
- [ ] Tạo variant (Size M, Size L...)
- [ ] Set variant mặc định
- [ ] Product detail trả variants
- [ ] Sửa giá variant
- [ ] Xóa variant
- [ ] Đơn cũ không đổi giá (OrderItem snapshot)
- [ ] Upload ảnh Cloudinary
