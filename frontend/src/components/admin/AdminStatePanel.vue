<script setup>
defineProps({
  state: { type: String, required: true, validator: value => ['loading', 'error', 'empty'].includes(value) },
  title: { type: String, required: true },
  message: { type: String, required: true },
});
const emit = defineEmits(['retry']);
</script>

<template>
  <section
    class="admin-state-panel"
    :class="`admin-state-panel--${state}`"
    :role="state === 'error' ? 'alert' : state === 'loading' ? 'status' : undefined"
    :aria-busy="state === 'loading' ? 'true' : undefined"
  >
    <div class="admin-state-panel__mark" aria-hidden="true"></div>
    <div class="admin-state-panel__copy">
      <h2>{{ title }}</h2>
      <p>{{ message }}</p>
    </div>
    <button v-if="state === 'error'" type="button" @click="emit('retry')">Thử lại</button>
  </section>
</template>

<style scoped>
.admin-state-panel{display:grid;justify-items:center;gap:var(--space-4);padding:var(--space-10) var(--space-6);border:1px solid var(--admin-border);border-radius:var(--admin-workspace-radius);background:var(--admin-surface);color:var(--admin-foreground);text-align:center}
.admin-state-panel__mark{width:var(--space-8);height:var(--space-2);border-radius:var(--admin-control-radius);background:var(--admin-muted)}
.admin-state-panel--loading .admin-state-panel__mark{background:var(--admin-brand)}
.admin-state-panel--error .admin-state-panel__mark{background:var(--admin-danger)}
.admin-state-panel__copy{display:grid;gap:var(--space-2);max-width:52ch}
.admin-state-panel h2,.admin-state-panel p{margin:0}.admin-state-panel h2{font-size:1rem}.admin-state-panel p{color:var(--admin-muted);line-height:1.6}
.admin-state-panel button{min-width:44px;min-height:44px;padding:0 var(--space-4);border:1px solid var(--admin-brand);border-radius:var(--admin-control-radius);background:var(--admin-brand);color:var(--admin-surface);font-weight:700}
.admin-state-panel button:focus-visible{outline:3px solid var(--admin-foreground);outline-offset:3px}
@media(max-width:640px){.admin-state-panel{padding:var(--space-8) var(--space-4)}}
</style>
