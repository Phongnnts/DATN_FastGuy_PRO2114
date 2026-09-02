<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { formatPrice, formatDate } from '@/utils/format';
import { adminOrderReturnContext, inlineOrderActionMeta, inlineOrderActions, paymentMethodLabel, paymentStatusLabel } from '@/utils/adminOrderWorkspace';

const route = useRoute();
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
    if (props.busy) return;
    if (props.pendingAction) {
      emit('cancel-action');
      return;
    }
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
    <div v-if="open" class="order-modal-backdrop" @mousedown.self="requestClose">
      <aside ref="dialogRef" class="order-modal" role="dialog" aria-modal="true" aria-labelledby="order-modal-title">
        <header class="modal-header order-drawer-identity">
          <div>
            <small>Chi tiết đơn hàng</small>
            <h2 id="order-modal-title">{{ order?.orderCode || 'Đang tải' }}</h2>
            <div v-if="order" class="modal-statuses"><OrderStatusBadge :status="order.status" /><span>{{ paymentMethodLabel(order.paymentMethod) }} · {{ paymentStatusLabel(order.paymentStatus) }}</span></div>
          </div>
          <button ref="closeRef" class="order-modal-close" type="button" aria-label="Đóng chi tiết đơn hàng" :disabled="busy" @click="requestClose"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </header>

        <div class="modal-scroll">
          <div v-if="loading" class="modal-state" role="status">Đang tải chi tiết...</div>
          <div v-else-if="error" class="modal-state error" role="alert">{{ error }}</div>
          <div v-else-if="order" class="modal-content-grid">
            <div class="modal-facts">
              <section class="order-drawer-fulfillment" aria-labelledby="modal-customer"><h3 id="modal-customer">Khách hàng & giao nhận</h3><strong>{{ order.customerName || 'Chưa có tên khách hàng' }}</strong><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><span v-else>Chưa có số điện thoại</span></section>
              <section aria-labelledby="modal-delivery"><h3 id="modal-delivery">Giao hàng</h3><p>{{ order.customerAddress || 'Chưa có địa chỉ giao hàng' }}</p><p v-if="order.deliveryNote">Ghi chú: {{ order.deliveryNote }}</p><p v-if="order.staffName || order.shipperName">Nhân sự: {{ [order.staffName, order.shipperName].filter(Boolean).join(' · ') }}</p></section>
              <section class="order-drawer-items" aria-labelledby="modal-items"><h3 id="modal-items">Món trong đơn</h3><ul class="modal-items"><li v-for="(item, index) in order.items" :key="`${item.productName}-${item.variantName}-${index}`"><img v-if="item.imageUrl" :src="item.imageUrl" alt="" /><span v-else class="item-fallback" aria-hidden="true"><i class="bi bi-basket"></i></span><div><strong>{{ item.productName || 'Sản phẩm' }}</strong><small>{{ item.variantName || 'Tiêu chuẩn' }} · ×{{ item.quantity }}</small></div><span><small>{{ formatPrice(item.unitPrice || 0) }}</small><strong>{{ formatPrice(item.totalPrice || 0) }}</strong></span></li></ul></section>
              <section class="order-drawer-payment" aria-labelledby="modal-payment"><h3 id="modal-payment">Thanh toán</h3><dl class="payment-breakdown"><div><dt>Tạm tính</dt><dd>{{ formatPrice(order.totalAmount || 0) }}</dd></div><div><dt>Phí giao hàng</dt><dd>{{ formatPrice(order.shippingFee || 0) }}</dd></div><div><dt>Giảm giá</dt><dd>−{{ formatPrice(order.discountAmount || 0) }}</dd></div><div class="total"><dt>Tổng cộng</dt><dd>{{ formatPrice(order.finalAmount || 0) }}</dd></div></dl></section>
              <router-link class="full-detail-link" :to="{ path: `/admin/orders/${order.orderId}`, query: { returnTo: adminOrderReturnContext(route.fullPath) } }">Mở trang đầy đủ <i class="bi bi-arrow-up-right" aria-hidden="true"></i></router-link>
            </div>
            <section class="modal-timeline order-drawer-timeline" aria-labelledby="modal-history"><h3 id="modal-history">Lịch sử đơn hàng</h3><OrderTimeline v-if="order.statusHistory?.length" :history="order.statusHistory" /><p v-else>{{ createdHistory }}</p></section>
          </div>
        </div>

        <footer v-if="order" class="modal-actions order-drawer-actions">
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
.order-modal-backdrop{position:fixed;z-index:120;inset:0;display:grid;place-items:center;padding:24px;background:rgba(238,240,243,.78);backdrop-filter:blur(8px)}
.order-modal{display:grid;grid-template-rows:auto minmax(0,1fr) auto;width:850px;max-width:calc(100vw - 48px);max-height:85dvh;border:1px solid var(--admin-border);border-radius:20px;background:var(--admin-surface-subtle);box-shadow:0 24px 70px rgba(76,83,94,.16);overflow:hidden;color:var(--admin-foreground)}
.modal-header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:20px;border-bottom:1px solid var(--admin-border)}
.modal-header small{color:var(--admin-muted);font-size:12px}.modal-header h2{margin:3px 0 8px}.modal-statuses{display:flex;align-items:center;gap:7px;flex-wrap:wrap}.modal-statuses>span{padding:5px 9px;border-radius:999px;background:var(--admin-canvas);font-size:12px;font-weight:650}
.order-modal-close{width:44px;height:44px;border-radius:10px}.order-modal :is(button,a,textarea){min-height:40px}.order-modal :is(button,a,textarea):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}
.modal-scroll{overflow:auto;padding:18px 20px}.modal-content-grid{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,.9fr);gap:28px}.modal-facts,.modal-timeline{display:grid;align-content:start;gap:18px}.modal-scroll section{display:grid;gap:8px}.modal-scroll h3{margin:0;color:var(--admin-muted);font-size:12px;letter-spacing:.04em;text-transform:uppercase}.modal-scroll p{margin:0;line-height:1.5}.modal-scroll a{color:var(--admin-brand);font-weight:650}
.modal-state{display:grid;min-height:240px;place-items:center;color:var(--admin-muted)}.modal-state.error{color:var(--admin-danger)}
.modal-items{display:grid;gap:10px;margin:0;padding:0;list-style:none}.modal-items li{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid var(--admin-border)}.modal-items img,.item-fallback{width:48px;height:48px;border-radius:12px;object-fit:cover}.item-fallback{display:grid;place-items:center;background:var(--admin-canvas)}.modal-items li>div,.modal-items li>span:last-child{display:grid;gap:3px}.modal-items li>span:last-child{text-align:right}.modal-items small{color:var(--admin-muted)}
.payment-breakdown{display:grid;gap:9px;margin:0}.payment-breakdown div{display:flex;justify-content:space-between;gap:16px}.payment-breakdown dd{margin:0;font-variant-numeric:tabular-nums}.payment-breakdown .total{padding-top:10px;border-top:1px solid var(--admin-border);font-weight:750}.full-detail-link{display:inline-flex;align-items:center;gap:6px;width:max-content;min-height:44px}
.modal-actions{display:flex;gap:8px;flex-wrap:wrap;padding:14px 20px calc(14px + env(safe-area-inset-bottom));border-top:1px solid var(--admin-border);background:var(--admin-surface);box-shadow:none}.modal-actions .btn-primary{border-color:#edcbbf;background:#fff1eb;color:#a64f31}.modal-actions .btn-danger,.modal-actions .btn-outline-danger{border-color:#efcfcc;background:#fcefee;color:#a85b55}.modal-actions>p,.modal-actions>label,.modal-actions>textarea{flex-basis:100%;margin:0}.modal-actions>div{display:flex;gap:8px;width:100%}.modal-actions>.btn:first-of-type,.modal-actions>div>button:first-child{margin-left:auto}.modal-actions textarea{width:100%;resize:vertical}.action-message{color:var(--admin-success)}.action-error{color:var(--admin-danger)}.btn-outline-danger{min-height:40px;padding:0 14px;border:1px solid var(--admin-danger);border-radius:10px;color:var(--admin-danger)}
@media(max-width:768px){.modal-content-grid{grid-template-columns:1fr}}
@media(max-width:640px){.order-modal-backdrop{padding:0}.order-modal{width:100%;max-width:none;height:100dvh;max-height:none;border-radius:0}.modal-header,.modal-scroll,.modal-actions{padding-inline:16px}.modal-actions>.btn{flex:1}}
@media(prefers-reduced-motion:no-preference){.order-modal{animation:modal-in .18s ease-out}@keyframes modal-in{from{transform:translateY(12px);opacity:.8}to{transform:none;opacity:1}}}
.order-modal-backdrop{--order-paper:#f6f5f4;--order-coral:#FF6846;--order-coral-hover:#F85B38;--order-coral-active:#E94F30;--order-coral-soft:#FFF1EC;--order-coral-border:#FFD8CC;--order-coral-text:#E95635;background:rgba(246,245,244,.84);backdrop-filter:blur(6px)}.order-modal{border:1px solid rgba(0,0,0,.1);border-radius:16px;background:var(--order-paper);box-shadow:none;font-family:Inter,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}.modal-header{padding:20px 22px;border-color:rgba(0,0,0,.08);background:#fff}.modal-header small{color:rgba(0,0,0,.42);font-size:11px;font-weight:600}.modal-header h2{font-size:24px;font-weight:600;letter-spacing:-.025em}.order-modal-close{border:1px solid rgba(0,0,0,.09);border-radius:8px;background:#fff;color:rgba(0,0,0,.62)}.modal-scroll{padding:14px}.modal-content-grid{gap:12px}.modal-facts,.modal-timeline{gap:12px}.modal-scroll section{padding:18px;border:1px solid rgba(0,0,0,.08);border-radius:12px;background:#fff}.modal-scroll h3{color:rgba(0,0,0,.42);font-size:11px;font-weight:600}.modal-scroll p,.modal-scroll li,.payment-breakdown{font-size:14px}.modal-items li{border-color:rgba(0,0,0,.08)}.modal-items img,.item-fallback{border-radius:8px}.payment-breakdown .total{border-color:rgba(0,0,0,.08)}.full-detail-link{color:var(--order-coral)!important;font-weight:500}.modal-actions{padding:14px 18px;border-color:rgba(0,0,0,.08);background:#fff}.modal-actions :is(.btn,button,a){border-radius:10px;font-size:13px;font-weight:500}.modal-actions .btn-primary{border-color:var(--order-coral);background:var(--order-coral);color:#fff;box-shadow:0 2px 5px rgba(255,104,70,.16),0 0 14px rgba(255,104,70,.10);transition:background 150ms ease,box-shadow 150ms ease,transform 150ms ease}.modal-actions .btn-primary:hover{background:var(--order-coral-hover);box-shadow:0 3px 7px rgba(255,104,70,.20),0 0 18px rgba(255,104,70,.14);transform:translateY(-1px)}.modal-actions .btn-primary:active{background:var(--order-coral-active);box-shadow:0 1px 3px rgba(255,104,70,.14);transform:none}.modal-actions .btn-outline{border-color:#E7E7E7;background:#fff;color:#3F3F46}.modal-actions .btn-outline:hover{border-color:var(--order-coral-border);background:#FFF7F4;color:var(--order-coral-text)}.modal-actions .btn-danger,.modal-actions .btn-outline-danger{border-color:#efd0cc;background:#f9e9e7;color:#9b504a}
</style>
