<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { cloneProductState, isValidProductId, sectionDirty, validateComboItem } from '@/utils/adminProductEditor';

const props = defineProps({
  modelValue: { type: Object, required: true },
  busy: Boolean,
  baselineVersion: { type: Number, required: true },
  productId: { type: Number, default: null },
});
const emit = defineEmits(['update:modelValue', 'reload', 'dirty-change']);
const adminStore = useAdminStore();
const catalogState = ref('idle');
const catalogError = ref('');
const newItem = ref({ variantId: null, quantity: 1 });
const itemErrors = ref({});
const message = ref('');
const mutating = ref(false);
const snapshot = ref(cloneProductState(props.modelValue.combo));
let generation = 0;
let stopped = false;

const combo = computed(() => props.modelValue.combo);
const locked = computed(() => !isValidProductId(props.productId));
const choices = computed(() => adminStore.allProducts.flatMap((product) => (product.variants || []).map((variant) => ({ ...variant, label: `${product.name} - ${variant.variantName}`, productName: product.name }))));

watch(() => props.modelValue.combo, (value) => {
  emit('dirty-change', sectionDirty(snapshot.value, value ?? null));
}, { deep: true });

watch(() => props.baselineVersion, () => {
  snapshot.value = cloneProductState(props.modelValue.combo);
  itemErrors.value = {};
  message.value = '';
  emit('dirty-change', false);
});

onMounted(() => { if (catalogState.value === 'idle') loadChoices(); });
watch(() => props.busy, (busy) => { if (!busy && catalogState.value === 'idle') loadChoices(); });

onBeforeUnmount(() => { stopped = true; generation += 1; });

function currentRequest(request) {
  return !stopped && request.generation === generation;
}

async function loadChoices() {
  if (props.busy || mutating.value) return;
  const request = { generation: ++generation };
  catalogState.value = 'loading';
  catalogError.value = '';
  try {
    await adminStore.fetchProducts();
    if (currentRequest(request)) catalogState.value = 'ready';
  } catch (error) {
    if (currentRequest(request)) {
      catalogState.value = 'error';
      catalogError.value = error.message || 'Không thể tải danh mục biến thể';
    }
  }
}

async function mutate(action) {
  if (props.busy || mutating.value || locked.value) return false;
  const request = { generation: ++generation };
  mutating.value = true;
  message.value = '';
  try {
    await action();
    if (currentRequest(request)) emit('reload');
    return currentRequest(request);
  } catch (error) {
    if (currentRequest(request)) message.value = error.message || 'Không thể lưu combo';
    return false;
  } finally {
    if (currentRequest(request)) mutating.value = false;
  }
}

async function toggleCombo() {
  const nextActive = !(combo.value?.isActive ?? false);
  await mutate(() => adminApi.saveCombo(props.productId, { isActive: nextActive }));
}

async function addItem() {
  itemErrors.value = validateComboItem(newItem.value);
  if (Object.keys(itemErrors.value).length) return;
  const ok = await mutate(async () => {
    if (!combo.value) await adminApi.saveCombo(props.productId, { isActive: true });
    await adminApi.createComboItem(props.productId, { variantId: Number(newItem.value.variantId), quantity: Number(newItem.value.quantity) });
  });
  if (ok) newItem.value = { variantId: null, quantity: 1 };
}

async function removeItem(item) {
  await mutate(() => adminApi.deleteComboItem(props.productId, item.comboItemId));
}

function itemLabel(item) {
  return choices.find((choice) => choice.variantId === item.variantId)?.label || `Biến thể #${item.variantId}`;
}
</script>

<template>
  <section class="editor-card" aria-labelledby="combo-title">
    <div class="heading"><h2 id="combo-title">Combo cố định</h2><button v-if="!locked && catalogState === 'ready'" class="btn btn-outline" type="button" :disabled="busy || mutating" @click="toggleCombo">{{ combo?.isActive ? 'Tắt combo' : 'Bật combo' }}</button></div>
    <p v-if="locked" class="hint" role="alert">Tạo sản phẩm trước khi cấu hình combo.</p>
    <template v-else-if="catalogState === 'loading'"><p class="hint" role="status">Đang tải danh mục biến thể...</p></template>
    <template v-else-if="catalogState === 'error'"><p class="hint error" role="alert">{{ catalogError }}</p><button class="btn btn-outline" type="button" :disabled="busy || mutating" @click="loadChoices">Thử lại</button></template>
    <template v-else>
      <p v-if="message" class="message" role="alert">{{ message }}</p>
      <div class="combo-block">
        <h3>Thêm thành phần</h3>
        <div class="inline-form">
          <div class="field grow">
            <label for="combo-variant">Biến thể</label>
            <select id="combo-variant" v-model="newItem.variantId" :disabled="busy || mutating" :aria-invalid="Boolean(itemErrors.variantId)" :aria-describedby="itemErrors.variantId ? 'combo-variant-error' : undefined"><option :value="null">Chọn biến thể</option><option v-for="choice in choices" :key="choice.variantId" :value="choice.variantId">{{ choice.label }}</option></select>
            <span v-if="itemErrors.variantId" id="combo-variant-error" role="alert">{{ itemErrors.variantId }}</span>
          </div>
          <div class="field">
            <label for="combo-quantity">Số lượng</label>
            <input id="combo-quantity" v-model.number="newItem.quantity" type="number" min="1" :disabled="busy || mutating" :aria-invalid="Boolean(itemErrors.quantity)" :aria-describedby="itemErrors.quantity ? 'combo-quantity-error' : undefined" />
            <span v-if="itemErrors.quantity" id="combo-quantity-error" role="alert">{{ itemErrors.quantity }}</span>
          </div>
          <button class="btn btn-outline" type="button" :disabled="busy || mutating" @click="addItem">Thêm</button>
        </div>
      </div>
      <div v-if="!combo?.items?.length" class="hint">Chưa có thành phần combo. Bật combo và thêm biến thể.</div>
      <ul v-else class="item-list" aria-label="Thành phần combo">
        <li v-for="item in combo.items" :key="item.comboItemId">
          <span class="item-label">{{ itemLabel(item) }}</span>
          <span class="item-qty">x{{ item.quantity }}</span>
          <button class="btn btn-sm btn-outline" type="button" :disabled="busy || mutating" :aria-label="`Xóa ${itemLabel(item)} khỏi combo`" @click="removeItem(item)">Xóa</button>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.editor-card{display:grid;gap:16px;padding:24px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff}.heading{display:flex;align-items:center;justify-content:space-between}.heading h2{margin:0}.hint{color:var(--text-mid);font-size:13px}.hint.error{color:#b91c1c}.message{margin:0;color:#b91c1c;font-size:12px}.combo-block{display:grid;gap:12px;padding:16px;border:1px solid rgba(23,23,23,.08);border-radius:14px}.combo-block h3{margin:0;font-size:13px;color:var(--text-mid)}.inline-form{display:flex;align-items:end;gap:12px;flex-wrap:wrap}.inline-form .grow{flex:1;min-width:200px}.field{display:grid;gap:6px}.field label{font-size:12px;font-weight:700}.field input,.field select{min-height:42px;padding:9px 11px;border:1px solid #ddd;border-radius:9px;background:#fff}.field [role=alert]{color:#b91c1c;font-size:12px}.item-list{display:grid;gap:8px;margin:0;padding:0;list-style:none}.item-list li{display:flex;align-items:center;gap:10px;padding:10px 12px;border:1px solid rgba(23,23,23,.08);border-radius:10px}.item-label{flex:1;font-size:13px}.item-qty{font-weight:700}@media(max-width:600px){.heading{flex-direction:column;align-items:start;gap:10px}.inline-form{flex-direction:column;align-items:stretch}}
</style>
