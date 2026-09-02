<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import { buildProductPayload, cloneProductState, createProductDraft, isCurrentEditorRequest, isValidProductId, nextEnabledSectionIndex, normalizeProductDetail, normalizeProductScope, validateGeneral, variantPayload, withProductSlice } from '@/utils/adminProductEditor';
import ProductGeneralSection from '@/components/admin/product-editor/ProductGeneralSection.vue';
import ProductMediaSection from '@/components/admin/product-editor/ProductMediaSection.vue';
import ProductVariantsSection from '@/components/admin/product-editor/ProductVariantsSection.vue';
import ProductModifiersSection from '@/components/admin/product-editor/ProductModifiersSection.vue';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const toast = useToast();
const draft = ref(createProductDraft());
const baseline = ref(cloneProductState(draft.value));
const baselineVersion = ref(0);
const activeSection = ref('general');
const loading = ref(false);
const saving = ref(false);
const loadState = ref('ready');
const loadMessage = ref('');
const reloadMessage = ref('');
const categoryState = ref('loading');
const categoryMessage = ref('');
const generalErrors = ref({});
const generalSection = ref(null);
const tabRefs = ref([]);
const pendingVariants = ref([]);
const partialCreateMessage = ref('');
let requestGeneration = 0;
let mutationGeneration = 0;
let stopped = false;

const isCreateMode = computed(() => route.name === 'AdminProductCreate');
const productId = computed(() => isCreateMode.value ? null : Number(route.params.id));
const editorRouteKey = computed(() => `${String(route.name)}:${String(route.params.id ?? '')}`);
const sections = computed(() => [
  { id: 'general', label: 'Thông tin chung' },
  { id: 'media', label: 'Hình ảnh' },
  { id: 'variants', label: 'Kích cỡ' },
  { id: 'modifiers', label: 'Topping' },
].map((section) => ({ ...section, disabled: isCreateMode.value && section.id === 'modifiers' })));
const dirtySections = ref({ general: false, media: false, variants: false, modifiers: false });
const confirmDialogOpen = ref(false);
let pendingNavigation = null;
let suppressLeaveGuard = false;

function hasDirtySections() {
  return Object.values(dirtySections.value).some(Boolean);
}

function guardDirtyNavigation(to, from, next) {
  if (suppressLeaveGuard) return next();
  if (!hasDirtySections()) return next();
  pendingNavigation = next;
  confirmDialogOpen.value = true;
}

onBeforeRouteLeave(guardDirtyNavigation);
onBeforeRouteUpdate(guardDirtyNavigation);

function confirmLeave() {
  const proceed = pendingNavigation;
  pendingNavigation = null;
  confirmDialogOpen.value = false;
  if (proceed) proceed();
}

function cancelLeave() {
  pendingNavigation = null;
  confirmDialogOpen.value = false;
}

function handleBeforeUnload(event) {
  if (!hasDirtySections()) return;
  event.preventDefault();
  event.returnValue = '';
}

function setSectionDirty(section, value) {
  if (!(section in dirtySections.value)) return;
  dirtySections.value[section] = Boolean(value);
}

function acceptBaseline(scope) {
  if (!scope) {
    baseline.value = cloneProductState(draft.value);
    baselineVersion.value += 1;
    dirtySections.value = { general: false, media: false, variants: false, modifiers: false };
    return;
  }
  baseline.value = withProductSlice(baseline.value, draft.value, scope);
  baselineVersion.value += 1;
  scope.forEach((id) => { dirtySections.value[id] = false; });
}

function loadAccepted(request) {
  return isCurrentEditorRequest(request, requestGeneration, editorRouteKey.value, stopped);
}

function mutationAccepted(request) {
  return isCurrentEditorRequest(request, mutationGeneration, editorRouteKey.value, stopped);
}

async function loadCategories() {
  categoryState.value = 'loading';
  categoryMessage.value = '';
  try {
    await adminStore.fetchCategories();
    if (stopped) return;
    categoryState.value = 'ready';
  } catch (error) {
    if (stopped) return;
    categoryState.value = 'error';
    categoryMessage.value = error.message || 'Không thể tải danh mục';
  }
}

async function loadProduct() {
  mutationGeneration += 1;
  const request = { generation: ++requestGeneration, routeKey: editorRouteKey.value };
  loadMessage.value = '';
  reloadMessage.value = '';
  generalErrors.value = {};
  activeSection.value = 'general';
  if (isCreateMode.value) {
    draft.value = createProductDraft();
    loadState.value = 'ready';
    acceptBaseline();
    return;
  }
  if (!isValidProductId(route.params.id)) {
    loadState.value = 'invalid';
    return;
  }
  loading.value = true;
  loadState.value = 'loading';
  try {
    const detail = await adminApi.getProduct(productId.value);
    if (!loadAccepted(request)) return;
    if (!detail) {
      loadState.value = 'not-found';
      return;
    }
    draft.value = normalizeProductDetail(detail);
    loadState.value = 'ready';
    acceptBaseline();
  } catch (error) {
    if (!loadAccepted(request)) return;
    const status = error.response?.status;
    loadState.value = status === 404 ? 'not-found' : 'error';
    loadMessage.value = error.message || 'Không thể tải sản phẩm';
  } finally {
    if (loadAccepted(request)) loading.value = false;
  }
}

function applyCanonicalSlice(detail, scope) {
  draft.value = withProductSlice(draft.value, detail, scope);
  acceptBaseline(scope);
}

async function reloadAfterSave(mutationRequest, scope) {
  if (!mutationAccepted(mutationRequest)) return;
  const request = { generation: ++mutationGeneration, routeKey: editorRouteKey.value };
  try {
    const detail = await adminApi.getProduct(productId.value);
    if (!mutationAccepted(request)) return;
    if (!detail) return;
    applyCanonicalSlice(normalizeProductDetail(detail), normalizeProductScope(scope));
  } catch (error) {
    if (mutationAccepted(request)) reloadMessage.value = error.message || 'Đã lưu nhưng không thể tải lại dữ liệu';
  }
}

function payloadFromDraft() {
  return buildProductPayload(draft.value);
}

async function saveProduct() {
  if (saving.value) return;
  const errors = validateGeneral(draft.value);
  generalErrors.value = errors;
  if (Object.keys(errors).length) {
    activeSection.value = 'general';
    await nextTick(() => generalSection.value?.focusFirstError());
    return;
  }
  const request = { generation: ++mutationGeneration, routeKey: editorRouteKey.value };
  saving.value = true;
  try {
    const payload = payloadFromDraft();
    if (isCreateMode.value) {
      const createdId = await createProductWithVariants(request);
      if (createdId === null) return;
      acceptBaseline();
      toast.success('Đã tạo sản phẩm');
      if (!mutationAccepted(request)) return;
      await router.replace({ name: 'AdminProductEdit', params: { id: createdId } });
    } else {
      await adminApi.updateProduct(productId.value, payload);
      if (!mutationAccepted(request)) return;
      acceptBaseline(['general', 'media']);
      toast.success('Đã lưu sản phẩm');
      reloadMessage.value = '';
      saving.value = false;
      reloadAfterSave(request, ['general', 'media']);
    }
  } catch (error) {
    if (!mutationAccepted(request)) return;
    toast.error(error.message || 'Không thể lưu sản phẩm');
  } finally {
    if (mutationAccepted(request)) saving.value = false;
  }
}

function selectSection(section) {
  if (section.disabled) return;
  activeSection.value = section.id;
  document.getElementById(`product-section-${section.id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function reloadFromSection(scope) {
  reloadAfterSave({ generation: mutationGeneration, routeKey: editorRouteKey.value }, scope);
}

async function handlePartialCreate({ productId: createdId, failed }) {
  pendingVariants.value = failed.map((variant) => ({ ...variant }));
  partialCreateMessage.value = `Đã tạo sản phẩm nhưng ${failed.length} kích cỡ chưa lưu được`;
  suppressLeaveGuard = true;
  try {
    await router.replace({ name: 'AdminProductEdit', params: { id: createdId } });
  } finally {
    suppressLeaveGuard = false;
  }
}

async function createProductWithVariants(request) {
  let createdId = isValidProductId(draft.value.id) ? Number(draft.value.id) : null;
  if (createdId === null) {
    const created = await adminApi.createProduct(payloadFromDraft());
    if (!mutationAccepted(request)) return null;
    createdId = created?.productId ?? created?.id;
    if (!isValidProductId(createdId)) throw new Error('API không trả về mã sản phẩm hợp lệ');
    draft.value.id = createdId;
  }
  const failed = [];
  for (const variant of draft.value.variants) {
    if (variant.variantId) continue;
    try {
      const created = await adminApi.createVariant(createdId, variantPayload(variant));
      if (created) variant.variantId = created.variantId ?? created.id;
    } catch (error) {
      if (!mutationAccepted(request)) return null;
      failed.push({ ...variant, message: error.message || 'Không thể tạo kích cỡ' });
    }
  }
  if (!mutationAccepted(request)) return null;
  if (failed.length) {
    await handlePartialCreate({ productId: createdId, failed });
    return null;
  }
  return createdId;
}

async function saveVariantsSection() {
  if (saving.value || !isCreateMode.value) return;
  const errors = validateGeneral(draft.value);
  generalErrors.value = errors;
  if (Object.keys(errors).length) {
    activeSection.value = 'general';
    await nextTick(() => generalSection.value?.focusFirstError());
    return;
  }
  const request = { generation: ++mutationGeneration, routeKey: editorRouteKey.value };
  saving.value = true;
  try {
    const createdId = await createProductWithVariants(request);
    if (createdId === null) return;
    acceptBaseline();
    toast.success('Đã tạo sản phẩm');
    if (!mutationAccepted(request)) return;
    await router.replace({ name: 'AdminProductEdit', params: { id: createdId } });
  } catch (error) {
    if (!mutationAccepted(request)) return;
    toast.error(error.message || 'Không thể tạo sản phẩm');
  } finally {
    if (mutationAccepted(request)) saving.value = false;
  }
}

async function retryPendingVariants() {
  if (saving.value || isCreateMode.value || !isValidProductId(productId.value)) return;
  const request = { generation: ++mutationGeneration, routeKey: editorRouteKey.value };
  saving.value = true;
  try {
    const remaining = [];
    for (const variant of pendingVariants.value) {
      try {
        await adminApi.createVariant(productId.value, variantPayload(variant));
      } catch (error) {
        if (!mutationAccepted(request)) return;
        remaining.push({ ...variant, message: error.message || 'Không thể tạo kích cỡ' });
      }
    }
    if (!mutationAccepted(request)) return;
    if (remaining.length) {
      pendingVariants.value = remaining;
      partialCreateMessage.value = `${remaining.length} kích cỡ chưa lưu được`;
    } else {
      pendingVariants.value = [];
      partialCreateMessage.value = '';
      saving.value = false;
      reloadAfterSave(request, ['variants']);
    }
  } catch (error) {
    if (!mutationAccepted(request)) return;
    toast.error(error.message || 'Không thể tạo kích cỡ');
  } finally {
    if (mutationAccepted(request)) saving.value = false;
  }
}

function handleTabKeydown(event, index) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  const nextIndex = nextEnabledSectionIndex(sections.value, index, event.key);
  activeSection.value = sections.value[nextIndex].id;
  nextTick(() => tabRefs.value[nextIndex]?.focus());
}

async function initialize() {
  await Promise.all([loadCategories(), loadProduct()]);
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload);
  initialize();
});
watch(() => [route.name, route.params.id], () => {
  mutationGeneration += 1;
  saving.value = false;
  loadProduct();
});
watch(pendingVariants, (list) => {
  if (!list.length) partialCreateMessage.value = '';
});
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
  stopped = true;
  requestGeneration += 1;
  mutationGeneration += 1;
});
</script>

<template>
  <main class="editor-page" :inert="confirmDialogOpen ? '' : undefined">
    <nav class="breadcrumbs" aria-label="Đường dẫn"><RouterLink :to="{ name: 'AdminProducts' }">Sản phẩm</RouterLink><span aria-hidden="true">/</span><span aria-current="page">{{ isCreateMode ? 'Thêm mới' : 'Chỉnh sửa' }}</span></nav>
    <header class="editor-header"><div><span class="eyebrow">PRODUCT EDITOR</span><h1>{{ isCreateMode ? 'Thêm sản phẩm' : draft.name || 'Chỉnh sửa sản phẩm' }}</h1><p>{{ isCreateMode ? 'Tạo thông tin cơ bản trước khi cấu hình topping.' : `Mã sản phẩm #${productId}` }}</p></div><RouterLink class="btn btn-outline" :to="{ name: 'AdminProducts' }">Quay lại danh sách</RouterLink></header>
    <aside v-if="reloadMessage" class="reload-banner" role="alert">{{ reloadMessage }} <button class="btn btn-sm btn-outline" type="button" @click="loadProduct">Đồng bộ lại</button></aside>
    <aside v-if="partialCreateMessage" class="reload-banner" role="alert">{{ partialCreateMessage }}</aside>
    <section v-if="loadState === 'loading' || loading || categoryState === 'loading'" class="state" role="status">Đang tải sản phẩm...</section>
    <section v-else-if="categoryState === 'error'" class="state state-error" role="alert"><h2>Không thể tải danh mục</h2><p>{{ categoryMessage }}</p><button class="btn btn-outline" type="button" @click="loadCategories">Thử lại</button></section>
    <section v-else-if="loadState === 'invalid'" class="state state-error" role="alert"><h2>Mã sản phẩm không hợp lệ</h2><RouterLink class="btn btn-outline" :to="{ name: 'AdminProducts' }">Về danh sách</RouterLink></section>
    <section v-else-if="loadState === 'not-found'" class="state state-error" role="alert"><h2>Không tìm thấy sản phẩm</h2><RouterLink class="btn btn-outline" :to="{ name: 'AdminProducts' }">Về danh sách sản phẩm</RouterLink></section>
    <section v-else-if="loadState === 'error'" class="state state-error" role="alert"><h2>Không thể tải sản phẩm</h2><p>{{ loadMessage }}</p><button class="btn btn-outline" type="button" @click="loadProduct">Thử lại</button></section>
    <template v-else-if="loadState === 'ready' && categoryState === 'ready'">
      <nav class="section-tabs" aria-label="Phần chỉnh sửa sản phẩm">
        <button v-for="(section, index) in sections" :id="`product-nav-${section.id}`" :key="section.id" :ref="(element) => { tabRefs[index] = element; }" type="button" :aria-current="activeSection === section.id ? 'location' : undefined" :aria-controls="`product-section-${section.id}`" :tabindex="activeSection === section.id ? 0 : -1" :disabled="section.disabled" @click="selectSection(section)" @keydown="handleTabKeydown($event, index)">{{ section.label }}<span v-if="dirtySections[section.id]" class="dirty" aria-label="Có thay đổi">•</span></button>
      </nav>
      <div class="editor-layout">
        <aside class="save-context" aria-label="Trạng thái lưu">
          <span class="eyebrow">TIẾN ĐỘ</span>
          <strong>{{ hasDirtySections() ? 'Có thay đổi chưa lưu' : 'Mọi thay đổi đã lưu' }}</strong>
          <p>{{ isCreateMode ? 'Lưu sản phẩm để mở cấu hình topping.' : 'Thông tin và hình ảnh dùng chung một lần lưu.' }}</p>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="saveProduct">{{ saving ? 'Đang lưu...' : isCreateMode ? 'Tạo sản phẩm' : 'Lưu thay đổi' }}</button>
        </aside>
        <div class="editor-workflow">
          <div id="product-section-general"><ProductGeneralSection ref="generalSection" v-model="draft" :categories="adminStore.allCategories" :busy="saving" :baseline-version="baselineVersion" :external-errors="generalErrors" @save="saveProduct" @dirty-change="setSectionDirty('general', $event)" /></div>
          <div id="product-section-media"><ProductMediaSection v-model="draft" :busy="saving" :baseline-version="baselineVersion" @save="saveProduct" @dirty-change="setSectionDirty('media', $event)" /></div>
          <div id="product-section-variants"><ProductVariantsSection v-model="draft" :busy="saving" :baseline-version="baselineVersion" :product-id="productId" :mode="isCreateMode ? 'create' : 'edit'" :pending="pendingVariants" @save="saveVariantsSection" @retry-pending="retryPendingVariants" @reload="reloadFromSection('variants')" @dirty-change="setSectionDirty('variants', $event)" @update:pending="pendingVariants = $event" /></div>
          <div id="product-section-modifiers"><ProductModifiersSection v-model="draft" :busy="saving" :baseline-version="baselineVersion" :product-id="productId" @reload="reloadFromSection('modifiers')" @dirty-change="setSectionDirty('modifiers', $event)" /></div>
          <div class="mobile-final-save"><button class="btn btn-primary" type="button" :disabled="saving" @click="saveProduct">{{ saving ? 'Đang lưu...' : isCreateMode ? 'Tạo sản phẩm' : 'Lưu thay đổi' }}</button></div>
        </div>
      </div>
    </template>
    <ConfirmDialog :open="confirmDialogOpen" title="Rời khỏi trang?" message="Bạn có thay đổi chưa lưu. Hãy hủy để tiếp tục chỉnh sửa hoặc xác nhận rời khỏi." confirm-label="Rời khỏi" @confirm="confirmLeave" @cancel="cancelLeave" />
  </main>
</template>

<style scoped>
.editor-page{display:grid;gap:18px;color:var(--text-dark)}.breadcrumbs{display:flex;gap:8px;align-items:center;font-size:12px}.breadcrumbs a{color:var(--primary)}.editor-header{display:flex;align-items:end;justify-content:space-between;gap:24px;padding:28px;border-radius:24px;color:#fff;background:linear-gradient(125deg,#1b1714,#3f2b21)}.editor-header h1{margin:7px 0;font-size:clamp(28px,4vw,42px)}.editor-header p{margin:0;color:rgba(255,255,255,.65)}.eyebrow{color:var(--route-amber);font-size:10px;font-weight:800;letter-spacing:.15em}.section-tabs{display:flex;gap:8px;overflow-x:auto;padding:6px;border-radius:15px;background:#eee8e3}.section-tabs button{min-height:44px;padding:10px 15px;white-space:nowrap;border:0;border-radius:10px;background:transparent;font-weight:700}.section-tabs button[aria-current=location]{color:#fff;background:#34251e}.section-tabs button:disabled{opacity:.45}.dirty{margin-left:5px;color:var(--route-amber)}.state,.placeholder{display:grid;gap:12px;place-items:center;min-height:240px;padding:30px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff;text-align:center}.reload-banner{display:flex;align-items:center;justify-content:center;gap:12px;min-height:48px;padding:10px 16px;border-radius:12px;color:#92400e;background:#fffbeb;border:1px solid #fde68a}.state-error h2,.placeholder h2{margin:0}.editor-layout{display:grid;grid-template-columns:minmax(0,1fr) 240px;gap:18px;align-items:start}.editor-workflow{display:grid;gap:18px;grid-column:1;grid-row:1}.save-context{display:grid;gap:12px;grid-column:2;grid-row:1;padding:20px;border:1px solid rgba(23,23,23,.08);border-radius:16px;background:#fff}.save-context strong{font-size:16px}.save-context p{margin:0;color:var(--text-mid);font-size:13px;line-height:1.55}.mobile-final-save{display:none}@media(min-width:901px){.save-context{position:sticky;top:18px}}@media(max-width:900px){.editor-layout{grid-template-columns:1fr}.editor-workflow{grid-column:1}.save-context{display:none}.mobile-final-save{display:flex;justify-content:stretch;padding-bottom:8px}.mobile-final-save .btn{width:100%;min-height:48px}}@media(max-width:700px){.editor-header{align-items:start;flex-direction:column}.editor-header .btn{width:100%}.section-tabs{margin-inline:-4px}.editor-workflow{gap:14px}.editor-page :deep(.editor-card){padding:18px;border-radius:16px}.editor-page :deep(.actions){position:static;padding:0;background:transparent;box-shadow:none}.editor-page :deep(.actions .btn){width:100%;min-height:46px}}
</style>
