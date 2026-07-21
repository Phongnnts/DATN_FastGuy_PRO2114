<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { shiftApi } from '@/api';

const props = defineProps({
  role: { type: String, required: true },
});

const shifts = ref([]);
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const now = ref(new Date());
const todayKey = ref(toLocalDateKey(new Date()));
let clockTimer;

const currentShift = computed(() => shifts.value.find((shift) => shift.role === props.role && shift.shiftDate === todayKey.value && !shift.checkOutAt) || null);
const action = computed(() => currentShift.value?.checkInAt ? 'checkOut' : 'checkIn');
const canCheckOut = computed(() => !currentShift.value?.endTime || now.value >= parseShiftEndDatetime(todayKey.value, currentShift.value.endTime, currentShift.value.startTime));

function time(value) {
  return value ? String(value).slice(0, 5) : '';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await shiftApi.getMine();
    shifts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (!currentShift.value || saving.value) return;
  saving.value = true;
  error.value = '';
  try {
    const updated = await shiftApi[action.value](currentShift.value.shiftId);
    const index = shifts.value.findIndex((shift) => shift.shiftId === updated.shiftId);
    if (index >= 0) shifts.value[index] = updated;
    window.dispatchEvent(new Event('staff-shift-changed'));
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
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
  <section class="shift-card">
    <div class="shift-heading">
      <div>
        <span class="shift-kicker">Ca làm hôm nay</span>
        <strong v-if="currentShift">{{ time(currentShift.startTime) }} – {{ time(currentShift.endTime) }}</strong>
        <strong v-else>Không có ca</strong>
      </div>
      <span v-if="currentShift" class="shift-state" :class="{ active: currentShift.checkInAt }">
        {{ currentShift.checkInAt ? 'Đang trong ca' : 'Đã xếp lịch' }}
      </span>
    </div>
    <p v-if="loading" class="shift-note">Đang tải ca làm...</p>
    <p v-else-if="error" class="shift-note error">{{ error }}</p>
    <template v-else-if="currentShift">
      <p class="shift-note">{{ currentShift.checkInAt ? `Check-in ${currentShift.checkInAt}` : 'Chưa check-in' }}</p>
      <div aria-live="polite" role="status" class="sr-only">{{ saving ? 'Đang cập nhật trạng thái ca.' : currentShift.checkInAt ? canCheckOut ? 'Đã cho phép check-out.' : `Có thể check-out từ ${time(currentShift.endTime)}.` : 'Đang sẵn sàng check-in.' }}</div>
      <button class="btn btn-sm" :class="currentShift.checkInAt ? 'btn-outline' : 'btn-primary'" :disabled="saving || (currentShift.checkInAt && !canCheckOut)" @click="submit">
        {{ saving ? 'Đang cập nhật...' : currentShift.checkInAt ? canCheckOut ? 'Check-out' : `Có thể check-out từ ${time(currentShift.endTime)}` : 'Check-in' }}
      </button>
    </template>
    <p v-else class="shift-note">Chưa có ca được phân công hôm nay.</p>
  </section>
</template>

<style scoped>
.shift-card { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 14px; margin-bottom: 14px; }
.shift-heading { display: flex; justify-content: space-between; gap: 12px; align-items: start; }
.shift-kicker { display: block; color: var(--text-mid); font-size: 11px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; margin-bottom: 3px; }
.shift-state { background: #fef3c7; border-radius: var(--radius-full); color: #92400e; font-size: 11px; font-weight: 700; padding: 4px 8px; white-space: nowrap; }
.shift-state.active { background: #dcfce7; color: #166534; }
.shift-note { color: var(--text-mid); font-size: 13px; margin: 8px 0; }
.error { color: var(--red-active); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
</style>
