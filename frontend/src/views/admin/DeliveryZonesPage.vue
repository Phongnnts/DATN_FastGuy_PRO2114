<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { formatPrice } from '@/utils/format'

const adminStore = useAdminStore()

onMounted(() => adminStore.fetchZones())
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Khu vực giao hàng</h1>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>ID</th><th>Quận/Huyện</th><th>Phí ship</th><th>Kích hoạt</th></tr></thead>
          <tbody>
            <tr v-for="z in adminStore.allZones" :key="z.zoneId">
              <td>{{ z.zoneId }}</td>
              <td><strong>{{ z.districtName }}</strong></td>
              <td>{{ z.shippingFee === 0 ? 'Miễn phí' : formatPrice(z.shippingFee) }}</td>
              <td><i :class="z.isActive ? 'bi bi-check-circle-fill' : 'bi bi-x-circle-fill'" :style="{ color: z.isActive ? '#4CAF50' : 'var(--red-active)' }"></i></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
