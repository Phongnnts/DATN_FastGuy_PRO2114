import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');
const authStore = read('../src/stores/auth.js');
const profile = read('../src/views/user/ProfilePage.vue');
const login = read('../src/views/guest/LoginPage.vue');
const tracking = read('../src/views/guest/TrackOrderPage.vue');
const orderDetail = read('../src/views/user/OrderDetailPage.vue');
const orderSuccess = read('../src/views/user/OrderSuccessPage.vue');
const adminOrderDetail = read('../src/views/admin/OrderDetailPage.vue');
const privacy = read('../src/views/guest/PrivacyPage.vue');
const router = read('../src/router/index.js');
const guestLayout = read('../src/layouts/GuestLayout.vue');
const productDetail = read('../src/views/guest/ProductDetailPage.vue');
const cartPage = read('../src/views/guest/CartPage.vue');
const cartMigration = read('../src/utils/cartMigration.js');
const menuPage = read('../src/views/guest/MenuPage.vue');
const promotionsPage = read('../src/views/guest/PromotionsPage.vue');
const customerSources = [
  router,
  guestLayout,
  ...fs.readdirSync(new URL('../src/views/guest/', import.meta.url)).filter(name => name.endsWith('.vue')).map(name => read(`../src/views/guest/${name}`)),
  ...fs.readdirSync(new URL('../src/views/user/', import.meta.url)).filter(name => name.endsWith('.vue')).map(name => read(`../src/views/user/${name}`)),
];

test('profiles and admin users support Cloudinary avatar upload and removal', () => {
  const profile = read('../src/views/user/ProfilePage.vue');
  const users = read('../src/views/admin/UsersPage.vue');
  for (const source of [profile, users]) {
    assert.match(source, /import axios from 'axios'/);
    assert.match(source, /import \{ CLOUDINARY \} from '@\/utils\/constants'/);
    assert.match(source, /data\.append\('upload_preset', CLOUDINARY\.uploadPreset\)/);
    assert.match(source, /axios\.post\(CLOUDINARY\.uploadUrl, data\)/);
    assert.match(source, /secure_url/);
    assert.match(source, /avatarUrl/);
  }
  assert.match(profile, /await auth\.updateProfile\(\{ fullName: name, email, phone, avatarUrl: form\.value\.avatarUrl \|\| null \}\)/);
  assert.match(profile, /Xóa ảnh/);
  assert.match(profile, /v-if="auth\.isUser" class="panel loyalty-panel"/);
  assert.match(users, /const payload = \{ fullName, email, phone, roleName: form\.value\.roleName, avatarUrl: form\.value\.avatarUrl \|\| null \}/);
  assert.match(users, /user\.avatarUrl/);
  assert.match(users, /Xóa ảnh/);
});

test('staff and shipper expose shared profile routes outside shift guards', () => {
  assert.match(router, /path: 'profile',\s*name: 'StaffProfile',\s*component: \(\) => import\('@\/views\/user\/ProfilePage\.vue'\)/s);
  assert.match(router, /path: 'profile',\s*name: 'ShipperProfile',\s*component: \(\) => import\('@\/views\/user\/ProfilePage\.vue'\)/s);
  assert.match(read('../src/layouts/StaffLayout.vue'), /\{ label: 'Hồ sơ', path: '\/staff\/profile', icon: 'bi-person-circle' \}/);
  assert.match(read('../src/layouts/ShipperLayout.vue'), /\{ path: '\/shipper\/profile', name: 'Hồ sơ', icon: 'bi-person-circle' \}/);
});

test('profile hydrates authoritative API data and persists normalized user', () => {
  assert.match(authStore, /async function hydrateProfile\(\)/);
  assert.match(authStore, /createProfileHydrationController/);
  assert.match(authStore, /requestProfile: \(\) => authApi\.getProfile\(\)/);
  assert.match(authStore, /persist,/);
  assert.match(authStore, /hydrateProfile,/);
});

test('profile page exposes loading error and retry states', () => {
  assert.match(profile, /const profileLoading = ref\(true\)/);
  assert.match(profile, /const profileError = ref\(''\)/);
  assert.match(profile, /hydrate: \(\) => auth\.hydrateProfile\(\)/);
  assert.match(profile, /role="status"/);
  assert.match(profile, /role="alert"/);
  assert.match(profile, /@click="loadProfile"/);
  assert.match(profile, /onUnmounted\(profileLoader\.stop\)/);
  assert.match(profile, /prefers-reduced-motion: reduce/);
});

test('login awaits cart migration and announces recoverable failure', () => {
  assert.match(login, /const migrationWarning = ref\(''\)/);
  assert.match(login, /createLoginMigrationController/);
  assert.match(login, /migrate: \(\) => cart\.migrateToUser\(\)/);
  assert.match(login, /await loginMigration\.submit/);
  assert.doesNotMatch(login, /migrateToUser\(\)\.catch\(\(\) => \{\}\)/);
  assert.match(login, /role="alert"/);
  assert.match(cartMigration, /Một số món chưa được đồng bộ/);
});

test('tracking displays only valid normalized backend ETA with accessible status', () => {
  assert.match(tracking, /const eta = computed\(\(\) => createEtaModel\(trackingResult\.value\?\.estimatedDeliveryAt\)\)/);
  assert.match(tracking, /v-if="eta"/);
  assert.match(tracking, /:datetime="eta\.datetime"/);
  assert.match(tracking, /\{\{ eta\.display \}\}/);
  assert.match(tracking, /Giờ Việt Nam/);
  assert.match(tracking, /class="eta-card" role="status"/);
  assert.match(tracking, /'DELIVERED', 'CANCELLED', 'RETURNED_TO_STORE'/);
  assert.doesNotMatch(tracking, /Date\.now\(\).*estimatedDeliveryAt/);
});

test('reorder delegates planning and execution before announcing accessible result', () => {
  assert.match(orderDetail, /modifiers: Array\.isArray\(i\.modifiers\) \? i\.modifiers : \[\]/);
  assert.match(orderDetail, /createReorderController/);
  assert.match(orderDetail, /await reorderController\.run\(order\.value\.items\)/);
  assert.match(orderDetail, /role="status" aria-live="polite" aria-atomic="true"/);
  assert.match(orderDetail, /:key="`\$\{modifier\.groupId\}:\$\{modifier\.modifierOptionId\}`"/);
});

test('product reviews stay private from homepage publication controls', () => {
  assert.match(orderDetail, /reviewApi|StarRating|createOrderReviewController/);
  assert.doesNotMatch(orderDetail, /homepageConsent|review-consent/);
  assert.doesNotMatch(adminOrderDetail, /updateFeaturedReview|order\.review|reviewSaving|featureIneligibilityReason/);
  assert.doesNotMatch(privacy, /chỉ được hiển thị công khai khi bạn đồng ý rõ ràng và quản trị viên chọn đăng/);
  assert.doesNotMatch(privacy, /đánh giá[^<]*hiển thị công khai|gửi yêu cầu qua tài khoản/i);
});

test('menu keeps essential filters in a compact customer-facing layout', () => {
  assert.match(menuPage, /class="compact-toolbar"/);
  assert.match(menuPage, /class="desktop-filter-toggle"/);
  assert.match(menuPage, /class="menu-sidebar advanced-filters filter-dropdown"/);
  assert.match(menuPage, /Khoảng giá<\/legend>/);
  assert.match(menuPage, /Tình trạng<\/legend>/);
  assert.match(menuPage, /Đang giảm giá/);
  assert.match(menuPage, /Bán chạy/);
  assert.doesNotMatch(menuPage, /productType/);
  assert.doesNotMatch(menuPage, /legend>Loại món<\/legend>/);
  assert.match(menuPage, /\.category-chips\{[^}]*max-height:46px/);
  assert.match(menuPage, /class="[^"]*filter-dropdown[^"]*"/);
  assert.match(menuPage, /class="result-summary"/);
  assert.match(menuPage, /class="result-count"/);
  assert.match(menuPage, /class="result-context"/);
  assert.match(menuPage, /class="result-actions"/);
  assert.match(menuPage, /class="filter-group"/);
  assert.match(menuPage, /\.filter-dropdown\{[^}]*position:absolute/);
  assert.match(menuPage, /fa-solid fa-sliders/);
  assert.match(menuPage, /class="active-filters"/);
  assert.match(menuPage, /class="toolbar-sort">Sắp xếp:/);
  assert.match(menuPage, /const sort = ref\('best-selling'\)/);
  assert.match(menuPage, /label: 'Phổ biến nhất'/);
  assert.match(menuPage, /class="pagination-summary"/);
  assert.match(menuPage, /class="sheet-footer"/);
  assert.match(menuPage, /Xem \{\{ store\.catalogMeta\.totalItems \}\} món/);
  assert.match(menuPage, /max-height:85vh/);
  assert.match(menuPage, /min-height:44px/);
  assert.match(menuPage, /menuSearchSuggestions\(store\.allProducts, q\.value\)/);
  assert.match(menuPage, /role="combobox"/);
  assert.match(menuPage, /role="listbox"/);
  assert.match(menuPage, /@keydown="onSearchKeydown"/);
  assert.match(menuPage, /aria-activedescendant/);
  assert.doesNotMatch(menuPage, /productApi|\/api\/products/);
});

test('promotions use compact commercial voucher tickets and short claim actions', () => {
  assert.match(promotionsPage, /const activeFilter = ref\('ALL'\)/);
  assert.match(promotionsPage, /const filteredCoupons = computed/);
  assert.match(promotionsPage, /class="promo-filters"/);
  assert.match(promotionsPage, /class="ticket-notch notch-top"/);
  assert.match(promotionsPage, /class="ticket-notch notch-bottom"/);
  assert.match(promotionsPage, /class="coupon-copy"/);
  assert.match(promotionsPage, /class="coupon-meta"/);
  assert.match(promotionsPage, />Nhận mã\s*<i/);
  assert.doesNotMatch(promotionsPage, /Đăng nhập để nhận/);
  assert.match(promotionsPage, /\.promo-grid\s*\{[^}]*grid-template-columns:\s*repeat\(2/);
  assert.match(promotionsPage, /border-left:\s*1px dashed/);
  assert.match(promotionsPage, /:class="`value-\$\{coupon\.type\.toLowerCase\(\)\}`"/);
  assert.match(promotionsPage, /\.value-fixed\{[^}]*font-size:clamp\(19px,1\.8vw,24px\)/);
  assert.match(promotionsPage, /\.value-free_shipping\{[^}]*font-size:clamp\(18px,1\.65vw,22px\)/);
  assert.match(promotionsPage, /\.discount-block strong\{[^}]*max-width:100%[^}]*overflow-wrap:anywhere/);
});

test('product detail favorites, option groups, and narrow related cards expose premium states', () => {
  assert.match(productDetail, /const favoritePending = ref\(false\)/);
  assert.match(productDetail, /if \(favoritePending\.value\) return/);
  assert.match(productDetail, /:disabled="favoritePending"/);
  assert.match(productDetail, /:aria-pressed="favoriteStore\.isFavorite\(product\.productId\)"/);
  assert.match(productDetail, /:aria-busy="favoritePending"/);
  assert.match(productDetail, /<fieldset v-if="product\.variants\?\.length" class="selection-group">/);
  assert.match(productDetail, /<legend class="selection-title">/);
  assert.match(productDetail, /:aria-invalid="Boolean\(modifierErrors\[group\.modifierGroupId\]\)"/);
  assert.match(productDetail, /:aria-describedby="`modifier-help-\$\{group\.modifierGroupId\}`"/);
  assert.match(productDetail, /@media \(max-width: 360px\)[^{]*\{[^}]*\.related-products\{grid-template-columns:1fr\}/);
  assert.doesNotMatch(productDetail, /\bbi-/);
});

test('customer surfaces do not hardcode unsupported delivery claims', () => {
  for (const source of customerSources) {
    assert.doesNotMatch(source, /giao[^\n<]{0,40}30 phút/i);
    assert.doesNotMatch(source, /Miễn phí ship/i);
    assert.doesNotMatch(source, /giao[^\n<]{0,60}(liên tỉnh|toàn quốc|khắp (?:cả )?nước)/i);
  }
  assert.match(productDetail, /createStoreConfigController/);
  assert.match(cartPage, /Thời gian và phí giao hàng được tính sau khi bạn chọn địa chỉ nhận hàng\./);
  assert.match(orderDetail, /formatPrice\(order\.shippingFee\)/);
  assert.match(orderSuccess, /formatPrice\(order\.shippingFee\)/);
  assert.doesNotMatch(orderDetail, /shippingFee[^\n]{0,80}Miễn phí/);
  assert.doesNotMatch(orderSuccess, /shippingFee[^\n]{0,80}Miễn phí/);
});
