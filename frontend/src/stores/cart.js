import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/product'

const GUEST_SESSION_KEY = 'guest_session_id'

function generateSessionId() {
  return 'sess_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

function getGuestSession() {
  let sid = localStorage.getItem(GUEST_SESSION_KEY)
  if (!sid) {
    sid = generateSessionId()
    localStorage.setItem(GUEST_SESSION_KEY, sid)
  }
  return sid
}

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const isLoaded = ref(false)
  const sessionId = ref(getGuestSession())

  const itemCount = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const subtotal = computed(() => items.value.reduce((sum, i) => sum + (i.discountPrice || i.price) * i.quantity, 0))

  function loadFromStorage() {
    try {
      const stored = localStorage.getItem('cart_' + sessionId.value)
      items.value = stored ? JSON.parse(stored) : []
    } catch { items.value = [] }
    isLoaded.value = true
  }

  function save() {
    localStorage.setItem('cart_' + sessionId.value, JSON.stringify(items.value))
  }

  function findProduct(productId) {
    const productStore = useProductStore()
    return productStore.allProducts.find(p => p.id === Number(productId))
  }

  async function addItem(productId, quantity = 1) {
    const auth = useAuthStore()
    if (auth.isLoggedIn) {
      const product = findProduct(productId)
      try {
        await cartApi.addItem({
          productId,
          quantity,
          unitPrice: product ? (product.discountPrice || product.price) : 0,
          optionData: null
        })
        addLocalItem(productId, quantity)
      } catch (err) {
        console.error('Cart API addItem failed, falling back to local:', err)
        addLocalItem(productId, quantity)
      }
    } else {
      addLocalItem(productId, quantity)
    }
  }

  function addLocalItem(productId, quantity) {
    const existing = items.value.find(i => i.productId === productId)
    if (existing) {
      existing.quantity += quantity
    } else {
      const product = findProduct(productId)
      if (!product) return
      items.value.push({
        productId: product.id,
        name: product.name,
        price: product.price,
        discountPrice: product.discountPrice,
        image: product.image,
        quantity
      })
    }
    save()
  }

  function updateQuantity(productId, quantity) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      if (quantity <= 0) {
        removeItem(productId)
        return
      }
      item.quantity = quantity
      save()
    }
  }

  function removeItem(productId) {
    items.value = items.value.filter(i => i.productId !== productId)
    save()
  }

  async function fetchCart() {
    try {
      const data = await cartApi.get()
      if (data && data.items) {
        items.value = data.items.map(ci => ({
          cartItemId: ci.cartItemId,
          productId: ci.productId,
          name: ci.productName || '',
          price: ci.unitPrice ? parseFloat(ci.unitPrice) : 0,
          discountPrice: null,
          image: ci.imageUrl || '',
          quantity: ci.quantity
        }))
        save()
      }
    } catch (err) {
      console.error('Cart fetchCart failed:', err)
      return null
    }
  }

  function clear() {
    items.value = []
    save()
  }

  async function migrateToUser() {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) return
    const localItems = [...items.value]
    sessionId.value = 'user_' + Date.now()
    localStorage.removeItem(GUEST_SESSION_KEY)
    items.value = []
    save()
    for (const item of localItems) {
      try {
        await cartApi.addItem({
          productId: item.productId,
          quantity: item.quantity,
          unitPrice: item.discountPrice || item.price,
          optionData: null
        })
      } catch (err) {
        console.error('Cart migration addItem failed:', err)
      }
    }
    await fetchCart()
  }

  loadFromStorage()

  return { items, isLoaded, itemCount, subtotal, sessionId, loadFromStorage, addItem, updateQuantity, removeItem, clear, migrateToUser, fetchCart }
})
