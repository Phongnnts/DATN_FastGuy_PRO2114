<script setup>
const links = [
  { label: 'Thông tin', path: '/account/profile', icon: 'bi-person' },
  { label: 'Đơn hàng', path: '/account/orders', icon: 'bi-box' },
  { label: 'Yêu thích', path: '/account/favorites', icon: 'bi-heart' },
  { label: 'Hỗ trợ', path: '/account/support', icon: 'bi-headset' },
  { label: 'Mật khẩu', path: '/account/change-password', icon: 'bi-lock' },
];
</script>

<template>
  <nav class="account-tabs" aria-label="Tài khoản">
    <router-link v-for="link in links" :key="link.path" :to="link.path" class="account-tab">
      <i class="bi" :class="link.icon" aria-hidden="true"></i>
      <span>{{ link.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.account-tabs {
  display: flex;
  overflow-x: auto;
  scrollbar-width: thin;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: var(--radius) var(--radius) 0 0;
}
.account-tab {
  min-height: var(--control-height);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 18px;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}
.account-tab:hover { color: var(--color-text); background: var(--color-surface-muted); }
.account-tab.router-link-active { color: var(--color-accent); border-bottom-color: var(--color-accent); }
@media (max-width: 520px) {
  .account-tab { flex: 1 0 auto; padding: 0 14px; }
}
</style>
