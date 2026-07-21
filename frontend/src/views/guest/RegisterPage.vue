<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = ref({ name: '', email: '', phone: '', password: '', confirmPassword: '' });
const error = ref('');
const loading = ref(false);
const showPassword = ref(false);

async function handleRegister() {
  error.value = '';
  const phonePattern = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  if (form.value.name.trim().length < 2) { error.value = 'Họ tên phải từ 2 ký tự'; return; }
  if (!phonePattern.test(form.value.phone.trim())) { error.value = 'Số điện thoại không hợp lệ (VD: 0912345678)'; return; }
  if (form.value.password.length < 8) { error.value = 'Mật khẩu phải từ 8 ký tự'; return; }
  if (!/[a-zA-Z]/.test(form.value.password) || !/[0-9]/.test(form.value.password)) {
    error.value = 'Mật khẩu phải có ít nhất 1 chữ và 1 số';
    return;
  }
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
            <label class="form-label" for="register-name">Họ tên</label>
            <input id="register-name" v-model="form.name" type="text" class="form-input" autocomplete="name" placeholder="Nguyễn Văn A" minlength="2" maxlength="100" required />
          </div>
          <div class="form-group">
            <label class="form-label" for="register-email">Email</label>
            <input id="register-email" v-model="form.email" type="email" class="form-input" autocomplete="email" placeholder="your@email.com" required />
          </div>
          <div class="form-group">
            <label class="form-label" for="register-phone">Số điện thoại</label>
            <input id="register-phone" v-model="form.phone" type="tel" class="form-input" autocomplete="tel" inputmode="tel" placeholder="0912345678" required />
          </div>
          <div class="form-row-2">
            <div class="form-group">
               <label class="form-label" for="register-password">Mật khẩu</label>
               <input id="register-password" v-model="form.password" :type="showPassword ? 'text' : 'password'" class="form-input" autocomplete="new-password" placeholder="Tối thiểu 8 ký tự, có chữ và số" minlength="8" maxlength="72" required />
            </div>
            <div class="form-group">
               <label class="form-label" for="register-confirm">Xác nhận</label>
               <input id="register-confirm" v-model="form.confirmPassword" :type="showPassword ? 'text' : 'password'" class="form-input" autocomplete="new-password" placeholder="••••••" minlength="8" maxlength="72" required />
            </div>
          </div>
           <label class="show-password"><input v-model="showPassword" type="checkbox" /> Hiện mật khẩu</label>
           <p v-if="error" class="form-error" role="alert" aria-live="assertive">{{ error }}</p>
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
.show-password { display: flex; align-items: center; gap: 8px; margin: 2px 0 12px; color: var(--text-mid); font-size: 13px; }
.show-password input { accent-color: var(--primary); }
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-mid);
}
@media (max-width: 480px) { .auth-page { padding: 24px 10px; } .auth-card { padding: 24px 16px; } .form-row-2 { grid-template-columns: 1fr; gap: 0; } }
</style>
