<script setup>
import { onMounted } from 'vue'
import { useShipperStore } from '@/stores/shipper'
import { formatPrice } from '@/utils/format'

const shipperStore = useShipperStore()

onMounted(() => { shipperStore.fetchDeliveries() })
</script>

<template>
  <div>
    <div class="stat-grid" style="grid-template-columns:repeat(2,1fr)">
      <div class="stat-card" style="text-align:center;border:2px solid #E8F5E9">
        <div class="stat-icon" style="background:#E8F5E9;color:#4CAF50;margin:0 auto"><i class="bi bi-box-seam"></i></div>
        <div class="stat-value">{{ shipperStore.readyDeliveries.length }}</div>
        <div class="stat-label">Sẵn sàng</div>
      </div>
      <div class="stat-card" style="text-align:center;border:2px solid #FFF3CD">
        <div class="stat-icon" style="background:#FFF3CD;color:#f5a623;margin:0 auto"><i class="bi bi-truck"></i></div>
        <div class="stat-value">{{ shipperStore.deliveringDeliveries.length }}</div>
        <div class="stat-label">Đang giao</div>
      </div>
    </div>
    <h4 style="font-weight:700;margin:16px 0 12px">Đơn sẵn sàng</h4>
    <div v-if="shipperStore.readyDeliveries.length === 0" style="text-align:center;padding:24px;color:var(--text-mid);font-size:14px">Không có đơn nào</div>
    <div v-for="d in shipperStore.readyDeliveries" :key="d.id" class="card" style="margin-bottom:12px;cursor:pointer" @click="$router.push('/shipper/deliveries/' + d.id)">
      <div style="display:flex;justify-content:space-between;margin-bottom:8px">
        <strong>{{ d.orderCode }}</strong>
        <span class="badge badge-warning">Sẵn sàng</span>
      </div>
      <div style="font-size:13px;color:var(--text-mid);margin-bottom:4px"><i class="bi bi-geo-alt"></i> {{ d.address }}</div>
      <div style="font-size:13px;color:var(--text-mid)"><i class="bi bi-person"></i> {{ d.customerName }}</div>
      <div style="margin-top:8px;display:flex;justify-content:space-between;align-items:center">
        <span style="font-size:12px;color:var(--text-light)"><i class="bi bi-clock"></i> {{ d.estimatedTime }}</span>
        <span style="font-weight:700;color:var(--primary)">{{ formatPrice(d.total) }}</span>
      </div>
    </div>
  </div>
</template>
