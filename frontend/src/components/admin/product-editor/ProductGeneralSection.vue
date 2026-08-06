<script setup>
import { ref, watch } from 'vue';
import { cloneProductState, sectionDirty, validateGeneral } from '@/utils/adminProductEditor';

const props = defineProps({
  modelValue: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
  busy: Boolean,
  baselineVersion: { type: Number, required: true },
  externalErrors: { type: Object, default: () => ({}) },
});
const emit = defineEmits(['update:modelValue', 'save', 'dirty-change']);
const errors = ref({});
const snapshot = ref(cloneProductState(props.modelValue));

watch(() => props.baselineVersion, () => {
  snapshot.value = cloneProductState(props.modelValue);
  errors.value = {};
  emit('dirty-change', false);
});
watch(() => props.externalErrors, (value) => { errors.value = value; }, { deep: true });
watch(() => props.modelValue, (value) => {
  emit('dirty-change', sectionDirty(snapshot.value, value));
}, { deep: true });

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value });
}

function submit() {
  errors.value = validateGeneral(props.modelValue);
  if (Object.keys(errors.value).length) return;
  emit('save');
}

function focusFirstError() {
  const field = Object.keys(errors.value)[0];
  if (field) document.getElementById(`product-${field === 'categoryId' ? 'category' : field === 'basePrice' ? 'price' : field}`)?.focus();
}

defineExpose({ focusFirstError });
</script>

<template>
  <form class="editor-card" novalidate @submit.prevent="submit">
    <div class="field">
      <label for="product-name">Tên sản phẩm</label>
      <input id="product-name" :value="modelValue.name" :disabled="busy" :aria-invalid="Boolean(errors.name)" :aria-describedby="errors.name ? 'product-name-error' : undefined" @input="update('name', $event.target.value)" />
      <span v-if="errors.name" id="product-name-error" role="alert">{{ errors.name }}</span>
    </div>
    <div class="field">
      <label for="product-category">Danh mục</label>
      <select id="product-category" :value="modelValue.categoryId ?? ''" :disabled="busy" :aria-invalid="Boolean(errors.categoryId)" :aria-describedby="errors.categoryId ? 'product-category-error' : undefined" @change="update('categoryId', $event.target.value ? Number($event.target.value) : null)">
        <option value="">Chọn danh mục</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
      <span v-if="errors.categoryId" id="product-category-error" role="alert">{{ errors.categoryId }}</span>
    </div>
    <div class="field">
      <label for="product-price">Giá gốc</label>
      <input id="product-price" type="number" min="0" :value="modelValue.basePrice" :disabled="busy" :aria-invalid="Boolean(errors.basePrice)" :aria-describedby="errors.basePrice ? 'product-price-error' : undefined" @input="update('basePrice', $event.target.value === '' ? '' : Number($event.target.value))" />
      <span v-if="errors.basePrice" id="product-price-error" role="alert">{{ errors.basePrice }}</span>
    </div>
    <div class="field wide">
      <label for="product-description">Mô tả</label>
      <textarea id="product-description" :value="modelValue.description" :disabled="busy" @input="update('description', $event.target.value)"></textarea>
    </div>
    <div class="field">
      <label for="product-status">Trạng thái</label>
      <select id="product-status" :value="modelValue.status" :disabled="busy" @change="update('status', $event.target.value)"><option value="AVAILABLE">Đang bán</option><option value="UNAVAILABLE">Ngừng bán</option></select>
    </div>
    <div class="field">
      <label for="product-from">Bắt đầu bán</label>
      <input id="product-from" type="time" :value="modelValue.availableFrom" :disabled="busy" :aria-invalid="Boolean(errors.availableFrom)" :aria-describedby="errors.availableFrom ? 'product-from-error' : undefined" @input="update('availableFrom', $event.target.value)" />
      <span v-if="errors.availableFrom" id="product-from-error" role="alert">{{ errors.availableFrom }}</span>
    </div>
    <div class="field">
      <label for="product-to">Kết thúc bán</label>
      <input id="product-to" type="time" :value="modelValue.availableTo" :disabled="busy" :aria-invalid="Boolean(errors.availableTo)" :aria-describedby="errors.availableTo ? 'product-to-error' : undefined" @input="update('availableTo', $event.target.value)" />
      <span v-if="errors.availableTo" id="product-to-error" role="alert">{{ errors.availableTo }}</span>
    </div>
    <div class="actions"><button class="btn btn-primary" type="submit" :disabled="busy">{{ busy ? 'Đang lưu...' : 'Lưu thông tin' }}</button></div>
  </form>
</template>

<style scoped>
.editor-card{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px;padding:24px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff}.field{display:grid;gap:7px}.field label{font-size:12px;font-weight:700}.field input,.field select,.field textarea{min-height:44px;padding:10px 12px;border:1px solid #ddd;border-radius:10px;background:#fff}.field textarea{min-height:110px;resize:vertical}.field [role=alert]{color:#b91c1c;font-size:12px}.wide,.actions{grid-column:1/-1}.actions{display:flex;justify-content:flex-end}@media(max-width:700px){.editor-card{grid-template-columns:1fr}.wide,.actions{grid-column:auto}}
</style>
