<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const CODE_LABELS = { MORNING: 'Sáng', AFTERNOON: 'Chiều', EVENING: 'Tối' };
const toast = useToast();
const users = ref([]);
const attendance = ref([]);
const attendanceMonth = ref(dateKey(new Date()).slice(0, 7));
const attendanceStatus = ref('PENDING');
const attendanceUserId = ref('');
const attendanceLoading = ref(false);
const attendanceError = ref('');
const attendanceSavingId = ref(null);
let attendanceGeneration = 0;
const staff = computed(() => users.value.filter(user => user.roleName === 'STAFF' && (user.status || 'ACTIVE') === 'ACTIVE'));

function dateKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
async function loadUsers() { try { const data = await adminApi.getUsers(); users.value = Array.isArray(data) ? data : []; } catch (error) { attendanceError.value = error.message || 'Không thể tải nhân viên'; } }
async function loadAttendance() {
  const generation = ++attendanceGeneration;
  const requested = `${attendanceMonth.value}|${attendanceStatus.value}|${attendanceUserId.value}`;
  attendanceLoading.value = true;
  attendanceError.value = '';
  try {
    const data = await adminApi.getShiftAttendance({ month: attendanceMonth.value, status: attendanceStatus.value || undefined, userId: attendanceUserId.value ? Number(attendanceUserId.value) : undefined });
    if (generation !== attendanceGeneration || requested !== `${attendanceMonth.value}|${attendanceStatus.value}|${attendanceUserId.value}`) return;
    attendance.value = Array.isArray(data) ? data : [];
  } catch (error) {
    if (generation === attendanceGeneration && requested === `${attendanceMonth.value}|${attendanceStatus.value}|${attendanceUserId.value}`) attendanceError.value = error.message || 'Không thể tải chấm công';
  } finally { if (generation === attendanceGeneration) attendanceLoading.value = false; }
}
async function approve(item) {
  if (attendanceSavingId.value !== null) return;
  attendanceSavingId.value = item.shiftId;
  attendanceError.value = '';
  try {
    await adminApi.approveShiftAttendance(item.shiftId, { expectedUpdatedAt: item.updatedAt, approvedMinutes: Number(item.approvedMinutes ?? item.overlapEligibleMinutes), approvedOvertimeMinutes: Number(item.approvedOvertimeMinutes ?? item.potentialOvertimeMinutes), attendanceNote: item.attendanceNote || null });
    await loadAttendance();
    toast.success('Đã duyệt chấm công');
  } catch (error) {
    attendanceError.value = error.status === 409 ? 'Dữ liệu đã thay đổi. Danh sách đang được tải lại.' : error.message || 'Không thể duyệt chấm công';
    if (error.status === 409) await loadAttendance();
  } finally { attendanceSavingId.value = null; }
}
onMounted(async () => { await loadUsers(); await loadAttendance(); });
onUnmounted(() => { attendanceGeneration++; });
</script>

<template>
  <main class="attendance-page">
    <header class="page-header"><div><p class="eyebrow">Nhân sự</p><h1>Chấm công & tiền công</h1><p>Đối chiếu giờ làm thực tế và duyệt công theo tháng.</p></div></header>
    <section class="panel" aria-labelledby="attendance-title">
      <div class="toolbar"><h2 id="attendance-title">Chấm công</h2><label>Tháng <input v-model="attendanceMonth" type="month" @change="loadAttendance"></label><label>Trạng thái <select v-model="attendanceStatus" class="form-select" @change="loadAttendance"><option value="">Tất cả</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option></select></label><label>Nhân viên <select v-model="attendanceUserId" class="form-select" @change="loadAttendance"><option value="">Tất cả</option><option v-for="user in staff" :key="user.userId" :value="String(user.userId)">{{ user.fullName }}</option></select></label><button class="btn btn-outline" type="button" :disabled="attendanceLoading" @click="loadAttendance">Làm mới</button></div>
      <div v-if="attendanceError" class="state error" role="alert">{{ attendanceError }} <button class="btn btn-outline" type="button" @click="loadAttendance">Thử lại</button></div>
      <div v-else-if="attendanceLoading && !attendance.length" class="state" role="status">Đang tải chấm công...</div>
      <div v-else-if="!attendance.length" class="state">Không có chấm công phù hợp.</div>
      <div v-else class="table-wrapper"><table class="table"><thead><tr><th>Ngày / ca</th><th>Nhân viên</th><th>Thực tế</th><th>Hợp lệ</th><th>Muộn / sớm</th><th>OT</th><th>Phút duyệt</th><th>OT duyệt</th><th>Ghi chú</th><th></th></tr></thead><tbody><tr v-for="item in attendance" :key="item.shiftId"><td data-label="Ngày / ca">{{ item.shiftDate }}<small>{{ CODE_LABELS[item.shiftCode] }}</small></td><td data-label="Nhân viên">{{ item.staffName }}</td><td data-label="Thực tế">{{ item.actualMinutes }}</td><td data-label="Hợp lệ">{{ item.overlapEligibleMinutes }}</td><td data-label="Muộn / sớm">{{ item.lateMinutes }} / {{ item.earlyLeaveMinutes }}</td><td data-label="OT">{{ item.potentialOvertimeMinutes }}</td><td data-label="Phút duyệt"><input v-model.number="item.approvedMinutes" type="number" min="0" :max="item.overlapEligibleMinutes" aria-label="Phút duyệt"></td><td data-label="OT duyệt"><input v-model.number="item.approvedOvertimeMinutes" type="number" min="0" :max="item.potentialOvertimeMinutes" aria-label="Phút tăng ca duyệt"></td><td data-label="Ghi chú"><input v-model="item.attendanceNote" maxlength="500" aria-label="Ghi chú chấm công"></td><td data-label="Thao tác"><button class="btn btn-primary" type="button" :disabled="attendanceSavingId !== null || item.attendanceStatus === 'APPROVED'" @click="approve(item)">{{ attendanceSavingId === item.shiftId ? 'Đang duyệt...' : item.attendanceStatus === 'APPROVED' ? 'Đã duyệt' : 'Duyệt' }}</button></td></tr></tbody></table></div>
    </section>
  </main>
</template>

<style scoped>
.attendance-page{display:grid;gap:18px}.page-header h1{margin:3px 0}.page-header p{margin:0;color:var(--text-mid)}.eyebrow{color:var(--role-admin);font-size:11px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.panel{overflow:hidden;border:1px solid var(--border);border-radius:16px;background:#fff}.toolbar{display:flex;align-items:end;gap:12px;flex-wrap:wrap;padding:16px}.toolbar h2{margin:0 auto 0 0}.toolbar label{display:grid;gap:5px;color:var(--text-mid);font-size:12px;font-weight:700}.toolbar input,.toolbar select,.toolbar button,.table input,.table button{min-height:40px}.state{display:flex;min-height:220px;align-items:center;justify-content:center;gap:12px;color:var(--text-mid)}.error{color:#b91c1c}.table-wrapper{overflow-x:auto}.table{min-width:1080px}.table td small{display:block;color:var(--text-mid)}.attendance-page :is(button,input,select):focus-visible{outline:3px solid var(--primary);outline-offset:2px}@media(max-width:700px){.toolbar,.toolbar label,.toolbar .btn{width:100%}.toolbar h2{width:100%}.table-wrapper{overflow:visible}.table{min-width:0}.table thead{display:none}.table,.table tbody,.table tr,.table td{display:block;width:100%}.table tr{padding:14px;border-top:1px solid var(--border)}.table td{display:grid;grid-template-columns:110px minmax(0,1fr);gap:10px;align-items:center;padding:7px 0;border:0}.table td::before{content:attr(data-label);color:var(--text-mid);font-size:12px;font-weight:700}.table input,.table button{width:100%}}
</style>
