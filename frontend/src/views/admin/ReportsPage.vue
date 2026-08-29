<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Chart, registerables } from 'chart.js';
import { adminApi } from '@/api';
import { formatPrice } from '@/utils/format';
import InventoryReportsPage from './InventoryReportsPage.vue';
import OperatingExpensesPage from './OperatingExpensesPage.vue';

Chart.register(...registerables);

const route = useRoute();
const router = useRouter();
const activeTab = computed(() => ['menu', 'expenses'].includes(route.query.tab) ? route.query.tab : 'overview');
const tabRefs = ref([]);

function selectTab(tab) {
  if (tab === activeTab.value) return;
  router.push({ query: { ...route.query, tab: tab === 'overview' ? undefined : tab } });
}

function handleTabKeydown(event, index) {
  const targets = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: 2 };
  if (!(event.key in targets)) return;
  event.preventDefault();
  const next = (targets[event.key] + 3) % 3;
  selectTab(['overview', 'menu', 'expenses'][next]);
  nextTick(() => tabRefs.value[next]?.focus());
}

const data = ref({});
const operatingProfit = ref(null);
const activePreset = ref('6m');
const customFrom = ref('');
const customTo = ref('');
const customActive = ref(false);
const loading = ref(true);
const reportWarning = ref('');
const financeWarning = ref('');
const charts = {};
let requestId = 0;

const refs = {
  revenueDay: ref(null), revenueMonth: ref(null), orderStatus: ref(null),
  topProducts: ref(null), revenueCategory: ref(null), paymentMethod: ref(null),
  revenueHour: ref(null), weekday: ref(null), refundTrend: ref(null), exceptionReasons: ref(null),
};
const COLORS = { primary: '#D4764A', blue: '#3b82f6', green: '#10b981', red: '#ef4444', yellow: '#f59e0b', purple: '#8b5cf6', pink: '#ec4899', cyan: '#06b6d4', gray: '#9ca3af' };
const STATUS_COLORS = { PENDING: COLORS.yellow, CONFIRMED: COLORS.blue, PREPARING: COLORS.purple, READY: COLORS.cyan, ASSIGNED: COLORS.blue, PICKED_UP: COLORS.pink, DELIVERY_FAILED: COLORS.yellow, RETURNED_TO_STORE: COLORS.gray, DELIVERED: COLORS.green, CANCELLED: COLORS.red };
const STATUS_LABELS = { PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chế biến', READY: 'Sẵn sàng giao', ASSIGNED: 'Đã gán shipper', PICKED_UP: 'Đang giao', DELIVERY_FAILED: 'Giao thất bại', RETURNED_TO_STORE: 'Đã hoàn kho', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy' };
const CATEGORY_COLORS = Object.values(COLORS);
const today = new Date().toLocaleDateString('en-CA');
const dateError = computed(() => {
  if (!customFrom.value || !customTo.value) return 'Vui lòng chọn đủ từ ngày và đến ngày.';
  if (customFrom.value > customTo.value) return 'Từ ngày không được sau đến ngày.';
  if (customTo.value > today) return 'Đến ngày không được sau hôm nay.';
  return '';
});
const periodLabel = computed(() => customActive.value ? `${customFrom.value} – ${customTo.value}` : ({ '7d': '7 ngày gần nhất', '30d': '30 ngày gần nhất', '6m': '6 tháng gần nhất', '1y': '1 năm gần nhất' }[activePreset.value]));
const completionRate = computed(() => Math.round(Number(data.value.completionRate || 0)));
const topProduct = computed(() => data.value.topProducts?.[0]);
const netRevenue = computed(() => Number(data.value.netCashRevenue ?? data.value.netRevenue ?? (Number(data.value.periodRevenue || 0) - Number(data.value.refundTotal || 0))));
const grossRevenue = computed(() => Number(data.value.grossRevenue ?? data.value.periodRevenue ?? 0));

function dateKey() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}`;
}
function csvCell(v) {
  let s = String(v ?? '');
  if (/^[=+\-@]/.test(s)) s = `'${s}`;
  return /[";\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}
function exportCsv() {
  const d = data.value;
  const refund = Number(d.refundTotal ?? 0);
  const rows = [
    ['Kỳ báo cáo', periodLabel.value],
    ['Tiền món', Number(d.itemRevenue ?? 0)],
    ['Phí giao hàng thu khách', Number(d.shippingRevenue ?? 0)],
    ['Giảm giá', Number(d.discountTotal ?? 0)],
    ['Doanh thu gộp', grossRevenue.value],
    [`Đã hoàn tiền (${Number(d.refundCount ?? 0).toLocaleString('vi-VN')} đơn)`, refund],
    ['Doanh thu thuần', netRevenue.value],
    ['Đơn phát sinh', Number(d.operationalOrderCount ?? 0)],
    ['Đơn hoàn tất cùng cohort', Number(d.operationalCompletedCount ?? 0)],
    [],
    ['Hạng', 'Sản phẩm', 'Số lượng bán', 'Doanh thu', 'Tỷ trọng'],
    ...(d.topProducts || []).map((p, i) => [i + 1, p.name, Number(p.sold ?? 0), Number(p.revenue ?? 0), `${(Number(p.revenue ?? 0) * 100 / (Number(d.itemRevenue) || 1)).toFixed(1)}%`]),
  ];
  const csv = '\uFEFF' + rows.map(r => r.map(csvCell).join(';')).join('\r\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `báo-cáo-${dateKey()}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function financeRange(params) {
  if (params.startDate) return { fromDate: params.startDate, toDate: params.endDate };
  const days = { '7d': 7, '30d': 30, '6m': 183, '1y': 365 }[params.period];
  const to = new Date(); const from = new Date(); from.setDate(from.getDate() - days + 1);
  return { fromDate: from.toLocaleDateString('en-CA'), toDate: to.toLocaleDateString('en-CA') };
}
async function load(params, activate) {
  const id = ++requestId;
  loading.value = true;
  reportWarning.value = ''; financeWarning.value = '';
  const [reportResult, financeResult] = await Promise.allSettled([adminApi.getFullReport(params), adminApi.getOperatingProfitReport(financeRange(params))]);
  if (id !== requestId) return;
  if (reportResult.status === 'fulfilled') { data.value = reportResult.value || {}; activate(); await nextTick(); buildAllCharts(); }
  else reportWarning.value = reportResult.reason?.response?.data?.message || reportResult.reason?.message || 'Không thể tải báo cáo kinh doanh.';
  if (financeResult.status === 'fulfilled') operatingProfit.value = financeResult.value;
  else financeWarning.value = financeResult.reason?.response?.data?.message || financeResult.reason?.message || 'Không thể tải báo cáo lợi nhuận hoạt động.';
  loading.value = false;
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
  const refundByDate = new Map((data.value.refundTrend || []).map(x => [x.date, Number(x.amount || 0)]));
  if (day.length && refs.revenueDay.value) replaceChart('revenueDay', config('line', day.map(x => x.date?.slice(5)), [{ label: 'Doanh thu gộp', data: day.map(x => x.revenue), borderColor: COLORS.primary, backgroundColor: COLORS.primary + '18', fill: true, tension: .3, pointRadius: day.length > 30 ? 0 : 3 }, { label: 'Hoàn tiền', data: day.map(x => refundByDate.get(x.date) || 0), borderColor: COLORS.red, backgroundColor: COLORS.red + '12', tension: .25, pointRadius: day.length > 30 ? 0 : 2 }, { label: 'Doanh thu thuần', data: day.map(x => Number(x.revenue || 0) - (refundByDate.get(x.date) || 0)), borderColor: COLORS.green, backgroundColor: COLORS.green + '10', tension: .3, pointRadius: day.length > 30 ? 0 : 2 }], { plugins: { legend: { display: true, position: 'bottom' } }, scales: { x: axis(), y: axis(true) } }));
  const monthly = data.value.monthlyFinancialTrend || [];
  if (monthly.length && refs.revenueMonth.value) replaceChart('revenueMonth', config('bar', monthly.map(x => `${String(x.month).padStart(2, '0')}/${x.year}`), [{ label: 'Doanh thu gộp', data: monthly.map(x => x.grossRevenue), backgroundColor: COLORS.primary + 'b8', borderRadius: 6 }, { label: 'Hoàn tiền', data: monthly.map(x => x.refundTotal), backgroundColor: COLORS.red + '9a', borderRadius: 6 }, { label: 'Doanh thu thuần', data: monthly.map(x => x.netCashRevenue), backgroundColor: COLORS.green + 'a8', borderRadius: 6 }], { plugins: { legend: { display: true, position: 'bottom' } }, scales: { x: { grid: { display: false }, ticks: { color: text, maxRotation: 0 } }, y: axis(true) } }));
  const status = data.value.ordersByStatus || [];
  if (status.length && refs.orderStatus.value) replaceChart('orderStatus', config('doughnut', status.map(x => STATUS_LABELS[x.status] || x.status), [{ label: 'Đơn hàng', data: status.map(x => x.count), backgroundColor: status.map(x => STATUS_COLORS[x.status] || COLORS.gray), borderWidth: 2 }], { plugins: { legend: { display: true, position: 'right' } }, extra: { cutout: '58%' } }));
  const top = [...(data.value.topProducts || [])].sort((a, b) => Number(b.revenue || 0) - Number(a.revenue || 0)).slice(0, 8);
  if (top.length && refs.topProducts.value) replaceChart('topProducts', config('bar', top.map(x => x.name), [{ label: 'Doanh thu tiền món', data: top.map(x => x.revenue), backgroundColor: COLORS.primary + 'a8', borderRadius: 7 }], { plugins: { tooltip: { callbacks: { label: ctx => `Doanh thu: ${formatPrice(ctx.raw)}`, afterLabel: ctx => `Số lượng: ${Number(top[ctx.dataIndex]?.sold || 0).toLocaleString('vi-VN')}` } } }, extra: { indexAxis: 'y' }, scales: { x: axis(true), y: { grid: { display: false }, ticks: { color: text, callback: (_, index) => String(top[index]?.name || '').slice(0, 26) } } } }));
  const category = [...(data.value.revenueByCategory || [])].sort((a, b) => Number(b.revenue || 0) - Number(a.revenue || 0));
  if (category.length && refs.revenueCategory.value) replaceChart('revenueCategory', config('bar', category.map(x => x.category), [{ label: 'Doanh thu tiền món', data: category.map(x => x.revenue), backgroundColor: category.map((_, i) => CATEGORY_COLORS[i % CATEGORY_COLORS.length] + '9a'), borderRadius: 7 }], { extra: { indexAxis: 'y' }, scales: { x: axis(true), y: { grid: { display: false }, ticks: { color: text } } } }));
  const payment = data.value.paymentMethodStats || [];
  const paymentTotal = payment.reduce((sum, item) => sum + Number(item.count || 0), 0);
  if (payment.length && refs.paymentMethod.value) replaceChart('paymentMethod', config('bar', payment.map(x => x.method === 'BANK_TRANSFER' ? 'PayOS' : 'COD'), [{ label: 'Tỷ trọng đơn', data: payment.map(x => paymentTotal ? Number(x.count || 0) * 100 / paymentTotal : 0), backgroundColor: [COLORS.blue + 'b8', COLORS.green + 'b8'], borderRadius: 8 }], { plugins: { tooltip: { callbacks: { label: ctx => `${ctx.raw.toFixed(1)}% · ${Number(payment[ctx.dataIndex]?.count || 0).toLocaleString('vi-VN')} đơn`, afterLabel: ctx => `Doanh thu: ${formatPrice(payment[ctx.dataIndex]?.revenue || 0)}` } } }, extra: { indexAxis: 'y' }, scales: { x: { ...axis(), max: 100, ticks: { color: text, callback: value => `${value}%` } }, y: { grid: { display: false }, ticks: { color: text } } } }));
  const hourly = data.value.revenueByHour || [];
  if (hourly.length && refs.revenueHour.value) replaceChart('revenueHour', config('bar', hourly.map(x => `${String(x.hour).padStart(2, '0')}:00`), [{ label: 'Doanh thu', data: hourly.map(x => x.revenue), backgroundColor: COLORS.blue + '99', borderRadius: 6 }], { scales: { x: { grid: { display: false }, ticks: { color: text } }, y: axis(true) } }));
  const weekday = data.value.performanceByWeekday || [];
  const weekdayLabels = ['T2','T3','T4','T5','T6','T7','CN'];
  if (weekday.length && refs.weekday.value) replaceChart('weekday', config('bar', weekday.map(x => weekdayLabels[x.weekday - 1]), [{ label: 'Đơn phát sinh', data: weekday.map(x => x.orders), backgroundColor: COLORS.purple + '88', borderRadius: 6 }, { label: 'Đơn hoàn tất', data: weekday.map(x => x.completed), backgroundColor: COLORS.green + '99', borderRadius: 6 }], { plugins: { legend: { display: true, position: 'bottom' } }, scales: { x: { stacked: false, grid: { display: false }, ticks: { color: text } }, y: axis() } }));
  const refunds = data.value.refundTrend || [];
  if (refunds.length && refs.refundTrend.value) replaceChart('refundTrend', config('line', refunds.map(x => x.date?.slice(5)), [{ label: 'Hoàn tiền', data: refunds.map(x => x.amount), borderColor: COLORS.red, backgroundColor: COLORS.red + '18', fill: true, tension: .3 }], { scales: { x: axis(), y: axis(true) } }));
  const reasons = data.value.exceptionReasons || [];
  if (reasons.length && refs.exceptionReasons.value) replaceChart('exceptionReasons', config('bar', reasons.map(x => x.reason), [{ label: 'Số đơn', data: reasons.map(x => x.count), backgroundColor: COLORS.yellow + 'aa', borderRadius: 6 }], { extra: { indexAxis: 'y' }, scales: { x: axis(), y: { grid: { display: false }, ticks: { color: text } } } }));
}
function has(key) { return Array.isArray(data.value[key]) && data.value[key].length; }
watch(activeTab, (tab) => {
  if (tab === 'overview') nextTick(() => Object.keys(data.value).length ? buildAllCharts() : usePreset(activePreset.value));
  else Object.values(charts).forEach(chart => chart?.destroy());
});
onMounted(() => { if (activeTab.value === 'overview') usePreset('6m'); });
onUnmounted(() => { requestId++; Object.values(charts).forEach(chart => chart.destroy()); });
</script>

<template>
  <div class="reports-page">
    <header class="page-heading"><div><p class="eyebrow">Phân tích</p><h1>Báo cáo kinh doanh</h1><p>Theo dõi doanh thu, đơn hàng và hiệu suất sản phẩm.</p></div><div v-if="activeTab === 'overview'" class="head-actions"><button class="btn btn-outline" :disabled="loading || !Object.keys(data).length" @click="exportCsv"><i class="bi bi-download"></i> Xuất CSV</button><button class="btn btn-outline" :disabled="loading" @click="refresh"><i class="bi bi-arrow-clockwise"></i> Làm mới</button></div></header>

    <nav class="report-tabs" role="tablist" aria-label="Báo cáo kinh doanh">
      <button id="report-overview-tab" :ref="el => tabRefs[0] = el" type="button" role="tab" aria-controls="report-overview-panel" :aria-selected="activeTab === 'overview'" :tabindex="activeTab === 'overview' ? 0 : -1" :class="{ active: activeTab === 'overview' }" @keydown="handleTabKeydown($event, 0)" @click="selectTab('overview')">Tổng quan</button>
      <button id="report-menu-tab" :ref="el => tabRefs[1] = el" type="button" role="tab" aria-controls="report-menu-panel" :aria-selected="activeTab === 'menu'" :tabindex="activeTab === 'menu' ? 0 : -1" :class="{ active: activeTab === 'menu' }" @keydown="handleTabKeydown($event, 1)" @click="selectTab('menu')">Hiệu quả món</button>
      <button id="report-expenses-tab" :ref="el => tabRefs[2] = el" type="button" role="tab" aria-controls="report-expenses-panel" :aria-selected="activeTab === 'expenses'" :tabindex="activeTab === 'expenses' ? 0 : -1" :class="{ active: activeTab === 'expenses' }" @keydown="handleTabKeydown($event, 2)" @click="selectTab('expenses')">Chi phí</button>
    </nav>

    <section v-if="activeTab === 'menu'" id="report-menu-panel" class="tab-panel" role="tabpanel" aria-labelledby="report-menu-tab"><InventoryReportsPage /></section>
    <section v-else-if="activeTab === 'expenses'" id="report-expenses-panel" class="tab-panel" role="tabpanel" aria-labelledby="report-expenses-tab"><OperatingExpensesPage /></section>
    <section v-else id="report-overview-panel" class="tab-panel" role="tabpanel" aria-labelledby="report-overview-tab">
    <section class="filter-panel" aria-label="Khoảng thời gian báo cáo">
      <div><span class="filter-label">Khoảng nhanh</span><div class="presets"><button v-for="item in [{v:'7d',l:'7 ngày'},{v:'30d',l:'30 ngày'},{v:'6m',l:'6 tháng'},{v:'1y',l:'1 năm'}]" :key="item.v" :class="{ active: !customActive && activePreset === item.v }" :aria-pressed="!customActive && activePreset === item.v" @click="usePreset(item.v)">{{ item.l }}</button></div></div>
      <div class="custom-dates"><label>Từ ngày<input v-model="customFrom" class="form-input" type="date" :max="customTo || today"></label><label>Đến ngày<input v-model="customTo" class="form-input" type="date" :min="customFrom || undefined" :max="today"></label><button class="btn btn-primary" :disabled="!!dateError || loading" @click="applyCustom">Áp dụng</button><p v-if="(customFrom || customTo) && dateError" role="alert">{{ dateError }}</p></div>
    </section>

    <div v-if="reportWarning" class="error-banner" role="alert"><i class="bi bi-exclamation-circle"></i><span><strong>Không thể cập nhật báo cáo kinh doanh</strong>Dữ liệu gần nhất vẫn được giữ nguyên. {{ reportWarning }}</span><button class="btn btn-outline" @click="refresh">Thử lại</button></div>
    <div v-if="financeWarning" class="error-banner" role="alert"><i class="bi bi-exclamation-circle"></i><span><strong>Không thể cập nhật báo cáo lợi nhuận</strong>Dữ liệu gần nhất vẫn được giữ nguyên. {{ financeWarning }}</span><button class="btn btn-outline" @click="refresh">Thử lại</button></div>
    <div v-if="loading && !Object.keys(data).length" class="loading-state"><span class="spinner"></span>Đang tải báo cáo...</div>

    <template v-if="Object.keys(data).length">
      <section class="stats" :aria-label="`Tổng quan ${periodLabel}`"><article class="orange"><i class="bi bi-basket"></i><div><small>Tiền món</small><strong>{{ formatPrice(data.itemRevenue || 0) }}</strong></div></article><article class="blue"><i class="bi bi-truck"></i><div><small>Phí giao hàng thu khách</small><strong>{{ formatPrice(data.shippingRevenue || 0) }}</strong></div></article><article class="red"><i class="bi bi-ticket-perforated"></i><div><small>Giảm giá</small><strong>{{ formatPrice(data.discountTotal || 0) }}</strong></div></article><article class="orange"><i class="bi bi-graph-up-arrow"></i><div><small>Doanh thu gộp</small><strong>{{ formatPrice(data.grossRevenue || 0) }}</strong></div></article><article class="red"><i class="bi bi-arrow-counterclockwise"></i><div><small>Đã hoàn tiền</small><strong>{{ formatPrice(data.refundTotal || 0) }}</strong><em class="sub">{{ Number(data.refundCount || 0).toLocaleString('vi-VN') }} đơn</em></div></article><article class="green"><i class="bi bi-bag-check"></i><div><small>Đơn hoàn tất cùng cohort</small><strong>{{ Number(data.operationalCompletedCount || 0).toLocaleString('vi-VN') }}</strong></div></article><article class="violet"><i class="bi bi-check2-circle"></i><div><small>Tỷ lệ hoàn tất</small><strong>{{ completionRate }}%</strong></div></article></section>
      <section class="finance-kpis" aria-label="Hiệu quả kinh doanh"><article><small>Giá vốn</small><strong>{{ data.cogs == null ? 'Chưa đủ dữ liệu' : formatPrice(data.cogs) }}</strong></article><article><small>Lợi nhuận gộp</small><strong>{{ data.grossProfit == null ? 'Chưa đủ dữ liệu' : formatPrice(data.grossProfit) }}</strong></article><article><small>Tỷ lệ giá vốn</small><strong>{{ data.foodCostPercent == null ? '—' : `${Number(data.foodCostPercent).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%` }}</strong></article><article><small>Biên lợi nhuận gộp</small><strong>{{ data.grossMarginPercent == null ? '—' : `${Number(data.grossMarginPercent).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%` }}</strong></article><article><small>Giá trị đơn trung bình</small><strong>{{ formatPrice(data.aov) }}</strong></article><p v-if="data.cogs == null" role="status">Chưa đủ dữ liệu giá vốn; lợi nhuận và tỷ lệ không được tính thành 0.</p></section>
      <section v-if="operatingProfit" class="finance-kpis" aria-label="Báo cáo lợi nhuận hoạt động"><article v-for="item in [['Doanh thu thuần','netRevenue'],['COGS','cogs'],['Lợi nhuận gộp','grossProfit'],['Chi phí vận hành','operatingExpenses'],['Lợi nhuận trước khấu hao (mô phỏng)','profitBeforeDepreciation'],['Khấu hao','depreciation'],['Lợi nhuận hoạt động','operatingProfit']]" :key="item[1]"><small>{{ item[0] }}</small><strong>{{ !operatingProfit.costComplete && ['cogs','grossProfit','profitBeforeDepreciation','operatingProfit'].includes(item[1]) ? 'Chưa đầy đủ' : formatPrice(operatingProfit[item[1]]) }}</strong></article><p v-if="!operatingProfit.costComplete" role="status">Chưa đầy đủ giá vốn: {{ operatingProfit.missingCostItemCount }} dòng thiếu dữ liệu, không tính thành 0.</p><p class="finance-note">Số liệu mô phỏng quản trị sinh viên, không phải kế toán thuế.</p></section>
      <section class="summary"><i class="bi bi-lightbulb"></i><p><strong>Tóm tắt {{ periodLabel }}:</strong> {{ Number(data.operationalOrderCount || 0).toLocaleString('vi-VN') }} đơn phát sinh, {{ Number(data.operationalCompletedCount || 0).toLocaleString('vi-VN') }} đơn trong cùng cohort đã giao thành công.<span v-if="topProduct"> Sản phẩm dẫn đầu là <b>{{ topProduct.name }}</b> với {{ topProduct.sold }} sản phẩm.</span></p></section>

      <section class="charts-grid">
        <article class="chart-card"><header><div><h2>Xu hướng doanh thu</h2><p>Gross theo ngày giao; refund theo ngày xử lý; doanh thu thuần là phần còn lại sau hoàn tiền</p></div><i class="bi bi-graph-up"></i></header><div v-if="has('revenueByDay')" class="chart"><canvas :ref="el => refs.revenueDay.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-bar-chart"></i><strong>Chưa có doanh thu theo ngày</strong><span>Không có đơn DELIVERED và PAID trong kỳ.</span></div></article>
        <article class="chart-card"><header><div><h2>Doanh thu theo tháng</h2><p>Doanh thu gộp, hoàn tiền và doanh thu thuần theo MM/YYYY</p></div><i class="bi bi-calendar3"></i></header><div v-if="has('revenueByMonth')" class="chart"><canvas :ref="el => refs.revenueMonth.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-calendar-x"></i><strong>Chưa có dữ liệu theo tháng</strong></div></article>
        <article class="chart-card"><header><div><h2>Trạng thái đơn hàng</h2><p>Toàn bộ đơn phát sinh trong kỳ</p></div><i class="bi bi-pie-chart"></i></header><div v-if="has('ordersByStatus')" class="chart"><canvas :ref="el => refs.orderStatus.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-inbox"></i><strong>Chưa có đơn hàng</strong></div></article>
        <article class="chart-card"><header><div><h2>Sản phẩm bán chạy</h2><p>Xếp theo doanh thu tiền món; tooltip có số lượng</p></div><i class="bi bi-trophy"></i></header><div v-if="has('topProducts')" class="chart"><canvas :ref="el => refs.topProducts.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-basket"></i><strong>Chưa có sản phẩm bán ra</strong></div></article>
        <article class="chart-card"><header><div><h2>Doanh thu theo danh mục</h2><p>Xếp giảm dần theo doanh thu tiền món</p></div><i class="bi bi-grid"></i></header><div v-if="has('revenueByCategory')" class="chart"><canvas :ref="el => refs.revenueCategory.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-grid"></i><strong>Chưa có doanh thu danh mục</strong></div></article>
        <article class="chart-card"><header><div><h2>Phương thức thanh toán</h2><p>Tỷ trọng đơn thành công và doanh thu</p></div><i class="bi bi-credit-card"></i></header><div v-if="has('paymentMethodStats')" class="chart"><canvas :ref="el => refs.paymentMethod.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-credit-card-2-front"></i><strong>Chưa có thanh toán thành công</strong></div></article>
        <article class="chart-card chart-wide"><header><div><h2>Doanh thu theo giờ</h2><p>Khung giờ tạo doanh thu từ đơn đã giao</p></div><i class="bi bi-clock"></i></header><div v-if="has('revenueByHour')" class="chart"><canvas :ref="el => refs.revenueHour.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-clock-history"></i><strong>Chưa có dữ liệu theo giờ</strong></div></article>
        <article class="chart-card chart-wide"><header><div><h2>Hiệu suất theo thứ</h2><p>Đơn phát sinh và hoàn tất cùng cohort</p></div><i class="bi bi-calendar-week"></i></header><div v-if="has('performanceByWeekday')" class="chart"><canvas :ref="el => refs.weekday.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-calendar-x"></i><strong>Chưa có dữ liệu theo thứ</strong></div></article>
        <article class="chart-card chart-half"><header><div><h2>Xu hướng hoàn tiền</h2><p>Sự kiện hoàn tiền theo ngày xử lý</p></div><i class="bi bi-arrow-counterclockwise"></i></header><div v-if="has('refundTrend')" class="chart"><canvas :ref="el => refs.refundTrend.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-arrow-counterclockwise"></i><strong>Không phát sinh hoàn tiền</strong></div></article>
        <article class="chart-card chart-half"><header><div><h2>Lý do ngoại lệ</h2><p>Đơn hủy, giao thất bại hoặc hoàn kho</p></div><i class="bi bi-exclamation-diamond"></i></header><div v-if="has('exceptionReasons')" class="chart"><canvas :ref="el => refs.exceptionReasons.value = el"></canvas></div><div v-else class="empty"><i class="bi bi-check2-circle"></i><strong>Không có ngoại lệ trong kỳ</strong></div></article>
      </section>

      <section class="top-table"><header><div><h2>Chi tiết sản phẩm bán chạy</h2><p>Xếp hạng theo số lượng bán trong {{ periodLabel }}</p></div><span>{{ data.topProducts?.length || 0 }} sản phẩm</span></header><div v-if="has('topProducts')" class="table-wrapper"><table class="table"><thead><tr><th>Hạng</th><th>Sản phẩm</th><th>Số lượng bán</th><th>Doanh thu</th><th>Tỷ trọng</th></tr></thead><tbody><tr v-for="(product, index) in data.topProducts" :key="product.name"><td><b class="rank" :class="`rank-${index + 1}`">{{ index + 1 }}</b></td><td><strong>{{ product.name }}</strong></td><td>{{ Number(product.sold).toLocaleString('vi-VN') }}</td><td><strong>{{ formatPrice(product.revenue || 0) }}</strong></td><td>{{ data.itemRevenue ? (Number(product.revenue || 0) * 100 / data.itemRevenue).toFixed(1) : 0 }}%</td></tr></tbody></table></div><div v-else class="empty"><i class="bi bi-table"></i><strong>Chưa có dữ liệu xếp hạng</strong></div></section>
    </template>
    </section>
  </div>
</template>

<style scoped>
.reports-page { display: grid; gap: 20px; }.report-tabs{display:flex;gap:4px;width:max-content;max-width:100%;padding:4px;border:1px solid var(--border-light);border-radius:12px;background:#fff}.report-tabs button{min-height:40px;padding:8px 16px;border-radius:8px;color:var(--text-mid);font-weight:700}.report-tabs button.active{background:var(--primary);color:#fff}.report-tabs button:focus-visible{outline:3px solid var(--primary);outline-offset:2px}.page-heading { align-items: flex-end; display: flex; justify-content: space-between; gap: 16px; }.page-heading h1 { font-size: 28px; margin: 2px 0 4px; }.page-heading p { color: var(--text-mid); font-size: 14px; }.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }.head-actions { display: flex; gap: 9px; }
.filter-panel { align-items: end; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-xs); display: flex; flex-wrap: wrap; gap: 22px; padding: 16px; }.filter-label,.custom-dates label { color: var(--text-mid); display: block; font-size: 11px; font-weight: 700; margin-bottom: 6px; }.presets { background: var(--surface); border-radius: 9px; display: flex; padding: 3px; }.presets button { border-radius: 7px; color: var(--text-mid); font-size: 12px; padding: 9px 13px; }.presets button.active { background: var(--white); box-shadow: var(--shadow-xs); color: var(--role-admin); font-weight: 700; }.custom-dates { align-items: end; display: flex; flex: 1; flex-wrap: wrap; gap: 9px; }.custom-dates label { margin: 0; }.custom-dates input { margin-top: 6px; width: 155px; }.custom-dates p { color: var(--red-active); flex-basis: 100%; font-size: 11px; margin: 0; }
.error-banner { align-items: center; background: #fef2f2; border: 1px solid #fecaca; border-radius: var(--radius); color: #b91c1c; display: flex; gap: 12px; padding: 13px 16px; }.error-banner > i { font-size: 22px; }.error-banner span { display: grid; flex: 1; font-size: 12px; }.loading-state { align-items: center; color: var(--text-mid); display: flex; gap: 10px; justify-content: center; min-height: 280px; }
.stats { display: grid; gap: 14px; grid-template-columns: repeat(3,minmax(0,1fr)); }.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); display: flex; gap: 13px; padding: 18px; }.stats article > i { align-items: center; border-radius: 11px; display: flex; flex: 0 0 44px; font-size: 20px; height: 44px; justify-content: center; }.stats small { color: var(--text-mid); display: block; font-size: 11px; margin-bottom: 3px; }.stats strong { font-size: 19px; }.stats .sub { color: var(--text-mid); display: block; font-size: 11px; margin-top: 2px; }.stats .orange i { color: #c2410c; background: #ffedd5; }.stats .green i { color: #047857; background: #d1fae5; }.stats .blue i { color: #1d4ed8; background: #dbeafe; }.stats .violet i { color: #7c3aed; background: #ede9fe; }.stats .teal i { color: #0f766e; background: #ccfbf1; }.stats .red i { color: #b91c1c; background: #fee2e2; }
.finance-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.finance-kpis article{display:grid;gap:5px;padding:16px;border:1px solid var(--border);border-radius:var(--radius);background:#fff}.finance-kpis small{color:var(--text-mid)}.finance-kpis>p{grid-column:1/-1;margin:0;padding:12px;border-radius:8px;background:#fff7ed;color:#9a3412}.finance-note{background:#f5f5f4!important;color:var(--text-mid)!important}
.summary { align-items: center; background: #fff7ed; border: 1px solid #fed7aa; border-radius: var(--radius); color: #9a3412; display: flex; gap: 12px; padding: 13px 16px; }.summary > i { font-size: 20px; }.summary p { font-size: 13px; margin: 0; }
.charts-grid { display: grid; gap: 16px; grid-template-columns:repeat(12,minmax(0,1fr)); }
.chart-card { grid-column: span 6; }
.chart-card:first-child { grid-column: span 8; }
.chart-card:nth-child(2) { grid-column: span 4; }
.chart-wide { grid-column: span 6; }
.chart-half { grid-column: span 6; }.chart-card,.top-table { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-xs); overflow: hidden; }.chart-card header,.top-table header { align-items: center; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; padding: 15px 17px; }.chart-card h2,.top-table h2 { font-size: 14px; margin: 0 0 3px; }.chart-card header p,.top-table header p { color: var(--text-mid); font-size: 11px; margin: 0; }.chart-card header > i { color: var(--role-admin); font-size: 18px; }.chart { height: 285px; padding: 14px; }.empty { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 7px; justify-content: center; min-height: 285px; padding: 25px; text-align: center; }.empty i { color: var(--text-light); font-size: 32px; }.empty span { font-size: 11px; }.top-table header > span { background: var(--role-admin-soft); border-radius: 20px; color: var(--role-admin); font-size: 11px; font-weight: 700; padding: 5px 9px; }.top-table .table { min-width: 680px; }.top-table th { color: var(--text-mid); font-size: 10px; letter-spacing: .05em; text-transform: uppercase; }.rank { align-items: center; background: var(--surface); border-radius: 50%; display: inline-flex; height: 27px; justify-content: center; width: 27px; }.rank-1 { background: #fef3c7; color: #b45309; }.rank-2 { background: #f3f4f6; color: #4b5563; }.rank-3 { background: #ffedd5; color: #c2410c; }
@media (max-width: 1050px) { .stats { grid-template-columns: repeat(2,1fr); } }@media (max-width: 720px) { .page-heading { align-items: flex-start; flex-direction: column; }.filter-panel { align-items: stretch; }.custom-dates label { flex: 1; }.custom-dates input { width: 100%; }.charts-grid { grid-template-columns: 1fr; }.chart-card,.chart-card:first-child,.chart-card:nth-child(2),.chart-wide,.chart-half{grid-column:1/-1}.stats { grid-template-columns: 1fr; }.presets { overflow-x: auto; }.error-banner { align-items: flex-start; flex-wrap: wrap; } }
</style>
