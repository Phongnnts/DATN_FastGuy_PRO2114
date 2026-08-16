<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { codSettlementApi } from '@/api';
import { formatPrice } from '@/utils/format';
import { canSubmitSettlement, submitSettlement as runSubmitSettlement } from './cod-settlement-state';

const loading = ref(false);
const submitting = ref(false);
const error = ref('');
const formError = ref('');
const successMessage = ref('');
const conflictMessage = ref('');
const current = ref(null);
const history = ref([]);
const submittedAmount = ref('');
let generation = 0;
let stopped = false;

const shift = computed(() => current.value?.shift || null);
const settlement = computed(() => current.value?.settlement || null);
const canSubmit = computed(() => canSubmitSettlement(current.value) && !submitting.value);
const resultLabel = computed(() => ({ SUBMITTED: 'Đang chờ Admin kiểm đếm', SETTLED: 'Đã khớp', SHORT: 'Thiếu tiền', OVER: 'Thừa tiền' }[settlement.value?.status] || 'Chưa gửi bàn giao'));
const statusLabel = status => ({ SUBMITTED: 'Đang chờ Admin kiểm đếm', SETTLED: 'Đã khớp', SHORT: 'Thiếu tiền', OVER: 'Thừa tiền' }[status] || status || 'Chưa xác nhận');
const formatDateTime = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';
const formatShift = item => `${item.shiftDate || '—'} · ${item.startTime || '—'}–${item.endTime || '—'}`;

async function load() {
  const requestGeneration = ++generation;
  loading.value = true;
  error.value = '';
  try {
    const [currentResult, historyResult] = await Promise.all([
      codSettlementApi.getCurrent(),
      codSettlementApi.getMine(),
    ]);
    if (stopped || requestGeneration !== generation) return;
    current.value = currentResult;
    history.value = Array.isArray(historyResult) ? historyResult : historyResult?.items || [];
  } catch (loadError) {
    if (!stopped && requestGeneration === generation) error.value = loadError?.message || 'Không thể tải dữ liệu bàn giao COD.';
  } finally {
    if (!stopped && requestGeneration === generation) loading.value = false;
  }
}

async function submitSettlement() {
  successMessage.value = '';
  submitting.value = true;
  const state = {
    current: current.value,
    submittedAmount: submittedAmount.value,
    formError: formError.value,
    announcement: conflictMessage.value,
  };
  try {
    const submitted = await runSubmitSettlement(state, {
      submit: codSettlementApi.submit,
      refresh: load,
    });
    formError.value = state.formError;
    conflictMessage.value = state.announcement;
    if (submitted) {
      successMessage.value = 'Đã gửi bàn giao COD. Số tiền không thể sửa sau khi gửi.';
      await load();
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
onUnmounted(() => { stopped = true; generation += 1; });
</script>

<template>
  <main class="cash-page">
    <header class="page-header"><h1><i class="bi bi-cash-coin" aria-hidden="true"></i> Bàn giao COD</h1></header>
    <div v-if="loading" class="state" role="status" aria-live="polite">Đang tải bàn giao COD...</div>
    <div v-else-if="error" class="state error" role="alert">
      <p>{{ error }}</p>
      <button type="button" class="btn btn-outline" @click="load">Thử lại</button>
    </div>
    <template v-else>
      <section v-if="current?.state === 'NO_ACTIVE_SHIFT'" class="empty-card" aria-labelledby="no-shift-title">
        <h2 id="no-shift-title">Chưa có ca đang hoạt động</h2>
        <p>Check-in ca Shipper để xem và gửi bàn giao COD.</p>
      </section>
      <template v-else>
        <section class="money-grid" aria-label="Tổng quan bàn giao COD">
          <article class="money-card"><small>Tiền dự kiến theo ca</small><strong>{{ formatPrice(shift?.expectedAmount ?? 0) }}</strong></article>
          <article class="money-card"><small>Đã gửi bàn giao</small><strong>{{ settlement ? formatPrice(settlement.submittedAmount) : 'Chưa gửi' }}</strong></article>
          <article class="money-card"><small>Kết quả Admin xác nhận</small><strong>{{ resultLabel }}</strong><span>{{ settlement?.verifiedAmount == null ? '—' : formatPrice(settlement.verifiedAmount) }}</span></article>
        </section>
        <p v-if="conflictMessage" class="conflict-banner" role="alert">{{ conflictMessage }}</p>

        <form v-if="canSubmit" class="submit-card" @submit.prevent="submitSettlement" novalidate>
          <h2>Gửi bàn giao ca</h2>
          <label for="submitted-amount">Số tiền thực nộp</label>
          <input id="submitted-amount" v-model="submittedAmount" type="number" min="0" step="1000" inputmode="numeric" :aria-describedby="formError ? 'submitted-warning submitted-error' : 'submitted-warning'" :aria-invalid="Boolean(formError)">
          <p id="submitted-warning" class="warning">Kiểm tra kỹ: số tiền không thể sửa sau khi gửi.</p>
          <p v-if="formError" id="submitted-error" role="alert" class="field-error">{{ formError }}</p>
          <span v-else id="submitted-error" class="sr-only">Số tiền hợp lệ</span>
          <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? 'Đang gửi...' : 'Gửi bàn giao' }}</button>
        </form>
        <div v-else-if="settlement" class="settlement-note">
          <p><strong>Trạng thái:</strong> {{ resultLabel }}</p>
          <p v-if="settlement.reason"><strong>Lý do:</strong> {{ settlement.reason }}</p>
        </div>
      </template>

      <p v-if="successMessage" class="success" role="status" aria-live="polite">{{ successMessage }}</p>

      <section class="history" aria-labelledby="history-title">
        <h2 id="history-title">Lịch sử bàn giao</h2>
        <p v-if="!history.length" class="state">Chưa có lịch sử bàn giao COD.</p>
        <ul v-else>
          <li v-for="item in history" :key="item.settlementId" class="history-card">
            <div class="history-heading"><strong>Ca #{{ item.shiftId }} · {{ formatShift(item) }}</strong><span>{{ statusLabel(item.status) }}</span></div>
            <dl>
              <div><dt>Tiền dự kiến</dt><dd>{{ formatPrice(item.expectedAmount) }}</dd></div>
              <div><dt>Thực nộp</dt><dd>{{ formatPrice(item.submittedAmount) }}</dd></div>
              <div><dt>Admin xác nhận</dt><dd>{{ item.verifiedAmount == null ? '—' : formatPrice(item.verifiedAmount) }}</dd></div>
            </dl>
            <p v-if="item.reason"><strong>Lý do:</strong> {{ item.reason }}</p>
            <p><strong>Người nhận:</strong> {{ item.receivedByName || 'Chưa có' }}</p>
            <p class="timestamps">Gửi: {{ formatDateTime(item.submittedAt) }} · Xác nhận: {{ formatDateTime(item.verifiedAt) }}</p>
          </li>
        </ul>
      </section>
    </template>
  </main>
</template>

<style scoped>
.cash-page { display: grid; gap: 14px; }.page-header h1 { font-size: 18px; }.money-grid { display: grid; gap: 8px; }.money-card, .submit-card, .settlement-note, .history-card, .empty-card { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 14px; }.money-card small, .money-card strong, .money-card span { display: block; }.money-card small { color: var(--text-mid); font-size: 12px; }.money-card strong { margin-top: 5px; font-size: 19px; }.money-card span { margin-top: 3px; color: var(--text-mid); }.submit-card { display: grid; gap: 8px; }.submit-card h2, .history h2, .empty-card h2 { font-size: 15px; }.submit-card label { font-weight: 600; }.submit-card input { width: 100%; min-height: 44px; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 10px 12px; font: inherit; }.submit-card button, .error button { min-height: 44px; }.warning { color: #92400e; font-size: 12px; }.field-error, .error { color: var(--red-active); }.success { color: #166534; }.state { text-align: center; padding: 24px; color: var(--text-mid); }.history ul { display: grid; gap: 10px; list-style: none; margin: 10px 0 0; padding: 0; }.history-heading { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 6px; }.history-heading span { color: var(--primary); font-weight: 600; }.history-card dl { display: grid; gap: 6px; margin: 12px 0; }.history-card dl div { display: flex; justify-content: space-between; gap: 12px; }.history-card dt { color: var(--text-mid); }.history-card dd { margin: 0; font-weight: 600; }.history-card p { margin-top: 6px; }.timestamps { color: var(--text-mid); font-size: 12px; }.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@media(min-width:700px) { .money-grid { grid-template-columns: repeat(3, 1fr); }.history-card dl { grid-template-columns: repeat(3, 1fr); }.history-card dl div { display: block; }.history-card dd { margin-top: 3px; } }
@media(prefers-reduced-motion:reduce) { *, *::before, *::after { scroll-behavior: auto; transition: none !important; animation: none !important; } }
</style>
