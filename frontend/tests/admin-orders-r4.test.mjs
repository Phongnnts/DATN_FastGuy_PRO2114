import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const page = readFileSync(new URL('../src/views/admin/OrdersPage.vue', import.meta.url), 'utf8');
const dashboard = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/admin.js', import.meta.url), 'utf8');

test('R4 exposes backend-defined attention tab and reason badges', () => {
  assert.match(page, /route\.query\.status/);
  assert.match(page, /key:\s*'ATTENTION'/);
  assert.match(page, /attentionOnly/);
  for (const reason of ['PROCESSING_OVERDUE', 'DELIVERY_FAILED', 'PENDING_REFUND']) assert.match(page, new RegExp(reason));
  assert.match(page, /Quá hạn xử lý/);
  assert.match(page, /Giao thất bại/);
  assert.match(page, /Chờ hoàn tiền/);
});

test('R4 attention requests ignore date controls and reject stale responses', () => {
  assert.match(store, /ordersRequestGeneration/);
  assert.match(store, /requestGeneration !== ordersRequestGeneration/);
  assert.match(page, /attentionOnly[^\n]*true/);
  assert.match(page, /:disabled=".*attention/i);
});

test('dashboard order attention links open the unified attention tab', () => {
  assert.match(dashboard, /OVERDUE_PENDING_ORDERS[^\n]*status:\s*'ATTENTION'/);
  assert.match(dashboard, /DELIVERY_FAILED_ORDERS[^\n]*status:\s*'ATTENTION'/);
  assert.match(dashboard, /PENDING_REFUNDS[^\n]*\/admin\/refunds/);
});
