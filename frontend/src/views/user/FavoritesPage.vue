<script setup>
import { ref, computed } from 'vue'
import { useProductStore } from '@/stores/product'
import ProductCard from '@/components/common/ProductCard.vue'

const productStore = useProductStore()
const favorites = ref(JSON.parse(localStorage.getItem('favorites') || '[]'))

const favoriteProducts = computed(() =>
  productStore.allProducts.filter(p => favorites.value.includes(p.productId))
)

function toggleFavorite(productId) {
  const idx = favorites.value.indexOf(productId)
  if (idx >= 0) favorites.value.splice(idx, 1)
  else favorites.value.push(productId)
  localStorage.setItem('favorites', JSON.stringify(favorites.value))
}
</script>

<template>
  <div class="favorites-page">
    <div class="card">
      <h3 style="font-size:18px;font-weight:700;margin-bottom:20px">Món yêu thích</h3>
      <div v-if="favoriteProducts.length === 0" class="empty-state" style="padding:40px 0">
        <i class="bi bi-heart"></i>
        <h3>Chưa có món yêu thích</h3>
        <p>Thêm món ăn vào danh sách yêu thích nhé!</p>
        <router-link to="/menu" class="btn btn-primary">Khám phá thực đơn</router-link>
      </div>
      <div v-else class="grid-4">
        <div v-for="product in favoriteProducts" :key="product.productId" style="position:relative">
          <ProductCard :product="product" />
          <button class="fav-btn" @click="toggleFavorite(product.productId)"><i class="bi bi-heart-fill"></i></button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fav-btn { position: absolute; top: 8px; right: 8px; width: 36px; height: 36px; border-radius: 25px; border: none; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 16px; color: var(--red-active); z-index: 2; }
.fav-btn:hover { background: #fff; }
</style>
