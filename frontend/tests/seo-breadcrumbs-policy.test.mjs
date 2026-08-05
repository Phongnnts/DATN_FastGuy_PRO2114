import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { resolveCanonical, isIndexable } from '../src/router/seo.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const breadcrumbs = read('../src/components/common/AppBreadcrumbs.vue');

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
  assert.equal((router.match(/breadcrumb:/g) || []).length, 22);
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
