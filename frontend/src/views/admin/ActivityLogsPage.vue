<script setup>
import { onMounted, reactive, ref } from 'vue';
import { adminApi } from '@/api';

const ACTION_LABELS = {
  ORDER_CANCELLED: 'Hủy đơn hàng',
  ORDER_REFUND_RECORDED: 'Ghi nhận hoàn tiền',
  DELIVERY_ATTEMPT_OVERRIDDEN: 'Ghi đè lần giao hàng',
  ATTENDANCE_APPROVED: 'Duyệt chấm công',
  STAFF_PAY_RATE_CREATED: 'Tạo mức lương nhân viên',
  STOCK_COUNT_APPROVED: 'Duyệt kiểm kê kho',
};
const filters = reactive({ from: '', to: '', actionType: '', actorUserId: '' });
const items = ref([]);
const pagination = reactive({ page: 1, pageSize: 20, totalItems: 0, totalPages: 0 });
const loading = ref(true);
const error = ref('');
let loadGeneration = 0;

function dateTime(value, end = false) {
  if (!value) return undefined;
  return `${value}T${end ? '23:59:59' : '00:00:00'}`;
}
function metadataEntries(metadata) {
  return Object.entries(metadata || {});
}
function displayValue(value) {
  return value === null ? '—' : String(value);
}
function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value));
}
async function load(page = pagination.page) {
  const generation = ++loadGeneration;
  loading.value = true;
  error.value = '';
  const params = {
    from: dateTime(filters.from),
    to: dateTime(filters.to, true),
    actionType: filters.actionType || undefined,
    actorUserId: filters.actorUserId ? Number(filters.actorUserId) : undefined,
    page,
    pageSize: pagination.pageSize,
  };
  try {
    const data = await adminApi.getActivityLogs(params);
    if (generation !== loadGeneration) return;
    items.value = Array.isArray(data?.items) ? data.items : [];
    Object.assign(pagination, data?.pagination || { page, pageSize: pagination.pageSize, totalItems: 0, totalPages: 0 });
  } catch (cause) {
    if (generation === loadGeneration) error.value = cause.message || 'Không thể tải nhật ký hoạt động';
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}
function applyFilters() {
  load(1);
}

onMounted(() => load(1));
</script>

<template>
  <main class="activity-page">
    <header class="page-header"><div><h1>Nhật ký hoạt động</h1><p>Theo dõi các thao tác quản trị mới nhất.</p></div></header>
    <form class="filters" @submit.prevent="applyFilters">
      <label>Từ ngày<input v-model="filters.from" type="date" class="form-control" /></label>
      <label>Đến ngày<input v-model="filters.to" type="date" class="form-control" /></label>
      <label>Loại thao tác<select v-model="filters.actionType" class="form-select"><option value="">Tất cả</option><option v-for="(label, value) in ACTION_LABELS" :key="value" :value="value">{{ label }}</option></select></label>
      <label>ID người thực hiện<input v-model="filters.actorUserId" type="number" min="1" inputmode="numeric" class="form-control" /></label>
      <button class="btn btn-primary" type="submit" :disabled="loading">Lọc</button>
    </form>

    <div v-if="error" class="state error" role="alert">{{ error }} <button class="btn btn-outline" type="button" @click="load()">Thử lại</button></div>
    <div v-else-if="loading" class="state" aria-live="polite">Đang tải nhật ký...</div>
    <div v-else-if="!items.length" class="state">Không có nhật ký phù hợp.</div>
    <section v-else class="log-list" aria-label="Danh sách nhật ký hoạt động">
      <article v-for="item in items" :key="item.activityLogId" class="log-card">
        <div class="log-main"><strong>{{ ACTION_LABELS[item.actionType] }}</strong><span>{{ formatDate(item.createdAt) }}</span></div>
        <p>{{ item.summary }}</p>
        <dl class="facts"><div><dt>Người thực hiện</dt><dd>{{ item.actor.fullName }} (#{{ item.actor.userId }})</dd></div><div><dt>Đối tượng</dt><dd>{{ item.targetType }}<template v-if="item.targetId"> #{{ item.targetId }}</template></dd></div></dl>
        <dl v-if="metadataEntries(item.metadata).length" class="metadata"><div v-for="([key, value]) in metadataEntries(item.metadata)" :key="key"><dt>{{ key }}</dt><dd>{{ displayValue(value) }}</dd></div></dl>
      </article>
    </section>

    <nav v-if="!loading && !error && pagination.totalPages > 0" class="pagination" aria-label="Phân trang nhật ký">
      <button class="btn btn-outline" type="button" :disabled="pagination.page <= 1" @click="load(pagination.page - 1)">Trang trước</button>
      <span>Trang {{ pagination.page }}/{{ pagination.totalPages }} · {{ pagination.totalItems }} mục</span>
      <button class="btn btn-outline" type="button" :disabled="pagination.page >= pagination.totalPages" @click="load(pagination.page + 1)">Trang sau</button>
    </nav>
  </main>
</template>

<style scoped>
.page-header p{margin:5px 0 0;color:var(--text-mid)}.filters{display:grid;grid-template-columns:repeat(4,minmax(150px,1fr)) auto;align-items:end;gap:12px;margin:18px 0;padding:16px;border:1px solid var(--border);border-radius:16px;background:var(--white)}.filters label{display:grid;gap:6px;color:var(--text-mid);font-size:12px;font-weight:700}.filters .btn{min-height:44px}.state{display:flex;min-height:220px;align-items:center;justify-content:center;gap:12px;color:var(--text-mid)}.error{color:#b91c1c}.log-list{display:grid;gap:12px}.log-card{padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--white)}.log-card p{margin:10px 0}.log-main{display:flex;justify-content:space-between;gap:12px}.log-main span{color:var(--text-mid);font-size:13px}.facts,.metadata{display:flex;flex-wrap:wrap;gap:10px 24px;margin:12px 0 0}.facts div,.metadata div{min-width:140px}.facts dt,.metadata dt{color:var(--text-mid);font-size:11px;font-weight:700}.facts dd,.metadata dd{margin:3px 0 0;overflow-wrap:anywhere}.metadata{padding-top:12px;border-top:1px solid var(--border-light)}.pagination{display:flex;align-items:center;justify-content:center;gap:14px;margin-top:18px}.btn:focus-visible,input:focus-visible,select:focus-visible{outline:3px solid var(--primary);outline-offset:2px}@media (max-width: 768px){.filters{grid-template-columns:1fr}.filters .btn{width:100%}.log-main{align-items:flex-start;flex-direction:column}.facts,.metadata{display:grid;grid-template-columns:1fr}.pagination{justify-content:space-between}.pagination span{text-align:center;font-size:12px}}
</style>
