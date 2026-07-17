<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { orderApi } from '@/api';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();

const status = ref('loading');
const orderId = ref(Number(route.query.orderId) || null);
const orderCode = ref(String(route.query.orderCode || ''));
let pollingTimer = null;
let redirectTimer = null;
let unmounted = false;

onMounted(async () => {
  if (!orderId.value && !orderCode.value) {
    status.value = 'error';
    return;
  }
  await pollPayment();
});

onUnmounted(() => {
  unmounted = true;
  if (pollingTimer) { clearTimeout(pollingTimer); pollingTimer = null; }
  if (redirectTimer) { clearTimeout(redirectTimer); redirectTimer = null; }
});

async function pollPayment() {
  for (let attempt = 0; attempt < 12; attempt++) {
    if (unmounted) return;
    try {
      if (!auth.isLoggedIn || !orderId.value) {
        status.value = 'pending';
        scheduleRedirect();
        return;
      }
      const order = await orderApi.getById(orderId.value);
      if (unmounted) return;
      if (order?.paymentStatus === 'PAID') {
        status.value = 'success';
        cart.clear();
        scheduleRedirect();
        return;
      }
      if (order?.status === 'CANCELLED') {
        status.value = 'cancelled';
        scheduleRedirect();
        return;
      }
    } catch {}
    await new Promise(resolve => { pollingTimer = setTimeout(resolve, 2500); });
    if (unmounted) return;
  }
  status.value = 'pending';
  scheduleRedirect();
}

function scheduleRedirect() {
  if (redirectTimer) clearTimeout(redirectTimer);
  redirectTimer = setTimeout(() => {
    if (!unmounted) redirect();
  }, 2000);
}

function redirect() {
  if (auth.isLoggedIn && orderId.value) router.replace(`/account/orders/${orderId.value}`);
  else if (orderCode.value) router.replace(`/track-order?code=${orderCode.value}`);
  else router.replace('/');
}
</script>

<template>
  <div class="payment-return-page">
    <div class="card payment-card">
      <div v-if="status === 'loading'" class="payment-state">
        <i class="bi bi-arrow-repeat spin"></i>
        <h3>Dang xu ly...</h3>
      </div>

      <div v-else-if="status === 'success'" class="payment-state success">
        <i class="bi bi-check-circle-fill icon-success"></i>
        <h3>Thanh toan thanh cong!</h3>
        <p v-if="orderCode">Don hang <strong>{{ orderCode }}</strong> da duoc thanh toan.</p>
        <p class="text-mid">Dang chuyen huong...</p>
      </div>

      <div v-else-if="status === 'cancelled'" class="payment-state cancelled">
        <i class="bi bi-x-circle-fill icon-cancelled"></i>
        <h3>Da huy thanh toan</h3>
        <p>Ban co the thu lai hoac chon phuong thuc thanh toan khac.</p>
        <p class="text-mid">Dang chuyen huong...</p>
      </div>

      <div v-else-if="status === 'pending'" class="payment-state">
        <i class="bi bi-clock-fill icon-pending"></i>
        <h3>Dang cho xu ly</h3>
        <p>Vui long kiem tra trang thai don hang sau.</p>
        <p class="text-mid">Dang chuyen huong...</p>
      </div>

      <div v-else class="payment-state error">
        <i class="bi bi-exclamation-triangle-fill icon-error"></i>
        <h3>Khong xac dinh duoc trang thai</h3>
        <p>Vui long kiem tra lai don hang cua ban.</p>
        <router-link to="/" class="btn btn-primary">Ve trang chu</router-link>
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
