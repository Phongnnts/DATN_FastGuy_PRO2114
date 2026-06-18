<script setup>
import { ref } from 'vue';
import { useOrderStore } from '@/stores/order';
import { ORDER_STATUS_LABEL } from '@/utils/constants';
import { formatDate } from '@/utils/format';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const orderStore = useOrderStore();
const orderCode = ref('');
const trackingResult = ref(null);
const error = ref('');
const searched = ref(false);

function track() {
  if (!orderCode.value) return;
  error.value = '';
  searched.value = true;
  const result = orderStore.trackOrder(orderCode.value);
  if (result) {
    trackingResult.value = result;
  } else {
    trackingResult.value = null;
    error.value = 'Không tìm thấy đơn hàng với mã này';
  }
}
</script>

<template>
  <div class="track-order-page">
    <div class="container">
      <div class="page-header text-center">
        <h1>Tra cứu đơn hàng</h1>
        <p>Nhập mã đơn hàng để kiểm tra tình trạng</p>
      </div>
      <div class="track-search card" style="max-width: 500px; margin: 0 auto">
        <div class="search-box" style="display: flex; gap: 8px">
          <input
            v-model="orderCode"
            type="text"
            class="form-input"
            placeholder="VD: FG-20240201-001"
            @keyup.enter="track"
            style="flex: 1"
          />
          <button class="btn btn-primary" @click="track">
            <i class="bi bi-search"></i>
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
.track-timeline h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}
</style>
