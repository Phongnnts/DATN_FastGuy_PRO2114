<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useProductStore } from '@/stores/product';
import ProductCard from '@/components/common/ProductCard.vue';

const productStore = useProductStore();
const route = useRoute();
const searchInput = ref('');
const activeCategory = ref(null);
const sortBy = ref('name');
const viewMode = ref('grid');
const priceRange = ref([0, 999999]);
const inStockOnly = ref(false);
const currentPage = ref(1);
const pageSize = 10;

const categories = computed(() => [
  { id: null, name: 'Tất cả' },
  ...productStore.allCategories,
]);

const filteredProducts = computed(() => {
  let result = productStore.allProducts;
  if (activeCategory.value) {
    result = result.filter((p) => p.categoryId === activeCategory.value);
  }
  if (searchInput.value) {
    const q = searchInput.value.toLowerCase();
    result = result.filter((p) =>
      p.name.toLowerCase().includes(q) ||
      (p.description && p.description.toLowerCase().includes(q))
    );
  }
  if (inStockOnly.value) {
    result = result.filter(p => p.inStock);
  }
  result = result.filter(p => p.price >= priceRange.value[0] && p.price <= priceRange.value[1]);
  if (sortBy.value === 'price-asc') result.sort((a,b) => a.price - b.price);
  if (sortBy.value === 'price-desc') result.sort((a,b) => b.price - a.price);
  if (sortBy.value === 'name') result.sort((a,b) => a.name.localeCompare(b.name));
  if (sortBy.value === 'newest') result.sort((a,b) => b.productId - a.productId);
  return result;
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredProducts.value.length / pageSize)),
);

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProducts.value.slice(start, start + pageSize);
});

const pageNumbers = computed(() => {
  const total = totalPages.value;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const cp = currentPage.value;
  if (cp <= 3) return [1, 2, 3, 4, '...', total];
  if (cp >= total - 2)
    return [1, '...', total - 3, total - 2, total - 1, total];
  return [1, '...', cp - 1, cp, cp + 1, '...', total];
});

function goPage(p) {
  if (p >= 1 && p <= totalPages.value) currentPage.value = p;
}

watch([filteredProducts, activeCategory, searchInput], () => {
  currentPage.value = 1;
});

onMounted(async () => {
  if (!productStore.fetched) await productStore.init();
  if (route.query.category) {
    const cat = productStore.allCategories.find(
      (c) => c.id === Number(route.query.category)
    );
    if (cat) activeCategory.value = cat.id;
  }
});

function selectCategory(id) {
  activeCategory.value = id;
}
</script>

<template>
  <div class="menu-page">
    <div class="container">
      <div class="page-header">
        <h1>Thực đơn</h1>
        <p>Khám phá các món ăn ngon tại FastGuy</p>
      </div>
      <div class="menu-toolbar" style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <div class="search-box" style="flex:1;min-width:200px">
          <i class="bi bi-search"></i>
          <input v-model="searchInput" type="text" class="form-input" placeholder="Tìm món ăn..." />
        </div>
        <select v-model="sortBy" class="form-select" style="width:auto">
          <option value="name">Tên A-Z</option>
          <option value="price-asc">Giá thấp→cao</option>
          <option value="price-desc">Giá cao→thấp</option>
          <option value="newest">Mới nhất</option>
        </select>
        <label style="display:flex;align-items:center;gap:4px;font-size:13px;white-space:nowrap">
          <input type="checkbox" v-model="inStockOnly" /> Còn hàng
        </label>
        <div class="view-toggle" style="display:flex;gap:4px">
          <button class="btn btn-sm" :class="viewMode === 'grid' ? 'btn-primary' : 'btn-outline'" @click="viewMode = 'grid'"><i class="bi bi-grid-3x3-gap"></i></button>
          <button class="btn btn-sm" :class="viewMode === 'list' ? 'btn-primary' : 'btn-outline'" @click="viewMode = 'list'"><i class="bi bi-list"></i></button>
        </div>
      </div>
      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat.id ?? 'all'"
          class="category-tab"
          :class="{ active: activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>
      <div v-if="filteredProducts.length === 0" class="empty-state">
        <i class="bi bi-search"></i>
        <h3>Không tìm thấy món ăn</h3>
        <p>Thử tìm kiếm với từ khóa khác</p>
      </div>
      <template v-else>
        <div :class="viewMode === 'grid' ? 'grid-5' : 'product-list'">
          <ProductCard
            v-for="product in pagedProducts"
            :key="product.productId"
            :product="product"
            :listMode="viewMode === 'list'"
          />
        </div>
        <div class="pagination">
          <button :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">
            <i class="bi bi-chevron-left"></i>
          </button>
          <button
            v-for="p in pageNumbers"
            :key="p"
            :class="{ active: p === currentPage, 'page-ellipsis': p === '...' }"
            :disabled="p === '...'"
            @click="goPage(p)"
          >
            {{ p }}
          </button>
          <button
            :disabled="currentPage >= totalPages"
            @click="goPage(currentPage + 1)"
          >
            <i class="bi bi-chevron-right"></i>
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.menu-page {
  padding: 24px 0;
}
.menu-toolbar {
  margin-bottom: 20px;
}
.category-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}
.category-tab {
  padding: 7px 18px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.category-tab:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.category-tab.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
</style>
