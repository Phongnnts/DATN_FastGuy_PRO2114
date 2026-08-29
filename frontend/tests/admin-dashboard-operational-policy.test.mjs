import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const dashboard = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('dashboard uses canonical today KPIs and exception statuses', () => {
  for (const field of ['revenueToday','deliveredOrdersToday','activeOrdersToday','aovToday','grossProfitToday']) assert.match(dashboard, new RegExp(`data\\.${field}`));
  assert.match(dashboard, /DELIVERY_FAILED: 'Giao thất bại'/);
  assert.match(dashboard, /RETURNED_TO_STORE: 'Đã hoàn kho'/);
  assert.match(dashboard, /Cần chú ý/);
});
