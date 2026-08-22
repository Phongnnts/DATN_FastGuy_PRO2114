<script setup>
import { formatPrice } from '@/utils/format';

const props = defineProps({ items: { type: Array, default: () => [] } });
</script>

<template>
  <section v-if="props.items.length" class="occasions" aria-labelledby="occasion-title">
    <div class="container">
      <div class="section-head"><div><p>Đi cùng nhau ngon hơn</p><h2 id="occasion-title">Combo đáng thử</h2><span>Những lựa chọn vừa đủ cho bữa đôi và cuộc vui đông người.</span></div><router-link to="/menu">Xem thực đơn <i class="bi bi-arrow-right" aria-hidden="true"></i></router-link></div>
      <div class="combo-grid">
        <router-link v-for="item in props.items.slice(0, 4)" :key="item.occasion" :to="`/product/${item.product.productId}`" class="combo-feature">
          <span class="combo-image"><img v-if="item.product.image" :src="item.product.image" :alt="item.product.name" loading="lazy"><i v-else class="bi bi-basket2" aria-hidden="true"></i></span>
          <span class="combo-copy"><small>{{ item.label }}</small><strong>{{ item.product.name }}</strong><span>{{ item.product.description || item.copy }}</span><b>{{ formatPrice(item.product.price) }}</b><em>Chọn combo <i class="bi bi-arrow-up-right" aria-hidden="true"></i></em></span>
        </router-link>
      </div>
    </div>
  </section>
</template>

<style scoped>
.occasions{padding:88px 0;background:#fff5ec}.section-head{display:flex;align-items:end;justify-content:space-between;gap:30px;margin-bottom:28px}.section-head p{margin:0 0 9px;color:#f26a2e;font-size:10px;font-weight:900;letter-spacing:.14em;text-transform:uppercase}.section-head h2{margin:0;font-size:clamp(32px,4vw,48px);font-weight:900;letter-spacing:-.05em}.section-head span{display:block;margin-top:10px;color:#7e6c61;font-size:13px}.section-head>a{display:flex;align-items:center;gap:8px;min-height:44px;color:#1b1714;font-size:12px;font-weight:800}.combo-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.combo-feature{display:grid;grid-template-columns:minmax(170px,.8fr) minmax(0,1.2fr);min-height:230px;overflow:hidden;border:1px solid #ead8ca;border-radius:24px;color:#1b1714;background:#fff;transition:transform .22s,box-shadow .22s}.combo-feature:hover,.combo-feature:focus-visible{box-shadow:0 20px 45px rgba(43,25,15,.1);transform:translateY(-4px)}.combo-image{display:grid;min-height:230px;place-items:center;overflow:hidden;background:#f0dfd0}.combo-image img{width:100%;height:100%;object-fit:cover}.combo-image>i{color:#f26a2e;font-size:42px}.combo-copy{display:flex;align-items:flex-start;justify-content:center;flex-direction:column;padding:26px}.combo-copy small{color:#f26a2e;font-size:9px;font-weight:900;letter-spacing:.12em;text-transform:uppercase}.combo-copy strong{margin:7px 0;font-size:21px;line-height:1.15}.combo-copy>span{display:-webkit-box;overflow:hidden;color:#7e6c61;font-size:12px;line-height:1.55;-webkit-box-orient:vertical;-webkit-line-clamp:2}.combo-copy b{margin-top:14px;font-size:18px}.combo-copy em{display:flex;align-items:center;gap:7px;margin-top:14px;font-size:11px;font-style:normal;font-weight:850}@media(max-width:760px){.section-head{align-items:flex-start;flex-direction:column}.combo-grid{display:flex;overflow-x:auto;margin-inline:-16px;padding:0 16px 14px;scroll-snap-type:x proximity;scrollbar-width:none}.combo-feature{grid-template-columns:1fr;min-width:min(82vw,330px);scroll-snap-align:start}.combo-image{min-height:170px}.combo-copy{padding:20px}}@media(prefers-reduced-motion:reduce){.combo-feature{transition:none}.combo-feature:hover{transform:none}}
</style>
