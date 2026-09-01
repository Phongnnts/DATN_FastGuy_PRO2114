<template>
  <section class="admin-workspace" aria-label="Không gian làm việc">
    <section class="admin-workspace__toolbar" aria-label="Thanh công cụ"><slot name="toolbar" /></section>
    <section class="admin-workspace__content" aria-label="Nội dung làm việc"><slot /></section>
    <footer class="admin-workspace__footer" aria-label="Thông tin bổ sung"><slot name="footer" /></footer>
  </section>
</template>

<style scoped>
.admin-workspace{display:grid;min-width:0;border:1px solid var(--admin-border);border-radius:var(--admin-workspace-radius);background:var(--admin-surface);box-shadow:var(--admin-card-shadow);color:var(--admin-foreground);overflow:hidden}
.admin-workspace__toolbar{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);min-width:0;padding:var(--space-4) var(--space-5);border-bottom:1px solid var(--admin-border);background:var(--admin-surface-subtle);flex-wrap:wrap}
.admin-workspace__content{min-width:0;padding:var(--space-5)}
.admin-workspace__footer{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);padding:var(--space-4) var(--space-5);border-top:1px solid var(--admin-border);color:var(--admin-muted);flex-wrap:wrap}
@media(max-width:640px){.admin-workspace__toolbar,.admin-workspace__content,.admin-workspace__footer{padding:var(--space-4)}}
</style>
