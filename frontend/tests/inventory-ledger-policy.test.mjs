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

test('admin API exposes inventory transactions endpoint forwarding params', () => {
  assert.match(api, /getInventoryTransactions\s*\(\s*params\s*\)\s*\{\s*return client\.get\('\/admin\/inventory\/transactions',\s*\{\s*params\s*\}\);\s*\}/);
});

test('ledger route is registered with page and title', () => {
  assert.match(router, /path: 'inventory\/ledger'[\s\S]*name: 'AdminInventoryLedger'[\s\S]*InventoryLedgerPage\.vue/);
  assert.match(router, /AdminInventoryLedger: 'Sổ tồn kho'/);
});

test('sidebar keeps grouped inventory overview, recipes and report history entry', () => {
  assert.match(layout, /Quản lý kho/);
  assert.match(layout, /label: 'Tổng quan', path: '\/admin\/inventory'/);
  assert.match(layout, /label: 'Công thức món', path: '\/admin\/recipes'/);
  assert.match(layout, /label: 'Báo cáo & lịch sử', path: '\/admin\/inventory\/reports'/);
});

test('ledger whitelists contracted transaction types and builds item filters', () => {
  assert.match(page, /TRANSACTION_TYPES = \['RECEIPT', 'RESERVE', 'RELEASE', 'CONSUME', 'ADJUSTMENT', 'WASTE', 'RETURN'\]/);
  assert.match(page, /const params = \{ page: page\.value - 1, size: size\.value \};/);
  assert.match(page, /if \(inventoryItemId\.value\.trim\(\)\) params\.inventoryItemId/);
  assert.match(page, /if \(orderId\.value\.trim\(\)\) params\.orderId/);
  assert.match(page, /if \(transactionType\.value\) params\.transactionType/);
  assert.match(page, /if \(fromDate\.value\) params\.fromDate/);
  assert.match(page, /if \(toDate\.value\) params\.toDate/);
  assert.doesNotMatch(page, /params\.variantId|params\.productId/);
});

test('ledger validates date range and prevents invalid load', () => {
  assert.match(page, /Từ ngày không được sau đến ngày\./);
  assert.match(page, /if \(dateError\.value\) return;/);
  assert.match(page, /:max="toDate \|\| undefined"/);
  assert.match(page, /:min="fromDate \|\| undefined"/);
});

test('ledger paginates with totalItems and size select', () => {
  assert.match(page, /totalPages = computed\(\(\) => Math\.max\(1, Math\.ceil\(total\.value \/ size\.value\)\)\)/);
  assert.match(page, /Number\(data\?\.totalItems\) \|\| 0/);
  assert.match(page, /SIZE_OPTIONS = \[20, 50, 100\]/);
  assert.match(page, /@change="changeSize"/);
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
});

test('ledger displays raw ISO createdAt and links order rows by id', () => {
  assert.match(page, /<time :datetime="row\.createdAt">\{\{ row\.createdAt \}\}<\/time>/);
  assert.match(page, /Thời gian<\/th>/);
  assert.match(page, /v-if="row\.orderId" class="order-link"/);
  assert.doesNotMatch(page, /orderCode|createdByName|variantName|productName/);
});

test('ledger shows per-type KPI cards and responsive card table', () => {
  assert.match(page, /class="stats"/);
  assert.match(page, /kpi\[type\]/);
  assert.match(page, /typeClass\(row\.transactionType\)/);
  assert.match(page, /class="table-wrapper"/);
  assert.match(page, /data-label="Thời gian"/);
  assert.match(page, /\.table td::before \{ content: attr\(data-label\)/);
});
