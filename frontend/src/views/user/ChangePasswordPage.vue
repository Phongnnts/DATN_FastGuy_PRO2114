<script setup>
import { ref } from 'vue';

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
  setTimeout(() => {
    error.value = 'Tính năng đổi mật khẩu chưa được bật. Vui lòng liên hệ quản trị viên.';
    loading.value = false;
  }, 300);
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
          <input v-model="form.newPassword" type="password" class="form-input" required />
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
.alert-success {
  background: #dcfce7;
  color: #166534;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
