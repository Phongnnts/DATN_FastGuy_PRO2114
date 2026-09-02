import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  PRIMARY_ORDER_STATUSES,
  OTHER_ORDER_STATUSES,
  normalizeOrderStatus,
  parseOrderIdQuery,
  isOtherOrderStatus,
  paymentMethodLabel,
  paymentStatusLabel,
  activeOrderFilterChips,
  inlineOrderActionMeta,
  inlineOrderActions,
} from '../src/utils/adminOrderWorkspace.js';

test('status navigation keeps primary values visible and secondary values in Khác', () => {
  assert.deepEqual(PRIMARY_ORDER_STATUSES.map(item => item.key), ['', 'ATTENTION', 'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED']);
  assert.deepEqual(OTHER_ORDER_STATUSES.map(item => item.key), ['ASSIGNED', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'CANCELLED', 'REFUND_PENDING']);
  assert.equal(normalizeOrderStatus('ATTENTION'), 'ATTENTION');
  assert.equal(normalizeOrderStatus('REFUND_PENDING'), 'REFUND_PENDING');
  assert.equal(normalizeOrderStatus('INVALID'), '');
  assert.equal(normalizeOrderStatus(['PENDING']), '');
  assert.equal(isOtherOrderStatus('DELIVERY_FAILED'), true);
  assert.equal(isOtherOrderStatus('PENDING'), false);
});

test('orderId query accepts only one positive safe integer string', () => {
  assert.equal(parseOrderIdQuery('9'), 9);
  assert.equal(parseOrderIdQuery(String(Number.MAX_SAFE_INTEGER)), Number.MAX_SAFE_INTEGER);
  for (const value of [undefined, null, '', '0', '-9', '9.5', '9abc', '01', ['9'], ['9', '10'], String(Number.MAX_SAFE_INTEGER + 1)]) {
    assert.equal(parseOrderIdQuery(value), null, String(value));
  }
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

test('full order detail mirrors drawer fact order and preserves browser return context', () => {
  const source = readFileSync(new URL('../src/views/admin/OrderDetailPage.vue', import.meta.url), 'utf8');
  const orderedMarkers = [
    'order-detail-identity',
    'order-detail-fulfillment',
    'order-detail-items',
    'order-detail-payment',
    'order-detail-timeline',
    'order-detail-actions',
  ];
  let previousIndex = -1;
  for (const marker of orderedMarkers) {
    const index = source.indexOf(marker);
    assert.ok(index > previousIndex, `${marker} must follow the drawer hierarchy`);
    previousIndex = index;
  }
  assert.match(source, /href="`tel:\$\{order\.customerPhone\}`"/);
  assert.match(source, /paymentMethodLabel\(order\.paymentMethod\)/);
  assert.match(source, /paymentStatusLabel\(order\.paymentStatus\)/);
  assert.match(source, /@click="router\.back\(\)"/);
});

test('full order detail keeps canonical loader and existing mutations', () => {
  const source = readFileSync(new URL('../src/views/admin/OrderDetailPage.vue', import.meta.url), 'utf8');
  assert.match(source, /adminApi\.getOrderById\(route\.params\.id\)/);
  assert.match(source, /adminApi\.cancelOrder\(order\.value\.orderId, \{ expectedStatus: order\.value\.status, reason:/);
  assert.match(source, /adminApi\.overrideDeliveryAttempt\(order\.value\.orderId, order\.value\.status,/);
  assert.match(source, /adminApi\.addOrderNote\(order\.value\.orderId, order\.value\.status,/);
});
