import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('homepage API client requests the contract endpoint and is exported', async () => {
  const [api, index] = await Promise.all([
    read('../src/api/homepage.js'),
    read('../src/api/index.js'),
  ]);
  assert.match(api, /client\.get\('\/homepage'\)/);
  assert.match(index, /homepageApi/);
});

test('homepage store protects stale responses and exposes retry state', async () => {
  const source = await read('../src/stores/homepage.js');
  assert.match(source, /let requestGeneration = 0/);
  assert.match(source, /const request = \+\+requestGeneration/);
  assert.match(source, /if \(request !== requestGeneration\) return/);
  assert.match(source, /const loading = ref\(false\)/);
  assert.match(source, /const error = ref\(''\)/);
  assert.match(source, /async function load\(\)/);
  assert.match(source, /async function retry\(\)/);
});

test('homepage V2 keeps contract loading while review and occasion UI stay dormant', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /useHomepageStore/);
  assert.doesNotMatch(source, /HomepageOccasions|occasionCombos/);
  assert.doesNotMatch(source, /HomepageProof|featuredReviews/);
  assert.match(source, /homepageStore\.load\(\)/);
  assert.doesNotMatch(source, /productStore\.fetchFeatured/);
  assert.doesNotMatch(source, /productStore\.fetchFeatured/);
});

test('homepage occasion mapping derives copy from contract enum only', async () => {
  const [component, mapper] = await Promise.all([
    read('../src/components/guest/HomepageOccasions.vue'),
    read('../src/utils/homepage.js'),
  ]);
  for (const occasion of ['QUICK_BREAK', 'OFFICE_LUNCH', 'STUDENT', 'GROUP']) assert.match(mapper, new RegExp(occasion));
  assert.match(component, /props\.items\.slice\(0, 4\)/);
  assert.match(component, /ProductCard/);
});

test('homepage keeps non-review reasons without support or live-location claims', async () => {
  const page = await read('../src/views/guest/HomePage.vue');
  assert.match(page, /Món rõ giá, dễ chọn/);
  assert.match(page, /Tùy chỉnh theo khẩu vị/);
  assert.match(page, /Theo dõi trạng thái xử lý và giao đơn/);
  assert.doesNotMatch(page, /HomepageProof|featuredReviews|Hỗ trợ khi cần|kênh hỗ trợ|đang ở đâu|bất cứ lúc nào/);
});

test('product card maps visible homepage badges and mobile controls remain touch sized', async () => {
  const source = await read('../src/components/common/ProductCard.vue');
  assert.match(source, /product\.isNew/);
  assert.match(source, /product\.spiceLevel/);
  assert.match(source, /\.new-badge\{background:/);
  assert.match(source, /\.spice-badge\{background:/);
  assert.match(source, /\.fav-btn,.product-card:not\(\.list-mode\) \.add-btn\{width:44px;height:44px;min-height:44px\}/);
  assert.match(source, /\.option-btn\{[^}]*min-height:44px/);
});

test('featured products contains no dead spotlight or top-pick presentation', async () => {
  const source = await read('../src/components/guest/FeaturedProducts.vue');
  assert.doesNotMatch(source, /spotlight|top-pick/);
});

test('conversion homepage uses real categories and accessible sales hero controls', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /productApi\.getCategories\(\)/);
  assert.match(source, /class="quick-categories"/);
  assert.match(source, /:to="\{ path: '\/menu', query: \{ category: category\.id \} \}"/);
  assert.match(source, /class="hero-counter"/);
  assert.match(source, /:aria-label="`Chọn banner \$\{index \+ 1\}`"/);
  assert.match(source, /prefers-reduced-motion:reduce/);
  assert.doesNotMatch(source, /countdown|người đang chọn|2\.384/);
});

test('homepage categories use managed images with an accessible fallback', async () => {
  const [page, admin, store] = await Promise.all([
    read('../src/views/guest/HomePage.vue'),
    read('../src/views/admin/CategoriesPage.vue'),
    read('../src/stores/product.js'),
  ]);
  assert.match(page, /imageUrl: item\.imageUrl/);
  assert.match(page, /class="category-image"/);
  assert.match(page, /:src="category\.imageUrl"/);
  assert.match(page, /v-else class="category-image-fallback"/);
  assert.match(admin, /v-model="form\.imageUrl"/);
  assert.match(admin, /Ảnh danh mục/);
  assert.match(store, /imageUrl: c\.imageUrl \|\| ''/);
});

test('homepage categories use visual cards with responsive grid and mobile rail', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /class="category-card"/);
  assert.match(source, /class="category-icon"/);
  assert.match(source, /class="category-arrow"/);
  assert.match(source, /\.category-list\{[^}]*grid-template-columns:repeat\(6,minmax\(0,1fr\)\)/);
  assert.match(source, /@media\(max-width:1000px\)[\s\S]*\.category-list\{grid-template-columns:repeat\(3,minmax\(0,1fr\)\)/);
  assert.match(source, /@media\(max-width:520px\)[\s\S]*grid-auto-columns:140px/);
  assert.match(source, /\.category-card:hover \.category-icon/);
});

test('homepage product presentation uses explicit action labels without changing catalog cards', async () => {
  const [card, featured, occasions] = await Promise.all([
    read('../src/components/common/ProductCard.vue'),
    read('../src/components/guest/FeaturedProducts.vue'),
    read('../src/components/guest/HomepageOccasions.vue'),
  ]);
  assert.match(card, /homepage: \{ type: Boolean, default: false \}/);
  assert.match(card, /'homepage-card': homepage/);
  assert.match(card, />Thêm</);
  assert.match(card, />Chọn món</);
  assert.match(featured, /<ProductCard :product="product" homepage/);
  assert.match(occasions, /<ProductCard :product="item\.product" homepage/);
});

test('homepage embeds an accessible map from public store configuration', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /storeApi\.getConfig\(\)/);
  assert.match(source, /encodeURIComponent\(storeAddress\.value\)/);
  assert.match(source, /v-if="storeAddress" class="store-location"/);
  assert.match(source, /<iframe[^>]*:src="mapEmbedUrl"[^>]*:title="`Bản đồ \$\{storeName\}`"/);
  assert.match(source, /loading="lazy"/);
  assert.match(source, /referrerpolicy="no-referrer-when-downgrade"/);
  assert.match(source, /:href="mapOpenUrl"[^>]*target="_blank"[^>]*rel="noopener noreferrer"/);
});

test('guest header exposes a descriptive desktop cart summary', async () => {
  const source = await read('../src/components/common/PublicHeader.vue');
  assert.match(source, /class="cart-summary"/);
  assert.match(source, /Giỏ hàng/);
  assert.match(source, /\{\{ cart\.itemCount \}\} món/);
});

test('guest and account layouts share one complete public header', async () => {
  const guest = await read('../src/layouts/GuestLayout.vue');
  const user = await read('../src/layouts/UserLayout.vue');
  for (const source of [guest, user]) {
    assert.match(source, /import PublicHeader from '@\/components\/common\/PublicHeader\.vue'/);
    assert.match(source, /<PublicHeader \/>/);
  }
});

test('public header balances complete navigation and role actions across breakpoints', async () => {
  const source = await read('../src/components/common/PublicHeader.vue');
  assert.match(source, /\{ label: 'Trang chủ', path: '\/home' \}/);
  assert.match(source, /\{ label: 'Thực đơn', path: '\/menu' \}/);
  assert.match(source, /\{ label: 'Khuyến mãi', path: '\/promotions' \}/);
  assert.match(source, /\{ label: 'Tra cứu đơn', path: '\/track-order' \}/);
  assert.match(source, /class="site-header"/);
  assert.match(source, /class="nav-actions"/);
  assert.match(source, /<NotificationBell v-if="auth\.isUser" \/>/);
  assert.match(source, /class="cart-summary"/);
  assert.match(source, /v-if="auth\.isUser" to="\/account\/overview"/);
  assert.match(source, /v-else to="\/" class="login-btn"/);
  assert.match(source, /id="public-navigation"/);
  assert.match(source, /aria-controls="public-navigation"/);
  assert.match(source, /:aria-expanded="mobileMenuOpen"/);
  assert.match(source, /class="nav-backdrop"/);
  assert.match(source, /if \(event\.key === 'Escape' && mobileMenuOpen\.value\) closeMenu\(true\)/);
  assert.match(source, /document\.body\.style\.overflow = open \? 'hidden' : ''/);
  assert.match(source, /\.nav-links \{ position: fixed;[^}]*height: calc\(100dvh - var\(--header-height\)\);[^}]*overflow-y: auto;/);
});
