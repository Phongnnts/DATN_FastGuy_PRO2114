<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';
import { isActiveShipperMode } from '@/utils/shipperOperations';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const route = useRoute();
const store = useShipperStore();
const searchTerm = ref('');
const sort = ref('newest');
const dateFrom = ref('');
const dateTo = ref('');
const activeTab = ref('ASSIGNED');
const historyOnly = computed(() => !isActiveShipperMode(route.name));
const orders = computed(() => historyOnly.value ? store.historyOrders : store.activeOrders);
let timer;
let inFlight = false;
let stopped = false;
const timestamp = order => order.deliveredAt || order.pickedUpAt || order.assignedAt || order.createdAt;
const visibleOrders = computed(() => {
  const q = searchTerm.value.trim().toLowerCase();
  return orders.value.filter(order => {
    const date = String(timestamp(order) || '').slice(0, 10);
    return (historyOnly.value || order.status === activeTab.value)
      && (!q || [order.orderCode, order.customerName, order.customerPhone, order.customerAddress].some(value => String(value || '').toLowerCase().includes(q)))
      && (!historyOnly.value || (!dateFrom.value || date >= dateFrom.value) && (!dateTo.value || date <= dateTo.value));
  }).sort((a, b) => (sort.value === 'oldest' ? 1 : -1) * String(timestamp(a) || '').localeCompare(String(timestamp(b) || '')) || String(a.orderCode).localeCompare(String(b.orderCode)));
});

async function load(silent = false) {
  if (inFlight || stopped) return;
  inFlight = true;
  try { if (historyOnly.value) await store.fetchHistory(); else await store.fetchActiveOrders(silent); } catch {} finally { inFlight = false; }
}
function stopPolling() { clearInterval(timer); timer = undefined; }
function startPolling() { stopPolling(); if (!historyOnly.value && !stopped) timer = setInterval(() => load(true), 30_000); }
function retry() { load(false); }
function handleCardKeydown(event) { if (event.key === ' ') { event.preventDefault(); event.currentTarget.click(); } }

watch(historyOnly, async () => { store.invalidateListRequests(); inFlight = false; stopPolling(); await load(); startPolling(); });
onMounted(async () => { await load(); startPolling(); });
onUnmounted(() => { stopped = true; store.invalidateListRequests(); stopPolling(); });
</script>

<template>
  <section>
    <h1>{{ historyOnly ? 'Lịch sử giao hàng' : 'Đơn đang hoạt động' }}</h1>
    <div class="filters"><label class="sr-only" for="shipper-search">Tìm đơn hàng</label><input id="shipper-search" v-model="searchTerm" class="form-input" placeholder="Mã đơn, khách, điện thoại, địa chỉ" /><label class="sr-only" for="shipper-sort">Sắp xếp</label><select id="shipper-sort" v-model="sort" class="form-select"><option value="newest">Mới nhất</option><option value="oldest">Cũ nhất</option></select><template v-if="historyOnly"><label for="shipper-date-from">Từ ngày</label><input id="shipper-date-from" v-model="dateFrom" class="form-input" type="date" :max="dateTo || undefined" /><label for="shipper-date-to">Đến ngày</label><input id="shipper-date-to" v-model="dateTo" class="form-input" type="date" :min="dateFrom || undefined" /></template></div>
    <div v-if="!historyOnly" class="tabs" role="tablist" aria-label="Trạng thái đơn giao"><button id="assigned-tab" role="tab" :aria-selected="activeTab === 'ASSIGNED'" aria-controls="orders-panel" :tabindex="activeTab === 'ASSIGNED' ? 0 : -1" @click="activeTab = 'ASSIGNED'">Chờ lấy</button><button id="picked-up-tab" role="tab" :aria-selected="activeTab === 'PICKED_UP'" aria-controls="orders-panel" :tabindex="activeTab === 'PICKED_UP' ? 0 : -1" @click="activeTab = 'PICKED_UP'">Đang giao</button></div>
    <div v-if="store.listLoading" class="state">Đang tải...</div><div v-else-if="store.listError" class="state error" role="alert"><p>{{ store.listError }}</p><button class="btn btn-outline btn-sm" @click="retry">Thử lại</button></div><div v-else-if="!visibleOrders.length" class="state">{{ searchTerm ? 'Không tìm thấy đơn hàng' : 'Chưa có đơn hàng' }}</div>
    <div v-else id="orders-panel" class="order-cards" role="tabpanel" :aria-labelledby="historyOnly ? undefined : activeTab === 'ASSIGNED' ? 'assigned-tab' : 'picked-up-tab'">
      <router-link v-for="order in visibleOrders" :key="order.id" :to="`/shipper/orders/${order.id}`" class="order-card" @keydown="handleCardKeydown"><div class="card-top"><strong>{{ order.orderCode }}</strong><OrderStatusBadge :status="order.status" /></div><p>{{ order.customerName }} · {{ order.customerPhone }}</p><p>{{ order.customerAddress }}</p><div class="meta"><span>{{ order.itemCount }} món</span><span>{{ order.paymentMethod }} · {{ order.paymentStatus }}</span></div><div class="card-bottom"><strong>{{ formatPrice(order.total) }}</strong><time :datetime="timestamp(order)">{{ formatDate(timestamp(order)) }}</time></div></router-link>
    </div>
  </section>
</template>

<style scoped>
h1{font-size:18px;margin-bottom:12px}.filters{display:grid;grid-template-columns:1fr 110px;gap:8px;margin-bottom:12px}.tabs{display:flex;gap:8px;margin-bottom:12px}.tabs button{flex:1;padding:9px}.order-cards{display:grid;gap:10px}.order-card{display:block;background:#fff;border:1px solid var(--border-light);border-radius:var(--radius);padding:14px;color:inherit;text-decoration:none}.card-top,.card-bottom,.meta{display:flex;justify-content:space-between;gap:10px}.order-card p,.meta{color:var(--text-mid);font-size:13px;margin-top:6px}.card-bottom{border-top:1px solid var(--border-light);margin-top:10px;padding-top:10px}.card-bottom time{color:var(--text-light);font-size:12px}.state{text-align:center;padding:40px;color:var(--text-mid)}.error{color:var(--red-active)}
</style>
