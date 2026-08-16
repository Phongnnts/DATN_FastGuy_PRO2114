import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const checkout = fs.readFileSync(new URL('../src/views/user/CheckoutPage.vue', import.meta.url), 'utf8');

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
  assert.match(checkout, /:aria-disabled="!isPaymentEnabled\(key\)"/);
  assert.match(checkout, /paymentAvailability\[key\]\?\.reason/);
});

test('disabled payment cannot be selected or submitted from stale state', () => {
  assert.match(checkout, /function selectPaymentMethod\(key\)/);
  assert.match(checkout, /if \(!isPaymentEnabled\(key\)\) return;/);
  assert.match(checkout, /if \(!isPaymentEnabled\(paymentMethod\.value\)\)/);
  assert.match(checkout, /@click="selectPaymentMethod\(key\)"/);
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
