import { defineStore } from 'pinia';
import { ref } from 'vue';
import { homepageApi } from '@/api';
import { mapHomepageProduct } from '@/utils/homepage';

export const useHomepageStore = defineStore('homepage', () => {
  const bestSellers = ref([]);
  const featuredReviews = ref([]);
  const loading = ref(false);
  const error = ref('');
  let requestGeneration = 0;

  async function load() {
    const request = ++requestGeneration;
    loading.value = true;
    error.value = '';
    try {
      const data = await homepageApi.get();
      if (request !== requestGeneration) return;
      bestSellers.value = Array.isArray(data?.bestSellers) ? data.bestSellers.map(mapHomepageProduct) : [];
      featuredReviews.value = Array.isArray(data?.featuredReviews) ? data.featuredReviews : [];
    } catch (cause) {
      if (request !== requestGeneration) return;
      bestSellers.value = [];
      featuredReviews.value = [];
      error.value = cause.message || 'Không thể tải nội dung trang chủ';
    } finally {
      if (request === requestGeneration) loading.value = false;
    }
  }

  async function retry() {
    await load();
  }

  return { bestSellers, featuredReviews, loading, error, load, retry };
});
