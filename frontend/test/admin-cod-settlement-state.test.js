import test from 'node:test';
import assert from 'node:assert/strict';
import {
  acceptsAdminCodRequest,
  buildVerificationPayload,
  canVerifySettlement,
  createModalLifecycle,
  focusCycleTarget,
  openVerification,
  submitVerification,
  validateVerification,
} from '../src/views/admin/cod-settlement-state.js';

const settlement = { settlementId: 9, status: 'SUBMITTED', submittedAmount: 120000 };

test('admin COD validation enforces status amount direction and reason', () => {
  assert.equal(validateVerification({ settlement, status: 'SETTLED', verifiedAmount: '119999', reason: '' }), 'Số tiền khớp phải bằng số đã nộp.');
  assert.equal(validateVerification({ settlement, status: 'SHORT', verifiedAmount: '120000', reason: 'Thiếu' }), 'Thiếu tiền cần số kiểm đếm thấp hơn số đã nộp.');
  assert.equal(validateVerification({ settlement, status: 'OVER', verifiedAmount: '120001', reason: '  ' }), 'Vui lòng nhập lý do chênh lệch.');
  assert.equal(validateVerification({ settlement, status: 'OVER', verifiedAmount: '120001', reason: ' Thừa 1.000đ ' }), '');
});

test('admin COD builds exact verification payload and disables terminal rows', () => {
  assert.deepEqual(buildVerificationPayload(settlement, 'SHORT', '119000', ' Thiếu 1.000đ '), {
    expectedStatus: 'SUBMITTED', status: 'SHORT', verifiedAmount: 119000, reason: 'Thiếu 1.000đ',
  });
  assert.deepEqual(buildVerificationPayload(settlement, 'SETTLED', '120000', 'ignored'), {
    expectedStatus: 'SUBMITTED', status: 'SETTLED', verifiedAmount: 120000, reason: null,
  });
  assert.equal(canVerifySettlement(settlement), true);
  for (const status of ['SHORT', 'OVER', 'SETTLED']) assert.equal(canVerifySettlement({ ...settlement, status }), false);
});

test('admin COD 409 submits once retains dialog values and refreshes canonical list', async () => {
  const state = openVerification({ selected: null }, settlement, { id: 'trigger' });
  state.status = 'SHORT';
  state.verifiedAmount = '119000';
  state.reason = 'Thiếu 1.000đ';
  let submitCalls = 0;
  let refreshCalls = 0;
  await submitVerification(state, {
    verify: async () => { submitCalls += 1; throw Object.assign(new Error('Conflict'), { status: 409 }); },
    refresh: async () => { refreshCalls += 1; return [{ ...settlement, status: 'SETTLED' }]; },
  });
  assert.equal(submitCalls, 1);
  assert.equal(refreshCalls, 1);
  assert.equal(state.selected, settlement);
  assert.equal(state.status, 'SHORT');
  assert.equal(state.verifiedAmount, '119000');
  assert.equal(state.reason, 'Thiếu 1.000đ');
  assert.equal(state.conflictMessage, 'Bàn giao đã được xử lý ở nơi khác. Danh sách mới nhất đã được tải.');
  assert.equal(state.rows[0].status, 'SETTLED');
});

test('admin COD closes only after successful verification and preserves opener target', async () => {
  const trigger = { id: 'verify-trigger' };
  const state = openVerification({ selected: null }, settlement, trigger);
  assert.equal(state.focusTarget, 'first-control');
  await submitVerification(state, { verify: async () => {}, refresh: async () => [] });
  assert.equal(state.selected, null);
  assert.equal(state.restoreTarget, trigger);
});

test('admin COD focus cycle wraps boundaries and leaves inside or outside focus unchanged', () => {
  const controls = ['close', 'outcome', 'amount', 'cancel', 'submit'];
  assert.equal(focusCycleTarget({ controls, active: 'submit', shiftKey: false }), 'close');
  assert.equal(focusCycleTarget({ controls, active: 'close', shiftKey: true }), 'submit');
  assert.equal(focusCycleTarget({ controls, active: 'amount', shiftKey: false }), null);
  assert.equal(focusCycleTarget({ controls, active: 'outside', shiftKey: false }), 'close');
  assert.equal(focusCycleTarget({ controls: [], active: 'outside', shiftKey: true }), null);
});

test('admin COD modal lifecycle executes focus containment escape restore and cleanup', () => {
  const listeners = new Map();
  const document = {
    activeElement: null,
    addEventListener(type, listener) { listeners.set(type, listener); },
    removeEventListener(type, listener) { if (listeners.get(type) === listener) listeners.delete(type); },
  };
  const element = id => ({ id, isConnected: true, focusCalls: 0, focus() { this.focusCalls += 1; document.activeElement = this; } });
  const opener = element('opener');
  const fallback = element('fallback');
  const first = element('first');
  const last = element('last');
  const outside = element('outside');
  const dialog = { contains(target) { return target === first || target === last; }, focusCalls: 0, focus() { this.focusCalls += 1; document.activeElement = this; } };
  let closeCalls = 0;
  const lifecycle = createModalLifecycle({ document, getDialog: () => dialog, getFocusable: () => [first, last], onEscape: () => { closeCalls += 1; }, getFallback: () => fallback });

  lifecycle.attach();
  assert.deepEqual([...listeners.keys()].sort(), ['focusin', 'keydown']);
  lifecycle.open(opener);
  assert.equal(first.focusCalls, 1);
  listeners.get('focusin')({ target: outside });
  assert.equal(first.focusCalls, 2);
  listeners.get('keydown')({ key: 'Escape' });
  assert.equal(closeCalls, 1);
  lifecycle.close();
  assert.equal(opener.focusCalls, 1);
  opener.isConnected = false;
  lifecycle.open(opener);
  lifecycle.close();
  assert.equal(fallback.focusCalls, 1);
  lifecycle.detach();
  assert.equal(listeners.size, 0);
});

test('admin COD modal lifecycle falls back to dialog when no focusable control exists', () => {
  const listeners = new Map();
  const document = { addEventListener(type, listener) { listeners.set(type, listener); }, removeEventListener(type) { listeners.delete(type); } };
  const dialog = { focusCalls: 0, contains() { return false; }, focus() { this.focusCalls += 1; } };
  const lifecycle = createModalLifecycle({ document, getDialog: () => dialog, getFocusable: () => [], onEscape() {}, getFallback: () => null });
  lifecycle.attach();
  lifecycle.open(null);
  listeners.get('focusin')({ target: {} });
  assert.equal(dialog.focusCalls, 2);
  lifecycle.detach();
});

test('admin COD request acceptance rejects stale filter responses', () => {
  assert.equal(acceptsAdminCodRequest({ requestGeneration: 3, latestGeneration: 3, requestStatus: 'OVER', activeStatus: 'OVER', stopped: false }), true);
  assert.equal(acceptsAdminCodRequest({ requestGeneration: 2, latestGeneration: 3, requestStatus: 'SHORT', activeStatus: 'OVER', stopped: false }), false);
  assert.equal(acceptsAdminCodRequest({ requestGeneration: 3, latestGeneration: 3, requestStatus: 'SHORT', activeStatus: 'OVER', stopped: false }), false);
  assert.equal(acceptsAdminCodRequest({ requestGeneration: 3, latestGeneration: 3, requestStatus: 'OVER', activeStatus: 'OVER', stopped: true }), false);
});
