<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { formatPrice, formatDate } from '@/utils/format';
import { inlineOrderActionMeta, inlineOrderActions, paymentMethodLabel, paymentStatusLabel } from '@/utils/adminOrderWorkspace';

const props = defineProps({
  open: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  order: { type: Object, default: null },
  error: { type: String, default: '' },
  busy: { type: Boolean, default: false },
  actionError: { type: String, default: '' },
  actionMessage: { type: String, default: '' },
  pendingAction: { type: String, default: '' },
  actionNote: { type: String, default: '' },
});
const emit = defineEmits(['close', 'select-action', 'update:action-note', 'cancel-action', 'confirm-action']);
const dialogRef = ref(null);
const closeRef = ref(null);
const noteRef = ref(null);
let previousOverflow = '';
const actions = computed(() => inlineOrderActions(props.order?.allowedActions));
const pendingMeta = computed(() => inlineOrderActionMeta(props.pendingAction));
const actionStillAllowed = computed(() => actions.value.some(action => action.key === props.pendingAction));
const canConfirm = computed(() => pendingMeta.value && actionStillAllowed.value && (!pendingMeta.value.requiresNote || props.actionNote.trim()));
const createdHistory = computed(() => props.order?.createdAt ? `Đơn được tạo lúc ${formatDate(props.order.createdAt)}` : 'Chưa có lịch sử trạng thái.');

function requestClose() {
  if (!props.busy) emit('close');
}
function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    requestClose();
    return;
  }
  if (event.key !== 'Tab' || !dialogRef.value) return;
  const focusable = [...dialogRef.value.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (!dialogRef.value.contains(document.activeElement)) {
    event.preventDefault();
    (event.shiftKey ? last : first).focus();
  } else if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}
watch(() => props.open, async open => {
  if (open) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeydown);
    await nextTick();
    closeRef.value?.focus();
  } else {
    document.removeEventListener('keydown', handleKeydown);
    document.body.style.overflow = previousOverflow;
  }
});
watch(() => props.pendingAction, async action => {
  if (!action) return;
  await nextTick();
  (pendingMeta.value?.requiresNote ? noteRef.value : dialogRef.value?.querySelector('.confirm-action'))?.focus();
});
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown);
  if (props.open) document.body.style.overflow = previousOverflow;
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="order-drawer-backdrop" @mousedown.self="requestClose">
      <aside ref="dialogRef" class="order-drawer" role="dialog" aria-modal="true" aria-labelledby="order-drawer-title">
        <header class="drawer-header">
          <div>
            <small>Chi tiết đơn hàng</small>
            <h2 id="order-drawer-title">{{ order?.orderCode || 'Đang tải' }}</h2>
            <div v-if="order" class="drawer-statuses"><OrderStatusBadge :status="order.status" /><span>{{ paymentMethodLabel(order.paymentMethod) }} · {{ paymentStatusLabel(order.paymentStatus) }}</span></div>
          </div>
          <button ref="closeRef" class="order-drawer-close" type="button" aria-label="Đóng chi tiết đơn hàng" :disabled="busy" @click="requestClose"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </header>

        <div class="drawer-scroll">
          <div v-if="loading" class="drawer-state" role="status">Đang tải chi tiết...</div>
          <div v-else-if="error" class="drawer-state error" role="alert">{{ error }}</div>
          <template v-else-if="order">
            <section aria-labelledby="drawer-customer"><h3 id="drawer-customer">Khách hàng</h3><strong>{{ order.customerName || 'Chưa có tên khách hàng' }}</strong><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><span v-else>Chưa có số điện thoại</span></section>
            <section aria-labelledby="drawer-delivery"><h3 id="drawer-delivery">Giao hàng</h3><p>{{ order.customerAddress || 'Chưa có địa chỉ giao hàng' }}</p><p v-if="order.deliveryNote">Ghi chú: {{ order.deliveryNote }}</p><p v-if="order.staffName || order.shipperName">Nhân sự: {{ [order.staffName, order.shipperName].filter(Boolean).join(' · ') }}</p></section>
            <section aria-labelledby="drawer-items"><h3 id="drawer-items">Món trong đơn</h3><ul class="drawer-items"><li v-for="(item, index) in order.items" :key="`${item.productName}-${item.variantName}-${index}`"><img v-if="item.imageUrl" :src="item.imageUrl" alt="" /><span v-else class="item-fallback" aria-hidden="true"><i class="bi bi-basket"></i></span><div><strong>{{ item.productName || 'Sản phẩm' }}</strong><small>{{ item.variantName || 'Tiêu chuẩn' }} · ×{{ item.quantity }}</small></div><span><small>{{ formatPrice(item.unitPrice || 0) }}</small><strong>{{ formatPrice(item.totalPrice || 0) }}</strong></span></li></ul></section>
            <section aria-labelledby="drawer-payment"><h3 id="drawer-payment">Thanh toán</h3><dl class="payment-breakdown"><div><dt>Tạm tính</dt><dd>{{ formatPrice(order.totalAmount || 0) }}</dd></div><div><dt>Phí giao hàng</dt><dd>{{ formatPrice(order.shippingFee || 0) }}</dd></div><div><dt>Giảm giá</dt><dd>−{{ formatPrice(order.discountAmount || 0) }}</dd></div><div class="total"><dt>Tổng cộng</dt><dd>{{ formatPrice(order.finalAmount || 0) }}</dd></div></dl></section>
            <section aria-labelledby="drawer-history"><h3 id="drawer-history">Lịch sử đơn hàng</h3><OrderTimeline v-if="order.statusHistory?.length" :history="order.statusHistory" /><p v-else>{{ createdHistory }}</p></section>
            <router-link class="full-detail-link" :to="`/admin/orders/${order.orderId}`">Mở trang đầy đủ <i class="bi bi-arrow-up-right" aria-hidden="true"></i></router-link>
          </template>
        </div>

        <footer v-if="order" class="drawer-actions">
          <p v-if="actionMessage" class="action-message" role="status" aria-live="polite">{{ actionMessage }}</p>
          <p v-if="actionError" class="action-error" role="alert">{{ actionError }}</p>
          <template v-if="pendingMeta">
            <label v-if="pendingMeta.requiresNote" for="drawer-action-note">Lý do hủy đơn</label>
            <textarea v-if="pendingMeta.requiresNote" id="drawer-action-note" ref="noteRef" :value="actionNote" maxlength="500" rows="3" @input="emit('update:action-note', $event.target.value)"></textarea>
            <p>Xác nhận: {{ pendingMeta.label }}?</p>
            <div><button type="button" class="btn btn-outline" :disabled="busy" @click="emit('cancel-action')">Quay lại</button><button type="button" :class="['btn', pendingMeta.tone === 'danger' ? 'btn-danger' : 'btn-primary', 'confirm-action']" :disabled="busy || !canConfirm" @click="emit('confirm-action')">{{ busy ? 'Đang xử lý...' : pendingMeta.label }}</button></div>
          </template>
          <template v-else>
            <router-link v-if="order.refundStatus === 'PENDING'" class="btn btn-outline" :to="{ path: '/admin/refunds', query: { status: 'PENDING' } }">Xử lý hoàn tiền</router-link>
            <button v-for="action in actions" :key="action.key" type="button" :class="['btn', action.tone === 'danger' ? 'btn-outline-danger' : 'btn-primary']" @click="emit('select-action', action.key)">{{ action.label }}</button>
          </template>
        </footer>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.order-drawer-backdrop{position:fixed;z-index:120;inset:0;display:flex;justify-content:flex-end;background:rgba(23,33,43,.36)}
.order-drawer{display:grid;grid-template-rows:auto minmax(0,1fr) auto;width:min(460px,100%);height:100dvh;border-radius:20px 0 0 20px;background:var(--admin-surface);box-shadow:-12px 0 32px rgba(23,33,43,.14);overflow:hidden;color:var(--admin-foreground)}
.drawer-header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:20px;border-bottom:1px solid var(--admin-border)}
.drawer-header small{color:var(--admin-muted);font-size:12px}.drawer-header h2{margin:3px 0 8px}.drawer-statuses{display:flex;align-items:center;gap:7px;flex-wrap:wrap}.drawer-statuses>span{padding:5px 9px;border-radius:999px;background:var(--admin-canvas);font-size:12px;font-weight:650}
.order-drawer-close{width:44px;height:44px;border-radius:10px}.order-drawer :is(button,a,textarea):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}
.drawer-scroll{display:grid;align-content:start;gap:18px;overflow:auto;padding:18px 20px}.drawer-scroll section{display:grid;gap:8px}.drawer-scroll h3{margin:0;color:var(--admin-muted);font-size:12px;letter-spacing:.04em;text-transform:uppercase}.drawer-scroll p{margin:0;line-height:1.5}.drawer-scroll a{color:var(--admin-brand);font-weight:650}
.drawer-state{display:grid;min-height:240px;place-items:center;color:var(--admin-muted)}.drawer-state.error{color:var(--admin-danger)}
.drawer-items{display:grid;gap:10px;margin:0;padding:0;list-style:none}.drawer-items li{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid var(--admin-border)}.drawer-items img,.item-fallback{width:48px;height:48px;border-radius:12px;object-fit:cover}.item-fallback{display:grid;place-items:center;background:var(--admin-canvas)}.drawer-items li>div,.drawer-items li>span:last-child{display:grid;gap:3px}.drawer-items li>span:last-child{text-align:right}.drawer-items small{color:var(--admin-muted)}
.payment-breakdown{display:grid;gap:9px;margin:0}.payment-breakdown div{display:flex;justify-content:space-between;gap:16px}.payment-breakdown dd{margin:0;font-variant-numeric:tabular-nums}.payment-breakdown .total{padding-top:10px;border-top:1px solid var(--admin-border);font-weight:750}.full-detail-link{display:inline-flex;align-items:center;gap:6px;width:max-content;min-height:44px}
.drawer-actions{display:flex;justify-content:flex-end;gap:8px;flex-wrap:wrap;padding:14px 20px calc(14px + env(safe-area-inset-bottom));border-top:1px solid var(--admin-border);background:var(--admin-surface);box-shadow:0 -8px 24px rgba(23,33,43,.05)}.drawer-actions>p,.drawer-actions>label,.drawer-actions>textarea{flex-basis:100%;margin:0}.drawer-actions>div{display:flex;justify-content:flex-end;gap:8px;width:100%}.drawer-actions textarea{width:100%;resize:vertical}.action-message{color:var(--admin-success)}.action-error{color:var(--admin-danger)}.btn-outline-danger{min-height:40px;padding:0 14px;border:1px solid var(--admin-danger);border-radius:10px;color:var(--admin-danger)}
@media(max-width:640px){.order-drawer{width:100%;border-radius:0}.drawer-header,.drawer-scroll,.drawer-actions{padding-inline:16px}.drawer-actions>.btn{flex:1}}
@media(prefers-reduced-motion:no-preference){.order-drawer{animation:drawer-in .18s ease-out}@keyframes drawer-in{from{transform:translateX(18px);opacity:.8}to{transform:none;opacity:1}}}
</style>
