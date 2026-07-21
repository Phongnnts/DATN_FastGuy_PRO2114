<script setup>
import ProductCard from '@/components/common/ProductCard.vue';

defineProps({ products: { type: Array, required: true }, loading: { type: Boolean, default: false }, error: { type: String, default: '' } });
defineEmits(['retry']);
</script>

<template>
  <section class="featured"><div class="container"><div class="section-head"><div><p>Được yêu thích</p><h2>Món nổi bật</h2></div><router-link to="/menu">Xem thực đơn <i class="bi bi-arrow-right"></i></router-link></div><div v-if="loading" class="grid" role="status" aria-label="Đang tải món nổi bật"><div v-for="n in 4" :key="n" class="skeleton"></div></div><div v-else-if="products.length" class="showcase"><ProductCard class="lead" :product="products[0]" /><div class="grid"><ProductCard v-for="product in products.slice(1, 7)" :key="product.productId" :product="product" /></div></div><div v-else class="state"><p>{{ error || 'Món nổi bật đang được cập nhật.' }}</p><button v-if="error" @click="$emit('retry')">Thử lại</button></div></div></section>
</template>

<style scoped>
.featured{padding:72px 0;background:linear-gradient(180deg,#fff,#fff8f3)}.section-head{display:flex;align-items:end;justify-content:space-between;margin-bottom:26px}.section-head p{color:var(--primary);font-size:11px;font-weight:900;letter-spacing:.15em;text-transform:uppercase}.section-head h2{font-size:clamp(28px,4vw,42px);letter-spacing:-.04em}.section-head a{min-height:44px;display:flex;align-items:center;gap:8px;color:var(--primary);font-weight:800}.showcase{display:grid;grid-template-columns:minmax(260px,1.15fr) 2fr;gap:20px}.showcase>.lead{height:100%}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px}.skeleton{height:330px;border-radius:20px;background:linear-gradient(90deg,#eee 25%,#fafafa 50%,#eee 75%);background-size:200%;animation:pulse 1.3s infinite}.state{padding:50px;text-align:center;color:var(--text-mid)}.state button{min-height:44px;margin-top:12px;padding:8px 18px;border-radius:99px;color:#fff;background:var(--primary)}@keyframes pulse{to{background-position:-200%}}@media(max-width:900px){.showcase{grid-template-columns:1fr}.grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:560px){.featured{padding:48px 0}.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}}@media(prefers-reduced-motion:reduce){.skeleton{animation:none}}
</style>
