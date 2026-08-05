<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { shiftApi } from '@/api';
import { toLocalDateKey } from '@/api/shift';
import ShiftStatus from '@/components/common/ShiftStatus.vue';

const shifts = ref([]);
const loading = ref(true);
const error = ref('');
const todayKey = ref(toLocalDateKey(new Date()));
let clockTimer;
let stopped = false;
let generation = 0;

const upcomingShifts = computed(() => shifts.value.filter(s => s.shiftDate > todayKey.value).sort((a, b) => a.shiftDate.localeCompare(b.shiftDate)));
const todayShifts = computed(() => shifts.value.filter(s => s.shiftDate === todayKey.value).sort((a, b) => (a.startTime || '').localeCompare(b.startTime || '')));
const pastShifts = computed(() => shifts.value.filter(s => s.shiftDate < todayKey.value).sort((a, b) => b.shiftDate.localeCompare(a.shiftDate)));

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
  const requestGeneration = ++generation;
  loading.value = true;
  error.value = '';
  try {
    const data = await shiftApi.getMine();
    if (stopped || requestGeneration !== generation) return;
    shifts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    if (!stopped && requestGeneration === generation) error.value = e.message;
  } finally {
    if (!stopped && requestGeneration === generation) loading.value = false;
  }
}

function refreshDay() {
  todayKey.value = toLocalDateKey(new Date());
  load();
}

onMounted(() => {
  load();
  clockTimer = setInterval(refreshDay, 30000);
  window.addEventListener('staff-shift-changed', load);
});
onBeforeUnmount(() => {
  stopped = true;
  generation += 1;
  clearInterval(clockTimer);
  window.removeEventListener('staff-shift-changed', load);
});
</script>

<template>
  <div>
    <div class="page-header"><h1><i class="bi bi-calendar-week"></i> Ca làm của tôi</h1></div>
    <ShiftStatus role="SHIPPER" />

    <div v-if="loading" class="state">Đang tải...</div>
    <div v-else-if="error" class="state error" role="alert"><p>{{ error }}</p><button class="btn btn-outline btn-sm" @click="load">Thử lại</button></div>

    <template v-else>
      <section v-if="todayShifts.length" class="card">
        <h3>Hôm nay</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>Ngày</th><th>Giờ</th><th>Check-in</th><th>Check-out</th><th>Trạng thái</th></tr></thead>
            <tbody>
              <tr v-for="s in todayShifts" :key="s.shiftId">
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

      <section v-if="upcomingShifts.length" class="card">
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
        <h3>Lịch sử ca</h3>
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

      <p v-if="!upcomingShifts.length && !todayShifts.length && !pastShifts.length" class="state">Chưa có ca làm nào.</p>
    </template>
  </div>
</template>

<style scoped>
.card { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 14px; margin-bottom: 14px; }
.card h3 { margin-bottom: 12px; font-size: 15px; }
.shift-badge { display: inline-block; padding: 2px 8px; border-radius: var(--radius-full); font-size: 11px; font-weight: 600; }
.status-active { background: #dcfce7; color: #166534; }
.status-scheduled { background: #fef3c7; color: #92400e; }
.status-done { background: #e5e7eb; color: #6b7280; }
.state { text-align: center; padding: 24px; color: var(--text-mid); }
.error { color: var(--red-active); }
</style>
