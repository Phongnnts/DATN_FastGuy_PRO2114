<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import { useToast } from '@/stores/toast';
import { validateRefund } from '@/utils/refundPolicy';

const REFUND_STATUS_KEYS = ['PENDING', 'REFUNDED', 'REJECTED'];

const toast = useToast();
const route = useRoute();
const router = useRouter();
const rows = ref([]);
const loading = ref(false);
const loadError = ref('');
const activeStatus = ref('');
const searchTerm = ref('');
const filterFromDate = ref('');
const filterToDate = ref('');
const refundOrder = ref(null);
const refundDialog = ref(null);
const refundForm = ref({ refundAmount: 0, refundNote: '', status: 'REFUNDED' });
const refunding = ref(false);
const previousFocus = ref(null);

const statusFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ hoàn' },
  { key: 'REFUNDED', label: 'Đã hoàn' },
  { key: 'REJECTED', label: 'Từ chối' },
];

const dateError = computed(() => filterFromDate.value && filterToDate.value && filterFromDate.value > filterToDate.value ? 'Từ ngày không được sau đến ngày.' : '');
const kpi = computed(() => {
  const counts = { PENDING: 0, REFUNDED: 0, REJECTED: 0 };
  rows.value.forEach((r) => {
    if (counts[r.refundStatus] !== undefined) counts[r.refundStatus] += 1;
  });
  return counts;
});
const countFor = (key) => (key ? kpi.value[key] || 0 : rows.value.length);

const filtered = computed(() => {
  let list = rows.value;
  if (activeStatus.value) list = list.filter((r) => r.refundStatus === activeStatus.value);
  const q = searchTerm.value.trim().toLocaleLowerCase('vi');
  if (q) list = list.filter((r) => `${r.orderCode || ''} ${r.customerName || ''} ${r.customerPhone || ''}`.toLocaleLowerCase('vi').includes(q));
  return list;
});

let loadGeneration = 0;

async function load() {
  if (dateError.value) return;
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = '';
  try {
    const params = {};
    if (filterFromDate.value) params.fromDate = filterFromDate.value;
    if (filterToDate.value) params.toDate = filterToDate.value;
    const data = await adminApi.getRefunds(params);
    if (generation !== loadGeneration) return;
    rows.value = Array.isArray(data) ? data : [];
  } catch (e) {
    if (generation !== loadGeneration) return;
    loadError.value = e?.response?.data?.message || e.message || 'Không thể tải danh sách hoàn tiền.';
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

function localDate(daysAgo = 0) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  return date.toLocaleDateString('en-CA');
}
function setDatePreset(days) {
  filterFromDate.value = days === 0 ? localDate() : localDate(days - 1);
  filterToDate.value = localDate();
  load();
}
function setStatus(key) {
  activeStatus.value = key;
  router.replace({ query: { status: key || undefined } });
}
function statusFromQuery(raw) {
  return REFUND_STATUS_KEYS.includes(raw) ? raw : '';
}
function loadPreset() {
  activeStatus.value = statusFromQuery(route.query.status);
  load();
}
function resetFilters() {
  setStatus('');
  searchTerm.value = '';
  filterFromDate.value = '';
  filterToDate.value = '';
  load();
}
onMounted(loadPreset);

watch(() => route.query.status, (raw) => {
  const key = statusFromQuery(raw);
  if (activeStatus.value !== key) activeStatus.value = key;
});

function openRefund(order) {
  previousFocus.value = document.activeElement;
  refundOrder.value = order;
  refundForm.value = { refundAmount: Number(order.finalAmount || 0), refundNote: '', status: 'REFUNDED' };
  nextTick(() => refundDialog.value?.focus());
}
function dismissRefund() {
  refundOrder.value = null;
  nextTick(() => previousFocus.value?.focus());
  previousFocus.value = null;
}
function closeRefund() {
  if (refunding.value) return;
  dismissRefund();
}
const formError = computed(() => validateRefund({
  status: refundForm.value.status,
  amount: Number(refundForm.value.refundAmount),
  finalAmount: Number(refundOrder.value?.finalAmount),
  note: refundForm.value.refundNote,
}));
async function saveRefund() {
  if (refunding.value) return;
  if (formError.value) {
    toast.error(formError.value);
    return;
  }
  refunding.value = true;
  try {
    await adminApi.updateRefund(refundOrder.value.orderId, {
      status: refundForm.value.status,
      refundAmount: refundForm.value.status === 'REFUNDED' ? Number(refundForm.value.refundAmount) : null,
      refundNote: refundForm.value.refundNote.trim(),
    });
    toast.success(refundForm.value.status === 'REFUNDED' ? 'Đã hoàn tiền thành công' : 'Đã từ chối hoàn tiền');
    dismissRefund();
    await load();
  } catch (e) {
    toast.error(e?.response?.data?.message || e.message || 'Không thể xử lý hoàn tiền');
  } finally {
    refunding.value = false;
  }
}
</script>

<template>
  <main class="refunds-page">
    <header class="page-heading">
      <div><p class="eyebrow">Vận hành</p><h1>Quản lý hoàn tiền</h1><p>Xử lý yêu cầu hoàn tiền cho đơn đã hủy.</p></div>
      <button class="btn btn-outline" :disabled="loading" @click="load"><i class="bi bi-arrow-clockwise"></i> Làm mới</button>
    </header>

    <section class="stats" aria-label="Tổng quan hoàn tiền">
      <article><span class="stat-icon amber"><i class="bi bi-hourglass-split"></i></span><div><small>Chờ hoàn</small><strong>{{ countFor('PENDING') }}</strong></div></article>
      <article><span class="stat-icon green"><i class="bi bi-check2-circle"></i></span><div><small>Đã hoàn</small><strong>{{ countFor('REFUNDED') }}</strong></div></article>
      <article><span class="stat-icon red"><i class="bi bi-x-circle"></i></span><div><small>Từ chối</small><strong>{{ countFor('REJECTED') }}</strong></div></article>
    </section>
    <p class="kpi-hint"><i class="bi bi-info-circle"></i> KPI tính theo bộ lọc ngày tạo đơn đang áp dụng.</p>

    <section class="panel">
      <nav class="status-tabs" aria-label="Lọc trạng thái hoàn tiền">
        <button v-for="item in statusFilters" :key="item.key" :class="{ active: activeStatus === item.key }" :aria-pressed="activeStatus === item.key" @click="setStatus(item.key)">
          {{ item.label }} <span>{{ countFor(item.key) }}</span>
        </button>
      </nav>

      <div class="filter-area">
        <div class="search-box wide"><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" aria-label="Tìm mã đơn, khách hàng" placeholder="Tìm mã đơn, tên khách, SĐT..." /></div>
      </div>
      <div class="date-row">
        <div class="presets"><button @click="setDatePreset(0)">Hôm nay</button><button @click="setDatePreset(7)">7 ngày</button><button @click="setDatePreset(30)">30 ngày</button></div>
        <label>Từ ngày tạo <input v-model="filterFromDate" type="date" class="form-input" :max="filterToDate || undefined" /></label>
        <label>Đến ngày tạo <input v-model="filterToDate" type="date" class="form-input" :min="filterFromDate || undefined" /></label>
        <button class="btn btn-primary" :disabled="!!dateError || loading" @click="load">Áp dụng</button>
        <button class="btn btn-outline" @click="resetFilters"><i class="bi bi-x-circle"></i> Đặt lại</button>
        <p v-if="dateError" class="field-error" role="alert">{{ dateError }}</p>
      </div>

      <div v-if="loading" class="state" role="status"><span class="spinner"></span><strong>Đang tải yêu cầu hoàn tiền...</strong></div>
      <div v-else-if="loadError" class="state error" role="alert"><i class="bi bi-exclamation-circle"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-inbox"></i><strong>Không có yêu cầu hoàn tiền</strong><span>Thử thay đổi hoặc đặt lại bộ lọc.</span><button class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Đơn hàng</th><th>Khách hàng</th><th>Giá trị</th><th>Thanh toán</th><th>Hoàn tiền</th><th>Ngày tạo</th><th>Thao tác</th></tr></thead>
            <tbody><tr v-for="row in filtered" :key="row.orderId">
              <td data-label="Đơn hàng"><router-link class="order-link" :to="`/admin/orders/${row.orderId}`" :aria-label="`Xem đơn ${row.orderCode}`">{{ row.orderCode }}</router-link></td>
              <td data-label="Khách hàng"><strong>{{ row.customerName || 'Khách' }}</strong><small class="sub">{{ row.customerPhone || '' }}</small></td>
              <td data-label="Giá trị"><strong>{{ formatPrice(row.finalAmount) }}</strong></td>
              <td data-label="Thanh toán">
                <span class="payment-method">{{ row.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }}</span>
                <small :class="['payment-state', String(row.paymentStatus).toLowerCase()]">{{ row.paymentStatus === 'PAID' ? 'Đã thanh toán' : row.paymentStatus === 'REFUNDED' ? 'Đã hoàn' : row.paymentStatus }}</small>
              </td>
              <td data-label="Hoàn tiền">
                <span v-if="row.refundStatus === 'REFUNDED'" class="refund-badge refund-done">Đã hoàn {{ formatPrice(row.refundAmount) }}</span>
                <span v-else-if="row.refundStatus === 'REJECTED'" class="refund-badge refund-rejected">Đã từ chối</span>
                <span v-else class="refund-badge refund-pending">Chờ hoàn</span>
              </td>
              <td data-label="Ngày tạo">{{ formatDate(row.createdAt) }}</td>
              <td data-label="Thao tác"><button v-if="row.refundStatus === 'PENDING'" class="refund-action" @click="openRefund(row)"><i class="bi bi-arrow-return-left"></i> Xử lý</button><span v-else class="muted">—</span></td>
            </tr></tbody>
          </table>
        </div>
      </template>
    </section>

    <div v-if="refundOrder" class="modal-overlay" @click.self="closeRefund" @keydown.esc="closeRefund">
      <form ref="refundDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="refund-title" tabindex="-1" @submit.prevent="saveRefund">
        <div class="modal-header"><div><small>HOÀN TIỀN</small><h3 id="refund-title">{{ refundOrder.orderCode }}</h3></div><button type="button" class="icon-button" aria-label="Đóng" :disabled="refunding" @click="closeRefund"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div class="refund-order-info">
            <div><span>Khách hàng</span><strong>{{ refundOrder.customerName || 'Khách' }}</strong></div>
            <div><span>Giá trị đơn</span><strong>{{ formatPrice(refundOrder.finalAmount) }}</strong></div>
            <div><span>Thanh toán</span><strong>{{ refundOrder.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }} · {{ refundOrder.paymentStatus }}</strong></div>
          </div>
          <label class="form-group"><span class="form-label">Hành động</span><select v-model="refundForm.status" class="form-select"><option value="REFUNDED">Xác nhận đã hoàn tiền</option><option value="REJECTED">Từ chối hoàn tiền</option></select></label>
          <label v-if="refundForm.status === 'REFUNDED'" class="form-group"><span class="form-label">Số tiền hoàn</span><input v-model.number="refundForm.refundAmount" class="form-input" type="number" min="1" step="1000" :max="Number(refundOrder.finalAmount)" required /><small>Tối đa {{ formatPrice(refundOrder.finalAmount) }}</small></label>
          <label class="form-group"><span class="form-label">{{ refundForm.status === 'REJECTED' ? 'Lý do từ chối *' : 'Ghi chú' }}</span><textarea v-model="refundForm.refundNote" class="form-input" rows="3" :required="refundForm.status === 'REJECTED'"></textarea></label>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-outline" :disabled="refunding" @click="closeRefund">Hủy</button><button class="btn" :class="refundForm.status === 'REFUNDED' ? 'btn-primary' : 'btn-danger'" :disabled="refunding">{{ refunding ? 'Đang xử lý...' : 'Xác nhận' }}</button></div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.refunds-page { display: grid; gap: 24px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-heading h1 { font-size: 28px; line-height: 1.25; margin: 2px 0 4px; }
.page-heading p { color: var(--text-mid); font-size: 14px; }
.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-xs); display: flex; gap: 14px; padding: 18px; }
.stat-icon { align-items: center; border-radius: 10px; display: inline-flex; flex: 0 0 42px; height: 42px; justify-content: center; font-size: 19px; }
.stat-icon.amber { color: #b45309; background: #fef3c7; }.stat-icon.green { color: #047857; background: #d1fae5; }.stat-icon.red { color: #b91c1c; background: #fee2e2; }
.stats small { color: var(--text-mid); display: block; font-size: 12px; margin-bottom: 2px; }.stats strong { font-size: 20px; }
.kpi-hint { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; gap: 6px; margin: -10px 0 0; }
.panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.status-tabs { display: flex; gap: 4px; overflow-x: auto; padding: 14px 16px 0; border-bottom: 1px solid var(--border); }
.status-tabs button { color: var(--text-mid); flex: none; font-size: 12px; font-weight: 600; padding: 9px 10px 12px; position: relative; }
.status-tabs button.active { color: var(--role-admin); }.status-tabs button.active::after { background: var(--role-admin); border-radius: 3px 3px 0 0; bottom: 0; content: ''; height: 3px; left: 8px; position: absolute; right: 8px; }
.status-tabs span { background: var(--surface); border-radius: 12px; font-size: 10px; margin-left: 3px; padding: 2px 6px; }
.filter-area { display: grid; grid-template-columns: minmax(240px, 2fr); gap: 10px; padding: 16px 16px 10px; }.wide { max-width: none; }
.date-row { align-items: end; display: flex; flex-wrap: wrap; gap: 10px; padding: 0 16px 16px; position: relative; }.date-row label { color: var(--text-mid); font-size: 11px; font-weight: 600; }.date-row input { margin-top: 4px; width: 145px; }
.presets { background: var(--surface); border-radius: var(--radius-sm); display: flex; padding: 3px; }.presets button { border-radius: 6px; color: var(--text-mid); font-size: 12px; padding: 8px 10px; }.presets button:hover { background: white; color: var(--text-dark); }
.field-error { color: var(--red-active); flex-basis: 100%; font-size: 12px; }
.table-wrapper { border-top: 1px solid var(--border); overflow-x: auto; }.table { min-width: 1020px; }.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .03em; text-transform: uppercase; }.table td { vertical-align: middle; }
.order-link { color: var(--role-admin); font-weight: 700; }.order-link:hover, .order-link:focus { text-decoration: underline; }
.sub { color: var(--text-light); display: block; font-size: 11px; }
.payment-method { display: block; font-size: 13px; font-weight: 700; }.payment-state { color: #b45309; display: block; font-size: 11px; }.payment-state.paid { color: #047857; }.payment-state.failed { color: #b91c1c; }
.refund-badge, .refund-action { border-radius: 99px; display: inline-flex; font-size: 11px; font-weight: 700; padding: 5px 9px; white-space: nowrap; }.refund-done { color: #047857; background: #d1fae5; }.refund-rejected { color: #b91c1c; background: #fee2e2; }.refund-pending { color: #92400e; background: #fef3c7; }.refund-action { background: #fef3c7; color: #92400e; border: 0; cursor: pointer; gap: 5px; }.refund-action:hover { background: #fde68a; }.muted { color: var(--text-light); }
.state { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 10px; justify-content: center; min-height: 280px; padding: 32px; text-align: center; }.state > i { color: var(--text-light); font-size: 36px; }.state.error > i { color: var(--red-active); }.spinner { animation: spin .8s linear infinite; border: 3px solid var(--border); border-radius: 50%; border-top-color: var(--role-admin); height: 30px; width: 30px; }@keyframes spin { to { transform: rotate(360deg); } }
.modal { max-width: 520px; width: calc(100% - 32px); }.modal:focus { outline: none; }.modal-header small { color: var(--role-admin); font-size: 10px; font-weight: 800; letter-spacing: .1em; }.icon-button { border-radius: 8px; font-size: 18px; padding: 8px; }.icon-button:hover { background: var(--surface); }.icon-button:disabled { cursor: not-allowed; opacity: .5; }
.refund-order-info { background: var(--surface); border-radius: var(--radius-sm); margin-bottom: 18px; padding: 8px 14px; }.refund-order-info div { display: flex; font-size: 13px; justify-content: space-between; padding: 9px 0; }.refund-order-info div + div { border-top: 1px solid var(--border); }.refund-order-info span { color: var(--text-mid); }.form-group small { color: var(--text-mid); display: block; font-size: 11px; margin-top: 5px; }
@media (max-width: 900px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .table-wrapper { overflow: visible; }
  .table thead { display: none; }
  .table, .table tbody, .table tr, .table td { display: block; width: 100%; }
  .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); }
  .table td { display: grid; grid-template-columns: 108px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; }
  .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; }
}
@media (max-width: 640px) { .page-heading { align-items: flex-start; flex-direction: column; }.stats { grid-template-columns: 1fr; }.date-row > label { flex: 1; }.date-row input { width: 100%; } }
</style>
