<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import FormField from '@/components/common/FormField.vue';
import { required, validEmail } from '@/utils/formValidation';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useToast } from '@/stores/toast';
import { createLoginMigrationController } from '@/utils/cartMigration';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const cart = useCartStore();
const toast = useToast();
const email = ref('');
const password = ref('');
const error = ref('');
const migrationWarning = ref('');
const loading = ref(false);
const touched = reactive({ email: false, password: false });
const errors = reactive({ email: '', password: '' });
const loginMigration = createLoginMigrationController({
  login: (emailValue, passwordValue) => auth.login(emailValue, passwordValue),
  migrate: () => cart.migrateToUser(),
  warn: (message) => { migrationWarning.value = message; toast.error(message); },
  navigate: (redirect) => router.push(redirect),
});

function validateField(field) {
  if (field === 'email') errors.email = !required(email.value) ? 'Vui lòng nhập email' : !validEmail(email.value) ? 'Email không hợp lệ' : '';
  if (field === 'password') errors.password = required(password.value) ? '' : 'Vui lòng nhập mật khẩu';
  return !errors[field];
}
function blurField(field) { touched[field] = true; validateField(field); }
function inputField(field) { if (touched[field] || errors[field]) validateField(field); }
function validateForm() { touched.email = true; touched.password = true; return validateField('email') & validateField('password'); }
async function handleLogin() {
  if (loading.value) return;
  if (!validateForm()) return;
  error.value = '';
  migrationWarning.value = '';
  loading.value = true;
  try { await loginMigration.submit(email.value, password.value, route.query.redirect); }
  catch (e) { error.value = e.message; }
  finally { loading.value = false; }
}
</script>

<template>
  <div class="auth-page"><div class="auth-container"><div class="auth-card">
    <div class="auth-header"><router-link to="/home" class="auth-brand">Fast<span>Guy</span></router-link><h1>Đăng nhập</h1><p>Chào mừng bạn quay trở lại!</p></div>
    <form class="auth-form" novalidate @submit.prevent="handleLogin">
      <FormField id="login-email" label="Email" required :error="errors.email"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="email" type="email" class="form-input" placeholder="your@email.com" @blur="blurField('email')" @input="inputField('email')" /></template></FormField>
      <FormField id="login-password" label="Mật khẩu" required :error="errors.password"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="password" type="password" class="form-input" placeholder="Nhập mật khẩu" @blur="blurField('password')" @input="inputField('password')" /></template></FormField>
      <div class="form-row"><router-link to="/forgot-password" class="forgot-link">Quên mật khẩu?</router-link></div>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p><p v-if="migrationWarning" class="form-error" role="alert">{{ migrationWarning }}</p>
      <button type="submit" class="btn btn-primary btn-lg submit-btn" :disabled="loading"><i v-if="loading" class="bi bi-arrow-repeat spin"></i>{{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}</button>
    </form>
    <div class="auth-footer">Chưa có tài khoản? <router-link to="/register" class="fw-semibold text-primary">Đăng ký ngay</router-link></div>
  </div></div></div>
</template>

<style scoped>
.auth-page { min-height:100%;display:flex;align-items:center;justify-content:center;padding:48px 16px }.auth-container{width:100%;max-width:420px}.auth-card{background:#fff;border:1px solid var(--border-light);border-radius:var(--radius-lg);padding:40px;box-shadow:var(--shadow-md)}.auth-header{text-align:center;margin-bottom:32px}.auth-brand{display:inline-block;font-size:24px;font-weight:800;letter-spacing:-.5px;margin-bottom:20px}.auth-brand span{color:var(--primary)}.auth-header h1{font-size:22px;font-weight:700;margin-bottom:6px}.auth-header p{color:var(--text-mid);font-size:14px}.auth-form{display:flex;flex-direction:column;gap:4px}.form-row{text-align:right;margin-bottom:8px}.forgot-link{font-size:13px;color:var(--text-mid);transition:color var(--transition-fast)}.forgot-link:hover{color:var(--primary)}.submit-btn{width:100%}.auth-footer{text-align:center;margin-top:24px;font-size:14px;color:var(--text-mid)}.spin{animation:spin .8s linear infinite;display:inline-block}@keyframes spin{from{transform:rotate(0)}to{transform:rotate(360deg)}}
</style>
