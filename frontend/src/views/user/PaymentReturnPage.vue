<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const cart = useCartStore();

const status = ref('loading');
const orderId = ref(null);
const orderCode = ref('');

onMounted(() => {
  const payosStatus = route.query.status;
  const isCancel = route.query.cancel === 'true';
  const payosOrderCode = route.query.orderCode;

  const stored = JSON.parse(sessionStorage.getItem('payos_order') || '{}');
  orderId.value = stored.orderId || null;
  orderCode.value = stored.orderCode || '';

  if (payosStatus === 'PAID') {
    status.value = 'success';
    cart.clear();
    sessionStorage.removeItem('payos_order');
    setTimeout(() => redirect(), 3000);
  } else if (isCancel || payosStatus === 'CANCELLED') {
    status.value = 'cancelled';
    sessionStorage.removeItem('payos_order');
    setTimeout(() => redirect(), 3000);
  } else if (payosOrderCode) {
    // Unknown status but we have an order code
    status.value = 'pending';
    setTimeout(() => redirect(), 3000);
  } else {
    status.value = 'error';
  }
});

function redirect() {
  if (auth.isLoggedIn && orderId.value) {
    router.replace(`/account/orders/${orderId.value}`);
  } else if (orderCode.value) {
    router.replace(`/track-order?code=${orderCode.value}`);
  } else {
    router.replace('/');
  }
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
