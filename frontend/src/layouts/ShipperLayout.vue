<script setup>
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { useRouter, useRoute } from 'vue-router';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { shiftApi } from '@/api';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const route = useRoute();
const shiftState = ref('UNKNOWN');
const checkedIn = computed(() => shiftState.value === 'CHECKED_IN');
const chipLabel = computed(() => shiftState.value === 'CHECKED_IN' ? 'Tuyến đang giao' : shiftState.value === 'CHECKED_OUT' ? 'Ca đã kết thúc' : 'Chưa check-in');

let shiftSequence = 0;

const navItems = [
  { path: '/shipper', name: 'Trang chủ', icon: 'bi-house-door' },
  { path: '/shipper/orders', name: 'Đơn giao', icon: 'bi-bicycle' },
  { path: '/shipper/history', name: 'Lịch sử', icon: 'bi-clock-history' },
  { path: '/shipper/shifts', name: 'Ca làm', icon: 'bi-calendar-week' },
  { path: '/shipper/cash', name: 'COD', icon: 'bi-cash-coin' },
];

function activeClass(path) {
  if (path === '/shipper/orders' && route.path.startsWith('/shipper/orders/')) return 'active';
  return route.path === path ? 'active' : '';
}

async function checkShift() {
  const token = ++shiftSequence;
  shiftState.value = 'UNKNOWN';
  try {
    const data = await shiftApi.getCurrent();
    if (token !== shiftSequence) return;
    shiftState.value = data?.state || 'UNKNOWN';
  } catch { if (token !== shiftSequence) return; shiftState.value = 'UNKNOWN'; }
}

onMounted(async () => {
  window.addEventListener('staff-shift-changed', checkShift);
  await checkShift();
  notificationStore.startPolling();
});
onUnmounted(() => {
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
  <div class="shipper-layout fg-shell fg-shell-shipper">
    <a class="skip-link" href="#shipper-main">Bỏ qua đến nội dung chính</a>
    <header class="shipper-header">
      <div class="shipper-brand">
        <span>Fast<span class="accent">Guy</span></span>
        <span class="role-badge">Shipper</span>
        <span class="fg-status-chip">{{ chipLabel }}</span>
      </div>
      <div class="header-actions">
        <NotificationBell />
        <button class="logout-btn" @click="logout">
          <i class="bi bi-arrow-right-from-bracket"></i><span>Thoát</span>
        </button>
      </div>
    </header>
    <main id="shipper-main" class="shipper-main fg-page" tabindex="-1">
      <div v-if="!checkedIn" class="no-shift-banner">
        <i class="bi bi-calendar-x"></i>
        <div v-if="shiftState === 'UNKNOWN'">
          <strong>Không thể xác định trạng thái ca</strong>
          <p>Thử lại hoặc mở lịch làm việc.</p>
          <button class="btn btn-outline btn-sm" @click="checkShift">Thử lại</button>
        </div>
        <div v-else-if="shiftState === 'CHECKED_OUT'">
          <strong>Ca làm đã kết thúc</strong>
          <p>Các nghiệp vụ đang bị khóa.</p>
        </div>
        <div v-else>
          <strong>Bạn chưa check-in</strong>
          <p>Vui lòng xem lịch làm việc để check-in đúng giờ.</p>
        </div>
      </div>
      <router-view v-if="shiftState !== 'UNKNOWN' || $route.name === 'ShipperShifts' || $route.name === 'ShipperOrderHistory' || $route.name === 'ShipperCash'" aria-live="polite" role="region" />
    </main>
    <nav class="shipper-nav" aria-label="Điều hướng Shipper">
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
.skip-link {
  position: fixed;
  top: 8px;
  left: 8px;
  z-index: 100;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: var(--text-dark);
  color: #fff;
  font-weight: 700;
  text-decoration: none;
  transform: translateY(-150%);
}
.skip-link:focus { transform: translateY(0); }
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
  padding-bottom: calc(84px + env(safe-area-inset-bottom));
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
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -1px 3px rgba(0,0,0,0.04);
}
.shipper-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height:44px;
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
.no-shift-banner { display:flex; align-items:center; gap:12px; padding:14px 16px; margin-bottom:16px; border-radius:var(--radius); background:#fef3c7; color:#92400e; font-size:14px; }
.no-shift-banner i { font-size:28px; flex-shrink:0; }
.no-shift-banner strong { display:block; margin-bottom:2px; }
.no-shift-banner p { margin:0; font-size:13px; opacity:.8; }
@media(max-width:360px) {
  .shipper-header { padding:0 10px; }
  .shipper-brand { gap:6px; font-size:16px; }
  .role-badge { display:none; }
  .logout-btn span { display:none; }
  .logout-btn { min-width:44px; min-height:44px; justify-content:center; }
  .shipper-main { padding:12px; padding-bottom:calc(82px + env(safe-area-inset-bottom)); }
  .shipper-nav-item { font-size:10px; }
  .shipper-nav-item i { font-size:18px; }
}
</style>
