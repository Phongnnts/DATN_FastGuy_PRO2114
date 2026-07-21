<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const toast = useToast();
const shifts = ref([]);
const users = ref([]);
const loading = ref(true);
const loadError = ref('');
const showModal = ref(false);
const editing = ref(null);
const submitting = ref(false);
const actionId = ref(null);
const search = ref('');
const role = ref('');
const userId = ref('');
const status = ref('');
const fromDate = ref('');
const toDate = ref('');
const currentPage = ref(1);
const pageSize = 10;
const form = ref(emptyForm());
const firstField = ref(null);

const statusMeta = {
  SCHEDULED: { label: 'Đã lên lịch', className: 'scheduled' },
  CHECKED_IN: { label: 'Đang làm', className: 'working' },
  CHECKED_OUT: { label: 'Đã hoàn thành', className: 'completed' },
};
const employees = computed(() => users.value.filter((u) => ['STAFF', 'SHIPPER'].includes(u.roleName) && (u.status || 'ACTIVE') === 'ACTIVE'));
const roleEmployees = computed(() => role.value ? employees.value.filter((u) => u.roleName === role.value) : employees.value);
const filtered = computed(() => {
  const term = search.value.trim().toLocaleLowerCase('vi');
  return shifts.value.filter((shift) => (!status.value || shift.status === status.value) && (!term || String(shift.userName || '').toLocaleLowerCase('vi').includes(term)));
});
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => filtered.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize));
const stats = computed(() => ({
  total: filtered.value.length,
  scheduled: filtered.value.filter((shift) => shift.status === 'SCHEDULED').length,
  working: filtered.value.filter((shift) => shift.status === 'CHECKED_IN').length,
  completed: filtered.value.filter((shift) => shift.status === 'CHECKED_OUT').length,
}));
const hasFilters = computed(() => search.value || role.value || userId.value || status.value || fromDate.value || toDate.value);

function emptyForm() { return { userId: '', shiftDate: '', startTime: '08:00', endTime: '17:00' }; }
function localDate(date = new Date()) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function timeValue(value) { return value ? String(value).split('T').pop().slice(0, 5) : '—'; }
function dateValue(value) { if (!value) return '—'; const date = new Date(String(value).length === 10 ? `${value}T00:00:00` : value); return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('vi-VN').format(date); }
function dateTimeValue(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(date); }
function roleLabel(value) { return value === 'SHIPPER' ? 'Giao hàng' : 'Nhân viên'; }

async function loadShifts() {
  if (fromDate.value && toDate.value && fromDate.value > toDate.value) { loadError.value = 'Từ ngày không được sau đến ngày'; return; }
  loading.value = true;
  loadError.value = '';
  try {
    const params = {};
    if (role.value) params.role = role.value;
    if (userId.value) params.userId = userId.value;
    if (fromDate.value) params.fromDate = fromDate.value;
    if (toDate.value) params.toDate = toDate.value;
    const data = await adminApi.getShifts(params);
    shifts.value = Array.isArray(data) ? data : [];
  } catch (error) { loadError.value = error.message || 'Không thể tải ca làm'; }
  finally { loading.value = false; }
}
async function initialize() {
  try { const data = await adminApi.getUsers(); users.value = Array.isArray(data) ? data : []; }
  catch (error) { toast.error(error.message || 'Không thể tải nhân viên'); }
  await loadShifts();
}
function roleChanged() {
  if (userId.value && !roleEmployees.value.some((user) => String(user.userId) === String(userId.value))) userId.value = '';
  currentPage.value = 1;
  loadShifts();
}
function preset(type) {
  const now = new Date();
  if (type === 'today') fromDate.value = toDate.value = localDate(now);
  else { const day = now.getDay() || 7; const start = new Date(now); start.setDate(now.getDate() - day + 1); const end = new Date(start); end.setDate(start.getDate() + 6); fromDate.value = localDate(start); toDate.value = localDate(end); }
  currentPage.value = 1;
  loadShifts();
}
function reset() { search.value = ''; role.value = ''; userId.value = ''; status.value = ''; fromDate.value = ''; toDate.value = ''; currentPage.value = 1; loadShifts(); }
async function openCreate() { editing.value = null; form.value = emptyForm(); showModal.value = true; await nextTick(); firstField.value?.focus(); }
async function openEdit(shift) { editing.value = shift; form.value = { userId: shift.userId, shiftDate: shift.shiftDate, startTime: timeValue(shift.startTime), endTime: timeValue(shift.endTime) }; showModal.value = true; await nextTick(); firstField.value?.focus(); }
function closeModal() { if (!submitting.value) showModal.value = false; }
async function save() {
  if (form.value.endTime <= form.value.startTime) return toast.error('Giờ kết thúc phải sau giờ bắt đầu');
  submitting.value = true;
  try {
    const payload = { ...form.value, userId: Number(form.value.userId) };
    if (editing.value) await adminApi.updateShift(editing.value.shiftId, payload); else await adminApi.createShift(payload);
    showModal.value = false; toast.success(editing.value ? 'Đã cập nhật ca làm' : 'Đã tạo ca làm'); await loadShifts();
  } catch (error) { toast.error(error.message || 'Không thể lưu ca làm'); }
  finally { submitting.value = false; }
}
async function remove(shift) {
  if (!confirm(`Xóa ca của ${shift.userName}?`)) return;
  actionId.value = shift.shiftId;
  try { await adminApi.deleteShift(shift.shiftId); toast.success('Đã xóa ca làm'); await loadShifts(); }
  catch (error) { toast.error(error.message || 'Không thể xóa ca làm'); }
  finally { actionId.value = null; }
}
function handleModalKey(event) { if (event.key === 'Escape') closeModal(); }
watch([search, status], () => { currentPage.value = 1; });
watch(totalPages, (pages) => { if (currentPage.value > pages) currentPage.value = pages; });
onMounted(initialize);
</script>

<template>
  <main class="shifts-page">
    <header class="page-header"><div><h1>Quản lý ca làm</h1><p>Lập lịch và theo dõi chấm công của đội ngũ vận hành.</p></div><div class="header-actions"><button class="btn btn-outline" :disabled="loading" @click="loadShifts"><i class="bi bi-arrow-clockwise"></i> Làm mới</button><button class="btn btn-primary" @click="openCreate"><i class="bi bi-plus-lg"></i> Tạo ca</button></div></header>
    <section class="stats-grid" aria-label="Thống kê ca làm"><article><i class="bi bi-calendar3"></i><div><strong>{{ stats.total }}</strong><span>Tổng ca</span></div></article><article class="orange"><i class="bi bi-clock"></i><div><strong>{{ stats.scheduled }}</strong><span>Đã lên lịch</span></div></article><article class="green"><i class="bi bi-person-check"></i><div><strong>{{ stats.working }}</strong><span>Đang làm</span></div></article><article class="blue"><i class="bi bi-check2-circle"></i><div><strong>{{ stats.completed }}</strong><span>Hoàn thành</span></div></article></section>
    <section class="panel">
      <div class="toolbar"><label class="search-box"><i class="bi bi-search"></i><input v-model="search" aria-label="Tìm theo tên nhân viên" placeholder="Tìm theo tên nhân viên"><button v-if="search" aria-label="Xóa tìm kiếm" @click="search = ''"><i class="bi bi-x-circle-fill"></i></button></label><div class="presets"><button @click="preset('today')">Hôm nay</button><button @click="preset('week')">Tuần này</button></div></div>
      <div class="filters"><label>Vai trò<select v-model="role" class="form-select" @change="roleChanged"><option value="">Tất cả</option><option value="STAFF">Nhân viên</option><option value="SHIPPER">Giao hàng</option></select></label><label>Nhân viên<select v-model="userId" class="form-select" @change="loadShifts"><option value="">Tất cả</option><option v-for="user in roleEmployees" :key="user.userId" :value="user.userId">{{ user.fullName }}</option></select></label><label>Trạng thái<select v-model="status" class="form-select"><option value="">Tất cả</option><option v-for="(meta, key) in statusMeta" :key="key" :value="key">{{ meta.label }}</option></select></label><label>Từ ngày<input v-model="fromDate" class="form-input" type="date" :max="toDate || undefined" @change="loadShifts"></label><label>Đến ngày<input v-model="toDate" class="form-input" type="date" :min="fromDate || undefined" @change="loadShifts"></label><button v-if="hasFilters" class="reset" @click="reset"><i class="bi bi-x-lg"></i> Đặt lại</button></div>
      <div v-if="loadError" class="state error"><i class="bi bi-exclamation-triangle"></i><div><strong>Không thể tải dữ liệu</strong><span>{{ loadError }}</span></div><button class="btn btn-sm btn-outline" @click="loadShifts">Thử lại</button></div>
      <div v-else-if="loading" class="state"><span class="spinner"></span>Đang tải ca làm...</div>
      <div v-else-if="!paged.length" class="empty-state"><i class="bi bi-calendar-x"></i><h3>Không tìm thấy ca làm</h3><p>Thử thay đổi bộ lọc hoặc tạo ca mới.</p></div>
      <div v-else class="table-wrapper"><table class="table shifts-table"><thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Ngày & giờ</th><th>Trạng thái</th><th>Check-in</th><th>Check-out</th><th><span class="sr-only">Thao tác</span></th></tr></thead><tbody><tr v-for="shift in paged" :key="shift.shiftId"><td><strong>{{ shift.userName }}</strong><small>#{{ shift.userId }}</small></td><td><span class="role-pill">{{ roleLabel(shift.role) }}</span></td><td><strong>{{ dateValue(shift.shiftDate) }}</strong><small>{{ timeValue(shift.startTime) }} – {{ timeValue(shift.endTime) }}</small></td><td><span class="status-pill" :class="statusMeta[shift.status]?.className">{{ statusMeta[shift.status]?.label || shift.status }}</span></td><td>{{ dateTimeValue(shift.checkInAt) }}</td><td>{{ dateTimeValue(shift.checkOutAt) }}</td><td><div class="actions"><button :disabled="shift.status !== 'SCHEDULED' || actionId === shift.shiftId" :aria-label="`Sửa ca ${shift.userName}`" @click="openEdit(shift)"><i class="bi bi-pencil-square"></i></button><button class="danger" :disabled="shift.status !== 'SCHEDULED' || actionId === shift.shiftId" :aria-label="`Xóa ca ${shift.userName}`" @click="remove(shift)"><span v-if="actionId === shift.shiftId" class="spinner"></span><i v-else class="bi bi-trash3"></i></button></div></td></tr></tbody></table></div>
      <footer v-if="!loading && filtered.length" class="pagination"><span>Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filtered.length) }} / {{ filtered.length }}</span><div><button :disabled="currentPage === 1" aria-label="Trang trước" @click="currentPage--"><i class="bi bi-chevron-left"></i></button><b>{{ currentPage }} / {{ totalPages }}</b><button :disabled="currentPage === totalPages" aria-label="Trang sau" @click="currentPage++"><i class="bi bi-chevron-right"></i></button></div></footer>
    </section>
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal" @keydown="handleModalKey"><div class="modal shift-modal" role="dialog" aria-modal="true" aria-labelledby="shift-title"><div class="modal-header"><div><span class="modal-icon"><i class="bi bi-calendar-plus"></i></span><div><h2 id="shift-title">{{ editing ? 'Chỉnh sửa ca làm' : 'Tạo ca làm' }}</h2><p>{{ editing ? 'Chỉ ca chưa chấm công mới có thể sửa.' : 'Phân công lịch làm mới cho nhân viên.' }}</p></div></div><button class="modal-close" type="button" aria-label="Đóng" @click="closeModal"><i class="bi bi-x-lg"></i></button></div><form @submit.prevent="save"><div class="modal-body form-grid"><label class="full">Nhân viên *<select ref="firstField" v-model="form.userId" class="form-select" required><option disabled value="">Chọn nhân viên</option><option v-for="user in employees" :key="user.userId" :value="user.userId">{{ user.fullName }} · {{ roleLabel(user.roleName) }}</option></select></label><label class="full">Ngày làm *<input v-model="form.shiftDate" class="form-input" type="date" required></label><label>Bắt đầu *<input v-model="form.startTime" class="form-input" type="time" required></label><label>Kết thúc *<input v-model="form.endTime" class="form-input" type="time" :min="form.startTime" required></label></div><div class="modal-footer"><button type="button" class="btn btn-ghost" :disabled="submitting" @click="closeModal">Hủy</button><button class="btn btn-primary" :disabled="submitting"><span v-if="submitting" class="spinner"></span>{{ submitting ? 'Đang lưu...' : 'Lưu ca làm' }}</button></div></form></div></div>
  </main>
</template>

<style scoped>
.page-header p{margin:5px 0 0;color:var(--text-mid);font-size:14px}.header-actions{display:flex;gap:9px}.stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:18px}.stats-grid article{display:flex;align-items:center;gap:14px;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--white);box-shadow:0 4px 18px rgba(31,41,55,.04)}.stats-grid i{display:grid;width:46px;height:46px;place-items:center;border-radius:13px;color:#7c3aed;background:#ede9fe;font-size:20px}.stats-grid .orange i{color:#c2410c;background:#ffedd5}.stats-grid .green i{color:#047857;background:#d1fae5}.stats-grid .blue i{color:#1d4ed8;background:#dbeafe}.stats-grid strong,.stats-grid span{display:block}.stats-grid strong{font-size:24px}.stats-grid span{margin-top:4px;color:var(--text-mid);font-size:12px}.panel{overflow:hidden;border:1px solid var(--border);border-radius:16px;background:var(--white);box-shadow:0 8px 30px rgba(31,41,55,.05)}.toolbar{display:flex;gap:12px;padding:18px 20px 12px}.search-box{display:flex;align-items:center;width:min(440px,100%);height:42px;padding:0 13px;border:1px solid var(--border);border-radius:11px;background:var(--bg)}.search-box:focus-within{border-color:var(--primary);box-shadow:0 0 0 3px rgba(212,118,74,.12)}.search-box input{width:100%;padding:0 9px;border:0;outline:0;background:transparent;font:inherit}.search-box button{border:0;background:none;color:var(--text-mid)}.presets{display:flex;gap:7px;margin-left:auto}.presets button,.reset{padding:0 12px;border:1px solid var(--border);border-radius:9px;background:var(--white);color:var(--text-mid);cursor:pointer}.filters{display:grid;grid-template-columns:repeat(5,minmax(130px,1fr)) auto;align-items:end;gap:10px;padding:0 20px 18px;border-bottom:1px solid var(--border)}.filters label,.form-grid label{display:grid;gap:6px;color:var(--text-mid);font-size:12px;font-weight:600}.reset{height:40px}.state{display:flex;min-height:240px;align-items:center;justify-content:center;gap:10px;color:var(--text-mid)}.state.error{min-height:auto;margin:18px;padding:14px;justify-content:flex-start;border:1px solid #fecaca;border-radius:12px;color:#b91c1c;background:#fef2f2}.state.error div{display:grid}.state.error span{font-size:12px}.state.error button{margin-left:auto}.empty-state{padding:55px 20px}.empty-state>i{font-size:38px}.shifts-table{min-width:1000px}.shifts-table th{padding:12px 18px;background:#fafafa;color:#6b7280;font-size:11px;text-transform:uppercase}.shifts-table td{padding:14px 18px}.shifts-table td strong,.shifts-table td small{display:block}.shifts-table td small{margin-top:4px;color:var(--text-light);font-size:11px}.role-pill,.status-pill{display:inline-flex;padding:6px 9px;border-radius:9px;font-size:11px;font-weight:700;white-space:nowrap}.role-pill{color:#2563eb;background:#dbeafe}.status-pill.scheduled{color:#c2410c;background:#ffedd5}.status-pill.working{color:#047857;background:#d1fae5}.status-pill.completed{color:#475569;background:#e2e8f0}.actions{display:flex;justify-content:flex-end;gap:5px}.actions button{display:grid;width:34px;height:34px;place-items:center;border:1px solid transparent;border-radius:9px;color:#2563eb;background:transparent;cursor:pointer}.actions .danger{color:#dc2626}.actions button:hover:not(:disabled){border-color:currentColor}.actions button:disabled{opacity:.35;cursor:not-allowed}.pagination{display:flex;align-items:center;justify-content:space-between;padding:13px 20px;border-top:1px solid var(--border);color:var(--text-mid);font-size:12px}.pagination div{display:flex;align-items:center;gap:9px}.pagination button{display:grid;width:34px;height:34px;place-items:center;border:1px solid var(--border);border-radius:9px;background:white}.pagination button:disabled{opacity:.4}.shift-modal{max-width:580px;overflow:hidden}.modal-header>div{display:flex;align-items:center;gap:12px}.modal-header h2{margin:0;font-size:19px}.modal-header p{margin:3px 0 0;color:var(--text-mid);font-size:12px}.modal-icon{display:grid;width:42px;height:42px;place-items:center;border-radius:12px;color:var(--primary-dark);background:var(--primary-light);font-size:19px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.form-grid .full{grid-column:1/-1}.modal-footer{background:#fafafa}.modal-footer .btn-primary{min-width:130px}@media(max-width:1050px){.stats-grid{grid-template-columns:repeat(2,1fr)}.filters{grid-template-columns:repeat(3,1fr)}}@media(max-width:640px){.page-header{align-items:flex-start}.header-actions{width:100%}.header-actions .btn{flex:1}.stats-grid{gap:8px}.stats-grid article{padding:12px}.toolbar{display:grid;padding:14px}.presets{margin:0}.filters{grid-template-columns:1fr 1fr;padding:0 14px 14px}.filters label:nth-child(2){grid-column:span 2}.reset{grid-column:span 2}.form-grid{grid-template-columns:1fr}.form-grid .full{grid-column:auto}.pagination{padding:12px 14px}}
</style>
