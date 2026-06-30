<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useStaffStore } from '@/stores/staff';
import { formatPrice } from '@/utils/format';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const staffStore = useStaffStore();
const loading = ref(true);
const statusChartRef = ref(null);
const revenueChartRef = ref(null);
let statusChart = null;
let revenueChart = null;

const data = computed(() => staffStore.dashboard);

const totalOrdersToday = computed(() => {
  if (!data.value) return 0;
  return data.value.ordersToday || 0;
});

const orderStatusLabels = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chế biến',
  READY: 'Đã sẵn sàng',
  DELIVERING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

const statusColors = {
  PENDING: '#F59E0B',
  CONFIRMED: '#3B82F6',
  PREPARING: '#8B5CF6',
  READY: '#10B981',
  DELIVERING: '#06B6D4',
  DELIVERED: '#22C55E',
  CANCELLED: '#EF4444',
};

function getCSSVar(name) {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}

function buildCharts() {
  if (!data.value) return;
  destroyCharts();
  const d = data.value;
  const obs = d.ordersByStatus || {};

  const labels = Object.keys(obs).filter((k) => obs[k] > 0);
  const values = labels.map((k) => obs[k]);

  if (statusChartRef.value && values.length) {
    const primary = getCSSVar('--primary') || '#D4764A';
    statusChart = new Chart(statusChartRef.value, {
      type: 'bar',
      data: {
        labels: labels.map((k) => orderStatusLabels[k] || k),
        datasets: [
          {
            label: 'Số đơn',
            data: values,
            backgroundColor: labels.map((k) => (statusColors[k] || '#999') + '66'),
            borderColor: labels.map((k) => statusColors[k] || '#999'),
            borderWidth: 2,
            borderRadius: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1 } },
        },
      },
    });
  }

  if (revenueChartRef.value && data.value?.revenueByMonth?.length) {
    const primary = getCSSVar('--primary') || '#D4764A';
    revenueChart = new Chart(revenueChartRef.value, {
      type: 'bar',
      data: {
        labels: data.value.revenueByMonth.map(m => 'Tháng ' + m.month),
        datasets: [{
          label: 'Doanh thu',
          data: data.value.revenueByMonth.map(m => m.revenue),
          backgroundColor: primary + '33',
          borderColor: primary,
          borderWidth: 2,
          borderRadius: 4,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (v) => (v / 1000000).toFixed(0) + 'tr' } },
        },
      },
    });
  }
}

function destroyCharts() {
  statusChart?.destroy(); statusChart = null;
  revenueChart?.destroy(); revenueChart = null;
}

onMounted(async () => {
  await staffStore.fetchDashboard();
  loading.value = false;
  buildCharts();
});

watch(
  () => staffStore.dashboard,
  () => {
    buildCharts();
  },
);

onUnmounted(destroyCharts);
</script>

<template>
  <div>
    <div class="page-header"><h1>Tổng quan</h1></div>
    <div
      v-if="loading"
      style="text-align: center; padding: 40px; color: var(--text-mid)"
    >
      Đang tải...
    </div>
    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-value">{{ totalOrdersToday.toLocaleString() }}</div>
          <div class="stat-label">Tổng đơn hôm nay</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ data.pendingOrders || 0 }}</div>
          <div class="stat-label">Chờ xác nhận</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ data.confirmedOrders || 0 }}</div>
          <div class="stat-label">Đã xác nhận</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ data.todaySchedule ? 1 : 0 }}</div>
          <div class="stat-label">Nhân viên trực</div>
        </div>
      </div>
      <div class="grid-3">
        <div class="chart-container">
          <h3 style="margin-bottom: 12px">Đơn hàng theo trạng thái</h3>
          <div style="height: 260px"><canvas ref="statusChartRef"></canvas></div>
        </div>
        <div class="chart-container">
          <h3 style="margin-bottom: 12px">Doanh thu theo tháng</h3>
          <div style="height: 260px"><canvas ref="revenueChartRef"></canvas></div>
        </div>
        <div class="chart-container">
          <h3 style="margin-bottom: 12px">Thông tin ca làm việc</h3>
          <div v-if="data.todaySchedule" style="padding: 16px 0">
            <p style="margin-bottom: 8px">
              <strong>Ngày:</strong> {{ data.todaySchedule.workDate }}
            </p>
            <p>
              <strong>Ca:</strong>
              {{ data.todaySchedule.shift === 'MORNING' ? 'Sáng (6h-14h)' : data.todaySchedule.shift === 'AFTERNOON' ? 'Chiều (14h-22h)' : data.todaySchedule.shift }}
            </p>
          </div>
          <div v-else style="padding: 16px 0; color: var(--text-mid)">
            <p>Hôm nay chưa có ca làm việc</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
