import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/staff/OrderHistoryPage.vue', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/staff.js', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/api/staff.js', import.meta.url), 'utf8');

test('staff history uses server filters and pagination metadata', () => {
  assert.match(page, /statusFilter/);
  assert.match(page, /fromDate/);
  assert.match(page, /toDate/);
  assert.match(page, /searchTerm/);
  assert.match(page, /totalPages/);
  assert.match(page, /function applyFilters/);
  assert.match(page, /function goTo\(target\)/);
  assert.match(page, /Trang \{\{ page \}\} \/ \{\{ totalPages \}\}/);
  assert.match(store, /historyTotal/);
  assert.match(store, /data\?\.items/);
  assert.match(api, /getOrderHistory\(params\)/);
});

test('staff history exports current filters and renders resilient states', () => {
  assert.match(page, /staffApi\.exportOrders\(filterParams/);
  assert.match(page, /Đang tải lịch sử/);
  assert.match(page, /role="alert"/);
  assert.match(page, /Thử lại/);
  assert.match(page, /Không tìm thấy đơn hàng/);
  assert.match(page, /@media\(max-width:768px\)/);
});
