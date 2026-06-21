<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const adminStore = useAdminStore();
const data = adminStore.dashboard;
const dateRange = ref('6months');
const filteredData = ref(data.revenueByMonth);
const chartRef = ref(null);
let chart = null;

function getCSSVar(name) {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}

function buildChart() {
  const primary = getCSSVar('--primary') || '#D4764A';
  const border = getCSSVar('--border') || '#E8E8E8';
  const textMid = getCSSVar('--text-mid') || '#6B6B6B';
  const chartFont = { family: "'Be Vietnam Pro', sans-serif", size: 12 };

  chart?.destroy();
  if (!chartRef.value) return;

  chart = new Chart(chartRef.value, {
    type: 'bar',
    data: {
      labels: filteredData.value.map((m) => m.month),
      datasets: [
        {
          label: 'Doanh thu',
          data: filteredData.value.map((m) => m.revenue),
          backgroundColor: primary + '33',
          borderColor: primary,
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
        x: {
          grid: { color: border },
          ticks: { font: chartFont, color: textMid },
        },
        y: {
          grid: { color: border },
          ticks: {
            font: chartFont,
            color: textMid,
            callback: (v) => (v / 1000000).toFixed(0) + 'tr',
          },
          beginAtZero: true,
        },
      },
    },
  });
}

onMounted(buildChart);
onUnmounted(() => chart?.destroy());

function exportExcel() {
  alert('Xuất Excel thành công!');
}
function exportPDF() {
  alert('Xuất PDF thành công!');
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Báo cáo doanh thu</h1>
      <div style="display: flex; gap: 8px; align-items: center">
        <select v-model="dateRange" class="form-select" style="width: auto">
          <option value="7days">7 ngày</option>
          <option value="30days">30 ngày</option>
          <option value="6months">6 tháng</option>
          <option value="1year">1 năm</option>
        </select>
        <button class="btn btn-sm btn-outline" @click="exportExcel">
          <i class="bi bi-file-earmark-excel"></i> Excel
        </button>
        <button class="btn btn-sm btn-outline" @click="exportPDF">
          <i class="bi bi-file-earmark-pdf"></i> PDF
        </button>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value" style="color: var(--primary)">
          {{ formatPrice(data.totalRevenue) }}
        </div>
        <div class="stat-label">Tổng doanh thu</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: var(--primary)">
          {{ data.totalOrders.toLocaleString() }}
        </div>
        <div class="stat-label">Tổng đơn hàng</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: var(--primary)">
          {{ formatPrice(data.revenueToday) }}
        </div>
        <div class="stat-label">Doanh thu hôm nay</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: var(--primary)">
          {{ data.ordersToday }}
        </div>
        <div class="stat-label">Đơn hôm nay</div>
      </div>
    </div>
    <div class="chart-container">
      <h3 style="margin-bottom: 12px">Doanh thu theo tháng</h3>
      <div style="height: 300px"><canvas ref="chartRef"></canvas></div>
    </div>
  </div>
</template>
