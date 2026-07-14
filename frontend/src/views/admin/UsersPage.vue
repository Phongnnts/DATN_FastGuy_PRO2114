<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';

const adminStore = useAdminStore();
const searchTerm = ref('');
const showForm = ref(false);
const editingId = ref(null);
const form = ref({ fullName: '', email: '', phone: '', password: '', roleName: 'USER' });
const success = ref('');
const loadError = ref('');

async function load() {
  loadError.value = '';
  try { await adminStore.fetchUsers(); }
  catch (e) { loadError.value = e.message || 'Không thể tải người dùng'; }
}

onMounted(load);

const filtered = computed(() => {
  if (!searchTerm.value) return adminStore.allUsers;
  const q = searchTerm.value.toLowerCase();
  return adminStore.allUsers.filter(
    (u) => (u.fullName || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q),
  );
});

function openAdd() {
  editingId.value = null;
  form.value = { fullName: '', email: '', phone: '', password: '', roleName: 'USER' };
  showForm.value = true;
}

function openEdit(u) {
  editingId.value = u.userId;
  form.value = { fullName: u.fullName, email: u.email, phone: u.phone || '', password: '', roleName: u.roleName || 'USER' };
  showForm.value = true;
}

async function save() {
  try {
    if (editingId.value) {
      const data = { fullName: form.value.fullName, email: form.value.email, phone: form.value.phone, roleName: form.value.roleName };
      if (form.value.password) data.password = form.value.password;
      await adminStore.updateUser(editingId.value, data);
      success.value = 'Cập nhật thành công!';
    } else {
      await adminStore.createUser({ ...form.value });
      success.value = 'Thêm người dùng thành công!';
    }
    showForm.value = false;
    await adminStore.fetchUsers();
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    alert(e.message || 'Lỗi khi lưu');
  }
}

async function deleteUser(u) {
  if (!confirm(`Xóa người dùng "${u.fullName}"?`)) return;
  try {
    await adminStore.deleteUser(u.userId);
    success.value = 'Đã xóa!';
    await adminStore.fetchUsers();
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    alert(e.message || 'Lỗi khi xóa');
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý người dùng</h1>
      <button class="btn btn-sm btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm</button>
    </div>
    <div v-if="success" class="alert alert-success" style="margin-bottom:12px">{{ success }}</div>
    <div v-if="loadError" class="admin-error"><span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <div v-else class="card card-flat">
      <div class="search-box" style="margin-bottom:16px;max-width:320px">
        <i class="bi bi-search"></i>
        <input v-model="searchTerm" class="form-input" placeholder="Tìm tên hoặc email..." />
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Vai trò</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filtered" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td><strong>{{ u.fullName }}</strong></td>
              <td>{{ u.email }}</td>
              <td>{{ u.phone }}</td>
              <td><span class="badge" v-text="u.roleName"></span></td>
              <td>
                <button class="btn btn-sm btn-outline" style="margin-right:4px" @click="openEdit(u)"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline" style="border-color:var(--red-active);color:var(--red-active)" @click="deleteUser(u)"><i class="bi bi-trash3"></i></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="filtered.length === 0" class="empty-state" style="padding:20px">Không có dữ liệu</p>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editingId ? 'Sửa người dùng' : 'Thêm người dùng' }}</h3>
          <button class="btn btn-sm btn-outline" @click="showForm = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Họ tên</label>
            <input v-model="form.fullName" class="form-input" required />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input v-model="form.email" type="email" class="form-input" required />
          </div>
          <div class="form-group">
            <label class="form-label">Số điện thoại</label>
            <input v-model="form.phone" class="form-input" />
          </div>
          <div class="form-group" v-if="!editingId">
            <label class="form-label">Mật khẩu</label>
            <input v-model="form.password" type="password" class="form-input" required />
          </div>
          <div class="form-group" v-if="editingId">
            <label class="form-label">Mật khẩu mới (để trống nếu không đổi)</label>
            <input v-model="form.password" type="password" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">Vai trò</label>
            <select v-model="form.roleName" class="form-select">
              <option value="USER">USER</option>
              <option value="STAFF">STAFF</option>
              <option value="SHIPPER">SHIPPER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showForm = false">Hủy</button>
          <button class="btn btn-primary" @click="save">{{ editingId ? 'Cập nhật' : 'Thêm' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-error { display:flex; align-items:center; gap:10px; padding:14px; border:1px solid #fecaca; border-radius:var(--radius); color:var(--red-active); background:#fef2f2; }
</style>
