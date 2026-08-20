<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { useAuthStore } from '@/stores/auth';
import { useOrderStore } from '@/stores/order';
import { formatDate, formatPrice } from '@/utils/format';
import CheckoutStepper from '@/components/common/CheckoutStepper.vue';

const route = useRoute();
const auth = useAuthStore();
const orders = useOrderStore();
const order = ref(null);
const loading = ref(false);
const loadError = ref('');
const orderId = computed(() => /^\d+$/.test(String(route.query.orderId || '')) ? String(route.query.orderId) : '');
const orderCode = computed(() => String(route.query.orderCode || '').slice(0, 64));
const canLoadDetail = computed(() => auth.isLoggedIn && auth.isUser && Boolean(orderId.value));
const verifiedGuest = ref(false);
const shownCode = computed(() => order.value?.orderCode || (verifiedGuest.value ? orderCode.value : ''));
const trackingLink = computed(() => ({ path: '/track-order', query: orderCode.value ? { code: orderCode.value } : {} }));
const paymentLabel = computed(() => ({
  PAID: 'Đã thanh toán',
  UNPAID: 'Chưa thanh toán',
  PENDING: 'Đang xử lý',
  FAILED: 'Thanh toán thất bại',
  REFUNDED: 'Đã hoàn tiền',
})[order.value?.paymentStatus] || order.value?.paymentStatus || 'Chưa thanh toán');
const paymentMethod = computed(() => order.value?.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 'Chuyển khoản');

onMounted(() => {
  if (!canLoadDetail.value && orderCode.value) {
    const key = `order-success:${orderCode.value}`;
    verifiedGuest.value = sessionStorage.getItem(key) === '1';
    sessionStorage.removeItem(key);
  }
  loadOrder();
});

async function loadOrder() {
  if (!canLoadDetail.value) return;
  loading.value = true;
  loadError.value = '';
  try {
    order.value = await orders.fetchById(orderId.value);
  } catch (error) {
    loadError.value = error.message || 'Không thể tải chi tiết đơn hàng';
  } finally {
    if (!order.value && orders.error) loadError.value = orders.error;
    loading.value = false;
  }
}

function printReceipt() {
  window.print();
}
</script>

<template>
  <div class="success-page">
    <div class="success-shell">
      <CheckoutStepper :current="4" />
    <section class="card success-card" aria-live="polite">
      <header class="success-header success-hero">
        <i class="bi bi-check-circle-fill success-icon" aria-hidden="true"></i>
        <div>
          <h1>{{ canLoadDetail || verifiedGuest ? 'Đặt hàng thành công!' : 'Tra cứu đơn hàng' }}</h1>
          <p>{{ canLoadDetail || verifiedGuest ? 'Đơn hàng đã được tạo và đang chờ cửa hàng xác nhận.' : 'Không thể xác nhận phiên đặt hàng này. Hãy tra cứu để kiểm tra trạng thái đơn.' }}</p>
          <p v-if="shownCode" class="order-code">Mã đơn: <strong>{{ shownCode }}</strong></p>
        </div>
      </header>

      <div v-if="loading" class="state" role="status">
        <span class="spinner" aria-hidden="true"></span>
        <span>Đang tải thông tin đơn hàng...</span>
      </div>
      <div v-else-if="loadError" class="state state-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <span>{{ loadError }}</span>
        <button class="btn btn-sm btn-outline" @click="loadOrder">Thử lại</button>
      </div>
      <div v-else-if="canLoadDetail && !order" class="state" role="status">
        <i class="bi bi-box" aria-hidden="true"></i>
        <span>Không tìm thấy thông tin đơn hàng.</span>
      </div>

      <template v-else-if="order">
        <section class="overview" aria-label="Trạng thái đơn hàng">
          <div><span>Trạng thái</span><OrderStatusBadge :status="order.status" /></div>
          <div><span>Thanh toán</span><strong>{{ paymentLabel }}</strong><small>{{ paymentMethod }}</small></div>
          <div v-if="order.createdAt"><span>Ngày đặt</span><strong>{{ formatDate(order.createdAt) }}</strong></div>
        </section>

        <section class="detail-section">
          <h2>Sản phẩm</h2>
          <div v-if="order.items?.length" class="items">
            <article v-for="(item, index) in order.items" :key="`${item.productId}-${item.variantId || 'default'}-${index}`" class="item">
              <img v-if="item.image" :src="item.image" :alt="item.productName" />
              <div class="item-info"><strong>{{ item.productName }}</strong><small v-if="item.variantName">{{ item.variantName }}</small><span>{{ formatPrice(item.price) }} × {{ item.quantity }}</span></div>
              <strong class="item-total">{{ formatPrice(item.totalPrice || item.price * item.quantity) }}</strong>
            </article>
          </div>
          <p v-else class="empty-copy">Đơn hàng chưa có sản phẩm hiển thị.</p>
        </section>

        <section class="detail-grid">
          <div class="detail-section address">
            <h2>Địa chỉ giao hàng</h2>
            <p><i class="bi bi-geo-alt" aria-hidden="true"></i>{{ order.shippingAddress || 'Chưa có thông tin địa chỉ.' }}</p>
            <p v-if="order.note"><i class="bi bi-chat-dots" aria-hidden="true"></i>{{ order.note }}</p>
          </div>
          <div class="detail-section totals">
            <h2>Thanh toán</h2>
            <div><span>Tạm tính</span><span>{{ formatPrice(order.subtotal) }}</span></div>
            <div><span>Phí giao hàng</span><span>{{ formatPrice(order.shippingFee) }}</span></div>
            <div v-if="order.discount > 0" class="discount"><span>Giảm giá</span><span>-{{ formatPrice(order.discount) }}</span></div>
            <div class="grand-total"><span>Tổng cộng</span><strong>{{ formatPrice(order.total) }}</strong></div>
          </div>
        </section>
      </template>

      <div v-else class="guest-message">
        <i class="bi bi-truck" aria-hidden="true"></i>
        <p>Lưu mã đơn để tra cứu tiến trình giao hàng tại trang theo dõi đơn.</p>
      </div>

      <div class="actions">
        <template v-if="order">
          <button class="btn btn-outline print-action" @click="printReceipt"><i class="bi bi-printer" aria-hidden="true"></i> In biên nhận</button>
          <router-link :to="`/account/orders/${order.id}`" class="btn btn-primary">Xem chi tiết đơn hàng</router-link>
        </template>
        <router-link v-else :to="trackingLink" class="btn btn-primary">Theo dõi đơn hàng</router-link>
        <router-link to="/menu" class="btn btn-outline">Tiếp tục mua sắm</router-link>
      </div>
    </section>
    </div>
  </div>
</template>

<style scoped>
.success-page { min-height: 60vh; padding: 32px 16px; background: var(--bg); }
.success-card { width: 100%; max-width: 920px; margin: 0 auto; padding: 32px; }
.success-header { display: flex; align-items: center; justify-content: center; gap: 18px; text-align: left; }
.success-icon { flex: 0 0 auto; color: #10b981; font-size: 58px; }
h1 { margin-bottom: 6px; font-size: 26px; }
p { color: var(--text-mid); }
.order-code { margin-top: 10px; color: var(--text-dark); }
.state { display: flex; align-items: center; justify-content: center; gap: 10px; min-height: 120px; margin-top: 24px; border-top: 1px solid var(--border-light); color: var(--text-mid); }
.state-error { flex-wrap: wrap; color: var(--red-active); }
.overview { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 28px; }
.overview > div { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; padding: 16px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); }
.overview span, .overview small { color: var(--text-mid); font-size: 13px; }
.detail-section { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border-light); }
.detail-section h2 { margin-bottom: 14px; font-size: 16px; }
.item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border-light); }
.item:last-child { border-bottom: 0; }
.item img { width: 56px; height: 56px; border-radius: var(--radius-sm); object-fit: cover; }
.item-info { display: flex; flex: 1; flex-direction: column; gap: 3px; min-width: 0; }
.item-info strong { overflow-wrap: anywhere; }
.item-info small, .item-info span, .empty-copy { color: var(--text-mid); font-size: 13px; }
.item-total { min-width: 100px; text-align: right; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
.address p { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.totals > div { display: flex; justify-content: space-between; gap: 16px; padding: 5px 0; font-size: 14px; }
.totals .discount { color: var(--red-active); }
.totals .grand-total { margin-top: 6px; padding-top: 12px; border-top: 1px solid var(--border-light); font-size: 18px; }
.guest-message { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 24px; padding: 18px; border-radius: var(--radius-sm); background: var(--bg); text-align: left; }
.guest-message i { color: var(--primary); font-size: 24px; }
.actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 28px; }
@media (max-width: 640px) {
  .success-page { padding: 16px 10px; }
  .success-card { padding: 24px 16px; }
  .success-header { flex-direction: column; text-align: center; }
  .success-icon { font-size: 50px; }
  .overview, .detail-grid { grid-template-columns: 1fr; gap: 10px; }
  .detail-grid .detail-section { margin-top: 10px; }
  .item { align-items: flex-start; flex-wrap: wrap; }
  .item-info { min-width: calc(100% - 68px); }
  .item-total { margin-left: auto; }
  .actions { flex-direction: column; }
  .actions .btn { width: 100%; }
}
@media print {
  .success-page { padding: 0; background: #fff; }
  .success-card { max-width: none; box-shadow: none; }
  .actions, .print-action, .guest-message { display: none; }
}
</style>

<style scoped>
.success-page{min-height:100vh;padding:24px 16px 72px;background:linear-gradient(180deg,#fff8f0,#faf8f6)}.success-shell{width:min(980px,100%);margin:0 auto}.success-card{max-width:none;padding:0 32px 32px;overflow:hidden;border:1px solid #ece4de;border-radius:22px;background:#fff;box-shadow:0 20px 55px rgba(40,27,20,.08)}.success-hero{margin:0 -32px;padding:34px 32px;color:#fff;background:linear-gradient(135deg,#1f1a17,#3a2b24)}.success-hero p{color:rgba(255,255,255,.72)}.success-hero .order-code{color:#f6c58c}.success-icon{color:#f4a261}.overview>div,.detail-section{border-color:#ece4de}.overview>div{border-radius:14px;background:#fffaf6}.detail-section{padding:20px;border:1px solid #ece4de;border-radius:16px;background:#fff}.detail-grid{gap:14px}.detail-grid .detail-section{margin-top:14px}.items .item:last-child{border-bottom:0}.actions .btn{min-height:44px;border-radius:999px}@media(max-width:640px){.success-page{padding:14px 10px 48px}.success-card{padding:0 16px 22px}.success-hero{margin:0 -16px;padding:28px 18px}.overview{margin-top:18px}.detail-section{margin-top:14px;padding:15px}}
</style>
