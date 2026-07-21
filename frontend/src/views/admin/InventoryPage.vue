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
const categoryFilter = ref('ALL');
const sortBy = ref('product-asc');
const savingId = ref(null);
const draftStock = ref({});
const loading = ref(true);
const loadError = ref('');

async function loadProducts() {
  loading.value = true;
  loadError.value = '';
  try {
    await adminStore.fetchProducts();
  } catch (error) {
    loadError.value = error.message || 'Không thể tải dữ liệu tồn kho';
  } finally {
    loading.value = false;
  }
}

onMounted(loadProducts);

const rows = computed(() => adminStore.allProducts.flatMap((product) => (product.variants || []).map((variant) => ({
  productId: product.id,
  productName: product.name,
  categoryId: product.categoryId,
  categoryName: product.categoryName,
  image: product.image,
  productStatus: product.status,
  variantId: variant.variantId,
  variantName: variant.variantName || 'Mặc định',
  sku: variant.sku || variant.SKU || '',
  price: Number(variant.price) || 0,
  status: variant.status || 'UNAVAILABLE',
  stock: variant.quantityAvailable === null || variant.quantityAvailable === undefined ? null : Number(variant.quantityAvailable),
}))));

const categories = computed(() => [...new Map(rows.value.map((row) => [String(row.categoryId ?? row.categoryName), {
  id: String(row.categoryId ?? row.categoryName),
  name: row.categoryName || 'Chưa phân loại',
}])).values()].sort((a, b) => a.name.localeCompare(b.name, 'vi')));
const managedRows = computed(() => rows.value.filter((row) => row.stock !== null));
const outOfStockRows = computed(() => managedRows.value.filter((row) => row.stock <= 0));
const lowStockRows = computed(() => managedRows.value.filter((row) => row.stock > 0 && row.stock <= 5));
const unmanagedRows = computed(() => rows.value.filter((row) => row.stock === null));
const totalStock = computed(() => managedRows.value.reduce((sum, row) => sum + row.stock, 0));

const filteredRows = computed(() => {
  const query = searchTerm.value.trim().toLocaleLowerCase('vi');
  const result = rows.value.filter((row) => {
    const searchable = [row.productName, row.variantName, row.categoryName, row.sku].join(' ').toLocaleLowerCase('vi');
    const categoryId = String(row.categoryId ?? row.categoryName);
    if (query && !searchable.includes(query)) return false;
    if (categoryFilter.value !== 'ALL' && categoryFilter.value !== categoryId) return false;
    if (activeFilter.value === 'OUT') return row.stock !== null && row.stock <= 0;
    if (activeFilter.value === 'LOW') return row.stock !== null && row.stock > 0 && row.stock <= 5;
    if (activeFilter.value === 'UNMANAGED') return row.stock === null;
    if (activeFilter.value === 'UNAVAILABLE') return row.status !== 'AVAILABLE' || row.productStatus !== 'AVAILABLE';
    return true;
  });
  const [field, direction] = sortBy.value.split('-');
  const multiplier = direction === 'desc' ? -1 : 1;
  return result.sort((a, b) => {
    if (field === 'stock') {
      if (a.stock === null) return 1;
      if (b.stock === null) return -1;
      return (a.stock - b.stock) * multiplier;
    }
    if (field === 'variant') return a.variantName.localeCompare(b.variantName, 'vi') * multiplier;
    if (field === 'category') return (a.categoryName || '').localeCompare(b.categoryName || '', 'vi') * multiplier;
    return a.productName.localeCompare(b.productName, 'vi') * multiplier;
  });
});

function getDraft(row) {
  return Object.hasOwn(draftStock.value, row.variantId) ? draftStock.value[row.variantId] : row.stock;
}

function setDraft(row, value) {
  draftStock.value[row.variantId] = value === '' ? null : value;
}

function normalizedDraft(row) {
  const value = getDraft(row);
  if (value === null) return null;
  const number = Number(value);
  return Number.isFinite(number) && Number.isInteger(number) && number >= 0 ? number : undefined;
}

function isChanged(row) {
  const value = normalizedDraft(row);
  return value !== undefined && value !== row.stock;
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
  const value = normalizedDraft(row);
  if (value === undefined) return toast.error('Tồn kho phải là số nguyên không âm hoặc để trống');
  savingId.value = row.variantId;
  try {
    await adminStore.updateVariant(row.variantId, { quantityAvailable: value });
    delete draftStock.value[row.variantId];
    toast.success('Đã cập nhật tồn kho');
  } catch (error) {
    toast.error(error.message || 'Không thể cập nhật tồn kho');
  } finally {
    savingId.value = null;
  }
}

function editProduct(row) {
  router.push(`/admin/products?edit=${row.productId}`);
}
</script>

<template>
  <main class="inventory-page">
    <header class="page-header">
      <div>
        <h1>Quản lý tồn kho</h1>
        <p class="page-subtitle">Theo dõi và cập nhật tồn kho từng biến thể</p>
      </div>
      <button class="btn btn-outline" :disabled="loading" @click="loadProducts">
        <i class="bi bi-arrow-clockwise" :class="{ spin: loading }" aria-hidden="true"></i> Làm mới
      </button>
    </header>

    <section class="stat-grid inventory-stats" aria-label="Tổng quan tồn kho">
      <button class="stat-card" :class="{ active: activeFilter === 'ALL' }" :aria-pressed="activeFilter === 'ALL'" @click="activeFilter = 'ALL'">
        <span class="stat-icon stat-blue"><i class="bi bi-boxes" aria-hidden="true"></i></span><strong class="stat-value">{{ rows.length }}</strong><span class="stat-label">Tổng biến thể</span>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'LOW' }" :aria-pressed="activeFilter === 'LOW'" @click="activeFilter = 'LOW'">
        <span class="stat-icon stat-yellow"><i class="bi bi-exclamation-triangle" aria-hidden="true"></i></span><strong class="stat-value">{{ lowStockRows.length }}</strong><span class="stat-label">Sắp hết</span>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'OUT' }" :aria-pressed="activeFilter === 'OUT'" @click="activeFilter = 'OUT'">
        <span class="stat-icon stat-red"><i class="bi bi-x-circle" aria-hidden="true"></i></span><strong class="stat-value">{{ outOfStockRows.length }}</strong><span class="stat-label">Hết hàng</span>
      </button>
      <button class="stat-card" :class="{ active: activeFilter === 'UNMANAGED' }" :aria-pressed="activeFilter === 'UNMANAGED'" @click="activeFilter = 'UNMANAGED'">
        <span class="stat-icon stat-cyan"><i class="bi bi-infinity" aria-hidden="true"></i></span><strong class="stat-value">{{ unmanagedRows.length }}</strong><span class="stat-label">Không giới hạn</span>
      </button>
      <div class="stat-card stat-total">
        <span class="stat-icon stat-green"><i class="bi bi-stack" aria-hidden="true"></i></span><strong class="stat-value">{{ totalStock }}</strong><span class="stat-label">Tổng đơn vị tồn</span>
      </div>
    </section>

    <section class="card card-flat" aria-label="Danh sách tồn kho">
      <div class="toolbar">
        <label class="search-box">
          <span class="sr-only">Tìm kiếm tồn kho</span><i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="searchTerm" class="form-input" type="search" placeholder="Tìm sản phẩm, biến thể, SKU, danh mục..." />
        </label>
        <div class="filters">
          <label><span class="sr-only">Danh mục</span><select v-model="categoryFilter" class="form-select"><option value="ALL">Mọi danh mục</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
          <label><span class="sr-only">Trạng thái kho</span><select v-model="activeFilter" class="form-select"><option value="ALL">Mọi trạng thái</option><option value="LOW">Sắp hết</option><option value="OUT">Hết hàng</option><option value="UNMANAGED">Không giới hạn</option><option value="UNAVAILABLE">Ngừng bán</option></select></label>
          <label><span class="sr-only">Sắp xếp</span><select v-model="sortBy" class="form-select"><option value="product-asc">Sản phẩm A–Z</option><option value="product-desc">Sản phẩm Z–A</option><option value="variant-asc">Biến thể A–Z</option><option value="category-asc">Danh mục A–Z</option><option value="stock-asc">Tồn kho tăng dần</option><option value="stock-desc">Tồn kho giảm dần</option></select></label>
        </div>
      </div>
      <div class="result-count" aria-live="polite">{{ filteredRows.length }} / {{ rows.length }} biến thể</div>

      <div v-if="loading" class="state-panel" role="status"><i class="bi bi-arrow-repeat spin" aria-hidden="true"></i><span>Đang tải dữ liệu tồn kho...</span></div>
      <div v-else-if="loadError" class="state-panel error-panel" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ loadError }}</strong><button class="btn btn-outline btn-sm" @click="loadProducts">Thử lại</button></div>
      <div v-else-if="filteredRows.length === 0" class="state-panel"><i class="bi bi-inbox" aria-hidden="true"></i><strong>Không tìm thấy biến thể</strong><span>Điều chỉnh từ khóa hoặc bộ lọc.</span></div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead><tr><th scope="col">Sản phẩm</th><th scope="col">Biến thể</th><th scope="col">Giá</th><th scope="col">Trạng thái</th><th scope="col">Tồn kho</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr></thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.variantId">
              <td data-label="Sản phẩm"><div class="product-cell"><img :src="row.image" :alt="row.productName" loading="lazy" /><div><strong>{{ row.productName }}</strong><div class="muted">{{ row.categoryName || 'Chưa phân loại' }}</div></div></div></td>
              <td data-label="Biến thể"><strong>{{ row.variantName }}</strong><div v-if="row.sku" class="muted">SKU: {{ row.sku }}</div></td>
              <td data-label="Giá">{{ formatPrice(row.price) }}</td>
              <td data-label="Trạng thái"><span class="badge" :class="statusClass(row)">{{ statusLabel(row) }}</span></td>
              <td data-label="Tồn kho"><div class="stock-edit"><label><span class="sr-only">Tồn kho {{ row.productName }} - {{ row.variantName }}</span><input class="form-input" type="number" min="0" step="1" :value="getDraft(row)" :aria-invalid="normalizedDraft(row) === undefined" placeholder="Không giới hạn" @input="setDraft(row, $event.target.value)" /></label><button class="btn btn-sm btn-primary" :disabled="savingId === row.variantId || !isChanged(row)" @click="saveStock(row)"><i v-if="savingId === row.variantId" class="bi bi-arrow-repeat spin" aria-hidden="true"></i><span v-else>Lưu</span></button></div></td>
              <td data-label="Thao tác"><button class="btn btn-sm btn-ghost" :aria-label="`Sửa sản phẩm ${row.productName}`" @click="editProduct(row)"><i class="bi bi-pencil" aria-hidden="true"></i></button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<style scoped>
.inventory-page { display: grid; gap: 24px; }
.page-subtitle { margin: 4px 0 0; color: var(--text-mid); font-size: 14px; }
.inventory-stats .stat-card { text-align: left; border: 1px solid var(--border-light); background: #fff; }
.inventory-stats button.stat-card { cursor: pointer; }
.inventory-stats button.stat-card:hover { border-color: var(--primary); transform: translateY(-1px); }
.inventory-stats .stat-card.active { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-50); }
.stat-icon { color: #fff; }
.stat-blue { background: linear-gradient(135deg,#2563eb,#60a5fa); }
.stat-yellow { background: linear-gradient(135deg,#d97706,#fbbf24); }
.stat-red { background: linear-gradient(135deg,#dc2626,#f87171); }
.stat-cyan { background: linear-gradient(135deg,#0891b2,#22d3ee); }
.stat-green { background: linear-gradient(135deg,#059669,#34d399); }
.stat-total { cursor: default; }
.toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; }
.search-box { position: relative; max-width: 440px; flex: 1; }
.search-box i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-mid); }
.search-box .form-input { width: 100%; padding-left: 40px; }
.filters { display: flex; gap: 8px; }
.filters .form-select { min-width: 160px; }
.result-count { margin: 12px 0; color: var(--text-mid); font-size: 13px; }
.product-cell { display: flex; gap: 12px; align-items: center; min-width: 220px; }
.product-cell img { width: 46px; height: 46px; border-radius: var(--radius-sm); object-fit: cover; background: var(--surface); }
.muted { color: var(--text-mid); font-size: 12px; margin-top: 3px; }
.stock-edit { display: flex; gap: 8px; align-items: center; min-width: 210px; }
.stock-edit .form-input { width: 145px; }
.state-panel { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-mid); text-align: center; }
.state-panel > i { font-size: 32px; }
.error-panel { color: var(--danger, #dc2626); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 1024px) { .toolbar { align-items: stretch; flex-direction: column; } .search-box { max-width: none; } .filters { display: grid; grid-template-columns: repeat(3, 1fr); } .filters .form-select { width: 100%; min-width: 0; } }
@media (max-width: 680px) { .inventory-page { gap: 16px; } .page-header { align-items: flex-start; } .filters { grid-template-columns: 1fr; } .table-wrapper { overflow: visible; } .table thead { display: none; } .table, .table tbody, .table tr, .table td { display: block; width: 100%; } .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); } .table td { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; } .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; } .product-cell { min-width: 0; } .stock-edit { min-width: 0; } .stock-edit label { min-width: 0; } .stock-edit .form-input { width: 100%; } }
</style>
