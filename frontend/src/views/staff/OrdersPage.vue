<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStaffStore } from '@/stores/staff'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'

const router = useRouter()
const staffStore = useStaffStore()

const activeTab = ref('PENDING')
const searchTerm = ref('')
const error = ref('')

const tabs = [
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng giao' },
]

onMounted(() => switchTab('PENDING'))

async function switchTab(tab) {
  activeTab.value = tab
  error.value = ''
  try {
    if (tab === 'PENDING') await staffStore.fetchOrders()
    else if (tab === 'CONFIRMED') await staffStore.fetchConfirmedOrders()
    else if (tab === 'PREPARING') await staffStore.fetchPreparingOrders()
    else if (tab === 'READY') await staffStore.fetchReadyOrders()
  } catch (e) {
    error.value = e.message || 'Lỗi tải dữ liệu'
  }
}

const filteredOrders = computed(() => {
  let result = staffStore.allOrders
  if (activeTab.value !== 'ALL') {
    result = result.filter(o => o.status === activeTab.value)
  }
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase()
    result = result.filter(o => o.orderCode.toLowerCase().includes(q))
  }
  return result
})

function goDetail(id) { router.push(`/staff/orders/${id}`) }
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý đơn hàng</h1>
      <div class="search-box" style="max-width:320px">
        <i class="bi bi-search"></i>
        <input v-model="searchTerm" type="text" class="form-input" placeholder="Tìm mã đơn..." />
      </div>
    </div>
    <div class="card card-flat">
      <div class="tabs">
        <button v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" @click="switchTab(tab.key)">{{ tab.label }}</button>
      </div>
      <div v-if="error" class="empty-state">
        <i class="bi bi-exclamation-triangle"></i>
        <h3>{{ error }}</h3>
      </div>
      <div v-else-if="staffStore.loading" class="empty-state">
        <i class="bi bi-arrow-repeat spin"></i>
        <h3>Đang tải đơn hàng...</h3>
      </div>
      <div v-else-if="filteredOrders.length === 0" class="empty-state">
        <i class="bi bi-receipt"></i>
        <h3>Chưa có đơn hàng</h3>
      </div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead>
            <tr><th>Mã đơn</th><th>Khách hàng</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Ngày đặt</th><th>Trạng thái</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id" @click="goDetail(order.id)" class="order-row">
              <td data-label="Mã đơn"><strong>{{ order.orderCode }}</strong></td>
              <td data-label="Khách hàng">{{ order.customerName || 'Người dùng #' + order.userId }}</td>
              <td data-label="Sản phẩm">{{ order.items ? order.items.length : 0 }} món</td>
              <td data-label="Tổng tiền">{{ formatPrice(order.total) }}</td>
              <td data-label="Ngày đặt">{{ formatDate(order.createdAt) }}</td>
              <td data-label="Trạng thái"><OrderStatusBadge :status="order.status" /></td>
              <td data-label="Chi tiết"><i class="bi bi-chevron-right"></i></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.table tbody tr {
  cursor: pointer;
  transition: background var(--transition-fast);
}
.table tbody tr:hover {
  background: #f0f4f8;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@media (max-width: 768px) {
  .table thead { display: none; }
  .table tbody tr { display: block; padding: 12px; margin-bottom: 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); }
  .table tbody td { display: flex; justify-content: space-between; padding: 6px 0; border: none; font-size: 13px; }
  .table tbody td::before { content: attr(data-label); font-weight: 600; color: var(--text-mid); }
}
</style>
