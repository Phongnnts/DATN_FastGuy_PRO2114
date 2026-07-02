<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const router = useRouter();
const orderStore = useOrderStore();
const activeTab = ref('ALL');
const orders = computed(() => orderStore.userOrders);

onMounted(() => orderStore.fetchOrders());

const tabs = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chuẩn bị' },
  { key: 'READY', label: 'Sẵn sàng giao' },
  { key: 'PICKED_UP', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

const filteredOrders = computed(() => {
  if (activeTab.value === 'ALL') return orders.value;
  return orders.value.filter((o) => o.status === activeTab.value);
});

function goDetail(id) {
  router.push(`/account/orders/${id}`);
}
</script>

<template>
  <div class="orders-page">
    <div class="card">
      <h3 style="font-size: 18px; font-weight: 700; margin-bottom: 20px">
        Đơn hàng của tôi
      </h3>
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
      <div
        v-if="filteredOrders.length === 0"
        class="empty-state"
        style="padding: 40px 0"
      >
        <i class="bi bi-box"></i>
        <h3>Chưa có đơn hàng</h3>
        <router-link to="/menu" class="btn btn-primary"
          >Đặt hàng ngay</router-link
        >
      </div>
      <div v-else class="orders-list">
        <div
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
          @click="goDetail(order.id)"
        >
          <div class="order-card-header">
            <div>
              <span class="order-code">{{ order.orderCode }}</span>
              <span class="order-date">{{ formatDate(order.createdAt) }}</span>
            </div>
            <OrderStatusBadge :status="order.status" />
          </div>
          <div class="order-card-items">
            <div
              v-for="item in order.items.slice(0, 3)"
              :key="item.productId"
              class="order-item-thumb"
            >
              <img :src="item.image" :alt="item.productName" />
            </div>
            <span v-if="order.items.length > 3" class="more-items"
              >+{{ order.items.length - 3 }}</span
            >
          </div>
          <div class="order-card-footer">
            <span class="order-total">{{ formatPrice(order.total) }}</span>
            <span class="order-payment">{{
              order.paymentMethod === 'COD' ? 'COD' : 'Đã thanh toán'
            }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.order-card {
  background: #fff;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.order-card:hover {
  border-color: var(--primary);
}
.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.order-code {
  font-size: 14px;
  font-weight: 700;
  display: block;
}
.order-date {
  font-size: 12px;
  color: var(--text-light);
}
.order-card-items {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.order-item-thumb img {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}
.more-items {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-mid);
}
.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-total {
  font-size: 18px;
  font-weight: 800;
}
.order-payment {
  font-size: 12px;
  color: var(--text-mid);
}
</style>
