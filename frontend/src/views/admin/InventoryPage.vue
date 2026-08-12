<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { adminApi } from '@/api';
import { formatPrice } from '@/utils/format';
import { useToast } from '@/stores/toast';

const toast = useToast();
const adminStore = useAdminStore();
const router = useRouter();
const searchTerm = ref('');
const activeFilter = ref('ALL');
const categoryFilter = ref('ALL');
const sortBy = ref('product-asc');
const loading = ref(true);
const loadError = ref('');
const REASONS = [
  { value: 'STOCK_COUNT', label: 'Kiểm kê' },
  { value: 'DAMAGE', label: 'Hư hỏng' },
  { value: 'EXPIRED', label: 'Hết hạn' },
  { value: 'OTHER', label: 'Khác' },
];
const OPERATIONS = [
  { value: 'INCREASE', label: 'Tăng', id: 'adjust-increase' },
  { value: 'DECREASE', label: 'Giảm', id: 'adjust-decrease' },
  { value: 'SET', label: 'Đặt mới', id: 'adjust-set' },
];
const adjustmentRow = ref(null);
const wasteRow = ref(null);
const adjustmentModal = ref(null);
const adjustmentForm = ref({ operation: 'INCREASE', quantity: '', reasonCode: 'STOCK_COUNT', note: '' });
const wasteForm = ref({ quantity: '', reasonCode: 'DAMAGE', note: '' });
const adjustmentError = ref('');
const submitting = ref(false);
let adjustmentTrigger = null;

const projectedQuantity = computed(() => {
  if (!adjustmentRow.value) return null;
  const quantity = Number(adjustmentForm.value.quantity);
  if (!Number.isInteger(quantity)) return null;
  if (adjustmentForm.value.operation === 'INCREASE') return adjustmentRow.value.stock + quantity;
  if (adjustmentForm.value.operation === 'DECREASE') return adjustmentRow.value.stock - quantity;
  return quantity;
});

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

function editProduct(row) {
  router.push({ name: 'AdminProductEdit', params: { id: row.productId } });
}

async function openAdjust(row, event) {
  wasteRow.value = null;
  adjustmentTrigger = event.currentTarget;
  adjustmentRow.value = { ...row };
  adjustmentError.value = '';
  adjustmentForm.value = { operation: 'INCREASE', quantity: '', reasonCode: 'STOCK_COUNT', note: '' };
  await nextTick();
  adjustmentModal.value.querySelector('[role="tab"]').focus();
}

function openWaste(row) {
  adjustmentRow.value = null;
  wasteRow.value = row;
  wasteForm.value = { quantity: '', reasonCode: 'DAMAGE', note: '' };
}

async function closeModals() {
  const restoreTarget = adjustmentRow.value ? adjustmentTrigger : null;
  adjustmentRow.value = null;
  wasteRow.value = null;
  await nextTick();
  restoreTarget?.focus();
}

function selectOperation(operation) {
  adjustmentForm.value.operation = operation;
  adjustmentForm.value.quantity = '';
  adjustmentError.value = '';
}

function handleTabKey(event) {
  const current = OPERATIONS.findIndex(({ value }) => value === adjustmentForm.value.operation);
  let next = current;
  if (event.key === 'ArrowRight') next = (current + 1) % OPERATIONS.length;
  else if (event.key === 'ArrowLeft') next = (current - 1 + OPERATIONS.length) % OPERATIONS.length;
  else if (event.key === 'Home') next = 0;
  else if (event.key === 'End') next = OPERATIONS.length - 1;
  else return;
  event.preventDefault();
  selectOperation(OPERATIONS[next].value);
  nextTick(() => document.getElementById(OPERATIONS[next].id)?.focus());
}

function handleModalKey(event) {
  if (event.key === 'Escape' && !submitting.value) return closeModals();
  if (event.key !== 'Tab') return;
  const controls = [...adjustmentModal.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)')];
  const first = controls[0];
  const last = controls.at(-1);
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

async function submitAdjust(event) {
  adjustmentError.value = '';
  const quantity = Number(adjustmentForm.value.quantity);
  if (!Number.isInteger(quantity) || (adjustmentForm.value.operation === 'SET' ? quantity < 0 : quantity <= 0)) {
    adjustmentError.value = adjustmentForm.value.operation === 'SET' ? 'Tồn kho mới phải là số nguyên không âm' : 'Số lượng phải là số nguyên dương';
    return;
  }
  if (projectedQuantity.value < 0) {
    adjustmentError.value = 'Tồn kho dự kiến không thể âm';
    return;
  }
  if (!adjustmentForm.value.reasonCode) {
    adjustmentError.value = 'Vui lòng chọn lý do điều chỉnh';
    return;
  }
  if (adjustmentForm.value.reasonCode === 'OTHER' && !adjustmentForm.value.note.trim()) {
    adjustmentError.value = 'Ghi chú là bắt buộc khi chọn lý do Khác';
    return;
  }
  submitting.value = true;
  try {
    await adminApi.adjustInventory(adjustmentRow.value.variantId, {
      operation: adjustmentForm.value.operation,
      quantity,
      expectedQuantity: adjustmentRow.value.stock,
      reasonCode: adjustmentForm.value.reasonCode,
      note: adjustmentForm.value.note.trim(),
    });
    toast.success('Đã điều chỉnh tồn kho');
    await closeModals();
    await loadProducts();
  } catch (error) {
    if (error.response?.status === 409) {
      const currentQuantity = error.response.data?.data?.currentQuantity;
      if (Number.isInteger(currentQuantity)) adjustmentRow.value.stock = currentQuantity;
      adjustmentError.value = 'Tồn kho đã thay đổi. Đã cập nhật số lượng hiện tại, vui lòng kiểm tra và gửi lại.';
      return;
    }
    adjustmentError.value = error.message || 'Không thể điều chỉnh tồn kho';
  } finally {
    submitting.value = false;
  }
}

async function submitWaste() {
  const quantity = Number(wasteForm.value.quantity);
  if (!Number.isInteger(quantity) || quantity <= 0) return toast.error('Số lượng lãng phí phải là số nguyên dương');
  if (quantity > wasteRow.value.stock) return toast.error('Số lượng lãng phí vượt quá tồn kho hiện tại');
  if (!wasteForm.value.reasonCode) return toast.error('Vui lòng chọn lý do lãng phí');
  submitting.value = true;
  try {
    await adminApi.wasteInventory(wasteRow.value.variantId, {
      quantity,
      reasonCode: wasteForm.value.reasonCode,
      note: wasteForm.value.note.trim(),
    });
    toast.success('Đã ghi nhận lãng phí');
    closeModals();
    await loadProducts();
  } catch (error) {
    toast.error(error.message || 'Không thể ghi nhận lãng phí');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="inventory-page">
    <header class="page-header">
      <div>
        <h1>Quản lý tồn kho</h1>
        <p class="page-subtitle">Theo dõi và cập nhật tồn kho từng biến thể</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="router.push({ name: 'AdminInventoryLedger' })">
          <i class="bi bi-journal-text" aria-hidden="true"></i> Sổ tồn kho
        </button>
        <button class="btn btn-outline" :disabled="loading" @click="loadProducts">
          <i class="bi bi-arrow-clockwise" :class="{ spin: loading }" aria-hidden="true"></i> Làm mới
        </button>
      </div>
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
               <td data-label="Tồn kho">{{ row.stock === null ? 'Không giới hạn' : row.stock }}</td>
              <td data-label="Thao tác">
                <div class="row-actions">
                  <template v-if="row.stock !== null">
                    <button class="btn btn-sm btn-outline" @click="openAdjust(row, $event)"><i class="bi bi-sliders" aria-hidden="true"></i> Điều chỉnh</button>
                    <button class="btn btn-sm btn-outline" @click="openWaste(row)"><i class="bi bi-trash3" aria-hidden="true"></i> Lãng phí</button>
                  </template>
                  <button class="btn btn-sm btn-ghost" :aria-label="`Sửa sản phẩm ${row.productName}`" @click="editProduct(row)"><i class="bi bi-pencil" aria-hidden="true"></i></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="adjustmentRow" class="modal-overlay" @mousedown.self="closeModals">
      <form ref="adjustmentModal" class="modal" role="dialog" aria-modal="true" aria-labelledby="adjust-title" @keydown="handleModalKey" @submit.prevent="submitAdjust">
        <div class="modal-header">
          <div><small>ĐIỀU CHỈNH TỒN KHO</small><h3 id="adjust-title">{{ adjustmentRow.productName }} – {{ adjustmentRow.variantName }}</h3></div>
          <button type="button" class="icon-button" aria-label="Đóng" :disabled="submitting" @click="closeModals"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </div>
        <div class="modal-body">
          <p class="muted">Tồn kho hiện tại: <strong>{{ adjustmentRow.stock }}</strong> · SKU: {{ adjustmentRow.sku || '—' }}</p>
          <div class="adjust-tabs" role="tablist" aria-label="Kiểu điều chỉnh" @keydown="handleTabKey">
            <button v-for="operation in OPERATIONS" :id="operation.id" :key="operation.value" type="button" role="tab" :aria-selected="adjustmentForm.operation === operation.value" aria-controls="adjust-panel" :tabindex="adjustmentForm.operation === operation.value ? 0 : -1" @click="selectOperation(operation.value)">{{ operation.label }}</button>
          </div>
          <div id="adjust-panel" role="tabpanel" :aria-labelledby="OPERATIONS.find(({ value }) => value === adjustmentForm.operation).id">
            <label class="form-group" for="adjust-quantity"><span class="form-label">{{ adjustmentForm.operation === 'SET' ? 'Tồn kho mới' : 'Số lượng' }}</span></label>
            <input id="adjust-quantity" v-model="adjustmentForm.quantity" class="form-input" type="number" :min="adjustmentForm.operation === 'SET' ? 0 : 1" step="1" required />
          </div>
          <p class="stock-preview" aria-live="polite">Tồn kho dự kiến: <strong>{{ projectedQuantity === null ? '—' : projectedQuantity }}</strong></p>
          <label class="form-group" for="adjust-reason"><span class="form-label">Lý do</span></label><select id="adjust-reason" v-model="adjustmentForm.reasonCode" class="form-select" required><option v-for="reason in REASONS" :key="reason.value" :value="reason.value">{{ reason.label }}</option></select>
          <label class="form-group" for="adjust-note"><span class="form-label">Ghi chú{{ adjustmentForm.reasonCode === 'OTHER' ? ' (bắt buộc)' : '' }}</span></label><textarea id="adjust-note" v-model="adjustmentForm.note" class="form-input" rows="3" maxlength="500" :required="adjustmentForm.reasonCode === 'OTHER'"></textarea>
          <p v-if="adjustmentError" class="adjust-error" role="alert">{{ adjustmentError }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" :disabled="submitting" @click="closeModals">Hủy</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? 'Đang lưu...' : 'Lưu điều chỉnh' }}</button>
        </div>
      </form>
    </div>

    <div v-if="wasteRow" class="modal-overlay" @mousedown.self="closeModals">
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="waste-title" @submit.prevent="submitWaste">
        <div class="modal-header">
          <div><small>GHI NHẬN LÃNG PHÍ</small><h3 id="waste-title">{{ wasteRow.productName }} – {{ wasteRow.variantName }}</h3></div>
          <button type="button" class="icon-button" aria-label="Đóng" :disabled="submitting" @click="closeModals"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <p class="muted">Tồn kho hiện tại: <strong>{{ wasteRow.stock }}</strong> · SKU: {{ wasteRow.sku || '—' }}</p>
          <label class="form-group"><span class="form-label">Số lượng lãng phí</span><input v-model.number="wasteForm.quantity" class="form-input" type="number" min="1" step="1" :max="wasteRow.stock" required /></label>
          <label class="form-group"><span class="form-label">Lý do</span><select v-model="wasteForm.reasonCode" class="form-select" required><option v-for="reason in REASONS" :key="reason.value" :value="reason.value">{{ reason.label }}</option></select></label>
          <label class="form-group"><span class="form-label">Ghi chú</span><textarea v-model="wasteForm.note" class="form-input" rows="3" maxlength="500"></textarea></label>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" :disabled="submitting" @click="closeModals">Hủy</button>
          <button type="submit" class="btn btn-danger" :disabled="submitting">{{ submitting ? 'Đang lưu...' : 'Xác nhận lãng phí' }}</button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.inventory-page { display: grid; gap: 24px; }
.header-actions { display: flex; gap: 8px; }
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
.row-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.modal-overlay { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: rgba(13,20,33,.64); backdrop-filter: blur(3px); }
.modal { width: min(520px,100%); max-height: calc(100vh - 40px); overflow: hidden; display: flex; flex-direction: column; border-radius: 16px; background: #fff; box-shadow: 0 25px 80px rgba(0,0,0,.25); }
.modal-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header small { color: var(--role-admin); font-size: 10px; font-weight: 800; letter-spacing: .1em; }
.modal-header h3 { margin: 3px 0 0; font-size: 18px; }
.icon-button { border: 0; background: transparent; border-radius: 8px; font-size: 18px; padding: 8px; cursor: pointer; color: var(--text-mid); }
.icon-button:hover { background: var(--surface); }
.modal-body { display: flex; flex-direction: column; gap: 14px; overflow-y: auto; padding: 22px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); background: #fff; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; font-weight: 700; color: var(--text-mid); }
.adjust-tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.adjust-tabs button { min-height: 40px; border: 1px solid var(--border-light); border-radius: 8px; background: #fff; color: var(--text-mid); font-weight: 700; cursor: pointer; }
.adjust-tabs button[aria-selected="true"] { border-color: var(--primary); background: var(--primary-50); color: var(--primary); }
.adjust-tabs button:focus-visible, .icon-button:focus-visible { outline: 3px solid var(--primary); outline-offset: 2px; }
.stock-preview { margin: 0; padding: 10px 12px; border-radius: 8px; background: var(--surface); }
.adjust-error { margin: 0; color: var(--danger, #dc2626); font-size: 13px; font-weight: 600; }
.state-panel { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-mid); text-align: center; }
.state-panel > i { font-size: 32px; }
.error-panel { color: var(--danger, #dc2626); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 1024px) { .toolbar { align-items: stretch; flex-direction: column; } .search-box { max-width: none; } .filters { display: grid; grid-template-columns: repeat(3, 1fr); } .filters .form-select { width: 100%; min-width: 0; } }
@media (max-width: 680px) { .inventory-page { gap: 16px; } .page-header { align-items: flex-start; } .filters { grid-template-columns: 1fr; } .table-wrapper { overflow: visible; } .table thead { display: none; } .table, .table tbody, .table tr, .table td { display: block; width: 100%; } .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); } .table td { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; } .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; } .product-cell { min-width: 0; } }
</style>
