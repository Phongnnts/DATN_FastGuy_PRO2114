<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { shiftApi, staffApi } from '@/api';
import { parseShiftEndDatetime, toLocalDateKey } from '@/api/shift';

const shifts = ref([]);
const activeOwnershipCount = ref(0);
const loading = ref(true);
const savingShiftId = ref(null);
const error = ref('');
const attendance = ref([]);
const attendanceMonth = ref(toLocalDateKey(new Date()).slice(0, 7));
const attendanceLoading = ref(false);
const attendanceError = ref('');
const now = ref(new Date());
const todayKey = ref(toLocalDateKey(new Date()));
const weekStart = ref(mondayKey(new Date()));
const currentWeekStart = mondayKey(new Date());
const selectedDateKey = ref(todayKey.value);
let clockTimer;
let attendanceGeneration = 0;

const isCurrentWeek = computed(() => weekStart.value === currentWeekStart);
const weekEnd = computed(() => {
  const date = fromKey(weekStart.value);
  date.setDate(date.getDate() + 6);
  return toLocalDateKey(date);
});
const selectedDate = computed(() => fromKey(selectedDateKey.value));
const selectedShifts = computed(() => shifts.value
  .filter((shift) => shift.shiftDate === selectedDateKey.value)
  .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime))));
const calendarMonth = computed(() => new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(selectedDate.value));
const selectedWeekday = computed(() => new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(selectedDate.value));
const assignedDays = computed(() => new Set(shifts.value.map((shift) => shift.shiftDate)));
const calendarDays = computed(() => {
  const anchor = selectedDate.value;
  const first = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
  const mondayOffset = (first.getDay() + 6) % 7;
  const start = new Date(first);
  start.setDate(first.getDate() - mondayOffset);
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const key = toLocalDateKey(date);
    return {
      key,
      number: date.getDate(),
      currentMonth: date.getMonth() === anchor.getMonth(),
      inLoadedWeek: key >= weekStart.value && key <= weekEnd.value,
      hasShifts: assignedDays.value.has(key),
      today: key === todayKey.value,
    };
  });
});

function fromKey(key) { const [year, month, day] = key.split('-').map(Number); return new Date(year, month - 1, day); }
function mondayKey(date) { const monday = new Date(date); const day = monday.getDay() || 7; monday.setDate(monday.getDate() - day + 1); return toLocalDateKey(monday); }
function time(value) { return value ? String(value).slice(0, 5) : '—'; }
function isCheckedIn(shift) { return Boolean(shift.checkInAt) || shift.status === 'CHECKED_IN'; }
function isCheckedOut(shift) { return Boolean(shift.checkOutAt) || ['CHECKED_OUT', 'COMPLETED'].includes(shift.status); }
function isCurrent(shift) { const start = new Date(`${shift.shiftDate}T${time(shift.startTime)}:00`); const end = parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime); return now.value >= start && now.value < end; }
function canCheckOut(shift) { return now.value >= parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime); }
function countdown(shift) { const seconds = Math.max(0, Math.ceil((parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime) - now.value) / 1000)); const hours = Math.floor(seconds / 3600); const minutes = Math.floor((seconds % 3600) / 60); return `${hours ? `${hours} giờ ` : ''}${minutes} phút`; }
function sourceLabel(source) { return source === 'AUTO' ? 'Tự động trước đây' : source === 'MANUAL' ? 'Thủ công' : '—'; }
function statusLabel(status) { return { CHECKED_IN: 'Đang trong ca', CHECKED_OUT: 'Đã kết thúc', COMPLETED: 'Hoàn thành', SCHEDULED: 'Đã xếp lịch' }[status] || status; }
function statusClass(status) { return status === 'CHECKED_IN' ? 'active' : ['CHECKED_OUT', 'COMPLETED'].includes(status) ? 'done' : 'scheduled'; }
function shiftCodeLabel(code) { return { MORNING: 'Sáng', AFTERNOON: 'Chiều', EVENING: 'Tối' }[code] || code; }
function selectDay(day) { if (day.inLoadedWeek) selectedDateKey.value = day.key; }

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [data, ownership] = await Promise.all([shiftApi.getWeek(weekStart.value), staffApi.getOwnershipCount()]);
    shifts.value = Array.isArray(data?.shifts) ? data.shifts : [];
    activeOwnershipCount.value = Number(ownership?.activeOwnershipCount || 0);
    if (todayKey.value >= weekStart.value && todayKey.value <= weekEnd.value) selectedDateKey.value = todayKey.value;
    else selectedDateKey.value = shifts.value[0]?.shiftDate || weekStart.value;
  } catch (e) { error.value = e.message || 'Không thể tải lịch làm'; }
  finally { loading.value = false; }
}
async function moveWeek(offset) { if (offset > 0 && isCurrentWeek.value) return; const date = fromKey(weekStart.value); date.setDate(date.getDate() + offset * 7); weekStart.value = toLocalDateKey(date); await load(); }
async function loadAttendance() {
  const generation = ++attendanceGeneration; const requested = attendanceMonth.value; attendanceLoading.value = true; attendanceError.value = '';
  try { const data = await shiftApi.getAttendance(requested); if (generation !== attendanceGeneration || requested !== attendanceMonth.value) return; attendance.value = Array.isArray(data) ? data : []; }
  catch (e) { if (generation === attendanceGeneration && requested === attendanceMonth.value) attendanceError.value = e.message || 'Không thể tải chấm công'; }
  finally { if (generation === attendanceGeneration) attendanceLoading.value = false; }
}
function replaceShift(updated) { const index = shifts.value.findIndex((shift) => shift.shiftId === updated.shiftId); if (index >= 0) shifts.value[index] = updated; }
async function checkIn(shift) {
  if (savingShiftId.value !== null || isCheckedIn(shift)) return;
  savingShiftId.value = shift.shiftId;
  try { replaceShift(await shiftApi.checkIn(shift.shiftId)); window.dispatchEvent(new Event('staff-shift-changed')); }
  catch (e) { error.value = e.message; }
  finally { savingShiftId.value = null; }
}
async function checkOut(shift) {
  if (savingShiftId.value !== null || !isCheckedIn(shift) || isCheckedOut(shift) || !canCheckOut(shift) || activeOwnershipCount.value > 0) return;
  savingShiftId.value = shift.shiftId;
  try { replaceShift(await shiftApi.checkOut(shift.shiftId)); window.dispatchEvent(new Event('staff-shift-changed')); }
  catch (e) { if (e.status === 409) activeOwnershipCount.value = Number(e.data?.activeOwnershipCount || 0); error.value = e.status === 409 ? `Không thể check-out: còn ${activeOwnershipCount.value} đơn đang thuộc ca này.` : e.message; }
  finally { savingShiftId.value = null; }
}

onMounted(() => { load(); loadAttendance(); clockTimer = setInterval(() => { const previous = now.value; now.value = new Date(); todayKey.value = toLocalDateKey(now.value); const crossedShiftEnd = shifts.value.some(shift => previous < parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime) && now.value >= parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime)); if (crossedShiftEnd) load(); }, 30000); });
onUnmounted(() => { attendanceGeneration++; clearInterval(clockTimer); });
</script>

<template>
  <main class="staff-shifts">
    <header class="page-header">
      <div><span class="eyebrow">Vận hành cửa hàng</span><h1>Lịch làm của tôi</h1><p>Chọn ngày màu xanh để xem ca. Check-in và check-out đều do bạn thực hiện.</p></div>
      <nav aria-label="Điều hướng tuần"><button class="btn btn-outline" type="button" aria-label="Tuần trước" @click="moveWeek(-1)">‹ Tuần trước</button><button class="btn btn-outline" type="button" aria-label="Tuần sau" :disabled="isCurrentWeek" @click="moveWeek(1)">Tuần sau ›</button></nav>
    </header>
    <div v-if="error" class="page-error" role="alert">{{ error }}</div>
    <div v-if="loading" class="state" aria-live="polite">Đang tải lịch làm...</div>
    <section v-else class="calendar-shell" aria-label="Lịch tháng và chi tiết ca">
      <aside class="day-detail" aria-live="polite">
        <div class="selected-date"><strong>{{ selectedDate.getDate() }}</strong><span>{{ selectedWeekday }}</span><small>{{ selectedDateKey }}</small></div>
        <div class="detail-heading"><span>Lịch trong ngày</span><b>{{ selectedShifts.length }} ca</b></div>
        <div v-if="selectedShifts.length" class="shift-list">
          <article v-for="shift in selectedShifts" :key="shift.shiftId" class="shift-card" :class="{ current: isCurrent(shift) }">
            <div class="shift-card-head"><div><span>{{ shiftCodeLabel(shift.shiftCode) }}</span><strong>{{ time(shift.startTime) }}–{{ time(shift.endTime) }}</strong></div><span class="status-badge" :class="statusClass(shift.status)">{{ statusLabel(shift.status) }}</span></div>
            <dl><div><dt>Check-in</dt><dd>{{ shift.checkInAt || '—' }}</dd></div><div><dt>Check-out</dt><dd>{{ shift.checkOutAt || '—' }}</dd></div><div><dt>Nguồn</dt><dd>{{ sourceLabel(shift.checkInSource) }} / {{ sourceLabel(shift.checkOutSource) }}</dd></div></dl>
            <div class="actions" aria-live="polite"><button v-if="!isCheckedIn(shift)" class="btn detail-action" type="button" :disabled="savingShiftId !== null" :aria-label="`Check-in thủ công ca ${shiftCodeLabel(shift.shiftCode)} ngày ${shift.shiftDate}`" @click="checkIn(shift)">{{ savingShiftId === shift.shiftId ? 'Đang xử lý...' : 'Check-in thủ công' }}</button><button v-else-if="!isCheckedOut(shift)" class="btn detail-action secondary" type="button" :disabled="savingShiftId !== null || !canCheckOut(shift) || activeOwnershipCount > 0" :aria-label="`Check-out thủ công ca ${shiftCodeLabel(shift.shiftCode)} ngày ${shift.shiftDate}`" @click="checkOut(shift)">{{ savingShiftId === shift.shiftId ? 'Đang xử lý...' : 'Check-out thủ công' }}</button><span v-if="isCheckedIn(shift) && !isCheckedOut(shift) && !canCheckOut(shift)" class="countdown">Kết thúc theo lịch sau {{ countdown(shift) }}</span><span v-if="isCheckedIn(shift) && !isCheckedOut(shift) && activeOwnershipCount > 0" class="ownership" role="alert">Còn {{ activeOwnershipCount }} đơn đang phụ trách.</span></div>
          </article>
        </div>
        <div v-else class="day-empty"><i class="bi bi-calendar2-check" aria-hidden="true"></i><strong>Không có ca</strong><span>Bạn không được phân công trong ngày này.</span></div>
        <div class="week-summary">Tuần {{ weekStart }} – {{ weekEnd }} · {{ assignedDays.size }} ngày có lịch</div>
      </aside>

      <div class="calendar-panel">
        <div class="calendar-heading"><div><span class="eyebrow">Tuần đang xem</span><h2>{{ calendarMonth }}</h2></div><button v-if="!isCurrentWeek" class="today-button" type="button" @click="weekStart = currentWeekStart; load()">Về tuần này</button></div>
        <div class="weekday-row" aria-hidden="true"><span>T2</span><span>T3</span><span>T4</span><span>T5</span><span>T6</span><span>T7</span><span>CN</span></div>
        <div class="calendar-grid">
          <button v-for="day in calendarDays" :key="day.key" class="calendar-day" :class="{ muted: !day.currentMonth || !day.inLoadedWeek, assigned: day.hasShifts, selected: selectedDateKey === day.key, today: day.today }" type="button" :disabled="!day.inLoadedWeek" :aria-pressed="selectedDateKey === day.key" :aria-label="`${day.key}${day.hasShifts ? ', có ca được phân công' : ', không có ca'}`" @click="selectDay(day)"><span>{{ day.number }}</span><small v-if="day.hasShifts">Có ca</small></button>
        </div>
        <div class="legend"><span><i class="legend-dot assigned-dot"></i>Có ca được xếp</span><span><i class="legend-dot selected-dot"></i>Ngày đang chọn</span><span>Chỉ hiển thị dữ liệu của tuần đang xem</span></div>
      </div>
    </section>
    <section class="attendance-panel" aria-labelledby="attendance-title">
      <div class="attendance-heading"><div><span class="eyebrow">Chấm công</span><h2 id="attendance-title">Chấm công tháng</h2></div><label>Tháng <input v-model="attendanceMonth" type="month" @change="loadAttendance"></label></div>
      <div v-if="attendanceError" class="page-error" role="alert">{{ attendanceError }} <button type="button" class="btn btn-outline" @click="loadAttendance">Thử lại</button></div>
      <div v-else-if="attendanceLoading" class="state" aria-live="polite">Đang tải chấm công...</div>
      <div v-else-if="!attendance.length" class="state">Chưa có dữ liệu chấm công tháng này.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Ngày</th><th>Ca</th><th>Thực tế</th><th>Hợp lệ</th><th>Đi muộn</th><th>Về sớm</th><th>OT tiềm năng</th><th>Trạng thái</th></tr></thead><tbody><tr v-for="item in attendance" :key="item.shiftId"><td>{{ item.shiftDate }}</td><td>{{ shiftCodeLabel(item.shiftCode) }}</td><td>{{ item.actualMinutes }} phút</td><td>{{ item.overlapEligibleMinutes }} phút</td><td>{{ item.lateMinutes }} phút</td><td>{{ item.earlyLeaveMinutes }} phút</td><td>{{ item.potentialOvertimeMinutes }} phút</td><td>{{ item.attendanceStatus === 'APPROVED' ? 'Đã duyệt' : item.attendanceStatus === 'PENDING' ? 'Chờ duyệt' : '—' }}</td></tr></tbody></table></div>
    </section>
  </main>
</template>

<style scoped>
.staff-shifts{display:grid;gap:18px}.attendance-panel{overflow:hidden;border:1px solid var(--border);border-radius:16px;background:#fff}.attendance-heading{display:flex;align-items:end;justify-content:space-between;padding:16px}.attendance-heading h2{margin:3px 0 0}.attendance-heading label{display:grid;gap:4px;font-size:12px;font-weight:700}.attendance-heading input{min-height:40px;padding:6px;border:1px solid var(--border);border-radius:8px}.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:18px}.page-header h1{margin:3px 0;font-size:28px;letter-spacing:-.035em;text-wrap:balance}.page-header p{margin:0;color:var(--text-mid);font-size:13px}.page-header nav{display:flex;gap:8px}.eyebrow{color:var(--role-staff,#0f766e);font-size:10px;font-weight:800;letter-spacing:.13em;text-transform:uppercase}.state{padding:48px;text-align:center;color:var(--text-mid)}.page-error{padding:12px 14px;border-radius:10px;background:#fef2f2;color:#b91c1c}.calendar-shell{display:grid;grid-template-columns:minmax(300px,.72fr) minmax(460px,1.28fr);min-height:590px;overflow:hidden;border:1px solid #d9e2e7;border-radius:20px;background:#fff;box-shadow:0 18px 48px rgba(23,32,51,.1)}.day-detail{display:flex;flex-direction:column;padding:30px;background:linear-gradient(145deg,#0f766e,#15958a);color:#fff}.selected-date strong{display:block;font-size:78px;line-height:.9;letter-spacing:-.07em;font-variant-numeric:tabular-nums}.selected-date span{display:block;margin-top:8px;font-size:16px;font-weight:800;text-transform:capitalize}.selected-date small{display:block;margin-top:4px;color:rgba(255,255,255,.72)}.detail-heading{display:flex;align-items:center;justify-content:space-between;margin:28px 0 10px;padding-top:18px;border-top:1px solid rgba(255,255,255,.24);font-size:11px;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.detail-heading b{padding:3px 8px;border-radius:999px;background:rgba(255,255,255,.14)}.shift-list{display:grid;gap:10px}.shift-card{padding:14px;border:1px solid rgba(255,255,255,.22);border-radius:12px;background:rgba(255,255,255,.1)}.shift-card.current{background:rgba(255,255,255,.18);box-shadow:inset 3px 0 #fff}.shift-card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.shift-card-head div>span,.shift-card-head strong{display:block}.shift-card-head div>span{font-size:10px;color:rgba(255,255,255,.72);font-weight:700;text-transform:uppercase}.shift-card-head strong{margin-top:3px;font-size:17px;font-variant-numeric:tabular-nums}.status-badge{padding:4px 8px;border-radius:999px;background:#fef3c7;color:#92400e;font-size:10px;font-weight:800;white-space:nowrap}.status-badge.active{background:#dcfce7;color:#166534}.status-badge.done{background:rgba(255,255,255,.18);color:#fff}.shift-card dl{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:12px 0 0}.shift-card dl div{min-width:0}.shift-card dt{font-size:9px;color:rgba(255,255,255,.65)}.shift-card dd{margin:3px 0 0;overflow-wrap:anywhere;font-size:10px;font-weight:650}.actions{display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin-top:12px}.detail-action{min-height:40px;background:#fff;color:#0f766e}.detail-action.secondary{background:transparent;border-color:rgba(255,255,255,.55);color:#fff}.countdown,.ownership{font-size:10px}.ownership{color:#fef3c7}.day-empty{display:grid;justify-items:start;gap:5px;padding:18px;border:1px dashed rgba(255,255,255,.35);border-radius:12px;color:rgba(255,255,255,.76)}.day-empty i{font-size:22px}.day-empty strong{color:#fff}.day-empty span{font-size:11px}.week-summary{margin-top:auto;padding-top:20px;border-top:1px solid rgba(255,255,255,.24);font-size:10px;color:rgba(255,255,255,.75)}.calendar-panel{padding:30px}.calendar-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}.calendar-heading h2{margin:4px 0 0;font-size:23px;text-transform:capitalize}.today-button{min-height:40px;padding:8px 12px;border:1px solid var(--border);border-radius:9px;background:#fff;color:#0f766e;font-weight:700}.weekday-row,.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:7px}.weekday-row{margin-bottom:6px}.weekday-row span{text-align:center;color:var(--text-light);font-size:10px;font-weight:800}.calendar-day{position:relative;min-width:0;min-height:62px;display:grid;place-items:center;align-content:center;gap:2px;border:1px solid transparent;border-radius:12px;background:transparent;color:var(--text-mid);cursor:pointer;transition:transform .15s ease-out,background-color .15s ease-out,border-color .15s ease-out,box-shadow .15s ease-out}.calendar-day span{font-size:13px;font-weight:650;font-variant-numeric:tabular-nums}.calendar-day small{font-size:8px;font-weight:800}.calendar-day:hover:not(:disabled){transform:translateY(-2px);background:#f0fdfa}.calendar-day:focus-visible{outline:3px solid #5eead4;outline-offset:2px}.calendar-day.muted{color:#c8cdd5}.calendar-day:disabled{cursor:default}.calendar-day.assigned{border-color:#86efac;background:#f0fdf4;color:#166534}.calendar-day.assigned:after{content:"";position:absolute;bottom:6px;width:5px;height:5px;border-radius:50%;background:#22c55e}.calendar-day.selected{border-color:#0f766e;background:#0f766e;color:#fff;box-shadow:0 7px 16px rgba(15,118,110,.25)}.calendar-day.selected:after{background:#fff}.calendar-day.today:not(.selected){box-shadow:inset 0 0 0 2px #f97316}.legend{display:flex;flex-wrap:wrap;gap:14px;margin-top:22px;color:var(--text-light);font-size:10px}.legend-dot{display:inline-block;width:8px;height:8px;margin-right:5px;border-radius:50%}.assigned-dot{background:#22c55e}.selected-dot{background:#0f766e}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
@media(max-width:900px){.page-header{align-items:flex-start;flex-direction:column}.calendar-shell{grid-template-columns:1fr}.calendar-panel{order:1}.day-detail{order:2;min-height:440px}.calendar-day{min-height:54px}.shift-card dl{grid-template-columns:1fr 1fr}}
@media(max-width:560px){.calendar-panel,.day-detail{padding:20px}.weekday-row,.calendar-grid{gap:3px}.calendar-day{min-height:46px;border-radius:9px}.calendar-day small{font-size:0}.calendar-day small:after{content:"•";font-size:12px}.page-header nav{width:100%}.page-header nav .btn{flex:1}.selected-date strong{font-size:62px}}
@media(prefers-reduced-motion:reduce){.calendar-day{transition:none}}
</style>
