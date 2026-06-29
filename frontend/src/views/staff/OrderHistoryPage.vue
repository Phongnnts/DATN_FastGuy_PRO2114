<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStaffStore } from '@/stores/staff'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'

const staffStore = useStaffStore()
const searchTerm = ref('')

const historyOrders = computed(() => {
  let result = staffStore.allOrders.filter(o => ['DELIVERED', 'CANCELLED'].includes(o.status))
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase()
    result = result.filter(o => o.orderCode.toLowerCase().includes(q))
  }
  return result
})

onMounted(async () => {
  await staffStore.fetchOrders()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Lịch sử đơn hàng</h1>
      <div class="search-box" style="max-width:320px">
        <i class="bi bi-search"></i>
        <input v-model="searchTerm" type="text" class="form-input" placeholder="Tìm mã đơn..." />
      </div>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Tổng tiền</th><th>Ngày</th><th>Kết thúc</th><th>Trạng thái</th></tr></thead>
          <tbody>
            <tr v-for="order in historyOrders" :key="order.id">
              <td><strong>{{ order.orderCode }}</strong></td>
              <td>Người dùng #{{ order.userId }}</td>
              <td>{{ formatPrice(order.total) }}</td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td>{{ formatDate(order.updatedAt) }}</td>
              <td><OrderStatusBadge :status="order.status" /></td>
            </tr>
            <tr v-if="historyOrders.length === 0"><td colspan="6" class="text-center" style="padding:40px">Chưa có lịch sử</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
