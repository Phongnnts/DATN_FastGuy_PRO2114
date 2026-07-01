<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';

const router = useRouter();
const shipperStore = useShipperStore();
const loading = ref(true);

const dashboard = computed(() => shipperStore.dashboard);
const availableCount = computed(() => shipperStore.availableOrders.length);

onMounted(async () => {
  await Promise.all([
    shipperStore.fetchDashboard(),
    shipperStore.fetchAvailableOrders(),
  ]);
  loading.value = false;
});

function goDetail(id) {
  router.push(`/shipper/orders/${id}`);
}
</script>

<template>
  <div>
    <div class="shipper-hero">
      <div class="hero-stat">
        <span class="hero-number">{{ availableCount }}</span>
        <span class="hero-label">Đơn chờ nhận</span>
      </div>
    </div>

    <div v-if="dashboard" class="shipper-stats">
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.todayDelivered || 0 }}</span>
        <span class="mini-label">Đã giao hôm nay</span>
      </div>
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.activeCount || 0 }}</span>
        <span class="mini-label">Đang giao</span>
      </div>
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.totalDelivered || 0 }}</span>
        <span class="mini-label">Tổng đã giao</span>
      </div>
    </div>

    <h3 style="margin: 16px 0 12px; font-size: 15px; font-weight: 700;">
      <i class="bi bi-box"></i> Đơn hàng sẵn sàng
    </h3>

    <div v-if="loading" class="shipper-empty">Đang tải...</div>
    <div v-else-if="availableCount === 0" class="shipper-empty">
      <i class="bi bi-emoji-smile" style="font-size: 40px;"></i>
      <p>Không có đơn nào đang chờ</p>
    </div>
    <div v-else class="order-cards">
      <div
        v-for="order in shipperStore.availableOrders"
        :key="order.id"
        class="order-card"
        @click="goDetail(order.id)"
      >
        <div class="card-top">
          <strong class="order-code">{{ order.orderCode }}</strong>
          <span class="order-status pending">Sẵn sàng giao</span>
        </div>
        <div class="card-body">
          <p><i class="bi bi-person"></i> {{ order.customerName }}</p>
          <p><i class="bi bi-geo-alt"></i> {{ order.customerAddress }}</p>
        </div>
        <div class="card-bottom">
          <span class="order-total">{{ formatPrice(order.total) }}</span>
          <span class="order-time">{{ formatDate(order.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shipper-hero {
  background: var(--primary);
  color: #fff;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  margin-bottom: 12px;
}
.shipper-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.mini-stat {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.mini-num {
  font-size: 22px;
  font-weight: 800;
  display: block;
  color: var(--primary);
}
.mini-label {
  font-size: 11px;
  color: var(--text-mid);
  margin-top: 2px;
}
.hero-number {
  font-size: 44px;
  font-weight: 800;
  display: block;
}
.hero-label {
  font-size: 14px;
  opacity: 0.9;
}
.shipper-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-mid);
}
.order-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-code {
  font-size: 14px;
}
.order-status.pending {
  background: #FEF3C7;
  color: #92400E;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 600;
}
.card-body p {
  font-size: 13px;
  color: var(--text-mid);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}
.order-total {
  font-size: 17px;
  font-weight: 800;
}
.order-time {
  font-size: 12px;
  color: var(--text-light);
}
</style>
