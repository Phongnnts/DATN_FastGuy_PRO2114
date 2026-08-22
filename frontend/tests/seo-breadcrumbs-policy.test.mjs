import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { resolveCanonical, isIndexable } from '../src/router/seo.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const breadcrumbs = read('../src/components/common/AppBreadcrumbs.vue');
const terms = read('../src/views/guest/TermsPage.vue');
const privacy = read('../src/views/guest/PrivacyPage.vue');
const products = read('../src/views/admin/ProductsPage.vue');
const inventory = read('../src/views/admin/InventoryPage.vue');
const ledger = read('../src/views/admin/InventoryLedgerPage.vue');

test('resolveCanonical keeps literal paths and substitutes params', () => {
  assert.equal(resolveCanonical('/menu', {}), '/menu');
  assert.equal(resolveCanonical('/product/:id', { params: { id: 7 } }), '/product/7');
  assert.equal(resolveCanonical('/product/:id', { params: {} }), '/product/:id');
  assert.equal(resolveCanonical('/product/:id', { params: { id: 0 } }), '/product/0');
});

test('resolveCanonical supports function canonical values', () => {
  const route = { params: { id: 3 } };
  assert.equal(resolveCanonical((to) => `/p/${to.params.id}`, route), '/p/3');
});

test('isIndexable accepts only explicit leading index token', () => {
  assert.equal(isIndexable('index,follow'), true);
  assert.equal(isIndexable('index'), true);
  assert.equal(isIndexable('noindex,nofollow'), false);
  assert.equal(isIndexable('noindex'), false);
  assert.equal(isIndexable(''), false);
});

test('head manager wires split-based indexable predicate, not substring match', () => {
  assert.match(router, /import \{ resolveCanonical, isIndexable \} from '\.\/seo'/);
  assert.match(router, /const indexable = isIndexable\(robots\)/);
  assert.doesNotMatch(router, /robots\.includes\('index'\)/);
});

test('noindex routes emit no og tags through head manager', () => {
  assert.match(router, /upsertMeta\('og:title', indexable && title \? .* : ''\)/);
  assert.match(router, /upsertMeta\('og:description', indexable \? .* : ''\)/);
  assert.match(router, /upsertMeta\('og:url', indexable \? canonical : ''\)/);
});

test('seven indexable routes carry robots description and canonical', () => {
  assert.equal((router.match(/robots: 'index,follow'/g) || []).length, 7);
  assert.equal((router.match(/description:/g) || []).length, 7);
  for (const path of ['/home', '/menu', '/product/:id', '/promotions', '/help', '/terms', '/privacy']) {
    assert.ok(router.includes(`canonical: '${path}'`), `missing canonical ${path}`);
  }
});

test('breadcrumb meta covers account, admin detail, and staff operations pages', () => {
  assert.equal((router.match(/breadcrumb:/g) || []).length, 23);
  assert.match(router, /name: 'StaffProfile',[\s\S]*?breadcrumb: \[\{ label: 'Vận hành', to: '\/staff' \}, \{ label: 'Hồ sơ' \}\]/);
  assert.match(router, /name: 'ShipperProfile',[\s\S]*?breadcrumb: \[\{ label: 'Giao hàng', to: '\/shipper' \}, \{ label: 'Hồ sơ' \}\]/);
  const productEditor = router.slice(
    router.indexOf("name: 'AdminProductCreate'"),
    router.indexOf("name: 'AdminProducts'"),
  );
  assert.doesNotMatch(productEditor, /breadcrumb/);
  assert.match(router, /{ label: 'Đơn hàng', to: '\/admin\/orders' \},\s*{ label: 'Chi tiết' }/);
  assert.match(router, /{ label: 'Đơn hàng', to: '\/admin\/orders' \},\s*{ label: 'Hoàn tiền' }/);
  assert.match(router, /{ label: 'Tồn kho', to: '\/admin\/inventory' \},\s*{ label: 'Sổ tồn kho' }/);
});

test('AppBreadcrumbs renders meta.breadcrumb with aria contract and no title fallback', () => {
  assert.match(breadcrumbs, /route\.meta\.breadcrumb/);
  assert.match(breadcrumbs, /v-if="crumbs\.length"/);
  assert.match(breadcrumbs, /aria-label="Breadcrumb"/);
  assert.match(breadcrumbs, /aria-current="page"/);
  assert.doesNotMatch(breadcrumbs, /meta\.title/);
});

test('public policy and SEO copy does not promise unavailable support, review, or combo features', () => {
  assert.doesNotMatch(terms, /gửi yêu cầu hỗ trợ/);
  assert.doesNotMatch(privacy, /Đánh giá chỉ được hiển thị công khai|gửi yêu cầu qua tài khoản/);
  assert.doesNotMatch(router, /description:.*combo/i);
  assert.match(router, /description:.*món.*kích cỡ.*topping/i);
});

test('admin catalog and inventory surfaces label variants as sizes', () => {
  for (const page of [products, inventory, ledger]) {
    assert.doesNotMatch(page, />Biến thể</);
    assert.doesNotMatch(page, /data-label="Biến thể"/);
    assert.match(page, /Kích cỡ/);
  }
});
