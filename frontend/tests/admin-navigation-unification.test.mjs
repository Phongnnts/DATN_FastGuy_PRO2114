import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const layout = read('../src/layouts/AdminLayout.vue');
const router = read('../src/router/index.js');
const inventory = read('../src/views/admin/InventoryPage.vue');
const reports = read('../src/views/admin/ReportsPage.vue');

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
