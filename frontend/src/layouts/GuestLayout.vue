<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useRouter } from 'vue-router';
import { ref } from 'vue';

const auth = useAuthStore();
const cart = useCartStore();
const router = useRouter();
const mobileMenuOpen = ref(false);

function goCart() {
  router.push('/cart');
}
function goLogin() {
  router.push('/login');
}
function logout() {
  auth.logout();
  router.push('/');
}

const navLinks = [
  { label: 'Trang chủ', path: '/' },
  { label: 'Thực đơn', path: '/menu' },
  { label: 'Tra cứu đơn', path: '/track-order' },
];
</script>

<template>
  <div class="guest-layout">
    <nav class="navbar">
      <div class="container">
        <div class="nav-inner">
          <router-link to="/" class="brand">
            <span class="brand-text"
              >Fast<span class="brand-highlight">Guy</span></span
            >
          </router-link>
          <div class="nav-links" :class="{ open: mobileMenuOpen }">
            <router-link
              v-for="link in navLinks"
              :key="link.path"
              :to="link.path"
              class="nav-link"
              @click="mobileMenuOpen = false"
              >{{ link.label }}</router-link
            >
          </div>
          <div class="nav-actions">
            <button class="nav-icon-btn" @click="goCart" title="Giỏ hàng">
              <i class="bi bi-cart3"></i>
              <span v-if="cart.itemCount > 0" class="cart-badge">{{
                cart.itemCount
              }}</span>
            </button>
            <template v-if="auth.isLoggedIn">
              <router-link
                v-if="auth.isUser"
                to="/account/profile"
                class="nav-link"
                title="Tài khoản"
                ><i class="bi bi-person"></i
              ></router-link>
              <router-link
                v-else-if="auth.isStaff"
                to="/staff"
                class="btn btn-sm"
                >Staff</router-link
              >
              <router-link
                v-else-if="auth.isAdmin"
                to="/admin"
                class="btn btn-sm"
                >Admin</router-link
              >
              <button class="btn btn-sm btn-ghost" @click="logout">
                <i class="bi bi-box-arrow-right"></i>
              </button>
            </template>
            <template v-else>
              <router-link to="/login" class="btn btn-sm btn-primary"
                >Đăng nhập</router-link
              >
            </template>
            <button
              class="mobile-toggle"
              @click="mobileMenuOpen = !mobileMenuOpen"
            >
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
        <div class="footer-inner">
          <div class="footer-col">
            <p class="footer-brand">FastGuy</p>
            <p class="footer-text">
              Thức ăn nhanh, giao tận nơi trong 30 phút.
            </p>
          </div>
          <div class="footer-col">
            <router-link to="/" class="footer-link">Trang chủ</router-link>
            <router-link to="/menu" class="footer-link">Thực đơn</router-link>
            <router-link to="/track-order" class="footer-link"
              >Tra cứu đơn</router-link
            >
          </div>
          <div class="footer-col">
            <span class="footer-link">1900 1234</span>
            <span class="footer-link">support@fastguy.vn</span>
          </div>
        </div>
        <div class="footer-bottom">&copy; 2024 FastGuy</div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.navbar {
  background: #fff;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 6px;
}
.brand-text {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.3px;
}
.brand-highlight {
  color: var(--primary);
}
.nav-links {
  display: flex;
  gap: 2px;
  height: 100%;
}
.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-mid);
  padding: 0 16px;
  display: flex;
  align-items: center;
  transition: color 0.15s;
  border-bottom: 2px solid transparent;
}
.nav-link:hover {
  color: var(--text-dark);
}
.nav-link.router-link-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}
.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  font-size: 16px;
  color: var(--text-dark);
  transition: border-color 0.15s;
}
.nav-icon-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.cart-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 17px;
  height: 17px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mobile-toggle {
  display: none;
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-dark);
}
.main {
  min-height: calc(100vh - 180px);
}
.footer {
  background: var(--bg);
  border-top: 1px solid var(--border);
  padding: 32px 0 16px;
  margin-top: 60px;
}
.footer-inner {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 32px;
  margin-bottom: 24px;
}
.footer-brand {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}
.footer-text {
  font-size: 13px;
  color: var(--text-mid);
}
.footer-link {
  display: block;
  font-size: 13px;
  color: var(--text-mid);
  margin-bottom: 6px;
  transition: color 0.15s;
}
.footer-link:hover {
  color: var(--text-dark);
}
.footer-bottom {
  border-top: 1px solid var(--border);
  padding-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-light);
}
@media (max-width: 768px) {
  .nav-links {
    display: none;
    position: absolute;
    top: 56px;
    left: 0;
    right: 0;
    background: #fff;
    flex-direction: column;
    border-bottom: 1px solid var(--border);
    z-index: 100;
    padding: 8px 0;
    height: auto;
  }
  .nav-links.open {
    display: flex;
  }
  .nav-link {
    padding: 10px 24px;
    border-bottom: none;
  }
  .nav-link.router-link-active {
    border-bottom: none;
    background: var(--primary-light);
  }
  .mobile-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .footer-inner {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
