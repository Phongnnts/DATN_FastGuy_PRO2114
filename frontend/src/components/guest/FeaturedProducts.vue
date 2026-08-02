<script setup>
import ProductCard from '@/components/common/ProductCard.vue';

defineProps({ products: { type: Array, required: true }, loading: { type: Boolean, default: false }, error: { type: String, default: '' } });
defineEmits(['retry']);
</script>

<template>
  <section class="featured">
    <div class="container">
      <div class="section-head">
        <div class="heading-copy">
          <p><span></span> Được yêu thích</p>
          <h2>Món ngon khách hàng<br>luôn chọn đầu tiên</h2>
          <span class="section-desc">Những lựa chọn nổi bật dựa trên lượt đặt thực tế tại FastGuy.</span>
        </div>
        <div class="heading-actions">
          <div class="trust-points" aria-label="Ưu điểm dịch vụ">
            <span><i class="bi bi-lightning-charge-fill"></i> Đặt nhanh</span>
            <span><i class="bi bi-patch-check-fill"></i> Giá rõ ràng</span>
          </div>
          <router-link to="/menu">Khám phá thực đơn <i class="bi bi-arrow-right"></i></router-link>
        </div>
      </div>
      <div v-if="loading" class="grid" role="status" aria-label="Đang tải món nổi bật"><div v-for="n in 4" :key="n" class="skeleton"></div></div>
      <div v-else-if="products.length" class="grid">
        <div v-for="(product, index) in products.slice(0, 7)" :key="product.productId" class="product-wrap" :class="{ spotlight: index === 0 }">
          <div v-if="index === 0" class="top-pick"><i class="bi bi-fire"></i> Top 1 hôm nay</div>
          <ProductCard :product="product" :list-mode="index === 0" />
        </div>
      </div>
      <div v-else class="state"><p>{{ error || 'Món nổi bật đang được cập nhật.' }}</p><button v-if="error" @click="$emit('retry')">Thử lại</button></div>
    </div>
  </section>
</template>

<style scoped>
.featured{position:relative;padding:88px 0 92px;overflow:hidden;background:linear-gradient(145deg,#fff 0%,#fff8f2 58%,#fceee5 100%)}.featured::before{position:absolute;top:-180px;right:-120px;width:420px;height:420px;border:1px solid rgba(232,115,74,.15);border-radius:50%;content:""}.section-head{position:relative;display:flex;align-items:end;justify-content:space-between;gap:40px;margin-bottom:34px}.heading-copy{max-width:620px}.heading-copy>p{display:flex;align-items:center;gap:9px;margin-bottom:12px;color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.15em;text-transform:uppercase}.heading-copy>p span{width:28px;height:2px;background:var(--primary)}.section-head h2{font-size:clamp(30px,4vw,48px);font-weight:800;line-height:1.12;letter-spacing:-.045em}.section-desc{display:block;max-width:540px;margin-top:14px;color:var(--text-mid);font-size:14px;line-height:1.7}.heading-actions{display:flex;flex-direction:column;align-items:flex-end;gap:14px}.trust-points{display:flex;gap:16px;color:var(--text-mid);font-size:12px;font-weight:600}.trust-points span{display:flex;align-items:center;gap:6px}.trust-points i{color:var(--primary)}.heading-actions>a{display:flex;align-items:center;gap:9px;min-height:46px;padding:10px 18px;border:1px solid var(--charcoal);border-radius:999px;color:#fff;background:var(--charcoal);font-size:13px;font-weight:700;white-space:nowrap;transition:background var(--transition-fast),transform var(--transition-fast)}.heading-actions>a:hover{background:var(--primary);border-color:var(--primary);transform:translateY(-2px)}.grid{position:relative;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.product-wrap{min-width:0}.product-wrap:deep(.product-card){height:100%;border-radius:20px}.spotlight{position:relative;grid-column:1/-1}.spotlight:deep(.product-card){min-height:300px;border-color:rgba(232,115,74,.25);box-shadow:0 22px 50px rgba(85,45,28,.1)}.spotlight:deep(.product-card.list-mode .product-main){grid-template-columns:minmax(300px,42%) 1fr}.spotlight:deep(.product-card.list-mode .product-name){font-size:clamp(21px,2.5vw,30px);font-weight:800;letter-spacing:-.035em}.spotlight:deep(.product-card.list-mode .product-desc){max-width:520px;margin-top:8px;font-size:14px;line-height:1.6}.spotlight:deep(.product-card.list-mode .product-meta){font-size:12px}.spotlight:deep(.product-footer){padding-right:20px;padding-left:20px}.top-pick{position:absolute;z-index:3;top:16px;left:16px;display:flex;align-items:center;gap:7px;padding:7px 11px;border-radius:999px;color:#fff;background:rgba(23,23,23,.88);font-size:10px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;backdrop-filter:blur(8px)}.top-pick i{color:var(--route-amber)}.skeleton{height:360px;border-radius:20px;background:linear-gradient(90deg,#eee 25%,#fafafa 50%,#eee 75%);background-size:200%;animation:pulse 1.3s infinite}.state{padding:50px;text-align:center;color:var(--text-mid)}.state button{min-height:44px;margin-top:12px;padding:8px 18px;border-radius:99px;color:#fff;background:var(--primary)}@keyframes pulse{to{background-position:-200%}}@media(max-width:950px){.section-head{align-items:start}.trust-points{display:none}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.spotlight:deep(.product-card.list-mode .product-main){grid-template-columns:minmax(240px,40%) 1fr}}@media(max-width:700px){.featured{padding:58px 0}.section-head{align-items:start;gap:20px}.section-head h2 br{display:none}.section-desc{display:none}.heading-actions>a{width:46px;padding:0;justify-content:center}.heading-actions>a{font-size:0}.heading-actions>a i{font-size:15px}.spotlight:deep(.product-card.list-mode .product-main){grid-template-columns:180px 1fr}.spotlight:deep(.product-card.list-mode .product-info){padding:18px}.spotlight:deep(.product-card.list-mode .product-name){font-size:19px}}@media(max-width:560px){.section-head{margin-bottom:24px}.grid{gap:10px}.spotlight{grid-column:auto}.spotlight:deep(.product-card){min-height:0;box-shadow:none}.spotlight:deep(.product-card.list-mode .product-main){display:block}.spotlight:deep(.product-card.list-mode .product-image){height:auto;aspect-ratio:1.32}.spotlight:deep(.product-card.list-mode .product-info){padding:9px 9px 12px}.spotlight:deep(.product-card.list-mode .product-name){font-size:12px}.spotlight:deep(.product-card.list-mode .product-desc){display:none}.spotlight:deep(.product-card.list-mode .product-meta){font-size:10px}.top-pick{top:8px;left:8px;padding:5px 8px;font-size:8px}}@media(prefers-reduced-motion:reduce){.skeleton{animation:none}.heading-actions>a{transition:none}}
</style>
