<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useProductStore } from '@/stores/product';
import CategoryGrid from '@/components/guest/CategoryGrid.vue';
import FeaturedProducts from '@/components/guest/FeaturedProducts.vue';
import bannerApi from '@/api/banner';

const productStore = useProductStore();
const currentSlide = ref(0);
const carouselPaused = ref(false);
const reduceMotion = ref(false);
const fallbackSlides = [
  { title: 'Món ngon cho mọi cuộc vui', subtitle: 'Khám phá thực đơn đa dạng và chọn món bạn yêu thích.', cta: 'Xem thực đơn', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Burger/classic-burger.jpg' },
  { title: 'Lựa chọn dễ dàng, đặt món nhanh chóng', subtitle: 'Burger, pizza, gà rán và thức uống trong một thực đơn.', cta: 'Khám phá món ngon', link: '/menu', image: 'https://res.cloudinary.com/ds4dnsj0o/image/upload/v1/Image_Cloudinery/Pizza/pizza-margherita.jpg' },
];
const banners = ref(fallbackSlides);
const activeSlide = computed(() => banners.value[currentSlide.value] || fallbackSlides[0]);
const previewProducts = computed(() => productStore.featuredProducts);
let slideInterval;
let motionQuery;

function previousSlide() { currentSlide.value = (currentSlide.value - 1 + banners.value.length) % banners.value.length; }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % banners.value.length; }
function navigateSlide(index) { currentSlide.value = index; startCarousel(); }
function retryProducts() { productStore.fetched = false; productStore.init(); }
function stopCarousel() { clearInterval(slideInterval); slideInterval = undefined; }
function startCarousel() {
  stopCarousel();
  if (!reduceMotion.value && !carouselPaused.value && banners.value.length > 1) slideInterval = setInterval(nextSlide, 5000);
}
function setPaused(value) { carouselPaused.value = value; value ? stopCarousel() : startCarousel(); }
function updateMotion(event) { reduceMotion.value = event.matches; startCarousel(); }

onMounted(async () => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
  reduceMotion.value = motionQuery.matches;
  motionQuery.addEventListener('change', updateMotion);
  if (!productStore.fetched) productStore.init();
  productStore.fetchFeatured();
  try {
    const data = await bannerApi.getActive();
    banners.value = data?.length ? data : fallbackSlides;
    currentSlide.value = 0;
  } catch {}
  startCarousel();
});
onBeforeUnmount(() => {
  stopCarousel();
  motionQuery?.removeEventListener('change', updateMotion);
});
</script>

<template>
  <div>
    <section class="hero" aria-roledescription="carousel" aria-label="Giới thiệu FastGuy" @mouseenter="setPaused(true)" @mouseleave="setPaused(false)" @focusin="setPaused(true)" @focusout="setPaused(false)">
      <div class="hero-bg" :style="{ backgroundImage: `url(${activeSlide.imageUrl || activeSlide.image})` }"></div>
      <div class="hero-overlay"></div>
      <div class="container hero-content">
        <div class="hero-copy">
          <p class="hero-eyebrow">FastGuy · Đặt món trực tuyến</p>
          <h1>{{ activeSlide.title }}</h1>
          <p class="hero-desc">{{ activeSlide.subtitle }}</p>
          <div class="hero-actions"><router-link :to="activeSlide.link || '/menu'" class="hero-btn">{{ activeSlide.cta || 'Xem thực đơn' }} <i class="bi bi-arrow-right"></i></router-link><router-link to="/track-order" class="hero-secondary"><i class="bi bi-geo-alt"></i> Theo dõi đơn</router-link></div>
        </div>
        <div class="hero-panel" aria-hidden="true"><span>FASTGUY</span><strong>Chọn món.<br>Đặt nhanh.<br>Thưởng thức.</strong><i class="bi bi-arrow-up-right"></i></div>
      </div>
      <div v-if="banners.length > 1" class="hero-controls"><button aria-label="Banner trước" @click="navigateSlide((currentSlide - 1 + banners.length) % banners.length)"><i class="bi bi-chevron-left"></i></button><button aria-label="Banner tiếp theo" @click="navigateSlide((currentSlide + 1) % banners.length)"><i class="bi bi-chevron-right"></i></button></div>
      <div v-if="banners.length > 1" class="hero-dots"><button v-for="(_, index) in banners" :key="index" :class="{ active: currentSlide === index }" :aria-label="`Đến banner ${index + 1}`" :aria-current="currentSlide === index ? 'true' : undefined" @click="navigateSlide(index)"></button></div>
    </section>

    <div v-if="productStore.loading" class="status" role="status"><span class="spinner"></span> Đang tải thực đơn...</div>
    <div v-else-if="productStore.error" class="status" role="alert"><span>{{ productStore.error }}</span><button class="btn btn-outline" @click="retryProducts">Thử lại</button></div>
    <template v-else>
      <CategoryGrid :categories="productStore.allCategories" />
      <FeaturedProducts :products="previewProducts" :loading="productStore.featuredLoading" :error="productStore.featuredError" @retry="productStore.fetchFeatured" />
      <section class="benefits"><div class="container"><div class="section-head"><div><p>Trải nghiệm FastGuy</p><h2>Đơn giản từ lúc chọn đến lúc nhận</h2></div></div><div class="benefit-grid"><article><i class="bi bi-grid"></i><h3>Thực đơn đa dạng</h3><p>Nhiều nhóm món phù hợp từng khẩu vị.</p></article><article><i class="bi bi-bag-check"></i><h3>Đặt món thuận tiện</h3><p>Thông tin món và giá được trình bày rõ ràng.</p></article><article><i class="bi bi-geo-alt"></i><h3>Theo dõi đơn hàng</h3><p>Chủ động kiểm tra trạng thái đơn đã đặt.</p></article></div></div></section>
    </template>
  </div>
</template>

<style scoped>
.hero{position:relative;min-height:580px;overflow:hidden;color:#fff;background:#19130f}.hero-bg{position:absolute;inset:0 0 0 42%;background-position:center;background-size:cover}.hero-overlay{position:absolute;inset:0;background:linear-gradient(90deg,#19130f 0%,rgba(25,19,15,.94) 42%,rgba(25,19,15,.28) 100%)}.hero-content{position:relative;z-index:1;display:grid;grid-template-columns:1.2fr .8fr;align-items:center;gap:72px;min-height:580px;padding:72px 0}.hero-copy{max-width:650px}.hero-eyebrow,.section-head p{margin:0 0 10px;color:var(--route-amber);font-size:12px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.hero h1{margin:0 0 20px;font-size:clamp(44px,6vw,72px);font-weight:900;line-height:1.02;letter-spacing:-.055em}.hero-desc{max-width:520px;margin:0 0 30px;color:rgba(255,255,255,.76);font-size:17px;line-height:1.65}.hero-actions{display:flex;flex-wrap:wrap;gap:12px}.hero-btn,.hero-secondary{display:inline-flex;align-items:center;justify-content:center;gap:9px;min-height:50px;padding:12px 22px;border-radius:999px;font-weight:800}.hero-btn{color:#fff;background:var(--route-orange)}.hero-secondary{color:#fff;border:1px solid rgba(255,255,255,.25);background:rgba(255,255,255,.08)}.hero-panel{display:flex;flex-direction:column;gap:20px;padding:32px;border:1px solid rgba(255,255,255,.18);border-radius:28px;background:rgba(20,15,12,.44);backdrop-filter:blur(10px)}.hero-panel span{font-size:11px;font-weight:900;letter-spacing:.22em}.hero-panel strong{font-size:clamp(28px,4vw,44px);line-height:1.12}.hero-panel i{align-self:flex-end;font-size:28px}.hero-controls{position:absolute;z-index:2;right:24px;bottom:24px;display:flex;gap:8px}.hero-controls button{display:grid;place-items:center;width:44px;height:44px;border:1px solid rgba(255,255,255,.25);border-radius:50%;color:#fff;background:rgba(0,0,0,.25)}.hero-dots{position:absolute;z-index:2;bottom:38px;left:50%;display:flex;gap:8px;transform:translateX(-50%)}.hero-dots button{width:8px;height:8px;padding:0;border:0;border-radius:99px;background:rgba(255,255,255,.45)}.hero-dots button.active{width:26px;background:#fff}.status{display:flex;align-items:center;justify-content:center;gap:12px;padding:56px 20px;color:var(--text-mid)}.section,.benefits{padding:64px 0}.section-head{display:flex;align-items:end;justify-content:space-between;gap:18px;margin-bottom:26px}.section-head p{margin-bottom:6px;color:var(--primary)}.section-head h2{margin:0;font-size:clamp(25px,3vw,36px);letter-spacing:-.035em}.section-head a{color:var(--primary);font-weight:750}.products{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px}.empty{color:var(--text-mid)}.benefits{background:#fff8f3}.benefit-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}.benefit-grid article{padding:28px;border:1px solid var(--border-light);border-radius:20px;background:#fff}.benefit-grid i{display:grid;place-items:center;width:48px;height:48px;margin-bottom:28px;border-radius:14px;color:var(--primary);background:var(--primary-light);font-size:21px}.benefit-grid h3{margin:0 0 8px;font-size:17px}.benefit-grid p{margin:0;color:var(--text-mid);font-size:14px;line-height:1.6}
@media(max-width:900px){.hero-bg{left:0;opacity:.45}.hero-content{grid-template-columns:1fr}.hero-panel{display:none}.products{grid-template-columns:repeat(2,1fr)}}@media(max-width:640px){.hero,.hero-content{min-height:520px}.hero-content{padding:58px 0 90px}.hero-actions{flex-direction:column}.hero-btn,.hero-secondary{width:100%}.hero-controls{display:none}.hero-dots{left:auto;right:20px;transform:none}.section,.benefits{padding:44px 0}.section-head{align-items:start}.products,.benefit-grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.hero *{scroll-behavior:auto;transition:none!important}}
</style>
