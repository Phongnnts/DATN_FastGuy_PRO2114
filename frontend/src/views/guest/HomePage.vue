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
const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 700"%3E%3Crect width="1200" height="700" fill="%23171313"/%3E%3C/svg%3E';
const fallbackSlides = [
  { title: 'Món ngon cho mọi cuộc vui', subtitle: 'Khám phá thực đơn đa dạng và chọn món bạn yêu thích.', cta: 'Xem thực đơn', link: '/menu', image: fallbackImage },
  { title: 'Lựa chọn dễ dàng, đặt món nhanh chóng', subtitle: 'Burger, pizza, gà rán và thức uống trong một thực đơn.', cta: 'Khám phá món ngon', link: '/menu', image: fallbackImage },
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
      <section class="experience-section">
        <div class="container">
          <div class="experience-head">
            <div><p>Trải nghiệm FastGuy</p><h2>Đơn giản từ lúc chọn<br>đến lúc nhận</h2></div>
            <span>Ba bước rõ ràng để món ngon đến tay bạn nhanh và thuận tiện hơn.</span>
          </div>
          <div class="experience-grid">
            <article><span class="step">01</span><div class="step-icon"><i class="bi bi-grid"></i></div><h3>Chọn món yêu thích</h3><p>Khám phá thực đơn đa dạng và tùy chỉnh món theo khẩu vị riêng.</p><router-link to="/menu">Xem thực đơn <i class="bi bi-arrow-right"></i></router-link></article>
            <article><span class="step">02</span><div class="step-icon"><i class="bi bi-bag-check"></i></div><h3>Đặt hàng thuận tiện</h3><p>Giá và tùy chọn được trình bày rõ ràng trước khi bạn xác nhận.</p><router-link to="/cart">Xem giỏ hàng <i class="bi bi-arrow-right"></i></router-link></article>
            <article><span class="step">03</span><div class="step-icon"><i class="bi bi-geo-alt"></i></div><h3>Theo dõi chủ động</h3><p>Kiểm tra hành trình đơn hàng theo từng trạng thái ngay khi cần.</p><router-link to="/track-order">Tra cứu đơn <i class="bi bi-arrow-right"></i></router-link></article>
          </div>
        </div>
      </section>

      <section class="about-section">
        <div class="container">
          <div class="about-grid">
            <div class="about-copy"><p class="about-kicker">Về FastGuy</p><h2>Không chỉ giao món.<br>Chúng tôi giao đúng khoảnh khắc.</h2><p>FastGuy giúp bạn chọn món yêu thích chỉ trong vài thao tác, từ burger, pizza, gà rán đến thức uống. Mọi thông tin đều rõ ràng để bạn yên tâm từ khi đặt đến khi nhận.</p><router-link to="/menu" class="about-cta">Bắt đầu chọn món <i class="bi bi-arrow-right"></i></router-link></div>
            <div class="about-visual">
              <div class="metric primary"><span>Đơn hàng thành công</span><strong>10,000+</strong><small>và vẫn tiếp tục mỗi ngày</small></div>
              <div class="metric"><span>Khách hàng đánh giá</span><strong>4.8<small>/5</small></strong><div class="stars"><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i></div></div>
              <div class="metric wide"><i class="bi bi-geo-alt-fill"></i><div><strong>TP.HCM</strong><span>Khu vực phục vụ</span></div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="home-cta"><div class="container"><div class="cta-panel"><div><span>Sẵn sàng cho bữa ngon?</span><h2>Chọn món ngay.<br>FastGuy lo phần còn lại.</h2></div><div class="cta-actions"><router-link to="/menu">Khám phá thực đơn <i class="bi bi-arrow-right"></i></router-link><router-link to="/track-order" class="secondary">Tra cứu đơn hàng</router-link></div></div></div></section>
    </template>
  </div>
</template>

<style scoped>
.hero{position:relative;min-height:580px;overflow:hidden;color:#fff;background:#19130f}.hero-bg{position:absolute;inset:0 0 0 42%;background-position:center;background-size:cover}.hero-overlay{position:absolute;inset:0;background:linear-gradient(90deg,#19130f 0%,rgba(25,19,15,.94) 42%,rgba(25,19,15,.28) 100%)}.hero-content{position:relative;z-index:1;display:grid;grid-template-columns:1.2fr .8fr;align-items:center;gap:72px;min-height:580px;padding:72px 0}.hero-copy{max-width:650px}.hero-eyebrow,.section-head p{margin:0 0 10px;color:var(--route-amber);font-size:12px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.hero h1{margin:0 0 20px;font-size:clamp(44px,6vw,72px);font-weight:900;line-height:1.02;letter-spacing:-.055em}.hero-desc{max-width:520px;margin:0 0 30px;color:rgba(255,255,255,.76);font-size:17px;line-height:1.65}.hero-actions{display:flex;flex-wrap:wrap;gap:12px}.hero-btn,.hero-secondary{display:inline-flex;align-items:center;justify-content:center;gap:9px;min-height:50px;padding:12px 22px;border-radius:999px;font-weight:800}.hero-btn{color:#fff;background:var(--route-orange)}.hero-secondary{color:#fff;border:1px solid rgba(255,255,255,.25);background:rgba(255,255,255,.08)}.hero-panel{display:flex;flex-direction:column;gap:20px;padding:32px;border:1px solid rgba(255,255,255,.18);border-radius:28px;background:rgba(20,15,12,.44);backdrop-filter:blur(10px)}.hero-panel span{font-size:11px;font-weight:900;letter-spacing:.22em}.hero-panel strong{font-size:clamp(28px,4vw,44px);line-height:1.12}.hero-panel i{align-self:flex-end;font-size:28px}.hero-controls{position:absolute;z-index:2;right:24px;bottom:24px;display:flex;gap:8px}.hero-controls button{display:grid;place-items:center;width:44px;height:44px;border:1px solid rgba(255,255,255,.25);border-radius:50%;color:#fff;background:rgba(0,0,0,.25)}.hero-dots{position:absolute;z-index:2;bottom:38px;left:50%;display:flex;gap:8px;transform:translateX(-50%)}.hero-dots button{width:8px;height:8px;padding:0;border:0;border-radius:99px;background:rgba(255,255,255,.45)}.hero-dots button.active{width:26px;background:#fff}.status{display:flex;align-items:center;justify-content:center;gap:12px;padding:56px 20px;color:var(--text-mid)}.section,.benefits{padding:64px 0}.section-head{display:flex;align-items:end;justify-content:space-between;gap:18px;margin-bottom:26px}.section-head p{margin-bottom:6px;color:var(--primary)}.section-head h2{margin:0;font-size:clamp(25px,3vw,36px);letter-spacing:-.035em}.section-head a{color:var(--primary);font-weight:750}.products{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px}.empty{color:var(--text-mid)}.benefits{background:#fff8f3}.benefit-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}.benefit-grid article{padding:28px;border:1px solid var(--border-light);border-radius:20px;background:#fff}.benefit-grid i{display:grid;place-items:center;width:48px;height:48px;margin-bottom:28px;border-radius:14px;color:var(--primary);background:var(--primary-light);font-size:21px}.benefit-grid h3{margin:0 0 8px;font-size:17px}.benefit-grid p{margin:0;color:var(--text-mid);font-size:14px;line-height:1.6}.about-section{padding:72px 0;background:linear-gradient(180deg,#fff8f3,#fff)}.about-grid{display:grid;grid-template-columns:1.2fr 1fr;gap:48px;align-items:center}.about-kicker{margin:0 0 10px;color:var(--primary);font-size:12px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.about-copy h2{margin:0 0 18px;font-size:clamp(26px,3.5vw,38px);letter-spacing:-.04em;line-height:1.15}.about-copy p{margin:0 0 24px;color:var(--text-mid);font-size:15px;line-height:1.7}.about-highlights{display:flex;flex-wrap:wrap;gap:16px}.about-highlights div{display:flex;align-items:center;gap:8px;font-size:14px;font-weight:650;color:var(--text-dark)}.about-highlights i{color:var(--primary);font-size:18px}.about-visual{display:grid;gap:14px}.about-card{display:flex;align-items:center;gap:14px;padding:20px 24px;border:1px solid var(--border-light);border-radius:18px;background:#fff;transition:transform .2s,box-shadow .2s}.about-card:hover{transform:translateY(-2px);box-shadow:var(--shadow-sm)}.about-card i{font-size:28px;color:var(--primary)}.about-card strong{font-size:22px;font-weight:800;color:var(--text-dark)}.about-card span{font-size:14px;color:var(--text-mid)}@media(max-width:900px){.about-grid{grid-template-columns:1fr;gap:32px}.about-highlights{flex-direction:column;gap:12px}}@media(max-width:640px){.about-section{padding:48px 0}}
@media(max-width:900px){.hero-bg{left:0;opacity:.45}.hero-content{grid-template-columns:1fr}.hero-panel{display:none}.products{grid-template-columns:repeat(2,1fr)}}@media(max-width:640px){.hero,.hero-content{min-height:520px}.hero-content{padding:58px 0 90px}.hero-actions{flex-direction:column}.hero-btn,.hero-secondary{width:100%}.hero-controls{display:none}.hero-dots{left:auto;right:20px;transform:none}.section,.benefits{padding:44px 0}.section-head{align-items:start}.products,.benefit-grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.hero *{scroll-behavior:auto;transition:none!important}}
</style>

<style scoped>
.experience-section{padding:100px 0;background:#f7f4f0}.experience-head{display:flex;align-items:end;justify-content:space-between;gap:40px;margin-bottom:42px}.experience-head>div>p,.about-kicker{margin-bottom:12px;color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.15em;text-transform:uppercase}.experience-head h2,.about-copy h2{font-size:clamp(32px,4.5vw,52px);font-weight:800;line-height:1.1;letter-spacing:-.05em}.experience-head>span{max-width:390px;color:var(--text-mid);font-size:14px;line-height:1.7}.experience-grid{display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid #dcd6d0;border-left:1px solid #dcd6d0}.experience-grid article{position:relative;display:flex;min-height:330px;flex-direction:column;padding:28px;border-right:1px solid #dcd6d0;border-bottom:1px solid #dcd6d0;background:rgba(255,255,255,.45);transition:color var(--transition-normal),background var(--transition-normal)}.experience-grid article:hover{color:#fff;background:var(--charcoal)}.step{color:var(--text-light);font-size:10px;font-weight:800;letter-spacing:.15em}.step-icon{display:grid;width:54px;height:54px;margin:48px 0 24px;place-items:center;border-radius:50%;color:var(--primary);background:#fff;box-shadow:var(--shadow-sm);font-size:21px}.experience-grid h3{font-size:20px;letter-spacing:-.025em}.experience-grid p{margin:9px 0 22px;color:var(--text-mid);font-size:13px;line-height:1.65}.experience-grid article:hover p{color:rgba(255,255,255,.62)}.experience-grid a{display:flex;align-items:center;gap:8px;margin-top:auto;color:var(--primary);font-size:12px;font-weight:800}.experience-grid article:hover a{color:var(--route-amber)}.about-section{position:relative;padding:110px 0;overflow:hidden;color:#fff;background:#1b1714}.about-section::before{position:absolute;top:-260px;left:-180px;width:600px;height:600px;border:1px solid rgba(255,255,255,.07);border-radius:50%;content:""}.about-grid{position:relative;display:grid;grid-template-columns:minmax(0,1fr) minmax(420px,.9fr);align-items:center;gap:90px}.about-copy{max-width:620px}.about-copy h2{color:#fff}.about-copy>p:not(.about-kicker){max-width:580px;margin:24px 0 30px;color:rgba(255,255,255,.62);font-size:15px;line-height:1.8}.about-cta{display:inline-flex;align-items:center;gap:9px;min-height:48px;padding:11px 20px;border-radius:999px;color:#1b1714;background:var(--route-amber);font-size:13px;font-weight:800}.about-visual{display:grid;grid-template-columns:1fr 1fr;gap:12px}.metric{display:flex;min-height:190px;flex-direction:column;justify-content:flex-end;padding:24px;border:1px solid rgba(255,255,255,.12);border-radius:24px;background:rgba(255,255,255,.06)}.metric.primary{color:#1b1714;background:#f2a77f;border-color:#f2a77f}.metric>span{margin-bottom:auto;color:rgba(255,255,255,.55);font-size:10px;font-weight:700;letter-spacing:.1em;text-transform:uppercase}.metric.primary>span{color:rgba(23,23,23,.6)}.metric strong{font-size:clamp(32px,4vw,48px);line-height:1}.metric strong small{font-size:18px}.metric>small{margin-top:9px;opacity:.62}.stars{margin-top:12px;color:var(--route-amber);font-size:12px;letter-spacing:3px}.metric.wide{grid-column:1/-1;min-height:110px;flex-direction:row;align-items:center;justify-content:flex-start;gap:18px}.metric.wide>i{display:grid;width:48px;height:48px;place-items:center;border-radius:50%;color:#1b1714;background:var(--route-amber);font-size:18px}.metric.wide strong{display:block;font-size:22px}.metric.wide span{color:rgba(255,255,255,.55);font-size:11px}.home-cta{padding:0;background:#1b1714}.cta-panel{display:flex;align-items:center;justify-content:space-between;gap:40px;padding:58px 64px;border-radius:30px 30px 0 0;background:var(--primary)}.cta-panel span{color:rgba(255,255,255,.7);font-size:11px;font-weight:800;letter-spacing:.13em;text-transform:uppercase}.cta-panel h2{margin-top:8px;color:#fff;font-size:clamp(28px,4vw,46px);line-height:1.12;letter-spacing:-.045em}.cta-actions{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:10px}.cta-actions a{display:flex;align-items:center;justify-content:center;gap:8px;min-height:48px;padding:11px 20px;border-radius:999px;color:#fff;background:var(--charcoal);font-size:13px;font-weight:800}.cta-actions .secondary{border:1px solid rgba(255,255,255,.4);background:transparent}
@media(max-width:900px){.experience-head{align-items:start}.experience-grid{grid-template-columns:1fr}.experience-grid article{min-height:260px}.step-icon{margin:30px 0 20px}.about-grid{grid-template-columns:1fr;gap:55px}.about-copy{max-width:700px}}
@media(max-width:640px){.experience-section{padding:64px 0}.experience-head{display:block}.experience-head>span{display:block;margin-top:16px}.experience-head h2 br{display:none}.experience-grid article{min-height:240px;padding:22px}.about-section{padding:72px 0}.about-copy h2 br{display:none}.about-visual{grid-template-columns:1fr}.metric{min-height:155px}.metric.wide{grid-column:auto}.cta-panel{align-items:flex-start;flex-direction:column;padding:40px 24px;border-radius:24px 24px 0 0}.cta-panel h2 br{display:none}.cta-actions{width:100%;justify-content:flex-start}.cta-actions a{width:100%}}
@media(prefers-reduced-motion:reduce){.experience-grid article{transition:none}}
</style>
