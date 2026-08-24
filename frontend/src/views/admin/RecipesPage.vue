<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import { buildRecipePayload, validateRecipeForm } from '@/utils/inventoryItem';
import { buildInventorySettingsPayload, formatInventoryQuantity, formatReferenceCost, isInventorySettingsDirty, isRecipeDraftDirty, presentRecipeDraft } from '@/utils/recipeAdmin';

const MODES = [
  { value: 'INGREDIENT', label: 'Tự động theo công thức' },
  { value: 'FINISHED_GOOD', label: 'Đếm món làm sẵn' },
  { value: 'UNTRACKED', label: 'Không quản lý tồn' },
  { value: 'SUSPENDED', label: 'Tạm ngừng bán' },
];
const toast = useToast();
const adminStore = useAdminStore();
const route = useRoute();
const loading = ref(true);
const loadError = ref('');
const recipeError = ref('');
const variantLoading = ref(false);
const savingRecipe = ref(false);
const savingSettings = ref(false);
const saveError = ref('');
const settingsError = ref('');
const conflictNotice = ref('');
const liveMessage = ref('');
const productId = ref('');
const variantId = ref('');
const productSearch = ref('');
const inventoryMode = ref('UNTRACKED');
const form = ref(blankForm());
const baseline = ref(null);
const capacity = ref(null);
const items = ref([]);
const pickerOpen = ref(false);
const pickerSearch = ref('');
const pickerItem = ref(null);
const pickerQuantity = ref('');
const confirmAction = ref('');
let pendingAction = null;
const pickerDialog = ref(null);
const pickerSearchInput = ref(null);
const pickerTrigger = ref(null);
const recipeSaveButton = ref(null);
const settingsSaveButton = ref(null);
let pickerPreviousFocus = null;
let pickerPreviousOverflow = '';
let generation = 0;

function blankForm() { return { yieldQuantity: '1', active: true, items: [] }; }
const products = computed(() => adminStore.allProducts.filter((product) => product.name.toLocaleLowerCase('vi').includes(productSearch.value.trim().toLocaleLowerCase('vi'))));
const selectedProduct = computed(() => adminStore.allProducts.find((product) => String(product.id) === productId.value) || null);
const variants = computed(() => (selectedProduct.value?.variants || []).filter((variant) => variant.variantId));
const selectedVariant = computed(() => variants.value.find((variant) => String(variant.variantId) === variantId.value) || null);
const itemsById = computed(() => new Map(items.value.map((item) => [Number(item.inventoryItemId), item])));
const recipeDirty = computed(() => isRecipeDraftDirty(form.value, baseline.value));
const settingsDirty = computed(() => isInventorySettingsDirty(inventoryMode.value, baseline.value));
const savePending = computed(() => savingRecipe.value || savingSettings.value);
const localPresentation = computed(() => presentRecipeDraft(form.value, items.value, selectedVariant.value?.price));
const presentation = computed(() => inventoryMode.value === 'INGREDIENT' && recipeDirty.value ? localPresentation.value : capacity.value);
const ingredientRows = computed(() => localPresentation.value.lines || []);
const capacityRows = computed(() => capacity.value?.ingredients || []);
const summaryRows = computed(() => presentation.value?.ingredients || presentation.value?.lines || []);
const actionsDisabled = computed(() => variantLoading.value || Boolean(recipeError.value) || !baseline.value);
const selectedIds = computed(() => new Set(form.value.items.map((line) => Number(line.inventoryItemId))));
const pickerItems = computed(() => items.value
  .filter((item) => item.active !== false && item.itemType === 'INGREDIENT' && !selectedIds.value.has(Number(item.inventoryItemId)))
  .filter((item) => `${item.name} ${item.inventoryCode}`.toLocaleLowerCase('vi').includes(pickerSearch.value.trim().toLocaleLowerCase('vi')))
  .sort((a, b) => a.name.localeCompare(b.name, 'vi')));
const money = (value) => value == null ? '—' : `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 0 })} ₫`;
const baseUnitLabel = (unit) => ({ G: 'g', ML: 'ml', PIECE: 'cái' }[unit] || unit || '');
const costComplete = computed(() => presentation.value?.costStatus === 'COMPLETE');

async function loadBaseData() {
  loading.value = true;
  loadError.value = '';
  try {
    const [itemList] = await Promise.all([adminApi.getInventoryItems(), adminStore.fetchProducts()]);
    items.value = Array.isArray(itemList) ? itemList : [];
  } catch (error) {
    loadError.value = error.message || 'Không thể tải dữ liệu công thức';
  } finally { loading.value = false; }
}

function applyLoaded(recipe, settings) {
  form.value = recipe ? {
    yieldQuantity: String(recipe.yieldQuantity ?? 1),
    active: Boolean(recipe.active),
    items: (recipe.items || []).map((line) => ({ inventoryItemId: String(line.inventoryItemId), quantity: String(line.quantity) })),
  } : blankForm();
  inventoryMode.value = settings.inventoryMode;
  baseline.value = { recipe: buildRecipePayload(form.value), recipeUpdatedAt: recipe?.updatedAt || null, inventoryMode: inventoryMode.value, settingsUpdatedAt: settings.updatedAt };
  conflictNotice.value = '';
}

function recipeForm(recipe) {
  return recipe ? {
    yieldQuantity: String(recipe.yieldQuantity ?? 1),
    active: Boolean(recipe.active),
    items: (recipe.items || []).map((line) => ({ inventoryItemId: String(line.inventoryItemId), quantity: String(line.quantity) })),
  } : blankForm();
}

async function refreshRecipeAndCapacity(requestVariantId, requestGeneration) {
  try {
    const [recipe, refreshedCapacity] = await Promise.all([
      adminApi.getVariantRecipe(requestVariantId).catch((error) => error.status === 404 ? null : Promise.reject(error)),
      adminApi.getVariantInventoryCapacity(requestVariantId).catch((error) => error.status === 404 ? null : Promise.reject(error)),
    ]);
    if (requestGeneration !== generation || requestVariantId !== variantId.value) return;
    form.value = recipeForm(recipe);
    baseline.value = { ...baseline.value, recipe: buildRecipePayload(form.value), recipeUpdatedAt: recipe.updatedAt };
    capacity.value = refreshedCapacity;
  } catch (error) {
    if (requestGeneration === generation && requestVariantId === variantId.value) saveError.value = 'Đã lưu công thức. Không thể tải lại công thức vừa lưu và năng lực hiện tại.';
  }
}

async function refreshSettingsAndCapacity(requestVariantId, requestGeneration) {
  try {
    const [settings, refreshedCapacity] = await Promise.all([
      adminApi.getVariantInventorySettings(requestVariantId),
      adminApi.getVariantInventoryCapacity(requestVariantId).catch((error) => error.status === 404 ? null : Promise.reject(error)),
    ]);
    if (requestGeneration !== generation || requestVariantId !== variantId.value) return;
    inventoryMode.value = settings.inventoryMode;
    baseline.value = { ...baseline.value, inventoryMode: inventoryMode.value, settingsUpdatedAt: settings.updatedAt };
    capacity.value = refreshedCapacity;
  } catch (error) {
    if (requestGeneration === generation && requestVariantId === variantId.value) settingsError.value = 'Đã lưu cách quản lý tồn. Không thể tải lại cách quản lý tồn vừa lưu và năng lực hiện tại.';
  }
}

async function loadVariant() {
  const request = ++generation;
  const requestVariantId = variantId.value;
  variantLoading.value = true;
  recipeError.value = '';
  saveError.value = '';
  settingsError.value = '';
  conflictNotice.value = '';
  replacePendingAction(null);
  confirmAction.value = '';
  form.value = blankForm();
  inventoryMode.value = 'UNTRACKED';
  baseline.value = null;
  capacity.value = null;
  if (!requestVariantId) { variantLoading.value = false; return; }
  try {
    const [recipeResult, settings, capacityResult] = await Promise.all([
      adminApi.getVariantRecipe(requestVariantId).catch((error) => error.status === 404 ? null : Promise.reject(error)),
      adminApi.getVariantInventorySettings(requestVariantId),
      adminApi.getVariantInventoryCapacity(requestVariantId).catch((error) => error.status === 404 ? null : Promise.reject(error)),
    ]);
    if (request !== generation) return;
    applyLoaded(recipeResult, settings);
    capacity.value = capacityResult;
  } catch (error) {
    if (request === generation) recipeError.value = error.message || 'Không thể tải công thức hiện tại';
  } finally { if (request === generation && requestVariantId === variantId.value) variantLoading.value = false; }
}

function dirty() { return recipeDirty.value || settingsDirty.value; }
function applySelection(type, value) {
  if (type === 'product') { productId.value = value; variantId.value = ''; }
  else variantId.value = value;
}
function replacePendingAction(action) {
  if (pendingAction?.type === 'route') pendingAction.next(false);
  pendingAction = action;
}
function requestSelection(type, event) {
  const value = event.target.value;
  const current = type === 'product' ? productId.value : variantId.value;
  if (savePending.value) { event.target.value = current; return; }
  if (!dirty()) { applySelection(type, value); return; }
  event.target.value = current;
  replacePendingAction({ type: 'selection', selection: { type, value } });
  confirmAction.value = 'discard';
}
function lineFor(row) { return form.value.items.find((line) => Number(line.inventoryItemId) === Number(row.inventoryItemId)); }
function lineErrorMessage(index) { return validateRecipeForm(form.value).lines?.[index] || ''; }
function removeLine(index) { form.value.items.splice(index, 1); }
function resetDraft() { if (!baseline.value) return; form.value = { ...baseline.value.recipe, yieldQuantity: String(baseline.value.recipe.yieldQuantity), items: baseline.value.recipe.items.map((line) => ({ ...line, inventoryItemId: String(line.inventoryItemId), quantity: String(line.quantity) })) }; saveError.value = ''; }

function openPicker() {
  pickerPreviousFocus = document.activeElement;
  pickerPreviousOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  pickerSearch.value = '';
  pickerItem.value = null;
  pickerQuantity.value = '';
  pickerOpen.value = true;
  nextTick(() => pickerSearchInput.value?.focus());
}
function closePicker() { pickerOpen.value = false; pickerItem.value = null; document.body.style.overflow = pickerPreviousOverflow; nextTick(() => pickerPreviousFocus?.focus()); }
function selectPickerItem(item) {
  const index = form.value.items.findIndex((line) => Number(line.inventoryItemId) === Number(item.inventoryItemId));
  if (index >= 0) {
    liveMessage.value = `${item.name} đã có trong công thức. Đã chuyển đến dòng hiện có.`;
    closePicker();
    nextTick(() => document.querySelector(`[data-recipe-line="${index}"] input`)?.focus());
    return;
  }
  pickerItem.value = item;
  pickerQuantity.value = '';
  nextTick(() => pickerDialog.value?.querySelector('input[type="number"]')?.focus());
}
function addPickerItem() {
  const quantity = Number(String(pickerQuantity.value).replace(',', '.'));
  if (!pickerItem.value || !(quantity > 0)) { liveMessage.value = 'Nhập số lượng lớn hơn 0.'; return; }
  form.value.items.push({ inventoryItemId: String(pickerItem.value.inventoryItemId), quantity: String(pickerQuantity.value).replace(',', '.') });
  liveMessage.value = `Đã thêm ${pickerItem.value.name}.`;
  closePicker();
}

function focusable(dialog) { return [...dialog.querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),summary,[tabindex]:not([tabindex="-1"])')]; }
function trap(event, dialog) {
  if (event.key !== 'Tab') return;
  const nodes = focusable(dialog);
  if (!nodes.length) return;
  if (event.shiftKey && document.activeElement === nodes[0]) { event.preventDefault(); nodes.at(-1).focus(); }
  else if (!event.shiftKey && document.activeElement === nodes.at(-1)) { event.preventDefault(); nodes[0].focus(); }
}
function onKeydown(event) {
  if (event.key === 'Escape' && pickerOpen.value) closePicker();
  else if (pickerOpen.value) trap(event, pickerDialog.value);
}
function containPickerFocus(event) {
  if (pickerOpen.value && pickerDialog.value && !pickerDialog.value.contains(event.target)) pickerSearchInput.value?.focus();
}
function requestRecipeSave() {
  if (!recipeDirty.value || savingRecipe.value || variantLoading.value || recipeError.value || !baseline.value) return;
  saveError.value = '';
  const errors = validateRecipeForm(form.value);
  if (Object.keys(errors).length) { saveError.value = errors.yieldQuantity || errors.items || 'Kiểm tra lại các dòng nguyên liệu'; return; }
  confirmAction.value = 'recipe';
}
function requestSettingsSave() { if (!settingsDirty.value || savingSettings.value || variantLoading.value || recipeError.value || !baseline.value) return; settingsError.value = ''; confirmAction.value = 'settings'; }
function requestConflictReload() { confirmAction.value = 'reload'; }
function closeConfirm() {
  replacePendingAction(null);
  confirmAction.value = '';
}

async function saveRecipe() {
  const requestVariantId = variantId.value;
  const requestGeneration = generation;
  const payload = buildRecipePayload(form.value, baseline.value.recipeUpdatedAt);
  if (actionsDisabled.value || savingRecipe.value) return;
  savingRecipe.value = true;
  saveError.value = '';
  closeConfirm();
  try {
    if (requestGeneration !== generation || requestVariantId !== variantId.value) return;
    await adminApi.replaceVariantRecipe(requestVariantId, payload);
    toast.success('Đã lưu công thức');
    await refreshRecipeAndCapacity(requestVariantId, requestGeneration);
  } catch (error) {
    if (error.status === 409) conflictNotice.value = 'Dữ liệu đã thay đổi ở nơi khác. Bản nháp của bạn vẫn được giữ.';
    saveError.value = error.status === 409 ? (error.message || 'Dữ liệu đã thay đổi. Tải lại dữ liệu mới trước khi lưu.') : error.status === 400 ? 'Công thức chưa hợp lệ. Kiểm tra số phần và nguyên liệu rồi thử lại.' : error.status === 404 ? 'Không tìm thấy kích cỡ món. Chọn lại món rồi thử lại.' : 'Không thể lưu công thức. Vui lòng thử lại.';
  } finally { savingRecipe.value = false; nextTick(() => recipeSaveButton.value?.focus()); }
}

async function saveSettings() {
  const requestVariantId = variantId.value;
  const requestGeneration = generation;
  const payload = buildInventorySettingsPayload(inventoryMode.value, baseline.value.settingsUpdatedAt);
  if (actionsDisabled.value || savingSettings.value) return;
  savingSettings.value = true;
  settingsError.value = '';
  closeConfirm();
  try {
    if (requestGeneration !== generation || requestVariantId !== variantId.value) return;
    await adminApi.updateVariantInventorySettings(requestVariantId, payload);
    toast.success('Đã lưu cách quản lý tồn');
    await refreshSettingsAndCapacity(requestVariantId, requestGeneration);
  } catch (error) {
    if (error.status === 409) conflictNotice.value = 'Dữ liệu đã thay đổi ở nơi khác. Bản nháp của bạn vẫn được giữ.';
    settingsError.value = error.status === 409 ? (error.message || 'Dữ liệu đã thay đổi. Tải lại dữ liệu mới trước khi lưu.') : error.status === 404 ? 'Không tìm thấy kích cỡ món. Chọn lại món rồi thử lại.' : 'Không thể lưu cách quản lý tồn. Vui lòng thử lại.';
  } finally { savingSettings.value = false; nextTick(() => settingsSaveButton.value?.focus()); }
}
function confirmSave() {
  if (confirmAction.value === 'reload') { confirmAction.value = ''; loadVariant(); return; }
  if (confirmAction.value === 'recipe') { saveRecipe(); return; }
  if (confirmAction.value === 'settings') { saveSettings(); return; }
  const action = pendingAction;
  pendingAction = null;
  confirmAction.value = '';
  if (action?.type === 'selection') applySelection(action.selection.type, action.selection.value);
  else if (action?.type === 'route') action.next();
}

onBeforeRouteLeave((to, from, next) => {
  if (savePending.value) { next(false); return; }
  if (!dirty()) { next(); return; }
  replacePendingAction({ type: 'route', next });
  confirmAction.value = 'discard';
});
function handleBeforeUnload(event) {
  if (!dirty() && !savePending.value) return;
  event.preventDefault();
  event.returnValue = '';
}

watch(variantId, () => loadVariant());
onMounted(async () => {
  document.addEventListener('keydown', onKeydown);
  document.addEventListener('focusin', containPickerFocus);
  window.addEventListener('beforeunload', handleBeforeUnload);
  await loadBaseData();
  const requested = Number(route.query.variantId);
  const product = adminStore.allProducts.find((entry) => (entry.variants || []).some((variant) => Number(variant.variantId) === requested));
  if (product) { productId.value = String(product.id); variantId.value = String(requested); }
});
onBeforeUnmount(() => { document.removeEventListener('keydown', onKeydown); document.removeEventListener('focusin', containPickerFocus); window.removeEventListener('beforeunload', handleBeforeUnload); replacePendingAction(null); if (pickerOpen.value) document.body.style.overflow = pickerPreviousOverflow; });
</script>

<template>
  <main class="recipes-page">
    <header class="page-heading"><div><p class="eyebrow">Vận hành</p><h1>Công thức cho 1 phần</h1><p>Chọn món và kích cỡ, sau đó khai báo lượng nguyên liệu.</p></div><button class="btn btn-outline" :disabled="loading" @click="loadBaseData">Làm mới</button></header>
    <p class="sr-only" aria-live="polite">{{ liveMessage }}</p>
    <section v-if="loadError" class="panel error-inline" role="alert">{{ loadError }} <button class="btn btn-outline" @click="loadBaseData">Thử lại</button></section>
    <template v-else>
      <section class="panel selector-panel" aria-label="Chọn món và kích cỡ">
        <div><label class="form-label" for="product-search">Tìm món</label><input id="product-search" v-model="productSearch" class="form-input" type="search" placeholder="Tên món" /></div>
        <div><label class="form-label" for="product-select">Món</label><select id="product-select" :value="productId" class="form-select" :disabled="savePending" @change="requestSelection('product', $event)"><option value="">Chọn món</option><option v-for="product in products" :key="product.id" :value="String(product.id)">{{ product.name }}</option></select></div>
        <div><label class="form-label" for="variant-select">Kích cỡ</label><select id="variant-select" :value="variantId" class="form-select" :disabled="!selectedProduct || savePending" @change="requestSelection('variant', $event)"><option value="">Chọn kích cỡ</option><option v-for="variant in variants" :key="variant.variantId" :value="String(variant.variantId)">{{ variant.variantName || 'Mặc định' }}</option></select></div>
      </section>

      <template v-if="selectedVariant">
        <aside v-if="conflictNotice" class="panel error-inline" role="alert"><span>{{ conflictNotice }}</span><button type="button" class="btn btn-outline" @click="requestConflictReload">Tải lại dữ liệu mới</button></aside>
        <section class="panel settings-panel"><h2>Cài đặt tồn kho</h2><fieldset :disabled="actionsDisabled || savingSettings"><legend class="sr-only">Cách quản lý tồn kho</legend><label v-for="mode in MODES" :key="mode.value"><input v-model="inventoryMode" type="radio" name="inventory-mode" :value="mode.value" /> {{ mode.label }}</label></fieldset><p v-if="settingsError" class="error-inline" role="alert">{{ settingsError }}</p><button ref="settingsSaveButton" type="button" class="btn btn-primary settings-save" :disabled="!settingsDirty || savingSettings || actionsDisabled" @click="requestSettingsSave">{{ savingSettings ? 'Đang lưu cách quản lý tồn...' : 'Lưu cách quản lý tồn' }}</button></section>

        <form class="panel editor-form" aria-label="Công thức" @submit.prevent="requestRecipeSave"><fieldset class="editor-fields" :disabled="actionsDisabled || savingRecipe">
          <p v-if="recipeError" class="error-inline" role="alert">{{ recipeError }}</p>
          <div v-if="form.active === false" class="inactive-note" role="status"><strong>Công thức đang tắt.</strong><label><input v-model="form.active" type="checkbox" /> Bật công thức khi lưu</label></div>
          <details class="advanced"><summary>Chi tiết công thức</summary><label><span class="form-label">Một mẻ làm được bao nhiêu phần?</span><input v-model="form.yieldQuantity" class="form-input" type="number" min="0.0001" step="0.0001" required /></label></details>
          <section aria-labelledby="ingredients-heading"><div class="section-heading"><div><h2 id="ingredients-heading">Nguyên liệu</h2><p class="hint">Lượng dùng cho một mẻ; hệ thống tự tính cho một phần.</p></div><button ref="pickerTrigger" type="button" class="btn btn-outline" :disabled="!items.length" @click="openPicker">Thêm nguyên liệu</button></div>
            <p v-if="!form.items.length" class="empty">Chưa có nguyên liệu.</p>
            <div class="recipe-table desktop-only"><table><thead><tr><th>Nguyên liệu</th><th>Lượng / phần</th><th>Khả dụng</th><th>Làm được</th><th>Giá tham chiếu</th><th>Chi phí / phần</th><th></th></tr></thead><tbody><tr v-for="(row, index) in ingredientRows" :key="row.inventoryItemId" :class="{ limiting: row.limiting }" :data-recipe-line="index"><th scope="row">{{ row.name }}<span v-if="row.limiting" class="badge">Giới hạn</span></th><td><label class="quantity-edit"><span class="sr-only">Lượng {{ row.name }} cho một mẻ</span><input v-model="lineFor(row).quantity" type="number" min="0.0001" step="0.0001" /> <span>{{ baseUnitLabel(row.baseUnit) }}</span></label><small>{{ formatInventoryQuantity(row.requiredPerServing, row.baseUnit) }}/phần</small></td><td>{{ formatInventoryQuantity(row.availableQuantity, row.baseUnit) }}</td><td>{{ row.availableServings ?? '—' }} phần</td><td>{{ formatReferenceCost(row.averageUnitCost, row.baseUnit) }}</td><td>{{ row.costAvailable ? money(row.costPerServing) : 'Chưa đủ dữ liệu' }}</td><td><button type="button" class="icon-button" :aria-label="`Xóa ${row.name}`" @click="removeLine(index)">Xóa</button></td></tr></tbody></table></div>
            <div class="mobile-only ingredient-cards"><article v-for="(row, index) in ingredientRows" :key="row.inventoryItemId" :class="{ limiting: row.limiting }" :data-recipe-line="index"><h3>{{ row.name }} <span v-if="row.limiting" class="badge">Giới hạn</span></h3><label class="quantity-edit"><span>Lượng cho một mẻ</span><input v-model="lineFor(row).quantity" type="number" min="0.0001" step="0.0001" /> {{ baseUnitLabel(row.baseUnit) }}</label><dl><div><dt>Mỗi phần</dt><dd>{{ formatInventoryQuantity(row.requiredPerServing, row.baseUnit) }}</dd></div><div><dt>Khả dụng</dt><dd>{{ formatInventoryQuantity(row.availableQuantity, row.baseUnit) }}</dd></div><div><dt>Làm được</dt><dd>{{ row.availableServings ?? '—' }} phần</dd></div><div><dt>Giá tham chiếu</dt><dd>{{ formatReferenceCost(row.averageUnitCost, row.baseUnit) }}</dd></div><div><dt>Chi phí / phần</dt><dd>{{ row.costAvailable ? money(row.costPerServing) : 'Chưa đủ dữ liệu' }}</dd></div></dl><button type="button" class="btn btn-outline" @click="removeLine(index)">Xóa {{ row.name }}</button></article></div>
            <p v-for="(_, index) in form.items" :key="`error-${index}`" v-show="lineErrorMessage(index)" class="field-error" role="alert">{{ lineErrorMessage(index) }}</p>
          </section>

          <section v-if="presentation" class="summary" aria-label="Năng lực và chi phí"><h2>Năng lực và chi phí</h2><p v-if="inventoryMode === 'INGREDIENT' && recipeDirty" class="estimate-label">Ước tính theo bản nháp công thức</p><dl><div><dt>Có thể làm</dt><dd>{{ presentation.availableServings ?? '—' }} phần</dd></div><template v-if="costComplete"><div><dt>Chi phí công thức / phần</dt><dd>{{ money(presentation.recipeCostPerServing) }}</dd></div><div><dt>Giá bán kích cỡ</dt><dd>{{ money(presentation.variantPrice ?? selectedVariant.price) }}</dd></div><div><dt>Food cost</dt><dd>{{ presentation.foodCostPercent?.toLocaleString('vi-VN') }}%</dd></div></template><div v-else-if="presentation.costStatus === 'INCOMPLETE'" class="incomplete"><dt>Chi phí</dt><dd><strong>Chưa đủ dữ liệu</strong><span>{{ presentation.missingCostItemCount }} nguyên liệu thiếu giá: {{ (presentation.missingCostItems || []).map(item => item.name).join(', ') || 'chưa xác định' }}</span></dd></div><div v-else><dt>Chi phí</dt><dd>Không áp dụng</dd></div></dl>
            <details v-if="summaryRows.length"><summary>Cách tính số phần</summary><ul><li v-for="row in summaryRows" :key="row.inventoryItemId">{{ row.name }}: {{ formatInventoryQuantity(row.availableQuantity, row.baseUnit) }} khả dụng / {{ formatInventoryQuantity(row.requiredPerServing, row.baseUnit) }} mỗi phần = {{ row.availableServings }} phần.<small> Hiện có {{ formatInventoryQuantity(row.onHandQuantity, row.baseUnit) }}, đã giữ {{ formatInventoryQuantity(row.reservedQuantity, row.baseUnit) }}.</small></li></ul><p>MIN({{ summaryRows.map(row => row.availableServings).join(', ') }}) = <strong>{{ presentation.availableServings }} phần</strong>.</p></details>
          </section>
          <p v-if="saveError" class="error-inline" role="alert">{{ saveError }}</p>
          <section v-if="capacity" class="summary" aria-label="Ảnh chụp năng lực đã lưu"><h2>Năng lực đã lưu</h2><p>{{ capacity.availableServings ?? '—' }} phần · {{ capacityRows.length }} mặt hàng</p></section>
          <div class="sticky-actions"><button type="button" class="btn btn-outline" :disabled="!recipeDirty || savingRecipe || actionsDisabled" @click="resetDraft">Hủy thay đổi</button><button ref="recipeSaveButton" type="submit" class="btn btn-primary" :disabled="!recipeDirty || savingRecipe || actionsDisabled">{{ savingRecipe ? 'Đang lưu công thức...' : 'Lưu công thức' }}</button></div></fieldset>
        </form>
      </template>
      <section v-else class="panel empty">Chọn món và kích cỡ để chỉnh công thức.</section>
    </template>

    <div v-if="pickerOpen" class="modal-backdrop"><section ref="pickerDialog" class="dialog" role="dialog" aria-modal="true" aria-labelledby="picker-title"><div class="dialog-heading"><h2 id="picker-title">{{ pickerItem ? `Số lượng ${pickerItem.name}` : 'Chọn nguyên liệu' }}</h2><button type="button" class="icon-button" aria-label="Đóng" @click="closePicker">Đóng</button></div><template v-if="!pickerItem"><label><span class="form-label">Tìm theo tên hoặc mã</span><input ref="pickerSearchInput" v-model="pickerSearch" class="form-input" type="search" /></label><p class="hint">Danh sách mặt hàng hiện có, sắp xếp theo tên.</p><ul class="picker-list"><li v-for="item in pickerItems" :key="item.inventoryItemId"><button type="button" @click="selectPickerItem(item)"><strong>{{ item.name }}</strong><span>{{ item.inventoryCode }} · {{ formatInventoryQuantity(item.availableQuantity, item.baseUnit) }}</span></button></li></ul><p v-if="!pickerItems.length" class="empty">Không còn mặt hàng phù hợp.</p></template><template v-else><p>Nhập lượng dùng cho một mẻ. Đơn vị gốc: <strong>{{ baseUnitLabel(pickerItem.baseUnit) }}</strong>.</p><label><span class="form-label">Số lượng ({{ baseUnitLabel(pickerItem.baseUnit) }})</span><input v-model="pickerQuantity" class="form-input" type="number" min="0.0001" step="0.0001" @keyup.enter="addPickerItem" /></label><div class="dialog-actions"><button type="button" class="btn btn-outline" @click="pickerItem = null">Quay lại</button><button type="button" class="btn btn-primary" @click="addPickerItem">Thêm</button></div></template></section></div>

    <ConfirmDialog :open="Boolean(confirmAction)" :busy="savePending" :title="confirmAction === 'reload' ? 'Tải lại dữ liệu mới?' : confirmAction === 'discard' ? 'Bỏ thay đổi chưa lưu?' : confirmAction === 'settings' ? 'Xác nhận cách quản lý tồn' : 'Xác nhận lưu công thức'" :message="confirmAction === 'reload' ? 'Bản nháp hiện tại sẽ bị bỏ và thay bằng dữ liệu mới nhất.' : confirmAction === 'discard' ? 'Các thay đổi công thức và cài đặt tồn kho chưa lưu sẽ bị bỏ.' : confirmAction === 'settings' ? 'Chỉ cách quản lý tồn của kích cỡ này sẽ được cập nhật.' : `Chỉ công thức gồm ${form.items.length} dòng nguyên liệu sẽ được lưu.`" :confirm-label="confirmAction === 'reload' ? 'Tải lại dữ liệu mới' : confirmAction === 'discard' ? 'Bỏ thay đổi' : confirmAction === 'settings' ? 'Xác nhận lưu cách quản lý tồn' : 'Xác nhận lưu công thức'" @confirm="confirmSave" @cancel="closeConfirm" />
  </main>
</template>

<style scoped>
.recipes-page{display:grid;gap:18px;padding-bottom:80px}.page-heading,.section-heading,.dialog-heading,.dialog-actions,.sticky-actions{display:flex;align-items:center;justify-content:space-between;gap:12px}.page-heading h1{margin:2px 0 4px;font-size:28px}.page-heading p,.hint{margin:0;color:var(--text-mid);font-size:13px}.eyebrow,.estimate-label{margin:0;color:var(--role-admin);font-size:11px;font-weight:800;letter-spacing:.1em;text-transform:uppercase}.panel{display:grid;gap:14px;padding:20px;border:1px solid var(--border-light);border-radius:14px;background:#fff}.panel h2{margin:0;font-size:17px}.selector-panel{grid-template-columns:minmax(180px,1fr) minmax(180px,1fr) minmax(180px,1fr)}label{display:grid;gap:6px}.form-label{font-size:12px;font-weight:700;color:var(--text-mid)}.settings-panel fieldset{display:flex;gap:10px 24px;flex-wrap:wrap;margin:0;padding:0;border:0}.settings-panel label{display:flex;align-items:center;min-height:40px;gap:8px}.advanced{padding:8px 12px;border:1px solid var(--border-light);border-radius:9px}.advanced summary,.summary summary{display:flex;align-items:center;min-height:40px;font-weight:700;cursor:pointer}.advanced label{padding:8px 0;max-width:360px}.inactive-note,.error-inline{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;border-radius:8px;background:#fff7ed;color:#9a3412}.error-inline{background:#fef2f2;color:#b91c1c}.recipe-table{overflow:hidden;border:1px solid var(--border-light);border-radius:10px}.recipe-table table{width:100%;border-collapse:collapse}.recipe-table th,.recipe-table td{padding:11px 9px;border-bottom:1px solid var(--border-light);text-align:left;vertical-align:top;font-size:13px}.recipe-table th{font-weight:700}.recipe-table thead th{background:var(--surface);color:var(--text-mid);font-size:11px;text-transform:uppercase}.recipe-table tr:last-child>*{border-bottom:0}.limiting{background:#fff7ed!important;box-shadow:inset 4px 0 #ea580c}.badge{display:inline-block;margin-left:6px;padding:2px 6px;border-radius:999px;background:#fed7aa;color:#9a3412;font-size:10px}.quantity-edit{display:flex;align-items:center;gap:5px}.quantity-edit input{width:90px;min-height:40px}.quantity-edit small,td small{display:block;margin-top:3px;color:var(--text-mid)}.summary{display:grid;gap:10px;padding:16px;border-radius:12px;background:var(--surface)}.summary h2{margin:0}.summary dl,.ingredient-cards dl{display:grid;gap:8px;margin:0}.summary dl>div,.ingredient-cards dl>div{display:flex;justify-content:space-between;gap:12px}.summary dt,.ingredient-cards dt{color:var(--text-mid)}.summary dd,.ingredient-cards dd{margin:0;font-weight:700}.summary .incomplete dd{display:grid;text-align:right}.summary ul{padding-left:22px}.summary li{margin:6px 0}.sticky-actions{position:sticky;bottom:12px;z-index:5;padding:12px;border:1px solid var(--border-light);border-radius:12px;background:rgba(255,255,255,.96);box-shadow:0 8px 28px rgba(15,23,42,.12)}.modal-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:16px;background:rgba(15,23,42,.55)}.dialog{display:grid;gap:14px;width:min(560px,100%);max-height:min(720px,90vh);overflow:auto;padding:20px;border-radius:14px;background:#fff}.dialog h2{margin:0}.picker-list{display:grid;gap:7px;margin:0;padding:0;list-style:none}.picker-list button{display:flex;align-items:center;justify-content:space-between;width:100%;min-height:48px;padding:9px 12px;border:1px solid var(--border-light);border-radius:8px;background:#fff;text-align:left}.picker-list span{color:var(--text-mid);font-size:12px}.empty{padding:20px;color:var(--text-mid);text-align:center}.icon-button{min-width:40px;min-height:40px;border:0;background:transparent;color:var(--primary);cursor:pointer}.mobile-only{display:none}.field-error{color:#b91c1c}.recipes-page :is(button,input,select,summary):focus-visible{outline:3px solid var(--primary);outline-offset:2px}.recipes-page :is(button,input,select){min-height:40px}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
@media(max-width:760px){.page-heading,.section-heading{align-items:flex-start;flex-direction:column}.selector-panel{grid-template-columns:1fr}.desktop-only{display:none}.mobile-only{display:grid}.ingredient-cards{gap:10px}.ingredient-cards article{display:grid;gap:12px;padding:14px;border:1px solid var(--border-light);border-radius:10px}.ingredient-cards h3{margin:0;font-size:16px}.ingredient-cards .quantity-edit{display:grid;grid-template-columns:1fr auto auto}.ingredient-cards .quantity-edit input{width:100%}.settings-panel fieldset{display:grid}.sticky-actions{bottom:8px}.sticky-actions .btn{flex:1}.dialog-actions .btn{flex:1}.recipes-page{min-width:0;overflow:hidden}}
</style>
