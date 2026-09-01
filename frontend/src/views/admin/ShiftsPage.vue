<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const SHIFT_CODES = ['MORNING', 'AFTERNOON', 'EVENING'];
const TAB_KEYS = ['schedule', 'monitoring'];
const VIEW_KEYS = ['week', 'month'];
const CODE_LABELS = { MORNING: 'Sáng', AFTERNOON: 'Chiều', EVENING: 'Tối' };
const STATE_LABELS = { SCHEDULED: 'Đã lên lịch', CHECK_IN_WINDOW: 'Có thể check-in thủ công', LATE: 'Chưa check-in', ACTIVE_MANUAL: 'Đang làm · thủ công', ACTIVE_AUTO: 'Đang làm · tự động trước đây', CHECK_OUT_WINDOW: 'Có thể check-out thủ công', COMPLETED_MANUAL: 'Hoàn tất · thủ công', COMPLETED_AUTO: 'Hoàn tất · tự động trước đây', MISSING_STAFF: 'Thiếu nhân viên', MISSING_NEXT_SHIFT: 'Thiếu ca kế tiếp', ROLLOVER_BLOCKED: 'Bị chặn bàn giao' };
const toast = useToast();
const route = useRoute();
const router = useRouter();
const tabFromQuery = raw => TAB_KEYS.includes(raw) ? raw : 'schedule';
const viewFromQuery = raw => VIEW_KEYS.includes(raw) ? raw : 'week';
const validDateQuery = raw => typeof raw === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(raw) && raw <= dateKey(new Date()) ? raw : dateKey(new Date());
const tab = ref(tabFromQuery(route.query.tab));
const view = ref(viewFromQuery(route.query.view));
const selectedDate = ref(validDateQuery(route.query.date));
const monthAnchor = ref(selectedDate.value.slice(0, 7));
const users = ref([]);
const shifts = ref([]);
const selections = ref({});
const monitoring = ref([]);
const monthShifts = ref([]);
const monthLoading = ref(false);
const monthError = ref('');
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
let monthGeneration = 0;
let baseline = '[]';
const tabRefs = ref([]);

const staff = computed(() => users.value.filter((user) => user.roleName === 'STAFF' && (user.status || 'ACTIVE') === 'ACTIVE'));
const isCurrentWeek = computed(() => weekStart.value === currentWeekStart);
const todayKey = dateKey(new Date());
const days = computed(() => Array.from({ length: 7 }, (_, index) => { const date = fromKey(weekStart.value); date.setDate(date.getDate() + index); const key = dateKey(date); return { key, label: new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }).format(date), today: key === todayKey }; }));
const monthDays = computed(() => { const [year, month] = monthAnchor.value.split('-').map(Number); const first = new Date(year, month - 1, 1); const start = new Date(first); start.setDate(first.getDate() - ((first.getDay() + 6) % 7)); return Array.from({ length: 42 }, (_, index) => { const date = new Date(start); date.setDate(start.getDate() + index); const key = dateKey(date); return { key, number: date.getDate(), currentMonth: date.getMonth() === month - 1, today: key === todayKey, disabled: key > todayKey }; }); });
const selectedDayShifts = computed(() => SHIFT_CODES.map(code => monthShifts.value.find(shift => shift.shiftDate === selectedDate.value && shift.shiftCode === code) || { shiftDate: selectedDate.value, shiftCode: code }));

function dateKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function fromKey(key) { const [year, month, day] = key.split('-').map(Number); return new Date(year, month - 1, day); }
function mondayKey(date) { const monday = new Date(date); const day = monday.getDay() || 7; monday.setDate(monday.getDate() - day + 1); return dateKey(monday); }
function slotKey(date, code) { return `${date}|${code}`; }
function monthWeekStarts() { const starts = [...new Set(monthDays.value.map(day => mondayKey(fromKey(day.key))))]; return starts.filter(key => key <= currentWeekStart).slice(0, 6); }
function shiftsForDay(key) { return monthShifts.value.filter(shift => shift.shiftDate === key).sort((a, b) => SHIFT_CODES.indexOf(a.shiftCode) - SHIFT_CODES.indexOf(b.shiftCode)); }
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
async function loadMonth() {
  const generation = ++monthGeneration;
  monthLoading.value = true;
  monthError.value = '';
  try {
    const results = await Promise.all(monthWeekStarts().map(key => adminApi.getShiftWeek(key)));
    if (generation !== monthGeneration) return;
    const byId = new Map();
    results.flatMap(data => Array.isArray(data?.shifts) ? data.shifts : []).forEach(shift => byId.set(shift.shiftId || slotKey(shift.shiftDate, shift.shiftCode), shift));
    monthShifts.value = [...byId.values()];
  } catch (error) { if (generation === monthGeneration) monthError.value = error.message || 'Không thể tải lịch tháng'; }
  finally { if (generation === monthGeneration) monthLoading.value = false; }
}
function selectView(value) {
  if (!VIEW_KEYS.includes(value)) return;
  view.value = value;
  router.replace({ query: { ...route.query, tab: 'schedule', view: value, date: selectedDate.value } });
  if (value === 'month') loadMonth();
}
function selectDate(day) {
  if (day.disabled || day.key > todayKey) return;
  selectedDate.value = day.key;
  router.replace({ query: { ...route.query, tab: 'schedule', view: view.value, date: day.key } });
}
async function moveMonth(offset) {
  const [year, month] = monthAnchor.value.split('-').map(Number);
  const next = new Date(year, month - 1 + offset, 1);
  const current = new Date();
  if (next > new Date(current.getFullYear(), current.getMonth(), 1)) return;
  monthAnchor.value = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`;
  selectedDate.value = dateKey(next);
  await loadMonth();
}
async function loadMonitoring() {
  const generation = ++monitorGeneration; monitorLoading.value = true; monitorError.value = '';
  try { const data = await adminApi.getShiftMonitoring(); if (generation !== monitorGeneration) return; monitoring.value = Array.isArray(data) ? data : []; }
  catch (error) { if (generation !== monitorGeneration) return; monitorError.value = error.message || 'Không thể tải giám sát'; }
  finally { if (generation === monitorGeneration) monitorLoading.value = false; }
}
function selectTab(value) {
  if (!TAB_KEYS.includes(value)) return;
  if (tab.value !== value) tab.value = value;
  if (route.query.tab !== value) router.replace({ query: { ...route.query, tab: value } });
}
function handleTabKeydown(event, index) { const keys = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: 1 }; if (!(event.key in keys)) return; event.preventDefault(); const next = (keys[event.key] + 2) % 2; selectTab(['schedule', 'monitoring'][next]); tabRefs.value[next]?.focus(); }

watch(() => route.query.tab, (raw) => {
  const next = tabFromQuery(raw);
  if (tab.value !== next) tab.value = next;
});
watch(() => route.query.view, (raw) => { const next = viewFromQuery(raw); if (view.value !== next) { view.value = next; if (next === 'month') loadMonth(); } });
watch(() => route.query.date, (raw) => { const next = validDateQuery(raw); if (selectedDate.value !== next) { selectedDate.value = next; monthAnchor.value = next.slice(0, 7); } });
watch(tab, (value, previous) => { if (value === 'monitoring' && previous !== 'monitoring') loadMonitoring(); });
onMounted(() => { initialize(); if (tab.value === 'monitoring') loadMonitoring(); if (tab.value === 'schedule' && view.value === 'month') loadMonth(); monitorTimer = setInterval(() => { if (tab.value === 'monitoring') loadMonitoring(); }, 30000); });
onUnmounted(() => { monitorGeneration++; monthGeneration++; loadGeneration++; clearInterval(monitorTimer); });
</script>

<template>
  <main class="shifts-page">
    <header class="page-header"><div><h1>Ca làm nhân viên</h1><p>Lập lịch tuần và giám sát vận hành.</p></div></header>
    <div class="shift-mode-tabs tabs" role="tablist" aria-label="Quản lý ca làm"><button id="schedule-tab" :ref="el => tabRefs[0] = el" role="tab" :aria-selected="tab === 'schedule'" aria-controls="schedule-panel" :tabindex="tab === 'schedule' ? 0 : -1" @keydown="handleTabKeydown($event, 0)" @click="selectTab('schedule')">Lịch tuần</button><button id="monitor-tab" :ref="el => tabRefs[1] = el" role="tab" :aria-selected="tab === 'monitoring'" aria-controls="monitor-panel" :tabindex="tab === 'monitoring' ? 0 : -1" @keydown="handleTabKeydown($event, 1)" @click="selectTab('monitoring')">Giám sát</button></div>

    <section v-if="tab === 'schedule'" id="schedule-panel" class="panel" role="tabpanel" aria-labelledby="schedule-tab">
      <div class="schedule-toolbar week-toolbar"><template v-if="view === 'week'"><button class="btn btn-outline" type="button" aria-label="Tuần trước" @click="moveWeek(-1)">‹</button><strong>Tuần từ {{ weekStart }}</strong><button class="btn btn-outline" type="button" aria-label="Tuần sau" :disabled="isCurrentWeek" @click="moveWeek(1)">›</button></template><template v-else><button class="btn btn-outline" type="button" aria-label="Tháng trước" @click="moveMonth(-1)">‹</button><strong>Tháng {{ monthAnchor }}</strong><button class="btn btn-outline" type="button" aria-label="Tháng sau" :disabled="monthAnchor === todayKey.slice(0, 7)" @click="moveMonth(1)">›</button></template><div class="view-switch" role="group" aria-label="Chế độ lịch"><button type="button" :aria-pressed="view === 'week'" @click="selectView('week')">Tuần</button><button type="button" :aria-pressed="view === 'month'" @click="selectView('month')">Tháng</button></div><button v-if="view === 'week'" class="btn btn-primary" type="button" :disabled="loading || saving" @click="saveWeek">{{ saving ? 'Đang lưu...' : 'Lưu lịch' }}</button></div>
      <template v-if="view === 'week'"><div v-if="loadError" class="state error" role="alert">{{ loadError }} <button class="btn btn-outline" @click="loadWeek">Thử lại</button></div><div v-else-if="loading" class="state" aria-live="polite">Đang tải lịch tuần...</div><div v-else class="week-calendar-shell week-calendar" aria-label="Lịch bảy ngày ba ca"><div class="calendar-corner"></div><div v-for="day in days" :key="`head-${day.key}`" class="calendar-head" :class="{ today: day.today }">{{ day.label }}</div><template v-for="code in SHIFT_CODES" :key="code"><div class="shift-label"><strong>{{ CODE_LABELS[code] }}</strong></div><label v-for="day in days" :key="slotKey(day.key, code)" class="assignment-cell week-slot"><span class="sr-only">{{ CODE_LABELS[code] }} {{ day.label }}</span><select v-model="selections[slotKey(day.key, code)]" class="form-select" :aria-label="`${CODE_LABELS[code]} ${day.label}`"><option value="">Chưa phân công</option><option v-for="user in staff" :key="user.userId" :value="String(user.userId)">{{ user.fullName }}</option></select></label></template></div></template>
      <template v-else><div v-if="monthError" class="state error" role="alert">{{ monthError }} <button class="btn btn-outline" @click="loadMonth">Thử lại</button></div><div v-else-if="monthLoading" class="state" aria-live="polite">Đang tải lịch tháng...</div><div v-else class="month-layout"><div class="month-grid" aria-label="Lịch tháng"><div v-for="label in ['T2','T3','T4','T5','T6','T7','CN']" :key="label" class="month-weekday">{{ label }}</div><button v-for="day in monthDays" :key="day.key" type="button" class="month-day" :class="{ outside: !day.currentMonth, today: day.today, selected: selectedDate === day.key }" :disabled="day.disabled" :aria-pressed="selectedDate === day.key" @click="selectDate(day)"><strong>{{ day.number }}</strong><span v-for="shift in shiftsForDay(day.key)" :key="shift.shiftCode" class="month-shift">{{ CODE_LABELS[shift.shiftCode] }} · {{ shift.staffName || 'Chưa phân công' }}</span></button></div><aside class="month-inspector day-inspector" aria-labelledby="selected-day-title"><p>Ngày đã chọn</p><h2 id="selected-day-title">{{ selectedDate }}</h2><article v-for="shift in selectedDayShifts" :key="shift.shiftCode"><strong>{{ CODE_LABELS[shift.shiftCode] }}</strong><span>{{ shift.staffName || 'Chưa phân công' }}</span><small>{{ time(shift.startTime) }}–{{ time(shift.endTime) }}</small></article></aside></div></template>
    </section>

    <section v-else-if="tab === 'monitoring'" id="monitor-panel" class="monitoring-workspace panel" role="tabpanel" aria-labelledby="monitor-tab">
      <div class="week-toolbar"><strong>Giám sát ca hiện tại và liền kề</strong><button class="btn btn-outline" type="button" :disabled="monitorLoading" @click="loadMonitoring">Làm mới</button></div>
      <div v-if="monitorError" class="state error" role="alert">{{ monitorError }} <button class="btn btn-outline" @click="loadMonitoring">Thử lại</button></div>
      <div v-else-if="monitorLoading && !monitoring.length" class="state" aria-live="polite">Đang tải giám sát...</div>
      <div v-else-if="!monitoring.length" class="state">Không có dữ liệu giám sát.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Ngày</th><th>Ca</th><th>Nhân viên</th><th>Giờ</th><th>Check-in</th><th>Check-out</th><th>Trạng thái</th><th>Mức cảnh báo</th></tr></thead><tbody><tr v-for="item in monitoring" :key="`${item.shiftDate}-${item.shiftCode}`" :class="`severity-${item.alertSeverity.toLowerCase()}`"><td>{{ item.shiftDate }}</td><td>{{ CODE_LABELS[item.shiftCode] }}</td><td>{{ item.staffName || 'Chưa phân công' }}</td><td>{{ time(item.startTime) }}–{{ time(item.endTime) }}</td><td>{{ item.checkInAt || '—' }}<small>{{ source(item.checkInSource) }}</small></td><td>{{ item.checkOutAt || '—' }}<small>{{ source(item.checkOutSource) }}</small></td><td>{{ STATE_LABELS[item.monitoringState] || item.monitoringState }}</td><td><strong :class="{ critical: item.alertSeverity === 'CRITICAL' }" :role="item.alertSeverity === 'CRITICAL' ? 'alert' : undefined">{{ item.alertSeverity }}</strong></td></tr></tbody></table></div>
    </section>


  </main>
</template>

<style scoped>
.shifts-page{display:grid;gap:14px;color:var(--admin-foreground)}.page-header h1{margin:0;font-size:28px;letter-spacing:-.04em}.page-header p{margin:4px 0 0;color:var(--admin-muted)}.tabs{display:flex;gap:4px;border-bottom:1px solid var(--admin-hairline)}.tabs button{min-height:44px;padding:0 14px;border:0;border-bottom:2px solid transparent;background:transparent;color:var(--admin-muted);font-weight:700;cursor:pointer}.tabs button[aria-selected="true"]{border-color:var(--admin-brand);color:var(--admin-foreground)}.panel{overflow:hidden;border:1px solid var(--admin-hairline);border-radius:14px;background:var(--admin-surface);box-shadow:var(--admin-card-shadow)}.week-toolbar{display:flex;align-items:center;gap:9px;padding:14px 16px;border-bottom:1px solid var(--admin-hairline)}.week-toolbar .btn-primary{margin-left:auto}.view-switch{display:flex;margin-left:auto;padding:3px;border-radius:10px;background:var(--admin-surface-subtle)}.view-switch button{min-height:34px;padding:0 14px;border:0;border-radius:8px;background:transparent;color:var(--admin-muted);font-weight:700}.view-switch button[aria-pressed="true"]{background:#fff;color:var(--admin-foreground);box-shadow:0 2px 7px rgba(20,20,35,.08)}.week-calendar{display:grid;grid-template-columns:100px repeat(7,minmax(130px,1fr));min-width:1010px}.week-calendar>*{border-right:1px solid var(--admin-hairline);border-bottom:1px solid var(--admin-hairline)}.calendar-corner,.calendar-head{min-height:58px;padding:10px;background:var(--admin-surface-subtle)}.calendar-head{text-align:center;color:var(--admin-muted);font-size:11px;font-weight:750}.calendar-head.today{color:var(--admin-brand-dark);background:var(--admin-brand-soft)}.shift-label{padding:12px;background:var(--admin-surface-subtle);font-size:12px}.week-slot{min-height:82px;padding:9px}.week-slot select{height:100%;min-height:52px;border-radius:6px;border-left:2px solid var(--admin-brand);background:var(--admin-surface-subtle);font-size:11px}.month-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px}.month-grid{display:grid;grid-template-columns:repeat(7,1fr)}.month-weekday{padding:10px;border-right:1px solid var(--admin-hairline);border-bottom:1px solid var(--admin-hairline);color:var(--admin-muted);text-align:center;font-size:10px;font-weight:750}.month-day{display:flex;min-height:94px;flex-direction:column;align-items:stretch;gap:4px;padding:8px;border:0;border-right:1px solid var(--admin-hairline);border-bottom:1px solid var(--admin-hairline);border-radius:0;background:#fff;text-align:left}.month-day.outside{color:var(--admin-subtle);background:#fcfcfd}.month-day.today{background:#fffaf7}.month-day.selected{box-shadow:inset 0 0 0 2px var(--admin-brand)}.month-day:disabled{opacity:.45;cursor:not-allowed}.month-shift{overflow:hidden;padding:3px 5px;border-left:2px solid var(--admin-brand);border-radius:4px;background:var(--admin-surface-subtle);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.day-inspector{padding:18px;border-left:1px solid var(--admin-hairline)}.day-inspector>p{color:var(--admin-muted);font-size:10px;text-transform:uppercase}.day-inspector h2{margin:3px 0 14px;font-size:18px}.day-inspector article{display:grid;gap:3px;padding:12px 0;border-top:1px solid var(--admin-hairline)}.day-inspector span,.day-inspector small{color:var(--admin-muted);font-size:11px}.state{display:flex;min-height:220px;align-items:center;justify-content:center;gap:12px;color:var(--admin-muted)}.error{color:var(--admin-danger)}.table{min-width:1050px}.table td small{display:block;color:var(--admin-muted)}.severity-warning{background:var(--admin-warning-soft)}.severity-critical{background:var(--admin-danger-soft);color:var(--admin-danger)}.critical{color:var(--admin-danger)}.tabs button:focus-visible,.view-switch button:focus-visible,.month-day:focus-visible,.btn:focus-visible,select:focus-visible{outline:3px solid rgba(255,116,72,.35);outline-offset:2px}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}@media(max-width:1000px){.panel{overflow-x:auto}.month-layout{grid-template-columns:1fr}.day-inspector{border-top:1px solid var(--admin-hairline);border-left:0}}@media(max-width:640px){.week-toolbar{align-items:stretch;flex-wrap:wrap}.view-switch{order:-1;width:100%;margin:0}.view-switch button{flex:1}.week-toolbar .btn-primary{width:100%;margin:0}.month-day{min-height:76px}.month-shift{font-size:8px}}@media(prefers-reduced-motion:reduce){.tabs button,.month-day{transition:none}}
</style>

<style scoped>
.shifts-page{gap:18px}.page-header{padding:14px 4px 8px}.page-header h1{font-size:clamp(30px,3vw,40px);line-height:1.08;letter-spacing:-.055em}.shift-mode-tabs{gap:12px}.shift-mode-tabs button{padding-inline:3px 16px}.panel{border-radius:var(--admin-panel-radius);box-shadow:var(--admin-card-shadow)}.schedule-toolbar{min-height:72px;padding:14px 18px}.schedule-toolbar>strong{font-size:17px;letter-spacing:-.025em}.schedule-toolbar>.btn:first-child,.schedule-toolbar>template+.btn{width:42px;min-width:42px;padding:0}.schedule-toolbar .btn-primary{min-height:44px;padding-inline:18px;background:var(--admin-action);box-shadow:0 8px 18px rgba(196,63,22,.18)}.view-switch{border:1px solid var(--admin-hairline)}.week-calendar-shell{overflow:visible;background:var(--admin-surface)}.calendar-head{min-height:66px;display:grid;place-items:center}.calendar-head.today{position:relative;color:var(--admin-brand-dark);background:#fff8f4}.calendar-head.today::after{position:absolute;bottom:0;left:22%;right:22%;height:3px;border-radius:3px;background:var(--admin-brand);content:""}.shift-label{display:flex;align-items:flex-start;padding:16px 13px}.assignment-cell{min-height:96px;padding:9px}.assignment-cell select{min-height:66px;padding:9px;border:1px solid var(--admin-hairline);border-left:3px solid var(--admin-brand);border-radius:10px;background:#fff;box-shadow:0 3px 10px rgba(24,34,48,.035)}.month-layout{gap:14px;padding:14px;background:var(--admin-surface-subtle)}.month-grid,.month-inspector{border:1px solid var(--admin-hairline);border-radius:14px;background:#fff;overflow:hidden}.month-day{min-height:100px}.month-day.selected{box-shadow:inset 0 0 0 2px var(--admin-brand);background:#fff9f5}.month-day.today strong{display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:var(--admin-brand);color:#fff}.month-shift{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.month-inspector{padding:20px}.month-inspector>p{color:var(--admin-brand);font-size:9px;font-weight:850;letter-spacing:.12em;text-transform:uppercase}.month-inspector h2{margin:4px 0 14px;font-size:21px}.month-inspector article{padding:14px 0}.monitoring-workspace .week-toolbar{min-height:68px;padding:14px 18px}.monitoring-workspace .table-wrapper{border-top:1px solid var(--admin-hairline)}.monitoring-workspace table th{padding:13px 16px;background:var(--admin-surface-subtle);color:var(--admin-muted);font-size:10px;letter-spacing:.06em;text-transform:uppercase}.monitoring-workspace table td{position:relative;padding:15px 16px}.monitoring-workspace tbody tr{background:#fff}.monitoring-workspace tbody tr.severity-critical{background:#fff8f8}.monitoring-workspace tbody tr.severity-warning{background:#fffcf5}.monitoring-workspace tbody tr::before{position:absolute;left:0;width:3px;height:34px;margin-top:7px;border-radius:0 3px 3px 0;background:transparent;content:""}.monitoring-workspace tbody tr.severity-critical::before{background:var(--admin-danger)}.monitoring-workspace tbody tr.severity-warning::before{background:var(--admin-warning)}.monitoring-workspace td>strong{display:inline-flex;padding:5px 8px;border-radius:999px;background:var(--admin-warning-soft);color:var(--admin-warning);font-size:10px}.monitoring-workspace td>strong.critical{background:var(--admin-danger-soft);color:var(--admin-danger)}@media(max-width:900px){.schedule-toolbar{align-items:flex-start;flex-wrap:wrap}.schedule-toolbar .view-switch{order:3;margin-left:0}.schedule-toolbar .btn-primary{order:4;margin-left:auto}.month-layout{grid-template-columns:1fr}.month-inspector{display:block}.week-calendar-shell{overflow-x:auto}.monitoring-workspace .table-wrapper{overflow-x:auto}}@media(max-width:600px){.schedule-toolbar>strong{width:calc(100% - 102px)}.schedule-toolbar .view-switch{width:100%}.view-switch button{flex:1}.schedule-toolbar .btn-primary{width:100%;justify-content:center}.month-layout{padding:9px}.month-day{min-height:72px;padding:6px}.month-shift{font-size:8px}.monitoring-workspace .week-toolbar{align-items:flex-start;flex-direction:column}.monitoring-workspace .week-toolbar .btn{width:100%}}
</style>
