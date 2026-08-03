<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { shiftApi } from '@/api';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';
import { validateExactCod } from '@/utils/shipperOperations';
import { useToast } from '@/stores/toast';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const route = useRoute();
const store = useShipperStore();
const toast = useToast();
const order = ref(null);
const collectedAmount = ref('');
const submitting = ref(false);
const actionError = ref('');
const checkedIn = ref(false);
let stopped = false;
let loadGeneration = 0;
const allowedActions = computed(() => checkedIn.value ? order.value?.allowedActions || [] : []);
const canPickup = computed(() => allowedActions.value.includes('PICKED_UP') || allowedActions.value.includes('PICKUP'));
const canDeliver = computed(() => allowedActions.value.includes('DELIVERED') || allowedActions.value.includes('DELIVER'));
const mapsUrl = computed(() => order.value?.customerAddress ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(order.value.customerAddress)}` : '');

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
    if (action === 'pickup') await store.pickUpOrder(order.value.id); else await store.deliverOrder(order.value.id, order.value.paymentMethod === 'COD' ? cod.amount : undefined);
    if (!stopped) await load();
  } catch (error) { if (!stopped) { actionError.value = error?.response?.data?.message || error.message; toast.error(actionError.value); } } finally { if (!stopped) submitting.value = false; }
}

watch(() => route.params.id, () => { store.invalidateDetailRequests(); order.value = null; checkedIn.value = false; load(); }, { immediate: true });
onUnmounted(() => { stopped = true; loadGeneration += 1; store.invalidateDetailRequests(); });
</script>

<template>
  <div v-if="store.detailLoading" class="state">Đang tải...</div><div v-else-if="store.detailError" class="state error" role="alert"><p>{{ store.detailError }}</p><button class="btn btn-outline btn-sm" @click="load">Thử lại</button></div>
  <div v-else-if="order"><header class="detail-header"><div><h1>{{ order.orderCode }}</h1><time :datetime="order.createdAt">Tạo {{ formatDate(order.createdAt) }}</time></div><OrderStatusBadge :status="order.status" /></header>
    <section class="info-card"><h2>Khách hàng</h2><p>{{ order.customerName }}</p><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a><p>{{ order.customerAddress }}</p><a v-if="mapsUrl" :href="mapsUrl" target="_blank" rel="noopener noreferrer">Mở trong Google Maps</a></section><section v-if="order.deliveryNote" class="info-card"><h2>Ghi chú giao hàng</h2><p>{{ order.deliveryNote }}</p></section>
    <section class="info-card"><h2>Sản phẩm</h2><div v-for="(item,index) in order.items" :key="`${item.productId}-${index}`" class="item-row"><div><strong>{{ item.productName }}{{ item.variantName ? ` · ${item.variantName}` : '' }}</strong><small v-for="modifier in item.modifiers" :key="modifier.id || modifier.name">{{ modifier.name }}{{ modifier.price ? ` · ${formatPrice(modifier.price)}` : '' }}</small></div><span>x{{ item.quantity }}</span></div></section>
    <section class="info-card"><h2>Thanh toán</h2><div class="row"><span>Phương thức</span><strong>{{ order.paymentMethod }}</strong></div><div class="row"><span>Trạng thái</span><strong>{{ order.paymentStatus }}</strong></div><div class="row"><span>Phí giao</span><span>{{ formatPrice(order.shippingFee) }}</span></div><div class="row total"><span>Tổng thu</span><strong>{{ formatPrice(order.total) }}</strong></div><div v-if="order.codCollectedAmount != null" class="row"><span>COD đã thu</span><strong>{{ formatPrice(order.codCollectedAmount) }}</strong></div></section>
    <section class="info-card"><h2>Mốc thời gian</h2><p v-if="order.assignedAt">Phân công: {{ formatDate(order.assignedAt) }}</p><p v-if="order.pickedUpAt">Lấy hàng: {{ formatDate(order.pickedUpAt) }}</p><p v-if="order.deliveredAt">Giao hàng: {{ formatDate(order.deliveredAt) }}</p><OrderTimeline :history="order.statusHistory" /></section>
    <section v-if="canDeliver && order.paymentMethod === 'COD'" class="info-card"><label for="cod-amount">Số tiền COD đã thu</label><input id="cod-amount" v-model="collectedAmount" class="form-input" type="number" min="0" step="0.01" :aria-invalid="!!actionError" :aria-describedby="actionError ? 'action-error' : undefined" /></section><p v-if="actionError" id="action-error" class="error" role="alert">{{ actionError }}</p><p v-if="order.allowedActions.length && !checkedIn" class="state">Check-in ca làm để thao tác đơn</p><div class="actions"><button v-if="canPickup" class="btn btn-primary" :disabled="submitting" @click="mutate('pickup')">{{ submitting ? 'Đang xử lý...' : 'Đã lấy hàng' }}</button><button v-if="canDeliver" class="btn btn-success" :disabled="submitting" @click="mutate('deliver')">{{ submitting ? 'Đang xử lý...' : 'Đã giao thành công' }}</button></div>
  </div><div v-else class="state">Không tìm thấy đơn hàng</div>
</template>

<style scoped>
.state{text-align:center;padding:24px;color:var(--text-mid)}.error{color:var(--red-active)}.detail-header{display:flex;justify-content:space-between;gap:12px;margin-bottom:12px}.detail-header h1{font-size:18px}.detail-header time{color:var(--text-mid);font-size:12px}.info-card{background:#fff;border:1px solid var(--border-light);border-radius:var(--radius);padding:14px;margin-bottom:10px}.info-card h2{color:var(--text-mid);font-size:12px;text-transform:uppercase;margin-bottom:8px}.item-row,.row{display:flex;justify-content:space-between;gap:12px;padding:7px 0}.item-row small{display:block;color:var(--text-mid);margin-top:3px}.total{border-top:1px solid var(--border-light);font-size:17px}.actions{display:grid;gap:8px;margin-top:14px}.actions .btn{width:100%;padding:14px}
</style>
