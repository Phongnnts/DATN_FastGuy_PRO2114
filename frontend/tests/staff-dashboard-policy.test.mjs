import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const dashboard = readFileSync(new URL('../src/views/staff/DashboardPage.vue', import.meta.url), 'utf8');

test('staff dashboard renders realtime shift operations and shift report', () => {
  assert.match(dashboard, /shiftCompletedOrders/);
  assert.match(dashboard, /shiftFailedOrders/);
  assert.match(dashboard, /shiftNetRevenue/);
  assert.match(dashboard, /priorityOrders/);
  assert.match(dashboard, /Đơn hoàn thành trong ca/);
  assert.match(dashboard, /Đơn thất bại trong ca/);
  assert.match(dashboard, /Doanh thu thuần trong ca/);
  assert.match(dashboard, /Đơn cần ưu tiên/);
  assert.match(dashboard, /waitingDuration/);
  assert.match(dashboard, /formatPrice/);
  assert.match(dashboard, /setInterval\([^]*30000\)/);
});

test('staff dashboard refreshes silently and exposes accessible states', () => {
  assert.match(dashboard, /async function load\(\{ silent = false \} = \{\}\)/);
  assert.match(dashboard, /aria-live="polite"/);
  assert.match(dashboard, /role="alert"/);
  assert.match(dashboard, /lastUpdated/);
  assert.match(dashboard, /@media\(max-width:768px\)/);
  assert.match(dashboard, /prefers-reduced-motion:reduce/);
});
