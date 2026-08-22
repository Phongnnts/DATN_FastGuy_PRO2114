import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { favoriteApi, productApi } from '@/api';
import { createFavoriteLoadController } from '@/utils/favoriteHydration';

export const useFavoriteStore = defineStore('favorite', () => {
  const items = ref([]);
  const ids = ref(new Set());
  const loading = ref(false);
  const error = ref('');
  const warning = ref('');
  let generation = 0;
  const toggleSequences = new Map();

  const count = computed(() => ids.value.size);

  function sync(products) {
    items.value = products || [];
    ids.value = new Set(items.value.map((item) => item.productId));
  }

  const loader = createFavoriteLoadController({
    getFavorites: favoriteApi.getAll,
    getCatalog: () => productApi.getAll(),
    apply: sync,
    fail: (e) => { error.value = e.message || 'Không thể tải món yêu thích.'; },
    warn: () => { warning.value = 'Một số thông tin món ăn chưa được cập nhật. Bạn vẫn có thể xem danh sách yêu thích.'; },
    setLoading: (value) => { loading.value = value; },
  });

  async function fetchFavorites() {
    error.value = '';
    warning.value = '';
    await loader.load();
  }

  async function check(productId) {
    const requestGeneration = generation;
    const data = await favoriteApi.check(productId);
    if (requestGeneration !== generation) return undefined;
    if (data.favorite) ids.value = new Set([...ids.value, Number(productId)]);
    return data.favorite;
  }

  async function toggle(product) {
    const productId = Number(product.productId || product);
    const requestGeneration = generation;
    const sequence = (toggleSequences.get(productId) || 0) + 1;
    toggleSequences.set(productId, sequence);
    const data = await favoriteApi.toggle(productId);
    if (requestGeneration !== generation || toggleSequences.get(productId) !== sequence) return undefined;
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
    generation += 1;
    toggleSequences.clear();
    loader.invalidate();
    items.value = [];
    ids.value = new Set();
    error.value = '';
    warning.value = '';
  }

  return { items, ids, loading, error, warning, count, fetchFavorites, check, toggle, isFavorite, clear };
});
