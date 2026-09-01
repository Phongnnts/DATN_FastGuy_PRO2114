<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { adminApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import AdminOrderDrawer from '@/components/admin/AdminOrderDrawer.vue';
import {
  PRIMARY_ORDER_STATUSES,
  OTHER_ORDER_STATUSES,
  normalizeOrderStatus,
  isOtherOrderStatus,
  activeOrderFilterChips,
  paymentMethodLabel,
  paymentStatusLabel,
  inlineOrderActions,
} from '@/utils/adminOrderWorkspace';

const adminStore = useAdminStore();
const route = useRoute();
const router = useRouter();
const searchTerm = ref('');
const activeStatus = computed(() => normalizeOrderStatus(route.query.status));
const attentionActive = computed(() => activeStatus.value === 'ATTENTION');
const primaryTabButtons = ref([]);
const otherMenuOpen = ref(false);
const otherTrigger = ref(null);
const otherItems = ref([]);
const advancedFiltersOpen = ref(false);
const filterPanelRef = ref(null);
const appliedFilterChips = computed(() => activeOrderFilterChips({ ...route.query, refundStatus: activeStatus.value === 'REFUND_PENDING' ? undefined : route.query.refundStatus }));
const paymentStatus = ref('');
const refundStatus = ref('');
const sortBy = ref('WAITING_DESC');
const currentPage = ref(1);
const pageSize = 20;
const filterFromDate = ref('');
const filterToDate = ref('');
const loading = ref(false);
const refreshing = ref(false);
const loadError = ref('');
const selectedOrder = ref(null);
const detailLoading = ref(false);
const detailError = ref('');
const actionBusy = ref(false);
const actionError = ref('');
const actionMessage = ref('');
const pendingAction = ref('');
const actionNote = ref('');
let detailTrigger = null;
let detailTriggerOrderId = null;
let listRequestGeneration = 0;
let detailRequestGeneration = 0;
let detailRequestOrderId = null;
let stopped = false;

const dateError = computed(() => filterFromDate.value && filterToDate.value && filterFromDate.value > filterToDate.value ? 'Từ ngày không được sau đến ngày.' : '');
const ATTENTION_REASON_LABELS = {
  PROCESSING_OVERDUE: 'Quá hạn xử lý',
  DELIVERY_FAILED: 'Giao thất bại',
  PENDING_REFUND: 'Chờ hoàn tiền',
};
function routeOrderId() {
  const value = Number.parseInt(route.query.orderId, 10);
  return Number.isInteger(value) && value > 0 ? value : null;
}
function buildOrderParams() {
  return {
    search: searchTerm.value.trim() || undefined,
    status: !attentionActive.value && activeStatus.value && activeStatus.value !== 'REFUND_PENDING' ? activeStatus.value : undefined,
    attentionOnly: attentionActive.value || undefined,
    paymentStatus: paymentStatus.value || undefined,
    refundStatus: activeStatus.value === 'REFUND_PENDING' ? 'PENDING' : refundStatus.value || undefined,
    fromDate: attentionActive.value ? undefined : filterFromDate.value || undefined,
    toDate: attentionActive.value ? undefined : filterToDate.value || undefined,
    sort: sortBy.value,
    page: currentPage.value,
    pageSize,
  };
}
async function loadOrders({ silent = false } = {}) {
  if (!attentionActive.value && dateError.value) return;
  const requestGeneration = ++listRequestGeneration;
  loading.value = !silent;
  refreshing.value = silent;
  if (!silent) adminStore.allOrders = [];
  loadError.value = '';
  try {
    await adminStore.fetchOrders(buildOrderParams(), { silent });
    const orderId = routeOrderId();
    if (orderId && detailRequestOrderId !== orderId && selectedOrder.value?.orderId !== orderId) loadOrderDetail(orderId);
  } catch (e) {
    if (requestGeneration === listRequestGeneration) loadError.value = e?.response?.data?.message || e.message || 'Không thể tải danh sách đơn hàng.';
  } finally {
    if (requestGeneration !== listRequestGeneration) return;
    loading.value = false;
    refreshing.value = false;
  }
}

function localDate(daysAgo = 0) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  return date.toLocaleDateString('en-CA');
}
function setDatePreset(days) {
  filterFromDate.value = days === 0 ? localDate() : localDate(days - 1);
  filterToDate.value = localDate();
  currentPage.value = 1;
  applyFilters();
}
async function selectStatus(status) {
  closeOtherMenu();
  await router.push({ query: { ...route.query, status: status || undefined, refundStatus: status === 'REFUND_PENDING' ? undefined : route.query.refundStatus, page: undefined } });
}
function closeOtherMenu({ restoreFocus = false } = {}) {
  otherMenuOpen.value = false;
  if (restoreFocus) nextTick(() => otherTrigger.value?.focus());
}
function openOtherMenu() {
  otherMenuOpen.value = true;
  nextTick(() => otherItems.value[0]?.focus());
}
function handlePrimaryTabKeydown(event, index) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    selectStatus(PRIMARY_ORDER_STATUSES[index].key);
    return;
  }
  if (event.key === 'ArrowLeft' && index === 0) {
    event.preventDefault();
    otherTrigger.value?.focus();
    return;
  }
  if (event.key === 'ArrowRight' && index === PRIMARY_ORDER_STATUSES.length - 1) {
    event.preventDefault();
    otherTrigger.value?.focus();
    return;
  }
  const targets = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: PRIMARY_ORDER_STATUSES.length - 1 };
  if (!(event.key in targets)) return;
  event.preventDefault();
  primaryTabButtons.value[targets[event.key]]?.focus();
}
function handleOtherTriggerKeydown(event) {
  if (['Enter', ' ', 'ArrowDown'].includes(event.key)) {
    event.preventDefault();
    openOtherMenu();
    return;
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    primaryTabButtons.value[PRIMARY_ORDER_STATUSES.length - 1]?.focus();
  } else if (event.key === 'ArrowRight' || event.key === 'Home') {
    event.preventDefault();
    primaryTabButtons.value[0]?.focus();
  } else if (event.key === 'End') {
    event.preventDefault();
    otherTrigger.value?.focus();
  }
}
async function selectOtherStatus(status) {
  await selectStatus(status);
  closeOtherMenu({ restoreFocus: true });
}
function handleOtherMenuKeydown(event, index) {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeOtherMenu({ restoreFocus: true });
    return;
  }
  const targets = { ArrowUp: index - 1, ArrowDown: index + 1, Home: 0, End: OTHER_ORDER_STATUSES.length - 1 };
  if (!(event.key in targets)) return;
  event.preventDefault();
  const next = (targets[event.key] + OTHER_ORDER_STATUSES.length) % OTHER_ORDER_STATUSES.length;
  otherItems.value[next]?.focus();
}
function removeFilter(key) {
  router.push({ query: { ...route.query, [key]: undefined, page: undefined } });
}
function revealFilters() {
  advancedFiltersOpen.value = true;
  nextTick(() => filterPanelRef.value?.focus());
}
function handleOutsidePointer(event) {
  if (otherMenuOpen.value && !event.target.closest('.other-status-menu')) closeOtherMenu();
}
function resetFilters() {
  router.push({ query: { status: route.query.status } });
}
function hydrateQuery() {
  searchTerm.value = typeof route.query.search === 'string' ? route.query.search : '';
  paymentStatus.value = ['UNPAID','PAID','FAILED','REFUNDED'].includes(route.query.paymentStatus) ? route.query.paymentStatus : '';
  refundStatus.value = activeStatus.value === 'REFUND_PENDING' ? 'PENDING' : ['PENDING','REFUNDED','REJECTED'].includes(route.query.refundStatus) ? route.query.refundStatus : '';
  sortBy.value = ['WAITING_DESC','CREATED_DESC'].includes(route.query.sort) ? route.query.sort : 'WAITING_DESC';
  filterFromDate.value = typeof route.query.fromDate === 'string' ? route.query.fromDate : '';
  filterToDate.value = typeof route.query.toDate === 'string' ? route.query.toDate : '';
  currentPage.value = Math.max(1, Number.parseInt(route.query.page, 10) || 1);
}
function applyFilters() {
  router.push({ query: {
    ...route.query,
    search: searchTerm.value.trim() || undefined,
    paymentStatus: paymentStatus.value || undefined,
    refundStatus: activeStatus.value === 'REFUND_PENDING' ? undefined : refundStatus.value || undefined,
    sort: sortBy.value === 'WAITING_DESC' ? undefined : sortBy.value,
    fromDate: attentionActive.value ? undefined : filterFromDate.value || undefined,
    toDate: attentionActive.value ? undefined : filterToDate.value || undefined,
    page: undefined,
  } });
}
function goPage(page) { router.push({ query: { ...route.query, page: page === 1 ? undefined : page } }); }
async function loadOrderDetail(orderId) {
  const requestGeneration = ++detailRequestGeneration;
  detailRequestOrderId = orderId;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const detail = await adminApi.getOrderById(orderId);
    if (stopped || requestGeneration !== detailRequestGeneration) return null;
    selectedOrder.value = detail;
    return detail;
  } catch (error) {
    if (!stopped && requestGeneration === detailRequestGeneration) detailError.value = error.message || 'Không thể tải chi tiết đơn hàng';
    return null;
  } finally {
    if (!stopped && requestGeneration === detailRequestGeneration) {
      detailLoading.value = false;
      detailRequestOrderId = null;
    }
  }
}
async function openOrder(order, event) {
  detailTrigger = event?.currentTarget || null;
  detailTriggerOrderId = order.orderId;
  selectedOrder.value = null;
  clearDrawerAction();
  await loadOrderDetail(order.orderId);
}
function openOrderFromRow(order, event) {
  if (event.target.closest('button,a,input,select,textarea')) return;
  if (event.type === 'keydown') event.preventDefault();
  openOrder(order, event);
}
function closeOrder() {
  ++detailRequestGeneration;
  detailRequestOrderId = null;
  selectedOrder.value = null;
  detailError.value = '';
  detailLoading.value = false;
  clearDrawerAction();
  if (route.query.orderId !== undefined) router.replace({ query: { ...route.query, orderId: undefined } });
  nextTick(() => {
    const trigger = detailTrigger?.isConnected ? detailTrigger : document.querySelector(`[data-order-id="${detailTriggerOrderId}"]`);
    trigger?.focus();
  });
}
function selectedAllowedActionKeys() {
  return new Set(inlineOrderActions(selectedOrder.value?.allowedActions).map(action => action.key));
}
function selectDrawerAction(action) {
  if (!selectedAllowedActionKeys().has(action) || actionBusy.value) return;
  pendingAction.value = action;
  actionNote.value = '';
  actionError.value = '';
  actionMessage.value = '';
}
function clearDrawerAction() {
  if (actionBusy.value) return;
  pendingAction.value = '';
  actionNote.value = '';
  actionError.value = '';
}
async function reloadCanonicalOrder(orderId) {
  selectedOrder.value = null;
  const detail = await loadOrderDetail(orderId);
  await loadOrders({ silent: true });
  if (detail) return true;
  actionError.value = 'Không thể tải lại dữ liệu mới nhất. Vui lòng thử lại.';
  return false;
}
async function confirmDrawerAction() {
  if (!selectedOrder.value || actionBusy.value || !selectedAllowedActionKeys().has(pendingAction.value)) return;
  if (pendingAction.value === 'CANCELLED' && !actionNote.value.trim()) {
    actionError.value = 'Vui lòng nhập lý do hủy đơn.';
    return;
  }
  const orderId = selectedOrder.value.orderId;
  actionBusy.value = true;
  actionError.value = '';
  actionMessage.value = '';
  try {
    if (pendingAction.value === 'CANCELLED') await adminApi.cancelOrder(selectedOrder.value.orderId, { expectedStatus: selectedOrder.value.status, reason: actionNote.value.trim() });
    else await adminApi.updateOrderStatus(selectedOrder.value.orderId, { expectedStatus: selectedOrder.value.status, status: pendingAction.value, note: null });
    pendingAction.value = '';
    actionNote.value = '';
    if (await reloadCanonicalOrder(orderId)) actionMessage.value = 'Đã cập nhật đơn hàng.';
  } catch (error) {
    if (error.status === 409) {
      if (await reloadCanonicalOrder(orderId)) actionError.value = 'Đơn hàng đã thay đổi. Dữ liệu mới đã được tải; kiểm tra rồi xác nhận lại.';
    } else {
      actionError.value = error.message || 'Không thể cập nhật đơn hàng.';
    }
  } finally {
    actionBusy.value = false;
  }
}

onMounted(async () => {
  document.addEventListener('pointerdown', handleOutsidePointer);
  if (activeStatus.value === 'REFUND_PENDING' && route.query.refundStatus !== undefined) {
    await router.replace({ query: { ...route.query, refundStatus: undefined, page: undefined } });
    return;
  }
  hydrateQuery();
  loadOrders();
});
onBeforeUnmount(() => { stopped = true; ++detailRequestGeneration; document.removeEventListener('pointerdown', handleOutsidePointer); });
watch(() => route.query, async () => {
  if (activeStatus.value === 'REFUND_PENDING' && route.query.refundStatus !== undefined) {
    await router.replace({ query: { ...route.query, refundStatus: undefined, page: undefined } });
    return;
  }
  hydrateQuery();
  loadOrders();
}, { deep: true });
const filtered = computed(() => adminStore.allOrders);
const initialError = computed(() => loadError.value && !filtered.value.length);
const refreshWarning = computed(() => loadError.value && filtered.value.length ? `${loadError.value} Dữ liệu gần nhất vẫn được giữ lại.` : '');
const totalPages = computed(() => Math.max(1, adminStore.orderPagination.totalPages || 1));
const paged = computed(() => adminStore.allOrders);
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <div><h1>Quản lý đơn hàng</h1><p>Theo dõi, xác nhận, giao nhận và xử lý ngoại lệ.</p></div>
      <div class="header-actions"><button class="btn btn-outline" type="button" @click="revealFilters"><i class="bi bi-sliders" aria-hidden="true"></i> Tùy chỉnh bộ lọc</button><button class="btn btn-outline" type="button" :disabled="loading || refreshing" @click="loadOrders({ silent: true })"><i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Làm mới</button></div>
    </header>

    <section class="panel queue-workspace">
      <nav class="status-segments" role="tablist" aria-label="Lọc trạng thái đơn hàng">
        <button v-for="(item, index) in PRIMARY_ORDER_STATUSES" :id="`order-status-tab-${index}`" :key="item.key" :ref="element => primaryTabButtons[index] = element" role="tab" aria-controls="order-queue-panel" :aria-selected="activeStatus === item.key" :tabindex="activeStatus === item.key ? 0 : -1" :class="{ active: activeStatus === item.key }" @keydown="handlePrimaryTabKeydown($event, index)" @click="selectStatus(item.key)">{{ item.label }}</button>
        <div class="other-status-menu"><button id="order-status-tab-other" ref="otherTrigger" type="button" role="tab" aria-controls="order-queue-panel" aria-haspopup="menu" :aria-expanded="otherMenuOpen" :aria-selected="isOtherOrderStatus(activeStatus)" :tabindex="isOtherOrderStatus(activeStatus) ? 0 : -1" :class="{ active: isOtherOrderStatus(activeStatus) }" @keydown="handleOtherTriggerKeydown" @click="otherMenuOpen ? closeOtherMenu() : openOtherMenu()">{{ OTHER_ORDER_STATUSES.find(item => item.key === activeStatus)?.label || 'Khác' }} <i class="bi bi-chevron-down" aria-hidden="true"></i></button><div v-if="otherMenuOpen" role="menu" aria-label="Trạng thái khác"><button v-for="(item, index) in OTHER_ORDER_STATUSES" :key="item.key" :ref="element => otherItems[index] = element" role="menuitemradio" :aria-checked="activeStatus === item.key" type="button" @keydown="handleOtherMenuKeydown($event, index)" @click="selectOtherStatus(item.key)">{{ item.label }}</button></div></div>
      </nav>

      <div id="order-queue-panel" role="tabpanel" aria-label="Hàng đợi đơn hàng">
      <section ref="filterPanelRef" class="filter-toolbar" tabindex="-1" aria-labelledby="order-filter-title">
        <h2 id="order-filter-title" class="sr-only">Bộ lọc đơn hàng</h2>
        <div class="search-box wide"><i class="bi bi-search" aria-hidden="true"></i><input v-model="searchTerm" class="form-input" aria-label="Tìm mã đơn, khách hàng hoặc số điện thoại" placeholder="Tìm mã đơn, khách hàng, SĐT..." @keyup.enter="applyFilters" /></div><select v-model="paymentStatus" class="form-select" aria-label="Trạng thái thanh toán"><option value="">Mọi thanh toán</option><option value="PAID">Đã thanh toán</option><option value="UNPAID">Chờ thanh toán</option><option value="FAILED">Thất bại</option><option value="REFUNDED">Đã hoàn</option></select><select v-model="refundStatus" class="form-select" aria-label="Trạng thái hoàn tiền" :disabled="activeStatus === 'REFUND_PENDING'"><option value="">Mọi hoàn tiền</option><option value="PENDING">Chờ hoàn</option><option value="REFUNDED">Đã hoàn</option><option value="REJECTED">Từ chối</option></select><select v-model="sortBy" class="form-select" aria-label="Sắp xếp"><option value="WAITING_DESC">Chờ lâu nhất</option><option value="CREATED_DESC">Mới nhất</option></select><button type="button" class="advanced-filter-trigger" :aria-expanded="advancedFiltersOpen" @click="advancedFiltersOpen = !advancedFiltersOpen">Bộ lọc</button><button class="btn btn-primary" type="button" :disabled="loading" @click="applyFilters">Áp dụng</button>
      </section>
      <section v-if="advancedFiltersOpen" class="advanced-filter-panel" aria-label="Bộ lọc ngày đặt"><div class="presets"><button type="button" :disabled="attentionActive" @click="setDatePreset(0)">Hôm nay</button><button type="button" :disabled="attentionActive" @click="setDatePreset(7)">7 ngày</button><button type="button" :disabled="attentionActive" @click="setDatePreset(30)">30 ngày</button></div><label>Từ ngày <input v-model="filterFromDate" type="date" class="form-input" :disabled="attentionActive" :max="filterToDate || undefined" /></label><label>Đến ngày <input v-model="filterToDate" type="date" class="form-input" :disabled="attentionActive" :min="filterFromDate || undefined" /></label><button class="btn btn-primary" type="button" :disabled="attentionActive || !!dateError || loading" @click="applyFilters">Áp dụng ngày</button><p v-if="attentionActive" class="date-note">Cần xử lý luôn hiển thị toàn bộ việc chưa giải quyết, không giới hạn ngày.</p><p v-else-if="dateError" class="field-error" role="alert">{{ dateError }}</p></section>
      <div v-if="appliedFilterChips.length" class="active-filters" aria-label="Bộ lọc đang áp dụng"><button v-for="chip in appliedFilterChips" :key="chip.key" type="button" @click="removeFilter(chip.key)">{{ chip.label }} <i class="bi bi-x" aria-hidden="true"></i><span class="sr-only">Bỏ bộ lọc</span></button><button type="button" class="reset-filters" @click="resetFilters">Xóa tất cả</button></div>

      <p v-if="refreshing" class="refresh-status" role="status" aria-live="polite">Đang cập nhật danh sách...</p>
      <p v-if="refreshWarning" class="refresh-warning" role="alert">{{ refreshWarning }}</p>
      <div v-if="loading && !filtered.length" class="table-skeleton" role="status" aria-label="Đang tải đơn hàng"><span v-for="row in 6" :key="row"></span></div>
      <div v-else-if="initialError" class="state error" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="loadOrders">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-inbox" aria-hidden="true"></i><strong>Không tìm thấy đơn hàng</strong><span>Thử thay đổi hoặc đặt lại bộ lọc.</span><button class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="desktop-order-table">
          <table class="table" aria-label="Danh sách đơn hàng">
            <thead><tr><th>Đơn hàng</th><th>Khách hàng</th><th>Đã chờ</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th>Hoàn tiền</th></tr></thead>
            <tbody><tr v-for="order in paged" :key="order.orderId" class="order-row-trigger" :class="{ attention: order.attentionReasons?.length }" :data-order-id="order.orderId" tabindex="0" :aria-label="`Xem chi tiết đơn hàng ${order.orderCode}`" @click="openOrderFromRow(order, $event)" @keydown.enter="openOrderFromRow(order, $event)" @keydown.space="openOrderFromRow(order, $event)">
              <td><button class="order-link" type="button" :aria-label="`Xem chi tiết đơn hàng ${order.orderCode}`" @click="openOrder(order, $event)">{{ order.orderCode }}</button></td><td><strong>{{ order.customerName || 'Khách' }}</strong></td><td><strong>{{ order.waitingMinutes }} phút</strong><small class="muted">{{ formatDate(order.createdAt) }}</small></td><td>{{ order.itemCount || 0 }} món</td><td><strong>{{ formatPrice(order.finalAmount || 0) }}</strong></td><td><span class="payment-method">{{ paymentMethodLabel(order.paymentMethod) }}</span><small :class="['payment-state', String(order.paymentStatus).toLowerCase()]">{{ paymentStatusLabel(order.paymentStatus) }}</small></td><td><OrderStatusBadge :status="order.status" /><div v-if="order.attentionReasons?.length" class="attention-reasons"><span v-for="reason in order.attentionReasons" :key="reason">{{ ATTENTION_REASON_LABELS[reason] }}</span></div></td><td><span v-if="order.refundStatus === 'REFUNDED'" class="refund-badge refund-done">Đã hoàn {{ formatPrice(order.refundAmount) }}</span><span v-else-if="order.refundStatus === 'REJECTED'" class="refund-badge refund-rejected">Đã từ chối</span><router-link v-else-if="order.refundStatus === 'PENDING'" class="refund-action" :to="{ path: '/admin/refunds', query: { status: 'PENDING' } }">Xử lý hoàn</router-link><span v-else class="muted">—</span></td>
            </tr></tbody>
          </table>
        </div>
        <div class="mobile-order-list" aria-label="Danh sách đơn hàng"><article v-for="order in paged" :key="order.orderId" :class="['mobile-order-card', { attention: order.attentionReasons?.length }]"><header><button type="button" :aria-label="`Xem chi tiết đơn hàng ${order.orderCode}`" @click="openOrder(order, $event)">{{ order.orderCode }}</button><OrderStatusBadge :status="order.status" /></header><div class="mobile-order-main"><strong>{{ order.customerName || 'Khách' }}</strong><span>Đã chờ {{ order.waitingMinutes }} phút</span></div><dl><div><dt>Sản phẩm</dt><dd>{{ order.itemCount || 0 }} món</dd></div><div><dt>Tổng tiền</dt><dd>{{ formatPrice(order.finalAmount || 0) }}</dd></div><div><dt>Thanh toán</dt><dd>{{ paymentMethodLabel(order.paymentMethod) }} · {{ paymentStatusLabel(order.paymentStatus) }}</dd></div></dl><div v-if="order.attentionReasons?.length" class="attention-reasons"><span v-for="reason in order.attentionReasons" :key="reason">{{ ATTENTION_REASON_LABELS[reason] }}</span></div><button :aria-label="`Xem chi tiết đơn hàng ${order.orderCode}`" class="mobile-detail-action" type="button" @click="openOrder(order, $event)">Xem chi tiết <i class="bi bi-arrow-right" aria-hidden="true"></i></button></article></div>
        <footer class="table-footer"><span>{{ adminStore.orderPagination.totalItems }} đơn phù hợp</span><div class="pagination"><button :disabled="currentPage === 1" aria-label="Trang trước" @click="goPage(currentPage - 1)"><i class="bi bi-chevron-left"></i></button><span>Trang {{ currentPage }} / {{ totalPages }}</span><button :disabled="currentPage >= totalPages" aria-label="Trang sau" @click="goPage(currentPage + 1)"><i class="bi bi-chevron-right"></i></button></div></footer>
      </template>
      </div>
    </section>

    <AdminOrderDrawer :open="detailLoading || !!selectedOrder || !!detailError" :loading="detailLoading" :order="selectedOrder" :error="detailError" :busy="actionBusy" :action-error="actionError" :action-message="actionMessage" :pending-action="pendingAction" :action-note="actionNote" @close="closeOrder" @select-action="selectDrawerAction" @update:action-note="actionNote = $event" @cancel-action="clearDrawerAction" @confirm-action="confirmDrawerAction" />
  </main>
</template>

<style scoped>
.orders-page { display: grid; gap: 24px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-heading h1 { font-size: 28px; line-height: 1.25; margin: 2px 0 4px; }
.page-heading p { color: var(--text-mid); font-size: 14px; }
.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.stats article { align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-xs); display: flex; gap: 14px; padding: 18px; }
.stat-icon { align-items: center; border-radius: 10px; display: inline-flex; flex: 0 0 42px; height: 42px; justify-content: center; font-size: 19px; }
.stat-icon.violet { color: var(--role-admin); background: var(--role-admin-soft); }.stat-icon.amber { color: #b45309; background: #fef3c7; }.stat-icon.green { color: #047857; background: #d1fae5; }.stat-icon.blue { color: #1d4ed8; background: #dbeafe; }
.stats small { color: var(--text-mid); display: block; font-size: 12px; margin-bottom: 2px; }.stats strong { font-size: 20px; }
.panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.status-tabs { display: flex; gap: 4px; overflow-x: auto; padding: 14px 16px 0; border-bottom: 1px solid var(--border); }
.status-tabs button { color: var(--text-mid); flex: none; font-size: 12px; font-weight: 600; padding: 9px 10px 12px; position: relative; }
.status-tabs button.active { color: var(--role-admin); }.status-tabs button.active::after { background: var(--role-admin); border-radius: 3px 3px 0 0; bottom: 0; content: ''; height: 3px; left: 8px; position: absolute; right: 8px; }
.status-tabs span { background: var(--surface); border-radius: 12px; font-size: 10px; margin-left: 3px; padding: 2px 6px; }
.filter-area { display: grid; grid-template-columns: minmax(240px, 2fr) repeat(4, minmax(130px, 1fr)); gap: 10px; padding: 16px 16px 10px; }.wide { max-width: none; }
.date-row { align-items: end; display: flex; flex-wrap: wrap; gap: 10px; padding: 0 16px 16px; position: relative; }.date-row label { color: var(--text-mid); font-size: 11px; font-weight: 600; }.date-row input { margin-top: 4px; width: 145px; }
.presets { background: var(--surface); border-radius: var(--radius-sm); display: flex; padding: 3px; }.presets button { border-radius: 6px; color: var(--text-mid); font-size: 12px; padding: 8px 10px; }.presets button:hover { background: white; color: var(--text-dark); }
.field-error, .date-note { flex-basis: 100%; font-size: 12px; }.field-error { color: var(--red-active); }.date-note { color: var(--text-mid); }.presets button:disabled { cursor: not-allowed; opacity: .45; }
.attention-reasons { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }.attention-reasons span { background: #fff3e8; border-radius: 99px; color: #9a4b24; font-size: 10px; font-weight: 700; padding: 3px 7px; white-space: nowrap; }
.desktop-order-table{overflow-x:auto;border-top:1px solid var(--admin-border)}.desktop-order-table table{width:100%;min-width:980px;border-collapse:separate;border-spacing:0}.desktop-order-table thead{position:sticky;top:128px;z-index:2;background:var(--admin-surface);pointer-events:none}.desktop-order-table tbody tr{height:68px;transition:background-color .15s ease;cursor:pointer}.desktop-order-table tbody tr:hover,.desktop-order-table tbody tr:focus,.desktop-order-table tbody tr:focus-within{background:#FFF8F5;outline:none;box-shadow:inset 3px 0 0 var(--admin-brand)}.desktop-order-table tbody tr.attention{box-shadow:inset 3px 0 0 var(--admin-warning);background:color-mix(in srgb,var(--admin-warning) 8%,var(--admin-surface))}.desktop-order-table tbody tr.attention:focus{background:#FFF8F5}.table th{color:var(--text-mid);font-size:11px;letter-spacing:.03em;text-transform:uppercase}.desktop-order-table tbody tr{scroll-margin-top:176px}.table td{vertical-align:middle;font-variant-numeric:tabular-nums}.mobile-order-list{display:none}.table-skeleton{display:grid;gap:1px;background:var(--admin-border)}.table-skeleton span{display:block;height:68px;background:linear-gradient(90deg,var(--admin-surface),var(--admin-canvas),var(--admin-surface));background-size:200% 100%;animation:skeleton-slide 1.2s ease-in-out infinite}.refresh-status,.refresh-warning{margin:0 16px 12px;padding:9px 12px;border-radius:10px}.refresh-status{color:var(--admin-muted);background:var(--admin-canvas)}.refresh-warning{color:var(--admin-danger);background:color-mix(in srgb,var(--admin-danger) 8%,var(--admin-surface))}@keyframes skeleton-slide{50%{background-position:100% 0}}
.order-link { color: var(--role-admin); font-weight: 700; }.order-link:hover, .order-link:focus { text-decoration: underline; }
.payment-method { display: block; font-size: 13px; font-weight: 700; }.payment-state { color: #b45309; display: block; font-size: 11px; }.payment-state.paid { color: #047857; }.payment-state.failed { color: #b91c1c; }
.refund-badge, .refund-action { border-radius: 99px; display: inline-flex; font-size: 11px; font-weight: 700; padding: 5px 9px; white-space: nowrap; }.refund-done { color: #047857; background: #d1fae5; }.refund-rejected { color: #b91c1c; background: #fee2e2; }.refund-action { background: #fef3c7; color: #92400e; gap: 5px; }.muted { color: var(--text-light); }
.table-footer { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; justify-content: space-between; padding: 14px 16px; }.pagination { align-items: center; display: flex; gap: 10px; }.pagination button { align-items: center; border: 1px solid var(--border); border-radius: 7px; display: inline-flex; height: 32px; justify-content: center; width: 32px; }.pagination button:disabled { cursor: not-allowed; opacity: .4; }
.state { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 10px; justify-content: center; min-height: 280px; padding: 32px; text-align: center; }.state > i { color: var(--text-light); font-size: 36px; }.state.error > i { color: var(--red-active); }.spinner { animation: spin .8s linear infinite; border: 3px solid var(--border); border-radius: 50%; border-top-color: var(--role-admin); height: 30px; width: 30px; }@keyframes spin { to { transform: rotate(360deg); } }
.modal { max-width: 520px; width: calc(100% - 32px); }.modal:focus { outline: none; }.modal-header small { color: var(--role-admin); font-size: 10px; font-weight: 800; letter-spacing: .1em; }.icon-button { border-radius: 8px; font-size: 18px; padding: 8px; }.icon-button:hover { background: var(--surface); }.refund-order-info { background: var(--surface); border-radius: var(--radius-sm); margin-bottom: 18px; padding: 8px 14px; }.refund-order-info div { display: flex; font-size: 13px; justify-content: space-between; padding: 9px 0; }.refund-order-info div + div { border-top: 1px solid var(--border); }.refund-order-info span { color: var(--text-mid); }.form-group small { color: var(--text-mid); display: block; font-size: 11px; margin-top: 5px; }
.drawer-backdrop{position:fixed;inset:0;z-index:120;background:rgba(23,33,43,.36);display:flex;justify-content:flex-end}.order-drawer{width:min(480px,100%);height:100dvh;overflow:auto;background:var(--admin-surface);padding:20px;box-shadow:-12px 0 32px rgba(23,33,43,.14);display:grid;align-content:start;gap:20px}.order-drawer>header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;border-bottom:1px solid var(--admin-border);padding-bottom:14px}.order-drawer h2{margin:2px 0}.order-drawer-close{width:44px;height:44px;border-radius:8px}.drawer-facts{display:grid;gap:10px;margin:0}.drawer-facts div{display:grid;grid-template-columns:110px 1fr;gap:12px}.drawer-facts dt{color:var(--admin-muted)}.drawer-facts dd{margin:0;font-weight:650;overflow-wrap:anywhere}.drawer-items{list-style:none;margin:0;padding:0;display:grid;gap:8px}.drawer-items li{display:flex;justify-content:space-between;gap:12px;padding-block:8px;border-bottom:1px solid var(--admin-border)}
.orders-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.orders-header h1{margin:0;font-size:30px;letter-spacing:-.025em}.orders-header p{margin:6px 0 0;color:var(--admin-muted);font-size:14px}.header-actions{display:flex;gap:8px;flex-wrap:wrap}.queue-workspace{overflow:visible;border-radius:18px}.filter-toolbar{position:sticky;top:64px;z-index:3;display:grid;grid-template-columns:minmax(240px,2fr) repeat(3,minmax(130px,1fr)) auto auto;gap:10px;padding:12px 16px;background:var(--admin-surface);border-bottom:1px solid var(--admin-border)}.filter-toolbar:focus-visible{box-shadow:inset 0 0 0 3px var(--admin-brand);outline:none}.advanced-filter-trigger{min-height:40px;padding:8px 12px;border:1px solid var(--admin-border);border-radius:10px;font-weight:650}.advanced-filter-panel{display:flex;align-items:end;flex-wrap:wrap;gap:10px;padding:12px 16px;border-bottom:1px solid var(--admin-border);background:var(--admin-canvas)}.advanced-filter-panel label{color:var(--admin-muted);font-size:12px;font-weight:650}.advanced-filter-panel input{display:block;margin-top:4px}.status-segments{display:flex;align-items:center;gap:4px;flex-wrap:wrap;padding:14px;border-bottom:1px solid var(--admin-border)}.status-segments>button,.other-status-menu>button{min-height:40px;padding:8px 12px;border-radius:10px;color:var(--admin-muted);font-weight:650}.status-segments .active{color:var(--admin-brand);background:var(--admin-brand-soft)}.other-status-menu{position:relative}.other-status-menu>[role="menu"]{position:absolute;z-index:20;top:calc(100% + 6px);right:0;display:grid;min-width:220px;padding:6px;border:1px solid var(--admin-border);border-radius:12px;background:var(--admin-surface);box-shadow:var(--shadow-md)}.other-status-menu [role^="menuitem"]{min-height:40px;padding:8px 10px;border-radius:8px;color:var(--admin-foreground);text-align:left}.other-status-menu [role^="menuitem"]:hover,.other-status-menu [role^="menuitem"]:focus-visible{background:var(--admin-brand-soft)}.filter-card{padding:16px;outline:none}.filter-card:focus-visible{box-shadow:inset 0 0 0 3px var(--admin-brand)}.filter-card-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:12px}.filter-card-heading h2{margin:0;font-size:15px}.filter-disclosure{min-height:40px;padding:8px 11px;border-radius:10px;color:var(--admin-brand);font-weight:700}.advanced-date-filters{display:flex;align-items:end;flex-wrap:wrap;gap:10px;padding-top:12px}.advanced-date-filters label{color:var(--admin-muted);font-size:11px;font-weight:650}.advanced-date-filters input{display:block;width:145px;margin-top:4px}.active-filters{display:flex;align-items:center;gap:6px;flex-wrap:wrap;margin-top:12px}.active-filters button:not(.reset-filters){min-height:32px;padding:5px 9px;border-radius:999px;color:var(--admin-foreground);background:var(--admin-canvas)}.reset-filters{min-height:32px;padding:5px 9px;color:var(--admin-brand);font-weight:700}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}.orders-page :is(button,a,input,select):focus-visible{outline:3px solid var(--admin-brand);outline-offset:2px}
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); }.filter-toolbar { grid-template-columns: repeat(2, 1fr); }.filter-area .wide { grid-column: 1 / -1; } }
@media (max-width: 767px) { .desktop-order-table{display:none}.mobile-order-list{display:grid;gap:10px;padding:12px}.mobile-order-card{display:grid;gap:12px;padding:14px;border:1px solid var(--admin-border);border-radius:14px;background:var(--admin-surface)}.mobile-order-card.attention{border-left:3px solid var(--admin-warning);background:color-mix(in srgb,var(--admin-warning) 8%,var(--admin-surface))}.mobile-order-card header,.mobile-order-main,.mobile-detail-action{display:flex;align-items:center;justify-content:space-between;gap:10px}.mobile-order-card header>button{color:var(--admin-brand);font-weight:750}.mobile-order-card dl{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;margin:0}.mobile-order-card dt{color:var(--admin-muted);font-size:12px}.mobile-order-card dd{margin:3px 0 0;font-weight:650}.mobile-detail-action{min-height:44px;color:var(--admin-brand);font-weight:700} }
@media (max-width: 640px) { .orders-header { align-items: flex-start; flex-direction: column; }.header-actions{width:100%}.header-actions .btn{flex:1}.filter-toolbar{position:static;grid-template-columns:1fr}.advanced-filter-panel>label{flex:1}.advanced-filter-panel input{width:100%}.stats { grid-template-columns: 1fr; }.filter-area { grid-template-columns: 1fr; }.advanced-date-filters > label { flex: 1; }.advanced-date-filters input { width: 100%; }.table-footer { align-items: flex-start; flex-direction: column; gap: 10px; } }
@media(prefers-reduced-motion:reduce){.desktop-order-table tbody tr{transition:none}.table-skeleton span{background:var(--admin-canvas);animation:none}}
</style>
