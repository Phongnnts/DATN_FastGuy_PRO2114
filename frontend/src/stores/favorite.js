import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { favoriteApi } from '@/api';

export const useFavoriteStore = defineStore('favorite', () => {
  const items = ref([]);
  const ids = ref(new Set());
  const loading = ref(false);

  const count = computed(() => ids.value.size);

  function sync(products) {
    items.value = products || [];
    ids.value = new Set(items.value.map((item) => item.productId));
  }

  async function fetchFavorites() {
    loading.value = true;
    try {
      const data = await favoriteApi.getAll();
      sync(Array.isArray(data) ? data : []);
    } finally {
      loading.value = false;
    }
  }

  async function check(productId) {
    const data = await favoriteApi.check(productId);
    if (data.favorite) ids.value = new Set([...ids.value, Number(productId)]);
    return data.favorite;
  }

  async function toggle(product) {
    const productId = Number(product.productId || product);
    const data = await favoriteApi.toggle(productId);
    const next = new Set(ids.value);
    if (data.favorite) {
      next.add(productId);
      if (product.productId && !items.value.some((item) => item.productId === productId)) {
        items.value = [product, ...items.value];
      }
    } else {
      next.delete(productId);
      items.value = items.value.filter((item) => item.productId !== productId);
    }
    ids.value = next;
    return data.favorite;
  }

  function isFavorite(productId) {
    return ids.value.has(Number(productId));
  }

  function clear() {
    items.value = [];
    ids.value = new Set();
  }

  return { items, ids, loading, count, fetchFavorites, check, toggle, isFavorite, clear };
});
