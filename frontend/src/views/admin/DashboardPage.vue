<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const adminStore = useAdminStore();

const revenueChartRef = ref(null);
const topChartRef = ref(null);

let revenueChart = null;
let topChart = null;

const data = computed(
  () =>
    adminStore.dashboard || {
      totalUsers: 0,
      totalOrders: 0,
      totalProducts: 0,
      totalRevenue: 0,
      ordersToday: 0,
      revenueToday: 0,
      revenueByMonth: [],
      topProducts: [],
    },
);

function getCSSVar(name) {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}

function destroyCharts() {
  if (revenueChart) {
    revenueChart.destroy();
    revenueChart = null;
  }

  if (topChart) {
    topChart.destroy();
    topChart = null;
  }
}

function buildCharts() {
  if (!adminStore.dashboard) return;

  destroyCharts();

  const d = adminStore.dashboard;

  const primary = getCSSVar('--primary') || '#D4764A';
  const border = getCSSVar('--border') || '#E8E8E8';
  const textMid = getCSSVar('--text-mid') || '#6B6B6B';

  const chartFont = {
    family: "'Be Vietnam Pro', sans-serif",
    size: 12,
  };

  const chartDefaults = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      x: {
        grid: {
          color: border,
        },
        ticks: {
          font: chartFont,
          color: textMid,
        },
      },
      y: {
        grid: {
          color: border,
        },
        ticks: {
          font: chartFont,
          color: textMid,
        },
      },
    },
  };

  const months = d.revenueByMonth || [];
  const tops = d.topProducts || [];

  if (revenueChartRef.value && months.length > 0) {
    revenueChart = new Chart(revenueChartRef.value, {
      type: 'bar',
      data: {
        labels: months.map((m) => m.month || m.label),
        datasets: [
          {
            label: 'Doanh thu',
            data: months.map((m) => m.revenue || m.value),
            backgroundColor: `${primary}33`,
            borderColor: primary,
            borderWidth: 2,
            borderRadius: 4,
          },
        ],
      },
      options: {
        ...chartDefaults,
        scales: {
          x: {
            ...chartDefaults.scales.x,
          },
          y: {
            ...chartDefaults.scales.y,
            beginAtZero: true,
            ticks: {
              ...chartDefaults.scales.y.ticks,
              callback: (value) =>
                `${(Number(value) / 1000000).toFixed(0)}tr`,
            },
          },
        },
      },
    });
  }

  if (topChartRef.value && tops.length > 0) {
    revenueChart;

    topChart = new Chart(topChartRef.value, {
      type: 'bar',
      data: {
        labels: tops.slice(0, 5).map((p) => p.name),
        datasets: [
          {
            label: 'Đã bán',
            data: tops.slice(0, 5).map((p) => p.sold || p.value),
            backgroundColor: `${primary}33`,
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
          x: {
            ...chartDefaults.scales.x,
            beginAtZero: true,
          },
          y: {
            ...chartDefaults.scales.y,
          },
        },
      },
    });
  }
}

onMounted(async () => {
  await adminStore.fetchDashboard();

  await nextTick();

  buildCharts();
});

watch(
  () => adminStore.dashboard,
  async () => {
    await nextTick();

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
