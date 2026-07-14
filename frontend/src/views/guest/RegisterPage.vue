<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = ref({ name: '', email: '', phone: '', password: '', confirmPassword: '' });
const error = ref('');
const loading = ref(false);

async function handleRegister() {
  error.value = '';
  const phonePattern = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  if (form.value.name.trim().length < 2) { error.value = 'Họ tên phải từ 2 ký tự'; return; }
  if (!phonePattern.test(form.value.phone.trim())) { error.value = 'Số điện thoại không hợp lệ'; return; }
  if (form.value.password.length < 6) { error.value = 'Mật khẩu phải từ 6 ký tự'; return; }
  if (form.value.password !== form.value.confirmPassword) { error.value = 'Mật khẩu không khớp'; return; }
  loading.value = true;
  try {
    await auth.register({
      fullName: form.value.name.trim(),
      email: form.value.email.trim(),
      phone: form.value.phone.trim(),
      password: form.value.password,
    });
    router.push('/');
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
          <h1>Đăng ký</h1>
          <p>Tạo tài khoản FastGuy ngay!</p>
        </div>
        <form @submit.prevent="handleRegister" class="auth-form">
          <div class="form-group">
            <label class="form-label">Họ tên</label>
            <input v-model="form.name" type="text" class="form-input" placeholder="Nguyễn Văn A" minlength="2" maxlength="100" required />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input v-model="form.email" type="email" class="form-input" placeholder="your@email.com" required />
          </div>
          <div class="form-group">
            <label class="form-label">Số điện thoại</label>
            <input v-model="form.phone" type="tel" class="form-input" placeholder="0912345678" required />
          </div>
          <div class="form-row-2">
            <div class="form-group">
              <label class="form-label">Mật khẩu</label>
              <input v-model="form.password" type="password" class="form-input" placeholder="••••••" minlength="6" maxlength="72" required />
            </div>
            <div class="form-group">
              <label class="form-label">Xác nhận</label>
              <input v-model="form.confirmPassword" type="password" class="form-input" placeholder="••••••" minlength="6" maxlength="72" required />
            </div>
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button type="submit" class="btn btn-primary btn-lg submit-btn" :disabled="loading">
            {{ loading ? 'Đang đăng ký...' : 'Đăng ký' }}
          </button>
        </form>
        <div class="auth-footer">
          Đã có tài khoản?
          <router-link to="/login" class="fw-semibold text-primary">Đăng nhập</router-link>
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
  padding: 40px 16px;
}
.auth-container { width: 100%; max-width: 440px; }
.auth-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 40px;
  box-shadow: var(--shadow-md);
}
.auth-header { text-align: center; margin-bottom: 28px; }
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
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.submit-btn { width: 100%; }
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-mid);
}
</style>
