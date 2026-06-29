<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';

const router = useRouter();
const shipperStore = useShipperStore();
const activeTab = ref('active');

const activeOrders = computed(() =>
  shipperStore.myOrders.filter(o => o.status === 'PICKED_UP' || o.status === 'READY')
);
const historyOrders = computed(() =>
  shipperStore.myOrders.filter(o => o.status === 'DELIVERED' || o.status === 'CANCELLED')
);

onMounted(async () => {
  await shipperStore.fetchMyOrders();
});

function goDetail(id) {
  router.push(`/shipper/orders/${id}`);
}
</script>

<template>
  <div>
    <h3 style="font-size: 16px; font-weight: 700; margin-bottom: 12px;">
      <i class="bi bi-bicycle"></i> Đơn hàng của tôi
    </h3>

    <div class="shipper-tabs">
      <button class="tab" :class="{ active: activeTab === 'active' }" @click="activeTab = 'active'">
        Đang giao ({{ activeOrders.length }})
      </button>
      <button class="tab" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
        Lịch sử ({{ historyOrders.length }})
      </button>
    </div>

    <div v-if="activeTab === 'active'" class="order-cards">
      <div v-if="activeOrders.length === 0" class="shipper-empty">
        <i class="bi bi-inbox"></i>
        <p>Chưa có đơn nào đang giao</p>
      </div>
      <div
        v-for="order in activeOrders"
        :key="order.id"
        class="order-card"
        @click="goDetail(order.id)"
      >
        <div class="card-top">
          <strong class="order-code">{{ order.orderCode }}</strong>
          <span class="active-badge">Đang giao</span>
        </div>
        <div class="card-body">
          <p><i class="bi bi-person"></i> {{ order.customerName }}</p>
          <p><i class="bi bi-telephone"></i> {{ order.customerPhone }}</p>
          <p><i class="bi bi-geo-alt"></i> {{ order.customerAddress }}</p>
        </div>
        <div class="card-bottom">
          <span class="order-total">{{ formatPrice(order.total) }}</span>
        </div>
      </div>
    </div>

    <div v-else class="order-cards">
      <div v-if="historyOrders.length === 0" class="shipper-empty">
        <i class="bi bi-clock-history"></i>
        <p>Chưa có lịch sử giao hàng</p>
      </div>
      <div
        v-for="order in historyOrders"
        :key="order.id"
        class="order-card"
        @click="goDetail(order.id)"
      >
        <div class="card-top">
          <strong class="order-code">{{ order.orderCode }}</strong>
          <span :class="'status-' + order.status.toLowerCase()">
            {{ order.status === 'DELIVERED' ? 'Đã giao' : 'Đã hủy' }}
          </span>
        </div>
        <div class="card-body">
          <p><i class="bi bi-person"></i> {{ order.customerName }}</p>
          <p><i class="bi bi-geo-alt"></i> {{ order.customerAddress }}</p>
        </div>
        <div class="card-bottom">
          <span class="order-total">{{ formatPrice(order.total) }}</span>
          <span class="order-time">{{ formatDate(order.deliveredAt || order.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shipper-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.shipper-tabs .tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: #fff;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: var(--text-mid);
}
.shipper-tabs .tab.active {
  background: var(--primary);
  color: #fff;
}
.shipper-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-mid);
}
.shipper-empty i {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
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
.active-badge {
  background: #DBEAFE;
  color: #1E40AF;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 600;
}
.status-delivered {
  background: #D1FAE5;
  color: #065F46;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 600;
}
.status-cancelled {
  background: #FEE2E2;
  color: #991B1B;
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
