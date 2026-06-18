<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const form = ref({ name: '', email: '', phone: '' });
const editMode = ref(false);
const success = ref('');

onMounted(() => {
  if (auth.user) {
    form.value = {
      name: auth.user.name,
      email: auth.user.email,
      phone: auth.user.phone || '',
    };
  }
});

async function save() {
  await auth.updateProfile(form.value);
  success.value = 'Cập nhật thành công!';
  editMode.value = false;
  setTimeout(() => (success.value = ''), 3000);
}
</script>

<template>
  <div class="profile-page">
    <div class="card">
      <div class="card-header">
        <h3>Thông tin cá nhân</h3>
        <button
          v-if="!editMode"
          class="btn btn-sm btn-outline"
          @click="editMode = true"
        >
          <i class="bi bi-pencil"></i> Chỉnh sửa
        </button>
      </div>
      <div v-if="success" class="alert alert-success">
        <i class="bi bi-check-circle-fill"></i> {{ success }}
      </div>
      <div class="profile-avatar-section">
        <img
          :src="auth.user?.avatar || 'https://i.pravatar.cc/150?u=default'"
          class="profile-avatar"
        />
        <div>
          <div class="profile-name">{{ auth.user?.name }}</div>
          <div class="profile-role">Thành viên</div>
        </div>
      </div>
      <form @submit.prevent="save">
        <div class="form-group">
          <label class="form-label">Họ tên</label>
          <input v-model="form.name" class="form-input" :disabled="!editMode" />
        </div>
        <div class="form-group">
          <label class="form-label">Email</label>
          <input
            v-model="form.email"
            type="email"
            class="form-input"
            :disabled="!editMode"
          />
        </div>
        <div class="form-group">
          <label class="form-label">Số điện thoại</label>
          <input
            v-model="form.phone"
            type="tel"
            class="form-input"
            :disabled="!editMode"
          />
        </div>
        <div v-if="editMode" class="form-actions">
          <button type="submit" class="btn btn-primary">
            <i class="bi bi-check-lg"></i> Lưu thay đổi
          </button>
          <button
            type="button"
            class="btn btn-outline"
            @click="editMode = false"
          >
            Hủy
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.card-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}
.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 25px;
  object-fit: cover;
}
.profile-name {
  font-size: 18px;
  font-weight: 700;
}
.profile-role {
  font-size: 13px;
  color: var(--text-mid);
}
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.alert-success {
  background: #d4edda;
  color: #155724;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
