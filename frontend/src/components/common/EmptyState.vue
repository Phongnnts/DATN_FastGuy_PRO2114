<script setup>
defineProps({
  icon: { type: String, default: 'bi-inbox' },
  title: { type: String, default: 'Không có dữ liệu' },
  description: { type: String, default: '' },
});
</script>

<template>
  <div class="empty-state">
    <i :class="'bi ' + icon"></i>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
