<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useProductStore } from '@/stores/product';
import CategoryGrid from '@/components/guest/CategoryGrid.vue';
import FeaturedProducts from '@/components/guest/FeaturedProducts.vue';
import ProductCard from '@/components/common/ProductCard.vue';
import bannerApi from '@/api/banner';

const productStore = useProductStore();
const newsletterEmail = ref('');
const newsletterMessage = ref('');
const currentSlide = ref(0);

const fallbackSlides = [
  { title: 'Burger ngon mỗi ngày', subtitle: 'Khám phá những món nóng hổi, làm mới mỗi ngày.', cta: 'Xem thực đơn', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg' },
  { title: 'Ưu đãi cho đơn đầu tiên', subtitle: 'Nhập mã FASTGUY20 khi thanh toán để nhận ưu đãi.', cta: 'Đặt ngay', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/GaRan/gran-split.jpg' },
  { title: 'Giao nhanh, món vẫn nóng', subtitle: 'Từ bếp đến tay bạn trong khoảng 30 phút.', cta: 'Khám phá món ngon', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Pizza/pizza-margherita.jpg' },
];
const banners = ref(fallbackSlides);
const activeSlide = computed(() => banners.value[currentSlide.value] || fallbackSlides[0]);
const previewProducts = computed(() => productStore.allProducts.slice(0, 10));
const newProducts = computed(() => [...productStore.allProducts].sort((a, b) => b.productId - a.productId).slice(0, 8));
let slideInterval;

function previousSlide() { currentSlide.value = (currentSlide.value - 1 + banners.value.length) % banners.value.length; }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % banners.value.length; }
function retryProducts() { productStore.fetched = false; productStore.init(); }

onMounted(async () => {
  if (!productStore.fetched) productStore.init();
  try {
    const data = await bannerApi.getActive();
    banners.value = data?.length ? data : fallbackSlides;
    currentSlide.value = 0;
  } catch {}
  slideInterval = setInterval(nextSlide, 5000);
});
onBeforeUnmount(() => clearInterval(slideInterval));

function subscribeNewsletter() {
  if (!newsletterEmail.value) return;
  newsletterMessage.value = 'Cảm ơn bạn đã đăng ký nhận tin!';
  newsletterEmail.value = '';
}
</script>

<template>
  <div>
    <section class="hero">
      <div class="hero-bg" :style="{ backgroundImage: `url(${activeSlide.imageUrl || activeSlide.image})` }"></div>
      <div class="hero-overlay"></div>
      <div class="container hero-content">
        <div class="hero-copy">
          <p class="hero-eyebrow">Fast food giao tận nơi</p>
          <h1 class="hero-title">{{ activeSlide.title }}</h1>
          <p class="hero-desc">{{ activeSlide.subtitle }}</p>
          <div class="hero-actions">
            <router-link :to="activeSlide.link || '/menu'" class="hero-btn">{{ activeSlide.cta || 'Xem thực đơn' }} <i class="bi bi-arrow-right"></i></router-link>
            <router-link to="/track-order" class="hero-secondary"><i class="bi bi-geo-alt"></i> Theo dõi đơn</router-link>
          </div>
        </div>
        <div class="hero-route-card" aria-label="Thông tin giao hàng nhanh">
          <div class="route-map"><div class="route-path"></div><div class="route-point kitchen"><i class="bi bi-fire"></i></div><div class="route-point rider"><i class="bi bi-bicycle"></i></div><div class="route-point home"><i class="bi bi-house-door"></i></div></div>
          <div class="route-order-card"><span>Giao tận nơi</span><strong>~ 30 phút</strong><small>Từ bếp nóng đến cửa nhà bạn</small></div>
        </div>
      </div>
      <div class="hero-controls" v-if="banners.length > 1"><button class="hero-arrow" aria-label="Banner trước" @click="previousSlide"><i class="bi bi-chevron-left"></i></button><button class="hero-arrow" aria-label="Banner tiếp theo" @click="nextSlide"><i class="bi bi-chevron-right"></i></button></div>
      <div v-if="banners.length > 1" class="hero-dots"><button v-for="(_, index) in banners" :key="index" class="dot" :class="{ active: currentSlide === index }" :aria-label="`Đến banner ${index + 1}`" :aria-current="currentSlide === index" @click="currentSlide = index"></button></div>
    </section>

    <div v-if="productStore.loading" class="loading-wrap" role="status" aria-live="polite"><div class="container"><span class="spinner"></span> Đang tải thực đơn...</div></div>
    <div v-else-if="productStore.error" class="loading-wrap" role="alert"><div class="container"><p>{{ productStore.error }}</p><button class="btn btn-outline" @click="retryProducts">Thử lại</button></div></div>

    <template v-else>
      <CategoryGrid :categories="productStore.allCategories" />
      <section class="section"><div class="container"><div class="section-head"><div><p class="section-kicker">Chọn nhanh hôm nay</p><h2>Thực đơn hôm nay</h2></div><router-link to="/menu" class="see-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link></div><div v-if="previewProducts.length" class="grid-5"><ProductCard v-for="product in previewProducts" :key="product.productId" :product="product" /></div><p v-else class="section-empty">Thực đơn đang được cập nhật.</p></div></section>
      <section v-if="newProducts.length" class="section section-alt"><div class="container"><div class="section-head"><div><p class="section-kicker">Vừa cập nhật</p><h2>Món mới</h2></div><router-link to="/menu" class="see-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link></div><div class="grid-5"><ProductCard v-for="product in newProducts" :key="`new-${product.productId}`" :product="product" /></div></div></section>
      <section class="section"><div class="container"><h2 class="section-title-center">Vì sao chọn FastGuy?</h2><div class="features-grid"><div class="feature-card"><div class="feature-icon"><i class="bi bi-clock"></i></div><h3>Giao hàng nhanh</h3><p>Đồ ăn được chuẩn bị và giao đến khi vẫn nóng hổi.</p></div><div class="feature-card"><div class="feature-icon"><i class="bi bi-basket2"></i></div><h3>Nguyên liệu tươi</h3><p>Chọn lọc kỹ từng nguyên liệu cho mỗi món ăn.</p></div><div class="feature-card"><div class="feature-icon"><i class="bi bi-emoji-smile"></i></div><h3>Nhiều lựa chọn</h3><p>Burger, pizza, gà rán và thức uống cho mọi khẩu vị.</p></div><div class="feature-card"><div class="feature-icon"><i class="bi bi-wallet2"></i></div><h3>Giá hợp lý</h3><p>Nhiều ưu đãi đơn giản, rõ ràng cho khách hàng.</p></div></div></div></section>
      <FeaturedProducts v-if="productStore.featuredProducts.length" :products="productStore.featuredProducts" />
      <section class="section newsletter-section"><div class="container"><div class="newsletter"><h2>Đăng ký nhận tin</h2><p>Nhận ưu đãi và món mới mỗi tuần.</p><form class="newsletter-form" @submit.prevent="subscribeNewsletter"><label class="sr-only" for="newsletter-email">Email nhận tin</label><input id="newsletter-email" v-model="newsletterEmail" type="email" class="form-input" placeholder="Email của bạn" required><button type="submit" class="btn btn-primary btn-lg">Đăng ký</button></form><p v-if="newsletterMessage" class="newsletter-message" role="status">{{ newsletterMessage }}</p></div></div></section>
    </template>
  </div>
</template>

<style scoped>
.loading-wrap { padding: 56px 0; text-align: center; color: var(--text-mid); }.loading-wrap .container { display:flex; justify-content:center; align-items:center; gap:10px; }.loading-wrap p { color:var(--red-active); margin:0; }.loading-wrap .btn { margin-left:8px; }
.hero { position:relative; min-height:540px; overflow:hidden; color:#fff; background:#1d1612; }.hero-bg { position:absolute; inset:0 0 0 auto; width:55%; background-size:cover; background-position:center; transition:opacity .45s ease; }.hero-overlay { position:absolute; inset:0; background:linear-gradient(90deg,rgba(20,15,12,.97) 0%,rgba(20,15,12,.82) 44%,rgba(20,15,12,.3) 100%); }.hero-content { position:relative; z-index:1; min-height:540px; display:grid; grid-template-columns:minmax(0,1.1fr) minmax(300px,.85fr); align-items:center; gap:52px; padding:76px 0; }.hero-copy { max-width:600px; }.hero-eyebrow,.section-kicker { margin:0 0 10px; font-size:12px; font-weight:800; letter-spacing:.1em; text-transform:uppercase; color:var(--route-amber); }.hero-title { max-width:640px; margin:0 0 18px; font-size:clamp(42px,6vw,64px); font-weight:900; line-height:1.08; letter-spacing:-.045em; }.hero-desc { max-width:470px; margin:0 0 30px; color:rgba(255,255,255,.78); font-size:17px; line-height:1.6; }.hero-actions { display:flex; flex-wrap:wrap; gap:12px; }.hero-btn,.hero-secondary { display:inline-flex; align-items:center; justify-content:center; gap:9px; min-height:48px; padding:12px 22px; border-radius:var(--radius-full); font-size:14px; font-weight:800; transition:transform var(--transition-fast),background var(--transition-fast); }.hero-btn { color:#fff; background:var(--route-orange); box-shadow:0 12px 24px rgba(255,107,53,.22); }.hero-secondary { color:#fff; border:1px solid rgba(255,255,255,.2); background:rgba(255,255,255,.08); }.hero-btn:hover,.hero-secondary:hover { transform:translateY(-2px); }
.hero-route-card { position:relative; min-height:310px; overflow:hidden; border:1px solid rgba(255,255,255,.15); border-radius:24px; background:rgba(255,255,255,.08); box-shadow:0 18px 48px rgba(0,0,0,.25); }.route-map { position:absolute; inset:20px; border-radius:18px; background:rgba(255,255,255,.04); }.route-path { position:absolute; top:64px; left:54px; width:70%; height:140px; border:3px dashed rgba(255,183,3,.88); border-left:0; border-bottom:0; border-radius:0 100px 0 0; transform:rotate(7deg); }.route-point { position:absolute; display:grid; place-items:center; width:42px; height:42px; border-radius:50%; color:#fff; background:var(--route-orange); box-shadow:0 8px 18px rgba(255,107,53,.3); }.route-point.kitchen { top:48px; left:25px; }.route-point.rider { top:125px; left:49%; }.route-point.home { right:32px; bottom:64px; }.route-order-card { position:absolute; right:20px; bottom:20px; left:20px; padding:16px; border-radius:16px; color:var(--text-dark); background:#fff; }.route-order-card span { display:block; color:var(--primary); font-size:11px; font-weight:800; text-transform:uppercase; letter-spacing:.08em; }.route-order-card strong { display:block; margin:4px 0; font-size:26px; }.route-order-card small { color:var(--text-mid); }.hero-controls { position:absolute; z-index:2; top:50%; right:0; left:0; display:flex; justify-content:space-between; padding:0 18px; pointer-events:none; }.hero-arrow { display:grid; place-items:center; width:44px; height:44px; border:1px solid rgba(255,255,255,.18); border-radius:50%; color:#fff; background:rgba(0,0,0,.18); cursor:pointer; pointer-events:auto; }.hero-arrow:hover { background:rgba(255,255,255,.17); }.hero-dots { position:absolute; z-index:2; bottom:22px; left:50%; display:flex; gap:8px; transform:translateX(-50%); }.dot { width:8px; height:8px; padding:0; border:0; border-radius:50%; background:rgba(255,255,255,.45); cursor:pointer; transition:all var(--transition-fast); }.dot.active { width:24px; border-radius:99px; background:var(--route-amber); }
.section { padding:48px 0; }.section-alt { background:#fff8f4; }.section-head { display:flex; align-items:end; justify-content:space-between; gap:16px; margin-bottom:20px; }.section-head h2 { margin:0; font-size:22px; letter-spacing:-.025em; }.section-kicker { margin-bottom:5px; color:var(--primary); }.see-all { display:inline-flex; align-items:center; gap:5px; color:var(--primary); font-size:14px; font-weight:700; }.see-all:hover { gap:9px; }.section-empty { padding:24px 0; color:var(--text-mid); }.section-title-center { margin-bottom:28px; text-align:center; font-size:24px; letter-spacing:-.025em; }.features-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; }.feature-card { padding:28px 18px; border:1px solid var(--border-light); border-radius:var(--radius); background:#fff; text-align:center; }.feature-icon { display:grid; place-items:center; width:48px; height:48px; margin:0 auto 14px; border-radius:14px; background:var(--primary-light); color:var(--primary); font-size:20px; }.feature-card h3 { margin-bottom:7px; font-size:15px; }.feature-card p { color:var(--text-mid); font-size:13px; line-height:1.6; }.newsletter-section { padding-bottom:32px; }.newsletter { padding:42px; border:1px solid var(--border-light); border-radius:var(--radius-lg); background:var(--primary-light); text-align:center; }.newsletter h2 { margin-bottom:6px; font-size:23px; }.newsletter p { color:var(--text-mid); }.newsletter-form { display:flex; gap:10px; max-width:440px; margin:22px auto 0; }.newsletter-form input { flex:1; }.newsletter-message { margin:12px 0 0; color:var(--primary); font-size:14px; font-weight:600; }
@media (max-width:900px) { .hero { min-height:auto; }.hero-bg { width:100%; opacity:.28; }.hero-content { grid-template-columns:1fr; min-height:auto; padding:64px 0 80px; }.hero-route-card { max-width:500px; width:100%; min-height:250px; }.hero-controls { top:auto; bottom:68px; justify-content:flex-end; gap:8px; }.hero-arrow { width:38px; height:38px; }.features-grid { grid-template-columns:repeat(2,1fr); } }
@media (max-width:768px) { .hero-title { font-size:clamp(36px,11vw,50px); }.newsletter { padding:30px 20px; }.newsletter-form { flex-direction:column; } }
@media (max-width:480px) { .hero-content { padding:58px 0 78px; }.hero-route-card { display:none; }.hero-actions { flex-direction:column; }.hero-btn,.hero-secondary { width:100%; }.features-grid { grid-template-columns:1fr; }.hero-controls { bottom:56px; }.hero-dots { bottom:22px; } }
</style>
