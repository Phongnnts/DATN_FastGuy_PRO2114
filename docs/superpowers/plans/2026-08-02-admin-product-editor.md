# FastGuy Admin Product Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tách catalog sản phẩm và editor theo route, giữ nguyên nghiệp vụ media, variants, modifiers và combo hiện có.

**Architecture:** `ProductsPage.vue` chỉ quản lý catalog. `ProductEditorPage.vue` sở hữu detail fetch, section navigation, dirty state và mutation orchestration; các section được tách thành component tập trung theo domain. API backend hiện có được giữ nguyên.

**Tech Stack:** Vue 3, Vue Router, Pinia, Axios, Vite, Node test; Java 17/Jakarta Servlet backend chỉ được verify, không sửa.

## Global Constraints

- Routes: `/admin/products`, `/admin/products/new`, `/admin/products/:id/edit`.
- Không sửa backend hoặc database.
- Không thêm dependency mới.
- Không dùng `window.confirm()` cho hành động mới.
- Product create thành công nhưng variant lỗi phải chuyển edit route; không tạo lại product.
- Modifier và Combo chỉ khả dụng sau khi product tồn tại.
- Không sửa `README.md` hoặc thay đổi ngoài Product Editor slice.
- Không commit nếu người dùng chưa yêu cầu.

---

### Task 1: Route và API Detail Contract

**Files:**
- Modify: `Frontend/src/router/index.js`
- Modify: `Frontend/src/api/admin.js`
- Modify: `Frontend/src/layouts/AdminLayout.vue`
- Create: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Produces route names `AdminProductCreate`, `AdminProductEdit`.
- Produces `adminApi.getProduct(id)` returning `/admin/products/{id}` data.

- [ ] Viết test fail cho routes new/edit, title, nested active-nav và API detail.
- [ ] Chạy `node --test tests/admin-product-editor-contract.test.mjs`; xác nhận fail.
- [ ] Thêm routes trước dynamic/admin fallback, thêm titles.
- [ ] Thêm `getProduct(id) { return client.get(`/admin/products/${id}`); }`.
- [ ] Sửa Admin nav active để `/admin/products/new` và `/:id/edit` giữ Products active.
- [ ] Chạy focused test và build.

### Task 2: Pure Editor State Utilities

**Files:**
- Create: `Frontend/src/utils/adminProductEditor.js`
- Create: `Frontend/tests/admin-product-editor-helpers.test.mjs`

**Interfaces:**
- Produces `normalizeProductDetail(raw)`.
- Produces `createProductDraft()`.
- Produces `isValidProductId(value)`.
- Produces `sectionDirty(snapshot, value)` using deterministic serialization.
- Produces `validateGeneral(form)` returning field-error object.
- Produces `validateVariant(variant)` returning field-error object.

- [ ] Viết executable tests cho null/default fields, valid/invalid ID, dirty comparison và validation.
- [ ] Chạy test; xác nhận fail module missing.
- [ ] Implement minimal pure helpers; giữ numeric/null semantics của API.
- [ ] Chạy focused test; xác nhận pass.

### Task 3: Product Catalog Extraction

**Files:**
- Modify: `Frontend/src/views/admin/ProductsPage.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Catalog điều hướng new/edit qua named routes.
- Catalog giữ list/filter/sort/pagination/KPI và hide action.

- [ ] Viết source-policy assertions: không còn modal editor, variant/modifier/combo orchestration; có route push new/edit; wording “Ẩn sản phẩm”.
- [ ] Chạy test; xác nhận fail.
- [ ] Xóa editor state/template/styles khỏi catalog, giữ list behavior.
- [ ] Đổi create/edit buttons sang route navigation.
- [ ] Đổi delete wording/action UI thành hide với accessible dialog hiện có hoặc minimal application dialog.
- [ ] Chạy focused/all tests và build.

### Task 4: Product Editor Shell

**Files:**
- Create: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Consumes Task 1 routes/API và Task 2 helpers.
- Provides section IDs: `general`, `media`, `variants`, `modifiers`, `combo`.
- Fetches edit product directly by ID.

- [ ] Viết test assertions cho invalid ID, detail fetch, loading/error/not-found/retry, section locks, request generation và unmount guard.
- [ ] Chạy test; xác nhận fail.
- [ ] Tạo shell với breadcrumb/header/back, section tabs, route-mode computed.
- [ ] Implement `loadProduct()` với generation/stopped guards.
- [ ] Invalid ID/404/error states không render empty form.
- [ ] Modifier/Combo tabs disabled ở create mode.
- [ ] Chạy focused test/build.

### Task 5: General và Media Sections

**Files:**
- Create: `Frontend/src/components/admin/product-editor/ProductGeneralSection.vue`
- Create: `Frontend/src/components/admin/product-editor/ProductMediaSection.vue`
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Sections emit `update:modelValue`, `save`, `dirty-change`.
- General consumes categories and product draft.
- Media reuses existing Cloudinary upload API/path from former `ProductsPage.vue`.

- [ ] Viết tests cho labels, inline errors, media preview/remove, mutation lock và dirty event.
- [ ] Di chuyển general/media form logic từ catalog, không nhân đôi upload code.
- [ ] Save create/update product using existing API payload.
- [ ] Create success route replace sang `AdminProductEdit`.
- [ ] Upload failure không xóa draft/media hiện có.
- [ ] Chạy focused/all tests/build.

### Task 6: Variant Section và Partial Create

**Files:**
- Create: `Frontend/src/components/admin/product-editor/ProductVariantsSection.vue`
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/src/utils/adminProductEditor.js`
- Modify: `Frontend/tests/admin-product-editor-helpers.test.mjs`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Draft create variants have no `variantId`.
- Persisted variants mutate via existing create/update/delete methods.
- Parent receives `partial-create` with created product ID and failed variant details.

- [ ] Viết executable helper tests cho variant validation/default selection.
- [ ] Viết contract assertions cho add/edit/hide, quantity null, original price/SKU/status fields.
- [ ] Di chuyển variant UI/API logic.
- [ ] Create flow: create product once; then create variants; on any variant failure route replace edit ID, retain failed draft and show retry state.
- [ ] Edit flow reloads detail after each accepted mutation.
- [ ] Chạy focused/all tests/build.

### Task 7: Modifier Section

**Files:**
- Create: `Frontend/src/components/admin/product-editor/ProductModifiersSection.vue`
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Requires persisted `productId`.
- Uses existing modifier group/option API methods.

- [ ] Viết assertions cho create-mode lock, group min/max validation, option price validation, mutation lock/refetch.
- [ ] Di chuyển modifier group/option logic từ old page.
- [ ] Fail closed khi product ID thiếu.
- [ ] Sau mutation emit `reload`; parent fetches canonical detail.
- [ ] Chạy focused/all tests/build.

### Task 8: Combo Section

**Files:**
- Create: `Frontend/src/components/admin/product-editor/ProductComboSection.vue`
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Requires persisted `productId`.
- Consumes catalog products/variants only for combo item choices.
- Uses current combo CRUD endpoints.

- [ ] Viết assertions cho create lock, enable/disable, valid product/variant/quantity, item add/remove, loading/error.
- [ ] Di chuyển combo logic.
- [ ] Load combo-choice catalog only khi section Combo mở lần đầu.
- [ ] Guard request races/unmount.
- [ ] Chạy focused/all tests/build.

### Task 9: Dirty Navigation Dialog

**Files:**
- Modify: `Frontend/src/views/admin/ProductEditorPage.vue`
- Create: `Frontend/src/components/common/ConfirmDialog.vue`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- `ConfirmDialog` props: `open`, `title`, `message`, `confirmLabel`, `busy`.
- Emits `confirm`, `cancel`.
- Editor uses `onBeforeRouteLeave` and pending navigation continuation.

- [ ] Viết tests/source assertions cho dirty route leave, cancel/confirm, focus initial/trap/Escape/restore/body unlock.
- [ ] Implement reusable accessible dialog, không `window.confirm()`.
- [ ] Track dirty theo section snapshots.
- [ ] Navigation clean đi thẳng; dirty mở dialog; confirm tiếp tục đúng target một lần.
- [ ] Unmount cleanup body/focus.
- [ ] Chạy focused/all tests/build.

### Task 10: Legacy và Inventory Navigation

**Files:**
- Modify: `Frontend/src/views/admin/ProductsPage.vue`
- Modify: `Frontend/src/views/admin/InventoryPage.vue`
- Modify: `Frontend/src/router/index.js`
- Modify: `Frontend/tests/admin-product-editor-contract.test.mjs`

**Interfaces:**
- Legacy `/admin/products?edit=42` route replace `AdminProductEdit` ID 42.
- Inventory edit link uses named edit route.

- [ ] Viết failing assertions cho legacy redirect và Inventory link.
- [ ] Implement one-time query migration without navigation loop.
- [ ] Invalid legacy edit query stays catalog and removes bad query.
- [ ] Inventory edit link routes directly.
- [ ] Chạy focused/all tests/build.

### Task 11: Full Review và Verification

**Files:**
- Review all Product Editor files.

- [ ] Review spec line-by-line: catalog/editor split, five sections, partial create, dirty guard, legacy link, responsive/accessibility.
- [ ] Run `node --test tests/*.test.mjs`; expected 0 failures.
- [ ] Run `npm run build`; expected success.
- [ ] Run `mvn verify`; expected all tests/WAR pass without backend change.
- [ ] Run `git diff --check`; expected no errors.
- [ ] Manual smoke at 375px, 768px, 1440px for create, edit, reload, API error and dirty navigation.
