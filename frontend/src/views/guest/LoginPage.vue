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
    const redirect = route.query.redirect || (role === 'USER' ? '/' : `/${role.toLowerCase()}`);
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
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <router-link to="/" class="auth-brand">Fast<span>Guy</span></router-link>
          <h1>Đăng nhập</h1>
          <p>Chào mừng bạn quay trở lại!</p>
        </div>
        <form @submit.prevent="handleLogin" class="auth-form">
          <div class="form-group">
            <label class="form-label">Email</label>
            <input v-model="email" type="email" class="form-input" placeholder="your@email.com" required />
          </div>
          <div class="form-group">
            <label class="form-label">Mật khẩu</label>
            <input v-model="password" type="password" class="form-input" placeholder="••••••" required />
          </div>
          <div class="form-row">
            <router-link to="/forgot-password" class="forgot-link">Quên mật khẩu?</router-link>
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button type="submit" class="btn btn-primary btn-lg submit-btn" :disabled="loading">
            <i v-if="loading" class="bi bi-arrow-repeat spin"></i>
            {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
          </button>
        </form>
        <div class="auth-footer">
          Chưa có tài khoản?
          <router-link to="/register" class="fw-semibold text-primary">Đăng ký ngay</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}
.auth-container { width: 100%; max-width: 420px; }
.auth-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 40px;
  box-shadow: var(--shadow-md);
}
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-brand {
  display: inline-block;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin-bottom: 20px;
}
.auth-brand span { color: var(--primary); }
.auth-header h1 { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
.auth-header p { color: var(--text-mid); font-size: 14px; }
.auth-form { display: flex; flex-direction: column; gap: 4px; }
.form-row { text-align: right; margin-bottom: 8px; }
.forgot-link { font-size: 13px; color: var(--text-mid); transition: color var(--transition-fast); }
.forgot-link:hover { color: var(--primary); }
.submit-btn { width: 100%; }
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-mid);
}
.spin { animation: spin 0.8s linear infinite; display: inline-block; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
