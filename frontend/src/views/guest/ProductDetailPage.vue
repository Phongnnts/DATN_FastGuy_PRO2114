<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';
import { formatPrice } from '@/utils/format';
import StarRating from '@/components/common/StarRating.vue';

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();
const cart = useCartStore();
const quantity = ref(1);

const product = computed(() => productStore.currentProduct);

onMounted(async () => {
  if (!productStore.fetched) await productStore.init();
  productStore.fetchById(route.params.id);
});

async function addToCart() {
  if (!product.value?.inStock) return;
  try {
    await cart.addItem(product.value.id, quantity.value);
    router.push('/cart');
  } catch (err) {
    console.error('Add to cart failed:', err);
  }
}
</script>

<template>
  <div class="page" v-if="product">
    <div class="container">
      <div class="breadcrumb">
        <router-link to="/">Trang chủ</router-link>
        <span class="breadcrumb-sep">/</span>
        <router-link to="/menu">Thực đơn</router-link>
        <span class="breadcrumb-sep">/</span>
        <span>{{ product.name }}</span>
      </div>
      <div class="detail-layout">
        <div class="detail-image">
          <div class="detail-image-bg">
            <img :src="product.image" :alt="product.name" />
          </div>
        </div>
        <div class="detail-info">
          <h1 class="detail-title">{{ product.name }}</h1>
          <div class="detail-rating">
            <StarRating :modelValue="product.rating" readonly :size="18" />
            <span class="detail-rating-text"
              >{{ product.rating }} ({{ product.reviewCount }} đánh giá)</span
            >
          </div>
          <div class="detail-price">
            <template v-if="product.discountPrice">
              <span class="detail-price-current">{{
                formatPrice(product.discountPrice)
              }}</span>
              <span class="detail-price-old">{{
                formatPrice(product.price)
              }}</span>
              <span class="detail-discount"
                >-{{
                  Math.round((1 - product.discountPrice / product.price) * 100)
                }}%</span
              >
            </template>
            <template v-else>
              <span class="detail-price-current">{{
                formatPrice(product.price)
              }}</span>
            </template>
          </div>
          <p class="detail-desc">{{ product.description }}</p>
          <div class="detail-avail">
            <span v-if="product.inStock" class="avail-yes"
              ><i class="bi bi-check-circle-fill"></i> Còn hàng</span
            >
            <span v-else class="avail-no"
              ><i class="bi bi-x-circle-fill"></i> Hết hàng</span
            >
          </div>
          <div class="detail-actions">
            <div class="qty-selector">
              <button
                class="qty-btn"
                @click="quantity = Math.max(1, quantity - 1)"
              >
                <i class="bi bi-dash"></i>
              </button>
              <input
                type="number"
                v-model.number="quantity"
                min="1"
                class="qty-input"
              />
              <button class="qty-btn" @click="quantity++">
                <i class="bi bi-plus"></i>
              </button>
            </div>
            <button
              class="btn btn-lg btn-primary"
              :disabled="!product.inStock"
              @click="addToCart"
            >
              <i class="bi bi-cart-plus"></i> Thêm vào giỏ
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="container" style="padding: 60px 0">
    <div class="empty-state">
      <i class="bi bi-box"></i>
      <h3>Không tìm thấy sản phẩm</h3>
      <router-link to="/menu" class="btn btn-primary"
        >Quay lại thực đơn</router-link
      >
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 24px 0;
}
.breadcrumb {
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 28px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.breadcrumb a {
  color: var(--text-mid);
}
.breadcrumb a:hover {
  color: var(--primary);
}
.breadcrumb-sep {
  color: var(--border);
}
.detail-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: start;
}
.detail-image {
  position: sticky;
  top: 80px;
}
.detail-image-bg {
  background: #f5f5f5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
}
.detail-image-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.detail-title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 12px;
  line-height: 1.2;
}
.detail-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.detail-rating-text {
  font-size: 14px;
  color: var(--text-mid);
}
.detail-price {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.detail-price-current {
  font-size: 32px;
  font-weight: 800;
  color: var(--red-active);
}
.detail-price-old {
  font-size: 18px;
  color: var(--text-light);
  text-decoration: line-through;
}
.detail-discount {
  background: var(--red-active);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
}
.detail-desc {
  font-size: 15px;
  color: var(--text-mid);
  line-height: 1.7;
  margin-bottom: 20px;
}
.detail-avail {
  margin-bottom: 28px;
}
.avail-yes {
  color: #4caf50;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.avail-no {
  color: var(--red-active);
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.qty-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 3px;
}
.qty-btn {
  width: 34px;
  height: 34px;
  border: none;
  background: var(--bg);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 15px;
  color: var(--text-dark);
}
.qty-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
}
.qty-input {
  width: 44px;
  text-align: center;
  border: none;
  font-size: 16px;
  font-weight: 700;
  background: transparent;
  -moz-appearance: textfield;
}
.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}
@media (max-width: 768px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .detail-image {
    position: static;
  }
  .detail-image-bg {
    aspect-ratio: 1;
  }
  .detail-title {
    font-size: 22px;
  }
  .detail-price-current {
    font-size: 26px;
  }
}
</style>
