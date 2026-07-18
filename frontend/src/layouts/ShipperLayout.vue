<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRouter, useRoute } from 'vue-router';
import { onMounted, onUnmounted } from 'vue';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const route = useRoute();

const navItems = [
  { path: '/shipper', name: 'Trang chủ', icon: 'bi-house-door' },
  { path: '/shipper/orders', name: 'Đơn giao', icon: 'bi-bicycle' },
];

function activeClass(path) {
  return route.path === path ? 'active' : '';
}

onMounted(() => notificationStore.startPolling());
onUnmounted(() => notificationStore.stopPolling());

function logout() {
  notificationStore.reset();
  auth.logout();
  router.push('/login');
}
</script>

<template>
  <div class="shipper-layout fg-shell fg-shell-shipper">
    <header class="shipper-header">
      <div class="shipper-brand">
        <span>Fast<span class="accent">Guy</span></span>
        <span class="role-badge">Shipper</span>
        <span class="fg-status-chip">Live route</span>
      </div>
      <div class="header-actions">
        <NotificationBell />
        <button class="logout-btn" @click="logout">
          <i class="bi bi-arrow-right-from-bracket"></i><span>Thoát</span>
        </button>
      </div>
    </header>
    <main class="shipper-main fg-page">
      <router-view />
    </main>
    <nav class="shipper-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="shipper-nav-item"
        :class="activeClass(item.path)"
      >
        <i :class="item.icon"></i>
        <span>{{ item.name }}</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.shipper-layout {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  max-width: 480px;
  margin: 0 auto;
  background: var(--bg);
}
.shipper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 56px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  position: sticky;
  top: 0;
  z-index: 10;
}
.shipper-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.3px;
}
.accent { color: var(--primary); }
.role-badge {
  font-size: 10px;
  font-weight: 600;
  background: var(--primary-50);
  color: var(--primary);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  color: var(--text-mid);
  transition: all var(--transition-fast);
}
.icon-btn:hover { background: var(--surface); color: var(--text-dark); }
.logout-btn { display:inline-flex; align-items:center; gap:5px; min-height:34px; padding:0 8px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:12px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.shipper-main {
  flex: 1;
  padding: 16px;
  padding-bottom: 80px;
  overflow-y: auto;
}
.shipper-nav {
  display: flex;
  background: #fff;
  border-top: 1px solid var(--border-light);
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  z-index: 10;
  box-shadow: 0 -1px 3px rgba(0,0,0,0.04);
}
.shipper-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0 8px;
  font-size: 11px;
  color: var(--text-light);
  text-decoration: none;
  gap: 3px;
  transition: color var(--transition-fast);
}
.shipper-nav-item i { font-size: 20px; }
.shipper-nav-item.active {
  color: var(--primary);
  font-weight: 600;
}
</style>
