<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { useToast } from '@/stores/toast';

const toast = useToast();
const adminStore = useAdminStore();
const router = useRouter();
const searchTerm = ref('');
const activeFilter = ref('ALL');
const savingId = ref(null);
const draftStock = ref({});

onMounted(async () => {
  await adminStore.fetchProducts();
});

const rows = computed(() => {
  const result = [];
  for (const product of adminStore.allProducts) {
    for (const variant of product.variants || []) {
      const stock = variant.quantityAvailable === null || variant.quantityAvailable === undefined ? null : Number(variant.quantityAvailable);
      result.push({
        productId: product.id,
        productName: product.name,
        categoryName: product.categoryName,
        image: product.image,
        productStatus: product.status,
        variantId: variant.variantId,
        variantName: variant.variantName || 'Mặc định',
        price: Number(variant.price) || 0,
        status: variant.status || 'UNAVAILABLE',
        stock,
      });
    }
  }
  return result;
});

const managedRows = computed(() => rows.value.filter((r) => r.stock !== null));
const outOfStockRows = computed(() => rows.value.filter((r) => r.stock !== null && r.stock <= 0));
const lowStockRows = computed(() => rows.value.filter((r) => r.stock !== null && r.stock > 0 && r.stock <= 5));
const unmanagedRows = computed(() => rows.value.filter((r) => r.stock === null));
const totalStock = computed(() => managedRows.value.reduce((sum, r) => sum + r.stock, 0));

const filteredRows = computed(() => {
  const q = searchTerm.value.trim().toLowerCase();
  return rows.value.filter((r) => {
    const matchesSearch = !q
      || r.productName.toLowerCase().includes(q)
      || r.variantName.toLowerCase().includes(q)
      || (r.categoryName || '').toLowerCase().includes(q);
    if (!matchesSearch) return false;
    if (activeFilter.value === 'OUT') return r.stock !== null && r.stock <= 0;
    if (activeFilter.value === 'LOW') return r.stock !== null && r.stock > 0 && r.stock <= 5;
    if (activeFilter.value === 'UNMANAGED') return r.stock === null;
    if (activeFilter.value === 'UNAVAILABLE') return r.status !== 'AVAILABLE' || r.productStatus !== 'AVAILABLE';
    return true;
  });
});

function getDraft(row) {
  return row.variantId in draftStock.value ? draftStock.value[row.variantId] : row.stock;
}

function setDraft(row, value) {
  draftStock.value[row.variantId] = value === '' || value === null ? null : Number(value);
}

function statusLabel(row) {
  if (row.status !== 'AVAILABLE' || row.productStatus !== 'AVAILABLE') return 'Ngừng bán';
  if (row.stock === null) return 'Không giới hạn';
  if (row.stock <= 0) return 'Hết hàng';
  if (row.stock <= 5) return 'Sắp hết';
  return 'Còn hàng';
}

function statusClass(row) {
  if (row.status !== 'AVAILABLE' || row.productStatus !== 'AVAILABLE') return 'badge-secondary';
  if (row.stock === null) return 'badge-info';
  if (row.stock <= 0) return 'badge-danger';
  if (row.stock <= 5) return 'badge-warning';
  return 'badge-success';
}

async function saveStock(row) {
  const value = getDraft(row);
  if (value !== null && value !== undefined && Number(value) < 0) return toast.error('Tồn kho không được âm');
  savingId.value = row.variantId;
  try {
    await adminStore.updateVariant(row.variantId, {
      quantityAvailable: value === '' || value === undefined ? null : value,
    });
    await adminStore.fetchProducts();
    delete draftStock.value[row.variantId];
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật tồn kho');
  } finally {
    savingId.value = null;
  }
}

function editProduct(row) {
  router.push('/admin/products');
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1>Quản lý tồn kho</h1>
        <p class="page-subtitle">Theo dõi tồn kho từng biến thể sản phẩm</p>
      </div>
      <button class="btn btn-outline" @click="adminStore.fetchProducts()">
        <i class="bi bi-arrow-clockwise"></i> Làm mới
      </button>
    </div>

    <div class="stat-grid inventory-stats">
      <button class="stat-card" :class="{ active: activeFilter === 'ALL' }" @click="activeFilter = 'ALL'">
        <div class="stat-icon" style="background:linear-gradient(135deg,#3b82f6,#60a5fa)"><i class="bi bi-boxes"></i></div>
        <div class="stat-value">{{ rows.length }}</div>
        <div class="stat-label">Tổng biến thể</div>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'LOW' }" @click="activeFilter = 'LOW'">
        <div class="stat-icon" style="background:linear-gradient(135deg,#f59e0b,#fbbf24)"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-value">{{ lowStockRows.length }}</div>
        <div class="stat-label">Sắp hết</div>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'OUT' }" @click="activeFilter = 'OUT'">
        <div class="stat-icon" style="background:linear-gradient(135deg,#ef4444,#f87171)"><i class="bi bi-x-circle"></i></div>
        <div class="stat-value">{{ outOfStockRows.length }}</div>
        <div class="stat-label">Hết hàng</div>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'UNMANAGED' }" @click="activeFilter = 'UNMANAGED'">
        <div class="stat-icon" style="background:linear-gradient(135deg,#06b6d4,#22d3ee)"><i class="bi bi-infinity"></i></div>
        <div class="stat-value">{{ unmanagedRows.length }}</div>
        <div class="stat-label">Chưa quản lý</div>
      </button>
      <div class="stat-card">
        <div class="stat-icon" style="background:linear-gradient(135deg,#10b981,#34d399)"><i class="bi bi-stack"></i></div>
        <div class="stat-value">{{ totalStock }}</div>
        <div class="stat-label">Tổng tồn đang quản lý</div>
      </div>
    </div>

    <div class="card card-flat">
      <div class="toolbar">
        <div class="search-box">
          <i class="bi bi-search"></i>
          <input v-model="searchTerm" class="form-input" placeholder="Tìm món, biến thể, danh mục..." />
        </div>
        <select v-model="activeFilter" class="form-select filter-select">
          <option value="ALL">Tất cả</option>
          <option value="LOW">Sắp hết</option>
          <option value="OUT">Hết hàng</option>
          <option value="UNMANAGED">Chưa quản lý tồn</option>
          <option value="UNAVAILABLE">Ngừng bán</option>
        </select>
      </div>

      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Biến thể</th>
              <th>Giá</th>
              <th>Trạng thái</th>
              <th style="width: 220px">Tồn kho</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.variantId">
              <td>
                <div class="product-cell">
                  <img :src="row.image" :alt="row.productName" />
                  <div>
                    <strong>{{ row.productName }}</strong>
                    <div class="muted">{{ row.categoryName || 'Chưa phân loại' }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.variantName }}</td>
              <td>{{ formatPrice(row.price) }}</td>
              <td><span class="badge" :class="statusClass(row)">{{ statusLabel(row) }}</span></td>
              <td>
                <div class="stock-edit">
                  <input
                    class="form-input"
                    type="number"
                    min="0"
                    :value="getDraft(row)"
                    placeholder="Trống = không giới hạn"
                    @input="setDraft(row, $event.target.value)"
                  />
                  <button class="btn btn-sm btn-primary" :disabled="savingId === row.variantId" @click="saveStock(row)">
                    <i v-if="savingId === row.variantId" class="bi bi-arrow-repeat spin"></i>
                    <span v-else>Lưu</span>
                  </button>
                </div>
              </td>
              <td>
                <button class="btn btn-sm btn-ghost" @click="editProduct(row)">
                  <i class="bi bi-pencil"></i>
                </button>
              </td>
            </tr>
            <tr v-if="filteredRows.length === 0">
              <td colspan="6" class="empty-row">Không có dữ liệu tồn kho</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--text-mid);
  font-size: 14px;
}
.inventory-stats .stat-card {
  text-align: left;
  border: 1px solid var(--border-light);
  cursor: pointer;
  background: #fff;
}
.inventory-stats .stat-card.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-50);
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.search-box { max-width: 420px; flex: 1; }
.filter-select { width: 200px; }
.product-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}
.product-cell img {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--surface);
}
.muted {
  color: var(--text-mid);
  font-size: 12px;
  margin-top: 2px;
}
.stock-edit {
  display: flex;
  gap: 8px;
  align-items: center;
}
.stock-edit .form-input { min-width: 140px; }
.empty-row {
  text-align: center;
  color: var(--text-mid);
  padding: 32px;
}
@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: stretch; }
  .filter-select { width: 100%; }
}
</style>
