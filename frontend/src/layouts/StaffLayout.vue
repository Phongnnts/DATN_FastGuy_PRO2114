<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRouter } from 'vue-router';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { staffApi, shiftApi } from '@/api';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const sidebarOpen = ref(false);
const pendingCount = ref(0);
const hasTodayShift = ref(true);

let refreshTimer;

async function refreshPendingCount() {
  try {
    const orders = await staffApi.getOrders();
    pendingCount.value = Array.isArray(orders) ? orders.filter((order) => order.orderStatus === 'PENDING' || order.status === 'PENDING').length : 0;
  } catch {}
}

async function checkShift() {
  try {
    const data = await shiftApi.hasToday();
    hasTodayShift.value = data?.hasShift ?? false;
  } catch { hasTodayShift.value = true; }
}

const sidebarLinks = computed(() => {
  if (!hasTodayShift.value) {
    return [{ label: 'Ca làm', path: '/staff/shifts', icon: 'bi-calendar-week' }];
  }
  return [
    { label: 'Tổng quan', path: '/staff', icon: 'bi-speedometer2' },
    { label: 'Đơn hàng', path: '/staff/orders', icon: 'bi-receipt' },
    { label: 'Lịch sử đơn', path: '/staff/orders/history', icon: 'bi-clock-history' },
    { label: 'Ca làm', path: '/staff/shifts', icon: 'bi-calendar-week' },
    { label: 'Hỗ trợ', path: '/staff/support', icon: 'bi-headset' },
  ];
});

onMounted(async () => {
  await checkShift();
  notificationStore.startPolling();
  if (hasTodayShift.value) {
    refreshPendingCount();
    refreshTimer = setInterval(refreshPendingCount, 30000);
  }
});

onUnmounted(() => {
  clearInterval(refreshTimer);
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
          <span class="fg-status-chip">Kitchen queue</span>
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
        <div v-if="!hasTodayShift" class="no-shift-banner">
          <i class="bi bi-calendar-x"></i>
          <div>
            <strong>Bạn chưa được phân ca hôm nay</strong>
            <p>Vui lòng xem lịch làm việc để biết thêm chi tiết.</p>
          </div>
        </div>
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
