<script setup>
import { onMounted, computed } from 'vue';
import { useProductStore } from '@/stores/product';
import HeroBanner from '@/components/guest/HeroBanner.vue';
import CategoryGrid from '@/components/guest/CategoryGrid.vue';
import FeaturedProducts from '@/components/guest/FeaturedProducts.vue';
import ProductCard from '@/components/common/ProductCard.vue';

const productStore = useProductStore();

const previewProducts = computed(() => productStore.allProducts.slice(0, 10));

onMounted(() => {
  if (!productStore.fetched) productStore.init();
});
</script>

<template>
  <div>
    <HeroBanner />
    <div v-if="productStore.loading" class="loading-section">
      <div class="container">
        <p class="loading-text">Đang tải dữ liệu...</p>
      </div>
    </div>
    <div v-else-if="productStore.error" class="loading-section">
      <div class="container">
        <p class="error-text">{{ productStore.error }}</p>
      </div>
    </div>
    <template v-else>
      <CategoryGrid :categories="productStore.allCategories" />

      <section class="section preview-section">
        <div class="container">
          <div class="section-header">
            <h2 class="section-title">Thực đơn hôm nay</h2>
            <router-link to="/menu" class="section-all"
              >Xem tất cả <i class="bi bi-arrow-right"></i
            ></router-link>
          </div>
          <div class="grid-5">
            <ProductCard
              v-for="p in previewProducts"
              :key="p.productId"
              :product="p"
            />
          </div>
        </div>
      </section>

      <section class="section about-section">
        <div class="container">
          <h2 class="section-title text-center">Vì sao chọn FastGuy?</h2>
          <div class="about-grid">
            <div class="about-card">
              <div class="about-icon"><i class="bi bi-clock"></i></div>
              <h3>Giao hàng siêu tốc</h3>
              <p>
                Chỉ 30 phút từ bếp nóng đến tay bạn, đảm bảo đồ ăn luôn nóng
                hổi.
              </p>
            </div>
            <div class="about-card">
              <div class="about-icon"><i class="bi bi-basket2"></i></div>
              <h3>Nguyên liệu tươi ngon</h3>
              <p>
                Chúng tôi chọn lọc kỹ từng nguyên liệu, cam kết chất lượng và an
                toàn vệ sinh.
              </p>
            </div>
            <div class="about-card">
              <div class="about-icon"><i class="bi bi-emoji-smile"></i></div>
              <h3>Đa dạng món ăn</h3>
              <p>
                200+ món ngon từ burger, pizza, gà rán đến nước uống, đáp ứng
                mọi khẩu vị.
              </p>
            </div>
            <div class="about-card">
              <div class="about-icon"><i class="bi bi-wallet2"></i></div>
              <h3>Giá cả hợp lý</h3>
              <p>
                Mức giá cạnh tranh cùng nhiều ưu đãi hấp dẫn cho khách hàng thân
                thiết.
              </p>
            </div>
          </div>
        </div>
      </section>

      <FeaturedProducts
        v-if="productStore.featuredProducts.length"
        :products="productStore.featuredProducts"
      />
    </template>
  </div>
</template>

<style scoped>
.loading-section {
  padding: 40px 0;
  text-align: center;
}
.loading-text {
  color: var(--text-mid);
  font-size: 15px;
}
.error-text {
  color: var(--red-active);
  font-size: 14px;
}

.preview-section {
  padding: 40px 0;
  background: var(--bg);
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

.about-section {
  padding: 56px 0;
}
.about-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 28px;
}
.about-card {
  text-align: center;
  padding: 24px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.about-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  background: var(--primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--primary);
}
.about-card h3 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.about-card p {
  font-size: 13px;
  color: var(--text-mid);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .about-grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 480px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
