<script setup>
import { ORDER_STATUS, ORDER_STATUS_LABEL } from '@/utils/constants';
import { formatTime } from '@/utils/format';

const props = defineProps({
  history: { type: Array, default: () => [] },
});

const statusOrder = [
  ORDER_STATUS.PENDING,
  ORDER_STATUS.CONFIRMED,
  ORDER_STATUS.PREPARING,
  ORDER_STATUS.DELIVERING,
  ORDER_STATUS.DELIVERED,
];

function getStatusClass(entry, index) {
  if (entry.status === ORDER_STATUS.CANCELLED) return 'cancelled';
  const statusIndex = statusOrder.indexOf(entry.status);
  const currentMax = statusOrder.indexOf(
    props.history[props.history.length - 1]?.status,
  );
  if (statusIndex < currentMax) return 'completed';
  if (statusIndex === currentMax) return 'active';
  return '';
}
</script>

<template>
  <div class="timeline">
    <div
      v-for="(entry, index) in history"
      :key="index"
      class="timeline-item"
      :class="getStatusClass(entry, index)"
    >
      <div class="timeline-dot"></div>
      <div class="timeline-content">
        <strong>{{ ORDER_STATUS_LABEL[entry.status] || entry.status }}</strong>
        <p
          v-if="entry.note"
          style="font-size: 13px; color: var(--text-mid); margin: 2px 0"
        >
          {{ entry.note }}
        </p>
        <div class="time">{{ formatTime(entry.time) }}</div>
      </div>
    </div>
  </div>
</template>
