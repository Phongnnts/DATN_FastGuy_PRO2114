<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter, useRoute } from 'vue-router';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { shiftApi } from '@/api';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const shiftState = ref('UNKNOWN');
const checkedIn = computed(() => shiftState.value === 'CHECKED_IN');
const chipLabel = computed(() => shiftState.value === 'CHECKED_IN' ? 'Tuyến đang giao' : shiftState.value === 'CHECKED_OUT' ? 'Ca đã kết thúc' : 'Chưa check-in');

let shiftSequence = 0;

const desktopNavItems = [
  { path: '/shipper', name: 'Trang chủ', icon: 'bi-house-door' },
  { path: '/shipper/orders', name: 'Đơn giao', icon: 'bi-bicycle' },
  { path: '/shipper/history', name: 'Lịch sử', icon: 'bi-clock-history' },
  { path: '/shipper/shifts', name: 'Ca làm', icon: 'bi-calendar-week' },
  { path: '/shipper/cash', name: 'COD', icon: 'bi-cash-coin' },
  { path: '/shipper/profile', name: 'Hồ sơ', icon: 'bi-person-circle' },
];
const mobileNavItems = desktopNavItems.filter(item => item.path !== '/shipper/shifts');

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
});
onUnmounted(() => {
  window.removeEventListener('staff-shift-changed', checkShift);
});

function logout() {
  auth.logout();
  router.push('/');
}
</script>

<template>
  <div class="shipper-layout fg-shell fg-shell-shipper">
    <a class="skip-link" href="#shipper-main">Bỏ qua đến nội dung chính</a>
    <aside class="shipper-sidebar">
      <div class="sidebar-brand"><span>Fast<span class="accent">Guy</span></span><small>FIELD COMMAND</small></div>
      <div class="sidebar-shift"><span class="status-dot" :class="{ active: checkedIn }"></span><div><strong>{{ chipLabel }}</strong><small>Trạng thái vận hành</small></div></div>
      <nav class="sidebar-nav" aria-label="Điều hướng Shipper desktop">
        <router-link v-for="item in desktopNavItems" :key="item.path" :to="item.path" class="sidebar-nav-item" :class="activeClass(item.path)" :aria-current="activeClass(item.path) ? 'page' : undefined">
          <i :class="`bi ${item.icon}`" aria-hidden="true"></i><span>{{ item.name }}</span>
        </router-link>
      </nav>
      <button class="sidebar-logout" @click="logout"><i class="bi bi-arrow-right-from-bracket" aria-hidden="true"></i><span>Đăng xuất</span></button>
    </aside>
    <div class="shipper-workspace">
    <header class="shipper-header">
      <div class="shipper-brand">
        <span>Fast<span class="accent">Guy</span></span>
        <span class="role-badge">Shipper</span>
        <span class="fg-status-chip">{{ chipLabel }}</span>
      </div>
      <div class="header-actions">
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
      <router-view v-if="shiftState !== 'UNKNOWN' || $route.name === 'ShipperShifts' || $route.name === 'ShipperOrderHistory' || $route.name === 'ShipperCash' || $route.name === 'ShipperProfile'" aria-live="polite" role="region" />
    </main>
    <nav class="shipper-bottom-nav" aria-label="Điều hướng Shipper">
      <router-link
        v-for="item in mobileNavItems"
        :key="item.path"
        :to="item.path"
        class="shipper-nav-item"
        :class="activeClass(item.path)"
        :aria-current="activeClass(item.path) ? 'page' : undefined"
      >
        <i :class="`bi ${item.icon}`" aria-hidden="true"></i>
        <span>{{ item.name }}</span>
      </router-link>
    </nav>
    </div>
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
  min-height: 100dvh;
  background:#f3f5f7;
}
.shipper-workspace { display:flex; flex-direction:column; min-height:100dvh; max-width:480px; margin:0 auto; background:#f3f5f7; }
.shipper-sidebar { display:none; }
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
.logout-btn { display:inline-flex; align-items:center; gap:5px; min-height:44px; padding:0 12px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:12px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.shipper-main {
  flex: 1;
  padding: 16px;
  padding-bottom: calc(84px + env(safe-area-inset-bottom));
  overflow-y: auto;
}
.shipper-bottom-nav {
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
@media (min-width: 900px) {
  .shipper-layout { display:grid; grid-template-columns:248px minmax(0,1fr); }
  .shipper-sidebar { position:sticky; top:0; display:flex; flex-direction:column; min-height:100dvh; height:100dvh; padding:28px 18px 20px; background:#172033; color:#fff; }
  .sidebar-brand { display:flex; flex-direction:column; padding:0 10px 24px; font-size:24px; font-weight:850; letter-spacing:-.04em; }
  .sidebar-brand small { margin-top:4px; color:#94a3b8; font-size:9px; letter-spacing:.18em; }
  .sidebar-shift { display:flex; align-items:center; gap:10px; padding:13px; border:1px solid #334155; border-radius:12px; background:#1e293b; }
  .sidebar-shift strong,.sidebar-shift small { display:block; }.sidebar-shift strong { font-size:12px; }.sidebar-shift small { margin-top:2px; color:#94a3b8; font-size:10px; }
  .status-dot { width:9px; height:9px; border-radius:50%; background:#f59e0b; box-shadow:0 0 0 4px rgba(245,158,11,.12); }.status-dot.active { background:#22c55e; box-shadow:0 0 0 4px rgba(34,197,94,.12); }
  .sidebar-nav { display:grid; gap:5px; margin-top:24px; }
  .sidebar-nav-item,.sidebar-logout { display:flex; align-items:center; gap:12px; min-height:44px; padding:0 13px; border:0; border-radius:10px; color:#aeb9c9; background:transparent; text-decoration:none; font:inherit; font-size:13px; font-weight:650; cursor:pointer; }
  .sidebar-nav-item i,.sidebar-logout i { width:20px; font-size:17px; text-align:center; }
  .sidebar-nav-item:hover,.sidebar-nav-item.active { background:#263348; color:#fff; }.sidebar-nav-item.active { box-shadow:inset 3px 0 #ef4444; }
  .sidebar-logout { margin-top:auto; width:100%; color:#fca5a5; }.sidebar-logout:hover { background:#3b2630; color:#fff; }
  .shipper-workspace { max-width:none; margin:0; min-width:0; }
  .shipper-header { padding:0 32px; height:68px; }
  .shipper-brand > span:first-child,.role-badge { display:none; }
  .shipper-main { width:100%; max-width:1280px; margin:0 auto; padding:28px 32px 40px; overflow:visible; }
  .shipper-bottom-nav { display:none; }
}
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
