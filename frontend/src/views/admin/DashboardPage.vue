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
  PREPARING: 'Đang chế biến', READY: 'Đã sẵn sàng',
  DELIVERING: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy',
};

const statusColors = {
  PENDING: '#F59E0B', CONFIRMED: '#3B82F6', PREPARING: '#8B5CF6',
  READY: '#10B981', DELIVERING: '#06B6D4', DELIVERED: '#22C55E', CANCELLED: '#EF4444',
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

function getCSSVar(name) {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}

function buildCharts() {
  if (!adminStore.dashboard) return;
  destroyCharts();
  const d = adminStore.dashboard;
  const primary = getCSSVar('--primary') || '#D4764A';
  const border = getCSSVar('--border') || '#E8E8E8';
  const textMid = getCSSVar('--text-mid') || '#6B6B6B';

  const chartFont = { family: "'Be Vietnam Pro', sans-serif", size: 12 };
  const chartDefaults = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        grid: { color: border },
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
      type: 'bar',
      data: {
        labels: months.map((m) => m.month || m.label),
        datasets: [
          {
            label: 'Doanh thu',
            data: months.map((m) => m.revenue || m.value),
            backgroundColor: primary + '33',
            borderColor: primary,
            borderWidth: 2,
            borderRadius: 4,
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
            backgroundColor: primary + '33',
            borderColor: primary,
            borderWidth: 2,
            borderRadius: 4,
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
          data: statusValues,
          backgroundColor: statusLabels.map((k) => statusColors[k] || '#999'),
          borderWidth: 2,
          borderColor: '#fff',
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              font: { family: "'Be Vietnam Pro', sans-serif", size: 11 },
              padding: 12,
              usePointStyle: true,
            },
          },
        },
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

<style scoped>
@media (max-width: 1024px) { :deep(.grid-3) { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { :deep(.grid-3) { grid-template-columns: 1fr; } }
</style>

<template>
  <div>
    <div class="page-header"><h1>Tổng quan</h1></div>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ data.totalUsers.toLocaleString() }}</div>
        <div class="stat-label">Người dùng</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data.totalOrders.toLocaleString() }}</div>
        <div class="stat-label">Đơn hàng</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ formatPrice(data.totalRevenue) }}</div>
        <div class="stat-label">Doanh thu</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data.totalProducts }}</div>
        <div class="stat-label">Sản phẩm</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data.ordersToday }}</div>
        <div class="stat-label">Đơn hôm nay</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ formatPrice(data.revenueToday) }}</div>
        <div class="stat-label">Doanh thu hôm nay</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data.pendingOrders || 0 }}</div>
        <div class="stat-label">Chờ xác nhận</div>
      </div>
    </div>
    <div class="grid-3">
      <div class="chart-container">
        <h3 style="margin-bottom: 12px">Doanh thu theo tháng</h3>
        <div style="height: 260px"><canvas ref="revenueChartRef"></canvas></div>
      </div>
      <div class="chart-container">
        <h3 style="margin-bottom: 12px">Sản phẩm bán chạy</h3>
        <div style="height: 260px"><canvas ref="topChartRef"></canvas></div>
      </div>
      <div class="chart-container">
        <h3 style="margin-bottom: 12px">Đơn hàng theo trạng thái</h3>
        <div style="height: 260px"><canvas ref="statusChartRef"></canvas></div>
      </div>
    </div>
  </div>
</template>
