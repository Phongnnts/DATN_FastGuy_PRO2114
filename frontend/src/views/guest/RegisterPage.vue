<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = ref({
  name: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
});
const error = ref('');
const loading = ref(false);

async function handleRegister() {
  error.value = '';
  if (form.value.password !== form.value.confirmPassword) {
    error.value = 'Mật khẩu không khớp';
    return;
  }
  loading.value = true;
  try {
    await auth.register({
      fullName: form.value.name,
      email: form.value.email,
      phone: form.value.phone,
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
    <div class="container">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Đăng ký</h2>
          <p>Tạo tài khoản FastGuy ngay!</p>
        </div>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label class="form-label">Họ tên</label>
            <input
              v-model="form.name"
              type="text"
              class="form-input"
              placeholder="Nguyễn Văn A"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              v-model="form.email"
              type="email"
              class="form-input"
              placeholder="your@email.com"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Số điện thoại</label>
            <input
              v-model="form.phone"
              type="tel"
              class="form-input"
              placeholder="0912345678"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Mật khẩu</label>
            <input
              v-model="form.password"
              type="password"
              class="form-input"
              placeholder="••••••"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Xác nhận mật khẩu</label>
            <input
              v-model="form.confirmPassword"
              type="password"
              class="form-input"
              placeholder="••••••"
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
            {{ loading ? 'Đang đăng ký...' : 'Đăng ký' }}
          </button>
        </form>
        <div class="auth-footer">
          Đã có tài khoản?
          <router-link to="/login" class="text-primary fw-semibold"
            >Đăng nhập</router-link
          >
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  padding: 40px 0;
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
  color: var(--text-mid);
}
</style>
