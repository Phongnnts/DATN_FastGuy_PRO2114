<script setup>
import { ORDER_STATUS_BADGE, ORDER_STATUS_LABEL } from '@/utils/constants';

const props = defineProps({
  status: { type: String, required: true },
});

const badgeClass = ORDER_STATUS_BADGE[props.status] || 'secondary';
const label = ORDER_STATUS_LABEL[props.status] || props.status;
</script>

<template>
  <span class="badge" :class="'badge-' + badgeClass">{{ label }}</span>
</template>
