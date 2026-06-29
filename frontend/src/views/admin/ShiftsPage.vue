<script setup>
import { ref, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatTime } from '@/utils/format';
import { SCHEDULE_STATUS_LABEL, SHIFT_TYPES } from '@/utils/constants';

const adminStore = useAdminStore();
const showShiftForm = ref(false);

onMounted(() => {
  adminStore.fetchShifts();
  adminStore.fetchSchedules();
  adminStore.fetchUsers();
});
const showScheduleForm = ref(false);
const shiftForm = ref({ name: '', startTime: '06:00', endTime: '14:00' });
const scheduleForm = ref({
  userId: null,
  shiftId: null,
  date: '',
  status: 'PENDING',
});
const editingShiftId = ref(null);

function openAddShift() {
  editingShiftId.value = null;
  shiftForm.value = {
    name: 'Sáng (6h-14h)',
    startTime: '06:00',
    endTime: '14:00',
  };
  showShiftForm.value = true;
}

function openEditShift(s) {
  editingShiftId.value = s.id;
  shiftForm.value = {
    name: s.name,
    startTime: s.startTime,
    endTime: s.endTime,
  };
  showShiftForm.value = true;
}

function saveShift() {
  if (editingShiftId.value)
    adminStore.updateShift(editingShiftId.value, shiftForm.value);
  else adminStore.createShift(shiftForm.value);
  showShiftForm.value = false;
}

function deleteShift(id) {
  if (confirm('Xóa ca?')) adminStore.deleteShift(id);
}

function openAddSchedule() {
  scheduleForm.value = {
    userId: null,
    shiftId: adminStore.allShifts[0]?.id || null,
    date: '',
    status: 'PENDING',
  };
  showScheduleForm.value = true;
}

function saveSchedule() {
  adminStore.createSchedule(scheduleForm.value);
  showScheduleForm.value = false;
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý ca làm việc</h1>
      <div style="display: flex; gap: 8px">
        <button class="btn btn-sm btn-outline" @click="openAddSchedule">
          <i class="bi bi-calendar-plus"></i> Phân ca
        </button>
        <button class="btn btn-sm btn-primary" @click="openAddShift">
          <i class="bi bi-plus-lg"></i> Thêm ca
        </button>
      </div>
    </div>
    <div class="grid-2">
      <div class="card">
        <h3 style="font-weight: 700; margin-bottom: 16px">Ca làm việc</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Tên ca</th>
                <th>Giờ</th>
                <th>NV</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in adminStore.allShifts" :key="s.id">
                <td>
                  <strong>{{ s.name }}</strong>
                </td>
                <td>{{ s.startTime }} - {{ s.endTime }}</td>
                <td>{{ s.staffCount }}</td>
                <td>
                  <button
                    class="btn btn-sm btn-ghost"
                    @click="openEditShift(s)"
                  >
                    <i class="bi bi-pencil"></i></button
                  ><button
                    class="btn btn-sm btn-ghost"
                    style="color: var(--red-active)"
                    @click="deleteShift(s.id)"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card">
        <h3 style="font-weight: 700; margin-bottom: 16px">Lịch phân ca</h3>
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Nhân viên</th>
                <th>Ca</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>TT</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sc in adminStore.allSchedules" :key="sc.id">
                <td>{{ sc.date }}</td>
                <td>{{ sc.userName }}</td>
                <td>{{ sc.shiftName }}</td>
                <td>{{ sc.checkedInAt ? formatTime(sc.checkedInAt) : '-' }}</td>
                <td>
                  {{ sc.checkedOutAt ? formatTime(sc.checkedOutAt) : '-' }}
                </td>
                <td>
                  <span
                    class="badge"
                    :class="
                      sc.status === 'CHECKED_IN'
                        ? 'badge-success'
                        : sc.status === 'CHECKED_OUT'
                          ? 'badge-secondary'
                          : sc.status === 'ABSENT'
                            ? 'badge-danger'
                            : 'badge-warning'
                    "
                    >{{ SCHEDULE_STATUS_LABEL[sc.status] }}</span
                  >
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <div
      v-if="showShiftForm"
      class="modal-overlay"
      @click.self="showShiftForm = false"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ editingShiftId ? 'Sửa ca' : 'Thêm ca' }}
          </h3>
          <button class="modal-close" @click="showShiftForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="saveShift">
          <div class="form-group">
            <label class="form-label">Tên ca</label
            ><input v-model="shiftForm.name" class="form-input" required />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Bắt đầu</label
              ><input
                v-model="shiftForm.startTime"
                type="time"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">Kết thúc</label
              ><input
                v-model="shiftForm.endTime"
                type="time"
                class="form-input"
              />
            </div>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-outline"
              @click="showShiftForm = false"
            >
              Hủy</button
            ><button type="submit" class="btn btn-primary">
              {{ editingShiftId ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </form>
      </div>
    </div>
    <div
      v-if="showScheduleForm"
      class="modal-overlay"
      @click.self="showScheduleForm = false"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">Phân ca</h3>
          <button class="modal-close" @click="showScheduleForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="saveSchedule">
          <div class="form-group">
            <label class="form-label">Ngày</label
            ><input
              v-model="scheduleForm.date"
              type="date"
              class="form-input"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Nhân viên</label
            ><select v-model="scheduleForm.userId" class="form-select">
              <option
                v-for="u in adminStore.allUsers.filter(
                  (u) => u.roleName === 'STAFF',
                )"
                :key="u.id"
                :value="u.id"
              >
                {{ u.fullName }} ({{ u.roleName }})
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Ca</label
            ><select v-model="scheduleForm.shiftId" class="form-select">
              <option
                v-for="s in adminStore.allShifts"
                :key="s.id"
                :value="s.id"
              >
                {{ s.name }}
              </option>
            </select>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-outline"
              @click="showScheduleForm = false"
            >
              Hủy</button
            ><button type="submit" class="btn btn-primary">Thêm</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
