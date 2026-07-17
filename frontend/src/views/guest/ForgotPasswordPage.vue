<script setup>
import { ref } from 'vue';
import { authApi } from '@/api';

const email = ref('');
const message = ref('');
const error = ref('');
const loading = ref(false);
const sent = ref(false);

async function handleSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const res = await authApi.forgotPassword(email.value);
    message.value = res?.message || 'Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi.';
    sent.value = true;
  } catch (e) {
    error.value = e.message || 'Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau.';
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
          <h1>Quên mật khẩu</h1>
          <p>Nhập email để nhận hướng dẫn đặt lại mật khẩu</p>
        </div>
        <div v-if="sent" class="sent-state">
          <div class="sent-icon"><i class="bi bi-check-lg"></i></div>
          <h3>Đã gửi!</h3>
          <p>{{ message }}</p>
          <router-link to="/login" class="btn btn-primary">Quay lại đăng nhập</router-link>
        </div>
        <form v-else @submit.prevent="handleSubmit" class="auth-form">
          <div class="form-group">
            <label class="form-label">Email</label>
            <input v-model="email" type="email" class="form-input" placeholder="your@email.com" required />
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button type="submit" class="btn btn-primary btn-lg submit-btn" :disabled="loading">
            {{ loading ? 'Đang gửi...' : 'Gửi yêu cầu' }}
          </button>
        </form>
        <div class="auth-footer">
          <router-link to="/login" class="fw-semibold text-primary">
            <i class="bi bi-arrow-left"></i> Quay lại đăng nhập
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page { min-height:100%; display:flex; align-items:center; justify-content:center; padding:60px 16px; }
.auth-container { width:100%; max-width:420px; }
.auth-card { background:#fff; border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:40px; box-shadow:var(--shadow-md); }
.auth-header { text-align:center; margin-bottom:32px; }
.auth-brand { display:inline-block; font-size:24px; font-weight:800; letter-spacing:-0.5px; margin-bottom:20px; }
.auth-brand span { color:var(--primary); }
.auth-header h1 { font-size:22px; font-weight:700; margin-bottom:6px; }
.auth-header p { color:var(--text-mid); font-size:14px; }
.auth-form { display:flex; flex-direction:column; gap:4px; }
.submit-btn { width:100%; }
.sent-state { text-align:center; padding:20px 0; }
.sent-icon { width:56px; height:56px; border-radius:50%; background:#dcfce7; color:#16a34a; font-size:24px; display:flex; align-items:center; justify-content:center; margin:0 auto 16px; }
.sent-state h3 { font-size:18px; font-weight:700; margin-bottom:8px; }
.sent-state p { color:var(--text-mid); font-size:14px; margin-bottom:20px; }
.auth-footer { text-align:center; margin-top:24px; font-size:14px; color:var(--text-mid); }
</style>
