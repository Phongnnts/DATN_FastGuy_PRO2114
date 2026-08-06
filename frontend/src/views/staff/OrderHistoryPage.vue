<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { staffApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const router = useRouter();
const staffStore = useStaffStore();
const searchTerm = ref('');
const statusFilter = ref('ALL');
const fromDate = ref('');
const toDate = ref('');
const page = ref(1);
const size = ref(20);
const error = ref('');
const exporting = ref(false);
const orders = computed(() => staffStore.allOrders);
const total = computed(() => staffStore.historyTotal);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

function filterParams(includePage = true) {
  const params = {};
  if (includePage) Object.assign(params, { page: page.value, size: size.value });
  if (statusFilter.value !== 'ALL') params.status = statusFilter.value;
  if (fromDate.value) params.from = fromDate.value;
  if (toDate.value) params.to = toDate.value;
  if (searchTerm.value.trim()) params.search = searchTerm.value.trim();
  return params;
}
function validDates() {
  error.value = fromDate.value && toDate.value && fromDate.value > toDate.value ? 'Ngày bắt đầu không được sau ngày kết thúc.' : '';
  return !error.value;
}
async function load() {
  if (!validDates()) return;
  error.value = '';
  try {
    await staffStore.fetchHistory(filterParams());
    if (page.value > totalPages.value) {
      page.value = totalPages.value;
      await staffStore.fetchHistory(filterParams());
    }
  } catch (cause) {
    error.value = cause.message || 'Không thể tải lịch sử đơn hàng.';
  }
}
function applyFilters() {
  page.value = 1;
  load();
}
function resetFilters() {
  searchTerm.value = '';
  statusFilter.value = 'ALL';
  fromDate.value = '';
  toDate.value = '';
  page.value = 1;
  load();
}
function goTo(target) {
  if (target < 1 || target > totalPages.value || target === page.value || staffStore.loading) return;
  page.value = target;
  load();
}
function changeSize() {
  page.value = 1;
  load();
}
async function exportCsv() {
  if (!validDates() || exporting.value) return;
  exporting.value = true;
  try {
    const blob = await staffApi.exportOrders(filterParams(false));
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `lich-su-don-staff-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (cause) {
    error.value = cause.message || 'Không thể xuất CSV.';
  } finally {
    exporting.value = false;
  }
}
function openOrder(id) {
  router.push(`/staff/orders/${id}`);
}

onMounted(load);
</script>

<template>
  <main class="history-page">
    <header class="page-header">
      <div><span class="eyebrow">TRA CỨU VẬN HÀNH</span><h1>Lịch sử đơn hàng</h1><p>Tìm nhanh đơn đã hoàn thành hoặc đã hủy.</p></div>
      <button class="btn btn-outline" :disabled="exporting || staffStore.loading" @click="exportCsv"><i class="bi bi-download"></i> {{ exporting ? 'Đang xuất...' : 'Xuất CSV' }}</button>
    </header>

    <form class="filter-panel" @submit.prevent="applyFilters">
      <label class="search-field"><span>Tìm kiếm</span><div class="search-box"><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" maxlength="100" placeholder="Mã đơn, tên hoặc số điện thoại" /></div></label>
      <label><span>Trạng thái</span><select v-model="statusFilter" class="form-input"><option value="ALL">Tất cả</option><option value="DELIVERED">Đã giao</option><option value="CANCELLED">Đã hủy</option></select></label>
      <label><span>Từ ngày</span><input v-model="fromDate" type="date" class="form-input" /></label>
      <label><span>Đến ngày</span><input v-model="toDate" type="date" class="form-input" /></label>
      <div class="filter-actions"><button type="submit" class="btn btn-primary">Áp dụng</button><button type="button" class="btn btn-outline" @click="resetFilters">Đặt lại</button></div>
    </form>

    <p v-if="error" class="state-alert" role="alert"><i class="bi bi-exclamation-circle"></i><span>{{ error }}</span><button v-if="!orders.length" class="btn btn-sm btn-outline" @click="load">Thử lại</button></p>

    <section class="history-card">
      <div class="result-head"><div><strong>{{ total.toLocaleString('vi-VN') }} đơn</strong><span>Kết quả phù hợp bộ lọc</span></div><label><span>Hiển thị</span><select v-model.number="size" class="form-input" @change="changeSize"><option :value="10">10</option><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option></select></label></div>

      <div v-if="staffStore.loading && !orders.length" class="empty-state" aria-live="polite"><span class="spinner"></span><strong>Đang tải lịch sử...</strong></div>
      <div v-else-if="!orders.length" class="empty-state"><i class="bi bi-inbox"></i><strong>Không tìm thấy đơn hàng</strong><span>Thử thay đổi từ khóa hoặc khoảng ngày.</span></div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Liên hệ</th><th>Tổng tiền</th><th>Kết thúc</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody><tr v-for="order in orders" :key="order.id" @click="openOrder(order.id)"><td data-label="Mã đơn"><strong>{{ order.orderCode }}</strong></td><td data-label="Khách hàng">{{ order.customerName || `Người dùng #${order.userId}` }}</td><td data-label="Liên hệ">{{ order.customerPhone || '—' }}</td><td data-label="Tổng tiền"><strong>{{ formatPrice(order.total) }}</strong></td><td data-label="Kết thúc">{{ formatDate(order.endedAt) }}</td><td data-label="Trạng thái"><OrderStatusBadge :status="order.status" /></td><td><i class="bi bi-chevron-right"></i></td></tr></tbody>
        </table>
      </div>

      <footer v-if="orders.length" class="pagination"><span>Hiển thị {{ (page - 1) * size + 1 }}–{{ Math.min(page * size, total) }} / {{ total }} đơn</span><div><button class="btn btn-sm btn-outline" :disabled="page === 1 || staffStore.loading" aria-label="Trang trước" @click="goTo(page - 1)"><i class="bi bi-chevron-left"></i></button><b>Trang {{ page }} / {{ totalPages }}</b><button class="btn btn-sm btn-outline" :disabled="page === totalPages || staffStore.loading" aria-label="Trang sau" @click="goTo(page + 1)"><i class="bi bi-chevron-right"></i></button></div></footer>
    </section>
  </main>
</template>

<style scoped>
.history-page{color:var(--text-dark)}.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:18px}.page-header h1{margin:5px 0;font-size:30px;letter-spacing:-.03em}.page-header p{margin:0;color:var(--text-mid);font-size:13px}.eyebrow{color:var(--role-staff);font-size:10px;font-weight:800;letter-spacing:.13em}.filter-panel{display:grid;grid-template-columns:minmax(220px,1.6fr) repeat(3,minmax(140px,.7fr)) auto;align-items:end;gap:12px;padding:18px;margin-bottom:14px;border:1px solid var(--border-light);border-radius:18px;background:#fff}.filter-panel label>span,.result-head label>span{display:block;margin-bottom:6px;color:var(--text-mid);font-size:11px;font-weight:700}.filter-actions{display:flex;gap:8px}.search-box{position:relative}.search-box i{position:absolute;left:12px;top:50%;color:var(--text-mid);transform:translateY(-50%)}.search-box input{padding-left:36px}.state-alert{display:flex;align-items:center;gap:9px;padding:12px 14px;margin-bottom:14px;border:1px solid #fecaca;border-radius:14px;color:#991b1b;background:#fef2f2}.state-alert span{flex:1}.history-card{overflow:hidden;border:1px solid var(--border-light);border-radius:20px;background:#fff;box-shadow:0 8px 28px rgba(15,23,42,.04)}.result-head{display:flex;align-items:center;justify-content:space-between;padding:16px 20px;border-bottom:1px solid var(--border-light)}.result-head>div{display:flex;flex-direction:column;gap:2px}.result-head span{color:var(--text-mid);font-size:12px}.result-head label{display:flex;align-items:center;gap:8px}.result-head label>span{margin:0}.result-head select{width:80px}.table tbody tr{cursor:pointer}.table tbody tr:hover{background:#f8fafc}.table td:last-child{color:var(--text-mid);text-align:right}.empty-state{display:flex;align-items:center;justify-content:center;flex-direction:column;gap:8px;min-height:260px;color:var(--text-mid);text-align:center}.empty-state i{color:var(--role-staff);font-size:32px}.empty-state strong{color:var(--text-dark)}.pagination{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 20px;border-top:1px solid var(--border-light);color:var(--text-mid);font-size:12px}.pagination>div{display:flex;align-items:center;gap:10px}.pagination b{color:var(--text-dark);font-weight:700}@media(max-width:1050px){.filter-panel{grid-template-columns:repeat(2,1fr)}.search-field{grid-column:1/-1}.filter-actions{justify-content:flex-end}}@media(max-width:768px){.page-header{align-items:flex-start;flex-direction:column}.filter-panel{grid-template-columns:1fr}.search-field{grid-column:auto}.filter-actions{justify-content:stretch}.filter-actions .btn{flex:1}.table-wrapper{overflow:visible}.table thead{display:none}.table tbody{display:grid;gap:10px;padding:12px}.table tbody tr{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:14px;border:1px solid var(--border-light);border-radius:14px}.table td{display:flex;justify-content:space-between;padding:3px 0;border:0;text-align:right}.table td::before{color:var(--text-mid);content:attr(data-label);font-size:11px}.table td:last-child{display:none}.pagination{align-items:flex-start;flex-direction:column}.pagination>div{width:100%;justify-content:space-between}}
</style>
