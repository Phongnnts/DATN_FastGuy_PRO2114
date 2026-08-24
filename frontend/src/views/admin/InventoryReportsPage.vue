<script setup>
import { computed, onMounted, ref } from 'vue';
import { adminApi } from '@/api';
import { dateRangeForDays } from '@/utils/inventoryOperations';
import { loadInventoryReports } from '@/utils/inventoryReports';

const range = ref(dateRangeForDays(30));
const reports = ref({
  summary: { data: null, error: '' },
  itemLoss: { data: null, error: '' },
  menuCost: { data: null, error: '' },
});
const loading = ref(false);
const dateError = computed(() => range.value.fromDate > range.value.toDate ? 'Từ ngày không được sau đến ngày.' : '');
const summary = computed(() => reports.value.summary.data);
const cards = computed(() => summary.value ? [
  ['Giá trị nhập', summary.value.purchaseCost, 'bi-box-arrow-in-down'],
  ['Giá vốn tiêu thụ', summary.value.consumptionCost, 'bi-basket'],
  ['Hao hụt', summary.value.wasteCost, 'bi-trash3'],
  ['Lỗ kiểm kê', summary.value.stockCountLossCost, 'bi-arrow-down'],
  ['Tăng kiểm kê', summary.value.stockCountGainCost, 'bi-arrow-up'],
  ['Tổng thất thoát', summary.value.totalLossCost, 'bi-exclamation-triangle'],
  ['Giá trị tồn cuối kỳ', summary.value.endingInventoryValue, 'bi-boxes'],
] : []);

function money(value) {
  return Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
}
function quantity(value) {
  return Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 3 });
}
function preset(days) {
  range.value = dateRangeForDays(days);
  load();
}
async function load() {
  if (dateError.value) return;
  loading.value = true;
  reports.value = await loadInventoryReports({
    summary: () => adminApi.getInventoryCostSummary(range.value),
    itemLoss: () => adminApi.getInventoryItemLoss(range.value),
    menuCost: () => adminApi.getInventoryMenuCost(),
  });
  loading.value = false;
}

onMounted(load);
</script>

<template>
  <main class="report-page">
    <header class="page-heading"><div><p class="eyebrow">Tồn kho</p><h1>Báo cáo giá trị kho</h1><p>Giá vốn, thất thoát và giá trị tồn theo khoảng ngày.</p></div></header>
    <section class="panel filters">
      <div class="presets"><button @click="preset(1)">Hôm nay</button><button @click="preset(7)">7 ngày</button><button @click="preset(30)">30 ngày</button></div>
      <label>Từ ngày<input v-model="range.fromDate" class="form-input" type="date" :max="range.toDate" /></label>
      <label>Đến ngày<input v-model="range.toDate" class="form-input" type="date" :min="range.fromDate" /></label>
      <button class="btn btn-primary" :disabled="loading || !!dateError" @click="load">Xem báo cáo</button>
      <p v-if="dateError" role="alert">{{ dateError }}</p>
    </section>

    <section aria-labelledby="summary-title">
      <h2 id="summary-title">Tổng quan giá trị kho</h2>
      <div v-if="loading" class="panel state" role="status">Đang tổng hợp báo cáo...</div>
      <div v-else-if="reports.summary.error" class="panel state error" role="alert">{{ reports.summary.error }}<button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!summary" class="panel state">Chưa có dữ liệu báo cáo.</div>
      <template v-else>
        <section class="kpi-grid" aria-label="Chỉ số giá trị kho"><article v-for="card in cards" :key="card[0]"><i class="bi" :class="card[2]" aria-hidden="true"></i><span>{{ card[0] }}</span><strong>{{ money(card[1]) }}</strong></article></section>
        <section class="loss-card"><div><span>Tỷ lệ thất thoát</span><strong>{{ Number(summary.lossRate).toLocaleString('vi-VN', { maximumFractionDigits: 2 }) }}%</strong></div><div class="loss-track" aria-hidden="true"><span :style="{ width: `${Math.min(100, Number(summary.lossRate))}%` }"></span></div></section>
      </template>
    </section>

    <section class="panel report-section" aria-labelledby="item-loss-title">
      <h2 id="item-loss-title">Thất thoát theo mặt hàng</h2>
      <div v-if="loading" class="state" role="status">Đang tải thất thoát...</div>
      <div v-else-if="reports.itemLoss.error" class="state error" role="alert">{{ reports.itemLoss.error }}<button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!reports.itemLoss.data?.length" class="state">Không có thất thoát trong khoảng ngày này.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Mã</th><th>Mặt hàng</th><th>SL hao hụt</th><th>Tiền hao hụt</th><th>SL lỗ kiểm kê</th><th>Tiền lỗ kiểm kê</th><th>Tổng thất thoát</th></tr></thead><tbody><tr v-for="item in reports.itemLoss.data" :key="item.inventoryItemId"><td data-label="Mã">{{ item.inventoryCode }}</td><td data-label="Mặt hàng"><strong>{{ item.name }}</strong></td><td data-label="SL hao hụt">{{ quantity(item.wasteQuantity) }}</td><td data-label="Tiền hao hụt">{{ money(item.wasteCost) }}</td><td data-label="SL lỗ kiểm kê">{{ quantity(item.stockCountLossQuantity) }}</td><td data-label="Tiền lỗ kiểm kê">{{ money(item.stockCountLossCost) }}</td><td data-label="Tổng thất thoát"><strong>{{ money(item.totalLossCost) }}</strong></td></tr></tbody></table></div>
    </section>

    <section class="panel report-section" aria-labelledby="menu-cost-title">
      <h2 id="menu-cost-title">Giá vốn công thức món</h2>
      <div v-if="loading" class="state" role="status">Đang tải giá vốn món...</div>
      <div v-else-if="reports.menuCost.error" class="state error" role="alert">{{ reports.menuCost.error }}<button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!reports.menuCost.data?.length" class="state">Chưa có dữ liệu giá vốn công thức.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>SKU</th><th>Biến thể</th><th>Định lượng</th><th>Giá công thức</th><th>Giá mỗi suất</th></tr></thead><tbody><tr v-for="item in reports.menuCost.data" :key="item.variantId"><td data-label="SKU">{{ item.sku || '—' }}</td><td data-label="Biến thể"><strong>{{ item.variantName || '—' }}</strong></td><td data-label="Định lượng">{{ quantity(item.yieldQuantity) }}</td><td data-label="Giá công thức">{{ money(item.recipeCost) }}</td><td data-label="Giá mỗi suất"><strong>{{ money(item.costPerServing) }}</strong></td></tr></tbody></table></div>
    </section>
  </main>
</template>

<style scoped>
.report-page{display:grid;gap:20px}.page-heading h1{margin:2px 0;font-size:28px}.page-heading p{margin:0;color:var(--text-mid)}.eyebrow{color:var(--role-admin)!important;font-size:11px!important;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.panel,.loss-card{background:#fff;border:1px solid var(--border-light);border-radius:14px;padding:20px}.filters{display:flex;align-items:end;gap:12px;flex-wrap:wrap}.filters label{display:grid;gap:6px;color:var(--text-mid);font-size:12px;font-weight:700}.filters p{width:100%;margin:0;color:#b91c1c}.presets{display:flex;padding:3px;background:var(--surface);border-radius:8px}.presets button{padding:9px;border-radius:6px}.presets button:hover{background:#fff}.state{text-align:center;color:var(--text-mid);display:grid;justify-items:center;gap:10px}.error{color:#b91c1c}.report-page h2{margin:0 0 12px;font-size:18px}.kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px}.kpi-grid article{display:grid;grid-template-columns:auto 1fr;gap:4px 12px;background:#fff;border:1px solid var(--border-light);border-radius:12px;padding:18px}.kpi-grid i{grid-row:1/3;color:var(--primary);font-size:24px}.kpi-grid span{color:var(--text-mid);font-size:12px}.kpi-grid strong{font-size:18px}.loss-card{margin-top:14px}.loss-card>div:first-child{display:flex;justify-content:space-between}.loss-track{height:10px;margin-top:12px;background:var(--surface);border-radius:99px;overflow:hidden}.loss-track span{display:block;height:100%;background:#dc2626}.report-section{overflow:hidden}.table-wrapper{overflow-x:auto}.table{min-width:760px}.table th,.table td{white-space:nowrap}.table th:nth-child(2),.table td:nth-child(2){white-space:normal}
@media(max-width:600px){.filters,.filters label,.filters .btn{width:100%}.presets button{flex:1}.presets{width:100%}.report-section{padding:16px}.table-wrapper{overflow:visible}.table{min-width:0}.table thead{display:none}.table tbody{display:grid;gap:10px}.table tbody tr{display:grid;gap:7px;padding:12px;border:1px solid var(--border-light);border-radius:10px}.table td{display:flex;justify-content:space-between;gap:16px;padding:2px 0;border:0;text-align:right;white-space:normal}.table td::before{color:var(--text-mid);content:attr(data-label);font-size:11px;text-align:left}.table td:nth-child(2){text-align:right}}
</style>
