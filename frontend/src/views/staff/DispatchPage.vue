<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { staffApi } from '@/api';
import { useStaffStore } from '@/stores/staff';
import { useToast } from '@/stores/toast';
import { formatPrice } from '@/utils/format';
import { acceptsDispatchRequest, dispatchTabTarget, sortAvailableShippers, validDispatchSelections, waitingDuration } from '@/utils/staffKitchen';

const staffStore = useStaffStore();
const toast = useToast();
const tabs = [
  { filter: 'PRIORITY', label: 'Ưu tiên', countKey: 'priority' },
  { filter: 'NEW', label: 'Đơn mới', countKey: 'new' },
  { filter: 'REVIEW', label: 'Xem lại', countKey: 'review' },
];
const activeFilter = ref('PRIORITY');
const tabElements = ref([]);
const shippers = ref([]);
const selections = ref({});
const assigningId = ref(null);
const shippersLoading = ref(false);
const shippersError = ref('');
let shippersInFlight = false;
let generation = 0;
let stopped = false;
let pollTimer = null;

const canAssign = computed(() => activeFilter.value !== 'REVIEW');
const readyOrders = computed(() => staffStore.dispatchItems
  .filter((order) => !canAssign.value || !order.shipperId)
  .map((order) => ({ ...order, classification: classificationLabel(order) })));
const dispatchCounts = computed(() => staffStore.dispatchCounts);
const ordersLoading = computed(() => staffStore.dispatchLoading);
const ordersError = computed(() => staffStore.dispatchError);
const sortedShippers = computed(() => sortAvailableShippers(shippers.value));

async function loadOrders(filter = activeFilter.value) {
  if (stopped) return;
  try {
    await staffStore.fetchDispatchOrders(filter);
  } catch {
    // The store owns the visible error for the latest request.
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
async function poll() { await Promise.allSettled([staffStore.fetchDispatchOrders(activeFilter.value), loadShippers()]); }
async function retryOrders() { await loadOrders(); }
async function retryShippers() { await loadShippers(); }

function switchFilter(filter) {
  if (filter === activeFilter.value) return;
  activeFilter.value = filter;
  selections.value = {};
  void loadOrders(filter);
}

function handleTabKeydown(event, index) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  const nextIndex = dispatchTabTarget(index, event.key, tabs.length);
  activeFilter.value = tabs[nextIndex].filter;
  selections.value = {};
  tabElements.value[nextIndex]?.focus();
  void loadOrders(tabs[nextIndex].filter);
}

function classificationLabel(order) {
  if (order.classification === 'REVIEW') return 'Cần xem lại';
  if (order.classification === 'NEW') return 'Mới';
  if (order.classification === 'PRIORITY' && order.minutesUntilClose != null && order.minutesUntilClose <= 30) return 'Sắp đóng cửa';
  if (order.classification === 'PRIORITY') return 'Chờ lâu';
  return '';
}

async function assign(order) {
  const shipperId = selections.value[order.id];
  if (!canAssign.value || order.status !== 'READY' || !shipperId || assigningId.value || stopped) return;
  const requestGeneration = generation;
  assigningId.value = order.id;
  try {
    await staffApi.assignShipper(order.id, shipperId, order.status);
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    delete selections.value[order.id];
    toast.success(`Đã giao ${order.orderCode} cho shipper`);
  } catch (error) {
    if (!acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) return;
    delete selections.value[order.id];
    if (error.status === 409) selections.value = { ...selections.value };
    else selections.value = validDispatchSelections(selections.value, shippers.value.filter((shipper) => String(shipper.id) !== String(shipperId)));
    toast.error(error.status === 422 ? 'Shipper không còn trong ca hoạt động' : error.message || 'Không thể gán shipper');
  } finally {
    if (acceptsDispatchRequest({ requestGeneration, latestGeneration: generation, stopped })) {
      assigningId.value = null;
      await Promise.allSettled([loadOrders(), loadShippers()]);
    }
  }
}

onMounted(() => {
  load();
  pollTimer = setInterval(poll, 30000);
});
onBeforeUnmount(() => {
  stopped = true;
  generation += 1;
  staffStore.invalidateDispatch();
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<template>
  <div>
    <div class="page-header"><div><h1>Điều phối giao hàng</h1><p>Ghép đơn sẵn sàng với shipper đang khả dụng.</p></div><button class="btn btn-sm btn-outline refresh-control" :disabled="ordersLoading || shippersLoading" @click="load">Làm mới</button></div>
    <div class="dispatch-tabs" role="tablist" aria-label="Bộ lọc điều phối">
      <button v-for="(tab, index) in tabs" :id="`dispatch-tab-${tab.filter}`" :key="tab.filter" :ref="(element) => { if (element) tabElements[index] = element; }" role="tab" class="dispatch-tab" :class="{ active: activeFilter === tab.filter }" :aria-selected="activeFilter === tab.filter" aria-controls="dispatch-panel" :tabindex="activeFilter === tab.filter ? 0 : -1" @click="switchFilter(tab.filter)" @keydown="handleTabKeydown($event, index)"><span>{{ tab.label }}</span><span class="count-badge">{{ dispatchCounts[tab.countKey] }}</span></button>
    </div>
    <div class="lane-state" role="status" aria-live="polite" aria-atomic="true"><span v-if="ordersLoading">Đang tải đơn...</span><span v-if="ordersError" class="error">{{ ordersError }} <button class="btn btn-sm btn-outline refresh-control" @click="retryOrders">Thử lại đơn</button></span><template v-if="canAssign"><span v-if="shippersLoading">Đang tải shipper...</span><span v-if="shippersError" class="error">{{ shippersError }} <button class="btn btn-sm btn-outline refresh-control" @click="retryShippers">Thử lại shipper</button></span></template></div>
    <div id="dispatch-panel" role="tabpanel" :aria-labelledby="`dispatch-tab-${activeFilter}`" tabindex="0">
      <div v-if="readyOrders.length" class="card card-flat table-wrapper"><table class="table"><caption class="sr-only">Danh sách đơn điều phối</caption><thead><tr><th>Đơn</th><th>Khách hàng</th><th>Giao hàng</th><th>Giá trị</th><template v-if="canAssign"><th>Shipper khả dụng</th><th>Thao tác</th></template><template v-else><th>Sự cố</th><th>Thao tác</th></template></tr></thead><tbody><tr v-for="order in readyOrders" :key="order.id"><td data-label="Đơn"><router-link :to="`/staff/orders/${order.id}`" class="order-link">{{ order.orderCode }}</router-link><small>Chờ {{ waitingDuration(order.readyAt || order.createdAt) }}</small><span class="status-badge">{{ order.classification }}</span></td><td data-label="Khách hàng"><strong>{{ order.customerName || (order.userId != null ? `Khách #${order.userId}` : 'Khách vãng lai') }}</strong><small>{{ order.itemCount ?? 0 }} món</small><a v-if="order.customerPhone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a></td><td data-label="Giao hàng">{{ order.shippingAddress || 'Chưa có địa chỉ' }}</td><td data-label="Giá trị">{{ formatPrice(order.total) }}</td><template v-if="canAssign"><td data-label="Shipper"><select v-model="selections[order.id]" class="form-select" :aria-label="`Chọn shipper cho ${order.orderCode}`" :disabled="assigningId === order.id || shippersLoading || !!shippersError"><option :value="undefined">Chọn shipper</option><option v-for="shipper in sortedShippers" :key="shipper.id" :value="shipper.id">{{ shipper.fullName }} · Đang giao: {{ shipper.activeOrderCount || 0 }} đơn</option></select><small v-if="!shippersLoading && !shippersError && !sortedShippers.length">Không có shipper khả dụng</small></td><td data-label="Thao tác"><button class="btn btn-primary btn-sm target" :disabled="!selections[order.id] || assigningId !== null" @click="assign(order)">{{ assigningId === order.id ? 'Đang gán...' : 'Gán shipper' }}</button></td></template><template v-else><td data-label="Sự cố"><strong>{{ order.failureNote || order.deliveryFailureCode || 'Chưa có lý do' }}</strong><small>Lần thử {{ order.deliveryAttemptCount }}/{{ order.deliveryAttemptLimit }}</small></td><td data-label="Thao tác"><router-link class="btn btn-outline btn-sm target" :to="`/staff/orders/${order.id}`">Xử lý lại</router-link></td></template></tr></tbody></table></div>
      <div v-else-if="!ordersLoading && !ordersError" class="empty-state"><i class="bi bi-truck" aria-hidden="true"></i><h3>Không có đơn trong bộ lọc</h3><p>Chọn bộ lọc khác hoặc làm mới danh sách.</p></div>
    </div>
  </div>
</template>

<style scoped>
.page-header p{margin:4px 0 0;color:var(--text-mid);font-size:14px}.dispatch-tabs{display:flex;gap:8px;margin-bottom:12px;overflow-x:auto}.dispatch-tab{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:44px;padding:8px 16px;border:1px solid var(--border-light);border-radius:var(--radius-sm);background:var(--bg-white);color:var(--text-mid);font-weight:700;cursor:pointer}.dispatch-tab.active{border-color:var(--primary);color:var(--primary)}.dispatch-tab:focus-visible{outline:3px solid var(--primary);outline-offset:2px}.count-badge,.status-badge{display:inline-block;padding:2px 7px;border-radius:999px;background:var(--bg-light);color:var(--text-dark);font-size:12px;font-weight:750}.status-badge{margin-top:5px}.lane-state{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;color:var(--text-mid)}.error{color:var(--red-active)}.refresh-control{min-height:44px}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}.order-link{display:block;color:var(--text-dark);font-weight:750}.table td strong,.table td a,.table td small{display:block}.table td small{margin-top:4px;color:var(--text-mid)}.form-select{min-width:270px;min-height:44px}.target{display:inline-flex;align-items:center;justify-content:center;min-height:44px}@media(max-width:768px){.table thead{display:none}.table tbody tr{display:block;margin-bottom:8px;padding:12px;border:1px solid var(--border-light);border-radius:var(--radius-sm)}.table tbody td{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:6px 0;border:0;text-align:right}.table tbody td::before{content:attr(data-label);color:var(--text-mid);font-weight:650}.form-select{min-width:0;max-width:70%}}
</style>
