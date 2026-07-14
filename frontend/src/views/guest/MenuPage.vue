<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useProductStore } from '@/stores/product';
import ProductCard from '@/components/common/ProductCard.vue';

const productStore = useProductStore();
const route = useRoute();
const searchInput = ref('');
const activeCategory = ref(null);
const activePrice = ref('ALL');
const sortBy = ref('name');
const viewMode = ref('grid');
const inStockOnly = ref(false);
const currentPage = ref(1);
const pageSize = 12;

const categoryIcons = ['bi-grid-3x3-gap', 'bi-egg-fried', 'bi-cup-straw', 'bi-fire', 'bi-pie-chart', 'bi-cup-hot'];
const priceFilters = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'UNDER_30', label: 'Dưới 30k' },
  { key: '30_60', label: '30k - 60k' },
  { key: 'OVER_60', label: 'Trên 60k' },
];

const categories = computed(() => [
  { id: null, name: 'Tất cả', icon: 'bi-grid-3x3-gap', count: productStore.allProducts.length },
  ...productStore.allCategories.map((category, index) => ({
    ...category,
    icon: categoryIcons[(index + 1) % categoryIcons.length],
    count: productStore.allProducts.filter((product) => product.categoryId === category.id).length,
  })),
]);

const filteredProducts = computed(() => {
  let result = [...productStore.allProducts];
  if (activeCategory.value) result = result.filter((product) => product.categoryId === activeCategory.value);
  if (searchInput.value) {
    const query = searchInput.value.toLowerCase();
    result = result.filter((product) => product.name.toLowerCase().includes(query) || (product.description && product.description.toLowerCase().includes(query)));
  }
  if (activePrice.value === 'UNDER_30') result = result.filter((product) => product.price < 30000);
  if (activePrice.value === '30_60') result = result.filter((product) => product.price >= 30000 && product.price <= 60000);
  if (activePrice.value === 'OVER_60') result = result.filter((product) => product.price > 60000);
  if (inStockOnly.value) result = result.filter((product) => product.inStock);
  if (sortBy.value === 'price-asc') result.sort((a, b) => a.price - b.price);
  if (sortBy.value === 'price-desc') result.sort((a, b) => b.price - a.price);
  if (sortBy.value === 'name') result.sort((a, b) => a.name.localeCompare(b.name));
  if (sortBy.value === 'newest') result.sort((a, b) => b.productId - a.productId);
  return result;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProducts.value.length / pageSize)));
const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProducts.value.slice(start, start + pageSize);
});

const pageNumbers = computed(() => {
  const total = totalPages.value;
  if (total <= 5) return Array.from({ length: total }, (_, index) => index + 1);
  const page = currentPage.value;
  if (page <= 3) return [1, 2, 3, '...', total];
  if (page >= total - 2) return [1, '...', total - 2, total - 1, total];
  return [1, '...', page, '...', total];
});

function goPage(page) {
  if (page >= 1 && page <= totalPages.value) currentPage.value = page;
}

watch([filteredProducts, activeCategory, searchInput, activePrice, inStockOnly], () => { currentPage.value = 1; });

onMounted(async () => {
  if (!productStore.fetched) await productStore.init();
  if (route.query.category) {
    const category = productStore.allCategories.find((item) => item.id === Number(route.query.category));
    if (category) activeCategory.value = category.id;
  }
});
</script>

<template>
  <div class="menu-page">
    <div class="container">
      <div class="menu-breadcrumb"><router-link to="/">Trang chủ</router-link><i class="bi bi-chevron-right"></i><span>Thực đơn</span></div>
      <div class="menu-heading">
        <div>
          <span class="route-chip">Hot Route Menu</span>
          <h1>Thực đơn</h1>
          <p>Khám phá đồ ăn ngon, đặt ngay!</p>
        </div>
        <div class="menu-search search-box">
          <i class="bi bi-search"></i>
          <input v-model="searchInput" type="text" class="form-input" placeholder="Tìm kiếm món ăn..." />
        </div>
      </div>

      <div class="menu-layout">
        <aside class="menu-sidebar">
          <section class="filter-panel">
            <h2>Danh mục</h2>
            <button
              v-for="category in categories"
              :key="category.id ?? 'all'"
              class="category-row"
              :class="{ active: activeCategory === category.id }"
              @click="activeCategory = category.id"
            >
              <i :class="'bi ' + category.icon"></i>
              <span>{{ category.name }}</span>
              <small>({{ category.count }})</small>
            </button>
          </section>

          <section class="filter-panel">
            <h2>Khoảng giá</h2>
            <label v-for="filter in priceFilters" :key="filter.key" class="filter-radio">
              <input v-model="activePrice" type="radio" :value="filter.key" />
              <span>{{ filter.label }}</span>
            </label>
          </section>

          <section class="filter-panel">
            <h2>Sắp xếp theo</h2>
            <select v-model="sortBy" class="form-select">
              <option value="name">Tên A-Z</option>
              <option value="newest">Mới nhất</option>
              <option value="price-asc">Giá thấp đến cao</option>
              <option value="price-desc">Giá cao đến thấp</option>
            </select>
            <label class="stock-toggle">
              <input v-model="inStockOnly" type="checkbox" />
              <span>Chỉ món còn hàng</span>
            </label>
          </section>
        </aside>

        <main class="menu-content">
          <div class="menu-result-header">
            <strong>Hiển thị {{ filteredProducts.length }} món ăn</strong>
            <div class="view-toggle">
              <button :class="{ active: viewMode === 'grid' }" title="Dạng lưới" @click="viewMode = 'grid'"><i class="bi bi-grid-3x3-gap"></i></button>
              <button :class="{ active: viewMode === 'list' }" title="Dạng danh sách" @click="viewMode = 'list'"><i class="bi bi-list"></i></button>
            </div>
          </div>

          <div v-if="productStore.loading" class="menu-grid">
            <div v-for="number in 12" :key="number" class="skeleton-card"></div>
          </div>
          <div v-else-if="filteredProducts.length === 0" class="empty-state">
            <i class="bi bi-search"></i>
            <h3>Không tìm thấy món ăn</h3>
            <p>Thử tìm kiếm với từ khóa hoặc bộ lọc khác.</p>
          </div>
          <template v-else>
            <div :class="viewMode === 'grid' ? 'menu-grid' : 'product-list'">
              <ProductCard v-for="product in pagedProducts" :key="product.productId" :product="product" :listMode="viewMode === 'list'" />
            </div>
            <div v-if="totalPages > 1" class="pagination">
              <button :disabled="currentPage <= 1" @click="goPage(currentPage - 1)"><i class="bi bi-chevron-left"></i></button>
              <button v-for="page in pageNumbers" :key="page" :class="{ active: page === currentPage }" :disabled="page === '...'" @click="goPage(page)">{{ page }}</button>
              <button :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)"><i class="bi bi-chevron-right"></i></button>
            </div>
          </template>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.menu-page { padding: 28px 0 56px; background: #fff8f0; min-height: 100vh; }
.menu-breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 600; color: var(--text-mid); }
.menu-breadcrumb a { color: var(--text-dark); }
.menu-breadcrumb i { color: var(--text-light); font-size: 10px; }
.menu-heading { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 10px 0 28px; }
.menu-heading h1 { font-size: clamp(30px, 4vw, 42px); font-weight: 900; line-height: 1; letter-spacing: -0.06em; margin: 12px 0 8px; }
.menu-heading p { font-size: 15px; color: var(--text-mid); }
.menu-search { width: min(340px, 100%); }
.menu-layout { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 24px; align-items: start; }
.menu-sidebar { position: sticky; top: 78px; display: grid; gap: 16px; }
.filter-panel { background: rgba(255,255,255,0.86); border: 1px solid rgba(232,115,74,0.09); border-radius: var(--radius-lg); padding: 14px 12px; box-shadow: var(--shadow-xs); }
.filter-panel h2 { text-transform: uppercase; font-size: 12px; letter-spacing: 0.06em; font-weight: 800; margin: 0 0 10px; }
.category-row { width: 100%; display: grid; grid-template-columns: 18px 1fr auto; gap: 8px; align-items: center; padding: 9px 10px; border-radius: var(--radius-sm); color: var(--text-dark); font-size: 13px; text-align: left; transition: all var(--transition-fast); }
.category-row i { font-size: 13px; }
.category-row small { font-size: 11px; color: var(--text-mid); }
.category-row:hover { background: var(--primary-50); }
.category-row.active { background: var(--primary); color: #fff; font-weight: 700; }
.category-row.active small { color: #fff; }
.filter-radio, .stock-toggle { display: flex; align-items: center; gap: 9px; margin: 10px 0; font-size: 13px; color: var(--text-mid); cursor: pointer; }
.filter-radio input, .stock-toggle input { accent-color: var(--primary); }
.filter-panel .form-select { padding: 8px 10px; font-size: 13px; }
.stock-toggle { margin: 14px 0 2px; color: var(--text-dark); }
.menu-result-header { display: flex; align-items: center; justify-content: space-between; margin: 2px 0 16px; font-size: 14px; }
.view-toggle { display: flex; gap: 4px; }
.view-toggle button { width: 36px; height: 36px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; color: var(--text-mid); transition: all var(--transition-fast); }
.view-toggle button.active { background: var(--primary); border-color: var(--primary); color: #fff; }
.menu-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.skeleton-card { height: 308px; border-radius: var(--radius-lg); background: linear-gradient(90deg, #f3eee9 25%, #fff 50%, #f3eee9 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.product-list { display: grid; gap: 12px; }
@keyframes shimmer { from { background-position: -200% 0; } to { background-position: 200% 0; } }
@media (max-width: 1024px) { .menu-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 768px) { .menu-heading { align-items: stretch; flex-direction: column; } .menu-search { width: 100%; } .menu-layout { grid-template-columns: 1fr; } .menu-sidebar { position: static; grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 560px) { .menu-sidebar { grid-template-columns: 1fr; } .menu-grid { grid-template-columns: 1fr; } }
</style>
