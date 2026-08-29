import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/admin/DashboardPage.vue', import.meta.url), 'utf8');

test('R3 dashboard presents five today KPIs and truthful incomplete profit', () => {
  for (const field of ['revenueToday', 'deliveredOrdersToday', 'activeOrdersToday', 'aovToday', 'grossProfitToday', 'costComplete']) assert.match(page, new RegExp(`data\\.${field}`));
  for (const label of ['Doanh thu hôm nay', 'Đơn đã giao', 'Đơn đang xử lý', 'Giá trị đơn trung bình', 'Lợi nhuận gộp']) assert.match(page, new RegExp(label));
  assert.match(page, /costComplete[^]*Chưa đủ dữ liệu|Chưa đủ dữ liệu[^]*costComplete/);
  assert.doesNotMatch(page, /Tổng doanh thu|Tổng đơn hàng|Khách hàng|Sản phẩm đang bán/);
});

test('R3 attention area maps six backend types to actionable destinations', () => {
  for (const type of ['OVERDUE_PENDING_ORDERS', 'DELIVERY_FAILED_ORDERS', 'PENDING_REFUNDS', 'STAFF_COVERAGE_GAPS', 'LOW_STOCK_ITEMS', 'PENDING_COD_SETTLEMENTS']) assert.match(page, new RegExp(type));
  for (const destination of ['/admin/orders', '/admin/refunds', '/admin/shifts', '/admin/inventory', '/admin/cod-settlements']) assert.match(page, new RegExp(destination.replaceAll('/', '\\/')));
  assert.match(page, /Cần chú ý/);
  assert.match(page, /attentionItems/);
});

test('R3 trend section keeps only revenue status and top product charts', () => {
  assert.match(page, /revenueChartRef/);
  assert.match(page, /statusChartRef/);
  assert.match(page, /topChartRef/);
  for (const label of ['Doanh thu gần đây', 'Trạng thái đơn', 'Món bán chạy']) assert.match(page, new RegExp(label));
});
