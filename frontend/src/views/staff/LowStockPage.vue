<script setup>
import { useStaffStore } from '@/stores/staff'
import { formatPrice, formatDate } from '@/utils/format'

const staffStore = useStaffStore()
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Nguyên liệu tồn kho thấp</h1>
      <p>Các nguyên liệu cần nhập thêm</p>
    </div>
    <div class="stat-grid">
      <div class="stat-card" style="border:2px solid var(--red-active)">
        <div class="stat-icon" style="background:#F8D7DA;color:var(--red-active)"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-value">{{ staffStore.lowStockIngredients.length }}</div>
        <div class="stat-label">Nguyên liệu sắp hết</div>
      </div>
    </div>
    <div class="card card-flat">
      <div v-if="staffStore.lowStockIngredients.length === 0" class="empty-state" style="padding:40px">
        <i class="bi bi-check-circle-fill" style="color:#4CAF50;font-size:48px"></i>
        <h3>Tồn kho ổn định</h3>
        <p>Không có nguyên liệu nào dưới mức tối thiểu</p>
      </div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead><tr><th>Nguyên liệu</th><th>ĐVT</th><th>Tồn kho</th><th>Tối thiểu</th><th>Cần nhập thêm</th><th>Đơn giá</th><th></th></tr></thead>
          <tbody>
            <tr v-for="ing in staffStore.lowStockIngredients" :key="ing.id" style="background:#FFF5F5">
              <td><strong>{{ ing.name }}</strong></td>
              <td>{{ ing.unit }}</td>
              <td style="color:var(--red-active);font-weight:700">{{ ing.currentStock }}</td>
              <td>{{ ing.minStock }}</td>
              <td>{{ ing.minStock - ing.currentStock }} {{ ing.unit }}</td>
              <td>{{ formatPrice(ing.price) }}</td>
              <td><router-link :to="'/staff/ingredients'" class="btn btn-sm btn-primary"><i class="bi bi-plus-lg"></i> Nhập kho</router-link></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
