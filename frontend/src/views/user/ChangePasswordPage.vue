<script setup>
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const form = ref({ currentPassword: '', newPassword: '', confirmPassword: '' });
const error = ref('');
const success = ref('');
const loading = ref(false);

async function submit() {
  error.value = '';
  success.value = '';
  if (form.value.newPassword.length < 6 || form.value.newPassword.length > 72) {
    error.value = 'Mật khẩu mới phải từ 6 đến 72 ký tự';
    return;
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Mật khẩu mới không khớp';
    return;
  }
  if (form.value.currentPassword === form.value.newPassword) {
    error.value = 'Mật khẩu mới phải khác mật khẩu hiện tại';
    return;
  }
  loading.value = true;
  try {
    await auth.changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    });
    form.value = { currentPassword: '', newPassword: '', confirmPassword: '' };
    success.value = 'Đổi mật khẩu thành công';
  } catch (e) {
    error.value = e.message || 'Đổi mật khẩu thất bại';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="change-password-page">
    <div class="card">
      <h3 style="font-size: 18px; font-weight: 700; margin-bottom: 24px">
        Đổi mật khẩu
      </h3>
      <div v-if="success" class="alert alert-success">
        <i class="bi bi-check-circle-fill"></i> {{ success }}
      </div>
      <form @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label">Mật khẩu hiện tại</label>
          <input
            v-model="form.currentPassword"
            type="password"
            class="form-input"
            minlength="6"
            maxlength="72"
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Mật khẩu mới</label>
          <input
            v-model="form.newPassword"
            type="password"
            class="form-input"
            minlength="6"
            maxlength="72"
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Xác nhận mật khẩu mới</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="form-input"
            minlength="6"
            maxlength="72"
            required
          />
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
.alert-success {
  background: #d4edda;
  color: #155724;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
