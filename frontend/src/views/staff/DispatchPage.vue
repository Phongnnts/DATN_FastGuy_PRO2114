<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { staffApi } from '@/api';
import { useStaffStore } from '@/stores/staff';
import { useToast } from '@/stores/toast';
import { formatPrice } from '@/utils/format';
import { acceptsDispatchRequest, sortAvailableShippers, validDispatchSelections, waitingDuration } from '@/utils/staffKitchen';

const staffStore = useStaffStore();
const toast = useToast();
const shippers = ref([]);
const selections = ref({});
const assigningId = ref(null);
const ordersLoading = ref(false);
const shippersLoading = ref(false);
const ordersError = ref('');
const shippersError = ref('');
let ordersInFlight = false;
let shippersInFlight = false;
let generation = 0;
let stopped = false;
let pollTimer = null;

const readyOrders = computed(() => staffStore.allOrders.filter((order) => order.status === 'READY' && !order.shipperId));
const sortedShippers = computed(() => sortAvailableShippers(shippers.value));

async function loadOrders() {
  if (ordersInFlight || stopped) return;
  const requestGeneration = generation;
  ordersInFlight = true;
  ordersLoading.value = true;
  ordersError.value = '';
  try {
    await staffStore.fetchReadyOrders();
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    if (staffStore.error) ordersError.value = staffStore.error;
  } catch (error) {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) ordersError.value = error.message || 'Không thể tải đơn sẵn sàng';
  } finally {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) ordersLoading.value = false;
    ordersInFlight = false;
  }
}

async function loadShippers() {
  if (shippersInFlight || stopped) return;
  const requestGeneration = generation;
  shippersInFlight = true;
  shippersLoading.value = true;
  shippersError.value = '';
  try {
    const data = await staffApi.getAvailableShippers();
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    shippers.value = Array.isArray(data) ? data : [];
    selections.value = validDispatchSelections(selections.value, shippers.value);
  } catch (error) {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) shippersError.value = error.message || 'Không thể tải danh sách shipper';
  } finally {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) shippersLoading.value = false;
    shippersInFlight = false;
  }
}

async function load() { await Promise.allSettled([loadOrders(), loadShippers()]); }
async function retryOrders() { await loadOrders(); }
async function retryShippers() { await loadShippers(); }

async function assign(order) {
  const shipperId = selections.value[order.id];
  if (!shipperId || assigningId.value || stopped) return;
  const requestGeneration = generation;
  assigningId.value = order.id;
  try {
    await staffApi.assignShipper(order.id, shipperId);
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    delete selections.value[order.id];
    toast.success(`Đã giao ${order.orderCode} cho shipper`);
  } catch (error) {
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    delete selections.value[order.id];
    selections.value = validDispatchSelections(selections.value, shippers.value.filter((shipper) => String(shipper.id) !== String(shipperId)));
    toast.error(error.message || 'Không thể gán shipper');
  } finally {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) {
      assigningId.value = null;
      await Promise.allSettled([loadOrders(), loadShippers()]);
    }
  }
}

onMounted(() => {
  load();
  pollTimer = setInterval(load, 30000);
});
onBeforeUnmount(() => {
  stopped = true;
  generation += 1;
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<template>
  <div>
    <div class="page-header"><div><h1>Điều phối giao hàng</h1><p>Ghép đơn sẵn sàng với shipper đang khả dụng.</p></div><button class="btn btn-sm btn-outline" :disabled="ordersLoading || shippersLoading" @click="load">Làm mới</button></div>
    <div class="lane-state" role="status" aria-live="polite" aria-atomic="true"><span v-if="ordersLoading">Đang tải đơn...</span><span v-if="ordersError" class="error">{{ ordersError }} <button class="btn btn-sm btn-outline" @click="retryOrders">Thử lại đơn</button></span><span v-if="shippersLoading">Đang tải shipper...</span><span v-if="shippersError" class="error">{{ shippersError }} <button class="btn btn-sm btn-outline" @click="retryShippers">Thử lại shipper</button></span></div>
    <div v-if="readyOrders.length" class="card card-flat table-wrapper"><table class="table"><caption class="sr-only">Danh sách đơn sẵn sàng và shipper khả dụng</caption><thead><tr><th>Đơn</th><th>Khách hàng</th><th>Giao hàng</th><th>Giá trị</th><th>Shipper khả dụng</th><th>Thao tác</th></tr></thead><tbody><tr v-for="order in readyOrders" :key="order.id"><td data-label="Đơn"><router-link :to="`/staff/orders/${order.id}`" class="order-link">{{ order.orderCode }}</router-link><small>Chờ {{ waitingDuration(order.createdAt) }}</small></td><td data-label="Khách hàng"><strong>{{ order.customerName || (order.userId != null ? `Khách #${order.userId}` : 'Khách vãng lai') }}</strong><small>{{ order.itemCount ?? 0 }} món</small><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a></td><td data-label="Giao hàng">{{ order.shippingAddress || 'Chưa có địa chỉ' }}</td><td data-label="Giá trị">{{ formatPrice(order.total) }}</td><td data-label="Shipper"><select v-model="selections[order.id]" class="form-select" :aria-label="`Chọn shipper cho ${order.orderCode}`" :disabled="assigningId === order.id || shippersLoading || !!shippersError"><option :value="undefined">Chọn shipper</option><option v-for="shipper in sortedShippers" :key="shipper.id" :value="shipper.id">{{ shipper.fullName }} · Đang giao: {{ shipper.activeOrderCount || 0 }} đơn</option></select><small v-if="!shippersLoading && !shippersError && !sortedShippers.length">Không có shipper khả dụng</small></td><td data-label="Thao tác"><button class="btn btn-primary btn-sm" :disabled="!selections[order.id] || assigningId !== null" @click="assign(order)">{{ assigningId === order.id ? 'Đang gán...' : 'Gán shipper' }}</button></td></tr></tbody></table></div>
    <div v-else-if="!ordersLoading && !ordersError" class="empty-state"><i class="bi bi-truck" aria-hidden="true"></i><h3>Không có đơn chờ điều phối</h3><p>Đơn sẵn sàng giao sẽ xuất hiện tại đây.</p></div>
  </div>
</template>

<style scoped>
.page-header p{margin:4px 0 0;color:var(--text-mid);font-size:14px}.lane-state{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;color:var(--text-mid)}.error{color:var(--red-active)}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}.order-link{display:block;color:var(--text-dark);font-weight:750}.table td strong,.table td a,.table td small{display:block}.table td small{margin-top:4px;color:var(--text-mid)}.form-select{min-width:270px}@media(max-width:768px){.table thead{display:none}.table tbody tr{display:block;margin-bottom:8px;padding:12px;border:1px solid var(--border-light);border-radius:var(--radius-sm)}.table tbody td{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:6px 0;border:0;text-align:right}.table tbody td::before{content:attr(data-label);color:var(--text-mid);font-weight:650}.form-select{min-width:0;max-width:70%}}
</style>
