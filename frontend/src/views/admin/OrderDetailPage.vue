<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import { paymentMethodLabel, paymentStatusLabel } from '@/utils/adminOrderWorkspace';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { useToast } from '@/stores/toast';

const toast = useToast();
const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const loadError = ref('');
const saving = ref(false);
const showCancelModal = ref(false);
const cancelReason = ref('');
const showNoteModal = ref(false);
const noteText = ref('');
const overrideNote = ref('');
const overrideError = ref('');

const canCancel = computed(() => order.value?.allowedActions?.includes('CANCELLED'));
const canPrint = computed(() => true);

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    order.value = await adminApi.getOrderById(route.params.id);
    if (!order.value) loadError.value = 'Không tìm thấy đơn hàng';
  } catch (e) {
    loadError.value = e.message || 'Không thể tải đơn hàng';
  } finally {
    loading.value = false;
  }
}

async function cancelOrder() {
  if (!cancelReason.value.trim()) return;
  saving.value = true;
  try {
    await adminApi.cancelOrder(order.value.orderId, { expectedStatus: order.value.status, reason: cancelReason.value.trim() });
    toast.success('Đã hủy đơn hàng');
    showCancelModal.value = false;
    await load();
  } catch (e) { if (e.status === 409) { await load(); toast.error('Đơn hàng đã thay đổi. Kiểm tra dữ liệu mới rồi gửi lại.'); } else toast.error(e.message); }
  finally { saving.value = false; }
}

async function overrideDeliveryAttempt() {
  if (!overrideNote.value.trim() || saving.value) return;
  saving.value = true;
  overrideError.value = '';
  try {
    await adminApi.overrideDeliveryAttempt(order.value.orderId, order.value.status, overrideNote.value.trim());
    overrideNote.value = '';
    await load();
  } catch (e) {
    overrideError.value = e.message || 'Không thể mở thêm lượt giao';
    if (e.status === 409) await load();
  } finally { saving.value = false; }
}

async function saveNote() {
  if (!noteText.value.trim()) return;
  saving.value = true;
  try {
    await adminApi.addOrderNote(order.value.orderId, order.value.status, noteText.value.trim());
    toast.success('Đã lưu ghi chú');
    showNoteModal.value = false;
    noteText.value = '';
    await load();
  } catch (e) { if (e.status === 409) { await load(); toast.error('Đơn hàng đã thay đổi. Ghi chú vẫn được giữ để gửi lại.'); } else toast.error(e.message); }
  finally { saving.value = false; }
}

function printInvoice() { window.print(); }

onMounted(load);
</script>

<template>
  <div v-if="loading" class="detail-state" role="status">Đang tải chi tiết...</div>
  <div v-else-if="loadError" class="detail-state detail-state-error" role="alert"><p>{{ loadError }}</p><button class="btn btn-outline" type="button" @click="router.back()">Quay lại</button></div>
  <main v-else-if="order" class="order-detail-page">
    <header class="order-detail-identity">
      <button class="back-link no-print" type="button" @click="router.back()"><i class="bi bi-arrow-left" aria-hidden="true"></i><span>Quay lại danh sách</span></button>
      <div class="identity-row"><div><small>Chi tiết đơn hàng</small><h1>{{ order.orderCode }}</h1></div><div class="identity-status"><OrderStatusBadge :status="order.status" /><span>{{ paymentMethodLabel(order.paymentMethod) }} · {{ paymentStatusLabel(order.paymentStatus) }}</span></div></div>
    </header>

    <div class="detail-layout">
      <div class="detail-primary">
        <section class="detail-section order-detail-fulfillment" aria-labelledby="detail-fulfillment"><h2 id="detail-fulfillment">Khách hàng & giao nhận</h2><div class="fact-grid"><div><span>Khách hàng</span><strong>{{ order.customerName || 'Chưa có tên khách hàng' }}</strong></div><div><span>Điện thoại</span><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><strong v-else>Chưa có số điện thoại</strong></div><div class="wide"><span>Địa chỉ giao hàng</span><strong>{{ order.customerAddress || 'Chưa có địa chỉ giao hàng' }}</strong></div><div v-if="order.deliveryNote" class="wide"><span>Ghi chú giao hàng</span><strong>{{ order.deliveryNote }}</strong></div><div v-if="order.staffName || order.shipperName" class="wide"><span>Nhân sự</span><strong>{{ [order.staffName, order.shipperName].filter(Boolean).join(' · ') }}</strong></div></div><template v-if="order.status === 'CANCELLED'"><div class="fact-divider"></div><div class="fact-grid"><div><span>Hủy bởi</span><strong>{{ order.cancelledBy === 'STAFF' ? 'Nhân viên' : order.cancelledBy === 'ADMIN' ? 'Quản trị' : order.cancelledBy === 'SHIPPER' ? 'Shipper' : 'Khách' }}</strong></div><div v-if="order.failureReason"><span>Lý do</span><strong>{{ order.failureReason }}</strong></div><div v-if="order.refundStatus" class="wide"><span>Hoàn tiền</span><strong>{{ order.refundStatus }}{{ order.refundNote ? ` · ${order.refundNote}` : '' }}</strong></div></div></template><template v-if="['DELIVERY_FAILED', 'RETURNED_TO_STORE'].includes(order.status)"><div class="fact-divider"></div><div class="fact-grid"><div><span>Mã lỗi giao</span><strong>{{ order.deliveryFailureCode || 'Không có' }}</strong></div><div><span>Số lần giao</span><strong>{{ order.deliveryAttemptCount }} / {{ order.deliveryAttemptLimit }}</strong></div><div class="wide"><span>Ghi chú thất bại</span><strong>{{ order.failureNote || 'Không có' }}</strong></div><div v-if="order.retryScheduledAt" class="wide"><span>Lịch giao lại</span><strong>{{ formatDate(order.retryScheduledAt) }}</strong></div></div></template></section>

        <section class="detail-section order-detail-items" aria-labelledby="detail-items"><h2 id="detail-items">Món trong đơn</h2><ul class="item-list"><li v-for="(item, idx) in order.items" :key="idx"><span class="item-fallback" aria-hidden="true"><i class="bi bi-basket"></i></span><div><strong>{{ item.productName }}</strong><small>{{ item.variantName || 'Tiêu chuẩn' }} · ×{{ item.quantity }}</small></div><span><small>{{ formatPrice(item.unitPrice) }}</small><strong>{{ formatPrice(item.totalPrice) }}</strong></span></li></ul></section>

        <section class="detail-section order-detail-payment" aria-labelledby="detail-payment"><div class="section-heading"><h2 id="detail-payment">Thanh toán</h2><span>{{ paymentMethodLabel(order.paymentMethod) }} · {{ paymentStatusLabel(order.paymentStatus) }}</span></div><dl class="payment-breakdown"><div><dt>Tạm tính</dt><dd>{{ formatPrice(order.totalAmount || 0) }}</dd></div><div><dt>Phí giao hàng</dt><dd>{{ formatPrice(order.shippingFee || 0) }}</dd></div><div><dt>Giảm giá</dt><dd>−{{ formatPrice(order.discountAmount || 0) }}</dd></div><div class="total"><dt>Tổng cộng</dt><dd>{{ formatPrice(order.finalAmount) }}</dd></div></dl><dl v-if="order.payment" class="payment-meta"><div><dt>Nhà cung cấp</dt><dd>{{ order.payment.provider }}</dd></div><div><dt>Mã tham chiếu</dt><dd>{{ order.payment.providerReference }}</dd></div><div><dt>Trạng thái giao dịch</dt><dd>{{ order.payment.attemptStatus }}</dd></div><div><dt>Số tiền giao dịch</dt><dd>{{ formatPrice(order.payment.attemptAmount) }}</dd></div></dl></section>

        <section class="detail-section order-detail-timeline" aria-labelledby="detail-timeline"><h2 id="detail-timeline">Lịch sử đơn hàng</h2><OrderTimeline v-if="order.statusHistory?.length" :history="order.statusHistory" /><p v-else>Đơn được tạo lúc {{ formatDate(order.createdAt) }}.</p></section>
      </div>

      <aside class="detail-secondary">
        <section v-if="order.internalNote" class="detail-section"><h2>Ghi chú nội bộ</h2><p class="internal-note">{{ order.internalNote }}</p></section>
        <section v-if="order.status === 'DELIVERY_FAILED'" class="detail-section"><h2>Mở thêm lượt giao</h2><form @submit.prevent="overrideDeliveryAttempt"><label class="form-label" for="override-note">Lý do quản trị *</label><textarea id="override-note" v-model="overrideNote" class="form-textarea" maxlength="500" required :aria-invalid="Boolean(overrideError)" aria-describedby="override-error"></textarea><p id="override-error" class="field-error" aria-live="polite">{{ overrideError }}</p><button class="btn btn-primary" type="submit" :disabled="saving || !overrideNote.trim()">Mở thêm lượt giao</button></form></section>
        <section class="detail-section order-detail-actions" aria-labelledby="detail-actions"><h2 id="detail-actions">Thao tác đơn hàng</h2><div class="action-stack"><button v-if="canCancel" class="btn btn-danger" type="button" @click="showCancelModal = true">Hủy đơn</button><button class="btn btn-outline" type="button" @click="showNoteModal = true">Thêm ghi chú</button><button class="btn btn-outline no-print" type="button" @click="printInvoice">In đơn hàng</button></div></section>
      </aside>
    </div>

    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false"><div class="modal"><div class="modal-header"><h3>Hủy đơn {{ order.orderCode }}</h3><button class="btn btn-sm btn-outline" type="button" aria-label="Đóng" @click="showCancelModal = false"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><div class="modal-body"><div class="form-group"><label class="form-label">Lý do hủy *</label><textarea v-model="cancelReason" class="form-textarea" rows="3" placeholder="Nhập lý do hủy đơn" maxlength="500"></textarea></div></div><div class="modal-footer"><button class="btn btn-outline" type="button" @click="showCancelModal = false">Quay lại</button><button class="btn btn-danger" type="button" :disabled="saving || !cancelReason.trim()" @click="cancelOrder">{{ saving ? 'Đang hủy...' : 'Xác nhận hủy' }}</button></div></div></div>
    <div v-if="showNoteModal" class="modal-overlay" @click.self="showNoteModal = false"><div class="modal"><div class="modal-header"><h3>Thêm ghi chú</h3><button class="btn btn-sm btn-outline" type="button" aria-label="Đóng" @click="showNoteModal = false"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><div class="modal-body"><div class="form-group"><label class="form-label">Nội dung ghi chú</label><textarea v-model="noteText" class="form-textarea" rows="3" placeholder="Nhập ghi chú cho đơn hàng" maxlength="500"></textarea></div></div><div class="modal-footer"><button class="btn btn-outline" type="button" @click="showNoteModal = false">Hủy</button><button class="btn btn-primary" type="button" :disabled="saving || !noteText.trim()" @click="saveNote">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button></div></div></div>
  </main>
</template>

<style scoped>
.detail-state{display:grid;min-height:320px;place-items:center;color:var(--admin-muted)}.detail-state-error{align-content:center;gap:16px;color:var(--admin-danger)}.detail-state-error p{margin:0}.order-detail-page{display:grid;gap:20px;color:var(--admin-foreground)}.order-detail-identity{display:grid;gap:18px;padding-bottom:20px;border-bottom:1px solid var(--admin-border)}.back-link{display:inline-flex;align-items:center;gap:8px;width:max-content;min-height:44px;border:0;background:transparent;color:var(--admin-brand);font-weight:700}.identity-row{display:flex;align-items:end;justify-content:space-between;gap:20px}.identity-row small,.detail-section h2{color:var(--admin-muted);font-size:12px;letter-spacing:.04em;text-transform:uppercase}.identity-row h1{margin:4px 0 0;font-size:clamp(28px,4vw,42px);letter-spacing:-.04em}.identity-status{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.identity-status>span,.section-heading>span{padding:6px 10px;border-radius:999px;background:var(--admin-canvas);font-size:12px;font-weight:700}.detail-layout{display:grid;grid-template-columns:minmax(0,1fr) minmax(260px,320px);gap:20px;align-items:start}.detail-primary,.detail-secondary{display:grid;gap:16px}.detail-secondary{position:sticky;top:20px}.detail-section{padding:20px;border:1px solid var(--admin-border);border-radius:16px;background:var(--admin-surface)}.detail-section h2{margin:0 0 16px}.fact-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.fact-grid>div{display:grid;gap:5px}.fact-grid .wide{grid-column:1/-1}.fact-grid span{color:var(--admin-muted);font-size:12px}.fact-grid a{color:var(--admin-brand);font-weight:700}.fact-divider{height:1px;margin:18px 0;background:var(--admin-border)}.item-list{display:grid;gap:4px;margin:0;padding:0;list-style:none}.item-list li{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--admin-border)}.item-list li:last-child{border-bottom:0}.item-fallback{display:grid;width:48px;height:48px;place-items:center;border-radius:12px;background:var(--admin-canvas)}.item-list li>div,.item-list li>span:last-child{display:grid;gap:4px}.item-list li>span:last-child{text-align:right}.item-list small{color:var(--admin-muted)}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.payment-breakdown,.payment-meta{display:grid;gap:10px;margin:0}.payment-breakdown div,.payment-meta div{display:flex;justify-content:space-between;gap:16px}.payment-breakdown dd,.payment-meta dd{margin:0;text-align:right;font-variant-numeric:tabular-nums}.payment-breakdown .total{padding-top:12px;border-top:1px solid var(--admin-border);font-size:18px;font-weight:800}.payment-meta{margin-top:16px;padding-top:16px;border-top:1px solid var(--admin-border);font-size:13px}.internal-note{margin:0;white-space:pre-wrap;line-height:1.55}.action-stack{display:grid;gap:8px}.action-stack .btn{width:100%;min-height:44px}.modal{width:min(480px,calc(100vw - 32px))}.order-detail-page :is(button,a,textarea):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}@media(max-width:900px){.detail-layout{grid-template-columns:1fr}.detail-secondary{position:static}}@media(max-width:600px){.identity-row{align-items:start;flex-direction:column}.fact-grid{grid-template-columns:1fr}.fact-grid .wide{grid-column:auto}.detail-section{padding:16px}.section-heading{align-items:start;flex-direction:column}.item-list li{grid-template-columns:40px minmax(0,1fr) auto}.item-fallback{width:40px;height:40px}}
</style>
