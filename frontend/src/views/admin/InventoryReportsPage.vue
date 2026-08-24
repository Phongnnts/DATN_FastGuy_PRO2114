<script setup>
import { computed, onMounted, ref } from 'vue';
import { adminApi } from '@/api';
import { dateRangeForDays } from '@/utils/inventoryOperations';

const range = ref(dateRangeForDays(30));
const report = ref(null);
const loading = ref(false);
const error = ref('');
const dateError = computed(() => range.value.fromDate > range.value.toDate ? 'Từ ngày không được sau đến ngày.' : '');
const cards = computed(() => report.value ? [
  ['Doanh thu sau giảm', report.value.netRevenue],
  ['Giá vốn', report.value.costComplete ? report.value.cost : 'Chưa đủ dữ liệu'],
  ['Lãi gộp', report.value.costComplete ? report.value.grossProfit : 'Chưa đủ dữ liệu'],
  ['Food cost', report.value.costComplete ? `${percent(report.value.foodCostPercent)}%` : '—'],
  ['Biên lãi gộp', report.value.costComplete ? `${percent(report.value.grossMarginPercent)}%` : '—'],
] : []);

function money(value) { return Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }); }
function percent(value) { return Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 1 }); }
function preset(days) { range.value = dateRangeForDays(days); load(); }
async function load() {
  if (dateError.value) return;
  loading.value = true; error.value = '';
  try { report.value = await adminApi.getMenuPerformanceReport(range.value); }
  catch (exception) { error.value = exception.message || 'Không thể tải báo cáo theo món'; }
  finally { loading.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="report-page">
    <header class="page-heading"><div><p class="eyebrow">Kinh doanh</p><h1>Báo cáo hiệu quả món ăn</h1><p>Đơn đã giao, tính theo ngày giao. Giá vốn dùng snapshot tại thời điểm đặt món.</p></div></header>
    <section class="panel filters" aria-label="Khoảng thời gian báo cáo">
      <div class="presets"><button type="button" @click="preset(1)">Hôm nay</button><button type="button" @click="preset(7)">7 ngày</button><button type="button" @click="preset(30)">30 ngày</button></div>
      <label for="report-from">Từ ngày<input id="report-from" v-model="range.fromDate" class="form-input" type="date" :max="range.toDate" /></label>
      <label for="report-to">Đến ngày<input id="report-to" v-model="range.toDate" class="form-input" type="date" :min="range.fromDate" /></label>
      <button class="btn btn-primary" :disabled="loading || !!dateError" @click="load">Xem báo cáo</button>
      <p v-if="dateError" role="alert">{{ dateError }}</p>
    </section>

    <div v-if="loading" class="panel state" role="status">Đang tổng hợp doanh thu và giá vốn...</div>
    <div v-else-if="error" class="panel state error" role="alert">{{ error }}<button class="btn btn-outline" @click="load">Thử lại</button></div>
    <template v-else-if="report">
      <section class="kpi-grid" aria-label="Tổng quan hiệu quả món ăn"><article v-for="card in cards" :key="card[0]"><span>{{ card[0] }}</span><strong>{{ typeof card[1] === 'string' ? card[1] : money(card[1]) }}</strong></article></section>
      <aside v-if="!report.costComplete" class="warning" role="status"><strong>Chưa đủ dữ liệu giá vốn.</strong> {{ report.missingCostItemCount }} dòng đơn hàng lịch sử chưa có snapshot; các dòng này không bị tính thành 0.</aside>
      <section class="panel report-section" aria-labelledby="menu-report-title">
        <div class="section-heading"><div><h2 id="menu-report-title">Chi tiết theo món</h2><p>Doanh thu sau giảm được phân bổ theo tỷ trọng thành tiền từng dòng.</p></div></div>
        <div v-if="!report.items?.length" class="state">Không có đơn đã giao trong khoảng ngày này.</div>
        <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Món / variant</th><th>Đã bán</th><th>Doanh thu gộp</th><th>Giảm giá</th><th>Doanh thu sau giảm</th><th>Giá vốn</th><th>Lãi gộp</th><th>Food cost</th><th>Biên lãi gộp</th></tr></thead><tbody><tr v-for="item in report.items" :key="item.variantId ?? `${item.productName}-${item.variantName}`"><th scope="row" data-label="Món / variant"><strong>{{ item.productName }}</strong><small>{{ item.variantName || 'Mặc định' }}</small></th><td data-label="Đã bán">{{ item.quantitySold }}</td><td data-label="Doanh thu gộp">{{ money(item.grossRevenue) }}</td><td data-label="Giảm giá">{{ money(item.allocatedDiscount) }}</td><td data-label="Doanh thu sau giảm"><strong>{{ money(item.netRevenue) }}</strong></td><td data-label="Giá vốn"><span v-if="item.costComplete">{{ money(item.cost) }}</span><span v-else class="missing">Chưa đủ dữ liệu</span></td><td data-label="Lãi gộp">{{ item.costComplete ? money(item.grossProfit) : '—' }}</td><td data-label="Food cost">{{ item.costComplete ? `${percent(item.foodCostPercent)}%` : '—' }}</td><td data-label="Biên lãi gộp">{{ item.costComplete ? `${percent(item.grossMarginPercent)}%` : '—' }}</td></tr></tbody></table></div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.report-page{display:grid;gap:20px}.page-heading h1{margin:2px 0;font-size:28px}.page-heading p,.section-heading p{margin:0;color:var(--text-mid)}.eyebrow{color:var(--role-admin)!important;font-size:11px!important;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.panel{background:#fff;border:1px solid var(--border-light);border-radius:14px;padding:20px}.filters{display:flex;align-items:end;gap:12px;flex-wrap:wrap}.filters label{display:grid;gap:6px;color:var(--text-mid);font-size:12px;font-weight:700}.filters>p{width:100%;margin:0;color:#b91c1c}.presets{display:flex;padding:3px;background:var(--surface);border-radius:8px}.presets button{min-height:40px;padding:9px;border-radius:6px}.presets button:hover{background:#fff}.state{text-align:center;color:var(--text-mid);display:grid;justify-items:center;gap:10px}.error,.missing{color:#b91c1c}.kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px}.kpi-grid article{display:grid;gap:6px;padding:18px;background:#fff;border:1px solid var(--border-light);border-radius:12px}.kpi-grid span{color:var(--text-mid);font-size:12px}.kpi-grid strong{font-size:20px}.warning{padding:14px 16px;border:1px solid #fed7aa;border-radius:10px;background:#fff7ed;color:#9a3412}.report-section{overflow:hidden}.section-heading h2{margin:0 0 4px}.table-wrapper{overflow-x:auto}.table{min-width:1180px}.table th,.table td{white-space:nowrap}.table th:first-child{white-space:normal}.table small{display:block;margin-top:3px;color:var(--text-mid)}
@media(max-width:700px){.filters,.filters label,.filters .btn{width:100%}.presets{width:100%}.presets button{flex:1}.report-section{padding:14px}.table-wrapper{overflow:visible}.table{min-width:0}.table thead{display:none}.table tbody{display:grid;gap:12px}.table tbody tr{display:grid;gap:7px;padding:14px;border:1px solid var(--border-light);border-radius:10px}.table td,.table tbody th{display:flex;justify-content:space-between;gap:16px;padding:2px 0;border:0;text-align:right;white-space:normal}.table :is(td,th)::before{content:attr(data-label);color:var(--text-mid);font-size:11px;text-align:left}}
</style>
