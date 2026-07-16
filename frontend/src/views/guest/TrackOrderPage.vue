<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import { ORDER_STATUS_LABEL } from '@/utils/constants';
import { formatDate } from '@/utils/format';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const route = useRoute();
const orderStore = useOrderStore();
const orderCode = ref('');
const phoneSuffix = ref('');
const trackingResult = ref(null);
const error = ref('');
const searched = ref(false);
const justCreated = ref(route.query.created === '1');
let paymentPolling = null;

async function track() {
  if (!orderCode.value || !phoneSuffix.value) return;
  if (!/^\d{4}$/.test(phoneSuffix.value)) {
    error.value = 'Số điện thoại phải là đúng 4 chữ số cuối';
    return;
  }
  error.value = '';
  searched.value = true;
  try {
    const result = await orderStore.trackOrder(orderCode.value, phoneSuffix.value);
    if (!result) {
      trackingResult.value = null;
      error.value = 'Không tìm thấy đơn hàng với mã này';
      return;
    }
    trackingResult.value = result;
    if (result.checkoutUrl && result.paymentStatus === 'UNPAID') window.location.assign(result.checkoutUrl);
    if (result.paymentMethod === 'BANK_TRANSFER' && result.paymentStatus === 'UNPAID') startPaymentPolling();
  } catch (e) {
    trackingResult.value = null;
    error.value = e.message || 'Không thể tra cứu đơn hàng';
  }
}

function startPaymentPolling() {
  if (paymentPolling || !trackingResult.value?.id) return;
  paymentPolling = setInterval(async () => {
    try {
      const result = await orderStore.trackOrder(orderCode.value, phoneSuffix.value);
      if (!result) return;
       trackingResult.value = result;
       if (result.checkoutUrl && result.paymentStatus === 'UNPAID') {
         window.location.assign(result.checkoutUrl);
         return;
       }
       if (result.paymentStatus === 'PAID' || result.status === 'CANCELLED') {
        clearInterval(paymentPolling);
        paymentPolling = null;
      }
    } catch {}
  }, 5000);
}

onMounted(async () => {
  const code = route.query.code;
  if (code) {
    orderCode.value = code;
    await track();
    startPaymentPolling();
  }
});

onUnmounted(() => {
  if (paymentPolling) clearInterval(paymentPolling);
});
</script>

<template>
  <div class="track-order-page">
    <div class="container">
      <div class="page-header text-center">
        <h1>Tra cứu đơn hàng</h1>
        <p>Nhập mã đơn hàng để kiểm tra tình trạng</p>
      </div>
      <div class="track-search card" style="max-width: 500px; margin: 0 auto">
        <div class="search-box" style="display: flex; flex-direction: column; gap: 8px">
          <input
            v-model="orderCode"
            type="text"
            class="form-input"
            placeholder="VD: FG-20240201-001"
            @keyup.enter="track"
          />
          <input
            v-model="phoneSuffix"
            type="text"
            inputmode="numeric"
            pattern="\d{4}"
            class="form-input"
            maxlength="4"
            placeholder="4 chữ số cuối SĐT (VD: 1234)"
            @keyup.enter="track"
          />
          <button class="btn btn-primary" @click="track">
            <i class="bi bi-search"></i> Tra cứu
          </button>
        </div>
      </div>
      <div v-if="error" class="empty-state" style="margin-top: 40px">
        <i class="bi bi-search"></i>
        <h3>{{ error }}</h3>
      </div>
      <div
        v-if="trackingResult"
        class="track-result card"
        style="max-width: 600px; margin: 24px auto 0"
      >
        <div v-if="justCreated" class="track-success">
          <i class="bi bi-check-circle-fill"></i> Đặt đơn thành công. Hãy lưu lại mã đơn hàng để theo dõi.
        </div>
        <div class="track-header">
          <div>
            <h3>Đơn hàng {{ trackingResult.orderCode }}</h3>
            <p style="color: var(--text-mid); font-size: 14px">
              {{ formatDate(trackingResult.createdAt) }}
            </p>
          </div>
          <OrderStatusBadge :status="trackingResult.status" />
        </div>
        <div class="track-items">
          <div
            v-for="item in trackingResult.items"
            :key="item.productId"
            class="track-item"
          >
            <img :src="item.image" :alt="item.productName" />
            <div>
              <div>{{ item.productName }}</div>
              <div style="font-size: 13px; color: var(--text-mid)">
                x{{ item.quantity }}
              </div>
            </div>
          </div>
        </div>
        <div v-if="trackingResult.paymentMethod === 'BANK_TRANSFER' && trackingResult.paymentStatus === 'UNPAID' && trackingResult.checkoutUrl" class="payment-card">
          <h4>Thanh toán PayOS</h4>
          <a :href="trackingResult.checkoutUrl" class="btn btn-primary">Mở trang thanh toán</a>
        </div>
        <div class="track-timeline">
          <h4>Trạng thái đơn hàng</h4>
          <OrderTimeline :history="trackingResult.statusHistory" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.track-order-page {
  padding: 40px 0;
}
.track-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.track-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.track-items {
  margin-bottom: 20px;
}
.track-success { margin-bottom: 16px; padding: 10px 12px; border-radius: var(--radius-sm); background: #ecfdf5; color: #047857; font-size: 13px; font-weight: 600; }
.track-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.track-item img {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}
.payment-card {
  margin: 20px 0;
  padding: 16px;
  text-align: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.payment-card h4 { font-size: 16px; margin-bottom: 12px; }
.payment-card img { width: 220px; height: 220px; }
.payment-card p { margin-top: 8px; color: var(--text-mid); }
.track-timeline h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}
</style>
