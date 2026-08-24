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
  <div class="dashboard">
    <div class="dashboard-heading">
      <div><span class="eyebrow">Tổng quan tuyến</span><h1>Hôm nay</h1></div>
      <div class="active-count"><strong>{{ activeOrders.length }}</strong><span>đơn đang phụ trách</span></div>
    </div>
    <ShiftStatus role="SHIPPER" />
    <div v-if="store.dashboardLoading || store.listLoading" class="state">Đang tải...</div>
    <div v-if="store.dashboardError || store.listError" class="state error" role="alert"><p>{{ store.dashboardError || store.listError }}</p><button class="btn btn-outline btn-sm" @click="retry">Thử lại</button></div>
    <div class="operations-grid">
    <section class="next-task" aria-labelledby="next-task-title">
      <div class="section-heading"><div><span class="eyebrow">Ưu tiên</span><h2 id="next-task-title">Việc tiếp theo</h2></div><router-link class="shift-shortcut" to="/shipper/shifts">Xem ca làm</router-link></div>
      <router-link v-if="nextOrder" :to="`/shipper/orders/${nextOrder.id}`" class="next-order-card next-order priority-order">
        <div class="order-main"><span class="order-status">{{ nextOrder.status === 'PICKED_UP' ? 'Đang giao' : 'Chờ lấy hàng' }}</span><strong>{{ nextOrder.orderCode }}</strong><span class="order-address">{{ nextOrder.customerAddress }}</span></div>
        <div class="order-action"><strong>{{ formatPrice(nextOrder.total) }}</strong><span>{{ nextOrder.status === 'PICKED_UP' ? 'Tiếp tục giao' : 'Đi lấy hàng' }}</span></div>
      </router-link>
      <div v-else-if="!store.listLoading && !store.listError" class="state empty-task">Chưa có đơn đang hoạt động — <router-link class="state-link" to="/shipper/shifts">Xem ca làm</router-link></div>
    </section>
    <section class="mini-stats" aria-labelledby="stats-title">
      <div class="section-heading"><h2 id="stats-title">Hiệu suất</h2></div>
      <div class="shipper-stats">
        <div class="mini-stat"><strong>{{ assignedCount }}</strong><span>Chờ lấy</span></div>
        <div class="mini-stat"><strong>{{ pickedUpCount }}</strong><span>Đang giao</span></div>
        <div class="mini-stat"><strong>{{ dashboard?.todayDelivered || 0 }}</strong><span>Đã giao hôm nay</span></div>
        <div class="mini-stat"><strong>{{ dashboard?.totalDelivered || 0 }}</strong><span>Tổng đã giao</span></div>
        <div class="mini-stat cod-stat"><strong>{{ formatPrice(dashboard?.todayCodCollected || 0) }}</strong><span>COD hôm nay</span></div>
      </div>
    </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard { display:flex; flex-direction:column; gap:18px; }
.dashboard-heading { display:flex; align-items:flex-end; justify-content:space-between; gap:16px; padding:4px 2px; }
h1,h2 { margin:0; color:var(--text-dark); }
h1 { font-size:26px; letter-spacing:-.04em; }
h2 { font-size:16px; }
.eyebrow { display:block; margin-bottom:3px; color:var(--text-light); font-size:10px; font-weight:750; letter-spacing:.12em; text-transform:uppercase; }
.active-count { text-align:right; }
.active-count strong,.active-count span { display:block; }
.active-count strong { color:var(--primary); font-size:30px; line-height:.9; }
.active-count span { margin-top:5px; color:var(--text-mid); font-size:11px; }
.next-task { display:flex; flex-direction:column; gap:9px; }
.section-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:0 2px; }
.section-heading i { color:var(--primary); }.shift-shortcut{display:inline-flex;align-items:center;min-height:44px;color:var(--primary);font-size:12px;font-weight:750;text-decoration:none}
.operations-grid { display:grid; gap:18px; }.next-order { display:flex; align-items:stretch; justify-content:space-between; gap:16px; min-height:156px; padding:20px; overflow:hidden; background:linear-gradient(145deg,#172033,#29364a); border:1px solid #3b4a61; border-radius:16px; box-shadow:0 14px 30px rgba(23,32,51,.18); color:#fff; text-decoration:none; }
.order-main,.order-action { display:flex; flex-direction:column; }
.order-main { min-width:0; }
.order-status { align-self:flex-start; padding:4px 8px; border-radius:var(--radius-full); background:rgba(255,255,255,.12); color:#fde8d8; font-size:10px; font-weight:700; text-transform:uppercase; }
.order-main strong { margin-top:14px; font-size:21px; letter-spacing:-.02em; }
.order-address { display:-webkit-box; margin-top:6px; overflow:hidden; color:#e6d7cf; font-size:12px; line-height:1.45; -webkit-box-orient:vertical; -webkit-line-clamp:2; }
.order-action { flex-shrink:0; align-items:flex-end; justify-content:space-between; text-align:right; }
.order-action strong { font-size:15px; }
.order-action span { padding:7px 10px; border-radius:var(--radius-sm); background:var(--primary); color:#fff; font-size:11px; font-weight:750; }
.mini-stats { display:flex; flex-direction:column; gap:9px; }
.shipper-stats { display:grid; grid-template-columns:repeat(2,1fr); gap:8px; }
.mini-stat { min-width:0; padding:13px 12px; background:#fff; border:1px solid var(--border-light); border-radius:var(--radius); }
.mini-stat strong,.mini-stat span { display:block; }
.mini-stat strong { overflow:hidden; color:var(--text-dark); font-size:21px; text-overflow:ellipsis; }
.mini-stat span { margin-top:3px; color:var(--text-mid); font-size:11px; }
.cod-stat { grid-column:1 / -1; background:var(--primary-50); border-color:transparent; }
.cod-stat strong { color:var(--primary-dark); }
.state { text-align:center; padding:24px; color:var(--text-mid); }
.empty-task { padding:28px 16px; background:#fff; border:1px dashed var(--border); border-radius:var(--radius); }
.error { color:var(--red-active); }
.state-link { color:var(--primary); font-weight:600; }
@media(max-width:360px) {
  .dashboard-heading { align-items:center; }
  h1 { font-size:23px; }
  .active-count span { max-width:88px; }
  .next-order { flex-direction:column; min-height:0; }
  .order-action { flex-direction:row; align-items:center; text-align:left; }
}
@media(min-width:900px) { .operations-grid { grid-template-columns:minmax(0,1.45fr) minmax(320px,.75fr); align-items:start; }.shipper-stats { grid-template-columns:repeat(2,1fr); }.cod-stat { grid-column:1 / -1; }.next-order { min-height:220px; } }
</style>
