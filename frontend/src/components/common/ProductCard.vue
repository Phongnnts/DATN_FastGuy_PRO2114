<script setup>
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';

const props = defineProps({ product: { type: Object, required: true }, listMode: { type: Boolean, default: false } });
const cart = useCartStore();
const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const router = useRouter();
const canAdd = () => props.product.inStock && props.product.isAvailableNow !== false && !!props.product.defaultVariant?.variantId;

async function addToCart() {
  const variantId = props.product.defaultVariant?.variantId;
  if (!variantId || !canAdd()) return;
  const stock = props.product.defaultVariant?.quantityAvailable;
  if (stock !== null && stock !== undefined && Number(stock) <= 0) return alert('Món đã hết hàng');
  try { await cart.addItem(props.product.productId, variantId); } catch (error) { alert(error.message || 'Không thể thêm vào giỏ'); }
}
async function toggleFavorite() { if (!auth.isLoggedIn) { router.push('/login'); return; } try { await favoriteStore.toggle(props.product); } catch {} }
</script>

<template>
  <article class="product-card" :class="{ 'list-mode': listMode }">
    <router-link :to="`/product/${product.productId}`" class="product-main" :aria-label="`Xem chi tiết ${product.name}`">
      <div class="product-image"><img :src="product.image" :alt="product.name" loading="lazy"><div v-if="product.discountPrice" class="hot-badge">-{{ Math.round((1 - product.discountPrice / product.price) * 100) }}%</div><div v-if="!product.inStock" class="stock-badge">{{ product.isAvailableNow === false ? 'Ngoài giờ bán' : 'Hết hàng' }}</div></div>
      <div class="product-info"><h3 class="product-name">{{ product.name }}</h3><p v-if="product.description" class="product-desc">{{ product.description }}</p><div class="product-price"><template v-if="product.discountPrice"><span class="price-now">{{ Number(product.discountPrice).toLocaleString('vi-VN') }}₫</span><span class="price-old">{{ Number(product.price).toLocaleString('vi-VN') }}₫</span></template><span v-else class="price-now">{{ Number(product.price).toLocaleString('vi-VN') }}₫</span></div></div>
    </router-link>
    <button class="fav-btn" :class="{ active: favoriteStore.isFavorite(product.productId) }" :aria-label="favoriteStore.isFavorite(product.productId) ? `Bỏ yêu thích ${product.name}` : `Yêu thích ${product.name}`" @click="toggleFavorite"><i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i></button>
    <button v-if="canAdd()" class="add-btn" :aria-label="`Thêm ${product.name} vào giỏ`" @click="addToCart"><i class="bi bi-plus"></i></button>
  </article>
</template>

<style scoped>
.product-card { position:relative; overflow:hidden; border:1px solid var(--border-light); border-radius:var(--radius-lg); background:#fff; transition:box-shadow var(--transition-normal),transform var(--transition-normal); }.product-card:hover { box-shadow:0 12px 28px rgba(30,20,15,.1); transform:translateY(-3px); }.product-main { display:block; color:inherit; }.product-main:focus-visible { outline:3px solid var(--primary-50); outline-offset:-3px; }.product-image { position:relative; overflow:hidden; aspect-ratio:1.32; background:var(--surface); }.product-image img { width:100%; height:100%; object-fit:cover; transition:transform .35s var(--ease-out); }.product-card:hover .product-image img { transform:scale(1.04); }.hot-badge { position:absolute; top:10px; left:10px; padding:4px 8px; border-radius:var(--radius-full); background:rgba(23,23,23,.72); color:#fff; font-size:10px; font-weight:800; }.stock-badge { position:absolute; inset:0; display:grid; place-items:center; color:var(--text-mid); font-size:13px; font-weight:700; background:rgba(255,255,255,.78); backdrop-filter:blur(2px); }.product-info { padding:13px 14px 15px; }.product-name { display:-webkit-box; overflow:hidden; margin-bottom:4px; font-size:14px; font-weight:650; line-height:1.4; -webkit-line-clamp:2; -webkit-box-orient:vertical; }.product-desc { overflow:hidden; margin:0 0 10px; color:var(--text-mid); font-size:12px; line-height:1.35; text-overflow:ellipsis; white-space:nowrap; }.product-price { display:flex; align-items:baseline; gap:6px; min-height:22px; }.price-now { color:var(--primary); font-size:15px; font-weight:750; }.price-old { color:var(--text-light); font-size:12px; text-decoration:line-through; }.fav-btn,.add-btn { position:absolute; z-index:2; display:grid; place-items:center; width:40px; height:40px; border:0; border-radius:50%; cursor:pointer; transition:transform var(--transition-fast),background var(--transition-fast); }.fav-btn { top:9px; right:9px; color:var(--text-mid); background:rgba(255,255,255,.92); box-shadow:var(--shadow-xs); }.fav-btn.active { color:#ef4444; }.fav-btn:hover,.fav-btn:focus-visible { transform:scale(1.08); }.add-btn { right:12px; bottom:12px; color:#fff; background:var(--primary); box-shadow:0 4px 10px rgba(232,115,74,.28); }.add-btn:hover { background:var(--primary-dark); transform:scale(1.06); }.list-mode { display:flex; }.list-mode .product-main { display:flex; flex:1; gap:14px; }.list-mode .product-image { width:150px; flex-shrink:0; aspect-ratio:auto; }.list-mode .product-info { display:flex; flex:1; flex-direction:column; justify-content:center; }
</style>
