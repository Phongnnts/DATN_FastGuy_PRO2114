<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const toast = useToast();
const router = useRouter();
const adminStore = useAdminStore();
const searchTerm = ref('');
const activeStatus = ref('');
const paymentMethod = ref('');
const paymentStatus = ref('');
const refundStatus = ref('');
const sortBy = ref('date-desc');
const currentPage = ref(1);
const pageSize = 10;
const refundOrder = ref(null);
const refundDialog = ref(null);
const refundForm = ref({ refundAmount: 0, refundNote: '', status: 'REFUNDED' });
const refunding = ref(false);
const filterFromDate = ref('');
const filterToDate = ref('');
const loading = ref(false);
const loadError = ref('');

const statusFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'REFUND_PENDING', label: 'Cần hoàn tiền' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng' },
  { key: 'PICKED_UP', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

const dateError = computed(() => filterFromDate.value && filterToDate.value && filterFromDate.value > filterToDate.value ? 'Từ ngày không được sau đến ngày.' : '');
const statusCount = (key) => key === 'REFUND_PENDING'
  ? adminStore.allOrders.filter((o) => o.status === 'CANCELLED' && o.refundStatus === 'PENDING').length
  : key ? adminStore.allOrders.filter((o) => o.status === key).length : adminStore.allOrders.length;
const totalRevenue = computed(() => adminStore.allOrders.filter((o) => o.paymentStatus === 'PAID' && o.status !== 'CANCELLED').reduce((sum, o) => sum + Number(o.finalAmount || 0), 0));
const todayCount = computed(() => {
  const today = new Date().toLocaleDateString('en-CA');
  return adminStore.allOrders.filter((o) => String(o.createdAt || '').slice(0, 10) === today).length;
});

async function loadOrders() {
  if (dateError.value) return;
  loading.value = true;
  loadError.value = '';
  try {
    const params = {};
    if (filterFromDate.value) params.fromDate = filterFromDate.value;
    if (filterToDate.value) params.toDate = filterToDate.value;
    await adminStore.fetchOrders(params);
  } catch (e) {
    loadError.value = e?.response?.data?.message || e.message || 'Không thể tải danh sách đơn hàng.';
  } finally {
    loading.value = false;
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
  loadOrders();
}
function resetFilters() {
  searchTerm.value = '';
  activeStatus.value = '';
  paymentMethod.value = '';
  paymentStatus.value = '';
  refundStatus.value = '';
  sortBy.value = 'date-desc';
  filterFromDate.value = '';
  filterToDate.value = '';
  currentPage.value = 1;
  loadOrders();
}
onMounted(loadOrders);

const filtered = computed(() => {
  let list = [...adminStore.allOrders];
  if (activeStatus.value === 'REFUND_PENDING') list = list.filter((o) => o.status === 'CANCELLED' && o.refundStatus === 'PENDING');
  else if (activeStatus.value) list = list.filter((o) => o.status === activeStatus.value);
  if (paymentMethod.value) list = list.filter((o) => o.paymentMethod === paymentMethod.value);
  if (paymentStatus.value) list = list.filter((o) => o.paymentStatus === paymentStatus.value);
  if (refundStatus.value) list = list.filter((o) => o.refundStatus === refundStatus.value);
  const q = searchTerm.value.trim().toLocaleLowerCase('vi');
  if (q) list = list.filter((o) => `${o.orderCode || ''} ${o.customerName || ''}`.toLocaleLowerCase('vi').includes(q));
  return list.sort((a, b) => {
    if (sortBy.value === 'amount-asc') return Number(a.finalAmount || 0) - Number(b.finalAmount || 0);
    if (sortBy.value === 'amount-desc') return Number(b.finalAmount || 0) - Number(a.finalAmount || 0);
    const delta = new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
    return sortBy.value === 'date-asc' ? delta : -delta;
  });
});
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => filtered.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize));
watch([searchTerm, activeStatus, paymentMethod, paymentStatus, refundStatus, sortBy], () => { currentPage.value = 1; });
watch(totalPages, (pages) => { if (currentPage.value > pages) currentPage.value = pages; });

function openRefund(order) {
  refundOrder.value = order;
  refundForm.value = { refundAmount: Number(order.finalAmount || 0), refundNote: '', status: 'REFUNDED' };
  nextTick(() => refundDialog.value?.focus());
}
function closeRefund() {
  if (!refunding.value) refundOrder.value = null;
}
async function saveRefund() {
  const amount = Number(refundForm.value.refundAmount);
  const maximum = Number(refundOrder.value.finalAmount || 0);
  if (refundForm.value.status === 'REFUNDED' && (!Number.isFinite(amount) || amount <= 0 || amount > maximum)) {
    toast.error(`Số tiền hoàn phải lớn hơn 0 và không quá ${formatPrice(maximum)}`);
    return;
  }
  if (refundForm.value.status === 'REJECTED' && !refundForm.value.refundNote.trim()) {
    toast.error('Vui lòng nhập lý do từ chối');
    return;
  }
  const message = refundForm.value.status === 'REFUNDED' ? `Xác nhận hoàn ${formatPrice(amount)} cho đơn ${refundOrder.value.orderCode}?` : `Xác nhận từ chối hoàn tiền đơn ${refundOrder.value.orderCode}?`;
  if (!confirm(message)) return;
  refunding.value = true;
  try {
    await adminApi.updateRefund(refundOrder.value.orderId, { ...refundForm.value, refundAmount: amount });
    toast.success(refundForm.value.status === 'REFUNDED' ? 'Đã hoàn tiền thành công' : 'Đã từ chối hoàn tiền');
    refundOrder.value = null;
    await loadOrders();
  } catch (e) {
    toast.error(e?.response?.data?.message || e.message || 'Không thể xử lý hoàn tiền');
  } finally {
    refunding.value = false;
  }
}
</script>

<template>
  <main class="orders-page">
    <header class="page-heading">
      <div><p class="eyebrow">Vận hành</p><h1>Quản lý đơn hàng</h1><p>Theo dõi thanh toán, giao nhận và hoàn tiền.</p></div>
      <button class="btn btn-outline" :disabled="loading" @click="loadOrders"><i class="bi bi-arrow-clockwise"></i> Làm mới</button>
    </header>

    <section class="stats" aria-label="Tổng quan đơn hàng">
      <article><span class="stat-icon violet"><i class="bi bi-receipt"></i></span><div><small>Tổng đơn</small><strong>{{ adminStore.allOrders.length }}</strong></div></article>
      <article><span class="stat-icon amber"><i class="bi bi-hourglass-split"></i></span><div><small>Chờ xử lý</small><strong>{{ statusCount('PENDING') }}</strong></div></article>
      <article><span class="stat-icon green"><i class="bi bi-calendar-check"></i></span><div><small>Đơn hôm nay</small><strong>{{ todayCount }}</strong></div></article>
      <article><span class="stat-icon blue"><i class="bi bi-cash-stack"></i></span><div><small>Doanh thu đã thu</small><strong>{{ formatPrice(totalRevenue) }}</strong></div></article>
    </section>

    <section class="panel">
      <nav class="status-tabs" aria-label="Lọc trạng thái">
        <button v-for="item in statusFilters" :key="item.key" :class="{ active: activeStatus === item.key }" :aria-pressed="activeStatus === item.key" @click="activeStatus = item.key">
          {{ item.label }} <span>{{ statusCount(item.key) }}</span>
        </button>
      </nav>

      <div class="filter-area">
        <div class="search-box wide"><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" aria-label="Tìm mã đơn hoặc khách hàng" placeholder="Tìm mã đơn, tên khách hàng..." /></div>
        <select v-model="paymentMethod" class="form-select" aria-label="Phương thức thanh toán"><option value="">Mọi phương thức</option><option value="COD">COD</option><option value="BANK_TRANSFER">PayOS</option></select>
        <select v-model="paymentStatus" class="form-select" aria-label="Trạng thái thanh toán"><option value="">Mọi thanh toán</option><option value="PAID">Đã thanh toán</option><option value="PENDING">Chờ thanh toán</option><option value="FAILED">Thất bại</option></select>
        <select v-model="refundStatus" class="form-select" aria-label="Trạng thái hoàn tiền"><option value="">Mọi hoàn tiền</option><option value="PENDING">Chờ hoàn</option><option value="REFUNDED">Đã hoàn</option><option value="REJECTED">Từ chối</option></select>
        <select v-model="sortBy" class="form-select" aria-label="Sắp xếp"><option value="date-desc">Mới nhất</option><option value="date-asc">Cũ nhất</option><option value="amount-desc">Giá trị cao nhất</option><option value="amount-asc">Giá trị thấp nhất</option></select>
      </div>
      <div class="date-row">
        <div class="presets"><button @click="setDatePreset(0)">Hôm nay</button><button @click="setDatePreset(7)">7 ngày</button><button @click="setDatePreset(30)">30 ngày</button></div>
        <label>Từ ngày <input v-model="filterFromDate" type="date" class="form-input" :max="filterToDate || undefined" /></label>
        <label>Đến ngày <input v-model="filterToDate" type="date" class="form-input" :min="filterFromDate || undefined" /></label>
        <button class="btn btn-primary" :disabled="!!dateError || loading" @click="currentPage = 1; loadOrders()">Áp dụng</button>
        <button class="btn btn-outline" @click="resetFilters"><i class="bi bi-x-circle"></i> Đặt lại</button>
        <p v-if="dateError" class="field-error" role="alert">{{ dateError }}</p>
      </div>

      <div v-if="loading" class="state" role="status"><span class="spinner"></span><strong>Đang tải đơn hàng...</strong></div>
      <div v-else-if="loadError" class="state error" role="alert"><i class="bi bi-exclamation-circle"></i><strong>{{ loadError }}</strong><button class="btn btn-outline" @click="loadOrders">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-inbox"></i><strong>Không tìm thấy đơn hàng</strong><span>Thử thay đổi hoặc đặt lại bộ lọc.</span><button class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Đơn hàng</th><th>Khách hàng</th><th>Sản phẩm</th><th>Giá trị</th><th>Thanh toán</th><th>Ngày tạo</th><th>Trạng thái</th><th>Hoàn tiền</th></tr></thead>
            <tbody><tr v-for="order in paged" :key="order.orderId">
              <td><router-link class="order-link" :to="`/admin/orders/${order.orderId}`" :aria-label="`Xem đơn hàng ${order.orderCode}`">{{ order.orderCode }}</router-link></td>
              <td><strong>{{ order.customerName || 'Khách' }}</strong></td>
              <td>{{ order.itemCount || 0 }} món</td>
              <td><strong>{{ formatPrice(order.finalAmount || 0) }}</strong></td>
              <td><span class="payment-method">{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }}</span><small :class="['payment-state', String(order.paymentStatus).toLowerCase()]">{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'FAILED' ? 'Thất bại' : 'Chờ thanh toán' }}</small></td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td><OrderStatusBadge :status="order.status" /></td>
              <td>
                <span v-if="order.refundStatus === 'REFUNDED'" class="refund-badge refund-done">Đã hoàn {{ formatPrice(order.refundAmount) }}</span>
                <span v-else-if="order.refundStatus === 'REJECTED'" class="refund-badge refund-rejected">Đã từ chối</span>
                <button v-else-if="order.refundStatus === 'PENDING'" class="refund-action" @click="openRefund(order)"><i class="bi bi-arrow-return-left"></i> Xử lý hoàn</button>
                <span v-else class="muted">—</span>
              </td>
            </tr></tbody>
          </table>
        </div>
        <footer class="table-footer"><span>Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filtered.length) }} / {{ filtered.length }} đơn</span><div class="pagination"><button :disabled="currentPage === 1" aria-label="Trang trước" @click="currentPage--"><i class="bi bi-chevron-left"></i></button><span>Trang {{ currentPage }} / {{ totalPages }}</span><button :disabled="currentPage === totalPages" aria-label="Trang sau" @click="currentPage++"><i class="bi bi-chevron-right"></i></button></div></footer>
      </template>
    </section>

    <div v-if="refundOrder" class="modal-overlay" @click.self="closeRefund" @keydown.esc="closeRefund">
      <form ref="refundDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="refund-title" tabindex="-1" @submit.prevent="saveRefund">
        <div class="modal-header"><div><small>HOÀN TIỀN</small><h3 id="refund-title">{{ refundOrder.orderCode }}</h3></div><button type="button" class="icon-button" aria-label="Đóng" @click="closeRefund"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div class="refund-order-info"><div><span>Khách hàng</span><strong>{{ refundOrder.customerName || 'Khách' }}</strong></div><div><span>Giá trị đơn</span><strong>{{ formatPrice(refundOrder.finalAmount) }}</strong></div><div><span>Thanh toán</span><strong>{{ refundOrder.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }} · {{ refundOrder.paymentStatus }}</strong></div></div>
          <label class="form-group"><span class="form-label">Hành động</span><select v-model="refundForm.status" class="form-select"><option value="REFUNDED">Xác nhận đã hoàn tiền</option><option value="REJECTED">Từ chối hoàn tiền</option></select></label>
          <label v-if="refundForm.status === 'REFUNDED'" class="form-group"><span class="form-label">Số tiền hoàn</span><input v-model.number="refundForm.refundAmount" class="form-input" type="number" min="1" step="1000" :max="Number(refundOrder.finalAmount)" required /><small>Tối đa {{ formatPrice(refundOrder.finalAmount) }}</small></label>
          <label class="form-group"><span class="form-label">{{ refundForm.status === 'REJECTED' ? 'Lý do từ chối' : 'Ghi chú' }}</span><textarea v-model="refundForm.refundNote" class="form-input" rows="3" :required="refundForm.status === 'REJECTED'"></textarea></label>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-outline" @click="closeRefund">Hủy</button><button class="btn" :class="refundForm.status === 'REFUNDED' ? 'btn-primary' : 'btn-danger'" :disabled="refunding">{{ refunding ? 'Đang xử lý...' : 'Xác nhận' }}</button></div>
      </form>
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
.field-error { color: var(--red-active); flex-basis: 100%; font-size: 12px; }
.table-wrapper { border-top: 1px solid var(--border); overflow-x: auto; }.table { min-width: 1020px; }.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .03em; text-transform: uppercase; }.table td { vertical-align: middle; }
.order-link { color: var(--role-admin); font-weight: 700; }.order-link:hover, .order-link:focus { text-decoration: underline; }
.payment-method { display: block; font-size: 13px; font-weight: 700; }.payment-state { color: #b45309; display: block; font-size: 11px; }.payment-state.paid { color: #047857; }.payment-state.failed { color: #b91c1c; }
.refund-badge, .refund-action { border-radius: 99px; display: inline-flex; font-size: 11px; font-weight: 700; padding: 5px 9px; white-space: nowrap; }.refund-done { color: #047857; background: #d1fae5; }.refund-rejected { color: #b91c1c; background: #fee2e2; }.refund-action { background: #fef3c7; color: #92400e; gap: 5px; }.muted { color: var(--text-light); }
.table-footer { align-items: center; color: var(--text-mid); display: flex; font-size: 12px; justify-content: space-between; padding: 14px 16px; }.pagination { align-items: center; display: flex; gap: 10px; }.pagination button { align-items: center; border: 1px solid var(--border); border-radius: 7px; display: inline-flex; height: 32px; justify-content: center; width: 32px; }.pagination button:disabled { cursor: not-allowed; opacity: .4; }
.state { align-items: center; color: var(--text-mid); display: flex; flex-direction: column; gap: 10px; justify-content: center; min-height: 280px; padding: 32px; text-align: center; }.state > i { color: var(--text-light); font-size: 36px; }.state.error > i { color: var(--red-active); }.spinner { animation: spin .8s linear infinite; border: 3px solid var(--border); border-radius: 50%; border-top-color: var(--role-admin); height: 30px; width: 30px; }@keyframes spin { to { transform: rotate(360deg); } }
.modal { max-width: 520px; width: calc(100% - 32px); }.modal:focus { outline: none; }.modal-header small { color: var(--role-admin); font-size: 10px; font-weight: 800; letter-spacing: .1em; }.icon-button { border-radius: 8px; font-size: 18px; padding: 8px; }.icon-button:hover { background: var(--surface); }.refund-order-info { background: var(--surface); border-radius: var(--radius-sm); margin-bottom: 18px; padding: 8px 14px; }.refund-order-info div { display: flex; font-size: 13px; justify-content: space-between; padding: 9px 0; }.refund-order-info div + div { border-top: 1px solid var(--border); }.refund-order-info span { color: var(--text-mid); }.form-group small { color: var(--text-mid); display: block; font-size: 11px; margin-top: 5px; }
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); }.filter-area { grid-template-columns: repeat(2, 1fr); }.filter-area .wide { grid-column: 1 / -1; } }
@media (max-width: 640px) { .page-heading { align-items: flex-start; flex-direction: column; }.stats { grid-template-columns: 1fr; }.filter-area { grid-template-columns: 1fr; }.date-row > label { flex: 1; }.date-row input { width: 100%; }.table-footer { align-items: flex-start; flex-direction: column; gap: 10px; } }
</style>
