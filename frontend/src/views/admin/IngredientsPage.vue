<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { formatPrice } from '@/utils/format'
import { INGREDIENT_UNITS } from '@/utils/constants'

const adminStore = useAdminStore()
const showForm = ref(false)

onMounted(() => adminStore.fetchIngredients())
const editingId = ref(null)
const form = ref({ name: '', unit: 'kg', currentStock: 0, minStock: 0, maxStock: 0, price: 0 })

function openAdd() {
  editingId.value = null; form.value = { name: '', unit: 'kg', currentStock: 0, minStock: 0, maxStock: 0, price: 0 }; showForm.value = true
}

function openEdit(i) {
  editingId.value = i.id; form.value = { name: i.name, unit: i.unit, currentStock: i.currentStock, minStock: i.minStock, maxStock: i.maxStock, price: i.price }; showForm.value = true
}

function save() {
  if (editingId.value) adminStore.updateIngredient(editingId.value, form.value)
  else adminStore.createIngredient(form.value)
  showForm.value = false
}

function remove(id) { if (confirm('Xóa nguyên liệu?')) adminStore.deleteIngredient(id) }
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý nguyên liệu</h1>
      <button class="btn btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm nguyên liệu</button>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Tên</th><th>ĐVT</th><th>Tồn kho</th><th>Tối thiểu</th><th>Tối đa</th><th>Đơn giá</th><th>Mức</th><th></th></tr></thead>
          <tbody>
            <tr v-for="i in adminStore.allIngredients" :key="i.id">
              <td><strong>{{ i.name }}</strong></td><td>{{ i.unit }}</td>
              <td>{{ i.currentStock }}</td><td>{{ i.minStock }}</td><td>{{ i.maxStock }}</td><td>{{ formatPrice(i.price) }}</td>
              <td>
                <span class="badge" :class="i.currentStock <= i.minStock ? 'badge-danger' : i.currentStock <= i.maxStock * 0.5 ? 'badge-warning' : 'badge-success'">
                  {{ i.currentStock <= i.minStock ? 'Thấp' : i.currentStock <= i.maxStock * 0.5 ? 'TB' : 'Tốt' }}
                </span>
              </td>
              <td><button class="btn btn-sm btn-ghost" @click="openEdit(i)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="remove(i.id)"><i class="bi bi-trash3"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header"><h3 class="modal-title">{{ editingId ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu' }}</h3><button class="modal-close" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <form @submit.prevent="save">
          <div class="grid-2">
            <div class="form-group"><label class="form-label">Tên</label><input v-model="form.name" class="form-input" required /></div>
            <div class="form-group"><label class="form-label">ĐVT</label><select v-model="form.unit" class="form-select"><option v-for="u in INGREDIENT_UNITS" :key="u" :value="u">{{ u }}</option></select></div>
          </div>
          <div class="grid-3">
            <div class="form-group"><label class="form-label">Tồn kho</label><input v-model.number="form.currentStock" type="number" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Tối thiểu</label><input v-model.number="form.minStock" type="number" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Tối đa</label><input v-model.number="form.maxStock" type="number" class="form-input" /></div>
          </div>
          <div class="form-group"><label class="form-label">Đơn giá</label><input v-model.number="form.price" type="number" class="form-input" /></div>
          <div class="modal-footer"><button type="button" class="btn btn-outline" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary">{{ editingId ? 'Cập nhật' : 'Thêm mới' }}</button></div>
        </form>
      </div>
    </div>
  </div>
</template>
