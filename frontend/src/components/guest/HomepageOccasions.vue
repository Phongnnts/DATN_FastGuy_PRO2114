<script setup>
import ProductCard from '@/components/common/ProductCard.vue';

const props = defineProps({ items: { type: Array, default: () => [] } });
</script>

<template>
  <section v-if="props.items.length" class="occasions" aria-labelledby="occasion-title">
    <div class="container">
      <div class="section-head"><div><p><i class="bi bi-fire" aria-hidden="true"></i> Chọn theo dịp</p><h2 id="occasion-title">Combo vừa ý cho hôm nay</h2><span>Không cần nghĩ lâu. Chọn đúng bữa, đúng lúc.</span></div><router-link to="/menu">Xem tất cả món <i class="bi bi-arrow-right" aria-hidden="true"></i></router-link></div>
      <div class="occasion-grid">
        <article v-for="(item, index) in props.items.slice(0, 4)" :key="item.occasion" class="occasion" :class="`occasion-${item.color}`">
          <div class="occasion-copy"><span>0{{ index + 1 }}</span><div><p>{{ item.label }}</p><small>{{ item.copy }}</small></div></div>
          <ProductCard :product="item.product" homepage />
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.occasions{padding:94px 0;background:#fff8f2}.section-head{display:flex;align-items:end;justify-content:space-between;gap:30px;margin-bottom:30px}.section-head>div>p{display:flex;align-items:center;gap:7px;margin:0 0 9px;color:#f26a2e;font-size:10px;font-weight:900;letter-spacing:.14em;text-transform:uppercase}.section-head h2{font-size:clamp(32px,4vw,48px);font-weight:900;letter-spacing:-.05em}.section-head>div>span{display:block;margin-top:10px;color:#7e6c61;font-size:13px}.section-head>a{display:flex;align-items:center;gap:8px;min-height:44px;color:#1b1714;font-size:12px;font-weight:800}.occasion-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px}.occasion{min-width:0;padding:12px;border:1px solid #eadfd7;border-radius:26px;background:#fff;transition:transform .25s,box-shadow .25s}.occasion:hover{box-shadow:0 20px 45px rgba(43,25,15,.1);transform:translateY(-4px)}.occasion-copy{display:flex;align-items:center;gap:12px;padding:7px 5px 15px}.occasion-copy>span{display:grid;width:36px;height:36px;flex:0 0 36px;place-items:center;border-radius:12px;color:#fff;background:#1b1714;font-size:10px;font-weight:900}.occasion-copy p{margin:0 0 3px;font-size:13px;font-weight:850}.occasion-copy small{display:block;overflow:hidden;color:#8a776c;font-size:10px;line-height:1.35;text-overflow:ellipsis;white-space:nowrap}.occasion-orange .occasion-copy>span{background:#f26a2e}.occasion-amber .occasion-copy>span{color:#1b1714;background:#ffc857}.occasion-green .occasion-copy>span{background:#0f766e}.occasion:deep(.homepage-card){height:calc(100% - 58px);box-shadow:none}.occasion:deep(.homepage-card .product-image){aspect-ratio:1.05}.occasion:deep(.homepage-card .product-desc){display:none}.occasion:deep(.homepage-card .product-name){font-size:14px}@media(max-width:980px){.occasion-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:560px){.occasions{padding:64px 0}.section-head{align-items:flex-start;flex-direction:column;margin-bottom:22px}.occasion-grid{gap:10px}.occasion{padding:8px;border-radius:17px}.occasion-copy{gap:7px;padding:4px 2px 10px}.occasion-copy>span{width:29px;height:29px;flex-basis:29px;border-radius:9px}.occasion-copy p{font-size:11px}.occasion-copy small{display:none}.occasion:deep(.homepage-card){height:calc(100% - 43px)}}@media(prefers-reduced-motion:reduce){.occasion{transition:none}.occasion:hover{transform:none}}
</style>
