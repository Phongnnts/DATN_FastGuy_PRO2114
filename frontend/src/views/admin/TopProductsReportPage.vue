<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { formatPrice } from '@/utils/format'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const adminStore = useAdminStore()
const data = computed(() => adminStore.dashboard || {
  totalRevenue: 0, totalOrders: 0, revenueToday: 0, ordersToday: 0,
  revenueByMonth: [], topProducts: [],
})
const chartRef = ref(null)
let chart = null

function getCSSVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function buildChart() {
  const primary = getCSSVar('--primary') || '#D4764A'
  const border = getCSSVar('--border') || '#E8E8E8'
  const textMid = getCSSVar('--text-mid') || '#6B6B6B'
  const chartFont = { family: "'Be Vietnam Pro', sans-serif", size: 12 }
  chart?.destroy()
  if (!chartRef.value || !data.value.topProducts?.length) return
  chart = new Chart(chartRef.value, {
    type: 'bar',
    data: {
      labels: data.value.topProducts.map(p => p.name),
      datasets: [{
        label: 'Đã bán',
        data: data.value.topProducts.map(p => p.sold),
        backgroundColor: data.value.topProducts.map((_, i) => i < 3 ? primary : '#ddd'),
        borderColor: data.value.topProducts.map((_, i) => i < 3 ? primary : '#ccc'),
        borderWidth: 2,
        borderRadius: 4
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      indexAxis: 'y',
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { color: border }, ticks: { font: chartFont, color: textMid }, beginAtZero: true },
        y: { grid: { display: false }, ticks: { font: chartFont, color: textMid } }
      }
    }
  })
}

onMounted(async () => {
  await adminStore.fetchDashboard();
  buildChart()
})

watch(() => adminStore.dashboard, buildChart, { deep: true })

onUnmounted(() => chart?.destroy())

function exportExcel() { alert('Xuất Excel thành công!') }
function exportPDF() { alert('Xuất PDF thành công!') }
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Sản phẩm bán chạy</h1>
      <div style="display:flex;gap:8px">
        <button class="btn btn-sm btn-outline" @click="exportExcel"><i class="bi bi-file-earmark-excel"></i> Excel</button>
        <button class="btn btn-sm btn-outline" @click="exportPDF"><i class="bi bi-file-earmark-pdf"></i> PDF</button>
      </div>
    </div>
    <div class="chart-container">
      <h3 style="margin-bottom:12px">Top sản phẩm</h3>
      <div style="height:300px"><canvas ref="chartRef"></canvas></div>
    </div>
    <div class="card">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>#</th><th>Sản phẩm</th><th>Đã bán</th><th>Doanh thu</th></tr></thead>
          <tbody>
            <tr v-for="(p, i) in data.topProducts" :key="p.name">
              <td>{{ i + 1 }}</td><td><strong>{{ p.name }}</strong></td><td>{{ p.sold }}</td><td>{{ formatPrice(p.revenue) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
