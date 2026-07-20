<script setup>
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const form = ref({ currentPassword: '', newPassword: '', confirmPassword: '' });
const error = ref('');
const success = ref('');
const loading = ref(false);

function isStrongPassword(pw) {
  if (pw.length < 8) return false;
  return /[a-zA-Z]/.test(pw) && /[0-9]/.test(pw);
}

async function submit() {
  error.value = '';
  success.value = '';

  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Mật khẩu mới không khớp';
    return;
  }

  if (!isStrongPassword(form.value.newPassword)) {
    error.value = 'Mật khẩu mới phải từ 8 ký tự, có ít nhất 1 chữ và 1 số';
    return;
  }

  loading.value = true;
  try {
    await auth.changePassword(form.value.currentPassword, form.value.newPassword);
    success.value = 'Đổi mật khẩu thành công';
    form.value = { currentPassword: '', newPassword: '', confirmPassword: '' };
  } catch (e) {
    error.value = e.message || 'Đổi mật khẩu thất bại';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="change-pw-page">
    <h2 class="page-title">Đổi mật khẩu</h2>
    <div class="card" style="max-width:480px">
      <div v-if="success" class="alert-success">
        <i class="bi bi-check-circle-fill"></i> {{ success }}
      </div>
      <form @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label">Mật khẩu hiện tại</label>
          <input v-model="form.currentPassword" type="password" class="form-input" required />
        </div>
        <div class="form-group">
          <label class="form-label">Mật khẩu mới</label>
          <input v-model="form.newPassword" type="password" class="form-input" placeholder="Tối thiểu 8 ký tự, có chữ và số" required />
        </div>
        <div class="form-group">
          <label class="form-label">Xác nhận mật khẩu mới</label>
          <input v-model="form.confirmPassword" type="password" class="form-input" required />
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Đang xử lý...' : 'Đổi mật khẩu' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.change-pw-page { padding: 32px 0; }
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 20px; }
.alert-success { background:#dcfce7; color:#166534; padding:12px 16px; border-radius:var(--radius-sm); margin-bottom:20px; font-size:14px; display:flex; align-items:center; gap:8px; }
</style>
