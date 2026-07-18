<script setup>
import { ref, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';

const adminStore = useAdminStore();
const showForm = ref(false);
const editingId = ref(null);
const form = ref({ name: '', description: '' });
const saving = ref(false);
const error = ref('');

onMounted(load);

async function load() {
  error.value = '';
  try { await adminStore.fetchCategories(); } catch (e) { error.value = e.message || 'Không thể tải danh mục'; }
}

function openAdd() { editingId.value = null; form.value = { name: '', description: '' }; showForm.value = true; }
function openEdit(category) { editingId.value = category.id; form.value = { name: category.name, description: category.description || '' }; showForm.value = true; }

async function save() {
  if (!form.value.name.trim()) return alert('Nhập tên danh mục');
  saving.value = true;
  try {
    const data = { name: form.value.name.trim(), description: form.value.description.trim() };
    if (editingId.value) await adminStore.updateCategory(editingId.value, data);
    else await adminStore.createCategory(data);
    showForm.value = false;
  } catch (e) { alert(e.message || 'Không thể lưu danh mục'); }
  finally { saving.value = false; }
}

async function remove(category) {
  if (!confirm(`Xóa danh mục "${category.name}"?`)) return;
  try { await adminStore.deleteCategory(category.id); }
  catch (e) { alert(e.message || 'Không thể xóa danh mục có sản phẩm'); }
}
</script>

<template>
  <div>
    <div class="page-header"><div><h1>Quản lý danh mục</h1><p>Phân loại sản phẩm trên thực đơn.</p></div><button class="btn btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm danh mục</button></div>
    <div v-if="error" class="admin-error"><span>{{ error }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <div v-else class="card card-flat">
      <div v-if="adminStore.allCategories.length" class="table-wrapper"><table class="table"><thead><tr><th scope="col">ID</th><th scope="col">Tên</th><th scope="col">Mô tả</th><th scope="col">Số SP</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr></thead><tbody><tr v-for="category in adminStore.allCategories" :key="category.id"><td>{{ category.id }}</td><td><strong>{{ category.name }}</strong></td><td>{{ category.description || '—' }}</td><td>{{ category.productCount }}</td><td class="actions"><button class="btn btn-sm btn-ghost" :aria-label="`Sửa ${category.name}`" @click="openEdit(category)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost text-danger" :aria-label="`Xóa ${category.name}`" @click="remove(category)"><i class="bi bi-trash3"></i></button></td></tr></tbody></table></div>
      <div v-else class="empty-state"><i class="bi bi-tags"></i><h3>Chưa có danh mục</h3><p>Tạo danh mục đầu tiên để phân loại sản phẩm.</p></div>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false"><div class="modal" role="dialog" aria-modal="true" aria-labelledby="category-modal-title"><div class="modal-header"><h2 id="category-modal-title" class="modal-title">{{ editingId ? 'Sửa danh mục' : 'Thêm danh mục' }}</h2><button class="modal-close" aria-label="Đóng" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div><form class="modal-body" @submit.prevent="save"><div class="form-group"><label class="form-label" for="category-name">Tên danh mục *</label><input id="category-name" v-model="form.name" class="form-input" maxlength="100" required></div><div class="form-group"><label class="form-label" for="category-description">Mô tả</label><textarea id="category-description" v-model="form.description" class="form-textarea" rows="3" maxlength="500"></textarea></div><div class="modal-footer"><button type="button" class="btn btn-ghost" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button></div></form></div></div>
  </div>
</template>

<style scoped>
.page-header p { margin: 4px 0 0; color: var(--text-mid); font-size: 14px; }
.actions { display: flex; gap: 4px; }
.admin-error { display: flex; align-items: center; gap: 10px; padding: 14px; border: 1px solid #fecaca; border-radius: var(--radius); color: var(--red-active); background: #fef2f2; }
</style>
