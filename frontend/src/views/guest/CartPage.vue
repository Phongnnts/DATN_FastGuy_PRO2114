<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { formatPrice } from '@/utils/format';
import CartItem from '@/components/common/CartItem.vue';

const router = useRouter();
const cart = useCartStore();
const hasInvalidItems = computed(() => cart.items.some((item) => (item.variantStatus && item.variantStatus !== 'AVAILABLE') || (item.quantityAvailable != null && (Number(item.quantityAvailable) <= 0 || item.quantity > Number(item.quantityAvailable)))));

async function updateQty(productId, variantId, quantity, modifierOptionIds) {
  try { await cart.updateQuantity(productId, variantId, quantity, modifierOptionIds); }
  catch (error) { alert(error.message || 'Không thể cập nhật số lượng'); }
}
function removeItem(productId, variantId, modifierOptionIds) { cart.removeItem(productId, variantId, modifierOptionIds); }
function proceedCheckout() {
  if (hasInvalidItems.value) return alert('Vui lòng cập nhật giỏ hàng theo tồn kho hiện tại');
  router.push('/checkout');
}
</script>

<template>
  <div class="cart-page">
    <div class="container">
      <div class="cart-breadcrumb"><router-link to="/">Trang chủ</router-link><i class="bi bi-chevron-right"></i><strong>Giỏ hàng</strong></div>

      <div class="checkout-stepper" aria-label="Tiến trình đặt hàng">
        <div class="step active"><span><i class="bi bi-check-lg"></i></span><strong>Giỏ hàng</strong></div>
        <div class="step-line"></div>
        <div class="step"><span>2</span><strong>Thông tin giao</strong></div>
        <div class="step-line"></div>
        <div class="step"><span>3</span><strong>Thanh toán</strong></div>
        <div class="step-line"></div>
        <div class="step"><span>4</span><strong>Hoàn tất</strong></div>
      </div>

      <div v-if="!cart.isLoaded" class="skeleton-cart"><div v-for="number in 3" :key="number" class="skeleton-cart-item"></div></div>

      <div v-else-if="cart.items.length === 0" class="empty-state cart-empty">
        <i class="bi bi-bag"></i><h3>Giỏ hàng trống</h3><p>Hãy thêm món ăn vào giỏ hàng nhé!</p><router-link to="/menu" class="btn btn-primary">Khám phá thực đơn</router-link>
      </div>

      <div v-else class="cart-layout">
        <main class="cart-main">
          <section class="cart-block">
            <div class="block-title"><span><i class="bi bi-basket2"></i> Sản phẩm trong giỏ</span><small>{{ cart.itemCount }} món</small></div>
            <div class="cart-columns"><span>Sản phẩm</span><span>Số lượng</span><span>Thành tiền</span></div>
            <CartItem v-for="item in cart.items" :key="item.key" :item="item" @update:quantity="updateQty" @remove="removeItem" />
            <div class="coupon-preview"><i class="bi bi-ticket-perforated"></i><span>Mã giảm giá sẽ được áp dụng ở bước thanh toán.</span><router-link to="/promotions">Xem ưu đãi</router-link></div>
          </section>

          <section class="cart-block route-note">
            <i class="bi bi-geo-alt"></i>
            <div><strong>Giao nhanh trong 30 phút</strong><p>Phí giao chính xác được tính sau khi bạn chọn địa chỉ nhận hàng.</p></div>
          </section>
        </main>

        <aside class="order-summary">
          <h2>Đơn hàng</h2>
          <div class="summary-row"><span>Tạm tính ({{ cart.itemCount }} món)</span><strong>{{ formatPrice(cart.subtotal) }}</strong></div>
          <div class="summary-row"><span>Phí giao hàng</span><span>--</span></div>
          <div class="summary-divider"></div>
          <div class="summary-row total"><span>Tổng cộng</span><strong>{{ formatPrice(cart.subtotal) }}</strong></div>
          <p v-if="hasInvalidItems" class="stock-warning"><i class="bi bi-exclamation-circle"></i> Có món đã hết hàng hoặc vượt tồn kho.</p>
          <button class="order-button" :disabled="hasInvalidItems" @click="proceedCheckout">Đặt hàng ngay <i class="bi bi-arrow-right"></i></button>
          <p class="terms"><i class="bi bi-lock"></i> Thanh toán an toàn 100%</p>
          <div class="points-card"><i class="bi bi-stars"></i><span>Bạn sẽ nhận được điểm thưởng sau khi đơn hàng hoàn thành.</span></div>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page { min-height: 100vh; padding: 22px 0 56px; background: #fff8f0; }
.cart-breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-mid); margin-bottom: 18px; }.cart-breadcrumb a { color: var(--text-dark); font-weight: 600; }.cart-breadcrumb i { font-size: 10px; color: var(--text-light); }
.checkout-stepper { display: grid; grid-template-columns: auto 1fr auto 1fr auto 1fr auto; align-items: center; gap: 12px; padding: 18px 24px; background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius-lg); margin-bottom: 16px; }.step { display: grid; justify-items: center; gap: 6px; white-space: nowrap; color: var(--text-mid); font-size: 12px; }.step span { width: 28px; height: 28px; border-radius: 50%; background: #e9eef8; display: grid; place-items: center; font-size: 12px; font-weight: 800; }.step strong { font-weight: 600; }.step.active { color: var(--primary-dark); }.step.active span { color: #fff; background: var(--primary-dark); }.step-line { height: 1px; background: linear-gradient(90deg, var(--primary-100), var(--border)); }
.cart-layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 18px; align-items: start; }.cart-main { display: grid; gap: 16px; }.cart-block, .order-summary { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 18px; box-shadow: var(--shadow-xs); }.block-title { display: flex; justify-content: space-between; align-items: center; padding-bottom: 14px; font-size: 14px; font-weight: 800; border-bottom: 1px solid var(--border-light); }.block-title i { color: var(--primary-dark); margin-right: 6px; }.block-title small { color: var(--text-mid); font-weight: 500; }.cart-columns { display: grid; grid-template-columns: 1fr 132px 100px; gap: 12px; padding: 12px 0 4px; color: var(--text-mid); font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }.cart-columns span:nth-child(2), .cart-columns span:nth-child(3) { text-align: center; }.coupon-preview { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin: 14px 0 2px; padding: 12px; border: 1px dashed var(--primary-100); border-radius: var(--radius-sm); color: var(--text-mid); font-size: 12px; }.coupon-preview i, .coupon-preview a { color: var(--primary-dark); }.coupon-preview a { margin-left: auto; font-weight: 700; }.route-note { display: flex; gap: 12px; align-items: start; background: #fff1df; border-color: transparent; }.route-note > i { color: var(--primary-dark); font-size: 22px; }.route-note strong { font-size: 13px; }.route-note p { margin: 3px 0 0; color: var(--text-mid); font-size: 12px; }
.order-summary { position: sticky; top: 82px; }.order-summary h2 { font-size: 17px; margin: 0 0 14px; }.summary-row { display: flex; justify-content: space-between; gap: 10px; padding: 8px 0; color: var(--text-mid); font-size: 13px; }.summary-row strong { color: var(--text-dark); }.summary-divider { border-top: 1px dashed var(--border); margin: 8px 0; }.summary-row.total { align-items: baseline; color: var(--text-dark); font-weight: 700; }.summary-row.total strong { color: var(--primary-dark); font-size: 24px; letter-spacing: -.03em; }.order-button { width: 100%; min-height: 48px; border-radius: var(--radius-full); margin: 16px 0 10px; background: linear-gradient(135deg, var(--primary-dark), var(--route-orange)); color: #fff; font-size: 14px; font-weight: 800; box-shadow: 0 12px 22px rgba(212,97,58,.22); }.order-button:disabled { opacity: .45; cursor: not-allowed; }.terms { text-align: center; color: var(--text-mid); font-size: 11px; }.terms i { color: var(--primary-dark); }.stock-warning { color: var(--red-active); font-size: 12px; font-weight: 600; }.points-card { display: flex; gap: 8px; align-items: start; margin-top: 16px; padding: 12px; border-radius: var(--radius-sm); background: #fff1df; color: #7c3b20; font-size: 12px; }.points-card i { color: var(--primary-dark); }
.skeleton-cart { display: grid; gap: 12px; }.skeleton-cart-item { height: 110px; border-radius: var(--radius); background: linear-gradient(90deg, #f3eee9 25%, #fff 50%, #f3eee9 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }.cart-empty { background: #fff; border-radius: var(--radius-lg); }@keyframes shimmer { from { background-position: -200% 0; } to { background-position: 200% 0; } }
@media (max-width: 768px) { .cart-layout { grid-template-columns: 1fr; }.order-summary { position: static; }.cart-columns { display: none; }.checkout-stepper { grid-template-columns: repeat(4, 1fr); gap: 4px; padding: 14px 8px; }.step { white-space: normal; text-align: center; font-size: 10px; }.step-line { display: none; } }
</style>
