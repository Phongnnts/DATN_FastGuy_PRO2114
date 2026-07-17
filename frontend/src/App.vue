<script setup>
import { useToast } from '@/utils/toast';
const { toasts } = useToast();
</script>

<template>
  <router-view />
  <div class="toast-container">
    <transition-group name="toast-fade">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast-item"
        :class="'toast-' + t.type"
      >
        <i :class="t.type === 'success' ? 'bi bi-check-circle-fill' : t.type === 'error' ? 'bi bi-x-circle-fill' : t.type === 'warning' ? 'bi bi-exclamation-triangle-fill' : 'bi bi-info-circle-fill'"></i>
        <span>{{ t.message }}</span>
      </div>
    </transition-group>
  </div>
</template>

<style>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}
.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  pointer-events: auto;
  min-width: 250px;
  max-width: 400px;
}
.toast-success { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; }
.toast-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.toast-warning { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
.toast-info { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
.toast-fade-enter-active { transition: all 0.3s ease; }
.toast-fade-leave-active { transition: all 0.2s ease; }
.toast-fade-enter-from { opacity: 0; transform: translateX(40px); }
.toast-fade-leave-to { opacity: 0; transform: translateX(40px); }
</style>
