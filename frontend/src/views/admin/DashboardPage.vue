<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { Chart, registerables } from 'chart.js';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { dashboardViewState } from '@/utils/adminDashboardViewState';

Chart.register(...registerables);

const adminStore = useAdminStore();
const activeOrderChartRef = ref(null);
const loadState = ref('loading');
const loadError = ref(null);
let activeOrderChart = null;
let requestGeneration = 0;
let stopped = false;

const ACTIVE_ORDER_STATUSES = [
  { key: 'PENDING', label: 'Chờ xác nhận', color: '--admin-warning' },
  { key: 'CONFIRMED', label: 'Đã xác nhận', color: '--admin-info' },
  { key: 'PREPARING', label: 'Đang chế biến', color: '--admin-brand' },
  { key: 'READY', label: 'Sẵn sàng giao', color: '--admin-success' },
  { key: 'ASSIGNED', label: 'Đã gán shipper', color: '--admin-info' },
  { key: 'PICKED_UP', label: 'Đang giao', color: '--admin-brand' },
  { key: 'DELIVERY_FAILED', label: 'Giao thất bại', color: '--admin-danger' },
];
const ATTENTION = {
  OVERDUE_PENDING_ORDERS: { label: 'Đơn chờ xác nhận quá lâu', to: { path: '/admin/orders', query: { status: 'ATTENTION' } } },
  DELIVERY_FAILED_ORDERS: { label: 'Đơn giao thất bại', to: { path: '/admin/orders', query: { status: 'ATTENTION' } } },
  PENDING_REFUNDS: { label: 'Yêu cầu hoàn tiền đang chờ', to: { path: '/admin/refunds', query: { status: 'PENDING' } } },
  STAFF_COVERAGE_GAPS: { label: 'Ca làm cần bổ sung nhân viên', to: { path: '/admin/shifts', query: { tab: 'monitoring' } } },
  LOW_STOCK_ITEMS: { label: 'Mặt hàng dưới mức an toàn', to: { path: '/admin/inventory', query: { filter: 'LOW' } } },
  PENDING_COD_SETTLEMENTS: { label: 'Bàn giao COD đang chờ', to: { path: '/admin/cod-settlements', query: { status: 'SUBMITTED' } } },
};

const data = computed(() => adminStore.dashboard);
const viewState = computed(() => dashboardViewState(data.value, loadState.value, loadError.value, data.value?.sectionAvailability));
const showContent = computed(() => ['ready', 'refreshing', 'partial'].includes(viewState.value));
const errorMessage = computed(() => loadError.value?.message || adminStore.error || 'Không thể tải tổng quan');
const today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }).format(new Date());
const attentionItems = computed(() => (data.value?.attentionItems || []).map(item => ({ ...item, ...ATTENTION[item.type] })).filter(item => item.label));
const activeOrderSeries = computed(() => ACTIVE_ORDER_STATUSES.map(status => ({ ...status, count: Number(data.value?.activeOrdersByStatus?.[status.key]) })).filter(item => Number.isFinite(item.count) && item.count > 0));
const chartAriaLabel = computed(() => `Luồng đơn đang hoạt động. ${activeOrderSeries.value.map(item => `${item.label}: ${item.count}`).join('; ')}`);
const banner = computed(() => {
  if (loadError.value && showContent.value) return { role: 'alert', message: errorMessage.value };
  if (viewState.value === 'refreshing') return { role: 'status', message: 'Đang cập nhật tổng quan...' };
  if (viewState.value === 'partial') return { role: 'status', message: 'Một số dữ liệu tạm thời chưa khả dụng.' };
  return null;
});

function available(section) {
  return data.value?.sectionAvailability?.[section] === 'AVAILABLE';
}

function getCSSVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function buildCharts() {
  destroyCharts();
  if (!available('orders') || !activeOrderChartRef.value || !activeOrderSeries.value.length) return;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const text = getCSSVar('--admin-muted');
  const border = getCSSVar('--admin-border');
  activeOrderChart = new Chart(activeOrderChartRef.value, {
    type: 'bar',
    data: {
      labels: activeOrderSeries.value.map(item => item.label),
      datasets: [{
        label: 'Số đơn',
        data: activeOrderSeries.value.map(item => item.count),
        backgroundColor: activeOrderSeries.value.map(item => getCSSVar(item.color)),
        borderWidth: 0,
        borderRadius: 3,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: reducedMotion ? false : { duration: 240 },
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false }, ticks: { color: text, font: { family: "'Be Vietnam Pro', sans-serif", size: 11 } } },
        y: { beginAtZero: true, grid: { color: border }, ticks: { color: text, precision: 0, font: { family: "'Be Vietnam Pro', sans-serif", size: 11 } } },
      },
    },
  });
}

function destroyCharts() {
  activeOrderChart?.destroy();
  activeOrderChart = null;
}

async function loadDashboard() {
  const request = { generation: ++requestGeneration };
  loadState.value = 'loading';
  loadError.value = null;
  if (!adminStore.dashboard) destroyCharts();
  try {
    await adminStore.fetchDashboard();
    if (stopped || request.generation !== requestGeneration) return;
    loadState.value = 'ready';
    await nextTick();
    if (stopped || request.generation !== requestGeneration) return;
    buildCharts();
  } catch (error) {
    if (stopped || request.generation !== requestGeneration) return;
    loadError.value = error;
    loadState.value = 'error';
  }
}

onMounted(loadDashboard);
onUnmounted(() => {
  stopped = true;
  requestGeneration += 1;
  destroyCharts();
});
</script>

<template>
  <div class="dashboard">
    <section v-if="viewState === 'loading'" class="dashboard-skeleton" role="status" aria-label="Đang tải tổng quan">
      <span class="sr-only">Đang tải tổng quan...</span>
      <div class="skeleton-heading"><i></i><i></i></div>
      <div class="skeleton-attention"></div>
      <div class="skeleton-metrics"><i v-for="index in 6" :key="index"></i></div>
      <div class="skeleton-flow"></div>
    </section>
    <section v-else-if="viewState === 'forbidden'" class="dashboard-state permission-state" role="alert">
      <i class="bi bi-shield-lock" aria-hidden="true"></i>
      <strong>Bạn không có quyền xem tổng quan vận hành.</strong>
      <span>Liên hệ quản trị viên nếu bạn cần quyền truy cập.</span>
    </section>
    <section v-else-if="viewState === 'error'" class="dashboard-state error-state" role="alert">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <strong>{{ errorMessage }}</strong>
      <button class="btn btn-outline" type="button" @click="loadDashboard">Thử lại</button>
    </section>
    <template v-else>
      <header class="dashboard-heading" data-dashboard-section="header">
        <div><h1>Hoạt động hôm nay</h1><p>{{ today }}</p></div>
        <button class="btn btn-outline" type="button" :disabled="loadState === 'loading'" @click="loadDashboard"><i class="bi bi-arrow-clockwise" aria-hidden="true"></i>Làm mới</button>
        <div v-if="banner" class="dashboard-banner" :class="{ 'error-state': banner.role === 'alert' }" :role="banner.role" :data-store-error="adminStore.error || undefined">{{ banner.message }}</div>
      </header>

      <section class="attention-panel" data-dashboard-section="attention" aria-labelledby="attention-title">
        <header><h2 id="attention-title">Cần xử lý ngay</h2><strong>{{ attentionItems.length }}</strong></header>
        <div v-if="attentionItems.length" class="attention-list">
          <router-link v-for="item in attentionItems" :key="item.type" :to="item.to" :class="item.severity.toLowerCase()">
            <span class="status-rail" aria-hidden="true"></span>
            <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
            <span class="attention-copy"><strong>{{ item.label }}</strong><small>{{ item.severity === 'CRITICAL' ? 'Khẩn cấp' : 'Cảnh báo' }}</small></span>
            <strong class="attention-count">{{ item.count }}</strong>
            <i class="bi bi-chevron-right" aria-hidden="true"></i>
          </router-link>
        </div>
        <p v-else class="attention-empty"><i class="bi bi-check2-circle" aria-hidden="true"></i>Không có việc cần xử lý ngay.</p>
      </section>

      <section class="operating-metrics" data-dashboard-section="metrics" aria-label="Chỉ số vận hành">
        <article role="group" aria-label="Doanh thu thuần hôm nay"><span>Doanh thu thuần hôm nay</span><strong>{{ available('financial') ? formatPrice(data.netCashRevenueToday) : 'Không khả dụng' }}</strong></article>
        <article role="group" aria-label="Đơn đang hoạt động"><span>Đơn đang hoạt động</span><strong>{{ available('orders') ? Number(data.activeOrderCount).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article role="group" aria-label="Hoàn tiền chờ xử lý"><span>Hoàn tiền chờ xử lý</span><strong>{{ available('refunds') ? Number(data.pendingRefundCount).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article role="group" aria-label="COD chờ xác nhận"><span>COD chờ xác nhận</span><strong>{{ available('cod') ? Number(data.pendingCodCount).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article role="group" aria-label="Mặt hàng sắp hết"><span>Mặt hàng sắp hết</span><strong>{{ available('inventory') ? Number(data.lowStockItemCount).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article role="group" aria-label="Ca thiếu nhân sự"><span>Ca thiếu nhân sự</span><strong>{{ available('staffing') ? Number(data.staffingGapCount).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
      </section>

      <section class="active-flow" data-dashboard-section="flow" aria-labelledby="active-flow-title">
        <header><div><h2 id="active-flow-title">Luồng đơn đang hoạt động</h2><p>Chỉ gồm trạng thái đang cần vận hành.</p></div><strong v-if="available('orders')">{{ Number(data.activeOrderCount).toLocaleString('vi-VN') }} đơn</strong></header>
        <div v-if="!available('orders')" class="flow-state">Dữ liệu luồng đơn không khả dụng.</div>
        <div v-else-if="activeOrderSeries.length" class="chart-canvas"><canvas ref="activeOrderChartRef" role="img" :aria-label="chartAriaLabel" aria-describedby="active-flow-data"></canvas></div>
        <div v-else class="flow-state"><i class="bi bi-inbox" aria-hidden="true"></i>Không có đơn đang hoạt động.</div>
      </section>

      <section v-if="available('orders')" id="active-flow-data" class="flow-data" data-dashboard-section="flow-data" aria-label="Dữ liệu thay thế biểu đồ">
        <details open>
          <summary>Dữ liệu chi tiết</summary>
          <table aria-label="Dữ liệu luồng đơn đang hoạt động">
            <thead><tr><th scope="col">Trạng thái</th><th scope="col">Số đơn</th></tr></thead>
            <tbody v-if="activeOrderSeries.length"><tr v-for="item in activeOrderSeries" :key="item.key"><th scope="row">{{ item.label }}</th><td>{{ item.count }}</td></tr></tbody>
            <tbody v-else><tr><td colspan="2">Không có đơn đang hoạt động.</td></tr></tbody>
          </table>
        </details>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard{display:grid;gap:14px;color:var(--admin-foreground)}
.dashboard-heading{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:8px 16px}.dashboard-heading h1{margin:0;font-size:28px;line-height:1.2}.dashboard-heading p{margin:5px 0 0;color:var(--admin-muted);text-transform:capitalize}.dashboard-heading .btn{gap:7px}.dashboard-banner{grid-column:1/-1;padding:9px 12px;border:1px solid var(--admin-border);border-radius:var(--radius-sm);background:var(--admin-surface);color:var(--admin-muted);font-size:13px}.dashboard-banner.error-state{border-color:color-mix(in srgb,var(--admin-danger) 35%,var(--admin-border));color:var(--admin-danger)}
.attention-panel,.active-flow,.flow-data{border:1px solid var(--admin-border);border-radius:var(--radius-lg);background:var(--admin-surface)}.attention-panel>header,.active-flow>header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:13px 16px;border-bottom:1px solid var(--admin-border)}.attention-panel h2,.active-flow h2{margin:0;font-size:18px}.attention-panel>header>strong{min-width:28px;padding:3px 8px;border-radius:999px;background:var(--admin-brand-soft);color:var(--admin-brand);text-align:center}.attention-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.attention-list a{position:relative;display:grid;grid-template-columns:3px 20px minmax(0,1fr) auto 16px;align-items:center;gap:9px;min-height:56px;padding:9px 13px;color:inherit;border-bottom:1px solid var(--admin-border)}.attention-list a:nth-child(odd){border-right:1px solid var(--admin-border)}.attention-list a:hover{background:var(--admin-brand-soft)}.status-rail{align-self:stretch;border-radius:2px;background:var(--admin-warning)}.attention-list a.critical .status-rail{background:var(--admin-danger)}.attention-list a>i{color:var(--admin-warning)}.attention-list a.critical>i{color:var(--admin-danger)}.attention-copy{display:grid;gap:1px}.attention-copy strong{font-size:13px}.attention-copy small{color:var(--admin-muted);font-size:11px}.attention-count{font-variant-numeric:tabular-nums}.attention-empty{display:flex;align-items:center;gap:8px;margin:0;padding:13px 16px;color:var(--admin-success)}
.operating-metrics{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));border-block:1px solid var(--admin-border);background:var(--admin-surface)}.operating-metrics article{display:grid;align-content:center;gap:5px;min-height:86px;padding:12px 14px;border-right:1px solid var(--admin-border)}.operating-metrics article:last-child{border-right:0}.operating-metrics span{color:var(--admin-muted);font-size:11px;line-height:1.35}.operating-metrics strong{font-size:18px;font-variant-numeric:tabular-nums;white-space:nowrap}
.active-flow>header p{margin:3px 0 0;color:var(--admin-muted);font-size:12px}.active-flow>header>strong{color:var(--admin-brand);font-size:14px}.chart-canvas{height:260px;padding:14px 16px}.flow-state{display:flex;min-height:160px;align-items:center;justify-content:center;gap:8px;color:var(--admin-muted)}.flow-data{padding:0 16px 14px}.flow-data summary{display:flex;min-height:40px;align-items:center;width:max-content;color:var(--admin-brand);font-size:12px;font-weight:700;cursor:pointer}.flow-data table{width:100%;border-collapse:collapse}.flow-data th,.flow-data td{padding:8px 10px;border-top:1px solid var(--admin-border);text-align:left;font-size:12px}.flow-data td:last-child,.flow-data thead th:last-child{text-align:right;font-variant-numeric:tabular-nums}.flow-data tbody th{font-weight:600}
.dashboard-state{display:flex;min-height:360px;align-items:center;justify-content:center;flex-direction:column;gap:10px;color:var(--admin-muted);text-align:center}.dashboard-state>i{font-size:28px}.dashboard-state.error-state{color:var(--admin-danger)}.permission-state>i{color:var(--admin-warning)}.dashboard-skeleton{display:grid;gap:14px}.skeleton-heading,.skeleton-attention,.skeleton-metrics i,.skeleton-flow{background:var(--admin-border);border-radius:var(--radius-sm);animation:skeleton-pulse 1.2s ease-in-out infinite}.skeleton-heading{display:flex;justify-content:space-between;height:54px;background:transparent}.skeleton-heading i{display:block;width:32%;height:38px;border-radius:var(--radius-sm);background:var(--admin-border)}.skeleton-heading i:last-child{width:90px}.skeleton-attention{height:126px}.skeleton-metrics{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:1px}.skeleton-metrics i{height:86px}.skeleton-flow{height:330px}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}@keyframes skeleton-pulse{50%{opacity:.55}}
.dashboard :is(button,a,summary):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}
@media(max-width:1100px){.operating-metrics,.skeleton-metrics{grid-template-columns:repeat(3,1fr)}.operating-metrics article:nth-child(3){border-right:0}.operating-metrics article:nth-child(-n+3){border-bottom:1px solid var(--admin-border)}}
@media(max-width:760px){.dashboard-heading{align-items:start}.attention-list{grid-template-columns:1fr}.attention-list a:nth-child(odd){border-right:0}.operating-metrics,.skeleton-metrics{grid-template-columns:repeat(2,1fr)}.operating-metrics article:nth-child(3){border-right:1px solid var(--admin-border)}.operating-metrics article:nth-child(2n){border-right:0}.operating-metrics article:nth-child(-n+4){border-bottom:1px solid var(--admin-border)}.chart-canvas{height:240px;padding:10px}.flow-data{padding-inline:10px}}
@media(max-width:480px){.dashboard-heading{grid-template-columns:1fr}.dashboard-heading .btn{justify-self:start}.operating-metrics article{min-height:82px;padding:10px}.operating-metrics strong{font-size:16px}}
@media(prefers-reduced-motion:reduce){.skeleton-heading,.skeleton-attention,.skeleton-metrics i,.skeleton-flow{animation:none}}
</style>
