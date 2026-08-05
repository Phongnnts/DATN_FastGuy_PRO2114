<script setup>
import { computed, onMounted, onUnmounted } from 'vue';
import { useShipperStore } from '@/stores/shipper';
import ShiftStatus from '@/components/common/ShiftStatus.vue';
import { formatPrice } from '@/utils/format';

const store = useShipperStore();
const dashboard = computed(() => store.dashboard);
const activeOrders = computed(() => store.activeOrders);
let inFlight = false;
let stopped = false;
let timer;
const assignedCount = computed(() => activeOrders.value.filter(order => order.status === 'ASSIGNED').length);
const pickedUpCount = computed(() => activeOrders.value.filter(order => order.status === 'PICKED_UP').length);
const nextOrder = computed(() => [...activeOrders.value].sort((a, b) => (a.status === 'PICKED_UP' ? -1 : 1) - (b.status === 'PICKED_UP' ? -1 : 1) || String(a.assignedAt || a.createdAt).localeCompare(String(b.assignedAt || b.createdAt)))[0]);

async function retry(silent = false) {
  if (inFlight || stopped) return;
  inFlight = true;
  await Promise.allSettled([store.fetchDashboard(silent), store.fetchActiveOrders(silent)]);
  if (!stopped) inFlight = false;
}

onMounted(() => {
  retry();
  timer = setInterval(() => retry(true), 30_000);
});
onUnmounted(() => { stopped = true; clearInterval(timer); store.invalidateListRequests(); });
</script>

<template>
  <div>
    <div class="shipper-hero"><span class="hero-kicker">Delivery cockpit</span><span class="hero-number">{{ activeOrders.length }}</span><span>Đơn đang phụ trách</span></div>
    <ShiftStatus role="SHIPPER" />
    <div v-if="store.dashboardLoading || store.listLoading" class="state">Đang tải...</div>
    <div v-if="store.dashboardError || store.listError" class="state error" role="alert"><p>{{ store.dashboardError || store.listError }}</p><button class="btn btn-outline btn-sm" @click="retry">Thử lại</button></div>
    <div class="shipper-stats">
      <div class="mini-stat"><strong>{{ assignedCount }}</strong><span>Chờ lấy</span></div>
      <div class="mini-stat"><strong>{{ pickedUpCount }}</strong><span>Đang giao</span></div>
      <div class="mini-stat"><strong>{{ dashboard?.todayDelivered || 0 }}</strong><span>Đã giao hôm nay</span></div>
      <div class="mini-stat"><strong>{{ dashboard?.totalDelivered || 0 }}</strong><span>Tổng đã giao</span></div>
      <div class="mini-stat"><strong>{{ formatPrice(dashboard?.todayCodCollected || 0) }}</strong><span>COD hôm nay</span></div>
    </div>
    <router-link v-if="nextOrder" :to="`/shipper/orders/${nextOrder.id}`" class="next-order">
      <div><small>Việc tiếp theo</small><strong>{{ nextOrder.orderCode }}</strong><span>{{ nextOrder.customerAddress }}</span></div>
      <div><strong>{{ formatPrice(nextOrder.total) }}</strong><span>{{ nextOrder.status === 'PICKED_UP' ? 'Tiếp tục giao' : 'Đi lấy hàng' }}</span></div>
    </router-link>
    <div v-else-if="!store.listLoading && !store.listError" class="state">
      Chưa có đơn đang hoạt động — <router-link class="state-link" to="/shipper/shifts">Xem ca làm</router-link>
    </div>
  </div>
</template>

<style scoped>
.shipper-hero { background:linear-gradient(135deg,#1f130d,var(--primary-dark)); color:#fff; border-radius:var(--radius-lg); padding:28px; text-align:center; margin-bottom:12px; }
.hero-kicker,.hero-number { display:block; }.hero-kicker { font-size:11px; text-transform:uppercase; opacity:.75; }.hero-number { font-size:44px; font-weight:800; }
.shipper-stats { display:grid; grid-template-columns:repeat(2,1fr); gap:8px; margin:12px 0; }.mini-stat { background:#fff; border:1px solid var(--border-light); border-radius:var(--radius); padding:12px 6px; text-align:center; }.mini-stat strong,.mini-stat span { display:block; }.mini-stat strong { color:var(--primary); font-size:22px; }.mini-stat span { color:var(--text-mid); font-size:11px; }
.next-order { display:flex; justify-content:space-between; gap:12px; padding:16px; background:#fff; border:1px solid var(--primary); border-radius:var(--radius); color:inherit; text-decoration:none; }.next-order div,.next-order strong,.next-order span,.next-order small { display:block; }.next-order div:last-child { text-align:right; }.next-order span,.next-order small { color:var(--text-mid); font-size:12px; margin-top:4px; }
.state { text-align:center; padding:24px; color:var(--text-mid); }.error { color:var(--red-active); }.state-link { color:var(--primary); font-weight:600; }
</style>
