import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const dashboard = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('dashboard uses canonical operational KPIs and exception statuses', () => {
  assert.match(dashboard, /data\.customerCount/);
  assert.match(dashboard, /data\.activeProductCount/);
  assert.match(dashboard, /data\.value\.completionRate/);
  assert.match(dashboard, /DELIVERY_FAILED: 'Giao thất bại'/);
  assert.match(dashboard, /RETURNED_TO_STORE: 'Đã hoàn kho'/);
  assert.match(dashboard, /Khách hàng/);
  assert.match(dashboard, /Sản phẩm đang bán/);
});
