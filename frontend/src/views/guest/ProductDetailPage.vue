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
const selectedOption = ref(null);
const activeImageIndex = ref(0);

const product = computed(() => productStore.currentProduct);

const effectivePrice = computed(() => {
  if (!product.value) return 0;
  const base = product.value.discountPrice || product.value.price;
  if (selectedOption.value) {
    return base + selectedOption.value.extraPrice;
  }
  return base;
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
  if (!productStore.fetched) await productStore.init();
  await productStore.fetchById(route.params.id);
});

function selectOption(option) {
  selectedOption.value =
    selectedOption.value?.optionId === option.optionId ? null : option;
}

function getOptionData() {
  if (!selectedOption.value) return null;
  return {
    optionId: selectedOption.value.optionId,
    optionName: selectedOption.value.optionName,
    extraPrice: selectedOption.value.extraPrice,
  };
}

async function addToCart() {
  if (!product.value?.inStock) return;
  try {
    const optionData = getOptionData();
    await cart.addItem(product.value.id, quantity.value, optionData);
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
            <img :src="galleryImages[activeImageIndex] || product.image" :alt="product.name" />
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
            <template v-if="product.discountPrice">
              <span class="detail-price-current">{{
                formatPrice(effectivePrice)
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
                formatPrice(effectivePrice)
              }}</span>
              <span v-if="selectedOption" class="detail-price-extra"
                >(+{{ formatPrice(selectedOption.extraPrice) }})</span
              >
            </template>
          </div>
          <p class="detail-desc">{{ product.description }}</p>

          <div v-if="product.options && product.options.length" class="detail-options">
            <label class="option-label">Chọn tùy chọn:</label>
            <div class="option-list">
              <button
                v-for="opt in product.options"
                :key="opt.optionId"
                class="option-chip"
                :class="{ active: selectedOption?.optionId === opt.optionId }"
                @click="selectOption(opt)"
              >
                <span class="option-name">{{ opt.optionName }}</span>
                <span v-if="opt.extraPrice > 0" class="option-price"
                  >+{{ formatPrice(opt.extraPrice) }}</span
                >
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
          </div>

          <div class="detail-review-placeholder">
            <h3>Đánh giá sản phẩm</h3>
            <p class="review-empty">Chưa có đánh giá nào.</p>
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
.gallery-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.gallery-thumb {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  background: #f5f5f5;
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
.detail-review-placeholder {
  border-top: 1px solid var(--border);
  padding-top: 24px;
}
.detail-review-placeholder h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}
.review-empty {
  color: var(--text-light);
  font-size: 14px;
  padding: 20px 0;
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
