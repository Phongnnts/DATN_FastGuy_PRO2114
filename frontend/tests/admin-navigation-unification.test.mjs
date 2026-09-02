import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const layout = read('../src/layouts/AdminLayout.vue');
const router = read('../src/router/index.js');
const inventory = read('../src/views/admin/InventoryPage.vue');
const reports = read('../src/views/admin/ReportsPage.vue');
const codSettlements = read('../src/views/admin/CodSettlementsPage.vue');
const variables = read('../src/assets/styles/variables.css');
const globalStyles = read('../src/assets/styles/global.css');

function relativeLuminance(hex) {
  const channels = hex.match(/[\da-f]{2}/gi).map(channel => Number.parseInt(channel, 16) / 255);
  const [red, green, blue] = channels.map(channel => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4);
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function contrastRatio(foreground, background) {
  const lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
  const darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
  return (lighter + 0.05) / (darker + 0.05);
}

const expectedGroups = {
  'Vận hành': ['/admin', '/admin/orders'],
  'Kho': ['/admin/inventory', '/admin/inventory/receipts', '/admin/recipes', '/admin/inventory/stock-counts'],
  'Tài chính': ['/admin/cod-settlements', '/admin/refunds', '/admin/reports'],
  'Marketing': ['/admin/products', '/admin/categories', '/admin/coupons', '/admin/banners'],
  'Nhân sự': ['/admin/hr', '/admin/users', '/admin/shifts', '/admin/attendance'],
  'Cấu hình': ['/admin/activity-logs', '/admin/settings'],
};

test('admin sidebar places every destination exactly once in the six approved business groups', () => {
  let previousGroupIndex = -1;
  const allPaths = Object.values(expectedGroups).flat();

  for (const [group, paths] of Object.entries(expectedGroups)) {
    const groupIndex = layout.indexOf(`label: '${group}'`);
    assert.ok(groupIndex > previousGroupIndex, `${group} must follow the approved group order`);
    previousGroupIndex = groupIndex;
    for (const path of paths) {
      assert.equal(layout.match(new RegExp(`path: '${path.replaceAll('/', '\\/')}'`, 'g'))?.length, 1, `${path} must appear exactly once`);
    }
  }

  assert.equal(layout.match(/\{ label: '[^']+', links:/g)?.length, 6);
  assert.equal(allPaths.length, 19);
  assert.match(layout, /class="page-content fg-page"/);
});

test('inventory unifies current stock and movement history with navigable accessible tabs', () => {
  assert.match(inventory, /InventoryLedgerPage/);
  assert.match(inventory, /Tồn hiện tại/);
  assert.match(inventory, /Lịch sử biến động/);
  assert.match(inventory, /route\.query\.tab/);
  assert.match(inventory, /router\.push/);
  assert.doesNotMatch(inventory, /router\.replace/);
  assert.match(inventory, /function handleTabKeydown/);
  assert.match(inventory, /ArrowLeft/);
  assert.match(inventory, /ArrowRight/);
  assert.match(inventory, /role="tabpanel"/);
  assert.match(inventory, /id="inventory-current-panel"/);
  assert.match(inventory, /id="inventory-history-panel"/);
});

test('business reports unify overview menu performance and store expenses with tab-owned actions', () => {
  assert.match(reports, /InventoryReportsPage/);
  assert.match(reports, /OperatingExpensesPage/);
  for (const tab of ['Tổng quan', 'Hiệu quả món', 'Chi phí']) assert.match(reports, new RegExp(tab));
  assert.match(reports, /route\.query\.tab/);
  assert.match(reports, /router\.push/);
  assert.doesNotMatch(reports, /router\.replace/);
  assert.match(reports, /function handleTabKeydown/);
  assert.match(reports, /v-if="activeTab === 'overview'" class="head-actions"/);
  assert.match(reports, /role="tabpanel"/);
  assert.match(reports, /id="report-overview-panel"/);
  assert.match(reports, /id="report-menu-panel"/);
  assert.match(reports, /id="report-expenses-panel"/);
});

test('legacy routes redirect to unified query tabs while fixed assets remain addressable', () => {
  assert.match(router, /path: 'inventory\/ledger'[\s\S]*redirect:[\s\S]*tab: 'history'/);
  assert.match(router, /path: 'inventory\/reports'[\s\S]*redirect:[\s\S]*tab: 'menu'/);
  assert.match(router, /path: 'operating-expenses'[\s\S]*redirect:[\s\S]*tab: 'expenses'/);
  assert.match(router, /path: 'fixed-assets'[\s\S]*FixedAssetsPage\.vue/);
});

test('admin shell exposes the approved semantic palette through scoped aliases', () => {
  const expectedTokens = {
    '--admin-canvas': '#f4f5f7',
    '--admin-surface': '#fcfcfd',
    '--admin-surface-subtle': '#f7f8fa',
    '--admin-foreground': '#2b3038',
    '--admin-muted': '#7d838c',
    '--admin-subtle': '#9a9fa7',
    '--admin-brand': '#f45b2a',
    '--admin-brand-dark': '#C43F16',
    '--admin-brand-soft': '#FFF0EA',
    '--admin-info': '#635C96',
    '--admin-success': '#16845B',
    '--admin-warning': '#946000',
    '--admin-danger': '#C74848',
  };

  for (const [token, value] of Object.entries(expectedTokens)) {
    assert.match(variables, new RegExp(`${token}: ${value};`));
  }

  assert.match(globalStyles, /\.fg-shell-admin\s*\{[^}]*--bg:\s*var\(--admin-canvas\);[^}]*--bg-card:\s*var\(--admin-surface\);[^}]*--text-dark:\s*var\(--admin-foreground\);[^}]*--text-mid:\s*var\(--admin-muted\);[^}]*--border:\s*var\(--admin-border\);[^}]*--role-accent:\s*var\(--admin-brand\);[^}]*--role-soft:\s*var\(--admin-brand-soft\);[^}]*--radius-sm:\s*8px;[^}]*--radius:\s*10px;[^}]*--radius-lg:\s*12px;/s);
});

test('admin text-bearing primary controls use a scoped WCAG AA orange pairing', () => {
  const action = variables.match(/--admin-action:\s*(#[\dA-F]{6});/i)?.[1];
  assert.ok(action, 'missing --admin-action');
  assert.ok(contrastRatio('#FFFFFF', action) >= 4.5, `${action} must have at least 4.5:1 contrast with white`);
  assert.match(variables, /--admin-brand:\s*#f45b2a;/);
  assert.match(globalStyles, /\.fg-shell-admin :is\(\.btn-primary, \.verify-button\)\s*\{[^}]*background:\s*var\(--admin-action\);[^}]*border-color:\s*var\(--admin-action\);/s);
  assert.match(codSettlements, /\.verify-button\{[^}]*color:#fff;[^}]*background:var\(--admin-action\)/);
  assert.doesNotMatch(codSettlements, /\.verify-button\{[^}]*background:var\(--role-admin\)/);
});

test('admin visual foundation uses semantic surfaces and accessible active navigation', () => {
  assert.match(layout, /\.fg-shell-admin :deep\(\.sidebar\)\{[^}]*height:calc\(100vh - 24px\);[^}]*border-radius:20px;[^}]*background:linear-gradient\(155deg,var\(--admin-sidebar-highlight\),var\(--admin-sidebar\)\)/);
  assert.match(layout, /\.sidebar\{width:232px\}/);
  assert.match(layout, /\.main-content\{min-width:0;margin-left:244px;[^}]*border-radius:20px/);
  assert.match(layout, /\.sidebar-brand\{[^}]*border-bottom:0/);
  assert.match(layout, /\.topbar\{height:56px;[^}]*border-bottom:1px solid var\(--admin-hairline\);[^}]*background:rgba\(252,252,253,\.92\);[^}]*box-shadow:none/);
  assert.match(layout, /\.page-content\{width:100%;max-width:1440px;margin-inline:auto;background:var\(--admin-canvas\)\}/);
  assert.match(layout, /\.sidebar-nav a\.router-link-active\{[^}]*color:var\(--admin-foreground\);background:var\(--admin-sidebar-active\);[^}]*font-weight:720\}/);
  assert.match(layout, /\.sidebar-nav a\.router-link-active i\{color:var\(--admin-brand\)\}/);
  assert.match(layout, /\.sidebar-nav a\.router-link-active::before\{content:none\}/);
  assert.doesNotMatch(layout, /transition:\s*all/);
});

test('admin visual foundation uses tabular metrics and flat semantic foundations', () => {
  assert.match(globalStyles, /\.fg-shell-admin\s+:is\(\.stat-value,\s*\.fg-metric-card\s+strong,\s*\.today-kpis\s+strong,\s*\.stats\s+strong,\s*\.stats-grid\s+strong,\s*\.kpi-grid\s+strong,\s*\.receipt-kpis\s+strong,\s*td\.numeric,\s*\.numeric\)\s*\{\s*font-variant-numeric:\s*tabular-nums;\s*\}/);
  assert.match(globalStyles, /\.fg-shell-admin\.fg-shell\s*\{\s*background:\s*var\(--admin-canvas\);\s*\}/);
  assert.match(globalStyles, /\.fg-shell-admin\s+\.fg-panel\s*\{[^}]*background:\s*var\(--admin-surface\);[^}]*border:\s*1px solid var\(--admin-border\);[^}]*border-radius:\s*var\(--radius-lg\);[^}]*box-shadow:\s*none;/s);
});
