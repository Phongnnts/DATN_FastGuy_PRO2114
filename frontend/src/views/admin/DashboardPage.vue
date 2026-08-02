<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const adminStore = useAdminStore();
const revenueChartRef = ref(null);
const topChartRef = ref(null);
const statusChartRef = ref(null);
let revenueChart = null;
let topChart = null;
let statusChart = null;

const orderStatusLabels = {
  PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', ASSIGNED: 'Đã gán shipper',
  PICKED_UP: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy',
};

const statusColors = {
  PENDING: '#F59E0B', CONFIRMED: '#3B82F6', PREPARING: '#8B5CF6',
  READY: '#10B981', ASSIGNED: '#3B82F6', PICKED_UP: '#06B6D4', DELIVERED: '#22C55E', CANCELLED: '#EF4444',
};

const data = computed(
  () =>
    adminStore.dashboard || {
      totalUsers: 0,
      totalOrders: 0,
      totalProducts: 0,
      totalRevenue: 0,
      pendingOrders: 0,
      ordersToday: 0,
      revenueToday: 0,
      revenueByMonth: [],
      topProducts: [],
      ordersByStatus: {},
    },
);
const today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }).format(new Date());
const completionRate = computed(() => {
  const statuses = data.value.ordersByStatus || {};
  const total = Object.values(statuses).reduce((sum, value) => sum + Number(value || 0), 0);
  return total ? Math.round((Number(statuses.DELIVERED || 0) / total) * 100) : 0;
});

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

onMounted(async () => {
  await adminStore.fetchDashboard();
  buildCharts();
});

watch(
  () => adminStore.dashboard,
  () => {
    buildCharts();
  },
);

onUnmounted(destroyCharts);
</script>

<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <div class="hero-copy"><span>FASTGUY CONTROL CENTER</span><h1>Tổng quan vận hành</h1><p>{{ today }}</p></div>
      <div class="hero-today"><div><span>Doanh thu hôm nay</span><strong>{{ formatPrice(data.revenueToday) }}</strong></div><router-link to="/admin/reports">Xem báo cáo <i class="bi bi-arrow-up-right"></i></router-link></div>
      <div class="hero-orbit" aria-hidden="true"></div>
    </section>

    <section class="primary-stats" aria-label="Chỉ số tổng quan">
      <article class="metric-card revenue"><div class="metric-top"><span>Tổng doanh thu</span><i class="bi bi-graph-up-arrow"></i></div><strong>{{ formatPrice(data.totalRevenue) }}</strong><small>Giá trị tích lũy toàn hệ thống</small></article>
      <article class="metric-card"><div class="metric-top"><span>Tổng đơn hàng</span><i class="bi bi-receipt"></i></div><strong>{{ data.totalOrders.toLocaleString() }}</strong><small>{{ data.ordersToday }} đơn phát sinh hôm nay</small></article>
      <article class="metric-card"><div class="metric-top"><span>Khách hàng</span><i class="bi bi-people"></i></div><strong>{{ data.totalUsers.toLocaleString() }}</strong><small>Tài khoản trên hệ thống</small></article>
      <article class="metric-card"><div class="metric-top"><span>Sản phẩm</span><i class="bi bi-box-seam"></i></div><strong>{{ data.totalProducts }}</strong><small>Món trong danh mục quản lý</small></article>
    </section>

    <section class="operation-strip">
      <div><span class="signal warning"><i class="bi bi-clock-history"></i></span><p>Chờ xác nhận<strong>{{ data.pendingOrders || 0 }} đơn</strong></p><router-link to="/admin/orders">Xử lý ngay</router-link></div>
      <div><span class="signal"><i class="bi bi-cart-check"></i></span><p>Đơn hôm nay<strong>{{ data.ordersToday }}</strong></p><small>Đang được vận hành</small></div>
      <div><span class="signal success"><i class="bi bi-check2-circle"></i></span><p>Tỷ lệ hoàn tất<strong>{{ completionRate }}%</strong></p><small>Theo trạng thái hiện tại</small></div>
    </section>

    <section class="analytics-grid">
      <div class="chart-card revenue-chart">
        <div class="chart-head"><div><span>Hiệu suất tài chính</span><h2>Doanh thu theo tháng</h2></div><router-link to="/admin/reports">Chi tiết <i class="bi bi-arrow-right"></i></router-link></div>
        <div class="chart-canvas large"><canvas ref="revenueChartRef"></canvas></div>
      </div>
      <div class="chart-card status-chart">
        <div class="chart-head"><div><span>Luồng đơn hàng</span><h2>Trạng thái đơn</h2></div></div>
        <div class="chart-canvas compact"><canvas ref="statusChartRef"></canvas><div class="donut-center"><strong>{{ data.totalOrders }}</strong><span>Tổng đơn</span></div></div>
      </div>
      <div class="chart-card top-chart">
        <div class="chart-head"><div><span>Khách hàng lựa chọn</span><h2>Sản phẩm bán chạy</h2></div><router-link to="/admin/products">Sản phẩm <i class="bi bi-arrow-right"></i></router-link></div>
        <div class="chart-canvas medium"><canvas ref="topChartRef"></canvas></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard{color:var(--text-dark)}.dashboard-hero{position:relative;display:flex;align-items:end;justify-content:space-between;gap:30px;min-height:220px;margin-bottom:22px;padding:34px 38px;overflow:hidden;border-radius:28px;color:#fff;background:linear-gradient(125deg,#1b1714 0%,#2a211c 65%,#41271d 100%);box-shadow:0 22px 55px rgba(39,25,18,.15)}.hero-copy{position:relative;z-index:1}.hero-copy>span{color:var(--route-amber);font-size:10px;font-weight:800;letter-spacing:.18em}.hero-copy h1{margin:10px 0 8px;font-size:clamp(30px,4vw,48px);line-height:1.05;letter-spacing:-.05em}.hero-copy p{color:rgba(255,255,255,.5);font-size:12px;text-transform:capitalize}.hero-today{position:relative;z-index:1;display:flex;align-items:end;gap:28px;padding:20px 22px;border:1px solid rgba(255,255,255,.1);border-radius:20px;background:rgba(255,255,255,.06);backdrop-filter:blur(10px)}.hero-today div{display:flex;flex-direction:column;gap:5px}.hero-today span{color:rgba(255,255,255,.5);font-size:10px;font-weight:700;text-transform:uppercase}.hero-today strong{font-size:24px}.hero-today a{display:flex;align-items:center;gap:6px;color:var(--route-amber);font-size:11px;font-weight:700}.hero-orbit{position:absolute;right:-120px;top:-230px;width:500px;height:500px;border:1px solid rgba(255,255,255,.09);border-radius:50%}.hero-orbit::after{position:absolute;inset:80px;border:1px solid rgba(232,115,74,.18);border-radius:50%;content:""}.primary-stats{display:grid;grid-template-columns:1.35fr repeat(3,1fr);gap:14px;margin-bottom:14px}.metric-card{min-width:0;padding:22px;border:1px solid rgba(23,23,23,.06);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(42,28,20,.05);transition:transform var(--transition-normal),box-shadow var(--transition-normal)}.metric-card:hover{box-shadow:0 14px 35px rgba(42,28,20,.09);transform:translateY(-3px)}.metric-card.revenue{color:#fff;background:linear-gradient(135deg,var(--primary),#f09a73);border-color:transparent}.metric-top{display:flex;align-items:center;justify-content:space-between;color:var(--text-mid);font-size:11px;font-weight:700}.metric-card.revenue .metric-top{color:rgba(255,255,255,.72)}.metric-top i{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;color:var(--primary);background:var(--primary-50);font-size:15px}.metric-card.revenue .metric-top i{color:#fff;background:rgba(255,255,255,.15)}.metric-card>strong{display:block;margin:18px 0 5px;overflow:hidden;font-size:clamp(23px,2.4vw,34px);line-height:1.1;letter-spacing:-.04em;text-overflow:ellipsis;white-space:nowrap}.metric-card>small{color:var(--text-light);font-size:10px}.metric-card.revenue>small{color:rgba(255,255,255,.65)}.operation-strip{display:grid;grid-template-columns:repeat(3,1fr);margin-bottom:14px;border:1px solid rgba(23,23,23,.06);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(42,28,20,.04)}.operation-strip>div{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:13px;padding:17px 20px;border-right:1px solid var(--border-light)}.operation-strip>div:last-child{border:0}.signal{display:grid;width:38px;height:38px;place-items:center;border-radius:12px;color:var(--primary);background:var(--primary-50)}.signal.warning{color:#b45309;background:#fff7e6}.signal.success{color:#15803d;background:#ecfdf3}.operation-strip p{display:flex;flex-direction:column;color:var(--text-light);font-size:9px;font-weight:700;text-transform:uppercase}.operation-strip strong{margin-top:2px;color:var(--text-dark);font-size:15px;text-transform:none}.operation-strip a{color:var(--primary);font-size:10px;font-weight:800}.operation-strip small{color:var(--text-light);font-size:9px}.analytics-grid{display:grid;grid-template-columns:minmax(0,1.7fr) minmax(280px,.8fr);gap:14px}.chart-card{min-width:0;padding:24px;border:1px solid rgba(23,23,23,.06);border-radius:22px;background:#fff;box-shadow:0 8px 30px rgba(42,28,20,.05)}.top-chart{grid-column:1/-1}.chart-head{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:20px}.chart-head span{color:var(--primary);font-size:9px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.chart-head h2{margin-top:4px;font-size:17px;letter-spacing:-.025em}.chart-head a{display:flex;align-items:center;gap:6px;color:var(--text-mid);font-size:10px;font-weight:700}.chart-canvas{position:relative}.chart-canvas.large{height:310px}.chart-canvas.medium{height:280px}.chart-canvas.compact{height:310px}.donut-center{position:absolute;top:50%;left:50%;display:flex;flex-direction:column;align-items:center;pointer-events:none;transform:translate(-50%,-50%)}.donut-center strong{font-size:25px}.donut-center span{color:var(--text-light);font-size:9px;text-transform:uppercase}
@media(max-width:1100px){.primary-stats{grid-template-columns:repeat(2,1fr)}.analytics-grid{grid-template-columns:1fr}.top-chart{grid-column:auto}}
@media(max-width:760px){.dashboard-hero{align-items:flex-start;flex-direction:column;min-height:0;padding:26px}.hero-today{width:100%;justify-content:space-between}.operation-strip{grid-template-columns:1fr}.operation-strip>div{border-right:0;border-bottom:1px solid var(--border-light)}.analytics-grid{grid-template-columns:minmax(0,1fr)}}
@media(max-width:520px){.primary-stats{grid-template-columns:1fr}.hero-today{align-items:flex-start;flex-direction:column;gap:12px}.chart-card{padding:18px}.chart-canvas.large,.chart-canvas.medium,.chart-canvas.compact{height:260px}}
@media(prefers-reduced-motion:reduce){.metric-card{transition:none}}
</style>
