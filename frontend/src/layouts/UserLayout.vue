<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useFavoriteStore } from '@/stores/favorite';
import { useNotificationStore } from '@/stores/notification';
import { useRouter } from 'vue-router';
import { ref, onMounted, onUnmounted } from 'vue';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const cart = useCartStore();
const favoriteStore = useFavoriteStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const mobileMenuOpen = ref(false);
const sidebarHover = ref(false);
let hoverTimer = null;

function showSidebar() {
  clearTimeout(hoverTimer);
  if (window.innerWidth > 768) sidebarHover.value = true;
}
function hideSidebar() {
  hoverTimer = setTimeout(() => { sidebarHover.value = false; }, 300);
}

function logout() {
  favoriteStore.clear();
  notificationStore.reset();
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Thông tin cá nhân', path: '/account/profile', icon: 'bi-person' },
  { label: 'Đơn hàng', path: '/account/orders', icon: 'bi-box' },
  { label: 'Lịch sử mua', path: '/account/history', icon: 'bi-clock-history' },
  { label: 'Món yêu thích', path: '/account/favorites', icon: 'bi-heart' },
  { label: 'Thành viên', path: '/account/loyalty', icon: 'bi-stars' },
  { label: 'Hỗ trợ', path: '/account/support', icon: 'bi-headset' },
  { label: 'Đổi mật khẩu', path: '/account/change-password', icon: 'bi-lock' },
];

onMounted(() => {
  favoriteStore.fetchFavorites();
  notificationStore.startPolling();
});
onUnmounted(() => notificationStore.stopPolling());
</script>

<template>
  <div class="user-layout fg-shell fg-shell-user">
    <nav class="top-bar">
      <div class="container">
        <div class="top-bar-inner">
          <router-link to="/" class="top-bar-brand">
            <span class="brand-text">Fast<span class="brand-accent">Guy</span></span>
          </router-link>
          <div class="top-bar-actions">
            <router-link to="/menu" class="top-bar-link">Thực đơn</router-link>
            <NotificationBell />
            <button class="icon-btn" @click="router.push('/cart')" title="Giỏ hàng">
              <i class="bi bi-bag"></i>
              <span v-if="cart.itemCount > 0" class="badge-dot">{{ cart.itemCount }}</span>
            </button>
            <button class="logout-btn" @click="logout">
              <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
            </button>
            <button class="mobile-toggle" @click="mobileMenuOpen = !mobileMenuOpen">
              <i :class="mobileMenuOpen ? 'bi bi-x-lg' : 'bi bi-list'"></i>
            </button>
          </div>
        </div>
      </div>
    </nav>
    <div class="container">
      <div class="layout-inner" @mouseleave="hideSidebar">
        <div class="sidebar-trigger" @mouseenter="showSidebar"></div>
        <aside class="sidebar" :class="{ open: mobileMenuOpen || sidebarHover }" @mouseenter="showSidebar" @mouseleave="hideSidebar">
          <div class="sidebar-user">
            <img :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'" class="sidebar-avatar" />
            <div>
              <div class="sidebar-name">{{ auth.user?.fullName }}</div>
              <div class="sidebar-email">{{ auth.user?.email }}</div>
            </div>
          </div>
          <div class="fg-status-chip" style="margin-bottom:12px">My Route</div>
          <nav class="sidebar-nav">
            <router-link
              v-for="link in sidebarLinks"
              :key="link.path"
              :to="link.path"
              class="sidebar-link"
              @click="mobileMenuOpen = false"
            >
              <i :class="link.icon"></i>
              <span>{{ link.label }}</span>
            </router-link>
          </nav>
        </aside>
        <div class="content fg-page" @click="mobileMenuOpen = false">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-layout {
  min-height: 100vh;
  background: var(--bg);
}
.top-bar {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-light);
}
.top-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
}
.top-bar-brand { display: flex; align-items: center; }
.brand-text { font-size: 20px; font-weight: 800; letter-spacing: -0.5px; }
.brand-accent { color: var(--primary); }
.top-bar-link {
  font-size: 14px;
  color: var(--text-mid);
  padding: 0 12px;
  font-weight: 500;
  transition: color var(--transition-fast);
}
.top-bar-link:hover { color: var(--text-dark); }
.top-bar-actions {
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
  position: relative;
  font-size: 16px;
  color: var(--text-mid);
  transition: all var(--transition-fast);
}
.icon-btn:hover { background: var(--surface); color: var(--text-dark); }
.logout-btn { display:inline-flex; align-items:center; gap:6px; min-height:34px; padding:0 10px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff; color:var(--text-mid); cursor:pointer; font-size:13px; font-weight:650; }
.logout-btn:hover { border-color:var(--red-active); color:var(--red-active); }
.badge-dot {
  position: absolute;
  top: 4px;
  right: 2px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mobile-toggle {
  display: none;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 16px;
  cursor: pointer;
  color: var(--text-mid);
  align-items: center;
  justify-content: center;
}
.layout-inner {
  display: flex;
  gap: var(--space-6);
  padding: var(--space-6) 0;
}
.sidebar { width: 240px; flex-shrink: 0; }
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  margin-bottom: var(--space-3);
  background: #fff;
}
.sidebar-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}
.sidebar-name { font-size: 14px; font-weight: 600; }
.sidebar-email { font-size: 12px; color: var(--text-mid); }
.sidebar-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--text-mid);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  font-weight: 450;
}
.sidebar-link:hover { background: var(--surface); color: var(--text-dark); }
.sidebar-link.router-link-active {
  background: var(--primary-50);
  color: var(--primary);
  font-weight: 600;
}
.sidebar-link i {
  font-size: 16px;
  width: 18px;
  text-align: center;
}
.content { flex: 1; min-width: 0; }
@media (max-width: 1024px) {
  .sidebar { width: 200px; }
  .layout-inner { gap: 16px; }
}
@media (max-width: 768px) {
  .sidebar {
    display: none;
    position: fixed;
    width: 280px;
    background: #fff;
    box-shadow: var(--shadow-xl);
    top: 56px;
    left: 0;
    bottom: 0;
    z-index: 99;
    padding: 16px;
    overflow-y: auto;
    border-right: 1px solid var(--border-light);
  }
  .sidebar.open { display: block; }
  .layout-inner { flex-direction: column; padding: var(--space-4) 0; }
  .mobile-toggle { display: flex; }
}
</style>
