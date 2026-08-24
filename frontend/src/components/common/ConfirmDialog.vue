<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: 'Xác nhận' },
  busy: { type: Boolean, default: false },
});
const emit = defineEmits(['confirm', 'cancel']);

const dialogRef = ref(null);
const cancelRef = ref(null);
const previousFocus = ref(null);
let previousOverflow = '';

function requestCancel() {
  if (props.busy) return;
  emit('cancel');
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    requestCancel();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = [...dialogRef.value.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (!dialogRef.value.contains(document.activeElement)) {
    event.preventDefault();
    (event.shiftKey ? last : first).focus();
    return;
  }
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

watch(() => props.open, async (open) => {
  if (open) {
    previousFocus.value = document.activeElement;
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeydown);
    await nextTick();
    cancelRef.value?.focus();
    return;
  }
  if (previousFocus.value || previousOverflow !== '') {
    document.removeEventListener('keydown', handleKeydown);
    const focusTarget = previousFocus.value;
    document.body.style.overflow = previousOverflow;
    previousOverflow = '';
    nextTick(() => focusTarget?.focus());
    previousFocus.value = null;
  }
});

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown);
  if (props.open || previousOverflow !== '') document.body.style.overflow = previousOverflow;
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="confirm-overlay" @click.self="requestCancel">
      <section ref="dialogRef" class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-message" tabindex="-1">
        <h2 id="confirm-dialog-title">{{ title }}</h2>
        <p id="confirm-dialog-message">{{ message }}</p>
        <div class="dialog-actions">
          <button ref="cancelRef" class="btn btn-outline" type="button" :disabled="busy" @click="requestCancel">Hủy</button>
          <button class="btn btn-danger" type="button" :disabled="busy" @click="emit('confirm')">{{ busy ? 'Đang xử lý...' : confirmLabel }}</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.confirm-overlay{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.55)}
.confirm-dialog{width:min(420px,100%);padding:24px;border-radius:18px;background:var(--surface);box-shadow:0 24px 70px rgba(15,23,42,.25)}
.confirm-dialog h2{margin:0 0 8px}
.confirm-dialog p{margin:0 0 18px;color:var(--text-mid);line-height:1.5}
.dialog-actions{display:flex;align-items:center;justify-content:flex-end;gap:10px}
</style>
