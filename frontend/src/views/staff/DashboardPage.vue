<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStaffStore } from '@/stores/staff'
import { formatPrice } from '@/utils/format'

const staffStore = useStaffStore()
const loading = ref(true)

const data = computed(() => {
  const raw = staffStore.dashboard
  if (!raw) return null
  return {
    totalOrdersToday: (raw.confirmedOrders || 0) + (raw.pendingOrders || 0),
    pendingOrders: raw.pendingOrders || 0,
    preparingOrders: raw.confirmedOrders || 0,
    deliveringOrders: 0,
    lowStockIngredients: raw.lowStockIngredients || 0,
    activeStaff: raw.todaySchedule ? 1 : 0,
    ordersByHour: [],
    revenueToday: 0
  }
})

onMounted(async () => {
  await staffStore.fetchDashboard()
  loading.value = false
})
</script>

<template>
  <div>
    <div class="page-header"><h1>Tổng quan</h1></div>
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-mid)">Đang tải...</div>
    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-value">{{ data.totalOrdersToday }}</div><div class="stat-label">Tổng đơn hôm nay</div></div>
        <div class="stat-card"><div class="stat-value">{{ data.pendingOrders }}</div><div class="stat-label">Chờ xác nhận</div></div>
        <div class="stat-card"><div class="stat-value">{{ data.preparingOrders }}</div><div class="stat-label">Đang chế biến</div></div>
        <div class="stat-card"><div class="stat-value">{{ data.deliveringOrders }}</div><div class="stat-label">Đang giao</div></div>
        <div class="stat-card"><div class="stat-value">{{ data.lowStockIngredients }}</div><div class="stat-label">Nguyên liệu sắp hết</div></div>
        <div class="stat-card"><div class="stat-value">{{ data.activeStaff }}</div><div class="stat-label">Nhân viên trực</div></div>
      </div>
    </template>
  </div>
</template>
