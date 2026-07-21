<script setup>
defineProps({
  icon: { type: String, default: 'bi-inbox' },
  title: { type: String, default: 'Không có dữ liệu' },
  description: { type: String, default: '' },
});
</script>

<template>
  <section class="empty-state" role="status" aria-live="polite">
    <i :class="'bi ' + icon" aria-hidden="true"></i>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot />
  </section>
</template>
