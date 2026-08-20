import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { ROLES } from '@/utils/constants';
import { authApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { clearStoredSession, isTokenValid, parseStoredUser } from '@/utils/session';
import { createProfileHydrationController } from '@/utils/profileHydration';

export const useAuthStore = defineStore('auth', () => {
  const storedToken = localStorage.getItem('token');
  if (storedToken && !isTokenValid(storedToken)) clearStoredSession();
  const token = ref(isTokenValid(storedToken) ? storedToken : null);
  const user = ref(token.value ? parseStoredUser(localStorage.getItem('user')) : null);
  if (token.value && !user.value) clearStoredSession();
  if (!user.value) token.value = null;
  let sessionGeneration = 0;

  function clearReactiveSession() {
    sessionGeneration += 1;
    profileHydration.invalidate();
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

  const profileHydration = createProfileHydrationController({
    getSession: () => ({ token: token.value, userId: user.value?.id, role: user.value?.role, generation: sessionGeneration }),
    requestProfile: () => authApi.getProfile(),
    applyProfile: (profile) => { user.value = profile; },
    persist,
  });

  async function login(email, password) {
    const data = await authApi.login({ login: email, password });
    sessionGeneration += 1;
    profileHydration.invalidate();
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
    sessionGeneration += 1;
    profileHydration.invalidate();
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
    sessionGeneration += 1;
    profileHydration.invalidate();
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

  async function hydrateProfile() {
    return await profileHydration.hydrate();
  }

  async function updateProfile(data) {
    if (!user.value) throw new Error('Chưa đăng nhập');
    try {
      const profile = await authApi.updateProfile(data);
      sessionGeneration += 1;
      profileHydration.invalidate();
      Object.assign(user.value, profile || data);
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
    hydrateProfile,
    updateProfile,
    changePassword,
  };
});
