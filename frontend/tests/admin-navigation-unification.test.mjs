import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const layout = read('../src/layouts/AdminLayout.vue');
const router = read('../src/router/index.js');
const inventory = read('../src/views/admin/InventoryPage.vue');
const reports = read('../src/views/admin/ReportsPage.vue');
const variables = read('../src/assets/styles/variables.css');
const globalStyles = read('../src/assets/styles/global.css');

const expectedGroups = {
  'Tổng quan': ['Dashboard'],
  'Vận hành': ['Đơn hàng', 'Đối soát COD', 'Hoàn tiền'],
  'Bán hàng': ['Sản phẩm', 'Danh mục', 'Mã giảm giá', 'Banner'],
  'Nhân sự': ['Người dùng', 'Ca làm'],
  'Kho hàng': ['Tồn kho', 'Nhập hàng', 'Công thức & định mức'],
  'Báo cáo': ['Báo cáo kinh doanh'],
  'Hệ thống': ['Cài đặt'],
};

test('admin sidebar follows task-based navigation groups', () => {
  for (const [group, labels] of Object.entries(expectedGroups)) {
    assert.match(layout, new RegExp(`label: '${group}'`));
    for (const label of labels) assert.match(layout, new RegExp(`label: '${label}'`));
  }
  assert.doesNotMatch(layout, /label: 'Tài sản cố định'|label: 'Lịch sử kho'|label: 'Báo cáo theo món'|label: 'Chi phí vận hành'/);
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
    '--admin-canvas': '#F5F6F7',
    '--admin-surface': '#FFFFFF',
    '--admin-foreground': '#17212B',
    '--admin-muted': '#66717D',
    '--admin-border': '#DDE2E7',
    '--admin-brand': '#D85F32',
    '--admin-brand-soft': '#FFF0E9',
    '--admin-info': '#2764C8',
    '--admin-success': '#267A50',
    '--admin-warning': '#A35C00',
    '--admin-danger': '#B42318',
  };

  for (const [token, value] of Object.entries(expectedTokens)) {
    assert.match(variables, new RegExp(`${token}: ${value};`));
  }

  assert.match(globalStyles, /\.fg-shell-admin\s*\{[^}]*--bg:\s*var\(--admin-canvas\);[^}]*--bg-card:\s*var\(--admin-surface\);[^}]*--text-dark:\s*var\(--admin-foreground\);[^}]*--text-mid:\s*var\(--admin-muted\);[^}]*--border:\s*var\(--admin-border\);[^}]*--role-accent:\s*var\(--admin-brand\);[^}]*--role-soft:\s*var\(--admin-brand-soft\);[^}]*--radius-sm:\s*8px;[^}]*--radius:\s*10px;[^}]*--radius-lg:\s*12px;/s);
});

test('admin visual foundation uses flat active navigation and tabular metrics', () => {
  assert.match(layout, /\.sidebar-nav a\.router-link-active\{[^}]*background:var\(--admin-brand\);[^}]*box-shadow:none[^}]*\}/);
  assert.doesNotMatch(layout, /\.sidebar-nav a\.router-link-active\{[^}]*(?:linear-gradient|box-shadow:(?!none))/);
  assert.match(globalStyles, /\.fg-shell-admin\s+:is\(\.stat-value,\s*\.fg-metric-card\s+strong,\s*\.today-kpis\s+strong,\s*\.stats\s+strong,\s*\.stats-grid\s+strong,\s*\.kpi-grid\s+strong,\s*\.receipt-kpis\s+strong,\s*td\.numeric,\s*\.numeric\)\s*\{\s*font-variant-numeric:\s*tabular-nums;\s*\}/);
  assert.match(globalStyles, /\.fg-shell-admin\.fg-shell\s*\{\s*background:\s*var\(--admin-canvas\);\s*\}/);
  assert.match(globalStyles, /\.fg-shell-admin\s+\.fg-panel\s*\{[^}]*background:\s*var\(--admin-surface\);[^}]*border:\s*1px solid var\(--admin-border\);[^}]*border-radius:\s*var\(--radius-lg\);[^}]*box-shadow:\s*none;/s);
});
