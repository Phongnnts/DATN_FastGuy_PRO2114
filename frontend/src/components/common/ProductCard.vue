<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';

const props = defineProps({ product: { type: Object, required: true }, listMode: { type: Boolean, default: false } });
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
const hasOptions = computed(() => props.product.variants?.length > 1 || props.product.modifierGroups?.length > 0);
const formatPrice = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value) || 0);
const canAdd = () => props.product.inStock && props.product.isAvailableNow !== false && !!props.product.defaultVariant?.variantId && Array.isArray(props.product.modifierGroups) && !props.product.modifierGroups.some((group) => Number(group.minSelections) > 0) && Array.isArray(props.product.variants) && props.product.variants.length === 1;
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
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } }); return; }
  try { await favoriteStore.toggle(props.product); } catch (error) { notify(error.message || 'Không thể cập nhật yêu thích'); }
}
</script>

<template>
  <article class="product-card" :class="{ 'list-mode': listMode }">
    <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
      <div class="product-image"><img v-if="product.image && !imageFailed" :src="product.image" :alt="product.name" loading="lazy" decoding="async" @error="imageFailed = true"><div v-else class="image-fallback" role="img" :aria-label="`Chưa có ảnh ${product.name}`"><i class="bi bi-image"></i></div><div class="badges"><span v-if="product.bestSeller" class="best-badge">Bán chạy</span><span v-if="discountPrice" class="hot-badge">-{{ Math.round((1 - discountPrice / product.price) * 100) }}%</span><span v-if="product.combo || product.productType === 'COMBO'" class="combo-badge">Combo</span><span v-if="hasOptions" class="option-badge">Tùy chọn</span></div><div v-if="!product.inStock || product.isAvailableNow === false" class="stock-badge">{{ product.isAvailableNow === false ? 'Ngoài giờ bán' : 'Hết hàng' }}</div></div>
      <div class="product-info"><div class="product-meta"><span>{{ product.categoryName }}</span><span v-if="product.soldCount">Đã bán {{ product.soldCount }}</span></div><h3 class="product-name">{{ product.name }}</h3><p v-if="product.description" class="product-desc">{{ product.description }}</p><div class="product-price"><template v-if="discountPrice"><span class="price-now">{{ formatPrice(discountPrice) }}</span><span class="price-old">{{ formatPrice(product.price) }}</span></template><span v-else class="price-now">{{ formatPrice(product.price) }}</span></div></div>
    </router-link>
    <button class="fav-btn" :class="{ active: favoriteStore.isFavorite(product.productId) }" :aria-label="favoriteStore.isFavorite(product.productId) ? `Bỏ yêu thích ${product.name}` : `Yêu thích ${product.name}`" @click="toggleFavorite"><i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i></button>
    <button v-if="canAdd()" class="add-btn" :disabled="pending" :aria-label="pending ? `Đang thêm ${product.name}` : `Thêm ${product.name} vào giỏ`" @click="addToCart"><span v-if="pending" class="mini-spinner"></span><i v-else class="bi bi-plus"></i></button>
    <router-link v-else-if="product.inStock && product.isAvailableNow !== false" class="option-btn" :to="`/product/${product.productId}`" :aria-label="`Tùy chọn cho ${product.name}`"><span>Tùy chọn</span><i class="bi bi-chevron-right"></i></router-link>
    <div v-if="message" class="toast" role="status" aria-live="polite">{{ message }}</div>
  </article>
</template>

<style scoped>
.product-card{position:relative;overflow:hidden;border:1px solid var(--border-light);border-radius:var(--radius-lg);background:#fff;transition:box-shadow var(--transition-normal),transform var(--transition-normal)}.product-card:hover{box-shadow:0 12px 28px rgba(30,20,15,.1);transform:translateY(-3px)}.product-main{display:block;color:inherit}.product-main:focus-visible{outline:3px solid var(--primary-50);outline-offset:-3px}.product-image{position:relative;overflow:hidden;aspect-ratio:1.32;background:var(--surface)}.product-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s var(--ease-out)}.product-card:hover .product-image img{transform:scale(1.04)}.image-fallback{display:grid;width:100%;height:100%;place-items:center;color:var(--text-light);background:linear-gradient(135deg,#faf6f2,#f0e8e1);font-size:34px}.badges{position:absolute;top:8px;left:8px;display:flex;flex-wrap:wrap;gap:4px;max-width:calc(100% - 54px)}.badges span{padding:4px 7px;border-radius:99px;color:#fff;font-size:9px;font-weight:800}.hot-badge{background:#dc2626}.best-badge{background:#f59e0b}.combo-badge{background:#7c3aed}.option-badge{background:#0f766e}.product-meta{display:flex;justify-content:space-between;gap:6px;margin-bottom:5px;color:var(--text-mid);font-size:10px}.product-meta span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.stock-badge{position:absolute;inset:0;display:grid;place-items:center;color:var(--text-mid);background:rgba(255,255,255,.8);font-size:13px;font-weight:700;backdrop-filter:blur(2px)}.product-info{padding:13px 14px 15px}.product-name{display:-webkit-box;overflow:hidden;margin-bottom:4px;font-size:14px;font-weight:650;line-height:1.4;-webkit-line-clamp:2;-webkit-box-orient:vertical}.product-desc{overflow:hidden;margin:0 0 10px;color:var(--text-mid);font-size:12px;line-height:1.35;text-overflow:ellipsis;white-space:nowrap}.product-price{display:flex;align-items:baseline;gap:6px;min-height:22px}.price-now{color:var(--primary);font-size:15px;font-weight:750}.price-old{color:var(--text-light);font-size:12px;text-decoration:line-through}.fav-btn,.add-btn,.option-btn{position:absolute;z-index:2;display:grid;min-height:44px;place-items:center;border:0;cursor:pointer}.fav-btn,.add-btn{width:44px;height:44px;border-radius:50%}.option-btn{right:12px;bottom:12px;grid-auto-flow:column;gap:5px;padding:0 12px;border-radius:22px;color:#fff;background:var(--primary);font-size:12px;font-weight:700}.fav-btn{top:9px;right:9px;color:var(--text-mid);background:rgba(255,255,255,.92);box-shadow:var(--shadow-xs)}.fav-btn.active{color:var(--primary)}.add-btn{right:13px;bottom:13px;color:#fff;background:var(--primary);font-size:22px;box-shadow:0 6px 15px rgba(230,80,35,.25)}.add-btn:disabled{cursor:wait;opacity:.7}.mini-spinner{width:15px;height:15px;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;border-radius:50%;animation:spin .7s linear infinite}.toast{position:absolute;z-index:5;right:10px;bottom:60px;left:10px;padding:9px 12px;border-radius:10px;color:#fff;background:rgba(25,19,15,.92);font-size:12px;text-align:center;box-shadow:var(--shadow-sm)}.list-mode .product-main{display:grid;grid-template-columns:180px 1fr}.list-mode .product-image{height:140px;aspect-ratio:auto}.list-mode .product-desc{white-space:normal}.list-mode .add-btn{bottom:18px}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:520px){.list-mode .product-main{grid-template-columns:120px 1fr}.list-mode .product-image{height:130px}}@media(prefers-reduced-motion:reduce){.product-card,.product-image img{transition:none}.product-card:hover{transform:none}}
.product-card.list-mode{min-height:180px}.product-card.list-mode .product-main{display:grid;grid-template-columns:minmax(180px,30%) 1fr}.product-card.list-mode .product-image{height:100%;aspect-ratio:auto}.product-card.list-mode .product-info{display:flex;flex-direction:column;justify-content:center;padding:24px}.product-card.list-mode .product-desc{white-space:normal;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}.product-card.list-mode .add-btn{right:16px;bottom:16px}@media(max-width:560px){.product-card:not(.list-mode){border-radius:14px}.product-card:not(.list-mode) .product-info{padding:9px 9px 12px}.product-card:not(.list-mode) .product-name{font-size:12px}.product-card:not(.list-mode) .product-desc{display:none}.product-card:not(.list-mode) .price-now{font-size:13px}.product-card:not(.list-mode) .fav-btn,.product-card:not(.list-mode) .add-btn{width:36px;height:36px;min-height:36px}.product-card:not(.list-mode) .option-btn{right:7px;bottom:7px;min-height:36px;padding:0 9px}.product-card:not(.list-mode) .option-btn span{display:none}.product-card.list-mode .product-main{grid-template-columns:120px 1fr}.product-card.list-mode .product-info{padding:14px}}
</style>
