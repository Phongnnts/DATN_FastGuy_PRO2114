import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { dashboardViewState } from '../src/utils/adminDashboardViewState.js';

const page = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('admin dashboard exposes out and low stock SKU counts with inventory actions', () => {
  assert.match(page, /outOfStockSkuCount/);
  assert.match(page, /lowStockSkuCount/);
  assert.match(page, /lowStockThreshold/);
  assert.match(page, /Hết hàng/);
  assert.match(page, /Sắp hết/);
  assert.match(page, /\/admin\/inventory/);
});

test('initial dashboard request blocks on loading without data', () => {
  assert.deepEqual(dashboardViewState(null, 'loading', ''), {
    showContent: false,
    showInitialLoading: true,
    banner: null,
  });
});

test('refresh retains dashboard content with nonblocking loading banner', () => {
  const data = { totalRevenue: 120000, pendingCodAmount: 50000, outOfStockSkuCount: 0 };
  assert.deepEqual(dashboardViewState(data, 'loading', ''), {
    showContent: true,
    showInitialLoading: false,
    banner: { role: 'status', message: 'Đang cập nhật tổng quan...' },
  });
});

test('refresh error retains valid dashboard with nonblocking error banner', () => {
  const data = { totalRevenue: 120000, lowStockSkuCount: 0, lowStockThreshold: 5 };
  assert.deepEqual(dashboardViewState(data, 'error', 'Mất kết nối'), {
    showContent: true,
    showInitialLoading: false,
    banner: { role: 'alert', message: 'Mất kết nối' },
  });
});

test('stock cards preserve distinct states zero values threshold and inventory destinations', () => {
  assert.match(page, /Hết hàng<strong>\{\{ Number\(data\.outOfStockSkuCount \|\| 0\) \}\} SKU/);
  assert.match(page, /Sắp hết<strong>\{\{ Number\(data\.lowStockSkuCount \|\| 0\) \}\} SKU/);
  assert.match(page, /filter: 'OUT'/);
  assert.match(page, /filter: 'LOW'/);
  assert.match(page, /Ngưỡng ≤ \{\{ data\.lowStockThreshold \}\}/);
});

test('dashboard keeps COD and revenue production fields', () => {
  assert.match(page, /data\.revenueToday/);
  assert.match(page, /data\.totalRevenue/);
  assert.match(page, /data\.pendingCodAmount/);
  assert.match(page, /data\.pendingCodCount/);
  assert.match(page, /\/admin\/cod-settlements/);
});

test('stock icons are decorative and responsive source policy remains', () => {
  assert.match(page, /bi bi-x-octagon" aria-hidden="true"/);
  assert.match(page, /bi bi-exclamation-triangle" aria-hidden="true"/);
  assert.match(page, /repeat\(auto-fit,minmax\(180px,1fr\)\)/);
  assert.match(page, /@media\(max-width:760px\).*\.operation-strip\{grid-template-columns:1fr\}/s);
});

test('admin stock alerts remain current response UI only', () => {
  assert.doesNotMatch(page, /Notification|markRead|notify/);
});
