<script setup>
import { ref, onBeforeUnmount, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { orderApi } from '@/api';
import CheckoutStepper from '@/components/common/CheckoutStepper.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();

const status = ref('loading');
const orderId = ref(Number(route.query.orderId) || null);
const orderCode = ref(String(route.query.orderCode || ''));
const proofStorageKey = `guest-payment-proof:${orderCode.value}`;
const returnProof = String(route.query.token || sessionStorage.getItem(proofStorageKey) || '');
let stopped = false;
let timer = null;
const wait = ms => new Promise(resolve => { timer = setTimeout(resolve, ms); });

function clearReturnProof() {
  if (orderCode.value) sessionStorage.removeItem(proofStorageKey);
}

async function checkStatus() {
  if (!orderId.value && !orderCode.value) {
    status.value = 'error';
    return;
  }
  if (!auth.isLoggedIn && (!orderCode.value || !returnProof)) {
    status.value = 'error';
    return;
  }
  status.value = 'loading';
  for (let attempt = 0; attempt < 12 && !stopped; attempt++) {
    try {
      const order = auth.isLoggedIn && orderId.value
        ? await orderApi.getById(orderId.value)
        : await orderApi.getGuestPaymentStatus(orderCode.value, returnProof);
      if (order?.paymentStatus === 'PAID') {
        status.value = 'success';
        clearReturnProof();
        cart.clear();
        timer = setTimeout(redirect, 2000);
        return;
      }
      if ((order?.status || order?.orderStatus) === 'CANCELLED') {
        status.value = 'cancelled';
        clearReturnProof();
        timer = setTimeout(redirect, 2000);
        return;
      }
    } catch {}
    await wait(2500);
  }
  if (!stopped) status.value = 'pending';
}

function retry() {
  clearTimeout(timer);
  checkStatus();
}

onMounted(() => {
  if (route.query.token) router.replace({ query: { ...route.query, token: undefined } });
  checkStatus();
});
onBeforeUnmount(() => { stopped = true; clearTimeout(timer); });

function redirect() {
  if (auth.isLoggedIn && orderId.value) router.replace(`/account/orders/${orderId.value}`);
  else if (orderCode.value) router.replace(`/track-order?code=${orderCode.value}`);
  else router.replace('/');
}
</script>

<template>
  <div class="payment-return-page">
    <div class="payment-shell">
      <CheckoutStepper :current="3" />
    <div class="card payment-card payment-status-card" :class="`status-${status}`" aria-live="polite" aria-atomic="true">
      <div v-if="status === 'loading'" class="payment-state">
        <i class="bi bi-arrow-repeat spin"></i>
        <h3>Đang xử lý...</h3>
      </div>

      <div v-else-if="status === 'success'" class="payment-state success">
        <i class="bi bi-check-circle-fill icon-success"></i>
        <h3>Thanh toán thành công!</h3>
        <p v-if="orderCode">Đơn hàng <strong>{{ orderCode }}</strong> đã được thanh toán.</p>
        <p class="text-mid">Đang chuyển hướng...</p>
      </div>

      <div v-else-if="status === 'cancelled'" class="payment-state cancelled">
        <i class="bi bi-x-circle-fill icon-cancelled"></i>
        <h3>Đã hủy thanh toán</h3>
        <p>Bạn có thể thử lại hoặc chọn phương thức thanh toán khác.</p>
        <p class="text-mid">Đang chuyển hướng...</p>
      </div>

      <div v-else-if="status === 'pending'" class="payment-state">
        <i class="bi bi-clock-fill icon-pending"></i>
        <h3>Đang chờ xử lý</h3>
        <p>Vui lòng kiểm tra trạng thái đơn hàng sau.</p>
        <button type="button" class="btn btn-primary" @click="retry">Kiểm tra lại</button>
        <button type="button" class="btn btn-outline" @click="redirect">Xem đơn hàng</button>
      </div>

      <div v-else class="payment-state error">
        <i class="bi bi-exclamation-triangle-fill icon-error"></i>
        <h3>Không xác định được trạng thái</h3>
        <p>Vui lòng kiểm tra lại đơn hàng của bạn.</p>
        <router-link to="/home" class="btn btn-primary">Về trang chủ</router-link>
      </div>
    </div>
    </div>
  </div>
</template>

<style scoped>
.payment-return-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 24px;
}
.payment-card {
  max-width: 480px;
  width: 100%;
  text-align: center;
  padding: 48px 24px;
}
.payment-state i {
  font-size: 56px;
  margin-bottom: 16px;
}
.payment-state h3 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}
.payment-state p {
  color: var(--text-mid);
  margin-bottom: 4px;
}
.text-mid {
  color: var(--text-mid);
  font-size: 13px;
}
.icon-success { color: #10b981; }
.icon-cancelled { color: #f59e0b; }
.icon-pending { color: #3b82f6; }
.icon-error { color: #ef4444; }
.spin {
  animation: spin 1s linear infinite;
  color: var(--primary);
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.btn { margin-top: 16px; }
</style>

<style scoped>
.payment-return-page{align-items:flex-start;min-height:100vh;padding:24px 16px 64px;background:linear-gradient(180deg,#fff8f0,#faf8f6)}.payment-shell{width:min(980px,100%)}.payment-status-card{position:relative;overflow:hidden;max-width:620px;margin:36px auto 0;padding:54px 32px;border:1px solid #ece4de;border-radius:22px;background:#fff;box-shadow:0 20px 55px rgba(40,27,20,.08)}.payment-status-card::before{position:absolute;top:0;right:0;left:0;height:5px;background:linear-gradient(90deg,#df683e,#f3b05f);content:""}.payment-state i{display:inline-grid;width:78px;height:78px;place-items:center;margin-bottom:20px;border-radius:50%;background:#fff5ef}.payment-state h3{font-size:26px}.payment-state .btn{min-height:44px;border-radius:999px}@media(max-width:640px){.payment-return-page{padding:14px 10px 48px}.payment-status-card{margin-top:18px;padding:40px 18px}.payment-state h3{font-size:22px}}
</style>
