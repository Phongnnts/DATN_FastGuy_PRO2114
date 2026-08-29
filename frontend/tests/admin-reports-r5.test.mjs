import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const reports = readFileSync(new URL('../src/views/admin/ReportsPage.vue', import.meta.url), 'utf8');
const expenses = readFileSync(new URL('../src/views/admin/OperatingExpensesPage.vue', import.meta.url), 'utf8');
const menu = readFileSync(new URL('../src/views/admin/InventoryReportsPage.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/api/admin.js', import.meta.url), 'utf8');

test('R5 shares one inclusive period across all report tabs', () => {
  assert.match(reports, /const selectedRange = computed/);
  assert.match(reports, /<InventoryReportsPage :range="selectedRange"/);
  assert.match(reports, /<OperatingExpensesPage :range="selectedRange"/);
  assert.match(menu, /defineProps\(\{ range:/);
  assert.match(expenses, /defineProps\(\{ range:/);
});

test('R5 presents estimated operating result without depreciation in primary UI', () => {
  for (const field of ['netRevenue', 'cogs', 'storeExpenses', 'estimatedOperatingResult']) assert.match(reports, new RegExp(`operatingProfit\\.${field}`));
  assert.match(reports, /Kết quả vận hành ước tính/);
  assert.match(reports, /Chi phí cửa hàng/);
  assert.doesNotMatch(reports, /Lợi nhuận trước khấu hao \(mô phỏng\)|\['Khấu hao','depreciation'\]/);
});

test('R5 filters expenses by the shared period and labels manual salary', () => {
  assert.match(api, /getOperatingExpenses\(params\).*operating-expenses', \{ params \}/s);
  assert.match(expenses, /getOperatingExpenses\(props\.range\)/);
  assert.match(expenses, /SALARY.*Nhập tay/s);
  assert.match(expenses, /watch\(\(\) => props\.range/);
});
