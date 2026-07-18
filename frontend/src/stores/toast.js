import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([]);
  let nextId = 0;

  function add(message, type = 'info', duration = 4000) {
    const id = nextId++;
    toasts.value.push({ id, message, type });
    if (duration > 0) {
      setTimeout(() => remove(id), duration);
    }
    return id;
  }

  function success(message) {
    return add(message, 'success');
  }

  function error(message) {
    return add(message, 'error', 6000);
  }

  function info(message) {
    return add(message, 'info');
  }

  function warning(message) {
    return add(message, 'warning', 5000);
  }

  function remove(id) {
    const idx = toasts.value.findIndex((t) => t.id === id);
    if (idx !== -1) toasts.value.splice(idx, 1);
  }

  function dismiss(id) {
    remove(id);
  }

  return { toasts, success, error, info, warning, remove, dismiss };
});
