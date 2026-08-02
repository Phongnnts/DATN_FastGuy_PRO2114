<script setup>
import { onMounted } from 'vue';
import { useFavoriteStore } from '@/stores/favorite';
import ProductCard from '@/components/common/ProductCard.vue';

const favoriteStore = useFavoriteStore();

onMounted(() => favoriteStore.fetchFavorites());
</script>

<template>
  <div class="favorites-page">
    <div class="page-header">
      <div>
        <h1>Món yêu thích</h1>
        <p>Lưu lại món ngon để đặt nhanh hơn</p>
      </div>
      <router-link to="/menu" class="btn btn-primary">Khám phá thêm</router-link>
    </div>

    <div v-if="favoriteStore.loading" class="empty-state">
      <i class="bi bi-arrow-repeat spin"></i>
      <h3>Đang tải món yêu thích...</h3>
    </div>

    <div v-else-if="favoriteStore.error" class="empty-state card" role="alert">
      <i class="bi bi-exclamation-circle"></i>
      <h3>Không thể tải món yêu thích</h3>
      <p>{{ favoriteStore.error }}</p>
      <button type="button" class="btn btn-outline" @click="favoriteStore.fetchFavorites">Thử lại</button>
    </div>

    <div v-else-if="favoriteStore.items.length === 0" class="empty-state card">
      <i class="bi bi-heart"></i>
      <h3>Chưa có món yêu thích</h3>
      <p>Bấm trái tim trên món ăn để lưu vào danh sách này.</p>
      <router-link to="/menu" class="btn btn-primary">Xem thực đơn</router-link>
    </div>

    <div v-else class="grid-4">
      <ProductCard v-for="product in favoriteStore.items" :key="product.productId" :product="product" />
    </div>
  </div>
</template>

<style scoped>
.favorites-page {
  padding-bottom: 24px;
}
.page-header h1 {
  font-size: 24px;
  font-weight: 900;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
