<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import CartItem from '@/components/common/CartItem.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

function updateQty(productId, qty) { cart.updateQuantity(productId, qty) }
function removeItem(productId) { cart.removeItem(productId) }

function proceedCheckout() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/checkout' } })
  } else {
    router.push('/checkout')
  }
}
</script>

<template>
  <div class="cart-page">
    <div class="container">
      <div class="page-header">
        <h1>Giỏ hàng</h1>
        <p v-if="cart.itemCount > 0">{{ cart.itemCount }} sản phẩm</p>
      </div>
      <div v-if="cart.items.length === 0" class="empty-state">
        <i class="bi bi-cart3"></i>
        <h3>Giỏ hàng trống</h3>
        <p>Hãy thêm món ăn vào giỏ hàng nhé!</p>
        <router-link to="/menu" class="btn btn-primary"><i class="bi bi-arrow-left"></i> Tiếp tục mua</router-link>
      </div>
      <div v-else class="cart-layout">
        <div class="cart-items card">
          <CartItem
            v-for="item in cart.items"
            :key="item.productId"
            :item="item"
            @update:quantity="updateQty"
            @remove="removeItem"
          />
        </div>
        <div class="cart-summary card">
          <h3>Tổng cộng</h3>
          <div class="summary-row"><span>Tạm tính</span><span>{{ formatPrice(cart.subtotal) }}</span></div>
          <div class="summary-row"><span>Phí giao hàng</span><span class="text-primary">Miễn phí</span></div>
          <div class="summary-divider"></div>
          <div class="summary-row summary-total"><span>Tổng cộng</span><span>{{ formatPrice(cart.subtotal) }}</span></div>
          <button class="btn btn-lg btn-primary checkout-btn" @click="proceedCheckout">
            <i class="bi bi-credit-card"></i> Tiến hành đặt hàng
          </button>
          <router-link to="/menu" class="continue-shopping"><i class="bi bi-arrow-left"></i> Tiếp tục mua</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page { padding: 24px 0; }
.cart-layout { display: grid; grid-template-columns: 1fr 360px; gap: 24px; align-items: start; }
.cart-items { padding: 16px 24px; }
.cart-summary { position: sticky; top: 100px; }
.cart-summary h3 { font-size: 18px; font-weight: 700; margin-bottom: 16px; }
.summary-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
.summary-total { font-size: 18px; font-weight: 800; }
.summary-divider { height: 1px; background: var(--border); margin: 8px 0; }
.checkout-btn { width: 100%; margin-top: 16px; }
.continue-shopping { display: block; text-align: center; margin-top: 12px; font-size: 14px; color: var(--text-mid); }
.continue-shopping:hover { color: var(--primary); }
@media (max-width: 768px) {
  .cart-layout { grid-template-columns: 1fr; }
}
</style>
