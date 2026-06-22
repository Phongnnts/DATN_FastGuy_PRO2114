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

const tabs = [
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Đã sẵn sàng' },
]

onMounted(() => staffStore.fetchOrders())

async function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'PENDING') await staffStore.fetchOrders()
  else if (tab === 'CONFIRMED') await staffStore.fetchConfirmedOrders()
  else if (tab === 'PREPARING') await staffStore.fetchPreparingOrders()
  else if (tab === 'READY') await staffStore.fetchReadyOrders()
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
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr><th>Mã đơn</th><th>Khách hàng</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Ngày đặt</th><th>Trạng thái</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id" @click="goDetail(order.id)" style="cursor:pointer">
              <td><strong>{{ order.orderCode }}</strong></td>
              <td>{{ order.customerName || 'Người dùng #' + order.userId }}</td>
              <td>{{ order.items ? order.items.length : 0 }} món</td>
              <td>{{ formatPrice(order.total) }}</td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td><OrderStatusBadge :status="order.status" /></td>
              <td><i class="bi bi-chevron-right"></i></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
