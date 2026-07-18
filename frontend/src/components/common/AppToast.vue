<script setup>
import { useToastStore } from '@/stores/toast';

const toast = useToastStore();

function iconClass(type) {
  return {
    success: 'bi-check-circle-fill',
    error: 'bi-exclamation-circle-fill',
    warning: 'bi-exclamation-triangle-fill',
    info: 'bi-info-circle-fill',
  }[type] || 'bi-info-circle-fill';
}
</script>

<template>
  <div class="toast-container" aria-live="polite" aria-atomic="true">
    <TransitionGroup name="toast">
      <div
        v-for="t in toast.toasts"
        :key="t.id"
        :class="['toast-item', 'toast-' + t.type]"
        role="status"
      >
        <i :class="['bi', iconClass(t.type)]"></i>
        <span class="toast-message">{{ t.message }}</span>
        <button
          class="toast-dismiss"
          aria-label="Đóng thông báo"
          @click="toast.dismiss(t.id)"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
  max-width: 400px;
}
.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius);
  background: #fff;
  box-shadow: var(--shadow-lg);
  border-left: 4px solid var(--primary);
  pointer-events: auto;
  font-size: 14px;
  line-height: 1.4;
}
.toast-success {
  border-left-color: #22c55e;
  color: #166534;
}
.toast-error {
  border-left-color: #ef4444;
  color: #991b1b;
}
.toast-warning {
  border-left-color: #f59e0b;
  color: #92400e;
}
.toast-info {
  border-left-color: #3b82f6;
  color: #1e40af;
}
.toast-item i:first-child {
  font-size: 18px;
  flex-shrink: 0;
}
.toast-success i:first-child { color: #22c55e; }
.toast-error i:first-child { color: #ef4444; }
.toast-warning i:first-child { color: #f59e0b; }
.toast-info i:first-child { color: #3b82f6; }
.toast-message {
  flex: 1;
  word-break: break-word;
}
.toast-dismiss {
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  font-size: 12px;
  color: inherit;
  opacity: 0.5;
  flex-shrink: 0;
}
.toast-dismiss:hover {
  opacity: 1;
}
.toast-enter-active {
  transition: all 0.3s ease;
}
.toast-leave-active {
  transition: all 0.2s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(100px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
</style>
