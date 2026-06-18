<script setup>
import { ref } from 'vue'
import { useStaffStore } from '@/stores/staff'
import { formatPrice, formatDate } from '@/utils/format'

const staffStore = useStaffStore()
const showStockIn = ref(false)
const stockInIngredient = ref(null)
const stockInQty = ref(0)
const stockInNote = ref('')

function openStockIn(ing) {
  stockInIngredient.value = ing
  stockInQty.value = 0
  stockInNote.value = ''
  showStockIn.value = true
}

function confirmStockIn() {
  if (stockInQty.value <= 0) return
  staffStore.stockIn(stockInIngredient.value.id, stockInQty.value, stockInNote.value)
  showStockIn.value = false
}

function getStockLevel(ing) {
  const ratio = ing.currentStock / ing.maxStock
  if (ratio <= 0.2) return { class: 'low', label: 'Thấp' }
  if (ratio <= 0.5) return { class: 'medium', label: 'TB' }
  return { class: 'high', label: 'Tốt' }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Nguyên liệu</h1>
      <router-link to="/staff/ingredients/low-stock" class="btn btn-sm btn-danger"><i class="bi bi-exclamation-triangle"></i> Tồn kho thấp</router-link>
    </div>
    <div class="card card-flat">
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Nguyên liệu</th><th>ĐVT</th><th>Tồn kho</th><th>Tối thiểu</th><th>Tối đa</th><th>Đơn giá</th><th>Nhập gần nhất</th><th>Mức</th><th></th></tr></thead>
          <tbody>
            <tr v-for="ing in staffStore.allIngredients" :key="ing.id">
              <td><strong>{{ ing.name }}</strong></td>
              <td>{{ ing.unit }}</td>
              <td>{{ ing.currentStock }}</td>
              <td>{{ ing.minStock }}</td>
              <td>{{ ing.maxStock }}</td>
              <td>{{ formatPrice(ing.price) }}</td>
              <td>{{ formatDate(ing.lastRestocked) }}</td>
              <td><span class="badge" :class="'badge-' + (getStockLevel(ing).class === 'low' ? 'danger' : getStockLevel(ing).class === 'medium' ? 'warning' : 'success')">{{ getStockLevel(ing).label }}</span></td>
              <td><button class="btn btn-sm btn-primary" @click="openStockIn(ing)"><i class="bi bi-plus-lg"></i> Nhập</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="showStockIn" class="modal-overlay" @click.self="showStockIn = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">Nhập kho: {{ stockInIngredient?.name }}</h3>
          <button class="modal-close" @click="showStockIn = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="form-group">
          <label class="form-label">Số lượng ({{ stockInIngredient?.unit }})</label>
          <input v-model.number="stockInQty" type="number" class="form-input" min="1" />
        </div>
        <div class="form-group">
          <label class="form-label">Ghi chú</label>
          <input v-model="stockInNote" class="form-input" placeholder="VD: Nhập từ nhà cung cấp A" />
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showStockIn = false">Hủy</button>
          <button class="btn btn-primary" @click="confirmStockIn" :disabled="stockInQty <= 0"><i class="bi bi-check-lg"></i> Xác nhận nhập</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.badge-low { background: #F8D7DA; color: #721C24; }
.badge-medium { background: #FFF3CD; color: #856404; }
.badge-high { background: #D4EDDA; color: #155724; }
</style>
