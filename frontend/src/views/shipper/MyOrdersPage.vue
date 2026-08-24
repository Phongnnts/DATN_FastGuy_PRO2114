<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';
import { isActiveShipperMode } from '@/utils/shipperOperations';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderActionSheet from '@/components/shipper/OrderActionSheet.vue';

const route = useRoute();
const store = useShipperStore();
const searchTerm = ref('');
const sort = ref('newest');
const dateFrom = ref('');
const dateTo = ref('');
const activeTab = ref('ASSIGNED');
const activeOrder = ref(null);
const historyOnly = computed(() => !isActiveShipperMode(route.name));
const orders = computed(() => historyOnly.value ? store.historyOrders : store.activeOrders);
const loading = computed(() => historyOnly.value ? store.historyLoading : store.listLoading);
const error = computed(() => historyOnly.value ? store.historyError : store.listError);
const totalPages = computed(() => Math.max(1, Math.ceil(store.historyTotal / (store.historySize || 1))));
let timer;
let inFlight = false;
let stopped = false;
const timestamp = order => order.deliveredAt || order.pickedUpAt || order.assignedAt || order.createdAt;
const activeStatuses = ['ASSIGNED', 'PICKED_UP'];
const visibleOrders = computed(() => {
  const q = searchTerm.value.trim().toLowerCase();
  return orders.value.filter(order => {
    return (historyOnly.value || activeStatuses.includes(order.status) && order.status === activeTab.value)
      && (!q || [order.orderCode, order.customerName, order.customerPhone, order.customerAddress].some(value => String(value || '').toLowerCase().includes(q)));
  }).sort((a, b) => (sort.value === 'oldest' ? 1 : -1) * String(timestamp(a) || '').localeCompare(String(timestamp(b) || '')) || String(a.orderCode).localeCompare(String(b.orderCode)));
});

function primaryLabel(order) {
  if (order.status === 'ASSIGNED') return 'Lấy hàng';
  if (order.status === 'PICKED_UP') return 'Giao';
  return 'Chi tiết';
}
const mapsUrl = order => order.customerAddress ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(order.customerAddress)}` : '';

async function load(silent = false) {
  if (inFlight || stopped) return;
  inFlight = true;
  try {
    if (historyOnly.value) await store.fetchHistory({ page: store.historyPage, size: store.historySize, fromDate: dateFrom.value || undefined, toDate: dateTo.value || undefined });
    else await store.fetchActiveOrders(silent);
  } catch {} finally { inFlight = false; }
}
function stopPolling() { clearInterval(timer); timer = undefined; }
function startPolling() { stopPolling(); if (!historyOnly.value && !stopped) timer = setInterval(() => load(true), 30_000); }
function retry() { load(false); }
function handleCardKeydown(event) { if (event.key === ' ') { event.preventDefault(); event.currentTarget.click(); } }
function handleTabKeydown(event) {
  const tabs = ['ASSIGNED', 'PICKED_UP'];
  const current = tabs.indexOf(activeTab.value);
  let next = current;
  if (event.key === 'ArrowRight') next = (current + 1) % tabs.length;
  else if (event.key === 'ArrowLeft') next = (current - 1 + tabs.length) % tabs.length;
  else if (event.key === 'Home') next = 0;
  else if (event.key === 'End') next = tabs.length - 1;
  else return;
  event.preventDefault();
  activeTab.value = tabs[next];
  event.currentTarget.parentElement?.querySelectorAll('[role="tab"]')[next]?.focus();
}
function openSheet(order) { activeOrder.value = order; }
async function onSheetUpdated() { activeOrder.value = null; await load(); }
function goPrev() { if (store.historyPage > 1) { store.historyPage -= 1; inFlight = false; load(); } }
function goNext() { if (store.historyPage < totalPages.value) { store.historyPage += 1; inFlight = false; load(); } }
function onDateChange() {
  if (!historyOnly.value) return;
  store.historyPage = 1;
  inFlight = false;
  load();
}

watch(historyOnly, async () => { store.invalidateListRequests(); inFlight = false; stopPolling(); activeOrder.value = null; await load(); startPolling(); });
watch([dateFrom, dateTo], onDateChange);
onMounted(async () => { await load(); startPolling(); });
onUnmounted(() => { stopped = true; store.invalidateListRequests(); stopPolling(); });
</script>

<template>
  <section>
    <h1>{{ historyOnly ? 'Lịch sử giao hàng' : 'Đơn đang hoạt động' }}</h1>
    <div class="filters order-toolbar"><label class="sr-only" for="shipper-search">Tìm đơn hàng</label><input id="shipper-search" v-model="searchTerm" class="form-input" placeholder="Mã đơn, khách, điện thoại, địa chỉ" /><label class="sr-only" for="shipper-sort">Sắp xếp</label><select id="shipper-sort" v-model="sort" class="form-select"><option value="newest">Mới nhất</option><option value="oldest">Cũ nhất</option></select><template v-if="historyOnly"><label for="shipper-date-from">Từ ngày</label><input id="shipper-date-from" v-model="dateFrom" class="form-input" type="date" :max="dateTo || undefined" /><label for="shipper-date-to">Đến ngày</label><input id="shipper-date-to" v-model="dateTo" class="form-input" type="date" :min="dateFrom || undefined" /></template></div>
    <div v-if="!historyOnly" class="tabs" role="tablist" aria-label="Trạng thái đơn giao" @keydown="handleTabKeydown"><button id="assigned-tab" role="tab" :aria-selected="activeTab === 'ASSIGNED'" aria-controls="orders-panel" :tabindex="activeTab === 'ASSIGNED' ? 0 : -1" @click="activeTab = 'ASSIGNED'">Chờ lấy</button><button id="picked-up-tab" role="tab" :aria-selected="activeTab === 'PICKED_UP'" aria-controls="orders-panel" :tabindex="activeTab === 'PICKED_UP' ? 0 : -1" @click="activeTab = 'PICKED_UP'">Đang giao</button></div>
    <div v-if="loading" class="state">Đang tải...</div><div v-else-if="error" class="state error" role="alert"><p>{{ error }}</p><button class="btn btn-outline btn-sm" @click="retry">Thử lại</button></div><div v-else-if="!visibleOrders.length" class="state">{{ searchTerm ? 'Không tìm thấy đơn hàng' : 'Chưa có đơn hàng' }}</div>
    <div v-else id="orders-panel" class="order-cards" role="tabpanel" :aria-labelledby="historyOnly ? undefined : activeTab === 'ASSIGNED' ? 'assigned-tab' : 'picked-up-tab'">
      <article v-for="order in visibleOrders" :key="order.id" class="order-card">
        <router-link class="card-body" :to="`/shipper/orders/${order.id}`" @keydown="handleCardKeydown"><div class="card-top"><strong>{{ order.orderCode }}</strong><OrderStatusBadge :status="order.status" /></div><p>{{ order.customerName }} · {{ order.customerPhone }}</p><p>{{ order.customerAddress }}</p><div class="meta"><span>{{ order.itemCount }} món</span><span>{{ order.paymentMethod }} · {{ order.paymentStatus }}</span></div><div class="card-bottom"><strong>{{ formatPrice(order.total) }}</strong><time :datetime="timestamp(order)">{{ formatDate(timestamp(order)) }}</time></div></router-link>
        <div class="quick-actions order-card-actions">
          <a v-if="order.customerPhone" class="quick-btn" :href="`tel:${order.customerPhone}`" :aria-label="`Gọi ${order.customerName}`"><i class="bi bi-telephone"></i></a>
          <a v-if="mapsUrl(order)" class="quick-btn" :href="mapsUrl(order)" target="_blank" rel="noopener noreferrer" aria-label="Mở bản đồ"><i class="bi bi-geo-alt"></i></a>
          <button class="quick-btn primary" @click="openSheet(order)">{{ primaryLabel(order) }}</button>
        </div>
      </article>
    </div>
    <div v-if="historyOnly" class="pagination">
      <button class="btn btn-outline btn-sm" :disabled="store.historyPage <= 1 || loading" @click="goPrev"><i class="bi bi-chevron-left"></i> Trước</button>
      <span class="page-info">Trang {{ store.historyPage }} / {{ totalPages }}</span>
      <button class="btn btn-outline btn-sm" :disabled="store.historyPage >= totalPages || loading" @click="goNext">Sau <i class="bi bi-chevron-right"></i></button>
    </div>
    <OrderActionSheet v-if="activeOrder" :order="activeOrder" @close="activeOrder = null" @updated="onSheetUpdated" />
  </section>
</template>

<style scoped>
h1{font-size:24px;letter-spacing:-.03em;margin-bottom:16px}.filters{display:grid;grid-template-columns:minmax(0,1fr) 110px;gap:8px;margin-bottom:12px;max-width:100%;padding:12px;background:#fff;border:1px solid var(--border-light);border-radius:14px}.filters:has(input[type="date"]){grid-template-columns:minmax(0,1fr) 110px 90px 90px}.filters>*{min-width:0}.tabs{display:flex;gap:8px;margin-bottom:12px;padding:4px;background:#e8ebef;border-radius:12px}.tabs button{flex:1;min-height:44px;padding:9px;border:0;border-radius:9px;background:transparent;font-weight:700}.tabs button[aria-selected="true"]{background:#172033;color:#fff}.order-cards{display:grid;gap:12px}.order-card{background:#fff;border:1px solid var(--border-light);border-radius:14px;padding:14px;box-shadow:0 5px 16px rgba(23,32,51,.05)}.card-body{display:block;color:inherit;text-decoration:none}.card-top,.card-bottom,.meta{display:flex;justify-content:space-between;gap:10px}.card-body p,.meta{color:var(--text-mid);font-size:13px;margin-top:6px}.card-bottom{border-top:1px solid var(--border-light);margin-top:10px;padding-top:10px}.card-bottom time{color:var(--text-light);font-size:12px}.quick-actions{display:flex;gap:8px;margin-top:12px;border-top:1px solid var(--border-light);padding-top:12px}.quick-btn{flex:1;display:flex;align-items:center;justify-content:center;gap:6px;min-height:44px;border:1px solid var(--border);border-radius:10px;background:#fff;color:#334155;font-size:13px;font-weight:700;cursor:pointer;text-decoration:none}.quick-btn.primary{border-color:#ef4444;background:#ef4444;color:#fff}.quick-btn:hover{opacity:.9}.pagination{display:flex;align-items:center;justify-content:center;gap:12px;margin-top:14px}.pagination .btn{min-height:44px}.page-info{color:var(--text-mid);font-size:13px}.state{text-align:center;padding:40px;color:var(--text-mid)}.error{color:var(--red-active)}@media(max-width:480px){.filters,.filters:has(input[type="date"]){grid-template-columns:minmax(0,1fr) minmax(0,110px)}.filters label:not(.sr-only),.filters input[type="date"]{grid-column:auto}.order-card{padding:12px}.card-top,.card-bottom,.meta{flex-wrap:wrap}.quick-actions{display:grid;grid-template-columns:44px 44px minmax(0,1fr)}}@media(min-width:900px){.order-cards{grid-template-columns:repeat(2,minmax(0,1fr))}.order-toolbar{position:sticky;top:84px;z-index:2}.tabs{max-width:460px}}
</style>
