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
  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Mật khẩu mới không khớp';
    return;
  }
  loading.value = true;
  try {
    await auth.changePassword(
      form.value.currentPassword,
      form.value.newPassword,
    );
    success.value = 'Đổi mật khẩu thành công!';
    form.value = { currentPassword: '', newPassword: '', confirmPassword: '' };
  } catch (e) {
    error.value = e.message;
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
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Mật khẩu mới</label>
          <input
            v-model="form.newPassword"
            type="password"
            class="form-input"
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Xác nhận mật khẩu mới</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="form-input"
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
