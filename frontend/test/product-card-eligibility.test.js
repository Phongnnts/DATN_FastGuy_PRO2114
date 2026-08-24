import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { canDirectAddProduct } from '../src/utils/productCard.js';

const productCardSource = readFileSync(new URL('../src/components/common/ProductCard.vue', import.meta.url), 'utf8');

const product = (overrides = {}) => ({
  productId: 1,
  cardDataComplete: true,
  productType: 'SIMPLE',
  inStock: true,
  isAvailableNow: true,
  defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: null },
  variants: [{ variantId: 10, status: 'AVAILABLE', quantityAvailable: null }],
  modifierGroups: [],
  ...overrides,
});

test('direct add ignores dormant modifiers for a simple product with one available variant', () => {
  assert.equal(canDirectAddProduct(product()), true);
  assert.equal(canDirectAddProduct(product({ modifierGroups: [{ groupId: 1, minSelections: 1, options: [{ modifierOptionId: 1 }] }] })), true);
  assert.equal(canDirectAddProduct(product({ productType: 'COMBO' })), false);
  assert.equal(canDirectAddProduct(product({ variants: [product().defaultVariant, { variantId: 11, status: 'AVAILABLE', quantityAvailable: 5 }] })), false);
});

test('direct add rejects reduced fallback metadata even when it resembles a simple product', () => {
  assert.equal(canDirectAddProduct(product({ cardDataComplete: false })), false);
  assert.equal(canDirectAddProduct(product({ cardDataComplete: undefined })), false);
});

test('direct add rejects unavailable, out-of-stock, or outside-hours products', () => {
  assert.equal(canDirectAddProduct(product({ inStock: false })), false);
  assert.equal(canDirectAddProduct(product({ isAvailableNow: false })), false);
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'UNAVAILABLE', quantityAvailable: 5 } })), false);
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: 0 } })), false);
});

test('direct add rejects server-declared sold-out and suspended availability even with stock fields', () => {
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: 5, availabilityStatus: 'OUT_OF_STOCK' } })), false);
  assert.equal(canDirectAddProduct(product({ defaultVariant: { variantId: 10, status: 'AVAILABLE', quantityAvailable: null, availabilityStatus: 'SUSPENDED' } })), false);
});

test('ProductCard presents the confirmed hierarchy and action copy', () => {
  assert.match(productCardSource, /class="best-badge"><i class="fa-solid fa-fire" aria-hidden="true"><\/i>Bán chạy/);
  assert.match(productCardSource, /class="fa-solid fa-star" aria-hidden="true"><\/i>\{\{ ratingText \}\}/);
  assert.match(productCardSource, /\$\{averageRating\.value\.toFixed\(1\)\} · \$\{reviewCount\.value\} đánh giá/);
  assert.match(productCardSource, /class="product-sold"><i class="fa-solid fa-fire" aria-hidden="true"><\/i>\{\{ soldCount \}\} đã bán/);
  assert.match(productCardSource, /:class="added \? 'fa-solid fa-check' : 'fa-solid fa-plus'" aria-hidden="true"/);
  assert.match(productCardSource, /<span>Chọn món<\/span><i class="fa-solid fa-chevron-right" aria-hidden="true"/);
  assert.match(productCardSource, /\.product-image\{[^}]*height:200px/);
  assert.match(productCardSource, /\.product-name\{[^}]*min-height:43\.2px/);
  assert.match(productCardSource, /\.product-desc\{[^}]*min-height:34\.8px[^}]*-webkit-line-clamp:2/);
  assert.match(productCardSource, /\.product-footer\{[^}]*margin-top:auto/);
  assert.match(productCardSource, /\.price-now\{[^}]*font-size:20px[^}]*font-weight:800/);
});

test('ProductCard keeps controls accessible and mobile-safe', () => {
  assert.match(productCardSource, /\.fav-btn,.add-btn,.option-btn\{[^}]*min-height:44px/);
  assert.match(productCardSource, /:focus-visible/);
  assert.match(productCardSource, /prefers-reduced-motion:reduce/);
  assert.match(productCardSource, /@media\(max-width:560px\)[\s\S]*min-width:0/);
});

test('ProductCard favorite exposes toggle state and blocks repeated requests', () => {
  assert.match(productCardSource, /const favoritePending = ref\(false\)/);
  assert.match(productCardSource, /if \(favoritePending\.value\) return/);
  assert.match(productCardSource, /:disabled="favoritePending"/);
  assert.match(productCardSource, /:aria-pressed="favoriteStore\.isFavorite\(product\.productId\)"/);
  assert.match(productCardSource, /:aria-busy="favoritePending"/);
});

test('ProductCard renders only a positive finite computed discount and no Bootstrap icon classes', () => {
  assert.match(productCardSource, /const discountPercent = computed/);
  assert.match(productCardSource, /Number\.isFinite/);
  assert.match(productCardSource, /percent > 0/);
  assert.match(productCardSource, /v-if="discountPercent" class="hot-badge">-\{\{ discountPercent \}\}%/);
  assert.doesNotMatch(productCardSource, /\bbi-/);
});

test('ProductCard maps server availability to customer copy and locks the CTA when sold out', () => {
  assert.match(productCardSource, /'Ngoài giờ bán' : 'Tạm hết'/);
  assert.match(productCardSource, /v-if="availability\.status === 'LOW_STOCK'" class="stock-note">\{\{ availability\.label \}\}/);
  assert.match(productCardSource, /v-else-if="!availability\.available" class="option-btn soldout-btn" disabled>Tạm hết</);
  assert.match(productCardSource, /if \(!availability\.value\.available\) return notify\('Món hiện tạm hết'\)/);
});
