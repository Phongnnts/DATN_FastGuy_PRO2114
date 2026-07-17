<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '@/api';

const route = useRoute();
const router = useRouter();
const token = String(route.query.token || '');
const password = ref('');
const confirmPassword = ref('');
const error = ref('');
const loading = ref(false);

async function submit() {
  error.value = '';
  if (!token || password.value !== confirmPassword.value) {
    error.value = 'Mật khẩu mới không khớp hoặc liên kết không hợp lệ';
    return;
  }
  if (password.value.length < 8 || !/[a-zA-Z]/.test(password.value) || !/[0-9]/.test(password.value)) {
    error.value = 'Mật khẩu mới phải từ 8 ký tự, có ít nhất 1 chữ và 1 số';
    return;
  }
  loading.value = true;
  try {
    await authApi.resetPassword(token, password.value);
    router.replace({ name: 'Login', query: { reset: '1' } });
  } catch (e) {
    error.value = e.message || 'Không thể đặt lại mật khẩu';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page"><div class="auth-container"><div class="auth-card">
    <div class="auth-header"><router-link to="/" class="auth-brand">Fast<span>Guy</span></router-link><h1>Đặt lại mật khẩu</h1></div>
    <form @submit.prevent="submit" class="auth-form">
      <div class="form-group"><label class="form-label">Mật khẩu mới</label><input v-model="password" type="password" class="form-input" required /></div>
      <div class="form-group"><label class="form-label">Xác nhận mật khẩu mới</label><input v-model="confirmPassword" type="password" class="form-input" required /></div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="btn btn-primary btn-lg submit-btn" :disabled="loading">{{ loading ? 'Đang cập nhật...' : 'Đặt lại mật khẩu' }}</button>
    </form>
  </div></div></div>
</template>

<style scoped>
.auth-page { min-height:100%; display:flex; align-items:center; justify-content:center; padding:60px 16px; }.auth-container { width:100%; max-width:420px; }.auth-card { background:#fff; border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:40px; box-shadow:var(--shadow-md); }.auth-header { text-align:center; margin-bottom:32px; }.auth-brand { display:inline-block; font-size:24px; font-weight:800; margin-bottom:20px; }.auth-brand span { color:var(--primary); }.auth-header h1 { font-size:22px; font-weight:700; }.auth-form { display:flex; flex-direction:column; gap:4px; }.submit-btn { width:100%; }
</style>
