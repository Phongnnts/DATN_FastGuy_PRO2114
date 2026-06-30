<script setup>
import { ref, onMounted, computed } from 'vue';
import { useProductStore } from '@/stores/product';
import HeroBanner from '@/components/guest/HeroBanner.vue';
import CategoryGrid from '@/components/guest/CategoryGrid.vue';
import FeaturedProducts from '@/components/guest/FeaturedProducts.vue';
import ProductCard from '@/components/common/ProductCard.vue';

const productStore = useProductStore();
const newsletterEmail = ref('');
const currentSlide = ref(0);

const previewProducts = computed(() => productStore.allProducts.slice(0, 10));
const newProducts = computed(() => [...productStore.allProducts].sort((a,b) => b.productId - a.productId).slice(0, 8));

const heroSlides = [
  { title: 'Burger Ngon Mỗi Ngày', subtitle: 'Khám phá bộ sưu tập burger mới', cta: 'Xem thực đơn', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg' },
  { title: 'Giảm 20% Đơn Đầu Tiên', subtitle: 'Nhập mã FASTGUY20 khi thanh toán', cta: 'Đặt ngay', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/GaRan/gran-split.jpg' },
  { title: 'Giao Hàng Siêu Tốc 30 Phút', subtitle: 'Nóng hổi từ bếp đến tay bạn', cta: 'Tìm hiểu thêm', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Pizza/pizza-margherita.jpg' },
];

let slideInterval;
onMounted(() => {
  if (!productStore.fetched) productStore.init();
  slideInterval = setInterval(() => { currentSlide.value = (currentSlide.value + 1) % heroSlides.length; }, 5000);
});

function subscribeNewsletter() {
  if (!newsletterEmail.value) return;
  alert('Cảm ơn bạn đã đăng ký nhận tin!');
  newsletterEmail.value = '';
}
</script>

<template>
  <div>
    <!-- Hero Carousel -->
    <section class="hero-carousel">
      <div v-for="(slide, i) in heroSlides" :key="i" class="hero-slide" :class="{ active: currentSlide === i }" :style="{ backgroundImage: 'url(' + slide.image + ')' }">
        <div class="hero-overlay"></div>
        <div class="container hero-content">
          <h1 class="hero-title">{{ slide.title }}</h1>
          <p class="hero-subtitle">{{ slide.subtitle }}</p>
          <router-link :to="slide.link" class="btn btn-lg btn-primary" style="background:#fff;color:var(--primary);border:none">{{ slide.cta }}</router-link>
        </div>
      </div>
      <div class="carousel-dots">
        <button v-for="(_, i) in heroSlides" :key="i" class="dot" :class="{ active: currentSlide === i }" @click="currentSlide = i"></button>
      </div>
    </section>
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

      <section v-if="newProducts.length" class="section new-section">
        <div class="container">
          <div class="section-header">
            <h2 class="section-title"><i class="bi bi-star" style="color:var(--primary)"></i> Món mới</h2>
            <router-link to="/menu" class="section-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link>
          </div>
          <div class="grid-5">
            <ProductCard v-for="p in newProducts" :key="'new-' + p.productId" :product="p" />
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

      <section class="section newsletter-section">
        <div class="container newsletter-box">
          <h2>Đăng ký nhận tin</h2>
          <p>Nhận ưu đãi và món mới mỗi tuần</p>
          <form @submit.prevent="subscribeNewsletter" class="newsletter-form">
            <input v-model="newsletterEmail" type="email" class="form-input" placeholder="Email của bạn..." required />
            <button type="submit" class="btn btn-primary">Đăng ký</button>
          </form>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.loading-section { padding: 40px 0; text-align: center; }
.loading-text { color: var(--text-mid); font-size: 15px; }
.error-text { color: var(--red-active); font-size: 14px; }
.preview-section { padding: 40px 0; background: var(--bg); }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 18px; font-weight: 700; }
.section-all { font-size: 14px; color: var(--primary); font-weight: 500; display: flex; align-items: center; gap: 4px; }
.about-section { padding: 56px 0; }
.about-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-top: 28px; }
.about-card { text-align: center; padding: 24px 16px; border: 1px solid var(--border); border-radius: var(--radius); transition: all var(--transition-normal); }
.about-card:hover { box-shadow: var(--shadow-md); transform: translateY(-4px); border-color: var(--primary); }
.about-icon { width: 56px; height: 56px; margin: 0 auto 12px; background: var(--primary-light); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; color: var(--primary); }
.about-card h3 { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.about-card p { font-size: 13px; color: var(--text-mid); line-height: 1.5; }

.hero-carousel { position: relative; color: #fff; min-height: 360px; overflow: hidden; }
.hero-slide { position: absolute; inset: 0; display: flex; align-items: center; opacity: 0; transition: opacity 0.6s ease; background-size: cover; background-position: center; }
.hero-slide.active { opacity: 1; position: relative; }
.hero-overlay { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(0,0,0,0.6), rgba(0,0,0,0.3)); }
.hero-content { padding: 60px 0; text-align: center; position: relative; z-index: 1; }
.hero-title { font-size: 36px; font-weight: 800; margin-bottom: 12px; }
.hero-subtitle { font-size: 16px; opacity: 0.9; margin-bottom: 24px; }
.carousel-dots { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); display: flex; gap: 8px; }
.dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.6); background: transparent; cursor: pointer; }
.dot.active { background: #fff; border-color: #fff; }
.new-section { padding: 40px 0; background: #FFFBF5; }
.newsletter-section { padding: 60px 0; }
.newsletter-box { background: var(--primary-light); border-radius: var(--radius-lg); padding: 40px; text-align: center; }
.newsletter-box h2 { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
.newsletter-box p { color: var(--text-mid); margin-bottom: 20px; }
.newsletter-form { display: flex; gap: 8px; max-width: 440px; margin: 0 auto; }
.newsletter-form input { flex: 1; }

@media (max-width: 768px) {
  .about-grid { grid-template-columns: 1fr 1fr; }
  .hero-title { font-size: 24px; }
  .newsletter-form { flex-direction: column; }
}
@media (max-width: 480px) { .about-grid { grid-template-columns: 1fr; } }
</style>
