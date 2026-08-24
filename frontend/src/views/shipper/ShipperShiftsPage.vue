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
  <div class="shift-sections">
    <div class="page-header"><h1><i class="bi bi-calendar-week"></i> Ca làm của tôi</h1></div>
    <ShiftStatus role="SHIPPER" />

    <div v-if="loading" class="state">Đang tải...</div>
    <div v-else-if="error" class="state error" role="alert"><p>{{ error }}</p><button class="btn btn-outline btn-sm" @click="load">Thử lại</button></div>

    <template v-else>
      <section v-if="todayShifts.length" class="card">
        <h2>Hôm nay</h2>
        <article v-for="s in todayShifts" :key="s.shiftId" class="shift-card">
          <div class="shift-main"><strong>{{ s.shiftDate }}</strong><span>{{ time(s.startTime) }} – {{ time(s.endTime) }}</span></div>
          <dl class="shift-times"><div><dt>Check-in</dt><dd>{{ s.checkInAt || 'Chưa check-in' }}</dd></div><div><dt>Check-out</dt><dd>{{ s.checkOutAt || 'Chưa check-out' }}</dd></div></dl>
          <span class="shift-badge" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
        </article>
      </section>

      <section v-if="upcomingShifts.length" class="card">
        <h2>Ca sắp tới</h2>
        <article v-for="s in upcomingShifts" :key="s.shiftId" class="shift-card compact">
          <div class="shift-main"><strong>{{ s.shiftDate }}</strong><span>{{ time(s.startTime) }} – {{ time(s.endTime) }}</span></div>
          <span class="shift-badge" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
        </article>
      </section>

      <section v-if="pastShifts.length" class="card">
        <h2>Lịch sử ca</h2>
        <article v-for="s in pastShifts" :key="s.shiftId" class="shift-card">
          <div class="shift-main"><strong>{{ s.shiftDate }}</strong><span>{{ time(s.startTime) }} – {{ time(s.endTime) }}</span></div>
          <dl class="shift-times"><div><dt>Check-in</dt><dd>{{ s.checkInAt || '—' }}</dd></div><div><dt>Check-out</dt><dd>{{ s.checkOutAt || '—' }}</dd></div></dl>
          <span class="shift-badge" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
        </article>
      </section>

      <p v-if="!upcomingShifts.length && !todayShifts.length && !pastShifts.length" class="state">Chưa có ca làm nào.</p>
    </template>
  </div>
</template>

<style scoped>
.shift-sections { display:grid; gap:14px; }.card { margin-bottom: 0; }
.card h2 { margin-bottom: 10px; font-size: 15px; }
.shift-card { display: grid; gap: 12px; padding: 14px; background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); }
.shift-card + .shift-card { margin-top: 10px; }
.shift-main { display: flex; justify-content: space-between; gap: 12px; }
.shift-main span { color: var(--text-mid); font-weight: 600; }
.shift-times { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 0; }
.shift-times div { padding: 9px; background: var(--surface); border-radius: var(--radius-sm); }
.shift-times dt { color: var(--text-light); font-size: 11px; }
.shift-times dd { margin: 3px 0 0; color: var(--text-dark); font-size: 12px; font-weight: 600; overflow-wrap: anywhere; }
.shift-badge { width: fit-content; display: inline-block; padding: 3px 9px; border-radius: var(--radius-full); font-size: 11px; font-weight: 700; }
.status-active { background: #dcfce7; color: #166534; }
.status-scheduled { background: #fef3c7; color: #92400e; }
.status-done { background: #e5e7eb; color: #4b5563; }
.state { text-align: center; padding: 24px; color: var(--text-mid); }
.error { color: var(--red-active); }
@media(min-width:600px) {
  .shift-card { grid-template-columns: minmax(190px, 1fr) minmax(260px, 1.5fr) auto; align-items: center; }
  .shift-card.compact { grid-template-columns: 1fr auto; }
  .shift-main { justify-content: flex-start; gap: 24px; }
}
@media(min-width:900px) { .shift-sections { grid-template-columns:repeat(2,minmax(0,1fr)); align-items:start; }.shift-sections>.page-header,.shift-sections>.shift-command,.shift-sections>.state { grid-column:1/-1; }.shift-sections>.card:last-of-type { grid-column:1/-1; } }
</style>
