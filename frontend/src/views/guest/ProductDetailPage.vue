<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import { formatDate, formatPrice } from '@/utils/format';
import { reviewApi } from '@/api';
import StarRating from '@/components/common/StarRating.vue';

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();
const cart = useCartStore();
const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const quantity = ref(1);
const selectedVariant = ref(null);
const activeImageIndex = ref(0);
const loading = ref(true);
const reviews = ref([]);

const product = computed(() => productStore.currentProduct);

const effectivePrice = computed(() => {
  if (!product.value) return 0;
  if (selectedVariant.value) return selectedVariant.value.price;
  return product.value.price;
});

const galleryImages = computed(() => {
  if (!product.value) return [];
  const all = [];
  if (product.value.image) all.push(product.value.image);
  if (product.value.galleryImages && product.value.galleryImages.length) {
    all.push(...product.value.galleryImages);
  }
  return all;
});

onMounted(async () => {
  loading.value = true;
  try {
    if (!productStore.fetched) await productStore.init();
    await productStore.fetchById(route.params.id);
    if (product.value?.variants?.length) {
      const def = product.value.variants.find((v) => v.isDefault);
      selectedVariant.value = def || product.value.variants[0];
    }
    await loadReviews();
    if (auth.isLoggedIn && product.value?.productId) await favoriteStore.check(product.value.productId);
  } finally {
    loading.value = false;
  }
});

function selectVariant(variant) {
  selectedVariant.value = variant;
}

async function loadReviews() {
  if (!product.value?.productId) return;
  reviews.value = await reviewApi.getByProduct(product.value.productId);
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    router.push('/login');
    return;
  }
  await favoriteStore.toggle(product.value);
}

async function addToCart() {
  if (!product.value?.inStock || !selectedVariant.value) return;
  try {
    await cart.addItem(
      product.value.productId,
      selectedVariant.value.variantId,
      quantity.value,
    );
    router.push('/cart');
  } catch (err) {
    console.error('Add to cart failed:', err);
  }
}
</script>

<template>
  <div v-if="loading" class="container" style="padding: 60px 0">
    <div class="empty-state">
      <i class="bi bi-arrow-repeat spin"></i>
      <h3>Đang tải sản phẩm...</h3>
    </div>
  </div>
  <div class="page" v-else-if="product">
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
            <img
              :src="galleryImages[activeImageIndex] || product.image"
              :alt="product.name"
            />
          </div>
          <div v-if="galleryImages.length > 1" class="gallery-thumbs">
            <button
              v-for="(img, idx) in galleryImages"
              :key="idx"
              class="gallery-thumb"
              :class="{ active: activeImageIndex === idx }"
              @click="activeImageIndex = idx"
            >
              <img :src="img" :alt="'Ảnh ' + (idx + 1)" />
            </button>
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
            <span class="detail-price-current">{{
              formatPrice(effectivePrice)
            }}</span>
          </div>
          <p class="detail-desc">{{ product.description }}</p>

          <div
            v-if="product.variants && product.variants.length"
            class="detail-options"
          >
            <label class="option-label">Chọn phân loại:</label>
            <div class="option-list">
              <button
                v-for="v in product.variants"
                :key="v.variantId"
                class="option-chip"
                :class="{ active: selectedVariant?.variantId === v.variantId }"
                @click="selectVariant(v)"
              >
                <span class="option-name">{{ v.variantName }}</span>
                <span class="option-price">+{{ formatPrice(v.price) }}</span>
              </button>
            </div>
          </div>

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
            <button class="btn btn-lg btn-outline favorite-detail-btn" type="button" @click="toggleFavorite">
              <i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i>
              {{ favoriteStore.isFavorite(product.productId) ? 'Đã thích' : 'Thích món' }}
            </button>
          </div>

          <div class="detail-review-placeholder">
            <div class="review-header">
              <h3>Đánh giá sản phẩm</h3>
              <span>{{ reviews.length }} đánh giá</span>
            </div>

            <div v-if="reviews.length" class="review-list">
              <div v-for="review in reviews" :key="review.reviewId" class="review-item">
                <img :src="review.avatarUrl || 'https://i.pravatar.cc/80?u=fastguy'" :alt="review.userName">
                <div>
                  <div class="review-item-head">
                    <strong>{{ review.userName }}</strong>
                    <span>{{ formatDate(review.createdAt) }}</span>
                  </div>
                  <StarRating :modelValue="review.rating" readonly :size="14" />
                  <p>{{ review.comment || 'Ngon, sẽ ủng hộ tiếp.' }}</p>
                </div>
              </div>
            </div>
            <p v-else class="review-empty">Chưa có đánh giá nào.</p>
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
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.detail-image-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-normal);
}
.detail-image-bg:hover img {
  transform: scale(1.08);
}
.gallery-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.gallery-thumb {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  background: #f5f5f5;
  transition: all var(--transition-fast);
}
.gallery-thumb:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
}
.gallery-thumb.active {
  border-color: var(--primary);
}
.gallery-thumb img {
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
  flex-wrap: wrap;
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
.detail-price-extra {
  font-size: 14px;
  color: var(--text-mid);
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
.detail-options {
  margin-bottom: 20px;
}
.option-label {
  font-size: 14px;
  font-weight: 600;
  display: block;
  margin-bottom: 10px;
}
.option-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.option-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
}
.option-chip:hover {
  border-color: var(--primary);
}
.option-chip.active {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.option-price {
  font-size: 13px;
  color: var(--red-active);
  font-weight: 600;
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
  margin-bottom: 32px;
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
.favorite-detail-btn i {
  color: #ef4444;
}
.detail-review-placeholder {
  border-top: 1px solid var(--border);
  padding-top: 24px;
}
.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.review-header h3 {
  font-size: 18px;
  font-weight: 800;
}
.review-header span {
  color: var(--text-mid);
  font-size: 13px;
  font-weight: 700;
}
.review-form {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fffaf1;
  margin-bottom: 18px;
}
.review-form-top {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.review-form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}
.review-help,
.review-message {
  color: var(--text-mid);
  font-size: 12px;
}
.review-message {
  margin-top: 8px;
  font-weight: 700;
}
.review-list {
  display: grid;
  gap: 12px;
}
.review-item {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fff;
}
.review-item img {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  object-fit: cover;
}
.review-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
}
.review-item-head span {
  color: var(--text-light);
  font-size: 12px;
}
.review-item p {
  color: var(--text-mid);
  font-size: 13px;
  margin-top: 4px;
}
.review-empty {
  color: var(--text-light);
  font-size: 14px;
  padding: 20px 0;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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
  .detail-actions,
  .review-form-actions,
  .review-form-top {
    align-items: stretch;
    flex-direction: column;
    grid-template-columns: 1fr;
  }
}
</style>
