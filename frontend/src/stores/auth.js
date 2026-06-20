import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { ROLES } from '@/utils/constants';
import { authApi } from '@/api';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null);
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'));

  const isLoggedIn = computed(() => !!token.value);
  const role = computed(() => user.value?.role || ROLES.GUEST);
  const isUser = computed(() => role.value === ROLES.USER);
  const isStaff = computed(() => role.value === ROLES.STAFF);


  const isAdmin = computed(() => role.value === ROLES.ADMIN);

  function persist() {
    if (token.value) {
      localStorage.setItem('token', token.value);
      localStorage.setItem('user', JSON.stringify(user.value));
    } else {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }

  async function login(email, password) {
    const data = await authApi.login({ login: email, password });
    token.value = data.token;
    user.value = {
      id: data.userId,
      fullName: data.fullName,
      role: data.role,
      email: email,
      phone: '',
      avatarUrl: data.avatarUrl || '',
    };
    persist();
    return user.value;
  }

  async function register(data) {
    const result = await authApi.register({
      fullName: data.fullName,
      phone: data.phone,
      email: data.email,
      password: data.password,
    });
    token.value = result.token;
    user.value = {
      id: result.userId,
      fullName: result.fullName,
      role: result.role,
      email: data.email,
      phone: data.phone,
      avatarUrl: '',
    };
    persist();
    return user.value;
  }

  function logout() {
    token.value = null;
    user.value = null;
    persist();
  }

  async function updateProfile(data) {
    if (!user.value) throw new Error('Chưa đăng nhập');
    Object.assign(user.value, data);
    persist();
    return user.value;
  }

  async function changePassword(currentPassword, newPassword) {
    return true;
  }

  async function forgotPassword(email) {
    return { message: 'Link đặt lại mật khẩu đã được gửi đến email của bạn' };
  }

  return {
    token,
    user,
    isLoggedIn,
    role,
    isUser,
    isStaff,


    isAdmin,
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    forgotPassword,
  };
});
