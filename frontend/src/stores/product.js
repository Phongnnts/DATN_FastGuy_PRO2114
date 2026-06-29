import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { productApi } from '@/api';

const PLACEHOLDER_IMAGE =
  'https://placehold.co/400x300/FFF0E8/D4764A?text=FastGuy';

function ensureImage(url) {
  return url && url.trim() ? url : PLACEHOLDER_IMAGE;
}

function parsePrice(v) {
  return typeof v === 'string' ? parseFloat(v) : v;
}

export const useProductStore = defineStore('product', () => {
  const allProducts = ref([]);
  const allCategories = ref([]);
  const currentProduct = ref(null);
  const loading = ref(false);
  const error = ref('');
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

  const featuredProducts = computed(() =>
    allProducts.value.filter((p) => p.featured),
  );
  const inStockProducts = computed(() =>
    allProducts.value.filter((p) => p.inStock),
  );

  function mapProduct(p) {
    return {
      productId: p.productId,
      name: p.name,
      categoryId: p.categoryId,
      categoryName: p.categoryName || '',
      basePrice: parsePrice(p.basePrice),
      price: parsePrice(p.price),
      discountPrice: p.discountPrice || null,
      defaultVariant: p.defaultVariant || null,
      variants: p.variants || [],
      image: ensureImage(p.imageUrl),
      description: p.description || '',
      rating: p.rating || 0,
      reviewCount: p.reviewCount || 0,
      inStock: p.inStock !== undefined ? p.inStock : (p.status === 'AVAILABLE'),
      featured: p.featured || false,
      galleryImages: p.galleryImages || [],
    };
  }

  function mapCategory(c) {
    return {
      id: c.categoryId,
      name: c.name,
      slug: c.name
        ? c.name.toLowerCase().replace(/ \/ /g, '-').replace(/\s+/g, '-')
        : '',
      icon: c.icon || 'bi-grid',
      image: ensureImage(c.image || c.imageUrl),
      productCount: c.productCount || 0,
      description: c.description || '',
    };
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
      if (productsData && Array.isArray(productsData)) {
        allProducts.value = productsData.map(mapProduct);
      }
      if (categoriesData && Array.isArray(categoriesData)) {
        allCategories.value = categoriesData.map(mapCategory);
      }
      fetched.value = true;
    } catch (e) {
      error.value = e.message || 'Không thể tải dữ liệu';
    } finally {
      loading.value = false;
    }
  }

  async function fetchById(id) {
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
    searchQuery,
    selectedCategory,
    fetched,
    filteredProducts,
    featuredProducts,
    inStockProducts,
    fetchById,
    fetchByCategory,
    search,
    clearFilters,
    init,
  };
});
