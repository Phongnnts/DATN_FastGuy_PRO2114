<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRoute, useRouter } from 'vue-router';
import { ref, onMounted, onUnmounted } from 'vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import AppBreadcrumbs from '@/components/common/AppBreadcrumbs.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const route = useRoute();
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
   { label: 'Sổ tồn kho', path: '/admin/inventory/ledger', icon: 'bi-journal-text' },
   { label: 'Danh mục', path: '/admin/categories', icon: 'bi-tags' },
  { label: 'Đơn hàng', path: '/admin/orders', icon: 'bi-receipt' },
  { label: 'Đối soát COD', path: '/admin/cod-settlements', icon: 'bi-cash-stack' },
  { label: 'Hoàn tiền', path: '/admin/refunds', icon: 'bi-arrow-return-left' },
  { label: 'Báo cáo', path: '/admin/reports', icon: 'bi-graph-up' },
   { label: 'Mã giảm giá', path: '/admin/coupons', icon: 'bi-ticket-perforated' },
   { label: 'Banner', path: '/admin/banners', icon: 'bi-images' },
   { label: 'Ca làm', path: '/admin/shifts', icon: 'bi-calendar-week' },
  { label: 'Cài đặt', path: '/admin/settings', icon: 'bi-gear' },
];

function isLinkActive(link) {
  if (link.path === '/admin/products') return route.path === link.path || route.path.startsWith(`${link.path}/`);
  return route.path === link.path;
}
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
          :class="{ 'router-link-active': isLinkActive(link) }"
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
          <span class="fg-status-chip">Trung tâm quản trị</span>
        </div>
        <div class="topbar-right">
          <NotificationBell />
          <router-link to="/home" class="icon-btn" title="Website">
            <i class="bi bi-house"></i>
          </router-link>
          <button class="logout-btn" @click="logout">
            <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
          </button>
        </div>
      </div>
      <div class="page-content fg-page">
        <AppBreadcrumbs />
        <router-view />
      </div>
    </div>
    <button v-if="sidebarOpen" class="sidebar-overlay" aria-label="Đóng menu quản trị" @click="sidebarOpen = false"></button>
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

<style scoped>
.fg-shell-admin{--role-accent:var(--primary);--role-soft:var(--primary-50)}
.fg-shell-admin :deep(.sidebar){border-right:1px solid rgba(23,23,23,.06);background:linear-gradient(180deg,#fff 0%,#fffaf6 100%)}
.sidebar-brand{border-bottom-color:rgba(232,115,74,.12)}
.sidebar-brand-title{letter-spacing:-.04em}
.sidebar-nav a{border-radius:12px}
.sidebar-nav a.router-link-active{color:#fff;background:linear-gradient(135deg,var(--primary),#ef956f);box-shadow:0 8px 20px rgba(232,115,74,.24)}
.sidebar-nav a.router-link-active i{color:#fff}
.topbar{height:64px;border-bottom-color:rgba(23,23,23,.06);background:rgba(250,250,250,.88)}
.topbar h2{font-size:15px;letter-spacing:-.02em}
.fg-status-chip{color:var(--primary-dark);background:var(--primary-50);border-color:var(--primary-100)}
.page-content{background:#f7f5f2}
</style>
