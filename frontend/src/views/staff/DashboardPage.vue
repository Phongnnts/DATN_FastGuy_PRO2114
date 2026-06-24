<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useStaffStore } from '@/stores/staff';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const staffStore = useStaffStore();

const loading = ref(true);
const statusChartRef = ref(null);

let statusChart = null;

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

function getShiftName(shift) {
  switch (shift) {
    case 'MORNING':
      return 'Sáng (06:00 - 14:00)';
    case 'AFTERNOON':
      return 'Chiều (14:00 - 22:00)';
    case 'EVENING':
      return 'Tối';
    default:
      return shift;
  }
}

function destroyCharts() {
  if (statusChart) {
    statusChart.destroy();
    statusChart = null;
  }
}

function buildCharts() {
  if (!data.value) return;

  destroyCharts();

  const ordersByStatus = data.value.ordersByStatus || {};

  const labels = Object.keys(ordersByStatus).filter(
    (key) => ordersByStatus[key] > 0,
  );

  if (!labels.length) return;

  const values = labels.map((key) => ordersByStatus[key]);

  if (statusChartRef.value) {
    statusChart = new Chart(statusChartRef.value, {
      type: 'doughnut',
      data: {
        labels: labels.map((key) => orderStatusLabels[key] || key),
        datasets: [
          {
            data: values,
            backgroundColor: labels.map(
              (key) => statusColors[key] || '#9CA3AF',
            ),
            borderWidth: 2,
            borderColor: '#FFFFFF',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              font: {
                family: "'Be Vietnam Pro', sans-serif",
                size: 12,
              },
              padding: 16,
              usePointStyle: true,
            },
          },
        },
      },
    });
  }
}

onMounted(async () => {
  try {
    await staffStore.fetchDashboard();
  } finally {
    loading.value = false;
    buildCharts();
  }
});

watch(
  () => staffStore.dashboard,
  () => {
    buildCharts();
  },
  {
    deep: true,
  },
);

onUnmounted(() => {
  destroyCharts();
});
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Tổng quan</h1>
    </div>

    <div
      v-if="loading"
      style="text-align:center;padding:40px;color:var(--text-mid)"
    >
      Đang tải dữ liệu...
    </div>

    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-value">
            {{ totalOrdersToday.toLocaleString() }}
          </div>
          <div class="stat-label">Tổng đơn hôm nay</div>
        </div>

        <div class="stat-card">
          <div class="stat-value">
            {{ data.pendingOrders || 0 }}
          </div>
          <div class="stat-label">Chờ xác nhận</div>
        </div>

        <div class="stat-card">
          <div class="stat-value">
            {{ data.confirmedOrders || 0 }}
          </div>
          <div class="stat-label">Đã xác nhận</div>
        </div>

        <div class="stat-card">
          <div class="stat-value">
            {{ data.lowStockIngredients || 0 }}
          </div>
          <div class="stat-label">Nguyên liệu sắp hết</div>
        </div>

        <div class="stat-card">
          <div class="stat-value">
            {{ data.todaySchedule ? 1 : 0 }}
          </div>
          <div class="stat-label">Ca làm hôm nay</div>
        </div>
      </div>

      <div class="grid-2">
        <div class="chart-container">
          <h3 style="margin-bottom:12px">
            Đơn hàng theo trạng thái
          </h3>

          <div
            v-if="Object.keys(data.ordersByStatus || {}).length"
            style="height:260px"
          >
            <canvas ref="statusChartRef"></canvas>
          </div>

          <div
            v-else
            style="
              height:260px;
              display:flex;
              align-items:center;
              justify-content:center;
              color:var(--text-mid);
            "
          >
            Chưa có dữ liệu đơn hàng hôm nay
          </div>
        </div>

        <div class="chart-container">
          <h3 style="margin-bottom:12px">
            Thông tin ca làm việc
          </h3>

          <div
            v-if="data.todaySchedule"
            style="padding:16px 0"
          >
            <p style="margin-bottom:8px">
              <strong>Ngày:</strong>
              {{ data.todaySchedule.workDate }}
            </p>

            <p>
              <strong>Ca:</strong>
              {{ getShiftName(data.todaySchedule.shift) }}
            </p>
          </div>

          <div
            v-else
            style="padding:16px 0;color:var(--text-mid)"
          >
            Hôm nay chưa có ca làm việc
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
