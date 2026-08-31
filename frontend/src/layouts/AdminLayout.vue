<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRoute, useRouter } from 'vue-router';
import { ref } from 'vue';
import AppBreadcrumbs from '@/components/common/AppBreadcrumbs.vue';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const sidebarOpen = ref(false);

function logout() {
  auth.logout();
  router.push('/');
}

const navigationGroups = [
  { label: 'Tổng quan', links: [{ label: 'Dashboard', path: '/admin', icon: 'bi-speedometer2' }] },
  { label: 'Vận hành', links: [
    { label: 'Đơn hàng', path: '/admin/orders', icon: 'bi-receipt' },
    { label: 'Đối soát COD', path: '/admin/cod-settlements', icon: 'bi-cash-stack' },
    { label: 'Hoàn tiền', path: '/admin/refunds', icon: 'bi-arrow-return-left' },
  ] },
  { label: 'Bán hàng', links: [
    { label: 'Sản phẩm', path: '/admin/products', icon: 'bi-box-seam' },
    { label: 'Danh mục', path: '/admin/categories', icon: 'bi-tags' },
    { label: 'Mã giảm giá', path: '/admin/coupons', icon: 'bi-ticket-perforated' },
    { label: 'Banner', path: '/admin/banners', icon: 'bi-images' },
  ] },
  { label: 'Nhân sự', links: [
    { label: 'Người dùng', path: '/admin/users', icon: 'bi-people' },
    { label: 'Ca làm', path: '/admin/shifts', icon: 'bi-calendar-week' },
    { label: 'Chấm công & tiền công', path: '/admin/attendance', icon: 'bi-person-check' },
  ] },
  { label: 'Kho hàng', links: [
    { label: 'Tồn kho', path: '/admin/inventory', icon: 'bi-boxes' },
    { label: 'Nhập hàng', path: '/admin/inventory/receipts', icon: 'bi-box-arrow-in-down' },
    { label: 'Công thức & định mức', path: '/admin/recipes', icon: 'bi-diagram-3' },
    { label: 'Kiểm kê kho', path: '/admin/inventory/stock-counts', icon: 'bi-clipboard-check' },
  ] },
  { label: 'Báo cáo', links: [{ label: 'Báo cáo kinh doanh', path: '/admin/reports', icon: 'bi-graph-up' }] },
  { label: 'Hệ thống', links: [
    { label: 'Nhật ký hoạt động', path: '/admin/activity-logs', icon: 'bi-clock-history' },
    { label: 'Cài đặt', path: '/admin/settings', icon: 'bi-gear' },
  ] },
];

function isLinkActive(link) {
  if (link.path === '/admin/products') return route.path === link.path || route.path.startsWith(`${link.path}/`);
  if (link.path === '/admin/inventory') return route.path === link.path;
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
      <nav class="sidebar-nav" aria-label="Điều hướng quản trị">
        <section v-for="(group, index) in navigationGroups" :key="group.label" class="nav-group" :aria-labelledby="`admin-nav-group-${index}`">
          <h2 :id="`admin-nav-group-${index}`">{{ group.label }}</h2>
          <router-link
            v-for="link in group.links"
            :key="link.path"
            :to="link.path"
            :class="{ 'router-link-active': isLinkActive(link) }"
            @click="sidebarOpen = false"
          >
            <i :class="link.icon" aria-hidden="true"></i><span>{{ link.label }}</span>
          </router-link>
        </section>
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
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  align-items: center;
  justify-content: center;
}
.mobile-toggle-sidebar:hover { background: var(--surface); }
.icon-btn {
  width: 40px;
  height: 40px;
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
.logout-btn { display:inline-flex; align-items:center; gap:6px; min-height:40px; padding:0 10px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:13px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.mobile-toggle-sidebar:focus-visible,.icon-btn:focus-visible,.logout-btn:focus-visible,.sidebar-nav a:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
.nav-group{display:grid;gap:3px;margin:8px 0;padding:10px 0;border-block:1px solid var(--border-light)}
.nav-group h2{margin:0;padding:4px 14px;color:var(--text-mid);font-size:11px;letter-spacing:.08em;text-transform:uppercase}
.nav-group .nav-child{margin-left:18px;font-size:12px}
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
.main-content{min-width:0}
.fg-shell-admin :deep(.sidebar){border-right:1px solid var(--admin-border);background:var(--admin-surface)}
.sidebar-brand{border-bottom-color:rgba(232,115,74,.12)}
.sidebar-brand-title{letter-spacing:-.04em}
.sidebar-nav a{border-radius:12px}
.sidebar-nav a.router-link-active{color:#fff;background:var(--admin-brand);box-shadow:none}
.sidebar-nav a.router-link-active i{color:#fff}
.topbar{height:64px;border-bottom-color:rgba(23,23,23,.06);background:rgba(250,250,250,.88)}
.topbar h2{font-size:15px;letter-spacing:-.02em}
.fg-status-chip{color:var(--primary-dark);background:var(--primary-50);border-color:var(--primary-100)}
.page-content{background:#f7f5f2}
</style>
