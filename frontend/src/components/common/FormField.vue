<script setup>
import { computed, useSlots } from 'vue';

const props = defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  required: { type: Boolean, default: false },
  error: { type: String, default: '' },
});

const slots = useSlots();
const errorId = computed(() => `${props.id}-error`);
const controlAttrs = computed(() => ({
  id: props.id,
  required: props.required,
  'aria-invalid': props.error ? 'true' : 'false',
  'aria-describedby': props.error ? errorId.value : undefined,
  class: { 'field-control-error': Boolean(props.error) },
}));
</script>

<template>
  <div class="form-field">
    <label class="form-label" :for="id">{{ label }}<span v-if="required" class="required-marker" aria-hidden="true"> *</span></label>
    <slot v-if="slots.default" :control-attrs="controlAttrs" />
    <p v-if="error" :id="errorId" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.form-field { min-width: 0; margin-bottom: 16px; }
.required-marker, .field-error { color: #ef4444; }
.field-error { margin: 4px 0 0; font-size: 13px; }
:deep(.field-control-error) { border-color: #ef4444; }
:deep(.field-control-error:focus) { border-color: #ef4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, .14); }
</style>
