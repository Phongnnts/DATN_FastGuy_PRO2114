<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';
import { staffApi } from '@/api';

const auth = useAuthStore();
const router = useRouter();
const sidebarOpen = ref(false);
const pendingCount = ref(0);

onMounted(async () => {
  try {
    const orders = await staffApi.getOrders();
    pendingCount.value = Array.isArray(orders) ? orders.length : 0;
  } catch {}
});

function logout() {
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Tổng quan', path: '/staff', icon: 'bi-speedometer2' },
  { label: 'Đơn hàng', path: '/staff/orders', icon: 'bi-receipt' },
  { label: 'Lịch sử đơn', path: '/staff/orders/history', icon: 'bi-clock-history' },
];
</script>

<template>
  <div class="sidebar-layout">
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="sidebar-brand-title">Fast<span class="sidebar-brand-highlight">Guy</span></span>
        <span class="sidebar-brand-subtitle">Staff</span>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="link in sidebarLinks"
          :key="link.path"
          :to="link.path"
          @click="sidebarOpen = false"
        >
          <i :class="link.icon"></i>
          <span>{{ link.label }}</span>
          <span v-if="link.path === '/staff/orders' && pendingCount > 0" class="badge badge-warning" style="margin-left:auto">{{ pendingCount }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <img
            :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'"
            class="user-avatar"
          />
          <div>
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role">Nhân viên</div>
          </div>
        </div>
      </div>
    </aside>
    <div class="main-content">
      <div class="topbar">
        <div class="topbar-left">
          <button
            class="mobile-toggle-sidebar"
            @click="sidebarOpen = !sidebarOpen"
          >
            <i class="bi bi-list"></i>
          </button>
          <h2>Staff Panel</h2>
        </div>
        <div class="topbar-right">
          <router-link to="/" class="btn btn-sm btn-ghost">
            <i class="bi bi-house"></i> Website
          </router-link>
          <button class="btn btn-sm btn-ghost" @click="logout">
            <i class="bi bi-box-arrow-right"></i> Đăng xuất
          </button>
        </div>
      </div>
      <div class="page-content">
        <router-view />
      </div>
    </div>
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>
  </div>
</template>

<style scoped>
.mobile-toggle-sidebar {
  display: none;
  background: none;
  border: none;
  font-size: 20px;
  padding: 4px;
  cursor: pointer;
  color: var(--text-dark);
}
@media (max-width: 768px) {
  .mobile-toggle-sidebar { display: block; }
}
.sidebar-overlay {
  display: none;
}
@media (max-width: 768px) {
  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.3);
    z-index: 99;
  }
}
</style>
