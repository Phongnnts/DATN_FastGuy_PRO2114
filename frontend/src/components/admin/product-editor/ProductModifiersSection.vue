<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { cloneProductState, isValidProductId, sectionDirty, validateModifierGroup, validateModifierOption } from '@/utils/adminProductEditor';

const props = defineProps({
  modelValue: { type: Object, required: true },
  busy: Boolean,
  baselineVersion: { type: Number, required: true },
  productId: { type: Number, default: null },
});
const emit = defineEmits(['update:modelValue', 'reload', 'dirty-change']);
const groupForm = ref({ name: '', minSelections: 0, maxSelections: 1 });
const optionForm = ref({ groupId: null, name: '', price: 0 });
const groupFormErrors = ref({});
const optionFormErrors = ref({});
const groupErrors = ref({});
const optionErrors = ref({});
const message = ref('');
const mutating = ref(false);
const snapshot = ref(cloneProductState(props.modelValue.modifierGroups || []));
let generation = 0;
let stopped = false;

const groups = computed(() => props.modelValue.modifierGroups || []);
const locked = computed(() => !isValidProductId(props.productId));

watch(() => props.modelValue.modifierGroups, (value) => {
  emit('dirty-change', sectionDirty(snapshot.value, value || []));
}, { deep: true });

watch(() => props.baselineVersion, () => {
  snapshot.value = cloneProductState(props.modelValue.modifierGroups || []);
  groupFormErrors.value = {};
  optionFormErrors.value = {};
  groupErrors.value = {};
  optionErrors.value = {};
  message.value = '';
  emit('dirty-change', false);
});

onBeforeUnmount(() => { stopped = true; generation += 1; });

function currentRequest(request) {
  return !stopped && request.generation === generation;
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
    if (currentRequest(request)) message.value = error.message || 'Không thể lưu tùy chọn';
    return false;
  } finally {
    if (currentRequest(request)) mutating.value = false;
  }
}

async function saveGroup() {
  const errors = validateModifierGroup(groupForm.value);
  groupFormErrors.value = errors;
  if (Object.keys(errors).length) return;
  const created = await mutate(() => adminApi.createModifierGroup(props.productId, { name: groupForm.value.name, minSelections: Number(groupForm.value.minSelections), maxSelections: Number(groupForm.value.maxSelections) }));
  if (created) groupForm.value = { name: '', minSelections: 0, maxSelections: 1 };
}

async function updateGroup(group) {
  const errors = validateModifierGroup(group);
  groupErrors.value = { ...groupErrors.value, [group.modifierGroupId]: errors };
  if (Object.keys(errors).length) return;
  await mutate(() => adminApi.updateModifierGroup(group.modifierGroupId, { name: group.name, minSelections: Number(group.minSelections), maxSelections: Number(group.maxSelections), isActive: group.isActive }));
}

async function deleteGroup(group) {
  await mutate(() => adminApi.deleteModifierGroup(group.modifierGroupId));
}

async function saveOption() {
  const errors = validateModifierOption(optionForm.value);
  optionFormErrors.value = errors;
  if (Object.keys(errors).length) return;
  const created = await mutate(() => adminApi.createModifierOption(optionForm.value.groupId, { name: optionForm.value.name, price: Number(optionForm.value.price) }));
  if (created) optionForm.value = { groupId: null, name: '', price: 0 };
}

async function updateOption(group, option) {
  const errors = validateModifierOption(option);
  optionErrors.value = { ...optionErrors.value, [option.modifierOptionId]: errors };
  if (Object.keys(errors).length) return;
  await mutate(() => adminApi.updateModifierOption(group.modifierGroupId, option.modifierOptionId, { name: option.name, price: Number(option.price), isActive: option.isActive }));
}

async function deleteOption(group, option) {
  await mutate(() => adminApi.deleteModifierOption(group.modifierGroupId, option.modifierOptionId));
}
</script>

<template>
  <section class="editor-card" aria-labelledby="modifiers-title">
    <h2 id="modifiers-title">Topping</h2>
    <p v-if="locked" class="hint" role="alert">Tạo sản phẩm trước khi cấu hình topping.</p>
    <template v-else>
      <p v-if="message" class="message" role="alert">{{ message }}</p>
      <div class="group-block">
        <h3>Thêm nhóm topping</h3>
        <div class="inline-form">
          <div class="field">
            <label for="modifier-group-name">Tên nhóm</label>
            <input id="modifier-group-name" v-model="groupForm.name" :disabled="busy || mutating" :aria-invalid="Boolean(groupFormErrors.name)" :aria-describedby="groupFormErrors.name ? 'modifier-group-name-error' : undefined" />
            <span v-if="groupFormErrors.name" id="modifier-group-name-error" role="alert">{{ groupFormErrors.name }}</span>
          </div>
          <div class="field">
            <label for="modifier-group-min">Tối thiểu</label>
            <input id="modifier-group-min" v-model.number="groupForm.minSelections" type="number" min="0" :disabled="busy || mutating" :aria-invalid="Boolean(groupFormErrors.minSelections)" :aria-describedby="groupFormErrors.minSelections ? 'modifier-group-min-error' : undefined" />
            <span v-if="groupFormErrors.minSelections" id="modifier-group-min-error" role="alert">{{ groupFormErrors.minSelections }}</span>
          </div>
          <div class="field">
            <label for="modifier-group-max">Tối đa</label>
            <input id="modifier-group-max" v-model.number="groupForm.maxSelections" type="number" min="0" :disabled="busy || mutating" :aria-invalid="Boolean(groupFormErrors.maxSelections)" :aria-describedby="groupFormErrors.maxSelections ? 'modifier-group-max-error' : undefined" />
            <span v-if="groupFormErrors.maxSelections" id="modifier-group-max-error" role="alert">{{ groupFormErrors.maxSelections }}</span>
          </div>
          <button class="btn btn-outline" type="button" :disabled="busy || mutating" @click="saveGroup">Thêm nhóm</button>
        </div>
      </div>
      <div v-if="!groups.length" class="hint">Chưa có nhóm topping nào.</div>
      <div v-for="group in groups" :key="group.modifierGroupId" class="group-block">
        <div class="inline-form">
          <div class="field grow">
            <label :for="`group-${group.modifierGroupId}-name`">Tên nhóm</label>
            <input :id="`group-${group.modifierGroupId}-name`" v-model="group.name" :disabled="busy || mutating" :aria-invalid="Boolean(groupErrors[group.modifierGroupId]?.name)" />
          </div>
          <div class="field">
            <label :for="`group-${group.modifierGroupId}-min`">Tối thiểu</label>
            <input :id="`group-${group.modifierGroupId}-min`" v-model.number="group.minSelections" type="number" min="0" :disabled="busy || mutating" :aria-invalid="Boolean(groupErrors[group.modifierGroupId]?.minSelections)" />
          </div>
          <div class="field">
            <label :for="`group-${group.modifierGroupId}-max`">Tối đa</label>
            <input :id="`group-${group.modifierGroupId}-max`" v-model.number="group.maxSelections" type="number" min="0" :disabled="busy || mutating" :aria-invalid="Boolean(groupErrors[group.modifierGroupId]?.maxSelections)" />
          </div>
          <label class="check-field"><input type="checkbox" v-model="group.isActive" :disabled="busy || mutating" /> Bật</label>
          <div class="row-actions">
            <button class="btn btn-sm btn-primary" type="button" :disabled="busy || mutating" @click="updateGroup(group)">Lưu</button>
            <button class="btn btn-sm btn-outline" type="button" :disabled="busy || mutating" :aria-label="`Xóa nhóm ${group.name}`" @click="deleteGroup(group)">Xóa</button>
          </div>
        </div>
        <div v-if="groupErrors[group.modifierGroupId]?.name || groupErrors[group.modifierGroupId]?.minSelections || groupErrors[group.modifierGroupId]?.maxSelections" class="message" role="alert">{{ groupErrors[group.modifierGroupId]?.name || groupErrors[group.modifierGroupId]?.minSelections || groupErrors[group.modifierGroupId]?.maxSelections }}</div>
        <ul v-if="group.options?.length" class="option-list">
          <li v-for="option in group.options" :key="option.modifierOptionId">
            <input v-model="option.name" :disabled="busy || mutating" :aria-label="`Tên tùy chọn của ${group.name}`" :aria-invalid="Boolean(optionErrors[option.modifierOptionId]?.name)" />
            <input v-model.number="option.price" type="number" min="0" :disabled="busy || mutating" :aria-label="`Giá tùy chọn của ${group.name}`" :aria-invalid="Boolean(optionErrors[option.modifierOptionId]?.price)" />
            <label class="check-field"><input type="checkbox" v-model="option.isActive" :disabled="busy || mutating" /> Bật</label>
            <button class="btn btn-sm btn-primary" type="button" :disabled="busy || mutating" @click="updateOption(group, option)">Lưu</button>
            <button class="btn btn-sm btn-outline" type="button" :disabled="busy || mutating" :aria-label="`Xóa tùy chọn ${option.name}`" @click="deleteOption(group, option)">Xóa</button>
            <span v-if="optionErrors[option.modifierOptionId]?.name || optionErrors[option.modifierOptionId]?.price" class="option-error" role="alert">{{ optionErrors[option.modifierOptionId]?.name || optionErrors[option.modifierOptionId]?.price }}</span>
          </li>
        </ul>
      </div>
      <div v-if="groups.length" class="group-block">
        <h3>Thêm tùy chọn</h3>
        <div class="inline-form">
          <div class="field">
            <label for="modifier-option-group">Nhóm</label>
            <select id="modifier-option-group" v-model="optionForm.groupId" :disabled="busy || mutating"><option :value="null">Chọn nhóm</option><option v-for="group in groups" :key="group.modifierGroupId" :value="group.modifierGroupId">{{ group.name }}</option></select>
          </div>
          <div class="field">
            <label for="modifier-option-name">Tên tùy chọn</label>
            <input id="modifier-option-name" v-model="optionForm.name" :disabled="busy || mutating" :aria-invalid="Boolean(optionFormErrors.name)" :aria-describedby="optionFormErrors.name ? 'modifier-option-name-error' : undefined" />
            <span v-if="optionFormErrors.name" id="modifier-option-name-error" role="alert">{{ optionFormErrors.name }}</span>
          </div>
          <div class="field">
            <label for="modifier-option-price">Giá</label>
            <input id="modifier-option-price" v-model.number="optionForm.price" type="number" min="0" :disabled="busy || mutating" :aria-invalid="Boolean(optionFormErrors.price)" :aria-describedby="optionFormErrors.price ? 'modifier-option-price-error' : undefined" />
            <span v-if="optionFormErrors.price" id="modifier-option-price-error" role="alert">{{ optionFormErrors.price }}</span>
          </div>
          <button class="btn btn-outline" type="button" :disabled="busy || mutating" @click="saveOption">Thêm tùy chọn</button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.editor-card{display:grid;gap:18px;padding:24px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff}.editor-card h2{margin:0}.editor-card h3{margin:0;font-size:13px;color:var(--text-mid)}.hint{color:var(--text-mid);font-size:13px}.message{grid-column:1/-1;margin:0;color:#b91c1c;font-size:12px}.group-block{display:grid;gap:12px;padding:16px;border:1px solid rgba(23,23,23,.08);border-radius:14px}.inline-form{display:flex;align-items:end;flex-wrap:wrap;gap:12px}.inline-form .grow{flex:1;min-width:160px}.field{display:grid;gap:6px;min-width:110px}.field label{font-size:12px;font-weight:700}.field input,.field select{min-height:42px;padding:9px 11px;border:1px solid #ddd;border-radius:9px;background:#fff}.field [role=alert]{color:#b91c1c;font-size:12px}.check-field{display:flex;align-items:center;gap:6px;min-height:42px;white-space:nowrap;cursor:pointer}.row-actions{display:flex;gap:8px;align-items:center}.option-list{display:grid;gap:8px;margin:0;padding:0;list-style:none}.option-list li{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.option-list input{padding:8px 10px;border:1px solid #ddd;border-radius:8px}.option-error{margin:0;color:#b91c1c;font-size:12px}@media(max-width:600px){.inline-form{flex-direction:column;align-items:stretch}.row-actions{justify-content:flex-end}}
</style>
