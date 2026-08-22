<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import { canDirectAddProduct } from '@/utils/productCard';

const props = defineProps({ product: { type: Object, required: true }, listMode: { type: Boolean, default: false }, homepage: { type: Boolean, default: false } });
const cart = useCartStore();
const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const router = useRouter();
const pending = ref(false);
const message = ref('');
const imageFailed = ref(false);
let messageTimer;
const discountPrice = computed(() => {
  const price = Number(props.product.price);
  const discount = Number(props.product.discountPrice);
  return price > 0 && discount > 0 && discount < price ? discount : null;
});
const originalPrice = computed(() => Number(props.product.originalPrice) > Number(props.product.price) ? Number(props.product.originalPrice) : null);
const hasDiscount = computed(() => props.product.discountPercent != null || discountPrice.value);
const currentPrice = computed(() => discountPrice.value || Number(props.product.price));
const crossedPrice = computed(() => originalPrice.value || (discountPrice.value ? Number(props.product.price) : null));
const hasOptions = computed(() => props.product.variants?.length > 1 || props.product.modifierGroups?.length > 0);
const averageRating = computed(() => Math.min(5, Math.max(0, Number(props.product.averageRating) || 0)));
const reviewCount = computed(() => Math.max(0, Math.floor(Number(props.product.reviewCount) || 0)));
const ratingText = computed(() => reviewCount.value > 0 ? `★ ${averageRating.value.toFixed(1)}/5` : 'Chưa có đánh giá');
const ratingLabel = computed(() => reviewCount.value > 0 ? `Đánh giá ${averageRating.value.toFixed(1)} trên 5 từ ${reviewCount.value} lượt` : 'Chưa có đánh giá, 0 lượt');
const formatPrice = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value) || 0);
const canAdd = () => canDirectAddProduct(props.product);
function notify(value) { message.value = value; clearTimeout(messageTimer); messageTimer = setTimeout(() => { message.value = ''; }, 2500); }
onBeforeUnmount(() => clearTimeout(messageTimer));
async function addToCart() {
  const variantId = props.product.defaultVariant?.variantId;
  if (!variantId || !canAdd() || pending.value) return;
  const stock = props.product.defaultVariant?.quantityAvailable;
  if (stock !== null && stock !== undefined && Number(stock) <= 0) return notify('Món đã hết hàng');
  pending.value = true;
  try { await cart.addItem(props.product.productId, variantId); notify('Đã thêm vào giỏ hàng'); } catch (error) { notify(error.message || 'Không thể thêm vào giỏ'); } finally { pending.value = false; }
}
async function toggleFavorite() {
  if (!auth.isLoggedIn) { router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }); return; }
  try { await favoriteStore.toggle(props.product); } catch (error) { notify(error.message || 'Không thể cập nhật yêu thích'); }
}
</script>

<template>
  <article class="product-card" :class="{ 'list-mode': listMode, 'homepage-card': homepage }">
    <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
      <div class="product-image"><img v-if="product.image && !imageFailed" :src="product.image" :alt="product.name" loading="lazy" decoding="async" @error="imageFailed = true"><div v-else class="image-fallback" role="img" :aria-label="`Chưa có ảnh ${product.name}`"><i class="bi bi-image"></i></div><div class="badges"><span v-if="product.bestSeller" class="best-badge">Bán chạy</span><span v-if="product.isNew" class="new-badge">Mới</span><span v-if="hasDiscount" class="hot-badge">-{{ Math.round(product.discountPercent ?? (1 - discountPrice / product.price) * 100) }}%</span><span v-if="product.spiceLevel > 0" class="spice-badge">Cay {{ product.spiceLevel }}/3</span><span v-if="hasOptions" class="option-badge">Tùy chọn</span></div><span class="rating-badge" :aria-label="ratingLabel">{{ ratingText }}</span><div v-if="!product.inStock || product.isAvailableNow === false" class="stock-badge">{{ product.isAvailableNow === false ? 'Ngoài giờ bán' : 'Hết hàng' }}</div></div>
      <div class="product-info"><div class="product-meta"><span>{{ product.categoryName }}</span><span v-if="product.soldCount">Đã bán {{ product.soldCount }}</span></div><h3 class="product-name">{{ product.name }}</h3><p v-if="product.description" class="product-desc">{{ product.description }}</p></div>
    </router-link>
    <button class="fav-btn" :class="{ active: favoriteStore.isFavorite(product.productId) }" :aria-label="favoriteStore.isFavorite(product.productId) ? `Bỏ yêu thích ${product.name}` : `Yêu thích ${product.name}`" @click="toggleFavorite"><i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i></button>
    <div class="product-footer">
      <div class="product-price"><template v-if="crossedPrice"><span class="price-now">{{ formatPrice(currentPrice) }}</span><span class="price-old">{{ formatPrice(crossedPrice) }}</span></template><span v-else class="price-now">{{ formatPrice(currentPrice) }}</span></div>
      <button v-if="canAdd()" class="add-btn" :disabled="pending" :aria-label="pending ? `Đang thêm ${product.name}` : `Thêm ${product.name} vào giỏ`" @click="addToCart"><span v-if="pending" class="mini-spinner"></span><template v-else><i class="bi bi-plus" aria-hidden="true"></i><span v-if="homepage">Thêm</span></template></button>
      <router-link v-else-if="product.inStock && product.isAvailableNow !== false" class="option-btn" :to="`/product/${product.productId}`" :aria-label="`Chọn món ${product.name}`"><span v-if="homepage">Chọn món</span><span v-else>Tùy chọn</span><i class="bi bi-chevron-right" aria-hidden="true"></i></router-link>
    </div>
    <div v-if="message" class="toast" role="status" aria-live="polite">{{ message }}</div>
  </article>
</template>

<style scoped>
.product-card{position:relative;overflow:hidden;border:1px solid var(--border-light);border-radius:var(--radius-lg);background:#fff;transition:box-shadow var(--transition-normal),transform var(--transition-normal)}.product-card:hover{box-shadow:0 12px 28px rgba(30,20,15,.1);transform:translateY(-3px)}.product-main{display:block;color:inherit}.product-main:focus-visible{outline:3px solid var(--primary-50);outline-offset:-3px}.product-image{position:relative;overflow:hidden;aspect-ratio:1.32;background:var(--surface)}.product-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s var(--ease-out)}.product-card:hover .product-image img{transform:scale(1.04)}.image-fallback{display:grid;width:100%;height:100%;place-items:center;color:var(--text-light);background:linear-gradient(135deg,#faf6f2,#f0e8e1);font-size:34px}.badges{position:absolute;top:8px;left:8px;display:flex;flex-wrap:wrap;gap:4px;max-width:calc(100% - 54px)}.badges span{padding:4px 7px;border-radius:99px;color:#fff;font-size:9px;font-weight:800}.hot-badge{background:#dc2626}.best-badge{background:#b45309}.new-badge{background:#166534}.spice-badge{background:#b91c1c}.option-badge{background:#0f766e}.rating-badge{position:absolute;right:8px;bottom:8px;max-width:calc(100% - 16px);overflow:hidden;padding:5px 8px;border-radius:99px;color:#fff;background:rgba(30,20,15,.82);font-size:10px;font-weight:800;line-height:1.2;text-overflow:ellipsis;white-space:nowrap}.product-meta{display:flex;justify-content:space-between;gap:6px;margin-bottom:5px;color:var(--text-mid);font-size:10px}.product-meta span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.stock-badge{position:absolute;inset:0;display:grid;place-items:center;color:var(--text-mid);background:rgba(255,255,255,.8);font-size:13px;font-weight:700;backdrop-filter:blur(2px)}.product-info{padding:13px 14px 15px}.product-name{display:-webkit-box;overflow:hidden;margin-bottom:4px;font-size:14px;font-weight:650;line-height:1.4;-webkit-line-clamp:2;-webkit-box-orient:vertical}.product-desc{overflow:hidden;margin:0 0 10px;color:var(--text-mid);font-size:12px;line-height:1.35;text-overflow:ellipsis;white-space:nowrap}.product-price{display:flex;align-items:baseline;gap:6px;min-height:22px}.price-now{color:var(--primary);font-size:15px;font-weight:750}.price-old{color:var(--text-light);font-size:12px;text-decoration:line-through}.fav-btn,.add-btn,.option-btn{position:absolute;z-index:2;display:grid;min-height:44px;place-items:center;border:0;cursor:pointer}.fav-btn,.add-btn{width:44px;height:44px;border-radius:50%}.option-btn{right:12px;bottom:12px;grid-auto-flow:column;gap:5px;padding:0 12px;border-radius:22px;color:#fff;background:var(--primary);font-size:12px;font-weight:700}.fav-btn{top:9px;right:9px;color:var(--text-mid);background:rgba(255,255,255,.92);box-shadow:var(--shadow-xs)}.fav-btn.active{color:var(--primary)}.add-btn{right:13px;bottom:13px;color:#fff;background:var(--primary);font-size:22px;box-shadow:0 6px 15px rgba(230,80,35,.25)}.add-btn:disabled{cursor:wait;opacity:.7}.mini-spinner{width:15px;height:15px;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;border-radius:50%;animation:spin .7s linear infinite}.toast{position:absolute;z-index:5;right:10px;bottom:60px;left:10px;padding:9px 12px;border-radius:10px;color:#fff;background:rgba(25,19,15,.92);font-size:12px;text-align:center;box-shadow:var(--shadow-sm)}.list-mode .product-main{display:grid;grid-template-columns:180px 1fr}.list-mode .product-image{height:140px;aspect-ratio:auto}.list-mode .product-desc{white-space:normal}.list-mode .add-btn{bottom:18px}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:520px){.list-mode .product-main{grid-template-columns:120px 1fr}.list-mode .product-image{height:130px}}@media(prefers-reduced-motion:reduce){.product-card,.product-image img{transition:none}.product-card:hover{transform:none}}
.product-card.list-mode{min-height:180px}.product-card.list-mode .product-main{display:grid;grid-template-columns:minmax(180px,30%) 1fr}.product-card.list-mode .product-image{height:100%;aspect-ratio:auto}.product-card.list-mode .product-info{display:flex;flex-direction:column;justify-content:center;padding:24px}.product-card.list-mode .product-desc{white-space:normal;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}.product-card.list-mode .add-btn{right:16px;bottom:16px}@media(max-width:560px){.product-card:not(.list-mode){border-radius:14px}.product-card:not(.list-mode) .product-info{padding:9px 9px 12px}.product-card:not(.list-mode) .product-name{font-size:12px}.product-card:not(.list-mode) .product-desc{display:none}.product-card:not(.list-mode) .price-now{font-size:13px}.product-card:not(.list-mode) .fav-btn,.product-card:not(.list-mode) .add-btn{width:44px;height:44px;min-height:44px}.product-card:not(.list-mode) .option-btn{right:7px;bottom:7px;min-height:44px;padding:0 10px}.product-card:not(.list-mode) .option-btn span{display:none}.product-card.list-mode .product-main{grid-template-columns:120px 1fr}.product-card.list-mode .product-info{padding:14px}}
</style>

<style scoped>
.product-card { display: flex; flex-direction: column; }
.product-main { flex: 1; }
.product-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 58px; padding: 10px 14px 14px; border-top: 1px solid var(--border-light); }
.product-footer .product-price { min-width: 0; }
.product-footer .add-btn, .product-footer .option-btn { position: static; flex: 0 0 auto; }
.product-footer .option-btn { min-height: 38px; }
.product-card.list-mode .product-main { flex: 1; }
@media(max-width:560px){.product-footer{min-height:60px;padding:8px 9px}.product-footer .option-btn{min-height:44px;padding:0 10px}.product-footer .option-btn span{display:none}.rating-badge{max-width:calc(100% - 16px);font-size:9px}}
.homepage-card{border:0;border-radius:22px;box-shadow:0 12px 34px rgba(43,25,15,.08)}.homepage-card .product-image{aspect-ratio:1.18}.homepage-card .product-info{padding:17px 18px 14px}.homepage-card .product-meta{margin-bottom:8px;color:#8d7b70;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.06em}.homepage-card .product-name{font-size:17px;font-weight:850;line-height:1.25;letter-spacing:-.025em}.homepage-card .product-desc{display:-webkit-box;margin-top:7px;line-height:1.5;white-space:normal;-webkit-box-orient:vertical;-webkit-line-clamp:2}.homepage-card .product-footer{min-height:70px;padding:13px 18px 17px;border-top:0}.homepage-card .price-now{font-size:19px;font-weight:900}.homepage-card .price-old{font-size:11px}.homepage-card .add-btn{display:flex;width:auto;min-width:88px;height:44px;gap:5px;padding:0 15px;border-radius:999px;font-size:12px;font-weight:850}.homepage-card .add-btn i{font-size:17px}.homepage-card .option-btn{min-height:44px;padding:0 15px}.homepage-card .badges{top:12px;left:12px}.homepage-card .badges span{padding:6px 9px;font-size:9px;letter-spacing:.04em;text-transform:uppercase}.homepage-card .best-badge,.homepage-card .hot-badge{background:#f26a2e}.homepage-card:hover{box-shadow:0 20px 42px rgba(43,25,15,.14)}@media(max-width:560px){.homepage-card{border-radius:15px}.homepage-card .product-image{aspect-ratio:1.05}.homepage-card .product-info{padding:10px 10px 7px}.homepage-card .product-name{font-size:13px}.homepage-card .product-meta span:first-child{display:none}.homepage-card .product-footer{min-height:62px;padding:7px 9px 10px}.homepage-card .price-now{font-size:14px}.homepage-card .add-btn{width:44px;min-width:44px;padding:0;border-radius:50%}.homepage-card .add-btn span{display:none}.homepage-card .option-btn span{display:none}}
</style>
