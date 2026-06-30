<script setup>
import { ref, watch } from 'vue';
const props = defineProps({ message: String, type: { type: String, default: 'success' } });
const visible = ref(false);
watch(() => props.message, (val) => {
  if (val) { visible.value = true; setTimeout(() => { visible.value = false; }, 3000); }
});
</script>
<template>
  <teleport to="body">
    <div v-if="visible" :class="['toast', 'toast-' + type]">
      <i :class="type === 'success' ? 'bi-check-circle' : 'bi-exclamation-circle'"></i>
      {{ message }}
    </div>
  </teleport>
</template>
