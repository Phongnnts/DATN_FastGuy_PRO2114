<template>
  <div class="toast-container" aria-live="polite" aria-relevant="additions removals">
    <transition-group name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        :class="['toast-item', 'toast-' + t.type]"
        :role="t.type === 'error' ? 'alert' : 'status'"
      >
        <span class="toast-icon">
          <i v-if="t.type === 'success'" class="bi bi-check-circle-fill"></i>
          <i v-else-if="t.type === 'error'" class="bi bi-exclamation-circle-fill"></i>
          <i v-else-if="t.type === 'warning'" class="bi bi-exclamation-triangle-fill"></i>
          <i v-else class="bi bi-info-circle-fill"></i>
        </span>
        <span class="toast-message">{{ t.message }}</span>
        <button class="toast-dismiss" @click="dismiss(t.id)" aria-label="Đóng thông báo">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToastStore } from '@/stores/toast';
const { toasts, dismiss } = useToastStore();
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: var(--z-toast);
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 400px;
}
.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  font-size: 14px;
  color: #fff;
}
.toast-success { background: #10b981; }
.toast-error { background: #ef4444; }
.toast-warning { background: #f59e0b; }
.toast-info { background: #3b82f6; }
.toast-icon { font-size: 18px; flex-shrink: 0; }
.toast-message { flex: 1; }
.toast-dismiss {
  width: var(--control-height);
  height: var(--control-height);
  margin: -10px -12px -10px 0;
  background: none;
  border: none;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  font-size: 12px;
}
.toast-dismiss:hover { color: #fff; }
.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.2s ease; }
.toast-enter-from { opacity: 0; transform: translateX(40px); }
.toast-leave-to { opacity: 0; transform: translateX(40px); }
@media (max-width: 480px) {
  .toast-container { top: 8px; right: 8px; left: 8px; max-width: none; }
  .toast-item { width: 100%; }
}
</style>
