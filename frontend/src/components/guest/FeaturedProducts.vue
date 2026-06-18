<script setup>
import ProductCard from '@/components/common/ProductCard.vue';

defineProps({
  products: { type: Array, required: true },
});
</script>

<template>
  <section class="section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">Món nổi bật</h2>
        <router-link to="/menu" class="section-all"
          >Xem tất cả <i class="bi bi-arrow-right"></i
        ></router-link>
      </div>
      <div class="grid-5">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.section {
  padding: 40px 0;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-title {
  font-size: 18px;
  font-weight: 700;
}
.section-all {
  font-size: 14px;
  color: var(--primary);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
