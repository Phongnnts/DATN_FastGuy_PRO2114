<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRouter } from 'vue-router';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { staffApi, shiftApi } from '@/api';
import NotificationBell from '@/components/common/NotificationBell.vue';
import AppBreadcrumbs from '@/components/common/AppBreadcrumbs.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const sidebarOpen = ref(false);
const pendingCount = ref(0);
const shiftState = ref('UNKNOWN');
const checkedIn = computed(() => shiftState.value === 'CHECKED_IN');

let refreshTimer = 0;
let shiftSequence = 0;

async function refreshPendingCount() {
  try {
    const orders = await staffApi.getOrders();
    pendingCount.value = Array.isArray(orders) ? orders.filter((order) => order.orderStatus === 'PENDING' || order.status === 'PENDING').length : 0;
  } catch {}
}

async function checkShift() {
  const token = ++shiftSequence;
  shiftState.value = 'UNKNOWN';
  clearInterval(refreshTimer);
  refreshTimer = 0;
  try {
    const data = await shiftApi.getCurrent();
    if (token !== shiftSequence) return;
    shiftState.value = data?.state || 'UNKNOWN';
  } catch { if (token !== shiftSequence) return; shiftState.value = 'UNKNOWN'; }
  if (checkedIn.value) {
    refreshPendingCount();
    refreshTimer = setInterval(refreshPendingCount, 30000);
  } else pendingCount.value = 0;
}

const sidebarLinks = computed(() => [
  ...(checkedIn.value ? [
    { label: 'Tổng quan', path: '/staff', icon: 'bi-speedometer2' },
    { label: 'Bếp · Đơn hàng', path: '/staff/orders', icon: 'bi-receipt' },
    { label: 'Điều phối giao hàng', path: '/staff/dispatch', icon: 'bi-truck' },
  ] : []),
  { label: 'Lịch sử đơn', path: '/staff/orders/history', icon: 'bi-clock-history' },
  { label: 'Ca làm', path: '/staff/shifts', icon: 'bi-calendar-week' },
  { label: 'Thông báo', path: '/staff/notifications', icon: 'bi-bell' },
  { label: 'Hồ sơ', path: '/staff/profile', icon: 'bi-person-circle' },
  { label: 'Hỗ trợ', path: '/staff/support', icon: 'bi-headset' },
]);

onMounted(async () => {
  window.addEventListener('staff-shift-changed', checkShift);
  await checkShift();
  notificationStore.startPolling();
});

onUnmounted(() => {
  clearInterval(refreshTimer);
  window.removeEventListener('staff-shift-changed', checkShift);
  notificationStore.stopPolling();
});

function logout() {
  notificationStore.reset();
  auth.logout();
  router.push('/');
}
</script>

<template>
  <div class="sidebar-layout fg-shell fg-shell-staff">
    <aside id="staff-sidebar" class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="sidebar-brand-title">Fast<span class="sidebar-brand-highlight">Guy</span></span>
        <span class="sidebar-brand-subtitle">Staff</span>
      </div>
      <nav class="sidebar-nav" aria-label="Điều hướng nhân viên">
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
          <img :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'" :alt="auth.user?.fullName || 'Nhân viên'" class="user-avatar" />
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
          <button class="mobile-toggle-sidebar" aria-label="Mở menu nhân viên" :aria-expanded="sidebarOpen" aria-controls="staff-sidebar" @click="sidebarOpen = !sidebarOpen">
            <i class="bi bi-list"></i>
          </button>
          <h2>Staff</h2>
          <span class="fg-status-chip">Hàng đợi bếp</span>
        </div>
        <div class="topbar-right">
          <NotificationBell />
          <router-link to="/home" class="icon-btn" aria-label="Mở website">
            <i class="bi bi-house"></i>
          </router-link>
          <button class="logout-btn" @click="logout">
            <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
          </button>
        </div>
      </div>
      <div class="page-content fg-page">
        <AppBreadcrumbs />
        <div v-if="!checkedIn" class="no-shift-banner">
          <i class="bi bi-calendar-x"></i>
          <div v-if="shiftState === 'UNKNOWN'">
            <strong>Không thể xác định trạng thái ca</strong>
            <p>Chỉ lịch làm việc, lịch sử đơn và hỗ trợ đang khả dụng.</p>
          </div>
          <div v-else-if="shiftState === 'CHECKED_OUT'">
            <strong>Ca làm đã kết thúc</strong>
            <p>Các nghiệp vụ đang được khóa.</p>
          </div>
          <div v-else>
            <strong>Bạn chưa check-in</strong>
            <p>Vui lòng xem lịch làm việc để check-in đúng giờ.</p>
          </div>
        </div>
        <div class="staff-view">
          <router-view v-if="shiftState !== 'UNKNOWN' || $route.name === 'StaffShifts' || $route.name === 'StaffOrderHistory' || $route.name === 'StaffNotifications' || $route.name === 'StaffProfile' || $route.name === 'StaffSupport'" aria-live="polite" role="region" />
        </div>
      </div>
    </div>
    <button v-if="sidebarOpen" class="sidebar-overlay" aria-label="Đóng menu nhân viên" @click="sidebarOpen = false"></button>
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
.sidebar-overlay { display: none; }
.no-shift-banner { display:flex; align-items:center; gap:12px; padding:14px 16px; margin-bottom:16px; border-radius:var(--radius); background:#fef3c7; color:#92400e; font-size:14px; }
.no-shift-banner i { font-size:28px; flex-shrink:0; }
.no-shift-banner strong { display:block; margin-bottom:2px; }
.no-shift-banner p { margin:0; font-size:13px; opacity:.8; }
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
.fg-shell-staff{--role-accent:var(--role-staff);--role-soft:var(--role-staff-soft)}
.fg-shell-staff :deep(.sidebar){border-right:1px solid rgba(15,118,110,.1);background:linear-gradient(180deg,#f8fffe 0%,#effaf8 100%)}
.sidebar-brand{border-bottom-color:rgba(15,118,110,.12)}
.sidebar-brand-title{letter-spacing:-.04em}
.sidebar-brand-highlight{color:var(--role-staff)}
.sidebar-nav a{border-radius:12px;transition-property:color,background-color,box-shadow,transform;transition-duration:var(--transition-fast)}
.sidebar-nav a:hover{color:#115e59;background:rgba(15,118,110,.08);transform:translateX(2px)}
.sidebar-nav a.router-link-active{color:#fff;background:linear-gradient(135deg,var(--role-staff),#14b8a6);box-shadow:0 8px 20px rgba(15,118,110,.24)}
.sidebar-nav a.router-link-active i{color:#fff}
.topbar{height:64px;border-bottom-color:rgba(15,118,110,.1);background:rgba(248,255,254,.9);backdrop-filter:blur(14px)}
.topbar h2{font-size:15px;letter-spacing:-.02em}
.fg-status-chip{color:#115e59;background:var(--role-staff-soft);border-color:rgba(15,118,110,.18)}
.page-content{background:linear-gradient(180deg,#f3faf9 0,#f8faf9 220px,#f7f7f6 100%)}
.no-shift-banner{border:1px solid rgba(15,118,110,.18);background:#ecfdf5;color:#115e59;box-shadow:var(--shadow-xs)}
.staff-view{display:grid;gap:var(--space-5)}
.staff-view :deep(.page-header){align-items:flex-end;margin-bottom:20px;padding:4px 2px}
.staff-view :deep(.page-header h1){font-size:clamp(24px,3vw,34px);line-height:1.15;letter-spacing:-.045em;text-wrap:balance}
.staff-view :deep(.page-header p){max-width:680px;margin-top:6px;color:#52706d;text-wrap:pretty}
.staff-view :deep(.card),.staff-view :deep(.kitchen-queue),.staff-view :deep(.empty-state){border:1px solid rgba(15,118,110,.1);border-radius:18px;box-shadow:0 10px 28px rgba(15,23,42,.05);background:rgba(255,255,255,.96)}
.staff-view :deep(.card){padding:22px}
.staff-view :deep(.card-flat){overflow:hidden}
.staff-view :deep(.stat-grid),.staff-view :deep(.queue-cards){gap:14px}
.staff-view :deep(.stat-card),.staff-view :deep(.queue-card){border:1px solid rgba(15,118,110,.1);border-radius:16px;background:linear-gradient(145deg,#fff,#f2fbf9);transition-property:transform,box-shadow,border-color;transition-duration:var(--transition-fast)}
.staff-view :deep(.stat-card:hover),.staff-view :deep(.queue-card:hover){transform:translateY(-2px);border-color:rgba(15,118,110,.28);box-shadow:0 12px 24px rgba(15,118,110,.1)}
.staff-view :deep(.stat-value),.staff-view :deep(.queue-card strong),.staff-view :deep(.order-totals){font-variant-numeric:tabular-nums}
.staff-view :deep(.tabs){gap:6px;padding:6px;border-radius:14px;background:#edf7f5}
.staff-view :deep(.tabs button){min-height:40px;border-radius:10px}
.staff-view :deep(.tabs button.active){color:#fff;background:var(--role-staff);box-shadow:0 6px 14px rgba(15,118,110,.2)}
.staff-view :deep(.btn-primary){border-color:var(--role-staff);background:var(--role-staff)}
.staff-view :deep(.btn-primary:hover){border-color:#115e59;background:#115e59}
.staff-view :deep(.form-input:focus),.staff-view :deep(.form-select:focus),.staff-view :deep(.form-textarea:focus){border-color:var(--role-staff);box-shadow:0 0 0 3px rgba(15,118,110,.13)}
.staff-view :deep(.table){min-width:720px}
.staff-view :deep(.table thead th){color:#46615e;background:#f0f8f7;border-bottom-color:rgba(15,118,110,.12)}
.staff-view :deep(.table tbody tr:hover){background:#f4fbfa}
.staff-view :deep(.staff-state),.staff-view :deep(.lane-state),.staff-view :deep(.state){border:1px dashed rgba(15,118,110,.25);border-radius:16px;padding:22px;background:rgba(255,255,255,.72)}
.staff-view :deep(.empty-state){padding:42px 24px;color:#52706d}
.staff-view :deep(.empty-state i){color:var(--role-staff)}
.staff-view :deep(.modal),.staff-view :deep(.modal-content){border:1px solid rgba(15,118,110,.12);border-radius:20px;box-shadow:0 24px 60px rgba(15,23,42,.2)}
@media (max-width:768px){
  .topbar{height:58px}
  .fg-status-chip{display:none}
  .logout-btn span{display:none}
  .logout-btn{width:40px;padding:0;justify-content:center}
  .staff-view :deep(.page-header){align-items:stretch}
  .staff-view :deep(.page-header>div:last-child){width:100%;flex-wrap:wrap}
  .staff-view :deep(.page-header .btn){flex:1;justify-content:center}
  .staff-view :deep(.card){padding:16px}
  .staff-view :deep(.table){min-width:680px}
  .staff-view :deep(.table:has(td[data-label])){min-width:0}
  .staff-view :deep(.table:has(td[data-label]) thead){display:none}
  .staff-view :deep(.table:has(td[data-label]) tbody){display:grid;gap:12px}
  .staff-view :deep(.table tbody tr:has(td[data-label])){display:grid;gap:8px;padding:16px;border:1px solid rgba(15,118,110,.12);border-radius:14px;background:#fff;box-shadow:var(--shadow-xs)}
  .staff-view :deep(.table tbody tr:has(td[data-label]) td){display:grid;grid-template-columns:minmax(96px,36%) 1fr;gap:10px;padding:4px 0;border:0;text-align:left}
  .staff-view :deep(.table tbody td[data-label]::before){content:attr(data-label);color:#52706d;font-size:12px;font-weight:750}
}
</style>
