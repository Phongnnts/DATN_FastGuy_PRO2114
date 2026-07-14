<script setup>
import { ref, computed, onMounted } from 'vue';
import { adminApi } from '@/api';

const shifts = ref([]);
const users = ref([]);
const loading = ref(true);
const showModal = ref(false);
const editing = ref(null);
const submitting = ref(false);
const form = ref({ userId: '', shiftDate: '', startTime: '08:00', endTime: '17:00', status: 'SCHEDULED' });

const employees = computed(() => users.value.filter((u) => u.roleName === 'STAFF' || u.roleName === 'SHIPPER'));

function timeValue(value) {
  return value ? String(value).slice(0, 5) : '';
}

async function load() {
  loading.value = true;
  try {
    const [shiftData, userData] = await Promise.all([adminApi.getShifts(), adminApi.getUsers()]);
    shifts.value = Array.isArray(shiftData) ? shiftData : [];
    users.value = Array.isArray(userData) ? userData : [];
  } catch (e) {
    alert(e.message);
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  form.value = { userId: '', shiftDate: '', startTime: '08:00', endTime: '17:00', status: 'SCHEDULED' };
  showModal.value = true;
}

function openEdit(shift) {
  editing.value = shift;
  form.value = {
    userId: shift.userId,
    shiftDate: shift.shiftDate,
    startTime: timeValue(shift.startTime),
    endTime: timeValue(shift.endTime),
    status: shift.status || 'SCHEDULED',
  };
  showModal.value = true;
}

async function remove(shift) {
  if (!confirm(`Xóa ca của ${shift.userName}?`)) return;
  try {
    await adminApi.deleteShift(shift.shiftId);
    await load();
  } catch (e) {
    alert(e.message || 'Không thể xóa ca đã bắt đầu');
  }
}

async function save() {
  submitting.value = true;
  try {
    const payload = { ...form.value, userId: Number(form.value.userId) };
    if (editing.value) await adminApi.updateShift(editing.value.shiftId, payload);
    else await adminApi.createShift(payload);
    showModal.value = false;
    await load();
  } catch (e) {
    alert(e.message);
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý ca làm</h1>
      <button class="btn btn-sm btn-primary" @click="openCreate"><i class="bi bi-plus-lg"></i> Tạo ca</button>
    </div>
    <div v-if="loading" class="card card-flat text-center" style="padding:32px"><div class="spinner"></div></div>
    <div v-else class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Ngày</th><th>Bắt đầu</th><th>Kết thúc</th><th>Trạng thái</th><th>Check-in</th><th>Check-out</th><th></th></tr></thead>
          <tbody>
            <tr v-for="shift in shifts" :key="shift.shiftId">
              <td><strong>{{ shift.userName }}</strong></td>
              <td><span class="badge">{{ shift.role }}</span></td>
              <td>{{ shift.shiftDate }}</td>
              <td>{{ timeValue(shift.startTime) }}</td>
              <td>{{ timeValue(shift.endTime) }}</td>
              <td><span class="badge">{{ shift.status }}</span></td>
              <td>{{ shift.checkInAt || '—' }}</td>
              <td>{{ shift.checkOutAt || '—' }}</td>
              <td><button class="btn btn-sm btn-outline" :aria-label="`Sửa ca ${shift.userName}`" @click="openEdit(shift)"><i class="bi bi-pencil"></i></button><button v-if="shift.status === 'SCHEDULED'" class="btn btn-sm btn-ghost text-danger" :aria-label="`Xóa ca ${shift.userName}`" @click="remove(shift)"><i class="bi bi-trash3"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="shifts.length === 0" class="empty-state" style="padding:20px">Chưa có ca làm</p>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <form class="modal" @submit.prevent="save">
        <div class="modal-header">
          <h3>{{ editing ? 'Sửa ca làm' : 'Tạo ca làm' }}</h3>
          <button type="button" class="btn btn-sm btn-outline" @click="showModal = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Nhân viên</label>
            <select v-model="form.userId" class="form-select" required>
              <option disabled value="">Chọn STAFF hoặc SHIPPER</option>
              <option v-for="user in employees" :key="user.userId" :value="user.userId">{{ user.fullName }} ({{ user.roleName }})</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Ngày làm</label>
            <input v-model="form.shiftDate" class="form-input" type="date" required>
          </div>
          <div class="form-group">
            <label class="form-label">Bắt đầu</label>
            <input v-model="form.startTime" class="form-input" type="time" required>
          </div>
          <div class="form-group">
            <label class="form-label">Kết thúc</label>
            <input v-model="form.endTime" class="form-input" type="time" required>
          </div>
          <div class="form-group">
            <label class="form-label">Trạng thái</label>
            <select v-model="form.status" class="form-select">
              <option value="SCHEDULED">SCHEDULED</option>
              <option value="CHECKED_IN">CHECKED_IN</option>
              <option value="COMPLETED">COMPLETED</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="showModal = false">Hủy</button>
          <button class="btn btn-primary" :disabled="submitting">{{ submitting ? 'Đang lưu...' : 'Lưu' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>
