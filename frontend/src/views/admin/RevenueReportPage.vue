<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { adminApi } from '@/api';
import { useToastStore } from '@/stores/toast';
import { formatPrice } from '@/utils/format';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const toast = useToastStore();
const data = ref({ totalRevenue: 0, periodRevenue: 0, periodOrders: 0, revenueToday: 0, ordersToday: 0, revenueByMonth: [] });
const dateRange = ref('6m');
const filteredData = ref([]);
const chartRef = ref(null);
const loading = ref(true);
const error = ref('');

watch(() => data.value.revenueByMonth, (val) => {
  filteredData.value = val || [];
}, { immediate: true });

async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await adminApi.getRevenueReport({ period: dateRange.value });
    if (!data.value || !data.value.revenueByMonth?.length) {
      toast.info('Không có dữ liệu doanh thu cho kỳ này');
    }
    buildChart();
  } catch (e) {
    error.value = e.message || 'Không thể tải báo cáo';
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
}
watch(dateRange, load);
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
  chart = null;
  if (!chartRef.value) return;

  const items = filteredData.value;
  if (!items.length) return;

  chart = new Chart(chartRef.value, {
    type: 'bar',
    data: {
      labels: items.map((m) => m.month),
      datasets: [
        {
          label: 'Doanh thu',
          data: items.map((m) => m.revenue),
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

onMounted(load);
onUnmounted(() => { chart?.destroy(); chart = null; });
</script>

<style scoped>
.admin-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 180px;
  color: var(--text-mid);
}
.admin-error {
  flex-direction: column;
  color: var(--red-active);
}
</style>

<template>
  <div>
    <div class="page-header">
      <h1>Báo cáo doanh thu</h1>
      <div style="display: flex; gap: 8px; align-items: center">
        <select v-model="dateRange" class="form-select" style="width: auto">
          <option value="7d">7 ngày</option>
          <option value="30d">30 ngày</option>
          <option value="6m">6 tháng</option>
          <option value="1y">1 năm</option>
        </select>
      </div>
    </div>
    <div v-if="loading" class="admin-state"><span class="spinner"></span> Đang tải báo cáo...</div>
    <div v-else-if="error" class="admin-state admin-error"><span>{{ error }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <template v-else>
      <div class="empty-state" v-if="!filteredData.length && !data.periodRevenue && !data.totalRevenue">
        <i class="bi bi-graph-up"></i>
        <h3>Chưa có dữ liệu doanh thu</h3>
        <p>Không tìm thấy dữ liệu doanh thu cho khoảng thời gian này.</p>
      </div>
      <template v-else>
        <div class="stat-grid">
          <div class="stat-card">
            <div class="stat-value" style="color: var(--primary)">
              {{ formatPrice(data.periodRevenue ?? data.totalRevenue) }}
            </div>
            <div class="stat-label">Doanh thu kỳ chọn</div>
          </div>
          <div class="stat-card">
            <div class="stat-value" style="color: var(--primary)">
              {{ Number(data.periodOrders || 0).toLocaleString() }}
            </div>
            <div class="stat-label">Đơn đã giao kỳ chọn</div>
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
      </template>
    </template>
  </div>
</template>
