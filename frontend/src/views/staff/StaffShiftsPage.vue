<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { shiftApi } from '@/api';
import { toLocalDateKey, parseShiftEndDatetime } from '@/api/shift';
import { formatDate } from '@/utils/format';

const shifts = ref([]);
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const now = ref(new Date());
const todayKey = ref(toLocalDateKey(new Date()));
let clockTimer;

const todayShift = computed(() => shifts.value.find(s => s.shiftDate === todayKey.value) || null);
const canCheckOut = computed(() => !todayShift.value?.endTime || now.value >= parseShiftEndDatetime(todayKey.value, todayShift.value.endTime, todayShift.value.startTime));
const upcomingShifts = computed(() => shifts.value.filter(s => s.shiftDate > todayKey).sort((a, b) => a.shiftDate.localeCompare(b.shiftDate)));
const pastShifts = computed(() => shifts.value.filter(s => s.shiftDate < todayKey).sort((a, b) => b.shiftDate.localeCompare(a.shiftDate)));

function time(val) { return val ? String(val).slice(0, 5) : ''; }
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
    const data = await shiftApi.getMine();
    shifts.value = Array.isArray(data) ? data : [];
  } catch (e) { error.value = e.message; }
  finally { loading.value = false; }
}

async function checkIn(shift) {
  if (saving.value) return;
  saving.value = true;
  try {
    const updated = await shiftApi.checkIn(shift.shiftId);
    const idx = shifts.value.findIndex(s => s.shiftId === updated.shiftId);
    if (idx >= 0) shifts.value[idx] = updated;
    window.dispatchEvent(new Event('staff-shift-changed'));
  } catch (e) { error.value = e.message; }
  finally { saving.value = false; }
}

async function checkOut(shift) {
  if (saving.value) return;
  saving.value = true;
  try {
    const updated = await shiftApi.checkOut(shift.shiftId);
    const idx = shifts.value.findIndex(s => s.shiftId === updated.shiftId);
    if (idx >= 0) shifts.value[idx] = updated;
    window.dispatchEvent(new Event('staff-shift-changed'));
  } catch (e) { error.value = e.message; }
  finally { saving.value = false; }
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
    <div v-else-if="error" style="text-align:center;padding:40px;color:var(--red-active)">{{ error }}</div>

    <template v-else>
      <section v-if="todayShift" class="card" style="margin-bottom:16px">
        <h3>Ca hôm nay</h3>
        <div class="shift-detail">
          <div class="shift-info">
            <div class="shift-time"><i class="bi bi-clock"></i> {{ time(todayShift.startTime) }} – {{ time(todayShift.endTime) }}</div>
            <div class="shift-date">{{ todayShift.shiftDate }}</div>
          </div>
          <div class="shift-status" :class="statusClass(todayShift.status)">{{ statusLabel(todayShift.status) }}</div>
        </div>
        <div v-if="todayShift.checkInAt" class="shift-check">
          Check-in: {{ todayShift.checkInAt }}
          <span v-if="todayShift.checkOutAt"> · Check-out: {{ todayShift.checkOutAt }}</span>
        </div>
        <div class="shift-actions">
          <button v-if="!todayShift.checkInAt" class="btn btn-primary" :disabled="saving" @click="checkIn(todayShift)">
            <i class="bi bi-box-arrow-in-right"></i> {{ saving ? 'Đang xử lý...' : 'Check-in' }}
          </button>
          <button v-else-if="!todayShift.checkOutAt" class="btn btn-outline" :disabled="saving || !canCheckOut" @click="checkOut(todayShift)">
            <i class="bi bi-box-arrow-right"></i> {{ saving ? 'Đang xử lý...' : canCheckOut ? 'Check-out' : `Có thể check-out từ ${time(todayShift.endTime)}` }}
          </button>
          <span v-else class="shift-done"><i class="bi bi-check-circle-fill"></i> Đã hoàn thành ca</span>
        </div>
      </section>
      <section v-else class="card" style="margin-bottom:16px">
        <h3>Ca hôm nay</h3>
        <p style="color:var(--text-mid)">Không có ca được phân công hôm nay.</p>
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
.shift-badge { display: inline-block; padding: 2px 8px; border-radius: var(--radius-full); font-size: 11px; font-weight: 600; }
</style>
