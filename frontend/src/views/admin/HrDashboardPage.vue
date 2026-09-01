<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { adminApi } from '@/api';

const SHIFT_CODES = ['MORNING', 'AFTERNOON', 'EVENING'];
const SHIFT_LABELS = { MORNING: 'Ca sáng', AFTERNOON: 'Ca chiều', EVENING: 'Ca tối' };
const STATE_LABELS = { SCHEDULED: 'Đã lên lịch', CHECK_IN_WINDOW: 'Có thể check-in', LATE: 'Chưa check-in', ACTIVE_MANUAL: 'Đang làm', ACTIVE_AUTO: 'Đang làm · tự động', CHECK_OUT_WINDOW: 'Có thể check-out', COMPLETED_MANUAL: 'Hoàn tất', COMPLETED_AUTO: 'Hoàn tất · tự động', MISSING_STAFF: 'Thiếu nhân viên', MISSING_NEXT_SHIFT: 'Thiếu ca kế tiếp', ROLLOVER_BLOCKED: 'Bị chặn bàn giao' };

const users = ref([]);
const schedule = ref([]);
const monitoring = ref([]);
const attendance = ref([]);
const usersLoading = ref(true);
const scheduleLoading = ref(true);
const monitoringLoading = ref(true);
const attendanceLoading = ref(true);
const usersError = ref('');
const scheduleError = ref('');
const monitoringError = ref('');
const attendanceError = ref('');
let loadGeneration = 0;

const today = dateKey(new Date());
const weekStart = mondayKey(new Date());
const month = today.slice(0, 7);
const activeWorkforce = computed(() => users.value.filter(user => ['STAFF', 'SHIPPER'].includes(user.roleName) && (user.status || 'ACTIVE') === 'ACTIVE').length);
const staffedToday = computed(() => new Set(schedule.value.filter(shift => shift.shiftDate === today && shift.userId).map(shift => shift.shiftCode)).size);
const pendingAttendance = computed(() => attendance.value.filter(item => item.attendanceStatus === 'PENDING').length);
const exceptions = computed(() => monitoring.value.filter(item => item.alertSeverity === 'WARNING' || item.alertSeverity === 'CRITICAL'));
const todayShifts = computed(() => SHIFT_CODES.map(code => monitoring.value.find(item => item.shiftCode === code) || schedule.value.find(item => item.shiftDate === today && item.shiftCode === code) || { shiftCode: code }));

function dateKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function mondayKey(date) { const monday = new Date(date); const day = monday.getDay() || 7; monday.setDate(monday.getDate() - day + 1); return dateKey(monday); }
function initials(name) { return String(name || '?').split(/\s+/).filter(Boolean).slice(-2).map(part => part[0]).join('').toUpperCase(); }
function message(error, fallback) { return error?.message || fallback; }
function isStale(generation) { return generation !== loadGeneration; }

async function load() {
  const generation = ++loadGeneration;
  usersLoading.value = scheduleLoading.value = monitoringLoading.value = attendanceLoading.value = true;
  usersError.value = scheduleError.value = monitoringError.value = attendanceError.value = '';
  await Promise.allSettled([
    adminApi.getUsers().then(data => { if (!isStale(generation)) users.value = Array.isArray(data) ? data : []; }).catch(error => { if (!isStale(generation)) usersError.value = message(error, 'Không thể tải nhân sự'); }).finally(() => { if (!isStale(generation)) usersLoading.value = false; }),
    adminApi.getShiftWeek(weekStart).then(data => { if (!isStale(generation)) schedule.value = Array.isArray(data?.shifts) ? data.shifts : []; }).catch(error => { if (!isStale(generation)) scheduleError.value = message(error, 'Không thể tải lịch ca'); }).finally(() => { if (!isStale(generation)) scheduleLoading.value = false; }),
    adminApi.getShiftMonitoring().then(data => { if (!isStale(generation)) monitoring.value = Array.isArray(data) ? data : []; }).catch(error => { if (!isStale(generation)) monitoringError.value = message(error, 'Không thể tải theo dõi ca'); }).finally(() => { if (!isStale(generation)) monitoringLoading.value = false; }),
    adminApi.getShiftAttendance({ month, status: 'PENDING' }).then(data => { if (!isStale(generation)) attendance.value = Array.isArray(data) ? data : []; }).catch(error => { if (!isStale(generation)) attendanceError.value = message(error, 'Không thể tải chấm công'); }).finally(() => { if (!isStale(generation)) attendanceLoading.value = false; }),
  ]);
}

onMounted(load);
onBeforeUnmount(() => { loadGeneration++; });
</script>

<template>
  <main class="hr-dashboard">
    <header class="page-header"><div><p class="eyebrow">Đội ngũ hôm nay</p><h1>Dashboard nhân sự</h1><p>Nắm tình hình ca làm, ngoại lệ và chấm công trong một nơi.</p></div><button class="btn btn-outline" type="button" @click="load">Làm mới</button></header>
    <section class="metric-grid" aria-label="Tổng quan nhân sự">
      <article class="metric-card"><span>Nhân sự vận hành</span><strong>{{ usersLoading ? '—' : activeWorkforce }}</strong><small v-if="usersError" role="alert">{{ usersError }}</small><router-link v-else to="/admin/users">Mở Người dùng</router-link></article>
      <article class="metric-card"><span>Ca đã bố trí hôm nay</span><strong>{{ scheduleLoading ? '—' : `${staffedToday} / 3` }}</strong><small v-if="scheduleError" role="alert">{{ scheduleError }}</small><router-link v-else to="/admin/shifts">Mở lịch ca</router-link></article>
      <article class="metric-card"><span>Chấm công chờ duyệt</span><strong>{{ attendanceLoading ? '—' : pendingAttendance }}</strong><small v-if="attendanceError" role="alert">{{ attendanceError }}</small><router-link v-else to="/admin/attendance">Duyệt chấm công</router-link></article>
      <article class="metric-card"><span>Ngoại lệ cần xử lý</span><strong>{{ monitoringLoading ? '—' : exceptions.length }}</strong><small v-if="monitoringError" role="alert">{{ monitoringError }}</small><router-link v-else :to="{ path: '/admin/shifts', query: { tab: 'monitoring' } }">Mở theo dõi</router-link></article>
    </section>
    <div class="dashboard-grid">
      <section class="panel" aria-labelledby="today-shifts-title"><div class="panel-heading"><div><h2 id="today-shifts-title">Ca hôm nay</h2><p>Ba khung ca vận hành cố định</p></div><router-link to="/admin/shifts">Mở lịch ca →</router-link></div><div v-if="monitoringLoading && scheduleLoading" class="panel-state" role="status">Đang tải ca hôm nay...</div><div v-else class="shift-list"><article v-for="shift in todayShifts" :key="shift.shiftCode" class="shift-row"><div><strong>{{ SHIFT_LABELS[shift.shiftCode] }}</strong><small>{{ shift.startTime || 'Chưa có giờ' }}<template v-if="shift.endTime">–{{ shift.endTime }}</template></small></div><div class="person"><span class="avatar">{{ initials(shift.staffName) }}</span><div><strong>{{ shift.staffName || 'Chưa phân công' }}</strong><small>{{ STATE_LABELS[shift.monitoringState] || 'Theo lịch phân ca' }}</small></div></div><span class="state-pill" :class="String(shift.alertSeverity || '').toLowerCase()">{{ STATE_LABELS[shift.monitoringState] || (shift.staffName ? 'Đã lên lịch' : 'Thiếu người') }}</span></article></div></section>
      <section class="panel" aria-labelledby="attention-title"><div class="panel-heading"><div><h2 id="attention-title">Cần chú ý</h2><p>Theo mức ảnh hưởng vận hành</p></div></div><div v-if="monitoringError" class="panel-state error" role="alert">{{ monitoringError }}</div><div v-else-if="monitoringLoading" class="panel-state" role="status">Đang tải ngoại lệ...</div><div v-else-if="!exceptions.length" class="panel-state">Không có ngoại lệ cần xử lý.</div><div v-else class="attention-list"><router-link v-for="item in exceptions" :key="item.shiftCode" :to="{ path: '/admin/shifts', query: { tab: 'monitoring' } }"><span class="attention-icon" aria-hidden="true">!</span><span><strong>{{ STATE_LABELS[item.monitoringState] || item.monitoringState }}</strong><small>{{ SHIFT_LABELS[item.shiftCode] }}</small></span><i class="bi bi-chevron-right" aria-hidden="true"></i></router-link></div></section>
    </div>
  </main>
</template>

<style scoped>
.hr-dashboard{display:grid;gap:16px;color:var(--admin-foreground)}.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.page-header h1{margin:3px 0;font-size:28px;letter-spacing:-.04em}.page-header p{margin:0;color:var(--admin-muted)}.eyebrow{color:var(--admin-brand)!important;font-size:10px;font-weight:800;letter-spacing:.1em;text-transform:uppercase}.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.metric-card,.panel{border:1px solid var(--admin-hairline);border-radius:14px;background:var(--admin-surface);box-shadow:var(--admin-card-shadow)}.metric-card{position:relative;padding:18px;overflow:hidden}.metric-card::before{position:absolute;inset:0 18px auto;height:2px;border-radius:2px;background:var(--admin-brand-soft);content:""}.metric-card span,.metric-card small{display:block;color:var(--admin-muted);font-size:12px}.metric-card strong{display:block;margin:14px 0 8px;font-size:27px;line-height:1;font-variant-numeric:tabular-nums}.metric-card a,.panel-heading a{color:var(--admin-brand-dark);font-size:12px;font-weight:700}.dashboard-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(280px,.65fr);gap:14px}.panel{padding:18px}.panel-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:14px}.panel-heading h2{margin:0;font-size:17px}.panel-heading p{margin:2px 0 0;color:var(--admin-muted);font-size:12px}.shift-list,.attention-list{display:grid;gap:8px}.shift-row{display:grid;grid-template-columns:110px minmax(0,1fr) auto;align-items:center;gap:12px;padding:11px 12px;border:1px solid var(--admin-hairline);border-radius:10px;background:var(--admin-surface-subtle)}.shift-row small,.person small{display:block;color:var(--admin-muted);font-size:11px}.person{display:flex;align-items:center;gap:9px}.avatar{display:grid;width:34px;height:34px;place-items:center;border-radius:50%;background:var(--admin-brand-soft);color:var(--admin-brand-dark);font-size:10px;font-weight:800}.state-pill{padding:5px 8px;border-radius:99px;background:var(--admin-success-soft);color:var(--admin-success);font-size:10px;font-weight:750}.state-pill.warning{background:var(--admin-warning-soft);color:var(--admin-warning)}.state-pill.critical{background:var(--admin-danger-soft);color:var(--admin-danger)}.attention-list a{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:9px;min-height:58px;padding:9px;border-radius:10px;background:var(--admin-surface-subtle)}.attention-list small{display:block;color:var(--admin-muted)}.attention-icon{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:var(--admin-danger-soft);color:var(--admin-danger);font-weight:800}.panel-state{display:grid;min-height:160px;place-items:center;color:var(--admin-muted);text-align:center}.panel-state.error,.metric-card small{color:var(--admin-danger)}@media(max-width:1000px){.metric-grid{grid-template-columns:repeat(2,1fr)}.dashboard-grid{grid-template-columns:1fr}}@media(max-width:640px){.page-header{align-items:flex-start;flex-direction:column}.page-header .btn{width:100%}.metric-grid{grid-template-columns:1fr 1fr}.metric-card{padding:14px}.shift-row{grid-template-columns:88px 1fr}.state-pill{grid-column:2;justify-self:start}}@media(prefers-reduced-motion:reduce){.metric-card,.shift-row{transition:none}}
</style>
