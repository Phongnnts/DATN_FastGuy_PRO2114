import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const read = (p) => readFileSync(join(root, p), 'utf8');

const api = read('src/api/admin.js');
const router = read('src/router/index.js');
const layout = read('src/layouts/AdminLayout.vue');
const page = read('src/views/admin/InventoryLedgerPage.vue');
const inventory = read('src/views/admin/InventoryPage.vue');

test('admin API exposes inventory transactions endpoint forwarding params', () => {
  assert.match(api, /getInventoryTransactions\s*\(\s*params\s*\)\s*\{\s*return client\.get\('\/admin\/inventory\/transactions',\s*\{\s*params\s*\}\);\s*\}/);
});

test('ledger route is registered with page and title', () => {
  assert.match(router, /path: 'inventory\/ledger'[\s\S]*name: 'AdminInventoryLedger'[\s\S]*InventoryLedgerPage\.vue/);
  assert.match(router, /AdminInventoryLedger: 'Sổ tồn kho'/);
});

test('sidebar keeps Tồn kho overview and adds Sổ tồn kho link', () => {
  assert.match(layout, /label: 'Tồn kho', path: '\/admin\/inventory'/);
  assert.match(layout, /label: 'Sổ tồn kho', path: '\/admin\/inventory\/ledger'/);
});

test('ledger whitelists transaction types and builds filter params', () => {
  assert.match(page, /TRANSACTION_TYPES\s*=\s*\[\s*'RESERVE'\s*,\s*'RELEASE'\s*,\s*'CONSUME'\s*,\s*'WASTE'\s*,\s*'ADJUSTMENT'\s*\]/);
  assert.match(page, /const params = \{ page: page\.value, size: size\.value \};/);
  assert.match(page, /if \(variantId\.value\.trim\(\)\) params\.variantId/);
  assert.match(page, /if \(productId\.value\.trim\(\)\) params\.productId/);
  assert.match(page, /if \(transactionType\.value\) params\.transactionType/);
  assert.match(page, /if \(fromDate\.value\) params\.fromDate/);
  assert.match(page, /if \(toDate\.value\) params\.toDate/);
  assert.match(page, /<option v-for="t in TRANSACTION_TYPES" :key="t" :value="t">/);
  assert.match(page, /dateError/);
  assert.match(page, /field-error/);
});

test('ledger validates date range and prevents invalid load', () => {
  assert.match(page, /Từ ngày không được sau đến ngày\./);
  assert.match(page, /if \(dateError\.value\) return;/);
  assert.match(page, /:max="toDate \|\| undefined"/);
  assert.match(page, /:min="fromDate \|\| undefined"/);
});

test('ledger paginates with total and totalPages plus size select', () => {
  assert.match(page, /totalPages = computed\(\(\) => Math\.max\(1, Math\.ceil\(total\.value \/ size\.value\)\)\)/);
  assert.match(page, /total\.value = Number\(data\?\.total\) \|\| 0/);
  assert.match(page, /SIZE_OPTIONS = \[20, 50, 100\]/);
  assert.match(page, /@change="changeSize"/);
  assert.match(page, /@click="goTo\(page - 1\)"/);
  assert.match(page, /@click="goTo\(page \+ 1\)"/);
  assert.match(page, /Trang {{ page }} \/ {{ totalPages }}/);
  assert.match(page, /Hiển thị {{ rangeStart }}–{{ rangeEnd }} \/ {{ total }} giao dịch/);
});

test('ledger resets to first page on filter or size change', () => {
  assert.match(page, /function applyFilters\(\)\s*\{\s*page\.value = 1;\s*load\(\);\s*\}/);
  assert.match(page, /function changeSize\(\)\s*\{\s*page\.value = 1;\s*load\(\);\s*\}/);
  assert.match(page, /function goTo\(target\)[\s\S]*if \(target < 1 \|\| target > totalPages\.value\) return;/);
});

test('ledger renders loading, error with retry, and empty states', () => {
  assert.match(page, /v-if="loading" class="state" role="status"/);
  assert.match(page, /v-else-if="loadError" class="state error" role="alert"/);
  assert.match(page, /@click="load">Thử lại<\/button>/);
  assert.match(page, /Không có giao dịch/);
  assert.match(page, /@click="resetFilters">Đặt lại bộ lọc<\/button>/);
  assert.match(page, /@click="applyFilters">Áp dụng<\/button>/);
});

test('ledger guards stale requests and cleans up on unmount', () => {
  assert.match(page, /let loadGeneration = 0;/);
  assert.match(page, /let stopped = false;/);
  assert.match(page, /\+\+loadGeneration/);
  assert.match(page, /if \(stopped \|\| request\.generation !== loadGeneration\) return;/);
  assert.match(page, /if \(request\.generation === loadGeneration\) \{\s*loading\.value = false;/);
  assert.match(page, /onBeforeUnmount\(\(\) => \{\s*stopped = true;\s*\}\)/);
});

test('ledger clamps page to totalPages and reloads once without loop', () => {
  assert.match(page, /let clampingPage = false;/);
  assert.match(page, /if \(page\.value > totalPages\.value\) \{/);
  assert.match(page, /if \(clampingPage\) return;/);
  assert.match(page, /clampingPage = true;/);
  assert.match(page, /page\.value = totalPages\.value;/);
  assert.match(page, /clampingPage = false;/);
  const clampBranch = page.slice(page.indexOf('if (page.value > totalPages.value)'), page.indexOf("} catch (e)"));
  assert.ok(clampBranch.indexOf('clampingPage = true') < clampBranch.indexOf('page.value = totalPages.value'));
  assert.ok(clampBranch.indexOf('page.value = totalPages.value') < clampBranch.indexOf('load()'));
  assert.doesNotMatch(clampBranch, /while|for\s*\(/);
});

test('ledger displays raw ISO createdAt and links order rows', () => {
  assert.match(page, /<time :datetime="row\.createdAt">\{\{ row\.createdAt \}\}<\/time>/);
  assert.match(page, /Thời gian<\/th>/);
  assert.match(page, /v-if="row\.orderId" class="order-link"/);
  assert.match(page, /\{\{ row\.orderCode \|\| row\.orderId \}\}/);
});

test('ledger shows per-type KPI cards and responsive card table', () => {
  assert.match(page, /class="stats"/);
  assert.match(page, /TYPE_LABELS\.RESERVE/);
  assert.match(page, /kpi\.RESERVE/);
  assert.match(page, /kpi\.RELEASE/);
  assert.match(page, /kpi\.CONSUME/);
  assert.match(page, /kpi\.WASTE/);
  assert.match(page, /typeClass\(row\.type\)/);
  assert.match(page, /class="table-wrapper"/);
  assert.match(page, /data-label="Thời gian"/);
  assert.match(page, /@media \(max-width: 900px\)[\s\S]*\.table thead \{ display: none; \}/);
  assert.match(page, /\.table td::before \{ content: attr\(data-label\)/);
});

test('inventory overview keeps read controls and links audited stock workflows', () => {
  assert.match(inventory, /Quản lý tồn kho/);
  assert.match(inventory, /adminStore\.fetchProducts\(\)/);
  assert.doesNotMatch(inventory, /saveStock|draftStock/);
  assert.match(inventory, /openAdjust/);
  assert.match(inventory, /openWaste/);
  assert.match(inventory, /router\.push\(\{ name: 'AdminInventoryLedger' \}\)/);
});
