<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { shiftApi, staffApi } from '@/api';
import { parseShiftEndDatetime, toLocalDateKey } from '@/api/shift';

const SHIFT_CODES = ['MORNING', 'AFTERNOON', 'EVENING'];
const shifts = ref([]);
const activeOwnershipCount = ref(0);
const loading = ref(true);
const savingShiftId = ref(null);
const error = ref('');
const now = ref(new Date());
const todayKey = ref(toLocalDateKey(new Date()));
const weekStart = ref(mondayKey(new Date()));
const currentWeekStart = mondayKey(new Date());
let clockTimer;

const isCurrentWeek = computed(() => weekStart.value === currentWeekStart);
const days = computed(() => Array.from({ length: 7 }, (_, index) => {
  const date = fromKey(weekStart.value);
  date.setDate(date.getDate() + index);
  const key = toLocalDateKey(date);
  return { key, label: new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' }).format(date), shifts: shifts.value.filter((shift) => shift.shiftDate === key) };
}));

function fromKey(key) { const [year, month, day] = key.split('-').map(Number); return new Date(year, month - 1, day); }
function mondayKey(date) { const monday = new Date(date); const day = monday.getDay() || 7; monday.setDate(monday.getDate() - day + 1); return toLocalDateKey(monday); }
function time(value) { return value ? String(value).slice(0, 5) : '—'; }
function isToday(day) { return day.key === todayKey.value; }
function isCheckedIn(shift) { return Boolean(shift.checkInAt) || shift.status === 'CHECKED_IN'; }
function isCheckedOut(shift) { return Boolean(shift.checkOutAt) || ['CHECKED_OUT', 'COMPLETED'].includes(shift.status); }
function isCurrent(shift) { const start = new Date(`${shift.shiftDate}T${time(shift.startTime)}:00`); const end = parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime); return now.value >= start && now.value < end; }
function canCheckOut(shift) { return now.value >= parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime); }
function countdown(shift) { const seconds = Math.max(0, Math.ceil((parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime) - now.value) / 1000)); const hours = Math.floor(seconds / 3600); const minutes = Math.floor((seconds % 3600) / 60); return `${hours ? `${hours} giờ ` : ''}${minutes} phút`; }
function sourceLabel(source) { return source === 'AUTO' ? 'Tự động' : source === 'MANUAL' ? 'Thủ công' : '—'; }
function statusLabel(status) { return { CHECKED_IN: 'Đang trong ca', CHECKED_OUT: 'Đã kết thúc', COMPLETED: 'Hoàn thành', SCHEDULED: 'Đã xếp lịch' }[status] || status; }
function statusClass(status) { return status === 'CHECKED_IN' ? 'active' : ['CHECKED_OUT', 'COMPLETED'].includes(status) ? 'done' : 'scheduled'; }
function shiftCodeLabel(code) { return { MORNING: 'Sáng', AFTERNOON: 'Chiều', EVENING: 'Tối' }[code] || code; }

async function load() {
  loading.value = true; error.value = '';
  try {
    const [data, ownership] = await Promise.all([shiftApi.getWeek(weekStart.value), staffApi.getOwnershipCount()]);
    shifts.value = Array.isArray(data?.shifts) ? data.shifts : [];
    activeOwnershipCount.value = Number(ownership?.activeOwnershipCount || 0);
  } catch (e) { error.value = e.message || 'Không thể tải lịch làm'; }
  finally { loading.value = false; }
}
async function moveWeek(offset) { if (offset > 0 && isCurrentWeek.value) return; const date = fromKey(weekStart.value); date.setDate(date.getDate() + offset * 7); weekStart.value = toLocalDateKey(date); await load(); }
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

onMounted(() => { load(); clockTimer = setInterval(() => { const previous = now.value; now.value = new Date(); todayKey.value = toLocalDateKey(now.value); const crossedShiftEnd = shifts.value.some(shift => previous < parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime) && now.value >= parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime)); if (crossedShiftEnd) load(); }, 30000); });
onUnmounted(() => clearInterval(clockTimer));
</script>

<template>
  <main class="staff-shifts">
    <header class="page-header"><div><h1><i class="bi bi-calendar-week" aria-hidden="true"></i> Lịch làm tuần</h1><p>{{ weekStart }}</p></div><nav aria-label="Điều hướng tuần"><button class="btn btn-outline" type="button" aria-label="Tuần trước" @click="moveWeek(-1)">‹ Tuần trước</button><button class="btn btn-outline" type="button" aria-label="Tuần sau" :disabled="isCurrentWeek" @click="moveWeek(1)">Tuần sau ›</button></nav></header>
    <div v-if="error" class="page-error" role="alert">{{ error }}</div>
    <div v-if="loading" class="state" aria-live="polite">Đang tải lịch làm...</div>
    <section v-else class="card" aria-label="Lịch làm bảy ngày">
      <div v-if="!shifts.length" class="empty">Không có ca được phân công trong tuần này.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Ngày</th><th>Ca</th><th>Giờ</th><th>Trạng thái</th><th>Chấm công</th><th>Nguồn</th><th>Thao tác</th></tr></thead><tbody>
        <template v-for="day in days" :key="day.key"><tr v-for="shift in day.shifts" :key="shift.shiftId" :class="{ today: isToday(day), current: isCurrent(shift) }"><th scope="row">{{ day.label }}<span v-if="isToday(day)" class="today-label">Hôm nay</span></th><td>{{ shiftCodeLabel(shift.shiftCode) }}</td><td>{{ time(shift.startTime) }}–{{ time(shift.endTime) }}</td><td><span class="badge" :class="statusClass(shift.status)">{{ statusLabel(shift.status) }}</span></td><td>Vào: {{ shift.checkInAt || '—' }}<br>Ra: {{ shift.checkOutAt || '—' }}</td><td>Vào: {{ sourceLabel(shift.checkInSource) }}<br>Ra: {{ sourceLabel(shift.checkOutSource) }}<span class="sr-only">MANUAL AUTO</span></td><td><div class="actions" aria-live="polite"><button v-if="!isCheckedIn(shift)" class="btn btn-primary" type="button" :disabled="savingShiftId !== null" :aria-label="`Check-in ca ${shiftCodeLabel(shift.shiftCode)} ngày ${day.key}`" @click="checkIn(shift)">{{ savingShiftId === shift.shiftId ? 'Đang xử lý...' : 'Check-in' }}</button><button v-else-if="!isCheckedOut(shift)" class="btn btn-outline" type="button" :disabled="savingShiftId !== null || !canCheckOut(shift) || activeOwnershipCount > 0" :aria-label="`Check-out ca ${shiftCodeLabel(shift.shiftCode)} ngày ${day.key}`" @click="checkOut(shift)">{{ savingShiftId === shift.shiftId ? 'Đang xử lý...' : 'Check-out' }}</button><span v-if="isCheckedIn(shift) && !isCheckedOut(shift) && !canCheckOut(shift)" class="countdown">Kết thúc theo lịch sau {{ countdown(shift) }}</span><span v-if="isCheckedIn(shift) && !isCheckedOut(shift) && activeOwnershipCount > 0" class="ownership" role="alert">Còn {{ activeOwnershipCount }} đơn cần bàn giao.</span></div></td></tr></template>
      </tbody></table></div>
    </section>
  </main>
</template>

<style scoped>
.page-header{display:flex;align-items:center;justify-content:space-between}.page-header nav,.actions{display:flex;align-items:center;gap:8px}.page-header p{margin:4px 0 0;color:var(--text-mid)}.state,.empty{padding:40px;text-align:center;color:var(--text-mid)}.page-error{margin-bottom:16px;padding:12px;border-radius:var(--radius-sm);background:#fef2f2;color:#b91c1c}.table{min-width:1050px}.table th,.table td{vertical-align:top}.today{background:#fff7ed}.current{box-shadow:inset 4px 0 #16a34a}.today-label{display:block;color:#9a3412;font-size:11px}.badge{display:inline-block;padding:3px 9px;border-radius:999px;font-size:12px;font-weight:700}.scheduled{background:#fef3c7;color:#92400e}.active{background:#dcfce7;color:#166534}.done{background:#e5e7eb;color:#4b5563}.actions{align-items:flex-start;flex-direction:column}.countdown{color:#9a3412;font-weight:600}.ownership{color:#b91c1c}.btn:focus-visible{outline:3px solid #2563eb;outline-offset:2px}
</style>
