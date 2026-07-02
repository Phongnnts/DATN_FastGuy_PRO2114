<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const adminStore = useAdminStore();
const searchTerm = ref('');
const activeStatus = ref('');
const currentPage = ref(1);
const pageSize = 10;

const statusFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

onMounted(() => adminStore.fetchOrders());

const filtered = computed(() => {
  let list = adminStore.allOrders;
  if (activeStatus.value) {
    list = list.filter((o) => o.status === activeStatus.value);
  }
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase();
    list = list.filter((o) => (o.orderCode || '').toLowerCase().includes(q));
  }
  return list;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filtered.value.slice(start, start + pageSize);
});

function setStatus(s) {
  activeStatus.value = s;
  currentPage.value = 1;
}
</script>

<template>
  <div>
    <div class="page-header"><h1>Quản lý đơn hàng</h1></div>
    <div class="card card-flat">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin-bottom:16px">
        <div class="search-box" style="max-width:320px">
          <i class="bi bi-search"></i>
          <input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn..." />
        </div>
      </div>
      <div class="tabs" style="margin-bottom:16px">
        <button v-for="f in statusFilters" :key="f.key" class="tab" :class="{ active: activeStatus === f.key }" @click="setStatus(f.key)">{{ f.label }}</button>
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>SP</th>
              <th>Tổng</th>
              <th>Thanh toán</th>
              <th>Ngày</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in paged" :key="o.id || o.orderId">
              <td><strong>{{ o.orderCode }}</strong></td>
              <td>User #{{ o.userId }}</td>
              <td>{{ (o.items || []).length }}</td>
              <td>{{ formatPrice(o.total) }}</td>
              <td>{{ o.paymentMethod }}</td>
              <td>{{ formatDate(o.createdAt) }}</td>
              <td><OrderStatusBadge :status="o.status" /></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="pagination" style="margin-top:16px;justify-content:center">
        <button :disabled="currentPage <= 1" @click="currentPage--"><i class="bi bi-chevron-left"></i></button>
        <span style="padding:0 12px;font-size:14px">{{ currentPage }} / {{ totalPages }}</span>
        <button :disabled="currentPage >= totalPages" @click="currentPage++"><i class="bi bi-chevron-right"></i></button>
      </div>
    </div>
  </div>
</template>
