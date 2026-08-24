<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import { buildRecipePayload, formatQuantity, recipeServings, validateRecipeForm } from '@/utils/inventoryItem';
import { recipeCost } from '@/utils/inventoryOperations';

const toast = useToast();
const adminStore = useAdminStore();
const route = useRoute();

const MODES = [
  { value: 'INGREDIENT', label: 'Nguyên liệu (tính theo công thức)' },
  { value: 'FINISHED_GOOD', label: 'Thành phẩm (theo mặt hàng kho)' },
  { value: 'UNTRACKED', label: 'Không theo dõi' },
  { value: 'SUSPENDED', label: 'Tạm dừng bán' },
];
const AVAILABILITY_LABELS = {
  IN_STOCK: 'Còn hàng',
  OUT_OF_STOCK: 'Hết hàng',
  UNTRACKED: 'Không theo dõi',
  SUSPENDED: 'Tạm dừng',
};

const loading = ref(true);
const loadError = ref('');
const recipeError = ref('');
const variantId = ref('');
const saving = ref(false);
const saveError = ref('');
const form = ref({ inventoryMode: '', yieldQuantity: '', active: true, items: [] });
const availability = ref(null);
const recipe = ref(null);
const items = ref([]);
let generation = 0;

const variants = computed(() => adminStore.allProducts
  .flatMap((product) => (product.variants || []).map((variant) => ({
    variantId: variant.variantId,
    label: `${product.name} – ${variant.variantName || 'Mặc định'}`,
    price: variant.price,
  })))
  .filter((variant) => variant.variantId));
const itemsById = computed(() => new Map(items.value.map((item) => [item.inventoryItemId, item])));
const selectedVariant = computed(() => variants.value.find((variant) => String(variant.variantId) === String(variantId.value)) || null);
const hasRecipe = computed(() => Boolean(recipe.value));
const servingsInfo = computed(() => recipeServings(recipe.value, Object.fromEntries(items.value.map((item) => [item.inventoryItemId, item]))));
const limitingName = computed(() => (servingsInfo.value.limitingItemId ? itemsById.value.get(servingsInfo.value.limitingItemId)?.name || `#${servingsInfo.value.limitingItemId}` : ''));
const costInfo = computed(() => recipeCost(recipe.value, Object.fromEntries(items.value.map((item) => [item.inventoryItemId, item])), selectedVariant.value?.price));
const money = (value) => value == null ? '—' : `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 0 })} ₫`;

function blankForm() {
  return { inventoryMode: '', yieldQuantity: '', active: true, items: [] };
}

async function loadBaseData() {
  const request = ++generation;
  loading.value = true;
  loadError.value = '';
  try {
    const [itemList] = await Promise.all([adminApi.getInventoryItems(), adminStore.fetchProducts()]);
    if (request !== generation) return;
    items.value = Array.isArray(itemList) ? itemList : [];
  } catch (error) {
    if (request !== generation) return;
    loadError.value = error.message || 'Không thể tải dữ liệu kho';
    items.value = [];
  } finally {
    if (request === generation) loading.value = false;
  }
}

async function loadRecipe() {
  const request = ++generation;
  recipe.value = null;
  availability.value = null;
  recipeError.value = '';
  form.value = blankForm();
  if (!variantId.value) return;
  try {
    const data = await adminApi.getVariantRecipe(variantId.value);
    if (request !== generation) return;
    recipe.value = data;
    form.value = {
      inventoryMode: '',
      yieldQuantity: String(data.yieldQuantity ?? ''),
      active: Boolean(data.active),
      items: (data.items || []).map((line) => ({ inventoryItemId: String(line.inventoryItemId), quantity: String(line.quantity) })),
    };
  } catch (error) {
    if (request !== generation) return;
    if (error.status === 404) return;
    recipeError.value = error.message || 'Không thể tải công thức hiện tại';
  }
  try {
    availability.value = await adminApi.getVariantAvailability(variantId.value);
  } catch {
    if (request === generation) availability.value = null;
  }
}

function addLine() {
  form.value.items.push({ inventoryItemId: '', quantity: '' });
}

function removeLine(index) {
  form.value.items.splice(index, 1);
}

function lineErrorMessage(index) {
  return validateRecipeForm(form.value).lines?.[index] || '';
}

async function submitRecipe() {
  saveError.value = '';
  const errors = validateRecipeForm(form.value);
  if (Object.keys(errors).length) {
    saveError.value = errors.inventoryMode || errors.yieldQuantity || errors.items || 'Kiểm tra lại các dòng nguyên liệu';
    return;
  }
  saving.value = true;
  try {
    await adminApi.replaceVariantRecipe(variantId.value, buildRecipePayload(form.value));
    toast.success('Đã lưu công thức');
    await Promise.all([loadRecipe(), loadBaseData()]);
  } catch (error) {
    saveError.value = error.message || 'Không thể lưu công thức';
  } finally {
    saving.value = false;
  }
}

watch(variantId, loadRecipe);
onMounted(async () => {
  await loadBaseData();
  const requested = Number(route.query.variantId);
  if (Number.isSafeInteger(requested) && requested > 0 && variants.value.some((variant) => variant.variantId === requested)) {
    variantId.value = String(requested);
  }
});
</script>

<template>
  <main class="recipes-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">Vận hành</p>
        <h1>Công thức định lượng</h1>
        <p>Chọn kích cỡ và thiết lập chế độ kho cùng định lượng nguyên liệu cho từng xuất món.</p>
      </div>
      <button class="btn btn-outline" :disabled="loading" @click="loadBaseData"><i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Làm mới</button>
    </header>

    <section v-if="loadError" class="state error" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="loadBaseData">Thử lại</button></section>

    <template v-else>
      <section class="panel">
        <label class="variant-select">
          <span class="form-label">Kích cỡ</span>
          <select v-model="variantId" class="form-select">
            <option value="">— Chọn kích cỡ —</option>
            <option v-for="variant in variants" :key="variant.variantId" :value="String(variant.variantId)">{{ variant.label }}</option>
          </select>
        </label>
        <p v-if="!variants.length" class="hint">Chưa có kích cỡ nào. Thêm kích cỡ trong trang Sản phẩm trước.</p>
      </section>

      <template v-if="selectedVariant">
        <section class="panel availability-panel" aria-label="Trạng thái khả dụng">
          <h2>{{ selectedVariant.label }}</h2>
          <p v-if="recipeError" role="alert">{{ recipeError }}</p>
          <ul class="availability-facts">
            <li><span class="fact-label">Trạng thái khách thấy</span><strong v-if="availability">{{ AVAILABILITY_LABELS[availability.availabilityStatus] || availability.availabilityStatus }}<template v-if="availability.availabilityStatus === 'LOW_STOCK' && availability.remainingServings != null"> (chỉ còn {{ availability.remainingServings }} phần)</template></strong><span v-else>—</span></li>
            <li><span class="fact-label">Số phần khả dụng theo công thức</span><strong v-if="hasRecipe && servingsInfo.servings !== null">{{ servingsInfo.servings }}</strong><span v-else>—</span></li>
            <li><span class="fact-label">Giới hạn bởi</span><strong v-if="limitingName">{{ limitingName }}</strong><span v-else>—</span></li>
            <li><span class="fact-label">Giá vốn ước tính / phần</span><strong>{{ money(costInfo.cost) }}</strong></li>
            <li v-if="costInfo.foodCostPercent !== null"><span class="fact-label">Food cost hiện tại</span><strong>{{ costInfo.foodCostPercent.toLocaleString('vi-VN') }}%</strong></li>
          </ul>
        </section>

        <form class="panel editor-form" aria-label="Công thức" @submit.prevent="submitRecipe">
          <fieldset class="mode-fieldset">
            <legend>Chế độ kho</legend>
            <p class="hint">Chế độ hiện tại không được API trả về — hãy chọn lại chế độ mong muốn trước khi lưu.</p>
            <label class="form-group" for="recipe-mode"><span class="form-label">Chế độ</span></label>
            <select id="recipe-mode" v-model="form.inventoryMode" class="form-select" required>
              <option value="" disabled>— Chọn chế độ kho —</option>
              <option v-for="mode in MODES" :key="mode.value" :value="mode.value">{{ mode.label }}</option>
            </select>
          </fieldset>

          <div class="yield-row">
            <label class="form-group" for="recipe-yield"><span class="form-label">Số phần đầu ra (yield)</span></label>
            <input id="recipe-yield" v-model="form.yieldQuantity" class="form-input" type="number" min="0.0001" step="0.0001" inputmode="decimal" required />
            <label class="checkbox-field"><input v-model="form.active" type="checkbox" /> Công thức đang hoạt động</label>
          </div>

          <fieldset class="lines-fieldset">
            <legend>Định lượng nguyên liệu</legend>
            <p v-if="!items.length" class="hint">Chưa có mặt hàng kho. Thêm mặt hàng trong Tổng quan kho trước.</p>
            <div v-for="(line, index) in form.items" :key="index" class="line-row">
              <select v-model="line.inventoryItemId" class="form-select" :aria-label="`Mặt hàng dòng ${index + 1}`" :aria-invalid="Boolean(lineErrorMessage(index))">
                <option value="">— Chọn mặt hàng —</option>
                <option v-for="item in items" :key="item.inventoryItemId" :value="String(item.inventoryItemId)">#{{ item.inventoryItemId }} {{ item.name }} (còn {{ formatQuantity(item.availableQuantity) }})</option>
              </select>
              <input v-model="line.quantity" class="form-input" type="number" min="0.0001" step="0.0001" inputmode="decimal" placeholder="Định lượng / yield" :aria-label="`Định lượng dòng ${index + 1}`" :aria-invalid="Boolean(lineErrorMessage(index))" />
              <button type="button" class="btn btn-sm btn-outline" :aria-label="`Bỏ dòng ${index + 1}`" @click="removeLine(index)">Bỏ</button>
              <span v-if="lineErrorMessage(index)" class="field-error" role="alert">{{ lineErrorMessage(index) }}</span>
            </div>
            <button type="button" class="btn btn-outline btn-sm" :disabled="!items.length" @click="addLine">+ Thêm dòng</button>
          </fieldset>

          <p v-if="saveError" class="error-inline" role="alert">{{ saveError }}</p>
          <div class="actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu công thức' }}</button>
          </div>
        </form>
      </template>
      <section v-else class="panel"><p class="hint">Chọn một kích cỡ để xem và chỉnh công thức.</p></section>
    </template>
  </main>
</template>

<style scoped>
.recipes-page { display: grid; gap: 20px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-heading h1 { font-size: 28px; line-height: 1.25; margin: 2px 0 4px; }
.page-heading p { color: var(--text-mid); font-size: 14px; margin: 0; }
.eyebrow { color: var(--role-admin); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.panel { background: #fff; border: 1px solid var(--border-light); border-radius: 14px; padding: 20px; display: grid; gap: 12px; }
.variant-select { display: grid; gap: 6px; max-width: 480px; }
.form-label { font-size: 12px; font-weight: 700; color: var(--text-mid); }
.hint { color: var(--text-mid); font-size: 13px; margin: 0; }
.availability-panel h2 { margin: 0; font-size: 17px; }
.availability-panel p[role='alert'] { margin: 0; color: #b91c1c; font-size: 13px; }
.availability-facts { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; }
.availability-facts li { display: flex; justify-content: space-between; gap: 16px; font-size: 14px; border-bottom: 1px dashed var(--border-light); padding-bottom: 6px; }
.fact-label { color: var(--text-mid); }
.mode-fieldset, .lines-fieldset { border: 1px solid var(--border-light); border-radius: 10px; padding: 14px; display: grid; gap: 10px; margin: 0; }
legend { font-weight: 800; font-size: 13px; padding: 0 6px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.yield-row { display: flex; align-items: end; gap: 16px; flex-wrap: wrap; }
.yield-row .form-group { min-width: 200px; }
.checkbox-field { display: flex; align-items: center; gap: 8px; min-height: 42px; cursor: pointer; }
.line-row { display: grid; grid-template-columns: minmax(0, 2fr) minmax(120px, 1fr) auto; gap: 10px; align-items: center; }
.field-error { grid-column: 1 / -1; color: #b91c1c; font-size: 12px; }
.actions { display: flex; justify-content: flex-end; }
.state.error { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 32px; color: #b91c1c; text-align: center; }
.error-inline { margin: 0; padding: 10px 12px; border-radius: 8px; color: #b91c1c; background: #fef2f2; font-size: 13px; }
@media (max-width: 700px) { .page-heading { align-items: flex-start; flex-direction: column; } .line-row { grid-template-columns: 1fr auto; } .line-row .form-select { grid-column: 1 / -1; } .availability-facts li { flex-direction: column; gap: 2px; } }
</style>
