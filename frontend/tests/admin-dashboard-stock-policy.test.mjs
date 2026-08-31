import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { dashboardViewState } from '../src/utils/adminDashboardViewState.js';

const page = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('admin dashboard exposes consolidated stock attention with inventory action', () => {
  assert.match(page, /LOW_STOCK_ITEMS/);
  assert.match(page, /Mặt hàng dưới mức an toàn/);
  assert.match(page, /\/admin\/inventory/);
});

test('initial dashboard request blocks on loading without data', () => {
  assert.equal(dashboardViewState(null, 'loading', ''), 'loading');
});

test('refresh retains dashboard content in refreshing state', () => {
  const data = { totalRevenue: 120000, pendingCodAmount: 50000, outOfStockSkuCount: 0 };
  assert.equal(dashboardViewState(data, 'loading', ''), 'refreshing');
});

test('refresh error retains valid dashboard in ready state', () => {
  const data = { totalRevenue: 120000, lowStockSkuCount: 0, lowStockThreshold: 5 };
  assert.equal(dashboardViewState(data, 'error', 'Mất kết nối'), 'ready');
});

test('stock risk is one actionable attention item', () => {
  assert.match(page, /LOW_STOCK_ITEMS/);
  assert.match(page, /Mặt hàng dưới mức an toàn/);
  assert.match(page, /filter: 'LOW'/);
});

test('dashboard keeps canonical today revenue and COD attention destinations', () => {
  assert.match(page, /data\.netCashRevenueToday/);
  assert.doesNotMatch(page, /data\.revenueToday/);
  assert.match(page, /PENDING_COD_SETTLEMENTS/);
  assert.match(page, /\/admin\/cod-settlements/);
});

test('attention icons are decorative and responsive source policy remains', () => {
  assert.match(page, /bi bi-exclamation-circle" aria-hidden="true"/);
  assert.match(page, /repeat\(6,minmax\(0,1fr\)\)/);
  assert.match(page, /@media\(max-width:760px\).*\.attention-list\{grid-template-columns:1fr\}/s);
});

test('admin stock alerts remain current response UI only', () => {
  assert.doesNotMatch(page, /Notification|markRead|notify/);
});
