<script setup>
import { onMounted } from 'vue'
import { useShipperStore } from '@/stores/shipper'
import { formatPrice } from '@/utils/format'

const shipperStore = useShipperStore()

onMounted(() => { shipperStore.fetchDeliveries() })
</script>

<template>
  <div>
    <div class="delivery-tabs">
      <div class="delivery-tab active" @click="$router.push('/shipper')" style="cursor:pointer">
        <i class="bi bi-box-seam"></i>
        <span>Chờ nhận ({{ shipperStore.readyDeliveries.length }})</span>
      </div>
      <div class="delivery-tab" @click="$router.push('/shipper')" style="cursor:pointer">
        <i class="bi bi-truck"></i>
        <span>Đang giao ({{ shipperStore.deliveringDeliveries.length }})</span>
      </div>
    </div>
    <div v-for="d in shipperStore.allDeliveries" :key="d.id" class="card" style="margin-bottom:12px;cursor:pointer" @click="$router.push('/shipper/deliveries/' + d.id)">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <strong>{{ d.orderCode }}</strong>
        <span class="badge" :class="d.status === 'READY' ? 'badge-warning' : d.status === 'DELIVERING' ? 'badge-info' : 'badge-success'">{{ d.status === 'READY' ? 'Sẵn sàng' : d.status === 'DELIVERING' ? 'Đang giao' : 'Đã giao' }}</span>
      </div>
      <div style="font-size:13px;color:var(--text-mid);margin-bottom:4px"><i class="bi bi-person"></i> {{ d.customerName }}</div>
      <div style="font-size:13px;color:var(--text-mid);margin-bottom:4px;display:-webkit-box;-webkit-line-clamp:1;-webkit-box-orient:vertical;overflow:hidden"><i class="bi bi-geo-alt"></i> {{ d.address }}</div>
      <div style="display:flex;justify-content:space-between;margin-top:8px">
        <span style="font-size:13px;color:var(--primary);font-weight:700">{{ formatPrice(d.total) }}</span>
        <span style="font-size:12px;color:var(--text-light)">{{ d.paymentMethod }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.delivery-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.delivery-tab { flex: 1; background: var(--bg); padding: 12px; border-radius: var(--radius-sm); text-align: center; font-size: 13px; font-weight: 600; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.delivery-tab i { font-size: 20px; }
.delivery-tab.active { background: var(--primary-light); color: var(--primary); }
</style>
