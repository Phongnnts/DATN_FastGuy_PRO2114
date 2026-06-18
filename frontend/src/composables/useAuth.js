import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ROLES } from '@/utils/constants'

export function useAuth() {
  const auth = useAuthStore()
  const cart = useCartStore()

  const roleRoutes = computed(() => ({
    [ROLES.USER]: { name: 'Profile' },
    [ROLES.STAFF]: { name: 'StaffDashboard' },
    [ROLES.SHIPPER]: { name: 'ShipperDashboard' },
    [ROLES.ADMIN]: { name: 'AdminDashboard' }
  }))

  const homeRoute = computed(() => roleRoutes.value[auth.role] || { name: 'Home' })

  async function handleLogin(email, password) {
    await auth.login(email, password)
    return auth.user
  }

  async function handleRegister(data) {
    await auth.register(data)
    cart.migrateToUser()
  }

  function handleLogout() {
    auth.logout()
  }

  return { auth: computed(() => auth), homeRoute, handleLogin, handleRegister, handleLogout }
}
