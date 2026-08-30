<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const SHIFT_CODES = ['MORNING', 'AFTERNOON', 'EVENING'];
const CODE_LABELS = { MORNING: 'Sáng', AFTERNOON: 'Chiều', EVENING: 'Tối' };
const STATE_LABELS = { SCHEDULED: 'Đã lên lịch', CHECK_IN_WINDOW: 'Có thể check-in thủ công', LATE: 'Chưa check-in', ACTIVE_MANUAL: 'Đang làm · thủ công', ACTIVE_AUTO: 'Đang làm · tự động trước đây', CHECK_OUT_WINDOW: 'Có thể check-out thủ công', COMPLETED_MANUAL: 'Hoàn tất · thủ công', COMPLETED_AUTO: 'Hoàn tất · tự động trước đây', MISSING_STAFF: 'Thiếu nhân viên', MISSING_NEXT_SHIFT: 'Thiếu ca kế tiếp', ROLLOVER_BLOCKED: 'Bị chặn bàn giao' };
const toast = useToast();
const tab = ref('schedule');
const users = ref([]);
const shifts = ref([]);
const selections = ref({});
const monitoring = ref([]);
const loading = ref(true);
const saving = ref(false);
const loadError = ref('');
const monitorLoading = ref(false);
const monitorError = ref('');
const weekStart = ref(mondayKey(new Date()));
const currentWeekStart = mondayKey(new Date());
let monitorTimer;
let monitorGeneration = 0;
let loadGeneration = 0;
let baseline = '[]';
const tabRefs = ref([]);

const staff = computed(() => users.value.filter((user) => user.roleName === 'STAFF' && (user.status || 'ACTIVE') === 'ACTIVE'));
const isCurrentWeek = computed(() => weekStart.value === currentWeekStart);
const days = computed(() => Array.from({ length: 7 }, (_, index) => { const date = fromKey(weekStart.value); date.setDate(date.getDate() + index); const key = dateKey(date); return { key, label: new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }).format(date) }; }));

function dateKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function fromKey(key) { const [year, month, day] = key.split('-').map(Number); return new Date(year, month - 1, day); }
function mondayKey(date) { const monday = new Date(date); const day = monday.getDay() || 7; monday.setDate(monday.getDate() - day + 1); return dateKey(monday); }
function slotKey(date, code) { return `${date}|${code}`; }
function time(value) { return value ? String(value).slice(0, 5) : '—'; }
function source(sourceValue) { return sourceValue === 'AUTO' ? 'Tự động trước đây' : sourceValue === 'MANUAL' ? 'Thủ công' : '—'; }
function normalizedSlots(data) { return (Array.isArray(data?.shifts) ? data.shifts : []).map(({ shiftDate, shiftCode, userId }) => ({ shiftDate, shiftCode, userId: Number(userId), role: 'STAFF' })).sort((a, b) => `${a.shiftDate}|${a.shiftCode}|${a.userId}`.localeCompare(`${b.shiftDate}|${b.shiftCode}|${b.userId}`)); }
function hydrate(data) { shifts.value = Array.isArray(data?.shifts) ? data.shifts : []; selections.value = Object.fromEntries(shifts.value.map((shift) => [slotKey(shift.shiftDate, shift.shiftCode), String(shift.userId)])); baseline = JSON.stringify(normalizedSlots(data)); }

async function loadWeek() {
  const generation = ++loadGeneration; const requestedWeek = weekStart.value;
  loading.value = true; loadError.value = '';
  try { const data = await adminApi.getShiftWeek(requestedWeek); if (generation !== loadGeneration || requestedWeek !== weekStart.value) return; hydrate(data); }
  catch (error) { if (generation === loadGeneration) loadError.value = error.message || 'Không thể tải lịch tuần'; }
  finally { if (generation === loadGeneration) loading.value = false; }
}
async function initialize() {
  try { const data = await adminApi.getUsers(); users.value = Array.isArray(data) ? data : []; }
  catch (error) { loadError.value = error.message || 'Không thể tải nhân viên'; }
  await loadWeek();
}
async function moveWeek(offset) { if (offset > 0 && isCurrentWeek.value) return; const date = fromKey(weekStart.value); date.setDate(date.getDate() + offset * 7); weekStart.value = dateKey(date); await loadWeek(); }
async function saveWeek() {
  if (saving.value) return;
  saving.value = true;
  const requestedWeek = weekStart.value;
  const slots = days.value.flatMap((day) => SHIFT_CODES.flatMap((code) => { const userId = selections.value[slotKey(day.key, code)]; return userId ? [{ shiftDate: day.key, shiftCode: code, userId: Number(userId), role: 'STAFF' }] : []; }));
  try {
    const latest = await adminApi.getShiftWeek(requestedWeek);
    if (requestedWeek !== weekStart.value || JSON.stringify(normalizedSlots(latest)) !== baseline) { toast.error('Lịch tuần đã được thay đổi. Vui lòng tải lại trước khi lưu.'); return; }
    hydrate(await adminApi.replaceShiftWeek({ weekStart: requestedWeek, slots })); toast.success('Đã lưu lịch tuần');
  } catch (error) { toast.error(error.message || 'Không thể lưu lịch tuần'); }
  finally { saving.value = false; }
}
async function loadMonitoring() {
  const generation = ++monitorGeneration; monitorLoading.value = true; monitorError.value = '';
  try { const data = await adminApi.getShiftMonitoring(); if (generation !== monitorGeneration) return; monitoring.value = Array.isArray(data) ? data : []; }
  catch (error) { if (generation !== monitorGeneration) return; monitorError.value = error.message || 'Không thể tải giám sát'; }
  finally { if (generation === monitorGeneration) monitorLoading.value = false; }
}
function selectTab(value) { tab.value = value; if (value === 'monitoring') loadMonitoring(); }
function handleTabKeydown(event, index) { const keys = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: 1 }; if (!(event.key in keys)) return; event.preventDefault(); const next = (keys[event.key] + 2) % 2; selectTab(['schedule', 'monitoring'][next]); tabRefs.value[next]?.focus(); }

onMounted(() => { initialize(); monitorTimer = setInterval(() => { if (tab.value === 'monitoring') loadMonitoring(); }, 30000); });
onUnmounted(() => { monitorGeneration++; clearInterval(monitorTimer); });
</script>

<template>
  <main class="shifts-page">
    <header class="page-header"><div><h1>Ca làm nhân viên</h1><p>Lập lịch tuần và giám sát vận hành.</p></div></header>
    <div class="tabs" role="tablist" aria-label="Quản lý ca làm"><button id="schedule-tab" :ref="el => tabRefs[0] = el" role="tab" :aria-selected="tab === 'schedule'" aria-controls="schedule-panel" :tabindex="tab === 'schedule' ? 0 : -1" @keydown="handleTabKeydown($event, 0)" @click="selectTab('schedule')">Lịch tuần</button><button id="monitor-tab" :ref="el => tabRefs[1] = el" role="tab" :aria-selected="tab === 'monitoring'" aria-controls="monitor-panel" :tabindex="tab === 'monitoring' ? 0 : -1" @keydown="handleTabKeydown($event, 1)" @click="selectTab('monitoring')">Giám sát</button></div>

    <section v-if="tab === 'schedule'" id="schedule-panel" class="panel" role="tabpanel" aria-labelledby="schedule-tab">
      <div class="week-toolbar"><button class="btn btn-outline" type="button" aria-label="Tuần trước" @click="moveWeek(-1)">‹ Tuần trước</button><strong>Tuần từ {{ weekStart }}</strong><button class="btn btn-outline" type="button" aria-label="Tuần sau" :disabled="isCurrentWeek" @click="moveWeek(1)">Tuần sau ›</button><button class="btn btn-primary" type="button" :disabled="loading || saving" @click="saveWeek">{{ saving ? 'Đang lưu...' : 'Lưu lịch' }}</button></div>
      <div v-if="loadError" class="state error" role="alert">{{ loadError }} <button class="btn btn-outline" @click="loadWeek">Thử lại</button></div>
      <div v-else-if="loading" class="state" aria-live="polite">Đang tải lịch tuần...</div>
      <div v-else class="calendar" aria-label="Lịch bảy ngày ba ca"><article v-for="day in days" :key="day.key" class="day"><h2>{{ day.label }}</h2><label v-for="code in SHIFT_CODES" :key="code">{{ CODE_LABELS[code] }}<select v-model="selections[slotKey(day.key, code)]" class="form-select" :aria-label="`${CODE_LABELS[code]} ${day.label}`"><option value="">Chưa phân công</option><option v-for="user in staff" :key="user.userId" :value="String(user.userId)">{{ user.fullName }}</option></select></label></article></div>
    </section>

    <section v-else-if="tab === 'monitoring'" id="monitor-panel" class="panel" role="tabpanel" aria-labelledby="monitor-tab">
      <div class="week-toolbar"><strong>Giám sát ca hiện tại và liền kề</strong><button class="btn btn-outline" type="button" :disabled="monitorLoading" @click="loadMonitoring">Làm mới</button></div>
      <div v-if="monitorError" class="state error" role="alert">{{ monitorError }} <button class="btn btn-outline" @click="loadMonitoring">Thử lại</button></div>
      <div v-else-if="monitorLoading && !monitoring.length" class="state" aria-live="polite">Đang tải giám sát...</div>
      <div v-else-if="!monitoring.length" class="state">Không có dữ liệu giám sát.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Ngày</th><th>Ca</th><th>Nhân viên</th><th>Giờ</th><th>Check-in</th><th>Check-out</th><th>Trạng thái</th><th>Mức cảnh báo</th></tr></thead><tbody><tr v-for="item in monitoring" :key="`${item.shiftDate}-${item.shiftCode}`" :class="`severity-${item.alertSeverity.toLowerCase()}`"><td>{{ item.shiftDate }}</td><td>{{ CODE_LABELS[item.shiftCode] }}</td><td>{{ item.staffName || 'Chưa phân công' }}</td><td>{{ time(item.startTime) }}–{{ time(item.endTime) }}</td><td>{{ item.checkInAt || '—' }}<small>{{ source(item.checkInSource) }}</small></td><td>{{ item.checkOutAt || '—' }}<small>{{ source(item.checkOutSource) }}</small></td><td>{{ STATE_LABELS[item.monitoringState] || item.monitoringState }}</td><td><strong :class="{ critical: item.alertSeverity === 'CRITICAL' }" :role="item.alertSeverity === 'CRITICAL' ? 'alert' : undefined">{{ item.alertSeverity }}</strong></td></tr></tbody></table></div>
    </section>


  </main>
</template>

<style scoped>
.page-header p{margin:5px 0 0;color:var(--text-mid)}.tabs{display:flex;gap:4px;margin-bottom:14px;border-bottom:1px solid var(--border)}.tabs button{min-height:44px;padding:0 20px;border:0;border-bottom:3px solid transparent;background:transparent;font-weight:700;cursor:pointer}.tabs button[aria-selected="true"]{border-color:var(--primary);color:var(--primary)}.panel{overflow:hidden;border:1px solid var(--border);border-radius:16px;background:var(--white)}.week-toolbar{display:flex;align-items:center;gap:10px;padding:16px}.week-toolbar .btn-primary{margin-left:auto}.calendar{display:grid;grid-template-columns:repeat(7,minmax(145px,1fr));border-top:1px solid var(--border)}.day{min-height:310px;padding:12px;border-right:1px solid var(--border)}.day:last-child{border:0}.day h2{margin:0 0 14px;font-size:14px;text-transform:capitalize}.day label{display:grid;gap:5px;margin-bottom:14px;color:var(--text-mid);font-size:12px;font-weight:700}.state{display:flex;min-height:220px;align-items:center;justify-content:center;gap:12px;color:var(--text-mid)}.error{color:#b91c1c}.table{min-width:1050px}.table td small{display:block;color:var(--text-mid)}.severity-warning{background:#fffbeb}.severity-critical{background:#fef2f2;color:#991b1b}.critical{color:#b91c1c}.tabs button:focus-visible,.btn:focus-visible,select:focus-visible{outline:3px solid #2563eb;outline-offset:2px}
</style>
