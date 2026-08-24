<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import {
  buildAdjustmentPayload,
  buildItemPayload,
  formatQuantity,
  inventoryKpis,
  itemStockState,
  parseQuantity,
} from '@/utils/inventoryItem';

const toast = useToast();
const adminStore = useAdminStore();
const router = useRouter();

const CONFLICT_MESSAGE = 'Tồn kho đã thay đổi. Đã cập nhật số lượng hiện tại, vui lòng kiểm tra và gửi lại.';
const ITEM_TYPES = [{ value: 'INGREDIENT', label: 'Nguyên liệu' }, { value: 'FINISHED_GOOD', label: 'Thành phẩm' }];
const UNITS = [{ value: 'G', label: 'g (gram)' }, { value: 'ML', label: 'ml (mililit)' }, { value: 'PIECE', label: 'cái (phần)' }];
const FREQUENCY_LABELS = { DAILY: 'Hàng ngày', WEEKLY: 'Hàng tuần' };
const STATUS_FILTERS = [
  { value: 'ALL', label: 'Mọi trạng thái' },
  { value: 'OK', label: 'Còn hàng' },
  { value: 'LOW', label: 'Dưới mức tối thiểu' },
  { value: 'OUT', label: 'Hết hàng' },
  { value: 'INACTIVE', label: 'Ngừng theo dõi' },
];
const TYPE_LABELS = Object.fromEntries(ITEM_TYPES.map(({ value, label }) => [value, label]));
const UNIT_LABELS = Object.fromEntries(UNITS.map(({ value, label }) => [value, label.split(' ')[0]]));
const STATE_LABELS = { OK: 'Còn hàng', LOW: 'Dưới tối thiểu', OUT: 'Hết hàng', INACTIVE: 'Ngừng theo dõi' };
const STATE_CLASSES = { OK: 'badge-success', LOW: 'badge-warning', OUT: 'badge-danger', INACTIVE: 'badge-secondary' };

const items = ref([]);
const loading = ref(true);
const loadError = ref('');
const searchTerm = ref('');
const statusFilter = ref('ALL');
const unavailableVariants = ref(null);
const recentTransactions = ref([]);
const saving = ref(false);
const dialog = ref(null);
const dialogError = ref('');
const modalRef = ref(null);
let dialogTrigger = null;

const kpis = computed(() => inventoryKpis(items.value));
const itemNameById = computed(() => new Map(items.value.map((item) => [item.inventoryItemId, item.name])));

const filteredItems = computed(() => {
  const query = searchTerm.value.trim().toLocaleLowerCase('vi');
  return items.value
    .filter((item) => {
      if (query && !`${item.name} ${item.inventoryCode} ${item.inventoryItemId}`.toLocaleLowerCase('vi').includes(query)) return false;
      return statusFilter.value === 'ALL' || itemStockState(item) === statusFilter.value;
    })
    .sort((a, b) => a.name.localeCompare(b.name, 'vi'));
});

async function loadItems() {
  loading.value = true;
  loadError.value = '';
  try {
    const data = await adminApi.getInventoryItems();
    items.value = Array.isArray(data) ? data : [];
  } catch (error) {
    loadError.value = error.message || 'Không thể tải danh sách mặt hàng';
  } finally {
    loading.value = false;
  }
}

// ponytail: one availability call per variant — fine at menu scale (~dozens); batch endpoint arrives in phase 2
async function loadUnavailableVariants() {
  try {
    await adminStore.fetchProducts();
    const variantIds = adminStore.allProducts.flatMap((product) => (product.variants || []).map((variant) => variant.variantId)).filter(Boolean);
    const results = await Promise.all(variantIds.map((id) => adminApi.getVariantAvailability(id).catch(() => null)));
    unavailableVariants.value = results.filter((entry) => entry && ['OUT_OF_STOCK', 'SUSPENDED'].includes(entry.availabilityStatus)).length;
  } catch {
    unavailableVariants.value = null;
  }
}

async function loadRecentTransactions() {
  try {
    const page = await adminApi.getInventoryTransactions({ page: 0, size: 5 });
    recentTransactions.value = Array.isArray(page?.items) ? page.items : [];
  } catch {
    recentTransactions.value = [];
  }
}

function refreshSideData() {
  loadRecentTransactions();
  loadUnavailableVariants();
}

function openDialog(kind, item, event) {
  dialogTrigger = event?.currentTarget || null;
  dialogError.value = '';
  const base = { kind, item: item ? { ...item } : null, form: {} };
  if (kind === 'create') base.form = { inventoryCode: '', name: '', itemType: 'INGREDIENT', baseUnit: 'G', minimumQuantity: '0', countFrequency: 'DAILY', active: true };
  if (kind === 'edit') base.form = { inventoryCode: item.inventoryCode, name: item.name, itemType: item.itemType, baseUnit: item.baseUnit, minimumQuantity: String(item.minimumQuantity), countFrequency: item.countFrequency, active: Boolean(item.active) };
  if (kind === 'adjust') base.form = { operation: 'INCREASE', quantity: '', reason: '', note: '' };
  dialog.value = base;
  nextTick(() => modalRef.value?.querySelector('input:not(:disabled), select:not(:disabled)')?.focus());
}

async function closeDialog() {
  if (saving.value) return;
  const trigger = dialogTrigger;
  dialog.value = null;
  dialogError.value = '';
  await nextTick();
  trigger?.focus?.();
}

function handleModalKeydown(event) {
  if (event.key === 'Escape') return closeDialog();
  if (event.key !== 'Tab') return;
  const controls = [...modalRef.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)')];
  const current = controls.indexOf(document.activeElement);
  if (!controls.length) return;
  const next = event.shiftKey
    ? (current <= 0 ? controls.length - 1 : current - 1)
    : (current === controls.length - 1 ? 0 : current + 1);
  event.preventDefault();
  controls[next].focus();
}

function projectedQuantity(form) {
  const parsed = parseQuantity(form.quantity);
  if (!parsed.ok) return null;
  return form.operation === 'DECREASE'
    ? Number((dialog.value.item.onHandQuantity - parsed.value).toFixed(4))
    : Number((dialog.value.item.onHandQuantity + parsed.value).toFixed(4));
}

function applyConflict(error) {
  const current = error.currentOnHandQuantity;
  if (current !== undefined && current !== null && dialog.value?.item) {
    dialog.value.item.onHandQuantity = Number(current);
    syncRow(dialog.value.item);
  }
  dialogError.value = `${CONFLICT_MESSAGE} Hiện có: ${formatQuantity(current)}`;
  loadItems();
}

function syncRow(item) {
  const index = items.value.findIndex((row) => row.inventoryItemId === item.inventoryItemId);
  if (index >= 0) items.value[index] = { ...items.value[index], ...item };
}

async function submitItemForm() {
  const { kind, item, form } = dialog.value;
  if (!/^[A-Z0-9-]{1,30}$/.test(String(form.inventoryCode).trim().toUpperCase())) return showFormError('Mã kho chỉ gồm A-Z, 0-9, dấu gạch ngang');
  if (!form.name.trim()) return showFormError('Nhập tên mặt hàng');
  if (form.name.trim().length > 150) return showFormError('Tên mặt hàng không vượt quá 150 ký tự');
  if (!/^\d+(\.\d{1,4})?$/.test(String(form.minimumQuantity).trim().replace(',', '.') || '0')) return showFormError('Mức tối thiểu phải là số không âm, tối đa 4 chữ số thập phân');
  saving.value = true;
  try {
    const payload = buildItemPayload(form);
    const saved = kind === 'create'
      ? await adminApi.createInventoryItem(payload)
      : await adminApi.updateInventoryItem(item.inventoryItemId, payload);
    if (kind === 'create') items.value.push(saved);
    else syncRow(saved);
    toast.success(kind === 'create' ? 'Đã tạo mặt hàng' : 'Đã lưu mặt hàng');
    saving.value = false;
    await closeDialog();
    refreshSideData();
  } catch (error) {
    dialogError.value = error.message || 'Không thể lưu mặt hàng';
  } finally {
    saving.value = false;
  }
}

async function submitMutation(kind) {
  const { item, form } = dialog.value;
  const parsed = parseQuantity(form.quantity);
  if (!parsed.ok) return showFormError('Số lượng phải là số dương, tối đa 4 chữ số thập phân');
  if (!form.reason.trim()) return showFormError('Vui lòng nhập lý do');
  if (form.reason.trim().length > 100) return showFormError('Lý do không vượt quá 100 ký tự');
  if (kind === 'adjust' && projectedQuantity(form) < 0) return showFormError('Khả dụng sau điều chỉnh không thể âm');
  saving.value = true;
  try {
    const payload = buildAdjustmentPayload(item, form);
    const saved = await adminApi.adjustInventoryItem(payload);
    syncRow(saved);
    toast.success('Đã điều chỉnh tồn kho');
    saving.value = false;
    await closeDialog();
    refreshSideData();
  } catch (error) {
    if (error.status === 409) return applyConflict(error);
    dialogError.value = error.message || 'Không thể lưu giao dịch';
  } finally {
    saving.value = false;
  }
}

function showFormError(message) {
  dialogError.value = message;
}

onMounted(() => {
  loadItems();
  refreshSideData();
});
</script>

<template>
  <main class="inventory-page">
    <header class="page-header">
      <div>
        <h1>Tổng quan kho</h1>
        <p class="page-subtitle">Theo dõi nguyên liệu và thành phẩm theo mặt hàng kho</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openDialog('create')"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm mặt hàng</button>
        <button class="btn btn-outline" @click="router.push({ name: 'AdminGoodsReceipts' })"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Phiếu nhập</button>
        <button class="btn btn-outline" @click="router.push({ name: 'AdminStockCounts' })"><i class="bi bi-clipboard-check" aria-hidden="true"></i> Kiểm kê</button>
        <button class="btn btn-outline" @click="router.push({ name: 'AdminInventoryLedger' })"><i class="bi bi-journal-text" aria-hidden="true"></i> Sổ tồn kho</button>
        <button class="btn btn-outline" :disabled="loading" @click="loadItems"><i class="bi bi-arrow-clockwise" :class="{ spin: loading }" aria-hidden="true"></i> Làm mới</button>
      </div>
    </header>

    <section class="stat-grid inventory-stats" aria-label="Tổng quan tồn kho">
      <div class="stat-card stat-total"><span class="stat-icon stat-blue"><i class="bi bi-boxes" aria-hidden="true"></i></span><strong class="stat-value">{{ kpis.itemCount }}</strong><span class="stat-label">Tổng mặt hàng</span></div>
      <button class="stat-card" :class="{ active: statusFilter === 'LOW' }" :aria-pressed="statusFilter === 'LOW'" @click="statusFilter = statusFilter === 'LOW' ? 'ALL' : 'LOW'">
        <span class="stat-icon stat-yellow"><i class="bi bi-exclamation-triangle" aria-hidden="true"></i></span><strong class="stat-value">{{ kpis.belowMinimumCount }}</strong><span class="stat-label">Dưới mức tối thiểu</span>
      </button>
      <div class="stat-card stat-total"><span class="stat-icon stat-red"><i class="bi bi-slash-circle" aria-hidden="true"></i></span><strong class="stat-value">{{ unavailableVariants === null ? '—' : unavailableVariants }}</strong><span class="stat-label">Kích cỡ hết hàng</span></div>
      <button class="stat-card stat-total" @click="router.push({ name: 'AdminInventoryLedger' })"><span class="stat-icon stat-green"><i class="bi bi-receipt" aria-hidden="true"></i></span><strong class="stat-value">{{ recentTransactions.length }}</strong><span class="stat-label">Giao dịch gần đây</span></button>
    </section>

    <section class="card card-flat" aria-label="Danh sách mặt hàng kho">
      <div class="toolbar">
        <label class="search-box">
          <span class="sr-only">Tìm mặt hàng</span><i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="searchTerm" class="form-input" type="search" placeholder="Tìm tên hoặc mã mặt hàng..." />
        </label>
        <label class="status-select"><span class="sr-only">Trạng thái</span>
          <select v-model="statusFilter" class="form-select"><option v-for="filter in STATUS_FILTERS" :key="filter.value" :value="filter.value">{{ filter.label }}</option></select>
        </label>
      </div>
      <div class="result-count" aria-live="polite">{{ filteredItems.length }} / {{ items.length }} mặt hàng</div>

      <div v-if="loading" class="state-panel" role="status"><i class="bi bi-arrow-repeat spin" aria-hidden="true"></i><span>Đang tải dữ liệu tồn kho...</span></div>
      <div v-else-if="loadError" class="state-panel error-panel" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>{{ loadError }}</strong><button class="btn btn-outline btn-sm" @click="loadItems">Thử lại</button></div>
      <div v-else-if="filteredItems.length === 0" class="state-panel"><i class="bi bi-inbox" aria-hidden="true"></i><strong>Không tìm thấy mặt hàng</strong><span>Điều chỉnh từ khóa hoặc bộ lọc.</span></div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead><tr><th scope="col">Mặt hàng</th><th scope="col">Loại</th><th scope="col">Hiện có</th><th scope="col">Đã giữ</th><th scope="col">Khả dụng</th><th scope="col">Đơn vị</th><th scope="col">Tối thiểu</th><th scope="col">Lịch kiểm</th><th scope="col">Giá vốn TB</th><th scope="col">Giá trị tồn</th><th scope="col">Kiểm gần nhất</th><th scope="col">Trạng thái</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr></thead>
          <tbody>
            <tr v-for="item in filteredItems" :key="item.inventoryItemId">
              <td data-label="Mặt hàng"><strong>{{ item.name }}</strong><div class="muted">{{ item.inventoryCode }} · #{{ item.inventoryItemId }}</div></td>
              <td data-label="Loại">{{ TYPE_LABELS[item.itemType] || item.itemType }}</td>
              <td data-label="Hiện có">{{ formatQuantity(item.onHandQuantity) }}</td>
              <td data-label="Đã giữ">{{ formatQuantity(item.reservedQuantity) }}</td>
              <td data-label="Khả dụng"><strong>{{ formatQuantity(item.availableQuantity) }}</strong></td>
              <td data-label="Đơn vị">{{ UNIT_LABELS[item.baseUnit] || item.baseUnit }}</td>
              <td data-label="Tối thiểu">{{ formatQuantity(item.minimumQuantity) }}</td>
              <td data-label="Lịch kiểm">{{ FREQUENCY_LABELS[item.countFrequency] || item.countFrequency }}</td>
              <td data-label="Giá vốn TB">{{ Number(item.averageUnitCost).toLocaleString('vi-VN') }} ₫</td>
              <td data-label="Giá trị tồn"><strong>{{ Number(Number(item.onHandQuantity) * Number(item.averageUnitCost)).toLocaleString('vi-VN') }} ₫</strong></td>
              <td data-label="Kiểm gần nhất">{{ item.lastCountedAt || '—' }}</td>
              <td data-label="Trạng thái"><span class="badge" :class="STATE_CLASSES[itemStockState(item)]">{{ STATE_LABELS[itemStockState(item)] }}</span></td>
              <td data-label="Thao tác">
                <div class="row-actions">
                  <button class="btn btn-sm btn-outline" :aria-label="`Tạo phiếu nhập cho ${item.name}`" @click="router.push({ name: 'AdminGoodsReceipts' })"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Nhập</button>
                  <button class="btn btn-sm btn-outline" :aria-label="`Điều chỉnh ${item.name}`" @click="openDialog('adjust', item, $event)"><i class="bi bi-sliders" aria-hidden="true"></i> Điều chỉnh</button>
                  <button class="btn btn-sm btn-ghost" :aria-label="`Sửa ${item.name}`" @click="openDialog('edit', item, $event)"><i class="bi bi-pencil" aria-hidden="true"></i></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="recentTransactions.length" class="card card-flat recent-panel" aria-label="Giao dịch gần đây">
      <h2>Giao dịch gần đây</h2>
      <ul class="recent-list">
        <li v-for="tx in recentTransactions" :key="tx.inventoryTransactionId">
          <time :datetime="tx.createdAt">{{ tx.createdAt }}</time>
          <span class="badge badge-secondary">{{ tx.transactionType }}</span>
          <span>{{ tx.inventoryItemId ? `#${tx.inventoryItemId} ${itemNameById.get(tx.inventoryItemId) || ''}` : '—' }}</span>
          <strong>{{ formatQuantity(tx.quantity) }}</strong>
        </li>
      </ul>
      <router-link class="recent-link" :to="{ name: 'AdminInventoryLedger' }">Xem sổ tồn kho</router-link>
    </section>

    <div v-if="dialog" class="modal-overlay" @mousedown.self="closeDialog">
      <form ref="modalRef" class="modal" role="dialog" aria-modal="true" :aria-labelledby="dialog.kind === 'create' || dialog.kind === 'edit' ? 'item-dialog-title' : 'stock-dialog-title'" @keydown="handleModalKeydown" @submit.prevent="dialog.kind === 'create' || dialog.kind === 'edit' ? submitItemForm() : submitMutation(dialog.kind)">
        <div class="modal-header">
          <div v-if="dialog.kind === 'create' || dialog.kind === 'edit'">
            <small>MẶT HÀNG KHO</small>
            <h3 id="item-dialog-title">{{ dialog.kind === 'create' ? 'Thêm mặt hàng' : `Sửa ${dialog.item.name}` }}</h3>
          </div>
          <div v-else>
            <small>ĐIỀU CHỈNH TỒN KHO</small>
            <h3 id="stock-dialog-title">{{ dialog.item.name }} <span class="muted">#{{ dialog.item.inventoryItemId }}</span></h3>
          </div>
          <button type="button" class="icon-button" aria-label="Đóng" :disabled="saving" @click="closeDialog"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </div>
        <div class="modal-body">
          <template v-if="dialog.kind === 'create' || dialog.kind === 'edit'">
            <label class="form-group" for="item-code"><span class="form-label">Mã kho</span></label>
            <input id="item-code" v-model="dialog.form.inventoryCode" class="form-input" type="text" maxlength="30" pattern="[A-Za-z0-9-]+" required />
            <label class="form-group" for="item-name"><span class="form-label">Tên mặt hàng</span></label>
            <input id="item-name" v-model="dialog.form.name" class="form-input" type="text" maxlength="150" required />
            <label class="form-group" for="item-type"><span class="form-label">Loại</span></label>
            <select id="item-type" v-model="dialog.form.itemType" class="form-select" required><option v-for="type in ITEM_TYPES" :key="type.value" :value="type.value">{{ type.label }}</option></select>
            <label class="form-group" for="item-unit"><span class="form-label">Đơn vị cơ sở</span></label>
            <select id="item-unit" v-model="dialog.form.baseUnit" class="form-select" required><option v-for="unit in UNITS" :key="unit.value" :value="unit.value">{{ unit.label }}</option></select>
            <p v-if="dialog.kind === 'edit'" class="hint muted">Đơn vị không thể đổi sau khi mặt hàng được dùng trong công thức hoặc sổ kho.</p>
            <label class="form-group" for="item-minimum"><span class="form-label">Mức tối thiểu</span></label>
            <input id="item-minimum" v-model="dialog.form.minimumQuantity" class="form-input" type="number" min="0" step="0.0001" required />
            <label class="form-group" for="item-frequency"><span class="form-label">Tần suất kiểm</span></label>
            <select id="item-frequency" v-model="dialog.form.countFrequency" class="form-select" required><option value="DAILY">Hàng ngày</option><option value="WEEKLY">Hàng tuần</option></select>
            <label class="checkbox-field" for="item-active"><input id="item-active" v-model="dialog.form.active" type="checkbox" /> Đang theo dõi</label>
          </template>
          <template v-else>
            <p class="muted">Hiện có: <strong>{{ formatQuantity(dialog.item.onHandQuantity) }}</strong> · Khả dụng: {{ formatQuantity(dialog.item.availableQuantity) }} {{ UNIT_LABELS[dialog.item.baseUnit] || dialog.item.baseUnit }}</p>
            <div v-if="dialog.kind === 'adjust'" class="operation-tabs" role="group" aria-label="Kiểu điều chỉnh">
              <button type="button" :aria-pressed="dialog.form.operation === 'INCREASE'" :class="{ active: dialog.form.operation === 'INCREASE' }" @click="dialog.form.operation = 'INCREASE'">Tăng (+)</button>
              <button type="button" :aria-pressed="dialog.form.operation === 'DECREASE'" :class="{ active: dialog.form.operation === 'DECREASE' }" @click="dialog.form.operation = 'DECREASE'">Giảm (−)</button>
            </div>
            <label class="form-group" for="stock-quantity"><span class="form-label">Số lượng</span></label>
            <input id="stock-quantity" v-model="dialog.form.quantity" class="form-input" type="number" min="0.0001" step="0.0001" inputmode="decimal" required />
            <p v-if="dialog.kind === 'adjust' && projectedQuantity(dialog.form) !== null" class="stock-preview" aria-live="polite">Khả dụng dự kiến: <strong>{{ formatQuantity(projectedQuantity(dialog.form)) }}</strong></p>
            <label class="form-group" for="stock-reason"><span class="form-label">Lý do</span></label>
            <input id="stock-reason" v-model="dialog.form.reason" class="form-input" type="text" maxlength="100" required />
            <label class="form-group" for="stock-note"><span class="form-label">Ghi chú</span></label>
            <textarea id="stock-note" v-model="dialog.form.note" class="form-input" rows="3" maxlength="500"></textarea>
          </template>
          <p v-if="dialogError" class="dialog-error" role="alert">{{ dialogError }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" :disabled="saving" @click="closeDialog">Hủy</button>
          <button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : (dialog.kind === 'adjust' ? 'Lưu điều chỉnh' : 'Lưu') }}</button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.inventory-page { display: grid; gap: 24px; }
.header-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.page-subtitle { margin: 4px 0 0; color: var(--text-mid); font-size: 14px; }
.inventory-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.inventory-stats .stat-card { text-align: left; border: 1px solid var(--border-light); background: #fff; cursor: pointer; }
.inventory-stats button.stat-card:hover { border-color: var(--primary); transform: translateY(-1px); }
.inventory-stats .stat-card.active { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-50); }
.stat-icon { color: #fff; }
.stat-blue { background: linear-gradient(135deg,#2563eb,#60a5fa); }
.stat-yellow { background: linear-gradient(135deg,#d97706,#fbbf24); }
.stat-red { background: linear-gradient(135deg,#dc2626,#f87171); }
.stat-green { background: linear-gradient(135deg,#059669,#34d399); }
.stat-total { cursor: default; }
.toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; }
.search-box { position: relative; max-width: 440px; flex: 1; }
.search-box i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-mid); }
.search-box .form-input { width: 100%; padding-left: 40px; }
.status-select .form-select { min-width: 200px; }
.result-count { margin: 12px 0; color: var(--text-mid); font-size: 13px; }
.muted { color: var(--text-mid); font-size: 12px; margin-top: 3px; }
.row-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.recent-panel h2 { margin: 0 0 12px; font-size: 16px; }
.recent-list { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.recent-list li { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px dashed var(--border-light); font-size: 13px; }
.recent-list time { color: var(--text-mid); min-width: 170px; }
.recent-list strong { margin-left: auto; }
.recent-link { font-weight: 700; font-size: 13px; }
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
.checkbox-field { display: flex; align-items: center; gap: 8px; min-height: 40px; cursor: pointer; }
.hint { margin: 0; font-size: 12px; }
.operation-tabs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
.operation-tabs button { min-height: 40px; border: 1px solid var(--border-light); border-radius: 8px; background: #fff; color: var(--text-mid); font-weight: 700; cursor: pointer; }
.operation-tabs button.active { border-color: var(--primary); background: var(--primary-50); color: var(--primary); }
.stock-preview { margin: 0; padding: 10px 12px; border-radius: 8px; background: var(--surface); }
.dialog-error { margin: 0; color: var(--danger, #dc2626); font-size: 13px; font-weight: 600; }
.state-panel { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-mid); text-align: center; }
.state-panel > i { font-size: 32px; }
.error-panel { color: var(--danger, #dc2626); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 1024px) { .inventory-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } .toolbar { align-items: stretch; flex-direction: column; } .search-box { max-width: none; } }
@media (max-width: 680px) { .inventory-page { gap: 16px; } .inventory-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } .table-wrapper { overflow: visible; } .table thead { display: none; } .table, .table tbody, .table tr, .table td { display: block; width: 100%; } .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); } .table td { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; } .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; } }
</style>
