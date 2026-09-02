<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { Chart, registerables } from 'chart.js';
import { useRouter } from 'vue-router';
import { adminApi } from '@/api';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { dashboardViewState } from '@/utils/adminDashboardViewState';

Chart.register(...registerables);

const adminStore = useAdminStore();
const router = useRouter();
const priorityOrders = ref([]);
const priorityState = ref('loading');
const priorityError = ref('');
const revenueChartRef = ref(null);
const statusChartRef = ref(null);
const topProductsChartRef = ref(null);
const loadState = ref('loading');
const loadError = ref(null);
let revenueChart;
let statusChart;
let topProductsChart;
let requestGeneration = 0;
let priorityRequestGeneration = 0;
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
const revenueSeries = computed(() => data.value?.revenueLast7Days || []);
const topProducts = computed(() => data.value?.topProductsLast7Days || []);
const lowStockProducts = computed(() => data.value?.lowStockProducts || []);
const banner = computed(() => {
  if (loadError.value && showContent.value) return { role: 'alert', message: errorMessage.value };
  if (viewState.value === 'refreshing') return { role: 'status', message: 'Đang cập nhật tổng quan...' };
  if (viewState.value === 'partial') return { role: 'status', message: 'Một số dữ liệu tạm thời chưa khả dụng.' };
  return null;
});

function available(section) {
  return data.value?.sectionAvailability?.[section] === 'AVAILABLE';
}
function css(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}
function shortDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { weekday: 'short' }).format(new Date(`${value}T00:00:00`));
}
function destroyCharts() {
  revenueChart?.destroy();
  statusChart?.destroy();
  topProductsChart?.destroy();
  revenueChart = statusChart = topProductsChart = null;
}
function buildCharts() {
  destroyCharts();
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const animation = reducedMotion ? false : { duration: 240 };
  const muted = css('--admin-muted');
  const border = css('--admin-border');
  const brand = css('--admin-brand');
  if (available('financial') && revenueChartRef.value && revenueSeries.value.length) revenueChart = new Chart(revenueChartRef.value, {
    type: 'line',
    data: { labels: revenueSeries.value.map(row => shortDate(row.date)), datasets: [{ label: 'Doanh thu', data: revenueSeries.value.map(row => Number(row.revenue)), borderColor: brand, backgroundColor: `${brand}22`, fill: true, tension: .35, pointRadius: 3 }] },
    options: { responsive: true, maintainAspectRatio: false, animation, plugins: { legend: { display: false } }, scales: { x: { grid: { display: false }, ticks: { color: muted } }, y: { beginAtZero: true, grid: { color: border }, ticks: { color: muted, callback: value => `${Number(value).toLocaleString('vi-VN')}đ` } } } },
  });
  if (available('orders') && statusChartRef.value && activeOrderSeries.value.length) statusChart = new Chart(statusChartRef.value, {
    type: 'doughnut',
    data: { labels: activeOrderSeries.value.map(row => row.label), datasets: [{ data: activeOrderSeries.value.map(row => row.count), backgroundColor: activeOrderSeries.value.map(row => css(row.color)), borderWidth: 0 }] },
    options: { responsive: true, maintainAspectRatio: false, cutout: '68%', animation, plugins: { legend: { position: 'bottom', labels: { color: muted, boxWidth: 9, usePointStyle: true } } } },
  });
  if (available('orders') && topProductsChartRef.value && topProducts.value.length) topProductsChart = new Chart(topProductsChartRef.value, {
    type: 'bar',
    data: { labels: topProducts.value.map(row => row.name), datasets: [{ label: 'Đã bán', data: topProducts.value.map(row => Number(row.sold)), backgroundColor: brand, borderRadius: 5 }] },
    options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, animation, plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, grid: { color: border }, ticks: { color: muted, precision: 0 } }, y: { grid: { display: false }, ticks: { color: muted } } } },
  });
}
async function loadPriorityOrders() {
  const generation = ++priorityRequestGeneration;
  priorityState.value = 'loading';
  priorityError.value = '';
  try {
    const result = await adminApi.getOrders({ attentionOnly: true, sort: 'WAITING_DESC', page: 1, pageSize: 8 });
    if (stopped || generation !== priorityRequestGeneration) return;
    priorityOrders.value = Array.isArray(result?.items) ? result.items.slice(0, 8) : Array.isArray(result) ? result.slice(0, 8) : [];
    priorityState.value = 'ready';
  } catch (error) {
    if (stopped || generation !== priorityRequestGeneration) return;
    priorityError.value = error.message || 'Không thể tải đơn cần ưu tiên.';
    priorityState.value = 'error';
  }
}
async function loadDashboard() {
  const request = { generation: ++requestGeneration };
  loadState.value = 'loading';
  loadError.value = null;
  try {
    await adminStore.fetchDashboard();
    if (stopped || request.generation !== requestGeneration) return;
    loadState.value = 'ready';
    await nextTick();
    if (!stopped && request.generation === requestGeneration) buildCharts();
  } catch (error) {
    if (stopped || request.generation !== requestGeneration) return;
    loadError.value = error;
    loadState.value = 'error';
  }
}
onMounted(() => Promise.allSettled([loadDashboard(), loadPriorityOrders()]));
onUnmounted(() => { stopped = true; requestGeneration += 1; priorityRequestGeneration += 1; destroyCharts(); });
</script>

<template>
  <div class="dashboard">
    <section v-if="viewState === 'loading'" class="dashboard-skeleton" role="status" aria-label="Đang tải tổng quan"><span class="sr-only">Đang tải tổng quan...</span><div class="skeleton-heading"></div><div class="skeleton-metrics"></div><div class="skeleton-attention"></div><div class="skeleton-flow"></div></section>
    <section v-else-if="viewState === 'forbidden'" class="dashboard-state permission-state" role="alert"><i class="bi bi-shield-lock" aria-hidden="true"></i><strong>Bạn không có quyền xem tổng quan vận hành.</strong></section>
    <section v-else-if="viewState === 'error'" class="dashboard-state error-state" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ errorMessage }}</strong><button class="btn btn-outline" type="button" @click="loadDashboard">Thử lại</button></section>
    <template v-else>
      <header class="operations-hero" data-dashboard-section="header"><div><p class="eyebrow">FASTGUY LIVE OPERATIONS</p><h1>Hoạt động hôm nay</h1><p>{{ today }}</p></div><div class="hero-actions"><router-link class="primary-action" :to="{ path: '/admin/orders', query: { status: 'ATTENTION' } }">Xem đơn cần xử lý</router-link><button class="btn btn-outline" type="button" @click="loadDashboard"><i class="bi bi-arrow-clockwise" aria-hidden="true"></i>Làm mới</button></div><div v-if="banner" class="dashboard-banner" :role="banner.role" :data-store-error="adminStore.error || undefined">{{ banner.message }}</div></header>
      <section class="dashboard-kpi-grid business-health-header" data-dashboard-section="metrics" aria-label="Chỉ số điều hành hôm nay">
        <article class="metric-card revenue-metric"><span>Doanh thu thuần hôm nay</span><strong>{{ available('financial') ? formatPrice(data.netCashRevenueToday) : 'Không khả dụng' }}</strong><small>{{ today }}</small></article>
        <article class="metric-card"><span>Đơn hàng hôm nay</span><strong>{{ available('orders') ? Number(data.operationalOrderCountToday).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article class="metric-card"><span>Tỷ lệ hoàn thành</span><strong>{{ available('orders') ? `${Number(data.completionRateToday).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%` : 'Không khả dụng' }}</strong></article>
      </section>
      <div class="primary-operations-grid business-performance-workspace">
        <section class="panel revenue-panel" aria-labelledby="revenue-title"><header><div><h2 id="revenue-title">Doanh thu 7 ngày</h2><p>Doanh thu đơn đã giao và thanh toán</p></div><strong>{{ available('financial') ? formatPrice(revenueSeries.reduce((sum, row) => sum + Number(row.revenue), 0)) : 'Không khả dụng' }}</strong></header><div v-if="available('financial') && revenueSeries.length" class="chart large"><canvas ref="revenueChartRef" role="img" aria-label="Biểu đồ doanh thu 7 ngày"></canvas></div><p v-else-if="available('financial')" class="empty">Chưa có dữ liệu doanh thu trong 7 ngày gần nhất.</p><p v-else class="empty">Không khả dụng</p><details v-if="available('financial') && revenueSeries.length" class="chart-data"><summary>Xem dữ liệu biểu đồ doanh thu</summary><table><thead><tr><th>Ngày</th><th>Doanh thu</th></tr></thead><tbody><tr v-for="row in revenueSeries" :key="row.date"><td>{{ row.date }}</td><td>{{ formatPrice(row.revenue) }}</td></tr></tbody></table></details></section>
        <section class="panel attention-panel" data-dashboard-section="attention" aria-labelledby="attention-title"><header><div><h2 id="attention-title">Cần xử lý</h2><p>Ngoại lệ ưu tiên theo thời gian</p></div><strong>{{ attentionItems.length }}</strong></header><div v-if="attentionItems.length" class="attention-list"><router-link v-for="item in attentionItems" :key="item.type" :to="item.to" :class="String(item.severity || '').toLowerCase()"><i class="attention-rail" aria-hidden="true"></i><span><strong>{{ item.label }}</strong><small>{{ item.severity === 'CRITICAL' ? 'Khẩn cấp' : 'Cảnh báo' }}</small></span><b>{{ item.count }}</b><i class="bi bi-chevron-right" aria-hidden="true"></i></router-link></div><p v-else class="empty">Không có việc cần xử lý ngay.</p></section>
        <section class="panel status-panel" aria-labelledby="status-title"><header><h2 id="status-title">Trạng thái đơn hàng</h2><strong>{{ available('orders') ? `${Number(data.activeOrderCount).toLocaleString('vi-VN')} đơn` : 'Không khả dụng' }}</strong></header><div v-if="available('orders') && activeOrderSeries.length" class="chart medium"><canvas ref="statusChartRef" role="img" aria-label="Biểu đồ trạng thái đơn hàng"></canvas></div><p v-else class="empty">{{ available('orders') ? 'Không có đơn đang hoạt động.' : 'Không khả dụng' }}</p><details v-if="available('orders') && activeOrderSeries.length" class="chart-data"><summary>Xem dữ liệu biểu đồ trạng thái</summary><ul><li v-for="row in activeOrderSeries" :key="row.key"><span>{{ row.label }}</span><strong>{{ row.count }}</strong></li></ul></details></section>
      </div>
      <div class="secondary-insights-grid">
        <section class="panel products-panel" aria-labelledby="products-title"><header><h2 id="products-title">Món bán chạy</h2><span>7 ngày gần nhất</span></header><div v-if="available('orders') && topProducts.length" class="chart medium"><canvas ref="topProductsChartRef" role="img" aria-label="Biểu đồ món bán chạy"></canvas></div><p v-else class="empty">{{ available('orders') ? 'Chưa có dữ liệu bán hàng.' : 'Không khả dụng' }}</p><details v-if="available('orders') && topProducts.length" class="chart-data"><summary>Xem dữ liệu biểu đồ món bán chạy</summary><table><thead><tr><th>Món</th><th>Đã bán</th></tr></thead><tbody><tr v-for="row in topProducts" :key="row.productId"><td>{{ row.name }}</td><td>{{ row.sold }}</td></tr></tbody></table></details></section>
        <section class="panel stock-panel" aria-labelledby="stock-title"><header><h2 id="stock-title">Món sắp tạm hết</h2><router-link to="/admin/inventory">Xem kho</router-link></header><ul v-if="available('inventory') && lowStockProducts.length"><li v-for="product in lowStockProducts" :key="product.productId"><span><strong>{{ product.name }}</strong><small>Còn đủ nguyên liệu cho khoảng {{ product.remainingServings }} phần</small></span><b>{{ product.remainingServings }}</b></li></ul><p v-else class="empty">{{ available('inventory') ? 'Các món đang đủ nguyên liệu.' : 'Không khả dụng' }}</p></section>
      </div>
      <aside class="cash-risk-rail" aria-label="Dòng tiền và rủi ro">
        <router-link v-if="available('refunds')" class="risk-item" data-cash-risk-section="refunds" :to="{ path: '/admin/refunds', query: { status: 'PENDING' } }"><span>Hoàn tiền đang chờ</span><strong>{{ Number(data.pendingRefundCount).toLocaleString('vi-VN') }}</strong></router-link><article v-else class="risk-item unavailable" data-cash-risk-section="refunds" aria-disabled="true"><span>Hoàn tiền đang chờ</span><strong>Không khả dụng</strong></article>
        <router-link v-if="available('cod')" class="risk-item" data-cash-risk-section="cod" :to="{ path: '/admin/cod-settlements', query: { status: 'SUBMITTED' } }"><span>COD đang chờ bàn giao</span><strong>{{ Number(data.pendingCodCount).toLocaleString('vi-VN') }}</strong></router-link><article v-else class="risk-item unavailable" data-cash-risk-section="cod" aria-disabled="true"><span>COD đang chờ bàn giao</span><strong>Không khả dụng</strong></article>
        <router-link v-if="available('staffing')" class="risk-item" data-cash-risk-section="staffing" :to="{ path: '/admin/shifts', query: { tab: 'monitoring' } }"><span>Ca làm thiếu nhân sự</span><strong>{{ Number(data.staffingGapCount).toLocaleString('vi-VN') }}</strong></router-link><article v-else class="risk-item unavailable" data-cash-risk-section="staffing" aria-disabled="true"><span>Ca làm thiếu nhân sự</span><strong>Không khả dụng</strong></article>
      </aside>
      <section class="panel priority-workspace priority-orders-panel" aria-labelledby="priority-orders-title">
        <header><div><h2 id="priority-orders-title">Đơn cần ưu tiên</h2><p>Xếp theo thời gian chờ và ngoại lệ vận hành</p></div><router-link :to="{ path: '/admin/orders', query: { status: 'ATTENTION' } }">Xem tất cả</router-link></header>
        <div v-if="priorityState === 'loading'" class="priority-order-skeleton" role="status">Đang tải đơn cần ưu tiên...</div>
        <div v-else-if="priorityState === 'error'" class="priority-order-error" role="alert"><span>{{ priorityError }}</span><button type="button" @click="loadPriorityOrders">Thử lại</button></div>
        <div v-else-if="priorityOrders.length" class="priority-order-list"><button v-for="order in priorityOrders" :key="order.orderId" type="button" @click="router.push({ path: '/admin/orders', query: { status: 'ATTENTION', orderId: order.orderId } })"><span><strong>{{ order.orderCode }}</strong><small>{{ order.customerName || 'Khách hàng' }}</small></span><span>{{ order.waitingMinutes }} phút</span><strong>{{ formatPrice(order.finalAmount || 0) }}</strong><OrderStatusBadge :status="order.status" /></button></div>
        <p v-else class="empty">Không có đơn cần ưu tiên.</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard{display:grid;gap:20px;color:var(--admin-foreground)}.operations-hero{display:grid;grid-template-columns:1fr auto;align-items:end;gap:12px 20px;padding:8px 2px}.operations-hero h1{margin:2px 0 0;font-size:32px;letter-spacing:-.035em}.operations-hero p{margin:5px 0 0;color:var(--admin-muted);text-transform:capitalize}.operations-hero .eyebrow{margin:0;color:var(--admin-brand);font-size:11px;font-weight:700;letter-spacing:.11em;text-transform:uppercase}.hero-actions{display:flex;align-items:center;gap:10px}.primary-action{display:inline-flex;align-items:center;min-height:42px;padding:0 16px;border-radius:10px;background:var(--admin-action);color:#fff;font-weight:650}.dashboard-banner{grid-column:1/-1;padding:10px 12px;border:1px solid var(--admin-border);border-radius:10px;background:var(--admin-surface)}.cockpit-grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:16px}.metric-card,.panel{border:1px solid var(--admin-border);border-radius:17px;background:var(--admin-surface);box-shadow:0 4px 14px rgba(15,23,42,.055);overflow:hidden}.metric-card{grid-column:span 3;display:grid;gap:10px;padding:20px}.operating-metrics span{color:var(--admin-muted);font-size:12px}.operating-metrics strong{font-size:24px;letter-spacing:-.025em;font-variant-numeric:tabular-nums}.panel>header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:17px 19px;border-bottom:1px solid var(--admin-border)}.panel h2{margin:0;font-size:16px}.panel header p,.panel header span{margin:3px 0 0;color:var(--admin-muted);font-size:11px}.panel header>strong{color:var(--admin-brand);font-size:14px}.revenue-panel{grid-column:span 8}.attention-panel{grid-column:span 4}.status-panel{grid-column:span 4}.products-panel{grid-column:span 5}.stock-panel{grid-column:span 3}.chart{padding:14px 18px}.chart.large{height:290px}.chart.medium{height:280px}.chart-data{border-top:1px solid var(--admin-border);padding:12px 18px}.chart-data summary{min-height:40px;color:var(--admin-brand);cursor:pointer;font-size:12px;font-weight:650}.chart-data table{width:100%;border-collapse:collapse}.chart-data th,.chart-data td{padding:8px 0;border-top:1px solid var(--admin-border);font-size:12px;text-align:left}.chart-data th:last-child,.chart-data td:last-child{text-align:right}.chart-data ul{display:grid;list-style:none;margin:0;padding:0}.chart-data li{display:flex;justify-content:space-between;padding:8px 0;border-top:1px solid var(--admin-border);font-size:12px}.attention-list{display:grid}.attention-list a{display:grid;grid-template-columns:20px 1fr auto 14px;align-items:center;gap:9px;min-height:58px;padding:9px 16px;border-bottom:1px solid var(--admin-border);color:inherit}.attention-list a:hover{background:var(--admin-brand-soft)}.attention-list a:last-child{border-bottom:0}.attention-list a>i:first-child{color:var(--admin-warning)}.attention-list span{display:grid;gap:2px}.attention-list small,.stock-panel li small,.priority-order-list small{color:var(--admin-muted);font-size:10px}.stock-panel ul{list-style:none;margin:0;padding:0}.stock-panel li{display:flex;justify-content:space-between;gap:10px;padding:13px 16px;border-bottom:1px solid var(--admin-border)}.stock-panel li span{display:grid;gap:3px}.stock-panel li b{color:var(--admin-warning)}.priority-orders-panel{grid-column:1/-1}.priority-order-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;background:var(--admin-border)}.priority-order-list button{display:grid;grid-template-columns:minmax(0,1fr) auto auto auto;align-items:center;gap:12px;min-height:64px;padding:10px 16px;border:0;background:var(--admin-surface);color:inherit;text-align:left}.priority-order-list button:hover{background:var(--admin-brand-soft)}.priority-order-list button>span:first-child{display:grid}.priority-order-skeleton,.priority-order-error{padding:18px}.priority-order-error{display:flex;align-items:center;justify-content:space-between;gap:12px}.empty{margin:0;padding:24px 18px;color:var(--admin-muted);text-align:center}.dashboard-state{display:grid;place-items:center;gap:12px;min-height:300px;text-align:center}.dashboard-state i{font-size:32px}.error-state{color:var(--admin-danger)}.dashboard-skeleton{display:grid;gap:16px}.dashboard-skeleton>div{border-radius:17px;background:linear-gradient(90deg,var(--admin-surface-muted),var(--admin-surface),var(--admin-surface-muted));background-size:200% 100%;animation:pulse 1.4s infinite}.skeleton-heading{height:55px}.skeleton-metrics{height:110px}.skeleton-attention{height:220px}.skeleton-flow{height:300px}@keyframes pulse{to{background-position:-200% 0}}@media(max-width:1050px){.metric-card{grid-column:span 6}.revenue-panel,.attention-panel,.status-panel,.products-panel,.stock-panel{grid-column:span 6}.priority-order-list{grid-template-columns:1fr}}@media(max-width:760px){.operations-hero{grid-template-columns:1fr}.hero-actions{align-items:stretch;flex-direction:column}.metric-card,.revenue-panel,.attention-panel,.status-panel,.products-panel,.stock-panel{grid-column:1/-1}.chart.large,.chart.medium{height:240px}.priority-order-list button{grid-template-columns:1fr auto}.priority-order-list button>*:nth-child(3),.priority-order-list button>*:nth-child(4){display:none}}@media(prefers-reduced-motion:reduce){.dashboard-skeleton>div{animation:none}}
</style>

<style scoped>
.dashboard{gap:18px;min-width:0}.chart canvas{width:100%!important;max-width:100%}.business-health-header .revenue-metric{grid-column:span 6}.business-health-header .revenue-metric strong{font-size:clamp(34px,4vw,52px)}.business-health-header .revenue-metric small{color:var(--admin-muted);text-transform:capitalize}.business-health-header>.metric-card:not(.revenue-metric){grid-column:span 3}.business-performance-workspace .revenue-panel{grid-column:span 8}.business-performance-workspace .attention-panel{grid-column:span 4}.business-performance-workspace .status-panel{grid-column:1/-1}.secondary-insights-grid .products-panel{grid-column:span 8}.secondary-insights-grid .stock-panel{grid-column:span 4}.cash-risk-rail{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.risk-item{display:grid;gap:8px;min-width:0;min-height:88px;padding:17px 19px;border:1px solid var(--admin-border);border-radius:var(--admin-panel-radius);background:var(--admin-surface);box-shadow:var(--admin-card-shadow)}.risk-item span{color:var(--admin-muted);font-size:12px}.risk-item strong{font-size:18px;font-variant-numeric:tabular-nums}.risk-item.unavailable{box-shadow:none;color:var(--admin-muted)}.risk-item:focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}@media(max-width:760px){.dashboard .dashboard-kpi-grid,.dashboard .primary-operations-grid,.dashboard .secondary-insights-grid,.dashboard .cash-risk-rail{grid-template-columns:minmax(0,1fr)}.business-health-header .revenue-metric,.business-health-header>.metric-card:not(.revenue-metric),.business-performance-workspace .revenue-panel,.business-performance-workspace .attention-panel,.business-performance-workspace .status-panel,.secondary-insights-grid .products-panel,.secondary-insights-grid .stock-panel{grid-column:1}.dashboard,.panel,.metric-card,.risk-item{max-width:100%;min-width:0}.hero-actions{width:100%;flex-wrap:wrap}.hero-actions>*{flex:1 1 150px;justify-content:center}}
.dashboard{gap:18px}.operations-hero{position:relative;display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:14px 24px;padding:14px 4px 6px}.operations-hero::after{position:absolute;right:8%;bottom:-8px;width:220px;height:80px;border-radius:50%;background:radial-gradient(circle,rgba(244,91,42,.09),transparent 68%);content:"";pointer-events:none}.operations-hero h1{margin:4px 0 2px;font-size:clamp(30px,3vw,42px);line-height:1.08;letter-spacing:-.055em}.operations-hero>div:first-child>p:last-child{color:var(--admin-muted);text-transform:capitalize}.operations-hero .eyebrow{color:var(--admin-brand);font-size:10px;font-weight:850;letter-spacing:.14em}.dashboard-kpi-grid,.primary-operations-grid,.secondary-insights-grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:14px}.dashboard-kpi-grid .metric-card{grid-column:span 3;min-height:132px;padding:20px;border-radius:var(--admin-panel-radius);box-shadow:var(--admin-card-shadow)}.dashboard-kpi-grid .metric-card span{color:var(--admin-muted);font-size:12px}.dashboard-kpi-grid .metric-card strong{align-self:end;font-size:clamp(22px,2vw,30px);line-height:1.1;letter-spacing:-.04em}.panel{border-radius:var(--admin-panel-radius);box-shadow:var(--admin-card-shadow)}.panel>header{padding:18px 20px}.attention-list{padding:8px}.attention-list a{position:relative;grid-template-columns:4px minmax(0,1fr) auto 18px;min-height:62px;padding:10px 9px;border-radius:12px;background:transparent}.attention-list a:hover{background:var(--admin-surface-subtle)}.attention-rail{width:3px;height:34px;border-radius:3px;background:var(--admin-warning)}.attention-list a.critical .attention-rail{background:var(--admin-danger)}.priority-workspace{overflow:hidden}.priority-workspace>header{background:linear-gradient(90deg,var(--admin-surface),var(--admin-surface-subtle))}.priority-order-list button{min-height:62px}.primary-action{box-shadow:0 8px 18px rgba(196,63,22,.18)}@media(max-width:1100px){.dashboard-kpi-grid .metric-card{grid-column:span 6}.business-performance-workspace .revenue-panel,.business-performance-workspace .attention-panel{grid-column:1/-1}.secondary-insights-grid .products-panel{grid-column:span 6}.secondary-insights-grid .stock-panel{grid-column:1/-1}}@media(max-width:680px){.operations-hero{grid-template-columns:1fr;align-items:start}.operations-hero::after{display:none}.hero-actions{width:100%;flex-wrap:wrap}.hero-actions .primary-action{flex:1;justify-content:center}.dashboard-kpi-grid .metric-card{grid-column:span 6;min-height:118px;padding:16px}.secondary-insights-grid .status-panel,.secondary-insights-grid .products-panel,.secondary-insights-grid .stock-panel{grid-column:1/-1}.priority-order-list button{grid-template-columns:minmax(0,1fr) auto}.priority-order-list button>:nth-child(2){display:none}}@media(max-width:430px){.dashboard-kpi-grid{gap:10px}.dashboard-kpi-grid .metric-card{grid-column:1/-1;min-height:104px}.operations-hero h1{font-size:30px}}@media(prefers-reduced-motion:reduce){.primary-action,.attention-list a{transition:none}}
</style>
