import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => { try { return readFileSync(new URL(path, import.meta.url), 'utf8'); } catch { return ''; } };
const api = read('../src/api/admin.js');
const router = read('../src/router/index.js');
const layout = read('../src/layouts/AdminLayout.vue');
const page = read('../src/views/admin/ActivityLogsPage.vue');

const actions = {
  ORDER_CANCELLED: 'Hủy đơn hàng',
  ORDER_REFUND_RECORDED: 'Ghi nhận hoàn tiền',
  DELIVERY_ATTEMPT_OVERRIDDEN: 'Ghi đè lần giao hàng',
  ATTENDANCE_APPROVED: 'Duyệt chấm công',
  STAFF_PAY_RATE_CREATED: 'Tạo mức lương nhân viên',
  STOCK_COUNT_APPROVED: 'Duyệt kiểm kê kho',
};

test('R7 exposes the contract API, admin route, title, and sidebar destination', () => {
  assert.match(api, /getActivityLogs\(params\).*activity-logs', \{ params \}/s);
  assert.match(router, /path: 'activity-logs'[\s\S]*name: 'AdminActivityLogs'[\s\S]*ActivityLogsPage\.vue/);
  assert.match(router, /AdminActivityLogs: 'Nhật ký hoạt động'/);
  assert.match(layout, /label: 'Nhật ký hoạt động', path: '\/admin\/activity-logs'/);
});

test('R7 activity log page provides exact action labels and contract filters', () => {
  for (const [action, label] of Object.entries(actions)) {
    assert.match(page, new RegExp(`${action}: '${label}'`));
  }
  assert.equal((page.match(/^\s{2}[A-Z_]+: '/gm) || []).length, 6);
  for (const token of ['from', 'to', 'actionType', 'actorUserId', 'page', 'pageSize']) assert.match(page, new RegExp(token));
  assert.match(page, /getActivityLogs\(params\)/);
});

test('R7 activity log page handles server pagination, stale requests, states, safe metadata, and mobile layout', () => {
  for (const token of ['pagination.totalPages', 'pagination.totalItems', 'loadGeneration', 'Đang tải nhật ký', 'Không thể tải nhật ký hoạt động', 'Không có nhật ký phù hợp']) assert.match(page, new RegExp(token));
  assert.match(page, /Object\.entries\(metadata/);
  assert.doesNotMatch(page, /v-html/);
  assert.match(page, /createdAt/);
  assert.match(page, /@media \(max-width: 768px\)/);
});
