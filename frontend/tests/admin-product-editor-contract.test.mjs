import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const adminApi = read('../src/api/admin.js');
const adminLayout = read('../src/layouts/AdminLayout.vue');
const productsPage = read('../src/views/admin/ProductsPage.vue');
const inventoryPage = read('../src/views/admin/InventoryPage.vue');
const confirmDialog = read('../src/components/common/ConfirmDialog.vue');
const productEditorPage = read('../src/views/admin/ProductEditorPage.vue');
const productGeneralSection = read('../src/components/admin/product-editor/ProductGeneralSection.vue');
const productMediaSection = read('../src/components/admin/product-editor/ProductMediaSection.vue');
const productVariantsSection = read('../src/components/admin/product-editor/ProductVariantsSection.vue');
const productModifiersSection = read('../src/components/admin/product-editor/ProductModifiersSection.vue');

test('defines product create and edit routes before product catalog route', () => {
  const create = router.indexOf("name: 'AdminProductCreate'");
  const edit = router.indexOf("name: 'AdminProductEdit'");
  const catalog = router.indexOf("name: 'AdminProducts'");
  assert.ok(create >= 0);
  assert.ok(edit >= 0);
  assert.ok(create < catalog);
  assert.ok(edit < catalog);
  assert.match(router, /path: 'products\/new'[\s\S]*name: 'AdminProductCreate'/);
  assert.match(router, /path: 'products\/:id\/edit'[\s\S]*name: 'AdminProductEdit'/);
});

test('defines product editor page titles', () => {
  assert.match(router, /AdminProductCreate: 'Thêm sản phẩm'/);
  assert.match(router, /AdminProductEdit: 'Chỉnh sửa sản phẩm'/);
});

test('admin API exposes product detail endpoint', () => {
  assert.match(adminApi, /getProduct\(id\)\s*\{\s*return client\.get\(`\/admin\/products\/\$\{id\}`\);\s*\}/);
});

test('products navigation remains active for nested editor routes', () => {
  assert.match(adminLayout, /isLinkActive\(link\)/);
  assert.match(adminLayout, /link\.path === '\/admin\/products'/);
  assert.match(adminLayout, /route\.path\.startsWith\(`\$\{link\.path\}\/`\)/);
  assert.match(adminLayout, /:class="\{ 'router-link-active': isLinkActive\(link\) \}"/);
});

test('product catalog delegates create and edit to named routes', () => {
  assert.match(productsPage, /router\.push\(\{ name: 'AdminProductCreate' \}\)/);
  assert.match(productsPage, /router\.push\(\{ name: 'AdminProductEdit', params: \{ id: product\.id \} \}\)/);
});

test('product catalog contains no editor or mutation orchestration', () => {
  assert.doesNotMatch(productsPage, /showForm|modal-content|syncVariants|modifierGroup|modifierOption|comboVariantId|uploadToCloudinary|createVariant|updateVariant|deleteVariant|createModifier|updateModifier|deleteModifier|createCombo|updateCombo|deleteCombo/);
});

test('product lifecycle exposes accessible hide restore and permanent delete actions', () => {
  assert.match(productsPage, /Ẩn sản phẩm/);
  assert.match(productsPage, /Khôi phục/);
  assert.match(productsPage, /Xóa vĩnh viễn/);
  assert.match(productsPage, /statusFilter = ref\('AVAILABLE'\)/);
  assert.match(productsPage, /product\.status === 'AVAILABLE'/);
  assert.match(productsPage, /role="dialog"/);
  assert.match(productsPage, /aria-modal="true"/);
  assert.match(productsPage, /nextTick/);
  assert.match(productsPage, /hideCancelButton\.value\?\.focus\(\)/);
  assert.match(productsPage, /event\.key === 'Escape'/);
  assert.match(productsPage, /event\.key !== 'Tab'/);
  assert.match(productsPage, /previousFocus\?\.focus\(\)/);
  assert.match(productsPage, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(productsPage, /onBeforeUnmount/);
  assert.match(productsPage, /<div class="catalog-content" :inert="productToHide \|\| productToDelete \? '' : undefined">[\s\S]*<\/div>\s*<div v-if="productToHide" class="dialog-overlay"/);
  assert.doesNotMatch(productsPage, /dialog-overlay"[^>]*:inert/);
  assert.doesNotMatch(productsPage, /window\.confirm|\bconfirm\s*\(/);
  assert.doesNotMatch(productsPage, /window\.confirm|\bconfirm\s*\(/);
});

test('catalog wires canonical product types KPI and focused retry', () => {
  assert.match(productsPage, /productTypes\(adminStore\.allProducts\)/);
  assert.match(productsPage, /catalogCounts\(adminStore\.allProducts\)/);
  assert.match(productsPage, /v-model="productTypeFilter"/);
  assert.match(productsPage, /@click="loadCatalog"/);
  assert.doesNotMatch(productsPage, /\$router\.go\(0\)/);
});

test('catalog provides desktop table and mobile cards', () => {
  assert.match(productsPage, /class="[^"]*desktop-catalog[^"]*"/);
  assert.match(productsPage, /class="mobile-catalog"/);
  assert.match(productsPage, /@media\(max-width:700px\)[\s\S]*\.desktop-catalog\{display:none\}[\s\S]*\.mobile-catalog\{display:grid\}/);
});

test('routes render dedicated product editor shell', () => {
  assert.match(router, /AdminProductCreate'[\s\S]*ProductEditorPage\.vue/);
  assert.match(router, /AdminProductEdit'[\s\S]*ProductEditorPage\.vue/);
});

test('editor validates ID and directly loads detail with stale request guards', () => {
  assert.match(productEditorPage, /isValidProductId\(route\.params\.id\)/);
  assert.match(productEditorPage, /adminApi\.getProduct\(productId\.value\)/);
  assert.match(productEditorPage, /requestGeneration/);
  assert.match(productEditorPage, /stopped/);
  assert.match(productEditorPage, /onBeforeUnmount/);
  assert.match(productEditorPage, /response\?\.status === 404|status === 404/);
  assert.match(productEditorPage, /@click="loadProduct"/);
  assert.match(productEditorPage, /Đang tải sản phẩm|Không tìm thấy sản phẩm|Không thể tải sản phẩm/);
});

test('editor exposes accessible in-page section navigation and create locks', () => {
  assert.match(productEditorPage, /<nav class="section-tabs" aria-label="Phần chỉnh sửa sản phẩm">/);
  assert.doesNotMatch(productEditorPage, /role="tablist"|role="tab"|role="tabpanel"|aria-selected/);
  assert.match(productEditorPage, /:aria-current="activeSection === section\.id \? 'location' : undefined"/);
  assert.match(productEditorPage, /:aria-controls="`product-section-\$\{section\.id\}`"/);
  for (const id of ['general', 'media', 'variants', 'modifiers']) {
    assert.match(productEditorPage, new RegExp(`id: '${id}'`));
    assert.match(productEditorPage, new RegExp(`id="product-section-${id}"`));
  }
  assert.doesNotMatch(productEditorPage, /id: 'combo'|ProductComboSection|activeSection === 'combo'/);
  assert.match(productEditorPage, /disabled: isCreateMode\.value && section\.id === 'modifiers'/);
});

test('general section provides linked inline errors dirty events and mutation lock', () => {
  assert.match(productGeneralSection, /defineEmits\(\['update:modelValue', 'save', 'dirty-change'\]\)/);
  assert.match(productGeneralSection, /validateGeneral/);
  assert.match(productGeneralSection, /aria-describedby/);
  assert.match(productGeneralSection, /aria-invalid/);
  assert.match(productGeneralSection, /role="alert"/);
  assert.match(productGeneralSection, /:disabled="busy"/);
  assert.match(productGeneralSection, /sectionDirty/);
});

test('media section preserves draft on upload failure and supports preview removal', () => {
  assert.match(productMediaSection, /defineEmits\(\['update:modelValue', 'save', 'dirty-change'\]\)/);
  assert.match(productMediaSection, /CLOUDINARY_CONFIG\.uploadUrl/);
  assert.match(productMediaSection, /upload_preset/);
  assert.match(productMediaSection, /galleryImages/);
  assert.match(productMediaSection, /removeImage/);
  assert.match(productMediaSection, /sectionDirty/);
  assert.match(productMediaSection, /catch \(error\)/);
  assert.match(productMediaSection, /:disabled="busy \|\| uploading"/);
});

test('all save entries validate general and reveal focused errors', () => {
  assert.match(productEditorPage, /validateGeneral\(draft\.value\)/);
  assert.match(productEditorPage, /activeSection\.value = 'general'/);
  assert.match(productEditorPage, /generalErrors\.value = errors/);
  assert.match(productEditorPage, /nextTick\(\(\) => generalSection\.value\?\.focusFirstError\(\)\)/);
  assert.match(productGeneralSection, /externalErrors/);
  assert.match(productGeneralSection, /focusFirstError/);
});

test('parent owns canonical baseline and resets it only after accepted load or save', () => {
  assert.match(productEditorPage, /baselineVersion/);
  assert.match(productEditorPage, /baseline\.value = cloneProductState\(draft\.value\)/);
  assert.match(productEditorPage, /:baseline-version="baselineVersion"/);
  assert.match(productGeneralSection, /baselineVersion/);
  assert.match(productMediaSection, /baselineVersion/);
  assert.doesNotMatch(productGeneralSection, /resetSnapshot/);
  assert.doesNotMatch(productMediaSection, /resetSnapshot/);
});

test('mutation suppresses stale route responses before side effects', () => {
  assert.match(productEditorPage, /mutationGeneration/);
  assert.match(productEditorPage, /editorRouteKey/);
  assert.match(productEditorPage, /isCurrentEditorRequest/);
  assert.match(productEditorPage, /if \(!mutationAccepted\(request\)\) return/);
});

test('media uses shared Cloudinary config and validates upload response', () => {
  assert.match(productMediaSection, /CLOUDINARY_CONFIG/);
  assert.match(productMediaSection, /CLOUDINARY_CONFIG\.uploadPreset/);
  assert.match(productMediaSection, /CLOUDINARY_CONFIG\.uploadUrl/);
  assert.match(productMediaSection, /typeof response\.data\?\.secure_url !== 'string'/);
  assert.doesNotMatch(productMediaSection, /FastGuy_unsigned|ds4dnsj0o/);
});

test('category failure blocks editor with retry and not-found links catalog', () => {
  assert.match(productEditorPage, /categoryState/);
  assert.match(productEditorPage, /Không thể tải danh mục/);
  assert.match(productEditorPage, /@click="loadCategories"/);
  assert.match(productEditorPage, /loadState === 'ready' && categoryState === 'ready'/);
  assert.match(productEditorPage, /loadState === 'not-found'[\s\S]*AdminProducts/);
});

test('section navigation supports arrow home end navigation and focuses the current enabled control', () => {
  assert.match(productEditorPage, /nextEnabledSectionIndex/);
  assert.match(productEditorPage, /ArrowLeft/);
  assert.match(productEditorPage, /ArrowRight/);
  assert.match(productEditorPage, /Home/);
  assert.match(productEditorPage, /End/);
  assert.match(productEditorPage, /tabRefs/);
  assert.match(productEditorPage, /\.focus\(\)/);
  assert.match(productEditorPage, /@keydown="handleTabKeydown\(\$event, index\)"/);
});

test('accepted update establishes clean baseline for product slice before non-blocking refetch', () => {
  const updateBranch = productEditorPage.slice(productEditorPage.indexOf('await adminApi.updateProduct'), productEditorPage.indexOf("} catch (error)", productEditorPage.indexOf('await adminApi.updateProduct')));
  assert.ok(updateBranch.indexOf("acceptBaseline(['general', 'media'])") > updateBranch.indexOf('if (!mutationAccepted(request)) return'));
  assert.ok(updateBranch.indexOf("acceptBaseline(['general', 'media'])") < updateBranch.indexOf("reloadAfterSave(request, ['general', 'media'])"));
  assert.ok(updateBranch.indexOf('saving.value = false') < updateBranch.indexOf("reloadAfterSave(request, ['general', 'media'])"));
  assert.match(productEditorPage, /reloadMessage/);
  assert.match(productEditorPage, /async function reloadAfterSave\(mutationRequest, scope\)/);
  assert.match(productEditorPage, /reloadMessage\.value = error\.message \|\| 'Đã lưu nhưng không thể tải lại dữ liệu'/);
  assert.match(productEditorPage, /v-if="reloadMessage"[\s\S]*@click="loadProduct"/);
});

test('reload bumps mutation generation per call so stale responses lose ordering', () => {
  assert.match(productEditorPage, /async function reloadAfterSave\(mutationRequest, scope\)[\s\S]*const request = \{ generation: \+\+mutationGeneration, routeKey: editorRouteKey\.value \};/);
  assert.match(productEditorPage, /if \(mutationAccepted\(request\)\) reloadMessage\.value/);
});

test('full load invalidates in-flight reloads and drops stale reload message', () => {
  const loadBranch = productEditorPage.slice(productEditorPage.indexOf('async function loadProduct()'), productEditorPage.indexOf('async function reloadAfterSave('));
  assert.ok(loadBranch.indexOf('mutationGeneration += 1') >= 0);
  assert.ok(loadBranch.indexOf('mutationGeneration += 1') < loadBranch.indexOf('const request = { generation: ++requestGeneration'));
  assert.ok(loadBranch.indexOf("reloadMessage.value = ''") >= 0);
  assert.match(productEditorPage, /withProductSlice/);
});

test('section reload applies only affected canonical slice and preserves other dirty flags', () => {
  assert.match(productEditorPage, /function applyCanonicalSlice\(detail, scope\)[\s\S]*draft\.value = withProductSlice\(draft\.value, detail, scope\)[\s\S]*acceptBaseline\(scope\);/);
  assert.match(productEditorPage, /function acceptBaseline\(scope\)[\s\S]*baseline\.value = withProductSlice\(baseline\.value, draft\.value, scope\)/);
  assert.match(productEditorPage, /const accepted = scope \|\| Object\.keys\(dirtySections\.value\)/);
  assert.match(productEditorPage, /accepted\.forEach\(\(id\) => \{ dirtySections\.value\[id\] = false; \}\)/);
  assert.match(productEditorPage, /reloadAfterSave\(request, \['variants'\]\)/);
  assert.match(productEditorPage, /@reload="reloadFromSection\('variants'\)"/);
  assert.match(productEditorPage, /@reload="reloadFromSection\('modifiers'\)"/);
  assert.doesNotMatch(productEditorPage, /reloadFromSection\('combo'\)/);
});

test('editor saves existing API payload and replaces create route after success', () => {
  assert.match(productEditorPage, /adminApi\.createProduct\(payloadFromDraft\(\)\)/);
  assert.match(productEditorPage, /adminApi\.updateProduct\(productId\.value, payload\)/);
  assert.match(productEditorPage, /router\.replace\(\{ name: 'AdminProductEdit', params: \{ id: createdId \} \}\)/);
  assert.match(productEditorPage, /buildProductPayload\(draft\.value\)/);
});

test('editor wires variants and modifiers while combo stays dormant', () => {
  assert.match(productEditorPage, /ProductVariantsSection/);
  assert.match(productEditorPage, /ProductModifiersSection/);
  assert.doesNotMatch(productEditorPage, /ProductComboSection|activeSection === 'combo'/);
  assert.match(productEditorPage, /:pending="pendingVariants"/);
  assert.match(productEditorPage, /:mode="isCreateMode \? 'create' : 'edit'"/);
  assert.match(productEditorPage, /@retry-pending="retryPendingVariants"/);
  assert.match(productEditorPage, /@reload="reloadFromSection\('variants'\)"/);
  assert.match(productEditorPage, /@reload="reloadFromSection\('modifiers'\)"/);
});

test('variant section drafts without IDs and persists via existing endpoints', () => {
  assert.match(productVariantsSection, /createVariantDraft/);
  assert.match(productVariantsSection, /rows\.value\.push\(withUid\(\[createVariantDraft\(\)\]\)\[0\]\)/);
  assert.match(productVariantsSection, /row\.variantId/);
  assert.match(productVariantsSection, /adminApi\.createVariant/);
  assert.match(productVariantsSection, /adminApi\.updateVariant/);
  assert.match(productVariantsSection, /adminApi\.deleteVariant/);
  assert.match(productVariantsSection, /validateVariant/);
  assert.match(productVariantsSection, /variantPayload/);
  assert.doesNotMatch(productVariantsSection, /quantityAvailable/);
  assert.match(productVariantsSection, /originalPrice/);
  assert.match(productVariantsSection, /sku/);
  assert.match(productVariantsSection, /isDefault/);
  assert.match(productVariantsSection, /setDefault/);
  assert.match(productVariantsSection, /role="alert"/);
  assert.match(productVariantsSection, /aria-invalid/);
  assert.match(productVariantsSection, /:disabled="busy \|\| mutating"/);
  assert.match(productVariantsSection, /onBeforeUnmount/);
  assert.match(productVariantsSection, /stopped/);
  assert.match(productVariantsSection, /retry-pending|retryPending/);
  assert.match(productVariantsSection, /emit\('reload'\)/);
  assert.doesNotMatch(productVariantsSection, /window\.confirm|\bconfirm\s*\(/);
});

test('variant section disables clearing persisted original price and emits dirty and pending updates', () => {
  assert.match(productVariantsSection, /defineEmits\(\['update:modelValue', 'update:pending', 'save', 'reload', 'retry-pending', 'dirty-change'\]\)/);
  assert.match(productVariantsSection, /Boolean\(row\.variantId\)/);
  assert.match(productVariantsSection, /Giá gốc không thay đổi sau khi lưu/);
  assert.match(productVariantsSection, /emit\('dirty-change'/);
  assert.match(productVariantsSection, /emit\('update:pending'/);
  assert.match(productVariantsSection, /emitPending\(\)/);
  assert.match(productVariantsSection, /removePending/);
  assert.match(productVariantsSection, /:key="row\._uid"/);
  assert.match(productVariantsSection, /:key="pending\._uid"/);
  assert.match(productVariantsSection, /_uid: item\._uid \?\? \+?\+uid/);
  assert.match(productVariantsSection, /emit\('update:pending', pendingRows\.value\.map\(\(item\) => \(\{ \.\.\.item \}\)\)\)/);
  assert.match(productVariantsSection, /onMounted\(\(\) => \{ if \(pendingRows\.value\.length\) emit\('dirty-change', true\); \}\)/);
  assert.doesNotMatch(productVariantsSection, /:key="index"/);
});

test('loaded variants compare canonical shapes so navigation is not blocked before editing', () => {
  assert.match(productVariantsSection, /snapshot\.value = variants\.map\(variantShape\)/);
  assert.match(productVariantsSection, /sectionDirty\(snapshot\.value, rows\.value\.map\(variantShape\)\)/);
});

test('accepted baseline stays clean after section watchers flush', () => {
  assert.match(productEditorPage, /nextTick\(\(\) => clearAcceptedDirty\(scope\)\)/);
});

test('editor tracks dirty across visible sections and syncs editable pending variants', () => {
  assert.match(productEditorPage, /dirtySections = ref\(\{ general: false, media: false, variants: false, modifiers: false \}\)/);
  assert.match(productEditorPage, /function setSectionDirty\(section, value\)/);
  assert.match(productEditorPage, /dirtySections\[section\.id\]/);
  assert.match(productEditorPage, /setSectionDirty\('general', \$event\)/);
  assert.match(productEditorPage, /setSectionDirty\('media', \$event\)/);
  assert.match(productEditorPage, /setSectionDirty\('variants', \$event\)/);
  assert.match(productEditorPage, /setSectionDirty\('modifiers', \$event\)/);
  assert.doesNotMatch(productEditorPage, /setSectionDirty\('combo', \$event\)/);
  assert.match(productEditorPage, /@update:pending="pendingVariants = \$event"/);
  assert.match(productEditorPage, /clearAcceptedDirty\(scope\)/);
});

test('editor orchestrates partial create and retry retaining failed drafts', () => {
  assert.match(productEditorPage, /saveVariantsSection/);
  assert.match(productEditorPage, /handlePartialCreate/);
  assert.match(productEditorPage, /retryPendingVariants/);
  assert.match(productEditorPage, /pendingVariants/);
  assert.match(productEditorPage, /isValidProductId\(draft\.value\.id\)/);
  assert.match(productEditorPage, /adminApi\.createProduct\(payloadFromDraft\(\)\)/);
  assert.match(productEditorPage, /adminApi\.createVariant\(createdId, variantPayload\(variant\)\)/);
  assert.match(productEditorPage, /failed\.length/);
  assert.match(productEditorPage, /partialCreateMessage/);
  assert.match(productEditorPage, /adminApi\.createVariant\(productId\.value, variantPayload\(variant\)\)/);
  assert.match(productEditorPage, /if \(variant\.variantId\) continue/);
  assert.match(productEditorPage, /Đã tạo sản phẩm nhưng/);
});

test('modifier section fails closed and validates group limits and option price', () => {
  assert.match(productModifiersSection, /isValidProductId\(props\.productId\)/);
  assert.match(productModifiersSection, /validateModifierGroup/);
  assert.match(productModifiersSection, /validateModifierOption/);
  assert.match(productModifiersSection, /minSelections/);
  assert.match(productModifiersSection, /maxSelections/);
  assert.match(productModifiersSection, /validateModifierGroup\(groupForm\.value\)/);
  assert.match(productModifiersSection, /groupErrors\[group\.modifierGroupId\]\?\.maxSelections/);
  assert.match(productModifiersSection, /Number\(groupForm\.value\.maxSelections\)/);
  assert.match(productModifiersSection, /adminApi\.createModifierGroup/);
  assert.match(productModifiersSection, /adminApi\.updateModifierGroup/);
  assert.match(productModifiersSection, /adminApi\.deleteModifierGroup/);
  assert.match(productModifiersSection, /adminApi\.createModifierOption/);
  assert.match(productModifiersSection, /adminApi\.updateModifierOption/);
  assert.match(productModifiersSection, /adminApi\.deleteModifierOption/);
  assert.match(productModifiersSection, /emit\('reload'\)/);
  assert.match(productModifiersSection, /:disabled="busy \|\| mutating"/);
  assert.match(productModifiersSection, /onBeforeUnmount/);
  assert.match(productModifiersSection, /stopped/);
  assert.match(productModifiersSection, /role="alert"/);
  assert.doesNotMatch(productModifiersSection, /window\.confirm|\bconfirm\s*\(/);
});

test('modifier section keys errors per group and option and emits dirty state', () => {
  assert.match(productModifiersSection, /groupErrors\.value = \{ \.\.\.groupErrors\.value, \[group\.modifierGroupId\]: errors \}/);
  assert.match(productModifiersSection, /optionErrors\.value = \{ \.\.\.optionErrors\.value, \[option\.modifierOptionId\]: errors \}/);
  assert.match(productModifiersSection, /groupErrors\[group\.modifierGroupId\]\?\.name/);
  assert.match(productModifiersSection, /optionErrors\[option\.modifierOptionId\]\?\.name/);
  assert.match(productModifiersSection, /groupFormErrors/);
  assert.match(productModifiersSection, /optionFormErrors/);
  assert.match(productModifiersSection, /emit\('dirty-change'/);
  assert.match(productModifiersSection, /sectionDirty\(snapshot\.value, value \|\| \[\]\)/);
  assert.doesNotMatch(productModifiersSection, /groupErrors\.name/);
  assert.doesNotMatch(productModifiersSection, /optionErrors\.name/);
});

test('editor intercepts dirty route leave and update with shared pending navigation', () => {
  assert.match(productEditorPage, /import \{ onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter \} from 'vue-router';/);
  assert.match(productEditorPage, /import ConfirmDialog from '@\/components\/common\/ConfirmDialog\.vue';/);
  assert.match(productEditorPage, /function guardDirtyNavigation\(to, from, next\)/);
  assert.match(productEditorPage, /Object\.values\(dirtySections\.value\)\.some\(Boolean\)/);
  assert.match(productEditorPage, /onBeforeRouteLeave\(guardDirtyNavigation\)/);
  assert.match(productEditorPage, /onBeforeRouteUpdate\(guardDirtyNavigation\)/);
  assert.match(productEditorPage, /pendingNavigation = next/);
  assert.match(productEditorPage, /pendingNavigation = null/);
  assert.match(productEditorPage, /if \(proceed\) proceed\(\);/);
  assert.match(productEditorPage, /confirmDialogOpen/);
  assert.match(productEditorPage, /<ConfirmDialog/);
  assert.match(productEditorPage, /@confirm="confirmLeave"/);
  assert.match(productEditorPage, /@cancel="cancelLeave"/);
  assert.doesNotMatch(productEditorPage, /window\.confirm|\bconfirm\s*\(/);
});

test('guard supersedes pending navigation and confirm resumes latest target once', () => {
  const guardBranch = productEditorPage.slice(productEditorPage.indexOf('function guardDirtyNavigation'), productEditorPage.indexOf('function confirmLeave'));
  assert.ok(guardBranch.indexOf('if (suppressLeaveGuard) return next();') >= 0);
  assert.ok(guardBranch.indexOf('pendingNavigation = next') > guardBranch.indexOf('if (!hasDirtySections()) return next();'));
  assert.doesNotMatch(guardBranch, /if \(pendingNavigation\) return/);
  const confirmBranch = productEditorPage.slice(productEditorPage.indexOf('function confirmLeave()'), productEditorPage.indexOf('function cancelLeave()'));
  assert.ok(confirmBranch.indexOf('pendingNavigation = null') < confirmBranch.indexOf('if (proceed) proceed();'));
  assert.match(productEditorPage, /function confirmLeave\(\)[\s\S]*function cancelLeave\(\)/);
});

test('editor bypasses guard once and resets suppress flag in finally', () => {
  assert.match(productEditorPage, /if \(suppressLeaveGuard\) return next\(\);/);
  assert.match(productEditorPage, /suppressLeaveGuard = true;[\s\S]*finally \{[\s\S]*suppressLeaveGuard = false;/);
  assert.match(productEditorPage, /await handlePartialCreate\(/);
});

test('editor warns before unload when dirty and removes the listener on unmount', () => {
  assert.match(productEditorPage, /function handleBeforeUnload\(event\)[\s\S]*event\.preventDefault\(\)[\s\S]*event\.returnValue = ''/);
  assert.match(productEditorPage, /addEventListener\('beforeunload', handleBeforeUnload\)/);
  assert.match(productEditorPage, /removeEventListener\('beforeunload', handleBeforeUnload\)/);
});

test('create saves flush draft variants through shared create-once loop', () => {
  assert.match(productEditorPage, /async function createProductWithVariants\(request\)/);
  assert.match(productEditorPage, /let createdId = isValidProductId\(draft\.value\.id\) \? Number\(draft\.value\.id\) : null;/);
  assert.match(productEditorPage, /if \(createdId === null\) \{[\s\S]*adminApi\.createProduct\(payloadFromDraft\(\)\)/);
  assert.match(productEditorPage, /if \(variant\.variantId\) continue/);
  assert.match(productEditorPage, /adminApi\.createVariant\(createdId, variantPayload\(variant\)\)/);
  assert.match(productEditorPage, /if \(failed\.length\) \{[\s\S]*await handlePartialCreate\(\{ productId: createdId, failed \}\)/);
  assert.match(productEditorPage, /return createdId;/);
  assert.match(productEditorPage, /if \(createdId === null\) return;/);
});

test('create flush mirrors persisted variant id so re-invocation skips saved rows', () => {
  assert.match(productEditorPage, /const created = await adminApi\.createVariant\(createdId, variantPayload\(variant\)\);[\s\S]*if \(created\) variant\.variantId = created\.variantId \?\? created\.id;/);
  assert.match(productEditorPage, /if \(variant\.variantId\) continue;/);
});

test('saveProduct and saveVariantsSection both use shared variant flush helper', () => {
  const saveProductBranch = productEditorPage.slice(productEditorPage.indexOf('async function saveProduct()'), productEditorPage.indexOf('function selectSection'));
  assert.match(saveProductBranch, /createProductWithVariants\(request\)/);
  const saveVariantsBranch = productEditorPage.slice(productEditorPage.indexOf('async function saveVariantsSection()'), productEditorPage.indexOf('async function retryPendingVariants'));
  assert.match(saveVariantsBranch, /createProductWithVariants\(request\)/);
  assert.doesNotMatch(saveVariantsBranch, /adminApi\.createProduct|adminApi\.createVariant/);
});

test('editor presents the complete workflow in semantic order with persistent save context', () => {
  const general = productEditorPage.indexOf('<ProductGeneralSection');
  const media = productEditorPage.indexOf('<ProductMediaSection');
  const variants = productEditorPage.indexOf('<ProductVariantsSection');
  const modifiers = productEditorPage.indexOf('<ProductModifiersSection');
  assert.ok(general >= 0 && general < media && media < variants && variants < modifiers);
  assert.match(productEditorPage, /class="editor-workflow"/);
  assert.match(productEditorPage, /class="save-context"/);
  assert.match(productEditorPage, /class="mobile-final-save"/);
  assert.match(productEditorPage, /@click="saveProduct"/);
  assert.match(productEditorPage, /Object\.values\(dirtySections\.value\)\.some\(Boolean\)/);
  assert.match(productEditorPage, /@media\(min-width:901px\)[\s\S]*\.save-context[\s\S]*position:sticky/);
  assert.match(productEditorPage, /@media\(max-width:900px\)[\s\S]*\.mobile-final-save[\s\S]*display:flex/);
});

test('editor sections expose visible headings helpers and adjacent errors', () => {
  assert.match(productGeneralSection, /<h2[^>]*>Thông tin chung<\/h2>/);
  assert.match(productGeneralSection, /class="section-helper"/);
  assert.match(productGeneralSection, /aria-describedby="errors\.name \? 'product-name-error' : 'product-name-helper'"/);
  assert.match(productMediaSection, /class="section-helper"/);
  assert.match(productMediaSection, /id="primary-image-helper"/);
  assert.match(productMediaSection, /aria-describedby="primary-image-helper"/);
  assert.match(productVariantsSection, /class="section-helper"/);
  assert.match(productModifiersSection, /class="section-helper"/);
});

test('ConfirmDialog declares accessible confirmation props and events', () => {
  assert.match(confirmDialog, /const props = defineProps\(\{/);
  assert.match(confirmDialog, /open: \{ type: Boolean, default: false \}/);
  assert.match(confirmDialog, /title: \{ type: String, default: '' \}/);
  assert.match(confirmDialog, /message: \{ type: String, default: '' \}/);
  assert.match(confirmDialog, /confirmLabel: \{ type: String, default: 'Xác nhận' \}/);
  assert.match(confirmDialog, /busy: \{ type: Boolean, default: false \}/);
  assert.match(confirmDialog, /defineEmits\(\['confirm', 'cancel'\]\)/);
  assert.doesNotMatch(confirmDialog, /window\.confirm/);
});

test('ConfirmDialog traps focus unlocks body and restores trigger focus', () => {
  assert.match(confirmDialog, /role="dialog"/);
  assert.match(confirmDialog, /aria-modal="true"/);
  assert.match(confirmDialog, /event\.key === 'Escape'/);
  assert.match(confirmDialog, /event\.key !== 'Tab'/);
  assert.match(confirmDialog, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(confirmDialog, /previousFocus/);
  assert.match(confirmDialog, /nextTick/);
  assert.match(confirmDialog, /onBeforeUnmount/);
  assert.match(confirmDialog, /\.focus\(\)/);
  assert.match(confirmDialog, /:disabled="busy"/);
});

test('router migrates legacy edit query to edit route without navigation loop', () => {
  assert.match(router, /import \{ isValidProductId \} from '@\/utils\/adminProductEditor';/);
  assert.match(router, /'edit' in to\.query/);
  assert.match(router, /isValidProductId\(editId\)/);
  assert.match(router, /next\(\{ name: 'AdminProductEdit', params: \{ id: Number\(editId\) \} \}\)/);
  assert.match(router, /next\(\{ name: 'AdminProducts' \}\)/);
});

test('inventory overview drops legacy product edit shortcut', () => {
  assert.doesNotMatch(inventoryPage, /products\?edit=/);
  assert.doesNotMatch(inventoryPage, /AdminProductEdit/);
});
