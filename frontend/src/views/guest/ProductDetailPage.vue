<script setup>
import { ref, computed, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import { formatDate, formatPrice } from '@/utils/format';
import { useToast } from '@/stores/toast';
import { reviewApi, storeApi } from '@/api';
import { createStoreConfigController } from '@/utils/deliveryClaims';
import { createReviewPageController } from '@/utils/reviewPage';
import { resolveProductDetailPricing } from '@/utils/productDetailPricing';
import ProductCard from '@/components/common/ProductCard.vue';

const toast = useToast();
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
const selectedModifiers = ref([]);
const loadError = ref('');
const modifierErrors = ref({});
const favoritePending = ref(false);
const estimatedDeliveryMinutes = ref(null);
const reviewPage = ref(1);
const reviewSize = 10;
const reviewLoading = ref(false);
const reviewInitialError = ref('');
const reviewRefreshError = ref('');
const reviewData = ref(null);
const reviewsTitle = ref(null);
const storeConfigController = createStoreConfigController({
  requestConfig: () => storeApi.getConfig(),
  applyEstimate: (value) => { estimatedDeliveryMinutes.value = value; },
});

const product = computed(() => productStore.currentProduct);
const selectedStock = computed(() => selectedVariant.value?.quantityAvailable == null ? null : Number(selectedVariant.value.quantityAvailable));
const selectedAvailable = computed(() => product.value?.inStock && product.value?.isAvailableNow !== false && selectedVariant.value?.status === 'AVAILABLE' && (selectedStock.value === null || selectedStock.value > 0));
const modifierPrice = computed(() => selectedModifiers.value.reduce((sum, option) => sum + Number(option.price || 0), 0));
const pricing = computed(() => resolveProductDetailPricing(product.value, selectedVariant.value));
const effectivePrice = computed(() => pricing.value.currentPrice + modifierPrice.value);
const oldPrice = computed(() => pricing.value.crossedPrice ? pricing.value.crossedPrice + modifierPrice.value : null);
const galleryImages = computed(() => {
  if (!product.value) return [];
  return [product.value.image, ...(product.value.galleryImages || [])].filter(Boolean);
});
const reviewAverage = computed(() => Number(reviewData.value?.averageRating ?? product.value?.averageRating ?? 0));
const reviewCount = computed(() => Number(reviewData.value?.reviewCount ?? product.value?.reviewCount ?? 0));
const relatedProducts = computed(() => (productStore.allProducts || [])
  .filter((candidate) => candidate.productId !== product.value?.productId && candidate.categoryId === product.value?.categoryId)
  .slice(0, 4));
const reviewController = createReviewPageController({
  requestPage: (productId, params) => reviewApi.getByProduct(productId, params),
  applyState: (state) => {
    reviewPage.value = state.page;
    reviewLoading.value = state.loading;
    reviewInitialError.value = state.initialError;
    reviewRefreshError.value = state.refreshError;
    reviewData.value = state.data;
  },
});

function loadReviews(productId, options) {
  return reviewController.load(productId, options);
}

function changeReviewPage(page) {
  reviewController.goToPage(page, route.params.id);
}

function retryReviews() {
  reviewController.retry();
}

function scrollToReviews() {
  reviewsTitle.value?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
  reviewsTitle.value?.focus({ preventScroll: true });
}

async function loadProduct(id) {
  loading.value = true;
  loadError.value = '';
  selectedVariant.value = null;
  selectedModifiers.value = [];
  activeImageIndex.value = 0;
  quantity.value = 1;
  loadReviews(id, { reset: true });
  storeConfigController.load();
  try {
    if (!productStore.fetched) await productStore.init();
    await productStore.fetchById(id);
    selectedVariant.value = product.value?.variants?.find((variant) => {
      const stock = variant.quantityAvailable == null ? null : Number(variant.quantityAvailable);
      return variant.status === 'AVAILABLE' && (stock === null || stock > 0);
    }) || null;
    if (auth.isLoggedIn && product.value?.productId) await favoriteStore.check(product.value.productId);
  } catch (error) {
    loadError.value = error.message || 'Không thể tải sản phẩm';
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.id, loadProduct, { immediate: true });
onUnmounted(() => {
  reviewController.stop();
  storeConfigController.stop();
});

function selectVariant(variant) {
  const stock = variant.quantityAvailable == null ? null : Number(variant.quantityAvailable);
  if (variant.status !== 'AVAILABLE' || (stock !== null && stock <= 0)) return;
  selectedVariant.value = variant;
  if (stock !== null) quantity.value = Math.min(quantity.value, stock || 1);
}

function toggleModifier(group, option) {
  const selected = selectedModifiers.value.filter((item) => item.modifierGroupId === group.modifierGroupId);
  if (selected.some((item) => item.modifierOptionId === option.modifierOptionId)) {
    selectedModifiers.value = selectedModifiers.value.filter((item) => item.modifierOptionId !== option.modifierOptionId);
  } else if (selected.length < group.maxSelections) {
    selectedModifiers.value.push({ ...option, modifierGroupId: group.modifierGroupId, groupName: group.name });
  }
}

function modifiersValid() {
  const errors = {};
  for (const group of product.value?.modifierGroups || []) {
    const count = selectedModifiers.value.filter((item) => item.modifierGroupId === group.modifierGroupId).length;
    if (count < group.minSelections) errors[group.modifierGroupId] = `Vui lòng chọn ít nhất ${group.minSelections}`;
    if (count > group.maxSelections) errors[group.modifierGroupId] = `Chỉ được chọn tối đa ${group.maxSelections}`;
  }
  modifierErrors.value = errors;
  return Object.keys(errors).length === 0;
}

async function toggleFavorite() {
  if (favoritePending.value) return;
  if (!auth.isLoggedIn) return router.push({ name: 'Login' });
  favoritePending.value = true;
  try {
    await favoriteStore.toggle(product.value);
  } catch (error) {
    toast.error(error.message || 'Không thể cập nhật yêu thích');
  } finally {
    favoritePending.value = false;
  }
}

async function placeInCart(destination) {
  if (!selectedAvailable.value || !modifiersValid()) return;
  if (selectedStock.value !== null && quantity.value > selectedStock.value) {
    quantity.value = selectedStock.value;
    return;
  }
  try {
    await cart.addItem(product.value.productId, selectedVariant.value.variantId, quantity.value, selectedModifiers.value);
    router.push(destination);
  } catch (error) {
    toast.error(error.message || 'Không thể thêm vào giỏ');
  }
}
</script>

<template>
  <div v-if="loading" class="container loading-page">
    <div class="empty-state"><i class="fa-solid fa-rotate spin" aria-hidden="true"></i><h3>Đang tải sản phẩm...</h3></div>
  </div>

  <div v-else-if="loadError" class="container loading-page" role="alert">
    <div class="empty-state"><i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i><h3>{{ loadError }}</h3><button class="btn btn-primary" @click="loadProduct(route.params.id)">Thử lại</button></div>
  </div>

  <div v-else-if="product" class="product-page">
    <div class="container">
      <div class="product-breadcrumb">
        <router-link to="/home">Trang chủ</router-link><i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
        <router-link to="/menu">Thực đơn</router-link><i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
        <span>{{ product.categoryName || 'Món ăn' }}</span><i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
        <strong>{{ product.name }}</strong>
      </div>

      <div class="product-detail-layout">
        <section class="product-gallery">
          <div class="main-image-wrap">
            <img :src="galleryImages[activeImageIndex] || product.image" :alt="product.name" class="main-image" />
            <div v-if="pricing.discountPercent" class="image-sale">-{{ pricing.discountPercent }}%</div>
          </div>
          <div v-if="galleryImages.length > 1" class="gallery-thumbs">
            <button
              v-for="(image, index) in galleryImages"
              :key="image + index"
              class="gallery-thumb"
              :class="{ active: activeImageIndex === index }"
              :aria-label="`Xem ảnh ${index + 1} của ${product.name}`"
              :aria-pressed="activeImageIndex === index"
              @click="activeImageIndex = index"
            ><img :src="image" :alt="`Ảnh ${index + 1}`" /></button>
          </div>
        </section>

        <section class="product-purchase-panel">
          <div class="detail-topline">
            <div class="product-meta">
              <span>{{ product.categoryName || 'Món ăn' }}</span>
              <span v-if="product.bestSeller"><i class="fa-solid fa-bolt" aria-hidden="true"></i>Bán chạy</span>
            </div>
            <button class="favorite-detail-btn" type="button" :disabled="favoritePending" :aria-pressed="favoriteStore.isFavorite(product.productId)" :aria-busy="favoritePending" :aria-label="favoriteStore.isFavorite(product.productId) ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'" @click="toggleFavorite">
              <i :class="favoriteStore.isFavorite(product.productId) ? 'fa-solid fa-heart' : 'fa-regular fa-heart'" aria-hidden="true"></i>
              {{ favoriteStore.isFavorite(product.productId) ? 'Đã thích' : 'Yêu thích' }}
            </button>
          </div>
          <h1>{{ product.name }}</h1>
          <a class="detail-rating" href="#reviews-title" :aria-label="reviewCount ? `Đánh giá ${reviewAverage.toFixed(1)} trên 5 từ ${reviewCount} lượt. Xem đánh giá` : 'Chưa có đánh giá. Xem đánh giá'" @click.prevent="scrollToReviews">
            <i class="fa-solid fa-star" aria-hidden="true"></i>
            <strong>{{ reviewAverage.toFixed(1) }}</strong>
            <span>{{ reviewCount ? `${reviewCount} đánh giá` : 'Chưa có đánh giá' }}</span>
          </a>

          <p class="product-description">{{ product.description || 'Món ngon được chuẩn bị nóng hổi từ bếp FastGuy.' }}</p>

          <div class="price-row">
            <strong>{{ formatPrice(effectivePrice) }}</strong>
            <del v-if="oldPrice">{{ formatPrice(oldPrice) }}</del>
          </div>

          <fieldset v-if="product.variants?.length" class="selection-group">
            <legend class="selection-title"><span>Kích cỡ</span><small>Chọn một</small></legend>
            <div class="variant-grid">
              <button
                v-for="variant in product.variants"
                :key="variant.variantId"
                class="variant-option"
                :class="{ active: selectedVariant?.variantId === variant.variantId, disabled: product.isAvailableNow === false || variant.status !== 'AVAILABLE' || (variant.quantityAvailable != null && Number(variant.quantityAvailable) <= 0) }"
                :aria-pressed="selectedVariant?.variantId === variant.variantId"
                :disabled="product.isAvailableNow === false || variant.status !== 'AVAILABLE' || (variant.quantityAvailable != null && Number(variant.quantityAvailable) <= 0)"
                @click="selectVariant(variant)"
              >
                <strong>{{ variant.variantName }}</strong>
                <span>{{ formatPrice(variant.price) }}</span>
                <small>{{ variant.quantityAvailable == null ? 'Còn hàng' : Number(variant.quantityAvailable) > 0 ? `Còn ${variant.quantityAvailable}` : 'Hết hàng' }}</small>
              </button>
            </div>
          </fieldset>

          <fieldset v-for="group in product.modifierGroups" :key="group.modifierGroupId" class="selection-group">
            <legend class="selection-title"><span>{{ group.name }}</span><small>{{ group.minSelections ? `Chọn ${group.minSelections}-${group.maxSelections}` : `Tối đa ${group.maxSelections}` }}</small></legend>
            <div class="variant-grid">
              <button v-for="option in group.options" :key="option.modifierOptionId" class="variant-option" :class="{ active: selectedModifiers.some(item => item.modifierOptionId === option.modifierOptionId) }" :aria-pressed="selectedModifiers.some(item => item.modifierOptionId === option.modifierOptionId)" :aria-invalid="Boolean(modifierErrors[group.modifierGroupId])" :aria-describedby="`modifier-help-${group.modifierGroupId}`" @click="toggleModifier(group, option)">
                <strong>{{ option.name }}</strong><span>+{{ formatPrice(option.price) }}</span>
              </button>
            </div>
            <small :id="`modifier-help-${group.modifierGroupId}`" :class="{ 'modifier-error': modifierErrors[group.modifierGroupId] }">{{ modifierErrors[group.modifierGroupId] || `${group.minSelections ? 'Bắt buộc. ' : ''}Tối đa ${group.maxSelections} lựa chọn` }}</small>
          </fieldset>
          <div class="availability" :class="{ unavailable: !selectedAvailable }">
            <i :class="selectedAvailable ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-xmark'" aria-hidden="true"></i>
            {{ selectedAvailable ? selectedStock == null ? 'Còn hàng, sẵn sàng giao nóng' : `Còn ${selectedStock} phần` : product.isAvailableNow === false ? `Ngoài giờ bán${product.availableFrom || product.availableTo ? ` (${product.availableFrom || '00:00'} - ${product.availableTo || '24:00'})` : ''}` : 'Sản phẩm hiện đã hết hàng' }}
          </div>

          <div class="purchase-actions">
            <div class="purchase-row">
              <div class="quantity-control">
                <button aria-label="Giảm số lượng" :disabled="quantity <= 1" @click="quantity = Math.max(1, quantity - 1)"><i class="fa-solid fa-minus" aria-hidden="true"></i></button>
                <span aria-live="polite">{{ quantity }}</span>
                <button aria-label="Tăng số lượng" :disabled="selectedStock != null && quantity >= selectedStock" @click="quantity = selectedStock == null ? quantity + 1 : Math.min(selectedStock, quantity + 1)"><i class="fa-solid fa-plus" aria-hidden="true"></i></button>
              </div>
              <button class="add-cart-btn" :disabled="!selectedAvailable" @click="placeInCart('/cart')"><i class="fa-solid fa-cart-shopping" aria-hidden="true"></i>Thêm vào giỏ - {{ formatPrice(effectivePrice * quantity) }}</button>
            </div>
            <button class="buy-now-btn" :disabled="!selectedAvailable" @click="placeInCart('/checkout')">Mua ngay<i class="fa-solid fa-arrow-right" aria-hidden="true"></i></button>
          </div>

          <div class="delivery-grid">
            <div v-if="estimatedDeliveryMinutes">
              <i class="fa-solid fa-clock" aria-hidden="true"></i>
              <span><strong>Dự kiến {{ estimatedDeliveryMinutes }} phút</strong><small>Thời gian thực tế xác nhận khi tính giao hàng</small></span>
            </div>
            <div>
              <i class="fa-solid fa-truck-fast" aria-hidden="true"></i>
              <span><strong>Phí giao hàng theo địa chỉ</strong><small>Hiển thị chính xác tại bước thanh toán</small></span>
            </div>
          </div>
        </section>
      </div>

      <section class="product-reviews" aria-labelledby="reviews-title">
        <div class="review-heading">
          <div>
            <p class="review-eyebrow">Khách hàng nói gì</p>
            <h2 id="reviews-title" ref="reviewsTitle" tabindex="-1">Đánh giá sản phẩm</h2>
          </div>
          <p class="review-rating" :aria-label="reviewCount ? `Đánh giá trung bình ${reviewAverage.toFixed(1)} trên 5 từ ${reviewCount} lượt` : 'Chưa có đánh giá'">
            <i class="fa-solid fa-star" aria-hidden="true"></i><strong>{{ reviewAverage.toFixed(1) }}</strong><small>/5 · {{ reviewCount }} lượt</small>
          </p>
        </div>

        <div v-if="reviewLoading && !reviewData" class="review-state" role="status" aria-live="polite"><i class="fa-solid fa-rotate spin" aria-hidden="true"></i> Đang tải đánh giá...</div>
        <div v-else-if="reviewInitialError && !reviewData" class="review-state review-error" role="alert">
          <span>{{ reviewInitialError }}</span>
          <button type="button" class="btn btn-primary" @click="retryReviews">Thử lại</button>
        </div>
        <div v-else-if="reviewData">
          <div v-if="reviewRefreshError" class="review-error-banner" role="status" aria-live="polite">
            <span>{{ reviewRefreshError }}</span>
            <button type="button" @click="retryReviews">Thử lại</button>
          </div>
          <div v-if="reviewLoading" class="review-loading-banner" role="status" aria-live="polite"><i class="fa-solid fa-rotate spin" aria-hidden="true"></i> Đang cập nhật đánh giá...</div>
          <div v-if="reviewData.total === 0" class="review-state"><i class="fa-regular fa-comment-dots" aria-hidden="true"></i><strong>Chưa có đánh giá</strong><span>Hãy là người đầu tiên chia sẻ trải nghiệm.</span></div>
          <div v-else class="review-content">
          <div class="rating-distribution" aria-label="Phân phối đánh giá">
            <div v-for="rating in [5, 4, 3, 2, 1]" :key="rating" class="distribution-row">
              <span>{{ rating }} sao</span>
              <div class="distribution-track" role="progressbar" :aria-label="`${rating} sao: ${reviewData.ratingDistribution[rating]} lượt`" :aria-valuenow="reviewData.ratingDistribution[rating]" aria-valuemin="0" :aria-valuemax="reviewData.reviewCount">
                <span :style="{ width: `${reviewData.reviewCount ? reviewData.ratingDistribution[rating] * 100 / reviewData.reviewCount : 0}%` }"></span>
              </div>
              <strong>{{ reviewData.ratingDistribution[rating] }}</strong>
            </div>
          </div>

          <div class="review-list">
            <article v-for="review in reviewData.items" :key="review.reviewId" class="review-item">
              <header><strong>{{ review.userName }}</strong><time :datetime="review.createdAt">{{ formatDate(review.createdAt) }}</time></header>
              <p class="review-stars" :aria-label="`${review.rating} trên 5 sao`"><i v-for="star in 5" :key="star" :class="star <= review.rating ? 'fa-solid fa-star' : 'fa-regular fa-star'" aria-hidden="true"></i></p>
              <p v-if="review.comment" class="review-comment">{{ review.comment }}</p>
            </article>
          </div>

          <nav class="review-pagination" aria-label="Phân trang đánh giá">
            <button type="button" :disabled="reviewPage === 1" @click="changeReviewPage(reviewPage - 1)"><i class="fa-solid fa-chevron-left" aria-hidden="true"></i>Trang trước</button>
            <span>Trang {{ reviewPage }} / {{ Math.max(1, Math.ceil(reviewData.total / reviewSize)) }}</span>
            <button type="button" :disabled="reviewPage * reviewSize >= reviewData.total" @click="changeReviewPage(reviewPage + 1)">Trang sau<i class="fa-solid fa-chevron-right" aria-hidden="true"></i></button>
          </nav>
          </div>
        </div>
      </section>

      <section v-if="relatedProducts.length" class="related-section" aria-labelledby="related-title">
        <div class="related-heading">
          <div><p class="review-eyebrow">Có thể bạn sẽ thích</p><h2 id="related-title">Món cùng danh mục</h2></div>
          <router-link to="/menu">Xem thực đơn<i class="fa-solid fa-arrow-right" aria-hidden="true"></i></router-link>
        </div>
        <div class="related-products">
          <ProductCard v-for="related in relatedProducts" :key="related.productId" :product="related" />
        </div>
      </section>
    </div>
  </div>

  <div v-else class="container loading-page">
    <div class="empty-state"><i class="fa-solid fa-box-open" aria-hidden="true"></i><h3>Không tìm thấy sản phẩm</h3><router-link to="/menu" class="btn btn-primary">Quay lại thực đơn</router-link></div>
  </div>
</template>

<style scoped>
.loading-page { padding: 60px 0; }
.product-page { padding: 26px 0 64px; background: #fff8f0; min-height: 100vh; }
.product-breadcrumb { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; color: var(--text-mid); font-size: 13px; margin-bottom: 24px; }
.product-breadcrumb a { color: var(--text-dark); font-weight: 600; }
.product-breadcrumb i { color: var(--text-light); font-size: 10px; }
.product-breadcrumb strong { color: var(--primary-dark); }
.product-detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(400px, 0.95fr); gap: clamp(28px, 5vw, 56px); align-items: start; }
.product-gallery { position: sticky; top: 82px; min-width: 0; }
.main-image-wrap { position: relative; border-radius: var(--radius-xl); overflow: hidden; aspect-ratio: 1.12; background: var(--surface); box-shadow: var(--shadow-md); }
.main-image { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition-slow); }
.main-image-wrap:hover .main-image { transform: scale(1.05); }
.image-sale { display: inline-flex; align-items: center; gap: 4px; position: absolute; left: 18px; top: 18px; padding: 6px 12px; border-radius: var(--radius-full); color: #fff; background: var(--primary-dark); font-size: 11px; font-weight: 800; letter-spacing: .05em; }
.gallery-thumbs { display: flex; gap: 12px; margin-top: 14px; overflow-x: auto; padding-bottom: 3px; }
.gallery-thumb { flex: 0 0 82px; height: 72px; border: 2px solid transparent; border-radius: var(--radius-sm); overflow: hidden; background: #fff; padding: 0; }
.gallery-thumb.active { border-color: var(--primary-dark); }
.gallery-thumb img { width: 100%; height: 100%; object-fit: cover; }
.product-purchase-panel { min-width: 0; background: rgba(255,255,255,.82); border: 1px solid rgba(232,115,74,.14); border-radius: var(--radius-xl); padding: clamp(22px, 3vw, 32px); box-shadow: var(--shadow-sm); }
.detail-topline { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.product-meta,.detail-rating{display:flex;align-items:center;flex-wrap:wrap}
.product-meta{gap:8px;color:var(--primary-dark);font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:.06em}.product-meta span{display:inline-flex;align-items:center;gap:5px}.product-meta span+span{padding:5px 9px;border-radius:999px;color:#fff;background:var(--primary-dark)}
.favorite-detail-btn { display:inline-flex;min-height:44px;align-items:center;gap:7px;padding:0 10px;color:var(--text-mid);font-size:13px;font-weight:600; }
.favorite-detail-btn i { color:var(--red-active);font-size:16px; }
.favorite-detail-btn:disabled{cursor:wait;opacity:.7}
.product-purchase-panel h1 { font-size: clamp(30px, 4vw, 48px); line-height: 1.03; letter-spacing: -.055em; margin: 14px 0 10px; }
.detail-rating{width:max-content;min-height:44px;gap:7px;color:var(--text-mid);font-size:13px}.detail-rating i{color:#f59e0b}.detail-rating strong{color:var(--text-dark);font-size:16px}.detail-rating:focus-visible,#reviews-title:focus-visible{outline:3px solid var(--primary);outline-offset:3px}
.product-description { color: var(--text-mid); font-size: 15px; line-height: 1.7; margin: 18px 0; }
.price-row { display: flex; align-items: baseline; gap: 14px; margin: 10px 0 24px; }
.price-row strong { color: var(--primary-dark); font-size: clamp(32px, 4vw, 48px); letter-spacing: -.05em; }
.price-row del { color: var(--text-light); font-size: 16px; }
.selection-group { min-width:0; border:0; border-top: 1px solid var(--border-light); padding:18px 0 0; margin:18px 0 0; }
.selection-title { display: flex; width:100%; justify-content: space-between; margin-bottom: 10px; padding:0; font-size: 14px; font-weight: 800; }
.selection-title small { color: var(--text-mid); font-weight: 500; }
.variant-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 9px; }
.variant-option { display: grid; min-height:44px; gap: 2px; text-align: left; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; transition: all var(--transition-fast); }
.variant-option:hover { border-color: var(--primary); }
.variant-option.active { background: var(--primary-light); color: var(--primary-dark); border: 2px solid var(--primary-dark); }
.variant-option.disabled { opacity: .45; cursor: not-allowed; }
.variant-option strong { font-size: 14px; }.variant-option span { font-size: 13px; font-weight: 700; }.variant-option small { font-size: 11px; color: var(--text-mid); }
.future-addons { opacity: .78; }.addon-row { display: flex; justify-content: space-between; padding: 10px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); margin-top: 8px; font-size: 13px; }.addon-row span { color: var(--text-mid); }.addon-row strong { color: var(--primary-dark); }
.availability { display: flex; align-items: center; gap: 7px; margin: 20px 0; color: #15803d; font-size: 13px; font-weight: 700; }.availability.unavailable { color: var(--red-active); }
.purchase-actions{display:grid;gap:12px}.purchase-row { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 12px; }.quantity-control { display: flex; align-items: center; gap: 6px; padding: 4px; border: 1px solid var(--border); border-radius: var(--radius-full); background: #fff; }.quantity-control button { width:44px;height:44px;min-height:44px;border-radius:50%;color:var(--primary-dark); }.quantity-control button:hover { background: var(--primary-light); }.quantity-control button:disabled { opacity: .35; }.quantity-control span { min-width: 30px; text-align: center; font-weight: 800; }
.add-cart-btn,.buy-now-btn{display:inline-flex;min-height:44px;align-items:center;justify-content:center;gap:8px;border-radius:var(--radius-full);font-size:14px;font-weight:800;transition:all var(--transition-fast)}.add-cart-btn{color:#fff;background:linear-gradient(135deg,var(--primary-dark),var(--route-orange));box-shadow:0 12px 24px rgba(212,97,58,.22)}.add-cart-btn:hover{transform:translateY(-1px)}.add-cart-btn:disabled,.buy-now-btn:disabled{cursor:not-allowed;opacity:.45}.buy-now-btn{width:100%;border:2px solid var(--primary-dark);color:var(--primary-dark);background:transparent}.buy-now-btn:hover{background:var(--primary-light)}
.delivery-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;margin-top:24px}.delivery-grid>div{display:flex;align-items:center;gap:10px;padding:14px;border-radius:var(--radius);background:var(--primary-light)}.delivery-grid i{color:var(--primary-dark);font-size:20px}.delivery-grid strong,.delivery-grid small{display:block}.delivery-grid strong{font-size:12px}.delivery-grid small{color:var(--text-mid);font-size:11px}
.product-reviews { margin-top: 48px; padding: clamp(22px, 4vw, 36px); border: 1px solid rgba(232,115,74,.14); border-radius: var(--radius-xl); background: #fff; box-shadow: var(--shadow-sm); }
.review-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 24px; }.review-eyebrow { margin: 0 0 4px; color: var(--primary-dark); font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: .08em; }.review-heading h2 { margin: 0; font-size: clamp(24px, 3vw, 34px); }.review-rating { display: flex; align-items: baseline; gap: 6px; margin: 0; white-space: nowrap; }.review-rating>i,.review-stars { color: #f59e0b; }.review-rating strong { font-size: 28px; }.review-rating small { color: var(--text-mid); }
.review-state { min-height: 150px; display: flex; align-items: center; justify-content: center; gap: 9px; flex-direction: column; color: var(--text-mid); text-align: center; }.review-error .btn { margin-top: 8px; min-height: 44px; }.review-error-banner, .review-loading-banner { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; padding: 10px 14px; border-radius: var(--radius); background: #fff7ed; color: #9a3412; }.review-error-banner button { min-height: 44px; padding: 6px 14px; border: 1px solid currentColor; border-radius: var(--radius-full); font-weight: 700; }.review-error-banner button:focus-visible { outline: 3px solid var(--primary); outline-offset: 2px; }.review-loading-banner { justify-content: flex-start; color: var(--text-mid); background: var(--primary-light); }.review-content { display: grid; grid-template-columns: minmax(220px, .7fr) minmax(0, 1.5fr); gap: clamp(24px, 5vw, 54px); }.rating-distribution { display: grid; align-content: start; gap: 12px; }.distribution-row { display: grid; grid-template-columns: 48px minmax(80px, 1fr) 28px; align-items: center; gap: 9px; font-size: 13px; }.distribution-row strong { text-align: end; }.distribution-track { height: 9px; overflow: hidden; border-radius: var(--radius-full); background: var(--border-light); }.distribution-track > span { display: block; height: 100%; border-radius: inherit; background: #f59e0b; }
.review-list { display: grid; gap: 12px; min-width: 0; }.review-item { padding: 16px 0; border-bottom: 1px solid var(--border-light); }.review-item:first-child { padding-top: 0; }.review-item header { display: flex; justify-content: space-between; gap: 12px; }.review-item time { color: var(--text-mid); font-size: 12px; }.review-stars{display:flex;gap:3px;margin:6px 0;color:#f59e0b}.review-comment { margin: 0; color: var(--text-mid); line-height: 1.6; overflow-wrap: anywhere; }.review-pagination { grid-column: 1 / -1; display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 10px; }.review-pagination button { display:inline-flex;min-height:44px;align-items:center;gap:7px;padding: 8px 14px; border: 1px solid var(--border); border-radius: var(--radius-full); color: var(--primary-dark); font-weight: 700; }.review-pagination button:hover:not(:disabled) { background: var(--primary-light); }.review-pagination button:disabled { opacity: .4; cursor: not-allowed; }.review-pagination button:focus-visible { outline: 3px solid var(--primary); outline-offset: 2px; }
.related-section{margin-top:48px}.related-heading{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:20px}.related-heading h2{margin:0;font-size:clamp(24px,3vw,34px)}.related-heading a{display:inline-flex;min-height:44px;align-items:center;gap:8px;color:var(--primary-dark);font-weight:800}.related-heading a:focus-visible{outline:3px solid var(--primary);outline-offset:2px}.related-products{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:18px}
.product-purchase-panel button:focus-visible,.gallery-thumb:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
@media (max-width: 1100px) { .related-products{grid-template-columns:repeat(3,minmax(0,1fr))} }
@media (max-width: 900px) { .product-detail-layout { grid-template-columns: 1fr; }.product-gallery { position: static; }.main-image-wrap { aspect-ratio: 1.3; }.review-content { grid-template-columns: 1fr; } }
@media (max-width: 800px) { .related-products{grid-template-columns:repeat(2,minmax(0,1fr))} }
@media (max-width: 480px) { .product-page { padding-top: 18px; }.purchase-row,.delivery-grid { grid-template-columns: 1fr; }.quantity-control { justify-content: center; }.product-purchase-panel { padding:20px 16px;border-radius:var(--radius-lg); }.product-purchase-panel h1 { font-size: 32px; }.review-summary, .review-heading { align-items: flex-start; flex-direction: column; }.product-reviews { margin-top: 28px; padding: 20px 16px; border-radius: var(--radius-lg); }.review-pagination { flex-wrap: wrap; gap: 8px; }.review-pagination span { order: -1; flex-basis: 100%; text-align: center; }.review-pagination button { flex: 1; min-width: 120px; }.related-heading{align-items:flex-start;flex-direction:column}.related-products{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px} }
@media (max-width: 360px) { .related-products{grid-template-columns:1fr} }
@media (prefers-reduced-motion: reduce){html{scroll-behavior:auto}.main-image,.variant-option,.add-cart-btn,.buy-now-btn{transition:none}.main-image-wrap:hover .main-image,.add-cart-btn:hover{transform:none}.spin{animation:none}}
</style>
