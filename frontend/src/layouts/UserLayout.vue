<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useFavoriteStore } from '@/stores/favorite';
import { useNotificationStore } from '@/stores/notification';
import { useRouter } from 'vue-router';
import { onMounted, onUnmounted } from 'vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import AccountTabs from '@/components/common/AccountTabs.vue';

const auth = useAuthStore();
const cart = useCartStore();
const favoriteStore = useFavoriteStore();
const notificationStore = useNotificationStore();
const router = useRouter();

function logout() {
  favoriteStore.clear();
  notificationStore.reset();
  auth.logout();
  router.push('/');
}
onMounted(() => {
  favoriteStore.fetchFavorites();
  notificationStore.startPolling();
});
onUnmounted(() => notificationStore.stopPolling());
</script>

<template>
  <div class="user-layout fg-shell fg-shell-user">
    <a class="skip-link" href="#account-content">Bỏ qua đến nội dung</a>
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand" aria-label="FastGuy - Trang chủ">Fast<span>Guy</span></router-link>
        <nav class="header-nav" aria-label="Điều hướng nhanh">
          <router-link to="/menu" class="menu-link">Thực đơn</router-link>
          <NotificationBell />
          <router-link to="/cart" class="icon-btn" aria-label="Giỏ hàng">
            <i class="bi bi-bag" aria-hidden="true"></i>
            <span v-if="cart.itemCount" class="badge-dot" aria-hidden="true">{{ cart.itemCount }}</span>
            <span class="sr-only">{{ cart.itemCount }} sản phẩm</span>
          </router-link>
          <router-link to="/account/profile" class="account-link" aria-label="Tài khoản">
            <i class="bi bi-person-circle" aria-hidden="true"></i><span>{{ auth.user?.fullName || 'Tài khoản' }}</span>
          </router-link>
          <button class="logout-btn" aria-label="Đăng xuất" @click="logout"><i class="bi bi-box-arrow-right" aria-hidden="true"></i><span>Đăng xuất</span></button>
        </nav>
      </div>
    </header>
    <div class="container account-shell">
      <AccountTabs />
      <main id="account-content" class="content fg-page" tabindex="-1"><router-view /></main>
    </div>
  </div>
</template>

<style scoped>
.user-layout { min-height: 100vh; background: var(--color-bg); }
.skip-link { position: fixed; top: 8px; left: 8px; z-index: var(--z-toast); padding: 10px 14px; background: var(--color-text); color: white; border-radius: var(--radius-sm); transform: translateY(-150%); }
.skip-link:focus { transform: translateY(0); }
.site-header { position: sticky; top: 0; z-index: var(--z-header); height: var(--header-height); background: rgba(255,255,255,.9); backdrop-filter: blur(16px); border-bottom: 1px solid var(--color-border); box-shadow: var(--shadow-xs); }
.header-inner, .header-nav { height: 100%; display: flex; align-items: center; }
.header-inner { justify-content: space-between; }
.header-nav { gap: 6px; }
.brand { font-size: 22px; font-weight: 800; letter-spacing: -.5px; }
.brand span { color: var(--color-accent); }
.menu-link, .account-link, .logout-btn { min-height: var(--control-height); display: inline-flex; align-items: center; gap: 7px; padding: 0 12px; border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 14px; font-weight: 600; }
.menu-link:hover, .account-link:hover, .logout-btn:hover { color: var(--color-text); background: var(--color-surface-muted); }
.logout-btn:hover { color: var(--red-active); }
.icon-btn { width: var(--control-height); height: var(--control-height); display: inline-flex; align-items: center; justify-content: center; position: relative; border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 18px; }
.icon-btn:hover { background: var(--color-surface-muted); color: var(--color-text); }
.badge-dot { position: absolute; top: 2px; right: 1px; min-width: 18px; height: 18px; padding: 0 4px; display: grid; place-items: center; border-radius: var(--radius-full); background: var(--color-accent); color: white; font-size: 10px; font-weight: 700; }
.account-shell { padding-top: var(--space-6); padding-bottom: var(--space-8); }
.content { min-height: calc(100vh - 180px); padding-top: var(--space-5); }
@media (max-width: 700px) {
  .account-link span, .logout-btn span { display: none; }
  .account-link, .logout-btn { width: var(--control-height); padding: 0; justify-content: center; font-size: 18px; }
}
@media (max-width: 480px) { .menu-link { display: none; } }
</style>
