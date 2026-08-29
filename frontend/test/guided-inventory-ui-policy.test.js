import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../src/', import.meta.url);
const read = (path) => readFile(new URL(path, root), 'utf8');
const [layout, inventory, receipts, recipes, counts, variants] = await Promise.all([
  read('layouts/AdminLayout.vue'),
  read('views/admin/InventoryPage.vue'),
  read('views/admin/GoodsReceiptsPage.vue'),
  read('views/admin/RecipesPage.vue'),
  read('views/admin/StockCountsPage.vue'),
  read('components/admin/product-editor/ProductVariantsSection.vue'),
]);

test('inventory navigation is grouped under one plain-language warehouse section', () => {
  assert.match(layout, /label: 'Kho hàng'/);
  for (const label of ['Tồn kho', 'Nhập hàng', 'Công thức & định mức']) assert.ok(layout.includes(label), label);
  for (const path of ['/admin/inventory', '/admin/inventory/receipts', '/admin/recipes']) assert.ok(layout.includes(path), path);
  assert.doesNotMatch(layout, /label: 'Kiểm kê'|label: 'Lịch sử kho'|label: 'Báo cáo theo món'/);
});

test('inventory overview guides work then keeps operational detail expandable', () => {
  for (const label of ['Hôm nay cần làm gì?', 'Nhập hàng', 'Công thức', 'Bán món', 'Lãi gộp', 'Giá trị tồn hiện tại', 'Giá vốn hiện tại']) assert.ok(inventory.includes(label), label);
  assert.doesNotMatch(inventory, /AdminStockCounts/);
});

test('goods receipts use three stages, truthful previews, draft priority, and accessible confirmations', () => {
  for (const label of ['Thông tin phiếu', 'Nguyên liệu nhận được', 'Tóm tắt phiếu', 'Số lượng nhận', 'Đơn vị mua', 'Kho sẽ tăng', 'Giá vốn quy đổi', 'Lưu nháp', 'Kiểm tra & duyệt']) assert.ok(receipts.includes(label), label);
  assert.match(receipts, /goodsReceiptTotal/);
  assert.match(receipts, /goodsReceiptUnitOptions/);
  assert.match(receipts, /receiptCostWarning/);
  assert.match(receipts, /async function save\(review = false\)[\s\S]*confirmation\.value = \{ kind: 'approve', receipt: saved \}/);
  assert.match(receipts, /ConfirmDialog/);
  assert.doesNotMatch(receipts, /window\.confirm/);
  assert.match(receipts, /DRAFT[\s\S]*sort|sort[\s\S]*DRAFT/);
  assert.doesNotMatch(receipts, /Mỗi đơn vị mua có|Lưu nháp an toàn|Chi phí mỗi đơn vị cơ sở/);
});

test('recipes explain modes, outcomes, advanced yield, and per-serving ingredient effects', () => {
  for (const label of ['Tự động theo công thức', 'Đếm món làm sẵn', 'Không quản lý tồn', 'Tạm ngừng bán', 'Một mẻ làm được bao nhiêu phần?', 'Có thể làm', 'Giới hạn', 'Chi phí / phần', 'Food cost']) assert.ok(recipes.includes(label), label);
  assert.match(recipes, /<details/);
  assert.match(recipes, /presentRecipeDraft/);
  for (const label of ['Mỗi phần', 'Khả dụng', 'Làm được', 'Giá tham chiếu']) assert.ok(recipes.includes(label), label);
  assert.match(recipes, /buildRecipePayload\(form\.value/);
  assert.match(recipes, /Lưu cách quản lý tồn/);
  assert.match(recipes, /ConfirmDialog/);
  assert.doesNotMatch(recipes, /updateVariantInventorySettings[\s\S]{0,300}replaceVariantRecipe|replaceVariantRecipe[\s\S]{0,300}updateVariantInventorySettings/);
  assert.match(recipes, /Dữ liệu đã thay đổi/);
});

test('variant changes clear all variant state and disable actions until the new load succeeds', () => {
  assert.match(recipes, /const variantLoading = ref\(false\)/);
  assert.match(recipes, /async function loadVariant\(\)[\s\S]*variantLoading\.value = true[\s\S]*form\.value = blankForm\(\)[\s\S]*baseline\.value = null[\s\S]*capacity\.value = null/);
  assert.match(recipes, /finally[\s\S]*variantLoading\.value = false/);
  assert.match(recipes, /function requestRecipeSave\(\)[\s\S]*variantLoading\.value[\s\S]*recipeError\.value/);
  assert.match(recipes, /function requestSettingsSave\(\)[\s\S]*variantLoading\.value[\s\S]*recipeError\.value/);
  assert.match(recipes, /const requestVariantId = variantId\.value/);
  assert.match(recipes, /requestVariantId !== variantId\.value/);
});

test('dirty recipe navigation is confirmed before selectors or routes change', () => {
  assert.match(recipes, /onBeforeRouteLeave/);
  assert.match(recipes, /beforeunload/);
  assert.match(recipes, /Bỏ thay đổi chưa lưu\?/);
  assert.match(recipes, /requestSelection/);
  assert.doesNotMatch(recipes, /v-model="productId"[^>]*@change="chooseProduct"/);
  assert.doesNotMatch(recipes, /v-model="variantId"/);
});

test('dirty recipe intents have one latest pending action and always settle superseded routes', () => {
  assert.match(recipes, /let pendingAction = null/);
  assert.doesNotMatch(recipes, /pendingSelection|pendingNavigation/);
  assert.match(recipes, /function replacePendingAction\(action\)[\s\S]*pendingAction\?\.type === 'route'[\s\S]*pendingAction\.next\(false\)[\s\S]*pendingAction = action/);
  assert.match(recipes, /requestSelection[\s\S]*replacePendingAction\(\{ type: 'selection', selection: \{ type, value \} \}\)/);
  assert.match(recipes, /onBeforeRouteLeave[\s\S]*replacePendingAction\(\{ type: 'route', next \}\)/);
  assert.match(recipes, /function closeConfirm\(\)[\s\S]*replacePendingAction\(null\)/);
  assert.match(recipes, /onBeforeUnmount\([\s\S]*replacePendingAction\(null\)/);
});

test('recipe picker only offers active ingredient inventory items', () => {
  assert.match(recipes, /item\.active\s*!==\s*false\s*&&\s*item\.itemType\s*===\s*'INGREDIENT'/);
});

test('editable rows always come from the local recipe draft, never the saved capacity snapshot', () => {
  assert.match(recipes, /const ingredientRows = computed\(\(\) => localPresentation\.value\.lines/);
  assert.doesNotMatch(recipes, /ingredientRows = computed\(\(\) => presentation\.value\.ingredients/);
  assert.match(recipes, /const capacityRows = computed/);
});

test('variant mode changes use settings endpoint and recipe writes never include inventoryMode', () => {
  assert.match(variants, /changeMode[\s\S]*updateVariantInventorySettings\(row\.variantId, \{ inventoryMode: selectedMode, expectedUpdatedAt: row\.updatedAt \}\)/);
  assert.match(variants, /const updated = await adminApi\.updateVariantInventorySettings[\s\S]*row\.updatedAt = updated\.updatedAt/);
  assert.doesNotMatch(variants, /changeMode[\s\S]*replaceVariantRecipe/);
  assert.doesNotMatch(`${variants}\n${recipes}`, /replaceVariantRecipe\([^)]*,\s*\{[^}]*inventoryMode/s);
  assert.match(variants, /const previousMode = row\.inventoryMode/);
  assert.match(variants, /row\.inventoryMode = previousMode/);
  assert.doesNotMatch(variants, /row\.inventoryMode = ''/);
});

test('persisted variant deletion requires the shared confirmation while draft removal stays immediate', () => {
  assert.match(variants, /import ConfirmDialog from '@\/components\/common\/ConfirmDialog\.vue'/);
  assert.match(variants, /function requestDelete\(row\)[\s\S]*if \(!row\.variantId\)[\s\S]*rows\.value\.splice[\s\S]*return;[\s\S]*pendingDelete\.value = row/);
  assert.match(variants, /async function confirmDelete\(\)[\s\S]*if \(props\.busy \|\| mutating\.value \|\| locked\.value \|\| !pendingDelete\.value\?\.variantId\) return;[\s\S]*await adminApi\.deleteVariant\(row\.variantId\)/);
  assert.doesNotMatch(variants.match(/function requestDelete\(row\)[\s\S]*?function cancelDelete/)?.[0] || '', /adminApi\.deleteVariant/);
  assert.match(variants, /function cancelDelete\(\)[\s\S]*if \(!mutating\.value\) pendingDelete\.value = null/);
  assert.match(variants, /<ConfirmDialog[\s\S]*:open="Boolean\(pendingDelete\)"[\s\S]*:busy="mutating"[\s\S]*@confirm="confirmDelete"[\s\S]*@cancel="cancelDelete"/);
  assert.match(variants, /pendingDelete\?\.variantName/);
  assert.match(variants, /xóa vĩnh viễn/i);
  assert.match(variants, /@click="requestDelete\(row\)"/);
});

test('stale recipe and settings saves keep drafts and require explicit confirmed reload', () => {
  assert.match(recipes, /buildRecipePayload\(form\.value, baseline\.value\.recipeUpdatedAt\)/);
  assert.match(recipes, /buildInventorySettingsPayload\(inventoryMode\.value, baseline\.value\.settingsUpdatedAt\)/);
  assert.match(recipes, /error\.status === 409[\s\S]*conflictNotice\.value/);
  assert.match(recipes, /Tải lại dữ liệu mới/);
  assert.match(recipes, /confirmAction === 'reload'/);
  assert.doesNotMatch(recipes, /error\.status === 409[\s\S]{0,300}loadVariant\(\)/);
});

test('clean summaries use server capacity for every mode and only dirty ingredient drafts use estimates', () => {
  assert.match(recipes, /const presentation = computed\(\(\) => inventoryMode\.value === 'INGREDIENT' && recipeDirty\.value \? localPresentation\.value : capacity\.value/);
  assert.match(recipes, /v-if="inventoryMode === 'INGREDIENT' && recipeDirty" class="estimate-label"/);
  assert.match(recipes, /const summaryRows = computed\(\(\) => presentation\.value\?\.ingredients/);
});

test('single-resource saves refresh only their own baseline and capacity', () => {
  assert.match(recipes, /async function refreshRecipeAndCapacity/);
  assert.match(recipes, /async function refreshSettingsAndCapacity/);
  assert.match(recipes, /Không thể tải lại công thức vừa lưu/);
  assert.match(recipes, /Không thể tải lại cách quản lý tồn vừa lưu/);
  assert.doesNotMatch(recipes, /async function saveRecipe\(\)[\s\S]*?await loadVariant\(\)/);
  assert.doesNotMatch(recipes, /async function saveSettings\(\)[\s\S]*?await loadVariant\(\)/);
});

test('each save locks only its resource while every save blocks selection and navigation', () => {
  assert.match(recipes, /const savingRecipe = ref\(false\)/);
  assert.match(recipes, /const savingSettings = ref\(false\)/);
  assert.match(recipes, /const savePending = computed\(\(\) => savingRecipe\.value \|\| savingSettings\.value\)/);
  assert.match(recipes, /requestSelection[\s\S]*savePending\.value/);
  assert.match(recipes, /onBeforeRouteLeave[\s\S]*savePending\.value/);
  assert.match(recipes, /class="panel settings-panel"[\s\S]*fieldset :disabled="actionsDisabled \|\| savingSettings"/);
  assert.match(recipes, /class="editor-fields" :disabled="actionsDisabled \|\| savingRecipe"/);
  assert.match(recipes, /savingRecipe \? 'Đang lưu công thức\.\.\.' : 'Lưu công thức'/);
  assert.match(recipes, /savingSettings \? 'Đang lưu cách quản lý tồn\.\.\.' : 'Lưu cách quản lý tồn'/);
});

test('ingredient picker contains outside focus, locks scroll, and closes through one helper', () => {
  assert.match(recipes, /focusin/);
  assert.match(recipes, /document\.body\.style\.overflow\s*=\s*'hidden'/);
  assert.match(recipes, /pickerDialog\.value\.contains\(event\.target\)/);
  assert.match(recipes, /function closePicker\(\)[\s\S]*pickerOpen\.value\s*=\s*false/);
  assert.doesNotMatch(recipes, /@click="pickerOpen\s*=\s*false"/);
});

test('stock counts provide staged progress, filters, variance-only reasons, summaries, and accessible approval', () => {
  for (const label of ['Chọn nhóm', 'Nhập số đếm', 'Xem chênh lệch & duyệt', 'Chưa đếm', 'Có chênh lệch', 'Đã khớp', 'Mặt hàng thiếu', 'Mặt hàng dư', 'Giá trị hao hụt']) assert.ok(counts.includes(label), label);
  assert.match(counts, /stockCountProgress/);
  assert.match(counts, /stockCountSummary/);
  assert.match(counts, /ConfirmDialog/);
  assert.doesNotMatch(counts, /window\.confirm/);
  assert.match(counts, /variance\(line\)\.quantity\s*!==\s*0/);
  assert.match(counts, /buildStockCountPayload\(selected\.value\.items\)/);
  assert.match(counts, /requestApproval/);
  assert.match(counts, /validateStockCount\(selected\.value\.items\)/);
  assert.match(counts, /shortageItemCount/);
  assert.match(counts, /surplusItemCount/);
  assert.match(counts, /@media\(max-width:900px\)[\s\S]*\.table\{min-width:0\}/);
});

test('confirmation dialog redirects outside focus into its focus cycle', async () => {
  const dialog = await read('components/common/ConfirmDialog.vue');
  assert.match(dialog, /document\.addEventListener\('keydown', handleKeydown\)/);
  assert.match(dialog, /!dialogRef\.value\.contains\(document\.activeElement\)/);
  assert.match(dialog, /event\.shiftKey\s*\?\s*last\s*:\s*first/);
  assert.match(dialog, /event\.key === 'Escape'[\s\S]*requestCancel\(\)/);
  assert.match(dialog, /previousFocus\.value = document\.activeElement/);
  assert.match(dialog, /nextTick\(\(\) => focusTarget\?\.focus\(\)\)/);
});

test('guided inventory controls retain touch and visible focus policy', () => {
  for (const source of [layout, inventory, receipts, recipes, counts]) {
    assert.match(source, /min-height:\s*(40|4[4-9])px/);
    assert.match(source, /:focus-visible/);
  }
});
