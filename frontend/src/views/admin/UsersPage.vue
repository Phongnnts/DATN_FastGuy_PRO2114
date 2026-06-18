<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'

const adminStore = useAdminStore()
const searchTerm = ref('')
const showForm = ref(false)
const editingId = ref(null)
const form = ref({ name: '', email: '', phone: '', password: '', role: 'USER' })

onMounted(() => { adminStore.fetchUsers() })

function openAdd() {
  editingId.value = null
  form.value = { name: '', email: '', phone: '', password: '', role: 'USER' }
  showForm.value = true
}

function openEdit(u) {
  editingId.value = u.id
  form.value = { name: u.name, email: u.email, phone: u.phone, password: '', role: u.role }
  showForm.value = true
}

async function save() {
  if (editingId.value) {
    await adminStore.updateUser(editingId.value, { name: form.value.name, email: form.value.email, phone: form.value.phone, role: form.value.role })
  } else {
    await adminStore.createUser(form.value)
  }
  showForm.value = false
}

async function remove(id) {
  if (confirm('Xóa người dùng này?')) await adminStore.deleteUser(id)
}

const filtered = computed(() => {
  if (!searchTerm.value) return adminStore.allUsers
  const q = searchTerm.value.toLowerCase()
  return adminStore.allUsers.filter(u => (u.name || u.fullName || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q))
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý người dùng</h1>
      <button class="btn btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm người dùng</button>
    </div>
    <div class="card card-flat">
      <div class="search-box" style="margin-bottom:16px;max-width:320px">
        <i class="bi bi-search"></i>
        <input v-model="searchTerm" class="form-input" placeholder="Tìm tên hoặc email..." />
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>ID</th><th>Tên</th><th>Email</th><th>SĐT</th><th>Vai trò</th><th></th></tr></thead>
          <tbody>
            <tr v-for="u in filtered" :key="u.id || u.userId">
              <td>{{ u.id || u.userId }}</td>
              <td><strong>{{ u.name || u.fullName }}</strong></td>
              <td>{{ u.email }}</td>
              <td>{{ u.phone }}</td>
              <td><span class="badge" v-text="u.role || u.roleName"></span></td>
              <td><button class="btn btn-sm btn-ghost" @click="openEdit(u)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="remove(u.id || u.userId)"><i class="bi bi-trash3"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="filtered.length === 0" class="empty-state" style="padding:20px">Không có dữ liệu</p>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ editingId ? 'Sửa người dùng' : 'Thêm người dùng' }}</h3>
          <button class="modal-close" @click="showForm = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <form @submit.prevent="save">
          <div class="form-group"><label class="form-label">Tên</label><input v-model="form.name" class="form-input" required /></div>
          <div class="form-group"><label class="form-label">Email</label><input v-model="form.email" type="email" class="form-input" required /></div>
          <div class="form-group"><label class="form-label">SĐT</label><input v-model="form.phone" class="form-input" /></div>
          <div class="form-group" v-if="!editingId"><label class="form-label">Mật khẩu</label><input v-model="form.password" type="password" class="form-input" required /></div>
          <div class="form-group"><label class="form-label">Vai trò</label><select v-model="form.role" class="form-select"><option value="USER">User</option><option value="STAFF">Staff</option><option value="SHIPPER">Shipper</option><option value="ADMIN">Admin</option></select></div>
          <div class="modal-footer"><button type="button" class="btn btn-outline" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary">{{ editingId ? 'Cập nhật' : 'Thêm mới' }}</button></div>
        </form>
      </div>
    </div>
  </div>
</template>
