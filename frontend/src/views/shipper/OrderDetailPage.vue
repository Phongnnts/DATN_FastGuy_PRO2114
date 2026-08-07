<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { shiftApi, storeApi } from '@/api';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';
import { validateExactCod } from '@/utils/shipperOperations';
import { useToast } from '@/stores/toast';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const route = useRoute();
const store = useShipperStore();
const toast = useToast();
const order = ref(null);
const collectedAmount = ref('');
const submitting = ref(false);
const actionError = ref('');
const checkedIn = ref(false);
const confirmAction = ref(null);
const issueOpen = ref(false);
const issueLoading = ref(false);
const issueError = ref('');
const storeInfo = ref(null);
const issueDialog = ref(null);
const previousFocus = ref(null);
const now = ref(new Date());
let previousOverflow = '';
let stopped = false;
let loadGeneration = 0;
let clockTimer;

const isTerminal = computed(() => ['DELIVERED', 'CANCELLED'].includes(order.value?.status));
const backTo = computed(() => isTerminal.value ? '/shipper/history' : '/shipper/orders');
const allowedActions = computed(() => checkedIn.value ? order.value?.allowedActions || [] : []);
const canPickup = computed(() => allowedActions.value.includes('PICKED_UP') || allowedActions.value.includes('PICKUP'));
const canDeliver = computed(() => allowedActions.value.includes('DELIVERED') || allowedActions.value.includes('DELIVER'));
const mapsUrl = computed(() => order.value?.customerAddress ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(order.value.customerAddress)}` : '');
const waitingMinutes = computed(() => {
  if (!order.value?.assignedAt || isTerminal.value) return null;
  return Math.max(0, Math.floor((now.value.getTime() - new Date(order.value.assignedAt).getTime()) / 60000));
});
const confirmTitle = computed(() => confirmAction.value === 'pickup' ? 'Xác nhận lấy hàng' : 'Xác nhận giao hàng');
const confirmMessage = computed(() => {
  if (confirmAction.value === 'pickup') return 'Bạn đã lấy hàng từ cửa hàng?';
  return order.value?.paymentMethod === 'COD' ? `Bạn đã giao hàng thành công và thu ${formatPrice(order.value.total)}?` : 'Bạn đã giao hàng thành công?';
});
const confirmLabel = computed(() => confirmAction.value === 'pickup' ? 'Đã lấy hàng' : 'Đã giao thành công');

async function checkShift() {
  try { checkedIn.value = (await shiftApi.getCurrent())?.state === 'CHECKED_IN'; } catch { checkedIn.value = false; }
  return checkedIn.value;
}
async function load() {
  const generation = ++loadGeneration;
  actionError.value = '';
  try {
    const [data] = await Promise.all([store.fetchOrderById(route.params.id), checkShift()]);
    if (stopped || generation !== loadGeneration || !data) return;
    order.value = data;
    collectedAmount.value = data.paymentMethod === 'COD' ? String(data.total) : '';
  } catch {}
}
async function mutate(action) {
  if (!order.value || submitting.value || stopped) return;
  actionError.value = '';
  if (!await checkShift()) { actionError.value = 'Bạn cần check-in ca làm để thực hiện thao tác này'; return; }
  if ((action === 'pickup' && !canPickup.value) || (action === 'deliver' && !canDeliver.value)) return;
  const cod = validateExactCod(collectedAmount.value, order.value.total);
  if (action === 'deliver' && order.value.paymentMethod === 'COD' && !cod.valid) { actionError.value = `Số tiền COD phải đúng ${formatPrice(order.value.total)}`; return; }
  submitting.value = true;
  try {
    if (action === 'pickup') await store.pickUpOrder(order.value.id, order.value.status); else await store.deliverOrder(order.value.id, order.value.paymentMethod === 'COD' ? cod.amount : undefined, order.value.status);
    if (!stopped) { confirmAction.value = null; await load(); }
  } catch (error) { if (!stopped) { actionError.value = error?.status === 409 ? 'Đơn hàng đã thay đổi trạng thái. Dữ liệu mới nhất đã được tải lại.' : error.message; toast.error(actionError.value); confirmAction.value = null; if (error?.status === 409) await load(); } } finally { if (!stopped) submitting.value = false; }
}
function requestAction(action) {
  if (!order.value || submitting.value) return;
  actionError.value = '';
  confirmAction.value = action;
}
async function openIssue() {
  issueOpen.value = true;
  if (storeInfo.value || issueLoading.value) return;
  issueLoading.value = true;
  issueError.value = '';
  try {
    storeInfo.value = await storeApi.getConfig();
  } catch {
    issueError.value = 'Không thể tải thông tin liên hệ cửa hàng';
  } finally {
    issueLoading.value = false;
  }
}
function closeIssue() { issueOpen.value = false; }
function handleIssueKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeIssue();
    return;
  }
  if (event.key !== 'Tab' || !issueDialog.value) return;
  const focusable = [...issueDialog.value.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
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

watch(issueOpen, async (open) => {
  if (open) {
    previousFocus.value = document.activeElement;
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    await nextTick();
    issueDialog.value?.focus();
    return;
  }
  if (previousFocus.value || previousOverflow !== '') {
    document.body.style.overflow = previousOverflow;
    previousOverflow = '';
    nextTick(() => previousFocus.value?.focus());
    previousFocus.value = null;
  }
});
watch(() => route.params.id, () => { store.invalidateDetailRequests(); order.value = null; checkedIn.value = false; confirmAction.value = null; issueOpen.value = false; load(); }, { immediate: true });
onMounted(() => { clockTimer = setInterval(() => { now.value = new Date(); }, 30_000); });
onBeforeUnmount(() => {
  if (issueOpen.value || previousOverflow !== '') document.body.style.overflow = previousOverflow;
});
onUnmounted(() => { stopped = true; loadGeneration += 1; store.invalidateDetailRequests(); clearInterval(clockTimer); });
</script>

<template>
  <div v-if="store.detailLoading" class="state">Đang tải...</div><div v-else-if="store.detailError" class="state error" role="alert"><p>{{ store.detailError }}</p><button class="btn btn-outline btn-sm" @click="load">Thử lại</button></div>
  <div v-else-if="order"><router-link class="back-link" :to="backTo">← Quay lại</router-link><header class="detail-header"><div><h1>{{ order.orderCode }}</h1><time :datetime="order.createdAt">Tạo {{ formatDate(order.createdAt) }}</time></div><OrderStatusBadge :status="order.status" /></header>
    <section class="info-card priority-card"><h2>Khách hàng</h2><p><strong>{{ order.customerName }}</strong></p><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><p>{{ order.customerAddress }}</p><a v-if="mapsUrl" class="map-link" :href="mapsUrl" target="_blank" rel="noopener noreferrer">Mở trong Google Maps</a></section>
    <section class="info-card cod-summary"><h2>Thanh toán</h2><div class="row"><span>Phương thức</span><strong>{{ order.paymentMethod }}</strong></div><div class="row"><span>Trạng thái</span><strong>{{ order.paymentStatus }}</strong></div><div class="row total"><span>Tổng thu</span><strong>{{ formatPrice(order.total) }}</strong></div><div v-if="order.codCollectedAmount != null" class="row"><span>COD đã thu</span><strong>{{ formatPrice(order.codCollectedAmount) }}</strong></div></section>
    <section v-if="canDeliver && order.paymentMethod === 'COD'" class="info-card"><label for="cod-amount">Số tiền COD đã thu</label><input id="cod-amount" v-model="collectedAmount" class="form-input" type="number" min="0" step="0.01" :aria-invalid="!!actionError" :aria-describedby="actionError ? 'action-error' : undefined" /></section><p v-if="actionError" id="action-error" class="error" role="alert">{{ actionError }}</p><p v-if="order.allowedActions.length && !checkedIn" class="state">Check-in ca làm để thao tác đơn</p><div class="sticky-actions"><button v-if="canPickup" class="btn btn-primary" :disabled="submitting" @click="requestAction('pickup')">{{ submitting ? 'Đang xử lý...' : 'Đã lấy hàng' }}</button><button v-if="canDeliver" class="btn btn-success" :disabled="submitting" @click="requestAction('deliver')">{{ submitting ? 'Đang xử lý...' : 'Đã giao thành công' }}</button><button class="btn btn-outline" @click="openIssue">Báo sự cố</button></div>
    <details v-if="order.deliveryNote" class="info-card"><summary>Ghi chú giao hàng</summary><p>{{ order.deliveryNote }}</p></details>
    <details class="info-card"><summary>Sản phẩm ({{ order.items.length }})</summary><div v-for="(item,index) in order.items" :key="`${item.productId}-${index}`" class="item-row"><div><strong>{{ item.productName }}{{ item.variantName ? ` · ${item.variantName}` : '' }}</strong><small v-for="modifier in item.modifiers" :key="modifier.id || modifier.name">{{ modifier.name }}{{ modifier.price ? ` · ${formatPrice(modifier.price)}` : '' }}</small></div><span>x{{ item.quantity }}</span></div></details>
    <details class="info-card"><summary>Chi tiết phí</summary><div class="row"><span>Phí giao</span><span>{{ formatPrice(order.shippingFee) }}</span></div><div v-if="order.serviceFee > 0" class="row"><span>Phí dịch vụ</span><span>{{ formatPrice(order.serviceFee) }}</span></div><div v-if="order.discount > 0" class="row"><span>Giảm giá</span><span>-{{ formatPrice(order.discount) }}</span></div></details>
    <details class="info-card"><summary>Mốc thời gian</summary><p v-if="waitingMinutes != null">Đã chờ {{ waitingMinutes }} phút</p><p v-if="order.assignedAt">Phân công: {{ formatDate(order.assignedAt) }}</p><p v-if="order.pickedUpAt">Lấy hàng: {{ formatDate(order.pickedUpAt) }}</p><p v-if="order.deliveredAt">Giao hàng: {{ formatDate(order.deliveredAt) }}</p><OrderTimeline :history="order.statusHistory" /></details>
    <ConfirmDialog :open="Boolean(confirmAction)" :title="confirmTitle" :message="confirmMessage" :confirm-label="confirmLabel" :busy="submitting" @confirm="mutate(confirmAction)" @cancel="confirmAction = null" />
    <div v-if="issueOpen" class="modal-overlay" @click.self="closeIssue"><section ref="issueDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="issue-title" tabindex="-1" @keydown="handleIssueKeydown"><div class="modal-header"><h2 id="issue-title" class="modal-title">Báo sự cố</h2><button class="modal-close" type="button" aria-label="Đóng" @click="closeIssue"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><div class="modal-body"><p v-if="issueLoading" class="state">Đang tải thông tin liên hệ...</p><div v-else-if="issueError" class="state error" role="alert"><p>{{ issueError }}</p><button class="btn btn-outline btn-sm" @click="openIssue">Thử lại</button></div><template v-else><p>Gặp sự cố trong quá trình giao hàng? Liên hệ cửa hàng để được hỗ trợ kịp thời.</p><p v-if="storeInfo?.storeName" class="store-name">{{ storeInfo.storeName }}</p><p v-if="storeInfo?.storePhone" class="hotline"><a :href="`tel:${storeInfo.storePhone}`">{{ storeInfo.storePhone }}</a></p><p v-else-if="!storeInfo">Gặp sự cố trong quá trình giao hàng? Liên hệ cửa hàng để được hỗ trợ.</p></template></div><div class="modal-footer"><button class="btn btn-ghost" type="button" @click="closeIssue">Đóng</button></div></section></div>
  </div><div v-else class="state">Không tìm thấy đơn hàng</div>
</template>

<style scoped>
.state{text-align:center;padding:24px;color:var(--text-mid)}.error{color:var(--red-active)}.back-link{display:inline-flex;align-items:center;min-height:44px;color:var(--primary);text-decoration:none;font-size:14px;font-weight:650;margin-bottom:8px}.detail-header{display:flex;justify-content:space-between;gap:12px;margin-bottom:12px}.detail-header h1{font-size:18px}.detail-header time{color:var(--text-mid);font-size:12px}.info-card{background:#fff;border:1px solid var(--border-light);border-radius:var(--radius);padding:14px;margin-bottom:10px}.info-card h2{color:var(--text-mid);font-size:12px;text-transform:uppercase;margin-bottom:8px}.info-card summary{min-height:44px;display:flex;align-items:center;font-weight:700;cursor:pointer}.map-link{display:inline-flex;align-items:center;min-height:44px;color:var(--primary);font-weight:700}.item-row,.row{display:flex;justify-content:space-between;gap:12px;padding:7px 0}.item-row small{display:block;color:var(--text-mid);margin-top:3px}.total{border-top:1px solid var(--border-light);font-size:17px}.sticky-actions{position:sticky;z-index:10;bottom:0;display:grid;gap:8px;margin:14px -8px 12px;padding:10px 8px calc(10px + env(safe-area-inset-bottom));background:color-mix(in srgb,var(--surface) 94%,transparent);backdrop-filter:blur(10px);border-top:1px solid var(--border-light)}.sticky-actions .btn{width:100%;min-height:44px;padding:12px}.modal-overlay{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.55)}.modal{width:min(420px,100%);background:var(--surface);border-radius:18px;box-shadow:0 24px 70px rgba(15,23,42,.25)}.modal-header{display:flex;justify-content:space-between;align-items:center;padding:16px 20px}.modal-title{font-size:16px;margin:0}.modal-close{border:0;background:none;min-width:44px;min-height:44px;font-size:18px;cursor:pointer;color:var(--text-mid)}.modal-body{padding:0 20px}.modal-footer{display:flex;justify-content:flex-end;gap:8px;padding:14px 20px calc(14px + env(safe-area-inset-bottom))}.store-name{font-weight:700;margin:0}.hotline{margin:4px 0}.hotline a{color:var(--primary);font-weight:700;text-decoration:none}
</style>
