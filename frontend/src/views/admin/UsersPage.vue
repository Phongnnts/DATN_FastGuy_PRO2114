<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';

const adminStore = useAdminStore();
const searchTerm = ref('');

onMounted(() => {
  adminStore.fetchUsers();
});

const filtered = computed(() => {
  if (!searchTerm.value) return adminStore.allUsers;
  const q = searchTerm.value.toLowerCase();
  return adminStore.allUsers.filter(
    (u) =>
      (u.fullName || '').toLowerCase().includes(q) ||
      (u.email || '').toLowerCase().includes(q),
  );
});
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý người dùng</h1>
    </div>
    <div class="card card-flat">
      <div class="search-box" style="margin-bottom: 16px; max-width: 320px">
        <i class="bi bi-search"></i>
        <input
          v-model="searchTerm"
          class="form-input"
          placeholder="Tìm tên hoặc email..."
        />
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Họ tên</th>
              <th>Email</th>
              <th>SĐT</th>
              <th>Vai trò</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filtered" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td>
                <strong>{{ u.fullName }}</strong>
              </td>
              <td>{{ u.email }}</td>
              <td>{{ u.phone }}</td>
              <td><span class="badge" v-text="u.roleName"></span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="filtered.length === 0" class="empty-state" style="padding: 20px">
        Không có dữ liệu
      </p>
    </div>
  </div>
</template>
