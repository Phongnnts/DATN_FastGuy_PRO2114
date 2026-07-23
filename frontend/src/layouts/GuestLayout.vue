<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useFavoriteStore } from '@/stores/favorite';
import { useRouter } from 'vue-router';
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

const auth = useAuthStore();
const cart = useCartStore();
const favoriteStore = useFavoriteStore();
const router = useRouter();
const mobileMenuOpen = ref(false);
const scrolled = ref(false);
const menu = ref(null);
const toggle = ref(null);
const year = new Date().getFullYear();

const navLinks = [
  { label: 'Trang chủ', path: '/' },
  { label: 'Thực đơn', path: '/menu' },
  { label: 'Khuyến mãi', path: '/promotions' },
  { label: 'Tra cứu đơn', path: '/track-order' },
];

function logout() {
  favoriteStore.clear();
  auth.logout();
  router.push('/');
}
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
function handleScroll() { scrolled.value = window.scrollY > 10; }

watch(mobileMenuOpen, async (open) => {
  document.body.style.overflow = open ? 'hidden' : '';
  if (open) {
    await nextTick();
    menu.value?.querySelector('a')?.focus();
  }
});
onMounted(() => {
  if (auth.isLoggedIn) favoriteStore.fetchFavorites();
  window.addEventListener('scroll', handleScroll, { passive: true });
  document.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => {
  document.body.style.overflow = '';
  window.removeEventListener('scroll', handleScroll);
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div class="guest-layout fg-shell fg-shell-guest">
    <a class="skip-link" href="#main-content">Bỏ qua đến nội dung</a>
    <header class="site-header" :class="{ scrolled }">
      <div class="container header-inner">
        <router-link to="/" class="brand" aria-label="FastGuy - Trang chủ">Fast<span>Guy</span></router-link>
        <nav ref="menu" id="guest-navigation" class="nav-links" :class="{ open: mobileMenuOpen }" aria-label="Điều hướng chính">
          <router-link v-for="link in navLinks" :key="link.path" :to="link.path" class="nav-link" @click="closeMenu()">{{ link.label }}</router-link>
        </nav>
        <div class="nav-actions">
          <router-link to="/cart" class="icon-btn" aria-label="Giỏ hàng">
            <i class="bi bi-bag" aria-hidden="true"></i>
            <span v-if="cart.itemCount" class="badge-dot" aria-hidden="true">{{ cart.itemCount }}</span>
            <span class="sr-only">{{ cart.itemCount }} sản phẩm</span>
          </router-link>
          <template v-if="auth.isLoggedIn">
            <router-link v-if="auth.isUser" to="/account/profile" class="icon-btn" aria-label="Tài khoản"><i class="bi bi-person" aria-hidden="true"></i></router-link>
            <router-link v-else-if="auth.isStaff" to="/staff" class="btn btn-sm btn-outline">Staff</router-link>
            <router-link v-else-if="auth.isAdmin" to="/admin" class="btn btn-sm btn-outline">Admin</router-link>
            <button class="logout-btn" @click="logout"><i class="bi bi-box-arrow-right" aria-hidden="true"></i><span>Đăng xuất</span></button>
          </template>
          <router-link v-else to="/login" class="btn btn-sm btn-primary">Đăng nhập</router-link>
          <button ref="toggle" class="mobile-toggle" aria-label="Mở menu" aria-controls="guest-navigation" :aria-expanded="mobileMenuOpen" @click="mobileMenuOpen = !mobileMenuOpen"><i :class="mobileMenuOpen ? 'bi bi-x-lg' : 'bi bi-list'" aria-hidden="true"></i></button>
        </div>
      </div>
    </header>
    <button v-if="mobileMenuOpen" class="nav-backdrop" aria-label="Đóng menu" @click="closeMenu(true)"></button>
    <main id="main-content" class="main" tabindex="-1"><router-view /></main>
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-about"><div class="brand">Fast<span>Guy</span></div><p>Đặt đồ ăn nhanh, giao tận nơi trong 30 phút. Thực đơn đa dạng, thanh toán tiện lợi.</p></div>
          <div class="footer-col"><h2>Về FastGuy</h2><router-link to="/">Trang chủ</router-link><router-link to="/menu">Thực đơn</router-link><router-link to="/promotions">Khuyến mãi</router-link></div>
          <div class="footer-col"><h2>Liên kết</h2><router-link to="/">Trang chủ</router-link><router-link to="/menu">Thực đơn</router-link><router-link to="/track-order">Tra cứu đơn</router-link></div>
          <div class="footer-col"><h2>Hỗ trợ</h2><a href="tel:19001234">1900 1234</a><a href="mailto:support@fastguy.vn">support@fastguy.vn</a></div>
        </div>
        <div class="footer-bottom">&copy; {{ year }} FastGuy. Tất cả quyền được bảo lưu.</div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.skip-link { position: fixed; top: 8px; left: 8px; z-index: var(--z-toast); padding: 10px 14px; background: var(--color-text); color: var(--text-inverse); border-radius: var(--radius-sm); transform: translateY(-150%); }
.skip-link:focus { transform: translateY(0); }
.site-header { position: sticky; top: 0; z-index: var(--z-header); height: var(--header-height); background: rgba(255,255,255,.9); backdrop-filter: blur(16px); border-bottom: 1px solid transparent; transition: box-shadow var(--transition-fast), border-color var(--transition-fast); }
.site-header.scrolled { border-color: var(--color-border); box-shadow: var(--shadow-sm); }
.header-inner { height: 100%; display: flex; align-items: center; justify-content: space-between; }
.brand { font-size: 22px; font-weight: 800; letter-spacing: -.5px; }
.brand span { color: var(--color-accent); }
.nav-links, .nav-actions { display: flex; align-items: center; }
.nav-link { min-height: var(--control-height); display: flex; align-items: center; padding: 0 16px; color: var(--color-text-muted); font-size: 14px; font-weight: 600; border-radius: var(--radius-sm); }
.nav-link:hover { color: var(--color-text); background: var(--color-surface-muted); }
.nav-link.router-link-active { color: var(--color-accent); }
.nav-actions { gap: 6px; }
.icon-btn, .mobile-toggle { width: var(--control-height); height: var(--control-height); display: inline-flex; align-items: center; justify-content: center; position: relative; border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 18px; }
.icon-btn:hover, .mobile-toggle:hover { background: var(--color-surface-muted); color: var(--color-text); }
.badge-dot { position: absolute; top: 2px; right: 1px; min-width: 18px; height: 18px; padding: 0 4px; display: grid; place-items: center; border-radius: var(--radius-full); background: var(--color-accent); color: white; font-size: 10px; font-weight: 700; }
.logout-btn { min-height: var(--control-height); display: inline-flex; align-items: center; gap: 7px; padding: 0 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); color: var(--color-text-muted); }
.logout-btn:hover { color: var(--red-active); border-color: var(--red-active); }
.mobile-toggle { display: none; }
.main { min-height: calc(100vh - 260px); }
.footer { margin-top: var(--space-8); background: var(--color-surface); border-top: 1px solid var(--color-border); }
.footer-grid { display: grid; grid-template-columns: 1.5fr 1fr 1fr 1fr; gap: 36px; padding: 48px 0 32px; }
.footer-about p { color: var(--color-text-muted); font-size: 14px; line-height: 1.6; }
.footer p, .footer-col a { color: var(--color-text-muted); font-size: 14px; }
.footer p { margin-top: 12px; }
.footer-col h2 { margin-bottom: 14px; color: var(--color-text); font-size: 13px; text-transform: uppercase; }
.footer-col a { display: block; min-height: 32px; }
.footer-col a:hover { color: var(--color-accent); }
.footer-bottom { padding: 20px 0; border-top: 1px solid var(--border-light); text-align: center; color: var(--text-light); font-size: 13px; }
.nav-backdrop { display: none; }
@media (max-width: 768px) {
  .nav-links { position: fixed; top: var(--header-height); right: 0; bottom: 0; z-index: var(--z-drawer); width: min(320px, 86vw); padding: 16px; flex-direction: column; align-items: stretch; background: var(--color-surface); box-shadow: var(--shadow-xl); transform: translateX(105%); transition: transform var(--transition-normal); }
  .nav-links.open { transform: translateX(0); }
  .nav-link { padding: 0 16px; }
  .mobile-toggle { display: inline-flex; }
  .nav-backdrop { display: block; position: fixed; inset: var(--header-height) 0 0; z-index: var(--z-backdrop); width: 100%; background: rgba(0,0,0,.45); }
  .logout-btn span { display: none; }
  .logout-btn { width: var(--control-height); padding: 0; justify-content: center; }
  .footer-grid { grid-template-columns: 1fr; gap: 28px; padding: 32px 0 20px; }
}
@media (max-width: 480px) { .nav-actions > .btn { padding-inline: 10px; } }
</style>
