<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { dashboardViewState } from '@/utils/adminDashboardViewState';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const adminStore = useAdminStore();
const revenueChartRef = ref(null);
const topChartRef = ref(null);
const statusChartRef = ref(null);
let revenueChart = null;
let topChart = null;
let statusChart = null;
let requestGeneration = 0;
let stopped = false;
const loadState = ref('loading');
const loadError = ref('');

const orderStatusLabels = {
  PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', ASSIGNED: 'Đã gán shipper',
  PICKED_UP: 'Đang giao', DELIVERY_FAILED: 'Giao thất bại', RETURNED_TO_STORE: 'Đã hoàn kho', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy',
};

const statusColors = {
  PENDING: '#F59E0B', CONFIRMED: '#3B82F6', PREPARING: '#8B5CF6',
  READY: '#10B981', ASSIGNED: '#3B82F6', PICKED_UP: '#06B6D4', DELIVERY_FAILED: '#F97316', RETURNED_TO_STORE: '#64748B', DELIVERED: '#22C55E', CANCELLED: '#EF4444',
};

const data = computed(() => adminStore.dashboard);
const viewState = computed(() => dashboardViewState(data.value, loadState.value, loadError.value));
const today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }).format(new Date());
const ATTENTION = {
  OVERDUE_PENDING_ORDERS: { label: 'Đơn chờ xác nhận quá lâu', to: { path: '/admin/orders', query: { status: 'ATTENTION' } } },
  DELIVERY_FAILED_ORDERS: { label: 'Đơn giao thất bại', to: { path: '/admin/orders', query: { status: 'ATTENTION' } } },
  PENDING_REFUNDS: { label: 'Yêu cầu hoàn tiền đang chờ', to: { path: '/admin/refunds', query: { status: 'PENDING' } } },
  STAFF_COVERAGE_GAPS: { label: 'Ca làm cần bổ sung nhân viên', to: { path: '/admin/shifts', query: { tab: 'monitoring' } } },
  LOW_STOCK_ITEMS: { label: 'Mặt hàng dưới mức an toàn', to: { path: '/admin/inventory', query: { filter: 'LOW' } } },
  PENDING_COD_SETTLEMENTS: { label: 'Bàn giao COD đang chờ', to: { path: '/admin/cod-settlements', query: { status: 'PENDING' } } },
};
const attentionItems = computed(() => (data.value.attentionItems || []).map(item => ({ ...item, ...ATTENTION[item.type] })).filter(item => item.label));

function getCSSVar(name) {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}

function buildCharts() {
  if (!adminStore.dashboard) return;
  destroyCharts();
  const d = adminStore.dashboard;
  const primary = getCSSVar('--primary') || '#E8734A';
  const border = 'rgba(23,23,23,.07)';
  const textMid = getCSSVar('--text-mid') || '#6B6B6B';

  const chartFont = { family: "'Be Vietnam Pro', sans-serif", size: 12 };
  const chartDefaults = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: chartFont, color: textMid },
      },
      y: {
        grid: { color: border },
        ticks: { font: chartFont, color: textMid },
      },
    },
  };

  const months = d.revenueByMonth || [];
  const tops = d.topProducts || [];
  const obs = d.ordersByStatus || {};

  if (revenueChartRef.value && months.length) {
    revenueChart = new Chart(revenueChartRef.value, {
      type: 'line',
      data: {
        labels: months.map((m) => m.month || m.label),
        datasets: [
          {
            label: 'Doanh thu',
            data: months.map((m) => m.revenue || m.value),
            backgroundColor: primary + '18',
            borderColor: primary,
            borderWidth: 3,
            pointRadius: 0,
            pointHoverRadius: 5,
            fill: true,
            tension: 0.38,
          },
        ],
      },
      options: {
        ...chartDefaults,
        scales: {
          x: { ...chartDefaults.scales.x },
          y: {
            ...chartDefaults.scales.y,
            beginAtZero: true,
            ticks: {
              ...chartDefaults.scales.y.ticks,
              callback: (v) => (v / 1000000).toFixed(0) + 'tr',
            },
          },
        },
      },
    });
  }

  if (topChartRef.value && tops.length) {
    topChart = new Chart(topChartRef.value, {
      type: 'bar',
      data: {
        labels: tops.slice(0, 5).map((p) => p.name),
        datasets: [
          {
            label: 'Đã bán',
            data: tops.slice(0, 5).map((p) => p.sold || p.value),
            backgroundColor: [primary, '#F09A73', '#F5B99D', '#F9D6C5', '#FCE8DE'],
            borderWidth: 0,
            borderRadius: 8,
          },
        ],
      },
      options: {
        ...chartDefaults,
        indexAxis: 'y',
        scales: {
          x: { ...chartDefaults.scales.x, beginAtZero: true },
          y: { ...chartDefaults.scales.y },
        },
      },
    });
  }

  const statusLabels = Object.keys(obs).filter((k) => obs[k] > 0);
  const statusValues = statusLabels.map((k) => obs[k]);
  if (statusChartRef.value && statusValues.length) {
    statusChart = new Chart(statusChartRef.value, {
      type: 'doughnut',
      data: {
        labels: statusLabels.map((k) => orderStatusLabels[k] || k),
        datasets: [{
          label: 'Số đơn',
          data: statusValues,
          backgroundColor: statusLabels.map((k) => statusColors[k] || '#999'),
          borderColor: '#fff',
          borderWidth: 4,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '68%',
        plugins: { legend: { display: false }, tooltip: { bodyFont: chartFont, titleFont: chartFont } },
      },
    });
  }
}

function destroyCharts() {
  revenueChart?.destroy(); revenueChart = null;
  topChart?.destroy(); topChart = null;
  statusChart?.destroy(); statusChart = null;
}

async function loadDashboard() {
  const request = { generation: ++requestGeneration };
  loadState.value = 'loading';
  loadError.value = '';
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
    loadError.value = error.message || 'Không thể tải tổng quan';
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
    <section v-if="viewState.showInitialLoading" class="dashboard-state" role="status">Đang tải tổng quan...</section>
    <section v-else-if="!viewState.showContent" class="dashboard-state error-state" role="alert">
      <strong>{{ loadError }}</strong>
      <button class="btn btn-outline" type="button" @click="loadDashboard">Thử lại</button>
    </section>
    <template v-else>
    <section v-if="viewState.banner" class="dashboard-banner" :class="{ 'error-state': viewState.banner.role === 'alert' }" :role="viewState.banner.role">
      <span>{{ viewState.banner.message }}</span>
      <button v-if="viewState.banner.role === 'alert'" class="btn btn-outline" type="button" @click="loadDashboard">Thử lại</button>
    </section>
    <header class="dashboard-heading"><div><span>TRUNG TÂM ĐIỀU HÀNH</span><h1>Hoạt động hôm nay</h1><p>{{ today }}</p></div><button class="btn btn-outline" type="button" :disabled="loadState === 'loading'" @click="loadDashboard">Làm mới</button></header>

    <section class="today-kpis" aria-label="Kết quả hôm nay">
      <article><span>Doanh thu hôm nay</span><strong>{{ formatPrice(data.revenueToday || 0) }}</strong></article>
      <article><span>Đơn đã giao</span><strong>{{ Number(data.deliveredOrdersToday || 0).toLocaleString('vi-VN') }}</strong></article>
      <article><span>Đơn đang xử lý</span><strong>{{ Number(data.activeOrdersToday || 0).toLocaleString('vi-VN') }}</strong></article>
      <article><span>Giá trị đơn trung bình</span><strong>{{ formatPrice(data.aovToday || 0) }}</strong></article>
      <article><span>Lợi nhuận gộp</span><strong>{{ data.costComplete ? formatPrice(data.grossProfitToday || 0) : 'Chưa đủ dữ liệu' }}</strong></article>
    </section>

    <section class="attention-panel" aria-labelledby="attention-title"><header><div><span>Ưu tiên xử lý</span><h2 id="attention-title">Cần chú ý</h2></div><strong>{{ attentionItems.length }}</strong></header><div v-if="attentionItems.length" class="attention-list"><router-link v-for="item in attentionItems" :key="item.type" :to="item.to" :class="item.severity.toLowerCase()"><span><i class="bi bi-exclamation-circle" aria-hidden="true"></i>{{ item.label }}</span><strong>{{ item.count }}</strong></router-link></div><p v-else class="attention-empty"><i class="bi bi-check2-circle" aria-hidden="true"></i>Không có vấn đề cần xử lý ngay.</p></section>

    <section class="analytics-grid" aria-label="Xu hướng vận hành">
      <div class="chart-card revenue-chart"><div class="chart-head"><div><span>Xu hướng</span><h2>Doanh thu gần đây</h2></div></div><div class="chart-canvas large"><canvas ref="revenueChartRef"></canvas></div></div>
      <div class="chart-card status-chart"><div class="chart-head"><div><span>Vận hành</span><h2>Trạng thái đơn</h2></div></div><div class="chart-canvas compact"><canvas ref="statusChartRef"></canvas></div></div>
      <div class="chart-card top-chart"><div class="chart-head"><div><span>Sản phẩm</span><h2>Món bán chạy</h2></div></div><div class="chart-canvas medium"><canvas ref="topChartRef"></canvas></div></div>
    </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard{color:var(--text-dark)}.dashboard-heading{display:flex;align-items:end;justify-content:space-between;gap:16px;margin-bottom:20px}.dashboard-heading span,.attention-panel header span{color:var(--role-admin);font-size:10px;font-weight:800;letter-spacing:.13em}.dashboard-heading h1{margin:4px 0;font-size:32px}.dashboard-heading p{margin:0;color:var(--text-mid);text-transform:capitalize}.today-kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-bottom:16px}.today-kpis article{display:grid;gap:8px;padding:18px;border:1px solid var(--border-light);border-radius:16px;background:#fff}.today-kpis span{color:var(--text-mid);font-size:12px}.today-kpis strong{font-size:20px}.attention-panel{margin-bottom:16px;overflow:hidden;border:1px solid #fed7aa;border-radius:18px;background:#fff}.attention-panel>header{display:flex;align-items:center;justify-content:space-between;padding:16px 18px;border-bottom:1px solid #ffedd5}.attention-panel h2{margin:3px 0 0;font-size:20px}.attention-panel>header>strong{display:grid;place-items:center;width:34px;height:34px;border-radius:50%;background:#fff7ed;color:#c2410c}.attention-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;background:var(--border-light)}.attention-list a{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:54px;padding:13px 16px;background:#fff;color:inherit}.attention-list a:hover{background:#fffaf6}.attention-list a>span{display:flex;align-items:center;gap:9px}.attention-list a.warning i{color:#d97706}.attention-list a.critical i{color:#dc2626}.attention-empty{display:flex;align-items:center;gap:8px;margin:0;padding:18px;color:#047857}.dashboard-banner{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:14px;padding:12px 16px;border:1px solid var(--border-light);border-radius:14px;background:#fff}.dashboard-state{min-height:360px;display:flex;align-items:center;justify-content:center;gap:14px;flex-direction:column;text-align:center}.error-state{color:var(--danger,#dc2626)}.dashboard-hero{position:relative;display:flex;align-items:end;justify-content:space-between;gap:30px;min-height:220px;margin-bottom:22px;padding:34px 38px;overflow:hidden;border-radius:28px;color:#fff;background:linear-gradient(125deg,#1b1714 0%,#2a211c 65%,#41271d 100%);box-shadow:0 22px 55px rgba(39,25,18,.15)}.hero-copy{position:relative;z-index:1}.hero-copy>span{color:var(--route-amber);font-size:10px;font-weight:800;letter-spacing:.18em}.hero-copy h1{margin:10px 0 8px;font-size:clamp(30px,4vw,48px);line-height:1.05;letter-spacing:-.05em}.hero-copy p{color:rgba(255,255,255,.5);font-size:12px;text-transform:capitalize}.hero-today{position:relative;z-index:1;display:flex;align-items:end;gap:28px;padding:20px 22px;border:1px solid rgba(255,255,255,.1);border-radius:20px;background:rgba(255,255,255,.06);backdrop-filter:blur(10px)}.hero-today div{display:flex;flex-direction:column;gap:5px}.hero-today span{color:rgba(255,255,255,.5);font-size:10px;font-weight:700;text-transform:uppercase}.hero-today strong{font-size:24px}.hero-today a{display:flex;align-items:center;gap:6px;color:var(--route-amber);font-size:11px;font-weight:700}.hero-orbit{position:absolute;right:-120px;top:-230px;width:500px;height:500px;border:1px solid rgba(255,255,255,.09);border-radius:50%}.hero-orbit::after{position:absolute;inset:80px;border:1px solid rgba(232,115,74,.18);border-radius:50%;content:""}.primary-stats{display:grid;grid-template-columns:1.35fr repeat(3,1fr);gap:14px;margin-bottom:14px}.metric-card{min-width:0;padding:22px;border:1px solid rgba(23,23,23,.06);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(42,28,20,.05);transition:transform var(--transition-normal),box-shadow var(--transition-normal)}.metric-card:hover{box-shadow:0 14px 35px rgba(42,28,20,.09);transform:translateY(-3px)}.metric-card.revenue{color:#fff;background:linear-gradient(135deg,var(--primary),#f09a73);border-color:transparent}.metric-top{display:flex;align-items:center;justify-content:space-between;color:var(--text-mid);font-size:11px;font-weight:700}.metric-card.revenue .metric-top{color:rgba(255,255,255,.72)}.metric-top i{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;color:var(--primary);background:var(--primary-50);font-size:15px}.metric-card.revenue .metric-top i{color:#fff;background:rgba(255,255,255,.15)}.metric-card>strong{display:block;margin:18px 0 5px;overflow:hidden;font-size:clamp(23px,2.4vw,34px);line-height:1.1;letter-spacing:-.04em;text-overflow:ellipsis;white-space:nowrap}.metric-card>small{color:var(--text-light);font-size:10px}.metric-card.revenue>small{color:rgba(255,255,255,.65)}.operation-strip{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));margin-bottom:14px;border:1px solid rgba(23,23,23,.06);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(42,28,20,.04)}.operation-strip>div{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:13px;padding:17px 20px;border-right:1px solid var(--border-light)}.operation-strip>div:last-child{border:0}.signal{display:grid;width:38px;height:38px;place-items:center;border-radius:12px;color:var(--primary);background:var(--primary-50)}.signal.warning{color:#b45309;background:#fff7e6}.signal.success{color:#15803d;background:#ecfdf3}.operation-strip p{display:flex;flex-direction:column;color:var(--text-light);font-size:9px;font-weight:700;text-transform:uppercase}.operation-strip strong{margin-top:2px;color:var(--text-dark);font-size:15px;text-transform:none}.operation-strip a{color:var(--primary);font-size:10px;font-weight:800}.operation-strip small{color:var(--text-light);font-size:9px}.analytics-grid{display:grid;grid-template-columns:minmax(0,1.7fr) minmax(280px,.8fr);gap:14px}.chart-card{min-width:0;padding:24px;border:1px solid rgba(23,23,23,.06);border-radius:22px;background:#fff;box-shadow:0 8px 30px rgba(42,28,20,.05)}.top-chart{grid-column:1/-1}.chart-head{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:20px}.chart-head span{color:var(--primary);font-size:9px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.chart-head h2{margin-top:4px;font-size:17px;letter-spacing:-.025em}.chart-head a{display:flex;align-items:center;gap:6px;color:var(--text-mid);font-size:10px;font-weight:700}.chart-canvas{position:relative}.chart-canvas.large{height:310px}.chart-canvas.medium{height:280px}.chart-canvas.compact{height:310px}.donut-center{position:absolute;top:50%;left:50%;display:flex;flex-direction:column;align-items:center;pointer-events:none;transform:translate(-50%,-50%)}.donut-center strong{font-size:25px}.donut-center span{color:var(--text-light);font-size:9px;text-transform:uppercase}
@media(max-width:1100px){.today-kpis{grid-template-columns:repeat(2,1fr)}.analytics-grid{grid-template-columns:1fr}.top-chart{grid-column:auto}}
@media(max-width:760px){.dashboard-heading{align-items:flex-start;flex-direction:column}.attention-list{grid-template-columns:1fr}.analytics-grid{grid-template-columns:minmax(0,1fr)}}
@media(max-width:520px){.today-kpis{grid-template-columns:1fr}.chart-card{padding:18px}.chart-canvas.large,.chart-canvas.medium,.chart-canvas.compact{height:260px}}
@media(prefers-reduced-motion:reduce){.metric-card{transition:none}}
</style>
