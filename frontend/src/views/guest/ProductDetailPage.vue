<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import { formatPrice } from '@/utils/format';
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
const selectedModifiers = ref([]);

const product = computed(() => productStore.currentProduct);
const selectedStock = computed(() => selectedVariant.value?.quantityAvailable == null ? null : Number(selectedVariant.value.quantityAvailable));
const selectedAvailable = computed(() => product.value?.inStock && product.value?.isAvailableNow !== false && selectedVariant.value?.status === 'AVAILABLE' && (selectedStock.value === null || selectedStock.value > 0));
const modifierPrice = computed(() => selectedModifiers.value.reduce((sum, option) => sum + Number(option.price || 0), 0));
const effectivePrice = computed(() => Number(selectedVariant.value?.price || product.value?.price || 0) + modifierPrice.value);
const galleryImages = computed(() => {
  if (!product.value) return [];
  return [product.value.image, ...(product.value.galleryImages || [])].filter(Boolean);
});
const oldPrice = computed(() => product.value?.discountPrice ? product.value.price : null);

onMounted(async () => {
  loading.value = true;
  try {
    if (!productStore.fetched) await productStore.init();
    await productStore.fetchById(route.params.id);
    if (product.value?.variants?.length) {
      selectedVariant.value = product.value.variants.find((variant) => variant.isDefault) || product.value.variants[0];
    }
    if (auth.isLoggedIn && product.value?.productId) await favoriteStore.check(product.value.productId);
  } finally {
    loading.value = false;
  }
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
  return (product.value?.modifierGroups || []).every((group) => {
    const count = selectedModifiers.value.filter((item) => item.modifierGroupId === group.modifierGroupId).length;
    return count >= group.minSelections && count <= group.maxSelections;
  });
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) return router.push('/login');
  await favoriteStore.toggle(product.value);
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
    alert(error.message || 'Không thể thêm vào giỏ');
  }
}
</script>

<template>
  <div v-if="loading" class="container loading-page">
    <div class="empty-state"><i class="bi bi-arrow-repeat spin"></i><h3>Đang tải sản phẩm...</h3></div>
  </div>

  <div v-else-if="product" class="product-page">
    <div class="container">
      <div class="product-breadcrumb">
        <router-link to="/">Trang chủ</router-link><i class="bi bi-chevron-right"></i>
        <router-link to="/menu">Thực đơn</router-link><i class="bi bi-chevron-right"></i>
        <span>{{ product.categoryName || 'Món ăn' }}</span><i class="bi bi-chevron-right"></i>
        <strong>{{ product.name }}</strong>
      </div>

      <div class="product-detail-layout">
        <section class="product-gallery">
          <div class="main-image-wrap">
            <img :src="galleryImages[activeImageIndex] || product.image" :alt="product.name" class="main-image" />
            <div v-if="product.discountPrice" class="image-sale">GIẢM GIÁ</div>
          </div>
          <div v-if="galleryImages.length > 1" class="gallery-thumbs">
            <button
              v-for="(image, index) in galleryImages"
              :key="image + index"
              class="gallery-thumb"
              :class="{ active: activeImageIndex === index }"
              @click="activeImageIndex = index"
            ><img :src="image" :alt="`Ảnh ${index + 1}`" /></button>
          </div>
        </section>

        <section class="product-purchase-panel">
          <div class="detail-topline">
            <span class="hot-label"><i class="bi bi-lightning-charge-fill"></i> HOT</span>
            <button class="favorite-detail-btn" type="button" @click="toggleFavorite">
              <i :class="favoriteStore.isFavorite(product.productId) ? 'bi bi-heart-fill' : 'bi bi-heart'"></i>
              {{ favoriteStore.isFavorite(product.productId) ? 'Đã thích' : 'Yêu thích' }}
            </button>
          </div>
          <h1>{{ product.name }}</h1>
          <div class="rating-row">
            <StarRating :modelValue="product.rating || 5" readonly :size="18" />
            <span>{{ product.reviewCount || 0 }} đánh giá</span>
          </div>
          <p class="product-description">{{ product.description || 'Món ngon được chuẩn bị nóng hổi từ bếp FastGuy.' }}</p>

          <div class="price-row">
            <strong>{{ formatPrice(effectivePrice) }}</strong>
            <del v-if="oldPrice">{{ formatPrice(oldPrice) }}</del>
          </div>

          <div v-if="product.variants?.length" class="selection-group">
            <div class="selection-title"><span>Phân loại</span><small>Chọn một</small></div>
            <div class="variant-grid">
              <button
                v-for="variant in product.variants"
                :key="variant.variantId"
                class="variant-option"
:class="{ active: selectedVariant?.variantId === variant.variantId, disabled: product.isAvailableNow === false || variant.status !== 'AVAILABLE' || (variant.quantityAvailable != null && Number(variant.quantityAvailable) <= 0) }"
                 :disabled="product.isAvailableNow === false || variant.status !== 'AVAILABLE' || (variant.quantityAvailable != null && Number(variant.quantityAvailable) <= 0)"
                @click="selectVariant(variant)"
              >
                <strong>{{ variant.variantName }}</strong>
                <span>{{ formatPrice(variant.price) }}</span>
                <small>{{ variant.quantityAvailable == null ? 'Còn hàng' : Number(variant.quantityAvailable) > 0 ? `Còn ${variant.quantityAvailable}` : 'Hết hàng' }}</small>
              </button>
            </div>
          </div>

          <div v-for="group in product.modifierGroups" :key="group.modifierGroupId" class="selection-group">
            <div class="selection-title"><span>{{ group.name }}</span><small>{{ group.minSelections ? `Chọn ${group.minSelections}-${group.maxSelections}` : `Tối đa ${group.maxSelections}` }}</small></div>
            <div class="variant-grid">
              <button v-for="option in group.options" :key="option.modifierOptionId" class="variant-option" :class="{ active: selectedModifiers.some(item => item.modifierOptionId === option.modifierOptionId) }" @click="toggleModifier(group, option)">
                <strong>{{ option.name }}</strong><span>+{{ formatPrice(option.price) }}</span>
              </button>
            </div>
          </div>
          <div v-if="product.combo" class="selection-group">
            <div class="selection-title"><span>Combo gồm</span><small>Cố định</small></div>
            <div v-for="item in product.combo.items" :key="item.comboItemId" class="addon-row"><span>{{ item.productName }} - {{ item.variantName }}</span><strong>x{{ item.quantity }}</strong></div>
          </div>

          <div class="availability" :class="{ unavailable: !selectedAvailable }">
            <i :class="selectedAvailable ? 'bi bi-check-circle-fill' : 'bi bi-x-circle-fill'"></i>
            {{ selectedAvailable ? selectedStock == null ? 'Còn hàng, sẵn sàng giao nóng' : `Còn ${selectedStock} phần` : product.isAvailableNow === false ? `Ngoài giờ bán${product.availableFrom || product.availableTo ? ` (${product.availableFrom || '00:00'} - ${product.availableTo || '24:00'})` : ''}` : 'Sản phẩm hiện đã hết hàng' }}
          </div>

          <div class="purchase-row">
            <div class="quantity-control">
              <button @click="quantity = Math.max(1, quantity - 1)"><i class="bi bi-dash"></i></button>
              <span>{{ quantity }}</span>
              <button :disabled="selectedStock != null && quantity >= selectedStock" @click="quantity = selectedStock == null ? quantity + 1 : Math.min(selectedStock, quantity + 1)"><i class="bi bi-plus"></i></button>
            </div>
            <button class="add-cart-btn" :disabled="!selectedAvailable" @click="placeInCart('/cart')">Thêm vào giỏ - {{ formatPrice(effectivePrice * quantity) }}</button>
          </div>
          <button class="buy-now-btn" :disabled="!selectedAvailable" @click="placeInCart('/checkout')">Mua ngay <i class="bi bi-arrow-right"></i></button>

          <div class="benefit-grid">
            <div><i class="bi bi-clock-history"></i><span><strong>Giao 30 phút</strong><small>Nhanh và nóng hổi</small></span></div>
            <div><i class="bi bi-truck"></i><span><strong>Miễn phí ship</strong><small>Cho đơn từ 50k</small></span></div>
          </div>
        </section>
      </div>
    </div>
  </div>

  <div v-else class="container loading-page">
    <div class="empty-state"><i class="bi bi-box"></i><h3>Không tìm thấy sản phẩm</h3><router-link to="/menu" class="btn btn-primary">Quay lại thực đơn</router-link></div>
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
.product-gallery { position: sticky; top: 82px; }
.main-image-wrap { position: relative; border-radius: var(--radius-xl); overflow: hidden; aspect-ratio: 1.12; background: var(--surface); box-shadow: var(--shadow-md); }
.main-image { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition-slow); }
.main-image-wrap:hover .main-image { transform: scale(1.05); }
.image-sale, .hot-label { display: inline-flex; align-items: center; gap: 4px; color: #fff; background: var(--primary-dark); border-radius: var(--radius-full); font-weight: 800; }
.image-sale { position: absolute; left: 18px; top: 18px; padding: 6px 12px; font-size: 11px; letter-spacing: .05em; }
.gallery-thumbs { display: flex; gap: 12px; margin-top: 14px; overflow-x: auto; padding-bottom: 3px; }
.gallery-thumb { flex: 0 0 82px; height: 72px; border: 2px solid transparent; border-radius: var(--radius-sm); overflow: hidden; background: #fff; padding: 0; }
.gallery-thumb.active { border-color: var(--primary-dark); }
.gallery-thumb img { width: 100%; height: 100%; object-fit: cover; }
.product-purchase-panel { background: rgba(255,255,255,.82); border: 1px solid rgba(232,115,74,.14); border-radius: var(--radius-xl); padding: clamp(22px, 3vw, 32px); box-shadow: var(--shadow-sm); }
.detail-topline { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.hot-label { padding: 5px 12px; font-size: 12px; }
.favorite-detail-btn { color: var(--text-mid); font-size: 13px; font-weight: 600; }
.favorite-detail-btn i { color: var(--red-active); font-size: 16px; }
.product-purchase-panel h1 { font-size: clamp(30px, 4vw, 48px); line-height: 1.03; letter-spacing: -.055em; margin: 14px 0 10px; }
.rating-row { display: flex; gap: 10px; align-items: center; color: var(--text-mid); font-size: 13px; }
.product-description { color: var(--text-mid); font-size: 15px; line-height: 1.7; margin: 18px 0; }
.price-row { display: flex; align-items: baseline; gap: 14px; margin: 10px 0 24px; }
.price-row strong { color: var(--primary-dark); font-size: clamp(32px, 4vw, 48px); letter-spacing: -.05em; }
.price-row del { color: var(--text-light); font-size: 16px; }
.selection-group { border-top: 1px solid var(--border-light); padding-top: 18px; margin-top: 18px; }
.selection-title { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; font-weight: 800; }
.selection-title small { color: var(--text-mid); font-weight: 500; }
.variant-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 9px; }
.variant-option { display: grid; gap: 2px; text-align: left; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; transition: all var(--transition-fast); }
.variant-option:hover { border-color: var(--primary); }
.variant-option.active { background: var(--primary-light); color: var(--primary-dark); border: 2px solid var(--primary-dark); }
.variant-option.disabled { opacity: .45; cursor: not-allowed; }
.variant-option strong { font-size: 14px; }.variant-option span { font-size: 13px; font-weight: 700; }.variant-option small { font-size: 11px; color: var(--text-mid); }
.future-addons { opacity: .78; }.addon-row { display: flex; justify-content: space-between; padding: 10px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); margin-top: 8px; font-size: 13px; }.addon-row span { color: var(--text-mid); }.addon-row strong { color: var(--primary-dark); }
.availability { display: flex; align-items: center; gap: 7px; margin: 20px 0; color: #15803d; font-size: 13px; font-weight: 700; }.availability.unavailable { color: var(--red-active); }
.purchase-row { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 12px; }.quantity-control { display: flex; align-items: center; gap: 6px; padding: 4px; border: 1px solid var(--border); border-radius: var(--radius-full); background: #fff; }.quantity-control button { width: 34px; height: 34px; border-radius: 50%; color: var(--primary-dark); }.quantity-control button:hover { background: var(--primary-light); }.quantity-control button:disabled { opacity: .35; }.quantity-control span { min-width: 30px; text-align: center; font-weight: 800; }
.add-cart-btn, .buy-now-btn { min-height: 46px; border-radius: var(--radius-full); font-size: 14px; font-weight: 800; transition: all var(--transition-fast); }.add-cart-btn { background: linear-gradient(135deg, var(--primary-dark), var(--route-orange)); color: #fff; box-shadow: 0 12px 24px rgba(212,97,58,.22); }.add-cart-btn:hover { transform: translateY(-1px); }.add-cart-btn:disabled, .buy-now-btn:disabled { opacity: .45; cursor: not-allowed; }.buy-now-btn { width: 100%; margin-top: 12px; color: var(--primary-dark); border: 2px solid var(--primary-dark); background: transparent; }.buy-now-btn:hover { background: var(--primary-light); }
.benefit-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 24px; }.benefit-grid > div { display: flex; align-items: center; gap: 10px; padding: 12px; border-radius: var(--radius); background: var(--primary-light); }.benefit-grid i { color: var(--primary-dark); font-size: 20px; }.benefit-grid strong, .benefit-grid small { display: block; }.benefit-grid strong { font-size: 12px; }.benefit-grid small { color: var(--text-mid); font-size: 11px; }
@media (max-width: 900px) { .product-detail-layout { grid-template-columns: 1fr; }.product-gallery { position: static; }.main-image-wrap { aspect-ratio: 1.3; } }
@media (max-width: 480px) { .product-page { padding-top: 18px; }.purchase-row { grid-template-columns: 1fr; }.quantity-control { justify-content: center; }.benefit-grid { grid-template-columns: 1fr; }.product-purchase-panel { border-radius: var(--radius-lg); }.product-purchase-panel h1 { font-size: 32px; } }
</style>
