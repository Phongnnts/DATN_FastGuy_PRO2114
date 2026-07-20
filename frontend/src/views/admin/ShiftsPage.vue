<script setup>
import { ref, computed, onMounted } from 'vue';
import { adminApi } from '@/api';
import { formatDateShort } from '@/utils/format';
import { useToast } from '@/stores/toast';

const toast = useToast();
const shifts = ref([]);
const users = ref([]);
const loading = ref(true);
const showModal = ref(false);
const editing = ref(null);
const submitting = ref(false);
const form = ref({ userId: '', shiftDate: '', startTime: '08:00', endTime: '17:00', status: 'SCHEDULED' });

const filterRole = ref('');
const filterUserId = ref('');
const filterFromDate = ref('');
const filterToDate = ref('');

const employees = computed(() => users.value.filter((u) => u.roleName === 'STAFF' || u.roleName === 'SHIPPER'));
const filteredEmployees = computed(() => {
  if (!filterRole.value) return employees.value;
  return employees.value.filter((u) => u.roleName === filterRole.value);
});

function timeValue(value) {
  return value ? String(value).slice(0, 5) : '';
}

async function load() {
  loading.value = true;
  try {
    const params = {};
    if (filterRole.value) params.role = filterRole.value;
    if (filterUserId.value) params.userId = filterUserId.value;
    if (filterFromDate.value) params.fromDate = filterFromDate.value;
    if (filterToDate.value) params.toDate = filterToDate.value;
    const [shiftData, userData] = await Promise.all([adminApi.getShifts(params), adminApi.getUsers()]);
    shifts.value = Array.isArray(shiftData) ? shiftData : [];
    users.value = Array.isArray(userData) ? userData : [];
  } catch (e) {
    toast.error(e.message);
  } finally {
    loading.value = false;
  }
}

function clearFilters() {
  filterRole.value = '';
  filterUserId.value = '';
  filterFromDate.value = '';
  filterToDate.value = '';
  load();
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
    toast.error(e.message || 'Không thể xóa ca đã bắt đầu');
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
    toast.error(e.message);
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
    <div class="card card-flat" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:end">
        <div class="form-group" style="margin:0;min-width:140px">
          <label class="form-label" style="font-size:12px">Vai trò</label>
          <select v-model="filterRole" class="form-select" @change="load">
            <option value="">Tất cả</option>
            <option value="STAFF">Staff</option>
            <option value="SHIPPER">Shipper</option>
          </select>
        </div>
        <div class="form-group" style="margin:0;min-width:180px">
          <label class="form-label" style="font-size:12px">Nhân viên</label>
          <select v-model="filterUserId" class="form-select" @change="load">
            <option value="">Tất cả</option>
            <option v-for="u in filteredEmployees" :key="u.userId" :value="u.userId">{{ u.fullName }}</option>
          </select>
        </div>
        <div class="form-group" style="margin:0">
          <label class="form-label" style="font-size:12px">Từ ngày</label>
          <input v-model="filterFromDate" type="date" class="form-input" @change="load">
        </div>
        <div class="form-group" style="margin:0">
          <label class="form-label" style="font-size:12px">Đến ngày</label>
          <input v-model="filterToDate" type="date" class="form-input" @change="load">
        </div>
        <button v-if="filterRole || filterUserId || filterFromDate || filterToDate" class="btn btn-sm btn-outline" @click="clearFilters"><i class="bi bi-x-lg"></i> Xóa bộ lọc</button>
      </div>
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
              <td>{{ formatDateShort(shift.shiftDate) }}</td>
              <td>{{ timeValue(shift.startTime) }}</td>
              <td>{{ timeValue(shift.endTime) }}</td>
              <td><span class="badge" :class="'badge-' + (shift.status === 'CHECKED_IN' ? 'success' : shift.status === 'CHECKED_OUT' ? 'secondary' : 'warning')">{{ shift.status }}</span></td>
              <td>{{ shift.checkInAt ? formatDateShort(shift.checkInAt) + ' ' + timeValue(shift.checkInAt) : '—' }}</td>
              <td>{{ shift.checkOutAt ? formatDateShort(shift.checkOutAt) + ' ' + timeValue(shift.checkOutAt) : '—' }}</td>
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
              <option value="CHECKED_OUT">CHECKED_OUT</option>
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
