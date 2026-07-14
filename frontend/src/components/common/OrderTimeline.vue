<script setup>
import { ORDER_STATUS, ORDER_STATUS_LABEL } from '@/utils/constants';
import { formatTime } from '@/utils/format';

const props = defineProps({
  history: { type: Array, default: () => [] },
});

const statusOrder = [
  ORDER_STATUS.WAITING_STOCK_CONFIRM,
  ORDER_STATUS.PENDING,
  ORDER_STATUS.CONFIRMED,
  ORDER_STATUS.PREPARING,
  ORDER_STATUS.READY,
  ORDER_STATUS.PICKED_UP,
  ORDER_STATUS.DELIVERED,
];

function getStatusClass(entry) {
  if (entry.status === ORDER_STATUS.CANCELLED) return 'cancelled';
  const statusIndex = statusOrder.indexOf(entry.status);
  const latest = [...props.history].reverse().find((item) => item.status !== ORDER_STATUS.CANCELLED);
  const currentMax = statusOrder.indexOf(latest?.status);
  if (statusIndex < currentMax) return 'completed';
  if (statusIndex === currentMax) return 'active';
  return '';
}
</script>

<template>
  <div class="route-timeline">
    <div
      v-for="(entry, index) in history"
      :key="index"
      class="route-step"
      :class="getStatusClass(entry)"
    >
      <div class="route-dot"><i class="bi bi-geo-alt-fill"></i></div>
      <div class="route-content">
        <strong>{{ ORDER_STATUS_LABEL[entry.status] || entry.status }}</strong>
        <p v-if="entry.note">{{ entry.note }}</p>
        <div class="time">{{ formatTime(entry.time) }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.route-timeline {
  position: relative;
  padding-left: 36px;
}
.route-timeline::before {
  content: '';
  position: absolute;
  left: 13px;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: var(--radius-full);
  background: repeating-linear-gradient(
    180deg,
    var(--route-orange) 0 16px,
    transparent 16px 26px
  );
  opacity: 0.36;
}
.route-step {
  position: relative;
  padding: 0 0 22px;
}
.route-step:last-child { padding-bottom: 0; }
.route-dot {
  position: absolute;
  left: -35px;
  top: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #fff;
  color: var(--text-light);
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-xs);
  z-index: 1;
}
.route-step.completed .route-dot,
.route-step.active .route-dot {
  background: linear-gradient(135deg, var(--route-orange), var(--route-amber));
  border-color: transparent;
  color: #fff;
  box-shadow: 0 6px 16px rgba(255,107,53,0.28);
}
.route-step.cancelled .route-dot {
  background: var(--red-active);
  border-color: transparent;
  color: #fff;
}
.route-step.active .route-dot::after {
  content: '';
  position: absolute;
  inset: -7px;
  border-radius: 50%;
  border: 2px solid rgba(255,107,53,0.22);
  animation: pulseRoute 1.6s ease-out infinite;
}
@keyframes pulseRoute {
  0% { opacity: 1; transform: scale(0.7); }
  100% { opacity: 0; transform: scale(1.35); }
}
.route-content {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: 12px 14px;
}
.route-content strong { font-size: 14px; }
.route-content p {
  font-size: 13px;
  color: var(--text-mid);
  margin: 3px 0;
}
.time {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
}
</style>
