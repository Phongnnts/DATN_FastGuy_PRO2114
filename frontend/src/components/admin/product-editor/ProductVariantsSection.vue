<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { createVariantDraft, isValidProductId, sectionDirty, validateVariant, variantPayload } from '@/utils/adminProductEditor';

const props = defineProps({
  modelValue: { type: Object, required: true },
  busy: Boolean,
  baselineVersion: { type: Number, required: true },
  productId: { type: Number, default: null },
  mode: { type: String, default: 'edit' },
  pending: { type: Array, default: () => [] },
});
const emit = defineEmits(['update:modelValue', 'update:pending', 'save', 'reload', 'retry-pending', 'dirty-change']);
const rows = ref([]);
const snapshot = ref([]);
const pendingRows = ref([]);
const errors = ref({});
const mutating = ref(false);
let uid = 0;
let generation = 0;
let stopped = false;

const isCreate = computed(() => props.mode === 'create');
const locked = computed(() => !isValidProductId(props.productId));

function withUid(list) {
  return (list || []).map((item) => ({ ...item, _uid: item._uid ?? ++uid }));
}

function variantShape(row) {
  const { _uid, ...rest } = row;
  return rest;
}

function syncRows() {
  rows.value = withUid(props.modelValue.variants || []);
  snapshot.value = (props.modelValue.variants || []).map((variant) => ({ ...variant }));
}

function rowsDirty() {
  return sectionDirty(snapshot.value, rows.value.map(variantShape));
}

function emitDirty() {
  emit('dirty-change', pendingRows.value.length > 0 || rowsDirty());
}

function commit() {
  emit('update:modelValue', { ...props.modelValue, variants: rows.value.map(variantShape) });
  emitDirty();
}

function emitPending() {
  emit('update:pending', pendingRows.value.map((item) => ({ ...item })));
  emitDirty();
}

syncRows();
pendingRows.value = withUid(props.pending);

watch(() => props.baselineVersion, () => {
  syncRows();
  pendingRows.value = withUid(props.pending);
  errors.value = {};
  emitDirty();
});
watch(() => props.pending, (list) => {
  pendingRows.value = withUid(list);
  emitDirty();
});
watch(() => props.mode, () => {
  errors.value = {};
  emitDirty();
});

onMounted(() => { if (pendingRows.value.length) emit('dirty-change', true); });
onBeforeUnmount(() => { stopped = true; generation += 1; });

function currentRequest(request) {
  return !stopped && request.generation === generation;
}

function rowIndex(row) {
  return rows.value.indexOf(row);
}

function updateRow(row, field, value) {
  row[field] = value;
  commit();
}

function addRow() {
  rows.value.push(withUid([createVariantDraft()])[0]);
  errors.value = {};
  commit();
}

function setDefault(row) {
  rows.value.forEach((other) => { other.isDefault = other === row; });
  commit();
}

function saveAll() {
  const allErrors = {};
  let hasError = false;
  rows.value.forEach((row, index) => {
    const found = validateVariant(row);
    if (Object.keys(found).length) { allErrors[index] = found; hasError = true; }
  });
  errors.value = allErrors;
  if (hasError) return;
  emit('save');
}

async function saveRow(row) {
  if (props.busy || mutating.value || locked.value) return;
  const found = validateVariant(row);
  if (Object.keys(found).length) {
    errors.value = { ...errors.value, [rowIndex(row)]: found };
    return;
  }
  const request = { generation: ++generation };
  mutating.value = true;
  try {
    if (row.variantId) {
      await adminApi.updateVariant(row.variantId, variantPayload(row, { includeStock: false }));
    } else {
      const created = await adminApi.createVariant(props.productId, variantPayload(row));
      if (currentRequest(request) && created) row.variantId = created.variantId ?? created.id;
    }
    if (currentRequest(request)) {
      const next = { ...errors.value };
      delete next[rowIndex(row)];
      errors.value = next;
      emit('reload');
    }
  } catch (error) {
    if (currentRequest(request)) errors.value = { ...errors.value, [rowIndex(row)]: { _server: error.message || 'Không thể lưu biến thể' } };
  } finally {
    if (currentRequest(request)) mutating.value = false;
  }
}

async function deleteRow(row) {
  if (props.busy || mutating.value) return;
  if (!row.variantId) {
    rows.value.splice(rowIndex(row), 1);
    errors.value = {};
    commit();
    return;
  }
  if (locked.value) return;
  const request = { generation: ++generation };
  mutating.value = true;
  try {
    await adminApi.deleteVariant(row.variantId);
    if (currentRequest(request)) {
      rows.value.splice(rowIndex(row), 1);
      errors.value = {};
      commit();
      emit('reload');
    }
  } catch (error) {
    if (currentRequest(request)) errors.value = { ...errors.value, [rowIndex(row)]: { _server: error.message || 'Không thể xóa biến thể' } };
  } finally {
    if (currentRequest(request)) mutating.value = false;
  }
}

function updatePending(pending, field, value) {
  pending[field] = value;
  emitPending();
}

function removePending(pending) {
  if (props.busy || mutating.value) return;
  pendingRows.value = pendingRows.value.filter((item) => item._uid !== pending._uid);
  emitPending();
}

function retryPending() {
  if (props.busy || mutating.value) return;
  emit('retry-pending');
}
</script>

<template>
  <section class="editor-card" aria-labelledby="variants-title">
    <div class="heading"><h2 id="variants-title">Biến thể</h2><button class="btn btn-outline" type="button" :disabled="busy || mutating" @click="addRow">+ Thêm biến thể</button></div>
    <p v-if="isCreate" class="hint">Biến thể sẽ được tạo cùng sản phẩm khi lưu.</p>
    <p v-else-if="locked" class="hint">Tạo sản phẩm trước khi thêm biến thể.</p>
    <aside v-if="pendingRows.length" class="pending-banner" role="alert">
      <strong>{{ pendingRows.length }} biến thể chưa lưu được</strong>
      <p class="hint">Chỉnh sửa lại dữ liệu rồi bấm Thử lại.</p>
      <div v-for="pending in pendingRows" :key="pending._uid" class="pending-row">
        <input :value="pending.variantName" :disabled="busy || mutating" aria-label="Tên biến thể chưa lưu" @input="updatePending(pending, 'variantName', $event.target.value)" placeholder="Tên biến thể" />
        <input :value="pending.price" type="number" min="0" :disabled="busy || mutating" aria-label="Giá biến thể chưa lưu" @input="updatePending(pending, 'price', $event.target.value === '' ? '' : Number($event.target.value))" />
        <input :value="pending.quantityAvailable ?? ''" type="number" min="0" :disabled="busy || mutating" aria-label="Tồn kho biến thể chưa lưu" @input="updatePending(pending, 'quantityAvailable', $event.target.value === '' ? null : Number($event.target.value))" placeholder="Trống = không giới hạn" />
        <select :value="pending.status" :disabled="busy || mutating" aria-label="Trạng thái biến thể chưa lưu" @change="updatePending(pending, 'status', $event.target.value)"><option value="AVAILABLE">Còn bán</option><option value="UNAVAILABLE">Ngừng bán</option></select>
        <label class="checkbox-field"><input type="checkbox" :checked="pending.isDefault" :disabled="busy || mutating" @change="updatePending(pending, 'isDefault', $event.target.checked)" /> Mặc định</label>
        <button class="btn btn-sm btn-outline" type="button" :disabled="busy || mutating" :aria-label="`Bỏ biến thể chưa lưu ${pending.variantName || 'chưa đặt tên'}`" @click="removePending(pending)">Bỏ</button>
      </div>
      <button class="btn btn-sm btn-primary" type="button" :disabled="busy || mutating" @click="retryPending">{{ mutating ? 'Đang lưu...' : 'Thử lại' }}</button>
    </aside>
    <p v-if="!rows.length && !pendingRows.length" class="hint">Chưa có biến thể nào. Thêm ít nhất một biến thể nếu sản phẩm có nhiều phân loại.</p>
    <div v-for="(row, index) in rows" :key="row._uid" class="variant-row">
      <div class="field">
        <label :for="`variant-name-${index}`">Tên biến thể</label>
        <input :id="`variant-name-${index}`" :value="row.variantName" :disabled="busy || mutating" :aria-invalid="Boolean(errors[index]?.variantName)" :aria-describedby="errors[index]?.variantName ? `variant-name-error-${index}` : undefined" @input="updateRow(row, 'variantName', $event.target.value)" placeholder="Vd: Size L" />
        <span v-if="errors[index]?.variantName" :id="`variant-name-error-${index}`" role="alert">{{ errors[index].variantName }}</span>
      </div>
      <div class="field">
        <label :for="`variant-price-${index}`">Giá</label>
        <input :id="`variant-price-${index}`" type="number" min="0" :value="row.price" :disabled="busy || mutating" :aria-invalid="Boolean(errors[index]?.price)" :aria-describedby="errors[index]?.price ? `variant-price-error-${index}` : undefined" @input="updateRow(row, 'price', $event.target.value === '' ? '' : Number($event.target.value))" />
        <span v-if="errors[index]?.price" :id="`variant-price-error-${index}`" role="alert">{{ errors[index].price }}</span>
      </div>
      <div class="field">
        <label :for="`variant-original-${index}`">Giá gốc</label>
        <input :id="`variant-original-${index}`" type="number" min="0" :value="row.originalPrice ?? ''" :disabled="busy || mutating || Boolean(row.variantId)" :aria-invalid="Boolean(errors[index]?.originalPrice)" :aria-describedby="errors[index]?.originalPrice ? `variant-original-error-${index}` : undefined" @input="updateRow(row, 'originalPrice', $event.target.value === '' ? null : Number($event.target.value))" :title="row.variantId ? 'Giá gốc không thay đổi sau khi lưu' : undefined" />
        <span v-if="errors[index]?.originalPrice" :id="`variant-original-error-${index}`" role="alert">{{ errors[index].originalPrice }}</span>
      </div>
      <div class="field">
        <label :for="`variant-sku-${index}`">SKU</label>
        <input :id="`variant-sku-${index}`" :value="row.sku" :disabled="busy || mutating" @input="updateRow(row, 'sku', $event.target.value)" placeholder="Mã hàng (tùy chọn)" />
      </div>
      <div class="field">
        <label :for="`variant-qty-${index}`">Tồn kho</label>
        <input :id="`variant-qty-${index}`" type="number" min="0" :value="row.quantityAvailable ?? ''" :disabled="busy || mutating || Boolean(row.variantId)" :aria-invalid="Boolean(errors[index]?.quantityAvailable)" :aria-describedby="errors[index]?.quantityAvailable ? `variant-qty-error-${index}` : undefined" @input="updateRow(row, 'quantityAvailable', $event.target.value === '' ? null : Number($event.target.value))" placeholder="Trống = không giới hạn" />
        <span v-if="errors[index]?.quantityAvailable" :id="`variant-qty-error-${index}`" role="alert">{{ errors[index].quantityAvailable }}</span>
      </div>
      <div class="field">
        <label :for="`variant-status-${index}`">Trạng thái</label>
        <select :id="`variant-status-${index}`" :value="row.status" :disabled="busy || mutating" @change="updateRow(row, 'status', $event.target.value)"><option value="AVAILABLE">Còn bán</option><option value="UNAVAILABLE">Ngừng bán</option></select>
      </div>
      <div class="field">
        <label class="checkbox-field"><input type="checkbox" :checked="row.isDefault" :disabled="busy || mutating" @change="setDefault(row)" /> Mặc định</label>
      </div>
      <div class="row-actions">
        <button v-if="!isCreate" class="btn btn-sm btn-primary" type="button" :disabled="busy || mutating" @click="saveRow(row)">{{ row.variantId ? 'Lưu' : 'Tạo' }}</button>
        <button class="btn btn-sm btn-outline" type="button" :disabled="busy || mutating" :aria-label="`Xóa biến thể ${row.variantName || index + 1}`" @click="deleteRow(row)">Xóa</button>
      </div>
      <p v-if="errors[index]?._server" class="server-error" role="alert">{{ errors[index]._server }}</p>
    </div>
    <div v-if="isCreate" class="actions"><button class="btn btn-primary" type="button" :disabled="busy || mutating" @click="saveAll">{{ busy ? 'Đang lưu...' : 'Lưu biến thể' }}</button></div>
  </section>
</template>

<style scoped>
.editor-card{display:grid;gap:16px;padding:24px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff}.heading{display:flex;align-items:center;justify-content:space-between}.heading h2{margin:0}.hint{color:var(--text-mid);font-size:13px}.variant-row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;padding:16px;border:1px solid rgba(23,23,23,.08);border-radius:14px}.field{display:grid;gap:6px}.field label{font-size:12px;font-weight:700}.field input,.field select{min-height:42px;padding:9px 11px;border:1px solid #ddd;border-radius:9px;background:#fff}.field [role=alert]{color:#b91c1c;font-size:12px}.checkbox-field{display:flex;align-items:center;gap:7px;min-height:42px;cursor:pointer}.row-actions{display:flex;align-items:end;gap:8px}.server-error{grid-column:1/-1;margin:0;color:#b91c1c;font-size:12px}.pending-banner{display:grid;gap:10px;padding:14px;border-radius:12px;color:#92400e;background:#fffbeb;border:1px solid #fde68a}.pending-row{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.pending-row input,.pending-row select{padding:7px 9px;border:1px solid #fde68a;border-radius:8px;background:#fff}.pending-row input[type=number],.pending-row input:first-child{min-width:120px}.actions{display:flex;justify-content:flex-end}@media(max-width:900px){.variant-row{grid-template-columns:repeat(2,minmax(0,1fr))}.row-actions{grid-column:1/-1;align-items:center}}@media(max-width:600px){.variant-row{grid-template-columns:1fr}.heading{flex-direction:column;align-items:start;gap:10px}}
</style>
