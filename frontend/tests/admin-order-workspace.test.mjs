import test from 'node:test';
import assert from 'node:assert/strict';
import {
  ORDER_SHORTCUTS,
  PRIMARY_ORDER_STATUSES,
  OTHER_ORDER_STATUSES,
  normalizeOrderStatus,
  isOtherOrderStatus,
  paymentMethodLabel,
  paymentStatusLabel,
  activeOrderFilterChips,
  inlineOrderActionMeta,
  inlineOrderActions,
} from '../src/utils/adminOrderWorkspace.js';

test('friendly shortcuts navigate without fabricated counts', () => {
  assert.deepEqual(ORDER_SHORTCUTS.map(({ key, label }) => ({ key, label })), [
    { key: 'ATTENTION', label: 'Cần xử lý' },
    { key: 'PREPARING', label: 'Đang chuẩn bị' },
    { key: 'PICKED_UP', label: 'Đang giao' },
    { key: 'DELIVERY_FAILED', label: 'Có vấn đề' },
  ]);
  for (const shortcut of ORDER_SHORTCUTS) assert.equal('count' in shortcut, false);
});

test('status navigation keeps primary values visible and secondary values in Khác', () => {
  assert.deepEqual(PRIMARY_ORDER_STATUSES.map(item => item.key), ['', 'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED']);
  assert.deepEqual(OTHER_ORDER_STATUSES.map(item => item.key), ['ASSIGNED', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'CANCELLED', 'REFUND_PENDING']);
  assert.equal(normalizeOrderStatus('ATTENTION'), 'ATTENTION');
  assert.equal(normalizeOrderStatus('REFUND_PENDING'), 'REFUND_PENDING');
  assert.equal(normalizeOrderStatus('INVALID'), '');
  assert.equal(normalizeOrderStatus(['PENDING']), '');
  assert.equal(isOtherOrderStatus('DELIVERY_FAILED'), true);
  assert.equal(isOtherOrderStatus('PENDING'), false);
});

test('payment labels never expose raw known enums', () => {
  assert.equal(paymentMethodLabel('BANK_TRANSFER'), 'PayOS');
  assert.equal(paymentMethodLabel('COD'), 'COD');
  assert.equal(paymentStatusLabel('PAID'), 'Đã thanh toán');
  assert.equal(paymentStatusLabel('UNPAID'), 'Chờ thanh toán');
  assert.equal(paymentStatusLabel('FAILED'), 'Thất bại');
  assert.equal(paymentStatusLabel(null), 'Chưa có trạng thái');
});

test('active filter chips omit defaults and use Vietnamese labels', () => {
  assert.deepEqual(activeOrderFilterChips({
    search: 'FG-12', paymentStatus: 'PAID', refundStatus: 'PENDING',
    sort: 'CREATED_DESC', fromDate: '2026-09-01', toDate: '2026-09-01',
    status: 'PENDING', page: '3',
  }), [
    { key: 'search', label: 'Tìm: FG-12' },
    { key: 'paymentStatus', label: 'Thanh toán: Đã thanh toán' },
    { key: 'refundStatus', label: 'Hoàn tiền: Chờ hoàn' },
    { key: 'sort', label: 'Sắp xếp: Mới nhất' },
    { key: 'fromDate', label: 'Từ: 2026-09-01' },
    { key: 'toDate', label: 'Đến: 2026-09-01' },
  ]);
  assert.deepEqual(activeOrderFilterChips({ sort: 'WAITING_DESC', status: 'ATTENTION', page: '2' }), []);
});

test('inline actions fail closed to approved contract-safe transitions', () => {
  assert.deepEqual(inlineOrderActions(['CONFIRMED', 'CANCELLED', 'ASSIGNED', 'DELIVERED', 'UNKNOWN']).map(action => action.key), ['CONFIRMED', 'CANCELLED']);
  assert.equal(inlineOrderActions(['CANCELLED'])[0].requiresNote, true);
  assert.equal(inlineOrderActionMeta('CANCELLED').label, 'Hủy đơn');
  assert.equal(inlineOrderActionMeta('ASSIGNED'), null);
  assert.deepEqual(inlineOrderActions(null), []);
});
