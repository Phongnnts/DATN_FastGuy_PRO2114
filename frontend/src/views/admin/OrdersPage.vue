<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { adminApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const adminStore = useAdminStore();
const route = useRoute();
const router = useRouter();
const searchTerm = ref('');
const activeStatus = computed(() => statusFilters.some((item) => item.key === route.query.status) ? route.query.status : '');
const attentionActive = computed(() => activeStatus.value === 'ATTENTION');
const tabButtons = ref([]);
const paymentStatus = ref('');
const refundStatus = ref('');
const sortBy = ref('WAITING_DESC');
const currentPage = ref(1);
const pageSize = 20;
const filterFromDate = ref('');
const filterToDate = ref('');
const loading = ref(false);
const loadError = ref('');
const selectedOrder = ref(null);
const detailLoading = ref(false);
const detailError = ref('');
let detailTrigger = null;

const statusFilters = [
  { key: 'ATTENTION', label: 'Cần xử lý' },
  { key: '', label: 'Tất cả' },
  { key: 'REFUND_PENDING', label: 'Cần hoàn tiền' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng' },
  { key: 'ASSIGNED', label: 'Đã gán shipper' },
  { key: 'PICKED_UP', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'DELIVERY_FAILED', label: 'Giao chưa thành công' },
  { key: 'RETURNED_TO_STORE', label: 'Đã trả về cửa hàng' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

const dateError = computed(() => filterFromDate.value && filterToDate.value && filterFromDate.value > filterToDate.value ? 'Từ ngày không được sau đến ngày.' : '');
const statusCount = (key) => key === activeStatus.value ? adminStore.orderPagination.totalItems : 0;
const ATTENTION_REASON_LABELS = {
  PROCESSING_OVERDUE: 'Quá hạn xử lý',
  DELIVERY_FAILED: 'Giao thất bại',
  PENDING_REFUND: 'Chờ hoàn tiền',
};
async function loadOrders({ silent = false } = {}) {
  if (!attentionActive.value && dateError.value) return;
  if (!silent) loading.value = true;
  loadError.value = '';
  try {
    const params = {
      search: searchTerm.value.trim() || undefined,
      status: !attentionActive.value && activeStatus.value ? activeStatus.value : undefined,
      attentionOnly: attentionActive.value || undefined,
      paymentStatus: paymentStatus.value || undefined,
      refundStatus: activeStatus.value === 'REFUND_PENDING' ? 'PENDING' : refundStatus.value || undefined,
      fromDate: attentionActive.value ? undefined : filterFromDate.value || undefined,
      toDate: attentionActive.value ? undefined : filterToDate.value || undefined,
      sort: sortBy.value,
      page: currentPage.value,
      pageSize,
    };
    await adminStore.fetchOrders(params, { silent });
  } catch (e) {
    loadError.value = e?.response?.data?.message || e.message || 'Không thể tải danh sách đơn hàng.';
  } finally {
    if (!silent) loading.value = false;
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
  await router.push({ query: { ...route.query, status: status || undefined, page: undefined } });
}
function handleTabKeydown(event, index) {
  const targets = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: statusFilters.length - 1 };
  if (!(event.key in targets)) return;
  event.preventDefault();
  const next = (targets[event.key] + statusFilters.length) % statusFilters.length;
  selectStatus(statusFilters[next].key);
  nextTick(() => tabButtons.value[next]?.focus());
}
function resetFilters() {
  router.push({ query: { status: route.query.status } });
}
function hydrateQuery() {
  searchTerm.value = typeof route.query.search === 'string' ? route.query.search : '';
  paymentStatus.value = ['UNPAID','PAID','FAILED','REFUNDED'].includes(route.query.paymentStatus) ? route.query.paymentStatus : '';
  refundStatus.value = ['PENDING','REFUNDED','REJECTED'].includes(route.query.refundStatus) ? route.query.refundStatus : '';
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
    refundStatus: refundStatus.value || undefined,
    sort: sortBy.value === 'WAITING_DESC' ? undefined : sortBy.value,
    fromDate: attentionActive.value ? undefined : filterFromDate.value || undefined,
    toDate: attentionActive.value ? undefined : filterToDate.value || undefined,
    page: undefined,
  } });
}
function goPage(page) { router.push({ query: { ...route.query, page: page === 1 ? undefined : page } }); }
async function openOrder(order, event) {
  detailTrigger = event?.currentTarget || null;
  detailLoading.value = true; detailError.value = ''; selectedOrder.value = null;
  try { selectedOrder.value = await adminApi.getOrderById(order.orderId); await nextTick(); document.querySelector('.order-drawer-close')?.focus(); }
  catch (error) { detailError.value = error.message || 'Không thể tải chi tiết đơn hàng'; }
  finally { detailLoading.value = false; }
}
function closeOrder() { selectedOrder.value = null; detailError.value = ''; document.body.style.overflow = ''; nextTick(() => detailTrigger?.isConnected && detailTrigger.focus()); }
watch([detailLoading, selectedOrder, detailError], ([pending, order, error]) => { document.body.style.overflow = pending || order || error ? 'hidden' : ''; });

onMounted(() => { hydrateQuery(); loadOrders(); });
onBeforeUnmount(() => { document.body.style.overflow = ''; });
watch(() => route.query, () => { hydrateQuery(); loadOrders(); }, { deep: true });
const filtered = computed(() => adminStore.allOrders);
const totalPages = computed(() => Math.max(1, adminStore.orderPagination.totalPages || 1));
const paged = computed(() => adminStore.allOrders);
</script>

<template>
  <main class="orders-page">
    <header class="page-heading">
      <div><p class="eyebrow">Vận hành</p><h1>Quản lý đơn hàng</h1><p>Theo dõi thanh toán, giao nhận và hoàn tiền.</p></div>
      <button class="btn btn-outline" :disabled="loading" @click="loadOrders"><i class="bi bi-arrow-clockwise"></i> Làm mới</button>
    </header>

    <section class="panel">
      <nav class="status-tabs" role="tablist" aria-label="Lọc trạng thái">
        <button v-for="(item, index) in statusFilters" :key="item.key" :ref="element => tabButtons[index] = element" role="tab" :aria-selected="activeStatus === item.key" :tabindex="activeStatus === item.key ? 0 : -1" :class="{ active: activeStatus === item.key }" @keydown="handleTabKeydown($event, index)" @click="selectStatus(item.key)">
          {{ item.label }} <span>{{ statusCount(item.key) }}</span>
        </button>
      </nav>

      <div class="filter-area">
        <div class="search-box wide"><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" aria-label="Tìm mã đơn, khách hàng hoặc số điện thoại" placeholder="Tìm mã đơn, tên hoặc số điện thoại..." @keyup.enter="applyFilters" /></div>
        <select v-model="paymentStatus" class="form-select" aria-label="Trạng thái thanh toán"><option value="">Mọi thanh toán</option><option value="PAID">Đã thanh toán</option><option value="UNPAID">Chờ thanh toán</option><option value="FAILED">Thất bại</option><option value="REFUNDED">Đã hoàn</option></select>
        <select v-model="refundStatus" class="form-select" aria-label="Trạng thái hoàn tiền"><option value="">Mọi hoàn tiền</option><option value="PENDING">Chờ hoàn</option><option value="REFUNDED">Đã hoàn</option><option value="REJECTED">Từ chối</option></select>
        <select v-model="sortBy" class="form-select" aria-label="Sắp xếp"><option value="WAITING_DESC">Chờ lâu nhất</option><option value="CREATED_DESC">Mới nhất</option></select>
        <button class="btn btn-primary" :disabled="loading" @click="applyFilters">Áp dụng</button>
      </div>
      <div class="date-row">
        <div class="presets"><button :disabled="attentionActive" @click="setDatePreset(0)">Hôm nay</button><button :disabled="attentionActive" @click="setDatePreset(7)">7 ngày</button><button :disabled="attentionActive" @click="setDatePreset(30)">30 ngày</button></div>
        <label>Từ ngày <input v-model="filterFromDate" type="date" class="form-input" :disabled="attentionActive" :max="filterToDate || undefined" /></label>
        <label>Đến ngày <input v-model="filterToDate" type="date" class="form-input" :disabled="attentionActive" :min="filterFromDate || undefined" /></label>
        <button class="btn btn-primary" :disabled="attentionActive || !!dateError || loading" @click="applyFilters">Áp dụng ngày</button>
        <button class="btn btn-outline" @click="resetFilters"><i class="bi bi-x-circle"></i> Đặt lại</button>
        <p v-if="attentionActive" class="date-note">Cần xử lý luôn hiển thị toàn bộ việc chưa giải quyết, không giới hạn ngày.</p>
        <p v-else-if="dateError" class="field-error" role="alert">{{ dateError }}</p>
      </div>

      <div v-if="loading" class="state" role="status"><span class="spinner"></span><strong>Đang tải đơn hàng...</strong></div>
      <div v-else-if="loadError" class="state error" role="alert"><i class="bi bi-exclamation-circle"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="loadOrders">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-inbox"></i><strong>Không tìm thấy đơn hàng</strong><span>Thử thay đổi hoặc đặt lại bộ lọc.</span><button class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Đơn hàng</th><th>Khách hàng</th><th>Đã chờ</th><th>Sản phẩm</th><th>Giá trị</th><th>Thanh toán</th><th>Trạng thái</th><th>Hoàn tiền</th></tr></thead>
            <tbody><tr v-for="order in paged" :key="order.orderId">
              <td><button class="order-link" type="button" :aria-label="`Xem nhanh đơn hàng ${order.orderCode}`" @click="openOrder(order, $event)">{{ order.orderCode }}</button></td>
              <td><strong>{{ order.customerName || 'Khách' }}</strong></td>
              <td><strong>{{ order.waitingMinutes }} phút</strong><small class="muted">{{ formatDate(order.createdAt) }}</small></td>
              <td>{{ order.itemCount || 0 }} món</td>
              <td><strong>{{ formatPrice(order.finalAmount || 0) }}</strong></td>
              <td><span class="payment-method">{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }}</span><small :class="['payment-state', String(order.paymentStatus).toLowerCase()]">{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'FAILED' ? 'Thất bại' : 'Chờ thanh toán' }}</small></td>
              <td><OrderStatusBadge :status="order.status" /><div v-if="order.attentionReasons?.length" class="attention-reasons"><span v-for="reason in order.attentionReasons" :key="reason">{{ ATTENTION_REASON_LABELS[reason] }}</span></div></td>
              <td>
                <span v-if="order.refundStatus === 'REFUNDED'" class="refund-badge refund-done">Đã hoàn {{ formatPrice(order.refundAmount) }}</span>
                <span v-else-if="order.refundStatus === 'REJECTED'" class="refund-badge refund-rejected">Đã từ chối</span>
                <router-link v-else-if="order.refundStatus === 'PENDING'" class="refund-action" :to="{ path: '/admin/refunds', query: { status: 'PENDING' } }"><i class="bi bi-arrow-return-left"></i> Xử lý hoàn</router-link>
                <span v-else class="muted">—</span>
              </td>
            </tr></tbody>
          </table>
        </div>
        <footer class="table-footer"><span>{{ adminStore.orderPagination.totalItems }} đơn phù hợp</span><div class="pagination"><button :disabled="currentPage === 1" aria-label="Trang trước" @click="goPage(currentPage - 1)"><i class="bi bi-chevron-left"></i></button><span>Trang {{ currentPage }} / {{ totalPages }}</span><button :disabled="currentPage >= totalPages" aria-label="Trang sau" @click="goPage(currentPage + 1)"><i class="bi bi-chevron-right"></i></button></div></footer>
      </template>
    </section>

    <div v-if="detailLoading || selectedOrder || detailError" class="drawer-backdrop" @mousedown.self="closeOrder">
      <aside class="order-drawer" role="dialog" aria-modal="true" aria-labelledby="order-drawer-title">
        <header><div><small>Chi tiết đơn hàng</small><h2 id="order-drawer-title">{{ selectedOrder?.orderCode || 'Đang tải' }}</h2></div><button class="order-drawer-close" type="button" aria-label="Đóng chi tiết đơn hàng" @click="closeOrder"><i class="bi bi-x-lg"></i></button></header>
        <div v-if="detailLoading" class="state" role="status">Đang tải chi tiết...</div>
        <div v-else-if="detailError" class="state error" role="alert">{{ detailError }}</div>
        <template v-else-if="selectedOrder">
          <dl class="drawer-facts"><div><dt>Khách hàng</dt><dd>{{ selectedOrder.customerName }}</dd></div><div><dt>Điện thoại</dt><dd>{{ selectedOrder.customerPhone }}</dd></div><div><dt>Địa chỉ</dt><dd>{{ selectedOrder.customerAddress }}</dd></div><div><dt>Thanh toán</dt><dd>{{ selectedOrder.paymentMethod }} · {{ selectedOrder.paymentStatus }}</dd></div><div><dt>Tổng cộng</dt><dd>{{ formatPrice(selectedOrder.finalAmount) }}</dd></div></dl>
          <section><h3>Món trong đơn</h3><ul class="drawer-items"><li v-for="item in selectedOrder.items" :key="`${item.productName}-${item.variantName}`"><span>{{ item.productName }} {{ item.variantName }}</span><strong>×{{ item.quantity }}</strong></li></ul></section>
          <section><h3>Trạng thái</h3><OrderStatusBadge :status="selectedOrder.status" /><p v-if="selectedOrder.failureReason">{{ selectedOrder.failureReason }}</p></section>
          <router-link class="btn btn-primary" :to="`/admin/orders/${selectedOrder.orderId}`">Mở trang đầy đủ</router-link>
        </template>
      </aside>
    </div>
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
.table-wrapper { border-top: 1px solid var(--border); overflow-x: auto; }.table { min-width: 1020px; }.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .03em; text-transform: uppercase; }.table td { vertical-align: middle; }
.order-link { color: var(--role-admin); font-weight: 700; }.order-link:hover, .order-link:focus { text-decoration: underline; }
.payment-method { display: block; font-size: 13px; font-weight: 700; }.payment-state { color: #b45309; display: block; font-size: 11px; }.payment-state.paid { color: #047857; }.payment-state.failed { color: #b91c1c; }
.refund-badge, .refund-action { border-radius: 99px; display: inline-flex; font-size: 11px; font-weight: 700; padding: 5px 9px; white-space: nowrap; }.refund-done { color: #047857; background: #d1fae5; }.refund-rejected { color: #b91c1c; background: #fee2e2; }.refund-action { background: #fef3c7; color: #92400e; gap: 5px; }.muted { color: var(--text-light); }
.table-footer { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; justify-content: space-between; padding: 14px 16px; }.pagination { align-items: center; display: flex; gap: 10px; }.pagination button { align-items: center; border: 1px solid var(--border); border-radius: 7px; display: inline-flex; height: 32px; justify-content: center; width: 32px; }.pagination button:disabled { cursor: not-allowed; opacity: .4; }
.state { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 10px; justify-content: center; min-height: 280px; padding: 32px; text-align: center; }.state > i { color: var(--text-light); font-size: 36px; }.state.error > i { color: var(--red-active); }.spinner { animation: spin .8s linear infinite; border: 3px solid var(--border); border-radius: 50%; border-top-color: var(--role-admin); height: 30px; width: 30px; }@keyframes spin { to { transform: rotate(360deg); } }
.modal { max-width: 520px; width: calc(100% - 32px); }.modal:focus { outline: none; }.modal-header small { color: var(--role-admin); font-size: 10px; font-weight: 800; letter-spacing: .1em; }.icon-button { border-radius: 8px; font-size: 18px; padding: 8px; }.icon-button:hover { background: var(--surface); }.refund-order-info { background: var(--surface); border-radius: var(--radius-sm); margin-bottom: 18px; padding: 8px 14px; }.refund-order-info div { display: flex; font-size: 13px; justify-content: space-between; padding: 9px 0; }.refund-order-info div + div { border-top: 1px solid var(--border); }.refund-order-info span { color: var(--text-mid); }.form-group small { color: var(--text-mid); display: block; font-size: 11px; margin-top: 5px; }
.drawer-backdrop{position:fixed;inset:0;z-index:120;background:rgba(23,33,43,.36);display:flex;justify-content:flex-end}.order-drawer{width:min(480px,100%);height:100dvh;overflow:auto;background:var(--admin-surface);padding:20px;box-shadow:-12px 0 32px rgba(23,33,43,.14);display:grid;align-content:start;gap:20px}.order-drawer>header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;border-bottom:1px solid var(--admin-border);padding-bottom:14px}.order-drawer h2{margin:2px 0}.order-drawer-close{width:44px;height:44px;border-radius:8px}.drawer-facts{display:grid;gap:10px;margin:0}.drawer-facts div{display:grid;grid-template-columns:110px 1fr;gap:12px}.drawer-facts dt{color:var(--admin-muted)}.drawer-facts dd{margin:0;font-weight:650;overflow-wrap:anywhere}.drawer-items{list-style:none;margin:0;padding:0;display:grid;gap:8px}.drawer-items li{display:flex;justify-content:space-between;gap:12px;padding-block:8px;border-bottom:1px solid var(--admin-border)}
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); }.filter-area { grid-template-columns: repeat(2, 1fr); }.filter-area .wide { grid-column: 1 / -1; } }
@media (max-width: 640px) { .page-heading { align-items: flex-start; flex-direction: column; }.stats { grid-template-columns: 1fr; }.filter-area { grid-template-columns: 1fr; }.date-row > label { flex: 1; }.date-row input { width: 100%; }.table-footer { align-items: flex-start; flex-direction: column; gap: 10px; } }
</style>
