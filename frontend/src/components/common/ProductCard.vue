<script setup>
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';

const props = defineProps({
  product: { type: Object, required: true },
  listMode: { type: Boolean, default: false },
});

const cart = useCartStore();
const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const router = useRouter();

  async function addToCart(e) {
    e.stopPropagation();
    const variantId = props.product.defaultVariant?.variantId;
    if (!variantId) return;
    try {
      await cart.addItem(props.product.productId, variantId);
    } catch (err) {
      console.error('Add to cart failed:', err);
    }
  }

  async function toggleFavorite(e) {
    e.stopPropagation();
    if (!auth.isLoggedIn) {
      router.push('/login');
      return;
    }
    try {
      await favoriteStore.toggle(props.product);
    } catch (err) {
      console.error('Toggle favorite failed:', err);
    }
  }

  function goDetail() {
    router.push(`/product/${props.product.productId}`);
  }
</script>

<template>
  <article
    class="product-card"
    :class="{ 'list-mode': listMode }"
    role="link"
    tabindex="0"
    :aria-label="`Xem chi tiết ${product.name}`"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
  >
    <div class="product-image">
      <div class="product-image-bg">
        <img :src="product.image" :alt="product.name" />
        <button
          class="favorite-btn"
          type="button"
          :class="{ active: favoriteStore.isFavorite(product.productId) }"
          :aria-label="favoriteStore.isFavorite(product.productId) ? 'Bỏ thích món' : 'Thích món'"
          @click="toggleFavorite"
        >
          <i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i>
        </button>
        <div v-if="!product.inStock" class="out-of-stock-overlay">Hết hàng</div>
      </div>
    </div>
    <div class="product-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <div class="product-meta">
        <span class="product-rating">
          <i class="bi bi-star-fill"></i> {{ product.rating }}
        </span>
        <span class="product-reviews">({{ product.reviewCount }})</span>
      </div>
      <div class="product-price-row">
        <div class="product-price">
          <template v-if="product.discountPrice">
            <span class="price-current">{{ product.discountPrice.toLocaleString('vi-VN') }}₫</span>
            <span class="price-old">{{ product.price.toLocaleString('vi-VN') }}₫</span>
          </template>
          <template v-else>
            <span class="price-current">{{ product.price.toLocaleString('vi-VN') }}₫</span>
          </template>
        </div>
        <button v-if="product.inStock" class="btn btn-sm btn-primary" type="button" :aria-label="`Thêm ${product.name} vào giỏ`" @click="addToCart">
          <i class="bi bi-plus-lg"></i>
        </button>
        <span v-else class="badge" style="color: var(--text-light); font-size: 11px">Hết hàng</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  border: none;
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
  background: #fff;
}
.product-card:hover,
.product-card:focus-visible {
  box-shadow: var(--shadow-lg);
}
.product-card img {
  transition: transform var(--transition-normal);
}
.product-card:hover img {
  transform: scale(1.05);
}
.product-image {
  padding: 0;
  width: 100%;
}
.product-image-bg {
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  position: relative;
}
.product-image-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.favorite-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255,255,255,0.7);
  border-radius: 999px;
  background: rgba(255,255,255,0.9);
  color: var(--text-mid);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
  z-index: 2;
}
.favorite-btn.active {
  color: #ef4444;
  background: #fff;
}
.out-of-stock-overlay {
  position: absolute; inset: 0;
  background: rgba(255,255,255,0.7);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: var(--text-mid);
}
.product-info {
  padding: 10px 0 0;
}
.product-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-mid);
  margin-bottom: 6px;
}
.product-meta i {
  color: var(--primary);
  font-size: 12px;
}
.product-reviews {
  color: var(--text-light);
}
.product-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.product-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.price-current {
  font-size: 15px;
  font-weight: 700;
  color: var(--red-active);
}
.price-old {
  font-size: 11px;
  color: var(--text-light);
  text-decoration: line-through;
}
.list-mode { display: flex; gap: 12px; }
.list-mode .product-image { width: 120px; flex-shrink: 0; }
.list-mode .product-info { flex: 1; padding-top: 0; display: flex; flex-direction: column; justify-content: center; }
</style>
