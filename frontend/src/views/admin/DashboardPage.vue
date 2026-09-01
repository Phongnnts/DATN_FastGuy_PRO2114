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
      <header class="dashboard-heading" data-dashboard-section="header"><div><h1>Hoạt động hôm nay</h1><p>{{ today }}</p></div><button class="btn btn-outline" type="button" @click="loadDashboard"><i class="bi bi-arrow-clockwise" aria-hidden="true"></i>Làm mới</button><div v-if="banner" class="dashboard-banner" :role="banner.role" :data-store-error="adminStore.error || undefined">{{ banner.message }}</div></header>
      <section class="operating-metrics cockpit-grid" data-dashboard-section="metrics" aria-label="Chỉ số vận hành">
        <article><i class="bi bi-cash-stack"></i><span>Doanh thu thuần hôm nay</span><strong>{{ available('financial') ? formatPrice(data.netCashRevenueToday) : 'Không khả dụng' }}</strong></article>
        <article><i class="bi bi-receipt"></i><span>Đơn hàng hôm nay</span><strong>{{ available('orders') ? Number(data.operationalOrderCountToday).toLocaleString('vi-VN') : 'Không khả dụng' }}</strong></article>
        <article><i class="bi bi-bag-check"></i><span>Giá trị đơn trung bình</span><strong>{{ available('financial') ? formatPrice(data.aovToday) : 'Không khả dụng' }}</strong></article>
        <article><i class="bi bi-check2-circle"></i><span>Tỷ lệ hoàn thành</span><strong>{{ available('orders') ? `${Number(data.completionRateToday).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%` : 'Không khả dụng' }}</strong></article>
      </section>
      <section class="panel priority-orders-panel" aria-labelledby="priority-orders-title">
        <header><div><h2 id="priority-orders-title">Đơn cần ưu tiên</h2><p>Xếp theo thời gian chờ và ngoại lệ vận hành</p></div><router-link :to="{ path: '/admin/orders', query: { status: 'ATTENTION' } }">Xem tất cả</router-link></header>
        <div v-if="priorityState === 'loading'" class="priority-order-skeleton" role="status">Đang tải đơn cần ưu tiên...</div>
        <div v-else-if="priorityState === 'error'" class="priority-order-error" role="alert"><span>{{ priorityError }}</span><button type="button" @click="loadPriorityOrders">Thử lại</button></div>
        <div v-else-if="priorityOrders.length" class="priority-order-list">
          <button v-for="order in priorityOrders" :key="order.orderId" type="button" @click="router.push({ path: '/admin/orders', query: { status: 'ATTENTION', orderId: order.orderId } })"><span><strong>{{ order.orderCode }}</strong><small>{{ order.customerName || 'Khách hàng' }}</small></span><span>{{ order.waitingMinutes }} phút</span><strong>{{ formatPrice(order.finalAmount || 0) }}</strong><OrderStatusBadge :status="order.status" /></button>
        </div>
        <p v-else class="empty">Không có đơn cần ưu tiên.</p>
      </section>
      <div class="cockpit-grid">
        <section class="panel revenue-panel" aria-labelledby="revenue-title"><header><div><h2 id="revenue-title">Doanh thu 7 ngày</h2><p>Doanh thu đơn đã giao và thanh toán</p></div><strong>{{ available('financial') ? formatPrice(revenueSeries.reduce((sum, row) => sum + Number(row.revenue), 0)) : 'Không khả dụng' }}</strong></header><div v-if="available('financial') && revenueSeries.length" class="chart large"><canvas ref="revenueChartRef" role="img" aria-label="Biểu đồ doanh thu 7 ngày"></canvas></div><p v-else class="empty">Không khả dụng</p></section>
        <section class="panel attention-panel" data-dashboard-section="attention" aria-labelledby="attention-title"><header><h2 id="attention-title">Cần xử lý</h2><strong>{{ attentionItems.length }}</strong></header><div v-if="attentionItems.length" class="attention-list"><router-link v-for="item in attentionItems" :key="item.type" :to="item.to"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span><strong>{{ item.label }}</strong><small>{{ item.severity === 'CRITICAL' ? 'Khẩn cấp' : 'Cảnh báo' }}</small></span><b>{{ item.count }}</b><i class="bi bi-chevron-right" aria-hidden="true"></i></router-link></div><p v-else class="empty">Không có việc cần xử lý ngay.</p></section>
        <section class="panel status-panel" aria-labelledby="status-title"><header><h2 id="status-title">Trạng thái đơn hàng</h2><strong>{{ Number(data.activeOrderCount).toLocaleString('vi-VN') }} đơn</strong></header><div v-if="activeOrderSeries.length" class="chart medium"><canvas ref="statusChartRef" role="img" aria-label="Biểu đồ trạng thái đơn hàng"></canvas></div><p v-else class="empty">Không có đơn đang hoạt động.</p></section>
        <section class="panel products-panel" aria-labelledby="products-title"><header><h2 id="products-title">Món bán chạy</h2><span>7 ngày gần nhất</span></header><div v-if="topProducts.length" class="chart medium"><canvas ref="topProductsChartRef" role="img" aria-label="Biểu đồ món bán chạy"></canvas></div><p v-else class="empty">Chưa có dữ liệu bán hàng.</p></section>
        <section class="panel stock-panel" aria-labelledby="stock-title"><header><h2 id="stock-title">Món sắp tạm hết</h2><router-link to="/admin/inventory">Xem kho</router-link></header><ul v-if="available('inventory') && lowStockProducts.length"><li v-for="product in lowStockProducts" :key="product.productId"><span><strong>{{ product.name }}</strong><small>Còn đủ nguyên liệu cho khoảng {{ product.remainingServings }} phần</small></span><b>{{ product.remainingServings }}</b></li></ul><p v-else class="empty">{{ available('inventory') ? 'Các món đang đủ nguyên liệu.' : 'Không khả dụng' }}</p></section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard{display:grid;gap:16px;color:var(--admin-foreground)}.dashboard-heading{display:grid;grid-template-columns:1fr auto;align-items:end;gap:8px 16px}.dashboard-heading h1{margin:0;font-size:28px}.dashboard-heading p{margin:5px 0 0;color:var(--admin-muted);text-transform:capitalize}.dashboard-banner{grid-column:1/-1;padding:10px 12px;border:1px solid var(--admin-border);border-radius:10px;background:var(--admin-surface)}.cockpit-grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:16px}.operating-metrics article,.panel{border:1px solid var(--admin-border);border-radius:12px;background:var(--admin-surface);box-shadow:0 2px 8px rgba(15,23,42,.04)}.operating-metrics article{grid-column:span 3;display:grid;grid-template-columns:auto 1fr;gap:7px 10px;align-items:center;padding:18px}.operating-metrics article>i{grid-row:1/3;display:grid;place-items:center;width:40px;height:40px;border-radius:10px;background:var(--admin-brand-soft);color:var(--admin-brand);font-size:19px}.operating-metrics span{color:var(--admin-muted);font-size:12px}.operating-metrics strong{font-size:21px;font-variant-numeric:tabular-nums}.panel>header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:16px 18px;border-bottom:1px solid var(--admin-border)}.panel h2{margin:0;font-size:16px}.panel header p,.panel header span{margin:3px 0 0;color:var(--admin-muted);font-size:11px}.panel header>strong{color:var(--admin-brand);font-size:14px}.revenue-panel{grid-column:span 8}.attention-panel{grid-column:span 4}.status-panel{grid-column:span 4}.products-panel{grid-column:span 5}.stock-panel{grid-column:span 3}.chart{padding:14px 18px}.chart.large{height:290px}.chart.medium{height:280px}.attention-list{display:grid}.attention-list a{display:grid;grid-template-columns:20px 1fr auto 14px;align-items:center;gap:9px;min-height:58px;padding:9px 16px;border-bottom:1px solid var(--admin-border);color:inherit}.attention-list a:hover{background:var(--admin-brand-soft)}.attention-list a>i:first-child{color:var(--admin-warning)}.attention-list span{display:grid}.attention-list small,.stock-panel small{color:var(--admin-muted);font-size:11px}.stock-panel ul{list-style:none;margin:0;padding:0}.stock-panel li{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:14px 16px;border-bottom:1px solid var(--admin-border)}.stock-panel li span{display:grid;gap:3px;min-width:0}.stock-panel li strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}.stock-panel li b{display:grid;place-items:center;min-width:34px;height:34px;border-radius:50%;background:var(--admin-brand-soft);color:var(--admin-brand)}.empty{display:flex;min-height:120px;align-items:center;justify-content:center;margin:0;padding:16px;color:var(--admin-muted);text-align:center}.dashboard-state{display:flex;min-height:360px;align-items:center;justify-content:center;flex-direction:column;gap:10px;color:var(--admin-muted)}.error-state{color:var(--admin-danger)}.dashboard-skeleton{display:grid;gap:16px}.skeleton-heading,.skeleton-metrics,.skeleton-attention,.skeleton-flow{border-radius:12px;background:var(--admin-border);animation:pulse 1.2s infinite}.skeleton-heading{height:54px}.skeleton-metrics{height:110px}.skeleton-attention{height:260px}.skeleton-flow{height:280px}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}.dashboard :is(button,a):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}@keyframes pulse{50%{opacity:.55}}@media(max-width:1100px){.operating-metrics article{grid-column:span 6}.revenue-panel,.attention-panel{grid-column:span 6}.status-panel,.products-panel,.stock-panel{grid-column:span 6}}@media(max-width:760px){.dashboard-heading{grid-template-columns:1fr}.operating-metrics article,.revenue-panel,.attention-panel,.status-panel,.products-panel,.stock-panel{grid-column:1/-1}.chart.large,.chart.medium{height:250px}}@media(prefers-reduced-motion:reduce){.dashboard-skeleton>*{animation:none}}
</style>
