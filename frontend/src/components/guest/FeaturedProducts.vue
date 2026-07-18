<script setup>
import ProductCard from '@/components/common/ProductCard.vue';

defineProps({
  products: { type: Array, required: true },
});
</script>

<template>
  <section class="section">
    <div class="container">
      <div class="section-head">
        <h2>Món nổi bật</h2>
        <router-link to="/menu" class="see-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link>
      </div>
      <div class="grid-5">
        <ProductCard v-for="product in products" :key="product.productId" :product="product" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.section { padding: 48px 0; }
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.section-head h2 { font-size: 20px; font-weight: 700; }
.see-all { font-size: 14px; color: var(--primary); font-weight: 600; display: flex; align-items: center; gap: 4px; }
</style>
