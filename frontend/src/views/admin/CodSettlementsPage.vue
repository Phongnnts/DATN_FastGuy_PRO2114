<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { codSettlementApi } from '@/api';
import { formatPrice } from '@/utils/format';
import { acceptsAdminCodRequest, canVerifySettlement, createModalLifecycle, focusCycleTarget, submitVerification, validateVerification } from './cod-settlement-state';

const STATUS_KEYS = ['SUBMITTED', 'SHORT', 'OVER', 'SETTLED'];
const filters = [
  { value: 'SUBMITTED', label: 'Chờ xác nhận' },
  { value: 'SHORT', label: 'Thiếu tiền' },
  { value: 'OVER', label: 'Thừa tiền' },
  { value: 'SETTLED', label: 'Đã khớp' },
];
const route = useRoute();
const router = useRouter();
const statusFromQuery = raw => STATUS_KEYS.includes(raw) ? raw : 'SUBMITTED';
const filter = ref(statusFromQuery(route.query.status));
const rows = ref([]);
const loading = ref(true);
const error = ref('');
const selected = ref(null);
const verifiedAmount = ref('');
const outcome = ref('SETTLED');
const reason = ref('');
const formError = ref('');
const saving = ref(false);
const successMessage = ref('');
const conflictMessage = ref('');
const dialogRef = ref(null);
const pageRef = ref(null);
const triggerRef = ref(null);
const media = window.matchMedia('(max-width: 760px)');
const mobile = ref(media.matches);
let requestGeneration = 0;
let stopped = false;
const modalLifecycle = createModalLifecycle({
  document,
  getDialog: () => dialogRef.value,
  getFocusable: focusable,
  onEscape: closeDialog,
  getFallback: () => pageRef.value,
});

const statusLabels = { SUBMITTED: 'Chờ xác nhận', SHORT: 'Thiếu tiền', OVER: 'Thừa tiền', SETTLED: 'Đã khớp' };

function messageOf(value, fallback) {
  return value?.response?.data?.message || value?.message || fallback;
}

async function load() {
  const request = { generation: ++requestGeneration, status: filter.value };
  loading.value = true;
  error.value = '';
  try {
    const data = await codSettlementApi.getAdmin(request.status);
    if (!acceptsAdminCodRequest({ requestGeneration: request.generation, latestGeneration: requestGeneration, requestStatus: request.status, activeStatus: filter.value, stopped })) return rows.value;
    rows.value = Array.isArray(data) ? data : [];
  } catch (loadError) {
    if (acceptsAdminCodRequest({ requestGeneration: request.generation, latestGeneration: requestGeneration, requestStatus: request.status, activeStatus: filter.value, stopped })) error.value = messageOf(loadError, 'Không thể tải danh sách bàn giao COD.');
  } finally {
    if (acceptsAdminCodRequest({ requestGeneration: request.generation, latestGeneration: requestGeneration, requestStatus: request.status, activeStatus: filter.value, stopped })) loading.value = false;
  }
  return rows.value;
}

async function openVerify(row, event) {
  if (!canVerifySettlement(row)) return;
  triggerRef.value = event.currentTarget;
  selected.value = row;
  verifiedAmount.value = String(row.submittedAmount);
  outcome.value = 'SETTLED';
  reason.value = '';
  formError.value = '';
  conflictMessage.value = '';
  await nextTick();
  modalLifecycle.open(triggerRef.value);
}

async function closeDialog() {
  if (saving.value) return;
  selected.value = null;
  await nextTick();
  modalLifecycle.close();
}

function focusable() {
  return [...dialogRef.value.querySelectorAll('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
}

function trapFocus(event) {
  if (event.key !== 'Tab') return;
  const controls = focusable();
  const target = focusCycleTarget({ controls, active: document.activeElement, shiftKey: event.shiftKey });
  if (target) { event.preventDefault(); target.focus(); }
}

function validate() {
  return validateVerification({ settlement: selected.value, status: outcome.value, verifiedAmount: verifiedAmount.value, reason: reason.value });
}

async function verify() {
  const state = {
    selected: selected.value,
    status: outcome.value,
    verifiedAmount: verifiedAmount.value,
    reason: reason.value,
    formError: '',
    conflictMessage: '',
    successMessage: '',
    rows: rows.value,
  };
  saving.value = true;
  try {
    const closed = await submitVerification(state, { verify: codSettlementApi.verify, refresh: load });
    formError.value = state.formError;
    conflictMessage.value = state.conflictMessage;
    successMessage.value = state.successMessage;
    if (closed) {
      selected.value = null;
      await nextTick();
      modalLifecycle.close();
    }
  } catch (error) {
    formError.value = messageOf(error, 'Không thể xác nhận bàn giao COD.');
  } finally {
    saving.value = false;
  }
}

function formatDate(value) {
  return value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';
}
function differenceClass(value) { const amount = Number(value || 0); return amount < 0 ? 'short' : amount > 0 ? 'over' : 'settled'; }
function differenceText(value) { const amount = Number(value || 0); return amount === 0 ? 'Khớp' : `${amount > 0 ? '+' : ''}${formatPrice(amount)}`; }

function updateMobile(event) { mobile.value = event.matches; }

watch(filter, (value) => {
  if (route.query.status !== value) router.replace({ query: { ...route.query, status: value } });
  load();
});
watch(() => route.query.status, (raw) => {
  const next = statusFromQuery(raw);
  if (filter.value !== next) filter.value = next;
});
watch(outcome, () => { formError.value = ''; if (outcome.value === 'SETTLED') reason.value = ''; });
onMounted(() => { modalLifecycle.attach(); load(); media.addEventListener('change', updateMobile); });
onBeforeUnmount(() => { stopped = true; requestGeneration += 1; modalLifecycle.detach(); media.removeEventListener('change', updateMobile); });
</script>

<template>
  <main ref="pageRef" class="cod-page" tabindex="-1">
    <header><div><span>VẬN HÀNH TÀI CHÍNH</span><h1>Đối soát COD</h1><p>Kiểm tra bàn giao tiền mặt theo từng ca giao hàng.</p></div></header>
    <div class="filter-tabs" role="group" aria-label="Lọc trạng thái bàn giao">
      <button v-for="item in filters" :key="item.value" type="button" :class="{ active: filter === item.value }" :aria-pressed="filter === item.value" @click="filter = item.value">{{ item.label }}</button>
    </div>
    <label class="mobile-filter" for="cod-status-filter">Trạng thái<select id="cod-status-filter" v-model="filter"><option v-for="item in filters" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
    <p v-if="successMessage" class="notice" role="status">{{ successMessage }}</p>
    <section v-if="loading" class="state" role="status">Đang tải bàn giao COD...</section>
    <section v-else-if="error" class="state error" role="alert"><p>{{ error }}</p><button type="button" @click="load">Thử lại</button></section>
    <section v-else-if="!rows.length" class="state"><i class="bi bi-inbox" aria-hidden="true"></i><p>Không có bàn giao ở trạng thái này.</p></section>
    <template v-else>
      <div v-if="!mobile" class="table-wrap"><table><thead><tr><th>Shipper</th><th>Ca</th><th>Dự kiến</th><th>Đã nộp</th><th>Chênh lệch</th><th>Trạng thái</th><th>Gửi lúc</th><th v-if="filter !== 'SUBMITTED'">Kết quả xác nhận</th><th>Thao tác</th></tr></thead><tbody><tr v-for="row in rows" :key="row.settlementId"><td><strong>{{ row.shipperName }}</strong></td><td>{{ row.shiftDate }}<small>{{ row.startTime }}–{{ row.endTime }}</small></td><td>{{ formatPrice(row.expectedAmount) }}</td><td>{{ formatPrice(row.submittedAmount) }}</td><td><strong class="difference" :class="differenceClass(row.differenceAmount)">{{ differenceText(row.differenceAmount) }}</strong></td><td><span class="status" :class="row.status.toLowerCase()">{{ statusLabels[row.status] }}</span></td><td>{{ formatDate(row.submittedAt) }}</td><td v-if="filter !== 'SUBMITTED'" class="history"><strong>{{ formatPrice(row.verifiedAmount) }}</strong><small v-if="row.reason">{{ row.reason }}</small><small>{{ row.receivedByName }} · {{ formatDate(row.verifiedAt) }}</small></td><td><button v-if="canVerifySettlement(row)" type="button" class="verify-button" @click="openVerify(row, $event)">Xác nhận</button><span v-else>Đã xử lý</span></td></tr></tbody></table></div>
      <div v-else class="cards"><article v-for="row in rows" :key="row.settlementId"><div class="card-head"><strong>{{ row.shipperName }}</strong><span class="status" :class="row.status.toLowerCase()">{{ statusLabels[row.status] }}</span></div><dl><div><dt>Ca</dt><dd>{{ row.shiftDate }} · {{ row.startTime }}–{{ row.endTime }}</dd></div><div><dt>Dự kiến</dt><dd>{{ formatPrice(row.expectedAmount) }}</dd></div><div><dt>Đã nộp</dt><dd>{{ formatPrice(row.submittedAmount) }}</dd></div><div><dt>Chênh lệch</dt><dd><strong class="difference" :class="differenceClass(row.differenceAmount)">{{ differenceText(row.differenceAmount) }}</strong></dd></div><div><dt>Gửi lúc</dt><dd>{{ formatDate(row.submittedAt) }}</dd></div><template v-if="row.status !== 'SUBMITTED'"><div><dt>Kiểm đếm</dt><dd>{{ formatPrice(row.verifiedAmount) }}</dd></div><div v-if="row.reason"><dt>Lý do</dt><dd>{{ row.reason }}</dd></div><div><dt>Người nhận</dt><dd>{{ row.receivedByName }} · {{ formatDate(row.verifiedAt) }}</dd></div></template></dl><button v-if="canVerifySettlement(row)" type="button" class="verify-button" @click="openVerify(row, $event)">Xác nhận bàn giao</button></article></div>
    </template>

    <div v-if="selected" class="modal-backdrop" @click.self="closeDialog"><section ref="dialogRef" class="dialog" role="dialog" aria-modal="true" aria-labelledby="verify-title" @keydown="trapFocus"><div class="dialog-head"><div><span>KIỂM ĐẾM TIỀN MẶT</span><h2 id="verify-title">Xác nhận bàn giao COD</h2></div><button type="button" aria-label="Đóng hộp thoại" :disabled="saving" @click="closeDialog"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><dl class="verify-summary"><div><dt>Shipper</dt><dd>{{ selected.shipperName }}</dd></div><div><dt>Dự kiến</dt><dd>{{ formatPrice(selected.expectedAmount) }}</dd></div><div><dt>Đã nộp</dt><dd>{{ formatPrice(selected.submittedAmount) }}</dd></div><div><dt>Chênh lệch</dt><dd><strong class="difference" :class="differenceClass(selected.differenceAmount)">{{ differenceText(selected.differenceAmount) }}</strong></dd></div></dl><form @submit.prevent="verify"><label for="verify-outcome">Kết quả<select id="verify-outcome" v-model="outcome" :disabled="saving"><option value="SETTLED">Khớp</option><option value="SHORT">Thiếu tiền</option><option value="OVER">Thừa tiền</option></select></label><label for="verified-amount">Số tiền kiểm đếm<input id="verified-amount" v-model="verifiedAmount" type="number" min="0" step="0.01" required :disabled="saving" :aria-invalid="!!formError" :aria-describedby="formError ? 'verify-error' : undefined"></label><label v-if="outcome !== 'SETTLED'" for="verify-reason">Lý do<textarea id="verify-reason" v-model="reason" maxlength="500" required :disabled="saving" :aria-invalid="!!formError" :aria-describedby="formError ? 'verify-error' : undefined"></textarea></label><p v-if="conflictMessage" class="form-error" role="alert" aria-live="assertive">{{ conflictMessage }}</p><p v-if="formError" id="verify-error" class="form-error" role="alert">{{ formError }}</p><div class="dialog-actions"><button type="button" :disabled="saving" @click="closeDialog">Hủy</button><button type="submit" class="verify-button" :disabled="saving">{{ saving ? 'Đang xác nhận...' : 'Xác nhận' }}</button></div></form></section></div>
  </main>
</template>

<style scoped>
.cod-page{display:flex;flex-direction:column;gap:18px;color:var(--text-dark)}header{padding:4px 2px 12px;border-bottom:1px solid var(--admin-border);color:var(--admin-foreground)}header span,.dialog-head span{font-size:10px;font-weight:800;letter-spacing:.1em;color:var(--admin-brand)}h1{margin:5px 0;font-size:28px}header p{color:var(--admin-muted)}.filter-tabs{display:flex;gap:8px;flex-wrap:wrap}.filter-tabs button,.error button,.dialog button,.verify-button,select,input,textarea{min-height:44px}.filter-tabs button,.error button,.dialog button{padding:0 16px;border:1px solid var(--border);border-radius:12px;background:#fff;font-weight:700}.filter-tabs button.active{color:var(--admin-foreground);border-color:var(--admin-brand-soft);background:var(--admin-brand-soft)}.mobile-filter{display:none}.notice,.state{padding:18px;border-radius:14px;background:#fff}.notice{color:#047857;background:#ecfdf5}.state{text-align:center}.state.error,.form-error{color:#b91c1c}.table-wrap{overflow:auto;border:1px solid var(--border);border-radius:18px;background:#fff}table{width:100%;min-width:1050px;border-collapse:collapse}th,td{padding:15px;text-align:left;border-bottom:1px solid var(--border-light)}th{font-size:11px;text-transform:uppercase;color:var(--text-mid)}td small,.history small{display:block;margin-top:4px;color:var(--text-mid)}.status{display:inline-flex;padding:6px 9px;border-radius:20px;font-size:11px;font-weight:800}.status.submitted{color:#92400e;background:#fef3c7}.status.short{color:#b91c1c;background:#fee2e2}.status.over{color:#1d4ed8;background:#dbeafe}.status.settled{color:#047857;background:#d1fae5}.difference{font-variant-numeric:tabular-nums}.difference.short{color:var(--admin-danger)}.difference.over{color:var(--admin-info)}.difference.settled{color:var(--admin-success)}.verify-summary{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;margin:0;padding:12px;background:var(--admin-canvas);border-radius:10px}.verify-summary div{display:grid;gap:3px}.verify-summary dt{font-size:11px;color:var(--admin-muted)}.verify-summary dd{margin:0;font-weight:700}.verify-button{padding:0 16px;border:0;border-radius:11px;color:#fff;background:var(--admin-action);font-weight:800}.cards{display:grid;gap:12px}.cards article{padding:18px;border:1px solid var(--border);border-radius:16px;background:#fff}.card-head{display:flex;justify-content:space-between;gap:12px}.cards dl{display:grid;gap:8px}.cards dl div{display:flex;justify-content:space-between;gap:14px}.cards dt{color:var(--text-mid)}.cards dd{text-align:right}.cards .verify-button{width:100%}.modal-backdrop{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:18px;background:rgba(0,0,0,.55)}.dialog{width:min(520px,100%);max-height:calc(100vh - 36px);padding:24px;overflow:auto;border-radius:20px;background:#fff}.dialog-head{display:flex;justify-content:space-between;gap:16px}.dialog-head h2{margin:5px 0}.dialog-head>button{width:44px;padding:0}.dialog form,.dialog label{display:flex;flex-direction:column;gap:7px}.dialog form{gap:14px;margin-top:18px}.dialog label{font-weight:700}.dialog select,.dialog input,.dialog textarea{padding:10px 12px;border:1px solid var(--border);border-radius:10px;background:#fff}.dialog textarea{min-height:88px;resize:vertical}.dialog-actions{display:flex;justify-content:flex-end;gap:10px}.dialog-actions .verify-button{border:0;background:var(--admin-action)}
@media(max-width:760px){.filter-tabs{display:none}.mobile-filter{display:flex;flex-direction:column;gap:6px;font-weight:700}.mobile-filter select{padding:0 12px;border:1px solid var(--border);border-radius:12px;background:#fff}h1{font-size:27px}}
@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto!important;transition:none!important}}
</style>
