<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Chart, registerables } from 'chart.js';
import { useStaffStore } from '@/stores/staff';
import { formatPrice, formatTime } from '@/utils/format';
import { waitingDuration } from '@/utils/staffKitchen';

Chart.register(...registerables);

const router = useRouter();
const staffStore = useStaffStore();
const loading = ref(true);
const refreshing = ref(false);
const loadError = ref('');
const lastUpdated = ref(null);
const statusChartRef = ref(null);
let statusChart = null;
let refreshTimer = null;
let inFlight = false;
const data = computed(() => staffStore.dashboard || {});
const labels = { PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', ASSIGNED: 'Đã gán shipper', PICKED_UP: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy' };
const colors = { PENDING: '#f59e0b', CONFIRMED: '#3b82f6', PREPARING: '#8b5cf6', READY: '#10b981', ASSIGNED: '#2563eb', PICKED_UP: '#06b6d4', DELIVERED: '#22c55e', CANCELLED: '#ef4444' };
const activeTotal = computed(() => ['PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP'].reduce((sum, status) => sum + Number(data.value.ordersByStatus?.[status] || 0), 0));
const alertCount = computed(() => Number(data.value.overdueOrders || 0) + Number(data.value.awaitingShipperOrders || 0));

function goOrders(tab) {
  router.push({ path: '/staff/orders', query: tab ? { tab } : {} });
}
function openOrder(orderId) {
  router.push(`/staff/orders/${orderId}`);
}
function destroyChart() {
  statusChart?.destroy();
  statusChart = null;
}
async function buildChart() {
  await nextTick();
  destroyChart();
  const statuses = data.value.ordersByStatus || {};
  const keys = Object.keys(statuses).filter((key) => Number(statuses[key]) > 0);
  if (!statusChartRef.value || !keys.length) return;
  statusChart = new Chart(statusChartRef.value, {
    type: 'doughnut',
    data: {
      labels: keys.map((key) => labels[key] || key),
      datasets: [{ data: keys.map((key) => statuses[key]), backgroundColor: keys.map((key) => colors[key] || '#94a3b8'), borderColor: '#fff', borderWidth: 4 }],
    },
    options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { display: false } } },
  });
}
async function load({ silent = false } = {}) {
  if (inFlight) return;
  inFlight = true;
  if (silent) refreshing.value = true;
  else loading.value = true;
  loadError.value = '';
  try {
    await staffStore.fetchDashboard();
    lastUpdated.value = new Date();
  } catch (error) {
    loadError.value = error.message || 'Không thể tải tổng quan';
  } finally {
    loading.value = false;
    refreshing.value = false;
    inFlight = false;
  }
}
onMounted(async () => {
  await load();
  refreshTimer = setInterval(() => load({ silent: true }), 30000);
});
watch(() => staffStore.dashboard, buildChart);
onUnmounted(() => {
  clearInterval(refreshTimer);
  destroyChart();
});
</script>

<template>
  <main class="dashboard">
    <header class="dashboard-hero">
      <div>
        <span class="eyebrow">FASTGUY OPERATIONS</span>
        <h1>Tổng quan ca làm việc</h1>
        <p v-if="data.currentShift">Đã check-in lúc {{ formatTime(data.currentShift.checkInAt) }} · Ca {{ data.currentShift.startTime }}–{{ data.currentShift.endTime }}</p>
        <p v-else>Đang đồng bộ trạng thái vận hành.</p>
      </div>
      <div class="hero-actions">
        <span class="live-pill"><i></i> Đang vận hành</span>
        <router-link to="/staff/shifts" class="btn btn-sm btn-outline">Ca làm</router-link>
        <button class="btn btn-sm btn-outline" :disabled="refreshing" @click="load({ silent: true })"><i class="bi bi-arrow-clockwise" :class="{ spin: refreshing }"></i> Làm mới</button>
      </div>
      <div class="hero-orbit" aria-hidden="true"></div>
    </header>

    <div v-if="loading" class="staff-state" aria-live="polite"><span class="spinner"></span> Đang tải tổng quan...</div>
    <div v-else-if="loadError && !staffStore.dashboard" class="staff-state staff-error" role="alert"><span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load()">Thử lại</button></div>

    <template v-else>
      <p v-if="loadError" class="stale-alert" role="alert">{{ loadError }} Dữ liệu gần nhất vẫn được giữ lại.</p>

      <section class="metric-grid" aria-label="Chỉ số vận hành hiện tại">
        <button class="metric-card warning" @click="goOrders('PENDING')"><span><i class="bi bi-clock-history"></i> Chờ xử lý</span><strong>{{ data.pendingOrders || 0 }}</strong><small>Cần xác nhận sớm</small></button>
        <button class="metric-card violet" @click="goOrders('PREPARING')"><span><i class="bi bi-fire"></i> Đang chế biến</span><strong>{{ data.ordersByStatus?.PREPARING || 0 }}</strong><small>Trong hàng đợi bếp</small></button>
        <button class="metric-card success" @click="goOrders('READY')"><span><i class="bi bi-bag-check"></i> Sẵn sàng giao</span><strong>{{ data.awaitingShipperOrders || 0 }}</strong><small>Đang chờ shipper</small></button>
        <article class="metric-card danger"><span><i class="bi bi-exclamation-triangle"></i> Đơn quá 25 phút</span><strong>{{ data.overdueOrders || 0 }}</strong><small>Cần kiểm tra ngay</small></article>
      </section>

      <section class="shift-report">
        <div class="section-heading"><div><span>Báo cáo theo ca</span><h2>Hiệu quả ca đang làm</h2></div><small v-if="lastUpdated">Cập nhật {{ formatTime(lastUpdated) }}</small></div>
        <div class="report-grid">
          <article><i class="bi bi-check2-circle"></i><div><span>Đơn hoàn thành trong ca</span><strong>{{ data.shiftCompletedOrders || 0 }}</strong></div></article>
          <article class="failed"><i class="bi bi-x-circle"></i><div><span>Đơn thất bại trong ca</span><strong>{{ data.shiftFailedOrders || 0 }}</strong></div></article>
          <article class="revenue"><i class="bi bi-cash-stack"></i><div><span>Doanh thu thuần trong ca</span><strong>{{ formatPrice(Number(data.shiftNetRevenue || 0)) }}</strong><small>Đã trừ hoàn tiền</small></div></article>
        </div>
      </section>

      <section v-if="alertCount" class="operations-alert" role="status"><i class="bi bi-exclamation-circle"></i><div><strong>{{ alertCount }} tín hiệu cần chú ý</strong><span>{{ data.overdueOrders || 0 }} đơn quá thời gian · {{ data.awaitingShipperOrders || 0 }} đơn chờ shipper</span></div><button class="btn btn-sm btn-outline" @click="goOrders('PENDING')">Kiểm tra</button></section>

      <div class="content-grid">
        <section class="priority-panel">
          <div class="section-heading"><div><span>Hàng đợi realtime</span><h2>Đơn cần ưu tiên</h2></div><button class="text-link" @click="goOrders()">Xem tất cả <i class="bi bi-arrow-right"></i></button></div>
          <div v-if="data.priorityOrders?.length" class="priority-list">
            <button v-for="order in data.priorityOrders" :key="order.orderId" @click="openOrder(order.orderId)">
              <div class="order-main"><strong>{{ order.orderCode }}</strong><span>{{ order.customerName || 'Khách hàng' }}</span></div>
              <span class="status-chip" :data-status="order.status">{{ labels[order.status] || order.status }}</span>
              <div class="order-meta"><strong>{{ waitingDuration(order.createdAt) }}</strong><span>{{ formatPrice(Number(order.finalAmount || 0)) }}</span></div>
              <i class="bi bi-chevron-right"></i>
            </button>
          </div>
          <div v-else class="empty-state"><i class="bi bi-check2-circle"></i><strong>Hàng đợi đang trống</strong><span>Không có đơn cần ưu tiên.</span></div>
        </section>

        <section class="status-panel">
          <div class="section-heading"><div><span>Luồng hiện tại</span><h2>Phân bố trạng thái</h2></div></div>
          <div class="chart-wrap"><canvas ref="statusChartRef"></canvas><div class="chart-center"><strong>{{ activeTotal }}</strong><span>đơn hoạt động</span></div></div>
          <div class="legend"><div v-for="status in ['PENDING','CONFIRMED','PREPARING','READY','PICKED_UP']" :key="status"><i :style="{ background: colors[status] }"></i><span>{{ labels[status] }}</span><strong>{{ data.ordersByStatus?.[status] || 0 }}</strong></div></div>
        </section>
      </div>
    </template>
  </main>
</template>

<style scoped>
.dashboard{color:var(--text-dark)}.dashboard-hero{position:relative;display:flex;align-items:flex-end;justify-content:space-between;gap:24px;min-height:190px;margin-bottom:18px;padding:30px 34px;overflow:hidden;border-radius:26px;color:#fff;background:linear-gradient(128deg,#064e49 0%,var(--role-staff) 64%,#14b8a6 150%);box-shadow:0 20px 48px rgba(15,118,110,.22)}.dashboard-hero>div:not(.hero-orbit){position:relative;z-index:1}.eyebrow,.section-heading span{font-size:10px;font-weight:800;letter-spacing:.13em;text-transform:uppercase}.eyebrow{color:#99f6e4}.dashboard-hero h1{margin:10px 0 8px;font-size:clamp(28px,4vw,42px);letter-spacing:-.04em}.dashboard-hero p{margin:0;color:rgba(255,255,255,.72);font-size:13px}.hero-actions{display:flex;align-items:center;gap:8px}.hero-actions .btn{border-color:rgba(255,255,255,.24);color:#fff;background:rgba(255,255,255,.1)}.live-pill{display:flex;align-items:center;gap:7px;margin-right:4px;font-size:12px;font-weight:700}.live-pill i{width:8px;height:8px;border-radius:50%;background:#5eead4;box-shadow:0 0 0 5px rgba(94,234,212,.15)}.hero-orbit{position:absolute;right:-90px;top:-220px;width:430px;height:430px;border:1px solid rgba(255,255,255,.1);border-radius:50%}.hero-orbit::after{position:absolute;inset:70px;border:1px solid rgba(255,255,255,.1);border-radius:50%;content:""}.staff-state{display:flex;align-items:center;justify-content:center;gap:10px;min-height:260px;color:var(--text-mid)}.staff-error{flex-direction:column;color:var(--red-active)}.stale-alert,.operations-alert{display:flex;align-items:center;gap:10px;padding:12px 14px;margin-bottom:14px;border:1px solid #fde68a;border-radius:14px;color:#92400e;background:#fffbeb}.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:14px}.metric-card{min-width:0;padding:18px;border:1px solid var(--border-light);border-radius:18px;background:#fff;text-align:left;box-shadow:0 8px 24px rgba(15,23,42,.04)}button.metric-card{cursor:pointer}.metric-card>span{display:flex;align-items:center;gap:8px;color:var(--text-mid);font-size:12px;font-weight:700}.metric-card>span i{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;color:#b45309;background:#fef3c7}.metric-card.violet>span i{color:#6d28d9;background:#ede9fe}.metric-card.success>span i{color:#047857;background:#d1fae5}.metric-card.danger>span i{color:#b91c1c;background:#fee2e2}.metric-card>strong{display:block;margin:14px 0 3px;font-size:30px;line-height:1}.metric-card small{color:var(--text-mid)}button.metric-card:hover{border-color:var(--role-staff);transform:translateY(-2px)}.shift-report,.priority-panel,.status-panel{padding:20px;border:1px solid var(--border-light);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(15,23,42,.04)}.shift-report{margin-bottom:14px}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px}.section-heading span{color:var(--role-staff)}.section-heading h2{margin:4px 0 0;font-size:18px}.section-heading small{color:var(--text-mid)}.report-grid{display:grid;grid-template-columns:1fr 1fr 1.35fr;gap:12px}.report-grid article{display:flex;align-items:center;gap:13px;padding:16px;border-radius:14px;background:#f0fdf4}.report-grid article>i{font-size:24px;color:#16a34a}.report-grid article.failed{background:#fef2f2}.report-grid article.failed>i{color:#dc2626}.report-grid article.revenue{color:#fff;background:linear-gradient(135deg,#0f766e,#14b8a6)}.report-grid article.revenue>i{color:#ccfbf1}.report-grid span{display:block;margin-bottom:4px;color:var(--text-mid);font-size:12px}.report-grid .revenue span,.report-grid .revenue small{color:rgba(255,255,255,.72)}.report-grid strong{font-size:23px}.operations-alert div{display:flex;flex:1;flex-direction:column;gap:2px}.operations-alert span{font-size:12px}.content-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(300px,.75fr);gap:14px}.text-link{border:0;color:var(--role-staff);background:none;cursor:pointer;font-size:12px;font-weight:700}.priority-list{display:grid}.priority-list>button{display:grid;grid-template-columns:minmax(0,1.2fr) auto minmax(120px,.6fr) auto;align-items:center;gap:14px;padding:14px 4px;border:0;border-bottom:1px solid var(--border-light);background:#fff;cursor:pointer;text-align:left}.priority-list>button:last-child{border-bottom:0}.priority-list>button:hover{background:#f8fafc}.order-main,.order-meta{display:flex;flex-direction:column;gap:3px}.order-main span,.order-meta span{overflow:hidden;color:var(--text-mid);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.order-meta{text-align:right}.status-chip{padding:5px 8px;border-radius:999px;color:#6d28d9;background:#ede9fe;font-size:11px;font-weight:700}.status-chip[data-status="PENDING"]{color:#92400e;background:#fef3c7}.status-chip[data-status="READY"]{color:#047857;background:#d1fae5}.empty-state{display:flex;align-items:center;flex-direction:column;padding:42px;color:var(--text-mid);text-align:center}.empty-state i{margin-bottom:8px;color:var(--role-staff);font-size:30px}.empty-state strong{color:var(--text-dark)}.chart-wrap{position:relative;height:230px}.chart-center{position:absolute;inset:0;display:grid;place-content:center;pointer-events:none;text-align:center}.chart-center strong{font-size:28px}.chart-center span{color:var(--text-mid);font-size:11px}.legend{display:grid;gap:8px;margin-top:14px}.legend div{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:8px;font-size:12px}.legend i{width:8px;height:8px;border-radius:50%}.legend span{color:var(--text-mid)}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:1050px){.metric-grid{grid-template-columns:repeat(2,1fr)}.content-grid{grid-template-columns:1fr}.report-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:768px){.dashboard-hero{align-items:flex-start;flex-direction:column;padding:24px}.hero-actions{flex-wrap:wrap}.live-pill{width:100%;margin-bottom:4px}.metric-grid,.report-grid{grid-template-columns:1fr}.priority-list>button{grid-template-columns:1fr auto}.priority-list .status-chip{grid-column:2}.order-meta{grid-row:2;text-align:left}.priority-list>button>i{grid-column:2;grid-row:2}.operations-alert{align-items:flex-start;flex-wrap:wrap}.operations-alert .btn{margin-left:26px}}@media(prefers-reduced-motion:reduce){.metric-card{transition:none}.spin{animation:none}}
</style>
