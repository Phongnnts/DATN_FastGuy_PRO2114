<script setup>
import { ref, onMounted } from 'vue';
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

onMounted(async () => {
  if (!orderId.value && !orderCode.value) {
    status.value = 'error';
    return;
  }
  if (!auth.isLoggedIn && !orderId.value) {
    status.value = 'pending';
    setTimeout(redirect, 3000);
    return;
  }
  for (let attempt = 0; attempt < 12; attempt++) {
    try {
      if (orderId.value) {
        if (attempt > 0 && attempt % 3 === 0) {
          await orderApi.verifyPayment(orderId.value).catch(() => {});
        }
        const order = await orderApi.getById(orderId.value);
        if (order?.paymentStatus === 'PAID') {
          status.value = 'success';
          cart.clear();
          setTimeout(redirect, 2000);
          return;
        }
        if (order?.status === 'CANCELLED') {
          status.value = 'cancelled';
          setTimeout(redirect, 2000);
          return;
        }
      }
    } catch {}
    await new Promise(resolve => setTimeout(resolve, 2500));
  }
  status.value = 'pending';
  setTimeout(redirect, 2000);
});

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
        <p class="text-mid">Đang chuyển hướng...</p>
      </div>

      <div v-else class="payment-state error">
        <i class="bi bi-exclamation-triangle-fill icon-error"></i>
        <h3>Không xác định được trạng thái</h3>
        <p>Vui lòng kiểm tra lại đơn hàng của bạn.</p>
        <router-link to="/" class="btn btn-primary">Về trang chủ</router-link>
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
