<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { formatPrice } from '@/utils/format'

const adminStore = useAdminStore()
const showForm = ref(false)

onMounted(() => adminStore.fetchZones())
const editingId = ref(null)
const form = ref({ name: '', type: 'Quận', price: 0, estimatedTime: '', isActive: true })

function openAdd() {
  editingId.value = null
  form.value = { name: '', type: 'Quận', price: 0, estimatedTime: '', isActive: true }
  showForm.value = true
}

function openEdit(z) {
  editingId.value = z.id
  form.value = { name: z.name, type: z.type, price: z.price, estimatedTime: z.estimatedTime, isActive: z.isActive }
  showForm.value = true
}

function save() {
  if (editingId.value) adminStore.updateZone(editingId.value, form.value)
  else adminStore.createZone(form.value)
  showForm.value = false
}

function remove(id) { if (confirm('Xóa khu vực?')) adminStore.deleteZone(id) }
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Khu vực giao hàng</h1>
      <button class="btn btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm khu vực</button>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Tên</th><th>Loại</th><th>Phí ship</th><th>Thời gian</th><th>Kích hoạt</th><th></th></tr></thead>
          <tbody>
            <tr v-for="z in adminStore.allZones" :key="z.id">
              <td><strong>{{ z.name }}</strong></td><td>{{ z.type }}</td>
              <td>{{ z.price === 0 ? 'Miễn phí' : formatPrice(z.price) }}</td><td>{{ z.estimatedTime }}</td>
              <td><i :class="z.isActive ? 'bi bi-check-circle-fill' : 'bi bi-x-circle-fill'" :style="{ color: z.isActive ? '#4CAF50' : 'var(--red-active)' }"></i></td>
              <td><button class="btn btn-sm btn-ghost" @click="openEdit(z)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="remove(z.id)"><i class="bi bi-trash3"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header"><h3 class="modal-title">{{ editingId ? 'Sửa khu vực' : 'Thêm khu vực' }}</h3><button class="modal-close" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <form @submit.prevent="save">
          <div class="grid-2">
            <div class="form-group"><label class="form-label">Tên</label><input v-model="form.name" class="form-input" required /></div>
            <div class="form-group"><label class="form-label">Loại</label><select v-model="form.type" class="form-select"><option>Quận</option><option>Huyện</option><option>Phường</option><option>Xã</option></select></div>
          </div>
          <div class="grid-2">
            <div class="form-group"><label class="form-label">Phí ship (₫)</label><input v-model.number="form.price" type="number" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Thời gian</label><input v-model="form.estimatedTime" class="form-input" placeholder="VD: 15-25 phút" /></div>
          </div>
          <div class="form-group"><label class="form-label">Kích hoạt</label><select v-model="form.isActive" class="form-select"><option :value="true">Hoạt động</option><option :value="false">Tắt</option></select></div>
          <div class="modal-footer"><button type="button" class="btn btn-outline" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary">{{ editingId ? 'Cập nhật' : 'Thêm mới' }}</button></div>
        </form>
      </div>
    </div>
  </div>
</template>
