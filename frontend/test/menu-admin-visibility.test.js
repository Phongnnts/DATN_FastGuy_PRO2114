import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const menu = read('../src/views/guest/MenuPage.vue');
const layout = read('../src/layouts/AdminLayout.vue');
const router = read('../src/router/index.js');
const settings = read('../src/views/admin/SettingsPage.vue');

test('menu renders all categories directly and exposes exactly four truthful quick filters', () => {
  assert.match(menu, /v-for="item in categories"/);
  assert.doesNotMatch(menu, /primaryCategories|overflowCategories|>Thêm\s/);
  for (const label of ['Bán chạy', 'Đang giảm giá', 'Dưới 40K', 'Còn hàng']) assert.match(menu, new RegExp(`label: '${label}'`));
  assert.doesNotMatch(menu, /officeCombo|studentCombo|Combo văn phòng|Combo sinh viên/);
});

test('menu delegates drawer URL chip and reset consistency to executable filter state helpers', () => {
  assert.match(menu, /key: 'available'/);
  for (const helper of ['createEffectiveMenuFilterDraft', 'applyEffectiveMenuFilterDraft', 'hydrateMenuFilterState', 'removeMenuFilter', 'resetMenuFilters']) assert.match(menu, new RegExp(helper));
});

test('admin activity logs are absent from navigation and routes', () => {
  assert.doesNotMatch(layout, /Nhật ký hoạt động|\/admin\/activity-logs/);
  assert.doesNotMatch(router, /AdminActivityLogs|path: 'activity-logs'|ActivityLogsPage/);
});

test('settings hides fees and delivery while preserving remaining tabs', () => {
  assert.doesNotMatch(settings, /\{ id: 'fees'|\{ id: 'delivery'/);
  for (const id of ['store', 'hours', 'inventory', 'notice', 'payment', 'ghn']) assert.match(settings, new RegExp(`\\{ id: '${id}'`));
});
