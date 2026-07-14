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
const scrolled = ref(false);

function goCart() { router.push('/cart'); }
function logout() {
  favoriteStore.clear();
  auth.logout();
  router.push('/');
}

const navLinks = [
  { label: 'Trang chủ', path: '/' },
  { label: 'Thực đơn', path: '/menu' },
  { label: 'Khuyến mãi', path: '/promotions' },
  { label: 'Tra cứu đơn', path: '/track-order' },
];

onMounted(() => {
  if (auth.isLoggedIn) favoriteStore.fetchFavorites();
  window.addEventListener('scroll', () => { scrolled.value = window.scrollY > 10; });
});
</script>

<template>
  <div class="guest-layout fg-shell fg-shell-guest">
    <nav class="navbar" :class="{ scrolled }">
      <div class="container">
        <div class="nav-inner">
          <router-link to="/" class="brand">
            <span class="brand-text">Fast<span class="brand-accent">Guy</span></span>
          </router-link>
          <div class="nav-links" :class="{ open: mobileMenuOpen }">
            <router-link
              v-for="link in navLinks"
              :key="link.path"
              :to="link.path"
              class="nav-link"
              @click="mobileMenuOpen = false"
            >{{ link.label }}</router-link>
          </div>
          <div class="nav-actions">
            <button class="icon-btn" @click="goCart" title="Giỏ hàng">
              <i class="bi bi-bag"></i>
              <span v-if="cart.itemCount > 0" class="badge-dot">{{ cart.itemCount }}</span>
            </button>
            <template v-if="auth.isLoggedIn">
              <router-link v-if="auth.isUser" to="/account/profile" class="icon-btn" title="Tài khoản">
                <i class="bi bi-person"></i>
              </router-link>
              <router-link v-else-if="auth.isStaff" to="/staff" class="btn btn-sm btn-outline">Staff</router-link>
              <router-link v-else-if="auth.isAdmin" to="/admin" class="btn btn-sm btn-outline">Admin</router-link>
              <button class="logout-btn" @click="logout">
                <i class="bi bi-arrow-right-from-bracket"></i><span>Đăng xuất</span>
              </button>
            </template>
            <template v-else>
              <router-link to="/login" class="btn btn-sm btn-primary">Đăng nhập</router-link>
            </template>
            <button class="mobile-toggle" @click="mobileMenuOpen = !mobileMenuOpen">
              <i :class="mobileMenuOpen ? 'bi bi-x-lg' : 'bi bi-list'"></i>
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-brand">
            <span class="brand-text" style="font-size:20px">Fast<span class="brand-accent">Guy</span></span>
            <p>Thức ăn nhanh, giao tận nơi trong 30 phút.</p>
          </div>
          <div class="footer-col">
            <h4>Liên kết</h4>
            <router-link to="/">Trang chủ</router-link>
            <router-link to="/menu">Thực đơn</router-link>
            <router-link to="/track-order">Tra cứu đơn</router-link>
          </div>
          <div class="footer-col">
            <h4>Hỗ trợ</h4>
            <span>1900 1234</span>
            <span>support@fastguy.vn</span>
          </div>
        </div>
        <div class="footer-bottom">
          <span>&copy; 2024 FastGuy. Tất cả quyền được bảo lưu.</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.navbar {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid transparent;
  position: sticky;
  top: 0;
  z-index: 100;
  transition: all var(--transition-normal);
}
.navbar.scrolled {
  border-bottom-color: var(--border-light);
  box-shadow: var(--shadow-xs);
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 6px;
}
.brand-text {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--text-dark);
}
.brand-accent { color: var(--primary); }
.nav-links {
  display: flex;
  gap: 0;
}
.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-mid);
  padding: 0 18px;
  height: 60px;
  display: flex;
  align-items: center;
  transition: color var(--transition-fast);
  position: relative;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 20px;
  height: 2px;
  background: var(--primary);
  border-radius: 1px;
  transition: transform var(--transition-fast);
}
.nav-link:hover { color: var(--text-dark); }
.nav-link:hover::after { transform: translateX(-50%) scaleX(1); }
.nav-link.router-link-active {
  color: var(--primary);
  font-weight: 600;
}
.nav-link.router-link-active::after {
  transform: translateX(-50%) scaleX(1);
  width: 100%;
  max-width: 32px;
}
.nav-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  font-size: 17px;
  color: var(--text-mid);
  transition: all var(--transition-fast);
  border: none;
}
.icon-btn:hover {
  background: var(--surface);
  color: var(--text-dark);
}
.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text-mid);
  cursor: pointer;
  font-size: 13px;
  font-weight: 650;
}
.logout-btn:hover { border-color: var(--red-active); color: var(--red-active); }
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
  line-height: 1;
}
.mobile-toggle {
  display: none;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 18px;
  color: var(--text-mid);
  align-items: center;
  justify-content: center;
  border: none;
}
.main {
  min-height: calc(100vh - 200px);
}

.footer {
  background: #fff;
  border-top: 1px solid var(--border-light);
  margin-top: var(--space-6);
}
.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 48px;
  padding: 48px 0 32px;
}
.footer-brand p {
  color: var(--text-mid);
  font-size: 14px;
  margin-top: 12px;
  max-width: 280px;
}
.footer-col h4 {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-mid);
  margin-bottom: 16px;
}
.footer-col a,
.footer-col span {
  display: block;
  font-size: 14px;
  color: var(--text-mid);
  margin-bottom: 10px;
  transition: color var(--transition-fast);
}
.footer-col a:hover { color: var(--text-dark); }
.footer-bottom {
  border-top: 1px solid var(--border-light);
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-light);
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    background: rgba(255,255,255,0.98);
    backdrop-filter: blur(16px);
    flex-direction: column;
    border-bottom: 1px solid var(--border-light);
    z-index: 100;
    padding: 8px 16px;
    box-shadow: var(--shadow-md);
  }
  .nav-links.open { display: flex; }
  .nav-link {
    padding: 12px 16px;
    height: auto;
    border-radius: var(--radius-sm);
  }
  .nav-link::after { display: none; }
  .nav-link.router-link-active { background: var(--primary-50); border-radius: var(--radius-sm); }
  .mobile-toggle { display: flex; }
  .footer-grid { grid-template-columns: 1fr; gap: 32px; padding: 32px 0 20px; }
}
</style>
