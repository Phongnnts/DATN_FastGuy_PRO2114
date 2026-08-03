# FastGuy Admin Product Editor Design

## Mục tiêu

Tách trang quản lý sản phẩm hiện tại thành catalog và editor theo route, giảm độ phức tạp của `ProductsPage.vue`, hỗ trợ deep-link create/edit và giữ nguyên toàn bộ nghiệp vụ product, media, variant, modifier, combo hiện có.

## Phạm vi

### Routes

- `/admin/products`: catalog sản phẩm.
- `/admin/products/new`: tạo sản phẩm.
- `/admin/products/:id/edit`: chỉnh sửa sản phẩm.
- Query legacy `/admin/products?edit=:id` chuyển hướng sang route edit mới.
- Link chỉnh tồn kho chuyển sang `/admin/products/:id/edit`.

### Product Catalog

Catalog chỉ chịu trách nhiệm:

- KPI tổng số, khả dụng, hết hàng và giảm giá.
- Search theo tên sản phẩm.
- Filter category/status/product type.
- Sort và pagination hiện tại.
- Điều hướng tạo/chỉnh sửa.
- Ẩn sản phẩm với wording đúng backend; không gọi hành động này là xóa vĩnh viễn.
- Loading, empty, error, retry và responsive card/table.

Catalog không chứa form editor, variant, modifier hoặc combo mutation.

### Product Editor

Một page duy nhất có section navigation:

1. **Thông tin chung**
   - Tên, category, description, base price, status.
   - Khung giờ khả dụng.
   - Validation trước submit.

2. **Hình ảnh**
   - Ảnh chính.
   - Gallery.
   - Upload Cloudinary theo cơ chế hiện có.
   - Preview và remove.

3. **Biến thể**
   - Variant name, price, original price, SKU, stock, default, status.
   - `quantityAvailable = null` tiếp tục mang nghĩa không giới hạn.
   - Add/edit/hide từng variant bằng endpoint hiện có.

4. **Tùy chọn món**
   - Modifier groups.
   - Min/max selections.
   - Modifier options và giá cộng thêm.
   - Chỉ khả dụng sau khi product đã được tạo.

5. **Combo**
   - Bật/tắt combo.
   - Chọn product/variant và quantity.
   - Chỉ khả dụng sau khi product đã được tạo.

## Luồng tạo mới

1. User mở `/admin/products/new`.
2. Editor chỉ bật Thông tin chung, Hình ảnh và draft Variants.
3. Submit tạo product trước.
4. Tạo từng draft variant bằng API hiện có.
5. Khi tất cả bước bắt buộc thành công, route dùng `replace` sang `/admin/products/:id/edit`.
6. Modifier và Combo được mở khóa.
7. Nếu product tạo thành công nhưng một variant thất bại, giữ product ID, chuyển sang edit route và hiển thị section variant cần retry; không tạo lại product.

## Luồng chỉnh sửa

1. Route ID phải là số nguyên dương.
2. Gọi `GET /api/admin/products/{id}` trực tiếp, không tải toàn catalog để tìm product.
3. Loading skeleton trong lúc fetch.
4. 404 hiển thị not-found với CTA quay lại catalog.
5. Lỗi mạng/API giữ editor ở error state với Retry.
6. Mỗi section lưu độc lập theo API hiện có; sau mutation tải lại product detail chuẩn.

## State và thay đổi chưa lưu

- Form state thuộc `ProductEditorPage`, không dùng catalog list làm source of truth.
- Mỗi section có snapshot và `dirty` riêng.
- Chuyển section không làm mất draft.
- Rời editor khi có section dirty yêu cầu xác nhận bằng dialog trong ứng dụng.
- Không dùng `window.confirm()` cho hành động mới.
- Mutation có loading riêng, chống double-submit.
- Response cũ bị bỏ qua bằng request generation khi route ID đổi/unmount.

## API Contract

Thêm frontend API method:

```js
getProduct(id) {
  return client.get(`/admin/products/${id}`);
}
```

Giữ nguyên các endpoint hiện có:

- Product CRUD.
- Variant CRUD tại `/admin/variants/:id` và `/admin/products/:id/variants`.
- Modifier group/option CRUD.
- Combo/item CRUD.

Không mở rộng backend hoặc database trong slice này.

## Component Boundary

- `ProductsPage.vue`: catalog only.
- `ProductEditorPage.vue`: orchestration, loading/error/not-found, section navigation, dirty state.
- Chỉ tách component section nếu phần được di chuyển độc lập và giảm rõ kích thước editor:
  - `ProductGeneralSection.vue`
  - `ProductMediaSection.vue`
  - `ProductVariantsSection.vue`
  - `ProductModifiersSection.vue`
  - `ProductComboSection.vue`
- Không tạo generic form framework hoặc global editor store.

## Navigation và UI

- Admin sidebar vẫn dùng `/admin/products`.
- Nested edit/new route giữ Products nav active.
- Editor header có breadcrumb, tên/draft state, Back và Save section hiện tại.
- Desktop: section navigation dọc hoặc tabs cố định; content rộng.
- Mobile: horizontal section tabs, form một cột, sticky action bar.
- Mọi page có loading, empty, error, retry.
- Inputs có label, inline error, `aria-invalid`; section navigation có semantics tab phù hợp.

## Error Handling

- Validation client-side phải khớp backend fields.
- Product create thành công nhưng variant create lỗi được xem là partial success; chuyển edit route và retry variant, không rollback giả.
- Mutation lỗi không xóa draft.
- API 401/403 dùng client behavior hiện tại.
- API 404 hiển thị not-found, không biến thành empty editor.
- Upload lỗi chỉ ảnh hưởng Media section.

## Testing

### Frontend contract tests

- Routes new/edit tồn tại và legacy query redirect đúng.
- `adminApi.getProduct(id)` gọi endpoint detail.
- Catalog không còn editor modal/variant/modifier/combo orchestration.
- Editor create chuyển sang edit route.
- Edit fetch theo route ID, xử lý invalid ID, 404 và retry.
- Modifier/Combo bị khóa trước create.
- Partial create không submit lại product.
- Dirty section chặn route leave qua application dialog.
- Request cũ không ghi state sau route change/unmount.
- Inventory edit link dùng route mới.
- Responsive/accessibility source policies.

### Verification

- `node --test tests/*.test.mjs`
- `npm run build`
- `mvn verify` để bảo đảm backend contract hiện tại không regression.
- `git diff --check`
- Manual smoke tại 375px, 768px, 1440px cho create/edit và từng section.

## Không thuộc phạm vi

- Atomic product aggregate save backend.
- Backend validation hardening.
- SKU/default variant database constraints.
- Media library/backend upload.
- Reusable modifier library.
- Bulk import/export.
- Version history/autosave.
- Inventory ledger/refund queue.
- Schema migration hoặc dependency mới.
