<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useFavoriteStore } from '@/stores/favorite';
import { useNotificationStore } from '@/stores/notification';
import NotificationBell from '@/components/common/NotificationBell.vue';

const auth = useAuthStore();
const cart = useCartStore();
const favoriteStore = useFavoriteStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const mobileMenuOpen = ref(false);
const scrolled = ref(false);
const menu = ref(null);
const toggle = ref(null);

const navLinks = [
  { label: 'Trang chủ', path: '/home' },
  { label: 'Thực đơn', path: '/menu' },
  { label: 'Khuyến mãi', path: '/promotions' },
  { label: 'Tra cứu đơn', path: '/track-order' },
];

function closeMenu(restoreFocus = false) {
  mobileMenuOpen.value = false;
  if (restoreFocus) nextTick(() => toggle.value?.focus());
}
function handleKeydown(event) {
  if (event.key === 'Escape' && mobileMenuOpen.value) closeMenu(true);
  if (event.key !== 'Tab' || !mobileMenuOpen.value || !menu.value) return;
  const focusable = [...menu.value.querySelectorAll('a, button')];
  if (!focusable.length) return;
  if (event.shiftKey && document.activeElement === focusable[0]) {
    event.preventDefault();
    focusable.at(-1).focus();
  } else if (!event.shiftKey && document.activeElement === focusable.at(-1)) {
    event.preventDefault();
    focusable[0].focus();
  }
}
function handleScroll() {
  scrolled.value = window.scrollY > 10;
}
function logout() {
  closeMenu();
  favoriteStore.clear();
  notificationStore.reset();
  auth.logout();
  router.push('/');
}

watch(mobileMenuOpen, async (open) => {
  document.body.style.overflow = open ? 'hidden' : '';
  if (open) {
    await nextTick();
    menu.value?.querySelector('a')?.focus();
  }
});
onMounted(() => {
  if (auth.isUser) notificationStore.startPolling();
  window.addEventListener('scroll', handleScroll, { passive: true });
  document.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => {
  document.body.style.overflow = '';
  notificationStore.stopPolling();
  window.removeEventListener('scroll', handleScroll);
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <header class="site-header" :class="{ scrolled }">
    <div class="container header-inner">
      <router-link to="/home" class="brand" aria-label="FastGuy - Trang chủ">Fast<span>Guy</span></router-link>
      <nav ref="menu" id="public-navigation" class="nav-links" :class="{ open: mobileMenuOpen }" aria-label="Điều hướng chính">
        <div class="public-links">
          <router-link v-for="link in navLinks" :key="link.path" :to="link.path" class="nav-link" @click="closeMenu()">{{ link.label }}</router-link>
        </div>
        <div class="mobile-account-actions">
          <router-link v-if="auth.isUser" to="/account/notifications" class="drawer-action" @click="closeMenu()"><i class="bi bi-bell" aria-hidden="true"></i> Thông báo</router-link>
          <router-link v-if="auth.isUser" to="/account/overview" class="drawer-action" @click="closeMenu()"><i class="bi bi-person-circle" aria-hidden="true"></i> {{ auth.user?.fullName || 'Tài khoản' }}</router-link>
          <router-link v-else-if="auth.isStaff" to="/staff" class="drawer-action" @click="closeMenu()">Khu vực Staff</router-link>
          <router-link v-else-if="auth.isAdmin" to="/admin" class="drawer-action" @click="closeMenu()">Khu vực Admin</router-link>
          <button v-if="auth.isLoggedIn" class="drawer-action logout" @click="logout"><i class="bi bi-box-arrow-right" aria-hidden="true"></i> Đăng xuất</button>
          <router-link v-else to="/" class="login-btn" @click="closeMenu()">Đăng nhập</router-link>
        </div>
      </nav>
      <div class="nav-actions">
        <NotificationBell v-if="auth.isUser" />
        <router-link to="/cart" class="cart-summary" :aria-label="`Giỏ hàng, ${cart.itemCount} món`">
          <i class="bi bi-bag" aria-hidden="true"></i>
          <span class="cart-copy"><strong>Giỏ hàng</strong><small>{{ cart.itemCount }} món</small></span>
          <span v-if="cart.itemCount" class="badge-dot" aria-hidden="true">{{ cart.itemCount }}</span>
        </router-link>
        <router-link v-if="auth.isUser" to="/account/overview" class="account-link" aria-label="Tài khoản"><i class="bi bi-person-circle" aria-hidden="true"></i><span>{{ auth.user?.fullName || 'Tài khoản' }}</span></router-link>
        <router-link v-else-if="auth.isStaff" to="/staff" class="role-link">Staff</router-link>
        <router-link v-else-if="auth.isAdmin" to="/admin" class="role-link">Admin</router-link>
        <button v-if="auth.isLoggedIn" class="logout-btn" aria-label="Đăng xuất" @click="logout"><i class="bi bi-box-arrow-right" aria-hidden="true"></i><span>Đăng xuất</span></button>
        <router-link v-else to="/" class="login-btn">Đăng nhập</router-link>
        <button ref="toggle" class="mobile-toggle" aria-label="Mở menu" aria-controls="public-navigation" :aria-expanded="mobileMenuOpen" @click="mobileMenuOpen = !mobileMenuOpen"><i :class="mobileMenuOpen ? 'bi bi-x-lg' : 'bi bi-list'" aria-hidden="true"></i></button>
      </div>
    </div>
  </header>
  <button v-if="mobileMenuOpen" class="nav-backdrop" aria-label="Đóng menu" @click="closeMenu(true)"></button>
</template>

<style scoped>
.site-header { position: sticky; top: 0; z-index: var(--z-header); height: var(--header-height); background: rgba(255,255,255,.94); backdrop-filter: blur(16px); border-bottom: 1px solid transparent; transition: box-shadow var(--transition-fast), border-color var(--transition-fast); }
.site-header.scrolled { border-color: var(--color-border); box-shadow: var(--shadow-sm); }
.header-inner { height: 100%; display: grid; grid-template-columns: minmax(120px, 1fr) auto minmax(300px, 1fr); align-items: center; gap: 18px; }
.brand { justify-self: start; font-size: 22px; font-weight: 800; letter-spacing: -.5px; }
.brand span { color: var(--color-accent); }
.nav-links, .public-links, .nav-actions { display: flex; align-items: center; }
.public-links { justify-content: center; }
.nav-link { min-height: var(--control-height); display: flex; align-items: center; padding: 0 14px; border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 14px; font-weight: 600; white-space: nowrap; }
.nav-link:hover { color: var(--color-text); background: var(--color-surface-muted); }
.nav-link.router-link-active { color: var(--color-accent); background: #fff7f2; }
.nav-actions { justify-self: end; gap: 6px; }
.cart-summary { position: relative; min-height: 44px; display: flex; align-items: center; gap: 9px; padding: 5px 14px 5px 11px; border: 1px solid var(--color-border); border-radius: var(--radius-full); color: var(--color-text); background: #fff; }
.cart-summary > i { color: var(--color-accent); font-size: 17px; }
.cart-copy { display: flex; flex-direction: column; line-height: 1.1; }
.cart-copy strong { font-size: 11px; }
.cart-copy small { margin-top: 3px; color: var(--color-text-muted); font-size: 9px; }
.cart-summary:hover { border-color: var(--color-accent); box-shadow: 0 7px 18px rgba(43,25,15,.07); }
.badge-dot { position: absolute; top: -5px; right: -3px; min-width: 18px; height: 18px; padding: 0 4px; display: grid; place-items: center; border-radius: var(--radius-full); background: var(--color-accent); color: white; font-size: 10px; font-weight: 700; }
.account-link, .role-link, .logout-btn, .login-btn, .mobile-toggle { min-height: var(--control-height); display: inline-flex; align-items: center; justify-content: center; gap: 7px; padding: 0 12px; border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 13px; font-weight: 700; }
.account-link { max-width: 150px; background: var(--color-surface-muted); }
.account-link span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-link:hover, .role-link:hover, .logout-btn:hover, .mobile-toggle:hover { color: var(--color-text); background: #f3ebe6; }
.logout-btn { width: var(--control-height); padding: 0; font-size: 18px; }
.logout-btn span { display: none; }
.logout-btn:hover { color: var(--red-active); }
.login-btn { color: white; background: var(--color-accent); }
.login-btn:hover { color: white; filter: brightness(.94); }
.mobile-toggle, .mobile-account-actions, .nav-backdrop { display: none; }
@media (max-width: 1120px) {
  .header-inner { grid-template-columns: auto 1fr auto; gap: 10px; }
  .nav-link { padding-inline: 10px; }
  .account-link { width: var(--control-height); padding: 0; font-size: 18px; }
  .account-link span { display: none; }
  .cart-copy { display: none; }
  .cart-summary { width: var(--control-height); padding: 0; justify-content: center; border: 0; }
  .cart-summary > i { color: var(--color-text-muted); font-size: 18px; }
}
@media (max-width: 820px) {
  .header-inner { display: flex; justify-content: space-between; }
  .nav-links { position: fixed; top: var(--header-height); right: 0; bottom: 0; z-index: var(--z-drawer); display: none; width: min(340px, 88vw); height: calc(100dvh - var(--header-height)); overflow-y: auto; padding: 18px; flex-direction: column; align-items: stretch; background: var(--color-surface); box-shadow: var(--shadow-xl); }
  .nav-links.open { display: flex; }
  .public-links { flex-direction: column; align-items: stretch; }
  .nav-link { min-height: 48px; padding: 0 14px; }
  .mobile-account-actions { display: grid; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--color-border); }
  .drawer-action { width: 100%; min-height: 46px; display: flex; align-items: center; gap: 10px; padding: 0 14px; border-radius: var(--radius-sm); color: var(--color-text); background: var(--color-surface-muted); font-size: 14px; font-weight: 700; }
  .drawer-action.logout { color: var(--red-active); }
  .mobile-account-actions .login-btn { width: 100%; }
  .nav-actions > :not(.cart-summary):not(.mobile-toggle) { display: none; }
  .mobile-toggle { width: var(--control-height); padding: 0; display: inline-flex; font-size: 19px; }
  .nav-backdrop { display: block; position: fixed; inset: var(--header-height) 0 0; z-index: var(--z-backdrop); width: 100%; background: rgba(0,0,0,.45); }
}
</style>
