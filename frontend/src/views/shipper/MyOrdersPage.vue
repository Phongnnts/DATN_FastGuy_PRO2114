<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';

const router = useRouter();
const shipperStore = useShipperStore();
const activeTab = ref('pickup');
const searchTerm = ref('');

const pickupOrders = computed(() =>
  shipperStore.myOrders.filter(o => o.status === 'READY')
);
const deliveringOrders = computed(() =>
  shipperStore.myOrders.filter(o => o.status === 'PICKED_UP')
);
const historyOrders = computed(() =>
  shipperStore.myOrders.filter(o => o.status === 'DELIVERED' || o.status === 'CANCELLED')
);

const filteredPickup = computed(() => {
  if (!searchTerm.value) return pickupOrders.value;
  const q = searchTerm.value.toLowerCase();
  return pickupOrders.value.filter(o =>
    o.orderCode.toLowerCase().includes(q) ||
    o.customerName.toLowerCase().includes(q) ||
    o.customerAddress.toLowerCase().includes(q)
  );
});

const filteredDelivering = computed(() => {
  if (!searchTerm.value) return deliveringOrders.value;
  const q = searchTerm.value.toLowerCase();
  return deliveringOrders.value.filter(o =>
    o.orderCode.toLowerCase().includes(q) ||
    o.customerName.toLowerCase().includes(q) ||
    o.customerAddress.toLowerCase().includes(q)
  );
});

const filteredHistory = computed(() => {
  if (!searchTerm.value) return historyOrders.value;
  const q = searchTerm.value.toLowerCase();
  return historyOrders.value.filter(o =>
    o.orderCode.toLowerCase().includes(q) ||
    o.customerName.toLowerCase().includes(q)
  );
});

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

    <div class="search-box" style="margin-bottom:12px">
      <i class="bi bi-search"></i>
      <input v-model="searchTerm" class="form-input" placeholder="Tìm đơn hàng..." />
    </div>

    <div class="shipper-tabs">
      <button class="tab" :class="{ active: activeTab === 'pickup' }" @click="activeTab = 'pickup'">
        Chờ lấy ({{ pickupOrders.length }})
      </button>
      <button class="tab" :class="{ active: activeTab === 'delivering' }" @click="activeTab = 'delivering'">
        Đang giao ({{ deliveringOrders.length }})
      </button>
      <button class="tab" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
        Lịch sử ({{ historyOrders.length }})
      </button>
    </div>

    <div v-if="activeTab === 'pickup'" class="order-cards">
      <div v-if="filteredPickup.length === 0" class="shipper-empty">
        <i class="bi bi-inbox"></i>
        <p>{{ searchTerm ? 'Không tìm thấy đơn hàng' : 'Chưa có đơn chờ lấy' }}</p>
      </div>
      <div
        v-for="order in filteredPickup"
        :key="order.id"
        class="order-card pickup-card"
        @click="goDetail(order.id)"
      >
        <div class="card-top">
          <strong class="order-code">{{ order.orderCode }}</strong>
          <span class="pickup-badge">Chờ lấy</span>
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

    <div v-else-if="activeTab === 'delivering'" class="order-cards">
      <div v-if="filteredDelivering.length === 0" class="shipper-empty">
        <i class="bi bi-inbox"></i>
        <p>{{ searchTerm ? 'Không tìm thấy đơn hàng' : 'Chưa có đơn đang giao' }}</p>
      </div>
      <div
        v-for="order in filteredDelivering"
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
      <div v-if="filteredHistory.length === 0" class="shipper-empty">
        <i class="bi bi-clock-history"></i>
        <p>{{ searchTerm ? 'Không tìm thấy đơn hàng' : 'Chưa có lịch sử giao hàng' }}</p>
      </div>
      <div
        v-for="order in filteredHistory"
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
  border: 1px solid var(--border-light);
  background: #fff;
  border-radius: var(--radius);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: var(--text-mid);
  transition: all var(--transition-fast);
}
.shipper-tabs .tab.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.shipper-empty { text-align: center; padding: 40px 0; color: var(--text-mid); }
.shipper-empty i { font-size: 32px; display: block; margin-bottom: 8px; color: var(--border); }
.order-cards { display: flex; flex-direction: column; gap: 10px; }
.order-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.order-card:hover { transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-code { font-size: 14px; }
.active-badge,
.status-delivered,
.status-cancelled {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}
.active-badge { background: #dbeafe; color: #1e40af; }
.pickup-badge { background: #fef3c7; color: #92400e; }
.status-delivered { background: #dcfce7; color: #166534; }
.status-cancelled { background: #fee2e2; color: #991b1b; }
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
  border-top: 1px solid var(--border-light);
}
.order-total { font-size: 17px; font-weight: 800; }
.order-time { font-size: 12px; color: var(--text-light); }
</style>
