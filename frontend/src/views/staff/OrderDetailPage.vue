<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { staffApi } from '@/api';
import { formatDate as formatDateTime, formatPrice } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { useToast } from '@/stores/toast';
import { DELIVERY_FAILURE_REASON_LABEL } from '@/utils/constants';
import { deliveryRetryMode } from '@/utils/deliveryRecovery';

const toast = useToast();
const route = useRoute();
const staffStore = useStaffStore();
const order = ref(null);
const loading = ref(true);
const loadError = ref('');
const saving = ref(false);
const internalNote = ref('');
const showCancelModal = ref(false);
const cancelReason = ref('');
const selectedShipperId = ref(null);
const shippers = ref([]);
const shipperLoading = ref(false);
const shipperError = ref('');
const showAssignmentDialog = ref(false);
const assignmentDialog = ref(null);
const assignmentSelect = ref(null);
const cancelDialog = ref(null);
const cancelReasonInput = ref(null);
const showRecoveryDialog = ref(false);
const recoveryDialog = ref(null);
const recoveryShipper = ref(null);
const retryMode = ref('IMMEDIATE');
const scheduledAt = ref('');
const recoveryNote = ref('');
const recoveryError = ref('');
const showReturnConfirmation = ref(false);
let previousFocus = null;
let previousBodyOverflow = '';
let stopped = false;
let generation = 0;

const allowedActions = computed(() => Array.isArray(order.value?.allowedActions) ? order.value.allowedActions : []);
const pendingPayment = computed(() => order.value?.paymentMethod === 'BANK_TRANSFER' && order.value?.paymentStatus !== 'PAID');
const assignedShipper = computed(() => order.value?.shipperName || '');
const atEffectiveLimit = computed(() => Number(order.value?.deliveryAttemptCount || 0) >= Number(order.value?.deliveryAttemptLimit || 0));
const recoveryValid = computed(() => recoveryNote.value.trim() && (showReturnConfirmation.value || (recoveryShipper.value && (retryMode.value === 'IMMEDIATE' || scheduledAt.value))));

function acceptsRequest(requestGeneration) {
  return !stopped && requestGeneration === generation;
}

async function load({ silent = false } = {}) {
  if (loading.value || saving.value || stopped) return;
  const requestGeneration = generation;
  if (!silent) loading.value = true;
  loadError.value = '';
  try {
    const nextOrder = await staffStore.fetchOrderById(route.params.id);
    if (!acceptsRequest(requestGeneration)) return;
    if (!nextOrder) throw new Error('Không tìm thấy đơn hàng');
    order.value = nextOrder;
  } catch (error) {
    if (acceptsRequest(requestGeneration)) loadError.value = error.message || 'Không thể tải đơn hàng';
  } finally {
    if (acceptsRequest(requestGeneration)) loading.value = false;
  }
}

async function loadShippers() {
  if (shipperLoading.value || stopped) return;
  const requestGeneration = generation;
  shipperLoading.value = true;
  shipperError.value = '';
  try {
    const data = await staffApi.getAvailableShippers();
    if (!acceptsRequest(requestGeneration)) return;
    shippers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    if (acceptsRequest(requestGeneration)) shipperError.value = error.message || 'Không thể tải danh sách shipper';
  } finally {
    if (acceptsRequest(requestGeneration)) shipperLoading.value = false;
  }
}

async function updateStatus(status, reason = null) {
  if (!order.value || saving.value || stopped || !allowedActions.value.includes(status)) return;
  const requestGeneration = generation;
  saving.value = true;
  try {
    await staffStore.updateOrderStatus(order.value.id, status, order.value.status, reason);
    if (!acceptsRequest(requestGeneration)) return;
    saving.value = false;
    await load({ silent: true });
  } catch (error) {
    if (acceptsRequest(requestGeneration)) {
      toast.error(error.message || 'Không thể cập nhật trạng thái');
      if (error.status === 409) {
        saving.value = false;
        await load({ silent: true });
      }
    }
  } finally {
    if (acceptsRequest(requestGeneration)) saving.value = false;
  }
}

async function assignShipper() {
  if (!order.value || !selectedShipperId.value || saving.value || stopped || !allowedActions.value.includes('ASSIGNED')) return;
  const requestGeneration = generation;
  saving.value = true;
  try {
    await staffApi.assignShipper(order.value.id, selectedShipperId.value, order.value.status);
    if (!acceptsRequest(requestGeneration)) return;
    saving.value = false;
    closeAssignmentDialog();
    await load({ silent: true });
  } catch (error) {
    if (acceptsRequest(requestGeneration)) {
      toast.error(error.message || 'Không thể gán shipper');
      if (error.status === 409) {
        saving.value = false;
        closeAssignmentDialog();
        await load({ silent: true });
      }
    }
  } finally {
    if (acceptsRequest(requestGeneration)) saving.value = false;
  }
}

async function openAssignmentDialog(event) {
  previousFocus = event.currentTarget;
  previousBodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  showAssignmentDialog.value = true;
  await loadShippers();
  if (stopped) return;
  await nextTick();
  if (!stopped) assignmentSelect.value?.focus();
}

function closeAssignmentDialog() {
  if (saving.value) return;
  showAssignmentDialog.value = false;
  document.body.style.overflow = previousBodyOverflow;
  previousFocus?.focus();
}

function trapDialogFocus(event, dialog, close) {
  if (event.key === 'Escape' && !saving.value) {
    event.preventDefault();
    close();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = [...(dialog?.querySelectorAll('textarea, select, button:not([disabled])') || [])];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}

function handleAssignmentKeydown(event) { trapDialogFocus(event, assignmentDialog.value, closeAssignmentDialog); }
async function openRecovery(event, returning = false) {
  previousFocus = event.currentTarget;
  previousBodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  showReturnConfirmation.value = returning;
  retryMode.value = deliveryRetryMode(order.value?.deliveryFailureCode);
  recoveryError.value = '';
  showRecoveryDialog.value = true;
  if (!returning) await loadShippers();
  await nextTick();
  recoveryDialog.value?.querySelector('textarea, select')?.focus();
}
function closeRecovery() {
  if (saving.value) return;
  showRecoveryDialog.value = false;
  document.body.style.overflow = previousBodyOverflow;
  previousFocus?.focus();
}
function handleRecoveryKeydown(event) { trapDialogFocus(event, recoveryDialog.value, closeRecovery); }
async function submitRecovery() {
  if (!recoveryValid.value || saving.value) return;
  saving.value = true;
  recoveryError.value = '';
  try {
    if (showReturnConfirmation.value) await staffApi.returnToStore(order.value.id, order.value.status, recoveryNote.value.trim());
    else await staffApi.retryDelivery(order.value.id, { expectedStatus: order.value.status, shipperId: Number(recoveryShipper.value), retryMode: retryMode.value, scheduledAt: retryMode.value === 'SCHEDULED' ? scheduledAt.value : null, note: recoveryNote.value.trim() });
    saving.value = false;
    closeRecovery();
    await load({ silent: true });
  } catch (error) {
    recoveryError.value = error.message || 'Không thể xử lý giao hàng';
    if (error.status === 409) {
      saving.value = false;
      await load({ silent: true });
    }
  } finally { saving.value = false; }
}
async function startScheduledRetry() {
  if (saving.value) return;
  saving.value = true;
  try { await staffApi.startScheduledRetry(order.value.id, order.value.status); await load({ silent: true }); }
  catch (error) { toast.error(error.message || 'Không thể bắt đầu giao lại'); if (error.status === 409) { saving.value = false; await load({ silent: true }); } }
  finally { saving.value = false; }
}
async function openCancelModal(event) {
  previousFocus = event.currentTarget;
  previousBodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  cancelReason.value = '';
  showCancelModal.value = true;
  await nextTick();
  cancelReasonInput.value?.focus();
}
function closeCancelModal() {
  if (saving.value) return;
  showCancelModal.value = false;
  document.body.style.overflow = previousBodyOverflow;
  previousFocus?.focus();
}
function handleCancelKeydown(event) { trapDialogFocus(event, cancelDialog.value, closeCancelModal); }
async function cancelOrder() { if (!cancelReason.value.trim()) return; await updateStatus('CANCELLED', cancelReason.value.trim()); closeCancelModal(); }
async function saveInternalNote() {
  const note = internalNote.value.trim();
  if (!order.value || !note || saving.value || stopped) return;
  const requestGeneration = generation;
  saving.value = true;
  try {
    await staffStore.saveInternalNote(order.value.id, note);
    if (!acceptsRequest(requestGeneration)) return;
    internalNote.value = '';
    saving.value = false;
    await load({ silent: true });
  } catch (error) {
    if (acceptsRequest(requestGeneration)) toast.error(error.message || 'Không thể lưu ghi chú nội bộ');
  } finally {
    if (acceptsRequest(requestGeneration)) saving.value = false;
  }
}
function printInvoice() { window.print(); }
onMounted(() => { loading.value = false; load(); });
onBeforeUnmount(() => {
  stopped = true;
  generation += 1;
  document.body.style.overflow = previousBodyOverflow;
});
</script>

<template>
  <div v-if="loading" class="staff-state"><span class="spinner"></span> Đang tải đơn hàng...</div>
  <div v-else-if="loadError && !order" class="staff-state staff-error"><span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
  <div v-else-if="order">
    <div class="page-header"><div class="title-wrap"><h1>Đơn hàng {{ order.orderCode }}</h1><OrderStatusBadge :status="order.status" /></div><button class="btn btn-sm btn-outline no-print" @click="printInvoice"><i class="bi bi-printer"></i> In hóa đơn</button></div>
    <div v-if="loadError" class="inline-error" role="alert">{{ loadError }} <button class="btn btn-sm btn-outline" @click="load({ silent: true })">Thử lại</button></div>
    <div class="grid-2"><section class="card"><h3>Thông tin đơn hàng</h3><div class="info-row"><span>Khách hàng</span><strong>{{ order.customerName }}</strong></div><div class="info-row"><span>Điện thoại</span><strong><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><span v-else>Không có</span></strong></div><div class="info-row"><span>Địa chỉ</span><strong>{{ order.shippingAddress }}</strong></div><div class="info-row"><span>Phương thức</span><strong>{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }}</strong></div><div class="info-row"><span>Thanh toán</span><strong :class="{ paid: order.paymentStatus === 'PAID' }">{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</strong></div><div class="info-row"><span>Ghi chú giao hàng</span><strong>{{ order.note || 'Không có' }}</strong></div><div class="info-row"><span>Hoàn tiền</span><strong>{{ order.refundStatus || 'Không có' }}{{ order.refundAmount ? ` · ${formatPrice(order.refundAmount)}` : '' }}{{ order.refundedAt ? ` · ${formatDateTime(order.refundedAt)}` : '' }}{{ order.refundNote ? ` · ${order.refundNote}` : '' }}</strong></div><div v-if="order.failureReason" class="info-row"><span>Lý do hủy</span><strong class="danger">{{ order.failureReason }}</strong></div></section>
      <section class="card"><h3>Thao tác</h3><div class="actions"><button v-if="allowedActions.includes('CONFIRMED')" class="btn btn-primary" :disabled="saving" @click="updateStatus('CONFIRMED')">Xác nhận</button><p v-if="pendingPayment" class="payment-wait">Chờ khách thanh toán PayOS</p><button v-if="allowedActions.includes('PREPARING')" class="btn btn-primary" :disabled="saving" @click="updateStatus('PREPARING')">Bắt đầu chế biến</button><button v-if="allowedActions.includes('READY')" class="btn btn-success" :disabled="saving" @click="updateStatus('READY')">Hoàn thành</button><button v-if="allowedActions.includes('CANCELLED')" class="btn btn-outline danger-button" :disabled="saving" @click="openCancelModal">Hủy đơn</button></div>
        <div v-if="order.status === 'DELIVERY_FAILED'" class="recovery-card" aria-live="polite"><h4>Xử lý giao chưa thành công</h4><p><strong>Lý do:</strong> {{ DELIVERY_FAILURE_REASON_LABEL[order.deliveryFailureCode] || order.deliveryFailureCode }}</p><p v-if="order.failureNote"><strong>Ghi chú:</strong> {{ order.failureNote }}</p><p>Thất bại lúc {{ formatDateTime(order.deliveryFailedAt) }} · Lần {{ order.deliveryAttemptCount }} / {{ order.deliveryAttemptLimit }}</p><p v-if="order.retryScheduledAt">Lịch giao lại: {{ formatDateTime(order.retryScheduledAt) }}</p><div class="actions"><button v-if="order.retryScheduledAt && !atEffectiveLimit" class="btn btn-primary" :disabled="saving" @click="startScheduledRetry">Bắt đầu giao lại</button><button v-if="!atEffectiveLimit" class="btn btn-primary" :disabled="saving" @click="openRecovery($event)">Giao lại</button><button class="btn btn-outline danger-button" :disabled="saving" @click="openRecovery($event, true)">Trả về cửa hàng</button></div></div>
        <div v-if="allowedActions.includes('ASSIGNED') || assignedShipper" class="shipper-panel"><h4>Giao shipper</h4><p v-if="assignedShipper" class="assigned">{{ assignedShipper }}</p><button v-else class="btn btn-primary btn-sm" :disabled="saving" @click="openAssignmentDialog">Chọn shipper</button></div>
      </section></div>
    <section class="card mt-3"><h3>Sản phẩm</h3><div class="table-wrapper"><table class="table"><thead><tr><th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr></thead><tbody><tr v-for="item in order.items" :key="`${item.productId}-${item.variantId}`"><td><strong>{{ item.productName }}</strong><ul v-if="item.modifiers.length" class="modifiers"><li v-for="modifier in item.modifiers" :key="`${modifier.id || modifier.name}-${modifier.price || 0}`">{{ modifier.name }}<span v-if="modifier.price"> +{{ formatPrice(modifier.price) }}</span></li></ul></td><td>{{ item.variantName || 'Mặc định' }}</td><td>{{ formatPrice(item.price) }}</td><td>{{ item.quantity }}</td><td><strong>{{ formatPrice(item.totalPrice) }}</strong></td></tr></tbody></table></div><div class="order-totals"><div>Tạm tính: {{ formatPrice(order.subtotal) }}</div><div>Phí ship: {{ formatPrice(order.shippingFee) }}</div><div>Phí dịch vụ: {{ formatPrice(order.serviceFee) }}</div><div>Giảm giá: -{{ formatPrice(order.discount) }}</div><strong>Tổng: {{ formatPrice(order.total) }}</strong></div></section>
    <section class="card mt-3"><h3>Ghi chú nội bộ</h3><ul v-if="order.internalNotes.length" class="notes"><li v-for="(note, index) in order.internalNotes" :key="note.id || index">{{ note.content || note.note || note }}</li></ul><form class="note-form no-print" @submit.prevent="saveInternalNote"><label class="form-label" for="internal-note">Thêm ghi chú</label><textarea id="internal-note" v-model="internalNote" class="form-textarea" rows="3" maxlength="1000" :disabled="saving"></textarea><button class="btn btn-primary btn-sm" type="submit" :disabled="saving || !internalNote.trim()">{{ saving ? 'Đang lưu...' : 'Lưu ghi chú' }}</button></form></section>
    <section class="card mt-3"><h3>Lịch sử trạng thái</h3><OrderTimeline :history="order.statusHistory" /></section>
    <div v-if="showAssignmentDialog" class="modal-overlay"><div ref="assignmentDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="assignment-title" @keydown="handleAssignmentKeydown"><div class="modal-header"><h2 id="assignment-title" class="modal-title">Giao shipper</h2><button class="modal-close" aria-label="Đóng" :disabled="saving" @click="closeAssignmentDialog"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><div class="modal-body"><div v-if="shipperLoading" class="compact-state" role="status"><span class="spinner"></span> Đang tải shipper...</div><div v-else-if="shipperError" class="compact-state error" role="alert">{{ shipperError }} <button class="btn btn-sm btn-outline" @click="loadShippers">Thử lại</button></div><p v-else-if="!shippers.length" class="compact-state">Không có shipper khả dụng</p><div v-else><label class="form-label" for="assignment-shipper">Shipper</label><select id="assignment-shipper" ref="assignmentSelect" v-model="selectedShipperId" class="form-select" :disabled="saving"><option :value="null">Chọn shipper</option><option v-for="shipper in shippers" :key="shipper.id" :value="shipper.id">{{ shipper.fullName }} · {{ shipper.phone }}</option></select></div><div class="modal-footer"><button class="btn btn-ghost" :disabled="saving" @click="closeAssignmentDialog">Quay lại</button><button class="btn btn-primary" :disabled="saving || !selectedShipperId" @click="assignShipper">{{ saving ? 'Đang gán...' : 'Gán shipper' }}</button></div></div></div></div>
    <div v-if="showRecoveryDialog" class="modal-overlay"><div ref="recoveryDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="recovery-title" @keydown="handleRecoveryKeydown"><div class="modal-header"><h2 id="recovery-title" class="modal-title">{{ showReturnConfirmation ? 'Trả đơn về cửa hàng' : 'Thiết lập giao lại' }}</h2><button class="modal-close" aria-label="Đóng" :disabled="saving" @click="closeRecovery"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><form class="modal-body recovery-form" @submit.prevent="submitRecovery"><template v-if="showReturnConfirmation"><p>Hàng hóa sẽ được ghi nhận hao hụt.</p><p>Mã giảm giá sẽ được giải phóng.</p><p>Thanh toán online có thể cần hoàn tiền.</p></template><template v-else><label for="recovery-shipper">Shipper thay thế</label><select id="recovery-shipper" v-model="recoveryShipper" class="form-select" required><option :value="null">Chọn shipper</option><option v-for="shipper in shippers" :key="shipper.id" :value="shipper.id">{{ shipper.fullName }} · {{ shipper.phone }}</option></select><fieldset><legend>Thời điểm giao lại</legend><label><input v-model="retryMode" type="radio" value="IMMEDIATE" :disabled="order.deliveryFailureCode === 'CUSTOMER_RESCHEDULED'"> Ngay</label><label><input v-model="retryMode" type="radio" value="SCHEDULED"> Theo lịch</label></fieldset><label v-if="retryMode === 'SCHEDULED'" for="recovery-schedule">Lịch giao lại</label><input v-if="retryMode === 'SCHEDULED'" id="recovery-schedule" v-model="scheduledAt" class="form-input" type="datetime-local" required></template><label for="recovery-note">Ghi chú *</label><textarea id="recovery-note" v-model="recoveryNote" class="form-textarea" maxlength="500" required :aria-invalid="Boolean(recoveryError)" aria-describedby="recovery-error"></textarea><p id="recovery-error" class="inline-error" aria-live="polite">{{ recoveryError }}</p><div class="modal-footer"><button type="button" class="btn btn-ghost" :disabled="saving" @click="closeRecovery">Quay lại</button><button type="submit" class="btn btn-primary" :disabled="saving || !recoveryValid">{{ saving ? 'Đang xử lý...' : 'Xác nhận' }}</button></div></form></div></div>
    <div v-if="showCancelModal" class="modal-overlay" @click.self="closeCancelModal"><div ref="cancelDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="cancel-title" @keydown="handleCancelKeydown"><div class="modal-header"><h2 id="cancel-title" class="modal-title">Hủy đơn hàng</h2><button class="modal-close" aria-label="Đóng" :disabled="saving" @click="closeCancelModal"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><div class="modal-body"><label class="form-label" for="cancel-reason">Lý do hủy *</label><textarea id="cancel-reason" ref="cancelReasonInput" v-model="cancelReason" class="form-textarea" rows="3" maxlength="500"></textarea><div class="modal-footer"><button class="btn btn-ghost" :disabled="saving" @click="closeCancelModal">Quay lại</button><button class="btn btn-danger" :disabled="saving || !cancelReason.trim()" @click="cancelOrder">{{ saving ? 'Đang hủy...' : 'Xác nhận hủy' }}</button></div></div></div></div>
  </div>
</template>

<style scoped>
.staff-state{display:flex;align-items:center;justify-content:center;gap:10px;min-height:240px;color:var(--text-mid)}.staff-error,.compact-state.error,.inline-error{color:var(--red-active)}.staff-error{flex-direction:column}.inline-error{margin-bottom:12px}.title-wrap{display:flex;align-items:center;gap:12px;flex-wrap:wrap}.card h3{margin-bottom:16px;font-size:16px}.info-row{display:flex;justify-content:space-between;gap:16px;padding:10px 0;border-bottom:1px solid var(--border-light);font-size:14px}.info-row span{color:var(--text-mid)}.info-row strong{max-width:62%;text-align:right}.paid,.assigned{color:#15803d}.danger{color:var(--red-active)}.actions{display:flex;flex-wrap:wrap;gap:8px}.danger-button{color:var(--red-active);border-color:var(--red-active)}.payment-wait{width:100%;margin:0;padding:10px 12px;background:#fff7ed;color:#c2410c}.shipper-panel,.recovery-card{margin-top:18px;padding-top:16px;border-top:1px solid var(--border-light)}.recovery-form button,.recovery-form select,.recovery-form input,.recovery-form textarea{min-height:44px}.recovery-form fieldset{margin:12px 0;border:0;padding:0}.recovery-form fieldset label{display:inline-flex;align-items:center;gap:6px;margin-right:16px}.recovery-form input[type="radio"]{min-height:auto}.shipper-form{display:flex;gap:8px}.compact-state{display:flex;align-items:center;gap:8px}.modifiers,.notes{margin:6px 0 0;padding-left:18px;color:var(--text-mid);font-size:13px}.note-form{margin-top:16px}.note-form .btn{margin-top:8px}.order-totals{margin-top:16px;color:var(--text-mid);line-height:1.9;text-align:right}.order-totals strong{display:block;color:var(--text-dark);font-size:19px}@media print{.no-print,.actions,.shipper-panel{display:none!important}}@media(max-width:640px){.shipper-form{flex-direction:column}.info-row strong{max-width:58%}}
</style>
