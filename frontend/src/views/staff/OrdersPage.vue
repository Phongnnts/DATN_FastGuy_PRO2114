<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { acceptsKitchenRequest, matchesKitchenSearch, waitingDuration } from '@/utils/staffKitchen';
import { DELIVERY_FAILURE_REASON_LABEL } from '@/utils/constants';

const route = useRoute();
const router = useRouter();
const staffStore = useStaffStore();
const activeTab = ref('PENDING');
const searchTerm = ref('');
const inFlight = ref(false);
const loadedTab = ref(null);
const rows = ref([]);
const lastUpdated = ref(null);
let refreshTimer;
let requestGeneration = 0;
let queuedRefresh = null;
const tabs = [
  { key: 'PENDING', label: 'Chờ xử lý' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng giao' },
  { key: 'DELIVERY_FAILED', label: 'Giao chưa thành công' },
];
const staleErrors = ref(Object.fromEntries(tabs.map((tab) => [tab.key, ''])));
const staleError = computed(() => staleErrors.value[activeTab.value]);

function normalizedTab(value) {
  return tabs.some((tab) => tab.key === value) ? value : 'PENDING';
}

async function refresh({ silent = false } = {}) {
  if (inFlight.value) {
    queuedRefresh = { silent };
    requestGeneration += 1;
    return;
  }
  const requestTab = activeTab.value;
  const currentGeneration = ++requestGeneration;
  inFlight.value = true;
  try {
    const nextRows = await staffStore.fetchKitchenOrders(requestTab);
    if (!acceptsKitchenRequest({ requestGeneration: currentGeneration, latestGeneration: requestGeneration, requestTab, activeTab: activeTab.value })) return;
    staleErrors.value[requestTab] = '';
    rows.value = nextRows;
    loadedTab.value = requestTab;
    lastUpdated.value = new Date();
  } catch (error) {
    if (acceptsKitchenRequest({ requestGeneration: currentGeneration, latestGeneration: requestGeneration, requestTab, activeTab: activeTab.value })) {
      staleErrors.value[requestTab] = error.message || 'Không thể tải danh sách đơn hàng';
      if (!silent) loadedTab.value = null;
    }
  } finally {
    inFlight.value = false;
    if (queuedRefresh) {
      const queuedOptions = queuedRefresh;
      queuedRefresh = null;
      await refresh(queuedOptions);
    }
  }
}

async function switchTab(tab) {
  activeTab.value = normalizedTab(tab);
  await router.replace({ query: { ...route.query, tab: activeTab.value } });
  await refresh();
}

const filteredOrders = computed(() => {
  return rows.value
    .filter((order) => order.status === activeTab.value)
    .filter((order) => matchesKitchenSearch(order, searchTerm.value))
    .sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));
});

function isOverdue(order) {
  const created = new Date(order.createdAt).getTime();
  return Number.isFinite(created)
    && ['PENDING', 'CONFIRMED', 'PREPARING'].includes(order.status)
    && Date.now() - created > 25 * 60 * 1000;
}

function modifierSummary(order) {
  return (order.items || [])
    .flatMap((item) => item.modifiers || [])
    .map((modifier) => modifier.name)
    .filter(Boolean)
    .join(', ');
}

function goDetail(id) {
  router.push(`/staff/orders/${id}`);
}

watch(() => route.query.tab, async (value) => {
  const tab = normalizedTab(value);
  if (tab === activeTab.value) return;
  activeTab.value = tab;
  await refresh();
});

onMounted(async () => {
  activeTab.value = normalizedTab(route.query.tab);
  if (route.query.tab !== activeTab.value) {
    await router.replace({ query: { ...route.query, tab: activeTab.value } });
  }
  await refresh();
  refreshTimer = setInterval(() => refresh({ silent: true }), 30000);
});

onUnmounted(() => clearInterval(refreshTimer));
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1>Quản lý đơn hàng</h1>
        <p>Ưu tiên đơn theo thời gian tạo.</p>
      </div>
      <label class="search-box">
        <span class="sr-only">Tìm mã đơn, tên hoặc số điện thoại khách hàng</span>
        <i class="bi bi-search"></i>
        <input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn, tên, số điện thoại">
      </label>
    </div>
    <div class="card card-flat">
      <div class="tabs" role="tablist" aria-label="Trạng thái đơn hàng">
        <button v-for="tab in tabs" :id="`kitchen-tab-${tab.key}`" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" role="tab" :aria-controls="`kitchen-panel-${tab.key}`" :aria-selected="activeTab === tab.key" @click="switchTab(tab.key)">{{ tab.label }}</button>
      </div>
      <section :id="`kitchen-panel-${activeTab}`" role="tabpanel" :aria-labelledby="`kitchen-tab-${activeTab}`">
      <div v-if="staleError" class="stale-status" role="alert">
        <span>Dữ liệu có thể đã cũ. {{ staleError }}</span>
        <button class="btn btn-sm btn-outline" @click="refresh()">Thử lại</button>
      </div>
      <p v-if="lastUpdated" class="updated-at" aria-live="polite">Cập nhật lúc {{ formatDate(lastUpdated) }}<span v-if="inFlight"> · Đang làm mới...</span></p>
      <div v-if="inFlight && loadedTab !== activeTab" class="staff-state"><span class="spinner"></span> Đang tải đơn hàng...</div>
      <div v-else-if="filteredOrders.length" class="table-wrapper">
        <table class="table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Chờ</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id" :class="{ overdue: isOverdue(order) }">
              <td data-label="Mã đơn"><router-link :to="`/staff/orders/${order.id}`" class="order-link">{{ order.orderCode }}</router-link></td>
              <td data-label="Khách hàng"><strong>{{ order.customerName || 'Khách vãng lai' }}</strong><a v-if="order.customerPhone" class="customer-phone" :href="`tel:${order.customerPhone}`">{{ order.customerPhone }}</a></td>
              <td data-label="Sản phẩm"><span>{{ order.itemCount }} món</span><small v-if="modifierSummary(order)" class="modifier-summary">{{ modifierSummary(order) }}</small><template v-if="order.status === 'DELIVERY_FAILED'"><small><strong>{{ DELIVERY_FAILURE_REASON_LABEL[order.deliveryFailureCode] || order.deliveryFailureCode }}</strong></small><small v-if="order.failureNote">{{ order.failureNote }}</small><small>{{ formatDate(order.deliveryFailedAt) }} · Lần {{ order.deliveryAttemptCount }} / {{ order.deliveryAttemptLimit }}</small><small v-if="order.retryScheduledAt">Lịch giao lại {{ formatDate(order.retryScheduledAt) }}</small></template></td>
              <td data-label="Tổng tiền">{{ formatPrice(order.total) }}</td>
              <td data-label="Chờ"><span :class="{ 'overdue-label': isOverdue(order) }"><i v-if="isOverdue(order)" class="bi bi-exclamation-triangle-fill"></i> {{ waitingDuration(order.createdAt) }}</span><small v-if="order.createdAt">{{ formatDate(order.createdAt) }}</small></td>
              <td data-label="Trạng thái"><OrderStatusBadge :status="order.status" /></td>
              <td><button class="btn btn-sm btn-ghost" :aria-label="`Mở ${order.orderCode}`" @click="goDetail(order.id)"><i class="bi bi-chevron-right"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else-if="loadedTab === activeTab" class="empty-state"><i class="bi bi-receipt"></i><h3>Không có đơn trong hàng đợi</h3><p>Đơn mới sẽ xuất hiện tại đây.</p></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-header p { margin: 4px 0 0; color: var(--text-mid); font-size: 14px; }
.search-box { max-width: 360px; }
.order-link { color: var(--text-dark); font-weight: 750; }
.order-link:hover { color: var(--role-accent, var(--primary)); }
.customer-phone, .modifier-summary, td small { display: block; margin-top: 3px; color: var(--text-mid); font-size: 12px; }
.customer-phone:hover { color: var(--role-accent, var(--primary)); }
.modifier-summary { max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.overdue { background: #fff7ed; }
.overdue td:first-child { border-left: 3px solid #f97316; }
.overdue-label { color: #c2410c; font-size: 12px; font-weight: 700; }
.order-error, .stale-status { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 12px; padding: 10px 12px; border-radius: var(--radius-sm); color: #b91c1c; background: #fef2f2; font-size: 13px; }
.stale-status { color: #92400e; background: #fffbeb; }
.updated-at { margin: 10px 12px 0; color: var(--text-mid); font-size: 12px; text-align: right; }
.staff-state { display: flex; justify-content: center; align-items: center; gap: 10px; min-height: 180px; color: var(--text-mid); }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .search-box { width: 100%; max-width: none; }
  .table thead { display: none; }
  .table tbody tr { display: block; margin-bottom: 8px; padding: 12px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); background: #fff; }
  .table tbody td { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; border: 0; font-size: 13px; text-align: right; }
  .table tbody td::before { content: attr(data-label); color: var(--text-mid); font-weight: 650; text-align: left; }
  .table tbody td:last-child::before { content: ''; }
  .modifier-summary { max-width: 190px; }
}
</style>
