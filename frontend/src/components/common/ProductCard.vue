<script setup>
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';

const props = defineProps({
  product: { type: Object, required: true },
});

const cart = useCartStore();
const router = useRouter();

async function addToCart(e) {
  e.stopPropagation();
  try {
    await cart.addItem(props.product.id);
  } catch (err) {
    console.error('Add to cart failed:', err);
  }
}

function goDetail() {
  router.push(`/product/${props.product.id}`);
}
</script>

<template>
  <div class="product-card" @click="goDetail">
    <div class="product-image">
      <div class="product-image-bg">
        <img :src="product.image" :alt="product.name" />
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
            <span class="price-current"
              >{{ product.discountPrice.toLocaleString('vi-VN') }}₫</span
            >
            <span class="price-old"
              >{{ product.price.toLocaleString('vi-VN') }}₫</span
            >
          </template>
          <template v-else>
            <span class="price-current"
              >{{ product.price.toLocaleString('vi-VN') }}₫</span
            >
          </template>
        </div>
        <button
          v-if="product.inStock"
          class="btn btn-sm btn-primary"
          @click="addToCart"
        >
          <i class="bi bi-plus-lg"></i>
        </button>
        <span
          v-else
          class="badge"
          style="color: var(--text-light); font-size: 11px"
          >Hết hàng</span
        >
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s;
  background: #fff;
}
.product-card:hover {
  border-color: var(--primary);
}
.product-image {
  padding: 20px 20px 0;
}
.product-image-bg {
  background: #f9f9f9;
  border-radius: calc(var(--radius) - 4px);
  display: flex;
  align-items: center;
  justify-content: center;
  height: 140px;
  overflow: hidden;
}
.product-image-bg img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 12px;
}
.product-info {
  padding: 14px 16px 16px;
}
.product-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-mid);
  margin-bottom: 10px;
}
.product-meta i {
  color: var(--primary);
  font-size: 13px;
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
  font-size: 17px;
  font-weight: 700;
  color: var(--red-active);
}
.price-old {
  font-size: 12px;
  color: var(--text-light);
  text-decoration: line-through;
}
</style>
