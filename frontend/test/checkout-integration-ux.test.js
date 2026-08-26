import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const checkout = fs.readFileSync(new URL('../src/views/user/CheckoutPage.vue', import.meta.url), 'utf8');
const cart = fs.readFileSync(new URL('../src/views/guest/CartPage.vue', import.meta.url), 'utf8');
const paymentReturn = fs.readFileSync(new URL('../src/views/user/PaymentReturnPage.vue', import.meta.url), 'utf8');
const success = fs.readFileSync(new URL('../src/views/user/OrderSuccessPage.vue', import.meta.url), 'utf8');
const stepper = fs.readFileSync(new URL('../src/components/common/CheckoutStepper.vue', import.meta.url), 'utf8');

test('customer order flow shares one accessible four-step visual system', () => {
  assert.match(stepper, /const steps = \[/);
  for (const label of ['Giỏ hàng', 'Thông tin giao', 'Thanh toán', 'Hoàn tất']) assert.match(stepper, new RegExp(label));
  assert.match(stepper, /:aria-current="step\.number === props\.current \? 'step' : undefined"/);
  assert.match(cart, /<CheckoutStepper :current="1"/);
  assert.match(checkout, /<CheckoutStepper :current="currentStep === 3 \? 3 : 2"/);
  assert.match(paymentReturn, /<CheckoutStepper :current="3"/);
  assert.match(success, /<CheckoutStepper :current="4"/);
});

test('checkout merges delivery and voucher before moving directly to payment', () => {
  assert.match(checkout, /<CheckoutStepper :current="currentStep === 3 \? 3 : 2"/);
  assert.match(checkout, /class="checkout-coupon checkout-section coupon-section"/);
  assert.match(checkout, /@click="currentStep = 3">Tiếp tục thanh toán/);
  assert.match(checkout, /@click="currentStep = 1">Quay lại thông tin/);
  assert.doesNotMatch(checkout, /currentStep\+\+|currentStep--/);
});

test('checkout keeps delivery content visible before payment and can restore saved addresses', () => {
  assert.match(checkout, /v-show="currentStep <= 2"/);
  assert.match(checkout, /function returnToSavedAddresses\(\)/);
  assert.match(checkout, /v-if="useNewAddress && savedAddresses\.length"[^>]*@click="returnToSavedAddresses"/);
  assert.match(checkout, /Chọn địa chỉ đã lưu/);
});

test('checkout presents delivery coupon and payment as professional ordered panels', () => {
  assert.match(checkout, /class="checkout-shell"/);
  assert.match(checkout, /class="checkout-main"/);
  assert.match(checkout, /class="checkout-sidebar checkout-summary-panel"/);
  assert.match(checkout, /class="[^"]*checkout-section delivery-section"/);
  assert.match(checkout, /class="checkout-coupon checkout-section coupon-section"/);
  assert.match(checkout, /class="[^"]*checkout-section payment-section"/);
});

test('payment return and order success expose branded completion surfaces', () => {
  assert.match(paymentReturn, /class="payment-shell"/);
  assert.match(paymentReturn, /class="[^"]*payment-status-card/);
  assert.match(success, /class="success-shell"/);
  assert.match(success, /class="success-header success-hero"/);
});

test('GHN failure remains unavailable and blocks checkout with retry copy', () => {
  assert.match(checkout, /shippingFee\.value = null;/);
  assert.match(checkout, /Dịch vụ giao hàng chưa được cấu hình hoặc tạm không khả dụng\. Vui lòng thử lại sau\./);
  assert.match(checkout, /if \(shippingFee\.value === null\) return toast\.error/);
  assert.match(checkout, /@click="calculateShipping\(\)">Thử lại/);
  assert.match(checkout, /!canPlaceOrder\(\)/);
});

test('PayOS transfer always renders disabled with reason when unavailable', () => {
  assert.match(checkout, /availablePaymentMethods = ref\(\['COD', 'BANK_TRANSFER'\]\)/);
  assert.match(checkout, /paymentAvailability/);
  assert.match(checkout, /function isPaymentEnabled\(key\)/);
  assert.match(checkout, /:aria-disabled="!isPaymentEnabled\(key\) && !\(isGuest && key === 'COD'\)"/);
  assert.match(checkout, /paymentAvailability\[key\]\?\.reason/);
});

test('guest checkout defaults to PayOS and gates COD behind account modal', () => {
  assert.match(checkout, /paymentMethod = ref\(isGuest\.value \? 'BANK_TRANSFER' : 'COD'\)/);
  assert.match(checkout, /if \(isGuest\.value && key === 'COD'\)/);
  assert.match(checkout, /codAccountDialog/);
  assert.match(checkout, /router\.push\(\{ name, query: \{ redirect: '\/checkout' \} \}\)/);
  assert.match(checkout, /goToAccount\('Login'\)/);
  assert.match(checkout, /goToAccount\('Register'\)/);
});

test('payment return verifies PayOS through payment status endpoint for authenticated orders', () => {
  assert.match(paymentReturn, /orderApi\.getPaymentStatus\(orderId\.value\)/);
  assert.doesNotMatch(paymentReturn, /orderApi\.getById\(orderId\.value\)/);
});

test('checkout keeps the website polling while PayOS opens separately', () => {
  assert.match(checkout, /paymentWindow = paymentMethod\.value === 'BANK_TRANSFER' \? window\.open\('', '_blank'\) : null/);
  assert.match(checkout, /paymentWindow\.location\.href = result\.checkoutUrl/);
  assert.match(checkout, /window\.__fastGuyPaymentWindow = paymentWindow/);
  assert.match(checkout, /name: 'PaymentReturn'/);
  assert.match(checkout, /returnProof: result\.returnProof/);
  assert.match(paymentReturn, /window\.__fastGuyPaymentWindow\?\.close\(\)/);
});

test('saved address selection reloads GHN hierarchy and fee without relying on province watcher', () => {
  assert.match(checkout, /applySavedAddress\(addr\)/);
  assert.match(checkout, /loadAddressHierarchy/);
  assert.match(checkout, /addressSelectionGeneration/);
});

test('disabled payment cannot be selected or submitted from stale state', () => {
  assert.match(checkout, /function selectPaymentMethod\(key, event\)/);
  assert.match(checkout, /if \(!isPaymentEnabled\(key\)\) return;/);
  assert.match(checkout, /if \(!isPaymentEnabled\(paymentMethod\.value\)\)/);
  assert.match(checkout, /@click="selectPaymentMethod\(key, \$event\)"/);
});

test('manual coupon form is available to guest and user while wallet remains user-only', () => {
  assert.match(checkout, /<form class="coupon-manual" @submit\.prevent="verifyCoupon"/);
  assert.match(checkout, /id="checkout-coupon-code"/);
  assert.match(checkout, /aria-describedby="checkout-coupon-status"/);
  assert.match(checkout, /<div v-if="!isGuest && !appliedCoupon" class="my-coupons">/);
  assert.doesNotMatch(checkout, /<div v-if="!isGuest" class="checkout-coupon">/);
});

test('coupon verification announces loading, errors, and applied state', () => {
  assert.match(checkout, /id="checkout-coupon-status"/);
  assert.match(checkout, /role="status"/);
  assert.match(checkout, /role="alert"/);
  assert.match(checkout, /:disabled="verifyingCoupon \|\| !couponCode\.trim\(\)"/);
  assert.equal(checkout.match(/couponCode: appliedCoupon\.value\?\.code \|\| ''/g)?.length, 2);
});

test('checkout uses coupon controller invalidation and accessible remove control', () => {
  assert.match(checkout, /createCouponController/);
  assert.match(checkout, /watch\(\[\(\) => cart\.subtotal, shippingFee\], invalidateCoupon\)/);
  assert.match(checkout, /aria-label="Xoá mã giảm giá"/);
  assert.match(checkout, /\.applied-remove \{[\s\S]*?min-width: 44px;[\s\S]*?min-height: 44px;/);
});
