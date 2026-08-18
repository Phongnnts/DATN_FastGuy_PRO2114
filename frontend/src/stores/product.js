import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { productApi } from '@/api';
import { mapProduct } from '@/utils/productMapper';

export const useProductStore = defineStore('product', () => {
  const allProducts = ref([]);
  const allCategories = ref([]);
  const currentProduct = ref(null);
  const loading = ref(false);
  const error = ref('');
  const catalog = ref([]);
  const catalogLoading = ref(false);
  const catalogError = ref('');
  const catalogMeta = ref({ totalItems: 0, totalPages: 0, page: 0, size: 12 });
  let catalogRequest = 0;
  const searchQuery = ref('');
  const selectedCategory = ref(null);
  const fetched = ref(false);

  const filteredProducts = computed(() => {
    let result = allProducts.value;
    if (selectedCategory.value) {
      result = result.filter((p) => p.categoryId === selectedCategory.value);
    }
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase();
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          (p.description && p.description.toLowerCase().includes(q)),
      );
    }
    return result;
  });

  const featured = ref([]);
  const bestSellers = ref([]);
  const featuredLoading = ref(false);
  const featuredError = ref('');
  const featuredProducts = computed(() => featured.value.length ? featured.value : allProducts.value
    .filter((p) => p.inStock && p.isAvailableNow !== false)
    .sort((a, b) => b.productId - a.productId)
    .slice(0, 8));
  const inStockProducts = computed(() =>
    allProducts.value.filter((p) => p.inStock),
  );

  function mapCategory(c) {
    return {
      id: c.categoryId,
      name: c.name,
      slug: c.name
        ? c.name.toLowerCase().replace(/ \/ /g, '-').replace(/\s+/g, '-')
        : '',
      icon: c.icon || 'bi-grid',
      productCount: c.productCount || 0,
      description: c.description || '',
    };
  }

  function listFrom(data) {
    if (Array.isArray(data)) return data;
    return data?.content || data?.items || data?.data || [];
  }

  async function fetchCatalog(params = {}) {
    const request = ++catalogRequest;
    catalogLoading.value = true;
    catalogError.value = '';
    try {
      const data = await productApi.getAll(params);
      if (request !== catalogRequest) return;
      catalog.value = listFrom(data).map(mapProduct);
      catalogMeta.value = {
        totalItems: Number(data?.totalItems) || 0,
        totalPages: Number(data?.totalPages) || 0,
        page: Number(data?.page) || 0,
        size: Number(data?.size) || Number(params.size) || 12,
      };
    } catch (e) {
      if (request !== catalogRequest) return;
      catalog.value = [];
      catalogError.value = e.message || 'Không thể tải thực đơn';
    } finally {
      if (request === catalogRequest) catalogLoading.value = false;
    }
  }

  async function fetchFeatured() {
    featuredLoading.value = true;
    featuredError.value = '';
    try {
      bestSellers.value = listFrom(await productApi.getBestSellers()).map(mapProduct);
      featured.value = bestSellers.value;
    } catch (e) {
      bestSellers.value = [];
      featured.value = [];
      featuredError.value = e.message || 'Không thể tải món bán chạy';
    } finally {
      featuredLoading.value = false;
    }
  }

  async function init() {
    if (fetched.value) return;
    loading.value = true;
    error.value = '';
    try {
      const [productsData, categoriesData] = await Promise.all([
        productApi.getAll(),
        productApi.getCategories(),
      ]);
      allProducts.value = listFrom(productsData).map(mapProduct);
      allCategories.value = listFrom(categoriesData).map(mapCategory);
      fetched.value = true;
    } catch (e) {
      error.value = e.message || 'Không thể tải dữ liệu';
    } finally {
      loading.value = false;
    }
  }

  async function fetchById(id) {
    currentProduct.value = null;
    const local = allProducts.value.find((p) => p.productId === Number(id));
    if (local) {
      currentProduct.value = local;
    }
    try {
      const data = await productApi.getById(id);
      if (data) {
        currentProduct.value = mapProduct(data);
      }
    } catch {
      if (!currentProduct.value) {
        currentProduct.value = null;
      }
    }
    return currentProduct.value;
  }

  function fetchByCategory(categoryId) {
    selectedCategory.value = categoryId;
  }

  function search(query) {
    searchQuery.value = query;
  }

  function clearFilters() {
    searchQuery.value = '';
    selectedCategory.value = null;
  }

  return {
    allProducts,
    allCategories,
    currentProduct,
    loading,
    error,
    catalog,
    catalogLoading,
    catalogError,
    catalogMeta,
    searchQuery,
    selectedCategory,
    fetched,
    filteredProducts,
    featured,
    bestSellers,
    featuredProducts,
    featuredLoading,
    featuredError,
    inStockProducts,
    fetchById,
    fetchByCategory,
    search,
    clearFilters,
    fetchCatalog,
    fetchFeatured,
    init,
  };
});
