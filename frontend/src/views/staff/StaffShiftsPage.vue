<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { shiftApi, staffApi } from '@/api';
import { toLocalDateKey, parseShiftEndDatetime } from '@/api/shift';

const shifts = ref([]);
const activeOwnershipCount = ref(0);
const loading = ref(true);
const savingShiftId = ref(null);
const error = ref('');
const now = ref(new Date());
const todayKey = ref(toLocalDateKey(new Date()));
let clockTimer;

const todayShifts = computed(() => shifts.value.filter(s => s.shiftDate === todayKey.value).sort((a, b) => String(a.startTime).localeCompare(String(b.startTime))));
const upcomingShifts = computed(() => shifts.value.filter(s => s.shiftDate > todayKey.value).sort((a, b) => a.shiftDate.localeCompare(b.shiftDate)));
const pastShifts = computed(() => shifts.value.filter(s => s.shiftDate < todayKey.value).sort((a, b) => b.shiftDate.localeCompare(a.shiftDate)));

function time(val) { return val ? String(val).slice(0, 5) : ''; }
function isCheckedIn(shift) { return Boolean(shift.checkInAt) || shift.status === 'CHECKED_IN'; }
function isCheckedOut(shift) { return Boolean(shift.checkOutAt) || ['CHECKED_OUT', 'COMPLETED'].includes(shift.status); }
function canCheckOut(shift) { return !shift.endTime || now.value >= parseShiftEndDatetime(shift.shiftDate, shift.endTime, shift.startTime); }
function isSaving(shift) { return savingShiftId.value === shift.shiftId; }
function statusLabel(s) {
  if (s === 'CHECKED_IN') return 'Đang trong ca';
  if (s === 'CHECKED_OUT') return 'Đã kết thúc';
  if (s === 'COMPLETED') return 'Hoàn thành';
  return 'Đã xếp lịch';
}
function statusClass(s) {
  if (s === 'CHECKED_IN') return 'status-active';
  if (s === 'CHECKED_OUT' || s === 'COMPLETED') return 'status-done';
  return 'status-scheduled';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [data, ownership] = await Promise.all([shiftApi.getMine(), staffApi.getOwnershipCount()]);
    shifts.value = Array.isArray(data) ? data : [];
    activeOwnershipCount.value = Number(ownership?.activeOwnershipCount || 0);
  } catch (e) { error.value = e.message; }
  finally { loading.value = false; }
}

async function checkIn(shift) {
  if (savingShiftId.value !== null || isCheckedIn(shift)) return;
  savingShiftId.value = shift.shiftId;
  try {
    const updated = await shiftApi.checkIn(shift.shiftId);
    const idx = shifts.value.findIndex(s => s.shiftId === updated.shiftId);
    if (idx >= 0) shifts.value[idx] = updated;
    window.dispatchEvent(new Event('staff-shift-changed'));
  } catch (e) { error.value = e.message; }
  finally { savingShiftId.value = null; }
}

async function checkOut(shift) {
  if (savingShiftId.value !== null || !isCheckedIn(shift) || isCheckedOut(shift) || !canCheckOut(shift) || activeOwnershipCount.value > 0) return;
  savingShiftId.value = shift.shiftId;
  try {
    const updated = await shiftApi.checkOut(shift.shiftId);
    const idx = shifts.value.findIndex(s => s.shiftId === updated.shiftId);
    if (idx >= 0) shifts.value[idx] = updated;
    window.dispatchEvent(new Event('staff-shift-changed'));
  } catch (error) {
    if (error.status === 409) {
      activeOwnershipCount.value = Number(error.data?.activeOwnershipCount || 0);
      error.value = `Không thể check-out: còn ${activeOwnershipCount.value} đơn đang thuộc ca này.`;
    } else error.value = error.message;
  }
  finally { savingShiftId.value = null; }
}

onMounted(() => {
  load();
  clockTimer = setInterval(() => {
    now.value = new Date();
    todayKey.value = toLocalDateKey(new Date());
  }, 30000);
});
onUnmounted(() => clearInterval(clockTimer));
</script>

<template>
  <div>
    <div class="page-header"><h1><i class="bi bi-calendar-week"></i> Ca làm của tôi</h1></div>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-mid)">Đang tải...</div>

    <template v-else>
      <div v-if="error" class="page-error" role="alert">{{ error }} <router-link v-if="activeOwnershipCount > 0" to="/staff/orders?tab=HANDOVER">Mở danh sách bàn giao</router-link></div>
      <section class="card" style="margin-bottom:16px" aria-labelledby="today-shifts-title">
        <h3 id="today-shifts-title">Ca hôm nay</h3>
        <div v-if="todayShifts.length" class="today-shifts">
          <article v-for="shift in todayShifts" :key="shift.shiftId" class="today-shift">
            <div class="shift-detail">
              <div class="shift-info">
                <div class="shift-time"><i class="bi bi-clock" aria-hidden="true"></i> {{ time(shift.startTime) }} – {{ time(shift.endTime) }}</div>
                <div class="shift-date">{{ shift.shiftDate }}</div>
              </div>
              <div class="shift-status" :class="statusClass(shift.status)">{{ statusLabel(shift.status) }}</div>
            </div>
            <div v-if="shift.checkInAt" class="shift-check">
              Check-in: {{ shift.checkInAt }}
              <span v-if="shift.checkOutAt"> · Check-out: {{ shift.checkOutAt }}</span>
            </div>
            <div v-if="isCheckedIn(shift) && !isCheckedOut(shift) && activeOwnershipCount > 0" class="ownership-warning" role="alert">
              Còn {{ activeOwnershipCount }} đơn cần bàn giao trước khi check-out.
              <router-link to="/staff/orders?tab=HANDOVER">Mở danh sách bàn giao</router-link>
            </div>
            <div class="shift-actions" aria-live="polite">
              <button v-if="!isCheckedIn(shift)" type="button" class="btn btn-primary" :disabled="savingShiftId !== null" @click="checkIn(shift)">
                <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i> {{ isSaving(shift) ? 'Đang xử lý...' : 'Check-in' }}
              </button>
              <button v-else-if="!isCheckedOut(shift)" type="button" class="btn btn-outline" :disabled="savingShiftId !== null || !canCheckOut(shift) || activeOwnershipCount > 0" @click="checkOut(shift)">
                <i class="bi bi-box-arrow-right" aria-hidden="true"></i> {{ isSaving(shift) ? 'Đang xử lý...' : canCheckOut(shift) ? 'Check-out' : `Có thể check-out từ ${time(shift.endTime)}` }}
              </button>
              <span v-else class="shift-done"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Đã hoàn thành ca</span>
            </div>
          </article>
        </div>
        <p v-else style="color:var(--text-mid)">Không có ca được phân công hôm nay.</p>
      </section>

      <section v-if="upcomingShifts.length" class="card" style="margin-bottom:16px">
        <h3>Ca sắp tới</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Ngày</th><th>Giờ</th><th>Trạng thái</th></tr></thead>
            <tbody>
              <tr v-for="s in upcomingShifts" :key="s.shiftId">
                <td>{{ s.shiftDate }}</td>
                <td>{{ time(s.startTime) }} – {{ time(s.endTime) }}</td>
                <td><span class="shift-badge" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="pastShifts.length" class="card">
        <h3>Lịch sử ca làm</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Ngày</th><th>Giờ</th><th>Check-in</th><th>Check-out</th><th>Trạng thái</th></tr></thead>
            <tbody>
              <tr v-for="s in pastShifts" :key="s.shiftId">
                <td>{{ s.shiftDate }}</td>
                <td>{{ time(s.startTime) }} – {{ time(s.endTime) }}</td>
                <td>{{ s.checkInAt || '—' }}</td>
                <td>{{ s.checkOutAt || '—' }}</td>
                <td><span class="shift-badge" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.card h3 { margin-bottom: 12px; font-size: 15px; }
.page-error { margin-bottom: 16px; padding: 12px; border-radius: var(--radius-sm); background: #fef2f2; color: #b91c1c; }
.page-error a { margin-left: 6px; font-weight: 700; }
.today-shifts { display: grid; gap: 12px; }
.today-shift { padding: 14px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--teal-light); }
.shift-detail { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.shift-time { font-size: 20px; font-weight: 800; }
.shift-date { font-size: 13px; color: var(--text-mid); margin-top: 2px; }
.shift-status { padding: 4px 12px; border-radius: var(--radius-full); font-size: 12px; font-weight: 700; white-space: nowrap; }
.status-active { background: #dcfce7; color: #166534; }
.status-scheduled { background: #fef3c7; color: #92400e; }
.status-done { background: #e5e7eb; color: #6b7280; }
.shift-check { font-size: 13px; color: var(--text-mid); margin-bottom: 12px; }
.shift-actions { display: flex; gap: 8px; align-items: center; }
.shift-done { color: #166534; font-size: 14px; font-weight: 600; }
.ownership-warning { margin-bottom: 12px; padding: 10px 12px; border-radius: var(--radius-sm); background: #fff7ed; color: #9a3412; font-size: 13px; }
.ownership-warning a { display: inline-block; min-height: 44px; margin-left: 6px; font-weight: 700; line-height: 44px; }
.shift-badge { display: inline-block; padding: 2px 8px; border-radius: var(--radius-full); font-size: 11px; font-weight: 600; }
</style>
