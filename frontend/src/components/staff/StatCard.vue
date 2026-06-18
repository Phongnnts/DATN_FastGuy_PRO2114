<script setup>
defineProps({
  icon: String,
  label: String,
  value: [String, Number],
});
</script>

<template>
  <div class="stat-card">
    <div class="stat-value">{{ value }}</div>
    <div class="stat-label">{{ label }}</div>
  </div>
</template>
