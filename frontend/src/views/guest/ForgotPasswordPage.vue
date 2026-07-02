<script setup>
import { ref } from 'vue';

const email = ref('');
const message = ref('');
const error = ref('');
const loading = ref(false);
const sent = ref(false);

function handleSubmit() {
  error.value = '';
  loading.value = true;
  setTimeout(() => {
    message.value = 'Tính năng đặt lại mật khẩu chưa được bật. Vui lòng liên hệ quản trị viên.';
    sent.value = true;
    loading.value = false;
  }, 300);
}
</script>

<template>
  <div class="auth-page">
    <div class="container">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Quên mật khẩu</h2>
          <p>Nhập email để nhận link đặt lại mật khẩu</p>
        </div>
        <div v-if="sent" class="text-center" style="padding: 20px 0">
          <i
            class="bi bi-check-circle-fill"
            style="
              font-size: 48px;
              color: #4caf50;
              margin-bottom: 16px;
              display: block;
            "
          ></i>
          <h3>Đã gửi! Kiểm tra email của bạn.</h3>
          <p style="color: var(--text-mid); font-size: 14px; margin: 12px 0">
            {{ message }}
          </p>
          <router-link to="/login" class="btn btn-primary"
            >Quay lại đăng nhập</router-link
          >
        </div>
        <form v-else @submit.prevent="handleSubmit">
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
          <p v-if="error" class="form-error">{{ error }}</p>
          <button
            type="submit"
            class="btn btn-lg btn-primary"
            style="width: 100%"
            :disabled="loading"
          >
            {{ loading ? 'Đang gửi...' : 'Gửi yêu cầu' }}
          </button>
        </form>
        <div class="auth-footer">
          <router-link to="/login" class="text-primary fw-semibold"
            ><i class="bi bi-arrow-left"></i> Quay lại đăng nhập</router-link
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
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
}
</style>
