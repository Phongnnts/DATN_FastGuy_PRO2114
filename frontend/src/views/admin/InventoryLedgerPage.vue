<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { adminApi } from '@/api';

const TRANSACTION_TYPES = ['RESERVE', 'RELEASE', 'CONSUME', 'WASTE'];
const TYPE_LABELS = { RESERVE: 'Giữ chỗ', RELEASE: 'Trả lại', CONSUME: 'Tiêu thụ', WASTE: 'Hao hụt' };
const SIZE_OPTIONS = [20, 50, 100];

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const variantId = ref('');
const productId = ref('');
const transactionType = ref('');
const fromDate = ref('');
const toDate = ref('');
const loading = ref(false);
const loadError = ref('');

const dateError = computed(() =>
  fromDate.value && toDate.value && fromDate.value > toDate.value ? 'Từ ngày không được sau đến ngày.' : ''
);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
const rangeStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * size.value + 1));
const rangeEnd = computed(() => Math.min(page.value * size.value, total.value));

const kpi = computed(() => {
  const counts = { RESERVE: 0, RELEASE: 0, CONSUME: 0, WASTE: 0 };
  rows.value.forEach((row) => {
    if (counts[row.type] !== undefined) counts[row.type] += 1;
  });
  return counts;
});

function typeClass(type) {
  return { RESERVE: 'badge-info', RELEASE: 'badge-success', CONSUME: 'badge-warning', WASTE: 'badge-danger' }[type] || 'badge-secondary';
}

function buildParams() {
  const params = { page: page.value, size: size.value };
  if (variantId.value.trim()) params.variantId = variantId.value.trim();
  if (productId.value.trim()) params.productId = productId.value.trim();
  if (transactionType.value) params.transactionType = transactionType.value;
  if (fromDate.value) params.fromDate = fromDate.value;
  if (toDate.value) params.toDate = toDate.value;
  return params;
}

let loadGeneration = 0;
let stopped = false;
let clampingPage = false;

async function load() {
  if (dateError.value) return;
  const request = { generation: ++loadGeneration, params: buildParams() };
  loading.value = true;
  loadError.value = '';
  try {
    const data = await adminApi.getInventoryTransactions(request.params);
    if (stopped || request.generation !== loadGeneration) return;
    rows.value = Array.isArray(data?.items) ? data.items : [];
    total.value = Number(data?.total) || 0;
    if (page.value > totalPages.value) {
      if (clampingPage) return;
      clampingPage = true;
      page.value = totalPages.value;
      load();
    }
  } catch (e) {
    if (stopped || request.generation !== loadGeneration) return;
    loadError.value = e.message || 'Không thể tải sổ tồn kho.';
  } finally {
    if (request.generation === loadGeneration) {
      loading.value = false;
      clampingPage = false;
    }
  }
}

function applyFilters() {
  page.value = 1;
  load();
}

function changeSize() {
  page.value = 1;
  load();
}

function goTo(target) {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
  load();
}

function resetFilters() {
  variantId.value = '';
  productId.value = '';
  transactionType.value = '';
  fromDate.value = '';
  toDate.value = '';
  applyFilters();
}

function localDate(daysAgo = 0) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  return date.toLocaleDateString('en-CA');
}

function setDatePreset(days) {
  fromDate.value = days === 0 ? localDate() : localDate(days - 1);
  toDate.value = localDate();
  applyFilters();
}

onMounted(load);
onBeforeUnmount(() => {
  stopped = true;
});
</script>

<template>
  <main class="ledger-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">Vận hành</p>
        <h1>Sổ tồn kho</h1>
        <p>Tra cứu dòng biến động tồn kho theo giao dịch.</p>
      </div>
      <button class="btn btn-outline" :disabled="loading" @click="load">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Làm mới
      </button>
    </header>

    <section class="stats" aria-label="Tổng quan giao dịch tồn kho">
      <article>
        <span class="stat-icon slate"><i class="bi bi-receipt" aria-hidden="true"></i></span>
        <div><small>Tổng giao dịch</small><strong>{{ rows.length }}</strong></div>
      </article>
      <article>
        <span class="stat-icon blue"><i class="bi bi-cart-plus" aria-hidden="true"></i></span>
        <div><small>{{ TYPE_LABELS.RESERVE }}</small><strong>{{ kpi.RESERVE }}</strong></div>
      </article>
      <article>
        <span class="stat-icon green"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i></span>
        <div><small>{{ TYPE_LABELS.RELEASE }}</small><strong>{{ kpi.RELEASE }}</strong></div>
      </article>
      <article>
        <span class="stat-icon amber"><i class="bi bi-fire" aria-hidden="true"></i></span>
        <div><small>{{ TYPE_LABELS.CONSUME }}</small><strong>{{ kpi.CONSUME }}</strong></div>
      </article>
      <article>
        <span class="stat-icon red"><i class="bi bi-trash" aria-hidden="true"></i></span>
        <div><small>{{ TYPE_LABELS.WASTE }}</small><strong>{{ kpi.WASTE }}</strong></div>
      </article>
    </section>
    <p class="kpi-hint"><i class="bi bi-info-circle" aria-hidden="true"></i> KPI tính theo dữ liệu trang hiện tại.</p>

    <section class="panel">
      <div class="filter-area">
        <label><span class="form-label">Mã biến thể</span><input v-model="variantId" class="form-input" type="text" inputmode="numeric" placeholder="Ví dụ: 12" @keydown.enter="applyFilters" /></label>
        <label><span class="form-label">Mã sản phẩm</span><input v-model="productId" class="form-input" type="text" inputmode="numeric" placeholder="Ví dụ: 5" @keydown.enter="applyFilters" /></label>
        <label><span class="form-label">Loại giao dịch</span>
          <select v-model="transactionType" class="form-select">
            <option value="">Tất cả loại</option>
            <option v-for="t in TRANSACTION_TYPES" :key="t" :value="t">{{ TYPE_LABELS[t] }} ({{ t }})</option>
          </select>
        </label>
        <div class="presets">
          <button type="button" @click="setDatePreset(0)">Hôm nay</button>
          <button type="button" @click="setDatePreset(7)">7 ngày</button>
          <button type="button" @click="setDatePreset(30)">30 ngày</button>
        </div>
        <label><span class="form-label">Từ ngày</span><input v-model="fromDate" type="date" class="form-input" :max="toDate || undefined" /></label>
        <label><span class="form-label">Đến ngày</span><input v-model="toDate" type="date" class="form-input" :min="fromDate || undefined" /></label>
        <button class="btn btn-primary" :disabled="!!dateError || loading" @click="applyFilters">Áp dụng</button>
        <button class="btn btn-outline" @click="resetFilters"><i class="bi bi-x-circle" aria-hidden="true"></i> Đặt lại</button>
        <p v-if="dateError" class="field-error" role="alert">{{ dateError }}</p>
      </div>

      <div v-if="loading" class="state" role="status"><span class="spinner" aria-hidden="true"></span><strong>Đang tải sổ tồn kho...</strong></div>
      <div v-else-if="loadError" class="state error" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!rows.length" class="state"><i class="bi bi-inbox" aria-hidden="true"></i><strong>Không có giao dịch</strong><span>Thử thay đổi hoặc đặt lại bộ lọc.</span><button class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th scope="col">Thời gian</th><th scope="col">Loại</th><th scope="col">Số lượng</th><th scope="col">Biến thể</th><th scope="col">Sản phẩm</th><th scope="col">Đơn hàng</th></tr></thead>
            <tbody>
              <tr v-for="row in rows" :key="row.transactionId">
                <td data-label="Thời gian"><time :datetime="row.createdAt">{{ row.createdAt }}</time></td>
                <td data-label="Loại"><span class="badge" :class="typeClass(row.type)">{{ TYPE_LABELS[row.type] || row.type }}</span></td>
                <td data-label="Số lượng">{{ row.quantity }}</td>
                <td data-label="Biến thể"><strong>{{ row.variantName || '—' }}</strong><small v-if="row.variantId" class="sub">ID: {{ row.variantId }}</small></td>
                <td data-label="Sản phẩm"><strong>{{ row.productName || '—' }}</strong><small v-if="row.productId" class="sub">ID: {{ row.productId }}</small></td>
                <td data-label="Đơn hàng"><router-link v-if="row.orderId" class="order-link" :to="`/admin/orders/${row.orderId}`">{{ row.orderCode || row.orderId }}</router-link><span v-else class="muted">—</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer class="table-footer">
          <span>Hiển thị {{ rangeStart }}–{{ rangeEnd }} / {{ total }} giao dịch</span>
          <div class="footer-right">
            <label class="size-label">Mỗi trang <select v-model.number="size" class="form-select" @change="changeSize"><option v-for="s in SIZE_OPTIONS" :key="s" :value="s">{{ s }}</option></select></label>
            <div class="pagination">
              <button :disabled="page === 1" aria-label="Trang trước" @click="goTo(page - 1)"><i class="bi bi-chevron-left" aria-hidden="true"></i></button>
              <span>Trang {{ page }} / {{ totalPages }}</span>
              <button :disabled="page === totalPages" aria-label="Trang sau" @click="goTo(page + 1)"><i class="bi bi-chevron-right" aria-hidden="true"></i></button>
            </div>
          </div>
        </footer>
      </template>
    </section>
  </main>
</template>

<style scoped>
.ledger-page { display: grid; gap: 24px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-heading h1 { font-size: 28px; line-height: 1.25; margin: 2px 0 4px; }
.page-heading p { color: var(--text-mid); font-size: 14px; }
.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.stats { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; }
.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-xs); display: flex; gap: 14px; padding: 18px; }
.stat-icon { align-items: center; border-radius: 10px; display: inline-flex; flex: 0 0 42px; height: 42px; justify-content: center; font-size: 19px; }
.stat-icon.slate { color: #1f2937; background: #e5e7eb; }
.stat-icon.blue { color: #1d4ed8; background: #dbeafe; }
.stat-icon.green { color: #047857; background: #d1fae5; }
.stat-icon.amber { color: #b45309; background: #fef3c7; }
.stat-icon.red { color: #b91c1c; background: #fee2e2; }
.stats small { color: var(--text-mid); display: block; font-size: 12px; margin-bottom: 2px; }
.stats strong { font-size: 20px; }
.kpi-hint { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; gap: 6px; margin: -10px 0 0; }
.panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.filter-area { align-items: end; display: grid; grid-template-columns: repeat(3, minmax(130px, 1fr)) auto auto auto auto auto; gap: 12px; padding: 18px 18px 14px; border-bottom: 1px solid var(--border); position: relative; }
.filter-area label { display: grid; gap: 6px; color: var(--text-mid); font-size: 12px; font-weight: 600; }
.presets { background: var(--surface); border-radius: var(--radius-sm); display: flex; padding: 3px; }
.presets button { border-radius: 6px; color: var(--text-mid); font-size: 12px; padding: 8px 10px; }
.presets button:hover { background: #fff; color: var(--text-dark); }
.field-error { color: var(--red-active); flex-basis: 100%; font-size: 12px; margin: 0; }
.state { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 10px; justify-content: center; min-height: 280px; padding: 32px; text-align: center; }
.state > i { color: var(--text-light); font-size: 36px; }
.state.error > i { color: var(--red-active); }
.table-wrapper { border-top: 1px solid var(--border); overflow-x: auto; }
.table { min-width: 1020px; }
.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .03em; text-transform: uppercase; }
.table td { vertical-align: middle; }
.sub { color: var(--text-light); display: block; font-size: 11px; }
.muted { color: var(--text-light); }
.order-link { color: var(--role-admin); font-weight: 700; }
.order-link:hover, .order-link:focus { text-decoration: underline; }
.table-footer { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; justify-content: space-between; padding: 14px 16px; border-top: 1px solid var(--border); }
.footer-right { align-items: center; display: flex; gap: 14px; }
.size-label { align-items: center; display: inline-flex; gap: 6px; }
.size-label .form-select { width: 80px; }
.pagination { align-items: center; display: flex; gap: 10px; margin: 0; }
.pagination button { align-items: center; border: 1px solid var(--border); border-radius: 7px; display: inline-flex; height: 32px; justify-content: center; width: 32px; }
.pagination button:disabled { cursor: not-allowed; opacity: .4; }
@media (max-width: 1100px) {
  .filter-area { grid-template-columns: repeat(2, minmax(130px, 1fr)); }
}
@media (max-width: 900px) {
  .stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .table-wrapper { overflow: visible; }
  .table thead { display: none; }
  .table, .table tbody, .table tr, .table td { display: block; width: 100%; }
  .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); }
  .table td { display: grid; grid-template-columns: 108px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; }
  .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; }
}
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-area { grid-template-columns: 1fr; }
  .table-footer { align-items: stretch; flex-direction: column; gap: 10px; }
  .footer-right { justify-content: space-between; }
}
</style>
