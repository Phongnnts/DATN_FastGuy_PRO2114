<script setup>
import { ref, onMounted, computed } from 'vue';
import { useProductStore } from '@/stores/product';
import CategoryGrid from '@/components/guest/CategoryGrid.vue';
import FeaturedProducts from '@/components/guest/FeaturedProducts.vue';
import ProductCard from '@/components/common/ProductCard.vue';
import bannerApi from '@/api/banner';

const productStore = useProductStore();
const newsletterEmail = ref('');
const currentSlide = ref(0);
const banners = ref([]);

const previewProducts = computed(() => productStore.allProducts.slice(0, 10));
const newProducts = computed(() => [...productStore.allProducts].sort((a,b) => b.productId - a.productId).slice(0, 8));

const fallbackSlides = [
  { title: 'Burger Ngon Mỗi Ngày', subtitle: 'Khám phá bộ sưu tập burger mới', cta: 'Xem thực đơn', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg' },
  { title: 'Giảm 20% Đơn Đầu Tiên', subtitle: 'Nhập mã FASTGUY20 khi thanh toán', cta: 'Đặt ngay', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/GaRan/gran-split.jpg' },
  { title: 'Giao Hàng Siêu Tốc 30 Phút', subtitle: 'Nóng hổi từ bếp đến tay bạn', cta: 'Tìm hiểu thêm', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Pizza/pizza-margherita.jpg' },
];

let slideInterval;
onMounted(async () => {
  if (!productStore.fetched) productStore.init();
  try {
    const data = await bannerApi.getActive();
    banners.value = (data && data.length > 0) ? data : fallbackSlides;
  } catch {
    banners.value = fallbackSlides;
  }
  slideInterval = setInterval(() => { currentSlide.value = (currentSlide.value + 1) % banners.value.length; }, 5000);
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
      <div v-for="(slide, i) in banners" :key="i" class="hero-slide" :class="{ active: currentSlide === i }" :style="{ backgroundImage: 'url(' + slide.imageUrl + ')' }">
        <div class="hero-overlay">
          <div class="hero-overlay-shine"></div>
        </div>
        <div class="container hero-content">
          <span class="hero-badge">FastGuy</span>
          <h1 class="hero-title">{{ slide.title }}</h1>
          <p class="hero-subtitle">{{ slide.subtitle }}</p>
          <router-link :to="slide.link || '/menu'" class="hero-cta">{{ slide.cta || 'Xem thực đơn' }} <i class="bi bi-arrow-right"></i></router-link>
        </div>
      </div>
      <div class="carousel-controls">
        <button class="carousel-arrow prev" @click="currentSlide = (currentSlide - 1 + banners.length) % banners.length"><i class="bi bi-chevron-left"></i></button>
        <button class="carousel-arrow next" @click="currentSlide = (currentSlide + 1) % banners.length"><i class="bi bi-chevron-right"></i></button>
      </div>
      <div class="carousel-footer">
        <div class="carousel-dots">
          <button v-for="(_, i) in banners" :key="i" class="dot" :class="{ active: currentSlide === i }" @click="currentSlide = i"><span></span></button>
        </div>
        <span class="carousel-counter">{{ String(currentSlide + 1).padStart(2, '0') }}/{{ String(banners.length).padStart(2, '0') }}</span>
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

.hero-carousel { position: relative; color: #fff; min-height: 420px; overflow: hidden; background: #1a1a2e; }
.hero-slide { position: absolute; inset: 0; display: flex; align-items: center; opacity: 0; transition: opacity 0.6s ease, transform 0.6s ease; background-size: cover; background-position: center; transform: scale(1.05); }
.hero-slide.active { opacity: 1; position: relative; transform: scale(1); }
.hero-overlay { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.3) 50%, rgba(0,0,0,0.1) 100%); overflow: hidden; }
.hero-overlay-shine {
  position: absolute; inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.04) 45%, transparent 50%);
  animation: shine 8s ease-in-out infinite;
}
@keyframes shine {
  0%, 100% { transform: translateX(-100%); }
  50% { transform: translateX(100%); }
}
.hero-content { padding: 60px 0; text-align: center; position: relative; z-index: 1; }
.hero-badge {
  display: inline-block;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(8px);
  padding: 4px 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 16px;
}
.hero-title { font-size: 42px; font-weight: 900; margin-bottom: 12px; line-height: 1.15; }
.hero-subtitle { font-size: 16px; opacity: 0.85; margin-bottom: 28px; max-width: 500px; margin-left: auto; margin-right: auto; }
.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: #fff;
  color: var(--primary);
  font-size: 15px;
  font-weight: 700;
  border-radius: 999px;
  text-decoration: none;
  transition: all 0.25s;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}
.hero-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(0,0,0,0.2);
  color: var(--primary-dark);
}
.carousel-controls { position: absolute; top: 50%; left: 0; right: 0; transform: translateY(-50%); z-index: 2; display: flex; justify-content: space-between; padding: 0 16px; pointer-events: none; }
.carousel-arrow { width: 40px; height: 40px; border-radius: 50%; border: none; background: rgba(255,255,255,0.15); backdrop-filter: blur(4px); color: #fff; font-size: 18px; cursor: pointer; transition: all 0.2s; pointer-events: auto; display: flex; align-items: center; justify-content: center; }
.carousel-arrow:hover { background: rgba(255,255,255,0.3); transform: scale(1.1); }
.carousel-footer { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 2; display: flex; align-items: center; gap: 16px; }
.carousel-dots { display: flex; gap: 8px; }
.dot { width: 24px; height: 4px; border-radius: 2px; border: none; background: rgba(255,255,255,0.4); cursor: pointer; padding: 0; transition: all 0.3s; overflow: hidden; }
.dot span { display: block; height: 100%; width: 0; background: #fff; border-radius: 2px; transition: width 0.3s; }
.dot.active { width: 36px; }
.dot.active span { width: 100%; }
.carousel-counter { font-size: 12px; font-weight: 600; opacity: 0.6; letter-spacing: 1px; }
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
