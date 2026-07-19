import { ref } from 'vue';

const toasts = ref([]);
let nextId = 0;

export function useToast() {
  function show(message, type = 'info', duration = 3000) {
    const id = nextId++;
    toasts.value.push({ id, message, type });
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id);
    }, duration);
  }

  function success(message, duration = 3000) {
    show(message, 'success', duration);
  }

  function error(message, duration = 4000) {
    show(message, 'error', duration);
  }

  function warning(message, duration = 3500) {
    show(message, 'warning', duration);
  }

  function info(message, duration = 3000) {
    show(message, 'info', duration);
  }

  return { toasts, show, success, error, warning, info };
}
