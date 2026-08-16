import test from 'node:test';
import assert from 'node:assert/strict';
import { buildRefundPresentation, canMutateRefund, canViewRefundDetail, refundAuditDetail } from '../src/utils/refund-state.js';

const pending = { orderId: 7, paymentStatus: 'REFUNDED', refundStatus: 'PENDING' };

test('production refund presentation prioritizes pending refund status over payment status', () => {
  assert.deepEqual(buildRefundPresentation(pending), {
    paymentLabel: 'Chưa xác nhận hoàn',
    refundLabel: 'Chờ hoàn thủ công',
    pendingDetail: 'Tiền chưa được xác nhận đã hoàn',
    audit: null,
  });
});

test('production refund guards allow only pending mutation and terminal detail', () => {
  assert.equal(canMutateRefund(pending), true);
  for (const refundStatus of ['REFUNDED', 'REJECTED']) {
    const row = { ...pending, refundStatus };
    assert.equal(canMutateRefund(row), false);
    assert.equal(canViewRefundDetail(row), true);
  }
});

test('production refund detail prefers processor name then ID and preserves audit fields', () => {
  assert.deepEqual(refundAuditDetail({
    ...pending,
    refundStatus: 'REFUNDED',
    refundProcessedBy: 8,
    refundProcessedByName: '',
    refundReference: 'REF-8',
    refundNote: 'Đã hoàn qua ngân hàng',
    refundedAt: '2026-08-15T12:00:00Z',
  }), { processor: 'Admin #8', reference: 'REF-8', note: 'Đã hoàn qua ngân hàng', refundedAt: '2026-08-15T12:00:00Z' });
  assert.equal(refundAuditDetail({ ...pending, refundProcessedBy: 8, refundProcessedByName: 'Nguyễn An' }).processor, 'Nguyễn An');
});
