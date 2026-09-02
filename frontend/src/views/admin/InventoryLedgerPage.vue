<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { adminApi } from '@/api';

const TRANSACTION_TYPES = ['RECEIPT', 'RESERVE', 'RELEASE', 'CONSUME', 'ADJUSTMENT', 'WASTE', 'RETURN'];
const TYPE_LABELS = { RECEIPT: 'Nhập hàng', RESERVE: 'Giữ chỗ', RELEASE: 'Trả lại', CONSUME: 'Tiêu thụ', ADJUSTMENT: 'Điều chỉnh', WASTE: 'Hao hụt', RETURN: 'Khách trả lại' };
const REASON_LABELS = { OPENING_BALANCE: 'Số dư đầu kỳ' };
const SIZE_OPTIONS = [20, 50, 100];
const money = (value) => value == null ? '—' : `${Number(value).toLocaleString('vi-VN')} ₫`;

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const inventoryItemId = ref('');
const orderId = ref('');
const transactionType = ref('');
const fromDate = ref('');
const toDate = ref('');
const loading = ref(false);
const loadError = ref('');
const itemNameById = ref(new Map());

const dateError = computed(() =>
  fromDate.value && toDate.value && fromDate.value > toDate.value ? 'Từ ngày không được sau đến ngày.' : ''
);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
const rangeStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * size.value + 1));
const rangeEnd = computed(() => Math.min(page.value * size.value, total.value));

const kpi = computed(() => {
  const counts = Object.fromEntries(TRANSACTION_TYPES.map((type) => [type, 0]));
  rows.value.forEach((row) => {
    if (counts[row.transactionType] !== undefined) counts[row.transactionType] += 1;
  });
  return counts;
});

function typeClass(type) {
  return { RECEIPT: 'badge-success', RESERVE: 'badge-info', RELEASE: 'badge-secondary', CONSUME: 'badge-warning', ADJUSTMENT: 'badge-secondary', WASTE: 'badge-danger', RETURN: 'badge-info' }[type] || 'badge-secondary';
}

function reasonLabel(reason) {
  return REASON_LABELS[reason] || reason;
}

function itemName(id) {
  if (!id) return null;
  const name = itemNameById.value.get(Number(id));
  return name ? `#${id} · ${name}` : `#${id}`;
}

function buildParams() {
  const params = { page: page.value - 1, size: size.value };
  if (inventoryItemId.value.trim()) params.inventoryItemId = inventoryItemId.value.trim();
  if (orderId.value.trim()) params.orderId = orderId.value.trim();
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
    total.value = Number(data?.totalItems) || 0;
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

async function loadItemNames() {
  try {
    const items = await adminApi.getInventoryItems();
    itemNameById.value = new Map((Array.isArray(items) ? items : []).map((item) => [item.inventoryItemId, item.name]));
  } catch {
    itemNameById.value = new Map();
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
  inventoryItemId.value = '';
  orderId.value = '';
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

onMounted(() => {
  loadItemNames();
  load();
});
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
        <div><small>Tổng giao dịch</small><strong>{{ total }}</strong></div>
      </article>
      <article v-for="type in TRANSACTION_TYPES" :key="type">
        <span class="stat-icon slate"><i class="bi bi-list-check" aria-hidden="true"></i></span>
        <div><small>{{ TYPE_LABELS[type] }}</small><strong>{{ kpi[type] }}</strong></div>
      </article>
    </section>
    <p class="kpi-hint"><i class="bi bi-info-circle" aria-hidden="true"></i> KPI tính theo dữ liệu trang hiện tại.</p>

    <section class="panel">
      <div class="filter-area">
        <label><span class="form-label">Mã mặt hàng</span><input v-model="inventoryItemId" class="form-input" type="text" inputmode="numeric" placeholder="Ví dụ: 3" @keydown.enter="applyFilters" /></label>
        <label><span class="form-label">Mã đơn hàng</span><input v-model="orderId" class="form-input" type="text" inputmode="numeric" placeholder="Ví dụ: 12" @keydown.enter="applyFilters" /></label>
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
        <div class="ledger-frame">
          <div class="table-wrapper">
          <table class="ledger-table table">
            <thead><tr><th scope="col">Thời gian</th><th scope="col">Loại</th><th scope="col">Mặt hàng</th><th scope="col">Đơn hàng</th><th scope="col">Số lượng</th><th scope="col">Biến động</th><th scope="col">Giá vốn đơn vị</th><th scope="col">Tổng giá vốn</th><th scope="col">Chi tiết</th></tr></thead>
            <tbody>
              <tr v-for="row in rows" :key="row.inventoryTransactionId">
                <td data-label="Thời gian"><time :datetime="row.createdAt">{{ row.createdAt }}</time></td>
                <td data-label="Loại"><span class="badge" :class="typeClass(row.transactionType)">{{ TYPE_LABELS[row.transactionType] || row.transactionType }}</span></td>
                <td data-label="Mặt hàng"><strong v-if="itemName(row.inventoryItemId)">{{ itemName(row.inventoryItemId) }}</strong><span v-else class="muted">—</span></td>
                <td data-label="Đơn hàng"><router-link v-if="row.orderId" class="order-link" :to="`/admin/orders/${row.orderId}`">#{{ row.orderId }}</router-link><span v-else class="muted">—</span></td>
                <td data-label="Số lượng"><strong>{{ row.quantity }}</strong></td>
                <td data-label="Biến động"><span v-if="row.quantityBefore !== null && row.quantityAfter !== null">{{ row.quantityBefore }} → {{ row.quantityAfter }}</span><span v-else class="muted">—</span></td>
                <td data-label="Giá vốn đơn vị">{{ money(row.unitCostSnapshot) }}</td>
                <td data-label="Tổng giá vốn"><strong>{{ money(row.totalCost) }}</strong></td>
                <td data-label="Chi tiết"><div v-if="row.reason || row.note" class="detail-cell"><span v-if="row.reason" class="badge badge-secondary">{{ reasonLabel(row.reason) }}</span><small v-if="row.note" class="sub">{{ row.note }}</small></div><span v-else class="muted">—</span></td>
              </tr>
            </tbody>
          </table>
          </div>
        </div>
        <ul class="ledger-card-list" aria-label="Bằng chứng biến động tồn kho">
          <li v-for="row in rows" :key="`card-${row.inventoryTransactionId}`" class="ledger-card">
            <div class="ledger-card-heading"><time :datetime="row.createdAt">{{ row.createdAt }}</time><span class="badge" :class="typeClass(row.transactionType)">{{ TYPE_LABELS[row.transactionType] || row.transactionType }}</span></div>
            <dl>
              <div><dt>Mặt hàng</dt><dd>{{ itemName(row.inventoryItemId) || '—' }}</dd></div>
              <div><dt>Đơn hàng</dt><dd><router-link v-if="row.orderId" :to="`/admin/orders/${row.orderId}`">#{{ row.orderId }}</router-link><span v-else>—</span></dd></div>
              <div><dt>Số lượng</dt><dd>{{ row.quantity }}</dd></div>
              <div><dt>Biến động</dt><dd>{{ row.quantityBefore !== null && row.quantityAfter !== null ? `${row.quantityBefore} → ${row.quantityAfter}` : '—' }}</dd></div>
              <div><dt>Tổng giá vốn</dt><dd>{{ money(row.totalCost) }}</dd></div>
              <div><dt>Chi tiết</dt><dd>{{ row.reason ? reasonLabel(row.reason) : (row.note || '—') }}</dd></div>
            </dl>
          </li>
        </ul>
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
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 16px; }
.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-xs); display: flex; gap: 14px; padding: 18px; }
.stat-icon { align-items: center; border-radius: 10px; display: inline-flex; flex: 0 0 42px; height: 42px; justify-content: center; font-size: 19px; }
.stat-icon.slate { color: #1f2937; background: #e5e7eb; }
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
.ledger-frame { border-top: 1px solid var(--border); overflow:hidden; }.table-wrapper{overflow-x:auto}
.ledger-table { min-width: 980px; }
.ledger-card-list{display:none;margin:0;padding:0;list-style:none}.ledger-card{padding:16px;border-bottom:1px solid var(--admin-hairline);background:var(--admin-surface)}.ledger-card-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.ledger-card-heading time{color:var(--admin-muted);font-size:12px}.ledger-card dl{display:grid;gap:8px;margin:14px 0 0}.ledger-card dl div{display:grid;grid-template-columns:96px minmax(0,1fr);gap:12px}.ledger-card dt{color:var(--admin-muted);font-size:12px}.ledger-card dd{margin:0;font-weight:650;overflow-wrap:anywhere}
.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .03em; text-transform: uppercase; }
.table td { vertical-align: middle; }
.sub { color: var(--text-light); display: block; font-size: 11px; }
.muted { color: var(--text-light); }
.detail-cell { display: flex; flex-direction: column; gap: 4px; }
.detail-cell .badge { align-self: flex-start; }
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
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ledger-table { display: none; }
  .ledger-card-list { display: grid; }
}
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-area { grid-template-columns: 1fr; }
  .table-footer { align-items: stretch; flex-direction: column; gap: 10px; }
  .footer-right { justify-content: space-between; }
}
</style>
