<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRouter } from 'vue-router';
import { ref, onMounted, onUnmounted } from 'vue';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const sidebarOpen = ref(false);

onMounted(() => notificationStore.startPolling());
onUnmounted(() => notificationStore.stopPolling());

function logout() {
  notificationStore.reset();
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Tổng quan', path: '/admin', icon: 'bi-speedometer2' },
  { label: 'Người dùng', path: '/admin/users', icon: 'bi-people' },
  { label: 'Sản phẩm', path: '/admin/products', icon: 'bi-box-seam' },
   { label: 'Tồn kho', path: '/admin/inventory', icon: 'bi-boxes' },
   { label: 'Danh mục', path: '/admin/categories', icon: 'bi-tags' },
  { label: 'Đơn hàng', path: '/admin/orders', icon: 'bi-receipt' },
  { label: 'Doanh thu', path: '/admin/reports/revenue', icon: 'bi-graph-up' },
  { label: 'Bán chạy', path: '/admin/reports/top-products', icon: 'bi-trophy' },
   { label: 'Mã giảm giá', path: '/admin/coupons', icon: 'bi-ticket-perforated' },
   { label: 'Banner', path: '/admin/banners', icon: 'bi-images' },
   { label: 'Ca làm', path: '/admin/shifts', icon: 'bi-calendar-week' },
  { label: 'Cài đặt', path: '/admin/settings', icon: 'bi-gear' },
];
</script>

<template>
  <div class="sidebar-layout fg-shell fg-shell-admin">
    <aside id="admin-sidebar" class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="sidebar-brand-title">Fast<span class="sidebar-brand-highlight">Guy</span></span>
        <span class="sidebar-brand-subtitle">Quản trị</span>
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
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <img :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'" :alt="auth.user?.fullName || 'Quản trị viên'" class="user-avatar" />
          <div>
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role">Quản trị viên</div>
          </div>
        </div>
      </div>
    </aside>
    <div class="main-content">
      <div class="topbar">
        <div class="topbar-left">
          <button class="mobile-toggle-sidebar" aria-label="Mở menu quản trị" :aria-expanded="sidebarOpen" aria-controls="admin-sidebar" @click="sidebarOpen = !sidebarOpen">
            <i class="bi bi-list"></i>
          </button>
          <h2>Admin</h2>
          <span class="fg-status-chip">Control cockpit</span>
        </div>
        <div class="topbar-right">
          <NotificationBell />
          <router-link to="/" class="icon-btn" title="Website">
            <i class="bi bi-house"></i>
          </router-link>
          <button class="logout-btn" @click="logout">
            <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
          </button>
        </div>
      </div>
      <div class="page-content fg-page">
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
  color: var(--text-mid);
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  align-items: center;
  justify-content: center;
}
.mobile-toggle-sidebar:hover { background: var(--surface); }
.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  color: var(--text-mid);
  border: none;
  transition: all var(--transition-fast);
}
.icon-btn:hover { background: var(--surface); color: var(--text-dark); }
.logout-btn { display:inline-flex; align-items:center; gap:6px; min-height:34px; padding:0 10px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:13px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.sidebar-overlay {
  display: none;
}
@media (max-width: 768px) {
  .mobile-toggle-sidebar { display: flex; }
  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.4);
    backdrop-filter: blur(4px);
    z-index: 99;
  }
}
</style>
