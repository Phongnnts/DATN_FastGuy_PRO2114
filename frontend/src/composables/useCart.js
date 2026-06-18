import { computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'

export function useCart() {
  const cart = useCartStore()
  const auth = useAuthStore()

  const isGuestCart = computed(() => !auth.isLoggedIn)

  function addToCart(productId, quantity = 1) {
    cart.addItem(productId, quantity)
  }

  return { cart: computed(() => cart), isGuestCart, addToCart }
}
