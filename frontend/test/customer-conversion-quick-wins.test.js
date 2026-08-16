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
const router = read('../src/router/index.js');
const guestLayout = read('../src/layouts/GuestLayout.vue');
const productDetail = read('../src/views/guest/ProductDetailPage.vue');
const cartPage = read('../src/views/guest/CartPage.vue');
const cartMigration = read('../src/utils/cartMigration.js');
const customerSources = [
  router,
  guestLayout,
  ...fs.readdirSync(new URL('../src/views/guest/', import.meta.url)).filter(name => name.endsWith('.vue')).map(name => read(`../src/views/guest/${name}`)),
  ...fs.readdirSync(new URL('../src/views/user/', import.meta.url)).filter(name => name.endsWith('.vue')).map(name => read(`../src/views/user/${name}`)),
];

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
