<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { adminApi } from '@/api'
import { formatPrice } from '@/utils/format'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const data = ref({ topProducts: [], periodTopProducts: [] })
const dateRange = ref('6m')
const chartRef = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await adminApi.getTopProducts({ period: dateRange.value })
    buildChart()
  } catch (e) {
    error.value = e.message || 'Không thể tải báo cáo'
  } finally {
    loading.value = false
  }
}
let chart = null
const products = () => data.value.periodTopProducts?.length ? data.value.periodTopProducts : data.value.topProducts || []

function getCSSVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function buildChart() {
  const primary = getCSSVar('--primary') || '#D4764A'
  const border = getCSSVar('--border') || '#E8E8E8'
  const textMid = getCSSVar('--text-mid') || '#6B6B6B'
  const chartFont = { family: "'Be Vietnam Pro', sans-serif", size: 12 }
  chart?.destroy()
  if (!chartRef.value || !products().length) return
  chart = new Chart(chartRef.value, {
    type: 'bar',
    data: {
      labels: products().map(p => p.name),
      datasets: [{
        label: 'Đã bán',
        data: products().map(p => p.sold),
        backgroundColor: products().map((_, i) => i < 3 ? primary : '#ddd'),
        borderColor: products().map((_, i) => i < 3 ? primary : '#ccc'),
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

onMounted(load)
watch(dateRange, load)
watch(data, buildChart, { deep: true })
onUnmounted(() => chart?.destroy())
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Sản phẩm bán chạy</h1>
      <div style="display:flex;gap:8px">
        <button class="btn btn-sm" :class="dateRange === '7d' ? 'btn-primary' : 'btn-outline'" @click="dateRange = '7d'">7 ngày</button>
        <button class="btn btn-sm" :class="dateRange === '30d' ? 'btn-primary' : 'btn-outline'" @click="dateRange = '30d'">30 ngày</button>
        <button class="btn btn-sm" :class="dateRange === '6m' ? 'btn-primary' : 'btn-outline'" @click="dateRange = '6m'">6 tháng</button>
        <button class="btn btn-sm" :class="dateRange === '1y' ? 'btn-primary' : 'btn-outline'" @click="dateRange = '1y'">1 năm</button>
      </div>
    </div>
    <div v-if="loading" class="admin-state"><span class="spinner"></span> Đang tải báo cáo...</div>
    <div v-else-if="error" class="admin-state admin-error"><span>{{ error }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <template v-else>
    <div class="chart-container">
      <h3 style="margin-bottom:12px">Top sản phẩm</h3>
      <div style="height:300px"><canvas ref="chartRef"></canvas></div>
    </div>
    <div class="card">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>#</th><th>Sản phẩm</th><th>Đã bán</th><th>Doanh thu</th></tr></thead>
          <tbody>
            <tr v-for="(p, i) in products()" :key="p.name">
              <td>{{ i + 1 }}</td><td><strong>{{ p.name }}</strong></td><td>{{ p.sold }}</td><td>{{ formatPrice(p.revenue) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </template>
  </div>
</template>
