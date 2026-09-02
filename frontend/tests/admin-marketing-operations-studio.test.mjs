import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const coupons = read('../src/views/admin/CouponsPage.vue');
const banners = read('../src/views/admin/BannersPage.vue');
const homepagePolicy = read('./admin-homepage-controls.test.mjs');

test('coupons expose campaign validity eligibility and guarded lifecycle state', () => {
  assert.match(coupons, /campaign-header/);
  assert.match(coupons, /coupon-campaign-workspace/);
  assert.match(coupons, /Đã hết hiệu lực/);
  assert.match(coupons, /Thời hạn ưu đãi/);
  assert.match(coupons, /Đơn tối thiểu/);
  assert.match(coupons, /:disabled="!coupon.canDelete \|\| !!pendingAction"/);
  assert.match(coupons, /<label class="form-label" for="coupon-/);
});

test('banners expose preview destination visibility and ordering evidence', () => {
  assert.match(banners, /banner-content-workspace/);
  assert.match(banners, /preview-led-content-list/);
  assert.match(banners, /:alt="banner.title"/);
  assert.match(banners, /Đang hiển thị/);
  assert.match(banners, /Đang ẩn/);
  assert.match(banners, /Đích đến/);
  assert.match(banners, /Thứ tự/);
  assert.match(banners, /confirm\(`Xóa banner/);
});

test('homepage review merchandising remains dormant outside reachable UI', () => {
  assert.match(homepagePolicy, /admin review feature stays dormant outside reachable order UI/);
  assert.match(homepagePolicy, /doesNotMatch\(order, \/updateFeaturedReview/);
});
