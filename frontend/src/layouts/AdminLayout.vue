<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRoute, useRouter } from 'vue-router';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import AppBreadcrumbs from '@/components/common/AppBreadcrumbs.vue';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const sidebarOpen = ref(false);
const isDrawerViewport = ref(false);
const sidebarInert = ref(false);
const backgroundInert = ref(false);
const sidebar = ref(null);
const drawerTrigger = ref(null);
const drawerClose = ref(null);
const websiteLink = ref(null);
let drawerMedia;
let triggerToRestore;
let previousBodyOverflow = '';

const userInitials = computed(() => {
  const words = auth.user?.fullName?.trim().split(/\s+/).filter(Boolean) || [];
  if (!words.length) return 'FG';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return `${words[0][0]}${words.at(-1)[0]}`.toUpperCase();
});

function drawerFocusableElements() {
  return [...sidebar.value.querySelectorAll('button:not([disabled]),a[href]')].filter(element => element.offsetParent !== null);
}

function handleDrawerKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeDrawer();
    return;
  }
  if (event.key !== 'Tab') return;
  const elements = drawerFocusableElements();
  const first = elements[0];
  const last = elements.at(-1);
  if (event.shiftKey && (document.activeElement === first || !sidebar.value.contains(document.activeElement))) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && (document.activeElement === last || !sidebar.value.contains(document.activeElement))) {
    event.preventDefault();
    first.focus();
  }
}

async function openDrawer(event) {
  if (!isDrawerViewport.value || sidebarOpen.value) return;
  triggerToRestore = event?.currentTarget || drawerTrigger.value;
  previousBodyOverflow = document.body.style.overflow;
  sidebarInert.value = false;
  sidebarOpen.value = true;
  await nextTick();
  drawerClose.value?.focus();
  backgroundInert.value = true;
  document.body.style.overflow = 'hidden';
  document.addEventListener('keydown', handleDrawerKeydown);
}

async function closeDrawer(restoreFocus = true, fallbackFocus = null) {
  if (!sidebarOpen.value) return;
  backgroundInert.value = false;
  await nextTick();
  const focusTarget = fallbackFocus?.$el || fallbackFocus || (restoreFocus && triggerToRestore?.isConnected ? triggerToRestore : null);
  focusTarget?.focus?.();
  sidebarOpen.value = false;
  sidebarInert.value = isDrawerViewport.value;
  document.removeEventListener('keydown', handleDrawerKeydown);
  document.body.style.overflow = previousBodyOverflow;
  triggerToRestore = undefined;
}

function toggleDrawer(event) {
  if (sidebarOpen.value) closeDrawer();
  else openDrawer(event);
}

async function syncDrawerViewport(event) {
  const enteringDrawer = event.matches;
  if (enteringDrawer && sidebar.value?.contains(document.activeElement)) drawerTrigger.value?.focus();
  isDrawerViewport.value = enteringDrawer;
  if (!enteringDrawer && sidebarOpen.value) {
    await closeDrawer(false, websiteLink.value);
    sidebarInert.value = false;
    return;
  }
  sidebarInert.value = enteringDrawer && !sidebarOpen.value;
}

function logout() {
  auth.logout();
  router.push('/');
}

watch(() => route.fullPath, () => closeDrawer());

onMounted(() => {
  drawerMedia = window.matchMedia('(max-width: 1279px)');
  syncDrawerViewport(drawerMedia);
  drawerMedia.addEventListener('change', syncDrawerViewport);
});

onBeforeUnmount(() => {
  drawerMedia?.removeEventListener('change', syncDrawerViewport);
  document.removeEventListener('keydown', handleDrawerKeydown);
  if (sidebarOpen.value) document.body.style.overflow = previousBodyOverflow;
});

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
    { label: 'Dashboard nhân sự', path: '/admin/hr', icon: 'bi-person-workspace' },
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
    <aside id="admin-sidebar" ref="sidebar" class="sidebar" :class="{ open: sidebarOpen }" aria-label="Menu quản trị" :role="isDrawerViewport && sidebarOpen ? 'dialog' : undefined" :aria-modal="isDrawerViewport && sidebarOpen ? 'true' : undefined" :inert="sidebarInert ? '' : undefined">
      <div class="sidebar-brand">
        <div class="sidebar-brand-identity">
          <span class="sidebar-brand-mark" aria-hidden="true">FG</span>
          <span><strong>FastGuy</strong><small>Operations Admin</small></span>
        </div>
        <button ref="drawerClose" class="drawer-close" type="button" aria-label="Đóng điều hướng quản trị" @click="closeDrawer">
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>
      </div>
      <nav class="sidebar-nav" aria-label="Điều hướng quản trị">
        <section v-for="(group, index) in navigationGroups" :key="group.label" class="nav-group" :aria-labelledby="`admin-nav-group-${index}`">
          <h2 :id="`admin-nav-group-${index}`">{{ group.label }}</h2>
          <router-link
            v-for="link in group.links"
            :key="link.path"
            :to="link.path"
            :class="{ 'router-link-active': isLinkActive(link) }"
            @click="closeDrawer"
          >
            <i :class="link.icon" aria-hidden="true"></i><span>{{ link.label }}</span>
          </router-link>
        </section>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="" class="user-avatar" />
          <span v-else class="user-avatar avatar-initials" aria-hidden="true">{{ userInitials }}</span>
          <div>
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role">Quản trị viên</div>
          </div>
        </div>
      </div>
    </aside>
    <div class="main-content" :inert="backgroundInert ? '' : undefined">
      <header class="topbar" role="banner">
        <div class="topbar-left">
          <button ref="drawerTrigger" class="mobile-toggle-sidebar" type="button" aria-label="Mở menu quản trị" :aria-expanded="sidebarOpen" aria-controls="admin-sidebar" @click="toggleDrawer">
            <i class="bi bi-list" aria-hidden="true"></i>
          </button>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <div class="topbar-right">
          <router-link ref="websiteLink" to="/home" class="icon-btn" aria-label="Mở website FastGuy">
            <i class="bi bi-house" aria-hidden="true"></i>
          </router-link>
          <button class="logout-btn" @click="logout">
            <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
          </button>
        </div>
      </header>
      <div class="page-content fg-page">
        <AppBreadcrumbs />
        <router-view />
      </div>
    </div>
    <button v-if="isDrawerViewport && sidebarOpen" class="sidebar-overlay" type="button" aria-label="Đóng menu quản trị" @click="closeDrawer"></button>
  </div>
</template>

<style scoped>
.mobile-toggle-sidebar,
.drawer-close {
  display: none;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  padding: 0;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-mid);
  font-size: 20px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
}
.mobile-toggle-sidebar:hover,
.drawer-close:hover { background: var(--surface); }
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
  transition: background-color var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast);
}
.icon-btn:hover { background: var(--surface); color: var(--text-dark); }
.logout-btn { display:inline-flex; align-items:center; gap:6px; min-height:40px; padding:0 10px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:13px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.mobile-toggle-sidebar:focus-visible,.drawer-close:focus-visible,.icon-btn:focus-visible,.logout-btn:focus-visible,.sidebar-nav a:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
.nav-group{display:grid;gap:3px;margin:8px 0;padding:10px 0;border-block:1px solid var(--border-light)}
.nav-group h2{margin:0;padding:4px 14px;color:var(--text-mid);font-size:11px;letter-spacing:.08em;text-transform:uppercase}
.nav-group .nav-child{margin-left:18px;font-size:12px}
.avatar-initials{display:grid;place-items:center;flex:0 0 36px;background:var(--admin-brand-soft);color:var(--admin-brand);font-size:12px;font-weight:800}
.sidebar-overlay{display:none}
@media (max-width: 1279px) {
  .mobile-toggle-sidebar,.drawer-close{display:flex}
  .sidebar{transform:translateX(-100%)}
  .sidebar-overlay{display:block;position:fixed;inset:0;border:0;background:rgba(0,0,0,.4);backdrop-filter:blur(4px);z-index:99}
}
@media (max-width: 767px) {
  .topbar{padding-inline:16px}
  .page-content{padding:20px 16px}
}
@media (min-width:768px) and (max-width:768px) {
  .topbar{padding-inline:32px}
  .page-content{padding:28px 32px}
}
@media (max-width: 360px) {
  .page-content{padding:16px 12px}
}
</style>

<style scoped>
.fg-shell-admin{--role-accent:var(--admin-brand);--role-soft:var(--admin-brand-soft);background:var(--admin-canvas)}
.sidebar{width:248px}
.main-content{min-width:0;margin-left:248px}
.fg-shell-admin :deep(.sidebar){inset:10px auto 10px 10px;height:calc(100vh - 20px);border:1px solid var(--admin-hairline);border-radius:16px;background:rgba(255,255,255,.96);box-shadow:var(--admin-shell-shadow)}
.fg-shell-admin :deep(.main-content){background:var(--admin-canvas)}
.sidebar-brand{display:flex;align-items:center;justify-content:space-between;gap:12px;border-bottom:0;padding:14px 16px 18px}
.sidebar-brand-identity{display:flex;align-items:center;gap:10px;min-width:0}
.sidebar-brand-identity>span:last-child{display:grid;line-height:1.25}
.sidebar-brand-identity strong{color:var(--admin-foreground);font-size:15px;letter-spacing:-.03em}
.sidebar-brand-identity small{color:var(--admin-muted);font-size:10px}
.sidebar-brand-mark{display:grid;width:38px;height:38px;flex:0 0 38px;place-items:center;border-radius:10px;background:var(--admin-brand);box-shadow:0 6px 14px rgba(255,116,72,.2);color:#fff;font-size:12px;font-weight:800}
.sidebar-nav{padding-inline:10px;scroll-behavior:auto}
.nav-group{margin:0;padding:7px 0;border:0}
.nav-group h2{padding:7px 10px 5px;color:var(--admin-subtle);font-size:9px;font-weight:750}
.sidebar-nav a{min-height:40px;border-radius:8px;color:var(--admin-muted);font-size:13px;font-weight:560}
.sidebar-nav a.router-link-active{color:var(--admin-brand-dark);background:var(--admin-brand-soft);box-shadow:inset 0 0 0 1px rgba(255,116,72,.08);font-weight:720}
.sidebar-nav a.router-link-active::before{content:none}
.sidebar-nav a.router-link-active i{color:var(--admin-brand)}
.sidebar-footer{margin:8px 10px 10px;border:0;border-radius:10px;background:var(--admin-surface-subtle);box-shadow:inset 0 0 0 1px rgba(20,20,35,.035)}
.topbar{height:64px;margin:10px 14px 0;border:1px solid var(--admin-hairline);border-radius:14px;background:rgba(255,255,255,.94);box-shadow:var(--admin-shell-shadow);backdrop-filter:blur(18px)}
.topbar h1{font-size:15px;letter-spacing:-.02em}
.page-content{max-width:1600px;background:var(--admin-canvas)}
@media (max-width:1279px){.main-content{margin-left:0}.fg-shell-admin :deep(.sidebar){inset:0 auto 0 0;height:100vh;border-radius:0 16px 16px 0}.topbar{margin:8px 10px 0}}
@media (prefers-reduced-motion:reduce){.icon-btn,.sidebar-nav a{transition:none}}
</style>
