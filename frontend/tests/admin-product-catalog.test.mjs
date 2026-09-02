import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import {
  catalogAvailabilityPresentation,
  catalogCounts,
  filterProducts,
  paginateProducts,
  productTypes,
} from '../src/utils/adminProductCatalog.js';

const productsPage = readFileSync(new URL('../src/views/admin/ProductsPage.vue', import.meta.url), 'utf8');
const inventoryPage = readFileSync(new URL('../src/views/admin/InventoryPage.vue', import.meta.url), 'utf8');
const adminStore = readFileSync(new URL('../src/stores/admin.js', import.meta.url), 'utf8');

const products = [
  { id: 1, name: 'Burger', productType: 'SIMPLE', status: 'AVAILABLE', inStock: true, discountPrice: null, categoryId: 1, categoryName: 'Món chính', basePrice: 50000, variants: [] },
  { id: 2, name: 'Combo', productType: 'COMBO', status: 'AVAILABLE', inStock: false, discountPrice: 80000, categoryId: 2, categoryName: 'Combo', basePrice: 100000, variants: [] },
  { id: 3, name: 'Pizza', productType: 'VARIANT', status: 'UNAVAILABLE', inStock: false, discountPrice: 0, categoryId: 1, categoryName: 'Món chính', basePrice: 90000, variants: [{ variantName: 'L', sku: 'PIZZA-L' }] },
];

test('productTypes returns actual unique canonical values', () => {
  assert.deepEqual(productTypes(products), ['COMBO', 'SIMPLE', 'VARIANT']);
});

test('filterProducts applies canonical product type with search and category', () => {
  assert.deepEqual(filterProducts(products, { query: 'combo', categoryId: '2', productType: 'COMBO' }).map((product) => product.id), [2]);
  assert.deepEqual(filterProducts(products, { query: 'pizza-l', productType: 'VARIANT' }).map((product) => product.id), [3]);
});

test('catalogCounts uses canonical availability stock and discount fields', () => {
  assert.deepEqual(catalogCounts(products), { total: 3, available: 2, outOfStock: 2, discounted: 1 });
});

test('catalog availability ignores physical stock for an available product', () => {
  const zeroStockAvailable = { status: 'AVAILABLE', variants: [{ quantityAvailable: 0 }] };
  assert.deepEqual(catalogAvailabilityPresentation(zeroStockAvailable), { label: 'Đang hiển thị', tone: 'success' });
});

test('paginateProducts clamps pages and returns stable bounds', () => {
  assert.deepEqual(paginateProducts(products, 2, 2), { page: 2, pageCount: 2, start: 2, end: 3, items: [products[2]] });
  assert.deepEqual(paginateProducts(products, 9, 2), { page: 2, pageCount: 2, start: 2, end: 3, items: [products[2]] });
  assert.deepEqual(paginateProducts([], 1, 10), { page: 1, pageCount: 1, start: 0, end: 0, items: [] });
});

test('admin catalog hides COMBO type while preserving regular product rows', () => {
  assert.match(productsPage, /productTypes\(adminStore\.allProducts\)\.filter\(\(type\) => type !== 'COMBO'\)/);
  assert.doesNotMatch(productsPage, /\{\{ product\.productType \}\}/);
  assert.match(productsPage, /#\{\{ product\.id \}\} · \{\{ categoryName\(product\) \}\}/);
});

test('products page keeps shared low-stock policy while inventory uses item-level states', () => {
  assert.match(productsPage, /productStockSummary/);
  assert.match(productsPage, /lowStockThreshold/);
  assert.doesNotMatch(productsPage, /stock <= 10|stock > 10/);
  assert.match(inventoryPage, /itemStockState/);
  assert.match(inventoryPage, /availableQuantity/);
  assert.doesNotMatch(inventoryPage, /lowStockThreshold|inventoryRowsSummary/);
  assert.match(adminStore, /createLatestCatalogFetcher/);
});

test('products page composes the catalog workspace for desktop and mobile', () => {
  assert.match(productsPage, /class="catalog-header"/);
  assert.match(productsPage, /class="catalog-toolbar"/);
  assert.match(productsPage, /class="catalog-workspace"/);
  assert.match(productsPage, /class="mobile-catalog"[^>]*aria-label="Danh sách sản phẩm trên thiết bị di động"/);
  assert.match(productsPage, /class="product-mobile-card"/);
});

test('products page distinguishes catalog availability from physical capacity facts', () => {
  assert.match(productsPage, />Hiển thị trong danh mục</);
  assert.match(productsPage, />Tồn kho và năng lực bán</);
  assert.match(productsPage, /Dữ liệu tồn kho theo từng kích cỡ/);
  assert.match(productsPage, /capacityText\(variant\)\.label/);
  assert.match(productsPage, /catalogAvailability\(product\)\.label/);
  assert.doesNotMatch(productsPage, /stockSummary\(product\)\.status === 'AVAILABLE'/);
});

test('products page preserves filter defaults and dialog safeguards', () => {
  assert.match(productsPage, /const statusFilter = ref\('AVAILABLE'\)/);
  assert.match(productsPage, /statusFilter\.value = 'AVAILABLE'/);
  assert.match(productsPage, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(productsPage, /event\.key === 'Escape'/);
  assert.match(productsPage, /event\.key !== 'Tab'/);
  assert.match(productsPage, /nextTick\(\(\) => previousFocus\?\.focus\(\)\)/);
});

test('products page provides at least 40px interactive controls', () => {
  assert.match(productsPage, /\.catalog-toolbar input,[^}]*min-height:40px/);
  assert.match(productsPage, /\.icon-action\{[^}]*width:40px[^}]*height:40px/);
});
