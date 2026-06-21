<script setup>
import { ref, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';

const adminStore = useAdminStore();
const showForm = ref(false);
const editingId = ref(null);
const form = ref({ name: '', description: '' });

onMounted(() => adminStore.fetchCategories());

function openAdd() {
  editingId.value = null;
  form.value = { name: '', description: '' };
  showForm.value = true;
}

function openEdit(c) {
  editingId.value = c.id;
  form.value = { name: c.name, description: c.description || '' };
  showForm.value = true;
}

function save() {
  if (editingId.value) adminStore.updateCategory(editingId.value, form.value);
  else adminStore.createCategory(form.value);
  showForm.value = false;
}

function remove(id) {
  if (confirm('Xóa danh mục?')) adminStore.deleteCategory(id);
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý danh mục</h1>
      <button class="btn btn-primary" @click="openAdd">
        <i class="bi bi-plus-lg"></i> Thêm danh mục
      </button>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên</th>
              <th>Mô tả</th>
              <th>Số SP</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in adminStore.allCategories" :key="c.id">
              <td>{{ c.id }}</td>
              <td>
                <strong>{{ c.name }}</strong>
              </td>
              <td>{{ c.description }}</td>
              <td>{{ c.productCount }}</td>
              <td>
                <button class="btn btn-sm btn-ghost" @click="openEdit(c)">
                  <i class="bi bi-pencil"></i></button
                ><button
                  class="btn btn-sm btn-ghost"
                  style="color: var(--red-active)"
                  @click="remove(c.id)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ editingId ? 'Sửa danh mục' : 'Thêm danh mục' }}
          </h3>
          <button class="modal-close" @click="showForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="save">
          <div class="form-group">
            <label class="form-label">Tên danh mục</label
            ><input v-model="form.name" class="form-input" required />
          </div>
          <div class="form-group">
            <label class="form-label">Mô tả</label
            ><textarea
              v-model="form.description"
              class="form-textarea"
              rows="2"
            ></textarea>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-outline"
              @click="showForm = false"
            >
              Hủy</button
            ><button type="submit" class="btn btn-primary">
              {{ editingId ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
