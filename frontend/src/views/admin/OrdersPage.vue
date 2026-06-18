<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'

const adminStore = useAdminStore()
const searchTerm = ref('')

onMounted(() => adminStore.fetchOrders())

const filtered = computed(() => {
  const list = adminStore.allOrders
  if (!searchTerm.value) return list
  const q = searchTerm.value.toLowerCase()
  return list.filter(o => (o.orderCode || '').toLowerCase().includes(q))
})
</script>

<template>
  <div>
    <div class="page-header"><h1>Quản lý đơn hàng</h1></div>
    <div class="card card-flat">
      <div class="search-box" style="max-width:320px;margin-bottom:16px">
        <i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn..." />
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>SP</th><th>Tổng</th><th>Thanh toán</th><th>Ngày</th><th>Trạng thái</th></tr></thead>
          <tbody>
            <tr v-for="o in filtered" :key="o.id">
              <td><strong>{{ o.orderCode }}</strong></td><td>User #{{ o.userId }}</td><td>{{ o.items.length }}</td>
              <td>{{ formatPrice(o.total) }}</td><td>{{ o.paymentMethod }}</td><td>{{ formatDate(o.createdAt) }}</td>
              <td><OrderStatusBadge :status="o.status" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
