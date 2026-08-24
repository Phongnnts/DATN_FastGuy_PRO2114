import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { createMemoryHistory, createRouter } from 'vue-router';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const userOrder = read('../src/views/user/OrderDetailPage.vue');
const adminOrder = read('../src/views/admin/OrderDetailPage.vue');
const home = read('../src/views/guest/HomePage.vue');
const guestLayout = read('../src/layouts/GuestLayout.vue');
const staffLayout = read('../src/layouts/StaffLayout.vue');
const accountTabs = read('../src/components/common/AccountTabs.vue');
const accountOverview = read('../src/views/user/AccountOverviewPage.vue');
const help = read('../src/views/guest/HelpPage.vue');
const productDetail = read('../src/views/guest/ProductDetailPage.vue');
const productEditor = read('../src/views/admin/ProductEditorPage.vue');
const variantsSection = read('../src/components/admin/product-editor/ProductVariantsSection.vue');
const modifiersSection = read('../src/components/admin/product-editor/ProductModifiersSection.vue');
const orderSuccess = read('../src/views/user/OrderSuccessPage.vue');
const staffOrder = read('../src/views/staff/OrderDetailPage.vue');
const productCard = read('../src/components/common/ProductCard.vue');
const productsCatalog = read('../src/views/admin/ProductsPage.vue');
const inventory = read('../src/views/admin/InventoryPage.vue');
const inventoryLedger = read('../src/views/admin/InventoryLedgerPage.vue');
const terms = read('../src/views/guest/TermsPage.vue');
const privacy = read('../src/views/guest/PrivacyPage.vue');
const checkout = read('../src/views/user/CheckoutPage.vue');
const shipperOrder = read('../src/views/shipper/OrderDetailPage.vue');
const shipperSheet = read('../src/components/shipper/OrderActionSheet.vue');
const settings = read('../src/views/admin/SettingsPage.vue');
const reports = read('../src/views/admin/ReportsPage.vue');

function routeRecords() {
  const start = router.indexOf('const routes = [');
  const end = router.indexOf('\n];', start) + 3;
  const declaration = router.slice(start, end);
  const layout = {};
  return Function('GuestLayout', 'UserLayout', 'StaffLayout', 'ShipperLayout', 'AdminLayout', 'ROLES', `${declaration}; return routes;`)(layout, layout, layout, layout, layout, { USER: 'USER', STAFF: 'STAFF', SHIPPER: 'SHIPPER', ADMIN: 'ADMIN' });
}

test('product review UI stays scoped to delivered customer orders', () => {
  assert.match(userOrder, /v-if="isDelivered"/);
  assert.match(userOrder, /reviewApi|StarRating|Đánh giá sản phẩm/);
  assert.doesNotMatch(userOrder, /homepageConsent|review-consent/);
  assert.doesNotMatch(adminOrder, /updateFeaturedReview|order\.review|review-card/);
  assert.match(home, /featuredReviews/);
  assert.match(home, /Nguyên liệu mỗi ngày/);
  assert.match(home, /Làm khi có đơn/);
  assert.match(home, /Kiểm tra và đóng gói/);
  assert.doesNotMatch(home, /Hỗ trợ khi cần|kênh hỗ trợ/);
});

test('removed support paths resolve to NotFound', () => {
  const matcher = createRouter({ history: createMemoryHistory(), routes: routeRecords() });
  for (const path of ['/account/support', '/staff/support']) {
    assert.equal(matcher.resolve(path).name, 'NotFound');
  }
});

test('support navigation and ticket CTA are absent', () => {
  for (const source of [guestLayout, staffLayout, accountTabs, accountOverview, help]) {
    assert.doesNotMatch(source, /\/account\/support|\/staff\/support/);
  }
  assert.doesNotMatch(help, /đăng nhập để gửi|gửi yêu cầu hỗ trợ/i);
});

test('COD refund and shifts remain visible', () => {
  assert.match(userOrder, /refundLabel/);
  assert.match(staffLayout, /StaffShifts|Ca làm việc/);
  assert.match(router, /StaffShifts/);
});

test('combo UI is absent from homepage, product detail, and admin editor', () => {
  assert.doesNotMatch(home, /HomepageOccasions|occasionCombos/);
  assert.doesNotMatch(productDetail, /product\.combo|candidate\.isCombo|Combo gồm/);
  assert.doesNotMatch(productDetail, /review\.(avatar|orderId|homepageConsent|featured)/);
  assert.doesNotMatch(productEditor, /ProductComboSection|id: 'combo'|activeSection === 'combo'/);
});

test('product cards hide combo presentation while preserving truthful badges and actions', () => {
  assert.doesNotMatch(productCard, /product\.isCombo|combo-badge|>Combo</);
  assert.match(productCard, /product\.bestSeller/);
  assert.match(productCard, /product\.isNew/);
  assert.match(productCard, /product\.discountPercent/);
  assert.doesNotMatch(productCard, /product\.spiceLevel|spice-badge|hasOptions/);
  assert.match(productCard, /addToCart|toggleFavorite/);
});

test('visible product option wording uses Kích cỡ and Topping or Tùy chọn', () => {
  assert.match(productDetail, /Kích cỡ/);
  assert.doesNotMatch(productDetail, />Phân loại</);
  assert.match(variantsSection, /Kích cỡ/);
  assert.doesNotMatch(variantsSection, />Biến thể</);
  assert.match(modifiersSection, /Topping|Tùy chọn/);
  assert.doesNotMatch(staffOrder, /<th>Phân loại<\/th>/);
  assert.doesNotMatch(adminOrder, /<th>Phân loại<\/th>/);
  assert.doesNotMatch(userOrder, /<th>Phân loại<\/th>/);
  assert.doesNotMatch(orderSuccess, /<th>Phân loại<\/th>/);
});

test('legal pages expose help contact without removed review or account support wording', () => {
  assert.match(terms, /Thông tin hướng dẫn về đơn hàng và điều khoản được đăng tại <RouterLink to="\/help">Trung tâm trợ giúp<\/RouterLink>\./);
  assert.doesNotMatch(terms, /đăng nhập để gửi yêu cầu hỗ trợ/i);
  assert.match(privacy, /Nội dung bạn chủ động gửi khi liên hệ với FastGuy\./);
  assert.doesNotMatch(privacy, /đánh giá[^<]*hiển thị công khai|gửi yêu cầu qua tài khoản/i);
  assert.match(privacy, /Thông tin về quyền riêng tư và bảo vệ dữ liệu được đăng tại <RouterLink to="\/help">Trung tâm trợ giúp<\/RouterLink>\./);
});

test('public descriptions and admin inventory use menu option wording', () => {
  assert.match(router, /đặt món ăn nhanh trực tuyến, chọn kích cỡ và topping theo sở thích/);
  assert.match(router, /Thực đơn FastGuy — chọn món, kích cỡ và topping theo sở thích/);
  assert.doesNotMatch(router, /combo tiết kiệm/i);
  for (const source of [productsCatalog, inventory, inventoryLedger]) {
    assert.doesNotMatch(source, /biến thể/i);
  }
  assert.match(productsCatalog, /Kích cỡ/);
});

test('service fee presentation is absent while checkout arithmetic and backend totals remain', () => {
  for (const source of [checkout, userOrder, orderSuccess, staffOrder, shipperOrder, shipperSheet, settings, reports, adminOrder]) {
    assert.doesNotMatch(source, /Phí dịch vụ|Phí phục vụ/);
  }
  assert.match(checkout, /cart\.subtotal \+ \(shippingFee\.value \|\| 0\) \+ serviceFee\.value - couponDiscount\.value/);
  assert.match(userOrder, /total: data\.finalAmount \|\| 0/);
  assert.match(userOrder, /formatPrice\(order\.total\)/);
  assert.match(orderSuccess, /formatPrice\(order\.total\)/);
  assert.match(staffOrder, /formatPrice\(order\.total\)/);
  assert.match(shipperOrder, /formatPrice\(order\.total\)/);
  assert.match(adminOrder, /formatPrice\(order\.finalAmount\)/);
  assert.doesNotMatch(reports, /serviceFeeRevenue/);
});

test('refund COD shifts and backend report totals remain', () => {
  assert.match(staffOrder, /COD|paymentMethod/);
  assert.match(reports, /grossRevenue/);
  assert.match(reports, /refundTotal/);
  assert.match(reports, /netCashRevenue/);
});
