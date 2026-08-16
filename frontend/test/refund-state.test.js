import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildRefundPresentation,
  canProcessRefund,
  canViewRefundDetail,
  refundAuditDetail,
  createRefundModalLifecycle,
  focusCycleTarget,
  submitPendingRefund,
} from '../src/views/admin/refund-state.js';

const pending = {
  orderId: 7,
  paymentStatus: 'REFUNDED',
  refundStatus: 'PENDING',
  refundProcessedBy: null,
  refundProcessedByName: null,
  refundReference: null,
  refundNote: null,
  refundedAt: null,
};

test('pending presentation overrides refunded payment copy and has no terminal audit', () => {
  assert.deepEqual(buildRefundPresentation(pending), {
    paymentLabel: 'Chưa xác nhận hoàn',
    refundLabel: 'Chờ hoàn thủ công',
    pendingDetail: 'Tiền chưa được xác nhận đã hoàn',
    audit: null,
  });
});

test('terminal refund presentation exposes complete read-only audit with processor fallback', () => {
  assert.deepEqual(buildRefundPresentation({
    ...pending,
    refundStatus: 'REFUNDED',
    refundAmount: 100000,
    refundProcessedBy: 4,
    refundProcessedByName: '',
    refundReference: 'BANK-9',
    refundNote: 'Đã chuyển khoản',
    refundedAt: '2026-08-15T10:00:00Z',
  }), {
    paymentLabel: 'Đã hoàn',
    refundLabel: 'Đã xác nhận hoàn',
    pendingDetail: '',
    audit: {
      processor: 'Admin #4',
      reference: 'BANK-9',
      note: 'Đã chuyển khoản',
      refundedAt: '2026-08-15T10:00:00Z',
    },
  });
  const rejected = buildRefundPresentation({ ...pending, refundStatus: 'REJECTED', refundProcessedByName: 'Nguyễn An' });
  assert.equal(rejected.paymentLabel, 'Không hoàn tiền');
  assert.deepEqual(rejected.audit, {
    processor: 'Nguyễn An', reference: '—', note: '—', refundedAt: null,
  });
});

test('terminal detail eligibility and data stay read-only and complete', () => {
  assert.equal(canViewRefundDetail(pending), false);
  assert.equal(canViewRefundDetail({ ...pending, refundStatus: 'REFUNDED' }), true);
  assert.equal(canViewRefundDetail({ ...pending, refundStatus: 'REJECTED' }), true);
  assert.deepEqual(refundAuditDetail({
    ...pending,
    refundStatus: 'REFUNDED',
    refundProcessedBy: 8,
    refundProcessedByName: '',
    refundReference: 'REF-8',
    refundNote: 'Đã hoàn qua ngân hàng',
    refundedAt: '2026-08-15T12:00:00Z',
  }), {
    processor: 'Admin #8',
    reference: 'REF-8',
    note: 'Đã hoàn qua ngân hàng',
    refundedAt: '2026-08-15T12:00:00Z',
  });
});

test('refund mutation accepts only current pending row and rejects stale terminal state', async () => {
  assert.equal(canProcessRefund(pending), true);
  assert.equal(canProcessRefund({ ...pending, refundStatus: 'REFUNDED' }), false);
  let calls = 0;
  const state = { selected: pending, rows: [{ ...pending, refundStatus: 'REFUNDED' }], statusMessage: '', errorMessage: '' };
  const result = await submitPendingRefund(state, async () => { calls += 1; });
  assert.equal(result, false);
  assert.equal(calls, 0);
  assert.equal(state.errorMessage, 'Yêu cầu hoàn tiền đã được xử lý. Vui lòng tải lại dữ liệu mới nhất.');
});

test('refund mutation submits once when selected and current row remain pending', async () => {
  let calls = 0;
  const state = { selected: pending, rows: [pending], statusMessage: '', errorMessage: '' };
  const result = await submitPendingRefund(state, async order => { calls += 1; assert.equal(order, pending); });
  assert.equal(result, true);
  assert.equal(calls, 1);
  assert.equal(state.statusMessage, 'Đã lưu kết quả hoàn tiền.');
});

test('refund focus cycle handles outside focus and both tab boundaries', () => {
  const controls = ['close', 'status', 'submit'];
  assert.equal(focusCycleTarget({ controls, active: 'outside', shiftKey: false }), 'close');
  assert.equal(focusCycleTarget({ controls, active: 'submit', shiftKey: false }), 'close');
  assert.equal(focusCycleTarget({ controls, active: 'close', shiftKey: true }), 'submit');
  assert.equal(focusCycleTarget({ controls, active: 'status', shiftKey: false }), null);
});

test('refund modal lifecycle contains outside focus handles escape and restores on cleanup', () => {
  const listeners = new Map();
  const document = {
    activeElement: null,
    addEventListener(type, listener) { listeners.set(type, listener); },
    removeEventListener(type, listener) { if (listeners.get(type) === listener) listeners.delete(type); },
  };
  const element = id => ({ id, isConnected: true, focusCalls: 0, focus() { this.focusCalls += 1; document.activeElement = this; } });
  const opener = element('opener');
  const first = element('first');
  const last = element('last');
  const outside = element('outside');
  const fallback = element('fallback');
  const dialog = { contains: target => target === first || target === last, focus() {} };
  let escapes = 0;
  const lifecycle = createRefundModalLifecycle({ document, getDialog: () => dialog, getFocusable: () => [first, last], onEscape: () => { escapes += 1; }, getFallback: () => fallback });

  lifecycle.attach();
  lifecycle.open(opener);
  assert.equal(first.focusCalls, 1);
  listeners.get('focusin')({ target: outside });
  assert.equal(first.focusCalls, 2);
  listeners.get('keydown')({ key: 'Escape', preventDefault() {} });
  assert.equal(escapes, 1);
  lifecycle.close();
  assert.equal(opener.focusCalls, 1);
  lifecycle.detach();
  assert.equal(listeners.size, 0);
});
