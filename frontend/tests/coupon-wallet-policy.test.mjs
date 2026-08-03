import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/user/CouponWalletPage.vue', import.meta.url), 'utf8');

test('coupon wallet follows claimed coupon response policy', () => {
  assert.match(page, /couponApi\.getClaimed\(\)/);
  assert.match(page, /v-for="coupon in coupons" :key="coupon\.claimedId"/);
  assert.match(page, /navigator\.clipboard\.writeText\(coupon\.code\)/);
  assert.match(page, /to="\/menu"[\s\S]*Dùng ngay/);
  assert.match(page, /v-if="loading"[\s\S]*v-else-if="error"[\s\S]*coupons\.length === 0/);
  assert.doesNotMatch(page, /coupon\.(?:usedAt|isUsed|expiresAt)/);
});
