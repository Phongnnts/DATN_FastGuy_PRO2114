<script setup>
import { useShipperStore } from '@/stores/shipper'
import { formatPrice, formatDate } from '@/utils/format'

const shipperStore = useShipperStore()
const history = shipperStore.deliveredDeliveries
</script>

<template>
  <div>
    <h3 style="font-weight:700;margin-bottom:16px">Lịch sử giao hàng</h3>
    <div v-if="history.length === 0" style="text-align:center;padding:40px;color:var(--text-mid)">Chưa có lịch sử</div>
    <div v-for="d in history" :key="d.id" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between;margin-bottom:6px">
        <strong>{{ d.orderCode }}</strong>
        <span class="badge badge-success">Đã giao</span>
      </div>
      <div style="font-size:13px;color:var(--text-mid)">{{ d.customerName }} - {{ d.customerPhone }}</div>
      <div style="font-size:13px;color:var(--text-mid);margin:4px 0">{{ d.address }}</div>
      <div style="display:flex;justify-content:space-between;font-size:13px">
        <span style="color:var(--text-light)">{{ formatDate(d.deliveredAt) }}</span>
        <span style="font-weight:700">{{ formatPrice(d.total) }}</span>
      </div>
      <div v-if="d.collectedCOD > 0" style="font-size:12px;color:var(--primary);margin-top:4px">Đã thu COD: {{ formatPrice(d.collectedCOD) }}</div>
    </div>
  </div>
</template>
