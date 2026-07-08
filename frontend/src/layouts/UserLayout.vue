<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useFavoriteStore } from '@/stores/favorite';
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';

const auth = useAuthStore();
const cart = useCartStore();
const favoriteStore = useFavoriteStore();
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
function isSidebarVisible() {
  return mobileMenuOpen.value || sidebarHover.value || window.innerWidth > 768;
}

function logout() {
  favoriteStore.clear();
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Thông tin cá nhân', path: '/account/profile', icon: 'bi-person' },
  { label: 'Đơn hàng', path: '/account/orders', icon: 'bi-box' },
  { label: 'Lịch sử mua', path: '/account/history', icon: 'bi-clock-history' },
  { label: 'Món yêu thích', path: '/account/favorites', icon: 'bi-heart' },
  { label: 'Đổi mật khẩu', path: '/account/change-password', icon: 'bi-lock' },
];

onMounted(() => favoriteStore.fetchFavorites());
</script>

<template>
  <div class="user-layout">
    <nav class="top-bar">
      <div class="container">
        <div class="top-bar-inner">
          <router-link to="/" class="top-bar-brand">
            <span class="brand-text"
              >Fast<span class="brand-highlight">Guy</span></span
            >
          </router-link>
          <div class="top-bar-actions">
            <router-link to="/menu" class="top-bar-link">Thực đơn</router-link>
            <button
              class="top-bar-btn"
              type="button"
              aria-label="Mở giỏ hàng"
              @click="router.push('/cart')"
              title="Giỏ hàng"
            >
              <i class="bi bi-cart3"></i>
              <span v-if="cart.itemCount > 0" class="cart-badge">{{
                cart.itemCount
              }}</span>
            </button>
            <button class="top-bar-btn" type="button" aria-label="Đăng xuất" @click="logout" title="Đăng xuất">
              <i class="bi bi-box-arrow-right"></i>
            </button>
            <button
              class="mobile-toggle"
              type="button"
              aria-label="Mở menu tài khoản"
              @click="mobileMenuOpen = !mobileMenuOpen"
            >
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
            <img
              :src="
                auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'
              "
              class="sidebar-avatar"
              alt="Avatar người dùng"
            />
            <div>
              <div class="sidebar-name">{{ auth.user?.fullName }}</div>
              <div class="sidebar-email">{{ auth.user?.email }}</div>
            </div>
          </div>
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
        <div class="content" @click="mobileMenuOpen = false">
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
  background: #fff;
  border-bottom: 1px solid var(--border);
}
.top-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
}
.top-bar-brand {
  display: flex;
  align-items: center;
  gap: 6px;
}
.brand-text {
  font-size: 20px;
  font-weight: 800;
}
.brand-highlight {
  color: var(--primary);
}
.top-bar-link {
  font-size: 14px;
  color: var(--text-mid);
  padding: 0 12px;
}
.top-bar-link:hover {
  color: var(--text-dark);
}
.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.top-bar-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  font-size: 15px;
  color: var(--text-dark);
  transition: border-color 0.15s;
}
.top-bar-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.cart-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mobile-toggle {
  display: none;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 16px;
  cursor: pointer;
  color: var(--text-dark);
}
.layout-inner {
  display: flex;
  gap: 24px;
  padding: 24px 0;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
}
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-bottom: 8px;
}
.sidebar-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.sidebar-name {
  font-size: 14px;
  font-weight: 600;
}
.sidebar-email {
  font-size: 12px;
  color: var(--text-mid);
}
.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--text-mid);
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.sidebar-link:hover {
  background: var(--bg);
  color: var(--text-dark);
}
.sidebar-link.router-link-active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.sidebar-link i {
  font-size: 16px;
  width: 18px;
  text-align: center;
}
.content {
  flex: 1;
  min-width: 0;
}
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
    box-shadow: var(--shadow-lg);
    top: 52px;
    left: 0;
    bottom: 0;
    z-index: 99;
    padding: 12px;
    overflow-y: auto;
  }
  .sidebar.open {
    display: block;
  }
  .sidebar-user {
    margin-bottom: 8px;
  }
  .layout-inner {
    flex-direction: column;
  }
  .mobile-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
