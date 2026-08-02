import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { ROLES } from '@/utils/constants';
import { authApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { clearStoredSession, isTokenValid, parseStoredUser } from '@/utils/session';

export const useAuthStore = defineStore('auth', () => {
  const storedToken = localStorage.getItem('token');
  if (storedToken && !isTokenValid(storedToken)) clearStoredSession();
  const token = ref(isTokenValid(storedToken) ? storedToken : null);
  const user = ref(token.value ? parseStoredUser(localStorage.getItem('user')) : null);
  if (token.value && !user.value) clearStoredSession();
  if (!user.value) token.value = null;

  function clearReactiveSession() {
    token.value = null;
    user.value = null;
  }
  window.addEventListener('fastguy-session-cleared', clearReactiveSession);

  const isLoggedIn = computed(() => isTokenValid(token.value));
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
    const cart = useCartStore();
    cart.clear();
    token.value = null;
    user.value = null;
    persist();
  }

  function validateSession() {
    if (isTokenValid(token.value) && user.value) return true;
    token.value = null;
    user.value = null;
    persist();
    return false;
  }

  async function updateProfile(data) {
    if (!user.value) throw new Error('Chưa đăng nhập');
    try {
      await authApi.updateProfile(data);
      Object.assign(user.value, data);
      persist();
      return user.value;
    } catch {
      throw new Error('Cập nhật thất bại');
    }
  }

  async function changePassword(currentPassword, newPassword) {
    return await authApi.changePassword({ currentPassword, newPassword });
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
    validateSession,
    updateProfile,
    changePassword,
  };
});
