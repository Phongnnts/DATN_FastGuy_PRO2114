<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const cart = useCartStore();

const email = ref('');
const password = ref('');
const error = ref('');
const loading = ref(false);

async function handleLogin() {
  error.value = '';
  loading.value = true;
  try {
    const user = await auth.login(email.value, password.value);
    cart.migrateToUser();
    const role = user?.role || '';
    const redirect =
      route.query.redirect ||
      (role === 'USER' ? '/' : `/${role.toLowerCase()}`);
    router.push(redirect);
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="container">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Đăng nhập</h2>
          <p>Chào mừng bạn quay trở lại FastGuy!</p>
        </div>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              v-model="email"
              type="email"
              class="form-input"
              placeholder="your@email.com"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Mật khẩu</label>
            <input
              v-model="password"
              type="password"
              class="form-input"
              placeholder="••••••"
              required
            />
          </div>
          <div class="form-group" style="text-align: right">
            <router-link
              to="/forgot-password"
              style="font-size: 13px; color: var(--primary)"
              >Quên mật khẩu?</router-link
            >
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button
            type="submit"
            class="btn btn-lg btn-primary"
            style="width: 100%"
            :disabled="loading"
          >
            <i v-if="loading" class="bi bi-arrow-repeat spin"></i>
            {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
          </button>
        </form>
        <div class="auth-footer">
          Chưa có tài khoản?
          <router-link to="/register" class="text-primary fw-semibold"
            >Đăng ký ngay</router-link
          >
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  padding: 60px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.auth-card {
  max-width: 420px;
  margin: 0 auto;
  padding: 40px;
}
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}
.auth-header h2 {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 8px;
}
.auth-header p {
  color: var(--text-mid);
  font-size: 14px;
}
.auth-divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}
.auth-divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  right: 0;
  height: 1px;
  background: var(--border);
}
.auth-divider span {
  background: #fff;
  padding: 0 16px;
  position: relative;
  color: var(--text-light);
  font-size: 13px;
}
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-mid);
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
