<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { Chart, registerables } from 'chart.js';
import { adminApi } from '@/api';
import { formatPrice } from '@/utils/format';

Chart.register(...registerables);

const data = ref({});
const activePreset = ref('6m');
const customFrom = ref('');
const customTo = ref('');
const customActive = ref(false);
const loading = ref(true);
const error = ref('');
const charts = {};
let requestId = 0;

const refs = {
  revenueDay: ref(null), revenueMonth: ref(null), orderStatus: ref(null),
  topProducts: ref(null), revenueCategory: ref(null), paymentMethod: ref(null),
};
const COLORS = { primary: '#D4764A', blue: '#3b82f6', green: '#10b981', red: '#ef4444', yellow: '#f59e0b', purple: '#8b5cf6', pink: '#ec4899', cyan: '#06b6d4', gray: '#9ca3af' };
const STATUS_COLORS = { PENDING: COLORS.yellow, CONFIRMED: COLORS.blue, PREPARING: COLORS.purple, READY: COLORS.cyan, PICKED_UP: COLORS.pink, DELIVERED: COLORS.green, CANCELLED: COLORS.red };
const STATUS_LABELS = { PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', PICKED_UP: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy' };
const CATEGORY_COLORS = Object.values(COLORS);
const today = new Date().toLocaleDateString('en-CA');
const dateError = computed(() => {
  if (!customFrom.value || !customTo.value) return 'Vui lòng chọn đủ từ ngày và đến ngày.';
  if (customFrom.value > customTo.value) return 'Từ ngày không được sau đến ngày.';
  if (customTo.value > today) return 'Đến ngày không được sau hôm nay.';
  return '';
});
const periodLabel = computed(() => customActive.value ? `${customFrom.value} – ${customTo.value}` : ({ '7d': '7 ngày gần nhất', '30d': '30 ngày gần nhất', '6m': '6 tháng gần nhất', '1y': '1 năm gần nhất' }[activePreset.value]));
const completionRate = computed(() => Number(data.value.totalOrdersInPeriod) ? Math.round(Number(data.value.periodOrders || 0) * 100 / Number(data.value.totalOrdersInPeriod)) : 0);
const topProduct = computed(() => data.value.topProducts?.[0]);

async function load(params, activate) {
  const id = ++requestId;
  loading.value = true;
  error.value = '';
  try {
    const result = await adminApi.getFullReport(params);
    if (id !== requestId) return;
    data.value = result || {};
    activate();
    await nextTick();
    buildAllCharts();
  } catch (e) {
    if (id === requestId) error.value = e?.response?.data?.message || e.message || 'Không thể tải báo cáo.';
  } finally {
    if (id === requestId) loading.value = false;
  }
}
function usePreset(period) { load({ period }, () => { activePreset.value = period; customActive.value = false; }); }
function applyCustom() { if (!dateError.value) load({ startDate: customFrom.value, endDate: customTo.value }, () => { customActive.value = true; }); }
function refresh() { customActive.value ? applyCustom() : usePreset(activePreset.value); }
function css(name, fallback) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback; }
function moneyTick(value) { return new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(value) + '₫'; }
function config(type, labels, datasets, options = {}) {
  return { type, data: { labels, datasets }, options: { responsive: true, maintainAspectRatio: false, interaction: { intersect: false, mode: 'index' }, plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => ctx.dataset.label?.includes('Doanh thu') ? `${ctx.dataset.label}: ${formatPrice(ctx.raw)}` : `${ctx.dataset.label || ''}: ${Number(ctx.raw).toLocaleString('vi-VN')}` } }, ...options.plugins }, scales: options.scales || {}, ...options.extra } };
}
function replaceChart(key, chartConfig) { charts[key]?.destroy(); charts[key] = new Chart(refs[key].value, chartConfig); }
function buildAllCharts() {
  Object.values(charts).forEach(chart => chart?.destroy());
  Object.keys(charts).forEach(key => delete charts[key]);
  const border = css('--border', '#e5e7eb');
  const text = css('--text-mid', '#6b7280');
  const axis = (money = false) => ({ grid: { color: border }, ticks: { color: text, callback: money ? moneyTick : undefined }, beginAtZero: true });
  const day = data.value.revenueByDay || [];
  if (day.length && refs.revenueDay.value) replaceChart('revenueDay', config('line', day.map(x => x.date?.slice(5)), [{ label: 'Doanh thu', data: day.map(x => x.revenue), borderColor: COLORS.primary, backgroundColor: COLORS.primary + '22', fill: true, tension: .3, pointRadius: day.length > 30 ? 0 : 3 }], { scales: { x: axis(), y: axis(true) } }));
  const month = data.value.revenueByMonth || [];
  if (month.length && refs.revenueMonth.value) replaceChart('revenueMonth', config('bar', month.map(x => `${x.month}/${x.year}`), [{ label: 'Doanh thu', data: month.map(x => x.revenue), backgroundColor: COLORS.primary + '88', borderRadius: 5 }], { scales: { x: axis(), y: axis(true) } }));
  const status = data.value.ordersByStatus || [];
  if (status.length && refs.orderStatus.value) replaceChart('orderStatus', config('doughnut', status.map(x => STATUS_LABELS[x.status] || x.status), [{ label: 'Đơn hàng', data: status.map(x => x.count), backgroundColor: status.map(x => STATUS_COLORS[x.status] || COLORS.gray), borderWidth: 2 }], { plugins: { legend: { display: true, position: 'right' } }, extra: { cutout: '58%' } }));
  const top = (data.value.topProducts || []).slice(0, 8);
  if (top.length && refs.topProducts.value) replaceChart('topProducts', config('bar', top.map(x => x.name), [{ label: 'Đã bán', data: top.map(x => x.sold), backgroundColor: COLORS.primary + '99', borderRadius: 5 }], { extra: { indexAxis: 'y' }, scales: { x: axis(), y: { grid: { display: false }, ticks: { color: text } } } }));
  const category = data.value.revenueByCategory || [];
  if (category.length && refs.revenueCategory.value) replaceChart('revenueCategory', config('bar', category.map(x => x.category), [{ label: 'Doanh thu', data: category.map(x => x.revenue), backgroundColor: category.map((_, i) => CATEGORY_COLORS[i % CATEGORY_COLORS.length] + '99'), borderRadius: 5 }], { scales: { x: axis(), y: axis(true) } }));
  const payment = data.value.paymentMethodStats || [];
  if (payment.length && refs.paymentMethod.value) replaceChart('paymentMethod', config('doughnut', payment.map(x => x.method === 'BANK_TRANSFER' ? 'PayOS' : 'COD'), [{ label: 'Đơn hàng', data: payment.map(x => x.count), backgroundColor: [COLORS.blue, COLORS.green], borderWidth: 2 }], { plugins: { legend: { display: true, position: 'right' } }, extra: { cutout: '58%' } }));
}
function has(key) { return Array.isArray(data.value[key]) && data.value[key].length; }
onMounted(() => usePreset('6m'));
onUnmounted(() => { requestId++; Object.values(charts).forEach(chart => chart.destroy()); });
</script>

<template>
  <main class="reports-page">
    <header class="page-heading"><div><p class="eyebrow">Phân tích</p><h1>Báo cáo kinh doanh</h1><p>Theo dõi doanh thu, đơn hàng và hiệu suất sản phẩm.</p></div><button class="btn btn-outline" :disabled="loading" @click="refresh"><i class="bi bi-arrow-clockwise"></i> Làm mới</button></header>

    <section class="filter-panel" aria-label="Khoảng thời gian báo cáo">
      <div><span class="filter-label">Khoảng nhanh</span><div class="presets"><button v-for="item in [{v:'7d',l:'7 ngày'},{v:'30d',l:'30 ngày'},{v:'6m',l:'6 tháng'},{v:'1y',l:'1 năm'}]" :key="item.v" :class="{ active: !customActive && activePreset === item.v }" :aria-pressed="!customActive && activePreset === item.v" @click="usePreset(item.v)">{{ item.l }}</button></div></div>
      <div class="custom-dates"><label>Từ ngày<input v-model="customFrom" class="form-input" type="date" :max="customTo || today"></label><label>Đến ngày<input v-model="customTo" class="form-input" type="date" :min="customFrom || undefined" :max="today"></label><button class="btn btn-primary" :disabled="!!dateError || loading" @click="applyCustom">Áp dụng</button><p v-if="(customFrom || customTo) && dateError" role="alert">{{ dateError }}</p></div>
    </section>

    <div v-if="error" class="error-banner" role="alert"><i class="bi bi-exclamation-circle"></i><span><strong>Không thể cập nhật báo cáo</strong>Dữ liệu gần nhất vẫn được giữ nguyên. {{ error }}</span><button class="btn btn-outline" @click="refresh">Thử lại</button></div>
    <div v-if="loading && !Object.keys(data).length" class="loading-state"><span class="spinner"></span>Đang tải báo cáo...</div>

    <template v-if="Object.keys(data).length">
      <section class="stats" :aria-label="`Tổng quan ${periodLabel}`"><article class="orange"><i class="bi bi-graph-up-arrow"></i><div><small>Doanh thu kỳ</small><strong>{{ formatPrice(data.periodRevenue || 0) }}</strong></div></article><article class="green"><i class="bi bi-bag-check"></i><div><small>Đơn đã giao</small><strong>{{ Number(data.periodOrders || 0).toLocaleString('vi-VN') }}</strong></div></article><article class="blue"><i class="bi bi-receipt"></i><div><small>Giá trị đơn trung bình</small><strong>{{ formatPrice(data.avgOrderValue || 0) }}</strong></div></article><article class="violet"><i class="bi bi-check2-circle"></i><div><small>Tỷ lệ hoàn tất</small><strong>{{ completionRate }}%</strong></div></article></section>
      <section class="summary"><i class="bi bi-lightbulb"></i><p><strong>Tóm tắt {{ periodLabel }}:</strong> {{ Number(data.totalOrdersInPeriod || 0).toLocaleString('vi-VN') }} đơn phát sinh, {{ Number(data.periodOrders || 0).toLocaleString('vi-VN') }} đơn giao thành công.<span v-if="topProduct"> Sản phẩm dẫn đầu là <b>{{ topProduct.name }}</b> với {{ topProduct.sold }} sản phẩm.</span></p></section>

      <section class="charts-grid">
        <article class="chart-card"><header><div><h2>Xu hướng doanh thu</h2><p>Theo ngày giao hàng thành công</p></div><i class="bi bi-graph-up"></i></header><div v-if="has('revenueByDay')" class="chart"><canvas :ref="el => refs.revenueDay.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-bar-chart"></i><strong>Chưa có doanh thu theo ngày</strong><span>Không có đơn DELIVERED và PAID trong kỳ.</span></div></article>
        <article class="chart-card"><header><div><h2>Doanh thu theo tháng</h2><p>So sánh các tháng trong kỳ</p></div><i class="bi bi-calendar3"></i></header><div v-if="has('revenueByMonth')" class="chart"><canvas :ref="el => refs.revenueMonth.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-calendar-x"></i><strong>Chưa có dữ liệu theo tháng</strong></div></article>
        <article class="chart-card"><header><div><h2>Trạng thái đơn hàng</h2><p>Toàn bộ đơn phát sinh trong kỳ</p></div><i class="bi bi-pie-chart"></i></header><div v-if="has('ordersByStatus')" class="chart"><canvas :ref="el => refs.orderStatus.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-inbox"></i><strong>Chưa có đơn hàng</strong></div></article>
        <article class="chart-card"><header><div><h2>Sản phẩm bán chạy</h2><p>Số lượng từ đơn đã giao, đã thanh toán</p></div><i class="bi bi-trophy"></i></header><div v-if="has('topProducts')" class="chart"><canvas :ref="el => refs.topProducts.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-basket"></i><strong>Chưa có sản phẩm bán ra</strong></div></article>
        <article class="chart-card"><header><div><h2>Doanh thu theo danh mục</h2><p>Cơ cấu đóng góp doanh thu</p></div><i class="bi bi-grid"></i></header><div v-if="has('revenueByCategory')" class="chart"><canvas :ref="el => refs.revenueCategory.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-grid"></i><strong>Chưa có doanh thu danh mục</strong></div></article>
        <article class="chart-card"><header><div><h2>Phương thức thanh toán</h2><p>Tỷ trọng đơn thành công</p></div><i class="bi bi-credit-card"></i></header><div v-if="has('paymentMethodStats')" class="chart"><canvas :ref="el => refs.paymentMethod.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-credit-card-2-front"></i><strong>Chưa có thanh toán thành công</strong></div></article>
      </section>

      <section class="top-table"><header><div><h2>Chi tiết sản phẩm bán chạy</h2><p>Xếp hạng theo số lượng bán trong {{ periodLabel }}</p></div><span>{{ data.topProducts?.length || 0 }} sản phẩm</span></header><div v-if="has('topProducts')" class="table-wrapper"><table class="table"><thead><tr><th>Hạng</th><th>Sản phẩm</th><th>Số lượng bán</th><th>Doanh thu</th><th>Tỷ trọng</th></tr></thead><tbody><tr v-for="(product, index) in data.topProducts" :key="product.name"><td><b class="rank" :class="`rank-${index + 1}`">{{ index + 1 }}</b></td><td><strong>{{ product.name }}</strong></td><td>{{ Number(product.sold).toLocaleString('vi-VN') }}</td><td><strong>{{ formatPrice(product.revenue || 0) }}</strong></td><td>{{ data.periodRevenue ? (Number(product.revenue || 0) * 100 / data.periodRevenue).toFixed(1) : 0 }}%</td></tr></tbody></table></div><div v-else class="empty"><i class="bi bi-table"></i><strong>Chưa có dữ liệu xếp hạng</strong></div></section>
    </template>
  </main>
</template>

<style scoped>
.reports-page { display: grid; gap: 20px; }.page-heading { align-items: flex-end; display: flex; justify-content: space-between; gap: 16px; }.page-heading h1 { font-size: 28px; margin: 2px 0 4px; }.page-heading p { color: var(--text-mid); font-size: 14px; }.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.filter-panel { align-items: end; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-xs); display: flex; flex-wrap: wrap; gap: 22px; padding: 16px; }.filter-label,.custom-dates label { color: var(--text-mid); display: block; font-size: 11px; font-weight: 700; margin-bottom: 6px; }.presets { background: var(--surface); border-radius: 9px; display: flex; padding: 3px; }.presets button { border-radius: 7px; color: var(--text-mid); font-size: 12px; padding: 9px 13px; }.presets button.active { background: var(--white); box-shadow: var(--shadow-xs); color: var(--role-admin); font-weight: 700; }.custom-dates { align-items: end; display: flex; flex: 1; flex-wrap: wrap; gap: 9px; }.custom-dates label { margin: 0; }.custom-dates input { margin-top: 6px; width: 155px; }.custom-dates p { color: var(--red-active); flex-basis: 100%; font-size: 11px; margin: 0; }
.error-banner { align-items: center; background: #fef2f2; border: 1px solid #fecaca; border-radius: var(--radius); color: #b91c1c; display: flex; gap: 12px; padding: 13px 16px; }.error-banner > i { font-size: 22px; }.error-banner span { display: grid; flex: 1; font-size: 12px; }.loading-state { align-items: center; color: var(--text-mid); display: flex; gap: 10px; justify-content: center; min-height: 280px; }
.stats { display: grid; gap: 14px; grid-template-columns: repeat(4,minmax(0,1fr)); }.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); display: flex; gap: 13px; padding: 18px; }.stats article > i { align-items: center; border-radius: 11px; display: flex; flex: 0 0 44px; font-size: 20px; height: 44px; justify-content: center; }.stats small { color: var(--text-mid); display: block; font-size: 11px; margin-bottom: 3px; }.stats strong { font-size: 19px; }.stats .orange i { color: #c2410c; background: #ffedd5; }.stats .green i { color: #047857; background: #d1fae5; }.stats .blue i { color: #1d4ed8; background: #dbeafe; }.stats .violet i { color: #7c3aed; background: #ede9fe; }
.summary { align-items: center; background: #fff7ed; border: 1px solid #fed7aa; border-radius: var(--radius); color: #9a3412; display: flex; gap: 12px; padding: 13px 16px; }.summary > i { font-size: 20px; }.summary p { font-size: 13px; margin: 0; }
.charts-grid { display: grid; gap: 16px; grid-template-columns: repeat(2,minmax(0,1fr)); }.chart-card,.top-table { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-xs); overflow: hidden; }.chart-card header,.top-table header { align-items: center; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; padding: 15px 17px; }.chart-card h2,.top-table h2 { font-size: 14px; margin: 0 0 3px; }.chart-card header p,.top-table header p { color: var(--text-mid); font-size: 11px; margin: 0; }.chart-card header > i { color: var(--role-admin); font-size: 18px; }.chart { height: 285px; padding: 14px; }.empty { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 7px; justify-content: center; min-height: 285px; padding: 25px; text-align: center; }.empty i { color: var(--text-light); font-size: 32px; }.empty span { font-size: 11px; }.top-table header > span { background: var(--role-admin-soft); border-radius: 20px; color: var(--role-admin); font-size: 11px; font-weight: 700; padding: 5px 9px; }.top-table .table { min-width: 680px; }.top-table th { color: var(--text-mid); font-size: 10px; letter-spacing: .05em; text-transform: uppercase; }.rank { align-items: center; background: var(--surface); border-radius: 50%; display: inline-flex; height: 27px; justify-content: center; width: 27px; }.rank-1 { background: #fef3c7; color: #b45309; }.rank-2 { background: #f3f4f6; color: #4b5563; }.rank-3 { background: #ffedd5; color: #c2410c; }
@media (max-width: 1050px) { .stats { grid-template-columns: repeat(2,1fr); } }@media (max-width: 720px) { .page-heading { align-items: flex-start; flex-direction: column; }.filter-panel { align-items: stretch; }.custom-dates label { flex: 1; }.custom-dates input { width: 100%; }.charts-grid { grid-template-columns: 1fr; }.stats { grid-template-columns: 1fr; }.presets { overflow-x: auto; }.error-banner { align-items: flex-start; flex-wrap: wrap; } }
</style>
