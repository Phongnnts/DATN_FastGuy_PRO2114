<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import { formatPrice } from '@/utils/format';
import { catalogCounts, filterProducts, paginateProducts, productTypes } from '@/utils/adminProductCatalog';
import { productStockSummary } from '@/utils/stockPolicy';
import { createStockPageLoader, productMatchesStockFilter } from '@/utils/adminStockOperations';

const router = useRouter();
const adminStore = useAdminStore();
const toast = useToast();
const searchTerm = ref('');
const categoryFilter = ref('');
const statusFilter = ref('');
const productTypeFilter = ref('');
const stockFilter = ref('');
const sortBy = ref('name-asc');
const page = ref(1);
const pageSize = 10;
const loading = ref(true);
const loadError = ref('');
const dashboardError = ref('');
const threshold = ref(null);
const loader = createStockPageLoader({
  get loading() { return loading.value; },
  set loading(value) { loading.value = value; },
  get error() { return loadError.value; },
  set error(value) { loadError.value = value; },
  get threshold() { return threshold.value; },
  set threshold(value) { threshold.value = value; },
  get dashboardError() { return dashboardError.value; },
  set dashboardError(value) { dashboardError.value = value; },
});
const productToHide = ref(null);
const hiding = ref(false);
const hideDialog = ref(null);
const hideCancelButton = ref(null);
let previousFocus = null;
let previousBodyOverflow = '';

async function loadCatalog() {
  await loader.load({
    required: [() => adminStore.fetchProducts(), () => adminStore.fetchCategories()],
    dashboard: () => adminStore.fetchDashboard(),
    errorMessage: 'Không thể tải danh sách sản phẩm',
  });
}

onMounted(loadCatalog);

function openAdd() {
  router.push({ name: 'AdminProductCreate' });
}

function openEdit(product) {
  router.push({ name: 'AdminProductEdit', params: { id: product.id } });
}

async function requestHide(product, event) {
  previousFocus = event.currentTarget;
  previousBodyOverflow = document.body.style.overflow;
  productToHide.value = product;
  document.body.style.overflow = 'hidden';
  await nextTick();
  hideCancelButton.value?.focus();
}

function cancelHide() {
  if (hiding.value) return;
  productToHide.value = null;
  document.body.style.overflow = previousBodyOverflow;
  nextTick(() => previousFocus?.focus());
}

function handleDialogKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    cancelHide();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = [...hideDialog.value.querySelectorAll('button:not([disabled])')];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

onBeforeUnmount(() => {
  loader.stop();
  document.body.style.overflow = previousBodyOverflow;
});

async function hideProduct() {
  if (!productToHide.value || hiding.value) return;
  hiding.value = true;
  try {
    await adminStore.deleteProduct(productToHide.value.id);
    productToHide.value = null;
    document.body.style.overflow = previousBodyOverflow;
    nextTick(() => previousFocus?.focus());
  } catch (error) {
    toast.error(error.message || 'Không thể ẩn sản phẩm đang được sử dụng');
  } finally {
    hiding.value = false;
  }
}

function categoryName(product) {
  return product.categoryName || adminStore.allCategories.find((category) => category.id === product.categoryId)?.name || '-';
}

const lowStockThreshold = computed(() => threshold.value);

function stockSummary(product) {
  return productStockSummary(product, lowStockThreshold.value);
}

function stockOf(product) {
  return stockSummary(product).total;
}

function stockStatusLabel(product) {
  const status = stockSummary(product).status;
  if (status === 'UNAVAILABLE') return 'Ngừng bán';
  if (status === 'UNKNOWN') return 'Không xác định';
  if (status === 'OUT') return 'Hết hàng';
  return 'Còn hàng';
}

const counts = computed(() => catalogCounts(adminStore.allProducts));
const availableProductTypes = computed(() => productTypes(adminStore.allProducts).filter((type) => type !== 'COMBO'));

const filtered = computed(() => {
  const products = filterProducts(adminStore.allProducts, {
    query: searchTerm.value,
    categoryId: categoryFilter.value,
    productType: productTypeFilter.value,
    status: statusFilter.value,
  }).filter((product) => productMatchesStockFilter(product, stockFilter.value, lowStockThreshold.value));
  return products.sort((first, second) => {
    if (sortBy.value === 'price-asc') return first.basePrice - second.basePrice;
    if (sortBy.value === 'price-desc') return second.basePrice - first.basePrice;
    if (sortBy.value.startsWith('stock-')) {
      const firstStock = stockOf(first);
      const secondStock = stockOf(second);
      if (firstStock === null) return 1;
      if (secondStock === null) return -1;
      return sortBy.value === 'stock-asc' ? firstStock - secondStock : secondStock - firstStock;
    }
    return sortBy.value === 'name-desc' ? second.name.localeCompare(first.name, 'vi') : first.name.localeCompare(second.name, 'vi');
  });
});

const pagination = computed(() => paginateProducts(filtered.value, page.value, pageSize));
const pageCount = computed(() => pagination.value.pageCount);
const paginated = computed(() => pagination.value.items);
watch([searchTerm, categoryFilter, productTypeFilter, statusFilter, stockFilter, sortBy], () => { page.value = 1; });
watch(pagination, (value) => { if (page.value !== value.page) page.value = value.page; });

function resetFilters() {
  searchTerm.value = '';
  categoryFilter.value = '';
  statusFilter.value = '';
  productTypeFilter.value = '';
  stockFilter.value = '';
  sortBy.value = 'name-asc';
}
</script>

<template>
  <main class="products-page">
    <div class="catalog-content" :inert="productToHide ? '' : undefined">
    <header class="products-hero">
      <div><span class="eyebrow">FASTGUY CATALOG</span><h1>Quản lý sản phẩm</h1><p>Kiểm soát thực đơn, giá bán, kích cỡ và tồn kho trong một không gian.</p></div>
      <button class="add-product" type="button" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm sản phẩm</button>
    </header>
    <div v-if="dashboardError" class="state state-warning" role="status">Đang dùng ngưỡng tồn kho đã lưu gần nhất ({{ lowStockThreshold }}). Không thể xác nhận dữ liệu dashboard mới: {{ dashboardError }}</div>
    <div class="stats-grid" aria-label="Thống kê sản phẩm">
      <article class="stat stat-total"><span class="stat-icon"><i class="bi bi-box-seam"></i></span><div><span>Tổng sản phẩm</span><strong>{{ counts.total }}</strong></div></article>
      <article class="stat stat-green"><span class="stat-icon"><i class="bi bi-check2-circle"></i></span><div><span>Đang bán</span><strong>{{ counts.available }}</strong></div></article>
      <article class="stat stat-amber"><span class="stat-icon"><i class="bi bi-exclamation-circle"></i></span><div><span>Hết hàng</span><strong>{{ counts.outOfStock }}</strong></div></article>
      <article class="stat stat-red"><span class="stat-icon"><i class="bi bi-percent"></i></span><div><span>Đang giảm giá</span><strong>{{ counts.discounted }}</strong></div></article>
    </div>
    <section class="catalog-card">
      <div class="catalog-heading"><div><span>Danh mục vận hành</span><h2>Danh sách sản phẩm</h2></div><p>{{ filtered.length }} / {{ counts.total }} sản phẩm</p></div>
      <div class="toolbar">
        <div class="search-box"><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" placeholder="Tìm tên, danh mục, SKU, kích cỡ..." aria-label="Tìm sản phẩm" /></div>
        <select v-model="categoryFilter" class="form-select" aria-label="Lọc danh mục"><option value="">Mọi danh mục</option><option v-for="category in adminStore.allCategories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select>
        <select v-model="productTypeFilter" class="form-select" aria-label="Lọc loại sản phẩm"><option value="">Mọi loại</option><option v-for="type in availableProductTypes" :key="type" :value="type">{{ type }}</option></select>
        <select v-model="statusFilter" class="form-select" aria-label="Lọc trạng thái"><option value="">Mọi trạng thái</option><option value="AVAILABLE">Đang bán</option><option value="UNAVAILABLE">Ngừng bán</option></select>
        <select v-model="stockFilter" class="form-select" aria-label="Lọc tồn kho"><option value="">Mọi tồn kho</option><option value="in">Còn hàng trên {{ lowStockThreshold }}</option><option value="low">Có SKU sắp hết (1–{{ lowStockThreshold }})</option><option value="out">Có SKU hết hàng</option><option value="unlimited">Không giới hạn</option><option value="unknown">Không xác định</option></select>
        <select v-model="sortBy" class="form-select" aria-label="Sắp xếp"><option value="name-asc">Tên A–Z</option><option value="name-desc">Tên Z–A</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option><option value="stock-asc">Tồn kho tăng dần</option><option value="stock-desc">Tồn kho giảm dần</option></select>
        <button class="reset-button" type="button" @click="resetFilters"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại</button>
      </div>
      <div v-if="loading" class="state" role="status"><span class="spinner"></span> Đang tải sản phẩm...</div>
      <div v-else-if="loadError" class="state state-error" role="alert">{{ loadError }} <button class="btn btn-sm btn-outline" type="button" @click="loadCatalog">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-box-seam"></i><strong>Không tìm thấy sản phẩm</strong><span>Thử thay đổi bộ lọc hoặc thêm sản phẩm mới.</span><button class="btn btn-outline" type="button" @click="resetFilters">Xóa bộ lọc</button></div>
      <div v-else class="table-wrapper desktop-catalog">
        <table class="table">
          <thead><tr><th></th><th>Tên</th><th>Danh mục</th><th>Giá gốc</th><th>Kích cỡ</th><th>Tồn kho</th><th>Trạng thái</th><th>Ảnh</th><th></th></tr></thead>
          <tbody>
            <tr v-for="product in paginated" :key="product.id">
              <td><img class="product-thumb" :src="product.image" :alt="product.name" loading="lazy" /></td>
              <td><div class="product-name"><strong>{{ product.name }}</strong><small>#{{ product.id }}</small></div></td>
              <td>{{ categoryName(product) }}</td>
              <td>{{ formatPrice(product.basePrice) }}</td>
              <td><span v-if="product.variants?.length" class="badge badge-info">{{ product.variants.length }} kích cỡ</span><span v-else class="text-muted">0</span></td>
              <td><strong>{{ stockSummary(product).unknownSkus > 0 ? 'Không xác định' : stockOf(product) === null ? 'Không giới hạn' : stockOf(product) }}</strong></td>
              <td><span :class="'badge badge-' + (stockSummary(product).status === 'AVAILABLE' ? 'success' : stockSummary(product).status === 'UNKNOWN' ? 'secondary' : 'danger')">{{ stockStatusLabel(product) }}</span></td>
              <td><span v-if="product.galleryImages?.length" class="badge badge-info">{{ product.galleryImages.length }} ảnh</span><span v-else class="text-muted">0</span></td>
              <td><div class="row-actions"><button class="icon-action" type="button" aria-label="Sửa sản phẩm" @click="openEdit(product)"><i class="bi bi-pencil"></i></button><button class="icon-action danger" type="button" aria-label="Ẩn sản phẩm" @click="requestHide(product, $event)"><i class="bi bi-eye-slash"></i></button></div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!loading && !loadError && filtered.length" class="mobile-catalog">
        <article v-for="product in paginated" :key="product.id" class="product-mobile-card">
          <img class="product-thumb" :src="product.image" :alt="product.name" loading="lazy" />
          <div class="mobile-product-main"><strong>{{ product.name }}</strong><small>#{{ product.id }} · {{ categoryName(product) }}</small><span>{{ formatPrice(product.basePrice) }} · {{ stockStatusLabel(product) }}</span></div>
          <div class="row-actions"><button class="icon-action" type="button" :aria-label="`Sửa ${product.name}`" @click="openEdit(product)"><i class="bi bi-pencil"></i></button><button class="icon-action danger" type="button" :aria-label="`Ẩn ${product.name}`" @click="requestHide(product, $event)"><i class="bi bi-eye-slash"></i></button></div>
        </article>
      </div>
      <div v-if="!loading && !loadError && filtered.length" class="pagination">
        <span>Hiển thị {{ pagination.start + 1 }}–{{ pagination.end }} / {{ filtered.length }}</span>
        <div><button class="btn btn-sm btn-outline" type="button" :disabled="page === 1" aria-label="Trang trước" @click="page--"><i class="bi bi-chevron-left"></i></button><span>Trang {{ page }} / {{ pageCount }}</span><button class="btn btn-sm btn-outline" type="button" :disabled="page === pageCount" aria-label="Trang sau" @click="page++"><i class="bi bi-chevron-right"></i></button></div>
      </div>
    </section>
    </div>
    <div v-if="productToHide" class="dialog-overlay" @click.self="cancelHide">
      <section ref="hideDialog" class="hide-dialog" role="dialog" aria-modal="true" aria-labelledby="hide-product-title" aria-describedby="hide-product-message" tabindex="-1" @keydown="handleDialogKeydown">
        <h2 id="hide-product-title">Ẩn sản phẩm</h2>
        <p id="hide-product-message">Ẩn “{{ productToHide.name }}” khỏi danh mục bán hàng?</p>
        <div class="dialog-actions"><button ref="hideCancelButton" class="btn btn-outline" type="button" :disabled="hiding" @click="cancelHide">Hủy</button><button class="btn btn-danger" type="button" :disabled="hiding" autofocus @click="hideProduct">{{ hiding ? 'Đang ẩn...' : 'Ẩn sản phẩm' }}</button></div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:14px}.stat{display:flex;align-items:center;gap:14px;min-height:104px;padding:18px;border:1px solid rgba(23,23,23,.06);border-radius:18px;background:#fff;box-shadow:0 7px 25px rgba(42,28,20,.045)}.stat .stat-icon{display:grid;flex:0 0 42px;height:42px;place-items:center;border-radius:13px;color:var(--primary);background:var(--primary-50)}.stat>div{display:flex;flex-direction:column}.stat span:not(.stat-icon){color:var(--text-mid);font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.07em}.stat strong{color:var(--text);font-size:27px}.stat-green .stat-icon{color:#15803d;background:#ecfdf3}.stat-amber .stat-icon{color:#b45309;background:#fff7e6}.stat-red .stat-icon{color:#dc2626;background:#fff1f1}.products-page{color:var(--text-dark)}.products-hero{position:relative;display:flex;align-items:end;justify-content:space-between;gap:30px;min-height:190px;margin-bottom:18px;padding:30px 34px;overflow:hidden;border-radius:26px;color:#fff;background:linear-gradient(125deg,#1b1714,#30231d 72%,#4a291d);box-shadow:0 20px 50px rgba(39,25,18,.13)}.products-hero>div,.add-product{position:relative;z-index:1}.eyebrow{color:var(--route-amber);font-size:10px;font-weight:800;letter-spacing:.17em}.products-hero h1{margin:9px 0 7px;font-size:clamp(29px,4vw,43px);line-height:1.05}.products-hero p{color:rgba(255,255,255,.53);font-size:12px}.add-product{display:flex;align-items:center;gap:8px;min-height:46px;padding:10px 18px;border:0;border-radius:999px;color:#1b1714;background:var(--route-amber);font-size:12px;font-weight:800}.catalog-card{overflow:hidden;border:1px solid rgba(23,23,23,.06);border-radius:22px;background:#fff;box-shadow:0 10px 35px rgba(42,28,20,.055)}.catalog-heading{display:flex;align-items:end;justify-content:space-between;padding:22px 24px 15px}.catalog-heading span{color:var(--primary);font-size:9px;font-weight:800;letter-spacing:.14em}.catalog-heading h2{margin:4px 0 0;font-size:20px}.catalog-heading p{color:var(--text-light);font-size:11px}.toolbar{display:grid;grid-template-columns:minmax(220px,2fr) repeat(5,minmax(120px,1fr)) auto;gap:10px;padding:0 24px 18px}.search-box{position:relative}.search-box i{position:absolute;top:50%;left:13px;transform:translateY(-50%);color:var(--text-light)}.search-box input{padding-left:36px}.state{display:flex;min-height:280px;align-items:center;justify-content:center;flex-direction:column;gap:10px;color:var(--text-light);text-align:center}.state-error{color:var(--red-active)}.spinner{width:28px;height:28px;border:3px solid var(--border-light);border-top-color:var(--primary);border-radius:50%;animation:spin .8s linear infinite}.pagination{display:flex;align-items:center;justify-content:space-between;padding:16px 24px;color:var(--text-mid);font-size:13px}.pagination div,.row-actions,.dialog-actions{display:flex;align-items:center;gap:10px}.product-name{display:flex;flex-direction:column}.product-thumb{width:48px;height:48px;border-radius:12px;object-fit:cover}.dialog-overlay{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.55)}.hide-dialog{width:min(420px,100%);padding:24px;border-radius:18px;background:var(--surface);box-shadow:0 24px 70px rgba(15,23,42,.25)}.hide-dialog h2{margin:0 0 10px}.hide-dialog p{margin:0 0 22px;color:var(--text-mid)}.dialog-actions{justify-content:flex-end}.mobile-catalog{display:none}.product-mobile-card{display:grid;grid-template-columns:auto 1fr auto;gap:12px;align-items:center;padding:14px 16px;border-top:1px solid var(--border-light)}.mobile-product-main{display:flex;min-width:0;flex-direction:column;gap:3px}.mobile-product-main small,.mobile-product-main span{color:var(--text-mid);font-size:12px}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:1100px){.stats-grid{grid-template-columns:repeat(2,1fr)}.toolbar{grid-template-columns:repeat(2,1fr)}.search-box{grid-column:1/-1}}@media(max-width:700px){.desktop-catalog{display:none}.mobile-catalog{display:grid}.products-hero{align-items:flex-start;flex-direction:column;min-height:0;padding:25px}.add-product{width:100%;justify-content:center}.stats-grid,.toolbar{grid-template-columns:1fr}.search-box{grid-column:auto}.catalog-heading,.pagination{align-items:flex-start;flex-direction:column;gap:8px}}@media(max-width:430px){.stats-grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.spinner{animation:none}}
</style>
