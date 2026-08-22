<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice } from '@/utils/format';
import { validateExactCod } from '@/utils/shipperOperations';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const props = defineProps({
  order: { type: Object, required: true },
});
const emit = defineEmits(['close', 'updated']);

const store = useShipperStore();
const sheet = ref(null);
const collectedAmount = ref('');
const submitting = ref(false);
const actionError = ref('');
const confirmOpen = ref(false);
let previousFocus = null;
let previousBodyOverflow = '';

const mapsUrl = computed(() => props.order.customerAddress ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(props.order.customerAddress)}` : '');
const isCodDeliver = computed(() => props.order.status === 'PICKED_UP' && props.order.paymentMethod === 'COD');
const primaryAction = computed(() => props.order.status === 'ASSIGNED' ? 'Lấy hàng' : props.order.status === 'PICKED_UP' ? 'Giao đơn' : '');

function close() {
  if (submitting.value) return;
  emit('close');
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    close();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = [...sheet.value.querySelectorAll('button:not(:disabled), a[href], input:not(:disabled)')];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

onMounted(() => {
  previousFocus = document.activeElement;
  previousBodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  if (isCodDeliver.value) collectedAmount.value = String(props.order.total);
  document.addEventListener('keydown', handleKeydown);
  nextTick(() => sheet.value?.focus());
});
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
  document.body.style.overflow = previousBodyOverflow;
  if (previousFocus?.focus) previousFocus.focus();
});

function requestPrimary() {
  if (!primaryAction.value || submitting.value) return;
  actionError.value = '';
  const cod = validateExactCod(collectedAmount.value, props.order.total);
  if (isCodDeliver.value && !cod.valid) {
    actionError.value = `Số tiền COD phải đúng ${formatPrice(props.order.total)}`;
    return;
  }
  confirmOpen.value = true;
}

async function runPrimary() {
  if (!primaryAction.value || submitting.value) return;
  confirmOpen.value = false;
  actionError.value = '';
  const cod = validateExactCod(collectedAmount.value, props.order.total);
  if (isCodDeliver.value && !cod.valid) {
    actionError.value = `Số tiền COD phải đúng ${formatPrice(props.order.total)}`;
    return;
  }
  submitting.value = true;
  try {
    if (props.order.status === 'ASSIGNED') await store.pickUpOrder(props.order.id, props.order.status);
    else await store.deliverOrder(props.order.id, isCodDeliver.value ? cod.amount : undefined, props.order.status);
    emit('updated');
  } catch (error) {
    actionError.value = error?.status === 409 ? 'Đơn hàng đã thay đổi trạng thái. Dữ liệu mới nhất đã được tải lại.' : error.message || 'Thao tác thất bại';
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="sheet-overlay" @click.self="close">
    <section ref="sheet" class="sheet" role="dialog" aria-modal="true" aria-labelledby="sheet-title" tabindex="-1">
      <div class="sheet-handle" aria-hidden="true"></div>
      <div class="sheet-header">
        <div class="sheet-heading">
          <h2 id="sheet-title">{{ order.orderCode }}</h2>
          <OrderStatusBadge :status="order.status" />
        </div>
        <button type="button" class="icon-btn" aria-label="Đóng" :disabled="submitting" @click="close"><i class="bi bi-x-lg"></i></button>
      </div>
      <div class="sheet-body">
        <div class="info-block">
          <strong>{{ order.customerName }}</strong>
          <a v-if="order.customerPhone" class="info-link" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a>
          <p>{{ order.customerAddress }}</p>
        </div>
        <div class="totals">
          <div class="row"><span>Tổng thu</span><strong>{{ formatPrice(order.total) }}</strong></div>
          <div v-if="order.shippingFee" class="row"><span>Phí giao</span><span>{{ formatPrice(order.shippingFee) }}</span></div>
          <div v-if="order.discount" class="row"><span>Giảm giá</span><span>{{ formatPrice(order.discount) }}</span></div>
          <div class="row"><span>Thanh toán</span><span>{{ order.paymentMethod }} · {{ order.paymentStatus }}</span></div>
          <div v-if="order.codCollectedAmount != null" class="row"><span>COD đã thu</span><strong>{{ formatPrice(order.codCollectedAmount) }}</strong></div>
        </div>
        <div v-if="isCodDeliver" class="cod-block">
          <label for="sheet-cod-amount">Số tiền COD đã thu</label>
          <input id="sheet-cod-amount" v-model="collectedAmount" class="form-input" type="number" min="0" step="0.01" :aria-invalid="!!actionError" :aria-describedby="actionError ? 'sheet-action-error' : undefined" />
        </div>
        <p v-if="actionError" id="sheet-action-error" class="error" role="alert">{{ actionError }}</p>
      </div>
      <div class="sheet-actions">
        <a v-if="order.customerPhone" class="btn btn-outline" :href="`tel:${order.customerPhone}`"><i class="bi bi-telephone"></i> Gọi</a>
        <a v-if="mapsUrl" class="btn btn-outline" :href="mapsUrl" target="_blank" rel="noopener noreferrer"><i class="bi bi-geo-alt"></i> Bản đồ</a>
        <button v-if="primaryAction" class="btn btn-primary" :disabled="submitting" @click="requestPrimary">{{ submitting ? 'Đang xử lý...' : order.status === 'PICKED_UP' ? 'Giao thành công' : primaryAction }}</button>
        <router-link class="btn btn-outline" :to="`/shipper/orders/${order.id}`" @click="close">{{ order.status === 'PICKED_UP' ? 'Mở chi tiết để báo thất bại' : 'Xem chi tiết' }}</router-link>
      </div>
    </section>
    <ConfirmDialog :open="confirmOpen" :title="order.status === 'ASSIGNED' ? 'Xác nhận lấy hàng' : 'Xác nhận giao hàng'" :message="order.status === 'ASSIGNED' ? 'Bạn đã lấy hàng từ cửa hàng?' : isCodDeliver ? `Bạn đã giao hàng và thu ${formatPrice(order.total)}?` : 'Bạn đã giao hàng thành công?'" :confirm-label="primaryAction" :busy="submitting" @confirm="runPrimary" @cancel="confirmOpen = false" />
  </div>
</template>

<style scoped>
.sheet-overlay { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: flex-end; justify-content: center; background: rgba(13,20,33,.64); backdrop-filter: blur(3px); }
.sheet { width: 100%; max-width: 480px; max-height: 90dvh; display: flex; flex-direction: column; background: #fff; border-radius: 16px 16px 0 0; box-shadow: 0 -8px 40px rgba(0,0,0,.25); }
.sheet-handle { width: 40px; height: 4px; border-radius: 99px; background: var(--border); margin: 10px auto 0; }
.sheet-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px 0; }
.sheet-heading { display: flex; align-items: center; gap: 10px; }
.sheet-heading h2 { font-size: 18px; margin: 0; }
.icon-btn { width: 36px; height: 36px; border: none; border-radius: var(--radius-sm); background: var(--surface); display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 16px; color: var(--text-mid); }
.icon-btn:disabled { cursor: not-allowed; opacity: .5; }
.sheet-body { overflow-y: auto; padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.info-block p { color: var(--text-mid); font-size: 13px; margin: 4px 0 0; }
.info-link { color: var(--primary); font-weight: 600; }
.totals { border: 1px solid var(--border-light); border-radius: var(--radius); padding: 4px 12px; }
.row { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; font-size: 13px; color: var(--text-mid); }
.row + .row { border-top: 1px solid var(--border-light); }
.row strong { color: var(--text-dark); }
.cod-block { display: flex; flex-direction: column; gap: 6px; }
.cod-block label { font-size: 13px; font-weight: 700; }
.error { color: var(--red-active); font-size: 13px; margin: 0; }
.sheet-actions { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 12px 16px calc(16px + env(safe-area-inset-bottom)); border-top: 1px solid var(--border-light); }
.sheet-actions .btn { margin: 0; }
</style>
