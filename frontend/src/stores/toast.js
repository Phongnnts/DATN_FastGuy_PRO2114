import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([]);
  let nextId = 0;

  function show(message, type = 'info', duration = 3000) {
    const id = nextId++;
    toasts.value.push({ id, message, type });
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id);
    }, duration);
  }

  function success(message, duration = 3000) { show(message, 'success', duration); }
  function error(message, duration = 4000) { show(message, 'error', duration); }
  function warning(message, duration = 3500) { show(message, 'warning', duration); }
  function info(message, duration = 3000) { show(message, 'info', duration); }
  function dismiss(id) { toasts.value = toasts.value.filter(t => t.id !== id); }

  return { toasts, show, success, error, warning, info, dismiss };
});

// backward compat alias
export function useToast() {
  return useToastStore();
}
