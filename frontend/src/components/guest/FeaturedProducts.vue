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
        <div v-for="product in products.slice(0, 6)" :key="product.productId" class="product-wrap"><ProductCard :product="product" homepage /></div>
      </div>
      <div v-else class="state" :role="error ? 'alert' : undefined"><p>{{ error || 'Món nổi bật đang được cập nhật.' }}</p><button v-if="error" @click="$emit('retry')">Thử lại</button></div>
    </div>
  </section>
</template>

<style scoped>
.featured{position:relative;padding:88px 0 92px;overflow:hidden;background:linear-gradient(145deg,#fff 0%,#fff8f2 58%,#fceee5 100%)}.featured::before{position:absolute;top:-180px;right:-120px;width:420px;height:420px;border:1px solid rgba(232,115,74,.15);border-radius:50%;content:""}.section-head{position:relative;display:flex;align-items:end;justify-content:space-between;gap:40px;margin-bottom:34px}.heading-copy{max-width:620px}.heading-copy>p{display:flex;align-items:center;gap:9px;margin-bottom:12px;color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.15em;text-transform:uppercase}.heading-copy>p span{width:28px;height:2px;background:var(--primary)}.section-head h2{font-size:clamp(30px,4vw,48px);font-weight:800;line-height:1.12;letter-spacing:-.045em}.section-desc{display:block;max-width:540px;margin-top:14px;color:var(--text-mid);font-size:14px;line-height:1.7}.heading-actions{display:flex;flex-direction:column;align-items:flex-end;gap:14px}.trust-points{display:flex;gap:16px;color:var(--text-mid);font-size:12px;font-weight:600}.trust-points span{display:flex;align-items:center;gap:6px}.trust-points i{color:var(--primary)}.heading-actions>a{display:flex;align-items:center;gap:9px;min-height:46px;padding:10px 18px;border:1px solid var(--charcoal);border-radius:999px;color:#fff;background:var(--charcoal);font-size:13px;font-weight:700;white-space:nowrap;transition:background var(--transition-fast),transform var(--transition-fast)}.heading-actions>a:hover{border-color:var(--primary);background:var(--primary);transform:translateY(-2px)}.grid{position:relative;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.product-wrap{min-width:0}.product-wrap:deep(.product-card){height:100%;border-radius:20px}.skeleton{min-height:330px;border-radius:20px;background:linear-gradient(90deg,#eee 25%,#f7f7f7 50%,#eee 75%);background-size:200% 100%;animation:shimmer 1.4s infinite}.state{position:relative;padding:54px;border:1px dashed var(--border);border-radius:20px;text-align:center;background:rgba(255,255,255,.7)}.state p{margin-bottom:14px;color:var(--text-mid)}.state button{min-height:44px;padding:8px 18px;border-radius:999px;color:#fff;background:var(--primary);font-weight:700}@keyframes shimmer{to{background-position:-200% 0}}@media(max-width:900px){.section-head{align-items:flex-start;flex-direction:column}.heading-actions{align-items:flex-start}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:560px){.featured{padding:62px 0}.section-head h2 br{display:none}.heading-actions{width:100%}.trust-points{flex-wrap:wrap}.heading-actions>a{width:100%;justify-content:center}.grid{gap:10px}.product-wrap:deep(.product-card){border-radius:14px}}@media(prefers-reduced-motion:reduce){.skeleton{animation:none}}
</style>
