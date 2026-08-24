<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import { canDirectAddProduct } from '@/utils/productCard';
import { customerAvailability } from '@/utils/stockPolicy';

const props = defineProps({ product: { type: Object, required: true }, listMode: { type: Boolean, default: false }, homepage: { type: Boolean, default: false } });
const cart = useCartStore();
const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const router = useRouter();
const pending = ref(false);
const added = ref(false);
const favoritePending = ref(false);
const message = ref('');
const imageFailed = ref(false);
let messageTimer;
let addedTimer;
const discountPrice = computed(() => {
  const price = Number(props.product.price);
  const discount = Number(props.product.discountPrice);
  return price > 0 && discount > 0 && discount < price ? discount : null;
});
const originalPrice = computed(() => Number(props.product.originalPrice) > Number(props.product.price) ? Number(props.product.originalPrice) : null);
const currentPrice = computed(() => discountPrice.value || Number(props.product.price));
const crossedPrice = computed(() => originalPrice.value || (discountPrice.value ? Number(props.product.price) : null));
const discountPercent = computed(() => {
  const supplied = Number(props.product.discountPercent);
  const derived = crossedPrice.value > currentPrice.value ? (1 - currentPrice.value / crossedPrice.value) * 100 : 0;
  const percent = Math.round(Number.isFinite(supplied) && supplied > 0 ? supplied : derived);
  return Number.isFinite(percent) && percent > 0 ? percent : null;
});
const averageRating = computed(() => Math.min(5, Math.max(0, Number(props.product.averageRating) || 0)));
const reviewCount = computed(() => Math.max(0, Math.floor(Number(props.product.reviewCount) || 0)));
const ratingText = computed(() => reviewCount.value > 0 ? `${averageRating.value.toFixed(1)} · ${reviewCount.value} đánh giá` : 'Chưa có đánh giá');
const ratingLabel = computed(() => reviewCount.value > 0 ? `Đánh giá ${averageRating.value.toFixed(1)} trên 5 từ ${reviewCount.value} lượt` : 'Chưa có đánh giá, 0 lượt');
const soldCount = computed(() => Math.max(0, Math.floor(Number(props.product.soldCount) || 0)));
const formatPrice = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value) || 0);
const availability = computed(() => customerAvailability(props.product.defaultVariant));
const canAdd = () => canDirectAddProduct(props.product);
function notify(value) { message.value = value; clearTimeout(messageTimer); messageTimer = setTimeout(() => { message.value = ''; }, 2500); }
onBeforeUnmount(() => { clearTimeout(messageTimer); clearTimeout(addedTimer); });
async function addToCart() {
  const variantId = props.product.defaultVariant?.variantId;
  if (!variantId || !canAdd() || pending.value) return;
  if (!availability.value.available) return notify('Món hiện tạm hết');
  pending.value = true;
  try { await cart.addItem(props.product.productId, variantId); added.value = true; clearTimeout(addedTimer); addedTimer = setTimeout(() => { added.value = false; }, 900); notify('Đã thêm vào giỏ hàng'); } catch (error) { notify(error.message || 'Không thể thêm vào giỏ'); } finally { pending.value = false; }
}
async function toggleFavorite() {
  if (favoritePending.value) return;
  if (!auth.isLoggedIn) { router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }); return; }
  favoritePending.value = true;
  try { await favoriteStore.toggle(props.product); } catch (error) { notify(error.message || 'Không thể cập nhật yêu thích'); } finally { favoritePending.value = false; }
}
</script>

<template>
  <article class="product-card" :class="{ 'list-mode': listMode, 'homepage-card': homepage }">
    <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
      <div class="product-image">
        <img v-if="product.image && !imageFailed" :src="product.image" :alt="product.name" loading="lazy" decoding="async" @error="imageFailed = true">
        <div v-else class="image-fallback" role="img" :aria-label="`Chưa có ảnh ${product.name}`"><strong>FastGuy</strong><span>Ảnh món đang được cập nhật</span></div>
        <div class="product-tags">
          <span v-if="product.bestSeller" class="best-badge"><i class="fa-solid fa-fire" aria-hidden="true"></i>Bán chạy</span>
          <span v-if="product.isNew" class="new-badge">Mới</span>
          <span v-if="discountPercent" class="hot-badge">-{{ discountPercent }}%</span>
        </div>
        <div v-if="!product.inStock || product.isAvailableNow === false" class="stock-badge">{{ product.isAvailableNow === false ? 'Ngoài giờ bán' : 'Tạm hết' }}</div>
      </div>
      <div class="product-info">
        <h3 class="product-name">{{ product.name }}</h3>
        <p class="product-desc">{{ product.description || '\u00a0' }}</p>
        <p class="product-rating" :aria-label="ratingLabel"><i class="fa-solid fa-star" aria-hidden="true"></i>{{ ratingText }}</p>
        <p class="product-sold"><i class="fa-solid fa-fire" aria-hidden="true"></i>{{ soldCount }} đã bán</p>
        <p v-if="availability.status === 'LOW_STOCK'" class="stock-note">{{ availability.label }}</p>
      </div>
    </router-link>
    <button class="fav-btn" :class="{ active: favoriteStore.isFavorite(product.productId) }" :disabled="favoritePending" :aria-pressed="favoriteStore.isFavorite(product.productId)" :aria-busy="favoritePending" :aria-label="favoriteStore.isFavorite(product.productId) ? `Bỏ yêu thích ${product.name}` : `Yêu thích ${product.name}`" @click="toggleFavorite"><i :class="favoriteStore.isFavorite(product.productId) ? 'fa-solid fa-heart' : 'fa-regular fa-heart'" aria-hidden="true"></i></button>
    <div class="product-footer">
      <div class="product-price"><span class="price-now">{{ formatPrice(currentPrice) }}</span><span v-if="crossedPrice" class="price-old">{{ formatPrice(crossedPrice) }}</span></div>
      <button v-if="canAdd()" class="add-btn" :class="{ added }" :disabled="pending" :aria-label="pending ? `Đang thêm ${product.name}` : added ? `Đã thêm ${product.name}` : `Thêm ${product.name} vào giỏ`" @click="addToCart"><span v-if="pending" class="mini-spinner"></span><i v-else :class="added ? 'fa-solid fa-check' : 'fa-solid fa-plus'" aria-hidden="true"></i></button>
      <router-link v-else-if="product.cardDataComplete === false || (product.inStock && product.isAvailableNow !== false)" class="option-btn" :to="`/product/${product.productId}`" :aria-label="`Chọn món ${product.name}`"><span>Chọn món</span><i class="fa-solid fa-chevron-right" aria-hidden="true"></i></router-link>
      <button v-else-if="!availability.available" class="option-btn soldout-btn" disabled>Tạm hết</button>
    </div>
    <div v-if="message" class="toast" role="status" aria-live="polite">{{ message }}</div>
  </article>
</template>

<style scoped>
.product-card{position:relative;display:flex;min-width:0;overflow:hidden;flex-direction:column;border:1px solid rgba(89,64,48,.12);border-radius:22px;background:#fff;box-shadow:0 6px 20px rgba(55,35,23,.06);transition:box-shadow var(--transition-normal),transform var(--transition-normal)}
.product-card:hover{box-shadow:0 18px 40px rgba(55,35,23,.13);transform:translateY(-4px)}
.product-main{display:flex;min-width:0;flex:1;flex-direction:column;color:inherit}
.product-main:focus-visible,.fav-btn:focus-visible,.add-btn:focus-visible,.option-btn:focus-visible{outline:3px solid #fb7185;outline-offset:2px}
.product-image{position:relative;overflow:hidden;height:200px;flex:0 0 200px;background:var(--surface)}
.product-image img{width:100%;height:100%;object-fit:cover;outline:1px solid rgba(0,0,0,.08);outline-offset:-1px;transition:transform .35s var(--ease-out)}
.product-card:hover .product-image img{transform:scale(1.04)}
.image-fallback{display:flex;width:100%;height:100%;align-items:center;justify-content:center;flex-direction:column;gap:5px;color:var(--text-light);background:linear-gradient(135deg,#faf6f2,#f0e8e1)}.image-fallback strong{color:var(--primary);font-size:18px;letter-spacing:-.03em}.image-fallback span{font-size:10px}
.product-tags{position:absolute;top:10px;left:10px;display:flex;max-width:calc(100% - 64px);flex-wrap:wrap;gap:5px}
.product-tags>span{display:inline-flex;align-items:center;gap:5px;padding:6px 9px;border-radius:999px;color:#fff;background:var(--primary);font-size:10px;font-weight:800;box-shadow:0 4px 12px rgba(55,35,23,.14)}
.hot-badge,.best-badge,.new-badge{background:var(--primary)}
.stock-badge{position:absolute;inset:0;display:grid;place-items:center;color:var(--text-mid);background:rgba(255,255,255,.8);font-size:13px;font-weight:700;backdrop-filter:blur(2px)}
.product-info{display:flex;min-width:0;flex:1;flex-direction:column;padding:16px 16px 8px}
.product-name{display:-webkit-box;overflow:hidden;min-height:43.2px;font-size:16px;font-weight:700;line-height:1.35;text-wrap:balance;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.product-desc{display:-webkit-box;overflow:hidden;min-height:34.8px;margin-top:6px;color:var(--text-mid);font-size:12px;line-height:1.45;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.product-rating,.product-sold{display:flex;align-items:center;gap:5px;overflow:hidden;color:var(--text-mid);font-size:11px;text-overflow:ellipsis;white-space:nowrap}
.product-rating{margin-top:10px;color:#76513f}.product-rating i{color:#f59e0b}.product-sold{margin-top:5px}
.stock-note{margin-top:5px;color:#b45309;font-size:11px;font-weight:700}
.product-footer{display:flex;min-width:0;align-items:center;justify-content:space-between;gap:8px;margin-top:auto;padding:10px 16px 16px}
.product-price{display:flex;min-width:0;flex-wrap:wrap;align-items:baseline;gap:5px;font-variant-numeric:tabular-nums}
.price-now{color:var(--primary-dark);font-size:20px;font-weight:800}
.price-old{color:var(--text-light);font-size:12px;text-decoration:line-through}
.fav-btn,.add-btn,.option-btn{z-index:2;display:grid;min-height:44px;place-items:center;border:1px solid transparent;cursor:pointer;transition:color 180ms ease-out,background-color 180ms ease-out,border-color 180ms ease-out,box-shadow 180ms ease-out,transform 180ms ease-out}
.fav-btn{position:absolute;top:9px;right:9px;width:44px;height:44px;border-color:#e7e5e4;border-radius:50%;color:var(--text-mid);background:#fff;box-shadow:var(--shadow-xs)}
.fav-btn:hover{border-color:#fb7185;color:#e11d48;background:#fff1f2;box-shadow:0 0 0 3px rgba(251,113,133,.16),0 8px 18px rgba(225,29,72,.16);transform:scale(1.04)}
.fav-btn.active{border-color:#e11d48;color:#fff;background:#e11d48;box-shadow:0 0 0 3px #fff1f2,0 8px 18px rgba(225,29,72,.2)}
.fav-btn:active{transform:scale(.98)}
.fav-btn:disabled{cursor:wait;opacity:.7}
.add-btn{display:grid;width:44px;height:44px;min-width:44px;min-height:44px;place-items:center;border:0;border-radius:50%;color:#fff;background:linear-gradient(135deg,var(--primary),var(--primary-dark));box-shadow:0 8px 18px rgba(212,97,58,.24)}
.add-btn:disabled{cursor:wait;opacity:.7}.add-btn.added{background:#15803d;box-shadow:0 8px 18px rgba(21,128,61,.24)}
.option-btn{grid-auto-flow:column;gap:7px;min-width:44px;padding:0 12px;border-color:transparent;border-radius:999px;color:#fff;background:#f26a2e;font-size:12px;font-weight:700;white-space:nowrap}
.option-btn i{transition:transform 180ms ease-out}
.option-btn:hover{border-color:#f5a06f;background:#dc4f19;box-shadow:0 0 0 3px rgba(242,106,46,.14),0 8px 18px rgba(220,79,25,.18);transform:translateY(-1px)}
.option-btn:hover i{transform:translateX(3px)}
.option-btn:active{transform:translateY(1px)}
.option-btn.soldout-btn{cursor:not-allowed;background:#a8a29e;box-shadow:none}
.option-btn.soldout-btn:hover{background:#a8a29e;box-shadow:none;transform:none}
.mini-spinner{width:15px;height:15px;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;border-radius:50%;animation:spin .7s linear infinite}
.toast{position:absolute;z-index:5;right:10px;bottom:60px;left:10px;padding:9px 12px;border-radius:10px;color:#fff;background:rgba(25,19,15,.92);font-size:12px;text-align:center;box-shadow:var(--shadow-sm)}
.product-card.list-mode .product-main{display:grid;grid-template-columns:minmax(180px,30%) 1fr}
.product-card.list-mode .product-image{height:100%;flex-basis:auto}
.product-card.list-mode .product-info{justify-content:center;padding:24px}
@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:560px){.product-card:not(.list-mode){min-width:0;border-radius:16px}.product-card:not(.list-mode) .product-image{height:auto;flex-basis:auto;aspect-ratio:4/3}.product-card:not(.list-mode) .product-info{padding:10px 9px 6px}.product-card:not(.list-mode) .product-name{min-height:35.1px;font-size:13px}.product-card:not(.list-mode) .product-desc{min-height:31.9px;font-size:11px}.product-card:not(.list-mode) .product-rating,.product-card:not(.list-mode) .product-sold{font-size:10px}.product-card:not(.list-mode) .product-footer{align-items:flex-end;gap:4px;padding:7px 9px 10px}.product-card:not(.list-mode) .product-price{display:block}.product-card:not(.list-mode) .price-now{display:block;font-size:15px;white-space:nowrap}.product-card:not(.list-mode) .price-old{display:block;font-size:10px;white-space:nowrap}.product-card:not(.list-mode) .option-btn{overflow:hidden;max-width:76px;padding:0 7px;text-overflow:ellipsis}.product-card.list-mode .product-main{grid-template-columns:120px 1fr}.product-card.list-mode .product-info{padding:14px}}
@media(prefers-reduced-motion:reduce){.product-card,.product-image img,.fav-btn,.add-btn,.option-btn,.option-btn i{transition:none}.product-card:hover,.product-card:hover .product-image img,.fav-btn:hover,.fav-btn:active,.option-btn:hover,.option-btn:active,.option-btn:hover i{transform:none}.mini-spinner{animation:none}}
</style>
