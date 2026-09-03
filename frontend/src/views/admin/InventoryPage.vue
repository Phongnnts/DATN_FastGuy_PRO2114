<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api';
import InventoryLedgerPage from './InventoryLedgerPage.vue';
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
import { paginateWarehouseItems } from '@/utils/inventoryOperations';
import { inventoryAnalyticsDelta, inventoryAnalyticsPeriod, normalizeInventoryAnalyticsSeries } from '@/utils/inventoryAnalytics';

const toast = useToast();
const adminStore = useAdminStore();
const route = useRoute();
const router = useRouter();
const activeTab = computed(() => route.query.tab === 'history' ? 'history' : 'current');
const tabRefs = ref([]);
let currentLoaded = false;

function loadCurrentTab() {
  if (currentLoaded) return;
  currentLoaded = true;
  loadItems();
  refreshSideData();
}

function selectTab(tab) {
  if (tab === activeTab.value) return;
  if (tab === 'current') loadCurrentTab();
  router.push({ query: { ...route.query, tab: tab === 'current' ? undefined : tab } });
}

function handleTabKeydown(event, index) {
  const targets = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: 1 };
  if (!(event.key in targets)) return;
  event.preventDefault();
  const next = (targets[event.key] + 2) % 2;
  selectTab(['current', 'history'][next]);
  nextTick(() => tabRefs.value[next]?.focus());
}

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
const statusFromQuery = raw => STATUS_FILTERS.some(({ value }) => value === raw) ? raw : 'ALL';
const TYPE_LABELS = Object.fromEntries(ITEM_TYPES.map(({ value, label }) => [value, label]));
const UNIT_LABELS = Object.fromEntries(UNITS.map(({ value, label }) => [value, label.split(' ')[0]]));
const STATE_LABELS = { OK: 'Còn hàng', LOW: 'Dưới tối thiểu', OUT: 'Hết hàng', INACTIVE: 'Ngừng theo dõi' };
const STATE_CLASSES = { OK: 'badge-success', LOW: 'badge-warning', OUT: 'badge-danger', INACTIVE: 'badge-secondary' };

const items = ref([]);
const loading = ref(true);
const loadError = ref('');
const searchTerm = ref('');
const statusFilter = ref(statusFromQuery(route.query.filter));
const PAGE_SIZE = 10;
const inventoryPage = ref(1);
const analytics = ref(null);
const analyticsLoading = ref(false);
const analyticsError = ref('');
const analyticsDays = ref([7, 30, 90].includes(Number(route.query.days)) ? Number(route.query.days) : 30);
let analyticsGeneration = 0;
const unavailableVariants = ref(null);
const recentTransactions = ref([]);
const saving = ref(false);
const dialog = ref(null);
const dialogError = ref('');
const modalRef = ref(null);
let dialogTrigger = null;

const kpis = computed(() => inventoryKpis(items.value));
const itemNameById = computed(() => new Map(items.value.map((item) => [item.inventoryItemId, item.name])));
const todayActions = computed(() => [
  kpis.value.belowMinimumCount ? { label: `${kpis.value.belowMinimumCount} mặt hàng cần nhập thêm`, route: 'AdminGoodsReceipts', action: 'Nhập hàng' } : null,
  unavailableVariants.value > 0 ? { label: `${unavailableVariants.value} kích cỡ đang không bán được`, route: 'AdminRecipes', action: 'Xem công thức' } : null,
].filter(Boolean));
const shortageRiskItems = computed(() => items.value.filter(item => item.active !== false && Number(item.reservedQuantity) > Number(item.onHandQuantity)));
const lowStockItems = computed(() => items.value.filter(item => item.active !== false && ['LOW', 'OUT'].includes(itemStockState(item))));
const inventoryValue = computed(() => items.value.reduce((total, item) => total + Number(item.onHandQuantity || 0) * Number(item.averageUnitCost || 0), 0));
const money = (value) => `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 0 })} ₫`;
const analyticsSeries = computed(() => normalizeInventoryAnalyticsSeries(analytics.value?.series));
const inventoryValueDelta = computed(() => inventoryAnalyticsDelta(analytics.value?.kpis?.inventoryValue, analytics.value?.previousKpis?.inventoryValue));
const chartMax = computed(() => Math.max(1, ...analyticsSeries.value.map(point => point.inventoryValue)));
const movementMax = computed(() => Math.max(1, ...analyticsSeries.value.flatMap(point => [point.receiptValue, point.consumptionValue, point.wasteValue + point.adjustmentLossValue])));
const deltaText = (current, previous, percent = false) => { const delta = inventoryAnalyticsDelta(current, previous); if (percent && !delta.comparable) return 'Chưa có kỳ so sánh'; const value = percent ? `${Math.abs(delta.percent).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%` : Math.abs(delta.value).toLocaleString('vi-VN'); return `${delta.value >= 0 ? 'Tăng' : 'Giảm'} ${value} so kỳ trước`; };
async function loadAnalytics() { const request = ++analyticsGeneration; analyticsLoading.value = true; analyticsError.value = ''; try { const data = await adminApi.getInventoryAnalytics(inventoryAnalyticsPeriod(analyticsDays.value)); if (request !== analyticsGeneration) return; analytics.value = data; } catch (error) { if (request !== analyticsGeneration) return; analyticsError.value = error.message || 'Không thể tải phân tích tồn kho'; } finally { if (request === analyticsGeneration) analyticsLoading.value = false; } }
function selectAnalyticsDays(days) { if (analyticsDays.value === days) return; analyticsDays.value = days; router.push({ query: { ...route.query, days: days === 30 ? undefined : String(days) } }); loadAnalytics(); }

const filteredItems = computed(() => {
  const query = searchTerm.value.trim().toLocaleLowerCase('vi');
  return items.value
    .filter((item) => {
      if (query && !`${item.name} ${item.inventoryCode} ${item.inventoryItemId}`.toLocaleLowerCase('vi').includes(query)) return false;
      return statusFilter.value === 'ALL' || itemStockState(item) === statusFilter.value;
    })
    .sort((a, b) => a.name.localeCompare(b.name, 'vi'));
});
const paginatedItems = computed(() => paginateWarehouseItems(filteredItems.value, inventoryPage.value, PAGE_SIZE));

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
  if (kind === 'waste') base.form = { operation: 'DECREASE', quantity: '', reason: 'Hỏng nguyên liệu', note: '' };
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
    if (error.status === 409) {
      dialogError.value = error.message || 'Mặt hàng đang được sử dụng và không thể đổi loại hoặc ngừng hoạt động.';
      return;
    }
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
    if (kind === 'waste') payload.quantity = Math.abs(payload.quantity);
    const saved = kind === 'waste' ? await adminApi.recordInventoryWaste(payload) : await adminApi.adjustInventoryItem(payload);
    syncRow(saved);
    toast.success(kind === 'waste' ? 'Đã ghi nhận hao hụt' : 'Đã điều chỉnh tồn kho');
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

watch(activeTab, (tab) => { if (tab === 'current') loadCurrentTab(); });
watch(searchTerm, () => { inventoryPage.value = 1; });
watch(() => paginatedItems.value.page, page => { if (inventoryPage.value !== page) inventoryPage.value = page; });
watch(() => route.query.filter, (raw) => {
  const next = statusFromQuery(raw);
  if (statusFilter.value !== next) statusFilter.value = next;
});
watch(statusFilter, (value) => {
  inventoryPage.value = 1;
  if (!STATUS_FILTERS.some(filter => filter.value === value) || value === statusFromQuery(route.query.filter)) return;
  router.push({ query: { ...route.query, filter: value === 'ALL' ? undefined : value } });
});
onMounted(() => {
  if (activeTab.value === 'current') { loadCurrentTab(); loadAnalytics(); }
});
</script>

<template>
  <div class="inventory-page">
    <header class="page-header">
      <div>
        <h1>Vận hành kho hôm nay</h1>
        <p class="page-subtitle">Biết việc cần làm trước, xem số liệu chi tiết khi cần.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openDialog('create')"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm mặt hàng</button>
        <button class="btn btn-outline" @click="router.push({ name: 'AdminGoodsReceipts' })"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Phiếu nhập</button>
        <button class="btn btn-outline" @click="selectTab('history')"><i class="bi bi-journal-text" aria-hidden="true"></i> Lịch sử biến động</button>
        <button class="btn btn-outline" :disabled="loading" @click="loadItems"><i class="bi bi-arrow-clockwise" :class="{ spin: loading }" aria-hidden="true"></i> Làm mới</button>
      </div>
    </header>

    <div v-if="activeTab === 'current'" class="period-controls" aria-label="Khoảng thời gian phân tích"><button v-for="days in [7,30,90]" :key="days" type="button" :aria-pressed="analyticsDays === days" @click="selectAnalyticsDays(days)">{{ days }} ngày</button></div>
    <section v-if="activeTab === 'current'" class="kpi-strip" aria-label="Chỉ số tồn kho"><article><span>Tổng mặt hàng</span><strong>{{ analytics?.kpis?.itemCount ?? kpis.itemCount }}</strong><small>{{ analytics ? deltaText(analytics.kpis.itemCount, analytics.previousKpis.itemCount) : 'Theo dữ liệu hiện tại' }}</small></article><article><span>Cần nhập thêm</span><strong>{{ analytics?.kpis?.lowStockCount ?? lowStockItems.length }}</strong><small>{{ analytics ? deltaText(analytics.kpis.lowStockCount, analytics.previousKpis.lowStockCount) : 'Theo dữ liệu hiện tại' }}</small></article><article><span>Hết hàng</span><strong>{{ analytics?.kpis?.outOfStockCount ?? 0 }}</strong><small>{{ analytics ? deltaText(analytics.kpis.outOfStockCount, analytics.previousKpis.outOfStockCount) : 'Theo dữ liệu hiện tại' }}</small></article><article><span>Giá trị tồn</span><strong>{{ money(analytics?.kpis?.inventoryValue ?? inventoryValue) }}</strong><small>{{ analytics ? deltaText(analytics.kpis.inventoryValue, analytics.previousKpis.inventoryValue, true) : 'Theo giá vốn hiện tại' }}</small></article></section>
    <section v-if="activeTab === 'current'" class="analytics-grid" aria-label="Xu hướng tồn kho"><article class="analytics-card value-chart"><header><div><span>Giá trị tồn kho</span><strong>{{ money(analytics?.kpis?.inventoryValue ?? inventoryValue) }}</strong></div><small>{{ inventoryValueDelta.comparable ? deltaText(analytics?.kpis?.inventoryValue, analytics?.previousKpis?.inventoryValue, true) : 'Chưa có kỳ so sánh' }}</small></header><div v-if="analyticsLoading" class="chart-state" role="status">Đang tải xu hướng...</div><div v-else-if="analyticsError" class="chart-state" role="alert">{{ analyticsError }} <button type="button" @click="loadAnalytics">Thử lại</button></div><div v-else class="line-chart" role="img" aria-label="Biểu đồ giá trị tồn kho theo ngày"><i v-for="point in analyticsSeries" :key="point.date" :style="{ height: `${Math.max(3, point.inventoryValue / chartMax * 100)}%` }" :title="`${point.date}: ${money(point.inventoryValue)}`"></i></div></article><article class="analytics-card"><header><div><span>Biến động tồn kho</span><strong>Theo giá trị</strong></div><small>Nhập · tiêu hao · hao hụt</small></header><div class="movement-chart" role="img" aria-label="Biểu đồ biến động tồn kho theo ngày"><div v-for="point in analyticsSeries" :key="point.date" :title="`${point.date}: nhập ${money(point.receiptValue)}, tiêu hao ${money(point.consumptionValue)}, hao hụt ${money(point.wasteValue + point.adjustmentLossValue)}`"><i class="receipt" :style="{ height: `${point.receiptValue / movementMax * 100}%` }"></i><i class="consume" :style="{ height: `${point.consumptionValue / movementMax * 100}%` }"></i><i class="waste" :style="{ height: `${(point.wasteValue + point.adjustmentLossValue) / movementMax * 100}%` }"></i></div></div></article></section>
    <section v-if="activeTab === 'current'" class="secondary-grid"><article class="analytics-card health-card"><header><div><span>Sức khỏe tồn kho</span><strong>{{ analytics?.kpis?.itemCount ?? items.length }} mặt hàng</strong></div></header><div class="health-bar"><i class="healthy" :style="{ flex: analytics?.health?.healthyCount || 0 }"></i><i class="attention" :style="{ flex: analytics?.health?.attentionCount || 0 }"></i><i class="low" :style="{ flex: analytics?.health?.lowStockCount || 0 }"></i><i class="out" :style="{ flex: analytics?.health?.outOfStockCount || 0 }"></i></div><p>{{ analytics?.health?.healthyCount || 0 }} ổn · {{ analytics?.health?.attentionCount || 0 }} chú ý · {{ analytics?.health?.lowStockCount || 0 }} cần nhập · {{ analytics?.health?.outOfStockCount || 0 }} hết hàng</p></article><article class="analytics-card action-today"><header><div><span>Cần làm hôm nay</span><strong>{{ lowStockItems.length ? `${lowStockItems.length} mặt hàng cần nhập thêm` : 'Kho đang ổn' }}</strong></div></header><p>{{ lowStockItems.slice(0,3).map(item => item.name).join(' · ') || 'Không có mặt hàng nào cần xử lý ngay.' }}</p><button v-if="lowStockItems.length" type="button" class="btn btn-primary" @click="router.push({ name: 'AdminGoodsReceipts' })">Tạo phiếu nhập</button></article></section>
    <section v-if="false" class="command-rail inventory-command-rail" aria-label="Ưu tiên vận hành kho">
      <button class="command-card inventory-risk-command" :class="{ urgent: shortageRiskItems.length }" @click="statusFilter = shortageRiskItems.length ? 'OUT' : 'ALL'"><span>Rủi ro thiếu</span><strong>{{ shortageRiskItems.length }} mặt hàng</strong><small>Lượng đã giữ cao hơn tồn thực tế</small></button>
      <button class="command-card attention" @click="statusFilter = 'LOW'"><span>Cần nhập thêm</span><strong>{{ lowStockItems.length }}</strong><small>Dưới mức tối thiểu hoặc đã hết</small></button>
      <article class="command-card"><span>Giá trị tồn hiện tại</span><strong>{{ money(inventoryValue) }}</strong><small>Tính từ tồn thực tế và giá vốn</small></article>
    </section>

    <nav class="page-tabs" role="tablist" aria-label="Tồn kho">
      <button id="inventory-current-tab" :ref="el => tabRefs[0] = el" type="button" role="tab" aria-controls="inventory-current-panel" :aria-selected="activeTab === 'current'" :tabindex="activeTab === 'current' ? 0 : -1" :class="{ active: activeTab === 'current' }" @keydown="handleTabKeydown($event, 0)" @click="selectTab('current')">Tồn hiện tại</button>
      <button id="inventory-history-tab" :ref="el => tabRefs[1] = el" type="button" role="tab" aria-controls="inventory-history-panel" :aria-selected="activeTab === 'history'" :tabindex="activeTab === 'history' ? 0 : -1" :class="{ active: activeTab === 'history' }" @keydown="handleTabKeydown($event, 1)" @click="selectTab('history')">Lịch sử biến động</button>
    </nav>

    <section v-if="activeTab === 'current'" id="inventory-current-panel" class="tab-panel stock-workspace" role="tabpanel" aria-labelledby="inventory-current-tab">
    <div class="workspace-heading">
      <div><p class="eyebrow">Tồn hiện tại</p><h2>Tổng quan sức khỏe kho</h2></div>
      <p>Ưu tiên rủi ro thiếu hàng trước khi đi vào từng mặt hàng.</p>
    </div>
    <section v-if="false" class="risk-strip today-panel priority-card" aria-labelledby="today-title">
      <div><p class="eyebrow">Ưu tiên</p><h2 id="today-title">Hôm nay cần làm gì?</h2></div>
      <div class="action-grid" aria-live="polite">
        <article v-for="action in todayActions" :key="action.route" class="action-card">
          <strong>{{ action.label }}</strong><button class="btn btn-outline" @click="router.push({ name: action.route })">{{ action.action }}</button>
        </article>
      </div>
    </section>

    <nav v-if="false" class="workflow" aria-label="Quy trình tồn kho và giá vốn">
      <router-link :to="{ name: 'AdminGoodsReceipts' }"><span>1</span><strong>Nhập hàng</strong><small>Ghi nhận hàng về</small></router-link>
      <router-link :to="{ name: 'AdminRecipes' }"><span>2</span><strong>Công thức</strong><small>Đặt lượng dùng</small></router-link>
      <span class="workflow-step"><span>3</span><strong>Bán món</strong><small>Kho tự trừ</small></span>
      <router-link :to="{ path: '/admin/reports', query: { tab: 'menu' } }"><span>4</span><strong>Lãi gộp</strong><small>Xem theo món</small></router-link>
    </nav>

    <section v-if="false" class="stat-grid inventory-stats" aria-label="Tổng quan tồn kho">
      <div class="stat-card stat-total"><span class="stat-icon stat-blue"><i class="bi bi-boxes" aria-hidden="true"></i></span><strong class="stat-value">{{ kpis.itemCount }}</strong><span class="stat-label">Tổng mặt hàng</span></div>
      <button class="stat-card" :class="{ active: statusFilter === 'LOW' }" :aria-pressed="statusFilter === 'LOW'" @click="statusFilter = statusFilter === 'LOW' ? 'ALL' : 'LOW'">
        <span class="stat-icon stat-yellow"><i class="bi bi-exclamation-triangle" aria-hidden="true"></i></span><strong class="stat-value">{{ kpis.belowMinimumCount }}</strong><span class="stat-label">Dưới mức tối thiểu</span>
      </button>
      <div class="stat-card stat-total"><span class="stat-icon stat-red"><i class="bi bi-slash-circle" aria-hidden="true"></i></span><strong class="stat-value">{{ unavailableVariants === null ? '—' : unavailableVariants }}</strong><span class="stat-label">Kích cỡ hết hàng</span></div>
      <div class="stat-card stat-total"><span class="stat-icon stat-green"><i class="bi bi-cash-coin" aria-hidden="true"></i></span><strong class="stat-value">{{ money(inventoryValue) }}</strong><span class="stat-label">Giá trị tồn hiện tại</span></div>
    </section>

    <section class="card card-flat" aria-label="Danh sách nguyên liệu">
      <div class="section-title"><div><p class="eyebrow">Theo dõi hàng ngày</p><h2>Nguyên liệu</h2></div><span>Chi tiết số liệu được thu gọn</span></div>
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
          <thead><tr><th scope="col">Mặt hàng</th><th scope="col">Tồn hiện tại</th><th scope="col">Tối thiểu</th><th scope="col">Giá vốn hiện tại</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
          <tbody>
            <tr v-for="item in paginatedItems.items" :key="item.inventoryItemId">
              <td data-label="Mặt hàng"><strong>{{ item.name }}</strong><div class="muted">{{ item.inventoryCode }} · {{ TYPE_LABELS[item.itemType] || item.itemType }}</div><small class="muted">Đã giữ {{ formatQuantity(item.reservedQuantity) }} {{ UNIT_LABELS[item.baseUnit] || item.baseUnit }}</small></td>
              <td data-label="Tồn hiện tại"><strong>{{ formatQuantity(item.onHandQuantity) }} {{ UNIT_LABELS[item.baseUnit] || item.baseUnit }}</strong><small class="available-quantity">Có thể dùng {{ formatQuantity(item.availableQuantity) }} {{ UNIT_LABELS[item.baseUnit] || item.baseUnit }}</small></td>
              <td data-label="Tối thiểu">{{ formatQuantity(item.minimumQuantity) }} {{ UNIT_LABELS[item.baseUnit] || item.baseUnit }}</td>
              <td data-label="Giá vốn hiện tại"><strong>{{ Number(item.averageUnitCost) > 0 ? `${money(item.averageUnitCost)}/${UNIT_LABELS[item.baseUnit] || item.baseUnit}` : 'Chưa có dữ liệu' }}</strong></td>
              <td data-label="Trạng thái"><span class="badge" :class="STATE_CLASSES[itemStockState(item)]">{{ STATE_LABELS[itemStockState(item)] }}</span></td>
              <td data-label="Thao tác">
                <div class="row-actions">
                  <button class="btn btn-sm btn-outline" :aria-label="`Tạo phiếu nhập cho ${item.name}`" @click="router.push({ name: 'AdminGoodsReceipts' })"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Nhập</button>
                  <button class="btn btn-sm btn-outline" :aria-label="`Ghi nhận hao hụt ${item.name}`" @click="openDialog('waste', item, $event)"><i class="bi bi-trash3" aria-hidden="true"></i> Hao hụt</button><button class="btn btn-sm btn-outline" :aria-label="`Điều chỉnh ${item.name}`" @click="openDialog('adjust', item, $event)"><i class="bi bi-sliders" aria-hidden="true"></i> Điều chỉnh</button>
                  <button class="btn btn-sm btn-ghost" :aria-label="`Sửa ${item.name}`" @click="openDialog('edit', item, $event)"><i class="bi bi-pencil" aria-hidden="true"></i></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer v-if="filteredItems.length" class="pagination-footer"><span>{{ paginatedItems.from }}–{{ paginatedItems.to }} / {{ paginatedItems.totalItems }} mặt hàng</span><nav aria-label="Phân trang tồn kho"><button type="button" :disabled="paginatedItems.page === 1" @click="inventoryPage--">‹</button><button v-for="page in paginatedItems.totalPages" :key="page" type="button" :aria-current="page === paginatedItems.page ? 'page' : undefined" :class="{ active: page === paginatedItems.page }" @click="inventoryPage = page">{{ page }}</button><button type="button" :disabled="paginatedItems.page === paginatedItems.totalPages" @click="inventoryPage++">›</button></nav></footer>
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
      <button type="button" class="recent-link" @click="selectTab('history')">Xem lịch sử biến động</button>
    </section>

    </section>
    <section v-else id="inventory-history-panel" class="tab-panel history-workspace" role="tabpanel" aria-labelledby="inventory-history-tab">
      <div class="workspace-heading">
        <div><p class="eyebrow">Dòng biến động</p><h2>Lịch sử có thể đối soát</h2></div>
        <p>Lọc theo mặt hàng, đơn hàng, loại giao dịch và thời gian.</p>
      </div>
      <InventoryLedgerPage />
    </section>

    <div v-if="dialog && activeTab === 'current'" class="modal-overlay" @mousedown.self="closeDialog">
      <form ref="modalRef" class="modal" role="dialog" aria-modal="true" :aria-labelledby="dialog.kind === 'create' || dialog.kind === 'edit' ? 'item-dialog-title' : 'stock-dialog-title'" @keydown="handleModalKeydown" @submit.prevent="dialog.kind === 'create' || dialog.kind === 'edit' ? submitItemForm() : submitMutation(dialog.kind)">
        <div class="modal-header">
          <div v-if="dialog.kind === 'create' || dialog.kind === 'edit'">
            <small>MẶT HÀNG KHO</small>
            <h3 id="item-dialog-title">{{ dialog.kind === 'create' ? 'Thêm mặt hàng' : `Sửa ${dialog.item.name}` }}</h3>
          </div>
          <div v-else>
            <small>{{ dialog.kind === 'waste' ? 'GHI NHẬN HAO HỤT' : 'ĐIỀU CHỈNH TỒN KHO' }}</small>
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
          <button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : (dialog.kind === 'waste' ? 'Ghi nhận hao hụt' : dialog.kind === 'adjust' ? 'Lưu điều chỉnh' : 'Lưu') }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.inventory-page { display: grid; grid-template-columns: minmax(0, 1fr); gap: 24px; }
.period-controls{display:flex;justify-content:flex-end;gap:6px}.period-controls button{min-height:40px;padding:0 13px;border:1px solid var(--admin-border);border-radius:999px;background:#fff}.period-controls button[aria-pressed=true]{background:var(--admin-foreground);color:#fff}.kpi-strip{display:grid;grid-template-columns:repeat(4,1fr);overflow:hidden;border:1px solid var(--admin-border);border-radius:18px;background:#fff;box-shadow:0 12px 30px rgba(15,23,42,.06)}.kpi-strip article{display:grid;gap:6px;padding:18px;border-right:1px solid var(--admin-border)}.kpi-strip article:last-child{border:0}.kpi-strip span,.analytics-card header span{color:var(--admin-muted);font-size:11px;font-weight:800;text-transform:uppercase;letter-spacing:.06em}.kpi-strip strong{font-size:25px}.kpi-strip small{color:var(--admin-muted)}.analytics-grid,.secondary-grid{display:grid;grid-template-columns:1.25fr 1fr;gap:14px}.analytics-card{display:grid;gap:16px;min-width:0;padding:20px;border:1px solid var(--admin-border);border-radius:18px;background:#fff;box-shadow:0 12px 30px rgba(15,23,42,.06)}.analytics-card header{display:flex;justify-content:space-between;gap:12px}.analytics-card header div{display:grid;gap:5px}.analytics-card header strong{font-size:20px}.analytics-card header small,.analytics-card p{color:var(--admin-muted)}.line-chart,.movement-chart{display:flex;align-items:end;gap:4px;height:210px;padding:16px 4px 0;border-bottom:1px solid var(--admin-border);background:linear-gradient(to bottom,transparent 24%,var(--admin-border) 25%,transparent 26%,transparent 49%,var(--admin-border) 50%,transparent 51%,transparent 74%,var(--admin-border) 75%,transparent 76%)}.line-chart>i{flex:1;min-width:3px;border-radius:5px 5px 0 0;background:var(--admin-brand)}.movement-chart>div{display:flex;align-items:end;gap:1px;flex:1;height:100%}.movement-chart i{flex:1;min-width:2px;border-radius:3px 3px 0 0}.movement-chart .receipt{background:var(--admin-brand)}.movement-chart .consume{background:var(--admin-info)}.movement-chart .waste{background:var(--admin-danger)}.chart-state{display:grid;place-items:center;min-height:210px;color:var(--admin-muted)}.health-bar{display:flex;height:12px;overflow:hidden;border-radius:999px;background:var(--admin-border)}.health-bar .healthy{background:var(--admin-success)}.health-bar .attention{background:var(--admin-warning)}.health-bar .low{background:var(--admin-brand)}.health-bar .out{background:var(--admin-danger)}.action-today{align-content:start}.action-today .btn{justify-self:start}.command-rail{display:grid;grid-template-columns:1.2fr 1fr 1fr;gap:12px}.command-card{display:grid;gap:6px;min-height:112px;padding:18px;border:1px solid var(--admin-border);border-radius:var(--admin-panel-radius);background:#fff;color:inherit;text-align:left}.command-card>span{color:var(--admin-muted);font-size:11px;font-weight:800;letter-spacing:.06em;text-transform:uppercase}.command-card strong{font-size:24px}.command-card small{color:var(--admin-muted)}button.command-card{cursor:pointer}.command-card.attention{background:var(--admin-warning-soft)}.inventory-risk-command.urgent{border-color:var(--admin-danger);background:var(--admin-danger-soft);color:var(--admin-danger)}.available-quantity{display:block;margin-top:4px;color:var(--admin-brand);font-weight:700}
.stock-workspace,.history-workspace{display:grid;gap:20px;min-width:0}.workspace-heading{display:flex;align-items:end;justify-content:space-between;gap:24px}.workspace-heading h2{margin:2px 0 0;font-size:22px}.workspace-heading>p{max-width:52ch;margin:0;color:var(--admin-muted);font-size:13px;text-align:right}.risk-strip{border-radius:var(--admin-panel-radius)}
.page-tabs{display:flex;gap:4px;width:max-content;max-width:100%;padding:4px;border:1px solid var(--border-light);border-radius:12px;background:#fff}.page-tabs button{min-height:40px;padding:8px 16px;border-radius:8px;color:var(--text-mid);font-weight:700}.page-tabs button.active{background:var(--admin-brand-soft);color:var(--admin-foreground)}.page-tabs button:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
.today-panel{display:grid;gap:14px;padding:22px;border:1px solid var(--admin-border);border-radius:18px;background:#fff;color:var(--admin-foreground);box-shadow:0 10px 28px rgba(15,23,42,.06)}.today-panel h2{margin:2px 0 0;font-size:24px}.eyebrow{margin:0;color:#f2aa87;font-size:11px;font-weight:800;letter-spacing:.1em;text-transform:uppercase}.action-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}.action-card{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:56px;padding:12px 14px;border:1px solid var(--admin-border);border-radius:12px;background:var(--admin-surface)}.action-card .btn{justify-self:start;background:#fff}.workflow{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:8px}.workflow>a,.workflow-step{display:flex;align-items:center;gap:8px;min-height:44px;padding:8px 12px;border:1px solid var(--border-light);border-radius:999px;background:#fff;color:inherit;text-decoration:none}.workflow span>span,.workflow a>span{display:grid;place-items:center;width:26px;height:26px;border-radius:50%;background:var(--primary-50);color:var(--primary);font-weight:800}.workflow small{color:var(--text-mid)}
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
.result-count { margin: 12px 0; color: var(--text-mid); font-size: 13px; }.pagination-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;padding-top:14px;border-top:1px solid var(--admin-border);color:var(--admin-muted);font-size:12px}.pagination-footer nav{display:flex;gap:6px}.pagination-footer button{min-width:40px;min-height:40px;border:1px solid var(--admin-border);border-radius:10px;background:#fff}.pagination-footer button.active{border-color:var(--admin-brand);background:var(--admin-brand-soft);color:var(--admin-brand)}
.muted { color: var(--text-mid); font-size: 12px; margin-top: 3px; }
.row-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.section-title{display:flex;align-items:end;justify-content:space-between;gap:12px}.section-title h2{margin:2px 0 0}.section-title>span{color:var(--text-mid);font-size:13px}details{margin-top:8px}summary{width:max-content;min-height:40px;display:flex;align-items:center;color:var(--primary);font-size:12px;font-weight:700;cursor:pointer}dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px 16px;margin:6px 0 0;padding:10px;border-radius:8px;background:var(--surface)}dl div{display:flex;justify-content:space-between;gap:8px}dt{color:var(--text-mid)}dd{margin:0;font-weight:650}
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
.inventory-page :is(button,a,summary,input,select):focus-visible{outline:3px solid var(--primary);outline-offset:2px}.inventory-page :is(.btn,input,select){min-height:40px}
@media (max-width: 1024px) { .kpi-strip{grid-template-columns:1fr 1fr}.kpi-strip article:nth-child(2){border-right:0}.analytics-grid,.secondary-grid{grid-template-columns:1fr}.inventory-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } .toolbar { align-items: stretch; flex-direction: column; } .search-box { max-width: none; } }
@media (max-width: 900px){.command-rail{grid-template-columns:1fr}.action-grid{grid-template-columns:1fr}.workflow{grid-template-columns:1fr 1fr}.workflow>a,.workflow-step{min-height:88px}}
@media(prefers-reduced-motion:reduce){.inventory-page *{scroll-behavior:auto!important;animation-duration:.01ms!important}}
@media (max-width: 680px) { .period-controls{justify-content:flex-start;overflow-x:auto}.kpi-strip{grid-template-columns:1fr}.kpi-strip article{border-right:0;border-bottom:1px solid var(--admin-border)}.line-chart,.movement-chart{height:170px}.workspace-heading{align-items:flex-start;flex-direction:column;gap:6px}.workspace-heading>p{text-align:left}.inventory-page,.inventory-stats,.inventory-stats .stat-card,.table { min-width: 0; } .inventory-page { gap: 16px; } .inventory-stats { grid-template-columns: 1fr; } .workflow{grid-template-columns:1fr}.section-title{align-items:flex-start;flex-direction:column}.table-wrapper { overflow: visible; } .table thead { display: none; } .table, .table tbody, .table tr, .table td { display: block; width: 100%; } .table tr { padding: 16px; border-bottom: 1px solid var(--border-light); } .table td { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 12px; align-items: center; padding: 8px 0; border: 0; } .table td::before { content: attr(data-label); color: var(--text-mid); font-size: 12px; font-weight: 600; } dl{grid-template-columns:1fr} }
</style>
