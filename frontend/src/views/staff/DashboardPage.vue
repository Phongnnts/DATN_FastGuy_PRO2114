<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import ShiftStatus from '@/components/common/ShiftStatus.vue';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

const router = useRouter();
const staffStore = useStaffStore();
const loading = ref(true);
const loadError = ref('');
const statusChartRef = ref(null);
let statusChart = null;
let refreshTimer = null;
const data = computed(() => staffStore.dashboard);
const queue = computed(() => [
  { status: 'PENDING', label: 'Chờ xử lý', icon: 'bi-clock-history' },
  { status: 'CONFIRMED', label: 'Đã xác nhận', icon: 'bi-check-circle' },
  { status: 'PREPARING', label: 'Đang chế biến', icon: 'bi-fire' },
]);
const labels = { PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', PICKED_UP: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy' };
const colors = { PENDING:'#f59e0b', CONFIRMED:'#3b82f6', PREPARING:'#8b5cf6', READY:'#10b981', PICKED_UP:'#06b6d4', DELIVERED:'#22c55e', CANCELLED:'#ef4444' };
function goOrders(tab = 'PENDING') { router.push({ path: '/staff/orders', query: { tab } }); }
function destroyChart() { statusChart?.destroy(); statusChart = null; }
function buildChart() { destroyChart(); const statuses = data.value?.ordersByStatus || {}; const keys = Object.keys(statuses).filter(key => statuses[key] > 0); if (!statusChartRef.value || !keys.length) return; statusChart = new Chart(statusChartRef.value, { type:'bar', data:{ labels:keys.map(key => labels[key] || key), datasets:[{ data:keys.map(key => statuses[key]), backgroundColor:keys.map(key => `${colors[key] || '#999'}66`), borderColor:keys.map(key => colors[key] || '#999'), borderWidth:2, borderRadius:5 }] }, options:{ responsive:true, maintainAspectRatio:false, plugins:{ legend:{ display:false } }, scales:{ y:{ beginAtZero:true, ticks:{ stepSize:1 } } } } }); }
async function load() { loading.value = true; loadError.value = ''; try { await staffStore.fetchDashboard(); } catch (e) { loadError.value = e.message || 'Không thể tải tổng quan'; } finally { loading.value = false; } }
onMounted(async () => { await load(); refreshTimer = setInterval(load, 30000); });
watch(() => staffStore.dashboard, buildChart);
onUnmounted(() => { clearInterval(refreshTimer); destroyChart(); });
</script>

<template>
  <div><div class="page-header"><div><h1>Tổng quan</h1><p>Theo dõi hàng đợi và tiến độ vận hành.</p></div><button class="btn btn-sm btn-outline" @click="load"><i class="bi bi-arrow-clockwise"></i> Làm mới</button></div>
    <div v-if="loading" class="staff-state"><span class="spinner"></span> Đang tải tổng quan...</div>
    <div v-else-if="loadError" class="staff-state staff-error"><span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <template v-else-if="data"><ShiftStatus role="STAFF" />
      <section class="kitchen-queue"><div class="queue-heading"><div><span class="queue-kicker">Vận hành bếp</span><h2>Hàng đợi chế biến</h2></div><button class="btn btn-sm btn-outline" @click="goOrders()">Xem đơn hàng</button></div><div class="queue-cards"><button v-for="item in queue" :key="item.status" class="queue-card" @click="goOrders(item.status)"><i :class="['bi',item.icon]"></i><span>{{ item.label }}</span><strong>{{ data.ordersByStatus?.[item.status] || 0 }}</strong></button></div><div class="attention-panel"><i class="bi bi-info-circle"></i><span>Ưu tiên đơn chờ xử lý theo thời gian tạo. Đơn quá 25 phút cần được kiểm tra.</span></div></section>
      <div class="stat-grid"><button class="stat-card" @click="goOrders('PENDING')"><div class="stat-icon wait"><i class="bi bi-clock-history"></i></div><div class="stat-value">{{ data.pendingOrders || 0 }}</div><div class="stat-label">Chờ xử lý</div></button><button class="stat-card" @click="goOrders('CONFIRMED')"><div class="stat-icon confirm"><i class="bi bi-check-circle"></i></div><div class="stat-value">{{ data.confirmedOrders || 0 }}</div><div class="stat-label">Đã xác nhận</div></button><div class="stat-card"><div class="stat-icon today"><i class="bi bi-receipt"></i></div><div class="stat-value">{{ data.ordersToday || 0 }}</div><div class="stat-label">Đơn hôm nay</div></div><button class="stat-card" @click="goOrders('READY')"><div class="stat-icon ready"><i class="bi bi-bag-check"></i></div><div class="stat-value">{{ data.ordersByStatus?.READY || 0 }}</div><div class="stat-label">Sẵn sàng giao</div></button></div>
      <div class="dashboard-grid"><section class="chart-container"><h3>Đơn hàng theo trạng thái</h3><div class="chart"><canvas ref="statusChartRef"></canvas><p v-if="!Object.values(data.ordersByStatus || {}).some(Boolean)" class="muted">Chưa có đơn cần theo dõi.</p></div></section><section class="chart-container"><h3>Trạng thái vận hành</h3><div class="status-summary"><div><span>Đang chế biến</span><strong>{{ data.ordersByStatus?.PREPARING || 0 }}</strong></div><div><span>Chờ shipper</span><strong>{{ data.ordersByStatus?.READY || 0 }}</strong></div><div><span>Đang giao</span><strong>{{ data.ordersByStatus?.PICKED_UP || 0 }}</strong></div><div><span>Đã giao</span><strong>{{ data.ordersByStatus?.DELIVERED || 0 }}</strong></div></div></section></div>
    </template>
  </div>
</template>

<style scoped>
.page-header p { margin:4px 0 0; color:var(--text-mid); font-size:14px; }.staff-state { display:flex; align-items:center; justify-content:center; gap:10px; min-height:240px; color:var(--text-mid); }.staff-error { flex-direction:column; color:var(--red-active); }.kitchen-queue { padding:18px; margin-bottom:16px; border:1px solid var(--border-light); border-radius:var(--radius); background:#fff; }.queue-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:14px; }.queue-heading h2 { margin:2px 0 0; font-size:18px; }.queue-kicker { color:var(--text-mid); font-size:11px; font-weight:800; letter-spacing:.08em; text-transform:uppercase; }.queue-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }.queue-card { display:grid; grid-template-columns:auto 1fr; gap:3px 9px; padding:13px; border:1px solid var(--border-light); border-radius:var(--radius-sm); color:var(--text-dark); background:var(--surface); cursor:pointer; text-align:left; }.queue-card:hover { border-color:var(--role-accent,var(--primary)); }.queue-card i { grid-row:span 2; color:var(--role-accent,var(--primary)); }.queue-card span { color:var(--text-mid); font-size:12px; }.queue-card strong { font-size:21px; }.attention-panel { display:flex; gap:8px; padding:10px 12px; margin-top:12px; border:1px solid #fde68a; border-radius:var(--radius-sm); color:#92400e; background:#fffbeb; font-size:13px; }.stat-card { border:1px solid var(--border-light); cursor:default; }.stat-card:not(:nth-child(3)) { cursor:pointer; }.stat-card:not(:nth-child(3)):hover { border-color:var(--role-accent,var(--primary)); }.stat-icon.wait { background:linear-gradient(135deg,#f59e0b,#fbbf24); }.stat-icon.confirm { background:linear-gradient(135deg,#3b82f6,#60a5fa); }.stat-icon.today { background:linear-gradient(135deg,#8b5cf6,#a78bfa); }.stat-icon.ready { background:linear-gradient(135deg,#10b981,#34d399); }.dashboard-grid { display:grid; grid-template-columns:1.2fr .8fr; gap:16px; }.chart-container { padding:18px; border:1px solid var(--border-light); border-radius:var(--radius); background:#fff; }.chart-container h3 { margin-bottom:14px; font-size:16px; }.chart { position:relative; height:260px; }.muted { color:var(--text-mid); }.status-summary { display:grid; gap:10px; }.status-summary div { display:flex; justify-content:space-between; padding:12px; border-radius:var(--radius-sm); background:var(--surface); }.status-summary span { color:var(--text-mid); font-size:14px; }.status-summary strong { color:var(--text-dark); }@media(max-width:768px){.queue-cards,.dashboard-grid{grid-template-columns:1fr;}.queue-heading{align-items:flex-start;flex-direction:column;}.chart{height:220px;}}
</style>
